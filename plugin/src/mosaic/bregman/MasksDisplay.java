package mosaic.bregman;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import mosaic.core.utils.MosaicUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
//import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


//TODO : display for 3D
public class MasksDisplay {
	private ImageStack imgcolocstack;
	private ImageStack imgcolocastack;
	//private ImageStack imgcolocbstack;

	private ImagePlus imgcoloc;
	private ImagePlus imgcoloca;
	private ImagePlus imgcolocb;
	private int [] [] colors;
	private int []  color;
	private ColorProcessor cp;
	private ImagePlus img;
	private int ni,nj,nz,nlevels;
	public boolean firstdisp=true;
	public boolean firstdispa=true;
	public boolean firstdispb=true;
	private Parameters p;
	private ImagePlus img3d;

	private ImageStack ims3d;
	//static ImagePlus imgda=new ImagePlus();
	//static ImagePlus imgdb=new ImagePlus();
	ImagePlus imgda, imgdb;

	//static boolean dispa=true;
	//static boolean dispb=true;

	public MasksDisplay(int ni, int nj,int nz,  int nlevels, double [] cl, Parameters params){
		this.imgda=new ImagePlus();
		this.imgdb=new ImagePlus();
		this.ni=ni;this.nj=nj;this.nlevels=nlevels;this.nz=nz;
		this.p=params;
		this.colors= new int [this.nlevels] [3];
		this.color= new int [3];
		this.cp= new ColorProcessor(ni,nj);
		this.img=new ImagePlus();

		this.imgcoloc=new ImagePlus();
		this.imgcoloca=new ImagePlus();
		this.imgcolocb=new ImagePlus();


		//		this.img3d=new ImagePlus("3d mask", ims3d);
		//heatmap R=x, G=sqrt(x), B=x**2 on kmeans found intensities
		for(int l=0; l< this.nlevels;l++){
			colors[l][1]= (int) Math.min(255, 255*Math.sqrt(cl[l])) ; //Green
			colors[l][0]= (int) Math.min(255, 255*cl[l]) ; //Red
			colors[l][2]= (int) Math.min(255, 255*Math.pow(cl[l],2)) ; //Blue
		}


		if (!p.displowlevels){
			colors[0][1]=0; colors[0][0]= 0;colors[0][2]= 0;
			colors[1][1]=0; colors[1][0]= 0;colors[1][2]= 0;
		}

	}


	public void display(int [] [] [] maxmask, String s){

		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) {  
				cp.putPixel(i, j, colors[maxmask[0][i][j]]);
			}
		}	
		img.setProcessor(s,cp);
		if(firstdisp){img.show(); 
		firstdisp=false;} 
	}

	public  void display2regionswcolor(double [] [] array, String s){


		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) {  
				color[1]= (int) Math.min(255, 255*Math.sqrt(array[i][j])) ; //Green
				color[0]= (int) Math.min(255, 255*array[i][j]) ; //Red
				color[2]= (int) Math.min(255, 255*Math.pow(array[i][j],2)) ; //Blue
				cp.putPixel(i, j, color);
			}
		}


		img.setProcessor(s,cp);
		if(firstdisp){img.show(); 
		firstdisp=false;}




	}


	public void display2regions(double [] [] array, String s, int channel){

		float [] [] temp= new float [ni][nj];


		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) {  	
				temp[i][j]= (float) array[i][j];
			}	
		}

		ImageProcessor imp= new FloatProcessor(temp);
		if(channel==0){
			imgda.setProcessor(s + " X" ,imp);
			if(firstdispa){ imgda.show(); firstdispa=false;}
			imgda.changes=false;
		}
		else{
			imgdb.setProcessor(s + " Y" ,imp);
			if(firstdispb){ imgdb.show(); firstdispb=false;}
			imgdb.changes=false;
		}
	}

	public  ImagePlus display2regionsnew(float [] [] array, String s, int channel){

		float [] [] temp= new float [ni][nj];
		ImagePlus imgtemp=new ImagePlus();


		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) {  	
				temp[i][j]= (float) array[i][j];
			}	
		}

		ImageProcessor imp= new FloatProcessor(temp);
		if(channel==0)
			imgtemp.setProcessor(s + "X",imp);
		else
			imgtemp.setProcessor(s + "Y",imp);
		imgtemp.show(); 
		return imgtemp;
	}

	public  ImagePlus display2regionsnewd(double [] [] array, String s, int channel){

		float [] [] temp= new float [ni][nj];
		ImagePlus imgtemp=new ImagePlus();


		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) {  	
				temp[i][j]= (float) array[i][j];
			}	
		}

		ImageProcessor imp= new FloatProcessor(temp);
		if(channel==0)
			imgtemp.setProcessor(s + "X",imp);
		else
			imgtemp.setProcessor(s + "Y",imp);
		imgtemp.show(); 
		return imgtemp;
	}

	public  ImagePlus display2regionsnewboolean(boolean [] [] array, String s, int channel){

		float [] [] temp= new float [ni][nj];
		ImagePlus imgtemp=new ImagePlus();


		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) { 
				if(array[i][j])
					temp[i][j]=1; 
				else
					temp[i][j]=0;
			}	
		}

		ImageProcessor imp= new FloatProcessor(temp);
		if(channel==0)
			imgtemp.setProcessor(s + "X",imp);
		else
			imgtemp.setProcessor(s + "Y",imp);
		imgtemp.show(); 
		return imgtemp;
	}


	public  ImagePlus display2regionsnew(double [] [] array, String s, int channel){

		float [] [] temp= new float [ni][nj];
		ImagePlus imgtemp=new ImagePlus();


		for (int i=0; i<ni; i++) {  
			for (int j=0;j< nj; j++) {  	
				temp[i][j]= (float) array[i][j];
			}	
		}

		ImageProcessor imp= new FloatProcessor(temp);
		if(channel==0)
			imgtemp.setProcessor(s + "X",imp);
		else
			imgtemp.setProcessor(s + "Y",imp);
		imgtemp.show(); 
		imgtemp.changes=false;
		return imgtemp;
	}

	public  ImagePlus display2regions3Dnew(double [] [] [] array, String s, int channel){
		ImageStack img3temp=new ImageStack(ni,nj);

		ImagePlus imgtemp=new ImagePlus();

		for (int z=0; z<nz; z++) {  
			float [] [] temp= new float [ni][nj];


			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp[i][j]= (float) array[z][i][j];
				}	
			}
			ImageProcessor imp= new FloatProcessor(temp);
			img3temp.addSlice("", imp);
		}

		if(channel==0)
			imgtemp.setStack(s + "X",img3temp);
		else
			imgtemp.setStack(s + "Y",img3temp);
		
		imgtemp.show(); 
		imgtemp.changes=false;
		return imgtemp;
	}


	public void display2regions3D(double [] [] [] array, String s, int channel){

		this.ims3d=new ImageStack(ni,nj);
		for (int z=0; z<nz; z++) {  
			//IJ.log("add slice number :" + z);
			//float [] [] temp= new float [ni][nj];
			byte[] temp = new byte[ni*nj];

			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp[j * p.ni + i]= (byte) ( (int)(255*array[z][i][j]));//(float) array[z][i][j];
					//ims3d.setVoxel(i, j, z, array[z][i][j]);
				}	
			}
			ImageProcessor bp= new ByteProcessor(ni,nj);
			bp.setPixels(temp);
			this.ims3d.addSlice("", bp);
		}


		if(channel==0){
			imgda.setStack(s + " X", ims3d);
			imgda.resetDisplayRange();
			if(firstdispa){imgda.show(); firstdispa=false;}
		}
		else{
			imgdb.setStack(s + " Y", ims3d);
			imgdb.resetDisplayRange();
			if(firstdispb){ imgdb.show(); firstdispb=false;}
		}


	}



	public ImagePlus display2regions3Dnew(byte [] [] [] array, String s, int channel){

		ImageStack ims3da = new ImageStack(ni,nj);
		ImagePlus imgd = new ImagePlus();

		//this.ims3d=new ImageStack(ni,nj);
		for (int z=0; z<nz; z++) {  
			//IJ.log("add slice number :" + z);
			//float [] [] temp= new float [ni][nj];
			byte[] temp = new byte[ni*nj];

			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp[j * p.ni + i]= (byte) ( (int)(array[z][i][j]));//(float) array[z][i][j];
					//ims3d.setVoxel(i, j, z, array[z][i][j]);
				}	
			}
			ImageProcessor bp= new ByteProcessor(ni,nj);
			bp.setPixels(temp);
			ims3da.addSlice("", bp);
		}

		if(channel==0)
			imgd.setStack(s + " X", ims3da);
		else
			imgd.setStack(s + " Y", ims3da);
		imgd.resetDisplayRange();
		imgd.show();


		return imgd;	
	}



	public ImagePlus display2regions3Dnew(float [] [] [] array, String s, int channel){

		ImageStack ims3da = new ImageStack(ni,nj);
		ImagePlus imgd = new ImagePlus();

		//this.ims3d=new ImageStack(ni,nj);
		for (int z=0; z<nz; z++) {  
			//IJ.log("add slice number :" + z);
			//float [] [] temp= new float [ni][nj];
			byte[] temp = new byte[ni*nj];

			for (int j=0;j< nj; j++) {  	
				for (int i=0; i<ni; i++) {  
					temp[j * p.ni + i]= (byte) ( (int)(array[z][i][j]));//(float) array[z][i][j];
					//ims3d.setVoxel(i, j, z, array[z][i][j]);
				}	
			}
			ImageProcessor bp= new ByteProcessor(ni,nj);
			bp.setPixels(temp);
			ims3da.addSlice("", bp);
		}

		if(channel==0)
			imgd.setStack(s + " X", ims3da);
		else
			imgd.setStack(s + " Y", ims3da);
		imgd.resetDisplayRange();
		imgd.show();


		return imgd;	
	}

	public  void display2regions3Dscaled(double [] [] [] array, String s){



		this.ims3d=new ImageStack(ni,nj);
		for (int z=0; z<nz; z++) {  
			//IJ.log("add slice number :" + z);
			float [] [] temp= new float [ni][nj];

			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  	
					temp[i][j]= (float) array[z][i][j];
					//ims3d.setVoxel(i, j, z, array[z][i][j]);
				}	
			}
			ImageProcessor imp= new FloatProcessor(temp);
			this.ims3d.addSlice(s, imp);
			this.ims3d.addSlice(s, imp);
			this.ims3d.addSlice(s, imp);
			this.ims3d.addSlice(s, imp);
			this.ims3d.addSlice(s, imp);

		}
		this.img3d=new ImagePlus("3d mask scaled", ims3d);
		img3d.show();



	}



	public  void display2regions3Dscaledcolor(double [] [] [] array, String s){


		this.ims3d=new ImageStack(ni,nj);
		for (int z=0; z<nz; z++) {  
			//IJ.log("add slice number :" + z);
			//float [] [] temp= new float [ni][nj];
			ColorProcessor cp2= new ColorProcessor(ni,nj);


			for (int i=0; i<ni; i++) {  
				for (int j=0;j< nj; j++) {  
					color[1]= (int) Math.min(255, 255*Math.sqrt(array[z][i][j])) ; //Green
					color[0]= (int) Math.min(255, 255*array[z][i][j]) ; //Red
					color[2]= (int) Math.min(255, 255*Math.pow(array[z][i][j],2)) ; //Blue
					cp2.putPixel(i, j, color);
				}
			}


			this.ims3d.addSlice(s, cp2);
			this.ims3d.addSlice(s, cp2);
			this.ims3d.addSlice(s, cp2);
			this.ims3d.addSlice(s, cp2);
			this.ims3d.addSlice(s, cp2);

		}
		this.img3d=new ImagePlus("3d mask scaled color", ims3d);
		img3d.show();



	}




	public void displaycolocold(ArrayList<Region> regionslistA, ArrayList<Region> regionslistB, Vector<ImagePlus> ip)
	{
		this.imgcolocstack=new ImageStack(ni,nj);
		for (int z=0; z<nz; z++) {  

			ColorProcessor cpcoloc= new ColorProcessor(ni,nj);

			//set all to zero
			for (int i=0;i<ni;i++) {  
				for (int j=0;j< nj;j++) {  
					color[1]= 0; //Green
					color[0]= 0;
					color[2]= 0;
					cpcoloc.putPixel(i, j, color);
				}
			}

			//set green  pixels 

			for (Iterator<Region> it = regionslistA.iterator(); it.hasNext();){
				Region r = it.next();

				for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
					Pix p = it2.next();
					cpcoloc.getPixel(p.px, p.py, color);
					color[1]=255;//green
					cpcoloc.putPixel(p.px, p.py, color);
				}

			}

			//set red pixels 
			for (Iterator<Region> it = regionslistB.iterator(); it.hasNext();){
				Region r = it.next();

				for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
					Pix p = it2.next();
					cpcoloc.getPixel(p.px, p.py, color);
					color[0]=255;//red
					cpcoloc.putPixel(p.px, p.py, color);
				}

			}

			this.imgcolocstack.addSlice("Colocalization", cpcoloc);

		}

		this.imgcoloc=new ImagePlus("Colocalization", imgcolocstack);
		imgcoloc.show();

		ip.add(imgcoloc);
	}

	/**
	 * 
	 * Display the colocalization image
	 * 
	 * @param savepath path + filename "_coloc.zip" is appended to the name, extension is removed
	 * @param regionslistA Regions list A
	 * @param regionslistB Regions list B
	 * 
	 */

	public void displaycoloc(String savepath, ArrayList<Region> regionslistA,ArrayList<Region> regionslistB, Vector<ImagePlus> ip){

		
		
		byte [] imagecolor = new byte [nz*ni*nj*3];

		//set all to zero
		for (int z=0; z<nz; z++) {  
			for (int i=0;i<ni;i++) { 
				int t=z*ni*nj*3+i*nj*3;
				for (int j=0;j< nj;j++){  
					imagecolor[t+j*3+0]=0;//Red channel
					imagecolor[t+j*3+1]=0;//Green channel
					imagecolor[t+j*3+2]=0;//Blue channel
				}
			}
		}

		//set green  pixels 
		imagecolor[1]=(byte) 255;
		for (Iterator<Region> it = regionslistA.iterator(); it.hasNext();){
			Region r = it.next();

			for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
				Pix p = it2.next();
				int t=p.pz*ni*nj*3+p.px*nj*3;
				imagecolor[t+p.py*3+1]=(byte) 255;
				//green
			}	
		}


		//set red pixels 
		for (Iterator<Region> it = regionslistB.iterator(); it.hasNext();){
			Region r = it.next();

			for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
				Pix p = it2.next();
				int t=p.pz*ni*nj*3+p.px*nj*3;
				imagecolor[t+p.py*3+0]=(byte) 255;
				//red
				//cpcoloc.putPixel(p.px, p.py, color);
			}
		}


				
			
		int [] tabt= new int [3];
		
		this.imgcolocastack=new ImageStack(ni,nj);
		for (int z=0; z<nz; z++) {  
			ColorProcessor cpcoloc= new ColorProcessor(ni,nj);
			for (int i=0;i<ni;i++) {  
				int t=z*ni*nj*3+i*nj*3;
				for (int j=0;j< nj;j++){
					tabt[0]=imagecolor[t+j*3 + 0] & 0xFF;
					tabt[1]=imagecolor[t+j*3 + 1] & 0xFF;
					tabt[2]=imagecolor[t+j*3 + 2] & 0xFF;	
					//if(i==0 && j==0){IJ.log("tabt0 :" + tabt[0] + "t1 :" + tabt[1] + "t2:" + tabt[2]);}
					cpcoloc.putPixel(i, j, tabt);
				}
			}
			this.imgcolocastack.addSlice("Colocalization", cpcoloc);

		}
		this.imgcoloc.setStack("Colocalization", imgcolocastack);
		//this.imgcoloca=new ImagePlus("Colocalization", imgcolocastack);

		ip.add(this.imgcoloc);
		
		if(Analysis.p.dispwindows){
			this.imgcoloc.show();
			this.imgcoloc.getWindow().setLocation(100, 120);
		}

		if (Analysis.p.save_images){
			IJ.run(this.imgcoloc,"RGB Color", "");
			
			savepath = MosaicUtils.removeExtension(savepath);
			savepath = savepath + "_coloc" +".zip";
			IJ.saveAs(this.imgcoloc, "ZIP", savepath);

			//			FileSaver fs= new FileSaver(this.imgcoloc);
			//			String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_coloc" +".tif";
			//			if (Analysis.p.nz >1) fs.saveAsTiffStack(savepath);
			//			else fs.saveAsTiff(savepath);	
		}
	}

	

	public void displaycolocpositiveA(ArrayList<Region> regionslistA, Vector<ImagePlus> ip){

		int [][][][] imagecolor = new int [nz][ni][nj][3];

		//set all to zero
		for (int z=0; z<nz; z++) {  
			for (int i=0;i<ni;i++) {  
				for (int j=0;j< nj;j++){  
					imagecolor[z][i][j][0]=0;
					imagecolor[z][i][j][1]=0;//Green channel
					imagecolor[z][i][j][2]=0;
				}
			}
		}

		//set green  pixels 

		for (Iterator<Region> it = regionslistA.iterator(); it.hasNext();){
			Region r = it.next();

			for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
				Pix p = it2.next();
				//cpcoloc.getPixel(p.px, p.py, color);
				//imagecolor[p.pz][p.px][p.py]=color;

				if(r.colocpositive) {
					imagecolor[p.pz][p.px][p.py][0]=255;
					imagecolor[p.pz][p.px][p.py][1]=255;
					imagecolor[p.pz][p.px][p.py][2]=255;
				}//white
				else {
					imagecolor[p.pz][p.px][p.py][0]=0;
					imagecolor[p.pz][p.px][p.py][1]=255;
					imagecolor[p.pz][p.px][p.py][2]=0;
					//green
				}
				//imagecolor[p.pz][p.px][p.py]=color;
				//cpcoloc.putPixel(p.px, p.py, color);
			}	
		}

		this.imgcolocastack=new ImageStack(ni,nj);
		for (int z=0; z<nz; z++) {  

			ColorProcessor cpcoloc= new ColorProcessor(ni,nj);
			for (int i=0;i<ni;i++) {  
				for (int j=0;j< nj;j++){  
					cpcoloc.putPixel(i, j, imagecolor[z][i][j]);
				}
			}
			this.imgcolocastack.addSlice("Positive X vesicles", cpcoloc);

		}
		this.imgcoloca.setStack("Positive X vesicles", imgcolocastack);
		//this.imgcoloca=new ImagePlus("Positive X vesicles", imgcolocastack);
		imgcoloca.show();
		ip.add(imgcoloca);
	}



	public void displaycolocpositiveB(ArrayList<Region> regionslistA, Vector<ImagePlus> ip){

		int [][][][] imagecolor = new int [nz][ni][nj][3];

		//set all to zero
		for (int z=0; z<nz; z++) {  
			for (int i=0;i<ni;i++) {  
				for (int j=0;j< nj;j++){  
					imagecolor[z][i][j][0]=0;
					imagecolor[z][i][j][1]=0;//Green channel
					imagecolor[z][i][j][2]=0;
				}
			}
		}

		//set green  pixels 

		for (Iterator<Region> it = regionslistA.iterator(); it.hasNext();){
			Region r = it.next();

			for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
				Pix p = it2.next();
				//cpcoloc.getPixel(p.px, p.py, color);

				if(r.colocpositive) {
					imagecolor[p.pz][p.px][p.py][0]=255;
					imagecolor[p.pz][p.px][p.py][1]=255;
					imagecolor[p.pz][p.px][p.py][2]=255;
				}//white
				else {
					imagecolor[p.pz][p.px][p.py][0]=255;
					imagecolor[p.pz][p.px][p.py][1]=0;
					imagecolor[p.pz][p.px][p.py][2]=0;
					//red
				}
			}	
		}

		this.imgcolocastack=new ImageStack(ni,nj);
		for (int z=0; z<nz; z++) {  

			ColorProcessor cpcoloc= new ColorProcessor(ni,nj);
			for (int i=0;i<ni;i++) {  
				for (int j=0;j< nj;j++){  
					cpcoloc.putPixel(i, j, imagecolor[z][i][j]);
				}
			}
			this.imgcolocastack.addSlice("Positive Y vesicles", cpcoloc);

		}
		this.imgcolocb.setStack("Positive Y vesicles", imgcolocastack);
		//this.imgcoloca=new ImagePlus("Positive Y vesicles", imgcolocastack);
		imgcolocb.show();
		ip.add(imgcolocb);

	}

	public void displayint(int [][][] regions, int width,int height,int depth){
		ImageStack regstackx;
		ImagePlus regsresultx =new ImagePlus();




		regstackx=new ImageStack(width,height);

		//int min = 0;
		//int max = Math.max(regionslist_refined.size(), 255 );
		for (int z=0; z<depth; z++){
			short[] mask_short = new short[width*height];
			for (int i=0; i<width; i++) {
				for (int j=0; j<height; j++) {  
					mask_short[j * width + i]= (short) regions[z][i][j];
				}
			}
			ShortProcessor sp = new ShortProcessor(width, height);
			sp.setPixels(mask_short);
			//sp.setMinAndMax( min, max );
			regstackx.addSlice("", sp);
		}




		regsresultx.setStack("Mask",regstackx);


		//IJ.log("displaying");
		regsresultx.show(); 
		regsresultx.setActivated();






	}

	void closeAll()
	{
		if (imgcoloc != null)
			imgcoloc.close();
	}
	
	//	public void displaycolocpositiveBold(ArrayList<Region> regionslistA){
	//
	//
	//		this.imgcolocbstack=new ImageStack(ni,nj);
	//		for (int z=0; z<nz; z++) {  
	//
	//			ColorProcessor cpcoloc= new ColorProcessor(ni,nj);
	//
	//			//set all to zero
	//			for (int i=0;i<ni;i++) {  
	//				for (int j=0;j< nj;j++){  
	//					color[1]= 0; //Green
	//					color[0]= 0;
	//					color[2]= 0;
	//					cpcoloc.putPixel(i, j, color);
	//				}
	//			}
	//
	//			//set green  pixels 
	//
	//			for (Iterator<Region> it = regionslistA.iterator(); it.hasNext();){
	//				Region r = it.next();
	//
	//				for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
	//					Pix p = it2.next();
	//					cpcoloc.getPixel(p.px, p.py, color);
	//
	//					if(r.colocpositive) {color[0]=255;color[1]=255;color[2]=255;}//white
	//					else color[0]=255;//red
	//					cpcoloc.putPixel(p.px, p.py, color);
	//				}
	//
	//			}
	//
	//
	//			this.imgcolocbstack.addSlice("Positive Y vesicles", cpcoloc);
	//
	//		}
	//
	//		this.imgcolocb=new ImagePlus("Positive Y vesicles", imgcolocbstack);
	//		imgcolocb.show();
	//
	//
	//	}



}
