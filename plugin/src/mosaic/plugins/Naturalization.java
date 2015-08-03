package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import mosaic.plugins.utils.PlugIn8bitBase;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
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
public class Naturalization extends PlugIn8bitBase
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

    // Keeps values of PSNR for all images and channels in case of RGB. Maps: imageNumber -> map (channel, PSNR value)
    private Map<Integer, Map<Integer, Float>> iPsnrOutput = new TreeMap<Integer, Map<Integer, Float>>();
    private synchronized void addPsnr(int aSlice, int aChannel, float aValue) {
        Map<Integer, Float> map = iPsnrOutput.get(aSlice);
        boolean isNewMap = false;
        if (map == null) {
            map = new TreeMap<Integer, Float>();
            isNewMap = true;
        }
        map.put(aChannel, aValue);
        if (isNewMap) iPsnrOutput.put(aSlice, map);
    }

    @Override
    protected void processImg(ByteProcessor aOutputImg, ByteProcessor aOrigImg, int aChannelNumber) {
        // perform naturalization
        ImagePlus nat = naturalize8bitImage(aOrigImg, aChannelNumber);

        // set processed pixels to output image
        aOutputImg.setPixels(nat.getProcessor().getPixels());
    }
    
    @Override 
    protected void postprocess() {
        ResultsTable rs = new ResultsTable();
        
        for (Entry<Integer, Map<Integer, Float>> e : iPsnrOutput.entrySet()) {
            rs.incrementCounter();
            for (Entry<Integer, Float> m : e.getValue().entrySet()) {
                switch(m.getKey()) {
                    case 0: rs.addValue("Naturalization R", m.getValue()); rs.addValue("Estimated R PSNR", calculate_PSNR(m.getValue())); break;
                    case 1: rs.addValue("Naturalization G", m.getValue()); rs.addValue("Estimated G PSNR", calculate_PSNR(m.getValue())); break;
                    case 2: rs.addValue("Naturalization B", m.getValue()); rs.addValue("Estimated B PSNR", calculate_PSNR(m.getValue())); break;
                    case 3: rs.addValue("Naturalization", m.getValue()); rs.addValue("Estimated PSNR", calculate_PSNR(m.getValue())); break;
                }
            }
        }
        
        if (!Interpreter.isBatchMode()) {
            rs.show("Naturalization and PSNR");
            showMessage();
        }
    };

    private ImagePlus naturalize8bitImage(ByteProcessor imp, int aChannelNumber) {
        long rdims[] = new long[3];
        rdims[0] = imp.getWidth();
        rdims[1] = imp.getHeight();
        rdims[2] = 1;

        Img<UnsignedByteType> Channel = new ArrayImgFactory<UnsignedByteType>().create(rdims, new UnsignedByteType());
        
        Img<UnsignedByteType> TChannel = ImagePlusAdapter.wrap(new ImagePlus("", imp));
        int priorNumber = (aChannelNumber < 3) ? 2-aChannelNumber : 3;
        float T2_prior = T2_pr[priorNumber];
        float[] result = {0.0f}; // ugly but one of way to get result back via parameters;
        TChannel = performNaturalization(TChannel, T2_prior, result);
        
        // Merge the result
        IntervalView<UnsignedByteType> view = Views.hyperSlice(Channel, 2, 0);
        RandomAccess<UnsignedByteType> result_ra = view.randomAccess();
    
        int loc[] = new int[2];
        Cursor<UnsignedByteType> rc = TChannel.cursor();
    
        while (rc.hasNext()) {
            rc.next();
            rc.localize(loc);
            result_ra.setPosition(loc);
            result_ra.get().set(rc.get());
        }
       
        addPsnr(imp.getSliceNumber(), aChannelNumber, result[0]);

        return ImageJFunctions.wrap(Channel,"temporaryName");
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
    private <T extends NumericType<T> & NativeType<T> & RealType<T>, S extends RealType<S>> Img<T> doNaturalization(Img<T> image_orig, S Theta,Class<T> cls_t, Class<S> cls_s, float T2_prior, float[] result) throws InstantiationException, IllegalAccessException
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
        
        float Nf = findNaturalizationFactor(image_orig, Theta, T2_prior);
        result[0] = Nf;
        
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
  
    private Img<UnsignedByteType> performNaturalization(Img<UnsignedByteType> channel, float T2_prior, float[] result) {
        // Parameters balance between first order and second order     
        FloatType Theta = new FloatType(0.5f);
        try {
            channel = doNaturalization(channel, Theta, UnsignedByteType.class, FloatType.class, T2_prior, result);
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

    @Override
    protected boolean showDialog() {
        return true;
    }

    @Override
    protected boolean setup(String aArgs) {
        setFilePrefix("naturalized_");
        return true;
    }
}
