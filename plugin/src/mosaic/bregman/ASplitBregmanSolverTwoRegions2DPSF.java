package mosaic.bregman;


import java.util.concurrent.CountDownLatch;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;
import mosaic.utils.ArrayOps;


class ASplitBregmanSolverTwoRegions2DPSF extends ASplitBregmanSolver {

    private final double[][][] eigenPsf2D;
    private final DoubleDCT_2D dct2d;
    
    public ASplitBregmanSolverTwoRegions2DPSF(Parameters aParameters, double[][][] image, double[][][] mask, AnalysePatch ap, double aBetaMleOut, double aBetaMleIn, double aLreg, double aMinIntensity) {
        super(aParameters, image, mask, ap, aBetaMleOut, aBetaMleIn, aLreg, aMinIntensity);
        dct2d = new DoubleDCT_2D(ni, nj);
        eigenPsf2D = new double[nz][ni][nj];
        compute_eigenPSF();
        
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                eigenLaplacian[i][j] = eigenLaplacian[i][j] - 2;
            }
        }

        convolveAndScale(mask[0]);
        calculateGradientsXandY(mask);
    }

    @Override
    protected void init() {
        iBetaMleOut = betaMle[0];
        iBetaMleIn = betaMle[1];
        compute_eigenPSF();
        
        convolveAndScale(w3k[0]);
        calculateGradientsXandY(w3k);
    }

    private void convolveAndScale(double[][] aValues) {
        Tools.convolve2D(temp3[0], aValues, ni, nj, iParameters.PSF);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[z][i][j] = (iBetaMleIn - iBetaMleOut) * temp3[z][i][j] + iBetaMleOut;
                }
            }
        }
    }

    private void calculateGradientsXandY(double[][][] aValues) {
        LocalTools.fgradx2D(w2xk, aValues);
        LocalTools.fgrady2D(w2yk, aValues);
    }

    @Override
    protected void step(boolean aEvaluateEnergy, boolean aLastIteration) throws InterruptedException {
        // WARNING !! : temp1 and temp2 (resp =w2xk and =w2yk) passed from iteration to next iteration : do not change .
        final CountDownLatch ZoneDoneSignal = new CountDownLatch(iParameters.nthreads);// subprob 1 and 3
        final CountDownLatch Sync1 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync2 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync3 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync4 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync5 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync6 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync7 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync8 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync9 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync10 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync11 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Sync12 = new CountDownLatch(iParameters.nthreads);
        final CountDownLatch Dct = new CountDownLatch(1);

        final int ichunk = ni / iParameters.nthreads;
        final int ilastchunk = ni - ichunk * (iParameters.nthreads - 1);
        final int jchunk = nj / iParameters.nthreads;
        final int jlastchunk = nj - jchunk * (iParameters.nthreads - 1);
        int iStart = 0;
        int jStart = 0;

        for (int nt = 0; nt < iParameters.nthreads - 1; nt++) {
            final ZoneTask task = new ZoneTask(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ichunk, jStart, jStart + jchunk, nt,
                    this, LocalTools, aEvaluateEnergy, aLastIteration);
            executor.execute(task);
            iStart += ichunk;
            jStart += jchunk;
        }
        final ZoneTask task = new ZoneTask(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ilastchunk, jStart, jStart + jlastchunk,
                iParameters.nthreads - 1, this, LocalTools, aEvaluateEnergy, aLastIteration);
        executor.execute(task);
        // temp1=uk
        Sync4.await();

        // Check match here
        dct2d.forward(temp1[0], true);

        // inversion int DCT space
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                if ((1 + eigenLaplacian[i][j] + eigenPsf2D[0][i][j]) != 0) {
                    temp1[0][i][j] = temp1[0][i][j] / (1 + eigenLaplacian[i][j] + eigenPsf2D[0][i][j]);
                }
            }
        }

        dct2d.inverse(temp1[0], true);
        Dct.countDown();
        ZoneDoneSignal.await();

        if (aEvaluateEnergy) {
            energy = 0;
            for (int nt = 0; nt < iParameters.nthreads; nt++) {
                energy += energytab2[nt];
            }
        }
    }

    private void compute_eigenPSF() {
        final int[] sz = iParameters.PSF.getSuggestedImageSize();
        final int xmin = Math.min(sz[0], eigenPsf2D[0].length);
        final int ymin = Math.min(sz[1], eigenPsf2D[0][0].length);
        Tools.convolve2D(eigenPsf2D[0], iParameters.PSF.getImage2DAsDoubleArray(), xmin, ymin, iParameters.PSF);

        ArrayOps.fill(temp1, 0);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < xmin; i++) {
                for (int j = 0; j < ymin; j++) {
                    temp1[z][i][j] = eigenPsf2D[z][i][j];
                }
            }
        }

        final int cc = (sz[0] / 2) + 1;
        final int cr = (sz[1] / 2) + 1;

        LocalTools.dctshift(temp3[0], temp1[0], cc, cr);
        dct2d.forward(temp3[0], true);

        ArrayOps.fill(temp2, 0);
        temp2[0][0][0] = 1;
        dct2d.forward(temp2[0], true);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenPsf2D[z][i][j] = Math.pow(iBetaMleIn - iBetaMleOut, 2) * temp3[z][i][j] / temp2[z][i][j];
                } 
            }
        }
    }
}
