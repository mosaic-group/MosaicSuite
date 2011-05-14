package mosaic.plugins;

import java.awt.Color;
import java.awt.Image;

import mosaic.region_competition.LabelImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/**
 * This plugin immediately ends ImageJ with return code 0.
 * The purpose of the plugin is to end ImageJ via the Macro language when running on a cluster
 * with a virtual frame buffer.
 * @author Janick Cardinale, ETH Zurich
 * @version 1.0, September 2010
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
public class Region_Competition implements PlugInFilter{
		
	LabelImage labelImage;
	ImagePlus originalIP;
	
	public int setup(String aArgs, ImagePlus aImp) 
	{
		if(aImp == null) {
//			IJ.showMessage("Select an Image.");
//			return DONE;
			
			//DEGUG create empty image
			aImp = new ImagePlus("empty test image", new ShortProcessor(400, 300));
			
		}
		originalIP = aImp;
		
		initWithCustom();
//		initializeLabelImageAndContourContainer();
		
		return DOES_ALL + DONE;
	}
	
	public void run(ImageProcessor aImageProcessor) 
	{
		
	}
	
	void initWithCustom()
	{
		ImageProcessor oProc = originalIP.getChannelProcessor();
		labelImage = new LabelImage(originalIP);
//		labelImage = oProc.duplicate();
		labelImage.insert(oProc, 0, 0);
		labelImage.initBoundary();
		labelImage.generateContour();
		labelImage.computeStatistics();
		
		new ImagePlus("LabelImage", labelImage).show();
	}
	
	/** 
	 * Generates a labelImage filled with bgLabel and a one-pixel boundary of value forbiddenLabel
	 */
	private void initializeLabelImageAndContourContainer() 
	{
		labelImage = new LabelImage(originalIP);
		labelImage.initZero();
		labelImage.initBoundary();
		labelImage.initialGuess();
		labelImage.generateContour();
		labelImage.computeStatistics();
		
		new ImagePlus("LabelImage", labelImage).show();
	}


}
