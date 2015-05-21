package mosaic.region_competition.utils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.IntType;


/**
 * Raw-converts {@link ImagePlus}, {@link ImageProcessor} and Arrays to int-versions
 */
public class IntConverter
{
	public static ImagePlus IPtoInt(ImagePlus ip)
	{
		
		ImageStack stack = ip.getStack();
		int nSlices = stack.getSize();
		
		ImageStack newStack = new ImageStack(ip.getWidth(), ip.getHeight());
		
		ImagePlus newIP = new ImagePlus();
		for(int i=1; i<=nSlices; i++)
		{
			ImageProcessor proc = stack.getProcessor(i);
			ColorProcessor newProc = procToIntProc(proc);
			newStack.addSlice(stack.getSliceLabel(i), newProc);
		}
		newIP.setStack(newStack);
		newIP.setDimensions(ip.getNChannels(), ip.getNSlices(), ip.getNFrames());
		
		return newIP;

	}
	
	/**
	 * @param proc
	 * @return copy or converted proc
	 */
	public static ColorProcessor procToIntProc(ImageProcessor proc)
	{
		Object pixels = proc.getPixels();
		
		int[] intArray = null;
		if(pixels instanceof int[]){
			intArray = ((int[])pixels).clone();
		} else {
			intArray = arrayToInt(pixels);
		}
		
		ColorProcessor newProc = new ColorProcessor(proc.getWidth(), proc.getHeight());
		newProc.setPixels(intArray);
		
		return newProc;
	}
	
	/**
	 * Converts float/byte/short array into int array <br>
	 * @param array float/byte/short array (NO int!)
	 * @return converted array
	 */
	public static int[] arrayToInt(Object array)
	{
		int[] intArray = null;
		
		if(array instanceof float[]){
			intArray = floatToInt((float[])array);
		} else if(array instanceof byte[]){
			intArray = byteToInt((byte[])array);
		} else if(array instanceof short[]){
			intArray = shortToInt((short[])array);
		} else {
			throw new RuntimeException("not Supported conversion");
		}
		
		return intArray;
	}
	
	public static int[] floatToInt(float[] proc)
	{
		int n = proc.length;
		int[] pixels = new int[n];
		
		for(int i=0; i<n; i++) {
			pixels[i] = (int)proc[i];
		}
		return pixels;
	}
	
	public static int[] shortToInt(short[] proc)
	{
		int n = proc.length;
		int[] pixels = new int[n];
		
		for(int i=0; i<n; i++) {
			pixels[i] = proc[i];
		}
		return pixels;
	}
	
	public static int[] byteToInt(byte[] proc)
	{
		int n = proc.length;
		int[] pixels = new int[n];
		
		for(int i=0; i<n; i++) {
			pixels[i] = proc[i];
		}
		return pixels;
	}
	
	
	public static short[] intToShort(int[] proc)
	{
		return intToShort(proc, false, false, true);
	}
	
	
	/**
	 * 
	 * @param ints int[] array
	 * @param abs Math.abs() the array
	 * @param borderRemove Short.MAX_VALUE to Zero
	 * @param clamp Values > Short.MAX_VALUE to Short.MAX_VALUE (same for MIN_VALUE)
	 * @return
	 */
	public static short[] intToShort(int[] ints, boolean abs, boolean borderRemove, boolean clamp)
	{
		int n = ints.length;
		short[] shorts = new short[n];
		
		if(abs){
			for(int i=0; i<n; i++) {
				int a = Math.abs(ints[i]);
				if(clamp)
				{
					if(a>Short.MAX_VALUE)
						a=Short.MAX_VALUE;
				}
				shorts[i] = (short)a;
			}
		} else {
			for(int i=0; i<n; i++) {
				int a = ints[i];
				if(clamp)
				{
					if(a>Short.MAX_VALUE)
						a=Short.MAX_VALUE;
					else if(a<Short.MIN_VALUE)
						a=Short.MIN_VALUE;
				}
				shorts[i] = (short)a;
			}
		}
		
		if(borderRemove)
		{
			for(int i=0; i<n; i++) 
			{
				short a = shorts[i];
				if(a==Short.MAX_VALUE){
					a=0;
				}
				shorts[i]=a;
			}
		}

		
		return shorts;
	}
	
	/**
	 * 
	 * @param ints int[] array
	 * @param abs Math.abs() the array
	 * @param borderRemove Short.MAX_VALUE to Zero
	 * @param clamp Values > Short.MAX_VALUE to Short.MAX_VALUE (same for MIN_VALUE)
	 * @return
	 */
	public static short[] ImgToShort(Img <IntType> ints, boolean abs, boolean borderRemove, boolean clamp)
	{
		int n = (int)ints.size();
		short[] shorts = new short[n];
		
		Cursor <IntType> cur = ints.cursor();
		
		if(abs)
		{
			for(int i=0; i<n && cur.hasNext(); i++) 
			{
				cur.fwd();
				int a = Math.abs(cur.get().get());
				if(clamp)
				{
					if(a>Short.MAX_VALUE)
						a=Short.MAX_VALUE;
				}
				shorts[i] = (short)a;
			}
		} 
		else 
		{
			for(int i=0; i<n && cur.hasNext(); i++) 
			{
				cur.fwd();
				int a = Math.abs(cur.get().get());
				if(clamp)
				{
					if(a>Short.MAX_VALUE)
						a=Short.MAX_VALUE;
					else if(a<Short.MIN_VALUE)
						a=Short.MIN_VALUE;
				}
				shorts[i] = (short)a;
			}
		}
		
		if(borderRemove)
		{
			for(int i=0; i<n; i++) 
			{
				short a = shorts[i];
				if(a==Short.MAX_VALUE){
					a=0;
				}
				shorts[i]=a;
			}
		}

		
		return shorts;
	}
	
	
	
	public static int[] intStackToArray(ImageStack stack)
	{
		int[] result;
		
		int zs = stack.getSize();
		int area = stack.getWidth()*stack.getHeight();
		result = new int[zs*area];
		
		for(int z=0; z<zs; z++)
		{
			int[] pixels = (int[])stack.getPixels(z+1);
			for(int j=0; j<area; j++){
				result[z*area+j]=pixels[j];
			}
		}
		
		return result;
	}
	
	
	public static ImageStack intArrayToShortStack(int[] intData, int[] dims)
	{
		return intArrayToShortStack(intData, dims, false, false, false);
		
	}
	
	/**
	 * @param clean Takes absolute values, clamp to short values and remove boundary
	 */
	public static ImageStack intArrayToShortStack(int[] intData, int[] dims, 
			boolean clean)
	{
		return intArrayToShortStack(intData, dims, clean, clean, clean);
		
	}
	
	/**
	 * @param clean Takes absolute values, clamp to short values and remove boundary
	 */
	public static ImageStack ImgToShortStack(Img<IntType> intData, int[] dims, 
			boolean clean)
	{
		return ImgToShortStack(intData, dims, clean, clean, clean);
		
	}
	
	/**
	 * 
	 * Int array to short image conversion
	 * 
	 * @param intData
	 * @param dims
	 * @param abs
	 * @param borderRemove
	 * @param clamp
	 * @return
	 */
	
	public static ImageStack intArrayToShortStack(int[] intData, int[] dims, 
			boolean abs, boolean borderRemove, boolean clamp)
	{
		short shortData[] = IntConverter.intToShort(intData, abs, borderRemove, clamp);

		int w,h,z;
		w=dims[0];
		h=dims[1];
		z=dims[2];
		int area = w*h;
		
		ImageStack stack = new ImageStack(w,h);
		for(int i=0; i<z; i++)
		{
			Object pixels = Arrays.copyOfRange(shortData, i*area, (i+1)*area);
			stack.addSlice("", pixels);
		}
		
		return stack;
		
	}
	
	public static ImageStack ImgToShortStack(Img <IntType> intData, int[] dims, 
			boolean abs, boolean borderRemove, boolean clamp)
	{
		short shortData[] = IntConverter.ImgToShort(intData, abs, borderRemove, clamp);

		int w,h,z;
		w=dims[0];
		h=dims[1];
		z=dims[2];
		int area = w*h;
		
		ImageStack stack = new ImageStack(w,h);
		for(int i=0; i<z; i++)
		{
			Object pixels = Arrays.copyOfRange(shortData, i*area, (i+1)*area);
			stack.addSlice("", pixels);
		}
		
		return stack;
		
	}
	
	/**
	 * @param intData can be reference, method makes copy
	 * @param dims
	 * @return
	 */
	public static ImageStack intArrayToStack(int[] intData, int[] dims)
	{
		int dim = dims.length;
		
		int w,h,z;
		w=dims[0];
		h=dims[1];
		z=1;
		if(dim==3){
			z=dims[2];
		}
		int area = w*h;
		
		ImageStack stack = new ImageStack(w,h);
		for(int i=0; i<z; i++)
		{
			Object pixels = Arrays.copyOfRange(intData, i*area, (i+1)*area);
			stack.addSlice("", pixels);
		}
		
		return stack;
	}

	public static ImageStack ImgToStack(Img<IntType> initData, int[] dims) 
	{
		int dim = dims.length;
		
		int w,h,z;
		w=dims[0];
		h=dims[1];
		z=1;
		if(dim==3){
			z=dims[2];
		}
		int area = w*h;
		
		ImageStack stack = new ImageStack(w,h);
		int pixels[] = new int[area];
		
		Cursor<IntType> cur = initData.cursor();
		
		for(int i=0; i<z && cur.hasNext(); i++)
		{
			for (int j = 0 ; j < area && cur.hasNext() ; j++)
			{
				cur.fwd();
				pixels[j] = cur.get().get();
			}
			
			stack.addSlice("", pixels);
		}
		
		return stack;
	}
	
	
	
	
}
