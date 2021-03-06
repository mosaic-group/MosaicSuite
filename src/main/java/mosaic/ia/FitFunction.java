package mosaic.ia;


import org.apache.log4j.Logger;

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
    private static final Logger logger = Logger.getLogger(FitFunction.class);
    
    private final double[] iContextQdPdf;
    private final double[] iContextQdDistancesGrid;
    private final double[] iNearestNeighborDistancesXtoYPdf;
    private final double[] iNearestNeighborDistancesXtoY;
    private final Potential iPotential;

    private double[] iObservedModelFitPdPdf;
    private static final double MachineEpsilon = Math.ulp(1.0);
    
    double iLowRange = 0;
    double iHighRange = 0;
    
    public FitFunction(double[] aContextQdPdf, double[] aContextQdDistncesGrid, double[] aNearestNeighborDistancesXtoYPdf, double[] aNearestNeighborDistancesXtoY, Potential aPotential) {
        iContextQdPdf = aContextQdPdf;
        iContextQdDistancesGrid = aContextQdDistncesGrid;
        iNearestNeighborDistancesXtoYPdf = aNearestNeighborDistancesXtoYPdf;
        iNearestNeighborDistancesXtoY = aNearestNeighborDistancesXtoY;
        iPotential = aPotential;
        
        MinMaxMean mmmDistanceGrid = StatisticsUtils.getMinMaxMean(iContextQdDistancesGrid);
        MinMaxMean mmmNNDistances = StatisticsUtils.getMinMaxMean(iNearestNeighborDistancesXtoY);
        iLowRange = Math.max(Math.min(mmmDistanceGrid.min, mmmNNDistances.min), MachineEpsilon);
        iHighRange = Math.max(mmmDistanceGrid.max, mmmNNDistances.max);
        logger.debug("Fit function range: " + iLowRange+ " <= x[1] <= " + iHighRange);
    }
    
    public double[] getObservedModelFitPdPdf() {
        return iObservedModelFitPdPdf;
    }

    @Override
    public boolean isFeasible(double[] x) {
        // NOPARAM is always OK
        if (iPotential.getType() == PotentialType.NONPARAM) {
            return true;
        }

        // Check if epsilon/strenght and threshold/scale have reasonable values to not overflow calculations.
        if (Math.abs(x[0]) > MachineEpsilon && x[1] >= iLowRange && x[1] <= iHighRange) {
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
        double[] gibbsPotential = iPotential.calculate(iContextQdDistancesGrid, params).getGibbsPotential();
        final double Z = calculateNormalizationConstantZ(gibbsPotential);
        iObservedModelFitPdPdf = new double[iContextQdDistancesGrid.length];
        
        double value = 0;
        for (int i = 0; i < iContextQdDistancesGrid.length; i++) {
            iObservedModelFitPdPdf[i] = gibbsPotential[i] * iContextQdPdf[i] * (1 / Z);
            // Sum squared errors
            value += Math.pow((iObservedModelFitPdPdf[i] - iNearestNeighborDistancesXtoYPdf[i]), 2);
        }
        
        return value;
    }
    
    private double calculateNormalizationConstantZ(double[] aGibbsPotential) {
        final double[] support = new double[iContextQdDistancesGrid.length];
        for (int i = 0; i < iContextQdDistancesGrid.length; i++) {
            support[i] = aGibbsPotential[i] * iContextQdPdf[i];
        }
        
        // Integrate
        double Z = 0;
        for (int i = 0; i < iContextQdDistancesGrid.length - 1; i++) {
            Z += (support[i] + support[i + 1]) / 2 * (iContextQdDistancesGrid[i+1] - iContextQdDistancesGrid[i]);
        }
        
        return Z;
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

}
