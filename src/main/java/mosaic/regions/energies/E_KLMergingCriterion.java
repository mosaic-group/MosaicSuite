package mosaic.regions.energies;


import java.util.HashMap;

import mosaic.core.imageUtils.Point;
import mosaic.regions.RC.ContourParticle;
import mosaic.regions.energies.Energy.ExternalEnergy;
import mosaic.regions.utils.LabelStatistics;


public class E_KLMergingCriterion extends ExternalEnergy {

    private final float m_RegionMergingThreshold;
    private final int bgLabel;

    public E_KLMergingCriterion(int bgLabel, float m_RegionMergingThreshold) {
        this.bgLabel = bgLabel;
        this.m_RegionMergingThreshold = m_RegionMergingThreshold;
    }

    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int fromLabel = contourParticle.label;
        final boolean merge = CalculateMergingEnergyForLabel(fromLabel, toLabel, labelMap);
        // TODO: Is this null here OK? This result might be later used in computations.
        return new EnergyResult(null, merge);
    }

    private boolean CalculateMergingEnergyForLabel(int aLabelA, int aLabelB, HashMap<Integer, LabelStatistics> labelMap) {
        // store this event to check afterwards if we should merge the 2 regions.
        if (aLabelA != bgLabel && aLabelB != bgLabel) // we are competing.
        {
            // test if merge should be performed:
            final double value = CalculateKLMergingCriterion(aLabelA, aLabelB, labelMap);
            if (value < m_RegionMergingThreshold) {
                return true;
            }
        }
        return false;
    }

    private double CalculateKLMergingCriterion(int L1, int L2, HashMap<Integer, LabelStatistics> labelMap) {
        final LabelStatistics aL1 = labelMap.get(L1);
        final LabelStatistics aL2 = labelMap.get(L2);

        final double vMu1 = aL1.iMeanIntensity;
        final double vMu2 = aL2.iMeanIntensity;
        final double vVar1 = aL1.iVarIntensity;
        final double vVar2 = aL2.iVarIntensity;
        final int vN1 = aL1.iLabelCount;
        final int vN2 = aL2.iLabelCount;

        return calc(vMu1, vMu2, vVar1, vVar2, vN1, vN2);
    }

    static double calc(double aMu1, double aMu2, double aVar1, double aVar2, int aN1, int aN2) {
        final double vMu12 = (aN1 * aMu1 + aN2 * aMu2) / (aN1 + aN2);
        final double vSumOfSq1 = aVar1 * (aN1 - 1) + aN1 * aMu1 * aMu1;
        final double vSumOfSq2 = aVar2 * (aN2 - 1) + aN2 * aMu2 * aMu2;

        final double vVar12 = (1.0 / (aN1 + aN2 - 1.0)) * (vSumOfSq1 + vSumOfSq2 - (aN1 + aN2) * vMu12 * vMu12);

        if (vVar12 <= 0) {
            return 0;
        }
        if (aVar1 < 0) {
            aVar1 = 0;
        }
        if (aVar2 < 0) {
            aVar2 = 0;
        }
        final double vDKL1 = (aMu1 - vMu12) * (aMu1 - vMu12) / (2.0 * vVar12) + 0.5 * (aVar1 / vVar12 - 1.0 - Math.log(aVar1 / vVar12));
        final double vDKL2 = (aMu2 - vMu12) * (aMu2 - vMu12) / (2.0 * vVar12) + 0.5 * (aVar2 / vVar12 - 1.0 - Math.log(aVar2 / vVar12));
        // TODO: Is this working? result=Infinity in cases that were checked (including tests) but it is not 
        // recognized by isNan below...
        //        double R = (aMu1 - vMu12) * (aMu1 - vMu12) *(aMu2 - vMu12) * (aMu2 - vMu12) / (2.0 * vVar12) + 0.5 * ((aVar1 + aVar2) / vVar12 - 2.0 - Math.log(aVar2 * aVar1 / vVar12));
        double result = vDKL1 + vDKL2;
        if (Double.isNaN(result)) {
            // TODO: Example line from tests for later investigation (from DRS with maxima init on flower img) with settings:
            /*
             * [{
                  "offBoundarySampleProbability": 0.25,
                  "useBiasedProposal": false,
                  "usePairProposal": false,
                  "burnInFactor": 0.5,
                  "showLabelImage": false,
                  "saveLabelImage": false,
                  "showProbabilityImage": true,
                  "saveProbabilityImage": false,
                  "initType": "LocalMax",
                  "initBoxRatio": 0.4,
                  "initBubblesRadius": 10,
                  "initBubblesDisplacement": 100,
                  "initLocalMaxGaussBlurSigma": 2.0,
                  "initLocalMaxTolerance": 0.005,
                  "initLocalMaxMinimumRegionSize": 4,
                  "initLocalMaxBubblesRadius": 5,
                  "energyCurvatureMaskRadius": 6,
                  "energyPsGaussEnergyRadius": 8,
                  "energyPsBalloonForceCoeff": 0.0,
                  "energyFunctional": "e_PS",
                  "regularizationType": "Sphere_Regularization",
                  "energyContourLengthCoeff": 0.5,
                  "allowFusion": true,
                  "allowFission": true,
                  "allowHandles": false,
                  "maxNumOfIterations": 120000
                }] 
             */
            // [NaN problem] [0.5505446874433093] [0.03307758006469711] [0.005456976309865315] [7.239539005930498E-4] [0] [0]
            mosaic.utils.Debug.print("E_KLMergingCriterion.java: NaN problem", aMu1,  aMu2, aVar1, aVar2, aN1,  aN2);
            result = 0;
        }
        return result;
    }
}
