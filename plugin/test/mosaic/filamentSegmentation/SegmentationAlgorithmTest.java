package mosaic.filamentSegmentation;

import static org.junit.Assert.*;

import java.awt.Dimension;

import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.math.Matrix;

import org.junit.Test;

public class SegmentationAlgorithmTest {

    // 7 x 11, 1 filament, without noise
    final static double[][] simpleImage1filament = new double[][] {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 
                                                                   {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 
                                                                   {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0}, 
                                                                   {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0}, 
                                                                   {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 
                                                                   {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 
                                                                   {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    @Test
    public void test() {
        SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament, 
                                                             NoiseType.GAUSSIAN, 
                                                             PsfType.GAUSSIAN, 
                                                             new Dimension(2,2), 
                                /* subpixel sumpling */      1, 
                                /* scale */                  1, 
                                /* regularizer term */       0.0001);
            
        sa.performSegmentation();
    }
    
}
