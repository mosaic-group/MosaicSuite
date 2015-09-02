package mosaic.plugins;

import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextArea;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mosaic.filamentSegmentation.SegmentationAlgorithm;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.math.CubicSmoothingSpline;
import mosaic.math.MFunc;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;

/**
 * Implementation of filament segmentation plugin.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FilamentSegmentation extends PlugInFloatBase {
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
    
    // Synchronized map used to collect segmentation data from all plugin threads
	private Map<Integer, Map<Integer, List<CubicSmoothingSpline>>> m = new TreeMap<Integer, Map<Integer, List<CubicSmoothingSpline>>>();
	private synchronized void addNewFinding(List<CubicSmoothingSpline> aCubicSpline, Integer aSlieceNumber, Integer aChannelNumber) {
	    if (m.get(aSlieceNumber) == null) m.put(aSlieceNumber, new TreeMap<Integer, List<CubicSmoothingSpline>>());
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
	private void segmentation(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
		// Get dimensions of input image
        final int originalWidth = aOrigImg.getWidth();
        final int originalHeight = aOrigImg.getHeight();

        // Convert to array
        double[][] img = new double[originalHeight][originalWidth];
        ImgUtils.ImgToYX2Darray(aOrigImg, img, 1.0f);

        // --------------- SEGMENTATION --------------------------------------------
        SegmentationAlgorithm sa = new SegmentationAlgorithm(img, 
                                                             iNoiseType, 
                                                             iPsfType, 
                                                             iPsfDimension, 
                                /* subpixel sampling */      iSubpixelSampling, 
                                /* scale */                  iCoefficientStep, 
                                /* regularizer term */       iRegularizerTerm,
                                                             iNumberOfIterations);
        List<CubicSmoothingSpline> ps = sa.performSegmentation();

        // Save results and update output image
        addNewFinding(ps, aOrigImg.getSliceNumber(), aChannelNumber);
        drawFinalImg(aOrigImg, aChannelNumber, ps);
	}

    private synchronized void drawFinalImg(FloatProcessor aOrigImg, int aChannelNumber, List<CubicSmoothingSpline> ps) {
        // TODO: This is temporary implementation. After taking decision how to 
        // proceed with filaments this method must be revised
        ImageStack stack = iOutputColorImg.getStack();
        ImageProcessor ip = stack.getProcessor(aOrigImg.getSliceNumber());
        int noOfChannels = iInputImg.getStack().getProcessor(aOrigImg.getSliceNumber()).getNChannels();
        if (noOfChannels != 1) {
            ip.setPixels(aChannelNumber, iInputImg.getStack().getProcessor(aOrigImg.getSliceNumber()).toFloat(aChannelNumber, null));
        }
        else {
            for (int c = 0; c <= 2; ++c) {
                ip.setPixels(c, iInputImg.getStack().getProcessor(aOrigImg.getSliceNumber()).toFloat(0, null));   
            }
        }
        int[] pixels = (int[]) ip.getPixels();
        for (CubicSmoothingSpline css : ps) {
            final CubicSmoothingSpline css1 = css;
            double start = css1.getKnot(0);
            double stop = css1.getKnot(css1.getNumberOfKNots() - 1);

            final Matrix x = Matlab.linspace(start, stop, 1000);
            Matrix y = x.copy().process(new MFunc() {
                @Override
                public double f(double aElement, int aRow, int aCol) {
                    return css1.getValue(x.get(aRow, aCol));
                }
            });

            int w = ip.getWidth(), h = ip.getHeight();
            int color = 0;
            if (aChannelNumber == 0) color = 255 << 16;
            else if (aChannelNumber == 1) color = 255 << 8;
            else if (aChannelNumber == 2) color = 255 << 0;
            for (int i = 0; i < x.size(); ++i) {
                int xp = (int) (x.get(i)) - 1;
                int yp = (int) (y.get(i)) - 1;
                if (xp < 0 || xp >= w - 1 || yp < 0 || yp >= h - 1)
                    continue;
                pixels[yp * w + xp] = pixels[yp * w + xp] | color;
            }
        }
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
        Plot plot = new Plot("All filaments", "X", "Y");
        plot.setLimits(0, iInputImg.getWidth(), 0, iInputImg.getHeight());
        
        
        // Plot data
        plot.setColor(Color.blue);
        for (Map<Integer, List<CubicSmoothingSpline>> ms : m.values())
        for (List<CubicSmoothingSpline> ps : ms.values()) {
            int count = 0;
            for (CubicSmoothingSpline css : ps) {
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
                double start = css1.getKnot(0);
                double stop = css1.getKnot(css1.getNumberOfKNots() - 1);
    
                final Matrix x = Matlab.linspace(start, stop, 100);
                Matrix y = x.copy().process(new MFunc() {
                    @Override
                    public double f(double aElement, int aRow, int aCol) {
                        return css1.getValue(aElement);
                    }
                });
    
                
                plot.addPoints(x.getData(),y.getData(), PlotWindow.LINE);
            }
        }
        plot.show();
	}
	
	@Override
	protected boolean showDialog() {
	    // Create GUI for entering segmentation parameters
	    GenericDialog gd = new GenericDialog("Filament Segmentation Settings");

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
        Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        TextArea ta = new TextArea(info, 7, 40, TextArea.SCROLLBARS_NONE); 
        ta.setBackground(SystemColor.control);
        ta.setEditable(false);
        ta.setFocusable(true);
        panel.add(ta);
        gd.addPanel(panel);
        
        // Show and check if user want to continue
        gd.showDialog();
        if (gd.wasCanceled()) return false;
        
        // Read data from all fields and remember it in preferences
        String noise = gd.getNextRadioButton();
        String psf = gd.getNextRadioButton();
        int psfx = (int)gd.getNextNumber();
        int psfy = (int)gd.getNextNumber();
        String subpixel = gd.getNextRadioButton();
        String scale = gd.getNextRadioButton();
        double lambda = gd.getNextNumber();
        int iterations = (int)gd.getNextNumber();
        
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
	    ImageWindow window = iProcessedImg.getWindow();
	    
	    // When in batch mode there is no window
	    if (window != null) {
    	    window.addComponentListener(new ComponentAdapter() {
    	        @Override
    	        public void componentResized(ComponentEvent e)
    	        {
    	            // TODO: Add here implementation for drawing filaments when windows size/zoom is changed.
    	        }
    	    });
	    }
	};
	
	@Override
	protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
		segmentation(aOutputImg, aOrigImg, aChannelNumber);
	}

}
