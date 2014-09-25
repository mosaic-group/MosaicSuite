package mosaic.bregman;

import java.util.Date;
import edu.emory.mathcs.jtransforms.dct.DoubleDCT_3D;

public class ASplitBregmanSolverTwoRegions3D  extends ASplitBregmanSolverTwoRegions {

	public double [] [] [] [] w2zk;
	public double [] [] [] [] b2zk;
	public double [] [] [] [] ukz;
	public double [] [] [] eigenLaplacian3D;
	public DoubleDCT_3D dct3d;

	public ASplitBregmanSolverTwoRegions3D(Parameters params, 
			double [] [] [] image, double [] [] [] [] speedData, double [] [] [] [] mask,
			MasksDisplay md, int channel, AnalysePatch ap){
		super(params,image,speedData,mask,md, channel,ap);
		this.w2zk= new double [nl] [nz] [ni] [nj];
		this.ukz= new double [nl] [nz] [ni] [nj];
		this.b2zk= new double [nl] [nz] [ni] [nj];
		this.eigenLaplacian3D= new double [nz] [ni] [nj];
		dct3d= new DoubleDCT_3D(nz,ni,nj);
		
		for(int i =0; i< nl;i++){
			LocalTools.fgradz2D(w2zk[i], mask[i]);
		}
		
		for(int l =0; l< nl;l++){
			for (int z=0; z<nz; z++){
				for (int i=0; i<ni; i++) {  
					for (int j=0; j<nj; j++) {  
						b2zk[l][z][i][j]=0;
					}	
				}
			}
		}


		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++){  
				for (int j=0;j< nj; j++){  
					this.eigenLaplacian3D[z][i][j]=
							2
							+		(
									2-2*Math.cos((j)*Math.PI/(nj))
									+ (2-2*Math.cos((i)*Math.PI/(ni)))
									+ (2-2*Math.cos((z)*Math.PI/(nz)))
									)
									;			
				}	
			}
		}
		
		//Tools.disp_vals(eigenLaplacian3D[5], "eigen3D");

	}

	@Override
	protected void step() throws InterruptedException {
		long lStartTime = new Date().getTime(); //start time
		//energy=0;

		
		// IJ.log("thread : " +l +"starting work");
		LocalTools.subtab(temp1[l], temp1[l], b2xk[l]);  
		LocalTools.subtab(temp2[l], temp2[l], b2yk[l]);
		LocalTools.subtab(temp4[l], w2zk[l], b2zk[l]);

		//temp3=divwb
		LocalTools.mydivergence3D(temp3[l], temp1[l], temp2[l], temp4[l]);//, temp3[l]);

		//Tools.disp_vals(temp3[l][5], "divergence");
		
		//RHS = -divwb+w2k-b2k+w3k-b3k;
		//temp1=RHS
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					temp1[l][z][i][j]=-temp3[l][z][i][j]+w1k[l][z][i][j]-b1k[l][z][i][j]+w3k[l][z][i][j]-b3k[l][z][i][j];
				}	
			}
		}

		//Tools.disp_vals(temp1[l][5], "RHS");

		//temp1=uk

		dct3d.forward(temp1[l], true);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					if(eigenLaplacian[i][j] !=0) temp1[l][z][i][j]=temp1[l][z][i][j]/eigenLaplacian3D[z][i][j];
				}	
			}
		}
		dct3d.inverse(temp1[l], true);

		//Tools.disp_vals(speedData[l][5], "speed");
		//Tools.disp_vals(temp1[l][5], "uk");
		//Tools.addtab(temp4[l], b3k[l], temp1[l]);

		//%-- w1k subproblem
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					w1k[l][z][i][j]=-(p.ldata/p.lreg_[channel])*p.gamma*speedData[l][z][i][j] +b1k[l][z][i][j] + temp1[l][z][i][j];
				}
			}
		}

		//Tools.disp_vals(w1k[l][5], "w1k");


		//%-- w3k subproblem		
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					w3k[l][z][i][j]=Math.max(Math.min(temp1[l][z][i][j]+ b3k[l][z][i][j],1),0);
				}	
			}
		}


		//Tools.disp_vals(w3k[l][5], "w3k");
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					b1k[l][z][i][j]=b1k[l][z][i][j] +temp1[l][z][i][j]-w1k[l][z][i][j];
					b3k[l][z][i][j]=b3k[l][z][i][j] +temp1[l][z][i][j]-w3k[l][z][i][j];	
					//mask[l][z][i][j]=w3k[l][z][i][j];
				}	
			}
		}

		
		//%-- w2k sub-problem
		//temp4=ukx, temp3=uky
		LocalTools.fgradx2D(temp3[l], temp1[l]);
		LocalTools.fgrady2D(temp4[l], temp1[l]);
		LocalTools.fgradz2D(ukz[l], temp1[l]);

		LocalTools.addtab(temp1[l], temp3[l], b2xk[l]);
		LocalTools.addtab(temp2[l], temp4[l], b2yk[l]);
		LocalTools.addtab(w2zk[l], ukz[l], b2zk[l]);
		//temp1=w2xk temp2=w2yk
		LocalTools.shrink3D(temp1[l], temp2[l], w2zk[l], temp1[l], temp2[l], w2zk[l], p.gamma);
		//do shrink3D

		
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++){
					b2xk[l][z][i][j]=b2xk[l][z][i][j] + temp3[l][z][i][j]-temp1[l][z][i][j];
					b2yk[l][z][i][j]=b2yk[l][z][i][j] + temp4[l][z][i][j]-temp2[l][z][i][j];
					b2zk[l][z][i][j]=b2zk[l][z][i][j] + ukz[l][z][i][j]-w2zk[l][z][i][j];
					//mask[l][z][i][j]=w3k[l][z][i][j];
				}
			}
		}
		//Tools.disp_vals(b2xk[l][5], "b2xk");

				
//		normtab[l]=0;
//		for (int z=0; z<nz; z++){
//			for (int i=0; i<ni; i++) {  
//				for (int j=0; j<nj; j++) {  
//					//					l2normtab[l]+=Math.sqrt(Math.pow(w3k[l][z][i][j]-w3kp[l][z][i][j],2));
//					normtab[l]+=Math.abs(w3k[l][z][i][j]-w3kp[l][z][i][j]);
//				}	
//			}
//		}

//		Tools.copytab(w3kp[l], w3k[l]);

		energytab[l]=LocalTools.computeEnergy3D(speedData[l], w3k[l], temp3[l], temp4[l], ukz[l], p.ldata, p.lreg_[channel]);


		//doneSignal2.await();

		energy+=energytab[l];

		//if(p.livedisplay) md.display2regions3D(w3k[l], "Mask");


		long lEndTime = new Date().getTime(); //end time

		long difference = lEndTime - lStartTime; //check different
		totaltime +=difference;
		//IJ.log("Elapsed milliseconds: " + difference);

	}


}
