package mosaic.core.detection;


import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;
import mosaic.core.imageUtils.convolution.Convolver;
import mosaic.core.imageUtils.convolution.Kernel1D;
import mosaic.core.imageUtils.convolution.Kernel2D;
import mosaic.core.imageUtils.convolution.Kernel3D;
import mosaic.core.utils.DilateImage;
import mosaic.core.utils.DilateImageClij;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Vector;

/**
 * FeaturePointDetector detects the "real" particles in provided frames.
 */
public class FeaturePointDetector {
    private static final Logger logger = Logger.getLogger(FeaturePointDetector.class);
    
    public static enum Mode { ABS_THRESHOLD_MODE, PERCENTILE_MODE }

    // user defined parameters and settings
    private float iGlobalMax;
    private float iGlobalMin;
    private double iCutoff;
    private float iPercentile;
    private int iRadius;
    private float iAbsIntensityThreshold;
    private Mode iThresholdMode = Mode.PERCENTILE_MODE;
    private boolean iUseCLIJ = false;

    // Internal stuff
    private Vector<Particle> iParticles;
    private int[][] iMask;
    
    
    /**
     * @param aGlobalMax - global maximum taken from processed movie / image sequence
     * @param aGlobalMin - global minimum
     */
    public FeaturePointDetector(float aGlobalMax, float aGlobalMin) {
        iGlobalMax = aGlobalMax;
        iGlobalMin = aGlobalMin;
        setDetectionParameters(0.001f, 0.005f, 3, 0.0f, false, false);
    }

    /**
     * First phase of the algorithm - time and memory consuming !! <br>
     * Determines the "real" particles in this frame (only for frame constructed from Image) <br>
     * Converts the <code>original_ip</code> to <code>FloatProcessor</code>, normalizes it, convolutes and dilates it,
     * finds the particles, refine their position and filters out non particles
     * @return container with disovered particles
     */
    public Vector<Particle> featurePointDetection(ImageStack original_ips) {
        logger.debug("Input stack dims(x/y/z): " + original_ips.getWidth() + "/" + original_ips.getHeight() + "/" + original_ips.getSize() + " " + iGlobalMin + "-" + iGlobalMax);

        /*
         * Converting the original imageProcessor to float
         * This is a constraint caused by the lack of floating point precision of pixels
         * value in 16bit and 8bit image processors in ImageJ therefore, if the image is not
         * converted to 32bit floating point, false particles get detected
         */
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
        pointLocationsEstimation(restored_fps);
        
        /* Refinement of the point location - Step 3 of the algorithm */
        pointLocationsRefinement(restored_fps);
        
        /* Non Particle Discrimination(set a flag to particles) - Step 4 of the algorithm */
        nonParticleDiscrimination();

        return iParticles;
    }

    /**
     * Normalizes a given <code>ImageProcessor</code> to [0,1]. <br>
     * According to the pre determend global min and max pixel value in the movie. <br>
     * All pixel intensity values I are normalized as (I-gMin)/(gMax-gMin)
     * 
     * @param is ImageProcessor to be normalized
     * @param global_min
     * @param global_max
     */
    private void normalizeFrameFloat(ImageStack is, float global_min, float global_max) {
        for (int s = 1; s <= is.getSize(); s++) {
            final float[] pixels = (float[]) is.getPixels(s);
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = (pixels[i] - global_min) / (global_max - global_min);
            }
        }
    }

    /**
     * Finds and sets the threshold value for this frame given the
     * user defined percenticle and an ImageProcessor if the thresholdmode is PERCENTILE.
     * If not, the threshold is set to its absolute (normalized) value. There is only one parameter
     * used, either percent or aIntensityThreshold depending on the threshold mode.
     * 
     * @param ips ImageProcessor after conversion, normalization and restoration
     * @param percent the upper rth percentile to be considered as candidate Particles
     * @param absIntensityThreshold2 a intensity value which defines a threshold.
     * @return 
     */
    private float findThreshold(ImageStack ips, double percent, float absIntensityThreshold2) {
        if (iThresholdMode == Mode.ABS_THRESHOLD_MODE) {
            final StackStatistics stack_stats = new StackStatistics(new ImagePlus("", ips));
            final float max = (float) stack_stats.max;
            final float min = (float) stack_stats.min;

            // the percent parameter corresponds to an absolute value (not percent)
            float threshold = (absIntensityThreshold2 - iGlobalMin) / (iGlobalMax - iGlobalMin);
            float threshold2 = threshold * (max - min) + min;
            logger.debug("Calculated absolute threshold: " + threshold + " for params[" + absIntensityThreshold2 + ", " + iGlobalMin + ", " + iGlobalMax + "]");
            logger.debug("New min/max: " + min + "/" + max + " New threshold: " + threshold2);
            
            // the percent parameter corresponds to an absolute value (not percent)
            return (absIntensityThreshold2 - iGlobalMin) / (iGlobalMax - iGlobalMin);
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
     * @param ips ImageProcessor, should be after conversion, normalization and restoration
     */
    private void pointLocationsEstimation(ImageStack ips) {
        float threshold = findThreshold(ips, iPercentile, iAbsIntensityThreshold);
        /* do a grayscale dilation */

        final ImageStack dilated_ips = iUseCLIJ ?
                DilateImageClij.dilate(ips, iRadius) :
                DilateImage.dilate(ips, iRadius, 4);

        // new StackWindow(new ImagePlus("dilated ", dilated_ips));

        iParticles = new Vector<Particle>();
        /* loop over all pixels */
        final int height = ips.getHeight();
        final int width = ips.getWidth();
        
        // Since the pixel coordinate system has half-integer values in center of a pixel
        // use shift when we are dealing with image that has depth. For 2D images use 0.0
        float depthShift = ips.getSize() > 1 ? 0.5f : 0.0f;
        
        for (int s = 0; s < ips.getSize(); s++) {
            final float[] ips_pixels = (float[]) ips.getProcessor(s + 1).getPixels();
            final float[] ips_dilated_pixels = (float[]) dilated_ips.getProcessor(s + 1).getPixels();
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (ips_pixels[i * width + j] > threshold && ips_pixels[i * width + j] == ips_dilated_pixels[i * width + j]) { // check if pixel is a local maximum

                        /* and add each particle that meets the criteria to the particles array */
                        // (the starting point is the middle of the pixel and exactly on a focal plane:)
                        iParticles.add(new Particle(j + 0.5f, i + .5f, s + depthShift, -1));
                    }
                }
            }
        }
        logger.info("Detected " + iParticles.size() + " particles.");
    }

    private void pointLocationsRefinement(ImageStack ips) {
        final int mask_width = 2 * iRadius + 1;
        final int imageWidth = ips.getWidth();
        final int imageHeight = ips.getHeight();
        final int imageDepth = ips.getSize();
        
        /* Set every value that is smaller than 0 to 0 */
        for (int s = 0; s < ips.getSize(); ++s) {
            final float[] pixels = (float[]) ips.getPixels(s + 1);
            for (int i = 0; i < pixels.length; ++i) {
                if (pixels[i] < 0) {
                    pixels[i] = 0;
                }
            }
        }

        for (int m = 0; m < iParticles.size(); ++m) {
            final Particle currentParticle = iParticles.elementAt(m);
            float epsx = 0;
            float epsy = 0;
            float epsz = 0;
            
            do {
                currentParticle.m0 = 0;
                currentParticle.m1 = 0;
                currentParticle.m2 = 0;
                currentParticle.m3 = 0;
                currentParticle.m4 = 0;
                epsx = 0;
                epsy = 0;
                epsz = 0;
                
                for (int s = -iRadius; s <= iRadius; ++s) {
                    if (((int) currentParticle.iZ + s) < 0 || ((int) currentParticle.iZ + s) >= imageDepth) {
                        continue;
                    }
                    int z = (int) currentParticle.iZ + s;
                    float[] pixels = (float[]) ips.getPixels(z + 1);
                    
                    for (int k = -iRadius; k <= iRadius; ++k) {
                        if (((int) currentParticle.iY + k) < 0 || ((int) currentParticle.iY + k) >= imageHeight) {
                            continue;
                        }
                        int y = (int) currentParticle.iY + k;

                        for (int l = -iRadius; l <= iRadius; ++l) {
                            if (((int) currentParticle.iX + l) < 0 || ((int) currentParticle.iX + l) >= imageWidth) {
                                continue;
                            }
                            int x = (int) currentParticle.iX + l;
                            
                            float c = pixels[y * imageWidth + x] * iMask[s + iRadius][(k + iRadius) * mask_width + (l + iRadius)];
                            epsx += l * c;
                            epsy += k * c;
                            epsz += s * c;
                            currentParticle.m0 += c;
                            int squaredDistance = k * k + l * l + s * s;
                            currentParticle.m1 += (float) Math.sqrt(squaredDistance) * c;
                            currentParticle.m2 += squaredDistance * c;
                            currentParticle.m3 += (float) Math.pow(squaredDistance, 1.5f) * c;
                            currentParticle.m4 += (float) Math.pow(squaredDistance, 2) * c;
                        }
                    }
                }
                epsx /= currentParticle.m0;
                epsy /= currentParticle.m0;
                epsz /= currentParticle.m0;
                currentParticle.m1 /= currentParticle.m0;
                currentParticle.m2 /= currentParticle.m0;
                currentParticle.m3 /= currentParticle.m0;
                currentParticle.m4 /= currentParticle.m0;

                int tx = (int) (10.0 * epsx);
                int ty = (int) (10.0 * epsy);
                int tz = (int) (10.0 * epsz);

                if ((tx) / 10.0 > 0.5) {
                    if ((int) currentParticle.iX + 1 < imageWidth) {
                        currentParticle.iX++;
                    }
                }
                else if ((tx) / 10.0 < -0.5) {
                    if ((int) currentParticle.iX - 1 >= 0) {
                        currentParticle.iX--;
                    }
                }
                if ((ty) / 10.0 > 0.5) {
                    if ((int) currentParticle.iY + 1 < imageHeight) {
                        currentParticle.iY++;
                    }
                }
                else if ((ty) / 10.0 < -0.5) {
                    if ((int) currentParticle.iY - 1 >= 0) {
                        currentParticle.iY--;
                    }
                }
                if ((tz) / 10.0 > 0.5) {
                    if ((int) currentParticle.iZ + 1 < imageDepth) {
                        currentParticle.iZ++;
                    }
                }
                else if ((tz) / 10.0 < -0.5) {
                    if ((int) currentParticle.iZ - 1 >= 0) {
                        currentParticle.iZ--;
                    }
                }

                if ((tx) / 10.0 <= 0.5 && (tx) / 10.0 >= -0.5 && (ty) / 10.0 <= 0.5 && (ty) / 10.0 >= -0.5 && (tz) / 10.0 <= 0.5 && (tz) / 10.0 >= -0.5) {
                    break;
                }
            } while (epsx > 0.5 || epsx < -0.5 || epsy > 0.5 || epsy < -0.5 || epsz > 0.5 || epsz < -0.5);
            
            currentParticle.iX += epsx;
            currentParticle.iY += epsy;
            currentParticle.iZ += epsz;
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
        if (iParticles.size() == 1) {
            // If there is only one particle it should not be discriminated - big enough value will do the job.
            iParticles.elementAt(0).nonParticleDiscriminationScore = Float.MAX_VALUE;
            iParticles.elementAt(0).special = true;
        }
        
        // Find maximum coordinates of particles and clear/set proper features of particle.
        int maxX = 1;
        int maxY = 1;
        int maxZ = 1;
        for (int j = 0; j < iParticles.size(); ++j) {
            final Particle pJ = iParticles.elementAt(j);
            pJ.special = true;
            pJ.nonParticleDiscriminationScore = 0;
            maxX = Math.max((int) pJ.iX, maxX);
            maxY = Math.max((int) pJ.iY, maxY);
            maxZ = Math.max((int) pJ.iZ, maxZ);
        }

        // Remove duplicates (happens when dealing with artificial images)
        // Create a bitmap (with ghostlayers to not have to perform bounds checking)
        // +3 since:
        // +1 to max dimension to have correct array size (coordinates are 0-based) and then 
        // +1 to have ghost layer on left and 
        // +1 to have ghost layer on right
        final boolean[][][] takenPositions = new boolean[maxZ + 3][maxY + 3][maxX + 3];
        for (int j = 0; j < iParticles.size(); ++j) {
            final Particle pJ = iParticles.elementAt(j);
            boolean particleInNeighborhood = false;
            for (int oz = -1; !particleInNeighborhood && oz <= 1; ++oz) {
                for (int oy = -1; !particleInNeighborhood && oy <= 1; ++oy) {
                    for (int ox = -1; !particleInNeighborhood && ox <= 1; ++ox) {
                        if (takenPositions[(int) pJ.iZ + 1 + oz][(int) pJ.iY + 1 + oy][(int) pJ.iX + 1 + ox]) {
                            particleInNeighborhood = true;
                        }
                    }
                }
            }

            if (particleInNeighborhood) {
                pJ.special = false; // Mark particle as not valid
            }
            else {
                takenPositions[(int) pJ.iZ + 1][(int) pJ.iY + 1][(int) pJ.iX + 1] = true; // Mark position as occupied
            }
        }
        
        // Calculate Nt - number of valid particles
        int Nt = 0;
        for (int j = 0; j < iParticles.size(); ++j) {
            if (iParticles.elementAt(j).special == true) ++Nt;
        }
        logger.debug("Detected " + Nt + " non duplicated particles.");
        
        // Calculate score for each valid particle
        int countValid = 0;
        double sigma0 = 0.1;
        double sigma2 = 0.1;
        for (int j = 0; j < iParticles.size(); ++j) {
            final Particle pJ = iParticles.elementAt(j);
            if (!pJ.special) continue; // Skip not valid particle

            for (int k = j; k < iParticles.size(); ++k) {
                final Particle pK = iParticles.elementAt(k);
                if (!pK.special) continue; // Skip not valid particle
                double score = (1.0 / (2.0 * Math.PI * sigma0 * sigma2 * Nt)) * Math.exp(- Math.pow((pJ.m0 - pK.m0), 2) / (2.0 * sigma0 * sigma0) - Math.pow((pJ.m2 - pK.m2), 2) / (2.0 * sigma2 * sigma2));
                pJ.nonParticleDiscriminationScore += score;
                if (j != k) pK.nonParticleDiscriminationScore += score;
            }
            
            // Normalize score
            pJ.nonParticleDiscriminationScore /= 1/(2.0 * Math.PI * sigma0 * sigma2);
            
            if (pJ.nonParticleDiscriminationScore < iCutoff) {
                pJ.special = false;  // Mark particle as not valid
            }
            else {
                countValid++;
            }
        }
        logger.info("Detected " + countValid + " after non particle discrimination phase.");
    }

    /**
     * Corrects imperfections in the given <code>ImageStack</code> by
     * convolving it (slice by slice, not 3D) with the pre calculated <code>kernel</code>
     * 
     * @param is ImageStack to be restored
     * @return the restored <code>ImageProcessor</code>
     */
    private ImageStack imageRestoration(ImageStack is) {
        Convolver img = new Convolver(is.getWidth(), is.getHeight(), is.getSize() /*depth*/);
        img.initFromImageStack(is);
        
        Convolver gaussImg = new Convolver(is.getWidth(), is.getHeight(), is.getSize() /*depth*/);
        Kernel1D gauss = new GaussSeparable1D(1, iRadius);
        gaussImg.x1D(img, gauss);
        gaussImg.y1D(gauss);
        
        Convolver backgroundImg = new Convolver(is.getWidth(), is.getHeight(), is.getSize() /*depth*/);
        Kernel1D carbox = new CarBoxSeparable1D(iRadius);
        backgroundImg.x1D(img, carbox);
        backgroundImg.y1D(carbox);
        
        boolean is3D = is.getSize() > 1;
        if (is3D) { // for 3D handle also z-direction
            gaussImg.z1D(gauss);
            backgroundImg.z1D(carbox);
        }
        
        // Calculate final result = (1/K0) * (IMG*G - IMG*B)
        gaussImg.sub(backgroundImg).div(calculateK0(is3D ? 3 : 2, 1, iRadius));
        
        return gaussImg.getImageStack();
    }

    public double calculateK0(int dimension, int lambda, int radius) {
        // Calculate B normalization
        double B = 0.0; 
        for(int i = -radius; i <= radius; i++) { 
            B += Math.exp(-(i * i)/(4.0 * lambda * lambda));
        }
        B = Math.pow(B, dimension);
    
        // Calculate K0 normalization
        double K0 = 0.0; 
        for(int i = -radius; i <= radius; i++) { 
            K0 += Math.exp(-(i * i)/(2.0 * lambda * lambda));
        }
        int width = (2 * radius) + 1; 
        K0 = Math.pow(K0, dimension) / B - B / Math.pow(width, dimension);
        
        return K0;
    }
    
    public class GaussSeparable1D extends Kernel1D
    {
        public GaussSeparable1D(int lambda, int radius) {
            k = gaussSeparable1D(lambda, radius);
            iHalfWidth = k.length / 2;
        }
        
        private double[] gaussSeparable1D(double lambda, int radius) {
            int width = (2 * radius) + 1; 
            double[] kernel = new double[width];

            // Calculate B normalization
            double B = 0.0; 
            for(int i = -radius; i <= radius; ++i) { 
                B += Math.exp(-(i * i)/(4.0 * lambda * lambda));
            }
            
            // Calculate kernel
            for (int i = -radius; i <= radius; ++i) {
                kernel[i + radius] = (1.0 / B) * Math.exp(-((i * i) / (4.0 * lambda * lambda)));
            }    

            return kernel;
        }
    }
    
    public class CarBoxSeparable1D extends Kernel1D
    {
        public CarBoxSeparable1D(int radius) {
            int width = (2 * radius) + 1; 
            k = new double[width];
            Arrays.fill(k, 1.0 / width);
            iHalfWidth = radius;
        }
    }
    
    public class RestorationKernel2D extends Kernel2D
    {
        public RestorationKernel2D(int lambda, int radius) {
            k = resorationKernel2D(lambda, radius);
            int width = k[0].length;
            iHalfWidth = width / 2;
        }
        
        private double[][] resorationKernel2D(float lambda, int radius) {
            int width = (2 * radius) + 1; 
            double[][] kernel = new double[width][width];

            // Calculate B normalization
            double B = 0.0; 
            for(int i = -radius; i <= radius; i++) { 
                B += Math.exp(-(i * i)/(4.0 * lambda * lambda));
            }
            B = B * B;

            // Calculate K0 normalization
            double K0 = 0.0; 
            for(int i = -radius; i <= radius; i++) { 
                K0 += Math.exp(-(i * i)/(2.0 * lambda * lambda));
            }
            K0 = K0 * K0 / B - B / (width * width);

            // Calculate kernel
            for (int i = -radius; i <= radius; i++) {
                for (int j = -radius; j <= radius; j++) {
                    kernel[i + radius][ j + radius] = (1.0f / B) * Math.exp(-((i * i + j * j) / (4.0f * lambda * lambda))) - (1.0f / (width * width));
                    kernel[i + radius][ j + radius] /= K0;
                }    
            }            

            return kernel;
        }
        
        @Override
        public String toString() { return "Gauss/BoxCar 2D"; }
    }
    
    public class RestorationKernel3D extends Kernel3D
    {
        public RestorationKernel3D(int lambda, int radius) {
            k = resorationKernel3D(lambda, radius);
            int width = k[0].length;
            iHalfWidth = width / 2;
        }

        private double[][][] resorationKernel3D(float lambda, int radius) {
            int width = (2 * radius) + 1; 
            double[][][] kernel = new double[width][width][width];

            // Calculate B normalization
            double B = 0.0; 
            for(int i = -radius; i <= radius; i++) { 
                B += Math.exp(-(i * i)/(4.0 * lambda * lambda));
            }
            B = B * B * B;

            // Calculate K0 normalization
            double K0 = 0.0; 
            for(int i = -radius; i <= radius; i++) { 
                K0 += Math.exp(-(i * i)/(2.0 * lambda * lambda));
            }
            K0 = K0 * K0 * K0 / B - B / (width * width * width);

            // Calculate kernel
            for (int i = -radius; i <= radius; i++) {
                for (int j = -radius; j <= radius; j++) {
                    for (int k = -radius; k <= radius; k++) {
                        kernel[i + radius][ j + radius][k + radius] = (1.0f / B) * Math.exp(-((i * i + j * j + k * k) / (4.0f * lambda * lambda))) - (1.0f / (width * width * width));
                        kernel[i + radius][ j + radius][k + radius] /= K0;
                    }
                }    
            }            

            return kernel;
        }

        @Override
        public String toString() { return "Gauss/BoxCar 3D"; }
    }
    
    /**
     * (Re)Initialize the binary and weighted masks. This is necessary if the radius changed.
     * @param mask_radius
     */
    private void generateDilationMasks(int mask_radius) {
        iMask = DilateImage.generateMask(mask_radius);
    }

    /**
     * Sets user defined params that are necessary to particle detection
     * and generates the <code>mask</code> according to these params
     * @return 
     * 
     * @see #generateDilationMasks(int)
     */
    public boolean setDetectionParameters(double cutoff, float percentile, int radius, float Threshold, boolean absolute, boolean aUseClij) {
        final boolean changed = (radius != iRadius || cutoff != iCutoff || (percentile != iPercentile));// && intThreshold != absIntensityThreshold || mode != getThresholdMode() || thsmode != getThresholdMode();

        iCutoff = cutoff;
        iPercentile = percentile;
        iAbsIntensityThreshold = Threshold;
        iRadius = radius;
        if (absolute == true) {
            iThresholdMode = Mode.ABS_THRESHOLD_MODE;
        }
        else {
            iThresholdMode = Mode.PERCENTILE_MODE;
        }

        iUseCLIJ = aUseClij;

        logger.info("Detection options: radius=" + iRadius + " cutoff=" + iCutoff + " percentile=" + iPercentile + " threshold=" + iAbsIntensityThreshold + " mode=" + (absolute ? "THRESHOLD" : "PERCENTILE") + " useCLIJ=" + iUseCLIJ);
        System.out.println("Detection options: radius=" + iRadius + " cutoff=" + iCutoff + " percentile=" + iPercentile + " threshold=" + iAbsIntensityThreshold + " mode=" + (absolute ? "THRESHOLD" : "PERCENTILE") + " useCLIJ=" + iUseCLIJ);

        // create Mask for Dilation with the user defined radius
        generateDilationMasks(iRadius);
        
        return changed;
    }

    public int getRadius() {
        return iRadius;
    }
    
    public Mode getThresholdMode() {
        return iThresholdMode;
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
        return iCutoff;
    }

    public float getPercentile() {
        return iPercentile;
    }
    
    public float getAbsIntensityThreshold() {
        return iAbsIntensityThreshold;
    }
}
