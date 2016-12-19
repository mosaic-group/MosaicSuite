package mosaic.region_competition.DRS;

import java.util.ArrayList;
import java.util.List;

import mosaic.utils.ImgUtils;
import net.imglib2.Cursor;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;

public class SobelImg {
    /**
     * Runs Sobel operator on provided image
     * @param aImg - input image (2D or 3D)
     * @param aChangeInput - if false, new image is generated, otherwise operates in place.
     * @return processed image
     */
    public static Img<FloatType> filter(Img<FloatType> aImg, boolean aChangeInput) {
        int numDimensions = aImg.numDimensions();
        if (numDimensions < 2 || numDimensions > 3) {
            throw new RuntimeException("Unhandled number of dimensions (only 2D and 3D is possible): " + numDimensions);
        }
        
        Img<FloatType> outputImg = (aChangeInput) ? aImg : ImgUtils.createNewEmpty(aImg);
        
        // Create copy of input images
        List<Img<FloatType>> imgs = new ArrayList<Img<FloatType>>(numDimensions);
        List<Cursor<FloatType>> cursors = new ArrayList<Cursor<FloatType>>(numDimensions);
        for (int i = 0; i < numDimensions; ++i) {
            Img<FloatType> newImg = aImg.copy();
            imgs.add(newImg);
            cursors.add(newImg.cursor());
        }
        
        // Generate sobel kernels in each direction and convolve via FFT
        List<Img<FloatType>> kernels = generateKernels(numDimensions);
        for (int i = 0; i < numDimensions; ++i) {
            new FFTConvolution<FloatType>( imgs.get(i), kernels.get(i)).convolve();
        }
        
        // Calculate final Sobel filter sqrt( x1*x1 + x2*x2 + ...)
        Cursor<FloatType> o = outputImg.cursor();
        while (o.hasNext()) {
            o.fwd();
            double v = 0;
            for (int i = 0; i < numDimensions; ++i) {
                Cursor<FloatType> c = cursors.get(i);
                c.fwd();
                v += Math.pow(c.get().getRealDouble(), 2);
            }
            o.get().setReal(Math.sqrt(v));
        }
        
        // Normalize in range 0 - 1
        Normalize.normalize(outputImg, new FloatType(0.0f), new FloatType(1.0f));
        
        return outputImg;
    }

    /**
     * Generates Sobel kernels for provided number of dimensions
     * @param numDimensions - currently 2 or 3 are available
     * @return kernels in form of imglib2 images
     */
    private static List<Img<FloatType>> generateKernels(int numDimensions) {
        List<Img<FloatType>> kernels = new ArrayList<Img<FloatType>>(numDimensions);
        
        if (numDimensions == 2) {
            final int len = 3;
            
            // Default kernel for 2D
            double[][] k = new double[3][3];
            k[0][0] = -1.0/9; k[0][1] = 0; k[0][2] = 1.0/9;
            k[1][0] = -2.0/9; k[1][1] = 0; k[1][2] = 2.0/9;
            k[2][0] = -1.0/9; k[2][1] = 0; k[2][2] = 1.0/9;
            
            // Create it in form of linear array for two directions
            float[] k1 = new float[len * len];
            float[] k2 = new float[len * len];
            for (int x = 0; x < len; ++x) {
                for (int y = 0; y < len; ++y) {
                    k1[len * x + y] = (float) k[x][y];
                    k2[len * x + y] = (float) k[y][x];
                }
            }
            kernels.add(ArrayImgs.floats(k1, new long[] {len, len}));
            kernels.add(ArrayImgs.floats(k2, new long[] {len, len}));
        }
        else if (numDimensions == 3) {
            final int len = 3;
            
            // Default kernel for 3D
            double[][][] k = new double[len][len][len];
            k[0][0][0] = -2.0/27; k[0][0][1] = 0; k[0][0][2] = 2.0/27;
            k[0][1][0] = -3.0/27; k[0][1][1] = 0; k[0][1][2] = 3.0/27;
            k[0][2][0] = -2.0/27; k[0][2][1] = 0; k[0][2][2] = 2.0/27;
            k[1][0][0] = -3.0/27; k[1][0][1] = 0; k[1][0][2] = 3.0/27;
            k[1][1][0] = -6.0/27; k[1][1][1] = 0; k[1][1][2] = 6.0/27;
            k[1][2][0] = -3.0/27; k[1][2][1] = 0; k[1][2][2] = 3.0/27;
            k[2][0][0] = -2.0/27; k[2][0][1] = 0; k[2][0][2] = 2.0/27;
            k[2][1][0] = -3.0/27; k[2][1][1] = 0; k[2][1][2] = 3.0/27;
            k[2][2][0] = -2.0/27; k[2][2][1] = 0; k[2][2][2] = 2.0/27;

            // Create it in form of linear array for three directions
            float[] k1 = new float[len * len * len];
            float[] k2 = new float[len * len * len];
            float[] k3 = new float[len * len * len];
            for (int x = 0; x < len; ++x) {
                for (int y = 0; y < len; ++y) {
                    for (int z = 0; z < len; ++z) {
                        k1[len * len * x + len * y + z] = (float) k[x][y][z];
                        k2[len * len * x + len * y + z] = (float) k[x][z][y];
                        k3[len * len * x + len * y + z] = (float) k[z][y][x];
                    }
                }
            }
            kernels.add(ArrayImgs.floats(k1, new long[] {len, len, len}));
            kernels.add(ArrayImgs.floats(k2, new long[] {len, len, len}));
            kernels.add(ArrayImgs.floats(k3, new long[] {len, len, len}));
        }
        
        return kernels;
    }
}
