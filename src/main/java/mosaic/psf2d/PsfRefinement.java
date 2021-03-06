package mosaic.psf2d;


import ij.plugin.filter.Convolver;
import ij.process.ImageProcessor;


/**
 * Refines Point Source Positions to floating point accuracy based on intensity centroid calculation. <br>
 * Code for Refinement is mainly taken from <a href="http://weeman.inf.ethz.ch/ParticleTracker/">
 * Particle Tracker<a> developed at CBL, ETH Zurich, Switzerland.
 */
public class PsfRefinement {

    /** Radius that defines area of position refinement */
    private final int radius;

    // some variables for the refinement algorithm
    private int[] mask;
    final private int lambda_n = 1;
    private float[] kernel;
    private final PsfSourcePosition particle;

    private final ImageProcessor original_ip; // the original image
    private ImageProcessor original_fp; // the original image after convertion to float processor
    private ImageProcessor restored_fp; // the floating processor after image restoration

    /**
     * Constructor
     * 
     * @param ip ImageProcessor to work on
     * @param rad Radius in which centroid should be calculated
     * @param pat Particle holding position information
     */
    public PsfRefinement(ImageProcessor ip, int rad, PsfSourcePosition pat) {
        this.radius = rad;
        this.original_ip = ip;
        this.particle = pat;
        // create Kernel and Mask for imageRestoration with the user defined radius
        makeKernel(radius);
        generateMask(radius);
    }

    /**
     * Refines particle position <br>
     * Converts the <code>original_ip</code> to <code>FloatProcessor</code>, normalizes and convolutes it,
     * refines particle position to sub-pixel accuracy based on centriod-detection using internal methods
     * 
     * @see ImageProcessor#convertToFloat()
     * @see #normalizeFrameFloat(ImageProcessor)
     * @see #imageRestoration(ImageProcessor)
     * @see #pointLocationsRefinement(ImageProcessor)
     */
    public void refineParticlePosition() {

        /*
         * Converting the original imageProcessor to float
         * This is a constraint caused by the lack of floating point precision of pixels
         * value in 16bit and 8bit image processors in ImageJ
         */
        this.original_fp = this.original_ip.convertToFloat();

        /* Image Restoration */
        this.restored_fp = imageRestoration(this.original_fp);

        /* Refinement of the point location */
        pointLocationsRefinement(this.restored_fp);
    }

    /**
     * Corrects imperfections in the given <code>ImageProcessor</code> by
     * convolving it with the pre calculated <code>kernel</code>
     * 
     * @param ip ImageProcessor to be restored
     * @return the restored <code>ImageProcessor</code>
     * @see Convolver#convolve(ij.process.ImageProcessor, float[], int, int)
     * @see ParticleTracker_#kernel
     */
    private ImageProcessor imageRestoration(ImageProcessor ip) {

        final ImageProcessor restored = ip.duplicate();
        final int kernel_radius = radius;
        final int kernel_width = (kernel_radius * 2) + 1;
        final Convolver convolver = new Convolver();
        // no need to normalize the kernel - its already normalized
        convolver.setNormalize(false);
        convolver.convolve(restored, kernel, kernel_width, kernel_width);
        return restored;
    }

    /**
     * The positions of the found particles will be refined according to their momentum terms <br>
     * Adapted "as is" from Ingo Oppermann implementation
     * 
     * @param ip ImageProcessor, should be after conversion, normalization and restoration
     */
    private void pointLocationsRefinement(ImageProcessor ip) {

        int k, l, x, y, tx, ty;
        float epsx, epsy, c;

        final int mask_width = 2 * radius + 1;

        /* Set every value that ist smaller than 0 to 0 */
        for (int i = 0; i < ip.getHeight(); i++) {
            for (int j = 0; j < ip.getWidth(); j++) {
                if (ip.getPixelValue(j, i) < 0.0) {
                    ip.putPixelValue(j, i, 0.0);
                }
            }
        }

        /* Calculate Refined Positions */
        epsx = epsy = 1.0F;

        while (epsy > 0.5 || epsy < -0.5 || epsx > 0.5 || epsx < -0.5) {
            float m0 = 0.0F;
            epsx = 0.0F;
            epsy = 0.0F;

            for (k = -radius; k <= radius; k++) {
                if (((int) particle.iY + k) < 0 || ((int) particle.iY + k) >= ip.getHeight()) {
                    continue;
                }
                y = (int) particle.iY + k;

                for (l = -radius; l <= radius; l++) {
                    if (((int) particle.iX + l) < 0 || ((int) particle.iX + l) >= ip.getWidth()) {
                        continue;
                    }
                    x = (int) particle.iX + l;

                    c = ip.getPixelValue(x, y) * mask[coord(k + radius, l + radius, mask_width)];
                    m0 += c;
                    epsy += k * c;
                    epsx += l * c;
                }
            }

            epsy /= m0;
            epsx /= m0;

            // This is a little hack to avoid numerical inaccuracy
            tx = (int) (10.0 * epsx);
            ty = (int) (10.0 * epsy);

            if ((ty) / 10.0 > 0.5) {
                if ((int) particle.iY + 1 < ip.getHeight()) {
                    particle.iY++;
                }
            }
            else if ((ty) / 10.0 < -0.5) {
                if ((int) particle.iY - 1 >= 0) {
                    particle.iY--;
                }
            }
            if ((tx) / 10.0 > 0.5) {
                if ((int) particle.iX + 1 < ip.getWidth()) {
                    particle.iX++;
                }
            }
            else if ((tx) / 10.0 < -0.5) {
                if ((int) particle.iX - 1 >= 0) {
                    particle.iX--;
                }
            }

            if ((ty) / 10.0 <= 0.5 && (ty) / 10.0 >= -0.5 && (tx) / 10.0 <= 0.5 && (tx) / 10.0 >= -0.5) {
                break;
            }
        }

        particle.iX += epsx;
        particle.iY += epsy;
    }

    /**
     * Generates the dilation mask. <code>mask</code> is a var of class ParticleTracker_ and its modified internally here
     * Adapted from Ingo Oppermann implementation
     * 
     * @param mask_radius the radius of the mask (user defined)
     */
    private void generateMask(int mask_radius) {

        final int width = (2 * mask_radius) + 1;
        this.mask = new int[width * width];

        for (int i = -mask_radius; i <= mask_radius; i++) {
            for (int j = -mask_radius; j <= mask_radius; j++) {
                final int index = coord(i + mask_radius, j + mask_radius, width);
                if ((i * i) + (j * j) <= mask_radius * mask_radius) {
                    this.mask[index] = 1;
                }
                else {
                    this.mask[index] = 0;
                }

            }
        }
    }

    /**
     * Generates the Convolution Kernel as described in the Image Restoration
     * part of the original algorithm. <code>kernel</code> is a var of class ParticleTracker_ and is modified internally here
     * 
     * @param kernel_radius (the radius of the kernel (user defined))
     */
    private void makeKernel(int kernel_radius) {

        final int kernel_width = (kernel_radius * 2) + 1;
        this.kernel = new float[kernel_width * kernel_width];
        final double b = calculateB(kernel_radius, lambda_n);
        final double norm_cons = calculateNormalizationConstant(b, kernel_radius, lambda_n);

        for (int i = -kernel_radius; i <= kernel_radius; i++) {
            for (int j = -kernel_radius; j <= kernel_radius; j++) {
                final int index = (i + kernel_radius) * kernel_width + j + kernel_radius;
                this.kernel[index] = (float) ((1.0 / b) * Math.exp(-((i * i + j * j) / (4.0 * lambda_n * lambda_n))));
                this.kernel[index] = this.kernel[index] - (float) (1.0 / (kernel_width * kernel_width));
                this.kernel[index] = (float) (this.kernel[index] / norm_cons);
            }
        }
    }

    /**
     * Auxiliary function for the kernel generation.
     * 
     * @param kernel_radius
     * @param lambda
     * @return the calculated B parameter
     * @see #makeKernel(int)
     */
    private double calculateB(int kernel_radius, int lambda) {
        double b = 0.0;
        for (int i = -(kernel_radius); i <= kernel_radius; i++) {
            b = b + Math.exp(-((i * i) / (4.0 * (lambda * lambda))));
        }
        b = b * b;
        return b;
    }

    /**
     * Auxiliary function for the kernel generation.
     * 
     * @param b
     * @param kernel_radius
     * @param lambda
     * @return the calculated normalization constant
     * @see #makeKernel(int)
     */
    private double calculateNormalizationConstant(double b, int kernel_radius, int lambda) {
        double constant = 0.0;
        final int kernel_width = (kernel_radius * 2) + 1;
        for (int i = -(kernel_radius); i <= kernel_radius; i++) {
            constant = constant + Math.exp(-(i * i / (2.0 * (lambda * lambda))));
        }
        constant = ((constant * constant) / b) - (b / (kernel_width * kernel_width));
        return constant;
    }

    /**
     * Auxiliary function for Dilation Mask generation.
     * 
     * @param a
     * @param b
     * @param c
     * @return (((a) * (c)) + (b))
     * @see #generateMask(int)
     */
    private int coord(int a, int b, int c) {
        return (((a) * (c)) + (b));
    }

    public PsfSourcePosition getRefinedParticle() {
        return particle;
    }
}
