package mosaic.plugins;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import mosaic.core.psf.GaussPSF;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Dilate;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
/**
 * @author Pietro Incardona
 * 
 * Class filter used as a callback for debugging
 * 
 */

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
	public void run() 
	{
		ImgOpener io = new ImgOpener();
/*		ImgSaver sv = new ImgSaver();
		
		// Correct the images for not even illumination
		
		File fi_correction;
		
		fi_correction = new File("/mnt/BigStorage/MOSAIC/Users/Shamba/Uneven illumination correction.tif");
		
		Img<UnsignedShortType> imgCor = null;
		try {
			imgCor = (Img<UnsignedShortType>) io.openImgs(fi_correction.getAbsolutePath()).get(0).getImg();
		} catch (ImgIOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		File f_i_to_cor[] = new File[4];
		
		f_i_to_cor[0] = new File("/mnt/BigStorage/MOSAIC/Users/Shamba/PGL3A-mEGFP_2_micromolar.tif");
		f_i_to_cor[1] = new File("/mnt/BigStorage/MOSAIC/Users/Shamba/PGL3A-mEGFP_5_micromolar.tif");
		f_i_to_cor[2] = new File("/mnt/BigStorage/MOSAIC/Users/Shamba/PGL3A-mEGFP_10_micromolar.tif");
		f_i_to_cor[0] = new File("/mnt/BigStorage/MOSAIC/Users/Shamba/PGL3A-mEGFP_19_micromolar.tif");
		
		for (int i = 0 ; i < f_i_to_cor.length ; i++)
		{
			double max[] = new double[20];
			
			// get the max of the correction image
			
			for (int frame = 0 ; frame < 20 ; frame++)
			{
				IntervalView<UnsignedShortType> inte = Views.hyperSlice(imgCor, imgCor.numDimensions()-1, frame);
				IterableInterval<UnsignedShortType> inte_c = Views.iterable(inte);
				
				Cursor<UnsignedShortType> cur = inte_c.cursor();
			
				while (cur.hasNext())
				{
					cur.next();
					int val = cur.get().getInteger();
					
					if (val >= max[frame])
					{
						max[frame] = val;
					}
				}
			}
			
			Img<UnsignedShortType> dataset = null;
			try {
				dataset = (Img<UnsignedShortType>) io.openImgs(f_i_to_cor[i].getAbsolutePath()).get(0).getImg();
			} catch (ImgIOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// rescale the image
			
			for (int frame = 0 ; frame < 20 ; frame++)
			{
				IntervalView<UnsignedShortType> inte = Views.hyperSlice(dataset, dataset.numDimensions()-1, frame);
				IterableInterval<UnsignedShortType> inte_c = Views.iterable(inte);
				
				Cursor<UnsignedShortType> cur = inte_c.cursor();
			
				// 
				
				IntervalView<UnsignedShortType> inte_cor = Views.hyperSlice(imgCor, imgCor.numDimensions()-1, frame);
				IterableInterval<UnsignedShortType> inte_cor_c = Views.iterable(inte_cor);
				
				Cursor<UnsignedShortType> cur_cor = inte_cor_c.cursor();
				
				while (cur.hasNext())
				{
					cur.next();
					cur_cor.next();
					
					int val = cur_cor.get().getInteger();
					
					float cor_fact = (float) ((float)val/max[frame]);
					
					cur.get().set((int)(cor_fact * cur.get().get()));
				}
			}
		
			// save the image
			
			ImagePlus imp = ImageJFunctions.wrap(dataset, f_i_to_cor[i].getName() + "_cor.tiff");
//			IJ.saveAsTiff(imp,f_i_to_cor[i].getAbsolutePath() + "_cor.tiff");
			imp.show();
		}*/
		
		//
		
		//
		
		PrintWriter out = null;
		try {
			out = new PrintWriter("/mnt/BigStorage/MOSAIC/Users/Shamba/out.csv");
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		// For all dataset compute
		
		for (int k = 1 ; k <= 4 ; k++)
		{			
			String ds = null;
			
			switch (k)
			{
			case 1:
				ds = new String("2");
				break;
			case 2:
				ds = new String("5");
				break;
			case 3:
				ds = new String("10");
				break;
			case 4:
				ds = new String("19");
				break;
			}
		
			out.println("**********************" + ds + "********************************");
			
			File fi[] = new File[20];
			File fo[] = new File[20];
		
			for (int i = 0 ; i < 20 ; i ++)
			{				
				fi[i] = new File("/mnt/BigStorage/MOSAIC/Users/Shamba/" + ds + "/__intensities_c1.zip/tmp_" + (i+1) + "_intensities_c1.tif");
				fo[i] = new File("/mnt/BigStorage/MOSAIC/Users/Shamba/" + ds + "/_.tif/PGL3A-mEGFP_" + ds + "_micromolar.tif_cor-1_" + (i+1) + ".tif");
			}

			for (int i = 0 ; i < fi.length ; i++)
			{
				int Ndilop_orig = 0;
				final ImgFactory< FloatType > imgFactoryF = new ArrayImgFactory< FloatType >( );
				final ImgFactory< BitType > imgFactoryB = new ArrayImgFactory< BitType >( );
				Img<FloatType> imgInt = null;
				Img<UnsignedShortType> imgInt_ = null;
				Img<UnsignedShortType> imgOrg = null;
				Img< FloatType > imgConv = null;
				Img< BitType > imgDil = null;
				Img< BitType > imgDilop = null;
			
				long[] dims = null;
			
				try {
//					ImagePlus iCam = new Opener().openImage( "/mnt/BigStorage/MOSAIC/Users/Shamba/Camera error.tif" );
//					ImagePlus iCamF = MosaicUtils.getImageFrame(iCam, 1);
//					imgCam = ImagePlusAdapter.wrap( iCamF );
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
					dil.compute(imgDil, imgDilop);
//					dil.compute(imgDilop, imgDil);
//					dil.compute(imgDil, imgDilop);
//					dil.compute(imgDilop, imgDil);
//					dil.compute(imgDil, imgDilop);
//					dil.compute(imgDilop, imgDil);
//					dil.compute(imgDil, imgDilop);
				
					imgOrg = (Img<UnsignedShortType>) io.openImgs(fo[i].getAbsolutePath()).get(0).getImg();
				} catch (ImgIOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
	        
				// find the best Z
				
				int BigZ = 0;
				int loc[] = new int[3];
				Cursor<FloatType> cur = imgInt.cursor();
				Cursor<BitType> curDilop = imgDilop.cursor();
				
				while(curDilop.hasNext())
				{
					curDilop.next();
				
					// Case 1
				
					if (curDilop.get().getRealDouble() >= 1.0)
					{
						curDilop.localize(loc);
					}
					
					if (loc[2] > BigZ)
					{
						BigZ = loc[2];
					}
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
	        	cur = imgInt.cursor();
	        	Cursor<FloatType> curConv = imgConv.cursor();
	        	Cursor<FloatType> curI = imgBin.cursor();
				curDilop = imgDilop.cursor();
			
				int Ndilop = 0;
				int Nbck = 0;
				double IntegralBck = 0.0;
				double IntegralDroplet = 0.0;
			
				while(cur.hasNext())
				{
					cur.next();
					curO.next();
					curI.next();
					curConv.next();
					curDilop.next();
				
					cur.localize(loc);
					if (loc[2] >= BigZ)
						continue;
					
					// Case 1
				
					if (curDilop.get().getRealDouble() >= 1.0)
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
						Nbck++;
					}
				
					// Case 2
				
/*					if (cur.get().getRealDouble() >= 1.0)
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
						Nbck++;
					}*/
				
				
					IntegralDroplet += cur.get().getRealDouble();
				}
			
				// Now calculate the killed part
			
				ImageJFunctions.show(imgBin);
			
				double RealIntegral = IntegralBck;
				double Mean_Real = RealIntegral / Nbck;
				RealIntegral += (Ndilop - Ndilop_orig)*Mean_Real;
			
//				IJ.showMessage("Ndil: "+ Ndilop + "    Ndilop_orig: " + Ndilop_orig + "        Real Mean: " + Mean_Real + "Integral droplet: " + IntegralDroplet + "    Integral Real background: " + RealIntegral);
									
				out.println("Nbck: " + Nbck + " Ndil: "+ Ndilop + "    Ndilop_orig: " + Ndilop_orig + "        Real Mean: " + Mean_Real + "     Integral droplet: " + IntegralDroplet + "    Integral Real background: " + RealIntegral);
				out.flush();
			}
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


