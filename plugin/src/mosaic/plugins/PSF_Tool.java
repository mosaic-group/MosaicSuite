package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.io.SaveDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import mosaic.psf2d.PsfBessel;
import mosaic.psf2d.PsfPointSpreadFunction;
import mosaic.psf2d.PsfRefinement;
import mosaic.psf2d.PsfSourcePosition;

/**
 * <h2>PSF_Tool</h2>
 * <h3>An ImageJ Plugin that implements a Point Spread Function Measurement Tool</h3>
 * <p><b>Disclaimer</b>
 * <br>IN NO EVENT SHALL THE ETH BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, 
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND
 * ITS DOCUMENTATION, EVEN IF THE ETH HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * THE ETH SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. 
 * THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE ETH HAS NO 
 * OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.<p>
 * 
 * @version 1.0 , February 2007
 * @since 1.0
 * @author Benedikt Baumgartner - Academic guest at the <a href="http://www.cbl.ethz.ch/">Computational Biophysics Lab<a>, ETH Zurich
 */
public class PSF_Tool implements PlugInFilter, MouseListener, ActionListener, WindowListener{

	/** Radius of selected point sources*/
	public double radius;	
	
	/** Maximum sampling radius*/
	public double sample_radius;
	
	/** Pixel size on chip */
	public float pix_size;
	
	/** Numerical Aperture of Microscope Objective*/
	public double na;
	
	/** Wavelength of detected light*/
	public double lambda;
	
	/** Magnification factor of microscope */
	public float mic_mag;
	
	/** Magnification factor of pixel size (how fine should PSF be estimated?)*/
	public final int mag_fact = 4;			
	
	/**	 Number of selected sources*/
	public int num_of_particles = 0;	
	
	/**	 Number of sample points*/
	public final int sample_points = 50;		

	private boolean select_start = false;		// Flag to test if in selection-mode or not
	private PsfRefinement Refine;					// Used to refine positions based on centroid calculation
	private PsfPointSpreadFunction EstimatePSF[];	// Instance of class PointSpreadFunction that does the actual work		
	private float[] PSF;						// Float Array containing PSF-values
	private float rad[];						// Float Array containing PSF-radius to plot the function
	private Vector<PsfSourcePosition> Positions;					// Vector to hold user selected coordinates
	private double whm = 0.0;					// Width of PSF at half maximum
	
	private ImagePlus imp;						// Image to be loaded
	private ImageCanvas canvas;					// Needed to apply MouseListener
	private ImageProcessor org_ip;				// Original Image Processor used for all the calculations
	private ImageProcessor eq;					// Image Processor holding histogram equalized values
	private ImageProcessor color;				// Image Processor used to display RGB image
	private ImageProcessor lastcolor;			// Image Processor holding original image converted to RGB
	private int[] iArray;						// Array containing RGB values of user selection

	// Some GUI elements
	private JFrame ui = new JFrame("Point Selection");
	private JButton start;
	private JButton equalize;
	private JButton estimate;
	private JButton report;
	private JPanel button_panel;
	private JPanel checkbox_panel;
	private JTextArea text_panel;
	private JScrollPane scroller;
	
	// Some elements for report file
	private StringBuffer selections = new StringBuffer();
	private String centroid = new String("%Centroid Positions:\n%\tx-coord\ty-coord");
	
	
	/** 
	 * This method sets up the plugin filter for use.
	 * @param arg A string command that determines the mode of this plugin - can be empty
	 * @param imp The ImagePlus that is the original input image sequence - 
	 * if null then <code>text_files_mode</code> is activated after an OK from the user 
	 * @return a flag word that represents the filters capabilities according to arg String argument
	 * @see PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	public int setup(String arg, ImagePlus imp){
		if (arg.equals("about")) {
			showAbout(); 
			return DONE;
		}
		
		if (imp==null) {
			IJ.error("You must load an Image first");            
			return DONE;
		}
		
		this.imp = imp;
		return DOES_8G + NO_CHANGES + SUPPORTS_MASKING;
	}
	
	
	/**
	 * This method runs the plugin, what implemented here is what the plugin actually 
	 * does. It takes the image processor it works on as an argument. 
	 * @see PlugInFilter#run(ij.process.ImageProcessor)
	 */
	public void run(ImageProcessor ip){
		
		org_ip = ip;	// Original image processor to do calculations on
		eq = ip;		// ImageProcessor used for contrast enhancement
		color = new ColorProcessor(org_ip.getWidth(),org_ip.getHeight());	// ColorProcessor to make selections visible
		lastcolor = new ColorProcessor(org_ip.getWidth(),org_ip.getHeight());	// ColorProcessor of original image
		iArray = new int[3];	// RGB value of last selection on ColorProcessor of original image
		
		// Start Dialog to get necessary plugin parameters
		GenericDialog gd = new GenericDialog("Configuration");
		gd.addMessage("Please enter necessary Information");
//		gd.addNumericField("Apparent Radius of Point Sources [Pixel]", 3, 2);
		gd.addNumericField("Maximum Sampling Radius [Pixel]", 6, 2);
		gd.addNumericField("Pixel Size on Camera Chip [um]", 16, 2);
		gd.addNumericField("Numerical Aperture of Microscope Objective", 1.3, 3);
		gd.addNumericField("Wavelength of Detected Light [nm]", 532, 2);
		gd.addNumericField("Microscope Magnification Factor", 100, 2);
		//gd.addNumericField("Total Magnification", 2, 0);
		//gd.addNumericField("Number of Sample Points", 4, 0);
		gd.showDialog();
		if (gd.wasCanceled()) {
			IJ.error("PlugIn canceled!");
			return;
		}
		// Read user inputs
//		radius = gd.getNextNumber();
		radius = 3;
		sample_radius = gd.getNextNumber();
		pix_size = (float)gd.getNextNumber();
		na = gd.getNextNumber();
		lambda = gd.getNextNumber();
		mic_mag = (float)gd.getNextNumber();
		//mag_fact = (int)gd.getNextNumber();
		//sample_points = (int)gd.getNextNumber();
		
		Positions = new Vector<PsfSourcePosition>();	// Create Vector to hold selected Positions
		

		
		
		// Add MouseListener to displayed image
		ImageWindow win = imp.getWindow();
		canvas = win.getCanvas();
		canvas.addMouseListener(this);
		
		  javax.swing.SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	userInterface();	// Start GUI
	            }
	        });
	}
	
	
	 /**
     * Shows an ImageJ message with info about this plugin
     */
	private void showAbout() {
        IJ.showMessage("Point Spread Function Estimation Tool",
            "An ImageJ Plugin for Point Spread Function Estimation\n" +
            "Written by: Benedikt Baumgartner at the Computational Biophysics Lab, ETH Zurich\n" +
            "Version: 1.0-Beta Februar, 2007\n" +
            "Requires: ImageJ 1.36b or higher\n");
	 }
	
	
	/**
	 * Creates new point sources based on user selection
	 * @see MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public synchronized void mouseClicked(MouseEvent e){
		// If we are in the point selection mode and user clicks on image, a new source on refined position will be created	
		if (select_start == true){
			//TODO
			PsfSourcePosition selected = new PsfSourcePosition(canvas.offScreenX(e.getX()), canvas.offScreenY(e.getY()));
			Refine = new PsfRefinement(org_ip, (int)radius, selected);							// Start centroid refinement
			Refine.refineParticlePosition();
			selected = Refine.getRefinedParticle();
			singlePSF(selected);//TODO
			selections.setLength(0);
			selections.append(centroid);
			selections.append("\n%\t" + selected.x + "\t" + selected.y);
			
			Positions.addElement(selected);			// Add point source to vector containing user-selections
			num_of_particles = Positions.size();	// Update number of sources

			// Add Checkbox to GUI
			JCheckBox chb = new JCheckBox("Refined Centroid Position: " + selected.x + ", " + selected.y + "  Width at Half Maximum: " + (int)(whm*100)/100.0 + " nm");
			chb.setSelected(true);
			checkbox_panel.setLayout(new GridLayout(num_of_particles,1));
			checkbox_panel.add(chb);
			checkbox_panel.updateUI();
			// Make selection visible in a nice color
			color.setColor(Color.RED);
			color.drawPixel(Math.round(selected.x), Math.round(selected.y));
			imp.updateAndDraw();			
		}
	}
	
	// Following methods only needed to make sure the MouseListener interface works
	/**
	 * @see MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public synchronized void mouseEntered(MouseEvent e){}
	/**
	 * @see MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public synchronized void mouseExited(MouseEvent e){}
	/**
	 * @see MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public synchronized void mousePressed(MouseEvent e){}
	/**
	 * @see MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public synchronized void mouseReleased(MouseEvent e){}
	
	
	
	/** 
	 * Defines the action taken upon an <code>ActionEvent</code> triggered from buttons
	 * @see ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae){
		Object source = ae.getSource();	// Get Button that was clicked
		
		// start new selection
		if (source == start){
			selections.setLength(0);
			selections.append(centroid);
			eq = org_ip.duplicate();
			Positions.removeAllElements();
			num_of_particles = 0;
			
			text_panel.setText("Click on Point Source in Image now!");
			text_panel.append("\nYour Selections:");
			checkbox_panel.removeAll();
			checkbox_panel.updateUI();
			select_start = true;
			
			eq.convertToRGB();			// Convert loaded image to rgb, so selections can me made visible
			lastcolor.insert(eq,0,0);	// save image colors so selections can be undone
			color.insert(eq,0,0);
			imp.setProcessor("Point Source Selection",color);
			imp.updateAndDraw();
		}
		
		// Enhance Image Contrast and Convert Image Processor to RGB
		if (source == equalize){
			eq = enhanceContrast();		// see method enhanceContrast()
			eq.convertToRGB();			// Convert loaded image to rgb, so selections can me made visible
			lastcolor.insert(eq,0,0);	// save image colors so selections can be undone
			color.insert(eq,0,0);
			imp.setProcessor("Point Source Selection",color);
			imp.updateAndDraw();
		}
		
		// start PSF estimation TODO
		if (source == estimate){			
			if (Positions.isEmpty())
				IJ.error("Please select point source before refinement!");
			else{
				deleteUnchecked();				// only calculate psf for selected checkboxes
				if (num_of_particles == 0) {
					// All checkboxes were unchecked -> nothing to do
					return;
				}
				selections.setLength(0);		// and clear variables
				for (int i=0; i<PSF.length;i++)
					PSF[i] = 0;
				selections.append(centroid);
				// Start the calculations
				PsfSourcePosition[] particles = new PsfSourcePosition[num_of_particles];
				EstimatePSF = new PsfPointSpreadFunction[num_of_particles];
				// Loop over Point Sources
				for (int i=0; i<num_of_particles; i++){
					float[] PSFtmp;
					particles[i] = Positions.elementAt(i);
					selections.append("\n%" + (i+1) + ":\t" + particles[i].x + "\t" + particles[i].y);	// update report file
					EstimatePSF[i] = new PsfPointSpreadFunction(org_ip, particles[i], (int)sample_radius, sample_points, mag_fact, mic_mag, pix_size);
					EstimatePSF[i].calculatePSF();
					PSFtmp = EstimatePSF[i].getPSF();
					for (int j=0; j<PSFtmp.length; j++)
						PSF[j] = PSF[j] + PSFtmp[j];
				}
				// Calculate Mean
				for (int i=0; i<PSF.length;i++)
					PSF[i] = PSF[i]/num_of_particles;
				// Normalize values to [0,1] TODO
				EstimatePSF[0].normalizePSF(PSF);
				
				rad = EstimatePSF[0].getRadius();
				whm = widthAtHalfMaximum();
				text_panel.setText("PSF estimation done. Continue by starting a new selection!");
				drawPSF("PSF");
			}		
		}
		
		// Write report file
		if (source == report){
			SaveDialog sd = new SaveDialog("Save report", IJ.getDirectory("image"), "PSF_Report.txt");
			if (sd.getDirectory() == null || sd.getFileName() == null) return; 
			write2File(sd.getDirectory(), sd.getFileName(), getFullReport().toString());
			return;
		}
	}
	
	
	/**
	 * Deletes unselected checkboxes, related point sources and their color marks in the image window
	 */
	private void deleteUnchecked(){
		// lets see which checkboxes are unchecked and delete the related sources
		Component[] components = checkbox_panel.getComponents(  );
		for (int i = components.length - 1; i >= 0; i--) {
			JCheckBox cb = (JCheckBox)components[i];
			if (!cb.isSelected(  )){
	       	  	num_of_particles--;
	       	  	PsfSourcePosition last = Positions.elementAt(i);
	       	  	Positions.removeElementAt(i);
	       	  	// Get original RGB-value and re-draw
	       	  	iArray  = lastcolor.getPixel(Math.round(last.x), Math.round(last.y), iArray);
	       	  	color.putPixel(Math.round(last.x), Math.round(last.y), iArray);
	       	  	imp.updateAndDraw();
		        checkbox_panel.remove(cb);
	        }
       	}
		checkbox_panel.updateUI();	// update gui
	}
	
	
	/**
	 * GUI for Point Source Selection and PSF estimation
	 */
	private void userInterface(){;
        ui.setLayout(new BorderLayout());
		// Initialize Buttons and add ActionListener
		start = new JButton("Start New Selection");
		equalize = new JButton("Enhance Contrast");
		estimate = new JButton("Estimate PSF");
		report = new JButton("Create Report");
		start.addActionListener(this);
		equalize.addActionListener(this);
		estimate.addActionListener(this);
		report.addActionListener(this);
		// Pack Buttons onto a Panel
		button_panel = new JPanel(new GridLayout(2,2));
		button_panel.add(start);
		button_panel.add(equalize);
		button_panel.add(estimate);
		button_panel.add(report);
		// Create Panel for Checkboxes
		checkbox_panel = new JPanel();
		checkbox_panel.setLayout(new GridLayout(1,1));
		// Create TextArea for user information
		text_panel = new JTextArea("Start by clicking on \"Start New Selection\"");
		text_panel.setEditable(false);
		scroller = new JScrollPane(checkbox_panel);
		ui.getContentPane().add(text_panel, BorderLayout.NORTH);
		ui.getContentPane().add(scroller, BorderLayout.CENTER);
		ui.getContentPane().add(button_panel, BorderLayout.SOUTH);
		ui.setSize(500,250);
		ui.setLocation(300,200);
		ui.setVisible(true);
		//ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.addWindowListener(this);
	}
	
	
	/**
	 * Draws PSF of one single selection			
	 * @param single_part PointSource 
	 */
	//	TODO think about better ways to handle single/multiple selections
	private void singlePSF(PsfSourcePosition single_part){
		PsfPointSpreadFunction single_estimate = new PsfPointSpreadFunction(org_ip, single_part, (int)sample_radius, sample_points, mag_fact, mic_mag, pix_size);
		single_estimate.calculatePSF();
		PSF = single_estimate.getPSF();
		rad = single_estimate.getRadius();
		whm = widthAtHalfMaximum();
		drawPSF("PSF of Last Selection");
	}
	

	/**
	 * Calculates the Width of PSF at half maximum
	 * @return Width at half maximum
	 */
	public double widthAtHalfMaximum(){
		double d = 0.5;
		if (PSF[0]< d)
			return 0.0;
		else{
		int v = 0;
		while (PSF[v] > d){
			v++;
		}
		double p0 = PSF[v-1];
		double p1 = PSF[v];
		double r0 = rad[v-1];
		double r1 = rad[v];
		whm = r0 + (r1-r0)/(p1-p0)*(d-p0);
		return whm;
		}
	}

	
	/**
	 * Draws the plot of the calculated Point Spread Function into a PlotWindow
	 * @see PlotWindow
	 */
	private void drawPSF(String title){
		Plot plotWin = new Plot(title, "Radius [nm]", "Intensity", rad, PSF);
		plotWin.setLimits(0, rad[rad.length - 1], 0, 1);
		double[] r = new double[rad.length];
		for (int i=0; i<r.length; i++)
			r[i] = rad[i];
		double[] thpsf = theoreticalPSF();
		plotWin.setColor(Color.BLACK);
		plotWin.addLabel(0.5,0.4,"Theoretical PSF");
		plotWin.addPoints(r, thpsf, Plot.LINE);
		plotWin.setColor(Color.BLUE);
		// Draw Width at half maximum if != 0
		if (whm != 0.0f){
			plotWin.setColor(Color.BLUE);
			plotWin.addLabel(0.5,0.25,"Width at Half Maximum: " + (int)(whm*100)/100.0 + " nm");
			double[] x = new double[2];
			double[] y = new double[2];
			x[0] = 0;
			x[1] = whm;
			y[0] = 0.5;
			y[1] = 0.5;
			plotWin.addPoints(x,y,Plot.LINE);
		}
		plotWin.show();
	}
	
	
	/**
	 * Calculate theoretical PSF based on Microscope parameters and modeled as a first-order Bessel function
	 * @return Theoretical PSF
	 */
	private double[] theoreticalPSF(){
		double arg = 2*Math.PI*na/lambda;
		double[] b = new double[rad.length];
		//double barg = Math.PI*2.68;
		double barg;
		for (int i=0;i<rad.length;i++){
			barg = arg*rad[i];
			b[i] = (2*PsfBessel.j1(barg)/barg)*(2*PsfBessel.j1(barg)/barg);
		}
		b[0] = 1;
		return b;
	}
	
	
	/**
	 * Contrast Enhancement. Pixel values are scaled between 0 and 255
	 * @return Contrast Enhanced ImageProcessor converted to RGB
	 */
	private ImageProcessor enhanceContrast(){
		int max = 0;
		int min = 255;
		int c = 0;
		int w = eq.getWidth();
		int h = eq.getHeight();
		
		for (int v=0; v<h; v++) {
			for (int u=0; u<w; u++) {
				int p = eq.getPixel(u,v);
				if (p>max)
					max = p;
				if (p<min)
					min = p;
			}
		}
		c = max - min;
		
		for (int v=0; v<h; v++) {
			for (int u=0; u<w; u++) {
				int p = eq.getPixel(u,v);
				int q = (p-min)*255/c;
				eq.putPixel(u,v,q);		
			}
		}
		return eq;
	}	
	
	
	/**
	 * Creates a Textfile containing all relevant configurations and results
	 * @param directory Directory of file
	 * @param file_name File name
	 * @param info String holding the text that should be written to the file
	 * @return boolean (writing to file successful or error message)
	 */
	private boolean write2File(String directory, String file_name, String info) {
		PrintWriter print_writer = null;
		try {
            FileOutputStream fos = new FileOutputStream(directory + file_name);
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
	 * Formats all configuration parameters to a StringBuffer
	 * @see StringBuffer
	 * @return StringBuffer containing configuration parameters
	 */
	private StringBuffer getConfiguration(){
		StringBuffer configuration = new StringBuffer("%User Configuration:\n");
		configuration.append("%Apparent Radius of Point Sources [Pixel]:\t");
		configuration.append(radius + "\n");
		configuration.append("%Maximum Sampling Radius [Pixel]:\t");
		configuration.append(sample_radius + "\n");
		configuration.append("%Pixel Size on Chip [um]:\t");
		configuration.append(pix_size + "\n");
		configuration.append("%Numerical Aperture:\t");
		configuration.append(na + "\n");
		configuration.append("%Wavelength [nm]:\t");
		configuration.append(lambda + "\n");
		configuration.append("%Microscope Magnification Factor:\t");
		configuration.append(mic_mag + "\n");
		configuration.append("%Pixel Magnification:\t");
		configuration.append(mag_fact + "\n");
		configuration.append("%Number of Sample Points:\t");
		configuration.append(sample_points + "\n");
		return configuration;
	}
	
	
	/**
	 * Returns PSF values as StringBuffer
	 * @return StringBuffer with PSF values
	 */
	private StringBuffer getPSFdata(){
		StringBuffer PSFdata = new StringBuffer("%PSF Data:\n");
		PSFdata.append("%\tRadius [nm]\t\tValue\n");
		for (int i=0;i<PSF.length;i++)
			PSFdata.append("\t" + rad[i] + "\t\t" + PSF[i] + "\n");
				
		PSFdata.append("%\n");
		PSFdata.append("%Width at half maximum [nm]:\t" + (int)(whm*100)/100.0 + "\n");
		return PSFdata;
	}
	
	
	/**
	 * Collects all program information and appends it to a StringBuffer
	 * @see StringBuffer
	 * @return StringBuffer containing all program information
	 */
	private StringBuffer getFullReport() {
			StringBuffer report = new StringBuffer();
			report.append(this.getConfiguration());
			report.append("\n");
			report.append(this.selections);
			if (PSF != null){
				report.append("\n\n");
				report.append(getPSFdata().toString());
			}
			report.append("\n");
			if (EstimatePSF != null){
				for (int i=0; i<EstimatePSF.length; i++){
					report.append("\n\n%Point Source " + (i+1) + ":");
					report.append(EstimatePSF[i].getPSFreport());
				}
			}	
			else
				report.append("Press Button \"Estimate PSF\" before creating the report file to get more detailed results!");
			return report;
	}


	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	public void windowClosing(WindowEvent arg0) {
        ui.setVisible(false);
        ui.dispose();
        canvas.removeMouseListener(this);
		
	}


	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}