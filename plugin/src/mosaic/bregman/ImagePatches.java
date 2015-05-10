package mosaic.bregman;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
//import java.util.Date;
import java.util.Iterator;
//import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
//import ij.gui.ImageWindow;
//import ij.process.ByteProcessor;
//import ij.process.ColorProcessor;


public class ImagePatches {
	ImagePlus regsresultx;
	ImagePlus regsresulty;
	//	ImageWindow iwx;
	//	ImageWindow iwy;
	int interp;
	int interpz;
	int jobs_done;
	int nb_jobs;
	//reg
	int osxy;
	int osz; //oversampling 
	int sx;
	int sy;
	int sz;//size of full image with oversampling
	Parameters p;
	public short [] [] [] regions_refined;
	double [][][] image;
	double [][][] w3kbest;
	byte [] imagecolor_c1;//for display ints = new int [dz][di][dj][3];

	double max;
	double min;
	public ArrayList<Region> regionslist_refined;
	private ArrayList<Region> globalList;
	int channel;
	boolean fcallx, fcally;



	public ImagePatches(Parameters pa,ArrayList<Region> regionslist, double [][][] imagei, int channeli, double [][][] w3k, double min , double max)
	{
		if (!pa.subpixel){pa.oversampling2ndstep=1; pa.interpolation=1;}
		else{pa.oversampling2ndstep=pa.overs;}
		this.regsresulty=new ImagePlus();
		this.regsresultx=new ImagePlus();
		this.w3kbest=w3k;
		//		this.iwx= new ImageWindow("Objects X");
		//		this.iwy= new ImageWindow("Objects Y");
		//		fcallx=true;
		//		fcally=true;

		this.channel=channeli;
		this.p=pa;
		this.interp=pa.interpolation;
		this.osxy=p.oversampling2ndstep*pa.interpolation;
		this.globalList=new ArrayList<Region>();
		this.regionslist_refined=regionslist;
		this.image=imagei;
		this.sx=p.ni*pa.oversampling2ndstep*pa.interpolation;
		this.sy=p.nj*pa.oversampling2ndstep*pa.interpolation;
		this.jobs_done=0;
		this.max = max;
		this.min = min;
		//IJ.log("sx " + sx);
		if(p.nz==1)
		{
			this.sz=1;
			this.osz=1;
			this.interpz=1;
		}
		else
		{
			this.sz=p.nz*p.oversampling2ndstep*pa.interpolation;
			this.osz=p.oversampling2ndstep*pa.interpolation;
		}
		//IJ.log("regions refined");
		//Tools.showmem();
		//Runtime.getRuntime().gc();
		//Tools.showmem();

		regions_refined= new short[sz][sx][sy];
		//Tools.showmem();

		if(p.dispint)
		{
			//IJ.log("disp int");
			//Tools.showmem();
			//Runtime.getRuntime().gc();
			//Tools.showmem();
			imagecolor_c1 = new byte [sz*sx*sy*3]; // add fill background
			//Tools.showmem();
			int b0, b1, b2;
			b0=(int) Math.min(255, 255*Analysis.p.betaMLEoutdefault);
			b1= (int) Math.min(255, 255*Math.sqrt(Analysis.p.betaMLEoutdefault)) ;
			b2=(int) Math.min(255, 255*Math.pow(Analysis.p.betaMLEoutdefault,2)) ;


			//set all to background
			for (int z=0; z<sz; z++) 
			{
				for (int i=0;i<sx;i++) 
				{
					int t=z*sx*sy*3+i*sy*3;
					for (int j=0;j< sy;j++)
					{
						imagecolor_c1[t+j*3+0]= (byte) b0 ; //Red
						imagecolor_c1[t+j*3+1]= (byte) b1 ; //Green
						imagecolor_c1[t+j*3+2]= (byte) b2 ; //Blue
					}
				}
			}


		}
		fill_refined();

	}

	public void run(){
		distribute_regions();

		//		if(p.dispint){
		//			IJ.log("disp int");
		//			displayintensities(regionslist_refined, sz,sx,sy);
		//		}

		Analysis.meansize_refined=computeMeanRegionSize(regionslist_refined)/(interp*osxy);
		//IJ.log("Mean size of refined regions : " + Analysis.meansize_refined);
	}

	/**
	 * 
	 * Patch creation, distribution and assembly
	 * 
	 */
	
	public void distribute_regions()
	{
		//assuming rvoronoi and regionslists (objects)  in same order (and same length)

		final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		ThreadPoolExecutor threadPool;
		if(p.debug == true){threadPool=new ThreadPoolExecutor(1, 1,
				1, TimeUnit.DAYS, queue);}
		else{
		threadPool=new ThreadPoolExecutor(/*p.nthreads*/1, /*p.nthreads*/1,
				1, TimeUnit.DAYS, queue);}

		nb_jobs=regionslist_refined.size();
		AnalysePatch ap;
		for (Iterator<Region> it = regionslist_refined.iterator(); it.hasNext();) 
		{			
			Region r = it.next();
			//if(r.value==5){
			//IJ.log("call os " + p.oversampling2ndstep);
			//IJ.log("interp " + p.interpolation);
			if(p.interpolation>1)p.subpixel=true;
			if(p.subpixel)p.oversampling2ndstep=p.overs;
			else p.oversampling2ndstep=1;
			ap=new AnalysePatch(image, r, p, p.oversampling2ndstep, channel,regions_refined,this);
			if(p.mode_voronoi2)
			{
				threadPool.execute(ap);
				//IJ.log("size q:"+ queue.size());
			}
			else
			{
				//	ap.run();//execute
			}
			
			/////////////////
			
			//add refined result into regions refined :
			if(!p.mode_voronoi2){


				if(interp==1)
					assemble_result(ap,r);
				else
					assemble_result_interpolated(ap,r);
			}
			
			//			else{
			//				assemble_result_voronoi2(ap);				
			//			}
			//}
		}
		//IJ.log("loop done");
		
		//
		
		threadPool.shutdown();
		
		try
		{
			//IJ.log("await termination");
			threadPool.awaitTermination(1, TimeUnit.DAYS);
		}
		catch (InterruptedException ex) {}
		
		//long lStartTime = new Date().getTime(); //start time
		
		if(p.mode_voronoi2)
		{
			//find_regions();
			regionslist_refined=globalList;


//			final LinkedBlockingQueue<Runnable> queue2 = new LinkedBlockingQueue<Runnable>();
			ThreadPoolExecutor threadPool2=new ThreadPoolExecutor(1, 1,
					1, TimeUnit.DAYS, queue);

			ObjectProperties Op;
			//calculate regions intensities
			for (Iterator<Region> it = regionslist_refined.iterator(); it.hasNext();) {
				Region r = it.next();
				//IJ.log("int region :" +r.value);
				Op= new ObjectProperties(image, r, sx, sy, sz, p,  osxy,  osz, imagecolor_c1, regions_refined );
				threadPool2.execute(Op);
				//Op.run();
			}

			threadPool2.shutdown();
			try{
				//IJ.log("await termination");
				//Tools.showmem();
				threadPool2.awaitTermination(1, TimeUnit.DAYS);
			}catch (InterruptedException ex) {}
			
			// here we analyse the patch
			// if we have a big region with intensity near the background
			// kill that region
			
			boolean changed = false;
			
			ArrayList<Region> regionslist_refined_filter = new ArrayList<Region>();
			
			for (Region r : regionslist_refined)
			{
				if (r.intensity * (max-min) + min > p.min_region_filter_intensities)
				{
					regionslist_refined_filter.add(r);
				}
				else
				{
					changed= true;
				}
			}
			
			regionslist_refined = regionslist_refined_filter;
			
			// if changed, reassemble
			
			if (changed == true)
			{
				for (int i = 0; i < regions_refined.length  ; i++)
				{
					for (int j = 0; j < regions_refined[i].length  ; j++)
					{
						for (int k = 0; k < regions_refined[i][j].length  ; k++)
						{
							regions_refined[i][j][k] = 0;
						}	
					}
				}
				assemble(regionslist_refined);
			}
			
			//
			
			regionslist_refined = regionslist_refined_filter;
		}
		
		//Tools.showmem();
		int no=regionslist_refined.size();
		if(channel==0)
		{
			IJ.log(no + " objects found in X.");
			Frame frame = WindowManager.getFrame("Log"); 
			if (frame != null) { 
				GenericGUI.setwindowlocation(100, 680, frame);
			} 
		}
		else
		{IJ.log(no + " objects found in Y.");}
	}

	void assemble(ArrayList<Region> regionslist_refined)
	{
		for (Iterator<Region> it = regionslist_refined.iterator(); it.hasNext();) 
		{
			Region r = it.next();

			for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
				Pix v = it2.next();
				//count number of free edges
				regions_refined[v.pz][v.px][v.py]= (short) r.value;

			}
		}
	}

	/**
	 * 
	 * Assemble the result
	 * 
	 * @param regionslist_refined List of regions to assemble
	 * @param regions_refined regions refined
	 */
	
	static public void assemble(Collection<Region> regionslist_refined, short[][][] regions_refined)
	{
		for (Iterator<Region> it = regionslist_refined.iterator(); it.hasNext();) 
		{
			Region r = it.next();

			for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
				Pix v = it2.next();
				//count number of free edges
//				if (v.pz >= regions_refined.length || v.px >= regions_refined[0].length || v.py >= regions_refined[0][0].length)
//				{
//					int debug = 0;
//					debug++;
//				}
				
				regions_refined[v.pz][v.px][v.py]= (short) r.value;

			}
		}
	}
	
//	private void assemble_result_voronoi2(AnalysePatch ap){
//
//		//IJ.log("assemble vo2");
//		//build regions refined (for interpolated regions)
//		if(interp==1){
//			//IJ.log("no interpolation");
//			for (int z=0; z<ap.sz; z++){
//				for (int i=0;i<ap.sx; i++){  
//					for (int j=0;j< ap.sy; j++){  
//						if (ap.object[z][i][j]==1){
//							regions_refined[z+ap.offsetz*osz][i+ap.offsetx*osxy][j+ap.offsety*osxy] = (short) ap.r.value;
//							//if(ap.r.value ==3){IJ.log("z" + z +" i" +i+" j" +j);
//							//IJ.log("offsetz x  y " + ap.offsetz +" " + ap.offsetx +" " + ap.offsety+ "osz xy " + osz +" " + osxy);}
//						}
//					}
//				}
//			}
//		}
//		else{
//			for (int z=0; z<ap.isz; z++){
//				for (int i=0;i<ap.isx; i++){  
//					for (int j=0;j< ap.isy; j++){  
//						if (ap.interpolated_object[z][i][j]==1){
//							regions_refined[z+ap.offsetz*osz][i+ap.offsetx*osxy][j+ap.offsety*osxy]= (short) ap.r.value;
//
//						}
//					}
//
//					//					else
//					//						regions_refined[z+ap.offsetz*osz][i+ap.offsetx*osxy][j+ap.offsety*osxy] =0;
//				}
//			}
//		}
//
//		//IJ.log("assemble done");
//	}

	public synchronized void addRegionstoList(ArrayList<Region> localList){
		//boolean disp=false;
		//		if(localList.size()>1)
		//			disp=true;
		int index;//build index of region (will be r.value)
		for (Iterator<Region> it = localList.iterator(); it.hasNext();) {
			Region r = it.next();
			index= globalList.size()+1;
			//			if(disp){
			//				IJ.log("index" + index + "size" + r.pixels.size());
			//			}
			r.value=index;
			globalList.add(r);
		}


		jobs_done++;

		IJ.showStatus("Computing segmentation  " + round(55+ (45*((double) jobs_done)/(nb_jobs)),2) + "%");
		IJ.showProgress(0.55+0.45*(jobs_done)/(nb_jobs));


	}

	private double round(double y, int z){
		//Special tip to round numbers to 10^-2
		y*=Math.pow(10,z);
		y=(int) y;
		y/=Math.pow(10,z);
		return y;
	}

//	private void find_regions(){
//
//		//IJ.log("find regions vo 2");
//		//find connected regions
//		ImagePlus maska_im= new ImagePlus();
//		ImageStack maska_ims= new ImageStack(sx,sy);
//
//		for (int z=0; z<sz; z++){  
//			byte[] maska_bytes = new byte[sx*sy];
//			for (int j=0;j< sy; j++){  
//				for (int i=0; i<sx; i++){  
//					if(regions_refined[z][i][j]>=1){maska_bytes[j * sx + i] = (byte) (regions_refined[z][i][j]);}
//					else
//					{maska_bytes[j * sx + i] = (byte) 0;}
//				}
//			}
//			ByteProcessor bp = new ByteProcessor(sx, sy);
//			bp.setPixels(maska_bytes);
//			maska_ims.addSlice("", bp);
//		}
//
//		maska_im.setStack("test Mask vo2",maska_ims);
//		//maska_im.show();
//
//		FindConnectedRegions fcr= new FindConnectedRegions(maska_im, sx,sy,sz);//maska_im only
//
//
//
//		double thr=0.5;
//
//		float [][][]	Ri = new float [sz][sx][sy];
//		for (int z=0; z<sz; z++){
//			for (int i=0; i<sx; i++) {  
//				for (int j=0; j<sy; j++) {  
//					Ri[z][i][j]= (float)thr;
//				}
//			}
//		}
//
//		fcr.run(thr,channel,sx*sy*sz,3*osxy,0,Ri,true,false);//min size was 5//5*osxy
//		//fcr.run(d,0,p.maxves_size,p.minves_size,255*p.min_intensity,Ri,true,p.save_images&&(!p.refinement));
//		regionslist_refined=fcr.results;
//		IJ.log("Objects found 2nd pass:" + regionslist_refined.size());
//		//fcr.run(d,0,p.maxves_size,p.minves_size,255*p.min_intensity,Ri,true,p.save_images&&(!p.refinement));
//		regions_refined=fcr.tempres;
//
//	}


	private void assemble_result(AnalysePatch ap, Region r)
	{		
		ArrayList<Pix> rpixels = new ArrayList<Pix>();
		int pixcount=0;
		for (int z=0; z<ap.sz; z++)
		{
			for (int i=0;i<ap.sx; i++)
			{
				for (int j=0;j< ap.sy; j++)
				{
					if (ap.object[z][i][j]==1)
					{
						regions_refined[z+ap.offsetz*osz][i+ap.offsetx*osxy][j+ap.offsety*osxy] =(short) r.value;
						rpixels.add(new Pix(z+ap.offsetz*osz,i+ap.offsetx*osxy,j+ap.offsety*osxy));
						//rpixels.add(new Pix(z,i,j));
						pixcount++;
					}

					//					else
					//						regions_refined[z+ap.offsetz*osz][i+ap.offsetx*osxy][j+ap.offsety*osxy] =0;
				}
			}
		}
		//assign new pixel list to region and refined size
		r.pixels=rpixels;
		r.rsize=pixcount;
		r.intensity=ap.cin*(ap.intmax-ap.intmin) +ap.intmin;
		//		r.cx=r.cx*osxy;
		//		r.cy=r.cy*osxy;
		//		r.cz=r.cz*osz;

	}

	private void assemble_result_interpolated(AnalysePatch ap, Region r){

		ArrayList<Pix> rpixels = new ArrayList<Pix>();
		int pixcount=0;
		for (int z=0; z<ap.isz; z++){
			for (int i=0;i<ap.isx; i++){  
				for (int j=0;j< ap.isy; j++){  
					if (ap.interpolated_object[z][i][j]==1){
						regions_refined[z+ap.offsetz*osz][i+ap.offsetx*osxy][j+ap.offsety*osxy]= (short) r.value;
						rpixels.add(new Pix(z+ap.offsetz*osz,i+ap.offsetx*osxy,j+ap.offsety*osxy));
						//rpixels.add(new Pix(z+ap.offsetz*osz,i+ap.offsetx*osxy,i+ap.offsetx*osxy));
						pixcount++;
					}
				}

				//					else
				//						regions_refined[z+ap.offsetz*osz][i+ap.offsetx*osxy][j+ap.offsety*osxy] =0;
			}
		}
		//IJ.log("added with size " + pixcount);
		//IJ.log("added with int" + ap.cin);
		//assign new pixel list to region and refined size
		r.pixels=rpixels;
		r.rsize=pixcount;
		//r.intensity=ap.cin;
		r.intensity=ap.cin*(ap.intmax-ap.intmin) +ap.intmin;
		//		r.cx=r.cx*osxy;
		//		r.cy=r.cy*osxy;
		//		r.cz=r.cz*osz;


	}



	private void fill_refined(){
		for (int z=0; z<sz; z++){
			for (int i=0;i<sx; i++){  
				for (int j=0;j< sy; j++){  
					regions_refined[z][i][j]=0;;
				}
			}
		}


	}
	//display function
/*	public void displayRegions(short [][][] regions, int width,int height,int depth, int channel, boolean displ, boolean save, boolean invert){
		ImageStack regstackx;
		ImageStack regstacky;




		if(channel==0){
			regstackx=new ImageStack(width,height);

			int min = 0;
			int max = Math.max(regionslist_refined.size(), 255 );
			for (int z=0; z<depth; z++){
				short[] mask_short = new short[width*height];
				for (int i=0; i<width; i++) {
					for (int j=0; j<height; j++) {  
						mask_short[j * width + i]= (short) regions[z][i][j];
					}
				}
				ShortProcessor sp = new ShortProcessor(width, height);
				sp.setPixels(mask_short);
				sp.setMinAndMax( min, max );
				regstackx.addSlice("", sp);
			}



			regstackx.setColorModel(backgroundAndSpectrum(Math.min(regionslist_refined.size(),255), invert));				
			regsresultx.setStack("Regions X",regstackx);





			if(displ && p.dispcolors && p.dispwindows){// && !Analysis.p.save_images
				regsresultx.setTitle("Colorized objects, channel 1");
				regsresultx.show();
				GenericGUI.setimagelocation(1200,50,regsresultx);
			}

			if(displ && p.displabels  && p.dispwindows){// && !Analysis.p.save_images
				ImagePlus dupx= regsresultx.duplicate();
				dupx.setTitle("Labelized objects, channel 1");
				IJ.run(dupx, "Grays", "");
				dupx.show();
				GenericGUI.setimagelocation(1210,60,dupx);
			}


			//			if(displ && p.dispwindows){
			//				//IJ.log("displaying");
			//				regsresultx.show(); 
			//				//ImageWindow iw= ij.WindowManager.getCurrentWindow();
			//				//	iw.setLocationAndSize(100,100,512,512);
			//				//regsresultx.setActivated();
			//				//				if(fcallx){
			//				//					iwx= new ImageWindow(regsresultx);
			//				//					fcallx=false;
			//				//				}
			//				//				else{
			//				//				iwx.updateImage(regsresultx);
			//				//				}
			//			}

			String savepath;
			if(save && Analysis.p.save_images && Analysis.p.displabels){
				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1" +".zip";
				IJ.saveAs(regsresultx, "ZIP", savepath);
			}

			if(save && Analysis.p.save_images && Analysis.p.dispcolors){
				IJ.run(regsresultx,"RGB Color", "");
				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1_RGB" +".zip";
				IJ.saveAs(regsresultx, "ZIP", savepath);
			}

		}
		else
		{

			regstacky=new ImageStack(width,height);

			int min = 0;
			int max = Math.max(regionslist_refined.size(), 255 );
			for (int z=0; z<depth; z++){
				short[] mask_short = new short[width*height];
				for (int i=0; i<width; i++) {
					for (int j=0; j<height; j++) {  
						mask_short[j * width + i]= (short) regions[z][i][j];
					}
				}
				ShortProcessor sp = new ShortProcessor(width, height);
				sp.setPixels(mask_short);
				sp.setMinAndMax( min, max );
				regstacky.addSlice("", sp);
			}


			regstacky.setColorModel(backgroundAndSpectrum(Math.min(regionslist_refined.size(),255), invert));				
			regsresulty.setStack("Regions Y",regstacky);
			//			if(displ && p.dispwindows){
			//				regsresulty.show(); 
			//				regsresulty.setActivated();
			//			}


			if(displ && p.dispcolors && p.dispwindows){// && !Analysis.p.save_images
				regsresulty.setTitle("Colorized objects, channel 2");
				regsresulty.show();
				GenericGUI.setimagelocation(1200,630,regsresulty);
			}

			if(displ && p.displabels  && p.dispwindows){// && !Analysis.p.save_images
				ImagePlus dupy= regsresulty.duplicate();
				dupy.setTitle("Labelized objects, channel 2");
				IJ.run(dupy, "Grays", "");
				dupy.show();
				GenericGUI.setimagelocation(1210,640,dupy);
			}


			String savepath;
			if(save && Analysis.p.save_images && Analysis.p.displabels){
				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c2" +".zip";
				IJ.saveAs(regsresulty, "ZIP", savepath);
			}

			if(save && Analysis.p.save_images && Analysis.p.dispcolors){
				IJ.run(regsresulty,"RGB Color", "");
				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c2_RGB" +".zip";
				IJ.saveAs(regsresulty, "ZIP", savepath);
			}


		}

	}*/

/*	private IndexColorModel backgroundAndSpectrum(int maximum, boolean invert) {
		//IJ.log("make color model");
		if( maximum > 255 )
			maximum = 255;
		byte [] reds = new byte[256];
		byte [] greens = new byte[256];
		byte [] blues = new byte[256];
		// Set all to white:
		for( int i = 0; i < 256; ++i ) {
			reds[i] = greens[i] = blues[i] = (byte)255;
		}
		// Set 0 to black:
		reds[0] = greens[0] = blues[0] = 0;
		float divisions = maximum;
		Color c;
		for( int i = 1; i <= maximum; ++i ) {
			float h;
			if(invert){
				//h = (i - 1) / divisions;
				h = (maximum -i) / divisions;
			}
			else
			{
				h = (i - 1) / divisions;
				//h = (maximum -i) / divisions;
			}
			c = Color.getHSBColor(h,1f,1f);
			reds[i] = (byte)c.getRed();
			greens[i] = (byte)c.getGreen();
			blues[i] = (byte)c.getBlue();
			//if(i==10){IJ.log("red" + reds[i]+"green"+greens[i]+"blue"+blues[i]);}
		}
		return new IndexColorModel( 8, 256, reds, greens, blues );
	}*/

	public static double computeMeanRegionSize(ArrayList<Region> regionlist){
		double total =0;
		int number=0;
		for (Iterator<Region> it = regionlist.iterator(); it.hasNext();) {
			Region r = it.next();
			total+=r.rsize;
			number++;
		}

		return (total/number);
	}




}
