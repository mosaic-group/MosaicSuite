package mosaic.core.psf;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.NumericType;


public interface psf<T> extends RandomAccess<T>, PSFGui
{
	/**
	 * 
	 * Convolve the image with the PSF
	 * 
	 * @param img Image
	 * @return the convolved image
	 */
	
	public <S extends NumericType<S>> void convolve(RandomAccessibleInterval<S> img, S bound);
	
	/**
	 * 
	 * Get the suggested size of the image to reppresent the PSF
	 * 
	 * @return suggested size
	 */
	
	public int[] getSuggestedSize();
	
	/**
	 * 
	 * Set the suggested size to reppresent the PSF, can be used to internally create lookup table
	 * and speedup the process
	 * 
	 */
	
	public void setSuggestedSize(int [] sz);
	
	/**
	 * 
	 * Center the position of the PSF
	 * 
	 * @param pos position
	 */
	
	public void setCenter(int[] pos);
};
