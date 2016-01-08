package mosaic.bregman;


import java.util.Date;
import java.util.concurrent.CountDownLatch;


class ASplitBregmanSolverTwoRegions3DPSF extends ASplitBregmanSolverTwoRegions3D {

    public final double[][][] eigenPSF;
    double c0, c1;
    public final double[] energytab2;

    public ASplitBregmanSolverTwoRegions3DPSF(Parameters params, double[][][] image, double[][][][] speedData, double[][][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        super(params, image, speedData, mask, md, channel, ap);

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

        this.compute_eigenPSF3D();

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    this.eigenLaplacian3D[z][i][j] = this.eigenLaplacian3D[z][i][j] - 2;
                }
            }
        }

        Tools.convolve3Dseparable(temp3[l], mask[l], ni, nj, nz, p.PSF, temp4[l]);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[l][z][i][j] = (c1 - c0) * temp3[l][z][i][j] + c0;
                }
            }
        }

        for (int i = 0; i < nl; i++) {
            LocalTools.fgradx2D(temp1[i], mask[i]);
            LocalTools.fgrady2D(temp2[i], mask[i]);
        }
    }

    @Override
    protected void init() {
        this.compute_eigenPSF();

        Tools.convolve3Dseparable(temp3[l], w3k[l], ni, nj, nz, p.PSF, temp4[l]);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[l][z][i][j] = (c1 - c0) * temp3[l][z][i][j] + c0;
                }
            }
        }

        for (int i = 0; i < nl; i++) {
            LocalTools.fgradx2D(temp1[i], w3k[i]);
            LocalTools.fgrady2D(temp2[i], w3k[i]);
        }
    }

    /**
     * Multithread split bregman
     * @throws InterruptedException
     */
    private void step_multit() throws InterruptedException {
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
        final Thread t[] = new Thread[p.nthreads];

        // Force the allocation of the buffers internally
        // if you do not do you can have race conditions in the
        // multi thread part
        // DO NOT REMOVE THEM EVEN IF THEY LOOK UNUSEFULL
        p.PSF.getSeparableImageAsDoubleArray(0);
        p.PSF.getSeparableImageAsDoubleArray(1);
        p.PSF.getSeparableImageAsDoubleArray(2);

        for (int nt = 0; nt < p.nthreads - 1; nt++) {
            // Check if we can create threads
            t[nt] = new Thread(new ZoneTask3D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, Sync13, Dct, iStart, iStart + ichunk, jStart,
                    jStart + jchunk, nt, this, LocalTools));
            t[nt].start();

            iStart += ichunk;
            jStart += jchunk;
        }

        // At least on linux you can go out of memory for threads
        final Thread T_ext = new Thread(new ZoneTask3D(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, Sync13, Dct, iStart, iStart + ilastchunk,
                jStart, jStart + jlastchunk, p.nthreads - 1, this, LocalTools));

        T_ext.start();

        Sync4.await();

        dct3d.forward(temp1[l], true);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if ((1 + eigenLaplacian[i][j] + eigenPSF[0][i][j]) != 0) {
                        temp1[l][z][i][j] = temp1[l][z][i][j] / (1 + eigenLaplacian3D[z][i][j] + eigenPSF[z][i][j]);
                    }
                }
            }
        }
        dct3d.inverse(temp1[l], true);

        Dct.countDown();

        // do fgradx without parallelization
        LocalTools.fgradx2D(temp4[l], temp1[l]);
        SyncFgradx.countDown();

        ZoneDoneSignal.await();

        if (stepk % p.energyEvaluationModulo == 0) {
            energy = 0;
            for (int nt = 0; nt < p.nthreads; nt++) {
                energy += energytab2[nt];
            }
        }

        if (p.livedisplay && p.firstphase) {
            md.display2regions3D(w3k[l], "Mask", channel);
        }

        final long lEndTime = new Date().getTime(); // end time
        final long difference = lEndTime - lStartTime; // check different
        totaltime += difference;
    }

    /**
     * Single thread split Bregman
     */
    private void step_single() {
        final long lStartTime = new Date().getTime(); // start time

        final int ilastchunk = p.ni;
        final int jlastchunk = p.nj;
        final int iStart = 0;
        final int jStart = 0;


        // At least on linux you can go out of memory for threads
        final ZoneTask3D zt = new ZoneTask3D(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, iStart, iStart + ilastchunk, jStart, jStart + jlastchunk,
                p.nthreads - 1, this, LocalTools);

        zt.run();

        if (stepk % p.energyEvaluationModulo == 0) {
            energy = 0;
            for (int nt = 0; nt < p.nthreads; nt++) {
                energy += energytab2[nt];
            }
        }

        if (p.livedisplay && p.firstphase) {
            md.display2regions3D(w3k[l], "Mask", channel);
        }

        final long lEndTime = new Date().getTime(); // end time
        final long difference = lEndTime - lStartTime; // check different
        totaltime += difference;
    }

    @Override
    protected void step() throws InterruptedException {
        if (p.nthreads == 1) {
            step_single();
        }
        else {
            step_multit();
        }
    }

    private void compute_eigenPSF3D() {
        this.c0 = p.cl[0];
        this.c1 = p.cl[1];

        int[] sz = p.PSF.getSuggestedImageSize();

        Tools.convolve3Dseparable(eigenPSF, p.PSF.getImage3DAsDoubleArray(), sz[0], sz[1], sz[2], p.PSF, temp4[l]);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    temp2[l][z][i][j] = 0;
                }
            }
        }

        sz = p.PSF.getSuggestedImageSize();
        for (int z = 0; z < sz[2]; z++) {
            for (int i = 0; i < sz[0]; i++) {
                for (int j = 0; j < sz[1]; j++) {
                    temp2[l][z][i][j] = eigenPSF[z][i][j];
                }
            }
        }

        final int cr = (sz[0] / 2) + 1;
        final int cc = (sz[1] / 2) + 1;
        final int cs = (sz[2] / 2) + 1;

        // temp1 = e1
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    temp1[l][z][i][j] = 0;
                }
            }
        }

        temp1[l][0][0][0] = 1;
        LocalTools.dctshift3D(temp3[l], temp2[l], cr, cc, cs);
        dct3d.forward(temp3[l], true);
        dct3d.forward(temp1[l], true);

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenPSF[z][i][j] = Math.pow(c1 - c0, 2) * temp3[l][z][i][j] / temp1[l][z][i][j];
                } //
            }
        }
    }
}
