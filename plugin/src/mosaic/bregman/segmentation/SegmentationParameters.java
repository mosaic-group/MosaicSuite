package mosaic.bregman.segmentation;

public class SegmentationParameters {
    // ================================ Constant segmentation parameters 
    final int energyEvaluationModulo = 5;
    final boolean debug = false;
    final double ldata = 1;
    final double gamma = 1;
    final double tol = 1e-7;  // Energy margin below which minimizing energy is stopped
    final double betaMLEindefault = 1.0;
    final double betaMLEoutdefault = 0.0003;
    final int minves_size = 2;

    // ================================ General settings
    public int nthreads = 4;
    public int interpolation = -5;
    
    // ================================ Segmentation parameters (from segmentation GUI)
    public double regularization = 0.05;
    public double minObjectIntensity = 0.15;
    public boolean exclude_z_edges = true;
    public int mode_intensity = 0; // 0 - "Automatic", 1 - "Low", 2 - "Medium", 3 - "High"
    public int noise_model = 0; // 0 - "Poisson", 1 - "Gauss"
    public double sigma_gaussian = 0.8;
    public double zcorrec = 1;
    public double min_region_filter_intensities = 0.0;
}
