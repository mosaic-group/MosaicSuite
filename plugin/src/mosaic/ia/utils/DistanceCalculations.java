package mosaic.ia.utils;


import java.util.Arrays;
import java.util.Vector;

import javax.vecmath.Point3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import weka.estimators.KernelEstimator;


public abstract class DistanceCalculations {

    private final double gridDeltaLenght;
    private final double kernelWeightq;
    private final int iNumberOfBins;
    
    private final float[][][] maskImage3d;

    protected Point3d[] iparticlesX;
    protected Point3d[] iParticlesY;
    protected double zscale = 1;
    protected double xscale = 1;
    protected double yscale = 1;

    private double[] iDistances;
    private double[][] iProbabilityAndDistancesDistribution;

    DistanceCalculations(ImagePlus mask, double gridSize, double kernelWeightq, int discretizationSize) {
        this.gridDeltaLenght = gridSize;
        this.kernelWeightq = kernelWeightq;
        this.iNumberOfBins = discretizationSize;
        maskImage3d = mask != null ? imageTo3Darray(mask) : null;
    }
    
    public double[][] getProbabilityDistribution() {
        return iProbabilityAndDistancesDistribution;
    }

    public double[] getDistancesOfX() {
        return iDistances;
    }

    protected void stateDensity(double x1, double y1, double z1, double x2, double y2, double z2) {
        final int x_size = (int) Math.floor(Math.abs(x1 - x2) * xscale / gridDeltaLenght) + 1;
        final int y_size = (int) Math.floor(Math.abs(y1 - y2) * yscale / gridDeltaLenght) + 1;
        final int z_size = (int) Math.floor(Math.abs(z1 - z2) * zscale / gridDeltaLenght) + 1;

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
                        position.x = x1 + i * gridDeltaLenght;
                        position.y = y1 + j * gridDeltaLenght;
                        position.z = z1 + k * gridDeltaLenght;
                        double distance = nearestNeighbor.getNearestNeighborDistance(position);
                        kernelEstimator.addValue(distance, kernelWeightq);
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

        iProbabilityAndDistancesDistribution = new double[2][iNumberOfBins];
        for (int i = 0; i < iNumberOfBins; i++) {
            iProbabilityAndDistancesDistribution[0][i] = i * binLength;
            iProbabilityAndDistancesDistribution[1][i] = kernelEstimator.getProbability(i * binLength);
        }
        System.out.println(Arrays.toString(iProbabilityAndDistancesDistribution[1]));
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
            // May happen if mask is applied to loaded coordinates. In that case 
            // checks are not done.
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
