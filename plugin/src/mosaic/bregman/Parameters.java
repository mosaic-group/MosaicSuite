package mosaic.bregman;


import java.io.Serializable;

import mosaic.core.psf.psf;
import net.imglib2.type.numeric.real.DoubleType;


public class Parameters implements Serializable {
    private static final long serialVersionUID = 2894976420127964864L;

    // ================================ this guys are set only in Parameters class and used as const values later on
    final int oc_s = 1; // Already deleted in code - before removing update config file. 
    final int maxves_size = -1; // Already deleted in code - before removing update config file.
    final boolean mode_classic = false; // Already deleted in code - before removing update config file.
    final public boolean mode_voronoi2 = true; // Already deleted in code - before removing update config file.
    final int model_oversampling = 1; // Already deleted in code - before removing update config file.
    final boolean displowlevels = true; // Already deleted in code - before removing update config file.
    final boolean looptest = false; // Already deleted in code - before removing update config file.
    final boolean fastsquassh = false; // Already deleted in code - before removing update config file.
    final int overs = 2; // Already deleted in code - before removing update config file.
    final boolean usePSF = true; // Already deleted in code - before removing update config file.
    final boolean automatic_int = false; // Already deleted in code - before removing update config file.
    final int dispEmodulo = 10; // Already deleted in code - before removing update config file.
    final boolean remask = false; // Already deleted in code - before removing update config file.
    final int nlevels = 1; // Already deleted in code - before removing update config file.
    final public  boolean dispvoronoi = false; // Already deleted in code - before removing update config file.
    final int RSSmodulo = 5000; // Already deleted in code - before removing update config file.
    final public int regionSegmentLevel = 2;  // Already deleted in code - before removing update config file.
    final boolean RSSinit = false; // Already deleted in code - before removing update config file.
    final double thresh = 0.75; // Already deleted in code - before removing update config file.
    final boolean findregionthresh = true; // Already deleted in code - before removing update config file.
    
    // Other parameters not changeable directly by user and/or nasty globals
    final double colocthreshold = 0.5;
    final int RegionsIntensitymodulo = 3000;
    final int energyEvaluationModulo = 5;
    final public boolean debug = false;
    final double ldata = 1;
    final double gamma = 1;
    final double tol = 1e-7;
    final double betaMLEindefault = 1.0;
    final double betaMLEoutdefault = 0.0003;
    
    public int interpolation = 1;// 4
    public int oversampling2ndstep = 2;// 2
    public int minves_size = 5;// 5// set in genericgui now (pour mode voronoi2)
    public int max_nsb = 201;
    public boolean refinement = false;
    public double regionthresh = 0.19; // pour mitochondria 0.25 //0.19
    public double regionthreshy = 0.19; // pour mitochondria 0.25 //0.19
    public String wd = null;
    public boolean dispwindows = true;
    final double[] cl;
    public int nthreads = 4;
    public int nchannels = 2;
    boolean firstphase = true;
    public psf<DoubleType> PSF;
    int ni, nj, nz;
    
    // ================================ parameters changed in GUI
    
    // Segmentation options
    final public double lreg_[] = { 0.05, 0.05 };
    public double min_intensity = 0.15;
    public double min_intensityY = 0.15;
    public boolean subpixel = false;
    public boolean exclude_z_edges = true;
    public int mode_intensity = 0; // 0 - "Automatic", 1 - "Low", 2 - "Medium", 3 - "High"
    public int noise_model = 0; // 0 - "Poisson", 1 - "Gauss"
    public double sigma_gaussian = 0.8;
    public double zcorrec = 1;
    public double min_region_filter_intensities = 0.0;
    public String patches_from_file;

    // Background subtracter
    public boolean removebackground = false;
    public int size_rollingball = 10;
    
    // Colocalization / Cell masks
    public boolean usecellmaskX = false;
    public boolean usecellmaskY = false;
    public double thresholdcellmask = 0.0015;
    public double thresholdcellmasky = 0.0015;// (RAB channel)

    // Visualization    
    public boolean livedisplay = false;
    public boolean dispcolors = false;
    public boolean dispint = false;
    public boolean displabels = false;
    public boolean dispoutline = true;
    public boolean dispSoftMask = false;
    public boolean save_images = true;
    public int nbconditions = 1;
    
    // Set condition names... RScriptWindow
    public String ch1 = "channel 1 name";
    public String ch2 = "channel 2 name";
    public int[] nbimages = { 1, 1, 1, 1, 1 };// init with size 5, extended if needed in Rscriptlistener
    public String[] groupnames = { "Condition " + 1 + " name", "Condition " + 2 + " name", "Condition " + 3 + " name", "Condition " + 4 + " name", "Condition " + 5 + " name" };
    
    
    public Parameters() {
        cl = new double[2];
    }

    // copy constructor
    public Parameters(Parameters p) {
        this.save_images = p.save_images;
        this.wd = p.wd;

        this.interpolation = p.interpolation;
        for (int i = 0; i < this.lreg_.length; i++) {
            this.lreg_[i] = p.lreg_[i];
        }
        this.size_rollingball = p.size_rollingball;
        this.max_nsb = p.max_nsb;
        this.removebackground = p.removebackground;
        this.livedisplay = p.livedisplay;
        this.minves_size = p.minves_size;
        this.min_intensity = p.min_intensity;
        this.min_intensityY = p.min_intensityY;
        this.sigma_gaussian = p.sigma_gaussian;
        this.zcorrec = p.zcorrec;
        this.regionthresh = p.regionthresh; 
        this.regionthreshy = p.regionthreshy;
        this.nthreads = p.nthreads;
        this.usecellmaskX = p.usecellmaskX;
        this.usecellmaskY = p.usecellmaskY;
        this.thresholdcellmask = p.thresholdcellmask;
        this.thresholdcellmasky = p.thresholdcellmasky;
        this.nchannels = p.nchannels;
        this.min_region_filter_intensities = p.min_region_filter_intensities;

        this.dispSoftMask = p.dispSoftMask;
        this.dispint = p.dispint;
        this.displabels = p.displabels;
        this.dispcolors = p.dispcolors;
        this.dispoutline = p.dispoutline;
        this.PSF = p.PSF;
        this.mode_intensity = p.mode_intensity;
        this.noise_model = p.noise_model;

        this.ni = p.ni;
        this.nj = p.nj;
        this.nz = p.nz;

        this.cl = new double[2];
    }

    @Override
    public String toString() {
        String str = "save_images=" + this.save_images + "\n";
        str += "wd=" + this.wd + "\n";
        for (int i = 0; i < this.lreg_.length; i++) {
            str += "lreg_[" + i + "]" + this.lreg_[i] + "\n";
        }
        str += "size_rollingball=" + this.size_rollingball + "\n";
        str += "removebackground=" + this.removebackground + "\n";
        str += "min_intensity=" + this.min_intensity + "\n";
        str += "min_intensityY" + this.min_intensityY + "\n";
        str += "sigma_gaussian=" + this.sigma_gaussian + "\n";
        str += "zcorrec=" + this.zcorrec + "\n";

        return str;
    }
}
