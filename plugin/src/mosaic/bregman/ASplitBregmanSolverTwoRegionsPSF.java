package mosaic.bregman;


import java.util.Date;
import java.util.concurrent.CountDownLatch;


class ASplitBregmanSolverTwoRegionsPSF extends ASplitBregmanSolverTwoRegions {

    private final double[][][] eigenPSF;
    double c0, c1;
    public final double energytab2[];

    public ASplitBregmanSolverTwoRegionsPSF(Parameters params, double[][][] image, double[][][][] speedData, double[][][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        super(params, image, speedData, mask, md, channel, ap);
        this.c0 = params.cl[0];
        this.c1 = params.cl[1];
        eigenPSF = new double[nz][ni][nj];
        this.compute_eigenPSF();
        this.energytab2 = new double[p.nthreads];

        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                this.eigenLaplacian[i][j] = this.eigenLaplacian[i][j] - 2;
            }
        }

        Tools.convolve2D(temp3[l][0], mask[l][0], ni, nj, p.PSF);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[l][z][i][j] = (c1 - c0) * temp3[l][z][i][j] + c0;
                }
            }
        }

        for (int i = 0; i < nl; i++) {
            LocalTools.fgradx2D(w2xk[i], mask[i]);
            LocalTools.fgrady2D(w2yk[i], mask[i]);
        }
    }

    @Override
    protected void init() {
        this.compute_eigenPSF();

        Tools.convolve2D(temp3[l][0], w3k[l][0], ni, nj, p.PSF);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[l][z][i][j] = (c1 - c0) * temp3[l][z][i][j] + c0;
                }
            }
        }

        for (int i = 0; i < nl; i++) {
            LocalTools.fgradx2D(w2xk[i], w3k[i]);
            LocalTools.fgrady2D(w2yk[i], w3k[i]);
        }
    }

    @Override
    protected void step() throws InterruptedException {
        // WARNING !! : temp1 and temp2 (resp =w2xk and =w2yk) passed from iteration to next iteration : do not change .

        final long lStartTime = new Date().getTime(); // start time

        this.c0 = p.cl[0];
        this.c1 = p.cl[1];

        // IJ.log("creates latch ");
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

        // Force the allocation of the buffers internally
        // if you do not do you can have race conditions in the
        // multi thread part
        // DO NOT REMOVE THEM EVEN IF THEY LOOK UNUSEFULL
        p.PSF.getSeparableImageAsDoubleArray(0);
        p.PSF.getSeparableImageAsDoubleArray(1);

        for (int nt = 0; nt < p.nthreads - 1; nt++) {
            new Thread(new ZoneTask(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ichunk, jStart, jStart + jchunk, nt,
                    this, LocalTools)).start();
            iStart += ichunk;
            jStart += jchunk;
        }
        new Thread(new ZoneTask(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ilastchunk, jStart, jStart + jlastchunk,
                p.nthreads - 1, this, LocalTools)).start();

        // temp1=uk
        Sync4.await();

        // Check match here
        dct2d.forward(temp1[l][0], true);

        // inversion int DCT space
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                if ((1 + eigenLaplacian[i][j] + eigenPSF[0][i][j]) != 0) {
                    temp1[l][0][i][j] = temp1[l][0][i][j] / (1 + eigenLaplacian[i][j] + eigenPSF[0][i][j]);
                }
            }
        }

        dct2d.inverse(temp1[l][0], true);
        Dct.countDown();
        ZoneDoneSignal.await();

        if (stepk % p.energyEvaluationModulo == 0) {
            energy = 0;
            for (int nt = 0; nt < p.nthreads; nt++) {
                energy += energytab2[nt];
            }
        }

        if (p.livedisplay && p.firstphase) {
            md.display2regions(w3k[l][0], "Mask", channel);
        }

        final long lEndTime = new Date().getTime(); // end time

        final long difference = lEndTime - lStartTime; // check different
        totaltime += difference;
    }

    @Override
    public void compute_eigenPSF() {
        this.c0 = p.cl[0];
        this.c1 = p.cl[1];

        final int[] sz = p.PSF.getSuggestedImageSize();
        final int xmin = Math.min(sz[0], eigenPSF[0].length);
        final int ymin = Math.min(sz[1], eigenPSF[0][0].length);

        Tools.convolve2D(eigenPSF[0], p.PSF.getImage2DAsDoubleArray(), xmin, ymin, p.PSF);

        // paddedPSF = padPSF(PSF2,dims);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    temp1[l][z][i][j] = 0;
                }
            }
        }

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < xmin; i++) {
                for (int j = 0; j < ymin; j++) {
                    temp1[l][z][i][j] = eigenPSF[z][i][j];
                }
            }
        }

        final int cc = (sz[0] / 2) + 1;
        final int cr = (sz[1] / 2) + 1;

        // temp1 = e1
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    temp2[l][z][i][j] = 0;
                }
            }
        }

        // IJ.log("cr " + cr + "cc " + cc );
        temp2[l][0][0][0] = 1;

        LocalTools.dctshift(temp3[l], temp1[l], cc, cr);
        dct2d.forward(temp3[l][0], true);
        dct2d.forward(temp2[l][0], true);

        // IJ.log("c0 " + c0 + "c1 " + c1);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    eigenPSF[z][i][j] = Math.pow(c1 - c0, 2) * temp3[l][z][i][j] / temp2[l][z][i][j];
                } 
            }
        }
    }
}
