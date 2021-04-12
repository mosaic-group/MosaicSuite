package mosaic.particleTracker;


import java.awt.Color;

import ij.gui.Roi;
import mosaic.core.detection.Particle;


/**
 * Defines a Trajectory that is basically an array of sequential <code>Particle</code>s. <br>
 * Trajectory class has methods to display and anllyse this trajectory
 * @see Particle
 */
public class Trajectory {
    public final Particle[] iParticles;
    public int iSerialNumber;

    public boolean to_display = true; // flag for display filter
    public boolean showParticles = true; // draw particle, if false only trajectory line is drawn
    public Color color; // the display color of this Trajectory
    public Roi trajectoryArea; // The Roi area where a mouse click will select this trajectory

    /**
     * Constructor. <br>
     * Constructs a Trajectory from the given <code>Particle</code> array. <br>
     * Sets its length according to information of the first and last particles <br>
     * Sets its <code>Color</code> to default (red)
     * @param particles the array containing all the particles defining this Trajectory
     */
    public Trajectory(Particle[] particles, int aSerialNum) {
        iParticles = particles;
        iSerialNumber = aSerialNum;
        color = Color.red;
        setTrajectoryArea();
    }

    public boolean toDisplay() {
        return to_display;
    }

    public boolean drawParticle() {
        return showParticles;
    }
    
    public int getStartFrame() {
        return iParticles[0].getFrame();
    }
    
    public int getStopFrame() {
        return iParticles[iParticles.length - 1].getFrame();
    }
    
    public int getLength() {
        return getStopFrame() - getStartFrame() + 1;
    }

    private void setTrajectoryArea() {
        /* find the min and max values of the x and y positions */
        double min_x = Double.MAX_VALUE;
        double min_y = Double.MAX_VALUE;
        double max_x = -Double.MAX_VALUE;
        double max_y = -Double.MAX_VALUE;
        for (Particle p : iParticles) {
            min_x = Math.min(p.iX, min_x);
            min_y = Math.min(p.iY, min_y);
            max_x = Math.max(p.iX, max_x);
            max_y = Math.max(p.iY, max_y);
        }
        min_x = Math.floor(min_x);
        max_x = Math.ceil(max_x);
        min_y = Math.floor(min_y);
        max_y = Math.ceil(max_y);

        int focus_x = (int) min_x;
        int focus_y = (int) min_y;
        int focus_height = (int) (max_y - min_y) + 1;
        int focus_width = (int) (max_x - min_x) + 1;
        trajectoryArea = new Roi(focus_x, focus_y, focus_width, focus_height);
    }

    @Override
    public String toString() {
        return toStringBuffer().toString();
    }

    /**
     * Generates a "ready to print" StringBuffer with the particles defined
     * in this trajectory in the right order
     * 
     * @return a <code>StringBuffer</code> with the info
     */
    public StringBuffer toStringBuffer() {
        final StringBuffer s = new StringBuffer();
        for (Particle p : iParticles) {
            s.append(p.toStringBuffer());
        }
        return s.append("\n");
    }
}
