package mosaic.plugins;




import java.util.HashMap;

import javax.vecmath.Point2d;

import mosaic.region_competition.ContourParticle;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
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
		
	HashMap<Point2d, ContourParticle> mContourContainer;
	ImageProcessor mLabelImage;
	ImagePlus mOriginalIP;
	int mForbiddenLabel = Integer.MAX_VALUE;
	
	
	
	public int setup(String aArgs, ImagePlus aImp) {
		if(aImp == null) {
			IJ.showMessage("Select an Image.");
			return DONE;
		}
		mOriginalIP = aImp;
		
		initializeLabelImageAndContourContainer();
		
		return DOES_ALL + DONE;
	}
	
	public void run(ImageProcessor aImageProcessor) {
		
	}
	
	private void initializeLabelImageAndContourContainer() {
		mLabelImage=new ShortProcessor(mOriginalIP.getWidth(), mOriginalIP.getHeight());
		for(int vY = 0; vY < mOriginalIP.getHeight(); vY++){
			mLabelImage.set(0, vY, mForbiddenLabel);
			mLabelImage.set(mOriginalIP.getWidth()-1,vY,mForbiddenLabel);
		}
		for(int vX = 0; vX < mOriginalIP.getWidth(); vX++){
			mLabelImage.set(vX, 0, mForbiddenLabel);
			mLabelImage.set(vX, mOriginalIP.getHeight()-1, mForbiddenLabel);
		}
		
		Roi vRectangleROI = new Roi(10, 10, mOriginalIP.getWidth()-20, mOriginalIP.getHeight()-20);
		mLabelImage.setValue(1);
		mLabelImage.fill(vRectangleROI);
		
		for(int vY = 0; vY < mOriginalIP.getHeight(); vY++){
			for(int vX = 0; vX < mOriginalIP.getWidth(); vX++){
				///hier weiter:
				
			}
		}
		
		new ImagePlus("LabelImage", mLabelImage).show();
	}
}
