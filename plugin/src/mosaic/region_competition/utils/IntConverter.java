package mosaic.region_competition.utils;


import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import mosaic.utils.ConvertArray;

import java.util.Arrays;


/**
 * Raw-converts {@link ImagePlus}, {@link ImageProcessor} and Arrays to
 * int-versions
 */
public class IntConverter {

    public static ImagePlus IPtoInt(ImagePlus ip) {

        final ImageStack stack = ip.getStack();
        final int nSlices = stack.getSize();

        final ImageStack newStack = new ImageStack(ip.getWidth(), ip.getHeight());

        final ImagePlus newIP = new ImagePlus();
        for (int i = 1; i <= nSlices; i++) {
            final ImageProcessor proc = stack.getProcessor(i);
            final ColorProcessor newProc = procToIntProc(proc);
            newStack.addSlice(stack.getSliceLabel(i), newProc);
        }
        newIP.setStack(newStack);
        newIP.setDimensions(ip.getNChannels(), ip.getNSlices(), ip.getNFrames());

        return newIP;
    }

    /**
     * @param proc
     * @return copy or converted proc
     */
    private static ColorProcessor procToIntProc(ImageProcessor proc) {
        int[] intArray = getIntArray(proc);

        final ColorProcessor newProc = new ColorProcessor(proc.getWidth(), proc.getHeight());
        newProc.setPixels(intArray);

        return newProc;
    }

    public static int[] getIntArray(ImageProcessor aImgProcessor) {
        final Object pixels = aImgProcessor.getPixels();

        int[] intArray = null;
        if (pixels instanceof int[]) {
            intArray = ((int[]) pixels).clone();
        }
        else {
            intArray = arrayToInt(pixels);
        }
        return intArray;
    }

    public static int[] intStackToArray(ImageStack stack) {
        int[] result;
    
        final int zs = stack.getSize();
        final int area = stack.getWidth() * stack.getHeight();
        result = new int[zs * area];
    
        for (int z = 0; z < zs; z++) {
            final int[] pixels = (int[]) stack.getPixels(z + 1);
            for (int j = 0; j < area; j++) {
                result[z * area + j] = pixels[j];
            }
        }
    
        return result;
    }

    public static int[] toIntArray(ImagePlus imagePlus) {
        final ImagePlus ip = IntConverter.IPtoInt(imagePlus);
        return IntConverter.intStackToArray(ip.getImageStack());
    }
    
    public static ImageStack intArrayToShortStack(int[] intData, int w, int h, int z, boolean clean) {
        return intArrayToShortStack(intData, w, h, z, clean, clean, clean);
    }

    /**
     * Int array to short image conversion
     */
    private static ImageStack intArrayToShortStack(int[] intData, int w, int h, int z, boolean abs, boolean borderRemove, boolean clamp) {
        final short shortData[] = IntConverter.intToShort(intData, abs, borderRemove, clamp);
        final int area = w * h;

        final ImageStack stack = new ImageStack(w, h);
        for (int i = 0; i < z; i++) {
            final Object pixels = Arrays.copyOfRange(shortData, i * area, (i + 1) * area);
            stack.addSlice("", pixels);
        }

        return stack;

    }

    /**
     * Converts float/byte/short array into int array <br>
     *
     * @param array float/byte/short array (NO int!)
     * @return converted array
     */
    private static int[] arrayToInt(Object array) {
        int[] intArray = null;

        if (array instanceof float[]) {
            intArray = ConvertArray.toInt((float[]) array);
        }
        else if (array instanceof byte[]) {
            intArray = ConvertArray.toInt((byte[]) array);
        }
        else if (array instanceof short[]) {
            intArray = ConvertArray.toInt((short[]) array);
        }
        else {
            throw new RuntimeException("not Supported conversion");
        }

        return intArray;
    }

    public static short[] intToShort(int[] proc) {
        return intToShort(proc, false, false, true);
    }

    /**
     * @param ints int[] array
     * @param abs Math.abs() the array
     * @param borderRemove Short.MAX_VALUE to Zero
     * @param clamp Values > Short.MAX_VALUE to Short.MAX_VALUE (same for
     *            MIN_VALUE)
     * @return
     */
    private static short[] intToShort(int[] ints, boolean abs, boolean borderRemove, boolean clamp) {
        final int n = ints.length;
        final short[] shorts = new short[n];

        if (abs) {
            for (int i = 0; i < n; i++) {
                int a = Math.abs(ints[i]);
                if (clamp) {
                    if (a > Short.MAX_VALUE) {
                        a = Short.MAX_VALUE;
                    }
                }
                shorts[i] = (short) a;
            }
        }
        else {
            for (int i = 0; i < n; i++) {
                int a = ints[i];
                if (clamp) {
                    if (a > Short.MAX_VALUE) {
                        a = Short.MAX_VALUE;
                    }
                    else if (a < Short.MIN_VALUE) {
                        a = Short.MIN_VALUE;
                    }
                }
                shorts[i] = (short) a;
            }
        }

        if (borderRemove) {
            for (int i = 0; i < n; i++) {
                short a = shorts[i];
                if (a == Short.MAX_VALUE) {
                    a = 0;
                }
                shorts[i] = a;
            }
        }

        return shorts;
    }

}
