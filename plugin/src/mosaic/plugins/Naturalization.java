package mosaic.plugins;

import java.util.Vector;

import net.imglib2.Cursor;
import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.iterable.unary.Mean;
import net.imglib2.type.NativeType;
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
import io.scif.img.ImgOpener;

/**
 * 
 * The Code is for RGB but it has to run on each
 * channel independently averaging all the channels
 * of T2_pr
 *
 */

class Naturalization implements PlugInFilter
{
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
	
	// Image factory for float images
	ImgFactory< FloatType > imgFactoryF;
	
	// Prior parameter for second order
	//
	// (Paramenters learned from trained dataset)
	// 
	// For different color R G B
	//
	// For one channel image use an average of them
	//
	
	float T2_pr[] = {0.2421f ,0.2550f,0.2474f};
	
	<T extends NumericType<T> & NativeType<T> & RealType<T>, S extends RealType<S>> void Naturalization(ImagePlus img, S Theta, Class<T> cls_t, Class<S> cls_s) throws InstantiationException, IllegalAccessException
	{
		imgFactoryF = new ArrayImgFactory< FloatType >( );
		final ImgFactory<IntType> imgFactoryI = new ArrayImgFactory< IntType >( );
		
		// Original image
		//
		// Check that the image dataset is 8 bit
		// Otherwise return an error or hint to scale down
		//
		Img<T> image_orig = ImagePlusAdapter.wrap( img );
		
		// Class to create to calculate the Mean
		Mean<T,S> m = new Mean<T,S>();
		
		// Check if the image is 8-bit
		T image_check = cls_t.newInstance();

		Object obj = image_check;
		if (!(obj instanceof UnsignedByteType))
		{
			IJ.error("Error it work only with 8-bit type");
			return;
		}
		
		// Calculate the parameters for any x-bit image
		
		int max_val = 0;
		
		if (obj instanceof UnsignedByteType)
		{
			max_val = 255;
		}
		else if (obj instanceof UnsignedShortType)
		{
			max_val = 65535;
		}
		
		N_Grad = 2*max_val;
		Grad_Offset = max_val;
		N_Lap = 8*max_val;
		Lap_Offset = 4*max_val;
		
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
		
		dims[0] = 2;
		dims[1] = N_Grad;
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
		    
		// Add images

		// Extended the original image
		// The borders are extended and the pixel at the borders are repeated
		
		ExtendedRandomAccessibleInterval<T, Img<T>> image = Views.extendBorder(image_orig);
		
		// Create Laplace field
		
		for (int i = 0 ; i < image_orig.numDimensions() ; i++)
		{dims[i] = image_orig.dimension(i);}
		
		Img<IntType> Field = imgFactoryI.create(dims,new IntType());
		

		// It calculate the Laplacian field = Field[], 
		// GradientCDF = Integral of the histogram of the
		//               of the Gradient field
		// LaplacianCDF = Integral of the Histogram of the 
		//                Laplacian field
		// 
		// i = channel
		
		div(image,Field,GradCDF,LapCDF);
		
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
        T_tmp = (float)FindT(GradCDF, N_Grad, Grad_Offset, EPS);
        //
        // for Y component
        //
        T_tmp += FindT(GradCDF, N_Grad, Grad_Offset, EPS);
        
        // Average them and divide by the prior parameter
        T1 = T_tmp/(2*T1_pr);
        
        // Average T2_pr
        
        float T2_pr_a = (float) ((T2_pr[0] + T2_pr[1] + T2_pr[2])/3.0);
        
   	 	// Find the best parameter and divide by the T2 prior
        T2 = (float)FindT(LapCDF, N_Lap, Lap_Offset, EPS)/T2_pr_a;
        
        float Nf = (float) ((1.0-Theta.getRealDouble())*T1 + Theta.getRealDouble()*T2_pr_a);
        
        // for each pixel naturalize
        
        Cursor<T> cur_orig = image_orig.cursor();
        Cursor<T> cur_ir = image_result.cursor();
        
        while (cur_orig.hasNext())
        {
        	// Next pixel
        	
        	cur_orig.next();
        	
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
	}
	
	@Override
	public int setup(String arg, ImagePlus imp) 
	{
		return DOES_ALL;
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
	double FindT(Img<FloatType> data, int N, int OffSet, float eps)
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
	<T extends RealType<T>> void div(ExtendedRandomAccessibleInterval<T, Img<T>> image, Img<IntType> field, Img<FloatType> GradCDF, Img<FloatType> LapCDF)
	{
		// It work only in 2D
		
		if (field.numDimensions() != 2)
		{
			IJ.error("Error it work only in 2D");
		}
		
		// Gradient on x pointer
		IntervalView<FloatType> Gradx = Views.hyperSlice(GradCDF, GradCDF.numDimensions() , 0);
		// Gradient on y pointer
		IntervalView<FloatType> Grady = Views.hyperSlice(GradCDF, GradCDF.numDimensions() , 1);

		// Create a 2D Gradient Field
		    
		long dims[] = new long[2];
		    
		Img<FloatType> GradD = imgFactoryF.create(dims, new FloatType());
		    
		// Laplacian pointer
		Img<FloatType> Lap = LapCDF;

		// Normalization 1/(Number of pixel of the original image)
		long n_pixel = 1;
		for (int i = 0 ; i < field.numDimensions() ; i++)
		{n_pixel *= field.dimension(i);}
		    
		// unit to sum
		double f = 1.0/(n_pixel);

		int tmp = 0;

		// Inside the image for Y
		    
		Cursor<IntType> cur = field.cursor();
		    
		// Cursor localization
		    
		int two_num_dim = 2 * 2;
		int[] indexD = new int[2];
		int[] loc = new int[field.numDimensions()];
		int[] loc_p = new int[field.numDimensions()];
		RandomAccess<IntType> Lap_f = field.randomAccess();
		RandomAccess<FloatType> Lap_hist = LapCDF.randomAccess();
		RandomAccess<FloatType> Grad_dist = GradD.randomAccess();
		
		// For each point of the Laplacian field
		
		while (cur.hasNext())
		{
			// Localize cursors
		    	
		    cur.localize(loc_p);
		    	
		    // get the stencil value;
		    	
		    Lap_f.setPosition(loc);
		    float L = -two_num_dim*Lap_f.get().getRealFloat();
		    	
		    // Laplacian
		    	
		    for (int i = 0 ; i < 2 ; i++)
		    {
		    	Lap_f.move(i, 1);
		    	float G_p = Lap_f.get().getRealFloat();
		    		
		    	Lap_f.move(i,-2);
		    	float G_m = Lap_f.get().getRealFloat();
		    	
		    	L += G_p + G_m;
		    		
		        // Calculate the gradient + convert into bin
		        indexD[i] = (int) (Grad_Offset + G_p - G_m);
		    }
		    
		    // Set the Laplacian field
		    Lap_f.get().setReal(L);
		    	
	        // Histogram bin conversion
	        tmp += Lap_Offset;
	            
	        Lap_hist.setPosition(0, (int)(tmp));
	        Lap_hist.get().setReal(Lap_hist.get().getRealFloat() + f);
	            
	        // increment the gradient bin
	        
	        Grad_dist.setPosition(indexD);
	        Grad_dist.get().setReal(Grad_dist.get().getRealFloat() + f);
		}
		
		
		//convert Grad2D into CDF
		Grad_dist = GradD.randomAccess();
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
		    	Grad_dist.move(0, 1);
		    	
		    	// integration up to the current position
		        Grad_dist.get().set(Grad_dist.get().getRealFloat() + prec);
		    }
		}
		
		//col integration
		for (int j = 1; j < N_Grad; ++j)
		{
			// Move to the actual position
			loc[0] = j-1;
			
		    for (int i = 0; i < N_Grad; ++i)
		    {
		    	loc[1] = i;
		    	
		    	Grad_dist.setPosition(loc);
		    	
		    	// Precedent float
		    	float prec = Grad_dist.get().getRealFloat();
		    	
		    	// Move to the actual position
		    	Grad_dist.setPosition(j, 1);
		    	
		    	Grad_dist.get().set(Grad_dist.get().getRealFloat() + prec);
		    }
		}
		    
		// pGrad2D has 2D CDF
		
		RandomAccess<FloatType> Gradx_r = Gradx.randomAccess();
		
		// For each row
		for (int i = 0; i < N_Grad; ++i)
		{
			loc[1] = i;
			
			Gradx_r.setPosition(0,i);
			
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
	    	Grady_r.setPosition(1,i);
	    	
	        for (int j = 0; j < N_Grad; ++j)
	        {
	        	loc[0] = j;
	        	
	        	Grad_dist.setPosition(loc);
	        	Grady_r.fwd(1);
	        	
	            Grady_r.get().set(Grady_r.get().getRealFloat() + Grad_dist.get().getRealFloat());
	        }
	    }
	    
		//scale, divide the number of integrated bins
		for (int i = 0; i < N_Grad; ++i)
		{
			Gradx_r.setPosition(0, i);
			Grady_r.setPosition(1, i);
			Gradx_r.get().set((float) (Gradx_r.get().getRealFloat() / 255.0));
			Grady_r.get().set((float) (Grady_r.get().getRealFloat() / 255.0));
		}

		//convert Lap to CDF
		for (int i = 1; i < N_Lap; ++i)
		{
			Lap_hist.setPosition(0, i-1);
			
			float prec = Lap_hist.get().getRealFloat();
			
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
}