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
};
