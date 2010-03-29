

import Jama.Matrix;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.process.StackStatistics;

public class PSFestimator_3D  implements  PlugInFilter{
	StackStatistics stackStatistics;
	Matrix Sigma;
	Matrix Mean;
	public void run(ImageProcessor ip) {
		
	}

	public int setup(String arg, ImagePlus imp) {		
		StackConverter sc = new StackConverter(imp);
		sc.convertToGray32();
		doStats(imp);
		subtractMinValue(imp);
		doStats(imp);	
		normalizeFrameFloat(imp.getStack());
		doStats(imp);
		fitGauss(imp.getStack());
		printOnStdOut();
		return PlugInFilter.DONE;
	}
	
	private void doStats(ImagePlus imp){
		stackStatistics = new StackStatistics(imp);
	}
	
	private void subtractMinValue(ImagePlus imp) {
		for(int s =0; s < imp.getStackSize(); s++) {
			ImageProcessor ip = imp.getStack().getProcessor(s+1);
			ip.add(-stackStatistics.min);
		}			
	}

	private void normalizeFrameFloat(ImageStack is) {
		float sum = 0;
		for(int s = 1; s <= is.getSize(); s++){
			float[] pixels=(float[])is.getPixels(s);
			for (int i = 0; i < pixels.length; i++) {
				sum += pixels[i];
			}
		}
		for(int s = 1; s <= is.getSize(); s++){
			float[] pixels=(float[])is.getPixels(s);
			for (int i = 0; i < pixels.length; i++) {
				pixels[i] /= sum;
			}
		}
	}
	
	private void fitGauss(ImageStack is){
		int w = is.getWidth();
		//calculate the mean
		
		Mean = new Matrix(3,1);
		float mean_x = 0;
		float mean_y = 0;
		float mean_z = 0;
		for(int s = 0; s < is.getSize(); s++) {
			ImageProcessor ip = is.getProcessor(s+1);
			float[] pixels = (float[])ip.convertToFloat().getPixels();
			for(int y = 0; y < ip.getHeight(); y++) {
				for(int x = 0; x < ip.getWidth(); x++) {
					mean_x += pixels[y*w+x] * x;			
					mean_y += pixels[y*w+x] * y;	
					mean_z += pixels[y*w+x] * s;	
				}
			}
		}
		Mean.set(0, 0, mean_x);
		Mean.set(1, 0, mean_y);
		Mean.set(2, 0, mean_z);
		
		
		//calculate the cov
		Matrix v = new Matrix(3,1);
		Sigma = new Matrix(3,3);
		for(int s = 0; s < is.getSize(); s++) {
			ImageProcessor ip = is.getProcessor(s+1);
			float[] pixels = (float[])ip.convertToFloat().getPixels();
			for(int y = 0; y < ip.getHeight(); y++) {
				for(int x = 0; x < ip.getWidth(); x++) {
					v.set(0, 0, x - mean_x);
					v.set(1, 0, y - mean_y);
					v.set(2, 0, s - mean_z);
					Matrix vT = v.transpose();
					Matrix c = v.times(vT);
					Sigma.plusEquals(c.times(pixels[y*w+x]));
				}
			}
		}
		
	}
	
	private void printOnStdOut(){
		System.out.println("Sigma = \n");
		Sigma.print(6, 3);
		System.out.println("Mean = \n");
		Mean.print(6, 3);
	}
	
	
}
