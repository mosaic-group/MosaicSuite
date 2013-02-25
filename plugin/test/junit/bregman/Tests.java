package junit.bregman;
import java.util.concurrent.CountDownLatch;


import mosaic.bregman.*;
//import mosaic.bregman.Analysis;
//import bregman.NRegions;
//import mosaic.bregman.Tools;
//import bregman.TwoRegions;
import ij.IJ;
import ij.ImagePlus;

public class Tests {
	static Tools Tools;
	static double meanSA, meanLA;
	static double colocAB,colocABnumber,colocBA,colocBAnumber;
	
	public static double TestEnergy2DPSF(){

		//load image
		ImagePlus img=IJ.openImage("test_images/Rab4_HEK_br_jtest.tif");
		
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		//set parameters
		Analysis.p.removebackground=false;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.lreg=0.075;
		Analysis.p.betaMLEoutdefault=0.0003;//0.0027356;
		Analysis.p.betaMLEindefault=1;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.max_nsb=11;
		Analysis.p.livedisplay=false;


		//launch computation
		Tools Tools;
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}

		return Analysis.bestEnergyX;

	}


	public static void TestObjects2DPSF_oldmode(){
		//load image
		ImagePlus img=IJ.openImage("test_images/Rab4_HEK_br_jtest.tif");
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		//set parameters
		Analysis.p.mode_voronoi2=false;
		Analysis.p.mode_classic=true;
		Analysis.p.refinement=false;
		Analysis.p.max_nsb=201;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=false;
		Analysis.p.betaMLEoutdefault=0.003;//0.0027356;
		Analysis.p.betaMLEindefault=0.3;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=false;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=false;
		Analysis.p.subpixel= false;

		//do segmentation
		CountDownLatch DoneSignala = new CountDownLatch(1);
		new Thread(new TwoRegions(Analysis.imgA,Analysis.p,DoneSignala,0)).start();

		//wait for computation
		try {
			DoneSignala.await();
		}catch (InterruptedException ex) {}


		//return best energy
		//return Analysis.bestEnergyX;
	}


	
	public static void TestObjects2DPSF_firstphase(){
		//load image
		ImagePlus img=IJ.openImage("test_images/Rab4_HEK_br_jtest.tif");
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		//set parameters
		Analysis.p.mode_voronoi2=true;
		Analysis.p.mode_classic=false;
		Analysis.p.refinement=false;
		Analysis.p.max_nsb=151;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=false;
		Analysis.p.betaMLEoutdefault=0.0003;//0.0027356;
		Analysis.p.betaMLEindefault=1;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.min_intensity = 0.15;
		Analysis.p.min_intensityY = 0.15;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.minves_size=2;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=false;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=false;
		Analysis.p.subpixel= false;
		Analysis.p.regionthresh=Analysis.p.min_intensity;
		Analysis.p.regionthreshy=Analysis.p.min_intensityY;

		//do segmentation
		//launch computation
		Tools Tools;
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}
		

	}
	
	
	
	public static double TestObjects2DPSF_secondphase(){
		//load image
		ImagePlus img=IJ.openImage("test_images/Rab4_HEK_br_jtest.tif");
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		
		Tools Tools;
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;
		
		//set parameters
		Analysis.p.mode_voronoi2=true;
		Analysis.p.mode_classic=false;
		Analysis.p.refinement=true;
		Analysis.p.max_nsb=151;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=false;
		Analysis.p.betaMLEoutdefault=0.0003;//0.0027356;
		Analysis.p.betaMLEindefault=1;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.min_intensity = 0.15;
		Analysis.p.min_intensityY = 0.15;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.minves_size=2;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=true;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=false;
		Analysis.p.regionthresh=Analysis.p.min_intensity;
		Analysis.p.regionthreshy=Analysis.p.min_intensityY;
		Analysis.p.subpixel= false;
		Analysis.p.save_images=true;//necessary to compute object properties
		
		//do segmentation
		//launch computation
		//Tools Tools;
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}
		
		
		Analysis.na=Analysis.regionslistA.size();
		//IJ.log("mean size");
		Analysis.meana=Analysis.meansize(Analysis.regionslistA);

		double meanSA= Analysis.meansurface(Analysis.regionslistA);			

		
		
		return(meanSA);			

	}
	
	
	
	public static void TestObjects2DPSF_secondphase_subpixel(){
		//load image
		ImagePlus img=IJ.openImage("test_images/Rab4_HEK_br_jtest.tif");
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		
		
		//set parameters
		Analysis.p.mode_voronoi2=true;
		Analysis.p.mode_classic=false;
		Analysis.p.refinement=true;
		Analysis.p.max_nsb=151;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=false;
		Analysis.p.betaMLEoutdefault=0.0003;//0.0027356;
		Analysis.p.betaMLEindefault=1;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.min_intensity = 0.15;
		Analysis.p.min_intensityY = 0.15;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.minves_size=2;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=true;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=false;
		Analysis.p.regionthresh=Analysis.p.min_intensity;
		Analysis.p.regionthreshy=Analysis.p.min_intensityY;
		Analysis.p.subpixel= true;
		Analysis.p.oversampling2ndstep=2;
		Analysis.p.interpolation=4;
		Analysis.p.save_images=true;//necessary to compute object properties
		
		//do segmentation
		//launch computation
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}
		
		
		Analysis.na=Analysis.regionslistA.size();
		//IJ.log("mean size");
		Analysis.meana=Analysis.meansize(Analysis.regionslistA);

		meanSA= Analysis.meansurface(Analysis.regionslistA);			
		meanLA= Analysis.meanlength(Analysis.regionslistA);
		

	}
	
	
	public static void TestObjects2DPSF_secondphase_low(){
		//load image
		ImagePlus img=IJ.openImage("test_images/Rab4_HEK_br_jtest.tif");
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		
		
		//set parameters
		Analysis.p.mode_voronoi2=true;
		Analysis.p.mode_classic=false;
		Analysis.p.refinement=true;
		Analysis.p.max_nsb=151;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=false;
		Analysis.p.betaMLEoutdefault=0.0003;//0.0027356;
		Analysis.p.betaMLEindefault=1;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.min_intensity = 0.15;
		Analysis.p.min_intensityY = 0.15;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.minves_size=2;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=true;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=false;
		Analysis.p.regionthresh=Analysis.p.min_intensity;
		Analysis.p.regionthreshy=Analysis.p.min_intensityY;
		Analysis.p.subpixel= false;
		Analysis.p.oversampling2ndstep=1;
		Analysis.p.interpolation=1;
		Analysis.p.save_images=true;//necessary to compute object properties
		Analysis.p.mode_intensity=1;
		
		//do segmentation
		//launch computation
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}
		
		
		Analysis.na=Analysis.regionslistA.size();
		//IJ.log("mean size");
		Analysis.meana=Analysis.meansize(Analysis.regionslistA);

		meanSA= Analysis.meansurface(Analysis.regionslistA);			
		meanLA= Analysis.meanlength(Analysis.regionslistA);
		//IJ.log("na" + Analysis.na + "menS"+Analysis.meana+"meanSA"+meanSA +"meanLA"+meanLA);

	}
	
	
	
	public static void TestObjects2DPSF_secondphase_medium(){
		//load image
		ImagePlus img=IJ.openImage("test_images/Rab4_HEK_br_jtest.tif");
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		
		
		//set parameters
		Analysis.p.mode_voronoi2=true;
		Analysis.p.mode_classic=false;
		Analysis.p.refinement=true;
		Analysis.p.max_nsb=151;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=false;
		Analysis.p.betaMLEoutdefault=0.0003;//0.0027356;
		Analysis.p.betaMLEindefault=1;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.min_intensity = 0.15;
		Analysis.p.min_intensityY = 0.15;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.minves_size=2;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=true;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=false;
		Analysis.p.regionthresh=Analysis.p.min_intensity;
		Analysis.p.regionthreshy=Analysis.p.min_intensityY;
		Analysis.p.subpixel= false;
		Analysis.p.oversampling2ndstep=1;
		Analysis.p.interpolation=1;
		Analysis.p.save_images=true;//necessary to compute object properties
		Analysis.p.mode_intensity=2;
		
		//do segmentation
		//launch computation
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}
		
		
		Analysis.na=Analysis.regionslistA.size();
		//IJ.log("mean size");
		Analysis.meana=Analysis.meansize(Analysis.regionslistA);

		meanSA= Analysis.meansurface(Analysis.regionslistA);			
		meanLA= Analysis.meanlength(Analysis.regionslistA);
		IJ.log("na" + Analysis.na + "menS"+Analysis.meana+"meanSA"+meanSA +"meanLA"+meanLA);

	}
	
	
	
	public static void TestObjects2DPSF_secondphase_high(){
		//load image
		ImagePlus img=IJ.openImage("test_images/Rab4_HEK_br_jtest.tif");
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		
		
		//set parameters
		Analysis.p.mode_voronoi2=true;
		Analysis.p.mode_classic=false;
		Analysis.p.refinement=true;
		Analysis.p.max_nsb=151;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=false;
		Analysis.p.betaMLEoutdefault=0.0003;//0.0027356;
		Analysis.p.betaMLEindefault=1;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.min_intensity = 0.15;
		Analysis.p.min_intensityY = 0.15;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.minves_size=2;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=true;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=false;
		Analysis.p.regionthresh=Analysis.p.min_intensity;
		Analysis.p.regionthreshy=Analysis.p.min_intensityY;
		Analysis.p.subpixel= false;
		Analysis.p.oversampling2ndstep=1;
		Analysis.p.interpolation=1;
		Analysis.p.save_images=true;//necessary to compute object properties
		Analysis.p.mode_intensity=3;
		
		//do segmentation
		//launch computation
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}
		
		
		Analysis.na=Analysis.regionslistA.size();
		//IJ.log("mean size");
		Analysis.meana=Analysis.meansize(Analysis.regionslistA);

		meanSA= Analysis.meansurface(Analysis.regionslistA);			
		meanLA= Analysis.meanlength(Analysis.regionslistA);
		IJ.log("na" + Analysis.na + "menS"+Analysis.meana+"meanSA"+meanSA +"meanLA"+meanLA);

	}
	
	
	public static void TestColoc2DPSF(){
		//load image
		ImagePlus img=IJ.openImage("test_images/Rab5_2channels.tif");
		Analysis.load2channels(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		
		
		//set parameters
		Analysis.p.mode_voronoi2=true;
		Analysis.p.mode_classic=false;
		Analysis.p.refinement=true;
		Analysis.p.max_nsb=151;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=false;
		Analysis.p.betaMLEoutdefault=0.0003;//0.0027356;
		Analysis.p.betaMLEindefault=1;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.min_intensity = 0.15;
		Analysis.p.min_intensityY = 0.15;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.minves_size=2;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=true;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=false;
		Analysis.p.regionthresh=Analysis.p.min_intensity;
		Analysis.p.regionthreshy=Analysis.p.min_intensityY;
		Analysis.p.subpixel= false;
		Analysis.p.oversampling2ndstep=1;
		Analysis.p.interpolation=1;
		Analysis.p.save_images=true;//necessary to compute object properties
		
		//do segmentation
		//launch computation
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}
		
		Analysis.segmentb();			 

		try {
			Analysis.DoneSignalb.await();
		}catch (InterruptedException ex) {}
		
		
		colocAB=Tools.round(Analysis.colocsegAB(null, 0),4);

		colocABnumber = Tools.round(Analysis.colocsegABnumber(),4);
		
		colocBA=Tools.round(Analysis.colocsegBA(null, 0),4);
		
		colocBAnumber = Tools.round(Analysis.colocsegBAnumber(),4);
		//IJ.log("AB" + colocAB+"ABn"+colocABnumber +"BA"+colocBA +"BAn"+colocBAnumber);
		

		

	}
	
	public static void TestObjects3DPSF(){
		//load image
		ImagePlus img=IJ.openImage("test_images/C1-120412_HEK_Rabx_H2B4.lif - HEK_Rab4_wt_H2B4_5x_P13.tif");
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		
		
		//set parameters
		Analysis.p.mode_voronoi2=true;
		Analysis.p.mode_classic=false;
		Analysis.p.refinement=true;
		Analysis.p.max_nsb=151;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=true;
		Analysis.p.size_rollingball = 10;
		Analysis.p.betaMLEoutdefault=0.0003;//0.0027356;
		Analysis.p.betaMLEindefault=1;
		Analysis.p.sigma_gaussian=0.9;
		Analysis.p.zcorrec=1.125;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.15;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.min_intensity = 0.15;
		Analysis.p.min_intensityY = 0.15;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.minves_size=2;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=true;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=false;
		Analysis.p.regionthresh=Analysis.p.min_intensity;
		Analysis.p.regionthreshy=Analysis.p.min_intensityY;
		Analysis.p.subpixel= false;
		Analysis.p.oversampling2ndstep=1;
		Analysis.p.interpolation=1;
		Analysis.p.save_images=true;//necessary to compute object properties
		Analysis.p.mode_intensity=0;
		
		//do segmentation
		//launch computation
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}
		
		
		Analysis.na=Analysis.regionslistA.size();
		//IJ.log("mean size");
		Analysis.meana=Analysis.meansize(Analysis.regionslistA);

		meanSA= Analysis.meansurface(Analysis.regionslistA);			
		meanLA= Analysis.meanlength(Analysis.regionslistA);
		

	}
	
	

	
	//C1-120412_HEK_Rabx_H2B4.lif - HEK_Rab4_wt_H2B4_5x_P13




	public static double TestColocBA(){

		//load images
		Analysis.imgA=IJ.openImage("test_images/120227_COS_RABx_wt_LBPA_29_R9_P6_z0_ch00_o.tif");
		//Analysis.imgA=IJ.openImage("test_images/Rab4_HEK_br.tif");
		Analysis.p.ni=Analysis.imgA.getWidth();
		Analysis.p.nj=Analysis.imgA.getHeight();
		Analysis.p.nz=Analysis.imgA.getNSlices();

		Analysis.Tools= new Tools(Analysis.p.ni, Analysis.p.nj, Analysis.p.nz);

		Analysis.imgB=IJ.openImage("test_images/120227_COS_RABx_wt_LBPA_29_R9_P6_z0_ch01_o.tif");


		//set parameters

		Analysis.p.mode_voronoi2=false;
		Analysis.p.mode_classic=true;
		Analysis.p.refinement=false;
		Analysis.p.max_nsb=201;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=true;
		Analysis.p.betaMLEoutdefault=0.003;//0.0027356;
		Analysis.p.betaMLEindefault=0.3;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=1;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.usePSF=true;
		Analysis.p.RSSinit=false;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=true;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.maxves_size=500;
		Analysis.p.minves_size=5;
		Analysis.p.min_intensity = 0.15;
		Analysis.p.min_intensityY = 0.15;
		Analysis.p.min_intensityY=0.50;
		Analysis.p.colocthreshold=0.5;
		Analysis.p.usecellmaskX=false;
		Analysis.p.usecellmaskY=false;
		Analysis.p.looptest=false;
		Analysis.p.JunitTest=false;
		Analysis.p.regionSegmentLevel = 2;

		//do segmentation
		CountDownLatch DoneSignala = new CountDownLatch(1);
		new Thread(new TwoRegions(Analysis.imgA,Analysis.p,DoneSignala,0)).start();

		//wait for computation
		try {
			DoneSignala.await();
		}catch (InterruptedException ex) {}

		CountDownLatch DoneSignalb = new CountDownLatch(1);
		new Thread(new TwoRegions(Analysis.imgB,Analysis.p,DoneSignalb,1)).start();

		//wait for computation
		try {
			DoneSignalb.await();
		}catch (InterruptedException ex) {}



		Analysis.computeOverallMask();
		Analysis.regionslistA=Analysis.removeExternalObjects(Analysis.regionslistA);
		Analysis.regionslistB=Analysis.removeExternalObjects(Analysis.regionslistB);

		Analysis.na=Analysis.regionslistA.size();
		Analysis.nb=Analysis.regionslistB.size();

		return Analysis.colocsegBA(null,0); 




		//return best energy

	}


	public static double TestEnergy2D_5levelswoPSF(){

		//	//load image
		//	Analysis.imgA=IJ.openImage("test_images/Rab4_HEK_br.tif");
		//
		//	Analysis.p.ni=Analysis.imgA.getWidth();
		//	Analysis.p.nj=Analysis.imgA.getHeight();
		//	Analysis.p.nz=Analysis.imgA.getNSlices();
		//
		//	Analysis.Tools= new Tools(Analysis.p.ni, Analysis.p.nj, Analysis.p.nz,5);
		//	//pb mettre 5 levels partout : la ne marche pas

		//load image
		ImagePlus img=IJ.openImage("/Users/arizk/Documents/work/SCOL_utils/Matlab_test_files/Rab4_HEK_br_jtest.tif");
		Analysis.load1channel(img);

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		Analysis.p.ni=Analysis.imgA.getWidth();
		Analysis.p.nj=Analysis.imgA.getHeight();
		Analysis.p.nz=Analysis.imgA.getNSlices();

		Analysis.Tools= new Tools(Analysis.p.ni, Analysis.p.nj, Analysis.p.nz,5);




		//set parameters
		Analysis.p.mode_voronoi2=false;
		Analysis.p.mode_classic=true;
		Analysis.p.refinement=false;
		Analysis.p.max_nsb=11;
		Analysis.p.tol=1e-7;
		Analysis.p.removebackground=false;
		//	Analysis.p.betaMLEoutdefault=0.003;//0.0027356;
		//	Analysis.p.betaMLEindefault=0.3;
		Analysis.p.sigma_gaussian=0.8;
		Analysis.p.nlevels=5;
		Analysis.p.ldata=1;
		Analysis.p.lreg=0.075;//0.075;//0.06//0.01 fro PSF test //0.05 for one region non psf test
		Analysis.p.gamma=1;
		Analysis.p.usePSF=false;
		Analysis.p.RSSinit=false;
		Analysis.p.initRegionsIntensity=false;
		Analysis.p.findregionthresh=false;
		Analysis.p.dispvoronoi=false;
		Analysis.p.RegionsIntensitymodulo=5000;
		Analysis.p.dispvesicles=false;
		Analysis.p.JunitTest=true;

		//do segmentation
		CountDownLatch DoneSignala = new CountDownLatch(1);	


		new Thread(new NRegions(Analysis.imgA,Analysis.p,DoneSignala, 0)).start();

		//wait for computation
		try {
			DoneSignala.await();
		}catch (InterruptedException ex) {}


		//return best energy
		return Analysis.bestEnergyX;

	}
	
	
}




