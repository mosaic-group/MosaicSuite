package mosaic.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import mosaic.core.utils.MosaicUtils;
import net.imglib2.Cursor;
import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.ops.operation.iterable.unary.Mean;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.ImgSaver;

/**
 * 
 * The Code is for RGB but it has to run on each
 * channel independently averaging all the channels
 * of T2_pr
 *
 */

public class Naturalization implements PlugInFilterExt
{
	ImagePlus source;
	ImagePlus nat;
	
	// Parameters balance between first order and second order
	// 
	// Parameter of the algorithm
	//
    // (if you give more than one it perform the algorithm with all
	// the specified parameters )
	//
	// is an ADVANCED parameter
	//
	Vector<Float> Theta;
	
	
	// Precision in finding your best T 
	final float EPS = 0.0001f;
	
	//
	// Prior parameter for first oder
	//
	// In this case is for all channels
	//
	// Fixed parameter
	//
	
	final float T1_pr = 0.3754f;
	
	// Number of bins for the Laplacian Histogram
	//
	// In general is 4 * N_Grad
	//
	// max of laplacian value is 4 * 255
	//
	int N_Lap = 2041;
	
	// Offset shift in the histogram bins
	// Has to be N_Lap / 2;
	int Lap_Offset = 1020;

	// Number of bins for the Gradient
	int N_Grad = 512;
	// Offset for the gradient histogram shift
	int Grad_Offset = 256;
	
	// Image factory for float and int images
	ImgFactory< FloatType > imgFactoryF;
	final ImgFactory<IntType> imgFactoryI = new ArrayImgFactory< IntType >( );
	final ImgFactory<UnsignedByteType> imgFactoryUS = new ArrayImgFactory< UnsignedByteType >( );
	final ImgFactory<ARGBType> imgFactoryARGB = new ArrayImgFactory< ARGBType >( );
	
	// Prior parameter for second order
	//
	// (Paramenters learned from trained dataset)
	// 
	// For different color R G B
	//
	// For one channel image use an average of them
	//
	
	float T2_pr[] = {0.2421f ,0.2550f, 0.2474f,0.24816666f};
	
	float Nf_s[] = {0,0,0};
	
	/**
	 * 
	 * Naturalize the image
	 * 
	 * @param Img original image
	 * @param Theta parameter
	 * @param Class<T> Original image
	 * @param Class<S> Calculation Type
	 * @param channel_prior Channel prior to use
	 * 
	 */
	
	<T extends NumericType<T> & NativeType<T> & RealType<T>, S extends RealType<S>> Img<T> Naturalization(Img<T> image_orig, S Theta, int channel_prior, Class<T> cls_t, Class<S> cls_s) throws InstantiationException, IllegalAccessException
	{
		if (image_orig == null)
		{IJ.error("Naturalization plugin require an 8-bit image");return null;}
		
		imgFactoryF = new ArrayImgFactory< FloatType >( );
		
		// Original image
		//
		// Check that the image dataset is 8 bit
		// Otherwise return an error or hint to scale down
		//
		
		// Class to calculate the Mean
		Mean<T,S> m = new Mean<T,S>();
		
		// Check if the image is 8-bit
		T image_check = cls_t.newInstance();

		Object obj = image_check;
		if (!(obj instanceof UnsignedByteType))
		{
			IJ.error("Error it work only with 8-bit type");
			return null;
		}
		
		// Calculate the parameters for any x-bit image
		
		// Mean of the original image
		S mean_original = null;
		try {
			mean_original = cls_s.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Calculate mean intensity of the original image
		m.compute(image_orig.cursor(), mean_original);
		
		// We have only one channel
		Img<T> field_R = image_orig;
		
		long dims[] = {N_Lap};
		
		// Create one dimensional image or (Histogram)
		Img<FloatType> LapCDF = imgFactoryF.create(dims, new FloatType());
		
		dims = new long[2];
		dims[0] = N_Grad;
		dims[1] = 2;
		Img<FloatType> GradCDF = imgFactoryF.create(dims,new FloatType());
		
		// Get the dimensions of the result image
	
		image_orig.dimensions(dims);
		
		// The image result
		Img<T> image_result = null;
		try {
			image_result = image_orig.factory().create(dims, cls_t.newInstance());
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Create Laplace field
		
		for (int i = 0 ; i < image_orig.numDimensions() ; i++)
		{dims[i] = image_orig.dimension(i);}
		
		Img<FloatType> Field = imgFactoryF.create(dims,new FloatType());
		

		// It calculate the Laplacian field = Field[], 
		// GradientCDF = Integral of the histogram of the
		//               of the Gradient field
		// LaplacianCDF = Integral of the Histogram of the 
		//                Laplacian field
		// 
		// i = channel
		
		div(image_orig,Field,GradCDF,LapCDF);
		
		// Find the best T
		float T1;
		float T2;
		float T_tmp;
		
   	 	// For each channel find the best T
   	 	// 
   	 	// EPS=precision
   	 	//
   	 	// for X component
   	 	//
        T_tmp = (float)FindT(Views.iterable(Views.hyperSlice(GradCDF, GradCDF.numDimensions()-1 , 0)), N_Grad, Grad_Offset, EPS);
        //
        // for Y component
        //
        T_tmp += FindT(Views.iterable(Views.hyperSlice(GradCDF, GradCDF.numDimensions()-1 , 1)), N_Grad, Grad_Offset, EPS);
        
        // Average them and divide by the prior parameter
        T1 = T_tmp/(2*T1_pr);
        
        // Average T2_pr
        
        float T2_pr_a = (float) ((T2_pr[channel_prior]));
        
   	 	// Find the best parameter and divide by the T2 prior
        T2 = (float)FindT(LapCDF, N_Lap, Lap_Offset, EPS)/T2_pr_a;
        
        float Nf = (float) ((1.0-Theta.getRealDouble())*T1 + Theta.getRealDouble()*T2);
        
        if (channel_prior < 3)
        	Nf_s[channel_prior] = Nf;
        else
        	Nf_s[0] = Nf;
        
        // for each pixel naturalize
        
        Cursor<T> cur_orig = image_orig.cursor();
        Cursor<T> cur_ir = image_result.cursor();
        
        while (cur_orig.hasNext())
        {
        	// Next pixel
        	
        	cur_orig.next();
        	cur_ir.next();
        	
        	// Naturalize
        	
        	float tmp = cur_orig.get().getRealFloat();
        	
        	// Naturalize
        	
        	float Nat = (float) ((tmp - mean_original.getRealFloat())*Nf + mean_original.getRealFloat() + 0.5);
        	if (Nat < 0)
        	{Nat = 0;}
        	else if (Nat > 255)
        	{Nat = 255;}
        	else
        	{Nat = (int)Nat;}
        	
        	cur_ir.get().setReal(Nat);
        }
        
        return image_result;
	}
	
	@Override
	public int setup(String arg, ImagePlus imp)
	{
		if (imp == null)
		{IJ.error("The plugin require an 8-bit image");return DONE;}
		
		source = imp;
		
		// Analyse the image
		
		if (imp.getType() == ImagePlus.COLOR_RGB)
		{
			// It work only on 2D images
			
			long dims[] = new long[2];
			dims[0] = imp.getWidth();
			dims[1] = imp.getHeight();
			
			// Copy the R Channel and Naturalize
			
			Img<UnsignedByteType> R_Channel = imgFactoryUS.create(dims, new UnsignedByteType());
			
			// Copy read channel
			
			Img<ARGBType> image_orig = ImagePlusAdapter.wrap( imp );
			
			Cursor<ARGBType> aCur = image_orig.cursor();
			Cursor<UnsignedByteType> r = R_Channel.cursor();
			
			// Copy
			
			while (r.hasNext())
			{
				r.next();
				aCur.next();
				
				r.get().set(ARGBType.red(aCur.get().get()));
			}
			
			FloatType Theta = new FloatType(0.5f);
			try {
				R_Channel = Naturalization(R_Channel,Theta,2,UnsignedByteType.class, FloatType.class);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Copy gren channel and naturalize
			
			Img<UnsignedByteType> G_Channel = imgFactoryUS.create(dims, new UnsignedByteType());
			
			// Copy green channel
			
			aCur = image_orig.cursor();
			Cursor<UnsignedByteType> g = G_Channel.cursor();
			
			// Copy
			
			while (g.hasNext())
			{
				g.next();
				aCur.next();
				
				g.get().set(ARGBType.green(aCur.get().get()));
			}
			
			Theta = new FloatType(0.5f);
			try {
				G_Channel = Naturalization(G_Channel,Theta,1,UnsignedByteType.class, FloatType.class);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Copy blue channel
			
			Img<UnsignedByteType> B_Channel = imgFactoryUS.create(dims, new UnsignedByteType());
			
			aCur = image_orig.cursor();
			Cursor<UnsignedByteType> b = B_Channel.cursor();
			
			// Copy
			
			while (b.hasNext())
			{
				b.next();
				aCur.next();
				
				b.get().set(ARGBType.blue(aCur.get().get()));
			}
			
			Theta = new FloatType(0.5f);
			try {
				B_Channel = Naturalization(B_Channel,Theta,0,UnsignedByteType.class, FloatType.class);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/////////////////////////////////
			
			// Merge the Naturalized channel into an RGB image
			
			Img<ARGBType> img_result = imgFactoryARGB.create(dims, new ARGBType());
			
			Cursor<ARGBType> img_result_cur = img_result.cursor();
			Cursor<UnsignedByteType> rc = R_Channel.cursor();
			Cursor<UnsignedByteType> gc = G_Channel.cursor();
			Cursor<UnsignedByteType> bc = B_Channel.cursor();
			
			while (img_result_cur.hasNext())
			{
				img_result_cur.next();
				rc.next();
				gc.next();
				bc.next();
				
				img_result_cur.get().set(ARGBType.rgba(rc.get().getRealFloat(), gc.get().getRealFloat(), bc.get().getRealFloat(), 255.0f));
			}
			
			/////////////////////////////////
			
			nat = ImageJFunctions.wrap(img_result,imp.getTitle() + "_naturalized");
			nat.show();
			
			IJ.saveAsTiff(nat, MosaicUtils.ValidFolderFromImage(imp)+ File.separator + MosaicUtils.removeExtension(imp.getTitle()) + "_nat.tif");
			
			IJ.showMessage("Naturalness factor R: " + Nf_s[2] + "   G: " + Nf_s[1] + "   B: " + Nf_s[0]);
		}
		else if (imp.getType() == ImagePlus.GRAY8)
		{
			// It work only on 2D images
			
			long dims[] = new long[2];
			dims[0] = imp.getWidth();
			dims[1] = imp.getHeight();
			
			// Copy the R Channel and Naturalize
			
			Img<UnsignedByteType> Channel = imgFactoryUS.create(dims, new UnsignedByteType());
			
			// Copy the channel
			
			Img<UnsignedByteType> image_orig = ImagePlusAdapter.wrap( imp );
			
			Cursor<UnsignedByteType> aCur = image_orig.cursor();
			Cursor<UnsignedByteType> r = Channel.cursor();
			
			// Copy
			
			while (r.hasNext())
			{
				r.next();
				aCur.next();
				
				r.get().set(aCur.get().get());
			}
			
			FloatType Theta = new FloatType(0.5f);
			try {
				Channel = Naturalization(Channel,Theta,3,UnsignedByteType.class, FloatType.class);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/////////////////////////////////
			
			nat = ImageJFunctions.wrap(Channel, imp.getTitle() + "_naturalized");
			nat.show();
			
			IJ.saveAsTiff(nat, MosaicUtils.ValidFolderFromImage(imp)+ File.separator + MosaicUtils.removeExtension(imp.getTitle()) + "_nat.tif");
			
			IJ.showMessage("Naturalization factor " + Nf_s[0]);
		}
		else
		{
			IJ.error("Naturalization require 8-bit images or RGB");
		}
		
/*		try {
			FloatType Theta = new FloatType(0.5f);
//			Naturalization(imp,Theta,4,IntType.class,FloatType.class);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		return DONE;
	}

	
	// 
	// Original data
	// N = nuber of bins
	// offset of the histogram
	// T current
	//
	double FindT_Evalue(float[] data, int N, int offset, float T)
	{
	    double error = 0;

	    double tmp;
	    float[] p_d = data;

	    for (int i=-offset; i<N-offset; ++i) {
	        tmp = Math.atan(T*(i)) - p_d[i+offset];
	        error += (tmp*tmp);
	    }

	    return error;
	}

	// Find the T
	//
	// CDF Histogram
	// N number of bins
	// Offset of the histogram
	// eps precision
	//
	double FindT(IterableInterval<FloatType> data, int N, int OffSet, float eps)
	{
	    //find the best parameter between data and model atan(Tx)/pi+0.5
			
		// Search between 0 and 1.0
	    float left = 0;
	    float right = 1.0f;

	    float m1 = 0.0f;
	    float m2 = 0.0f;

		// Crate p_t to save computation (shift and rescale the original CDF)
		float p_t[] = new float[N];
		
		// Copy the data
		Cursor<FloatType> cur_data = data.cursor();
		for (int i = 0; i < N; ++i)
		{
			cur_data.next();
			p_t[i] = (float) ((cur_data.get().getRealFloat() - 0.5)*Math.PI);
		}
		    
		// While the precision is bigger than eps
		while(right-left>=eps)
		{
		    // move left and right of 1/3 (m1 and m2)
		    m1=left+(right-left)/3;
		    m2=right-(right-left)/3;
		        
		    // Evaluate on m1 and m2, ane move the extreme point
		    if(FindT_Evalue(p_t, N, OffSet, m1) <=FindT_Evalue(p_t, N, OffSet, m2))
		    	right=m2;
		    else
		    	left=m1;
		}
		
		// return the average
		return (m1+m2)/2;
	}
	
	//process signal channel uint8 image
	<T extends RealType<T>> void div(Img<T> image, Img<FloatType> field, Img<FloatType> GradCDF, Img<FloatType> LapCDF)
	{
		// It work only in 2D
		
		if (field.numDimensions() != 2)
		{
			IJ.error("Error it work only in 2D");
		}
		
		// Gradient on x pointer
		IntervalView<FloatType> Gradx = Views.hyperSlice(GradCDF, GradCDF.numDimensions()-1 , 0);
		// Gradient on y pointer
		IntervalView<FloatType> Grady = Views.hyperSlice(GradCDF, GradCDF.numDimensions()-1 , 1);

		// Create a 2D Gradient Field
		    
		long dims[] = new long[2];
		dims[0] = N_Grad;
		dims[1] = N_Grad;
		
		Img<FloatType> GradD = imgFactoryF.create(dims, new FloatType());
		    
		// Laplacian pointer
		Img<FloatType> Lap = LapCDF;

		// Normalization 1/(Number of pixel of the original image)
		long n_pixel = 1;
		for (int i = 0 ; i < field.numDimensions() ; i++)
		{n_pixel *= field.dimension(i)-2;}
		    
		// unit to sum
		double f = 1.0/(n_pixel);

		// Inside the image for Y
		    
		Cursor<FloatType> cur = field.cursor();
		  
		int counter = 0;
		
		// Cursor localization
		    
		int two_num_dim = 2 * 2;
		int[] indexD = new int[2];
		int[] loc_p = new int[field.numDimensions()];
		RandomAccess<T> img_cur = image.randomAccess();
		RandomAccess<FloatType> Lap_f = field.randomAccess();
		RandomAccess<FloatType> Lap_hist = LapCDF.randomAccess();
		RandomAccess<FloatType> Grad_dist = GradD.randomAccess();
		
		// For each point of the Laplacian field
		
		while (cur.hasNext())
		{
			cur.next();
			
			// Localize cursors
		    	
		    cur.localize(loc_p);
		    
		    // Exclude the border
		    
		    boolean border = false;
		    
		    for (int i = 0 ; i < image.numDimensions() ; i++)
		    {
		    	if (loc_p[i] == 0)
		    	{border = true;}
		    	else if (loc_p[i] == image.dimension(i)-1)
		    	{border = true;}
		    }
		    
		    if (border == true)
		    	continue;
		    
		    // get the stencil value;
		    	
		    img_cur.setPosition(loc_p);
		    
		    float L = -4*img_cur.get().getRealFloat();
		    	
		    // Laplacian
		    	
		    for (int i = 0 ; i < 2 ; i++)
		    {
		    	img_cur.move(1, i);
		    	float G_p = img_cur.get().getRealFloat();
		    		
		    	img_cur.move(-1,i);
		    	float G_m = img_cur.get().getRealFloat();
		    	
		    	img_cur.move(-1, i);
		    	float L_m = img_cur.get().getRealFloat();
		    
		    	img_cur.setPosition(loc_p);
		    	
		    	L += G_p + L_m;
		    		
		        // Calculate the gradient + convert into bin
		        indexD[1-i] = (int) (Grad_Offset + G_p - G_m);
		    }
		    
		    Lap_f.setPosition(loc_p);
		    // Set the Laplacian field
		    Lap_f.get().setReal(L);
		    	
	        // Histogram bin conversion
	        L += Lap_Offset;
	            
	        Lap_hist.setPosition((int)(L),0);
	        Lap_hist.get().setReal(Lap_hist.get().getRealFloat() + f);
	        
	        Grad_dist.setPosition(indexD);
	        Grad_dist.get().setReal(Grad_dist.get().getRealFloat() + f);
		}
		
		int[] loc = new int[field.numDimensions()];		
		
		//convert Grad2D into CDF
		Grad_dist = GradD.randomAccess();
		
		// DEBUG show gradient dist
//		ImageJFunctions.show(GradD);
		
		// for each row
		for (int j = 0; j < GradD.dimension(1) ; j++)
		{
			loc[1] = j;
		    for (int i = 1; i < GradD.dimension(0) ; ++i)
		    {
		    	
		    	loc[0] = i-1;
		    	Grad_dist.setPosition(loc);
		    	
		    	// Precedent float
		    	float prec = Grad_dist.get().getRealFloat();
		    	
		    	// Move to the actual position
		    	Grad_dist.move(1, 0);
		    	
		    	// integration up to the current position
		        Grad_dist.get().set(Grad_dist.get().getRealFloat() + prec);
		    }
		}
		
		//col integration
		for (int j = 1; j < N_Grad; ++j)
		{
			// Move to the actual position
			loc[1] = j-1;
			
		    for (int i = 0; i < N_Grad; ++i)
		    {
		    	loc[0] = i;
		    	
		    	Grad_dist.setPosition(loc);
		    	
		    	// Precedent float
		    	float prec = Grad_dist.get().getRealFloat();
		    	
		    	// Move to the actual position
		    	Grad_dist.move(1, 1);
		    	
		    	Grad_dist.get().set(Grad_dist.get().getRealFloat() + prec);
		    }
		}
		
		// pGrad2D has 2D CDF
		
		RandomAccess<FloatType> Gradx_r = Gradx.randomAccess();
		
		// Integrate over the row
		for (int i = 0; i < N_Grad; ++i)
		{
			loc[1] = i;
			
			Gradx_r.setPosition(i,0);
			
			// get the row
		    for (int j = 0; j < N_Grad; ++j)
		    {
		    	loc[0] = j;
		    	
		    	// Set the position
		    	Grad_dist.setPosition(loc);
		    	
		    	// integrate over the row to get 1D vector
		        Gradx_r.get().set(Gradx_r.get().getRealFloat() + Grad_dist.get().getRealFloat());
		    }
		}
		
		RandomAccess<FloatType> Grady_r = Grady.randomAccess();
		
		// Integrate over the column
	    for (int i = 0; i < N_Grad; ++i)
	    {
	    	loc[1] = i;
	    	
	    	Grady_r.setPosition(0,0);
	    	
	        for (int j = 0; j < N_Grad; ++j)
	        {
	        	loc[0] = j;
	        	
	        	Grad_dist.setPosition(loc);
	        	
	            Grady_r.get().set(Grady_r.get().getRealFloat() + Grad_dist.get().getRealFloat());
	        	Grady_r.move(1,0);
	        }
	    }
	    
		//scale, divide the number of integrated bins
		for (int i = 0; i < N_Grad; ++i)
		{
			Gradx_r.setPosition(i,0);
			Grady_r.setPosition(i,0);
			Gradx_r.get().set((float) (Gradx_r.get().getRealFloat() / 255.0));
			Grady_r.get().set((float) (Grady_r.get().getRealFloat() / 255.0));
		}

		//convert Lap to CDF
		for (int i = 1; i < N_Lap; ++i)
		{
			Lap_hist.setPosition(i-1,0);
			
			float prec = Lap_hist.get().getRealFloat();
			
			Lap_hist.move(1,0);
			
			Lap_hist.get().set(Lap_hist.get().getRealFloat() + prec);
		}
	}
	
	
	
	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) 
	{
	}

	@Override
	public void closeAll() 
	{
		// Close source
		if (source != null)
			source.close();
		
		// Close naturalization
		if (nat != null)
			nat.close();
	}
}