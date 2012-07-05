package mosaic.region_competition;

import java.util.Arrays;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;


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
			pixels[i] = (int)proc[i];
		}
		return pixels;
	}
	
	public static int[] byteToInt(byte[] proc)
	{
		int n = proc.length;
		int[] pixels = new int[n];
		
		for(int i=0; i<n; i++) {
			pixels[i] = (int)proc[i];
		}
		return pixels;
	}
	
	
	public static short[] intToShort(int[] proc)
	{
		return intToShort(proc, false, false);
	}
	
	public static short[] intToShort(int[] proc, boolean abs, boolean borderRemove)
	{
		int n = proc.length;
		short[] pixels = new short[n];
		
		if(abs){
			for(int i=0; i<n; i++) {
				pixels[i] = (short)Math.abs(proc[i]);
			}
		} else {
			for(int i=0; i<n; i++) {
				pixels[i] = (short)proc[i];
			}
		}
		
		if(borderRemove){
			for(int i=0; i<n; i++) {
				pixels[i] = (pixels[i]==Short.MAX_VALUE ? 0 : pixels[i]);
			}
		}
		
		return pixels;
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
	
	
	
	
	
	
	
	
	
	
}