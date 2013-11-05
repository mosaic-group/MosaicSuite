

package mosaic.bregman;


import java.util.concurrent.CountDownLatch;



public class ZoneTask implements Runnable {
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
	private final CountDownLatch Dct;
	private ASplitBregmanSolverTwoRegionsPSF AS;
	private int iStart, iEnd, jStart, jEnd ;
	private int num;	
	public Tools LocalTools;

	ZoneTask(CountDownLatch ZoneDoneSignal,CountDownLatch Sync1,CountDownLatch Sync2,
			CountDownLatch Sync3,CountDownLatch Sync4,CountDownLatch Dct,CountDownLatch Sync5,CountDownLatch Sync6,
			CountDownLatch Sync7,CountDownLatch Sync8,CountDownLatch Sync9,
			int iStart, int iEnd, int jStart, int jEnd,int num, ASplitBregmanSolverTwoRegionsPSF AS, Tools tTools) {
		this.LocalTools=tTools;
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
		this.num=num;
		this.Dct = Dct;
		this.AS=AS;
		this.iStart=iStart;
		this.jStart=jStart;
		this.iEnd=iEnd;
		this.jEnd=jEnd;

	}


	public void run() {
		try {
			doWork();
		}catch (InterruptedException ex) {}

		ZoneDoneSignal.countDown();
	}

	void doWork() throws InterruptedException{
		// IJ.log("thread : " +l +"starting work");
		//		double c0, c1;
		//IJ.log("istart " + iStart +"iend" + iEnd);
		
		
		double c0t=AS.c0;
		double iDiff=AS.c1-AS.c0;
		double [][] tmp1t;
		double [][] tmp2t;
		double [][] tmp3t;
		double [][] tmp4t;
		double [][] b1kt;
		double [][] b3kt;
		double [][] w1kt;
		double [][] w3kt;
		double [][] imgt;
		double [][] b2xkt;
		double [][] b2ykt;
		double [][] w2xkt;
		double [][] w2ykt;
		
		LocalTools.subtab(AS.temp1[AS.l], AS.w2xk[AS.l], AS.b2xk[AS.l], iStart, iEnd);
		LocalTools.subtab(AS.temp2[AS.l], AS.w2yk[AS.l], AS.b2yk[AS.l], iStart, iEnd);

		//synchro
		Sync1.countDown();
		Sync1.await();


		//	IJ.log("thread + istart iend jstart jend"+
		//	iStart +" " + iEnd+" " + jStart+" " + jEnd);
		LocalTools.mydivergence(AS.temp3[AS.l], AS.temp1[AS.l], AS.temp2[AS.l],AS.temp4[AS.l],Sync2, iStart, iEnd, jStart, jEnd);//, temp3[l]);

		for (int z=0; z<AS.nz; z++){
			b1kt=AS.b1k[AS.l][z];
			tmp2t=AS.temp2[AS.l][z];
			w1kt=AS.w1k[AS.l][z];
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++){ 
					tmp2t[i][j]=w1kt[i][j]-b1kt[i][j] -c0t;
					//AS.temp2[AS.l][z][i][j]=AS.w1k[AS.l][z][i][j]-AS.b1k[AS.l][z][i][j] -AS.c0;
				}	
			}
		} 
		Sync3.countDown();
		Sync3.await();
		//IJ.log("ASni " + AS.ni + " ASnj " + AS.nj);
		Tools.convolve2Dseparable(
				AS.temp4[AS.l][0], AS.temp2[AS.l][0],
				AS.ni, AS.nj,
				AS.p.kernelx, AS.p.kernely,
				AS.p.px, AS.p.py,
				AS.temp1[AS.l][0],
				iStart, iEnd
				);

		for (int z=0; z<AS.nz; z++){
			tmp1t=AS.temp1[AS.l][z];
			tmp3t=AS.temp3[AS.l][z];
			tmp4t=AS.temp4[AS.l][z];
			w3kt=AS.w3k[AS.l][z];
			b3kt=AS.b3k[AS.l][z];
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {
					tmp1t[i][j]=-tmp3t[i][j]+w3kt[i][j]-b3kt[i][j] + iDiff*tmp4t[i][j];
				}	
			}
		}

		Sync4.countDown();
		Dct.await();



		//temp2=muk

		Tools.convolve2Dseparable(AS.temp2[AS.l][0], AS.temp1[AS.l][0], AS.ni, AS.nj, AS.p.kernelx, AS.p.kernely, AS.p.px, AS.p.py, AS.temp3[AS.l][0], iStart, iEnd);
		tmp2t=AS.temp2[AS.l][0];
		for (int i=iStart; i<iEnd; i++) {  
			for (int j=0; j<AS.nj; j++) {
				tmp2t[i][j]=iDiff*tmp2t[i][j] + c0t;
			}	
		}


		//%-- w1k subproblem

		if(AS.p.noise_model==0){
			//poisson
			//temp3=detw2
			//  detw2 = (lambda*gamma.*weightData-b2k-muk).^2+4*lambda*gamma*weightData.*image;

			double ratioPrior=(AS.p.ldata/AS.p.lreg)*AS.p.gamma;
			double ratioPrior4=4*(AS.p.ldata/AS.p.lreg)*AS.p.gamma;
			double temp;
			for (int z=0; z<AS.nz; z++){
				b1kt=AS.b1k[AS.l][z];
				tmp2t=AS.temp2[AS.l][z];
				tmp3t=AS.temp3[AS.l][z];
				imgt=AS.image[z];
				for (int i=iStart; i<iEnd; i++) {  
					for (int j=0; j<AS.nj; j++) {
						temp=ratioPrior -b1kt[i][j] - tmp2t[i][j];
						tmp3t[i][j]=
								temp*temp
								+ratioPrior4*imgt[i][j];
					}	
				}
			}

			//w2k = 0.5*(b2k+muk-lambda*gamma.*weightData+sqrt(detw2));
			double ratioPriorGamma=(AS.p.ldata/AS.p.lreg)*AS.p.gamma;
			
			for (int z=0; z<AS.nz; z++){
				b1kt=AS.b1k[AS.l][z];
				tmp2t=AS.temp2[AS.l][z];
				tmp3t=AS.temp3[AS.l][z];
				w1kt=AS.w1k[AS.l][z];
				for (int i=iStart; i<iEnd; i++) {  
					for (int j=0; j<AS.nj; j++){ 
						w1kt[i][j]=0.5*(b1kt[i][j] + tmp2t[i][j]- ratioPriorGamma
								+ Math.sqrt(tmp3t[i][j]));
					}	
				}
			}
		}
		else{
			//gaussian
			//w2k = (b2k+muk+2*lambda*gamma*weightData.*image)./(1+2*lambda*gamma*weightData);
			for (int z=0; z<AS.nz; z++){
				for (int i=iStart; i<iEnd; i++) {  
					for (int j=0; j<AS.nj; j++){  
						AS.w1k[AS.l][0][i][j]=
								(AS.b1k[AS.l][z][i][j] + AS.temp2[AS.l][z][i][j]+2*(AS.p.ldata/AS.p.lreg)*AS.p.gamma*AS.image[0][i][j])
								/(1+2*(AS.p.ldata/AS.p.lreg)*AS.p.gamma);
					}	
				}
			}

		}

		//%-- w3k subproblem		
		for (int z=0; z<AS.nz; z++){
			tmp1t=AS.temp1[AS.l][z];
			w3kt=AS.w3k[AS.l][z];
			b3kt=AS.b3k[AS.l][z];
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {
					w3kt[i][j]=Math.max(Math.min(tmp1t[i][j]+ b3kt[i][j],1),0);
				}	
			}
		}

		for (int z=0; z<AS.nz; z++){
			w3kt=AS.w3k[AS.l][z];
			b3kt=AS.b3k[AS.l][z];
			w1kt=AS.w1k[AS.l][z];
			b1kt=AS.b1k[AS.l][z];
			tmp1t=AS.temp1[AS.l][z];
			tmp2t=AS.temp2[AS.l][z];
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {
					b1kt[i][j]=b1kt[i][j] +tmp2t[i][j]-w1kt[i][j];
					b3kt[i][j]=b3kt[i][j] +tmp1t[i][j]-w3kt[i][j];	
				}	
			}
		}

		Sync5.countDown();
		Sync5.await();
		//		
		LocalTools.fgradx2D(AS.temp3[AS.l], AS.temp1[AS.l], jStart, jEnd);
		LocalTools.fgrady2D(AS.temp4[AS.l], AS.temp1[AS.l], iStart,iEnd);
		//
		Sync6.countDown();
		Sync6.await();
		//		
		LocalTools.addtab(AS.w2xk[AS.l], AS.temp3[AS.l], AS.b2xk[AS.l],iStart, iEnd);
		LocalTools.addtab(AS.w2yk[AS.l], AS.temp4[AS.l], AS.b2yk[AS.l],iStart, iEnd);
		//		//temp1 = w1xk temp2 = w2yk
		LocalTools.shrink2D(AS.w2xk[AS.l], AS.w2yk[AS.l], AS.w2xk[AS.l], AS.w2yk[AS.l], AS.p.gamma, iStart, iEnd);
		//
		for (int z=0; z<AS.nz; z++){
			b2xkt=AS.b2xk[AS.l][z];
			b2ykt=AS.b2yk[AS.l][z];
			w2xkt=AS.w2xk[AS.l][z];
			w2ykt=AS.w2yk[AS.l][z];

			
			tmp1t=AS.temp1[AS.l][z];
			tmp2t=AS.temp2[AS.l][z];
			tmp3t=AS.temp3[AS.l][z];
			tmp4t=AS.temp4[AS.l][z];
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {  
					b2xkt[i][j]=b2xkt[i][j] + tmp3t[i][j]-w2xkt[i][j];
					b2ykt[i][j]=b2ykt[i][j] + tmp4t[i][j]-w2ykt[i][j];
					
					//mask[l][z][i][j]=w3k[l][z][i][j];
				}
			}
		}

		Sync7.countDown();
		Sync7.await();

		if(AS.stepk % AS.p.energyEvaluationModulo ==0  || AS.stepk==AS.p.max_nsb -1){
			AS.energytab2[num]=LocalTools.computeEnergyPSF(AS.temp1[AS.l], AS.w3k[AS.l], AS.temp3[AS.l], AS.temp4[AS.l],
					AS.p.ldata, AS.p.lreg,AS.p,AS.c0,AS.c1,AS.image,
					iStart, iEnd, jStart, jEnd, Sync8,Sync9);
		}


	}
}
