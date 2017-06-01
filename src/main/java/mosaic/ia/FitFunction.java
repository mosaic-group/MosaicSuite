package mosaic.ia;


import fr.inria.optimization.cmaes.fitness.AbstractObjectiveFunction;
import mosaic.ia.Potentials.Potential;
import mosaic.ia.Potentials.PotentialNoParam;
import mosaic.ia.Potentials.PotentialType;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;

/**
 * Fit function for CMA-ES minimization. Its value is l2 norm (squared) between nearest neighbor PDF
 * and observed NN distances PDF (calculated for given potential).
 */
class FitFunction extends AbstractObjectiveFunction {
    private final double[] iContextQtPdf;
    private final double[] iContextQtDistancesGrid;
    private final double[] iNearestNeighborDistancesXtoYPdf;
    private final double[] iNearestNeighborDistancesXtoY;
    private final Potential iPotential;

    private double[] iObservedModelFitPdPdf;
    private static final double MachineEpsilon = Math.ulp(1.0);
    
    public FitFunction(double[] aContextQtPdf, double[] aContextQtDistncesGrid, double[] aNearestNeighborDistancesXtoYPdf, double[] aNearestNeighborDistancesXtoY, Potential aPotential) {
        iContextQtPdf = aContextQtPdf;
        iContextQtDistancesGrid = aContextQtDistncesGrid;
        iNearestNeighborDistancesXtoYPdf = aNearestNeighborDistancesXtoYPdf;
        iNearestNeighborDistancesXtoY = aNearestNeighborDistancesXtoY;
        iPotential = aPotential;
    }
    
    public double[] getObservedModelFitPdPdf() {
        return iObservedModelFitPdPdf;
    }

    @Override
    public boolean isFeasible(double[] x) {
        if (iPotential.getType() == PotentialType.NONPARAM) {
            return true;
        }
        MinMaxMean mmmDistanceGrid = StatisticsUtils.getMinMaxMean(iContextQtDistancesGrid);
        MinMaxMean mmmNNDistances = StatisticsUtils.getMinMaxMean(iNearestNeighborDistancesXtoY);

        // Check if epsilon/strenght and threshold/scale have reasonable values to not overflow calculations.
        if (x[0] >= MachineEpsilon && x[0] <= 50 &&
                x[1] >= Math.max(Math.min(mmmDistanceGrid.min, mmmNNDistances.min), MachineEpsilon) && 
                x[1] <= Math.max(mmmDistanceGrid.max, mmmNNDistances.max)) 
        {
            // 50 is aribtrary. but log(Double.MAXVAL) = 709.782712893384
            return true;
        }
        return false;
    }

    @Override
    public double valueOf(double[] x) {
        if (iPotential.getType() == PotentialType.NONPARAM) {
            return l2Norm(x) + nonParamPenalty(x, ((PotentialNoParam)iPotential).getSmoothness());
        }
        
        return l2Norm(x);
    }
    
    public double l2Norm(double[] params) {
        double[] gibbsPotential = iPotential.calculate(iContextQtDistancesGrid, params).getGibbsPotential();
        final double Z = calculateNormalizationConstantZ(gibbsPotential);
        iObservedModelFitPdPdf = new double[iContextQtDistancesGrid.length];
        
        double value = 0;
        for (int i = 0; i < iContextQtDistancesGrid.length; i++) {
            iObservedModelFitPdPdf[i] = gibbsPotential[i] * iContextQtPdf[i] * (1 / Z);
            value += Math.pow((iObservedModelFitPdPdf[i] - iNearestNeighborDistancesXtoYPdf[i]), 2);
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
        final double[] support = new double[iContextQtDistancesGrid.length];
        for (int i = 0; i < iContextQtDistancesGrid.length; i++) {
            support[i] = aGibbsPotential[i] * iContextQtPdf[i];
        }
        
        double Z = 0;
        for (int i = 0; i < iContextQtDistancesGrid.length - 1; i++) {
            Z += (support[i] + support[i + 1]) / 2;
        }
        Z += support[0] / 2 + support[iContextQtDistancesGrid.length - 1] / 2;

        return Z;
    }
}
