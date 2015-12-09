package mosaic.ia;

import mosaic.utils.math.Matlab;

/**
 * This class contains implementation of all used potentials. 
 * Way of calculating and potentials functions has been moved from old code and their
 * definitions has not been changed.
 */
public class Potentials {
    public static enum PotentialType {
        STEP,
        HERNQUIST,
        L1,
        L2,
        PlUMMER,
        NONPARAM,
        COULOMB
    }
    
    /**
     * Simple factory method for non "NONPARAM" potentials
     */
    public static Potential createPotential(PotentialType aType) {
        if (aType != PotentialType.NONPARAM) {
            return createPotential(aType, 0, 0, 0, 0);
        }
        throw new RuntimeException("Unknown or non parametric potential.");
    }
    
    /**
     * Simple factory method for potentials, in case of non "NOPARAM" potentials only aType parameter
     * is taken into account.
     */
    public static Potential createPotential(PotentialType aType, double min, double max, int numOfPoints, double smooth) {
        switch (aType) {
            case STEP:
                return new PotentialStep();
            case HERNQUIST:
                return new PotentialHernquist();
            case L1:
                return new PotentiaL1();
            case L2:
                return new PotentiaL2();
            case PlUMMER:
                return new PotentiaPlummer();
            case COULOMB:
                return new PotentiaCoulomb();
            case NONPARAM:
                return new  PotentialNoParam(min, max, numOfPoints, smooth);
            default:
                throw new RuntimeException("Unknown potential.");
        }
    }
    
    /**
     * Base class for all potentials
     */
    public static abstract class Potential {
        
        public abstract void calculate(double[] aDistances, double[] aParameters);
        public abstract void calculateWithoutEpsilon(double[] aDistances, double[] aParameters);
        public abstract int numOfDimensions();
        public abstract PotentialType getType();
        
        protected double[] potential;
        protected double[] gibbspotential;
        protected double sumPotential;
        
        public double[] getGibbsPotential() {
            return gibbspotential;
        }

        public double[] getPotential() {
            return potential;
        }

        public double getSumPotential() {
            return sumPotential;
        }
        
        protected void clearResults(int aLenght) {
            potential = new double[aLenght];
            sumPotential = 0;
            gibbspotential = new double[aLenght];
        }
    }
    
    public static class PotentialNoParam extends Potential {
        // smaller the smoother (smaller => penalty is high)
        private double iSmoothness; 
        private double[] iSupportPoints;
        
        public PotentialNoParam(double aMinDistance, double aMaxDistance, int aNumOfPoints, double aSmoothness) {
            iSmoothness = aSmoothness;
            iSupportPoints = Matlab.linspaceArray(aMinDistance, aMaxDistance, aNumOfPoints);
        }
        
        public double getSmoothness() {return iSmoothness;}
        public double[] getSupportPoints() {return iSupportPoints;}
        
        @Override 
        public int numOfDimensions() {return iSupportPoints.length - 1;}
        
        @Override
        public void calculate(double[] aDistances, double[] aParameters) {
            calc(aDistances, aParameters);
        }
        
        @Override
        public void calculateWithoutEpsilon(double[] aDistances, double[] aParameters) {
            calc(aDistances, aParameters);
        }
        
        public void calc(double[] aDistances, double[] aParameters) {
            clearResults(aDistances.length);
            for (int i = 0; i < aDistances.length; i++) {
                final double[] weights = aParameters;
                potential[i] = nonParametric(aDistances[i], weights);
                sumPotential = sumPotential + potential[i];
                gibbspotential[i] = Math.exp(-1 * potential[i]);
            }
        }
        
        double nonParametric(double di, double[] weights) {
            final double h = Math.abs(iSupportPoints[1] - iSupportPoints[0]);
            double sum = 0;
            double z = 0, kappa = 0;
            for (int i = 0; i < weights.length; i++) {
                z = Math.abs(di - iSupportPoints[i]);
                if (z <= h) {
                    kappa = z / h;
                }
                else {
                    kappa = 0;
                }
                sum = sum + weights[i] * kappa;
            }

            return sum;
        }

        @Override
        public PotentialType getType() {
            return PotentialType.NONPARAM;
        }
    }
    
    protected static class PotentialStep extends Potential {
        @Override 
        public int numOfDimensions() {return 2;}
        
        @Override
        public void calculate(double[] aDistances, double[] aParameters) {
            final double threshold = Math.abs(aParameters[1]);
            final double epsilon = Math.abs(aParameters[0]);
            calc(aDistances, threshold, epsilon);
        }
        
        @Override
        public void calculateWithoutEpsilon(double[] aDistances, double[] aParameters) {
            final double threshold = Math.abs(aParameters[1]);
            final double epsilon = 1.0;
            calc(aDistances, threshold, epsilon);
        }
        
        public void calc(double[] aDistances, double threshold, double epsilon) {
            clearResults(aDistances.length);
            for (int i = 0; i < aDistances.length; i++) {
                potential[i] = epsilon * stepPotential(aDistances[i], threshold);
                sumPotential += potential[i];
                gibbspotential[i] = Math.exp(-1 * potential[i]);
            }
        }
        
        double stepPotential(double di, double threshold) {
            if (di < threshold) {
                return -1d;
            }
            else {
                return 0d;
            }
        }

        @Override
        public PotentialType getType() {
            return PotentialType.STEP;
        }
    }
    
    protected static class PotentialHernquist extends Potential {
        @Override 
        public int numOfDimensions() {return 2;}
        
        @Override
        public void calculate(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]); // if sigma is large, z=d/sigma= small => -1/(1+z) is large => will be chosen during maximum likelihood.
            final double epsilon = Math.abs(aParameters[0]);
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        @Override
        public void calculateWithoutEpsilon(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]); // if sigma is large, z=d/sigma= small => -1/(1+z) is large => will be chosen during maximum likelihood.
            final double epsilon = 1;
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        public void calc(double[] aDistances, double threshold, double epsilon, double sigma) {
            clearResults(aDistances.length);
            for (int i = 0; i < aDistances.length; i++) {
                potential[i] = epsilon * hernquistPotential(aDistances[i], threshold, sigma);
                sumPotential += potential[i];
                gibbspotential[i] = Math.exp(-1 * potential[i]);
            }
        }
        
        double hernquistPotential(double di, double threshold, double sigma) {
            final double z = (di - threshold) / sigma;
            if (z > 0) {
                return -1 / (1 + z);
            }
            else {
                return -1 * (1 - z);
            }
        }

        @Override
        public PotentialType getType() {
            return PotentialType.HERNQUIST;
        }
    }
    
    protected static class PotentiaL1 extends Potential {
        @Override 
        public int numOfDimensions() {return 2;}
        
        @Override
        public void calculate(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]);
            final double epsilon = aParameters[0];
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        @Override
        public void calculateWithoutEpsilon(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]);
            final double epsilon = 1;
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        public void calc(double[] aDistances, double threshold, double epsilon, double sigma) {
            clearResults(aDistances.length);
            for (int i = 0; i < aDistances.length; i++) {
                potential[i] = epsilon * linearType1(aDistances[i], threshold, sigma);
                sumPotential += potential[i];
                gibbspotential[i] = Math.exp(-1 * potential[i]);
            }
        }
        
        double linearType1(double di, double threshold, double sigma) {
            final double z = (di - threshold) / sigma;
            if (z > 1) {
                return 0;
            }
            else {
                return -1 * (1 - z);
            }
        }

        @Override
        public PotentialType getType() {
            return PotentialType.L1;
        }
    }
    
    protected static class PotentiaL2 extends Potential {
        @Override 
        public int numOfDimensions() {return 2;}
        
        @Override
        public void calculate(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]);
            final double epsilon = Math.abs(aParameters[0]);
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        @Override
        public void calculateWithoutEpsilon(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]);
            final double epsilon = 1;
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        public void calc(double[] aDistances, double threshold, double epsilon, double sigma) {
            clearResults(aDistances.length);
            for (int i = 0; i < aDistances.length; i++) {
                potential[i] = epsilon * linearType2(aDistances[i], threshold, sigma);
                sumPotential += potential[i];
                gibbspotential[i] = Math.exp(-1 * potential[i]);
            }
        }
        
        static double linearType2(double di, double threshold, double sigma) {
            final double z = (di - threshold) / sigma;
            if (z > 1) {
                return 0;
            }
            else if (z < 0) {
                return -1;
            }
            else {
                return -1 * (1 - z);
            }
        }

        @Override
        public PotentialType getType() {
            return PotentialType.L2;
        }
    }
    
    protected static class PotentiaPlummer extends Potential {
        @Override 
        public int numOfDimensions() {return 2;}
        
        @Override
        public void calculate(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]);
            final double epsilon = aParameters[0];
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        @Override
        public void calculateWithoutEpsilon(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]);
            final double epsilon = 1;
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        public void calc(double[] aDistances, double threshold, double epsilon, double sigma) {
            clearResults(aDistances.length);
            for (int i = 0; i < aDistances.length; i++) {
                potential[i] = epsilon * plummerPotential(aDistances[i], threshold, sigma);
                sumPotential += potential[i];
                gibbspotential[i] = Math.exp(-1 * potential[i]);
            }
        }
        
        static double plummerPotential(double di, double threshold, double sigma) {
            final double z = (di - threshold) / sigma;
            if (z > 0) {
                return -1 * Math.pow(1 + z * z, -.5);
            }
            else {
                return -1;
            }
        }

        @Override
        public PotentialType getType() {
            return PotentialType.PlUMMER;
        }
    }
    
    protected static class PotentiaCoulomb extends Potential {
        @Override 
        public int numOfDimensions() {return 2;}
        
        @Override
        public void calculate(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]);
            final double epsilon = aParameters[0];
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        @Override
        public void calculateWithoutEpsilon(double[] aDistances, double[] aParameters) {
            final double threshold = 0;
            final double sigma = Math.abs(aParameters[1]);
            final double epsilon = 1;
            calc(aDistances, threshold, epsilon, sigma);
        }
        
        public void calc(double[] aDistances, double threshold, double epsilon, double sigma) {
            clearResults(aDistances.length);
            for (int i = 0; i < aDistances.length; i++) {
                potential[i] = epsilon * coulomb(aDistances[i], threshold, sigma);
                sumPotential += potential[i];
                gibbspotential[i] = Math.exp(-1 * potential[i]);
            }
        }
        
        double coulomb(double di, double threshold, double sigma) {
            final double z = (di - threshold) / sigma;
            if (z != 0) {
                return 1 / (z * z);
            }
            else {
                return Math.sqrt(Double.MAX_VALUE); // this is a hack.
            }
        }

        @Override
        public PotentialType getType() {
            return PotentialType.COULOMB;
        }
    }
}
