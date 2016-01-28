package mosaic.bregman;


import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;


class MasksDisplay {
    private final int iWidth, iHeigth, iDepth;

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
}
