package mosaic.regions.RC;


import static mosaic.core.imageUtils.images.LabelImage.BGLabel;

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
import mosaic.core.imageUtils.FloodFill;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.regions.energies.E_Deconvolution;
import mosaic.regions.energies.ImageModel;
import mosaic.regions.energies.OscillationDetection;
import mosaic.regions.energies.Energy.EnergyResult;
import mosaic.regions.topology.TopologicalNumber;
import mosaic.regions.topology.TopologicalNumber.TopologicalNumberResult;
import mosaic.regions.utils.LabelStatisticToolbox;
import mosaic.regions.utils.LabelStatistics;

public class AlgorithmRC {

    private static final Logger logger = Logger.getLogger(AlgorithmRC.class);

    // Input for Algorithm
    private final LabelImage iLabelImage;
    private final IntensityImage iIntensityImage;
    private final ImageModel iImageModel;
    private final SettingsRC iSettings;

    private final HashMap<Point, ContourParticle> iContourParticles = new HashMap<Point, ContourParticle>();
    private final HashMap<Integer, LabelStatistics> iLabelStatistics = new HashMap<Integer, LabelStatistics>();
    private final HashMap<Point, LabelPair> iCompetingRegions = new HashMap<Point, LabelPair>();
    private final HashMap<Point, ContourParticle> iCandidates = new HashMap<Point, ContourParticle>();

    private final LabelDispenser labelDispenser = new LabelDispenser();
    private final OscillationDetection oscillationDetection;
    private final TopologicalNumber iTopologicalNumber;

    // Settings
    private static final float AcceptedPointsFactor = 1;
    private static final boolean RemoveNonSignificantRegions = true;
    private static final int MinimumAreaSize = 1;
    private boolean shrinkFirst = false;
    private float acceptedPointsFactor = AcceptedPointsFactor;

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

    public AlgorithmRC(IntensityImage aIntensityImage, LabelImage aLabelImage, ImageModel aModel, SettingsRC aSettings) {
        iLabelImage = aLabelImage;
        iIntensityImage = aIntensityImage;
        iImageModel = aModel;
        iSettings = aSettings;

        oscillationDetection = new OscillationDetection(iSettings.oscillationThreshold, iSettings.maxNumOfIterations);
        iTopologicalNumber = new TopologicalNumber(iLabelImage);

        // Initialize label image
        iLabelImage.initBorder();
        List<Point> contourPoints = iLabelImage.initContour();
        initContourContainer(contourPoints);

        
        int maxUsedLabel = LabelStatisticToolbox.initStatistics(iLabelImage, iIntensityImage, iLabelStatistics);
        
        // Make sure that labelDispenser will not produce again any already used label
        // Safe to search with 'max' we have at least one value in container (background)
        labelDispenser.setMaxValueOfUsedLabel(maxUsedLabel);

        initEnergies();
    }

    /**
     * Find all contour points in LabelImage (and mark them as a contour => -labelValue) Creates ContourParticle for every contour point and stores it in container.
     */
    private void initContourContainer(List<Point> aContourPoints) {
        for (Point point : aContourPoints) {
            final ContourParticle particle = new ContourParticle(iLabelImage.getLabelAbs(point), iIntensityImage.get(point));
            iContourParticles.put(point, particle);
        }
    }

    double CalculateVariance(double aSumSq, double aMean, int aN) {
        if (aN < 2) return 0;
        return (aSumSq - aN * aMean * aMean)/(aN - 1.0);
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
        if (iSettings.usingDeconvolutionPcEnergy) {
            // Deconvolution: - Alocate and initialize the 'ideal image'
            // TODO: This is not OOP, handling energies should be redesigned
            ((E_Deconvolution) iImageModel.getEdata()).GenerateModelImage(iLabelImage, iLabelStatistics);
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
    }

    /**
     * Calculates the center of mass of each region (for each label) 
     * TODO: should calculateRegionsCenterOfMass be part of LabelStatistics and this method here? It is one time use from RC
     */
    public void calculateRegionsCenterOfMass() {
        // Reset mean position for all labels
        for (final LabelStatistics labelStats : iLabelStatistics.values()) {
            for (int i = 0; i < labelStats.iMeanPosition.length; ++i) {
                labelStats.iMeanPosition[i] = 0.0;
            }
        }

        // Iterate through whole label image and update mean position (only sum all coordinate values)
        final Iterator<Point> ri = new SpaceIterator(iLabelImage.getDimensions()).getPointIterator();
        while (ri.hasNext()) {
            final Point point = ri.next();
            int label = iLabelImage.getLabel(point);
            final LabelStatistics labelStats = iLabelStatistics.get(iLabelImage.labelToAbs(label));

            if (labelStats != null) {
                for (int i = 0; i < point.getNumOfDimensions(); ++i) {
                    labelStats.iMeanPosition[i] += point.iCoords[i];
                }
            }
            else {
                // There should be statistics for all labels excluding only forbidden border label.
                if (!iLabelImage.isBorderLabel(label)) {
                    logger.error("Cound not find label statistics for label: " + label + " at: " + point);
                }
            }
        }

        // Iterate through all label statistics and calculate center of mass basing on previous calcuation
        for (final LabelStatistics labelStats : iLabelStatistics.values()) {
            for (int i = 0; i < labelStats.iMeanPosition.length; ++i) {
                labelStats.iMeanPosition[i] /= labelStats.iLabelCount;
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
                final ContourParticle q = new ContourParticle(aAbsLabel, iIntensityImage.get(p));
                q.candidateLabel = BGLabel;
                iLabelImage.setLabel(p, iLabelImage.labelToNeg(aAbsLabel));
                iContourParticles.put(iLabelImage.indexToPoint(p), q);
            }
        }
    }
    
    /**
     * If neighbors of changed particle are enclosed, remove them from ContourParticles container and change their
     * type to interior.
     */
    private void removeEnclosedNeighboursFromContour(int aLabelAbs, Point aPoint) {
        for (final int qIndex : iLabelImage.iterateNeighbours(aPoint)) {
            if (iLabelImage.getLabel(qIndex) == iLabelImage.labelToNeg(aLabelAbs) && iLabelImage.isEnclosedByLabel(qIndex, aLabelAbs)) {
                iContourParticles.remove(iLabelImage.indexToPoint(qIndex));
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
    private void changeContourPointLabelToCandidateLabelAndUpdateNeighbours(Point aPoint, ContourParticle aContourParticle) {
        final int fromLabel = aContourParticle.label;
        final int toLabel = aContourParticle.candidateLabel;
        float intensity = aContourParticle.intensity;

        // Update the label image. The new point is either a contour point or 0,
        // therefore the negative label value is set.
        iLabelImage.setLabel(aPoint, iLabelImage.labelToNeg(toLabel));

        // Update the statistics of the propagating and the loser region.
        LabelStatisticToolbox.updateLabelStatistics(intensity, fromLabel, toLabel, iLabelStatistics);
        if (iSettings.usingDeconvolutionPcEnergy) {
            ((E_Deconvolution) iImageModel.getEdata()).UpdateConvolvedImage(aPoint, fromLabel, toLabel, iLabelStatistics);
        }

        // TODO: A bit a dirty hack: we store the old label for the relabeling procedure later on...
        // either introduce a new variable or rename the variable (which doesn't work currently :-).
        aContourParticle.candidateLabel = fromLabel;

        // ---------------- Clean up neighborhood -------------------

        // The loser region (if it is not the BG region) has to add the
        // neighbors of the lost point to the contour list.
        if (fromLabel != BGLabel) {
            changeNeighboursOfParticleToCountour(fromLabel, aPoint);
        }

        // Erase the point from the surface container in case it now belongs to the background.
        // Otherwise add the point to the container (or replace it in case it has been there already).
        if (toLabel == BGLabel) {
            iContourParticles.remove(aPoint);
        }
        else {
            aContourParticle.label = toLabel;
            // The point may or may not exist already in the m_InnerContainer.
            // The old value, if it exist, is just overwritten with the new contour point (with a new label).
            iContourParticles.put(aPoint, aContourParticle);

            // Remove 'enclosed' contour points from the container. For the BG this makes no sense.
            removeEnclosedNeighboursFromContour(toLabel, aPoint);
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
            else if (labelStat.iLabelCount == 1) {
                contourParticle.candidateLabel = BGLabel;
                changeContourPointLabelToCandidateLabelAndUpdateNeighbours(vIt.getKey(), vIt.getValue());
            }
        }
        LabelStatisticToolbox.removeEmptyStatistics(iLabelStatistics);
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
            else if (labelStats.getValue().iLabelCount <= MinimumAreaSize) {
                removeRegion(label);
            }
        }
        LabelStatisticToolbox.removeEmptyStatistics(iLabelStatistics);
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
                changeContourPointLabelToCandidateLabelAndUpdateNeighbours(tmp.getKey(), tmp.getValue());
                iter.remove();
            }

            // After above loop only outside "layer" of region was removed. Proceed until all stuff is removed.
            if (iLabelStatistics.get(aLabel).iLabelCount > 0) {
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
        if (acceptedPointsFactor >= 1) {
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
        int vNbElements =  (int) (sortedCandidates.size() * acceptedPointsFactor + 0.5);

        for (final ContourParticleWithIndex vSortedListIterator : sortedCandidates) {
            if (vNbElements-- < 1) {
                break;
            }
            // This candidate passed the test and is added to the TempRemoveCotainer:
            iCandidates.put(vSortedListIterator.iPoint, vSortedListIterator.iContourParticle);
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
        final FloodFill ff = new FloodFill(iLabelImage, aAreaWithOldLabels, aPoint);
        
        double sumOfVal = 0;
        double sumOfSqVal = 0;
        int count = 0;

        for(Integer currentPoint : ff) {
            final int oldLabel = iLabelImage.getLabel(currentPoint);

            iLabelImage.setLabel(currentPoint, aNewLabel);

            // the visited labels statistics will be removed later.
            oldLabels.add(iLabelImage.labelToAbs(oldLabel));
            if (iLabelImage.isContourLabel(oldLabel)) {
                oldContours.add(iLabelImage.indexToPoint(currentPoint));
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
        newLabelStats.iLabelCount = count;
        newLabelStats.iSum = sumOfVal;
        newLabelStats.iSumOfSq = sumOfSqVal;
        newLabelStats.iMeanIntensity = newLabelStats.iLabelCount > 0 ? (newLabelStats.iSum / newLabelStats.iLabelCount) : 0;
        newLabelStats.iVarIntensity = CalculateVariance(newLabelStats.iSumOfSq,  newLabelStats.iMeanIntensity, newLabelStats.iLabelCount);
        // TODO: What to do with median, by default it is zero but shouldn't it be calculated basing on merged labels?
//        newLabelStats.iMedianIntensity = newLabelStats.iMeanIntensity;
        
        iLabelStatistics.put(aNewLabel, newLabelStats);

        // Clean up the statistics of non valid regions.
        for (final int oldLabel : oldLabels) {
            iLabelStatistics.remove(oldLabel);
        }
        LabelStatisticToolbox.removeEmptyStatistics(iLabelStatistics);
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
            Entry<Point, ContourParticle> next = iter.next();
            ContourParticle particle = next.getValue();
            if (particle.energyDifference >= 0) {
                iter.remove();
            }
        }
    }
    
    public boolean performIteration() {
        if (iSettings.usingDeconvolutionPcEnergy) {
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
        
        if (RemoveNonSignificantRegions) {
            removeSinglePointRegions();
            removeNotSignificantRegions();
        }
        
        buildCandidateList();
        filterCandidates();
        if (oscillationDetection.DetectOscillations(iCandidates.values())) {
            acceptedPointsFactor /= 2.0;
        }
        limitNumberOfCandidates();
        boolean convergence = moveCandidates();
        LabelStatisticToolbox.removeEmptyStatistics(iLabelStatistics);
        
        if (shrinkFirst && convergence) {
            // Done with shrinking, now allow growing
            convergence = false;
            shrinkFirst = false;
            acceptedPointsFactor = AcceptedPointsFactor;
        }
        
        return convergence;
    }

    /**
     * Iterates through ContourParticles in iContourParticles container. Calculates energies for shrinking (contour particle becoming BG) 
     * and growing scenarios (contour particle expands on nearby BG region or on other region contour)
     */
    private void buildCandidateList() {
        // Put all current contour particles into candidates
        initiateCandidateList();
        
        // Calculate energy for change to BG (shrinking)
        for (final Entry<Point, ContourParticle> iter : iCandidates.entrySet()) {
            final Point point = iter.getKey();
            final ContourParticle contour = iter.getValue();

            contour.candidateLabel = BGLabel;
            contour.referenceCount = 0; // doesn't matter for the BG
            contour.energyDifference = iImageModel.calculateDeltaEnergy(point, contour, BGLabel, iLabelStatistics).energyDifference;
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
            final Point propagatingPoint = iter.getKey();
            ContourParticle propagatingContour = iter.getValue();
            final int propagatingRegionLabel = propagatingContour.label;

            for (final Integer neighbor : iLabelImage.iterateNeighbours(propagatingPoint)) {
                final int labelOfDefender = iLabelImage.getLabelAbs(neighbor);
                if (iLabelImage.isBorderLabel(labelOfDefender) || labelOfDefender == propagatingRegionLabel) {
                    // Skip forbidden border and same region labels
                    continue;
                }
                final Point neighbourPoint = iLabelImage.indexToPoint(neighbor);
                propagatingContour.addDaughter(neighbourPoint);
                
                // Get contour candidate, if it does not exist it is a background point (create), otherwise
                // it is another region contour.
                ContourParticle contourCandidate = iCandidates.get(neighbourPoint);
                if (contourCandidate == null) {
                    contourCandidate = new ContourParticle(labelOfDefender, iIntensityImage.get(neighbor));
                    iCandidates.put(neighbourPoint, contourCandidate);
                }
                
                contourCandidate.isDaughter = true;
                contourCandidate.addMother(propagatingPoint);

                // Check if the energy difference for this candidate label has not yet been calculated.
                if (!contourCandidate.hasLabelBeenTested((propagatingRegionLabel))) {
                    contourCandidate.setTestedLabel(propagatingRegionLabel);

                    final EnergyResult energyResult = iImageModel.calculateDeltaEnergy(neighbourPoint, contourCandidate, propagatingRegionLabel, iLabelStatistics);
                    if (energyResult.energyDifference < contourCandidate.energyDifference) {

                        contourCandidate.candidateLabel = propagatingRegionLabel;
                        contourCandidate.referenceCount = 1;
                        contourCandidate.energyDifference = energyResult.energyDifference;

                        if (energyResult.merge && contourCandidate.label != BGLabel && contourCandidate.candidateLabel != BGLabel) {
                            iCompetingRegions.put(propagatingPoint, new LabelPair(contourCandidate.candidateLabel, contourCandidate.label));
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

    /**
     * Initiates candidate list. Every current contour point is a new candidate mother without any other mother/daughter in her lists.
     */
    private void initiateCandidateList() {
        iCandidates.clear();
        iCandidates.putAll(iContourParticles);
        for (ContourParticle contour : iCandidates.values()) {
            contour.isMother = true;
            contour.isDaughter = false;
            contour.isProcessed = false;
            contour.clearLists();
        }
    }
    
    /**
     * Filters topological incompatible candidates (topological dependencies) and non-improving energies.
     */
    private void filterCandidates() {
        if (!shrinkFirst) {
            //Find topologically compatible candidates
            final List<Point> illegalPoints = new LinkedList<Point>();
            for (final Entry<Point, ContourParticle> e : iCandidates.entrySet()) {
                final Point point = e.getKey();
                final ContourParticle contour = e.getValue();

                // Check if this point already was processed and if it is a mother: 
                // Only mothers can be seed points of topological networks. Daughters are always part of a topological network of a mother.
                if (!contour.isProcessed && contour.isMother) {
                    contour.isProcessed = true;

                    // Build the dependency network for this seed point:
                    final List<ContourParticleWithIndex> networkMembers = buildDependencyNetwork(point);

                    // Filtering: Accept all members in ascending order that are compatible with the already selected members in the network.
                    final HashSet<Point> selectedCandidatesPoints = new HashSet<Point>();

                    for (final ContourParticleWithIndex networkPoint : networkMembers) {
                        // Rules: a candidate in the network is a legal candidate if:
                        // - If (daughter): The reference count >= 1. (Except the the candidate label is the BG - this allows creating BG regions in between two competing regions).
                        // - If ( mother ): All daughters (with the same 'old' label) in the accepted list have still a reference count > 1.
                        boolean isMoveLegal = true;
                        ContourParticle currentContour = networkPoint.iContourParticle;
                        Point currentPoint = networkPoint.iPoint;

                        // RULE 1: If networkPoint is a daughter point, the reference count is > 0 (if not BG label).
                        if (currentContour.isDaughter) {
                            if (currentContour.referenceCount < 1 && currentContour.candidateLabel != BGLabel) {
                                isMoveLegal = false;
                            }
                        }

                        // RULE 2: All daughters already accepted the label of this mother have at least one another mother.
                        // RULE 3: Mothers are still valid mothers (to not introduce holes in the FG region).
                        if (isMoveLegal && currentContour.isMother) {
                            // Iterate the daughters and check their reference count
                            boolean rule3Fulfilled = false;

                            for (final Point daughter : currentContour.getDaughterList()) {
                                final ContourParticle daughterContour = iCandidates.get(daughter);

                                // rule 2:
                                final boolean alreadyAccepted = selectedCandidatesPoints.contains(daughter);
                                if (alreadyAccepted) {
                                    // This daughter has been accepted and needs a reference count > 1, else the move is invalid.
                                    if (daughterContour.candidateLabel == currentContour.label && daughterContour.referenceCount <= 1) {
                                        isMoveLegal = false;
                                        break;
                                    }
                                }

                                // rule 3:
                                if (!rule3Fulfilled) {
                                    if (!alreadyAccepted) {
                                        // There is a daughter that has not yet been accepted.
                                        rule3Fulfilled = true;
                                    }
                                    else {
                                        // the daughter has been accepted, but may have another candidate label(rule 3b):
                                        if (daughterContour.candidateLabel != currentContour.label) {
                                            rule3Fulfilled = true;
                                        }
                                    }
                                }
                            }

                            if (!rule3Fulfilled) {
                                isMoveLegal = false;
                            }
                        }

                        if (isMoveLegal) {
                            selectedCandidatesPoints.add(currentPoint);
                            // If a mother is accepted, the reference count of all the daughters (with the same label) has to be decreased.
                            for (final Point daughter : currentContour.getDaughterList()) {
                                final ContourParticle daughterContour = iCandidates.get(daughter);
                                if (daughterContour.candidateLabel == currentContour.label) {
                                    daughterContour.referenceCount--;
                                }
                            }
                        }
                        else {
                            illegalPoints.add(currentPoint);
                        }
                    }
                }
            }

            //Filter all candidates with the illegal indices
            for (final Point illegalPoint : illegalPoints) {
                iCandidates.remove(illegalPoint);
            }
        }

        removeCandidatesWithNonNegativeDeltaEnergy();
    }
    
    /**
     * Creates sorted container (by energy difference) with all contour particles which are in interest of given mother (aMotherPoint) 
     * + all other mothers interested in the same point with its children (it repeats that process to find complete dependency network). 
     * @param aMotherPoint - input mother point for which network is going to be build
     * @return maximum connected subgraph Gk with input mother as a part of it
     */
    private List<ContourParticleWithIndex> buildDependencyNetwork(final Point aMotherPoint) {
        final List<ContourParticleWithIndex> networkMembers = new LinkedList<ContourParticleWithIndex>();
        
        final Stack<Point> pointsToVisit = new Stack<Point>();
        pointsToVisit.push(aMotherPoint);
        while (!pointsToVisit.empty()) {
            final Point motherPoint = pointsToVisit.pop();
            final ContourParticle motherContour = iCandidates.get(motherPoint);
            
            // Add the seed point to the network
            networkMembers.add(new ContourParticleWithIndex(motherPoint, motherContour));

            // Iterate all children of the seed, push to the stack if there is a mother.
            for (final Point daughterPoint : motherContour.getDaughterList()) {
                final ContourParticle daughterContour = iCandidates.get(daughterPoint);

                if (!daughterContour.isProcessed) {
                    daughterContour.isProcessed = true;

                    if (daughterContour.isMother) {
                        pointsToVisit.push(daughterPoint);
                    }
                    else {
                        networkMembers.add(new ContourParticleWithIndex(daughterPoint, daughterContour));
                    }

                    // Push all the non-processed mothers of this daughter to the stack
                    for (final Point mother : daughterContour.getMotherList()) {
                        final ContourParticle motherContourPoint = iCandidates.get(mother);
                        if (!motherContourPoint.isProcessed) {
                            motherContourPoint.isProcessed = true;
                            pointsToVisit.push(mother);
                        }
                    }
                }
            }
        }
        
        //sort the network
        Collections.sort(networkMembers);
        return networkMembers;
    }
    
    /**
     * Moves all FG-simple points
     * @return
     */
    private boolean moveAllFgSimplePoints() {
        boolean convergenceResult = true;
        
        // It happens that points that are not simple at the first place get simple after the change of other points. 
        // Iterate through all candidates until there is no more FG-simple points in container.
        boolean candidateWereMoved = true;
        while (!iCandidates.isEmpty() && candidateWereMoved) {
            candidateWereMoved = false;
            final Iterator<Entry<Point, ContourParticle>> candidateIter = iCandidates.entrySet().iterator();
            while (candidateIter.hasNext()) {
                final Entry<Point, ContourParticle> entry = candidateIter.next();
                final Point currentPoint = entry.getKey();
                final ContourParticle currentContour = entry.getValue();
                
                if (iTopologicalNumber.isPointFgSimple(currentPoint)) {
                    changeContourPointLabelToCandidateLabelAndUpdateNeighbours(currentPoint, currentContour);
                    candidateIter.remove();
                    candidateWereMoved = true;
                    convergenceResult = false;
                }
                
                // we will reuse the processed flag to indicate if a particle is a seed
                currentContour.isProcessed = false;
            }
        }
        
        return convergenceResult;
    }
    
    /**
     * @param currentCandidateLabel - current candidate label
     * @param topologicalNumbers - calculated topological numbers for current point
     * @return true if there is no handles introduced (or if new handles are allowed)
     */
    private boolean checkForHandles(final int currentCandidateLabel, List<TopologicalNumberResult> topologicalNumbers) {
        // if the point was not disqualified already and we disallow introducing handles (not only self fusion!), 
        // we check if there is an introduction of a handle.
        boolean validPoint = true;
        if (!iSettings.allowHandles) {
            for (final TopologicalNumberResult tn : topologicalNumbers) {
                // - "allow introducing holes": T_FG(x, L = l') > 1
                if (tn.iLabel == currentCandidateLabel && tn.iNumOfConnectedComponentsFG > 1) {
                    // Changing that point label to currentCandidateLabel would make fusion of FG regions, 
                    // and make a hole, disallow! 
                    validPoint = false;
                }

                // criterion to detect surface points (>= 3D), allowing to changing label would make a hole (with different
                // label) surrounded by current label.
                if (tn.iNumOfConnectedComponentsFG == 1 && tn.iNumOfConnectedComponentsBG > 1) {
                    validPoint = false;
                }
            }
        }
        
        return validPoint;
    }
    
    /**
     * @param seeds - container with seeds, it will be updated if point will (and can) make a split
     * @param currentPoint - current Point
     * @param currentLabel - current Label
     * @param topologicalNumbers - calculated topological numbers for current point
     * @return false if it is going to split but splits are not allowed
     */
    private boolean checkForSplits(final Set<Seed> seeds, final Point currentPoint, final int currentLabel, List<TopologicalNumberResult> topologicalNumbers) {
        boolean validPoint = true;
        
        // This we have to do either to forbid the change in topology or to register the seed point for relabeling.
        // if the point was not disqualified already and we disallow splits, then we check if the 'old' label undergoes a split.
        
        // Check first if "moving" current point would make a split
        boolean isItGoingToSplit = false;
        for (final TopologicalNumberResult tn : topologicalNumbers) {
            // - "allow splits": T_FG >= 2
            if (tn.iLabel == currentLabel && tn.iNumOfConnectedComponentsFG > 1) {
                isItGoingToSplit = true;
                break;
            }
        }
        
        if (isItGoingToSplit) {
            if (iSettings.allowFission) {
                registerNeighbourSeedsWithSameLabel(seeds, currentPoint, currentLabel);
            }
            else {
                validPoint = false;
            }
        }
        
        return validPoint;
    }
    
    /**
     * Merge competing regions if it is allowed
     * @return false if there is no merge
     */
    private boolean mergeRegions() {
        boolean didMerge = false;
        if (iSettings.allowFusion) {
            final Set<Integer> checkedLabels = new HashSet<Integer>();
            for (final Entry<Point, LabelPair> iter : iCompetingRegions.entrySet()) {
                final Point point = iter.getKey();
                final int firstLabel = iter.getValue().first;
                relabelMergedRegions(point, firstLabel, checkedLabels);
                didMerge = true;
            }
        }
        return didMerge;
    }

    /**
     * Relabel split regions
     * @param seeds - points from split regions
     * @return false if there is no split
     */
    private boolean relabelSplitRegions(final Set<Seed> seeds) {
        boolean didSplit = false;
        for (final Seed seed : seeds) {
            relabelRegion(seed.getPoint(), seed.getLabel());
            didSplit = true;
        }
        return didSplit;
    }
    
    /**
     * Move the points in the candidate list
     */
    private boolean moveCandidates() {
        // We first move all the FG-simple points. We do that because it happens that points that are not simple at the first 
        // place get simple after the change of other points. The non-simple points will be treated in a separate loop afterwards.
        boolean convergenceResult = moveAllFgSimplePoints();
        
        // Now we know that all the points in the list are not FG-simple. We move them anyway 
        // (if topological constraints allow) but record (for every particle) where to relabel (using the seed set). 
        // Placing the seed is necessary for every particle to ensure relabeling even if a bunch of neighboring particles 
        // change. The seed will be ignored later on if the corresponding FG region is not present in the neighborhood anymore.
        // TODO: The following code is dependent on the iteration order if splits/handles are not allowed. 
        // A solution would be to sort the candidates beforehand.
        final Iterator<Entry<Point, ContourParticle>> pointIterator = iCandidates.entrySet().iterator();
        final Set<Seed> seeds = new HashSet<Seed>();
        while (pointIterator.hasNext()) {
            final Entry<Point, ContourParticle> e = pointIterator.next();
            final Point currentPoint = e.getKey();
            final ContourParticle currentParticle = e.getValue();
            final int currentLabel = currentParticle.label;
            final int currentCandidateLabel = currentParticle.candidateLabel;
            
            // Precalculate topological numbers for current point
            List<TopologicalNumberResult> topologicalNumbers = iTopologicalNumber.getTopologicalNumbersForAllAdjacentLabels(currentPoint);
            
            // Check for handles:
            boolean validPoint = checkForHandles(currentCandidateLabel, topologicalNumbers);
            
            // Check for splits:
            validPoint = validPoint && checkForSplits(seeds, currentPoint, currentLabel, topologicalNumbers);
            
            if (validPoint) {
                // If the move doesn't change topology or is allowed (and registered as seed) to change the topology, 
                // perform the move (in the second iteration; in the first iteration seed points need to be collected):
                changeContourPointLabelToCandidateLabelAndUpdateNeighbours(currentPoint, currentParticle);
                convergenceResult = false;
                
                if (currentParticle.isProcessed) {
                    registerNeighbourSeedsWithSameLabel(seeds, currentPoint, currentLabel);
                    if (!seeds.remove(new Seed(currentPoint, currentLabel))) {
                        throw new RuntimeException("no seed in set");
                    }
                }
            }
            
            if (iCandidates.containsKey(currentPoint)) {
                // TODO: is this check needed? Can this point be removed somewhere else?
                pointIterator.remove();
            }
        }
        
        boolean didSplitOrMerge = false;
        if (relabelSplitRegions(seeds)) didSplitOrMerge = true;
        if (mergeRegions()) didSplitOrMerge = true;
        
        if (didSplitOrMerge) {
            if (iSettings.usingDeconvolutionPcEnergy) {
                ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
            }
        }
        
        return convergenceResult;
    }
    
    private void registerNeighbourSeedsWithSameLabel(Set<Seed> aSeeds, Point aPoint, int aLabel) {
        for (final Integer neighbour : iLabelImage.iterateNeighbours(aPoint)) {
            final int label = iLabelImage.getLabelAbs(neighbour);
            
            if (label == aLabel) {
                final Point neighbourPoint = iLabelImage.indexToPoint(neighbour);
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
}
