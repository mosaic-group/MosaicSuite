package mosaic.region_competition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import mosaic.plugins.Region_Competition;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.GroupedZProjector;
import ij.plugin.ZProjector;
import ij.plugin.filter.Filters;
import ij.plugin.filter.MaximumFinder;
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
	IndexIterator iterator; // iterates over the labelImage
	
	
	final int forbiddenLabel=Integer.MAX_VALUE; //short
	final int bgLabel = 0;
	final int negOfs = 10000;			// labels above this number stands for "negative numbers" (problem with displaying negative numbers in ij.ShortProcessor)
	private int m_MaxNLabels;
	private LabelDispenser labelDispenser;
	
	private Connectivity connFG;
	private Connectivity connBG;
	
	ForestFire forestFire;
	
	
	// data structures
	
	/** stores the contour particles. access via coordinates */
//	HashMap<Point, ContourParticle> m_InnerContourContainer;
	
	/** Maps the label(-number) to the information of a label */
	private HashMap<Integer, LabelInformation> labelMap;
//	private HashMap<Point, LabelPair> m_CompetingRegionsMap;
//	private TopologicalNumberImageFunction m_TopologicalNumberFunction;
	Set<Pair<Point, Integer>> m_Seeds = new HashSet<Pair<Point, Integer>>();

	
	public LabelImage(int dims[]) 
	{
		init(dims);
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
		System.out.println("dataLabel.length="+dataLabel.length);
	}
	
	
	public void initMembers()
    {
//		m_InnerContourContainer = new HashMap<Point, ContourParticle>();
		labelMap = new HashMap<Integer, LabelInformation>();
		
		labelDispenser = new LabelDispenser(negOfs);
		m_MaxNLabels=0;
    }
	
	
	private void initConnectivities(int d)
	{
		connFG = new Connectivity(d, d-1);
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
		
//		if(dim==3)
		{
			this.labelPlus = ip; 
			ImageStack stack = ip.getImageStack();
			this.dataLabel = IntConverter.intStackToArray(stack);
			this.labelIP = null;
		}
//		if(dim==2)
//		{
//			initWithImageProc(ip.getProcessor());
//		}
		
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
	
	/**
	 * creates an initial guess (of the size r*labelImageSize)
	 * @param r fraction of sizes of the guess
	 */
	public void initialGuessGrowing(double r)
	{
		ImageStack stack = labelPlus.getImageStack();
		
		int w = (int)(r*width);
		int h = (int)(r*height);
		int x = (width-w)/2;
		int y = (height-h)/2;
		
		// stacks start counting at 1
		for(int i=1; i<=stack.getSize(); i++)
		{
			ImageProcessor proc = stack.getProcessor(i);
			Roi vRectangleROI = new Roi(x, y, w, h);
			proc.setValue(1);
//			labelIP.setValue(labelDispenser.getNewLabel());
			proc.fill(vRectangleROI);
		}
	}
	
	
	/**
	 * initial guess by generating random ellipses (may overlap)
	 */
	public void initialGuessRandom()
	{
		Random rand = new Random();
		
		int maxNum=5;
		int n=1+rand.nextInt(maxNum);
		
		int ellipses[][]= new int[n][5];
		
		System.out.println("generating "+n+" random ellipses: ");
		System.out.println("x, y, w, h, label");
		System.out.println(n);
		
		for(int i=0; i<n; i++)
		{
			int min = 10;
			int w=min+rand.nextInt(width/2-min);
			int h=min+rand.nextInt(height/2-min);
			
			int x = w+rand.nextInt(width-2*w);
			int y = h+rand.nextInt(height-2*h);
			
			int label=i+1;
//			int label=i+labelDispenser.getNewLabel();
			
			ellipses[i][0]=x;
			ellipses[i][1]=y;
			ellipses[i][2]=w;
			ellipses[i][3]=h;
			ellipses[i][4]=label;
			
//			Roi roi = new OvalRoi(x, y, w, h);
//			labelIP.setValue(label);
//			labelIP.fill(roi);
			
			System.out.println(x+" "+y+" "+w+" "+h+" "+label+" ");
		}
		
		initialGuessEllipses(ellipses);
	}

	/**
	 * Only 2D <br>
	 * creates an initial guess from an array of ellipses. 
	 * @param ellipses array of ellipses
	 */
	public void initialGuessEllipses(int ellipses[][])
	{
		ImageProcessor proc = labelPlus.getImageStack().getProcessor(1);
		
		int x, y, w, h;
		int label;
		int n = ellipses.length;

		System.out.println(n);

		for(int i = 0; i < n; i++) 
		{
			int e[] = ellipses[i];
			x = e[0];
			y = e[1];
			w = e[2];
			h = e[3];
			label = e[4];

			Roi roi = new OvalRoi(x, y, w, h);
			proc.setValue(label);
			proc.fill(roi);

			System.out.println(x + " " + y + " " + w + " " + h + " " + label + " ");
		}
	}
	
	
	/**
	 * Debug function, to read in critical initial guesses (string output from random ellipses)
	 */
	public void initialGuessEllipsesFromString(String s)
	{
		Scanner scanner = new Scanner(s);
		int n = scanner.nextInt();
		
		int ellipses[][] = new int[n][5]; //5=2*dim+1
		
		for(int i = 0; i < n; i++) 
		{
			int e[]=ellipses[i];
			//coords
			for(int j=0; j<dim; j++){
				e[j]=scanner.nextInt();
			}
			//sizes
			for(int j=dim; j<2*dim; j++){
				e[j]=scanner.nextInt();
			}
			//label
			e[2*dim]=scanner.nextInt();
		}
		initialGuessEllipses(ellipses);
	}
	
	
	public void initBrightBubbles(IntensityImage intensityImage)
	{
		ImagePlus imp = intensityImage.imageIP;
		imp = new Duplicator().run(imp);
		IJ.run(imp, "Gaussian Blur...", "sigma=3 stack");
		
		ImageStack stack = imp.getStack();
		int n = stack.getSize();
		
		ImageStack byteStack = new ImageStack(stack.getWidth(), stack.getHeight());
		
		MaximumFinder finder = new MaximumFinder();
		for(int i=0; i<n; i++)
		{
			ImageProcessor proc = stack.getProcessor(i+1);
			ImageProcessor maxima = finder.findMaxima(proc, 0.01, 0, MaximumFinder.IN_TOLERANCE, false, false);
			byteStack.addSlice(maxima);
		}
		imp.setStack(byteStack);
		imp.setTitle("after findmax");
		imp.show();
		
		initWithIP(imp);
		connectedComponents();
		
//		IJ.run(imp, "Find Maxima...", "noise=0.01 output=[Maxima Within Tolerance]");
	}
	
	
	public ImagePlus findMaximaStack(ImagePlus imp, double tolerance)
	{
		ImageStack stack = imp.getStack();
		int n = stack.getSize();
		
		ImageStack byteStack = new ImageStack(stack.getWidth(), stack.getHeight());
		
		MaximumFinder finder = new MaximumFinder();
		for(int i=0; i<n; i++)
		{
			ImageProcessor proc = stack.getProcessor(i+1);
			ImageProcessor maxima = finder.findMaxima(proc, tolerance, ImageProcessor.NO_THRESHOLD, MaximumFinder.IN_TOLERANCE, false, false);
			byteStack.addSlice(maxima);
		}
		ImagePlus result = new ImagePlus("findMax"+imp, byteStack);
//		result.show();
		
		return result;
	}
	
	public void initSwissCheese(IntensityImage intensityImage)
	{
		ImagePlus imp = intensityImage.imageIP;
		imp = new Duplicator().run(imp);
		IJ.run(imp, "Gaussian Blur...", "sigma=3 stack");
//		imp.show();
		ImagePlus holes = new Duplicator().run(imp);
		
		// thresholding
		IJ.setAutoThreshold(imp, "Otsu dark stack");
		IJ.run(imp, "Convert to Mask", "  black");
		ImageStack stack1 = imp.getImageStack();
		
		// holes
		IJ.run(holes, "Invert", "stack");
		holes.setTitle("swiss cheese holes");
		holes = findMaximaStack(holes, 0.01);
		ImageStack stack2 = holes.getImageStack();

		
//		IJ.run(holes, "Find Maxima...", "noise=0 output=[Maxima Within Tolerance] light");
		
		
		int nSlizes = stack1.getSize();
		for(int i=0; i<nSlizes; i++)
		{
			ImageProcessor proc1 = stack1.getProcessor(i+1);
			ImageProcessor proc2 = stack2.getProcessor(i+1);
			
			int size = proc1.getPixelCount();
			for(int idx=0; idx<size; idx++)
			{
				int v1 = proc1.get(idx);
				int v2 = proc2.get(idx);
				
				if(v2!=0) // on hole (hole is non zero), set black in main imp
				{
					proc1.set(idx, 0);
				}
			}
			
		}
//		imp.show();
//		holes.show();
		
		imp.show();
		initWithIP(imp);
		connectedComponents();
		
	}
	
	
	
	public void initialGuessBubbles()
	{
		int rad = 10;
		int halfgap = 5;
		int displ = 2*(rad+halfgap);
		
		int[] grid = new int[dim];
		int[] region = new int[dim];
		int[] gap = new int[dim];
		
		for(int i=0; i<dim; i++)
		{
			int size=dimensions[i];
			grid[i]=size/(displ);	// how many bubbles per length
			region[i]=displ;		// size of region
			gap[i]=size%(displ);
		}
		
		Point gapPoint = new Point(gap);
		
		BubbleDrawer bd = new BubbleDrawer(this, rad, 2*rad);
		
		int bubbleIndex = 0;
		IndexIterator it = new IndexIterator(grid);
		for(Point ofs:it.getPointIterable())
		{
//			debug(ofs);
			ofs=ofs.mult(displ).add(gapPoint); // left upper startpoint of a bubble+spacing
//			RegionIterator rit = new RegionIterator(m_LabelImage.dimensions, region, ofs.x);
			
			bubbleIndex++;
			bd.doSphereIteration(ofs, bubbleIndex);
//			bd.doSphereIteration(ofs, labelDispenser.getNewLabel());
		}
		
//		System.out.println(Arrays.toString(dataLabel));
		
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
				MultipleThresholdImageFunction<Integer> aMultiThsFunctionPtr = new MultipleThresholdImageFunction<Integer>(this);
				aMultiThsFunctionPtr.AddThresholdBetween(l, l);
				FloodFill ff = new FloodFill(this, aMultiThsFunctionPtr, iterator.indexToPoint(i));
				
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


	boolean isBoundaryPoint(Point aIndex)
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
	boolean isEnclosedByLabel(Point pIndex, int pLabel)
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


	boolean isForbiddenLabel(int label)
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
	
	public HashMap<Integer, LabelInformation> getLabelMap()
	{
		return labelMap;
	}
	
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
	boolean isContourLabel(int label)
	{
		return (label<0);
//		if(isForbiddenLabel(label)) {
//			return false;
//		} else {
//			return (label > negOfs);
//		}
	}
	
	
	int getLabel(int index)
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
		return Math.abs(dataLabel[idx]);
//		return labelToAbs(getLabel(p));
	}

	public int getLabelAbs(int idx)
	{
		return Math.abs(dataLabel[idx]);
//		return labelToAbs(getLabel(idx));
	}

	
	/**
	 * sets the labelImage to val at point x,y
	 */
	void setLabel(int idx, int label) 
	{
		dataLabel[idx]=label;
//		dataLabel[idx]=(short)val;
//		labelIP.set(idx, val);
	}

	/**
	 * sets the labelImage to val at Point p
	 */
	void setLabel(Point p, int label) {
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
	 * @return if label was a contour label, get the absolut/inner label
	 */
	int labelToAbs(int label) {
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
	

	public int getDim()
	{
		return this.dim;
	}

	public int[] getDimensions()
	{
		return this.dimensions;
	}
	
	public Connectivity getConnFG() {
		return connFG;
	}


	public Connectivity getConnBG() {
		return connBG;
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
		for(int i=1; i<maxLabels; i++) // dont start at 0
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
	
	void checkAndSetNewMax(Integer newMax)
	{
		if(newMax==null)
		{
			newMax=highestLabelEverUsed+1;
		}
		if(newMax>highestLabelEverUsed)
			highestLabelEverUsed=newMax;
	}
	
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



