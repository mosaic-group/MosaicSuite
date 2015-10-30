package mosaic.bregman;


import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;


/**
 * Class to solve the N regions problems, two regions inherit from this
 * It remove the background, normalize the image, it run split bregman
 * and it can clusters the image based on intensities levels
 *
 * @author Aurelien Ritz
 */
class NRegions implements Runnable {

    final Tools LocalTools;
    // image and mask data
    public final double[][][] image;// 3D image
    public final double[][][][] mask;// nregions nslices ni nj
    final double[][][][] Ei;
    public MasksDisplay md;

    public final Parameters p;

    // properties
    // number of regions
    private final int bits;
    final int ni, nj, nz;// 3 dimensions
    final int nl;
    final int channel;
    final CountDownLatch DoneSignal;
    double min;
    double max;

    public NRegions(ImagePlus img, Parameters params, CountDownLatch DoneSignal, int channel) {
        final BackgroundSubtracter bs = new BackgroundSubtracter();
        this.p = params;

        this.LocalTools = new Tools(p.ni, p.nj, p.nz);
        this.DoneSignal = DoneSignal;
        this.nl = p.nlevels;
        ImageProcessor imp;
        final int os = p.model_oversampling;
        int osz;
        p.ni = p.ni * p.model_oversampling;
        p.nj = p.nj * p.model_oversampling;

        if (p.nz > 1) {
            osz = p.model_oversampling;
        }
        else {
            osz = 1;
        }

        p.nz = p.nz * osz;

        this.ni = p.ni;
        this.nj = p.nj;
        this.nz = p.nz;

        bits = img.getBitDepth();
        if (bits == 32) {
            IJ.log("Error converting float image to short");
        }

        this.channel = channel;

        LocalTools.setDims(ni, nj, nz, nl);

        // allocate
        image = new double[nz][ni][nj];
        mask = new double[nl][nz][ni][nj];
        if (p.nlevels > 1 || !p.usePSF) {
            Ei = new double[nl][nz][ni][nj];
        }
        else {
            Ei = null;
        }

        max = 0;
        min = Double.POSITIVE_INFINITY;
        // change : use max value instead of 65536

        /* Search for maximum and minimum value, normalization */

        if (Analysis.norm_max == 0) {
            for (int z = 0; z < nz / osz; z++) {
                img.setSlice(z + 1);
                imp = img.getProcessor();
                for (int i = 0; i < ni / os; i++) {
                    for (int j = 0; j < nj / os; j++) {
                        if (imp.getPixel(i, j) > max) {
                            max = imp.getPixel(i, j);
                        }
                        if (imp.getPixel(i, j) < min) {
                            min = imp.getPixel(i, j);
                        }
                    }
                }
            }
        }
        else {
            max = Analysis.norm_max;
            min = Analysis.norm_min;
        }

        // IJ.log("before, max : " + max + " min : " + min);

        if (p.usecellmaskX && channel == 0) {
            Analysis.cellMaskABinary = createBinaryCellMask(Analysis.p.thresholdcellmask * (max - min) + min, img, channel, osz);
        }
        if (p.usecellmaskY && channel == 1) {
            Analysis.cellMaskBBinary = createBinaryCellMask(Analysis.p.thresholdcellmasky * (max - min) + min, img, channel, osz);
            // get imagedata and copy to array image
        }

        max = 0;
        min = Double.POSITIVE_INFINITY;

        for (int z = 0; z < nz; z++) {
            img.setSlice(z / osz + 1);
            imp = img.getProcessor();
            // remove background
            // rollingball version
            if (p.removebackground) {
                bs.rollingBallBackground(imp, p.size_rollingball, false, false, false, true, true);
            }

            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    image[z][i][j] = imp.getPixel(i / os, j / os);
                    if (image[z][i][j] > max) {
                        max = image[z][i][j];
                    }
                    if (image[z][i][j] < min) {
                        min = image[z][i][j];
                    }
                }
            }

        }

        /* Again overload the parameter after background subtraction */

        if (Analysis.norm_max != 0) {
            max = Analysis.norm_max;
            if (p.removebackground) {
                // if we are removing the background we have no idea which
                // is the minumum across all the movie so let be conservative and put
                // min = 0.0 for sure cannot be < 0

                min = 0.0;
            }
            else {
                min = Analysis.norm_min;
            }
        }

        // IJ.log("after, max : " + max + " min : " + min);
        if (p.livedisplay && p.removebackground) {
            final ImagePlus back = img.duplicate();
            back.setTitle("Background reduction channel " + (channel + 1));
            back.changes = false;
            back.setDisplayRange(min, max);
            back.show();
        }

        // normalize the image
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    image[z][i][j] = (image[z][i][j] - min) / (max - min);
                    if (image[z][i][j] < 0.0) {
                        image[z][i][j] = 0.0;
                    }
                    else if (image[z][i][j] > 1.0) {
                        image[z][i][j] = 1.0;
                    }
                }
            }
        }

        if (p.nlevels > 2) {
            p.cl = cluster();
        }

        if (p.nlevels == 2 || p.nlevels == 1) {

            p.cl[0] = p.betaMLEoutdefault;// 0.0027356;
            // p.cl[1]=0.2340026;
            p.cl[1] = p.betaMLEindefault;// 0.1340026;
            // p.cl[1]=0.2;
        }

        if (Analysis.p.automatic_int) {
            final double[] levs = cluster_int(5);
            p.cl[0] = levs[0];// 0.0027356;
            p.betaMLEoutdefault = levs[0];
            p.cl[1] = levs[3];
            p.betaMLEindefault = levs[3];
            IJ.log("automatic background:" + Tools.round(p.cl[0], 3));
            IJ.log("automatic foreground:" + Tools.round(p.cl[1], 3));
        }

        LocalTools.createmask(mask, image, p.cl);
        if (p.nlevels > 1 || !p.usePSF) {
            for (int i = 0; i < nl; i++) {
                LocalTools.nllMean1(Ei[i], image, p.cl[i]);
            }
        }
    }

    @Override
    public void run() {
        md = new MasksDisplay(ni, nj, nz, nl, p.cl, p);
        md.firstdisp = p.livedisplay;
        ASplitBregmanSolver A_solver = new ASplitBregmanSolver(p, image, Ei, mask, md, channel);

        // first run
        try {
            A_solver.first_run();
        }
        catch (final InterruptedException ex) {
        }

        final double minInt = Analysis.p.min_intensity;
        Analysis.p.min_intensity = 0;
        if (channel == 0) {
            Analysis.setmaska(A_solver.maxmask);
            if (!Analysis.p.looptest) {
                if (p.nlevels == 2) {
                    Analysis.compute_connected_regions_a(0.5, null);
                }
                else {
                    Analysis.compute_connected_regions_a(1.5, null);
                }
                A_solver = null;
            }

        }
        else {
            Analysis.setmaskb(A_solver.maxmask);
            if (!Analysis.p.looptest) {
                if (p.nlevels == 2) {
                    Analysis.compute_connected_regions_b(0.5, null);
                }
                else {
                    Analysis.compute_connected_regions_b(1.5, null);
                }
                A_solver = null;
            }

        }
        Analysis.p.min_intensity = minInt;

        DoneSignal.countDown();

    }

    private double[] cluster() {
        final Dataset data = new DefaultDataset();
        final double[] pixel = new double[1];
        final int max = Math.max(2, nl);
        final double[] levels = new double[max];

        // get imagedata
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    pixel[0] = image[z][i][j];
                    final Instance instance = new DenseInstance(pixel);
                    data.add(instance);
                }
            }
        }

        final Clusterer km = new KMeans(nl);
        /*
         * Cluster the data, it will be returned as an array of data sets, with
         * each dataset representing a cluster.
         */
        final Dataset[] data2 = km.cluster(data);
        for (int i = 0; i < nl; i++) {
            final Instance inst = DatasetTools.average(data2[i]);
            levels[i] = inst.value(0);
        }

        Arrays.sort(levels);

        return levels;

    }

    private double[] cluster_int(int nll) {

        final Dataset data = new DefaultDataset();
        final double[] pixel = new double[1];
        final double[] levels = new double[nll];

        // get imagedata
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    pixel[0] = image[z][i][j];
                    final Instance instance = new DenseInstance(pixel);
                    data.add(instance);
                }
            }
        }

        final Clusterer km = new KMeans(nll);
        /*
         * Cluster the data, it will be returned as an array of data sets, with
         * each dataset representing a cluster.
         */
        final Dataset[] data2 = km.cluster(data);
        for (int i = 0; i < nll; i++) {
            final Instance inst = DatasetTools.average(data2[i]);
            levels[i] = inst.value(0);
        }

        Arrays.sort(levels);
        return levels;

    }

    private boolean[][][] createBinaryCellMask(double threshold, ImagePlus img, int channel, int osz) {
        final boolean[][][] cellmask = new boolean[nz][ni][nj];

        final ImagePlus maska_im = new ImagePlus();
        final ImageStack maska_ims = new ImageStack(ni, nj);
        ImageProcessor imp;

        for (int z = 0; z < nz; z++) {
            img.setSlice(z / osz + 1);
            imp = img.getProcessor();
            final byte[] maska_bytes = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (imp.getPixel(i / p.model_oversampling, j / p.model_oversampling) > threshold) {
                        maska_bytes[j * ni + i] = (byte) 255;
                    }
                    else {
                        maska_bytes[j * ni + i] = 0;
                    }

                }
            }
            final ByteProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(maska_bytes);
            maska_ims.addSlice("", bp);
        }

        maska_im.setStack("Cell mask channel " + (channel + 1), maska_ims);

        IJ.run(maska_im, "Invert", "stack");
        IJ.run(maska_im, "Fill Holes", "stack");
        IJ.run(maska_im, "Open", "stack");
        IJ.run(maska_im, "Invert", "stack");

        if (Analysis.p.dispwindows && Analysis.p.livedisplay) {
            maska_im.show();
        }

        if (Analysis.p.save_images) {
            String savepath;
            if (channel == 0) {
                savepath = Analysis.p.wd + img.getTitle().substring(0, img.getTitle().length() - 4) + "_mask_c1" + ".zip";
            }
            else {
                savepath = Analysis.p.wd + img.getTitle().substring(0, img.getTitle().length() - 4) + "_mask_c2" + ".zip";
            }
            IJ.saveAs(maska_im, "ZIP", savepath);

        }

        for (int z = 0; z < nz; z++) {
            maska_im.setSlice(z + 1);
            imp = maska_im.getProcessor();
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    cellmask[z][i][j] = imp.getPixel(i, j) > 254;
                }
            }
        }

        return cellmask;
    }
}
