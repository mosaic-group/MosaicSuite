package mosaic.bregman;


import java.io.Serializable;

import mosaic.core.psf.psf;
import net.imglib2.type.numeric.real.DoubleType;


public class Parameters implements Serializable {
    private static final long serialVersionUID = 2894976420127964864L;

    // =========================== Already deleted in code - before removing update config file.
    @SuppressWarnings("unused") final private int oc_s = 1;  
    @SuppressWarnings("unused") final private int maxves_size = -1; 
    @SuppressWarnings("unused") final private boolean mode_classic = false; 
    @SuppressWarnings("unused") final private boolean mode_voronoi2 = true; 
    @SuppressWarnings("unused") final private int model_oversampling = 1; 
    @SuppressWarnings("unused") final private boolean displowlevels = true; 
    @SuppressWarnings("unused") final private boolean looptest = false; 
    @SuppressWarnings("unused") final private boolean fastsquassh = false; 
    @SuppressWarnings("unused") final private int overs = 2; 
    @SuppressWarnings("unused") final private boolean usePSF = true; 
    @SuppressWarnings("unused") final private boolean automatic_int = false; 
    @SuppressWarnings("unused") final private int dispEmodulo = 10; 
    @SuppressWarnings("unused") final private boolean remask = false; 
    @SuppressWarnings("unused") final private int nlevels = 1; 
    @SuppressWarnings("unused") final private boolean dispvoronoi = false; 
    @SuppressWarnings("unused") final private int RSSmodulo = 5000; 
    @SuppressWarnings("unused") final private int regionSegmentLevel = 2;  
    @SuppressWarnings("unused") final private boolean RSSinit = false; 
    @SuppressWarnings("unused") final private double thresh = 0.75; 
    @SuppressWarnings("unused") final private boolean findregionthresh = true; 
    @SuppressWarnings("unused") final private double regionthresh = 0.19;
    @SuppressWarnings("unused") final private double regionthreshy = 0.19;
    
    // ================================ Other parameters not changeable directly by user and/or nasty globals
    
    // const segmentation parameters - might be useful if beter names are given (config file for test update!).
    final double colocthreshold = 0.5;
    final int RegionsIntensitymodulo = 3000;
    final int energyEvaluationModulo = 5;
    final public boolean debug = false;
    final double ldata = 1;
    final double gamma = 1;
    final double tol = 1e-7;
    final double betaMLEindefault = 1.0;
    final double betaMLEoutdefault = 0.0003;
    final public int minves_size = 2;
    
    // not yet investigated
    public int interpolation = 1;// 4
    public int oversampling2ndstep = 2;// 2
    public int max_nsb = 201;
    public boolean refinement = false;
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
    public double thresholdcellmasky = 0.0015;

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
        this.min_intensity = p.min_intensity;
        this.min_intensityY = p.min_intensityY;
        this.sigma_gaussian = p.sigma_gaussian;
        this.zcorrec = p.zcorrec;
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
