package mosaic.core.psf;

import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;

import ij.IJ;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import mosaic.region_competition.Settings;
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
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

class FilePSFSettings implements Serializable
{
	private static final long serialVersionUID = 1777976543628904166L;
	
	String filePSF;
}

/**
 * 
 * Class that produce Gaussian images
 * 
 * @author Pietro Incardona
 *
 * @param <T> Type of image to produce FloatType, Short .......
 */

public class FilePSF<T extends RealType<T> & NativeType<T>> implements psf<T> , PSFGui
{	
	FilePSFSettings settings = new FilePSFSettings();
	Img<T> image;
	RandomAccess<T> rd;
	Class<T> clCreator;
	String filename;
	int offset[];
	double [][] Image2DD;
	double [][][] Image3DD;
	float [][] Image2DF;
	float [][][] Image3DF;
	
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
		return rd;
	}

	@Override
	public int getIntPosition(int i) 
	{
		return rd.getIntPosition(i);
	}

	@Override
	public long getLongPosition(int i) 
	{
		return rd.getLongPosition(i);
	}

	@Override
	public void localize(int[] loc) 
	{
		rd.localize(loc);
	}

	@Override
	public void localize(long[] loc) 
	{
		rd.localize(loc);
	}

	@Override
	public double getDoublePosition(int i) 
	{
		return rd.getDoublePosition(i);
	}

	@Override
	public float getFloatPosition(int i) 
	{
		return rd.getFloatPosition(i);
	}

	@Override
	public void localize(float[] loc) 
	{
		rd.localize(loc);
	}

	@Override
	public void localize(double[] loc) 
	{
		rd.localize(loc);
	}

	@Override
	public int numDimensions() 
	{
		return rd.numDimensions();
	}

	@Override
	public void bck(int i) 
	{
		rd.bck(i);
	}

	@Override
	public void fwd(int i) 
	{
		rd.fwd(i);
	}

	@Override
	public void move(Localizable arg) 
	{
		rd.move(arg);
	}

	@Override
	public void move(int[] mv)
	{
		rd.move(mv);
	}

	@Override
	public void move(long[] mv)
	{
		rd.move(mv);
	}

	@Override
	public void move(int i, int j) 
	{
		rd.move(i,j);
	}

	@Override
	public void setPosition(Localizable arg) 
	{
		rd.setPosition(arg);
	}

	@Override
	public void setPosition(int[] pos_) 
	{
		rd.setPosition(pos_);
	}

	@Override
	public void setPosition(long[] pos_) 
	{
		rd.setPosition(pos_);
	}

	@Override
	public void setPosition(int i, int j)
	{
		rd.setPosition(i,j);
	}

	@Override
	public void setPosition(long i, int j) 
	{
		rd.setPosition(i,j);
	}

	@Override
	public Sampler<T> copy() 
	{
		return rd.copy();
	}

	@Override
	public T get() 
	{
		return rd.get();
	}

	@Override
	public void move(long i, int j) 
	{
		rd.move(i,j);
	}
	
	@Override
	public void getParamenters() 
	{
		settings.filePSF = new String();
		LoadConfigFile(IJ.getDirectory("temp")+ File.separator + "psf_file_settings.dat");
		
		GenericDialog gd = new GenericDialog("File PSF");
		
		// File to open
		
		gd.addStringField("File",settings.filePSF);
		final TextField PSFc = (TextField)gd.getStringFields().lastElement();
		{
			Button optionButton = new Button("Choose");
			GridBagConstraints c = new GridBagConstraints();
			int gridx = 2;
			int gridy = 0;
			c.gridx=gridx;
			c.gridy=gridy++; c.anchor = GridBagConstraints.EAST;
			gd.add(optionButton,c);
		
			optionButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					OpenDialog op = new OpenDialog("Choose PSF file",null);
					
					filename = op.getDirectory() + File.separator + op.getFileName();
					PSFc.setText(filename);
				}
			});
		}
		
		gd.showDialog();
		
		if (gd.wasCanceled())
			return;
		
		String filename = gd.getNextString();
		
		// open the image
		
		final ImgFactory<T> factory = new ArrayImgFactory<T>();
		ImgOpener imgOpener = new ImgOpener();
		try {
			try {
				image = (Img< T >) imgOpener.openImgs( filename, factory, clCreator.newInstance()).get(0);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ImgIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (image == null)
			return;
		
		rd = image.randomAccess();
		
		// save the parameters
		
		settings.filePSF = filename;
		try {
			SaveConfigFile(IJ.getDirectory("temp")+ File.separator + "psf_file_settings.dat",settings);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public <S extends RealType<S>> void convolve(RandomAccessibleInterval<S> img, S bound) 
	{		
		// Convolve
		
		new FFTConvolution< S > (img,(RandomAccessibleInterval<S>) image, new ArrayImgFactory<ComplexFloatType>()).run();
	}

	@Override
	public int[] getSuggestedImageSize() 
	{
		if (image == null)
			return null;
			
		int sz[] = new int [image.numDimensions()];
		
		for (int i = 0 ; i < image.numDimensions() ; i++)
		{
			sz[i] = (int)image.dimension(i);
		}
		
		return sz;
	}

	@Override
	public void setSuggestedImageSize(int[] sz) 
	{
		// not used
		
	}

	@Override
	public void setCenter(int[] pos) 
	{
		
		long pos_[] = new long[pos.length];
		
		for (int i = 0 ; i < pos.length ; i++)
		{
			pos_[i] = pos[i];
			offset[i] = pos[i];
		}
			
		rd = Views.offset(image, pos_).randomAccess();
	}

	@Override
	public String getStringParameters() 
	{
		String opt = new String();
				
		return "file="+filename;
	}
	
	@Override
	public boolean isFile() {
		// TODO Auto-generated method stub
		return true;
	}
	
	static public void SaveConfigFile(String sv, FilePSFSettings settings) throws IOException
	{
		FileOutputStream fout = new FileOutputStream(sv);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(settings);
		oos.close();
	}

	
	private boolean LoadConfigFile(String savedSettings)
	{
		System.out.println(savedSettings);
		
		try
		{
			FileInputStream fin = new FileInputStream(savedSettings);
			ObjectInputStream ois = new ObjectInputStream(fin);
			settings = (FilePSFSettings)ois.readObject();
			ois.close();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Settings File not found "+savedSettings);
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return true;
	}


	@Override
	public int[] getCenter() 
	{
		return offset;
	}


	@Override
	public boolean isSeparable() 
	{
		return false;
	}


	@Override
	public double[] getSeparableImageAsDoubleArray(int dim) 
	{
		return null;
	}


	@Override
	public float[] getSeparableImageAsFloatArray(int dim) 
	{
		return null;
	}


	@Override
	public float[][][] getImage3DAsFloatArray() 
	{
		if (Image3DF == null)
			Image3DF = GeneratePSF.generateImage3DAsFloatArray(this);
		return Image3DF;
	}

	@Override
	public double[][][] getImage3DAsDoubleArray() 
	{
		if (Image3DD == null)
			Image3DD = GeneratePSF.generateImage3DAsDoubleArray(this);
		return Image3DD;
	}

	@Override
	public double[][] getImage2DAsDoubleArray()
	{
		if (Image2DD == null)
			Image2DD = GeneratePSF.generateImage2DAsDoubleArray(this);
		return Image2DD;
	}

	@Override
	public float[][] getImage2DAsFloatArray() 
	{
		 Image2DF = GeneratePSF.generateImage2DAsFloatArray(this);
		 return Image2DF;
	}
}