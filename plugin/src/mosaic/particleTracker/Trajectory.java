package mosaic.particleTracker;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;

import java.awt.Color;
import java.util.ArrayList;

import mosaic.core.detection.Particle;

/**
 * Defines a Trajectory that is basically an array of sequential <code>Particle</code>s.
 * <br>Trajectory class has methods to display and anllyse this trajectory
 * @see Particle 
 */
public class Trajectory 
{
    private final static int SYSTEM = 0;
    private final static int IJ_RESULTS_WINDOW = 1;
    
    public int stop_frame;
    public int start_frame;
    public Particle[] existing_particles;       // holds all particles of this trajetory in order
    public int length;                         // number of frames this trajectory spans on
    public ImagePlus original_imp;
    
    ArrayList<int[]> gaps = new ArrayList<int[]>();     // holds arrays (int[]) of size 2 that holds  
    // 2 indexs of particles in the existing_particles.
    // These particles are the start and end points of a gap 
    // in this trajectory
    int num_of_gaps = 0;

    public int serial_number;                  // serial number of this trajectory (for report and display)
    public boolean to_display = true;          // flag for display filter
    public Color color;                     // the display color of this Trajectory
    public Roi mouse_selection_area;           // The Roi area where a mouse click will select this trajectory
    public Roi focus_area;                     // The Roi for focus display of this trajectory

    
    double scaling[];

    /**
     * Constructor.
     * <br>Constructs a Trajectory from the given <code>Particle</code> array.
     * <br>Sets its length according to information of the first and last particles
     * <br>Sets its <code>Color</code> to default (red) 
     * @param particles the array containing all the particles defining this Trajectory
     */
    public Trajectory(Particle[] particles, ImagePlus originalImp) {
        this.original_imp = originalImp;
        this.existing_particles = particles;
        // the length is the last trajectory frame - the first frame (first frame can be 0) 
        this.length = this.existing_particles[this.existing_particles.length-1].getFrame() - 
        this.existing_particles[0].getFrame();
        color = Color.red; //default
    }

    /**
     * 
     * Is the trajectory to show
     * 
     * @return true if is to display
     */
    
    public boolean toDisplay()
    {
        return to_display; 
    }
    
    /**
     * Set the <code>scaling</code> for this trajectory
     */
    public void setScaling(double scaling_[])
    {
        scaling = scaling_;
    }
    
    /**
     * Set the <code>focus_area</code> for this trajectory - it defines the area (ROI) focused
     * on when the user selects this trajectory to focus on
     * <br>The <code>focus_area</code> is an rectangular ROI that engulfs this trajectory
     * with 8 pixels margin from each edge
     * @see TrajectoryStackWindow#mousePressed(MouseEvent)
     */
    public void setFocusArea() {

        // there is no image so set focus does not make sense
        
        if (original_imp == null)
            return;
        
        /* find the min and max values of the x and y positions */
        float min_x = this.existing_particles[0].x;
        float min_y = this.existing_particles[0].y; 
        float max_x = this.existing_particles[0].x;
        float max_y = this.existing_particles[0].y; 
        for (int i = 0; i<this.existing_particles.length; i++){
            min_x = Math.min(this.existing_particles[i].x, min_x);
            min_y = Math.min(this.existing_particles[i].y, min_y);
            max_x = Math.max(this.existing_particles[i].x, max_x);
            max_y = Math.max(this.existing_particles[i].y, max_y);
        }

        /* set the focus area x, y , height, width to give focus area bigger by 8 pixels 
         * then minimal rectangle surroundings this trajectory */ 

        // X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
        // (0,0) is the upper left corner; x is vertical top to bottom, y is horizontal left to right           
        int focus_x = Math.max((int)min_x - 8, 0);
        int focus_y = Math.max((int)min_y - 8, 0);
        int focus_height = (int)max_y - focus_y + 8;
        int focus_width = (int)max_x - focus_x + 8;         
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
     * on which a mouse click will add this trajectory as a candidate for selection
     * <br>When this trajectory is selected with a mouse click this ROI is highlighted for the user
     * to see his selection.
     * <br>The <code>mouse_selection_area</code> is an rectangular ROI that engulfs this trajectory
     * with 1 pixel margin from each edge
     * @see TrajectoryStackWindow#mousePressed(MouseEvent)
     */
    public void setMouseSelectionArea () 
    {
        /* find the min and max values of the x and y positions */
        float min_x = this.existing_particles[0].x;
        float min_y = this.existing_particles[0].y; 
        float max_x = this.existing_particles[0].x;
        float max_y = this.existing_particles[0].y;
        for (int i = 0; i<this.existing_particles.length; i++)
        {
            min_x = Math.min(this.existing_particles[i].x, min_x);
            min_y = Math.min(this.existing_particles[i].y, min_y);
            max_x = Math.max(this.existing_particles[i].x, max_x);
            max_y = Math.max(this.existing_particles[i].y, max_y);
        }

        /* set the focus area x, y , height, width to give focus area bigger by 1 pixel 
         * then minimal rectangle surroundings this trajectory */ 

        // X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
        // (0,0) is the upper left corner; x is vertical top to bottom, y is horizontal left to right
        int focus_x = (int)min_x - 1;
        int focus_y = (int)min_y - 1;
        int focus_height = (int)max_x - focus_x + 1;
        int focus_width = (int)max_y - focus_y + 1;
        this.mouse_selection_area = new Roi(focus_x, focus_y, focus_height, focus_width);   

    }

    /**
     * Populates the <code>gaps</code> Vector with int arrays of size 2. 
     * <br>Each array represents a gap, while the values in the array are the <b>indexs</b>
     * of the particles that have a gap between them. 
     * <br>The index is of the particles in the <code>existing_particles</code> array - 
     * two sequential particles that are more then 1 frame apart give a GAP
     */
    public void populateGaps() {

        for (int i = 0; i<existing_particles.length-1; i++){
            // if two sequential particles are more then 1 frame apart - GAP 
            if (existing_particles[i+1].getFrame() - existing_particles[i].getFrame() > 1) {
                int[] gap = {i, i+1};
                gaps.add(gap);
                num_of_gaps++;
            }
        }
    }

//  private void drawStatic(Graphics g, ImageCanvas ic) {
//      int i;
//      g.setColor(this.color);
//      for (i = 0; i<this.existing_particles.length-1; i++) {
//          if (this.existing_particles[i+1].getFrame() - this.existing_particles[i].getFrame() > 1) {                     
//              g.setColor(Color.red); //gap
//          }
//          g.drawLine(ic.screenXD(this.existing_particles[i].y), 
//                  ic.screenYD(this.existing_particles[i].x), 
//                  ic.screenXD(this.existing_particles[i+1].y), 
//                  ic.screenYD(this.existing_particles[i+1].x));
//
//          g.setColor(this.color);                         
//      }
//      //mark death of particle
//      if((this.existing_particles[this.existing_particles.length-1].getFrame()) < frames_number - 1){
//          g.fillOval(ic.screenXD(this.existing_particles[this.existing_particles.length-1].y), 
//                  ic.screenYD(this.existing_particles[this.existing_particles.length-1].x), 5, 5);
//      }
//  }

    /**
     * Converts a floating-point offscreen x-coordinate (particle position) to a <code>traj_stack</code>
     * actual screen x-coordinate as accurate as possible according to the magnification of the 
     * display while taking into account that the <code>traj_stack</code> display can be only a part
     * of the original image 
     * <br> since ImageJ doesn't work with floating point precision - rounding is also applied 
     * @param particle_position floating-point offscreen x-coordinate (particle position <b>Y</b>)
     * @param magnification the magnification factor for the <code>traj_stack</code>
     * @return the converted coordinate
     */
//  private int getXDisplayPosition(float particle_position, int magnification) {
//
//      int roi_x = 0;
//      if (traj_stack.getHeight() != original_imp.getStack().getHeight() || 
//              traj_stack.getWidth() != original_imp.getStack().getWidth()) {
//          roi_x = IJ.getImage().getRoi().getBounds().x;
//      }           
//      particle_position = (particle_position-roi_x)*magnification + (float)(magnification/2.0) - (float)0.5;
//      return Math.round(particle_position);
//  }

    /**
     * Converts a floating-point offscreen y-coordinate (particle position) to a <code>traj_stack</code>
     * actual screen y-coordinate as accurate as possible according to the magnification of the 
     * display while taking into account that the <code>traj_stack</code> display can be only a part
     * of the original image 
     * <br> since ImageJ doesn't work with floating point precision - rounding is also applied 
     * @param particle_position floating-point offscreen y-coordinate (particle position <b>X</b>)
     * @param magnification the magnification factor for the <code>traj_stack</code>
     * @return the converted coordinate
     */
//  private int getYDisplayPosition(float particle_position, int magnification) {
//
//      int roi_y = 0;
//      if (traj_stack.getHeight() != original_imp.getStack().getHeight() || 
//              traj_stack.getWidth() != original_imp.getStack().getWidth()) {
//          roi_y = IJ.getImage().getRoi().getBounds().y;
//      }   
//      particle_position = (particle_position-roi_y)*magnification + (float)(magnification/2.0) - (float)0.5;
//      return Math.round(particle_position);
//  }
    
    /**
     * debug helper method
     * @param s
     */ 
    private void write(StringBuffer s) {

        int output = IJ_RESULTS_WINDOW; 
        switch (output) {
        case SYSTEM: 
            System.out.println(s);
            break;
        case IJ_RESULTS_WINDOW:
            IJ.log(s.toString());
            break;
        }       
    }   
    
    /**
     * Debug method - prints all the gaps in this trajectory (coordinates that defines a gap)
     */
    void printGaps() {
        if (gaps == null) return;           
        Object[] gaps_tmp = gaps.toArray();
        for (int i = 0; i<num_of_gaps; i++) {               
            write(new StringBuffer(Math.round((this.existing_particles[((int[])gaps_tmp[i])[0]]).y)));
            write(new StringBuffer(","));
            write(new StringBuffer(Math.round((this.existing_particles[((int[])gaps_tmp[i])[0]]).x)));
            write(new StringBuffer(","));
            write(new StringBuffer(Math.round((this.existing_particles[((int[])gaps_tmp[i])[1]]).y)));
            write(new StringBuffer(","));
            write(new StringBuffer(Math.round((this.existing_particles[((int[])gaps_tmp[i])[1]]).x))); 
        }
    }

    /**
     * Creates and show a dialog for the user the select the parameter of the particles to plot 
     * @return the <b>position</b> of the parameter the user selected in the <code>Particle.all_params</code>
     * array. -1 if the user cancelled the dialo
     */
    public int getUserParamForPlotting() {

        GenericDialog plot_dialog = new GenericDialog("Choose particle param to plot");         

        String[] param_list = new String [this.existing_particles[0].all_params.length];        
        for (int i = 0; i<param_list.length; i++) {
            param_list[i] = "" + (i+1);
        }
        plot_dialog.addChoice("Select Particle info", param_list, "1");
        plot_dialog.showDialog();
        if (plot_dialog.wasCanceled()) return -1;
        int param_choice = plot_dialog.getNextChoiceIndex();
        return param_choice;
    }

    /**
     * creates a <code>PlotWindow</code> and plots the given param position in the 
     * <code>Particle.all_params</code> array of the particles along this trajectory
     * The X values are the frame number of the particle
     * The Y values are the <code>Particle.all_params[param_choice]</code>
     * @param param_choice the <b>position</b> of the parameter to plot in the Particle.all_params array 
     */
    public void plotParticleAlongTrajectory(int param_choice) {

        if (param_choice >= this.existing_particles[0].all_params.length || param_choice < 0) {
            IJ.error("plotParticleAlongTrajectory\n" +
                    "The given parameter choice (" + (param_choice + 1) + ") does not exits");
            return;
        }
        double[] x_values = new double[this.existing_particles.length];
        for (int i = 0; i<this.existing_particles.length; i++) {
            x_values[i] = this.existing_particles[i].getFrame();
        }
        double[] y_values = new double[this.existing_particles.length];
        for (int i = 0; i<this.existing_particles.length; i++) {
            y_values[i] = Double.parseDouble(this.existing_particles[i].all_params[param_choice]);
        }           
        Plot pw = new Plot("Particle Data along trajectory " + this.serial_number, 
                "frame number", "param number " + (param_choice+1) + " value", x_values, y_values);     
        pw.draw();
    }

    /** 
     * Generates a "ready to print" string with the particles defined 
     * in this trajectory in the right order. 
     * @return a String with the info
     */
    public String toString() {
        return toStringBuffer().toString();
    }

    /**
     * The method <code>toString()</code> calls this method
     * <br>Generates a "ready to print" StringBuffer with the particles defined 
     * in this trajectory in the right order 
     * @return a <code>StringBuffer</code> with the info
     * @see Particle#toStringBuffer()
     */     
    public StringBuffer toStringBuffer() {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i< existing_particles.length; i++) {
            s.append(existing_particles[i].toStringBuffer());
            //              s.append(evaluateMomentaAfterDeath(existing_particles[i]));
        }
        s.append("\n");
        return s;
    }

}

