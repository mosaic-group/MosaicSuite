

import java.util.Random;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/**
 * A small ImageJ Plugin that inserts Poisson distributed noise to an image or an image stack. 
 * @author Janick Cardinale, ETH Zurich
 * @version 1.0, January 08
 * 
 * <p><b>Disclaimer</b>
 * <br>IN NO EVENT SHALL THE ETH BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, 
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND
 * ITS DOCUMENTATION, EVEN IF THE ETH HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * THE ETH SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. 
 * THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE ETH HAS NO 
 * OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.<p>
 *
 */
public class Poisson_Noise implements PlugInFilter{
	public int mSeed = 8888;
	private Random mRandomGenerator;
	private static final int BYTE=0, SHORT=1, FLOAT=2;
	
	public int setup(String aArgs, ImagePlus aImp) {
		mRandomGenerator = new Random(mSeed);
		//run(new FloatProcessor(1,1));		
		//return DONE;
		return DOES_ALL - DOES_RGB + DOES_STACKS;
	}
	
	public void run(ImageProcessor aImageProcessor) {
		int vType;
        if (aImageProcessor instanceof ByteProcessor)
        	vType = BYTE;
        else if (aImageProcessor instanceof ShortProcessor)
        	vType = SHORT;
        else if (aImageProcessor instanceof FloatProcessor)
        	vType = FLOAT;
        else {
        	IJ.showMessage("Wrong image type");
        	return;
        }
        
        
        switch (vType) {
        case BYTE:
        {
        	byte[] vPixels = (byte[])aImageProcessor.getPixels();
        	for(int vI = 0; vI < vPixels.length; vI++) {
        		vPixels[vI] = (byte)generatePoissonRV((int)(vPixels[vI]+.5f));
        	}
        	break;
        }
        case SHORT:
        {
        	short[] vPixels = (short[])aImageProcessor.getPixels();
        	for(int vI = 0; vI < vPixels.length; vI++) {
        		vPixels[vI] = (short)generatePoissonRV((int)(vPixels[vI]+.5f));
        	}
        	break;
        }
        case FLOAT:
        {
        	float[] vPixels = (float[])aImageProcessor.getPixels();
        	for(int vI = 0; vI < vPixels.length; vI++) {
        		vPixels[vI] = (float)generatePoissonRV((int)(vPixels[vI]+.5f));
        	}
        	break;
        }
        }

	}
	
	public int generatePoissonRV(int aLambda){
		if(aLambda >= 30) {
			return (int)(mRandomGenerator.nextGaussian() * Math.sqrt(aLambda) + aLambda + 0.5);
		}
		double p = 1;
		int k = 0;
		double vL = Math.exp(-aLambda);
		do{
			k++;
			p *= mRandomGenerator.nextDouble();
		} while(p >= vL);
		return k - 1;
	}

}
