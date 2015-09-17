package mosaic.bregman;


import java.util.Date;
import java.util.concurrent.CountDownLatch;


class ASplitBregmanSolverTwoRegionsPSF extends ASplitBregmanSolverTwoRegions {

    public double[][][] eigenPSF;
    double c0, c1;
    public double energytab2[];

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

        // IJ.log("init");
        // IJ.log("init c0 " + c0 + "c1 " + c1);
        Tools.convolve2D(temp3[l][0], w3k[l][0], ni, nj, p.PSF);
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    w1k[l][z][i][j] = (c1 - c0) * temp3[l][z][i][j] + c0;
                    // w1k[l][z][i][j]=(Ri[0][z][i][j]-Ro[0][z][i][j])*temp3[l][z][i][j] + Ro[0][z][i][j];
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

        long lStartTime = new Date().getTime(); // start time
        // energy=0;

        this.c0 = p.cl[0];
        this.c1 = p.cl[1];

        // IJ.log("creates latch ");
        CountDownLatch ZoneDoneSignal = new CountDownLatch(p.nthreads);// subprob 1 and 3
        CountDownLatch Sync1 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync2 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync3 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync4 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync5 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync6 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync7 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync8 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync9 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync10 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync11 = new CountDownLatch(p.nthreads);
        CountDownLatch Sync12 = new CountDownLatch(p.nthreads);
        CountDownLatch Dct = new CountDownLatch(1);

        int ichunk = p.ni / p.nthreads;
        int ilastchunk = p.ni - (p.ni / (p.nthreads)) * (p.nthreads - 1);
        int jchunk = p.nj / p.nthreads;
        int jlastchunk = p.nj - (p.nj / (p.nthreads)) * (p.nthreads - 1);
        int iStart = 0;
        int jStart = 0;

        // Force the allocation of the buffers internally
        // if you do not do you can have race conditions in the
        // multi thread part
        // DO NOT REMOVE THEM EVEN IF THEY LOOK UNUSEFULL

        @SuppressWarnings("unused")
        double kernelx[] = p.PSF.getSeparableImageAsDoubleArray(0);
        @SuppressWarnings("unused")
        double kernely[] = p.PSF.getSeparableImageAsDoubleArray(1);

        for (int nt = 0; nt < p.nthreads - 1; nt++) {
            // IJ.log("thread + istart iend jstart jend"+
            // iStart +" " + (iStart+ichunk)+" " + jStart+" " + (jStart+jchunk));

            new Thread(new ZoneTask(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ichunk, jStart, jStart + jchunk, nt,
                    this, LocalTools)).start();
            iStart += ichunk;
            jStart += jchunk;
        }
        // IJ.log("last thread + istart iend jstart jend"+
        // iStart +" " + (iStart+ilastchunk)+" " + jStart+" " + (jStart+jlastchunk));
        new Thread(new ZoneTask(ZoneDoneSignal, Sync1, Sync2, Sync3, Sync4, Dct, Sync5, Sync6, Sync7, Sync8, Sync9, Sync10, Sync11, Sync12, iStart, iStart + ilastchunk, jStart, jStart + jlastchunk,
                p.nthreads - 1, this, LocalTools)).start();

        // IJ.log("thread : " +l +"starting work");
        // temp1 =w1xk, temp2 = w2yk

        // Tools.subtab(temp1[l], temp1[l], b2xk[l]);
        // Tools.subtab(temp2[l], temp2[l], b2yk[l]);

        // temp3=divwb
        // Tools.mydivergence(temp3[l], temp1[l], temp2[l]);//, temp3[l]);

        // RHS = -divwb+w2k-b2k+w3k-b3k;

        // for (int z=0; z<nz; z++){
        // for (int i=0; i<ni; i++) {
        // for (int j=0; j<nj; j++){
        // temp2[l][z][i][j]=w1k[l][z][i][j]-b1k[l][z][i][j] -c0;
        // }
        // }
        // }

        // Tools.convolve2Dseparable(temp4[l][0], temp2[l][0], ni, nj, p.kernelx, p.kernely, p.px, p.py, temp1[l][0]);

        // temp1=RHS
        // for (int z=0; z<nz; z++){
        // for (int i=0; i<ni; i++) {
        // for (int j=0; j<nj; j++) {
        // temp1[l][z][i][j]=-temp3[l][z][i][j]+w3k[l][z][i][j]-b3k[l][z][i][j] + (c1-c0)*temp4[l][z][i][j];
        // }
        // }
        // }

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

        // //temp2=muk
        //
        // Tools.convolve2Dseparable(temp2[l][0], temp1[l][0], ni, nj, p.kernelx, p.kernely, p.px, p.py, temp3[l][0]);
        // for (int i=0; i<ni; i++) {
        // for (int j=0; j<nj; j++) {
        // temp2[l][0][i][j]=(c1-c0)*temp2[l][0][i][j] + c0;
        // }
        // }
        //
        //
        // //%-- w1k subproblem
        // //temp3=detw2
        // // detw2 = (lambda*gamma.*weightData-b2k-muk).^2+4*lambda*gamma*weightData.*image;
        //
        //
        // for (int z=0; z<nz; z++){
        // for (int i=0; i<ni; i++) {
        // for (int j=0; j<nj; j++) {
        // temp3[l][0][i][j]=
        // Math.pow(((p.ldata/p.lreg)*p.gamma -b1k[l][0][i][j] - temp2[l][0][i][j]),2)
        // +4*(p.ldata/p.lreg)*p.gamma*image[0][i][j];
        // }
        // }
        // }
        //
        // //w2k = 0.5*(b2k+muk-lambda*gamma.*weightData+sqrt(detw2));
        //
        // for (int z=0; z<nz; z++){
        // for (int i=0; i<ni; i++) {
        // for (int j=0; j<nj; j++){
        // w1k[l][0][i][j]=0.5*(b1k[l][z][i][j] + temp2[l][z][i][j]- (p.ldata/p.lreg)*p.gamma + Math.sqrt(temp3[l][z][i][j]));
        // }
        // }
        // }
        //
        //
        //
        // //%-- w3k subproblem
        // for (int z=0; z<nz; z++){
        // for (int i=0; i<ni; i++) {
        // for (int j=0; j<nj; j++) {
        // w3k[l][z][i][j]=Math.max(Math.min(temp1[l][z][i][j]+ b3k[l][z][i][j],1),0);
        // }
        // }
        // }
        //
        // for (int z=0; z<nz; z++){
        // for (int i=0; i<ni; i++) {
        // for (int j=0; j<nj; j++) {
        // b1k[l][z][i][j]=b1k[l][z][i][j] +temp2[l][z][i][j]-w1k[l][z][i][j];
        // b3k[l][z][i][j]=b3k[l][z][i][j] +temp1[l][z][i][j]-w3k[l][z][i][j];
        // }
        // }
        // }
        //

        ZoneDoneSignal.await();

        // %-- w2k sub-problem
        // temp4=ukx, temp3=uky
        // Tools.fgradx2D(temp3[l], temp1[l]);
        // Tools.fgrady2D(temp4[l], temp1[l]);
        //

        // Tools.addtab(temp1[l], temp3[l], b2xk[l]);
        // Tools.addtab(temp2[l], temp4[l], b2yk[l]);
        // temp1 = w1xk temp2 = w2yk
        // Tools.shrink2D(temp1[l], temp2[l], temp1[l], temp2[l], p.gamma);

        // for (int z=0; z<nz; z++){
        // for (int i=0; i<ni; i++) {
        // for (int j=0; j<nj; j++) {
        // b2xk[l][z][i][j]=b2xk[l][z][i][j] +temp3[l][z][i][j]-temp1[l][z][i][j];
        // b2yk[l][z][i][j]=b2yk[l][z][i][j] +temp4[l][z][i][j]-temp2[l][z][i][j];
        // //mask[l][z][i][j]=w3k[l][z][i][j];
        // }
        // }
        // }

        // WARNING !! : temp1 and temp2 (resp =w2xk and =w2yk) passed to next iteration : do not change .
        // Tools.disp_vals(b1k[l][0], "b1k");

        // normtab[l]=0;
        // for (int z=0; z<nz; z++){
        // for (int i=0; i<ni; i++) {
        // for (int j=0; j<nj; j++) {
        // // l2normtab[l]+=Math.sqrt(Math.pow(w3k[l][z][i][j]-w3kp[l][z][i][j],2));
        // normtab[l]+=Math.abs(w3k[l][z][i][j]-w3kp[l][z][i][j]);
        // }
        // }
        // }

        // Tools.copytab(w3kp[l], w3k[l]);

        // energytab[l]=Tools.computeEnergyPSF(speedData[l], w3k[l], temp3[l], temp4[l], p.ldata, p.lreg,p,c0,c1,image);

        if (stepk % p.energyEvaluationModulo == 0) {
            energy = 0;
            for (int nt = 0; nt < p.nthreads; nt++) {
                energy += energytab2[nt];
            }
        }

        // Tools.max_mask(maxmask, w3k);

        // doneSignal2.await();

        // energy=energytab[l];
        // norm=Math.max(norm, normtab[l]);
        // Tools.max_mask(maxmask, w3k);
        // Tools.disp_array(w3k[l][0], "mask");
        if (p.livedisplay && p.firstphase) {
            md.display2regions(w3k[l][0], "Mask", channel);
        }

        long lEndTime = new Date().getTime(); // end time

        long difference = lEndTime - lStartTime; // check different
        totaltime += difference;
        // IJ.log("Elapsed milliseconds: " + difference);

    }

    @Override
    public void compute_eigenPSF() {
        this.c0 = p.cl[0];
        this.c1 = p.cl[1];

        int[] sz = p.PSF.getSuggestedImageSize();
        int xmin = Math.min(sz[0], eigenPSF[0].length);
        int ymin = Math.min(sz[1], eigenPSF[0][0].length);

        // PSF2 = imfilter(PSF,PSF,'symmetric');
        // IJ.log("avant xmin "+ xmin + "ymin" + ymin);
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

        int cc = (sz[0] / 2) + 1;
        int cr = (sz[1] / 2) + 1;

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
                    // eigenPSF[z][i][j]=Math.pow(Ri[0][z][i][j]-Ro[0][z][i][j],2)*temp3[l][z][i][j]/temp2[l][z][i][j];

                } //
            }
        }

        // PSF2 = imfilter(PSF,PSF,'symmetric');
        // paddedPSF = padPSF(PSF2,dims);
        // center = ceil(size(PSF2)/2);
        // e1 = zeros(size(paddedPSF)); e1(1,1)=1;
        // S = dct2(dctshift(paddedPSF,center))./dct2(e1);
        // eigenPSF = (betaMLE_in-betaMLE_out)^2*S;

    }

}
