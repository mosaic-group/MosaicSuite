package mosaic.bregman;


import java.util.Date;
import java.util.concurrent.CountDownLatch;



class ASplitBregmanSolverTwoRegions3DPSF extends ASplitBregmanSolverTwoRegions3D {
	public double [] [] [] eigenPSF;
	double c0,c1;
	public double [] energytab2;

	
	
	public ASplitBregmanSolverTwoRegions3DPSF(Parameters params,
			double [] [] [] image, double [] [] [] [] speedData, double [] [] [] [] mask,
			MasksDisplay md, int channel, AnalysePatch ap){
		super(params,image,speedData,mask,md, channel , ap);
		
		// Beta MLE in and out
		
		this.c0=params.cl[0];
		this.c1=params.cl[1];
		
		this.energytab2 = new double [p.nthreads];
		//c0=p.betaMLEoutdefault;//0.0027356;
		//c1=p.betaMLEindefault;//0.2340026;//sometimes not here ???
		
		int[] sz = p.PSF.getSuggestedImageSize();
		eigenPSF = new double [Math.max(sz[2],nz)][Math.max(sz[0],ni)][Math.max(sz[1],nj)];
		//IJ.log("nl " + nl);

		// Reallocate temps
		// Unfortunatelly is allocated in ASplitBregmanSolver
		
		this.temp4= new double [nl] [Math.max(sz[2],nz)] [Math.max(sz[0],ni)] [Math.max(sz[1],nj)];
		this.temp3= new double [nl] [Math.max(sz[2],nz)] [Math.max(sz[0],ni)] [Math.max(sz[1],nj)];
		this.temp2= new double [nl] [Math.max(sz[2],nz)] [Math.max(sz[0],ni)] [Math.max(sz[1],nj)];
		this.temp1= new double [nl] [Math.max(sz[2],nz)] [Math.max(sz[0],ni)] [Math.max(sz[1],nj)];
		
		this.compute_eigenPSF3D();	


		//Tools.disp_vals(eigenPSF[2], "eigenPSF");
		//Tools.disp_valsc(eigenPSF[0], "eigenPSF");
		//eigenPSF OK

		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++){  
				for (int j=0;j< nj; j++){  
					this.eigenLaplacian3D[z][i][j]=this.eigenLaplacian3D[z][i][j] - 2;			
				}	
			}
		}
		//Tools.disp_vals(eigenLaplacian3D[2], "eigenlaplacian");

		Tools.convolve3Dseparable(temp3[l], mask[l],  ni, nj, nz, p.PSF, temp4[l]);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					w1k[l][z][i][j]=(c1-c0)*temp3[l][z][i][j] + c0;
				}	
			}
		} 
		//Tools.disp_vals(w1k[l][2], "w1k");


		for (int i =0; i< nl;i++){
			//temp1=w2xk temp2=w2yk
			LocalTools.fgradx2D(temp1[i], mask[i]);
			LocalTools.fgrady2D(temp2[i], mask[i]);			
		}
	}

	@Override
	protected void init() {
		this.compute_eigenPSF();
		
		
		//IJ.log("init");
		//IJ.log("init c0 " + c0 + "c1 " + c1);
		Tools.convolve3Dseparable(temp3[l], w3k[l],  ni, nj, nz, p.PSF, temp4[l]);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					w1k[l][z][i][j]=(c1-c0)*temp3[l][z][i][j] + c0;
				}	
			}
		}  


		for (int i =0; i< nl;i++){
			//temp1=w2xk temp2=w2yk
			LocalTools.fgradx2D(temp1[i], w3k[i]);
			LocalTools.fgrady2D(temp2[i], w3k[i]);			
		}

	}
	
	/**
	 * 
	 * Multithread split bregman
	 * 
	 * @throws InterruptedException
	 */
	
	private void step_multit() throws InterruptedException
	{
		long lStartTime = new Date().getTime(); //start time
		//energy=0;

		CountDownLatch ZoneDoneSignal = new CountDownLatch(p.nthreads);//subprob 1 and 3
		CountDownLatch Sync1 = new CountDownLatch(p.nthreads);
		CountDownLatch Sync2 = new CountDownLatch(p.nthreads);
		CountDownLatch Sync3 = new CountDownLatch(p.nthreads);
		CountDownLatch Sync4= new CountDownLatch(p.nthreads);
		CountDownLatch Sync5 = new CountDownLatch(p.nthreads);
		CountDownLatch Sync6 = new CountDownLatch(p.nthreads);
		CountDownLatch Sync7= new CountDownLatch(p.nthreads);
		CountDownLatch Sync8 = new CountDownLatch(p.nthreads);
		CountDownLatch Sync9= new CountDownLatch(p.nthreads);
		CountDownLatch Sync10= new CountDownLatch(p.nthreads);
		CountDownLatch Sync11= new CountDownLatch(p.nthreads);
		CountDownLatch Sync12= new CountDownLatch(p.nthreads);
		CountDownLatch Sync13= new CountDownLatch(p.nthreads);
		CountDownLatch Dct= new CountDownLatch(1);
		CountDownLatch SyncFgradx= new CountDownLatch(1);


		int ichunk= p.ni/p.nthreads;
		int ilastchunk= p.ni - (p.ni/(p.nthreads))*(p.nthreads -1); 
		int jchunk= p.nj/p.nthreads;
		int jlastchunk= p.nj - (p.nj/(p.nthreads))*(p.nthreads -1);
		int iStart=0; 
		int jStart=0;
		Thread t[] = new Thread[p.nthreads];
		
		// Force the allocation of the buffers internally
		// if you do not do you can have race conditions in the
		// multi thread part
		// DO NOT REMOVE THEM EVEN IF THEY LOOK UNUSEFULL
		
		@SuppressWarnings("unused")
		double kernelx[] = p.PSF.getSeparableImageAsDoubleArray(0);
		@SuppressWarnings("unused")
		double kernely[] = p.PSF.getSeparableImageAsDoubleArray(1);
		@SuppressWarnings("unused")
		double kernelz[] = p.PSF.getSeparableImageAsDoubleArray(2);
		
		for (int nt=0; nt< p.nthreads-1;nt++){
			//			IJ.log("thread + istart iend jstart jend"+
			//					iStart +" " + (iStart+ichunk)+" " + jStart+" " + (jStart+jchunk));			
			// Check if we can create threads
			
			t[nt] = new Thread(new ZoneTask3D(ZoneDoneSignal,Sync1,Sync2,Sync3,Sync4,Sync5,
					Sync6,Sync7,Sync8,Sync9,Sync10,Sync11,Sync12,Sync13,Dct,
					iStart, iStart+ichunk, jStart, jStart+jchunk,nt,this,LocalTools));
			t[nt].start();

			iStart+=ichunk;
			jStart+=jchunk;
		}
		
		//		IJ.log("last thread + istart iend jstart jend"+
		//				iStart +" " + (iStart+ilastchunk)+" " + jStart+" " + (jStart+jlastchunk));
		
		// At least on linux you can go out of memory for threads
		
		
		Thread T_ext = new Thread(new ZoneTask3D(ZoneDoneSignal,Sync1,Sync2,Sync3,Sync4,Sync5,
					Sync6,Sync7,Sync8,Sync9,Sync10,Sync11,Sync12,Sync13,Dct,
					iStart, iStart+ilastchunk, jStart, jStart+jlastchunk,p.nthreads-1,this,LocalTools));

		T_ext.start();


		Sync4.await();

		dct3d.forward(temp1[l], true);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					if ((1+ eigenLaplacian[i][j] + eigenPSF[0][i][j]) !=0) 
						temp1[l][z][i][j]=temp1[l][z][i][j]/(1+ eigenLaplacian3D[z][i][j] + eigenPSF[z][i][j]);
				}	
			}
		}
		dct3d.inverse(temp1[l], true);

		Dct.countDown();

		//do fgradx without parallelization
		LocalTools.fgradx2D(temp4[l], temp1[l]);
		SyncFgradx.countDown();

		ZoneDoneSignal.await(); 


		if (stepk % p.energyEvaluationModulo ==0){	
			energy=0;
			for (int nt=0; nt< p.nthreads;nt++){
				energy+=energytab2[nt];
			}
		}

		//int centerim=p.nz/2;
		if (p.livedisplay && p.firstphase)
			md.display2regions3D(w3k[l], "Mask", channel);

		long lEndTime = new Date().getTime(); //end time

		long difference = lEndTime - lStartTime; //check different
		totaltime +=difference;
		//IJ.log("Elapsed milliseconds: " + difference);		
	}
	
	/**
	 * 
	 * Single thread split Bregman
	 * 
	 */
	
	private void step_single()
	{
		long lStartTime = new Date().getTime(); //start time
		//energy=0;


		int ilastchunk= p.ni; 
		int jlastchunk= p.nj;
		int iStart=0; 
		int jStart=0;

		
		//		IJ.log("last thread + istart iend jstart jend"+
		//				iStart +" " + (iStart+ilastchunk)+" " + jStart+" " + (jStart+jlastchunk));
		
		// At least on linux you can go out of memory for threads
		
		
		ZoneTask3D zt = new ZoneTask3D(null,null,null,null,null,null,
					null,null,null,null,null,null,null,null,null,
					iStart, iStart+ilastchunk, jStart, jStart+jlastchunk,p.nthreads-1,this,LocalTools);

		zt.run();
		
		//energytab[l]=Tools.computeEnergyPSF3D(speedData[l], w3k[l], temp3[l], temp4[l], p.ldata, p.lreg,p,c0,c1,image);
		//energy+=energytab[l];

		if (stepk % p.energyEvaluationModulo ==0){	
			energy=0;
			for (int nt=0; nt< p.nthreads;nt++){
				energy+=energytab2[nt];
			}
		}

		//int centerim=p.nz/2;
		if (p.livedisplay && p.firstphase) md.display2regions3D(w3k[l], "Mask", channel);


		long lEndTime = new Date().getTime(); //end time

		long difference = lEndTime - lStartTime; //check different
		totaltime +=difference;
		//IJ.log("Elapsed milliseconds: " + difference);
	}
	
	@Override
	protected void step() throws InterruptedException 
	{
		if (p.nthreads == 1)
			step_single();
		else
			step_multit();

	}



	private void compute_eigenPSF3D(){
		this.c0=p.cl[0];
		this.c1=p.cl[1];
		
		//  PSF2   = imfilter(PSF,PSF,'symmetric');		
		
		int[] sz = p.PSF.getSuggestedImageSize();
		
		Tools.convolve3Dseparable(
				eigenPSF,p.PSF.getImage3DAsDoubleArray(),
				sz[0], sz[1], sz[2],
				p.PSF, temp4[l]);



		//Tools.disp_vals(eigenPSF[2], "eigenPSF2");

		//paddedPSF   = padPSF(PSF2,dims);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					temp2[l][z][i][j]=0;
				}	
			}
		}

		sz = p.PSF.getSuggestedImageSize();
		for (int z=0; z<sz[2]; z++){
			for (int i=0; i<sz[0]; i++) {  
				for (int j=0; j<sz[1]; j++) {  
					temp2[l][z][i][j]=eigenPSF[z][i][j];
				}	
			}
		}

		//Tools.disp_vals(temp2[l][2], "padded");

		int cr = (sz[0]/2) +1;
		int cc = (sz[1]/2) +1;
		int cs = (sz[2]/2) +1;

		//temp1 = e1
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					temp1[l][z][i][j]=0;
				}	
			}
		}

		//IJ.log("cr " + cr  + "cc " + cc  );
		temp1[l][0][0][0]=1;


		LocalTools.dctshift3D(temp3[l], temp2[l], cr,cc, cs);

		//Tools.disp_vals(temp3[l][2], "shifted");

		dct3d.forward(temp3[l], true);

		dct3d.forward(temp1[l], true);

		//	Tools.disp_vals(temp3[l][2], "dct3 t3");
		//Tools.disp_vals(temp1[l][2], "dct3 t1");

		//IJ.log("c0 " + c0 + "c1 " + c1);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					eigenPSF[z][i][j]=Math.pow(c1-c0,2)*temp3[l][z][i][j]/temp1[l][z][i][j];
				}	//
			}
		}
		//	Tools.disp_vals(eigenPSF[2], "eigenPSF func");

		//	    PSF2        = imfilter(PSF,PSF,'symmetric');
		//	    paddedPSF   = padPSF(PSF2,dims);
		//	    center      = ceil(size(PSF2)/2);
		//	    e1          = zeros(dims);
		//	    e1(1,1,1)   = 1;
		//	    S           = dctn(dctshift(paddedPSF,center))./dctn(e1);
		//	    eigenPSF    = (betaMLE_in-betaMLE_out)^2*S;


	}

}
