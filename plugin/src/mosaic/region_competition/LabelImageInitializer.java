package mosaic.region_competition;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.plugin.filter.MaximumFinder;
import ij.process.ImageProcessor;

public abstract class LabelImageInitializer
{
	
	LabelImage labelImage;
	
	public LabelImageInitializer(LabelImage labelImage)
	{
		this.labelImage=labelImage;
	}
	
	
	public abstract void initialize();
}



class ZeroInitializer extends LabelImageInitializer
{
	public ZeroInitializer(LabelImage labelImage)
	{
		super(labelImage);
	}

	@Override
	public void initialize()
	{
		int size = labelImage.size;
		for(int i=0; i<size; i++)
		{
			labelImage.setLabel(i, 0);
		}
	}
}


class BrightBubblesInitializer extends LabelImageInitializer
{
	IntensityImage intensityImage;

	public BrightBubblesInitializer(LabelImage labelImage, IntensityImage intensityImage)
	{
		super(labelImage);
		this.intensityImage = intensityImage;
	}

	@Override
	public void initialize()
	{
		initBrightBubbles(intensityImage);
	}
	
	public void initBrightBubbles(IntensityImage intensityImage)
	{
		ImagePlus imp = intensityImage.imageIP;
		imp = new Duplicator().run(imp);
		IJ.run(imp, "Gaussian Blur...", "sigma=3 stack");
		
		ImageStack stack = imp.getStack();
		int n = stack.getSize();
		
		ImageStack byteStack = new ImageStack(stack.getWidth(), stack.getHeight());
		
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
	
}









