package mosaic.core.detection;


import java.util.Vector;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Measurements;
import ij.plugin.filter.Convolver;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;
import mosaic.core.utils.MosaicImageProcessingTools;
import mosaic.core.utils.MosaicUtils;


/**
 * FeaturePointDetector detects the "real" particles in provided frames.
 */
public class FeaturePointDetector {
    public static enum Mode { ABS_THRESHOLD_MODE, PERCENTILE_MODE }

    private float iGlobalMax;
    private float iGlobalMin;
    
    private Vector<Particle> particles;
    private int particles_number; // number of particles initialy detected
    private int real_particles_number; // number of "real" particles discrimination

    /* user defined parameters */
    private int radius = 3; // default
    private double cutoff = 3.0; // default
    private float percentile = 0.001F; // default (user input/100)
    private float absIntensityThreshold = 0.0f; // user input
    public Mode threshold_mode = Mode.PERCENTILE_MODE;

    private int[][] mask;
    
    public FeaturePointDetector(float aGlobalMax, float aGlobalMin) {
        iGlobalMax = aGlobalMax;
        iGlobalMin = aGlobalMin;
        generateDilationMasks(this.radius);
    }

    /**
     * First phase of the algorithm - time and memory consuming !! <br>
     * Determines the "real" particles in this frame (only for frame constructed from Image) <br>
     * Converts the <code>original_ip</code> to <code>FloatProcessor</code>, normalizes it, convolutes and dilates it,
     * finds the particles, refine their position and filters out non particles
     * 
     * @see ImageProcessor#convertToFloat()
     */
    public void featurePointDetection(MyFrame frame) {
        /*
         * Converting the original imageProcessor to float
         * This is a constraint caused by the lack of floating point precision of pixels
         * value in 16bit and 8bit image processors in ImageJ therefore, if the image is not
         * converted to 32bit floating point, false particles get detected
         */
        final ImageStack original_ips = frame.getOriginalImageStack();
        ImageStack restored_fps = new ImageStack(original_ips.getWidth(), original_ips.getHeight());

        for (int i = 1; i <= original_ips.getSize(); i++) {
            // if it is already a float, ImageJ does not create a duplicate
            restored_fps.addSlice(null, original_ips.getProcessor(i).convertToFloat().duplicate());
        }

        /* The algorithm is initialized by normalizing the frame */
        normalizeFrameFloat(restored_fps, iGlobalMin, iGlobalMax);
        // new StackWindow(new ImagePlus("after normalization",restored_fps));

        /* Image Restoration - Step 1 of the algorithm */
        restored_fps = imageRestoration(restored_fps);
        // new StackWindow(new ImagePlus("after restoration",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored_fps, 1, restored_fps.getSize())));

        /* Estimation of the point location - Step 2 of the algorithm */
        pointLocationsEstimation(restored_fps, frame.frame_number, frame.linkrange);
        
        /* Refinement of the point location - Step 3 of the algorithm */
        pointLocationsRefinement(restored_fps);
        
        /* Non Particle Discrimination(set a flag to particles) - Step 4 of the algorithm */
        nonParticleDiscrimination();

        /* Save frame information before particle discrimination/deletion - it will be lost otherwise */
        frame.setParticles(particles, particles_number);
        frame.generateFrameInfoBeforeDiscrimination();

        /* remove all the "false" particles from particles array */
        removeNonParticle();
        frame.setParticles(particles, real_particles_number);

        /* Set the real_particle_number */
        frame.real_particles_number = real_particles_number;
    }

    /**
     * Normalizes a given <code>ImageProcessor</code> to [0,1]. <br>
     * According to the pre determend global min and max pixel value in the movie. <br>
     * All pixel intensity values I are normalized as (I-gMin)/(gMax-gMin)
     * 
     * @param ip ImageProcessor to be normalized
     * @param global_min
     * @param global_max
     */
    private void normalizeFrameFloat(ImageStack is, float global_min, float global_max) {
        for (int s = 1; s <= is.getSize(); s++) {
            final float[] pixels = (float[]) is.getPixels(s);
            float tmp_pix_value;
            for (int i = 0; i < pixels.length; i++) {
                tmp_pix_value = (pixels[i] - global_min) / (global_max - global_min);
                pixels[i] = tmp_pix_value;
            }
        }
    }

    /**
     * Finds and sets the threshold value for this frame given the
     * user defined percenticle and an ImageProcessor if the thresholdmode is PERCENTILE.
     * If not, the threshold is set to its absolute (normalized) value. There is only one parameter
     * used, either percent or aIntensityThreshold depending on the threshold mode.
     * 
     * @param ip ImageProcessor after conversion, normalization and restoration
     * @param percent the upper rth percentile to be considered as candidate Particles
     * @param absIntensityThreshold2 a intensity value which defines a threshold.
     * @return 
     */
    private float findThreshold(ImageStack ips, double percent, float absIntensityThreshold2) {
        if (threshold_mode == Mode.ABS_THRESHOLD_MODE) {
            // the percent parameter corresponds to an absolute value (not percent)
            return absIntensityThreshold2 - iGlobalMin / (iGlobalMax - iGlobalMin);
        }
        int s, i, j, thold, width;
        width = ips.getWidth();

        /* find this ImageStacks min and max pixel value */
        float min = 0f;
        float max = 0f;
        if (ips.getSize() > 1) {
            final StackStatistics sstats = new StackStatistics(new ImagePlus(null, ips));
            min = (float) sstats.min;
            max = (float) sstats.max;
        }
        else { // speeds up the 2d version:
            final ImageStatistics istats = ImageStatistics.getStatistics(ips.getProcessor(1), Measurements.MIN_MAX + Measurements.MODE + Measurements.STD_DEV, null);
            min = (float) istats.min;
            max = (float) istats.max;
        }

        final double[] hist = new double[256];
        for (i = 0; i < hist.length; i++) {
            hist[i] = 0;
        }
        for (s = 0; s < ips.getSize(); s++) {
            final float[] pixels = (float[]) ips.getProcessor(s + 1).getPixels();
            for (i = 0; i < ips.getHeight(); i++) {
                for (j = 0; j < ips.getWidth(); j++) {
                    hist[(int) ((pixels[i * width + j] - min) * 255.0 / (max - min))]++;
                }
            }
        }

        for (i = 254; i >= 0; i--) {
            hist[i] += hist[i + 1];
        }

        thold = 0;
        while (hist[255 - thold] / hist[0] < percent) {
            thold++;
            if (thold > 255) {
                break;
            }
        }
        thold = 255 - thold + 1;
        return ((float) (thold / 255.0) * (max - min) + min);
    }

    /**
     * Estimates the feature point locations in the given <code>ImageProcessor</code> <br>
     * Any pixel with the same value before and after dilation and value higher
     * then the pre calculated threshold is considered as a feature point (Particle). <br>
     * Adds each found <code>Particle</code> to the <code>particles</code> array. <br>
     * Mostly adapted from Ingo Oppermann implementation
     * 
     * @param ip ImageProcessor, should be after conversion, normalization and restoration
     */
    private void pointLocationsEstimation(ImageStack ips, int frame_number, int linkrange) {
        float threshold = findThreshold(ips, percentile, absIntensityThreshold);
        /* do a grayscale dilation */
        final ImageStack dilated_ips = MosaicImageProcessingTools.dilateGeneric(ips, radius, 4);
        // new StackWindow(new ImagePlus("dilated ", dilated_ips));
        particles = new Vector<Particle>();
        /* loop over all pixels */
        final int height = ips.getHeight();
        final int width = ips.getWidth();
        for (int s = 0; s < ips.getSize(); s++) {
            final float[] ips_pixels = (float[]) ips.getProcessor(s + 1).getPixels();
            final float[] ips_dilated_pixels = (float[]) dilated_ips.getProcessor(s + 1).getPixels();
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (ips_pixels[i * width + j] > threshold && ips_pixels[i * width + j] == ips_dilated_pixels[i * width + j]) { // check if pixel is a local maximum

                        /* and add each particle that meets the criteria to the particles array */
                        // (the starting point is the middle of the pixel and exactly on a focal plane:)
                        particles.add(new Particle(j + .5f, i + .5f, s, frame_number, linkrange));
                    }
                }
            }
        }
        particles_number = particles.size();
    }

    private void pointLocationsRefinement(ImageStack ips) {
        int m, k, l, x, y, z, tx, ty, tz;
        float epsx, epsy, epsz, c;

        final int mask_width = 2 * radius + 1;
        final int image_width = ips.getWidth();
        /* Set every value that is smaller than 0 to 0 */
        for (int s = 0; s < ips.getSize(); s++) {
            final float[] pixels = (float[]) ips.getPixels(s + 1);
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] < 0) {
                    pixels[i] = 0f;
                }
            }
        }

        /* Loop over all particles */
        for (m = 0; m < this.particles.size(); m++) {
            this.particles.elementAt(m).special = true;
            this.particles.elementAt(m).score = 0.0F;
            epsx = epsy = epsz = 1.0F;

            while (epsx > 0.5 || epsx < -0.5 || epsy > 0.5 || epsy < -0.5 || epsz > 0.5 || epsz < -0.5) {
                this.particles.elementAt(m).nbIterations++;
                this.particles.elementAt(m).m0 = 0.0F;
                this.particles.elementAt(m).m1 = 0.0F;
                this.particles.elementAt(m).m2 = 0.0F;
                this.particles.elementAt(m).m3 = 0.0F;
                this.particles.elementAt(m).m4 = 0.0F;
                epsx = 0.0F;
                epsy = 0.0F;
                epsz = 0.0F;
                for (int s = -radius; s <= radius; s++) {
                    if (((int) this.particles.elementAt(m).z + s) < 0 || ((int) this.particles.elementAt(m).z + s) >= ips.getSize()) {
                        continue;
                    }
                    z = (int) this.particles.elementAt(m).z + s;
                    for (k = -radius; k <= radius; k++) {
                        if (((int) this.particles.elementAt(m).y + k) < 0 || ((int) this.particles.elementAt(m).y + k) >= ips.getHeight()) {
                            continue;
                        }
                        x = (int) this.particles.elementAt(m).y + k;

                        for (l = -radius; l <= radius; l++) {
                            if (((int) this.particles.elementAt(m).x + l) < 0 || ((int) this.particles.elementAt(m).x + l) >= ips.getWidth()) {
                                continue;
                            }
                            y = (int) this.particles.elementAt(m).x + l;
                            //
                            // c =   ips.getProcessor(z + 1).getPixelValue(y, x) * (float)mask[s + radius][(k + radius) * mask_width + (l + radius)];
                            c = ((float[]) (ips.getPixels(z + 1)))[x * image_width + y] * mask[s + radius][(k + radius) * mask_width + (l + radius)];

                            this.particles.elementAt(m).m0 += c;
                            epsx += k * c;
                            epsy += l * c;
                            epsz += s * c;
                            this.particles.elementAt(m).m2 += (k * k + l * l + s * s) * c;
                            this.particles.elementAt(m).m1 += (float) Math.sqrt(k * k + l * l + s * s) * c;
                            this.particles.elementAt(m).m3 += (float) Math.pow(k * k + l * l + s * s, 1.5f) * c;
                            this.particles.elementAt(m).m4 += (float) Math.pow(k * k + l * l + s * s, 2) * c;
                        }
                    }
                }

                epsx /= this.particles.elementAt(m).m0;
                epsy /= this.particles.elementAt(m).m0;
                epsz /= this.particles.elementAt(m).m0;
                this.particles.elementAt(m).m2 /= this.particles.elementAt(m).m0;
                this.particles.elementAt(m).m1 /= this.particles.elementAt(m).m0;
                this.particles.elementAt(m).m3 /= this.particles.elementAt(m).m0;
                this.particles.elementAt(m).m4 /= this.particles.elementAt(m).m0;

                // This is a little hack to avoid numerical inaccuracy
                tx = (int) (10.0 * epsx);
                ty = (int) (10.0 * epsy);
                tz = (int) (10.0 * epsz);

                if ((tx) / 10.0 > 0.5) {
                    if ((int) this.particles.elementAt(m).y + 1 < ips.getHeight()) {
                        this.particles.elementAt(m).y++;
                    }
                }
                else if ((tx) / 10.0 < -0.5) {
                    if ((int) this.particles.elementAt(m).y - 1 >= 0) {
                        this.particles.elementAt(m).y--;
                    }
                }
                if ((ty) / 10.0 > 0.5) {
                    if ((int) this.particles.elementAt(m).x + 1 < ips.getWidth()) {
                        this.particles.elementAt(m).x++;
                    }
                }
                else if ((ty) / 10.0 < -0.5) {
                    if ((int) this.particles.elementAt(m).x - 1 >= 0) {
                        this.particles.elementAt(m).x--;
                    }
                }
                if ((tz) / 10.0 > 0.5) {
                    if ((int) this.particles.elementAt(m).z + 1 < ips.getSize()) {
                        this.particles.elementAt(m).z++;
                    }
                }
                else if ((tz) / 10.0 < -0.5) {
                    if ((int) this.particles.elementAt(m).z - 1 >= 0) {
                        this.particles.elementAt(m).z--;
                    }
                }

                if ((tx) / 10.0 <= 0.5 && (tx) / 10.0 >= -0.5 && (ty) / 10.0 <= 0.5 && (ty) / 10.0 >= -0.5 && (tz) / 10.0 <= 0.5 && (tz) / 10.0 >= -0.5) {
                    break;
                }
            }
            // System.out.println("iterations for particle " + m + ": " + this.particles.elementAt(m).nbIterations);
            this.particles.elementAt(m).y += epsx;
            this.particles.elementAt(m).x += epsy;
            this.particles.elementAt(m).z += epsz;
        }
    }

    /**
     * Rejects spurious particles detections such as unspecific signals, dust, or particle aggregates. <br>
     * The implemented classification algorithm after Crocker and Grier [68] is based on the
     * intensity moments of orders 0 and 2. <br>
     * Particles with lower final score than the user-defined cutoff are discarded <br>
     * Adapted "as is" from Ingo Oppermann implementation
     */
    private void nonParticleDiscrimination() {

        int j, k;
        double score;
        int max_x = 1, max_y = 1, max_z = 1;
        this.real_particles_number = this.particles_number;
        if (this.particles.size() == 1) {
            this.particles.elementAt(0).score = Float.MAX_VALUE;
        }
        for (j = 0; j < this.particles.size(); j++) {
            // int accepted = 1;
            max_x = Math.max((int) this.particles.elementAt(j).x, max_x);
            max_y = Math.max((int) this.particles.elementAt(j).y, max_y);
            max_z = Math.max((int) this.particles.elementAt(j).z, max_z);

            for (k = j + 1; k < this.particles.size(); k++) {
                score = (1.0 / (2.0 * Math.PI * 0.1 * 0.1))
                        * Math.exp(-(this.particles.elementAt(j).m0 - this.particles.elementAt(k).m0) * (this.particles.elementAt(j).m0 - this.particles.elementAt(k).m0) / (2.0 * 0.1)
                                - (this.particles.elementAt(j).m2 - this.particles.elementAt(k).m2) * (this.particles.elementAt(j).m2 - this.particles.elementAt(k).m2) / (2.0 * 0.1));
                this.particles.elementAt(j).score += score;
                this.particles.elementAt(k).score += score;
            }
            if (this.particles.elementAt(j).score < cutoff) {
                this.particles.elementAt(j).special = false;
                this.real_particles_number--;
                // accepted = 0;
            }
            // System.out.println(j + "\t" + this.particles.elementAt(j).m0 + "\t" + this.particles.elementAt(j).m2 + "\t" + accepted);
        }

        /*
         * Remove duplicates (happens when dealing with artificial images)
         */
        // Create a bitmap (with ghostlayers to not have to perform bounds checking)
        final boolean[][][] vBitmap = new boolean[max_z + 3][max_y + 3][max_x + 3];
        for (int z = 0; z < max_z + 3; z++) {
            for (int y = 0; y < max_y + 3; y++) {
                for (int x = 0; x < max_x + 3; x++) {
                    vBitmap[z][y][x] = false;
                }
            }
        }

        for (j = 0; j < this.particles.size(); j++) {
            boolean vParticleInNeighborhood = false;
            for (int oz = -1; !vParticleInNeighborhood && oz <= 1; oz++) {
                for (int oy = -1; !vParticleInNeighborhood && oy <= 1; oy++) {
                    for (int ox = -1; !vParticleInNeighborhood && ox <= 1; ox++) {
                        if (vBitmap[(int) this.particles.elementAt(j).z + 1 + oz][(int) this.particles.elementAt(j).y + 1 + oy][(int) this.particles.elementAt(j).x + 1 + ox]) {
                            vParticleInNeighborhood = true;
                        }
                    }
                }
            }

            if (vParticleInNeighborhood) {
                this.particles.elementAt(j).special = false;
                this.real_particles_number--;
            }
            else {
                vBitmap[(int) this.particles.elementAt(j).z + 1][(int) this.particles.elementAt(j).y + 1][(int) this.particles.elementAt(j).x + 1] = true;
            }
        }

    }

    /**
     * removes particles that were discarded by the <code>nonParticleDiscrimination</code> method
     * from the particles array. <br>
     * Non particles will be removed from the <code>particles</code> array so if their info is
     * needed, it should be saved before calling this method
     * 
     * @see MyFrame#nonParticleDiscrimination()
     */
    private void removeNonParticle() {
        for (int i = this.particles.size() - 1; i >= 0; i--) {
            if (!this.particles.elementAt(i).special) {
                this.particles.removeElementAt(i);
            }
        }
    }

    /**
     * Corrects imperfections in the given <code>ImageStack</code> by
     * convolving it (slice by slice, not 3D) with the pre calculated <code>kernel</code>
     * 
     * @param is ImageStack to be restored
     * @return the restored <code>ImageProcessor</code>
     * @see Convolver#convolve(ij.process.ImageProcessor, float[], int, int)
     */
    private ImageStack imageRestoration(ImageStack is) {
        // remove the clutter
        ImageStack restored = null;

        // pad the imagestack
        if (is.getSize() > 1) {
            // 3D mode
            restored = MosaicUtils.padImageStack3D(is, radius);
        }
        else {
            // we're in 2D mode
            final ImageProcessor rp = MosaicUtils.padImageStack2D(is.getProcessor(1), radius);
            restored = new ImageStack(rp.getWidth(), rp.getHeight());
            restored.addSlice("", rp);
        }

        // Old switch statement for:  case BOX_CAR_AVG:
        // There was found to be a 2*lambda_n for the sigma of the Gaussian kernel.
        // Set it back to 1*lambda_n to match the 2D implementation.
        final float lambda_n = 1;
        GaussBlur3D(restored, 1 * lambda_n);
        // new StackWindow(new ImagePlus("convolved 3d",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored, 1, restored.getSize())));
        boxCarBackgroundSubtractor(restored);
        // new StackWindow(new ImagePlus("after bg subtraction",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored, 1, restored.getSize())));


        if (is.getSize() > 1) {
            // again, 3D crop
            // new StackWindow(new ImagePlus("before cropping",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored, 1, restored.getSize())));
            restored = MosaicUtils.cropImageStack3D(restored, radius);
        }
        else {
            // 2D crop
            final ImageProcessor rp = MosaicUtils.cropImageStack2D(restored.getProcessor(1), radius);
            restored = new ImageStack(rp.getWidth(), rp.getHeight());
            restored.addSlice("", rp);
        }
        // new StackWindow(new ImagePlus("restored", GetSubStackCopyInFloat(restored, 1, restored.getSize())));
        return restored;

    }

    private void GaussBlur3D(ImageStack is, float aSigma) {
        final float[] vKernel = CalculateNormalizedGaussKernel(aSigma);
        int kernel_radius = vKernel.length / 2;
        final int nSlices = is.getSize();
        final int vWidth = is.getWidth();
        for (int i = 1; i <= nSlices; i++) {
            final ImageProcessor restored_proc = is.getProcessor(i);
            final Convolver convolver = new Convolver();
            // no need to normalize the kernel - its already normalized
            convolver.setNormalize(false);
            // the gaussian kernel is separable and can done in 3x 1D convolutions.
            convolver.convolve(restored_proc, vKernel, vKernel.length, 1);
            convolver.convolve(restored_proc, vKernel, 1, vKernel.length);
        }
        // 2D mode, abort here; the rest is unnecessary
        if (is.getSize() == 1) {
            return;
        }

        kernel_radius = vKernel.length / 2;
        // to speed up the method, store the processor in an array (not invoke getProcessor()):
        final float[][] vOrigProcessors = new float[nSlices][];
        final float[][] vRestoredProcessors = new float[nSlices][];
        for (int s = 0; s < nSlices; s++) {
            vOrigProcessors[s] = (float[]) is.getProcessor(s + 1).getPixelsCopy();
            vRestoredProcessors[s] = (float[]) is.getProcessor(s + 1).getPixels();
        }
        // convolution with 1D gaussian in 3rd dimension:
        for (int y = kernel_radius; y < is.getHeight() - kernel_radius; y++) {
            for (int x = kernel_radius; x < is.getWidth() - kernel_radius; x++) {
                for (int s = kernel_radius + 1; s <= is.getSize() - kernel_radius; s++) {
                    float sum = 0;
                    for (int i = -kernel_radius; i <= kernel_radius; i++) {
                        sum += vKernel[i + kernel_radius] * vOrigProcessors[s + i - 1][y * vWidth + x];
                    }
                    vRestoredProcessors[s - 1][y * vWidth + x] = sum;
                }
            }
        }
    }

    private void boxCarBackgroundSubtractor(ImageStack is) {
        final Convolver convolver = new Convolver();
        final float[] kernel = new float[radius * 2 + 1];
        final int n = kernel.length;
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] = 1f / n;
        }
        for (int s = 1; s <= is.getSize(); s++) {
            final ImageProcessor bg_proc = is.getProcessor(s).duplicate();
            convolver.convolveFloat(bg_proc, kernel, 1, n);
            convolver.convolveFloat(bg_proc, kernel, n, 1);
            is.getProcessor(s).copyBits(bg_proc, 0, 0, Blitter.SUBTRACT);
        }
    }

    private float[] CalculateNormalizedGaussKernel(float aSigma) {
        int vL = (int) aSigma * 3 * 2 + 1;
        if (vL < 3) {
            vL = 3;
        }
        final float[] vKernel = new float[vL];
        final int vM = vKernel.length / 2;
        for (int vI = 0; vI < vM; vI++) {
            vKernel[vI] = (float) (1f / (2f * Math.PI * aSigma * aSigma) * Math.exp(-(float) ((vM - vI) * (vM - vI)) / (2f * aSigma * aSigma)));
            vKernel[vKernel.length - vI - 1] = vKernel[vI];
        }
        vKernel[vM] = (float) (1f / (2f * Math.PI * aSigma * aSigma));

        // normalize the kernel numerically:
        float vSum = 0;
        for (int vI = 0; vI < vKernel.length; vI++) {
            vSum += vKernel[vI];
        }
        final float vScale = 1.0f / vSum;
        for (int vI = 0; vI < vKernel.length; vI++) {
            vKernel[vI] *= vScale;
        }
        return vKernel;
    }

    /**
     * (Re)Initialize the binary and weighted masks. This is necessary if the radius changed.
     * The memory allocations are performed in advance (in this method) for efficiency reasons.
     * 
     * @param mask_radius
     */
    private void generateDilationMasks(int mask_radius) {
        // standard boolean mask
        mask = MosaicImageProcessingTools.generateMask(mask_radius);
    }

    /**
     * Sets user defined params that are necessary to particle detection
     * and generates the <code>mask</code> according to these params
     * @return 
     * 
     * @see #generateDilationMasks(int)
     */
    public boolean setUserDefinedParameters(double cutoff, float percentile, int radius, float Threshold, boolean absolute) {
        final boolean changed = (radius != this.radius || cutoff != this.cutoff || (percentile != this.percentile));// && intThreshold != absIntensityThreshold || mode != getThresholdMode() || thsmode != getThresholdMode();
        
        this.cutoff = cutoff;
        this.percentile = percentile;
        this.absIntensityThreshold = Threshold;
        this.radius = radius;
        if (absolute == true) {
            this.threshold_mode = Mode.ABS_THRESHOLD_MODE;
        }
        else {
            this.threshold_mode = Mode.PERCENTILE_MODE;
        }

        // create Mask for Dilation with the user defined radius
        generateDilationMasks(this.radius);
        
        return changed;
    }

    public int getRadius() {
        return radius;
    }
    
    public Mode getThresholdMode() {
        return threshold_mode;
    }
    
    public float getGlobalMax() {
        return iGlobalMax;
    }
    
    public float getGlobalMin() {
        return iGlobalMin;
    }

    public void setGlobalMax(float aValue) {
        iGlobalMax = aValue;
    }

    public void setGlobalMin(float aValue) {
        iGlobalMin = aValue;
    }
    
    public double getCutoff() {
        return cutoff;
    }

    public float getPercentile() {
        return percentile;
    }
    
    public float getAbsIntensityThreshold() {
        return absIntensityThreshold;
    }
}
