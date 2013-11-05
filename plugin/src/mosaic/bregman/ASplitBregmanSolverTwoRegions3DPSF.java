package mosaic.bregman;


import java.util.Date;
import java.util.concurrent.CountDownLatch;



public class ASplitBregmanSolverTwoRegions3DPSF extends ASplitBregmanSolverTwoRegions3D {
	public double [] [] [] eigenPSF;
	double c0,c1;
	public double [] energytab2;

	
	
	public ASplitBregmanSolverTwoRegions3DPSF(Parameters params,
			double [] [] [] image, double [] [] [] [] speedData, double [] [] [] [] mask,
			MasksDisplay md, int channel, AnalysePatch ap){
		super(params,image,speedData,mask,md, channel , ap);
		this.c0=params.cl[0];
		this.c1=params.cl[1];
		this.energytab2 = new double [p.nthreads];
		//c0=p.betaMLEoutdefault;//0.0027356;
		//c1=p.betaMLEindefault;//0.2340026;//sometimes not here ???
		
		
		eigenPSF = new double [Math.max(7,nz)][Math.max(7,ni)][Math.max(7,nj)];
		//IJ.log("nl " + nl);

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

		Tools.convolve3Dseparable(temp3[l], mask[l],  ni, nj, nz, p.kernelx,p.kernely, p.kernelz, p.px, p.py, p.pz, temp4[l]);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					w1k[l][z][i][j]=(c1-c0)*temp3[l][z][i][j] + c0;
				}	
			}
		} 
		//Tools.disp_vals(w1k[l][2], "w1k");


		for(int i =0; i< nl;i++){
			//temp1=w2xk temp2=w2yk
			LocalTools.fgradx2D(temp1[i], mask[i]);
			LocalTools.fgrady2D(temp2[i], mask[i]);			
		}


	}

	@Override
	public void init(){
		this.compute_eigenPSF();
		
		
		//IJ.log("init");
		//IJ.log("init c0 " + c0 + "c1 " + c1);
		Tools.convolve3Dseparable(temp3[l], w3k[l],  ni, nj, nz, p.kernelx,p.kernely, p.kernelz, p.px, p.py, p.pz, temp4[l]);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					w1k[l][z][i][j]=(c1-c0)*temp3[l][z][i][j] + c0;
				}	
			}
		}  


		for(int i =0; i< nl;i++){
			//temp1=w2xk temp2=w2yk
			LocalTools.fgradx2D(temp1[i], w3k[i]);
			LocalTools.fgrady2D(temp2[i], w3k[i]);			
		}

	}
	
	
	@Override
	protected void step() throws InterruptedException {
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
		CountDownLatch Dct= new CountDownLatch(1);
		CountDownLatch SyncFgradx= new CountDownLatch(1);


		int ichunk= p.ni/p.nthreads;
		int ilastchunk= p.ni - (p.ni/(p.nthreads))*(p.nthreads -1); 
		int jchunk= p.nj/p.nthreads;
		int jlastchunk= p.nj - (p.nj/(p.nthreads))*(p.nthreads -1);
		int iStart=0; 
		int jStart=0;
		for(int nt=0; nt< p.nthreads-1;nt++){
			//			IJ.log("thread + istart iend jstart jend"+
			//					iStart +" " + (iStart+ichunk)+" " + jStart+" " + (jStart+jchunk));
			new Thread(new ZoneTask3D(ZoneDoneSignal,Sync1,Sync2,Sync3,Sync4,
					Sync7,Sync8,Sync9,Sync10,Dct,SyncFgradx,
					iStart, iStart+ichunk, jStart, jStart+jchunk,nt,this,LocalTools)).start();
			iStart+=ichunk;
			jStart+=jchunk;
		}
		//		IJ.log("last thread + istart iend jstart jend"+
		//				iStart +" " + (iStart+ilastchunk)+" " + jStart+" " + (jStart+jlastchunk));
		new Thread(new ZoneTask3D(ZoneDoneSignal,Sync1,Sync2,Sync3,Sync4,
				Sync7,Sync8,Sync9,Sync10,Dct,SyncFgradx,
				iStart, iStart+ilastchunk, jStart, jStart+jlastchunk,p.nthreads-1,this,LocalTools)).start();
		//		IJ.log("");





		//Tools.disp_vals(temp1[l][2], "w2xk debut");
		//Tools.disp_vals(temp2[l][2], "w2yk debut");

		// IJ.log("thread : " +l +"starting work");

		//Tools.subtab(temp1[l], temp1[l], b2xk[l]);  
		//Tools.subtab(temp2[l], temp2[l], b2yk[l]);
		//Tools.subtab(temp4[l], w2zk[l], b2zk[l]);

		//temp3=divwb
		//Tools.mydivergence3D(temp3[l], temp1[l], temp2[l], temp4[l]);//, temp3[l]);

		//		Tools.bgradxdbc2D(temp3[l], temp1[l]);
		//		Tools.bgradydbc2D(temp1[l], temp2[l]);
		//		Tools.addtab(temp3[l], temp3[l], temp1[l]);
		//		Tools.bgradzdbc2D(temp1[l], temp3[l]);
		//		Tools.addtab(temp3[l], temp3[l], temp1[l]);
		//		

		//Tools.disp_vals(temp3[l][2], "divergence");

		//		for (int z=0; z<nz; z++){
		//			for (int i=0; i<ni; i++) {  
		//				for (int j=0; j<nj; j++){  
		//					temp2[l][z][i][j]=w1k[l][z][i][j]-b1k[l][z][i][j] -c0;
		//				}	
		//			}
		//		} 

		//Tools.disp_vals(temp2[l][2], "temp2 sub");
		//Tools.disp_vals(temp2[l][0], "t2");
		//		Tools.convolve3Dseparable(temp4[l], temp2[l], 
		//				ni, nj, nz, 
		//				p.kernelx,p.kernely, p.kernelz,
		//				p.px, p.py, p.pz, temp1[l]);//which temp ?

		//Tools.disp_vals(temp4[l][0], "t4");
		//Tools.disp_vals(temp4[l][2], "filtered sub");
		//Tools.disp_vals(b3k[l][2], "b3k");
		//Tools.disp_vals(w3k[l][2], "w3k");
		//Tools.disp_vals(temp3[l][2], "verif div");

		//temp1=RHS
		//Tools.disp_vals(temp3[l][5], "divergence");
		//		for (int z=0; z<nz; z++){
		//			for (int i=0; i<ni; i++) {  
		//				for (int j=0; j<nj; j++) {  
		//					temp1[l][z][i][j]=-temp3[l][z][i][j]+w3k[l][z][i][j]-b3k[l][z][i][j] + (c1-c0)*temp4[l][z][i][j];
		//				}	
		//			}
		//		}

		//
		//Tools.disp_vals(temp1[l][2], "RHS");

		//Tools.disp_vals(temp1[l][5], "RHS");

		//temp1=uk



		Sync4.await();

		dct3d.forward(temp1[l], true);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					if((1+ eigenLaplacian[i][j] + eigenPSF[0][i][j]) !=0) 
						temp1[l][z][i][j]=temp1[l][z][i][j]/(1+ eigenLaplacian3D[z][i][j] + eigenPSF[z][i][j]);
				}	
			}
		}
		dct3d.inverse(temp1[l], true);

		Dct.countDown();

		//do fgradx without parallelization
		LocalTools.fgradx2D(temp4[l], temp1[l]);
		SyncFgradx.countDown();

		//Tools.disp_vals(temp1[l][2], "uk");

		//temp2=muk
		//		Tools.convolve3Dseparable(temp2[l], temp1[l], 
		//				ni, nj, nz, 
		//				p.kernelx,p.kernely, p.kernelz,
		//				p.px, p.py, p.pz, temp3[l]);
		//		for (int z=0; z<nz; z++){
		//			for (int i=0; i<ni; i++) {  
		//				for (int j=0; j<nj; j++) {  
		//					temp2[l][z][i][j]=(c1-c0)*temp2[l][z][i][j] + c0;
		//				}	
		//			}
		//		}
		//Tools.disp_vals(temp2[l][2], "muk");



		//detw2 = (lambda*gamma.*weightData-b2k-muk).^2+4*lambda*gamma*weightData.*image;
		//w2k = 0.5*(b2k+muk-lambda*gamma.*weightData+sqrt(detw2));

		//%-- w1k subproblem
		//temp3=detw2
		//		for (int z=0; z<nz; z++){
		//			for (int i=0; i<ni; i++) {  
		//				for (int j=0; j<nj; j++) {  
		//					temp3[l][z][i][j]=
		//							Math.pow(((p.ldata/p.lreg)*p.gamma -b1k[l][z][i][j] - temp2[l][z][i][j]),2)
		//							+4*(p.ldata/p.lreg)*p.gamma*image[z][i][j];
		//				}	
		//			}
		//		}
		//		for (int z=0; z<nz; z++){
		//			for (int i=0; i<ni; i++) {  
		//				for (int j=0; j<nj; j++) {  
		//					w1k[l][z][i][j]=0.5*(b1k[l][z][i][j] + temp2[l][z][i][j]- (p.ldata/p.lreg)*p.gamma
		//							+ Math.sqrt(temp3[l][z][i][j]));
		//				}	
		//			}
		//		}
		//Tools.disp_vals(w1k[l][2], "w1k");

		//Tools.addtab(temp4[l], b3k[l], temp1[l]);



		//Tools.disp_vals(w1k[l][5], "w1k");

		//Tools.disp_vals(b3k[l][2], " verif b3k");
		//Tools.disp_vals(temp1[l][2], " verif uk");

		//%-- w3k subproblem		
		//		for (int z=0; z<nz; z++){
		//			for (int i=0; i<ni; i++) {  
		//				for (int j=0; j<nj; j++) {  
		//					w3k[l][z][i][j]=Math.max(Math.min(temp1[l][z][i][j]+ b3k[l][z][i][j],1),0);
		//				}	
		//			}
		//		}
		//		//Tools.disp_vals(w3k[l][2], "w3k l2");
		//
		//		//Tools.disp_vals(w3k[l][5], "w3k l5");
		//		for (int z=0; z<nz; z++){
		//			for (int i=0; i<ni; i++) {  
		//				for (int j=0; j<nj; j++) {  
		//					b1k[l][z][i][j]=b1k[l][z][i][j] +temp2[l][z][i][j]-w1k[l][z][i][j];
		//					b3k[l][z][i][j]=b3k[l][z][i][j] +temp1[l][z][i][j]-w3k[l][z][i][j];	
		//					//mask[l][z][i][j]=w3k[l][z][i][j];
		//				}	
		//			}
		//		}
		//Tools.disp_vals(b1k[l][2], "b1k");
		//Tools.disp_vals(b3k[l][2], "b3k");

		//%-- w2k sub-problem
		//temp4=ukx, temp3=uky


		ZoneDoneSignal.await(); 



		//		Tools.fgradx2D(temp3[l], temp1[l]);
		//		Tools.fgrady2D(temp4[l], temp1[l]);
		//		Tools.fgradz2D(ukz[l], temp1[l]);

		//		Tools.addtab(temp1[l], temp3[l], b2xk[l]);
		//		Tools.addtab(temp2[l], temp4[l], b2yk[l]);
		//		Tools.addtab(w2zk[l], ukz[l], b2zk[l]);
		//temp1=w2xk temp2=w2yk
		//		Tools.shrink3D(temp1[l], temp2[l], w2zk[l], temp1[l], temp2[l], w2zk[l], p.gamma);
		//do shrink3D
		//Tools.disp_vals(w2zk[l][2], "w2zk");
		//Tools.disp_vals(temp1[l][2], "w2xk");
		//		for (int z=0; z<nz; z++){
		//			for (int i=0; i<ni; i++) {  
		//				for (int j=0; j<nj; j++){
		//					b2xk[l][z][i][j]=b2xk[l][z][i][j] + temp3[l][z][i][j]-temp1[l][z][i][j];
		//					b2yk[l][z][i][j]=b2yk[l][z][i][j] + temp4[l][z][i][j]-temp2[l][z][i][j];
		//					b2zk[l][z][i][j]=b2zk[l][z][i][j] + ukz[l][z][i][j]-w2zk[l][z][i][j];
		//					//mask[l][z][i][j]=w3k[l][z][i][j];
		//				}	
		//			}
		//		}
		//Tools.disp_vals(b2xk[l][2], "b2xk");
		//Tools.disp_vals(b2yk[l][2], "b2yk");
		//Tools.disp_vals(b2zk[l][2], "b2zk");
		//Tools.disp_vals(temp1[l][2], "w2xk");
		//Tools.disp_vals(temp2[l][2], "w2yk");
		//Tools.disp_vals(w2zk[l][2], "w2zk");

		////		normtab[l]=0;
		////		for (int z=0; z<nz; z++){
		////			for (int i=0; i<ni; i++) {  
		////				for (int j=0; j<nj; j++) {  
		////					//					l2normtab[l]+=Math.sqrt(Math.pow(w3k[l][z][i][j]-w3kp[l][z][i][j],2));
		////					normtab[l]+=Math.abs(w3k[l][z][i][j]-w3kp[l][z][i][j]);
		////				}	
		////			}
		////		}
		//
		//		Tools.copytab(w3kp[l], w3k[l]);

		//energytab[l]=Tools.computeEnergyPSF3D(speedData[l], w3k[l], temp3[l], temp4[l], p.ldata, p.lreg,p,c0,c1,image);
		//energy+=energytab[l];

		if(stepk % p.energyEvaluationModulo ==0){	
			energy=0;
			for(int nt=0; nt< p.nthreads;nt++){
				energy+=energytab2[nt];
			}
		}

		//int centerim=p.nz/2;
		if(p.livedisplay && p.firstphase) md.display2regions3D(w3k[l], "Mask", channel);


		long lEndTime = new Date().getTime(); //end time

		long difference = lEndTime - lStartTime; //check different
		totaltime +=difference;
		//IJ.log("Elapsed milliseconds: " + difference);

	}



	public void compute_eigenPSF3D(){
		this.c0=p.cl[0];
		this.c1=p.cl[1];
		
		//  PSF2   = imfilter(PSF,PSF,'symmetric');		

//		for (int i=0;i< 7; i++){  
//			IJ.log("psfx" + p.kernelx[i]);			
//		}
//	for (int i=0;i< 7; i++){  
//		IJ.log("psfy" + p.kernely[i]);			
//	}
//	for (int i=0;i< 7; i++){  
//		IJ.log("psfz" + p.kernelz[i]);			
//	}
		
		
		
		Tools.convolve3Dseparable(
				eigenPSF, p.PSF,
				p.px, p.py, p.pz, 
				p.kernelx, p.kernely, p.kernelz,
				p.px, p.py, p.pz, temp4[l]);



		//Tools.disp_vals(eigenPSF[2], "eigenPSF2");

		//paddedPSF   = padPSF(PSF2,dims);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					temp2[l][z][i][j]=0;
				}	
			}
		}


		for (int z=0; z<p.pz; z++){
			for (int i=0; i<p.px; i++) {  
				for (int j=0; j<p.py; j++) {  
					temp2[l][z][i][j]=eigenPSF[z][i][j];
				}	
			}
		}

		//Tools.disp_vals(temp2[l][2], "padded");

		int cr = (p.px/2) +1;
		int cc = (p.py/2) +1;
		int cs = (p.pz/2) +1;

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
