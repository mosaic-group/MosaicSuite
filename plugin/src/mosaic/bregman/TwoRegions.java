package mosaic.bregman;

import ij.IJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import mosaic.bregman.FindConnectedRegions.Region;

public class TwoRegions extends NRegions 
{
	double [] [] [] [] SpeedData;
	public TwoRegions(ImagePlus img,Parameters params, CountDownLatch DoneSignal, int channel)
	{
		super( img, params,  DoneSignal,channel);	


		if(p.nlevels >1  || !p.usePSF)
		{ 
			//save memory when Ei not needed
			SpeedData = new double [1] [nz] [ni] [nj];//only one level used 

			for (int z=0; z<nz; z++){
				for (int i=0; i<ni; i++) {  
					for (int j=0;j<nj; j++) {  
						SpeedData[0][z][i][j]=Ei[1][z][i][j] - Ei[0][z][i][j];		
					}	
				}
			}
		}
		else
			SpeedData=null;
		//Tools.disp_vals(SpeedData[1][0], "speedData");
		//Tools.disp_vals(mask[1][0], "mask");

	}

	@Override
	public void  run()
	{
		//p.nlevels=1;//create only one region not 2 : nz-1

		md= new MasksDisplay(ni,nj,nz,nl,p.cl,p);
		ASplitBregmanSolver A_solver;
		//TODO save test
		p.cl[0]=p.betaMLEoutdefault;
		//p.cl[1]=0.2340026;
		//p.cl[1]=0.2;
		p.cl[1]=p.betaMLEindefault;

		//p.cl[0]=0.006857039757524;;
		//p.cl[1]=0.709785769586498;
		p.nlevels=1;

		//IJ.log(String.format("Photometry default:%n backgroung %7.2e %n foreground %7.2e", p.cl[0],p.cl[1]));
		//Tools.showmem();
		if (p.usePSF && p.nz>1)
		{
			Tools.gaussian3Dbis(p.PSF, p.kernelx, p.kernely, p.kernelz, 7, p.sigma_gaussian*p.model_oversampling, p.zcorrec);

			A_solver= new ASplitBregmanSolverTwoRegions3DPSF(p,image,SpeedData,mask,md,channel,null);
		}
		else if (p.usePSF && p.nz==1)
		{

			//Tools.gaussian2D(p.PSF[0], p.kernelx, p.kernely, 7, 0.8);
			Tools.gaussian2D(p.PSF[0], p.kernelx, p.kernely, 7, p.sigma_gaussian*p.model_oversampling);
			//Tools.disp_valsc(p.PSF[1], "PSF computed 1");

			A_solver= new ASplitBregmanSolverTwoRegionsPSF(p,image,SpeedData,mask,md,channel,null);

		}
		else if (!p.usePSF && p.nz>1){
			//Tools.gaussian3D(p.PSF, p.kernelx, p.kernely,p.kernelz, 7, 1);
			A_solver= new ASplitBregmanSolverTwoRegions3D(p,image,SpeedData,mask,md,channel,null);
		}
		else //if (!p.usePSF && p.nz==1)
			A_solver= new ASplitBregmanSolverTwoRegions(p,image,SpeedData,mask,md,channel,null);

		//first run
		try 
		{
			//Tools.showmem();
			A_solver.first_run();
			//Tools.showmem();
		}
		catch (InterruptedException ex) {}
		if(channel==0){	

			//Analysis.maska=A_solver.w3kbest[0];//

			Analysis.setMaskaTworegions(A_solver.w3kbest[0]);
			//Analysis.setMaskaTworegions(A_solver.w3kbest[0],A_solver.bp_watermask);
			Analysis.bestEnergyX=A_solver.bestNrj;

			//A_solver A
			float [][][] RiN ;
			RiN = new float [p.nz][p.ni][p.nj];
			LocalTools.copytab(RiN, A_solver.Ri[0]);
			float [][][] RoN ;
			RoN = new float [p.nz][p.ni][p.nj];
			LocalTools.copytab(RoN, A_solver.Ro[0]);

			ArrayList<Region> regions=A_solver.regionsvoronoi;

			//A_solver=null; //for testing

			if(!Analysis.p.looptest){
				if(p.findregionthresh)Analysis.compute_connected_regions_a((int) 255*p.thresh,RiN);
				else Analysis.compute_connected_regions_a((int) 255*p.thresh,null);
				//A_solver=null; // for testing
				//test
				//IJ.log("start test" + "nlevels " +p.nlevels);
				if(Analysis.p.refinement&& Analysis.p.mode_voronoi2){
					Analysis.setregionsThresholds(Analysis.regionslistA, RiN, RoN);
					Analysis.SetRegionsObjsVoronoi(Analysis.regionslistA, regions, RiN);
					IJ.showStatus("Computing segmentation  " + 55 + "%");
					IJ.showProgress(0.55);
					
					//Tools.showmem();
					
					ImagePatches ipatches= new ImagePatches(p,Analysis.regionslistA,image,channel, A_solver.w3kbest[0]);
					A_solver=null;
					ipatches.run();
					Analysis.regionslistA=ipatches.regionslist_refined;
					Analysis.regionsA=ipatches.regions_refined;
					Analysis.imagecolor_c1=ipatches.imagecolor_c1;
					//Tools.showmem();
				}



				if(Analysis.p.refinement && Analysis.p.mode_classic){
					ImagePatches ipatches= new ImagePatches(p, Analysis.regionslistA,image,channel, A_solver.w3kbest[0]);
					A_solver=null;
					ipatches.run();
					Analysis.regionslistA=ipatches.regionslist_refined;
					Analysis.regionsA=ipatches.regions_refined;
				}
				//Analysis.testpatch(Analysis.regionslistA, image);
			}
			//			else
			//				Analysis.A_solverX=A_solver; // add for loop settings
		}
		else{
			//Analysis.maskb=A_solver.w3kbest[0];	

			
			Analysis.setMaskbTworegions(A_solver.w3kbest[0]);
			//Analysis.setMaskaTworegions(A_solver.w3kbest[0],A_solver.bp_watermask);
			Analysis.bestEnergyY=A_solver.bestNrj;

			//A_solver A
			float [][][] RiN ;
			RiN = new float [p.nz][p.ni][p.nj];
			LocalTools.copytab(RiN, A_solver.Ri[0]);
			float [][][] RoN ;
			RoN = new float [p.nz][p.ni][p.nj];
			LocalTools.copytab(RoN, A_solver.Ro[0]);

					ArrayList<Region> regions=A_solver.regionsvoronoi;

			//A_solver=null;
			
			
			if(!Analysis.p.looptest){
				if(p.findregionthresh)Analysis.compute_connected_regions_b((int) 255*p.thresh,RiN);
				else Analysis.compute_connected_regions_b((int) 255*p.thresh,null);
				//A_solver=null;


				if(Analysis.p.refinement&& Analysis.p.mode_voronoi2){
					Analysis.setregionsThresholds(Analysis.regionslistB, RiN, RoN);
					Analysis.SetRegionsObjsVoronoi(Analysis.regionslistB, regions, RiN);
					IJ.showStatus("Computing segmentation  " + 55 + "%");
					IJ.showProgress(0.55);
					
					ImagePatches ipatches= new ImagePatches(p,Analysis.regionslistB,image,channel, A_solver.w3kbest[0]);
					A_solver=null;
					ipatches.run();
					Analysis.regionslistB=ipatches.regionslist_refined;
					Analysis.regionsB=ipatches.regions_refined;
					Analysis.imagecolor_c2=ipatches.imagecolor_c1;
				}



				if(Analysis.p.refinement && Analysis.p.mode_classic){
					ImagePatches ipatches= new ImagePatches(p, Analysis.regionslistB,image,channel, A_solver.w3kbest[0]);
					A_solver=null;
					ipatches.run();
					Analysis.regionslistB=ipatches.regionslist_refined;
					Analysis.regionsB=ipatches.regions_refined;
				}

			}
			//			else
			//				Analysis.A_solverY=A_solver;
		}


		//correct the level number
		p.nlevels=2;
		DoneSignal.countDown();

	}
}
