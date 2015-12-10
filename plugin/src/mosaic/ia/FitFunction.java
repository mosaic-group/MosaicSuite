package mosaic.ia;


import fr.inria.optimization.cmaes.fitness.AbstractObjectiveFunction;
import mosaic.ia.Potentials.Potential;
import mosaic.ia.Potentials.PotentialNoParam;
import mosaic.ia.Potentials.PotentialType;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;

/**
 * Fit function for CMA-ES minimalization. Its value is l2 norm (squared) between nearest neighbour PDF
 * and observed NN distances PDF (calculated for given potential).
 */
class FitFunction extends AbstractObjectiveFunction {
    private final double[] iNearestNeighborDistances;
    private final double[] iDistancesGrid;
    private final double[] iNearestNeighborDistancePdf;
    private final double[] iDistancesPdf;
    private final Potential iPotential;

    private double[] observedNNDistancesPdf;
    private static final double MachineEpsilon = Math.ulp(1.0);
    
    public FitFunction(double[] aDistncesGrid, double[] aDistancesPdf, double[] aNearestNeighborDistances, Potential aPotential, double[] aNearestNeighborDistancePdf) {
        iNearestNeighborDistances = aNearestNeighborDistances;
        iDistancesGrid = aDistncesGrid;
        iNearestNeighborDistancePdf = aNearestNeighborDistancePdf;
        iDistancesPdf = aDistancesPdf;
        iPotential = aPotential;
    }
    
    public double[] getObservedNearestNeighborDistancesPdf() {
        return observedNNDistancesPdf;
    }

    @Override
    public boolean isFeasible(double[] x) {
        if (iPotential.getType() == PotentialType.NONPARAM) {
            return true;
        }
        else {
            MinMaxMean mmmDistanceGrid = StatisticsUtils.getMinMaxMean(iDistancesGrid);
            MinMaxMean mmmNNDistances = StatisticsUtils.getMinMaxMean(iNearestNeighborDistances);
            
            // Check if epsilon/strenght and threshold/scale have reasonable values to not overflow calculations.
            if (x[0] >= MachineEpsilon && x[0] <= 50 && 
                x[1] >= Math.max(Math.min(mmmDistanceGrid.min, mmmNNDistances.min), MachineEpsilon) && 
                x[1] <= Math.max(mmmDistanceGrid.max, mmmNNDistances.max)) 
            {
                // 50 is aribtrary. but log(Double.MAXVAL)= log((2-(2^-52))*(2^1023))= 709.7827
                return true;
            }
            else {
                return false;
            }
        }
    }
    
    @Override
    public double valueOf(double[] x) {
        if (iPotential.getType() == PotentialType.NONPARAM) {
            return l2Norm(x) + nonParamPenalty(x, ((PotentialNoParam)iPotential).getSmoothness());
        }
        
        return l2Norm(x);
    }
    
    public double l2Norm(double[] params) {
        double[] gibbsPotential = iPotential.calculate(iDistancesGrid, params).getGibbsPotential();
        final double Z = calculateNormalizationConstantZ(gibbsPotential);
        observedNNDistancesPdf = new double[iDistancesGrid.length];
        
        double value = 0;
        for (int i = 0; i < iDistancesGrid.length; i++) {
            observedNNDistancesPdf[i] = gibbsPotential[i] * iDistancesPdf[i] * (1 / Z);
            value += Math.pow((observedNNDistancesPdf[i] - iNearestNeighborDistancePdf[i]), 2);
        }
        
        return value;
    }
    
    private double nonParamPenalty(double[] x, double aSmoothness) {
        double sum = 0;
        for (int i = 0; i < x.length - 1; i++) {
            sum += Math.pow( (x[i] - x[i + 1]), 2);
        }
        // This is from original implementation where one of sum elements was (x[last] - 0)^2 - reason unknown
        sum += Math.pow(x[x.length - 1], 2);
        
        return sum * Math.pow(aSmoothness, 2);
    }

    private double calculateNormalizationConstantZ(double[] aGibbsPotential) {
        final double[] support = new double[iDistancesGrid.length];
        for (int i = 0; i < iDistancesGrid.length; i++) {
            support[i] = aGibbsPotential[i] * iDistancesPdf[i];
        }
        
        double Z = 0;
        for (int i = 0; i < iDistancesGrid.length - 1; i++) {
            Z += (support[i] + support[i + 1]) / 2;
        }
        Z += support[0] / 2 + support[iDistancesGrid.length - 1] / 2;

        return Z;
    }
}
