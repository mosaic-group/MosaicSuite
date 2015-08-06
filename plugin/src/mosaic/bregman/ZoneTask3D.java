package mosaic.bregman;



import java.util.concurrent.CountDownLatch;



class ZoneTask3D implements Runnable {

	private final CountDownLatch ZoneDoneSignal ;
	private final CountDownLatch Sync1 ;
	private final CountDownLatch Sync2 ;
	private final CountDownLatch Sync3 ;
	private final CountDownLatch Sync4;
	private final CountDownLatch Sync5 ;
	private final CountDownLatch Sync6 ;
	private final CountDownLatch Sync7;
	private final CountDownLatch Sync8 ;
	private final CountDownLatch Sync9;
	private final CountDownLatch Sync10;
	private final CountDownLatch Sync11;
	private final CountDownLatch Sync12;
	private final CountDownLatch Sync13;
	private final CountDownLatch Dct;
	private int iStart, iEnd, jStart, jEnd, nt;
	public Tools LocalTools;


	private ASplitBregmanSolverTwoRegions3DPSF AS;



	ZoneTask3D(CountDownLatch ZoneDoneSignal,CountDownLatch Sync1,CountDownLatch Sync2, 
			CountDownLatch Sync3,CountDownLatch Sync4,CountDownLatch Sync5,
			CountDownLatch Sync6,CountDownLatch Sync7,CountDownLatch Sync8,
			CountDownLatch Sync9,CountDownLatch Sync10,CountDownLatch Sync11, CountDownLatch Sync12, CountDownLatch Sync13, CountDownLatch Dct,
			int iStart, int iEnd, int jStart, int jEnd, int nt,
			ASplitBregmanSolverTwoRegions3DPSF AS, Tools tTools) {
		this.LocalTools=tTools;
		this.ZoneDoneSignal = ZoneDoneSignal;
		this.Sync1 = Sync1;this.Sync2 = Sync2;this.Sync3 = Sync3;
		this.Sync4 = Sync4;this.Sync5 = Sync5;this.Sync6 = Sync6;
		this.Sync7 = Sync7;this.Sync8 = Sync8;this.Sync9 = Sync9;
		this.Sync10 = Sync10;
		this.Sync11 = Sync11;
		this.Sync12 = Sync12;
		this.Sync13 = Sync13;
		this.Dct = Dct;
		this.AS=AS;
		this.nt = nt;
		this.iStart = iStart;this.jStart = jStart;
		this.iEnd = iEnd;this.jEnd= jEnd;

	}


	public void run() {
		try {
			doWork();
		}catch (InterruptedException ex) {}

		if (ZoneDoneSignal != null)
		{
			ZoneDoneSignal.countDown();
		}
	}

	void doWork() throws InterruptedException{


		LocalTools.subtab(AS.temp1[AS.l], AS.temp1[AS.l], AS.b2xk[AS.l], iStart,iEnd);  
		LocalTools.subtab(AS.temp2[AS.l], AS.temp2[AS.l], AS.b2yk[AS.l], iStart,iEnd);
		LocalTools.subtab(AS.temp4[AS.l], AS.w2zk[AS.l], AS.b2zk[AS.l], iStart,iEnd);

		if (Sync1 != null)
		{
			Sync1.countDown();
			Sync1.await();
		}

		//use w2zk as temp
		LocalTools.mydivergence3D(AS.temp3[AS.l], AS.temp1[AS.l], AS.temp2[AS.l], AS.temp4[AS.l], 
				AS.w2zk[AS.l],Sync2,
				iStart, iEnd, jStart, jEnd);//, temp3[l]);

		if (Sync12 != null)
		{
			Sync12.countDown();
			Sync12.await();
		}

		for (int z=0; z<AS.nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++){  
					AS.temp2[AS.l][z][i][j]=AS.w1k[AS.l][z][i][j]-AS.b1k[AS.l][z][i][j] -AS.c0;
				}	
			}
		} 

		if (Sync3 != null)
		{
			Sync3.countDown();
			Sync3.await();
		}
		
/*		if (nt == 0)
		{
			double tot = 0;
			for (int i = 0 ; i < AS.temp2[AS.l].length ; i++)
			{
				for (int j = 0 ; j < AS.temp2[AS.l][i].length ; j++)
				{
					for (int k = 0 ; k < AS.temp2[AS.l][i][j].length ; k++)
					{
						tot += AS.temp2[AS.l][i][j][k];
					}
				}
			}
			
			System.out.println("update 1: " + tot);
		}*/
		
		Tools.convolve3Dseparable(AS.temp4[AS.l], AS.temp2[AS.l], 
				AS.ni, AS.nj, AS.nz, 
				AS.p.PSF, AS.temp1[AS.l], iStart, iEnd);

		if (Sync11 != null)
		{
			Sync11.countDown();
			Sync11.await();
		}
		
		for (int z=0; z<AS.nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {  
					AS.temp1[AS.l][z][i][j]=-AS.temp3[AS.l][z][i][j]+AS.w3k[AS.l][z][i][j]-AS.b3k[AS.l][z][i][j] + (AS.c1-AS.c0)*AS.temp4[AS.l][z][i][j];
				}	
			}
		}

		if (Sync4 != null)
		{
			Sync4.countDown();
		}
			
		//		IJ.log("thread + istart iend jstart jend"+
		//		iStart +" " + iEnd+" " + jStart+" " + jEnd);

		if (Dct != null)
			Dct.await();
		else
		{
			AS.dct3d.forward(AS.temp1[AS.l], true);
			for (int z=0; z<AS.nz; z++){
				for (int i=0; i<AS.ni; i++) {  
					for (int j=0; j<AS.nj; j++) {  
						if ((1+ AS.eigenLaplacian[i][j] + AS.eigenPSF[0][i][j]) !=0) 
							AS.temp1[AS.l][z][i][j]=AS.temp1[AS.l][z][i][j]/(1+ AS.eigenLaplacian3D[z][i][j] + AS.eigenPSF[z][i][j]);
					}	
				}
			}
			AS.dct3d.inverse(AS.temp1[AS.l], true);
		}
		
		Tools.convolve3Dseparable(AS.temp2[AS.l], AS.temp1[AS.l], 
				AS.ni, AS.nj, AS.nz, 
				AS.p.PSF, AS.temp3[AS.l], iStart, iEnd);

		//synchro

		if (Sync10 != null)
		{
			Sync10.countDown();
			Sync10.await();
		}
			
		for (int z=0; z<AS.nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++) {  
					AS.temp2[AS.l][z][i][j]=(AS.c1-AS.c0)*AS.temp2[AS.l][z][i][j] + AS.c0;
				}	
			}
		}


		//%-- w1k subproblem
		if (AS.p.noise_model==0){
			//poisson
			for (int z=0; z<AS.nz; z++){
				for (int i=iStart; i<iEnd; i++) {  
					for (int j=0; j<AS.nj; j++) {  
						AS.temp3[AS.l][z][i][j]=
								Math.pow(((AS.p.ldata/AS.p.lreg_[AS.channel])*AS.p.gamma -AS.b1k[AS.l][z][i][j] - AS.temp2[AS.l][z][i][j]),2)
								+4*(AS.p.ldata/AS.p.lreg_[AS.channel])*AS.p.gamma*AS.image[z][i][j];
					}	
				}
			}
			for (int z=0; z<AS.nz; z++){
				for (int i=iStart; i<iEnd; i++) {  
					for (int j=0; j<AS.nj; j++) {  
						AS.w1k[AS.l][z][i][j]=0.5*(AS.b1k[AS.l][z][i][j] + AS.temp2[AS.l][z][i][j]- (AS.p.ldata/AS.p.lreg_[AS.channel])*AS.p.gamma
								+ Math.sqrt(AS.temp3[AS.l][z][i][j]));
					}	
				}
			}
		}
		else{
			//gaussian
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
					//mask[l][z][i][j]=w3k[l][z][i][j];
				}	
			}
		}

		if (Sync5 != null)
		{
			Sync5.countDown();
			Sync5.await();
		}
		
		LocalTools.fgradx2D(AS.temp3[AS.l], AS.temp1[AS.l], jStart,jEnd);
		LocalTools.fgrady2D(AS.temp4[AS.l], AS.temp1[AS.l], iStart, iEnd);
		LocalTools.fgradz2D(AS.ukz[AS.l], AS.temp1[AS.l], iStart, iEnd);

		if (Sync6 != null)
		{
			Sync6.countDown();
			Sync6.await();
		}
		LocalTools.addtab(AS.temp1[AS.l], AS.temp3[AS.l], AS.b2xk[AS.l], iStart, iEnd);
		LocalTools.addtab(AS.temp2[AS.l], AS.temp4[AS.l], AS.b2yk[AS.l],iStart, iEnd);
		LocalTools.addtab(AS.w2zk[AS.l], AS.ukz[AS.l], AS.b2zk[AS.l],iStart, iEnd);

		LocalTools.shrink3D(AS.temp1[AS.l], AS.temp2[AS.l],AS. w2zk[AS.l],
				AS.temp1[AS.l], AS.temp2[AS.l],AS.w2zk[AS.l], AS.p.gamma,
				iStart, iEnd);

		for (int z=0; z<AS.nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<AS.nj; j++){
					AS.b2xk[AS.l][z][i][j]=AS.b2xk[AS.l][z][i][j] + AS.temp3[AS.l][z][i][j]-AS.temp1[AS.l][z][i][j];
					AS.b2yk[AS.l][z][i][j]=AS.b2yk[AS.l][z][i][j] + AS.temp4[AS.l][z][i][j]-AS.temp2[AS.l][z][i][j];
					AS.b2zk[AS.l][z][i][j]=AS.b2zk[AS.l][z][i][j] + AS.ukz[AS.l][z][i][j]-AS.w2zk[AS.l][z][i][j];
					//mask[l][z][i][j]=w3k[l][z][i][j];
				}	
			}
		}
		
		if (Sync7 != null)
		{
			Sync7.countDown();
			Sync7.await();
		}
		
		// faire le menage dans les tableaux ici w2xk utilise comme temp
		if (AS.stepk % AS.p.energyEvaluationModulo ==0  || AS.stepk==AS.p.max_nsb -1){	
			AS.energytab2[nt]=
					LocalTools.computeEnergyPSF3D(AS.w2xk[AS.l], AS.w3k[AS.l], AS.temp3[AS.l], AS.temp4[AS.l],
							AS.p.ldata, AS.p.lreg_[AS.channel],AS.p,AS.c0,AS.c1,AS.image,
							iStart, iEnd, jStart, jEnd, Sync8,  Sync9, Sync13);
		}
	}
}
