package mosaic.region_competition;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


public class LabelImage //extends ShortProcessor
{
	static int forbiddenLabel=Short.MAX_VALUE;
	static int bgLabel = 0;
	static int negOfs=1000;			// 
	
	int width;
	int height;
	
	// data structures
	ImagePlus originalIP;
	ImageProcessor labelImage;
	
	public ImageProcessor getLabelImage() {
		return labelImage;
	}

	HashMap<Point, ContourParticle> contourContainer;
	HashMap<Integer, LabelInformation> labelMap;
	
	
	public LabelImage(ImagePlus ip) 
	{
		width=ip.getWidth();
		height=ip.getHeight();
		labelImage= new ShortProcessor(width, height);
//		super(ip.getWidth(), ip.getHeight());
		//TODO initial capacities
		contourContainer = new HashMap<Point, ContourParticle>();
		labelMap = new HashMap<Integer, LabelInformation>();
		originalIP=ip;
	}
	
	public void initZero()
	{
		for(int i=0; i<this.width; i++)
		{
			for(int j=0; j<this.height; j++)
			{
				set(i, j, 0);
			}
		}
	}
	
	/**
	 * sets the outermost pixels of the labelimage to the forbidden label
	 */
	public void initBoundary()
	{
		for(int y = 0; y < height; y++){
			set(0, y, forbiddenLabel);
			set(width-1,y,forbiddenLabel);
		}
		
		for(int x = 0; x < width; x++){
			set(x, 0, forbiddenLabel);
			set(x, height-1, forbiddenLabel);
		}
	}
	
	/**
	 * creates an initial guess (rectangular, label 1, 10pixels from edges)
	 */
	public void initialGuess()
	{
		Roi vRectangleROI = new Roi(10, 10, originalIP.getWidth()-20, originalIP.getHeight()-20);
		labelImage.setValue(1);
		labelImage.fill(vRectangleROI);
	}
	
	/**
	 * compute the statistics for each region, 
	 * stores them in labelMap for the corresponding label
	 */
	public void computeStatistics() 
	{
		//TODO seems to be slow
		for (int x = 0; x < width; x++) 
		{
			for (int y = 0; y < height; y++) 
			{
				int label = get(x, y);
				int absLabel = getAbsLabel(label);

				if (absLabel != bgLabel && absLabel != forbiddenLabel) 
				{
					if (!labelMap.containsKey(absLabel)) 
					{
						labelMap.put(absLabel, new LabelInformation(absLabel));
					}
					LabelInformation stats = labelMap.get(absLabel);
					// TODO grayscale
					stats.add(originalIP.getPixel(x, y)[0]);
				}
			}
		} // for all pixel
	}
	
	/**
	 * marks the contour of each region
	 * stores the contour particles in contourContainer
	 */
	public void generateContour()
	{
		// finds and saves contour particles
		for(int y = 1; y < height-1; y++)
		{
			for(int x = 1; x < width-1; x++)
			{
				int label=get(x, y);
				if(label!=bgLabel && label!=forbiddenLabel) // region pixel
					// && label<negOfs
				{
					Connectivity conn = new Connectivity2D_4();
					
					Point p = new Point(x,y);
					for(Point neighbor : conn.getNeighbors(p))
					{
						int neighborLabel=get(neighbor);
						if(neighborLabel!=label)
//						if(getAbsLabel(neighborLabel)!=label) // TODO insert if there exists already contour
						{
							ContourParticle particle = new ContourParticle();
							particle.label=label;
							//TODO grayscale
							particle.intensity=originalIP.getProcessor().get(x,y);
							contourContainer.put(p, particle);
							
							break;
						}
					}
					
					// version 2: without Connectivity.getNeighbors(Point) 
//					for(Point connOfs:conn)
//					{
//						Point p = new Point(x,y);
//						int neighborLabel=get(p.add(connOfs));
//						if(neighborLabel!=label)
////						if(getAbsLabel(neighborLabel)!=label) // TODO insert if there exists already contour
//						{
//							ContourParticle particle = new ContourParticle();
//							particle.label=label;
//							//TODO grayscale
//							particle.intensity=originalIP.getProcessor().get(x,y);
//							contourContainer.put(p, particle);
//							
//							break;
//						}
//					}

//					//version 1, non iterator connectivity version
//					int neighbors[][] = {{-1,0}, {1, 0}, {0,-1}, {0, 1}};
//					//TODO is length ok?
//					for(int i=0; i<neighbors.length; i++)
//					{
//						int neighborLabel=get(x+neighbors[i][0], y+neighbors[i][1]);
//						if(neighborLabel!=label)
//						//if(getAbsLabel(neighborLabel)!=label) // TODO insert if there exists already contour
//						{
//							ContourParticle particle = new ContourParticle();
//							particle.label=label;
//							//TODO grayscale
//							particle.intensity=originalIP.getProcessor().get(x,y);
//							contourContainer.put(new Point(x, y), particle);
//							break;
//						}
//					}  // for neighbors

					
				} // if region pixel
			}
		}
		
		// set contour to -label
		for(Entry<Point, ContourParticle> entry:contourContainer.entrySet())
		{
			Point key = entry.getKey();
			ContourParticle value = entry.getValue();
			//TODO cannot set neg values to ShortProcessor
			set(key, getNegLabel(value.label));
		}
	}
	


	void shrink()
	{
		//iterate over contour particles
		for(Entry<Point, ContourParticle> entry : contourContainer.entrySet()) 
		{
			Point particlePoint = entry.getKey();
			ContourParticle particle = entry.getValue();
			
			particle.candidateLabel=bgLabel;
			//TODO hier weiter
			particle.energyDifference=0;
			
			
			// wegverlängerung, 2D, 4-connected
			int nSameNeighbors=0;
			Connectivity conn = new Connectivity2D_4();
			
			// version 3 with COnnectivity.getNeighbots(Point)
			for(Point neighbor:conn.getNeighbors(particlePoint))
			{
				int neighborLabel=get(neighbor);
				neighborLabel=getAbsLabel(neighborLabel);
				if(neighborLabel==particle.label)
				{
					nSameNeighbors++;
				}
			}
			
//			//version 2
//			for(Point connOfs:conn)
//			{
//				int neighborLabel=get(particlePoint.add(connOfs));
//				neighborLabel=getAbsLabel(neighborLabel);
//				if(neighborLabel==particle.label)
//				{
//					nSameNeighbors++;
//				}
//			}
			
// version 1 with awt point and without connectivity
//			int neighbors[][] = {{-1,0}, {1, 0}, {0,-1}, {0, 1}};
//			for(int i=0; i<neighbors.length; i++)
//			{
//				int neighborLabel=get(particlePoint.x+neighbors[i][0], particlePoint.y+neighbors[i][1]);
//				neighborLabel=getAbsLabel(neighborLabel);
//				if(neighborLabel==particle.label)
//				{
//					nSameNeighbors++;
//				}
//			}  // for neighbors
			
			int nOtherNeighbors=4-nSameNeighbors;
			int dGamma = nSameNeighbors - nOtherNeighbors;
			
		}
	}
	
	
	void grow()
	{
		HashMap<Point, ContourParticle> M;
		// M = candidate list
		M=(HashMap<Point, ContourParticle>) contourContainer.clone();
		
		Connectivity conn = new Connectivity2D_4();
		
		// iterate over all particles
		for(Entry<Point,ContourParticle> entry:M.entrySet())
		{
			Point particlePoint = entry.getKey();
			ContourParticle motherParticle = entry.getValue();
			for(Point negOfs:conn)
			{
				int label=get(particlePoint.add(negOfs));
				int absLabel=getAbsLabel(label);
				if(absLabel==bgLabel)
				{
					// grow in BG
					// TODO create new particle and add to M
				} 
				else if(absLabel != motherParticle.label && absLabel != forbiddenLabel)
				{
//					grow to other region
					//TODO energy
				}
				else
				{
					// self or forbidden. do nothing
				}
			} // for neighbors
			
		}
		
		
		
		// neighboring points of different region: register p as mother
			// for bg labels: create particle and add to M
		// set daughter flag of q
	}

	
	/**
	 * @param label
	 * @return true, if label is a contour label
	 */
	boolean isContourLabel(int label) {
		if (isForbiddenLabel(label)) {
			return false;
		} else {
			return (label > negOfs);
		}
	}

	boolean isForbiddenLabel(int label) {
		return (label == forbiddenLabel);
	}
	
	/**
	 * @param label a label
	 * @return if label was a contour label, get the absolut/inner label
	 */
	int getAbsLabel(int label) {
		if (isContourLabel(label)) {
			return label - negOfs;
		} else {
			return label;
		}
	}

	/**
	 * @param label a label
	 * @return the contour form of the label
	 */
	int getNegLabel(int label) {
		if (label==bgLabel || isForbiddenLabel(label) || isContourLabel(label)) {
			return label;
		} else {
			return label + negOfs;
		}
	}
	
	
	/**
	 * @param p
	 * @return Returns the value of the LabelImage at x,y
	 */
	private int get(int x, int y) {
		return labelImage.get(x,y);
	}

	/**
	 * @param p
	 * @return Returns the value of the LabelImage at Point p
	 */
	private int get(Point p) {
		//TODO multidimension
		return get(p.x[0], p.x[1]);
	}

	private void set(int x, int y, int val) 
	{
		labelImage.set(x,y,val);
	}

	private void set(Point p, int value) {
		//TODO multidimension
		labelImage.set(p.x[0], p.x[1], value);
	}
	
	
	
}
