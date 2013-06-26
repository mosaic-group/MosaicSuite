package mosaic.region_competition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import mosaic.region_competition.topology.Connectivity;
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
	
	ImageProcessor labelIP;				// map positions -> labels
	ImagePlus labelPlus;				
//	public float[] dataIntensity;
	public int[] dataLabel;
//	public short[] dataLabelShort;
	
	
	
	int size;
	int dim;			// number of dimension
	int[] dimensions;	// dimensions (width, height, depth, ...)
	int width = 0;
	int height = 0;
	public IndexIterator iterator; // iterates over the labelImage
	
	
	public final int forbiddenLabel=Integer.MAX_VALUE; //short
	public final int bgLabel = 0;
	final int negOfs = 10000;			// labels above this number stands for "negative numbers" (problem with displaying negative numbers in ij.ShortProcessor)
//	private int m_MaxNLabels;
//	private LabelDispenser labelDispenser;
	
	private Connectivity connFG;
	private Connectivity connBG;
	
//	ForestFire forestFire;
	
	
	// data structures
	
	/** stores the contour particles. access via coordinates */
//	HashMap<Point, ContourParticle> m_InnerContourContainer;
	
	/** Maps the label(-number) to the information of a label */
	private HashMap<Integer, LabelInformation> labelMap;
//	private HashMap<Point, LabelPair> m_CompetingRegionsMap;
//	private TopologicalNumberImageFunction m_TopologicalNumberFunction;
//	Set<Pair<Point, Integer>> m_Seeds = new HashSet<Pair<Point, Integer>>();

	public LabelImage(LabelImage l)
	{
		init(l.getDimensions());
		initWithIP(l.labelPlus);
		iterator = new IndexIterator(l.getDimensions());
	}
		
	public LabelImage(int dims[]) 
	{
		init(dims);
	}
	
	
	public boolean isOutOfBound(Point p)
	{
		for (int i = 0 ; i < p.x.length ; i++)
		{if (p.x[i] < 0) return true; if (p.x[i] >= dimensions[i]) return true;}
		return false;
	}
	
//	public LabelImage(IntensityImage intensityImage)
//	{
//		int[] dims = intensityImage.getDimensions();
//		init(dims);
//	}
	
//	public LabelImage(ImagePlus ip) 
//	{
//		int[] dims = dimensionsFromIP(ip);
//		init(dims);
//	}
	
	
	private void init(int dims[])
	{
		initDimensions(dims);
		initConnectivities(dim);
		iterator = new IndexIterator(dims);
		initLabelData();
		initMembers();
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
	
	
	void initLabelData()
	{
		if(dim==3){
			labelPlus = null;
			labelIP = null;
			dataLabel = new int[size];
//			dataLabelShort = new short[size];
		}
		else
		{
			// TODO does init twice if guess loaded from file
			labelIP = new ColorProcessor(width, height);
//			labelIP = new ShortProcessor(width, height);
//			labelIP = new FloatProcessor(width, height);
			dataLabel = (int[])labelIP.getPixels();
			
			labelPlus = new ImagePlus("LabelImage", labelIP);
		}
	}
	
	
	public void initMembers()
    {
//		m_InnerContourContainer = new HashMap<Point, ContourParticle>();
		labelMap = new HashMap<Integer, LabelInformation>();
		
//		labelDispenser = new LabelDispenser(negOfs);
//		m_MaxNLabels=0;
    }
	
	
	private void initConnectivities(int d)
	{
		connFG = new Connectivity(d, d-1);
//		if(dim==3)
//			connFG = new Connectivity(3, 1);
		connBG = connFG.getComplementaryConnectivity();
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
		
		initBoundary();
	}
	
	public ImagePlus createMeanImage()
	{
		int nSlices = labelPlus.getNSlices();
		int area = width*height;
		
		for(int i=1; i<=nSlices; i++)
		{
			ImageProcessor ipr = new FloatProcessor(labelIP.getWidth(),labelIP.getHeight());
			float [] pixels = (float[])ipr.getPixels();
			int [] labid = (int [])labelIP.getPixels();
			for(int y=0; y<height; y++)
			{
				for(int x=0; x<width; x++)
				{
					if (labelMap.get(Math.abs(labid[y*width+x])) != null )
						pixels[y*width+x] = (float) labelMap.get(Math.abs(labid[y*width+x])).mean;
				}
			}
			
			ImagePlus ip = new ImagePlus("label_mean",ipr);
			return ip;
		}
		
		return null;
	}
	
	/**
	 * @param stack Stack of Int processors
	 */
	public void initWithStack(ImageStack stack)
	{
		this.dataLabel = IntConverter.intStackToArray(stack);
		initBoundary();
	}
	
	/**
	 * sets the outermost pixels of the labelimage to the forbidden label
	 */
	public void initBoundary()
	{
		for(int idx: iterator.getIndexIterable())
		{
			Point p = iterator.indexToPoint(idx);
			int xs[] = p.x;
			for(int d=0; d<dim; d++)
			{
				int x = xs[d];
				if(x == 0 || x==dimensions[d]-1)
				{
					setLabel(idx, forbiddenLabel);
					break;
				}
			}
		}
	}
	
	void clearStats()
	{
		//clear stats
		for(LabelInformation stat: labelMap.values())
		{
			stat.reset();
		}
	}
	
	public void save(String file)
	{
		FileSaver fs = new FileSaver(convert("save",256));
		
		fs.saveAsTiff(file);
	}
	
	
	public ImagePlus convert(Object title, int maxl)
	{
		if(getDim()==3)
		{
			System.out.println("Unsupported for now");
			return null;
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
	
	public int createStatistics(IntensityImage intensityImage)
	{	
		getLabelMap().clear();
		
		HashSet<Integer> usedLabels = new HashSet<Integer>();
		
		int size = iterator.getSize();
		for(int i=0; i<size; i++)
		{
//			int label = get(x, y);
//			int absLabel = labelToAbs(label);
			
			int absLabel= getLabelAbs(i);

			if (absLabel != forbiddenLabel /* && absLabel != bgLabel*/) 
			{
				usedLabels.add(absLabel);
				
				LabelInformation stats = labelMap.get(absLabel);
				if(stats==null)
				{
					stats = new LabelInformation(absLabel);
					labelMap.put(absLabel, stats);
				}
				double val = intensityImage.get(i);
				stats.count++;
				
				stats.mean+=val; // only sum up, mean and var are computed below
				stats.var = (stats.var+val*val);
			}
		}

		// if background label do not exist add it
		
		LabelInformation stats = labelMap.get(0);
		if(stats==null)
		{
			stats = new LabelInformation(0);
			labelMap.put(0, stats);
		}
		
		// now we have in all LabelInformation: 
		// in mean the sum of the values, in var the sum of val^2
		for(LabelInformation stat: labelMap.values())
		{
			int n = stat.count;
			if (n > 1)
			{
				double var = (stat.var-stat.mean*stat.mean/n)/(n-1);
				stat.var=(var);
//      	        	stat.var = (stat.var - stat.mean*stat.mean / n) / (n-1);
			}
			else
			{
				stat.var = 0;
			}
			
			if (n > 0)
				stat.mean = stat.mean/n;
			else
				stat.mean = 0.0;
			
			// Median on start set equal to mean
			
			stat.median = stat.mean;
		}
		return usedLabels.size();
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
			if(label!=bgLabel && label!=forbiddenLabel) // region pixel
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



	boolean isInnerLabel(int label)
	{
		if(label == forbiddenLabel || label == bgLabel || isContourLabel(label)) {
			return false;
		} else {
			return true;
		}
	}


	public boolean isForbiddenLabel(int label)
	{
		return (label == forbiddenLabel);
	}


	public ImageProcessor getLabelImageProcessor()
	{
		if(dim==3){
			return getProjected3D(true).getProcessor();
		}
		return labelIP;
	}
	
//	public HashMap<Integer, LabelInformation> getLabelMap()
//	{
//		return labelMap;
//	}
	
//	public HashMap<Point, ContourParticle> getContourContainer()
//	{
//		return m_InnerContourContainer;
//	}
	

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
	int labelToNeg(int label) 
	{
		if (label==bgLabel || isForbiddenLabel(label) || isContourLabel(label)) {
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
	
	public HashMap<Integer, LabelInformation> getLabelMap(){
		return this.labelMap;
	}
	
	public Object getSlice()
	{
		if(dim==3)
		{
			return get3DShortStack(false);
		}
		else
		{
			return getShortCopy();
		}
	}
	
	/**
	 * Gets a copy of the labelImage as a short array.
	 * @return short[] representation of the labelImage
	 */
	public short[] getShortCopy()
	{
		if(dim==3)
		{
			return (short[])getProjected3D(false).getProcessor().getPixels();
		}
		
		final int n = dataLabel.length;
		
		short[] shortData = new short[n];
		for(int i=0; i<n; i++)
		{
			shortData[i] = (short)dataLabel[i];
		}
		return shortData;
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
	
	
	public ImagePlus getProjected3D(boolean abs)
	{
		ImageStack stack = get3DShortStack(abs);
		int z = getDimensions()[2];

		ImagePlus imp = new ImagePlus("Projection stack ", stack);
		StackProjector projector = new StackProjector();
		imp = projector.doIt(imp, z);
		
		return imp;
	}

	/////////////////////////////////////////////////////////////

	static void debug(Object s)
	{
		System.out.println(s);
	}
	
	//////////////////////////////////////////////////
	
	public static LabelImage3D getLabelImage3D()
	{
		return new LabelImage3D();
	}

	
//	@Override
//	public Integer getT(int idx)
//	{
//		return getLabel(idx);
//	}
}


class LabelImage3D extends LabelImage
{

	public LabelImage3D()
	{
		super((int[])null);
		// TODO Auto-generated constructor stub
	}}



/**
 * A pair of labels, used to save 2 competing labels. 
 * Is comparable. 
 */
class LabelPair implements Comparable<LabelPair>
{
	int first;		// smaller value
	int second;		// bigger value
	
	public LabelPair(int l1, int l2)
	{
		if(l1<l2){
			first=l1;
			second=l2;
		}
		else{
			first=l2;
			second=l1;
		}
	}
	
	public int getSmaller(){
		return first;
	}
	
	public int getBigger(){
		return second;
	}

	/**
	 * Compares this object with the specified object for order. 
	 * Returns a negative integer, zero, or a positive integer 
	 * as this object is less than, equal to, or greater than the specified object. 
	 */
	@Override
	public int compareTo(LabelPair o)
	{
		int result = this.first-o.first;
		if(result == 0)
		{
			result = this.second - o.second;
		}
		return result;
	}
	
}


class LabelDispenser
{
	TreeSet<Integer> labels;
//	LinkedList<Integer> labels;
	
	/**
	 * Labels freed up during an iteration gets collected in here
	 * and added to labels at the end of the iteration. 
	 */
	TreeSet<Integer> tempList;
	
	private int highestLabelEverUsed;
	
	/**
	 * @param maxLabels maximal number of labels
	 */
	public LabelDispenser(int maxLabels)
	{
		labels = new TreeSet<Integer>();
//		labels = new LinkedList<Integer>();
		tempList = new TreeSet<Integer>();
		
		highestLabelEverUsed = 0;
		for(int i=1; i<maxLabels; i++) // don't start at 0
		{
			labels.add(i);
		}
	}

	/**
	 * Adds a freed up label to the list of free labels
	 * @param label Has to be absLabel (non-contour)
	 */
	public void addFreedUpLabel(int label)
	{
		tempList.add(label);
//		labels.add(label);
	}
	
	/**
	 * add the tempList to labels at the end of an iteration
	 */
	public void addTempFree()
	{
//		for(int label : tempList)
//		{
//			labels.addFirst(label);
//		}
		labels.addAll(tempList);
		
		tempList.clear();
	}
	
	/**
	 * @return an unused label
	 * @throws NoSuchElementException - if this list is empty
	 */
	public int getNewLabel()
	{
		Integer result = labels.pollFirst();
		checkAndSetNewMax(result);
		return result;
	}
	
	private void checkAndSetNewMax(Integer newMax)
	{
		if(newMax==null)
		{
			newMax=highestLabelEverUsed+1;
		}
		if(newMax>highestLabelEverUsed)
			highestLabelEverUsed=newMax;
	}
	
	/**
	 * If deleted labels gets reused, it may be possible that the 
	 * highest label in the final iteration is not the highest label ever used 
	 * (for example, initialization with n=100 regions, and only n=1...5 survive). 
	 * Setting brightness/contrast in visualization to 1...5 would not show the  
	 * labels>5 correctly 
	 * @return The highest label ever assigned to a region
	 */
	public int getHighestLabelEverUsed()
	{
		return highestLabelEverUsed;
	}
	
	public void setLabelsInUse(Collection<Integer> used)
	{
		labels.removeAll(used);
		int max = Collections.max(used);
		checkAndSetNewMax(max);
	}
	
	public LabelDispenserInc getIncrementingDispenser()
	{
		return new LabelDispenserInc();
	}
	
	
	/**
	 * A LabelDispenser that only increments and does not reuse labels 
	 */
	public static class LabelDispenserInc extends LabelDispenser
	{
		int label = 0;

		public LabelDispenserInc()
		{
			super(0);
		}
		
		@Override
		public int getHighestLabelEverUsed()
		{
			return label;
		}
		
		@Override
		public int getNewLabel()
		{
			label++;
			return label;
		}
		
		@Override
		public void setLabelsInUse(Collection<Integer> used)
		{
			if(used.isEmpty()) return;
			
			int max = Collections.max(used);
			if(max>label)
				label = max;
		}
		
		
	}
}


class LabelImageG<T> extends LabelImage
{

	public LabelImageG(ImagePlus ip)
	{
		super((int[])null);
		// TODO Auto-generated constructor stub
	}
	
	public T getG(int idx)
	{
		int i = getLabel(idx);
		T t=  (T)new Integer(i);
		
		return t;
	}
}


class StackProjector extends GroupedZProjector
{
	int method = ZProjector.MAX_METHOD;
	
	public StackProjector()
	{
//		method = ZProjector.SUM_METHOD;
//		method = ZProjector.AVG_METHOD;
	}
	
	public ImagePlus doIt(ImagePlus imp, int groupSize)
	{
//		method = ZProjector.SUM_METHOD;
//		method = ZProjector.AVG_METHOD;
		ImagePlus imp2 = groupZProject(imp, method, groupSize);
		
		return imp2;
	}
}



