package mosaic.core.utils;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import mosaic.bregman.Analysis;
import mosaic.core.GUI.ProgressBarWin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class MosaicUtils {
	
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
			// TODO Auto-generated catch block
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
		for(int i = offset, j = 0; j < croppedpx.length; i++, j++) {
			croppedpx[j] = origpx[i];		
			if(j%newWidth == newWidth - 1) {
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
		for(int s = cropSize + 1; s <= is.getSize()-cropSize; s++) {
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
		for(int i = 0; i < padSize*newWidth; i++) {
			if(i%newWidth < padSize) { 			//right corner
				paddedpx[i] = origpx[0];
				continue;
			}
			if(i%newWidth >= padSize + width) {
				paddedpx[i] = origpx[width-1];	//left corner
				continue;
			}
			paddedpx[i] = origpx[i%newWidth-padSize];
		}

		//the original pixel lines and left & right edges				
		for(int i = 0, j = padSize*newWidth; i < origpx.length; i++,j++) {
			int xcoord = i%width;
			if(xcoord==0) {//add r pixel rows (left)
				for(int a = 0; a < padSize; a++) {
					paddedpx[j] = origpx[i];
					j++;
				}
			}
			paddedpx[j] = origpx[i];
			if(xcoord==width-1) {//add r pixel rows (right)
				for(int a = 0; a < padSize; a++) {
					j++;
					paddedpx[j] = origpx[i];
				}
			}
		}

		//last r pixel lines
		int lastlineoffset = origpx.length-width;
		for(int j = (padSize+ip.getHeight())*newWidth, i = 0; j < paddedpx.length; j++, i++) {
			if(i%width == 0) { 			//left corner
				for(int a = 0; a < padSize; a++) {
					paddedpx[j] = origpx[lastlineoffset];
					j++;
				}
				//					continue;
			}
			if(i%width == width-1) {	
				for(int a = 0; a < padSize; a++) {
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
		for(int s = 0; s < is.getSize(); s++){
			ImageProcessor padded_proc = MosaicUtils.padImageStack2D(is.getProcessor(s+1),padSize);
			//if we are on the top or bottom of the stack, add r slices
			if(s == 0 || s == is.getSize() - 1) {
				for(int i = 0; i < padSize; i++) {
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
		if(aIS.getSize() > 1) { //only in the 3D case
			for(int s = 1; s <= padSize; s++) {
				aIS.deleteSlice(1);
				aIS.deleteLastSlice();
			}
		}
		for(int s = 1; s <= aIS.getSize(); s++) {
			float[] pixels = (float[])aIS.getProcessor(s).getPixels();
			int width = aIS.getWidth();
			int height = aIS.getHeight();
			for(int i = 0; i < pixels.length; i++) {
				int xcoord = i%width;
				int ycoord = i/width;
				if(xcoord < padSize && ycoord < padSize) {
					pixels[i] = pixels[padSize*width+padSize];
					continue;
				}
				if(xcoord < padSize && ycoord >= height-padSize) {
					pixels[i] = pixels[(height-padSize-1)*width+padSize];
					continue;
				}
				if(xcoord >= width-padSize && ycoord < padSize) {
					pixels[i] = pixels[(padSize + 1) * width - padSize - 1];
					continue;
				}
				if(xcoord >= width-padSize && ycoord >= height-padSize) {
					pixels[i] = pixels[(height-padSize)*width - padSize - 1];
					continue;
				}
				if(xcoord < padSize) {
					pixels[i] = pixels[ycoord*width+padSize];
					continue;
				}
				if(xcoord >= width - padSize) {
					pixels[i] = pixels[(ycoord+1)*width - padSize - 1];
					continue;
				}
				if(ycoord < padSize) {
					pixels[i] = pixels[padSize*width + xcoord];
					continue;
				}
				if(ycoord >= height-padSize) {
					pixels[i] = pixels[(height-padSize-1)*width + xcoord];
				}
			}
		}
		if(aIS.getSize() > 1) {
			for(int s = 1; s <= padSize; s++) { //only in 3D case
				aIS.addSlice("", aIS.getProcessor(1).duplicate(),1);
				aIS.addSlice("", aIS.getProcessor(aIS.getSize()).duplicate());
			}
		}
	}

	public static ImageStack GetSubStackInFloat(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if(startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for(int i = startPos; i <= endPos; i++) {
			res.addSlice(is.getSliceLabel(i), is.getProcessor(i).convertToFloat());
		}
		return res;
	}
	
	public static ImageStack GetSubStackCopyInFloat(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if(startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for(int i = startPos; i <= endPos; i++) {
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
		if(startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for(int i = startPos; i <= endPos; i++) {
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
	 * @param frame frame
	 * @return An ImagePlus of the frame
	 */
	
	public static ImagePlus getImageFrame(ImagePlus img, int frame)
	{
		int nImages = img.getNFrames();
	
		ImageStack stk = img.getStack();
	
		int stack_size = stk.getSize() / nImages;

		ImageStack tmp_stk = new ImageStack(img.getWidth(),img.getHeight());
		for (int j = 0 ; j < stack_size ; j++)
		{
			tmp_stk.addSlice("st"+j,stk.getProcessor(frame*stack_size+j+1));
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
	 * and base = "_tmp_"
	 * 
	 * it create two folder data_file1 and data_file2 and inside put all the file
	 * with pattern data_tmp_1file1 ..... data_tmp_Nfile1 in the first and data_tmp_1file2 ..... data_tmp_Nfile2
	 * in the second
	 * 
	 * @param output List of output patterns
	 * @param base String of the image/data to substitute
	 * @param sv Save path
	 */
	
	public static void reorganize(String output[], String base, String sv , int nf)
	{
		// reorganize
		
		try 
		{
			for (int j = 0 ; j < output.length ; j++)
			{
				String tmp = new String(output[j]);
		
				Process tProcess;
				tProcess = Runtime.getRuntime().exec("mkdir " + sv + "/" + tmp.replace("*","_"));
				tProcess.waitFor();
			}
				
			for (int j = 0 ; j < output.length ; j++)
			{
				String tmp = new String(output[j]);
					
				Process tProcess;
				for (int k = 0 ; k < nf ; k++)
				{
					tProcess = Runtime.getRuntime().exec("mv " + sv + "/" + tmp.replace("*",base + (k+1)) + "   " + sv + "/" + tmp.replace("*", "_"));
					tProcess.waitFor();
				}
			}
		} 
		catch (IOException e) 
		{
		// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
