package mosaic.bregman.segmentation;

public class SegmentationParameters {
    // ================================ Constant segmentation parameters 
    public final int energyEvaluationModulo = 5;
    public final boolean debug = false;
    public final double ldata = 1;
    public final double gamma = 1;
    public final double tol = 1e-7;  // Energy margin below which minimizing energy is stopped
    public final double betaMLEindefault = 1.0;
    public final double betaMLEoutdefault = 0.0003;
    public final int minves_size = 2;

    // ================================ General settings
    public int nthreads = 4;
    public String patches_from_file;
    
    // ================================ Segmentation parameters
    public double regularization = 0.05;
    public double minObjectIntensity = 0.15;
    public boolean subpixel = false;
    public boolean exclude_z_edges = true;
    public int mode_intensity = 0; // 0 - "Automatic", 1 - "Low", 2 - "Medium", 3 - "High"
    public int noise_model = 0; // 0 - "Poisson", 1 - "Gauss"
    public double sigma_gaussian = 0.8;
    public double zcorrec = 1;
    public double min_region_filter_intensities = 0.0;

    // ================================ *not yet investigated*
    public int interpolation = 1;
    public int oversampling2ndstep = 2;
}
