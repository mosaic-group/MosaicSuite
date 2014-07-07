package mosaic.core.utils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.utils.IndexIterator;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;
import mosaic.core.utils.Connectivity;
import mosaic.region_competition.utils.IntConverter;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.FileSaver;
import ij.plugin.GroupedZProjector;
import ij.plugin.ZProjector;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/*
//TODO TODOs
- refactor LabelImage, extract what's not supposed to be there (eg energy calc)
- does merging criterion have to be tested multiple times?
*/

public class LabelImage// implements MultipleThresholdImageFunction.ParamGetter<Integer>
{
//	ImagePlus imageIP;	// input image
//	ImageProcessor imageProc;
	
//	private final float imageMax; 	// maximal intensity of input image
	
	protected ImageProcessor labelIP;				// map positions -> labels
	protected ImagePlus labelPlus;
//	public float[] dataIntensity;
	public int[] dataLabel;
//	public short[] dataLabelShort;
	
	
	
	int size;
	protected int dim;			// number of dimension
	protected int[] dimensions;	// dimensions (width, height, depth, ...)
	protected int width = 0;
	protected int height = 0;
	public IndexIterator iterator; // iterates over the labelImage
	
	
	public final int bgLabel = 0;
	final int negOfs = 10000;			// labels above this number stands for "negative numbers" (problem with displaying negative numbers in ij.ShortProcessor)
//	private int m_MaxNLabels;
//	private LabelDispenser labelDispenser;
	
	protected Connectivity connFG;
	protected Connectivity connBG;

	/**
	 * 
	 * Create a label image from an ImgLib2 image
	 * use always native type for computation are
	 * much faster than imgLib2
	 * 
	 */
	
	public <T extends IntegerType<T>>LabelImage(Img<T> lbl)
	{
		// Get the image dimensions
		
		int dimensions[] = MosaicUtils.getImageIntDimensions(lbl);
		
		// get int dimension
		
		init(dimensions);
		initImgLib2(lbl);
		iterator = new IndexIterator(dimensions);
	}
	
	/**
	 * 
	 * Create a labelImage from another label Image
	 * 
	 * @param l LabelImage
	 */
	public LabelImage(LabelImage l)
	{
		init(l.getDimensions());
		initWithIP(l.labelPlus);
		iterator = new IndexIterator(l.getDimensions());
	}
	
	/**
	 * 
	 * Create an empty label image of a given dimension
	 * 
	 * @param dims dimensions of the LabelImage
	 */
	
	public LabelImage(int dims[]) 
	{
		init(dims);
	}
	
	/**
	 * 
	 * Check of p is inside the label image
	 * 
	 * @param p Point
	 * @return true if is inside
	 */
	
	public boolean isOutOfBound(Point p)
	{
		for (int i = 0 ; i < p.x.length ; i++)
		{if (p.x[i] < 0) return true; if (p.x[i] >= dimensions[i]) return true;}
		return false;
	}
	
	
	protected void init(int dims[])
	{
		initDimensions(dims);
		initConnectivities(dim);
		iterator = new IndexIterator(dims);
		initLabelData();
	}
	
	
	private void initDimensions(int[] dims)
	{
		this.dimensions = dims;
		this.dim = dimensions.length;
		if(dim>3)
		{
			throw new RuntimeException("Dim > 3 not supported");
		}
		
		this.width = dims[0];
		this.height = dims[1];
		
		size=1;
		for(int i=0; i<dim; i++)
		{
			size *= dimensions[i];
		}
	}
	
	
	private void initLabelData()
	{
		if(dim==3){
			labelPlus = null;
			labelIP = null;
			dataLabel = new int[size];
		}
		else
		{
			labelIP = new ColorProcessor(width, height);
			dataLabel = (int[])labelIP.getPixels();
			labelPlus = new ImagePlus("LabelImage", labelIP);
		}
	}
	
	private void initConnectivities(int d)
	{
		connFG = new Connectivity(d, d-1);
		connBG = connFG.getComplementaryConnectivity();
	}
	
	public ImagePlus getLabelPlus()
	{
		return labelPlus;
	}
	
	//////////////////////////////////////////////////////////////////
	
    /**
     * Initializes label image data to all zero
     */
	public void initZero()
	{
		for(int i=0; i<size; i++)
		{
			setLabel(i, 0);
		}
	}
	
	/**
	 * Only 2D
	 * Initializes label image with a predefined IP (by copying it)
	 * ip: without contour pixels/boundary
	 * (invoke initBoundary() and generateContour();
	 */
	@Deprecated
	public void initWithImageProc(ImageProcessor ip)
	{
		//TODO check for dimensions etc
		this.labelIP=IntConverter.procToIntProc(ip);
		this.dataLabel=(int[])labelIP.getPixels();
		this.labelPlus = new ImagePlus("labelImage", labelIP);
//		this.dataLabelShort =(short[])ip.getPixels();
	}
	
	/**
	 * LabelImage loaded from an imgLib2 image
	 * 
	 * @param imgLib2
	 * 
	 */
	private <T extends IntegerType<T>>void initImgLib2(Img<T> img)
	{
		RandomAccess<T> ra = img.randomAccess();
		
		// Create a region iterator
		
		RegionIterator rg = new RegionIterator(MosaicUtils.getImageIntDimensions(img));
		
		// load the image
		
		while (rg.hasNext())
		{
			Point p = rg.getPoint();
			int id = rg.next();
			
			ra.setPosition(p.x);
			dataLabel[id] = ra.get().getInteger();
		}
	}
	
	/**
	 * LabelImage loaded from file
	 */
	public void initWithIP(ImagePlus imagePlus)
	{
		ImagePlus ip = IntConverter.IPtoInt(imagePlus);
		
		if(dim==3)
		{
			this.labelPlus = ip; 
			ImageStack stack = ip.getImageStack();
			this.dataLabel = IntConverter.intStackToArray(stack);
			this.labelIP = null;
		}
		if(dim==2)
		{
			initWithImageProc(ip.getProcessor());
		}
	}
	
	
	/**
	 * 
	 * Close all the images
	 * 
	 */
	
	public void close()
	{
		if (labelPlus != null)
			labelPlus.close();
	}
	
	/**
	 * @param stack Stack of Int processors
	 */
	public void initWithStack(ImageStack stack)
	{
		this.dataLabel = IntConverter.intStackToArray(stack);
	}
	
	/**
	 * 
	 * Save the label image as tiff
	 * 
	 * @param file where to save (full or relative path)
	 */
	
	public void save(String file)
	{
		// Remove eventually the "file:" string
		
		if (file.indexOf("file:") >= 0)
			file = file.substring(file.indexOf("file:")+5);
		
		ImagePlus ip = convert("save",256);
		IJ.save(ip,file);
		ip.close();
	}
	
	public ImageProcessor getLabelImageProcessor()
	{
		return labelIP;
	}
	
	/**
	 * Gets a copy of the labelImage as a short array.
	 * @return short[] representation of the labelImage
	 */
	public short[] getShortCopy()
	{	
		final int n = dataLabel.length;
		
		short[] shortData = new short[n];
		for(int i=0; i<n; i++)
		{
			shortData[i] = (short)dataLabel[i];
		}
		return shortData;
	}
	
	public ImagePlus convert(Object title, int maxl)
	{
		if(getDim()==3)
		{
			ImagePlus imp = new ImagePlus("ResultWindow "+title, this.get3DShortStack(true));
			
//			IJ.setMinAndMax(imp, 0, maxl);
//			IJ.run(imp, "3-3-2 RGB", null);
			
//			imp.show();
			return imp;
		}
		
		// Colorprocessor doesn't support abs() (does nothing). 
//		li.absAll();
		ImageProcessor imProc = getLabelImageProcessor();
//		System.out.println(Arrays.toString((int[])(imProc.getPixels())));
		
		// convert it to short
		short[] shorts = getShortCopy();
		for(int i=0; i<shorts.length; i++){
			shorts[i] = (short)Math.abs(shorts[i]);
		}
		ShortProcessor shortProc = new ShortProcessor(imProc.getWidth(), imProc.getHeight());
		shortProc.setPixels(shorts);
		
//		TODO !!!! imProc.convertToShort() does not work, first converts to byte, then to short...
		String s = "ResultWindow "+title;
		String titleUnique = WindowManager.getUniqueName(s);
		
		ImagePlus imp = new ImagePlus(titleUnique, shortProc);
		IJ.setMinAndMax(imp, 0, maxl);
		IJ.run(imp, "3-3-2 RGB", null);
		return imp;
	}
	
	public ImagePlus show(Object title, int maxl)
	{		
		ImagePlus imp = convert(title,maxl);
		imp.show();
		return imp;
	}

	/* Particles delete */
	
	public void deleteParticles()
	{
		for(int i=0; i<size; i++)
		{
			setLabel(i,getLabelAbs(i));
		}
	}
	
	/**
	 * 
	 * Get an ImgLib2 from a intensity image
	 * 
	 * @return an ImgLib2 image
	 * 
	 */
	
	public <T extends NativeType<T> & IntegerType<T> > Img<T> getImgLib2(Class<T> cls)
	{
		long lg[] = new long[getDim()];
		
		// Take the size
		
		ImgFactory< T > imgFactory = new ArrayImgFactory< T >( );
		
		for (int i = 0 ; i < getDim() ; i++)
		{
			lg[i] = getDimensions()[i];
		}
		
        // create an Img of the same type of T and size of the imageLabel
        
        Img<T> it = null;
		try {
			it = imgFactory.create(lg , cls.newInstance() );
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		RandomAccess<T> randomAccess_it = it.randomAccess();
		
		// Region iterator
		
		RegionIterator ri = new RegionIterator(getDimensions());
		
		while (ri.hasNext())
		{
			Point p = ri.getPoint();
			int id = ri.next();
			
			randomAccess_it.setPosition(p.x);
			randomAccess_it.get().setInteger(dataLabel[id]);
		}
		
		return it;
	}
	
	/**
	 * Gives disconnected components in a labelImage distinct labels
	 * bg and forbidden label stay the same
	 * contour labels are treated as normal labels, 
	 * so use this function only for BEFORE contour particles are added to the labelImage
	 * (eg. to process user input for region guesses)
	 * @param li LabelImage
	 */
	public void connectedComponents()
	{
		//TODO ! test this
		
		HashSet<Integer> oldLabels = new HashSet<Integer>();		// set of the old labels
		ArrayList<Integer> newLabels = new ArrayList<Integer>();	// set of new labels
		
		int newLabel=1;
		
		int size=iterator.getSize();
		
		// what are the old labels?
		for(int i=0; i<size; i++)
		{
			int l=getLabel(i);
			if(l==bgLabel)
			{
				continue;
			}
			oldLabels.add(l);
		}
		
		for(int i=0; i<size; i++)
		{
			int l=getLabel(i);
			if(l==bgLabel)
			{
				continue;
			}
			if(oldLabels.contains(l))
			{
				// l is an old label
				BinarizedIntervalLabelImage aMultiThsFunctionPtr = new BinarizedIntervalLabelImage(this);
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
		}
		
//		labelDispenser.setLabelsInUse(newLabels);
//		for(int label: oldLabels)
//		{
//			labelDispenser.addFreedUpLabel(label);
//		}
		
	}

	public void initContour()
	{
		Connectivity conn = connFG;
		
		for(int i: iterator.getIndexIterable())
		{
			int label=getLabelAbs(i);
			if(label!=bgLabel) // region pixel
				// && label<negOfs
			{
				Point p = iterator.indexToPoint(i);
				for(Point neighbor : conn.iterateNeighbors(p))
				{
					int neighborLabel=getLabelAbs(neighbor);
					if(neighborLabel!=label)
					{
						setLabel(p, labelToNeg(label));
						
						break;
					}
				}
				
			} // if region pixel
		}
	}	
	
	/**
	 * 
	 * Is the point at the boundary
	 * 
	 * @param aIndex Point
	 * @return true if is at the boundary false otherwise
	 */
	
	public boolean isBoundaryPoint(Point aIndex)
	{
		int vLabelAbs = getLabelAbs(aIndex);
		for(Point q : connFG.iterateNeighbors(aIndex)) 
		{
			if(getLabelAbs(q) != vLabelAbs)
				return true;
		}

		return false;
	}


	/**
	 * is point surrounded by points of the same (abs) label
	 * @param aIndex
	 * @return
	 */
	public boolean isEnclosedByLabel(Point pIndex, int pLabel)
	{
		int absLabel = labelToAbs(pLabel);
		Connectivity conn = connFG;
		for(Point qIndex : conn.iterateNeighbors(pIndex))
		{
			if(labelToAbs(getLabel(qIndex))!=absLabel)
			{
				return false;
			}
		}
		return true;
	}	
	
	protected boolean isInnerLabel(int label)
	{
		if(label == bgLabel || isContourLabel(label)) {
			return false;
		} else {
			return true;
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * @param label
	 * @return true, if label is a contour label
	 * ofset version
	 */
	public boolean isContourLabel(int label)
	{
		return (label<0);
//		if(isForbiddenLabel(label)) {
//			return false;
//		} else {
//			return (label > negOfs);
//		}
	}
	
	/**
	 * 
	 * return the label at the position index (linearized)
	 * 
	 * @param index position
	 * @return the label value
	 */
	
	public int getLabel(int index)
	{
		return dataLabel[index];
//		return labelIP.get(index);
	}
	
	/**
	 * @param p
	 * @return Returns the (raw; contour information) value of the LabelImage at Point p. 
	 */
	public int getLabel(Point p) {
		int idx = iterator.pointToIndex(p);
		return dataLabel[idx];
//		return getLabel(idx);
	}
	

	/**
	 * @return the abs (no contour information) label at Point p
	 */
	public int getLabelAbs(Point p)
	{
		int idx = iterator.pointToIndex(p);
		
		if (idx >= dataLabel.length)
		{
			int debug = 0;
			debug++;
		}
		
		return Math.abs(dataLabel[idx]);
//		return labelToAbs(getLabel(p));
	}

	public int getLabelAbs(int idx)
	{
		if (idx >= dataLabel.length)
		{
			int debug = 0;
			debug++;
		}
		
		return Math.abs(dataLabel[idx]);
//		return labelToAbs(getLabel(idx));
	}

	
	/**
	 * sets the labelImage to val at point x,y
	 */
	public void setLabel(int idx, int label) 
	{
		dataLabel[idx]=label;
//		dataLabel[idx]=(short)val;
//		labelIP.set(idx, val);
	}

	/**
	 * sets the labelImage to val at Point p
	 */
	public void setLabel(Point p, int label) {
		int idx = iterator.pointToIndex(p);
		dataLabel[idx]=label;
//		setLabel(idx, label);
	}
	
	void setLabelNeg(Point p, int label)
	{
		int idx = iterator.pointToIndex(p);
		dataLabel[idx]=-Math.abs(label);
//		setLabel(p, labelToNeg(label));
	}
	
	void setLabelNeg(int idx, int label)
	{
		dataLabel[idx]=-Math.abs(label);
//		setLabel(idx, labelToNeg(label));
	}
	
	

	/**
	 * @param label a label
	 * @return if label was a contour label, get the absolute/inner label
	 */
	public int labelToAbs(int label) {
		return Math.abs(label);
//		if (isContourLabel(label)) {
//			return label - negOfs;
//		} else {
//			return label;
//		}
	}

	/**
	 * @param label a label
	 * @return the contour form of the label
	 */
	protected int labelToNeg(int label) 
	{
		if (label==bgLabel || isContourLabel(label)) {
			return label;
		} else {
			return -label;
//			return label + negOfs;
		}
	}
	

	/**
	 * @return The number of dimensions of this LabelImage
	 */
	public int getDim()
	{
		return this.dim;
	}

	/**
	 * The size of each dimension of this LabelImage as an int array
	 * @return Reference to the dimensions
	 */
	public int[] getDimensions()
	{
		return this.dimensions;
	}
	
	/**
	 * @return The number of pixels of this LabelImage
	 */
	public int getSize()
	{
		return this.size;
	}
	
	/**
	 * @return Connectivity of the foreground
	 */
	public Connectivity getConnFG() {
		return connFG;
	}

	/**
	 * @return Connectivity of the background
	 */
	public Connectivity getConnBG() {
		return connBG;
	}
	
	/**
	 * if 3D image, converts to a stack of ShortProcessors
	 * @return
	 */
	public ImageStack get3DShortStack(boolean clean)
	{
		int dims[] = getDimensions();
		int labeldata[] = dataLabel;
		
		ImageStack stack = IntConverter.intArrayToShortStack(labeldata, dims, clean);
		
//		ImagePlus ip = new ImagePlus();
//		ip.setStack(stack);
//		ip.show();
		
		return stack;
	}

	/////////////////////////////////////////////////////////////

	static void debug(Object s)
	{
		System.out.println(s);
	}
}


