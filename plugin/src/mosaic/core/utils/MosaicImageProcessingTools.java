package mosaic.core.utils;

import ij.IJ;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class MosaicImageProcessingTools {
	
	public static float[] CalculateNormalizedGaussKernel(float aSigma){
		int vL = (int)aSigma * 3 * 2 + 1;
		if (vL < 3) vL = 3;
		float[] vKernel = new float[vL];
		int vM = vKernel.length/2;
		for (int vI = 0; vI < vM; vI++){
			vKernel[vI] = (float)(1f/(2f*Math.PI*aSigma*aSigma) * Math.exp(-(float)((vM-vI)*(vM-vI))/(2f*aSigma*aSigma)));
			vKernel[vKernel.length - vI - 1] = vKernel[vI];
		}
		vKernel[vM] = (float)(1f/(2f*Math.PI*aSigma*aSigma));

		//normalize the kernel numerically:
		float vSum = 0;
		for (int vI = 0; vI < vKernel.length; vI++){
			vSum += vKernel[vI];
		}
		float vScale = 1.0f/vSum;
		for (int vI = 0; vI < vKernel.length; vI++){
			vKernel[vI] *= vScale;
		}
		return vKernel;
	}
	
	/**
	 * Dilates a copy of a given ImageProcessor with a spherical structuring element of size 
	 * <code>radius</code>.
	 * Adapted as is from Ingo Oppermann implementation
	 * @param ip ImageProcessor to do the dilation with
	 * @return the dilated copy of the given <code>ImageProcessor</code> 
	 */		
	public static ImageStack dilateGeneric(ImageStack ips, int radius, int number_of_threads) {
		return dilateGeneric(ips,radius,Float.NEGATIVE_INFINITY, number_of_threads);
	}
	
	/**
	 * Dilates all values larger than <code>threshold</code> and returns a copy of the input image.
	 * A spherical structuring element of radius <code>radius</code> is used. 
	 * Adapted as is from Ingo Oppermann implementation
	 * @param ip ImageProcessor to do the dilation with
	 * @return the dilated copy of the given <code>ImageProcessor</code> 
	 */		
	public static ImageStack dilateGeneric(ImageStack ips, int radius, float threshold, int number_of_threads) {
		FloatProcessor[] dilated_procs = new FloatProcessor[ips.getSize()];
		AtomicInteger z  = new AtomicInteger(-1);
		Vector<Thread> threadsVector = new Vector<Thread>();
		for (int thread_counter = 0; thread_counter < number_of_threads; thread_counter++){
			threadsVector.add(new DilateGenericThread(ips,radius,dilated_procs,z));
		}
		for (Thread t : threadsVector){
			t.start();                               
		}
		for (Thread t : threadsVector){
			try {
				t.join();                                        
			}catch (InterruptedException ie) {
				IJ.showMessage("Calculation interrupted. An error occured in parallel dilation:\n" + ie.getMessage());
			}
		}
		ImageStack dilated_ips = new ImageStack(ips.getWidth(), ips.getHeight());
		for (int s = 0; s < ips.getSize(); s++)
			dilated_ips.addSlice(null, dilated_procs[s]);
		//			new StackWindow(new ImagePlus("dilated image", dilated_ips));

		return dilated_ips;
	}

	/**
	 * CURRENTLY NOT USED. Generates the dilation mask
	 * Adapted from Ingo Oppermann implementation
	 * @param mask_radius the radius of the mask (user defined)
	 */
	public static void generateBinaryMask(int mask_radius) {    	
		int width = (2 * mask_radius) + 1;
		short[][] binary_mask = new short[width][width*width];
		for (int s = -mask_radius; s <= mask_radius; s++){
			for (int i = -mask_radius; i <= mask_radius; i++) {
				for (int j = -mask_radius; j <= mask_radius; j++) {
					int index = MosaicUtils.coord(i + mask_radius, j + mask_radius, width);
					if ((i * i) + (j * j) + (s * s) <= mask_radius * mask_radius)
						binary_mask[s + mask_radius][index] = 1;
					else
						binary_mask[s + mask_radius][index] = 0;

				}
			}
		}
	}

	/**
	 * CURRENTLY NOT USED. Generates the dilation mask
	 * Adapted from Ingo Oppermann implementation
	 * @param mask_radius the radius of the mask (user defined)
	 */
	public static int[][] generateMask(int mask_radius) {    	

		int width = (2 * mask_radius) + 1;
		int[][] mask = new int[width][width*width];
		for (int s = -mask_radius; s <= mask_radius; s++){
			for (int i = -mask_radius; i <= mask_radius; i++) {
				for (int j = -mask_radius; j <= mask_radius; j++) {
					int index = MosaicUtils.coord(i + mask_radius, j + mask_radius, width);
					if ((i * i) + (j * j) + (s * s) <= mask_radius * mask_radius)
						mask[s + mask_radius][index] = 1;
					else
						mask[s + mask_radius][index] = 0;

				}
			}
		}
		return mask;
	}

	public static void generateWeightedMask_old(int mask_radius, float xCenter, float yCenter, float zCenter) {
		
		int width = (2 * mask_radius) + 1;
		float[][] weighted_mask = new float[width][width*width];
		
		for (int iz = -mask_radius; iz <= mask_radius; iz++){
			for (int iy = -mask_radius; iy <= mask_radius; iy++) {
				for (int ix = -mask_radius; ix <= mask_radius; ix++) {
					int index = MosaicUtils.coord(iy + mask_radius, ix + mask_radius, width);

					float distPxToCenter = (float) Math.sqrt((xCenter-ix)*(xCenter-ix)+(yCenter-iy)*(yCenter-iy)+(zCenter-iz)*(zCenter-iz)); 

					//the weight is approximative the amount of the voxel inside the (spherical) mask.
					float weight = mask_radius - distPxToCenter + .5f; 
					if (weight < 0) {
						weight = 0f;    				
					} 
					if (weight > 1) {
						weight = 1f;
					}
					weighted_mask[iz + mask_radius][index] = weight;
				}
			}
		}
	}

	/**
	 * CURRENTLY NOT USED. This code was used for prototyping more accurate point detection in 2D.
	 * @param mask_radius
	 * @param xCenter
	 * @param yCenter
	 * @param zCenter
	 */
	public void generateWeightedMask_2D(int mask_radius, float xCenter, float yCenter, float zCenter) {
		int width = (2 * mask_radius) + 1;
		float[][] weighted_mask = new float[width][width*width];
		
		float pixel_radius = 0.5f;		
		float r = pixel_radius;
		float R = mask_radius;
		for (int iy = -mask_radius; iy <= mask_radius; iy++) {
			for (int ix = -mask_radius; ix <= mask_radius; ix++) {
				int index = MosaicUtils.coord(iy + mask_radius, ix + mask_radius, width);

				float distPxCenterToMaskCenter = (float) Math.sqrt((xCenter-ix)*(xCenter-ix)+(yCenter-iy)*(yCenter-iy)); 
				float d = distPxCenterToMaskCenter;
				//the weight is approximative the amount of the voxel inside the (spherical) mask. See formula 
				//http://mathworld.wolfram.com/Circle-CircleIntersection.html
				float weight = 0;
				if (distPxCenterToMaskCenter < mask_radius + pixel_radius){
					weight = 1;

					if (mask_radius < distPxCenterToMaskCenter + pixel_radius) {
						float v = (float) (pixel_radius*pixel_radius*
								Math.acos((d*d+r*r-R*R)/(2*d*r))
								+R*R*Math.acos((d*d+R*R-r*r)/(2*d*R))
								-0.5f*Math.sqrt((-d+r+R)*(d+r-R)*(d-r+R)*(d+r+R)));

						weight =  (v / ((float)Math.PI * pixel_radius*pixel_radius));
					}
				}

				for (int iz = -mask_radius; iz <= mask_radius; iz++){
					weighted_mask[iz + mask_radius][index] = weight;
				}
			}
		}
	}

	/**
	 * CURRENTLY NOT USED. This code was used for prototyping more accurate point detection in 2D.
	 * @param mask_radius
	 * @param xCenter
	 * @param yCenter
	 * @param zCenter
	 */
	public void generateWeightedMask_3D(int mask_radius, float xCenter, float yCenter, float zCenter) {
		int width = (2 * mask_radius) + 1;
		float voxel_radius = 0.5f;
		float[][] weighted_mask = new float[width][width*width];
		for (int iz = -mask_radius; iz <= mask_radius; iz++){
			for (int iy = -mask_radius; iy <= mask_radius; iy++) {
				for (int ix = -mask_radius; ix <= mask_radius; ix++) {
					int index = MosaicUtils.coord(iy + mask_radius, ix + mask_radius, width);

					float distPxCenterToMaskCenter = (float) Math.sqrt((xCenter-ix+.5f)*(xCenter-ix+.5f)+(yCenter-iy+.5f)*(yCenter-iy+.5f)+(zCenter-iz+.5f)*(zCenter-iz+.5f)); 

					//the weight is approximative the amount of the voxel inside the (spherical) mask.
					float weight = 0; 
					if (distPxCenterToMaskCenter < mask_radius + voxel_radius){
						weight = 1;

						if (mask_radius < distPxCenterToMaskCenter + voxel_radius) {

							//The volume is given by http://mathworld.wolfram.com/Sphere-SphereIntersection.html
							float v = (float) (Math.PI*Math.pow(voxel_radius + mask_radius - distPxCenterToMaskCenter ,2)
									*(distPxCenterToMaskCenter * distPxCenterToMaskCenter +2 * distPxCenterToMaskCenter * mask_radius 
											- 3 * mask_radius * mask_radius + 2 * distPxCenterToMaskCenter * voxel_radius 
											+ 6 * mask_radius * voxel_radius - 3 * voxel_radius * voxel_radius) 
											/ (12 * distPxCenterToMaskCenter));
							weight = (float) (v / ((4f*Math.PI/3f)*Math.pow(voxel_radius,3)));
						}
					}
					weighted_mask[iz + mask_radius][index] = weight;
				}
			}
		}
	}

}


class DilateGenericThread extends Thread{
	ImageStack ips;
	ImageProcessor[] dilated_ips;
	AtomicInteger atomic_z;
	int kernel_width;
	int image_width;
	int image_height;
	int radius;
	float threshold;
	int mask[][];
	
	public DilateGenericThread(ImageStack is, int aRadius, ImageProcessor[] dilated_is, AtomicInteger z) {
		initMembers(is, aRadius, Float.NEGATIVE_INFINITY, dilated_is,z);	
	}
	
	public DilateGenericThread(ImageStack is, int aRadius, float aThreshold, ImageProcessor[] dilated_is, AtomicInteger z) {
		initMembers(is, aRadius, aThreshold, dilated_is,z);	
	}
	
	private void initMembers(ImageStack is, int aRadius, float aThreshold, ImageProcessor[] dilated_is, AtomicInteger z) {
		ips = is;
		dilated_ips = dilated_is;
		atomic_z = z;

		radius = aRadius;
		kernel_width = radius*2 + 1;
		image_width = ips.getWidth();
		image_height = ips.getHeight();
		
		mask = MosaicImageProcessingTools.generateMask(radius);
		threshold = aThreshold;
	}

	public void run() {
		float max;
		int z;
		while ((z = atomic_z.incrementAndGet()) < ips.getSize()) {
			//					IJ.showStatus("Dilate Image: " + (z+1));
			//					IJ.showProgress(z, ips.getSize());
			FloatProcessor out_p = new FloatProcessor(image_width, image_height);
			float[] output = (float[])out_p.getPixels();
			float[] dummy_processor = (float[])ips.getPixels(z+1);
			for (int y = 0; y < image_height; y++) {
				for (int x = 0; x < image_width; x++) {
					//little big speed-up:
					if (dummy_processor[y*image_width+x] < threshold) {
						continue;
					}
					max = Float.NEGATIVE_INFINITY; 
					
					//a,b,c are the kernel coordinates corresponding to x,y,z
					for (int s = -radius; s <= radius; s++ ) {
						if (z + s < 0 || z + s >= ips.getSize())
							continue;
						float[] current_processor_pixels = (float[])ips.getPixels(z+s+1);
						for (int b = -radius; b <= radius; b++ ) {
							if (y + b < 0 || y + b >= ips.getHeight())
								continue;
							for (int a = -radius; a <= radius; a++ ) {
								if (x + a < 0 || x + a >= ips.getWidth())
									continue;
								if (mask[s + radius][(a + radius)* kernel_width+ (b + radius)] == 1) {
									float t;
									if ((t = current_processor_pixels[(y + b)* image_width +  (x + a)]) > max) {
										max = t;
									}
								}
							}
						}
					}
					output[y*image_width + x]= max;
				}
			}
			dilated_ips[z] = out_p;
		}
	}



}

