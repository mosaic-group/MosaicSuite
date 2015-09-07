package mosaic.core.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;

import java.awt.Choice;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mosaic.bregman.output.Region3DColocRScript;
import mosaic.core.GUI.ChooseGUI;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.ipc.MetaInfo;
import mosaic.core.utils.MosaicUtils.ToARGB;
import mosaic.plugins.BregmanGLM_Batch;
import mosaic.test.framework.SystemOperations;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

class FloatToARGB implements ToARGB
{
	double min = 0.0;
	double max = 255;
	
	@Override
	public ARGBType toARGB(Object data)
	{
		ARGBType t = new ARGBType();
		
		float td = 0.0f;
		td = (float) (255*(((RealType<?>)data).getRealFloat()-min)/max);
		t.set(ARGBType.rgba(td, td, td, 255.0f));
		
		return t;
	}

	@Override
	public void setMinMax(double min, double max) 
	{
		this.min = min;
		this.max = max;
	}
}

class IntToARGB implements ToARGB
{
	double min = 0.0;
	double max = 255;
	
	@Override
	public ARGBType toARGB(Object data)
	{
		ARGBType t = new ARGBType();
		
		int td = 0;
		td = (int) (255*(((IntegerType<?>)data).getInteger()-min)/max);
		t.set(ARGBType.rgba(td, td, td, 255));
		
		return t;
	}
	
	@Override
	public void setMinMax(double min, double max) 
	{
		this.min = min;
		this.max = max;
	}
}

class ARGBToARGB implements ToARGB
{
	double min = 0.0;
	double max = 255;
	
	@Override
	public ARGBType toARGB(Object data)
	{				
		return (ARGBType) data;
	}
	
	@Override
	public void setMinMax(double min, double max) 
	{
		this.min = min;
		this.max = max;
	}
}

public class MosaicUtils 
{
	public class SegmentationInfo
	{
		public File RegionList;
		public File RegionMask;
	}
	
	//////////////////////////////////// Procedures for draw //////////////////
	
	/////// Conversion to ARGB from different Type ////////////////////////////
	
	public interface ToARGB
	{
		void setMinMax(double min, double max);
		ARGBType toARGB(Object data);
	}
	
	/**
	 * 
	 * From an Object return a generic converter to ARGB type
	 * Useful when you have a Generic T, you can use in the following way
	 * 
	 * 
	 * T data;
	 * ToARGB conv = getConvertion(data)
	 * 
	 * 
	 * conv.toARGB(data);
	 * 
	 * @param data
	 * @return
	 */
	
	static public ToARGB getConversion(Object data)
	{
		if (data instanceof RealType)
		{
			return new FloatToARGB();
		}
		else if (data instanceof IntegerType)
		{
			return new IntToARGB();
		}
		else if (data instanceof ARGBType)
		{
			return new ARGBToARGB();
		}
		return null;
	}
	
	/**
	 * 
	 * From an Object return a generic converter to ARGB type
	 * and at the same time set a re-normalization
	 * Useful when you have a Generic T, you can use in the following way
	 * 
	 * 
	 * T data;
	 * ToARGB conv = getConvertion(data,cursor<T>)
	 * 
	 * 
	 * conv.toARGB(data);
	 * 
	 * @param data
	 * @return
	 */
	
	@SuppressWarnings("unchecked")
	static public <T extends RealType<T>> ToARGB getConversion(Object data, Cursor<T> crs)
	{
		ToARGB conv = null;
		if (data instanceof RealType)
		{
			conv = new FloatToARGB();
		}
		else if (data instanceof IntegerType)
		{
			conv = new IntToARGB();
		}
		else if (data instanceof ARGBType)
		{
			conv = new ARGBToARGB();
		}
		
        // Get the min and max
        
        T min = null;
        T max = null;
        crs.next();
		try {
			min = (T) crs.get().getClass().newInstance();
	        max = (T) crs.get().getClass().newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
        MosaicUtils.getMinMax(crs, min,max);
    	
        // get conversion;
        
        conv.setMinMax(min.getRealDouble(), max.getRealDouble());
		
		return conv;
	}
	
	
	/**
	 * 
	 * Filter out from Possible candidate file the one chosen by the user
	 * If only one file present nothing appear
	 * 
	 * @param PossibleFile Vector of possible File
	 * @return Chosen file
	 */
	
	static private File filter_possible(Vector<File> PossibleFile)
	{
		if (PossibleFile == null)
			return null;
		
		if (PossibleFile.size() > 1)
		{
			// Ask user to choose
			
			ChooseGUI cg = new ChooseGUI();
			
			return cg.choose("Choose segmentation","Found multiple segmentations", PossibleFile);
		}
		else
		{
			if (PossibleFile.size() == 1)
				return PossibleFile.get(0);
			else
				return null;
		}
	}
	
	
	/**
	 * 
	 * Check if there are segmentation information for the image
	 * 
	 * @param Image
	 * 
	 */

	static public boolean checkSegmentationInfo(ImagePlus aImp, String plugin)
	{
		String Folder = MosaicUtils.ValidFolderFromImage(aImp);
		Segmentation[] sg = MosaicUtils.getSegmentationPluginsClasses();
		
//		MosaicUtils MS = new MosaicUtils();
//		SegmentationInfo sI = MS.new SegmentationInfo();
		
		// Get infos from possible segmentation
		
		for (int i = 0 ; i < sg.length ; i++)
		{
			String sR[] = sg[i].getRegionList(aImp);
			for (int j = 0 ; j < sR.length ; j++)
			{
				File fR = new File(Folder + sR[j]);
				
				if (fR.exists())
				{
					return true;
				}
			}			
			
			// Check if there are Jobs directory
			// if there are open a job selector
			// and search inside the selected directory
			//
			
			String [] jb = ClusterSession.getJobDirectories(0, Folder);
			
			// check if the jobID and filename match
			
			for (int k = 0 ; k < jb.length ; k++)
			{
				// Filename
				
				String[] fl = MosaicUtils.readAndSplit(jb[k] + File.separator + "JobID");
				
				
				if (fl[2].contains(aImp.getTitle()) && sg[i].getName().equals(fl[3]))
				{
					if (plugin == null)
					{
						return true;
					}
					else
					{
						if (sg[i].getName().equals(plugin))
						{
							return true;
						}
					}
				}
			}
		}

		return false;
	} 
	
	/**
	 * 
	 * Get if there are segmentation information for the image
	 * 
	 * @param Image
	 * 
	 */

	static public SegmentationInfo getSegmentationInfo(ImagePlus aImp)
	{
		String Folder = MosaicUtils.ValidFolderFromImage(aImp);
		Segmentation[] sg = MosaicUtils.getSegmentationPluginsClasses();
		
		Vector<File> PossibleFile = new Vector<File>();
		
		MosaicUtils MS = new MosaicUtils();
		SegmentationInfo sI = MS.new SegmentationInfo();
		
		// Get infos from possible segmentation
		
		for (int i = 0 ; i < sg.length ; i++)
		{
			String sR[] = sg[i].getRegionList(aImp);
			for (int j = 0 ; j < sR.length ; j++)
			{
				File fR = new File(Folder + sR[j]);
				
				if (fR.exists())
				{
					PossibleFile.add(fR);
				}
			}			
			
			// Check if there are Jobs directory
			// if there are open a job selector
			// and search inside the selected directory
			//
			
			String [] jb = ClusterSession.getJobDirectories(0, Folder);
			
			for (int k = 0 ; k < jb.length ; k++)
			{
				// check if the jobID and filename match
					
				String[] fl = MosaicUtils.readAndSplit(jb[k] + File.separator + "JobID");
					
					
				if (fl[2].contains(aImp.getTitle()) && sg[i].getName().equals(fl[3]))
				{
					// Get the region list
						
					sR = sg[i].getRegionList(aImp);
					for (int j = 0 ; j < sR.length ; j++)
					{
						File fR = new File(jb[k] + File.separator + sR[j]);
						if (fR.exists() == true)
							PossibleFile.add(fR);
					}
				}
			}
			
			sI.RegionList = filter_possible(PossibleFile);
			
			// if not segmentation choosen
			
			if (sI.RegionList == null)
				return null;
			
			PossibleFile.clear();
			
			String dir = sI.RegionList.getParent();
			
			String sM[] = sg[i].getMask(aImp);
			for (int j = 0 ; j < sM.length ; j++)
			{
				File fM = new File(dir + File.separator + sM[j]);
			
				if (fM.exists())
				{
					PossibleFile.add(fM);
				}
			}

			sI.RegionMask = filter_possible(PossibleFile);
		}

		return sI;
	} 
	
	/**
	 * 
	 * Get segmentation classes
	 * 
	 * @return an array of the classes
	 */
	
	static public Segmentation[] getSegmentationPluginsClasses()
	{
		Segmentation[] sg = new Segmentation[1];
		
		sg[0] = new BregmanGLM_Batch();
		
		return sg;
	}
	
	
	/**
	 * 
	 * This function merge the frames of the image a2 into a1
	 * 
	 * @param a1 Image a1
	 * @param a2 Image a2
	 * 
	 */
	
	static public void MergeFrames(ImagePlus a1, ImagePlus a2)
	{	
		// If a1 does not have an imageStack set it to a2 and return
		if (a1.getImageStack().getSize() == 0)
		{
			a1.setStack("Merge frames", a2.getImageStack().duplicate());
			a1.setDimensions(a2.getNChannels(), a2.getNSlices(), a2.getNFrames());
			return;
		}
		
		// merge slices channels frames
		
		int hcount = a2.getNFrames() + a1.getNFrames();
		for (int k = 1 ; k <= a2.getNFrames() ; k++)
		{
			for (int j = 1 ; j <= a2.getNSlices() ; j++)
			{
				for (int i = 1 ; i <= a2.getNChannels() ; i++)
				{
					a2.setPosition(i, j, k);
					a1.getImageStack().addSlice("", a2.getChannelProcessor().getPixels());
				}
			}
		}
		a1.setDimensions(a2.getNChannels(), a2.getNSlices(), hcount);
	}
	
	/**
	 * 
	 * Read a file and split the String by space
	 * 
	 * @param file
	 * @return values array of String
	 */
	
	public static String[] readAndSplit(String file)
	{
	     //Z means: "The end of the input but for the final terminator, if any"
        String output = null;
		try 
		{
			output = new Scanner(new File(file)).useDelimiter("\\Z").next();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
			return null;
		}
		
		return output.split(" ");
	}
	
	/**
	 * 
	 * Return the folder where the image is stored, if it is not saved it return the
	 * folder of the original image 
	 * @param img Image
	 * @return folder of the image
	 */
	public static String ValidFolderFromImage(ImagePlus img)
	{
		if (img == null)
			return null;
			
		if (img.getFileInfo().directory == "")
		{
			if (img.getOriginalFileInfo() == null || img.getOriginalFileInfo().directory == "")
			{return null;}
			else {return img.getOriginalFileInfo().directory;}
		}
		else
		{
			return img.getFileInfo().directory;
		}
	}
	
	public static ImageProcessor cropImageStack2D(ImageProcessor ip, int cropsize) 
	{
		int width = ip.getWidth();
		int newWidth = width - 2*cropsize;
		FloatProcessor cropped_proc = new FloatProcessor(ip.getWidth()-2*cropsize, ip.getHeight()-2*cropsize);
		float[] croppedpx = (float[])cropped_proc.getPixels();
		float[] origpx = (float[])ip.getPixels();
		int offset = cropsize*width+cropsize;
		for (int i = offset, j = 0; j < croppedpx.length; i++, j++) {
			croppedpx[j] = origpx[i];		
			if (j%newWidth == newWidth - 1) {
				i+=2*cropsize;
			}
		} 
		return cropped_proc;
	}
	
	/**
	 * crops a 3D image at all of sides of the imagestack cube.  
	 * @param is a frame to crop
	 * @see pad ImageStack3D
	 * @return the cropped image
	 */
	public static ImageStack cropImageStack3D(ImageStack is, int cropSize) {
		ImageStack cropped_is = new ImageStack(is.getWidth()-2*cropSize, is.getHeight()-2*cropSize);
		for (int s = cropSize + 1; s <= is.getSize()-cropSize; s++) {
			cropped_is.addSlice("", MosaicUtils.cropImageStack2D(is.getProcessor(s), cropSize));
		}
		return cropped_is;
	}
	
	public static ImageProcessor padImageStack2D(ImageProcessor ip, int padSize) {
		int width = ip.getWidth();
		int newWidth = width + 2*padSize;
		FloatProcessor padded_proc = new FloatProcessor(ip.getWidth() + 2*padSize, ip.getHeight() + 2*padSize);
		float[] paddedpx = (float[])padded_proc.getPixels();
		float[] origpx = (float[])ip.getPixels();
		//first r pixel lines
		for (int i = 0; i < padSize*newWidth; i++) {
			if (i%newWidth < padSize) { 			//right corner
				paddedpx[i] = origpx[0];
				continue;
			}
			if (i%newWidth >= padSize + width) {
				paddedpx[i] = origpx[width-1];	//left corner
				continue;
			}
			paddedpx[i] = origpx[i%newWidth-padSize];
		}

		//the original pixel lines and left & right edges				
		for (int i = 0, j = padSize*newWidth; i < origpx.length; i++,j++) {
			int xcoord = i%width;
			if (xcoord==0) {//add r pixel rows (left)
				for (int a = 0; a < padSize; a++) {
					paddedpx[j] = origpx[i];
					j++;
				}
			}
			paddedpx[j] = origpx[i];
			if (xcoord==width-1) {//add r pixel rows (right)
				for (int a = 0; a < padSize; a++) {
					j++;
					paddedpx[j] = origpx[i];
				}
			}
		}

		//last r pixel lines
		int lastlineoffset = origpx.length-width;
		for (int j = (padSize+ip.getHeight())*newWidth, i = 0; j < paddedpx.length; j++, i++) {
			if (i%width == 0) { 			//left corner
				for (int a = 0; a < padSize; a++) {
					paddedpx[j] = origpx[lastlineoffset];
					j++;
				}
				//					continue;
			}
			if (i%width == width-1) {	
				for (int a = 0; a < padSize; a++) {
					paddedpx[j] = origpx[lastlineoffset+width-1];	//right corner
					j++;
				}
				//					continue;
			}
			paddedpx[j] = origpx[lastlineoffset + i % width];
		}
		return padded_proc;
	}
	
	/**
	 * Before convolution, the image is padded such that no artifacts occure at the edge of an image.
	 * @param is a frame (not a movie!)
	 * @see cropImageStack3D(ImageStack)
	 * @return the padded imagestack to (w+2*r, h+2r, s+2r) by copying the last pixel row/line/slice
	 */
	public static ImageStack padImageStack3D(ImageStack is, int padSize) 
	{
		ImageStack padded_is = new ImageStack(is.getWidth() + 2*padSize, is.getHeight() + 2*padSize);
		for (int s = 0; s < is.getSize(); s++){
			ImageProcessor padded_proc = MosaicUtils.padImageStack2D(is.getProcessor(s+1),padSize);
			//if we are on the top or bottom of the stack, add r slices
			if (s == 0 || s == is.getSize() - 1) {
				for (int i = 0; i < padSize; i++) {
					padded_is.addSlice("", padded_proc);
				}
			} 
			padded_is.addSlice("", padded_proc);
		}

		return padded_is;
	}
	



	/**
	 * Does the same as padImageStack3D but does not create a new image. It recreates the edge of the
	 * cube (frame).
	 * @see padImageStack3D, cropImageStack3D
	 * @param aIS
	 */
	public static void repadImageStack3D(ImageStack aIS, int padSize){
		if (aIS.getSize() > 1) { //only in the 3D case
			for (int s = 1; s <= padSize; s++) {
				aIS.deleteSlice(1);
				aIS.deleteLastSlice();
			}
		}
		for (int s = 1; s <= aIS.getSize(); s++) {
			float[] pixels = (float[])aIS.getProcessor(s).getPixels();
			int width = aIS.getWidth();
			int height = aIS.getHeight();
			for (int i = 0; i < pixels.length; i++) {
				int xcoord = i%width;
				int ycoord = i/width;
				if (xcoord < padSize && ycoord < padSize) {
					pixels[i] = pixels[padSize*width+padSize];
					continue;
				}
				if (xcoord < padSize && ycoord >= height-padSize) {
					pixels[i] = pixels[(height-padSize-1)*width+padSize];
					continue;
				}
				if (xcoord >= width-padSize && ycoord < padSize) {
					pixels[i] = pixels[(padSize + 1) * width - padSize - 1];
					continue;
				}
				if (xcoord >= width-padSize && ycoord >= height-padSize) {
					pixels[i] = pixels[(height-padSize)*width - padSize - 1];
					continue;
				}
				if (xcoord < padSize) {
					pixels[i] = pixels[ycoord*width+padSize];
					continue;
				}
				if (xcoord >= width - padSize) {
					pixels[i] = pixels[(ycoord+1)*width - padSize - 1];
					continue;
				}
				if (ycoord < padSize) {
					pixels[i] = pixels[padSize*width + xcoord];
					continue;
				}
				if (ycoord >= height-padSize) {
					pixels[i] = pixels[(height-padSize-1)*width + xcoord];
				}
			}
		}
		if (aIS.getSize() > 1) {
			for (int s = 1; s <= padSize; s++) { //only in 3D case
				aIS.addSlice("", aIS.getProcessor(1).duplicate(),1);
				aIS.addSlice("", aIS.getProcessor(aIS.getSize()).duplicate());
			}
		}
	}

	public static ImageStack GetSubStackInFloat(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if (startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for (int i = startPos; i <= endPos; i++) {
			res.addSlice(is.getSliceLabel(i), is.getProcessor(i).convertToFloat());
		}
		return res;
	}
	
	public static ImageStack GetSubStackCopyInFloat(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if (startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for (int i = startPos; i <= endPos; i++) {
			res.addSlice(is.getSliceLabel(i), is.getProcessor(i).convertToFloat().duplicate());
		}
		return res;
	}
	
	/**
	 * Returns a * c + b
	 * @param a: y-coordinate
	 * @param b: x-coordinate
	 * @param c: width
	 * @return
	 */
	public static int coord (int a, int b, int c) {
		return (((a) * (c)) + (b));
	}

	/**
	 * Writes the given <code>info</code> to given file information.
	 * <code>info</code> will be written to the beginning of the file, overwriting older information
	 * If the file does not exists it will be created.
	 * Any problem creating, writing to or closing the file will generate an ImageJ error   
	 * @param directory location of the file to write to 
	 * @param file_name file name to write to
	 * @param info info the write to file
	 * @see java.io.FileOutputStream#FileOutputStream(java.lang.String)
	 */
	public static boolean write2File(String directory, String file_name, String info) {
		try {
			FileOutputStream fos = new FileOutputStream(new File(directory, file_name));
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			PrintWriter print_writer = new PrintWriter(bos);
			print_writer.print(info);
			print_writer.close();
			return true;
		}
		catch (IOException e) {
			IJ.error("" + e);
			return false;
		}    			

	}
	
	public static ImageStack GetSubStack(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if (startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for (int i = startPos; i <= endPos; i++) {
			res.addSlice(is.getSliceLabel(i), is.getProcessor(i));
		}
		return res;
	}
	

	/**
	 * @param sliceIndex: 1..#slices
	 * @param nb_slices: the number of slices of this frame
	 * @return a frame index: 1..#frames
	 */
	public static int getFrameNumberFromSlice(int sliceIndex, int nb_slices) {
		return (sliceIndex-1) / nb_slices + 1;
	}
	
	/**
	 * 
	 * Copy an image B as a subspace into an image A
	 * 
	 * @param A Image A
	 * @param B Image B
	 * @param fix subspace on A, dim(A) - dim(B) == 1
	 * @return 
	 */

	static public <T extends NativeType<T>> boolean copyEmbedded(Img<T> A, Img<T> B, int fix)
	{
		// Check that the image are != null and the images has a difference
		// in dimensionality of one
		
		if (A == null || B == null)
			return false;
		
		if (A.numDimensions() - B.numDimensions() != 1)
			return false;
		
		Cursor<T> img_c = B.cursor();
        RandomAccessibleInterval< T > view = Views.hyperSlice( A, B.numDimensions(), fix );
		Cursor<T> img_v = Views.iterable(view).cursor();
        
		while (img_c.hasNext())
		{
			img_c.fwd();
			img_v.fwd();
			
			img_v.get().set(img_c.get());
		}
		
		return true;
	}
	
	/**
	 * 
	 * Get the frame ImagePlus from an ImagePlus
	 * 
	 * @param img Image
	 * @param frame frame (frame start from 1)
	 * @return An ImagePlus of the frame
	 */
	
	public static ImagePlus getImageFrame(ImagePlus img, int frame)
	{
		if (frame == 0)
			return null;
		
		int nImages = img.getNFrames();
	
		ImageStack stk = img.getStack();
	
		int stack_size = stk.getSize() / nImages;

		ImageStack tmp_stk = new ImageStack(img.getWidth(),img.getHeight());
		for (int j = 0 ; j < stack_size ; j++)
		{
			tmp_stk.addSlice("st"+j,stk.getProcessor((frame-1)*stack_size+j+1));
		}
		
		ImagePlus ip = new ImagePlus("tmp",tmp_stk);
		return ip;
	}
	
	
	/**
	 * 
	 * Get the ImagePlus slice from an ImagePlus
	 * 
	 * @param img Image
	 * @param channel (channel start from 1)
	 * @return An ImagePlus of the channel
	 */
	
	public static ImagePlus getImageSlice(ImagePlus img, int slice)
	{
		if (slice == 0)
			return null;
		
		int nImages = img.getNSlices();
	
		ImageStack stk = img.getStack();
	
		int stack_size = stk.getSize() / nImages;

		ImageStack tmp_stk = new ImageStack(img.getWidth(),img.getHeight());
		for (int j = 0 ; j < stack_size ; j++)
		{
			tmp_stk.addSlice("st"+j,stk.getProcessor((slice-1)*stack_size+j+1));
		}
		
		ImagePlus ip = new ImagePlus("tmp",tmp_stk);
		return ip;
	}
	
	/**
	 * 
	 * Reorganize the data in the directories inside sv, following the file patterns
	 * specified
	 * 
	 * Give output[] = {data*file1, data*file2}
	 * and bases = {"A","B","C" ..... , "Z"}
	 * 
	 * it create two folder data_file1 and data_file2 (* is replaced with _) and inside put all the file
	 * with pattern dataAfile1 ..... dataZfile1 in the first and dataAfile2 ..... dataZfile2
	 * in the second. If the folder is empty the folder is deleted
	 * 
	 * @param output List of output patterns
	 * @param bases String of the image/data to substitute
	 * @param sv base dir where the data are located
	 * 
	 */
	
	public static void reorganize(String output[], Vector<String> bases, String sv )
	{
		// Crate directories
		for (int j = 0 ; j < output.length ; j++) {
			String tmp = new String(output[j]);
			String dirName = sv + "/" + tmp.replace("*","_");
			SystemOperations.createDir(dirName);
		}
		
		// Copy all existing files
		for (int j = 0 ; j < output.length ; j++)
		{
			String tmp = new String(output[j]);
				
			for (int k = 0 ; k < bases.size() ; k++)
			{
				String src = sv + File.separator + tmp.replace("*",bases.get(k));
				String dest = sv + File.separator + tmp.replace("*", "_") + File.separator + bases.get(k) + tmp.replace("*", "");
				SystemOperations.moveFile(src, dest, true /* quiet */);
			}
		}
		
		// check for all the folder created if empty delete it
		for (int j = 0 ; j < output.length ; j++) {
			String tmp = new String(output[j]);
			String dirStr = sv + "/" + tmp.replace("*","_");
			File dir = new File(dirStr);
			if (dir.listFiles() != null && dir.listFiles().length == 0) {
				SystemOperations.removeDir(dir);
			}
		}	
	}
	
	
	/**
	 * 
	 * Reorganize the data in the directories inside sv, following the file patterns
	 * specified
	 * 
	 * Give output[] = {data*file1, data*file2}
	 * and base = "_tmp_"
	 * 
	 * it create two folder data_file1 and data_file2 (* is replaced with _) and inside put all the file
	 * with pattern data_tmp_1file1 ..... data_tmp_Nfile1 in the first and data_tmp_1file2 ..... data_tmp_Nfile2
	 * in the second. If the folder is empty the folder is deleted
	 * 
	 * @param output List of output patterns
	 * @param base String of the image/data to substitute
	 * @param sv base dir where the data are located
	 * @param nf is N, the number of file, if nf == 0 the file pattern is data_tmp_file without number
	 */
	public static void reorganize(String output[], String base, String sv , int nf)
	{
		// Crate directories
		for (int j = 0 ; j < output.length ; j++) {
			String tmp = new String(output[j]);
			String dirName = sv + "/" + tmp.replace("*","_");
			SystemOperations.createDir(dirName);
		}
		
		// Copy all existing files
		for (int j = 0 ; j < output.length ; j++) {
			String tmp = new String(output[j]);
				
			for (int k = 0 ; k < nf ; k++)
			{
				String src = "";
				String dest = "";
				if (new File(sv + File.separator + tmp.replace("*",base)).exists()) {
					src = sv + File.separator + tmp.replace("*",base);
					dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base + tmp.replace("*", "");
				}
				else {
					src = sv + File.separator + tmp.replace("*",base + (k+1));
					dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base + (k+1) + tmp.replace("*", "");
				}	

				SystemOperations.moveFile(src, dest, true /* quiet */);
			}
		}
		
		// check for all the folder created if empty delete it
		for (int j = 0 ; j < output.length ; j++) {
			String tmp = new String(output[j]);
			String dirStr = sv + "/" + tmp.replace("*","_");
			File dir = new File(dirStr);
			if (dir.listFiles() != null && dir.listFiles().length == 0) {
				SystemOperations.removeDir(dir);
			}
		}

	}
	
	/**
	 * 
	 * Reorganize the data in the directories inside sv, following the file patterns
	 * specified
	 * 
	 * Give output[] = {data*file1, data*file2}
	 * and base_src = "_src_"
	 * and base_dst = "_dst_"
	 * 
	 * it create two folder data_file1 and data_file2 (* is replaced with _) and inside put all the file
	 * with pattern data_src_1file1 ..... data_src_Nfile1 in the first renaming to data_dst_1file1 ..... data_dst_Nfile1
	 * and data_src_1file2 ..... data_src_Nfile2 in the second renaming to data_dst_1file2 ..... data_dst_Nfile2
	 * If the folder is empty the folder is deleted
	 * 
	 * @param output List of output patterns
	 * @param base String of the image/data to substitute
	 * @param sv base dir where the data are located
	 * @param nf is N, the number of file, if nf == 0 the file pattern is data_tmp_file without number
	 */
	
	public static void reorganize(String output[], String base_src, String base_dst, String sv , int nf)
	{
		// Crate directories
		for (int j = 0 ; j < output.length ; j++) {
			String tmp = new String(output[j]);
			String dirName = sv + "/" + tmp.replace("*","_");
			SystemOperations.createDir(dirName);
		}
			
		for (int j = 0 ; j < output.length ; j++)
		{
			String tmp = new String(output[j]);
			for (int k = 0 ; k < nf ; k++)
			{
				String src = "";
				String dest = "";
				if (new File(sv + File.separator + tmp.replace("*",base_src)).exists())
				{
					src = sv + File.separator + tmp.replace("*",base_src);
					dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base_dst + tmp.replace("*", "");
				}
				else
				{
					if (nf ==1) {
						src = sv + File.separator + tmp.replace("*",base_src + "_" + (k+1));
						dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base_dst + tmp.replace("*", "");
					}
					else {
						src = sv + File.separator + tmp.replace("*",base_src + "_" + (k+1));
						dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base_dst + "_" + (k+1) + tmp.replace("*", "");
					}
				}
				
				SystemOperations.moveFile(src, dest, true /* quiet */);
			}
		}
		
		// check for all the folder created if empty delete it
		for (int j = 0 ; j < output.length ; j++) {
			String tmp = new String(output[j]);
			String dirStr = sv + "/" + tmp.replace("*","_");
			File dir = new File(dirStr);
			if (dir.listFiles() != null && dir.listFiles().length == 0) {
				SystemOperations.removeDir(dir);
			}
		}
	}
	
	/**
	 * 
	 * Display outline overlay
	 * 
	 * @param label image
	 * @param data image
	 * @return the outline overlay image
	 * 
	 */
	
//	public  <T extends RealType<T>> Img<BitType> displayoutline(Img<BitType> labelImage, Img<T> image)
//	{
//		/* Create the output image */
//		
//		int dim = labelImage.numDimensions();
//		
//		long [] sz = new long[dim+1];
//		
//		sz[0] = labelImage.dimension(0);
//		sz[1] = labelImage.dimension(1);
//		sz[2] = 2;
//		if (dim >= 3)
//		{
//			sz[3] = labelImage.dimension(2);
//		}
//		final ImgFactory< BitType > imgFactory = new ArrayImgFactory< BitType >();
//		
//		// labelImage.firstElement() should ensure that the output is the same as labelImage
//		
//		Img<BitType> out = imgFactory.create( sz, labelImage.firstElement() );
//		
//		/* Convert Label Image into region contour image */
//		
//		BinaryOperationAssignment<BitType,BitType,BitType> m_imgManWith = new BinaryOperationAssignment<BitType,BitType,BitType>(new BinaryXor());
//		UnaryOperation<RandomAccessibleInterval<BitType>, RandomAccessibleInterval<BitType>> m_op = new Erode(ConnectedType.EIGHT_CONNECTED, null, 1);
//		
//		m_op.compute(labelImage, out);
//		m_imgManWith.compute(labelImage, out, out);
//		
//		/* Create the visualization */
//		
//		Cursor<T> cur = image.cursor();
//		IntervalView<BitType> iv = Views.hyperSlice(out, 2, 1);
//		IterableRandomAccessibleInterval<BitType> iout = IterableRandomAccessibleInterval.create(iv);
//		Cursor<BitType> curL = iout.cursor();
//		
//		while (curL.hasNext())
//		{
//			cur.next();
//			curL.next();
//			
//			curL.get().setReal(cur.get().getRealDouble());	
//		}
//		
//		return out;
//	}
	
	/**
	 * 
	 * Given an index return the set of Coordinate
	 * (Stride ordering)
	 * 
	 * @param index is the idex
	 * @param img is the image
	 * 
	 */

	public static int [] getCoord(long index, Img<?> img)
	{
		int crd[] = new int[img.numDimensions()];
		
		for (int i = 0 ; i < crd.length ; i++)
		{
			crd[i] = (int) (index % img.dimension(i));
			index /= img.dimension(i);
		}
		
		return crd;
	}
	
	/**
	 * 
	 * Create a choose image selector
	 * 
	 * @param gd Generic Dialog
	 * @param cs string for the choice caption
	 * @param imp ImagePlus to start with
	 * @return awt Choice control
	 */
	
	public static Choice chooseImage(GenericDialog gd, String cs, ImagePlus imp)
	{		
		int nOpenedImages = 0;
		int[] ids = WindowManager.getIDList();
			
		if (ids!=null)
		{
			nOpenedImages = ids.length;
		}
		
		
		String[] names = new String[nOpenedImages+1];
		names[0]="";
		for (int i = 0; i<nOpenedImages; i++)
		{
			ImagePlus ip = WindowManager.getImage(ids[i]);
			names[i+1] = ip.getTitle();
		}
		
		if (gd.getChoices() == null)
			return null;
		
		Choice choiceInputImage = (Choice)gd.getChoices().lastElement();
		
		if (imp !=null)
		{
			for (int i = 0 ; i < names.length ; i++)
				choiceInputImage.addItem(names[i]);
			
			String title = imp.getTitle();
			choiceInputImage.select(title);
		}
		else
		{
			choiceInputImage.select(0);
		}
		
		return choiceInputImage;
	}
	
	public static String getRegionMaskName(String filename)
	{
		// remove the extension
		
		String s = filename.substring(0,filename.lastIndexOf("."));
		
		return s + "_seg_c1.tif";
	}
	
	/**
	 * 
	 * Get the CSV Region filename 
	 * 
	 * @param filename of the image
	 * @param channel
	 * @return the CSV region filename
	 */
	
	public static String getRegionCSVName(String filename, int channel)
	{
		// remove the extension
		
		String s = filename.substring(0,filename.lastIndexOf("."));
		
		return s + "_ObjectsData_c"+channel+".csv";
	}
	
	/**
	 * 
	 * Get the CSV Region filename 
	 * 
	 * @param filename of the image
	 * @return the CSV region filename
	 */
	
	public static String getRegionCSVName(String filename)
	{
		// remove the extension
		
		String s = filename.substring(0,filename.lastIndexOf("."));
		
		return s + "_ObjectsData_c1.csv";
	}
	
	/**
	 * 
	 * Get the maximum and the minimum of a video
	 * 
	 * @param mm output min and max
	 */
	
	public static void getMaxMin(File fl, MM mm)
	{
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(fl.getAbsolutePath());
		
		float global_max = 0.0f;
		float global_min = 0.0f;
		
		if (imp != null)
		{
			StackStatistics stack_stats = new StackStatistics(imp);
			global_max = (float)stack_stats.max;
			global_min = (float)stack_stats.min;

			// get the min and the max
		}
		
		if (global_max > mm.max)
			mm.max = global_max;
		
		if (global_min < mm.min)
			mm.min = global_min;
	}
	
	/**
	 * 
	 * Get the maximum and the minimum of a video
	 * 
	 * @param mm output min and max
	 * 
	 */
	
	public static void getFilesMaxMin(File fls[], MM mm)
	{	
		for (File fl : fls)
		{
			getMaxMin(fl,mm);
		}
	}
	
	/**
	 * 
	 * Parse normalize
	 * 
	 * @param options string of options " ..... normalize = true ...... "
	 * @return the value of the argument, null if the argument does not exist
	 */
	
	public static Boolean parseNormalize(String options)
	{
		return parseBoolean("normalize",options);
	}
	
	/**
	 * 
	 * Parse the options string to get the argument max
	 * 
	 * @param options string of options " ..... max = 1.0 ...... "
	 * @return the value of the argument, null if the argument does not exist
	 */
	
	public static Double parseMax(String options)
	{
		return parseDouble("max",options);
	}
	
	/**
	 * 
	 * Parse the options string to get the argument min
	 * 
	 * @param options string of options " ..... min = 1.0 ...... "
	 * @return the value of the argument, null if the argument does not exist
	 */
	
	public static Double parseMin(String options)
	{
		return parseDouble("min",options);
	}
	
	/**
	 * 
	 * Parse the options string to get the argument config
	 * 
	 * @param options string of options " ..... config = xxxxx ...... "
	 * @return the value of the argument, null if the argument does not exist
	 */
	
	public static String parseConfig(String options)
	{
		return parseString("config",options);
	}
	
	/**
	 * 
	 * Parse the options string to get the argument output
	 * 
	 * @param options string of options " ..... config = xxxxx ...... "
	 * @return the value of the argument, null if the argument does not exist
	 */
	
	public static String parseOutput(String options)
	{
		return parseString("output",options);
	}
	
	/**
	 * 
	 * Parse the options string to get an argument
	 * 
	 * @param name the string identify the argument
	 * @param options string of options " ..... config = xxxxx ...... "
	 * @return the value of the argument, null if the argument does not exist
	 */
	
	public static Double parseDouble(String name, String options)
	{
		Pattern min = Pattern.compile("min");
		Pattern spaces = Pattern.compile("[\\s]*=[\\s]*");
		Pattern pathp = Pattern.compile("[a-zA-Z0-9/_.-]+");
		
		// min

		Matcher matcher = min.matcher(options);
		if (matcher.find())
		{
			String sub = options.substring(matcher.end());
			matcher = spaces.matcher(sub);
			if (matcher.find())
			{
				sub = sub.substring(matcher.end());
				matcher = pathp.matcher(sub);
				if (matcher.find())
				{
					String norm = matcher.group(0);
					
					return Double.parseDouble(norm);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * Parse the options string to get an argument
	 * 
	 * @param name the string identify the argument
	 * @param options string of options " ..... config = xxxxx ...... "
	 * @return the value of the argument, null if the argument does not exist
	 */
	
	public static String parseString(String name, String options)
	{
		if (options == null)
			return null;
		
		Pattern config = Pattern.compile(name);
		Pattern spaces = Pattern.compile("[\\s]*=[\\s]*");
		Pattern pathp = Pattern.compile("[a-zA-Z0-9/_.-:-]+");
		
		// config
		
		Matcher matcher = config.matcher(options);
		if (matcher.find())
		{
			String sub = options.substring(matcher.end());
			matcher = spaces.matcher(sub);
			if (matcher.find())
			{
				sub = sub.substring(matcher.end());
				matcher = pathp.matcher(sub);
				if (matcher.find())
				{
					return matcher.group(0);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * Parse the options string to get an argument
	 * 
	 * @param name the string identify the argument
	 * @param options string of options " ..... config = xxxxx ...... "
	 * @return the value of the argument, null if the argument does not exist
	 */
	
	public static Boolean parseBoolean(String name, String options)
	{
		Pattern config = Pattern.compile(name);
		Pattern spaces = Pattern.compile("[\\s]*=[\\s]*");
		Pattern pathp = Pattern.compile("[a-zA-Z0-9/_.-]+");
		
		// config
		
		Matcher matcher = config.matcher(options);
		if (matcher.find())
		{
			String sub = options.substring(matcher.end());
			matcher = spaces.matcher(sub);
			if (matcher.find())
			{
				sub = sub.substring(matcher.end());
				matcher = pathp.matcher(sub);
				if (matcher.find())
				{
					return Boolean.parseBoolean(matcher.group(0));
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * Given an imglib2 image return the dimensions as an array of integer
	 * 
	 * @param img Image
	 * @return array with the image dimensions
	 */
	
	public static <T> int [] getImageIntDimensions(Img<T> img)
	{
		int dimensions[] = new int[img.numDimensions()];
		for (int i = 0 ; i < img.numDimensions() ; i++)
		{
			dimensions[i] = (int) img.dimension(i);
		}
		
		return dimensions;
	}
	
	/**
	 * 
	 * Given an imglib2 image return the dimensions as an array of long
	 * 
	 * @param img Image
	 * @return array with the image dimensions
	 */
	
	public static <T> long [] getImageLongDimensions(Img<T> img)
	{
		long dimensions_l[] = new long[img.numDimensions()];
		for (int i = 0 ; i < img.numDimensions() ; i++)
		{
			dimensions_l[i] = img.dimension(i);
		}
		
		return dimensions_l;
	}
	
	/**
	 * 
	 * Get the minimal value and maximal value of an image
	 * 
	 * @param img Image
	 * @param min minimum value
	 * @param max maximum value
	 */
	
	public static <T extends RealType<T>> void getMinMax(Img<T> img, T min, T max)
	{
		// Set min and max
		
		min.setReal(Double.MAX_VALUE);
		max.setReal(Double.MIN_VALUE);
		
        Cursor<T> cur = img.cursor();
        
        // Get the min and max
        
        while (cur.hasNext())
        {
        	cur.fwd();
        	if (cur.get().getRealDouble() < min.getRealDouble())
        		min.setReal(cur.get().getRealDouble());
        	if (cur.get().getRealDouble() > max.getRealDouble())
        		max.setReal(cur.get().getRealDouble());
        }
	}
	
	/**
	 * 
	 * Get the minimal value and maximal value of an image
	 * 
	 * @param img Image
	 * @param min minimum value
	 * @param max maximum value
	 */
	
	public static <T extends RealType<T>> void getMinMax(Cursor<T> cur, T min, T max)
	{
		// Set min and max
		
		min.setReal(Double.MAX_VALUE);
		max.setReal(Double.MIN_VALUE);
        
        // Get the min and max
        
        while (cur.hasNext())
        {
        	cur.fwd();
        	if (cur.get().getRealDouble() < min.getRealDouble())
        		min.setReal(cur.get().getRealDouble());
        	if (cur.get().getRealDouble() > max.getRealDouble())
        		max.setReal(cur.get().getRealDouble());
        }
	}
	
	/**
	 * 
	 * Open an image
	 * 
	 * @param fl Filename
	 * @return An image
	 * 
	 */
	
	static public ImagePlus openImg(String fl)
	{
		return IJ.openImage(fl);
	}
	
	/**
	 * 
	 * Test data directory
	 * 
	 * @return Test data directory
	 * 
	 */
	
	static public String getTestDir()
	{
		return SystemOperations.getTestDataPath();
	}
	
	static String TestBaseDirectory = "/home/i-bird/Desktop/MOSAIC/image_test_2/ImageJ/plugin/Jtest_data";
	
	/**
	 * 
	 * It return the set of test images to test
	 * 
	 * @param name of the test set
	 * @param test name
	 * @return an array of test images
	 */
	
	static public ImgTest[] getTestImages(String plugin, String filter)
	{
		// Search for test images
		
		Vector<ImgTest> it = new Vector<ImgTest>();
		
		String TestFolder = new String();
		
		TestFolder +=  getTestDir() + File.separator + plugin + File.separator;
		IJ.log(TestFolder);
		ImgTest imgT = null;
		
		// List all directories
		
		File fl = new File(TestFolder);
		File dirs[] = fl.listFiles();
		
		if (dirs == null)
			return null;
		
		for (File dir : dirs)
		{		
			if (dir.isDirectory() == false)
				continue;
			
			// open config
		
			String cfg = dir.getAbsolutePath() + File.separator + "config.cfg";
		
			// Format
			//
			// Image
			// options
			// setup file
			// Expected setup return
			// number of images results
			// ..... List of images result
			// number of csv results
			// ..... List of csv result
		
			try
			{
				if (filter != null && filter.length() != 0 && dir.getAbsolutePath().endsWith(filter.trim()) == false)
					continue;
				
				BufferedReader br = new BufferedReader(new FileReader(cfg));
 
				imgT = new ImgTest();
			
				imgT.base = dir.getAbsolutePath();
				int nimage_file = Integer.parseInt(br.readLine());
				imgT.img = new String[nimage_file];
				for (int i = 0 ; i < imgT.img.length ; i++)
				{
					imgT.img[i] = dir.getAbsolutePath() + File.separator + br.readLine();
				}
				
				imgT.options = br.readLine();
				
				int nsetup_file = Integer.parseInt(br.readLine());
				imgT.setup_files = new String[nsetup_file];
				
				for (int i = 0 ; i < imgT.setup_files.length ; i++)
				{
					imgT.setup_files[i] = dir.getAbsolutePath() + File.separator + br.readLine();
				}
				
				imgT.setup_return = Integer.parseInt(br.readLine());
				int n_images = Integer.parseInt(br.readLine());
				imgT.result_imgs = new String[n_images];
				imgT.result_imgs_rel = new String[n_images];
				imgT.csv_results_rel = new String[n_images];
				
				for (int i = 0 ; i < imgT.result_imgs.length ; i++)
				{
					imgT.result_imgs_rel[i] = br.readLine();
					imgT.result_imgs[i] = dir.getAbsolutePath() + File.separator + imgT.result_imgs_rel[i];
				}
				
				int n_csv_res = Integer.parseInt(br.readLine());

				imgT.csv_results = new String[n_csv_res];
				imgT.csv_results_rel = new String[n_csv_res];
				for (int i = 0 ; i < imgT.csv_results.length ; i++)
				{
					imgT.csv_results_rel[i] = br.readLine();
					imgT.csv_results[i] = dir.getAbsolutePath() + File.separator + imgT.csv_results_rel[i];
				}
				br.close();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				return null;
			}
			
			it.add(imgT);
		}
		
		// Convert Vector to array
		
		ImgTest [] its = new ImgTest[it.size()];
		
		for (int i = 0 ; i < its.length ; i++)
		{
			its[i] = it.get(i);
		}
		
		return its;
	}
	
	/**
	 * 
	 * Compare two images
	 * 
	 * 
	 * @param img1 Image1
	 * @param img2 Image2
	 * @return true if they match, false otherwise
	 */
	
	public static boolean compare(Img<?> img1, Img<?> img2)
	{

		Cursor<?> ci1 = img1.cursor();
		RandomAccess<?> ci2 = img2.randomAccess();
		
		int loc[] = new int[img1.numDimensions()];
		
		while (ci1.hasNext())
		{
			ci1.fwd();
			ci1.localize(loc);
			ci2.setPosition(loc);
			
			Object t1 = ci1.get();
			Object t2 = ci2.get();
			
			if (!t1.equals(t2))
			{
				return false;
			}
		}
		
		return true;
	}
	
//	static private String filterJob(String dir,int n)
//	{
//		if (dir.startsWith("Job"))
//		{
//			// It is a job
//			
//			String[] fl = ClusterSession.getJobDirectories(0, dir);
//			
//			// search if exist
//			
//			for (int i = 0 ; i < fl.length ; i++)
//			{
//				
//				if (new File(dir.replace("*", ClusterSession.getJobNumber(fl[i]))).exists())
//				{
//					return dir.replace("*", ClusterSession.getJobNumber(fl[i]));
//				}
//			}
//		}
//		return dir;
//	}
	
	
	/**
	 * 
	 * Remove the file extension
	 * 
	 * @param str String from where to remove the extension
	 * @return the String
	 */
	
	public static String removeExtension(String str)
	{
		int idp = str.lastIndexOf(".");
		if (idp < 0)
		{
			return str;
		}
		else
		{
			return str.substring(0,idp);
		}
	}
	
	/**
	 * 
	 * Shift the dimensions on the right. Example suppose to have an image
	 * with Channels=20 Z=30 T=1, it reslice the stack to
	 * Channels = 1 Z=20 T=30
	 * If i reapply I get
	 * Channels=20 Z=1 T=20
	 * 
	 * @param ip
	 */
	
	public static void shiftDimensionsR(ImagePlus ip)
	{
		Calibration cal = ip.getCalibration();
		
		IJ.run(ip,"Properties...","channels=" + ip.getNFrames() + " slices=" + ip.getNChannels() + " frames=" + ip.getNSlices() + " unit=" + cal.getUnit() + " pixel_width=" + cal.pixelWidth + " pixel_height=" + cal.pixelHeight + " voxel_depth=" + cal.pixelDepth);
	}
	
	/**
	 * 
	 * Filter out the csv output dir
	 * 
	 * @param dir Array of directories
	 * @return CSV output dir
	 */
	
	public static String[] getCSV(String[] dir)
	{
		Vector<String> outcsv = new Vector<String>();
		
		for (int i = 0 ; i < dir.length ; i++)
		{
			if (dir[i].endsWith(".csv"))
			{
				outcsv.add(dir[i].replace("*", "_"));
			}
		}
		
		String [] outS = new String[outcsv.size()];
		
		for (int i = 0 ; i < outcsv.size() ; i++)
		{
			outS[i] = outcsv.get(i);
		}
		
		return outS;
	}
	
    /**
     * Stitch the CSV files all together in the directory dir/dir_p[] save the
     * result in output_file + dir_p[] "*" are substituted by "_"
     * 
     * @param dir_p list of directories
     * @param dir Base
     * @param output_file stitched file
     * @param Class<T> internal data for conversion
     * @return true if success, false otherwise
     */
    public static <T extends ICSVGeneral> boolean Stitch(String dir_p[], File dir, File output_file, MetaInfo ext[], Class<T> cls) {
        boolean first = true;
        InterPluginCSV<?> csv = new InterPluginCSV<T>(cls);

        for (int j = 0; j < dir_p.length; j++) {
            File[] fl = new File(dir + File.separator + dir_p[j].replace("*", "_")).listFiles();
            if (fl == null)
                continue;
            int nf = fl.length;

            String str[] = new String[nf];

            for (int i = 1; i <= nf; i++) {
                if (fl[i - 1].getName().endsWith(".csv"))
                    str[i - 1] = fl[i - 1].getAbsolutePath();
            }

            if (ext != null) {
                for (int i = 0; i < ext.length; i++)
                    csv.setMetaInformation(ext[i].parameter, ext[i].value);
            }

            if (first == true) {
                // if it is the first time set the file preference from the first file
                first = false;

                csv.setCSVPreferenceFromFile(str[0]);
            }

            csv.Stitch(str, output_file + dir_p[j]);
        }

        return true;
    }
	
	/**
	 * 
	 * Stitch the CSV in the directory
	 * 
	 * @param fl directory where search for files to stitch directory to stitch
	 * @param output string array that list all the outputs produced by the plugin
	 * @param background Set the background param string
	 * @return true if it stitch all the file success
	 */
	
	static public boolean StitchCSV(String fl, String[] output, String bck)
	{
		MetaInfo mt[] = null;
		if (bck != null)
		{
			mt = new MetaInfo[1];
			mt[0] = new MetaInfo();
			mt[0].parameter = new String("background");
			mt[0].value = new String(bck);
		}
		else
		{
			mt = new MetaInfo[0];
		}
		

		String[] outcsv = MosaicUtils.getCSV(output);
		Stitch(outcsv, new File(fl), new File(fl + File.separator + "stitch"), mt, Region3DColocRScript.class);
		
		return true;
	}
	
	/**
	 * 
	 * Stitch the CSV in the Jobs directory
	 * 
	 * @param fl directory where search for JobsXXX directory to stitch the csv
	 * @param output string array that list all the outputs produced by the plugin
	 * @param background Set the backgrond param string
	 * @return true if it stitch all the file success
	 */
	
	static public boolean StitchJobsCSV(String fl, String[] output, String bck)
	{		
		// get the job directories
		
		String[] JobDir = ClusterSession.getJobDirectories(0, fl);
		
		// for all job dir stitch
		
		for (int i = 0 ; i < JobDir.length ; i++)
		{
			StitchCSV(JobDir[i],output,bck);
		}
		
		return true;
	}

	/**
	 * 
	 * This function check if the Fiji respect all the requirement to run the MosaicToolSuite
	 * 
	 * @return true if respect the requirement
	 * 
	 */
	
	static public boolean checkRequirement()
	{
		if (IJ.versionLessThan("1.48"))
		{
			IJ.error("Your Fiji or ImageJ version is too old to run the MosaicToolSuite please update it");
			return false;
		}
		return true;
	}
}
