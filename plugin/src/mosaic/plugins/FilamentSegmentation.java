package mosaic.plugins;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Dimension;
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
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;

/**
 * Implementation of filament segmentation plugin.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FilamentSegmentation extends PlugInFloatBase {
	// Distance between control points 
	private int iCoefficientStep;

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
                                                             NoiseType.GAUSSIAN, 
                                                             PsfType.GAUSSIAN, 
                                                             new Dimension(3, 3), 
                                /* subpixel sumpling */      1, 
                                /* scale */                  iCoefficientStep, 
                                /* regularizer term */       0.0001,
                                                             100);
        
        List<CubicSmoothingSpline> ps = sa.performSegmentation();
        
        // Save results and update output image
        addNewFindings(ps, aOrigImg.getSliceNumber());
        drawFinalImg(aOrigImg, aChannelNumber, ps);
       
        // Convert array to Image with converting back range of values
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
	};
	
	@Override
	protected boolean showDialog() {
		// TODO: this data should be handled in dialog window, hard-coded in a meantime
		
		// Should take values from 0..4 -> distance between control points is then 2**scale => 1..16
		iCoefficientStep = 1;
		
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
