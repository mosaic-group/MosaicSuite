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


	public BLauncher(String path)
	{
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

	public BLauncher(ImagePlus aImp_)
	{
		wpath = null;
		aImp = aImp_;
		Headless_file();
		
		
	}
	
	public void Headless_file()
	{
		try
		{
			ImagePlus img = null;
			if (wpath != null)
			{
				Analysis.p.wd= (new File(wpath)).getParent() +File.separator;
				img=IJ.openImage(wpath);
			}
			else
			{
				
				if (aImp.getFileInfo().directory == "")
				{
					if (aImp.getOriginalFileInfo() == null || aImp.getOriginalFileInfo().directory == "")
					{Analysis.p.wd = null;}
					else {Analysis.p.wd = aImp.getOriginalFileInfo().directory;}
				}
				else
				{
					Analysis.p.wd = aImp.getFileInfo().directory;
				}
				
				
				img = aImp;
			}
			
			Analysis.p.nchannels=img.getNChannels();
			Analysis.p.dispwindows=true;


			if(Analysis.p.save_images)
			{
				String savepath = null;
				//IJ.log(wpath);
				if (wpath != null)
					savepath =  wpath.substring(0,wpath.length()-4);
				else
				{
					if (savepath == null)
						IJ.error("Error cannot track the image directory");
					savepath = Analysis.p.wd;
				}
				//IJ.log(savepath);


				if(Analysis.p.nchannels==2)
				{
					//IJ.log("looking for files at " + wpath);
					out  = new PrintWriter(savepath+"_ImagesData"+ ".csv");
					//out2 = new PrintWriter(savepath+"_Xdata"+ ".csv");
					out2 = new PrintWriter(savepath+"_ObjectsData_c1"+ ".csv");
					out3 = new PrintWriter(savepath+"_ObjectsData_c2"+ ".csv");
					
					Analysis.p.file1=savepath+"_ObjectsData_c1"+ ".csv";
					Analysis.p.file2=savepath+"_ObjectsData_c2"+ ".csv";
					Analysis.p.file3=savepath+"_ImagesData"+ ".csv";
					if(Analysis.p.save_images)
					{
						script = new RScript(
								Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
								Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
								Analysis.p.ch1,Analysis.p.ch2
								);
						script.writeScript();
					}
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





					if(Analysis.p.nz>1)
					{
						out2.print("Image ID" + ";" + "Object ID"+ ";" 
								+ "Size" + ";" + "Surface"  + ";" + "Length" + ";" +"Intensity" + ";" 
								+ "Overlap with ch2" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity" + ";" + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out2.println();
						out3.print("Image ID" + ";" + "Object ID" +";"
								+ "Size" + ";" + "Surface" + ";" + "Length" + ";" +"Intensity" + ";"
								+ "Overlap with ch1" +";"+ "Coloc object size" + ";"+ "Coloc object intensity" + ";" + "Single Coloc" + ";" + "Coloc image intensity"+ ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out3.println();
					}
					else
					{
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
				}
				else
				{


					out  = new PrintWriter(savepath+"_ImagesData"+ ".csv");
					out.println();
					out.print("File"+ ";" +"Image ID" + ";"+ "Objects ch1" + ";" + "Mean size in ch1"  +";" + "Mean surface in ch1"  +";"+ "Mean length in ch1");
					out.println();


					out2 = new PrintWriter(savepath+"_ObjectsData"+ ".csv");

					Analysis.p.file1=savepath+"_ObjectsData"+ ".csv";
					Analysis.p.file2=null;
					Analysis.p.file3=savepath+"_ImagesData"+ ".csv";
					if(Analysis.p.save_images)
					{
						script = new RScript(
								Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
								Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
								Analysis.p.ch1,Analysis.p.ch2
								);
						script.writeScript();
					}
					
					if(Analysis.p.nz>1){
						out2.print("Image ID" + ";" + "Object ID"+ ";" + "Size" + ";" + "Surface" + ";" + "Length" + ";" +  
								"Intensity" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out2.println();
						out2.flush();
					}
					else{
						out2.print("Image ID" + ";" + "Object ID"+ ";" + "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +
								"Intensity" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out2.println();
						out2.flush();					
					}

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
			//long Time = new Date().getTime(); //start time

			String [] list = new File(wpath).list();
			if (list==null) {IJ.log("No files in folder"); return;}
			Arrays.sort(list);

			//IJ.log("la");
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
				//IJ.log(savepath);
				//				IJ.log("1");
				out  = new PrintWriter(wpath+savepath+"_ImagesData"+ ".csv");
				out2 = new PrintWriter(wpath+savepath+"_ObjectsData_c1"+ ".csv");
				out3 = new PrintWriter(wpath+savepath+"_ObjectsData_c2"+ ".csv");

				Analysis.p.file1=wpath+savepath+"_ObjectsData_c1"+ ".csv";
				Analysis.p.file2=wpath+savepath+"_ObjectsData_c2"+ ".csv";
				Analysis.p.file3=wpath+savepath+"_ImagesData"+ ".csv";
				script = new RScript(
						Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
						Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
						Analysis.p.ch1,Analysis.p.ch2
						);
				script.writeScript();

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

			}

			else
			{

				String [] directrories=  wpath.split("\\"+File.separator);
				int nl = directrories.length;
				String savepath=(directrories[nl-1]).replaceAll("\\"+File.separator, ""); 
				out  = new PrintWriter(wpath+savepath+"_ImagesData"+ ".csv");
				out2 = new PrintWriter(wpath +savepath+"_ObjectsData" + ".csv");

				Analysis.p.file1=wpath+savepath+"_ObjectsData"+ ".csv";
				Analysis.p.file2=null;
				Analysis.p.file3=wpath+savepath+"_ImagesData"+ ".csv";
				script = new RScript(
						Analysis.p.wd, Analysis.p.file1, Analysis.p.file2, Analysis.p.file3,
						Analysis.p.nbconditions, Analysis.p.nbimages, Analysis.p.groupnames,
						Analysis.p.ch1,Analysis.p.ch2
						);
				script.writeScript();
				
				out.println();
				out.print("File"+ ";" +"Image ID" + ";"+ "Objects ch1" + ";" + "Mean size in ch 1" + ";" + "Mean surface in ch1"  +";"+ "Mean length in ch1"  );
				out.println();






				if(Analysis.p.nz>1){
					out2.print("Image ID" + ";" + "Object in ch1"+ ";" + "Size" + ";" + "Surface" + ";" + "Length" + ";" +
							"Intensity" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out2.println();
					out2.flush();
				}
				else{
					out2.print("Image ID" + ";" + "Object in ch1"+ ";" + "Size" + ";" + "Perimeter" + ";" + "Length" + ";" +
							"Intensity" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
					out2.println();
					out2.flush();					
				}


			}

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
					//IJ.log("opened");

					if(Analysis.p.pearson)
						bcolocheadless_pearson(img);
					else
						bcolocheadless(img);
					//IJ.log("done");

					Runtime.getRuntime().gc();
				}
			}
			IJ.log("");
			IJ.log("Done");

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


	public void bcolocheadless_pearson(ImagePlus img2){
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


	/* 
	 *  It segment the image and give co-localization analysis result
	 *  for a 2 channel image
	 *  
	 *  @param img2 Image to segment and analyse
	 * 
	 */
	
	public void bcolocheadless(ImagePlus img2)
	{
		double Ttime=0;
		long lStartTime = new Date().getTime(); //start time

		//Analysis.p.livedisplay=false;

		Analysis.p.blackbackground=ij.Prefs.blackBackground;
		ij.Prefs.blackBackground=false;
		Analysis.p.nchannels=img2.getNChannels();

		//IJ.log("dialog j" + ij.Prefs.useJFileChooser);

		if(Analysis.p.nchannels==2)
		{
			Analysis.load2channels(img2);
		}

		if(Analysis.p.nchannels==1)
		{
			Analysis.load1channel(img2);
		}

		//Analysis.p.dispvoronoi=true;
		//Analysis.p.livedisplay=true;
		if(Analysis.p.mode_voronoi2)
		{
			if(Analysis.p.nz>1)
			{
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


		double corr_raw=0;
		double [] temp;
		if(Analysis.p.nchannels==2 && Analysis.p.save_images){
			temp=Analysis.pearson_corr();
			corr_raw=temp[0];
		}



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

			MasksDisplay md= new MasksDisplay(Analysis.p.ni*factor2,Analysis.p.nj*factor2,Analysis.p.nz*fz2,Analysis.p.nlevels,Analysis.p.cl,Analysis.p);
			md.displaycoloc(Analysis.regionslistA,Analysis.regionslistB);


			if(Analysis.p.dispoutline)
			{
				//IJ.log("disp outline");
				int factor =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
				int fz;
				if(Analysis.p.nz>1)fz=factor; else fz=1;
				displayoutline(Analysis.regionsA, Analysis.imagea,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 1);
				displayoutline(Analysis.regionsB, Analysis.imageb,Analysis.p.nz*fz,Analysis.p.ni*factor,Analysis.p.nj*factor, 2);
			}
			if(Analysis.p.dispint)
			{
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
			//IJ.log("na");
			Analysis.na=Analysis.regionslistA.size();
			Analysis.nb=Analysis.regionslistB.size();

			//if display coloc
			//if(Analysis.p.dispcoloc){

			//}


			double colocAB=Tools.round(Analysis.colocsegAB(out2, hcount),4);

			double colocABnumber = Tools.round(Analysis.colocsegABnumber(),4);
			
			double colocABsize = Tools.round(Analysis.colocsegABsize(out2, hcount),4);

			double colocBA=Tools.round(Analysis.colocsegBA(out3, hcount),4);

			double colocBAnumber = Tools.round(Analysis.colocsegBAnumber(),4);

			double colocBAsize=Tools.round(Analysis.colocsegBAsize(out3, hcount),4);

			//double colocA=0;
			//double colocB=0;
			double colocA=Tools.round(Analysis.colocsegA(null),4);

			double colocB=Tools.round(Analysis.colocsegB(null),4);
			//IJ.log("e");

			Analysis.meana=Analysis.meansize(Analysis.regionslistA);
			Analysis.meanb=Analysis.meansize(Analysis.regionslistB);

			double meanSA= Analysis.meansurface(Analysis.regionslistA);
			double meanSB= Analysis.meansurface(Analysis.regionslistB);

			double meanLA= Analysis.meanlength(Analysis.regionslistA);
			double meanLB= Analysis.meanlength(Analysis.regionslistB);






			//IJ.log("f");

			//if(Analysis.p.dispwindows){
			IJ.log("Colocalization ch1 in ch2: " +colocAB);
			IJ.log("Colocalization ch2 in ch1: " +colocBA);
			//}
			if(Analysis.p.save_images){
				//pearson after background reduction
				double corr_mask;
				temp=Analysis.pearson_corr();
				corr_mask=temp[1];



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





			//IJ.log("mean size");
			Analysis.meana=Analysis.meansize(Analysis.regionslistA);

			double meanSA= Analysis.meansurface(Analysis.regionslistA);			
			double meanLA= Analysis.meanlength(Analysis.regionslistA);



			//IJ.log("save");
			if(Analysis.p.save_images){

				if(out!=null){
					out.print(img2.getTitle() + ";" + hcount +";"+ Analysis.na + ";" +
							Tools.round(Analysis.meana , 4)+";"+
							Tools.round(meanSA , 4)+";"+
							Tools.round(meanLA , 4)
							);
					out.println();
					out.flush();
				}

				//IJ.log("print objects");
				Analysis.printobjects(out2, hcount);
				//IJ.log("print objects done");
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

	public void displayoutline(short [][][] regions, double [][][] image, int dz, int di, int dj, int channel){
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
			over.setTitle("Objects outlines, channel " + channel);
			over.show();
			if(channel==1)
				GenericGUI.setimagelocation(1180,30,over);
			if(channel==2)
				GenericGUI.setimagelocation(1180,610,over);
		}



		if (Analysis.p.save_images){
			//IJ.run(over,"RGB Color", "");
			String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_outline_overlay" + "_c"+channel+".zip";
			IJ.saveAs(over, "ZIP", savepath);	
		}

	}



	public void displayintensities(ArrayList<Region> regionslist,int dz, int di, int dj, int channel, byte [] imagecolor){
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
			{
				if(out!=null) out.close();
				out2.close();
			}
		}
	}



}
