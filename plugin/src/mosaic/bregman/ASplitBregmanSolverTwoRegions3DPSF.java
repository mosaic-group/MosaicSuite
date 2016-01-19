package mosaic.bregman;


import java.util.Date;
import java.util.concurrent.CountDownLatch;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_3D;


class ASplitBregmanSolverTwoRegions3DPSF extends ASplitBregmanSolver {

    public final double[][][][] w2zk;
    public final double[][][][] b2zk;
    public final double[][][][] ukz;
    public final double[][][] eigenLaplacian3D;
    public final DoubleDCT_3D dct3d;
    public final double[][][] eigenPSF;
    double c0, c1;
    public final double[] energytab2;

    public ASplitBregmanSolverTwoRegions3DPSF(Parameters params, double[][][] image, double[][][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        super(params, image, mask, md, channel, ap);
        this.w2zk = new double[nl][nz][ni][nj];
        this.ukz = new double[nl][nz][ni][nj];
        this.b2zk = new double[nl][nz][ni][nj];
        this.eigenLaplacian3D = new double[nz][ni][nj];
        dct3d = new DoubleDCT_3D(nz, ni, nj);

        for (int i = 0; i < nl; i++) {
            LocalTools.fgradz2D(w2zk[i], mask[i]);
        }

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    this.eigenLaplacian3D[z][i][j] = 2 + (2 - 2 * Math.cos((j) * Math.PI / (nj)) + (2 - 2 * Math.cos((i) * Math.PI / (ni))) + (2 - 2 * Math.cos((z) * Math.PI / (nz))));
                }
            }
        }

        // Beta MLE in and out
        this.c0 = params.cl[0];
        this.c1 = params.cl[1];

        this.energytab2 = new double[p.nthreads];

        final int[] sz = p.PSF.getSuggestedImageSize();
        eigenPSF = new double[Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];

        // Reallocate temps
        // Unfortunatelly is allocated in ASplitBregmanSolver
        this.temp4 = new double[nl][Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        this.temp3 = new double[nl][Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        this.temp2 = new double[nl][Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];
        this.temp1 = new double[nl][Math.max(sz[2], nz)][Math.max(sz[0], ni)][Math.max(sz[1], nj)];

        compute_eigenPSF3D();

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    this.eigenLaplacian3D[z][i][j] = this.eigenLaplacian3D[z][i][j] - 2;
                }
            }
        }

        convolveAndScale(mask[levelOfMask]);
        calculateGradientsXandY(mask);
    }
    
    @Override
    protected void init() {
        convolveAndScale(w3k[levelOfMask]);
        calculateGradientsXandY(w3k);
    }

    private void calculateGradientsXandY(double[][][][] aValues) {
        for (int i = 0; i < nl; i++) {
            LocalTools.fgradx2D(temp1[i], aValues[i]);
            LocalTools.fgrady2D(temp2[i], aValues[i]);
        }
    }

    private void convolveAndScale(double[][][] aValues) {
        Tools.convolve3Dseparable(temp3[levelOfMask], aValues, ni, nj, nz, p.PSF, temp4[levelOfMask]);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[levelOfMask][z][i][j] = (c1 - c0) * temp3[levelOfMask][z][i][j] + c0;
                }
            }
        }
    }

    @Override
    protected void step() throws InterruptedException {
            final long lStartTime = new Date().getTime(); // start time
            
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
            
            final int ichunk = p.ni / p.nthreads;
            final int ilastchunk = p.ni - (p.ni / (p.nthreads)) * (p.nthreads - 1);
            final int jchunk = p.nj / p.nthreads;
            final int jlastchunk = p.nj - (p.nj / (p.nthreads)) * (p.nthreads - 1);
            int iStart = 0;
            int jStart = 0;
            
            // Force the allocation of the buffers internally
            // if you do not do you can have race conditions in the multi thread part
            // DO NOT REMOVE THEM EVEN IF THEY LOOK UNUSEFULL
            p.PSF.getSeparableImageAsDoubleArray(0);
            p.PSF.getSeparableImageAsDoubleArray(1);
            p.PSF.getSeparableImageAsDoubleArray(2);
            
            for (int nt = 0; nt < p.nthreads - 1; nt++) {
                // Check if we can create threads
                final Thread t = new Thread(new ZoneTask3D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, Sync13, Dct, iStart, iStart + ichunk, jStart,
                        jStart + jchunk, nt, this, LocalTools));
                t.start();
            
                iStart += ichunk;
                jStart += jchunk;
            }
            final Thread T_ext = new Thread(new ZoneTask3D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, Sync13, Dct, iStart, iStart + ilastchunk,
                    jStart, jStart + jlastchunk, p.nthreads - 1, this, LocalTools));
            T_ext.start();
            
            Sync4.await();
            
            dct3d.forward(temp1[levelOfMask], true);
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        if ((1 + eigenLaplacian[i][j] + eigenPSF[0][i][j]) != 0) {
                            temp1[levelOfMask][z][i][j] = temp1[levelOfMask][z][i][j] / (1 + eigenLaplacian3D[z][i][j] + eigenPSF[z][i][j]);
                        }
                    }
                }
            }
            dct3d.inverse(temp1[levelOfMask], true);
            
            Dct.countDown();
            
            // do fgradx without parallelization
            LocalTools.fgradx2D(temp4[levelOfMask], temp1[levelOfMask]);
            SyncFgradx.countDown();
            
            ZoneDoneSignal.await();
            
            if (stepk % p.energyEvaluationModulo == 0) {
                energy = 0;
                for (int nt = 0; nt < p.nthreads; nt++) {
                    energy += energytab2[nt];
                }
            }
            
            if (p.livedisplay && p.firstphase) {
                md.display2regions3D(w3k[levelOfMask], "Mask3", channel);
            }
            
            final long lEndTime = new Date().getTime(); // end time
            final long difference = lEndTime - lStartTime; // check different
            totaltime += difference;
    }

    private void compute_eigenPSF3D() {
        this.c0 = p.cl[0];
        this.c1 = p.cl[1];

        int[] sz = p.PSF.getSuggestedImageSize();

        Tools.convolve3Dseparable(eigenPSF, p.PSF.getImage3DAsDoubleArray(), sz[0], sz[1], sz[2], p.PSF, temp4[levelOfMask]);

        sz = p.PSF.getSuggestedImageSize();
        for (int z = 0; z < sz[2]; z++) {
            for (int i = 0; i < sz[0]; i++) {
                for (int j = 0; j < sz[1]; j++) {
                    temp2[levelOfMask][z][i][j] = eigenPSF[z][i][j];
                }
            }
        }

        final int cr = (sz[0] / 2) + 1;
        final int cc = (sz[1] / 2) + 1;
        final int cs = (sz[2] / 2) + 1;

        LocalTools.dctshift3D(temp3[levelOfMask], temp2[levelOfMask], cr, cc, cs);
        dct3d.forward(temp3[levelOfMask], true);
        temp1[levelOfMask][0][0][0] = 1;
        dct3d.forward(temp1[levelOfMask], true);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenPSF[z][i][j] = Math.pow(c1 - c0, 2) * temp3[levelOfMask][z][i][j] / temp1[levelOfMask][z][i][j];
                }
            }
        }
    }
}
