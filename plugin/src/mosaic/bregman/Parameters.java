package mosaic.bregman;


import java.io.Serializable;


public class Parameters implements Serializable {
    private static final long serialVersionUID = 2894976420127964864L;

    // ================================ General parameters
    public String wd = null; // Last working dir
    public int nthreads = 4; // Number of threads
    
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
    public int[] nbimages = { 1 }; // init with size 1, extended if needed in Rscriptlistener
    public String[] groupnames = { "Condition 1 name" };
    
    
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
        str += "--------- Segmentation ----------------\n";
        str += "subpixel: " + subpixel + "\n";
        str += "exclude_z_edges: " + exclude_z_edges + "\n";
        str += "--------- Visualization ----------------\n";
        str += "save_images: " + save_images + "\n";
        str += "dispoutline: " + dispoutline + "\n";
        str += "dispSoftMask: " + dispSoftMask + "\n";
        str += "displabels: " + displabels + "\n";
        str += "dispint: " + dispint + "\n";
        str += "dispcolors: " + dispcolors + "\n";
        
        return str;
    }
}
