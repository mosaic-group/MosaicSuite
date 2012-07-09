package mosaic.region_competition;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import mosaic.plugins.Region_Competition;

import ij.measure.ResultsTable;

/*
//TODO TODOs
- refactor LabelImage, extract what's not supposed to be there (eg energy calc)
- does merging criterion have to be testet multiple times?
*/

public class Algorithm
{
	private Region_Competition MVC; 	/** interface to image program */
	private LabelImage labelImage; 	// == this, exists for easier refactoring
	private float[] dataIntensity;
//	public int[] dataLabel;
	
	
//	ImagePlus imageIP;	// input image
//	ImageProcessor imageProc;
	
//	private final float imageMax; 	// maximal intensity of input image
	
//	ImagePlus labelPlus;				
//	ImageProcessor labelIP;				// map positions -> labels
//	public short[] dataLabelShort;
	
	
//	int size;
//	int dim;			// number of dimension
//	int[] dimensions;	// dimensions (width, height, depth, ...)
//	int width;	// TODO multidim
//	int height;
	
	private IndexIterator iterator; // iterates over the labelImage
	
	private final int bgLabel;
	private final int forbiddenLabel;
	
	private LabelDispenser labelDispenser;
	
	
	// data structures
	
	/** stores the contour particles. access via coordinates */
	private HashMap<Point, ContourParticle> m_InnerContourContainer;
	
	private HashMap<Point, ContourParticle> m_Candidates;
	
	/** Maps the label(-number) to the information of a label */
	private HashMap<Integer, LabelInformation> labelMap;

	private HashMap<Point, LabelPair> m_CompetingRegionsMap;
	
	
	private Connectivity connFG;
	private Connectivity connBG;
	private TopologicalNumberImageFunction m_TopologicalNumberFunction;

	private List<Integer> m_MergingHist;
	private List<Integer> m_FramesHist;
	
	private Set<Pair<Point, Integer>> m_Seeds = new HashSet<Pair<Point, Integer>>();
	
	/**
	 * creates a new LabelImage with size of ip
	 * @param proc is saved as originalIP
	 */
	public Algorithm(Region_Competition region_competition) 
	{

		MVC = region_competition;
		labelImage = MVC.getLabelImage();
//		dataLabel = labelImage.dataLabel;
		settings = MVC.settings;
		
		bgLabel = labelImage.bgLabel;
		forbiddenLabel = labelImage.forbiddenLabel;
		
		dataIntensity = labelImage.dataIntensity;
//		initIntensityData();
		
		
		iterator = labelImage.iterator;
		
		connFG = labelImage.getConnFG();
		connBG = labelImage.getConnBG();
		
		initMembers();
		labelImage.initBoundary();
		initContour();
	}

	
	//////////////////////////////////////////////////


	public Settings settings;
	
    boolean m_converged;
    float m_AcceptedPointsFactor;
    int m_iteration_counter; // member for debugging
    
    int m_OscillationsNumberHist[];
    double m_OscillationsEnergyHist[];
    
    EnergyFunctionalType m_EnergyFunctional;
    int m_OscillationHistoryLength;

	private int m_MaxNLabels;
    
	SphereBitmapImageSource sphereMaskIterator; // regularization
	
	SphereBitmapImageSource m_SphereMaskForLocalEnergy;
	int m_GaussPSEnergyRadius;
    
	// ///////////////////////////////////////////////////


	public void initMembers()
    {
//		m_MergingHist = new LinkedList<Integer>();
//		m_FramesHist = new LinkedList<Integer>();
		
		//TODO initial capacities
		m_InnerContourContainer = new HashMap<Point, ContourParticle>();
		m_Candidates = new HashMap<Point, ContourParticle>();
		labelMap = new HashMap<Integer, LabelInformation>();
		m_CompetingRegionsMap = new HashMap<Point, LabelPair>();
    	
		m_TopologicalNumberFunction = new TopologicalNumberImageFunction(labelImage, connFG, connBG);
    	sphereMaskIterator = new SphereBitmapImageSource(labelImage, (int)settings.m_CurvatureMaskRadius, 2*(int)settings.m_CurvatureMaskRadius+1);
    	m_GaussPSEnergyRadius=8;
    	m_SphereMaskForLocalEnergy = new SphereBitmapImageSource(labelImage, m_GaussPSEnergyRadius, 2*m_GaussPSEnergyRadius+1);

    	
    	//TODO half dummy
        m_EnergyFunctional = settings.m_EnergyFunctional;
        m_OscillationHistoryLength = settings.m_OscillationHistoryLength;
    	//END half dummy
    	
    	m_iteration_counter = 0;
    	m_converged = false;
    	m_AcceptedPointsFactor = settings.m_AcceptedPointsFactor;
    	
        m_OscillationsNumberHist = new int[m_OscillationHistoryLength];
        m_OscillationsEnergyHist = new double[m_OscillationHistoryLength];
    	
		for(int vI = 0; vI < m_OscillationHistoryLength; vI++) {
			m_OscillationsNumberHist[vI] = 0;
			m_OscillationsEnergyHist[vI] = 0;
		}
		
		labelDispenser = new LabelDispenser(10000);
		m_MaxNLabels=0;
    }
	
	
	

	/**
	 * marks the contour of each region (sets on labelImage)
	 * stores the contour particles in contourContainer
	 */
	public void initContour()
	{
		Connectivity conn = connFG;
		
		for(int i: iterator.getIndexIterable())
		{
			int label=labelImage.getLabelAbs(i);
			if(label!=bgLabel && label!=forbiddenLabel) // region pixel
				// && label<negOfs
			{
				Point p = iterator.indexToPoint(i);
				for(Point neighbor : conn.iterateNeighbors(p))
				{
					int neighborLabel=labelImage.getLabelAbs(neighbor);
					if(neighborLabel!=label)
					{
						ContourParticle particle = new ContourParticle();
						particle.label=label;
						particle.intensity=getIntensity(i);
						m_InnerContourContainer.put(p, particle);
						
						break;
					}
				}
				
			} // if region pixel
		}
		
		// set contour to -label
		for(Entry<Point, ContourParticle> entry:m_InnerContourContainer.entrySet())
		{
			Point key = entry.getKey();
			ContourParticle value = entry.getValue();
			//TODO cannot set neg values to ShortProcessor
			labelImage.setLabel(key, labelImage.labelToNeg(value.label));
		}
	}
	
	
	
	
	void clearStats()
	{
		//clear stats
		for(LabelInformation stat: labelMap.values())
		{
			stat.reset();
		}
		
		m_MaxNLabels = 0;
	}
	
	
	/**
	 * as computeStatistics, does not use iterative approach
	 * (first computes sum of values and sum of values^2)
	 */
	public void renewStatistics()
	{
		clearStats();
		
		HashSet<Integer> usedLabels = new HashSet<Integer>();
		
		int size = iterator.getSize();
		for(int i=0; i<size; i++)
		{
//			int label = get(x, y);
//			int absLabel = labelToAbs(label);
			
			int absLabel= labelImage.getLabelAbs(i);

			if (absLabel != forbiddenLabel /* && absLabel != bgLabel*/) 
			{
				usedLabels.add(absLabel);
				if(absLabel > m_MaxNLabels)
				{
					m_MaxNLabels = absLabel;
				}
				
				LabelInformation stats = labelMap.get(absLabel);
				if(stats==null)
				{
					stats = new LabelInformation(absLabel);
					labelMap.put(absLabel, stats);
				}
				double val = getIntensity(i);
				stats.count++;
				
				stats.mean+=val; // only sum up, mean and var are computed below
				stats.setVar(stats.var+val*val);
			}
		}

		
		// now we have in all LabelInformation: 
		// in mean the sum of the values, in var the sum of val^2
		for(LabelInformation stat: labelMap.values())
		{
			int n = stat.count;
            if (n > 1) {
            	double var = (stat.var-stat.mean*stat.mean/n)/(n-1);
            	stat.setVar(var);
//                stat.var = (stat.var - stat.mean*stat.mean / n) / (n-1);
            } else {
                stat.var = 0;
            }
            stat.mean = stat.mean/n;
		}
		
        m_MaxNLabels++; // this number points to the a free label.
        
        labelDispenser.setLabelsInUse(usedLabels);
	}
	
	
	public void showStatistics()
	{
		ResultsTable rt = new ResultsTable();
		
		// over all labels
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
	
	
	public void GenerateData()
	{
		/**
		 * Set up the regions and allocate the output-image
		 */
		
		// CopyRegionsAndAllocateOutput();

		/**
		 * Set up the containers and allocate the label image:
		 */
		
		// InitializeLabelImageAndContourContainer(this->GetInitInput());

		/**
		 * Set the inputs for the image functions:
		 */
		// m_SimplicityCriterion->SetInputImage(m_LabelImage);
		// m_TopologicalNumberFunction->SetInputImage(m_LabelImage);
		// m_ContourLengthFunction->SetInputImage(m_LabelImage);

		/**
		 * Initialize standard statistics (mean, variances, length, area etc)
		 */
		
		renewStatistics();

		/**
		 * Depending on the functional to use, prepare stuff for faster computation.
		 */
		// PrepareEnergyCaluclation();

		/**
		 * Start time measurement
		 */

		Timer timer = new Timer();
		timer.tic();

		/**
		 * Main loop of the algorithm
		 */

		boolean vConvergence = false;

		while(settings.m_MaxNbIterations > m_iteration_counter && !(vConvergence)) 
		{
			synchronized(pauseMonitor)
			{
				if(pause) {
					try {
						debug("enter pause");
						pauseMonitor.wait();
						debug("exit pause");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if(abort)
				{
					break;
				}
			}
			
			
			m_iteration_counter++;
			debug("=== iteration " + m_iteration_counter+" ===");

			// std::cout << "number of points: "<<m_InnerContourContainer.size() << std::endl;
			// m_ManyPointsToBeAdded.clear();
			// m_ManyPointsToBeDeleted.clear();
			vConvergence = DoOneIteration();

			labelImage.displaySlice("iteration " + m_iteration_counter);
			MVC.updateProgress(m_iteration_counter, settings.m_MaxNbIterations);
//			MVC.addSliceToStackAndShow("iteration " + m_iteration_counter,
//					this.labelIP.getPixelsCopy());

		}
		
		labelImage.displaySlice("final image iteration " + m_iteration_counter);
		
		m_converged = vConvergence;

		timer.toc();
		debug("Total time: "+timer.lastResult());

		/**
		 * Write the label image in a convenient form to the filters output image
		 */
		// WriteLabelImageToOutput();

		/**
		 * Debugging Output
		 */

		long executionTime = timer.lastResult();
		debug("time per iteration: " + executionTime / m_iteration_counter);

		if(m_converged) {
			debug("convergence after " + m_iteration_counter + " iterations.");
		} else {
			debug("no convergence !");
		}

	}


	boolean DoOneIteration()
	{
	
		boolean vConvergenceA;
		vConvergenceA = true;

		if(m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC && m_iteration_counter % 1 == 0)
		{
			// TODO sts
			// RenewDeconvolutionStatistics(m_LabelImage, this->GetDataInput());
		}

		if(settings.m_UseShapePrior)
		{
			// TODO sts
		}

		if(settings.m_RemoveNonSignificantRegions) 
		{
			RemoveSinglePointRegions();
			RemoveNotSignificantRegions();
		}

		labelDispenser.addTempFree();
		
		vConvergenceA = IterateContourContainerAndAdd();
		CleanUp();
		
//		debug("labelmap: "+labelMap.size());
//		debug("dispenser: "+labelDispenser.labels.size());

		return vConvergenceA;
	}
	
 
	/**
	 * comments with // on the left border are itk-code-fragments to check correctness of java translation
	 * comments with indentation and/or triple-/// is commented code from itk
	 */
	boolean IterateContourContainerAndAdd()
	{
		/// Convergence is set to false if a point moved:
		boolean vConvergence = true;
		
		m_Candidates.clear();
		m_Seeds.clear();


		/// clear the competing regions map, it will be refilled in RebuildCandidateList: (via CalculateEnergyDifferenceForLabel)
		m_CompetingRegionsMap.clear();
		
		RebuildCandidateList(m_Candidates);
		FilterCandidates(m_Candidates);
		DetectOscillations(m_Candidates); 


		/**
		 * Intermediate step: filter the candidates according to their rank
		 * and spatial position.
		 */
        if(m_AcceptedPointsFactor < 0.99) 
        {
            FilterCandidatesContainerUsingRanks(m_Candidates);
        }

        /**
         * Move all the points that are simple. Non simple points remain in the
         * candidates list.
         */
		        
	
        /// We first move all the FG-simple points. This we do because it happens
        /// that points that are not simple at the first place get simple after
        /// the change of other points. The non-simple points will be treated
        /// in a separate loop afterwards.
		
        boolean vChange = true;
		List<Pair<Integer, Pair<Integer, Integer>>> vFGTNvector;
		        
		while(vChange && !m_Candidates.isEmpty()) 
		{
			vChange = false;
			
			Iterator<Entry<Point, ContourParticle>> vPointIterator = m_Candidates.entrySet().iterator();
			while(vPointIterator.hasNext()) 
			{
				Entry<Point, ContourParticle> vStoreIt = vPointIterator.next();
				Point vCurrentIndex = vStoreIt.getKey();

				vFGTNvector = m_TopologicalNumberFunction.EvaluateAdjacentRegionsFGTNAtIndex(vCurrentIndex);
				boolean vSimple = true;
				// / Check for FG-simplicity:
				for(Pair<Integer, Pair<Integer, Integer>> vTopoNbItr : vFGTNvector) 
				{
					if(vTopoNbItr.second.first != 1 || vTopoNbItr.second.second != 1) {
						// This is a FG simple point; perform the move.
						vSimple = false;
//						debug("0");
					}
				}
				if(vSimple) 
				{
					vChange = true;
					ChangeContourPointLabelToCandidateLabel(vStoreIt);
					vPointIterator.remove();
					vConvergence = false;
//					debug("1");
				}
				/// we will reuse the processed flag to indicate if a particle is a seed
				vStoreIt.getValue().m_processed = false;
			}
		}
		        
        /// Now we know that all the points in the list are 'currently' not simple.
        /// We move them anyway (if topological constraints allow) but record
        /// (for every particle) where to relabel (using the seed set). Placing
        /// the seed is necessary for every particle to ensure relabeling even
        /// if a bunch of neighboring particles change. The seed will be ignored
        /// later on if the corresponding FG region is not present in the
        /// neighborhood anymore. 
        /// TODO: The following code is dependent on the iteration order if splits/handles
        /// are not allowed. A solution would be to sort the candidates beforehand.
        /// This should be computationally not too expensive since we assume there
        /// are not many non-simple points.
		        
		
		Iterator<Entry<Point, ContourParticle>> vPointIterator = m_Candidates.entrySet().iterator();
		

			while(vPointIterator.hasNext())
			{
				Entry<Point, ContourParticle> e = vPointIterator.next();
				ContourParticle vStoreIt = e.getValue();
				Point vCurrentIndex = e.getKey();
	
				int vCurrentLabel = vStoreIt.label;
				int vCandidateLabel = vStoreIt.candidateLabel;
	
				boolean vValidPoint = true;
	
				vFGTNvector = (m_TopologicalNumberFunction.EvaluateAdjacentRegionsFGTNAtIndex(vCurrentIndex));

                /// Check for handles:
                /// if the point was not disqualified already and we disallow
                /// introducing handles (not only self fusion!), we check if
                /// there is an introduction of a handle.
				
                if (vValidPoint && !settings.m_AllowHandles) 
                {
                    for (Pair<Integer, Pair<Integer, Integer>> vTopoNbItr : vFGTNvector ) 
                    {
                        if (vTopoNbItr.first == vCandidateLabel) {
                            if (vTopoNbItr.second.first > 1) {
                                vValidPoint = false;
                                //break;
                            }
                        }

                        /// criterion to detect surface points (only 3D?)
                        if (vTopoNbItr.second.first == 1 && vTopoNbItr.second.second > 1) {
                            vValidPoint = false;
                            //break;
                        }
                    }
                }

                /// Check for splits:
                /// This we have to do either to forbid
                /// the change in topology or to register the seed point for
                /// relabeling.
                /// if the point was not disqualified already and we disallow
                /// splits, then we check if the 'old' label undergoes a split.
				if(vValidPoint) 
				{
					// - "allow introducing holes": T_FG(x, L = l') > 1
					// - "allow splits": T_FG > 2 && T_BG == 1
					boolean vSplit = false;
					for(Pair<Integer, Pair<Integer, Integer>> vTopoNbItr : vFGTNvector) 
					{
						if(vTopoNbItr.first == vCurrentLabel) {
							if(vTopoNbItr.second.first > 1) {
								vSplit = true;
							}
						}
					}
					if(vSplit) 
					{
						if(settings.m_AllowFission)
						{
							RegisterSeedsAfterSplit(this, vCurrentIndex, vCurrentLabel, m_Candidates);
						} else {
							/// disallow the move.	
							vValidPoint = false;
						}
					}
				}
				
				if(!vValidPoint) {
					// m_Candidates.erase(vStoreIt);
					vPointIterator.remove();
				}
				
                /// If the move doesn't change topology or is allowed (and registered
                /// as seed) to change the topology, perform the move (in the
                /// second iteration; in the first iteration seed points need to
                /// be collected):

				if (vValidPoint) {
					ChangeContourPointLabelToCandidateLabel(e);
					vConvergence = false;
					
					if(e.getValue().m_processed) 
					{
						RegisterSeedsAfterSplit(this, vCurrentIndex, vCurrentLabel, m_Candidates);
						boolean wasContained = m_Seeds.remove(new Pair<Point,Integer>(vCurrentIndex, vCurrentLabel));
						if(!wasContained)
						{
							throw new RuntimeException("no seed in set");
						}
					}
				}

				/// safely remove the last element BEFORE the iterator
				//                    m_Candidates.erase(vStoreIt);
				vPointIterator.remove();                

			} // while(vPointIterator.hasNext())

		        
        /// Now we filtered non valid points and collected the seeds for
        /// relabeling. All moves remaining in the candidate list can now
        /// be performed.
		        
//	displaySlice("pre ChangeContourPointLabelToCandidateLabel");
//		
//		for(Entry<Point, ContourParticle> entry : m_Candidates.entrySet()) 
//		{
//			ChangeContourPointLabelToCandidateLabel(entry);
//			vConvergence = false;
//		}
		        
//		displaySlice("post ChangeContourPointLabelToCandidateLabel");
		
        /// Perform relabeling of the regions that did a split:
		boolean vSplit = false;
		boolean vMerge = false;
		        
		
		//debug seeds
//		if(vSeeds.size()>0)
//		{
//			//TODO seed output == debugging
//			debug("Seed points:");
//			for(Pair<Point, Integer> vSeedIt : vSeeds) 
//			{
//				debug(" "+vSeedIt.first+" label="+vSeedIt.second);
//			}
//			debug("End Seed points:");
//		}
		
		
		int vNSplits = 0;
		for(Pair<Point, Integer> vSeedIt : m_Seeds) 
		{
			RelabelRegionsAfterSplit(labelImage, vSeedIt.first, vSeedIt.second);
			vSplit = true;
			vNSplits++;
		}
		        
        /// Merge the the competing regions if they meet merging criterion.
		if(settings.m_AllowFusion) 
		{
			
			Set<Integer> vCheckedLabels = new HashSet<Integer>();
			
			for(Entry<Point, LabelPair> vCRit : m_CompetingRegionsMap.entrySet()) 
			{
//				Entry<Point, Pair<Integer, Integer>> vCRit=competingRegions[i];
				
				Point idx = vCRit.getKey();
				LabelPair labels = vCRit.getValue();
				
				int vLabel1 = labels.first;
				int vLabel2 = labels.second;
                
//				m_MergingHist.add(m_iteration_counter);
//				m_MergingHist.add(vLabel1);
//				m_MergingHist.add(vLabel2);
				
				if(vCheckedLabels.contains(vLabel1) || vCheckedLabels.contains(vLabel2))
				{
					// do nothing, since this label was already in a fusion-chain
				}
				else
				{
//					vCheckedLabels.clear(); //TODO WEGWEGWEG
					RelabelRegionsAfterFusion(labelImage, idx, vLabel1, vCheckedLabels);
				}
				vMerge = true;
			}
//			debug("merged " + m_MergingHist.size()+ " region pairs.");
		}

		if(vSplit || vMerge) 
		{
			if(m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) 
			{
				// TODO: this can be removed when the statistics are updated using
				// seeds and flood fill iteartors.
				//TODO sts
				// RenewDeconvolutionStatistics(m_LabelImage, this->GetDataInput());
			}
		}

		return vConvergence;
	}

	private void RegisterSeedsAfterSplit(Algorithm aLabelImage, Point aIndex, int aLabel, 
			HashMap<Point, ContourParticle> aCandidateContainer) {


		for (Point vSeedIndex : connFG.iterateNeighbors(aIndex)) {
			int vLabel = labelImage.getLabelAbs(vSeedIndex);

			if (vLabel == aLabel) 
			{
				Pair<Point, Integer> vSeed = new Pair<Point, Integer>(vSeedIndex, vLabel);

				m_Seeds.add(vSeed);

				/// At the position where we put the seed, inform the particle
				/// that it has to inform its neighbor in case it moves (if there
				/// is a particle at all at this spot; else we don't have a problem
				/// because the label will not move at the spot and therefore the
				/// seed will be effective).


				ContourParticle vParticle = aCandidateContainer.get(vSeedIndex);
				if(vParticle != null) {
					vParticle.m_processed = true;
				}
			}
		}
	}

	/**
	 * Detect oscillations and store values in history.
	 */
	private void DetectOscillations(HashMap<Point, ContourParticle> m_Candidates)
	{
		        
        double vSum = SumAllEnergies(m_Candidates);
//        debug("sum of energies: "+vSum);
		for(int vI = 0; vI < m_OscillationHistoryLength; vI++) 
		{
			double vSumOld = m_OscillationsEnergyHist[vI];
// debug("check nb: " + vAllCandidates.size() + " against " + m_OscillationsNumberHist[0]);
//			debug("m_Candidates.size()=" + m_Candidates.size()
//					+ "m_OscillationsNumberHist[vI]="
//					+ m_OscillationsNumberHist[vI]);

			if(m_Candidates.size() == m_OscillationsNumberHist[vI]
					&& Math.abs(vSum - vSumOld) <= 1e-5 * Math.abs(vSum)) 
			{
				/// here we assume that we're oscillating, so we decrease the
				/// acceptance factor:
				debug("nb of accepted points reduced to: " + m_AcceptedPointsFactor);
				m_AcceptedPointsFactor *= settings.m_AcceptedPointsReductionFactor;
			}
		}

		/// Shift the old elements:
		//TODO sts maybe optimize by modulo list?
		for (int vI = 1; vI < m_OscillationHistoryLength; vI++) {
		    m_OscillationsEnergyHist[vI-1] = m_OscillationsEnergyHist[vI];
		    m_OscillationsNumberHist[vI-1] = m_OscillationsNumberHist[vI];
		}

		/// Fill the new elements:
		m_OscillationsEnergyHist[m_OscillationHistoryLength-1] = vSum;
		m_OscillationsNumberHist[m_OscillationHistoryLength-1] = m_Candidates.size();

		
	}


	/**
	 * Iterates through ContourParticles in m_InnerContourContainer <br>
	 * Builds mothers, daughters, computes energies and <br>
	 * fills m_CompetingRegionsMap
	 * @param aReturnContainer
	 */
	void RebuildCandidateList(HashMap<Point, ContourParticle> aReturnContainer)
	{
		aReturnContainer.clear();

        /// Add all the mother points - this is copying the inner contour list.
        /// (Things get easier afterwards if this is done in advance.)
		
		//calculate energy for change to BG (shrinking)
		for(Entry<Point, ContourParticle> vPointIterator : m_InnerContourContainer.entrySet()) 
		{
			Point vCurrentIndex = vPointIterator.getKey();
			ContourParticle vVal = vPointIterator.getValue();
		
		
			vVal.candidateLabel = 0;
			vVal.referenceCount = 0; // doesn't matter for the BG
			vVal.isMother = true;
			vVal.isDaughter = false;
			vVal.m_processed = false;
			vVal.energyDifference = CalculateEnergyDifferenceForLabel(vCurrentIndex, vVal, bgLabel).first;
			vVal.getMotherList().clear(); // this is indeed necessary!
			vVal.getDaughterList().clear(); // this is necessary!!

			vVal.getTestedList().clear();
			vVal.setLabelHasBeenTested(bgLabel);

			aReturnContainer.put(vCurrentIndex, vVal);
		}
		
        /// Iterate the contour list and visit all the neighbors in the
        /// FG-Neighborhood.
		//calculate energy for expanding into neighborhood (growing)
		for(Entry<Point, ContourParticle> vPointIterator : m_InnerContourContainer.entrySet()) 
		{
			Point vCurrentIndex = vPointIterator.getKey();
			ContourParticle vVal = vPointIterator.getValue();

			int vLabelOfPropagatingRegion = vVal.label;
			// vLabelImageIterator.SetLocation(vCurrentIndex);
		
			Connectivity conn = connFG;
			for(Point q : conn.iterateNeighbors(vCurrentIndex)) 
			{
				int vLabelOfDefender = labelImage.getLabelAbs(q);
				if(vLabelOfDefender == forbiddenLabel) {
					continue;
				}
				
				// expanding into other region / label. (into same region: nothing happens)
				if(vLabelOfDefender != vLabelOfPropagatingRegion) 
				{
					Point vNeighborIndex = q;

					// Tell the mother about the daughter:
					//TODO sinnloser lookup? use vVal instead of aReturnContainer.get(vCurrentIndex)
					aReturnContainer.get(vCurrentIndex).getDaughterList().add(vNeighborIndex);
		
					ContourParticle vContourPointItr = aReturnContainer.get(vNeighborIndex);
					if(vContourPointItr == null) 
					{
						// create a new entry (a daughter), the contour point
						// has not been part of the contour so far.

						ContourParticle vOCCValue = new ContourParticle();
						//itk commented // vOCCValue.m_candidates.insert(vCandidateElement);
						vOCCValue.candidateLabel = vLabelOfPropagatingRegion;
						vOCCValue.label = vLabelOfDefender;
						vOCCValue.intensity = getIntensity(vNeighborIndex);
						vOCCValue.isDaughter = true;
						vOCCValue.isMother = false;
						vOCCValue.m_processed = false;
						vOCCValue.referenceCount = 1;
						vOCCValue.energyDifference = CalculateEnergyDifferenceForLabel(vNeighborIndex, vOCCValue, vLabelOfPropagatingRegion).first;
						vOCCValue.setLabelHasBeenTested(vLabelOfPropagatingRegion);
						// Tell the daughter about the mother:
						vOCCValue.getMotherList().add(vCurrentIndex);

						aReturnContainer.put(vNeighborIndex, vOCCValue);

					} 
					else /// the point is already part of the candidate list
					{
						vContourPointItr.isDaughter = true;
						/// Tell the daughter about the mother (label does not matter!):
						vContourPointItr.getMotherList().add(vCurrentIndex);

						/// Check if the energy difference for this candidate label 
						/// has not yet been calculated.
						if(!vContourPointItr.hasLabelBeenTested((vLabelOfPropagatingRegion))) 
						{
							vContourPointItr.setLabelHasBeenTested(vLabelOfPropagatingRegion);
							
							Pair<Double, Boolean> energyAndMerge = CalculateEnergyDifferenceForLabel(
									vNeighborIndex, vContourPointItr, vLabelOfPropagatingRegion);
							
							double vEnergyDiff = energyAndMerge.first;
							boolean aMerge = energyAndMerge.second;
							if(vEnergyDiff < vContourPointItr.energyDifference) 
							{
								vContourPointItr.candidateLabel = vLabelOfPropagatingRegion;
								vContourPointItr.energyDifference = vEnergyDiff;
								vContourPointItr.referenceCount = 1;

//TODO new sts merge 05.03.2012
//								if((*aMerge)[vP] && vParticle.m_label != 0 && vParticle.m_candidateLabel != 0)
// this is ALWAYS merging
								if(aMerge && vContourPointItr.label != bgLabel && vContourPointItr.candidateLabel != bgLabel)
								{
									int L1 = vContourPointItr.candidateLabel;
									int L2 = vContourPointItr.label;
									
									LabelPair pair = new LabelPair(L1, L2);

									m_CompetingRegionsMap.put(vCurrentIndex, pair);
									
									
//TODO removed from itk
						            /// Ensure the point does not move since we'd like to merge
						            /// here. Todo so, we set the energy to a large value.
						            //            vParticle.m_energyDifference = return NumericTraits<EnergyDifferenceType>::max();
//									aReturnContainer.remove(vCurrentIndex);
								}
								
							}
						} 
						else 
						{
							/// If the propagating label is the same as the candidate label,
							/// we have found 2 or more mothers of for this contour point.ten
							if(vContourPointItr.candidateLabel == vLabelOfPropagatingRegion) {
								vContourPointItr.referenceCount++;
							}
						}
						
					} // else the point is already part of the candidate list
				}

			} // for(Point q : conn.itNeighborsOf(vCurrentIndex))

		}

	}


	/**
	 * Filters topological incompatible candidates (topological dependencies) 
	 * and non-improving energies. 
	 */
	private void FilterCandidates(HashMap<Point, ContourParticle> m_Candidates)
	{
		
        /**
         * Find topologically compatible candidates and store their indices in
         * vLegalIndices.
         */
		List<Point> vLegalIndices = new LinkedList<Point>();
		List<Point> vIllegalIndices = new LinkedList<Point>();
		for(Entry<Point,ContourParticle> vPointIterator : m_Candidates.entrySet()) 
		{
			Point pIndex = vPointIterator.getKey();
			ContourParticle p = vPointIterator.getValue();

			// Check if this point already was processed
			if(!p.m_processed) 
			{
				// Check if it is a mother: only mothers can be seed points
				// of topological networks. Daughters are always part of a
				// topo network of a mother.
				if(!p.isMother) {
					continue;
				}

				/**
				 * Build the dependency network for this seed point:
				 */
				Stack<Point> vIndicesToVisit = new Stack<Point>();
				List<ContourPointWithIndexType> vSortedNetworkMembers = new LinkedList<ContourPointWithIndexType>();
				vIndicesToVisit.push(pIndex);
				p.m_processed = true;

				while(!vIndicesToVisit.empty())
				{
					Point vSeedIndex = vIndicesToVisit.pop();
					ContourParticle vCurrentMother = m_Candidates.get(vSeedIndex);

                    /// Add the seed point to the network
                    ContourPointWithIndexType vSeedContourPointWithIndex = new ContourPointWithIndexType(vSeedIndex, vCurrentMother);
                    vSortedNetworkMembers.add(vSeedContourPointWithIndex);

                    // Iterate all children of the seed, push to the stack if there
                    // is a mother.
					List<Point> vDaughterIt = vCurrentMother.getDaughterList();
					for(Point vDaughterContourIndex : vDaughterIt) 
					{
//	                    if(vAllCandidates.find(vDaughterContourIndex) == vAllCandidates.end())
//	                        std::cout << "daughter index found not in the list: " << vDaughterContourIndex << std::endl;

                        ContourParticle vDaughterContourPoint = m_Candidates.get(vDaughterContourIndex);

						if(!vDaughterContourPoint.m_processed) 
						{
							vDaughterContourPoint.m_processed = true;

							if(vDaughterContourPoint.isMother) {
								vIndicesToVisit.push(vDaughterContourIndex);
							} 
							else {
								ContourPointWithIndexType vDaughterContourPointWithIndex = 
									new ContourPointWithIndexType(vDaughterContourIndex, vDaughterContourPoint);
								vSortedNetworkMembers.add(vDaughterContourPointWithIndex);
							}

							/// Push all the non-processed mothers of this daughter to the stack
							List<Point> vDMIt = vDaughterContourPoint.getMotherList();
							for(Point vDM : vDMIt) 
							{
								ContourParticle vMotherOfDaughterPoint = m_Candidates.get(vDM);
								if(!vMotherOfDaughterPoint.m_processed) 
								{
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

				// TODO HashSet problem (maybe need hashmap)
				HashSet<Point> vSelectedCandidateIndices = new HashSet<Point>();
				
				for(ContourPointWithIndexType vNetworkIt : vSortedNetworkMembers)
				{

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
					if(vNetworkIt.p.isDaughter) 
					{
						ContourParticle vCand = m_Candidates.get(vNetworkIt.pIndex);
						if(vCand.referenceCount < 1 && vCand.candidateLabel != 0) {
							vLegalMove = false;
						}
                    }

                    ///
                    /// RULE 2: All daughters already accepted the label of this
                    ///        mother have at least one another mother.
                    /// AND
                    /// RULE 3: Mothers are still valid mothers (to not introduce
                    ///         holes in the FG region).
                    ///
					
                    if (vLegalMove && vNetworkIt.p.isMother) 
                    {
                        /// Iterate the daughters and check their reference count

                        boolean vRule3Fulfilled = false;

                        for(Point vDaughterIndicesIterator: vNetworkIt.p.getDaughterList())
                        {
                            ContourParticle vDaughterPoint = m_Candidates.get(vDaughterIndicesIterator);

                            /// rule 2:
                            boolean vAcceptedDaugtherItContained = vSelectedCandidateIndices.contains(vDaughterIndicesIterator);

                            if(vAcceptedDaugtherItContained) 
                            {
                                /// This daughter has been accepted 
                            	/// and needs a reference count > 1, 
                            	/// else the move is invalid.
                                if (vDaughterPoint.candidateLabel == vNetworkIt.p.label 
                                		&& vDaughterPoint.referenceCount <= 1) 
                                {
                                    vLegalMove = false;
                                    break;
                                }
                            }

                            /// rule 3:
                            if (!vRule3Fulfilled) 
                            {
                                if(!vAcceptedDaugtherItContained) {
                                    /// There is a daughter that has not yet been accepted.
                                    vRule3Fulfilled = true;
                                } else {
                                	
                                    /// the daughter has been accepted, but may
                                    /// have another candidate label(rule 3b):
                                	
                                	if(m_Candidates.get(vDaughterIndicesIterator).candidateLabel != vNetworkIt.p.label) 
                                	{
                                		vRule3Fulfilled = true;
                                	}
                                }
                            }
                        }

                        if (!vRule3Fulfilled) vLegalMove = false;
                    }

					if(vLegalMove) 
					{
                        /// the move is legal, store the index in the container
                        /// with accepted candidates of this network.
                        vSelectedCandidateIndices.add(vNetworkIt.pIndex);

                        /// also store the candidate in the global candidate index container:
                        //TODO used nowhere
                        vLegalIndices.add(vNetworkIt.pIndex);

                        /// decrease the references of its daughters(with the same 'old' label).
						for(Point vDaughterIndicesIterator : vNetworkIt.p.getDaughterList()) 
						{
							ContourParticle vDaughterPoint = m_Candidates.get(vDaughterIndicesIterator);
							if(vDaughterPoint.candidateLabel == vNetworkIt.p.label) {
								vDaughterPoint.referenceCount--;
							}
						}
                    } 
					else {
                        vIllegalIndices.add(vNetworkIt.pIndex);
                    }
                }
            }
        }
	
        /**
         * Filter all candidates with the illigal indices
         */
		        
		for(Point vIlligalIndicesIt : vIllegalIndices) {
			m_Candidates.remove(vIlligalIndicesIt);
		}
		

        /**
         * Filter candidates according to their energy
         */
		        
        Iterator<Entry<Point, ContourParticle>> it = m_Candidates.entrySet().iterator();
		while(it.hasNext()) 
		{
			Entry<Point, ContourParticle> vStoreIt = it.next(); // iterator to work with
			if(vStoreIt.getValue().energyDifference >= 0) {
				it.remove();
//				m_Candidates.remove(vStoreIt);
			}
		}
        //        WriteContourPointContainer("contourPointsAfterEnergyCheck.txt", vAllCandidates);

		
	}


	/**
	 * called while iterating over m_InnerContourContainer in RemoveSinglePointRegions()
	 * 
	 * calling
	 * m_InnerContourContainer.remove(vCurrentIndex);
	 * m_InnerContourContainer.put(vCurrentIndex, vContourPoint);
	 */
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
	
        ///
        /// Update the label image. The new point is either a contour point or 0,
        /// therefore the negative label value is set.
		//TODO sts is this true? what about enclosed pixels? 
		// ie first was like a 'U', then 'O' and now the hole is filled, then it is an inner point
		// where is this handled?
        ///
		labelImage.setLabel(vCurrentIndex, labelImage.labelToNeg(vToLabel));
		
//TODO sts inserted following lines
		if(labelImage.labelToAbs(vToLabel)!=vToLabel)
		{
			debug("changed label to absolute value. is this valid?\n");
			second.label=labelImage.labelToAbs(vToLabel);
		} 
// END inserted sts
	
        ///
        /// STATISTICS UPDATE
        /// Update the statistics of the propagating and the loser region.
        ///
		UpdateStatisticsWhenJump(aParticle, vFromLabel, vToLabel);

		if(m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
			// TODO
			// UpdateConvolvedImage(vCurrentIndex, vFromLabel, vToLabel);
		}

		/// TODO: A bit a dirty hack: we store the old label for the relabeling
		/// procedure later on...either introduce a new variable or rename the
		/// variable (which doesn't work currently :-).
		second.candidateLabel = vFromLabel;

		///
		/// Clean up
		///

		/// The loser region (if it is not the BG region) has to add the
		/// neighbors of the lost point to the contour list.
		if(vFromLabel != bgLabel) {
			AddNeighborsAtRemove(vFromLabel, vCurrentIndex);
		}

		///
		/// Erase the point from the surface container in case it now belongs to
		/// the background. Else, add the point to the container (or replace it
		/// in case it has been there already).
		///
		if(vToLabel == bgLabel) {
			m_InnerContourContainer.remove(vCurrentIndex);
		} else {
			
			//TODO compare with itk. vContourPoint = second is unnecessary in java. was this a copy in c++?
			
			ContourParticle vContourPoint = second;
			vContourPoint.label = vToLabel;
			/// The point may or may not exist already in the m_InnerContainer.
			/// The old value, if it exist, is just overwritten with the new
			/// contour point (with a new label).

			m_InnerContourContainer.put(vCurrentIndex, vContourPoint);
		}

		/// Remove 'enclosed' contour points from the container. For the BG this
		/// makes no sense.
		if(vToLabel != bgLabel) {
			MaintainNeighborsAtAdd(vToLabel, vCurrentIndex);
		}
	}


/**
 * 
 * iterator problem: calling m_InnerContourContainer.put() <br>
 * 
 * Removing a point / changing its label generates new contour points in general <br>
 * This method generates the new contour particles and adds them to container
 * itk::AddNeighborsAtRemove
 * @param pIndex 		changing point		
 * @param aAbsLabel 	old label of this point
 */
	void AddNeighborsAtRemove(int aAbsLabel, Point pIndex)
	{
		
		//TODO dummy
		Connectivity conn = connFG;
		//END dummy
		
		//TODO statistic update? 
		
//		ContourParticle p = m_InnerContourContainer.get(pIndex);
		for(Point qIndex:conn.iterateNeighbors(pIndex))
		{
			int qLabel = labelImage.getLabel(qIndex);
			
			//TODO can the labels be negative? somewhere, they are set (maybe only temporary) to neg values
			if(labelImage.isContourLabel(aAbsLabel))
			{
				debug("AddNeighborsAtRemove. one label is not absLabel\n");
				int dummy=0;
				dummy=dummy+0;
			}
			
			if(labelImage.isInnerLabel(qLabel) && qLabel==aAbsLabel) // q is a inner point with the same label as p
			{
				ContourParticle q = new ContourParticle();
				q.label=aAbsLabel;
				q.candidateLabel=bgLabel;
				q.intensity = getIntensity(qIndex);
				
				labelImage.setLabel(qIndex, labelImage.labelToNeg(aAbsLabel));
				m_InnerContourContainer.put(qIndex, q);
			}
			//TODO this never can be true 
			// (since isContourLabel==> neg values AND (qLabel == aAbsLabel) => pos labels
			else if(labelImage.isContourLabel(qLabel)&& qLabel == aAbsLabel) // q is contour of the same label
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
	 * iterator problem: m_InnerContourContainer.remove 
	 * <br> 
	 * itk::MaintainNeighborsAtAdd
     * Maintain the inner contour container: <br>
     * - Remove all the indices in the BG-connected neighborhood, that are
     *   interior points, from the contour container. <br>
     *   Interior here means that none neighbors in the FG-Neighborhood
     *   has a different label.
     */
    void MaintainNeighborsAtAdd(int aLabelAbs, Point pIndex) 
	{
//		ContourParticle p = m_InnerContourContainer.get(pIndex);
		int aLabelNeg = labelImage.labelToNeg(aLabelAbs);
		
        // itk 1646: we set the pixel value already to ensure the that the 'enclosed' check
        // afterwards works.
		//TODO is p.label (always) the correct label?
		labelImage.setLabel(pIndex, labelImage.labelToNeg(aLabelAbs));
		
		Connectivity conn = connBG;
		for(Point qIndex: conn.iterateNeighbors(pIndex))
		{
			// from itk:
            /// It might happen that a point, that was already accepted as a 
            /// candidate, gets enclosed by other candidates. This
            /// points must not be added to the container afterwards and thus
            /// removed from the main list.
			
			// TODO ??? why is BGconn important? a BGconnected neighbor cannot 
			// change the "enclosed" status for FG?
			if(labelImage.getLabel(qIndex)==aLabelNeg && labelImage.isEnclosedByLabel(qIndex, aLabelAbs))
			{
				m_InnerContourContainer.remove(qIndex);
				labelImage.setLabel(qIndex, aLabelAbs);
			}
		}
		
		if(labelImage.isEnclosedByLabel(pIndex, aLabelAbs))
		{
			m_InnerContourContainer.remove(pIndex);
			labelImage.setLabel(pIndex, aLabelAbs);
		}
	}
	
	/**
	 * The function relabels a region starting from the position aIndex.
	 * This method assumes the label image to be updated. It is used to relabel
	 * a region that was split by another region (maybe BG region).
	 */
	void RelabelRegionsAfterSplit(LabelImage aLabelImage, Point aIndex, int aLabel)
	{
//		debug("Split at " + aIndex.toString() + " of label " + aLabel);
//		MVC.selectPoint(aIndex);
		// template <class TInputImage, class TInitImage, class TOutputImage >
		if(aLabelImage.getLabelAbs(aIndex)==aLabel) 
		{
			MultipleThresholdImageFunction<Integer> vMultiThsFunction = new MultipleThresholdImageFunction<Integer>(aLabelImage);
			vMultiThsFunction.AddThresholdBetween(aLabel, aLabel);
			int negLabel = aLabelImage.labelToNeg(aLabel);
			vMultiThsFunction.AddThresholdBetween(negLabel, negLabel);

//			ForestFire(aLabelImage, aIndex, vMultiThsFunction, m_MaxNLabels++);
			ForestFire(aLabelImage, aIndex, vMultiThsFunction, labelDispenser.getNewLabel());
		}
	}


	/// The function relabels 2 neighboring regions. These 2 regions are
	/// fused into a new region. Both regions are at position of aIndex
	/// or adjacent to it.
	/// Relabel2AdjacentRegionsAfterToplogicalChange expects the label image to
	/// be updated already: both methods are connected via the seedpoint. This
	/// method may be used to fuse 2 regions in the region competition mode.
	
	void RelabelRegionsAfterFusion(LabelImage aLabelImage, Point aIndex, int aL1, Set<Integer> aCheckedLabels) 
	{
//		debug("Relabel after fusion at " + aIndex.toString() + " of label " + aL1);
//		if(m_iteration_counter==6)
//		{
//			System.out.println(6);
//		}
//		MVC.selectPoint(aIndex);
		
		MultipleThresholdImageFunction<Integer> vMultiThsFunction = new MultipleThresholdImageFunction<Integer>(aLabelImage);
		
//		LinkedList<Integer> vLabelsToCheck = new LinkedList<Integer>();
		Stack<Integer> vLabelsToCheck = new Stack<Integer>();
		
		vLabelsToCheck.push(aL1);
	    vMultiThsFunction.AddThresholdBetween(aL1, aL1);
	    int aL1Neg=aLabelImage.labelToNeg(aL1);
	    vMultiThsFunction.AddThresholdBetween(aL1Neg, aL1Neg);
	    aCheckedLabels.add(aL1);		
	    
//	    for(int vLabelToCheck : vLabelsToCheck) 
		while(!vLabelsToCheck.isEmpty())
		{
			int vLabelToCheck = vLabelsToCheck.pop();
			for(LabelPair vMergingLabelsPair : m_CompetingRegionsMap.values())
			{
				int vLabel1 = vMergingLabelsPair.first;
				int vLabel2 = vMergingLabelsPair.second;

				if(vLabel1 == vLabelToCheck && !aCheckedLabels.contains(vLabel2))
				{
					vMultiThsFunction.AddThresholdBetween(vLabel2, vLabel2);
					vMultiThsFunction.AddThresholdBetween(aLabelImage.labelToNeg(vLabel2), labelImage.labelToNeg(vLabel2));
					aCheckedLabels.add(vLabel2);
					vLabelsToCheck.push(vLabel2);
				}
				if(vLabel2 == vLabelToCheck && !aCheckedLabels.contains(vLabel1))
				{
					vMultiThsFunction.AddThresholdBetween(vLabel1, vLabel1);
					vMultiThsFunction.AddThresholdBetween(aLabelImage.labelToNeg(vLabel1), labelImage.labelToNeg(vLabel1));
					aCheckedLabels.add(vLabel1);
					vLabelsToCheck.push(vLabel1);
				}
			}
		}
		if(vMultiThsFunction.EvaluateAtIndex(iterator.pointToIndex(aIndex))){
//		    ForestFire(aLabelImage, aIndex, vMultiThsFunction, m_MaxNLabels++);
		    ForestFire(aLabelImage, aIndex, vMultiThsFunction, labelDispenser.getNewLabel());
		}
		
//		if(m_iteration_counter==6)
//		{
//			displaySlice(aIndex.toString());
//		}
		
	}


	void RemoveSinglePointRegions()
	{
		
		//TODO: here we go first through contour points to find different labels 
		// and then checking for labelInfo.count==1.
		// instead, we could iterate over all labels (fewer labels than contour points), 
		// detecting if one with count==1 exists, and only IFF one such label exists searching for the point.
		// but atm, im happy that it detects "orphan"-contourPoints (without attached labelInfo)
		
		
//TODO java.util.ConcurrentModificationException
		//TODO ugly casting
		
		Object[] copy = m_InnerContourContainer.entrySet().toArray();
		for(Object o : copy) 
//		for(Entry<Point, ContourParticle> vIt : copy) 
		{
			Entry<Point, ContourParticle> vIt = (Entry<Point, ContourParticle>) o;
			
			ContourParticle vWorkingIt = vIt.getValue();
			// if (m_Count[vWorkingIt->second.m_label] == 1) {
			
			LabelInformation info = labelMap.get(vWorkingIt.label);
			if(info==null)
			{
				//TODO debug
				labelImage.displaySlice("***info is null for: "+vIt.getKey());
				debug("***info is null for: "+vIt.getKey());
				MVC.selectPoint(vIt.getKey());
				continue;
			}
			if(info.count == 1) 
			{
				vWorkingIt.candidateLabel = bgLabel;
				// TODO changed from
				// ChangeContourPointLabelToCandidateLabel(vWorkingIt);
				// to this:

				// TODO!!!! vIt could be removed from container 
				// in ChangeContourPointLabelToCandidateLabel
				ChangeContourPointLabelToCandidateLabel(vIt);
			}
		}
		CleanUp();
	}

	
	/**
	 * Use only top vNbElements * m_AcceptedPointsFactor Elements. 
	 */
	private void FilterCandidatesContainerUsingRanks(HashMap<Point, ContourParticle> aContainer)
	{
	
		// Copy the candidates to a set (of ContourPointWithIndex). This
		// will sort them according to their energy gradients.
		List<ContourPointWithIndexType> vSortedList = new LinkedList<ContourPointWithIndexType>();

		for(Entry<Point, ContourParticle> vPointIterator : aContainer.entrySet()) 
		{
			ContourPointWithIndexType vCand = new ContourPointWithIndexType(vPointIterator.getKey(),vPointIterator.getValue());
			vSortedList.add(vCand);
		}

		Collections.sort(vSortedList);

		// EnergyDifferenceType vBestEnergy = vSortedList.front().m_ContourPoint.m_energyDifference;

		int vNbElements = vSortedList.size();
		vNbElements = (int)(vNbElements * m_AcceptedPointsFactor + 0.5);
		// if(vNbElements < 1) {
		// vNbElements = 1;
		// }

		/// Fill the container with the best candidate first, then
		/// the next best that does not intersect the tabu region of
		/// all inserted points before.
		aContainer.clear();

		for(ContourPointWithIndexType vSortedListIterator : vSortedList)
		{
			if(!(vNbElements >= 1)) break;

			vNbElements--;
			Point vCandCIndex = vSortedListIterator.pIndex;
			Iterator<Entry<Point, ContourParticle>> vAcceptedCandIterator = aContainer.entrySet().iterator();
			boolean vValid = true;
			while(vAcceptedCandIterator.hasNext())
			{
				Point vCIndex = vAcceptedCandIterator.next().getKey();

				// TODO nothing happens in here. itk::2074
				// itk commented

				// float vDist = 0;
				// for (unsigned int vD = 0; vD < m_Dim; vD++) {
				// vDist += (vCIndex[vD] - vCandCIndex[vD]) * (vCIndex[vD] - vCandCIndex[vD]);
				// }
				// InternalImageSizeType vPSFsize = m_PSF->GetLargestPossibleRegion().GetSize();

				// gong_test
				// if (sqrt(vDist) < 0.001) {//< vPSFsize[vD] / 400) {
				// /// This candidate didn't pass. Abort the test.
				// vValid = false;
				// break;
				// }

				/*
				 * if(vNbElements > vConstNbElements * 0.9 && vNbElements < vConstNbElements *
				 * 0.5){
				 * vValid = false;
				 * }
				 */

				/*
				 * if(vSortedListIterator->m_ContourPoint.m_energyDifference > 0.2 * vBestEnergy
				 * || vSortedListIterator->m_ContourPoint.m_energyDifference < 0.7 *
				 * vBestEnergy)
				 * {
				 * vValid=false;
				 * }
				 */
			}
			if(vValid)
			{
				/// This candidate passed the test and is added to the TempRemoveCotainer:
				aContainer.put(vSortedListIterator.pIndex, vSortedListIterator.p);
				// aContainer[vSortedListIterator.pIndex] = vSortedListIterator.m_ContourPoint;
			}
		}
		// std::cout << "container size: " << aContainer->size() << std::endl;
	}


	static float wGamma = 0.003f;
	double calcGammaEnergy(Entry<Point,ContourParticle> entry)
	{
		Point pIndex = entry.getKey();			// coords of motherpoint
		ContourParticle p = entry.getValue();	// mother particle
		int pLabel = p.label;					// label of motherpoint 
		
		int nSameNeighbors=0;
		Connectivity conn = connFG;
		
		// version 3 with COnnectivity.getNeighbots(Point)
		for(Point neighbor:conn.iterateNeighbors(pIndex))
		{
			int neighborLabel=labelImage.getLabelAbs(neighbor);
//			int neighborLabel=labelImage.getLabel(neighbor);
//			neighborLabel=labelImage.labelToAbs(neighborLabel);
			if(neighborLabel==pLabel)
			{
				nSameNeighbors++;
			}
		}
		
		int nOtherNeighbors=4-nSameNeighbors;
		int dGamma = nSameNeighbors - nOtherNeighbors;
		
		return wGamma*dGamma;
	}
	
	
	private Pair<Double, Boolean> CalculateEnergyDifferenceForLabel(Point aContourIndex, ContourParticle aContourPointPtr, int aToLabel) {
    //        typedef ContourPoint::ContourPointCandidateListType::iterator CandLabelItType;
    //        CandLabelItType vCandLabelIt = aContourPointIt->second.m_candidates.begin();
    //        CandLabelItType vCandLabelItend = aContourPointIt->second.m_candidates.end();
    //
    //        for (; vCandLabelIt != vCandLabelItend; ++vCandLabelIt) {
    //            LabelAbsPixelType aToLabel = aContourPointPtr->m_candidateLabel;
    //            OuterContourContainerKeyType vCurrentIndex = aContourPointPtr->first;

		float m_EnergyRegionCoeff = settings.m_EnergyRegionCoeff;
		
		float vCurrentImageValue = aContourPointPtr.intensity;
		int vCurrentLabel = aContourPointPtr.label;
		double vEnergy = 0.0;
		boolean vMerge = false;

        /// Calculate the change in energy due to the change of intensity when changing
        /// from one label 'from' to another 'to'.
        
		
		if (m_EnergyRegionCoeff != 0)
		{
			if(m_EnergyFunctional == EnergyFunctionalType.e_CV)
			{
				vEnergy += m_EnergyRegionCoeff * 
					CalculateCVEnergyDifference(vCurrentImageValue, vCurrentLabel, aToLabel);
			}
			else if(m_EnergyFunctional == EnergyFunctionalType.e_GaussPS)
			{
                Pair<Double, Boolean> vV = CalculateGaussPSEnergyDifference(aContourIndex,
                        vCurrentImageValue, vCurrentLabel, aToLabel);
                vEnergy += m_EnergyRegionCoeff * vV.first;
                vMerge = vV.second;
			}
			else if (m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC)
			{
			}
            else if (m_EnergyFunctional == EnergyFunctionalType.e_MS) 
            {
                vEnergy += m_EnergyRegionCoeff * CalculateMSEnergyDifference(
                        vCurrentImageValue, vCurrentLabel, aToLabel);
            } 
        }
		
		
		
//TODO hier weiter
//		System.out.println("*********hier weiter");
		
		//TODO dummy
		float m_EnergyContourLengthCoeff = settings.m_EnergyContourLengthCoeff;
		// end dummy

		if (settings.m_EnergyUseCurvatureRegularization &&
				m_EnergyFunctional != EnergyFunctionalType.e_PSwithCurvatureFlow &&
				m_EnergyContourLengthCoeff != 0) 
		{
		    if (m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
//		        vEnergy += //m_Intensities[aToLabel] * m_Intensities[aToLabel] *
//		        		m_EnergyContourLengthCoeff * CalculateCurvatureBasedGradientFlow(
//		                this->GetDataInput(), m_LabelImage, aContourIndex,
//		                vCurrentLabel, aToLabel);
		    } else if (m_EnergyFunctional == EnergyFunctionalType.e_CV) 
		    {
		    	
		    	double eCurv = CalculateCurvatureBasedGradientFlow(
		    			labelImage, aContourIndex,vCurrentLabel, aToLabel);
//		    	debug("eCurv="+eCurv+" vEnergy="+vEnergy);
		        vEnergy += //m_Means[aToLabel] * // m_Means[aToLabel] *
		        		m_EnergyContourLengthCoeff * eCurv;
		    } else {
		        vEnergy += m_EnergyContourLengthCoeff * CalculateCurvatureBasedGradientFlow(
		        		labelImage, aContourIndex, vCurrentLabel, aToLabel);
		    }
		}
		
		
        /// add a bolloon force and a constant outward flow. If fronts were
        /// touching, no constant flow is imposed (cancels out).
        if(vCurrentLabel == 0)  // growing
        {
            vEnergy -= settings.m_ConstantOutwardFlow;
            if (settings.m_BalloonForceCoeff > 0) { // outward flow
                vEnergy -= settings.m_BalloonForceCoeff * vCurrentImageValue;
            } else {
                vEnergy -= -settings.m_BalloonForceCoeff * (1 - vCurrentImageValue);
            }
        } else if (aToLabel == 0) // shrinking
        {
            vEnergy += settings.m_ConstantOutwardFlow;
        }
        
        
        /// For the full-region based energy models, register competing regions
        /// undergo a merge.
		if (m_EnergyFunctional == EnergyFunctionalType.e_CV || 
			m_EnergyFunctional == EnergyFunctionalType.e_GaussWithVariancePC || 
			m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC)
        {
			vMerge = CalculateMergingEnergyForLabel(vCurrentLabel, aToLabel);
        }
        
        return new Pair<Double, Boolean>(vEnergy, vMerge);

    }
	

	boolean CalculateMergingEnergyForLabel(int aLabelA, int aLabelB)
	{
		/// store this event to check afterwards if we should merge
		/// the 2 regions.

		if(aLabelA != bgLabel && aLabelB != bgLabel) // we are competeing.
		{ 
			/// test if merge should be performed:
			double value = CalculateKLMergingCriterion(aLabelA, aLabelB);
//			debug("KL: it="+m_iteration_counter+" "+aLabelA+" "+aLabelB+" "+value);
			if(value < settings.m_RegionMergingThreshold)
			{
				return true;
			}
		}

		return false;

	}

	
	double CalculateKLMergingCriterion(int L1, int L2)
	{

		LabelInformation aL1 = labelMap.get(L1);
		LabelInformation aL2 = labelMap.get(L2);

		double vMu1 = aL1.mean;
		double vMu2 = aL2.mean;
		double vVar1 = aL1.var;
		double vVar2 = aL2.var;
		int vN1 = aL1.count;
		int vN2 = aL2.count;

//		debug("l1="+L1+" L2="+L2);
		
		double result = CalculateKLMergingCriterion(vMu1, vMu2, vVar1, vVar2, vN1, vN2);
//		debug("KL: it="+m_iteration_counter+" "+L1+" "+L2+" "+result);
		if(Double.isNaN(result))
		{
			debug("CalculateKLMergingCriterion is NaN");
			throw new RuntimeException("Double.isNaN in CalculateKLMergingCriterion");
		}
		assert(!Double.isNaN(result));
		return result;

	}
	
	double CalculateKLMergingCriterion(double aMu1, double aMu2, double aVar1, double aVar2, int aN1, int aN2)
	{
		double vMu12 = (aN1 * aMu1 + aN2 * aMu2) / (aN1 + aN2);

		double vSumOfSq1 = aVar1 * (aN1 - 1) + aN1 * aMu1 * aMu1;
		double vSumOfSq2 = aVar2 * (aN2 - 1) + aN2 * aMu2 * aMu2;

		double vVar12 = (1.0 / (aN1 + aN2 - 1.0))
				* (vSumOfSq1 + vSumOfSq2 - (aN1 + aN2) * vMu12 * vMu12);
		
		if(vVar12==0)
		{
//			System.out.print("vVar12==0");
			debug("vVar12==0");
			return 0;
		}

		double vDKL1 = (aMu1 - vMu12) * (aMu1 - vMu12) / (2.0 * vVar12) + 0.5
				* (aVar1 / vVar12 - 1.0 - Math.log(aVar1 / vVar12));

		double vDKL2 = (aMu2 - vMu12) * (aMu2 - vMu12) / (2.0 * vVar12) + 0.5
				* (aVar2 / vVar12 - 1.0 - Math.log(aVar2 / vVar12));

		return (double)vDKL1 + (double)vDKL2;
	}
	
    double CalculateCVEnergyDifference(float aValue, int fromLabel, int toLabel) {
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
		double vNewToMean = (to.mean*to.count + aValue)/(to.count+1);
		return (aValue-vNewToMean)*(aValue-vNewToMean) - (aValue-from.mean)*(aValue-from.mean);
    }
    
    
    Pair<Double, Boolean> 
    CalculateGaussPSEnergyDifference(Point aCenterIndex, double aValue, int aFromLabel, int aToLabel)
	{
    	//read out the size of the mask
    	// vRegion is the size of our temporary window
    	int[] vRegion = m_SphereMaskForLocalEnergy.m_Size;

		// vOffset is basically the difference of the center and the start of the window
		Point vOffset = (new Point(vRegion)).div(2);
			
		Point start = aCenterIndex.sub(vOffset);				// "upper left point"
		
		RegionIterator vLabelIt = new RegionIterator(labelImage.dimensions, vRegion, start.x);
		RegionIterator vDataIt = new RegionIterator(labelImage.dimensions, vRegion, start.x);
		RegionIteratorMask vMaskIt = new RegionIteratorMask(labelImage.dimensions, vRegion, start.x);

		double vSumFrom = -aValue; // we ignore the value of the center point
		double vSumTo = 0;
		double vSumOfSqFrom = -aValue * aValue; // ignore the value of the center point.
		double vSumOfSqTo = 0.0;
		int vNFrom = -1;
		int vNTo = 0;

		while(vLabelIt.hasNext())
		{
			int labelIdx = vLabelIt.next();
			int dataIdx = vDataIt.next();
			int maskIdx = vMaskIt.next();
			
//			IndexIterator iii = new IndexIterator(m_LabelImage.dimensions);
//			System.out.println(iii.indexToPoint(labelIdx));
			
//			System.out.println(labelIdx+" "+maskIdx);
			
			if(m_SphereMaskForLocalEnergy.isInMask(maskIdx))
			{
				int absLabel=labelImage.getLabelAbs(labelIdx);
				if(absLabel == aFromLabel)
				{
					double data = getIntensity(dataIdx);
					vSumFrom += data;
					vSumOfSqFrom += data*data;
					vNFrom++;
				}
				else if(absLabel == aToLabel)
				{
					double data = getIntensity(dataIdx);
					vSumTo += data;
					vSumOfSqTo += data*data;
					vNTo++;
				}
			}
		}

		double vMeanTo;
		double vVarTo;
		double vMeanFrom;
		double vVarFrom;
		if(vNTo == 0) // this should only happen with the BG label
		{
			LabelInformation info = labelMap.get(aToLabel);
			vMeanTo = info.mean;
			vVarTo = info.var;
		}
		else
		{
			vMeanTo = vSumTo / vNTo;
// vVarTo = (vSumOfSqTo - vSumTo * vSumTo / vNTo) / (vNTo - 1);
			vVarTo = (vSumOfSqTo - vSumTo*vSumTo/vNTo) / (vNTo);
		}

		if(vNFrom == 0)
		{
			LabelInformation info = labelMap.get(aFromLabel);
			vMeanFrom = info.mean;
			vVarFrom = info.var;
		}
		else
		{
			vMeanFrom = vSumFrom / vNFrom;
			vVarFrom = (vSumOfSqFrom - vSumFrom * vSumFrom / vNFrom) / (vNFrom);
		}

		// double vVarFrom = (vSumOfSqFrom - vSumFrom * vSumFrom / vNFrom) / (vNFrom - 1);

		boolean vMerge = false;
		if(aFromLabel != bgLabel && aToLabel != bgLabel)
		{
			if(CalculateKLMergingCriterion(vMeanFrom, vMeanTo, vVarFrom, vVarTo, vNFrom, vNTo) < settings.m_RegionMergingThreshold)
			{
				vMerge = true;
			}
		}
// if(isnan(aValue) || isnan(vMeanTo) || isnan(vMeanFrom) || )
		// return abs(aValue - vMeanTo) - abs(aValue - vMeanFrom); doesnt work

		double vEnergyDiff = (aValue - vMeanTo) * (aValue - vMeanTo) - (aValue - vMeanFrom) * (aValue - vMeanFrom);

		return new Pair<Double, Boolean>(vEnergyDiff, vMerge);
    }
	
	
	
	
	double CalculateMSEnergyDifference(float aValue, int fromLabel, int toLabel)
	{
		LabelInformation to = labelMap.get(toLabel);
		LabelInformation from = labelMap.get(fromLabel);

		double aNewToMean = (to.mean*to.count + aValue)/(to.count+1);
// if (aFrom.var <= 0 || aTo.var <= 0) {
// assert("Region has negative variance.");
// }

		double M_PI = Math.PI;

		return ( (aValue-aNewToMean)*(aValue-aNewToMean)/(2.0*to.var) 
				+ 0.5 * Math.log(2.0*M_PI*to.var) 
				- ( (aValue-from.mean)*(aValue-from.mean)/(2.0*from.var) + 0.5*Math.log(2.0*M_PI*from.var) ) );
	}
	
	

//    template <class TInputImage, class TInitImage, class TOutputImage >
//    inline
//    typename FrontsCompetitionImageFilter<TInputImage, TInitImage, TOutputImage>::
//    EnergyDifferenceType
//    FrontsCompetitionImageFilter<TInputImage, TInitImage, TOutputImage>::
//    CalculateCurvatureBasedGradientFlow(
//    InputImagePointerType aDataImage,
//    LabelImagePointerType aLabelImage,
	//    ContourIndexType aIndex,
	//    unsigned int aFrom, unsigned int aTo) {
		

		
	
double CalculateCurvatureBasedGradientFlow(LabelImage aLabelImage, 
		Point aIndex, int aFrom, int aTo) 
{
//	SphereBitmapImageSource sphereMaskIterator = new SphereBitmapImageSource(dim, aLabelImage);
	
	//TODO Z finishing: take faster one
	double result;
	
	if(MVC.userDialog.useOldRegionIterator())
	{
		result = sphereMaskIterator.GenerateData(aIndex, aFrom, aTo);
	}
	else
	{
		result = sphereMaskIterator.GenerateData2(aIndex, aFrom, aTo);
	}
	
	return result;

}
	
	
	
	private double SumAllEnergies(HashMap<Point, ContourParticle> aContainer) 
	{
	
	    double vTotalEnergyDiff = 0;
	    
	    for (ContourParticle vPointIterator : aContainer.values()) {
	        vTotalEnergyDiff += vPointIterator.energyDifference;
	    }
	    return vTotalEnergyDiff;
	}


	/**
	 * m_InnerContourContainer.remove(vCurrentCIndex);
	 */
	void ForestFire(LabelImage aLabelImage, Point aIndex, MultipleThresholdImageFunction aMultiThsFunctionPtr, int aNewLabel)
	{
		
//		displaySlice("pre forestfire");

		//Set<Integer> vVisitedNewLabels = new HashSet<Integer>();
		Set<Integer> vVisitedOldLabels = new HashSet<Integer>();

		FloodFill ff = new FloodFill(aLabelImage, aMultiThsFunctionPtr, aIndex);
		Iterator<Point> vLit = ff.iterator();

		Set<Point> vSetOfAncientContourIndices = new HashSet<Point>(); // ContourIndexType

		double vSum = 0;
		double vSqSum = 0;
		int vN = 0;
//		double vLengthEnergy = 0;

        while(vLit.hasNext())
        {
			// InputPixelType vImageValue = vDit.Get();
			Point vCurrentIndex = vLit.next();
			int vLabelValue = labelImage.getLabel(vCurrentIndex);
			int absLabel = labelImage.labelToAbs(vLabelValue);
			float vImageValue = getIntensity(vCurrentIndex);
			
			// the visited labels statistics will be removed later.
			vVisitedOldLabels.add(absLabel);
			
			labelImage.setLabel(vCurrentIndex, aNewLabel);
			
			if(labelImage.isContourLabel(vLabelValue)) 
			{
				// m_InnerContourContainer[static_cast<ContourIndexType>(vLit.GetIndex())].first = vNewLabel;
				// ContourIndexType vCurrentIndex = static_cast<ContourIndexType>(vLit.GetIndex());
				
				vSetOfAncientContourIndices.add(vCurrentIndex);
				
			}

			vN++;
			vSum += vImageValue;
			vSqSum += vImageValue * vImageValue;

		} // while iterating over floodfill area


        /// Delete the contour points that are not needed anymore:
        for(Point vCurrentCIndex: vSetOfAncientContourIndices) 
        {
            if (labelImage.isBoundaryPoint(vCurrentCIndex)) 
            {
            	ContourParticle vPoint = m_InnerContourContainer.get(vCurrentCIndex);
            	vPoint.label = aNewLabel;
//	 			vPoint.modifiedCounter = 0;

//						vLengthEnergy += m_ContourLengthFunction->EvaluateAtIndex(vCurrentCIndex);
            	labelImage.setLabel(vCurrentCIndex, labelImage.labelToNeg(aNewLabel));
            } else {
                m_InnerContourContainer.remove(vCurrentCIndex);
            }
        }

        /// Store the statistics of the new region (the vectors will
        /// store more and more trash of old regions).
		double vN_ = vN;

		// create a labelInformation for the new label, add to container
		LabelInformation newLabelInformation = new LabelInformation(aNewLabel);
		labelMap.put(aNewLabel, newLabelInformation);

		newLabelInformation.mean = vSum / vN_;
		// TODO m_Intensities[vNewLabel] = m_Means[vNewLabel];
		double var = (vN_>1)?(vSqSum - vSum * vSum / vN_) / (vN_ - 1) : 0;
		newLabelInformation.setVar(var);
		newLabelInformation.count = vN;
		// TODO m_Lengths[vNewLabel] = vLengthEnergy;

//		displaySlice("after forestfire");
		
		/// Clean up the statistics of non valid regions.
		for(int vVisitedIt : vVisitedOldLabels) 
		{
//			debug("Freed label " + vVisitedIt);
			FreeLabelStatistics(vVisitedIt);
		}
		
		CleanUp();
		
		/// TODO: this must as well be only performed for the affected
		///       regions! A call to this function may result in segmentation
		///       faults since the labelimage is not 'consistent': It may contain
		///       still regions with 'old' labels. This happens if the 'old'
		///       region has 2 topological changes at once.
		//        if (m_EnergyFunctional == e_Deconvolution) {
		//            RenewDeconvolutionStatistics(m_LabelImage, this->GetDataInput());
		//        }
		
	}

	
	// calls: cleanup
	void FreeLabelStatistics(Iterator<Entry<Integer, LabelInformation>> vActiveLabelsIt) 
	{
//		in caller: labelDispenser.addFreedUpLabel(entry.getKey());
		vActiveLabelsIt.remove();
	}
	
	// calls: doOne.Iterate.RelabelRegions.ForestFire.foo
	void FreeLabelStatistics(int vVisitedIt) 
	{
		if(vVisitedIt==bgLabel)
		{
//TODO this is debug only
			debug("LabelImage.FreeLabelStatistics()");
			throw new RuntimeException("free bglabel in FreeLabelStatistics()");
		}
		labelMap.remove(vVisitedIt);
		labelDispenser.addFreedUpLabel(vVisitedIt);

		//        m_Means.erase(aLabelAbs);
		//        m_Variances.erase(aLabelAbs);
		//        m_Count.erase(aLabelAbs);
		//        m_Lengths.erase(aLabelAbs);
		//        m_Intensities.erase(aLabelAbs);
		///m_BBoxes.erase(aLabelAbs);
	}


	private void UpdateStatisticsWhenJump(Entry<Point,ContourParticle> aParticle, 
			int aFromLabelIdx, int aToLabelIdx)
	{
		Point vCurrentIndex = aParticle.getKey();
		ContourParticle vOCCV = aParticle.getValue();
		float vCurrentImageValue = vOCCV.intensity;

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
		
		//TODO divide by zero. why does this not happen at itk?
		double vNewMeanFromLabel;
		
		if(vNFrom>1)
		{
			vNewMeanFromLabel = (vNFrom * aFromLabel.mean - vCurrentImageValue) / (vNFrom - 1.0);
		}
		else
		{
			vNewMeanFromLabel = 0.0;
		}

        /// Calculate the new variances:
		double var;
		var = ((1.0 / (vNTo)) * (
				vToLabelSumOfSq + vCurrentImageValue * vCurrentImageValue 
				- 2.0 * vNewMeanToLabel * (aToLabel.mean * vNTo + vCurrentImageValue)
				+ (vNTo + 1.0) * vNewMeanToLabel*vNewMeanToLabel));
		aToLabel.setVar(var);

		if(vNFrom==2)
		{
			var=0.0;
		}
		else
		{
			var = ( 1.0/(vNFrom - 2.0) ) * (
					vFromLabelSumOfSq - vCurrentImageValue * vCurrentImageValue 
					- 2.0 * vNewMeanFromLabel * (aFromLabel.mean * vNFrom - vCurrentImageValue) 
					+ (vNFrom - 1.0) * vNewMeanFromLabel*vNewMeanFromLabel);
			
		}
		
		aFromLabel.setVar(var);

		// / Update the means:
		aToLabel.mean = vNewMeanToLabel;
		aFromLabel.mean = vNewMeanFromLabel;
		// m_Means[0] = 0;
		// m_Means[1] = 1.0403;
		// m_Means[2] = 1.0403;

		// / Add a sample point to the BG and remove it from the label-region:
		aToLabel.count++;
		aFromLabel.count--;

        /// Update the lengths for each region:
        /// ContourLengthFunction gives back the overall length change when changing
        /// aFromLabel to aToLabel. This change applies to both regions.

        //TODO  m_ContourLengthFunction
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
			if(vLabelAbs == bgLabel) {
				continue;
			}

			if (vActiveLabelsIt.getValue().count <= settings.m_AreaThreshold) {// &&
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
		/// Get the contour points of that label and copy them to vContainer:
		HashMap<Point, ContourParticle> vContainer = new HashMap<Point, ContourParticle>();

		// find all the element with this label and store them in a tentative
		// container:
		for(Entry<Point, ContourParticle> vIt : m_InnerContourContainer.entrySet()) 
		{
			if(vIt.getValue().label == aLabel) {
				vContainer.put(vIt.getKey(), vIt.getValue());
			}
		}

        /// Successively remove the points from the contour. In the end of the
        /// loop, new points are added to vContainer.
        while(!vContainer.isEmpty()) 
        {
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

            //TODO ??? why should i do that? answer: ogres are like onions
            //Refill the container
            if (labelMap.get(aLabel).count > 0) 
            {
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
	
	private void CleanUp()
	{
		// UIntStatisticsContainerType::iterator vActiveLabelsIt = m_Count.begin();
		//TODO ConcurrentModificationException
		
		Iterator<Entry<Integer, LabelInformation>> vActiveLabelsIt = labelMap.entrySet().iterator();
		
		while(vActiveLabelsIt.hasNext()) 
		{
			Entry<Integer, LabelInformation> entry = vActiveLabelsIt.next();
			
			
			if(entry.getValue().count == 0) 
			{
				if(entry.getKey()==bgLabel){
					debug("bglabel in"); System.out.println("LabelImage.CleanUp()");
					continue;
//					throw new RuntimeException("tried to remove bglabel in cleanUp()");
				}
				labelDispenser.addFreedUpLabel(entry.getKey());
				FreeLabelStatistics(vActiveLabelsIt);
			}
		}

	}

	
	
	
	
	float getIntensity(int idx)
	{
		return dataIntensity[idx];
	}
	
	float getIntensity(Point p)
	{
		int idx = iterator.pointToIndex(p);
		return dataIntensity[idx];
	}
	
	

	
	static void debug(Object s)
	{
		System.out.println(s);
	}
	
	
	
	// Control //////////////////////////////////////////////////
	

	Object pauseMonitor = new Object();
	boolean pause = false;
	boolean abort = false;
	/**
	 * Stops the algorithm after actual iteration
	 */
	public void stop()
	{
		synchronized(pauseMonitor)
		{
			abort = true;
			pause = false; 
			pauseMonitor.notify();
		}
	}
	
	public void pause()
	{
		pause=true;
	}
	
	public void resume()
	{
		synchronized(pauseMonitor)
		{
			pause = false;
			pauseMonitor.notify();
		}
	}
	
	///////////////////////////////////////////
//	
//	int getLabelAbs(Point p)
//	{
//		int idx = iterator.pointToIndex(p);
//		return Math.abs(dataLabel[idx]);
//	}
//
//	int getLabelAbs(int idx)
//	{
//		return Math.abs(dataLabel[idx]);
//	}
//
//
//	int getLabel(int idx)
//	{
//		return dataLabel[idx];
////		return labelIP.get(index);
//	}
//	
//	/**
//	 * @param p
//	 * @return Returns the (raw; contour information) value of the LabelImage at Point p. 
//	 */
//	int getLabel(Point p) {
//		int idx = iterator.pointToIndex(p);
//		return (dataLabel[idx]);
//	}
//
//	/**
//	 * sets the labelImage to val at point x,y
//	 */
//	void setLabel(int idx, int label) 
//	{
//		dataLabel[idx]=label;
////		dataLabel[index]=(short)val;
////		labelIP.set(index, val);
//	}
//
//	/**
//	 * sets the labelImage to val at Point p
//	 */
//	void setLabel(Point p, int label) {
//		int idx = iterator.pointToIndex(p);
//		dataLabel[idx]=label;
//	}

	
	
	
	
	
	
	
	
}




