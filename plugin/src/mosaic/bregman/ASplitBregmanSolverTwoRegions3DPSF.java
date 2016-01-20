package mosaic.bregman;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_3D;


class ASplitBregmanSolverTwoRegions3DPSF extends ASplitBregmanSolver {

    public final double[][][] w2zk;
    public final double[][][] b2zk;
    public final double[][][] ukz;
    public final double[][][] eigenLaplacian3D;

    public final double[][][] eigenPSF;
    public final DoubleDCT_3D dct3d;

    ExecutorService executor;
    
    public ASplitBregmanSolverTwoRegions3DPSF(Parameters params, double[][][] image, double[][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        super(params, image, mask, md, channel, ap);
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
        eigenPSF = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];

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
                    w1k[z][i][j] = (c1 - c0) * temp3[z][i][j] + c0;
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
            
            // Force the allocation of the buffers internally
            // if you do not do you can have race conditions in the multi thread part
            // DO NOT REMOVE THEM EVEN IF THEY LOOK UNUSEFULL
            p.PSF.getSeparableImageAsDoubleArray(0);
            p.PSF.getSeparableImageAsDoubleArray(1);
            p.PSF.getSeparableImageAsDoubleArray(2);
            
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
                        if ((1 + eigenLaplacian[i][j] + eigenPSF[0][i][j]) != 0) {
                            temp1[z][i][j] = temp1[z][i][j] / (1 + eigenLaplacian3D[z][i][j] + eigenPSF[z][i][j]);
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
                md.display2regions3D(w3k, "Mask3", channel);
            }
    }

    private void compute_eigenPSF3D() {
        c0 = p.cl[0];
        c1 = p.cl[1];

        int[] sz = p.PSF.getSuggestedImageSize();
        Tools.convolve3Dseparable(eigenPSF, p.PSF.getImage3DAsDoubleArray(), sz[0], sz[1], sz[2], p.PSF, temp4);

        sz = p.PSF.getSuggestedImageSize();
        for (int z = 0; z < sz[2]; z++) {
            for (int i = 0; i < sz[0]; i++) {
                for (int j = 0; j < sz[1]; j++) {
                    temp2[z][i][j] = eigenPSF[z][i][j];
                }
            }
        }

        final int cr = (sz[0] / 2) + 1;
        final int cc = (sz[1] / 2) + 1;
        final int cs = (sz[2] / 2) + 1;

        LocalTools.dctshift3D(temp3, temp2, cr, cc, cs);
        dct3d.forward(temp3, true);
        
        temp1[0][0][0] = 1;
        dct3d.forward(temp1, true);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenPSF[z][i][j] = Math.pow(c1 - c0, 2) * temp3[z][i][j] / temp1[z][i][j];
                }
            }
        }
    }
}
