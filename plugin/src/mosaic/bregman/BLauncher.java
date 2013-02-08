package mosaic.bregman;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.plugin.Resizer;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import mosaic.bregman.FindConnectedRegions.Region;

public class BLauncher {
	public  int hcount=0;
	public  String headlesscurrent;
	PrintWriter out;
	PrintWriter out2;
	PrintWriter out3;
	String wpath;
	Tools Tools;


	public BLauncher(String path){
		wpath=path;

		boolean processdirectory =(new File(wpath)).isDirectory();
		if(processdirectory){
			//IJ.log("Processing directory");
			Headless_directory();

		}
		else{
			//IJ.log("Processing file");
			Headless_file();

		}

	}

	public void Headless_file(){


		try{
			Analysis.p.wd= (new File(wpath)).getParent() +File.separator;
			ImagePlus img=IJ.openImage(wpath);
			Analysis.p.nchannels=img.getNChannels();
			Analysis.p.dispwindows=true;


			if(Analysis.p.save_images){
				//IJ.log(wpath);
				String savepath =  wpath.substring(0,wpath.length()-4);
				//IJ.log(savepath);


				if(Analysis.p.nchannels==2){
					//IJ.log("looking for files at " + wpath);
					out  = new PrintWriter(savepath+"_coloc"+ ".csv");
					//out2 = new PrintWriter(savepath+"_Xdata"+ ".csv");
					out2 = new PrintWriter(savepath+"_Objectsdata_c1"+ ".csv");
					out3 = new PrintWriter(savepath+"_Objectsdata_c2"+ ".csv");
					//out3 = new PrintWriter(savepath+"_Ydata"+ ".csv");
					//PrintWriter out = new PrintWriter(dir1.replaceAll("/", "_") + ".csv");
					//IJ.log("files open");

					//					out.print(
					//							"background removal" + Analysis.p.removebackground +
					//							"rolling ball window size" + Analysis.p.size_rollingball +
					//							"stddev PSF xy" + Analysis.p.sigma_gaussian+
					//							"stddev PSF z" + Analysis.p.sigma_gaussian/Analysis.p.zcorrec+
					//							"lambda prior" + Analysis.p.lreg  +
					//							"Min intensity " + Analysis.p.min_intensity +"," +
					//							"subpixel" + Analysis.p.subpixel +
					//							"Intensity estim�ation" + choice1[Analysis.p.mode_intensity] +
					//							"Noise model" + choice1[Analysis.p.noise_model]
					//							);

					out.println();
					out.print("File"+ "," +"Image number" + ","+ "Objects ch X" + "," + "Mean size ch X"  +"," 
							+ "Objects ch Y"+"," + "Mean size ch Y" +"," + "Colocalization X in Y"
							+"," + "Colocalization Y in X"
							+"," + "Mean Y intensity in X objects"
							+"," + "Mean X intensity in Y objects");
					out.println();


					out2.print("Image number" + "," + "Region in 1"+ "," 
							+ "Size" + "," + "Perimeter" + "," + "Length" + "," +"Intensity" + "," 
							+ "Overlap with 2" +","+ "MColoc size" + ","+ "MColoc Intensity" + "," + "Single Coloc" + ","  + "Coord X"+ "," + "Coord Y"+ "," + "Coord Z");
					out2.println();


					out3.print("Image number" + "," + "Region in 2" +","
							+ "Size" + "," + "Perimeter" + "," + "Length" + "," +"Intensity" + ","
							+ "Overlap with 1" +","+ "MColoc size" + ","+ "MColoc Intensity" + "," + "Single Coloc" + ","  + "Coord X"+ "," + "Coord Y"+ "," + "Coord Z");

					out3.println();


					out.flush();

				}

				else
				{
					out2 = new PrintWriter(savepath+"_Objectsdata"+ ".csv");


					out2.print("Image number" + "," + "Region in X"+ "," + "Size" + "," + "Perimeter" + "," + "Length" + "," +
							"Intensity" + ","  + "Coord X"+ "," + "Coord Y"+ "," + "Coord Z");
					out2.println();


					out2.flush();


				}
			}
			//IJ.log("single file start headless");
			//IJ.log("start headless file");
			bcolocheadless(img);
			IJ.log("");
			IJ.log("Done");


			if(Analysis.p.save_images){
				String choice1[] = {
						"Automatic", "Medium layer","High layer"};
				String choice2[] = {
						"Poisson", "Gauss"};
	
				out.print(
						"background removal " + Analysis.p.removebackground + "," +
						"rolling ball window size " + Analysis.p.size_rollingball + "," +
						"stddev PSF xy " + Analysis.p.sigma_gaussian+ "," +
						"stddev PSF z " + Analysis.p.sigma_gaussian/Analysis.p.zcorrec+ "," +
						"lambda prior " + Analysis.p.lreg  + "," +
						"Min intensity " + Analysis.p.min_intensity +"," +
						"subpixel " + Analysis.p.subpixel + "," +
						"Intensity estimation " + choice1[Analysis.p.mode_intensity] + "," +
						"Noise model " + choice2[Analysis.p.noise_model]
						);
			}

		}catch (Exception e){//Catch exception if any
			System.err.println("Error launcher file processing: " + e.getMessage());
		}

	}

	public void Headless_directory(){
		//IJ.log("starting dir");
		try{
			wpath=wpath + File.separator;
			Analysis.p.wd= wpath;
			Analysis.doingbatch=true;

			Analysis.p.livedisplay=false;

			Analysis.p.dispwindows=false;
			Analysis.p.save_images=true;

			IJ.log(Analysis.p.wd);
			long Time = new Date().getTime(); //start time

			String [] list = new File(wpath).list();
			if (list==null) {IJ.log("No files in folder"); return;}
			Arrays.sort(list);

			//IJ.log("la");
			int ii=0;
			boolean imgfound=false;
			while (ii<list.length && !imgfound) {
				IJ.log("read"+list[ii]);
				boolean isDir = (new File(wpath+list[ii])).isDirectory();
				if (	!isDir &&
						!list[ii].startsWith(".")&&
						!list[ii].startsWith("Coloc") &&
						!list[ii].startsWith("X_Vesicles")&&
						!list[ii].startsWith("Y_Vesicles")&&
						!list[ii].endsWith("_seg_c1.tif")&&
						!list[ii].endsWith("_seg_c2.tif")&&
						!list[ii].endsWith("_mask_c1.tif")&&
						!list[ii].endsWith("_mask_c2.tif")&&
						!list[ii].endsWith("_coloc.tif")&&
						!list[ii].endsWith(".zip")&&
						(list[ii].endsWith(".tif") || list[ii].endsWith(".tiff") ) 
						){

					ImagePlus img=IJ.openImage(wpath+list[ii]);
					Analysis.p.nchannels=img.getNChannels();
					imgfound=true;

				}
				ii++;
			}


			//IJ.log("nchannels" + Analysis.p.nchannels);

			if(Analysis.p.nchannels==2){
				IJ.log("looking for files at " + wpath);
				//IJ.log("0");
				//IJ.log("sep " + File.separator);
				String [] directrories=  wpath.split("\\"+File.separator);
				//IJ.log("1");
				int nl = directrories.length;
				//IJ.log("2 :" + nl);
				//IJ.log(directrories[nl-1]);
				String savepath=(directrories[nl-1]).replaceAll("\\"+File.separator, ""); 
				//IJ.log("3");
				IJ.log(savepath);
				//				IJ.log("1");
				out  = new PrintWriter(wpath+savepath+"_coloc"+ ".csv");
				out2 = new PrintWriter(wpath+savepath+"_Objectsdata_c1"+ ".csv");
				out3 = new PrintWriter(wpath+savepath+"_Objectsdata_c2"+ ".csv");


				//				out  = new PrintWriter(wpath +"Colocalization"+ Time   + ".csv");
				//				out2 = new PrintWriter(wpath +"X_Vesicles_data"+ Time + ".csv");
				//				out3 = new PrintWriter(wpath +"Y_Vesicles_data"+ Time + ".csv");
				//PrintWriter out = new PrintWriter(dir1.replaceAll("/", "_") + ".csv");
				//IJ.log("files open");

				//				out.print("Min intensity X " + Analysis.p.min_intensity +"," +
				//						"Min intensity Y" + Analysis.p.min_intensityY+"," +
				//						"Min vesicle size" + Analysis.p.minves_size+"," +
				//						"Max vesicle size" + Analysis.p.maxves_size+"," +
				//						"Overlap threshold" + Analysis.p.colocthreshold
				//						);

				out.println();
				out.print("File"+ "," +"Image number" + ","+ "Objects ch X" + "," + "Mean size ch X"  +"," 
						+ "Objects ch Y"+"," + "Mean size ch Y" +"," + "Colocalization X in Y"
						+"," + "Colocalization Y in X"
						+"," + "Mean Y intensity in X objects"
						+"," + "Mean X intensity in Y objects");
				out.println();



				if(Analysis.p.nz>1){
					out2.print("Image number" + "," + "Region in 1"+ "," 
							+ "Size" + "," + "Surface"  + "," +"Intensity" + "," 
							+ "Overlap with 2" +","+ "MColoc size" + ","+ "MColoc Intensity" + "," + "Single Coloc" + ","  + "Coord X"+ "," + "Coord Y"+ "," + "Coord Z");
					out2.println();
					out3.print("Image number" + "," + "Region in 2" +","
							+ "Size" + "," + "Surface" + "," +"Intensity" + ","
							+ "Overlap with 1" +","+ "MColoc size" + ","+ "MColoc Intensity" + "," + "Single Coloc" + ","  + "Coord X"+ "," + "Coord Y"+ "," + "Coord Z");
					out3.println();
				}
				else{
					out2.print("Image number" + "," + "Region in 1"+ "," 
							+ "Size" + "," + "Perimeter" + "," + "Length" + "," +"Intensity" + "," 
							+ "Overlap with 2" +","+ "MColoc size" + ","+ "MColoc Intensity" + "," + "Single Coloc" + ","  + "Coord X"+ "," + "Coord Y"+ "," + "Coord Z");
					out2.println();
					out3.print("Image number" + "," + "Region in 2" +","
							+ "Size" + "," + "Perimeter" + "," + "Length" + "," +"Intensity" + ","
							+ "Overlap with 1" +","+ "MColoc size" + ","+ "MColoc Intensity" + "," + "Single Coloc" + ","  + "Coord X"+ "," + "Coord Y"+ "," + "Coord Z");
					out3.println();		
				}




				out.flush();

			}

			else
			{

				out2 = new PrintWriter(wpath +"Objects_data"+ Time + ".csv");


				out2.print("Image number" + "," + "Region in X"+ "," + "Size" + "," + "Perimeter" + "," + "Length" + "," +
						"Intensity" + ","  + "Coord X"+ "," + "Coord Y"+ "," + "Coord Z");
				out2.println();

				out2.flush();


			}

			for (int i=0; i<list.length; i++) {
				IJ.log("read"+list[i]);
				boolean isDir = (new File(wpath+list[i])).isDirectory();
				if (	!isDir &&
						!list[i].startsWith(".") &&
						!list[i].startsWith("Coloc") &&
						!list[i].startsWith("X_Vesicles")&&
						!list[i].startsWith("Objects_data")&&
						!list[i].startsWith("Y_Vesicles")&&
						!list[i].endsWith("_seg_c1.tif")&&
						!list[i].endsWith("_seg_c2.tif")&&
						!list[i].endsWith("_mask_c1.tif")&&
						!list[i].endsWith("_mask_c2.tif")&&
						!list[i].endsWith("_coloc.tif")&&
						list[i].endsWith(".tif")&&
						!list[i].endsWith(".zip")
						){
					IJ.log("Analyzing " + list[i]+ "... ");
					ImagePlus img=IJ.openImage(wpath+list[i]);
					//IJ.log("opened");
					bcolocheadless(img);
					//IJ.log("done");
				}
			}
			IJ.log("");
			IJ.log("Done");

			String choice1[] = {
					"Automatic", "Medium layer","High layer"};
			String choice2[] = {
					"Poisson", "Gauss"};

			out.print(
					"background removal " + Analysis.p.removebackground + "," +
					"rolling ball window size " + Analysis.p.size_rollingball + "," +
					"stddev PSF xy " + Analysis.p.sigma_gaussian+ "," +
					"stddev PSF z " + Analysis.p.sigma_gaussian/Analysis.p.zcorrec+ "," +
					"lambda prior " + Analysis.p.lreg  + "," +
					"Min intensity " + Analysis.p.min_intensity +"," +
					"subpixel " + Analysis.p.subpixel + "," +
					"Intensity estimation " + choice1[Analysis.p.mode_intensity] + "," +
					"Noise model " + choice2[Analysis.p.noise_model]
					);

			finish();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error headless: " + e.getMessage());
		}
		Analysis.doingbatch=false;
	}

	public void bcolocheadless(ImagePlus img2){
		double Ttime=0;
		long lStartTime = new Date().getTime(); //start time
		
		//Analysis.p.livedisplay=false;

		Analysis.p.blackbackground=ij.Prefs.blackBackground;
		ij.Prefs.blackBackground=false;
		Analysis.p.nchannels=img2.getNChannels();

		//IJ.log("dialog j" + ij.Prefs.useJFileChooser);

		if(Analysis.p.nchannels==2){
			Analysis.load2channels(img2);
		}

		if(Analysis.p.nchannels==1){
			Analysis.load1channel(img2);
		}

		//Analysis.p.dispvoronoi=true;
		//Analysis.p.livedisplay=true;
		if(Analysis.p.mode_voronoi2){
			if(Analysis.p.nz>1){
				Analysis.p.max_nsb=151;
				Analysis.p.interpolation=2;
			}else
			{
				Analysis.p.max_nsb=151;
				Analysis.p.interpolation=4;
			}
		}

		int nni,nnj,nnz;
		nni=Analysis.imgA.getWidth();
		nnj=Analysis.imgA.getHeight();
		nnz=Analysis.imgA.getNSlices();

		Analysis.p.ni=nni;
		Analysis.p.nj=nnj;
		Analysis.p.nz=nnz;

		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;

		//IJ.log("dispcolors" + Analysis.p.dispcolors);
		Analysis.segmentA();			 

		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}


		//IJ.log("imgb :" +list[i+1]);

		if(Analysis.p.nchannels==2){
			Analysis.segmentb();			 

			try {
				Analysis.DoneSignalb.await();
			}catch (InterruptedException ex) {}
		}


		//TODO : why is it needed to reassign p.ni ...??
		Analysis.p.ni=Analysis.imgA.getWidth();
		Analysis.p.nj=Analysis.imgA.getHeight();
		Analysis.p.nz=Analysis.imgA.getNSlices();



		if(Analysis.p.nchannels==2){

			//IJ.log("computemask");
			Analysis.computeOverallMask();
			//IJ.log("1");
			Analysis.regionslistA=Analysis.removeExternalObjects(Analysis.regionslistA);
			//IJ.log("2");
			Analysis.regionslistB=Analysis.removeExternalObjects(Analysis.regionslistB);

			//IJ.log("setriongslabels");
			Analysis.setRegionsLabels(Analysis.regionslistA, Analysis.regionsA);
			Analysis.setRegionsLabels(Analysis.regionslistB, Analysis.regionsB);
			int factor2 =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
			int fz2;
			if(Analysis.p.nz>1)fz2=factor2; else fz2=1;
			//IJ.log("ici");
			MasksDisplay md= new MasksDisplay(Analysis.p.ni*factor2,Analysis.p.nj*factor2,Analysis.p.nz*fz2,Analysis.p.nlevels,Analysis.p.cl,Analysis.p);
			md.displaycoloc(Analysis.regionslistA,Analysis.regionslistB);

			//IJ.log("la");
			if(Analysis.p.dispoutline){
				//IJ.log("disp outline");
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				displayoutline(Analysis.regionsA, Analysis.imagea,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1);
				displayoutline(Analysis.regionsB, Analysis.imageb,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 2);
			}
			if(Analysis.p.dispint){
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				//IJ.log("factor" + factor);
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				displayintensities(Analysis.regionslistA, Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1, Analysis.imagecolor_c1);
				displayintensities(Analysis.regionslistB, Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 2, Analysis.imagecolor_c2);
			}

			//			if(Analysis.p.save_images){
			//				Analysis.setIntensitiesandCenters(Analysis.regionslistA, Analysis.imagea);
			//				Analysis.setIntensitiesandCenters(Analysis.regionslistB, Analysis.imageb);
			//
			//				Analysis.setPerimeter(Analysis.regionslistA,Analysis.regionsA);	
			//				Analysis.setPerimeter(Analysis.regionslistB,Analysis.regionsB);	
			//
			//				if(Analysis.p.nz==1){
			//					Analysis.setlength(Analysis.regionslistA,Analysis.regionsA);
			//					Analysis.setlength(Analysis.regionslistB,Analysis.regionsB);
			//				}
			//			}
			Analysis.na=Analysis.regionslistA.size();
			Analysis.nb=Analysis.regionslistB.size();

			//if display coloc
			//if(Analysis.p.dispcoloc){

			//}

			//IJ.log("a");
			double colocAB=Tools.round(Analysis.colocsegAB(out2, hcount),4);
			//IJ.log("b");
			double colocBA=Tools.round(Analysis.colocsegBA(out3, hcount),4);
			//IJ.log("c");
			double colocA=0;
			double colocB=0;
			//double colocA=Tools.round(Analysis.colocsegA(null),4);
			//IJ.log("d");
			//double colocB=Tools.round(Analysis.colocsegB(null),4);
			//IJ.log("e");

			Analysis.meana=Analysis.meansize(Analysis.regionslistA);
			Analysis.meanb=Analysis.meansize(Analysis.regionslistB);
			//IJ.log("f");
			if(Analysis.p.save_images){
				out.print(img2.getTitle() + "," + hcount +","+ Analysis.na + "," +
						Tools.round(Analysis.meana , 4)  +"," + Analysis.nb +"," + 
						Tools.round(Analysis.meanb , 4) +"," +
						colocAB +"," + 
						colocBA + ","+
						colocA+ ","+
						colocB);
				out.println();
				out.flush();

				Analysis.printobjectsA(out2, hcount);
				Analysis.printobjectsB(out3, hcount);
				out2.flush();
				out3.flush();


			}

			Analysis.doingbatch=false;
			hcount++;

		}


		if(Analysis.p.nchannels==1){

			if(Analysis.p.dispoutline){
				//IJ.log("disp outline");
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				//				long lStartTime = new Date().getTime(); //start time
				displayoutline(Analysis.regionsA, Analysis.imagea,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1);
				//				long lEndTime = new Date().getTime(); //start time
				//				long difference = lEndTime - lStartTime; //check different
				//				IJ.log("Elapsed milliseconds dispoutl: " + difference);
			}

			if(Analysis.p.dispint){
				//	IJ.log("disp int");
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				//	IJ.log("factor" + factor);
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				//				long lStartTime = new Date().getTime(); //start time
				displayintensities(Analysis.regionslistA, Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1, Analysis.imagecolor_c1);
				//				long lEndTime = new Date().getTime(); //start time
				//				long difference = lEndTime - lStartTime; //check different
				//				IJ.log("Elapsed milliseconds dispintsts: " + difference);
			}
			//	Analysis.setRegionsLabels(Analysis.regionslistA, Analysis.regionsA);
			//			long lStartTime = new Date().getTime(); //start time
			//IJ.log("analysis");
			Analysis.na=Analysis.regionslistA.size();
			//IJ.log("intensities");

			//			if(Analysis.p.save_images){
			////				Analysis.setIntensitiesandCenters(Analysis.regionslistA,Analysis.imagea);
			////
			////				Analysis.setPerimeter(Analysis.regionslistA,Analysis.regionsA);	
			//
			////				if(Analysis.p.nz==1){
			////					Analysis.setlength(Analysis.regionslistA,Analysis.regionsA);	
			////				}
			//			}

			//			long lEndTime = new Date().getTime(); //start time
			//			long difference = lEndTime - lStartTime; //check different
			//			IJ.log("Elapsed milliseconds postprocessing: " + difference);
			//IJ.log("todo disp int");


			//			if(Analysis.p.dispoutline){
			//				//IJ.log("disp outline");
			//				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
			//				int fz;
			//				if(Analysis.p.nz>1)fz=factor; else fz=1;
			//				long lStartTime = new Date().getTime(); //start time
			//				displayoutline(Analysis.regionsA, Analysis.imagea,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1);
			//				long lEndTime = new Date().getTime(); //start time
			//				long difference = lEndTime - lStartTime; //check different
			//				IJ.log("Elapsed milliseconds dispoutl: " + difference);
			//			}


			if(Analysis.p.save_images){
				Analysis.printobjects(out2, hcount);
				out2.flush();
			}






			hcount++;

		}
		ij.Prefs.blackBackground=Analysis.p.blackbackground;

		
		long lEndTime = new Date().getTime(); //start time

		long difference = lEndTime - lStartTime; //check different
		Ttime +=difference;
		IJ.log("Total Time : " + Ttime/1000 + "s");

	}

	public void displayoutline(int [][][] regions, double [][][] image, int dz, int di, int dj, int channel){
		ImageStack objS;
		ImagePlus objcts= new ImagePlus();


		//build stack and imageplus for objects
		objS=new ImageStack(di,dj);

		for (int z=0; z<dz; z++){
			byte[] mask_bytes = new byte[di*dj];
			for (int i=0; i<di; i++) {  
				for (int j=0; j<dj; j++) {  
					//mask_bytes[j * di + i]= 0;
					if(regions[z][i][j]> 0)
						mask_bytes[j * di + i]= 0;
					else
						mask_bytes[j * dj + i]=(byte) 255;
				}
			}

			ByteProcessor bp = new ByteProcessor(di,dj);
			bp.setPixels(mask_bytes);
			objS.addSlice("", bp);

		}
		objcts.setStack("Objects",objS);


		//build image in bytes
		ImageStack imgS;
		ImagePlus img= new ImagePlus();


		//build stack and imageplus
		imgS=new ImageStack(Analysis.p.ni,Analysis.p.nj);

		for (int z=0; z<Analysis.p.nz; z++){
			byte[] mask_bytes = new byte[Analysis.p.ni*Analysis.p.nj];
			for (int i=0; i<Analysis.p.ni; i++) {  
				for (int j=0; j<Analysis.p.nj; j++) {  
					mask_bytes[j * Analysis.p.nj + i]=(byte) ((int) 255*image[z][i][j]);
				}
			}

			ByteProcessor bp = new ByteProcessor(Analysis.p.ni,Analysis.p.nj);
			bp.setPixels(mask_bytes);

			imgS.addSlice("", bp);
			//			if(Analysis.p.subpixel)imgS.addSlice("", bp);

		}
		img.setStack("Image",imgS);


		//	img.show();

		//IJ.run(img, "Size...", "width="+di+" height="+dj+" depth="+dz+" constrain interpolation=None");
		//img.show();
		//resize z
		Resizer re = new Resizer();
		img=re.zScale(img, dz, ImageProcessor.NONE);
		//img.duplicate().show();
		ImageStack imgS2=new ImageStack(di,dj);
		for (int z=0; z<dz; z++){
			img.setSliceWithoutUpdate(z+1);
			//img.setProcessor(img.getProcessor().resize(di, dj, false));
			img.getProcessor().setInterpolationMethod(ImageProcessor.NONE);
			imgS2.addSlice("",img.getProcessor().resize(di, dj, false));
		}
		img.setStack(imgS2);
		//img.duplicate().show();



		//		objcts.show();
		//binary processor from byte processor then outline
		//IJ.run(objcts, "Outline", "");
		for (int z=1; z<=dz; z++){
			BinaryProcessor  bip = new BinaryProcessor((ByteProcessor) objcts.getStack().getProcessor(z));
			bip.outline();
			bip.invert();
		}



		//		for (int z=0; z<dz; z++){
		//			
		//			objcts
		//			invert
		//		}
		//		

		//IJ.run(objcts, "Invert", "");
		//	RGBStackMerge merger= new RGBStackMerge();	
		ImagePlus tab []= new ImagePlus [2];
		tab[0]=objcts;tab[1]=img;
		ImagePlus over =RGBStackMerge.mergeChannels(tab, false);
		//		IJ.run(objcts, "Merge Channels...",
		//				"red=*None* green="+
		//						objcts.getTitle()+
		//						" blue=*None* gray=" +
		//						img.getTitle());


		//		ImageStack overS;
		//		ImagePlus over= new ImagePlus();
		//		overS=new ImageStack(di,dj);
		//		for (int z=0; z<dz; z++) {  
		//			img.setSlice(z+1);
		//			objcts.setSlice(z+1);
		//			ColorProcessor cpcoloc= new ColorProcessor(di,dj);
		//			for (int i=0;i<di;i++) {  
		//				for (int j=0;j< dj;j++){ 
		//					int [] colors= new int [3];
		//					colors[0]=objcts.getProcessor().getPixel(i,j);
		//					colors[1]=img.getProcessor().getPixel(i,j);
		//					colors[2]=0;
		//					cpcoloc.putPixel(i, j, colors);
		//				}
		//			}
		//			overS.addSlice("Outlines overlay c" + channel, cpcoloc);
		//
		//		}
		//		over.setStack("Outlines overlay c" +channel, overS);

		if(Analysis.p.dispwindows){
			over.show();
		}



		if (Analysis.p.save_images){
			//IJ.run(over,"RGB Color", "");
			String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_outline_overlay" + "_c"+channel+".zip";
			IJ.saveAs(over, "ZIP", savepath);	
		}

	}



	public void displayintensities(ArrayList<Region> regionslist,int dz, int di, int dj, int channel, int [][][][] imagecolor){
		ImageStack intS;
		ImagePlus intensities= new ImagePlus();

		//int [][][][] imagecolor = new int [dz][di][dj][3];



		//		//IJ.log("ici");
		//		int b0, b1, b2;
		//		b0=(int) Math.min(255, 255*Analysis.p.betaMLEoutdefault);
		//		b1= (int) Math.min(255, 255*Math.sqrt(Analysis.p.betaMLEoutdefault)) ;
		//		b2=(int) Math.min(255, 255*Math.pow(Analysis.p.betaMLEoutdefault,2)) ;
		//		
		//		
		//		//set all to background
		//		for (int z=0; z<dz; z++) {  
		//			for (int i=0;i<di;i++) {  
		//				for (int j=0;j< dj;j++){  
		//					imagecolor[z][i][j][0]=b0 ; //Red
		//					imagecolor[z][i][j][1]= b1 ; //Green
		//					imagecolor[z][i][j][2]= b2 ; //Blue
		//				}
		//			}
		//		}

		//IJ.log("la");
		//set region  pixels 

		//		for (Iterator<Region> it = regionslist.iterator(); it.hasNext();){
		//			Region r = it.next();		
		//
		//			//IJ.log("rvalue " + r.intensity);
		//			//IJ.log("perimeter " + r.perimeter);
		//			//IJ.log("length " + r.length);
		//			int c1= (int) Math.min(255, 255*Math.sqrt(r.intensity)) ; //Green
		//			int c0= (int) Math.min(255, 255*r.intensity) ; //Red
		//			int c2= (int) Math.min(255, 255*Math.pow(r.intensity,2)) ; //Blue
		//
		//
		//			for (Iterator<Pix> it2 = r.pixels.iterator(); it2.hasNext();) {
		//				Pix p = it2.next();
		//				//set correct color
		//				imagecolor[p.pz][p.px][p.py][0]=c0;
		//				imagecolor[p.pz][p.px][p.py][1]=c1;
		//				imagecolor[p.pz][p.px][p.py][2]=c2;
		//				//green
		//			}	
		//
		//		}


		//build stack and imageplus
		intS=new ImageStack(di,dj);
		for (int z=0; z<dz; z++) {  

			ColorProcessor cpcoloc= new ColorProcessor(di,dj);
			for (int i=0;i<di;i++) {  
				for (int j=0;j< dj;j++){  
					cpcoloc.putPixel(i, j, imagecolor[z][i][j]);
				}
			}
			intS.addSlice("Intensities reconstruction c" + channel, cpcoloc);

		}
		intensities.setStack("Intensities reconstruction c" +channel, intS);
		if(Analysis.p.dispwindows){
			intensities.show();
		}

		if (Analysis.p.save_images){
			//IJ.run(intensities,"RGB Color", "");
			String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_intensities" + "_c"+channel+".zip";
			IJ.saveAs(intensities, "ZIP", savepath);			
		}

	}

	public  void finish(){
		if(Analysis.p.save_images){
			if(Analysis.p.nchannels==2){
				out.close();
				out2.close();
				out3.close();
			}
			else
				out2.close();
		}
	}



}
