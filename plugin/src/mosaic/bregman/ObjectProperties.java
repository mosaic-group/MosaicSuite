package mosaic.bregman;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;

import java.util.ArrayList;
import java.util.Iterator;

import mosaic.bregman.FindConnectedRegions.Region;

public class ObjectProperties implements Runnable {
	double intmin, intmax;
	double [][][] image;
	public Region region;
	double [][][][] temp1;
	double [][][][] temp2;
	double [][][][] temp3;
	double cout;
	double cin;
	double [][][] patch;
	short [][][] regions;
	int margin;
	int zmargin;
	int sx, sy, sz;//size for object
	int nx, ny, nz;//size of full oversampled work zone
	Parameters p;
	int cx,cy,cz;// coord of patch in full work zone (offset)
	int osxy, osz;
	public double [][][][] mask;//nregions nslices ni nj
	byte [] imagecolor_c1;

	public ObjectProperties(double [][][] im, Region reg, int nx, int ny, int nz, Parameters p1, int osxy, int osz, byte [] color_c1,
			short [][][]regs){
		this.regions=regs;
		this.p=new Parameters(p1);
		this.image=im;
		this.region=reg;
		this.nx=nx;
		this.ny=ny;
		this.nz=nz;
		this.imagecolor_c1=color_c1;
		margin=5;
		zmargin=2;

		this.osxy=osxy;
		this.osz=osz;



		set_patch_geom(region);

		temp1= new double [1] [sz] [sx] [sy];
		temp2= new double [1] [sz] [sx] [sy];
		temp3= new double [1] [sz] [sx] [sy];
		//		IJ.log("ni" +sx+"nj" +sy+"nz" +sz);
		//		
		//
		//		
		//		IJ.log("cx" +cx+"cy" +cy+"cz" +cz);
		//		IJ.log("sx" +sx+"sy" +sy+"sz" +sz);
		//		IJ.log("osxy" +osxy+"osz" +osz);
		//set size
		p.ni=sx;p.nj=sy;p.nz=sz;
		//set psf
		if ( p.nz>1){
			Tools.gaussian3Dbis(p.PSF, p.kernelx, p.kernely, p.kernelz, 7, p.sigma_gaussian*osxy, p.zcorrec*osz);//todo verif zcorrec
		}
		else
		{
			Tools.gaussian2D(p.PSF[0], p.kernelx, p.kernely, 7, p.sigma_gaussian*osxy);
		}
		// do PSF

	}

	public void run(){

		//		if(region.value==21){
		//			IJ.log("la");
		//		}

		fill_patch(image);
		normalize();
		fill_mask(region);
		estimate_int(mask[0]);
		region.intensity=cin*(intmax-intmin) +intmin;
		//IJ.log("region " + region.value + "size" +region.pixels.size()  + "osxy" + osxy);
		if(p.nz==1){
			region.rsize= round((region.pixels.size())/((float)osxy*osxy),3);
			//IJ.log("region " + region.value + "size" +region.rsize);	
		}
		else
			region.rsize= round((region.pixels.size())/((float) osxy*osxy*osxy),3);

		if(p.dispint)fill_ints();

		if(p.save_images){
			setIntensitiesandCenters(region,image);
			setPerimeter(region,regions);
			if(Analysis.p.nz==1){
				setlength(region, regions);
			}
			else
				setlength3D(region, regions);
		}
		//IJ.log("int :" +region.intensity +"r"+ region.value );
		//IJ.log(" " );

		//		MasksDisplay md= new MasksDisplay(sx,sy,sz,2,p.cl,p);
		//		if(region.value==10){md.display2regions3Dnew(patch, "iPatch "+region.value, 1);
		//		md.display2regions3Dnew(mask[0], "iMask "+region.value, 1);
		//		}
	}



	private void fill_ints(){
		int c1= (int) Math.min(255, 255*Math.sqrt(region.intensity)) ; //Green
		int c0= (int) Math.min(255, 255*region.intensity) ; //Red
		int c2= (int) Math.min(255, 255*Math.pow(region.intensity,2)) ; //Blue


		for (Iterator<Pix> it2 = region.pixels.iterator(); it2.hasNext();) {
			Pix p = it2.next();
			//set correct color
			int t=p.pz*nx*ny*3+p.px*ny*3;
			imagecolor_c1[t+p.py*3+0]=(byte) c0;
			imagecolor_c1[t+p.py*3+1]=(byte) c1;
			imagecolor_c1[t+p.py*3+2]=(byte) c2;
			//green
		}	

	}

	private void fill_patch(double [][][] image){
		this.patch = new double [sz][sx][sy];
		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){
				for (int j=0;j< sy; j++){  
					this.patch[z][i][j] = image[(cz+z)/osz][(cx+i)/osxy][(cy+j)/osxy];	
				}
			}
		}


	}

	private void estimate_int(double [][][] mask){
		RegionStatisticsSolver RSS;


		RSS= new RegionStatisticsSolver(temp1[0],temp2[0], temp3[0], patch, 10,p);
		RSS.eval(mask);
		//Analysis.p.cl[0]=RSS.betaMLEout;
		//Analysis.p.cl[1]=RSS.betaMLEin;

		cout=RSS.betaMLEout;
		cin=RSS.betaMLEin;
		//IJ.log("cin" + cin);

		//IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", RSS.betaMLEout,RSS.betaMLEin));	


	}


	private  void set_patch_geom(Region r){
		int xmin, ymin , zmin, xmax, ymax, zmax; 
		xmin=nx;ymin=ny;zmin=nz;
		xmax=0;ymax=0;zmax=0;

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			if(p.px<xmin)xmin=p.px;
			if(p.px>xmax)xmax=p.px;
			if(p.py<ymin)ymin=p.py;
			if(p.py>ymax)ymax=p.py;
			if(p.pz<zmin)zmin=p.pz;
			if(p.pz>zmax)zmax=p.pz; 
		}

		xmin=Math.max(0,xmin-margin);
		xmax=Math.min(nx,xmax+margin+1);

		ymin=Math.max(0,ymin-margin);
		ymax=Math.min(ny,ymax+margin+1);

		if(nz>1){//if(zmax-zmin>0){
			// do whole column
			//zmin=0;
			//zmax=p.nz-1;
			//old one
			zmin=Math.max(0,zmin-zmargin);
			zmax=Math.min(nz,zmax+zmargin+1);			
		}



		this.sx=(xmax-xmin);//todo :correct :+1 : done
		this.sy=(ymax-ymin);//correct :+1

		cx=xmin;
		cy=ymin;

		if(nz==1){//if(zmax-zmin==0){
			this.sz=1;
			cz=0;
		}
		else{
			this.sz=(zmax-zmin);
			cz=zmin;
		}



	}

	private void normalize(){

		intmin=Double.MAX_VALUE;
		intmax=0;
		for (int z=0; z<sz; z++){	
			for (int i=0; i<sx; i++){  
				for (int j=0;j< sy; j++){  		
					if(patch[z][i][j]>intmax)intmax=patch[z][i][j];
					if(patch[z][i][j]<intmin)intmin=patch[z][i][j];
				}	
			}
		}


		//IJ.log("max:"+intmax+" min:"+intmin);
		//rescale between 0 and 1
		for (int z=0; z<sz; z++){
			for (int i=0; i<sx; i++){  
				for (int j=0;j<sy; j++){  
					patch[z][i][j]= (patch[z][i][j] - intmin)/(intmax-intmin);		
				}	
			}
		}

	}

	private void fill_mask(Region r){
		this.mask=new double [1][sz][sx][sy];
		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){  
				for (int j=0;j< sy; j++){  
					this.mask[0][z][i][j] = 0;	
				}
			}
		}

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			int rz,rx,ry;
			rz=(p.pz-cz);
			rx=(p.px-cx);
			ry=(p.py-cy);
			this.mask[0][rz][rx][ry] = 1;	
			//this.mask[0][os*(p.pz-offsetz)][os*(p.px-offsetx)][os*(p.py-offsety)]=1;
		}

	}

	public  void setIntensitiesandCenters(Region r, double [][][] image){
		//IJ.log("starting setint" );

		//IJ.log("rvalue int comp" + r.value);
		regionIntensityAndCenter(r,image);

	}

	public  void setPerimeter(Region r, short [][][] regionsA){

		if(p.nz==1)
			regionPerimeter(r,regionsA);
		else
			regionPerimeter3D(r,regionsA);


	}


	public  void regionPerimeter(Region r, short [] [] [] regionsA){
		//2 Dimensions only
		double pr=0;
		int rvalue= r.value;

		//IJ.log("region: " + r.value);
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			int edges=0;
			Pix v = it.next();
			//count number of free edges
			if(v.px!=0 && v.px!=nx-1 && v.py!=0 && v.py!=ny-1){//not on edges of image
				if(regionsA[v.pz][v.px-1][v.py]==0)edges++;
				if(regionsA[v.pz][v.px+1][v.py]==0)edges++;
				if(regionsA[v.pz][v.px][v.py-1]==0)edges++;
				if(regionsA[v.pz][v.px][v.py+1]==0)edges++;//!=rvalue
			}
			else
				edges++;
			//			if(edges==1)pr+=1;
			//			if(edges==2)pr+=Math.sqrt(2);
			//			if(edges==3)pr+=2;

			pr+=edges; // real number of edges (should be used with the subpixel)

			//IJ.log("coord " + v.px + ", " + v.py +", "+ v.pz +"edges " + edges);


		}
		//return (sum/count);
		r.perimeter=pr;
		//IJ.log("perimeter " +pr);
		if(Analysis.p.subpixel){r.perimeter=pr/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);}
		//IJ.log("perimeter " +r.perimeter);

	}

	public  void regionPerimeter3D(Region r, short [] [] [] regionsA){
		//2 Dimensions only
		double pr=0;
		int rvalue= r.value;

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			int edges=0;
			Pix v = it.next();
			//count number of free edges
			if(v.px!=0 && v.px!=nx-1 && v.py!=0 && v.py!=ny-1 && v.pz!=0 && v.pz!=nz-1){//not on edges of image
				if(regionsA[v.pz][v.px-1][v.py]==0)edges++;
				if(regionsA[v.pz][v.px+1][v.py]==0)edges++;
				if(regionsA[v.pz][v.px][v.py-1]==0)edges++;
				if(regionsA[v.pz][v.px][v.py+1]==0)edges++;
				if(regionsA[v.pz+1][v.px][v.py]==0)edges++;
				if(regionsA[v.pz-1][v.px][v.py]==0)edges++;
			}else
				edges++;
			//			if(edges==1)pr+=1;
			//			if(edges==2)pr+=Math.sqrt(2);
			//			if(edges==3)pr+=Math.sqrt(2);
			//			if(edges==4)pr+=Math.sqrt(2);
			//			if(edges==5)pr+=2*Math.sqrt(2);

			pr+=edges;

			//IJ.log("coord " + v.px + ", " + v.py +", "+ v.pz +"edges " + edges);


		}
		//return (sum/count);
		r.perimeter=pr;
		//IJ.log("perimeter " +pr);
		if(osxy>1){
			if(p.nz==1){
				r.perimeter=pr/(osxy);
			}
			else
				r.perimeter=pr/(osxy*osxy);
		}
		//IJ.log("perimeter " +r.perimeter);

	}

	public void regionIntensityAndCenter(Region r, double [] [] [] image){

		int count=0;
		double sum=0;
		double sumx=0;
		double sumy=0;
		double sumz=0;
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			if(!Analysis.p.refinement){
				sum+= image[p.pz][p.px][p.py] ;
			}
			//sum+= mask[p.pz][p.px][p.py] & 0xFF; //for conversion, corrects sign problem

			//if(p.px==277 && p.py==202 && p.pz==7){IJ.log("test value rint:" + maskA[p.pz][p.px][p.py]);}
			//if(r.value==6)IJ.log("value byte" + mask[p.pz][p.px][p.py] + " x"+ p.px +"y" + p.py + "z"+ p.pz);
			sumx+=p.px;
			sumy+=p.py;
			sumz+=p.pz;
			count++;
		}

		//return (sum/count);
		//r.intensity=(sum/(count*255));
		if(!Analysis.p.refinement){
			r.intensity=(sum/(count));
		}//done in refinement
		//IJ.log("inten " + r.intensity);

		r.cx= (float) (sumx/count);
		r.cy= (float) (sumy/count);
		r.cz= (float) (sumz/count);

		if(Analysis.p.subpixel){
			r.cx= r.cx/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
			r.cy= r.cy/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
			r.cz= r.cz/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
		}

	}


	public  void setlength(Region r, short [][][] regionsA){
		//2D only yet
		ImagePlus skeleton= new ImagePlus();
		//compute skeletonization
		//	int osxy=1;
		//	int osz=1;
		//		if(p.subpixel && p.refinement){
		//			osxy=p.oversampling2ndstep*p.interpolation;
		//			if(p.nz>1){
		//				osz=p.oversampling2ndstep*p.interpolation;
		//			}
		//		}
		//		int di,dj;
		//		di=p.ni *osxy;
		//		dj=p.nj *osxy;
		byte[] mask_bytes = new byte[sx*sy];
		for (int i=0; i<sx; i++) {
			for (int j=0; j<sy; j++) {  
				if(regionsA[0][cx+i][cy+j]>0)
					mask_bytes[j * sx + i]= (byte) 0;
				else
					mask_bytes[j * sx + i]=(byte) 255;
			}
		}
		ByteProcessor bp = new ByteProcessor(sx, sy);
		bp.setPixels(mask_bytes);
		skeleton.setProcessor("Skeleton",bp);
		//skeleton.show();


		//do voronoi in 2D on Z projection
		IJ.run(skeleton, "Skeletonize", "");
		//skeleton.show();
		//		if (Analysis.p.save_images){
		//		String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_skel_c1" +".zip";
		//		IJ.saveAs(skeleton, "ZIP", savepath);
		//		}

		regionlength(r, skeleton);


	}


	public  void setlength3D(Region r, short [][][] regionsA){
		//2D only yet
		ImagePlus skeleton= new ImagePlus();
		//compute skeletonization
		//	int osxy=1;
		//	int osz=1;
		//		if(p.subpixel && p.refinement){
		//			osxy=p.oversampling2ndstep*p.interpolation;
		//			if(p.nz>1){
		//				osz=p.oversampling2ndstep*p.interpolation;
		//			}
		//		}
		//		int di,dj;
		//		di=p.ni *osxy;
		//		dj=p.nj *osxy;

		//set all to zero
		byte[] mask_bytes = new byte[sx*sy];
		for (int i=0; i<sx; i++) {
			for (int j=0; j<sy; j++) {  
				mask_bytes[j * sx + i]= (byte) 255;
			}
		}


		for (int i=0; i<sx; i++) {
			for (int j=0; j<sy; j++) {
				for (int k=0; k<sz; k++) {
					if(regionsA[cz+k][cx+i][cy+j]>0)
						mask_bytes[j * sx + i]= (byte) 0;
					//else
					//	mask_bytes[j * sx + i]=(byte) 255;
				}
			}
		}
		ByteProcessor bp = new ByteProcessor(sx, sy);
		bp.setPixels(mask_bytes);
		skeleton.setProcessor("Skeleton",bp);
		//skeleton.show();


		//do voronoi in 2D on Z projection
		IJ.run(skeleton, "Skeletonize", "");
		//if(region.value==6 ||region.value==19)
		//skeleton.show();


		//		if (Analysis.p.save_images){
		//		String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_skel_c1" +".zip";
		//		IJ.saveAs(skeleton, "ZIP", savepath);
		//		}

		regionlength3D(r, skeleton);



	}

	public  void regionlength3D(Region r, ImagePlus skel){
		//2 Dimensions only
		int length=0;
		int x,y;
		x= skel.getWidth();
		y=skel.getHeight();
		//IJ.log("object length "+ r.value);
		for (int i=0; i<x; i++) {
			for (int j=0; j<y; j++) {
				//Pix v = it.next();
				//count number of pixels in skeleton
				if(skel.getProcessor().getPixel(i, j)==0)length++;
				//if(skel.getProcessor().getPixel(v.px, v.py)==0)IJ.log("coord " + v.px + ", " + v.py);

			}
		}
		//return (sum/count);
		r.length=length;
		if(osxy>1){r.length= ((double)length)/osxy;}



	}


	public  void regionlength(Region r, ImagePlus skel){
		//2 Dimensions only
		int length=0;
		//IJ.log("object clength "+ r.value);
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix v = it.next();
			//count number of pixels in skeleton
			if(skel.getProcessor().getPixel(v.px-cx, v.py-cy)==0)length++;
			//if(skel.getProcessor().getPixel(v.px, v.py)==0)IJ.log("coord " + v.px + ", " + v.py);

		}
		//return (sum/count);
		r.length=length;
		if(osxy>1){r.length= ((double)length)/osxy;}



	}

	private float round(float y, int z){
		//Special tip to round numbers to 10^-2
		y*=Math.pow(10,z);
		y=(int) y;
		y/=Math.pow(10,z);
		return y;
	}

}
