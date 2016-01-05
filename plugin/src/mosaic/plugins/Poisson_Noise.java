package mosaic.plugins;

import java.awt.Rectangle;
import java.lang.reflect.Array;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import mosaic.noise_sample.GenericNoiseSampler;
import mosaic.noise_sample.NoiseSample;
import mosaic.noise_sample.noiseList;
import net.imglib2.Cursor;
import net.imglib2.histogram.BinMapper1d;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Integer1dBinMapper;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * A ImageJ Plugin that inserts Noise to an image or an image stack.
 * @author Pietro Incardona, MPI-CBG Dresden
 * @version 1.0, January 08
 */
public class Poisson_Noise implements ExtendedPlugInFilter {
    // Plugin configuration and input
    static final int FLAGS = DOES_8G + DOES_16 + DOES_32;
    ImagePlus iOrigImg;
    
    // user input (GUI)
    String iNoiseModel;
    double iDilatation = 1.0;


    @Override
    public int setup(String aArgs, ImagePlus aImp) {
        iOrigImg = aImp;
        return FLAGS;
    }

    @Override
    public int showDialog(ImagePlus arg0, String arg1, PlugInFilterRunner arg2) {
        final GenericDialog gd = new GenericDialog("Choose type of noise");
        gd.addChoice("Choose noise model", noiseList.noiseList, noiseList.noiseList[0]);
        gd.addNumericField("Offset", 0.0, 3);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return DONE;
        }
        
        iDilatation = gd.getNextNumber();
        iNoiseModel = gd.getNextChoice();

        return FLAGS;
    }

    @Override
    public void run(ImageProcessor aImageProcessor) {
       final int BYTE=0;
       final int SHORT=1;
       final int FLOAT=2;
       NoiseSample<?> ns;
        // Get the Type
        int vType;
        if (aImageProcessor instanceof ByteProcessor) {
            vType = BYTE;
            ns = noiseList.<UnsignedByteType> factory(iNoiseModel, iDilatation);
        } else if (aImageProcessor instanceof ShortProcessor) {
            vType = SHORT;
            ns = noiseList.<ShortType> factory(iNoiseModel, iDilatation);
        } else if (aImageProcessor instanceof FloatProcessor) {
            vType = FLOAT;
            ns = noiseList.<FloatType> factory(iNoiseModel, iDilatation);
        } else {
            IJ.showMessage("Wrong image type");
            return;
        }
    
        // We do not have a noise model (yet)
        if (ns == null) {
            if (vType == BYTE) {
                this.<UnsignedByteType> setupGenericNoise(UnsignedByteType.class);
            }
            else if (vType == SHORT) {
                this.<ShortType> setupGenericNoise(ShortType.class);
            }
            else if (vType == FLOAT) {
                this.<FloatType> setupGenericNoise(FloatType.class);
            }
        }
    
        // Sample from it
        if (vType == BYTE) {
            this.<UnsignedByteType> sample(iOrigImg, UnsignedByteType.class, ns);
        }
        else if (vType == SHORT) {
            this.<ShortType> sample(iOrigImg, ShortType.class, ns);
        }
        else if (vType == FLOAT) {
            this.<FloatType> sample(iOrigImg, FloatType.class, ns);
        }
    }

   @Override
   public void setNPasses(int arg0) {
       // Nothing to do here
   }

   /**
     * Create an integer bin mapper
     *
     * @param nbin number of bins
     * @return the integer bin mapper
     */
    private <S extends IntegerType<S>> BinMapper1d<S> createIntegerMapper(int nbin) {
        return new Integer1dBinMapper<S>(0,nbin,true);
    }

    /**
     * Create a bin mapper
     *
     * @param cls class
     * @param min value
     * @param max value
     * @return The bin mapper
     */
    @SuppressWarnings("unchecked") 
    <T extends RealType<T>> BinMapper1d<T> createMapper(Class<T> cls,double min,double max) {
        T test = null;
        try {
            test = cls.newInstance();
        } catch (final InstantiationException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        }

        final Object test_o = test;

        if (test_o instanceof UnsignedByteType) {
            return (BinMapper1d<T>) this.<UnsignedByteType>createIntegerMapper(256);
        }
        else if (test_o instanceof ShortType) {
            return (BinMapper1d<T>) this.<ShortType>createIntegerMapper(65536);
        }
        else if (test_o instanceof FloatType) {
            return new Real1dBinMapper<T>(min,max,65536,true);
        }
        return null;
    }

    /**
     * Process generic noise from segmentation
     *
     * @param cls Class<T>
     */
    private <T extends RealType<T>  & NativeType< T > > void setupGenericNoise(Class<T> cls) {
        // Create a generic noise
        final GenericNoiseSampler<T> gns = new GenericNoiseSampler<T>(cls);

        // Create histograms by ROI
        RoiManager manager = RoiManager.getInstance();
        if (manager == null) {
            manager = new RoiManager();
        }
        final Roi[] roisArray = manager.getRoisAsArray();
        for (final Roi roi : roisArray) {
            final ImagePlus tmp = new ImagePlus(roi.getName(),ij.WindowManager.getImage(roi.getImageID()).getProcessor());

            final Rectangle b = roi.getBounds();

            final ImageProcessor ip = tmp.getProcessor();
            ip.setRoi(b.x,b.y,b.width,b.height);
            tmp.setProcessor(null,ip.crop());

            // iterate trought all the image and create the histogram
            final double histMin = tmp.getStatistics().histMin;
            final double histMax = tmp.getStatistics().histMax;

            final BinMapper1d<T> bM = createMapper(cls,histMin,histMax);

            final Histogram1d<T> hist = new Histogram1d<T>(bM);

            // Convert an imagePlus into ImgLib2
            final Img< T > image = ImagePlusAdapter.wrap( tmp );
            final Cursor<T> cur = image.cursor();

            // Add data
            try {
                T mean_t = cls.newInstance();
                @SuppressWarnings("unchecked")
                final T [] ipnt = (T[]) Array.newInstance(cls, (int)image.size());
                
                double mean = 0.0;
                int cnt = 0;
                while (cur.hasNext()) {
                    ipnt[cnt] = cur.next();
                    mean += ipnt[cnt].getRealDouble();
                    cnt++;
                }
                
                mean /= cnt;
                mean_t.setReal(mean);
                hist.addData(Arrays.asList(ipnt));
                gns.setHistogram(mean_t, hist);
            } catch (final InstantiationException e) {
                e.printStackTrace();
                throw new RuntimeException();
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }
    }

    private <T extends RealType< T > & NativeType< T > > void sample(ImagePlus imp, Class<T> cls, NoiseSample<?> ns) {
        // Convert an imagePlus into ImgLib2
        final Img< T > image = ImagePlusAdapter.wrap(imp);
        final Cursor<T> cur = image.cursor();

        int numOfDims = 2;
        numOfDims += (imp.getNFrames() > 1) ? 1 : 0;
        numOfDims += (imp.getNChannels() > 1) ? 1 : 0;
        numOfDims += (imp.getNSlices() > 1) ? 1 : 0;
        final int loc[] = new int[numOfDims];

        @SuppressWarnings("unchecked")
        final NoiseSample<T> nsT = (NoiseSample<T>) ns;
        try {
            T smp = cls.newInstance();
            while (cur.hasNext()) {
                cur.next();
                cur.localize(loc);
                nsT.sample(cur.get(), smp);
                cur.get().set(smp);
            }
        } catch (final InstantiationException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
