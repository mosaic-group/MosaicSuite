package mosaic.bregman.segmentation.solver;


import java.util.concurrent.CountDownLatch;

import mosaic.bregman.segmentation.solver.SolverParameters.NoiseModel;


class ZoneTask2D implements Runnable {

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
    
    private final CountDownLatch Dct;
    private final ASplitBregmanSolver2D AS;
    private final int iStart, iEnd, jStart, jEnd, nt;
    private final SolverTools LocalTools;
    private final boolean iEvaluateEnergy;
    
    ZoneTask2D(CountDownLatch ZoneDoneSignal, CountDownLatch Sync1, CountDownLatch Sync2, CountDownLatch Sync3, CountDownLatch Sync4, CountDownLatch Dct, CountDownLatch Sync5, CountDownLatch Sync6,
            CountDownLatch Sync7, CountDownLatch Sync8, CountDownLatch Sync9, CountDownLatch Sync10, CountDownLatch Sync11, CountDownLatch Sync12, int iStart, int iEnd, int jStart, int jEnd, int num,
            ASplitBregmanSolver2D AS, SolverTools tTools, boolean aEvaluateEnergy) {
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
        
        this.Dct = Dct;
        this.AS = AS;
        this.nt = num;
        this.iStart = iStart;
        this.jStart = jStart;
        this.iEnd = iEnd;
        this.jEnd = jEnd;
        iEvaluateEnergy = aEvaluateEnergy;
    }

    @Override
    public void run() {
        try {
            doWork();
        }
        catch (final InterruptedException ex) {}

        ZoneDoneSignal.countDown();
    }

    private void doWork() throws InterruptedException {
        LocalTools.subtab(AS.temp1, AS.w2xk, AS.b2xk, iStart, iEnd);
        LocalTools.subtab(AS.temp2, AS.w2yk, AS.b2yk, iStart, iEnd);

        
        SolverTools.synchronizedWait(Sync1);

        
        LocalTools.mydivergence(AS.temp3, AS.temp1, AS.temp2, AS.temp4, Sync2, iStart, iEnd, jStart, jEnd);

        SolverTools.synchronizedWait(Sync12);

        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.temp2[z][i][j] = AS.w1k[z][i][j] - AS.b1k[z][i][j] - AS.iBetaMleOut;
                }
            }
        }
        
        SolverTools.synchronizedWait(Sync3);

        SolverTools.convolve2Dseparable(AS.temp4[0], AS.temp2[0], AS.ni, AS.nj, AS.iPsf, AS.temp1[0], iStart, iEnd);

        SolverTools.synchronizedWait(Sync11);

        for (int i = iStart; i < iEnd; i++) {
            for (int j = 0; j < AS.nj; j++) {
                AS.temp1[0][i][j] = -AS.temp3[0][i][j] + AS.w3k[0][i][j] - AS.b3k[0][i][j] + (AS.iBetaMleIn - AS.iBetaMleOut) * AS.temp4[0][i][j];
            }
        }

        Sync4.countDown();
        
        Dct.await();

        SolverTools.convolve2Dseparable(AS.temp2[0], AS.temp1[0], AS.ni, AS.nj, AS.iPsf, AS.temp3[0], iStart, iEnd);

        SolverTools.synchronizedWait(Sync10);

        
        for (int i = iStart; i < iEnd; i++) {
            for (int j = 0; j < AS.nj; j++) {
                AS.temp2[0][i][j] = (AS.iBetaMleIn - AS.iBetaMleOut) * AS.temp2[0][i][j] + AS.iBetaMleOut;
            }
        }

        
        // %-- w1k subproblem
        if (AS.iParameters.noiseModel == NoiseModel.POISSON) {
            // poisson
            // temp3=detw2
            // detw2 = (lambda*gamma.*weightData-b2k-muk).^2+4*lambda*gamma*weightData.*image;
            for (int z = 0; z < AS.nz; z++) {
                for (int i = iStart; i < iEnd; i++) {
                    for (int j = 0; j < AS.nj; j++) {
                        AS.temp3[0][i][j] = Math.pow(((AS.iParameters.lambdaData / AS.iRegularization) * AS.iParameters.gamma - AS.b1k[0][i][j] - AS.temp2[0][i][j]), 2) + 
                                         4 * (AS.iParameters.lambdaData / AS.iRegularization) * AS.iParameters.gamma * AS.iImage[0][i][j];
                    }
                }
            }
            for (int z = 0; z < AS.nz; z++) {
                for (int i = iStart; i < iEnd; i++) {
                    for (int j = 0; j < AS.nj; j++) {
                        AS.w1k[0][i][j] = 0.5 * (AS.b1k[z][i][j] + AS.temp2[z][i][j] - (AS.iParameters.lambdaData / AS.iRegularization) * AS.iParameters.gamma + Math.sqrt(AS.temp3[z][i][j]));
                    }
                }
            }
        }
        else {
            // gaussian
            // w2k = (b2k+muk+2*lambda*gamma*weightData.*image)./(1+2*lambda*gamma*weightData);
            for (int z = 0; z < AS.nz; z++) {
                for (int i = iStart; i < iEnd; i++) {
                    for (int j = 0; j < AS.nj; j++) {
                        AS.w1k[0][i][j] = (AS.b1k[z][i][j] + AS.temp2[z][i][j] + 2 * (AS.iParameters.lambdaData / AS.iRegularization) * AS.iParameters.gamma * AS.iImage[0][i][j])
                                / (1 + 2 * (AS.iParameters.lambdaData / AS.iRegularization) * AS.iParameters.gamma);
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

        SolverTools.synchronizedWait(Sync5);

        LocalTools.fgradx2D(AS.temp3, AS.temp1, jStart, jEnd);
        LocalTools.fgrady2D(AS.temp4, AS.temp1, iStart, iEnd);

        
        SolverTools.synchronizedWait(Sync6);

        LocalTools.addtab(AS.w2xk, AS.temp3, AS.b2xk, iStart, iEnd);
        LocalTools.addtab(AS.w2yk, AS.temp4, AS.b2yk, iStart, iEnd);
        LocalTools.shrink2D(AS.w2xk, AS.w2yk, AS.w2xk, AS.w2yk, AS.iParameters.gamma, iStart, iEnd);
        
        
        for (int z = 0; z < AS.nz; z++) {
            for (int i = iStart; i < iEnd; i++) {
                for (int j = 0; j < AS.nj; j++) {
                    AS.b2xk[z][i][j] = AS.b2xk[z][i][j] + AS.temp3[z][i][j] - AS.w2xk[z][i][j];
                    AS.b2yk[z][i][j] = AS.b2yk[z][i][j] + AS.temp4[z][i][j] - AS.w2yk[z][i][j];
                    
                }
            }
        }

        SolverTools.synchronizedWait(Sync7);

        // faire le menage dans les tableaux ici w2xk utilise comme temp
        // Google translation: do the household in here w2xk tables used as Temp
        if (iEvaluateEnergy) {
            AS.iEnergies[nt] = LocalTools.computeEnergyPSF(AS.temp1, AS.w3k, AS.temp3, AS.temp4, AS.iParameters.lambdaData, AS.iRegularization, AS.iPsf, AS.iBetaMleOut, AS.iBetaMleIn, AS.iImage, iStart,
                    iEnd, jStart, jEnd, Sync8, Sync9, AS.iNoiseModel);
        }
    }
}
