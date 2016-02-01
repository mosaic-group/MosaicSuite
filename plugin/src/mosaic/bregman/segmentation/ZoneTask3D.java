package mosaic.bregman.segmentation;


import java.util.concurrent.CountDownLatch;


class ZoneTask3D implements Runnable {

    private final CountDownLatch ZoneDoneSignal;
    private final CountDownLatch Sync1;
    private final CountDownLatch Sync2;
    private final CountDownLatch Sync3;
    private final CountDownLatch Sync4;
    private final CountDownLatch Sync5;
    private final CountDownLatch Sync6;
    private final CountDownLatch Sync7;
    private final CountDownLatch Sync8;
    private final CountDownLatch Sync9;
    private final CountDownLatch Sync10;
    private final CountDownLatch Sync11;
    private final CountDownLatch Sync12;
    private final CountDownLatch Sync13;
    private final CountDownLatch Dct;
    private final int iStart, iEnd, jStart, jEnd, nt;
    private final Tools LocalTools;
    private final ASplitBregmanSolverTwoRegions3DPSF AS;
    private final boolean iEvaluateEnergy;
    private final boolean iLastIteration;
    
    ZoneTask3D(CountDownLatch ZoneDoneSignal, CountDownLatch Sync1, CountDownLatch Sync2, CountDownLatch Sync3, CountDownLatch Sync4, CountDownLatch Sync5, CountDownLatch Sync6, CountDownLatch Sync7,
            CountDownLatch Sync8, CountDownLatch Sync9, CountDownLatch Sync10, CountDownLatch Sync11, CountDownLatch Sync12, CountDownLatch Sync13, CountDownLatch Dct, int iStart, int iEnd,
            int jStart, int jEnd, int nt, ASplitBregmanSolverTwoRegions3DPSF AS, Tools tTools, boolean aEvaluateEnergy, boolean aLastIteration) {
        this.LocalTools = tTools;
        this.ZoneDoneSignal = ZoneDoneSignal;
        this.Sync1 = Sync1;
        this.Sync2 = Sync2;
        this.Sync3 = Sync3;
        this.Sync4 = Sync4;
        this.Sync5 = Sync5;
        this.Sync6 = Sync6;
        this.Sync7 = Sync7;
        this.Sync8 = Sync8;
        this.Sync9 = Sync9;
        this.Sync10 = Sync10;
        this.Sync11 = Sync11;
        this.Sync12 = Sync12;
        this.Sync13 = Sync13;
        this.Dct = Dct;
        this.AS = AS;
        this.nt = nt;
        this.iStart = iStart;
        this.jStart = jStart;
        this.iEnd = iEnd;
        this.jEnd = jEnd;
        iEvaluateEnergy = aEvaluateEnergy;
        iLastIteration = aLastIteration;
    }

    @Override
    public void run() {
        try {
            doWork();
        }
        catch (final InterruptedException ex) {
        }

        ZoneDoneSignal.countDown();
    }

    private void doWork() throws InterruptedException {
        LocalTools.subtab(AS.temp1, AS.temp1, AS.b2xk, iStart, iEnd);
        LocalTools.subtab(AS.temp2, AS.temp2, AS.b2yk, iStart, iEnd);
        LocalTools.subtab(AS.temp4, AS.w2zk, AS.b2zk, iStart, iEnd);

        Tools.synchronizedWait(Sync1);

        // use w2zk as temp
        LocalTools.mydivergence3D(AS.temp3, AS.temp1, AS.temp2, AS.temp4, AS.w2zk, Sync2, iStart, iEnd, jStart, jEnd);// , temp3[l]);

        Tools.synchronizedWait(Sync12);

        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.temp2[z][i][j] = AS.w1k[z][i][j] - AS.b1k[z][i][j] - AS.iBetaMleOut;
                }
            }
        }

        Tools.synchronizedWait(Sync3);

        Tools.convolve3Dseparable(AS.temp4, AS.temp2, AS.ni, AS.nj, AS.nz, AS.iPsf, AS.temp1, iStart, iEnd);

        Tools.synchronizedWait(Sync11);

        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.temp1[z][i][j] = -AS.temp3[z][i][j] + AS.w3k[z][i][j] - AS.b3k[z][i][j] + (AS.iBetaMleIn - AS.iBetaMleOut) * AS.temp4[z][i][j];
                }
            }
        }

        Sync4.countDown();

        Dct.await();
 

        Tools.convolve3Dseparable(AS.temp2, AS.temp1, AS.ni, AS.nj, AS.nz, AS.iPsf, AS.temp3, iStart, iEnd);

        Tools.synchronizedWait(Sync10);

        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.temp2[z][i][j] = (AS.iBetaMleIn - AS.iBetaMleOut) * AS.temp2[z][i][j] + AS.iBetaMleOut;
                }
            }
        }

        // %-- w1k subproblem
        if (AS.iParameters.noise_model == 0) {
            // poisson
            for (int z = 0; z < AS.nz; z++) {
                for (int i = iStart; i < iEnd; i++) {
                    for (int j = 0; j < AS.nj; j++) {
                        AS.temp3[z][i][j] = Math.pow(((AS.iParameters.ldata / AS.lreg_) * AS.iParameters.gamma - AS.b1k[z][i][j] - AS.temp2[z][i][j]), 2) + 4
                                * (AS.iParameters.ldata / AS.lreg_) * AS.iParameters.gamma * AS.image[z][i][j];
                    }
                }
            }
            for (int z = 0; z < AS.nz; z++) {
                for (int i = iStart; i < iEnd; i++) {
                    for (int j = 0; j < AS.nj; j++) {
                        AS.w1k[z][i][j] = 0.5 * (AS.b1k[z][i][j] + AS.temp2[z][i][j] - (AS.iParameters.ldata / AS.lreg_) * AS.iParameters.gamma + Math.sqrt(AS.temp3[z][i][j]));
                    }
                }
            }
        }
        else {
            // gaussian
            for (int z = 0; z < AS.nz; z++) {
                for (int i = iStart; i < iEnd; i++) {
                    for (int j = 0; j < AS.nj; j++) {
                        AS.w1k[0][i][j] = (AS.b1k[z][i][j] + AS.temp2[z][i][j] + 2 * (AS.iParameters.ldata / AS.lreg_) * AS.iParameters.gamma * AS.image[0][i][j])
                                / (1 + 2 * (AS.iParameters.ldata / AS.lreg_) * AS.iParameters.gamma);
                    }
                }
            }
        }
        // %-- w3k subproblem
        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.w3k[z][i][j] = Math.max(Math.min(AS.temp1[z][i][j] + AS.b3k[z][i][j], 1), 0);
                }
            }
        }

        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.b1k[z][i][j] = AS.b1k[z][i][j] + AS.temp2[z][i][j] - AS.w1k[z][i][j];
                    AS.b3k[z][i][j] = AS.b3k[z][i][j] + AS.temp1[z][i][j] - AS.w3k[z][i][j];
                }
            }
        }

        Tools.synchronizedWait(Sync5);

        LocalTools.fgradx2D(AS.temp3, AS.temp1, jStart, jEnd);
        LocalTools.fgrady2D(AS.temp4, AS.temp1, iStart, iEnd);
        LocalTools.fgradz2D(AS.ukz, AS.temp1, iStart, iEnd);

        Tools.synchronizedWait(Sync6);
        
        LocalTools.addtab(AS.temp1, AS.temp3, AS.b2xk, iStart, iEnd);
        LocalTools.addtab(AS.temp2, AS.temp4, AS.b2yk, iStart, iEnd);
        LocalTools.addtab(AS.w2zk, AS.ukz, AS.b2zk, iStart, iEnd);

        LocalTools.shrink3D(AS.temp1, AS.temp2, AS.w2zk, AS.temp1, AS.temp2, AS.w2zk, AS.iParameters.gamma, iStart, iEnd);

        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.b2xk[z][i][j] = AS.b2xk[z][i][j] + AS.temp3[z][i][j] - AS.temp1[z][i][j];
                    AS.b2yk[z][i][j] = AS.b2yk[z][i][j] + AS.temp4[z][i][j] - AS.temp2[z][i][j];
                    AS.b2zk[z][i][j] = AS.b2zk[z][i][j] + AS.ukz[z][i][j] - AS.w2zk[z][i][j];
                }
            }
        }

        Tools.synchronizedWait(Sync7);

        // faire le menage dans les tableaux ici w2xk utilise comme temp
        // Google translation: do the household in here w2xk tables used as Temp
        if (iEvaluateEnergy || iLastIteration) {
            AS.energytab2[nt] = LocalTools.computeEnergyPSF3D(AS.w2xk, AS.w3k, AS.temp3, AS.temp4, AS.iParameters.ldata, AS.lreg_, AS.iPsf, AS.iBetaMleOut, AS.iBetaMleIn, AS.image, iStart,
                    iEnd, jStart, jEnd, Sync8, Sync9, Sync13, AS.iNoiseModel);
        }
    }
}
