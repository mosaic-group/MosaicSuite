package mosaic.bregman.segmentation.solver;


import java.util.concurrent.CountDownLatch;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import net.imglib2.type.numeric.real.DoubleType;


public class ASplitBregmanSolver2D extends ASplitBregmanSolver {

    private final double[][] eigenPsf2D;
    private final DoubleDCT_2D dct2d;
    private final double[][] eigenLaplacian;
    
    public ASplitBregmanSolver2D(SolverParameters aParameters, double[][][] image, double[][][] mask, psf<DoubleType> aPsf) {
        super(aParameters, image, mask, aPsf);
        dct2d = new DoubleDCT_2D(ni, nj);

        eigenLaplacian = new double[ni][nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                eigenLaplacian[i][j] = (2 - 2 * Math.cos((j) * Math.PI / (nj)) + (2 - 2 * Math.cos((i) * Math.PI / (ni))));
            }
        }
        
        eigenPsf2D = new double[ni][nj];
        compute_eigenPSF();
        
        convolveAndScale(mask[0]);
        calculateGradients(mask);
    }

    @Override
    public void init() {
        iBetaMleOut = betaMle[0];
        iBetaMleIn = betaMle[1];
        compute_eigenPSF();
        
        convolveAndScale(w3k[0]);
        calculateGradients(w3k);
    }

    private void convolveAndScale(double[][] aValues) {
        SolverTools.convolve2D(temp3[0], aValues, ni, nj, iPsf);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[z][i][j] = (iBetaMleIn - iBetaMleOut) * temp3[z][i][j] + iBetaMleOut;
                }
            }
        }
    }

    private void calculateGradients(double[][][] aValues) {
        iLocalTools.fgradx2D(w2xk, aValues);
        iLocalTools.fgrady2D(w2yk, aValues);
    }

    @Override
    protected void step(boolean aEvaluateEnergy) throws InterruptedException {
        // WARNING !! : temp1 and temp2 (resp =w2xk and =w2yk) passed from iteration to next iteration : do not change .
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
        final CountDownLatch Dct = new CountDownLatch(1);

        final int ichunk = ni / iParameters.numOfThreads;
        final int ilastchunk = ni - ichunk * (iParameters.numOfThreads - 1);
        final int jchunk = nj / iParameters.numOfThreads;
        final int jlastchunk = nj - jchunk * (iParameters.numOfThreads - 1);
        int iStart = 0;
        int jStart = 0;

        for (int nt = 0; nt < iParameters.numOfThreads - 1; nt++) {
            final ZoneTask2D task = new ZoneTask2D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ichunk, jStart, jStart + jchunk, nt,
                    this, iLocalTools, aEvaluateEnergy);
            executor.execute(task);
            iStart += ichunk;
            jStart += jchunk;
        }
        final ZoneTask2D task = new ZoneTask2D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ilastchunk, jStart, jStart + jlastchunk,
                iParameters.numOfThreads - 1, this, iLocalTools, aEvaluateEnergy);
        executor.execute(task);
        // temp1=uk
        Sync4.await();

        // Check match here
        dct2d.forward(temp1[0], true);
        // inversion int DCT space
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                final double denominator = 1 + eigenLaplacian[i][j] + eigenPsf2D[i][j];
                if (denominator != 0) {
                    temp1[0][i][j] = temp1[0][i][j] / denominator;
                }
            }
        }
        dct2d.inverse(temp1[0], true);
        Dct.countDown();
        
        ZoneDoneSignal.await();
    }

    private void compute_eigenPSF() {
        final int[] sz = iPsf.getSuggestedImageSize();
        final int xmin = Math.min(sz[0], eigenPsf2D.length);
        final int ymin = Math.min(sz[1], eigenPsf2D[0].length);
        SolverTools.convolve2D(eigenPsf2D, iPsf.getImage2DAsDoubleArray(), xmin, ymin, iPsf);

        ArrayOps.fill(temp1, 0);
        for (int i = 0; i < xmin; i++) {
            for (int j = 0; j < ymin; j++) {
                temp1[0][i][j] = eigenPsf2D[i][j];
            }
        }

        final int cc = (sz[0] / 2) + 1;
        final int cr = (sz[1] / 2) + 1;

        iLocalTools.dctshift(temp3[0], temp1[0], cc, cr);
        dct2d.forward(temp3[0], true);

        ArrayOps.fill(temp2, 0);
        temp2[0][0][0] = 1;
        dct2d.forward(temp2[0], true);

        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                eigenPsf2D[i][j] = Math.pow(iBetaMleIn - iBetaMleOut, 2) * temp3[0][i][j] / temp2[0][i][j];
            } 
        }
    }
}
