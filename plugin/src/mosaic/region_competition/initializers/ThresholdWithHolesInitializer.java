package mosaic.region_competition.initializers;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.plugin.filter.MaximumFinder;
import ij.process.ImageProcessor;
import mosaic.region_competition.IntensityImage;
import mosaic.region_competition.LabelImage;

public class ThresholdWithHolesInitializer extends DataDrivenInitializer
{

	public ThresholdWithHolesInitializer(IntensityImage intensityImage, LabelImage labelImage)
	{
		super(intensityImage, labelImage);
	}


	public void initSwissCheese()
	{
		ImagePlus imp = intensityImage.imageIP;
		imp = new Duplicator().run(imp);
		IJ.run(imp, "Gaussian Blur...", "sigma=3 stack");
//		imp.show();
		ImagePlus holes = new Duplicator().run(imp);
		
		// thresholding
		IJ.setAutoThreshold(imp, "Otsu dark stack");
		IJ.run(imp, "Convert to Mask", "  black");
		ImageStack stack1 = imp.getImageStack();
		
		// holes
		IJ.run(holes, "Invert", "stack");
		holes.setTitle("swiss cheese holes");
		holes = findMaximaStack(holes, 0.01);
		ImageStack stack2 = holes.getImageStack();

		
//		IJ.run(holes, "Find Maxima...", "noise=0 output=[Maxima Within Tolerance] light");
		
		
		int nSlizes = stack1.getSize();
		for(int i=0; i<nSlizes; i++)
		{
			ImageProcessor proc1 = stack1.getProcessor(i+1);
			ImageProcessor proc2 = stack2.getProcessor(i+1);
			
			int size = proc1.getPixelCount();
			for(int idx=0; idx<size; idx++)
			{
				int v1 = proc1.get(idx);
				int v2 = proc2.get(idx);
				
				if(v2!=0) // on hole (hole is non zero), set black in main imp
				{
					proc1.set(idx, 0);
				}
			}
			
		}
//		imp.show();
//		holes.show();
		
		imp.show();
		labelImage.initWithIP(imp);
		labelImage.connectedComponents();
	}
	
	public ImagePlus findMaximaStack(ImagePlus imp, double tolerance)
	{
		ImageStack stack = imp.getStack();
		int n = stack.getSize();
		
		ImageStack byteStack = new ImageStack(stack.getWidth(), stack.getHeight());
		
		MaximumFinder finder = new MaximumFinder();
		for(int i=0; i<n; i++)
		{
			ImageProcessor proc = stack.getProcessor(i+1);
			ImageProcessor maxima = finder.findMaxima(proc, tolerance, ImageProcessor.NO_THRESHOLD, MaximumFinder.IN_TOLERANCE, false, false);
			byteStack.addSlice(maxima);
		}
		ImagePlus result = new ImagePlus("findMax"+imp, byteStack);
//		result.show();
		
		return result;
	}
	
	
	@Override
	public void initDefault()
	{
		initSwissCheese();
	}

}
