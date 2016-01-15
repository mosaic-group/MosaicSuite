package mosaic.bregman;

class SingleRegionTask /*implements Runnable*/ {
//
//    private final CountDownLatch RegionsTasksDoneSignal;
//    private final CountDownLatch UkDoneSignal;
//    private final CountDownLatch W3kDoneSignal;
//    private final ASplitBregmanSolver AS;
//    private final int l;
//    private final int channel;
//    private final DoubleDCT_2D dct2d;
//    private final Tools Tools;
//
//    SingleRegionTask(CountDownLatch RegionsTasksDoneSignal, CountDownLatch UkDoneSignal, CountDownLatch W3kDoneSignal, int level, int channel, ASplitBregmanSolver AS, Tools tTools) {
//        this.Tools = tTools;
//        this.RegionsTasksDoneSignal = RegionsTasksDoneSignal;
//        this.UkDoneSignal = UkDoneSignal;
//        this.W3kDoneSignal = W3kDoneSignal;
//        this.AS = AS;
//        this.channel = channel;
//        this.l = level;
//        dct2d = new DoubleDCT_2D(AS.ni, AS.nj);
//    }
//
//    @Override
//    public void run() {
//        try {
//            doWork();
//        }
//        catch (final InterruptedException ex) {
//        }
//
//        RegionsTasksDoneSignal.countDown();
//    }
//
//    private void doWork() throws InterruptedException {
//        // IJ.log("thread : " +l +"starting work");
//        // WARNING !! : temp1 and temp2 (resp =w2xk and =w2yk) passed to next iteration : do not change .
//        Tools.subtab(AS.temp1[l], AS.temp1[l], AS.b2xk[l]);
//        Tools.subtab(AS.temp2[l], AS.temp2[l], AS.b2yk[l]);
//
//        // temp3=divwb
//        Tools.mydivergence(AS.temp3[l], AS.temp1[l], AS.temp2[l]);// , temp3[l]);
//
//        // RHS = -divwb+w2k-b2k+w3k-b3k;
//        // temp1=RHS
//        for (int z = 0; z < AS.nz; z++) {
//            for (int i = 0; i < AS.ni; i++) {
//                for (int j = 0; j < AS.nj; j++) {
//                    AS.temp1[l][z][i][j] = -AS.temp3[l][z][i][j] + AS.w1k[l][z][i][j] - AS.b1k[l][z][i][j] + AS.w3k[l][z][i][j] - AS.b3k[l][z][i][j];
//                }
//            }
//        }
//
//        // temp1=uk
//        dct2d.forward(AS.temp1[l][0], true);
//        for (int i = 0; i < AS.ni; i++) {
//            for (int j = 0; j < AS.nj; j++) {
//                if (AS.eigenLaplacian[i][j] != 0) {
//                    AS.temp1[l][0][i][j] = AS.temp1[l][0][i][j] / AS.eigenLaplacian[i][j];
//                }
//            }
//        }
//        dct2d.inverse(AS.temp1[l][0], true);
//
//        Tools.addtab(AS.temp4[l], AS.b3k[l], AS.temp1[l]);
//        UkDoneSignal.countDown();
//
//        // %-- w1k subproblem
//        for (int z = 0; z < AS.nz; z++) {
//            for (int i = 0; i < AS.ni; i++) {
//                for (int j = 0; j < AS.nj; j++) {
//                    AS.w1k[l][z][i][j] = -(AS.p.ldata) * AS.p.gamma * AS.speedData[l][z][i][j] + AS.b1k[l][z][i][j] + AS.temp1[l][z][i][j];
//                }
//            }
//        }
//
//        W3kDoneSignal.await();
//
//        for (int z = 0; z < AS.nz; z++) {
//            for (int i = 0; i < AS.ni; i++) {
//                for (int j = 0; j < AS.nj; j++) {
//                    AS.b1k[l][z][i][j] = AS.b1k[l][z][i][j] + AS.temp1[l][z][i][j] - AS.w1k[l][z][i][j];
//                    AS.b3k[l][z][i][j] = AS.b3k[l][z][i][j] + AS.temp1[l][z][i][j] - AS.w3k[l][z][i][j];
//                }
//            }
//        }
//
//        // %-- w2k sub-problem
//        Tools.fgradx2D(AS.temp3[l], AS.temp1[l]);
//        Tools.fgrady2D(AS.temp4[l], AS.temp1[l]);
//
//        Tools.addtab(AS.temp1[l], AS.temp3[l], AS.b2xk[l]);
//        Tools.addtab(AS.temp2[l], AS.temp4[l], AS.b2yk[l]);
//        // temp1=w2xk temp2=w2yk
//        Tools.shrink2D(AS.temp1[l], AS.temp2[l], AS.temp1[l], AS.temp2[l], AS.p.gamma * AS.p.lreg_[channel]);
//        // do shrink3D
//
//        for (int z = 0; z < AS.nz; z++) {
//            for (int i = 0; i < AS.ni; i++) {
//                for (int j = 0; j < AS.nj; j++) {
//                    AS.b2xk[l][z][i][j] = AS.b2xk[l][z][i][j] + AS.temp3[l][z][i][j] - AS.temp1[l][z][i][j];
//                    AS.b2yk[l][z][i][j] = AS.b2yk[l][z][i][j] + AS.temp4[l][z][i][j] - AS.temp2[l][z][i][j];
//                }
//            }
//        }
//
//        AS.normtab[l] = 0;
//        AS.energytab[l] = Tools.computeEnergy(AS.speedData[l], AS.w3k[l], AS.temp3[l], AS.temp4[l], AS.p.ldata, AS.p.lreg_[channel]);
//    }
}
