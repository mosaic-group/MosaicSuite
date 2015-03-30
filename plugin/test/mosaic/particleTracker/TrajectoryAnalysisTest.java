package mosaic.particleTracker;

import static org.junit.Assert.*;
import mosaic.core.detection.Particle;
import mosaic.test.framework.CommonBase;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/** 
 * This class is responsible for testing {@link TrajectoryAnalysis} class. 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TrajectoryAnalysisTest extends CommonBase {
    
    /** 
     * Tests simple trajectory with particles moving in equal length intervals
     * along x axis (x,y) = {(1, 0), (2,0), ...}
     */
    @Test
    public void testLinearMovement() {
        // Create trajectory 
        final int trajectoryLen = 6;
        Particle[] particles = new Particle[trajectoryLen];
        for (int i = 0; i < trajectoryLen; ++i) {
            particles[i] = new Particle(i + 1, 0, 0, i, 0);
        }
        
        // Prepare Trajectory Analysis for calculations
        TrajectoryAnalysis ta = new TrajectoryAnalysis(particles);
        ta.setTimeInterval(1.0);
        ta.setLengthOfAPixel(1.0);
        
        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should be successful", TrajectoryAnalysis.SUCCESS, ta.calculateAll());
        
        double epsilon = 0.000001;
        assertEquals("MSS slope", 1.0, ta.getMSSlinear(), epsilon);
        assertEquals("MSS y-axis intercept", 0.0, ta.getMSSlinearY0(), epsilon);
        assertEquals("D2 diffusion coefficient", 0.25, ta.getDiffusionCoefficients()[1], epsilon);
        assertEquals("MSD slope", 2.0, ta.getGammasLogarithmic()[1], epsilon);
        assertEquals("MSD y-axis intercept", 0.0, ta.getGammasLogarithmicY0()[1], epsilon);
    }

    /** 
     * Test Trajectory with enough number of frames but with every second frame missing
     * (containing only frames 0, 2, 4... ) In such case it is not possible to perform calculations
     * since there are no points enough to calculate slopes (at least two needed)
     */
    @Test
    public void testLinearMovementWithManySkippedFrames() {
        // Create trajectory 
        final int trajectoryLen = 6;
        Particle[] particles = new Particle[trajectoryLen];
        for (int i = 0; i < trajectoryLen; ++i) {
            particles[i] = new Particle(i + 1, 0, 0, i*2, 0);
        }
        
        // Prepare Trajectory Analysis for calculations
        TrajectoryAnalysis ta = new TrajectoryAnalysis(particles);
        ta.setTimeInterval(1.0);
        ta.setLengthOfAPixel(1.0);
        
        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should fail", TrajectoryAnalysis.FAILURE, ta.calculateAll());
    }
    
    /** 
     * With trajectory shorter that 6 points it is impossible to calculate MSS/MSD because
     * deltas are in range {1 ... length/3}
     */
    @Test
    public void testTooShortTrajectory() {
        
        // Create too short trajectory
        final int trajectoryLen = 5;
        Particle[] particles = new Particle[trajectoryLen];
        for (int i = 0; i < trajectoryLen; ++i) {
            particles[i] = new Particle(i + 1, 0, 0, i, 0);
        }
        
        // Prepare Trajectory Analysis for calculations
        TrajectoryAnalysis ta = new TrajectoryAnalysis(particles);
        ta.setTimeInterval(1.0);
        ta.setLengthOfAPixel(1.0);
        
        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should fail", TrajectoryAnalysis.FAILURE, ta.calculateAll());
    }
    
    /** 
     * With trajectory shorter that 6 points it is impossible to calculate MSS/MSD because
     * deltas are in range {1 ... length/3}
     */
    @Test
    public void testTooShortTrajectoryWithZeroElements() {
        
        // Create simple trajectory with particles moving in equal length intervals
        // along x axis
        final int trajectoryLen = 0;
        Particle[] particles = new Particle[trajectoryLen];
        for (int i = 0; i < trajectoryLen; ++i) {
            particles[i] = new Particle(i + 1, 0, 0, i, 0);
        }
        
        // Prepare Trajectory Analysis for calculations
        TrajectoryAnalysis ta = new TrajectoryAnalysis(particles);
        
        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should fail", TrajectoryAnalysis.FAILURE, ta.calculateAll());
    }
    
    /** 
     * Test extreme case with null trajectory. (test both constructors)
     */
    @Test
    public void testNullTrajectory() {
        Particle[] particles = null;
        TrajectoryAnalysis ta1 = new TrajectoryAnalysis(particles);
        
        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should fail", TrajectoryAnalysis.FAILURE, ta1.calculateAll());
        
        Trajectory trajectory = null;
        TrajectoryAnalysis ta2 = new TrajectoryAnalysis(trajectory);
        
        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should fail", TrajectoryAnalysis.FAILURE, ta2.calculateAll());
    }
}
