package mosaic.region_competition.DRS;


import static mosaic.core.imageUtils.images.LabelImage.BGLabel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.region_competition.GUI.SegmentationProcessWindow;
import mosaic.region_competition.RC.ContourParticle;
import mosaic.region_competition.RC.LabelStatistics;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.Energy.EnergyResult;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.topology.TopologicalNumber;
import mosaic.region_competition.topology.TopologicalNumber.TopologicalNumberResult;
import mosaic.utils.Debug;


public class AlgorithmDRS {

    private static final Logger logger = Logger.getLogger(AlgorithmDRS.class);

    // Constant Parameters
    private int iMcmcStepSize = 1;
    private float iMcmcTemperature = 1;

    // Input for Algorithm
    private final LabelImage iLabelImage;
    private final IntensityImage iIntensityImage;
    private final IntensityImage iEdgeImage;
    private final ImageModel iImageModel;
    private final SettingsDRS iSettings;

    private int iAcceptedMoves = 0;
    private int iIterationCounter = 0;
    
    private Rng iNumberGenerator = new Rng(1212);
    private Rng m_NumberGeneratorBoost = new Rng();

    // Connectivities
    private Point[] iFgNeighborsOffsets;
    private Point[] iBgNeighborsOffsets;
    private TopologicalNumber iTopoFunction;

    private float[] m_MCMClengthProposalMask = null;

    private LabelImage m_MCMCRegularParticlesMap;
    private EnumeratedDistribution<Integer> m_MCMCEdgeImageDistr = null;

    private List<Integer> m_MCMCRegionLabel = new ArrayList<>();
    
    private Map<Integer, Float> iParentsProposalNormalizer = new HashMap<>();
    private Map<Integer, Float> iChildrenProposalNormalizer = new HashMap<>();
    private float iTotalNormalizer;

    private Map<Integer, MinimalParticleIndexedSet> iChildren = new HashMap<>();
    private Map<Integer, MinimalParticleIndexedSet> iParents = new HashMap<>();

    private class ParticleHistoryElement {
        final MinimalParticle particle;
        final int originalLabel;
        final boolean wasAdded;

        public ParticleHistoryElement(MinimalParticle vReplacedParticle, int aCurrentLabel, boolean b) {
            particle = vReplacedParticle;
            originalLabel = aCurrentLabel;
            wasAdded = b;
        }

        @Override
        public String toString() {
            return "PHE{ P:" + particle + ", L:" + originalLabel + ", A:" + wasAdded + "}";
        }
    }

    private List<ParticleHistoryElement> iParticlesHistory = new ArrayList<>();
    private List<ParticleHistoryElement> iFloatingParticlesHistory = new ArrayList<>();

    private final HashMap<Integer, LabelStatistics> iLabelStatistics = new HashMap<Integer, LabelStatistics>();

    private MinimalParticleIndexedSet iFloatingParticles = new MinimalParticleIndexedSet();

    private float iFloatingParticlesProposalNormalizer = 0;
    private int m_MCMCNumberOfSamplesForBiasedPropApprox = 30;
    
    
    public AlgorithmDRS(IntensityImage aIntensityImage, LabelImage aLabelImage, IntensityImage aEdgeImage, ImageModel aModel, SettingsDRS aSettings) {
        logger.debug("DRS algorithm created");

        iLabelImage = aLabelImage;
        iIntensityImage = aIntensityImage;
        iEdgeImage = aEdgeImage;
        iImageModel = aModel;
        iSettings = aSettings;
        logger.debug("Settings:\n" + Debug.getJsonString(iSettings));

        // Initialize label image
        iLabelImage.initBorder();
//        iLabelImage.initContour();

        // init connectivities
        Connectivity connFG = iLabelImage.getConnFG();
        iFgNeighborsOffsets = connFG.getPointOffsets();
        Connectivity connBG = iLabelImage.getConnBG();
        iBgNeighborsOffsets = connBG.getPointOffsets();
        // TODO: m_TopologicalNumberFunction - set / check what is it.
        iTopoFunction = new TopologicalNumber(iLabelImage);

        // OK.. here starts algorithm
        m_MCMCRegularParticlesMap = new LabelImage(iLabelImage.getDimensions());

        m_MCMCEdgeImageDistr = generateDiscreteDistribution(iEdgeImage, m_NumberGeneratorBoost);

        // Prepare a fast proposal computation:
        if (iSettings.useBiasedProposal) {
            m_MCMClengthProposalMask = new float[iBgNeighborsOffsets.length];
            for (int i = 0; i < iBgNeighborsOffsets.length; ++i) {
                Point offset = iBgNeighborsOffsets[i];
                m_MCMClengthProposalMask[i] = (float) (1.0 / offset.length());
            }
        }

        // Register all labels from lableImage
        Set<Integer> visitedLabels = new HashSet<>();

        // By default add background
        m_MCMCRegionLabel.add(LabelImage.BGLabel);
        iParentsProposalNormalizer.put(LabelImage.BGLabel, 0f);
        iChildrenProposalNormalizer.put(LabelImage.BGLabel, 0f);
        visitedLabels.add(LabelImage.BGLabel);
        iTotalNormalizer = 0.0f;

        // TODO: It seems that input label image cannot have negative labels, this code temporary changes it to all positives.
//        final Iterator<Point> ri2 = new SpaceIterator(iLabelImage.getDimensions()).getPointIterator();
//        while (ri2.hasNext()) {
//            Point p = ri2.next();
//            iLabelImage.setLabel(p, iLabelImage.getLabelAbs(p));
//        }

        final Iterator<Point> ri = new SpaceIterator(iLabelImage.getDimensions()).getPointIterator();
        while (ri.hasNext()) {
            final Point point = ri.next();
            int label = iLabelImage.getLabel(point);
//            //System.out.println("------------ Init label: " + label + " at " + point);
            if (iLabelImage.isBorderLabel(label)) continue;

            // Add if not added so far
            int labelAbs = iLabelImage.labelToAbs(label);
            if (!visitedLabels.contains(labelAbs)) {
                visitedLabels.add(labelAbs);
                m_MCMCRegionLabel.add(label);
                iParentsProposalNormalizer.put(labelAbs, 0f);
                iChildrenProposalNormalizer.put(labelAbs, 0f);
            }
            // // Add all regular particles at this spot:
            MinimalParticleIndexedSet vPs = new MinimalParticleIndexedSet();
            getRegularParticles(iLabelImage.pointToIndex(point), vPs);

            for (int vI = 0; vI < vPs.size(); ++vI) {
                if (isParticleTopoValid(vPs.elementAt(vI))) {
                    insertCandidatesToContainers(vPs.elementAt(vI), label, false);
                    iLabelImage.setLabel(point, -labelAbs);
                    m_MCMCRegularParticlesMap.setLabel(vPs.elementAt(vI).iIndex, 1);
                }
            }
        }

        initStatistics();
        prepareEnergies(); // TODO: initEnergies from RC - should be handled differently (energies cleanup)
    }

    private boolean MCMCDoIteration() {
        PrepareEnergyCalculationForEachIteration();

        float vTotEnergyDiff = 0;
        boolean vHardReject = false;

        /// These list will help to revert the move in case it gets rejected.
        // ParticleHistoryElementListType vHistoryOfMovedParticles;
        iParticlesHistory.clear();
        iFloatingParticlesHistory.clear();
        iLabelImageHistory.clear();

        /// The following check is theoretically not needed as there should
        /// always be a floating particle somewhere (given the algorithm
        /// got initialized with more than one region). But in the burn-in phase
        /// we delete floating particles.
        //System.out.println("m_MCMCRegionLabel: " + m_MCMCRegionLabel );
        if (m_MCMCRegionLabel.size() < 2) {
            // TODO: Should be handled differently than RuntimeEx?
            throw new RuntimeException("No active region for MCMC available in iter: " + iIterationCounter);
        }

        /// Sample a region number (a FG region number; without BG)
        int vAbsLabelIndex = iNumberGenerator.GetIntegerVariate(m_MCMCRegionLabel.size() - 2) + 1;

        // FAKE:
//         vAbsLabelIndex = (int)rndFake[rndCnt++];

        //System.out.println("m_NumberGenerator1: " + vAbsLabelIndex);
        /// Find the corresponding label
        int vAbsLabel = m_MCMCRegionLabel.get(vAbsLabelIndex);
        //System.out.println("m_MCMCRegionLabel: " + m_MCMCRegionLabel + " " + vAbsLabelIndex);
        if (iSettings.allowFission && iSettings.allowFusion) {
            // linear annealing
            float vOffboundaryPerc = iSettings.offBoundarySampleProbability * (1.0f - (iIterationCounter) / (iSettings.burnInFactor * iSettings.maxNumOfIterations));
            //System.out.println("iSettings.m_OffBoundarySampleProbability: " + iSettings.offBoundarySampleProbability + " " + m_iteration_counter + " " + iSettings.burnInFactor + " " + iSettings.maxNumOfIterations);
            double rnd = iNumberGenerator.GetVariate();

            // FAKE:
//             rnd = rndFake[rndCnt++];

            //System.out.println("m_NumberGenerator2: " + rnd);
            boolean vOffBoundarySampling = (vOffboundaryPerc > 0) ? rnd < vOffboundaryPerc : false;
            //System.out.println("vOffBoundarySampling: " + vOffBoundarySampling + " " + vOffboundaryPerc + " " + rnd);
            if (vOffBoundarySampling) {
                double rnd2 = iNumberGenerator.GetUniformVariate(0, 1);

                // FAKE:
//                 rnd2 = rndFake[rndCnt++];

                boolean vGrowth = rnd2 < 0.5;
                //System.out.println("m_NumberGenerator3: " + rnd2);
                return sampleOffBoundary(vGrowth, vAbsLabel);
            }
        }

        /// Figure out if Particle A will cause growth, shrinkage or be floating particle
        double vProbabilityToProposeAFloatingParticle = // (m_MCMCFloatingParticles.size()>0)?0.9:0;
                iFloatingParticlesProposalNormalizer / (iTotalNormalizer + iFloatingParticlesProposalNormalizer);
        //System.out.println("vProbabilityToProposeAFloatingParticle: " + vProbabilityToProposeAFloatingParticle + " " + m_FloatingParticlesProposalNormalizer + " " + m_MCMCTotalNormalizer + " " + m_FloatingParticlesProposalNormalizer);
        // double vZetaCorrectionFactor = (m_MCMCFloatingParticles.size()>0)?0.5:0;
        boolean vParticleAIsFloating = false;
        MinimalParticleIndexedSet vActiveCandidates = null;
        double vR = iNumberGenerator.GetUniformVariate(0.0, 1.0);

        // FAKE:
//         vR = rndFake[rndCnt++];

        //System.out.println("m_NumberGenerator4: " + vR);

        if (vR < vProbabilityToProposeAFloatingParticle) { // + vZetaCorrectionFactor) {
            /// We will choose one out of the floating particles
            vActiveCandidates = iFloatingParticles;
            vParticleAIsFloating = true;
        }
        else if (vR < 0.5 * (vProbabilityToProposeAFloatingParticle + 1)) { // + vZetaCorrectionFactor)) {
            //System.out.println("second else");
            //System.out.println("m_MCMCchildren: " + m_MCMCchildren);
            /// We will grow and hence choose one out of the children list
            vActiveCandidates = iChildren.get(vAbsLabel);
            if (vActiveCandidates == null) {
                // vActiveCandidates = new MinimalParticleIndexedSet();
                // m_MCMCchildren.put(vAbsLabel, vActiveCandidates);
            }
        }
        else {
            //System.out.println("else\n");
            vActiveCandidates = iParents.get(vAbsLabel);
            if (vActiveCandidates == null) {
                // vActiveCandidates = new MinimalParticleIndexedSet();
                // m_MCMCparents.put(vAbsLabel, vActiveCandidates);
            }
        }
        //System.out.println("vR/vProbabilityToProposeAFloatingParticle: " + vR + "/" + vProbabilityToProposeAFloatingParticle + " " + vActiveCandidates);

        if (vActiveCandidates == null || vActiveCandidates.size() == 0) {
            /// This is an empty region. Maybe there exists a floating particle
            /// with no future for this region. But if m_Count == 0, it will not
            /// be accepted according to the definition of the energy. We hence
            /// cleanup the statistics (kill the region).

            MCMCUpdateRegionLabelVector();
            return false;
        }

        /// For each particle within the region, calculate the proposal
        // List<Float> vAllParticlesFwdProposals = new ArrayList<>();
        ArrayList<Pair<Integer, Double>> vAllParticlesFwdProposals = new ArrayList<>();

        /// Create a discrete distribution over particles
        int vIndexOffset = 0;
        boolean vApproxedIndex = false;
        // typedef boost::random::discrete_distribution<int, float> DiscreteDistType;
        // DiscreteDistType* vDiscreteDistr;

        if (iSettings.useBiasedProposal && !vParticleAIsFloating && (m_MCMCNumberOfSamplesForBiasedPropApprox == 0 || m_MCMCNumberOfSamplesForBiasedPropApprox >= vActiveCandidates.size())) {
            // vAllParticlesFwdProposals.reserve(vActiveCandidates.size());

            int index = 0;
            for (int vI = 0; vI < vActiveCandidates.size(); vI++) {
                vAllParticlesFwdProposals.add(new Pair<>(index++, (double) vActiveCandidates.elementAt(vI).iProposal));
            }
            //System.out.println("vAllParticlesFwdProposals1: " + vAllParticlesFwdProposals);
            //System.out.println(vAllParticlesFwdProposals.size());

        }
        else if (iSettings.useBiasedProposal && !vParticleAIsFloating) {
            vApproxedIndex = true;
            // vAllParticlesFwdProposals.reserve(m_MCMCNumberOfSamplesForBiasedPropApprox);
            vIndexOffset = iNumberGenerator.GetIntegerVariate(vActiveCandidates.size() - 1);

            // FAKE:
//             vIndexOffset = (int) rndFake[rndCnt++];

            //System.out.println("m_NumberGenerator5: " + vIndexOffset);
            int index = 0;
            for (int vI = 0; vI < m_MCMCNumberOfSamplesForBiasedPropApprox; vI++) {
                int vApproxParticleIndex = (vIndexOffset + vI) % vActiveCandidates.size();
                vAllParticlesFwdProposals.add(new Pair<>(index++, (double) vActiveCandidates.elementAt(vApproxParticleIndex).iProposal));
            }
            //System.out.println("2: " + vAllParticlesFwdProposals);
            //System.out.println(vAllParticlesFwdProposals.size());
        }
        else {
            // TODO: Dummy data since EnumeratedDistribution needs sth. This else is not needed and should be removed with some refactoring.
            vAllParticlesFwdProposals.add(new Pair<>(1, 1.0)); 
        }
        EnumeratedDistribution<Integer> vDiscreteDistr = new EnumeratedDistribution<>(m_NumberGeneratorBoost, vAllParticlesFwdProposals);

        /// Draw n particles from the discrete distribution
        ArrayList<MinimalParticle> vCandidateMoveVec = new ArrayList<>(Arrays.asList(new MinimalParticle[iMcmcStepSize]));
        ArrayList<MinimalParticle> vPartnerMoveVec = new ArrayList<>(Arrays.asList(new MinimalParticle[iMcmcStepSize]));
        for (int i = 0; i < iMcmcStepSize; ++i) {
            vCandidateMoveVec.set(i, new MinimalParticle());
            vPartnerMoveVec.set(i, new MinimalParticle());
        }

        // TODO: Can thos be changed to regular primitive arrays? And must they be pre-filled?
        ArrayList<Float> vq_A = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vq_A, 0f);
        ArrayList<Float> vq_B = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vq_B, 0f);
        ArrayList<Float> vq_A_B = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vq_A_B, 0f);
        ArrayList<Float> vq_B_A = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vq_B_A, 0f);
        ArrayList<Float> vqb_A = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vqb_A, 0f);
        ArrayList<Float> vqb_B = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vqb_B, 0f);
        ArrayList<Float> vqb_A_B = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vqb_A_B, 0f);
        ArrayList<Float> vqb_B_A = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vqb_B_A, 0f);
        ArrayList<Float> vLabelsBeforeJump_A = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vLabelsBeforeJump_A, 0f);
        ArrayList<Float> vLabelsBeforeJump_B = new ArrayList<>(Arrays.asList(new Float[iMcmcStepSize]));
        Collections.fill(vLabelsBeforeJump_B, 0f);

        ArrayList<Boolean> vParticle_Ab_IsFloating = new ArrayList<>(Arrays.asList(new Boolean[iMcmcStepSize]));
        Collections.fill(vParticle_Ab_IsFloating, Boolean.FALSE);
        ArrayList<Boolean> vParticle_Bb_IsFloating = new ArrayList<>(Arrays.asList(new Boolean[iMcmcStepSize]));
        Collections.fill(vParticle_Bb_IsFloating, Boolean.FALSE);
        ArrayList<Boolean> vParticle_A_IsFloating = new ArrayList<>(Arrays.asList(new Boolean[iMcmcStepSize]));
        Collections.fill(vParticle_A_IsFloating, Boolean.FALSE);

        boolean vSingleParticleMoveForPairProposals = false;

        /// Find particle A:
        for (int vPC = 0; vPC < iMcmcStepSize; vPC++) {

            int vParticleIndex;
            if (iSettings.useBiasedProposal && !vParticleAIsFloating) {
                vParticleIndex = vDiscreteDistr.sample();

                //System.out.println("distr1: " + vParticleIndex);

                // FAKE:
//                 vParticleIndex = distrFake[distrCnt++];

                //System.out.println("vParticleIndex1: " + vParticleIndex);
                vq_A.set(vPC, vAllParticlesFwdProposals.get(vParticleIndex).getSecond().floatValue());

                if (vApproxedIndex) {
                    vParticleIndex = (vIndexOffset + vParticleIndex) % vActiveCandidates.size();
                    //System.out.println("vParticleIndex2: " + vParticleIndex);
                }
            }
            else {
                vParticleIndex = iNumberGenerator.GetIntegerVariate(vActiveCandidates.size() - 1);

                // FAKE:
//                 vParticleIndex = (int) rndFake[rndCnt++];

                //System.out.println("m_NumberGenerator6: " + vParticleIndex);
                //System.out.println("vParticleIndex3: " + vParticleIndex);
                vq_A.set(vPC, 1.0f);
            }

            MinimalParticle vParticle = new MinimalParticle(vActiveCandidates.elementAt(vParticleIndex));
            //System.out.println("vParticle in loop: " + vParticle + " " + vParticleAIsFloating + " " + iLabelImage.getLabel(vParticle.iIndex));
            if (vParticleAIsFloating) {
                /// A floating particle was selected.
                vParticle_A_IsFloating.set(vPC, true);

                // Immediately accept the move (self transition) if the label
                // at the particular position is the same. Since this is a
                // self transition we keep the particle in the proposals.
                if (Math.abs(iLabelImage.getLabel(vParticle.iIndex)) == vParticle.iCandidateLabel) {
                    return false; // reject.
                }

                // METHOD 4: reject (because the backward prob is 0) --> 49.2 %
                if (MCMCIsRegularParticle(vParticle, Math.abs(iLabelImage.getLabel(vParticle.iIndex)))) {
                    return false;
                }
                // if we are here, do the job:
                eraseFloatingParticle(vParticle, true);
            }
            /// Fill in some properties for this particles:
            vLabelsBeforeJump_A.set(vPC, (float) Math.abs(iLabelImage.getLabel(vParticle.iIndex)));
            if (vParticle_A_IsFloating.get(vPC)) {
                vq_A.set(vPC, vq_A.get(vPC) / iFloatingParticlesProposalNormalizer);
            }
            else {
                vq_A.set(vPC, vq_A.get(vPC) / MCMCGetProposalNormalizer(vLabelsBeforeJump_A.get(vPC).intValue(), vParticle.iCandidateLabel));
            }
            vCandidateMoveVec.set(vPC, vParticle);
        }
        //System.out.println("vLabelsBeforeJump_A: " + vLabelsBeforeJump_A);
        //System.out.println("vLabelsBeforeJump_B: " + vLabelsBeforeJump_B);
        //System.out.println("vCandidateMoveVec:\n" + vCandidateMoveVec);
        //System.out.println("vq_A:\n" + vq_A);

        if (iSettings.useBiasedProposal && !vParticleAIsFloating) {
            vDiscreteDistr = null;
        }

        /// In case of pair proposals, we find a partner for each proposed particle.
        /// We now know A and Q(A). Now it needs to build
        /// another (2nd step) discrete proposal distribution. We sample from it
        /// to determine the partner particle B. Furthermore we calculate the
        /// conditional proposal probability Q(B|A). In a second step we calculate
        /// the conditional Q(A|B). The same then needs to be done for the backward
        /// probabilities Qb(A), Qb(B), Qb(A|B) and Qb(B|A).
        /// Notation:
        /// - Q is the forward and Qb the backward probability. A is
        /// a forward praticle and B' the backward particle.
        /// - Qb always assumes backward particles as its arguments! Hence,
        /// Qb_A_B is the probabily Qb(A'|B').
        //System.out.println("iSettings.m_MCMCusePairProposal: " + iSettings.usePairProposal);
        if (iSettings.usePairProposal) {

            /// Iterate over particles A:
            for (int vPC = 0; vPC < vCandidateMoveVec.size(); ++vPC) {
                MinimalParticle vPartIt = vCandidateMoveVec.get(vPC);
                MinimalParticle vA = new MinimalParticle(vPartIt);

                /// TODO: the next line shouldn't change anything?
                vA.iProposal = calculateProposal(vA.iIndex);

                //System.out.println("MCMCApplyParticle1");
                MCMCApplyParticle(vPartIt, true);
                //System.out.println("MCMCApplyParticle1 after: " + vPartIt);
                //System.out.println("vCandidateMoveVec1: " + vCandidateMoveVec);

                /// BTW we have to remember if A' is a floating particle in the
                /// state x -> A.
                vParticle_Ab_IsFloating.set(vPC, MCMCParticleHasFloatingProperty(vPartIt.iIndex, vPartIt.iCandidateLabel));

                /// Get the particles involved in the second step of the
                /// proposal (with updated proposals)
                MinimalParticleIndexedSet vParts_Q_BgivenA = new MinimalParticleIndexedSet();
                //System.out.println("MCMCgetPartnerParticleSet1: " + vA + " " + vParts_Q_BgivenA);
                getPartnerParticles(vA, vParts_Q_BgivenA);
                //System.out.println("MCMCgetPartnerParticleSet1 after: " + vA + " " + vParts_Q_BgivenA);

                /// Find B:
                MinimalParticle vB = new MinimalParticle();

                /// Choose B from Q(B|A) and calculate Q(B|A).
                if (iSettings.useBiasedProposal) {
                    ArrayList<Float> vProposalsVector = new ArrayList<>(vParts_Q_BgivenA.size());
                    float vNormalizer_Q_B_A = 0;

                    for (int vPI = 0; vPI < vParts_Q_BgivenA.size(); vPI++) {
                        MinimalParticle vCondParticle = vParts_Q_BgivenA.elementAt(vPI);
                        vProposalsVector.add(vCondParticle.iProposal);
                        vNormalizer_Q_B_A += vCondParticle.iProposal;
                    }
                    EnumeratedDistribution<Integer> vQ_B_A = createDiscreteDistrFromValues(vProposalsVector);
                    int vCondIndex = vQ_B_A.sample();
                    vB = vParts_Q_BgivenA.elementAt(vCondIndex);
                    /// The value m_Proposal of vB is currently equal to Q_B_A.
                    vq_B_A.set(vPC, vB.iProposal / vNormalizer_Q_B_A);
                }
                else {
                    int vPartI = iNumberGenerator.GetIntegerVariate(vParts_Q_BgivenA.size() - 1);
                    vB = vParts_Q_BgivenA.elementAt(vPartI);
                    vq_B_A.set(vPC, 1.0f / vParts_Q_BgivenA.size());
                }

                /// store B (and its original label).
                vPartnerMoveVec.set(vPC, vB);
                vLabelsBeforeJump_B.set(vPC, (float) Math.abs(iLabelImage.getLabel(vB.iIndex)));
                //System.out.println("vB vs vA= " + vA + " " + vB );
                if (vB.iIndex == vA.iIndex && vB.iCandidateLabel == vA.iCandidateLabel) {
                    vSingleParticleMoveForPairProposals = true;
                    vLabelsBeforeJump_B.set(vPC, vLabelsBeforeJump_A.get(vPC));
                }

                /// Get the reverse particle of A (without proposal update as it
                /// is not necessary):
                MinimalParticle vReverseParticleA = new MinimalParticle(vPartIt);
                //System.out.println("vReverseParticleA bef: " + vReverseParticleA);
                vReverseParticleA.iCandidateLabel = vLabelsBeforeJump_A.get(vPC).intValue();
                //System.out.println("vReverseParticleA aft: " + vReverseParticleA);

                /// In case that vA == vB, we must already undo the simulated
                /// move in order to calculate Q'(B'|A') (== Q'(A'|B'))
                if (vSingleParticleMoveForPairProposals) {
                    //System.out.println("MCMCApplyParticle2");
                    MCMCApplyParticle(vReverseParticleA, true);
                }

                /// Get the reverse particle of B:
                MinimalParticle vReverseB = new MinimalParticle(vB);
                vReverseB.iCandidateLabel = vLabelsBeforeJump_B.get(vPC).intValue();
                vReverseB.iProposal = calculateProposal(vReverseB.iIndex);

                /// in the current state of the label image and the
                /// containers we can calculate qb_A'_B' as well. We assume now
                /// that B' was applied and we calculate the probability for A'.
                MinimalParticleIndexedSet vParts_Qb_AgivenB = new MinimalParticleIndexedSet();
                getPartnerParticles(vB, vParts_Qb_AgivenB);

                if (iSettings.useBiasedProposal) {
                    //System.out.println("vParts_Qb_AgivenB: " + vParts_Qb_AgivenB);
                    float vNormalizer_Qb_A_B = 0;
                    for (int vPI = 0; vPI < vParts_Qb_AgivenB.size(); ++vPI) {
                        vNormalizer_Qb_A_B += vParts_Qb_AgivenB.elementAt(vPI).iProposal;
                    }

                    float vqb_A_B_unnorm = calculateProposal(vA.iIndex);
                    vqb_A_B.set(vPC, vqb_A_B_unnorm / vNormalizer_Qb_A_B);
                    //System.out.println("vqb_A_B[" + vPC + "] = " + (vqb_A_B_unnorm / vNormalizer_Qb_A_B) + " " + vNormalizer_Qb_A_B + " " + vqb_A_B_unnorm);
                }
                else {
                    //System.out.println("vqb_A_B[" + vPC + "] = " + (1.0f / vParts_Qb_AgivenB.size()));
                    vqb_A_B.set(vPC, 1.0f / vParts_Qb_AgivenB.size());
                }

                if (!vSingleParticleMoveForPairProposals) {

                    /// undo the simulated move.
                    //System.out.println("MCMCApplyParticle3");
                    MCMCApplyParticle(vReverseParticleA, true);

                    /// Now we can calculate Q_B (as we now know B and the original
                    /// state has been recovered).
                    if (!MCMCIsRegularParticle(vB, vLabelsBeforeJump_B.get(vPC).intValue())) {
                        vq_B.set(vPC, 0f);
                    }
                    else {
                        if (iSettings.useBiasedProposal) {
                            /// vB.mProposal needs to be recalculated here:
                            vq_B.set(vPC, calculateProposal(vB.iIndex) / MCMCGetProposalNormalizer(vLabelsBeforeJump_B.get(vPC).intValue(), vB.iCandidateLabel));
                        }
                        else {
                            vq_B.set(vPC, 1.0f / MCMCGetProposalNormalizer(vLabelsBeforeJump_B.get(vPC).intValue(), vB.iCandidateLabel));
                        }
                    } /// end if particle B exists
                }
            } /// end loop particles (all the A particles)
        } /// end if (pair proposal) condition

        /// Currently it is possible that the same candidate is in the move set.
        /// Hence we store the applied moves to avoid duplicates.
        MinimalParticleIndexedSet vAppliedParticles = new MinimalParticleIndexedSet();
        ArrayList<Integer> vAppliedParticleOrigLabels = new ArrayList<>();

        /// Iterate the candidates, calculate the energy and perform the moves.
        for (int vPC = 0; vPC < vCandidateMoveVec.size(); ++vPC) {
            MinimalParticle vParticleA = new MinimalParticle(vCandidateMoveVec.get(vPC));
            MinimalParticle vParticleB = new MinimalParticle(vPartnerMoveVec.get(vPC));

            /// apply particle A and B, start with B:
            int vN = (iSettings.usePairProposal && !vSingleParticleMoveForPairProposals) ? 2 : 1;
            for (; vN > 0; --vN) {

                /// it is necessary that we start with particle B as we have
                /// to calculate Q(A|B) and Qb(B|A).
                MinimalParticle vCurrentMinimalParticle = null;
                int vOriginalLabel;
                if (vN > 1) {
                    //System.out.println("vN > 1");
                    vCurrentMinimalParticle = new MinimalParticle(vParticleB);
                    vOriginalLabel = vLabelsBeforeJump_B.get(vPC).intValue();
                }
                else {
                    //System.out.println("vN <= 1");
                    vCurrentMinimalParticle = new MinimalParticle(vParticleA);
                    vOriginalLabel = vLabelsBeforeJump_A.get(vPC).intValue();
                }

                /// We calculate the energy and apply them move iff
                /// - the move has not been performed beforehand (a particle
                /// was sampled twice)
                /// - THE FOLLOWING IS CURRENTLY A NON ISSUE AS B CANNOT BE A':
                /// particle B is not the reverse particle of particle A (in
                /// case of m_usePairProposals, i.e. vN > 1). This is important
                /// because the energy update gets corrupted as particle B is
                /// not a valid particle before A was applied. To resolve this
                /// we just perform a one particle move (we don't apply B).

                if (vAppliedParticles.find(vCurrentMinimalParticle) == vAppliedParticles.size()) {
                    /// Calculate the energy difference when changing this candidate:
                    //System.out.println("vCurrentMinimalParticle: " + vCurrentMinimalParticle);
                    vTotEnergyDiff += CalculateEnergyDifference(vCurrentMinimalParticle.iIndex, vOriginalLabel, vCurrentMinimalParticle.iCandidateLabel,
                            iIntensityImage.get(vCurrentMinimalParticle.iIndex));
                     //System.out.println("vTotEnergyDiff: " + vTotEnergyDiff);
                    /// Finally, perform the (particle-)move
                    //System.out.println("MCMCApplyParticle4\n");
                    MCMCApplyParticle(vCurrentMinimalParticle, false);

                    vAppliedParticles.insert(vCurrentMinimalParticle);
                    vAppliedParticleOrigLabels.add(vOriginalLabel);
                }

                /// Calculate Q(A|B) and Qb(B|A) in case we moved B only; this is
                /// when vN == 2.
                if (vN == 2) {
                    //System.out.println("!!!!! vn==2");
                    /// Get the neighbors (conditional particles) and sum up
                    /// their proposal values; this is the normalizer for the
                    /// discrete probability Q(A|B)
                    MinimalParticleIndexedSet vParts_Q_AgivenB = new MinimalParticleIndexedSet();
                    getRegularParticlesInFgNeighborhood(vParticleB.iIndex, vParts_Q_AgivenB);
                    /// add particle B as this is always a candidate as well
                    vParticleB.iProposal = calculateProposal(vParticleB.iIndex);
                    vParts_Q_AgivenB.insert(vParticleB);

                    if (iSettings.useBiasedProposal) {
                        float vNormalizer_Q_A_B = 0;
                        for (int vPI = 0; vPI < vParts_Q_AgivenB.size(); vPI++) {
                            vNormalizer_Q_A_B += vParts_Q_AgivenB.elementAt(vPI).iProposal;
                        }
                        /// vParticleA.m_Proposal is not valid anymore. Particle A
                        /// got a new proposal when applying particle B.
                        float vProposalA = calculateProposal(vParticleA.iIndex);
                        vq_A_B.set(vPC, vProposalA / vNormalizer_Q_A_B);
                    }
                    else {
                        vq_A_B.set(vPC, 1.0f / vParts_Q_AgivenB.size());
                    }

                    /// create A'
                    MinimalParticle vReverseParticleA = new MinimalParticle(vParticleA);
                    vReverseParticleA.iCandidateLabel = vLabelsBeforeJump_A.get(vPC).intValue();
                    vReverseParticleA.iProposal = calculateProposal(vReverseParticleA.iIndex);

                    /// Calculate Qb(B'|A')
                    MinimalParticleIndexedSet vParts_Qb_BgivenA = new MinimalParticleIndexedSet();
                    getRegularParticlesInFgNeighborhood(vParticleA.iIndex, vParts_Qb_BgivenA);
                    vParts_Qb_BgivenA.insert(vReverseParticleA);
                    if (iSettings.useBiasedProposal) {
                        float vNormalizer_Qb_B_A = 0;
                        for (int vPI = 0; vPI < vParts_Qb_BgivenA.size(); vPI++) {
                            vNormalizer_Qb_B_A += vParts_Qb_BgivenA.elementAt(vPI).iProposal;
                        }
                        /// the proposal of the backward particle (given A) is:
                        float vProposalBb = calculateProposal(vParticleB.iIndex);
                        vqb_B_A.set(vPC, vProposalBb / vNormalizer_Qb_B_A);
                    }
                    else {
                        vqb_B_A.set(vPC, 1.0f / vParts_Qb_BgivenA.size());
                    }
                }
            }
        }

        for (int vPC = 0; vPC < vCandidateMoveVec.size(); ++vPC) {
            /// Correct the containers whenever floating particles were involved:
            /// The method moveParticles, for simplicity, only works on the regular
            /// particle set.

            /// First, figure out if A' or B' is floating:
            MinimalParticle vParticleA = vCandidateMoveVec.get(vPC);
            MinimalParticle vParticleB = vPartnerMoveVec.get(vPC);

            /// Figure out if the backward particles are floating:
            if (iSettings.usePairProposal) {
                vParticle_Bb_IsFloating.set(vPC, MCMCParticleHasFloatingProperty(vParticleB.iIndex, vLabelsBeforeJump_B.get(vPC).intValue()));
            }
            else {
                /// if we're not in pair proposal mode we did not yet check if
                /// A's reverse particle is floating (else we did already):
                vParticle_Ab_IsFloating.set(vPC, MCMCParticleHasFloatingProperty(vParticleA.iIndex, vLabelsBeforeJump_A.get(vPC).intValue()));
            }

            MinimalParticle vReverseFloatingP = null;
            int vLabelBeforeJump;

            /// the first condition is needed when not using pair proposal mode
            if (vParticle_Ab_IsFloating.get(vPC)) {
                vReverseFloatingP = new MinimalParticle(vCandidateMoveVec.get(vPC));
                vLabelBeforeJump = vLabelsBeforeJump_A.get(vPC).intValue();
                vReverseFloatingP.iCandidateLabel = vLabelBeforeJump;
                vReverseFloatingP.iProposal = calculateProposal(vReverseFloatingP.iIndex);
            }
            /// in pair proposal, if A' is floating, B' is as well (they are the
            /// same particle):
            if (iSettings.usePairProposal && vParticle_Bb_IsFloating.get(vPC)) { // only possible in pair proposal mode
                vReverseFloatingP = new MinimalParticle(vPartnerMoveVec.get(vPC));
                vLabelBeforeJump = vLabelsBeforeJump_B.get(vPC).intValue();
                vReverseFloatingP.iCandidateLabel = vLabelBeforeJump;
                vReverseFloatingP.iProposal = calculateProposal(vReverseFloatingP.iIndex);
            }

            /// finally convert the regular particle into a floating particle,
            /// i.e. insert it in the floating DS and remove it from the regular:
            if (vParticle_Ab_IsFloating.get(vPC) || vParticle_Bb_IsFloating.get(vPC)) {

                /// insert the reverse particle in the appropriate container. If
                /// there is no space, we reject the move.
                if (!(insertFloatingParticle(vReverseFloatingP, true))) {
                    // TODO: calling MCMCReject from here invalidates the result.
                    // MCMCReject(&vAppliedParticles,&vAppliedParticleOrigLabels);
                    vHardReject = true;
                }
            }
        }

        //System.out.println("m_MCMCFloatingParticles: " + m_MCMCFloatingParticles);
        //System.out.println("m_FloatingParticlesProposalNormalizer: " + m_FloatingParticlesProposalNormalizer);

        //System.out.println("vq(s) bef: " + vq_B + vq_A_B + vqb_B_A + vqb_B);
        /// We are now in the state x'.
        /// Calculate Q'(A) and maybe Q'(B). Note that this has to be done after
        /// all particles were applied.
        for (int vPC = 0; vPC < vCandidateMoveVec.size(); ++vPC) {
            MinimalParticle vParticleA = vCandidateMoveVec.get(vPC);
            MinimalParticle vParticleB = vPartnerMoveVec.get(vPC);

            /// Calculate vqb_A and vqb_B
            if (!iSettings.useBiasedProposal) {
                vqb_A.set(vPC, 1.0f);
                vqb_B.set(vPC, 1.0f);
            }
            else {
                vqb_A.set(vPC, calculateProposal(vParticleA.iIndex));
                if (iSettings.usePairProposal && !vSingleParticleMoveForPairProposals) {
                    vqb_B.set(vPC, calculateProposal(vParticleB.iIndex));
                }
            }
            /// Normalize vqb_A and vqb_B
            float vqb_A_normalizer = (vParticle_Ab_IsFloating.get(vPC)) ? (iFloatingParticlesProposalNormalizer)
                    : MCMCGetProposalNormalizer(vParticleA.iCandidateLabel, vLabelsBeforeJump_A.get(vPC).intValue());
            vqb_A.set(vPC, vqb_A.get(vPC) / vqb_A_normalizer);
            if (iSettings.usePairProposal && !vSingleParticleMoveForPairProposals) {
                float vqb_B_normalizer = vParticle_Bb_IsFloating.get(vPC) ? (iFloatingParticlesProposalNormalizer)
                        : MCMCGetProposalNormalizer(vParticleB.iCandidateLabel, vLabelsBeforeJump_B.get(vPC).intValue());
                vqb_B.set(vPC, vqb_B.get(vPC) / vqb_B_normalizer);
            }

            /// Finally, we omit half of the calculations if particle A == B
            if (vSingleParticleMoveForPairProposals) {
                vq_B.set(vPC, vq_A.get(vPC));
                vq_A_B.set(vPC, vq_B_A.get(vPC));
                vqb_B_A.set(vPC, vqb_A_B.get(vPC));
                vqb_B.set(vPC, vqb_A.get(vPC));
            }
        }
        //System.out.println("vq(s) aft: " + vq_B + vq_A_B + vqb_B_A + vqb_B);

        /// Calculate the forward-backward ratio:
        float vForwardBackwardRatio = 1.0f;
        for (int vPC = 0; vPC < vCandidateMoveVec.size(); ++vPC) {

            if (iSettings.usePairProposal) {
                if (vParticle_Ab_IsFloating.get(vPC) || vParticle_Bb_IsFloating.get(vPC) || vParticle_A_IsFloating.get(vPC)) {
                    vForwardBackwardRatio *= (vqb_B.get(vPC) * vqb_A_B.get(vPC)) / (vq_A.get(vPC) * vq_B_A.get(vPC));
                }
                else {
                    vForwardBackwardRatio *= (vqb_A.get(vPC) * vqb_B_A.get(vPC) + vqb_B.get(vPC) * vqb_A_B.get(vPC)) / (vq_A.get(vPC) * vq_B_A.get(vPC) + vq_B.get(vPC) * vq_A_B.get(vPC));
                }
            }
            else {
                if (vParticle_A_IsFloating.get(vPC)) {
                    /// we distroy a floating particle, in the next iteration there
                    /// will be one floating particle less, hence the probability
                    /// in the x' to sample a floating particle is (note that
                    /// both normalizers are in state x'):
                    float vPProposeAFloatInXb = iFloatingParticlesProposalNormalizer / (iFloatingParticlesProposalNormalizer + iTotalNormalizer);

                    vForwardBackwardRatio *= 0.5f * (1 - vPProposeAFloatInXb) / vProbabilityToProposeAFloatingParticle;
                    vForwardBackwardRatio *= vqb_A.get(vPC) / vq_A.get(vPC);
                }
                else if (vParticle_Ab_IsFloating.get(vPC)) {

                    /// we create a floating particle, in the next iteration there
                    /// will be one floating particle more, hence the probability
                    /// in the x' to sample a floating particle is (note that
                    /// m_MCMCTotalNormalizer is updated to x'):
                    float vPProposeAFloatInXb = iFloatingParticlesProposalNormalizer / (iFloatingParticlesProposalNormalizer + iTotalNormalizer);
                    vForwardBackwardRatio *= vPProposeAFloatInXb / (0.5f * (1 - vProbabilityToProposeAFloatingParticle));
                    vForwardBackwardRatio *= vqb_A.get(vPC) / vq_A.get(vPC);
                }
                else {
                    /// Shrinkage and growth events have the same probability.
                    /// We hence only need to compare the individual particle
                    /// ratios.
                    vForwardBackwardRatio *= vqb_A.get(vPC) / vq_A.get(vPC);
                }
            }
        }

        //System.out.println("vForwardBackwardRatio: " + vForwardBackwardRatio);

        /// Compute the Hastingsratio:
        float vHastingsRatio = (float) Math.exp(-vTotEnergyDiff / iMcmcTemperature) * vForwardBackwardRatio;

        /// Should I stay or shoud I go; the Metropolis-Hastings algorithm:
        boolean vAccept = false;
        if (vHastingsRatio >= 1) {
            vAccept = true;
        }
        else {
//            vAccept = (vHastingsRatio > m_NumberGenerator.GetUniformVariate(0, 1));
            double rnd =iNumberGenerator.GetUniformVariate(0, 1);
//            rnd = rndFake[rndCnt++];
            if (vHastingsRatio > rnd) {
                vAccept = true;
            } else {
                vAccept = false;
            }
        }

        /// Register the result (if we accept) or rollback to the previous state.
        if (vAccept && !vHardReject) {
            // typename std::list<LabelAbsPixelType>::iterator vOrigLabelIt = vAppliedParticleOrigLabels.begin();
            for (int vM = 0; vM < vAppliedParticles.size(); ++vM) {
                int vOrigLabelIt = vAppliedParticleOrigLabels.get(vM);
                /// store the results and finish (next iteration).
                int vCandidateIndex = vAppliedParticles.elementAt(vM).iIndex;
                MCMCStoreResults(vCandidateIndex, vOrigLabelIt, iIterationCounter);

                /// something changed, particles were inserted and deleted. We need
                /// to update the edge-map constant.
                MCMCUpdateRegularParticleMapInNeighborhood(vAppliedParticles.elementAt(vM).iIndex);
            }
        }
        else {
            MCMCReject(vAppliedParticles, vAppliedParticleOrigLabels);
        }
        //System.out.println("================= IMPL END =================================");
        // IMPL

        return vAccept;
    }

    private void MCMCReject(MinimalParticleIndexedSet aAppliedParticles, ArrayList<Integer> aOrigLabels) {
        /// First, recover the theoretical state:
        /// - recover the label image
        /// - recover the particle set
        /// As we have some (redundant) speedup statistics we also need to:
        /// - recover the statistics (by simulation of the backward particle)
        /// - recover the proposal normalizers (taken care of within the
        /// insertion/deletion methods).
        for (int i = iParticlesHistory.size() - 1; i >= 0; --i) {
            ParticleHistoryElement phe = iParticlesHistory.get(i);
            if (phe.wasAdded) {
                eraseCandidatesFromContainers(phe.particle, phe.originalLabel, false);
            }
            else {
                insertCandidatesToContainers(phe.particle, phe.originalLabel, false);
            }
        }

        for (int i = iFloatingParticlesHistory.size() - 1; i >= 0; --i) {
            ParticleHistoryElement phe = iFloatingParticlesHistory.get(i);
            if (phe.wasAdded) {
                eraseFloatingParticle(phe.particle, false);
            }
            else {
                MCMCInsertFloatingParticleCumulative(phe.particle, false);
            }
        }

        /// recover the label image:
        for (int i = iLabelImageHistory.size() - 1; i >= 0; --i) {
            LabelImageHistoryEvent vRLIt = iLabelImageHistory.get(i);
            iLabelImage.setLabel(vRLIt.index, vRLIt.label);
        }

        /// recover the statistics:
        for (int i = aOrigLabels.size() - 1; i >= 0; --i) {
            Integer origLabel = aOrigLabels.get(i);
            MinimalParticle vP = aAppliedParticles.elementAt(i);
            // UpdateStatisticsWhenJump(vP.iIndex, iIntensityImage.get(vP.iIndex), vP.iCandidateLabel, origLabel);
            updateLabelStatistics(iIntensityImage.get(vP.iIndex), vP.iCandidateLabel, origLabel);
        }
    }

    private void MCMCInsertFloatingParticleCumulative(MinimalParticle aParticle, boolean aDoRecord) {

        int vIndex = iFloatingParticles.find(aParticle);

        // if(m_MCMCFloatingParticles.size() != vIndex ){
        // if(m_MCMCFloatingParticles[vIndex].m_CandidateLabel != aParticle.m_CandidateLabel) {
        // std::cout << "ERROR!!! floating particle found but has different candlabel" << std::endl;
        // }
        // }

        MinimalParticle vParticleInserted;
        if (iFloatingParticles.size() == vIndex) {
            // the particle did not yet exist
            vParticleInserted = new MinimalParticle(aParticle);
        }
        else {
            // The element did already exist. We add up the proposal and insert
            // the element again (in order to overwrite).
            vParticleInserted = new MinimalParticle(iFloatingParticles.elementAt(vIndex));
            vParticleInserted.iProposal += aParticle.iProposal;
        }

        iFloatingParticles.insert(vParticleInserted);

        if (aDoRecord) {
            ParticleHistoryElement vPHE = new ParticleHistoryElement(aParticle, 0, true);
            iFloatingParticlesHistory.add(vPHE);
        }

        iFloatingParticlesProposalNormalizer += aParticle.iProposal;
        //System.out.println("m_FloatingParticlesProposalNormalizer1: " + m_FloatingParticlesProposalNormalizer);

    }

    private void MCMCUpdateRegularParticleMapInNeighborhood(int aIndex) {

        for (Point offset : iBgNeighborsOffsets) {

            int vIndex = aIndex + iLabelImage.pointToIndex(offset);
            if (iLabelImage.isBorderLabel(iLabelImage.getLabel(vIndex))) continue;

            if (iLabelImage.isBoundaryPoint(vIndex)) {
                if (0 == m_MCMCRegularParticlesMap.getLabel(vIndex)) {
                    m_MCMCRegularParticlesMap.setLabel(vIndex, 1 /* true */);
                }
            }
            else {
                if (1 == m_MCMCRegularParticlesMap.getLabel(vIndex)) {
                    m_MCMCRegularParticlesMap.setLabel(vIndex, 0 /* false */);
                }
            }
        }
    }

    private void MCMCStoreResults(int aCandidateIndex, int aLabelBefore, int aIteration) {
        /// Instead of storing the old or the new accepted state we just store
        /// the difference (nothing is stored if we do not accept):

        // create the result entry
        McmcResult vResult = new McmcResult(aIteration, aLabelBefore);

        // check if results are already stored at this location:
        // typename MCMCResultsType::iterator vR =
        List<McmcResult> vR = iMcmcResults.get(aCandidateIndex);
        if (vR == null) {
            /// Create a new entry
            ArrayList<McmcResult> vList = new ArrayList<>();
            // vList.push_back(vResult);
            vList.add(vResult);
            iMcmcResults.put(aCandidateIndex, vList);
        }
        else {
            vR.add(vResult);
        }
    }

    private float CalculateEnergyDifference(int aIndex, int aCurrentLabel, int aToLabel, float aImgValue) {
        ContourParticle contourCandidate = new ContourParticle(aCurrentLabel, aImgValue);
        EnergyResult res = iImageModel.calculateDeltaEnergy(iLabelImage.indexToPoint(aIndex), contourCandidate, aToLabel, iLabelStatistics);

        return res.energyDifference.floatValue();
    }

    private void MCMCApplyParticle(MinimalParticle aCandidateParticle, boolean aDoSimulate) {
        /// Maintain the regular particle container and the label image:
        MCMCAddAndRemoveParticlesWhenMove(aCandidateParticle);

        /// Update the label image. The new point is either a contour particle or 0,
        /// therefore the negative label value is set.
        int vFromLabel = Math.abs(iLabelImage.getLabel(aCandidateParticle.iIndex));

        int vSign = -1; /// standard sign of the label image of boundary particles
        if (iLabelImage.isEnclosedByLabelBgConnectivity(aCandidateParticle.iIndex, aCandidateParticle.iCandidateLabel)) {
            /// we created a floating particle.
            vSign = 1;
        }

        storeLabelImageHistory(aCandidateParticle.iIndex, iLabelImage.getLabel(aCandidateParticle.iIndex));
        iLabelImage.setLabel(aCandidateParticle.iIndex, vSign * aCandidateParticle.iCandidateLabel);

        ///
        /// Update the statistics of the propagating and the loser region.
        if (!aDoSimulate) { // use updateLabelStatistics(...)?
            // TODO: Not sure if correct - test it!
            // UpdateStatisticsWhenJump(aCandidateParticle.iIndex,
            // iIntensityImage.get(aCandidateParticle.iIndex),
            // vFromLabel,
            // aCandidateParticle.iCandidateLabel);
            updateLabelStatistics(iIntensityImage.get(aCandidateParticle.iIndex), vFromLabel, aCandidateParticle.iCandidateLabel);
        }

        /// Update the proposals for all particles in the neighborhood (as they
        /// might have changed).
        if (iSettings.useBiasedProposal || (!iSettings.allowFission || !iSettings.allowHandles)) {
            updateProposalsAndFilterTopologyInNeighborhood(aCandidateParticle);
        }
    }

    /// Updates the DS (label image and the regular particle containers) when
    /// applying a particle. Note that floating particles will not be updated!
    /// The method only ensures that L and the regular particles are correct.
    /// The method expects the label image NOT to be updated already. Else the
    /// operations performed will be wrong.
    private void MCMCAddAndRemoveParticlesWhenMove(MinimalParticle aCandidateParticle) {
        // TODO: Could be split into the following methods:
        // MCMCRemoveOrphinsAfterMove();
        // MCMCAddNewParentsAfterMove();
        // MCMCRemoveInteriorPointsAfterMove();
        // MCMCAddNewCandidatesAfterMove();

        int absLabelFrom = iLabelImage.getLabelAbs(aCandidateParticle.iIndex);
        int absLabelTo = aCandidateParticle.iCandidateLabel;
        int candidateIndex = aCandidateParticle.iIndex;

        // We remove the particle and insert the reverse particle: we cannot decide if the reverse particle is indeed.
        // The solution is that this method only works on the regular particle set. Floating particles will be detected and treated outside of
        // this method. Here we omit the potential insertion of floating particles.
        // In order to (maybe) replace the particle with its reverse particle we: Simulate the move, calculate the proposal for the backward particle,
        // create the backward particle, check if the backward particle is floating, and finally restore the label image:
        int savedOrigLabel = iLabelImage.getLabel(candidateIndex);
        iLabelImage.setLabel(candidateIndex, -absLabelTo);
        MinimalParticle reverseParticle = new MinimalParticle(candidateIndex, absLabelFrom, calculateProposal(candidateIndex));
        boolean reverseParticleIsFloating = MCMCParticleHasFloatingProperty(reverseParticle.iIndex, reverseParticle.iCandidateLabel);
        iLabelImage.setLabel(candidateIndex, savedOrigLabel);
        if (!reverseParticleIsFloating) {
            insertCandidatesToContainers(reverseParticle, absLabelTo, true);
        }

        // erase the currently applied particle (if its floating this will not hurt to call erase here).
        eraseCandidatesFromContainers(new MinimalParticle(candidateIndex, absLabelTo, 0), absLabelFrom, true);
        
        // Since we are storing the parents separately for each region, the following patch is needed. 
        // We need to shift the mother particle into the parents container of each others region.
        if (absLabelFrom != 0 && absLabelTo != 0) {
            MinimalParticle p = new MinimalParticle(candidateIndex, 0, calculateProposal(candidateIndex));
            eraseCandidatesFromContainers(p, absLabelFrom, true);
            insertCandidatesToContainers(p, absLabelTo, true);
        }

        // What particle would be added or removed to the contour lists of the currently shrinking region.
        // FG region is shrinking:
        if (absLabelFrom != 0) { 
            for (Point offset : iBgNeighborsOffsets) {
                int index = candidateIndex + iLabelImage.pointToIndex(offset);
                int label = iLabelImage.getLabel(index);
                // Check if new points enter the contour (internal point becomes a parent):
                // Internal points of this region are positive:
                if (label > 0 && Math.abs(label) == absLabelFrom) {
                    boolean notExisted = insertCandidatesToContainers(new MinimalParticle(index, 0, calculateProposal(index)), absLabelFrom, true);
                    if (notExisted) {
                        iLabelImage.setLabel(index, -absLabelFrom);
                        storeLabelImageHistory(index, label);
                    }
                }
            }

            for (Point offset : iFgNeighborsOffsets) {
                int index = candidateIndex + iLabelImage.pointToIndex(offset);
                int label = iLabelImage.getLabel(index);
                if (iLabelImage.isBorderLabel(label)) continue;

                /// check if there are FG-neighbors with no other mother of the same label --> orphan.
                /// we first 'simulate' the move:
                int savedLabel = iLabelImage.getLabel(candidateIndex);
                iLabelImage.setLabel(candidateIndex, -absLabelTo);

                if (Math.abs(label) != absLabelFrom) {
                    // check if this neighbor has other mothers from this label:
                    boolean hasOtherMother = false;
                    for (Point vOff2 : iFgNeighborsOffsets) {
                        int vL2 = iLabelImage.getLabelAbs(candidateIndex + iLabelImage.pointToIndex(vOff2.add(offset)));
                        if (vL2 == absLabelFrom) {
                            hasOtherMother = true;
                            break;
                        }
                    }
                    if (!hasOtherMother) {
                        // The orphin has label equal to what we read from the label image and has a candidate label of the currently shrinking region.
                        // The proposal is not important; we delete the value
                        eraseCandidatesFromContainers(new MinimalParticle(index, absLabelFrom, 0), Math.abs(label), true);
                    }
                }
                iLabelImage.setLabel(candidateIndex, savedLabel);
            }
        }

        /// Growing: figure out the changes of candidates for the expanding region
        if (absLabelTo != 0) { // we are growing
            /// Neighbors: Figure out what (neighboring)mother points are going to be interior points:
            /// simulate the move
            int vStoreLabel1 = iLabelImage.getLabel(candidateIndex);
            iLabelImage.setLabel(candidateIndex, -absLabelTo);

            for (Point vOff : iBgNeighborsOffsets) {
                int index = candidateIndex + iLabelImage.pointToIndex(vOff);
                int vL = iLabelImage.getLabel(index);
                // TODO: the isEnclosedByLabel method could use the above iterator
                if (vL == -absLabelTo && iLabelImage.isEnclosedByLabelBgConnectivity(index, absLabelTo)) {
                    /// Remove the parent that got enclosed; it had a the label
                    /// of the currently expanding region and a candidate label of 0.
                    eraseCandidatesFromContainers(new MinimalParticle(index, 0, 0), absLabelTo, true);
                    
                    /// update the label image (we're not using the update mechanism of the optimizer anymore):
                    iLabelImage.setLabel(index, Math.abs(vL));

                    storeLabelImageHistory(index, vL);
                }
            }
            iLabelImage.setLabel(candidateIndex, vStoreLabel1);

            /// Figure out if a point renders to a candidate. These are all the FG-neighbors with a different label that are not yet
            /// candidates of the currently expanding region.
            for (Point vOff : iFgNeighborsOffsets) {
                int index = candidateIndex + iLabelImage.pointToIndex(vOff);
                int vL = iLabelImage.getLabel(index);
                if (Math.abs(vL) != absLabelTo && !iLabelImage.isBorderLabel(Math.abs(vL))) {
                    // check if there is no other mother (hence the particle is not in the container yet). This we could do by checking the
                    // neighborhood of the label image or by checking the cointainers.
                    // Here: we check the (not yet updated!) label image.
                    boolean vHasOtherMother = false;
                    for (Point vOff2 : iFgNeighborsOffsets) {
                        int vL2 = iLabelImage.getLabel(candidateIndex + iLabelImage.pointToIndex(vOff2.add(vOff)));
                        if (Math.abs(vL2) == absLabelTo) {
                            vHasOtherMother = true;
                            break;
                        }
                    }
                    if (!vHasOtherMother) {
                        // This is a new child. It's current label we have to read from the label image, the candidate label is the label of the currently expanding region.
                        boolean vNotExisted = insertCandidatesToContainers(new MinimalParticle(index, absLabelTo, calculateProposal(index)), Math.abs(vL), true);
                        if (vNotExisted) {
                            iLabelImage.setLabel(index, -Math.abs(vL));
                            storeLabelImageHistory(index, vL);
                        }
                    }
                }
            }
        }
    }

    private boolean MCMCParticleHasFloatingProperty(int aIndex, int aCandLabel) {
        if (aCandLabel > 0) {
            /// This is a daughter. It is floating if there is no mother, i.e. if
            /// there is no corresponding region in the FG neighborhood.
            return iLabelImage.isSingleFgPoint(aIndex, aCandLabel);
        }
        /// else: this is a potential mother (candidate label is 0). According
        /// to the BG connectivity it fulfills the floating property
        /// only if there is no other region in the BG neighborhood. Otherwise
        /// this pixel might well go to the BG label without changing the topo.
        return iLabelImage.isEnclosedByLabelBgConnectivity(aIndex, Math.abs(iLabelImage.getLabel(aIndex)));
    }

    private float MCMCGetProposalNormalizer(int aCurrentLabel, int aCandidateLabel) {
        if (aCandidateLabel == 0) {
            return iParentsProposalNormalizer.get(aCurrentLabel);
        }
        return iChildrenProposalNormalizer.get(aCandidateLabel);
    }

    private boolean MCMCIsRegularParticle(MinimalParticle aParticle, int aCurrentLabel) {
        if (aParticle.iCandidateLabel == LabelImage.BGLabel) {
            return iParents.get(aCurrentLabel).contains(aParticle);
        }

        return iChildren.get(aParticle.iCandidateLabel).contains(aParticle);
    }

    /// Set up a vector MCMCRegionLabel that maps natural numbers (index of
    /// the vector to the region labels). We iterate the count statistics keys
    /// to get the region labels.
    private void MCMCUpdateRegionLabelVector() {

        m_MCMCRegionLabel.clear();

        // typename MinimalParticleIndexedSetMapType::iterator vActiveLabelsIt = m_MCMCparents.begin();
        // typename MinimalParticleIndexedSetMapType::iterator vActiveLabelsItEnd = m_MCMCparents.end();
        for (Entry<Integer, MinimalParticleIndexedSet> e : iParents.entrySet()) {
            m_MCMCRegionLabel.add(e.getKey());
        }
    }

    private void PrepareEnergyCalculationForEachIteration() {
        // Same as in RC algorithm
        if (iSettings.usingDeconvolutionPcEnergy) {
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
    }

    private void prepareEnergies() {
        // TODO: it is a'la initEnergies from RC
        if (iSettings.usingDeconvolutionPcEnergy) {
            // Deconvolution: - Alocate and initialize the 'ideal image'
            // TODO: This is not OOP, handling energies should be redesigned
            ((E_Deconvolution) iImageModel.getEdata()).GenerateModelImage(iLabelImage, iLabelStatistics);
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
    }

    /**
     * Initializes statistics. For each found label creates LabelStatistics object and stores it in labelStatistics container. TODO: Same as in AlgorithmRC - reuse somehow.
     */
    private void initStatistics() {
        // First create all LabelStatistics for each found label and calculate values needed for later variance/mean calculations
        int maxUsedLabel = 0;
        for (int i = 0; i < iLabelImage.getSize(); ++i) {
            final int absLabel = iLabelImage.getLabelAbs(i);

            if (!iLabelImage.isBorderLabel(absLabel)) {
                if (maxUsedLabel < absLabel) maxUsedLabel = absLabel;

                LabelStatistics stats = iLabelStatistics.get(absLabel);
                if (stats == null) {
                    stats = new LabelStatistics(absLabel, iLabelImage.getNumOfDimensions());
                    iLabelStatistics.put(absLabel, stats);
                }
                final double val = iIntensityImage.get(i);
                stats.iSum += val;
                stats.iSumOfSq += val*val;
                stats.iLabelCount++;
            }
        }

        // If background label do not exist add it to collection
        LabelStatistics stats = iLabelStatistics.get(BGLabel);
        if (stats == null) {
            stats = new LabelStatistics(BGLabel, iLabelImage.getNumOfDimensions());
            iLabelStatistics.put(BGLabel, stats);
        }

        // Finally - calculate variance, median and mean for each found label
        for (LabelStatistics stat : iLabelStatistics.values()) {
            int n = stat.iLabelCount;
            stat.iMeanIntensity = n > 0 ? (stat.iSum / n) : 0;
            stat.iVarIntensity = CalculateVariance(stats.iSumOfSq,  stat.iMeanIntensity, n);
            // Median on start set equal to mean
            stat.iMedianIntensity = stat.iMeanIntensity;
        }
        
        // Make sure that labelDispenser will not produce again any already used label
        // Safe to search with 'max' we have at least one value in container (background)
//        labelDispenser.setMaxValueOfUsedLabel(maxUsedLabel);
    }

    private double CalculateVariance(double aSumSq, double aMean, int aN) {
        if (aN < 2) return 0; //TODO: what would be appropriate?
        return (aSumSq - aN * aMean * aMean)/(aN - 1.0);
    }
    
    // TODO: Same as in AlgorithmRC - reuse somehow. (updated tu use sumOfSq and sum).
    private void updateLabelStatistics(double aIntensity, int aFromLabelIdx, int aToLabelIdx) {
        final LabelStatistics toStats = iLabelStatistics.get(aToLabelIdx);
        final LabelStatistics fromStats = iLabelStatistics.get(aFromLabelIdx);

        toStats.iSumOfSq += aIntensity*aIntensity;
        fromStats.iSumOfSq -= aIntensity*aIntensity;
        toStats.iSum += aIntensity;
        fromStats.iSum -= aIntensity;
        
        toStats.iLabelCount++;
        fromStats.iLabelCount--;
        
        // Update mean/var from updatet sums and label count
        toStats.iMeanIntensity = (toStats.iSum ) / (toStats.iLabelCount);
        fromStats.iMeanIntensity = (fromStats.iLabelCount > 0) ? (fromStats.iSum ) / (fromStats.iLabelCount) : 0;
        toStats.iVarIntensity = CalculateVariance(toStats.iSumOfSq, toStats.iMeanIntensity, toStats.iLabelCount);
        fromStats.iVarIntensity = CalculateVariance(fromStats.iSumOfSq, fromStats.iMeanIntensity, fromStats.iLabelCount);
    }

    public boolean performIteration() {
        iIterationCounter++;
        iAcceptedMoves += MCMCDoIteration() ? 1 : 0;
        if (iIterationCounter == iSettings.maxNumOfIterations) {
            System.out.println("Overall acceptance rate: " + ((float) iAcceptedMoves / iIterationCounter));
        }
        
        // never done earlier than wanted number of iterations
        return false;
    }

    public int getBiggestLabel() {
        return 10;
    }
    
    // ================================= CLEANED UP ===================================================
    
    private class McmcResult {
        public int iteration;
        public int previousLabel;

        public McmcResult(int aIteration, int aPreviousLabel) { iteration = aIteration; previousLabel = aPreviousLabel; }

        @Override
        public String toString() {
            return "Result{" + iteration + ", " + previousLabel + "}";
        }
    }
    private Map<Integer, List<McmcResult>> iMcmcResults = new HashMap<>(); // <point index -> result>
    
    
    private class LabelImageHistoryEvent {

        public LabelImageHistoryEvent(int aIndex, int aLabel) { index = aIndex; label = aLabel; }

        public int index;
        public int label;
    }
    private List<LabelImageHistoryEvent> iLabelImageHistory = new ArrayList<>();
    
    // ------------------------- methods -------------------------------------------------------------
    
    /**
     * Store an event (a change) on the label image. If the Metropolis algorithm refuses the current move, the m_MCMCLabelImageHistory will help to
     * undo the changes on the label image and hence help to undo the move.
     * @param aIndex
     * @param aOrigLabel
     */
    private void storeLabelImageHistory(int aIndex, int aOrigLabel) {
        iLabelImageHistory.add(new LabelImageHistoryEvent(aIndex, aOrigLabel));
    }
    
    /**
     * Check if particle and its neighbors are topologicly valid and update its proposal or removes if not.
     * @param aParticle
     */
    private void updateProposalsAndFilterTopologyInNeighborhood(MinimalParticle aParticle) {
        for (int i = -1; i < iBgNeighborsOffsets.length; ++i) {
            // for -1 use 0 offset (position of particle itself)
            Point offset = (i >= 0) ? iBgNeighborsOffsets[i] : new Point(iBgNeighborsOffsets[0]).zero();

            MinimalParticleIndexedSet particleSet = new MinimalParticleIndexedSet();
            getRegularParticles(aParticle.iIndex + iLabelImage.pointToIndex(offset), particleSet);

            for (MinimalParticle particle : particleSet) {
                int label = (particle.iCandidateLabel == 0) ? iLabelImage.getLabelAbs(particle.iIndex) : particle.iCandidateLabel;

                if (isParticleTopoValid(particle)) {
                    /// replace the particle with the new proposal
                    MinimalParticle updatedParticle = new MinimalParticle(particle.iIndex, particle.iCandidateLabel, calculateProposal(particle.iIndex));
                    insertCandidatesToContainers(updatedParticle, label, true);
                }
                else {
                    /// remove the particle
                    eraseCandidatesFromContainers(particle, label, true);
                }
            }
        }
    }
    
    /**
     *  Removes all non-simple points from aParticleSet
     * @param aParticleSet
     */
    private void topologyFiltering(MinimalParticleIndexedSet aParticleSet) {
        // Intentionally from back since then indexes are not invalidated in particle set
        for (int i = aParticleSet.size() - 1; i >= 0; --i) {
            MinimalParticle p = aParticleSet.elementAt(i);
            if (!isParticleTopoValid(p)) {
                aParticleSet.erase(p);
            }
        }
    }
    
    /**
     * @param aParticle
     * @return false if topology is changed when applying aParticle. This is based on the current state of the label image.
     */
    private boolean isParticleTopoValid(MinimalParticle aParticle) {
        if (!iSettings.allowFission || !iSettings.allowHandles) {
            
            // calculate the topo numbers for the current and the candidate label if they are != 0
            ArrayList<Integer> labelsToCheck = new ArrayList<>(2);
            if (aParticle.iCandidateLabel != 0) labelsToCheck.add(aParticle.iCandidateLabel);
            int particleLabel = iLabelImage.getLabelAbs(aParticle.iIndex);
            if (particleLabel != 0) labelsToCheck.add(particleLabel);
            
            // check if numbers are OK
            for (TopologicalNumberResult tnr : iTopoFunction.getTopologicalNumbers(iLabelImage.indexToPoint(aParticle.iIndex), labelsToCheck)) {
                if (tnr == null || !(tnr.iNumOfConnectedComponentsFG == 1 && tnr.iNumOfConnectedComponentsBG == 1)) {
                    return false;
                }
            }
        }

        return true;
    }
    
    /**
     * Gets regular particles into aList for BG neighborhood of aIndex
     * @param aIndex
     * @param aList
     */
    private void getRegularParticlesInBgNeighborhood(int aIndex, MinimalParticleIndexedSet aList) {
        for (Point offset : iBgNeighborsOffsets) {
            getRegularParticles(aIndex + iLabelImage.pointToIndex(offset), aList);
        }
    }
    
    /**
     * Gets regular particles into aList for FG neighborhood of aIndex
     * @param aIndex
     * @param aList
     */
    private void getRegularParticlesInFgNeighborhood(int aIndex, MinimalParticleIndexedSet aList) {
        for (Point offset : iFgNeighborsOffsets) {
            getRegularParticles(aIndex + iLabelImage.pointToIndex(offset), aList);
        }
    }
    
    /**
     * Inserts to a list all possible candidates from BG neighbors
     * @param aParticle
     * @param aSet
     */
    private void getPartnerParticles(MinimalParticle aParticle, MinimalParticleIndexedSet aSet) {
        // Get all correct particles in BG neighborhood
        MinimalParticleIndexedSet conditionalParticles = new MinimalParticleIndexedSet();
        getRegularParticlesInBgNeighborhood(aParticle.iIndex, conditionalParticles);
        for (MinimalParticle p : conditionalParticles) {
            if (p.iCandidateLabel != aParticle.iCandidateLabel) {
                aSet.insert(p);
            }
        }

        // filter the points to meet topological constraints
        if (!iSettings.allowFission || !iSettings.allowHandles) {
            topologyFiltering(aSet);
        }

        // Insert particle A in the set for B such that it is possible that we have a single particle move.
        aSet.insert(aParticle);
    }
    
    /**
     * Inserts to a list possible candidates for particle at aIndex
     * @param aIndex
     * @param aList
     */
    private void getRegularParticles(int aIndex, MinimalParticleIndexedSet aList) {
        int currentLabel = iLabelImage.getLabel(aIndex);
        if (iLabelImage.isBorderLabel(currentLabel)) return;
        
        final float proposal = calculateProposal(aIndex);
        int absCurrentLabel = iLabelImage.labelToAbs(currentLabel);
        
        boolean isParentInserted = false;
        
        for (Integer idx : iLabelImage.iterateNeighbours(aIndex)) {
            int neighborLabel = iLabelImage.getLabel(idx);
            int absNeighborLabel = iLabelImage.labelToAbs(neighborLabel);
            
            if (!iLabelImage.isBorderLabel(neighborLabel) && absNeighborLabel != absCurrentLabel) {
                // here should be a particle since two neighboring pixel have a different label.

                if (neighborLabel != LabelImage.BGLabel) {
                    // Insert FG labels have a children placed at this spot.
                    aList.insert(new MinimalParticle(aIndex, absNeighborLabel, proposal));
                }
                if (!isParentInserted && currentLabel != LabelImage.BGLabel) {
                    // this is a non-background pixel with different neighbors, hence there must be a parent in the list
                    aList.insert(new MinimalParticle(aIndex, 0, proposal));
                    isParentInserted = true;
                }
            }
        }

        /// Check the BG neighborhood now if we need to insert a parent.
        if (!isParentInserted && currentLabel != LabelImage.BGLabel) {
            for (Integer idx : iLabelImage.iterateBgNeighbours(aIndex)) {
                if (iLabelImage.getLabelAbs(idx) != absCurrentLabel) {
                    // This is a FG pixel with a neighbor of a different label. Finally, insert a parent particle.
                    aList.insert(new MinimalParticle(aIndex, 0, proposal));
                    break;
                }
            }
        }
    }
    
    /**
     * @return index of non-boundary point sampled from edge density
     */
    private int sampleIndexFromEdgeDensity() {
        // Sample non-boundary point
        int index = -1;
        do {
            index = m_MCMCEdgeImageDistr.sample();
        } while (iLabelImage.isBorderLabel(iLabelImage.getLabel(index)));
        
        return index;
    }
    
    /**
     * Inserts candidate particle into container
     * @param aParticle - candidate particle
     * @param aLabel - label for particle
     * @param aDoRecord - should record
     * @return true if new particle was added, false if replaced
     */
    private boolean insertCandidatesToContainers(MinimalParticle aParticle, int aLabel, boolean aDoRecord) {
        MinimalParticle replacedParticle = null;

        if (aParticle.iCandidateLabel == 0) {
            MinimalParticleIndexedSet particles = iParents.get(aLabel);
            if (particles == null) {
                particles = new MinimalParticleIndexedSet();
                iParents.put(aLabel, particles);
            }
            replacedParticle = particles.insert(aParticle);

            float diff = (replacedParticle == null) ? aParticle.iProposal : aParticle.iProposal - replacedParticle.iProposal;
            iParentsProposalNormalizer.replace(aLabel, iParentsProposalNormalizer.get(aLabel) + diff);
            iTotalNormalizer += diff;
        }
        else {
            MinimalParticleIndexedSet particles = iChildren.get(aParticle.iCandidateLabel);
            if (particles == null) {
                particles = new MinimalParticleIndexedSet();
                iChildren.put(aParticle.iCandidateLabel, particles);
            }
            replacedParticle = particles.insert(aParticle);
            
            float diff = (replacedParticle == null) ? aParticle.iProposal : aParticle.iProposal - replacedParticle.iProposal;
            iChildrenProposalNormalizer.replace(aParticle.iCandidateLabel, iChildrenProposalNormalizer.getOrDefault(aParticle.iCandidateLabel, 0f) + diff);
            iTotalNormalizer += diff;
        }

        if (aDoRecord) {
            // Note that the order here is important: first insert the particle that gets replaced. 
            // When restoring the state afterwards, the particle history is iterated in reverse order.
            if (replacedParticle != null) {
                iParticlesHistory.add(new ParticleHistoryElement(replacedParticle, aLabel, false));
            }
            iParticlesHistory.add(new ParticleHistoryElement(aParticle, aLabel, true));
        }

        return (replacedParticle == null);
    }
    
    /**
     * Removes candidate particle from containers for given label 
     * @param aParticle - candidate particle
     * @param aLabel - label of particle
     * @param aDoRecord - should keep history
     * @return true if particle existed
     */
    private boolean eraseCandidatesFromContainers(MinimalParticle aParticle, int aLabel, boolean aDoRecord) {
        MinimalParticle replacedParticle = null;

        if (aParticle.iCandidateLabel == 0) {
            MinimalParticleIndexedSet particles = iParents.get(aLabel);
            replacedParticle = particles.erase(aParticle);
            if (replacedParticle != null) {
                iParentsProposalNormalizer.put(aLabel, iParentsProposalNormalizer.get(aLabel) - replacedParticle.iProposal);
                iTotalNormalizer -= replacedParticle.iProposal;
            }
        }
        else {
            MinimalParticleIndexedSet particles = iChildren.get(aParticle.iCandidateLabel);
            replacedParticle = particles.erase(aParticle);
            if (replacedParticle != null) {
                iChildrenProposalNormalizer.put(aParticle.iCandidateLabel, iChildrenProposalNormalizer.get(aParticle.iCandidateLabel) - replacedParticle.iProposal);
                iTotalNormalizer -= replacedParticle.iProposal;
            }
        }
        
        if (replacedParticle != null && aDoRecord) {
            iParticlesHistory.add(new ParticleHistoryElement(replacedParticle, aLabel, false));
        }

        return replacedParticle != null;
    }
    
    /**
     * Helper function for creating indexed discrete distributions (starting indices from 0)
     * @param aList - list of values/probabilities
     * @return discrete distribution
     */
    private EnumeratedDistribution<Integer> createDiscreteDistrFromValues(ArrayList<Float> aList) {
        ArrayList<Pair<Integer, Double>> pmf = new ArrayList<>(aList.size());
        for (int index = 0; index < aList.size(); ++index) {
            pmf.add(new Pair<>(index, (double) aList.get(index)));
        }

        return new EnumeratedDistribution<>(m_NumberGeneratorBoost, pmf);
    }
    
    /**
     * Insert a floating particle only if it didn't exist. 
     * @param aParticle
     * @param aDoRecord - save info in history container
     * @return If particle existed, the return value will be false.
     */
    private boolean insertFloatingParticle(MinimalParticle aParticle, boolean aDoRecord) {
        if (iFloatingParticles.contains(aParticle)) {
            // the particle already exists
            return false;
        }

        iFloatingParticles.insert(aParticle);
        iFloatingParticlesProposalNormalizer += aParticle.iProposal;

        if (aDoRecord) {
            iFloatingParticlesHistory.add(new ParticleHistoryElement(aParticle, 0, true));
        }

        return true;
    }

    /**
     * Removes floating particle if exist. In case when proposals diff much then particle is only updated.
     * @param aParticle
     * @param aDoRecord
     */
    private void eraseFloatingParticle(MinimalParticle aParticle, boolean aDoRecord) {
        int index = iFloatingParticles.find(aParticle);
        if (iFloatingParticles.size() == index) {
            // particle does not exist
            return;
        }
        
        MinimalParticle updatedParticle = iFloatingParticles.elementAt(index);
        if (updatedParticle.iProposal - aParticle.iProposal > Math.ulp(1.0f) * 10) {
            /// the particle has still some proposal left so only update particle in a container without removing it
            iFloatingParticlesProposalNormalizer -= aParticle.iProposal;
            updatedParticle.iProposal -= aParticle.iProposal;
        }
        else {
            // the particle gets deleted. We only remove the amount from the normalizer that has been stored in the set.
            iFloatingParticlesProposalNormalizer -= updatedParticle.iProposal;
            iFloatingParticles.erase(updatedParticle);
        }
        
        if (aDoRecord) {
            iFloatingParticlesHistory.add(new ParticleHistoryElement(aParticle, 0, false));
        }
    }
    
    /**
     * Sample off-boundary particle and inserts/removes it from floating particles 
     * depending on if should growth or not.
     * @param aGrowth - true for growth, false otherwise
     * @param aLabel - wanted label for particle
     * @return false for rejected sample, true otherwise
     */
    private boolean sampleOffBoundary(boolean aGrowth, int aLabel) {
        /// sample candidate label and location
        int candidateLabel = (iNumberGenerator.GetUniformVariate(0, 1) > 0.5) ? aLabel : 0;
        int particleIndex = sampleIndexFromEdgeDensity();
        MinimalParticle particle = new MinimalParticle(particleIndex, candidateLabel, 0);
        
        boolean particleExists = (iFloatingParticles.find(particle) != iFloatingParticles.size());

        if (!aGrowth && particleExists) {
            eraseFloatingParticle(particle, false);
        }
        else if (aGrowth && !particleExists) {
            particle.iProposal = calculateProposal(particle.iIndex);
            insertFloatingParticle(particle, false);
        }
        else {
            // reject
            return false;
        }

        return true;
    }
    
    /**
     * @param aIndex
     * @return proposal for particle at aIndex
     */
    private float calculateProposal(int aIndex) {
        if (!iSettings.useBiasedProposal) return 1.0f;

        // Length-prior driven proposal
        float length = 0;
        boolean isFloatingParticle = true;
        int label = iLabelImage.getLabelAbs(aIndex);
        Point point = iLabelImage.indexToPoint(aIndex);
        for (int i = 0; i < iBgNeighborsOffsets.length; ++i) {
            Point offset = iBgNeighborsOffsets[i];
            int currLabel = iLabelImage.getLabelAbs(offset.add(point));
            if (currLabel != label) {
                length += m_MCMClengthProposalMask[i];
                isFloatingParticle = false;
            }
        }
        if (isFloatingParticle) {
            // floating particles need a proposal > 0: We take half of the smallest element in the mask
            // (we assume the smallest element to be at position 0).
            length = m_MCMClengthProposalMask[0] / 2.0f;
        }
        
        return length;
    }
    
    /**
     * Creates output probability image with slice for every label.
     * @return 
     */
    public SegmentationProcessWindow createProbabilityImage() {
        // Create output stack image
        //System.out.println("m_MCMCResults:\n" + iMcmcResults);
        int[] dims = iLabelImage.getDimensions();
        SegmentationProcessWindow resultImg = new SegmentationProcessWindow(dims[0], dims[1], true);

        int numOfBurnInIterations = (int) (iSettings.burnInFactor * iIterationCounter);
        int numOfCountableIterations = iIterationCounter - numOfBurnInIterations;
        
        for (int currentLabel : m_MCMCRegionLabel) {
            if (currentLabel == LabelImage.BGLabel) continue;

            // Create initial image for current label
            IntensityImage img = new IntensityImage(iLabelImage.getDimensions());
            for (int i = 0; i < iLabelImage.getSize(); ++i) {
                img.set(i, iLabelImage.getLabelAbs(i) == currentLabel ? 1f : 0f);
            }
            
            for (Entry<Integer, List<McmcResult>> e : iMcmcResults.entrySet()) {
                int index = e.getKey();
                List<McmcResult> results = e.getValue();

                int labelAtIndex = iLabelImage.getLabelAbs(index);

                // Calculate for how many iterations currentLabel was a result during countable iterations.
                long iterationsInLabel = (labelAtIndex == currentLabel) ? numOfCountableIterations : 0;
                for (int i = results.size() - 1; i >= 0; --i) {
                    McmcResult result = results.get(i);
                    
                    // Check if we are in the burn-in phase
                    if (result.iteration < numOfBurnInIterations) break;

                    if (currentLabel == labelAtIndex) {
                        iterationsInLabel -= (result.iteration - numOfBurnInIterations);
                    }
                    else if (currentLabel == result.previousLabel) {
                        iterationsInLabel += (result.iteration - numOfBurnInIterations);
                    }
                    labelAtIndex = result.previousLabel;
                }

                img.set(index, (float) ( (double)iterationsInLabel / numOfCountableIterations));
            }

            resultImg.addSliceToStack(img, "label_" + currentLabel);
        }
        
        resultImg.setImageTitle("Probability");
        
        return resultImg;
    }    
    
    /**
     * Generates distribution from input image.ds 
     * NOTICE: in case if values in input image are too small (< 10*eps(1)) it will provide flat distribution and will change all pixels of input image to 1.0
     * 
     * @param aImg input image to generate distribution from
     * @return generated distribution
     */
    static EnumeratedDistribution<Integer> generateDiscreteDistribution(IntensityImage aImg, Rng am_NumberGeneratorBoost) {
        ArrayList<Pair<Integer, Double>> pmf = new ArrayList<>(aImg.getSize());
        
        Iterator<Point> ri = new SpaceIterator(aImg.getDimensions()).getPointIterator();
        int index = 0;
        double sumOfPixelValues = 0;
        while (ri.hasNext()) {
            final Point point = ri.next();
            double value = aImg.get(point);
            sumOfPixelValues += value;
            pmf.add(new Pair<Integer, Double>(index++, value));
        }
        // if sum of all pixels is too small (< 10 * epislon(1f) as in original code) then just use flat distribution
        if (sumOfPixelValues < 10 * Math.ulp(1.0f)) {
            sumOfPixelValues = aImg.getSize();
            index = 0;
            ri = new SpaceIterator(aImg.getDimensions()).getPointIterator();
            while (ri.hasNext()) {
                final Point point = ri.next();
                aImg.set(point, 1.0f);
                pmf.set(index, new Pair<Integer, Double>(index, 1.0));
                ++index;
            }
        }

        return new EnumeratedDistribution<>(am_NumberGeneratorBoost, pmf);
    }
}
