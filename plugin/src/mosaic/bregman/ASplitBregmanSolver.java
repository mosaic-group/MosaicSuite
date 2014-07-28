package mosaic.bregman;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import mosaic.bregman.FindConnectedRegions.Region;

public class ASplitBregmanSolver {
	Tools LocalTools;
	public DoubleDCT_2D dct2d;
	double totaltime=0;
	boolean StopFlag;



	public boolean [][][] tmask;
	public boolean [][][] tmask_previous;

	public ByteProcessor bp_watermask;
	public ArrayList<Region> regionsvoronoi;
	public ArrayList<Region> regionslistr;
	public MasksDisplay md;
	public double [] [] [] image;
	public double weight;
	public double norm;
	public double [] [] [] [] speedData; //used only
	public double energt;
	//public double [] [] [] [] mask;

	public double [] [] eigenLaplacian;

	int stepk;

	public int channel;
	public double [] [] [] [] w1k;
	public double [] [] [] [] w3k;

	public double [] [] [] [] w2xk;
	public double [] [] [] [] w2yk;

	public double [] [] [] [] w3kbest;
	public int iw3kbest;
	public double [] [] [] [] b2xk;
	public double [] [] [] [] b2yk;

	public double [] [] []  RSSweights;

	public double [] [] [] [] b1k;
	public double [] [] [] [] b3k;

	public int  [] [] [] maxmask;
	public int  [] [] [] maxmask0;


	//public double [] [] [] [] w3kp; // used for norm calculation : not used now

	//public double [] [] [] [] divwb;
	//public double [] [] [] [] RHS;

	public double [] [] [] [] temp1;
	public double [] [] [] [] temp2;
	public double [] [] [] [] temp3;
	public double [] [] [] [] temp4;

	public float [] [] [] [] Ri;
	public float [] [] [] [] Ro;

	public double [] energytab;
	public double [] normtab;
	int ni, nj, nz;
	int nl;
	public double energy, lastenergy;
	public double bestNrj;
	public Parameters p;
	public RegionStatisticsSolver RSS;
	public AnalysePatch Ap;

	public ASplitBregmanSolver(Parameters params,
			double [] [] [] image, double [] [] [] [] speedData, double [] [] [] [] mask,
			MasksDisplay md, int channel, AnalysePatch ap){
		this(params,image,speedData,mask,md,channel);
		this.Ap=ap;
	}


	public ASplitBregmanSolver(Parameters params,
			double [] [] [] image, double [] [] [] [] speedData, double [] [] [] [] mask,
			MasksDisplay md, int channel){
		//initialization

		this.LocalTools= new Tools(params.ni,params.nj,params.nz);
		this.channel=channel;
		bestNrj=Double.MAX_VALUE;
		this.p= params;
		this.ni=params.ni ;
		this.nj=params.nj ;
		this.nz=params.nz ;

		this.nl=p.nlevels;

		this.energytab= new double [nl];
		this.normtab= new double [nl];

		this.StopFlag=false;
		this.md= md;
		//IJ.log("nlevels asplit" + p.nlevels);
		dct2d= new DoubleDCT_2D(ni,nj);
		//IJ.log("nlevels asplit" + p.nlevels);
		//speedData used as temp tab

		//if(nz>1)dct3d= new DoubleDCT_3D(nz,ni,nj);
		this.image=image;
		this.speedData=speedData;//used only for NRegions and two regions without PSF
		//this.mask=mask;


		this.eigenLaplacian= new double [ni] [nj];

		//allocate
		this.w1k= new double [nl] [nz] [ni] [nj];
		this.w3k= new double [nl] [nz] [ni] [nj];
		this.w3kbest= new double [nl] [nz] [ni] [nj];
		//this.w3kp= new double [nl] [nz] [ni] [nj];

		this.tmask= new boolean [nz][ni][nj];
		this.tmask_previous= new boolean [nz][ni][nj];

		this.b2xk= new double [nl] [nz] [ni] [nj];
		this.b2yk= new double [nl] [nz] [ni] [nj];

		this.b1k= new double [nl] [nz] [ni] [nj];
		this.b3k= new double [nl] [nz] [ni] [nj];

		this.w2xk= new double [nl] [nz] [ni] [nj];
		this.w2yk= new double [nl] [1] [ni] [nj];//save memory w2yk not used in 3d case

		this.Ri= new float [nl] [nz] [ni] [nj];
		this.Ro= new float [nl] [nz] [ni] [nj];

		this.maxmask= new int  [nz] [ni] [nj];
		this.maxmask0= new int [nz] [ni] [nj];

		int nzmin;
		if(nz>1){nzmin=Math.max(7, nz);}
		else nzmin=nz;

		int nimin=Math.max(7, ni);
		int njmin=Math.max(7, nj);

		this.temp1= new double [nl] [nzmin] [nimin] [njmin];
		//IJ.log(" ni " + ni +" nj" +nj+ " nl " +nl+ " nzmin "+nzmin);
		this.temp2= new double [nl] [nzmin] [nimin] [njmin];
		this.temp3= new double [nl] [nzmin] [nimin] [njmin];
		this.temp4= new double [nl] [nzmin] [nimin] [njmin];// hack to make it work : used for eigenPSF 7*7*7

		//temp4, temp5, speedData
		this.RSS= new RegionStatisticsSolver(temp1[0],temp2[0], temp3[0],image,10,p);

		//precompute eigenlaplacian
		for (int i=0; i<ni; i++)
		{  
			for (int j=0;j< nj; j++)
			{
				this.eigenLaplacian[i][j]=2+(2-2*Math.cos((j)*Math.PI/(nj))+ (2-2*Math.cos((i)*Math.PI/(ni))));
			}	
		}

		for(int i =0; i< nl;i++)
		{
			//temp1=w2xk temp2=w2yk
			LocalTools.fgradx2D(temp1[i], mask[i]);
			LocalTools.fgrady2D(temp2[i], mask[i]);

			LocalTools.copytab(w1k[i], mask[i]);
			LocalTools.copytab(w3k[i], mask[i]);
		}

		if(p.RSSinit)
		{
			RSS.eval(w3k[0]);
			//Analysis.p.cl[0]=RSS.betaMLEout;
			//Analysis.p.cl[1]=RSS.betaMLEin;

			p.cl[0]=RSS.betaMLEout;
			p.cl[1]=RSS.betaMLEin;

			IJ.log(String.format("Photometry init:%n background %7.2e %n foreground %7.2e", RSS.betaMLEout,RSS.betaMLEin));	
		}

		if(p.remask){
			LocalTools.createmask(mask,image,p.cl);
			md.display2regionsnewd(mask[0][0], "remask init", 0);;

		}


		for(int l =0; l< nl;l++){
			for (int z=0; z<nz; z++){
				for (int i=0; i<ni; i++) {  
					for (int j=0; j<nj; j++) {  
						//						Ro[l][z][i][j]=(float) (Analysis.p.cl[0]);
						//						Ri[l][z][i][j]=(float) (Analysis.p.cl[1]);
						Ro[l][z][i][j]=(float) (p.cl[0]);
						Ri[l][z][i][j]=(float) (p.cl[1]);
					}	
				}
			}
		}

		//RSS.eval(w3k[0]);
		//IJ.log(String.format("Photometry init :%n backgroung %7.2e %n foreground %7.2e", RSS.betaMLEout,RSS.betaMLEin));	

	}


	//first run
	public void first_run() throws InterruptedException {



		//initialize variables
		for(int l =0; l< nl;l++)
		{
			for (int z=0; z<nz; z++)
			{
				for (int i=0; i<ni; i++) 
				{  
					for (int j=0; j<nj; j++) 
					{
						b2xk[l][z][i][j]=0;b2yk[l][z][i][j]=0;
						b1k[l][z][i][j]=0;b3k[l][z][i][j]=0;
					}	
				}
			}
		}


		stepk=0;
		totaltime=0;
		int modulo=p.dispEmodulo;

		if(p.firstphase){
			IJ.showStatus("Computing segmentation");
			IJ.showProgress(0.0);
		}
		//IJ.log("max steps" + p.max_nsb);

		double lastenergy_mod = Double.MAX_VALUE;
		
		while(stepk<p.max_nsb  && !StopFlag){
			//Bregman step
			step();

			//energy=500-stepk;
			//stop criterion

			//			if(p.firstphase && stepk % p.energyEvaluationModulo ==0){
			//				//swap masks
			//				boolean [][][] temp = tmask_previous;
			//				tmask_previous=tmask;
			//				tmask=temp;
			//				compute_binaryMask(w3k[0], tmask);
			//				int diff=compare_binaryMask(tmask, tmask_previous);
			//				IJ.log("difference step "+ stepk +": " + diff);
			////				if(stepk==10 || stepk==11){md.display2regionsnewd(w3k[0][0], "w3k"+stepk, 0);
			////				md.display2regionsnewboolean(tmask[0], "mask"+stepk,0);
			////				}
			//			}

			if(energy < bestNrj) 
			{
				LocalTools.copytab(w3kbest[0], w3k[0]);
				iw3kbest=stepk;
				bestNrj=energy;
			}
			if(stepk % modulo ==0 || stepk==p.max_nsb -1)
			{	
				//IJ.log(String.format("Ediff %d : %7.6e", stepk, Math.abs((energy-lastenergy)/lastenergy)));	
				if(Math.abs((energy-lastenergy)/lastenergy) < p.tol){
					StopFlag=true;if(p.livedisplay && p.firstphase){IJ.log("energy stop");}}
				
				// experiment to speedup we stop if the energy increase after evaluation modulo step
				
				if (p.firstphase == true)
				{
					if (stepk % modulo == 0 && p.fastsquassh == true)
					{
						if ((energy - lastenergy_mod) > 0)
							StopFlag = true;
						else
							lastenergy_mod = energy;
					}
				}
					
			}
			lastenergy=energy;
			//energy output
			//if(p.livedisplay){

			if(stepk % modulo ==0 && p.livedisplay && p.firstphase)
				IJ.log(String.format("Energy at step %d : %7.6e", stepk, energy));
				//if(p.debug)IJ.log("cout " + p.cl[1]);
			//IJ.log("Energy at step :" + stepk+ " : " +Tools.round(energy,3));

			//IJ.log("Best energy step: " + stepk + " : " + Tools.round(bestNrj , 3));+ " norm between masks " + Tools.round(norm, 2));
			//}
			if((stepk+1) % p.RSSmodulo ==0 && stepk!=0)
			{
				RSS.eval(w3k[0]);
				//Analysis.p.cl[0]=RSS.betaMLEout;
				//Analysis.p.cl[1]=RSS.betaMLEin;
				p.cl[0]=RSS.betaMLEout;
				p.cl[1]=RSS.betaMLEin;
				this.init();
				IJ.log(String.format("Photometry :%n backgroung %10.8e %n foreground %10.8e", RSS.betaMLEout,RSS.betaMLEin));	
			}

			if(!p.firstphase  && p.mode_intensity==0 && (stepk==40 || stepk==70) ){ // && new mode automatic intensity && p.mode_intensity==0 // do it all the time
				//Analysepat
				//Analysis.p.cl[0]=RSS.betaMLEout;
				//Analysis.p.cl[1]=RSS.betaMLEin;
				
				
				if(p.debug && stepk==40 && (Ap.r.value==91 || Ap.r.value==102)){
				MasksDisplay md= new MasksDisplay(Ap.sx,Ap.sy,Ap.sz,2,p.cl,p);
				md.display2regions3Dnew(w3k[0], "Mask Patch 40 "+Ap.r.value, channel);
				}
				
				if(p.debug && stepk==70 && (Ap.r.value==91 || Ap.r.value==102)){
					MasksDisplay md= new MasksDisplay(Ap.sx,Ap.sy,Ap.sz,2,p.cl,p);
					md.display2regions3Dnew(w3k[0], "Mask Patch 70 "+Ap.r.value, channel);
					}
				
				
				
				Ap.find_best_thresh_and_int(w3k[0]);
				p.cl[0]=Math.max(0, Ap.cout);
				p.cl[1]=Math.max(0.75*(Ap.firstminval- Ap.intmin)/(Ap.intmax-Ap.intmin), Ap.cin);//lower bound withg some margin
				//p.cl[1]=Ap.cin;
				this.init();
				if(p.debug){
					IJ.log("region" + Ap.r.value +" pcout" + p.cl[1] );
					IJ.log("region" + Ap.r.value + String.format(" Photometry :%n backgroung %10.8e %n foreground %10.8e", Ap.cout, Ap.cin));}	
			}

			if(p.RegionsIntensitymodulo ==stepk && stepk!=0){
				IJ.log("best energy at"+iw3kbest);
				this.regions_intensity(w3kbest[0]);
				this.init();

			}

			stepk++;
			if(stepk % modulo ==0 &&p.firstphase)IJ.showStatus("Computing segmentation  " + LocalTools.round((((double) 50* stepk)/(p.max_nsb-1)),2) + "%");


			if(p.firstphase)
				IJ.showProgress(0.5*((double) stepk)/(p.max_nsb-1));
		}
		
		if(iw3kbest<50) { // use what iteration threshold  ?
			int iw3kbestold=iw3kbest;
			LocalTools.copytab(w3kbest[0], w3k[0]);
			iw3kbest=stepk-1;
			bestNrj=energy;
			if(p.livedisplay && p.firstphase) IJ.log("Warning : increasing energy. Last computed mask is then used for first phase object segmentation." + iw3kbestold);
		}

		if (p.findregionthresh )
		{
			this.regions_intensity_findthresh(w3kbest[0]);
		}

		if(!p.mode_voronoi2)
			IJ.showStatus("Segmentation Done");
		//display combination of masks
		//if(p.nlevels==1 && p.nz==1) md.display2regionsnew(w3kbest[0][0], "Best Mask", channel);
		if(p.livedisplay && p.firstphase){if(p.nlevels<=2 && p.nz==1)  md.display2regions(w3kbest[0][0], "Mask", channel);}
		if(p.livedisplay && p.nz>1 &&p.nlevels<=2 && p.firstphase) md.display2regions3D(w3kbest[0], "Mask", channel);

		//if(p.nz>1) md.display2regions3D(w3k[0], "Mask");
		////if(p.nz>1) md.display2regions3Dscaled(w3k[0], "Mask scaled");
		//if(p.nz>1) md.display2regions3Dscaledcolor(w3k[0], "Mask scaled");
		if(p.livedisplay){if(p.nlevels>2)md.display(maxmask, "Masks");}
		if(p.livedisplay && p.firstphase){
			IJ.log("Best energy : " + LocalTools.round(bestNrj , 3)+ ", found at step " + iw3kbest);
			IJ.log("Total phase one time: " + totaltime/1000 + "s");}
	}



	public  void compute_binaryMask(double [][][] softmask, boolean [][][]  tmask){


		for (int z=0; z<nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					if(softmask[z][i][j]>p.min_intensity) tmask[z][i][j]=true;
					else tmask[z][i][j]=false;
				}
			}
		}
	}

	public  int compare_binaryMask(boolean [][][] tmask1, boolean [][][]  tmask2){

		int cpt=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					if(tmask1[z][i][j]!=tmask2[z][i][j]) cpt++;
				}
			}
		}

		return cpt;
	}


	public void run() throws InterruptedException{
		stepk=0;
		while(stepk< p.max_nsb){
			step();
			if(energy < bestNrj) bestNrj=energy;
			stepk++;
		}
	}



	protected void step() throws InterruptedException {
		long lStartTime = new Date().getTime(); //start time
		//energy=0;

		//-- uk sub-problem
		//IJ.log("creates latch ");
		CountDownLatch RegionsTasksDoneSignal = new CountDownLatch(nl);//subprob 1 and 3
		CountDownLatch UkDoneSignal = new CountDownLatch(nl);
		CountDownLatch W3kDoneSignal = new CountDownLatch(1);

		for(int l=0; l< nl;l++){
			new Thread(new SingleRegionTask(RegionsTasksDoneSignal,UkDoneSignal,W3kDoneSignal, l, channel,this, LocalTools)).start();
		}


		//%-- w3k subproblem		
		UkDoneSignal.await(); 

		ProjectSimplexSpeed.project(w3k, temp4, ni,nj,nl);

		W3kDoneSignal.countDown();
		RegionsTasksDoneSignal.await(); 

		LocalTools.max_mask(maxmask, w3k);

		// number of != pixels in max mask (stop criterion ?)
		//int diff;
		//diff=Tools.computediff(maxmask, maxmask0);
		//IJ.log("diff in pixels : " +  diff);
		//Tools.copytab(maxmask0, maxmask);

		//doneSignal2.await();
		norm=0;
		energy=0;
		for(int l=0; l< nl;l++){
			energy+=energytab[l];
			norm=Math.max(norm, normtab[l]);
		}
		
		if(p.livedisplay) md.display(maxmask, "Masks");

		//if(p.livedisplay) md.display2regions(w3k[1][0], "Mask lev 1",0);




		long lEndTime = new Date().getTime(); //end time

		long difference = lEndTime - lStartTime; //check different
		totaltime +=difference;
		//IJ.log("Elapsed milliseconds: " + difference);


	}

	public void init(){
		if(p.debug)IJ.log("init super");
	}

	public void compute_eigenPSF(){
	}

	public void regions_intensity(double [][][] mask){
		//short [] [] [] regions;
		//  ArrayList<Region> regionslistr;
		double thresh=0.4;

		ImagePlus mask_im= new ImagePlus();
		ImageStack mask_ims= new ImageStack(p.ni,p.nj);
		for (int z=0; z<nz; z++){
			byte[] mask_bytes = new byte[p.ni*p.nj];
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					if(mask[z][i][j]> thresh)
						mask_bytes[j * p.ni + i]= 0;
					else
						mask_bytes[j * p.ni + i]=(byte) 255;
				}
			}

			ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
			bp.setPixels(mask_bytes);
			mask_ims.addSlice("", bp);

		}
		mask_im.setStack("Regions",mask_ims);

		IJ.run(mask_im,"Voronoi","");
		IJ.run(mask_im,"Invert","");
		IJ.run(mask_im,"3-3-2 RGB", "");
		mask_im.show("Voronoi");



		double thr=254;
		FindConnectedRegions fcr= new FindConnectedRegions(mask_im);

		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					Ri[0][z][i][j]=(float)thr;
				}
			}
		}

		fcr.run(thr,0,512*512,2,0,Ri[0],false,false);

		//		IJ.selectWindow("Test Voronoi");
		//		IJ.setThreshold(0, 254);
		//		IJ.run("Convert to Mask");
		//		ImagePlus imp1 = IJ.getImage();
		//		IJ.selectWindow("Mask X");
		//		IJ.run("8-bit");
		//		ImagePlus imp2 = IJ.getImage();
		//		ImageCalculator ic = new ImageCalculator();
		//		ImagePlus imp3 = ic.run("Add", imp1, imp2);
		//		IJ.run(imp3, "Invert LUT", "");
		//		//imp3.show();
		//		IJ.selectWindow("Mask X");

		//regions=fcr.tempres;
		this.regionslistr=fcr.results;
		int na=regionslistr.size();

		double total=Analysis.totalsize(regionslistr);
		IJ.log(na + " Voronoi1 cells found, total area : " + LocalTools.round(total,2)+ " pixels.");

		////////corect  Ri and Ro not double	RSS.eval(w3k[0], Ri[0], Ro[0], regionslistr);
		//	ImagePlus img=md.display2regionsnew(Ri[0][0], "Ri", 1);
		//	ImagePlus img2=md.display2regionsnew(Ro[0][0], "Ro", 1);

		RSS.cluster_region(Ri[0], Ro[0], regionslistr);
//		ImagePlus img3=
				md.display2regionsnew(Ri[0][0], "Ri cluster", 1);
//		ImagePlus img4=
				md.display2regionsnew(Ro[0][0], "Ro cluster", 1);	
	}	

	public void regions_intensity_findthresh(double [][][] mask){
		//short [] [] [] regions;
		ArrayList<Region> regionslist;
		double thresh;
		if(channel==0)
			thresh=p.regionthresh;//0.19
		else
			thresh=p.regionthreshy;
		//IJ.log("thresh" + thresh);
		ImagePlus mask_im= new ImagePlus();
		ImageStack mask_ims= new ImageStack(p.ni,p.nj);


		//construct mask as an imageplus
		for (int z=0; z<nz; z++)
		{
			float[] mask_float = new float[p.ni*p.nj];
			for (int i=0; i<ni; i++) 
			{
				for (int j=0; j<nj; j++) 
				{
					mask_float[j * p.ni + i]= (float) mask[z][i][j];
				}
			}
			FloatProcessor fp = new FloatProcessor(p.ni, p.nj);
			fp.setPixels(mask_float);
			mask_ims.addSlice("", fp);
		}
		mask_im.setStack("test",mask_ims);
		//mask_im.show("test");

		//project mask on single slice (maximum values)
		ZProjector proj = new ZProjector(mask_im);
		proj.setImage(mask_im);
		proj.setStartSlice(1);proj.setStopSlice(nz);
		proj.setMethod(ZProjector.MAX_METHOD);
		proj.doProjection();
		mask_im=proj.getProjection();
		IJ.showStatus("Computing segmentation  " + 52 + "%");
		IJ.showProgress(0.52);

		//IJ.log("thresh : " + thresh);
		//mask_im.duplicate().show();


		//IJ.run(mask_im, "Find Maxima...", "noise=0.1 output=[Segmented Particles]");
		//		MaximumFinder Mf= new MaximumFinder();
		//		ByteProcessor bp_water=Mf.findMaxima(mask_im.getProcessor(), 0.1, ImageProcessor.NO_THRESHOLD, MaximumFinder.SEGMENTED, false, false); 
		//		bp_watermask=bp_water;

		//threshold mask
		byte[] mask_bytes = new byte[p.ni*p.nj];
		for (int i=0; i<ni; i++) {
			for (int j=0; j<nj; j++) {
				//								if(i==361 && j==231)IJ.log("raw : " + mask_im.getProcessor().getPixelValue(i,j)
				//										+"convert int" + ((int)(255*mask_im.getProcessor().getPixelValue(i,j))) 
				//										+"convert byte" + ((byte)  ((int)(255*mask_im.getProcessor().getPixelValue(i,j))))
				//										+"convert float" + ((float) ( ((int)(255*mask_im.getProcessor().getPixelValue(i,j)))))
				//										+ "thresh " + (255*thresh)
				//												);
				if(  (float)  ( (int)  (255*mask_im.getProcessor().getPixelValue(i,j))  ) > 255*thresh) 
					//weird conversion to have same thing than in find connected regions
					mask_bytes[j * p.ni + i]= 0;
				else
					mask_bytes[j * p.ni + i]=(byte) 255;
			}
		}
		ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
		bp.setPixels(mask_bytes);
		mask_im.setProcessor("Voronoi",bp);
		//mask_im.show();
		//mask_im.duplicate().show();

		//do voronoi in 2D on Z projection

		//		if(p.blackbackground){
		//			mask_im.getProcessor().invert();
		//		}

		//perform voronoi
		
		/* Here we compute the Voronoi segmentation starting from the threshold mask */
		
		EDM filtEDM = new EDM(); 
		filtEDM.setup("voronoi", mask_im); 
		filtEDM.run(mask_im.getProcessor()); 
		//  filtEDM.setup("final", m_objImpInp); 

		//IJ.run(mask_im,"Voronoi","");
		//IJ.run(mask_im,"Invert","");
		//		if(!p.blackbackground){
		mask_im.getProcessor().invert();
		//		}
		IJ.showStatus("Computing segmentation  " + 53 + "%");
		IJ.showProgress(0.53);
		//mask_im.duplicate().show();

		//expand Voronoi in 3D
		ImageStack mask_ims3= new ImageStack(p.ni,p.nj);
		for (int z=0; z<nz; z++){
			byte[] mask_bytes3 = new byte[p.ni*p.nj];
			for (int i=0; i<ni; i++) {
				for (int j=0; j<nj; j++) {  
					mask_bytes3[j * p.ni + i]= (byte) mask_im.getProcessor().getPixel(i,j);//
					//mask_bytes3[j * p.ni + i]= (byte) (bp_water.getPixel(i,j) & 0xFF);
				}
			}
			ByteProcessor bp3 = new ByteProcessor(p.ni, p.nj);
			bp3.setPixels(mask_bytes3);
			mask_ims3.addSlice("", bp3);
		}
		mask_im.setStack("Voronoi",mask_ims3);

		// Here we are elaborating the Voronoi mask to get a nice subdivision
		
//		mask_im.duplicate().show();
		double thr=254;
		FindConnectedRegions fcr= new FindConnectedRegions(mask_im);

		for (int z=0; z<nz; z++)
		{
			for (int i=0; i<ni; i++) 
			{
				for (int j=0; j<nj; j++) 
				{
					Ri[0][z][i][j]= (float)thr;
				}
			}
		}

		if(p.mode_voronoi2)
			fcr.run(thr,1,p.ni*p.nj*p.nz,0,0,Ri[0],false,false);//min size was 5
		else
			fcr.run(thr,1,p.ni*p.nj*p.nz,5,0,Ri[0],false,false);//min size was 5

		if(p.dispvoronoi){
			if(nz==1) md.display2regions(w3kbest[0][0], "Mask", channel);
			else md.display2regions3D(w3kbest[0], "Mask", channel);

			IJ.setThreshold(mask_im,0,254);
			IJ.run(mask_im,"Convert to Mask","stack");

			if(channel==0)
				IJ.selectWindow("Mask X");
			else
				IJ.selectWindow("Mask Y");
			IJ.run("8-bit","stack");
			ImagePlus imp2 = IJ.getImage();

			//add images
			ImageStack mask_ims2= new ImageStack(p.ni,p.nj);
			for (int z=0; z<nz; z++){
				//imp1.setSlice(z+1);
				imp2.setSlice(z+1);
				byte[] mask_byte2 = new byte[p.ni*p.nj];
				for (int i=0; i<ni; i++) {  
					for (int j=0; j<nj; j++) {  
						mask_byte2[j * p.ni + i]= (byte)
								Math.min((mask_im.getProcessor().getPixel(i,j) +
										imp2.getProcessor().getPixel(i,j)),255);
					}
				}
				ByteProcessor bp2 = new ByteProcessor(p.ni, p.nj);
				bp2.setPixels(mask_byte2);
				mask_ims2.addSlice("", bp2);
			}
			//replace imageplus with additon of both

			if(channel==0){
				mask_im.setStack("Voronoi X",mask_ims2);

				//IJ.run(imp3, "Invert LUT","stack");
				mask_im.show("Voronoi X");
				IJ.selectWindow("Mask X");}

			else{
				mask_im.setStack("Voronoi Y",mask_ims2);

				//IJ.run(imp3, "Invert LUT","stack");
				mask_im.show("Voronoi Y");
				IJ.selectWindow("Mask Y");
			}


		}


		//regions=fcr.tempres;
		regionslist=fcr.results;
		regionsvoronoi=regionslist;
		int na=regionslist.size();

		double total=Analysis.totalsize(regionslist);
		if(p.dispvoronoi)IJ.log(na + " Voronoi cells found, total area : " + LocalTools.round(total,2)+ " pixels.");

		//use Ri to store voronoi regions indices

		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					Ri[0][z][i][j]=255;
				}
			}
		}

		RegionStatisticsSolver RSS2;
		if(p.mode_voronoi2){
			RSS2= new RegionStatisticsSolver(temp1[0],temp2[0], temp3[0], image,10, p);
		}
		else
			RSS2= new RegionStatisticsSolver(temp1[0],temp2[0], temp3[0], w3kbest[0],10, p);
		//RSS.eval(w3k[0], Ri[0], Ro[0], regionslist);
		//ImagePlus img=md.display2regionsnew(Ri[0][0], "Ri", 1);
		//ImagePlus img2=md.display2regionsnew(Ro[0][0], "Ro", 1);

		long lStartTime = new Date().getTime(); //start time


		if(p.mode_voronoi2)
			RSS2.cluster_region_voronoi2(Ri[0], Ro[0], regionslist);
		else
			RSS2.cluster_region(Ri[0], Ro[0], regionslist);

		long lEndTime = new Date().getTime(); //end time

		IJ.showStatus("Computing segmentation  " + 54 + "%");
		IJ.showProgress(0.54);

		long difference = lEndTime - lStartTime; //check different
		totaltime +=difference;
		//IJ.log("Elapsed milliseconds RSS2: " + difference);
		if(p.dispvoronoi){
			if(p.nz==1)
			{md.display2regionsnew(Ri[0][0], "Regions thresholds", channel);}
			else
			{md.display2regions3Dnew(Ri[0], "Regions thresholds", channel);}	}
		//ImagePlus img4=md.display2regionsnew(Ro[0][0], "Ro cluster", 1);	
	}	

}
