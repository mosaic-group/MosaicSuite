package mosaic.plugins;

import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import mosaic.filamentSegmentation.SegmentationAlgorithm;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.filamentSegmentation.SegmentationFunctions;
import mosaic.math.CubicSmoothingSpline;
import mosaic.math.MFunc;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.plugins.utils.Debug;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;

/**
 * Implementation of filament segmentation plugin.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FilamentSegmentation extends PlugInFloatBase {
	// Distance between control points 
	private NoiseType iNoiseType;
	private PsfType iPsfType;
	private Dimension iPsfDimension;
	private double iSubpixelSampling;
    private int iCoefficientStep;
	private double iRegularizerTerm;
	private int iNumberOfIterations;

	
    private final String PropNoiseType       = "FilamentSegmentation.noiseType";
    private final String PropPsfType         = "FilamentSegmentation.psfType";
    private final String PropPsfDimensionX   = "FilamentSegmentation.psfDimensionX";
    private final String PropPsfDimensionY   = "FilamentSegmentation.psfDimensionY";
    private final String PropSubpixel        = "FilamentSegmentation.subpixel";
    private final String PropScale           = "FilamentSegmentation.scale";
    private final String PropRegularizerTerm = "FilamentSegmentation.propRegularizerTerm";
    private final String PropNoOfIterations  = "FilamentSegmentation.noOfIterations";
    
//    iPsfType = PsfType.values()[Arrays.asList(psfType).indexOf(psf)];
//    iPsfDimension = new Dimension(psfx, psfy);
//    iSubpixelSampling = 1/Math.pow(2, Arrays.asList(subPixel).indexOf(subpixel));
//    iCoefficientStep = Arrays.asList(scales).indexOf(scale);
//    iRegularizerTerm = 1e-4;
//    iNumberOfIterations = iterations;
    
    
	private Map<Integer, List<CubicSmoothingSpline>> m = new TreeMap<Integer, List<CubicSmoothingSpline>>();;
	private synchronized void addNewFindings(List<CubicSmoothingSpline> css, Integer number) {
	    m.put(number, css);
	}

	private ImagePlus cp;
	
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
        System.out.println(sa);
        List<CubicSmoothingSpline> ps = sa.performSegmentation();
        
        // Save results and update output image
        addNewFindings(ps, aOrigImg.getSliceNumber());
        drawFinalImg(aOrigImg, aChannelNumber, ps);
       
        // Convert array back to Image
        ImgUtils.YX2DarrayToImg(img, aOutputImg, 1.0f);
	}

    private synchronized void drawFinalImg(FloatProcessor aOrigImg, int aChannelNumber, List<CubicSmoothingSpline> ps) {
        ImageStack stack = cp.getStack();
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
            for (int i = 0; i < x.size(); ++i) {
                int xp = (int) (x.get(i)) - 1;
                int yp = (int) (y.get(i)) - 1;
                if (xp < 0 || xp >= w - 1 || yp < 0 || yp >= h - 1)
                    continue;
                pixels[yp * w + xp] = pixels[yp * w + xp] | 255 << ((2-aChannelNumber) * 8);
            }
        }
    }

	@Override
    protected void postprocess() {
        iProcessedImg = cp;//  cp.show();
        
        // TODO: Output that to table or to file(s)
        System.out.println(m.size());
        for (Entry<Integer, List<CubicSmoothingSpline>> e : m.entrySet()) {
            String lenstr = "";
            for (CubicSmoothingSpline css : e.getValue()) {
                lenstr += SegmentationFunctions.calcualteFilamentLenght(css);
                lenstr += ", ";
            }
                   
           System.out.println(e.getKey() +  ", " + lenstr);
        }
        
        PlotWindow.noGridLines = false; // draw grid lines
        Plot plot = new Plot("FILAMENTS", "X", "Y");
        //plot.setLineWidth(2);
       // plot.setSize(WIDTH, HEIGHT);
        plot.setLimits(0, iInputImg.getWidth(), 0, iInputImg.getHeight());
        
        
        // Plot data
        plot.setColor(Color.blue);
        
        for (List<CubicSmoothingSpline> ps : m.values()) {
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
	    GenericDialog gd = new GenericDialog("Filament Segmentation Settings");

        final String[] noiseType = {"Gaussian", "Poisson"};
        gd.addRadioButtonGroup("Noise Type: ", noiseType, 3, 1, Prefs.get(PropNoiseType, noiseType[0]));
        
        final String[] psfType = {"Gaussian", "Dark Field", "Phase Contrast"};
        gd.addRadioButtonGroup("PSF Type: ", psfType, 3, 1, Prefs.get(PropPsfType, psfType[0]));
        gd.addNumericField("PSF dimensions:     [rows]", (int)Prefs.get(PropPsfDimensionY, 1), 0);
        gd.addNumericField("                 [columns]", (int)Prefs.get(PropPsfDimensionX, 1), 0);
        
        final String[] subPixel = {"1x", "2x", "4x"};
        gd.addRadioButtonGroup("Subpixel sampling: ", subPixel, 1, 3, Prefs.get(PropSubpixel, subPixel[0]));
        
        final String[] scales = {"100 %", "50 %", "25 %", "12.5 %", "6.25 %"};
        gd.addRadioButtonGroup("Scale of level set mask (% of input image): ", scales, 5, 1, Prefs.get(PropScale, scales[1]));

        gd.addMessage("");
        gd.addNumericField("Regularizer (lambda): 0.001 * ", Prefs.get(PropRegularizerTerm, 0.1), 3);
        gd.addNumericField("Maximum number of iterations: ", (int)Prefs.get(PropNoOfIterations, 100), 0);

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
        
        gd.showDialog();
        if (gd.wasCanceled()) return false;
        
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

        iNoiseType = NoiseType.values()[Arrays.asList(noiseType).indexOf(noise)];
        iPsfType = PsfType.values()[Arrays.asList(psfType).indexOf(psf)];
        iPsfDimension = new Dimension(psfx, psfy);
        iSubpixelSampling = 1/Math.pow(2, Arrays.asList(subPixel).indexOf(subpixel));
        iCoefficientStep = Arrays.asList(scales).indexOf(scale);
        iRegularizerTerm = lambda / 1000;
        iNumberOfIterations = iterations;
        
		return true;
	}

	@Override
	protected boolean setup(String aArgs) {
	    cp = createNewEmptyImgPlus(iInputImg, "segmented_" + iInputImg.getTitle(), 1, 1, true);
		return true;
	}

	@Override
	protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
		segmentation(aOutputImg, aOrigImg, aChannelNumber);
	}

}
