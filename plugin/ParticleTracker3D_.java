package ij.plugin;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.Duplicater;
import ij.process.*;

import ij.text.*;
import ij.measure.*;
import ij.gui.*;
import ij.io.FileInfo;
import ij.io.SaveDialog;
import ij.io.OpenDialog;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;



/**
 * <h2>ParticleTracker</h2>
 * <h3>An ImageJ Plugin for particles detection and tracking from digital videos</h3>
 * <p>This class implements a to 3d extended feature point detection and tracking algorithm as described in:
 * <br>I. F. Sbalzarini and P. Koumoutsakos. 
 * <br>Feature point tracking and trajectory analysis for video imaging in cell biology. 
 * <br>J. Struct. Biol., 151(2): 182�195, 2005.
 * <p>Any publications that made use of this plugin should cite the above reference.
 * <br>This helps to ensure the financial support of our project at ETH and will 
 * enable us to provide further updates and support.
 * <br>Thanks for your help!</p>
 * <br>For more information go <a href="http://weeman.inf.ethz.ch/particletracker/">here</a>
 * 
 * <p><b>Disclaimer</b>
 * <br>IN NO EVENT SHALL THE ETH BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, 
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND
 * ITS DOCUMENTATION, EVEN IF THE ETH HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * THE ETH SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. 
 * THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE ETH HAS NO 
 * OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.<p>
 * 
 * @version 1.2. Feb, 2009 (requires: ImageJ 1.36b and Java 5 or higher)
 * @author Guy Levy - Academic guest at the <a href="http://www.cbl.ethz.ch/">Computational Biophysics Lab<a>, ETH Zurich
 * @author Janick Cardinale, PhD Student at CBL, ETHZ
 */
public class ParticleTracker3D_ implements PlugInFilter, Measurements, ActionListener, AdjustmentListener   {	

	private final static int SYSTEM = 0;
	private final static int IJ_RESULTS_WINDOW = 1;
	public static final int NO_PREPROCESSING = 0, BOX_CAR_AVG = 1, BG_SUBTRACTOR = 2, LAPLACE_OP = 3;  
	public static final int ABS_THRESHOLD_MODE = 0, PERCENTILE_MODE = 1;
	public ImageStack stack ,traj_stack;	
	public StackConverter sc;
	public ImagePlus original_imp;
	public float global_max, global_min;
	public MyFrame[] frames;
	public Vector<Trajectory> all_traj;// = new Vector();
	public int number_of_trajectories, frames_number, slices_number;
	public String title;

	/* user defined parameters */
	public double cutoff = 3.0; 		// default
	public float percentile = 0.001F; 	// default (user input/100)
	public int absIntensityThreshold = 0; //user input 
	public int radius = 3; 				// default
	public int linkrange = 2; 			// default
	public double displacement = 10.0; 	// default
	int number_of_threads = 4;
	int threshold_mode = PERCENTILE_MODE; 
	public GenericDialog gd;

	/*	image Restoration vars	*/
	public short[][] binary_mask;
	public float[][] weighted_mask;
	public int[][] mask;
	public float lambda_n = 1;
	public int preprocessing_mode = BOX_CAR_AVG;	
	/* flags */	
	public boolean text_files_mode = false;
	public boolean only_detect = false;
	private boolean frames_processed = false;

	/* results display and file */	
	private int trajectory_tail;
	private int magnification_factor = 6;
	private int chosen_traj = -1;
	public ResultsWindow results_window;
	public PrintWriter print_writer = null;
	public PreviewCanvas preview_canvas = null;

	/* preview vars */
	public Button preview, save_detected;
	public Scrollbar preview_scrollbar;
	public Label previewLabel = new Label("");
	public int preview_slice_calculated;


	/* vars for text_files_mode*/
	public String files_dir;
	String[] files_list;
	boolean momentum_from_text, zcoord_from_text;	
	int max_coord = 0;			// max value of the loaded particle coordinates


	/** 
	 * This method sets up the plugin filter for use.
	 * <br>It is called by ImageJ upon selection of the plugin from the menus and the returned value is
	 * used to call the <code>run(ImageProcessor ip)</code> method. 
	 * <br>The <ocde>arg</code> is a string passed as an argument to the plugin, can also be an empty string. 
	 * <br>Different commands from the plugin menu call the same plugin class with a different argument.
	 * <ul>
	 * <li> "" (empty String) - the plugin will work in regular full default mode
	 * <li> "about" - will call the <code>showAbout()</code> method and return <code>DONE</code>, 
	 * meaning without doing anything else	
	 * <li> "only_detect" - the plugin will work in detector only mode and unlike the regular 
	 * mode will allow input of only one image 
	 * </ul>
	 * The argument <code>imp</code> is passed by ImageJ - the currently active image is passed.
	 * @param arg A string command that determines the mode of this plugin - can be empty
	 * @param imp The ImagePlus that is the original input image sequence - 
	 * if null then <code>text_files_mode</code> is activated after an OK from the user 
	 * @return a flag word that represents the filters capabilities according to arg String argument
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	public int setup(String arg, ImagePlus imp) {		
		if(IJ.versionLessThan("1.38u")){
			return DONE;
		}
		if (arg.equals("about")) {
			showAbout(); 
			return DONE;		
		}

		if (arg.equals("only_detect")) {
			only_detect = true;
		}
		
		this.original_imp = imp;	
		
		if (imp==null && !only_detect) {			
			if (IJ.showMessageWithCancel("Text Files Mode", "Do you want to load particles positions from text files?")) {				
				text_files_mode = true;
				return NO_IMAGE_REQUIRED;
			}
			IJ.error("You must load an Image Sequence or Movie first");            
			return DONE;
		}
		if (imp==null) {
			IJ.error("You must load an Image Sequence or Movie first");            
			return DONE;
		}
		if (only_detect && this.original_imp.getStackSize() == 1) {
			return DOES_ALL+NO_CHANGES+SUPPORTS_MASKING;
		}
		return DOES_ALL+NO_CHANGES+SUPPORTS_MASKING+PARALLELIZE_STACKS;
	}

	/**
	 * This method runs the plugin, what implemented here is what the plugin actually
	 * does. It takes the image processor it works on as an argument. 
	 * <br>In this implementation the processor is not used so that the original image is left unchanged. 
	 * <br>The original image is locked while the plugin is running.
	 * <br>This method is called by ImageJ after <code>setup(String arg, ImagePlus imp)</code> returns  
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	public void run(ImageProcessor ip) {

		initializeMembers();

		generatePreviewCanvas();

		/* get user defined params and set more initial params accordingly 	*/	
		if (!getUserDefinedParams()) return;				

		if (!processFrames()) return;		

		if (text_files_mode) {

			/* create an ImagePlus object to hold the particle information from the text files*/
			original_imp = new ImagePlus("From text files", createStackFromTextFiles());
		}

		/* link the particles found */
		IJ.showStatus("Linking Particles");		
		linkParticles();
		IJ.freeMemory();

		/* generate trajectories */		 
		IJ.showStatus("Generating Trajectories");
		generateTrajectories();
		IJ.freeMemory();
		if(IJ.isMacro()) {
			/* Write data to disk */
			writeDataToDisk();
		} else {
			/* Display results window */
			this.trajectory_tail = this.frames_number;
			results_window = new ResultsWindow("Results");
			results_window.configuration_panel.append(getConfiguration().toString());
			results_window.configuration_panel.append(getInputFramesInformation().toString());	
			results_window.text_panel.appendLine("Particle Tracker DONE!");
			results_window.text_panel.appendLine("Found " + this.number_of_trajectories + " Trajectories");
			results_window.setVisible(true);
		}

		IJ.freeMemory();
	}

	/**
	 * Initializes some members needed before going to previews on the user param dialog.
	 */
	private void initializeMembers(){	
		
		if (!text_files_mode){
			// initialize ImageStack stack

			stack = original_imp.getStack();
			this.title = original_imp.getTitle();

			// get global minimum and maximum
			StackStatistics stack_stats = new StackStatistics(original_imp);
			global_max = (float)stack_stats.max;
			global_min = (float)stack_stats.min;
			frames_number = original_imp.getNFrames();
			slices_number = original_imp.getNSlices();
		}else {
			slices_number = 1;
		}
	}

	/** 
	 * Iterates through all frames(ImageProcessors or text files). 
	 * <br>Creates a <code>MyFrame</code> object for each frame according to the input.
	 * <br>If non text mode: gets particles by applying <code>featurePointDetection</code> method on the current frame
	 * <br>if text mode set particles according to input files
	 * <br>Adds every <code>MyFrame</code> created to the <code>frames</code> array
	 * <br>Setes the <code>frames_processed</code> flag to true
	 * <br>If the frames were already processed do nothing and return true
	 * @see MyFrame
	 * @see MyFrame#featurePointDetection()
	 */
	public boolean processFrames() {

		if (frames_processed) return true;

		/* Initialise frames array */
		frames = new MyFrame[frames_number];
		MyFrame current_frame = null;

		for (int frame_i = 0, file_index = 0; frame_i < frames_number; frame_i++, file_index++) {			

			if (text_files_mode) {
				if (files_list[file_index].startsWith(".") || files_list[file_index].endsWith("~")) {
					frame_i--;
					continue;
				}

				// text_files_mode:
				// construct each frame from the conrosponding text file 
				IJ.showStatus("Reading Particles from file " + files_list[file_index] + 
						"(" + (frame_i) + "/" + files_list.length + ")");
				current_frame = new MyFrame(files_dir + files_list[file_index]);
				if (current_frame.particles == null) return false;

			} else {

				// sequence of images mode:
				// construct each frame from the corresponding image
				current_frame = new MyFrame(GetSubStackInFloat(stack, (frame_i) * slices_number + 1, (frame_i + 1) * slices_number), frame_i);

				// Detect feature points in this frame
				IJ.showStatus("Detecting Particles in Frame " + (frame_i+1) + "/" + frames_number);				
				current_frame.featurePointDetection();
			}
			frames[current_frame.frame_number] = current_frame;
			IJ.freeMemory();
		} // for
		frames_processed = true;
		return true;
	}

	/**
	 * Displays a dialog window to get user defined params and selections, 
	 * also initialize and sets other params according to the work mode.
	 * <ul>
	 * <br>For a sequence of images:
	 * <ul>
	 * <li>Gets user defined params:<code> radius, cutoff, precentile, linkrange, displacement</code>
	 * <li>Displays the preview Button and slider
	 * <li>Gives the option to convert the image seq to 8Bit if its color
	 * <li>Initialize and sets params:<code> stack, title, global_max, global_min, mask, kernel</code> 	
	 * <br></ul>
	 * For text_files_mode: 
	 * <ul>
	 * <li>Gets user defined params:<code> linkrange, displacement </code>
	 * <li>Initialize and sets params:<code> files_list, title, frames_number, momentum_from_text </code>
	 * </ul></ul>
	 * @return false if cancel button clicked or problem with input
	 * @see #makeKernel(int)
	 * @see #generateBinaryMask(int)	 
	 */
	boolean getUserDefinedParams() {

		gd = new GenericDialog("Particle Tracker...", IJ.getInstance());
		GenericDialog text_mode_gd;
		momentum_from_text = false;
		zcoord_from_text = false;
		boolean convert = false;
		if (text_files_mode) {
			// gets the input files directory form user
			files_list  = getFilesList();
			if (files_list == null) return false;

			this.title = "text_files";
			frames_number = 0;
			// EACH!! file in the given directory is considered as a frame
			for (int i = 0; i<files_list.length; i++) {
				if (!files_list[i].startsWith(".") && !files_list[i].endsWith("~")) {
					frames_number++;
				}
			}
			text_mode_gd = new GenericDialog("input files info", IJ.getInstance());
			text_mode_gd.addMessage("Please specify the info provided for the Particles...");
			text_mode_gd.addCheckbox("1st position - x (must)", true);
			text_mode_gd.addCheckbox("2nd position - y (must)", true);
			text_mode_gd.addCheckbox("3rd position - z (if 3D data)", true);
			text_mode_gd.addCheckbox("4rd to 8th positions - momentum (m0) to (m4)", false);
			//			text_mode_gd.addCheckbox("3rd or 5th position and on- all other data", true);

			((Checkbox)text_mode_gd.getCheckboxes().elementAt(0)).setEnabled(false);
			((Checkbox)text_mode_gd.getCheckboxes().elementAt(1)).setEnabled(false);
			text_mode_gd.showDialog();
			if (text_mode_gd.wasCanceled()) return false;
			text_mode_gd.getNextBoolean();
			text_mode_gd.getNextBoolean();
			zcoord_from_text = text_mode_gd.getNextBoolean();
			momentum_from_text = text_mode_gd.getNextBoolean();			
		} else {
			gd.addMessage("Particle Detection:");			
			// These 3 params are only relevant for non text_files_mode
			gd.addNumericField("Radius", 3, 0);
			gd.addNumericField("Cutoff", 3.0, 1);

			//	        gd.addChoice("Threshold mode", new String[]{"Absolute Threshold","Percentile"}, "Percentile");
			//	        ((Choice)gd.getChoices().firstElement()).addItemListener(new ItemListener(){
			//				public void itemStateChanged(ItemEvent e) {
			//					int mode = 0;
			//					if(e.getItem().toString().equals("Absolute Threshold")) {
			//						mode = ABS_THRESHOLD_MODE;						
			//					}
			//					if(e.getItem().toString().equals("Percentile")) {
			//						mode = PERCENTILE_MODE;						
			//					}
			//					thresholdModeChanged(mode);
			//				}});

			//	        gd.addNumericField("Percentile", 0.001, 5);
			//	        gd.addNumericField("Percentile / Abs.Threshold", 0.1, 5, 6, " % / Intensity");
			gd.addNumericField("Percentile", 0.1, 5, 6, " %");

			//	        gd.addPanel(makeThresholdPanel(), GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
			//	        gd.addChoice("Preprocessing mode", new String[]{"none", "box-car avg.", "BG Subtraction", "Laplace Operation"}, "box-car avg.");	        
			gd.addPanel(makePreviewPanel(), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));	        

			// check if the original images are not GRAY8, 16 or 32
			if (this.original_imp.getType() != ImagePlus.GRAY8 &&
					this.original_imp.getType() != ImagePlus.GRAY16 &&
					this.original_imp.getType() != ImagePlus.GRAY32) {
				gd.addCheckbox("Convert to Gray8 (recommended)", true);
				convert = true;
			}  
		}

		if (!only_detect) { 
			gd.addMessage("Particle Linking:\n");
			// These 2 params are relevant for both working modes
			gd.addNumericField("Link Range", 2, 0);
			gd.addNumericField("Displacement", 10.0, 2); 
		}

		gd.showDialog();

		// retrieve params from user
		if (!text_files_mode) {
			int rad = (int)gd.getNextNumber();
			//        	this.radius = (int)gd.getNextNumber();
			double cut = gd.getNextNumber(); 
			//            this.cutoff = gd.getNextNumber();   
			float per = ((float)gd.getNextNumber())/100;
			int intThreshold = (int)(per*100+0.5);
			//            this.percentile = ((float)gd.getNextNumber())/100;

			//        	int thsmode = gd.getNextChoiceIndex();
			//        	setThresholdMode(thsmode);

			//        	int mode = gd.getNextChoiceIndex();
			// even if the frames were already processed (particles detected) but
			// the user changed the detection params then the frames needs to be processed again
			if (rad != this.radius || cut != this.cutoff  || (per != this.percentile)){// && intThreshold != absIntensityThreshold || mode != getThresholdMode() || thsmode != getThresholdMode()) {
				if (this.frames_processed) {
					this.frames = null;
					this.frames_processed = false;
				}        		
			}
			this.radius = rad;
			this.cutoff = cut;
			this.percentile = per;
			this.absIntensityThreshold = intThreshold;
			//        	this.preprocessing_mode = mode;


			// add the option to convert only if   images are not GRAY8, 16 or 32
			if (convert) convert = gd.getNextBoolean();

			// create Mask for Dilation with the user defined radius
			generateMasks(this.radius);

		}
		if (only_detect) {
			return false;
		}
		this.linkrange = (int)gd.getNextNumber();
		this.displacement = gd.getNextNumber();

		// if Cancel button was clicked
		if (gd.wasCanceled()) return false;

		// if user choose to convert reset stack, title, frames number and global min, max
		if (convert) {
			sc = new StackConverter(original_imp);
			sc.convertToGray8();
			stack = original_imp.getStack();
			this.title = original_imp.getTitle();
			StackStatistics stack_stats = new StackStatistics(original_imp);
			global_max = (float)stack_stats.max;
			global_min = (float)stack_stats.min;
			frames_number = original_imp.getNFrames(); //??maybe not necessary
		}
		return true;
	}

	private void thresholdModeChanged(int aThresholdMode) {
		setThresholdMode(aThresholdMode);
		if(aThresholdMode == ABS_THRESHOLD_MODE) {
			int defaultIntensity = (int)(global_max - (global_max-global_min) / 5);
			((TextField)gd.getNumericFields().elementAt(2)).setText("" + defaultIntensity);
		}
		if(aThresholdMode == PERCENTILE_MODE) {
			((TextField)gd.getNumericFields().elementAt(2)).setText(IJ.d2s(0.1, 5));
		}

	}
	/**
	 * Gets user defined params that are necessary to display the preview of particle detection
	 * and generates the <code>kernel</code> and <code>mask</code> according to these params
	 * @see #makeKernel(int)
	 * @see #generateBinaryMask(int)
	 */
	void getUserDefinedPreviewParams() {

		Vector<TextField> vec = gd.getNumericFields();
		int rad = Integer.parseInt((vec.elementAt(0)).getText());
		double cut = Double.parseDouble((vec.elementAt(1)).getText());
		float per = (Float.parseFloat((vec.elementAt(2)).getText()))/100;
		//		int absIntensity = (int)(per*100+.5f);
		//		int thsmode = ((Choice)gd.getChoices().elementAt(0)).getSelectedIndex();
		//    	int mode = ((Choice)gd.getChoices().elementAt(1)).getSelectedIndex();

		// even if the frames were already processed (particles detected) but
		// the user changed the detection params then the frames needs to be processed again
		if (rad != this.radius || cut != this.cutoff || (per != this.percentile)){// && absIntensity != this.absIntensityThreshold || thsmode != getThresholdMode() || mode != this.preprocessing_mode) {
			if (this.frames_processed) {
				this.frames = null;
				this.frames_processed = false;
			}        		
		}
		this.radius = rad;
		this.cutoff = cut;
		this.percentile = per;
		//    	this.preprocessing_mode = mode;
		//    	this.absIntensityThreshold = absIntensity;
		//    	this.setThresholdMode(thsmode);
		generateMasks(this.radius);
	}

	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private void showAbout() {
		IJ.showMessage("ParticleTracker...",
				"An ImageJ Plugin for particles detection and tracking from digital videos.\n" +
				"The plugin implements an extended version of the feature point detection and tracking algorithm as described in:\n" +
				"I. F. Sbalzarini and P. Koumoutsakos.\n" +
				"Feature point tracking and trajectory analysis for video imaging in cell biology.\n" +
				"J. Struct. Biol., 151(2): 182�195, 2005.\n" +
				"Any publications that made use of this plugin should cite the above reference.\n" +
				"This helps to ensure the financial support of our project at ETH and will enable us to provide further updates and support.\n" +
				"Thanks for your help!\n" +
				"Written by: Guy Levy, Extended by: Janick Cardinale" +
				"Version: 1.0. January, 2008\n" +
				"Requires: ImageJ 1.38u or higher and Java 5\n" +
				"For more information go to http://weeman.inf.ethz.ch/particletracker/"            
		);
	}

	/**
	 * Defines a Trajectory that is basically an array of sequential <code>Particle</code>s.
	 * <br>Trajectory class has methods to display and anllyse this trajectory
	 * @see Particle 
	 */
	public class Trajectory {

		Particle[] existing_particles;		// holds all particles of this trajetory in order
		int length; 						// number of frames this trajectory spans on

		ArrayList<int[]> gaps = new ArrayList<int[]>(); 	// holds arrays (int[]) of size 2 that holds  
		// 2 indexs of particles in the existing_particles.
		// These particles are the start and end points of a gap 
		// in this trajectory
		int num_of_gaps = 0;

		int serial_number;					// serial number of this trajectory (for report and display)
		boolean to_display = true;			// flag for display filter
		Color color;						// the display color of this Trajectory
		Roi mouse_selection_area;			// The Roi area where a mouse click will select this trajectory
		Roi focus_area;						// The Roi for focus display of this trajectory


		/**
		 * Constructor.
		 * <br>Constructs a Trajectory from the given <code>Particle</code> array.
		 * <br>Sets its length according to information of the first and last particles
		 * <br>Sets its <code>Color</code> to default (red) 
		 * @param particles the array containing all the particles defining this Trajectory
		 */
		public Trajectory(Particle[] particles) {

			this.existing_particles = particles;
			// the length is the last trjectory frame - the first frame (first frame can be 0) 
			this.length = this.existing_particles[this.existing_particles.length-1].frame - 
			this.existing_particles[0].frame;
			color = Color.red; //default
		}

		/**
		 * Set the <code>focus_area</code> for this trajectory - it defines the area (ROI) focused
		 * on when the user selects this trajectory to focus on
		 * <br>The <code>focus_area</code> is an rectangular ROI that engulfs this trajectory
		 * with 8 pixels margin from each edge
		 * @see TrajectoryStackWindow#mousePressed(MouseEvent)
		 */
		private void setFocusArea() {

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
			int focus_x = Math.max((int)min_y - 8, 0);
			int focus_y = Math.max((int)min_x - 8, 0);
			int focus_height = (int)max_x - focus_y + 8;
			int focus_width = (int)max_y - focus_x + 8;			
			// make sure that the -8 or +8 didn�t create an ROI with bounds outside of the window
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
		private void setMouseSelectionArea () {

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

			/* set the focus area x, y , height, width to give focus area bigger by 1 pixel 
			 * then minimal rectangle surroundings this trajectory */ 

			// X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
			// (0,0) is the upper left corner; x is vertical top to bottom, y is horizontal left to right
			int focus_x = (int)min_y - 1;
			int focus_y = (int)min_x - 1;
			int focus_height = (int)max_x - focus_y + 1;
			int focus_width = (int)max_y - focus_x + 1;
			this.mouse_selection_area = new Roi(focus_x, focus_y, focus_width, focus_height);	

		}

		/**
		 * Populates the <code>gaps</code> Vector with int arrays of size 2. 
		 * <br>Each array represents a gap, while the values in the array are the <b>indexs</b>
		 * of the particles that have a gap between them. 
		 * <br>The index is of the particles in the <code>existing_particles</code> array - 
		 * two sequential particles that are more then 1 frame apart give a GAP
		 */
		private void populateGaps() {

			for (int i = 0; i<existing_particles.length-1; i++){
				// if two sequential particles are more then 1 frame apart - GAP 
				if (existing_particles[i+1].frame - existing_particles[i].frame > 1) {
					int[] gap = {i, i+1};
					gaps.add(gap);
					num_of_gaps++;
				}
			}
		}

		private void animate(int magnification) {
			animate(magnification, 0);
		}

		private void animate(int magnification, int removed_frames) {							

			int current_frame;
			int previous_frame = existing_particles[0].frame-removed_frames;
			for (int i = 0; i<existing_particles.length; i++){
				current_frame = existing_particles[i].frame+1-removed_frames;
				while (current_frame - previous_frame > 1) {
					previous_frame++;
					ImageStack previousFrameStack = GetSubStack(traj_stack, (previous_frame-1)*slices_number+1, 
							previous_frame*slices_number);
					draw4Dynamic(previousFrameStack, i, magnification);
					drawGaps4Dynamic(previousFrameStack, i, magnification);
				}
				// if some frames were removed from traj_stack then the frame number 
				// of a particle will not correspond with frame number in the stack.
				// by subtracting the number of removed frames from the particle frame number,
				// we will get the right frame in traj_stack.
				ImageStack currentFrameStack = GetSubStack(traj_stack, (current_frame-1)*slices_number+1, 
						current_frame*slices_number);
				draw4Dynamic(currentFrameStack, i, magnification);
				drawGaps4Dynamic(currentFrameStack, i, magnification);
				previous_frame = current_frame;
			}
		}

		private void drawStatic(Graphics g, ImageCanvas ic) {
			int i;
			g.setColor(this.color);
			for (i = 0; i<this.existing_particles.length-1; i++) {
				if (this.existing_particles[i+1].frame - this.existing_particles[i].frame > 1) {	    			   
					g.setColor(Color.red); //gap
				}
				g.drawLine(ic.screenXD(this.existing_particles[i].y), 
						ic.screenYD(this.existing_particles[i].x), 
						ic.screenXD(this.existing_particles[i+1].y), 
						ic.screenYD(this.existing_particles[i+1].x));

				g.setColor(this.color);							
			}
			//mark death of particle
			if((this.existing_particles[this.existing_particles.length-1].frame) < frames_number - 1){
				g.fillOval(ic.screenXD(this.existing_particles[this.existing_particles.length-1].y), 
						ic.screenYD(this.existing_particles[this.existing_particles.length-1].x), 5, 5);
			}
		}

		/**
		 * 
		 * @param ip: a frame
		 * @param last_frame
		 * @param magnification
		 */
		private void draw4Dynamic(ImageStack is, int last_frame, int magnification){
			for(int s = 1; s <= is.getSize(); s++) {
				ImageProcessor ip = is.getProcessor(s);
				ip.setColor(this.color);
				if (last_frame >= existing_particles.length) {
					//					TODO error	
				}
				if (existing_particles.length < 2) {
					//					TODO error
				}
				ip.setLineWidth(1);
				int i = Math.max(0, last_frame-trajectory_tail);

				ip.moveTo(getXDisplayPosition(this.existing_particles[i].y, magnification), 
						getYDisplayPosition(this.existing_particles[i].x, magnification));
				i++;
				ip.lineTo(getXDisplayPosition(this.existing_particles[i].y, magnification),
						getYDisplayPosition(this.existing_particles[i].x, magnification));
				for (i++; i<= last_frame; i++ ) {
					ip.drawLine(getXDisplayPosition(this.existing_particles[i-1].y, magnification),
							getYDisplayPosition(this.existing_particles[i-1].x, magnification), 
							getXDisplayPosition(this.existing_particles[i].y, magnification),
							getYDisplayPosition(this.existing_particles[i].x, magnification));
				}
			}
		}

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
		private int getXDisplayPosition(float particle_position, int magnification) {

			int roi_x = 0;
			if (traj_stack.getHeight() != original_imp.getStack().getHeight() || 
					traj_stack.getWidth() != original_imp.getStack().getWidth()) {
				roi_x = IJ.getImage().getRoi().getBounds().x;
			}			
			particle_position = (particle_position-roi_x)*magnification + (float)(magnification/2.0) - (float)0.5;
			return Math.round(particle_position);
		}

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
		private int getYDisplayPosition(float particle_position, int magnification) {

			int roi_y = 0;
			if (traj_stack.getHeight() != original_imp.getStack().getHeight() || 
					traj_stack.getWidth() != original_imp.getStack().getWidth()) {
				roi_y = IJ.getImage().getRoi().getBounds().y;
			}	
			particle_position = (particle_position-roi_y)*magnification + (float)(magnification/2.0) - (float)0.5;
			return Math.round(particle_position);
		}

		/**
		 * Draws a red line for all <code>gaps</code> in this <code>trajectory</code> in the range 
		 * of <code>trajectory_tail</code> from the <code>Particle</code> in the given location
		 * (particles[particle_index])
		 * <br>Draws directly (modifys) on the given <code>ip</code> (ImageProcessor)
		 * <br>This method is for generating a <b>progressive</b> trajectory view
		 * @param is Frame to draw the gap on
		 * @param particle_index index of the last particle until which gaps will be drawn 
		 * @param magnification the magnification factor of the image to draw on
		 * @see #getXDisplayPosition(float, int)
		 * @see #getYDisplayPosition(float, int)
		 */
		private void drawGaps4Dynamic(ImageStack is, int particle_index, int magnification) {
			for(int s = 1; s < is.getSize(); s++){
				ImageProcessor ip = is.getProcessor(s);
				if (gaps == null) return;
				/* set ip color to gaps color (RED) */
				ip.setColor(Color.red);
				Object[] gaps_tmp = gaps.toArray();

				/* go over all gaps in this trajectory*/
				for (int i = 0; i<num_of_gaps; i++) {

					// gaps_tmp is now an array of int[] (of size 2)  
					// each int[] holds 2 indexs of particles in the existing_particles.
					int start_particle_index = ((int[])gaps_tmp[i])[0];
					int end_particle_index = ((int[])gaps_tmp[i])[1];

					// only if this gap is in the range of the (particle at the given index - trajectory_tail)
					if (start_particle_index < particle_index && start_particle_index > particle_index - trajectory_tail) {
						ip.drawLine(getXDisplayPosition((this.existing_particles[start_particle_index]).y, magnification),
								getYDisplayPosition((this.existing_particles[start_particle_index]).x, magnification),
								getXDisplayPosition((this.existing_particles[end_particle_index]).y, magnification),
								getYDisplayPosition((this.existing_particles[end_particle_index]).x, magnification));
					}
				}
				/* set ip color back to this trajectory color */
				ip.setColor(this.color);
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
				x_values[i] = this.existing_particles[i].frame;
			}
			double[] y_values = new double[this.existing_particles.length];
			for (int i = 0; i<this.existing_particles.length; i++) {
				y_values[i] = Double.parseDouble(this.existing_particles[i].all_params[param_choice]);
			}			
			PlotWindow pw = new PlotWindow("Particle Data along trajectory " + this.serial_number, 
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
				//				s.append(evaluateMomentaAfterDeath(existing_particles[i]));
			}
			s.append("\n");
			return s;
		}

	}

	/**
	 * Defines a MyFrame that is based upon an ImageProcessor or information from a text file.
	 * <br>MyFrame class has all the necessary methods to detect and report the "real" particles 
	 * for them to be linked.
	 * <br>Some of its methods use global variables defined and calculated in <code>ParticleTracker_</code>
	 * @see ParticleTracker_#mMask
	 * @see ParticleTracker_#kernel
	 * @see ParticleTracker_#cutoff
	 * @see ParticleTracker_#percentile
	 * @see ParticleTracker_#mMaskRadius
	 * @see ParticleTracker_#linkrange
	 * @see ParticleTracker_#mGlobalMax
	 * @see ParticleTracker_#mGlobalMin
	 */
	public class MyFrame {

		//		Particle[] particles;		// an array Particle, holds all the particles detected in this frame
		//									// after particle discrimination holds only the "real" particles
		Vector<Particle> particles;
		int particles_number;		// number of particles initialy detected 
		int real_particles_number;	// number of "real" particles discrimination
		int frame_number;			// Serial number of this frame in the movie (can be 0)
		StringBuffer info_before_discrimination;// holdes string with ready to print info
		// about this frame before particle discrimination 

		/* only relevant to frames representing real images */
		ImageStack original_ips;	// the original image (pointer only)
		//		ImageStack original_fps; // the original image after convertion to float processor (if already float, then a copy)
		//		ImageStack restored_fps; // the floating processor after image restoration
		float threshold;			// threshold for particle detection 
		boolean normalized = false;


		/**
		 * Constructor for ImageProcessor based MyFrame.
		 * <br>All particles and other information will be derived from the given <code>ImageProcessor</code>
		 * by applying internal MyFrame methods  
		 * @param ip the original ImageProcessor upon this MyFrame is based, will remain unchanged!
		 * @param frame_num the serial number of this frame in the movie
		 */
		public MyFrame (ImageStack ips, int frame_num) {
			this.original_ips = ips;
			this.frame_number = frame_num;
		}

		/**
		 * Constructor for <code>text_files_mode</code>. 
		 * <br>constructs a MyFrame from a text file that holds the frame number and 
		 * particles information. unlike the <code>ImageProcessor</code> based constructor, 
		 * all the particles information is set immediately on construction.
		 * @param path full path to the file (including full file name) e.g c:\ImageJ\frame0.txt
		 */
		public MyFrame (String path) {
			loadParticlesFromFile (path);
		}

		/**
		 * ONLY FOR text_files_mode.
		 * <br>Loads particles information for this frame from the file located 
		 * at the given path and adds these particles to the <code>particles</code> array. 
		 * <br>These particles are considered to be "after discrimination".
		 * <br>File must have the word 'frame' (case sensitive) at the beginning of the first line
		 * followed by any number of space characters (\t \n) and the frame number.
		 * <br>Each next line represents a particle in the frame number given at the first line.
		 * <br>Each line must have 2 numbers or more separated by one or more space characters.
		 * <br>The 2 first numbers represents the X and Y coordinates of the particle (respectfully).
		 * <br>The next numbers represent other information of value about the particle
		 * (this information can be plotted later along a trajectory).
		 * <br>The number of parameters must be equal for all particles.
		 * <br>For more about X and Y coordinates (they are not in the usual graph coord) see <code>Particle</code>  
		 * @param path full path to the file (including full file name) e.g c:\ImageJ\frame0.txt
		 * @return false if there was any problem
		 * @see Particle   
		 */
		private boolean loadParticlesFromFile (String path) {

			Vector<String[]> particles_info = new Vector<String[]>(); 	// a vector to hold all particles info as String[]
			String[] particle_info; 				// will hold all the info for one particle (splitted)
			String[] frame_number_info;				// will fold the frame info line (splitted)
			String line;

			try {	        	
				/* open the file */
				BufferedReader r = new BufferedReader(new FileReader(path));

				/* set this frame number from the first line*/
				line = r.readLine();
				if (line == null || !line.startsWith("frame")) {
					IJ.error("File: " + path + "\ndoesnt have the string 'frame' in the begining if the first line");
					return false;
				}
				line = line.trim();
				frame_number_info = line.split("\\s+");
				if (frame_number_info[1] != null) {
					this.frame_number = Integer.parseInt(frame_number_info[1]);
				}

				/* go over all lines, count number of particles and save the information as String */
				while (true) {
					line = r.readLine();		            
					if (line == null) break;
					line = line.trim();
					if (line.startsWith("%"))	line = line.substring(1);
					line = line.trim();
					particles_info.addElement(line.split("\\s+"));
					this.particles_number++;					
				}
				this.real_particles_number = particles_number;
				/* close file */
				r.close();
			}
			catch (Exception e) {
				IJ.error(e.getMessage());
				return false;
			}

			/* initialise the particles array */
			this.particles = new Vector<Particle>();

			Iterator<String[]> iter = particles_info.iterator();
			int counter = 0;

			/* go over all particles String info and construct Particles Ojectes from it*/
			while (iter.hasNext()) {
				particle_info = iter.next();
				
				int info_index=0;
				float x_coord = Float.parseFloat(particle_info[info_index++]);
				float y_coord = Float.parseFloat(particle_info[info_index++]);
				float z_coord;
				
				if(zcoord_from_text) {
					if(particle_info.length < 3) {
						IJ.error("File: " + path + "\ndosent have z coord for all particles.");
						this.particles = null;
						return false;
					}
					z_coord = Float.parseFloat(particle_info[info_index++]);
				} else{ 
					z_coord = 0f;
				}
				
				this.particles.addElement(new Particle(x_coord, y_coord, z_coord, this.frame_number, particle_info));
				max_coord = Math.max((int)Math.max(this.particles.elementAt(counter).x, this.particles.elementAt(counter).y), max_coord);
				
				if (momentum_from_text) {
					if (particle_info.length < info_index+5 || particle_info[info_index] == null || particle_info[info_index+1] == null || 
							particle_info[info_index+2] == null || particle_info[info_index+3] == null ||
							particle_info[info_index+4] == null || particle_info[info_index+5] == null) {
						IJ.error("File: " + path + "\ndosent have momentum values for all particles");
						this.particles = null;
						return false;
					}
					this.particles.elementAt(counter).m0 = Float.parseFloat(particle_info[info_index++]);
					this.particles.elementAt(counter).m1 = Float.parseFloat(particle_info[info_index++]);
					this.particles.elementAt(counter).m2 = Float.parseFloat(particle_info[info_index++]);
					this.particles.elementAt(counter).m3 = Float.parseFloat(particle_info[info_index++]);
					this.particles.elementAt(counter).m4 = Float.parseFloat(particle_info[info_index++]);
				}
				counter++;
			}
			if (particles_info != null) particles_info.removeAllElements();
			return true;
		}

		/**
		 * First phase of the algorithm - time and memory consuming !!
		 * <br>Determines the "real" particles in this frame (only for frame constructed from Image)
		 * <br>Converts the <code>original_ip</code> to <code>FloatProcessor</code>, normalizes it, convolutes and dilates it,
		 * finds the particles, refine their position and filters out non particles
		 * @see ImageProcessor#convertToFloat()
		 * @see MyFrame#normalizeFrameFloat(ImageProcessor)
		 * @see MyFrame#imageRestoration(ImageProcessor)
		 * @see MyFrame#pointLocationsEstimation(ImageProcessor)
		 * @see MyFrame#pointLocationsRefinement(ImageProcessor)
		 * @see MyFrame#nonParticleDiscrimination()
		 */
		public void featurePointDetection () {		

			/* Converting the original imageProcessor to float 
			 * This is a constraint caused by the lack of floating point precision of pixels 
			 * value in 16bit and 8bit image processors in ImageJ therefore, if the image is not
			 * converted to 32bit floating point, false particles get detected */
			
			ImageStack restored_fps = new ImageStack(this.original_ips.getWidth(),this.original_ips.getHeight());


			for(int i = 1; i <= slices_number; i++) {
				//if it is already a float, ImageJ does not create a duplicate
				restored_fps.addSlice(null, this.original_ips.getProcessor(i).convertToFloat().duplicate());
			}

			/* The algorithm is initialized by normalizing the frame*/
			normalizeFrameFloat(restored_fps);
			//			new StackWindow(new ImagePlus("after normalization",restored_fps));


			/* Image Restoration - Step 1 of the algorithm */
			restored_fps = imageRestoration(restored_fps);

//						new StackWindow(new ImagePlus("after restoration",GetSubStackCopyInFloat(restored_fps, 1, 1)));

			/* Estimation of the point location - Step 2 of the algorithm */
			findThreshold(restored_fps, percentile, absIntensityThreshold);
			//			System.out.println("Threshold found: " + threshold);

			pointLocationsEstimation(restored_fps);
//
//						System.out.println("particles after location estimation:");
//						for(Particle p : this.particles) {
//							System.out.println("particle: " + p.toString());
//						}

			/* Refinement of the point location - Step 3 of the algorithm */
			pointLocationsRefinement(restored_fps);
			//			new StackWindow(new ImagePlus("after location ref",restored_fps));
			//			System.out.println("particles after location refinement:");
			//			for(Particle p : this.particles) {
			//				System.out.println("particle: " + p.toString());
			//			}

			/* Non Particle Discrimination(set a flag to particles) - Step 4 of the algorithm */
			nonParticleDiscrimination();

			/* Save frame information before particle discrimination/deletion - it will be lost otherwise*/
			generateFrameInfoBeforeDiscrimination();

			/* remove all the "false" particles from paricles array */
			removeNonParticle();			
		}		


		/**
		 * Normalizes a given <code>ImageProcessor</code> to [0,1].
		 * <br>According to the pre determend global min and max pixel value in the movie.
		 * <br>All pixel intensity values I are normalized as (I-gMin)/(gMax-gMin)
		 * @param ip ImageProcessor to be normalized
		 */
		private void normalizeFrameFloat(ImageStack is) {
			if (!this.normalized) {
				for(int s = 1; s <= is.getSize(); s++){
					float[] pixels=(float[])is.getPixels(s);
					float tmp_pix_value;
					for (int i = 0; i < pixels.length; i++) {
						tmp_pix_value = (pixels[i]-global_min)/(global_max - global_min);
						pixels[i] = (float)(tmp_pix_value);
					}
				}
				normalized = true;
			} else {
				//				TODO set error
			}
		}

		private ImageProcessor cropImageStack2D(ImageProcessor ip) 
		{
			int width = ip.getWidth();
			int newWidth = width - 2*radius;
			FloatProcessor cropped_proc = new FloatProcessor(ip.getWidth()-2*radius, ip.getHeight()-2*radius);
			float[] croppedpx = (float[])cropped_proc.getPixels();
			float[] origpx = (float[])ip.getPixels();
			int offset = radius*width;
			for(int i = offset, j = 0; j < croppedpx.length; i++, j++) {
				croppedpx[j] = origpx[i];		
				if(j%newWidth == 0 || j%newWidth == newWidth - 1) {
					i+=radius;
				}
			}
			return cropped_proc;
		}

		/**
		 * crops a 3D image at all of sides of the imagestack cube. 
		 * @param is a frame to crop
		 * @see pad ImageStack3D
		 * @return the cropped image
		 */
		private ImageStack cropImageStack3D(ImageStack is) {
			ImageStack cropped_is = new ImageStack(is.getWidth()-2*radius, is.getHeight()-2*radius);
			for(int s = radius + 1; s <= is.getSize()-radius; s++) {
				cropped_is.addSlice("", cropImageStack2D(is.getProcessor(s)));
			}
			return cropped_is;
		}

		private ImageProcessor padImageStack2D(ImageProcessor ip) {
			int width = ip.getWidth();
			int newWidth = width + 2*radius;
			FloatProcessor padded_proc = new FloatProcessor(ip.getWidth() + 2*radius, ip.getHeight() + 2*radius);
			float[] paddedpx = (float[])padded_proc.getPixels();
			float[] origpx = (float[])ip.getPixels();
			//first r pixel lines
			for(int i = 0; i < radius*newWidth; i++) {
				if(i%newWidth < radius) { 			//right corner
					paddedpx[i] = origpx[0];
					continue;
				}
				if(i%newWidth >= radius + width) {
					paddedpx[i] = origpx[width-1];	//left corner
					continue;
				}
				paddedpx[i] = origpx[i%newWidth-radius];
			}

			//the original pixel lines and left & right edges				
			for(int i = 0, j = radius*newWidth; i < origpx.length; i++,j++) {
				int xcoord = i%width;
				if(xcoord==0) {//add r pixel rows (left)
					for(int a = 0; a < radius; a++) {
						paddedpx[j] = origpx[i];
						j++;
					}
				}
				paddedpx[j] = origpx[i];
				if(xcoord==width-1) {//add r pixel rows (right)
					for(int a = 0; a < radius; a++) {
						j++;
						paddedpx[j] = origpx[i];
					}
				}
			}

			//last r pixel lines
			int lastlineoffset = origpx.length-width;
			for(int j = (radius+ip.getHeight())*newWidth, i = 0; j < paddedpx.length; j++, i++) {
				if(i%width == 0) { 			//left corner
					for(int a = 0; a < radius; a++) {
						paddedpx[j] = origpx[lastlineoffset];
						j++;
					}
					//					continue;
				}
				if(i%width == width-1) {	
					for(int a = 0; a < radius; a++) {
						paddedpx[j] = origpx[lastlineoffset+width-1];	//right corner
						j++;
					}
					//					continue;
				}
				paddedpx[j] = origpx[lastlineoffset + i % width];
			}
			return padded_proc;
		}

		/**
		 * Before convolving, the image is padded such that no artifacts occure at the edge of an image.
		 * @param is a frame (not a movie!)
		 * @see cropImageStack3D(ImageStack)
		 * @return the padded imagestack to (w+2*r, h+2r, s+2r) by copying the last pixel row/line/slice
		 */
		private ImageStack padImageStack3D(ImageStack is) 
		{
			ImageStack padded_is = new ImageStack(is.getWidth() + 2*radius, is.getHeight() + 2*radius);
			for(int s = 0; s < is.getSize(); s++){
				ImageProcessor padded_proc = padImageStack2D(is.getProcessor(s+1));
				//if we are on the top or bottom of the stack, add r slices
				if(s == 0 || s == is.getSize() - 1) {
					for(int i = 0; i < radius; i++) {
						padded_is.addSlice("", padded_proc);
					}
				} 
				padded_is.addSlice("", padded_proc);
			}

			return padded_is;
		}
		/**
		 * Does the same as padImageStack3D but does not create a new image. It recreates the edge of the
		 * cube (frame).
		 * @see padImageStack3D, cropImageStack3D
		 * @param aIS
		 */
		private void repadImageStack3D(ImageStack aIS){
			if(aIS.getSize() > 1) { //only in the 3D case
				for(int s = 1; s <= radius; s++) {
					aIS.deleteSlice(1);
					aIS.deleteLastSlice();
				}
			}
			for(int s = 1; s <= aIS.getSize(); s++) {
				float[] pixels = (float[])aIS.getProcessor(s).getPixels();
				int width = aIS.getWidth();
				int height = aIS.getHeight();
				for(int i = 0; i < pixels.length; i++) {
					int xcoord = i%width;
					int ycoord = i/width;
					if(xcoord < radius && ycoord < radius) {
						pixels[i] = pixels[radius*width+radius];
						continue;
					}
					if(xcoord < radius && ycoord >= height-radius) {
						pixels[i] = pixels[(height-radius-1)*width+radius];
						continue;
					}
					if(xcoord >= width-radius && ycoord < radius) {
						pixels[i] = pixels[(radius + 1) * width - radius - 1];
						continue;
					}
					if(xcoord >= width-radius && ycoord >= height-radius) {
						pixels[i] = pixels[(height-radius)*width - radius - 1];
						continue;
					}
					if(xcoord < radius) {
						pixels[i] = pixels[ycoord*width+radius];
						continue;
					}
					if(xcoord >= width - radius) {
						pixels[i] = pixels[(ycoord+1)*width - radius - 1];
						continue;
					}
					if(ycoord < radius) {
						pixels[i] = pixels[radius*width + xcoord];
						continue;
					}
					if(ycoord >= height-radius) {
						pixels[i] = pixels[(height-radius-1)*width + xcoord];
					}
				}
			}
			if(aIS.getSize() > 1) {
				for(int s = 1; s <= radius; s++) { //only in 3D case
					aIS.addSlice("", aIS.getProcessor(1).duplicate(),1);
					aIS.addSlice("", aIS.getProcessor(aIS.getSize()).duplicate());
				}
			}
		}

		/**
		 * Corrects imperfections in the given <code>ImageStack</code> by
		 * convolving it (slice by slice, not 3D) with the pre calculated <code>kernel</code>
		 * @param is ImageStack to be restored
		 * @return the restored <code>ImageProcessor</code>
		 * @see Convolver#convolve(ij.process.ImageProcessor, float[], int, int)
		 * @see ParticleTracker_#kernel
		 */
		private ImageStack imageRestoration(ImageStack is) {  
			//remove the clutter
			ImageStack restored = null; 

			//pad the imagestack 	
			if(is.getSize() > 1) {
				//3D mode (we also have to convolve and pad in third dimension)
				restored = padImageStack3D(is);
			} else {
				//we're in 2D mode (separated to do no unnecessary operations).
				ImageProcessor rp= padImageStack2D(is.getProcessor(1));
				restored = new ImageStack(rp.getWidth(), rp.getHeight());
				restored.addSlice("", rp);
			}

			switch(preprocessing_mode){
			case NO_PREPROCESSING:
				GaussBlur3D(restored, 2*lambda_n);
				break;
			case BOX_CAR_AVG:		

				GaussBlur3D(restored, 2*lambda_n);
				//				new StackWindow(new ImagePlus("convolved 3d",GetSubStackCopyInFloat(restored, 1, restored.getSize())));

				boxCarBackgroundSubtractor(restored);//TODO:3D? ->else: pad!
				//				new StackWindow(new ImagePlus("after bg subtraction",GetSubStackCopyInFloat(restored, 1, restored.getSize())));

				break;
			case BG_SUBTRACTOR:
				GaussBlur3D(restored, 2*lambda_n);
				BackgroundSubtractor2_ bgSubtractor = new BackgroundSubtractor2_();
				for(int s = 1; s <= restored.getSize(); s++) {
					//					IJ.showProgress(s, restored.getSize());
					//					IJ.showStatus("Preprocessing: subtracting background...");
					bgSubtractor.SubtractBackground(restored.getProcessor(s), radius*4);
				}
				break;
			case LAPLACE_OP:
				//remove noise then do the laplace op
				GaussBlur3D(restored, 2*lambda_n);
				repadImageStack3D(restored);
				restored = Laplace_Separable_3D(restored);
				break;
			default:
				break;
			}
			if(is.getSize() > 1) {
				//again, 3D crop
				restored = cropImageStack3D(restored);
			} else {
				//2D crop
				ImageProcessor rp= cropImageStack2D(restored.getProcessor(1));
				restored = new ImageStack(rp.getWidth(), rp.getHeight());
				restored.addSlice("", rp);
			}
			//			new StackWindow(new ImagePlus("restored", GetSubStackCopyInFloat(restored, 1, restored.getSize())));            
			return restored;


		}

		private void GaussBlur3D(ImageStack is, float aSigma) {
			float[] vKernel = CalculateNormalizedGaussKernel(aSigma);
			int kernel_radius = vKernel.length / 2;
			int nSlices = is.getSize();
			int vWidth = is.getWidth();
			for(int i = 1; i <= nSlices; i++){
				ImageProcessor restored_proc = is.getProcessor(i);
				Convolver convolver = new Convolver();
				// no need to normalize the kernel - its already normalized
				convolver.setNormalize(false);
				//the gaussian kernel is separable and can done in 3x 1D convolutions!
				convolver.convolve(restored_proc, vKernel, vKernel.length , 1);  
				convolver.convolve(restored_proc, vKernel, 1 , vKernel.length);  
			}
			//2D mode, abort here; the rest is unnecessary
			if(is.getSize() == 1) {
				return;
			}			

			//TODO: which kernel? since lambda_n = 1 pixel, it does not depend on the resolution -->not rescale
			//rescale the kernel for z dimension
			//			vKernel = CalculateNormalizedGaussKernel((float)(aRadius / (original_imp.getCalibration().pixelDepth / original_imp.getCalibration().pixelWidth)));

			kernel_radius = vKernel.length / 2;
			//to speed up the method, store the processor in an array (not invoke getProcessor()):
			float[][] vOrigProcessors = new float[nSlices][];
			float[][] vRestoredProcessors = new float[nSlices][];
			for(int s = 0; s < nSlices; s++) {
				vOrigProcessors[s] = (float[])is.getProcessor(s + 1).getPixelsCopy();
				vRestoredProcessors[s] = (float[])is.getProcessor(s + 1).getPixels();
			}
			//begin convolution with 1D gaussian in 3rd dimension:
			for(int y = kernel_radius; y < is.getHeight() - kernel_radius; y++){
				for(int x = kernel_radius; x < is.getWidth() - kernel_radius; x++){
					for(int s = kernel_radius + 1; s <= is.getSize() - kernel_radius; s++) {
						float sum = 0;
						for(int i = -kernel_radius; i <= kernel_radius; i++) {	        				
							sum += vKernel[i + kernel_radius] * vOrigProcessors[s + i - 1][y*vWidth+x];
						}
						vRestoredProcessors[s-1][y*vWidth+x] = sum;
					}
				}
			}
		}

		public void boxCarBackgroundSubtractor(ImageStack is) {
			Convolver convolver = new Convolver();
			float[] kernel = new float[radius * 2 +1];
			int n = kernel.length;
			for(int i = 0; i < kernel.length; i++)
				kernel[i] = 1f/(float)n;
			for(int s = 1; s <= is.getSize(); s++) {
				ImageProcessor bg_proc = is.getProcessor(s).duplicate();
				convolver.convolveFloat(bg_proc, kernel, 1, n);
				convolver.convolveFloat(bg_proc, kernel, n, 1);
				is.getProcessor(s).copyBits(bg_proc, 0, 0, Blitter.SUBTRACT);
			}
		}

		private ImageStack Laplace_Separable_3D(ImageStack aIS) {        
			float[] vKernel_1D = new float[]{-1, 2, -1};
			ImageStack vResultStack = new ImageStack(aIS.getWidth(), aIS.getHeight());
			int vKernelWidth = vKernel_1D.length;
			int vKernelRadius = vKernel_1D.length / 2;
			int vWidth = aIS.getWidth();
			//
			//in x dimension
			//
			for(int vI = 1; vI <= aIS.getSize(); vI++){
				ImageProcessor vConvolvedSlice = aIS.getProcessor(vI).duplicate();
				Convolver vConvolver = new Convolver();
				vConvolver.setNormalize(false);
				vConvolver.convolve(vConvolvedSlice, vKernel_1D, vKernelWidth , 1);  
				vResultStack.addSlice(null, vConvolvedSlice);
			}
			//
			//in y dimension and sum it to the result
			//
			for(int vI = 1; vI <= aIS.getSize(); vI++){
				ImageProcessor vConvolvedSlice = aIS.getProcessor(vI).duplicate();
				Convolver vConvolver = new Convolver();
				vConvolver.setNormalize(false);
				vConvolver.convolve(vConvolvedSlice, vKernel_1D, 1 , vKernelWidth);  
				vResultStack.getProcessor(vI).copyBits(vConvolvedSlice, 0, 0, Blitter.ADD);
			}
			//			if(true) return vResultStack; //TODO: abort here? yes if gauss3d is scaled in z

			//
			//z dimension
			//
			//first get all the processors of the frame in an array since the getProcessor method is expensive
			float[][] vOriginalStackPixels = new float[aIS.getSize()][];
			float[][] vConvolvedStackPixels = new float[aIS.getSize()][];
			float[][] vResultStackPixels = new float[aIS.getSize()][];
			for(int vS = 0; vS < aIS.getSize(); vS++) {
				vOriginalStackPixels[vS] = (float[])aIS.getProcessor(vS + 1).getPixels();			
				vConvolvedStackPixels[vS] = (float[])aIS.getProcessor(vS + 1).getPixelsCopy();
				vResultStackPixels[vS] = (float[])vResultStack.getProcessor(vS + 1).getPixels();
			}
			for(int vY = 0; vY < aIS.getHeight(); vY++){
				for(int vX = 0; vX < aIS.getWidth(); vX++){
					for(int vS = vKernelRadius; vS < aIS.getSize() - vKernelRadius; vS++) {
						float vSum = 0;
						for(int vI = -vKernelRadius; vI <= vKernelRadius; vI++) {
							vSum += vKernel_1D[vI + vKernelRadius] * vOriginalStackPixels[vS + vI][vY*vWidth+vX];
						}
						vConvolvedStackPixels[vS][vY*vWidth+vX] = vSum;
					}
				}
			}
			//add the results
			for(int vS = vKernelRadius; vS < aIS.getSize() - vKernelRadius; vS++){
				for(int vI = 0; vI < vResultStackPixels[vS].length; vI++){
					vResultStackPixels[vS][vI] += vConvolvedStackPixels[vS][vI];
				}
			}
			//			new StackWindow(new ImagePlus("after laplace copy",GetSubStackCopyInFloat(vResultStack, 1, vResultStack.getSize())));
			return vResultStack;	        
		}

		/**
		 * Estimates the feature point locations in the given <code>ImageProcessor</code>
		 * <br>Any pixel with the same value before and after dilation and value higher
		 * then the pre calculated threshold is considered as a feature point (Particle).
		 * <br>Adds each found <code>Particle</code> to the <code>particles</code> array.
		 * <br>Mostly adapted from Ingo Oppermann implementation
		 * @param ip ImageProcessor, should be after conversion, normalization and restoration 
		 */
		private void pointLocationsEstimation(ImageStack ips) {
			/* do a grayscale dilation */
			ImageStack dilated_ips = dilateGeneric(ips);
//			            new StackWindow(new ImagePlus("dilated ", dilated_ips));
			particles = new Vector<Particle>();
			/* loop over all pixels */ 
			int height = ips.getHeight();
			int width = ips.getWidth();
			for ( int s = 0; s < ips.getSize(); s++) {
				float[] ips_pixels = (float[])ips.getProcessor(s+1).getPixels();
				float[] ips_dilated_pixels = (float[])dilated_ips.getProcessor(s+1).getPixels();
				for (int i = 0; i < height; i++){
					for (int j = 0; j < width; j++){  
						if (ips_pixels[i*width+j] > this.threshold && 
								ips_pixels[i*width+j]  == ips_dilated_pixels[i*width+j] ){ //check if pixel is a local maximum
								
							/* and add each particle that meets the criteria to the particles array */
							//(the starting point is the middle of the pixel and exactly on a focal plane:)
							particles.add(new Particle(i+.5f, j+.5f, s, this.frame_number));

						} 
					}
				}
			}
			particles_number = particles.size();	        	        
		}

		private void pointLocationsRefinement_prop(ImageStack ips) {
			/* Set every value that is smaller than 0 to 0 */		
			for(int s = 0; s < ips.getSize(); s++) {
				for (int i = 0; i < ips.getHeight(); i++) {
					for (int j = 0; j < ips.getWidth(); j++) {
						if(ips.getProcessor(s + 1).getPixelValue(j, i) < 0.0)
							ips.getProcessor(s + 1).putPixelValue(j, i, 0.0);
					}
				}
			}
			if(ips.getSize() == 1){
				CircleGridIntersectionCalculator calc = new CircleGridIntersectionCalculator();
				for(int m = 0; m < this.particles.size(); m++) {
					float epsx = 1f, epsy = 1f, epsz = 1f;
					int iteration_nb=0;


					while ((Math.abs(epsx) > 0.001 || Math.abs(epsy) > 0.001)) {

						iteration_nb++;
						particles.elementAt(m).nbIterations++;
						if(iteration_nb > 200) {
							//						IJ.write("Warning for point " + m + " at x=" + this.particles.elementAt(m).x 
							//								+ ", y=" + this.particles.elementAt(m).y + ", z=" + this.particles.elementAt(m).z
							//								+ ": no convergence in point-location-refinement.");
							break;
						}

						this.particles.elementAt(m).m0 = 0.0F;
						this.particles.elementAt(m).m2 = 0.0F;

						epsx = 0.0F;
						epsy = 0.0F;

						float maskCenterX = this.particles.elementAt(m).x;
						float maskCenterY = this.particles.elementAt(m).y;
						calc.setCircle(maskCenterX, maskCenterY, radius);
						//
						// DEBUG
						//					System.out.println("new round "+ iteration_nb+"\n-----------------:\ncircleX = " + calc.getCircleX() + "circleY = " + calc.getCircleY());
						//					for(Point pixel : calc.getPixelToIntersectionPointsMap().keySet()) {
						//						for(Point2D.Float ips: calc.getPixelToIntersectionPointsMap().get(pixel)){
						//							System.out.println(ips.x + " " + ips.y + " ");
						//						}
						//
						//
						//					}
						//					System.out.println("centroids:");
						//					for(Point pixel : calc.getPixelToIntersectionPointsMap().keySet()) {
						//						System.out.println(pixel.x + " " + pixel.y + " " + calc.getPixelToCentroidMap().get(pixel).x + " " + calc.getPixelToCentroidMap().get(pixel).y + " " + 
						//								calc.getPixelToAreaMap().get(pixel));
						//					}
						//
						// DEBUG
						// Iterate through all pixel and check if all their corners are inside the 
						// circle
						for(int i = -radius; i <= radius; i++) {
							for(int j = -radius; j <= radius; j++) {
								float intensity = ips.getProcessor(1).getPixelValue((int)maskCenterY+j, (int)maskCenterX+i);
								if(calc.isInside((int)maskCenterX+i, (int)maskCenterY+j) &&
										calc.isInside((int)maskCenterX+i+1, (int)maskCenterY+j) &&
										calc.isInside((int)maskCenterX+i, (int)maskCenterY+j+1) &&
										calc.isInside((int)maskCenterX+i+1, (int)maskCenterY+j+1)) {
									this.particles.elementAt(m).m0 += intensity;

									this.particles.elementAt(m).m2 += (i+.5f)*(i+.5f) + (j+.5f)*(j+.5f) * intensity;

									epsx += intensity * ((int)maskCenterX+i+0.5f - maskCenterX);
									epsy += intensity * ((int)maskCenterY+j+0.5f - maskCenterY);
								}
							}
						}

						// Iterate through the pixels at the boundary of the circle:
						for(Point pixel: calc.getPixelToAreaMap().keySet()){

							float intensity = ips.getProcessor(1).getPixelValue(pixel.y, pixel.x);
							float area = calc.getPixelToAreaMap().get(pixel);
							Point2D.Float centroid = calc.getPixelToCentroidMap().get(pixel);



							this.particles.elementAt(m).m0 += area*intensity;
							this.particles.elementAt(m).m2 += (centroid.x-maskCenterX)*(centroid.x-maskCenterX) + (centroid.y-maskCenterY)*(centroid.y-maskCenterY) * area * intensity;
							//	this.particles.elementAt(m).m1 += (float)Math.sqrt(s_x*s_x + s_y*s_y) * weight * intensity;
							//	this.particles.elementAt(m).m3 += (float)Math.pow(s_x*s_x + s_y*s_y, 1.5f) * weight * intensity;
							//	this.particles.elementAt(m).m4 += (float)Math.pow(s_x*s_x + s_y*s_y, 2) * weight * intensity;																	

							epsx += intensity * area * (centroid.x-maskCenterX);
							epsy += intensity * area * (centroid.y-maskCenterY);
						}


						epsx /= this.particles.elementAt(m).m0;
						epsy /= this.particles.elementAt(m).m0;
						//					epsz /= this.particles.elementAt(m).m0;

						this.particles.elementAt(m).m2  /= this.particles.elementAt(m).m0;
						//					this.particles.elementAt(m).m1  /= this.particles.elementAt(m).m0;
						//					this.particles.elementAt(m).m3  /= this.particles.elementAt(m).m0;
						//					this.particles.elementAt(m).m4  /= this.particles.elementAt(m).m0;


						this.particles.elementAt(m).x += epsx;
						this.particles.elementAt(m).y += epsy;

					}
				}
			}
		}

		private void pointLocationsRefinement(ImageStack ips) {
			int m, k, l, x, y, z, tx, ty, tz;
			float epsx, epsy, epsz, c;

			int mask_width = 2 * radius +1;
			int image_width = ips.getWidth();
			/* Set every value that is smaller than 0 to 0 */		
			for(int s = 0; s < ips.getSize(); s++) {
				//				for (int i = 0; i < ips.getHeight(); i++) {
				//					for (int j = 0; j < ips.getWidth(); j++) {
				//						if(ips.getProcessor(s + 1).getPixelValue(j, i) < 0.0)
				//							ips.getProcessor(s + 1).putPixelValue(j, i, 0.0);
				//
				//					}
				//				}
				float[] pixels = (float[])ips.getPixels(s+1);
				for(int i = 0; i < pixels.length; i++) {
					if(pixels[i] < 0) {
						pixels[i] = 0f;
					}
				}
			}

			/* Loop over all particles */
			for(m = 0; m < this.particles.size(); m++) {
				this.particles.elementAt(m).special = true;
				this.particles.elementAt(m).score = 0.0F;
				epsx = epsy = epsz = 1.0F;

				while (epsx > 0.5 || epsx < -0.5 || epsy > 0.5 || epsy < -0.5 || epsz < 0.5 || epsz > 0.5) {
					this.particles.elementAt(m).nbIterations++;
					this.particles.elementAt(m).m0 = 0.0F;
					this.particles.elementAt(m).m1 = 0.0F;
					this.particles.elementAt(m).m2 = 0.0F;
					this.particles.elementAt(m).m3 = 0.0F;
					this.particles.elementAt(m).m4 = 0.0F;
					epsx = 0.0F;
					epsy = 0.0F;
					epsz = 0.0F;
					for(int s = -radius; s <= radius; s++) {
						if(((int)this.particles.elementAt(m).z + s) < 0 || ((int)this.particles.elementAt(m).z + s) >= ips.getSize())
							continue;
						z = (int)this.particles.elementAt(m).z + s;
						for(k = -radius; k <= radius; k++) {
							if(((int)this.particles.elementAt(m).x + k) < 0 || ((int)this.particles.elementAt(m).x + k) >= ips.getHeight())
								continue;
							x = (int)this.particles.elementAt(m).x + k;

							for(l = -radius; l <= radius; l++) {
								if(((int)this.particles.elementAt(m).y + l) < 0 || ((int)this.particles.elementAt(m).y + l) >= ips.getWidth())
									continue;
								y = (int)this.particles.elementAt(m).y + l;
								//
								//								c = ips.getProcessor(z + 1).getPixelValue(y, x) * (float)mask[s + radius][(k + radius)*mask_width + (l + radius)];
								c = ((float[])(ips.getPixels(z + 1)))[x*image_width+y] * (float)mask[s + radius][(k + radius)*mask_width + (l + radius)];

								this.particles.elementAt(m).m0 += c;
								epsx += (float)k * c;
								epsy += (float)l * c;
								epsz += (float)s * c;
								this.particles.elementAt(m).m2 += (float)(k * k + l * l + s * s) * c;
								this.particles.elementAt(m).m1 += (float)Math.sqrt(k * k + l * l + s * s) * c;
								this.particles.elementAt(m).m3 += (float)Math.pow(k * k + l * l + s * s, 1.5f) * c;
								this.particles.elementAt(m).m4 += (float)Math.pow(k * k + l * l + s * s, 2) * c;								
							}
						}
					}

					epsx /= this.particles.elementAt(m).m0;
					epsy /= this.particles.elementAt(m).m0;
					epsz /= this.particles.elementAt(m).m0;
					this.particles.elementAt(m).m2  /= this.particles.elementAt(m).m0;
					this.particles.elementAt(m).m1  /= this.particles.elementAt(m).m0;
					this.particles.elementAt(m).m3  /= this.particles.elementAt(m).m0;
					this.particles.elementAt(m).m4  /= this.particles.elementAt(m).m0;

					// This is a little hack to avoid numerical inaccuracy
					tx = (int)(10.0 * epsx);
					ty = (int)(10.0 * epsy);
					tz = (int)(10.0 * epsz);

					if((float)(tx)/10.0 > 0.5) {
						if((int)this.particles.elementAt(m).x + 1 < ips.getHeight())
							this.particles.elementAt(m).x++;
					}
					else if((float)(tx)/10.0 < -0.5) {
						if((int)this.particles.elementAt(m).x - 1 >= 0)
							this.particles.elementAt(m).x--;						
					}
					if((float)(ty)/10.0 > 0.5) {
						if((int)this.particles.elementAt(m).y + 1 < ips.getWidth())
							this.particles.elementAt(m).y++;
					}
					else if((float)(ty)/10.0 < -0.5) {
						if((int)this.particles.elementAt(m).y - 1 >= 0)
							this.particles.elementAt(m).y--;
					}
					if((float)(tz)/10.0 > 0.5) {
						if((int)this.particles.elementAt(m).z + 1 < ips.getSize())
							this.particles.elementAt(m).z++;
					}
					else if((float)(tz)/10.0 < -0.5) {
						if((int)this.particles.elementAt(m).z - 1 >= 0)
							this.particles.elementAt(m).z--;
					}

					if((float)(tx)/10.0 <= 0.5 && (float)(tx)/10.0 >= -0.5 && 
							(float)(ty)/10.0 <= 0.5 && (float)(ty)/10.0 >= -0.5 &&
							(float)(tz)/10.0 <= 0.5 && (float)(tz)/10.0 >= -0.5)
						break;
				}
				//				System.out.println("iterations for particle " + m + ": " + this.particles.elementAt(m).nbIterations);
				this.particles.elementAt(m).x += epsx;
				this.particles.elementAt(m).y += epsy;
				this.particles.elementAt(m).z += epsz;
			}					
		}

		private void pointLocationsRefinement_gaussfit(ImageStack ips) {
			//			/* Set every value that is smaller than 0 to 0 */		
			//			for(int s = 0; s < ips.getSize(); s++) {
			//				for (int i = 0; i < ips.getHeight(); i++) {
			//					for (int j = 0; j < ips.getWidth(); j++) {
			//						if(ips.getProcessor(s + 1).getPixelValue(j, i) < 0.0)
			//							ips.getProcessor(s + 1).putPixelValue(j, i, 0.0);
			//					}
			//				}
			//			}
			float s = 1f;

			for(int m = 0; m < this.particles.size(); m++) {
				float epsx = 1f, epsy = 1f, epsz = 1f;
				int iteration_nb=0;
				while ((Math.abs(epsx) > 0.000001 || Math.abs(epsy) > 0.000001)) {
					iteration_nb++;
					if(iteration_nb > 500) {
						//						IJ.write("Warning for point " + m + " at x=" + this.particles.elementAt(m).x 
						//								+ ", y=" + this.particles.elementAt(m).y + ", z=" + this.particles.elementAt(m).z
						//								+ ": no convergence in point-location-refinement.");

						break;
					}
					float x = this.particles.elementAt(m).x;
					float y = this.particles.elementAt(m).y;

					float num_x = 0f, num_y = 0f;
					float denum = 0f;
					for(int i = (int)x-radius; i <= (int)x+radius; i++) {
						for(int j = (int)y-radius; j <= (int)y+radius; j++) {
							float j_c = j+.5f;
							float i_c = i+.5f;
							float N = (float) Math.exp(-((i_c-x)*(i_c-x))/(2*s*s) - ((j_c-y)*(j_c-y))/(2*s*s));
							float intensity = ips.getProcessor(1).getPixelValue(j,i);
							num_x += i_c * intensity * N;
							num_y += j_c * intensity * N;
							denum += intensity * N;						
						}
					}
					float x_new = num_x/denum;
					float y_new = num_y/denum;

					epsx = x_new - x;
					epsy = y_new - y;

					this.particles.elementAt(m).x = x_new;
					this.particles.elementAt(m).y = y_new;
				}
			}
		}


		/**
		 * The positions of the found particles will be refined according to their momentum terms
		 * <br> Adapted "as is" from Ingo Oppermann implementation
		 * @param ip ImageProcessor, should be after conversion, normalization and restoration
		 */
		private void pointLocationsRefinement_sphere(ImageStack ips) {
			float r = 0.5f;
			float R = radius;
			float epsx, epsy, epsz, c;

			int mask_width = 2 * radius +1;

			/* Set every value that is smaller than 0 to 0 */		
			for(int s = 0; s < ips.getSize(); s++) {
				for (int i = 0; i < ips.getHeight(); i++) {
					for (int j = 0; j < ips.getWidth(); j++) {
						if(ips.getProcessor(s + 1).getPixelValue(j, i) < 0.0)
							ips.getProcessor(s + 1).putPixelValue(j, i, 0.0);
					}
				}
			}

			/* Loop over all particles */
			for(int m = 0; m < this.particles.size(); m++) {
				this.particles.elementAt(m).special = true;
				this.particles.elementAt(m).score = 0.0F;
				epsx = epsy = epsz = 1.0F;
				int iteration_nb = 0;

				while ((Math.abs(epsx) > 0.000001 || Math.abs(epsy) > 0.000001 || Math.abs(epsz) > 0.000001)) {
					iteration_nb++;
					if(iteration_nb > 200) {
						//						IJ.write("Warning for point " + m + " at x=" + this.particles.elementAt(m).x 
						//								+ ", y=" + this.particles.elementAt(m).y + ", z=" + this.particles.elementAt(m).z
						//								+ ": no convergence in point-location-refinement.");

						break;
					}
					//					System.out.println("iterations: "  + iteration_nb);
					this.particles.elementAt(m).m0 = 0.0F;
					this.particles.elementAt(m).m1 = 0.0F;
					this.particles.elementAt(m).m2 = 0.0F;
					this.particles.elementAt(m).m3 = 0.0F;
					this.particles.elementAt(m).m4 = 0.0F;

					epsx = 0.0F;
					epsy = 0.0F;
					epsz = 0.0F;

					float x_tilde = this.particles.elementAt(m).x;
					float y_tilde = this.particles.elementAt(m).y;
					float z_tilde = this.particles.elementAt(m).z;

					float x_hat = (float) (Math.floor(x_tilde)) + .5f;
					float y_hat = (float) (Math.floor(y_tilde)) + .5f;
					float z_hat = (float) (Math.floor(z_tilde)) + .5f;

					float x_diff = x_hat - x_tilde;
					float y_diff = y_hat - y_tilde;
					float z_diff = z_hat - z_tilde;

					if(ips.getSize() == 1){
						//						generateWeightedMask_2D(radius, xDiff, yDiff, zDiff);
						for(int i = -radius; i <= radius; i++) {
							if(x_hat+i < 0 || x_hat+i >= ips.getHeight()) continue;

							for(int j = -radius; j <= radius; j++) {
								if(y_hat+j < 0 || y_hat+j >= ips.getWidth()) continue;

								float intensity = ips.getProcessor(1).getPixelValue((int)y_hat+j,(int)x_hat+i);

								float delta_x = x_diff + i;
								float delta_y = y_diff + j;

								float d = (float) Math.sqrt(delta_x * delta_x + delta_y * delta_y);

								if(d > R + r){ 
									continue;
								}
								if(d < Math.abs(R - r)){
									epsx += intensity*delta_x;
									epsy += intensity*delta_y;
									this.particles.elementAt(m).m0 += intensity;
									this.particles.elementAt(m).m2 += (float)(delta_x*delta_x + delta_y*delta_y) * intensity;
									this.particles.elementAt(m).m1 += (float)Math.sqrt(delta_x*delta_x + delta_y*delta_y) * intensity;
									this.particles.elementAt(m).m3 += (float)Math.pow(delta_x*delta_x + delta_y*delta_y,1.5) * intensity;
									this.particles.elementAt(m).m4 += (float)Math.pow(delta_x*delta_x + delta_y*delta_y, 2) * intensity;	

									continue;
								}

								float d1 = (d*d-r*r+R*R)/(2*d);
								float d2 = (d*d+r*r-R*R)/(2*d);

								float alpha = (float) Math.acos(d1/R);
								float beta = (float) Math.acos(d2/r);

								float A1 = R * R * alpha - d1 * (float)Math.sqrt(R * R - d1 * d1);
								float A2 = r * r * beta - d2 * (float)Math.sqrt(r * r - d2 * d2);
								float A=A1+A2;

								float s1 = (float) ((2*R*Math.pow(Math.sin(alpha),3))/(3*(alpha-Math.sin(alpha)*d1/R)));
								float s2 = (float) ((2*r*Math.pow(Math.sin(beta),3))/(3*(beta-Math.sin(beta)*d2/r)));
								float s = (A1*s1 + A2*(d-s2))/A;

								float s_x = delta_x * s / d;
								float s_y = delta_y * s / d;

								float weight = (float) (A/(Math.PI*r*r));


								this.particles.elementAt(m).m0 += weight*intensity;
								this.particles.elementAt(m).m2 += (float)(s_x*s_x + s_y*s_y) * weight * intensity;
								this.particles.elementAt(m).m1 += (float)Math.sqrt(s_x*s_x + s_y*s_y) * weight * intensity;
								this.particles.elementAt(m).m3 += (float)Math.pow(s_x*s_x + s_y*s_y, 1.5f) * weight * intensity;
								this.particles.elementAt(m).m4 += (float)Math.pow(s_x*s_x + s_y*s_y, 2) * weight * intensity;																	

								epsx += intensity * weight * s_x;
								epsy += intensity * weight * s_y;
							}
						}
					} else{
						//						generateWeightedMask_3D(radius, xDiff, yDiff, zDiff);
					}

					epsx /= this.particles.elementAt(m).m0;
					epsy /= this.particles.elementAt(m).m0;
					epsz /= this.particles.elementAt(m).m0;

					this.particles.elementAt(m).m2  /= this.particles.elementAt(m).m0;
					this.particles.elementAt(m).m1  /= this.particles.elementAt(m).m0;
					this.particles.elementAt(m).m3  /= this.particles.elementAt(m).m0;
					this.particles.elementAt(m).m4  /= this.particles.elementAt(m).m0;


					this.particles.elementAt(m).x += epsx;
					this.particles.elementAt(m).y += epsy;
					this.particles.elementAt(m).z += epsz;

					//					System.out.println(particles.elementAt(m).x + "," + particles.elementAt(m).y);

				}
			}		
		}

		/**
		 * Rejects spurious particles detections such as unspecific signals, dust, or particle aggregates. 
		 * <br>The implemented classification algorithm after Crocker and Grier [68] is based on the
		 * intensity moments of orders 0 and 2.
		 * <br>Particles with lower final score than the user-defined cutoff are discarded 
		 * <br>Adapted "as is" from Ingo Oppermann implementation
		 */
		private void nonParticleDiscrimination() {

			int j, k;
			double score;
			this.real_particles_number = this.particles_number;
			if(this.particles.size() == 1){
				this.particles.elementAt(0).score = Float.MAX_VALUE;
			}
			for(j = 0; j < this.particles.size(); j++) {		
				//				int accepted = 1;
				for(k = j + 1; k < this.particles.size(); k++) {
					score = (double)((1.0 / (2.0 * Math.PI * 0.1 * 0.1)) * 
							Math.exp(-(this.particles.elementAt(j).m0 - this.particles.elementAt(k).m0) *
									(this.particles.elementAt(j).m0 - this.particles.elementAt(k).m0) / (2.0 * 0.1) -
									(this.particles.elementAt(j).m2 - this.particles.elementAt(k).m2) * 
									(this.particles.elementAt(j).m2 - this.particles.elementAt(k).m2) / (2.0 * 0.1)));
					this.particles.elementAt(j).score += score;
					this.particles.elementAt(k).score += score;
				}
				if(this.particles.elementAt(j).score < cutoff) {
					this.particles.elementAt(j).special = false;
					this.real_particles_number--;		
					//					accepted = 0;
				}
				//				System.out.println(j + "\t" + this.particles.elementAt(j).m0 + "\t" + this.particles.elementAt(j).m2 + "\t" + accepted);
			}				
		}

		/**
		 * removes particles that were discarded by the <code>nonParticleDiscrimination</code> method
		 * from the particles array. 
		 * <br>Non particles will be removed from the <code>particles</code> array so if their info is 
		 * needed, it should be saved before calling this method
		 * @see MyFrame#nonParticleDiscrimination()
		 */
		private void removeNonParticle() {

			//	    	Particle[] new_particles = new Particle[this.real_particles_number];
			//	    	int new_par_index = 0;
			//	    	for (int i = 0; i< this.particles.length; i++) {
			//	    		if (this.particles[i].special) {
			//	    			new_particles[new_par_index] = this.particles[i];
			//	    			new_par_index++;
			//	    		}
			//	    	}
			//	    	this.particles = new_particles;
			for(int i = this.particles.size()-1; i >= 0; i--) {
				if(!this.particles.elementAt(i).special) {
					this.particles.removeElementAt(i);
				}
			}
		}

		/**
		 * Finds and sets the threshold value for this frame given the 
		 * user defined percenticle and an ImageProcessor if the thresholdmode is PERCENTILE.
		 * If not, the threshold is set to its absolute (normalized) value. There is only one parameter
		 * used, either percent or aIntensityThreshold depending on the threshold mode.
		 * @param ip ImageProcessor after conversion, normalization and restoration
		 * @param percent the upper rth percentile to be considered as candidate Particles
		 * @param aIntensityThreshold a intensity value which defines a threshold.
		 */
		private void findThreshold(ImageStack ips, double percent, int aIntensityThreshold) {
			if(getThresholdMode() == ABS_THRESHOLD_MODE){
				//the percent parameter corresponds to an absolute value (not percent)
				this.threshold = (float)(aIntensityThreshold - global_min)/(global_max-global_min);
				return;
			}
			int s, i, j, thold, width;
			width = ips.getWidth();

			/* find this ImageStacks min and max pixel value */
			float min = 0f;
			float max = 0f;
			if(ips.getSize() > 1) {
				StackStatistics sstats = new StackStatistics(new ImagePlus(null,ips));
				min = (float)sstats.min;
				max = (float)sstats.max;
			} else { //speeds up the 2d version:
				ImageStatistics istats = ImageStatistics.getStatistics(ips.getProcessor(1), MIN_MAX, null);
				min = (float)istats.min;
				max = (float)istats.max;
			}

			double[] hist = new double[256];
			for (i = 0; i< hist.length; i++) {
				hist[i] = 0;
			}
			for(s = 0; s < ips.getSize(); s++) {
				float[] pixels = (float[])ips.getProcessor(s + 1).getPixels();
				for(i = 0; i < ips.getHeight(); i++) {
					for(j = 0; j < ips.getWidth(); j++) {
						hist[(int)((pixels[i*width+j] - min) * 255.0 / (max - min))]++;
					}
				}				
			}

			for(i = 254; i >= 0; i--)
				hist[i] += hist[i + 1];

			thold = 0;
			while(hist[255 - thold] / hist[0] < percent) {
				thold++;	
				if(thold > 255)
					break;				
			}
			thold = 255 - thold + 1;
			this.threshold = ((float)(thold / 255.0) * (max - min) + min);		
			//			System.out.println("THRESHOLD: " + this.threshold);
		}			

		/**
		 * Dilates a copy of a given ImageProcessor with a pre calculated <code>mask</code>.
		 * Adapted as is from Ingo Oppermann implementation
		 * @param ip ImageProcessor to do the dilation with
		 * @return the dilated copy of the given <code>ImageProcessor</code> 
		 * @see ParticleTracker_#mMask
		 */		
		private ImageStack dilateGeneric(ImageStack ips) {
			FloatProcessor[] dilated_procs = new FloatProcessor[ips.getSize()];
			AtomicInteger z  = new AtomicInteger(-1);
			Vector<Thread> threadsVector = new Vector<Thread>();
			for(int thread_counter = 0; thread_counter < number_of_threads; thread_counter++){
				threadsVector.add(new DilateGenericThread(ips,dilated_procs,z));
			}
			for(Thread t : threadsVector){
				t.start();                               
			}
			for(Thread t : threadsVector){
				try {
					t.join();                                        
				}catch (InterruptedException ie) {
					IJ.showMessage("Calculation interrupted. An error occured in parallel dilation:\n" + ie.getMessage());
				}
			}
			ImageStack dilated_ips = new ImageStack(ips.getWidth(), ips.getHeight());
			for(int s = 0; s < ips.getSize(); s++)
				dilated_ips.addSlice(null, dilated_procs[s]);
			//			new StackWindow(new ImagePlus("dilated image", dilated_ips));

			return dilated_ips;
		}



		private class DilateGenericThread extends Thread{
			ImageStack ips;
			ImageProcessor[] dilated_ips;
			AtomicInteger atomic_z;
			int kernel_width;
			int image_width;
			int image_height;
			int radius;

			public DilateGenericThread(ImageStack is, ImageProcessor[] dilated_is, AtomicInteger z) {
				ips = is;
				dilated_ips = dilated_is;
				atomic_z = z;

				radius = getRadius();
				kernel_width = (getRadius()*2) + 1;
				image_width = ips.getWidth();
				image_height = ips.getHeight();
			}

			public void run() {
				float max;
				int z;
				while((z = atomic_z.incrementAndGet()) < ips.getSize()) {
					//					IJ.showStatus("Dilate Image: " + (z+1));
					//					IJ.showProgress(z, ips.getSize());
					FloatProcessor out_p = new FloatProcessor(image_width, image_height);
					float[] output = (float[])out_p.getPixels();
					float[] dummy_processor = (float[])ips.getPixels(z+1);
					for(int y = 0; y < image_height; y++) {
						for(int x = 0; x < image_width; x++) {
							//little big speed-up:
							if(dummy_processor[y*image_width+x] < threshold) {
								continue;
							}
							max = 0;
							//a,b,c are the kernel coordinates corresponding to x,y,z
							for(int s = -radius; s <= radius; s++ ) {
								if(z + s < 0 || z + s >= ips.getSize())
									continue;
								float[] current_processor_pixels = (float[])ips.getPixels(z+s+1);
								for(int b = -radius; b <= radius; b++ ) {
									if(y + b < 0 || y + b >= ips.getHeight())
										continue;
									for(int a = -radius; a <= radius; a++ ) {
										if(x + a < 0 || x + a >= ips.getWidth())
											continue;
										if(binary_mask[s + radius][(a + radius)* kernel_width+ (b + radius)] == 1) {
											float t;
											if((t = current_processor_pixels[(y + b)* image_width +  (x + a)]) > max) {
												max = t;
											}
										}
									}
								}
							}
							output[y*image_width + x]= max;
						}
					}
					dilated_ips[z] = out_p;
				}
			}
		}

		/**
		 * Generates a "ready to print" string with all the 
		 * particles positions AFTER discrimination in this frame.
		 * @return a <code>StringBuffer</code> with the info
		 */
		private StringBuffer getFrameInfoAfterDiscrimination() {

			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(6);
			nf.setMinimumFractionDigits(6);

			// I work with StringBuffer since its faster than String
			StringBuffer info = new StringBuffer("%\tParticles after non-particle discrimination (");
			info.append(this.real_particles_number);
			info.append(" particles):\n");
			for (int i = 0; i<this.particles.size(); i++) {
				info.append("%\t\t");
				info.append(nf.format(this.particles.elementAt(i).x));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).y));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).z));

				//special version:
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m0));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m1));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m2));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m3));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m4));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).score));

				info.append("\n");

			}
			return info;
		}

		/**
		 * Generates a "ready to print" StringBuffer with all the particles initial
		 * and refined positions BEFORE discrimination in this frame.
		 * <br>sets <code>info_before_discrimination</code> to hold this info
		 * @see #info_before_discrimination
		 */
		private void generateFrameInfoBeforeDiscrimination() {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(6);
			nf.setMinimumFractionDigits(6);

			// I work with StringBuffer since its faster than String
			StringBuffer info = new StringBuffer("% Frame ");
			info.append(this.frame_number);
			info.append(":\n");
			info.append("%\t");
			info.append(this.particles_number);
			info.append(" particles found\n");
			info.append("%\tDetected particle positions:\n");
			for (int i = 0; i<this.particles.size(); i++) {
				info.append("%\t\t");
				info.append(nf.format(this.particles.elementAt(i).original_x));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).original_y));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).original_z));
				info.append("\n");
			}
			info.append("%\tParticles after position refinement:\n");
			for (int i = 0; i<this.particles.size(); i++) {
				info.append("%\t\t");
				info.append(nf.format(this.particles.elementAt(i).x));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).y));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).z));

				//special version:
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m0));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m1));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m2));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m3));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m4));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).score));

				info.append("\n");
			}
			info_before_discrimination = info;
		}

		/**
		 * Generates (in real time) a "ready to print" StringBuffer with this frame 
		 * infomation before and after non particles discrimination
		 * @return a StringBuffer with the info
		 * @see MyFrame#getFrameInfoAfterDiscrimination()
		 * @see #info_before_discrimination
		 */
		public StringBuffer getFullFrameInfo() {
			StringBuffer info = new StringBuffer();
			info.append(info_before_discrimination);
			info.append(getFrameInfoAfterDiscrimination());
			return info;					
		}

		/**
		 * Generates a "ready to print" string that shows for each particle in this frame 
		 * (AFTER discrimination) all the particles it is linked to.
		 * @return a String with the info
		 */	
		public String toString() {			
			return toStringBuffer().toString();
		}

		/**
		 * The method <code>toString()</code> calls this method
		 * <br>Generates a "ready to print" StringBuffer that shows for each particle in this frame 
		 * (AFTER discrimination) all the particles it is linked to.
		 * @return a <code>StringBuffer</code> with the info
		 */	
		public StringBuffer toStringBuffer() {

			// I work with StringBuffer since its faster than String
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(6);
			nf.setMinimumFractionDigits(6);
			StringBuffer sb = new StringBuffer("% Frame ");
			sb.append(this.frame_number);
			sb.append("\n");
			for(int j = 0; j < this.particles.size(); j++) {
				sb.append("%\tParticle ");
				sb.append(j);
				sb.append(" (");
				sb.append(nf.format(this.particles.elementAt(j).x));
				sb.append(", ");
				sb.append(nf.format(this.particles.elementAt(j).y));
				sb.append(", ");		
				sb.append(nf.format(this.particles.elementAt(j).z));
				sb.append(")\n");	
				for(int k = 0; k < linkrange; k++) {
					sb.append("%\t\tlinked to particle ");
					sb.append(this.particles.elementAt(j).next[k]);
					sb.append(" in frame ");
					sb.append((this.frame_number + k + 1));
					sb.append("\n");					
				}
			}
			return sb;
		}

		public Vector<Particle> getParticles(){
			return this.particles;
		}
		/**
		 * Generates (in real time) a "ready to save" <code>StringBuffer</code> with information
		 * about the detected particles defined in this MyFrame.
		 * <br>The format of the returned <code>StringBuffer</code> is the same as expected when 
		 * loading particles information from text files
		 * @param with_momentum if true, the momentum values (m0, m2) are also included
		 * if false - only x and y values are included
		 * @return the <code>StringBuffer</code> with this information
		 * @see MyFrame#loadParticlesFromFile(String) 
		 */
		private StringBuffer frameDetectedParticlesForSave(boolean with_momentum) {

			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(6);
			nf.setMinimumFractionDigits(6);
			StringBuffer info1 = new StringBuffer("frame ");
			info1.append(this.frame_number);
			info1.append("\n");
			for (int i = 0; i<this.particles.size(); i++) {
				info1.append(nf.format(this.particles.elementAt(i).x));
				info1.append(" ");
				info1.append(nf.format(this.particles.elementAt(i).y));		
				info1.append(" ");
				info1.append(nf.format(this.particles.elementAt(i).z));	
				if (with_momentum) {
					info1.append(" ");
					info1.append(nf.format(this.particles.elementAt(i).m0));
					info1.append(" ");
					info1.append(nf.format(this.particles.elementAt(i).m2));					
				}
				info1.append("\n");				
			}
			return info1;
		}

		/**
		 * Creates a <code>ByteProcessor</code> and draws on it the particles defined in this MyFrame 
		 * <br>The background color is <code>Color.black</code>
		 * <br>The color of the dots drawn for each particle is <code>Color.white</code>
		 * <br>particles position have floating point precision but can be drawn only at integer precision - 
		 * therefore the created image is only an estimation
		 * @param width defines the width of the created <code>ByteProcessor</code>
		 * @param height defines the height of the created <code>ByteProcessor</code>
		 * @return the created processor
		 * @see ImageProcessor#drawDot(int, int)
		 */
		private ImageStack createImage(int width, int height, int depth) {
			ImageStack is = new ImageStack(width, height);
			for(int d = 0; d < depth; d++) {
				ImageProcessor ip = new ByteProcessor(width, height);
				ip.setColor(Color.black);
				ip.fill();
				is.addSlice(null, ip);
				ip.setColor(Color.white);
			}
			for (int i = 0; i<this.particles.size(); i++) {
				is.getProcessor(Math.round(this.particles.elementAt(i).z) + 1).drawDot(
						Math.round(this.particles.elementAt(i).y), 
						Math.round(this.particles.elementAt(i).x));
			}
			return is;		
		}


		/**
		 * Creates a <code>ByteProcessor</code> and draws on it the particles defined in this MyFrame 
		 * <br>The background color is <code>Color.black</code>
		 * <br>The color of the dots drawn for each particle is <code>Color.white</code>
		 * <br>particles position have floating point precision but can be drawn only at integer precision - 
		 * therefore the created image is only an estimation
		 * @param width defines the width of the created <code>ByteProcessor</code>
		 * @param height defines the height of the created <code>ByteProcessor</code>
		 * @return the created processor
		 * @see ImageProcessor#drawDot(int, int)
		 */
		private ImageProcessor createImage(int width, int height) {
			ImageProcessor ip = new ByteProcessor(width, height);
			ip.setColor(Color.black);
			ip.fill();
			ip.setColor(Color.white);
			for (int i = 0; i<this.particles.size(); i++) {
				ip.drawDot(Math.round(this.particles.elementAt(i).y), Math.round(this.particles.elementAt(i).x));
			}
			return ip;		
		}
	}

	/**
	 * Defines a particle that holds all the relevant info for it.
	 * A particle is detected in an image or given as input in test file mode 
	 * 		X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
	 * 		(0,0) is the upper left corner
	 *  	x is vertical top to bottom
	 *  	y is horizontal left to right
	 */
	public class Particle {

		float x, y, z; 					// the originally given coordinates - to be refined 
		float original_x , original_y, original_z; 	// the originally given coordinates - not to be changed 		
		int frame; 						// the number of the frame this particle belonges to (can be 0)
		boolean special; 				// a flag that is used while detecting and linking particles
		int[] next; 					// array that holds in position i the particle number in frame i
		// that this particle is linked to  
		int nbIterations = 0; //debug
		/* only relevant to particles detected in images */
		float m0, m1, m2, m3, m4;		// intensity moment
		float score; 					// non-particle discrimination score

		/* only relevant to particles given as input */
		String[] all_params; 			// all params that relate to this particle,
		// 1st 2 should be x and y respectfully

		/**
		 * constructor. 
		 * @param x - original x coordinates
		 * @param y - original y coordinates
		 * @param frame_num - the number of the frame this particle belonges to
		 */
		public Particle (float x, float y, float z, int frame_num) {
			this.x = x;
			this.original_x = x;
			this.y = y;
			this.original_y = y;
			this.z = z;
			this.original_z = z;
			this.special = true;
			this.frame = frame_num;
			this.next = new int[linkrange];
		}

		/**
		 * constructor for particles created from text files.  
		 * @param x - original x coordinates
		 * @param y - original y coordinates
		 * @param frame_num - the number of the frame this particle is in
		 * @param params - all params that relate to this particle, first 2 should be x and y respectfully 
		 */
		public Particle (float x, float y, float z, int frame_num, String[] params) {
			this.x = x;
			this.original_x = x;
			this.y = y;
			this.original_y = y;
			this.z = z;
			this.original_z = z;
			this.all_params = params;
			this.special = true;
			this.frame = frame_num;
			this.next = new int[linkrange];
			this.score = 0.0F;
			this.m0 = 0.0F;
			this.m1 = 0.0F;
			this.m2 = 0.0F;
			this.m3 = 0.0F;
			this.m4 = 0.0F;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {  
			return toStringBuffer().toString();
		}

		/**
		 * The method <code>toString()</code> calls this method
		 * <br>Generates (in real time) a "ready to print" <code>StringBuffer</code> with information
		 * about this Particle:
		 * <ul>
		 * <li> frame
		 * <li> x	
		 * <li> y
		 * <li> m0
		 * <li> m2 
		 * <li> score
		 * </ul>
		 * For text files mode - just prints all the information given for the particles
		 * @return a StringBuffer with this infomation
		 */
		public StringBuffer toStringBuffer() {

			// I work with StringBuffer since its faster than String
			// At the end convert to String and return
			StringBuffer sb = new StringBuffer();
			StringBuffer sp = new StringBuffer(" ");

			// format the number to look nice in print (same number of digits)
			NumberFormat nf = NumberFormat.getInstance();			
			nf.setMaximumFractionDigits(6);
			nf.setMinimumFractionDigits(6);
			sb.append(this.frame);
			if (text_files_mode) {
				for (int i = 0; i<all_params.length; i++) {
					sb.append(sp);
					sb.append(nf.format(Float.parseFloat(all_params[i])));
				}
				sb.append("\n");
			} else {
				sb.append(sp);
				sb.append(nf.format(this.x));
				sb.append(sp);
				sb.append(nf.format(this.y));
				sb.append(sp);
				sb.append(nf.format(this.z));
				sb.append(sp);
				sb.append(nf.format(this.m0));
				sb.append(sp);
				sb.append(nf.format(this.m1));
				sb.append(sp);
				sb.append(nf.format(this.m2));
				sb.append(sp);
				sb.append(nf.format(this.m3));
				sb.append(sp);
				sb.append(nf.format(this.m4));
				sb.append(sp);
				sb.append(nf.format(this.score));
				sb.append("\n");
			}
			return sb;
		}		
	}

	/**
	 * Defines an overlay Canvas for a given <code>ImagePlus</code> on which the non 
	 * filtered found trajectories are displayed for further displaying and analysis options
	 */
	private class TrajectoryCanvas extends ImageCanvas {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 * <br>Creates an instance of TrajectoryCanvas from a given <code>ImagePlus</code>
		 * and <code>ImageCanvas</code>
		 * <br>Displays the detected particles from the given <code>MyFrame</code>
		 * @param aimp
		 */
		private TrajectoryCanvas(ImagePlus aimp) {
			super(aimp);
		}

		/* (non-Javadoc)
		 * @see java.awt.Component#paint(java.awt.Graphics)
		 */
		public void paint(Graphics g) {            
			super.paint(g);
			drawTrajectories(g); 
		}

		/**
		 * Draws each of the trajectories in <code>all_traj</code>
		 * on this Canvas according to each trajectorys <code>to_display</code> value
		 * @param g
		 * @see Trajectory#drawStatic(Graphics, ImageCanvas)
		 */
		private void drawTrajectories(Graphics g) {

			if (g == null) return;
			Iterator<Trajectory> iter = all_traj.iterator();  	   
			// Iterate over all the trajectories 
			while (iter.hasNext()) {
				Trajectory curr_traj = iter.next();	
				// if the trajectory to_display value is true
				if (curr_traj.to_display) {	   		   				   
					curr_traj.drawStatic(g, this);
				}
			}
		}
	}

	/**
	 * Defines an overlay Canvas for a given <code>ImagePlus</code> on which the detected particles from 
	 * a <code>MyFrame</code> are displayed for preview
	 */
	private class PreviewCanvas extends ImageCanvas {

		private static final long serialVersionUID = 1L;
		private MyFrame preview_frame;
		int magnification = 1;

		/**
		 * Constructor.
		 * <br>Creates an instance of PreviewCanvas from a given <code>ImagePlus</code>
		 * <br>Displays the detected particles from the given <code>MyFrame</code>
		 * @param aimp - the given image plus on which the detected particles are displayed
		 * @param preview_f - the <code>MyFrame</code> with the detected particles to display
		 * @param mag - the magnification factor of the <code>ImagePlus</code> relative to the initial
		 */
		private PreviewCanvas(ImagePlus aimp, double mag) {
			super(aimp);
			this.preview_frame = null;
			this.magnification = (int)mag;
		}

		public void setPreviewFrame(MyFrame aPreviewFrame) {
			this.preview_frame = aPreviewFrame;
		}

		/**
		 * Overloaded Constructor.
		 * <br>Creates an instance of PreviewCanvas from a given <code>ImagePlus</code>
		 * <br>Displays the detected particles from the given <code>MyFrame</code>
		 * <br> sets the magnification factor to 1
		 * @param aimp
		 * @param preview_f
		 */
		private PreviewCanvas(ImagePlus aimp) {
			this(aimp, 1);
		}

		/* (non-Javadoc)
		 * @see java.awt.Component#paint(java.awt.Graphics)
		 */
		public void paint(Graphics g) {            
			super.paint(g);
			int frameToDisplay = getFrameNumberFromSlice(this.imp.getCurrentSlice());
			Vector<Particle> particlesToDisplay = null;
			if(frameToDisplay == getFrameNumberFromSlice(preview_slice_calculated)) {
				// the preview display color is set to red
				g.setColor(Color.red);
				if(preview_frame != null){
					particlesToDisplay = preview_frame.particles;
					circleParticles(g, particlesToDisplay);
				}
			}
			if(frames != null){
				particlesToDisplay = frames[frameToDisplay-1].particles;
				// the located particles display color is set to blue
				g.setColor(Color.blue);
				circleParticles(g, particlesToDisplay);
			}

		}
		/**
		 * Inner class method
		 * <br> Invoked from the <code>paint</code> overwritten method
		 * <br> draws a dot and circles the detected particle directly of the given <code>Graphics</code>
		 * @param g
		 */
		private void circleParticles(Graphics g, Vector<Particle> particlesToDisplay) {
			if (particlesToDisplay == null || g == null) return;

			this.magnification = (int)Math.round(original_imp.getWindow().getCanvas().getMagnification());
			// go over all the detected particle 
			for (int i = 0; i< particlesToDisplay.size(); i++) {
				// draw a dot at the detected particle position (oval of hieght and windth of 0)
				// the members x, y of the Particle object are opposite to the screen X and Y axis
				// The x-axis points top-down and the y-axis is oriented left-right in the image plane. 
				g.drawOval(this.screenXD(particlesToDisplay.elementAt(i).y), 
						this.screenYD(particlesToDisplay.elementAt(i).x), 
						0, 0);
				// circle the  the detected particle position according to the set radius
				g.drawOval(this.screenXD(particlesToDisplay.elementAt(i).y-getRadius()/1.0), 
						this.screenYD(particlesToDisplay.elementAt(i).x-getRadius()/1.0), 
						2*getRadius()*this.magnification-1, 2*getRadius()*this.magnification-1); 
			}
		}


	}

	/**
	 * Defines a window to display trajectories according to their <code>to_display</code> status.
	 * The trajectories displayed on this window are drawn an given Canvas
	 * <br>In the window the user can select a specific Trajectory, a region of interest (ROI)
	 * and filter trajectories by length.
	 * <br>User requests regarding filtering will be listened to and engaged from the <code>actionPerformed</code>
	 * method implemented here. 
	 * <br>User selections of trajectories with the mouse will be listened to and engaged from the
	 * <code>mousePressed</code> method implemented here
	 * <br>All other ImageJ window options (e.g. ROI selection, focus, animation) are inherited 
	 * from the stackWindow Class
	 */
	private class TrajectoryStackWindow extends StackWindow implements ActionListener, MouseListener{

		private static final long serialVersionUID = 1L;
		private Button filter_length;
		private Label numberOfParticlesLabel;

		/**
		 * Constructor.
		 * <br>Creates an instance of TrajectoryStackWindow from a given <code>ImagePlus</code>
		 * and <code>ImageCanvas</code> and a creates GUI panel.
		 * <br>Adds this class as a <code>MouseListener</code> to the given <code>ImageCanvas</code>
		 * @param aimp
		 * @param icanvas
		 */
		private TrajectoryStackWindow(ImagePlus aimp, ImageCanvas icanvas) {
			super(aimp, icanvas);
			numberOfParticlesLabel = new Label("");
			icanvas.addMouseListener(this);
			addPanel();
			changeParticleNumberLabel();


			//            this.sliceSelector.addAdjustmentListener(new AdjustmentListener(){
			//				public void adjustmentValueChanged(AdjustmentEvent aE) {
			//					changeParticleNumberLabel();
			//				}
			//            });

		}

		private void changeParticleNumberLabel() {
			int currentframe = (this.getImagePlus().getCurrentSlice() - 1) / slices_number;
			numberOfParticlesLabel.setText("Frame " + (currentframe+1) + ": " + frames[currentframe].real_particles_number + " particles");
		}

		@Override
		public String createSubtitle() {
			//overrided to get the right moment to update the label.
			changeParticleNumberLabel();
			return super.createSubtitle();
		}

		/**
		 * Adds a Panel with filter options button and number of particles in it to this window 
		 */
		private void addPanel() {
			Panel panel = new Panel(new GridLayout(2,1));
			filter_length = new Button(" Filter Options ");
			filter_length.addActionListener(this);
			panel.add(filter_length);
			panel.add(numberOfParticlesLabel);
			//pack();            
			add(panel);
			pack();
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			Point loc = getLocation();
			Dimension size = getSize();
			if (loc.y+size.height>screen.height)
				getCanvas().zoomOut(0, 0);
		}

		/** 
		 * Defines the action taken upon an <code>ActionEvent</code> triggered from buttons
		 * that have class <code>TrajectoryStackWindow</code> as their action listener:
		 * <br><code>Button filter_length</code>
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public synchronized void actionPerformed(ActionEvent e) {
			Object b = e.getSource();
			if (b==filter_length) { 
				// if user cancelled the filter dialog - do nothing
				if (!filterTrajectories()) return;           	
			}
			// generate an updated view with the ImagePlus in this window according to the new filter
			generateView(this.imp);
		}

		/** 
		 * Defines the action taken upon an <code>MouseEvent</code> triggered by left-clicking 
		 * the mouse anywhere in this <code>TrajectoryStackWindow</code>
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		public synchronized void mousePressed(MouseEvent e) {

			/* get the coordinates of mouse while it was clicked*/
			int x = e.getX();
			int y = e.getY();
			/* covert them to offScreen coordinates using the ImageCanvas of this window*/
			int offscreenX = this.ic.offScreenX(x);
			int offscreenY = this.ic.offScreenY(y);

			boolean trajectory_clicked = false;
			int min_dis = Integer.MAX_VALUE;
			Iterator<Trajectory> iter = all_traj.iterator();
			/* find the best Trajectory to match the mouse click*/
			while (iter.hasNext()) {
				Trajectory curr_traj = iter.next();				
				// only trajectories that the mouse click is within their mouse_selection_area
				// and that are not filtered (to_display == true) are considered as a candidate
				if (curr_traj.mouse_selection_area.contains(offscreenX, offscreenY) && curr_traj.to_display){					
					// we have a least 1 candidate => a trajectory will be set
					trajectory_clicked = true;
					// for each particle in a candidate trajectory, check the distance 
					// from it to the mouse click point
					for (int i = 0; i<curr_traj.existing_particles.length; i++) {
						int dis = ((int)curr_traj.existing_particles[i].x - offscreenY)*
						((int)curr_traj.existing_particles[i].x - offscreenY) +
						((int)curr_traj.existing_particles[i].y - offscreenX)*
						((int)curr_traj.existing_particles[i].y - offscreenX);
						// if the distance for this particle  is lower than the min distance found
						// for all trajectories until now - save this trajectory for now
						if (dis < min_dis) {
							min_dis = dis;
							chosen_traj = curr_traj.serial_number;							
						}
					}//for
				} //if
			} //while			

			if (trajectory_clicked) {
				/* focus or mark the selected Trajectory according the the type of mouse click*/
				this.imp.killRoi();
				this.imp.updateImage();
				// show the number of the selected Trajectory on the per trajectory 
				// panel in the results window
				results_window.per_traj_label.setText("Trajectory " + chosen_traj);				
				if (e.getClickCount() == 2) {
					// "double-click" 
					// Set the ROI to the trajectory focus_area
					IJ.getImage().setRoi((all_traj.elementAt(chosen_traj-1)).focus_area);
					// focus on Trajectory (ROI)
					generateTrajFocusView(chosen_traj-1, magnification_factor);
				} else {
					// single-click - mark the selected trajectory by setting the ROI to the 
					// trajectory�s mouse_selection_area
					this.imp.setRoi((all_traj.elementAt(chosen_traj-1)).mouse_selection_area);
				}
			} else {
				chosen_traj = -1;
				results_window.per_traj_label.setText("Trajectory (select from view)");
			}			

		}


		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		public void mouseClicked(MouseEvent e) {
			// Auto-generated method stub
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		public void mouseEntered(MouseEvent arg0) {
			// Auto-generated method stub			
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		public void mouseExited(MouseEvent arg0) {
			// Auto-generated method stub			
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		public void mouseReleased(MouseEvent arg0) {
			// Auto-generated method stub			
		}

	} // CustomStackWindow inner class

	/**
	 * Defines a window to be the main user interface for result display and analysis
	 * upon completion of the algorithm.
	 * <br>All user requests will be listened to and engaged from the <code>actionPerformed</code>
	 * method implemented here
	 */
	private class ResultsWindow extends Frame implements FocusListener, ActionListener{

		private static final long serialVersionUID = 1L;
		private TextPanel text_panel, configuration_panel;
		private Button view_static, save_report, display_report, dummy,
		plot_particle, trajectory_focus, trajectory_info, traj_in_area_info, area_focus;

		private Label per_traj_label, area_label, all_label;
		private MenuItem tail, mag_factor, relink_particles;

		/**
		 * Default constructor
		 */
		private ResultsWindow () {
			this("DEFAULT");
		}

		/**
		 * Constructor.
		 * <br>Creates an instance of a ResultsWindow with all GUI elements in it,
		 * sets its size and location on the screen.
		 * @param title - title of the results window
		 */
		private ResultsWindow (String title) {

			super(title);
			enableEvents(AWTEvent.WINDOW_EVENT_MASK);
			addFocusListener(this);

			/* Set the layout of the window*/
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			setLayout(gridbag);  
			c.anchor = GridBagConstraints.NORTHWEST;
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = GridBagConstraints.REMAINDER;

			/* Add a TextPanel to the window for display of the configuration params*/
			c.weightx = 0.25;
			c.weighty = 0.25;
			configuration_panel = new TextPanel("configuration");
			gridbag.setConstraints(configuration_panel, c);	        
			add(configuration_panel);	        

			/* Add a TextPanel to the window for display results from user queries*/
			c.weightx = 1;
			c.weighty = 1;
			text_panel = new TextPanel("Results");
			text_panel.setTitle("Results");
			gridbag.setConstraints(text_panel, c);
			add(text_panel);	       

			/*----------------------------------------------------*/
			/* Panel to hold buttons for all trajectories options */
			/*----------------------------------------------------*/
			Panel all_options = new Panel();
			all_options.setBackground(Color.LIGHT_GRAY);
			all_options.setLayout(gridbag);

			/* Create the label for this Panel*/
			all_label = new Label("All Trajectories", Label.CENTER);        

			/* Create 3 buttons and set this class to be their ActionListener */
			save_report = new Button(" Save Full Report");
			save_report.addActionListener(this);
			display_report = new Button(" Display Full Report");
			display_report.addActionListener(this);
			view_static = new Button(" Visualize All Trajectories ");	
			view_static.addActionListener(this);	        

			/* Add the Label and 3 buttons to the all_options Panel */
			gridbag.setConstraints(all_label, c);
			all_options.add(all_label);
			gridbag.setConstraints(view_static, c);
			all_options.add(view_static);		        
			gridbag.setConstraints(save_report, c);
			all_options.add(save_report);
			gridbag.setConstraints(display_report, c);
			all_options.add(display_report);
			/*--------------------------------------------------*/


			/*--------------------------------------------------*/
			/* Panel to hold buttons for pre trajectory options */
			/*--------------------------------------------------*/
			Panel per_traj_options = new Panel();
			per_traj_options.setBackground(Color.GRAY);
			per_traj_options.setLayout(gridbag);

			/* Create the label for this Panel*/
			per_traj_label = new Label("Trajectory (select from visual)", Label.CENTER);

			/* Create 3 buttons and set this class to be their ActionListener */
			trajectory_focus = new Button("Focus on Selected Trajectory");
			trajectory_focus.addActionListener(this);	        
			trajectory_info = new Button("Selected Trajectory Info");
			trajectory_info.addActionListener(this);
			plot_particle = new Button(" Plot ");
			plot_particle.addActionListener(this);

			/* Add the Label and 3 buttons to the per_traj_options Panel */
			gridbag.setConstraints(per_traj_label, c);
			per_traj_options.add(per_traj_label);
			gridbag.setConstraints(trajectory_focus, c);
			per_traj_options.add(trajectory_focus);
			gridbag.setConstraints(trajectory_info, c);
			per_traj_options.add(trajectory_info);
			gridbag.setConstraints(plot_particle, c);
			per_traj_options.add(plot_particle);
			// the plot_particle option is only avalible for text_files_mode
			if (!text_files_mode) plot_particle.setEnabled(false);
			/*--------------------------------------------------*/


			/*----------------------------------------*/
			/* Panel to hold buttons for area options */
			/*----------------------------------------*/
			Panel area_options = new Panel();
			area_options.setBackground(Color.LIGHT_GRAY);
			area_options.setLayout(gridbag);

			/* Create the label for this Panel*/
			area_label = new Label(" Area ", Label.CENTER);

			/* Create 2 buttons and set this class to be their ActionListener */	        	        
			area_focus = new Button(" Focus on Area ");
			area_focus.addActionListener(this);
			traj_in_area_info = new Button(" Trajectories in Area Info ");
			traj_in_area_info.addActionListener(this);
			/* Create 1 dummy button for coherent display */
			dummy = new Button("");
			dummy.setEnabled(false);

			/* Add the Label and 3 buttons to the area_options Panel */
			gridbag.setConstraints(area_label, c);
			area_options.add(area_label);
			gridbag.setConstraints(area_focus, c);
			area_options.add(area_focus);
			gridbag.setConstraints(traj_in_area_info, c);
			area_options.add(traj_in_area_info);
			gridbag.setConstraints(dummy, c);
			area_options.add(dummy);
			/*--------------------------------------------------*/

			/* Create a Panel to contain all the 3 first panels*/ 
			Panel all_panels = new Panel(new GridLayout(1,3));
			all_panels.add(all_options);
			all_panels.add(per_traj_options);
			all_panels.add(area_options);	        

			/* Add the all_panels Panel to the window*/
			c.weighty = 0.01;
			gridbag.setConstraints(all_panels, c);
			add(all_panels);	        

			/* Create a Menu for viewing preferences*/					
			Menu view = new Menu("View Preferences");
			tail = new MenuItem("Trajecotry tail length");
			tail.addActionListener(this);
			mag_factor = new MenuItem("Magnification factor");
			mag_factor.addActionListener(this);
			view.add(tail);
			view.add(mag_factor);

			/* Create a Menu for re linking of particles*/
			Menu relink = new Menu("Relink Particles");
			relink_particles = new MenuItem("set new parameters for linking");
			relink_particles.addActionListener(this);
			relink.add(relink_particles);

			/* Set the MenuBar of this window to hold the 2 menus*/
			MenuBar mb = new MenuBar();
			mb.add(view);
			mb.add(relink);
			this.setMenuBar(mb);

			this.pack();
			WindowManager.addWindow(this);	        
			this.setSize((int)getMinimumSize().getWidth(), 512);	        
			GUI.center(this);			
		}

		/* (non-Javadoc)
		 * @see java.awt.Window#processWindowEvent(java.awt.event.WindowEvent)
		 */
		public void processWindowEvent(WindowEvent e) {
			super.processWindowEvent(e);
			int id = e.getID();
			if (id==WindowEvent.WINDOW_CLOSING) {
				setVisible(false);
				dispose();
				WindowManager.removeWindow(this);
			}
			else if (id==WindowEvent.WINDOW_ACTIVATED)
				WindowManager.setWindow(this);
		}

		/* (non-Javadoc)
		 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
		 */
		public void focusGained(FocusEvent e) {
			WindowManager.setWindow(this);
		}

		/* (non-Javadoc)
		 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
		 */
		public void focusLost(FocusEvent e) {}

		/** 
		 * Defines the action taken upon an <code>ActionEvent</code> triggered from buttons
		 * that have class <code>ResultsWindow</code> as their action listener:
		 * <br><code>Button view_static</code>
		 * <br><code>Button save_report</code>
		 * <br><code>Button display_report</code>
		 * <br><code>Button plot_particle</code>
		 * <br><code>Button trajectory_focus</code>
		 * <br><code>Button trajectory_info</code>
		 * <br><code>Button traj_in_area_info</code>
		 * <br><code>Button area_focus</code>
		 * <br><code>MenuItem tail</code>
		 * <br><code>MenuItem mag_factor</code>
		 * <br><code>MenuItem relink_particles</code>
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public synchronized void actionPerformed(ActionEvent ae) {

			Object source = ae.getSource();						
			Roi user_roi = null;

			/* view all trajectories */
			if (source == view_static) {
				// a new view is requested so reset the filter and generate a NEW view
				resetTrajectoriesFilter();			
				generateView(null);			
				return;
			}
			/* plot particle along a trajectory - ONLY TEXT MODE FILE */
			if (source == plot_particle) {
				// this can only be requested after selecting a trajectory from the view 
				if (chosen_traj == -1) {					
					IJ.error("Please select a trajectory first\n" +
					"Click with the mouse on a trajectory in 'All trajectories' display");					
					return;
				}
				// user selects trajectory according to serial number (starts with 1)
				// but all_traj Vector starts from 0 so (chosen_traj-1)
				int param_choice = (all_traj.elementAt(chosen_traj-1)).getUserParamForPlotting();
				if (param_choice == -1) return;
				(all_traj.elementAt(chosen_traj-1)).plotParticleAlongTrajectory(param_choice);
				return;
			}
			/* save full report to file */
			if (source == save_report) {				
				// show save file user dialog with default file name 'Traj_{title}.txt'				 
				SaveDialog sd = new SaveDialog("Save report", IJ.getDirectory("image"), "Traj_" + title, ".txt");
				// if user cancelled the save dialog - return
				if (sd.getDirectory() == null || sd.getFileName() == null) return; 
				// write full report to file
				write2File(sd.getDirectory(), sd.getFileName(), getFullReport().toString());
				return;
			}
			/* display full report on the text_panel*/
			if (source == display_report) {
				text_panel.selectAll();
				text_panel.clearSelection();
				text_panel.append(getFullReport().toString());
				return;				
			}
			/* check vadilty of state for area seletion*/
			if (source == area_focus || source == traj_in_area_info) {
				// for these options, an area (ROI) has to be selected on the display
				// varify it here
				user_roi = IJ.getImage().getRoi();
				if (user_roi == null) {
					IJ.error("The active image does not have a selection\n" +
							"Please select an area of interest first\n" +
					"Click and drag the mouse on the active image.");
					return;
				}				
			}
			/* create area focus view */
			if (source == area_focus) {					
				generateAreaFocusView(magnification_factor);
				return;
			}
			/* display (on the text_panel) info about trajectories that are in the selected area */
			if (source == traj_in_area_info) {
				results_window.text_panel.selectAll();
				results_window.text_panel.clearSelection();
				Iterator<Trajectory> iter = all_traj.iterator();
				// iterate of all trajectories
				while (iter.hasNext()) {					
					Trajectory traj = iter.next();
					// for each trajectory - go over all particles
					for (int i = 0; i< traj.existing_particles.length; i++) {
						// if a particle in the trajectory is within the ROI
						// print traj information to screen and go to next trajectory
						if (user_roi.getBounds().contains(traj.existing_particles[i].y, traj.existing_particles[i].x)
								&& traj.to_display) {							
							results_window.text_panel.appendLine("%% Trajectory " + traj.serial_number);
							results_window.text_panel.append(traj.toString());
							break;
						}
					} // for
				} // while 
				return;
			}
			/* check vadilty of state for Trajectory seletion */
			if (source == trajectory_focus || source == trajectory_info) {
				// These options can only be requested after selecting a trajectory from the view 
				// varify it here
				if (chosen_traj == -1) {
					IJ.error("Please select a trajectory first\n" +
					"Click with the mouse on a trajectory in 'All trajectories' display");										
					return;
				}
			}
			/* create Trajectory focus view */
			if (source == trajectory_focus) {
				// user selects trajectory according to serial number (starts with 1)
				// but all_traj Vector starts from 0 so (chosen_traj-1)
				generateTrajFocusView(chosen_traj-1, magnification_factor);
				return;
			}
			/* display (on the text_panel) info about the selected Trajectory */
			if (source == trajectory_info) {
				// user selects trajectory according to serial number (starts with 1)
				// but all_traj Vector starts from 0 so (chosen_traj-1)
				Trajectory traj = all_traj.elementAt(chosen_traj-1);
				results_window.text_panel.selectAll();
				results_window.text_panel.clearSelection();
				results_window.text_panel.appendLine("%% Trajectory " + traj.serial_number);
				results_window.text_panel.append(traj.toString());
				return;
			}
			/* define the trajectory displyed tail*/
			if (source == tail) {
				int ch_num = Math.max(frames_number/50+2,2);
				if (frames_number%50 == 0) ch_num = frames_number/50+1;
				String [] choices = new String[ch_num];
				int curr_length = 0;
				for (int i = 0; i < choices.length; i++) {
					choices[i] = "" + curr_length;
					curr_length += 50;
				}
				choices[choices.length-1] = "" +frames_number;				
				GenericDialog tail_dialog = new GenericDialog("Select Tarjectory Tail Length");
				tail_dialog.addChoice("Tail Length", choices, "" +frames_number);
				tail_dialog.showDialog();
				if (tail_dialog.wasCanceled()) return;
				trajectory_tail = Integer.parseInt(tail_dialog.getNextChoice());
				return;
			}
			/* define the mag factor for rescaling of focused view */
			if (source == mag_factor) {
				String[] mag_choices = {"1", "2", "4", "6", "8", "10"};
				GenericDialog mag_dialog = new GenericDialog("Select Magnification Factor");
				mag_dialog.addChoice("Magnification factor", mag_choices, "" + magnification_factor);
				mag_dialog.showDialog();
				if (mag_dialog.wasCanceled()) return;
				magnification_factor = Integer.parseInt(mag_dialog.getNextChoice());
				return;
			}
			/* option to relink the deteced particles with new parameters*/
			if (source == relink_particles) {
				GenericDialog relink_dialog = new GenericDialog("Select new linking parameters");
				relink_dialog.addNumericField("Link Range", linkrange, 0);
				relink_dialog.addNumericField("Displacement", displacement, 2); 
				relink_dialog.showDialog();
				if (relink_dialog.wasCanceled()) return;
				linkrange = (int)relink_dialog.getNextNumber();
				displacement = relink_dialog.getNextNumber();
				all_traj = null;

				/* link the particles found */
				IJ.showStatus("Linking Particles");		
				linkParticles();
				IJ.freeMemory();

				/* generate trajectories */		 
				IJ.showStatus("Generating Trajectories");
				generateTrajectories();
				IJ.freeMemory();

				results_window.configuration_panel.selectAll();
				results_window.configuration_panel.clearSelection();
				results_window.configuration_panel.append(getConfiguration().toString());
				results_window.configuration_panel.append(getInputFramesInformation().toString());	

				results_window.text_panel.selectAll();
				results_window.text_panel.clearSelection();
				results_window.text_panel.appendLine("Relinking DONE!");
				results_window.text_panel.appendLine("Found " + number_of_trajectories + " Trajectories");
			}
		}		
	}

	/**
	 * Second phase of the algorithm - 
	 * <br>Identifies points corresponding to the 
	 * same physical particle in subsequent frames and links the positions into trajectories
	 * <br>The length of the particles next array will be reset here according to the current linkrange
	 * <br>Adapted from Ingo Oppermann implementation
	 */
	private void linkParticles() {

		int m, i, j, k, nop, nop_next, n;
		int ok, prev, prev_s, x = 0, y = 0, curr_linkrange;
		int[] g;
		double min, z, max_cost;
		double[] cost;
		Vector<Particle> p1, p2;

		// set the length of the particles next array according to the linkrange
		// it is done now since link range can be modified after first run
		for (int fr = 0; fr<frames.length; fr++) {
			for (int pr = 0; pr<frames[fr].particles.size(); pr++) {
				frames[fr].particles.elementAt(pr).next = new int[linkrange];
			}
		}
		curr_linkrange = this.linkrange;

		/* If the linkrange is too big, set it the right value */
		if(frames_number < (curr_linkrange + 1))
			curr_linkrange = frames_number - 1;

		max_cost = this.displacement * this.displacement;

		for(m = 0; m < frames_number - curr_linkrange; m++) {
			nop = frames[m].particles.size();
			for(i = 0; i < nop; i++) {
				frames[m].particles.elementAt(i).special = false;
				for(n = 0; n < this.linkrange; n++)
					frames[m].particles.elementAt(i).next[n] = -1;
			}

			for(n = 0; n < curr_linkrange; n++) {
				max_cost = (double)(n + 1) * this.displacement * (double)(n + 1) * this.displacement;

				nop_next = frames[m + (n + 1)].particles.size();

				/* Set up the cost matrix */
				cost = new double[(nop + 1) * (nop_next + 1)];

				/* Set up the relation matrix */
				g = new int[(nop + 1) * (nop_next + 1)];

				/* Set g to zero */
				for (i = 0; i< g.length; i++) g[i] = 0;

				p1 = frames[m].particles;
				p2 = frames[m + (n + 1)].particles;
				//    			p1 = frames[m].particles;
				//    			p2 = frames[m + (n + 1)].particles;


				/* Fill in the costs */
				for(i = 0; i < nop; i++) {
					for(j = 0; j < nop_next; j++) {
						cost[coord(i, j, nop_next + 1)] = 
							(p1.elementAt(i).x - p2.elementAt(j).x)*(p1.elementAt(i).x - p2.elementAt(j).x) + 
							(p1.elementAt(i).y - p2.elementAt(j).y)*(p1.elementAt(i).y - p2.elementAt(j).y) + 
							(p1.elementAt(i).z - p2.elementAt(j).z)*(p1.elementAt(i).z - p2.elementAt(j).z) + 
							(p1.elementAt(i).m0 - p2.elementAt(j).m0)*(p1.elementAt(i).m0 - p2.elementAt(j).m0) + 
							(p1.elementAt(i).m2 - p2.elementAt(j).m2)*(p1.elementAt(i).m2 - p2.elementAt(j).m2);
					}
				}

				for(i = 0; i < nop + 1; i++)
					cost[coord(i, nop_next, nop_next + 1)] = max_cost;
				for(j = 0; j < nop_next + 1; j++)
					cost[coord(nop, j, nop_next + 1)] = max_cost;
				cost[coord(nop, nop_next, nop_next + 1)] = 0.0;

				/* Initialize the relation matrix */
				for(i = 0; i < nop; i++) { // Loop over the x-axis
					min = max_cost;
					prev = 0;
					for(j = 0; j < nop_next; j++) { // Loop over the y-axis
						/* Let's see if we can use this coordinate */
						ok = 1;
						for(k = 0; k < nop + 1; k++) {
							if(g[coord(k, j, nop_next + 1)] == 1) {
								ok = 0;
								break;
							}
						}
						if(ok == 0) // No, we can't. Try the next column
							continue;

						/* This coordinate is OK */
						if(cost[coord(i, j, nop_next + 1)] < min) {
							min = cost[coord(i, j, nop_next + 1)];
							g[coord(i, prev, nop_next + 1)] = 0;
							prev = j;
							g[coord(i, prev, nop_next + 1)] = 1;
						}
					}

					/* Check if we have a dummy particle */
					if(min == max_cost) {
						g[coord(i, prev, nop_next + 1)] = 0;
						g[coord(i, nop_next, nop_next + 1)] = 1;
					}
				}

				/* Look for columns that are zero */
				for(j = 0; j < nop_next; j++) {
					ok = 1;
					for(i = 0; i < nop + 1; i++) {
						if(g[coord(i, j, nop_next + 1)] == 1)
							ok = 0;
					}

					if(ok == 1)
						g[coord(nop, j, nop_next + 1)] = 1;
				}

				/* The relation matrix is initilized */

				/* Now the relation matrix needs to be optimized */
				min = -1.0;
				while(min < 0.0) {
					min = 0.0;
					prev = 0;
					prev_s = 0;
					for(i = 0; i < nop + 1; i++) {
						for(j = 0; j < nop_next + 1; j++) {
							if(i == nop && j == nop_next)
								continue;

							if(g[coord(i, j, nop_next + 1)] == 0 && 
									cost[coord(i, j, nop_next + 1)] <= max_cost) {
								/* Calculate the reduced cost */

								// Look along the x-axis, including
								// the dummy particles
								for(k = 0; k < nop + 1; k++) {
									if(g[coord(k, j, nop_next + 1)] == 1) {
										x = k;
										break;
									}
								}

								// Look along the y-axis, including
								// the dummy particles
								for(k = 0; k < nop_next + 1; k++) {
									if(g[coord(i, k, nop_next + 1)] == 1) {
										y = k;
										break;
									}
								}

								/* z is the reduced cost */
								if(j == nop_next)
									x = nop;
								if(i == nop)
									y = nop_next;

								z = cost[coord(i, j, nop_next + 1)] + 
								cost[coord(x, y, nop_next + 1)] - 
								cost[coord(i, y, nop_next + 1)] - 
								cost[coord(x, j, nop_next + 1)];
								if(z > -1.0e-10)
									z = 0.0;
								if(z < min) {
									min = z;
									prev = coord(i, j, nop_next + 1);
									prev_s = coord(x, y, nop_next + 1);
								}
							}
						}
					}

					if(min < 0.0) {
						g[prev] = 1;
						g[prev_s] = 1;
						g[coord(prev / (nop_next + 1), prev_s % (nop_next + 1), nop_next + 1)] = 0;
						g[coord(prev_s / (nop_next + 1), prev % (nop_next + 1), nop_next + 1)] = 0;
					}
				}

				/* After optimization, the particles needs to be linked */
				for(i = 0; i < nop; i++) {
					for(j = 0; j < nop_next; j++) {
						if(g[coord(i, j, nop_next + 1)] == 1)
							p1.elementAt(i).next[n] = j;
					}
				}
			}

			if(m == (frames_number - curr_linkrange - 1) && curr_linkrange > 1)
				curr_linkrange--;
		}

		/* At the last frame all trajectories end */
		for(i = 0; i < frames[frames_number - 1].particles.size(); i++) {
			frames[frames_number - 1].particles.elementAt(i).special = false;
			for(n = 0; n < this.linkrange; n++)
				frames[frames_number - 1].particles.elementAt(i).next[n] = -1;
		}
	}

	/**
	 * Generates <code>Trajectory</code> objects according to the infoamtion 
	 * avalible in each MyFrame and Particle. 
	 * <br>Populates the <code>all_traj</code> Vector.
	 */
	private void generateTrajectories() {

		int i, j, k;
		int found, n, m;
		// Bank of colors from which the trjectories color will be selected
		Color[] col={Color.blue,Color.green,Color.orange,Color.cyan,Color.magenta,Color.yellow,Color.white,Color.gray,Color.pink};

		Trajectory curr_traj;
		// temporary vector to hold particles for current trajctory
		Vector<Particle> curr_traj_particles = new Vector<Particle>(frames_number);		
		// initialize trajectories vector
		all_traj = new Vector<Trajectory>();
		this.number_of_trajectories = 0;		

		for(i = 0; i < frames_number; i++) {
			for(j = 0; j < this.frames[i].particles.size(); j++) {
				if(!this.frames[i].particles.elementAt(j).special) {
					this.frames[i].particles.elementAt(j).special = true;
					found = -1;
					// go over all particles that this particle (particles[j]) is linked to
					for(n = 0; n < this.linkrange; n++) {
						// if it is NOT a dummy particle - stop looking
						if(this.frames[i].particles.elementAt(j).next[n] != -1) {
							found = n;
							break;
						}
					}
					// if this particle is not linked to any other
					// go to next particle and dont add a trajectory
					if(found == -1)
						continue;

					// Added by Guy Levy, 18.08.06 - A change form original implementation
					// if this particle is linkd to a "real" paritcle that was already linked
					// break the trajectory and start again from the next particle. dont add a trajectory
					if(this.frames[i + n + 1].particles.elementAt(this.frames[i].particles.elementAt(j).next[n]).special) 
						continue;

					// this particle is linked to another "real" particle that is not already linked
					// so we have a trajectory
					this.number_of_trajectories++;					
					curr_traj_particles.add(this.frames[i].particles.elementAt(j));
					k = i;
					m = j;
					do {
						found = -1;
						for(n = 0; n < this.linkrange; n++) {
							if(this.frames[k].particles.elementAt(m).next[n] != -1) {
								// If this particle is linked to a "real" particle that
								// that is NOT already linked, continue with building the trajectory
								if(this.frames[k + n + 1].particles.elementAt(this.frames[k].particles.elementAt(m).next[n]).special == false) {
									found = n;
									break;
									// Added by Guy Levy, 18.08.06 - A change form original implementation
									// If this particle is linked to a "real" particle that
									// that is already linked, stop building the trajectory
								} else {									
									break;
								}
							}
						}
						if(found == -1)
							break;
						m = this.frames[k].particles.elementAt(m).next[found];
						k += (found + 1);
						curr_traj_particles.add(this.frames[k].particles.elementAt(m));
						this.frames[k].particles.elementAt(m).special = true;
					} while(m != -1);					

					// Create the current trajectory
					Particle[] curr_traj_particles_array = new Particle[curr_traj_particles.size()];
					curr_traj = new Trajectory((Particle[])curr_traj_particles.toArray(curr_traj_particles_array));

					// set current trajectory parameters
					curr_traj.serial_number = this.number_of_trajectories;
					curr_traj.color = col[this.number_of_trajectories% col.length];
					curr_traj.setFocusArea();
					curr_traj.setMouseSelectionArea();
					curr_traj.populateGaps();
					// add current trajectory to all_traj vactor
					all_traj.add(curr_traj);
					// clear temporary vector
					curr_traj_particles.removeAllElements();
				}				
			}
		}		
	}

	/**
	 * Generates (in real time) a "ready to print" report with all trajectories info.
	 * <br>For each Trajectory:
	 * <ul>
	 * <li> Its serial number
	 * <li> All frames of this trajectory with infomation about the particle in each frame
	 * </ul>
	 * @return a <code>StringBuffer</code> that holds this information
	 */
	private StringBuffer getTrajectoriesInfo() {

		StringBuffer traj_info = new StringBuffer("%% Trajectories:\n");
		traj_info.append("%%\t 1st column: frame number\n");
		traj_info.append("%%\t 2nd column: x coordinate top-down\n");
		traj_info.append("%%\t 3rd column: y coordinate left-right\n");
		traj_info.append("%%\t 4th column: z coordinate bottom-top\n");
		if (text_files_mode) {
			traj_info.append("%%\t next columns: other information provided for each particle in the given order\n");
		} else {
			traj_info.append("%%\t 4th column: zero-order intensity moment m0\n");
			traj_info.append("%%\t 5th column: first-order intensity moment m1\n");
			traj_info.append("%%\t 6th column: second-order intensity moment m2\n");
			traj_info.append("%%\t 7th column: second-order intensity moment m3\n");
			traj_info.append("%%\t 8th column: second-order intensity moment m4\n");
			traj_info.append("%%\t 9th column: non-particle discrimination score\n");
		}
		traj_info.append("\n");

		Iterator<Trajectory> iter = all_traj.iterator();
		while (iter.hasNext()) {
			Trajectory curr_traj = iter.next();
			traj_info.append("%% Trajectory " + curr_traj.serial_number +"\n");
			traj_info.append(curr_traj.toStringBuffer());
		}

		return traj_info;
	}

	private void writeDataToDisk() {
		// get original file location 
		FileInfo vFI = this.original_imp.getOriginalFileInfo();
		if(vFI == null) {
			IJ.error("You're running a macros. Data are written to disk at the directory where your image is stored. Please store youre image first.");
			return;
		}
		// create new directory
		//		File newDir = new File(vFI.directory,"ParticleTracker3DResults");
		//		if(!newDir.mkdir() && !newDir.exists()) {
		//			IJ.error("You probably do not have the permission to write in the directory where your image is stored. Data are not written to disk.");
		//			return;
		//		}
		//		// write data to file
		//		write2File(newDir.getAbsolutePath(), vFI.fileName + "PT3D.txt", getFullReport().toString());
		write2File(vFI.directory, "Traj_" + title + ".txt", getFullReport().toString());
	}

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
			IJ.write(s.toString());
			break;
		}		
	}	

	//	private Panel makeThresholdPanel() {
	//		//panel test
	//        GridBagLayout gbl = new GridBagLayout();
	//        GridBagConstraints c = new GridBagConstraints();
	//        Panel thresholdPanel = new Panel(gbl);
	//        Panel rightPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	//        final Label thresholdUnitLabel = new Label(" %");
	//        Choice thresholdChoice = new Choice();
	//        final TextField tf = new TextField(IJ.d2s(0.1, 5), IJ.isWindows() ? 4 : 6);
	//        if (IJ.isLinux()) tf.setBackground(Color.white);
	//        
	//        c.gridx = 0; c.gridy = 0;
	//		c.anchor = GridBagConstraints.EAST;
	//		c.gridwidth = 1;
	//		c.insets = new Insets(0, 0, 3, 0);
	//
	//		thresholdChoice.addItemListener(new ItemListener(){
	//			public void itemStateChanged(ItemEvent e) {
	//				if(e.getItem().toString().equals("Threshold")) {
	//					thresholdUnitLabel.setText("");
	//					tf.setText("20");
	//					setThresholdMode(ABS_THRESHOLD_MODE);
	//				} 
	//				if(e.getItem().toString().equals("Percentile")) {
	//					thresholdUnitLabel.setText(" %");
	//					tf.setText(IJ.d2s(0.1, 5));
	//					setThresholdMode(PERCENTILE_MODE);
	//				} 
	//			}});
	//		thresholdChoice.add("Percentile");
	//		thresholdChoice.add("Threshold");
	//
	//		rightPanel.add(tf);
	//		rightPanel.add(thresholdUnitLabel);
	//        
	//        gbl.setConstraints(thresholdChoice, c);
	//        thresholdPanel.add(thresholdChoice);
	//        
	//        c.gridx = 1; c.gridy = 0;
	//		c.anchor = GridBagConstraints.WEST;
	//		
	//        gbl.setConstraints(rightPanel, c);
	//        thresholdPanel.add(rightPanel);
	//        return thresholdPanel;
	//	}

	/**
	 * Creates the preview panel that gives the options to preview and save the detected particles,
	 * and also a scroll bar to navigate through the slices of the movie
	 * <br>Buttons and scrollbar created here use this ParticleTracker_ as <code>ActionListener</code>
	 * and <code>AdjustmentListener</code>
	 * @return the preview panel
	 */

	private Panel makePreviewPanel() {

		Panel preview_panel = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		preview_panel.setLayout(gridbag);  
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;

		/* scroll bar to navigate through the slices of the movie */
		preview_scrollbar = new Scrollbar(Scrollbar.HORIZONTAL, original_imp.getCurrentSlice(), 1, 1, original_imp.getStackSize()+1);
		preview_scrollbar.addAdjustmentListener(this);
		preview_scrollbar.setUnitIncrement(1); 
		preview_scrollbar.setBlockIncrement(1);

		/* button to generate preview of the detected particles */
		preview = new Button("Preview Detected");
		preview.addActionListener(this);

		/* button to save the detected particles */
		save_detected = new Button("Save Detected");
		save_detected.addActionListener(this);
		Label seperation = new Label("______________", Label.CENTER); 
		gridbag.setConstraints(preview, c);
		preview_panel.add(preview);
		gridbag.setConstraints(preview_scrollbar, c);	        
		preview_panel.add(preview_scrollbar);
		gridbag.setConstraints(save_detected, c);
		preview_panel.add(save_detected);
		gridbag.setConstraints(previewLabel, c);
		preview_panel.add(previewLabel);
		gridbag.setConstraints(seperation, c);
		preview_panel.add(seperation);
		return preview_panel;
	}

	/** 
	 * Defines the acation taken upon an <code>ActionEvent</code> triggered from buttons
	 * that have class <code>ParticleTracker_</code> as their action listener:
	 * <br><code>Button preview</code>
	 * <br><code>Button save_detected</code>
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public synchronized void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source==preview) {
			// set the original_imp window position next to the dialog window
			this.original_imp.getWindow().setLocation((int)gd.getLocationOnScreen().getX()+gd.getWidth(), (int)gd.getLocationOnScreen().getY());
			// do preview
			this.preview();
			preview_canvas.repaint();
			return;
		}
		if (source==save_detected) {			
			/* show save file user dialog with default file name 'frame' */
			SaveDialog sd = new SaveDialog("Save Detected Particles", IJ.getDirectory("image"), "frame", "");
			// if user cancelled the save dialog 
			if (sd.getDirectory() == null || sd.getFileName() == null) return;

			/* set the user defined pramars according to the valus in the dialog box */
			getUserDefinedPreviewParams();

			/* detect particles and save to files*/
			if (this.processFrames()) { // process the frames
				// for each frame - save the detected particles
				for (int i = 0; i<this.frames.length; i++) {					
					if (!write2File(sd.getDirectory(), sd.getFileName() + "_" + i, 
							this.frames[i].frameDetectedParticlesForSave(false).toString())) {
						// upon any problam savingto file - return
						IJ.write("Problem occured while writing to file.");
						return;
					}
				}
			}
			preview_canvas.repaint();
			return;
		}
	}

	/**
	 * Defines the acation taken upon an <code>AdjustmentEvent</code> triggered from manu bars
	 * that have class <code>ParticleTracker_</code> as their sdjustment listener:
	 * <br> <code>ScrollBar preview_scrollbar</code>
	 * @see java.awt.event.AdjustmentListener#adjustmentValueChanged(java.awt.event.AdjustmentEvent)
	 */
	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		Object source = e.getSource();
		/* if the preview scrollbar was moved*/
		if (source == preview_scrollbar) {
			// set the current visible slice to the one selected on the bar
			original_imp.setSlice(preview_scrollbar.getValue());
			// update the preview view to this silce
			//			this.preview();
		}
	}

	/**
	 * Detects particles in the current displayed frame according to the parameters curretly set 
	 * Draws dots on the positions of the detected partciles on the frame and circles them
	 * @see #getUserDefinedPreviewParams()
	 * @see MyFrame#featurePointDetection()
	 * @see PreviewCanvas
	 */
	public synchronized void preview() {		

		if (original_imp == null) return;

		// the stack of the original loaded image (it can be 1 frame)
		stack = original_imp.getStack();

		// get the frame number
		this.preview_slice_calculated = original_imp.getCurrentSlice();

		getUserDefinedPreviewParams();		

		int first_slice = (getFrameNumberFromSlice(this.preview_slice_calculated)-1) * slices_number + 1;
		// create a new MyFrame from the current_slice in the stack
		MyFrame preview_frame = new MyFrame(GetSubStackCopyInFloat(stack, first_slice, first_slice  + slices_number - 1), getFrameNumberFromSlice(this.preview_slice_calculated)-1);

		// detect particles in this frame
		preview_frame.featurePointDetection();
		previewLabel.setText("#Particles: " + preview_frame.getParticles().size());		

		preview_canvas.setPreviewFrame(preview_frame);
	}

	private void generatePreviewCanvas(){
		if(!text_files_mode) {			
			// save the current magnification factor of the current image window
			double magnification = original_imp.getWindow().getCanvas().getMagnification();

			// generate the previewCanvas - while generating the drawing will be done 
			preview_canvas = new PreviewCanvas(original_imp, magnification);

			// display the image and canvas in a stackWindow
			StackWindow sw = new StackWindow(original_imp, preview_canvas);

			// magnify the canvas to match the original image magnification
			while (sw.getCanvas().getMagnification() < magnification) {
				preview_canvas.zoomIn(0,0);
			}
		}
	}

	/**
	 * Displays a dialog for filtering trajectories according to minimum length
	 * <br>The <code>to_display</code> parameter of all trajectories will be set according to their length
	 * and the user choice.
	 * <br>Trajectories with shorter or equal length then given by the user will be set to false. 
	 * For the rest, it will be set the true.
	 * <br>Displays a text line on the result windows text panel with the number of trajectories after filter     
	 * @return true if user gave an input and false if the user cancelled the operation
	 */
	private boolean filterTrajectories() {

		int passed_traj = 0;
		GenericDialog fod = new GenericDialog("Filter Options...", IJ.getInstance());
		// default is not to filter any trajectories (min length of zero)
		fod.addNumericField("Only keep trajectories longer than", 0, 0, 10, "frames");

		//      fod.addNumericField("Only keep the", this.number_of_trajectories, 0, 10, "longest trajectories");
		fod.showDialog();
		int min_length_to_display = (int)fod.getNextNumber();
		//		this.trajectories_longer = (int)fod.getNextNumber();

		if (fod.wasCanceled()) return false;

		Iterator<Trajectory> iter = all_traj.iterator();		
		while (iter.hasNext()) {
			Trajectory curr_traj = iter.next();
			if (curr_traj.length <= min_length_to_display){
				curr_traj.to_display = false;
			} else {
				curr_traj.to_display = true;
				passed_traj++;
			}
		}
		results_window.text_panel.appendLine(passed_traj + " trajectories remained after filter");
		return true;
	}

	/**
	 * Resets the trajectories filter so no trajectory is filtered by
	 * setting the <code>to_display</code> param of each trajectory to true  
	 */
	private void resetTrajectoriesFilter() {

		Iterator<Trajectory> iter = all_traj.iterator();
		while (iter.hasNext()) {
			(iter.next()).to_display = true;					
		}
	}

	/**
	 * Generates (in real time) a "ready to print" report with information
	 * about the user defined parameters:
	 * <ul>
	 * <li> Radius	
	 * <li> Cutoff
	 * <li> Percentile
	 * <li> Displacement
	 * <li> Linkrange
	 * </ul>
	 * @return a <code>StringBuffer</code> that holds this information
	 */
	private StringBuffer getConfiguration() {

		StringBuffer configuration = new StringBuffer("% Configuration:\n");
		if (!this.text_files_mode){
			configuration.append("% \tKernel radius: ");
			configuration.append(this.radius);
			configuration.append("\n");		
			configuration.append("% \tCutoff radius: ");
			configuration.append(this.cutoff);
			configuration.append("\n");
			if(threshold_mode == PERCENTILE_MODE) {
				configuration.append("% \tPercentile: ");
				configuration.append((this.percentile*100));
				configuration.append("\n");
			}
			if(threshold_mode == ABS_THRESHOLD_MODE) {
				configuration.append("% \tAbsolute threshold: ");
				configuration.append((this.absIntensityThreshold));
				configuration.append("\n");
			}

		}
		configuration.append("% \tDisplacement : ");
		configuration.append(this.displacement);
		configuration.append("\n");
		configuration.append("% \tLinkrange    : ");
		configuration.append(this.linkrange);
		configuration.append("\n");
		return configuration;
	}

	/**
	 * Generates (in real time) a "ready to print" report with this information
	 * about the frames that were given as input (movie or images):
	 * <ul>
	 * <li> Width	
	 * <li> Height
	 * <li> Global pixel intensity max
	 * <li> Global pixel intensity min
	 * </ul>
	 * @return a <code>StringBuffer</code> that holds this information
	 */
	private StringBuffer getInputFramesInformation() {
		if (this.text_files_mode) 
			return new StringBuffer("Frames info was loaded from text files");
		StringBuffer info = new StringBuffer("% Frames information:\n");
		info.append("% \tWidth : ");
		info.append(stack.getWidth());
		info.append(" pixel\n");
		info.append("% \tHeight: ");
		info.append(stack.getHeight());
		info.append(" pixel\n");
		info.append("% \tDepth: ");
		info.append(slices_number);
		info.append(" slices\n");
		info.append("% \tGlobal minimum: ");
		info.append(this.global_min);
		info.append("\n");
		info.append("% \tGlobal maximum: ");
		info.append(this.global_max);
		info.append("\n");
		return info;
	}

	/**
	 * Creates a new view of the trajectories as an overlay on the given <code>ImagePlus</code>.
	 * <br> The new view is an instance of <code>TrajectoryStackWindow</code>
	 * <br>If the given image is null, a new <code>ImagePlus</code> is duplicated from <code>original_imp</code>
	 * <br>If the given image is NOT null, the overlay view is RE-created on top of it
	 * <br>The trajectories are drawn on <code>TrajectoryCanvas</code> when it's constructed and not on the image 
	 * @param duplicated_imp the image upon which the view will be updated - can be null 
	 * @see TrajectoryStackWindow
	 * @see TrajectoryCanvas
	 */
	public void generateView(ImagePlus duplicated_imp) {		

		double magnification;
		TrajectoryCanvas tc;		
		String new_title = "All Trajectories Visual";		

		if (duplicated_imp == null) {
			// if there is no image to generate the view on:
			// generate a new image by duplicating the original image
			Duplicater dup = new Duplicater();
			duplicated_imp= dup.duplicateStack(original_imp, new_title);
			if (this.text_files_mode) {
				// there is no original image so set magnification to default(1)	
				magnification = 1;
			} else {
				// Set magnification to the one of original_imp	
				magnification = original_imp.getWindow().getCanvas().getMagnification();
			}			
		} else { 
			// if the view is generated on an already existing image, 
			// set the updated view scale (magnification) to be the same as in the existing image
			magnification = duplicated_imp.getWindow().getCanvas().getMagnification();
		}

		// Create a new canvas based on the image - the canvas is the view
		// The trajectories are drawn on this canvas when it�s constructed and not on the image
		// Canvas is an overlay window on top of the ImagePlus
		tc = new TrajectoryCanvas(duplicated_imp);

		// Create a new window to hold the image and canvas
		TrajectoryStackWindow tsw = new TrajectoryStackWindow(duplicated_imp, tc);

		// zoom the window until its magnification will reach the set magnification magnification
		while (tsw.getCanvas().getMagnification() < magnification) {
			tc.zoomIn(0,0);
		}		
	}

	/**
	 * Generates and displays a new <code>StackWindow</code> with rescaled (magnified) 
	 * view of the trajectory specified by the given <code>trajectory_index</code>.
	 * <br>The new Stack will be made of RGB ImageProcessors upon which the trajectory will be drawn
	 * @param trajectory_index the trajectory index in the <code>trajectories</code> Vector (starts with 0)
	 * @param magnification the scale factor to use for rescaling the original image
	 * @see IJ#run(java.lang.String, java.lang.String)
	 * @see ImagePlus#getRoi()
	 * @see StackConverter#convertToRGB()
	 * @see Trajectory#animate(int)
	 */
	public void generateTrajFocusView(int trajectory_index, int magnification) {

		String new_title = "[Trajectory number " + (trajectory_index+1) + "]";

		// get the trajectory at the given index
		Trajectory traj = (all_traj.elementAt(trajectory_index));

		// set the Roito be magnified as the given trajectory predefined focus_area
		IJ.getImage().setRoi(traj.focus_area);

		// Save the ID of the last active image window - the one the ROI set on
		int roi_image_id = IJ.getImage().getID();

		// ImageJ macro command to rescale and image - this will create a new ImagePlus (stack)
		// that will be the active window 
		IJ.run("Scale...", "x=" + magnification + " y=" + magnification +" process create title=" + new_title);
		IJ.freeMemory();

		// Get the new-scaled image (stack) and assign it duplicated_imp
		ImagePlus duplicated_imp = IJ.getImage();

		// get the first and last frames of the trajectory
		int first_frame = traj.existing_particles[0].frame;
		int last_frame = traj.existing_particles[traj.existing_particles.length-1].frame;

		// remove from the new-scaled image stack any frames not relevant to this trajectory
		ImageStack tmp = duplicated_imp.getStack();
		int passed_frames = 0;
		int removed_frames_from_start = 0;
		for (int i = 1; i <= frames_number; i++) {			
			if (passed_frames< first_frame-5 || passed_frames>last_frame+5) {
				for(int s = 0; s < slices_number; s++)
					tmp.deleteSlice((i-1)*slices_number+1+s);	
				// when deleting slice from the stack, all following slice numbers are 
				// decreased by 1 so i is decreased by 1 as well.
				// there is no risk of infinite loop since tmp.getSize() is decreased as well
				// every time deleteSlice(i) is invoked
				i--;
			}
			if (passed_frames< first_frame-5) {
				// keep track of frames that were removed from start (prefix) of the stack
				// for the animate method later
				removed_frames_from_start++;
			}
			passed_frames++;
		}
		duplicated_imp.setStack(duplicated_imp.getTitle(), tmp);
		IJ.freeMemory();

		// Convert the stack to RGB so color can been drawn on it and get its ImageStac
		IJ.run("RGB Color");
		traj_stack = duplicated_imp.getStack();
		IJ.freeMemory();

		// Reset the active imageJ window to the one the trajectory was selected on - 
		// info from that window is still needed
		IJ.selectWindow(roi_image_id);

		// animate the trajectory 
		traj.animate(magnification, removed_frames_from_start);

		// set the new window to be the active one
		IJ.selectWindow(duplicated_imp.getID());

	}

	/**
	 * Generates and displays a new <code>StackWindow</code> with rescaled (magnified) 
	 * view of the Roi that was selected on ImageJs currently active window.
	 * <br>The new Stack will be made of RGB ImageProcessors upon which the trajectories in the Roi
	 * will be drawn
	 * <br>If Roi was not selected, an imageJ error is displayed and no new window is created
	 * @param magnification the scale factor to use for rescaling the original image
	 * @see IJ#run(java.lang.String, java.lang.String)
	 * @see ImagePlus#getRoi()
	 * @see StackConverter#convertToRGB()
	 * @see Trajectory#animate(int)
	 */
	public void generateAreaFocusView(int magnification) {		

		String new_title = "[Area Focus]";
		//	Save the ID of the last active image window - the one the ROI was selected on
		int roi_image_id = IJ.getImage().getID();

		// Get the ROI and check its valid
		Roi user_roi = IJ.getImage().getRoi();
		if (user_roi == null) {
			IJ.error("generateAreaFocusView: No Roi was selected");
			return;
		}

		// ImageJ macro command to rescale and image the select ROI in the active window
		// this will create a new ImagePlus (stack) that will be the active window 
		IJ.run("Scale...", "x=" + magnification + " y=" + magnification +" process create title=" + new_title);
		IJ.freeMemory();

		// Get the new-scaled image (stack) and assign it duplicated_imp
		ImagePlus duplicated_imp = IJ.getImage();

		// Convert the stack to RGB so color can been drawn on it and get its ImageStack
		IJ.run("RGB Color");
		traj_stack = duplicated_imp.getStack();
		IJ.freeMemory();

		// Reset the active imageJ window to the one the ROI was selected on - info from the Roi is still needed
		IJ.selectWindow(roi_image_id);

		// Iterate over all trajectories
		Iterator<Trajectory> iter = all_traj.iterator();
		while (iter.hasNext()) {
			Trajectory traj = iter.next();
			// Iterate over all particles in the current trajectory
			for (int i = 0; i< traj.existing_particles.length; i++) {
				// if at least one particle of this trajectory is in the selected area of the user (ROI)
				// and this trajectory was not filtered - animate it
				if (user_roi.getBounds().contains(traj.existing_particles[i].y, traj.existing_particles[i].x)
						&& traj.to_display) {
					traj.animate(magnification);
					break;
				}
			}
		}
		// set the new window to be the active one
		IJ.selectWindow(duplicated_imp.getID());

	}

	/**
	 * Opens an 'open file' dialog where the user can select a folder
	 * @return an array of All the file names in the selected folder 
	 * 			or null if the user cancelled the selection. 
	 * 			is some O.S (e.g. Linux) this may include '.' and '..'
	 * @see ij.io.OpenDialog#OpenDialog(java.lang.String, java.lang.String, java.lang.String)
	 * @see java.io.File#list() 
	 */
	private String[] getFilesList() {

		/* Opens an 'open file' with the default directory as the imageJ 'image' directory*/
		OpenDialog od = new OpenDialog("test", IJ.getDirectory("image"), "");

		this.files_dir = od.getDirectory();
		if (files_dir == null) return null;
		//		 TODO 	create a file filter so only the roght files will be taken
		//				and the folder could contain other files
		String[] list = new File(od.getDirectory()).list();
		return list;		
	}

	/**
	 * (Re)Initialize the binary and weighted masks. This is necessary if the radius changed.
	 * The memory allocations are performed in advance (in this method) for efficiency reasons.
	 * @param mask_radius
	 */
	public void generateMasks(int mask_radius){
		//the binary mask can be calculated already:
		int width = (2 * mask_radius) + 1;
		this.binary_mask = new short[width][width*width];
		generateBinaryMask(mask_radius);

		//the weighted mask is just initialized with the new radius:
		this.weighted_mask = new float[width][width*width];

		//standard boolean mask
		generateMask(mask_radius);

	}

	/**
	 * Generates the dilation mask
	 * <code>mask</code> is a var of class ParticleTracker_ and its modified internally here
	 * Adapted from Ingo Oppermann implementation
	 * @param mask_radius the radius of the mask (user defined)
	 */
	public void generateBinaryMask(int mask_radius) {    	
		int width = (2 * mask_radius) + 1;
		for(int s = -mask_radius; s <= mask_radius; s++){
			for(int i = -mask_radius; i <= mask_radius; i++) {
				for(int j = -mask_radius; j <= mask_radius; j++) {
					int index = coord(i + mask_radius, j + mask_radius, width);
					if((i * i) + (j * j) + (s * s) <= mask_radius * mask_radius)
						this.binary_mask[s + mask_radius][index] = 1;
					else
						this.binary_mask[s + mask_radius][index] = 0;

				}
			}
		}
		//    	System.out.println("mask crated");
	}

	/**
	 * Generates the dilation mask
	 * <code>mask</code> is a var of class ParticleTracker_ and its modified internally here
	 * Adapted from Ingo Oppermann implementation
	 * @param mask_radius the radius of the mask (user defined)
	 */
	public void generateMask(int mask_radius) {    	

		int width = (2 * mask_radius) + 1;
		this.mask = new int[width][width*width];
		for(int s = -mask_radius; s <= mask_radius; s++){
			for(int i = -mask_radius; i <= mask_radius; i++) {
				for(int j = -mask_radius; j <= mask_radius; j++) {
					int index = coord(i + mask_radius, j + mask_radius, width);
					if((i * i) + (j * j) + (s * s) <= mask_radius * mask_radius)
						this.mask[s + mask_radius][index] = 1;
					else
						this.mask[s + mask_radius][index] = 0;

				}
			}
		}
	}

	public void generateWeightedMask_old(int mask_radius, float xCenter, float yCenter, float zCenter) {
		int width = (2 * mask_radius) + 1;
		for(int iz = -mask_radius; iz <= mask_radius; iz++){
			for(int iy = -mask_radius; iy <= mask_radius; iy++) {
				for(int ix = -mask_radius; ix <= mask_radius; ix++) {
					int index = coord(iy + mask_radius, ix + mask_radius, width);

					float distPxToCenter = (float) Math.sqrt((xCenter-ix)*(xCenter-ix)+(yCenter-iy)*(yCenter-iy)+(zCenter-iz)*(zCenter-iz)); 

					//the weight is approximative the amount of the voxel inside the (spherical) mask.
					float weight = (float)mask_radius - distPxToCenter + .5f; 
					if(weight < 0) {
						weight = 0f;    				
					} 
					if(weight > 1) {
						weight = 1f;
					}
					this.weighted_mask[iz + mask_radius][index] = weight;
				}
			}
		}
	}

	public void generateWeightedMask_2D(int mask_radius, float xCenter, float yCenter, float zCenter) {
		int width = (2 * mask_radius) + 1;
		float pixel_radius = 0.5f;		
		float r = pixel_radius;
		float R = mask_radius;
		for(int iy = -mask_radius; iy <= mask_radius; iy++) {
			for(int ix = -mask_radius; ix <= mask_radius; ix++) {
				int index = coord(iy + mask_radius, ix + mask_radius, width);

				float distPxCenterToMaskCenter = (float) Math.sqrt((xCenter-ix)*(xCenter-ix)+(yCenter-iy)*(yCenter-iy)); 
				float d = distPxCenterToMaskCenter;
				//the weight is approximative the amount of the voxel inside the (spherical) mask. See formula 
				//http://mathworld.wolfram.com/Circle-CircleIntersection.html
				float weight = 0;
				if(distPxCenterToMaskCenter < mask_radius + pixel_radius){
					weight = 1;

					if(mask_radius < distPxCenterToMaskCenter + pixel_radius) {
						float v = (float) (pixel_radius*pixel_radius*
								Math.acos((d*d+r*r-R*R)/(2*d*r))
								+R*R*Math.acos((d*d+R*R-r*r)/(2*d*R))
								-0.5f*Math.sqrt((-d+r+R)*(d+r-R)*(d-r+R)*(d+r+R)));

						weight =  (v / ((float)Math.PI * pixel_radius*pixel_radius));
					}
				}

				for(int iz = -mask_radius; iz <= mask_radius; iz++){
					this.weighted_mask[iz + mask_radius][index] = weight;
				}
			}
		}


	}

	public void generateWeightedMask_3D(int mask_radius, float xCenter, float yCenter, float zCenter) {
		int width = (2 * mask_radius) + 1;
		float voxel_radius = 0.5f;
		for(int iz = -mask_radius; iz <= mask_radius; iz++){
			for(int iy = -mask_radius; iy <= mask_radius; iy++) {
				for(int ix = -mask_radius; ix <= mask_radius; ix++) {
					int index = coord(iy + mask_radius, ix + mask_radius, width);

					float distPxCenterToMaskCenter = (float) Math.sqrt((xCenter-ix+.5f)*(xCenter-ix+.5f)+(yCenter-iy+.5f)*(yCenter-iy+.5f)+(zCenter-iz+.5f)*(zCenter-iz+.5f)); 

					//the weight is approximative the amount of the voxel inside the (spherical) mask.
					float weight = 0; 
					if(distPxCenterToMaskCenter < mask_radius + voxel_radius){
						weight = 1;

						if(mask_radius < distPxCenterToMaskCenter + voxel_radius) {

							//The volume is given by http://mathworld.wolfram.com/Sphere-SphereIntersection.html
							float v = (float) (Math.PI*Math.pow(voxel_radius + mask_radius - distPxCenterToMaskCenter ,2)
									*(distPxCenterToMaskCenter * distPxCenterToMaskCenter +2 * distPxCenterToMaskCenter * mask_radius 
											- 3 * mask_radius * mask_radius + 2 * distPxCenterToMaskCenter * voxel_radius 
											+ 6 * mask_radius * voxel_radius - 3 * voxel_radius * voxel_radius) 
											/ (12 * distPxCenterToMaskCenter));
							weight = (float) (v / ((4f*Math.PI/3f)*Math.pow(voxel_radius,3)));
						}
					}
					this.weighted_mask[iz + mask_radius][index] = weight;
				}
			}
		}
	}

	//    /**
	//     * Generates the Convolution Kernel as described in the Image Restoration 
	//     * part of the original algorithm 
	//     * <code>kernel</code> is a var of class ParticleTracker_ and its modified internally here
	//     * @param kernel_radius (the radius of the kernel (user defined))
	//     */
	//    public void makeKernel(int kernel_radius){
	//		
	//    	int kernel_width = (kernel_radius * 2) + 1;		
	//		this.kernel = new float[kernel_width*kernel_width];		
	//		double b = calculateB(kernel_radius, lambda_n);
	//		double norm_cons = calculateNormalizationConstant(b, kernel_radius, lambda_n);
	//		
	////		COORD(a, b, c)	(((a) * (c)) + (b));
	//		for (int i = -kernel_radius; i<=kernel_radius; i++){
	//			for (int j = -kernel_radius; j<=kernel_radius; j++){
	//				int index = (i + kernel_radius)*kernel_width + j + kernel_radius;
	//				this.kernel[index]= (float)((1.0/b)* Math.exp(-((i * i + j * j)/(4.0*lambda_n*lambda_n))));				
	//				this.kernel[index]= this.kernel[index] - (float)(1.0/(kernel_width * kernel_width));
	//				this.kernel[index]= (float) ((double)this.kernel[index] / norm_cons);
	//			}
	//		}			
	//	}

	//    /**
	//     * Generates a normalized gaussian convolution kernel using the member lamda_n as sigma.
	//     * @param kernel_radius the radius of the kernel (user defined)
	//     */
	//    public void makeKernel1D(int kernel_radius){
	//    	int kernel_width = (kernel_radius * 2) + 1;
	//    	this.kernel = new float[kernel_width];
	//    	double b = Math.sqrt(calculateB(kernel_radius, lambda_n));
	//    	
	//    	for (int i = 1; i <= kernel_radius; i++){    		
	//    		this.kernel[kernel_radius+i] = (float)((1.0/b)* Math.exp(-((i * i )/(2.0*lambda_n*lambda_n))))
	//    										- (float)(1.0/(kernel_width * kernel_width));
	//    		this.kernel[kernel_radius-i] = this.kernel[kernel_radius+i];
	//    	}
	//    	//normalize the kernel numerically:
	//    	float sum = 0;
	//    	for(int i = 0; i < kernel.length; i++){
	//    		sum += kernel[i];
	//    	}
	//    	float scale = 1.0f/sum;
	//    	for(int i = 0; i < kernel.length; i++){
	//    		kernel[i] *= scale;
	//    	}
	//    }

	public float[] CalculateNormalizedGaussKernel(float aSigma){
		int vL = (int)aSigma * 3 * 2 + 1;
		if(vL < 3) vL = 3;
		float[] vKernel = new float[vL];
		int vM = vKernel.length/2;
		for(int vI = 0; vI < vM; vI++){
			vKernel[vI] = (float)(1f/(2f*Math.PI*aSigma*aSigma) * Math.exp(-(float)((vM-vI)*(vM-vI))/(2f*aSigma*aSigma)));
			vKernel[vKernel.length - vI - 1] = vKernel[vI];
		}
		vKernel[vM] = (float)(1f/(2f*Math.PI*aSigma*aSigma));

		//normalize the kernel numerically:
		float vSum = 0;
		for(int vI = 0; vI < vKernel.length; vI++){
			vSum += vKernel[vI];
		}
		float vScale = 1.0f/vSum;
		for(int vI = 0; vI < vKernel.length; vI++){
			vKernel[vI] *= vScale;
		}
		return vKernel;
	}

	private static ImageStack GetSubStack(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if(startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for(int i = startPos; i <= endPos; i++) {
			res.addSlice(is.getSliceLabel(i), is.getProcessor(i));
		}
		return res;
	}

	private static ImageStack GetSubStackInFloat(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if(startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for(int i = startPos; i <= endPos; i++) {
			res.addSlice(is.getSliceLabel(i), is.getProcessor(i).convertToFloat());
		}
		return res;
	}

	private static ImageStack GetSubStackCopyInFloat(ImageStack is, int startPos, int endPos){
		ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
		if(startPos > endPos || startPos < 0 || endPos < 0)
			return null;
		for(int i = startPos; i <= endPos; i++) {
			res.addSlice(is.getSliceLabel(i), is.getProcessor(i).convertToFloat().duplicate());
		}
		return res;
	}

	/**
	 * @param sliceIndex: 1..#slices
	 * @return a frame index: 1..#frames
	 */
	public int getFrameNumberFromSlice(int sliceIndex) {
		return (sliceIndex-1) / slices_number + 1;
	}

	/**
	 * Writes the given <code>info</code> to given file information.
	 * <code>info</code> will be written to the beginning of the file, overwriting older information
	 * If the file doesn�t exists it will be created.
	 * Any problem creating, writing to or closing the file will generate an ImageJ error   
	 * @param directory location of the file to write to 
	 * @param file_name file name to write to
	 * @param info info the write to file
	 * @see java.io.FileOutputStream#FileOutputStream(java.lang.String)
	 */
	public boolean write2File(String directory, String file_name, String info) {
		try {
			FileOutputStream fos = new FileOutputStream(new File(directory, file_name));
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			print_writer = new PrintWriter(bos);
			print_writer.print(info);
			print_writer.close();
			return true;
		}
		catch (IOException e) {
			IJ.error("" + e);
			return false;
		}    			

	}

	/**
	 * Creates a new <code>ImageStack</code> and draws particles on it according to
	 * the particle positions defined in <code>frames</code>.  
	 * <br>It is used to visualize particles and trajectories when working in text-files-mode,
	 * since there is no visual stack to start with
	 * @return the created ImageStack
	 * @see MyFrame#createImage(int, int)
	 */
	public ImageStack createStackFromTextFiles() {

		/* Create a new, empty, square ImageStack with 10 pixels padding from the max particle position*/
		ImageStack from_text = new ImageStack(max_coord+10, max_coord+10);
		
		/* for each frame we have add a slice (ImageParocessor) to the stack*/
		for (int i = 0; i<frames.length; i++) {
			from_text.addSlice("" + i, frames[i].createImage(max_coord+10, max_coord+10));
		}
		return from_text;
	}

	/**
	 * Generates (in real time) a "ready to print" report with this information:
	 * <ul>
	 * <li> System configuration	
	 * <li> Frames general information
	 * <li> Per frame information about detected particles
	 * <li> Particles linking 
	 * <li> All trajectories found
	 * </ul>
	 * @return a <code>StringBuffer</code> that holds this information
	 * @see #getConfiguration()
	 * @see #getInputFramesInformation()
	 * @see MyFrame#getFullFrameInfo()
	 * @see MyFrame#toStringBuffer()	 * @see #getTrajectoriesInfo()
	 */
	public StringBuffer getFullReport() {

		/* initial infomation to output */
		StringBuffer report = new StringBuffer();
		report.append(this.getConfiguration());
		report.append(this.getInputFramesInformation());
		report.append("\n");

		/* detected particles infomation per frame*/
		report.append("%\tPer frame information (verbose output):\n");
		for (int i = 0; i < frames.length; i++) {
			report.append(this.frames[i].getFullFrameInfo());
		}

		/* Add linking info */
		report.append("% Trajectory linking (verbose output):\n");
		for(int i = 0; i < frames.length; i++) {
			report.append(this.frames[i].toStringBuffer());
		}

		/* all trajectories info */
		report.append("\n");
		report.append(getTrajectoriesInfo());

		return report;
	}
	/**
	 * Returns a * c + b
	 * @param a: y-coordinate
	 * @param b: x-coordinate
	 * @param c: width
	 * @return
	 */
	private int coord (int a, int b, int c) {
		return (((a) * (c)) + (b));
	}

	int getRadius() {
		return this.radius;
	}

	void setThresholdMode(int aMode) {
		this.threshold_mode = aMode;
	}

	int getThresholdMode() {
		return this.threshold_mode;
	}

//	public StringBuffer evaluateMomentaAfterDeath(Particle aParticle) {
//		StringBuffer sb = new StringBuffer();
//		for(int aV : aParticle.next) {
//			if(aV != -1) {
//				return sb; //there is a linked particle to this one
//			}
//		}
//		NumberFormat nf = NumberFormat.getInstance();
//		nf.setMaximumFractionDigits(6);
//		nf.setMinimumFractionDigits(6);
//
//		int NbOfFTE = 10; // number of frames to evaluate after death of particle
//		int mask_width = 2 * radius + 1;
//		if(aParticle.frame < frames_number)
//			sb.append("%Intensty momenta of the position where the trajectory ended:\n");
//		float[][] vMomenta = new float[NbOfFTE][5];
//		for(int i = 1; i < NbOfFTE; i++) {
//			if(aParticle.frame + i >= frames_number) continue;
//			ImageStack ips = frames[aParticle.frame + i].restored_fps;
//
//			for(int s = -radius; s <= radius; s++) {
//				if(((int)aParticle.z + s) < 0 || ((int)aParticle.z + s) >= ips.getSize())
//					continue;
//				int z = (int)aParticle.z + s;
//				for(int k = -radius; k <= radius; k++) {
//					if(((int)aParticle.x + k) < 0 || ((int)aParticle.x + k) >= ips.getHeight())
//						continue;
//					int x = (int)aParticle.x + k;
//
//					for(int l = -radius; l <= radius; l++) {
//						if(((int)aParticle.y + l) < 0 || ((int)aParticle.y + l) >= ips.getWidth())
//							continue;
//						int y = (int)aParticle.y + l;
//
//						float c = ips.getProcessor(z + 1).getPixelValue(y, x) * (float)weighted_mask[s + radius][coord(k + radius, l + radius, mask_width)];
//						vMomenta[i][0] += c;
//						vMomenta[i][2] += (float)(k * k + l * l + s * s) * c;
//						vMomenta[i][1] += (float)Math.sqrt(k * k + l * l + s * s) * c;
//						vMomenta[i][3] += (float)Math.pow(k * k + l * l + s * s, 1.5f) * c;
//						vMomenta[i][4] += (float)Math.pow(k * k + l * l + s * s, 2f) * c;								
//					}
//				}
//			}
//
//			vMomenta[i][2]  /= vMomenta[i][0];
//			vMomenta[i][1]  /= vMomenta[i][0];
//			vMomenta[i][3]  /= vMomenta[i][0];
//			vMomenta[i][4]  /= vMomenta[i][0];
//			sb.append((aParticle.frame+i) + " ") ; 
//			sb.append(nf.format(vMomenta[i][0]) + " ");
//			sb.append(nf.format(vMomenta[i][1]) + " ");
//			sb.append(nf.format(vMomenta[i][2]) + " ");
//			sb.append(nf.format(vMomenta[i][3]) + " ");
//			sb.append(nf.format(vMomenta[i][4]) + "\n");
//		}
//		return sb;
//	}
}
