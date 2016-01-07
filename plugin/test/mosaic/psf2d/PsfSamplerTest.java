package mosaic.psf2d;

import org.junit.Assert;
import org.junit.Test;

import ij.process.ByteProcessor;


public class PsfSamplerTest {

    @Test
    public void testBlackBox() {
        /*
         * This test just check current behavior of PsfPointSpreadFunction. Output is check as "it is" and 
         * it is not validated if this is corrected or not. Main purpose of this test is to help to refactor code
         * and prevent from introducing unwanted changes. 
         */
        final byte[] originalImgArray2 = {  0, 0, 0, 0, 0,
                                            0, 0, 0, 0, 0,
                                            0, 1, 8, 1, 0,
                                            0, 8, 61,8, 0,
                                            0, 1, 8, 1, 0,
                                            0, 0, 0, 0, 0,
                                            0, 0, 0, 0, 0};
        final ByteProcessor fp = new ByteProcessor(5, 7, originalImgArray2);
        
        PsfSampler psf = new PsfSampler(fp, new PsfSourcePosition(2, 3), 2, 8, 3, 1, 1);
        float[] result = psf.getPsf();

        float[] expectedOutput = {1.0f, 0.81014556f, 0.43398404f, 0.13919266f, 0.027169045f, 0.0049144095f, 0.0f};
        Assert.assertArrayEquals(expectedOutput, result, 1e-5f);
    }
}
