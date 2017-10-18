package mosaic.bregman;


import java.io.Serializable;
import java.util.Arrays;


public class Parameters implements Serializable {
    private static final long serialVersionUID = 2894976420127964864L;

    // ================================ General parameters
    public String wd = null; // Last working dir
    public int nthreads = 4; // Number of threads
    
    // ================================ parameters changed in GUI
    // Segmentation options
    public double lreg_[] = { 0.05, 0.05 };
    public double min_intensity = 0.15;
    public double min_intensityY = 0.15;
    public boolean subpixel = false;
    public boolean exclude_z_edges = true;
    public int mode_intensity = 0; // 0 - "Automatic", 1 - "Low", 2 - "Medium", 3 - "High"
    public int noise_model = 0; // 0 - "Poisson", 1 - "Gauss"
    public double sigma_gaussian = 0.8;
    public double zcorrec = 1;
    public double min_region_filter_intensities = 0.0;
    public int min_region_filter_size = 2;
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
    public int[] nbimages = { 1 }; // number images per group, extended if needed in Rscriptlistener
    public String[] groupnames = { "Condition 1 name" };
    
    public void copy(Parameters s) {
        // ================================ General parameters
        wd = s.wd;
        nthreads = s.nthreads;
        
        // ================================ parameters changed in GUI
        // Segmentation options
        lreg_ = Arrays.copyOf(s.lreg_, s.lreg_.length);
        min_intensity = s.min_intensity;
        min_intensityY = s.min_intensityY;
        subpixel = s.subpixel;
        exclude_z_edges = s.exclude_z_edges;
        mode_intensity = s.mode_intensity;
        noise_model = s.noise_model;
        sigma_gaussian = s.sigma_gaussian;
        zcorrec = s.zcorrec;
        min_region_filter_intensities = s.min_region_filter_intensities;
        min_region_filter_size = s.min_region_filter_size;
        patches_from_file = s.patches_from_file;

        // Background subtracter
        removebackground = s.removebackground;
        size_rollingball = s.size_rollingball;
        
        // Colocalization / Cell masks
        usecellmaskX = s.usecellmaskX;
        usecellmaskY = s.usecellmaskY;
        thresholdcellmask = s.thresholdcellmask;
        thresholdcellmasky = s.thresholdcellmasky;

        // Visualization    
        livedisplay = s.livedisplay;
        dispcolors = s.dispcolors;
        dispint = s.dispint;
        displabels = s.displabels;
        dispoutline = s.dispoutline;
        dispSoftMask = s.dispSoftMask;
        save_images = s.save_images;
        nbconditions = s.nbconditions;
        
        // Set condition names... RScriptWindow
        ch1 = s.ch1;
        ch2 = s.ch2;
        nbimages = Arrays.copyOf(s.nbimages, s.nbimages.length);
        groupnames = Arrays.copyOf(s.groupnames, s.groupnames.length);
    }
    
    @Override
    public String toString() {
        return mosaic.utils.Debug.getJsonString(this);
    }
}
