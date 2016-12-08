package mosaic.particleTracker;


import java.awt.Color;
import java.awt.event.MouseEvent;

import ij.ImagePlus;
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

    private final ImagePlus original_imp;
    public boolean to_display = true; // flag for display filter
    public boolean showParticles = true; // draw particle, if false only trajectory line is drawn
    public Color color; // the display color of this Trajectory
    Roi mouse_selection_area; // The Roi area where a mouse click will select this trajectory
    public Roi focus_area; // The Roi for focus display of this trajectory

    /**
     * Constructor. <br>
     * Constructs a Trajectory from the given <code>Particle</code> array. <br>
     * Sets its length according to information of the first and last particles <br>
     * Sets its <code>Color</code> to default (red)
     * @param particles the array containing all the particles defining this Trajectory
     */
    public Trajectory(Particle[] particles, int aSerialNum, ImagePlus originalImp) {
        original_imp = originalImp;
        iParticles = particles;
        iSerialNumber = aSerialNum;
        color = Color.red;
        setFocusAndSelectionArea();
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
    
    /**
     * Set the <code>focus_area</code> for this trajectory - it defines the area (ROI) focused
     * on when the user selects this trajectory to focus on <br>
     * The <code>focus_area</code> is an rectangular ROI that engulfs this trajectory
     * with 8 pixels margin from each edge
     * Set the <code>mouse_selection_area</code> for this trajectory - it defines the area (ROI)
     * on which a mouse click will add this trajectory as a candidate for selection <br>
     * When this trajectory is selected with a mouse click this ROI is highlighted for the user
     * to see his selection. <br>
     * @see TrajectoryStackWindow#mousePressed(MouseEvent)
     */
    private void setFocusAndSelectionArea() {
        if (original_imp == null) {
            return;
        }

        /* find the min and max values of the x and y positions */
        float min_x = Float.MAX_VALUE;
        float min_y = Float.MAX_VALUE;
        float max_x = -Float.MAX_VALUE;
        float max_y = -Float.MAX_VALUE;
        for (Particle p : iParticles) {
            min_x = Math.min(p.iX, min_x);
            min_y = Math.min(p.iY, min_y);
            max_x = Math.max(p.iX, max_x);
            max_y = Math.max(p.iY, max_y);
        }

        /*
         * set the focus area x, y , height, width to give focus area bigger by 8 pixels
         * then minimal rectangle surroundings this trajectory
         */
        // X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
        // (0,0) is the upper left corner; x is vertical top to bottom, y is horizontal left to right
        int focus_x = Math.max((int) min_x - 8, 0);
        int focus_y = Math.max((int) min_y - 8, 0);
        int focus_height = (int) max_y - focus_y + 8;
        int focus_width = (int) max_x - focus_x + 8;
        // make sure that the -8 or +8 did not create an ROI with bounds outside of the window
        if (focus_x + focus_width > original_imp.getWidth()) {
            focus_width = original_imp.getWidth() - focus_x;
        }
        if (focus_y + focus_height > original_imp.getHeight()) {
            focus_height = original_imp.getHeight() - focus_y;
        }
        focus_area = new Roi(focus_x, focus_y, focus_width, focus_height);
        
        focus_x = (int) min_x - 1;
        focus_y = (int) min_y - 1;
        focus_height = (int) max_x - focus_x + 1;
        focus_width = (int) max_y - focus_y + 1;
        mouse_selection_area = new Roi(focus_x, focus_y, focus_height, focus_width);
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
