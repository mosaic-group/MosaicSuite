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
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import mosaic.core.binarize.BinarizedImage;
import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.utils.Connectivity;
import mosaic.core.utils.FloodFill;
import mosaic.core.utils.IndexIterator;
import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.Point;
import mosaic.plugins.Region_Competition;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.Energy.EnergyResult;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.energies.OscillationDetection;
import mosaic.region_competition.topology.TopologicalNumberImageFunction;
import mosaic.region_competition.topology.TopologicalNumberImageFunction.TopologicalNumberResult;
import mosaic.region_competition.utils.Timer;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;


public class Algorithm {

    private boolean shrinkFirst = false;

    private final Region_Competition MVC;
    private final LabelImageRC labelImage;
    private final IntensityImage intensityImage;
    private final ImageModel imageModel;
    private final IndexIterator labelImageIterator; // iterates over the labelImage

    private final int bgLabel;
    private final int forbiddenLabel;
    private LabelDispenser labelDispenser;

    /** stores the contour particles. access via coordinates */
    HashMap<Point, ContourParticle> m_InnerContourContainer;
    private HashMap<Point, ContourParticle> m_Candidates;

    /** Maps the label(-number) to the information of a label */
    final HashMap<Integer, LabelInformation> labelMap;

    private HashMap<Point, LabelPair> m_CompetingRegionsMap;
    private final Connectivity connFG;
    private final Connectivity connBG;
    private TopologicalNumberImageFunction m_TopologicalNumberFunction;

    private class Seed {
        private final Point iIndex;
        private final Integer iLabel;
        
        protected Seed(Point aIndex, Integer aLabel) {iIndex = aIndex; iLabel = aLabel;}
        protected Point getIndex() {return iIndex;}
        protected Integer getLabel() {return iLabel;}
       
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((iIndex == null) ? 0 : iIndex.hashCode());
            result = prime * result + ((iLabel == null) ? 0 : iLabel.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Seed other = (Seed) obj;
            if (iLabel == null && other.iLabel != null) return false;
            else if (!iLabel.equals(other.iLabel)) return false;
            if (iIndex == null && other.iIndex != null) return false;
            else if (!iIndex.equals(other.iIndex)) return false;
            return true;
        }     
    }
    private final Set<Seed> m_Seeds = new HashSet<Seed>();

    public Algorithm(IntensityImage intensityImage, LabelImageRC labelImage, ImageModel model, Settings settings, Region_Competition mvc) {
        if (shrinkFirst) {
            IJ.showMessage("shrinkfirst=true");
        }

        this.MVC = mvc;
        this.labelImage = labelImage;
        this.intensityImage = intensityImage;
        this.imageModel = model;
        this.settings = settings;

        bgLabel = labelImage.bgLabel;
        forbiddenLabel = labelImage.forbiddenLabel;
        labelImageIterator = labelImage.iterator;
        connFG = labelImage.getConnFG();
        connBG = labelImage.getConnBG();
        labelMap = labelImage.getLabelMap();

        initMembers();
        labelImage.initBoundary();
        initContour();
    }

    private final Settings settings;

    private boolean m_converged;
    public float m_AcceptedPointsFactor;
    private int m_iteration_counter; // member for debugging

    private OscillationDetection oscillationDetection;

    private EnergyFunctionalType m_EnergyFunctional;
    private int m_MaxNLabels;

    // Settings
    private final float AcceptedPointsFactor = 1;
    private final boolean RemoveNonSignificantRegions = true;
  
    
    ////////////////////////////////////////////////////

    private void initMembers() {
        m_InnerContourContainer = new HashMap<Point, ContourParticle>();
        m_Candidates = new HashMap<Point, ContourParticle>();
        m_CompetingRegionsMap = new HashMap<Point, LabelPair>();

        m_TopologicalNumberFunction = new TopologicalNumberImageFunction(labelImage, connFG, connBG);
        m_EnergyFunctional = settings.m_EnergyFunctional;
        oscillationDetection = new OscillationDetection(this, settings);

        m_iteration_counter = 0;
        m_converged = false;
        m_AcceptedPointsFactor = AcceptedPointsFactor;

        labelDispenser = new LabelDispenser();
        m_MaxNLabels = 0;
    }

    /**
     * marks the contour of each region (sets on labelImage)
     * stores the contour particles in contourContainer
     */
    private void initContour() {
        final Connectivity conn = connFG;

        for (final int i : labelImageIterator.getIndexIterable()) {
            final int label = labelImage.getLabelAbs(i);
            if (label != bgLabel && label != forbiddenLabel) // region pixel
            {
                final Point p = labelImageIterator.indexToPoint(i);
                for (final Point neighbor : conn.iterateNeighbors(p)) {
                    final int neighborLabel = labelImage.getLabelAbs(neighbor);
                    if (neighborLabel != label) {
                        final ContourParticle particle = new ContourParticle();
                        particle.label = label;
                        particle.intensity = intensityImage.get(i);
                        m_InnerContourContainer.put(p, particle);

                        break;
                    }
                }
            }
        }

        // set contour to -label
        for (final Entry<Point, ContourParticle> entry : m_InnerContourContainer.entrySet()) {
            final Point key = entry.getKey();
            final ContourParticle value = entry.getValue();
            // TODO cannot set neg values to ShortProcessor
            labelImage.setLabel(key, labelImage.labelToNeg(value.label));
        }
    }

    private void clearStats() {
        for (final LabelInformation stat : labelMap.values()) {
            stat.reset();
        }

        m_MaxNLabels = 0;
    }

    /**
     * as computeStatistics, does not use iterative approach
     * (first computes sum of values and sum of values^2)
     */
    private void renewStatistics() {
        clearStats();

        final HashSet<Integer> usedLabels = new HashSet<Integer>();

        final int size = labelImageIterator.getSize();
        for (int i = 0; i < size; i++) {
            final int absLabel = labelImage.getLabelAbs(i);

            if (absLabel != forbiddenLabel /* && absLabel != bgLabel */) {
                usedLabels.add(absLabel);
                if (absLabel > m_MaxNLabels) {
                    m_MaxNLabels = absLabel;
                }

                LabelInformation stats = labelMap.get(absLabel);
                if (stats == null) {
                    stats = new LabelInformation(absLabel, labelImage.getDim());
                    labelMap.put(absLabel, stats);
                }
                final double val = intensityImage.get(i);
                stats.count++;

                stats.mean += val; // only sum up, mean and var are computed below
                stats.var = (stats.var + val * val);
            }
        }

        // if background label do not exist add it
        LabelInformation stats = labelMap.get(0);
        if (stats == null) {
            stats = new LabelInformation(0, labelImage.getDim());
            labelMap.put(0, stats);
        }

        // now we have in all LabelInformation:
        // in mean the sum of the values, in var the sum of val^2
        for (final LabelInformation stat : labelMap.values()) {
            final int n = stat.count;
            if (n > 1) {
                final double var = (stat.var - stat.mean * stat.mean / n) / (n - 1);
                stat.var = (var);
            }
            else {
                stat.var = 0;
            }

            if (n > 0) {
                stat.mean = stat.mean / n;
            }
            else {
                stat.mean = 0.0;
            }

            // Median on start set equal to mean
            stat.median = stat.mean;
        }

        m_MaxNLabels++; // this number points to the a free label.

        labelDispenser.setLabelsInUse(usedLabels);
    }

    static private boolean RC_free = false;
    
    public boolean GenerateData(Img<FloatType> image_psf) {
        /**
         * Initialize standard statistics (mean, variances, length, area etc)
         */
        renewStatistics();

        /**
         * Depending on the functional to use, prepare stuff for faster
         * computation.
         */
        if (PrepareEnergyCaluclation(image_psf) == false) {
            return false;
        }

        /**
         * Start time measurement
         */

        final Timer timer = new Timer();
        timer.tic();

        /**
         * Main loop of the algorithm
         */

        boolean vConvergence = false;

        while (RC_free == true || (settings.m_MaxNbIterations > m_iteration_counter && !(vConvergence))) {
            synchronized (pauseMonitor) {
                if (pause) {
                    try {
                        debug("enter pause");
                        pauseMonitor.wait();
                        debug("exit pause");
                    }
                    catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (abort) {
                    break;
                }
            }

            m_iteration_counter++;
            debug("=== iteration " + m_iteration_counter + " ===");

            vConvergence = DoOneIteration();
            debug("time: " + timer.toc());

            if (shrinkFirst && vConvergence) {
                debug("Done with shrinking, now allow growing");
                vConvergence = false;
                shrinkFirst = false;
                m_AcceptedPointsFactor = AcceptedPointsFactor;
            }

            MVC.addSlice(labelImage, "iteration " + m_iteration_counter);
            MVC.updateProgress(m_iteration_counter, settings.m_MaxNbIterations);
        }

        MVC.addSlice(labelImage, "final image iteration " + m_iteration_counter);

        m_converged = vConvergence;

        timer.toc();
        debug("Total time: " + timer.lastResult());

        /**
         * Debugging Output
         */

        final long executionTime = timer.lastResult();
        debug("time per iteration: " + executionTime / m_iteration_counter);

        if (m_converged) {
            debug("convergence after " + m_iteration_counter + " iterations.");
        }
        else {
            debug("no convergence !");
        }

        return true;
    }

    private final Vector<ImagePlus> OpenedImages = new Vector<ImagePlus>();

    /**
     * Initialize the energy function
     *
     * @param img The PSF image optionally is de-convolving segmentation is
     *            selected
     * @return
     */

    private boolean PrepareEnergyCaluclation(Img<FloatType> image_psf) {
        /**
         * Deconvolution:
         * - prepare the PSF (if not set manually by the user)
         * - Alocate and initialize the 'ideal image'
         */
        if (settings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
            // Set deconvolution

            // if not PSF has been generated stop

            if (image_psf == null) {
                IJ.error("Error no PSF generated");
                return false;
            }

            final double Vol = IntensityImage.volume_image(image_psf);
            IntensityImage.rescale_image(image_psf, (float) (1.0f / Vol));

            // Show PSF Image

            if (MVC.getHideProcess() == false) {
                OpenedImages.add(ImageJFunctions.show(image_psf));
            }

            // Ugly forced to be float

            ((E_Deconvolution) imageModel.getEdata()).setPSF(image_psf);
            ((E_Deconvolution) imageModel.getEdata()).GenerateModelImage(labelImage, labelMap);
            ((E_Deconvolution) imageModel.getEdata()).RenewDeconvolution(labelImage);
        }

        return true;
    }

    private boolean DoOneIteration() {

        boolean vConvergenceA;
        vConvergenceA = true;

        if (m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC && m_iteration_counter % 1 == 0) {
            ((E_Deconvolution) imageModel.getEdata()).RenewDeconvolution(labelImage);
        }

        if (RemoveNonSignificantRegions) {
            RemoveSinglePointRegions();
            RemoveNotSignificantRegions();
        }

        vConvergenceA = IterateContourContainerAndAdd();
        CleanUp();

        MVC.showStatus("Done");
        return vConvergenceA;
    }

    private boolean IterateContourContainerAndAdd() {
        m_Candidates.clear();
        m_Seeds.clear();
        // Convergence is set to false if a point moved
        boolean convergence;

        // clear the competing regions map, it will be refilled in
        // RebuildCandidateList:
        m_CompetingRegionsMap.clear();

        MVC.showStatus("Rebuild Candidates");
        RebuildCandidateList(m_Candidates);

        MVC.showStatus("Filter Candidates");
        FilterCandidates(m_Candidates);

        MVC.showStatus("Detect Oscillations");
        DetectOscillations(m_Candidates);

        FilterCandidatesContainerUsingRanks(m_Candidates);

        MVC.showStatus("Move Points");
        convergence = MoveCandidates(m_Candidates);

        return convergence;
    }

    /**
     * Move the points in the candidate list
     *
     * @param m_Candidates
     * @return
     */
    private boolean MoveCandidates(HashMap<Point, ContourParticle> m_Candidates) {

        /**
         * Move all the points that are simple. Non simple points remain in the
         * candidates list.
         */

        // We first move all the FG-simple points. This we do because it
        // happens
        // that points that are not simple at the first place get simple after
        // the change of other points. The non-simple points will be treated
        // in a separate loop afterwards.

        boolean vChange = true;
        boolean vConvergence = true;

        List<TopologicalNumberResult> vFGTNvector;

        while (vChange && !m_Candidates.isEmpty()) {
            vChange = false;

            final Iterator<Entry<Point, ContourParticle>> vPointIterator = m_Candidates.entrySet().iterator();
            while (vPointIterator.hasNext()) {
                final Entry<Point, ContourParticle> vStoreIt = vPointIterator.next();
                final Point vCurrentIndex = vStoreIt.getKey();

                vFGTNvector = m_TopologicalNumberFunction.EvaluateAdjacentRegionsFGTNAtIndex(vCurrentIndex);
                boolean vSimple = true;
                // Check for FG-simplicity:
                for (final TopologicalNumberResult vTopoNbItr : vFGTNvector) {
                    if (vTopoNbItr.topologicalNumberPair.FGNumber != 1 || vTopoNbItr.topologicalNumberPair.BGNumber != 1) {
                        // This is a FG simple point; perform the move.
                        vSimple = false;
                        // debug("0");
                    }
                }
                if (vSimple) {
                    vChange = true;
                    ChangeContourPointLabelToCandidateLabel(vStoreIt);
                    vPointIterator.remove();
                    vConvergence = false;
                    // debug("1");
                }
                // we will reuse the processed flag to indicate if a particle
                // is a seed
                vStoreIt.getValue().m_processed = false;
            }
        }

        // Now we know that all the points in the list are 'currently' not
        // simple.
        // We move them anyway (if topological constraints allow) but record
        // (for every particle) where to relabel (using the seed set). Placing
        // the seed is necessary for every particle to ensure relabeling even
        // if a bunch of neighboring particles change. The seed will be
        // ignored
        // later on if the corresponding FG region is not present in the
        // neighborhood anymore.
        // TODO: The following code is dependent on the iteration order if
        // splits/handles
        // are not allowed. A solution would be to sort the candidates
        // beforehand.
        // This should be computationally not too expensive since we assume
        // there
        // are not many non-simple points.

        final Iterator<Entry<Point, ContourParticle>> vPointIterator = m_Candidates.entrySet().iterator();

        while (vPointIterator.hasNext()) {
            final Entry<Point, ContourParticle> e = vPointIterator.next();
            final ContourParticle vStoreIt = e.getValue();
            final Point vCurrentIndex = e.getKey();

            final int vCurrentLabel = vStoreIt.label;
            final int vCandidateLabel = vStoreIt.candidateLabel;

            boolean vValidPoint = true;

            vFGTNvector = (m_TopologicalNumberFunction.EvaluateAdjacentRegionsFGTNAtIndex(vCurrentIndex));

            // Check for handles:
            // if the point was not disqualified already and we disallow
            // introducing handles (not only self fusion!), we check if
            // there is an introduction of a handle.

            if (vValidPoint && !settings.m_AllowHandles) {
                for (final TopologicalNumberResult vTopoNbItr : vFGTNvector) {
                    if (vTopoNbItr.label == vCandidateLabel) {
                        if (vTopoNbItr.topologicalNumberPair.FGNumber > 1) {
                            vValidPoint = false;
                            // break;
                        }
                    }

                    // criterion to detect surface points (only 3D?)
                    if (vTopoNbItr.topologicalNumberPair.FGNumber == 1 && vTopoNbItr.topologicalNumberPair.BGNumber > 1) {
                        vValidPoint = false;
                        // break;
                    }
                }
            }

            // Check for splits:
            // This we have to do either to forbid
            // the change in topology or to register the seed point for
            // relabeling.
            // if the point was not disqualified already and we disallow
            // splits, then we check if the 'old' label undergoes a split.
            if (vValidPoint) {
                // - "allow introducing holes": T_FG(x, L = l') > 1
                // - "allow splits": T_FG > 2 && T_BG == 1
                boolean vSplit = false;
                for (final TopologicalNumberResult vTopoNbItr : vFGTNvector) {
                    if (vTopoNbItr.label == vCurrentLabel) {
                        if (vTopoNbItr.topologicalNumberPair.FGNumber > 1) {
                            vSplit = true;
                        }
                    }
                }
                if (vSplit) {
                    if (settings.m_AllowFission) {
                        RegisterSeedsAfterSplit(vCurrentIndex, vCurrentLabel, m_Candidates);
                    }
                    else {
                        // disallow the move.
                        vValidPoint = false;
                    }
                }
            }

            if (!vValidPoint) {
                vPointIterator.remove();
            }

            // If the move doesn't change topology or is allowed (and registered
            // as seed) to change the topology, perform the move (in the
            // second iteration; in the first iteration seed points need to
            // be collected):

            if (vValidPoint) {
                ChangeContourPointLabelToCandidateLabel(e);
                vConvergence = false;

                if (e.getValue().m_processed) {
                    RegisterSeedsAfterSplit(vCurrentIndex, vCurrentLabel, m_Candidates);
                    Seed seed = new Seed(vCurrentIndex, vCurrentLabel);
                    final boolean wasContained = m_Seeds.remove(seed);
                    if (!wasContained) {
                        throw new RuntimeException("no seed in set");
                    }
                }
            }

            if (m_Candidates.containsKey(vCurrentIndex)) {
                vPointIterator.remove();
            }

        } 

        // Perform relabeling of the regions that did a split:
        boolean vSplit = false;
        boolean vMerge = false;
        
        for (final Seed vSeedIt : m_Seeds) {
            RelabelRegionsAfterSplit(labelImage, vSeedIt.getIndex(), vSeedIt.getLabel());
            vSplit = true;
        }

        // Merge the the competing regions if they meet merging criterion.
        if (settings.m_AllowFusion) {

            final Set<Integer> vCheckedLabels = new HashSet<Integer>();

            for (final Entry<Point, LabelPair> vCRit : m_CompetingRegionsMap.entrySet()) {
                final Point idx = vCRit.getKey();
                final LabelPair labels = vCRit.getValue();

                final int vLabel1 = labels.first;
                final int vLabel2 = labels.second;

                if (vCheckedLabels.contains(vLabel1) || vCheckedLabels.contains(vLabel2)) {
                    // do nothing, since this label was already in a fusion-chain
                }
                else {
                    RelabelRegionsAfterFusion(labelImage, idx, vLabel1, vCheckedLabels);
                }
                vMerge = true;
            }
        }

        if (vSplit || vMerge) {
            if (m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
                ((E_Deconvolution) imageModel.getEdata()).RenewDeconvolution(labelImage);
            }
        }

        return vConvergence;
    }

    private void RegisterSeedsAfterSplit(Point aIndex, int aLabel, HashMap<Point, ContourParticle> aCandidateContainer) {

        for (final Point vSeedIndex : connFG.iterateNeighbors(aIndex)) {
            final int vLabel = labelImage.getLabelAbs(vSeedIndex);

            if (vLabel == aLabel) {
                final Seed vSeed = new Seed(vSeedIndex, vLabel);
                m_Seeds.add(vSeed);
                // At the position where we put the seed, inform the particle
                // that it has to inform its neighbor in case it moves (if
                // there
                // is a particle at all at this spot; else we don't have a
                // problem
                // because the label will not move at the spot and therefore
                // the
                // seed will be effective).

                final ContourParticle vParticle = aCandidateContainer.get(vSeedIndex);
                if (vParticle != null) {
                    vParticle.m_processed = true;
                }
            }
        }
    }

    /**
     * Detect oscillations and store values in history.
     */
    private void DetectOscillations(HashMap<Point, ContourParticle> m_Candidates) {
        if (RC_free == false) {
            oscillationDetection.DetectOscillations(m_Candidates);
        }
    }

    /**
     * Iterates through ContourParticles in m_InnerContourContainer <br>
     * Builds mothers, daughters, computes energies and <br>
     * fills m_CompetingRegionsMap
     *
     * @param aReturnContainer
     */
    private void RebuildCandidateList(HashMap<Point, ContourParticle> aReturnContainer) {
        aReturnContainer.clear();

        // Add all the mother points - this is copying the inner contour list.
        // (Things get easier afterwards if this is done in advance.)

        // calculate energy for change to BG (shrinking)
        int counter = 0;
        int size = m_InnerContourContainer.size();
        for (final Entry<Point, ContourParticle> vPointIterator : m_InnerContourContainer.entrySet()) {
            MVC.updateProgress(counter++, size);

            final Point vCurrentIndex = vPointIterator.getKey();
            final ContourParticle vVal = vPointIterator.getValue();

            vVal.candidateLabel = 0;
            vVal.referenceCount = 0; // doesn't matter for the BG
            vVal.isMother = true;
            vVal.m_processed = false;
            vVal.energyDifference = CalculateEnergyDifferenceForLabel(vCurrentIndex, vVal, bgLabel).energyDifference;
            vVal.clearLists();
            vVal.setTestedLabel(bgLabel);

            aReturnContainer.put(vCurrentIndex, vVal);
        }

        if (shrinkFirst) {
            return;
        }

        // Iterate the contour list and visit all the neighbors in the
        // FG-Neighborhood.
        // calculate energy for expanding into neighborhood (growing)
        counter = 0;
        size = m_InnerContourContainer.size();
        for (final Entry<Point, ContourParticle> vPointIterator : m_InnerContourContainer.entrySet()) {
            MVC.updateProgress(counter++, size);

            final Point vCurrentIndex = vPointIterator.getKey();
            final ContourParticle vVal = vPointIterator.getValue();

            final int vLabelOfPropagatingRegion = vVal.label;
            // vLabelImageIterator.SetLocation(vCurrentIndex);

            final Connectivity conn = connFG;
            for (final Point q : conn.iterateNeighbors(vCurrentIndex)) {
                final int vLabelOfDefender = labelImage.getLabelAbs(q);
                if (vLabelOfDefender == forbiddenLabel) {
                    continue;
                }

                // expanding into other region / label. (into same region:
                // nothing happens)
                if (vLabelOfDefender != vLabelOfPropagatingRegion) {
                    final Point vNeighborIndex = q;

                    // Tell the mother about the daughter:
                    // TODO senseless lookup? use vVal instead of
                    // aReturnContainer.get(vCurrentIndex)
                    aReturnContainer.get(vCurrentIndex).getDaughterList().add(vNeighborIndex);

                    final ContourParticle vContourPointItr = aReturnContainer.get(vNeighborIndex);
                    if (vContourPointItr == null) {
                        // create a new entry (a daughter), the contour point
                        // has not been part of the contour so far.

                        final ContourParticle vOCCValue = new ContourParticle();
                        vOCCValue.candidateLabel = vLabelOfPropagatingRegion;
                        vOCCValue.label = vLabelOfDefender;
                        vOCCValue.intensity = intensityImage.get(vNeighborIndex);
                        vOCCValue.isMother = false;
                        vOCCValue.m_processed = false;
                        vOCCValue.referenceCount = 1;
                        vOCCValue.energyDifference = CalculateEnergyDifferenceForLabel(vNeighborIndex, vOCCValue, vLabelOfPropagatingRegion).energyDifference;
                        vOCCValue.setTestedLabel(vLabelOfPropagatingRegion);
                        // Tell the daughter about the mother:
                        vOCCValue.getMotherList().add(vCurrentIndex);

                        aReturnContainer.put(vNeighborIndex, vOCCValue);

                    }
                    else // the point is already part of the candidate list
                    {
                        vContourPointItr.isMother = false;
                        // Tell the daughter about the mother (label does not
                        // matter!):
                        vContourPointItr.getMotherList().add(vCurrentIndex);

                        // Check if the energy difference for this candidate
                        // label
                        // has not yet been calculated.
                        if (!vContourPointItr.hasLabelBeenTested((vLabelOfPropagatingRegion))) {
                            vContourPointItr.setTestedLabel(vLabelOfPropagatingRegion);

                            final EnergyResult energyAndMerge = CalculateEnergyDifferenceForLabel(vNeighborIndex, vContourPointItr, vLabelOfPropagatingRegion);

                            final double vEnergyDiff = energyAndMerge.energyDifference;
                            final boolean aMerge = energyAndMerge.merge;
                            if (vEnergyDiff < vContourPointItr.energyDifference) {

                                vContourPointItr.candidateLabel = vLabelOfPropagatingRegion;
                                vContourPointItr.energyDifference = vEnergyDiff;
                                vContourPointItr.referenceCount = 1;

                                // TODO new sts merge 05.03.2012
                                // if ((*aMerge)[vP] && vParticle.m_label != 0
                                // && vParticle.m_candidateLabel != 0)
                                if (aMerge && vContourPointItr.label != bgLabel && vContourPointItr.candidateLabel != bgLabel) {
                                    final int L1 = vContourPointItr.candidateLabel;
                                    final int L2 = vContourPointItr.label;

                                    final LabelPair pair = new LabelPair(L1, L2);
                                    // System.out.println("merge pair: "+pair.first
                                    // + " " +pair.second);

                                    m_CompetingRegionsMap.put(vCurrentIndex, pair);

                                    // TODO removed from itk
                                    // Ensure the point does not move since
                                    // we'd like to merge
                                    // here. Todo so, we set the energy to a
                                    // large value.
                                    // vParticle.m_energyDifference = return
                                    // NumericTraits<EnergyDifferenceType>::max();
                                    // aReturnContainer.remove(vCurrentIndex);
                                }

                            }
                        }
                        else {
                            // If the propagating label is the same as the
                            // candidate label,
                            // we have found 2 or more mothers of for this
                            // contour point.ten
                            if (vContourPointItr.candidateLabel == vLabelOfPropagatingRegion) {
                                vContourPointItr.referenceCount++;
                            }
                        }

                    } // else the point is already part of the candidate list
                }

            } // for (Point q : conn.itNeighborsOf(vCurrentIndex))

        }

    }

    /* int max_graph = 0; */

    /**
     * Filters topological incompatible candidates (topological dependencies)
     * and non-improving energies.
     */
    private void FilterCandidates(HashMap<Point, ContourParticle> m_Candidates) {

        if (shrinkFirst) {
            final Iterator<Entry<Point, ContourParticle>> it = m_Candidates.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<Point, ContourParticle> vStoreIt = it.next(); // iterator
                // to work
                // with
                if (vStoreIt.getValue().energyDifference >= 0) {
                    it.remove();
                    // m_Candidates.remove(vStoreIt);
                }
            }
            return;
        }

        /**
         * Find topologically compatible candidates and store their indices in
         * vLegalIndices.
         */
        final List<Point> vLegalIndices = new LinkedList<Point>();
        final List<Point> vIllegalIndices = new LinkedList<Point>();
        for (final Entry<Point, ContourParticle> vPointIterator : m_Candidates.entrySet()) {
            final Point pIndex = vPointIterator.getKey();
            final ContourParticle p = vPointIterator.getValue();

            // Check if this point already was processed
            if (!p.m_processed) {
                // Check if it is a mother: only mothers can be seed points
                // of topological networks. Daughters are always part of a
                // topo network of a mother.
                if (!p.isMother) {
                    continue;
                }

                /**
                 * Build the dependency network for this seed point:
                 */
                final Stack<Point> vIndicesToVisit = new Stack<Point>();
                final List<ContourParticleWithIndexType> vSortedNetworkMembers = new LinkedList<ContourParticleWithIndexType>();
                vIndicesToVisit.push(pIndex);
                p.m_processed = true;

                while (!vIndicesToVisit.empty()) {
                    final Point vSeedIndex = vIndicesToVisit.pop();
                    final ContourParticle vCurrentMother = m_Candidates.get(vSeedIndex);

                    // Add the seed point to the network
                    final ContourParticleWithIndexType vSeedContourPointWithIndex = new ContourParticleWithIndexType(vSeedIndex, vCurrentMother);
                    vSortedNetworkMembers.add(vSeedContourPointWithIndex);

                    // Iterate all children of the seed, push to the stack if
                    // there
                    // is a mother.
                    final List<Point> vDaughterIt = vCurrentMother.getDaughterList();
                    for (final Point vDaughterContourIndex : vDaughterIt) {
                        // if (vAllCandidates.find(vDaughterContourIndex) ==
                        // vAllCandidates.end())
                        // std::cout << "daughter index found not in the list: "
                        // << vDaughterContourIndex << std::endl;

                        final ContourParticle vDaughterContourPoint = m_Candidates.get(vDaughterContourIndex);

                        if (!vDaughterContourPoint.m_processed) {
                            vDaughterContourPoint.m_processed = true;

                            if (vDaughterContourPoint.isMother) {
                                vIndicesToVisit.push(vDaughterContourIndex);
                            }
                            else {
                                final ContourParticleWithIndexType vDaughterContourPointWithIndex = new ContourParticleWithIndexType(vDaughterContourIndex, vDaughterContourPoint);
                                vSortedNetworkMembers.add(vDaughterContourPointWithIndex);
                            }

                            // Push all the non-processed mothers of this
                            // daughter to the stack
                            final List<Point> vDMIt = vDaughterContourPoint.getMotherList();
                            for (final Point vDM : vDMIt) {
                                final ContourParticle vMotherOfDaughterPoint = m_Candidates.get(vDM);
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

                /*
                 * if (vSortedNetworkMembers.size() >= max_graph)
                 * max_graph = vSortedNetworkMembers.size();
                 */

                /**
                 * Filtering: Accept all members in ascending order that are
                 * compatible with the already selected members in the network.
                 */

                // TODO HashSet problem (maybe need hashmap)
                final HashSet<Point> vSelectedCandidateIndices = new HashSet<Point>();

                for (final ContourParticleWithIndexType vNetworkIt : vSortedNetworkMembers) {

                    // If a mother is accepted, the reference count of all the
                    // daughters (with the same label) has to be decreased.
                    // Rules: a candidate in the network is a legal candidate
                    // if:
                    // - If (daughter): The reference count >= 1. (Except the
                    // the candidate label is the BG - this allows
                    // creating BG regions inbetween two competing
                    // regions).
                    // - If ( mother ): All daughters (with the same 'old'
                    // label) in the
                    // accepted list have still a reference count > 1.

                    boolean vLegalMove = true;

                    //
                    // RULE 1: If c is a daughter point, the reference count
                    // r_c is > 0.
                    //
                    if (!vNetworkIt.iParticle.isMother) {
                        final ContourParticle vCand = m_Candidates.get(vNetworkIt.iParticleIndex);
                        if (vCand.referenceCount < 1 && vCand.candidateLabel != 0) {
                            vLegalMove = false;
                        }
                    }

                    //
                    // RULE 2: All daughters already accepted the label of
                    // this
                    // mother have at least one another mother.
                    // AND
                    // RULE 3: Mothers are still valid mothers (to not
                    // introduce
                    // holes in the FG region).
                    //

                    if (vLegalMove && vNetworkIt.iParticle.isMother) {
                        // Iterate the daughters and check their reference
                        // count

                        boolean vRule3Fulfilled = false;

                        for (final Point vDaughterIndicesIterator : vNetworkIt.iParticle.getDaughterList()) {
                            final ContourParticle vDaughterPoint = m_Candidates.get(vDaughterIndicesIterator);

                            // rule 2:
                            final boolean vAcceptedDaugtherItContained = vSelectedCandidateIndices.contains(vDaughterIndicesIterator);

                            if (vAcceptedDaugtherItContained) {
                                // This daughter has been accepted
                                // and needs a reference count > 1,
                                // else the move is invalid.
                                if (vDaughterPoint.candidateLabel == vNetworkIt.iParticle.label && vDaughterPoint.referenceCount <= 1) {
                                    vLegalMove = false;
                                    break;
                                }
                            }

                            // rule 3:
                            if (!vRule3Fulfilled) {
                                if (!vAcceptedDaugtherItContained) {
                                    // There is a daughter that has not yet
                                    // been accepted.
                                    vRule3Fulfilled = true;
                                }
                                else {

                                    // the daughter has been accepted, but may
                                    // have another candidate label(rule 3b):

                                    if (m_Candidates.get(vDaughterIndicesIterator).candidateLabel != vNetworkIt.iParticle.label) {
                                        vRule3Fulfilled = true;
                                    }
                                }
                            }
                        }

                        if (!vRule3Fulfilled) {
                            vLegalMove = false;
                        }
                    }

                    if (vLegalMove) {
                        // the move is legal, store the index in the container
                        // with accepted candidates of this network.
                        vSelectedCandidateIndices.add(vNetworkIt.iParticleIndex);

                        // also store the candidate in the global candidate
                        // index container:
                        // TODO used nowhere
                        vLegalIndices.add(vNetworkIt.iParticleIndex);

                        // decrease the references of its daughters(with the
                        // same 'old' label).
                        for (final Point vDaughterIndicesIterator : vNetworkIt.iParticle.getDaughterList()) {
                            final ContourParticle vDaughterPoint = m_Candidates.get(vDaughterIndicesIterator);
                            if (vDaughterPoint.candidateLabel == vNetworkIt.iParticle.label) {
                                vDaughterPoint.referenceCount--;
                            }
                        }
                    }
                    else {
                        vIllegalIndices.add(vNetworkIt.iParticleIndex);
                    }
                }
            }
        }

        /**
         * Filter all candidates with the illegal indices
         */

        for (final Point vIlligalIndicesIt : vIllegalIndices) {
            m_Candidates.remove(vIlligalIndicesIt);
        }

        /**
         * Filter candidates according to their energy
         */

        final Iterator<Entry<Point, ContourParticle>> it = m_Candidates.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<Point, ContourParticle> vStoreIt = it.next(); // iterator to
            // work with
            if (vStoreIt.getValue().energyDifference >= 0) {
                it.remove();
                // m_Candidates.remove(vStoreIt);
            }
        }
        // WriteContourPointContainer("contourPointsAfterEnergyCheck.txt",
        // vAllCandidates);

    }

    /**
     * called while iterating over m_InnerContourContainer in
     * RemoveSinglePointRegions()
     * calling
     * m_InnerContourContainer.remove(vCurrentIndex);
     * m_InnerContourContainer.put(vCurrentIndex, vContourPoint);
     */
    private void ChangeContourPointLabelToCandidateLabel(Entry<Point, ContourParticle> aParticle) {
        final ContourParticle second = aParticle.getValue();
        final Point vCurrentIndex = aParticle.getKey();

        final int vFromLabel = second.label;
        final int vToLabel = second.candidateLabel;

        //
        // The particle was modified,reset the counter in order to process
        // the particle in the next iteration.
        //
        // TODO modifiedCounter
        // aParticle->second.m_modifiedCounter = 0;

        //
        // Update the label image. The new point is either a contour point or
        // 0,
        // therefore the negative label value is set.
        // TODO sts is this true? what about enclosed pixels?
        // ie first was like a 'U', then 'O' and now the hole is filled, then it
        // is an inner point
        // where is this handled?
        //
        labelImage.setLabel(vCurrentIndex, labelImage.labelToNeg(vToLabel));

        //
        // STATISTICS UPDATE
        // Update the statistics of the propagating and the loser region.
        //
        if (RC_free == false) {
            UpdateStatisticsWhenJump(aParticle, vFromLabel, vToLabel);
        }
        if (imageModel.getEdataType() == EnergyFunctionalType.e_DeconvolutionPC) {
            ((E_Deconvolution) imageModel.getEdata()).UpdateConvolvedImage(vCurrentIndex, labelImage, vFromLabel, vToLabel);
        }

        // TODO: A bit a dirty hack: we store the old label for the relabeling
        // procedure later on...either introduce a new variable or rename the
        // variable (which doesn't work currently :-).
        second.candidateLabel = vFromLabel;

        //
        // Clean up
        //

        // The loser region (if it is not the BG region) has to add the
        // neighbors of the lost point to the contour list.
        if (vFromLabel != bgLabel) {
            AddNeighborsAtRemove(vFromLabel, vCurrentIndex);
        }

        //
        // Erase the point from the surface container in case it now belongs to
        // the background. Else, add the point to the container (or replace it
        // in case it has been there already).
        if (vToLabel == bgLabel) {
            m_InnerContourContainer.remove(vCurrentIndex);
        }
        else {

            // TODO compare with itk. vContourPoint = second is unnecessary in
            // java. was this a copy in c++?

            final ContourParticle vContourPoint = second;
            vContourPoint.label = vToLabel;
            // The point may or may not exist already in the m_InnerContainer.
            // The old value, if it exist, is just overwritten with the new
            // contour point (with a new label).

            m_InnerContourContainer.put(vCurrentIndex, vContourPoint);
        }

        // Remove 'enclosed' contour points from the container. For the BG
        // this makes no sense.
        if (vToLabel != bgLabel) {
            MaintainNeighborsAtAdd(vToLabel, vCurrentIndex);
        }
    }

    /**
     * iterator problem: calling m_InnerContourContainer.put() <br>
     * Removing a point / changing its label generates new contour points in
     * general <br>
     * This method generates the new contour particles and adds them to
     * container
     * itk::AddNeighborsAtRemove
     *
     * @param pIndex changing point
     * @param aAbsLabel old label of this point
     */
    private void AddNeighborsAtRemove(int aAbsLabel, Point pIndex) {
        for (final Point qIndex : connFG.iterateNeighbors(pIndex)) {
            final int qLabel = labelImage.getLabel(qIndex);

            // TODO can the labels be negative? somewhere, they are set (maybe
            // only temporary) to neg values
            if (labelImage.isContourLabel(aAbsLabel)) {
                debug("AddNeighborsAtRemove. one label is not absLabel\n");
                int dummy = 0;
                dummy = dummy + 0;
            }

            // q is a inner point with the same label as p
            if (labelImage.isInnerLabel(qLabel) && qLabel == aAbsLabel) 
            {
                final ContourParticle q = new ContourParticle();
                q.label = aAbsLabel;
                q.candidateLabel = bgLabel;
                q.intensity = intensityImage.get(qIndex);

                labelImage.setLabel(qIndex, labelImage.labelToNeg(aAbsLabel));
                m_InnerContourContainer.put(qIndex, q);
            }
            // TODO this never can be true
            // (since isContourLabel==> neg values AND (qLabel == aAbsLabel) => pos labels
            else if (labelImage.isContourLabel(qLabel) && qLabel == aAbsLabel) 
            {
                // q is contour of the same label

                // TODO itk Line 1520, modifiedcounter
                // the point is already in the contour. We reactivate it by
                // ensuring that the energy is calculated in the next
                // iteration:
                // contourContainer.get(qIndex).m_modifiedCounter = 0;
                // m_InnerContourContainer[vI].m_modifiedCounter = 0;
            }
        }
    }

    /**
     * iterator problem: m_InnerContourContainer.remove <br>
     * itk::MaintainNeighborsAtAdd
     * Maintain the inner contour container: <br>
     * - Remove all the indices in the BG-connected neighborhood, that are
     * interior points, from the contour container. <br>
     * Interior here means that none neighbors in the FG-Neighborhood
     * has a different label.
     */
    private void MaintainNeighborsAtAdd(int aLabelAbs, Point pIndex) {
        // ContourParticle p = m_InnerContourContainer.get(pIndex);
        final int aLabelNeg = labelImage.labelToNeg(aLabelAbs);

        // itk 1646: we set the pixel value already to ensure the that the
        // 'enclosed' check
        // afterwards works.
        // TODO is p.label (always) the correct label?
        labelImage.setLabel(pIndex, labelImage.labelToNeg(aLabelAbs));

        final Connectivity conn = connBG;
        for (final Point qIndex : conn.iterateNeighbors(pIndex)) {
            // from itk:
            // It might happen that a point, that was already accepted as a
            // candidate, gets enclosed by other candidates. This
            // points must not be added to the container afterwards and thus
            // removed from the main list.

            // TODO ??? why is BGconn important? a BGconnected neighbor cannot
            // change the "enclosed" status for FG?
            if (labelImage.getLabel(qIndex) == aLabelNeg && labelImage.isEnclosedByLabel(qIndex, aLabelAbs)) {
                m_InnerContourContainer.remove(qIndex);
                labelImage.setLabel(qIndex, aLabelAbs);
            }
        }

        if (labelImage.isEnclosedByLabel(pIndex, aLabelAbs)) {
            m_InnerContourContainer.remove(pIndex);
            labelImage.setLabel(pIndex, aLabelAbs);
        }
    }

    /**
     * The function relabels a region starting from the position aIndex.
     * This method assumes the label image to be updated. It is used to relabel
     * a region that was split by another region (maybe BG region).
     */
    private void RelabelRegionsAfterSplit(LabelImageRC aLabelImage, Point aIndex, int aLabel) {
        if (aLabelImage.getLabelAbs(aIndex) == aLabel) {
            final BinarizedIntervalLabelImage vMultiThsFunction = new BinarizedIntervalLabelImage(aLabelImage);
            vMultiThsFunction.AddThresholdBetween(aLabel, aLabel);
            final int negLabel = aLabelImage.labelToNeg(aLabel);
            vMultiThsFunction.AddThresholdBetween(negLabel, negLabel);
            fire(aIndex, labelDispenser.getNewLabel(), vMultiThsFunction);
        }
    }

    // The function relabels 2 neighboring regions. These 2 regions are
    // fused into a new region. Both regions are at position of aIndex
    // or adjacent to it.
    // Relabel2AdjacentRegionsAfterToplogicalChange expects the label image to
    // be updated already: both methods are connected via the seedpoint. This
    // method may be used to fuse 2 regions in the region competition mode.
    private void RelabelRegionsAfterFusion(LabelImageRC aLabelImage, Point aIndex, int aL1, Set<Integer> aCheckedLabels) {

        final BinarizedIntervalLabelImage vMultiThsFunction = new BinarizedIntervalLabelImage(aLabelImage);

        // LinkedList<Integer> vLabelsToCheck = new LinkedList<Integer>();
        final Stack<Integer> vLabelsToCheck = new Stack<Integer>();

        vLabelsToCheck.push(aL1);
        vMultiThsFunction.AddThresholdBetween(aL1, aL1);
        final int aL1Neg = aLabelImage.labelToNeg(aL1);
        vMultiThsFunction.AddThresholdBetween(aL1Neg, aL1Neg);
        aCheckedLabels.add(aL1);

        // for (int vLabelToCheck : vLabelsToCheck)
        while (!vLabelsToCheck.isEmpty()) {
            final int vLabelToCheck = vLabelsToCheck.pop();
            for (final LabelPair vMergingLabelsPair : m_CompetingRegionsMap.values()) {
                final int vLabel1 = vMergingLabelsPair.first;
                final int vLabel2 = vMergingLabelsPair.second;

                if (vLabel1 == vLabelToCheck && !aCheckedLabels.contains(vLabel2)) {
                    vMultiThsFunction.AddThresholdBetween(vLabel2, vLabel2);
                    vMultiThsFunction.AddThresholdBetween(aLabelImage.labelToNeg(vLabel2), labelImage.labelToNeg(vLabel2));
                    aCheckedLabels.add(vLabel2);
                    vLabelsToCheck.push(vLabel2);
                }
                if (vLabel2 == vLabelToCheck && !aCheckedLabels.contains(vLabel1)) {
                    vMultiThsFunction.AddThresholdBetween(vLabel1, vLabel1);
                    vMultiThsFunction.AddThresholdBetween(aLabelImage.labelToNeg(vLabel1), labelImage.labelToNeg(vLabel1));
                    aCheckedLabels.add(vLabel1);
                    vLabelsToCheck.push(vLabel1);
                }
            }
        }
        if (vMultiThsFunction.EvaluateAtIndex(aIndex)) {
            fire(aIndex, labelDispenser.getNewLabel(), vMultiThsFunction);
        }

    }

    private void RemoveSinglePointRegions() {

        // TODO: here we go first through contour points to find different labels
        // and then checking for labelInfo.count==1.
        // instead, we could iterate over all labels (fewer labels than contour points),
        // detecting if one with count==1 exists, and only IFF one such label
        // exists searching for the point.
        // but atm, im happy that it detects "orphan"-contourPoints (without attached labelInfo)

        final Object[] copy = m_InnerContourContainer.entrySet().toArray();
        for (final Object o : copy)
        {
            @SuppressWarnings("unchecked")
            final Entry<Point, ContourParticle> vIt = (Entry<Point, ContourParticle>) o;
            final ContourParticle vWorkingIt = vIt.getValue();
            final LabelInformation info = labelMap.get(vWorkingIt.label);
            if (info == null) {
                debug("***info is null for: " + vIt.getKey());
                continue;
            }
            if (info.count == 1) {
                vWorkingIt.candidateLabel = bgLabel;
                ChangeContourPointLabelToCandidateLabel(vIt);
            }
        }
        CleanUp();
    }

    /**
     * Use only top vNbElements * m_AcceptedPointsFactor Elements.
     */
    private void FilterCandidatesContainerUsingRanks(HashMap<Point, ContourParticle> aContainer) {

        if (m_AcceptedPointsFactor >= 1) {
            // accept all
            // nothing to do here
            return;
        }

        // Copy the candidates to a set (of ContourPointWithIndex). This
        // will sort them according to their energy gradients.
        final List<ContourParticleWithIndexType> vSortedList = new LinkedList<ContourParticleWithIndexType>();

        for (final Entry<Point, ContourParticle> vPointIterator : aContainer.entrySet()) {
            final ContourParticleWithIndexType vCand = new ContourParticleWithIndexType(vPointIterator.getKey(), vPointIterator.getValue());
            vSortedList.add(vCand);
        }

        Collections.sort(vSortedList);

        int vNbElements = vSortedList.size();
        vNbElements = (int) (vNbElements * m_AcceptedPointsFactor + 0.5);

        // Fill the container with the best candidate first, then
        // the next best that does not intersect the tabu region of
        // all inserted points before.
        aContainer.clear();

        for (final ContourParticleWithIndexType vSortedListIterator : vSortedList) {
            if (!(vNbElements >= 1)) {
                break;
            }

            vNbElements--;
            // Point vCandCIndex = vSortedListIterator.pIndex;
            final Iterator<Entry<Point, ContourParticle>> vAcceptedCandIterator = aContainer.entrySet().iterator();
            final boolean vValid = true;
            while (vAcceptedCandIterator.hasNext()) {
                vAcceptedCandIterator.next().getKey();
            }
            if (vValid) {
                // This candidate passed the test and is added to the
                // TempRemoveCotainer:
                aContainer.put(vSortedListIterator.iParticleIndex, vSortedListIterator.iParticle);
            }
        }
    }

    private EnergyResult CalculateEnergyDifferenceForLabel(Point aContourIndex, ContourParticle aContourPointPtr, int aToLabel) {
        final EnergyResult result = imageModel.CalculateEnergyDifferenceForLabel(aContourIndex, aContourPointPtr, aToLabel);
        return result;
    }

    private void FreeLabelStatistics(Iterator<Entry<Integer, LabelInformation>> vActiveLabelsIt) {
        vActiveLabelsIt.remove();
    }

    void FreeLabelStatistics(int vVisitedIt) {
        labelMap.remove(vVisitedIt);
    }

    private void UpdateStatisticsWhenJump(Entry<Point, ContourParticle> aParticle, int aFromLabelIdx, int aToLabelIdx) {
        final ContourParticle vOCCV = aParticle.getValue();
        final float vCurrentImageValue = vOCCV.intensity;

        final LabelInformation aToLabel = labelMap.get(aToLabelIdx);
        final LabelInformation aFromLabel = labelMap.get(aFromLabelIdx);

        final double vNTo = aToLabel.count;
        final double vNFrom = aFromLabel.count;

        // Before changing the mean, compute the sum of squares of the
        // samples:
        final double vToLabelSumOfSq = aToLabel.var * (vNTo - 1.0) + vNTo * aToLabel.mean * aToLabel.mean;
        final double vFromLabelSumOfSq = aFromLabel.var * (aFromLabel.count - 1.0) + vNFrom * aFromLabel.mean * aFromLabel.mean;

        // Calculate the new means for the background and the label:
        final double vNewMeanToLabel = (aToLabel.mean * vNTo + vCurrentImageValue) / (vNTo + 1.0);

        // TODO divide by zero. why does this not happen at itk?
        double vNewMeanFromLabel;

        if (vNFrom > 1) {
            vNewMeanFromLabel = (vNFrom * aFromLabel.mean - vCurrentImageValue) / (vNFrom - 1.0);
        }
        else {
            vNewMeanFromLabel = 0.0;
        }

        // Calculate the new variances:
        double var;
        var = ((1.0 / (vNTo)) * (vToLabelSumOfSq + vCurrentImageValue * vCurrentImageValue - 2.0 * vNewMeanToLabel * (aToLabel.mean * vNTo + vCurrentImageValue) + (vNTo + 1.0) * vNewMeanToLabel
                * vNewMeanToLabel));
        aToLabel.var = (var);

        if (vNFrom == 2) {
            var = 0.0;
        }
        else {
            var = (1.0 / (vNFrom - 2.0))
                    * (vFromLabelSumOfSq - vCurrentImageValue * vCurrentImageValue - 2.0 * vNewMeanFromLabel * (aFromLabel.mean * vNFrom - vCurrentImageValue) + (vNFrom - 1.0) * vNewMeanFromLabel
                            * vNewMeanFromLabel);

        }

        aFromLabel.var = (var);

        // Update the means:
        aToLabel.mean = vNewMeanToLabel;
        aFromLabel.mean = vNewMeanFromLabel;

        // Add a sample point to the BG and remove it from the label-region:
        aToLabel.count++;
        aFromLabel.count--;
    }

    // Settings
    private final int AreaThreshold = 1;
    
    private void RemoveNotSignificantRegions() {
        // Iterate through the active labels and check for significance.
        for (final Entry<Integer, LabelInformation> vActiveLabelsIt : labelMap.entrySet()) {
            final int vLabelAbs = vActiveLabelsIt.getKey();
            if (vLabelAbs == bgLabel) {
                continue;
            }

            if (vActiveLabelsIt.getValue().count <= AreaThreshold) {
                RemoveFGRegion(vActiveLabelsIt.getKey());
            }
        }
        CleanUp();
    }

    private void RemoveFGRegion(int aLabel) {
        // Get the contour points of that label and copy them to vContainer:
        final HashMap<Point, ContourParticle> vContainer = new HashMap<Point, ContourParticle>();

        // find all the element with this label and store them in a tentative
        // container:
        for (final Entry<Point, ContourParticle> vIt : m_InnerContourContainer.entrySet()) {
            if (vIt.getValue().label == aLabel) {
                vContainer.put(vIt.getKey(), vIt.getValue());
            }
        }

        // Successively remove the points from the contour. In the end of the
        // loop, new points are added to vContainer.
        while (!vContainer.isEmpty()) {
            final Iterator<Entry<Point, ContourParticle>> vRemIt = vContainer.entrySet().iterator();
            while (vRemIt.hasNext()) {
                // TODO! does it really remove from first to last?
                final Entry<Point, ContourParticle> vTempIt = vRemIt.next();
                vTempIt.getValue().candidateLabel = bgLabel;
                ChangeContourPointLabelToCandidateLabel(vTempIt);
                vRemIt.remove();

            }

            // TODO ??? why should i do that? answer: ogres are like onions
            // Refill the container
            if (labelMap.get(aLabel).count > 0) {
                debug("refilling in remove fg region! count: " + labelMap.get(aLabel).count);
                // vIt = m_InnerContourContainer.entrySet();
                // vEnd = m_InnerContourContainer.end();
                for (final Entry<Point, ContourParticle> vIt : m_InnerContourContainer.entrySet()) {
                    if (vIt.getValue().label == aLabel) {
                        vContainer.put(vIt.getKey(), vIt.getValue());
                    }
                }
            }
        }
    }

    void CleanUp() {
        final Iterator<Entry<Integer, LabelInformation>> vActiveLabelsIt = labelMap.entrySet().iterator();

        while (vActiveLabelsIt.hasNext()) {
            final Entry<Integer, LabelInformation> entry = vActiveLabelsIt.next();

            if (entry.getValue().count == 0) {
                if (entry.getKey() == bgLabel) {
                    debug("bglabel in"); // System.out.println("LabelImage.CleanUp()");
                    continue;
                    // throw new
                    // RuntimeException("tried to remove bglabel in cleanUp()");
                }
                FreeLabelStatistics(vActiveLabelsIt);
            }
        }

    }

    private static void debug(@SuppressWarnings("unused") Object s) {
        //System.out.println(s);
    }

    // Control //////////////////////////////////////////////////

    private final Object pauseMonitor = new Object();
    private boolean pause = false;
    private boolean abort = false;

    /**
     * Close all created images
     */
    public void close() {
        for (int i = 0; i < OpenedImages.size(); i++) {
            OpenedImages.get(i).close();
        }
    }

    /**
     * Stops the algorithm after actual iteration
     */
    public void stop() {
        synchronized (pauseMonitor) {
            abort = true;
            pause = false;
            pauseMonitor.notify();
        }
    }

    public void pause() {
        pause = true;
    }

    public void resume() {
        synchronized (pauseMonitor) {
            pause = false;
            pauseMonitor.notify();
        }
    }

    public int getBiggestLabel() {
        return labelDispenser.getHighestLabelEverUsed();
    }

    public HashMap<Integer, LabelInformation> getLabelMap() {
        return labelMap;
    }
    
    private void fire(Point aIndex, int aNewLabel, BinarizedImage aMultiThsFunctionPtr) {
        final Set<Integer> vVisitedOldLabels = new HashSet<Integer>();
        final FloodFill ff = new FloodFill(labelImage.getConnFG(), aMultiThsFunctionPtr, aIndex);
        final Iterator<Point> vLit = ff.iterator();
        final Set<Point> vSetOfAncientContourIndices = new HashSet<Point>();

        double vSum = 0;
        double vSqSum = 0;
        int vN = 0;

        while (vLit.hasNext()) {
            final Point vCurrentIndex = vLit.next();
            final int vLabelValue = labelImage.getLabel(vCurrentIndex);
            final int absLabel = labelImage.labelToAbs(vLabelValue);
            final float vImageValue = intensityImage.get(vCurrentIndex);

            // the visited labels statistics will be removed later.
            vVisitedOldLabels.add(absLabel);

            labelImage.setLabel(vCurrentIndex, aNewLabel);

            if (labelImage.isContourLabel(vLabelValue)) {
                vSetOfAncientContourIndices.add(vCurrentIndex);
            }

            vN++;
            vSum += vImageValue;
            vSqSum += vImageValue * vImageValue;

        }

        // Delete the contour points that are not needed anymore:
        for (final Point vCurrentCIndex : vSetOfAncientContourIndices) {
            if (labelImage.isBoundaryPoint(vCurrentCIndex)) {
                final ContourParticle vPoint = m_InnerContourContainer.get(vCurrentCIndex);
                vPoint.label = aNewLabel;
                labelImage.setLabel(vCurrentCIndex, labelImage.labelToNeg(aNewLabel));
            }
            else {
                m_InnerContourContainer.remove(vCurrentCIndex);
            }
        }

        // Store the statistics of the new region (the vectors will
        // store more and more trash of old regions).
        final double vN_ = vN;

        // create a labelInformation for the new label, add to container
        final LabelInformation newLabelInformation = new LabelInformation(aNewLabel, labelImage.getDim());
        labelMap.put(aNewLabel, newLabelInformation);

        newLabelInformation.mean = vSum / vN_;
        final double var = (vN_ > 1) ? (vSqSum - vSum * vSum / vN_) / (vN_ - 1) : 0;
        newLabelInformation.var = (var);
        newLabelInformation.count = vN;

        // Clean up the statistics of non valid regions.
        for (final int vVisitedIt : vVisitedOldLabels) {
            FreeLabelStatistics(vVisitedIt);
        }

        CleanUp();
    }

}
