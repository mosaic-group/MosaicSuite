package mosaic.region_competition.initializers;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.plugin.filter.MaximumFinder;
import ij.process.ImageProcessor;
import mosaic.region_competition.IntensityImage;
import mosaic.region_competition.LabelImage;

@Deprecated
public class BrightBubbles extends DataDrivenInitializer
{

	public BrightBubbles(IntensityImage intensityImage, LabelImage labelImage)
	{
		super(intensityImage, labelImage);
	}


	public void initBrightBubbles()
	{
		ImagePlus imp = new ImagePlus("tmp_rg",intensityImage.imageIP);
		imp = new Duplicator().run(imp);
		IJ.run(imp, "Gaussian Blur...", "sigma=3 stack");
		
		ImageStack stack = imp.getStack();
		int n = stack.getSize();
		
		ImageStack byteStack = new ImageStack(stack.getWidth(), stack.getHeight());
		
		// local maximum per slice
		MaximumFinder finder = new MaximumFinder();
		for(int i=0; i<n; i++)
		{
			ImageProcessor proc = stack.getProcessor(i+1);
			ImageProcessor maxima = finder.findMaxima(proc, 0.01, 0, MaximumFinder.IN_TOLERANCE, false, false);
			byteStack.addSlice(maxima);
		}
		imp.setStack(byteStack);
		imp.setTitle("after findmax");
		imp.show();
		
		labelImage.initWithIP(imp);
		labelImage.connectedComponents();
		
//		IJ.run(imp, "Find Maxima...", "noise=0.01 output=[Maxima Within Tolerance]");
	}
	
	
	
	@Override
	public void initDefault()
	{
		initBrightBubbles();
	}

}
