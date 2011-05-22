package mosaic.region_competition;
import java.util.HashMap;
import java.util.Map.Entry;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/*
//TODO TODOs
- refactor LabelImage, extract what's not supposed to be there (eg energy calc)
- let ContourParticle have ref to Point / inherit from? 
- what to do with mother/daughter list
- getter/setter efficiency
- datastructure for labels, supporting neg values
*/

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
					// version 1: implicitly get()" twice (in contain() and in get())
//					if (!labelMap.containsKey(absLabel)) 
//					{
//						labelMap.put(absLabel, new LabelInformation(absLabel));
//					}
//					LabelInformation stats = labelMap.get(absLabel);
					
					//version 2. only get()s twice, if label does not exist
					LabelInformation stats = labelMap.get(absLabel);
					if(stats==null)
					{
						labelMap.put(absLabel, new LabelInformation(absLabel));
						stats = labelMap.get(absLabel);
					}
					
					stats.add(getIntensity(x, y));
				}
			}
		} // for all pixel
	}
	
	
	/**
	 * as computeStatistics, does not use iterative approach
	 * (first computes sum of values and sum of values^2)
	 */
	public void renewStatistics()
	{
		for (int x = 0; x < width; x++) 
		{
			for (int y = 0; y < height; y++) 
			{
				int label = get(x, y);
				int absLabel = getAbsLabel(label);

				if (absLabel != bgLabel && absLabel != forbiddenLabel) 
				{
					// version 1: implicitly get()" twice (in contain() and in get())
//					if (!labelMap.containsKey(absLabel)) 
//					{
//						labelMap.put(absLabel, new LabelInformation(absLabel));
//					}
//					LabelInformation stats = labelMap.get(absLabel);
					
					//TODO save the last label, the next label will be likely the same label, 
					// so you can save one map lookup (by the cost of 1 integer comparison)
					
					//version 2. only get()s twice, if label does not exist
					LabelInformation stats = labelMap.get(absLabel);
					if(stats==null)
					{
						labelMap.put(absLabel, new LabelInformation(absLabel));
						stats = labelMap.get(absLabel);
					}
					int val = getIntensity(x,y);
					stats.n++;
					stats.mean+=val;
					stats.var+=val*val;
				}
			}
		} // for all pixel
		
		//now we have in all LabelInformation in mean the sum of the values, int var the sum of val^2
		for(LabelInformation stat: labelMap.values())
		{
			stat.mean=stat.mean/stat.n;
			stat.var=stat.var/()
			//TODO hier weiter
		}
		
		
	}
	
	
	public void showStatistics()
	{
		ResultsTable rt = new ResultsTable();
		for(Entry<Integer, LabelInformation> entry: labelMap.entrySet())
		{
			LabelInformation info = entry.getValue();
			
			rt.incrementCounter();
			rt.addValue("label", info.label);
			rt.addValue("n", info.n);
			rt.addValue("mean", info.mean);
			rt.addValue("variance", info.var);
		}
		rt.show("statistics");
		
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
					for(Point neighbor : conn.getNeighborIterable(p))
					{
						int neighborLabel=get(neighbor);
						if(neighborLabel!=label)
//						if(getAbsLabel(neighborLabel)!=label) // TODO insert if there exists already contour
						{
							ContourParticle particle = new ContourParticle();
							particle.label=label;
							particle.intensity=getIntensity(x, y);
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
	
	/**
	 * Removes a contour particle from its labelregion, 
	 * generates the newly produced contourpixels and updates statistics
	 */
	void removeFromContour(Point pIndex)
	{
		
		//TODO ??? where is the removal of p in itk
		// 
		
		ContourParticle p = contourContainer.get(pIndex);
		
		Connectivity2D_4 conn = new Connectivity2D_4();
		for(Point qIndex:conn.getNeighborIterable(pIndex))
		{
			int qLabel = get(qIndex);
			if(qLabel == p.label) // q is a inner point with the same label as p
			{
				ContourParticle q = new ContourParticle();
				q.intensity = getIntensity(qIndex);
				contourContainer.put(qIndex, q);
				
				set(pIndex, getNegLabel(qLabel));
			}
			else if(getAbsLabel(qLabel) == p.label) // q is contour of the same label
			{
				//TODO itk Line 1520, modifiedcounter
			}
		}
	}
	
	
	void addToContour(Point pIndex)
	{
		ContourParticle p = contourContainer.get(pIndex);
//		int pLabel = p.label;
		set(pIndex, getNegLabel(p.label));
		
		Connectivity conn = new Connectivity2D_8();
		for(Point qIndex: conn.getNeighborIterable(pIndex))
		{
			// from itk:
            /// It might happen that a point, that was already accepted as a 
            /// candidate, gets enclosed by other candidates. This
            /// points must not be added to the container afterwards and thus
            /// removed from the main list.
			if(get(qIndex)==getNegLabel(p.label) && isEnclosedByLabel(qIndex, p.label))
			{
				contourContainer.remove(qIndex);
				set(qIndex, p.label);
			}
		}
		
		if(isEnclosedByLabel(pIndex, p.label))
		{
			contourContainer.remove(p);
			set(pIndex, p.label);
		}
		
	}
	
	boolean isEnclosedByLabel(Point pIndex, int pLabel)
	{
		
		Connectivity conn = new Connectivity2D_4();
		for(Point qIndex : conn.getNeighborIterable(pIndex))
		{
			if(get(qIndex)!=pLabel)
			{
				return false;
			}
		}
		return true;
	}
	
	
	void grow()
	{
		// TODO chaos with motherlist / daughterlist / count. 
		
		HashMap<Point, ContourParticle> M;
		// M = candidate list
		M=(HashMap<Point, ContourParticle>) contourContainer.clone();
		
		Connectivity conn = new Connectivity2D_4();
		
		// iterate over all particles
		for(Entry<Point,ContourParticle> entry:M.entrySet())
		{
			Point pIndex = entry.getKey();			// coords of motherpoint
			ContourParticle p = entry.getValue();	// mother particle
			int pLabel = p.label;					// label of motherpoint 
			
			// particle-label to background label (shrinking)
			p.candidateLabel=bgLabel;
			p.energyDifference=calcEnergy(entry);
			
			
			// particle label to nieghbor labels (growing)
			for(Point qIndex:conn.getNeighborIterable(pIndex))
			{
				int label=get(qIndex);
				int qLabel=getAbsLabel(label);
				
				if(qLabel == forbiddenLabel || qLabel == pLabel)
				{
					// forbidden or self. do nothing
					continue;
				}
				
				ContourParticle q = contourContainer.get(qIndex);
				
				if(q==null) // (absLabel==bgLabel)
				{
					// grow in BG
					// create new particle and add to M
					q = new ContourParticle();
					q.label=bgLabel;
					q.candidateLabel=pLabel;
					q.intensity=getIntensity(qIndex);
					q.energyDifference = calcEnergy(entry);
					q.getMotherList().add(p);
					q.daughterFlag=true;
					
					//TODO !!! this may make problems with "for(entry : M.entrySet())"
					// consider javadoc to entrySet() "the results of the iteration are undefined"
					// construct a afterparty list M', add to a after the party
					M.put(qIndex, q);
				} 
				else // nonforbidden, nonself, existing, other label
				{
					if(q.candidateLabel==pLabel) // supported already by same region
					{
						q.getMotherList().add(p);
					}
					else // grow to other region
					{
						float dE=calcEnergy(entry);
						if(q.energyDifference > dE)
						{
							q.candidateLabel=qLabel;
							q.energyDifference=dE;
							
						}
					}
				} // else
				
				// neighborParticle is q in Algorithm 2 (Optimization)
				//TODO dont know yet where it is used
				
				q.getMotherList().add(p);
				
			} // for neighbors
		}
	}

	
	
	
	float calcEnergy(Entry<Point,ContourParticle> entry)
	{
		float E_gamma = calcGammaEnergy(entry);
		float E_tot=E_gamma + 0 + 0; //TODO other energies
		return E_tot;
	}
	
	
//	float CalculateCVEnergyDifference(InputPixelType aValue, unsigned int aFrom, unsigned int aTo) {
//        /**
//         * Here we have the possibility to either put the current pixel
//         * value to the BG, calculate the BG-mean and then calculate the
//         * squared distance of the pixel to both means, BG and the mean
//         * of the region (where the pixel currently still belongs to).
//         *
//         * The second option is to remove the pixel from the region and
//         * calculate the new mean of this region. Then compare the squared
//         * distance to both means. This option needs a region to be larger
//         * than 1 pixel/voxel.
//         */
//        EnergyDifferenceType vNewToMean = (m_Means[aTo] * m_Count[aTo] + aValue) / (m_Count[aTo] + 1);
//        return (aValue - vNewToMean) * (aValue - vNewToMean) -
//                (aValue - m_Means[aFrom]) * (aValue - m_Means[aFrom]);
//    }
//
//    float CalculateMSEnergyDifference(InputPixelType aValue, int aFrom, int aTo) {
//        float aNewToMean = (m_Means[aTo] * m_Count[aTo] + aValue) / (m_Count[aTo] + 1);
//        if (m_Variances[aFrom] <= 0 || m_Variances[aTo] <= 0) {
//            assert("Region has negative variance.");
//        }
//
//        return (aValue - aNewToMean) * (aValue - aNewToMean) / (2.0 * m_Variances[aTo]) +
//                0.5 * log(2.0 * Math.PI * m_Variances[aTo]) -
//                ((aValue - m_Means[aFrom]) * (aValue - m_Means[aFrom]) /
//                (2.0 * m_Variances[aFrom]) +
//                0.5 * log(2.0 * Math.PI * m_Variances[aFrom]));
//    }
	
	
	
	static float wGamma = 0.003f;
	float calcGammaEnergy(Entry<Point,ContourParticle> entry)
	{
		
		//TODO ? this only calcs energy for one region
		
		Point pIndex = entry.getKey();			// coords of motherpoint
		ContourParticle p = entry.getValue();	// mother particle
		int pLabel = p.label;					// label of motherpoint 
		
		int nSameNeighbors=0;
		Connectivity conn = new Connectivity2D_4();
		
		// version 3 with COnnectivity.getNeighbots(Point)
		for(Point neighbor:conn.getNeighborIterable(pIndex))
		{
			int neighborLabel=get(neighbor);
			neighborLabel=getAbsLabel(neighborLabel);
			if(neighborLabel==pLabel)
			{
				nSameNeighbors++;
			}
		}
		
		int nOtherNeighbors=4-nSameNeighbors;
		int dGamma = nSameNeighbors - nOtherNeighbors;
		
		return wGamma*dGamma;
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
	
	boolean isInnerLabel(int label)
	{
		if(label==forbiddenLabel || label==bgLabel || isContourLabel(label))
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	

	boolean isForbiddenLabel(int label) {
		return (label == forbiddenLabel);
	}
	
	public ImageProcessor getLabelImage() {
		return labelImage;
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
	
	//TODO merge with int getIntensity(int x, int y)
	int getIntensity(Point p)
	{
		return getIntensity(p.x[0], p.x[1]);
	}
	
	int getIntensity(int x, int y)
	{
		return originalIP.getPixel(x, y)[0];
	}

	/**
	 * sets the labelImage to val at point x,y
	 */
	private void set(int x, int y, int val) 
	{
		labelImage.set(x,y,val);
	}

	/**
	 * sets the labelImage to val at Point p
	 */
	private void set(Point p, int value) {
		//TODO multidimension
		labelImage.set(p.x[0], p.x[1], value);
	}
	
	
	
}
