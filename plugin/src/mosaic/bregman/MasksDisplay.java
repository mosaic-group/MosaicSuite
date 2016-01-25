package mosaic.bregman;


import java.util.ArrayList;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


class MasksDisplay {
    private final int iWidth, iHeigth, iDepth;

    private final ImagePlus iColocImg = new ImagePlus();
    private final ImagePlus iAImg = new ImagePlus(); 
    private final ImagePlus iBImg = new ImagePlus();

    MasksDisplay(int ni, int nj, int nz) {
        iWidth = ni;
        iHeigth = nj;
        iDepth = nz;
    }

    /**
     * Display the soft membership
     *
     * @param aImgArray 3D array with image data [z][x][y]
     * @param aTitle Title of the image.
     * @return the generated ImagePlus
     */
    ImagePlus generateImgFromArray(double[][][] aImgArray, String aTitle) {
        final ImageStack stack = new ImageStack(iWidth, iHeigth);
    
        for (int z = 0; z < iDepth; z++) {
            final float[][] pixels = new float[iWidth][iHeigth];
            for (int i = 0; i < iWidth; i++) {
                for (int j = 0; j < iHeigth; j++) {
                    pixels[i][j] = (float) aImgArray[z][i][j];
                }
            }
            stack.addSlice("", new FloatProcessor(pixels));
        }
    
        final ImagePlus img = new ImagePlus(aTitle, stack);
        img.changes = false;
        
        return img;
    }

    /**
     * Display the soft membership
     *
     * @param array 3D array of double
     * @param s String of the image
     * @param channel channel
     */
    void display2regions(double[][][] array, String s, int channel) {
        final ImageStack ims = convertArrayToImageProcessor(array);

        if (channel == 0) {
            iAImg.setStack(s + " X", ims);
            iAImg.resetDisplayRange();
            iAImg.show();
        }
        else {
            iBImg.setStack(s + " Y", ims);
            iBImg.resetDisplayRange();
            iBImg.show();
        }
    }

    private ImageStack convertArrayToImageProcessor(double[][][] array) {
        final ImageStack ims = new ImageStack(iWidth, iHeigth);
        for (int z = 0; z < iDepth; z++) {
            final byte[] temp = new byte[iWidth * iHeigth];
            for (int j = 0; j < iHeigth; j++) {
                for (int i = 0; i < iWidth; i++) {
                    temp[j * iWidth + i] = (byte) ((int) (255 * array[z][i][j]));
                }
            }
            final ImageProcessor bp = new ByteProcessor(iWidth, iHeigth);
            bp.setPixels(temp);
            ims.addSlice("", bp);
        }
        return ims;
    }

    /**
     * Produces the colocalization image
     * @param aRegionsListA Regions list A
     * @param aRegionsListB Regions list B
     */
    ImagePlus generateColocImg(ArrayList<Region> aRegionsListA, ArrayList<Region> aRegionsListB) {
        final byte[] imagecolor = new byte[iDepth * iWidth * iHeigth * 3];

        // set green pixels
        for (final Region r : aRegionsListA) {
            for (final Pix p : r.pixels) {
                final int t = p.pz * iWidth * iHeigth * 3 + p.px * iHeigth * 3;
                imagecolor[t + p.py * 3 + 1] = (byte) 255;
            }
        }

        // set red pixels
        for (final Region r : aRegionsListB) {
            for (final Pix p : r.pixels) {
                final int t = p.pz * iWidth * iHeigth * 3 + p.px * iHeigth * 3;
                imagecolor[t + p.py * 3 + 0] = (byte) 255;
            }
        }

        // Merge them into one Color image
        final int[] tabt = new int[3];
        ImageStack imgcolocastack = new ImageStack(iWidth, iHeigth);
        for (int z = 0; z < iDepth; z++) {
            final ColorProcessor colorProc = new ColorProcessor(iWidth, iHeigth);
            for (int i = 0; i < iWidth; i++) {
                final int t = z * iWidth * iHeigth * 3 + i * iHeigth * 3;
                for (int j = 0; j < iHeigth; j++) {
                    tabt[0] = imagecolor[t + j * 3 + 0] & 0xFF;
                    tabt[1] = imagecolor[t + j * 3 + 1] & 0xFF;
                    tabt[2] = imagecolor[t + j * 3 + 2] & 0xFF;
                    colorProc.putPixel(i, j, tabt);
                }
            }
            imgcolocastack.addSlice("Colocalization", colorProc);
        }
        iColocImg.setStack("Colocalization", imgcolocastack);
        
        return iColocImg;
    }
}
