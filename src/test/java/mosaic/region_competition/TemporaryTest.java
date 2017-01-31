package mosaic.region_competition;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.process.StackStatistics;
import mosaic.region_competition.DRS.Rng;
import mosaic.region_competition.DRS.SobelImg;
import mosaic.region_competition.DRS.SobelVolume;
import mosaic.test.framework.CommonBase;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Temporary file to keep some tests/examples of libraries used during DRS implementation.
 * TODO: This file is to be removed at the end of implementation. 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class TemporaryTest extends CommonBase {
    private static final Logger logger = Logger.getLogger(TemporaryTest.class);

    @Test
    @Ignore
    public void genSobel3DviaImglib2() {
        ImagePlus imp = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/tutorials/advanced-imglib2/images/t1-head.tif");
        imp.setStack(imp.getImageStack().convertToFloat());
        Img<FloatType> img3 = SobelImg.filter(ImageJFunctions.wrapFloat(imp), false);
        final ImagePlus ip = ImageJFunctions.wrap(img3, ""); 
        IJ.save(ip, "/Users/gonciarz/Documents/MOSAIC/work/testInputs/HeadSobel6.tif");
    }

    @Test
    @Ignore
    public void genSobel3viaVolume() {
        ImagePlus img = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/tutorials/advanced-imglib2/images/t1-head.tif");
        img.setStack(img.getImageStack().convertToFloat());
        img.show();
        SobelVolume vn = new SobelVolume(img);
        vn.sobel3D();
        ImagePlus out = new ImagePlus("XXXX", vn.getImageStack());
        StackStatistics ss = new StackStatistics(out);
        out.setDisplayRange(ss.min, ss.max);
        out.show();
        IJ.save(out, "/Users/gonciarz/Documents/MOSAIC/work/testInputs/HeadSobel.tif");
    }

    @Test
    @Ignore
    public void genSobel2D() throws InterruptedException {
        ImagePlus img = IJ.openImage("https://upload.wikimedia.org/wikipedia/commons/3/3f/Bikesgray.jpg");
        img.setStack(img.getImageStack().convertToFloat());
        img.show();

        SobelVolume vn = new SobelVolume(img);
        vn.sobel2D();
        ImagePlus out = new ImagePlus("XXXX", vn.getImageStack());
        StackStatistics ss = new StackStatistics(out);
        out.setDisplayRange(ss.min,  ss.max);        
        out.show();

        Img<FloatType> img3 = SobelImg.filter(ImageJFunctions.wrapFloat(img), false);
        ImageJFunctions.wrap(img3, "imglib2 output").show();
        IJ.save(out, "/Users/gonciarz/Documents/MOSAIC/work/testInputs/BikesgraySobel.tif");
        Thread.sleep(15000);
    }

    @Test
    @Ignore
    public void test() {
        Rng rng = new Rng();
        for (int i = 0; i < 3; ++i) {
            System.out.println(rng.GetIntegerVariate(10));
            System.out.println(rng.GetUniformVariate(2, 5));
            System.out.println(rng.GetVariate());
        }

        List<Pair<Integer, Double>> pmf = new ArrayList<>();
        pmf.add(new Pair<Integer, Double>(0, 40.0));
        pmf.add(new Pair<Integer, Double>(3, 10.0));
        pmf.add(new Pair<Integer, Double>(2, 10.0));
        pmf.add(new Pair<Integer, Double>(1, 40.0));
        EnumeratedDistribution<Integer> drng = new EnumeratedDistribution<>(new Rng(), pmf);
        for (int i = 0; i < 5; ++i) {
            double x = drng.sample(); 
            System.out.println(x);
        }
        mosaic.utils.Debug.print(drng.getPmf()); 
    }

    @Test
    @Ignore
    public void testDistributionFromSobel() throws InterruptedException {
        ImagePlus img = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/testInputs/BikesgraySobel.tif");

        EnumeratedDistribution<Integer> drng = generateImgDistribution(img);
        generateProbabilityPlot(drng, 512).show();

        Thread.sleep(15000);
    }

    /**
     * Generates enumerated distribution from provided image.
     */
    EnumeratedDistribution<Integer> generateImgDistribution(ImagePlus aImg) {
        int index = 0;
        List<Pair<Integer, Double>> pmf = new ArrayList<>();
        if (aImg.getNSlices() > 1) {
            ImageStack is = aImg.getImageStack();
            for (int i = 1; i < is.size(); i++) {
                float[] pixels = (float[])is.getPixels(i);
                for (int x = 0; x < pixels.length; x++) {
                    pmf.add(new Pair<Integer, Double>(index++, (double) pixels[x]));
                }
            }
        }
        else {
            float[] pixels = (float[])aImg.getProcessor().getPixels();
            for (int x = 0; x < pixels.length; x++) {
                pmf.add(new Pair<Integer, Double>(index++, (double) pixels[x]));
            }
        }
        return new EnumeratedDistribution<>(new Rng(), pmf);
    }

    /**
     * Generates plot from enumerated distribution by putting all elements into bins. All probabilities
     * for given bin are summed.
     */
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
        
        return new Plot("Probability distribution", "Values", "Prob", xv, yv);
    }
}
