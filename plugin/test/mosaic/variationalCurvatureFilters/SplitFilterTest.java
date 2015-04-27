package mosaic.variationalCurvatureFilters;

import mosaic.test.framework.CommonBase;

import org.junit.Assert;
import org.junit.Test;

public class SplitFilterTest extends CommonBase {
    /**
     * Every time when filterKernel method is called 
     * it returns increasing integer number (starting from 1)
     */
    class IncreasingValueFilter implements FilterKernel {
        int startVal = 0;
        
        @Override
        public float filterKernel(float lu, float u, float ru, float l,
                float m, float r, float ld, float d, float rd) {
            return ++startVal;
        }
        
    }
    
    /** 
     * Test if pixels are processed in correct order.
     * Sequence:
     * col | row | set corresponding to split filter
     * ---------------------------------------------
     * 1     1     BC
     * 1     2     WT
     * 2     1     WC
     * 2     2     BT
     * Naming of subsets (BC, WT..) is taken from split version of filter.
     */
    @Test
    public void testOrderOfUpdatingPixels() {
        final float expectedPrecision = 0.000001f;
        final float[][] expectedOutput = {{0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                                          {0.0f, 1.0f, 7.0f, 2.0f, 0.0f},
                                          {0.0f, 5.0f, 9.0f, 6.0f, 0.0f},
                                          {0.0f, 3.0f, 8.0f, 4.0f, 0.0f},
                                          {0.0f, 0.0f, 0.0f, 0.0f, 0.0f}};
        final int yLen = expectedOutput.length;
        final int xLen = expectedOutput[0].length;
        float[][] img = new float[yLen][xLen];
        
        NoSplitFilter nsf = new NoSplitFilter(new IncreasingValueFilter());
        nsf.runFilter(img, 1);
        
        for (int y = 0; y < yLen; ++y) {
            Assert.assertArrayEquals("Arrays should have same values!",
                    expectedOutput[y], img[y], expectedPrecision);
        }
    }
    
    /**
     * Check if more than one iteration is correctly handled.
     */
    @Test
    public void testIncrements() {
        final float expectedPrecision = 0.000001f;
        final int noOfIncrements = 2;
        final float[][] expectedOutput = {{0.0f, 0.0f, 0.0f, 0.0f},
                                          {0.0f, 6.0f, 10.0f, 0.0f},
                                          {0.0f, 8.0f, 12.0f, 0.0f},
                                          {0.0f, 0.0f, 0.0f, 0.0f}};
        final int yLen = expectedOutput.length;
        final int xLen = expectedOutput[0].length;
        float[][] img = new float[yLen][xLen];
        
        SplitFilter nsf = new SplitFilter(new IncreasingValueFilter());
        nsf.runFilter(img, noOfIncrements);
        
        for (int y = 0; y < expectedOutput.length; ++y) {
            Assert.assertArrayEquals("Arrays should have same values!",
                    expectedOutput[y], img[y], expectedPrecision);
        }
    }

    /**
     * Split Filter should not support mask feature.
     */
    @Test (expected = RuntimeException.class)
    public void testMask() {
        final int noOfIncrements = 1;
        final float[][] expectedOutput = {{0.0f, 0.0f, 0.0f, 0.0f},
                                          {0.0f, 1.0f, 2.0f, 0.0f},
                                          {0.0f, 0.0f, 0.0f, 0.0f},
                                          {0.0f, 0.0f, 0.0f, 0.0f}};
        final int yLen = expectedOutput.length;
        final int xLen = expectedOutput[0].length;
        float[][] img = new float[yLen][xLen];
        
        SplitFilter nsf = new SplitFilter(new IncreasingValueFilter());
        
        // Should throw RuntimeException
        nsf.runFilter(img, noOfIncrements, new CurvatureFilter.Mask() {
            public boolean shouldBeProcessed(int x, int y) {
                // Allow to update pixels only for rows: 0, 1
                return y <= 1;
            }
        });
    }
}
