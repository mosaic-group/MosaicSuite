package mosaic.core.psf;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface psf<T extends RealType<T>> extends RandomAccess<T>, PSFGui {
    /**
     * 
     * Convolve the image with the PSF
     * 
     * @param img
     *            Image
     * @return the convolved image
     */

    public <S extends RealType<S>> void convolve(
            RandomAccessibleInterval<S> img, S bound);

    /**
     * 
     * Get the suggested size of the image to reppresent the PSF
     * 
     * @return suggested size
     */

    public int[] getSuggestedImageSize();

    /**
     * 
     * Set the suggested size to reppresent the PSF, can be used to internally
     * create lookup table and speedup the process
     * 
     */

    public void setSuggestedImageSize(int[] sz);

    /**
     * 
     * Get the center the position of the PSF
     * 
     * @return center position
     */

    public int[] getCenter();

    /**
     * 
     * Center the position of the PSF
     * 
     * @param pos
     *            position
     */

    public void setCenter(int[] pos);

    /**
     * 
     * Is this psf comming from a file
     * 
     * @return true if come from a file false otherwise
     */

    public boolean isFile();

    /**
     * 
     * Is this PSF separable
     * 
     * @return true if is separable
     */

    public boolean isSeparable();

    /**
     * 
     * Get the image for the kernel on one direction. Useful if the kernel is
     * separable. The origin is always in the center of the image
     * 
     * @param dim
     *            direction of the image
     * 
     * @return the 1D image of the kernel
     */

    public double[] getSeparableImageAsDoubleArray(int dim);

    /**
     * 
     * Get the image for the kernel on one direction. Useful if the kernel is
     * separable. The origin is always in the center of the image
     * 
     * @param dim
     *            direction of the image
     * 
     * @return the 1D image of the kernel
     */

    public float[] getSeparableImageAsFloatArray(int dim);

    /**
     * 
     * Get the image for the kernel as 3D array. The origin is always in the
     * center of the image
     * 
     * 
     * @return 3D Array of the PSF image
     */
    // @Deprecated
    public float[][][] getImage3DAsFloatArray();

    /**
     * 
     * Get the image for the kernel as 3D array. The origin is always in the
     * center of the image
     * 
     * 
     * @return 3D array of the PSF image
     */
    // @Deprecated
    public double[][][] getImage3DAsDoubleArray();

    /**
     * 
     * Get the image for the kernel as 2D array. The origin is always in the
     * center of the image
     * 
     * 
     * @return 2D array of the PSF image
     */
    // @Deprecated
    public double[][] getImage2DAsDoubleArray();

    /**
     * 
     * Get the image for the kernel as 2D array. The origin is always in the
     * center of the image
     * 
     * 
     * @return 2D array of the PSF image
     */
    // @Deprecated
    public float[][] getImage2DAsFloatArray();
};
