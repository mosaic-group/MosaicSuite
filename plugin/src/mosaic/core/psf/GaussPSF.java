package mosaic.core.psf;

import java.lang.reflect.Array;

import ij.IJ;
import ij.Macro;
import ij.gui.GenericDialog;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
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

public class GaussPSF<T extends RealType<T>> implements psf<T> , PSFGui
{
	RealType<T> pos[];
	RealType<T> var[];
	RealType<T> offset[];
	Class<T> clCreator;
	
	/**
	 * 
	 * Create a Gaussian object
	 * 
	 * getParameters() create a GUI to get the required parameters
	 * convolve() convolve the image with the PSF
	 * GaussPSF implements also RandomAccess to get the PSF value on one
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
	public GaussPSF(int dim , Class<T> cl)
	{
		clCreator = cl;
		pos = (RealType<T>[]) Array.newInstance(cl,dim);
		var = (RealType<T>[]) Array.newInstance(cl,dim);
		offset = (RealType<T>[]) Array.newInstance(cl,dim);
		
		try {
		
		for (int i = 0 ; i < dim ; i++)
		{
				pos[i] = cl.newInstance();
				var[i] = cl.newInstance();
				offset[i] = cl.newInstance();
		}
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void setVar(RealType<T> var_[])
	{
		var = var_;
	}
	
	@Override
	public RandomAccess<T> copyRandomAccess()
	{
		return this;
	}

	@Override
	public int getIntPosition(int i) 
	{
		return (int)pos[i].getRealDouble();
	}

	@Override
	public long getLongPosition(int i) 
	{
		return (int)pos[i].getRealDouble();
	}

	@Override
	public void localize(int[] loc) 
	{
		for (int i = 0 ; i < loc.length ; i++)
		{
			pos[i].setReal(loc[i]);
		}
	}

	@Override
	public void localize(long[] loc) 
	{
		for (int i = 0 ; i < loc.length ; i++)
		{
			pos[i].setReal(loc[i]);
		}
	}

	@Override
	public double getDoublePosition(int i) 
	{
		return pos[i].getRealDouble();
	}

	@Override
	public float getFloatPosition(int i) 
	{
		return pos[i].getRealFloat();
	}

	@Override
	public void localize(float[] loc) 
	{
		for (int i = 0 ; i < loc.length ; i++)
		{
			pos[i].setReal(loc[i]);
		}
	}

	@Override
	public void localize(double[] loc) 
	{
		for (int i = 0 ; i < loc.length ; i++)
		{
			pos[i].setReal(loc[i]);
		}
	}

	@Override
	public int numDimensions() 
	{
		return pos.length;
	}

	@Override
	public void bck(int i) 
	{
		pos[i].setReal(pos[i].getRealDouble() + 1.0);
	}

	@Override
	public void fwd(int i) 
	{
		pos[i].setReal(pos[i].getRealDouble() + 1.0);
	}

	@Override
	public void move(Localizable arg) 
	{
		for (int i = 0 ; i < arg.numDimensions() ; i++)
		{
			pos[i].setReal(pos[i].getRealDouble() + arg.getDoublePosition(i));
		}
	}

	@Override
	public void move(int[] mv)
	{
		for (int i = 0 ; i < mv.length ; i++)
		{
			pos[i].setReal(mv[i]);
		}
	}

	@Override
	public void move(long[] mv)
	{
		for (int i = 0 ; i < mv.length ; i++)
		{
			pos[i].setReal(pos[i].getRealDouble() + mv[i]);
		}
	}

	@Override
	public void move(int i, int j) 
	{
		pos[i].setReal(j);
	}

	@Override
	public void setPosition(Localizable arg) 
	{
		for (int i = 0 ; i < arg.numDimensions() ; i++)
		{
			pos[i].setReal(arg.getDoublePosition(i));
		}
	}

	@Override
	public void setPosition(int[] pos_) 
	{
		for (int i = 0 ; i < pos.length ; i++)
		{
			pos[i].setReal(pos_[i]);
		}
	}

	@Override
	public void setPosition(long[] pos_) 
	{
		for (int i = 0 ; i < pos.length ; i++)
		{
			pos[i].setReal(pos_[i]);
		}
	}

	@Override
	public void setPosition(int i, int j)
	{
		pos[i].setReal(j);
	}

	@Override
	public void setPosition(long i, int j) 
	{
		pos[(int)i].setReal(j);
	}

	@Override
	public Sampler<T> copy() 
	{
		return this;
	}

	@Override
	public T get() 
	{
		double res = 1.0;
		
		for (int i = 0 ; i < pos.length ; i++)
		{
			res *= 1.0 / Math.sqrt(2.0 * Math.PI * var[i].getRealDouble()) * Math.exp(-(pos[i].getRealDouble() - offset[i].getRealDouble())*(pos[i].getRealDouble() - offset[i].getRealDouble())/ (2.0 * var[i].getRealDouble() * var[i].getRealDouble()));
		}
		RealType<T> rc = pos[0].createVariable();
		rc.setReal(res);
		T rt = null;
		try {
			rt = clCreator.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		rt.setReal(res);
		return rt;
	}

	@Override
	public void move(long i, int j) 
	{
		pos[(int)i].setReal(pos[(int)i].getRealDouble() + j);
	}

	@Override
	public void getParamenters() 
	{
		GenericDialog gd = new GenericDialog("Gauss PSF");
		
		// sigma
		
		String test = Macro.getOptions();
		
		if (pos.length >= 1)
			gd.addNumericField("sigma_X", 1.0, 3);
		if (pos.length >= 2)
			gd.addNumericField("sigma_Y", 1.0, 3);
		if (pos.length >= 3)
			gd.addNumericField("sigma_Z", 1.0, 3);
		
		for (int i = 0 ; i < pos.length-3 ; i++)
		{
			gd.addNumericField("sigma_" + i, 1.0, 3);
		}
		
		for (int i = 0 ; i < pos.length-3 ; i++)
		{
			gd.addNumericField("mean_" + i, 1.0, 3);
		}
		
		gd.showDialog();
		
		if (gd.wasCanceled())
			return;
		
		for (int i = 0 ; i < var.length ; i++)
		{
			var[i].setReal(gd.getNextNumber());
		}
	}

	@Override
	public <S extends NumericType<S>> void convolve(RandomAccessibleInterval<S> img, S bound) 
	{
		final double[] sigma = new double[ img.numDimensions() ];
		
		if (var == null)
		{
			for ( int d = 0; d < sigma.length; ++d )
			{sigma[ d ] = 1.0;}
		}
		else
		{
			for ( int d = 0; d < sigma.length; ++d )
			{sigma[ d ] = var[d].getRealDouble();}
		}
		
		RandomAccessible< S > infiniteImg = Views.extendValue( img, bound);
		
		// Convolve with gaussian;
		
		try
		{Gauss3.gauss(sigma, infiniteImg, img);}
		catch (IncompatibleTypeException e) {e.printStackTrace();}
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
};