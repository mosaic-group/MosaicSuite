package mosaic.bregman;


import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ImageProcessor;
import mosaic.utils.ArrayOps.MinMax;
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

    protected final double[][][] image;// 3D image
    protected final double[][][][] mask;// nregions nslices ni nj
    protected final double[][][][] Ei;

    protected final Parameters p;

    protected final int ni, nj, nz;// 3 dimensions
    protected final int nl;
    protected final int channel;
    protected final CountDownLatch DoneSignal;

    protected final Tools LocalTools;
    protected double min, max;
    
    protected MasksDisplay md;
    
    public NRegions(ImagePlus img, Parameters params, CountDownLatch DoneSignal, int channel) {
        if (img.getBitDepth() == 32) {
            IJ.log("Error converting float image to short");
        }

        this.p = params;
        this.DoneSignal = DoneSignal;
        this.channel = channel;
        
        this.nl = p.nlevels;
        this.ni = p.ni;
        this.nj = p.nj;
        this.nz = p.nz;

        LocalTools = new Tools(ni, nj, nz, nl);

        image = new double[nz][ni][nj];
        mask = new double[nl][nz][ni][nj];
        if (p.nlevels > 1 || !p.usePSF) {
            Ei = new double[nl][nz][ni][nj];
        }
        else {
            Ei = null;
        }

        min = Double.POSITIVE_INFINITY;
        max = 0;

        /* Search for maximum and minimum value, normalization */
        if (Analysis.norm_max == 0) {
            MinMax<Double> mm = Tools.findMinMax(img);
            min = mm.getMin();
            max = mm.getMax();
        }
        else {
            min = Analysis.norm_min;
            max = Analysis.norm_max;
        }

        if (p.usecellmaskX && channel == 0) {
            Analysis.cellMaskABinary = Tools.createBinaryCellMask(Analysis.p.thresholdcellmask * (max - min) + min, img, channel, nz, ni, nj, true);
        }
        if (p.usecellmaskY && channel == 1) {
            Analysis.cellMaskBBinary = Tools.createBinaryCellMask(Analysis.p.thresholdcellmasky * (max - min) + min, img, channel, nz, ni, nj, true);
        }

        max = 0;
        min = Double.POSITIVE_INFINITY;

        for (int z = 0; z < nz; z++) {
            img.setSlice(z + 1);
            ImageProcessor imp = img.getProcessor();
            
            if (p.removebackground) {
                final BackgroundSubtracter bs = new BackgroundSubtracter();
                bs.rollingBallBackground(imp, p.size_rollingball, false, false, false, true, true);
            }

            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    image[z][i][j] = imp.getPixel(i, j);
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
                // if we are removing the background we have no idea which is the minumum across 
                // all the movie so let be conservative and put min = 0.0 for sure cannot be < 0
                min = 0.0;
            }
            else {
                min = Analysis.norm_min;
            }
        }

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
            p.cl = cluster_int(nl);
        }

        if (p.nlevels == 2 || p.nlevels == 1) {
            p.cl[0] = p.betaMLEoutdefault;
            p.cl[1] = p.betaMLEindefault;
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

        try {
            A_solver.first_run();
        }
        catch (final InterruptedException ex) {}

        // Save old value of min_intensity and restore it later
        final double minIntensityBackup = Analysis.p.min_intensity;
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
            }
        }
        // Restore old value
        Analysis.p.min_intensity = minIntensityBackup;

        DoneSignal.countDown();
    }

    private double[] cluster_int(int aNumOfLevels) {
        // get imagedata
        final Dataset data = new DefaultDataset();
        final double[] pixel = new double[1];
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    pixel[0] = image[z][i][j];
                    final Instance instance = new DenseInstance(pixel);
                    data.add(instance);
                }
            }
        }

        // Cluster the data, it will be returned as an array of data sets, with
        // each dataset representing a cluster.
        final Clusterer km = new KMeans(aNumOfLevels);
        final double[] levels = new double[Math.max(2, aNumOfLevels)];
        final Dataset[] data2 = km.cluster(data);
        for (int i = 0; i < aNumOfLevels; i++) {
            final Instance inst = DatasetTools.average(data2[i]);
            levels[i] = inst.value(0);
        }
        Arrays.sort(levels);
        
        return levels;
    }
}
