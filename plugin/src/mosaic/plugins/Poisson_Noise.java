package mosaic.plugins;


import java.awt.Choice;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.histogram.BinMapper1d;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Integer1dBinMapper;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.Img;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Erode;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import mosaic.core.binarize.BinarizedIntervalImgLib2Int;
import mosaic.core.utils.Connectivity;
import mosaic.core.utils.FloodFill;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.MosaicUtils.SegmentationInfo;
import mosaic.core.utils.Point;
import mosaic.core.utils.Point.PointFactory;
import mosaic.core.utils.Point.PointFactoryInterface;
import mosaic.noise_sample.GenericNoiseSampler;
import mosaic.noise_sample.NoiseSample;
import mosaic.noise_sample.noiseList;
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
 * @author Pietro Incardona, MPI-CBG Dresden
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
	
	interface ConnectedRegionsCB
	{
		public void regionProcess(Set<Point> pnt);
	}
	
	
	private <S extends IntegerType<S>> void FindConnectedRegions(Img<S> seg, ConnectedRegionsCB cb, S bck)
	{
		int lc[] = new int [seg.numDimensions()];
		HashMap<Integer,Point> LabelsList = new HashMap<Integer,Point>();		// set of the old labels
		
		Cursor<S> cur = seg.cursor();
		
		// Take all the labels in the image != bck
		
		while (cur.hasNext())
		{
			cur.next();
			if (cur.get().getInteger() == bck.getInteger() &&  LabelsList.get(cur.get().getInteger()) == null)
			{
				cur.localize(lc);
				Point p = new Point(lc);
				LabelsList.put(cur.get().getInteger(),p);
			}
		}
		
		// Create connectivity
		
		Connectivity cnv = new Connectivity(seg.numDimensions(),seg.numDimensions()-1);
		BinarizedIntervalImgLib2Int<S> img = new BinarizedIntervalImgLib2Int<S>(seg);
		
		// Now flood fill to find regions
		
		for (Map.Entry<Integer,Point> entry : LabelsList.entrySet()) 
		{
			img.AddThresholdBetween(entry.getKey(), entry.getKey());;
		    FloodFill ff = new FloodFill(cnv,img,entry.getValue());
		    
		    // Found a region and process it
		    
		    cb.regionProcess(ff.getPoints());
		}
	}
	
	class processCCRegion<T extends RealType<T>> implements ConnectedRegionsCB
	{
		Img<T> img;
		HashMap<T,Histogram1d<T>> hset;
		Class<T> cls;
		
		double max;
		double min;
		
		double intervalDelta;
		double deltaHalf;
		
		processCCRegion(Class<T> cls_)
		{
//			ComputeMinMax<T> cMM = new ComputeMinMax<T>(img);
			
			int Nbin = getNBins(cls);
			
			intervalDelta = (max - min) / Nbin;
			deltaHalf = intervalDelta / 2;
			cls = cls_;
		}
		
		/**
		 * 
		 * Return the histograms
		 * 
		 * @return histograms
		 */
		
		HashMap<T,Histogram1d<T>> getHist()
		{
			return hset;
		}
		
		/**
		 * 
		 * Create an integer bin mapper
		 * 
		 * @param nbin number of bins
		 * @return the integer bin mapper
		 */
		
		private <S extends IntegerType<S>> BinMapper1d<S> createIntegerMapper(int nbin)
		{
			return new Integer1dBinMapper<S>(0,nbin,true);
		}
		
		/**
		 * 
		 * Create a bin mapper
		 * 
		 * @param cls class
		 * @param min value
		 * @param max value
		 * @return The bin mapper
		 */
		
		@SuppressWarnings("unchecked")
		private BinMapper1d<T> createMapper(Class<T> cls,double min,double max)
		{
			T test = null;
			try {
				test = cls.newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (test instanceof UnsignedByteType)
				return (BinMapper1d<T>) this.<UnsignedByteType>createIntegerMapper(256);
			else if (test instanceof ShortType)
				return (BinMapper1d<T>) this.<ShortType>createIntegerMapper(65536);
			else if (test instanceof FloatType)
				return new Real1dBinMapper<T>(min,max,65536,true);
			return null;
		}
		
		@Override
		public void regionProcess(Set<Point> pnt) 
		{
			Iterator<Point> pnt_it = pnt.iterator();
			
			// filter out if the region is smaller that 30 pixel
			
			if (pnt.size() < 30)
				return;
			
			// Calculate the mean
			
			int tot_num = 0;
			double mean = 0.0;
			RandomAccess<T> ra = img.randomAccess();
			
			while (pnt_it.hasNext())
			{
				Point p = pnt_it.next();
				ra.setPosition(p.x);
				mean += ra.get().getRealDouble();
				tot_num++;
			}
			
			mean /= tot_num;
			
			// Get the center of the mean
			
			int cbin = (int) (mean / intervalDelta);
			mean = cbin * intervalDelta + deltaHalf; 
			
			// Get the histogram
			
			Histogram1d<T> hs = null;
			
			if ((hs = hset.get(mean)) == null)
			{
				// Create mapper
				
				BinMapper1d<T> b1d = createMapper(cls);
				
				// Create histogram
				
				Histogram1d<T> hist = new Histogram1d<T>(b1d);
				T meanT = cls.newInstance();
				meanT.setReal(mean);
				hset.put(meanT, hist);
				hs = hist;
			}
			
			// Add data
			
			@SuppressWarnings("unchecked")
			T [] ipnt = (T[]) Array.newInstance(cls, pnt.size());
			
			while (pnt_it.hasNext())
			{
				Point p = pnt_it.next();
				ra.setPosition(p.x);
				mean += ra.get().getRealDouble();
			}
			
			hs.addData(Arrays.asList(ipnt));;
		}
		
	}
	
	/**
	 * 
	 * Create histograms from segmented image
	 * 
	 * @param seg segmented image
	 * @param gn structure that store the noise profile
	 * @param clsS segmented type
	 * @param clsT intentity type
	 */
	
	private <T extends RealType<T>,S extends IntegerType<S>> void createHistogramsFromSegImage(Img<S> seg , GenericNoiseSampler<T> gn , Class<S> clsS, Class<T> clsT)
	{
		// ImgLib2 does not have Erosion for dimension bigger that 2
		// so manual implementation
		
		Connectivity cnv = new Connectivity(seg.numDimensions(),seg.numDimensions()-1);
		
		for (int i = 0 ; i < erodePixel ; i++)
		{
			S s = null;
			try {s = clsS.newInstance();}
			catch (InstantiationException e) {e.printStackTrace();}
			catch (IllegalAccessException e) {e.printStackTrace();}
			s.setZero();
			erode(seg,cnv,s);
		}
		
		// Find connected regions
		
		processCCRegion<T> pp = new processCCRegion<T>(clsT);
		S bck = null;
		try {
			bck = clsS.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		bck.setZero();
		FindConnectedRegions(seg,pp,bck);
		
		// Founded the connected regions and get processed the histograms
		// add them to GenericNoise
		
		HashMap<T,Histogram1d<T>> hists = pp.getHist();
		
		for (Map.Entry<T,Histogram1d<T>> entry : hists.entrySet()) 
		{
			// Ad the histograms to the general noise
			
			gn.setHistogram(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * 
	 * Process generic noise from segmentation
	 * 
	 * @param cls Class<T>
	 */
	
	private <T extends RealType<T>> void setupGenericNoise(Class<T> cls)
	{
		// Create a generic noise
		
		GenericNoiseSampler<T> gns = new GenericNoiseSampler<T>(cls);
		
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

			try
			{
				Img< UnsignedByteType > imageSeg = imgOpener.openImg( rgm.getAbsolutePath() );

				// Open the segmentation image, erode filter small region,
				// and create histograms
		        
				createHistogramsFromSegImage(imageSeg,gns,UnsignedByteType.class,cls);
			}
			catch (IncompatibleTypeException e) {}
			catch(ClassCastException e) {}
			catch(ImgIOException e) {}
			
			try
			{
				Img< ShortType > imageSeg = imgOpener.openImg( rgm.getAbsolutePath() );
				
				// Open the segmentation image, erode filter small region,
				// and create histograms
		        
				createHistogramsFromSegImage(imageSeg,gns,ShortType.class,cls);
			}
			catch (IncompatibleTypeException e) {}
			catch(ClassCastException e) {}
			catch(ImgIOException e) {}
			
			try
			{
				Img< IntType > imageSeg = imgOpener.openImg( rgm.getAbsolutePath() );
				
				// Open the segmentation image, erode filter small region,
				// and create histograms
		        
				createHistogramsFromSegImage(imageSeg,gns,IntType.class,cls);
				
			}
			catch (IncompatibleTypeException e) {}
			catch(ClassCastException e) {}
			catch(ImgIOException e) {}
		}
		
		// Create histograms by ROI
		
		
	}
	
	
	public int setup(String aArgs, ImagePlus aImp) 
	{
		mRandomGenerator = new Random(mSeed);
		//run(new FloatProcessor(1,1));		
		//return DONE;
		
		// Ask if you have an image or you know the noise type
		
		YesNoCancelDialog YNd = new YesNoCancelDialog(null, "Noise type","Do you know the noise or you do not have an image that replicate the noise ? \n"
				                                                         + " 1) You do not have an image of the noise you are tring to simulate. Press (Yes) \n"															 + " 2) You have an image of the noise you are tring to simulate. Press (No) \n"
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
			GenericDialog gd = new GenericDialog("Choose type of noise");
			
			gd.addChoice("Choose noise model", noiseList.noiseList, noiseList.noiseList[0]);
			
			gd.showDialog();
			
			if (gd.wasCanceled())
				return DONE;
			
			ns = noiseList.factory(gd.getNextChoice());
		}
		
//		YesNoCancelDialog YNd = new YesNoCancelDialog("Do you know the noise ?","Bhoo","Bhoo");
		
		// Ask to select some ROI
		
		return DOES_ALL - DOES_RGB + DOES_STACKS;
	}
	
	public void run(ImageProcessor aImageProcessor) 
	{
		// Get the Type
		
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
		
		// We do not have a noise model (yet)
		
		if (ns == null)
		{
			if (vType == BYTE)
				this.<UnsignedByteType>setupGenericNoise(UnsignedByteType.class);
			else if (vType == SHORT)
				this.<ShortType>setupGenericNoise(ShortType.class);
			else if (vType == FLOAT)
				this.<FloatType>setupGenericNoise(FloatType.class);
		}
		
		// Sample from it
        
        
       /* switch (vType) {
        case BYTE:
        {
        	byte[] vPixels = (byte[])aImageProcessor.getPixels();
        	for(int vI = 0; vI < vPixels.length; vI++) 
        	{
        		vPixels[vI] = (byte)ns.sample(x, out);((int)(vPixels[vI]+.5f));
        	}
        	break;
        }
        case SHORT:
        {
        	@SuppressWarnings("unchecked")
			NoiseSample<UnsignedByteType> ns_ = (NoiseSample<UnsignedByteType>) ns;
        	short[] vPixels = (short[])aImageProcessor.getPixels();
        	for(int vI = 0; vI < vPixels.length; vI++) 
        	{
        		vPixels[vI] = (short)ns_.sample(x, out);((int)(vPixels[vI]+.5f));
        	}
        	break;
        }
        case FLOAT:
        {
        	float[] vPixels = (float[])aImageProcessor.getPixels();
        	for(int vI = 0; vI < vPixels.length; vI++) 
        	{
        		vPixels[vI] = (float)generatePoissonRV((int)(vPixels[vI]+.5f));
        	}
        	break;
        }
        }*/

	}

}
