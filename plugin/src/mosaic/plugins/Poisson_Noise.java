package mosaic.plugins;


import java.awt.Choice;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.stats.Histogram;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Erode;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import mosaic.core.utils.Connectivity;
import mosaic.core.utils.FloodFill;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.MultipleThresholdImageFunction;
import mosaic.core.utils.MultipleThresholdLabelImageFunction;
import mosaic.core.utils.MosaicUtils.SegmentationInfo;
import mosaic.core.utils.Point;
import mosaic.core.utils.Point.PointFactory;
import mosaic.core.utils.Point.PointFactoryInterface;
import mosaic.noise_sample.GenericNoiseSampler;
import mosaic.noise_sample.NoiseSample;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/**
 * A ImageJ Plugin that inserts Noise to an image or an image stack. 
 * @author Pietro Incardona & Janick Cardinale, MPI-CBG Dresden
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
	int erodePixel;
	public int mSeed = 8888;
	private Random mRandomGenerator;
	private static final int BYTE=0, SHORT=1, FLOAT=2;
	NoiseSample<?> ns;
	SegmentationInfo seg;
	
	<T>int getNBins(T t)
	{
		if (t instanceof UnsignedByteType)
		{
			return 256;
		}
		else if (t instanceof ShortType)
		{
			return 16536;
		}
		else if (t instanceof FloatType)
		{
			return 16536;
		}
		return 0;
	}
	/**
	 * 
	 * Erode one iteration
	 * 
	 * @param seg image
	 * @param cnv Connectivity
	 */
	
	private <S extends IntegerType<S>> void erode(Img<S> seg, Connectivity cnv, S bck)
	{
		Vector<Point> pToErode = new Vector<Point>();
		int lc[] = new int[seg.numDimensions()];
		Cursor<S> cur = seg.cursor();
		RandomAccess<S> ra = seg.randomAccess();
		
		while (cur.hasNext())
		{
			boolean toErode = false;
			
			if (cur.get().getInteger() == bck.getInteger())
				continue;
			
			for (Point p : cnv)
			{
				cur.localize(lc);
				Point pos = Point.CopyLessArray(lc);
				pos = pos.add(p);
				ra.setPosition(pos.x);
				
				if (ra.get().getInteger() == bck.getInteger())
				{
					toErode = true;
				}
			}
			
			if (toErode == true)
			{
				cur.localize(lc);
				pToErode.add(Point.CopyLessArray(lc));
			}
		}
		
		// now erode
		
		for (int i = 0 ; i < pToErode.size() ; i++)
		{
			ra.setPosition(pToErode.get(i).x);
			ra.get().set(bck);
		}
	}
	
	private <T,S extends IntegerType<S>> void createHistogramsFromSegImage(Img<S> seg , Class<S> cls)
	{
		// ImgLib2 does not have Erosion for dimension bigger that 2
		// so manual implementation
		
		Connectivity cnv = new Connectivity(seg.numDimensions(),seg.numDimensions()-1);
		
		for (int i = 0 ; i < erodePixel ; i++)
		{
			S s = null;
			try {s = cls.newInstance();}
			catch (InstantiationException e) {e.printStackTrace();}
			catch (IllegalAccessException e) {e.printStackTrace();}
			s.setZero();
			erode(seg,cnv,s);
		}
		
		// Find connected regions
		
		//TODO ! test this
		
/*		HashSet<Integer> oldLabels = new HashSet<Integer>();		// set of the old labels
		ArrayList<Integer> newLabels = new ArrayList<Integer>();	// set of new labels
		
		int newLabel=1;
		
		int size=iterator.getSize();
		
		// what are the old labels?
		for(int i=0; i<size; i++)
		{
			int l=getLabel(i);
			if(l==forbiddenLabel || l==bgLabel)
			{
				continue;
			}
			oldLabels.add(l);
		}
		
		for(int i=0; i<size; i++)
		{
			int l=getLabel(i);
			if(l==forbiddenLabel || l==bgLabel)
			{
				continue;
			}
			if(oldLabels.contains(l))
			{
				// l is an old label
				MultipleThresholdImageFunction aMultiThsFunctionPtr = new MultipleThresholdLabelImageFunction(this);
				aMultiThsFunctionPtr.AddThresholdBetween(l, l);
				FloodFill ff = new FloodFill(connFG, aMultiThsFunctionPtr, iterator.indexToPoint(i));
				
				//find a new label
				while(oldLabels.contains(newLabel)){
					newLabel++;
				}
				
				// newLabel is now an unused label
				newLabels.add(newLabel);
				
				// set region to new label
				for(Point p:ff)
				{
					setLabel(p, newLabel);
				}
				// next new label
				newLabel++;
			}
		}*/
		
		// 
	}
	
	private <T> void setupGenericNoise(Class<T> cls)
	{
		// Create a generic noise
		
		GenericNoiseSampler gns = new GenericNoiseSampler(cls);
		
		// if segmentation is present process it
		
		if (seg != null)
		{
			try {
				int nbin = getNBins(cls.newInstance());
			} catch (InstantiationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			// check the type of the segmentation image
			
			ImgOpener imgOpener = new ImgOpener();
			File rgm = seg.RegionMask;
	        
			// Try to convert to unsigned byte, short and integer
			// to infer the type
			
			boolean convertedUb = false;
			boolean convertedS = false;
			boolean convertedI = false;

			try
			{
				Img< UnsignedByteType > imageSeg = imgOpener.openImg( rgm.getAbsolutePath() );
				convertedUb = true;
			}
			catch (IncompatibleTypeException e) {}
			catch(ClassCastException e) {}
			catch(ImgIOException e) {}
			
			try
			{
				Img< ShortType > imageSeg = imgOpener.openImg( rgm.getAbsolutePath() );
				convertedS = true;
			}
			catch (IncompatibleTypeException e) {}
			catch(ClassCastException e) {}
			catch(ImgIOException e) {}
			
			try
			{
				Img< IntType > imageSeg = imgOpener.openImg( rgm.getAbsolutePath() );
				convertedI = true;
			}
			catch (IncompatibleTypeException e) {}
			catch(ClassCastException e) {}
			catch(ImgIOException e) {}
			
			// Open the segmentation image, erode filter small region,
			// and create histograms
	        
			
			
//			Histogram<T> hist = new Histogram<T>();
		}
		
//		gns.setHistogram(intensity, hist);
	}
	
	
	public int setup(String aArgs, ImagePlus aImp) 
	{
		mRandomGenerator = new Random(mSeed);
		//run(new FloatProcessor(1,1));		
		//return DONE;
		
		// Ask if you have an image or you know the noise type
		
		YesNoCancelDialog YNd = new YesNoCancelDialog(null, "Noise type","Do you know the noise or you do not have an image that replicate the noise ? \n"
				                                                         + " 1) You do not have an image of the noise you are tring to simulate. Press (Yes) \n"
																		 + " 2) You have an image of the noise you are tring to simulate. Press (No) \n"
				                                                         + " 3) You have an image of the noise, but you are EXACTLY sure that the noise is Poisson or Gaussian ..., Press (No)");
		YNd.show(true);
		
		if (YNd.yesPressed())
		{
			GenericDialog gd = new GenericDialog("Select the source image");
			Choice cc = MosaicUtils.chooseImage(gd,aImp);
			
			gd.showDialog();
			
			// get the image
			
			String selImage = cc.getItem(cc.getSelectedIndex());
			ImagePlus imgs = WindowManager.getImage(selImage);
			
			if (imgs != null)
			{
				// Check if you have segmentation information
				
				if (MosaicUtils.checkSegmentationInfo(aImp))
				{					
					// Ask to the user if we will gone use it
					
					YesNoCancelDialog YNs = new YesNoCancelDialog(null, "Segmentation found","Do you want to use the segmentation to collect information on the noise profile");
					YNs.show(true);
					
					if (YNs.yesPressed())
					{
						// We will gonna use the segmentation information
						
						// get the segmentation
						
						seg = MosaicUtils.getSegmentationInfo(aImp);
						
						// Ask if the user know PSF to erode
						
						GenericDialog gde = new GenericDialog("PSF to erode");
						
						gde.addNumericField("N iteration", 0, 0);
						
						gde.addMessage("In order to eliminate border effect produced by the PSF, indicate how much to erode the regions");
						
						gde.showDialog();
						
						if (gde.wasOKed())
						{
							erodePixel = (int) gde.getNextNumber();
						}
					}
				}
			}
			
/*	        if (aImp.getProcessor() instanceof ByteProcessor)
	        {<UnsignedByteType>setupGenericNoise();}
	        else if (aImp.getProcessor() instanceof ShortProcessor)
	        {vType = SHORT;}
	        else if (aImp.getProcessor() instanceof FloatProcessor)
	        {vType = FLOAT;}
	        else {
	        	IJ.showMessage("Wrong image type");
	        	return;
	        }
			
			setupGenericNoiseSample();*/
		}
		else
		{
			
		}
		
//		YesNoCancelDialog YNd = new YesNoCancelDialog("Do you know the noise ?","Bhoo","Bhoo");
		
		// Ask to select some ROI
		
		return DOES_ALL - DOES_RGB + DOES_STACKS;
	}
	
	public void run(ImageProcessor aImageProcessor) {
		
		// Get the segmented image if needed erode it calculate the mean
		
		
		// Collect the point in the range of Mean +- Epsilon
				
		
		// Create the interpolation scheme
		
		// Sample from it
		
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
