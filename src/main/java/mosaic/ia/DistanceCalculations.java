package mosaic.ia;


import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.scijava.vecmath.Point3d;

import mosaic.ia.gui.Utils;
import mosaic.utils.Debug;
import mosaic.utils.math.NearestNeighborTree;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;
import weka.estimators.KernelEstimator;


public abstract class DistanceCalculations {
    private static final Logger logger = Logger.getLogger(DistanceCalculations.class);
    
    protected static final int NumberOfDistPoints = 1000;
    
    // These guys should be set in derived classes
    protected Point3d[] iParticlesX;
    protected Point3d[] iParticlesY;
    protected double iXscale = 1;
    protected double iYscale = 1;
    protected double iZscale = 1;
    
    // Input parameters
    private final double iGridSpacing;
    private final double iKernelWeightQ;
    private final double iKernelWeightP;
    private final int iNumberOfDistPoints;
    private final float[][][] iMaskImage3d; //[z][x][y]

    // Internal data structures
    private double[] iContextQdPdf;
    private double[] iContextQdDistancesGrid;
    private double[] iNearestNeighborsDistancesXtoY;
    private double[] iNearestNeighborsDistancesXtoYPdf;
    private double iMinXtoYdistance, iMaxXtoYdistance, iMeanXtoYdistance;
    
    DistanceCalculations(float[][][] aMaskImage3d, double aGridSpacing, double aKernelWeightQ, double aKernelWeightP, int aNumberOfDistPoints) {
        logger.debug("aMaskImage3d(z/x/y): " + Debug.getArrayDims(aMaskImage3d) + " aGridSpacing: " + aGridSpacing + " aKernelWeightQ: " + aKernelWeightQ + " aKernelWeightP: " + aKernelWeightP + " aNumberOfDistPoints: " + aNumberOfDistPoints);
        iMaskImage3d = aMaskImage3d;
        iGridSpacing = aGridSpacing;
        iKernelWeightQ = aKernelWeightQ;
        iKernelWeightP = aKernelWeightP;
        iNumberOfDistPoints = aNumberOfDistPoints;
    }
    
    public double[] getContextQdPdf() {
        return iContextQdPdf;
    }
    
    public double[] getContextQdDistancesGrid() {
        return iContextQdDistancesGrid;
    }

    public double[] getNearestNeighborsDistancesXtoY() {
        return iNearestNeighborsDistancesXtoY;
    }

    public double[] getNearestNeighborsDistancesXtoYPdf() {
        return iNearestNeighborsDistancesXtoYPdf;
    }
    
    public double getMinXtoYdistance() {
        return iMinXtoYdistance;
    }

    public double getMaxXtoYdistance() {
        return iMaxXtoYdistance;
    }
    
    public double getMeanXtoYdistance() {
        return iMeanXtoYdistance;
    }
    
    /**
     * Calculates the relative frequency of possible distances (state density)
     */
    protected void stateDensity(double aMinX, double aMaxX, double aMinY, double aMaxY, double aMinZ, double aMaxZ) {
        logger.debug("Number of points (X/Y): " + iParticlesX.length + " / " + iParticlesY.length);
        logger.debug("min/max of x: " + aMinX + "/" + aMaxX + " y: " + aMinY + "/" + aMaxY + " z: " + aMinZ + "/" + aMaxZ);
        if (iParticlesX.length == 0 || iParticlesY.length == 0) {
            Utils.messageDialog("IA - state density", "Number of discovered particles in must be greater than 0. \nNumber of particles in image X/Y: " + iParticlesX.length + "/" + iParticlesY.length);
            throw new RuntimeException("Not enough particles to perform calculations!");
        }
        
        // ----------------- Calculate grid data for Q(d)
        final int xGridSize = (int) Math.floor(Math.abs(aMinX - aMaxX) * iXscale / iGridSpacing) + 1;
        final int yGridSize = (int) Math.floor(Math.abs(aMinY - aMaxY) * iYscale / iGridSpacing) + 1;
        final int zGridSize = (int) Math.floor(Math.abs(aMinZ - aMaxZ) * iZscale / iGridSpacing) + 1;
        logger.debug("Number of grid points (x/y/z): " + xGridSize + " / " + yGridSize + " / " + zGridSize);

        // ----------------- Calculate context Q(d)
        final NearestNeighborTree nearestNeighbor = new NearestNeighborTree(iParticlesY);
        final KernelEstimator kernelEstimator = new KernelEstimator(0.01 /* precision */);
        double maxDist = -Double.MAX_VALUE;
        double minDist = Double.MAX_VALUE;
        
        Point3d position = new Point3d();
        position.x = aMinX;
        for (int i = 0; i < xGridSize; ++i) {
            position.y = aMinY;
            for (int j = 0; j < yGridSize; ++j) {
                position.z = aMinZ;
                for (int k = 0; k < zGridSize; ++k) {
                    // Skip points from outside of mask (if provided)
                    if (iMaskImage3d != null && !isInsideMask(position)) continue;

                    double distance = nearestNeighbor.getDistanceToNearestNeighbor(position);
                    kernelEstimator.addValue(distance, iKernelWeightQ);
                    if (distance > maxDist) maxDist = distance;
                    if (distance < minDist) minDist = distance;

                    position.z += iGridSpacing;
                }
                position.y += iGridSpacing;
            }
            position.x += iGridSpacing;
        }
        logger.debug("Min-Max distance in context q(d): " + minDist + " - " + maxDist);
        
        // ----------------- Calculate observed distribution X to Y
        iNearestNeighborsDistancesXtoY = nearestNeighbor.getDistancesToNearestNeighbors(iParticlesX);
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(iNearestNeighborsDistancesXtoY);
        iMinXtoYdistance = mmm.min;                         
        iMaxXtoYdistance = mmm.max;
        iMeanXtoYdistance = mmm.mean;
        logger.debug("Min/Max/Mean X to Y distance: " + iMinXtoYdistance + " / " + iMaxXtoYdistance + " / " + iMeanXtoYdistance);
        
        // ----------------- Calculate distances grid.
        // Extend min/max distance to match both - context and NN distances
        maxDist = Math.max(maxDist, iMaxXtoYdistance);
        minDist = Math.min(minDist, iMinXtoYdistance);
        double binLength = (maxDist - minDist) / (iNumberOfDistPoints - 1);
        logger.debug("Grid min/max/binLength: " + minDist + " / " + maxDist + " / " + binLength);
        
        iContextQdDistancesGrid = new double[iNumberOfDistPoints];
        for (int i = 0; i < iNumberOfDistPoints; ++i) {
            iContextQdDistancesGrid[i] = i * binLength + minDist;
        }

        // ----------------- Calculate X to Y PDF 
        final KernelEstimator kernelXtoY = new KernelEstimator(0.01 /*precision*/);
        for (double value : iNearestNeighborsDistancesXtoY) kernelXtoY.addValue(value, iKernelWeightP); 
        iNearestNeighborsDistancesXtoYPdf = new double[iContextQdDistancesGrid.length];
        for (int i = 0; i < iContextQdDistancesGrid.length; i++) {
            iNearestNeighborsDistancesXtoYPdf[i] = kernelXtoY.getProbability(iContextQdDistancesGrid[i]);
        }
        
        // ----------------- Calculate context Q(d) PDF
        iContextQdPdf = new double[iNumberOfDistPoints];
        for (int i = 0; i < iNumberOfDistPoints; ++i) {
            iContextQdPdf[i] = kernelEstimator.getProbability(iContextQdDistancesGrid[i]);
        }
    }

    private boolean isInsideMask(Point3d coords) {
        try {
            if (iMaskImage3d[(int) coords.z][(int) coords.x][(int) coords.y] > 0) {
                return true;
            }
        }
        catch (final ArrayIndexOutOfBoundsException e) {
            // It is OK to be here: may happen if mask is smaller than input data (image or loaded coordinates).
            // In that case we discard points outside mask.
        }

        return false;
    }
    
    protected Point3d[] getFilteredAndScaledCoordinates(Point3d[] points) {
        final ArrayList<Point3d> vectorPoints = new ArrayList<>(points.length);
        for (Point3d p : points) {
            if (iMaskImage3d == null || isInsideMask(p)) {
                vectorPoints.add(new Point3d(p.x * iXscale, p.y * iYscale, p.z * iZscale));
            }
        }
        return vectorPoints.toArray(new Point3d[0]);
    }
}
