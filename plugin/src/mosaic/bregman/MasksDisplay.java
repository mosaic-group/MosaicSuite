package mosaic.bregman;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.core.utils.MosaicUtils;


class MasksDisplay {

    private ImageStack imgcolocastack;

    private final ImagePlus imgcoloc;
    private final int[][] colors;
    private final ColorProcessor cp;
    private final ImagePlus img;
    private final int ni, nj, nz, nlevels;
    boolean firstdisp = true;
    private boolean firstdispa = true;
    private boolean firstdispb = true;
    private final Parameters p;

    private ImageStack ims3d;
    private final ImagePlus imgda, imgdb;

    MasksDisplay(int ni, int nj, int nz, int nlevels, double[] cl, Parameters params) {
        this.imgda = new ImagePlus();
        this.imgdb = new ImagePlus();
        this.ni = ni;
        this.nj = nj;
        this.nlevels = nlevels;
        this.nz = nz;
        this.p = params;
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

        if (!p.displowlevels) {
            colors[0][1] = 0;
            colors[0][0] = 0;
            colors[0][2] = 0;
            colors[1][1] = 0;
            colors[1][0] = 0;
            colors[1][2] = 0;
        }
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
     * @param vs visualize or not the image
     * @return the imagePlus
     */
    ImagePlus display2regionsnew(double[][] array, String s, int channel, boolean vs) {

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

        if (vs == true) {
            imgtemp.show();
        }
        imgtemp.changes = false;
        return imgtemp;
    }

    /**
     * Display the soft membership
     *
     * @param array 3D array of double
     * @param s String of the image
     * @param channel channel
     * @param vs Visualize or not the soft mask
     * @return the imagePlus
     */
    ImagePlus display2regions3Dnew(double[][][] array, String s, int channel, boolean vs) {
        final ImageStack img3temp = new ImageStack(ni, nj);

        final ImagePlus imgtemp = new ImagePlus();

        for (int z = 0; z < nz; z++) {
            final float[][] temp = new float[ni][nj];

            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    temp[i][j] = (float) array[z][i][j];
                }
            }
            final ImageProcessor imp = new FloatProcessor(temp);
            img3temp.addSlice("", imp);
        }

        if (channel == 0) {
            imgtemp.setStack(s + "X", img3temp);
        }
        else {
            imgtemp.setStack(s + "Y", img3temp);
        }

        if (vs == true) {
            imgtemp.show();
        }
        imgtemp.changes = false;
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

        this.ims3d = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            final byte[] temp = new byte[ni * nj];

            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    temp[j * p.ni + i] = (byte) ((int) (255 * array[z][i][j]));// (float)
                }
            }
            final ImageProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(temp);
            this.ims3d.addSlice("", bp);
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

        for (int z = 0; z < nz; z++) {
            final byte[] temp = new byte[ni * nj];

            for (int j = 0; j < nj; j++) {
                for (int i = 0; i < ni; i++) {
                    temp[j * p.ni + i] = (byte) ((int) (array[z][i][j]));// (float)
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

        // set all to zero
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                final int t = z * ni * nj * 3 + i * nj * 3;
                for (int j = 0; j < nj; j++) {
                    imagecolor[t + j * 3 + 0] = 0;// Red channel
                    imagecolor[t + j * 3 + 1] = 0;// Green channel
                    imagecolor[t + j * 3 + 2] = 0;// Blue channel
                }
            }
        }

        // set green pixels
        imagecolor[1] = (byte) 255;
        for (final Iterator<Region> it = regionslistA.iterator(); it.hasNext();) {
            final Region r = it.next();

            for (final Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
                final Pix p = it2.next();
                final int t = p.pz * ni * nj * 3 + p.px * nj * 3;
                imagecolor[t + p.py * 3 + 1] = (byte) 255;
                // green
            }
        }

        // set red pixels
        for (final Iterator<Region> it = regionslistB.iterator(); it.hasNext();) {
            final Region r = it.next();

            for (final Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
                final Pix p = it2.next();
                final int t = p.pz * ni * nj * 3 + p.px * nj * 3;
                imagecolor[t + p.py * 3 + 0] = (byte) 255;
            }
        }

        final int[] tabt = new int[3];

        this.imgcolocastack = new ImageStack(ni, nj);
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
            this.imgcolocastack.addSlice("Colocalization", cpcoloc);

        }
        this.imgcoloc.setStack("Colocalization", imgcolocastack);

        ip.add(this.imgcoloc);

        if (Analysis.p.dispwindows) {
            this.imgcoloc.show();
        }

        if (Analysis.p.save_images) {
            IJ.run(this.imgcoloc, "RGB Color", "");

            savepath = MosaicUtils.removeExtension(savepath);
            savepath = savepath + "_coloc" + ".zip";
            IJ.saveAs(this.imgcoloc, "ZIP", savepath);
        }
    }
}
