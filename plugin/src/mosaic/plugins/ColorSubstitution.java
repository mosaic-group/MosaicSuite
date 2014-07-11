package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import net.imglib2.img.ImagePlusAdapter;

import java.io.File;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import mosaic.plugins.Generate_PSF.Point3D;



public class ColorSubstitution implements  PlugInFilter{
	

	<T extends RealType<T> & NativeType<T> > void substitute(ImagePlus imp, double col_from, double col_to)
	{
		// Color substitution
		
		final Img<T> image = ImagePlusAdapter.wrap( imp );
		
		Cursor<T> cur = image.cursor();
		
		while (cur.hasNext())
		{
			cur.next();
			
			if (cur.get().getRealFloat() == col_from)
			{
				cur.get().setReal(col_to);
			}
		}
	}
	
	@Override
	public int setup(String arg, ImagePlus imp)
	{
		GenericDialog gd = new GenericDialog("Color substitution");
		
		gd.addNumericField("Color From", 0, 3);
		gd.addNumericField("Color To", 0, 3);

		gd.showDialog();
		
		if (gd.wasCanceled() == true)
			return DONE;
		
		double col_from = gd.getNextNumber();
		double col_to = gd.getNextNumber();
		
		this.<UnsignedByteType>substitute(imp,col_from,col_to);
		
		return DONE;
	}
	
	
	@Override
	public void run(ImageProcessor ip) 
	{
		// TODO Auto-generated method stub
		
	}
}