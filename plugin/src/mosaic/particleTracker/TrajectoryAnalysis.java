package mosaic.particleTracker;

import mosaic.core.detection.Particle;


/**
 *  This class is responsible for processing trajectories basing on methods presented in:
 *  I. F. Sbalzarini. Moments of displacement and their spectrum. 
 *  ICoS technical report, Institute of Computational Science (ICoS), ETH Zürich, 2005. 
 */
public class TrajectoryAnalysis {

    Trajectory iTrajectory;          // given Trajectory
    int[] iMomentOrders;             // requested moment orders to be calculated
    int[] iFrameShifts;              // requested frame shift (deltas)
    double[][] iMSDs;                // moments of displacement for every moment order
    double[] iGammasLogarithmic;     // vector of scaling coefficients (slopes) from logarithmic plot
    double[] iGammasLogarithmicY0;   // vector of y-axis intercept for scaling coefficients
    double[] iGammasLinear;          // vector of scaling coefficients (slopes) from linear plot
    double[] iGammasLinearY0;        // vector of y-axis intercept for scaling coefficients
    double[] iDiffusionCoefficients; // vector of diffusion coefficients
    double iMSSlinear;               // slope of moments scaling spectrum for linear plot
    double iMSSlinearY0;             // y-axis intercept of MSS for linear plot
    double iMSSlogarithmic;          // slope of moments scaling spectrum for logarithmic plot
    double iMSSlogarithmicY0;        // y-axis intercept of MSS for logarithmic plot
    
    public static final boolean SUCCESS = true;
    public static final boolean FAILURE = false;
    
    /**
     * @param aTrajectory Trajectory to be analyzed
     */
    public TrajectoryAnalysis(Trajectory aTrajectory) {
        iTrajectory = aTrajectory;
        
        // set some default data for calculations, it can be overwritten by user
        setFrameShifts(1, iTrajectory.length/3);
        setMomentOrders(1, 10);
    }
    
    /**
     * Sets orders used to calculate mean displacements in range from [aMin, aMax]
     * @param aMin start index (included)
     * @param aMax stop index (included)
     */
    public void setMomentOrders(int aMin, int aMax) {
        if (aMax >= aMin) {
            iMomentOrders = generateArrayRange(aMin, aMax);
        }
    }
    
    /**
     * Sets user defined orders used to calculate mean displacements.
     * @param aOrders (values should be >= 1)
     */
    public void setMomentOrders(int[] aOrders) {
        iMomentOrders = aOrders;
    }
    
    /**
     * @return currently set moment orders 
     */
    public int[] getMomentOrders() {
        return iMomentOrders;
    }
    
    /**
     * Sets frame shifts (deltas) in range from [aMin, aMax]
     * @param aMin start index (included)
     * @param aMax stop index (included)
     */
    public void setFrameShifts(int aMin, int aMax) {
        if (aMax >= aMin) {
            iFrameShifts = generateArrayRange(aMin, aMax);
        }
    }
    
    /**
     * Sets user defined frame shifts (deltas).
     * @param aFrameShifts (delta values should be >= 1)
     */
    public void setFrameShifts(int[] aFrameShifts) {
        iFrameShifts = aFrameShifts;
    }
    
    /**
     * @return currently set frame shifts (deltas) 
     */
    public int[] getFrameShifts() {
        return iFrameShifts;
    }
    
    /**
     * Calculates mean displacements, scaling coefficients and slope of moments scaling spectrum.
     * @return This method returns {@link #SUCCESS} or {@link #FAILURE}
     */
    public boolean calculateAll() {
        if (iFrameShifts != null && iFrameShifts.length >= 1 && 
            iMomentOrders != null && iMomentOrders.length >= 1) {

            return calculateMSDs() && 
            calculateGammasAndDiffusionCoefficients() &&
            calculateMSS();
            
        }
        return FAILURE;
    }
    
    /**
     * @return mean displacement for given index (according to given moment orders)
     */
    public double[] getMSDforMomentIdx(int aMomentIdx) {
        return iMSDs[aMomentIdx];
    }
    
    /**
     * @return vector of scaling coefficients (slopes) for logarithmic plot
     */
    public double[] getGammasLogarithmic() {
        return iGammasLogarithmic;
    }
    
    /**
     * @return y-axis intercept of scaling coefficients (slopes) for logarithmic plot
     */
    public double[] getGammasLogarithmicY0() {
        return iGammasLogarithmicY0;
    }

    /**
     * @return vector of scaling coefficients (slopes) for linear plot
     */
    public double[] getGammasLinear() {
        return iGammasLinear;
    }
    
    /**
     * @return y-axis intercept of scaling coefficients (slopes) for linear plot
     */
    public double[] getGammasLinearY0() {
        return iGammasLinearY0;
    }
    
    /**
     * @return Diffusion coefficients of all orders.
     *         D2 - corresponds to the regular diffusion constant (order=2 -> array index = 1)
     */
    public double[] getDiffusionCoefficients() {
        return iDiffusionCoefficients;
    }
    
    /**
     * @return slope of moments scaling spectrum
     */
    public double getMSSlinear() {
        return iMSSlinear;
    }
    
    /**
     * @return y-axis intercept of mss for linear plot
     */
    public double getMSSlinearY0() {
        return iMSSlinearY0;
    }

    /**
     * @return slope of moments scaling spectrum
     */
    public double getMSSlogarithmic() {
        return iMSSlogarithmic;
    }
    
    /**
     * @return y-axis intercept of mss for logarithmic plot
     */
    public double getMSSlogarithmicY0() {
        return iMSSlogarithmicY0;
    }

    @Override
    public String toString() {
        String str = "";
        
        str += "MSDs:\n";
        str += "-----------------------------------\n";
        
        for (double[] m : iMSDs) {
            String line = "";
            for (double d : m) {
                line += String.format("%15.4f ", d);
            }
            str += line + "\n";
        }
        
        str += "\nGAMMAs:\n";
        str += "-----------------------------------\n";
        String line = "";
        for (double g : iGammasLogarithmic) {
            line += String.format("%15.4f ", g);
        }
        str += line + "\n";
        
        str += "\nMSS:\n";
        str += "-----------------------------------\n";
        str += String.format("%15.4f\n", iMSSlinear);
        
        return str; 
    }
    
    public double[] toLogScale(double[] aVector) {
        double[] result = new double[aVector.length];
        for (int i = 0; i < aVector.length; ++i) {
            result[i] = Math.log(aVector[i]);
        }
        
        return result;
    }
    
    public double[] toLogScale(int[] aVector) {
        double[] result = new double[aVector.length];
        for (int i = 0; i < aVector.length; ++i) {
            result[i] = Math.log(aVector[i]);
        }
        
        return result;
    }
    
    public double[] toDouble(int[] aValues) {
        double[] result = new double[aValues.length];
        for (int i = 0; i < aValues.length; ++i) {
            result[i] = (double)aValues[i];
        }
        
        return result;
    }
    
    /**
     * Calculates mean displacement of order 'aOrder' for a specific frame shift 'aDelta' for 
     * a given trajectory 'aTrajectory'
     * 
     * @param aTrajectory trajectory to be processed
     * @param aDelta frame shift (shoult be >= 1)
     * @param aOrder order of mean moment. When aOrder=2 then this special case is called
     *               'mean suare displacement'
     * @return
     */
    private double meanDisplacement(Trajectory aTrajectory, int aDelta, int aOrder) {
        Particle[] particles = aTrajectory.existing_particles;
        final int noOfParticles = particles.length; 

        if (noOfParticles < 2 || aDelta <= 0) {
            // In case when it is impossible to calculete mean moment of order 'aOrder' just 
            // return 0
            
            return 0;
        }
        
        // Calculate mean moment
        double sum = 0;
        int noOfElements = 0;       
        
        for (int i = 0; i < noOfParticles; ++i) {
            Particle pi = particles[i];
            
            // It may happen that particle has not been discovered in each frame. Try to  
            // find a particle in aDelta distance. For futher information about this behaviour
            // please refere to 'Link Range' parameter.
            for (int j = i + 1; j < noOfParticles; ++j) {
                Particle pj = particles[j];
                if (pj.getFrame() == pi.getFrame() + aDelta) {
                    double dx = (pj.x - pi.x);
                    double dy = (pj.y - pi.y);
                    
                    // Calculate Euclidean norm to get distance between particles and 
                    // power it to aOrder
                    sum += Math.pow((dx*dx + dy*dy), aOrder/2.0d);
                    ++noOfElements;
                                  
                    // No need to look further
                    break;
                }
                else if (pj.getFrame() > pi.getFrame() + aDelta) {
                    // Particle in aDelta frame-distance has not been found -> continue.
                    break;
                }
            }
        }
        
        return noOfElements == 0 ? 0 : sum/noOfElements;
    }
    
    private boolean calculateMSDs() {
        iMSDs = new double[iMomentOrders.length][iFrameShifts.length];
        
        int orderIdx = 0;
        for (int order : iMomentOrders) {
            int deltaIdx = 0;
            for (int delta : iFrameShifts) {
                double displacement = meanDisplacement(iTrajectory, delta, order);
                iMSDs[orderIdx][deltaIdx] = displacement;
                deltaIdx++;
            }
            orderIdx++;
        }
        
        return SUCCESS;
    }

    private boolean calculateGammasAndDiffusionCoefficients() {
        LeastSquares ls = new LeastSquares();
        
        final int noOfMoments = iMomentOrders.length;
        iGammasLogarithmic = new double[noOfMoments];
        iGammasLinear = new double[noOfMoments];
        iGammasLogarithmicY0 = new double[noOfMoments];
        iGammasLinearY0 = new double[noOfMoments];
        iDiffusionCoefficients = new double[noOfMoments];
        
        int gammaIdx = 0;
        for (double[] m : iMSDs) {
            // Get rid of MSDs equal to 0 (could happen when trajectory has not enough points).
            double[] moments = new double[m.length];
            double[] deltas  = new double[m.length];
            int count = 0;
            for (int i = 0; i < iFrameShifts.length; ++ i) {
                if (m[i] != 0.0d) {
                    moments[count] = m[i];
                    deltas[count] = (double)iFrameShifts[i];
                    count++;
                }
            }
            if (count < 2) {
                // it is not possible to do linear regression with less than 2 points.
                return FAILURE;
            }
            double[] tmpMoments = new double[count];
            double[] tmpDeltas  = new double[count];
            for (int i = 0; i < count; ++i) {
                tmpMoments[i] = moments[i];
                tmpDeltas[i] = deltas[i];
            }
            
            
            double[] mLog = toLogScale(tmpMoments); // moments in log scale
            double[] dLog = toLogScale(tmpDeltas);  // deltas in log scale
            ls.calculate(dLog, mLog);
            if (Double.isNaN(ls.getAlpha()) || Double.isNaN(ls.getBeta())) {
                // Usually it is a result of not enough number of points in trajectory or 
                // missing detections in frames delta*n (for n=0...trajectoryLenght/3)
                return FAILURE;
            }
            iGammasLogarithmic[gammaIdx] = ls.getBeta();
            iGammasLogarithmicY0[gammaIdx] = ls.getAlpha();
            iDiffusionCoefficients[gammaIdx] = 0.25 * Math.exp(ls.getAlpha());
            ls.calculate(tmpDeltas, tmpMoments);
            iGammasLinear[gammaIdx] = ls.getBeta();
            iGammasLinearY0[gammaIdx] = ls.getAlpha();
            gammaIdx++;
        }
        
        return SUCCESS;
    }
    
    private boolean calculateMSS() {
        LeastSquares ls = new LeastSquares();
        
        ls.calculate(toDouble(iMomentOrders), iGammasLogarithmic);
        iMSSlinear = ls.getBeta();
        iMSSlinearY0 = ls.getAlpha();
        
        ls.calculate(toLogScale(iMomentOrders), toLogScale(iGammasLogarithmic));
        iMSSlogarithmic = ls.getBeta();
        iMSSlogarithmicY0 = ls.getAlpha();
        
        return SUCCESS;
    }
    
    
    /**
     * Generates int[] with values from aMin to aMax (included) with step 1
     * @param aMin
     * @param aMax
     * @return array of requested values
     */
    private int[] generateArrayRange(int aMin, int aMax) {
        int[] range = new int[aMax - aMin + 1];
        int idx = 0;
        for (int m = aMin; m <= aMax; ++m) {
            range[idx] = m;
            idx++;
        }
        
        return range;
    }
}
