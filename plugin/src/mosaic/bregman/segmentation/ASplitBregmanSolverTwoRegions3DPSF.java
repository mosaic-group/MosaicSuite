package mosaic.bregman.segmentation;


import java.util.concurrent.CountDownLatch;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_3D;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import mosaic.utils.Debug;
import net.imglib2.type.numeric.real.DoubleType;


class ASplitBregmanSolverTwoRegions3DPSF extends ASplitBregmanSolver {

    final double[][][] w2zk;
    final double[][][] b2zk;
    final double[][][] ukz;
    
    private final double[][][] eigenPsf3D;
    private final DoubleDCT_3D dct3d;
    private final double[][][] eigenLaplacian3D;

    public ASplitBregmanSolverTwoRegions3DPSF(SegmentationParameters aParameters, double[][][] image, double[][][] mask, AnalysePatch ap, double aBetaMleOut, double aBetaMleIn, double aLreg, double aMinIntensity, psf<DoubleType> aPsf) {
        super(aParameters, image, mask, ap, aBetaMleOut, aBetaMleIn, aLreg, aMinIntensity, aPsf);
        w2zk = new double[nz][ni][nj];
        b2zk = new double[nz][ni][nj];
        ukz = new double[nz][ni][nj];
        // Old comment: Reallocate temps. Unfortunatelly is allocated in ASplitBregmanSolver
        // TODO: This must be fixed, not "fixed"
        final int[] sz = iPsf.getSuggestedImageSize();
        temp4 = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        temp3 = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        temp2 = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        temp1 = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];

        dct3d = new DoubleDCT_3D(nz, ni, nj);

        eigenLaplacian3D = new double[nz][ni][nj];
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenLaplacian3D[z][i][j] = (2 - 2 * Math.cos((j) * Math.PI / (nj))) + (2 - 2 * Math.cos((i) * Math.PI / (ni))) + (2 - 2 * Math.cos((z) * Math.PI / (nz)));
                }
            }
        }

        eigenPsf3D = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        compute_eigenPSF3D();

        convolveAndScale(mask);
        calculateGradients(mask);
    }
    
    @Override
    protected void init() {
        // TODO: Why these values are not updated and compute_eigenPSF3D() is not called? (as
        //       it is done for 2D case?
//        mosaic.utils.Debug.print("BetaMLE: ", betaMle);
//        iBetaMleOut = betaMle[0];
//        iBetaMleIn = betaMle[1];
//        compute_eigenPSF3D();
        
        convolveAndScale(w3k);
        calculateGradients(w3k);
    }

    private void calculateGradients(double[][][] aValues) {
        iLocalTools.fgradx2D(w2xk, aValues);
        iLocalTools.fgrady2D(w2yk, aValues);
        iLocalTools.fgradz2D(w2zk, aValues);
    }

    private void convolveAndScale(double[][][] aValues) {
        Tools.convolve3Dseparable(temp3, aValues, ni, nj, nz, iPsf, temp4);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[z][i][j] = (iBetaMleIn - iBetaMleOut) * temp3[z][i][j] + iBetaMleOut;
                }
            }
        }
    }

    @Override
    protected void step(boolean aEvaluateEnergy, boolean aLastIteration) throws InterruptedException {

        final CountDownLatch ZoneDoneSignal = new CountDownLatch(iParameters.numOfThreads);// subprob 1 and 3
        final CountDownLatch Sync1 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync2 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync3 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync4 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync5 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync6 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync7 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync8 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync9 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync10 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync11 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync12 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Sync13 = new CountDownLatch(iParameters.numOfThreads);
        final CountDownLatch Dct = new CountDownLatch(1);

        int iStart = 0;
        int jStart = 0;
        final int ichunk = ni / iParameters.numOfThreads;
        final int ilastchunk = ni - ichunk * (iParameters.numOfThreads - 1);
        final int jchunk = nj / iParameters.numOfThreads;
        final int jlastchunk = nj - jchunk * (iParameters.numOfThreads - 1);

        for (int nt = 0; nt < iParameters.numOfThreads - 1; nt++) {
            // Check if we can create threads
            final ZoneTask3D task = new ZoneTask3D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, Sync13, Dct, iStart, iStart + ichunk, jStart,
                    jStart + jchunk, nt, this, iLocalTools, aEvaluateEnergy, aLastIteration);
            executor.execute(task);
            iStart += ichunk;
            jStart += jchunk;
        }
        final ZoneTask3D task = new ZoneTask3D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, Sync13, Dct, iStart, iStart + ilastchunk,
                jStart, jStart + jlastchunk, iParameters.numOfThreads - 1, this, iLocalTools, aEvaluateEnergy, aLastIteration);
        executor.execute(task);
        Sync4.await();

     // Check match here
        dct3d.forward(temp1, true);
     // inversion int DCT space
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    final double denominator = 1 + eigenLaplacian3D[z][i][j] + eigenPsf3D[z][i][j];
                    if (denominator != 0) {
                        temp1[z][i][j] = temp1[z][i][j] / denominator;
                    }
                }
            }
        }
        dct3d.inverse(temp1, true);
        Dct.countDown();
        
        ZoneDoneSignal.await();
    }

    private void compute_eigenPSF3D() {
        int[] sz = iPsf.getSuggestedImageSize();
        final int xmin = Math.min(sz[0], eigenPsf3D[0].length);
        final int ymin = Math.min(sz[1], eigenPsf3D[0][0].length);
        final int zmin = Math.min(sz[2], eigenPsf3D.length);
        Tools.convolve3Dseparable(eigenPsf3D, iPsf.getImage3DAsDoubleArray(), xmin, ymin, zmin, iPsf, temp4);
        ArrayOps.fill(temp1, 0);
        for (int z = 0; z < zmin; z++) {
            for (int i = 0; i < xmin; i++) {
                for (int j = 0; j < ymin; j++) {
                    temp1[z][i][j] = eigenPsf3D[z][i][j];
                }
            }
        }

        final int cr = (sz[0] / 2) + 1;
        final int cc = (sz[1] / 2) + 1;
        final int cs = (sz[2] / 2) + 1;

        iLocalTools.dctshift3D(temp3, temp1, cr, cc, cs);
        dct3d.forward(temp3, true);
        
        ArrayOps.fill(temp2, 0);
        temp2[0][0][0] = 1;
        dct3d.forward(temp2, true);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenPsf3D[z][i][j] = Math.pow(iBetaMleIn - iBetaMleOut, 2) * temp3[z][i][j] / temp2[z][i][j];
                }
            }
        }
    }
}
