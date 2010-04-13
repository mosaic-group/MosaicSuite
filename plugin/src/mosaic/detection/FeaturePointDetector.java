package mosaic.detection;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.StackWindow;
import ij.measure.Measurements;
import ij.plugin.filter.Convolver;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;

import java.awt.Button;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mosaic.core.Particle;
import mosaic.plugins.BackgroundSubtractor2_;
import mosaic.plugins.ParticleTracker_;

	/**
	 * <br>FeaturePointDetector class has all the necessary methods to detect the "real" particles
	 * for them to be linked.
	 * @see ParticleTracker_#mGlobalMax
	 * @see ParticleTracker_#mGlobalMin
	 */

public class FeaturePointDetector {

	public static final int ABS_THRESHOLD_MODE = 0, PERCENTILE_MODE = 1;
	public static final int NO_PREPROCESSING = 0, BOX_CAR_AVG = 1, BG_SUBTRACTOR = 2, LAPLACE_OP = 3; 

	public float global_max, global_min;
	Vector<Particle> particles;
	int particles_number;		// number of particles initialy detected 
	int real_particles_number;	// number of "real" particles discrimination
	
	Label previewLabel;

	/* user defined parameters */
	public double cutoff = 3.0; 		// default
	public float percentile = 0.001F; 	// default (user input/100)
	public int absIntensityThreshold = 0; //user input 
	public int radius = 3; 				// default
	int number_of_threads = 4;
	public int threshold_mode = PERCENTILE_MODE;
	
	private float threshold;

	/*	image Restoration vars	*/
	public short[][] binary_mask;
	public float[][] weighted_mask;
	public int[][] mask;
	public float lambda_n = 1;
	public int preprocessing_mode = BOX_CAR_AVG;



	public FeaturePointDetector(float global_max, float global_min) {
		this.global_max = global_max;
		this.global_min = global_min;
		// create Mask for Dilation
		generateMasks(this.radius);
	}


	/**
	 * First phase of the algorithm - time and memory consuming !!
	 * <br>Determines the "real" particles in this frame (only for frame constructed from Image)
	 * <br>Converts the <code>original_ip</code> to <code>FloatProcessor</code>, normalizes it, convolutes and dilates it,
	 * finds the particles, refine their position and filters out non particles
	 * @see ImageProcessor#convertToFloat()
	 */
	public void featurePointDetection (MyFrame frame) {		

		/* Converting the original imageProcessor to float 
		 * This is a constraint caused by the lack of floating point precision of pixels 
		 * value in 16bit and 8bit image processors in ImageJ therefore, if the image is not
		 * converted to 32bit floating point, false particles get detected */

		ImageStack original_ips = frame.original_ips;
		ImageStack restored_fps = new ImageStack(original_ips.getWidth(),original_ips.getHeight());


		for(int i = 1; i <= original_ips.getSize(); i++) {
			//if it is already a float, ImageJ does not create a duplicate
			restored_fps.addSlice(null, original_ips.getProcessor(i).convertToFloat().duplicate());
		}

		/* The algorithm is initialized by normalizing the frame*/
		normalizeFrameFloat(restored_fps, global_min, global_max);
		//			new StackWindow(new ImagePlus("after normalization",restored_fps));


		/* Image Restoration - Step 1 of the algorithm */
		restored_fps = imageRestoration(restored_fps);
		new StackWindow(new ImagePlus("after restoration",GetSubStackCopyInFloat(restored_fps, 1, 1)));

		/* Estimation of the point location - Step 2 of the algorithm */
		findThreshold(restored_fps, percentile, absIntensityThreshold);
		System.out.println("3D: Threshold found : " + getThreshold());

		pointLocationsEstimation(restored_fps, frame.frame_number);
		//
		//					System.out.println("particles after location estimation:");
		//					for(Particle p : this.particles) {
		//						System.out.println("particle: " + p.toString());
		//					}

		/* Refinement of the point location - Step 3 of the algorithm */
		pointLocationsRefinement(restored_fps);
		//			new StackWindow(new ImagePlus("after location ref",restored_fps));
		//			System.out.println("particles after location refinement:");
		//			for(Particle p : this.particles) {
		//				System.out.println("particle: " + p.toString());
		//			}

		/* Non Particle Discrimination(set a flag to particles) - Step 4 of the algorithm */
		nonParticleDiscrimination();

		/* Save frame information before particle discrimination/deletion - it will be lost otherwise*/
		frame.setParticles(particles, particles_number);
		frame.generateFrameInfoBeforeDiscrimination();

		/* remove all the "false" particles from particles array */
		removeNonParticle();
		frame.setParticles(particles, particles_number);
	}	


	/**
	 * Normalizes a given <code>ImageProcessor</code> to [0,1].
	 * <br>According to the pre determend global min and max pixel value in the movie.
	 * <br>All pixel intensity values I are normalized as (I-gMin)/(gMax-gMin)
	 * @param ip ImageProcessor to be normalized
	 * @param global_min 
	 * @param global_max 
	 */
	private void normalizeFrameFloat(ImageStack is, float global_min, float global_max) {
		for(int s = 1; s <= is.getSize(); s++){
			float[] pixels=(float[])is.getPixels(s);
			float tmp_pix_value;
			for (int i = 0; i < pixels.length; i++) {
				tmp_pix_value = (pixels[i]-global_min)/(global_max - global_min);
				pixels[i] = (float)(tmp_pix_value);
			}
		}
	}

	/**
	 * Finds and sets the threshold value for this frame given the 
	 * user defined percenticle and an ImageProcessor if the thresholdmode is PERCENTILE.
	 * If not, the threshold is set to its absolute (normalized) value. There is only one parameter
	 * used, either percent or aIntensityThreshold depending on the threshold mode.
	 * @param ip ImageProcessor after conversion, normalization and restoration
	 * @param percent the upper rth percentile to be considered as candidate Particles
	 * @param aIntensityThreshold a intensity value which defines a threshold.
	 */
	private void findThreshold(ImageStack ips, double percent, int aIntensityThreshold) {
		if(getThresholdMode() == ABS_THRESHOLD_MODE){
			//the percent parameter corresponds to an absolute value (not percent)
			this.setThreshold((float)(aIntensityThreshold - global_min)/(global_max-global_min));
			return;
		}
		int s, i, j, thold, width;
		width = ips.getWidth();

		/* find this ImageStacks min and max pixel value */
		float min = 0f;
		float max = 0f;
		if(ips.getSize() > 1) {
			StackStatistics sstats = new StackStatistics(new ImagePlus(null,ips));
			min = (float)sstats.min;
			max = (float)sstats.max;
		} else { //speeds up the 2d version:
			ImageStatistics istats = ImageStatistics.getStatistics(ips.getProcessor(1), Measurements.MIN_MAX, null);
			min = (float)istats.min;
			max = (float)istats.max;
		}

		double[] hist = new double[256];
		for (i = 0; i< hist.length; i++) {
			hist[i] = 0;
		}
		for(s = 0; s < ips.getSize(); s++) {
			float[] pixels = (float[])ips.getProcessor(s + 1).getPixels();
			for(i = 0; i < ips.getHeight(); i++) {
				for(j = 0; j < ips.getWidth(); j++) {
					hist[(int)((pixels[i*width+j] - min) * 255.0 / (max - min))]++;
				}
			}				
		}

		for(i = 254; i >= 0; i--)
			hist[i] += hist[i + 1];

		thold = 0;
		while(hist[255 - thold] / hist[0] < percent) {
			thold++;	
			if(thold > 255)
				break;				
		}
		thold = 255 - thold + 1;
		this.setThreshold(((float)(thold / 255.0) * (max - min) + min));		
		//			System.out.println("THRESHOLD: " + this.threshold);
	}	

	/**
	 * Estimates the feature point locations in the given <code>ImageProcessor</code>
	 * <br>Any pixel with the same value before and after dilation and value higher
	 * then the pre calculated threshold is considered as a feature point (Particle).
	 * <br>Adds each found <code>Particle</code> to the <code>particles</code> array.
	 * <br>Mostly adapted from Ingo Oppermann implementation
	 * @param ip ImageProcessor, should be after conversion, normalization and restoration 
	 */
	private void pointLocationsEstimation(ImageStack ips, int frame_number) {
		/* do a grayscale dilation */
		ImageStack dilated_ips = dilateGeneric(ips);
		//		            new StackWindow(new ImagePlus("dilated ", dilated_ips));
		particles = new Vector<Particle>();
		/* loop over all pixels */ 
		int height = ips.getHeight();
		int width = ips.getWidth();
		for ( int s = 0; s < ips.getSize(); s++) {
			float[] ips_pixels = (float[])ips.getProcessor(s+1).getPixels();
			float[] ips_dilated_pixels = (float[])dilated_ips.getProcessor(s+1).getPixels();
			for (int i = 0; i < height; i++){
				for (int j = 0; j < width; j++){  
					if (ips_pixels[i*width+j] > this.threshold && 
							ips_pixels[i*width+j]  == ips_dilated_pixels[i*width+j] ){ //check if pixel is a local maximum

						/* and add each particle that meets the criteria to the particles array */
						//(the starting point is the middle of the pixel and exactly on a focal plane:)
						particles.add(new Particle(i+.5f, j+.5f, s, frame_number));

					} 
				}
			}
		}
		particles_number = particles.size();	        	        
	}

	/**
	 * Dilates a copy of a given ImageProcessor with a pre calculated <code>mask</code>.
	 * Adapted as is from Ingo Oppermann implementation
	 * @param ip ImageProcessor to do the dilation with
	 * @return the dilated copy of the given <code>ImageProcessor</code> 
	 * @see ParticleTracker_#mMask
	 */		
	private ImageStack dilateGeneric(ImageStack ips) {
		FloatProcessor[] dilated_procs = new FloatProcessor[ips.getSize()];
		AtomicInteger z  = new AtomicInteger(-1);
		Vector<Thread> threadsVector = new Vector<Thread>();
		for(int thread_counter = 0; thread_counter < number_of_threads; thread_counter++){
			threadsVector.add(new DilateGenericThread(ips,dilated_procs,z));
		}
		for(Thread t : threadsVector){
			t.start();                               
		}
		for(Thread t : threadsVector){
			try {
				t.join();                                        
			}catch (InterruptedException ie) {
				IJ.showMessage("Calculation interrupted. An error occured in parallel dilation:\n" + ie.getMessage());
			}
		}
		ImageStack dilated_ips = new ImageStack(ips.getWidth(), ips.getHeight());
		for(int s = 0; s < ips.getSize(); s++)
			dilated_ips.addSlice(null, dilated_procs[s]);
		//			new StackWindow(new ImagePlus("dilated image", dilated_ips));

		return dilated_ips;
	}

	private class DilateGenericThread extends Thread{
		ImageStack ips;
		ImageProcessor[] dilated_ips;
		AtomicInteger atomic_z;
		int kernel_width;
		int image_width;
		int image_height;
		int radius;

		public DilateGenericThread(ImageStack is, ImageProcessor[] dilated_is, AtomicInteger z) {
			ips = is;
			dilated_ips = dilated_is;
			atomic_z = z;

			radius = getRadius();
			kernel_width = (getRadius()*2) + 1;
			image_width = ips.getWidth();
			image_height = ips.getHeight();
		}

		public void run() {
			float max;
			int z;
			while((z = atomic_z.incrementAndGet()) < ips.getSize()) {
				//					IJ.showStatus("Dilate Image: " + (z+1));
				//					IJ.showProgress(z, ips.getSize());
				FloatProcessor out_p = new FloatProcessor(image_width, image_height);
				float[] output = (float[])out_p.getPixels();
				float[] dummy_processor = (float[])ips.getPixels(z+1);
				for(int y = 0; y < image_height; y++) {
					for(int x = 0; x < image_width; x++) {
						//little big speed-up:
						if(dummy_processor[y*image_width+x] < threshold) {
							continue;
						}
						max = 0;
						//a,b,c are the kernel coordinates corresponding to x,y,z
						for(int s = -radius; s <= radius; s++ ) {
							if(z + s < 0 || z + s >= ips.getSize())
								continue;
							float[] current_processor_pixels = (float[])ips.getPixels(z+s+1);
							for(int b = -radius; b <= radius; b++ ) {
								if(y + b < 0 || y + b >= ips.getHeight())
									continue;
								for(int a = -radius; a <= radius; a++ ) {
									if(x + a < 0 || x + a >= ips.getWidth())
										continue;
									if(binary_mask[s + radius][(a + radius)* kernel_width+ (b + radius)] == 1) {
										float t;
										if((t = current_processor_pixels[(y + b)* image_width +  (x + a)]) > max) {
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

	private void pointLocationsRefinement(ImageStack ips) {
		int m, k, l, x, y, z, tx, ty, tz;
		float epsx, epsy, epsz, c;

		int mask_width = 2 * radius +1;
		int image_width = ips.getWidth();
		/* Set every value that is smaller than 0 to 0 */		
		for(int s = 0; s < ips.getSize(); s++) {
			//				for (int i = 0; i < ips.getHeight(); i++) {
			//					for (int j = 0; j < ips.getWidth(); j++) {
			//						if(ips.getProcessor(s + 1).getPixelValue(j, i) < 0.0)
			//							ips.getProcessor(s + 1).putPixelValue(j, i, 0.0);
			//
			//					}
			//				}
			float[] pixels = (float[])ips.getPixels(s+1);
			for(int i = 0; i < pixels.length; i++) {
				if(pixels[i] < 0) {
					pixels[i] = 0f;
				}
			}
		}

		/* Loop over all particles */
		for(m = 0; m < this.particles.size(); m++) {
			this.particles.elementAt(m).special = true;
			this.particles.elementAt(m).score = 0.0F;
			epsx = epsy = epsz = 1.0F;

			while (epsx > 0.5 || epsx < -0.5 || epsy > 0.5 || epsy < -0.5 || epsz < 0.5 || epsz > 0.5) {
				this.particles.elementAt(m).nbIterations++;
				this.particles.elementAt(m).m0 = 0.0F;
				this.particles.elementAt(m).m1 = 0.0F;
				this.particles.elementAt(m).m2 = 0.0F;
				this.particles.elementAt(m).m3 = 0.0F;
				this.particles.elementAt(m).m4 = 0.0F;
				epsx = 0.0F;
				epsy = 0.0F;
				epsz = 0.0F;
				for(int s = -radius; s <= radius; s++) {
					if(((int)this.particles.elementAt(m).z + s) < 0 || ((int)this.particles.elementAt(m).z + s) >= ips.getSize())
						continue;
					z = (int)this.particles.elementAt(m).z + s;
					for(k = -radius; k <= radius; k++) {
						if(((int)this.particles.elementAt(m).x + k) < 0 || ((int)this.particles.elementAt(m).x + k) >= ips.getHeight())
							continue;
						x = (int)this.particles.elementAt(m).x + k;

						for(l = -radius; l <= radius; l++) {
							if(((int)this.particles.elementAt(m).y + l) < 0 || ((int)this.particles.elementAt(m).y + l) >= ips.getWidth())
								continue;
							y = (int)this.particles.elementAt(m).y + l;
							//
							//								c = ips.getProcessor(z + 1).getPixelValue(y, x) * (float)mask[s + radius][(k + radius)*mask_width + (l + radius)];
							c = ((float[])(ips.getPixels(z + 1)))[x*image_width+y] * (float)mask[s + radius][(k + radius)*mask_width + (l + radius)];

							this.particles.elementAt(m).m0 += c;
							epsx += (float)k * c;
							epsy += (float)l * c;
							epsz += (float)s * c;
							this.particles.elementAt(m).m2 += (float)(k * k + l * l + s * s) * c;
							this.particles.elementAt(m).m1 += (float)Math.sqrt(k * k + l * l + s * s) * c;
							this.particles.elementAt(m).m3 += (float)Math.pow(k * k + l * l + s * s, 1.5f) * c;
							this.particles.elementAt(m).m4 += (float)Math.pow(k * k + l * l + s * s, 2) * c;								
						}
					}
				}

				epsx /= this.particles.elementAt(m).m0;
				epsy /= this.particles.elementAt(m).m0;
				epsz /= this.particles.elementAt(m).m0;
				this.particles.elementAt(m).m2  /= this.particles.elementAt(m).m0;
				this.particles.elementAt(m).m1  /= this.particles.elementAt(m).m0;
				this.particles.elementAt(m).m3  /= this.particles.elementAt(m).m0;
				this.particles.elementAt(m).m4  /= this.particles.elementAt(m).m0;

				// This is a little hack to avoid numerical inaccuracy
				tx = (int)(10.0 * epsx);
				ty = (int)(10.0 * epsy);
				tz = (int)(10.0 * epsz);

				if((float)(tx)/10.0 > 0.5) {
					if((int)this.particles.elementAt(m).x + 1 < ips.getHeight())
						this.particles.elementAt(m).x++;
				}
				else if((float)(tx)/10.0 < -0.5) {
					if((int)this.particles.elementAt(m).x - 1 >= 0)
						this.particles.elementAt(m).x--;						
				}
				if((float)(ty)/10.0 > 0.5) {
					if((int)this.particles.elementAt(m).y + 1 < ips.getWidth())
						this.particles.elementAt(m).y++;
				}
				else if((float)(ty)/10.0 < -0.5) {
					if((int)this.particles.elementAt(m).y - 1 >= 0)
						this.particles.elementAt(m).y--;
				}
				if((float)(tz)/10.0 > 0.5) {
					if((int)this.particles.elementAt(m).z + 1 < ips.getSize())
						this.particles.elementAt(m).z++;
				}
				else if((float)(tz)/10.0 < -0.5) {
					if((int)this.particles.elementAt(m).z - 1 >= 0)
						this.particles.elementAt(m).z--;
				}

				if((float)(tx)/10.0 <= 0.5 && (float)(tx)/10.0 >= -0.5 && 
						(float)(ty)/10.0 <= 0.5 && (float)(ty)/10.0 >= -0.5 &&
						(float)(tz)/10.0 <= 0.5 && (float)(tz)/10.0 >= -0.5)
					break;
			}
			//				System.out.println("iterations for particle " + m + ": " + this.particles.elementAt(m).nbIterations);
			this.particles.elementAt(m).x += epsx;
			this.particles.elementAt(m).y += epsy;
			this.particles.elementAt(m).z += epsz;
		}					
	}

	/**
	 * Rejects spurious particles detections such as unspecific signals, dust, or particle aggregates. 
	 * <br>The implemented classification algorithm after Crocker and Grier [68] is based on the
	 * intensity moments of orders 0 and 2.
	 * <br>Particles with lower final score than the user-defined cutoff are discarded 
	 * <br>Adapted "as is" from Ingo Oppermann implementation
	 */
	private void nonParticleDiscrimination() {

		int j, k;
		double score;
		this.real_particles_number = this.particles_number;
		if(this.particles.size() == 1){
			this.particles.elementAt(0).score = Float.MAX_VALUE;
		}
		for(j = 0; j < this.particles.size(); j++) {		
			//				int accepted = 1;
			for(k = j + 1; k < this.particles.size(); k++) {
				score = (double)((1.0 / (2.0 * Math.PI * 0.1 * 0.1)) * 
						Math.exp(-(this.particles.elementAt(j).m0 - this.particles.elementAt(k).m0) *
								(this.particles.elementAt(j).m0 - this.particles.elementAt(k).m0) / (2.0 * 0.1) -
								(this.particles.elementAt(j).m2 - this.particles.elementAt(k).m2) * 
								(this.particles.elementAt(j).m2 - this.particles.elementAt(k).m2) / (2.0 * 0.1)));
				this.particles.elementAt(j).score += score;
				this.particles.elementAt(k).score += score;
			}
			if(this.particles.elementAt(j).score < cutoff) {
				this.particles.elementAt(j).special = false;
				this.real_particles_number--;		
				//					accepted = 0;
			}
			//				System.out.println(j + "\t" + this.particles.elementAt(j).m0 + "\t" + this.particles.elementAt(j).m2 + "\t" + accepted);
		}				
	}

	/**
	 * removes particles that were discarded by the <code>nonParticleDiscrimination</code> method
	 * from the particles array. 
	 * <br>Non particles will be removed from the <code>particles</code> array so if their info is 
	 * needed, it should be saved before calling this method
	 * @see MyFrame#nonParticleDiscrimination()
	 */
	private void removeNonParticle() {

		//	    	Particle[] new_particles = new Particle[this.real_particles_number];
		//	    	int new_par_index = 0;
		//	    	for (int i = 0; i< this.particles.length; i++) {
		//	    		if (this.particles[i].special) {
		//	    			new_particles[new_par_index] = this.particles[i];
		//	    			new_par_index++;
		//	    		}
		//	    	}
		//	    	this.particles = new_particles;
		for(int i = this.particles.size()-1; i >= 0; i--) {
			if(!this.particles.elementAt(i).special) {
				this.particles.removeElementAt(i);
			}
		}
	}

	private static ImageStack GetSubStackInFloat(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if(startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for(int i = startPos; i <= endPos; i++) {
			res.addSlice(is.getSliceLabel(i), is.getProcessor(i).convertToFloat());
		}
		return res;
	}

	/**
	 * Corrects imperfections in the given <code>ImageStack</code> by
	 * convolving it (slice by slice, not 3D) with the pre calculated <code>kernel</code>
	 * @param is ImageStack to be restored
	 * @return the restored <code>ImageProcessor</code>
	 * @see Convolver#convolve(ij.process.ImageProcessor, float[], int, int)
	 * @see ParticleTracker_#kernel
	 */
	private ImageStack imageRestoration(ImageStack is) {  
		//remove the clutter
		ImageStack restored = null; 

		//pad the imagestack 	
		if(is.getSize() > 1) {
			//3D mode (we also have to convolve and pad in third dimension)
			restored = padImageStack3D(is);
		} else {
			//we're in 2D mode (separated to do no unnecessary operations).
			ImageProcessor rp= padImageStack2D(is.getProcessor(1));
			restored = new ImageStack(rp.getWidth(), rp.getHeight());
			restored.addSlice("", rp);
		}

		switch(preprocessing_mode){
		case NO_PREPROCESSING:
			GaussBlur3D(restored, 1*lambda_n);
			break;
		case BOX_CAR_AVG:		
			// There was found to be a 2*lambda_n for the sigma of the Gaussian kernel. 
			// Set it back to 1*lambda_n to match the 2D implementation.
			GaussBlur3D(restored, 1*lambda_n);
			//				new StackWindow(new ImagePlus("convolved 3d",GetSubStackCopyInFloat(restored, 1, restored.getSize())));

			boxCarBackgroundSubtractor(restored);//TODO:3D? ->else: pad!
			//				new StackWindow(new ImagePlus("after bg subtraction",GetSubStackCopyInFloat(restored, 1, restored.getSize())));

			break;
		case BG_SUBTRACTOR:
			GaussBlur3D(restored, 1*lambda_n);
			BackgroundSubtractor2_ bgSubtractor = new BackgroundSubtractor2_();
			for(int s = 1; s <= restored.getSize(); s++) {
				//					IJ.showProgress(s, restored.getSize());
				//					IJ.showStatus("Preprocessing: subtracting background...");
				bgSubtractor.SubtractBackground(restored.getProcessor(s), radius*4);
			}
			break;
		case LAPLACE_OP:
			//remove noise then do the laplace op
			GaussBlur3D(restored, 1*lambda_n);
			repadImageStack3D(restored);
			restored = Laplace_Separable_3D(restored);
			break;
		default:
			break;
		}
		if(is.getSize() > 1) {
			//again, 3D crop
			restored = cropImageStack3D(restored);
		} else {
			//2D crop
			ImageProcessor rp= cropImageStack2D(restored.getProcessor(1));
			restored = new ImageStack(rp.getWidth(), rp.getHeight());
			restored.addSlice("", rp);
		}
		//			new StackWindow(new ImagePlus("restored", GetSubStackCopyInFloat(restored, 1, restored.getSize())));            
		return restored;


	}

	private ImageProcessor cropImageStack2D(ImageProcessor ip) 
	{
		int width = ip.getWidth();
		int newWidth = width - 2*radius;
		FloatProcessor cropped_proc = new FloatProcessor(ip.getWidth()-2*radius, ip.getHeight()-2*radius);
		float[] croppedpx = (float[])cropped_proc.getPixels();
		float[] origpx = (float[])ip.getPixels();
		int offset = radius*width;
		for(int i = offset, j = 0; j < croppedpx.length; i++, j++) {
			croppedpx[j] = origpx[i];		
			if(j%newWidth == 0 || j%newWidth == newWidth - 1) {
				i+=radius;
			}
		}
		return cropped_proc;
	}

	/**
	 * crops a 3D image at all of sides of the imagestack cube. 
	 * @param is a frame to crop
	 * @see pad ImageStack3D
	 * @return the cropped image
	 */
	private ImageStack cropImageStack3D(ImageStack is) {
		ImageStack cropped_is = new ImageStack(is.getWidth()-2*radius, is.getHeight()-2*radius);
		for(int s = radius + 1; s <= is.getSize()-radius; s++) {
			cropped_is.addSlice("", cropImageStack2D(is.getProcessor(s)));
		}
		return cropped_is;
	}

	private ImageProcessor padImageStack2D(ImageProcessor ip) {
		int width = ip.getWidth();
		int newWidth = width + 2*radius;
		FloatProcessor padded_proc = new FloatProcessor(ip.getWidth() + 2*radius, ip.getHeight() + 2*radius);
		float[] paddedpx = (float[])padded_proc.getPixels();
		float[] origpx = (float[])ip.getPixels();
		//first r pixel lines
		for(int i = 0; i < radius*newWidth; i++) {
			if(i%newWidth < radius) { 			//right corner
				paddedpx[i] = origpx[0];
				continue;
			}
			if(i%newWidth >= radius + width) {
				paddedpx[i] = origpx[width-1];	//left corner
				continue;
			}
			paddedpx[i] = origpx[i%newWidth-radius];
		}

		//the original pixel lines and left & right edges				
		for(int i = 0, j = radius*newWidth; i < origpx.length; i++,j++) {
			int xcoord = i%width;
			if(xcoord==0) {//add r pixel rows (left)
				for(int a = 0; a < radius; a++) {
					paddedpx[j] = origpx[i];
					j++;
				}
			}
			paddedpx[j] = origpx[i];
			if(xcoord==width-1) {//add r pixel rows (right)
				for(int a = 0; a < radius; a++) {
					j++;
					paddedpx[j] = origpx[i];
				}
			}
		}

		//last r pixel lines
		int lastlineoffset = origpx.length-width;
		for(int j = (radius+ip.getHeight())*newWidth, i = 0; j < paddedpx.length; j++, i++) {
			if(i%width == 0) { 			//left corner
				for(int a = 0; a < radius; a++) {
					paddedpx[j] = origpx[lastlineoffset];
					j++;
				}
				//					continue;
			}
			if(i%width == width-1) {	
				for(int a = 0; a < radius; a++) {
					paddedpx[j] = origpx[lastlineoffset+width-1];	//right corner
					j++;
				}
				//					continue;
			}
			paddedpx[j] = origpx[lastlineoffset + i % width];
		}
		return padded_proc;
	}

	/**
	 * Before convolving, the image is padded such that no artifacts occure at the edge of an image.
	 * @param is a frame (not a movie!)
	 * @see cropImageStack3D(ImageStack)
	 * @return the padded imagestack to (w+2*r, h+2r, s+2r) by copying the last pixel row/line/slice
	 */
	private ImageStack padImageStack3D(ImageStack is) 
	{
		ImageStack padded_is = new ImageStack(is.getWidth() + 2*radius, is.getHeight() + 2*radius);
		for(int s = 0; s < is.getSize(); s++){
			ImageProcessor padded_proc = padImageStack2D(is.getProcessor(s+1));
			//if we are on the top or bottom of the stack, add r slices
			if(s == 0 || s == is.getSize() - 1) {
				for(int i = 0; i < radius; i++) {
					padded_is.addSlice("", padded_proc);
				}
			} 
			padded_is.addSlice("", padded_proc);
		}

		return padded_is;
	}
	/**
	 * Does the same as padImageStack3D but does not create a new image. It recreates the edge of the
	 * cube (frame).
	 * @see padImageStack3D, cropImageStack3D
	 * @param aIS
	 */
	private void repadImageStack3D(ImageStack aIS){
		if(aIS.getSize() > 1) { //only in the 3D case
			for(int s = 1; s <= radius; s++) {
				aIS.deleteSlice(1);
				aIS.deleteLastSlice();
			}
		}
		for(int s = 1; s <= aIS.getSize(); s++) {
			float[] pixels = (float[])aIS.getProcessor(s).getPixels();
			int width = aIS.getWidth();
			int height = aIS.getHeight();
			for(int i = 0; i < pixels.length; i++) {
				int xcoord = i%width;
				int ycoord = i/width;
				if(xcoord < radius && ycoord < radius) {
					pixels[i] = pixels[radius*width+radius];
					continue;
				}
				if(xcoord < radius && ycoord >= height-radius) {
					pixels[i] = pixels[(height-radius-1)*width+radius];
					continue;
				}
				if(xcoord >= width-radius && ycoord < radius) {
					pixels[i] = pixels[(radius + 1) * width - radius - 1];
					continue;
				}
				if(xcoord >= width-radius && ycoord >= height-radius) {
					pixels[i] = pixels[(height-radius)*width - radius - 1];
					continue;
				}
				if(xcoord < radius) {
					pixels[i] = pixels[ycoord*width+radius];
					continue;
				}
				if(xcoord >= width - radius) {
					pixels[i] = pixels[(ycoord+1)*width - radius - 1];
					continue;
				}
				if(ycoord < radius) {
					pixels[i] = pixels[radius*width + xcoord];
					continue;
				}
				if(ycoord >= height-radius) {
					pixels[i] = pixels[(height-radius-1)*width + xcoord];
				}
			}
		}
		if(aIS.getSize() > 1) {
			for(int s = 1; s <= radius; s++) { //only in 3D case
				aIS.addSlice("", aIS.getProcessor(1).duplicate(),1);
				aIS.addSlice("", aIS.getProcessor(aIS.getSize()).duplicate());
			}
		}
	}


	private void GaussBlur3D(ImageStack is, float aSigma) {
		float[] vKernel = CalculateNormalizedGaussKernel(aSigma);
		int kernel_radius = vKernel.length / 2;
		int nSlices = is.getSize();
		int vWidth = is.getWidth();
		for(int i = 1; i <= nSlices; i++){
			ImageProcessor restored_proc = is.getProcessor(i);
			Convolver convolver = new Convolver();
			// no need to normalize the kernel - its already normalized
			convolver.setNormalize(false);
			//the gaussian kernel is separable and can done in 3x 1D convolutions!
			convolver.convolve(restored_proc, vKernel, vKernel.length , 1);  
			convolver.convolve(restored_proc, vKernel, 1 , vKernel.length);  
		}
		//2D mode, abort here; the rest is unnecessary
		if(is.getSize() == 1) {
			return;
		}			

		//TODO: which kernel? since lambda_n = 1 pixel, it does not depend on the resolution -->not rescale
		//rescale the kernel for z dimension
		//			vKernel = CalculateNormalizedGaussKernel((float)(aRadius / (original_imp.getCalibration().pixelDepth / original_imp.getCalibration().pixelWidth)));

		kernel_radius = vKernel.length / 2;
		//to speed up the method, store the processor in an array (not invoke getProcessor()):
		float[][] vOrigProcessors = new float[nSlices][];
		float[][] vRestoredProcessors = new float[nSlices][];
		for(int s = 0; s < nSlices; s++) {
			vOrigProcessors[s] = (float[])is.getProcessor(s + 1).getPixelsCopy();
			vRestoredProcessors[s] = (float[])is.getProcessor(s + 1).getPixels();
		}
		//begin convolution with 1D gaussian in 3rd dimension:
		for(int y = kernel_radius; y < is.getHeight() - kernel_radius; y++){
			for(int x = kernel_radius; x < is.getWidth() - kernel_radius; x++){
				for(int s = kernel_radius + 1; s <= is.getSize() - kernel_radius; s++) {
					float sum = 0;
					for(int i = -kernel_radius; i <= kernel_radius; i++) {	        				
						sum += vKernel[i + kernel_radius] * vOrigProcessors[s + i - 1][y*vWidth+x];
					}
					vRestoredProcessors[s-1][y*vWidth+x] = sum;
				}
			}
		}
	}

	public void boxCarBackgroundSubtractor(ImageStack is) {
		Convolver convolver = new Convolver();
		float[] kernel = new float[radius * 2 +1];
		int n = kernel.length;
		for(int i = 0; i < kernel.length; i++)
			kernel[i] = 1f/(float)n;
		for(int s = 1; s <= is.getSize(); s++) {
			ImageProcessor bg_proc = is.getProcessor(s).duplicate();
			convolver.convolveFloat(bg_proc, kernel, 1, n);
			convolver.convolveFloat(bg_proc, kernel, n, 1);
			is.getProcessor(s).copyBits(bg_proc, 0, 0, Blitter.SUBTRACT);
		}
	}

	private ImageStack Laplace_Separable_3D(ImageStack aIS) {        
		float[] vKernel_1D = new float[]{-1, 2, -1};
		ImageStack vResultStack = new ImageStack(aIS.getWidth(), aIS.getHeight());
		int vKernelWidth = vKernel_1D.length;
		int vKernelRadius = vKernel_1D.length / 2;
		int vWidth = aIS.getWidth();
		//
		//in x dimension
		//
		for(int vI = 1; vI <= aIS.getSize(); vI++){
			ImageProcessor vConvolvedSlice = aIS.getProcessor(vI).duplicate();
			Convolver vConvolver = new Convolver();
			vConvolver.setNormalize(false);
			vConvolver.convolve(vConvolvedSlice, vKernel_1D, vKernelWidth , 1);  
			vResultStack.addSlice(null, vConvolvedSlice);
		}
		//
		//in y dimension and sum it to the result
		//
		for(int vI = 1; vI <= aIS.getSize(); vI++){
			ImageProcessor vConvolvedSlice = aIS.getProcessor(vI).duplicate();
			Convolver vConvolver = new Convolver();
			vConvolver.setNormalize(false);
			vConvolver.convolve(vConvolvedSlice, vKernel_1D, 1 , vKernelWidth);  
			vResultStack.getProcessor(vI).copyBits(vConvolvedSlice, 0, 0, Blitter.ADD);
		}
		//			if(true) return vResultStack; //TODO: abort here? yes if gauss3d is scaled in z

		//
		//z dimension
		//
		//first get all the processors of the frame in an array since the getProcessor method is expensive
		float[][] vOriginalStackPixels = new float[aIS.getSize()][];
		float[][] vConvolvedStackPixels = new float[aIS.getSize()][];
		float[][] vResultStackPixels = new float[aIS.getSize()][];
		for(int vS = 0; vS < aIS.getSize(); vS++) {
			vOriginalStackPixels[vS] = (float[])aIS.getProcessor(vS + 1).getPixels();			
			vConvolvedStackPixels[vS] = (float[])aIS.getProcessor(vS + 1).getPixelsCopy();
			vResultStackPixels[vS] = (float[])vResultStack.getProcessor(vS + 1).getPixels();
		}
		for(int vY = 0; vY < aIS.getHeight(); vY++){
			for(int vX = 0; vX < aIS.getWidth(); vX++){
				for(int vS = vKernelRadius; vS < aIS.getSize() - vKernelRadius; vS++) {
					float vSum = 0;
					for(int vI = -vKernelRadius; vI <= vKernelRadius; vI++) {
						vSum += vKernel_1D[vI + vKernelRadius] * vOriginalStackPixels[vS + vI][vY*vWidth+vX];
					}
					vConvolvedStackPixels[vS][vY*vWidth+vX] = vSum;
				}
			}
		}
		//add the results
		for(int vS = vKernelRadius; vS < aIS.getSize() - vKernelRadius; vS++){
			for(int vI = 0; vI < vResultStackPixels[vS].length; vI++){
				vResultStackPixels[vS][vI] += vConvolvedStackPixels[vS][vI];
			}
		}
		//			new StackWindow(new ImagePlus("after laplace copy",GetSubStackCopyInFloat(vResultStack, 1, vResultStack.getSize())));
		return vResultStack;	        
	}

	public float[] CalculateNormalizedGaussKernel(float aSigma){
		int vL = (int)aSigma * 3 * 2 + 1;
		if(vL < 3) vL = 3;
		float[] vKernel = new float[vL];
		int vM = vKernel.length/2;
		for(int vI = 0; vI < vM; vI++){
			vKernel[vI] = (float)(1f/(2f*Math.PI*aSigma*aSigma) * Math.exp(-(float)((vM-vI)*(vM-vI))/(2f*aSigma*aSigma)));
			vKernel[vKernel.length - vI - 1] = vKernel[vI];
		}
		vKernel[vM] = (float)(1f/(2f*Math.PI*aSigma*aSigma));

		//normalize the kernel numerically:
		float vSum = 0;
		for(int vI = 0; vI < vKernel.length; vI++){
			vSum += vKernel[vI];
		}
		float vScale = 1.0f/vSum;
		for(int vI = 0; vI < vKernel.length; vI++){
			vKernel[vI] *= vScale;
		}
		return vKernel;
	}

	private static ImageStack GetSubStackCopyInFloat(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if(startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for(int i = startPos; i <= endPos; i++) {
			res.addSlice(is.getSliceLabel(i), is.getProcessor(i).convertToFloat().duplicate());
		}
		return res;
	}


	/**
	 * (Re)Initialize the binary and weighted masks. This is necessary if the radius changed.
	 * The memory allocations are performed in advance (in this method) for efficiency reasons.
	 * @param mask_radius
	 */
	public void generateMasks(int mask_radius){
		//the binary mask can be calculated already:
		int width = (2 * mask_radius) + 1;
		this.binary_mask = new short[width][width*width];
		generateBinaryMask(mask_radius);

		//the weighted mask is just initialized with the new radius:
		this.weighted_mask = new float[width][width*width];

		//standard boolean mask
		generateMask(mask_radius);

	}

	/**
	 * Generates the dilation mask
	 * <code>mask</code> is a var of class ParticleTracker_ and its modified internally here
	 * Adapted from Ingo Oppermann implementation
	 * @param mask_radius the radius of the mask (user defined)
	 */
	public void generateBinaryMask(int mask_radius) {    	
		int width = (2 * mask_radius) + 1;
		for(int s = -mask_radius; s <= mask_radius; s++){
			for(int i = -mask_radius; i <= mask_radius; i++) {
				for(int j = -mask_radius; j <= mask_radius; j++) {
					int index = coord(i + mask_radius, j + mask_radius, width);
					if((i * i) + (j * j) + (s * s) <= mask_radius * mask_radius)
						this.binary_mask[s + mask_radius][index] = 1;
					else
						this.binary_mask[s + mask_radius][index] = 0;

				}
			}
		}
		//    	System.out.println("mask crated");
	}

	/**
	 * Generates the dilation mask
	 * <code>mask</code> is a var of class ParticleTracker_ and its modified internally here
	 * Adapted from Ingo Oppermann implementation
	 * @param mask_radius the radius of the mask (user defined)
	 */
	public void generateMask(int mask_radius) {    	

		int width = (2 * mask_radius) + 1;
		this.mask = new int[width][width*width];
		for(int s = -mask_radius; s <= mask_radius; s++){
			for(int i = -mask_radius; i <= mask_radius; i++) {
				for(int j = -mask_radius; j <= mask_radius; j++) {
					int index = coord(i + mask_radius, j + mask_radius, width);
					if((i * i) + (j * j) + (s * s) <= mask_radius * mask_radius)
						this.mask[s + mask_radius][index] = 1;
					else
						this.mask[s + mask_radius][index] = 0;

				}
			}
		}
	}

	public void generateWeightedMask_old(int mask_radius, float xCenter, float yCenter, float zCenter) {
		int width = (2 * mask_radius) + 1;
		for(int iz = -mask_radius; iz <= mask_radius; iz++){
			for(int iy = -mask_radius; iy <= mask_radius; iy++) {
				for(int ix = -mask_radius; ix <= mask_radius; ix++) {
					int index = coord(iy + mask_radius, ix + mask_radius, width);

					float distPxToCenter = (float) Math.sqrt((xCenter-ix)*(xCenter-ix)+(yCenter-iy)*(yCenter-iy)+(zCenter-iz)*(zCenter-iz)); 

					//the weight is approximative the amount of the voxel inside the (spherical) mask.
					float weight = (float)mask_radius - distPxToCenter + .5f; 
					if(weight < 0) {
						weight = 0f;    				
					} 
					if(weight > 1) {
						weight = 1f;
					}
					this.weighted_mask[iz + mask_radius][index] = weight;
				}
			}
		}
	}

	public void generateWeightedMask_2D(int mask_radius, float xCenter, float yCenter, float zCenter) {
		int width = (2 * mask_radius) + 1;
		float pixel_radius = 0.5f;		
		float r = pixel_radius;
		float R = mask_radius;
		for(int iy = -mask_radius; iy <= mask_radius; iy++) {
			for(int ix = -mask_radius; ix <= mask_radius; ix++) {
				int index = coord(iy + mask_radius, ix + mask_radius, width);

				float distPxCenterToMaskCenter = (float) Math.sqrt((xCenter-ix)*(xCenter-ix)+(yCenter-iy)*(yCenter-iy)); 
				float d = distPxCenterToMaskCenter;
				//the weight is approximative the amount of the voxel inside the (spherical) mask. See formula 
				//http://mathworld.wolfram.com/Circle-CircleIntersection.html
				float weight = 0;
				if(distPxCenterToMaskCenter < mask_radius + pixel_radius){
					weight = 1;

					if(mask_radius < distPxCenterToMaskCenter + pixel_radius) {
						float v = (float) (pixel_radius*pixel_radius*
								Math.acos((d*d+r*r-R*R)/(2*d*r))
								+R*R*Math.acos((d*d+R*R-r*r)/(2*d*R))
								-0.5f*Math.sqrt((-d+r+R)*(d+r-R)*(d-r+R)*(d+r+R)));

						weight =  (v / ((float)Math.PI * pixel_radius*pixel_radius));
					}
				}

				for(int iz = -mask_radius; iz <= mask_radius; iz++){
					this.weighted_mask[iz + mask_radius][index] = weight;
				}
			}
		}


	}

	public void generateWeightedMask_3D(int mask_radius, float xCenter, float yCenter, float zCenter) {
		int width = (2 * mask_radius) + 1;
		float voxel_radius = 0.5f;
		for(int iz = -mask_radius; iz <= mask_radius; iz++){
			for(int iy = -mask_radius; iy <= mask_radius; iy++) {
				for(int ix = -mask_radius; ix <= mask_radius; ix++) {
					int index = coord(iy + mask_radius, ix + mask_radius, width);

					float distPxCenterToMaskCenter = (float) Math.sqrt((xCenter-ix+.5f)*(xCenter-ix+.5f)+(yCenter-iy+.5f)*(yCenter-iy+.5f)+(zCenter-iz+.5f)*(zCenter-iz+.5f)); 

					//the weight is approximative the amount of the voxel inside the (spherical) mask.
					float weight = 0; 
					if(distPxCenterToMaskCenter < mask_radius + voxel_radius){
						weight = 1;

						if(mask_radius < distPxCenterToMaskCenter + voxel_radius) {

							//The volume is given by http://mathworld.wolfram.com/Sphere-SphereIntersection.html
							float v = (float) (Math.PI*Math.pow(voxel_radius + mask_radius - distPxCenterToMaskCenter ,2)
									*(distPxCenterToMaskCenter * distPxCenterToMaskCenter +2 * distPxCenterToMaskCenter * mask_radius 
											- 3 * mask_radius * mask_radius + 2 * distPxCenterToMaskCenter * voxel_radius 
											+ 6 * mask_radius * voxel_radius - 3 * voxel_radius * voxel_radius) 
											/ (12 * distPxCenterToMaskCenter));
							weight = (float) (v / ((4f*Math.PI/3f)*Math.pow(voxel_radius,3)));
						}
					}
					this.weighted_mask[iz + mask_radius][index] = weight;
				}
			}
		}
	}

	/**
	 * Returns a * c + b
	 * @param a: y-coordinate
	 * @param b: x-coordinate
	 * @param c: width
	 * @return
	 */
	private int coord (int a, int b, int c) {
		return (((a) * (c)) + (b));
	}
	
	/**
	 * Sets user defined params that are necessary to particle detection
	 * and generates the <code>mask</code> according to these params
	 * @see #generateMasks(int)
	 */
	public void setUserDefinedParameters(double cutoff, float percentile, int radius, int intThreshold) {
		this.cutoff = cutoff;
		this.percentile = percentile;
		this.absIntensityThreshold = intThreshold;
		//    	this.preprocessing_mode = mode;
		//    	this.setThresholdMode(thsmode);
		this.radius = radius;
		// create Mask for Dilation with the user defined radius
		generateMasks(this.radius);
	}

	public void addUserDefinedParametersDialog (GenericDialog gd) {
		gd.addMessage("Particle Detection:");
		// These 3 params are only relevant for non text_files_mode
		gd.addNumericField("Radius", radius, 0);
		gd.addNumericField("Cutoff", cutoff, 1);

		//	        gd.addChoice("Threshold mode", new String[]{"Absolute Threshold","Percentile"}, "Percentile");
		//	        ((Choice)gd.getChoices().firstElement()).addItemListener(new ItemListener(){
		//				public void itemStateChanged(ItemEvent e) {
		//					int mode = 0;
		//					if(e.getItem().toString().equals("Absolute Threshold")) {
		//						mode = ABS_THRESHOLD_MODE;						
		//					}
		//					if(e.getItem().toString().equals("Percentile")) {
		//						mode = PERCENTILE_MODE;						
		//					}
		//					thresholdModeChanged(mode);
		//				}});

		//	        gd.addNumericField("Percentile", 0.001, 5);
		//	        gd.addNumericField("Percentile / Abs.Threshold", 0.1, 5, 6, " % / Intensity");
		gd.addNumericField("Percentile", percentile*100, 5, 6, " %");

		//	        gd.addPanel(makeThresholdPanel(), GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
		//	        gd.addChoice("Preprocessing mode", new String[]{"none", "box-car avg.", "BG Subtraction", "Laplace Operation"}, "box-car avg.");	        
	}

	/**
	 * gd has to be shown with showDialog and handles the fields added to the dialog
	 * with addUserDefinedParamtersDialog(gd).
	 * @param gd	<code>GenericDialog</code> at which the UserDefinedParameter fields where added.
	 * @return		true if user changed the parameters and false if the user didn't changed them.
	 */
	public Boolean getUserDefinedParameters(GenericDialog gd) {
		int rad = (int)gd.getNextNumber();
		//        	this.radius = (int)gd.getNextNumber();
		double cut = gd.getNextNumber(); 
		//            this.cutoff = gd.getNextNumber();   
		float per = ((float)gd.getNextNumber())/100;
		int intThreshold = (int)(per*100+0.5);
		//            this.percentile = ((float)gd.getNextNumber())/100;

		//        	int thsmode = gd.getNextChoiceIndex();
		//        	setThresholdMode(thsmode);

		//        	int mode = gd.getNextChoiceIndex();

		Boolean changed = (rad != radius || cut != cutoff  || (per != percentile));// && intThreshold != absIntensityThreshold || mode != getThresholdMode() || thsmode != getThresholdMode();
		setUserDefinedParameters(cut, per, rad, intThreshold);
		//        	this.preprocessing_mode = mode;
		return changed;
	}
	
	/**
	 * Creates the preview panel that gives the options to preview and save the detected particles,
	 * and also a scroll bar to navigate through the slices of the movie
	 * <br>Buttons and scrollbar created here use this ParticleTracker_ as <code>ActionListener</code>
	 * and <code>AdjustmentListener</code>
	 * @return the preview panel
	 */

	public Panel makePreviewPanel(final PreviewInterface previewHandler, final ImagePlus img) {

		Panel preview_panel = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		preview_panel.setLayout(gridbag);  
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;

		/* scroll bar to navigate through the slices of the movie */
		final Scrollbar preview_scrollbar = new Scrollbar(Scrollbar.HORIZONTAL, img.getCurrentSlice(), 1, 1, img.getStackSize()+1);
		preview_scrollbar.addAdjustmentListener( new AdjustmentListener() {	
			public void adjustmentValueChanged(AdjustmentEvent e) {
				// set the current visible slice to the one selected on the bar
				img.setSlice(preview_scrollbar.getValue());
				// update the preview view to this silce
				//			this.preview();
			}
		});
		preview_scrollbar.setUnitIncrement(1); 
		preview_scrollbar.setBlockIncrement(1);

		/* button to generate preview of the detected particles */
		Button preview = new Button("Preview Detected");
		preview.addActionListener( new ActionListener() {
            						public void actionPerformed(ActionEvent e) {
            							previewHandler.preview(e);
            						}
        });

		/* button to save the detected particles */
		Button 	save_detected = new Button("Save Detected");
		save_detected.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				previewHandler.saveDetected(e);
			}
		});
		Label seperation = new Label("______________", Label.CENTER); 
		gridbag.setConstraints(preview, c);
		preview_panel.add(preview);
		gridbag.setConstraints(preview_scrollbar, c);	        
		preview_panel.add(preview_scrollbar);
		gridbag.setConstraints(save_detected, c);
		preview_panel.add(save_detected);
		previewLabel = new Label("");
		gridbag.setConstraints(previewLabel, c);
		preview_panel.add(previewLabel);
		gridbag.setConstraints(seperation, c);
		preview_panel.add(seperation);
		return preview_panel;
	}
	
	public void setPreviewLabel(String previewLabelText) {
		this.previewLabel.setText(previewLabelText);
	}
	
	void setThreshold(float threshold) {
		this.threshold = threshold;
	}


	float getThreshold() {
		return threshold;
	}

	int getThresholdMode() {
		return threshold_mode;
	}


	public int getRadius() {
		return radius;
	}

}

