package mosaic.region_competition;

import ij.plugin.filter.*;
import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.process.*;
import java.awt.*;
import java.util.*;

/** This ImageJ plug-in filter finds the maxima (or minima) of an image.
 * It can create a mask where the local maxima of the current image are
 * marked (255; unmarked pixels 0).
 * The plug-in can also create watershed-segmented particles: Assume a
 * landscape of inverted heights, i.e., maxima of the image are now water sinks.
 * For each point in the image, the sink that the water goes to determines which
 * particle it belongs to.
 * When finding maxima (not minima), pixels with a level below the lower threshold
 * can be left unprocessed.
 *
 * Except for segmentation, this plugin works with ROIs, including non-rectangular ROIs.
 * Since this plug-in creates a separate output image it processes
 *    only single images or slices, no stacks.
 *
 * Notes:
 * - When using one instance of MaximumFinder for more than one image in parallel threads,
 *   all must images have the same width and height.
 *
 * version 09-Nov-2006 Michael Schmid
 * version 21-Nov-2006 Wayne Rasband. Adds "Display Point Selection" option and "Count" output type.
 * version 28-May-2007 Michael Schmid. Preview added, bugfix: minima of calibrated images, uses Arrays.sort
 * version 07-Aug-2007 Fixed a bug that could delete particles when doing watershed segmentation of an EDM.
 * version 21-Apr-2007 Adapted for float instead of 16-bit EDM; correct progress bar on multiple calls
 * version 05-May-2009 Works for images>32768 pixels in width or height
 * version 01-Nov-2009 Bugfix: extra lines in segmented output eliminated; watershed is also faster now
 *                     Maximum points encoded in long array for sorting instead of separete objects that need gc
 *                     New output type 'List'
 * version 22-May-2011 Bugfix: Maximum search in EDM and float images with large dynamic range could omit maxima
 */

public class MaximumFinder3D
{
    //filter params
    /** maximum height difference between points that are not counted as separate maxima */
    private static double tolerance = 10;
    /** Output type single points */
    public final static int SINGLE_POINTS=0;
    /** Output type all points around the maximum within the tolerance */
    public final static int IN_TOLERANCE=1;
    /** Output type watershed-segmented image */
    public final static int SEGMENTED=2;
    /** Do not create image, only mark points */
    public final static int POINT_SELECTION=3;
    /** Do not create an image, just list x, y of maxima in the Results table */
    public final static int LIST=4;
    /** Do not create an image, just count maxima and add count to Results table */
    public final static int COUNT=5;
    /** what type of output to create (see constants above)*/
    private static int outputType;
    /** what type of output to create was chosen in the dialog (see constants above)*/
    private static int dialogOutputType = POINT_SELECTION;
    /** output type names */
    final static String[] outputTypeNames = new String[]
        {"Single Points", "Maxima Within Tolerance", "Segmented Particles", "Point Selection", "List", "Count"};
    /** whether to exclude maxima at the edge of the image*/
    private static boolean excludeOnEdges;
    /** whether to accept maxima only in the thresholded height range*/
    private static boolean useMinThreshold;
    /** whether to find darkest points on light background */
    private static boolean lightBackground;
//    private ImagePlus imp;                          // the ImagePlus of the setup call
    private boolean   thresholded;                  // whether the input image has a threshold
    private boolean   previewing;                   // true while dialog is displayed (processing for preview)
    private boolean thresholdWarningShown = false;  // whether the warning "can't find minima with thresholding" has been shown
    private Label     messageArea;                  // reference to the textmessage area for displaying the number of maxima
    private double    progressDone;                 // for progress bar, fraction of work done so far
    private int       nPasses = 0;                  // for progress bar, how many images to process (sequentially or parallel threads)
    //the following are class variables for having shorter argument lists
    private int       width, height;                // image dimensions
    /** directions to 8 neighboring pixels, clockwise: 0=North (-y), 1=NE, 2=East (+x), ... 7=NW */
    private int[]     dirOffset;                    // pixel offsets of neighbor pixels for direct addressing
    private int[][] points;                    // maxima found by findMaxima() when outputType is POINT_SELECTION
    final static int[] DIR_X_OFFSET = new int[] {  0,  1,  1,  1,  0, -1, -1, -1 };
    final static int[] DIR_Y_OFFSET = new int[] { -1, -1,  0,  1,  1,  1,  0, -1 };
    /** the following constants are used to set bits corresponding to pixel types */
    final static byte MAXIMUM = (byte)1;            // marks local maxima (irrespective of noise tolerance)
    final static byte LISTED = (byte)2;             // marks points currently in the list
    final static byte PROCESSED = (byte)4;          // marks points processed previously
    final static byte MAX_AREA = (byte)8;           // marks areas near a maximum, within the tolerance
    final static byte EQUAL = (byte)16;             // marks contigous maximum points of equal level
    final static byte MAX_POINT = (byte)32;         // marks a single point standing for a maximum
    final static byte ELIMINATED = (byte)64;        // marks maxima that have been eliminated before watershed
    /** type masks corresponding to the output types */
    final static byte[] outputTypeMasks = new byte[] {MAX_POINT, MAX_AREA, MAX_AREA};
    final static float SQRT2 = 1.4142135624f;

    
    private int depth;
    private int size;
    IndexIterator iterator;
    Connectivity conn;
    
    public MaximumFinder3D(int[] dims)
    {
    	this(dims[0], dims[1], dims[2]);
    }
    
    public MaximumFinder3D(int w, int h, int z)
	{
		this.width=w;
		this.height=h;
		this.depth=z;
		
		conn = new Connectivity(3, 0);
		
		this.size=width*height*depth;
		iterator = new IndexIterator(new int[] {width, height, depth});
	}

    /** Finds the image maxima and returns them as a Polygon. There
     * is an example at http://imagej.nih.gov/ij/macros/js/FindMaxima.js.
     * @param ip             The input image
     * @param tolerance      Height tolerance: maxima are accepted only if protruding more than this value
     *                       from the ridge to a higher maximum
     * @param excludeOnEdges Whether to exclude edge maxima
     * @return               A Polygon containing the coordinates of the maxima
     */
    public int[][] getMaxima(float[] ip, double tolerance, boolean excludeOnEdges) {
		findMaxima(ip, tolerance, ImageProcessor.NO_THRESHOLD,
			MaximumFinder.POINT_SELECTION, excludeOnEdges, false);
//		if (points==null)
//			return new Polygon();
//		else
			return points;
    }

    /** Here the processing is done: Find the maxima of an image (does not find minima).
     * @param ip             The input image
     * @param tolerance      Height tolerance: maxima are accepted only if protruding more than this value
     *                       from the ridge to a higher maximum
     * @param threshold      minimum height of a maximum (uncalibrated); for no minimum height set it to
     *                       ImageProcessor.NO_THRESHOLD
     * @param outputType     What to mark in output image: SINGLE_POINTS, IN_TOLERANCE or SEGMENTED.
     *                       No output image is created for output types POINT_SELECTION, LIST and COUNT.
     * @param excludeOnEdges Whether to exclude edge maxima
     * @param isEDM          Whether the image is a float Euclidian Distance Map
     * @return               A new byteProcessor with a normal (uninverted) LUT where the marked points
     *                       are set to 255 (Background 0). Pixels outside of the roi of the input ip are not set.
     *                       Returns null if outputType does not require an output or if cancelled by escape
     */
    public byte[] findMaxima(float[] ip, double tolerance, double threshold,
            int outputType, boolean excludeOnEdges, boolean isEDM) 
    {
//        if (dirOffset == null) makeDirectionOffsets(ip);
        
    	float globalMin = Float.MAX_VALUE;
    	float globalMax = -Float.MAX_VALUE;

    	for(int i=0; i<size; i++)
    	{
    		float v = ip[i];
			if (globalMin>v) globalMin = v;
			if (globalMax<v) globalMax = v;
    	}
    	
    	
//        ByteProcessor typeP = new ByteProcessor(width, height);     //will be a notepad for pixel types
        byte[] types = new byte[size];

        if (threshold !=ImageProcessor.NO_THRESHOLD)
            threshold -= (globalMax-globalMin)*1e-6;//avoid rounding errors
        //for segmentation, exclusion of edge maxima cannot be done now but has to be done after segmentation:
        boolean excludeEdgesNow = excludeOnEdges && outputType!=SEGMENTED;


        IJ.showStatus("Getting sorted maxima...");
        long[] maxPoints = getSortedMaxPoints(ip, types, excludeEdgesNow, isEDM, globalMin, globalMax, threshold);
        
        IJ.showStatus("Analyzing  maxima...");
        float maxSortingError = 0;
        
        maxSortingError = 1.1f * (isEDM ? SQRT2/2f : (globalMax-globalMin)/2e9f);
        
        analyzeAndMarkMaxima(ip, types, maxPoints, excludeEdgesNow, isEDM, globalMin, tolerance, outputType, maxSortingError);
        //new ImagePlus("Pixel types",typeP.duplicate()).show();
        if (outputType==POINT_SELECTION || outputType==LIST || outputType==COUNT)
            return null;
        
        byte[] outIp;
        if (outputType == SEGMENTED) {
        	outIp=null;
        } else //outputType other than SEGMENTED
        {
            for (int i=0; i<width*height; i++)
                types[i] = (byte)(((types[i]&outputTypeMasks[outputType])!=0)?255:0);
            outIp = types;
        }
        //IJ.write("roi: "+roi.toString());

        return outIp;
    } // public ByteProcessor findMaxima
        
    /** Find all local maxima (irrespective whether they finally qualify as maxima or not)
     * @param ip    The image to be analyzed
     * @param typeP A byte image, same size as ip, where the maximum points are marked as MAXIMUM
     *              (do not use it as output: for rois, the points are shifted w.r.t. the input image)
     * @param excludeEdgesNow Whether to exclude edge pixels
     * @param isEDM     Whether ip is a float Euclidian distance map
     * @param globalMin The minimum value of the image or roi
     * @param threshold The threshold (calibrated) below which no pixels are processed. Ignored if ImageProcessor.NO_THRESHOLD
     * @return          Maxima sorted by value. In each array element (long, i.e., 64-bit integer), the value
     *                  is encoded in the upper 32 bits and the pixel offset in the lower 32 bit
     * Note: Do not use the positions of the points marked as MAXIMUM in typeP, they are invalid for images with a roi.
     */    
    long[] getSortedMaxPoints(float[] ip, byte[] typeP, boolean excludeEdgesNow,
            boolean isEDM, float globalMin, float globalMax, double threshold) 
    {
        byte[] types =  typeP;
        int nMax = 0;  //counts local maxima
        boolean checkThreshold = threshold!=ImageProcessor.NO_THRESHOLD;
        
        for(int i=0; i<size; i++)
        {
        	Point p = iterator.indexToPoint(i);
        	int x = p.x[0];
        	int y = p.x[1];
        	int z = p.x[2];
        	
            float v = ip[i];
            if (v==globalMin) continue;
            
            boolean isBorder = (x==0 || x==width-1 || y==0 || y==height-1 || z==0 || z==depth-1);
            boolean isInner = !isBorder;
            
            if (excludeEdgesNow && isBorder) continue;
            if (checkThreshold && v<threshold) continue;
            boolean isMax = true;
            
            for(Point q : conn.iterateNeighbors(p))
            {
                if (isInner || iterator.isInBound(q))
                {
                    float vNeighbor = ip[iterator.pointToIndex(q)];//ip.getPixelValue(x+DIR_X_OFFSET[d], y+DIR_Y_OFFSET[d]);
                    if (vNeighbor > v) {
                        isMax = false;
                        break;
                    }
                }
            }
            if (isMax) {
                types[i] = MAXIMUM;
                nMax++;
            }
            
        } // for all pixels


        
        float vFactor = (float)(2e9/(globalMax-globalMin)); //for converting float values into a 32-bit int
        long[] maxPoints = new long[nMax];                  //value (int) is in the upper 32 bit, pixel offset in the lower
        int iMax = 0;
        
        for(int i=0; i<size; i++)
        {
			if(types[i] == MAXIMUM)
			{
				float fValue = ip[i];
				int iValue = (int)((fValue - globalMin) * vFactor); // 32-bit int, linear function of float value
				maxPoints[iMax++] = (long)iValue << 32 | i;
			}
        }

        Arrays.sort(maxPoints);                                 //sort the maxima by value
        //long t3 = System.currentTimeMillis();IJ.log("sort:"+(t3-t2));
        return maxPoints;
    } //getSortedMaxPoints

   /** Check all maxima in list maxPoints, mark type of the points in typeP
    * @param ip             the image to be analyzed
    * @param typeP          8-bit image, here the point types are marked by type: MAX_POINT, etc.
    * @param maxPoints      input: a list of all local maxima, sorted by height. Lower 32 bits are pixel offset
    * @param excludeEdgesNow whether to avoid edge maxima
    * @param isEDM          whether ip is a (float) Euclidian distance map
    * @param globalMin      minimum pixel value in ip
    * @param tolerance      minimum pixel value difference for two separate maxima
    * @param maxSortingError sorting may be inaccurate, sequence may be reversed for maxima having values
    *                       not deviating from each other by more than this (this could be a result of
    *                       precision loss when sorting ints instead of floats, or because sorting does not
    *                       take the height correction in 'trueEdmHeight' into account
    * @param outputType 
    */   
   void analyzeAndMarkMaxima(float[] ip, byte[] typeP, long[] maxPoints, boolean excludeEdgesNow,
        boolean isEDM, float globalMin, double tolerance, int outputType, float maxSortingError) {
        byte[] types =  typeP;
        int nMax = maxPoints.length;
        int [] pList = new int[size];       //here we enter points starting from a maximum
        Vector xyVector = null;
        Roi roi = null;
        boolean displayOrCount = outputType==POINT_SELECTION||outputType==LIST||outputType==COUNT;
        if (displayOrCount) 
            xyVector=new Vector();
      
        for (int iMax=nMax-1; iMax>=0; iMax--) {    //process all maxima now, starting from the highest
            int offset0 = (int)maxPoints[iMax];     //type cast gets 32 lower bits, where pixel index is encoded
            //int offset0 = maxPoints[iMax].offset;
            if ((types[offset0]&PROCESSED)!=0)      //this maximum has been reached from another one, skip it
                continue;
            //we create a list of connected points and start the list at the current maximum
            
            Point p = iterator.indexToPoint(offset0);
            
            int x0 = p.x[0];
            int y0 = p.x[1];
            int z0 = p.x[2];
            
            float v0 = ip[offset0];
            boolean sortingError;
            do {                                    //repeat if we have encountered a sortingError
                pList[0] = offset0;
                types[offset0] |= (EQUAL|LISTED);   //mark first point as equal height (to itself) and listed
                int listLen = 1;                    //number of elements in the list
                int listI = 0;                      //index of current element in the list
                boolean isEdgeMaximum = (x0==0 || x0==width-1 || y0==0 || y0==height-1 ||z0==0 || z0 ==depth-1);
                sortingError = false;       //if sorting was inaccurate: a higher maximum was not handled so far
                boolean maxPossible = true;         //it may be a true maximum
                double xEqual = x0;                 //for creating a single point: determine average over the
                double yEqual = y0;                 //  coordinates of contiguous equal-height points
                double zEqual = z0;
                int nEqual = 1;                     //counts xEqual/yEqual points that we use for averaging
                do 									//while neigbor list is not fully processed (to listLen)
                {
                    int offset = pList[listI];
                    Point pp = iterator.indexToPoint(offset);
                    
                    int x = pp.x[0];
                    int y = pp.x[1];
                    int z = pp.x[2]; 
                    //not necessary, but faster than isWithin
                    boolean isInner = (z!=0 && z!=depth-1) &&
                    	(y!=0 && y!=height-1) && 
                    	(x!=0 && x!=width-1);
                    
                    
//                    for (int d=0; d<8; d++) 	//analyze all neighbors (in 8 directions) at the same level
                    for(Point q : conn.iterateNeighbors(pp))
                    {       
                    	int offset2 = iterator.pointToIndex(q);
//                        int offset2 = offset+dirOffset[d];
                        if ((isInner || iterator.isInBound(q)) && (types[offset2]&LISTED)==0) 
                        {
                            if ((types[offset2]&PROCESSED)!=0) {
                                maxPossible = false; //we have reached a point processed previously, thus it is no maximum now
                                //if(x0<25&&y0<20)IJ.write("x0,y0="+x0+","+y0+":stop at processed neighbor from x,y="+x+","+y+", dir="+d);
                                break;
                            }
                            int x2 = q.x[0];
                            int y2 = q.x[1];
                            int z2 = q.x[2];
                            float v2 = ip[offset2];
                            if (v2 > v0 + maxSortingError) {
                                maxPossible = false;    //we have reached a higher point, thus it is no maximum
                                //if(x0<25&&y0<20)IJ.write("x0,y0="+x0+","+y0+":stop at higher neighbor from x,y="+x+","+y+", dir="+d+",value,value2,v2-v="+v0+","+v2+","+(v2-v0));
                                break;
                            } else if (v2 >= v0-(float)tolerance) {
                                if (v2 > v0) {          //maybe this point should have been treated earlier
                                    sortingError = true;
                                    offset0 = offset2;
                                    v0 = v2;
                                    x0 = x2;
                                    y0 = y2;
                                    z0 = z2;

                                }
                                pList[listLen] = offset2;
                                listLen++;              //we have found a new point within the tolerance
                                types[offset2] |= LISTED;
                                if (x2==0 || x2==width-1 || y2==0 || y2==height-1 || z2==0 || z2==depth-1) {
                                    isEdgeMaximum = true;
                                    if (excludeEdgesNow) {
                                        maxPossible = false;
                                        break;          //we have an edge maximum;
                                    }
                                }
                                if (v2==v0) {           //prepare finding center of equal points (in case single point needed)
                                    types[offset2] |= EQUAL;
                                    xEqual += x2;
                                    yEqual += y2;
                                    zEqual += z2;
                                    nEqual ++;
                                }
                            }
                        } // if isWithin & not LISTED
                    } // for directions d
                    listI++;
                } while (listI < listLen);

				if (sortingError)  {				  //if x0,y0 was not the true maximum but we have reached a higher one
					for (listI=0; listI<listLen; listI++)
						types[pList[listI]] = 0;	//reset all points encountered, then retry
				} else {
					int resetMask = ~(maxPossible?LISTED:(LISTED|EQUAL));
					xEqual /= nEqual;
					yEqual /= nEqual;
					zEqual /= nEqual;
					double minDist2 = 1e20;
					int nearestI = 0;
					for (listI=0; listI<listLen; listI++) {
						int offset = pList[listI];
						
						Point pp = iterator.indexToPoint(offset);
	                    int x = pp.x[0];
	                    int y = pp.x[1];
	                    int z = pp.x[2]; 
						types[offset] &= resetMask;		//reset attributes no longer needed
						types[offset] |= PROCESSED;		//mark as processed
						if (maxPossible) {
							types[offset] |= MAX_AREA;
							if ((types[offset]&EQUAL)!=0) {
								double dist2 = (xEqual-x)*(double)(xEqual-x) + (yEqual-y)*(double)(yEqual-y)+ (zEqual-z)*(double)(zEqual-z);
								if (dist2 < minDist2) {
									minDist2 = dist2;	//this could be the best "single maximum" point
									nearestI = listI;
								}
							}
						}
					} // for listI
					if (maxPossible) {
						int offset = pList[nearestI];
						types[offset] |= MAX_POINT;
						if (displayOrCount && !(this.excludeOnEdges && isEdgeMaximum)) 
						{
							Point pp = iterator.indexToPoint(offset);
							
		                    int x = pp.x[0];
		                    int y = pp.x[1];
		                    int z = pp.x[2]; 
							if (roi==null || roi.contains(x, y))
								xyVector.addElement(new int[] {x, y, z});
						}
					}
				} //if !sortingError
			} while (sortingError);				//redo if we have encountered a higher maximum: handle it now.
        } // for all maxima iMax

        if (Thread.currentThread().isInterrupted()) return;
        if (displayOrCount && xyVector!=null) {
            int npoints = xyVector.size();
            if (outputType == POINT_SELECTION && npoints>0) {
                int[] xpoints = new int[npoints];
                int[] ypoints = new int[npoints];
                int[] zpoints = new int[npoints];
                for (int i=0; i<npoints; i++) {
                    int[] xy = (int[])xyVector.elementAt(i);
                    xpoints[i] = xy[0];
                    ypoints[i] = xy[1];
                    zpoints[i] = xy[2];
                }

                points = new int[][] {xpoints, ypoints, zpoints};// (xpoints, ypoints, npoints);
            } else if (outputType==LIST) {
                Analyzer.resetCounter();
                ResultsTable rt = ResultsTable.getResultsTable();
                for (int i=0; i<npoints; i++) {
                    int[] xy = (int[])xyVector.elementAt(i);
                    rt.incrementCounter();
                    rt.addValue("X", xy[0]);
                    rt.addValue("Y", xy[1]);
                    rt.addValue("Z", xy[2]);
                }
                rt.show("Results");
            } else if (outputType==COUNT) {} 
        }
        if (previewing)
            messageArea.setText((xyVector==null ? 0 : xyVector.size())+" Maxima");
    } //void analyzeAndMarkMaxima

   /** Create an 8-bit image by scaling the pixel values of ip to 1-254 (<lower threshold 0) and mark maximum areas as 255.
    * For use as input for watershed segmentation
    * @param ip         The original image that should be segmented
    * @param typeP      Pixel types in ip
    * @param isEDM      Whether ip is an Euclidian distance map
    * @param globalMin  The minimum pixel value of ip
    * @param globalMax  The maximum pixel value of ip
    * @param threshold  Pixels of ip below this value (calibrated) are considered background. Ignored if ImageProcessor.NO_THRESHOLD
    * @return           The 8-bit output image.
    */
    ByteProcessor make8bit(ImageProcessor ip, ByteProcessor typeP, boolean isEDM, float globalMin, float globalMax, double threshold) {
        byte[] types = (byte[])typeP.getPixels();
        double minValue;
        if (isEDM) {
            threshold = 0.5;
            minValue = 1.;
        } else
            minValue = (threshold == ImageProcessor.NO_THRESHOLD)?globalMin:threshold;
        double offset = minValue - (globalMax-minValue)*(1./253/2-1e-6); //everything above minValue should become >(byte)0
        double factor = 253/(globalMax-minValue);
        //IJ.write("min,max="+minValue+","+globalMax+"; offset, 1/factor="+offset+", "+(1./factor));
        if (isEDM && factor>1)
            factor = 1;   // with EDM, no better resolution
        ByteProcessor outIp = new ByteProcessor(width, height);
        //convert possibly calibrated image to byte without damaging threshold (setMinAndMax would kill threshold)
        byte[] pixels = (byte[])outIp.getPixels();
        long v;
        for (int y=0, i=0; y<height; y++) {
            for (int x=0; x<width; x++, i++) {
                float rawValue = ip.getPixelValue(x, y);
                if (threshold!=ImageProcessor.NO_THRESHOLD && rawValue<threshold)
                    pixels[i] = (byte)0;
                else if ((types[i]&MAX_AREA)!=0)
                    pixels[i] = (byte)255;  //prepare watershed by setting "true" maxima+surroundings to 255
                else {
                    v = 1 + Math.round((rawValue-offset)*factor);
                    if (v < 1) pixels[i] = (byte)1;
                    else if (v<=254) pixels[i] = (byte)(v&255);
                    else pixels[i] = (byte)254;
                }
            }
        }
        return outIp;
    } // byteProcessor make8bit

   /** Get estimated "true" height of a maximum or saddle point of a Euclidian Distance Map.
     * This is needed since the point sampled is not necessarily at the highest position.
     * For simplicity, we don't care about the Sqrt(5) distance here although this would be more accurate
     * @param x     x-position of the point
     * @param y     y-position of the point
     * @param ip    the EDM (FloatProcessor)
     * @return      estimated height
     */
    float trueEdmHeight(int x, int y, ImageProcessor ip) {
        int xmax = width - 1;
        int ymax = ip.getHeight() - 1;
        float[] pixels = (float[])ip.getPixels();
        int offset = x + y*width;
        float v =  pixels[offset];
        if (x==0 || y==0 || x==xmax || y==ymax || v==0) {
            return v;                               //don't recalculate for edge pixels or background
        } else {
            float trueH = v + 0.5f*SQRT2;           //true height can never by higher than this
            boolean ridgeOrMax = false;
            for (int d=0; d<4; d++) {               //for all directions halfway around:
                int d2 = (d+4)%8;                   //get the opposite direction and neighbors
                float v1 = pixels[offset+dirOffset[d]];
                float v2 = pixels[offset+dirOffset[d2]];
                float h;
                if (v>=v1 && v>=v2) {
                    ridgeOrMax = true;
                    h = (v1 + v2)/2;
                } else {
                    h = Math.min(v1, v2);
                }
                h += (d%2==0) ? 1 : SQRT2;          //in diagonal directions, distance is sqrt2
                if (trueH > h) trueH = h;
            }
            if (!ridgeOrMax) trueH = v;
            return trueH;
        }
    }
    
    /** returns whether the neighbor in a given direction is within the image
     * NOTE: it is assumed that the pixel x,y itself is within the image!
     * Uses class variables width, height: dimensions of the image
     * @param x         x-coordinate of the pixel that has a neighbor in the given direction
     * @param y         y-coordinate of the pixel that has a neighbor in the given direction
     * @param direction the direction from the pixel towards the neighbor (see makeDirectionOffsets)
     * @return          true if the neighbor is within the image (provided that x, y is within)
     */
    boolean isWithin(int x, int y, int direction) {
        int xmax = width - 1;
        int ymax = height -1;
        switch(direction) {
            case 0:
                return (y>0);
            case 1:
                return (x<xmax && y>0);
            case 2:
                return (x<xmax);
            case 3:
                return (x<xmax && y<ymax);
            case 4:
                return (y<ymax);
            case 5:
                return (x>0 && y<ymax);
            case 6:
                return (x>0);
            case 7:
                return (x>0 && y>0);
        }
        return false;   //to make the compiler happy :-)
    } // isWithin


}


