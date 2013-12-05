package mosaic.bregman;

import java.util.concurrent.CountDownLatch;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;


public class Tools 
{
	static ImagePlus imgd=new ImagePlus();
	static boolean disp=true;
	public  int ni,nj,nz,nlevels;

	//convolution with symmetric boundaries extension

	
	
	public Tools(int nni, int nnj, int nnz){
		this.ni=nni;
		this.nj=nnj;
		this.nz=nnz;
		this.nlevels=1;
	}

	public Tools(int nni, int nnj, int nnz, int nnl){
		this.ni=nni;
		this.nj=nnj;
		this.nz=nnz;
		this.nlevels=nnl;
	}
	
	public void showmem(){
		// Get current size of heap in bytes
		long heapSize = Runtime.getRuntime().totalMemory();

		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.
		// Any attempt will result in an OutOfMemoryException.
		long heapMaxSize = Runtime.getRuntime().maxMemory();

		// Get amount of free memory within the heap in bytes. This size will increase
		// after garbage collection and decrease as new objects are created.
		long heapFreeSize = Runtime.getRuntime().freeMemory();
		
		long used = heapSize- heapFreeSize;
		
		IJ.log(" ");
		IJ.log("Total mem" + round(heapSize/Math.pow(2, 20),2));
		IJ.log("Max mem" + round(heapMaxSize/Math.pow(2, 20),2));
		IJ.log("Free mem" + round(heapFreeSize/Math.pow(2, 20),2));
		IJ.log("Used mem" + round(used/Math.pow(2, 20),2));
		IJ.log(" ");
	}

	
	public  void setDims(int i, int j, int z, int n){
		ni=i;nj=j;nz=z;nlevels=n;
	}

	//convolution with symmetric boundaries extension
	public static void convolve2D(double [] [] out,double[] [] in, int icols, int irows, 
			double [] [] kernel, int kx, int ky)
	{
		int i, j, m, n, mm, nn;
		int kCenterX, kCenterY;                         // center index of kernel
		double sum;                                      // temp accumulation buffer
		int rowIndex, colIndex;
		//IJ.log("apres xmin "+ icols + "ymin" + irows);

		// check validity of params
		//if(!in || !out || !kernel) return ;
		///if(ix <= 0 || kx <= 0) return ;

		// find center position of kernel (half of kernel size)
		kCenterX = kx / 2;
		kCenterY = ky / 2;

		for(i=0; i < icols; ++i)                // columns
		{
			for(j=0; j < irows; ++j)            // rows
			{
				sum = 0;                            // init to 0 before sum
				for(m=0; m < kx; ++m)      // kernel cols
				{
					mm = kx - 1 - m;       // col index of flipped kernel

					for(n=0; n < ky; ++n)  // kernel rows
					{
						nn = ky - 1 - n;   // row index of flipped kernel


						// index of input signal, used for checking boundary
						colIndex = i + m - kCenterX;
						rowIndex = j + n - kCenterY;


						if(rowIndex >= 0 && rowIndex < irows && colIndex >= 0 && colIndex < icols)
							sum += in[colIndex][rowIndex] * kernel[mm] [nn];
						// syymetric boundaries
						else{
							if(rowIndex < 0)rowIndex= - rowIndex -1;
							if(rowIndex > irows-1)rowIndex=irows-(rowIndex-irows) -1;

							if(colIndex < 0)colIndex=- colIndex -1;
							if(colIndex > icols-1)colIndex=icols-(colIndex-icols) -1;
							sum += in[colIndex][rowIndex] * kernel[mm][nn];							
						}

					}
				}
				out[i] [j] = sum;
			}
		}
		return ;
	}


	public static void convolve2Dseparable(double [] [] out,double[] [] in, int icols, int irows, 
			double [] kernelx, double kernely [], int kx, int ky, double temp [][]){
		convolve2Dseparable( out, in,  icols,  irows, 
				kernelx,  kernely ,  kx,  ky,  temp,  0,  icols);
	}


	public static void convolve2Dseparable(double [] [] out,double[] [] in, int icols, int irows, 
			double [] kernelx, double [] kernely, int kx, int ky, double [] [] temp, int iStart, int iEnd)
	{
		int i, j, m, n, mm, nn;
		int kCenterX, kCenterY;                         // center index of kernel
		double sum;                                      // temp accumulation buffer
		int rowIndex, colIndex;

		kCenterX = kx / 2;
		kCenterY = ky / 2;
		//IJ.log("convolve" + "irows" + irows+ "icols" + icols + "istart" + iStart + "iend" + iEnd);
		//convolve in x (i coordinate), horizontal
		for(i=iStart; i < iEnd; ++i)                // columns
		{
			for(j=0; j < irows; ++j)            // rows
			{
				sum = 0;                            // init to 0 before sum
				for(m=0; m < kx; ++m)      // kernel cols
				{
					mm = kx - 1 - m;       // col index of flipped kernel

					// index of input signal, used for checking boundary
					colIndex = i + m - kCenterX;
					rowIndex = j;


					if(colIndex >= 0 && colIndex < icols)
						sum += in[colIndex][rowIndex] * kernelx[mm];
					// syymetric boundaries
					else{
						if(colIndex < 0)colIndex=- colIndex -1;
						if(colIndex > icols-1)colIndex=icols-(colIndex-icols) -1;
						sum += in[colIndex][rowIndex] * kernelx[mm];							
					}


				}
				temp[i] [j] = sum;
			}
		}


		//convolve in y (j coordinate), vertical
		for(i=iStart; i < iEnd; ++i)                    // columns
		{
			for(j=0; j < irows; ++j)            // rows
			{
				sum = 0;                            // init to 0 before sum
				for(n=0; n < ky; ++n)  // kernel rows
				{
					nn = ky - 1 - n;   // row index of flipped kernel

					// index of input signal, used for checking boundary
					colIndex = i ;
					rowIndex = j + n - kCenterY;

					if(rowIndex >= 0 && rowIndex < irows)
						sum += temp[colIndex][rowIndex] * kernely[nn];
					// syymetric boundaries
					else{
						if(rowIndex < 0)rowIndex= - rowIndex -1;
						if(rowIndex > irows-1)rowIndex=irows-(rowIndex-irows) -1;
						sum += temp[colIndex][rowIndex] * kernely[nn];							
					}


				}
				out[i] [j] = sum;
			}
		}

		return ;
	}


	public static void convolve3Dseparable(double [] [] [] out,double [] [] [] in, int icols, int irows,int islices, 
			double [] kernelx, double kernely [],double kernelz [], int kx, int ky, int kz, double temp [][][])
	{
		//IJ.log("irows icols islices kx ky kz" +irows  +" "+ icols  +" "+ islices +" "+ kx +" "+ky +" " +kz);
		convolve3Dseparable(out,in, icols, irows,islices, 
				kernelx, kernely ,kernelz ,kx, ky,kz, temp ,0, icols);

	}


	public static void convolve3Dseparable(double [] [] [] out,double [] [] [] in, int icols, int irows, int islices, 
			double [] kernelx, double kernely [],double kernelz [], int kx, int ky, int kz, double temp [][][], int iStart, int iEnd)
	{
		int i, j, k, m, n, l, mm, nn, ll;
		int kCenterX, kCenterY, kCenterZ;                         // center index of kernel
		double sum;                                      // temp accumulation buffer
		int rowIndex, colIndex, sliceIndex;


		// check validity of params
		//if(!in || !out || !kernel) return ;
		///if(ix <= 0 || kx <= 0) return ;

		// find center position of kernel (half of kernel size)
		//double [] kernelx = {0.00176900911404382,	0.0215509428482683,	0.0965846250185641,	0.159241125690702,	0.0965846250185641,	0.0215509428482683,	0.00176900911404382};
		//double [] kernely = {0.011108996538242,	   0.135335283236613,	   0.606530659712635,	   1.000000000000000,	   0.606530659712635,	   0.135335283236613,	   0.011108996538242};

		//double [] [] temp= new double [icols][irows];

		kCenterX = kx / 2;
		kCenterY = ky / 2;
		kCenterZ = kz / 2;

		//convolve in x (i coordinate), horizontal
		for(k=0; k < islices; ++k)                // columns
		{
			for(i=iStart; i < iEnd; ++i)                // columns
			{
				for(j=0; j < irows; ++j)            // rows
				{
					sum = 0;                            // init to 0 before sum
					for(m=0; m < kx; ++m)      // kernel cols
					{
						mm = kx - 1 - m;       // col index of flipped kernel

						// index of input signal, used for checking boundary
						colIndex = i + m - kCenterX;
						rowIndex = j;


						if(colIndex >= 0 && colIndex < icols)
							sum += in[k][colIndex][rowIndex] * kernelx[mm];
						// syymetric boundaries
						else{
							if(colIndex < 0)colIndex=- colIndex -1;
							if(colIndex > icols-1)colIndex=icols-(colIndex-icols) -1;
							sum += in[k][colIndex][rowIndex] * kernelx[mm];							
						}


					}
					//IJ.log(" " +k +" " +i+" "+j);
					out[k][i][j] = sum;
				}
			}
		}


		//convolve in y (j coordinate), vertical
		for(k=0; k < islices; ++k)                // columns
		{
			for(i=iStart; i < iEnd; ++i)                // columns
			{
				for(j=0; j < irows; ++j)            // rows
				{
					sum = 0;                            // init to 0 before sum
					for(n=0; n < ky; ++n)  // kernel rows
					{
						nn = ky - 1 - n;   // row index of flipped kernel

						// index of input signal, used for checking boundary
						colIndex = i ;
						rowIndex = j + n - kCenterY;

						if(rowIndex >= 0 && rowIndex < irows)
							sum += out[k][colIndex][rowIndex] * kernely[nn];
						// syymetric boundaries
						else{
							if(rowIndex < 0)rowIndex= - rowIndex -1;
							if(rowIndex > irows-1)rowIndex=irows-(rowIndex-irows) -1;
							sum += out[k][colIndex][rowIndex] * kernely[nn];							
						}


					}
					temp[k][i][j] = sum;
				}
			}
		}




		//convolve in z (k coordinate), slices
		for(k=0; k < islices; ++k)                // columns
		{
			for(i=iStart; i < iEnd; ++i)          // columns
			{
				for(j=0; j < irows; ++j)            // rows
				{
					sum = 0;                            // init to 0 before sum
					for(l=0; l < kz; ++l)  // kernel slices
					{
						ll = kz - 1 - l;   // row index of flipped kernel

						// index of input signal, used for checking boundary
						colIndex = i ;
						rowIndex = j ;
						sliceIndex = k + l - kCenterZ;

						if(sliceIndex >= 0 && sliceIndex < islices)
							sum += temp[sliceIndex][colIndex][rowIndex] * kernelz[ll];
						// syymetric boundaries
						else{
							if(sliceIndex < 0)sliceIndex=Math.min(islices-1, - sliceIndex -1) ;
							//IJ.log("islices" + islices + "sliceindex "+ sliceIndex );
							if(sliceIndex > islices-1)sliceIndex=Math.max(0, islices-(sliceIndex-islices) -1);
							//IJ.log("sl" + sliceIndex + "col "+ colIndex + "row "+ rowIndex + "ll "+ ll);
							sum += temp[sliceIndex][colIndex][rowIndex] * kernelz[ll];							
						}


					}
					out[k][i][j] = sum;
				}
			}
		}

		return ;
	}



	//generategaussian
	public static void gaussian2D(double [] [] res, double [] resx, double [] resy,  int size, double sigma){
		int center = size/2;
		double sum=0;

		for (int i=0; i<size; i++) {  
			for (int j=0; j<size; j++) {  
				res[i][j]=Math.exp(-(Math.pow(i-center, 2) + Math.pow(j-center, 2))/(2*Math.pow(sigma, 2)));
				sum+=res[i][j];
			}	
		}

		for (int i=0; i<size; i++) {  
			for (int j=0; j<size; j++) {  
				res[i][j]=res[i][j]/sum;
			}	
		}

		for (int i=0; i<size; i++) {  
			resx[i]=res[i][center];
		}

		//		for (int i=0;i< 7; i++){  
		//			IJ.log("testx" + resx[i]);			
		//		}


		for (int i=0; i<size; i++) {  
			resy[i]=res[center][i]/res[center][center];
		}	

	}


	public static void gaussian3D(
			double [] [] [] res, 
			double [] resx, double [] resy,double [] resz, int size, double sigma){
		int center = size/2;

		double sum=0;
		for (int k=0; k<size; k++) {
			for (int i=0; i<size; i++) {  
				for (int j=0; j<size; j++) {  
					res[k][i][j]=Math.exp(-(Math.pow(k-center, 2)+ Math.pow(i-center, 2) + Math.pow(j-center, 2))/(2*Math.pow(sigma, 2)));
					sum+=res[k][i][j];
				}	
			}
		}

		for (int k=0; k<size; k++) {
			for (int i=0; i<size; i++) {  
				for (int j=0; j<size; j++) {  
					res[k][i][j]=res[k][i][j]/sum;
				}	
			}
		}


		for (int i=0; i<size; i++) {  
			resx[i]=res[center][i][center];
		}

		for (int i=0; i<size; i++) {  
			resy[i]=res[center][center][i]/res[center][center][center];
		}	

		for (int i=0; i<size; i++) {  
			resz[i]=resy[i];
		}	
	}



	public static void gaussian3Dbis(
			double [] [] [] res, 
			double [] resx, double [] resy,double [] resz, int size, double sigma, double zcorrec){
		int center = size/2;

		double sum=0;
		for (int k=0; k<size; k++) {
			for (int i=0; i<size; i++) {  
				for (int j=0; j<size; j++) {  
					res[k][i][j]=Math.exp(-(Math.pow(zcorrec*(k-center), 2)+ Math.pow(i-center, 2) + Math.pow(j-center, 2))/(2*Math.pow(sigma, 2)));
					sum+=res[k][i][j];
				}	
			}
		}

		for (int i=0;i< 7; i++){  
			//IJ.log("0testx" + res[center][i][center]);			
			//IJ.log("0testy" + res[center][center][i]);
			//IJ.log("0testz" + res[i][center][center]);
		}


		for (int k=0; k<size; k++) {
			for (int i=0; i<size; i++) {  
				for (int j=0; j<size; j++) {  
					res[k][i][j]=res[k][i][j]/sum;
				}	
			}
		}






		for (int i=0; i<size; i++) {  
			resx[i]=res[center][i][center];
		}
		for (int i=0;i< 7; i++){  
			//	IJ.log("testx" + resx[i]);			
		}


		for (int i=0; i<size; i++) {  
			resy[i]=res[center][center][i]/res[center][center][center];
		}

		for (int i=0;i< 7; i++){  
			//	IJ.log("testy" + resy[i]);			
		}

		for (int i=0; i<size; i++) {  
			resz[i]=res[i][center][center]/res[center][center][center];
		}	

		for (int i=0;i< 7; i++){  
			//	IJ.log("testz" + resz[i]);			
		}

	}

	//	public static double accessImage(int z, int i, int j, double [][][] image){
	//		//no interpolation :
	//		return(image[z/Analysis.p.model_oversampling][i/Analysis.p.model_oversampling][j/Analysis.p.model_oversampling]);
	//		//do interpolation ??
	//	}


	//	public static double [][][] resampleimage(double [][][] image){
	//		double [][][]= new double [nz*Analysis.p.model_oversampling][ni*Analysis.p.model_oversampling][nj*Analysis.p.model_oversampling];
	//		//no interpolation :
	//		return(image[z/Analysis.p.model_oversampling][i/Analysis.p.model_oversampling][j/Analysis.p.model_oversampling]);
	//		//do interpolation ??
	//	}


	public  void dctshift(double [] [] [] result, double [] [] [] PSF, int cc, int cr){

		//check if non square image
		int cols=PSF[0].length;
		int rows=PSF[0][0].length; 
		int k = Math.min(cr-1, Math.min(cc-1,Math.min(rows-cr, cols-cc)));

		int frow = cr-k;
		int lrow = cr+k;
		int rowSize = lrow-frow+1;

		int fcol = cc-k;
		int lcol = cc+k;
		int colSize = lcol-fcol+1;

		//IJ.log("ni" + ni + "nj" + nj);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					result[z][i][j]= 0;
				}	
			}
		}

		for (int z=0; z<nz; z++){
			for (int i=0; i<1+colSize - cc + fcol-1; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-1; j++) {  
					result[z][i][j]= PSF[z][cc - fcol+i][cr - frow+j];
				}	
			}
		}



		for (int z=0; z<nz; z++){
			for (int i=0; i<1+colSize - cc + fcol-2; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-1; j++) {  
					result[z][i][j]+= PSF[z][cc - fcol+1+i][cr - frow+j];
				}	
			}
		}


		for (int z=0; z<nz; z++){
			for (int i=0; i<1+colSize - cc + fcol-1; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-2; j++) {  
					result[z][i][j]+= PSF[z][cc - fcol+i][cr - frow+1+j];
				}	
			}
		}


		for (int z=0; z<nz; z++){
			for (int i=0; i<1+colSize - cc + fcol-2; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-2; j++) {  
					result[z][i][j]+= PSF[z][cc - fcol+1+i][cr - frow+1+j];
				}	
			}
		}

		//IJ.log("cols " + cols + "rows" + rows);
		for (int z=0; z<nz; z++){
			for (int i=2*k+1; i<cols; i++) {  
				for (int j=2*k+1; j<rows; j++) {  
					result[z][i][j]= 0;
				}	
			}
		}
		//Tools.disp_valsc(result[0], "dctshift4");


	}



	public  void dctshift3D(double [] [] [] result, double [] [] [] PSF, int cr, int cc, int cs){

		//check if non square image
		int cols=PSF[0].length;
		int rows=PSF[0][0].length; 
		int slices=PSF.length;


		int k = Math.min(cr-1, Math.min(cc-1,Math.min(rows-cr, Math.min(cols-cc, Math.min(cs-1, slices-cs)))));

		int frow = cr-k;
		int lrow = cr+k;
		int rowSize = lrow-frow+1;

		int fcol = cc-k;
		int lcol = cc+k;
		int colSize = lcol-fcol+1;


		int  fslice = cs-k;
		int  lslice = cs+k;
		int  sliceSize = lslice-fslice+1;


		//z1
		for (int z=0; z<1+sliceSize - cs + fslice-1; z++){
			for (int i=0; i<1+colSize - cc + fcol-1; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-1; j++) {  
					result[z][i][j]= PSF[cs - fslice +z][cc - fcol+i][cr - frow+j];
				}	
			}
		}

		//Tools.disp_valsc(result[2], "P1");


		for (int z=0; z<1+sliceSize - cs + fslice-1; z++){
			for (int i=0; i<1+colSize - cc + fcol-2; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-1; j++) {  
					result[z][i][j]+= PSF[cs - fslice +z][cc - fcol+1+i][cr - frow+j];
				}	
			}
		}


		for (int z=0; z<1+sliceSize - cs + fslice-1; z++){
			for (int i=0; i<1+colSize - cc + fcol-1; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-2; j++) {  
					result[z][i][j]+= PSF[cs - fslice +z][cc - fcol+i][cr - frow+1+j];
				}	
			}
		}


		for (int z=0; z<1+sliceSize - cs + fslice-1; z++){
			for (int i=0; i<1+colSize - cc + fcol-2; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-2; j++) {  
					result[z][i][j]+= PSF[cs - fslice +z][cc - fcol+1+i][cr - frow+1+j];
				}	
			}
		}


		//z 2
		for (int z=0; z<1+sliceSize - cs + fslice-2; z++){
			for (int i=0; i<1+colSize - cc + fcol-1; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-1; j++) {  
					result[z][i][j]+= PSF[cs - fslice +1 +z][cc - fcol+i][cr - frow+j];
				}	
			}
		}


		for (int z=0; z<1+sliceSize - cs + fslice-2; z++){
			for (int i=0; i<1+colSize - cc + fcol-2; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-1; j++) {  
					result[z][i][j]+= PSF[cs - fslice +1 +z][cc - fcol+1+i][cr - frow+j];
				}	
			}
		}


		for (int z=0; z<1+sliceSize - cs + fslice-2; z++){
			for (int i=0; i<1+colSize - cc + fcol-1; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-2; j++) {  
					result[z][i][j]+= PSF[cs - fslice +1 +z][cc - fcol+i][cr - frow+1+j];
				}	
			}
		}


		for (int z=0; z<1+sliceSize - cs + fslice-2; z++){
			for (int i=0; i<1+colSize - cc + fcol-2; i++) {  
				for (int j=0; j<1+rowSize - cr + frow-2; j++) {  
					result[z][i][j]+= PSF[cs - fslice +1 +z][cc - fcol+1+i][cr - frow+1+j];
				}	
			}
		}

		//IJ.log("product" + (2*k+1));
		//IJ.log("slices " + slices+ "cols " + cols + "rows " + rows);
		for (int z=2*k+1; z<slices; z++){
			for (int i=2*k+1; i<cols; i++) {  
				for (int j=2*k+1; j<rows; j++) {  
					result[z][i][j]= 0;
				}	
			}
		}

		//Tools.disp_valsc(result[2], "res");
		//Tools.disp_valsc(result[0], "dctshift4");


	}



	// all matrices have same dims
	public  void dotProduct(double[] [] [] res, double [] [] [] m1, double [] [] [] m2)
	{
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					res[z][i][j]=m1[z][i][j]*m2[z][i][j];
				}	
			}
		}
	}

	public  void add_scalar(double [] [] [] res, double [] [] [] m1, double s)
	{
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					res[z][i][j]=m1[z][i][j]+s;
				}	
			}
		}
	}

	public  void times_scalar(double [] [] [] res, double [] [] [] m1, double s)
	{
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					res[z][i][j]=s*m1[z][i][j];
				}	
			}
		}
	}

	public  void addtab(double [] [] [] res, double [] [] [] m1, double [] [] [] m2)
	{
		addtab(res,m1,m2,0,ni);
	}

	public  void addtab(double [] [] [] res, double [] [] [] m1, double [] [] [] m2, int iStart, int iEnd)
	{
		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<nj; j++) {  
					res[z][i][j]=m1[z][i][j]+m2[z][i][j];
				}	
			}
		}
	}



	public  void subtab(double [] [] [] res, double [] [] [] m1, double [] [] [] m2)
	{
		subtab(res,m1,m2,0,ni);
	}

	public  void subtab(double [] [] [] res, double [] [] [] m1, double [] [] [] m2,int iStart, int iEnd)
	{
		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<nj; j++) {  
					res[z][i][j]=m1[z][i][j]-m2[z][i][j];
				}	
			}
		}
	}


	public  void copytab(double [] [] [] res, double [] [] [] m1)
	{
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					res[z][i][j]=m1[z][i][j];
				}	
			}
		}
	}

	public  void copytab(float [] [] [] res, float [] [] [] m1)
	{
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					res[z][i][j]=m1[z][i][j];
				}	
			}
		}
	}

	public  void copytab(int [] [] [] res, int [] [] [] m1)
	{
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					res[z][i][j]=m1[z][i][j];
				}	
			}
		}
	}


	public  double sumtab(double [] [] [] m1)
	{
		double temp =0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					temp+=m1[z][i][j];
				}	
			}
		}
		return temp;
	}

	//nllmeanPoisson
	//todo catch  mu=0
	//if mu is a matrix
	public  void nllMeanPoisson2(double [] [] [] res, double [] [] [] image, double [][][] mu, double  weight, double ldata){
		nllMeanPoisson2( res, image, mu, weight, ldata,
				0, ni);
	}



	public  void nllMeanPoisson2(double [] [] [] res, double [] [] [] image, double [][][] mu, double  weight, double ldata,
			int iStart, int iEnd){


		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0;j< nj; j++) {  
					if(image[z][i][j] !=0)
						res[z][i][j]=ldata*weight*(image[z][i][j]*Math.log(image[z][i][j]/mu[z][i][j]) +mu[z][i][j] -image[z][i][j]);
					else
						res[z][i][j]=ldata*mu[z][i][j];
				}	
			}
		}

	}

	public  void nllMean(double [] [] [] res, double [] [] [] image, double [][][] mu, double  weight, double ldata){
		nllMean( res, image, mu, weight, ldata,
				0, ni);
	}



	public  void nllMean(double [] [] [] res, double [] [] [] image, double [][][] mu, double  weight, double ldata,
			int iStart, int iEnd){

		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0;j< nj; j++) {  
					res[z][i][j]=noise(image[z][i][j], mu[z][i][j]);
				}	
			}
		}

	}

	
	public  void nllMean1(double [] [] [] res, double [] [] [] image, double  mu, double  weight, double ldata){

		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  
					res[z][i][j]=noise(image[z][i][j], mu);
				}	
			}
		}

	}

	

	// mu is a scalar
	public  void nllMeanPoisson(double [] [] [] res, double [] [] [] image, double mu, double  weight, double ldata){


		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  
					if(image[z][i][j] !=0)
						res[z][i][j]=ldata*weight*(image[z][i][j]*Math.log(image[z][i][j]/mu) +mu -image[z][i][j]);
					else
						res[z][i][j]=ldata*mu;
				}	
			}
		}

	}
	public  void nllMeanGauss2(double [] [] [] res, double [] [] [] image, double [][][] mu, double  weight, double ldata){
		nllMeanGauss2( res, image, mu, weight, ldata,
				0, ni);
	}


	public  void nllMeanGauss2(double [] [] [] res, double [] [] [] image, double [][][]mu, double  weight, double ldata,
			int iStart, int iEnd){

		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0;j< nj; j++) {  
					if(image[z][i][j] !=0)
						res[z][i][j]=ldata*(Math.pow(image[z][i][j]-mu[z][i][j],2));
				}	
			}
		}

	}



	private double noise(double im, double mu){
		//
		double res;
		if(mu<0)mu=0.0001;
		if(Analysis.p.noise_model==0){//poisson

			if(im !=0)
				res=(im*Math.log(im/mu) +mu -im);
			else
				res=mu;
			
			if(mu==0)res=im;
		}
		else//gauss
		{
			res =Math.pow(im-mu,2);
		}

		return res;
	}



	public  void nllMeanGauss(double [] [] [] res, double [] [] [] image, double mu, double  weight, double ldata){

		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  
					if(image[z][i][j] !=0)
						res[z][i][j]=ldata*(Math.pow(image[z][i][j]-mu,2));
				}	
			}
		}

	}


	public  void nllMeanBernouilli(double [] [] [] res, double [] [] [] image, double mu, double  weight, double ldata){
		//TODO check if correct

		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  
					if(image[z][i][j] !=0 && image[z][i][j] !=1)
						res[z][i][j]=ldata*(-image[z][i][j]*Math.log(mu/image[z][i][j]) -(1-image[z][i][j])*Math.log((1-mu)/(1-image[z][i][j])));
					else if (image[z][i][j] !=0)
						res[z][i][j]=-ldata*Math.log(1-mu);
					else
						res[z][i][j]=-ldata*Math.log(mu);
				}	
			}
		}
	}


	public  void createmask(double [] [] [] [] res, double [] [] [] image, double [] cl)
	{
		//add 0 and 1 at extremities
		double [] cltemp = new double [nlevels+2];
		cltemp[0]=0;cltemp[nlevels+1]=1;
		for (int l=1; l<nlevels+1; l++)
		{
			cltemp[l]=cl[l-1];
		}
		double thr;

		for (int l=0; l<nlevels; l++)
		{
			if(nlevels>2)thr=cl[l]; else thr=cl[1];//if only two regions only first mask is used
			if(thr==1) thr=0.5;// should not have threhold to 1: creates empty mask and wrong behavior in dct3D  computation
			for (int z=0; z<nz; z++){
				for (int i=0; i<ni; i++) {  
					for (int j=0;j< nj; j++) {
						if(image[z][i][j]>=thr)//  && image[z][i][j]<=cltemp[l+2])
							res[l][z][i][j]=1;
						else
							res[l][z][i][j]=0;
					}	
				}
			}
		}


	}

	public  int computediff(int [] [] [] mmask1, int [] [] [] mmask2){

		int diff=0;

		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {
					if (mmask1[z][i][j]!=mmask2[z][i][j])diff++;
				}	
			}
		}
		return diff;
	}



	public  void fgradz2D(double [] [] [] res, double [] [] [] im){
		fgradz2D(res,im,0,ni);
	}

	public  void fgradz2D(double [] [] [] res, double [] [] [] im, int tStart, int tEnd){

		for (int z=0; z<nz-1; z++){
			for (int i=tStart; i<tEnd; i++) {  
				for (int j=0;j< nj; j++) {  
					res[z][i][j]= im[z+1][i][j]-im[z][i][j];			
				}	
			}
		}

		//von neumann boundary topslice
		for (int i=tStart; i<tEnd; i++) {  
			for (int j=0;j<nj; j++) {  
				res[nz-1][i][j]= 0;			
			}	
		}

	}


	public  void fgradx2D(double [] [] [] res, double [] [] [] im){
		fgradx2D( res,  im, 0, nj);
	}

	public  void fgradx2D(double [] [] [] res, double [] [] [] im, int tStart, int tEnd){

		for (int z=0; z<nz; z++){
			for (int i=0; i<ni-1; i++) {  
				for (int j=tStart;j< tEnd; j++) {  
					res[z][i][j]= im[z][i+1][j]-im[z][i][j];			
				}	
			}

			//von neumann boundary right
			for (int j=tStart;j< tEnd; j++) {  
				res[z][ni-1][j]= 0;			
			}
		}

	}	

	public  void fgrady2D(double [] [] [] res, double [] [] [] im){
		fgrady2D(res,im,0,ni);
	}

	//if x and y do same chunk : possibility to remove one synchronization after each gradient computation
	public  void fgrady2D(double [] [] [] res, double [] [] [] im,int tStart, int tEnd){

		for (int z=0; z<nz; z++){
			for (int j=0;j< nj-1; j++) {  
				for (int i=tStart; i<tEnd; i++) {  
					res[z][i][j]= im[z][i][j+1]-im[z][i][j];			
				}	
			}
			//von neumann boundary bottom
			for (int i=tStart;i< tEnd; i++) {  
				res[z][i][nj-1]= 0;			
			}
		}

	}


	public  void bgradxdbc2D(double [] [] [] res, double [] [] [] im){
		bgradxdbc2D(res,  im, 0, nj);
	}

	public  void bgradxdbc2D(double [] [] [] res, double [] [] [] im, int tStart, int tEnd){

		for (int z=0; z<nz; z++){
			for (int i=1; i<ni-1; i++) {  
				for (int j=tStart;j< tEnd; j++) {  
					res[z][i][j]= -im[z][i-1][j]+im[z][i][j];			
				}	
			}

			for (int j=tStart;j< tEnd; j++) {  
				//dirichlet boundary right
				res[z][ni-1][j]= -im[z][ni-2][j];			
				//dirichlet boundary left
				res[z][0][j]= im[z][0][j];
			}
		}

	}

	public  void bgradzdbc2D(double [] [] [] res, double [] [] [] im){
		bgradzdbc2D(res,  im, 0, ni);
	}

	public  void bgradzdbc2D(double [] [] [] res, double [] [] [] im, int tStart, int tEnd){

		for (int z=1; z<nz-1; z++){
			for (int i=tStart; i<tEnd; i++) {  
				for (int j=0;j< nj; j++) {  
					res[z][i][j]= -im[z-1][i][j]+im[z][i][j];			
				}	
			}
		}

		//bottom slice dirichlet
		for (int i=tStart; i<tEnd; i++) {  
			for (int j=0;j< nj; j++) {  	
				//dirichlet boundary left
				res[0][i][j]= im[0][i][j];
			}
		}

		//upper slice dirichlet
		for (int i=tStart; i<tEnd; i++) {  
			for (int j=0;j< nj; j++) {  
				//dirichlet boundary right
				res[nz-1][i][j]= -im[nz-2][i][j];			
			}
		}


	}

	public  void bgradydbc2D(double [] [] [] res, double [] [] [] im){
		bgradydbc2D(res,  im, 0, ni);
	}

	public  void bgradydbc2D(double [] [] [] res, double [] [] [] im, int tStart, int tEnd){

		for (int z=0; z<nz; z++){
			for (int j=1;j< nj-1; j++) {  
				for (int i=tStart; i<tEnd; i++) {  
					res[z][i][j]= -im[z][i][j-1]+im[z][i][j];			
				}	
			}
			//dirich boundary
			for (int i=tStart;i< tEnd; i++) {  
				res[z][i][nj-1]= 0;//??

				//dirichlet boundary top
				res[z][i][nj-1]= -im[z][i][nj-2];			
				//dirichlet boundary top
				res[z][i][0]= im[z][i][0];
			}
		}
	}


	public  void shrink2D(double [] [] [] res1,double [] [] [] res2, double [] [] [] u1,double [] [] [] u2, double  t){
		shrink2D(res1,res2,u1,u2,t,0,ni);	
	}


	public  void shrink2D(double [] [] [] res1,double [] [] [] res2, double [] [] [] u1,double [] [] [] u2, double  t,
			int iStart, int iEnd){
		double norm=0;

		for (int z=0; z<nz; z++){//todo : shrink3D
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<nj; j++) {  
					norm= Math.sqrt(Math.pow(u1[z][i][j],2) + Math.pow(u2[z][i][j], 2));
					if(norm>=t)
					{
						res1[z][i][j]=u1[z][i][j] - t*u1[z][i][j]/norm;
						res2[z][i][j]=u2[z][i][j] - t*u2[z][i][j]/norm;
					}
					else
					{
						res1[z][i][j]=0;
						res2[z][i][j]=0;
					}
				}	
			}
		}

	}
	public  void shrink3D(
			double [] [] [] res1,double [] [] [] res2,double [] [] [] res3,
			double [] [] [] u1,double [] [] [] u2,double [] [] [] u3, double  t){
		shrink3D(
				res1, res2, res3,
				u1, u2, u3, t,
				0, ni);
	}


	public  void shrink3D(
			double [] [] [] res1,double [] [] [] res2,double [] [] [] res3,
			double [] [] [] u1,double [] [] [] u2,double [] [] [] u3, double  t,
			int iStart, int iEnd){
		double norm=0;

		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0; j<nj; j++) {  
					norm= Math.sqrt(Math.pow(u1[z][i][j],2) + Math.pow(u2[z][i][j], 2)+ Math.pow(u3[z][i][j], 2));
					if(norm>=t)
					{
						res1[z][i][j]=u1[z][i][j] - t*u1[z][i][j]/norm;
						res2[z][i][j]=u2[z][i][j] - t*u2[z][i][j]/norm;
						res3[z][i][j]=u3[z][i][j] - t*u3[z][i][j]/norm;
					}
					else
					{
						res1[z][i][j]=0;
						res2[z][i][j]=0;
						res3[z][i][j]=0;
					}
				}	
			}
		}

	}


	public  double computeEnergy(
			double [] [] [] speedData, double [] [] [] mask, 
			double [] [] []  maskx, double [] [] []  masky,  
			double ldata, double lreg){


		double energyData=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyData+= speedData[z][i][j] * mask[z][i][j];
				}	
			}
		}

		fgradx2D(maskx, mask);
		fgrady2D(masky, mask);


		double energyPrior=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyPrior+= Math.sqrt(Math.pow(maskx[z][i][j], 2)+Math.pow(masky[z][i][j], 2) );
				}	
			}
		}
		double energy= ldata *energyData +lreg *energyPrior;

		return energy;
	}

	public  double computeEnergy3D(
			double [] [] [] speedData, double [] [] [] mask, 
			double [] [] []  maskx, double [] [] []  masky,double [] [] []  maskz,  
			double ldata, double lreg){


		double energyData=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyData+= speedData[z][i][j] * mask[z][i][j];
				}	
			}
		}

		fgradx2D(maskx, mask);
		fgrady2D(masky, mask);
		fgradz2D(maskz, mask);

		double energyPrior=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyPrior+= Math.sqrt(Math.pow(maskx[z][i][j], 2)+Math.pow(masky[z][i][j], 2)+Math.pow(maskz[z][i][j], 2) );
				}	
			}
		}
		double energy= ldata *energyData +lreg *energyPrior;

		return energy;
	}


	public  double computeEnergyPSF(
			double [] [] [] speedData, double [] [] [] mask, 
			double [] [] []  maskx, double [] [] []  masky,  
			double ldata, double lreg, Parameters p, double c0, double c1, double [][][] image){

		return computeEnergyPSF(
				speedData,  mask, 
				maskx,   masky,  
				ldata, lreg,  p, c0,  c1, image,
				0, ni, 0, nj);


	}


	public  double computeEnergyPSF(
			double [] [] [] speedData, double [] [] [] mask, 
			double [] [] []  maskx, double [] [] []  masky,  
			double ldata, double lreg, Parameters p, double c0, double c1, double [][][] image,
			int iStart, int iEnd, int jStart,int jEnd) {

		// mu = (betaMLE_in-betaMLE_out)*imfilter(mask,PSF,'symmetric')+betaMLE_out;

		//		Tools.disp_vals(mask[0], "mask");
		//		Tools.disp_valsc(p.PSF[0], "PSF");
		//		IJ.log("ni nj px py" + ni +" " + nj +" "+p.px +" "+p.py );

		//Tools.convolve2D(speedData[0], mask[0], ni, nj, p.PSF[0], p.px, p.py);
		Tools.convolve2Dseparable(speedData[0], mask[0], ni, nj, p.kernelx, p.kernely, p.px, p.py, maskx[0], iStart, iEnd);


		//for (int z=0; z<nz; z++){
		for (int i=iStart; i<iEnd	; i++) {  
			for (int j=0;j< nj; j++) {  	
				speedData[0][i][j]= (c1-c0)*speedData[0][i][j] + c0;
			}	
		}

		//}
		//nllMeanPoisson2(speedData, image, speedData, 1, ldata, iStart, iEnd);
		nllMean(speedData, image, speedData, 1, ldata, iStart, iEnd);
		double energyData=0;
		//for (int z=0; z<nz; z++){
		for (int i=iStart; i<iEnd; i++) {  
			for (int j=0;j< nj; j++) {  	
				energyData+= speedData[0][i][j];
			}	
		}
		//}


		fgradx2D(maskx, mask, jStart, jEnd);
		fgrady2D(masky, mask, iStart, iEnd);


		double energyPrior=0;
		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyPrior+= Math.sqrt(Math.pow(maskx[z][i][j], 2)+Math.pow(masky[z][i][j], 2) );
				}	
			}
		}


		double energy= ldata *energyData +lreg *energyPrior;
//		IJ.log("ldata" + ldata + "lreg" +lreg);
		//IJ.log("energy" + energy + " data" + energyData +" energyPrior"+energyPrior  );
		return energy;


	}
	
	
	public  double computeEnergyPSF_weighted(
			double [] [] [] speedData, double [] [] [] mask, 
			double [] [] []  maskx, double [] [] []  masky,
			double [][][] weights,  
			double ldata, double lreg, Parameters p, double c0, double c1, double [][][] image) {

		// mu = (betaMLE_in-betaMLE_out)*imfilter(mask,PSF,'symmetric')+betaMLE_out;

		//		Tools.disp_vals(mask[0], "mask");
		//		Tools.disp_valsc(p.PSF[0], "PSF");
		//		IJ.log("ni nj px py" + ni +" " + nj +" "+p.px +" "+p.py );

		//Tools.convolve2D(speedData[0], mask[0], ni, nj, p.PSF[0], p.px, p.py);
		Tools.convolve2Dseparable(speedData[0], mask[0], ni, nj, p.kernelx, p.kernely, p.px, p.py, maskx[0], 0, ni);


		//for (int z=0; z<nz; z++){
		for (int i=0; i<ni	; i++) {  
			for (int j=0;j< nj; j++) {  	
				speedData[0][i][j]= (c1-c0)*speedData[0][i][j] + c0;
			}	
		}

		//}
		//nllMeanPoisson2(speedData, image, speedData, 1, ldata, iStart, iEnd);
		nllMean(speedData, image, speedData, 1, ldata, 0, ni);
		double energyData=0;
		//for (int z=0; z<nz; z++){
		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) {
				energyData+= speedData[0][i][j]*weights[0][i][j];
			}	
		}
		//}


		fgradx2D(maskx, mask, 0, nj);
		fgrady2D(masky, mask, 0, ni);


		double energyPrior=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyPrior+= Math.sqrt(Math.pow(maskx[z][i][j], 2)+Math.pow(masky[z][i][j], 2) );
				}	
			}
		}


		double energy= ldata *energyData +lreg *energyPrior;
//		IJ.log("ldata" + ldata + "lreg" +lreg);
		//IJ.log("energy" + energy + " data" + energyData +" energyPrior"+energyPrior  );
		return energy;


	}


	public  double computeEnergyPSF(
			double [] [] [] speedData, double [] [] [] mask, 
			double [] [] []  maskx, double [] [] []  masky,  
			double ldata, double lreg, Parameters p, double c0, double c1, double [][][] image,
			int iStart, int iEnd, int jStart,int jEnd,CountDownLatch Sync8,CountDownLatch Sync9) throws InterruptedException {

		// mu = (betaMLE_in-betaMLE_out)*imfilter(mask,PSF,'symmetric')+betaMLE_out;

		//		Tools.disp_vals(mask[0], "mask");
		//		Tools.disp_valsc(p.PSF[0], "PSF");
		//		IJ.log("ni nj px py" + ni +" " + nj +" "+p.px +" "+p.py );

		//Tools.convolve2D(speedData[0], mask[0], ni, nj, p.PSF[0], p.px, p.py);
		Tools.convolve2Dseparable(speedData[0], mask[0], ni, nj, p.kernelx, p.kernely, p.px, p.py, maskx[0], iStart, iEnd);


		//for (int z=0; z<nz; z++){
		for (int i=iStart; i<iEnd	; i++) {  
			for (int j=0;j< nj; j++) {  	
				speedData[0][i][j]= (c1-c0)*speedData[0][i][j] + c0;
			}	
		}

		//}
		//nllMeanPoisson2(speedData, image, speedData, 1, ldata, iStart, iEnd);
		nllMean(speedData, image, speedData, 1, ldata, iStart, iEnd);
		double energyData=0;
		//for (int z=0; z<nz; z++){
		for (int i=iStart; i<iEnd; i++) {  
			for (int j=0;j< nj; j++) {  	
				energyData+= speedData[0][i][j];
			}	
		}
		//}

		Sync8.countDown();
		Sync8.await();
		fgradx2D(maskx, mask, jStart, jEnd);
		fgrady2D(masky, mask, iStart, iEnd);

		Sync9.countDown();
		Sync9.await();
		double energyPrior=0;
		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyPrior+= Math.sqrt(Math.pow(maskx[z][i][j], 2)+Math.pow(masky[z][i][j], 2) );
				}	
			}
		}


		double energy= ldata *energyData +lreg *energyPrior;

		return energy;


	}


	public  double computeEnergyPSF3D(
			double [] [] [] speedData, double [] [] [] mask, 
			double [] [] []  temp,double [] [] []  temp2, 
			double ldata, double lreg, Parameters p, double c0, double c1, double [][][] image,
			int iStart, int iEnd, int jStart,int jEnd,CountDownLatch Sync8,CountDownLatch Sync9, CountDownLatch Sync10) throws InterruptedException {

		// mu = (betaMLE_in-betaMLE_out)*imfilter(mask,PSF,'symmetric')+betaMLE_out;

		//		Tools.disp_vals(mask[0], "mask");
		//		Tools.disp_valsc(p.PSF[0], "PSF");
		//		IJ.log("ni nj px py" + ni +" " + nj +" "+p.px +" "+p.py );

		Tools.convolve3Dseparable(speedData, mask, 
				ni, nj, nz, 
				p.kernelx,p.kernely, p.kernelz,
				p.px, p.py, p.pz, temp, iStart, iEnd);




		//for (int z=0; z<nz; z++){
		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd	; i++) {  
				for (int j=0;j< nj; j++) {  	
					speedData[z][i][j]= (c1-c0)*speedData[z][i][j] + c0;
				}	
			}
		}

		//}
		//nllMeanPoisson2(speedData, image, speedData, 1, ldata, iStart, iEnd);
		nllMean(speedData, image, speedData, 1, ldata, iStart, iEnd);
		double energyData=0;
		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyData+= speedData[z][i][j];
				}	
			}
		}

		if (Sync8 != null)
		{
			Sync8.countDown();
			Sync8.await();
		}

		fgradx2D(temp, mask, jStart, jEnd);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=jStart;j< jEnd; j++) {  	
					temp2[z][i][j]= Math.pow(temp[z][i][j], 2);
				}	
			}
		}

		if (Sync10 != null)
		{
			Sync10.countDown();
			Sync10.await();
		}
		fgrady2D(temp, mask, iStart, iEnd);
		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp2[z][i][j]+= Math.pow(temp[z][i][j], 2);
				}	
			}
		}
		fgradz2D(temp, mask, iStart, iEnd);
		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp2[z][i][j]+= Math.pow(temp[z][i][j], 2);
				}	
			}
		}

		if (Sync9 != null)
		{
			Sync9.countDown();
			Sync9.await();
		}
		double energyPrior=0;
		for (int z=0; z<nz; z++){
			for (int i=iStart; i<iEnd; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyPrior+= Math.sqrt(temp2[z][i][j]);
				}	
			}
		}


		double energy= ldata *energyData +lreg *energyPrior;
		//IJ.log("energy" + energy + " data" + energyData +" energyPrior"+energyPrior  );
//		IJ.log("ldata" + ldata + "lreg" +lreg);
//		IJ.log("energy" + energy + " data" + energyData +" energyPrior"+energyPrior  );
		return energy;


	}



	public  double computeEnergyPSF3D(
			double [] [] [] speedData, double [] [] [] mask, 
			double [] [] []  temp,double [] [] []  temp2, 
			double ldata, double lreg, Parameters p, double c0, double c1, double [][][] image

			){

		// mu = (betaMLE_in-betaMLE_out)*imfilter(mask,PSF,'symmetric')+betaMLE_out;

		//		Tools.disp_vals(mask[0], "mask");
		//		Tools.disp_valsc(p.PSF[0], "PSF");
		//		IJ.log("ni nj px py" + ni +" " + nj +" "+p.px +" "+p.py );

		Tools.convolve3Dseparable(speedData, mask, 
				ni, nj, nz, 
				p.kernelx,p.kernely, p.kernelz,
				p.px, p.py, p.pz, temp);




		//for (int z=0; z<nz; z++){
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni	; i++) {  
				for (int j=0;j< nj; j++) {  	
					speedData[z][i][j]= (c1-c0)*speedData[z][i][j] + c0;
				}	
			}
		}

		//}
		//nllMeanPoisson2(speedData, image, speedData, 1, ldata);
		nllMean(speedData, image, speedData, 1, ldata);
		double energyData=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyData+= speedData[z][i][j];
				}	
			}
		}


		fgradx2D(temp, mask);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp2[z][i][j]= Math.pow(temp[z][i][j], 2);
				}	
			}
		}


		fgrady2D(temp, mask);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp2[z][i][j]+= Math.pow(temp[z][i][j], 2);
				}	
			}
		}
		fgradz2D(temp, mask);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp2[z][i][j]+= Math.pow(temp[z][i][j], 2);
				}	
			}
		}

		double energyPrior=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyPrior+= Math.sqrt(temp2[z][i][j]);
				}	
			}
		}


		double energy= ldata *energyData +lreg *energyPrior;
		//IJ.log("energy" + energy + " data" + energyData +" energyPrior"+energyPrior  );
//		IJ.log("ldata" + ldata + "lreg" +lreg);
//		IJ.log("energy" + energy + " data" + energyData +" energyPrior"+energyPrior  );
		return energy;


	}


	
	public  double computeEnergyPSF3D_weighted(
			double [] [] [] speedData, double [] [] [] mask, 
			double [] [] []  temp,double [] [] []  temp2, 
			double [][][] weights,
			double ldata, double lreg, Parameters p, double c0, double c1, double [][][] image

			){

		// mu = (betaMLE_in-betaMLE_out)*imfilter(mask,PSF,'symmetric')+betaMLE_out;

		//		Tools.disp_vals(mask[0], "mask");
		//		Tools.disp_valsc(p.PSF[0], "PSF");
		//		IJ.log("ni nj px py" + ni +" " + nj +" "+p.px +" "+p.py );

		Tools.convolve3Dseparable(speedData, mask, 
				ni, nj, nz, 
				p.kernelx,p.kernely, p.kernelz,
				p.px, p.py, p.pz, temp);




		//for (int z=0; z<nz; z++){
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni	; i++) {  
				for (int j=0;j< nj; j++) {  	
					speedData[z][i][j]= (c1-c0)*speedData[z][i][j] + c0;
				}	
			}
		}

		//}
		//nllMeanPoisson2(speedData, image, speedData, 1, ldata);
		nllMean(speedData, image, speedData, 1, ldata);
		double energyData=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyData+= speedData[z][i][j] *weights[z][i][j];
				}	
			}
		}


		fgradx2D(temp, mask);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp2[z][i][j]= Math.pow(temp[z][i][j], 2);
				}	
			}
		}


		fgrady2D(temp, mask);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp2[z][i][j]+= Math.pow(temp[z][i][j], 2);
				}	
			}
		}
		fgradz2D(temp, mask);
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp2[z][i][j]+= Math.pow(temp[z][i][j], 2);
				}	
			}
		}

		double energyPrior=0;
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					energyPrior+= Math.sqrt(temp2[z][i][j]);
				}	
			}
		}


		double energy= ldata *energyData +lreg *energyPrior;
		//IJ.log("energy" + energy + " data" + energyData +" energyPrior"+energyPrior  );
//		IJ.log("ldata" + ldata + "lreg" +lreg);
//		IJ.log("energy" + energy + " data" + energyData +" energyPrior"+energyPrior  );
		return energy;


	}

	public  void mydivergence(double [] [] [] res , double [] [] [] m1, double [] [] [] m2){
		mydivergence( res ,  m1, m2, 0, ni, 0,nj);
	}


	public  void mydivergence(double [] [] [] res , double [] [] [] m1, double [] [] [] m2, int iStart, int iEnd, int jStart, int jEnd){
		//Tools.bgradxdbc2D(res, m1);
		//Tools.bgradydbc2D(m1, m2);
		//Tools.addtab(res, res, m1);
		bgradxdbc2D(res, m1, jStart, jEnd);
		bgradydbc2D(m1, m2, iStart, iEnd);
		addtab(res, res, m1, iStart, iEnd);

	}

	public  void mydivergence(double [] [] [] res , double [] [] [] m1, double [] [] [] m2,double [] [] [] temp, CountDownLatch Sync2, 
			int iStart, int iEnd, int jStart, int jEnd) throws InterruptedException {
		bgradxdbc2D(res, m1, jStart, jEnd);
		bgradydbc2D(temp, m2, iStart, iEnd);
		Sync2.countDown();
		Sync2.await();
		addtab(res, res, temp, iStart, iEnd);
	}



	public  void mydivergence3D(double [] [] [] res , double [] [] [] m1, double [] [] [] m2,  double [] [] [] m3){
		mydivergence3D(res,m1,m2,m3, 0, ni, 0,nj);
	}


	public  void mydivergence3D(double [] [] [] res , double [] [] [] m1, double [] [] [] m2,  double [] [] [] m3,
			double [] [] [] temp, CountDownLatch Sync2,
			int iStart, int iEnd, int jStart, int jEnd) throws InterruptedException {
		bgradxdbc2D(res, m1, jStart, jEnd);
		bgradydbc2D(temp, m2, iStart, iEnd);
		if (Sync2 != null)
		{
			Sync2.countDown();
			Sync2.await();
		}
		addtab(res, res, temp, iStart, iEnd);
		bgradzdbc2D(m1, m3, iStart, iEnd);
		addtab(res, res, m1, iStart, iEnd);
	}

	public  void mydivergence3D(double [] [] [] res , double [] [] [] m1, double [] [] [] m2,  double [] [] [] m3,
			int iStart, int iEnd, int jStart, int jEnd){
		bgradxdbc2D(res, m1, jStart, jEnd);
		bgradydbc2D(m1, m2, iStart, iEnd);
		addtab(res, res, m1, iStart, iEnd);
		bgradzdbc2D(m1, m3, iStart, iEnd);
		addtab(res, res, m1, iStart, iEnd);
	}

	/*
	public static void compute_l2norm(double [] [] [] [] maskprevious, double [] [] [] [] maskcurrent){

	}
	 */

	public  void disp_array_new(double [] [] array, String s){
		int ni= array.length;
		int nj= array[1].length;
		float [] [] temp= new float [ni][nj];



		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) {  	
				temp[i][j]= (float) array[i][j];
			}	
		}


		ImagePlus img=new ImagePlus();
		ImageProcessor imp= new FloatProcessor(temp);
		img.setProcessor(s,imp);
		img.show();

	}
	
	static public  void disp_array3D_new(float [] [] [] array, String s){
		int ni= array[0].length;
		int nj= array[0][0].length;
		float [] [] temp= new float [ni][nj];

		ImageStack ss = new ImageStack(ni,nj);
		
		for (int k = 0 ; k < array.length ; k++)
		{
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp[i][j]= (float) array[k][i][j];
				}
			}
			ImageProcessor imp= new FloatProcessor(temp);
			ss.addSlice("slice " + k, imp);
		}
		
		ImagePlus img=new ImagePlus("temp",ss);
		img.show();

	}

	public  void disp_array(double [] [] array, String s){
		int ni= array.length;
		int nj= array[1].length;
		float [] [] temp= new float [ni][nj];



		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) {  	
				temp[i][j]= (float) array[i][j];
			}	
		}

		ImageProcessor imp= new FloatProcessor(temp);
		imgd.setProcessor(s,imp);
		if(disp){ imgd.show(); disp=false;} 

	}



	public  void disp_vals(double [] [] array, String s){

		String line = "";  
		IJ.log("array " + s + " from i0 to i2 and j0 to j2");
		for (int j=0;j< 3; j++) {  	
			line = "";
			for (int i=0; i<3; i++){  
				line = line + String.format("%7.2e", array[i][j]) +" ";			

			}
			IJ.log(line);
		}		

		line = "";  
		IJ.log("array " + s + " from i200 to i202 and j170 to j172");
		for (int j=170;j< 173; j++) {  	
			line = "";
			for (int i=200; i<203; i++) {  
				line = line + String.format("%7.2e", array[i][j]) +" ";				
			}
			IJ.log(line);
		}	

		line = "";  
		IJ.log("array " + s + " from i509 to i511 and j509 to j511");
		for (int j=509;j< 512; j++) {  	
			line = "";
			for (int i=509; i<512; i++) {  
				line = line + String.format("%7.2e", array[i][j]) +" ";				
			}
			IJ.log(line);
		}	

	}


	public  void disp_valsb(double [] [] array, String s){

		String line = "";  
		IJ.log("array " + s + " from i0 to i2 and j0 to j2");
		for (int j=0;j< 3; j++) {  	
			line = "";
			for (int i=0; i<3; i++) {  
				line = line + round(array[i][j],3) +" ";				
			}
			IJ.log(line);
		}		
	}

	public  void disp_valsc(double [] [] array, String s){

		String line = "";  
		IJ.log("array " + s + " from i0 to i6 and j0 to j6");
		for (int j=0;j< 7; j++) {  	
			line = "";
			for (int i=0; i<7; i++) {  
				line = line + round(array[i][j],5) +" ";				
			}
			IJ.log(line);
		}		
	}

	public  void max_mask(int [] [] [] res, double [] [] [] [] mask){
		double max;
		int index_max;
		for (int z=0; z<nz; z++) {  
			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  
					max=0;index_max=0;
					for(int l=0; l< nlevels;l++){
						if( (mask[l][z][i][j]) >= max){
							max=mask[l][z][i][j];	
							index_max=l;
						}
					}
					res[z][i][j]=index_max;
				}	
			}
		}

	}


	public static  double round(double y, int z){
		//Special tip to round numbers to 10^-2
		y*=Math.pow(10,z);
		y=(int) y;
		y/=Math.pow(10,z);
		return y;
	}



}
