package mosaic.region_competition.DRS;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.topology.TopologicalNumber;
import mosaic.region_competition.topology.TopologicalNumber.TopologicalNumberResult;

public class AlgorithmDRS {

    private static final Logger logger = Logger.getLogger(AlgorithmDRS.class);

    // Input for Algorithm
    private final LabelImage iLabelImage;
    private final IntensityImage iIntensityImage;
    private final IntensityImage iEdgeImage;
    private final ImageModel iImageModel;
    private final Settings iSettings;


    // TODO: Names below are map one to one from original source code
    //       Change them to something nicer after implementation.

    // Parameters
    boolean m_MCMCuseBiasedProposal = true;

    boolean m_AllowFission = true;
    boolean m_AllowHandles = true;

    
    // ==========================================================

    Rng m_NumberGenerator = new Rng(1212);
    
    // Connectivities
    int m_NeighborhoodSize_FG_Connectivity;
    Point[] m_NeighborsOffsets_FG_Connectivity;
    int m_NeighborhoodSize_BG_Connectivity;
    Point[] m_NeighborsOffsets_BG_Connectivity;
    TopologicalNumber m_TopologicalNumberFunction;
    
    float[] m_MCMClengthProposalMask = null;

    LabelImage m_MCMCRegularParticlesMap; // originaly boolean but  0 / 1 approach seems to be good
    double m_MCMCZe = 0.0; // sum of edge image values
    EnumeratedDistribution<Integer> m_MCMCEdgeImageDistr = null;
    private LabelImage m_MCMCInitBackupLabelImage;

    List<Integer> m_MCMCRegionLabel = new ArrayList<>();
    // ProposalNormalizerMapType
    Map<Integer, Float> m_MCMCparentsProposalNormalizer = new HashMap<>();
    Map<Integer, Float> m_MCMCchildrenProposalNormalizer = new HashMap<>();
    double m_MCMCTotalNormalizer;

    Map<Integer, MinimalParticleIndexedSet> m_MCMCchildren = new HashMap<>();
    Map<Integer, MinimalParticleIndexedSet> m_MCMCparents = new HashMap<>();
    
    class ParticleHistoryElement {
        public ParticleHistoryElement(MinimalParticle vReplacedParticle, int aCurrentLabel, boolean b) {
            m_Particle = vReplacedParticle;
            m_OriginalLabel = aCurrentLabel;
            m_WasAdded = b;
        }
        MinimalParticle m_Particle;
        int m_OriginalLabel;
        boolean m_WasAdded;
    }
    
    List<ParticleHistoryElement> m_MCMCParticleInContainerHistory = new ArrayList<>();
        
    public AlgorithmDRS(IntensityImage aIntensityImage, LabelImage aLabelImage, IntensityImage aEdgeImage, ImageModel aModel, Settings aSettings) {
        logger.debug("DRS algorithm created");

        iLabelImage = aLabelImage;
        iIntensityImage = aIntensityImage;
        iEdgeImage = aEdgeImage;
        iImageModel = aModel;
        iSettings = aSettings;

        logger.debug(mosaic.utils.Debug.getString(iIntensityImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iLabelImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iEdgeImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iImageModel.toString()));
        logger.debug(mosaic.utils.Debug.getString(iSettings.toString()));

        // Initialize label image
        iLabelImage.initBorder();
        iLabelImage.initContour();

        // init connectivities
        Connectivity connFG = iLabelImage.getConnFG();
        m_NeighborhoodSize_FG_Connectivity = connFG.getNumOfNeighbors();
        m_NeighborsOffsets_FG_Connectivity = connFG.getPointOffsets();
        Connectivity connBG = iLabelImage.getConnBG();
        m_NeighborhoodSize_BG_Connectivity = connBG.getNumOfNeighbors();
        m_NeighborsOffsets_BG_Connectivity = connBG.getPointOffsets();
        // TODO: m_TopologicalNumberFunction - set / check what is it.
        m_TopologicalNumberFunction = new TopologicalNumber(iLabelImage);
        
        // OK.. here starts algorithm
        m_MCMCRegularParticlesMap = new LabelImage(iLabelImage.getDimensions());

        Pair<Double, EnumeratedDistribution<Integer>> distPair = generateDiscreteDistribution(iEdgeImage);
        m_MCMCZe = distPair.getFirst();
        m_MCMCEdgeImageDistr = distPair.getSecond();

        // Prepare a fast proposal computation:
        System.out.println("m_MCMCuseBiasedProposal: " + (m_MCMCuseBiasedProposal ? "1" : "0") + " " + m_NeighborhoodSize_BG_Connectivity);
        for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; vN++) {
            System.out.println(vN + "_conn: " + m_NeighborsOffsets_BG_Connectivity[vN]);
        }
        if (m_MCMCuseBiasedProposal) {
            m_MCMClengthProposalMask = new float[m_NeighborhoodSize_BG_Connectivity];
            for (int i = 0; i < m_NeighborhoodSize_BG_Connectivity; ++i) {
                Point offset = m_NeighborsOffsets_BG_Connectivity[i];
                m_MCMClengthProposalMask[i] = (float) (1.0 / offset.length());
                System.out.println(i + "_mask: " + m_MCMClengthProposalMask[i]);
            }
        }

        // We need a copy of the label image for the reconstruction of the 
        // final the results (marginals). It is a backup of the initial state.
        m_MCMCInitBackupLabelImage = new LabelImage(iLabelImage);

        // Register all labels from lableImage
        Set<Integer> visitedLabels = new HashSet<>();

        // By default add background
        m_MCMCRegionLabel.add(LabelImage.BGLabel);
        m_MCMCparentsProposalNormalizer.put(LabelImage.BGLabel, 0f);
        m_MCMCchildrenProposalNormalizer.put(LabelImage.BGLabel, 0f);
        visitedLabels.add(LabelImage.BGLabel);
        m_MCMCTotalNormalizer = 0.0;

        // TODO: Remove it - only temporary input to match c++
        iLabelImage.setLabel(6, 0);
        iLabelImage.setLabel(7, 0);
        iLabelImage.setLabel(8, 1);
        iLabelImage.setLabel(11, 0);
        iLabelImage.setLabel(12, 1);
        iLabelImage.setLabel(13, 1);
        iLabelImage.setLabel(16, 1);
        iLabelImage.setLabel(17, 1);
        iLabelImage.setLabel(18, 1);
        
        final Iterator<Point> ri = new SpaceIterator(iLabelImage.getDimensions()).getPointIterator();
        while (ri.hasNext()) {
            final Point point = ri.next();
            int label = iLabelImage.getLabel(point);
            System.out.println("------------ Init label: " + label + " at " + point);
            if (iLabelImage.isBorderLabel(label)) continue;

            // Add if not added so far
            int labelAbs = iLabelImage.labelToAbs(label);
            if (!visitedLabels.contains(labelAbs)) {
                visitedLabels.add(labelAbs);
                m_MCMCRegionLabel.add(label);
                m_MCMCparentsProposalNormalizer.put(labelAbs, 0f);
                m_MCMCchildrenProposalNormalizer.put(labelAbs, 0f);
            }
            //            TODO: rest of initialization
            //            
            //            // Add all regular particles at this spot:
            MinimalParticleIndexedSet vPs = new MinimalParticleIndexedSet();
            MCMCgetRegularParticlesAtIndex(iLabelImage.pointToIndex(point), vPs);
                        
            for (int vI = 0; vI < vPs.size(); ++vI) {
                if (MCMCIsParticleTopoValid(vPs.elementAt(vI))) {
                    MCMCInsertCandidatesToContainers(vPs.elementAt(vI), label, false);
                    iLabelImage.setLabel(point, -labelAbs);
                    m_MCMCRegularParticlesMap.setLabel(vPs.elementAt(vI).iIndex, 1);
                    m_MCMCZe -= iEdgeImage.get(vPs.elementAt(vI).iIndex);
                }
            }
        }
        
        System.out.println("children:\n" + m_MCMCchildren);
        System.out.println("parents:\n" + m_MCMCparents);
    }

    boolean MCMCInsertCandidatesToContainers(MinimalParticle aParticle, int aCurrentLabel, boolean aDoRecord) {
        boolean vReplace = false;
        MinimalParticle vReplacedParticle = null;

        if (aParticle.iCandidateLabel == 0) {

            MinimalParticleIndexedSet partSet = m_MCMCparents.get(aCurrentLabel);
            if (partSet == null) {
                partSet = new MinimalParticleIndexedSet();
                m_MCMCparents.put(aCurrentLabel, partSet);
            }
            int vSizeBefore = partSet.size();
            partSet.insert(aParticle);

            if (vSizeBefore < partSet.size()){
                m_MCMCparentsProposalNormalizer.replace(aCurrentLabel, m_MCMCparentsProposalNormalizer.get(aCurrentLabel) + aParticle.iProposal);
                m_MCMCTotalNormalizer += aParticle.iProposal;
            } else {
                /// this is a replacement:
                float vDiff = aParticle.iProposal - partSet.getLastDeletedElement().iProposal;
                m_MCMCparentsProposalNormalizer.replace(aCurrentLabel, m_MCMCparentsProposalNormalizer.get(aCurrentLabel) + vDiff);
                m_MCMCTotalNormalizer += vDiff;

                vReplace = true;
                vReplacedParticle = partSet.getLastDeletedElement();
            }
        } else {
            System.out.println("MCMCInsertCandidatesToContainers: " + aParticle + " " + m_MCMCchildren.size());
            
            MinimalParticleIndexedSet partSet = m_MCMCchildren.get(aParticle.iCandidateLabel);
            if (partSet == null) {
                partSet = new MinimalParticleIndexedSet();
                m_MCMCchildren.put(aParticle.iCandidateLabel, partSet);
            }
            int vSizeBefore = partSet.size();
            partSet.insert(aParticle);
            if (vSizeBefore < partSet.size()) {
                if (!m_MCMCchildrenProposalNormalizer.containsKey(aParticle.iCandidateLabel)) {m_MCMCchildrenProposalNormalizer.put(aParticle.iCandidateLabel, 0f);}
                m_MCMCchildrenProposalNormalizer.replace(aParticle.iCandidateLabel, m_MCMCchildrenProposalNormalizer.get(aParticle.iCandidateLabel) + aParticle.iProposal);
                m_MCMCTotalNormalizer += aParticle.iProposal;
            } else {
                /// this is a replacement:
                float vDiff = aParticle.iProposal - partSet.getLastDeletedElement().iProposal;
                // TODO: next line might fail - do solution from above (check key and insert if necesarry) Intentinally left not fixed for now.
                m_MCMCchildrenProposalNormalizer.replace(aParticle.iCandidateLabel, m_MCMCchildrenProposalNormalizer.get(aParticle.iCandidateLabel) + vDiff);
                m_MCMCTotalNormalizer += vDiff;

                vReplace = true;
                vReplacedParticle = partSet.getLastDeletedElement();
            }
        }

        if (aDoRecord) {
            /// Note that the order here is important: first insert the particle
            /// that gets replaced. When restoring the state afterwards, the
            /// particle history is iterated in reverse order.
            if (vReplace) {
                ParticleHistoryElement vPHE2 = new ParticleHistoryElement(vReplacedParticle, aCurrentLabel, false);
                m_MCMCParticleInContainerHistory.add(vPHE2);
            }

            ParticleHistoryElement vPHE = new ParticleHistoryElement(aParticle, aCurrentLabel, true);
            m_MCMCParticleInContainerHistory.add(vPHE);
        }

        return !vReplace;
    }
    
    boolean MCMCIsParticleTopoValid(MinimalParticle aParticle) {
        // TODO: Currently not effiecient but seems to be correct with output. Must be checked how to find topo number wihtout looping after reults from getTopologicalNumbersForAllAdjacentLabels
        if(!m_AllowFission || !m_AllowHandles){
            /// get the correct label to access the container of the particle
            int vContainerLabel = (aParticle.iCandidateLabel == 0) ? 
                    Math.abs(iLabelImage.getLabel(aParticle.iIndex)) : aParticle.iCandidateLabel;

            List<TopologicalNumberResult> topologicalNumbersForAllAdjacentLabels = m_TopologicalNumberFunction.getTopologicalNumbersForAllAdjacentLabels(iLabelImage.indexToPoint(aParticle.iIndex));
            TopologicalNumberResult vTopoNb = null;
            for (TopologicalNumberResult tnr : topologicalNumbersForAllAdjacentLabels) {
                if (tnr.iLabel == vContainerLabel) {
                    vTopoNb = tnr;
                    break;
                }
            }
            
            //FGandBGTopoNbPairType vTopoNb = m_TopologicalNumberFunction->EvaluateFGTNOfLabelAtIndex(aParticle.m_Index, vContainerLabel);
            if (vTopoNb == null) {System.out.println("NULL TOPO NUMBER !!!!!!!!!!!!! 1st"); return false;}
            System.out.println("TN: " + vTopoNb.iNumOfConnectedComponentsFG + " " + vTopoNb.iNumOfConnectedComponentsBG);
            if (!(vTopoNb.iNumOfConnectedComponentsFG == 1 && vTopoNb.iNumOfConnectedComponentsBG == 1)) {
                return false;
            }

            /// if the both labels are not 0, we must calculate the
            /// topo numbers for the current and the candidate label.
            if(0 != aParticle.iCandidateLabel) {
//                vTopoNb = m_TopologicalNumberFunction->EvaluateFGTNOfLabelAtIndex(aParticle.iIndex, aParticle.iCandidateLabel);
                vTopoNb = null;
                for (TopologicalNumberResult tnr : topologicalNumbersForAllAdjacentLabels) {
                    if (tnr.iLabel == aParticle.iCandidateLabel) {
                        vTopoNb = tnr;
                        break;
                    }
                }
                if (vTopoNb == null) {System.out.println("NULL TOPO NUMBER !!!!!!!!!!!!!  2nd"); return false;}    
                if (!(vTopoNb.iNumOfConnectedComponentsFG == 1 && vTopoNb.iNumOfConnectedComponentsBG == 1)) {
                    return false;
                }
            }
        }

        return true;
    }
    
    float MCMCproposal(int aIndex, int aCandLabel) {
        if (!m_MCMCuseBiasedProposal) return 1;

        // Length-prior driven proposal:
        float vLength = 0;
        boolean vFloating = true;
        int vL = iLabelImage.getLabelAbs(aIndex);
        Point p = iLabelImage.indexToPoint(aIndex);
        // TODO: maybe change to neighbor iterator
        for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; ++vN) {
            Point vOff = m_NeighborsOffsets_BG_Connectivity[vN];
            int vLN = iLabelImage.getLabelAbs(vOff.add(p));
            if(vLN != vL) {
                vLength += m_MCMClengthProposalMask[vN];
                vFloating = false;
            }
        }

        if (vFloating) {
            vLength = MCMCGetUnnormalizedFloatingParticleProposal();
        }
        return vLength;
    }

    float MCMCGetUnnormalizedFloatingParticleProposal() {
        // floating particles need a proposal > 0: We take half of the smallest element in the mask 
        // (hack:we assume the smallest element to  be at position 0).
        if (!m_MCMCuseBiasedProposal) return 1;
        return m_MCMClengthProposalMask[0]/2.0f;
    }

    //    typedef ConstIndexedHashSet<
    //    MinimalParticleType,
    //    MinimalParticleHashFunctorType,
    //    MinimalParticleHashFunctorType> MinimalParticleIndexedSetType;
    //    
    void MCMCgetRegularParticlesAtIndex(int aIndex, MinimalParticleIndexedSet aList) {
        int currentLabel = iLabelImage.getLabel(aIndex);
        System.out.println("       V: " + iLabelImage.indexToPoint(aIndex) + " = " + currentLabel);
        if (currentLabel == LabelImage.BorderLabel) return;
        int absCurrentLabel = iLabelImage.labelToAbs(currentLabel);

        boolean vParentInserted = false; /// just a speed up
        // TODO: Maybe MaskOnSpaceMapper should be used with pre-computed mask.
        for (Integer idx : iLabelImage.iterateNeighbours(aIndex)) {
            int neighborLabel = iLabelImage.getLabel(idx);
            
            System.out.println("idx: " + iLabelImage.indexToPoint(aIndex) + " n: " + iLabelImage.indexToPoint(idx) + " vNeighPixel: " + neighborLabel);
            
            int absNeighborLabel = iLabelImage.labelToAbs(neighborLabel);
            if (!iLabelImage.isBorderLabel(neighborLabel) && absNeighborLabel != absCurrentLabel) {
                // here should be a particle since two neighboring pixel have a different label.

                // Insert FG labels have a daughter placed at this spot.
                if (neighborLabel != LabelImage.BGLabel) {
                    float vProposal = MCMCproposal(aIndex, absNeighborLabel);
                    aList.insert(new MinimalParticle(aIndex, absNeighborLabel, vProposal));

                }
                
                if (currentLabel != LabelImage.BGLabel && !vParentInserted) {
                    float vProposal = MCMCproposal(aIndex, 0);
                    aList.insert(new MinimalParticle(aIndex, 0, vProposal));
                    System.out.println("  ParentInserted");
                    vParentInserted = true;
                }
            }
        }

        /// Check the BG neighborhood now if we need to insert a parent.
        if(!vParentInserted && currentLabel != LabelImage.BGLabel){ /// speed-up
            for (Integer idx : iLabelImage.iterateBgNeighbours(aIndex)) {
                
                int vNeighPixel = iLabelImage.getLabel(idx);
                System.out.println("--- xxx: " + iLabelImage.indexToPoint(idx) + " " + vNeighPixel);
                if (iLabelImage.labelToAbs(vNeighPixel) != absCurrentLabel) {
                    /// This is a FG pixel with a neighbor of a different label.
                    /// finally, insert a parent particle
                    float vProposal = MCMCproposal(aIndex, 0);
                    aList.insert(new MinimalParticle(aIndex, 0, vProposal));
                    vParentInserted = true;
                    break;
                }
            }
            // C code:
//            for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; vN++) {
//                Point vOff = m_NeighborsOffsets_BG_Connectivity[vN];
//                System.out.println("xxx: " + vOff);
//                int vNeighPixel = iLabelImage.getLabelAbs(vOff);
//
//                if (vNeighPixel != absCurrentLabel) {
//                    /// This is a FG pixel with a neighbor of a different label.
//                    /// finally, insert a parent particle
//                    float vProposal = MCMCproposal(aIndex, 0);
//                    aList.insert(new MinimalParticle(aIndex, 0, vProposal));
//                    vParentInserted = true;
//                    break;
//                }
//            }
        }
    }

    public boolean performIteration() {
        boolean convergence = true;

        // TODO: real job strats here

        return convergence;
    }

    public int getBiggestLabel() {
        return 10;
    }
    
    // DONE so FAR
    
    /**
     * Generates distribution from input image.ds
     * NOTICE: in case if values in input image are too small (< 10*eps(1)) it will provide flat
     * distribution and will change all pixels of input image to 1.0
     * @param aImg input image to generate distribution from
     * @return pair (sum of all pixels in image, generated distribution)
     */
    static Pair<Double, EnumeratedDistribution<Integer>> generateDiscreteDistribution(IntensityImage aImg) {
        ArrayList<Pair<Integer, Double>> pmf = new ArrayList<>(aImg.getSize());
        Iterator<Point> ri = new SpaceIterator(aImg.getDimensions()).getPointIterator();
        int index = 0;
        double m_MCMCZe = 0;
        while (ri.hasNext()) {
            final Point point = ri.next();
            double value = aImg.get(point);
            m_MCMCZe += value; 
            pmf.add(new Pair<Integer, Double>(index++, value));
        }
        // if sum of all pixels is too small  (< 10 * epislon(1f) as in original code) then just use flat distribution 
        if (m_MCMCZe < 10 * Math.ulp(1.0f)) {
            m_MCMCZe = aImg.getSize();
            index = 0;
            ri = new SpaceIterator(aImg.getDimensions()).getPointIterator();
            while (ri.hasNext()) {
                final Point point = ri.next();
                aImg.set(point, 1.0f);
                pmf.set(index, new Pair<Integer, Double>(index, 1.0));
                ++index;
            }
        }

        EnumeratedDistribution<Integer> m_MCMCEdgeImageDistr = new EnumeratedDistribution<>(new Rng(), pmf);

        return new Pair<> (m_MCMCZe, m_MCMCEdgeImageDistr);
    }
    
    

}
