package mosaic.plugins;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextArea;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.filamentSegmentation.SegmentationAlgorithm;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.filamentSegmentation.SegmentationFunctions;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;
import mosaic.utils.math.CubicSmoothingSpline;
import mosaic.utils.math.MFunc;
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;

/**
 * Implementation of filament segmentation plugin.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FilamentSegmentation extends PlugInFloatBase { // NO_UCD
    // Segmentation parameters
    private NoiseType iNoiseType;
    private PsfType iPsfType;
    private Dimension iPsfDimension;
    private double iSubpixelSampling;
    private int iCoefficientStep;
    private double iRegularizerTerm;
    private int iNumberOfIterations;

    // Layer for visualization filaments
    public static enum VisualizationLayer {
        OVERLAY, IMAGE
    }
    VisualizationLayer iVisualizationLayer;
    
    // Properties names for saving data from GUI
    private final String PropNoiseType       = "FilamentSegmentation.noiseType";
    private final String PropPsfType         = "FilamentSegmentation.psfType";
    private final String PropPsfDimensionX   = "FilamentSegmentation.psfDimensionX";
    private final String PropPsfDimensionY   = "FilamentSegmentation.psfDimensionY";
    private final String PropSubpixel        = "FilamentSegmentation.subpixel";
    private final String PropScale           = "FilamentSegmentation.scale";
    private final String PropRegularizerTerm = "FilamentSegmentation.propRegularizerTerm";
    private final String PropNoOfIterations  = "FilamentSegmentation.noOfIterations";
    private final String PropResultLayer     = "FilamentSegmentation.resultLayer";
    
    // Synchronized map used to collect segmentation data from all plugin threads
    private final Map<Integer, Map<Integer, List<CubicSmoothingSpline>>> iFilamentsData = new TreeMap<Integer, Map<Integer, List<CubicSmoothingSpline>>>();
    private synchronized void addNewFinding(List<CubicSmoothingSpline> aCubicSpline, Integer aSlieceNumber, Integer aChannelNumber) {
        if (iFilamentsData.get(aSlieceNumber) == null) {
            iFilamentsData.put(aSlieceNumber, new TreeMap<Integer, List<CubicSmoothingSpline>>());
        }
        iFilamentsData.get(aSlieceNumber).put(aChannelNumber, aCubicSpline);
    }

    // Output image with marked filaments
    private ImagePlus iOutputColorImg;

    /**
     * Segmentation procedure for plugin
     * @param aOutputImg - output image (segmentation result)
     * @param aOrigImg - input image (to be segmented)
     * @param aChannelNumber - channel number used for drawing output image.
     */
    private void segmentation(FloatProcessor aOrigImg, int aChannelNumber) {
        // Get dimensions of input image
        final int originalWidth = aOrigImg.getWidth();
        final int originalHeight = aOrigImg.getHeight();

        // Convert to array
        final double[][] img = new double[originalHeight][originalWidth];
        ImgUtils.ImgToYX2Darray(aOrigImg, img, 1.0f);

        // --------------- SEGMENTATION --------------------------------------------
        final SegmentationAlgorithm sa = new SegmentationAlgorithm(img,
                                                                   iNoiseType,
                                                                   iPsfType,
                                                                   iPsfDimension,
                                      /* subpixel sampling */      iSubpixelSampling,
                                      /* scale */                  iCoefficientStep,
                                      /* regularizer term */       iRegularizerTerm,
                                                                   iNumberOfIterations);
        final List<CubicSmoothingSpline> ps = sa.performSegmentation();

        // Save results and update output image
        addNewFinding(ps, aOrigImg.getSliceNumber(), aChannelNumber);
    }

    private synchronized void drawFinalImgWithMarkedFilaments() {
        // Copy original image slices to output color image
        ImageStack stack = iOutputColorImg.getStack();
        for (int sn : iFilamentsData.keySet()) {
            ImageProcessor ip = stack.getProcessor(sn);
            int noOfChannels = iInputImg.getStack().getProcessor(sn).getNChannels();
    
            if (noOfChannels != 1) {
                for (int c = 0; c <= 2; ++c) {
                    ip.setPixels(c, iInputImg.getStack().getProcessor(sn).toFloat(c, null));
                }
            }
            else {
                // In case when input image is gray then copy it to all output channels (R,G,B)
                for (int c = 0; c <= 2; ++c) {
                    ip.setPixels(c, iInputImg.getStack().getProcessor(sn).toFloat(0, null));   
                }
            }
        }
        
        // Put on image all found filmanents 
        Overlay overlay = new Overlay();
        for (int sn : iFilamentsData.keySet()) {
            Map<Integer, List<CubicSmoothingSpline>> ms  = iFilamentsData.get(sn);
            ImageProcessor ip = stack.getProcessor(sn);
            for (Entry<Integer, List<CubicSmoothingSpline>> e : ms.entrySet()) {
                int[] pixels = (int[]) ip.getPixels();
                for (final CubicSmoothingSpline css : e.getValue()) {
                    
                    FilamentXyCoordinates coordinates = GenerateXyCoordinatesForFilament(css);
                    
                    if (iVisualizationLayer == VisualizationLayer.OVERLAY) {
                        Roi r = new PolygonRoi(coordinates.x.getArrayYXasFloats()[0], coordinates.y.getArrayYXasFloats()[0], Roi.POLYLINE);
                        r.setPosition(sn);
                        overlay.add(r);
                    }
                    else if (iVisualizationLayer == VisualizationLayer.IMAGE) {
                        int w = ip.getWidth(), h = ip.getHeight();
                        int color = 0;
                        if (e.getKey() == 0) color = 255 << 8;
                        else if (e.getKey() == 1) color = 255 << 0;
                        else if (e.getKey() == 2) color = 255 << 16;
                        for (int i = 0; i < coordinates.x.size(); ++i) {
                            int xp = (int) (coordinates.x.get(i));
                            int yp = (int) (coordinates.y.get(i));
                            if (xp < 0 || xp >= w - 1 || yp < 0 || yp >= h - 1)
                                continue;
                            pixels[yp * w + xp] = pixels[yp * w + xp] | color;
                        }
                    }
                }
            }
        }
        
        if (iVisualizationLayer == VisualizationLayer.OVERLAY) {
            iOutputColorImg.setOverlay(overlay);
        }
    }

    private Plot createPlotWithAllCalculetedSplines() {
        PlotWindow.noGridLines = false; // draw grid lines
        final Plot plot = new Plot("All filaments from " + iInputImg.getTitle(), "X", "Y");
        plot.setLimits(0, iInputImg.getWidth(), iInputImg.getHeight(), 0);
        
        // Plot data
        plot.setColor(Color.blue);
        for (final Map<Integer, List<CubicSmoothingSpline>> ms : iFilamentsData.values()) {
            for (final List<CubicSmoothingSpline> ps : ms.values()) {
                int count = 0;
                for (final CubicSmoothingSpline css : ps) {
                    // Mix colors - it is good for spotting changes for single filament
                    switch(count) {
                        case 0: plot.setColor(Color.BLUE);break;
                        case 1: plot.setColor(Color.RED);break;
                        case 2: plot.setColor(Color.GREEN);break;
                        case 3: plot.setColor(Color.BLACK);break;
                        case 4: plot.setColor(Color.CYAN);break;
                        default:plot.setColor(Color.MAGENTA);break;
                    }
                    count = (++count % 5); // Keeps all values in 0-5 range
                    
                    // Put stuff on plot
                    FilamentXyCoordinates coordinates = GenerateXyCoordinatesForFilament(css);
                    plot.addPoints(coordinates.x.getData(), coordinates.y.getData(), PlotWindow.LINE);
                }
            }
        }
        
        return plot;
    }

    private void generateResultsTableWithAllFilaments() {
        // Create result table with all filaments
        final ResultsTable rs = new ResultsTable();
        for (final Integer frame : iFilamentsData.keySet()) {
            final Map<Integer, List<CubicSmoothingSpline>> ms = iFilamentsData.get(frame);
            for (final List<CubicSmoothingSpline> ps : ms.values()) {
                int count = 1;
                for (final CubicSmoothingSpline css : ps) {
                    rs.incrementCounter();
                    rs.addValue("Frame", frame);
                    rs.addValue("Filament no", count);
                    rs.addValue("Lenght", SegmentationFunctions.calcualteFilamentLenght(css));
                    
                    // Find and adjust coordinates from 1..n range (used to be compatibilt wiht matlab code) 
                    // to 0..n-1 as used for images in fiji. 
                    // Additionally for x should point to middle of a pixel (currently segmentation 
                    // can found only integer values on x axis).
                    double xBegin = css.getKnot(0);
                    double xEnd = css.getKnot(css.getNumberOfKNots() - 1);
                    double yBegin = css.getValue(xBegin);
                    double yEnd = css.getValue(xEnd);
                    rs.addValue("begin x", xBegin - 1 + 0.5);
                    rs.addValue("begin y", yBegin - 1);
                    rs.addValue("end x", xEnd - 1 + 0.5);
                    rs.addValue("end y", yEnd - 1);
                    count++;
                }
            }
        }

        if (!Interpreter.isBatchMode()) {
            rs.show("Filaments segmentation results of " + iInputImg.getTitle());
        }
    }

    /**
     * Internal class used to pass generated coordinates output
     */
    private class FilamentXyCoordinates {
        Matrix x;
        Matrix y;
        
        protected FilamentXyCoordinates(Matrix aXvalues, Matrix aYvalues) {x = aXvalues; y = aYvalues;}
    }
    
    /**
     * Generates (x,y) coordinates from given cubic smoothing spline
     * @param css - input spline
     * @return 
     */
    private FilamentXyCoordinates GenerateXyCoordinatesForFilament(final CubicSmoothingSpline css) {
        // Generate x,y coordinates for current filament
        double start = css.getKnot(0);
        double stop = css.getKnot(css.getNumberOfKNots() - 1);

        final Matrix x = Matlab.linspace(start, stop, 1000);
        Matrix y = x.copy().process(new MFunc() {
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return css.getValue(x.get(aRow, aCol));
            }
        });

        // Adjust from 1..n range (used to be compatibilt wiht matlab code) to 0..n-1 as used for 
        // images in fiji. Additionally for x should point to middle of a pixel (currently segmentation 
        // can found only integer values on x axis).
        x.sub(1 - 0.5);
        y.sub(1);
        
        return new FilamentXyCoordinates(x, y);
    }
    
    @Override
    protected void postprocessBeforeShow() {
        // Show all segmentation results
        drawFinalImgWithMarkedFilaments();
        createPlotWithAllCalculetedSplines().show();
        generateResultsTableWithAllFilaments();
    }

    @Override
    protected boolean showDialog() {
        // Create GUI for entering segmentation parameters
        final GenericDialog gd = new GenericDialog("Filament Segmentation Settings");

        final String[] noiseType = {"Gaussian", "Poisson"};
        gd.addRadioButtonGroup("Noise_Type: ", noiseType, 3, 1, Prefs.get(PropNoiseType, noiseType[0]));

        final String[] psfType = {"Gaussian", "Dark Field", "Phase Contrast"};
        gd.addRadioButtonGroup("PSF_Type: ", psfType, 3, 1, Prefs.get(PropPsfType, psfType[0]));
        gd.addNumericField("PSF_dimensions:_____[rows]", (int)Prefs.get(PropPsfDimensionY, 1), 0);
        gd.addNumericField("                 [columns]", (int)Prefs.get(PropPsfDimensionX, 1), 0);

        final String[] subPixel = {"1x", "2x", "4x"};
        gd.addRadioButtonGroup("Subpixel_sampling: ", subPixel, 1, 3, Prefs.get(PropSubpixel, subPixel[0]));

        final String[] scales = {"100 %", "50 %", "25 %", "12.5 %", "6.25 %"};
        gd.addRadioButtonGroup("Scale of level set mask (% of input image): ", scales, 5, 1, Prefs.get(PropScale, scales[1]));

        gd.addMessage("");
        gd.addNumericField("Regularizer (lambda): 0.001 * ", Prefs.get(PropRegularizerTerm, 0.1), 3);
        gd.addNumericField("Maximum_number_of_iterations: ", (int)Prefs.get(PropNoOfIterations, 100), 0);

        final String[] layers = {"Overlay", "Image"};
        gd.addRadioButtonGroup("Filament visualization layer: ", layers, 2, 1, Prefs.get(PropResultLayer, layers[0]));
        
        gd.addMessage("\n");
        final String info = "\"Generalized Linear Models and B-Spline\nLevel-Sets enable Automatic Optimal\nFilament Segmentation with Sub-pixel Accuracy\"\n\nXun Xiao, Veikko Geyer, Hugo Bowne-Anderson,\nJonathon Howard, Ivo F. Sbalzarini";
        final Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        final TextArea ta = new TextArea(info, 7, 40, TextArea.SCROLLBARS_NONE);
        ta.setBackground(SystemColor.control);
        ta.setEditable(false);
        ta.setFocusable(true);
        panel.add(ta);
        gd.addPanel(panel);

        // Show and check if user want to continue
        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }

        // Read data from all fields and remember it in preferences
        final String noise = gd.getNextRadioButton();
        final String psf = gd.getNextRadioButton();
        final int psfx = (int)gd.getNextNumber();
        final int psfy = (int)gd.getNextNumber();
        final String subpixel = gd.getNextRadioButton();
        final String scale = gd.getNextRadioButton();
        final double lambda = gd.getNextNumber();
        final int iterations = (int)gd.getNextNumber();
        final String layer = gd.getNextRadioButton();
        
        Prefs.set(PropNoiseType, noise);
        Prefs.set(PropPsfType, psf);
        Prefs.set(PropPsfDimensionX, psfx);
        Prefs.set(PropPsfDimensionY, psfy);
        Prefs.set(PropSubpixel, subpixel);
        Prefs.set(PropScale, scale);
        Prefs.set(PropRegularizerTerm, lambda);
        Prefs.set(PropNoOfIterations, iterations);
        Prefs.set(PropResultLayer, layer);
        
        // Set segmentation paramters for futher use
        iNoiseType = NoiseType.values()[Arrays.asList(noiseType).indexOf(noise)];
        iPsfType = PsfType.values()[Arrays.asList(psfType).indexOf(psf)];
        iPsfDimension = new Dimension(psfx, psfy);
        iSubpixelSampling = 1/Math.pow(2, Arrays.asList(subPixel).indexOf(subpixel)); // 1, 0.5, 0.25
        iCoefficientStep = Arrays.asList(scales).indexOf(scale);
        iRegularizerTerm = lambda / 1000; // For easier user input it has scale * 1e-3
        iNumberOfIterations = iterations;
        iVisualizationLayer = VisualizationLayer.values()[Arrays.asList(layers).indexOf(layer)];
        
        return true;
    }

    @Override
    protected boolean setup(String aArgs) {
        // Generate new RGB ImagePlus and set it as a output/processed image;
        setResultDestination(ResultOutput.NONE);
        iOutputColorImg = createNewEmptyImgPlus(iInputImg, "segmented_" + iInputImg.getTitle(), 1, 1, true);
        setProcessedImg(iOutputColorImg);

        return true;
    }

    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
        segmentation(aOrigImg, aChannelNumber);
    }

}
