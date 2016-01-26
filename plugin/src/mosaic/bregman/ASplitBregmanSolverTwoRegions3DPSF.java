package mosaic.bregman;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_3D;
import mosaic.utils.ArrayOps;


class ASplitBregmanSolverTwoRegions3DPSF extends ASplitBregmanSolver {

    public final double[][][] w2zk;
    public final double[][][] b2zk;
    public final double[][][] ukz;
    private final double[][][] eigenLaplacian3D;

    private final double[][][] eigenPsf3D;
    private final DoubleDCT_3D dct3d;

    private ExecutorService executor;
    
    public ASplitBregmanSolverTwoRegions3DPSF(Parameters params, double[][][] image, double[][][] mask, MasksDisplay md, int channel, AnalysePatch ap, double aBetaMleOut, double aBetaMleIn) {
        super(params, image, mask, md, channel, ap, aBetaMleOut, aBetaMleIn);
        w2zk = new double[nz][ni][nj];
        ukz = new double[nz][ni][nj];
        b2zk = new double[nz][ni][nj];
        eigenLaplacian3D = new double[nz][ni][nj];
        dct3d = new DoubleDCT_3D(nz, ni, nj);

        LocalTools.fgradz2D(w2zk, mask);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenLaplacian3D[z][i][j] = 2 + (2 - 2 * Math.cos((j) * Math.PI / (nj)) + (2 - 2 * Math.cos((i) * Math.PI / (ni))) + (2 - 2 * Math.cos((z) * Math.PI / (nz))));
                }
            }
        }

        final int[] sz = p.PSF.getSuggestedImageSize();
        eigenPsf3D = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];

        // Reallocate temps
        // Unfortunatelly is allocated in ASplitBregmanSolver
        temp4 = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        temp3 = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        temp2 = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        temp1 = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];

        compute_eigenPSF3D();

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenLaplacian3D[z][i][j] = eigenLaplacian3D[z][i][j] - 2;
                }
            }
        }

        convolveAndScale(mask);
        calculateGradientsXandY(mask);
        System.out.println("========> CREATING POOL 3D");
        executor = Executors.newFixedThreadPool(p.nthreads);
    }
    
    @Override
    protected void init() {
        // TODO: Why these values are not updated and compute_eigenPSF3D() is not called? (as
        //       it is done for 2D case?
//        c0 = clIntensities[0];
//        c1 = clIntensities[1];
//        compute_eigenPSF3D();
        
        convolveAndScale(w3k);
        calculateGradientsXandY(w3k);
    }

    private void calculateGradientsXandY(double[][][] aValues) {
        LocalTools.fgradx2D(temp1, aValues);
        LocalTools.fgrady2D(temp2, aValues);
    }

    private void convolveAndScale(double[][][] aValues) {
        Tools.convolve3Dseparable(temp3, aValues, ni, nj, nz, p.PSF, temp4);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[z][i][j] = (iBetaMleIn - iBetaMleOut) * temp3[z][i][j] + iBetaMleOut;
                }
            }
        }
    }

    @Override
    protected void step() throws InterruptedException {

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
            final CountDownLatch Sync13 = new CountDownLatch(p.nthreads);
            final CountDownLatch Dct = new CountDownLatch(1);
            final CountDownLatch SyncFgradx = new CountDownLatch(1);
            
            int iStart = 0;
            int jStart = 0;
            final int ichunk = p.ni / p.nthreads;
            final int ilastchunk = p.ni - ichunk * (p.nthreads - 1);
            final int jchunk = p.nj / p.nthreads;
            final int jlastchunk = p.nj - jchunk * (p.nthreads - 1);
            
            for (int nt = 0; nt < p.nthreads - 1; nt++) {
                // Check if we can create threads
                final ZoneTask3D task = new ZoneTask3D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, Sync13, Dct, iStart, iStart + ichunk, jStart,
                        jStart + jchunk, nt, this, LocalTools);
                executor.execute(task);
                iStart += ichunk;
                jStart += jchunk;
            }
            final ZoneTask3D task = new ZoneTask3D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, Sync13, Dct, iStart, iStart + ilastchunk,
                    jStart, jStart + jlastchunk, p.nthreads - 1, this, LocalTools);
            executor.execute(task);
            Sync4.await();
            
            dct3d.forward(temp1, true);
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        if ((1 + eigenLaplacian3D[z][i][j] + eigenPsf3D[0][i][j]) != 0) {
                            temp1[z][i][j] = temp1[z][i][j] / (1 + eigenLaplacian3D[z][i][j] + eigenPsf3D[z][i][j]);
                        }
                    }
                }
            }
            dct3d.inverse(temp1, true);
            
            Dct.countDown();
            
            // do fgradx without parallelization
            LocalTools.fgradx2D(temp4, temp1);
            SyncFgradx.countDown();
            
            ZoneDoneSignal.await();
            
            if (stepk % p.energyEvaluationModulo == 0) {
                energy = 0;
                for (int nt = 0; nt < p.nthreads; nt++) {
                    energy += energytab2[nt];
                }
            }
            
            if (p.livedisplay && p.firstphase) {
                md.display2regions(w3k, "Mask3d", channel);
            }
    }

    private void compute_eigenPSF3D() {
        int[] sz = p.PSF.getSuggestedImageSize();
        final int xmin = Math.min(sz[0], eigenPsf3D[0].length);
        final int ymin = Math.min(sz[1], eigenPsf3D[0][0].length);
        final int zmin = Math.min(sz[2], eigenPsf3D.length);
        Tools.convolve3Dseparable(eigenPsf3D, p.PSF.getImage3DAsDoubleArray(), xmin, ymin, zmin, p.PSF, temp4);

        ArrayOps.fill(temp2, 0);
        for (int z = 0; z < zmin; z++) {
            for (int i = 0; i < xmin; i++) {
                for (int j = 0; j < ymin; j++) {
                    temp2[z][i][j] = eigenPsf3D[z][i][j];
                }
            }
        }

        final int cr = (sz[0] / 2) + 1;
        final int cc = (sz[1] / 2) + 1;
        final int cs = (sz[2] / 2) + 1;

        LocalTools.dctshift3D(temp3, temp2, cr, cc, cs);
        dct3d.forward(temp3, true);
        
        ArrayOps.fill(temp1, 0);
        temp1[0][0][0] = 1;
        dct3d.forward(temp1, true);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenPsf3D[z][i][j] = Math.pow(iBetaMleIn - iBetaMleOut, 2) * temp3[z][i][j] / temp1[z][i][j];
                }
            }
        }
    }
}
