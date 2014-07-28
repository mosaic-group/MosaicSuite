

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
	private final CountDownLatch Sync10;
	private final CountDownLatch Sync11;
	private final CountDownLatch Dct;
	private ASplitBregmanSolverTwoRegionsPSF AS;
	private int iStart, iEnd, jStart, jEnd ;
	private int num;	
	public Tools LocalTools;

	ZoneTask(CountDownLatch ZoneDoneSignal,CountDownLatch Sync1,CountDownLatch Sync2,
			CountDownLatch Sync3,CountDownLatch Sync4,CountDownLatch Dct,CountDownLatch Sync5,CountDownLatch Sync6,
			CountDownLatch Sync7,CountDownLatch Sync8,CountDownLatch Sync9,CountDownLatch Sync10,CountDownLatch Sync11,
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
		this.Sync10 = Sync10;
		this.Sync11 = Sync11;
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
		LocalTools.subtab(AS.temp1[AS.l], AS.w2xk[AS.l], AS.b2xk[AS.l], iStart, iEnd);
		LocalTools.subtab(AS.temp2[AS.l], AS.w2yk[AS.l], AS.b2yk[AS.l], iStart, iEnd);

		//synchro
		Sync1.countDown();
		Sync1.await();
		

		//	IJ.log("thread + istart iend jstart jend"+
		//	iStart +" " + iEnd+" " + jStart+" " + jEnd);
		LocalTools.mydivergence(AS.temp3[AS.l], AS.temp1[AS.l], AS.temp2[AS.l],AS.temp4[AS.l],Sync2, iStart, iEnd, jStart, jEnd);//, temp3[l]);
		
		for (int z=0; z<AS.nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++){ 
					AS.temp2[AS.l][z][i][j]=AS.w1k[AS.l][z][i][j]-AS.b1k[AS.l][z][i][j] -AS.c0;
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
				AS.p.PSF,
				AS.temp1[AS.l][0],
				iStart, iEnd
				);
		
		Sync11.countDown();
		Sync11.await();
		
		for (int z=0; z<AS.nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {  
					AS.temp1[AS.l][z][i][j]=-AS.temp3[AS.l][z][i][j]+AS.w3k[AS.l][z][i][j]-AS.b3k[AS.l][z][i][j] + (AS.c1-AS.c0)*AS.temp4[AS.l][z][i][j];
				}	
			}
		}

		Sync4.countDown();
		Dct.await();

		//temp2=muk

		Tools.convolve2Dseparable(AS.temp2[AS.l][0], AS.temp1[AS.l][0], AS.ni, AS.nj, AS.p.PSF, AS.temp3[AS.l][0], iStart, iEnd);
		
		//synchro

		Sync10.countDown();
		Sync10.await();
		
		for (int i=iStart; i<iEnd; i++) {  
			for (int j=0; j<AS.nj; j++) { 
				AS.temp2[AS.l][0][i][j]=(AS.c1-AS.c0)*AS.temp2[AS.l][0][i][j] + AS.c0;
			}	
		}


		//%-- w1k subproblem

		if(AS.p.noise_model==0){
			//poisson
			//temp3=detw2
			//  detw2 = (lambda*gamma.*weightData-b2k-muk).^2+4*lambda*gamma*weightData.*image;


			for (int z=0; z<AS.nz; z++){
				for (int i=iStart; i<iEnd; i++) {  
					for (int j=0; j<AS.nj; j++) {  
						AS.temp3[AS.l][0][i][j]=
								Math.pow(((AS.p.ldata/AS.p.lreg_[AS.channel])*AS.p.gamma -AS.b1k[AS.l][0][i][j] - AS.temp2[AS.l][0][i][j]),2)
								+4*(AS.p.ldata/AS.p.lreg_[AS.channel])*AS.p.gamma*AS.image[0][i][j];
					}	
				}
			}

			//w2k = 0.5*(b2k+muk-lambda*gamma.*weightData+sqrt(detw2));

			for (int z=0; z<AS.nz; z++){
				for (int i=iStart; i<iEnd; i++) {  
					for (int j=0; j<AS.nj; j++){  
						AS.w1k[AS.l][0][i][j]=0.5*(AS.b1k[AS.l][z][i][j] + AS.temp2[AS.l][z][i][j]- (AS.p.ldata/AS.p.lreg_[AS.channel])*AS.p.gamma + Math.sqrt(AS.temp3[AS.l][z][i][j]));
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
								(AS.b1k[AS.l][z][i][j] + AS.temp2[AS.l][z][i][j]+2*(AS.p.ldata/AS.p.lreg_[AS.channel])*AS.p.gamma*AS.image[0][i][j])
								/(1+2*(AS.p.ldata/AS.p.lreg_[AS.channel])*AS.p.gamma);
					}	
				}
			}

		}

		//%-- w3k subproblem		
		for (int z=0; z<AS.nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {  
					AS.w3k[AS.l][z][i][j]=Math.max(Math.min(AS.temp1[AS.l][z][i][j]+ AS.b3k[AS.l][z][i][j],1),0);
				}	
			}
		}

		for (int z=0; z<AS.nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {  
					AS.b1k[AS.l][z][i][j]=AS.b1k[AS.l][z][i][j] +AS.temp2[AS.l][z][i][j]-AS.w1k[AS.l][z][i][j];
					AS.b3k[AS.l][z][i][j]=AS.b3k[AS.l][z][i][j] +AS.temp1[AS.l][z][i][j]-AS.w3k[AS.l][z][i][j];
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
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {  
					AS.b2xk[AS.l][z][i][j]=AS.b2xk[AS.l][z][i][j] +AS.temp3[AS.l][z][i][j]-AS.w2xk[AS.l][z][i][j];
					AS.b2yk[AS.l][z][i][j]=AS.b2yk[AS.l][z][i][j] +AS.temp4[AS.l][z][i][j]-AS.w2yk[AS.l][z][i][j];
					//mask[l][z][i][j]=w3k[l][z][i][j];
				}
			}
		}

		Sync7.countDown();
		Sync7.await();

		if(AS.stepk % AS.p.energyEvaluationModulo ==0  || AS.stepk==AS.p.max_nsb -1){
			AS.energytab2[num]=LocalTools.computeEnergyPSF(AS.temp1[AS.l], AS.w3k[AS.l], AS.temp3[AS.l], AS.temp4[AS.l],
					AS.p.ldata, AS.p.lreg_[AS.channel],AS.p,AS.c0,AS.c1,AS.image,
					iStart, iEnd, jStart, jEnd, Sync8,Sync9);
		}


	}
}
