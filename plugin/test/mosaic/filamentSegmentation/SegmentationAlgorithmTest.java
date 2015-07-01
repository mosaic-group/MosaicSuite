package mosaic.filamentSegmentation;

import static org.junit.Assert.*;

import java.awt.Dimension;

import mosaic.filamentSegmentation.SegmentationAlgorithm.EnergyOutput;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
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
    
    final static double[][] smallImage1filament = new double [][] {{0, 0, 0, 0, 0, 0, 0, 0},  
                                                                   {0, 0, 1, 1, 1, 1, 0, 0}, 
                                                                   {0, 0, 0, 0, 0, 0, 0, 0},
                                                                   {0, 0, 0, 0, 0, 0, 0, 0}};
    
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
    public void testEnergyMinimalization() {
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
        
        double epsilon = 0.0005;
        assertTrue("Mask", new Matrix(expectedMask).compare(result.iMask, epsilon));
        assertEquals("MLEin", expectedMLEin, result.iRss.getBetaMLEin(), epsilon);
        assertEquals("MLEout", expectedMLEout, result.iRss.getBetaMLEout(), epsilon);
    }
}
