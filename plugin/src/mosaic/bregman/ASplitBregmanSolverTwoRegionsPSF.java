package mosaic.bregman;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;
import mosaic.utils.ArrayOps;


class ASplitBregmanSolverTwoRegionsPSF extends ASplitBregmanSolver {

    private final double[][][] eigenPSF;
    private final DoubleDCT_2D dct2d;
    private ExecutorService executor;
    
    public ASplitBregmanSolverTwoRegionsPSF(Parameters params, double[][][] image, double[][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        super(params, image, mask, md, channel, ap);
        dct2d = new DoubleDCT_2D(ni, nj);
        eigenPSF = new double[nz][ni][nj];
        compute_eigenPSF();
        
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                eigenLaplacian[i][j] = eigenLaplacian[i][j] - 2;
            }
        }

        convolveAndScale(mask[0]);
        calculateGradientsXandY(mask);
        System.out.println("========> CREATING POOL 2D");
        executor = Executors.newFixedThreadPool(p.nthreads);
    }

    @Override
    protected void init() {
        compute_eigenPSF();
        
        convolveAndScale(w3k[0]);
        calculateGradientsXandY(w3k);
    }

    private void convolveAndScale(double[][] aValues) {
        Tools.convolve2D(temp3[0], aValues, ni, nj, p.PSF);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[z][i][j] = (c1 - c0) * temp3[z][i][j] + c0;
                }
            }
        }
    }

    private void calculateGradientsXandY(double[][][] aValues) {
        LocalTools.fgradx2D(w2xk, aValues);
        LocalTools.fgrady2D(w2yk, aValues);
    }

    @Override
    protected void step() throws InterruptedException {
        // WARNING !! : temp1 and temp2 (resp =w2xk and =w2yk) passed from iteration to next iteration : do not change .

        c0 = p.cl[0];
        c1 = p.cl[1];

        final CountDownLatch ZoneDoneSignal = new CountDownLatch(p.nthreads);// subprob 1 and 3
        final CountDownLatch Sync1 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync2 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync3 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync4 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync5 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync6 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync7 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync8 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync9 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync10 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync11 = new CountDownLatch(p.nthreads);
        final CountDownLatch Sync12 = new CountDownLatch(p.nthreads);
        final CountDownLatch Dct = new CountDownLatch(1);

        final int ichunk = p.ni / p.nthreads;
        final int ilastchunk = p.ni - (p.ni / (p.nthreads)) * (p.nthreads - 1);
        final int jchunk = p.nj / p.nthreads;
        final int jlastchunk = p.nj - (p.nj / (p.nthreads)) * (p.nthreads - 1);
        int iStart = 0;
        int jStart = 0;

        for (int nt = 0; nt < p.nthreads - 1; nt++) {
            final ZoneTask task = new ZoneTask(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ichunk, jStart, jStart + jchunk, nt,
                    this, LocalTools);
            executor.execute(task);
            iStart += ichunk;
            jStart += jchunk;
        }
        final ZoneTask task = new ZoneTask(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ilastchunk, jStart, jStart + jlastchunk,
                p.nthreads - 1, this, LocalTools);
        executor.execute(task);
        // temp1=uk
        Sync4.await();

        // Check match here
        dct2d.forward(temp1[0], true);

        // inversion int DCT space
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                if ((1 + eigenLaplacian[i][j] + eigenPSF[0][i][j]) != 0) {
                    temp1[0][i][j] = temp1[0][i][j] / (1 + eigenLaplacian[i][j] + eigenPSF[0][i][j]);
                }
            }
        }

        dct2d.inverse(temp1[0], true);
        Dct.countDown();
        ZoneDoneSignal.await();

        if (stepk % p.energyEvaluationModulo == 0) {
            energy = 0;
            for (int nt = 0; nt < p.nthreads; nt++) {
                energy += energytab2[nt];
            }
        }

        if (p.livedisplay && p.firstphase) {
            md.display2regions(w3k, "Mask", channel);
        }
    }

    private void compute_eigenPSF() {
        c0 = p.cl[0];
        c1 = p.cl[1];

        final int[] sz = p.PSF.getSuggestedImageSize();
        final int xmin = Math.min(sz[0], eigenPSF[0].length);
        final int ymin = Math.min(sz[1], eigenPSF[0][0].length);

        Tools.convolve2D(eigenPSF[0], p.PSF.getImage2DAsDoubleArray(), xmin, ymin, p.PSF);

        ArrayOps.fill(temp1, 0);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < xmin; i++) {
                for (int j = 0; j < ymin; j++) {
                    temp1[z][i][j] = eigenPSF[z][i][j];
                }
            }
        }

        final int cc = (sz[0] / 2) + 1;
        final int cr = (sz[1] / 2) + 1;

        // temp1 = e1

        LocalTools.dctshift(temp3[0], temp1[0], cc, cr);
        dct2d.forward(temp3[0], true);

        ArrayOps.fill(temp2, 0);
        temp2[0][0][0] = 1;
        dct2d.forward(temp2[0], true);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenPSF[z][i][j] = Math.pow(c1 - c0, 2) * temp3[z][i][j] / temp2[z][i][j];
                } 
            }
        }
    }
}
