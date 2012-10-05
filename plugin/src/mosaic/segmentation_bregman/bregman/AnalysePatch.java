package bregman;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Resizer;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.clustering.evaluation.AICScore;
import net.sf.javaml.clustering.evaluation.BICScore;
import net.sf.javaml.clustering.evaluation.SumOfSquaredErrors;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;
import net.sf.javaml.tools.weka.WekaClusterer;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Cobweb;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.XMeans;

import bregman.FindConnectedRegions.Region;

public class AnalysePatch implements Runnable{
	//add oversampling here
	Tools Tools;
	ImagePatches impa;
	int interpolation;
	int interpolationz;
	double mint;//min threshold
	double cin, cout, cout_front;//estimated intensities
	double intmin, intmax;
	int os;
	int osz;
	//coordinate of pacth in original image
	int offsetx;
	int offsety;
	int offsetz;
	int margin;
	int zmargin;
	int ox,oy,oz;//size of full image
	int wmar;//weights margin
	int rmmar;//mask for refined object computation (must be inside)
	// size of patch
	int sx;
	int sy;
	int sz;

	int fsxy, fsz;

	int isx;
	int isy;
	int isz; //interpolated object sizes
	double [][][] patch;
	double [][][] object;
	double [][][] interpolated_object;
	public double [][][][] mask;//nregions nslices ni nj
	public double [][][][] speedData;//nregions nslices ni nj
	public Parameters p;
	Region r;
	int channel;
	double rescaled_min_int;
	double [][][][] temp1;
	double [][][][] temp2;
	double [][][][] temp3;
	double [][][] weights;
	//double [][][] w3kbest;
	double [][][] w3kpatch;
	double [][][] refined_mask;
	int [][][] regions_refined;



	public AnalysePatch(double [][][] image, Region r, Parameters pa, int oversampling, int channel, int [][][] regionsf, ImagePatches impa){
		//IJ.log("creating patch :"+ r.value);
		//	this.w3kbest=w3k;
		mint=0.2;
		this.impa=impa;
		this.os=oversampling;
		this.r=r;
		this.channel=channel;
		wmar=4;
		rmmar=8;
		ox=pa.ni;
		oy=pa.nj;
		oz=pa.nz;
		margin=4;// add margin to size of object to create patch
		if(pa.mode_voronoi2)margin=4;
		zmargin=2;
		this.regions_refined=regionsf;

		//create local parameters
		this.p=new Parameters(pa);

		this.interpolation= pa.interpolation;

		//compute patch geometry :
		set_patch_geom(r, oversampling);
		temp1= new double [1] [sz] [sx] [sy];
		temp2= new double [1] [sz] [sx] [sy];
		temp3= new double [1] [sz] [sx] [sy];

		//create weights mask (binary)
		if(p.mode_voronoi2)
			voronoi_mask(r.rvoronoi);//mask for voronoi region into weights
		else
			fill_weights(r);


		//create patch image with oversampling
		this.patch = new double [sz][sx][sy];
		fill_patch(image);
		//this.w3kpatch = new double [sz][sx][sy];
		//fill_w3kpatch(impa.w3kbest);
		fill_refined_mask(r);



		//create pbject (for result)
		this.object = new double [sz][sx][sy];

		//create mask
		this.mask=new double [1][sz][sx][sy];
		fill_mask(r);

		//set size
		p.ni=sx;p.nj=sy;p.nz=sz;
		this.Tools= new Tools(p.ni,p.nj,p.nz);
		//set psf
		if ( p.nz>1){
			Tools.gaussian3Dbis(p.PSF, p.kernelx, p.kernely, p.kernelz, 7, p.sigma_gaussian*oversampling, p.zcorrec*oversampling);//todo verif zcorrec
		}
		else
		{
			Tools.gaussian2D(p.PSF[0], p.kernelx, p.kernely, 7, p.sigma_gaussian*oversampling);
		}
		//normalize
		normalize();

		//estimate ints	
		if(p.mode_voronoi2){
			if(p.debug)	
				IJ.log("object :" + r.value);
			if(p.mode_intensity==0)
				estimate_int_weighted(mask[0]);
			//estimate_int_weighted(w3kpatch);
			else
				estimate_int_clustering(p.mode_intensity);
			p.cl[0]=cout;
			p.cl[1]=cin;
		}
		else{
			estimate_int_weighted(mask[0]);
			p.cl[0]=cout;
			p.cl[1]=cin;		
		}

		//max iterations
		p.max_nsb=101;
		p.nlevels=1;
		//betaMLE
		//done after launch :
		p.RSSinit=false;
		p.findregionthresh=false;
		p.RSSmodulo=501;

		p.thresh=0.75;
		p.remask=false;
		p.lreg=p.lreg*oversampling;

		//IJ.log("patch :"+ r.value + "created");
	}





	public void run(){
		//IJ.log("");
		//IJ.log("region " + r.value);
		MasksDisplay md= new MasksDisplay(sx,sy,sz,2,p.cl,p);

//								if(r.value==3 ||r.value==16)
//								//{md.display2regionsnew(patch[0], "Patch "+r.value, channel);}
//				{md.display2regions3Dnew(patch, "Patch "+r.value, channel);}
		//md.display2regionsnewd(mask[0][0],"mask binaire" +r.value, channel);
		//md.display2regionsnew(patch[0], "Mask" +r.value, channel);
		//md.display2regionsnewd(weights[0],"weights" +r.value, channel);
		//md.display2regionsnewd(refined_mask[0],"rmask" +r.value, channel);
		ASplitBregmanSolver A_solver;

		//IJ.log("pni " + p.ni + "pnj" + p.nj+ "pnz" + p.nz +"nlevels" +p.nlevels);
		//Tools.setDims(sx, sy, sz, p.nlevels);

		//		double [] cl = new double [2];
		//		cl[0]=0;
		//		cl[1]=0;

		p.nthreads=1;
		p.firstphase=false;
		//IJ.log("noise "+p.noise_model);
		
		
		if (p.nz>1){
			A_solver= new ASplitBregmanSolverTwoRegions3DPSF(p,patch,speedData,mask,md,channel);
		}
		else
		{
			A_solver= new ASplitBregmanSolverTwoRegionsPSF(p,patch,speedData,mask,md,channel);
		}

		try {
			A_solver.first_run();
			//md.display2regions(A_solver.w3kbest[0][0], "Mask patch" + r.value, channel);
//												if(r.value==3 || r.value==16)
//												//{md.display2regionsnew(A_solver.w3kbest[0][0], "Mask Patch "+r.value, channel);}
//						{md.display2regions3Dnew(A_solver.w3kbest[0], "Mask Patch "+r.value, channel);}
			cout=p.cl[0];
			cin=p.cl[1];
			//IJ.log("cout " + cout + "cin" + cin);
			double t=find_best_thresh(A_solver.w3kbest[0]);
			//todo : compute best threshold  for p.thresh
			if(p.debug)	
				IJ.log("best thresh : " + t +"region" + r.value);
			set_object(A_solver.w3kbest[0], t);
			if(p.mode_classic){
				estimate_int_weighted(object);
			}
			if(interpolation>1){
				build_interpolated_object(A_solver.w3kbest[0],t);
				object=interpolated_object;
			}
			//IJ.log("la");
			//IJ.log("last int cout " + cout + " cin " +cin);

			//assemble result into full image
			//assemble_result_voronoi2();

			assemble_patch();

		}catch (InterruptedException ex) {}

	}


	private void set_object(double [][][] w3kbest, double t){

		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){  
				for (int j=0;j< sy; j++){  
					if(w3kbest[z][i][j]>t && weights[z][i][j]==1)
						this.object[z][i][j] = 1;
					else
						this.object[z][i][j] = 0;
				}
			}
		}

	}


	private  void set_patch_geom(Region r, int oversampling){
		int xmin, ymin , zmin, xmax, ymax, zmax; 
		xmin=ox;ymin=oy;zmin=oz;
		xmax=0;ymax=0;zmax=0;

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			if(p.px<xmin)xmin=p.px;
			if(p.px>xmax)xmax=p.px;
			if(p.py<ymin)ymin=p.py;
			if(p.py>ymax)ymax=p.py;
			if(p.pz<zmin)zmin=p.pz;
			if(p.pz>zmax)zmax=p.pz; 
		}

		xmin=Math.max(0,xmin-margin);
		xmax=Math.min(ox,xmax+margin+1);

		ymin=Math.max(0,ymin-margin);
		ymax=Math.min(oy,ymax+margin+1);

		if(p.nz>1){//if(zmax-zmin>0){
			// do whole column
			//zmin=0;
			//zmax=p.nz-1;
			//old one
			zmin=Math.max(0,zmin-zmargin);
			zmax=Math.min(oz,zmax+zmargin+1);			
		}


		this.offsetx=xmin;
		this.offsety=ymin;
		this.offsetz=zmin;

		this.sx=(xmax-xmin)*oversampling;//todo :correct :+1 : done
		this.sy=(ymax-ymin)*oversampling;//correct :+1

		//IJ.log("oversampling" + oversampling);
		this.fsxy=oversampling*interpolation;
		this.isx=sx*interpolation;
		this.isy=sy*interpolation;

		if(p.nz==1){//if(zmax-zmin==0){
			this.osz=1;
			this.sz=1;
			this.fsz=1;
			this.isz=1;
			interpolationz=1;
		}
		else{
			this.osz=os;
			this.sz=(zmax-zmin)*oversampling;
			this.fsz=oversampling*interpolation;
			this.isz=sz*interpolation;
			interpolationz=interpolation;
		}


	}


	private void fill_patch(double [][][] image){

		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){  
				for (int j=0;j< sy; j++){  
					this.patch[z][i][j] = image[z/osz+offsetz][i/os+offsetx][j/os+offsety];	
				}
			}
		}

		if(p.mode_voronoi2){
			for (int z=0; z<sz; z++){
				for (int i=0;i<sx; i++){  
					for (int j=0;j< sy; j++){ 
						if(weights[z][i][j]==0)
							this.patch[z][i][j] = 0;	
					}
				}
			}
		}

	}

	private void fill_w3kpatch(double [][][] w3k){

		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){  
				for (int j=0;j< sy; j++){  
					this.w3kpatch[z][i][j] = w3k[z/osz+offsetz][i/os+offsetx][j/os+offsety];	
				}
			}
		}

		if(p.mode_voronoi2){
			for (int z=0; z<sz; z++){
				for (int i=0;i<sx; i++){  
					for (int j=0;j< sy; j++){ 
						if(weights[z][i][j]==0)
							this.w3kpatch[z][i][j] = 0;	
					}
				}
			}
		}

	}


	private void normalize(){

		intmin=Double.MAX_VALUE;
		intmax=0;
		for (int z=0; z<sz; z++){	
			for (int i=0; i<sx; i++){  
				for (int j=0;j< sy; j++){  		
					if(patch[z][i][j]>intmax)intmax=patch[z][i][j];
					if(patch[z][i][j]<intmin)intmin=patch[z][i][j];
				}	
			}
		}

		rescaled_min_int=((p.min_intensity - intmin)/(intmax-intmin));
		//IJ.log("max:"+intmax+" min:"+intmin);
		//IJ.log("rescaled min int: " + ((p.min_intensity - intmin)/(intmax-intmin)));
		//rescale between 0 and 1
		for (int z=0; z<sz; z++){
			for (int i=0; i<sx; i++){  
				for (int j=0;j<sy; j++){  
					patch[z][i][j]= (patch[z][i][j] - intmin)/(intmax-intmin);		
				}	
			}
		}

	}

	private void estimate_int(double [][][] mask){
		RegionStatisticsSolver RSS;


		RSS= new RegionStatisticsSolver(temp1[0],temp2[0], temp3[0], patch, 10,p);
		RSS.eval(mask);
		//Analysis.p.cl[0]=RSS.betaMLEout;
		//Analysis.p.cl[1]=RSS.betaMLEin;


		cout=RSS.betaMLEout;
		cout_front=cout;
		cin=Math.min(1, RSS.betaMLEin);
		mint=Math.min(0.25, (rescaled_min_int-cout)/(cin-cout)-0.05);

		//IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", RSS.betaMLEout,RSS.betaMLEin));	


	}


	private void estimate_int_weighted(double [][][] mask){
		RegionStatisticsSolver RSS;


		RSS= new RegionStatisticsSolver(temp1[0],temp2[0], temp3[0], patch,weights, 10,p);
		RSS.eval(mask);
		//Analysis.p.cl[0]=RSS.betaMLEout;
		//Analysis.p.cl[1]=RSS.betaMLEin;


		cout=RSS.betaMLEout;
		cout_front=cout;
		cin=Math.min(1, RSS.betaMLEin);
		mint=0.25;
		//mint=Math.min(0.25, (rescaled_min_int-cout)/(cin-cout)-0.05);

		if(p.debug){
		IJ.log("mint "+ mint);
		IJ.log("rescaled min int" + rescaled_min_int);
		IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", cout,cin));	
		}

	}


	private void estimate_int_clustering(int level){
		//IJ.log("level" +level);
		int nk;
		if(level==1)nk=4; else nk=3;//3level culstering for low (removed) and high, 4 levels for medium

		int nk_in=0;
		//if(level==0)nk_in=1;//low
		if(level==2)nk_in=2;//high
		if(level==1)nk_in=2;//medium
		//int nk=4;//3
		double [] pixel = new double[1];
		double [] levels= new double[nk];
		double [] levels2= new double[4];

		Dataset data = new DefaultDataset();
		for (int z=0; z<sz; z++){
			for (int i=0; i<sx; i++){  
				for (int j=0;j<sy; j++){  
					pixel[0]=patch[z][i][j];
					Instance instance = new DenseInstance(pixel);
					if(p.mode_voronoi2){
						if(weights[z][i][j]==1)data.add(instance);
					}
					else
						data.add(instance);
				}	
			}
		}





//		
//				/* Create Weka classifier */
//				SimpleKMeans xm4 = new SimpleKMeans();
//		
//		
//				try{
//					xm4.setNumClusters(nk);//3
//					xm4.setMaxIterations(100);
//				}catch (Exception ex) {}
//		
//		
//		
//		
//				/* Wrap Weka clusterer in bridge */
//				Clusterer jmlxm4 = new WekaClusterer(xm4);
//				/* Perform clustering */
//				Dataset[] data2 = jmlxm4.cluster(data);
//				/* Output results */
//				//System.out.println(clusters.length);




		//IJ.log("clust .. ");
		Clusterer km = new KMeans(nk,100);
		//Clusterer km = new KMeans(3,200);
		/* Cluster the data, it will be returned as an array of data sets, with
		 * each dataset representing a cluster. */
		Dataset[] data2 = km.cluster(data);
		//IJ.log("clust done ");



		nk=data2.length;//get number of clusters  really found (usually = 3 = setNumClusters but not always)
		for (int i=0; i<nk; i++) {  
			//Instance inst =DatasetTools.minAttributes(data2[i]);
			Instance inst =DatasetTools.average(data2[i]);
			levels[i]=inst.value(0);
		}

		Arrays.sort(levels);
		//IJ.log("nk " + nk + "parameter " + Analysis.p.regionSegmentLevel + "level" + levels[Math.min(Analysis.p.regionSegmentLevel, nk-1)] );
		nk=Math.min(nk_in, nk-1);
		cin=Math.max(rescaled_min_int, levels[nk]);//-1;
		//betaMLEout=levels[0];
		int nkm1=Math.max(nk-1, 0);
		cout_front=levels[nkm1];
		cout=levels[0];
		mint=0.25;
		//mint=Math.min(0.25, (rescaled_min_int-cout)/(cin-cout)  -0.05);

		if(p.debug){
			IJ.log("mint "+ mint);

			IJ.log("rescaled min int" + rescaled_min_int);
			IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e",cout,cin));	

			IJ.log("levels :");
			for (int i=0; i<4; i++) {  
				IJ.log("level "+(i+1) + " : " + levels[i]);
			}
		}


		//
		//		SimpleKMeans xm2 = new SimpleKMeans();
		//		try{
		//			xm2.setNumClusters(2);//3
		//			xm2.setMaxIterations(100);
		//		}catch (Exception ex) {}
		//
		//		/* Wrap Weka clusterer in bridge */
		//		Clusterer jmlxm2 = new WekaClusterer(xm2);
		//		/* Perform clustering */
		//		Dataset[] clust2 = jmlxm2.cluster(data);
		//		
		//				//IJ.log("kmeans clusts :" + 2);
		//				for (int i=0; i<2; i++) {  
		//					//Instance inst =DatasetTools.minAttributes(data2[i]);
		//					Instance inst =DatasetTools.average(clust2[i]);
		//					IJ.log("level "+inst.value(0));
		//					levels2[i]=inst.value(0);
		//				}
		//		
		//		
		//		SimpleKMeans xm3 = new SimpleKMeans();
		//		try{
		//			xm3.setNumClusters(3);//3
		//			xm3.setMaxIterations(100);
		//		}catch (Exception ex) {}
		//
		//		/* Wrap Weka clusterer in bridge */
		//		Clusterer jmlxm3 = new WekaClusterer(xm3);
		//		/* Perform clustering */
		//		Dataset[] clust3 = jmlxm3.cluster(data);
		//
		//		
		//		IJ.log("kmeans clusts :" + 3);
		//		for (int i=0; i<3; i++) {  
		//			//Instance inst =DatasetTools.minAttributes(data2[i]);
		//			Instance inst =DatasetTools.average(clust3[i]);
		//			IJ.log("level "+inst.value(0));
		//			levels2[i]=inst.value(0);
		//		}
		//		
	}



	//	Arrays.sort(levels);
	//		IJ.log("");
	//		IJ.log("levels :");
	//		IJ.log("level 1 : " + levels[0]);
	//		IJ.log("level 2 : " + levels[1]);
	//		IJ.log("level 3 : " + levels[2]);
	//		IJ.log("level 4 : " + levels[3]);

	//		IJ.log("levels :");
	//		for (int i=0; i<nk; i++) {  
	//			IJ.log("level "+(i+1) + " : " + levels[i]);
	//		}



	//		XMeans xmm2 = new XMeans();
	//		try{
	//			xmm2.setMaxIterations(5);
	//			//xm2.setCutOffFactor(0.5);
	//		}catch (Exception ex) {}
	//		Clusterer jmlxmm2 = new WekaClusterer(xmm2);
	//		//		/* Perform clustering */
	//		Dataset[] data22 = jmlxmm2.cluster(data);
	//		//		/* Output results */
	//		//		//System.out.println(clusters.length);
	//		//
	//		//
	//
	//		int nk2=data22.length;//get number of clusters  really found (usually = 3 = setNumClusters but not always)
	//		IJ.log("xmeans clusts :" + nk2);
	//		for (int i=0; i<nk2; i++) {  
	//			//Instance inst =DatasetTools.minAttributes(data2[i]);
	//			Instance inst =DatasetTools.average(data22[i]);
	//			levels2[i]=inst.value(0);
	//		}
	//		//
	//		//
	//				Arrays.sort(levels2);
	//		//		IJ.log("");
	//		//		IJ.log("levels :");
	//		//		IJ.log("level 1 : " + levels[0]);
	//		//		IJ.log("level 2 : " + levels[1]);
	//		//		IJ.log("level 3 : " + levels[2]);
	//		//		IJ.log("level 4 : " + levels[3]);
	//		//		
	////		IJ.log("levels2 :");
	////		for (int i=0; i<4; i++) {  
	////			IJ.log("level2 "+(i+1) + " : " + levels2[i]);
	////		}






	//		AICScore aic = new AICScore();
	//		BICScore bic = new BICScore();
	//		SumOfSquaredErrors sse = new SumOfSquaredErrors();
	//
	//		double aicScore2 = aic.score(clust2);
	//		double bicScore2 = bic.score(clust2);
	//		double sseScore2 = sse.score(clust2);
	//		IJ.log(" ");
	//		IJ.log("clust2 ");
	//		for (int i=0; i<2; i++) {  
	//			//Instance inst =DatasetTools.minAttributes(data2[i]);
	//			Instance inst =DatasetTools.average(clust2[i]);
	//			IJ.log("clust " + inst.value(0));
	//		}
	//
	//		double aicScore3 = aic.score(clust3);
	//		double bicScore3 = bic.score(clust3);
	//		double sseScore3 = sse.score(clust3);
	//
	//		IJ.log(" ");
	//		IJ.log("clust3 ");
	//		for (int i=0; i<3; i++) {  
	//			//Instance inst =DatasetTools.minAttributes(data2[i]);
	//			Instance inst =DatasetTools.average(clust3[i]);
	//			IJ.log("clust " + inst.value(0));
	//		}
	//
	//		double aicScore4 = aic.score(data2);
	//		double bicScore4 = bic.score(data2);
	//		double sseScore4 = sse.score(data2);
	//
	//		IJ.log(" ");
	//		IJ.log("clust4 ");
	//		for (int i=0; i<4; i++) {  
	//			//Instance inst =DatasetTools.minAttributes(data2[i]);
	//			Instance inst =DatasetTools.average(data2[i]);
	//			IJ.log("clust " + inst.value(0));
	//		}
	//		//	        
	//		//	        double aicScore5 = aic.score(data22);
	//		//	        double bicScore5 = bic.score(data22);
	//		//	        double sseScore5 = sse.score(data22);
	//
	//		int ibest =2;
	//		if(bic.compareScore(bicScore2, bicScore3))
	//		{
	//			if(bic.compareScore(bicScore3, bicScore4))	
	//				ibest=4;
	//			else
	//				ibest=3;
	//		}
	//		else{
	//			if(bic.compareScore(bicScore2, bicScore4))
	//				ibest=4;
	//		}
	//
	//		IJ.log("ibest " + ibest);
	//		IJ.log("AIC score: " + aicScore2+"\t"+aicScore3+ "\t"+aicScore4);
	//		IJ.log("BIC score: " + bicScore2+"\t"+bicScore3+ "\t"+bicScore4);
	//		IJ.log("Sum of squared errors: " + sseScore2+"\t"+sseScore3+ "\t"+sseScore4);


	//	}


	private void fill_mask(Region r){

		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){  
				for (int j=0;j< sy; j++){  
					this.mask[0][z][i][j] = 0;	
				}
			}
		}

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			int rz,rx,ry;
			rz=os*(p.pz-offsetz);
			rx=os*(p.px-offsetx);
			ry=os*(p.py-offsety);
			for (int z=rz; z<rz+osz; z++){
				for (int i=rx;i<rx+os; i++){  
					for (int j=ry;j< ry+os; j++){  
						this.mask[0][z][i][j] = 1;	
					}
				}
			}
			//this.mask[0][os*(p.pz-offsetz)][os*(p.px-offsetx)][os*(p.py-offsety)]=1;
		}

	}


	private void voronoi_mask(Region r){
		this.weights= new double [sz][sx][sy];
		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){  
				for (int j=0;j< sy; j++){  
					this.weights[z][i][j] = 0;	
				}
			}
		}

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			int rz,rx,ry;
			rz=os*(p.pz-offsetz);
			rx=os*(p.px-offsetx);
			ry=os*(p.py-offsety);
			if(rz<0 || rz+osz>sz ||rx<0 || rx+os>sx ||ry<0 || ry+os>sy)
				continue;

			for (int z=rz; z<rz+osz; z++){//for (int z=rz; z<rz+osz; z++){
				for (int i=rx;i<rx+os; i++){  
					for (int j=ry;j< ry+os; j++){  
						this.weights[z][i][j] = 1;	
					}
				}
			}
			//this.mask[0][os*(p.pz-offsetz)][os*(p.px-offsetx)][os*(p.py-offsety)]=1;
		}

	}

	private void fill_weights(Region r){
		this.weights= new double [sz][sx][sy];

		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){  
				for (int j=0;j< sy; j++){  
					this.weights[z][i][j] = 0;	
				}
			}
		}

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			int rzmin,rxmin,rymin;
			int rzmax,rxmax,rymax;
			//IJ.log("pixel px " + p.px + " py " + p.py);
			if(sz>1){
				rzmin=Math.max(0, os*(p.pz-offsetz)-wmar);
				rzmax=Math.min(sz, rzmin+osz+2*wmar);
			}else{
				rzmin=0;
				rzmax=1;
			}

			rxmin=Math.max(0, os*(p.px-offsetx) -wmar);
			rymin=Math.max(0, os*(p.py-offsety) -wmar);

			rxmax=Math.min(sx, rxmin+os+2*wmar);
			rymax=Math.min(sy, rymin+os+2*wmar);

			for (int z=rzmin; z<rzmax; z++){
				for (int i=rxmin;i<rxmax; i++){  
					for (int j=rymin;j< rymax; j++){  
						this.weights[z][i][j] = 1;	
					}
				}
			}
		}
	}


	private void fill_refined_mask(Region r){// a corriger comme fill weights pour l'utiliser eventuellement
		this.refined_mask= new double [sz][sx][sy];

		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){  
				for (int j=0;j< sy; j++){  
					this.refined_mask[z][i][j] = 0;	
				}
			}
		}

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			int rzmin,rxmin,rymin;
			int rzmax,rxmax,rymax;

			if(sz>1){
				rzmin=Math.max(0, os*(p.pz-offsetz)-rmmar);
				rzmax=Math.min(sz, rzmin+osz+rmmar);
			}else{
				rzmin=0;
				rzmax=1;
			}

			rxmin=Math.max(0, os*(p.px-offsetx) -rmmar);
			rymin=Math.max(0, os*(p.py-offsety) -rmmar);

			rxmax=Math.min(sx, rxmin+os+rmmar);
			rymax=Math.min(sy, rymin+os+rmmar);

			for (int z=rzmin; z<rzmax; z++){
				for (int i=rxmin;i<rxmax; i++){  
					for (int j=rymin;j< rymax; j++){  
						this.refined_mask[z][i][j] = 1;	
					}
				}
			}
		}
	}


	private double find_best_thresh(double [][][] w3kbest){
		double t;
		double energy=Double.MAX_VALUE;
		double threshold=0.1;
		double temp;
		//	IJ.log("lreg ap " + p.lreg);
		for (t=0.95;t> mint; t-=0.05){  
			set_object(w3kbest, t);

			if(p.nz==1){
				temp=Tools.computeEnergyPSF(temp1[0], object, temp2[0], temp3[0],
						p.ldata, p.lreg,p,cout_front,cin,patch);
			}
			else{
				temp=Tools.computeEnergyPSF3D(temp1[0], object, temp2[0], temp3[0],
						p.ldata, p.lreg,p,cout_front,cin,patch);	
			}

			if(p.debug)	
				IJ.log("energy " + temp + " t " + t + "region"+ r.value);
			if (temp<energy) {energy=temp;threshold=t; }

		}
		return threshold;


	}


	public void assemble_result_voronoi2(){

		//IJ.log("assemble vo2");
		//build regions refined (for interpolated regions)
		//			if(interpolation==1){
		//				//IJ.log("no interpolation");
		//				for (int z=0; z<sz; z++){
		//					for (int i=0;i<sx; i++){  
		//						for (int j=0;j<sy; j++){  
		//							if (object[z][i][j]==1){
		//								regions_refined[z+offsetz*osz][i+offsetx*os][j+offsety*os] = r.value;
		//								//if(ap.r.value ==3){IJ.log("z" + z +" i" +i+" j" +j);
		//								//IJ.log("offsetz x  y " + ap.offsetz +" " + ap.offsetx +" " + ap.offsety+ "osz xy " + osz +" " + osxy);}
		//							}
		//						}
		//					}
		//				}
		//			}
		//			else{
		for (int z=0; z<isz; z++){
			for (int i=0;i<isx; i++){  
				for (int j=0;j< isy; j++){  
					if (object[z][i][j]==1){
						regions_refined[z+offsetz*fsz][i+offsetx*fsxy][j+offsety*fsxy]=r.value;

					}
				}

				//					else
				//						regions_refined[z+ap.offsetz*osz][i+ap.offsetx*osxy][j+ap.offsety*osxy] =0;
			}
		}
		//	}

		if(p.livedisplay){
			impa.displayRegions(impa.regions_refined, impa.sx, impa.sy, impa.sz, impa.channel, true, false, true);
		}
		//IJ.log("assemble done");
	}

	//build local object list
	private void assemble_patch(){

		//IJ.log("find regions vo 2");
		//find connected regions
		ImagePlus maska_im= new ImagePlus();
		ImageStack maska_ims= new ImageStack(isx,isy);

		for (int z=0; z<isz; z++){  
			byte[] maska_bytes = new byte[isx*isy];
			for (int i=0; i<isx; i++){  
				for (int j=0;j< isy; j++){  
					if(object[z][i][j]>=1){maska_bytes[j * isx + i] = (byte) (object[z][i][j]);}
					else
					{maska_bytes[j * isx + i] = (byte) 0;}
				}
			}
			ByteProcessor bp = new ByteProcessor(isx, isy);
			bp.setPixels(maska_bytes);
			maska_ims.addSlice("", bp);
		}

		maska_im.setStack("test Mask vo2",maska_ims);
		//maska_im.show();

		FindConnectedRegions fcr= new FindConnectedRegions(maska_im, isx,isy,isz);//maska_im only

		double thr=0.5;

		float [][][]	Ri = new float [isz][isx][isy];
		for (int z=0; z<isz; z++){
			for (int i=0; i<isx; i++) {  
				for (int j=0; j<isy; j++) {  
					Ri[z][i][j]= (float)thr;
				}
			}
		}

		fcr.run(thr,channel,isx*isy*isz,3*fsxy,0,Ri,false,false);//min size was 5//5*osxy
		//fcr.run(d,0,p.maxves_size,p.minves_size,255*p.min_intensity,Ri,true,p.save_images&&(!p.refinement));

		//add to list with ciritical section
		impa.addRegionstoList(fcr.results);

		//add to regions refined with correct indexes
		assemble(fcr.results);
		//regions_refined=fcr.tempres;

	}



	private void assemble(ArrayList<Region> localList){

		for (Iterator<Region> it = localList.iterator(); it.hasNext();) {
			ArrayList<Pix> npixels = new ArrayList<Pix>();
			Region r = it.next();

			for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
				Pix v = it2.next();
				npixels.add(new Pix(v.pz+offsetz*fsz,v.px+offsetx*fsxy,v.py+offsety*fsxy));
				//count number of free edges
				regions_refined[v.pz+offsetz*fsz][v.px+offsetx*fsxy][v.py+offsety*fsxy]=r.value;

			}				
			r.pixels=npixels;
		}

	}


	//interpolation
	private void build_interpolated_object(double [][][] cmask, double t){
		ImagePlus iobject= new ImagePlus();
		ImageStack iobjectS;
		interpolated_object = new double [isz][isx][isy];

		//build imageplus form non interpolated object
		iobjectS=new ImageStack(sx,sy);

		for (int z=0; z<sz; z++){
			float[] twoD_float = new float[sx*sy];
			for (int i=0; i<sx; i++) {
				for (int j=0; j<sy; j++) {  
					twoD_float[j * sx + i]= (float) cmask[z][i][j];
				}
			}
			FloatProcessor fp = new FloatProcessor(sx, sy);
			fp.setPixels(twoD_float);
			iobjectS.addSlice("", fp);
		}
		iobject.setStack("Object x",iobjectS);

		//todo : check for 3D
		//do interpolation
		//IJ.run(iobject, "Size...", "width="+isx+" height="+isy+" interpolation=Bilinear");
		//iobject.show();

		Resizer re = new Resizer();
		//IJ.log("isz" + isz);
		if(isz!=sz){
			iobject=re.zScale(iobject, isz, ImageProcessor.BILINEAR);
		}
		//			//img.duplicate().show();
		ImageStack imgS2=new ImageStack(isx,isy);
		for (int z=0; z<isz; z++){
			iobject.setSliceWithoutUpdate(z+1);
			//img.setProcessor(img.getProcessor().resize(di, dj, false));
			iobject.getProcessor().setInterpolationMethod(ImageProcessor.BILINEAR);
			imgS2.addSlice("",iobject.getProcessor().resize(isx, isy, true));
		}
		iobject.setStack(imgS2);


		//put data into interpolted object
		ImageProcessor imp;
		for (int z=0; z<isz; z++){
			iobject.setSlice(z+1);
			imp=iobject.getProcessor();
			for (int i=0; i<isx; i++){  
				for (int j=0;j< isy; j++){  
					interpolated_object[z][i][j]=  (double) Float.intBitsToFloat(imp.getPixel(i,j));
				}	
			}
		}
		//iobject.show();

		//threshold 
		for (int z=0; z<isz; z++){
			for (int i=0;i<isx; i++){  
				for (int j=0;j< isy; j++){  
					if(interpolated_object[z][i][j]>t && weights[z/interpolationz][i/interpolation][j/interpolation]==1)
						this.interpolated_object[z][i][j] = 1;
					else
						this.interpolated_object[z][i][j] = 0;
				}
			}
		}
		//this.object=interpolated_object;
		//MasksDisplay md= new MasksDisplay(isx,isy,isz,2,p.cl,p);
		//md.display2regionsnew(interpolated_object[0],"iobject ici" +r.value, channel);


	}

}
