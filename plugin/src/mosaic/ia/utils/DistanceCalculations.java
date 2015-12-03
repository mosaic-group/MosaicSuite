package mosaic.ia.utils;


import java.util.Vector;

import javax.vecmath.Point3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import weka.estimators.KernelEstimator;


public abstract class DistanceCalculations {

    // Input parameters
    private final double iDeltaStepLenght;
    private final double iKernelWeight;
    private final int iNumberOfBins;
    private final float[][][] maskImage3d;

    // These guys should be set in derived classes
    protected Point3d[] iparticlesX;
    protected Point3d[] iParticlesY;
    protected double zscale = 1;
    protected double xscale = 1;
    protected double yscale = 1;

    // Internal data structures
    private double[] iDistances;
    private double[] iDistancesDistribution;
    private double[] iProbabilityDistribution;

    DistanceCalculations(ImagePlus mask, double aDeltaStepLength, double aKernelWeight, int aNumberOfBins) {
        iDeltaStepLenght = aDeltaStepLength;
        iKernelWeight = aKernelWeight;
        iNumberOfBins = aNumberOfBins;
        maskImage3d = mask != null ? imageTo3Darray(mask) : null;
    }
    
    public double[] getProbabilityDistribution() {
        return iProbabilityDistribution;
    }
    
    public double[] getDistancesDistribution() {
        return iDistancesDistribution;
    }

    public double[] getDistancesOfX() {
        return iDistances;
    }

    protected void stateDensity(double x1, double y1, double z1, double x2, double y2, double z2) {
        final int x_size = (int) Math.floor(Math.abs(x1 - x2) * xscale / iDeltaStepLenght) + 1;
        final int y_size = (int) Math.floor(Math.abs(y1 - y2) * yscale / iDeltaStepLenght) + 1;
        final int z_size = (int) Math.floor(Math.abs(z1 - z2) * zscale / iDeltaStepLenght) + 1;

        System.out.println("Number of grid points (x/y/z): " + x_size + " / " + y_size + " / " + z_size);

        final KDTreeNearestNeighbor nearestNeighbor = new KDTreeNearestNeighbor(iParticlesY);
        final KernelEstimator kernelEstimator = new KernelEstimator(0.01 /* precision */);
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        Point3d position = new Point3d();
        
        for (int i = 0; i < x_size; i++) {
            for (int j = 0; j < y_size; j++) {
                for (int k = 0; k < z_size; k++) {
                    try {
                        position.x = x1 + i * iDeltaStepLenght;
                        position.y = y1 + j * iDeltaStepLenght;
                        position.z = z1 + k * iDeltaStepLenght;
                        double distance = nearestNeighbor.getNearestNeighborDistance(position);
                        kernelEstimator.addValue(distance, iKernelWeight);
                        if (distance > max) max = distance;
                        if (distance < min) min = distance;
                    }
                    catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        final double binLength = (max - min) / iNumberOfBins;
        System.out.println("Maximum/Minimum distance found: " + max + " " + min);
        System.out.println("Bin lenght: " + binLength);

        iDistancesDistribution = new double[iNumberOfBins];
        iProbabilityDistribution = new double[iNumberOfBins];
        for (int i = 0; i < iNumberOfBins; i++) {
            iDistancesDistribution[i] = i * binLength;
            iProbabilityDistribution[i] = kernelEstimator.getProbability(i * binLength);
        }
    }

    protected void calcDistancesOfX() {
        System.out.println("Number of coordinates (X/Y): " + iparticlesX.length + " / " + iParticlesY.length);
        iDistances = new KDTreeNearestNeighbor(iParticlesY).getNearestNeighborDistances(iparticlesX);
    }

    private boolean isInsideMask(double[] coords) {
        try {
            if (maskImage3d[(int) Math.floor(coords[2])][(int) Math.floor(coords[1])][(int) Math.floor(coords[0])] > 0) {
                return true;
            }
        }
        catch (final ArrayIndexOutOfBoundsException e) {
            // May happen if mask is applied to loaded coordinates. In that case checks are not done.
            return false;
        }

        return false;
    }
    
    protected Point3d[] getFilteredViaMaskCoordinates(Point3d[] points) {
        if (maskImage3d == null) {
            return points;
        }
        final Vector<Point3d> vectorPoints = new Vector<Point3d>();
        final double[] coords = new double[3];
        for (Point3d p : points) {
            p.get(coords);
            if (isInsideMask(coords)) {
                vectorPoints.add(new Point3d(coords[0] * xscale, coords[1] * yscale, coords[2] * zscale));
            }
        }
        return vectorPoints.toArray(new Point3d[0]);
    }
    
    private static float[][][] imageTo3Darray(ImagePlus image) {
        final ImageStack is = image.getStack();
        final float[][][] image3d = new float[is.getSize()][is.getWidth()][is.getHeight()];

        for (int k = 0; k < is.getSize(); k++) {
            ImageProcessor imageProc = is.getProcessor(k + 1);
            image3d[k] = imageProc.getFloatArray();
        }
        
        return image3d;
    }
}
