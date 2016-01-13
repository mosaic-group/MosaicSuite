package mosaic.bregman;


import java.io.Serializable;

import mosaic.core.psf.psf;
import net.imglib2.type.numeric.real.DoubleType;


public class Parameters implements Serializable {
    private static final long serialVersionUID = 2894976420127964864L;

    // Output format (CSV)
    public final int oc_s = 1;

    // ================================ this guys are set only in Parameters class and used as const values later on
    boolean mode_classic = false; // Already deleted in code - before removing updated config file.
    public boolean mode_voronoi2 = true;
    public boolean debug = false;
    int model_oversampling = 1; // Already deleted in code - before removing updated config file.
    double ldata = 1;
    double gamma = 1;// was 10 : use 1 for two region PSF version
    double tol = 1e-7;// 1e-5
    boolean displowlevels = true;
    boolean looptest = false;
    int maxves_size = -1;
    double colocthreshold = 0.5;
    int RegionsIntensitymodulo = 3000;
    int energyEvaluationModulo = 5;
    boolean fastsquassh = false; // Already deleted in code - before removing updated config file.
    int overs = 2;
    boolean usePSF = true;
    
    // ================================ parameters changed in GUI
    public String patches_from_file;
    public boolean save_images = true;
    public String ch1 = "channel 1 name";
    public String ch2 = "channel 2 name";
    public boolean dispSoftMask = false;
    public boolean dispint = false;
    public boolean displabels = false;
    public boolean dispcolors = false;
    public boolean dispoutline = true;
    public boolean exclude_z_edges = true;
    public String[] groupnames = { "Condition " + 1 + " name", "Condition " + 2 + " name", "Condition " + 3 + " name", "Condition " + 4 + " name", "Condition " + 5 + " name" };
    public boolean livedisplay = false;
    final public double lreg_[] = { 0.05, 0.05 };
    public double min_intensity = 0.15;
    public double min_intensityY = 0.15;
    public double min_region_filter_intensities = 0.0;
    public int mode_intensity = 0; // 0 automatic, 1 low int, 2 high int (by clustering)
    public int nbconditions = 1;
    public int[] nbimages = { 1, 1, 1, 1, 1 };// init with size 5, extended if needed in Rscriptlistener
    public int noise_model = 0; 
    public boolean removebackground = false;
    public double sigma_gaussian = 0.8;
    public int size_rollingball = 10;
    public boolean subpixel = false;
    public double thresholdcellmask = 0.0015;
    public double thresholdcellmasky = 0.0015;// (RAB channel)
    public boolean usecellmaskY = false;
    public boolean usecellmaskX = false;
    public int interpolation = 2;// 4
    public int oversampling2ndstep = 2;// 2
    public int minves_size = 5;// 5// set in genericgui now (pour mode voronoi2)
    public boolean dispvoronoi = false;
    public int regionSegmentLevel = 2;
    public int max_nsb = 201;
    public boolean refinement = false;
    public double regionthresh = 0.19; // pour mitochondria 0.25 //0.19
    public double regionthreshy = 0.19; // pour mitochondria 0.25 //0.19
    public double betaMLEindefault = 0.3;// 0.082;//0.3;// 0.25;//25;//1340026;//0..//0.45 for//0.3
    public String wd = null;
    public boolean dispwindows = true;
    
    // ==================================
    
    boolean firstphase = true;
    final boolean automatic_int = false;
    int nlevels = 2;
    double[] cl;
    public double zcorrec = 1;// was 2
    int RSSmodulo = 5000;
    boolean RSSinit = false;
    boolean findregionthresh = true;
    public int nthreads = 8;
    final int dispEmodulo = 10;
    boolean remask = false;
    public int nchannels = 2;
    double thresh = 0.75;
    double betaMLEoutdefault = 0.0003;// 0.0298;//0.003;// 0.003 // 0.0027356;
    public psf<DoubleType> PSF;
    int ni, nj, nz;

    // ===============================================
    
    public Parameters() {
        final int max = Math.max(2, nlevels);
        cl = new double[max];
    }

    // copy constructor
    public Parameters(Parameters p) {
        this.save_images = p.save_images;
        this.wd = p.wd;

        this.debug = p.debug;
        this.model_oversampling = p.model_oversampling;
        this.interpolation = p.interpolation;
        this.ldata = p.ldata;
        for (int i = 0; i < this.lreg_.length; i++) {
            this.lreg_[i] = p.lreg_[i];
        }
        // region non psf test
        this.gamma = p.gamma;
        this.size_rollingball = p.size_rollingball;
        this.max_nsb = p.max_nsb;
        this.nlevels = p.nlevels;
        this.tol = p.tol;
        this.removebackground = p.removebackground;
        this.displowlevels = p.displowlevels;
        this.livedisplay = p.livedisplay;
        //this.usePSF = p.usePSF;
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
        this.findregionthresh = p.findregionthresh;
        this.regionthresh = p.regionthresh; // pour mitochondria 0.25
        this.regionthreshy = p.regionthreshy; // pour mitochondria 0.25
        this.dispvoronoi = p.dispvoronoi;
        this.RegionsIntensitymodulo = p.RegionsIntensitymodulo;
        this.nthreads = p.nthreads;
        this.usecellmaskX = p.usecellmaskX;
        this.usecellmaskY = p.usecellmaskY;
        this.thresholdcellmask = p.thresholdcellmask;
        this.thresholdcellmasky = p.thresholdcellmasky;
        this.energyEvaluationModulo = p.energyEvaluationModulo;
        this.regionSegmentLevel = p.regionSegmentLevel;
        this.nchannels = p.nchannels;
        this.thresh = p.thresh;
        this.betaMLEoutdefault = p.betaMLEoutdefault;// 0.003 // 0.0027356;
        this.betaMLEindefault = p.betaMLEindefault;// 0.25;//25;//1340026;//0..//0.45 for//0.3
        this.min_region_filter_intensities = p.min_region_filter_intensities;

        this.dispSoftMask = p.dispSoftMask;
        this.fastsquassh = p.fastsquassh;
        this.overs = p.overs;
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

        final int max = Math.max(2, this.nlevels);
        this.cl = new double[max];

        this.mode_voronoi2 = p.mode_voronoi2;
        this.mode_classic = p.mode_classic;        
    }

    @Override
    public String toString() {
        String str = new String();

        str += "save_images=" + this.save_images + "\n";
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
