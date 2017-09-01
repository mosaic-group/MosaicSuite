package mosaic.region_competition.DRS;


import static mosaic.core.imageUtils.images.LabelImage.BGLabel;

import java.util.ArrayList;
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

    public AlgorithmDRS(IntensityImage aIntensityImage, LabelImage aLabelImage, IntensityImage aEdgeImage, ImageModel aModel, SettingsDRS aSettings) {
        logger.debug("DRS algorithm created with settings:" + Debug.getJsonString(aSettings));

        // Save input parameters
        iLabelImage = aLabelImage;
        iIntensityImage = aIntensityImage;
        iEdgeImage = aEdgeImage;
        iImageModel = aModel;
        iSettings = aSettings;

        // Initialize label image
        iLabelImage.initBorder();

        initConnectivities();
        initEdgeDistribution();
        initLenghtProposal();
        initLabels();
        initStatistics();
        initEnergies();
    }

    public boolean performIteration() {
        ++iIterationCounter;
        iAcceptedMoves += runOneIteration() ? 1 : 0;
        if (iIterationCounter == iSettings.maxNumOfIterations) {
            logger.info("Overall acceptance rate: " + ((float) iAcceptedMoves / iIterationCounter));
        }
        
        // never done earlier than wanted number of iterations
        return false;
    }

    public int getBiggestLabel() {
        return 10;
    }
    
    private void initEdgeDistribution() {
        if (iSettings.offBoundarySampleProbability > 0) {
            iEdgeImageDistr = generateDiscreteDistribution(iEdgeImage, iDistrRng);
        }
    }
    
    private void initConnectivities() {
        iFgNeighborsOffsets = iLabelImage.getConnFG().getPointOffsets();
        iFgNeighborsIndices = new int[iFgNeighborsOffsets.length];
        for (int i = 0; i < iFgNeighborsOffsets.length; ++i) iFgNeighborsIndices[i] = iLabelImage.pointToIndex(iFgNeighborsOffsets[i]); 
        
        iBgNeighborsOffsets = iLabelImage.getConnBG().getPointOffsets();
        iBgNeighborsIndices = new int[iBgNeighborsOffsets.length];
        for (int i = 0; i < iBgNeighborsOffsets.length; ++i) iBgNeighborsIndices[i] = iLabelImage.pointToIndex(iBgNeighborsOffsets[i]); 

        iTopoFunction = new TopologicalNumber(iLabelImage);
    }
    
    private void initLenghtProposal() {
        if (iSettings.useBiasedProposal) {
            iLengthProposalMask = new float[iBgNeighborsOffsets.length];
            for (int i = 0; i < iBgNeighborsOffsets.length; ++i) {
                iLengthProposalMask[i] = (float) (1.0 / iBgNeighborsOffsets[i].length());
            }
        }
    }
    
    private void initLabels() {
        // By default add background
        iLabels.add(LabelImage.BGLabel);
        iParentsProposalNormalizer.put(LabelImage.BGLabel, 0f);
        iChildrenProposalNormalizer.put(LabelImage.BGLabel, 0f);
        iTotalNormalizer = 0.0f;

        // Register all labels from lableImage
        Set<Integer> visitedLabels = new HashSet<>();
        visitedLabels.add(LabelImage.BGLabel);
        final Iterator<Integer> ri = new SpaceIterator(iLabelImage.getDimensions()).getIndexIterator();
        while (ri.hasNext()) {
            final int idx = ri.next();
            int label = iLabelImage.getLabel(idx);
            if (iLabelImage.isBorderLabel(label)) continue;

            // Add if not added so far
            int labelAbs = iLabelImage.labelToAbs(label);
            if (!visitedLabels.contains(labelAbs)) {
                visitedLabels.add(labelAbs);
                iLabels.add(label);
                iParentsProposalNormalizer.put(labelAbs, 0f);
                iChildrenProposalNormalizer.put(labelAbs, 0f);
            }
            // // Add all regular particles at this spot:
            MinimalParticleIndexedSet regularParticles = getRegularParticles(idx, new MinimalParticleIndexedSet());

            for (MinimalParticle particle : regularParticles) {
                if (isParticleTopoValid(particle)) {
                    insertCandidatesToContainers(particle, label, false);
                    iLabelImage.setLabel(idx, -labelAbs);
                }
            }
        }
    }
    
    private void prepareEnergyCalculationForEachIteration() {
        // Same as in RC algorithm
        if (iSettings.usingDeconvolutionPcEnergy) {
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
    }

    private void initEnergies() {
        // TODO: it is a'la initEnergies from RC
        if (iSettings.usingDeconvolutionPcEnergy) {
            // Deconvolution: - Alocate and initialize the 'ideal image'
            // TODO: This is not OOP, handling energies should be redesigned
            ((E_Deconvolution) iImageModel.getEdata()).GenerateModelImage(iLabelImage, iLabelStatistics);
            ((E_Deconvolution) iImageModel.getEdata()).RenewDeconvolution(iLabelImage, iLabelStatistics);
        }
    }

    private boolean runOneIteration() {
        prepareEnergyCalculationForEachIteration();

        // These list will help to revert the move in case it gets rejected.
        iParticlesHistory.clear();
        iFloatingParticlesHistory.clear();
        iLabelImageHistory.clear();
        
        // Cleanup working containers (the rest of them will be overwrite anyway) 
        for (int i = 0; i < iMcmcStepSize; ++i) {
            vParticle_Ab_IsFloating[i] = false;
            vParticle_Bb_IsFloating[i] = false;
            vParticle_A_IsFloating[i] = false;
        }

        // In the burn-in phase we delete floating particles so it might happen.
        if (iLabels.size() < 2) {
            // TODO: Should be handled differently than RuntimeEx?
            throw new RuntimeException("No active region for MCMC available in iteration: " + iIterationCounter);
        }
        
        // Sample a region label (a FG region number; without BG) and find the corresponding label
        int sampledIndex = iRng.GetIntegerVariate(iLabels.size() - 2) + 1;
        int sampledAbsLabel = iLabels.get(sampledIndex);
        
        // Off-boundary handling
        if (iSettings.allowFission && iSettings.allowFusion) {
            // off-boundary probability decreases to 0 when burn-in phase ends
            float offBoundaryPerc = iSettings.offBoundarySampleProbability * (1.0f - iIterationCounter / (iSettings.burnInFactor * iSettings.maxNumOfIterations));
            double rnd = iRng.GetVariate();
            boolean offBoundarySampling = (offBoundaryPerc > 0) ? rnd < offBoundaryPerc : false;
            if (offBoundarySampling) {
                boolean growth = iRng.GetUniformVariate(0, 1) < 0.5;
                return sampleOffBoundary(growth, sampledAbsLabel);
            }
        }
        
        // Figure out if Particle A will cause growth, shrink or be floating particle
        double probabilityOfFloatingParticle = iFloatingParticlesProposalNormalizer / (iTotalNormalizer + iFloatingParticlesProposalNormalizer);

        // Find active candidates
        boolean vParticleAIsFloating = false;
        MinimalParticleIndexedSet vActiveCandidates = null;
        double vR = iRng.GetUniformVariate(0.0, 1.0);
        if (vR < probabilityOfFloatingParticle) {
            // We will choose one out of the floating particles
            vActiveCandidates = iFloatingParticles;
            vParticleAIsFloating = true;
        }
        else if (vR < 0.5 * (probabilityOfFloatingParticle + 1)) {
            // We will grow and hence choose one out of the children list
            vActiveCandidates = iChildren.get(sampledAbsLabel);
        }
        else {
            vActiveCandidates = iParents.get(sampledAbsLabel);
        }
        if (vActiveCandidates == null || vActiveCandidates.size() == 0) {
            // This is an empty region. Maybe there exists a floating particle with no future for this region. 
            // But if count == 0, it will not be accepted according to the definition of the energy. We hence cleanup the statistics (kill the region).
            resetLabelsToParents();
            return false;
        }

        // For each particle within the region, calculate the proposal and create a discrete distribution over particles
        int vIndexOffset = 0;
        ArrayList<Pair<Integer, Double>> allParticlesFwdProposals = new ArrayList<>();
        boolean vApproxedIndex = false;
        if (iSettings.useBiasedProposal && !vParticleAIsFloating) {
            final int NumberOfSamplesForBiasedPropApprox = 30; // must be >= 1
            if (vActiveCandidates.size() < NumberOfSamplesForBiasedPropApprox) {
                for (int i = 0; i < vActiveCandidates.size(); ++i) {
                    allParticlesFwdProposals.add(new Pair<>(i, (double) vActiveCandidates.get(i).iProposal));
                }
            }
            else {
                vApproxedIndex = true;
                vIndexOffset = iRng.GetIntegerVariate(vActiveCandidates.size() - 1);
                for (int i = 0; i < NumberOfSamplesForBiasedPropApprox; ++i) {
                    int vApproxParticleIndex = (vIndexOffset + i) % vActiveCandidates.size();
                    allParticlesFwdProposals.add(new Pair<>(i, (double) vActiveCandidates.get(vApproxParticleIndex).iProposal));
                }
            }
        }
        EnumeratedDistribution<Integer> vDiscreteDistr = (allParticlesFwdProposals.size() > 0) ? new EnumeratedDistribution<>(iDistrRng, allParticlesFwdProposals) : null;

        // Find particle A:
        for (int i = 0; i < iMcmcStepSize; ++i) {
            int vParticleIndex;
            if (iSettings.useBiasedProposal && !vParticleAIsFloating && vDiscreteDistr != null) {
                vParticleIndex = vDiscreteDistr.sample();
                vq_A[i] = allParticlesFwdProposals.get(vParticleIndex).getSecond().floatValue();

                if (vApproxedIndex) {
                    vParticleIndex = (vIndexOffset + vParticleIndex) % vActiveCandidates.size();
                }
            }
            else {
                vParticleIndex = iRng.GetIntegerVariate(vActiveCandidates.size() - 1);
                vq_A[i] = 1.0f;
            }
            MinimalParticle vParticle = vActiveCandidates.get(vParticleIndex);
            if (vParticleAIsFloating) {
                vParticle_A_IsFloating[i] = true;

                // Immediately accept the move (self transition) if the label at the particular position is the same. Since this is a
                // self transition we keep the particle in the proposals.
                if (iLabelImage.getLabelAbs(vParticle.iIndex) == vParticle.iCandidateLabel) {
                    return false; // reject.
                }
                // reject (because the backward prob is 0)
                if (isRegularParticle(vParticle, iLabelImage.getLabelAbs(vParticle.iIndex))) {
                    return false;
                }
                // if we are here, do the job
                eraseFloatingParticle(vParticle, true);
            }
            // Fill in some properties for this particles:
            vLabelsBeforeJump_A[i] = iLabelImage.getLabelAbs(vParticle.iIndex);
            vq_A[i] = (vParticle_A_IsFloating[i]) ?
                        vq_A[i] / iFloatingParticlesProposalNormalizer :
                        vq_A[i] / getProposalNormalizer(vLabelsBeforeJump_A[i], vParticle.iCandidateLabel);
            vCandidateMoveVec[i] = vParticle;
        }

        // In case of pair proposals, we find a partner for each proposed particle.
        // We now know A and Q(A). Now it needs to build another (2nd step) discrete proposal distribution. 
        // We sample from it to determine the partner particle B. Furthermore we calculate the conditional proposal probability Q(B|A). 
        // In a second step we calculate the conditional Q(A|B). The same then needs to be done for the backward probabilities Qb(A), Qb(B), Qb(A|B) and Qb(B|A).
        // Notation:
        // - Q is the forward and Qb the backward probability. 
        // A is a forward praticle and B' the backward particle.
        // - Qb always assumes backward particles as its arguments! Hence, Qb_A_B is the probability Qb(A'|B').
        boolean vSingleParticleMoveForPairProposals = false;
        if (iSettings.usePairProposal) {
            // Iterate over particles A:
            for (int i = 0; i < iMcmcStepSize; ++i) {
                MinimalParticle vPartIt = vCandidateMoveVec[i];

                MinimalParticle vA = new MinimalParticle(vPartIt);
                vA.iProposal = calculateProposal(vA.iIndex);

                applyParticle(vPartIt, true);

                // BTW we have to remember if A' is a floating particle in the state x -> A.
                vParticle_Ab_IsFloating[i] = isParticleFloating(vPartIt.iIndex, vPartIt.iCandidateLabel);

                // Get the particles involved in the second step of the proposal (with updated proposals)
                MinimalParticleIndexedSet vParts_Q_BgivenA = getPartnerParticles(vA, new MinimalParticleIndexedSet());

                // Find B:
                MinimalParticle vB;

                // Choose B from Q(B|A) and calculate Q(B|A).
                if (iSettings.useBiasedProposal) {
                    ArrayList<Float> vProposalsVector = new ArrayList<>(vParts_Q_BgivenA.size());
                    float vNormalizer_Q_B_A = 0;
                    for (int vPI = 0; vPI < vParts_Q_BgivenA.size(); vPI++) {
                        MinimalParticle vCondParticle = vParts_Q_BgivenA.get(vPI);
                        vProposalsVector.add(vCondParticle.iProposal);
                        vNormalizer_Q_B_A += vCondParticle.iProposal;
                    }
                    EnumeratedDistribution<Integer> vQ_B_A = createDiscreteDistrFromValues(vProposalsVector);
                    vB = vParts_Q_BgivenA.get(vQ_B_A.sample());
                    // The value iProposal of vB is currently equal to Q_B_A.
                    vq_B_A[i] = vB.iProposal / vNormalizer_Q_B_A;
                }
                else {
                    vB = vParts_Q_BgivenA.get(iRng.GetIntegerVariate(vParts_Q_BgivenA.size() - 1));
                    vq_B_A[i] = 1.0f / vParts_Q_BgivenA.size();
                }

                // store B (and its original label).
                vPartnerMoveVec[i] = vB;
                vLabelsBeforeJump_B[i] = iLabelImage.getLabelAbs(vB.iIndex);
                if (vB.iIndex == vA.iIndex && vB.iCandidateLabel == vA.iCandidateLabel) {
                    vSingleParticleMoveForPairProposals = true;
                    vLabelsBeforeJump_B[i] = vLabelsBeforeJump_A[i];
                }

                // Get the reverse particle of A (without proposal update as it is not necessary):
                MinimalParticle vReverseParticleA = new MinimalParticle(vPartIt);
                vReverseParticleA.iCandidateLabel = vLabelsBeforeJump_A[i];

                // In case that vA == vB, we must already undo the simulated move in order to calculate Q'(B'|A') (== Q'(A'|B'))
                if (vSingleParticleMoveForPairProposals) {
                    applyParticle(vReverseParticleA, true);
                }

                // In the current state of the label image and the containers we can calculate qb_A'_B' as well. We assume now
                // that B' was applied and we calculate the probability for A'.
                MinimalParticleIndexedSet vParts_Qb_AgivenB = getPartnerParticles(vB, new MinimalParticleIndexedSet());

                if (iSettings.useBiasedProposal) {
                    float vNormalizer_Qb_A_B = 0;
                    for (int vPI = 0; vPI < vParts_Qb_AgivenB.size(); ++vPI) {
                        vNormalizer_Qb_A_B += vParts_Qb_AgivenB.get(vPI).iProposal;
                    }
                    float vqb_A_B_unnorm = calculateProposal(vA.iIndex);
                    vqb_A_B[i] = vqb_A_B_unnorm / vNormalizer_Qb_A_B;
                }
                else {
                    vqb_A_B[i] = 1.0f / vParts_Qb_AgivenB.size();
                }

                if (!vSingleParticleMoveForPairProposals) {
                    // undo the simulated move.
                    applyParticle(vReverseParticleA, true);

                    // Now we can calculate Q_B (as we now know B and the original state has been recovered).
                    if (!isRegularParticle(vB, vLabelsBeforeJump_B[i])) {
                        vq_B[i] = 0f;
                    }
                    else {
                        if (iSettings.useBiasedProposal) {
                            vq_B[i] = calculateProposal(vB.iIndex) / getProposalNormalizer(vLabelsBeforeJump_B[i], vB.iCandidateLabel);
                        }
                        else {
                            vq_B[i] = 1.0f / getProposalNormalizer(vLabelsBeforeJump_B[i], vB.iCandidateLabel);
                        }
                    }
                }
            }
        }

        // Currently it is possible that the same candidate is in the move set.
        // Hence we store the applied moves to avoid duplicates.
        MinimalParticleIndexedSet vAppliedParticles = new MinimalParticleIndexedSet();
        ArrayList<Integer> vAppliedParticleOrigLabels = new ArrayList<>();

        // Iterate the candidates, calculate the energy and perform the moves.
        float vTotEnergyDiff = 0;
        for (int i = 0; i < iMcmcStepSize; ++i) {
            MinimalParticle vParticleA = vCandidateMoveVec[i];
            MinimalParticle vParticleB = vPartnerMoveVec[i];

            // apply particle A and B, start with B:
            int vN = (iSettings.usePairProposal && !vSingleParticleMoveForPairProposals) ? 2 : 1;
            for (; vN > 0; --vN) {

                // it is necessary that we start with particle B as we have
                // to calculate Q(A|B) and Qb(B|A).
                MinimalParticle vCurrentMinimalParticle = null;
                int vOriginalLabel;
                if (vN > 1) {
                    vCurrentMinimalParticle = vParticleB;
                    vOriginalLabel = vLabelsBeforeJump_B[i];
                }
                else {
                    vCurrentMinimalParticle = vParticleA;
                    vOriginalLabel = vLabelsBeforeJump_A[i];
                }

                // We calculate the energy and apply them move iff
                // - the move has not been performed beforehand (a particle was sampled twice)
                // - THE FOLLOWING IS CURRENTLY A NON ISSUE AS B CANNOT BE A':
                // particle B is not the reverse particle of particle A (in case of m_usePairProposals, i.e. vN > 1). This is important
                // because the energy update gets corrupted as particle B is not a valid particle before A was applied. To resolve this
                // we just perform a one particle move (we don't apply B).
                if (!vAppliedParticles.contains(vCurrentMinimalParticle)) {
                    // Calculate the energy difference when changing this candidate:
                    vTotEnergyDiff += calculateEnergyDifference(vCurrentMinimalParticle.iIndex, vOriginalLabel, vCurrentMinimalParticle.iCandidateLabel, iIntensityImage.get(vCurrentMinimalParticle.iIndex));
                    applyParticle(vCurrentMinimalParticle, false);
                    vAppliedParticles.insert(vCurrentMinimalParticle);
                    vAppliedParticleOrigLabels.add(vOriginalLabel);
                }

                // Calculate Q(A|B) and Qb(B|A) in case we moved B only; this is when vN == 2.
                if (vN == 2) {
                    // Get the neighbors (conditional particles) and sum up  their proposal values; this is the normalizer for the discrete probability Q(A|B)
                    MinimalParticleIndexedSet vParts_Q_AgivenB = getRegularParticlesInFgNeighborhood(vParticleB.iIndex, new MinimalParticleIndexedSet());
                    // add particle B as this is always a candidate as well
                    vParticleB.iProposal = calculateProposal(vParticleB.iIndex);
                    vParts_Q_AgivenB.insert(vParticleB);

                    if (iSettings.useBiasedProposal) {
                        float vNormalizer_Q_A_B = 0;
                        for (int vPI = 0; vPI < vParts_Q_AgivenB.size(); vPI++) {
                            vNormalizer_Q_A_B += vParts_Q_AgivenB.get(vPI).iProposal;
                        }
                        // vParticleA.m_Proposal is not valid anymore. Particle A got a new proposal when applying particle B.
                        vq_A_B[i] = calculateProposal(vParticleA.iIndex) / vNormalizer_Q_A_B;
                    }
                    else {
                        vq_A_B[i] = 1.0f / vParts_Q_AgivenB.size();
                    }

                    // create A'
                    MinimalParticle vReverseParticleA = new MinimalParticle(vParticleA.iIndex, vLabelsBeforeJump_A[i], calculateProposal(vParticleA.iIndex));

                    // Calculate Qb(B'|A')
                    MinimalParticleIndexedSet vParts_Qb_BgivenA = getRegularParticlesInFgNeighborhood(vParticleA.iIndex, new MinimalParticleIndexedSet());
                    vParts_Qb_BgivenA.insert(vReverseParticleA);
                    if (iSettings.useBiasedProposal) {
                        float vNormalizer_Qb_B_A = 0;
                        for (int vPI = 0; vPI < vParts_Qb_BgivenA.size(); vPI++) {
                            vNormalizer_Qb_B_A += vParts_Qb_BgivenA.get(vPI).iProposal;
                        }
                        // the proposal of the backward particle (given A) is:
                        float vProposalBb = calculateProposal(vParticleB.iIndex);
                        vqb_B_A[i] = vProposalBb / vNormalizer_Qb_B_A;
                    }
                    else {
                        vqb_B_A[i] = 1.0f / vParts_Qb_BgivenA.size();
                    }
                }
            }
        }

        boolean vHardReject = false;
        for (int i = 0; i < iMcmcStepSize; ++i) {
            // Correct the containers whenever floating particles were involved: The method moveParticles, for simplicity, only works on the regular particle set.

            // First, figure out if A' or B' is floating:
            MinimalParticle vParticleA = vCandidateMoveVec[i];
            MinimalParticle vParticleB = vPartnerMoveVec[i];

            // Figure out if the backward particles are floating:
            if (iSettings.usePairProposal) {
                vParticle_Bb_IsFloating[i] = isParticleFloating(vParticleB.iIndex, vLabelsBeforeJump_B[i]);
            }
            else {
                // if we're not in pair proposal mode we did not yet check if A's reverse particle is floating (else we did already):
                vParticle_Ab_IsFloating[i] = isParticleFloating(vParticleA.iIndex, vLabelsBeforeJump_A[i]);
            }

            MinimalParticle vReverseFloatingP = null;
            // the first condition is needed when not using pair proposal mode
            if (vParticle_Ab_IsFloating[i]) {
                vReverseFloatingP = new MinimalParticle(vCandidateMoveVec[i].iIndex, vLabelsBeforeJump_A[i], calculateProposal(vCandidateMoveVec[i].iIndex));
            }
            // in pair proposal, if A' is floating, B' is as well (they are (not always) the same particle) - TBI why
            if (iSettings.usePairProposal && vParticle_Bb_IsFloating[i]) {
                vReverseFloatingP = new MinimalParticle(vPartnerMoveVec[i].iIndex, vLabelsBeforeJump_B[i], calculateProposal(vPartnerMoveVec[i].iIndex));
            }

            // finally convert the regular particle into a floating particle,  i.e. insert it in the floating DS and remove it from the regular:
            if (vParticle_Ab_IsFloating[i] || vParticle_Bb_IsFloating[i]) {
                // insert the reverse particle in the appropriate container. If there is no space, we reject the move.
                if (!(insertFloatingParticle(vReverseFloatingP, true))) {
                    vHardReject = true; //TODO: we could end iteration here since it will be rejected anyway
                }
            }
        }

        // We are now in the state x'. Calculate Q'(A) and maybe Q'(B). Note that this has to be done after all particles were applied.
        for (int i = 0; i < iMcmcStepSize; ++i) {
            MinimalParticle vParticleA = vCandidateMoveVec[i];
            MinimalParticle vParticleB = vPartnerMoveVec[i];

            // Calculate vqb_A and vqb_B
            if (!iSettings.useBiasedProposal) {
                vqb_A[i] = 1.0f;
                vqb_B[i] = 1.0f;
            }
            else {
                vqb_A[i] = calculateProposal(vParticleA.iIndex);
                if (iSettings.usePairProposal && !vSingleParticleMoveForPairProposals) {
                    vqb_B[i] = calculateProposal(vParticleB.iIndex);
                }
            }
            // Normalize vqb_A and vqb_B
            float vqb_A_normalizer = (vParticle_Ab_IsFloating[i]) ? (iFloatingParticlesProposalNormalizer) : getProposalNormalizer(vParticleA.iCandidateLabel, vLabelsBeforeJump_A[i]);
            vqb_A[i] = vqb_A[i] / vqb_A_normalizer;
            if (iSettings.usePairProposal && !vSingleParticleMoveForPairProposals) {
                float vqb_B_normalizer = vParticle_Bb_IsFloating[i] ? (iFloatingParticlesProposalNormalizer) : getProposalNormalizer(vParticleB.iCandidateLabel, vLabelsBeforeJump_B[i]);
                vqb_B[i] = vqb_B[i] / vqb_B_normalizer;
            }

            // Finally, we omit half of the calculations if particle A == B
            if (vSingleParticleMoveForPairProposals) {
                vq_B[i] = vq_A[i];
                vq_A_B[i] = vq_B_A[i];
                vqb_B_A[i] = vqb_A_B[i];
                vqb_B[i] = vqb_A[i];
            }
        }
        
        // Calculate the forward-backward ratio:
        float vForwardBackwardRatio = 1.0f;
        for (int i = 0; i < iMcmcStepSize; ++i) {
            if (iSettings.usePairProposal) {
                if (vParticle_Ab_IsFloating[i] || vParticle_Bb_IsFloating[i] || vParticle_A_IsFloating[i]) {
                    vForwardBackwardRatio *= (vqb_B[i] * vqb_A_B[i]) / (vq_A[i] * vq_B_A[i]);
                }
                else {
                    vForwardBackwardRatio *= (vqb_A[i] * vqb_B_A[i] + vqb_B[i] * vqb_A_B[i]) / (vq_A[i] * vq_B_A[i] + vq_B[i] * vq_A_B[i]);
                }
            }
            else {
                if (vParticle_A_IsFloating[i]) {
                    // we distroy a floating particle, in the next iteration there will be one floating particle less, hence the probability
                    // in the x' to sample a floating particle is (note that both normalizers are in state x'):
                    float vPProposeAFloatInXb = iFloatingParticlesProposalNormalizer / (iFloatingParticlesProposalNormalizer + iTotalNormalizer);
                    vForwardBackwardRatio *= 0.5f * (1 - vPProposeAFloatInXb) / probabilityOfFloatingParticle;
                    vForwardBackwardRatio *= vqb_A[i] / vq_A[i];
                }
                else if (vParticle_Ab_IsFloating[i]) {
                    // we create a floating particle, in the next iteration there will be one floating particle more, hence the probability
                    // in the x' to sample a floating particle is (note that iTotalNormalizer is updated to x'):
                    float vPProposeAFloatInXb = iFloatingParticlesProposalNormalizer / (iFloatingParticlesProposalNormalizer + iTotalNormalizer);
                    vForwardBackwardRatio *= vPProposeAFloatInXb / (0.5f * (1 - probabilityOfFloatingParticle));
                    vForwardBackwardRatio *= vqb_A[i] / vq_A[i];
                }
                else {
                    // Shrink and growth events have the same probability. We hence only need to compare the individual particle ratios.
                    vForwardBackwardRatio *= vqb_A[i] / vq_A[i];
                }
            }
        }
        
        // Should I stay or should I go; the Metropolis-Hastings algorithm:
        float vHastingsRatio = (float) Math.exp(-vTotEnergyDiff / iMcmcTemperature) * vForwardBackwardRatio;
        boolean vAccept = (vHastingsRatio >= 1) ?
                          true :
                          (vHastingsRatio > iRng.GetUniformVariate(0, 1)) ? true : false;
        
        // Register the result (if we accept) or rollback to the previous state.
        if (vAccept && !vHardReject) {
            for (int i = 0; i < vAppliedParticles.size(); ++i) {
                storeResult(vAppliedParticles.get(i).iIndex, vAppliedParticleOrigLabels.get(i), iIterationCounter);
            }
        }
        else {
            rejectParticles(vAppliedParticles, vAppliedParticleOrigLabels);
        }

        return vAccept;
    }

    private void rejectParticles(MinimalParticleIndexedSet aAppliedParticles, ArrayList<Integer> aOrigLabels) {
        // First, recover the theoretical state:
        // - recover the label image
        // - recover the particle set
        // As we have some (redundant) speedup statistics we also need to:
        // - recover the statistics (by simulation of the backward particle)
        // - recover the proposal normalizers (taken care of within the insertion/deletion methods).
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
                insertFloatingParticleCumulative(phe.particle, false);
            }
        }

        // recover the label image:
        for (int i = iLabelImageHistory.size() - 1; i >= 0; --i) {
            LabelImageHistoryEvent lihe = iLabelImageHistory.get(i);
            iLabelImage.setLabel(lihe.index, lihe.label);
        }

        // recover the statistics:
        for (int i = aOrigLabels.size() - 1; i >= 0; --i) {
            MinimalParticle vP = aAppliedParticles.get(i);
            updateLabelStatistics(iIntensityImage.get(vP.iIndex), vP.iCandidateLabel, aOrigLabels.get(i));
        }
    }

    private void insertFloatingParticleCumulative(MinimalParticle aParticle, boolean aDoRecord) {
        int index = iFloatingParticles.getIndex(aParticle);

        MinimalParticle vParticleInserted = null;
        if (index == -1) {
            // the particle did not yet exist
            vParticleInserted = aParticle;
        }
        else {
            // TODO: Investigate if this else {... } is ever executed 
            // The element did already exist. We add up the proposal and insert the element again (in order to overwrite).
            vParticleInserted = new MinimalParticle(iFloatingParticles.get(index));
            vParticleInserted.iProposal += aParticle.iProposal;
        }

        iFloatingParticlesProposalNormalizer += aParticle.iProposal;
        iFloatingParticles.insert(vParticleInserted);

        if (aDoRecord) {
            iFloatingParticlesHistory.add(new ParticleHistoryElement(aParticle, 0, true));
        }
    }

    private void storeResult(int aCandidateIndex, int aLabelBefore, int aIteration) {
        List<McmcResult> resultsForIndex = iMcmcResults.get(aCandidateIndex);
        if (resultsForIndex == null) {
            resultsForIndex = new ArrayList<>();
            iMcmcResults.put(aCandidateIndex, resultsForIndex);
        }
        resultsForIndex.add(new McmcResult(aIteration, aLabelBefore));
    }

    private float calculateEnergyDifference(int aIndex, int aCurrentLabel, int aToLabel, float aImgValue) {
        ContourParticle contourCandidate = new ContourParticle(aCurrentLabel, aImgValue);
        EnergyResult res = iImageModel.calculateDeltaEnergy(iLabelImage.indexToPoint(aIndex), contourCandidate, aToLabel, iLabelStatistics);

        return res.energyDifference.floatValue();
    }

    private void applyParticle(MinimalParticle aCandidateParticle, boolean aDoSimulate) {
        // Maintain the regular particle container and the label image:
        addAndRemoveParticlesWhenMove(aCandidateParticle);

        // Update the label image. The new point is either a contour particle or 0,
        // therefore the negative label value is set.
        int vFromLabel = iLabelImage.getLabelAbs(aCandidateParticle.iIndex);

        int vSign = -1; // standard sign of the label image of boundary particles
        if (iLabelImage.isEnclosedByLabelBgConnectivity(aCandidateParticle.iIndex, aCandidateParticle.iCandidateLabel)) {
            // we created a floating particle.
            vSign = 1;
        }

        storeLabelImageHistory(aCandidateParticle.iIndex, iLabelImage.getLabel(aCandidateParticle.iIndex));
        iLabelImage.setLabel(aCandidateParticle.iIndex, vSign * aCandidateParticle.iCandidateLabel);

        // Update the statistics of the propagating and the loser region.
        if (!aDoSimulate) {
            updateLabelStatistics(iIntensityImage.get(aCandidateParticle.iIndex), vFromLabel, aCandidateParticle.iCandidateLabel);
        }

        // Update the proposals for all particles in the neighborhood (as they might have changed).
        if (iSettings.useBiasedProposal || (!iSettings.allowFission || !iSettings.allowHandles)) {
            updateProposalsAndFilterTopologyInNeighborhood(aCandidateParticle);
        }
    }

    // Updates the DS (label image and the regular particle containers) when applying a particle. Note that floating particles will not be updated!
    // The method only ensures that L and the regular particles are correct. The method expects the label image NOT to be updated already. Else the
    // operations performed will be wrong.
    private void addAndRemoveParticlesWhenMove(MinimalParticle aCandidateParticle) {
        int candidateIndex = aCandidateParticle.iIndex;
        int absLabelTo = aCandidateParticle.iCandidateLabel;
        int absLabelFrom = iLabelImage.getLabelAbs(candidateIndex);

        // We remove the particle and insert the reverse particle: we cannot decide if the reverse particle is indeed.
        // The solution is that this method only works on the regular particle set. Floating particles will be detected and treated outside of
        // this method. Here we omit the potential insertion of floating particles.
        // In order to (maybe) replace the particle with its reverse particle we: Simulate the move, calculate the proposal for the backward particle,
        // create the backward particle, check if the backward particle is floating, and finally restore the label image:
        int savedOrigLabel = iLabelImage.getLabel(candidateIndex);
        iLabelImage.setLabel(candidateIndex, -absLabelTo);
        MinimalParticle reverseParticle = new MinimalParticle(candidateIndex, absLabelFrom, calculateProposal(candidateIndex));
        boolean reverseParticleIsFloating = isParticleFloating(reverseParticle.iIndex, reverseParticle.iCandidateLabel);
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
            for (int offset : iBgNeighborsIndices) {
                int index = candidateIndex + offset;
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

            for (int i = 0; i < iFgNeighborsOffsets.length; ++i) {
                int index = candidateIndex + iFgNeighborsIndices[i];
                int label = iLabelImage.getLabel(index);
                if (iLabelImage.isBorderLabel(label)) continue;

                // check if there are FG-neighbors with no other mother of the same label --> orphan. we first 'simulate' the move:
                int savedLabel = iLabelImage.getLabel(candidateIndex);
                iLabelImage.setLabel(candidateIndex, -absLabelTo);

                if (Math.abs(label) != absLabelFrom) {
                    // check if this neighbor has other mothers from this label:
                    boolean hasOtherMother = false;
                    for (Point vOff2 : iFgNeighborsOffsets) {
                        int vL2 = iLabelImage.getLabelAbs(candidateIndex + iLabelImage.pointToIndex(vOff2.add(iFgNeighborsOffsets[i])));
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

        // Growing: figure out the changes of candidates for the expanding region
        if (absLabelTo != 0) { // we are growing
            // Neighbors: Figure out what (neighboring)mother points are going to be interior points: simulate the move
            int vStoreLabel1 = iLabelImage.getLabel(candidateIndex);
            iLabelImage.setLabel(candidateIndex, -absLabelTo);

            for (int vOff : iBgNeighborsIndices) {
                int index = candidateIndex + vOff;
                int vL = iLabelImage.getLabel(index);
                if (vL == -absLabelTo && iLabelImage.isEnclosedByLabelBgConnectivity(index, absLabelTo)) {
                    // Remove the parent that got enclosed; it had a the label of the currently expanding region and a candidate label of 0.
                    eraseCandidatesFromContainers(new MinimalParticle(index, 0, 0), absLabelTo, true);
                    
                    // update the label image (we're not using the update mechanism of the optimizer anymore):
                    iLabelImage.setLabel(index, Math.abs(vL));

                    storeLabelImageHistory(index, vL);
                }
            }
            iLabelImage.setLabel(candidateIndex, vStoreLabel1);

            // Figure out if a point renders to a candidate. These are all the FG-neighbors with a different label that are not yet
            // candidates of the currently expanding region.
            for (int i = 0; i < iFgNeighborsOffsets.length; ++i) {
                int index = candidateIndex + iFgNeighborsIndices[i];
                int label = iLabelImage.getLabel(index);
                int absLabel = Math.abs(label);
                if (absLabel != absLabelTo && !iLabelImage.isBorderLabel(absLabel)) {
                    // check if there is no other mother (hence the particle is not in the container yet). This we could do by checking the
                    // neighborhood of the label image or by checking the containers.
                    // Here: we check the (not yet updated!) label image.
                    boolean vHasOtherMother = false;
                    for (Point vOff2 : iFgNeighborsOffsets) {
                        int vL2 = iLabelImage.getLabelAbs(candidateIndex + iLabelImage.pointToIndex(vOff2.add(iFgNeighborsOffsets[i])));
                        if (vL2 == absLabelTo) {
                            vHasOtherMother = true;
                            break;
                        }
                    }
                    if (!vHasOtherMother) {
                        // This is a new child. It's current label we have to read from the label image, the candidate label is the label of the currently expanding region.
                        boolean vNotExisted = insertCandidatesToContainers(new MinimalParticle(index, absLabelTo, calculateProposal(index)), absLabel, true);
                        if (vNotExisted) {
                            iLabelImage.setLabel(index, -absLabel);
                            storeLabelImageHistory(index, label);
                        }
                    }
                }
            }
        }
    }

    private boolean isParticleFloating(int aIndex, int aCandLabel) {
        if (aCandLabel > 0) {
            // This is a daughter. It is floating if there is no mother, i.e. if there is no corresponding region in the FG neighborhood.
            return iLabelImage.isSingleFgPoint(aIndex, aCandLabel);
        }
        // else: this is a potential mother (candidate label is 0). According to the BG connectivity it fulfills the floating property
        // only if there is no other region in the BG neighborhood. Otherwise this pixel might well go to the BG label without changing the topo.
        return iLabelImage.isEnclosedByLabelBgConnectivity(aIndex, iLabelImage.getLabelAbs(aIndex));
    }

    private float getProposalNormalizer(int aCurrentLabel, int aCandidateLabel) {
        return (aCandidateLabel == 0) ? 
                    iParentsProposalNormalizer.get(aCurrentLabel) : 
                    iChildrenProposalNormalizer.get(aCandidateLabel);
    }

    private boolean isRegularParticle(MinimalParticle aParticle, int aCurrentLabel) {
        return (aParticle.iCandidateLabel == LabelImage.BGLabel) ? 
                    iParents.get(aCurrentLabel).contains(aParticle) : 
                    iChildren.get(aParticle.iCandidateLabel).contains(aParticle);
    }

    // ================================= CLEANED UP ===================================================
    
    // Input for Algorithm
    private final LabelImage iLabelImage;
    private final IntensityImage iIntensityImage;
    private final IntensityImage iEdgeImage;
    private final ImageModel iImageModel;
    private final SettingsDRS iSettings;

    private int iAcceptedMoves = 0;
    private int iIterationCounter = 0;
    
    private Rng iRng = new Rng(1212);
    private Rng iDistrRng = new Rng();

    // Connectivities
    private Point[] iFgNeighborsOffsets;
    private int[] iFgNeighborsIndices;
    private Point[] iBgNeighborsOffsets;
    private int[] iBgNeighborsIndices;
    private TopologicalNumber iTopoFunction;

    private float[] iLengthProposalMask = null;

    private EnumeratedDistribution<Integer> iEdgeImageDistr = null;

    private List<Integer> iLabels = new ArrayList<>();
    private Map<Integer, Float> iParentsProposalNormalizer = new HashMap<>();
    private Map<Integer, Float> iChildrenProposalNormalizer = new HashMap<>();
    private float iTotalNormalizer;
    private Map<Integer, MinimalParticleIndexedSet> iChildren = new HashMap<>();
    private Map<Integer, MinimalParticleIndexedSet> iParents = new HashMap<>();
    private MinimalParticleIndexedSet iFloatingParticles = new MinimalParticleIndexedSet();
    private float iFloatingParticlesProposalNormalizer = 0;

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
    
    // Constant Parameters
    private int iMcmcStepSize = 1; // 32 is maximum value (no reason found yet)
    private float iMcmcTemperature = 1;
    
    // Working containers needed for each iteration hence created once here.
    private float[] vq_A = new float[iMcmcStepSize];
    private float[] vq_B = new float[iMcmcStepSize];
    private float[] vq_A_B = new float[iMcmcStepSize];
    private float[] vq_B_A = new float[iMcmcStepSize];
    private float[] vqb_A = new float[iMcmcStepSize];
    private float[] vqb_B = new float[iMcmcStepSize];
    private float[] vqb_A_B = new float[iMcmcStepSize];
    private float[] vqb_B_A = new float[iMcmcStepSize];
    private int[] vLabelsBeforeJump_A = new int[iMcmcStepSize];
    private int[] vLabelsBeforeJump_B = new int[iMcmcStepSize];
    private boolean[] vParticle_Ab_IsFloating = new boolean[iMcmcStepSize];
    private boolean[] vParticle_Bb_IsFloating = new boolean[iMcmcStepSize];
    private boolean[] vParticle_A_IsFloating = new boolean[iMcmcStepSize];
    private MinimalParticle[] vCandidateMoveVec = new MinimalParticle[iMcmcStepSize];
    private MinimalParticle[] vPartnerMoveVec = new MinimalParticle[iMcmcStepSize];
    
    // ------------------------- methods -------------------------------------------------------------
    
    /**
     * Set up a vector iLabels (MCMCRegionLabel) that maps natural numbers (index of the vector to the region labels).
     */
    private void resetLabelsToParents() {
        iLabels.clear();
        iLabels.addAll(iParents.keySet());
    }

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
     * Check if particle and its neighbors are topologically valid and update its proposal or removes if not.
     * @param aParticle
     */
    private void updateProposalsAndFilterTopologyInNeighborhood(MinimalParticle aParticle) {
        for (int i = -1; i < iBgNeighborsIndices.length; ++i) {
            // for -1 use 0 offset (position of particle itself)
            int offset = (i >= 0) ? iBgNeighborsIndices[i] : 0;

            MinimalParticleIndexedSet particleSet = getRegularParticles(aParticle.iIndex + offset, new MinimalParticleIndexedSet());

            for (MinimalParticle particle : particleSet) {
                int label = (particle.iCandidateLabel == 0) ? iLabelImage.getLabelAbs(particle.iIndex) : particle.iCandidateLabel;

                if (isParticleTopoValid(particle)) {
                    // replace the particle with the new proposal
                    MinimalParticle updatedParticle = new MinimalParticle(particle.iIndex, particle.iCandidateLabel, calculateProposal(particle.iIndex));
                    insertCandidatesToContainers(updatedParticle, label, true);
                }
                else {
                    // remove the particle
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
            MinimalParticle p = aParticleSet.get(i);
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
    private MinimalParticleIndexedSet getRegularParticlesInBgNeighborhood(int aIndex, MinimalParticleIndexedSet aList) {
        for (int offset : iBgNeighborsIndices) {
            getRegularParticles(aIndex + offset, aList);
        }
        return aList;
    }
    
    /**
     * Gets regular particles into aList for FG neighborhood of aIndex
     * @param aIndex
     * @param aList
     */
    private MinimalParticleIndexedSet getRegularParticlesInFgNeighborhood(int aIndex, MinimalParticleIndexedSet aList) {
        for (int offset : iFgNeighborsIndices) {
            getRegularParticles(aIndex + offset, aList);
        }
        return aList;
    }
    
    /**
     * Inserts to a list all possible candidates from BG neighbors
     * @param aParticle
     * @param aSet
     */
    private MinimalParticleIndexedSet getPartnerParticles(MinimalParticle aParticle, MinimalParticleIndexedSet aSet) {
        // Get all correct particles in BG neighborhood
        MinimalParticleIndexedSet conditionalParticles = getRegularParticlesInBgNeighborhood(aParticle.iIndex, new MinimalParticleIndexedSet());
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
        return aSet;
    }
    
    /**
     * Inserts to a list possible candidates for particle at aIndex
     * @param aIndex
     * @param aSet
     */
    private MinimalParticleIndexedSet getRegularParticles(int aIndex, MinimalParticleIndexedSet aSet) {
        int currentLabel = iLabelImage.getLabel(aIndex);
        if (iLabelImage.isBorderLabel(currentLabel)) return aSet;
        
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
                    aSet.insert(new MinimalParticle(aIndex, absNeighborLabel, proposal));
                }
                if (!isParentInserted && currentLabel != LabelImage.BGLabel) {
                    // this is a non-background pixel with different neighbors, hence there must be a parent in the list
                    aSet.insert(new MinimalParticle(aIndex, 0, proposal));
                    isParentInserted = true;
                }
            }
        }

        // Check the BG neighborhood now if we need to insert a parent.
        if (!isParentInserted && currentLabel != LabelImage.BGLabel) {
            for (Integer idx : iLabelImage.iterateBgNeighbours(aIndex)) {
                if (iLabelImage.getLabelAbs(idx) != absCurrentLabel) {
                    // This is a FG pixel with a neighbor of a different label. Finally, insert a parent particle.
                    aSet.insert(new MinimalParticle(aIndex, 0, proposal));
                    break;
                }
            }
        }
        return aSet;
    }
    
    /**
     * @return index of non-boundary point sampled from edge density
     */
    private int sampleIndexFromEdgeDensity() {
        // Sample non-boundary point
        int index = -1;
        do {
            index = iEdgeImageDistr.sample();
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

        return new EnumeratedDistribution<>(iDistrRng, pmf);
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
        int index = iFloatingParticles.getIndex(aParticle);
        if (index == -1) {
            // particle does not exist
            return;
        }
        
        MinimalParticle updatedParticle = iFloatingParticles.get(index);
        if (updatedParticle.iProposal - aParticle.iProposal > Math.ulp(1.0f) * 10) {
            // the particle has still some proposal left so only update particle in a container without removing it
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
        // sample candidate label and location
        int candidateLabel = (iRng.GetUniformVariate(0, 1) > 0.5) ? aLabel : 0;
        int particleIndex = sampleIndexFromEdgeDensity();
        MinimalParticle particle = new MinimalParticle(particleIndex, candidateLabel, 0);
        
        boolean particleExists = iFloatingParticles.contains(particle);

        if (!aGrowth && particleExists) {
            eraseFloatingParticle(particle, false);
        }
        else if (aGrowth && !particleExists) {
            particle.iProposal = calculateProposal(particle.iIndex);
            insertFloatingParticle(particle, false);
        }
        else {
            return false; // reject
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
                length += iLengthProposalMask[i];
                isFloatingParticle = false;
            }
        }
        if (isFloatingParticle) {
            // floating particles need a proposal > 0: We take half of the smallest element in the mask
            // (we assume the smallest element to be at position 0).
            length = iLengthProposalMask[0] / 2.0f;
        }
        
        return length;
    }
    
    /**
     * Creates output probability image with slice for every label.
     * @return 
     */
    public SegmentationProcessWindow createProbabilityImage() {
        // Create output stack image
        int[] dims = iLabelImage.getDimensions();
        SegmentationProcessWindow resultImg = new SegmentationProcessWindow(dims[0], dims[1], true);

        int numOfBurnInIterations = (int) (iSettings.burnInFactor * iIterationCounter);
        int numOfCountableIterations = iIterationCounter - numOfBurnInIterations;
        
        for (int currentLabel : iLabels) {
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
    
    // TODO: Belaw is statistics stuff same as in AlgorithmRC - reuse somehow.
    
    /**
     * Initializes statistics. For each found label creates LabelStatistics object and stores it in labelStatistics container. TODO: Same as in AlgorithmRC - reuse somehow.
     */
    private int initStatistics() {
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
            stat.iVarIntensity = calculateVariance(stats.iSumOfSq,  stat.iMeanIntensity, n);
            // Median on start set equal to mean
            stat.iMedianIntensity = stat.iMeanIntensity;
        }
        
        return maxUsedLabel;
    }

    private double calculateVariance(double aSumSq, double aMean, int aN) {
        return (aN < 2) ? 0 : (aSumSq - aN * aMean * aMean) / (aN - 1.0);
    }
    
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
        toStats.iVarIntensity = calculateVariance(toStats.iSumOfSq, toStats.iMeanIntensity, toStats.iLabelCount);
        fromStats.iVarIntensity = calculateVariance(fromStats.iSumOfSq, fromStats.iMeanIntensity, fromStats.iLabelCount);
    }
}
