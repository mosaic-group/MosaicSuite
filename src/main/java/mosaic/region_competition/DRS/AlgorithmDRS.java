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


    // Connectivities
    int m_NeighborhoodSize_FG_Connectivity;
    Point[] m_NeighborsOffsets_FG_Connectivity;
    int m_NeighborhoodSize_BG_Connectivity;
    Point[] m_NeighborsOffsets_BG_Connectivity;

    Rng m_NumberGenerator = new Rng(1212);
    // m_TopologicalNumberFunction
    float[] m_MCMClengthProposalMask = null;

    LabelImage m_MCMCRegularParticlesMap;
    double m_MCMCZe = 0.0; // sum of edge image values
    EnumeratedDistribution<Integer> m_MCMCEdgeImageDistr = null;
    private LabelImage m_MCMCInitBackupLabelImage;

    List<Integer> m_MCMCRegionLabel = new ArrayList<>();
    // ProposalNormalizerMapType
    Map<Integer, Float> m_MCMCparentsProposalNormalizer = new HashMap<>();
    Map<Integer, Float> m_MCMCchildrenProposalNormalizer = new HashMap<>();
    double m_MCMCTotalNormalizer;

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

        // OK.. here starts algorithm
        m_MCMCRegularParticlesMap = new LabelImage(iLabelImage.getDimensions());

        Pair<Double, EnumeratedDistribution<Integer>> distPair = generateDiscreteDistribution(iEdgeImage);
        m_MCMCZe = distPair.getFirst();
        m_MCMCEdgeImageDistr = distPair.getSecond();

        // Prepare a fast proposal computation:
        if (m_MCMCuseBiasedProposal) {
            m_MCMClengthProposalMask = new float[m_NeighborhoodSize_BG_Connectivity];
            for (int i = 0; i < m_NeighborhoodSize_BG_Connectivity; ++i) {
                Point offset = m_NeighborsOffsets_BG_Connectivity[i];
                m_MCMClengthProposalMask[i] = (float) (1.0 / offset.length());
                System.out.println(i + ": " + m_MCMClengthProposalMask[i]);
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

        final Iterator<Point> ri = new SpaceIterator(iLabelImage.getDimensions()).getPointIterator();
        while (ri.hasNext()) {
            final Point point = ri.next();
            int label = iLabelImage.getLabelAbs(point);
            System.out.println("Init label: " + label);
            if (iLabelImage.isBorderLabel(label)) continue;

            // Add if not added so far
            if (!visitedLabels.contains(label)) {
                m_MCMCRegionLabel.add(label);
                m_MCMCparentsProposalNormalizer.put(label, 0f);
                m_MCMCchildrenProposalNormalizer.put(label, 0f);
                visitedLabels.add(label);
            }
            //            TODO: rest of initialization
            //            
            //            // Add all regular particles at this spot:
            MinimalParticleIndexedSet vPs = new MinimalParticleIndexedSet();
            MCMCgetRegularParticlesAtIndex(iLabelImage.pointToIndex(point), vPs);
            //            
            //            for(unsigned vI = 0; vI < vPs.size(); vI++) {
            //                if(MCMCIsParticleTopoValid(vPs[vI])) {
            //                    MCMCInsertCandidatesToContainers(vPs[vI], vLabelIt.Get(), false);
            //                    m_LabelImage->SetPixel(vLabelIt.GetIndex(), -abs(vLabelIt.Get()));
            //                    m_MCMCRegularParticlesMap->SetPixel(vPs[vI].m_Index, true);
            //                    m_MCMCZe -= m_EdgeImage->GetPixel(vPs[vI].m_Index);
            //                }
            //            }
        }
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
        if (currentLabel == LabelImage.BorderLabel) return;
        int absCurrentLabel = iLabelImage.labelToAbs(currentLabel);

        boolean vParentInserted = false; /// just a speed up
        // TODO: Maybe MaskOnSpaceMapper should be used with pre-computed mask.
        for (Integer idx : iLabelImage.iterateNeighbours(aIndex)) {
            int neighborLabel = iLabelImage.getLabel(idx);
            System.out.println("idx:" + aIndex + " " + idx + " vNeighPixel: " + neighborLabel);
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
                    vParentInserted = true;
                }

            }
        }

        /// Check the BG neighborhood now if we need to insert a parent.
        if(!vParentInserted && currentLabel != 0){ /// speed-up
            for (int vN = 0; vN < m_NeighborhoodSize_BG_Connectivity; vN++) {
                Point vOff = m_NeighborsOffsets_BG_Connectivity[vN];
                System.out.println(vOff);
                int vNeighPixel = iLabelImage.getLabelAbs(vOff);

                if (vNeighPixel != absCurrentLabel) {
                    /// This is a FG pixel with a neighbor of a different label.
                    /// finally, insert a parent particle
                    float vProposal = MCMCproposal(aIndex, 0);
                    aList.insert(new MinimalParticle(aIndex, 0, vProposal));
                    vParentInserted = true;
                    break;
                }
            }
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
