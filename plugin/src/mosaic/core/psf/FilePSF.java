package mosaic.core.psf;

import java.io.File;
import java.lang.reflect.Array;

import ij.IJ;
import ij.Macro;
import ij.gui.GenericDialog;
import io.scif.img.ImgOpener;
import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * 
 * Class that produce Gaussian images
 * 
 * @author Pietro Incardona
 *
 * @param <T> Type of image to produce FloatType, Short .......
 */

public class FilePSF<T extends RealType<T>> implements psf<T> , PSFGui
{
	Img<T> image;
	Class<T> clCreator;
	
	/**
	 * 
	 * Create an object
	 * 
	 * getParameters() create a GUI to get the required parameters
	 * convolve() convolve the image with the PSF
	 * FilePSF implements also RandomAccess to get the PSF value on one
	 * point.
	 * 
	 * PS if you want to generate a PSF image use GeneratePSF
	 * 
	 * @see psf<T>
	 * @see PSFGui
	 * 
	 * @param dim dimension
	 * @param cl give the class of the parameter T
	 */
	
	@SuppressWarnings("unchecked")
	public FilePSF(Class<T> cl)
	{
		clCreator = cl;

	}
	
	
	@Override
	public RandomAccess<T> copyRandomAccess()
	{
		return image.randomAccess();
	}

	@Override
	public int getIntPosition(int i) 
	{
		return image.randomAccess().getIntPosition(i);
	}

	@Override
	public long getLongPosition(int i) 
	{
		return image.randomAccess().getLongPosition(i);
	}

	@Override
	public void localize(int[] loc) 
	{
		image.randomAccess().localize(loc);
	}

	@Override
	public void localize(long[] loc) 
	{
		image.randomAccess().localize(loc);
	}

	@Override
	public double getDoublePosition(int i) 
	{
		return image.randomAccess().getDoublePosition(i);
	}

	@Override
	public float getFloatPosition(int i) 
	{
		return image.randomAccess().getFloatPosition(i);
	}

	@Override
	public void localize(float[] loc) 
	{
		image.randomAccess().localize(loc);
	}

	@Override
	public void localize(double[] loc) 
	{
		image.randomAccess().localize(loc);
	}

	@Override
	public int numDimensions() 
	{
		return image.numDimensions();
	}

	@Override
	public void bck(int i) 
	{
		image.randomAccess().bck(i);
	}

	@Override
	public void fwd(int i) 
	{
		image.randomAccess().fwd(i);
	}

	@Override
	public void move(Localizable arg) 
	{
		image.randomAccess().move(arg);
	}

	@Override
	public void move(int[] mv)
	{
		image.randomAccess().move(mv);
	}

	@Override
	public void move(long[] mv)
	{
		image.randomAccess().move(mv);
	}

	@Override
	public void move(int i, int j) 
	{
		image.randomAccess().move(i,j);
	}

	@Override
	public void setPosition(Localizable arg) 
	{
		image.randomAccess().setPosition(arg);
	}

	@Override
	public void setPosition(int[] pos_) 
	{
		image.randomAccess().setPosition(pos_);
	}

	@Override
	public void setPosition(long[] pos_) 
	{
		image.randomAccess().setPosition(pos_);
	}

	@Override
	public void setPosition(int i, int j)
	{
		image.randomAccess().setPosition(i,j);
	}

	@Override
	public void setPosition(long i, int j) 
	{
		image.randomAccess().setPosition(i,j);
	}

	@Override
	public Sampler<T> copy() 
	{
		return image.randomAccess().copy();
	}

	@Override
	public T get() 
	{
		return image.randomAccess().get();
	}

	@Override
	public void move(long i, int j) 
	{
		image.randomAccess().move(i,j);
	}

	@Override
	public void getParamenters() 
	{
		GenericDialog gd = new GenericDialog("File PSF");
		
		// File to open
		
		gd.addStringField("File","");
		
		gd.showDialog();
		
		if (gd.wasCanceled())
			return;
		
		String filename = gd.getNextString();
		
		// open the image
		
		ImgOpener imgOpener = new ImgOpener();
		image = (Img< T >) imgOpener.openImgs( filename ).get(0);
		
	}

	@Override
	public <S extends NumericType<S>> void convolve(RandomAccessibleInterval<S> img, S bound) 
	{
		Cursor< FloatType > cVModelImage = DevImage.cursor();
        int size = aLabelImage.getSize();
		for(int i=0; i < size && cVModelImage.hasNext() ; i++)
		{
			cVModelImage.fwd();
			int vLabel = aLabelImage.getLabelAbs(i);
            if (vLabel == aLabelImage.forbiddenLabel)
            {
                vLabel = 0; // Set Background value ??
            }
            
            if (Float.isInfinite((float)labelMap.get(vLabel).median))
            {
            	int debug = 0;
            	debug++;
            }
            	
			cVModelImage.get().set((float)labelMap.get(vLabel).median);

		}
		
		// Convolve
		
		new FFTConvolution< FloatType > (DevImage,m_PSF).run();
	}

	@Override
	public int[] getSuggestedSize() 
	{
		int sz[] = new int [pos.length];
		
		for (int i = 0 ; i < pos.length ; i++)
		{
			sz[i] = (int)(var[i].getRealDouble() * 8.0) + 1;
		}
		
		return sz;
	}

	@Override
	public void setSuggestedSize(int[] sz) 
	{
		// not used
		
	}

	@Override
	public void setCenter(int[] pos) 
	{
		for (int i = 0 ; i < pos.length ; i++)
		{
			this.offset[i].setReal(pos[i]);
		}
	}

	@Override
	public String getStringParameters() 
	{
		String opt = new String();
		
		if (var.length == 2)
			opt += "sigma_x="+var[0]+" sigma_y="+var[1]+" Dimensions="+var.length;
		else
			opt += "sigma_x="+var[0]+" sigma_y="+var[1]+" sigma_z="+var[2] + " Dimensions="+var.length;
		
		
		for (int i = 0 ; i < var.length-3 ; i++)
		{
			opt += "sigma_" + i + "=" + var[i+3];
		}
		
		return opt;
	}
