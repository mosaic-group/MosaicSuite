package mosaic.region_competition;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.process.StackStatistics;
import mosaic.test.framework.CommonBase;
import mosaic.utils.Debug;
import volume.Kernel2D;
import volume.Kernel3D;
import volume.Sobel;
import volume.Sobel3D;
import volume.VolumeFloat;

public class TemporaryTest extends CommonBase {
    private static final Logger logger = Logger.getLogger(TemporaryTest.class);
    
    
    @Test
    public void genSobel2D() throws InterruptedException {
        ImagePlus img = IJ.openImage("https://upload.wikimedia.org/wikipedia/commons/3/3f/Bikesgray.jpg");
        img.setStack(img.getImageStack().convertToFloat());
        img.show();
        
        VolumeFloat v = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
        v.load(img.getImageStack(), 0);
        Kernel2D k = new Sobel();

        SobelVolume vn = new SobelVolume(img.getWidth(), img.getHeight(), img.getNSlices());
        vn.sobel2D(v, k);
//      Implementation using original VolumeFloat:
//      
//        VolumeFloat dx = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
//        VolumeFloat dy = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
//        dx.convolvex(v, k);
//        dy.convolvey(v, k);
//        dx.mul(dx);
//        dy.mul(dy);
//        dx.add(dy);
//        dx.sqrt();
//        compareArrays(dx.getVolume(), vn.getVolume(), 0.0001f);
        
        ImagePlus out = new ImagePlus("XXXX", vn.getImageStack());
        StackStatistics ss = new StackStatistics(out);
        out.setDisplayRange(ss.min,  ss.max);        
        out.show();
        IJ.save(out, "/Users/gonciarz/Documents/MOSAIC/work/testInputs/BikesgraySobel.tif");
        Thread.sleep(15000);
    }
    
    @Test
//    @Ignore
    public void genSobel3D() throws InterruptedException {
        // start ImageJ
//        new ImageJ();

        ImagePlus img = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/tutorials/advanced-imglib2/images/t1-head.tif");
//        ImagePlus img = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/testInputs/region.tif");
        img.setStack(img.getImageStack().convertToFloat());
        img.show();
        
        VolumeFloat v = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
        v.load(img.getImageStack(), 0);
        Kernel3D k = new Sobel3D();
        
        SobelVolume vn = new SobelVolume(img.getWidth(), img.getHeight(), img.getNSlices());
        vn.sobel3D(v, k);
        
//        Implementation using original VolumeFloat does not work since valid(x,y) is used instead valid(x,y,z):
//        
//        VolumeFloat dx = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
//        VolumeFloat dy = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
//        VolumeFloat dz = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
//        dx.convolvex(v, k);
//        dy.convolvey(v, k);
//        dz.convolvez(v, k);
//        dx.mul(dx);
//        dy.mul(dy);
//        dz.mul(dz);
//        dx.add(dy);
//        dx.add(dz);
//        dx.sqrt();
//        compareArrays(dx.getVolume(), vn.getVolume(), 0.0001f);
        
        ImagePlus out = new ImagePlus("XXXX", vn.getImageStack());
        StackStatistics ss = new StackStatistics(out);
        out.setDisplayRange(ss.min,  ss.max);
        out.show();
        IJ.save(out, "/Users/gonciarz/Documents/MOSAIC/work/testInputs/HeadSobel.tif");
        Thread.sleep(15000);
    }
    
    double GetVariateWithOpenUpperRange(MersenneTwister aRng) {
        return (aRng.nextInt() & 0xffffffffL) * ( 1.0 / 4294967296.0 );
    }
    
    double GetUniformVariate(MersenneTwister aRng, double a, double b) {
        double u = GetVariateWithOpenUpperRange(aRng);
        return ( ( 1.0 - u ) * a + u * b );
    }

    int GetIntegerVariate(MersenneTwister aRng, int n) {
        // Find which bits are used in n
        long used = n;
        used |= used >> 1;
        used |= used >> 2;
        used |= used >> 4;
        used |= used >> 8;
        used |= used >> 16;
        used |= used >> 32;

        // Draw numbers until one is found in [0,n]
        long i;
        do {
            i = (aRng.nextInt() & 0xffffffffL) & used; // toss unused bits to shorten search
        }
        while ( i > n );

        return (int)i;
    }
    
    double GetVariate(MersenneTwister aRng) {
        return aRng.nextDouble();
    }

    @Test
    public void test() {
        MersenneTwister rng = new MersenneTwister(5489);
        for (int i = 0; i < 3; ++i) {
                System.out.println(GetIntegerVariate(rng, 10));
                System.out.println(GetUniformVariate(rng, 2, 5));
                System.out.println(GetVariate(rng));
        }
        
        rng.nextDouble();
        
        List<Pair<Integer, Double>> pmf = new ArrayList<>();
        pmf.add(new Pair<Integer, Double>(0, 40.0));
        pmf.add(new Pair<Integer, Double>(3, 10.0));
        pmf.add(new Pair<Integer, Double>(2, 10.0));
        pmf.add(new Pair<Integer, Double>(1, 40.0));
        EnumeratedDistribution<Integer> drng = new EnumeratedDistribution<>(new MersenneTwister(5489), pmf);
        int[] count = new int[6];
        for (int i = 0; i < 5; ++i) {
            double x = drng.sample(); 
            System.out.println(x);
        }
        mosaic.utils.Debug.print(count);
        mosaic.utils.Debug.print(drng.getPmf()); 
    }
    
    
    @Test
    public void testDistributionFromSobel() throws InterruptedException {
        ImagePlus img = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/testInputs/BikesgraySobel.tif");
//        ImagePlus img = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/testInputs/HeadSobel.tif");
        
        System.out.println("Adding points");
//        Map<Double, Double> values = new HashMap<>();
//        int count = 0;
//        int countZeros = 0;
//        int countZeros2 = 0;
//        if (img.getNSlices() > 1) {
//            ImageStack is = img.getImageStack();
//            for (int i = 1; i < is.size(); i++) {
//                System.out.println("Processing " + i);
//                float[] pixels = (float[])is.getPixels(i);
//                for (int x = 0; x < pixels.length; x++) {
//                    Double v = values.get((double)pixels[x]);
//                    if (v == null) {
//                        if (pixels[x] == 0.0) countZeros2++;
//                        v = 0.0;
//                    }
//                    v += 1.0;
//                    values.put((double) pixels[x], v);
//                    count++;
//                    if (pixels[x] == 0.0f ) countZeros++;
//                }
//            }}
//        else {
//            System.out.println("Only 1-z");
//            float[] pixels = (float[])img.getProcessor().getPixels();
//            for (int x = 0; x < pixels.length; x++) {
//                Double v = values.get(pixels[x]);
//                if (v == null) {
//                    v = 0.0;
//                }
//                v += 1;
//                values.put((double) pixels[x], v);
//                count++;
//                if (pixels[x] == 0.0f ) countZeros++;
//            }
//        }
//        System.out.println("0..........." + values.get(0.0) + " NUM " + countZeros + " " + countZeros2);
//        List<Pair<Double, Double>> pmf = new ArrayList<>();
//        for (Double k : values.keySet()) {
//            pmf.add(new Pair<Double, Double>(k, values.get(k)));
//        }
        System.out.println("Creating distr...");
//        EnumeratedDistribution<Double> drng = new EnumeratedDistribution<>(new MersenneTwister(5489), pmf);
        System.out.println("Creating distr... DONE");
//        mosaic.utils.Debug.print(count + " " + drng.getPmf().size() + " " + drng.getPmf().get(0));
        logger.debug("Start");
        EnumeratedDistribution<Integer> drng = generateImgDistribution(img);
        logger.debug("Stop");
        generateProbabilityPlot(drng, 512).show();
        int[] count = new int[10];
        logger.debug("Sampling start");
        for (int i = 0; i < 10000000; ++i) 
            count[drng.sample()/100000]++;
        logger.debug("Sampling stop");
        System.out.println(Debug.getString(count));
        Thread.sleep(115000);
        System.out.println("DONE");
    }
    
    EnumeratedDistribution<Integer> generateImgDistribution(ImagePlus img) {
        int index = 0;
        List<Pair<Integer, Double>> pmf = new ArrayList<>();
        if (img.getNSlices() > 1) {
            ImageStack is = img.getImageStack();
            for (int i = 1; i < is.size(); i++) {
                float[] pixels = (float[])is.getPixels(i);
                for (int x = 0; x < pixels.length; x++) {
                    pmf.add(new Pair<Integer, Double>(index++, (double) pixels[x]));
                }
            }
        }
        else {
            float[] pixels = (float[])img.getProcessor().getPixels();
            for (int x = 0; x < pixels.length; x++) {
                pmf.add(new Pair<Integer, Double>(index++, (double) pixels[x]));
            }
        }
        EnumeratedDistribution<Integer> drng = new EnumeratedDistribution<>(new MersenneTwister(5489), pmf);
        
        return drng;
    }

    Plot generateProbabilityPlot(EnumeratedDistribution<Integer> aDistribution, int aNumOfBins) {
        List<Pair<Integer, Double>> pmf = aDistribution.getPmf();
        
        // Find min/max for calculating bin width
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < pmf.size(); ++i) {
            Pair<Integer, Double> p = pmf.get(i);
            double v = p.getFirst();
            if (v > max) max = v;
            if (v < min) min = v;
        }

        // Create data for plot
        double binWidth = (max - min) / aNumOfBins;
        double sumOfProbs = 0.0;
        double[] xv = new double[aNumOfBins];
        double[] yv = new double[aNumOfBins];
        for (int i = 0; i < aNumOfBins; ++i) xv[i] = min + i * binWidth;
        for (int i = 0; i < pmf.size(); ++i) {
            Pair<Integer, Double> p = pmf.get(i);
            int idx = (int)((aNumOfBins-1) * ((double)p.getFirst() - min) / (max - min));
            yv[idx] += p.getSecond();
            sumOfProbs += p.getSecond();
        }
        logger.debug("Min: " + min + " Max: " + max + " NumOfBins: " + aNumOfBins + " Bin width: " + binWidth + " SumOfProbs: " + sumOfProbs);
        final Plot plot = new Plot("Probability distribution", "Values", "Prob", xv, yv);
        return plot;
    }
}
