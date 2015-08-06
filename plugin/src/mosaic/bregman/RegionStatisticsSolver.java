package mosaic.bregman;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;
import net.sf.javaml.tools.weka.WekaClusterer;
import weka.clusterers.SimpleKMeans;

/**
 * 
 * Regions statistics solver
 * 
 * @author i-bird
 *
 */
class RegionStatisticsSolver {
	private double [] [] [] Z;
	private double [] [] [] W;
	private double [] [] [] mu;
	private int max_iter;
	private Parameters p;

	private double [] [] [] weights;
	private double [] [] [] image;
	private double [] [] [] KMask;

	public double betaMLEin, betaMLEout;

	/**
	 * 
	 * Create a region statistic solver
	 * 
	 * @param temp1 buffer of the same size of image for internal calculation
	 * @param temp2 buffer of the same size of image for internal calculation
	 * @param temp3 buffer of the same size of image for internal calculation
	 * @param image The image pixel array
	 * @param weights
	 * @param max_iter Maximum number of iteration for the Fisher scoring
	 * @param p
	 */
	
	public RegionStatisticsSolver(double [] [] []  temp1, double [] [] [] temp2, double [][][] temp3,
			double [] [] [] image, double [][][] weights, int max_iter, Parameters p
			){
		this.p=p;
		this.Z=image;
		this.W=temp1;
		this.mu=temp2;
		this.KMask=temp3;
		this.image=image;
		this.max_iter=max_iter;
		this.weights=weights;

	}

//	Class<T> cls;
//	Img<T> image2;
//	
//	public RegionStatisticsSolver(Img<T> img,int max_iter, Class<T> cls)
//	{
//		this.image2 = img;
//		this.cls = cls;
//		
//		// Create native arrays for fast computation
//		
////		IntensityImage mimg = new IntensityImage(MosaicUtils.getImageIntDimensions(img));
////		IntensityImage W = new IntensityImage(MosaicUtils.getImageIntDimensions(img));
////		IntensityImage z = new IntensityImage(MosaicUtils.getImageIntDimensions(img));
////		IntensityImage mu = new IntensityImage(MosaicUtils.getImageIntDimensions(img));
//		
//		fill_weights();
//	}

	/**
	 * 
	 * Create a region statistic solver
	 * 
	 * @param temp1 buffer of the same size of image for internal calculation
	 * @param temp2 buffer of the same size of image for internal calculation
	 * @param temp3 buffer of the same size of image for internal calculation
	 * @param image The image pixel array
	 * @param weights
	 * @param max_iter Maximum number of iteration for the Fisher scoring
	 * @param p
	 */
	
	public RegionStatisticsSolver(double [] [] []  temp1, double [] [] [] temp2, double [][][] temp3,
			double [] [] [] image, int max_iter, Parameters p
			){
		this.p=p;
		this.Z=image;
		this.W=temp1;
		this.mu=temp2;
		this.KMask=temp3;
		this.image=image;
		this.max_iter=max_iter;
		fill_weights();
	}

	private void fill_weights(){
		int ni=p.ni;
		int nj=p.nj;
		int nz=p.nz;
		this.weights= new double [nz][ni][nj];
		for (int z=0; z<nz; z++) {  
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {   
					weights[z][i][j]=1;
				}	
			}
		}
	}

	/**
	 * 
	 * Evaluate the region intensity
	 * 
	 * @param Mask
	 */
	
/*	public  void eval(Img<T> Mask)
	{		
		//do 3D version
		//use mu as temp tab
		//Tools.disp_vals(Mask[0],"Mask0");
		//IJ.log("ni " + ni + "nj" + nj);
		
		// normalize Mask
		
		this.scale_mask(W,Mask);
		
		// Convolve the mask
		
		if (nz==1)
			Tools.convolve2Dseparable(KMask[0], W[0], ni, nj, Analysis.p.kernelx, Analysis.p.kernely, Analysis.p.px, Analysis.p.py, mu[0]);
		else
			Tools.convolve3Dseparable(KMask, W,
					ni,nj,nz,
					Analysis.p.kernelx, Analysis.p.kernely, Analysis.p.kernelz,
					Analysis.p.px, Analysis.p.py,Analysis.p.pz,
					mu);
		
		//Tools.disp_vals(KMask[0],"Kmask");
		//		
		//IJ.log("KMask done");

		//mu  = muInit;K11 //=Ih
		//Z   = link(mu)+(I_h-mu).*linkDerivative(mu);// = Ih
		//W   = priorWeights./(varFunction(mu).*linkDerivative(mu).^2+eps);

		double K11=0, K12=0, K22=0, U1=0, U2=0;
		double detK = 0;
		betaMLEout = 0; betaMLEin = 0;

		//		Tools.copytab(mu, image);
		//Tools.copytab(Z, image);


		for (int z=0; z<nz; z++) 
		{
			for (int i=0; i<ni; i++) 
			{
				for (int j=0;j< nj; j++) 
				{
					if (Z[z][i][j] != 0)
						W[z][i][j]=weights[z][i][j]/Z[z][i][j];
					else 
						W[z][i][j]=4.50359962737e+15;//1e4;
					//W[z][i][j]=10000;
				}	
			}
		}

		//Tools.disp_vals(W[0],"W");

		int iter=0;
		while (iter < max_iter)
		{
			K11=0;K12=0;K22=0;
			U1=0;U2=0;
			for (int z=0; z<nz; z++) {  
				for (int i=0; i<ni; i++) {  
					for (int j=0;j< nj; j++) {  
						K11+=W[z][i][j]*Math.pow(1-KMask[z][i][j],2);
						K12+=W[z][i][j]*(1-KMask[z][i][j])*KMask[z][i][j];
						K22+=W[z][i][j]*(KMask[z][i][j])*KMask[z][i][j];
						U1+=W[z][i][j]*(1-KMask[z][i][j])*Z[z][i][j];
						U2+=W[z][i][j]*(KMask[z][i][j])*Z[z][i][j];
					}
				}
			}


			//   detK = K11*K22-K12^2;
			// betaMLE_out = ( K22*U1-K12*U2)/detK;
			// betaMLE_in  = (-K12*U1+K11*U2)/detK;
			detK = K11*K22-Math.pow(K12,2);	
			if (detK!=0){
			betaMLEout = ( K22*U1-K12*U2)/detK;	
			betaMLEin  = (-K12*U1+K11*U2)/detK;
			}
			else{
				betaMLEout=p.betaMLEoutdefault;	
				betaMLEin=p.betaMLEindefault;
			}

			//IJ.log(String.format("K11 %7.2e K22 %7.2e K12 %7.2e U1 %7.2e U2 %7.2e detK %7.2e %n", K11,K22,K12,U1,U2, detK));

			//mu update
			for (int z=0; z<nz; z++) {  
				for (int i=0; i<ni; i++) {  
					for (int j=0;j< nj; j++) {  
						mu[z][i][j]=(betaMLEin-betaMLEout)*KMask[z][i][j]+betaMLEout;
					}	
				}
			}

			//Tools.disp_vals(mu[0],"m	u");


			//Z= image
			//W update
			for (int z=0; z<nz; z++) {
				for (int i=0; i<ni; i++) {  
					for (int j=0;j< nj; j++) {  
						if (mu[z][i][j] != 0)
							W[z][i][j]=weights[z][i][j]/mu[z][i][j];
						else 
							W[z][i][j]=4.50359962737e+15;//10000;//Double.MAX_VALUE;
					}	
				}
			}

			//Tools.disp_vals(W[0],"W");

			//IJ.log(String.format("Photometry %d:%n backgroung %7.2e %n foreground %7.2e", iter,betaMLEout,betaMLEin));
			//	
			iter++;
		}
	}*/
	
	/**
	 * 
	 * Evaluate the region intensity
	 * 
	 * @param Mask
	 */
	
	public  void eval(double [][][] Mask){

		int ni=p.ni;
		int nj=p.nj;
		int nz=p.nz;

		//do 3D version
		//use mu as temp tab
		//Tools.disp_vals(Mask[0],"Mask0");
		//IJ.log("ni " + ni + "nj" + nj);
		
		// normalize Mask
		
		this.scale_mask(W,Mask);
		
		// Convolve the mask
		
		if (nz==1)
			Tools.convolve2Dseparable(KMask[0], W[0], ni, nj, Analysis.p.PSF, mu[0]);
		else
			Tools.convolve3Dseparable(KMask, W,
					ni,nj,nz,
					Analysis.p.PSF,
					mu);
		
		//Tools.disp_vals(KMask[0],"Kmask");
		//		
		//IJ.log("KMask done");

		//mu  = muInit;K11 //=Ih
		//Z   = link(mu)+(I_h-mu).*linkDerivative(mu);// = Ih
		//W   = priorWeights./(varFunction(mu).*linkDerivative(mu).^2+eps);

		double K11=0, K12=0, K22=0, U1=0, U2=0;
		double detK = 0;
		betaMLEout = 0; betaMLEin = 0;

		//		Tools.copytab(mu, image);
		//Tools.copytab(Z, image);


		for (int z=0; z<nz; z++) 
		{
			for (int i=0; i<ni; i++) 
			{
				for (int j=0;j< nj; j++) 
				{
					if (Z[z][i][j] != 0)
						W[z][i][j]=weights[z][i][j]/Z[z][i][j];
					else 
						W[z][i][j]=4.50359962737e+15;//1e4;
					//W[z][i][j]=10000;
				}	
			}
		}

		//Tools.disp_vals(W[0],"W");

		int iter=0;
		while (iter < max_iter)
		{
			K11=0;K12=0;K22=0;
			U1=0;U2=0;
			for (int z=0; z<nz; z++) {  
				for (int i=0; i<ni; i++) {  
					for (int j=0;j< nj; j++) {  
						K11+=W[z][i][j]*Math.pow(1-KMask[z][i][j],2);
						K12+=W[z][i][j]*(1-KMask[z][i][j])*KMask[z][i][j];
						K22+=W[z][i][j]*(KMask[z][i][j])*KMask[z][i][j];
						U1+=W[z][i][j]*(1-KMask[z][i][j])*Z[z][i][j];
						U2+=W[z][i][j]*(KMask[z][i][j])*Z[z][i][j];
					}
				}
			}


			//   detK = K11*K22-K12^2;
			// betaMLE_out = ( K22*U1-K12*U2)/detK;
			// betaMLE_in  = (-K12*U1+K11*U2)/detK;
			detK = K11*K22-Math.pow(K12,2);	
			if (detK!=0){
			betaMLEout = ( K22*U1-K12*U2)/detK;	
			betaMLEin  = (-K12*U1+K11*U2)/detK;
			}
			else{
				betaMLEout=p.betaMLEoutdefault;	
				betaMLEin=p.betaMLEindefault;
			}

			//IJ.log(String.format("K11 %7.2e K22 %7.2e K12 %7.2e U1 %7.2e U2 %7.2e detK %7.2e %n", K11,K22,K12,U1,U2, detK));

			//mu update
			for (int z=0; z<nz; z++) {  
				for (int i=0; i<ni; i++) {  
					for (int j=0;j< nj; j++) {  
						mu[z][i][j]=(betaMLEin-betaMLEout)*KMask[z][i][j]+betaMLEout;
					}	
				}
			}

			//Tools.disp_vals(mu[0],"m	u");


			//Z= image
			//W update
			for (int z=0; z<nz; z++) {
				for (int i=0; i<ni; i++) {  
					for (int j=0;j< nj; j++) {  
						if (mu[z][i][j] != 0)
							W[z][i][j]=weights[z][i][j]/mu[z][i][j];
						else 
							W[z][i][j]=4.50359962737e+15;//10000;//Double.MAX_VALUE;
					}	
				}
			}

			//Tools.disp_vals(W[0],"W");

			//IJ.log(String.format("Photometry %d:%n backgroung %7.2e %n foreground %7.2e", iter,betaMLEout,betaMLEin));
			//	
			iter++;
		}
	}


	private void scale_mask(double [][][] ScaledMask, double [][][] Mask ){
		int ni=p.ni;
		int nj=p.nj;
		int nz=p.nz;



		double max=0;
		double min=Double.POSITIVE_INFINITY;

		//get imagedata and copy to array image


		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++){  
				for (int j=0;j< nj; j++){  	
					if (Mask[z][i][j]>max)max=Mask[z][i][j];
					if (Mask[z][i][j]<min)min=Mask[z][i][j];
				}	
			}
		}
		//IJ.log("max"+max+"min"+min);

		if ((max-min)!=0){
			for (int z=0; z<nz; z++){
				for (int i=0; i<ni; i++){  
					for (int j=0;j<nj; j++){  
						ScaledMask[z][i][j]= (Mask[z][i][j] -min)/(max-min);		
					}	
				}
			}
		}


	}

//
//	public  void eval(double [][][] Mask, double [][][] Ri,double [][][] Ro, ArrayList<Region> regionslist){
//
//		int ni=Analysis.p.ni;
//		int nj=Analysis.p.nj;
//		int nz=Analysis.p.nz;
//
//		//do 3D version
//		//use mu as temp tab
//		//Tools.disp_vals(Mask[0],"Mask0");
//
//		this.scale_mask(W,Mask);
//		Tools.convolve2Dseparable(KMask[0], W[0], ni, nj, Analysis.p.PSF, mu[0]);
//		//Tools.disp_vals(KMask[0],"Kmask");
//		//		
//		//IJ.log("KMask done");
//
//		//mu  = muInit;K11 //=Ih
//		//Z   = link(mu)+(I_h-mu).*linkDerivative(mu);// = Ih
//		//W   = priorWeights./(varFunction(mu).*linkDerivative(mu).^2+eps);
//
//		double K11=0, K12=0, K22=0, U1=0, U2=0;
//		double detK = 0;
//		betaMLEout = 0; betaMLEin = 0;
//
//		//		Tools.copytab(mu, image);
//		//Tools.copytab(Z, image);
//
//
//
//		for (int z=0; z<nz; z++) {  
//			for (int i=0; i<ni; i++) {  
//				for (int j=0;j< nj; j++) {  
//					if (Z[z][i][j] != 0)
//						W[z][i][j]=1/Z[z][i][j];
//					else 
//						W[z][i][j]=4.50359962737e+15;//1e4;
//					//W[z][i][j]=10000;
//				}	
//			}
//		}
//
//		//Tools.disp_vals(W[0],"W");
//
//		int iter=0;
//		while (iter < max_iter){
//
//
//
//			for (Iterator<Region> itr = regionslist.iterator(); itr.hasNext();) {
//				Region r = itr.next();
//
//
//				K11=0;K12=0;K22=0;
//				U1=0;U2=0;
//
//				for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
//					Pix p = it.next();
//					int i=p.px;
//					int j=p.py;
//					int z=p.pz;
//					K11+=W[z][i][j]*Math.pow(1-KMask[z][i][j],2);
//					K12+=W[z][i][j]*(1-KMask[z][i][j])*KMask[z][i][j];
//					K22+=W[z][i][j]*(KMask[z][i][j])*KMask[z][i][j];
//					U1+=W[z][i][j]*(1-KMask[z][i][j])*Z[z][i][j];
//					U2+=W[z][i][j]*(KMask[z][i][j])*Z[z][i][j];
//				}
//
//
//
//				//   detK = K11*K22-K12^2;
//				// betaMLE_out = ( K22*U1-K12*U2)/detK;
//				// betaMLE_in  = (-K12*U1+K11*U2)/detK;
//				detK = K11*K22-Math.pow(K12,2);		
//				betaMLEout = Math.max((K22*U1-K12*U2)/detK, 0.0001);	
//				betaMLEin  = Math.min((-K12*U1+K11*U2)/detK,0.9);
//				betaMLEout = ( K22*U1-K12*U2)/detK;	
//				betaMLEin  = (-K12*U1+K11*U2)/detK;
//
//
//				//IJ.log(String.format("K11 %7.2e K22 %7.2e K12 %7.2e U1 %7.2e U2 %7.2e detK %7.2e %n", K11,K22,K12,U1,U2, detK));
//				//IJ.log(String.format("bin %7.2e bout %7.2e  region %d iter %d %n", betaMLEin,betaMLEout, r.value, iter ));
//				//mu update
//				for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
//					Pix p = it.next();
//					int i=p.px;
//					int j=p.py;
//					int z=p.pz;
//
//					Ri[z][i][j]=betaMLEin;
//					Ro[z][i][j]=betaMLEout;
//					mu[z][i][j]=(betaMLEin-betaMLEout)*KMask[z][i][j]+betaMLEout;
//
//
//				}
//
//				//Tools.disp_vals(mu[0],"mu");
//
//
//				//Z= image
//				//W update
//				for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
//					Pix p = it.next();
//					int i=p.px;
//					int j=p.py;
//					int z=p.pz;
//					if (mu[z][i][j] != 0)
//						W[z][i][j]=1/mu[z][i][j];
//					else 
//						W[z][i][j]=4.50359962737e+15;//10000;//Double.MAX_VALUE;
//
//				}
//
//				//Tools.disp_vals(W[0],"W");
//
//				//IJ.log(String.format("Photometry %d:%n backgroung %7.2e %n foreground %7.2e", iter,betaMLEout,betaMLEin));
//				//	
//			}
//			iter++;
//		}
//
//
//
//	}
//

	void cluster_region(float [][][] Ri,float [][][] Ro, ArrayList<Region> regionslist){
		int nk=3;//3
		double [] pixel = new double[1];
		double [] levels= new double[nk];


		for (Iterator<Region> itr = regionslist.iterator(); itr.hasNext();) {
			Region r = itr.next();
			Dataset data = new DefaultDataset();
			//IJ.log("Region " + r.value + "size :" + r.points);
			if (r.points<6)continue;
			for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
				Pix p = it.next();
				int i=p.px;
				int j=p.py;
				int z=p.pz;
				//IJ.log("i j z " +i +" " +j+ " "+ z +"val" +  image[z][i][j]);
				pixel[0]=image[z][i][j];
				Instance instance = new DenseInstance(pixel);
				data.add(instance);
			}


			//						//IJ.log("clust .. ");
			//						Clusterer km = new KMeans(3,200);
			//						//Clusterer km = new KMeans(3,200);
			//						/* Cluster the data, it will be returned as an array of data sets, with
			//						 * each dataset representing a cluster. */
			//						Dataset[] data2 = km.cluster(data);
			//						//IJ.log("clust done ");



			/* Create Weka classifier */
			SimpleKMeans xm = new SimpleKMeans();
			try{
				xm.setNumClusters(3);//3
				xm.setMaxIterations(100);
			}catch (Exception ex) {}

			/* Wrap Weka clusterer in bridge */
			Clusterer jmlxm = new WekaClusterer(xm);
			/* Perform clustering */
			Dataset[] data2 = jmlxm.cluster(data);
			/* Output results */
			//System.out.println(clusters.length);


			nk=data2.length;//get number of clusters  really found (usually = 3 = setNumClusters but not always)
			for (int i=0; i<nk; i++) {  
				//Instance inst =DatasetTools.minAttributes(data2[i]);
				Instance inst =DatasetTools.average(data2[i]);
				levels[i]=inst.value(0);
			}


			Arrays.sort(levels);
			//			IJ.log("");
			//			IJ.log("levels :");
			//			IJ.log("level 1 : " + levels[0]);
			//			IJ.log("level 2 : " + levels[1]);
			//			IJ.log("level 3 : " + levels[2]);



			//IJ.log("nk " + nk + "parameter " + Analysis.p.regionSegmentLevel + "level" + levels[Math.min(Analysis.p.regionSegmentLevel, nk-1)] );
			nk=Math.min(Analysis.p.regionSegmentLevel, nk-1);
			betaMLEin=levels[nk];//-1;
			//betaMLEout=levels[0];
			int nkm1=Math.max(nk-1, 0);
			betaMLEout=levels[nkm1];

			//IJ.log("bin " + betaMLEin + " bout "+betaMLEout);

			if (p.mode_voronoi2){
				for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {

					Pix p = it.next();
					int i=p.px;
					int j=p.py;
					int z=p.pz;
					Ri[z][i][j]=regionslist.indexOf(r);
				}	
			}
			else{
				for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {

					Pix p = it.next();
					int i=p.px;
					int j=p.py;
					int z=p.pz;
					Ri[z][i][j]=(float) (255*betaMLEin);
					Ro[z][i][j]=(float) (255*betaMLEout);


				}
			}
			r.beta_in=(float) betaMLEin;
			r.beta_out=(float) betaMLEout;
		}
		// IJ.log("Region cluster done");

	}


	void cluster_region_voronoi2(float [][][] Ri,float [][][] Ro, ArrayList<Region> regionslist){
		//int nk=3;//3

		//double [] levels= new double[nk];

		for (Iterator<Region> itr = regionslist.iterator(); itr.hasNext();) {
			Region r = itr.next();
			//IJ.log("region" + r.value);
			for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {

				Pix p = it.next();
				int i=p.px;
				int j=p.py;
				int z=p.pz;
				Ri[z][i][j]=regionslist.indexOf(r);
				//IJ.log("i"+i +"j"+j + "index" +regionslist.indexOf(r));
			}	

		}
		// IJ.log("Region cluster done");

	}


}



