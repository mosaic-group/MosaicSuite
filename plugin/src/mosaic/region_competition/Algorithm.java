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

import org.apache.log4j.Logger;

import mosaic.core.binarize.BinarizedImage;
import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.image.FloodFill;
import mosaic.core.image.IntensityImage;
import mosaic.core.image.LabelImage;
import mosaic.core.image.Point;
import mosaic.core.image.RegionIterator;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.Energy.EnergyResult;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.energies.OscillationDetection;
import mosaic.region_competition.topology.TopologicalNumberImageFunction;
import mosaic.region_competition.topology.TopologicalNumberImageFunction.TopologicalNumberResult;


public class Algorithm {

    private static final Logger logger = Logger.getLogger(Algorithm.class);

    // Input for Algorithm
    private final LabelImage iLabelImage;
    private final IntensityImage iIntensityImage;
    private final ImageModel iImageModel;
    private final Settings iSettings;

    // Just aliases for stuff from labelImage
    private final static int BGLabel = LabelImage.BGLabel;

    private boolean shrinkFirst = false;
    // TODO: This var is changed only in OscilationDetection class... It should be moved there or sth.
    public float m_AcceptedPointsFactor = AcceptedPointsFactor;

    private final HashMap<Point, ContourParticle> iContourParticles = new HashMap<Point, ContourParticle>();
    private final HashMap<Integer, LabelStatistics> iLabelStatistics = new HashMap<Integer, LabelStatistics>();
    private final HashMap<Point, LabelPair> iCompetingRegions = new HashMap<Point, LabelPair>();
    private final HashMap<Point, ContourParticle> iCandidates = new HashMap<Point, ContourParticle>();

    private final LabelDispenser labelDispenser = new LabelDispenser();
    private final OscillationDetection oscillationDetection;
    private final TopologicalNumberImageFunction m_TopologicalNumberFunction;

    // Settings
    private static final float AcceptedPointsFactor = 1;
    private static final boolean RemoveNonSignificantRegions = true;
    private static final int MinimumAreaSize = 1;

    private class Seed {
        private final Point iPoint;
        private final Integer iLabel;

        protected Seed(Point aPoint, Integer aLabel) {
            iPoint = aPoint;
            iLabel = aLabel;
        }

        protected Point getPoint() {return iPoint;}
        protected Integer getLabel() {return iLabel;}

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((iPoint == null) ? 0 : iPoint.hashCode());
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
            if (iPoint == null && other.iPoint != null) return false;
            else if (!iPoint.equals(other.iPoint)) return false;
            return true;
        }
    }

    public Algorithm(IntensityImage aIntensityImage, LabelImage aLabelImage, ImageModel aModel, Settings aSettings) {
        iLabelImage = aLabelImage;
        iIntensityImage = aIntensityImage;
        iImageModel = aModel;
        iSettings = aSettings;

        oscillationDetection = new OscillationDetection(this, iSettings);
        m_TopologicalNumberFunction = new TopologicalNumberImageFunction(iLabelImage);

        // Initialize label image
        iLabelImage.initBoundary();
        List<Point> contourPoints = iLabelImage.initContour();
        initContourContainer(contourPoints);

        // Initialize standard statistics (mean, variances, length, area etc)
        initStatistics();

        // Depending on the functional to use, prepare stuff for faster computation.
        initEnergies();
    }

    /**
     * Find all contour points in LabelImage (and mark them as a contour => -labelValue) Creates ContourParticle for every contour point and stores it in container.
     */
    private void initContourContainer(List<Point> aContourPoints) {
        for (Point point : aContourPoints) {
            final ContourParticle particle = new ContourParticle();
            particle.label = iLabelImage.getLabelAbs(point);
            particle.intensity = iIntensityImage.get(point);
            iContourParticles.put(point, particle);
        }
    }

    /**
     * Initializes statistics. For each found label creates LabelStatistics object and stores it in labelStatistics container.
     */
    private void initStatistics() {
        // First create all LabelStatistics for each found label and calculate values needed for later
        // variance/mean calculations
        int maxUsedLabel = 0;
        for (int i = 0; i < iLabelImage.getSize(); i++) {
            final int absLabel = iLabelImage.getLabelAbs(i);

            if (!iLabelImage.isForbiddenLabel(absLabel)) {
                if (maxUsedLabel < absLabel) maxUsedLabel = absLabel;

                LabelStatistics stats = iLabelStatistics.get(absLabel);
                if (stats == null) {
                    stats = new LabelStatistics(absLabel, iLabelImage.getNumOfDimensions());
                    iLabelStatistics.put(absLabel, stats);
                }
                final double val = iIntensityImage.get(i);
                stats.count++;
                stats.mean += val;
                stats.var += val * val;
            }
        }

        // If background label do not exist add it to collection
        LabelStatistics stats = iLabelStatistics.get(BGLabel);
        if (stats == null) {
            stats = new LabelStatistics(BGLabel, iLabelImage.getNumOfDimensions());
            iLabelStatistics.put(BGLabel, stats);
        }

        // Finally - calculate variance, median and mean for each found label
        for (final LabelStatistics stat : iLabelStatistics.values()) {
            final int n = stat.count;
            stat.var = n > 1 ? ((stat.var - stat.mean * stat.mean / n) / (n - 1)) : 0;
            stat.mean = n > 0 ? (stat.mean / n) : 0;
            // Median on start set equal to mean
            stat.median = stat.mean;
        }

        // Make sure that labelDispenser will not produce again any already used label
        // Safe to search with 'max' we have at least one value in container (background)
        labelDispenser.setMaxValueOfUsedLabel(maxUsedLabel);
    }

    /**
     * @return value of biggest label ever used
     */
    public int getBiggestLabel() {
        return labelDispenser.getHighestLabelEverUsed();
    }

    /**
     * @return container with statistics per each label
     */
    public HashMap<Integer, LabelStatistics> getLabelStatistics() {
        return iLabelStatistics;
    }

    /**
     * Initialize the energy function
     */
    private void initEnergies() {
        /**
         * Deconvolution: - Alocate and initialize the 'ideal image'
         */
        if (iSettings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
            // TODO: This is not OOP, handling energies should be redesigned
            // Deconvolution: Allocate and initialize the 'ideal image'
            ((E_Deconvolution) iImageModel.getEdata()).GenerateModelImage(iLabelImage, iLabelStatistics);
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
    }

    /**
     * Calculates the center of mass of each region (for each label) TODO: should mean_pos be part of LabelStatistics and this method here? It is one time use from RC
     */
    public void calculateRegionsCenterOfMass() {
        // Reset mean position for all labels
        for (final LabelStatistics labelStats : iLabelStatistics.values()) {
            for (int i = 0; i < labelStats.mean_pos.length; ++i) {
                labelStats.mean_pos[i] = 0.0;
            }
        }

        // Iterate through whole label image and update mean position (only sum all coordinate values)
        final RegionIterator ri = new RegionIterator(iLabelImage.getDimensions());
        while (ri.hasNext()) {
            ri.next();
            final Point point = ri.getPoint();
            int label = iLabelImage.getLabel(point);
            final LabelStatistics labelStats = iLabelStatistics.get(iLabelImage.labelToAbs(label));

            if (labelStats != null) {
                for (int i = 0; i < point.getNumOfDimensions(); ++i) {
                    labelStats.mean_pos[i] += point.iCoords[i];
                }
            }
            else {
                // There should be statistics for all labels excluding only forbidden label.
                if (!iLabelImage.isForbiddenLabel(label)) {
                    logger.error("Cound not find label statistics for label: " + label + " at: " + point);
                }
            }
        }

        // Iterate through all label statistics and calculate center of mass basing on previous calcuation
        for (final LabelStatistics labelStats : iLabelStatistics.values()) {
            for (int i = 0; i < labelStats.mean_pos.length; ++i) {
                labelStats.mean_pos[i] /= labelStats.count;
            }
        }
    }

    /**
     * Removes from statistics labels with 'count == 0'
     */
    private void removeEmptyStatistics() {
        final Iterator<Entry<Integer, LabelStatistics>> labelStatsIt = iLabelStatistics.entrySet().iterator();

        while (labelStatsIt.hasNext()) {
            final Entry<Integer, LabelStatistics> entry = labelStatsIt.next();
            if (entry.getValue().count == 0) {
                if (entry.getKey() != BGLabel) {
                    labelStatsIt.remove();
                    continue;
                }
                logger.error("Tried to remove background label from label statistics!");
            }
        }
    }

    /**
     * Neighbors of point aPoint with aAbsLabel are changed to contour particles and added to CountourParicles container.
     * 
     * @param aPoint changing point
     * @param aAbsLabel old label of this point
     */
    private void changeNeighboursOfParticleToCountour(int aAbsLabel, Point aPoint) {
        if (iLabelImage.isContourLabel(aAbsLabel)) {
            logger.error("AddNeighborsAtRemove. one label is not absLabel " + aAbsLabel + " at " + aPoint);
        }

        for (final Integer p : iLabelImage.iterateNeighbours(aPoint)) {
            final int label = iLabelImage.getLabel(p);
            if (iLabelImage.isInnerLabel(label) && label == aAbsLabel) {
                // q is a inner point with the same label as p
                final ContourParticle q = new ContourParticle();
                q.label = aAbsLabel;
                q.candidateLabel = BGLabel;
                q.intensity = iIntensityImage.get(p);
                iLabelImage.setLabel(p, iLabelImage.labelToNeg(aAbsLabel));
                iContourParticles.put(iLabelImage.iIterator.indexToPoint(p), q);
            }
        }
    }
    
    /**
     * If neighbours of changed particle are enclosed, remove them from ContourParticles container and change their
     * type to interior.
     */
    private void removeEnclosedNeighboursFromContour(int aLabelAbs, Point aPoint) {
        for (final int qIndex : iLabelImage.iterateNeighbours(aPoint)) {
            if (iLabelImage.getLabel(qIndex) == iLabelImage.labelToNeg(aLabelAbs) && iLabelImage.isEnclosedByLabel(qIndex, aLabelAbs)) {
                iContourParticles.remove(iLabelImage.iIterator.indexToPoint(qIndex));
                iLabelImage.setLabel(qIndex, aLabelAbs);
            }
        }

        // TODO: Investigate cases it might happen and put comment
        if (iLabelImage.isEnclosedByLabel(aPoint, aLabelAbs)) {
            iContourParticles.remove(aPoint);
            iLabelImage.setLabel(aPoint, aLabelAbs);
        }
    }

    /**
     * Change CountourPoint label to candidate label. Perform needed cleanup around like changing neighbor particles
     * to inner or to contour points if needed.
     */
    private void changeContourPointLabelToCandidateLabelAndUpdateNeighbours(Entry<Point, ContourParticle> aParticle) {
        final Point point = aParticle.getKey();
        final ContourParticle contourParticle = aParticle.getValue();

        final int fromLabel = contourParticle.label;
        final int toLabel = contourParticle.candidateLabel;
        float intensity = contourParticle.intensity;

        // Update the label image. The new point is either a contour point or 0,
        // therefore the negative label value is set.
        iLabelImage.setLabel(point, iLabelImage.labelToNeg(toLabel));

        // Update the statistics of the propagating and the loser region.
        updateLabelStatistics(intensity, fromLabel, toLabel);
        if (iImageModel.getEdataType() == EnergyFunctionalType.e_DeconvolutionPC) {
            ((E_Deconvolution) iImageModel.getEdata()).UpdateConvolvedImage(point, fromLabel, toLabel, iLabelStatistics);
        }

        // TODO: A bit a dirty hack: we store the old label for the relabeling procedure later on...
        // either introduce a new variable or rename the variable (which doesn't work currently :-).
        contourParticle.candidateLabel = fromLabel;

        // ---------------- Clean up neighborhood -------------------

        // The loser region (if it is not the BG region) has to add the
        // neighbors of the lost point to the contour list.
        if (fromLabel != BGLabel) {
            changeNeighboursOfParticleToCountour(fromLabel, point);
        }

        // Erase the point from the surface container in case it now belongs to the background.
        // Otherwise add the point to the container (or replace it in case it has been there already).
        if (toLabel == BGLabel) {
            iContourParticles.remove(point);
        }
        else {
            contourParticle.label = toLabel;
            // The point may or may not exist already in the m_InnerContainer.
            // The old value, if it exist, is just overwritten with the new contour point (with a new label).
            iContourParticles.put(point, contourParticle);

            // Remove 'enclosed' contour points from the container. For the BG this makes no sense.
            removeEnclosedNeighboursFromContour(toLabel, point);
        }
    }
    
    /**
     * Find all contour particles with size = 1, and remove them (they become background)
     */
    private void removeSinglePointRegions() {
        // TODO: here we go first through contour points to find different labels
        // and then checking for labelInfo.count==1.
        // instead, we could iterate over all labels (fewer labels than contour points),
        // detecting if one with count==1 exists, and only IFF one such label
        // exists searching for the point.
        // but atm, im happy that it detects "orphan"-contourPoints (without attached labelInfo)

        // TODO: It must be converted to array since later code modifies HashMap (!) by adding/removing
        // entries in ChangeContourPointLabelToCandidateLabel.
        // Without such "hack" it generates ConcurrentModificationException. Anyway.. this
        // "solution" must be revisited.
        Object[] array = iContourParticles.entrySet().toArray();
        for (Object o : array) {
            @SuppressWarnings("unchecked")
            Entry<Point, ContourParticle> vIt = (Entry<Point, ContourParticle>) o;
            final ContourParticle contourParticle = vIt.getValue();
            final LabelStatistics labelStat = iLabelStatistics.get(contourParticle.label);
            if (labelStat == null) {
                logger.error("There is not label statistics for label: " + contourParticle.label + " at " + vIt.getKey());
                continue;
            }
            else if (labelStat.count == 1) {
                contourParticle.candidateLabel = BGLabel;
                changeContourPointLabelToCandidateLabelAndUpdateNeighbours(vIt);
            }
        }
        removeEmptyStatistics();
    }
    
    /**
     * Removes all regions with size less than MinimuAreaSize
     */
    private void removeNotSignificantRegions() {
        // Iterate through the active labels and check for significance.
        for (final Entry<Integer, LabelStatistics> labelStats : iLabelStatistics.entrySet()) {
            final int label = labelStats.getKey();
            if (label == BGLabel) {
                continue;
            }
            else if (labelStats.getValue().count <= MinimumAreaSize) {
                removeRegion(label);
            }
        }
        removeEmptyStatistics();
    }

    /**
     * Removes region marked with aLabel
     * TODO: Cannot it just be done by looping over CountourParticles and LabelImage and removing/setting 
     *       things to background?
     * @param aLabel
     */
    private void removeRegion(int aLabel) {
        // Find all the element with this label and store them in a tentative container
        final HashMap<Point, ContourParticle> labelParticles = new HashMap<Point, ContourParticle>();
        for (final Entry<Point, ContourParticle> particleIt : iContourParticles.entrySet()) {
            if (particleIt.getValue().label == aLabel) {
                labelParticles.put(particleIt.getKey(), particleIt.getValue());
            }
        }

        // Successively remove the points from the contour
        while (!labelParticles.isEmpty()) {
            final Iterator<Entry<Point, ContourParticle>> iter = labelParticles.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<Point, ContourParticle> tmp = iter.next();
                tmp.getValue().candidateLabel = BGLabel;
                changeContourPointLabelToCandidateLabelAndUpdateNeighbours(tmp);
                iter.remove();
            }

            // After above loop only outside "layer" of region was removed. Proceed until all stuff is removed.
            if (iLabelStatistics.get(aLabel).count > 0) {
                for (final Entry<Point, ContourParticle> particleIt : iContourParticles.entrySet()) {
                    if (particleIt.getValue().label == aLabel) {
                        labelParticles.put(particleIt.getKey(), particleIt.getValue());
                    }
                }
            }
        }
    }

    /**
     * Limits number of candidates to top (basing on energy values) elements with number calculated as a:
     *      numOfCandidates * iAcceptedPointsFactor 
     */
    private void limitNumberOfCandidates() {
        if (m_AcceptedPointsFactor >= 1) {
            // accept all - nothing to do here
            return;
        }

        // Copy the candidates to a set (of ContourPointWithIndex). This
        // will sort them according to their energy gradients.
        final List<ContourParticleWithIndex> sortedCandidates = new LinkedList<ContourParticleWithIndex>();
        for (final Entry<Point, ContourParticle> iter : iCandidates.entrySet()) {
            final ContourParticleWithIndex candidate = new ContourParticleWithIndex(iter.getKey(), iter.getValue());
            sortedCandidates.add(candidate);
        }
        Collections.sort(sortedCandidates);

        // Fill the container with the best candidate first
        iCandidates.clear();
        int vNbElements =  (int) (sortedCandidates.size() * m_AcceptedPointsFactor + 0.5);

        for (final ContourParticleWithIndex vSortedListIterator : sortedCandidates) {
            if (vNbElements-- < 1) {
                break;
            }
            // This candidate passed the test and is added to the TempRemoveCotainer:
            iCandidates.put(vSortedListIterator.iParticleIndex, vSortedListIterator.iParticle);
        }
    }
    
    /**
     * Relabels to aNewLabel region given in aAreaWithOldLabels.
     * @param aPoint - start point for relabeling
     * @param aNewLabel - value of new label
     * @param aAreaWithOldLabels - input with set thresholds with old labels.
     */
    private void relabelRegionAtPoint(Point aPoint, int aNewLabel, BinarizedImage aAreaWithOldLabels) {
        final Set<Integer> oldLabels = new HashSet<Integer>();
        final Set<Point> oldContours = new HashSet<Point>();
        final Iterator<Integer> regionIterator = new FloodFill(iLabelImage, aAreaWithOldLabels, aPoint).iteratorIndex();
        
        double sumOfVal = 0;
        double sumOfSqVal = 0;
        int count = 0;

        while (regionIterator.hasNext()) {
            final int currentPoint = regionIterator.next();
            final int oldLabel = iLabelImage.getLabel(currentPoint);

            iLabelImage.setLabel(currentPoint, aNewLabel);

            // the visited labels statistics will be removed later.
            oldLabels.add(iLabelImage.labelToAbs(oldLabel));
            if (iLabelImage.isContourLabel(oldLabel)) {
                oldContours.add(iLabelImage.iIterator.indexToPoint(currentPoint));
            }

            final float val = iIntensityImage.get(currentPoint);
            sumOfVal += val;
            sumOfSqVal += val * val;
            count++;
        }

        // Delete the contour points that are not needed anymore (they are now internal points) 
        // or relabel them to the new label value if they should be kept.
        for (final Point p : oldContours) {
            if (iLabelImage.isBoundaryPoint(p)) {
                final ContourParticle contourPoint = iContourParticles.get(p);
                contourPoint.label = aNewLabel;
                iLabelImage.setLabel(p, iLabelImage.labelToNeg(aNewLabel));
            }
            else {
                iContourParticles.remove(p);
            }
        }

        // Create a LabelStatistics for the new label and add it to container
        final LabelStatistics newLabelStats = new LabelStatistics(aNewLabel, iLabelImage.getNumOfDimensions());
        newLabelStats.mean = sumOfVal / count;
        newLabelStats.var = (count > 1) ? (sumOfSqVal - sumOfVal * sumOfVal / count) / (count - 1) : 0;
        newLabelStats.count = count;
        iLabelStatistics.put(aNewLabel, newLabelStats);

        // Clean up the statistics of non valid regions.
        for (final int oldLabel : oldLabels) {
            iLabelStatistics.remove(oldLabel);
        }
        removeEmptyStatistics();
    }
    
    /**
     * Relabels adjacent regions if they are on iCompetingRegions list. If any of labels is equal to aLabel, second one
     * will be processed as a merging area. And again second one will be checked again to find next possible region to be merged and so on.
     * @param aPoint - start point where merge should start
     * @param aLabel - label of region to be merged
     * @param aCheckedLabels - container to be updated with merged regions labels
     */
    private void relabelMergedRegions(Point aPoint, int aLabel, Set<Integer> aCheckedLabels) {
        final BinarizedIntervalLabelImage labelArea = new BinarizedIntervalLabelImage(iLabelImage);
        final Stack<Integer> labelsToCheck = new Stack<Integer>();

        addNewThreshold(aCheckedLabels, labelArea, labelsToCheck, aLabel);
        while (!labelsToCheck.isEmpty()) {
            final int labelToCheck = labelsToCheck.pop();
            for (final LabelPair mergingPair : iCompetingRegions.values()) {
                final int label1 = mergingPair.first;
                final int label2 = mergingPair.second;
                
                // If any of merging labels is equal to labelToCheck add second to containers and add new threshold for labelArea
                if (label1 == labelToCheck) addNewThreshold(aCheckedLabels, labelArea, labelsToCheck, label2);
                if (label2 == labelToCheck) addNewThreshold(aCheckedLabels, labelArea, labelsToCheck, label1);
            }
        }
        
        // Verify if aPoint is part of labelArea and relabel region around
        if (labelArea.EvaluateAtIndex(aPoint)) {
            relabelRegionAtPoint(aPoint, labelDispenser.getNewLabel(), labelArea);
        }
    }

    /**
     * If aLabelToAdd is not in aCheckedLabels this method adds new threshold (for positive and negative label value) to aLabelArea.
     * It also updates aCheckedLabels and aLabelsToCheck containers with aLabelToAdd.
     */
    private void addNewThreshold(Set<Integer> aCheckedLabels, final BinarizedIntervalLabelImage aLabelArea, final Stack<Integer> aLabelsToCheck, final int aLabelToAdd) {
        if (!aCheckedLabels.contains(aLabelToAdd)) {
            aLabelArea.AddOneValThreshold(aLabelToAdd);
            aLabelArea.AddOneValThreshold(iLabelImage.labelToNeg(aLabelToAdd));
            aCheckedLabels.add(aLabelToAdd);
            aLabelsToCheck.push(aLabelToAdd);
        }
    }
    
    /**
     * The function relabels a region with (+/-) aLabel values starting from the position aStartPoint. 
     * This method assumes the label image to be updated. 
     */
    private void relabelRegion(Point aStartPoint, int aLabel) {
        if (iLabelImage.getLabelAbs(aStartPoint) == aLabel) {
            final BinarizedIntervalLabelImage labelArea = new BinarizedIntervalLabelImage(iLabelImage);
            labelArea.AddOneValThreshold(aLabel);
            labelArea.AddOneValThreshold(iLabelImage.labelToNeg(aLabel));
            relabelRegionAtPoint(aStartPoint, labelDispenser.getNewLabel(), labelArea);
        }
    }
    
    /**
     * Removes from candidate list particles which have energyDifference >= 0
     */
    private void removeCandidatesWithNonNegativeDeltaEnergy() {
        final Iterator<Entry<Point, ContourParticle>> iter = iCandidates.entrySet().iterator();
        while (iter.hasNext()) {
            ContourParticle particle = iter.next().getValue();
            if (particle.energyDifference >= 0) {
                iter.remove();
            }
        }
    }
    
    ////////////////////////////////////////////////////////////
    // ---------------------------------------------------------
    ////////////////////////////////////////////////////////////

    public boolean performIteration() {
        if (iSettings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
        
        if (RemoveNonSignificantRegions) {
            removeSinglePointRegions();
            removeNotSignificantRegions();
        }
        
        RebuildCandidateList();
        FilterCandidates();
        oscillationDetection.DetectOscillations(iCandidates);
        limitNumberOfCandidates();
        boolean convergence = MoveCandidates();
        removeEmptyStatistics();

        if (shrinkFirst && convergence) {
            // Done with shrinking, now allow growing
            convergence = false;
            shrinkFirst = false;
            m_AcceptedPointsFactor = AcceptedPointsFactor;
        }

        return convergence;
    }

    /**
     * Move the points in the candidate list
     */
    private boolean MoveCandidates() {
        // We first move all the FG-simple points. We do that because it happens that points that are not simple 
        // at the first place get simple after the change of other points. The non-simple points will be treated
        // in a separate loop afterwards.
        boolean vChange = true;
        boolean vConvergence = true;
        final Set<Seed> seeds = new HashSet<Seed>();
        
        while (vChange && !iCandidates.isEmpty()) {
            vChange = false;

            final Iterator<Entry<Point, ContourParticle>> vPointIterator = iCandidates.entrySet().iterator();
            while (vPointIterator.hasNext()) {
                final Entry<Point, ContourParticle> vStoreIt = vPointIterator.next();
                final Point vCurrentIndex = vStoreIt.getKey();

                List<TopologicalNumberResult> vFGTNvector = m_TopologicalNumberFunction.EvaluateAdjacentRegionsFGTNAtIndex(vCurrentIndex);
                boolean vSimple = true;
                // Check for FG-simplicity:
                for (final TopologicalNumberResult vTopoNbItr : vFGTNvector) {
                    if (vTopoNbItr.topologicalNumberPair.FGNumber != 1 || vTopoNbItr.topologicalNumberPair.BGNumber != 1) {
                        // This is a FG simple point; perform the move.
                        vSimple = false;
                    }
                }
                if (vSimple) {
                    vChange = true;
                    changeContourPointLabelToCandidateLabelAndUpdateNeighbours(vStoreIt);
                    vPointIterator.remove();
                    vConvergence = false;
                }
                // we will reuse the processed flag to indicate if a particle is a seed
                vStoreIt.getValue().isProcessed = false;
            }
        }

        // Now we know that all the points in the list are 'currently' not simple.
        // We move them anyway (if topological constraints allow) but record
        // (for every particle) where to relabel (using the seed set). Placing
        // the seed is necessary for every particle to ensure relabeling even
        // if a bunch of neighboring particles change. The seed will be ignored
        // later on if the corresponding FG region is not present in the neighborhood anymore.
        // TODO: The following code is dependent on the iteration order if splits/handles
        // are not allowed. A solution would be to sort the candidates beforehand.
        // This should be computationally not too expensive since we assume there
        // are not many non-simple points.
        final Iterator<Entry<Point, ContourParticle>> vPointIterator = iCandidates.entrySet().iterator();

        while (vPointIterator.hasNext()) {
            final Entry<Point, ContourParticle> e = vPointIterator.next();
            final ContourParticle vStoreIt = e.getValue();
            final int vCurrentLabel = vStoreIt.label;
            final int vCandidateLabel = vStoreIt.candidateLabel;
            final Point vCurrentIndex = e.getKey();

            boolean vValidPoint = true;

            List<TopologicalNumberResult> vFGTNvector = m_TopologicalNumberFunction.EvaluateAdjacentRegionsFGTNAtIndex(vCurrentIndex);

            // Check for handles:
            // if the point was not disqualified already and we disallow introducing handles (not only self fusion!), 
            // we check if there is an introduction of a handle.
            if (vValidPoint && !iSettings.m_AllowHandles) {
                for (final TopologicalNumberResult vTopoNbItr : vFGTNvector) {
                    if (vTopoNbItr.label == vCandidateLabel) {
                        if (vTopoNbItr.topologicalNumberPair.FGNumber > 1) {
                            vValidPoint = false;
                        }
                    }

                    // criterion to detect surface points (only 3D?)
                    if (vTopoNbItr.topologicalNumberPair.FGNumber == 1 && vTopoNbItr.topologicalNumberPair.BGNumber > 1) {
                        vValidPoint = false;
                    }
                }
            }

            // Check for splits:
            // This we have to do either to forbid
            // the change in topology or to register the seed point for relabeling.
            // if the point was not disqualified already and we disallow splits, then we check if the 'old' label undergoes a split.
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
                    if (iSettings.m_AllowFission) {
                        registerNeighbourSeedsWithSameLabel(seeds, vCurrentIndex, vCurrentLabel);
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
                changeContourPointLabelToCandidateLabelAndUpdateNeighbours(e);
                vConvergence = false;

                if (e.getValue().isProcessed) {
                    registerNeighbourSeedsWithSameLabel(seeds, vCurrentIndex, vCurrentLabel);
                    if (!seeds.remove(new Seed(vCurrentIndex, vCurrentLabel))) {
                        throw new RuntimeException("no seed in set");
                    }
                }
            }

            if (iCandidates.containsKey(vCurrentIndex)) {
                vPointIterator.remove();
            }
        }

        // Perform relabeling of the regions that did a split:
        boolean didSplitOrMerge = false;

        for (final Seed vSeedIt : seeds) {
            relabelRegion(vSeedIt.getPoint(), vSeedIt.getLabel());
            didSplitOrMerge = true;
        }

        // Merge the the competing regions if they meet merging criterion.
        if (iSettings.m_AllowFusion) {
            final Set<Integer> vCheckedLabels = new HashSet<Integer>();
            for (final Entry<Point, LabelPair> vCRit : iCompetingRegions.entrySet()) {
                final Point idx = vCRit.getKey();
                relabelMergedRegions(idx, vCRit.getValue().first, vCheckedLabels);
                didSplitOrMerge = true;
            }
        }

        if (didSplitOrMerge) {
            if (iSettings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
                ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
            }
        }

        return vConvergence;
    }

    private void registerNeighbourSeedsWithSameLabel(Set<Seed> aSeeds, Point aPoint, int aLabel) {
        for (final Integer neighbour : iLabelImage.iterateNeighbours(aPoint)) {
            final int label = iLabelImage.getLabelAbs(neighbour);
            final Point neighbourPoint = iLabelImage.iIterator.indexToPoint(neighbour);
            
            if (label == aLabel) {
                aSeeds.add(new Seed(neighbourPoint, label));
                // At the position where we put the seed, inform the particle
                // that it has to inform its neighbor in case it moves (if there
                // is a particle at all at this spot; else we don't have a problem
                // because the label will not move at the spot and therefore
                // the seed will be effective).
                final ContourParticle contourParticle = iCandidates.get(neighbourPoint);
                if (contourParticle != null) {
                    contourParticle.isProcessed = true;
                }
            }
        }
    }

    /**
     * Iterates through ContourParticles in iContourParticles
     * Builds mothers, daughters, computes energies and fills m_CompetingRegionsMap
     */
    private void RebuildCandidateList() {
        // Put all current contour particles into candidates
        iCandidates.clear();
        iCandidates.putAll(iContourParticles);
        
        // Calculate energy for change to BG (shrinking)
        for (final Entry<Point, ContourParticle> iter : iContourParticles.entrySet()) {
            final Point point = iter.getKey();
            final ContourParticle contour = iter.getValue();

            contour.candidateLabel = 0;
            contour.isMother = true;
            contour.isDaughter = false;
            contour.isProcessed = false;
            contour.referenceCount = 0; // doesn't matter for the BG
            contour.energyDifference = iImageModel.calculateDeltaEnergy(point, contour, BGLabel, iLabelStatistics).energyDifference;
            contour.clearLists();
            contour.setTestedLabel(BGLabel);
        }

        if (shrinkFirst) {
            return;
        }

        // Iterate the contour list and visit all the neighbors in the FG-Neighborhood.
        // Calculate energy for expanding into neighborhood (growing)
        // Until that point iContourParticles = iCandidates, we keep iterating on first one since iCandidates will grow (possibly)
        iCompetingRegions.clear();
        for (final Entry<Point, ContourParticle> iter : iContourParticles.entrySet()) {
            final Point point = iter.getKey();
            ContourParticle contour = iter.getValue();
            final int propagatingRegionLabel = contour.label;

            for (final Integer neighbor : iLabelImage.iterateNeighbours(point)) {
                final int labelOfDefender = iLabelImage.getLabelAbs(neighbor);
                if (iLabelImage.isForbiddenLabel(labelOfDefender) || labelOfDefender == propagatingRegionLabel) {
                    // Skip forbidden and same region labels
                    continue;
                }
                final Point neighbourPoint = iLabelImage.iIterator.indexToPoint(neighbor);
                contour.getDaughterList().add(neighbourPoint);
                
                final ContourParticle contourCandidate = iCandidates.get(neighbourPoint);
                if (contourCandidate == null) {
                    // Neighbor is a background
                    final ContourParticle newContour = new ContourParticle();
                    newContour.candidateLabel = propagatingRegionLabel;
                    newContour.label = labelOfDefender;
                    newContour.intensity = iIntensityImage.get(neighbor);
                    newContour.isMother = false;
                    newContour.isDaughter = true;
                    newContour.isProcessed = false;
                    newContour.referenceCount = 1;
                    newContour.energyDifference = iImageModel.calculateDeltaEnergy(neighbourPoint, newContour, propagatingRegionLabel, iLabelStatistics).energyDifference;
                    newContour.setTestedLabel(propagatingRegionLabel);
                    newContour.getMotherList().add(point);
                    iCandidates.put(neighbourPoint, newContour);
                }
                else {
                    // Neighbor is another region contour
                    contourCandidate.isDaughter = true;
                    contourCandidate.getMotherList().add(point);

                    // Check if the energy difference for this candidate label has not yet been calculated.
                    if (!contourCandidate.hasLabelBeenTested((propagatingRegionLabel))) {
                        contourCandidate.setTestedLabel(propagatingRegionLabel);

                        final EnergyResult energyResult = iImageModel.calculateDeltaEnergy(neighbourPoint, contourCandidate, propagatingRegionLabel, iLabelStatistics);
                        if (energyResult.energyDifference < contourCandidate.energyDifference) {

                            contourCandidate.candidateLabel = propagatingRegionLabel;
                            contourCandidate.energyDifference = energyResult.energyDifference;
                            contourCandidate.referenceCount = 1;

                            if (energyResult.merge && contourCandidate.label != BGLabel && contourCandidate.candidateLabel != BGLabel) {
                                iCompetingRegions.put(point, new LabelPair(contourCandidate.candidateLabel, contourCandidate.label));
                            }
                        }
                    }
                    else {
                        // If the propagatingRegionLabel is the same as the candidateLabel, we have found 2 or more mothers of for this contour point.
                        if (contourCandidate.candidateLabel == propagatingRegionLabel) {
                            contourCandidate.referenceCount++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Filters topological incompatible candidates (topological dependencies) and non-improving energies.
     */
    private void FilterCandidates() {
        if (shrinkFirst) {
            removeCandidatesWithNonNegativeDeltaEnergy();
            return;
        }

        /**
         * Find topologically compatible candidates
         */
        final List<Point> vIllegalIndices = new LinkedList<Point>();
        for (final Entry<Point, ContourParticle> vPointIterator : iCandidates.entrySet()) {
            final Point pIndex = vPointIterator.getKey();
            final ContourParticle contourParticle = vPointIterator.getValue();

            // Check if this point already was processed
            if (!contourParticle.isProcessed) {
                // Check if it is a mother: only mothers can be seed points of topological networks. 
                // Daughters are always part of a topo network of a mother.
                if (!contourParticle.isMother) {
                    continue;
                }
                contourParticle.isProcessed = true;

                 // Build the dependency network for this seed point:
                final List<ContourParticleWithIndex> vSortedNetworkMembers = buildDependencyNetwork(pIndex);

                // Filtering: Accept all members in ascending order that are compatible with the already selected members in the network.
                final HashSet<Point> vSelectedCandidateIndices = new HashSet<Point>();

                for (final ContourParticleWithIndex vNetworkIt : vSortedNetworkMembers) {

                    // If a mother is accepted, the reference count of all the daughters (with the same label) has to be decreased.
                    // Rules: a candidate in the network is a legal candidate if:
                    // - If (daughter): The reference count >= 1. (Except the the candidate label is the BG - this allows creating BG regions in between two competing regions).
                    // - If ( mother ): All daughters (with the same 'old' label) in the accepted list have still a reference count > 1.
                    boolean vLegalMove = true;

                    // RULE 1: If c is a daughter point, the reference count r_c is > 0.
                    if (vNetworkIt.iParticle.isDaughter) {
                        final ContourParticle vCand = iCandidates.get(vNetworkIt.iParticleIndex);
                        if (vCand.referenceCount < 1 && vCand.candidateLabel != 0) {
                            vLegalMove = false;
                        }
                    }

                    // RULE 2: All daughters already accepted the label of this mother have at least one another mother.
                    // AND
                    // RULE 3: Mothers are still valid mothers (to not introduce holes in the FG region).
                    if (vLegalMove && vNetworkIt.iParticle.isMother) {
                        // Iterate the daughters and check their reference count
                        boolean vRule3Fulfilled = false;

                        for (final Point vDaughterIndicesIterator : vNetworkIt.iParticle.getDaughterList()) {
                            final ContourParticle vDaughterPoint = iCandidates.get(vDaughterIndicesIterator);

                            // rule 2:
                            final boolean vAcceptedDaugtherItContained = vSelectedCandidateIndices.contains(vDaughterIndicesIterator);
                            if (vAcceptedDaugtherItContained) {
                                // This daughter has been accepted and needs a reference count > 1, else the move is invalid.
                                if (vDaughterPoint.candidateLabel == vNetworkIt.iParticle.label && vDaughterPoint.referenceCount <= 1) {
                                    vLegalMove = false;
                                    break;
                                }
                            }

                            // rule 3:
                            if (!vRule3Fulfilled) {
                                if (!vAcceptedDaugtherItContained) {
                                    // There is a daughter that has not yet been accepted.
                                    vRule3Fulfilled = true;
                                }
                                else {
                                    // the daughter has been accepted, but may have another candidate label(rule 3b):
                                    if (iCandidates.get(vDaughterIndicesIterator).candidateLabel != vNetworkIt.iParticle.label) {
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

                        // decrease the references of its daughters(with the same 'old' label).
                        for (final Point vDaughterIndicesIterator : vNetworkIt.iParticle.getDaughterList()) {
                            final ContourParticle vDaughterPoint = iCandidates.get(vDaughterIndicesIterator);
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
            iCandidates.remove(vIlligalIndicesIt);
        }

        removeCandidatesWithNonNegativeDeltaEnergy();
    }

    private List<ContourParticleWithIndex> buildDependencyNetwork(final Point aStartPoint) {
        final Stack<Point> pointsToVisit = new Stack<Point>();
        final List<ContourParticleWithIndex> networkMembers = new LinkedList<ContourParticleWithIndex>();
        
        pointsToVisit.push(aStartPoint);
        while (!pointsToVisit.empty()) {
            final Point point = pointsToVisit.pop();
            final ContourParticle currentMother = iCandidates.get(point);

            // Add the seed point to the network
            networkMembers.add(new ContourParticleWithIndex(point, currentMother));

            // Iterate all children of the seed, push to the stack if there is a mother.
            for (final Point daughterPoint : currentMother.getDaughterList()) {
                final ContourParticle daughterContourPoint = iCandidates.get(daughterPoint);

                if (!daughterContourPoint.isProcessed) {
                    daughterContourPoint.isProcessed = true;

                    if (daughterContourPoint.isMother) {
                        pointsToVisit.push(daughterPoint);
                    }
                    else {
                        networkMembers.add(new ContourParticleWithIndex(daughterPoint, daughterContourPoint));
                    }

                    // Push all the non-processed mothers of this daughter to the stack
                    for (final Point motherPoint : daughterContourPoint.getMotherList()) {
                        final ContourParticle motherContourPoint = iCandidates.get(motherPoint);
                        if (!motherContourPoint.isProcessed) {
                            motherContourPoint.isProcessed = true;
                            pointsToVisit.push(motherPoint);
                        }
                    }
                }
            }
        }

        //sort the network
        Collections.sort(networkMembers);
        return networkMembers;
    }

    private void updateLabelStatistics(float aIntensity, int aFromLabelIdx, int aToLabelIdx) {
        final LabelStatistics toLabelStats = iLabelStatistics.get(aToLabelIdx);
        final LabelStatistics fromLabelStats = iLabelStatistics.get(aFromLabelIdx);
        final double toCount = toLabelStats.count;
        final double fromCount = fromLabelStats.count;

        // Before changing the mean, compute the sum of squares of the samples:
        final double vToLabelSumOfSq = toLabelStats.var * (toCount - 1.0) + toCount * toLabelStats.mean * toLabelStats.mean;
        final double vFromLabelSumOfSq = fromLabelStats.var * (fromCount - 1.0) + fromCount * fromLabelStats.mean * fromLabelStats.mean;

        // Calculate the new means for the background and the label:
        final double vNewMeanToLabel = (toLabelStats.mean * toCount + aIntensity) / (toCount + 1.0);

        // TODO: divide by zero. why does this not happen at itk?
        double vNewMeanFromLabel = (fromCount > 1) ? ((fromCount * fromLabelStats.mean - aIntensity) / (fromCount - 1.0)) : 0.0;

        // Calculate the new variances:
        double newToVar = ((1.0 / (toCount))
                * (vToLabelSumOfSq + aIntensity * aIntensity - 2.0 * vNewMeanToLabel * (toLabelStats.mean * toCount + aIntensity) + (toCount + 1.0) * vNewMeanToLabel * vNewMeanToLabel));

        double newFromVar = (fromCount != 2) ? (1.0 / (fromCount - 2.0))
                * (vFromLabelSumOfSq - aIntensity * aIntensity - 2.0 * vNewMeanFromLabel * (fromLabelStats.mean * fromCount - aIntensity) + (fromCount - 1.0) * vNewMeanFromLabel * vNewMeanFromLabel)
                : 0.0;

        // Update stats
        toLabelStats.var = newToVar;
        fromLabelStats.var = newFromVar;
        toLabelStats.mean = vNewMeanToLabel;
        fromLabelStats.mean = vNewMeanFromLabel;

        // Add a sample point to the BG and remove it from the label-region:
        toLabelStats.count++;
        fromLabelStats.count--;
    }
}
