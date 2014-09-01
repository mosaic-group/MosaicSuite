package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;

import mosaic.core.cluster.ClusterSession;
import mosaic.core.psf.GaussPSF;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Segmentation;




/**
 * @author Pietro Incardona
 * 
 * Class filter used as a callback for debugging
 * 
 */

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Dilate;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

public class Debug implements PlugInFilter {

	/**
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(String arg, ImagePlus imp) {
		// does not handle RGB, since the wrapped type is ARGBType (not a RealType)
		return NO_IMAGE_REQUIRED;
	}

	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) {
		run();
	}

	/**
	 * This should have been the method declared in PlugInFilter...
	 */
	public void run() {
		File fi[] = new File[1];
		File fo[] = new File[1];
		
		fi[0] = new File("/mnt/BigStorage/MOSAIC/Users/Shamba/Job764616/__intensities_c1.zip/tmp_1_intensities_c1.tif");
		fo[0] = new File("/mnt/BigStorage/MOSAIC/Users/Shamba/Job764616/_.tif/PGL3A-mEGFP_2_micromolar.tif_1.tif");
		
		ImgOpener io = new ImgOpener();
		
		for (int i = 0 ; i < fi.length ; i++)
		{
			int Ndilop_orig = 0;
	        final ImgFactory< FloatType > imgFactoryF = new ArrayImgFactory< FloatType >( );
	        final ImgFactory< BitType > imgFactoryB = new ArrayImgFactory< BitType >( );
			Img<FloatType> imgInt = null;
			Img<UnsignedShortType> imgInt_ = null;
			Img<UnsignedShortType> imgOrg = null;
			Img< FloatType > imgConv = null;
			Img< UnsignedShortType > imgCam = null;
			Img< BitType > imgDil = null;
			Img< BitType > imgDilop = null;
			
			long[] dims = null;
			
			try {
				ImagePlus iCam = new Opener().openImage( "/mnt/BigStorage/MOSAIC/Users/Shamba/Camera error.tif" );
				ImagePlus iCamF = MosaicUtils.getImageFrame(iCam, 1);
				imgCam = ImagePlusAdapter.wrap( iCamF );
				imgInt_ = (Img<UnsignedShortType>) io.openImgs(fi[i].getAbsolutePath()).get(0).getImg();
		        dims = new long[imgInt_.numDimensions()];
		        imgInt_.dimensions(dims);
				imgInt = imgFactoryF.create( dims, new FloatType() );
				imgDil = imgFactoryB.create(dims, new BitType());
				imgDilop = imgFactoryB.create(dims, new BitType());
				
				// Copy to Float apparently SCIFIO cannot convert or if can it is really bad documented
				
				Cursor<FloatType> curI = imgInt.cursor();
				Cursor<UnsignedShortType> curI_ = imgInt_.cursor();
				
				while (curI.hasNext())
				{
					curI.next();
					curI_.next();
					
					curI.get().set(curI_.get().getRealFloat());
				}
				imgConv = imgInt.copy();
				
				// Create an img to dilate
				
				Cursor<BitType> curDil = imgDil.cursor();
				Cursor<UnsignedShortType> curInt = imgInt_.cursor();
				
				while (curDil.hasNext())
				{
					curDil.next();
					curInt.next();
					if (curInt.get().getRealDouble() >= 1.0)
					{
						Ndilop_orig++;
						curDil.get().set(true);
					}
					else
						curDil.get().set(false);
				}
				
				// Dilate n-pixel
				
				OutOfBoundsFactory<BitType, RandomAccessibleInterval<BitType>> obf = new OutOfBoundsConstantValueFactory<BitType,RandomAccessibleInterval<BitType>>(new BitType(false));
				Dilate dil = new Dilate(ConnectedType.EIGHT_CONNECTED, obf, 1);
//				dil.compute(imgDil, imgDilop);
//				dil.compute(imgDilop, imgDil);
//				dil.compute(imgDil, imgDilop);
//				dil.compute(imgDilop, imgDil);
//				dil.compute(imgDil, imgDilop);
//				dil.compute(imgDilop, imgDil);
//				dil.compute(imgDil, imgDilop);
				
				imgOrg = (Img<UnsignedShortType>) io.openImgs(fo[i].getAbsolutePath()).get(0).getImg();
			} catch (ImgIOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
	        
	        // Convolve with gaussian
	        
	        GaussPSF<FloatType> gauss = new GaussPSF<FloatType>(3, FloatType.class);
	        FloatType st = new FloatType();
	        st.set(0);
	        FloatType[] var_ = new FloatType[3];
	        var_[0] = new FloatType(1.70f);
	        var_[1] = new FloatType(1.70f);
	        var_[2] = new FloatType(3.44f);
	        gauss.setVar(var_);
	        gauss.convolve(imgConv, st );
	        
	        // Image Back
	        
	        Img< FloatType > imgBin = imgFactoryF.create( dims, new FloatType() );
			
	        Cursor<UnsignedShortType> curO = imgOrg.cursor();
			Cursor<FloatType> cur = imgInt.cursor();
			Cursor<FloatType> curConv = imgConv.cursor();
			Cursor<FloatType> curI = imgBin.cursor();
			Cursor<UnsignedShortType> curCam = imgCam.cursor();
			Cursor<BitType> curDilop = imgDilop.cursor();
			
			int Ndilop = 0;
			int Nbck = 0;
			double IntegralBck = 0.0;
			double IntegralDroplet = 0.0;
			double IntegralCamera = 0.0;
			
			while(cur.hasNext())
			{
				cur.next();
				curO.next();
				curI.next();
				curConv.next();
				curCam.next();
				curDilop.next();
				
				// Case 1
				
/*				if (curDilop.get().getRealDouble() >= 1.0)
				{
					curI.get().set(0);
					Ndilop++;
				}
				else
				{
					float f = curO.get().getRealFloat();
					if (f <= 0)
						f = 0;
					curI.get().set(f);
					IntegralBck += f;
					IntegralCamera += curCam.get().getRealDouble();
					Nbck++;
				}*/
				
				// Case 2
				
				if (cur.get().getRealDouble() >= 1.0)
				{
					curI.get().set(0);
					Ndilop++;
				}
				else
				{
					float f = curO.get().getRealFloat() - curConv.get().getRealFloat();
					if (f <= 0)
						f = 0;
					curI.get().set(f);
					IntegralBck += f;
					IntegralCamera += curCam.get().getRealDouble();
					Nbck++;
				}
				
				
				IntegralDroplet += cur.get().getRealDouble();
			}
			
			// Now calculate the killed part
			
			ImageJFunctions.show(imgBin);
			
			double RealIntegral = IntegralBck - IntegralCamera;
			double Mean_Real = RealIntegral / Nbck;
			RealIntegral += (Ndilop - Ndilop_orig)*Mean_Real;
			
			IJ.showMessage("Ndil: "+ Ndilop + "    Ndilop_orig: " + Ndilop_orig + "        Real Mean: " + Mean_Real + "Integral droplet: " + IntegralDroplet + "    Integral Real background: " + RealIntegral);
		}
	}

	/**
	 * The actual processing is done here.
	 */
	public static<T extends RealType<T>> void add(Img<T> img, float value) {
		final Cursor<T> cursor = img.cursor();
		final T summand = cursor.get().createVariable();

		summand.setReal(value);

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.get().add(summand);
		}
	}
}


