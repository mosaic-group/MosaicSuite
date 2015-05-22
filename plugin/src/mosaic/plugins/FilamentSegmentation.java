package mosaic.plugins;

import net.jgeom.nurbs.util.InterpolationException;
import ij.process.FloatProcessor;
import mosaic.nurbs.BSplineSurface;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.ImgUtils.MinMax;
import mosaic.plugins.utils.PlugInFloatBase;

public class FilamentSegmentation extends PlugInFloatBase {
	// Distance between control points 
	private int iCoefficientStep;

	protected void segmentation(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
		// Get dimensions of input image
        final int originalWidth = aOrigImg.getWidth();
        final int originalHeight = aOrigImg.getHeight();
        int stepSize = (int) Math.pow(2, iCoefficientStep);
        int width = (int) (Math.ceil((float)originalWidth/stepSize) * stepSize);
        int height = (int) (Math.ceil((float)originalHeight/stepSize) * stepSize);        
        float[][] img = new float[width][height];

        // Convert to array and normalize to 0..1 values range
        ImgUtils.ImgToXY2Darray(aOrigImg, img, 1.0f);
        MinMax<Float> minMax = ImgUtils.findMinMax(img);
        ImgUtils.normalize(img);
        
        // TODO: segmentation goes here...
        try {
        	System.out.println("[" + img.length + ", " + img[0].length + "]");
        	new BSplineSurface(img, 3, 3, 16 ,16);
		} catch (InterpolationException e) {
			e.printStackTrace();
		}
        
        // Convert array to Image with converting back range of values
        ImgUtils.convertRange(img, minMax.getMax() - minMax.getMin(), minMax.getMin());
        ImgUtils.XY2DarrayToImg(img, aOutputImg, 1.0f);
	}
	
	@Override
	protected boolean showDialog() {
		// TODO: this data should be handled in dialog window, hard-coded in a meantime
		
		// Should take values from 0..4 -> distance between control points is then 2**scale => 1..16
		iCoefficientStep = 5;
		return true;
	}

	@Override
	protected boolean setup(String aArgs) {
		return true;
	}

	@Override
	protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
		segmentation(aOutputImg, aOrigImg);
	}

}
