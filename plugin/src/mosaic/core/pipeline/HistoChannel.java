package mosaic.core.pipeline;



import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * It try to figure out the type of the image looking at the histogram.
 * @author Pietro Incardona
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
public class HistoChannel implements PlugInFilter
{
	double max;
	double min;
	double mean;
	double stdDev;
	int peaks;
	double GaussBS[];
	
	class GaussBlurStat
	{
		int npeak;
		double minPeak;
		double maxPeak;
		double meanPeak;
		double minPeakDev;
		double maxPeakDev;
		double meanPeakDev;
		double Squaredeviation;
	}
	
	public void run(ImageProcessor aImP)
	{
		ImageStatistics st = aImP.getStatistics();
		
		max = st.max;
		min = st.min;
		mean = st.mean;
		stdDev = st.stdDev;
		
/*		Kull_Lulla = new double [5];
		
		// Five step of gaussian blur
		
		
		for (int i = 0 ; i < 5 ; i++)
		{
			
			
			Knull_();
		}*/
	}

	@Override
	public int setup(String arg0, ImagePlus arg1) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public double getMax()
	{
		return max;
	}
	
	public double getMin()
	{
		return min;
	}
	
	public double getStd()
	{
		return stdDev;
	}
	
	public double getMean()
	{
		return mean;
	}
	
}
