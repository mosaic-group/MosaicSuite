package mosaic.ia;


import org.scijava.vecmath.Point3d;

public class DistanceCalculationsCoords extends DistanceCalculations {

    private final Point3d[] X, Y;
    private final double x1, x2, y1, y2, z1, z2;
    
    public DistanceCalculationsCoords(Point3d[] X, Point3d[] Y, float[][][] mask, double xmin, double ymin, double zmin, double xmax, double ymax, double zmax, double gridSize, double kernelWeightq, double kernelWeightP) {
        this(X, Y, mask, xmin, ymin, zmin, xmax, ymax, zmax, gridSize, kernelWeightq, kernelWeightP, NumberOfDistPoints);
    }
    
    public DistanceCalculationsCoords(Point3d[] X, Point3d[] Y, float[][][] mask, double xmin, double ymin, double zmin, double xmax, double ymax, double zmax, double gridSize, double kernelWeightq, double kernelWeightP, int aNumberOfDistPoints) {
        super(mask, gridSize, kernelWeightq, kernelWeightP, aNumberOfDistPoints);
        this.X = X;
        this.Y = Y;
        x1 = xmin;
        y1 = ymin;
        z1 = zmin;
        y2 = ymax;
        x2 = xmax;
        z2 = zmax;
        
        calcDistances();
    }

    private void calcDistances() {
        iParticlesX = getFilteredAndScaledCoordinates(X);
        iParticlesY = getFilteredAndScaledCoordinates(Y);
        stateDensity(x1, x2, y1, y2, z1, z2);
    }
}
