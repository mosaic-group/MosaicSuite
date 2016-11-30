package mosaic.particleTracker;

import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import mosaic.core.detection.Particle;
import mosaic.test.framework.CommonBase;

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
        final Particle[] particles = new Particle[trajectoryLen];
        for (int i = 0; i < trajectoryLen; ++i) {
            particles[i] = new Particle(i + 1, 0, 0, i);
        }

        // Prepare Trajectory Analysis for calculations
        final TrajectoryAnalysis ta = new TrajectoryAnalysis(particles);
        ta.setTimeInterval(1.0);
        ta.setLengthOfAPixel(1.0);

        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should be successful", TrajectoryAnalysis.SUCCESS, ta.calculateAll());

        final double epsilon = 0.000001;
        assertEquals("MSS slope", 1.0, ta.getMSSlinear(), epsilon);
        assertEquals("MSS y-axis intercept", 0.0, ta.getMSSlinearY0(), epsilon);
        assertEquals("D2 diffusion coefficient", 0.25, ta.getDiffusionCoefficients()[1], epsilon);
        assertEquals("MSD slope", 2.0, ta.getGammasLogarithmic()[1], epsilon);
        assertEquals("MSD y-axis intercept", 0.0, ta.getGammasLogarithmicY0()[1], epsilon);
        
        assertEquals("Track lenght", 5.0, ta.getDistance(), epsilon);
        assertEquals("Avg step (per frame) lenght", 1.0, ta.getAvgDistance(), epsilon);
        assertEquals("Straightness", 1.0, ta.getStraightness(), epsilon);
        assertEquals("Bending", 0.0, ta.getBending(), epsilon);
        assertEquals("Bending (linear)", 0.0, ta.getBendingLinear(), epsilon);
        assertEquals("Efficiency", 1.0, ta.getEfficiency(), epsilon);
    }
    
    /**
     * Tests trajectory features calculated for zigzag movement
     */
    @Test
    public void testZigZagMovement() {
        // Create trajectory
        final int trajectoryLen = 6;
        final Particle[] particles = new Particle[trajectoryLen];
        particles[0] = new Particle(0, 0, 0, 1);
        particles[1] = new Particle(1, 1, 0, 2);
        particles[2] = new Particle(0, 2, 0, 3);
        particles[3] = new Particle(1, 3, 0, 4);
        particles[4] = new Particle(0, 4, 0, 5);
        particles[5] = new Particle(1, 5, 0, 6);
        
        // Prepare Trajectory Analysis for calculations
        final TrajectoryAnalysis ta = new TrajectoryAnalysis(particles);
        ta.setTimeInterval(1.0);
        ta.setLengthOfAPixel(1.0);

        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should be successful", TrajectoryAnalysis.SUCCESS, ta.calculateAll());

        final double epsilon = 0.000001;
        assertEquals("Track lenght", 5 * Math.sqrt(2), ta.getDistance(), epsilon);
        assertEquals("Avg step (per frame) lenght", Math.sqrt(2), ta.getAvgDistance(), epsilon);
        assertEquals("Straightness", 6.123e-17, ta.getStraightness(), epsilon);
        assertEquals("Bending", 0.0, ta.getBending(), epsilon);
        assertEquals("Bending (linear)", 0.0, ta.getBendingLinear(), epsilon);
        assertEquals("Efficiency", 0.52, ta.getEfficiency(), epsilon);
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
        final Particle[] particles = new Particle[trajectoryLen];
        for (int i = 0; i < trajectoryLen; ++i) {
            particles[i] = new Particle(i + 1, 0, 0, i*2);
        }

        // Prepare Trajectory Analysis for calculations
        final TrajectoryAnalysis ta = new TrajectoryAnalysis(particles);
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
        final Particle[] particles = new Particle[trajectoryLen];
        for (int i = 0; i < trajectoryLen; ++i) {
            particles[i] = new Particle(i + 1, 0, 0, i);
        }

        // Prepare Trajectory Analysis for calculations
        final TrajectoryAnalysis ta = new TrajectoryAnalysis(particles);
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
        final Particle[] particles = new Particle[trajectoryLen];
        for (int i = 0; i < trajectoryLen; ++i) {
            particles[i] = new Particle(i + 1, 0, 0, i);
        }

        // Prepare Trajectory Analysis for calculations
        final TrajectoryAnalysis ta = new TrajectoryAnalysis(particles);

        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should fail", TrajectoryAnalysis.FAILURE, ta.calculateAll());
    }

    /**
     * Test extreme case with null trajectory. (test both constructors)
     */
    @Test
    public void testNullTrajectory() {
        final Particle[] particles = null;
        final TrajectoryAnalysis ta1 = new TrajectoryAnalysis(particles);

        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should fail", TrajectoryAnalysis.FAILURE, ta1.calculateAll());

        final Trajectory trajectory = null;
        final TrajectoryAnalysis ta2 = new TrajectoryAnalysis(trajectory);

        // Set some tolerance on double numbers comparisons
        assertEquals("Calculation should fail", TrajectoryAnalysis.FAILURE, ta2.calculateAll());
    }
}
