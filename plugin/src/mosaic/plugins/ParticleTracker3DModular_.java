package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager; 
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Duplicater;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.process.StackStatistics;
import ij.text.TextPanel;

import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.detection.PreviewCanvas;
import mosaic.core.detection.PreviewInterface;
import mosaic.core.particleLinking.ParticleLinker;
import mosaic.core.utils.MosaicUtils;

/**
 * <h2>ParticleTracker</h2>
 * <h3>An ImageJ Plugin for particles detection and tracking from digital videos</h3>
 * <p>This class implements a to 3d extended feature point detection and tracking algorithm as described in:
 * <br>I. F. Sbalzarini and P. Koumoutsakos. 
 * <br>Feature point tracking and trajectory analysis for video imaging in cell biology. 
 * <br>J. Struct. Biol., 151(2), 2005.
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
 * @version 1.3, November 2010 (requires: ImageJ 1.36b and Java 5 or higher)
 * 
 * @author Guy Levy - Academic guest CBL 
 * 2d algorithm and GUI implementation
 *  
 * @author Janick Cardinale, PhD Student at Mosaic 
 * <a href="http://www.mosaic.ethz.ch/">Mosaic Group, Inst. of theoretical computer 
 * science<a>, ETH Zurich  
 * 3D extension, memory efficiency, parallelism, algorithmic speed up, batch processing
 *
 * feature extension made by:
 * 
 * @version 1.6 November 2010 (requires: ImageJ 1.44 or higher)
 * @author Kota Miura - CMCI, EMBL Heidelberg  (http://cmci.embl.de)
 * add functionality to automatically transfer resulting data to result table in ImageJ, 
 */

public class ParticleTracker3DModular_ implements PlugInFilter, Measurements, PreviewInterface  {	

	private final static int SYSTEM = 0;
	private final static int IJ_RESULTS_WINDOW = 1;
	public ImageStack stack ,traj_stack;	
	public StackConverter sc;
	public ImagePlus original_imp;
	public MyFrame[] frames;
	public Vector<Trajectory> all_traj;// = new Vector();
	public int number_of_trajectories, frames_number, slices_number;
	private FeaturePointDetector detector;
	private ParticleLinker linker;
	public String title;
	
	/* user defined parameters for linking*/
	public int linkrange = 2; 			// default
	public double displacement = 10.0; 	// default

	public GenericDialog gd;
	
	/* flags */	
	public boolean text_files_mode = false;
	public boolean only_detect = false; 
	private boolean frames_processed = false;

	/* results display and file */	
	private int trajectory_tail;
	private int magnification_factor = 6;
	private int chosen_traj = -1;
	public ResultsWindow results_window; 
	public PreviewCanvas preview_canvas = null;

	/* preview vars */ 
	//public Button preview, save_detected;
	//public Scrollbar preview_scrollbar;
	//public Label previewLabel = new Label("");
	public int preview_slice_calculated;


	/* vars for text_files_mode*/
	public String files_dir;
	String[] files_list;
	boolean momentum_from_text, zcoord_from_text;	
	


	/** 
	 * This method sets up the plugin filter for use.
	 * <br>It is called by ImageJ upon selection of the plugin from the menus and the returned value is
	 * used to call the <code>run(ImageProcessor ip)</code> method. 
	 * <br>The <code>arg</code> is a string passed as an argument to the plugin, can also be an empty string. 
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
		 
		if(!text_files_mode) {
			preview_canvas = detector.generatePreviewCanvas(original_imp);	
		} 

		/* get user defined params and set more initial params accordingly 	*/	
		if (!getUserDefinedParams()) return;				

		if (!processFrames()) return;		

		if (text_files_mode) {

			/* create an ImagePlus object to hold the particle information from the text files*/
			original_imp = new ImagePlus("From text files", createStackFromTextFiles());
		}

		/* link the particles found */
		IJ.showStatus("Linking Particles");		
		linker.linkParticles(frames, frames_number, linkrange, displacement);
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
			float global_max = (float)stack_stats.max;
			float global_min = (float)stack_stats.min;
			frames_number = original_imp.getNFrames();
			slices_number = original_imp.getNSlices();
			
			detector = new FeaturePointDetector(global_max, global_min);
			
		}else {
			slices_number = 1;
		}
		
		linker = new ParticleLinker();
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
				current_frame = new MyFrame(files_dir + files_list[file_index], frame_i, linkrange);
				if (current_frame.getParticles() == null) return false;

			} else {

				// sequence of images mode:
				// construct each frame from the corresponding image
				current_frame = new MyFrame(MosaicUtils.GetSubStackInFloat(stack, (frame_i) * slices_number + 1, (frame_i + 1) * slices_number), frame_i, linkrange);
				
				// Detect feature points in this frame
				IJ.showStatus("Detecting Particles in Frame " + (frame_i+1) + "/" + frames_number);				
				detector.featurePointDetection(current_frame);
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
			detector.addUserDefinedParametersDialog(gd);
			
			gd.addPanel(detector.makePreviewPanel(this, original_imp), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));	        

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
			Boolean changed = detector.getUserDefinedParameters(gd);
			// even if the frames were already processed (particles detected) but
			// the user changed the detection params then the frames needs to be processed again
			if (changed) {
				if (this.frames_processed) {
					this.frames = null;
					this.frames_processed = false;
				}        		
			}
			
			// add the option to convert only if   images are not GRAY8, 16 or 32
			if (convert) convert = gd.getNextBoolean();
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
			detector.global_max = (float)stack_stats.max;
			detector.global_min = (float)stack_stats.min;
			frames_number = original_imp.getNFrames(); //??maybe not necessary
		}
		return true;
	}

//	private void thresholdModeChanged(int aThresholdMode) {
//		setThresholdMode(aThresholdMode);
//		if(aThresholdMode == ABS_THRESHOLD_MODE) {
//			int defaultIntensity = (int)(global_max - (global_max-global_min) / 5);
//			((TextField)gd.getNumericFields().elementAt(2)).setText("" + defaultIntensity);
//		}
//		if(aThresholdMode == PERCENTILE_MODE) {
//			((TextField)gd.getNumericFields().elementAt(2)).setText(IJ.d2s(0.1, 5));
//		}
//
//	}

	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private void showAbout() {
		IJ.showMessage("ParticleTracker...",
				"An ImageJ Plugin for particles detection and tracking from digital videos.\n" +
				"The plugin implements an extended version of the feature point detection and tracking algorithm as described in:\n" +
				"I. F. Sbalzarini and P. Koumoutsakos.\n" +
				"Feature point tracking and trajectory analysis for video imaging in cell biology.\n" +
				"J. Struct. Biol., 151(2): 182195, 2005.\n" +
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
			this.length = this.existing_particles[this.existing_particles.length-1].getFrame() - 
			this.existing_particles[0].getFrame();
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
				if (existing_particles[i+1].getFrame() - existing_particles[i].getFrame() > 1) {
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
			int previous_frame = existing_particles[0].getFrame()-removed_frames;
			for (int i = 0; i<existing_particles.length; i++){
				current_frame = existing_particles[i].getFrame()+1-removed_frames;
				while (current_frame - previous_frame > 1) {
					previous_frame++;
					ImageStack previousFrameStack = MosaicUtils.GetSubStack(traj_stack, (previous_frame-1)*slices_number+1, 
							previous_frame*slices_number);
					draw4Dynamic(previousFrameStack, i, magnification);
					drawGaps4Dynamic(previousFrameStack, i, magnification);
				}
				// if some frames were removed from traj_stack then the frame number 
				// of a particle will not correspond with frame number in the stack.
				// by subtracting the number of removed frames from the particle frame number,
				// we will get the right frame in traj_stack.
				ImageStack currentFrameStack = MosaicUtils.GetSubStack(traj_stack, (current_frame-1)*slices_number+1, 
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
				if (this.existing_particles[i+1].getFrame() - this.existing_particles[i].getFrame() > 1) {	    			   
					g.setColor(Color.red); //gap
				}
				g.drawLine(ic.screenXD(this.existing_particles[i].y), 
						ic.screenYD(this.existing_particles[i].x), 
						ic.screenXD(this.existing_particles[i+1].y), 
						ic.screenYD(this.existing_particles[i+1].x));

				g.setColor(this.color);							
			}
			//mark death of particle
			if((this.existing_particles[this.existing_particles.length-1].getFrame()) < frames_number - 1){
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
				x_values[i] = this.existing_particles[i].getFrame();
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
					// trajectory mouse_selection_area
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

		private Button transfer_particles, transfer_trajs; //in panel left
		private Button transfer_particle, transfer_traj; //in panel center

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
			transfer_particles = new Button(" Segmented Particles to Table");	
			transfer_particles.addActionListener(this);	 
			transfer_trajs = new Button(" All Trajectories to Table");	
			transfer_trajs.addActionListener(this);	

			/* Add the Label and 3 buttons to the all_options Panel */
			gridbag.setConstraints(all_label, c);
			all_options.add(all_label);
			gridbag.setConstraints(view_static, c);
			all_options.add(view_static);		        
			gridbag.setConstraints(save_report, c);
			all_options.add(save_report);
			gridbag.setConstraints(display_report, c);
			all_options.add(display_report);
	        gridbag.setConstraints(transfer_particles, c);
	        all_options.add(transfer_particles);
	        gridbag.setConstraints(transfer_trajs, c);
	        all_options.add(transfer_trajs);

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
        	transfer_particle = new Button("");
        	transfer_particle.addActionListener(this);
        	transfer_traj = new Button("Selected Trajectory to Table");
        	transfer_traj.addActionListener(this);

			/* Add the Label and 3 buttons to the per_traj_options Panel */
			gridbag.setConstraints(per_traj_label, c);
			per_traj_options.add(per_traj_label);
			gridbag.setConstraints(trajectory_focus, c);
			per_traj_options.add(trajectory_focus);
			gridbag.setConstraints(trajectory_info, c);
			per_traj_options.add(trajectory_info);
			gridbag.setConstraints(plot_particle, c);
			per_traj_options.add(plot_particle);
        	gridbag.setConstraints(transfer_particle, c);
        	per_traj_options.add(transfer_particle);
        	gridbag.setConstraints(transfer_traj, c);
        	per_traj_options.add(transfer_traj);

        	// the plot_particle option is only avalible for text_files_mode
			if (!text_files_mode) plot_particle.setEnabled(false);
			/*--------------------------------------------------*/

			transfer_particle.setEnabled(false); 		//TODO			

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
		 * <br><code>Button transfer_particles</code>
    	 * <br><code>Button transfer_trajs</code>
    	 * <br><code>Button transfer_traj</code>
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
				MosaicUtils.write2File(sd.getDirectory(), sd.getFileName(), getFullReport().toString());
				return;
			}
			/* display full report on the text_panel*/
			if (source == display_report) {
				text_panel.selectAll();
				text_panel.clearSelection();
				text_panel.append(getFullReport().toString());
				return;				
			}
			/* check validity of state for area selection*/
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
			if (source == trajectory_focus || source == trajectory_info || source == transfer_traj) {
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
			
			if (source == transfer_traj) {
				Trajectory traj = all_traj.elementAt(chosen_traj-1);
				transferSelectedTrajectoriesToResultTable(traj);
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
				linker.linkParticles(frames, frames_number, linkrange, displacement);
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
			/* transfer segmented particle coordinates to ImageJ results window*/
			if (source == transfer_particles) {
				transferParticlesToResultsTable(); // ver 1.4 20101115
				return;
			}

			/* transfer trajectory coordinates to ImageJ results window*/
			if (source == transfer_trajs) {
				transferTrajectoriesToResultTable();
				return;
			}
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
			for(j = 0; j < this.frames[i].getParticles().size(); j++) {
				if(!this.frames[i].getParticles().elementAt(j).special) {
					this.frames[i].getParticles().elementAt(j).special = true;
					found = -1;
					// go over all particles that this particle (particles[j]) is linked to
					for(n = 0; n < this.linkrange; n++) {
						// if it is NOT a dummy particle - stop looking
						if(this.frames[i].getParticles().elementAt(j).next[n] != -1) {
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
					if(this.frames[i + n + 1].getParticles().elementAt(this.frames[i].getParticles().elementAt(j).next[n]).special) 
						continue;

					// this particle is linked to another "real" particle that is not already linked
					// so we have a trajectory
					this.number_of_trajectories++;					
					curr_traj_particles.add(this.frames[i].getParticles().elementAt(j));
					k = i;
					m = j;
					do {
						found = -1;
						for(n = 0; n < this.linkrange; n++) {
							if(this.frames[k].getParticles().elementAt(m).next[n] != -1) {
								// If this particle is linked to a "real" particle that
								// that is NOT already linked, continue with building the trajectory
								if(this.frames[k + n + 1].getParticles().elementAt(this.frames[k].getParticles().elementAt(m).next[n]).special == false) {
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
						m = this.frames[k].getParticles().elementAt(m).next[found];
						k += (found + 1);
						curr_traj_particles.add(this.frames[k].getParticles().elementAt(m));
						this.frames[k].getParticles().elementAt(m).special = true;
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
		MosaicUtils.write2File(vFI.directory, "Traj_" + title + ".txt", getFullReport().toString());
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
			IJ.log(s.toString());
			break;
		}		
	}	

	/**
	 * Detects particles in the current displayed frame according to the parameters currently set 
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

		detector.getUserDefinedPreviewParams(gd);		

		int first_slice = (MosaicUtils.getFrameNumberFromSlice(this.preview_slice_calculated, slices_number)-1) * slices_number + 1;
		// create a new MyFrame from the current_slice in the stack
		MyFrame preview_frame = new MyFrame(MosaicUtils.GetSubStackCopyInFloat(stack, first_slice, first_slice  + slices_number - 1), 
				MosaicUtils.getFrameNumberFromSlice(this.preview_slice_calculated, slices_number)-1, linkrange);
		

		// detect particles in this frame
		detector.featurePointDetection(preview_frame);
		detector.setPreviewLabel("#Particles: " + preview_frame.getParticles().size());		

		preview_canvas.setPreviewFrame(preview_frame);
		preview_canvas.setPreviewParticleRadius(detector.getRadius());
		preview_canvas.setPreviewSliceCalculated(preview_slice_calculated);
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
			configuration.append(detector.radius);
			configuration.append("\n");		
			configuration.append("% \tCutoff radius: ");
			configuration.append(detector.cutoff);
			configuration.append("\n");
			if(detector.threshold_mode == FeaturePointDetector.PERCENTILE_MODE) {
				configuration.append("% \tPercentile: ");
				configuration.append((detector.percentile*100));
				configuration.append("\n");
			}
			if(detector.threshold_mode == FeaturePointDetector.ABS_THRESHOLD_MODE) {
				configuration.append("% \tAbsolute threshold: ");
				configuration.append((detector.absIntensityThreshold));
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
		info.append(detector.global_min);
		info.append("\n");
		info.append("% \tGlobal maximum: ");
		info.append(detector.global_max);
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
		// The trajectories are drawn on this canvas when it is constructed and not on the image
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
		int first_frame = traj.existing_particles[0].getFrame();
		int last_frame = traj.existing_particles[traj.existing_particles.length-1].getFrame();

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







	/**
	 * Creates a new <code>ImageStack</code> and draws particles on it according to
	 * the particle positions defined in <code>frames</code>.  
	 * <br>It is used to visualize particles and trajectories when working in text-files-mode,
	 * since there is no visual stack to start with
	 * @return the created ImageStack
	 * @see MyFrame#createImage(int, int)
	 */
	public ImageStack createStackFromTextFiles() {
		int[] vMax = {0,0,0};	
		/* find the max coordinates for each coordinate */
		for (int i = 0; i < frames.length; i++) {
			for (int p = 0; p < frames[i].getParticles().size(); p++) {
				Particle vParticle = frames[i].getParticles().elementAt(p);
				if(vParticle.x > vMax[0]) {
					vMax[0] = (int) Math.ceil(vParticle.x);
				}
				if(vParticle.y > vMax[1]) {
					vMax[1] = (int) Math.ceil(vParticle.y);
				}
				if(vParticle.z > vMax[2]) {
					vMax[2] = (int) Math.ceil(vParticle.z);
				}
			}
		}
		
		
		/* Create a new, empty, square ImageStack with 10 pixels padding from the max particle position*/
		ImageStack from_text = new ImageStack(vMax[0]+10, vMax[1]+10);
		
		/* for each frame we have add a stack to the image */
		for (int i = 0; i<frames.length; i++) {
			// 2D
			if (vMax[2] < 1) {
				from_text.addSlice("" + i, frames[i].createImage(vMax[0]+10, vMax[1]+10));
			} else { // 3D
				for (int s = 0; s <= vMax[2]+1; s++) {
					from_text.addSlice("" + i, frames[i].createImage(vMax[0]+10, vMax[1]+10));
				}
			}
			
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
	

	@Override
	public void preview(ActionEvent e) {
		// set the original_imp window position next to the dialog window
		this.original_imp.getWindow().setLocation((int)gd.getLocationOnScreen().getX()+gd.getWidth(), (int)gd.getLocationOnScreen().getY());
		// do preview
		this.preview();
		preview_canvas.repaint();
		return;
	}

	@Override
	public void saveDetected(ActionEvent e) {
		/* set the user defined pramars according to the valus in the dialog box */
		detector.getUserDefinedPreviewParams(gd);

		/* detect particles and save to files*/
		if (this.processFrames()) { // process the frames
			detector.saveDetected(this.frames);
		}
		preview_canvas.repaint();
		return;
	}

	/** Extracts spot segmentation results <br>and show in in ImageJ static Results Table
	 * <br>Invoked by clicking button in ParticleTracker Results Window. 
	 * @author  Kota Miura <a href="http://cmci.embl.de">cmci.embl.de</a>
	 * @see ResultsWindow
	 */
	
	public void transferParticlesToResultsTable(){
		ResultsTable rt = null; 
		try {
			rt = ResultsTable.getResultsTable();//static, the one in Analyze
		} catch (Exception e) {}
		if ((rt.getCounter() != 0) || (rt.getLastColumn() != -1)) {
			if (IJ.showMessageWithCancel("Results Table", "Reset Results Table?")){
				rt.reset();
			} else
				return;
		}
	
		int rownum = 0;
		for (int i = 0; i < frames.length; i++) {
			Vector<Particle> particles = this.frames[i].getParticles();
			for (Particle p : particles) {
				rt.incrementCounter();
				rownum = rt.getCounter()-1;
				rt.setValue("frame", rownum, p.getFrame());
				rt.setValue("x", rownum, p.x);
				rt.setValue("y", rownum, p.y);
				rt.setValue("z", rownum, p.z);
				rt.setValue("m0", rownum, p.m0);
				rt.setValue("m1", rownum, p.m1);
				rt.setValue("m2", rownum, p.m2);
				rt.setValue("m3", rownum, p.m3);
				rt.setValue("m4", rownum, p.m4);
				rt.setValue("NPscore", rownum, p.score);
			}
		}			
		rt.show("Results");
	}

	/** Extracts tracking results <br>and show in in ImageJ static Results Table
	 * <br>Invoked by clicking button in ParticleTracker Results Window. 
	 * @author Kota Miura <a href="http://cmci.embl.de">cmci.embl.de</a>
	 * @see ResultsWindow
	 */	
	public void transferTrajectoriesToResultTable(){
		ResultsTable rt = null; 
		try {
			rt = ResultsTable.getResultsTable();//static, the one in Analyze
		} catch (Exception e) {}
		if ((rt.getCounter() != 0) || (rt.getLastColumn() != -1)) {
			if (IJ.showMessageWithCancel("Results Table", "Reset Results Table?")){
				rt.reset();
			} else
				return;
		}
		Iterator<Trajectory> iter = all_traj.iterator();
		int rownum = 0;
		while (iter.hasNext()) {
			Trajectory curr_traj = iter.next();
			Particle[] pts = curr_traj.existing_particles; 
			for (Particle p : pts){	
				rt.incrementCounter();
				rownum = rt.getCounter()-1;
				rt.setValue("Trajectory", rownum, curr_traj.serial_number); 
				rt.setValue("Frame", rownum, p.getFrame());
				rt.setValue("x", rownum, p.x);
				rt.setValue("y", rownum, p.y);
				rt.setValue("z", rownum, p.z);
				rt.setValue("m0", rownum, p.m0);
				rt.setValue("m1", rownum, p.m1);
				rt.setValue("m2", rownum, p.m2);
				rt.setValue("m3", rownum, p.m3);
				rt.setValue("m4", rownum, p.m4);
				rt.setValue("NPscore", rownum, p.score);
			}
		}
		rt.show("Results");
	}

	/**
	 * corrdinates of selected trajectory (as argument) will be copied to 
	 * ImageJ results table. 
	 *  
	 * @param traj
	 */
	public void transferSelectedTrajectoriesToResultTable(Trajectory traj){
		ResultsTable rt = null; 
		try {
			rt = ResultsTable.getResultsTable();//static, the one in Analyze
		} catch (Exception e) {}
		if ((rt.getCounter() != 0) || (rt.getLastColumn() != -1)) {
			if (IJ.showMessageWithCancel("Results Table", "Reset Results Table?")){
				rt.reset();
			} else
				return;
		}
		Trajectory curr_traj = traj;
		int rownum = 0;
		Particle[] pts = curr_traj.existing_particles; 

		for (Particle p : pts){	
			rt.incrementCounter();
			rownum = rt.getCounter()-1;
			rt.setValue("Trajectory", rownum, curr_traj.serial_number);
			rt.setValue("Frame", rownum, p.getFrame());
			rt.setValue("x", rownum, p.x);
			rt.setValue("y", rownum, p.y);
			rt.setValue("z", rownum, p.z);
			rt.setValue("m0", rownum, p.m0);
			rt.setValue("m1", rownum, p.m1);
			rt.setValue("m2", rownum, p.m2);
			rt.setValue("m3", rownum, p.m3);
			rt.setValue("m4", rownum, p.m4);
			rt.setValue("NPscore", rownum, p.score);
		}
		rt.show("Results");
	}
	
	public int getLinkRange() {
		return linkrange;
	}
}

