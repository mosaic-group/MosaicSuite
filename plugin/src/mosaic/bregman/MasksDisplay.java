package mosaic.bregman;


import java.util.ArrayList;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.core.utils.MosaicUtils;
import mosaic.utils.Debug;


class MasksDisplay {

    private final ImagePlus imgcoloc;
    private final int[][] colors;
    private final ColorProcessor cp;
    private final ImagePlus img;
    private final int ni, nj, nz, nlevels;
    boolean firstdisp = true;
    private boolean firstdispa = true;
    private boolean firstdispb = true;
    private final Parameters parameters;
    private final ImagePlus imgda, imgdb;

    MasksDisplay(int ni, int nj, int nz, int nlevels, double[] cl, Parameters params) {
        this.imgda = new ImagePlus();
        this.imgdb = new ImagePlus();
        this.ni = ni;
        this.nj = nj;
        this.nlevels = nlevels;
        this.nz = nz;
        this.parameters = params;
        this.colors = new int[this.nlevels][3];
        this.cp = new ColorProcessor(ni, nj);
        this.img = new ImagePlus();

        this.imgcoloc = new ImagePlus();

        // heatmap R=x, G=sqrt(x), B=x**2 on kmeans found intensities
        for (int l = 0; l < this.nlevels; l++) {
            colors[l][1] = (int) Math.min(255, 255 * Math.sqrt(cl[l])); // Green
            colors[l][0] = (int) Math.min(255, 255 * cl[l]); // Red
            colors[l][2] = (int) Math.min(255, 255 * Math.pow(cl[l], 2)); // Blue
        }
    }

    /**
     * Display the soft membership
     *
     * @param aImgArray 3D array with image data [z][x][y]
     * @param aTitle Title of the image.
     * @return the generated ImagePlus
     */
    ImagePlus generateImgFromArray(double[][][] aImgArray, String aTitle) {
        final ImageStack stack = new ImageStack(ni, nj);
    
        for (int z = 0; z < nz; z++) {
            final float[][] pixels = new float[ni][nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    pixels[i][j] = (float) aImgArray[z][i][j];
                }
            }
            stack.addSlice("", new FloatProcessor(pixels));
        }
    
        final ImagePlus img = new ImagePlus(aTitle, stack);
        img.changes = false;
        
        return img;
    }

    void display(int[][][] maxmask, String s) {
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                cp.putPixel(i, j, colors[maxmask[0][i][j]]);
            }
        }
        img.setProcessor(s, cp);
        if (firstdisp) {
            img.show();
            firstdisp = false;
        }
    }

    /**
     * Display the soft membership
     *
     * @param array 2D array of double
     * @param s String of the image
     * @param channel channel
     */
    void display2regions(double[][] array, String s, int channel) {

        final float[][] temp = new float[ni][nj];

        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                temp[i][j] = (float) array[i][j];
            }
        }

        final ImageProcessor imp = new FloatProcessor(temp);
        if (channel == 0) {
            imgda.setProcessor(s + " X", imp);
            if (firstdispa) {
                imgda.show();
                firstdispa = false;
            }
            imgda.changes = false;
        }
        else {
            imgdb.setProcessor(s + " Y", imp);
            if (firstdispb) {
                imgdb.show();
                firstdispb = false;
            }
            imgdb.changes = false;
        }
    }

    /**
     * Display the soft membership
     *
     * @param array 2D array of float
     * @param s String of the image
     * @param channel channel
     * @param vs visualize or not
     * @return ImagePlus image
     */
    ImagePlus display2regionsnew(float[][] array, String s, int channel, boolean vs) {

        final float[][] temp = new float[ni][nj];
        final ImagePlus imgtemp = new ImagePlus();

        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                temp[i][j] = array[i][j];
            }
        }

        final ImageProcessor imp = new FloatProcessor(temp);
        if (channel == 0) {
            imgtemp.setProcessor(s + "X", imp);
        }
        else {
            imgtemp.setProcessor(s + "Y", imp);
        }
        if (vs == true) {
            imgtemp.show();
        }
        return imgtemp;
    }

    /**
     * Display the soft membership
     *
     * @param array 2D array of double
     * @param s String of the image
     * @param channel channel
     * @return the imagePlus
     */
    ImagePlus display2regionsnewd(double[][] array, String s, int channel) {

        final float[][] temp = new float[ni][nj];
        final ImagePlus imgtemp = new ImagePlus();

        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                temp[i][j] = (float) array[i][j];
            }
        }

        final ImageProcessor imp = new FloatProcessor(temp);
        if (channel == 0) {
            imgtemp.setProcessor(s + "X", imp);
        }
        else {
            imgtemp.setProcessor(s + "Y", imp);
        }
        imgtemp.show();
        return imgtemp;
    }

    /**
     * Display the soft membership
     *
     * @param array 3D array of double
     * @param s String of the image
     * @param channel channel
     */
    void display2regions3D(double[][][] array, String s, int channel) {

        Debug.print("display2regions3D", ni, nj, parameters.ni, parameters.nj);
        ImageStack ims3d = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            final byte[] temp = new byte[ni * nj];

            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    temp[j * parameters.ni + i] = (byte) ((int) (255 * array[z][i][j]));
                }
            }
            final ImageProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(temp);
            ims3d.addSlice("", bp);
        }

        if (channel == 0) {
            imgda.setStack(s + " X", ims3d);
            imgda.resetDisplayRange();
            if (firstdispa) {
                imgda.show();
                firstdispa = false;
            }
        }
        else {
            imgdb.setStack(s + " Y", ims3d);
            imgdb.resetDisplayRange();
            if (firstdispb) {
                imgdb.show();
                firstdispb = false;
            }
        }
    }

    /**
     * Display the soft membership
     *
     * @param array 3D array of float
     * @param s String of the image
     * @param channel channel
     * @return the imagePlus
     */
    ImagePlus display2regions3Dnew(float[][][] array, String s, int channel) {

        final ImageStack ims3da = new ImageStack(ni, nj);
        final ImagePlus imgd = new ImagePlus();
        Debug.print("display2regions3Dnew", ni, nj, parameters.ni, parameters.nj);
        for (int z = 0; z < nz; z++) {
            final byte[] temp = new byte[ni * nj];

            for (int j = 0; j < nj; j++) {
                for (int i = 0; i < ni; i++) {
                    temp[j * parameters.ni + i] = (byte) ((int) (array[z][i][j]));// (float)
                }
            }
            final ImageProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(temp);
            ims3da.addSlice("", bp);
        }

        if (channel == 0) {
            imgd.setStack(s + " X", ims3da);
        }
        else {
            imgd.setStack(s + " Y", ims3da);
        }
        imgd.resetDisplayRange();
        imgd.show();

        return imgd;
    }

    /**
     * Display the colocalization image
     *
     * @param savepath path + filename "_coloc.zip" is appended to the name, extension is removed
     * @param regionslistA Regions list A
     * @param regionslistB Regions list B
     */
    void displaycoloc(String savepath, ArrayList<Region> regionslistA, ArrayList<Region> regionslistB, Vector<ImagePlus> ip) {

        final byte[] imagecolor = new byte[nz * ni * nj * 3];

        // set green pixels
        imagecolor[1] = (byte) 255;
        for (final Region r : regionslistA) {
            for (final Pix p : r.pixels) {
                final int t = p.pz * ni * nj * 3 + p.px * nj * 3;
                imagecolor[t + p.py * 3 + 1] = (byte) 255;
                // green
            }
        }

        // set red pixels
        for (final Region r : regionslistB) {
            for (final Pix p : r.pixels) {
                final int t = p.pz * ni * nj * 3 + p.px * nj * 3;
                imagecolor[t + p.py * 3 + 0] = (byte) 255;
            }
        }

        final int[] tabt = new int[3];

        ImageStack imgcolocastack = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            final ColorProcessor cpcoloc = new ColorProcessor(ni, nj);
            for (int i = 0; i < ni; i++) {
                final int t = z * ni * nj * 3 + i * nj * 3;
                for (int j = 0; j < nj; j++) {
                    tabt[0] = imagecolor[t + j * 3 + 0] & 0xFF;
                    tabt[1] = imagecolor[t + j * 3 + 1] & 0xFF;
                    tabt[2] = imagecolor[t + j * 3 + 2] & 0xFF;
                    cpcoloc.putPixel(i, j, tabt);
                }
            }
            imgcolocastack.addSlice("Colocalization", cpcoloc);

        }
        this.imgcoloc.setStack("Colocalization", imgcolocastack);

        ip.add(this.imgcoloc);

        if (Analysis.p.dispwindows) {
            this.imgcoloc.show();
        }

        if (Analysis.p.save_images) {
            IJ.run(this.imgcoloc, "RGB Color", "");

            savepath = MosaicUtils.removeExtension(savepath) + "_coloc.zip";
            IJ.saveAs(this.imgcoloc, "ZIP", savepath);
        }
    }
}
