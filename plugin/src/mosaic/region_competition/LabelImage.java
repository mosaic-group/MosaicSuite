package mosaic.region_competition;
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

import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
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
	
	private Region_Competition MVC; 	/** interface to image program */
	private LabelImage m_LabelImage; 	// == this, exists for easier refactoring
	
	ImagePlus imageIP;	// input image
	ImageProcessor imageProc;
	
	private final float imageMax; 	// maximal intensity of input image
	
	ImageProcessor labelIP;				// map positions -> labels
	
	int dim;			// number of dimension
	int[] dimensions;	// dimensions (width, height, depth, ...)
	private int width;	// TODO multidim
	private int height;
	IndexIterator iterator; // iterates over the labelImage
	
	
	final int forbiddenLabel=Short.MAX_VALUE;
	final int bgLabel = 0;
	final int negOfs=10000;			// labels above this number stands for "negative numbers" (problem with displaying negative numbers in ij.ShortProcessor)
	
	// data structures
	
	/** stores the contour particles. access via coordinates */
	private HashMap<Point, ContourParticle> m_InnerContourContainer;
	
	private HashMap<Point, ContourParticle> m_Candidates;
	
	/** Maps the label(-number) to the information of a label */
	private HashMap<Integer, LabelInformation> labelMap;

	private HashMap<Point, Pair<Integer, Integer>> m_CompetingRegionsMap;
	
	
	private Connectivity connFG;
	private Connectivity connBG;
	private TopologicalNumberImageFunction m_TopologicalNumberFunction;

	private List<Integer> m_MergingHist;
	private List<Integer> m_FramesHist;
	
	Set<Pair<Point, Integer>> m_Seeds = new HashSet<Pair<Point, Integer>>();
	
	/**
	 * creates a new LabelImage with size of ip
	 * @param ip is saved as originalIP
	 */
	public LabelImage(Region_Competition region_competition) 
	{
		m_LabelImage = this;
		
		MVC=region_competition;
		settings=MVC.settings;
		ImagePlus ip = MVC.getOriginalImPlus();
		imageIP=ip;
		imageProc=ip.getProcessor();
		imageMax=(float)imageIP.getStatistics().max;
		
		dim = ip.getNDimensions();
		dimensions = new int[dim];
		for(int i=0; i<dim; i++)
		{
			//TODO does this work for n-dim?
			dimensions[i]=ip.getDimensions()[i];
		}
		
		iterator = new IndexIterator(dimensions);
		
		
		//TODO multidim
		width=ip.getWidth();
		height=ip.getHeight();
		
		// TODO does init twice if guess loaded from file
		labelIP = new ShortProcessor(width, height);
		
		//TODO initial capacities
		m_InnerContourContainer = new HashMap<Point, ContourParticle>();
		m_Candidates = new HashMap<Point, ContourParticle>();
		labelMap = new HashMap<Integer, LabelInformation>();
		m_CompetingRegionsMap = new HashMap<Point, Pair<Integer, Integer>>();
		
		m_MergingHist = new LinkedList<Integer>();
		m_FramesHist = new LinkedList<Integer>();
		
		initMembers();
		initConnectivities(dim);
	}
	
	
	private void initConnectivities(int d)
	{
		//TODO generic
		connFG = new Connectivity(d, d-1);
		connBG = new Connectivity(d, d-2);
		m_TopologicalNumberFunction = new TopologicalNumberImageFunction(this, connFG, connBG);
	}


//	public void setStack(ImageStack stack)
//	{
//		this.stack=stack;
//	}
	
	//////////////////////////////////////////////////
	
	
	public Settings settings;
	
    boolean m_converged;
    float m_AcceptedPointsFactor;
    int m_iteration_counter; // member for debugging
    
    int m_OscillationsNumberHist[];
    double m_OscillationsEnergyHist[];
    
    EnergyFunctionalType m_EnergyFunctional;
    int m_OscillationHistoryLength;

	public int m_MaxNLabels;
    
	SphereBitmapImageSource sphereMaskIterator; // regularization
	
	SphereBitmapImageSource m_SphereMaskForLocalEnergy;
	int m_GaussPSEnergyRadius;
    
	// ///////////////////////////////////////////////////

    void initMembers()
    {
//    	settings = new Settings();
    	sphereMaskIterator = new SphereBitmapImageSource(m_LabelImage, (int)settings.m_CurvatureMaskRadius, 2*(int)settings.m_CurvatureMaskRadius+1);
    	m_GaussPSEnergyRadius=8;
    	m_SphereMaskForLocalEnergy = new SphereBitmapImageSource(m_LabelImage, m_GaussPSEnergyRadius, 2*m_GaussPSEnergyRadius+1);

    	
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
		
		m_MaxNLabels=0;
    }
	
	
    /**
     * Initializes label image data to all zero
     */
	public void initZero()
	{
		int size=1;
		for(int d=0; d<dim; d++)
		{
			size*=dimensions[d];
		}
		
		for(int i=0; i<size; i++)
		{
			labelIP.set(i, 0);
		}
	}
	
	/**
	 * Initializes label image with a predefined IP (by copying it)
	 * ip: without contour pixels/boundary
	 * (invoke initBoundary() and generateContour();
	 */
	public void initWithImageProc(ImageProcessor ip)
	{
		//TODO check for dimensions etc
		this.labelIP=ip.duplicate();
	}
	
	public void initWithIP(ImagePlus ip)
	{
		initWithImageProc(ip.getProcessor());
	}
	
	/**
	 * sets the outermost pixels of the labelimage to the forbidden label
	 */
	public void initBoundary()
	{
		
		//TODO multidim
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
		Roi vRectangleROI = new Roi(10, 10, imageIP.getWidth()-20, imageIP.getHeight()-20);
		labelIP.setValue(1);
		labelIP.fill(vRectangleROI);
	}
	
	/**
	 * creates an initial guess (of the size r*labelImageSize)
	 * @param r fraction of sizes of the guess
	 */
	public void initialGuessGrowing(double r)
	{
		int w = (int)(r*width);
		int h = (int)(r*height);
		int x = (width-w)/2;
		int y = (height-h)/2;
		
		Roi vRectangleROI = new Roi(x, y, w, h);
		labelIP.setValue(1);
		labelIP.fill(vRectangleROI);
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
	 * creates an initial guess from an array of ellipses. 
	 * @param ellipses array of ellipses
	 */
	public void initialGuessEllipses(int ellipses[][])
	{
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
			labelIP.setValue(label);
			labelIP.fill(roi);

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
		
		int bubbleIndex = 0;
		
		IndexIterator it = new IndexIterator(grid);
		for(Point ofs:it.getPointIterable())
		{
			System.out.println(ofs);
			ofs=ofs.mult(displ).add(gapPoint); // left upper startpoint of a bubble+spacing
//			RegionIterator rit = new RegionIterator(m_LabelImage.dimensions, region, ofs.x);
			
			BubbleDrawer bd = new BubbleDrawer(m_LabelImage, rad, 2*rad);
			bubbleIndex++;
			bd.doSphereIteration(ofs, bubbleIndex);
		}
	}
	
	/**
	 * compute the statistics for each region, 
	 * stores them in labelMap for the corresponding label
	 */
	public void computeStatistics()
	{
		clearStats();
		
		int size = iterator.getSize();
		for(int i=0; i<size; i++)
		{
			int absLabel = getAbs(i);

			if (absLabel != bgLabel && absLabel != forbiddenLabel) 
			{
				LabelInformation stats = labelMap.get(absLabel);
				if(stats==null)
				{
//					labelMap.put(absLabel, new LabelInformation(absLabel));
//					stats = labelMap.get(absLabel);

					stats = new LabelInformation(absLabel);
					labelMap.put(absLabel, stats);
				}
				stats.add(getIntensity(i));
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
		
		m_MaxNLabels = 0;
	}
	
	
	/**
	 * as computeStatistics, does not use iterative approach
	 * (first computes sum of values and sum of values^2)
	 */
	public void renewStatistics()
	{
		clearStats();
		
		int size = iterator.getSize();
		for(int i=0; i<size; i++)
		{
//			int label = get(x, y);
//			int absLabel = labelToAbs(label);
			
			int absLabel= getAbs(i);

			if (absLabel != forbiddenLabel /* && absLabel != bgLabel*/) 
			{
				if(absLabel > m_MaxNLabels)
				{
					m_MaxNLabels = absLabel;
				}
				
				//TODO save the last label, the next label will be likely the same label, 
				// so you can save one map lookup (by the cost of 1 integer comparison)
				
				LabelInformation stats = labelMap.get(absLabel);
				if(stats==null)
				{
					stats = new LabelInformation(absLabel);
					labelMap.put(absLabel, stats);
				}
				float val = getIntensity(i);
				stats.count++;
				
				stats.mean+=val; // only sum up, mean and var are computed below
				stats.setVar(stats.var+val*val);
			}
		}

		
		// now we have in all LabelInformation: 
		// in mean the sum of the values, in var the sum of val^2
		for(LabelInformation stat: labelMap.values())
		{
			int n= stat.count;
            if (n > 1) {
            	double var = (stat.var - stat.mean*stat.mean / n) / (n-1);
            	stat.setVar(var);
//                stat.var = (stat.var - stat.mean*stat.mean / n) / (n-1);
            } else {
                stat.var = 0;
            }
            stat.mean = stat.mean/n;
		}
		
        m_MaxNLabels++; // this number points to the a free label.
		
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
	
	
	/**
	 * marks the contour of each region
	 * stores the contour particles in contourContainer
	 */
	public void generateContour()
	{
		//TODO which connectivity?!
		Connectivity conn = connFG;
		
		// finds and saves contour particles
		
		//TODO multidim
		for(int y = 1; y < height-1; y++)
		{
			for(int x = 1; x < width-1; x++)
			{
				int label=get(x, y);
				if(label!=bgLabel && label!=forbiddenLabel) // region pixel
					// && label<negOfs
				{
					
					int xs[]={x,y};
					Point p = new Point(xs);
					for(Point neighbor : conn.itNeighborsOf(p))
					{
						int neighborLabel=get(neighbor);
						if(neighborLabel!=label)
						{
							ContourParticle particle = new ContourParticle();
							particle.label=label;
							particle.intensity=getIntensity(x, y);
							m_InnerContourContainer.put(p, particle);
							
							break;
						}
					}
					
				} // if region pixel
			}
		}
		
		// set contour to -label
		for(Entry<Point, ContourParticle> entry:m_InnerContourContainer.entrySet())
		{
			Point key = entry.getKey();
			ContourParticle value = entry.getValue();
			//TODO cannot set neg values to ShortProcessor
			set(key, labelToNeg(value.label));
		}
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

		while(settings.m_MaxNbIterations > m_iteration_counter && !(vConvergence) && !abort) 
		{
			m_iteration_counter++;
			debug("=== iteration " + m_iteration_counter+" ===");

			// std::cout << "number of points: "<<m_InnerContourContainer.size() << std::endl;
			// m_ManyPointsToBeAdded.clear();
			// m_ManyPointsToBeDeleted.clear();
			vConvergence = DoOneIteration();

			displaySlice("iteration " + m_iteration_counter);
//			MVC.addSliceToStackAndShow("iteration " + m_iteration_counter,
//					this.labelIP.getPixelsCopy());

		}
		
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

		vConvergenceA = IterateContourContainerAndAdd();
		CleanUp();

		return vConvergenceA;
	}
	
	
	private void MinimizeEnergy()
	{
		
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
					}
				}
				if(vSimple) 
				{
					vChange = true;
					ChangeContourPointLabelToCandidateLabel(vStoreIt);
					vPointIterator.remove();
					vConvergence = false;
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
	
				// int vTopoNb;
				boolean vValidPoint = true;
	
				vFGTNvector = (m_TopologicalNumberFunction.EvaluateAdjacentRegionsFGTNAtIndex(vCurrentIndex));
//	                FGTNIteratorType vTopoNbItr;
//	                FGTNIteratorType vTopoNbItrEnd = vFGTNvector.end();

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
						if(settings.m_AllowFission) {
							
							RegisterSeedsAfterSplit(this, vCurrentIndex, vCurrentLabel, m_Candidates);

						} else {
							/// disallow the move.	
							vValidPoint = false;
						}
					}
				}
				
				if(!vValidPoint) {
					// TODO check
					vPointIterator.remove();
					// m_Candidates.erase(vStoreIt);
				}
				
                /// If the move doesn't change topology or is allowed (and registered
                /// as seed) to change the topology, perform the move (in the
                /// second iteration; in the first iteration seed points need to
                /// be collected):

				if (vValidPoint) {
					ChangeContourPointLabelToCandidateLabel(e);
					vConvergence = false;
					
					if(e.getValue().m_processed) {
						
						RegisterSeedsAfterSplit(this, vCurrentIndex, vCandidateLabel, m_Candidates);
						m_Seeds.remove(new Pair<Point,Integer>(vCurrentIndex, vCurrentLabel));
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
//			LabelInformation info = labelMap.get(vSeedIt.second);
//			if(info == null) {
//				// TODO !!! Seed label not found
//				// This might happen, if there are multiple seeds for a split. The region was then 
//				// relabeled in the first seed, and the label of the next seeds are not found anymore. 
////				displaySlice("label not found for point "+ vSeedIt.first+ " label "+vSeedIt.second);
//				debug("label not found for point "+ vSeedIt.first+ " label "+vSeedIt.second);
//				continue;
//			}
//			int count  = info.count;
//			if(count > 1) //WRONG!
			{
				RelabelRegionsAfterSplit(this, vSeedIt.first, vSeedIt.second);
				vSplit = true;
				vNSplits++;
			}
		}
		        
        /// Merge the the competing regions if they meet merging criterion.
		if(settings.m_UseRegionCompetition && settings.m_AllowFusion) 
		{
			
			for(Entry<Point, Pair<Integer, Integer>> vCRit : m_CompetingRegionsMap.entrySet()) 
			{
//TODO !!!! m_CompetingRegionsMap is always empty?
				
				Point idx = vCRit.getKey();
				Pair<Integer, Integer> labels= vCRit.getValue();
				
				int vLabel1 = labels.first;
				int vLabel2 = labels.second;
                
                //TODO ist das debug?
				m_MergingHist.add(m_iteration_counter);
				m_MergingHist.add(vLabel1);
				m_MergingHist.add(vLabel2);
				
//TODO !!!!! funktion aus itk uebernehmen
//				RelabelAdjacentRegionsAfterToplogicalChange(m_LabelImage,idx, vLabel1, vLabel2);
				
				RelabelRegionsAfterFusion(m_LabelImage, idx, vLabel1);
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

	private void RegisterSeedsAfterSplit(LabelImage aLabelImage, Point aIndex, int aLabel, 
			HashMap<Point, ContourParticle> aCandidateContainer) {


		for (Point vSeedIndex : connFG.itNeighborsOf(aIndex)) {
			int vLabel = getAbs(vSeedIndex);

			if (vLabel == aLabel) {
				Pair<Point, Integer> vSeed = new Pair<Point, Integer>(vSeedIndex, vLabel);

				if(vLabel == 0)
					System.out.println("here");
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
			for(Point q : conn.itNeighborsOf(vCurrentIndex)) 
			{
				int vLabelOfDefender = getAbs(q);
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
									Pair<Integer, Integer> pair;
									if(L1<L2)
										pair = new Pair<Integer, Integer>(L1, L2);
									else
										pair = new Pair<Integer, Integer>(L2, L1);

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
	
        //        std::stringstream vSS3;
        //        vSS3 << "contourPointsAfterNetworkingStuffAtIt_" << (m_iteration_counter + 10000) << ".txt";
        //        WriteContourPointContainer(vSS3.str().c_str(), vAllCandidates);

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
        /// therefor the negative label value is set.
		//TODO sts is this true? what about enclosed pixels? 
		// ie first was like a 'U', then 'O' and now the hole is filled, then it is an inner point
		// where is this handled?
        ///
		set(vCurrentIndex, labelToNeg(vToLabel));
		
//TODO sts inserted following lines
		if(labelToAbs(vToLabel)!=vToLabel)
		{
			debug("changed label to absolute value. is this valid?\n");
			second.label=labelToAbs(vToLabel);
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
		for(Point qIndex:conn.itNeighborsOf(pIndex))
		{
			int qLabel = get(qIndex);
			
			//TODO can the labels be negative? somewhere, they are set (maybe only temporary) to neg values
			if(isContourLabel(aAbsLabel))
			{
				debug("AddNeighborsAtRemove. one label is not absLabel\n");
				int dummy=0;
				dummy=dummy+0;
			}
			
			if(isInnerLabel(qLabel) && qLabel==aAbsLabel) // q is a inner point with the same label as p
			{
				ContourParticle q = new ContourParticle();
				q.label=aAbsLabel;
				q.candidateLabel=bgLabel;
				q.intensity = getIntensity(qIndex);
				
				set(qIndex, labelToNeg(aAbsLabel));
				m_InnerContourContainer.put(qIndex, q);
			}
			//TODO this never can be true 
			// (since isContourLabel==> neg values AND (qLabel == aAbsLabel) => pos labels
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
		int aLabelNeg = labelToNeg(aLabelAbs);
		
        // itk 1646: we set the pixel value already to ensure the that the 'enclosed' check
        // afterwards works.
		//TODO is p.label (always) the correct label?
		set(pIndex, labelToNeg(aLabelAbs));
		
		Connectivity conn = connBG;
		for(Point qIndex: conn.itNeighborsOf(pIndex))
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
	
	/**
	 * The function relabels a region starting from the position aIndex.
	 * This method assumes the label image to be updated. It is used to relabel
	 * a region that was split by another region (maybe BG region).
	 */
	void RelabelRegionsAfterSplit(LabelImage aLabelImage, Point aIndex, int aLabel)
	{
		System.out.println("Split at " + aIndex.toString() + " of label " + aLabel);
		// template <class TInputImage, class TInitImage, class TOutputImage >
		if(getAbs(aIndex)==aLabel) {
			MultipleThresholdImageFunction vMultiThsFunction = new MultipleThresholdImageFunction(aLabelImage);
			vMultiThsFunction.AddThresholdBetween(aLabel, aLabel);
			int negLabel = labelToNeg(aLabel);
			vMultiThsFunction.AddThresholdBetween(negLabel, negLabel);

			ForestFire(aLabelImage, aIndex, vMultiThsFunction, m_MaxNLabels++);
		}
	}


	/// The function relabels 2 neighboring regions. These 2 regions are
	/// fused into a new region. Both regions are at position of aIndex
	/// or adjacent to it.
	/// Relabel2AdjacentRegionsAfterToplogicalChange expects the label image to
	/// be updated already: both methods are connected via the seedpoint. This
	/// method may be used to fuse 2 regions in the region competition mode.
	void RelabelRegionsAfterFusion(LabelImage aLabelImage, Point aIndex, int aL1) 
	{
		System.out.println("Relabel after fusion at " + aIndex.toString() + " of label " + aL1);
		int aL1Neg = labelToNeg(aL1);

		MultipleThresholdImageFunction vMultiThsFunction = new MultipleThresholdImageFunction(aLabelImage);
		
		LinkedList<Integer> vLabelsToCheck = new LinkedList<Integer>();
		Set<Integer> vCheckedLabels = new TreeSet<Integer>();
		
		vLabelsToCheck.addLast(aL1);
	    vMultiThsFunction.AddThresholdBetween(aL1, aL1);
	    vMultiThsFunction.AddThresholdBetween(aL1Neg, aL1Neg);
	    vCheckedLabels.add(aL1);		
	    
	    for(int vLabelToCheck : vLabelsToCheck) {
	    	for(Pair<Integer,Integer> vMergingLabelsPair : m_CompetingRegionsMap.values()) {
	    		int vLabel1 = vMergingLabelsPair.first;
	    		int vLabel2 = vMergingLabelsPair.second;
	    		
	    		if(vLabel1 == vLabelToCheck && !vCheckedLabels.contains(vLabel2)){
	    		    vMultiThsFunction.AddThresholdBetween(vLabel2, vLabel2);
	    		    vMultiThsFunction.AddThresholdBetween(labelToNeg(vLabel2), labelToNeg(vLabel2));
	    		    vCheckedLabels.add(vLabel2);
	    		    vLabelsToCheck.addLast(vLabel2);
	    		}
	    		if(vLabel2 == vLabelToCheck && !vCheckedLabels.contains(vLabel1)){
	    		    vMultiThsFunction.AddThresholdBetween(vLabel1, vLabel1);
	    		    vMultiThsFunction.AddThresholdBetween(labelToNeg(vLabel1), labelToNeg(vLabel1));
	    		    vCheckedLabels.add(vLabel1);
	    		    vLabelsToCheck.addLast(vLabel1);
	    		}
	    	}
	    }
		if(vMultiThsFunction.EvaluateAtIndex(aIndex)){
		    ForestFire(aLabelImage, aIndex, vMultiThsFunction, m_MaxNLabels++);
		}
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
				displaySlice("***info is null for: "+vIt.getKey());
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
		for(Point neighbor:conn.itNeighborsOf(pIndex))
		{
			int neighborLabel=get(neighbor);
			neighborLabel=labelToAbs(neighborLabel);
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
				vEnergy += m_EnergyRegionCoeff * 
				CalculateDeconvolutionPCEnergyDifference(m_LabelImage, aContourIndex,
                        vCurrentImageValue, vCurrentLabel, aToLabel);
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
		    			imageIP, m_LabelImage, aContourIndex,vCurrentLabel, aToLabel);
//		    	debug("eCurv="+eCurv+" vEnergy="+vEnergy);
		        vEnergy += //m_Means[aToLabel] * // m_Means[aToLabel] *
		        		m_EnergyContourLengthCoeff * eCurv;
		    } else {
		        vEnergy += m_EnergyContourLengthCoeff * CalculateCurvatureBasedGradientFlow(
		                imageIP, m_LabelImage, aContourIndex, vCurrentLabel, aToLabel);
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
	
	private double CalculateDeconvolutionPCEnergyDifference(LabelImage aLabelImage, Point aIndex, double aValue, int aFromLabel, int aToLabel)
	{

		double vEnergyDiff = 0;
//
//        /**
//         * Do not really subtract / add the scaled PSF to the 'ideal image'.
//         * Example for the case where the energyDiff is calculated for a possible
//         * change to the background (removal):
//         * Calc (J' - I)^2  -  (J - I)^2
//         * = (J - (c-BG) * PSF - I)^2 - (J - I)^2
//         */
//
//        /// Define the region of influence:
//        InputImageRegionType vRegion;
//        InputImageSizeType vRegionSize = m_PSF->GetLargestPossibleRegion().GetSize();
//
//        InputImageOffsetType vOffset;
//        vOffset.Fill(vRegionSize[0] / 2); // we assume the region to be a hypercube.
//        vRegion.SetSize(vRegionSize);
//        vRegion.SetIndex(aIndex - vOffset);
//        //        std::cout << vRegion << std::endl;
//
//
//        /// Move the PSF window such that it is centered on aIndex
//        InputImageRegionType vPSFRegion = m_PSF->GetLargestPossibleRegion().GetSize();
//        vPSFRegion.SetIndex(vRegion.GetIndex());
//        m_PSF->SetBufferedRegion(vPSFRegion);
//        //        std::cout << vPSFRegion << std::endl;
//
//        /// After the cropping at the data-image boundaries, vRegion will be the
//        /// region to treat in the data-image space.
//        vRegion.Crop(m_DeconvolutionModelImage->GetBufferedRegion());
//        //        std::cout << vRegion << std::endl;
//
//        /// Iterate through the region and subtract the psf from the conv image.
//        ImageRegionConstIterator<InputImageType> vIt(m_DeconvolutionModelImage, vRegion);
//        ImageRegionConstIterator<InputImageType> vPSFIt(m_PSF, vRegion);
//        ImageRegionConstIterator<InputImageType> vDataIt(this->GetInput(), vRegion);
//        EnergyDifferenceType vEnergyDiff = 0;
//
//        InternalPixelType vIntensity_FromLabel = m_Intensities[aFromLabel];
//        InternalPixelType vIntensity_ToLabel = m_Intensities[aToLabel];
//
//
//        for (vPSFIt.GoToBegin(), vIt.GoToBegin(), vDataIt.GoToBegin();
//                !vPSFIt.IsAtEnd();
//                ++vPSFIt, ++vIt, ++vDataIt) {
//            InternalPixelType vEOld = (vIt.Get() - vDataIt.Get());
//            vEOld = vEOld * vEOld;
//            //            vEOld = fabs(vEOld);
//            InternalPixelType vENew = (vIt.Get() - (vIntensity_FromLabel - vIntensity_ToLabel) *
//                    vPSFIt.Get()) - vDataIt.Get();
//            vENew = vENew * vENew;
//            //            vENew = fabs(vENew);
//            //                std::cout << vENew << std::endl;
//            vEnergyDiff += vENew - vEOld;
//        }
//        //        } else {
//        //            for (vPSFIt.GoToBegin(), vIt.GoToBegin(), vDataIt.GoToBegin();
//        //                    !vPSFIt.IsAtEnd();
//        //                    ++vPSFIt, ++vIt, ++vDataIt) {
//        //                double vEOld = (vIt.Get() - vDataIt.Get());
//        //                vEOld = vEOld * vEOld;
//        //                double vENew = (vIt.Get() + (m_Intensities[aToLabel] - m_Intensities[0]) *
//        //                        vPSFIt.Get()) - vDataIt.Get();
//        //                vENew = vENew * vENew;
//        //
//        //                vEnergyDiff += vENew - vEOld;
//        //            }
//        //        }

        return vEnergyDiff;
    }


	boolean CalculateMergingEnergyForLabel(int aLabelA, int aLabelB)
	{
		/// store this event to check afterwards if we should merge
		/// the 2 regions.

		if(aLabelA != bgLabel && aLabelB != bgLabel) // we are competeing.
		{ 
			/// test if merge should be performed:
			if(CalculateKLMergingCriterion(aLabelA, aLabelB) < settings.m_RegionMergingThreshold)
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

		double vDKL1 = (aMu1 - vMu12) * (aMu1 - vMu12) / (2.0f * vVar12) + 0.5f
				* (aVar1 / vVar12 - 1.0f - Math.log(aVar1 / vVar12));

		double vDKL2 = (aMu2 - vMu12) * (aMu2 - vMu12) / (2.0f * vVar12) + 0.5f
				* (aVar2 / vVar12 - 1.0f - Math.log(aVar2 / vVar12));

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
		
		RegionIterator vLabelIt = new RegionIterator(m_LabelImage.dimensions, vRegion, start.x);
		RegionIterator vDataIt = new RegionIterator(m_LabelImage.dimensions, vRegion, start.x);
		RegionIteratorMask vMaskIt = new RegionIteratorMask(m_LabelImage.dimensions, vRegion, start.x);

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
				int absLabel=getAbs(labelIdx);
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
		

		
	
double CalculateCurvatureBasedGradientFlow(ImagePlus aDataImage,
		LabelImage aLabelImage, Point aIndex, int aFrom, int aTo) 
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

//        Set<LabelAbsPixelType> LabelAbsPxSetType;
//		LabelAbsPxSetType vVisitedNewLabels;
//		LabelAbsPxSetType vVisitedOldLabels;
		
		//Set<Integer> vVisitedNewLabels = new HashSet<Integer>();
		Set<Integer> vVisitedOldLabels = new HashSet<Integer>();

//		displaySlice("forestfire um "+aIndex);
		
		//for(Point p : connFG.itNeighborsOf(aIndex)) 
		//{
			//int vLabel = get(p);

//			if(vLabel != 0 && vLabel != aLabelImage.forbiddenLabel 
//					&& !vVisitedNewLabels.contains(labelToAbs(vLabel))
//					&& aMultiThsFunctionPtr.EvaluateAtIndex(p)) 
//			{

				FloodFill ff = new FloodFill(this, aMultiThsFunctionPtr, aIndex);
				Iterator<Point> vLit = ff.iterator();


				Set<Point> vSetOfAncientContourIndices = new HashSet<Point>(); // ContourIndexType

				// profiling:
				// std::cout << "starting floodfilling..." << std::endl;

				double vSum = 0;
				double vSqSum = 0;
				int vN = 0;
				double vLengthEnergy = 0;

                while(vLit.hasNext())
                {
					// InputPixelType vImageValue = vDit.Get();
					Point vCurrentIndex = vLit.next();
					int vLabelValue = this.get(vCurrentIndex);
					int absLabel = labelToAbs(vLabelValue);
					float vImageValue = this.getIntensity(vCurrentIndex);

		
					
					//TODO !!! here be dragons! 
					/* may be eg label 1. and there might be a nonadjacent (to aIndex) region with label 1 
					 * (since there is another seed for it)
					 * this other label 1 region will NOT be relabeled. 
					 * but since label 1 is put into vVisitedOldLabels, 
					 * the LabelInformation for label 1 will get erased! 
					 * 
					 */
					
					
					// the visited labels statistics will be removed later.
					vVisitedOldLabels.add(absLabel);
					set(vCurrentIndex, aNewLabel);
					
					
					if(isContourLabel(vLabelValue)) 
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
                    if (isBoundaryPoint(vCurrentCIndex)) 
                    {
                    	ContourParticle vPoint = m_InnerContourContainer.get(vCurrentCIndex);
                    	vPoint.label = aNewLabel;
//	 					vPoint.modifiedCounter = 0;

//						vLengthEnergy += m_ContourLengthFunction->EvaluateAtIndex(vCurrentCIndex);
                        set(vCurrentCIndex, labelToNeg(aNewLabel));
                    } else {
                        m_InnerContourContainer.remove(vCurrentCIndex);
//                        m_LabelImage->SetPixel(vCurrentCIndex, vNewLabel);
//                        vLit.Set(vNewLabel);
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
				// TODO !! vN_ can be 1;
				double var = (vN_>1)?(vSqSum - vSum * vSum / vN_) / (vN_ - 1):0;
				newLabelInformation.setVar(var);
//				newLabelInformation.var = var;
				newLabelInformation.count = vN;
				// TODO m_Lengths[vNewLabel] = vLengthEnergy;

			//} // if neighbor label ok
			

		
//		displaySlice("after forestfire");
		
		/// Clean up the statistics of non valid regions.
		for(int vVisitedIt : vVisitedOldLabels) 
		{
			System.out.println("Freed label " + vVisitedIt);
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

	/**
	 * Gives disconnected components in a labelImage distinct labels
	 * bg and forbidden label stay the same
	 * contour labels are treated as normal labels, 
	 * so use this function only for BEFORE contour particles are added to the labelImage
	 * (eg. to process user input for region guesses)
	 * @param li LabelImage
	 */
	public static void connectedComponents(LabelImage li)
	{
		//TODO ! test this
		
		HashSet<Integer> oldLabels = new HashSet<Integer>();
		int newLabel=1;
		
		int size=li.iterator.getSize();
		
		// what are the old labels?
		for(int i=0; i<size; i++)
		{
			int l=li.get(i);
			if(l==li.forbiddenLabel || l==li.bgLabel)
			{
				continue;
			}
			oldLabels.add(l);
		}
		
		for(int i=0; i<size; i++)
		{
			int l=li.get(i);
			if(l==li.forbiddenLabel || l==li.bgLabel)
			{
				continue;
			}
			if(oldLabels.contains(l))
			{
				// l is an old label
				MultipleThresholdImageFunction aMultiThsFunctionPtr = new MultipleThresholdImageFunction(li);
				aMultiThsFunctionPtr.AddThresholdBetween(l, l);
				FloodFill ff = new FloodFill(li, aMultiThsFunctionPtr, li.iterator.indexToPoint(i));
				
				//find a new label
				while(oldLabels.contains(newLabel)){
					newLabel++;
				}
				// set region to new label
				for(Point p:ff)
				{
					li.set(p, newLabel);
				}
				// next new label
				newLabel++;
			}
		}
	}
	
	
	void FreeLabelStatistics(Iterator<Entry<Integer, LabelInformation>> vActiveLabelsIt) 
	{
		vActiveLabelsIt.remove();
	}
	
	void FreeLabelStatistics(int vVisitedIt) 
	{
		labelMap.remove(vVisitedIt);

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
	
	private void CleanUp()
	{
		// UIntStatisticsContainerType::iterator vActiveLabelsIt = m_Count.begin();
		//TODO ConcurrentModificationException
		
		Iterator<Entry<Integer, LabelInformation>> vActiveLabelsIt = labelMap.entrySet().iterator();
		
		while(vActiveLabelsIt.hasNext()) 
		{
			Entry<Integer, LabelInformation> entry = vActiveLabelsIt.next();
			
			if(entry.getValue().count == 0) {
				FreeLabelStatistics(vActiveLabelsIt);
			}
		}

	}

	private boolean isBoundaryPoint(Point aIndex)
	{
		int vLabelAbs = getAbs(aIndex);
		for(Point q : connFG.itNeighborsOf(aIndex)) 
		{
			if(getAbs(q) != vLabelAbs)
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
		for(Point qIndex : conn.itNeighborsOf(pIndex))
		{
			if(labelToAbs(get(qIndex))!=absLabel)
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
		return labelIP;
	}
	
	public HashMap<Integer, LabelInformation> getLabelMap()
	{
		return labelMap;
	}
	
	public HashMap<Point, ContourParticle> getContourContainer()
	{
		return m_InnerContourContainer;
	}
	

	// ofset version for contour particles representation

	/**
	 * @param label a label
	 * @return if label was a contour label, get the absolut/inner label
	 */
	int labelToAbs(int label) {
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
	int labelToNeg(int label) 
	{
		if (label==bgLabel || isForbiddenLabel(label) || isContourLabel(label)) {
			return label;
		} else {
			return label + negOfs;
		}
	}

	
	int get(int index)
	{
		return labelIP.get(index);
	}


	/**
	 * @param p
	 * @return Returns the value of the LabelImage at x,y
	 */
	int get(int x, int y) 
	{
		return labelIP.get(x,y);
	}
	
	
	/**
	 * @param label
	 * @return true, if label is a contour label
	 * ofset version
	 */
	boolean isContourLabel(int label)
	{
		if(isForbiddenLabel(label)) {
			return false;
		} else {
			return (label > negOfs);
		}
	}
	

	
	// version with neg contour particles. see class LabelImageNeg
	// only works with short datatype
//
//	int labelToAbs(int label)
//	{
//		return Math.abs((short)(label));
//	}
//	
//	int labelToNeg(int label)
//	{
//		// TODO Auto-generated method stub
//		if(label==forbiddenLabel)
//			return label;
//		else
//			return -(label);
//	}
//	
//	int get(int index)
//	{
//		return (short)labelIP.get(index);
//	}
//
//	int get(int x, int y) 
//	{
//		return (short) labelIP.get(x,y);
//	}
//	
//	
//	boolean isContourLabel(int label)
//	{
//		return label<0;
//
//	} 
	


	/**
	 * @param p
	 * @return Returns the (raw; contour information) value of the LabelImage at Point p. 
	 */
	public int get(Point p) {
		//TODO multidimension
		return get(p.x[0], p.x[1]);
	}


	/**
	 * @return the abs (no contour information) label at Point p
	 */
	public int getAbs(Point p)
	{
		return labelToAbs(get(p));
	}

	public int getAbs(int idx)
	{
		return labelToAbs(get(idx));
	}

	//TODO merge with int getIntensity(int x, int y)
	/**
	 * returns the image data of the originalIP at Point p
	 */
	float getIntensity(Point p)
	{
		return getIntensity(p.x[0], p.x[1]);
	}
	
	float getIntensity(int idx)
	{
		//TODO !!!!!!!!!! prenormalize image!!!
		//TODO getPixel does index check
//		return imageProc.getf(idx);
		return imageProc.get(idx)/imageMax;
	}

	float getIntensity(int x, int y)
	{
		//TODO !!!!!!!!!! prenormalize image!!!
		//TODO getPixel does index check
//		return imageProc.getf(x, y);
		return imageProc.get(x, y)/imageMax;
		
//		return originalIP.getPixel(x, y)[0]/imageMax;
	}


	/**
	 * sets the labelImage to val at point x,y
	 */
	void set(int x, int y, int val) 
	{
		labelIP.set(x,y,val);
	}
	
	/**
	 * sets the labelImage to val at point x,y
	 */
	void set(int index, int val) 
	{
		labelIP.set(index, val);
	}


	/**
	 * sets the labelImage to val at Point p
	 */
	void set(Point p, int value) {
		//TODO multidimension
		labelIP.set(p.x[0], p.x[1], value);
	}


	public Connectivity getConnFG() {
		return connFG;
	}


	public Connectivity getConnBG() {
		return connBG;
	}


	void displaySlice()
	{
		displaySlice(null);
	}


	void displaySlice(String s)
	{
		if(MVC.userDialog.useStack())
		{
			MVC.addSliceToStackAndShow(s, labelIP.getPixelsCopy());
		}
	}


	static void debug(Object s)
	{
		System.out.println(s);
	}
	
	/**
	 * is contourContainer consistent with labelImage?
	 */
	void consistencyCheck()
	{
		for(Entry<Point, ContourParticle> e : m_InnerContourContainer.entrySet())
		{
			int imageLabel = getAbs(e.getKey());
			int contourLabel = e.getValue().label;
			if(imageLabel!=contourLabel)
			{
				debug("*** Inconsistency!!! ***");
				int dummy=0;
			}
		}
	}
	
	boolean checkForGhostLabel(int abslabel)
	{
		int size = m_LabelImage.height*m_LabelImage.width;
		for(int i=0; i<size; i++)
		{
			if(getAbs(i)==abslabel)
			{
				displaySlice("found a ghost label at "+ iterator.indexToPoint(i));
				debug("found a ghost label at "+ iterator.indexToPoint(i));
				return true;
			}
		}
		return false;
	}
	
	
	private void checkForNan(double...fs)
	{
		for(double f:fs)
		{
			if(Double.isNaN(f))
				debug("sth is NaN");
		}
	}


	boolean abort = false;
	/**
	 * Stops the algorithm after actual iteration
	 */
	public void stop()
	{
		abort=true;
	}

	
	public static LabelImageNeg getLabelImageNeg(Region_Competition rc)
	{
		return new LabelImageNeg(rc);
	}
	public static LabelImageFloat getLabelImageFloat(Region_Competition rc)
	{
		return new LabelImageFloat(rc);
	}
}


/**
 * In this version contour particles are represented by negative values
 * (super class: offset by 10000, all are pos) <br>
 * Expects data (label image) to be in <b>short</b>
 *
 */
class LabelImageNeg extends LabelImage
{

	public LabelImageNeg(Region_Competition region_competition)
	{
		super(region_competition);
		// TODO Auto-generated constructor stub
	}
	

	int labelToAbs(int label)
	{
		return Math.abs((short)(label));
	}
	
	int labelToNeg(int label)
	{
		// TODO Auto-generated method stub
		if(label==forbiddenLabel)
			return label;
		else
			return -(label);
	}
	
	boolean isContourLabel(int label)
	{
		return label<0;

	}
	
	int get(int index)
	{
		return (short)labelIP.get(index);
	}

	int get(int x, int y) 
	{
		return (short) labelIP.get(x,y);
	}
	
}


/**
 * Input image is converted to a [0,1] normalized floatProcessor
 *
 */
class LabelImageFloat extends LabelImage
{

	public LabelImageFloat(Region_Competition region_competition)
	{
		super(region_competition);
		normalizeImage();
	}
	
	void normalizeImage()
	{
		imageProc=imageProc.convertToFloat();
		double max = ImageStatistics.getStatistics(imageProc, ImageStatistics.MIN_MAX, null).max;
		
		double fac = 1.0/max;
		imageProc.multiply(fac);
		imageIP.setProcessor(imageProc);
	}
	
	float getIntensity(int idx)
	{
		return imageProc.getf(idx);
	}

	float getIntensity(int x, int y)
	{
		return imageProc.getf(x, y);
	}
	
}













