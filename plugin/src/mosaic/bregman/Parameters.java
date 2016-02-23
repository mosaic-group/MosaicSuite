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
    @SuppressWarnings("unused") final private boolean dispwindows = true;
    @SuppressWarnings("unused") final private int RegionsIntensitymodulo = 3000;
    @SuppressWarnings("unused") final private double[] cl = new double[2];
    @SuppressWarnings("unused") final private boolean firstphase = true;
    @SuppressWarnings("unused") final private int ni = -1, nj = -1, nz = -1;
    @SuppressWarnings("unused") final private int max_nsb = 151;
    @SuppressWarnings("unused") final private boolean refinement = true;
    @SuppressWarnings("unused") final private int energyEvaluationModulo = 5;
    @SuppressWarnings("unused") final private boolean debug = false;
    @SuppressWarnings("unused") final private double ldata = 1;
    @SuppressWarnings("unused") final private double gamma = 1;
    @SuppressWarnings("unused") final private double tol = 1e-7;  // Energy margin below which minimizing energy is stopped
    @SuppressWarnings("unused") final private double betaMLEindefault = 1.0;
    @SuppressWarnings("unused") final private double betaMLEoutdefault = 0.0003;
    @SuppressWarnings("unused") final private int minves_size = 2;
    @SuppressWarnings("unused") final private psf<DoubleType> PSF = null;
    @SuppressWarnings("unused") final private int oversampling2ndstep = -3; // not used and set in segmentaiton
    @SuppressWarnings("unused") final private int interpolation = 3;// 4
    @SuppressWarnings("unused") final private double colocthreshold = 0.5;
    @SuppressWarnings("unused") final private int nchannels = 2;
    
    // ================================ General parameters
    public String wd = null;
    public int nthreads = 4;
    
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
    
    
    @Override
    public String toString() {
        String str = "save_images=" + save_images + "\n";
        str += "wd=" + wd + "\n";
        for (int i = 0; i < lreg_.length; i++) {
            str += "lreg_[" + i + "]" + lreg_[i] + "\n";
        }
        str += "size_rollingball=" + size_rollingball + "\n";
        str += "removebackground=" + removebackground + "\n";
        str += "min_intensity=" + min_intensity + "\n";
        str += "min_intensityY" + min_intensityY + "\n";
        str += "sigma_gaussian=" + sigma_gaussian + "\n";
        str += "zcorrec=" + zcorrec + "\n";

        return str;
    }
}
