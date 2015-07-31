package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * The Code is for RGB but it has to run on each
 * channel independently averaging all the channels
 * of T2_pr
 */
public class Naturalization implements PlugInFilter
{
    // Precision in finding your best T 
    private static final float EPS = 0.0001f;
    
    // Prior parameter for first oder
    // In this case is for all channels
    // Fixed parameter
    private static final float T1_pr = 0.3754f;
    
    // Number of bins for the Laplacian Histogram
    // In general is 4 * N_Grad
    // max of laplacian value is 4 * 255
    private static final int N_Lap = 2041;
    
    // Offset shift in the histogram bins
    // Has to be N_Lap / 2;
    private static final int Lap_Offset = 1020;

    // Number of bins for the Gradient
    private static final int N_Grad = 512;
    
    // Offset for the gradient histogram shift
    private static final int Grad_Offset = 256;
    
    // Prior parameter for second order
    // (Parameters learned from trained data set)
    // For different color R G B
    // For one channel image use an average of them
    private final float T2_pr[] = {0.2421f ,0.2550f, 0.2474f, 0.24816666f};
    private final float Nf_s[] = {0,0,0};

    @Override
    public int setup(String arg, ImagePlus imp)
    {
        if (imp == null || !(imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.GRAY8)) {
            IJ.error("Naturalization require 8-bit images or RGB");
            return DONE;
        }

        ImageStack is = imp.getStack();
        
        long rdims[] = new long[3];
        rdims[0] = imp.getWidth();
        rdims[1] = imp.getHeight();
        rdims[2] = is.getSize();
        
        ResultsTable rs = new ResultsTable();
        ImagePlus nat;
        
        if (imp.getType() == ImagePlus.COLOR_RGB) {
            nat = naturalizeColorRgb(imp, is, rdims, rs);
        }
        else if (imp.getType() == ImagePlus.GRAY8) {
            nat = naturalizeGray8(imp, is, rdims, rs);
        }
        else {
            return DONE; // Should not happen
        }
        
        nat.show();
        
        if (!Interpreter.isBatchMode()) {
            rs.show("Naturalization and PSNR");
            showMessage();
        }
        
        return DONE;
    }

    /**
     * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
     */
    @Override
    public void run(ImageProcessor ip) 
    {
    }
    
    private ImagePlus naturalizeColorRgb(ImagePlus imp, ImageStack is, long[] rdims, ResultsTable rs) {
        // It work only on 2D images
        long dims[] = new long[2];
        dims[0] = imp.getWidth();
        dims[1] = imp.getHeight();
        Img<ARGBType> img_result = new ArrayImgFactory<ARGBType>().create(rdims, new ARGBType());

        for (int i = 0 ; i < is.getSize() ; i++) {
            ImagePlus RGBb = new ImagePlus("RGB_tmp", is.getProcessor(i+1));
            Img<ARGBType> image_orig = ImagePlusAdapter.wrap( RGBb );
            
            // Red channel
            Img<UnsignedByteType> R_Channel = naturalizeOneChannel(dims, image_orig, 2);
            // Green channel
            Img<UnsignedByteType> G_Channel = naturalizeOneChannel(dims, image_orig, 1);
            // Blue channel
            Img<UnsignedByteType> B_Channel = naturalizeOneChannel(dims, image_orig, 0);
             
            // Merge the Naturalized channel into an RGB image      
            IntervalView<ARGBType> view = Views.hyperSlice(img_result, 2, i);
            RandomAccess<ARGBType> result_ra = view.randomAccess();
        
            int loc[] = new int[2];
            Cursor<UnsignedByteType> rc = R_Channel.cursor();
            Cursor<UnsignedByteType> gc = G_Channel.cursor();
            Cursor<UnsignedByteType> bc = B_Channel.cursor();
        
            while (rc.hasNext()) {
                rc.next();
                gc.next();
                bc.next();
            
                rc.localize(loc);
                result_ra.setPosition(loc);
                result_ra.get().set(ARGBType.rgba(rc.get().getRealFloat(), gc.get().getRealFloat(), bc.get().getRealFloat(), 255.0f));
            }
            
            rs.incrementCounter();
            rs.addValue("Naturalization R", Nf_s[2]);
            rs.addValue("Estimated R PSNR", calculate_PSNR(Nf_s[2]));
            rs.addValue("Naturalization G", Nf_s[1]);
            rs.addValue("Estimated G PSNR", calculate_PSNR(Nf_s[1]));
            rs.addValue("Naturalization B", Nf_s[0]);
            rs.addValue("Estimated B PSNR", calculate_PSNR(Nf_s[0]));
            
            IJ.showStatus("Processing image " + (i+1));
            IJ.showProgress((((double)i+1)/is.getSize()));
        }
        
        return ImageJFunctions.wrap(img_result,imp.getTitle() + "_naturalized");
    }

    private ImagePlus naturalizeGray8(ImagePlus imp, ImageStack is, long[] rdims, ResultsTable rs) {
        Img<UnsignedByteType> Channel = new ArrayImgFactory<UnsignedByteType>().create(rdims, new UnsignedByteType());
        
        for (int i = 0 ; i < is.getSize() ; i++) {
            ImagePlus Cb = new ImagePlus("Gray8_tmp", is.getProcessor(i+1));
            Img<UnsignedByteType> TChannel = ImagePlusAdapter.wrap( Cb );
            
            TChannel = performNaturalization(3, TChannel);
            
            // Merge the result
            IntervalView<UnsignedByteType> view = Views.hyperSlice(Channel, 2, i);
            RandomAccess<UnsignedByteType> result_ra = view.randomAccess();
        
            int loc[] = new int[2];
            Cursor<UnsignedByteType> rc = TChannel.cursor();
        
            while (rc.hasNext()) {
                rc.next();
                rc.localize(loc);
                result_ra.setPosition(loc);
                result_ra.get().set(rc.get());
            }
            
            rs.incrementCounter();
            rs.addValue("Naturalization ", Nf_s[0]);
            rs.addValue("Estimated PSNR", calculate_PSNR(Nf_s[0]));
            
            IJ.showStatus("Processing image " + (i+1));
            IJ.showProgress((((double)i+1)/is.getSize()));
        }
        
        return ImageJFunctions.wrap(Channel, imp.getTitle() + "_naturalized");
    }
    
    /**
     * Naturalize the image
     * 
     * @param Img original image
     * @param Theta parameter
     * @param Class<T> Original image
     * @param Class<S> Calculation Type
     * @param channel_prior Channel prior to use
     */
    private <T extends NumericType<T> & NativeType<T> & RealType<T>, S extends RealType<S>> Img<T> doNaturalization(Img<T> image_orig, S Theta, int channel_prior, Class<T> cls_t, Class<S> cls_s) throws InstantiationException, IllegalAccessException
    {
        if (image_orig == null)
        {return null;}
        
        // Check that the image data set is 8 bit
        // Otherwise return an error or hint to scale down
        T image_check = cls_t.newInstance();
        Object obj = image_check;
        if (!(obj instanceof UnsignedByteType)) {
            IJ.error("Error it work only with 8-bit type");
            return null;
        }
        
        float Nf = findNaturalizationFactor(image_orig, Theta, T2_pr[channel_prior]);
        
        if (channel_prior < 3)
            Nf_s[channel_prior] = Nf;
        else
            Nf_s[0] = Nf;
        
        Img<T> image_result = naturalizeImage(image_orig, Nf, cls_t, cls_s);
        
        return image_result;
    }

    private <S extends RealType<S>, T extends NumericType<T> & NativeType<T> & RealType<T>> 
            Img<T> naturalizeImage(Img<T> image_orig, float Nf, Class<T> cls_t, Class<S> cls_s)
            throws InstantiationException, IllegalAccessException
    {
        // Mean of the original image
        //        S mean_original = cls_s.newInstance();
        //        Mean<T,S> m = new Mean<T,S>();
        //        m.compute(image_orig.cursor(), mean_original);
        // TODO: quick fix for deprecated code above. Is new 'mean' utility introduced in imglib2?
        float mean_original = 0.0f;

        Cursor<T> c2 = image_orig.cursor();
        float count = 0.0f;
        while (c2.hasNext()) {
                c2.next();
                mean_original += c2.get().getRealFloat();
                count += 1.0f;
        }
        mean_original /= count;
        
        // Create result image
        long[] origImgDimensions = new long[2];
        image_orig.dimensions(origImgDimensions);
        Img<T> image_result = image_orig.factory().create(origImgDimensions, cls_t.newInstance());
        
        // for each pixel naturalize
        Cursor<T> cur_orig = image_orig.cursor();
        Cursor<T> cur_ir = image_result.cursor();
        
        while (cur_orig.hasNext()) {
            cur_orig.next();
            cur_ir.next();
            
            float tmp = cur_orig.get().getRealFloat();
            
            // Naturalize
            float Nat = (int) ((tmp - mean_original)*Nf + mean_original + 0.5);
            if (Nat < 0)
            {Nat = 0;}
            else if (Nat > 255)
            {Nat = 255;}
            
            cur_ir.get().setReal(Nat);
        }
        return image_result;
    }

    private <S extends RealType<S>, T extends NumericType<T> & NativeType<T> & RealType<T>> float findNaturalizationFactor(Img<T> image_orig, S Theta, float T2prior) {
        ImgFactory<FloatType> imgFactoryF = new ArrayImgFactory<FloatType>();
        
        // Create one dimensional image (Histogram)
        Img<FloatType> LapCDF = imgFactoryF.create(new long[] {N_Lap}, new FloatType());
        
        // Two dimensional image for Gradient
        Img<FloatType> GradCDF = imgFactoryF.create(new long[] {N_Grad, 2}, new FloatType());
        
        // GradientCDF = Integral of the histogram of the of the Gradient field
        // LaplacianCDF = Integral of the Histogram of the Laplacian field
        Img<FloatType> GradD = create2DGradientField();
        calculateLaplaceFieldAndGradient(image_orig, LapCDF, GradD);
        convertGrad2dToCDF(GradD);
        calculateGradCDF(GradCDF, GradD);
        calculateLapCDF(LapCDF);
        
        // For each channel find the best T1
        // EPS=precision
        // for X component
        float T_tmp = (float)FindT(Views.iterable(Views.hyperSlice(GradCDF, GradCDF.numDimensions()-1 , 0)), N_Grad, Grad_Offset, EPS);
        // for Y component
        T_tmp += FindT(Views.iterable(Views.hyperSlice(GradCDF, GradCDF.numDimensions()-1 , 1)), N_Grad, Grad_Offset, EPS);
        // Average them and divide by the prior parameter
        float T1 = T_tmp/(2*T1_pr);
        
        // Find the best parameter and divide by the T2 prior
        float T2 = (float)FindT(LapCDF, N_Lap, Lap_Offset, EPS)/T2prior;
        
        // Calculate naturalization factor!
        float Nf = (float) ((1.0-Theta.getRealDouble())*T1 + Theta.getRealDouble()*T2);
        
        return Nf;
    }
    
    /**
     * Calculate the peak SNR from the Naturalization factor
     * 
     * @param Nf naturalization factor
     * @return the PSNR
     */
    String calculate_PSNR(double x)
    {
        if (x >=  0 && x <= 0.934)
        {
            return String.format("%.2f", new Float(23.65 * Math.exp(0.6 * x) - 20.0 * Math.exp(-7.508 * x)));
        }
        else if (x > 0.934 && x < 1.07)
        {
            return new String("> 40");
        }
        else if (x >= 1.07 && x < 1.9)
        {
            return String.format("%.2f", new Float(-11.566 * x + 52.776));
        }
        else
        {
            return String.format("%.2f",new Float(13.06*x*x*x*x - 121.4 * x*x*x + 408.5 * x*x -595.5*x + 349));
        }
    }
    
    private Img<UnsignedByteType> naturalizeOneChannel(long[] dims, Img<ARGBType> image_orig, int channelPrior) {
        Img<UnsignedByteType> channel = new ArrayImgFactory<UnsignedByteType>().create(dims, new UnsignedByteType());
        Cursor<ARGBType> aCur = image_orig.cursor();
        Cursor<UnsignedByteType> cursor = channel.cursor();
        
        // Copy specified channel for further processing
        while (cursor.hasNext()) {
            cursor.next();
            aCur.next();
            switch (channelPrior) {
            case 0: {
                cursor.get().set(ARGBType.blue(aCur.get().get()));
                break;
            }
            case 1: {
                cursor.get().set(ARGBType.green(aCur.get().get()));
                break;
            }
            case 2: {
                cursor.get().set(ARGBType.red(aCur.get().get()));
                break;
            }
            default:
                break;
            }
        }
        
        channel = performNaturalization(channelPrior, channel);

        return channel;
    }

    private Img<UnsignedByteType> performNaturalization(int channelPrior, Img<UnsignedByteType> channel) {
        // Parameters balance between first order and second order     
        FloatType Theta = new FloatType(0.5f);
        try {
            channel = doNaturalization(channel, Theta, channelPrior, UnsignedByteType.class, FloatType.class);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        
        return channel;
    }

    // Original data
    // N = nuber of bins
    // offset of the histogram
    // T current
    private double FindT_Evalue(float[] p_d, int N, int offset, float T)
    {
        double error = 0;

        for (int i=-offset; i<N-offset; ++i) {
            double tmp = Math.atan(T*(i)) - p_d[i+offset];
            error += (tmp*tmp);
        }

        return error;
    }

    // Find the T
    // data CDF Histogram
    // N number of bins
    // Offset of the histogram
    // eps precision
    private double FindT(IterableInterval<FloatType> data, int N, int OffSet, float eps)
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

    private Img<FloatType> create2DGradientField() {
        long dims[] = new long[2];
        dims[0] = N_Grad;
        dims[1] = N_Grad;
        Img<FloatType> GradD = new ArrayImgFactory<FloatType>().create(dims, new FloatType());
        return GradD;
    }

    private void calculateLapCDF(Img<FloatType> LapCDF) {
        RandomAccess<FloatType> Lap_hist2 = LapCDF.randomAccess();
        //convert Lap to CDF
        for (int i = 1; i < N_Lap; ++i)
        {
            Lap_hist2.setPosition(i-1,0);
            float prec = Lap_hist2.get().getRealFloat();
            Lap_hist2.move(1,0);
            Lap_hist2.get().set(Lap_hist2.get().getRealFloat() + prec);
        }
    }

    private void calculateGradCDF(Img<FloatType> GradCDF, Img<FloatType> GradD) {
        RandomAccess<FloatType> Grad_dist = GradD.randomAccess();
        
        // Gradient on x pointer
        IntervalView<FloatType> Gradx = Views.hyperSlice(GradCDF, GradCDF.numDimensions()-1 , 0);
        // Gradient on y pointer
        IntervalView<FloatType> Grady = Views.hyperSlice(GradCDF, GradCDF.numDimensions()-1 , 1);
        
        integrateOverRowAndCol(Grad_dist, Gradx, Grady);

        scaleGradiens(Gradx, Grady);
    }

    private void scaleGradiens(IntervalView<FloatType> Gradx, IntervalView<FloatType> Grady) {
        RandomAccess<FloatType> Gradx_r2 = Gradx.randomAccess();
        RandomAccess<FloatType> Grady_r2 = Grady.randomAccess();
        //scale, divide the number of integrated bins
        for (int i = 0; i < N_Grad; ++i)
        {
            Gradx_r2.setPosition(i,0);
            Grady_r2.setPosition(i,0);
            Gradx_r2.get().set((float) (Gradx_r2.get().getRealFloat() / 255.0));
            Grady_r2.get().set((float) (Grady_r2.get().getRealFloat() / 255.0));
        }
    }

    private void integrateOverRowAndCol(RandomAccess<FloatType> Grad_dist, IntervalView<FloatType> Gradx, IntervalView<FloatType> Grady) {
        int[] loc = new int[2];     
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
    }

    private <T extends RealType<T>> void calculateLaplaceFieldAndGradient(Img<T> image, Img<FloatType> LapCDF, Img<FloatType> GradD) {
        RandomAccess<FloatType> Grad_dist = GradD.randomAccess();
        long[] origImgDimensions = new long[2];
        image.dimensions(origImgDimensions);
        Img<FloatType> laplaceField = new ArrayImgFactory<FloatType>().create(origImgDimensions, new FloatType());
        
        // Cursor localization
        int[] indexD = new int[2];
        int[] loc_p = new int[2];
        RandomAccess<T> img_cur = image.randomAccess();
        RandomAccess<FloatType> Lap_f = laplaceField.randomAccess();
        RandomAccess<FloatType> Lap_hist = LapCDF.randomAccess();
        

        // Normalization 1/(Number of pixel of the original image)
        long n_pixel = 1;
        for (int i = 0 ; i < laplaceField.numDimensions() ; i++)
        {n_pixel *= laplaceField.dimension(i)-2;}
        // unit to sum
        double f = 1.0/(n_pixel);
        
        // Inside the image for Y          
        Cursor<FloatType> cur = laplaceField.cursor();

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
    }

    private void convertGrad2dToCDF(Img<FloatType> GradD) {
        RandomAccess<FloatType> Grad_dist = GradD.randomAccess();
        int[] loc = new int[GradD.numDimensions()];  
        
        // for each row
        for (int j = 0; j < GradD.dimension(1); ++j)
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
        for (int j = 1; j < GradD.dimension(1); ++j)
        {
            // Move to the actual position
            loc[1] = j-1;
            
            for (int i = 0; i < GradD.dimension(0); ++i)
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
    }
    
    /**
     * Show information about authors and paper.
     */
    private void showMessage()
    {
        JDialog win = new JDialog((JDialog)null,"Naturalization",true);
        
        JPanel msg = new JPanel();
        win.getContentPane().add(msg);
        Border main_border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        msg.setBorder(main_border);
        win.add(msg);
        
        JPanel mP1 = new JPanel();
        JLabel lbl_pap1 = new JLabel("<html>Y. Gong and I. F. Sbalzarini. Image enhancement by gradient distribution specification. In Proc. ACCV, <br>"
                + "12th Asian Conference on Computer Vision, Workshop on Emerging Topics in Image Enhancement and Restoration,<br>"
                + "pages w7–p3, Singapore, November 2014.<br><br>"
                + "Y. Gong and I. F. Sbalzarini, Gradient Distributions Priors for Biomedical Image Processing, 2014 <a href=\"http://arxiv.org/abs/1408.3300\">http://arxiv.org/abs/1408.3300</a></html>");
        Border border = BorderFactory.createLineBorder(Color.BLACK, 2);
        lbl_pap1.setBorder(border);
        mP1.add(lbl_pap1);
        win.getContentPane().add(mP1, BorderLayout.SOUTH);
        
        win.pack();
        win.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        win.setVisible(true);
    }
}
