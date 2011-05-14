package mosaic.region_competition;
//import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ShortProcessor;


public class LabelImage extends ShortProcessor{
	
	static int forbiddenLabel=Short.MAX_VALUE;
	static int bgLabel = 0;
	static int negOfs=1000;			// 
	
	
	int dim=2;
	
	// data structures
	ImagePlus originalIP;
	HashMap<Point, ContourParticle> contourContainer;
	HashMap<Integer, LabelInformation> labelMap;
	
	
	public LabelImage(ImagePlus ip) 
	{
		super(ip.getWidth(), ip.getHeight());
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
		for(int y = 0; y < getHeight(); y++){
			set(0, y, forbiddenLabel);
			set(getWidth()-1,y,forbiddenLabel);
		}
		
		for(int x = 0; x < getWidth(); x++){
			set(x, 0, forbiddenLabel);
			set(x, getHeight()-1, forbiddenLabel);
		}
	}
	
	/**
	 * creates an initial guess (rectangular, label 1, 10pixels from edges)
	 */
	public void initialGuess()
	{
		Roi vRectangleROI = new Roi(10, 10, originalIP.getWidth()-20, originalIP.getHeight()-20);
		setValue(1);
		fill(vRectangleROI);
	}
	
	/**
	 * compute the statistics for each region, 
	 * stores them in labelMap for the corresponding label
	 */
	public void computeStatistics() 
	{
		for (int x = 0; x < getWidth(); x++) 
		{
			for (int y = 0; y < getHeight(); y++) 
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
		for(int y = 1; y < getHeight()-1; y++)
		{
			for(int x = 1; x < getWidth()-1; x++)
			{
				int label=get(x, y);
				if(label!=bgLabel && label!=forbiddenLabel) // region pixel
					// && label<negOfs
				{
					Connectivity conn = new Connectivity();
					for(Point connOfs:conn)
					{
						Point p = new Point(x,y);
						int neighborLabel=get(p.add(connOfs));
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

//					//old, non iterator connectivity version
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
		Iterator<Entry<Point, ContourParticle>> it = contourContainer.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<Point, ContourParticle> next = it.next();
			
			Point key = next.getKey();
			ContourParticle value = next.getValue();
			//TODO cannot set neg values to ShortProcessor
			set(key, getNegLabel(value.label));
		}
	}
	


	void shrink()
	{
		//iterate over contour particles
		Iterator<Entry<Point, ContourParticle>> it = contourContainer.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<Point, ContourParticle> next = it.next();
			Point particlePoint = next.getKey();
			ContourParticle particle = next.getValue();
			
			particle.candidateLabel=bgLabel;
			//TODO hier weiter
			particle.energyDifference=0;
			
			
			
			// wegverlängerung, 2D, 4-connected
			
			int nSameNeighbors=0;
			
			// TODO connectivity
			Connectivity conn = new Connectivity();
			for(Point connOfs:conn)
			{
				int neighborLabel=get(particlePoint.add(connOfs));
				neighborLabel=getAbsLabel(neighborLabel);
				if(neighborLabel==particle.label)
				{
					nSameNeighbors++;
				}
			}
			
// version with awt point and without connectivity
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
		// M = candidate list
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
	
	
	private int get(Point p) {
		//TODO multidimension
		return get(p.x[0], p.x[1]);
	}

	private void set(Point p, int value) {
		//TODO multidimension
		super.set(p.x[0], p.x[1], value);
	}
	
	
	
}
