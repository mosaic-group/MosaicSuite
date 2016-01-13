package mosaic.bregman;


import java.util.ArrayList;
import java.util.Arrays;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;
import net.sf.javaml.tools.weka.WekaClusterer;
import weka.clusterers.SimpleKMeans;


/**
 * Regions statistics solver
 * @author i-bird
 */
class RegionStatisticsSolver {

    private final double[][][] Z;
    private final double[][][] W;
    private final double[][][] mu;
    private final int max_iter;
    private final Parameters p;

    private double[][][] weights;
    private final double[][][] image;
    private final double[][][] KMask;

    public double betaMLEin, betaMLEout;

    /**
     * Create a region statistic solver
     *
     * @param temp1 buffer of the same size of image for internal calculation
     * @param temp2 buffer of the same size of image for internal calculation
     * @param temp3 buffer of the same size of image for internal calculation
     * @param image The image pixel array
     * @param weights
     * @param max_iter Maximum number of iteration for the Fisher scoring
     * @param p
     */
    public RegionStatisticsSolver(double[][][] temp1, double[][][] temp2, double[][][] temp3, double[][][] image, double[][][] weights, int max_iter, Parameters p) {
        this.p = p;
        this.Z = image;
        this.W = temp1;
        this.mu = temp2;
        this.KMask = temp3;
        this.image = image;
        this.max_iter = max_iter;
        this.weights = weights;
    }

    /**
     * Create a region statistic solver
     *
     * @param temp1 buffer of the same size of image for internal calculation
     * @param temp2 buffer of the same size of image for internal calculation
     * @param temp3 buffer of the same size of image for internal calculation
     * @param image The image pixel array
     * @param weights
     * @param max_iter Maximum number of iteration for the Fisher scoring
     * @param p
     */
    public RegionStatisticsSolver(double[][][] temp1, double[][][] temp2, double[][][] temp3, double[][][] image, int max_iter, Parameters p) {
        this.p = p;
        this.Z = image;
        this.W = temp1;
        this.mu = temp2;
        this.KMask = temp3;
        this.image = image;
        this.max_iter = max_iter;
        fill_weights();
    }

    private void fill_weights() {
        final int ni = p.ni;
        final int nj = p.nj;
        final int nz = p.nz;
        this.weights = new double[nz][ni][nj];
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    weights[z][i][j] = 1;
                }
            }
        }
    }

    /**
     * Evaluate the region intensity
     * @param Mask
     */
    public void eval(double[][][] Mask) {
        final int ni = p.ni;
        final int nj = p.nj;
        final int nz = p.nz;

        // normalize Mask
        this.scale_mask(W, Mask);

        // Convolve the mask
        if (nz == 1) {
            Tools.convolve2Dseparable(KMask[0], W[0], ni, nj, Analysis.p.PSF, mu[0]);
        }
        else {
            Tools.convolve3Dseparable(KMask, W, ni, nj, nz, Analysis.p.PSF, mu);
        }

        double K11 = 0, K12 = 0, K22 = 0, U1 = 0, U2 = 0;
        double detK = 0;
        betaMLEout = 0;
        betaMLEin = 0;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (Z[z][i][j] != 0) {
                        W[z][i][j] = weights[z][i][j] / Z[z][i][j];
                    }
                    else {
                        W[z][i][j] = 4.50359962737e+15;// 1e4;
                    }
                }
            }
        }

        int iter = 0;
        while (iter < max_iter) {
            K11 = 0;
            K12 = 0;
            K22 = 0;
            U1 = 0;
            U2 = 0;
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        K11 += W[z][i][j] * Math.pow(1 - KMask[z][i][j], 2);
                        K12 += W[z][i][j] * (1 - KMask[z][i][j]) * KMask[z][i][j];
                        K22 += W[z][i][j] * (KMask[z][i][j]) * KMask[z][i][j];
                        U1 += W[z][i][j] * (1 - KMask[z][i][j]) * Z[z][i][j];
                        U2 += W[z][i][j] * (KMask[z][i][j]) * Z[z][i][j];
                    }
                }
            }

            // detK = K11*K22-K12^2;
            // betaMLE_out = ( K22*U1-K12*U2)/detK;
            // betaMLE_in = (-K12*U1+K11*U2)/detK;
            detK = K11 * K22 - Math.pow(K12, 2);
            if (detK != 0) {
                betaMLEout = (K22 * U1 - K12 * U2) / detK;
                betaMLEin = (-K12 * U1 + K11 * U2) / detK;
            }
            else {
                betaMLEout = p.betaMLEoutdefault;
                betaMLEin = p.betaMLEindefault;
            }

            // mu update
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        mu[z][i][j] = (betaMLEin - betaMLEout) * KMask[z][i][j] + betaMLEout;
                    }
                }
            }

            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        if (mu[z][i][j] != 0) {
                            W[z][i][j] = weights[z][i][j] / mu[z][i][j];
                        }
                        else {
                            W[z][i][j] = 4.50359962737e+15;// 10000;//Double.MAX_VALUE;
                        }
                    }
                }
            }

            iter++;
        }
    }

    private void scale_mask(double[][][] ScaledMask, double[][][] Mask) {
        final int ni = p.ni;
        final int nj = p.nj;
        final int nz = p.nz;

        double max = 0;
        double min = Double.POSITIVE_INFINITY;

        // get imagedata and copy to array image
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (Mask[z][i][j] > max) {
                        max = Mask[z][i][j];
                    }
                    if (Mask[z][i][j] < min) {
                        min = Mask[z][i][j];
                    }
                }
            }
        }

        if ((max - min) != 0) {
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        ScaledMask[z][i][j] = (Mask[z][i][j] - min) / (max - min);
                    }
                }
            }
        }
    }

    void cluster_region(float[][][] Ri, float[][][] Ro, ArrayList<Region> regionslist) {
        int nk = 3;
        final double[] pixel = new double[1];
        final double[] levels = new double[nk];

        for (final Region r : regionslist) {
            final Dataset data = new DefaultDataset();
            if (r.points < 6) {
                continue;
            }
            for (final Pix p : r.pixels) {
                final int i = p.px;
                final int j = p.py;
                final int z = p.pz;
                pixel[0] = image[z][i][j];
                final Instance instance = new DenseInstance(pixel);
                data.add(instance);
            }

            /* Create Weka classifier */
            final SimpleKMeans xm = new SimpleKMeans();
            try {
                xm.setNumClusters(3);// 3
                xm.setMaxIterations(100);
            }
            catch (final Exception ex) {}

            /* Wrap Weka clusterer in bridge */
            final Clusterer jmlxm = new WekaClusterer(xm);
            /* Perform clustering */
            final Dataset[] data2 = jmlxm.cluster(data);
            /* Output results */
            nk = data2.length;// get number of clusters really found (usually = 3 = setNumClusters but not always)
            for (int i = 0; i < nk; i++) {
                final Instance inst = DatasetTools.average(data2[i]);
                levels[i] = inst.value(0);
            }

            Arrays.sort(levels);
            nk = Math.min(Analysis.p.regionSegmentLevel, nk - 1);
            betaMLEin = levels[nk];// -1;
            final int nkm1 = Math.max(nk - 1, 0);
            betaMLEout = levels[nkm1];

            if (p.mode_voronoi2) {
                for (final Pix p : r.pixels) {
                    final int i = p.px;
                    final int j = p.py;
                    final int z = p.pz;
                    Ri[z][i][j] = regionslist.indexOf(r);
                }
            }
            else {
                for (final Pix p : r.pixels) {
                    final int i = p.px;
                    final int j = p.py;
                    final int z = p.pz;
                    Ri[z][i][j] = (float) (255 * betaMLEin);
                    Ro[z][i][j] = (float) (255 * betaMLEout);
                }
            }
        }
    }

    void cluster_region_voronoi2(float[][][] Ri, ArrayList<Region> regionslist) {
        for (final Region r : regionslist) {
            for (final Pix p : r.pixels) {
                final int i = p.px;
                final int j = p.py;
                final int z = p.pz;
                Ri[z][i][j] = regionslist.indexOf(r);
            }
        }
    }
}
