package mosaic.regions.DRS;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import ij.ImagePlus;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.convolution.Convolver;
import mosaic.core.imageUtils.convolution.Gauss1D;
import mosaic.core.imageUtils.convolution.Kernel1D;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.regions.GUI.SegmentationProcessWindow;
import mosaic.regions.RC.ContourParticle;
import mosaic.regions.energies.Energy.EnergyResult;
import mosaic.regions.energies.ImageModel;
import mosaic.regions.topology.TopologicalNumber;
import mosaic.regions.topology.TopologicalNumber.TopologicalNumberResult;
import mosaic.regions.utils.LabelStatisticToolbox;
import mosaic.regions.utils.LabelStatistics;
import mosaic.utils.Debug;
import mosaic.utils.math.IndexedDiscreteDistribution;


public class AlgorithmDRS {
    private static final Logger logger = Logger.getLogger(AlgorithmDRS.class);
    
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

    private IndexedDiscreteDistribution iEdgeImageDistr = null;

    private List<Integer> iLabels = new ArrayList<>();
    private Map<Integer, Float> iParentsProposalNormalizer = new HashMap<>();
    private Map<Integer, Float> iChildrenProposalNormalizer = new HashMap<>();
    private float iTotalNormalizer;
    private Map<Integer, ParticleSet> iChildren = new HashMap<>();
    private Map<Integer, ParticleSet> iParents = new HashMap<>();
    private ParticleSet iFloatingParticles = new ParticleSet();
    private float iFloatingParticlesProposalNormalizer = 0;

    private class ParticleHistoryElement {
        final Particle particle;
        final int originalLabel;
        final boolean wasAdded;

        public ParticleHistoryElement(Particle vReplacedParticle, int aCurrentLabel, boolean b) {
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
    private int iMcmcStepSize = 1; // 32 is maximum value (seems that might be at max NumberOfSamplesForBiasedProposalApproximation + 2)
    private float iMcmcTemperature = 1;
    
    // Working containers (proposals, labels and floting info) needed for each iteration hence created once here.
    private float[] Q_A = new float[iMcmcStepSize];
    private int[] currentLabelAtParticleA = new int[iMcmcStepSize];
    private Particle[] candidateToMoveParticleA = new Particle[iMcmcStepSize];
    private float[] Q_B = new float[iMcmcStepSize];
    private int[] currentLabelAtParticleB = new int[iMcmcStepSize];
    private Particle[] partnerToMoveParticleB = new Particle[iMcmcStepSize];
    private float[] Q_A_B = new float[iMcmcStepSize];
    private float[] Q_B_A = new float[iMcmcStepSize];
    private float[] Qb_A = new float[iMcmcStepSize];
    private float[] Qb_B = new float[iMcmcStepSize];
    private float[] Qb_A_B = new float[iMcmcStepSize];
    private float[] Qb_B_A = new float[iMcmcStepSize];
    private boolean[] isParticleAfloating = new boolean[iMcmcStepSize];
    private boolean[] isParticleAbFloating = new boolean[iMcmcStepSize];
    private boolean[] isParticleBbFloating = new boolean[iMcmcStepSize];
    
    
    public AlgorithmDRS(IntensityImage aIntensityImage, LabelImage aLabelImage, ImageModel aModel, SettingsDRS aSettings) {
        logger.debug("DRS algorithm created with settings:" + Debug.getJsonString(aSettings));

        // Save input parameters
        iLabelImage = aLabelImage;
        iIntensityImage = aIntensityImage;
        logger.debug("Generating edge image");
        iEdgeImage = generateEdgeImage(iIntensityImage);
        iImageModel = aModel;
        iSettings = aSettings;

        // Initialize label image
        iLabelImage.initBorder();

        logger.debug("Initializing conectivities");
        initConnectivities();
        logger.debug("Initializing distribution");
        initEdgeDistribution();
        logger.debug("Initializing length proposal");
        initLenghtProposal();
        logger.debug("Initializing labels");
        initLabels();
        logger.debug("Initializing statistics");
        LabelStatisticToolbox.initStatistics(iLabelImage, iIntensityImage, iLabelStatistics);
    }
    
    /**
     * Init FG and BG connectivities and topological function
     */
    private void initConnectivities() {
        iFgNeighborsOffsets = iLabelImage.getConnFG().getPointOffsets();
        iFgNeighborsIndices = new int[iFgNeighborsOffsets.length];
        for (int i = 0; i < iFgNeighborsOffsets.length; ++i) iFgNeighborsIndices[i] = iLabelImage.pointToIndex(iFgNeighborsOffsets[i]); 
        
        iBgNeighborsOffsets = iLabelImage.getConnBG().getPointOffsets();
        iBgNeighborsIndices = new int[iBgNeighborsOffsets.length];
        for (int i = 0; i < iBgNeighborsOffsets.length; ++i) iBgNeighborsIndices[i] = iLabelImage.pointToIndex(iBgNeighborsOffsets[i]); 

        iTopoFunction = new TopologicalNumber(iLabelImage);
    }
    
    /**
     * Init Length Proposal
     */
    private void initLenghtProposal() {
        if (iSettings.useBiasedProposal) {
            iLengthProposalMask = new float[iBgNeighborsOffsets.length];
            for (int i = 0; i < iBgNeighborsOffsets.length; ++i) {
                iLengthProposalMask[i] = (float) (1.0 / iBgNeighborsOffsets[i].length());
            }
        }
    }
    
    /**
     * Init labels and parent/children normalizer from input label image
     * Marks by -labelValue all places that are topoligaccly valid for change in label image
     */
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
            // Add all regular particles at this spot
            for (Particle particle : getRegularParticles(idx, new ParticleSet())) {
                if (isParticleTopoValid(particle)) {
                    insertCandidatesToContainers(particle, label, false);
                    iLabelImage.setLabel(idx, -labelAbs);
                }
            }
        }
    }
    
    /**
     * Runs single iteration and counts accepted moves
     */
    public void runOneIteration() {
        ++iIterationCounter;
        iAcceptedMoves += runIteration() ? 1 : 0;
        if (iIterationCounter == iSettings.maxNumOfIterations) {
            logger.info("Overall acceptance rate: " + ((float) iAcceptedMoves / iIterationCounter));
        }
    }
    
    private boolean runIteration() {
        // These list will help to revert the move in case it gets rejected.
        iParticlesHistory.clear();
        iFloatingParticlesHistory.clear();
        iLabelImageHistory.clear();
        
        // Cleanup working containers (the rest of them will be overwritten anyway) 
        for (int i = 0; i < iMcmcStepSize; ++i) {
            isParticleAbFloating[i] = false;
            isParticleBbFloating[i] = false;
            isParticleAfloating[i] = false;
        }

        // In the burn-in phase we delete floating particles so it might happen.
        if (iLabels.size() < 2) {
            throw new IllegalStateException("No active region for MCMC available in iteration: " + iIterationCounter);
        }
        
        // Sample a region label (a FG region number; without BG) and find the corresponding label
        int sampledIndex = iRng.GetIntegerVariate(iLabels.size() - 2) + 1;
        int sampledLabel = iLabels.get(sampledIndex);
        
        // --------------------------------------------------- off-boundary handling
        if (iSettings.allowFission && iSettings.allowFusion && iSettings.offBoundarySampleProbability > 0) {
            // off-boundary probability decreases to 0 when burn-in phase ends
            float offBoundaryPerc = iSettings.offBoundarySampleProbability * (1.0f - iIterationCounter / (iSettings.burnInFactor * iSettings.maxNumOfIterations));
            if ((offBoundaryPerc > 0) && (iRng.GetVariate() < offBoundaryPerc)) {
                return sampleOffBoundary(sampledLabel);
            }
        }

        // --------------------------------------------------- Find active candidates
        boolean particleAisFloating = false;
        ParticleSet activeCandidates = null;
        double rand = iRng.GetUniformVariate(0.0, 1.0);
        double probabilityOfFloatingParticle = iFloatingParticlesProposalNormalizer / (iTotalNormalizer + iFloatingParticlesProposalNormalizer);
        if (rand < probabilityOfFloatingParticle) {
            // We will choose one out of the floating particles
            activeCandidates = iFloatingParticles;
            particleAisFloating = true;
        }
        else { 
            // divide range [probabilityOfFloatingParticle - 1] by 2 and decide children (growth) vs parents (shrink)
            activeCandidates = (rand < (probabilityOfFloatingParticle + 1) / 2) ? iChildren.get(sampledLabel) : iParents.get(sampledLabel);
        }
        
        if (activeCandidates.size() == 0) {
            // This is an empty region. Maybe there exists a floating particle with no future for this region. 
            // But if count == 0, it will not be accepted according to the definition of the energy. 
            // We hence cleanup the statistics (kill the region).
            resetLabelsToParents();
            return false;
        }

        // ------------------------------ For each particle within the region, calculate the proposal and create a discrete distribution over particles
        int approxedIndexOffset = 0;
        IndexedDiscreteDistribution candidatesProposalsDistr = null;
        if (iSettings.useBiasedProposal && !particleAisFloating) {
            final int NumberOfSamplesForBiasedProposalApproximation = 30; // must be >= 1
            int size = activeCandidates.size() < NumberOfSamplesForBiasedProposalApproximation ? activeCandidates.size() : NumberOfSamplesForBiasedProposalApproximation;
            boolean approxedIndex = (size == NumberOfSamplesForBiasedProposalApproximation);
            if (approxedIndex) approxedIndexOffset = iRng.GetIntegerVariate(activeCandidates.size() - 1);
            double[] allParticlesProposals = new double[size];
            for (int k = 0; k < size; ++k) {
                int idx = (k + approxedIndexOffset) % activeCandidates.size();
                allParticlesProposals[k] = activeCandidates.get(idx).iProposal;
            }
            candidatesProposalsDistr =  new IndexedDiscreteDistribution(iDistrRng, allParticlesProposals);
        }

        // ------------------------------ Find particle(s) A and udpate its proposals / canndidateToMove / currentLabel containers
        for (int i = 0; i < iMcmcStepSize; ++i) {
            int particleIndex;
            if (iSettings.useBiasedProposal && !particleAisFloating && candidatesProposalsDistr != null) {
                particleIndex = candidatesProposalsDistr.sample();
                // calculate real index if proposal was calculated from subset of all particles
                particleIndex = (particleIndex + approxedIndexOffset) % activeCandidates.size();
                Q_A[i] = activeCandidates.get(particleIndex).iProposal;
            }
            else {
                particleIndex = iRng.GetIntegerVariate(activeCandidates.size() - 1);
                Q_A[i] = 1.0f;
            }
            Particle particleA = activeCandidates.get(particleIndex);
            int currentAbsLabelAtA = iLabelImage.getLabelAbs(particleA.iIndex);
            
            if (particleAisFloating) {
                isParticleAfloating[i] = true;

                // Immediately accept the move (self transition) if the label at the particular position is the same. 
                // Since this is a self transition we keep the particle in the proposals.
                if (currentAbsLabelAtA == particleA.iCandidateLabel) {
                    return false; // reject.
                }
                // reject (because the backward probability is 0)
                if (isRegularParticle(particleA, currentAbsLabelAtA)) {
                    return false;
                }
                // it will be processed but remove it from container first
                eraseFloatingParticle(particleA, true);
            }
            
            Q_A[i] /= (isParticleAfloating[i]) ? iFloatingParticlesProposalNormalizer :
                                                 getProposalNormalizer(currentAbsLabelAtA, particleA.iCandidateLabel);
            currentLabelAtParticleA[i] = currentAbsLabelAtA;
            candidateToMoveParticleA[i] = particleA;
        }

        // In case of pair proposals, we find a partner for each proposed particle. We now know A and Q(A). 
        // We build another (2nd step) discrete proposal distribution and sample from it to determine the partner particle B. 
        // Furthermore we calculate the conditional proposal probability Q(B|A). Later we calculate the conditional Q(A|B). 
        // The same then needs to be done for the backward probabilities Qb(A), Qb(B), Qb(A|B) and Qb(B|A). Notation:
        // - Q is the forward and Qb the backward probability. 
        // - A is a forward praticle and B' the backward particle.
        // - Qb always assumes backward particles as its arguments! Hence, Qb_A_B is the probability Qb(A'|B').
        boolean singleParticleMoveForPairProposals = false;
        if (iSettings.usePairProposal) {
            for (int i = 0; i < iMcmcStepSize; ++i) {
                Particle particleA = candidateToMoveParticleA[i];
                Particle vA = new Particle(particleA.iIndex, particleA.iCandidateLabel, calculateProposal(particleA.iIndex));
                applyParticle(particleA, true);

                // BTW we have to remember if A' is a floating particle in the state x -> A.
                isParticleAbFloating[i] = isParticleFloating(particleA.iIndex, particleA.iCandidateLabel);

                Particle vB;
                // Get the particles involved in the second step of the proposal (with updated proposals)
                ParticleSet particles_Q_B_A = getPartnerParticles(vA);
                // Choose B from Q(B|A) and calculate Q(B|A).
                if (iSettings.useBiasedProposal) {
                    double[] proposals_Q_B_A = new double[particles_Q_B_A.size()];
                    float normalizer_Q_B_A = 0;
                    for (int k = 0; k < particles_Q_B_A.size(); ++k) {
                        proposals_Q_B_A[k] = particles_Q_B_A.get(k).iProposal;
                        normalizer_Q_B_A += proposals_Q_B_A[k];
                    }
                    IndexedDiscreteDistribution vQ_B_A = new IndexedDiscreteDistribution(iDistrRng, proposals_Q_B_A);
                    vB = particles_Q_B_A.get(vQ_B_A.sample());
                    Q_B_A[i] = vB.iProposal / normalizer_Q_B_A;
                }
                else {
                    vB = particles_Q_B_A.get(iRng.GetIntegerVariate(particles_Q_B_A.size() - 1));
                    Q_B_A[i] = 1.0f / particles_Q_B_A.size();
                }

                // store B (and its original label).
                partnerToMoveParticleB[i] = vB;
                currentLabelAtParticleB[i] = iLabelImage.getLabelAbs(vB.iIndex);
                if (vB.iIndex == vA.iIndex && vB.iCandidateLabel == vA.iCandidateLabel) {
                    singleParticleMoveForPairProposals = true;
                    currentLabelAtParticleB[i] = currentLabelAtParticleA[i];
                }

                // Get the reverse particle of A (without proposal update as it is not necessary):
                Particle vReverseParticleA = new Particle(particleA.iIndex, currentLabelAtParticleA[i], particleA.iProposal);

                // In case that vA == vB, we must already undo the simulated move in order to calculate Q'(B'|A') (== Q'(A'|B'))
                if (singleParticleMoveForPairProposals) applyParticle(vReverseParticleA, true);

                // In the current state of the label image and the containers we can calculate Qb_A_B aka Q(A'|B') as well. 
                // We assume now that B' was applied and we calculate the probability for A'.
                // TODO: Original comment above claims that B' was applied but ... where? In original code B' is created but not used.
                ParticleSet particles_Qb_A_B = getPartnerParticles(vB);
                if (iSettings.useBiasedProposal) {
                    float normalizer_Qb_A_B = 0;
                    for (Particle p : particles_Qb_A_B) normalizer_Qb_A_B += p.iProposal;
                    Qb_A_B[i] = calculateProposal(vA.iIndex) / normalizer_Qb_A_B;
                }
                else {
                    Qb_A_B[i] = 1.0f / particles_Qb_A_B.size();
                }

                if (!singleParticleMoveForPairProposals) {
                    applyParticle(vReverseParticleA, true); // undo the simulated move.
                    // Now we can calculate Q_B (as we now know B and the original state has been recovered).
                    Q_B[i] = 0f;
                    if (isRegularParticle(vB, currentLabelAtParticleB[i])) {
                        Q_B[i] = (iSettings.useBiasedProposal ? calculateProposal(vB.iIndex) : 1.0f) / getProposalNormalizer(currentLabelAtParticleB[i], vB.iCandidateLabel);
                    }
                }
            }
        }

        // Currently it is possible that the same candidate is in the move set. Hence we store the applied moves to avoid duplicates.
        ParticleSet appliedParticles = new ParticleSet();
        ArrayList<Integer> appliedParticleOrigLabels = new ArrayList<>();

        // Iterate the candidates, calculate the energy and perform the moves.
        float energyDiff = 0;
        for (int i = 0; i < iMcmcStepSize; ++i) {
            Particle particleA = candidateToMoveParticleA[i];
            Particle particleB = partnerToMoveParticleB[i];

            // apply particle A and B, start with B - it is necessary that we start with particle B as we have to calculate Q(A|B) and Qb(B|A)
            int numOfParticlesToApply = (iSettings.usePairProposal && !singleParticleMoveForPairProposals) ? 2 : 1;
            for (; numOfParticlesToApply > 0; --numOfParticlesToApply) {
                boolean shouldProcessB = (numOfParticlesToApply > 1);
                Particle currentMinimalParticle = shouldProcessB ? particleB : particleA;
                int originalLabel = shouldProcessB ? currentLabelAtParticleB[i] : currentLabelAtParticleA[i];

                // We calculate the energy and apply them move iff
                // - the move has not been performed beforehand (a particle was sampled twice)
                // - THE FOLLOWING IS CURRENTLY A NON ISSUE AS B CANNOT BE A':
                // particle B is not the reverse particle of particle A (in case of m_usePairProposals, i.e. vN > 1). This is important
                // because the energy update gets corrupted as particle B is not a valid particle before A was applied. To resolve this
                // we just perform a one particle move (we don't apply B).
                if (!appliedParticles.contains(currentMinimalParticle)) {
                    // Calculate the energy difference when changing this candidate:
                    energyDiff += calculateEnergyDifference(currentMinimalParticle.iIndex, originalLabel, currentMinimalParticle.iCandidateLabel, iIntensityImage.get(currentMinimalParticle.iIndex));
                    applyParticle(currentMinimalParticle, false);
                    appliedParticles.insert(currentMinimalParticle);
                    appliedParticleOrigLabels.add(originalLabel);
                }

                // Calculate Q(A|B) and Qb(B|A) in case we moved B only; this is when numOfParticlesToApply == 2.
                if (shouldProcessB) {
                    // Get the neighbors (conditional particles) and sum up  their proposal values; this is the normalizer for the discrete probability Q(A|B)
                    ParticleSet particles_Q_A_B = getRegularParticlesInFgNeighborhood(particleB.iIndex);
                    // add particle B as this is always a candidate as well
                    particleB.iProposal = calculateProposal(particleB.iIndex);
                    particles_Q_A_B.insert(particleB);
                    if (iSettings.useBiasedProposal) {
                        float normalizer_Q_A_B = 0;
                        for (Particle p : particles_Q_A_B) normalizer_Q_A_B += p.iProposal;
                        // vParticleA.m_Proposal is not valid anymore. Particle A got a new proposal when applying particle B.
                        Q_A_B[i] = calculateProposal(particleA.iIndex) / normalizer_Q_A_B;
                    }
                    else {
                        Q_A_B[i] = 1.0f / particles_Q_A_B.size();
                    }

                    // create A'
                    Particle reverseParticleA = new Particle(particleA.iIndex, currentLabelAtParticleA[i], calculateProposal(particleA.iIndex));

                    // Calculate Qb(B'|A')
                    ParticleSet particles_Qb_B_A = getRegularParticlesInFgNeighborhood(particleA.iIndex);
                    particles_Qb_B_A.insert(reverseParticleA);
                    if (iSettings.useBiasedProposal) {
                        float normalizer_Qb_B_A = 0;
                        for (Particle p : particles_Qb_B_A) normalizer_Qb_B_A += p.iProposal;
                        Qb_B_A[i] = calculateProposal(particleB.iIndex) / normalizer_Qb_B_A;
                    }
                    else {
                        Qb_B_A[i] = 1.0f / particles_Qb_B_A.size();
                    }
                }
            }
        }

        boolean hardReject = false;
        for (int i = 0; i < iMcmcStepSize; ++i) {
            // Correct the containers whenever floating particles were involved: The method moveParticles, for simplicity, only works on the regular particle set.

            // First, figure out if A' or B' is floating:
            Particle particleA = candidateToMoveParticleA[i];
            Particle particleB = partnerToMoveParticleB[i];
            if (iSettings.usePairProposal) {
                isParticleBbFloating[i] = isParticleFloating(particleB.iIndex, currentLabelAtParticleB[i]);
            }
            else {
                // if we're not in pair proposal mode we did not yet check if A's reverse particle is floating (else we did already):
                isParticleAbFloating[i] = isParticleFloating(particleA.iIndex, currentLabelAtParticleA[i]);
            }

            Particle reverseFloatingParticle = null;
            // the first condition is needed when not using pair proposal mode
            if (isParticleAbFloating[i]) {
                reverseFloatingParticle = new Particle(candidateToMoveParticleA[i].iIndex, currentLabelAtParticleA[i], calculateProposal(candidateToMoveParticleA[i].iIndex));
            }
            // in pair proposal, if A' is floating, B' is as well (they are (not always) the same particle) - TBI why
            if (iSettings.usePairProposal && isParticleBbFloating[i]) {
                reverseFloatingParticle = new Particle(partnerToMoveParticleB[i].iIndex, currentLabelAtParticleB[i], calculateProposal(partnerToMoveParticleB[i].iIndex));
            }

            // finally convert the regular particle into a floating particle,  i.e. insert it in the floating DS and remove it from the regular:
            if (isParticleAbFloating[i] || isParticleBbFloating[i]) {
                // insert the reverse particle in the appropriate container. If there is no space, we reject the move.
                if (!(insertFloatingParticle(reverseFloatingParticle, true))) {
                    hardReject = true;
                }
            }
        }

        // We are now in the state x'. Calculate Q'(A) and maybe Q'(B). Note that this has to be done after all particles were applied.
        for (int i = 0; i < iMcmcStepSize; ++i) {
            Particle particleA = candidateToMoveParticleA[i];
            Particle particleB = partnerToMoveParticleB[i];

            // Calculate Qb_A and Qb_B
            if (!iSettings.useBiasedProposal) {
                Qb_A[i] = 1.0f;
                Qb_B[i] = 1.0f;
            }
            else {
                Qb_A[i] = calculateProposal(particleA.iIndex);
                if (iSettings.usePairProposal && !singleParticleMoveForPairProposals) {
                    Qb_B[i] = calculateProposal(particleB.iIndex);
                }
            }
            
            // Normalize Qb_A and Qb_B
            float normalizer_Qb_A = (isParticleAbFloating[i]) ? (iFloatingParticlesProposalNormalizer) : getProposalNormalizer(particleA.iCandidateLabel, currentLabelAtParticleA[i]);
            Qb_A[i] /= normalizer_Qb_A;
            if (iSettings.usePairProposal && !singleParticleMoveForPairProposals) {
                float normalizer_Qb_B = isParticleBbFloating[i] ? (iFloatingParticlesProposalNormalizer) : getProposalNormalizer(particleB.iCandidateLabel, currentLabelAtParticleB[i]);
                Qb_B[i] /= normalizer_Qb_B;
            }

            // Finally, we omit half of the calculations if particle A == B
            if (singleParticleMoveForPairProposals) {
                Q_B[i] = Q_A[i];
                Q_A_B[i] = Q_B_A[i];
                Qb_B_A[i] = Qb_A_B[i];
                Qb_B[i] = Qb_A[i];
            }
        }
        
        // Calculate the forward-backward ratio:
        float forwardBackwardRatio = 1.0f;
        for (int i = 0; i < iMcmcStepSize; ++i) {
            if (iSettings.usePairProposal) {
                if (isParticleAbFloating[i] || isParticleBbFloating[i] || isParticleAfloating[i]) {
                    forwardBackwardRatio *= (Qb_B[i] * Qb_A_B[i]) / (Q_A[i] * Q_B_A[i]);
                }
                else {
                    forwardBackwardRatio *= (Qb_A[i] * Qb_B_A[i] + Qb_B[i] * Qb_A_B[i]) / (Q_A[i] * Q_B_A[i] + Q_B[i] * Q_A_B[i]);
                }
            }
            else {
                if (isParticleAfloating[i]) {
                    // we distroy a floating particle, in the next iteration there will be one floating particle less, hence the probability
                    // in the x' to sample a floating particle is (note that both normalizers are in state x'):
                    float vPProposeAFloatInXb = iFloatingParticlesProposalNormalizer / (iFloatingParticlesProposalNormalizer + iTotalNormalizer);
                    forwardBackwardRatio *= 0.5f * (1 - vPProposeAFloatInXb) / probabilityOfFloatingParticle;
                    forwardBackwardRatio *= Qb_A[i] / Q_A[i];
                }
                else if (isParticleAbFloating[i]) {
                    // we create a floating particle, in the next iteration there will be one floating particle more, hence the probability
                    // in the x' to sample a floating particle is (note that iTotalNormalizer is updated to x'):
                    float vPProposeAFloatInXb = iFloatingParticlesProposalNormalizer / (iFloatingParticlesProposalNormalizer + iTotalNormalizer);
                    forwardBackwardRatio *= vPProposeAFloatInXb / (0.5f * (1 - probabilityOfFloatingParticle));
                    forwardBackwardRatio *= Qb_A[i] / Q_A[i];
                }
                else {
                    // Shrink and growth events have the same probability. We hence only need to compare the individual particle ratios.
                    forwardBackwardRatio *= Qb_A[i] / Q_A[i];
                }
            }
        }
        
        // Should I stay or should I go; the Metropolis-Hastings algorithm:
        float hastingsRatio = (float) Math.exp(-energyDiff / iMcmcTemperature) * forwardBackwardRatio;
        boolean accept = (hastingsRatio >= 1) ?
                          true :
                          (hastingsRatio > iRng.GetUniformVariate(0, 1)) ? true : false;
        
        // Register the result (if we accept) or rollback to the previous state.
        if (!accept || hardReject) {
            rejectParticles(appliedParticles, appliedParticleOrigLabels);
        }
        else {
            for (int i = 0; i < appliedParticles.size(); ++i) {
                storeResult(appliedParticles.get(i).iIndex, appliedParticleOrigLabels.get(i), iIterationCounter);
            }
        }

        return accept;
    }
    
    /**
     * Init Edge distribution only if needed (off-boundary probability > 0)
     */
    private void initEdgeDistribution() {
        if (iSettings.offBoundarySampleProbability > 0) {
            iEdgeImageDistr = generateDiscreteDistribution(iEdgeImage, iDistrRng);
        }
    }
    
    /**
     * Recover label image / particle set / statistics by going backward in history and reverting changes.
     * @param aAppliedParticles
     * @param aOrigLabels
     */
    private void rejectParticles(ParticleSet aAppliedParticles, ArrayList<Integer> aOrigLabels) {
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
                insertFloatingParticle(phe.particle, false);
            }
        }

        // recover the label image:
        for (int i = iLabelImageHistory.size() - 1; i >= 0; --i) {
            LabelImageHistoryEvent lihe = iLabelImageHistory.get(i);
            iLabelImage.setLabel(lihe.index, lihe.label);
        }

        for (int i = aOrigLabels.size() - 1; i >= 0; --i) {
            Particle p = aAppliedParticles.get(i);
            LabelStatisticToolbox.updateLabelStatistics(iIntensityImage.get(p.iIndex), p.iCandidateLabel, aOrigLabels.get(i), iLabelStatistics);
        }
    }
    
    /**
     * Stores new mcmc result for candidateIndex
     */
    private void storeResult(int aCandidateIndex, int aLabelBefore, int aIteration) {
        List<McmcResult> resultsForIndex = iMcmcResults.get(aCandidateIndex);
        if (resultsForIndex == null) {
            resultsForIndex = new ArrayList<>();
            iMcmcResults.put(aCandidateIndex, resultsForIndex);
        }
        resultsForIndex.add(new McmcResult(aIteration, aLabelBefore));
    }

    /**
     * @return energy difference when provided particle would change from current to aToLabel
     */
    private float calculateEnergyDifference(int aIndex, int aCurrentLabel, int aToLabel, float aImgValue) {
        ContourParticle contourCandidate = new ContourParticle(aCurrentLabel, aImgValue);
        EnergyResult res = iImageModel.calculateDeltaEnergy(iLabelImage.indexToPoint(aIndex), contourCandidate, aToLabel, iLabelStatistics);

        return res.energyDifference.floatValue();
    }
    
    /**
     * Algorithm 7 from Cardinale2013 thesis.
     */
    private void applyParticle(Particle aCandidateParticle, boolean aDoSimulate) {
        // Maintain the regular particle container and the label image
        addAndRemoveParticlesWhenMove(aCandidateParticle);

        // Remember old value for reverting if needed
        storeLabelImageHistory(aCandidateParticle.iIndex, iLabelImage.getLabel(aCandidateParticle.iIndex));

        // Set new value
        int fromLabelOldValue = iLabelImage.getLabelAbs(aCandidateParticle.iIndex);
        // Update the label image. The new point is either a contour particle or 0, therefore the negative label value is set.
        int sign = -1; // standard sign of the label image of boundary particles
        if (iLabelImage.isEnclosedByLabelBgConnectivity(aCandidateParticle.iIndex, aCandidateParticle.iCandidateLabel)) {
            // we created a floating particle
            sign = 1;
        }
        iLabelImage.setLabel(aCandidateParticle.iIndex, sign * aCandidateParticle.iCandidateLabel);

        // Update the statistics of the propagating and the loser region.
        if (!aDoSimulate) {
            LabelStatisticToolbox.updateLabelStatistics(iIntensityImage.get(aCandidateParticle.iIndex), fromLabelOldValue, aCandidateParticle.iCandidateLabel, iLabelStatistics);
        }

        // Update the proposals for all particles in the neighborhood (as they might have changed).
        if (iSettings.useBiasedProposal || (!iSettings.allowFission || !iSettings.allowHandles)) {
            updateProposalsAndFilterTopologyInNeighborhood(aCandidateParticle);
        }
    }
    
    /** 
     * Updates the label image and the regular particle containers when applying a particle. Note that floating particles will not be updated!
     * The method only ensures that L and the regular particles are correct. The method expects the label image NOT to be updated already. 
     */
    private void addAndRemoveParticlesWhenMove(Particle aCandidateParticle) {
        int candidateIndex = aCandidateParticle.iIndex;
        int labelTo = aCandidateParticle.iCandidateLabel;
        int origAbsLabelFrom = iLabelImage.getLabelAbs(candidateIndex);

        // We remove the particle and insert the reverse particle: we cannot decide if the reverse particle is indeed.
        // The solution is that this method only works on the regular particle set. Floating particles will be detected and treated outside of
        // this method. Here we omit the potential insertion of floating particles.
        // In order to (maybe) replace the particle with its reverse particle we: Simulate the move, calculate the proposal for the backward particle,
        // create the backward particle, check if the backward particle is floating, and finally restore the label image:
        int savedOrigLabel = iLabelImage.getLabel(candidateIndex);
        iLabelImage.setLabel(candidateIndex, -labelTo);
        Particle reverseParticle = new Particle(candidateIndex, origAbsLabelFrom, calculateProposal(candidateIndex));
        boolean reverseParticleIsFloating = isParticleFloating(reverseParticle.iIndex, reverseParticle.iCandidateLabel);
        iLabelImage.setLabel(candidateIndex, savedOrigLabel); //restore
        if (!reverseParticleIsFloating) {
            insertCandidatesToContainers(reverseParticle, labelTo, true);
        }

        // erase the currently applied particle (if its floating this will not hurt to call erase here).
        eraseCandidatesFromContainers(aCandidateParticle, origAbsLabelFrom, true);
        
        // Since we are storing the parents separately for each region, the following patch is needed. 
        // We need to shift the parent particle into the parents container of each others region. (see getRegularParticles for more info).
        if (origAbsLabelFrom != LabelImage.BGLabel && labelTo != LabelImage.BGLabel) {
            Particle p = new Particle(candidateIndex, LabelImage.BGLabel, calculateProposal(candidateIndex));
            eraseCandidatesFromContainers(p, origAbsLabelFrom, true);
            insertCandidatesToContainers(p, labelTo, true);
        }

        // What particle would be added or removed to the contour lists of the currently shrinking region.
        // FG region is shrinking:
        if (origAbsLabelFrom != 0) { 
            for (int offset : iBgNeighborsIndices) {
                int index = candidateIndex + offset;
                int label = iLabelImage.getLabel(index);
                // Check if new points enter the contour (internal point becomes a parent). Internal points of this region are positive:
                if (label > 0 && label == origAbsLabelFrom) {
                    boolean notExisted = insertCandidatesToContainers(new Particle(index, 0, calculateProposal(index)), origAbsLabelFrom, true);
                    if (notExisted) {
                        iLabelImage.setLabel(index, -origAbsLabelFrom);
                        storeLabelImageHistory(index, label);
                    }
                }
            }

            for (int i = 0; i < iFgNeighborsOffsets.length; ++i) {
                int index = candidateIndex + iFgNeighborsIndices[i];
                int label = iLabelImage.getLabel(index);
                if (iLabelImage.isBorderLabel(label)) continue;

                // check if there are FG-neighbors with no other mother of the same label --> orphan. we first 'simulate' the move:
                int savedOriginalLabel = iLabelImage.getLabel(candidateIndex);
                iLabelImage.setLabel(candidateIndex, -labelTo);
                if (Math.abs(label) != origAbsLabelFrom) {
                    // check if this neighbor has other mothers from this label:
                    boolean hasOtherMother = false;
                    for (Point offset : iFgNeighborsOffsets) {
                        int label2 = iLabelImage.getLabelAbs(candidateIndex + iLabelImage.pointToIndex(offset.add(iFgNeighborsOffsets[i])));
                        if (label2 == origAbsLabelFrom) {
                            hasOtherMother = true;
                            break;
                        }
                    }
                    if (!hasOtherMother) {
                        // The orphin has label equal to what we read from the label image and has a candidate label of the currently shrinking region.
                        // The proposal is not important; we delete the value
                        eraseCandidatesFromContainers(new Particle(index, origAbsLabelFrom, 0), Math.abs(label), true);
                    }
                }
                iLabelImage.setLabel(candidateIndex, savedOriginalLabel); // restore
            }
        }

        // Growing: figure out the changes of candidates for the expanding region
        if (labelTo != 0) { // we are growing
            // Neighbors: Figure out what (neighboring)mother points are going to be interior points: simulate the move
            int savedOriginalLabel = iLabelImage.getLabel(candidateIndex);
            iLabelImage.setLabel(candidateIndex, -labelTo);
            for (int offset : iBgNeighborsIndices) {
                int index = candidateIndex + offset;
                int label = iLabelImage.getLabel(index);
                if (label == -labelTo && iLabelImage.isEnclosedByLabelBgConnectivity(index, labelTo)) {
                    // Remove the parent that got enclosed; it had a the label of the currently expanding region and a candidate label of 0.
                    eraseCandidatesFromContainers(new Particle(index, 0, 0), labelTo, true);
                    
                    // update the label image (we're not using the update mechanism of the optimizer anymore):
                    iLabelImage.setLabel(index, Math.abs(label));
                    storeLabelImageHistory(index, label);
                }
            }
            iLabelImage.setLabel(candidateIndex, savedOriginalLabel); // restore

            // Figure out if a point renders to a candidate. These are all the FG-neighbors with a different label that are not yet
            // candidates of the currently expanding region.
            for (int i = 0; i < iFgNeighborsOffsets.length; ++i) {
                int index = candidateIndex + iFgNeighborsIndices[i];
                int label = iLabelImage.getLabel(index);
                int absLabel = Math.abs(label);
                if (absLabel != labelTo && !iLabelImage.isBorderLabel(absLabel)) {
                    // check if there is no other mother (hence the particle is not in the container yet). This we could do by checking the
                    // neighborhood of the label image or by checking the containers.
                    // Here: we check the (not yet updated!) label image.
                    boolean hasOtherMother = false;
                    for (Point vOff2 : iFgNeighborsOffsets) {
                        int label2 = iLabelImage.getLabelAbs(candidateIndex + iLabelImage.pointToIndex(vOff2.add(iFgNeighborsOffsets[i])));
                        if (label2 == labelTo) {
                            hasOtherMother = true;
                            break;
                        }
                    }
                    if (!hasOtherMother) {
                        // This is a new child. It's current label we have to read from the label image, the candidate label is the label of the currently expanding region.
                        boolean notExisted = insertCandidatesToContainers(new Particle(index, labelTo, calculateProposal(index)), absLabel, true);
                        if (notExisted) {
                            iLabelImage.setLabel(index, -absLabel);
                            storeLabelImageHistory(index, label);
                        }
                    }
                }
            }
        }
    }

    
    /**
     * @return true if particle is floating
     */
    private boolean isParticleFloating(int aParticleIndex, int aCandidateLabel) {
        if (aCandidateLabel > 0) {
            // This is a child. It is floating if there is no mother, i.e. if there is no corresponding region in the FG neighborhood.
            return iLabelImage.isSingleFgPoint(aParticleIndex, aCandidateLabel);
        }
        // else: this is a potential parent (candidate label is 0). According to the BG connectivity it fulfills the floating property
        // only if there is no other region in the BG neighborhood. Otherwise this pixel might well go to the BG label without changing the topo.
        return iLabelImage.isEnclosedByLabelBgConnectivity(aParticleIndex, iLabelImage.getLabelAbs(aParticleIndex));
    }
    
    /**
     * @return proposal normalizer for given label(s)
     */
    private float getProposalNormalizer(int aCurrentLabel, int aCandidateLabel) {
        return (aCandidateLabel == 0) ? 
                    iParentsProposalNormalizer.get(aCurrentLabel) : 
                    iChildrenProposalNormalizer.get(aCandidateLabel);
    }

    /**
     * @return true if aParticle is regular particle
     */
    private boolean isRegularParticle(Particle aParticle, int aCurrentLabel) {
        return (aParticle.iCandidateLabel == LabelImage.BGLabel) ? 
                    iParents.get(aCurrentLabel).contains(aParticle) : 
                    iChildren.get(aParticle.iCandidateLabel).contains(aParticle);
    }
    
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
    private void updateProposalsAndFilterTopologyInNeighborhood(Particle aParticle) {
        for (int i = -1; i < iBgNeighborsIndices.length; ++i) {
            // for -1 use 0 offset (position of particle itself)
            int offset = (i >= 0) ? iBgNeighborsIndices[i] : 0;

            for (Particle particle : getRegularParticles(aParticle.iIndex + offset, new ParticleSet())) {
                int label = (particle.iCandidateLabel == 0) ? iLabelImage.getLabelAbs(particle.iIndex) : particle.iCandidateLabel;

                if (isParticleTopoValid(particle)) {
                    // replace the particle with the new proposal
                    particle.iProposal = calculateProposal(particle.iIndex);
                    insertCandidatesToContainers(particle, label, true);
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
    private void topologyFiltering(ParticleSet aParticleSet) {
        // Intentionally from back since then indexes are not invalidated in particle set
        for (int i = aParticleSet.size() - 1; i >= 0; --i) {
            Particle p = aParticleSet.get(i);
            if (!isParticleTopoValid(p)) {
                aParticleSet.erase(p);
            }
        }
    }
    
    /**
     * @param aParticle
     * @return false if topology is changed when applying aParticle. This is based on the current state of the label image.
     */
    private boolean isParticleTopoValid(Particle aParticle) {
        if (!iSettings.allowFission || !iSettings.allowHandles) {
            
            // calculate the topo numbers for the current and the candidate label if they are != 0
            ArrayList<Integer> labelsToCheck = new ArrayList<>(2);
            if (aParticle.iCandidateLabel != 0) labelsToCheck.add(aParticle.iCandidateLabel);
            int particleLabel = iLabelImage.getLabelAbs(aParticle.iIndex);
            if (particleLabel != 0) labelsToCheck.add(particleLabel);
            
            // check if numbers are OK
            for (TopologicalNumberResult tnr : iTopoFunction.getTopologicalNumbers(iLabelImage.indexToPoint(aParticle.iIndex), labelsToCheck)) {
                // if tnr == null there is no same label around and it would create seperate new region 
                // if both FG and BG numbers are not 1 then changing label would seperate region
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
    private ParticleSet getRegularParticlesInBgNeighborhood(int aIndex) {
        ParticleSet aList = new ParticleSet();
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
    private ParticleSet getRegularParticlesInFgNeighborhood(int aIndex) {
        ParticleSet aList = new ParticleSet();
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
    private ParticleSet getPartnerParticles(Particle aParticle) {
        // Get all correct particles in BG neighborhood
        ParticleSet aSet = new ParticleSet();
        ParticleSet conditionalParticles = getRegularParticlesInBgNeighborhood(aParticle.iIndex);
        for (Particle p : conditionalParticles) {
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
     * Inserts to a list possible candidates for particle label at aIndex
     * For !BGLabel: it adds all different value labels in FG + mandatory BGLabel if anything in FG is found
     *               when all labels are same then it adds BGLabel only if in BG neighborhood
     * In case of BGLabel it adds all possible labels != BGLabel
     * @param aIndex - index of interest
     * @param aSet - set to be updated
     */
    private ParticleSet getRegularParticles(int aIndex, ParticleSet aSet) {
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
                    aSet.insert(new Particle(aIndex, absNeighborLabel, proposal));
                }
                if (!isParentInserted && currentLabel != LabelImage.BGLabel) {
                    // this is a non-background pixel with different neighbors, hence there must be a parent in the list
                    aSet.insert(new Particle(aIndex, 0, proposal));
                    isParentInserted = true;
                }
            }
        }

        // Check the BG neighborhood now if we need to insert a parent.
        if (!isParentInserted && currentLabel != LabelImage.BGLabel) {
            for (Integer idx : iLabelImage.iterateBgNeighbours(aIndex)) {
                if (iLabelImage.getLabelAbs(idx) != absCurrentLabel) {
                    // This is a FG pixel with a neighbor of a different label. Finally, insert a parent particle.
                    aSet.insert(new Particle(aIndex, 0, proposal));
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
     * Inserts candidate particle into children/parens contianer. 
     * If candidateLabel == 0 it goes to parent with currentLabel
     * otherwise to children with candidateLabel
     * @param aParticle - candidate particle
     * @param aCurrentLabel - current label of particle
     * @param aDoRecord - should record
     * @return true if new particle was added, false if replaced
     */
    private boolean insertCandidatesToContainers(Particle aParticle, int aCurrentLabel, boolean aDoRecord) {
        Map<Integer, ParticleSet> container = (aParticle.iCandidateLabel == 0) ? iParents : iChildren;
        Map<Integer, Float> containerNormalizer = (aParticle.iCandidateLabel == 0) ? iParentsProposalNormalizer : iChildrenProposalNormalizer;
        int label = (aParticle.iCandidateLabel == 0) ? aCurrentLabel : aParticle.iCandidateLabel;
        
        ParticleSet particles = container.get(label);
        if (particles == null) {
            particles = new ParticleSet();
            container.put(label, particles);
        }
        Particle replacedParticle = particles.insert(aParticle);

        float diff = (replacedParticle == null) ? aParticle.iProposal : aParticle.iProposal - replacedParticle.iProposal;
        containerNormalizer.replace(label, containerNormalizer.getOrDefault(label, 0f) + diff);
        iTotalNormalizer += diff;

        if (aDoRecord) {
            // Note that the order here is important: first insert the particle that gets replaced. 
            // When restoring the state afterwards, the particle history is iterated in reverse order.
            if (replacedParticle != null) {
                iParticlesHistory.add(new ParticleHistoryElement(replacedParticle, aCurrentLabel, false));
            }
            iParticlesHistory.add(new ParticleHistoryElement(aParticle, aCurrentLabel, true));
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
    private boolean eraseCandidatesFromContainers(Particle aParticle, int aCurrentLabel, boolean aDoRecord) {
        Map<Integer, ParticleSet> container = (aParticle.iCandidateLabel == 0) ? iParents : iChildren;
        Map<Integer, Float> containerNormalizer = (aParticle.iCandidateLabel == 0) ? iParentsProposalNormalizer : iChildrenProposalNormalizer;
        int label = (aParticle.iCandidateLabel == 0) ? aCurrentLabel : aParticle.iCandidateLabel;

        ParticleSet particles = container.get(label);
        Particle replacedParticle = particles.erase(aParticle);
        if (replacedParticle != null) {
            containerNormalizer.put(label, containerNormalizer.get(label) - replacedParticle.iProposal);
            iTotalNormalizer -= replacedParticle.iProposal;
        }

        if (replacedParticle != null && aDoRecord) {
            iParticlesHistory.add(new ParticleHistoryElement(replacedParticle, aCurrentLabel, false));
        }

        return replacedParticle != null;
    }
    
    /**
     * Removes floating particle. In case when proposals diff much then particle is only updated.
     * @param aParticle
     * @param aDoRecord
     */
    private void eraseFloatingParticle(Particle aParticle, boolean aDoRecord) {
        // get from container same a particle with same index/candidateLabel
        int index = iFloatingParticles.getIndex(aParticle);
        Particle updatedParticle = iFloatingParticles.get(index);
        
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
     * Insert a floating particle only if it didn't exist. 
     * @param aParticle
     * @param aDoRecord - save info in history container
     * @return If particle existed, the return value will be false.
     */
    private boolean insertFloatingParticle(Particle aParticle, boolean aDoRecord) {
        if (iFloatingParticles.contains(aParticle)) {
            // the particle already exists
            return false;
        }

        iFloatingParticlesProposalNormalizer += aParticle.iProposal;
        iFloatingParticles.insert(aParticle);

        if (aDoRecord) {
            iFloatingParticlesHistory.add(new ParticleHistoryElement(aParticle, 0, true));
        }

        return true;
    }
    /**
     * Sample off-boundary particle and inserts/removes it from floating particles 
     * depending on if should growth or not.
     * @param aGrowth - true for growth, false otherwise
     * @param aLabel - wanted label for particle
     * @return false for rejected sample, true otherwise
     */
    private boolean sampleOffBoundary(int aLabel) {
        // sample candidate label/location and growth/shrink action
        boolean shouldGrowth = iRng.GetUniformVariate(0, 1) < 0.5;
        int candidateLabel = (iRng.GetUniformVariate(0, 1) > 0.5) ? aLabel : 0;
        int particleIndex = sampleIndexFromEdgeDensity();
        
        Particle particle = new Particle(particleIndex, candidateLabel, 0);
        boolean particleExists = iFloatingParticles.contains(particle);
        
        if (!shouldGrowth && particleExists) {
            // Calculate fraction of proposal to be removed
            // NOTE: This part was not existing in C++ code and iProposal was always set to 0
            //       which was pointless since particle was not removed at all (there were no effect at all)
            Particle p = iFloatingParticles.get(iFloatingParticles.getIndex(particle));
            particle.iProposal = (float) (iRng.GetVariate() * p.iProposal);
            eraseFloatingParticle(particle, false);
        }
        else if (shouldGrowth && !particleExists) {
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
    public ImagePlus createProbabilityImage() {
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

            resultImg.addSliceToStack(img, "label_" + currentLabel, false);
        }
        
        resultImg.setImageTitle("Probability");
        
        return resultImg.getImage();
    }    
    
    /**
     * Generates distribution from input image.ds 
     * NOTICE: in case if values in input image are too small (< 10*eps(1)) it will provide flat distribution and will change all pixels of input image to 1.0
     * 
     * @param aImg input image to generate distribution from
     * @return generated distribution
     */
    static IndexedDiscreteDistribution generateDiscreteDistribution(IntensityImage aImg, Rng am_NumberGeneratorBoost) {
        double[] pmf = new double[aImg.getSize()];
        
        Iterator<Point> ri = new SpaceIterator(aImg.getDimensions()).getPointIterator();
        int index = 0;
        double sumOfPixelValues = 0;
        while (ri.hasNext()) {
            final Point point = ri.next();
            double value = aImg.get(point);
            sumOfPixelValues += value;
            pmf[index++] = value;
        }
        // if sum of all pixels is too small (< 10 * epislon(1f) as in original code) then just use flat distribution
        if (sumOfPixelValues < 10 * Math.ulp(1.0f)) {
            sumOfPixelValues = aImg.getSize();
            index = 0;
            ri = new SpaceIterator(aImg.getDimensions()).getPointIterator();
            while (ri.hasNext()) {
                final Point point = ri.next();
                aImg.set(point, 1.0f);
                pmf[index] = 1.0;
                ++index;
            }
        }

        return new IndexedDiscreteDistribution(am_NumberGeneratorBoost, pmf);
    }
    
    private IntensityImage generateEdgeImage(IntensityImage aImage) {
        IntensityImage img = new IntensityImage(aImage);
        img.normalize();
        
        Convolver imgConvolver = new Convolver(img.getWidth(), img.getHeight(), img.getDepth() /*depth*/);
        imgConvolver.initFromIntensityImage(img);
        // Parameters of Gaussian kernel same as in C-code
        Kernel1D gauss = new Gauss1D(1.5, 7);
        imgConvolver.x1D(gauss);
        imgConvolver.y1D(gauss);
        if (img.getDepth() > 1) { //3D
            imgConvolver.z1D(gauss); // run Gaussian blur also in z-direction
            imgConvolver.sobel3D();
        }
        else {
            imgConvolver.sobel2D();
        }
        
        imgConvolver.getIntensityImage(img);
        return img;
    }
}
