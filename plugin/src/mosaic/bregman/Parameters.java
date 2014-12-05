package mosaic.bregman;

import java.io.Serializable;

import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import mosaic.core.psf.psf;

public class Parameters  implements Serializable
{
	private static final long serialVersionUID = 2894976420127964864L;

	//

	public int oc_s = 1;
	public String patches_from_file;
	public int output_format = -1;
	
	// method parameters
	public  boolean mode_classic=false;
	public  boolean mode_watershed=false;
	public  boolean mode_voronoi2=true;
	
	public boolean save_images=true;
	public String wd=null;

	public int overs=2;
	public boolean pearson = false;
	public boolean debug=false;
	public boolean blackbackground;
	public int noise_model=0; //0: poisson, 1:gauss
	public boolean firstphase=true;
	public boolean dispwindows=true;
	public boolean automatic_int=false;
	public boolean subpixel=false;
	public boolean refinement=false;
	public int model_oversampling=1;
	public int oversampling2ndstep=2;//2
	public int interpolation=2;//4
	public boolean JunitTest = false;
	public double ldata = 1;
	public double lreg_[] = {0.05,0.05};
	// region non psf test
	public double gamma = 1;// was 10 : use 1 for two region PSF version
	public int size_rollingball = 10;
	public int max_nsb = 201;
	public int nlevels = 2;
	public double tol = 1e-7;// 1e-5
	public boolean removebackground = false;
	public boolean displowlevels = true;
	public boolean livedisplay = false;
	public boolean usePSF = true;
	public boolean cAB = true;
	public boolean cBA = true;
	public boolean cint = true;
	public boolean cintY = true;
	public boolean looptest = false;
	public int maxves_size = -1;
	public int minves_size = 5;//5// set in genericgui now (pour mode voronoi2)
	public double min_intensity = 0.15;// 0.1
	public double min_intensityY = 0.15;// 0.1
	public double colocthreshold = 0.5;
	public double sigma_gaussian = 0.8;
	public double zcorrec = 1;// was 2
	public int RSSmodulo = 5000;
	public boolean RSSinit = false;
	public boolean initRegionsIntensity = false;
	public boolean findregionthresh = true;
	public double regionthresh = 0.19; // pour mitochondria 0.25 //0.19
	public double regionthreshy = 0.19; // pour mitochondria 0.25 //0.19
	public boolean dispvoronoi = false;
	public int RegionsIntensitymodulo = 3000;
	public int nthreads = 8;
	public boolean usecellmaskX = false;
	public boolean usecellmaskY = false;
	public boolean dispvesicles = true;
	public double thresholdcellmask = 0.0015;
	public double thresholdcellmasky = 0.0015;//(RAB channel)
	public int energyEvaluationModulo = 5;
	public int dispEmodulo=10;
	public boolean dispSoftMask = false;
	public int nbconditions = 1;
	public int regionSegmentLevel = 2;
	public boolean remask=false;
	public boolean exclude_z_edges=true;
	
	public int nchannels=2;
	public int mode_intensity=0;//0 automatic, 1 low int, 2 high int (by clustering)


	public double thresh = 0.75;
	public double betaMLEoutdefault =0.0003;// 0.0298;//0.003;// 0.003 // 0.0027356;
	public double betaMLEindefault = 0.3;//0.082;//0.3;// 0.25;//25;//1340026;//0..//0.45 for//0.3
	// mito segmentation 0.4 (in latest, was 0.45..)

	
	public boolean fastsquassh = false;
	public boolean dispint= false;
	public boolean displabels= false;
	public boolean dispcolors= false;
	public boolean dispoutline= true;
	public boolean dispcoloc= false;
	public double min_region_filter_intensities = 0.0;
	
	//Rscript parameters
	public boolean initrsettings= true;
	public String file1;
	public String file2;
	public String file3;
	public int [] nbimages ={1,1,1,1,1};//init with size 5, extended if needed in Rscriptlistener
	public String [] groupnames={"Condition " + 1 + " name","Condition " + 2 + " name",
			"Condition " + 3 + " name","Condition " + 4 + " name","Condition " + 5 + " name"};
	public String ch1="channel 1 name";
	public String ch2="channel 2 name";
	

	public psf<DoubleType> PSF;


	public int ni, nj, nz;

	public double[] cl;

	// todo : clean code by using ni nj nz in this parameters class

	public Parameters() 
	{
		int max = Math.max(2, nlevels);
		cl = new double[max];
		lreg_ = new double[2];
		lreg_[0] = 0.05;
		lreg_[1] = 0.05;
	}

	//copy constructor
	public Parameters(Parameters p)
	{
		this.save_images=p.save_images;
		this.wd=p.wd;

		this.debug=p.debug;
		this.model_oversampling=p.model_oversampling;
		this.interpolation=p.interpolation;
		this.JunitTest = p.JunitTest;
		this.ldata = p.ldata;
		for (int i = 0 ; i < this.lreg_.length ; i++)
		{
			this.lreg_[i] = p.lreg_[i];
		}
		// region non psf test
		this.gamma = p.gamma;// was 10 : use 1 for two region PSF version
		this.size_rollingball = p.size_rollingball;
		this.max_nsb = p.max_nsb;
		this.nlevels = p.nlevels;
		this.tol = p.tol;// 1e-5
		this.removebackground = p.removebackground;
		this.displowlevels = p.displowlevels;
		this.livedisplay = p.livedisplay;
		this.usePSF = p.usePSF;
		this.cAB = p.cAB;
		this.cBA = p.cBA;
		this.cint = p.cint;
		this.cintY = p.cintY;
		this.looptest = p.looptest;
		this.maxves_size = p.maxves_size;
		this.minves_size = p.minves_size;
		this.min_intensity = p.min_intensity;// 0.1
		this.min_intensityY = p.min_intensityY;// 0.1
		this.colocthreshold = p.colocthreshold;
		this.sigma_gaussian = p.sigma_gaussian;
		this.zcorrec = p.zcorrec;// was 2
		this.RSSmodulo = p.RSSmodulo;
		this.RSSinit = p.RSSinit;
		this.initRegionsIntensity = p.initRegionsIntensity;
		this.findregionthresh = p.findregionthresh;
		this.regionthresh = p.regionthresh; // pour mitochondria 0.25 
		this.regionthreshy = p.regionthreshy; // pour mitochondria 0.25 
		this.dispvoronoi = p.dispvoronoi;
		this.RegionsIntensitymodulo = p.RegionsIntensitymodulo;
		this.nthreads = p.nthreads;
		this.usecellmaskX = p.usecellmaskX;
		this.usecellmaskY = p.usecellmaskY;
		this.dispvesicles = p.dispvesicles;
		this.thresholdcellmask = p.thresholdcellmask;
		this.thresholdcellmasky = p.thresholdcellmasky;
		this.energyEvaluationModulo = p.energyEvaluationModulo;
		this.regionSegmentLevel = p.regionSegmentLevel;
		this.nchannels=p.nchannels;
		this.thresh = p.thresh;
		this.betaMLEoutdefault = p.betaMLEoutdefault;// 0.003 // 0.0027356;
		this.betaMLEindefault = p.betaMLEindefault;// 0.25;//25;//1340026;//0..//0.45 for//0.3
		this.min_region_filter_intensities = p.min_region_filter_intensities;
		
		this.dispSoftMask = p.dispSoftMask;
		this.fastsquassh = p.fastsquassh;
		this.overs=p.overs;
		this.dispint= p.dispint;
		this.displabels= p.displabels;
		this.dispcolors= p.dispcolors;
		this.dispoutline= p.dispoutline;
		this.dispcoloc= p.dispcoloc;
		this.PSF= p.PSF;
		this.mode_intensity=p.mode_intensity;
		this.noise_model=p.noise_model;

		this.ni=p.ni; this.nj=p.nj; this.nz=p.nz;
		this.blackbackground=p.blackbackground;
		
		int max = Math.max(2, this.nlevels);
		this.cl = new double[max];

		this.mode_voronoi2=p.mode_voronoi2;
		this.mode_classic=p.mode_classic;
		this.mode_watershed=p.mode_watershed;
		
	}

}
