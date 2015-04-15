package mosaic.bregman;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.BackgroundSubtracter;
//import net.sf.javaml.tools.weka.WekaClusterer;
//import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
//import weka.clusterers.Cobweb;
//import weka.clusterers.SimpleKMeans;
//import weka.clusterers.XMeans;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;

import net.sf.javaml.tools.DatasetTools;

/**
 * 
 * Class to solve the N regions problems, two regions inherit from this
 * 
 * It remove the background, normalize the image, it run split bregman
 * and it can clusters the image based on intensities levels
 * 
 * @author Aurelien Ritz
 *
 */

public class NRegions implements Runnable{

	Tools LocalTools;
	//image and mask data
	public double [] [] [] image;// 3D image
	public double [] [] [] [] mask;//nregions nslices ni nj
	public int [] [] [] maxmask;
	double [] [] [] [] Ei; 
	public MasksDisplay md;

	//levels value

	//double [] cl ={0.0027, 0.0480	,0.09  ,0.1653    ,0.3901};

	public Parameters p;

	//properties
	//number of regions
	int bits;
	int ni,nj,nz;//3 dimensions
	int nl;
	int channel;
	CountDownLatch DoneSignal;
	double min;
	double max;
	

	public NRegions(ImagePlus img, Parameters params, CountDownLatch DoneSignal, int channel)
	{
		//IJ.log("Computing segmentation ..");
		BackgroundSubtracter bs = new BackgroundSubtracter();
		//Parameters params = new Parameters();
		this.p = params;

		this.LocalTools=new Tools(p.ni,p.nj,p.nz);
		this.DoneSignal=DoneSignal;
		this.nl=p.nlevels;
		ImageProcessor imp;
		int os=p.model_oversampling;
		int osz;
		p.ni=p.ni*p.model_oversampling;
		p.nj=p.nj*p.model_oversampling;

		if(p.nz>1)
			osz=p.model_oversampling;
		else
			osz=1;

		p.nz=p.nz*osz;

		this.ni=p.ni;
		this.nj=p.nj;
		this.nz=p.nz;

		bits= img.getBitDepth();
		if(bits==32){IJ.log("Error converting float image to short");}

		this.channel=channel;


		LocalTools.setDims(ni, nj, nz, nl);

		//allocate
		image = new double [nz] [ni] [nj];
		mask= new double [nl] [nz] [ni] [nj];
		if(p.nlevels >1  || !p.usePSF) //save memory when Ei not needed
			Ei = new double [nl] [nz] [ni] [nj];
		else
			Ei=null;

		max=0;
		min=Double.POSITIVE_INFINITY;
		//change : use max value instead of 65536

		/* Search for maximum and minimum value, normalization */
		
		if (Analysis.norm_max == 0)
		{
			for (int z=0; z<nz/osz; z++)
			{
				img.setSlice(z+1);
				imp=img.getProcessor();
				for (int i=0; i<ni/os; i++)
				{
					for (int j=0;j< nj/os; j++)
					{
						if(imp.getPixel(i,j)>max)max=imp.getPixel(i,j);
						if(imp.getPixel(i,j)<min)min=imp.getPixel(i,j);
					}	
				}
			}
		}
		else
		{
			max = Analysis.norm_max;
			min = Analysis.norm_min;
		}
		
		//IJ.log("before, max : " + max + " min : " + min);


		if(p.usecellmaskX && channel==0)
			Analysis.cellMaskABinary=createBinaryCellMask(Analysis.p.thresholdcellmask*(max-min) +min, img, channel, osz);
		if(p.usecellmaskY && channel==1)
			Analysis.cellMaskBBinary=createBinaryCellMask(Analysis.p.thresholdcellmasky*(max-min) +min, img, channel, osz);
		//get imagedata and copy to array image

		max=0;
		min=Double.POSITIVE_INFINITY;

		for (int z=0; z<nz; z++)
		{
			img.setSlice(z/osz+1);
			imp=img.getProcessor();
			//remove background
			//rollingball version
			if(p.removebackground)
			{
				bs.rollingBallBackground(imp, p.size_rollingball, false, false, false, true, true);
			}

			//slidingparaboloid version test (do both lines)
			//bs.rollingBallBackground(imp, 0.1, false, false, true, true, true);
			//bs.rollingBallBackground(imp, 0.2, false, false, true, false, true);
			
			
			for (int i=0; i<ni; i++)
			{
				for (int j=0;j< nj; j++)
				{  
					image[z][i][j]=imp.getPixel(i/os,j/os);					
					if(image[z][i][j]>max)max=image[z][i][j];
					if(image[z][i][j]<min)min=image[z][i][j];
				}	
			}
			
		}
		
		/* Again overload the parameter after background subtraction */
		
		if (Analysis.norm_max != 0)
		{
			max = Analysis.norm_max;
			if(p.removebackground)
			{
				// if we are removing the background we have no idea which
				// is the minumum across all the movie so let be conservative and put
				// min = 0.0 for sure cannot be < 0
				
				min = 0.0;
			}
			else
			{
				min = Analysis.norm_min;
			}
		}
		
		//IJ.log("after, max : " + max + " min : " + min);
		if(p.livedisplay && p.removebackground)
		{
			ImagePlus back=img.duplicate();
			back.setTitle("Background reduction channel " + (channel+1));
			back.changes=false;
			back.setDisplayRange(min,max);
			back.show();
		}
		

		// normalize the image
		for (int z=0; z<nz; z++)
		{
			for (int i=0; i<ni; i++)
			{
				for (int j=0;j<nj; j++)
				{
					image[z][i][j]= (image[z][i][j] -min)/(max-min);
					if (image[z][i][j] < 0.0) image[z][i][j] = 0.0;
					else if (image[z][i][j] > 1.0) image[z][i][j] = 1.0;
				}	
			}
		}

		if(p.nlevels>2)
		{
			p.cl=cluster();
		}

		if(p.nlevels==2 || p.nlevels==1)
		{

			p.cl[0]=p.betaMLEoutdefault;//0.0027356;
			//p.cl[1]=0.2340026;
			p.cl[1]=p.betaMLEindefault;//0.1340026;
			//p.cl[1]=0.2;
		}

		if(Analysis.p.automatic_int)
		{
			double [] levs=cluster_int(5);
			p.cl[0]=levs[0];//0.0027356;
			p.betaMLEoutdefault=levs[0];
			p.cl[1]=levs[3];
			p.betaMLEindefault=levs[3];
			IJ.log("automatic background:" + Tools.round(p.cl[0],3));
			IJ.log("automatic foreground:" + Tools.round(p.cl[1],3));
		}


		//		double test [];
		//		test=cluster3();
		//		IJ.log("cl0 :" + test[0] + " cl1 " + test[1] + " cl2 " + test[2]);//" " + cl[3]+ " " + cl[4] );

		//IJ.log("cl :" + p.cl[0] + " " + p.cl[1] + " " + p.cl[2] + " " + p.cl[3]+ " " + p.cl[4] );
		//IJ.log(String.format(" cl0 :  %4.2e %ncl1 :  %4.2e %ncl2 :  %4.2e %ncl3 :  %4.2e %ncl4 :  %4.2e %n",
		//		 p.cl[0],p.cl[1],p.cl[2],p.cl[3],p.cl[4]));

		//		if(p.livedisplay){
		//			IJ.log("Intensities :");
		//			for(int i=0; i <p.nlevels; i++){
		//				IJ.log("level "+ i +" :"+ p.cl[i]);
		//			}
		//		}

		if(p.JunitTest)
		{
			p.cl[0]=0.00227;
			p.cl[1]=0.0504;
			p.cl[2]=0.165;
			p.cl[3]=0.396;
			p.cl[4]=0.684;
		}

		//md= new MasksDisplay(ni,nj,nz,nl,p.cl,p);
		LocalTools.createmask(mask, image,p.cl);	
		//md.display2regionsnewd(mask[0][0], "mask init", 0);
		if(p.nlevels >1  || !p.usePSF)
		{
			for(int i =0; i< nl;i++)
			{
				//Tools.nllMeanPoisson(Ei[i], image, p.cl[i], 1, p.ldata );
				LocalTools.nllMean1(Ei[i], image, p.cl[i], 1, p.ldata );
			}
		}
	}


	//	private int returnpix(ImageProcessor imp){
	//		
	//		if(bits==32)
	//		Float.intBitsToFloat()
	//	}


	public void  run()
	{
		md= new MasksDisplay(ni,nj,nz,nl,p.cl,p);
		md.firstdisp=p.livedisplay;
		ASplitBregmanSolver A_solver= new ASplitBregmanSolver(p,image,Ei, mask,md, channel);

		//first run
		try 
		{
			A_solver.first_run();
		}
		catch (InterruptedException ex) 
		{}

		double minInt=Analysis.p.min_intensity;
		Analysis.p.min_intensity=0;
		if(channel==0){
			//Analysis.maxmaska=A_solver.maxmask;
			Analysis.setmaska(A_solver.maxmask);
			Analysis.bestEnergyX=A_solver.bestNrj;
			//Analysis.maska=A_solver.w3k[0];
			if(!Analysis.p.looptest){
				if(p.nlevels==2)Analysis.compute_connected_regions_a(0.5,null);
				else Analysis.compute_connected_regions_a(1.5,null);
				A_solver=null;
			}
			else
				Analysis.solverX=A_solver;
		}
		else{
			Analysis.setmaskb(A_solver.maxmask);
			Analysis.bestEnergyY=A_solver.bestNrj;
			//Analysis.maxmaskb=A_solver.maxmask;
			//Analysis.maskb=A_solver.w3k[0];	
			if(!Analysis.p.looptest){
				if(p.nlevels==2)Analysis.compute_connected_regions_b(0.5,null);
				else Analysis.compute_connected_regions_b(1.5,null);
				A_solver=null;
			}
			else
				Analysis.solverY=A_solver;
		}
		Analysis.p.min_intensity=minInt;


		DoneSignal.countDown();

	}

	private double [] cluster()
	{
		Dataset data = new DefaultDataset();
		double [] pixel = new double[1];
		int max = Math.max(2, nl);
		double [] levels= new double[max];
		//double [] levels2= new double[5];

		//get imagedata
		for (int z=0; z<nz; z++)
		{
			for (int i=0; i<ni; i++) 
			{
				for (int j=0;j<nj; j++) 
				{
					pixel[0]=image[z][i][j];
					Instance instance = new DenseInstance(pixel);
					data.add(instance);
				}	
			}
		}


		Clusterer km = new KMeans(nl);
		/* Cluster the data, it will be returned as an array of data sets, with
		 * each dataset representing a cluster. */
		Dataset[] data2 = km.cluster(data);
		for (int i=0; i<nl; i++) 
		{  
			Instance inst =DatasetTools.average(data2[i]);
			//IJ.log("instance i" + i + "is " + inst.value(0));
			levels[i]=inst.value(0);
		}

		Arrays.sort(levels);





		//		
		//		
		//		
		//		//test Weka Xmeans clustering
		//		XMeans xm2 = new XMeans();
		//		try{
		//		//	xm2.setNumClusters(4);//3
		//			//xm2.setMaxIterations(100);
		//			
		//			xm2.setMaxNumClusters(5);
		//			xm2.setMinNumClusters(2);
		//			xm2.setMaxIterations(5);
		//			//xm2.setCutOffFactor(0.5);
		//		}catch (Exception ex) {}
		//		Clusterer jmlxm2 = new WekaClusterer(xm2);
		//		//		/* Perform clustering */
		//		Dataset[] clusts = jmlxm2.cluster(data);
		//		//		/* Output results */
		//		//		//System.out.println(clusters.length);
		//		//
		//		//
		//
		//		int nk2=clusts.length;//get number of clusters  really found (usually = 3 = setNumClusters but not always)
		//		IJ.log("xmeans clusts :" + nk2);
		//		for (int i=0; i<nk2; i++) {  
		//			//Instance inst =DatasetTools.minAttributes(data2[i]);
		//			Instance inst =DatasetTools.average(clusts[i]);
		//			levels2[i]=inst.value(0);
		//		}
		//		//
		//		//
		//				Arrays.sort(levels2);
		//		////		IJ.log("");
		//		////		IJ.log("levels :");
		//		////		IJ.log("level 1 : " + levels[0]);
		//		////		IJ.log("level 2 : " + levels[1]);
		//		////		IJ.log("level 3 : " + levels[2]);
		//		////		IJ.log("level 4 : " + levels[3]);
		//		//		
		//		IJ.log("levels2 :");
		//		for (int i=0; i<5; i++) {  
		//			IJ.log("level2 "+(i+1) + " : " + levels2[i]);
		//		}
		//		

		return levels;



	}


	public double [] cluster_int(int nll){

		Dataset data = new DefaultDataset();
		double [] pixel = new double[1];
		double [] levels= new double[nll];


		//get imagedata
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j<nj; j++) {  
					pixel[0]=image[z][i][j];
					Instance instance = new DenseInstance(pixel);
					data.add(instance);
				}	
			}
		}


		Clusterer km = new KMeans(nll);
		/* Cluster the data, it will be returned as an array of data sets, with
		 * each dataset representing a cluster. */
		Dataset[] data2 = km.cluster(data);
		for (int i=0; i<nll; i++) {  
			Instance inst =DatasetTools.average(data2[i]);
			//IJ.log("instance i test" + i + "is " + inst.value(0));
			levels[i]=inst.value(0);
		}


		Arrays.sort(levels);
		return levels;


	}



	boolean [][][] createBinaryCellMask(double threshold, ImagePlus img, int channel, int osz){
		boolean [][][] cellmask= new boolean [nz] [ni] [nj];

		ImagePlus maska_im= new ImagePlus();
		ImageStack maska_ims= new ImageStack(ni,nj);
		ImageProcessor imp;

		for (int z=0; z<nz; z++){
			img.setSlice(z/osz+1);
			imp=img.getProcessor();
			byte[] maska_bytes = new byte[ni*nj];
			for (int i=0; i<ni; i++){  
				for (int j=0;j< nj; j++){  
					if(imp.getPixel(i/p.model_oversampling,j/p.model_oversampling)>threshold)
						maska_bytes[j * ni + i]=(byte) 255;
					else 
						maska_bytes[j * ni + i]=0;

				}	
			}
			ByteProcessor bp = new ByteProcessor(ni, nj);
			bp.setPixels(maska_bytes);
			maska_ims.addSlice("", bp);
		}


		maska_im.setStack("Cell mask channel " + (channel+1),maska_ims);

		IJ.run(maska_im, "Invert", "stack");
		//		
		//		//IJ.run(maska_im, "Erode", "");
		IJ.run(maska_im, "Fill Holes", "stack");
		IJ.run(maska_im, "Open", "stack");


		IJ.run(maska_im, "Invert", "stack");
		//maska_im.show("mask");

		if(Analysis.p.dispwindows && Analysis.p.livedisplay){
		maska_im.show();
	}
		


		if (Analysis.p.save_images)
		{	
			//			FileSaver fs= new FileSaver(maska_im);
			//			//IJ.log(img.getTitle());
			String savepath;
			if (channel==0){
				savepath = Analysis.p.wd + img.getTitle().substring(0,img.getTitle().length()-4) + "_mask_c1" +".zip";
			}
			else{
				savepath = Analysis.p.wd + img.getTitle().substring(0,img.getTitle().length()-4) + "_mask_c2" +".zip";
			}
			//			if (Analysis.p.nz >1) fs.saveAsTiffStack(savepath);
			//			else fs.saveAsTiff(savepath);	

			//IJ.log("save path cell mask: " + savepath);
			IJ.saveAs(maska_im, "ZIP", savepath);

		}



		for (int z=0; z<nz; z++){
			maska_im.setSlice(z+1);
			imp=maska_im.getProcessor();
			for (int i=0; i<ni; i++){  
				for (int j=0;j< nj; j++){  
					cellmask[z][i][j]=imp.getPixel(i,j) >254;	
				}	
			}
		}

		return cellmask;

	}


}
