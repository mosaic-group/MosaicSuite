package mosaic.plugins;

import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextArea;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mosaic.filamentSegmentation.SegmentationAlgorithm;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;
import mosaic.utils.ConvertArray;
import mosaic.utils.math.CubicSmoothingSpline;
import mosaic.utils.math.MFunc;
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;
import ij.gui.PolygonRoi;

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

    // Properties names for saving data from GUI
    private final String PropNoiseType       = "FilamentSegmentation.noiseType";
    private final String PropPsfType         = "FilamentSegmentation.psfType";
    private final String PropPsfDimensionX   = "FilamentSegmentation.psfDimensionX";
    private final String PropPsfDimensionY   = "FilamentSegmentation.psfDimensionY";
    private final String PropSubpixel        = "FilamentSegmentation.subpixel";
    private final String PropScale           = "FilamentSegmentation.scale";
    private final String PropRegularizerTerm = "FilamentSegmentation.propRegularizerTerm";
    private final String PropNoOfIterations  = "FilamentSegmentation.noOfIterations";

    private Roi r;
    
    // Synchronized map used to collect segmentation data from all plugin threads
    private final Map<Integer, Map<Integer, List<CubicSmoothingSpline>>> m = new TreeMap<Integer, Map<Integer, List<CubicSmoothingSpline>>>();
    private synchronized void addNewFinding(List<CubicSmoothingSpline> aCubicSpline, Integer aSlieceNumber, Integer aChannelNumber) {
        if (m.get(aSlieceNumber) == null) {
            m.put(aSlieceNumber, new TreeMap<Integer, List<CubicSmoothingSpline>>());
        }
        m.get(aSlieceNumber).put(aChannelNumber, aCubicSpline);
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
        drawFinalImg(aOrigImg, aChannelNumber, ps);
    }

    private synchronized void drawFinalImg(FloatProcessor aOrigImg, int aChannelNumber, List<CubicSmoothingSpline> ps) {
        // TODO: This is temporary implementation. After taking decision how to
        // proceed with filaments this method must be revised
        final ImageStack stack = iOutputColorImg.getStack();
        final ImageProcessor ip = stack.getProcessor(aOrigImg.getSliceNumber());
        final int noOfChannels = iInputImg.getStack().getProcessor(aOrigImg.getSliceNumber()).getNChannels();
        if (noOfChannels != 1) {
            ip.setPixels(aChannelNumber, iInputImg.getStack().getProcessor(aOrigImg.getSliceNumber()).toFloat(aChannelNumber, null));
        }
        else {
            for (int c = 0; c <= 2; ++c) {
                ip.setPixels(c, iInputImg.getStack().getProcessor(aOrigImg.getSliceNumber()).toFloat(0, null));
            }
        }
        final int[] pixels = (int[]) ip.getPixels();
        Overlay overlay = new Overlay();
        for (final CubicSmoothingSpline css : ps) {
            final CubicSmoothingSpline css1 = css;
            final double start = css1.getKnot(0);
            final double stop = css1.getKnot(css1.getNumberOfKNots() - 1);

            final Matrix x = Matlab.linspace(start, stop, 1000);
            final Matrix y = x.copy().process(new MFunc() {
                @Override
                public double f(double aElement, int aRow, int aCol) {
                    return css1.getValue(x.get(aRow, aCol));
                }
            });
            x.sub(1);
            y.sub(1);
            r = new PolygonRoi(x.getArrayYXasFloats()[0], y.getArrayYXasFloats()[0], Roi.POLYLINE);
            overlay.add(r);
            final int w = ip.getWidth(), h = ip.getHeight();
            int color = 0;
            if (aChannelNumber == 0) {
                color = 255 << 16;
            }
            else if (aChannelNumber == 1) {
                color = 255 << 8;
            }
            else if (aChannelNumber == 2) {
                color = 255 << 0;
            }
            for (int i = 0; i < x.size(); ++i) {
                final int xp = (int) (x.get(i));
                final int yp = (int) (y.get(i));
                if (xp < 0 || xp >= w - 1 || yp < 0 || yp >= h - 1) {
                    continue;
                }
                pixels[yp * w + xp] = pixels[yp * w + xp] | color;
            }
        }
        
        iOutputColorImg.setSliceWithoutUpdate(aOrigImg.getSliceNumber());
        iOutputColorImg.setOverlay(overlay);
    }

    @Override
    protected void postprocessBeforeShow() {
        // TODO: Output that to table or to file(s)
        // TODO: This is temporary implementation. After taking decision how to
        // proceed with filaments this method must be revised
        System.out.println(m.size());
        //        for (Entry<Integer, List<CubicSmoothingSpline>> e : m.entrySet()) {
        //            String lenstr = "";
        //            for (CubicSmoothingSpline css : e.getValue()) {
        //                lenstr += SegmentationFunctions.calcualteFilamentLenght(css);
        //                lenstr += ", ";
        //            }
        //
        //           System.out.println(e.getKey() +  ", " + lenstr);
        //        }

        PlotWindow.noGridLines = false; // draw grid lines
        final Plot plot = new Plot("All filaments", "X", "Y");
        plot.setLimits(0, iInputImg.getWidth(), iInputImg.getHeight(), 0);
        
        // Plot data
        plot.setColor(Color.blue);
        for (final Map<Integer, List<CubicSmoothingSpline>> ms : m.values()) {
            for (final List<CubicSmoothingSpline> ps : ms.values()) {
                int count = 0;
                for (final CubicSmoothingSpline css : ps) {
                    switch(count) {
                        case 0: plot.setColor(Color.BLUE);break;
                        case 1: plot.setColor(Color.RED);break;
                        case 2: plot.setColor(Color.GREEN);break;
                        case 3: plot.setColor(Color.BLACK);break;
                        case 4: plot.setColor(Color.CYAN);break;
                        default:plot.setColor(Color.MAGENTA);break;
                    }
                    count++;
                    final CubicSmoothingSpline css1 = css;
                    final double start = css1.getKnot(0);
                    final double stop = css1.getKnot(css1.getNumberOfKNots() - 1);

                    final Matrix x = Matlab.linspace(start, stop, 100);
                    final Matrix y = x.copy().process(new MFunc() {
                        @Override
                        public double f(double aElement, int aRow, int aCol) {
                            return css1.getValue(aElement);
                        }
                    });


                    plot.addPoints(x.getData(),y.getData(), PlotWindow.LINE);
                }
            }
        }
        
        plot.show();
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

        Prefs.set(PropNoiseType, noise);
        Prefs.set(PropPsfType, psf);
        Prefs.set(PropPsfDimensionX, psfx);
        Prefs.set(PropPsfDimensionY, psfy);
        Prefs.set(PropSubpixel, subpixel);
        Prefs.set(PropScale, scale);
        Prefs.set(PropRegularizerTerm, lambda);
        Prefs.set(PropNoOfIterations, iterations);

        // Set segmentation paramters for futher use
        iNoiseType = NoiseType.values()[Arrays.asList(noiseType).indexOf(noise)];
        iPsfType = PsfType.values()[Arrays.asList(psfType).indexOf(psf)];
        iPsfDimension = new Dimension(psfx, psfy);
        iSubpixelSampling = 1/Math.pow(2, Arrays.asList(subPixel).indexOf(subpixel)); // 1, 0.5, 0.25
        iCoefficientStep = Arrays.asList(scales).indexOf(scale);
        iRegularizerTerm = lambda / 1000; // For easier user input it has scale * 1e-3
        iNumberOfIterations = iterations;

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
    protected void postprocessFinal() {
        final ImageWindow window = iProcessedImg.getWindow();

        // When in batch mode there is no window
        if (window != null) {
            window.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    // TODO: Add here implementation for drawing filaments when windows size/zoom is changed.
                    System.out.println("RESIZED");
                    iInputImg.updateAndDraw();
                    ImageCanvas canvas = iProcessedImg.getCanvas();
                    System.out.println(canvas.getHeight() + "x" + canvas.getWidth());
                    Graphics g = canvas.getGraphics();
                    g.setColor(Color.YELLOW);
                    g.drawLine(0, 0, canvas.getWidth()-1, canvas.getHeight()-1);
                }
            });
        }
        
//        ImagePlus.addImageListener(new ImageListener() {
//            
//            @Override
//            public void imageUpdated(ImagePlus arg0) {
//                System.out.println("imageUpdated");
//                ImageCanvas canvas = iProcessedImg.getCanvas();
//                System.out.println(canvas.getHeight() + "x" + canvas.getWidth());
//                Graphics g = canvas.getGraphics();
//                g.setColor(Color.RED);
//                g.drawLine(0, 0, canvas.getWidth()-1, canvas.getHeight()-1);
//                
//            }
//            
//            @Override
//            public void imageOpened(ImagePlus arg0) {
//                System.out.println("imageOpened");
//                
//            }
//            
//            @Override
//            public void imageClosed(ImagePlus arg0) {
//                System.out.println("imageClosed");
//                
//            }
//        });
    }

    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
        segmentation(aOrigImg, aChannelNumber);
    }

}
