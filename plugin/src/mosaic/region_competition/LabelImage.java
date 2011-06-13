package mosaic.region_competition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/*
//TODO TODOs
- at many places: unsure about removing/adding items while iterating. 
- refactor LabelImage, extract what's not supposed to be there (eg energy calc)
- let ContourParticle have ref to Point / inherit from? 
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
	ImageStack stack;
	
	HashMap<Point, ContourParticle> m_InnerContourContainer;
	HashMap<Integer, LabelInformation> labelMap;
	
	
	/**
	 * creates a new LabelImage with size of ip
	 * @param ip is saved as originalIP
	 */
	public LabelImage(ImagePlus ip) 
	{
		width=ip.getWidth();
		height=ip.getHeight();
		labelImage= new ShortProcessor(width, height);
//		super(ip.getWidth(), ip.getHeight());
		//TODO initial capacities
		m_InnerContourContainer = new HashMap<Point, ContourParticle>();
		labelMap = new HashMap<Integer, LabelInformation>();
		originalIP=ip;
		
		initMembers();
	}
	
	
	public void setStack(ImageStack stack)
	{
		this.stack=stack;
	}
	
	//////////////////////////////////////////////////
	
    boolean m_converged;
    static int m_OscillationHistoryLength = 10;
//    FixedArray<unsigned int, m_OscillationHistoryLength> m_OscillationsNumberHist;
//    FixedArray<float, m_OscillationHistoryLength> m_OscillationsEnergyHist;
    int m_OscillationsNumberHist[] = new int[m_OscillationHistoryLength];
    double m_OscillationsEnergyHist[] = new double[m_OscillationHistoryLength];

    float m_AcceptedPointsReductionFactor;
    float m_AcceptedPointsFactor;
    
    float m_EnergyContourLengthCoeff;
    float m_SigmaOfLoGFilter;
    /// Pushes the curve towards edges in the image
    float m_EnergyEdgeAttractionCoeff;
    float m_EnergyShapePriorCoeff;
    float m_EnergyRegionCoeff;
    float m_EnergySphericityCoeff;
    float m_BalloonForceCoeff;
    boolean m_EnergyUseCurvatureRegularization;

    float m_RegionMergingThreshold;
//    ArrayType m_SigmaPSF;
    boolean m_AllowFusion;
    boolean m_AllowFission;
    boolean m_AllowHandles;
    boolean m_UseRegionCompetition; // if fusion is disallowed, else digital topo is used.
    boolean m_RemoveNonSignificantRegions;
    boolean m_UseForbiddenRegion;
    boolean m_UseGaussianPSF;
    boolean m_UseShapePrior;
    boolean m_UseFastEvolution;
    int m_AreaThreshold;
//    ArrayType m_LocalCVEnergyRadius;
    int m_LocalLiEnergySigma;
    float m_CurvatureMaskRadius;
//    int m_ForbiddenRegionLabel;
//    typedef GaussianImageSource<InternalImageType> GaussianImageSourceType;
//    typename GaussianImageSourceType::Pointer m_GaussianImageSource;
    int m_iteration_counter; // member for debugging

	//Private:
    
	int m_MaxNbIterations;
	boolean m_InitializeFromRegion;
	EnergyFunctionalType m_EnergyFunctional;
    
	// ///////////////////////////////////////////////////

	void initMembers() {

		/**
		 * Initialize control members
		 */
		m_MaxNbIterations = 150;
		m_converged = false;
		m_AreaThreshold = 1; ///TODO find a good heuristic or stat test.

		/**
		 * Initialize energy related parameters
		 */
		// m_ContourLengthFunction =
		// ContourLengthApproxImageFunction<LabelImageType>::New();

		m_EnergyFunctional = EnergyFunctionalType.e_CV;
		m_EnergyUseCurvatureRegularization = true;
		m_EnergyContourLengthCoeff = 0.003f; // 0.04;//0.003f;
		m_EnergyRegionCoeff = 1.0f;

		m_EnergyEdgeAttractionCoeff = 0; ///EXPERIMENTAL!
		m_SigmaOfLoGFilter = 2.f;

		m_EnergySphericityCoeff = 0; ///EXPERIMENTAL!
		m_EnergyShapePriorCoeff = 0.0f;

		m_BalloonForceCoeff = 0.0f; /// Experimental

		m_AllowFusion = true;
		m_AllowFission = true;
		m_AllowHandles = true;

		m_RemoveNonSignificantRegions = true;
		m_UseForbiddenRegion = false;
		m_UseShapePrior = false;
		m_UseGaussianPSF = true;
		m_UseFastEvolution = false;
		// m_SigmaPSF.Fill(1);

		///use competing regions istead the concept of DT if fusion is
		// disallowed.
		m_UseRegionCompetition = false; // still has errors.

		// m_ForbiddenRegionLabel = NumericTraits<LabelPixelType>::max();

		// for(int vD = 0; vD < m_Dim; vD++) {
		// m_LocalCVEnergyRadius[vD] = 1;
		// }
		m_LocalLiEnergySigma = 5;
		m_CurvatureMaskRadius = 4;
		m_RegionMergingThreshold = 0.1f;
		m_iteration_counter = 0;

		for(int vI = 0; vI < m_OscillationHistoryLength; vI++) {
			m_OscillationsNumberHist[vI] = 0;
			m_OscillationsEnergyHist[vI] = 0;
		}
		m_AcceptedPointsFactor = 1;
		m_AcceptedPointsReductionFactor = 0.5f;
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
	 * without contours
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
		
		clearStats();
		
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
	
	void clearStats()
	{
		//clear stats
		
		for(LabelInformation stat: labelMap.values())
		{
			stat.reset();
		}
	}
	
	
	/**
	 * as computeStatistics, does not use iterative approach
	 * (first computes sum of values and sum of values^2)
	 */
	public void renewStatistics()
	{
		
		clearStats();

		
		for (int x = 0; x < width; x++) 
		{
			for (int y = 0; y < height; y++) 
			{
				int label = get(x, y);
				int absLabel = getAbsLabel(label);

				if (absLabel != forbiddenLabel /* && absLabel != bgLabel*/) 
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
					stats.count++;
					stats.mean+=val;
					stats.var+=val*val;
				}
			}
		} // for all pixel
		
		//now we have in all LabelInformation in mean the sum of the values, int var the sum of val^2
		for(LabelInformation stat: labelMap.values())
		{
			int n= stat.count;
            if (n > 1) {
                stat.var = (stat.var - stat.mean*stat.mean / n) / (n-1);
            } else {
                stat.var = 0;
            }
            stat.mean = stat.mean/n;
//TODO itk: was in itk            m_Intensities[vAbsLabel] = m_Means[vAbsLabel];
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
			rt.addValue("n", info.count);
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
							m_InnerContourContainer.put(p, particle);
							
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
		for(Entry<Point, ContourParticle> entry:m_InnerContourContainer.entrySet())
		{
			Point key = entry.getKey();
			ContourParticle value = entry.getValue();
			//TODO cannot set neg values to ShortProcessor
			set(key, getNegLabel(value.label));
		}
	}
	
	/**
	 * Removes a contour particle from its label region (moves it to bg), 
	 * generates the new contour particles, adds to container
	 * itk::AddNeighborsAtRemove
	 */
	void AddNeighborsAtRemove(int aAbsLabel, Point pIndex)
	{
		
		//TODO removal of p in itk; in ChangeContourPointLabelToCandidateLabel
		//TODO statistic update? 
		
		//TODO p is not used
		ContourParticle p = m_InnerContourContainer.get(pIndex);
		
		Connectivity2D_4 conn = new Connectivity2D_4();
		for(Point qIndex:conn.getNeighborIterable(pIndex))
		{
			int qLabel = get(qIndex);
			
			//can the labels be negative? somewhere, they are set (maybe only temporary) to neg values
			
			if(isInnerLabel(qLabel) && qLabel==aAbsLabel) // q is a inner point with the same label as p
			{
				ContourParticle q = new ContourParticle();
				q.label=aAbsLabel;
				q.candidateLabel=bgLabel;
				q.intensity = getIntensity(qIndex);
				
				set(qIndex, getNegLabel(aAbsLabel));
				m_InnerContourContainer.put(qIndex, q);
			}
			else if(isContourLabel(qLabel)&& qLabel == aAbsLabel) // q is contour of the same label
			{
				//TODO itk Line 1520, modifiedcounter
                /// the point is already in the contour. We reactivate it by
                /// ensuring that the energy is calculated in the next iteration:
//				contourContainer.get(qIndex).m_modifiedCounter = 0;
//                m_InnerContourContainer[vI].m_modifiedCounter = 0;
			}
		}
	}
	
	
	/**
	 * 
	 * itk::MaintainNeighborsAtAdd
     * Maintain the inner contour container:
     * - Remove all the indices in the BG-connected neighborhood, that are
     *   interior points, from the contour container.
     *   Interior here means that none neighbors in the FG-Neighborhood
     *   has a different label.
     */
//	old: void addToContour(Point pIndex)
    void MaintainNeighborsAtAdd(int aLabelAbs, Point pIndex) 
	{
		ContourParticle p = m_InnerContourContainer.get(pIndex);
		
		int aLabelNeg = getNegLabel(aLabelAbs);
		
        // itk 1646: we set the pixel value already to ensure the that the 'enclosed' check
        // afterwards works.
		//TODO is p.label (always) the correct label?
		set(pIndex, getNegLabel(aLabelAbs));
		
		Connectivity conn = new Connectivity2D_8();
		for(Point qIndex: conn.getNeighborIterable(pIndex))
		{
			// from itk:
            /// It might happen that a point, that was already accepted as a 
            /// candidate, gets enclosed by other candidates. This
            /// points must not be added to the container afterwards and thus
            /// removed from the main list.
			
			// TODO ??? why is BGconn important? a BGconnected neighbor cannot 
			// change the "enclosed" status for FG?
			if(get(qIndex)==aLabelNeg && isEnclosedByLabel(qIndex, aLabelAbs))
			{
				m_InnerContourContainer.remove(qIndex);
				set(qIndex, aLabelAbs);
			}
		}
		
		if(isEnclosedByLabel(pIndex, aLabelAbs))
		{
			m_InnerContourContainer.remove(pIndex);
			set(pIndex, aLabelAbs);
		}
		
	}
	
	boolean isEnclosedByLabel(Point pIndex, int pLabel)
	{
		int absLabel = getAbsLabel(pLabel);
		Connectivity conn = new Connectivity2D_4();
		for(Point qIndex : conn.getNeighborIterable(pIndex))
		{
			if(getAbsLabel(get(qIndex))!=absLabel)
			{
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * itk::RebuildCandidateList
	 * ij::RebuildCandidateList see below
	 */
	void grow()
	{
		// TODO chaos with motherlist / daughterlist / count. 
		
		HashMap<Point, ContourParticle> M;
		// M = candidate list
		M=(HashMap<Point, ContourParticle>) m_InnerContourContainer.clone();
		
		HashMap<Point, ContourParticle> M2 = new HashMap<Point, ContourParticle>();
		
		
		Connectivity conn = new Connectivity2D_4();
		
		// iterate over all particles
		for(Entry<Point,ContourParticle> entry:M.entrySet())
		{
			Point pIndex = entry.getKey();			// coords of motherpoint
			ContourParticle p = entry.getValue();	// mother particle
			int pLabel = p.label;					// label of motherpoint 
			
			// particle-label to background label (shrinking)
			p.candidateLabel=bgLabel;
			p.isMother=true;
			p.isDaughter=false;
			
			p.energyDifference=calcEnergy(entry); //TODO this to bg
			
			
			// particle label to neighbor labels (growing)
			for(Point qIndex:conn.getNeighborIterable(pIndex))
			{
				int label=get(qIndex);
				int qLabel=getAbsLabel(label);
				
				if(qLabel == forbiddenLabel || qLabel == pLabel)
				{
					// forbidden or self. do nothing
					continue;
				}
				
				ContourParticle q = m_InnerContourContainer.get(qIndex);
				
				// itk::Tell the mother about the daughter:
				p.getDaughterList().add(qIndex);
				
				if(q==null) // (absLabel==bgLabel)
				{
					// grow in BG
					// create new particle and add to M
					
					q = new ContourParticle();
					//TODO was wird hier implizit doppelt gesetzt nach constructor
					q.candidateLabel=pLabel;
					q.label=bgLabel;
					q.intensity=getIntensity(qIndex);
					q.isDaughter=true;
					q.isMother=false;
					q.m_processed=false;
					q.referenceCount=1;
					q.energyDifference = calcEnergy(entry); //TODO change neighbor to plabel
					
					q.setLabelHasBeenTested(pLabel);
					
					q.getMotherList().add(pIndex);
					
					//TODO !!! this may make problems with "for(entry : M.entrySet())"
					// consider javadoc to entrySet() "the results of the iteration are undefined"
					
					//TODO is this ok?
					// construct a afterparty list M2, add to a after the party
					M2.put(qIndex, q);
				} 
				else // nonforbidden, nonself, existing, other label
				{
					
					q.isDaughter=true;
					q.getMotherList().add(pIndex);
					
					//itk::3501
                    /// Check if the energy difference for this candiate
                    /// label has not yet been calculated.
					if(!q.hasLabelBeenTested(pLabel))
					{
						q.setLabelHasBeenTested(pLabel);
						double dE=calcEnergy(entry); //TODO change neighbor to plabel
						if(dE < q.energyDifference)
						{
							q.candidateLabel=pLabel;
							q.energyDifference=dE;
							q.referenceCount=1;
						}
						
					}
					else {
						//itk::3512
                        /// If the propagating label is the same as the candidate label,
                        /// we have found 2 or more mothers of for this contour point.ten
                        if (q.candidateLabel == pLabel) {
//                            if(m_iteration_counter == 30 && vLabelOfPropagatingRegion == 30){
//                                std::cout << "halt at rebuildcandlist: " << vContourPointItr->second << std::endl;
//                            }
                            q.referenceCount++;
                        }
                    }
					
					
					//non-itk version (paper algorithm?)
//					if(q.candidateLabel==pLabel) // supported already by same region
//					{
//						q.getMotherList().add(pIndex);
//					}
//					else // grow to other region
//					{
//						float dE=calcEnergy(entry);
//						if(q.energyDifference > dE)
//						{
//							q.candidateLabel=qLabel;
//							q.energyDifference=dE;
//						}
//					}
				} // else
				
				// neighborParticle is q in Algorithm 2 (Optimization)
				//TODO dont know yet where it is used
				
				q.getMotherList().add(pIndex);
				
			} // for neighbors
		}
	}

	
	
	
	void RebuildCandidateList(HashMap<Point, ContourParticle> aReturnContainer)
		{
	        aReturnContainer.clear();
	
	//        LabelImageNeighborhoodIteratorRadiusType vLabelImageIteratorRadius;
	//        vLabelImageIteratorRadius.Fill(1);
	//        LabelImageNeighborhoodIteratorType vLabelImageIterator(vLabelImageIteratorRadius,
	//                m_LabelImage,
	//                m_LabelImage->GetBufferedRegion());
	
	        /// Add all the mother points - this is copying the inner contour list.
	        /// (Things get easier afterwards if this is done in advance.)
	//        CellListedHashMapIteratorType vPointIterator = m_InnerContourContainer.begin();
	//        CellListedHashMapIteratorType vPointsEnd = m_InnerContourContainer.end();
	        for (Entry<Point, ContourParticle> vPointIterator: m_InnerContourContainer.entrySet()) 
	        {
	            Point vCurrentIndex = vPointIterator.getKey();
	            ContourParticle vVal = vPointIterator.getValue();
	
	//            if (m_UseFastEvolution && vVal.m_modifiedCounter != 0 &&
	//                    (m_iteration_counter) % vVal.m_modifiedCounter != 0) {
	//                continue;
	//            }
	
	            vVal.candidateLabel = 0;
	            vVal.referenceCount = 0; // doesn't matter for the BG
	            vVal.isMother = true;
	            vVal.isDaughter = false;
	            vVal.m_processed = false;
	//TODO itk: line 3420 ist doppelt drin?
	//            vVal.m_modifiedCounter = vVal.m_modifiedCounter + 1;
	//            vPointIterator->second.m_modifiedCounter++;
	            vVal.energyDifference = CalculateEnergyDifferenceForLabel(vCurrentIndex, vVal, bgLabel);
	            vVal.getMotherList().clear(); // this is indeed necessary!
	            vVal.getDaughterList().clear(); // this is necessary!!
	            
	            vVal.getTestedList().clear();
	            vVal.setLabelHasBeenTested(bgLabel);
	
	            aReturnContainer.put(vCurrentIndex, vVal);
	        }
	
	        /// Iterate the contour list and visit all the neighbors in the
	        /// FG-Neighborhood.
	//        vPointIterator = m_InnerContourContainer.begin();
	        for (Entry<Point, ContourParticle> vPointIterator: m_InnerContourContainer.entrySet()) 
	        {
	            Point vCurrentIndex = vPointIterator.getKey();
	            ContourParticle vVal = vPointIterator.getValue();
	
	//            if (m_UseFastEvolution && vVal.m_modifiedCounter - 1 != 0 &&
	//                    (m_iteration_counter) % (vVal.m_modifiedCounter - 1) != 0) {
	//                continue;
	//            }
	
	            int vLabelOfPropagatingRegion = vVal.label;
	//            vLabelImageIterator.SetLocation(vCurrentIndex);
	
	            Connectivity2D_4 conn = new Connectivity2D_4();
	            for (Point q : conn.getNeighborIterable(vCurrentIndex)) {
	//                InputImageOffsetType vOff = m_NeighborsOffsets_FG_Connectivity[vI];
	                int vLabelOfDefender = getAbsLabel(get(q));
	                if (vLabelOfDefender == forbiddenLabel) {
	                    continue;
	                }
	                if (vLabelOfDefender != vLabelOfPropagatingRegion) {
	                    Point vNeighborIndex = q;
	
	                    /// Tell the mother about the daughter:
	                    aReturnContainer.get(vCurrentIndex).getDaughterList().add(vNeighborIndex);
	
	//                    CellListedHashMapIteratorType vContourPointItr = aReturnContainer->find(vNeighborIndex);
	                   ContourParticle vContourPointItr = aReturnContainer.get(vNeighborIndex);
	                    if (vContourPointItr == null) {
	                        /// create a new entry (a daughter), the contour point
	                        /// has not been part of the contour so far.
	
	                        //                        OuterContourContainerValueType::ContourPointCandidateElement
	                        //                        vCandidateElement(vLabelOfPropagatingRegion, 0);
	
	                        ContourParticle vOCCValue= new ContourParticle();
	                        //                        vOCCValue.m_candidates.insert(vCandidateElement);
	                        //
							vOCCValue.candidateLabel = vLabelOfPropagatingRegion;
							vOCCValue.label = vLabelOfDefender;
							vOCCValue.intensity = getIntensity(vNeighborIndex);
							vOCCValue.isDaughter = true;
							vOCCValue.isMother = false;
							vOCCValue.m_processed = false;
							vOCCValue.referenceCount = 1;
	                        //TODO modified counter
	//                        vOCCValue.m_modifiedCounter = 0;
	                        vOCCValue.energyDifference = CalculateEnergyDifferenceForLabel(vNeighborIndex, vOCCValue, vLabelOfPropagatingRegion);
	                        vOCCValue.setLabelHasBeenTested(vLabelOfPropagatingRegion);
	                        /// Tell the daughter about the mother:
	                        vOCCValue.getMotherList().add(vCurrentIndex);
	
	                        aReturnContainer.put(vNeighborIndex, vOCCValue);
	
	                    } else { /// the point is already part of the candidate list
	                        vContourPointItr.isDaughter = true;
	                        /// Tell the daughter about the mother(label does not matter!):
	                        vContourPointItr.getMotherList().add(vCurrentIndex);
	
	                        /// Check if the energy difference for this candiate
	                        /// label has not yet been calculated.
	                        if (!vContourPointItr.hasLabelBeenTested((vLabelOfPropagatingRegion))) 
	                        {
	                            vContourPointItr.setLabelHasBeenTested(vLabelOfPropagatingRegion);
	                            double vEnergyDiff =
	                                    CalculateEnergyDifferenceForLabel(
	                                    vNeighborIndex, (vContourPointItr), vLabelOfPropagatingRegion);
	                            if (vEnergyDiff < vContourPointItr.energyDifference) {
	                                vContourPointItr.candidateLabel = vLabelOfPropagatingRegion;
	                                vContourPointItr.energyDifference = vEnergyDiff;
	                                vContourPointItr.referenceCount = 1;
	                            }
	                        } else {
	                            /// If the propagating label is the same as the candidate label,
	                            /// we have found 2 or more mothers of for this contour point.ten
	                            if (vContourPointItr.candidateLabel == vLabelOfPropagatingRegion) {
	//                                if(m_iteration_counter == 30 && vLabelOfPropagatingRegion == 30){
	//                                    std::cout << "halt at rebuildcandlist: " << vContourPointItr->second << std::endl;
	//                                }
	                                vContourPointItr.referenceCount++;
	                            }
	                        }
	
	                        //                        OuterContourContainerValueType::ContourPointCandidateListType::iterator
	                        //                        vCandLabelIt = vContourPointItr->m_candidates.begin();
	                        //                        OuterContourContainerValueType::ContourPointCandidateListType::iterator
	                        //                        vCandLabelItEnd = vContourPointItr->m_candidates.end();
	                        //                        bool vLabelExists = false;
	                        //                        for (; vCandLabelIt != vCandLabelItEnd; ++vCandLabelIt) {
	                        //                            if (vCandLabelIt->m_Label == vLabelOfPropagatingRegion) {
	                        //                                vLabelExists = true;
	                        //                            }
	                        //                        }
	                        //                        if (!vLabelExists) {
	                        //                            OuterContourContainerValueType::ContourPointCandidateElement
	                        //                            vCandidateElement(vLabelOfPropagatingRegion, 0);
	                        //                            vContourPointItr->m_candidates.insert(vCandidateElement);
	                        //                        }
	                    }
	                }
	            }
	        }
	    
		}

	double calcEnergy(Entry<Point,ContourParticle> entry)
	{
		double E_gamma = calcGammaEnergy(entry);
		double E_tot=E_gamma + 0 + 0; //TODO other energies
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
	double calcGammaEnergy(Entry<Point,ContourParticle> entry)
	{
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
	
	
	
	
	private double CalculateEnergyDifferenceForLabel(Point aContourIndex, ContourParticle aContourPointPtr, int aToLabel) {
        //        typedef ContourPoint::ContourPointCandidateListType::iterator CandLabelItType;
        //        CandLabelItType vCandLabelIt = aContourPointIt->second.m_candidates.begin();
        //        CandLabelItType vCandLabelItend = aContourPointIt->second.m_candidates.end();
        //
        //        for (; vCandLabelIt != vCandLabelItend; ++vCandLabelIt) {
        //            LabelAbsPixelType aToLabel = aContourPointPtr->m_candidateLabel;
        //            OuterContourContainerKeyType vCurrentIndex = aContourPointPtr->first;
        double vEnergy = 0;
        
        int vCurrentImageValue = aContourPointPtr.intensity;
        int vCurrentLabel = aContourPointPtr.label;

		/*
        /// For the full-region based energy models, register competing regions
        /// undergo a merge.
        if (m_EnergyFunctional == EnergyFunctionalType.e_CV || m_EnergyFunctional == EnergyFunctionalType.e_MS ||
                m_EnergyFunctional == EnergyFunctionalType.e_Deconvolution) {
            /// If we are competing (this is detected when the defender
            /// label is not equal to the background label; we checked
            /// already for the forbidden label and conqueror label), we
            /// store this event to check afterwards if we should merge
            /// the 2 regions.
            if (m_UseRegionCompetition) {
                if (vCurrentLabel != 0 && aToLabel != 0) { // we are competeing.
                    /// test if merge should be done:
                    /// TODO: this test is perfromed for each point, this is un-
                    ///       necessary.
                    if (CalculateKLMergingCriterion(vCurrentLabel, aToLabel) <
                            m_RegionMergingThreshold) {

                        CompetingRegionsPairType vPair;
                        vPair[0] = (vCurrentLabel < aToLabel) ?
                                vCurrentLabel : aToLabel;
                        vPair[1] = (vCurrentLabel > aToLabel) ?
                                vCurrentLabel : aToLabel;

                        //TODO: could be faster by first checking if the key
                        //      exists...
                        m_CompetingRegionsMap[vPair] = aContourIndex;
                        /// Ensure the point does not move since we'd like to merge
                        /// here. Todo so, we set the energy to a large value.
                        return NumericTraits<EnergyDifferenceType>::max();
                    }
                }
            }
        }

*/
//TODO expand to itk version below
        /// Calculate the change in energy due to the change of intensity when changing
        /// from one label 'from' to another 'to'.
        if (m_EnergyRegionCoeff != 0) {
            if (m_EnergyFunctional == EnergyFunctionalType.e_PSwithCurvatureFlow) 
            {
//                vEnergy += CalculatePSwithCurvatureFlowEnergyDifference(
//                        this->GetDataInput(), m_LabelImage, aContourIndex,
//                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } 
            else if (m_EnergyFunctional == EnergyFunctionalType.e_CV) 
            {
                vEnergy += m_EnergyRegionCoeff * CalculateCVEnergyDifference(
                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } 
            else if (m_EnergyFunctional == EnergyFunctionalType.e_MS) 
            {
                vEnergy += m_EnergyRegionCoeff * CalculateMSEnergyDifference(
                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } 
            else if (m_EnergyFunctional == EnergyFunctionalType.e_LocalCV) 
            {
//                vEnergy += m_EnergyRegionCoeff * CalculateLocalCVEnergyDifference(
//                        this->GetDataInput(), m_LabelImage, aContourIndex,
//                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } 
            else if (m_EnergyFunctional == EnergyFunctionalType.e_LocalLi) 
            {
//                vEnergy += m_EnergyRegionCoeff * CalculateLiEnergyDifference_Full(
//                        this->GetDataInput(), m_LabelImage, aContourIndex,
//                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } 
            else if (m_EnergyFunctional == EnergyFunctionalType.e_Deconvolution) 
            {
//                vEnergy += m_EnergyRegionCoeff * CalculateDeconvolutionEnergyDifference(
//                        this->GetDataInput(), m_LabelImage, aContourIndex,
//                        vCurrentImageValue, vCurrentLabel, aToLabel);
            }
        }
		
		
/* 
		
        /// Calculate the change in energy due to the change of intensity when changing
        /// from one label 'from' to another 'to'.
        if (m_EnergyRegionCoeff != 0) {
            if (m_EnergyFunctional == EnergyFunctionalType.e_PSwithCurvatureFlow) {
                vEnergy += CalculatePSwithCurvatureFlowEnergyDifference(
                        this->GetDataInput(), m_LabelImage, aContourIndex,
                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } else if (m_EnergyFunctional == EnergyFunctionalType.e_CV) {
                vEnergy += m_EnergyRegionCoeff * CalculateCVEnergyDifference(
                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } else if (m_EnergyFunctional == EnergyFunctionalType.e_MS) {
                vEnergy += m_EnergyRegionCoeff * CalculateMSEnergyDifference(
                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } else if (m_EnergyFunctional == EnergyFunctionalType.e_LocalCV) {
                vEnergy += m_EnergyRegionCoeff * CalculateLocalCVEnergyDifference(
                        this->GetDataInput(), m_LabelImage, aContourIndex,
                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } else if (m_EnergyFunctional == EnergyFunctionalType.e_LocalLi) {
                vEnergy += m_EnergyRegionCoeff * CalculateLiEnergyDifference_Full(
                        this->GetDataInput(), m_LabelImage, aContourIndex,
                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } else if (m_EnergyFunctional == EnergyFunctionalType.e_Deconvolution) {
                vEnergy += m_EnergyRegionCoeff * CalculateDeconvolutionEnergyDifference(
                        this->GetDataInput(), m_LabelImage, aContourIndex,
                        vCurrentImageValue, vCurrentLabel, aToLabel);
            }
        }

		if (m_EnergyUseCurvatureRegularization &&
                m_EnergyFunctional != EnergyFunctionalType.e_PSwithCurvatureFlow &&
                m_EnergyContourLengthCoeff != 0) {
            if (m_EnergyFunctional == EnergyFunctionalType.e_Deconvolution) {
                vEnergy += //m_Intensities[aToLabel] * m_Intensities[aToLabel] *
                        m_EnergyContourLengthCoeff * CalculateCurvatureBasedGradientFlow(
                        this->GetDataInput(), m_LabelImage, aContourIndex,
                        vCurrentLabel, aToLabel);
            } else if (m_EnergyFunctional == EnergyFunctionalType.e_CV) {
                vEnergy += //m_Means[aToLabel] * // m_Means[aToLabel] *
                        m_EnergyContourLengthCoeff * CalculateCurvatureBasedGradientFlow(
                        this->GetDataInput(), m_LabelImage, aContourIndex,
                        vCurrentLabel, aToLabel);
            } else {
                vEnergy += m_EnergyContourLengthCoeff * CalculateCurvatureBasedGradientFlow(
                        this->GetDataInput(), m_LabelImage, aContourIndex,
                        vCurrentLabel, aToLabel);
            }
        }

		if (!m_EnergyUseCurvatureRegularization &&
                m_EnergyFunctional == EnergyFunctionalType.e_PSwithCurvatureFlow &&
                (m_EnergyContourLengthCoeff != 0 || m_EnergySphericityCoeff != 0)) {
            // calculate the change in energy due to the length of the contour
            float vChangeInLength = -m_ContourLengthFunction->EvaluateLengthChange(aContourIndex, aToLabel);
            vEnergy += m_EnergyContourLengthCoeff * vChangeInLength;

            //            // calculate the change in energy due to sphericity for all regions
            //            // but the BG:
            //            // TODO: use specialized BG label instead of 0
            //            double vSphericityToBefore = 1.0;
            //            double vSphericityFromBefore = 1.0;
            //            double vSphericityToAfter = 1.0;
            //            double vSphericityFromAfter = 1.0;
            //            if (aTo != 0) {
            //                vSphericityToBefore = (m_PI_1_3 * pow(6.0 * m_Lengths[aTo], 2.0 / 3.0))
            //                        / (m_Count[aTo]);
            //                vSphericityToAfter = (m_PI_1_3) * pow(6.0 * m_Lengths[aTo] + vChangeInLength, 2.0 / 3.0)
            //                        / (m_Count[aTo] + 1);
            //                vEnergy += m_EnergySphericityCoeff *
            //                        ((1.0 - vSphericityToAfter) - (1.0 - vSphericityToBefore));
            //            }
            //            if(vCurrentLabel != 0) {
            //                vSphericityFromBefore = (m_PI_1_3 * pow(6.0 * m_Lengths[vCurrentLabel], 2.0 / 3.0))
            //                        / (m_Count[vCurrentLabel]);
            //                vSphericityFromAfter = (m_PI_1_3) * pow(6.0 * m_Lengths[vCurrentLabel] + vChangeInLength, 2.0 / 3.0)
            //                        / (m_Count[vCurrentLabel] - 1);
            //                vEnergy += m_EnergySphericityCoeff *
            //                        ((1.0 - vSphericityFromAfter) - (1.0 - vSphericityFromBefore));
            //            }
        }

		if (m_EnergyEdgeAttractionCoeff != 0) {
            // calculate the change in energy due to edge attraction:
            float vChangeInEdgeAttractionEnergy = -m_EdgeImage->GetPixel(aContourIndex);
            vEnergy += m_EnergyEdgeAttractionCoeff * vChangeInEdgeAttractionEnergy;
        }

		if (m_UseShapePrior) {
            float vChangeInShapeEnergy =
                    CalculateShapePriorEnergyDifference(aContourIndex, vCurrentLabel, aToLabel, m_Moments2D);
			vEnergy += m_EnergyShapePriorCoeff * vChangeInShapeEnergy;
            //            std::cout << "Energy difference of a contour point from label: " << vCurrentLabel << " to label: " << aToLabel << ": " << vEnergy << std::endl;
        }

        /// add a bolloon force:
        if(vCurrentLabel == 0) {
			vEnergy -= m_BalloonForceCoeff * vCurrentImageValue;
        }

        //        }
 */
        return vEnergy;

    }

	
	
    double CalculateCVEnergyDifference(int aValue, int fromLabel, int toLabel) {
        /**
         * Here we have the possibility to either put the current pixel
         * value to the BG, calculate the BG-mean and then calculate the
         * squared distance of the pixel to both means, BG and the mean
         * of the region (where the pixel currently still belongs to).
         *
         * The second option is to remove the pixel from the region and
         * calculate the new mean of this region. Then compare the squared
         * distance to both means. This option needs a region to be larger
         * than 1 pixel/voxel.
         */
    	
    	LabelInformation to = labelMap.get(toLabel);
    	LabelInformation from = labelMap.get(fromLabel);
        double vNewToMean = (to.mean * to.count + aValue) / (to.count + 1);
        return (aValue - vNewToMean) * (aValue - vNewToMean) -
                (aValue - from.mean) * (aValue - from.mean);
    }
	
    double CalculateMSEnergyDifference(int aValue, int fromLabel, int toLabel) {
    	
    	LabelInformation to = labelMap.get(toLabel);
    	LabelInformation from = labelMap.get(fromLabel);
    	
        double aNewToMean = (to.mean * to.count + aValue) / (to.count + 1);
//        if (aFrom.var <= 0 || aTo.var <= 0) {
//            assert("Region has negative variance.");
//        }

		double M_PI   =    Math.PI;

        return ((aValue - aNewToMean) * (aValue - aNewToMean) / (2.0 * to.var) +
                0.5 * Math.log(2.0 * M_PI * to.var) -
                ((aValue - from.mean) * (aValue - from.mean) /
                (2.0 * from.var) +
                0.5 * Math.log(2.0 * M_PI * from.var)));
    }
	
	
	
	
	
	
	
	
	//TODO !!!! BIG TODO: check all the for-loops, if adding within while iterating works
	
	/*
	 * comments with // on the left border are itk-code-fragments to check correctness of java translation
	 * comments with indentation and/or triple-/// is commented code from itk
	 */
	
	boolean IterateContourContainerAndAdd()
	{
		
		//TODO to be used in IterateContourContainerAndAdd(). elsewhere?
		HashMap<Point, ContourParticle> m_AllCandidates = new HashMap<Point, ContourParticle>();
		HashMap<Point, ContourParticle> m_CompetingRegionsMap = new HashMap<Point, ContourParticle>();
		
		//TODO set initially to boolean vConvergence = true;
	        boolean vConvergence = false;
	        
	        m_AllCandidates.clear();

	        List<Point> vLegalIndices = new LinkedList<Point>();
	        List<Point> vIllegalIndices = new LinkedList<Point>();
	        
	        /// clear the competing regions map, it will be refilled in RebuildCandidateList:
	        m_CompetingRegionsMap.clear();
	        RebuildCandidateList(m_AllCandidates);


	        /**
	         * Read the topology-dependency graphs and build containers:
	         */
		
//	        typedef std::list<ContourPointWithIndexType> NetworkMembersListType;

	        /**
	         * Find topologically compatible candidates and store their indices in
	         * vLegalIndices.
	         */
	        
	        for (Entry<Point,ContourParticle> vPointIterator : m_AllCandidates.entrySet())
	        {
	        	
				Point pIndex = vPointIterator.getKey();
	        	ContourParticle p = vPointIterator.getValue();

	            /// Check if this point already was processed
	            if(!p.m_processed) 
	            {

	                /// Check if it is a mother: only mothers can be seed points
	                /// of topological networks. Daughters are always part of a
	                /// topo network of a mother.
	                if (!p.isMother)
	                {
	                	continue;
	                }

	                /**
	                 * Build the dependency network for this seed point:
	                 */
	                Stack<Point> vIndicesToVisit = new Stack<Point>();
	                List<ContourPointWithIndexType> vSortedNetworkMembers= new LinkedList<ContourPointWithIndexType>();
	                vIndicesToVisit.push(pIndex);
	                p.m_processed = true;

	                while (!vIndicesToVisit.empty()) {

	                    Point vSeedIndex = vIndicesToVisit.pop();

	                    ContourParticle vCurrentMother = m_AllCandidates.get(vSeedIndex);
//	                    if(!vCurrentMother.m_processed) { //DEBUG OK!
//	                        std::cout << "Warning: unprocessed index in seed stack: "
	                    //                                << vSeedIndex << vCurrentMother << std::endl;
	                    //                    }

	                    /// Add the seed point to the network
	                    ContourPointWithIndexType vSeedContourPointWithIndex = new ContourPointWithIndexType(vSeedIndex, vCurrentMother);
	                    vSortedNetworkMembers.add(vSeedContourPointWithIndex);

	                    // Iterate all children of the seed, push to the stack if there
	                    // is a mother.
//	                    typename ContourPointType::DaughterIndexListType::iterator vDaughterIt =
//	                            vCurrentMother.m_daughterIndices.begin();
//	                    typename ContourPointType::DaughterIndexListType::iterator vDaughterItEnd =
//	                            vCurrentMother.m_daughterIndices.end();

	                    List<Point> vDaughterIt = vCurrentMother.getDaughterList();
	                    
	                    for (Point vDaughterContourIndex : vDaughterIt) {

	                        //                        if(vAllCandidates.find(vDaughterContourIndex) == vAllCandidates.end())
	                        //                            std::cout << "daughter index found not in the list: " << vDaughterContourIndex << std::endl;

	                        ContourParticle vDaughterContourPoint = m_AllCandidates.get(vDaughterContourIndex);

	                        if (!vDaughterContourPoint.m_processed) {
	                            vDaughterContourPoint.m_processed = true;

	                            if (vDaughterContourPoint.isMother) {
	                                vIndicesToVisit.push(vDaughterContourIndex);
	                            } else {
	                                ContourPointWithIndexType vDaughterContourPointWithIndex = new ContourPointWithIndexType(vDaughterContourIndex, vDaughterContourPoint);
	                                vSortedNetworkMembers.add(vDaughterContourPointWithIndex);
	                            }

	                            /// Push all the non-processed mothers of this daughter to the stack
//	                            typename ContourPointType::MotherIndexListType::iterator vDMIt =
//	                                    vDaughterContourPoint.m_motherIndices.begin();
//	                            typename ContourPointType::MotherIndexListType::iterator vDMItEnd =
//	                                    vDaughterContourPoint.m_motherIndices.end();
//	                            
	                            List<Point>vDMIt = vDaughterContourPoint.getMotherList();
	                            
	                            for (Point vDM : vDMIt) 
	                            {

	                                ContourParticle vMotherOfDaughterPoint = m_AllCandidates.get(vDM);
	                                if (!vMotherOfDaughterPoint.m_processed) {
	                                    vMotherOfDaughterPoint.m_processed = true;
	                                    vIndicesToVisit.push(vDM);
	                                }
	                            }
	                        }
	                    }
	                }

	                /**
	                 * sort the network
	                 */
	                Collections.sort(vSortedNetworkMembers);

	                /**
	                 * Filtering: Accept all members in ascending order that are
	                 * compatible with the already selected members in the network.
	                 */
	                
	                //TODO HashSet problem (maybe need hashmap)
	                HashSet<Point> vSelectedCandidateIndices = new HashSet<Point>();

	                //                ContourIndexType vFirstIndex = vNetworkIt->m_ContourIndex;
	                //                vSelectedCandidateIndices.insert(vFirstIndex);
	                //                //                ContourIndexType vDummyIndex;
	                //                //                vSelectedCandidateIndices.insert(vDummyIndex);
	                //                ++vNetworkIt;
	                for (ContourPointWithIndexType vNetworkIt : vSortedNetworkMembers) {

	                    /// If a mother is accepted, the reference count of all the
	                    /// daughters (with the same label) has to be decreased.
	                    /// Rules: a candidate in the network is a legal candidate if:
	                    /// - If (daughter): The reference count >= 1. (Except the
	                    ///                  the candidate label is the BG - this allows
	                    ///                  creating BG regions inbetween two competing
	                    ///                  regions).
	                    /// - If ( mother ): All daughters (with the same 'old' label) in the
	                    ///   accepted list have still a reference count > 1.
	                    boolean vLegalMove = true;

	                    ///
	                    /// RULE 1: If c is a daughter point, the reference count r_c is > 0.
	                    ///
	                    if (vNetworkIt.p.isDaughter) {
	                        ContourParticle vCand = m_AllCandidates.get(vNetworkIt.pIndex);
	                        if (vCand.referenceCount < 1 && vCand.candidateLabel != 0) {
	                            vLegalMove = false;
	                        }


//	                        typename ContourPointWithIndexType::ContourPointType::MotherIndexListType::iterator
//	                        vMotherIndicesIterator = vNetworkIt->m_ContourPoint.m_motherIndices.begin();
//	                        typename ContourPointWithIndexType::ContourPointType::MotherIndexListType::iterator
//	                        vMotherIndicesIteratorEnd = vNetworkIt->m_ContourPoint.m_motherIndices.end();
//	                        /// tentatively set to false until we find a mother not yet
//	                        /// in the accepted list.
//	                        vLegalMove = false;
//	                        for (; vMotherIndicesIterator != vMotherIndicesIteratorEnd;
//	                                ++vMotherIndicesIterator) {
	//
//	                            if (vSelectedCandidateIndices.end() ==
//	                                    vSelectedCandidateIndices.find(*vMotherIndicesIterator)) {
//	                                // there is a mother not yet in the candidate list.
//	                                vLegalMove = true;
//	                                break;
//	                            }
//	                        }
	                    }

	                    ///
	                    /// RULE 2: All daughters already accepted the label of this
	                    ///        mother have at least one another mother.
	                    /// AND
	                    /// RULE 3: Mothers are still valid mothers (to not introduce
	                    ///         holes in the FG region).
	                    ///
	                    if (vLegalMove && vNetworkIt.p.isMother) {
	                        /// Iterate the daughters and check their reference count

	                        boolean vRule3Fulfilled = false;

	                        for(Point vDaughterIndicesIterator: vNetworkIt.p.getDaughterList())
	                        {

	                            ContourParticle vDaughterPoint = m_AllCandidates.get(vDaughterIndicesIterator);

	                                //                                if (vAllCandidates.find(*vDaughterIndicesIterator) == vAllCandidates.end())
	                                //                                    std::cout << "daughter index found not in the list(2): " << *vDaughterIndicesIterator << std::endl;

	                            ///
	                            /// rule 2:
//	                            typename ContourIndexSetType::iterator 
//	                            vAcceptedDaugtherIt = vSelectedCandidateIndices.find(*vDaughterIndicesIterator);
	                            boolean vAcceptedDaugtherItContained = vSelectedCandidateIndices.contains(vDaughterIndicesIterator);
	                            //TODO HashSet problem (maybe need hashmap)
//	                            Point vAcceptedDaugtherIt = null;

	                            if(vAcceptedDaugtherItContained) 
	                            {
	                                /// This daughter has been accepted and needs
	                                /// a reference count > 1, else the move is
	                                /// invalid.
	                                if (vDaughterPoint.candidateLabel == vNetworkIt.p.label && vDaughterPoint.referenceCount <= 1) 
	                                {
	                                    vLegalMove = false;
	                                    break;
	                                }
	                            }

	                            ///
	                            /// rule 3:
	                            if (!vRule3Fulfilled) 
	                            {
	                                if(!vAcceptedDaugtherItContained) {
	                                    /// There is a daughter that has not yet been accepted.
	                                    vRule3Fulfilled = true;
	                                } else {
	                                	
	                                    /// the daughter has been accepted, but may
	                                    /// have another candidate label(rule 3b):
	                                	
//	                                    if(m_AllCandidates.get(vAcceptedDaugtherIt).candidateLabel !=
//	                                            vNetworkIt.ContourPoint.label) {
//	                                        vRule3Fulfilled = true;
//	                                    }
	                                	//TODO compare to itk above. is it the same?
	                                	//TODO HashSet problem (maybe need hashmap)
	                                    if(m_AllCandidates.get(vDaughterIndicesIterator).candidateLabel != vNetworkIt.p.label) 
	                                    {
	                                    	vRule3Fulfilled = true;
	                                    }
	                                }
	                            }
	                        }

	                        if (!vRule3Fulfilled) vLegalMove = false;
	                    }

	                    if (vLegalMove) {
	                        /// the move is legal, store the index in the container
	                        /// with accepted candidates of this network.
	                        vSelectedCandidateIndices.add(vNetworkIt.pIndex);

	                        /// also store the candidate in the global candidate
	                        /// index container:
	                        vLegalIndices.add(vNetworkIt.pIndex);

	                        /// decrease the references of its daughters(with the same 'old' label).
//	                        typename ContourPointWithIndexType::ContourPointType::MotherIndexListType::iterator
//	                        vDaughterIndicesIterator = vNetworkIt->m_ContourPoint.m_daughterIndices.begin();
//	                        typename ContourPointWithIndexType::ContourPointType::MotherIndexListType::iterator
//	                        vDaughterIndicesIteratorEnd = vNetworkIt->m_ContourPoint.m_daughterIndices.end();
	                        for (Point vDaughterIndicesIterator : vNetworkIt.p.getDaughterList()) {

	                            //                            if (vAllCandidates.find(*vDaughterIndicesIterator) == vAllCandidates.end())
	                            //                                    std::cout << "daughter index found not in the list(3): " << *vDaughterIndicesIterator << std::endl;
	                            ContourParticle vDaughterPoint = m_AllCandidates.get(vDaughterIndicesIterator);
	                            if (vDaughterPoint.candidateLabel ==
	                                    vNetworkIt.p.label) {
	                                vDaughterPoint.referenceCount--;
	                            }
	                        }
	                    } else {
	                        vIllegalIndices.add(vNetworkIt.pIndex);
	                    }
	                }
	            }
	        }

	        //        std::stringstream vSS3;
	        //        vSS3 << "contourPointsAfterNetworkingStuffAtIt_" << (m_iteration_counter + 10000) << ".txt";
	        //        WriteContourPointContainer(vSS3.str().c_str(), vAllCandidates);

	        /**
	         * Filter all candidates with the illigal indices
	         */
	        
	        for (Point vIlligalIndicesIt : vIllegalIndices) {
	            m_AllCandidates.remove(vIlligalIndicesIt);
	        }
	        //
	        //        std::stringstream vSS4;
	        //        vSS4 << "contourPointsAfterErasingIllegals" << (m_iteration_counter + 10000) << ".txt";
	        //        WriteContourPointContainer(vSS4.str().c_str(), vAllCandidates);

	        /**
	         * Filter candidates according to their energy
	         */

	        
	        Iterator<Entry<Point, ContourParticle>> it = m_AllCandidates.entrySet().iterator();

	        //!!! TODO is iterator working vs remove?
	        while (it.hasNext()) {
	        	Entry<Point, ContourParticle> vStoreIt = it.next(); // iterator to work with
//	            it.next(); // safely increment (before erasing anything in the container)
	            if (vStoreIt.getValue().energyDifference >= 0) {
	                m_AllCandidates.remove(vStoreIt);
	            }
	        }
	        //        WriteContourPointContainer("contourPointsAfterEnergyCheck.txt", vAllCandidates);

	        /**
	         * Detect oscillations and store values in history.
	         */
	        
	        double vSum = SumAllEnergies(m_AllCandidates);
	        debug("sum of energies: "+vSum);
	        for (int vI = 0; vI < m_OscillationHistoryLength; vI++) {
	            double vSumOld = m_OscillationsEnergyHist[vI];
//	            debug("check nb: " + vAllCandidates.size() + " against " + m_OscillationsNumberHist[0]);
	            debug("m_AllCandidates.size()="+m_AllCandidates.size()+
	            		"m_OscillationsNumberHist[vI]="+m_OscillationsNumberHist[vI]);
	            
	            if (m_AllCandidates.size() == m_OscillationsNumberHist[vI] &&
	                    Math.abs(vSum - vSumOld) <= 1e-5 * Math.abs(vSum)) {
	            	
//TODO wird noch nie aufgerufen
	            	
	                /// here we assume that we're oscillating, so we decrease the
	                /// acceptance factor:
	                m_AcceptedPointsFactor *= m_AcceptedPointsReductionFactor;
	                debug("nb of accepted points reduced to: "+m_AcceptedPointsFactor);
	            }
	        }
//	        std::cout << "nb cand: " << m_AllCandidates.size() << "\t energy: " << vSum << std::endl;
	        /// Shift the old elements:
	        //TODO sts maybe optimize by modulo list?
	        for ( int vI = 1; vI < m_OscillationHistoryLength; vI++) {
	            m_OscillationsEnergyHist[vI - 1] = m_OscillationsEnergyHist[vI];
	            m_OscillationsNumberHist[vI - 1] = m_OscillationsNumberHist[vI];
	        }

	        /// Fill the new elements:
	        m_OscillationsEnergyHist[m_OscillationHistoryLength - 1] = vSum;
	        m_OscillationsNumberHist[m_OscillationHistoryLength - 1] = m_AllCandidates.size();


	        /**
	         * Intermediate step: filter the candidates according to their rank
	         * and spacial position.
	         */

	        if(m_AcceptedPointsFactor < 0.99) {
	        	
	        	//TODO mein testbild verliert ein auge!
	            FilterCandidatesContainerUsingRanks(m_AllCandidates);
	        }

	        /**
	         * Move all the points that are simple. Non simple points remain in the
	         * candidates list.
	         */
	        
	        //TODO
//	        typedef typename TopologicalNumberCalculatorType::ForegroundTopologicalNumbersType FGTNType;
//	        typedef typename FGTNType::iterator FGTNIteratorType;
//	        FGTNType vFGTNvector;
	        boolean vChange = true;

//	        typedef std::pair<ContourIndexType, unsigned int> SeedType;
//	        typedef std::set<SeedType> SeedSetType;
//	        typedef typename SeedSetType::iterator SeedSetIteratorType;
	        Set<Pair<Point, Integer>> vSeeds = new HashSet<Pair<Point,Integer>>();
	        

	        /// We first move all the FG-simple points. This we do because it happens
	        /// that points that are not simple at the first place get simple after
	        /// the change of other points. The non-simple points will be treated
	        /// in a separate loop afterwards.
	        
//TODO
	        
//	        while (vChange && !m_AllCandidates.isEmpty()) {
//	        	vChange = false;
//	            Iterator<Entry<Point, ContourParticle>> vPointIterator = m_AllCandidates.entrySet().iterator();
//
//	            while (vPointIterator.hasNext()) {
////TODO !!! HERE ACTUAL ITERATOR PROBLEM
//	            	Entry<Point, ContourParticle> vStoreIt = vPointIterator.next();
//
//	                Point vCurrentIndex = vStoreIt.getKey();
//
//	                //TODO
////	                if (m_UseRegionCompetition) {
////	                    vFGTNvector =  (m_TopologicalNumberFunction->EvaluateAdjacentRegionsFGTNAtIndex(vCurrentIndex));
////	                    FGTNIteratorType vTopoNbItr;
////	                    FGTNIteratorType vTopoNbItrEnd = vFGTNvector.end();
//	                    boolean vSimple = true;
////	                    /// Check for FG-simplicity:
////	                    for (vTopoNbItr = vFGTNvector.begin();
////	                            vTopoNbItr != vTopoNbItrEnd; ++vTopoNbItr) {
////	                        if (vTopoNbItr->second.first != 1 || vTopoNbItr->second.second != 1) {
////	                            // This is a FG simple point; perform the move.
////	                            vSimple = false;
////	                        }
////	                    }
//	                    if (vSimple) {
//	                        vChange = true;
//	                        ChangeContourPointLabelToCandidateLabel(vStoreIt);
//	                        //TODO iterator remove problem
//	                        m_AllCandidates.remove(vStoreIt.getKey());
//	                        vConvergence = false;
//	                    }
////	                }
//	            }
//	        }
	        
	        /// Now we know that all the points in the list are 'currently' not simple.
	        /// We move them anyway (if no constraints about this topological event
	        /// is set) and record where to relabel using the seed set.
	        
	            /// Check for handles:
	            /// if the point was not disqualified already and we disallow
	            /// introducing handles (not only self fusion!), we check if
	            /// there is an introduction of a handle.
	        
	            /// Check for splits:
	            /// This we have to do either to forbid
	            /// the change in topology or to register the seed point for
	            /// relabelling.
	            /// if the point was not disqualified already and we disallow
	            /// splits, then we check if the 'old' label undergoes a split.
	        
	        
	        /// Now we filtered non valid points and collected the seeds for
	        /// relabeling. All moves remaining in the candidate list can now
	        /// be performed.
	        

	        //TODO iterator erase problem?
	        for(Entry<Point, ContourParticle> entry: m_AllCandidates.entrySet())
	        {
	            ChangeContourPointLabelToCandidateLabel(entry);
	            vConvergence = false;
	        }
	        
	        
	        /// Perform relabeling of the regions that did a split:

	        
	        /// Merge the the competing regions if they meet merging criterion.

	        
	        

	        return vConvergence;
			}

	private void FilterCandidatesContainerUsingRanks(HashMap<Point,ContourParticle> aContainer)
	{

        if (aContainer.size() > 0)
        {
            // Copy the candidates to a set (of ContourPointWithIndex). This
            // will sort them according to their energy gradients.
            List<ContourPointWithIndexType> vSortedList = new LinkedList<ContourPointWithIndexType>();

            for (Entry<Point, ContourParticle> vPointIterator : aContainer.entrySet()) {
                ContourPointWithIndexType vCand = new ContourPointWithIndexType(vPointIterator.getKey(), vPointIterator.getValue());
                vSortedList.add(vCand);
            }

            Collections.sort(vSortedList);

            //			EnergyDifferenceType vBestEnergy = vSortedList.front().m_ContourPoint.m_energyDifference;

            int vNbElements = vSortedList.size();
            vNbElements =(int)(vNbElements*m_AcceptedPointsFactor + 0.5);
//            if(vNbElements < 1) {
//                vNbElements = 1;
//            }

            /// Fill the container with the best candidate first, then
            /// the next best that does not intersect the tabu region of
            /// all inserted points before.
            aContainer.clear();
            for(ContourPointWithIndexType vSortedListIterator : vSortedList)
            {
            	if(! (vNbElements >= 1))
            	{
            		break;
            	}

                vNbElements--;
                Point vCandCIndex = vSortedListIterator.pIndex;
                Iterator<Entry<Point, ContourParticle>> vAcceptedCandIterator = aContainer.entrySet().iterator();
                boolean vValid = true;
                for (; vAcceptedCandIterator.hasNext();) {
                    Point vCIndex = vAcceptedCandIterator.next().getKey();
                    
                    //TODO nothing happens in here. itk::2074
                    
//                    float vDist = 0;
//                    for (unsigned int vD = 0; vD < m_Dim; vD++) {
//                        vDist += (vCIndex[vD] - vCandCIndex[vD]) * (vCIndex[vD] - vCandCIndex[vD]);
//                    }
                    //                    InternalImageSizeType vPSFsize = m_PSF->GetLargestPossibleRegion().GetSize();

                    //gong_test
//                    if (sqrt(vDist) < 0.001) {//< vPSFsize[vD] / 400) {
//                        /// This candidate didn't pass. Abort the test.
//                        vValid = false;
//                        break;
//                    }

                    /*
                    if(vNbElements > vConstNbElements * 0.9 && vNbElements < vConstNbElements * 0.5){
                            vValid = false;
                    }
                     */

                    /*
                    if(vSortedListIterator->m_ContourPoint.m_energyDifference > 0.2 * vBestEnergy || vSortedListIterator->m_ContourPoint.m_energyDifference < 0.7 * vBestEnergy)
                    {
                            vValid=false;
                    }
                     */
                }
                if (vValid) {
                    /// This candidate passed the test and is added to the TempRemoveCotainer:
                	//TODO iterator problem
                	aContainer.put(vSortedListIterator.pIndex, vSortedListIterator.p);
//                    (aContainer)[vSortedListIterator.pIndex] = vSortedListIterator.m_ContourPoint;
                }
            }
        }
//        std::cout << "container size: " << aContainer->size() << std::endl;
    }


	private double SumAllEnergies(HashMap<Point, ContourParticle> aContainer) 
	{

        double vTotalEnergyDiff = 0;
        
        for (ContourParticle vPointIterator : aContainer.values()) {
            vTotalEnergyDiff += vPointIterator.energyDifference;
        }
        return vTotalEnergyDiff;
    }
	
	public void GenerateData()
	{
		
        /**
         * Set up the regions and allocate the output-image
         */
//        CopyRegionsAndAllocateOutput();

        /**
         * Set up the containers and allocate the label image:
         */
//        InitializeLabelImageAndContourContainer(this->GetInitInput());


        /**
         * Set the inputs for the image functions:
         */
//        m_SimplicityCriterion->SetInputImage(m_LabelImage);
//        m_TopologicalNumberFunction->SetInputImage(m_LabelImage);
//        m_ContourLengthFunction->SetInputImage(m_LabelImage);

        /**
         * Initialize standard statistics (mean, variances, length, area etc)
         */
        renewStatistics();


        /**
         * Depending on the functional to use, prepare stuff for faster computation.
         */
//        PrepareEnergyCaluclation();


        /**
         * Start time measurement
         */
        
        Timer timer = new Timer();
        timer.tic();

        /**
         * Main loop of the algorithm
         */

        boolean vConvergence = false;
        

        while (m_MaxNbIterations > m_iteration_counter && !(vConvergence)) {
            m_iteration_counter++;

            //std::cout << "number of points: "<<m_InnerContourContainer.size() << std::endl;
            //m_ManyPointsToBeAdded.clear();
            //m_ManyPointsToBeDeleted.clear();
            vConvergence = DoOneIteration();
            
stack.addSlice("iteration "+m_iteration_counter, this.labelImage.getPixelsCopy());
            
        }
        m_converged = vConvergence;
        
        timer.toc();
        debug(timer.lastResult());

       
        

        /**
         * Write the label image in a convenient form to the filters output image
         */
//        WriteLabelImageToOutput();

        /**
         * Debugging Output
         */

        
        long executionTime = timer.lastResult();
        debug("time per iteration: " + executionTime/m_iteration_counter);

        if (m_converged) {
            debug("convergence after " + m_iteration_counter +" iterations.");
        } else {
            debug("no convergence !");
        }
    
	}
	
	
	boolean DoOneIteration()
	{

        boolean vConvergenceA;
        vConvergenceA = true;

        if (m_EnergyFunctional == EnergyFunctionalType.e_Deconvolution && m_iteration_counter % 1 == 0) {
//TODO sts
//            RenewDeconvolutionStatistics(m_LabelImage, this->GetDataInput());
        }

		if (m_UseShapePrior) {
            //RenewShapePrior();
            //data structure change
            /* new version, not ready
            //std::vector<OuterContourContainerKeyType>::iterator it;
            unsigned int vAddSize=m_PointsToBeAdded.size();
            unsigned int vDeleteSize=m_PointsToBeDeleted.size();
            vnl_matrix<double> vAddCoordinate(vAddSize,2);
            vnl_matrix<double> vDeleteCoordinate(vDeleteSize,2);
            for (unsigned int i=0;i<vAddSize;i++)
            {
                vAddCoordinate(i,0)=m_PointsToBeAdded[i][0];
                vAddCoordinate(i,1)=m_PointsToBeAdded[i][1];
            }
            for (unsigned int i=0;i<vDeleteSize;i++)
            {
                vDeleteCoordinate(i,0)=m_PointsToBeDeleted[i][0];
                vDeleteCoordinate(i,1)=m_PointsToBeDeleted[i][1];
            }

            m_Moments2D.AddManyPoints(m_Moments2D.GetCentralMoments(),&vAddCoordinate);
            m_Moments2D.DeleteManyPoints(m_Moments2D.GetCentralMoments(),&vDeleteCoordinate);
            
             */
        	
        	
            /*here the code is rescanning the lable image*/
/*//TODO sts
            int vCount = m_Count[1];
            vnl_matrix<double> vCoordinate;
            vCoordinate.set_size(vCount, 2);
            int vx, vy;
            LabelImageIndexType vCurrentIndex;
            typedef itk::ImageRegionConstIterator<LabelImageType> LabelImageConstIteraterType;
            LabelImageConstIteraterType vLabelIt(m_LabelImage, m_LabelImage->GetBufferedRegion());
            vCount = 0;
            clock_t vClocka, vClockb, vClockc;//, vClockd;
            vClocka = clock();
            for (vLabelIt.GoToBegin(); !vLabelIt.IsAtEnd(); ++vLabelIt) {
                LabelPixelType vLabelAtPositionOfTheIterator = abs(vLabelIt.Get());
                if (vLabelAtPositionOfTheIterator == 1) {
                    vCurrentIndex = vLabelIt.GetIndex();
                    vx = vCurrentIndex[0];
                    vy = vCurrentIndex[1];
                    vCoordinate(vCount, 0) = vx;
                    vCoordinate(vCount, 1) = vy;
                    vCount++;
                }
            }
            int vWidth = m_LabelImage->GetLargestPossibleRegion().GetSize()[0];
            int vHight = m_LabelImage->GetLargestPossibleRegion().GetSize()[1];
            //std::cout<<"**** current points number ***"<<vCount<<std::endl;
            m_Moments2D.SetInputData(&vCoordinate, vWidth, vHight);
            vClockb = clock();
            m_Moments2D.ComputeLegendreMoments();
            vClockc = clock();
            double gong_test = (*m_Moments2D.GetLegendreMoments()-*m_Moments2D.GetReference()).fro_norm();
            
 */
            
            //std::cout<<"**** current moments ***"<<std::endl<<*m_Moments2D.GetLegendreMoments()<<std::endl;
            /*
            FILE *pf= fopen("/Users/ygong/energy.txt","a");
            fprintf(pf,"%f scanning: %d compute: %d\n",gong_test, (int)(vClockb-vClocka),(int)(vClockc-vClockb));
            fclose(pf);
             */
            //std::cout<<" *** distance ***"<<gong_test<<std::endl;
            //std::cout<<"**** reference ***"<<std::endl<<*m_Moments2D.GetReference()<<std::endl;
            //*/
        }
        

		if (m_RemoveNonSignificantRegions) {
            RemoveSinglePointRegions();
            RemoveNotSignificantRegions();
        }

        vConvergenceA = IterateContourContainerAndAdd();
        CleanUp();

        return vConvergenceA;
        //return false;
    
	}
	
	
    private void CleanUp() 
    {

//    	UIntStatisticsContainerType::iterator vActiveLabelsIt =
//    		m_Count.begin();
//    	UIntStatisticsContainerType::iterator vActiveLabelsItEnd =
//    		m_Count.end();
    	for(Entry<Integer, LabelInformation> vActiveLabelsIt: labelMap.entrySet())
    	{
            if (vActiveLabelsIt.getValue().count == 0) {
                FreeLabelStatistics(vActiveLabelsIt.getKey());
            }
    	}
	}

    void FreeLabelStatistics(int aLabelAbs) 
    {
    	labelMap.remove(aLabelAbs);
    	
//        m_Means.erase(aLabelAbs);
//        m_Variances.erase(aLabelAbs);
//        m_Count.erase(aLabelAbs);
//        m_Lengths.erase(aLabelAbs);
//        m_Intensities.erase(aLabelAbs);
    	///m_BBoxes.erase(aLabelAbs);
    }
    
    
    
	void RemoveSinglePointRegions() {

        for(Entry<Point, ContourParticle> vIt: m_InnerContourContainer.entrySet())
        {
            ContourParticle vWorkingIt = vIt.getValue();
//            if (m_Count[vWorkingIt->second.m_label] == 1) {
            if(labelMap.get(vWorkingIt.label).count == 1)
            {
                vWorkingIt.candidateLabel = bgLabel;
                //TODO changed from
                //ChangeContourPointLabelToCandidateLabel(vWorkingIt);
                //to this:
                
                //TODO!!!! vIt could be removed from container in ChangeContourPointLabelToCandidateLabel
                ChangeContourPointLabelToCandidateLabel(vIt);
            }
        }
        CleanUp();
    }
	
    void ChangeContourPointLabelToCandidateLabel(Entry<Point, ContourParticle> aParticle) 
    {
    	ContourParticle second = aParticle.getValue();
    		
        Point vCurrentIndex = aParticle.getKey();
        int vFromLabel = second.label;
        int vToLabel = second.candidateLabel;

        ///
        /// The particle was modified,reset the counter in order to process
        /// the particle in the next iteration.
        ///
//TODO modifiedCounter
//        aParticle->second.m_modifiedCounter = 0;

//        if (vCurrentIndex[0] == 73 && vCurrentIndex[1] == 44) {
//            std::cout << "stop! aIndex: " << vCurrentIndex << " ;label = " << aParticle->second << std::endl;
//        }

        ///
        /// Update the label image. The new point is either a contour point or 0,
        /// therefor the negative label value is set.
        ///
    	set(vCurrentIndex, getNegLabel(vToLabel));

        ///
        /// STATISTICS UPDATE
        /// Update the statistics of the propagating and the loser region.
        ///
        UpdateStatisticsWhenJump(aParticle, vFromLabel, vToLabel);
        
		if (m_EnergyFunctional == EnergyFunctionalType.e_Deconvolution) {
			//TODO
//            UpdateConvolvedImage(vCurrentIndex, vFromLabel, vToLabel);
        }

        /// TODO: A bit a dirty hack: we store the old label for the relabeling
        ///       procedure later on...either introduce a new variable or rename the
        ///       variable (which doesn't work currently :-).
        second.candidateLabel = vFromLabel;

        ///
        /// Clean up
        ///

        /// The loser region (if it is not the BG region) has to add the
        /// neighbors of the lost point to the contour list.
        if (vFromLabel != bgLabel) {
            AddNeighborsAtRemove(vFromLabel, vCurrentIndex);
        }

        ///
        /// Erase the point from the surface container in case it now belongs to
        /// the background. Else, add the point to the container (or replace it
        /// in case it has been there already).
        ///
        if (vToLabel == 0) {
            m_InnerContourContainer.remove(vCurrentIndex);
        } else {
            ContourParticle vContourPoint = second;
            vContourPoint.label = vToLabel;
            /// The point may or may not exist already in the m_InnerContainer.
            /// The old value, if it exist, is just overwritten with the new
            /// contour point (with a new label).
            
            m_InnerContourContainer.put(vCurrentIndex, vContourPoint);
        }

        /// Remove 'enclosed' contour points from the container. For the BG this
        /// makes no sense.
        if (vToLabel != 0) {
            MaintainNeighborsAtAdd(vToLabel, vCurrentIndex);
        }
        }

	
	
    private void UpdateStatisticsWhenJump(
			Entry<Point, ContourParticle> aParticle, int aFromLabelIdx,
		    int aToLabelIdx) 
    {
        Point vCurrentIndex = aParticle.getKey();
        ContourParticle vOCCV = aParticle.getValue();
        int vCurrentImageValue = vOCCV.intensity;

        LabelInformation aToLabel = labelMap.get(aToLabelIdx);
        LabelInformation aFromLabel = labelMap.get(aFromLabelIdx);
        
        double vNTo = aToLabel.count;
        double vNFrom = aFromLabel.count;

        /// Before changing the mean, compute the sum of squares of the samples:
        double vToLabelSumOfSq = aToLabel.var * (vNTo - 1.0) +
                vNTo * aToLabel.mean * aToLabel.mean;
        double vFromLabelSumOfSq = aFromLabel.var * (aFromLabel.count - 1.0) +
                vNFrom * aFromLabel.mean * aFromLabel.mean;

        /// Calculate the new means for the background and the label:
        double vNewMeanToLabel = (aToLabel.mean * vNTo + vCurrentImageValue) / (vNTo + 1.0);
        double vNewMeanFromLabel = (vNFrom * aFromLabel.mean - vCurrentImageValue) / (vNFrom - 1.0);

        /// Calculate the new variances:
        aToLabel.var = ((1.0 / (vNTo)) * (
                vToLabelSumOfSq + vCurrentImageValue * vCurrentImageValue
                - 2.0 * vNewMeanToLabel * (aToLabel.mean * vNTo + vCurrentImageValue)
                + (vNTo + 1.0) * vNewMeanToLabel * vNewMeanToLabel));

        aFromLabel.var = ((1.0 / (vNFrom - 2.0)) * (
                vFromLabelSumOfSq - vCurrentImageValue * vCurrentImageValue
                - 2.0 * vNewMeanFromLabel * (aFromLabel.mean * vNFrom - vCurrentImageValue)
                + (vNFrom - 1.0) * vNewMeanFromLabel * vNewMeanFromLabel));

        /// Update the means:
        aToLabel.mean = vNewMeanToLabel;
        aFromLabel.mean = vNewMeanFromLabel;
        //        m_Means[0] = 0;
        //        m_Means[1] = 1.0403;
        //        m_Means[2] = 1.0403;

        /// Add a sample point to the BG and remove it from the label-region:
        aToLabel.count++;
        aFromLabel.count--;

        /// Update the lengths for each region:
        /// ContourLengthFunction gives back the overall length change when changing
        /// aFromLabel to aToLabel. This change applies to both regions.

//TODO 
        /*
        
        double vLC = m_ContourLengthFunction->EvaluateLengthChange(
                aParticle->first, aFromLabel, aToLabel);
        m_Lengths[aFromLabel] += vLC;
        m_Lengths[aToLabel] += vLC;

        //update the moments map
        //Gong_test
        if (m_UseShapePrior) {
            if (aFromLabel == 1) {
                m_PointsToBeDeleted.push_back(vCurrentIndex);
            } else {
                m_PointsToBeAdded.push_back(vCurrentIndex);
            }
        }
        
        */
    }

	void RemoveNotSignificantRegions()
    {
        // Iterate through the active labels and check for significance.
        for(Entry<Integer, LabelInformation> vActiveLabelsIt : labelMap.entrySet())
        {
            int vLabelAbs = vActiveLabelsIt.getKey();
            if (vLabelAbs == bgLabel) {
                continue;
            }

			if (vActiveLabelsIt.getValue().count <= m_AreaThreshold) {// &&
//                if (!(m_Means[vLabelAbs] < m_Means[0] - 3 * sqrt(m_Variances[0]) ||
//                        m_Means[0] + 3 * sqrt(m_Variances[0]) < m_Means[vLabelAbs])) {
                    RemoveFGRegion(vActiveLabelsIt.getKey());
//                    vNRemovedRegions++;
//                    std::cout << "removed label : " << vActiveLabelsIt->first
//                            << ", count = " << vActiveLabelsIt->second << std::endl;
//                }
            }

        }
//        std::cout << "number of removed labels = " << vNRemovedRegions << std::endl;
        CleanUp();
        }
	
	
	
	void RemoveFGRegion(int aLabel) 
	{
		
		//TODO iterator/remove problem

        /// Get the contour points of that label and copy them to vContainer:
        HashMap<Point, ContourParticle> vContainer = new HashMap<Point, ContourParticle>();

//        CellListedHashMapIteratorType vIt = m_InnerContourContainer.begin();
//        CellListedHashMapIteratorType vEnd = m_InnerContourContainer.end();

        // find all the element with this label and store them in a tentative
        // container:
        for (Entry<Point, ContourParticle> vIt: m_InnerContourContainer.entrySet()) {
            if (vIt.getValue().label == aLabel) {
            	vContainer.put(vIt.getKey(), vIt.getValue());
            }
        }

        /// Successively remove the points from the contour. In the end of the
        /// loop, new points are added to vContainer.
        while(!vContainer.isEmpty()) {
//            std::cout << "container size in remove fg region: " << vContainer.size() << std::endl;
            Iterator<Entry<Point, ContourParticle>> vRemIt = vContainer.entrySet().iterator();
//            HashMapIteratorType vRemEnd = vContainer.end();
            while(vRemIt.hasNext()) {
//                HashMapIteratorType vTempIt = vRemIt;
//                ++vRemIt;
////                std::cout << "(should not happen) point to remove in removeFGregion: \n" << vTempIt->first << "\n" << vTempIt->second << std::endl;
//                vTempIt->second.m_candidateLabel = bgLabel;
//                ChangeContourPointLabelToCandidateLabel(vTempIt);
//                vContainer.erase(vTempIt);
            	
            	//TODO! does it really remove from first to last?
            	Entry<Point, ContourParticle> vTempIt = vRemIt.next();
            	vTempIt.getValue().candidateLabel=bgLabel;
            	ChangeContourPointLabelToCandidateLabel(vTempIt);
            	vRemIt.remove();
            	
            	
            }

            //TODO ??? why should i do that?
            //Refill the container
            if (labelMap.get(aLabel).count > 0) {
            	debug("refilling in remove fg region! count: "+labelMap.get(aLabel).count);
//                vIt = m_InnerContourContainer.entrySet();
//                vEnd = m_InnerContourContainer.end();
                for (Entry<Point, ContourParticle> vIt: m_InnerContourContainer.entrySet()) {
                    if (vIt.getValue().label == aLabel) {
                    	vContainer.put(vIt.getKey(), vIt.getValue());
                    }
                }
            }
        }
    	}
	
	static void debug(Object s)
	{
		System.out.println(s);
	}
	
}

enum EnergyFunctionalType {
	e_CV, e_MS, e_LocalCV, e_LocalLi, e_Deconvolution, e_PSwithCurvatureFlow
}











