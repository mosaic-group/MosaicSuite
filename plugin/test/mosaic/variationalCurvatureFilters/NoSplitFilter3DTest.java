package mosaic.variationalCurvatureFilters;

import org.junit.Assert;
import org.junit.Test;

public class NoSplitFilter3DTest {

    /**
     * Every time when filterKernel method is called
     * it returns increasing integer number (starting from 1)
     */
    class IncreasingValueFilter implements FilterKernel3D {
        int startVal = 0;

        @Override
        public float filterKernel(float[][][] aImage, int aX, int aY, int aZ) {
            return ++startVal;
        }

    }

    @Test
    public void testOrderOfUpdatingPixels() {
        final float expectedPrecision = 0.000001f;
        final int noOfIncrements = 1;
        final float[][][] expectedOutput = {
                {{0.0f, 0.0f, 0.0f, 0.0f},
                    {0.0f, 0.0f, 0.0f, 0.0f},
                    {0.0f, 0.0f, 0.0f, 0.0f},
                    {0.0f, 0.0f, 0.0f, 0.0f}},

                    {{0.0f, 0.0f, 0.0f, 0.0f},
                        {0.0f, 1.0f, 6.0f, 0.0f},
                        {0.0f, 5.0f, 2.0f, 0.0f},
                        {0.0f, 0.0f, 0.0f, 0.0f}},

                        {{0.0f, 0.0f, 0.0f, 0.0f},
                            {0.0f, 7.0f, 3.0f, 0.0f},
                            {0.0f, 4.0f, 8.0f, 0.0f},
                            {0.0f, 0.0f, 0.0f, 0.0f}},

                            {{0.0f, 0.0f, 0.0f, 0.0f},
                                {0.0f, 0.0f, 0.0f, 0.0f},
                                {0.0f, 0.0f, 0.0f, 0.0f},
                                {0.0f, 0.0f, 0.0f, 0.0f}}
        };

        final int zLen = expectedOutput.length;
        final int yLen = expectedOutput[0].length;
        final int xLen = expectedOutput[0][0].length;

        float[][][] img = new float[zLen][yLen][xLen];

        NoSplitFilter3D nsf = new NoSplitFilter3D(new IncreasingValueFilter());
        nsf.runFilter(img, noOfIncrements);

        for (int z = 0; z < zLen; ++z) {
            for (int y = 0; y < yLen; ++y) {
                Assert.assertArrayEquals("Arrays should have same values!",
                        expectedOutput[z][y], img[z][y], expectedPrecision);
            }
        }
    }

    /**
     * Check if more than one iteration is correctly handled.
     */
     @Test
     public void testIncrements() {
         final float expectedPrecision = 0.000001f;
         final int noOfIncrements = 2;
         final float[][][] expectedOutput = {
                 {{0.0f, 0.0f, 0.0f, 0.0f},
                     {0.0f, 0.0f, 0.0f, 0.0f},
                     {0.0f, 0.0f, 0.0f, 0.0f},
                     {0.0f, 0.0f, 0.0f, 0.0f}},

                     {{0.0f, 0.0f, 0.0f, 0.0f},
                         {0.0f, 10.0f, 20.0f, 0.0f},
                         {0.0f, 18.0f, 12.0f, 0.0f},
                         {0.0f, 0.0f, 0.0f, 0.0f}},

                         {{0.0f, 0.0f, 0.0f, 0.0f},
                             {0.0f, 22.0f, 14.0f, 0.0f},
                             {0.0f, 16.0f, 24.0f, 0.0f},
                             {0.0f, 0.0f, 0.0f, 0.0f}},

                             {{0.0f, 0.0f, 0.0f, 0.0f},
                                 {0.0f, 0.0f, 0.0f, 0.0f},
                                 {0.0f, 0.0f, 0.0f, 0.0f},
                                 {0.0f, 0.0f, 0.0f, 0.0f}}
         };

         final int zLen = expectedOutput.length;
         final int yLen = expectedOutput[0].length;
         final int xLen = expectedOutput[0][0].length;

         float[][][] img = new float[zLen][yLen][xLen];

         NoSplitFilter3D nsf = new NoSplitFilter3D(new IncreasingValueFilter());
         nsf.runFilter(img, noOfIncrements);

         for (int z = 0; z < zLen; ++z) {
             for (int y = 0; y < yLen; ++y) {
                 Assert.assertArrayEquals("Arrays should have same values!",
                         expectedOutput[z][y], img[z][y], expectedPrecision);
             }
         }
     }
}
