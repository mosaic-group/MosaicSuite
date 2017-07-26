package mosaic.filamentSegmentation.GUI;

import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextArea;
import java.util.Arrays;

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.plugins.FilamentSegmentation.VisualizationLayer;

/**
 * User input dialog with filament segmentation settings
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class ConfigDialog {
    // Properties names for saving data from GUI
    private final String PropNoiseType       = "FilamentSegmentation.noiseType";
    private final String PropPsfType         = "FilamentSegmentation.psfType";
    private final String PropPsfDeviation    = "FilamentSegmentation.psfDeviation";
    private final String PropSubpixel        = "FilamentSegmentation.subpixel";
    private final String PropScale           = "FilamentSegmentation.scale";
    private final String PropRegularizerTerm = "FilamentSegmentation.propRegularizerTerm";
    private final String PropNoOfIterations  = "FilamentSegmentation.noOfIterations";
    private final String PropResultLayer     = "FilamentSegmentation.resultLayer";
    
    // Segmentation parameters and settings
    private NoiseType iNoiseType;
    private PsfType iPsfType;
    private double iPsfDeviation;
    private double iSubpixelSampling;
    private int iCoefficientStep;
    private double iRegularizerTerm;
    private int iNumberOfIterations;
    private VisualizationLayer iVisualizationLayer;
    
    
    public NoiseType getNoiseType() {
        return iNoiseType;
    }
    
    public PsfType getPsfType() {
        return iPsfType;
    }
    
    public double getPsfDeviation() {
        return iPsfDeviation;
    }
    
    public double getSubpixelSampling() {
        return iSubpixelSampling;
    }
    
    public int getCoefficientStep() {
        return iCoefficientStep;
    }
    
    public double getRegularizerTerm() {
        return iRegularizerTerm;
    }
    
    public int getNumberOfIterations() {
        return iNumberOfIterations;
    }
    
    public VisualizationLayer getVisualizationLayer() {
        return iVisualizationLayer;
    }
    
    public boolean getConfiguration() {
        boolean isConfigOK = true;
        final String[] noiseType = {"Gaussian", "Poisson"};
        final String[] psfType = {"Gaussian", "Dark Field", "Phase Contrast", "None"};
        final String[] subPixel = {"1x", "2x", "4x"};
        final String[] scales = {"100 %", "50 %", "25 %", "12.5 %", "6.25 %"};
        final String[] layers = {"Overlay (zoomable)", "Image Data"};

        do {
            // Set it for first (next) configuration loop
            isConfigOK = true;
            
            // Create GUI for entering segmentation parameters
            GenericDialog gd = createConfigWindow(noiseType, psfType, subPixel, scales, layers);
            if (gd.wasCanceled()) {
                return false;
            }
            
            isConfigOK = getUserInputAndVerify(noiseType, psfType, subPixel, scales, layers, gd);
            
        } while(!isConfigOK);
        
        return true;
    }
    
    private boolean getUserInputAndVerify(final String[] aNoiseType, final String[] aPsfType, final String[] aSubPixel, final String[] aScales, final String[] aLayers, GenericDialog aDialog) {
        // Read data from all fields and remember it in preferences
        final String noise = aDialog.getNextRadioButton();
        final String psf = aDialog.getNextRadioButton();
        final double psfDeviation = aDialog.getNextNumber();
        final String subpixel = aDialog.getNextRadioButton();
        final String scale = aDialog.getNextRadioButton();
        final double lambda = aDialog.getNextNumber();
        final int iterations = (int)aDialog.getNextNumber();
        final String layer = aDialog.getNextRadioButton();
        
        // Verify input (only things that can be entered not correctly, radio buttons are always OK)
        boolean isConfigOK = verifyInputParams(iterations, psfDeviation);
        
        if (isConfigOK) {
            // OK -> save and set all settings
            Prefs.set(PropNoiseType, noise);
            Prefs.set(PropPsfType, psf);
            Prefs.set(PropPsfDeviation, psfDeviation);
            Prefs.set(PropSubpixel, subpixel);
            Prefs.set(PropScale, scale);
            Prefs.set(PropRegularizerTerm, lambda);
            Prefs.set(PropNoOfIterations, iterations);
            Prefs.set(PropResultLayer, layer);
            
            // Set segmentation paramters for futher use
            iNoiseType = NoiseType.values()[Arrays.asList(aNoiseType).indexOf(noise)];
            iPsfType = PsfType.values()[Arrays.asList(aPsfType).indexOf(psf)];
            iPsfDeviation = psfDeviation; 
            iSubpixelSampling = 1/Math.pow(2, Arrays.asList(aSubPixel).indexOf(subpixel)); // sets: 1, 0.5, 0.25 0.125
            iCoefficientStep = Arrays.asList(aScales).indexOf(scale);
            iRegularizerTerm = lambda / 1000; // For easier user input it has scale * 1e-3
            iNumberOfIterations = iterations;
            iVisualizationLayer = VisualizationLayer.values()[Arrays.asList(aLayers).indexOf(layer)];
        }
        
        return isConfigOK;
    }

    private GenericDialog createConfigWindow(final String[] aNoiseType, final String[] aPsfType, final String[] aSubPixel, final String[] aScales, final String[] aLayers) {
        final GenericDialog gd = new GenericDialog("Filament Segmentation Settings");
        
        gd.addRadioButtonGroup("Noise_Type: ", aNoiseType, aNoiseType.length, 1, Prefs.get(PropNoiseType, aNoiseType[0]));
   
        gd.addRadioButtonGroup("PSF_Type: ", aPsfType, aPsfType.length, 1, Prefs.get(PropPsfType, aPsfType[0]));
        gd.addNumericField("PSF standard deviation:", Prefs.get(PropPsfDeviation, 0.5), 3);
   
        gd.addRadioButtonGroup("Subpixel_sampling: ", aSubPixel, 1, 3, Prefs.get(PropSubpixel, aSubPixel[0]));
   
        gd.addRadioButtonGroup("Scale of level set mask (% of input image): ", aScales, 1, 5, Prefs.get(PropScale, aScales[1]));
   
        gd.addMessage("");
        gd.addNumericField("Regularizer (lambda): 0.001 * ", Prefs.get(PropRegularizerTerm, 0.1), 3);
        gd.addNumericField("Maximum_number_of_iterations: ", (int)Prefs.get(PropNoOfIterations, 100), 0);
   
        gd.addRadioButtonGroup("Filament visualization layer: ", aLayers, 2, 1, Prefs.get(PropResultLayer, aLayers[0]));
        
        gd.addMessage("\n");
        final String referenceInfo = "\"Automatic optimal filament segmentation with sub-pixel accuracy using generalized linear models and B-spline level-sets\",\n"+
                                     "Med. Image Anal., 32:157-172, 2016\n\n" +
                                     "X. Xiao, V. F. Geyer, H. Bowne-Anderson,\n" +
                                     "J. Howard, and I. F. Sbalzarini.";
        final Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        final TextArea ta = new TextArea(referenceInfo, 6, 55, TextArea.SCROLLBARS_NONE);
        ta.setBackground(SystemColor.control);
        ta.setEditable(false);
        ta.setFocusable(true);
        panel.add(ta);
        gd.addPanel(panel);
   
        gd.showDialog();
        
        return gd;
    }

    private boolean verifyInputParams(final int iterations, final double deviation) {
        boolean isConfigOK = true;
        
        String errorMsg = "";
        if (iterations < 0) {
            isConfigOK = false;
            errorMsg += "Number of iteration cannot be lower that 0\n";
        }
        if (deviation <= 0) {
            isConfigOK = false;
            errorMsg += "PSF standard deviation must be > 0\n";
        }
        
        if (!isConfigOK) {
            // Show message to user and start again
            IJ.error(errorMsg);
        }
        
        return isConfigOK;
    }
}
