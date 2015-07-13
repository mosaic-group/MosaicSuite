package mosaic.filamentSegmentation;

import static org.junit.Assert.*;

import java.awt.Dimension;

import mosaic.filamentSegmentation.SegmentationAlgorithm.EnergyOutput;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.ThresholdFuzzyOutput;
import mosaic.math.Matrix;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class SegmentationAlgorithmTest extends CommonBase {

    // 7 x 11, 1 filament, without noise
    final static double[][] simpleImage1filament = new double[][] {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 
                                                                   {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 
                                                                   {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0}, 
                                                                   {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0}, 
                                                                   {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 
                                                                   {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 
                                                                   {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    
    @Test
    public void testRun() {
        // TODO: remove it after whole implementation - it is used only for develop-time.
        SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament, 
                                                             NoiseType.GAUSSIAN, 
                                                             PsfType.GAUSSIAN, 
                                                             new Dimension(3,2), 
                                /* subpixel sumpling */      1, 
                                /* scale */                  0, 
                                /* regularizer term */       0.0001);
        sa.performSegmentation();
    }
    
    @Test
    public void testEnergyMinimalizationGaussian() {
        // Expected data taken from Matlab implementation
        final double[][] expectedMask = new double[][] {
                    {0.00083539, 0.00015976, 2.7859e-05, 7.0548e-06, 4.0275e-06, 6.6138e-06, 1.8013e-05, 3.2308e-05, 2.2967e-05, 5.2069e-06, 2.7117e-07, 2.1423e-09},
                    {0.0040958, 0.0097657, 0.017571, 0.036922, 0.10925, 0.27552, 0.27648, 0.11332, 0.020963, 0.00081855, 5.0711e-06, 4.5346e-09},
                    {0.0061483, 0.023551, 0.052931, 0.18244, 0.85519, 0.92675, 0.96474, 0.36471, 0.098898, 0.0062066, 1.5201e-05, 5.554e-09},
                    {0.0036951, 0.023527, 0.068793, 0.2753, 0.9861, 0.81079, 1, 0.42402, 0.10737, 0.0048702, 9.9094e-06, 4.0904e-09},
                    {0.00090794, 0.0068506, 0.036208, 0.17335, 0.69411, 0.92014, 0.78486, 0.27113, 0.039036, 0.00060544, 1.9246e-06, 1.8884e-09},
                    {0.00010945, 0.0004401, 0.001856, 0.0082819, 0.033694, 0.076953, 0.06281, 0.014483, 0.00089053, 1.8395e-05, 1.5453e-07, 5.6737e-10},
                    {8.7077e-06, 2.0177e-05, 4.7915e-05, 0.00011018, 0.00021678, 0.00029341, 0.00019925, 5.2043e-05, 5.2748e-06, 2.5874e-07, 7.17e-09, 1.0669e-10},
                    {5.9905e-07, 1.6185e-06, 3.1325e-06, 4.2914e-06, 3.9799e-06, 2.3273e-06, 8.2537e-07, 1.8238e-07, 2.7235e-08, 3.0229e-09, 2.5202e-10, 0}
        };
        final double expectedMLEin = 1.0187;
        final double expectedMLEout = 0.0072118;
        
        SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament, 
                                                             NoiseType.GAUSSIAN, 
                                                             PsfType.GAUSSIAN, 
                                                             new Dimension(3,2), 
                                /* subpixel sumpling */      1, 
                                /* scale */                  1, 
                                /* regularizer term */       0.0001,
                                /* no of iterations */       2);
        
        // Tested function
        EnergyOutput result = sa.minimizeEnergy();
        
        double epsilon = 0.000005;
        assertTrue("Mask", new Matrix(expectedMask).compare(result.iMask, epsilon));
        assertEquals("MLEin", expectedMLEin, result.iRss.getBetaMLEin(), expectedMLEin/10000);
        assertEquals("MLEout", expectedMLEout, result.iRss.getBetaMLEout(), expectedMLEout/10000);
    }
    
    @Test
    public void testEnergyMinimalizationPoisson() {
        // Expected data taken from Matlab implementation
        final double[][] expectedMask = new double[][] {
                {0.0021514, 0.017863, 0.035141, 0.041675, 0.020672, 0.0036366, 0.00072863, 5.2948e-05, 1.7086e-06, 4.8453e-08, 1.3991e-09},
                {0.0056159, 0.047015, 0.08513, 0.11254, 1, 0.48198, 0.11764, 0.0035058, 1.4948e-05, 1.2395e-07, 2.3619e-09},
                {0.0054375, 0.10829, 0.67133, 0.54096, 0.78986, 0.48845, 0.18898, 0.078905, 6.4245e-05, 1.7332e-07, 2.4088e-09},
                {0.001856, 0.071827, 0.873, 0.69005, 0.4018, 0.28011, 0.17372, 0.052759, 3.0742e-05, 9.5446e-08, 1.4623e-09},
                {0.00026189, 0.0038935, 0.022522, 0.017879, 0.022364, 0.067843, 0.051255, 0.00049774, 2.0452e-06, 2.2811e-08, 5.3425e-10},
                {2.0589e-05, 0.0001397, 0.00039958, 0.00040321, 0.00035992, 0.00029793, 8.2334e-05, 3.8098e-06, 1.1051e-07, 3.6026e-09, 1.1304e-10},
                {1.1473e-06, 6.3642e-06, 2.0625e-05, 3.4001e-05, 2.5305e-05, 8.2007e-06, 1.2745e-06, 1.1776e-07, 7.9621e-09, 4.3228e-10, 0}
        };
        final double expectedMLEin = 1.5824;
        final double expectedMLEout = 4.9797e-05;
        
        SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament, 
                                                             NoiseType.POISSON, 
                                                             PsfType.NONE, 
                                                             new Dimension(1, 1), 
                                /* subpixel sumpling */      1, 
                                /* scale */                  0, 
                                /* regularizer term */       0.0001,
                                /* no of iterations */       2);
        
        // Tested function
        EnergyOutput result = sa.minimizeEnergy();
        
        double epsilon = 0.0005;
        assertTrue("Mask", new Matrix(expectedMask).compare(result.iMask, epsilon));
        assertEquals("MLEin", expectedMLEin, result.iRss.getBetaMLEin(), expectedMLEin/1000);
        assertEquals("MLEout", expectedMLEout, result.iRss.getBetaMLEout(), expectedMLEout/1000);
    }
    
    @Test
    public void testEnergyMinimalizationPhaseContrast() {
        // Expected data taken from Matlab implementation
        final double[][] expectedMask = new double[][] {
                {0.0012574, 0.018805, 0.058048, 0.078214, 0.24453, 0.059303, 0.0040948, 5.1499e-05, 7.4258e-07, 2.2028e-08, 1.1164e-09},
                {0.0042905, 0.047962, 0.14078, 0.35549, 0.70792, 0.14933, 0.022423, 0.00015753, 2.4228e-06, 6.4778e-08, 2.0537e-09},
                {0.0072685, 0.056632, 0.20871, 1, 0.1853, 0.089456, 0.092038, 0.0012018, 1.0064e-05, 1.2933e-07, 2.208e-09},
                {0.0035364, 0.048778, 0.20141, 0.96579, 0.15583, 0.049161, 0.047812, 0.00053588, 4.943e-06, 7.1299e-08, 1.3286e-09},
                {0.00024194, 0.0037146, 0.036377, 0.12608, 0.058528, 0.0067124, 0.0021934, 2.8212e-05, 5.0092e-07, 1.3511e-08, 4.5773e-10},
                {9.9742e-06, 8.3335e-05, 0.00065097, 0.0017016, 0.00085838, 0.00026324, 3.4532e-05, 1.2536e-06, 4.4746e-08, 2.0475e-09, 9.4118e-11},
                {7.4777e-07, 4.6069e-06, 2.087e-05, 3.3698e-05, 1.6726e-05, 5.5526e-06, 1.1589e-06, 1.0339e-07, 6.0908e-09, 3.3311e-10, 0}
        };
        final double expectedMLEin = 0.74455;
        final double expectedMLEout = 0.19467;
        
        SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament, 
                                                             NoiseType.POISSON, 
                                                             PsfType.PHASE_CONTRAST, 
                                                             new Dimension(1, 1), 
                                /* subpixel sumpling */      1, 
                                /* scale */                  0, 
                                /* regularizer term */       0.0001,
                                /* no of iterations */       1);
        
        // Tested function
        EnergyOutput result = sa.minimizeEnergy();
        
        double epsilon = 0.00001;
        
        assertTrue("Mask", new Matrix(expectedMask).compare(result.iMask, epsilon));
        assertEquals("MLEin", expectedMLEin, result.iRss.getBetaMLEin(), expectedMLEin/10000);
        assertEquals("MLEout", expectedMLEout, result.iRss.getBetaMLEout(), expectedMLEout/10000);
    }
    
    @Test
    public void testThreshold() {
     // Expected data taken from Matlab implementation
        final double[][] expectedMask = new double[][] {
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        };
        final double[][] expectedThresholdedMask = new double[][] {
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.353949199487381, 0.963959531864304, 0.970296763398045, 0.707563984435845, 0.339427117734516, 0, 0, 0},
                {0, 0, 0.853581652479588, 0.875996833679905, 0.717756931916987, 0.962762649768139, 1.000000000000000, 0.484340607708402, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        };
        
        SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament, 
                                                             NoiseType.GAUSSIAN, 
                                                             PsfType.GAUSSIAN, 
                                                             new Dimension(1,1), 
                                /* subpixel sumpling */      1, 
                                /* scale */                  0, 
                                /* regularizer term */       0.0001,
                                                             5);
        
        EnergyOutput resultOfEnergyMinimalization = sa.minimizeEnergy();
        
        // Tested method
        ThresholdFuzzyOutput resultOfThresholding = sa.ThresholdFuzzyVLS(resultOfEnergyMinimalization.iTotalEnergy);
        
        double epsilon = 0.0000025;
        assertTrue("Mask", new Matrix(expectedMask).compare(resultOfThresholding.iopt_MK, epsilon));
        assertTrue("Mask", new Matrix(expectedThresholdedMask).compare(resultOfThresholding.iH_f, epsilon));
    }
    
    @Test
    public void testPostprocess() {
     // Expected data taken from Matlab implementation

        
        SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament, 
                                                             NoiseType.GAUSSIAN, 
                                                             PsfType.GAUSSIAN, 
                                                             new Dimension(1,1), 
                                /* subpixel sumpling */      1, 
                                /* scale */                  0, 
                                /* regularizer term */       0.0001,
                                                             5);
        
        EnergyOutput resultOfEnergyMinimalization = sa.minimizeEnergy();
        ThresholdFuzzyOutput resultOfThresholding = sa.ThresholdFuzzyVLS(resultOfEnergyMinimalization.iTotalEnergy);
        
        // Tested method
        sa.generateFilamentInfo(resultOfThresholding);
        
        
    }
}
