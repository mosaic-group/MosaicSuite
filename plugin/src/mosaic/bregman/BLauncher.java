package mosaic.bregman;

/*
 * Colocalization analysis class
 */

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

import mosaic.bregman.FindConnectedRegions.Region;
/**
 * squassh analysis launcher
 * creates .csv results files and launches analysis 
 * @author Aurelien Rizk
 *
 */
public class BLauncher 
{
	public  int hcount=0;
	public  String headlesscurrent;
	PrintWriter out;
	PrintWriter out2;
	PrintWriter out3;
	String wpath;
	ImagePlus aImp;
	Tools Tools;
	RScript script;

	/**
	 * launch squassh from folder or file path
	 * @param path folder or file to analyze
	 */
	public BLauncher(String path){
		wpath=path;
		boolean processdirectory = (new File(wpath)).isDirectory();
		if(processdirectory)
			processDirectory();
		else
			processFile();
	}

	/**
	 * launch squassh from opend image
	 * @param aImp_ image to analyze
	 */
	public BLauncher(ImagePlus aImp_){
		wpath = null;
		aImp = aImp_;
		processFile();
	}
	
	/**
	 * process single file
	 *  (creates .csv results file, set paths and launch analysis)
	 */
	public void processFile(){
		try{
			ImagePlus img = null;
			if (wpath != null){
				Analysis.p.wd= (new File(wpath)).getParent() +File.separator;
				img=IJ.openImage(wpath);
			}
			else{
				if (aImp.getFileInfo().directory == ""){
					if (aImp.getOriginalFileInfo() == null || aImp.getOriginalFileInfo().directory == "")
						Analysis.p.wd = null;
					else
						Analysis.p.wd = aImp.getOriginalFileInfo().directory;
				}
				else{
					Analysis.p.wd = aImp.getFileInfo().directory;
				}

				img = aImp;
			}
			
			Analysis.p.nchannels=img.getNChannels();
			Analysis.p.dispwindows=true;

			//save results in .csv files, create files and file headers
			if(Analysis.p.save_images){
				String savepath = null;
				if (wpath != null)
					savepath =  wpath.substring(0,wpath.length()-4);
				else{
					if (savepath == null)
						IJ.error("Error cannot track the image directory");
					savepath = Analysis.p.wd;
				}

				//image with two channels
				if(Analysis.p.nchannels==2){
					out  = new PrintWriter(savepath+"_ImagesData"+ ".csv");
					out2 = new PrintWriter(savepath+"_ObjectsData_c1"+ ".csv");
					out3 = new PrintWriter(savepath+"_ObjectsData_c2"+ ".csv");

					Analysis.p.file1=savepath+"_ObjectsData_c1"+ ".csv";
					Analysis.p.file2=savepath+"_ObjectsData_c2"+ ".csv";
					Analysis.p.file3=savepath+"_ImagesData"+ ".csv";

					
					if(Analysis.p.save_images){
						script = new RScript(
								Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
								Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
								Analysis.p.ch1,Analysis.p.ch2
								);
						script.writeScript();
					}

					out.println();
					out.print("File"+ ";" +"Image ID" + ";"+ "Objects ch1" + ";" + "Mean size in ch1"  +";" + "Mean surface in ch1"  +";"+ "Mean length in ch1"  +";"  
							+ "Objects ch2"+";" + "Mean size in ch2" +";" + "Mean surface in ch2"  +";"+ "Mean length in ch2"  +";"+ "Colocalization ch1 in ch2 (signal based)"
							+";" + "Colocalization ch2 in ch1 (signal based)"
							+";" + "Colocalization ch1 in ch2 (size based)"
							+";" + "Colocalization ch2 in ch1 (size based)"
							+";" + "Colocalization ch1 in ch2 (objects numbers)"
							+";" + "Colocalization ch2 in ch1 (objects numbers)"
							+";" + "Mean ch2 intensity of ch1 objects"
							+";" + "Mean ch1 intensity of ch2 objects"
							+";" + "Pearson correlation"
							+";" + "Pearson correlation inside cell masks"
							);
					out.println();

					
					if(Analysis.p.nz>1){ //two channels 3D stack
						out2.print("Image ID" + ";" + "Object ID"+ ";" 
								+ "Size" + ";" + "Surface"  + ";" + "Length" + ";" +"Intensity" + ";" 
								+ "Overlap with ch2" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity" + ";" + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out2.println();
						out3.print("Image ID" + ";" + "Object ID" +";"
								+ "Size" + ";" + "Surface" + ";" + "Length" + ";" +"Intensity" + ";"
								+ "Overlap with ch1" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out3.println();
					}
					else{ //two channels 2D image
						out2.print("Image ID" + ";" + "Object ID"+ ";" 
								+ "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +"Intensity" + ";" 
								+ "Overlap with ch2" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out2.println();
						out3.print("Image ID" + ";" + "Object ID" +";"
								+ "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +"Intensity" + ";"
								+ "Overlap with ch1" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out3.println();		
					}
					out.flush();
				}//end two channels image
				else{ //one channel image
					out  = new PrintWriter(savepath+"_ImagesData"+ ".csv");
					out.println();
					out.print("File"+ ";" +"Image ID" + ";"+ "Objects ch1" + ";" + "Mean size in ch1"  +";" + "Mean surface in ch1"  +";"+ "Mean length in ch1");
					out.println();
					out2 = new PrintWriter(savepath+"_ObjectsData"+ ".csv");
					Analysis.p.file1=savepath+"_ObjectsData"+ ".csv";
					Analysis.p.file2=null;
					Analysis.p.file3=savepath+"_ImagesData"+ ".csv";
					
					if(Analysis.p.save_images){
						script = new RScript(
								Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
								Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
								Analysis.p.ch1,Analysis.p.ch2
								);
						script.writeScript();
					}
					
					if(Analysis.p.nz>1){//one channel 3D stack
						out2.print("Image ID" + ";" + "Object ID"+ ";" + "Size" + ";" + "Surface" + ";" + "Length" + ";" +  
								"Intensity" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out2.println();
						out2.flush();
					}
					else{//one channel 2D image
						out2.print("Image ID" + ";" + "Object ID"+ ";" + "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +
								"Intensity" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out2.println();
						out2.flush();					
					}
					out2.flush();
				}//end one channel image
			}//end save results in .csv files

			//segment and quantify image
			analyzeImage(img);
			IJ.log("");
			IJ.log("Done");

			//store plugin parameters in .csv file
			if(Analysis.p.save_images){
				String choice1[] = {
						"Automatic", "Low layer", "Medium layer","High layer"};
				String choice2[] = {
						"Poisson", "Gauss"};
				if(out!=null){
					out.println();
					out.print(
							"Parameters:" + " " + 
									"background removal " + " "+ Analysis.p.removebackground  + " " +
									"window size " + Analysis.p.size_rollingball + " " +
									"stddev PSF xy " + " "+ Tools.round(Analysis.p.sigma_gaussian, 5) + " " +
									"stddev PSF z " + " "+ Tools.round(Analysis.p.sigma_gaussian/Analysis.p.zcorrec, 5)+ " " +
									"Regularization " + Analysis.p.lreg  + " " +
									"Min intensity ch1 " + Analysis.p.min_intensity +" " +
									"Min intensity ch2 " + Analysis.p.min_intensityY +" " +
									"subpixel " + Analysis.p.subpixel + " " +
									"Cell mask ch1 " + Analysis.p.usecellmaskX + " " +
									"mask threshold ch1 " + Analysis.p.thresholdcellmask + " " +
									"Cell mask ch2 " + Analysis.p.usecellmaskY + " " +
									"mask threshold ch2 " + Analysis.p.thresholdcellmasky + " " +									
									"Intensity estimation " + choice1[Analysis.p.mode_intensity] + " " +
									"Noise model " + choice2[Analysis.p.noise_model]+ ";"
							);
					out.println();
					out.flush();
				}
				finish();
			}

		}
		catch (Exception e)
		{//Catch exception if any
			System.err.println("Error launcher file processing: " + e.getMessage());
		}

	}

	/**
	 * batch process files in directory
	 *  (create .csv results file, set paths and launch analysis)
	 */
	public void processDirectory(){

		try{
			wpath=wpath + File.separator;
			Analysis.p.wd= wpath;
			Analysis.doingbatch=true;
			Analysis.p.livedisplay=false;
			Analysis.p.dispwindows=false;
			Analysis.p.save_images=true;

			IJ.log(Analysis.p.wd);

			String [] list = new File(wpath).list();
			if (list==null) {IJ.log("No files in folder"); return;}
			Arrays.sort(list);

			int ii=0;
			boolean imgfound=false;
			while (ii<list.length && !imgfound) {
				if(Analysis.p.debug){IJ.log("read"+list[ii]);}
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
						!list[ii].endsWith("_ImageData.tif")&&
						!list[ii].endsWith(".zip")&&
						(list[ii].endsWith(".tif") || list[ii].endsWith(".tiff") ) 
						){

					ImagePlus img=IJ.openImage(wpath+list[ii]);
					Analysis.p.nchannels=img.getNChannels();
					imgfound=true;

				}
				ii++;
			}

			//two channels image
			if(Analysis.p.nchannels==2){
				IJ.log("looking for files at " + wpath);
				String [] directrories=  wpath.split("\\"+File.separator);
				int nl = directrories.length;
				String savepath=(directrories[nl-1]).replaceAll("\\"+File.separator, ""); 
				out  = new PrintWriter(wpath+savepath+"_ImagesData"+ ".csv");
				out2 = new PrintWriter(wpath+savepath+"_ObjectsData_c1"+ ".csv");
				out3 = new PrintWriter(wpath+savepath+"_ObjectsData_c2"+ ".csv");
				Analysis.p.file1=wpath+savepath+"_ObjectsData_c1"+ ".csv";
				Analysis.p.file2=wpath+savepath+"_ObjectsData_c2"+ ".csv";
				Analysis.p.file3=wpath+savepath+"_ImagesData"+ ".csv";
				
				//create R script
				script = new RScript(
						Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
						Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
						Analysis.p.ch1,Analysis.p.ch2
						);
				script.writeScript();


				out.println();
				out.print("File"+ ";" +"Image ID" + ";"+ "Objects ch1" + ";" + "Mean size in ch1"  +";" + "Mean surface in ch1"  +";"+ "Mean length in ch1"  +";"
						+ "Objects ch2"+";" + "Mean size in ch2" +";" + "Mean surface in ch2"  +";"+ "Mean length in ch2"  +";"+ "Colocalization ch1 in ch2 (signal based)"
						+";" + "Colocalization ch2 in ch1 (signal based)"
						+";" + "Colocalization ch1 in ch2 (size based)"
						+";" + "Colocalization ch2 in ch1 (size based)"
						+";" + "Colocalization ch1 in ch2 (objects numbers)"
						+";" + "Colocalization ch2 in ch1 (objects numbers)"
						+";" + "Mean ch2 intensity of ch1 objects"
						+";" + "Mean ch1 intensity of ch2 objects"
						+";" + "Pearson correlation"
						+";" + "Pearson correlation inside cell masks"
						);
				out.println();

				//two channels 3D stack
				if(Analysis.p.nz>1){
					out2.print("Image ID" + ";" + "Object ID"+ ";" 
							+ "Size" + ";" + "Surface"  + ";" + "Length" + ";" +"Intensity" + ";" 
							+ "Overlap with ch2" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out2.println();
					out3.print("Image ID" + ";" + "Object ID" +";"
							+ "Size" + ";" + "Surface" + ";" + "Length" + ";" +"Intensity" + ";"
							+ "Overlap with ch1" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out3.println();
				}
				//two channels 2D image
				else{
					out2.print("Image ID" + ";" + "Object ID"+ ";" 
							+ "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +"Intensity" + ";" 
							+ "Overlap with ch2" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out2.println();
					out3.print("Image ID" + ";" + "Object ID" +";"
							+ "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +"Intensity" + ";"
							+ "Overlap with ch1" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out3.println();		
				}

				out.flush();
			}//end two channels

			//one channel image
			else{

				String [] directrories=  wpath.split("\\"+File.separator);
				int nl = directrories.length;
				String savepath=(directrories[nl-1]).replaceAll("\\"+File.separator, ""); 
				out  = new PrintWriter(wpath+savepath+"_ImagesData"+ ".csv");
				out2 = new PrintWriter(wpath +savepath+"_ObjectsData" + ".csv");
				Analysis.p.file1=wpath+savepath+"_ObjectsData"+ ".csv";
				Analysis.p.file2=null;
				Analysis.p.file3=wpath+savepath+"_ImagesData"+ ".csv";
				//create R script
				script = new RScript(
						Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
						Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
						Analysis.p.ch1,Analysis.p.ch2
						);
				script.writeScript();
				out.println();
				out.print("File"+ ";" +"Image ID" + ";"+ "Objects ch1" + ";" + "Mean size in ch 1" + ";" + "Mean surface in ch1"  +";"+ "Mean length in ch1"  );
				out.println();

				//one channel 3D stack
				if(Analysis.p.nz>1){
					out2.print("Image ID" + ";" + "Object in ch1"+ ";" + "Size" + ";" + "Surface" + ";" + "Length" + ";" +
							"Intensity" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out2.println();
					out2.flush();
				}
				//one channel 2D image
				else{
					out2.print("Image ID" + ";" + "Object in ch1"+ ";" + "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +
							"Intensity" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out2.println();
					out2.flush();					
				}
			}//end one channel

			
			//process image files in directory
			for (int i=0; i<list.length; i++) {
				if(Analysis.p.debug){IJ.log("read"+list[i]);}
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
						!list[i].endsWith("_ImageData.tif")&&
						list[i].endsWith(".tif")&&
						!list[i].endsWith(".zip")
						){
					IJ.log("Analyzing " + list[i]+ "... ");
					ImagePlus img=IJ.openImage(wpath+list[i]);
	
					//segment and quantify image
					analyzeImage(img);

					Runtime.getRuntime().gc();
				}
			}
			IJ.log("");
			IJ.log("Done");

			
			//write plugin parameters in .csv file
			String choice1[] = {
					"Automatic", "Low layer", "Medium layer", "High layer"};
			String choice2[] = {
					"Poisson", "Gauss"};
			if(out!=null){
				out.println();
				out.print(
						"Parameters:" + " " + 
								"background removal " + " "+ Analysis.p.removebackground  + " " +
								"window size " + Analysis.p.size_rollingball + " " +
								"stddev PSF xy " + Tools.round(Analysis.p.sigma_gaussian, 5) + " " +
								"stddev PSF z " + Tools.round(Analysis.p.sigma_gaussian/Analysis.p.zcorrec, 5)+ " " +
								"Regularization " + Analysis.p.lreg  + " " +
								"Min intensity ch1 " + Analysis.p.min_intensity +" " +
								"Min intensity ch2 " + Analysis.p.min_intensityY +" " +
								"subpixel " + Analysis.p.subpixel + " " +
								"Cell mask ch1 " + Analysis.p.usecellmaskX + " " +
								"mask threshold ch1 " + Analysis.p.thresholdcellmask + " " +
								"Cell mask ch2 " + Analysis.p.usecellmaskY + " " +
								"mask threshold ch2 " + Analysis.p.thresholdcellmasky + " " +									
								"Intensity estimation " + choice1[Analysis.p.mode_intensity] + " " +
								"Noise model " + choice2[Analysis.p.noise_model ] + ";"
						);
				out.println();
			}

			finish();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error headless: " + e.getMessage());
		}
		Analysis.doingbatch=false;
	}


	/**
	 * CURRENTLY NOT USED. Computes pearson correlation only
	 * @param img2 Image to analyze
	 */
	public void computePearson(ImagePlus img2){
		double Ttime=0;
		long lStartTime = new Date().getTime(); //start time
		Analysis.p.blackbackground=ij.Prefs.blackBackground;
		ij.Prefs.blackBackground=false;
		Analysis.p.nchannels=img2.getNChannels();
		
		if(Analysis.p.nchannels==2){
			Analysis.load2channels(img2);
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

		double corr_mask, corr, corr_zero;
		double [] temp;
		temp=Analysis.pearson_corr();
		corr=temp[0];
		corr_mask=temp[1];
		corr_zero=temp[2];

		if(out!=null){
			out.print(img2.getTitle() + ";" + Tools.round(corr,3) + ";" + Tools.round(corr_mask,3)+ ";" + Tools.round(corr_zero,3));
			out.println();
			out.flush();
		}

		long lEndTime = new Date().getTime(); //start time
		long difference = lEndTime - lStartTime; //check different
		Ttime +=difference;
		IJ.log("Total Time : " + Ttime/1000 + "s");
	}


	/** 
	 *  Segment and quantify objetcs
	 *  in a 1 or 2 channels image
	 *  
	 *  @param img2 Image to segment and analyse
	 * 
	 */
	public void analyzeImage(ImagePlus img2)
	{
		double Ttime=0;
		long lStartTime = new Date().getTime(); //start time
		Analysis.p.blackbackground=ij.Prefs.blackBackground;
		ij.Prefs.blackBackground=false;
		Analysis.p.nchannels=img2.getNChannels();

		if(Analysis.p.nchannels==2){
			Analysis.load2channels(img2);
		}
		if(Analysis.p.nchannels==1){
			Analysis.load1channel(img2);
		}
		
		if(Analysis.p.mode_voronoi2){
			if(Analysis.p.nz>1){
				Analysis.p.max_nsb=151;
				Analysis.p.interpolation=2;
			}
			else{
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
		//creates Tools with image dimension
		Tools= new Tools(nni, nnj, nnz);
		Analysis.Tools=Tools;
		//computes pearson correlation in whole image
		double corr_raw=0;
		double [] temp;
		if(Analysis.p.nchannels==2 && Analysis.p.save_images){
			temp=Analysis.pearson_corr();
			corr_raw=temp[0];
		}
		
		//start segmentation of first channel
		Analysis.segmentA();			 
		
		//wait until segmentation is done
		try{
			Analysis.DoneSignala.await();
		}catch (InterruptedException ex) {}

		//start segpmentation of second channel
		if(Analysis.p.nchannels==2){
			Analysis.segmentb();			 
			//wait until segmentation is done
			try {
				Analysis.DoneSignalb.await();
			}catch (InterruptedException ex) {}
		}
		//bug if p.ni not reassigned
		Analysis.p.ni=Analysis.imgA.getWidth();
		Analysis.p.nj=Analysis.imgA.getHeight();
		Analysis.p.nz=Analysis.imgA.getNSlices();

		//quantify two channels image
		if(Analysis.p.nchannels==2){
			Analysis.computeOverallMask();
			Analysis.regionslistA=Analysis.removeExternalObjects(Analysis.regionslistA);
			Analysis.regionslistB=Analysis.removeExternalObjects(Analysis.regionslistB);
			Analysis.setRegionsLabels(Analysis.regionslistA, Analysis.regionsA);
			Analysis.setRegionsLabels(Analysis.regionslistB, Analysis.regionsB);

			int factor2 =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
			int fz2;
			if(Analysis.p.nz>1)fz2=factor2; else fz2=1;
			MasksDisplay md= new MasksDisplay(Analysis.p.ni*factor2,Analysis.p.nj*factor2,Analysis.p.nz*fz2,Analysis.p.nlevels,Analysis.p.cl,Analysis.p);
			md.displaycoloc(Analysis.regionslistA,Analysis.regionslistB);

			if(Analysis.p.dispoutline){
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				displayOutline(Analysis.regionsA, Analysis.imagea,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1);
				displayOutline(Analysis.regionsB, Analysis.imageb,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 2);
			}
			
			if(Analysis.p.dispint){
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				displayIntensities(Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1, Analysis.imagecolor_c1);
				displayIntensities(Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 2, Analysis.imagecolor_c2);
			}

			Analysis.na=Analysis.regionslistA.size();//number of objects in channel A
			Analysis.nb=Analysis.regionslistB.size();//numbers of objects in channel B

			double colocAB=Tools.round(Analysis.colocsegAB(out2, hcount),4); // C_signal A inside B
			double colocABnumber = Tools.round(Analysis.colocsegABnumber(),4); // C_number A inside B
			double colocABsize = Tools.round(Analysis.colocsegABsize(out2, hcount),4); // C_size A inside B
			double colocBA=Tools.round(Analysis.colocsegBA(out3, hcount),4); // C_signal B inside A
			double colocBAnumber = Tools.round(Analysis.colocsegBAnumber(),4); // C_number B inside A
			double colocBAsize=Tools.round(Analysis.colocsegBAsize(out3, hcount),4); // C_size B inside A
			double colocA=Tools.round(Analysis.colocsegA(null),4); //mean B intensity of objects in A
			double colocB=Tools.round(Analysis.colocsegB(null),4); //mean A intensity of objects in B

			//mean sizes (volume (3D) or surface (2D))
			Analysis.meana=Analysis.meansize(Analysis.regionslistA);
			Analysis.meanb=Analysis.meansize(Analysis.regionslistB);
			//mean surface(surface (3D) or perimeter(2D))
			double meanSA= Analysis.meansurface(Analysis.regionslistA);
			double meanSB= Analysis.meansurface(Analysis.regionslistB);
			//mean objects lenghts
			double meanLA= Analysis.meanlength(Analysis.regionslistA);
			double meanLB= Analysis.meanlength(Analysis.regionslistB);
			//display C_signal
			IJ.log("Colocalization ch1 in ch2: " +colocAB);
			IJ.log("Colocalization ch2 in ch1: " +colocBA);

			if(Analysis.p.save_images){
				//compute pearson correlation in cell mask
				double corr_mask;
				temp=Analysis.pearson_corr();
				corr_mask=temp[1];
				//write image mean results
				out.print(img2.getTitle() + ";" + hcount +";"+ Analysis.na + ";" +
						Tools.round(Analysis.meana , 4)  +";" + 
						Tools.round(meanSA , 4)  +";" +
						Tools.round(meanLA , 4)  +";" +
						+ Analysis.nb +";" + 
						Tools.round(Analysis.meanb , 4) +";" +
						Tools.round(meanSB , 4)  +";" +
						Tools.round(meanLB , 4)  +";" +
						colocAB +";" + 
						colocBA + ";"+
						colocABsize +";" + 
						colocBAsize + ";"+
						colocABnumber +";" + 
						colocBAnumber + ";"+
						colocA+ ";"+
						colocB+ ";"+
						Tools.round(corr_raw, 4) +";"+
						Tools.round(corr_mask, 4)
						);
				out.println();
				out.flush();

				//write channel A and channel B objects properties
				Analysis.printobjectsA(out2, hcount);
				Analysis.printobjectsB(out3, hcount);
				out2.flush();
				out3.flush();
			}
			Analysis.doingbatch=false;
			hcount++;
		}//end two channels

		if(Analysis.p.nchannels==1){
			if(Analysis.p.dispoutline){
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				displayOutline(Analysis.regionsA, Analysis.imagea,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1);
			}
			if(Analysis.p.dispint){
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				displayIntensities(Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1, Analysis.imagecolor_c1);
			}

			Analysis.na=Analysis.regionslistA.size();//number of objects in A
			Analysis.meana=Analysis.meansize(Analysis.regionslistA);//mean objects size			
			double meanSA= Analysis.meansurface(Analysis.regionslistA);//mean objetcs surface
			double meanLA= Analysis.meanlength(Analysis.regionslistA);//mean objects lenghts

			if(Analysis.p.save_images){
				//write mean object properties
				if(out!=null){
					out.print(img2.getTitle() + ";" + hcount +";"+ Analysis.na + ";" +
							Tools.round(Analysis.meana , 4)+";"+
							Tools.round(meanSA , 4)+";"+
							Tools.round(meanLA , 4)
							);
					out.println();
					out.flush();
				}
				//write object properties
				Analysis.printobjects(out2, hcount);
				out2.flush();
			}
			hcount++;
		}
		//set blackbacground parameter back
		ij.Prefs.blackBackground=Analysis.p.blackbackground;

		long lEndTime = new Date().getTime(); //start time
		long difference = lEndTime - lStartTime; //check different
		Ttime +=difference;
		IJ.log("Total Time : " + Ttime/1000 + "s");
	}

	/**
	 * Displays overlay of original image and segmented objects outlines
	 * @param regions segmented objects labels image
	 * @param image original image
	 * @param dz Z dimension
	 * @param di I dimension
	 * @param dj J dimension
	 * @param channel channel number (>=1)
	 */
	public void displayOutline(short [][][] regions, double [][][] image, int dz, int di, int dj, int channel){
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
						mask_bytes[j * di + i]=(byte) 255;
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
					mask_bytes[j * Analysis.p.ni + i]=(byte) ((int) 255*image[z][i][j]);
				}
			}
			ByteProcessor bp = new ByteProcessor(Analysis.p.ni,Analysis.p.nj);
			bp.setPixels(mask_bytes);
			imgS.addSlice("", bp);
		}
		img.setStack("Image",imgS);

		//resize z
		Resizer re = new Resizer();
		img=re.zScale(img, dz, ImageProcessor.NONE);
		ImageStack imgS2=new ImageStack(di,dj);
		for (int z=0; z<dz; z++){
			img.setSliceWithoutUpdate(z+1);
			img.getProcessor().setInterpolationMethod(ImageProcessor.NONE);
			imgS2.addSlice("",img.getProcessor().resize(di, dj, false));
		}
		img.setStack(imgS2);
		for (int z=1; z<=dz; z++){
			BinaryProcessor  bip = new BinaryProcessor((ByteProcessor) objcts.getStack().getProcessor(z));
			bip.outline();
			bip.invert();
		}

		ImagePlus tab []= new ImagePlus [2];
		tab[0]=objcts;tab[1]=img;
		ImagePlus over =RGBStackMerge.mergeChannels(tab, false);

		if(Analysis.p.dispwindows){
			over.setTitle("Objects outlines, channel " + channel);
			over.show();
			if(channel==1)
				GenericGUI.setimagelocation(1180,30,over);
			if(channel==2)
				GenericGUI.setimagelocation(1180,610,over);
		}

		if (Analysis.p.save_images){
			String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_outline_overlay" + "_c"+channel+".zip";
			IJ.saveAs(over, "ZIP", savepath);	
		}

	}

	/**
	 * Displays intensity heatmap of segmented objects
	 * @param dz Z dimension
	 * @param di I dimension
	 * @param dj J dimension
	 * @param channel channel number (>=1)
	 * @param imagecolor heatmap image (already computed in ImagePatches and ObjectProperties)
	 */
	public void displayIntensities(int dz, int di, int dj, int channel, byte [] imagecolor){
		ImageStack intS;
		ImagePlus intensities= new ImagePlus();

		//build stack and imageplus
		intS=new ImageStack(di,dj);
		for (int z=0; z<dz; z++) {  
			int [] tabt= new int [3];

			ColorProcessor cpcoloc= new ColorProcessor(di,dj);
			for (int i=0;i<di;i++) {
				int t=z*di*dj*3+i*dj*3;
				for (int j=0;j< dj;j++){
					tabt[0]=imagecolor[t+j*3+0] & 0xFF;
					tabt[1]=imagecolor[t+j*3+1] & 0xFF;
					tabt[2]=imagecolor[t+j*3+2] & 0xFF;
					cpcoloc.putPixel(i, j, tabt);
				}
			}
			intS.addSlice("Intensities reconstruction, channel " + channel, cpcoloc);

		}
		intensities.setStack("Intensities reconstruction, channel " +channel, intS);
		if(Analysis.p.dispwindows){
			intensities.show();
			if(channel==1)
				GenericGUI.setimagelocation(1190,40,intensities);
			if(channel==2)
				GenericGUI.setimagelocation(1190,620,intensities);
		}

		if (Analysis.p.save_images){
			String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_intensities" + "_c"+channel+".zip";
			IJ.saveAs(intensities, "ZIP", savepath);			
		}

	}

	/**
	 * Closes .csv output files
	 */
	public  void finish(){
		if(Analysis.p.save_images){
			if(Analysis.p.nchannels==2){
				out.close();
				out2.close();
				out3.close();
			}
			else
			{
				if(out!=null) out.close();
				out2.close();
			}
		}
	}



}
