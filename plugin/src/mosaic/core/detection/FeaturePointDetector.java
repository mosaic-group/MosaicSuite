package mosaic.core.detection;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.StackWindow;
import ij.io.SaveDialog;
import ij.measure.Measurements;
import ij.plugin.filter.Convolver;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Vector;

import mosaic.core.utils.MosaicImageProcessingTools;
import mosaic.core.utils.MosaicUtils;
import mosaic.plugins.BackgroundSubtractor2_;


/**
 * <br>
 * FeaturePointDetector class has all the necessary methods to detect the "real" particles
 * for them to be linked.
 */

public class FeaturePointDetector {

    public static final int ABS_THRESHOLD_MODE = 0, PERCENTILE_MODE = 1;
    public static final int NO_PREPROCESSING = 0, BOX_CAR_AVG = 1, BG_SUBTRACTOR = 2, LAPLACE_OP = 3;

    public float global_max, global_min;
    Vector<Particle> particles;
    int particles_number; // number of particles initialy detected
    int real_particles_number; // number of "real" particles discrimination

    Label previewLabel = new Label("");
    MyFrame preview_frame;

    /* user defined parameters */
    public double cutoff = 3.0; // default
    public float percentile = 0.001F; // default (user input/100)
    public float absIntensityThreshold = 0.0f; // user input
    public int radius = 3; // default
    public boolean absolute = false;
    // public float sigma_factor = 3;

    public int threshold_mode = PERCENTILE_MODE;

    private float threshold;

    /* image Restoration vars */
    // public short[][] binary_mask;
    // public float[][] weighted_mask;
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
        normalizeFrameFloat(restored_fps, global_min, global_max);
        // new StackWindow(new ImagePlus("after normalization",restored_fps));

        /* Image Restoration - Step 1 of the algorithm */
        restored_fps = imageRestoration(restored_fps);
        // new StackWindow(new ImagePlus("after restoration",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored_fps, 1, restored_fps.getSize())));

        /* Estimation of the point location - Step 2 of the algorithm */
        findThreshold(restored_fps, percentile, absIntensityThreshold);

        pointLocationsEstimation(restored_fps, frame.frame_number, frame.linkrange);
        //
        // System.out.println("particles after location estimation:");
        // for (Particle p : this.particles) {
        // System.out.println("particle: " + p.toString());
        // }

        /* Refinement of the point location - Step 3 of the algorithm */
        pointLocationsRefinement(restored_fps);
        // new StackWindow(new ImagePlus("after location ref",restored_fps));
        // System.out.println("particles after location refinement:");
        // for (Particle p : this.particles) {
        // System.out.println("particle: " + p.toString());
        // }

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
     */
    private void findThreshold(ImageStack ips, double percent, float absIntensityThreshold2) {
        if (getThresholdMode() == ABS_THRESHOLD_MODE) {
            // the percent parameter corresponds to an absolute value (not percent)
            this.setThreshold(absIntensityThreshold2 - global_min / (global_max - global_min));
            return;
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
        this.setThreshold(((float) (thold / 255.0) * (max - min) + min));

        // this.setThreshold(mode+sigma_factor*std);
        // System.out.println("min= " + min + ", max=" + max );
        // System.out.println("THRESHOLD: " + this.threshold);
        // System.out.println("simga_fac=" + this.sigma_factor);

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
                    if (ips_pixels[i * width + j] > this.threshold && ips_pixels[i * width + j] == ips_dilated_pixels[i * width + j]) { // check if pixel is a local maximum

                        /* and add each particle that meets the criteria to the particles array */
                        // (the starting point is the middle of the pixel and exactly on a focal plane:)
                        particles.add(new Particle(j + .5f, i + .5f, s, frame_number, linkrange));

                        /*
                         * now we found a local maximum, we have to prevent that all connected pixel do
                         * not generate a new particle. We thus set the dilated image around the current
                         * location to 0.
                         */
                        // for (int ii = Math.max(i-radius+1,0); ii < Math.min(height, i+radius-1); ii++){
                        // for (int jj = Math.max(j-radius, 0); jj < Math.min(width, j+radius-1); jj++){
                        // ips_dilated_pixels[i*width+j] = -1;
                        // }
                        // }

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
            // for (int i = 0; i < ips.getHeight(); i++) {
            // for (int j = 0; j < ips.getWidth(); j++) {
            // if (ips.getProcessor(s + 1).getPixelValue(j, i) < 0.0)
            // ips.getProcessor(s + 1).putPixelValue(j, i, 0.0);
            //
            // }
            // }
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
                            // c = ips.getProcessor(z + 1).getPixelValue(y, x) * (float)mask[s + radius][(k + radius)*mask_width + (l + radius)];
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
            // for (k = j + 1; k < this.particles.size(); k++) {
            // }
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

        // Particle[] new_particles = new Particle[this.real_particles_number];
        // int new_par_index = 0;
        // for (int i = 0; i< this.particles.length; i++) {
        // if (this.particles[i].special) {
        // new_particles[new_par_index] = this.particles[i];
        // new_par_index++;
        // }
        // }
        // this.particles = new_particles;
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

        switch (preprocessing_mode) {
            case NO_PREPROCESSING:
                GaussBlur3D(restored, 1 * lambda_n);
                break;
            case BOX_CAR_AVG:
                // There was found to be a 2*lambda_n for the sigma of the Gaussian kernel.
                // Set it back to 1*lambda_n to match the 2D implementation.
                GaussBlur3D(restored, 1 * lambda_n);
                // new StackWindow(new ImagePlus("convolved 3d",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored, 1, restored.getSize())));

                boxCarBackgroundSubtractor(restored);// TODO:3D? ->else: pad!
                // new StackWindow(new ImagePlus("after bg subtraction",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored, 1, restored.getSize())));

                break;
            case BG_SUBTRACTOR:
                GaussBlur3D(restored, 1 * lambda_n);
                final BackgroundSubtractor2_ bgSubtractor = new BackgroundSubtractor2_();
                for (int s = 1; s <= restored.getSize(); s++) {
                    // IJ.showProgress(s, restored.getSize());
                    // IJ.showStatus("Preprocessing: subtracting background...");
                    bgSubtractor.SubtractBackground(restored.getProcessor(s), radius * 4);
                }
                break;
            case LAPLACE_OP:
                // remove noise then do the laplace op
                GaussBlur3D(restored, 1 * lambda_n);
                MosaicUtils.repadImageStack3D(restored, radius);
                restored = Laplace_Separable_3D(restored);
                break;
            default:
                break;
        }
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

        // TODO: which kernel? since lambda_n = 1 pixel, it does not depend on the resolution -->not rescale
        // rescale the kernel for z dimension
        // vKernel = CalculateNormalizedGaussKernel((float)(aRadius / (original_imp.getCalibration().pixelDepth / original_imp.getCalibration().pixelWidth)));

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

    private ImageStack Laplace_Separable_3D(ImageStack aIS) {
        final float[] vKernel_1D = new float[] { -1, 2, -1 };
        final ImageStack vResultStack = new ImageStack(aIS.getWidth(), aIS.getHeight());
        final int vKernelWidth = vKernel_1D.length;
        final int vKernelRadius = vKernel_1D.length / 2;
        final int vWidth = aIS.getWidth();
        //
        // in x dimension
        //
        for (int vI = 1; vI <= aIS.getSize(); vI++) {
            final ImageProcessor vConvolvedSlice = aIS.getProcessor(vI).duplicate();
            final Convolver vConvolver = new Convolver();
            vConvolver.setNormalize(false);
            vConvolver.convolve(vConvolvedSlice, vKernel_1D, vKernelWidth, 1);
            vResultStack.addSlice(null, vConvolvedSlice);
        }
        //
        // in y dimension and sum it to the result
        //
        for (int vI = 1; vI <= aIS.getSize(); vI++) {
            final ImageProcessor vConvolvedSlice = aIS.getProcessor(vI).duplicate();
            final Convolver vConvolver = new Convolver();
            vConvolver.setNormalize(false);
            vConvolver.convolve(vConvolvedSlice, vKernel_1D, 1, vKernelWidth);
            vResultStack.getProcessor(vI).copyBits(vConvolvedSlice, 0, 0, Blitter.ADD);
        }
        // if (true) return vResultStack; //TODO: abort here? yes if gauss3d is scaled in z

        //
        // z dimension
        //
        // first get all the processors of the frame in an array since the getProcessor method is expensive
        final float[][] vOriginalStackPixels = new float[aIS.getSize()][];
        final float[][] vConvolvedStackPixels = new float[aIS.getSize()][];
        final float[][] vResultStackPixels = new float[aIS.getSize()][];
        for (int vS = 0; vS < aIS.getSize(); vS++) {
            vOriginalStackPixels[vS] = (float[]) aIS.getProcessor(vS + 1).getPixels();
            vConvolvedStackPixels[vS] = (float[]) aIS.getProcessor(vS + 1).getPixelsCopy();
            vResultStackPixels[vS] = (float[]) vResultStack.getProcessor(vS + 1).getPixels();
        }
        for (int vY = 0; vY < aIS.getHeight(); vY++) {
            for (int vX = 0; vX < aIS.getWidth(); vX++) {
                for (int vS = vKernelRadius; vS < aIS.getSize() - vKernelRadius; vS++) {
                    float vSum = 0;
                    for (int vI = -vKernelRadius; vI <= vKernelRadius; vI++) {
                        vSum += vKernel_1D[vI + vKernelRadius] * vOriginalStackPixels[vS + vI][vY * vWidth + vX];
                    }
                    vConvolvedStackPixels[vS][vY * vWidth + vX] = vSum;
                }
            }
        }
        // add the results
        for (int vS = vKernelRadius; vS < aIS.getSize() - vKernelRadius; vS++) {
            for (int vI = 0; vI < vResultStackPixels[vS].length; vI++) {
                vResultStackPixels[vS][vI] += vConvolvedStackPixels[vS][vI];
            }
        }
        // new StackWindow(new ImagePlus("after laplace copy",GetSubStackCopyInFloat(vResultStack, 1, vResultStack.getSize())));
        return vResultStack;
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
    private void generateMasks(int mask_radius) {
        // the binary mask can be calculated already:
        // int width = (2 * mask_radius) + 1;
        // this.binary_mask = new short[width][width*width];
        // generateBinaryMask(mask_radius);

        // the weighted mask is just initialized with the new radius:
        // this.weighted_mask = new float[width][width*width];

        // standard boolean mask
        mask = MosaicImageProcessingTools.generateMask(mask_radius);

    }

    /**
     * Sets user defined params that are necessary to particle detection
     * and generates the <code>mask</code> according to these params
     * 
     * @see #generateMasks(int)
     */
    private void setUserDefinedParameters(double cutoff, float percentile, int radius, float Threshold, boolean absolute) {
        // , float sigma_factor) {

        this.cutoff = cutoff;
        this.percentile = percentile;
        this.absIntensityThreshold = Threshold;
        // this.sigma_factor = sigma_factor;
        // this.preprocessing_mode = mode;
        // this.setThresholdMode(thsmode);
        this.radius = radius;
        if (absolute == true) {
            this.threshold_mode = ABS_THRESHOLD_MODE;
        }
        else {
            this.threshold_mode = PERCENTILE_MODE;
        }

        // create Mask for Dilation with the user defined radius
        generateMasks(this.radius);
    }

    public void addUserDefinedParametersDialog(GenericDialog gd) {
        gd.addMessage("Particle Detection:");
        // These 3 params are only relevant for non text_files_mode
        gd.addNumericField("Radius", radius, 0);
        gd.addNumericField("Cutoff", cutoff, 1);
        gd.addNumericField("Per/Abs", percentile * 100, 5, 6, " ");

        gd.addCheckbox("Absolute", absolute);

        // gd.addChoice("Threshold mode", new String[]{"Absolute Threshold","Percentile"}, "Percentile");
        // ((Choice)gd.getChoices().firstElement()).addItemListener(new ItemListener(){
        // public void itemStateChanged(ItemEvent e) {
        // int mode = 0;
        // if (e.getItem().toString().equals("Absolute Threshold")) {
        // mode = ABS_THRESHOLD_MODE;
        // }
        // if (e.getItem().toString().equals("Percentile")) {
        // mode = PERCENTILE_MODE;
        // }
        // thresholdModeChanged(mode);
        // }});

        // gd.addNumericField("Percentile", 0.001, 5);
        // gd.addNumericField("Percentile / Abs.Threshold", 0.1, 5, 6, " % / Intensity");
        // gd.addNumericField("sigma factor", sigma_factor, 5);
        // gd.addPanel(makeThresholdPanel(), GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
        // gd.addChoice("Preprocessing mode", new String[]{"none", "box-car avg.", "BG Subtraction", "Laplace Operation"}, "box-car avg.");
    }

    /**
     * gd has to be shown with showDialog and handles the fields added to the dialog
     * with addUserDefinedParamtersDialog(gd).
     * 
     * @param gd <code>GenericDialog</code> at which the UserDefinedParameter fields where added.
     * @return true if user changed the parameters and false if the user didn't changed them.
     */
    public Boolean getUserDefinedParameters(GenericDialog gd) {
        final int rad = (int) gd.getNextNumber();
        // this.radius = (int)gd.getNextNumber();
        final double cut = gd.getNextNumber();
        // this.cutoff = gd.getNextNumber();
        final float per = ((float) gd.getNextNumber()) / 100;
        final float intThreshold = per * 100;
        absolute = gd.getNextBoolean();

        // this.percentile = ((float)gd.getNextNumber())/100;

        // int thsmode = gd.getNextChoiceIndex();
        // setThresholdMode(thsmode);

        // int mode = gd.getNextChoiceIndex();
        // float sigma_fac = ((float)gd.getNextNumber());

        final Boolean changed = (rad != radius || cut != cutoff || (per != percentile));// && intThreshold != absIntensityThreshold || mode != getThresholdMode() || thsmode != getThresholdMode();
        setUserDefinedParameters(cut, per, rad, intThreshold, absolute);
        // this.preprocessing_mode = mode;
        return changed;
    }

    /**
     * Gets user defined params that are necessary to display the preview of particle detection.
     */
    public Boolean getUserDefinedPreviewParams(GenericDialog gd) {

        @SuppressWarnings("unchecked")
        final
        // the warning is due to old imagej code.
        Vector<TextField> vec = gd.getNumericFields();
        final int rad = Integer.parseInt((vec.elementAt(0)).getText());
        final double cut = Double.parseDouble((vec.elementAt(1)).getText());
        final float per = (Float.parseFloat((vec.elementAt(2)).getText())) / 100;
        // float sigma_fac = (Float.parseFloat((vec.elementAt(3)).getText()));
        final float intThreshold = per * 100;
        @SuppressWarnings("unchecked")
        final
        // the warning is due to old imagej code
        Vector<Checkbox> vecb = gd.getCheckboxes();
        final boolean absolute = vecb.elementAt(0).getState();
        // int thsmode = ((Choice)gd.getChoices().elementAt(0)).getSelectedIndex();
        // int mode = ((Choice)gd.getChoices().elementAt(1)).getSelectedIndex();

        // even if the frames were already processed (particles detected) but
        // the user changed the detection params then the frames needs to be processed again
        final Boolean changed = (rad != radius || cut != cutoff || (per != percentile));// && intThreshold != absIntensityThreshold || mode != getThresholdMode() || thsmode != getThresholdMode();
        setUserDefinedParameters(cut, per, rad, intThreshold, absolute);// , sigma_fac);
        return changed;
    }

    /**
     * Creates the preview panel that gives the options to preview and save the detected particles,
     * and also a scroll bar to navigate through the slices of the movie <br>
     * Buttons and scrollbar created here use this PreviewInterface previewHandler as <code>ActionListener</code> and <code>AdjustmentListener</code>
     * 
     * @return the preview panel
     */

    public Panel makePreviewPanel(final PreviewInterface previewHandler, final ImagePlus img) {

        final Panel preview_panel = new Panel();
        final GridBagLayout gridbag = new GridBagLayout();
        final GridBagConstraints c = new GridBagConstraints();
        preview_panel.setLayout(gridbag);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;

        /* scroll bar to navigate through the slices of the movie */
        final Scrollbar preview_scrollbar = new Scrollbar(Scrollbar.HORIZONTAL, img.getCurrentSlice(), 1, 1, img.getStackSize() + 1);
        preview_scrollbar.addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                // set the current visible slice to the one selected on the bar
                img.setSlice(preview_scrollbar.getValue());
                // update the preview view to this silce
                // this.preview();
            }
        });
        preview_scrollbar.setUnitIncrement(1);
        preview_scrollbar.setBlockIncrement(1);

        /* button to generate preview of the detected particles */
        final Button preview = new Button("Preview Detected");
        preview.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                previewHandler.preview(e);
            }
        });

        /* button to save the detected particles */
        final Button save_detected = new Button("Save Detected");
        save_detected.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                previewHandler.saveDetected(e);
            }
        });
        final Label seperation = new Label("______________", Label.CENTER);
        gridbag.setConstraints(preview, c);
        preview_panel.add(preview);
        gridbag.setConstraints(preview_scrollbar, c);
        preview_panel.add(preview_scrollbar);
        gridbag.setConstraints(save_detected, c);
        preview_panel.add(save_detected);
        gridbag.setConstraints(previewLabel, c);
        preview_panel.add(previewLabel);
        gridbag.setConstraints(seperation, c);
        preview_panel.add(seperation);
        return preview_panel;
    }

    public void setPreviewLabel(String previewLabelText) {
        this.previewLabel.setText(previewLabelText);
    }

    public PreviewCanvas generatePreviewCanvas(ImagePlus imp) {
        // save the current magnification factor of the current image window
        final double magnification = imp.getWindow().getCanvas().getMagnification();

        // generate the previewCanvas - while generating the drawing will be done
        final PreviewCanvas preview_canvas = new PreviewCanvas(imp, magnification);

        // display the image and canvas in a stackWindow
        final StackWindow sw = new StackWindow(imp, preview_canvas);

        // magnify the canvas to match the original image magnification
        while (sw.getCanvas().getMagnification() < magnification) {
            preview_canvas.zoomIn(0, 0);
        }
        return preview_canvas;
    }

    /**
     * Detects particles in the current displayed frame according to the parameters currently set
     * Draws dots on the positions of the detected partciles on the frame and circles them
     * 
     * @see #getUserDefinedPreviewParams()
     * @see MyFrame#featurePointDetection()
     */
    public synchronized void preview(ImagePlus imp, GenericDialog gd) {

        // the stack of the original loaded image (it can be 1 frame)
        final ImageStack stack = imp.getStack();

        getUserDefinedPreviewParams(gd);

        final int first_slice = imp.getCurrentSlice(); // TODO check what should be here, figure out how slices and frames numbers work(getFrameNumberFromSlice(impA.getCurrentSlice())-1) * impA.getNSlices() + 1;
        // create a new MyFrame from the current_slice in the stack, linkrange should not matter for a previewframe
        preview_frame = new MyFrame(MosaicUtils.GetSubStackCopyInFloat(stack, first_slice, first_slice + imp.getNSlices() - 1), getFrameNumberFromSlice(imp.getCurrentSlice(), imp.getNSlices()) - 1, 1);

        // detect particles in this frame
        featurePointDetection(preview_frame);
        setPreviewLabel("#Particles: " + preview_frame.getParticles().size());
    }

    /**
     * @param sliceIndex: 1..#slices
     * @return a frame index: 1..#frames
     */
    private int getFrameNumberFromSlice(int sliceIndex, int slices_number) {
        return (sliceIndex - 1) / slices_number + 1;
    }

    private void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    private int getThresholdMode() {
        return threshold_mode;
    }

    public int getRadius() {
        return radius;
    }

    public MyFrame getPreviewFrame() {
        return preview_frame;
    }

    // /**
    // * Second phase of the algorithm -
    // * <br>Identifies points corresponding to the
    // * same physical particle in subsequent frames and links the positions into trajectories
    // * <br>The length of the particles next array will be reset here according to the current linkrange
    // * <br>Adapted from Ingo Oppermann implementation
    // */
    // public void linkParticles(MyFrame[] frames, int frames_number, int linkrange, double displacement) {
    //
    // int m, i, j, k, nop, nop_next, n;
    // int ok, prev, prev_s, x = 0, y = 0, curr_linkrange;
    // int[] g;
    // double min, z, max_cost;
    // double[] cost;
    // Vector<Particle> p1, p2;
    //
    // // set the length of the particles next array according to the linkrange
    // // it is done now since link range can be modified after first run
    // for (int fr = 0; fr<frames.length; fr++) {
    // for (int pr = 0; pr<frames[fr].getParticles().size(); pr++) {
    // frames[fr].getParticles().elementAt(pr).next = new int[linkrange];
    // }
    // }
    // curr_linkrange = linkrange;
    //
    // /* If the linkrange is too big, set it the right value */
    // if (frames_number < (curr_linkrange + 1))
    // curr_linkrange = frames_number - 1;
    //
    // max_cost = displacement * displacement;
    //
    // for (m = 0; m < frames_number - curr_linkrange; m++) {
    // nop = frames[m].getParticles().size();
    // for (i = 0; i < nop; i++) {
    // frames[m].getParticles().elementAt(i).special = false;
    // for (n = 0; n < linkrange; n++)
    // frames[m].getParticles().elementAt(i).next[n] = -1;
    // }
    //
    // for (n = 0; n < curr_linkrange; n++) {
    // max_cost = (double)(n + 1) * displacement * (double)(n + 1) * displacement;
    //
    // nop_next = frames[m + (n + 1)].getParticles().size();
    //
    // /* Set up the cost matrix */
    // cost = new double[(nop + 1) * (nop_next + 1)];
    //
    // /* Set up the relation matrix */
    // g = new int[(nop + 1) * (nop_next + 1)];
    //
    // /* Set g to zero */
    // for (i = 0; i< g.length; i++) g[i] = 0;
    //
    // p1 = frames[m].getParticles();
    // p2 = frames[m + (n + 1)].getParticles();
    // // p1 = frames[m].particles;
    // // p2 = frames[m + (n + 1)].particles;
    //
    //
    // /* Fill in the costs */
    // for (i = 0; i < nop; i++) {
    // for (j = 0; j < nop_next; j++) {
    // cost[coord(i, j, nop_next + 1)] =
    // (p1.elementAt(i).x - p2.elementAt(j).x)*(p1.elementAt(i).x - p2.elementAt(j).x) +
    // (p1.elementAt(i).y - p2.elementAt(j).y)*(p1.elementAt(i).y - p2.elementAt(j).y) +
    // (p1.elementAt(i).z - p2.elementAt(j).z)*(p1.elementAt(i).z - p2.elementAt(j).z) +
    // (p1.elementAt(i).m0 - p2.elementAt(j).m0)*(p1.elementAt(i).m0 - p2.elementAt(j).m0) +
    // (p1.elementAt(i).m2 - p2.elementAt(j).m2)*(p1.elementAt(i).m2 - p2.elementAt(j).m2);
    // }
    // }
    //
    // for (i = 0; i < nop + 1; i++)
    // cost[coord(i, nop_next, nop_next + 1)] = max_cost;
    // for (j = 0; j < nop_next + 1; j++)
    // cost[coord(nop, j, nop_next + 1)] = max_cost;
    // cost[coord(nop, nop_next, nop_next + 1)] = 0.0;
    //
    // /* Initialize the relation matrix */
    // for (i = 0; i < nop; i++) { // Loop over the x-axis
    // min = max_cost;
    // prev = 0;
    // for (j = 0; j < nop_next; j++) { // Loop over the y-axis
    // /* Let's see if we can use this coordinate */
    // ok = 1;
    // for (k = 0; k < nop + 1; k++) {
    // if (g[coord(k, j, nop_next + 1)] == 1) {
    // ok = 0;
    // break;
    // }
    // }
    // if (ok == 0) // No, we can't. Try the next column
    // continue;
    //
    // /* This coordinate is OK */
    // if (cost[coord(i, j, nop_next + 1)] < min) {
    // min = cost[coord(i, j, nop_next + 1)];
    // g[coord(i, prev, nop_next + 1)] = 0;
    // prev = j;
    // g[coord(i, prev, nop_next + 1)] = 1;
    // }
    // }
    //
    // /* Check if we have a dummy particle */
    // if (min == max_cost) {
    // g[coord(i, prev, nop_next + 1)] = 0;
    // g[coord(i, nop_next, nop_next + 1)] = 1;
    // }
    // }
    //
    // /* Look for columns that are zero */
    // for (j = 0; j < nop_next; j++) {
    // ok = 1;
    // for (i = 0; i < nop + 1; i++) {
    // if (g[coord(i, j, nop_next + 1)] == 1)
    // ok = 0;
    // }
    //
    // if (ok == 1)
    // g[coord(nop, j, nop_next + 1)] = 1;
    // }
    //
    // /* The relation matrix is initilized */
    //
    // /* Now the relation matrix needs to be optimized */
    // min = -1.0;
    // while (min < 0.0) {
    // min = 0.0;
    // prev = 0;
    // prev_s = 0;
    // for (i = 0; i < nop + 1; i++) {
    // for (j = 0; j < nop_next + 1; j++) {
    // if (i == nop && j == nop_next)
    // continue;
    //
    // if (g[coord(i, j, nop_next + 1)] == 0 &&
    // cost[coord(i, j, nop_next + 1)] <= max_cost) {
    // /* Calculate the reduced cost */
    //
    // // Look along the x-axis, including
    // // the dummy particles
    // for (k = 0; k < nop + 1; k++) {
    // if (g[coord(k, j, nop_next + 1)] == 1) {
    // x = k;
    // break;
    // }
    // }
    //
    // // Look along the y-axis, including
    // // the dummy particles
    // for (k = 0; k < nop_next + 1; k++) {
    // if (g[coord(i, k, nop_next + 1)] == 1) {
    // y = k;
    // break;
    // }
    // }
    //
    // /* z is the reduced cost */
    // if (j == nop_next)
    // x = nop;
    // if (i == nop)
    // y = nop_next;
    //
    // z = cost[coord(i, j, nop_next + 1)] +
    // cost[coord(x, y, nop_next + 1)] -
    // cost[coord(i, y, nop_next + 1)] -
    // cost[coord(x, j, nop_next + 1)];
    // if (z > -1.0e-10)
    // z = 0.0;
    // if (z < min) {
    // min = z;
    // prev = coord(i, j, nop_next + 1);
    // prev_s = coord(x, y, nop_next + 1);
    // }
    // }
    // }
    // }
    //
    // if (min < 0.0) {
    // g[prev] = 1;
    // g[prev_s] = 1;
    // g[coord(prev / (nop_next + 1), prev_s % (nop_next + 1), nop_next + 1)] = 0;
    // g[coord(prev_s / (nop_next + 1), prev % (nop_next + 1), nop_next + 1)] = 0;
    // }
    // }
    //
    // /* After optimization, the particles needs to be linked */
    // for (i = 0; i < nop; i++) {
    // for (j = 0; j < nop_next; j++) {
    // if (g[coord(i, j, nop_next + 1)] == 1)
    // p1.elementAt(i).next[n] = j;
    // }
    // }
    // }
    //
    // if (m == (frames_number - curr_linkrange - 1) && curr_linkrange > 1)
    // curr_linkrange--;
    // }
    //
    // /* At the last frame all trajectories end */
    // for (i = 0; i < frames[frames_number - 1].getParticles().size(); i++) {
    // frames[frames_number - 1].getParticles().elementAt(i).special = false;
    // for (n = 0; n < linkrange; n++)
    // frames[frames_number - 1].getParticles().elementAt(i).next[n] = -1;
    // }
    // }

    public void saveDetected(MyFrame[] frames) {
        /* show save file user dialog with default file name 'frame' */
        final SaveDialog sd = new SaveDialog("Save Detected Particles", IJ.getDirectory("image"), "frame", "");
        // if user cancelled the save dialog
        if (sd.getDirectory() == null || sd.getFileName() == null) {
            return;
        }

        // for each frame - save the detected particles
        for (int i = 0; i < frames.length; i++) {
            if (!MosaicUtils.write2File(sd.getDirectory(), sd.getFileName() + "_" + i, frames[i].frameDetectedParticlesForSave(false).toString())) {
                // upon any problam savingto file - return
                IJ.log("Problem occured while writing to file.");
                return;
            }
        }

        return;
    }

}
