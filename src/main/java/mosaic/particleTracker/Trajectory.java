package mosaic.particleTracker;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.Color;
import java.util.ArrayList;

import mosaic.core.detection.Particle;


/**
 * Defines a Trajectory that is basically an array of sequential <code>Particle</code>s. <br>
 * Trajectory class has methods to display and anllyse this trajectory
 * 
 * @see Particle
 */
public class Trajectory {

    private final static int SYSTEM = 0;
    private final static int IJ_RESULTS_WINDOW = 1;

    public int stop_frame;
    public int start_frame;
    public final Particle[] existing_particles; // holds all particles of this trajetory in order
    public final int length; // number of frames this trajectory spans on
    private final ImagePlus original_imp;

    private final ArrayList<int[]> gaps = new ArrayList<int[]>(); // holds arrays (int[]) of size 2 that holds
    // 2 indexs of particles in the existing_particles.
    // These particles are the start and end points of a gap
    // in this trajectory
    private int num_of_gaps = 0;

    public int serial_number; // serial number of this trajectory (for report and display)
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
     * 
     * @param particles the array containing all the particles defining this Trajectory
     */
    public Trajectory(Particle[] particles, int aSerialNum, ImagePlus originalImp) {
        original_imp = originalImp;
        existing_particles = particles;
        serial_number = aSerialNum;
        // the length is the last trajectory frame - the first frame (first frame can be 0)
        start_frame = existing_particles[0].getFrame();
        stop_frame = existing_particles[this.existing_particles.length - 1].getFrame();
        length = stop_frame - start_frame + 1;
        color = Color.red; // default
        
        setFocusArea();
        setMouseSelectionArea();
        populateGaps();
    }

    /**
     * Is the trajectory to show
     *
     * @return true if is to display
     */

    public boolean toDisplay() {
        return to_display;
    }

    public boolean drawParticle() {
        return showParticles;
    }
    /**
     * Set the <code>focus_area</code> for this trajectory - it defines the area (ROI) focused
     * on when the user selects this trajectory to focus on <br>
     * The <code>focus_area</code> is an rectangular ROI that engulfs this trajectory
     * with 8 pixels margin from each edge
     * 
     * @see TrajectoryStackWindow#mousePressed(MouseEvent)
     */
    public void setFocusArea() {

        // there is no image so set focus does not make sense

        if (original_imp == null) {
            return;
        }

        /* find the min and max values of the x and y positions */
        float min_x = this.existing_particles[0].iX;
        float min_y = this.existing_particles[0].iY;
        float max_x = this.existing_particles[0].iX;
        float max_y = this.existing_particles[0].iY;
        for (int i = 0; i < this.existing_particles.length; i++) {
            min_x = Math.min(this.existing_particles[i].iX, min_x);
            min_y = Math.min(this.existing_particles[i].iY, min_y);
            max_x = Math.max(this.existing_particles[i].iX, max_x);
            max_y = Math.max(this.existing_particles[i].iY, max_y);
        }

        /*
         * set the focus area x, y , height, width to give focus area bigger by 8 pixels
         * then minimal rectangle surroundings this trajectory
         */

        // X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
        // (0,0) is the upper left corner; x is vertical top to bottom, y is horizontal left to right
        final int focus_x = Math.max((int) min_x - 8, 0);
        final int focus_y = Math.max((int) min_y - 8, 0);
        int focus_height = (int) max_y - focus_y + 8;
        int focus_width = (int) max_x - focus_x + 8;
        // make sure that the -8 or +8 did not create an ROI with bounds outside of the window
        if (focus_x + focus_width > original_imp.getWidth()) {
            focus_width = original_imp.getWidth() - focus_x;
        }
        if (focus_y + focus_height > original_imp.getHeight()) {
            focus_height = original_imp.getHeight() - focus_y;
        }
        this.focus_area = new Roi(focus_x, focus_y, focus_width, focus_height);
    }

    /**
     * Set the <code>mouse_selection_area</code> for this trajectory - it defines the area (ROI)
     * on which a mouse click will add this trajectory as a candidate for selection <br>
     * When this trajectory is selected with a mouse click this ROI is highlighted for the user
     * to see his selection. <br>
     * The <code>mouse_selection_area</code> is an rectangular ROI that engulfs this trajectory
     * with 1 pixel margin from each edge
     * 
     * @see TrajectoryStackWindow#mousePressed(MouseEvent)
     */
    public void setMouseSelectionArea() {
        /* find the min and max values of the x and y positions */
        float min_x = this.existing_particles[0].iX;
        float min_y = this.existing_particles[0].iY;
        float max_x = this.existing_particles[0].iX;
        float max_y = this.existing_particles[0].iY;
        for (int i = 0; i < this.existing_particles.length; i++) {
            min_x = Math.min(this.existing_particles[i].iX, min_x);
            min_y = Math.min(this.existing_particles[i].iY, min_y);
            max_x = Math.max(this.existing_particles[i].iX, max_x);
            max_y = Math.max(this.existing_particles[i].iY, max_y);
        }

        /*
         * set the focus area x, y , height, width to give focus area bigger by 1 pixel
         * then minimal rectangle surroundings this trajectory
         */

        // X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
        // (0,0) is the upper left corner; x is vertical top to bottom, y is horizontal left to right
        final int focus_x = (int) min_x - 1;
        final int focus_y = (int) min_y - 1;
        final int focus_height = (int) max_x - focus_x + 1;
        final int focus_width = (int) max_y - focus_y + 1;
        this.mouse_selection_area = new Roi(focus_x, focus_y, focus_height, focus_width);

    }

    /**
     * Populates the <code>gaps</code> Vector with int arrays of size 2. <br>
     * Each array represents a gap, while the values in the array are the <b>indexs</b>
     * of the particles that have a gap between them. <br>
     * The index is of the particles in the <code>existing_particles</code> array -
     * two sequential particles that are more then 1 frame apart give a GAP
     */
    public void populateGaps() {

        for (int i = 0; i < existing_particles.length - 1; i++) {
            // if two sequential particles are more then 1 frame apart - GAP
            if (existing_particles[i + 1].getFrame() - existing_particles[i].getFrame() > 1) {
                final int[] gap = { i, i + 1 };
                gaps.add(gap);
                num_of_gaps++;
            }
        }
    }

    /**
     * debug helper method
     * 
     * @param s
     */
    private void write(StringBuffer s) {

        final int output = IJ_RESULTS_WINDOW;
        switch (output) {
            case SYSTEM:
                System.out.println(s);
                break;
            case IJ_RESULTS_WINDOW:
            default:
                IJ.log(s.toString());
                break;
        }
    }

    /**
     * Debug method - prints all the gaps in this trajectory (coordinates that defines a gap)
     */
    void printGaps() { // NO_UCD (unused code)
        if (gaps == null) {
            return;
        }
        final Object[] gaps_tmp = gaps.toArray();
        for (int i = 0; i < num_of_gaps; i++) {
            write(new StringBuffer(Math.round((this.existing_particles[((int[]) gaps_tmp[i])[0]]).iY)));
            write(new StringBuffer(","));
            write(new StringBuffer(Math.round((this.existing_particles[((int[]) gaps_tmp[i])[0]]).iX)));
            write(new StringBuffer(","));
            write(new StringBuffer(Math.round((this.existing_particles[((int[]) gaps_tmp[i])[1]]).iY)));
            write(new StringBuffer(","));
            write(new StringBuffer(Math.round((this.existing_particles[((int[]) gaps_tmp[i])[1]]).iX)));
        }
    }

    /**
     * Generates a "ready to print" string with the particles defined
     * in this trajectory in the right order.
     * 
     * @return a String with the info
     */
    @Override
    public String toString() {
        return toStringBuffer().toString();
    }

    /**
     * The method <code>toString()</code> calls this method <br>
     * Generates a "ready to print" StringBuffer with the particles defined
     * in this trajectory in the right order
     * 
     * @return a <code>StringBuffer</code> with the info
     * @see Particle#toStringBuffer()
     */
    public StringBuffer toStringBuffer() {
        final StringBuffer s = new StringBuffer();
        for (int i = 0; i < existing_particles.length; i++) {
            s.append(existing_particles[i].toStringBuffer());
        }
        s.append("\n");
        return s;
    }

}
