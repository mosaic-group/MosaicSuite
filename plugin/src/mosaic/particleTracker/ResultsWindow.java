package mosaic.particleTracker;

import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.util.Iterator;

import ij.IJ;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.text.TextPanel;
import mosaic.core.utils.MosaicUtils;
import mosaic.plugins.ParticleTracker3DModular_;

/**
 * Defines a window to be the main user interface for result display and analysis
 * upon completion of the algorithm.
 * <br>
 * All user requests will be listened to and engaged from the <code>actionPerformed</code>
 * method implemented here
 */
public class ResultsWindow extends Frame implements FocusListener, ActionListener {
    private final ParticleTracker3DModular_ particleTracker3DModular;
    private static final long serialVersionUID = 1L;
    public final TextPanel text_panel;
    public final TextPanel configuration_panel;
    private final Button view_static, save_report, display_report, dummy, plot_particle, trajectory_focus, trajectory_info, traj_in_area_info, area_focus;

    private final Button transfer_particles, transfer_trajs; // in panel left
    private final Button mssButton, transfer_traj; // in panel center
    private final Button mssAllResultsButton;
    private final Button mssTrajectoryResultButton;

    final Label per_traj_label;
    private final Label area_label;
    private final Label all_label;
    private final MenuItem mag_factor, relink_particles;

    /**
     * Constructor.
     * <br>
     * Creates an instance of a ResultsWindow with all GUI elements in it,
     * sets its size and location on the screen.
     * 
     * @param title - title of the results window
     * @param particleTracker3DModular_ TODO
     */
    public ResultsWindow(ParticleTracker3DModular_ particleTracker3DModular_, String title) {

        super(title);
        particleTracker3DModular = particleTracker3DModular_;
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        addFocusListener(this);

        /* Set the layout of the window */
        final GridBagLayout gridbag = new GridBagLayout();
        final GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;

        /* Add a TextPanel to the window for display of the configuration params */
        c.weightx = 0.25;
        c.weighty = 0.25;
        configuration_panel = new TextPanel("configuration");
        gridbag.setConstraints(configuration_panel, c);
        add(configuration_panel);

        /* Add a TextPanel to the window for display results from user queries */
        c.weightx = 1;
        c.weighty = 1;
        text_panel = new TextPanel("Results");
        text_panel.setTitle("Results");
        gridbag.setConstraints(text_panel, c);
        add(text_panel);

        /*----------------------------------------------------*/
        /* Panel to hold buttons for all trajectories options */
        /*----------------------------------------------------*/
        final Panel all_options = new Panel();
        all_options.setBackground(Color.LIGHT_GRAY);
        all_options.setLayout(gridbag);

        /* Create the label for this Panel */
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
        mssAllResultsButton = new Button(" All MSS/MSD to Table ");
        mssAllResultsButton.addActionListener(this);

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
        gridbag.setConstraints(mssAllResultsButton, c);
        all_options.add(mssAllResultsButton);

        /*--------------------------------------------------*/

        /*--------------------------------------------------*/
        /* Panel to hold buttons for pre trajectory options */
        /*--------------------------------------------------*/
        final Panel per_traj_options = new Panel();
        per_traj_options.setBackground(Color.GRAY);
        per_traj_options.setLayout(gridbag);

        /* Create the label for this Panel */
        per_traj_label = new Label("Trajectory (select from visual)", Label.CENTER);

        /* Create 3 buttons and set this class to be their ActionListener */
        trajectory_focus = new Button("Focus on Selected Trajectory");
        trajectory_focus.addActionListener(this);
        trajectory_info = new Button("Selected Trajectory Info");
        trajectory_info.addActionListener(this);
        plot_particle = new Button("");
        plot_particle.addActionListener(this);
        plot_particle.setEnabled(false);
        mssButton = new Button(" MSS/MSD plots ");
        mssButton.addActionListener(this);
        transfer_traj = new Button("Selected Trajectory to Table");
        transfer_traj.addActionListener(this);
        mssTrajectoryResultButton = new Button(" MSS/MSD to Table");
        mssTrajectoryResultButton.addActionListener(this);

        /* Add the Label and 3 buttons to the per_traj_options Panel */
        gridbag.setConstraints(per_traj_label, c);
        per_traj_options.add(per_traj_label);
        gridbag.setConstraints(trajectory_focus, c);
        per_traj_options.add(trajectory_focus);
        gridbag.setConstraints(trajectory_info, c);
        per_traj_options.add(trajectory_info);
        gridbag.setConstraints(plot_particle, c);
        per_traj_options.add(plot_particle);
        gridbag.setConstraints(transfer_traj, c);
        per_traj_options.add(transfer_traj);
        gridbag.setConstraints(mssButton, c);
        per_traj_options.add(mssButton);
        gridbag.setConstraints(mssTrajectoryResultButton, c);
        per_traj_options.add(mssTrajectoryResultButton);

        /*----------------------------------------*/
        /* Panel to hold buttons for area options */
        /*----------------------------------------*/
        final Panel area_options = new Panel();
        area_options.setBackground(Color.LIGHT_GRAY);
        area_options.setLayout(gridbag);

        /* Create the label for this Panel */
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

        /* Create a Panel to contain all the 3 first panels */
        final Panel all_panels = new Panel(new GridLayout(1, 3));
        all_panels.add(all_options);
        all_panels.add(per_traj_options);
        all_panels.add(area_options);

        /* Add the all_panels Panel to the window */
        c.weighty = 0.01;
        gridbag.setConstraints(all_panels, c);
        add(all_panels);

        /* Create a Menu for viewing preferences */
        final Menu view = new Menu("View Preferences");
        mag_factor = new MenuItem("Magnification factor");
        mag_factor.addActionListener(this);
        view.add(mag_factor);

        /* Create a Menu for re linking of particles */
        final Menu relink = new Menu("Relink Particles");
        relink_particles = new MenuItem("set new parameters for linking");
        relink_particles.addActionListener(this);
        relink.add(relink_particles);

        /* Set the MenuBar of this window to hold the 2 menus */
        final MenuBar mb = new MenuBar();
        mb.add(view);
        mb.add(relink);
        this.setMenuBar(mb);

        this.pack();
        WindowManager.addWindow(this);
        this.setSize((int) getMinimumSize().getWidth(), 512);
        GUI.center(this);
    }

    /*
     * (non-Javadoc)
     * @see java.awt.Window#processWindowEvent(java.awt.event.WindowEvent)
     */
    @Override
    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        final int id = e.getID();
        if (id == WindowEvent.WINDOW_CLOSING) {
            setVisible(false);
            dispose();
            WindowManager.removeWindow(this);
        }
        else if (id == WindowEvent.WINDOW_ACTIVATED) {
            WindowManager.setWindow(this);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     */
    @Override
    public void focusGained(FocusEvent e) {
        WindowManager.setWindow(this);
    }

    // =====================================================================================
    // Fields required by trajectory analysis
    public double pixelDimensions = 1; // physical pixel dimensions in meters
    public double timeInterval = 1; // physical time interval between frames in seconds
    
    /*
     * (non-Javadoc)
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     */
    @Override
    public void focusLost(FocusEvent e) {
    }

    /**
     * Takes care about user actions.
     */
    @Override
    public synchronized void actionPerformed(ActionEvent ae) {
        final Object source = ae.getSource();
        Roi user_roi = null;

        /* view all trajectories */
        if (source == view_static) {
            // a new view is requested so reset the filter and generate a NEW view
            particleTracker3DModular.resetTrajectoriesFilter();
            particleTracker3DModular.generateView(null, particleTracker3DModular.out);
            return;
        }

        if (source == mssButton || source == mssTrajectoryResultButton || source == mssAllResultsButton) {

            // Get all calibration data from image
            final double width = particleTracker3DModular.iInputImage.getCalibration().pixelWidth;
            final double height = particleTracker3DModular.iInputImage.getCalibration().pixelHeight;
            final double interval = particleTracker3DModular.iInputImage.getCalibration().frameInterval;
            final String intervalUnit = particleTracker3DModular.iInputImage.getCalibration().getTimeUnit();
            final String unit = particleTracker3DModular.iInputImage.getCalibration().getUnit();

            // Do checking and complain if necessary
            String message = "";
            if (width != height) {
                message += "Pixel width is different than height. \n";
            }
            else if (interval == 0) {
                message += "Frame interval is equall to 0. To perform analysis it must have correct value \n";
            }
            else if (!(unit.equals("nm") || unit.equals(IJ.micronSymbol + "m") || unit.equals("um") || unit.equals("mm") || unit.equals("m"))) {
                message += "Dimension unit must be one of: m, mm, um or (" + IJ.micronSymbol + "m), nm";
            }
            else if (!(intervalUnit.equals("us") || intervalUnit.equals(IJ.micronSymbol + "s") || intervalUnit.equals("ms") || intervalUnit.equals("sec") || intervalUnit.equals("s"))) {
                message += "Time interval unit must be one of: s, sec, ms, us, " + IJ.micronSymbol + "m";
            }
            if (!message.equals("")) {
                IJ.showMessage(message);
                WindowManager.setCurrentWindow(particleTracker3DModular.iInputImage.getWindow());
                IJ.run("Properties...");
                return;
            }
            else {
                // All provided data are correct! Get it and recalculate if
                // needed.

                pixelDimensions = width;
                timeInterval = interval;

                // Convert dimension unit to meters
                if (unit.equals("nm")) {
                    pixelDimensions /= 1000000000; // convert from nm to m
                }
                else if (unit.equals(IJ.micronSymbol + "m") || unit.equals("um")) {
                    pixelDimensions /= 1000000; // convert from um to m
                }
                else if (unit.equals("mm")) {
                    pixelDimensions /= 1000; // convert from mm to nm
                }

                // convert time unit to seconds
                if (intervalUnit.equals(IJ.micronSymbol + "s") || intervalUnit.equals("us")) {
                    timeInterval /= 1000000; // convert from us to s
                }
                else if (intervalUnit.equals("ms")) {
                    timeInterval /= 1000; // convert from ms to s
                }

            }
        }

        /* save full report to file */
        if (source == save_report) {
            // show save file user dialog with default file name 'Traj_{title}.txt'
            final SaveDialog sd = new SaveDialog("Save report", IJ.getDirectory("image"), "Traj_" + particleTracker3DModular.resultFilesTitle, ".txt");
            // if user cancelled the save dialog - return
            if (sd.getDirectory() == null || sd.getFileName() == null) {
                return;
            }
            // write full report to file
            MosaicUtils.write2File(sd.getDirectory(), sd.getFileName(), particleTracker3DModular.getFullReport().toString());
            return;
        }
        /* display full report on the text_panel */
        if (source == display_report) {
            text_panel.selectAll();
            text_panel.clearSelection();
            text_panel.append(particleTracker3DModular.getFullReport().toString());
            return;
        }
        /* check validity of state for area selection */
        if (source == area_focus || source == traj_in_area_info) {
            // for these options, an area (ROI) has to be selected on the display
            // varify it here
            user_roi = IJ.getImage().getRoi();
            if (user_roi == null) {
                IJ.error("The active image does not have a selection\n" + "Please select an area of interest first\n" + "Click and drag the mouse on the active image.");
                return;
            }

            /* create area focus view */
            if (source == area_focus) {
                particleTracker3DModular.generateAreaFocusView(particleTracker3DModular.magnification_factor);
                return;
            }
            /* display (on the text_panel) info about trajectories that are in the selected area */
            if (source == traj_in_area_info) {
                text_panel.selectAll();
                text_panel.clearSelection();
                final Iterator<Trajectory> iter = particleTracker3DModular.all_traj.iterator();
                // iterate of all trajectories
                while (iter.hasNext()) {
                    final Trajectory traj = iter.next();
                    // for each trajectory - go over all particles
                    for (int i = 0; i < traj.existing_particles.length; i++) {
                        // if a particle in the trajectory is within the ROI
                        // print traj information to screen and go to next trajectory
                        if (user_roi.getBounds().contains(traj.existing_particles[i].iY, traj.existing_particles[i].iX) && traj.to_display) {
                            text_panel.appendLine("%% Trajectory " + traj.serial_number);
                            text_panel.append(particleTracker3DModular.trajectoryHeader());
                            text_panel.append(traj.toString());
                            break;
                        }
                    } // for
                } // while
                return;
            }
        }
        
        /* check validity of state for Trajectory selection */
        if (source == trajectory_focus || source == trajectory_info || source == transfer_traj || source == mssButton || source == mssTrajectoryResultButton) {
            // These options can only be requested after selecting a trajectory from the view
            // verify it here
            if (particleTracker3DModular.chosen_traj == -1) {
                IJ.error("Please select a trajectory first\n" + "Click with the mouse on a trajectory in 'All trajectories' display");
                return;
            }
        }

        /* create Trajectory focus view */
        if (source == trajectory_focus) {
            // user selects trajectory according to serial number (starts with 1)
            // but all_traj Vector starts from 0 so (chosen_traj-1)
            particleTracker3DModular.generateTrajFocusView(particleTracker3DModular.chosen_traj, particleTracker3DModular.magnification_factor);
            return;
        }
        /* display (on the text_panel) info about the selected Trajectory */
        if (source == trajectory_info) {
            // user selects trajectory according to serial number (starts with 1)
            // but all_traj Vector starts from 0 so (chosen_traj-1)
            final Trajectory traj = particleTracker3DModular.all_traj.elementAt(particleTracker3DModular.chosen_traj);
            text_panel.selectAll();
            text_panel.clearSelection();
            text_panel.appendLine("%% Trajectory " + traj.serial_number);
            text_panel.append(particleTracker3DModular.trajectoryHeader());
            text_panel.append(traj.toString());
            return;
        }

        if (source == transfer_traj) {
            final Trajectory traj = particleTracker3DModular.all_traj.elementAt(particleTracker3DModular.chosen_traj);
            particleTracker3DModular.transferSelectedTrajectoriesToResultTable(traj).show("Results");
            return;
        }
        /* define the mag factor for rescaling of focused view */
        if (source == mag_factor) {
            final String[] mag_choices = { "1", "2", "4", "6", "8", "10" };
            final GenericDialog mag_dialog = new GenericDialog("Select Magnification Factor");
            mag_dialog.addChoice("Magnification factor", mag_choices, "" + particleTracker3DModular.magnification_factor);
            mag_dialog.showDialog();
            if (mag_dialog.wasCanceled()) {
                return;
            }
            particleTracker3DModular.magnification_factor = Integer.parseInt(mag_dialog.getNextChoice());
            return;
        }
        /* option to relink the deteced particles with new parameters */
        if (source == relink_particles) {
            final GenericDialog relink_dialog = new GenericDialog("Select new linking parameters");
            relink_dialog.addNumericField("Link Range", particleTracker3DModular.linkrange, 0);
            relink_dialog.addNumericField("Displacement", particleTracker3DModular.displacement, 2);
            relink_dialog.showDialog();
            if (relink_dialog.wasCanceled()) {
                return;
            }
            particleTracker3DModular.linkrange = (int) relink_dialog.getNextNumber();
            particleTracker3DModular.displacement = relink_dialog.getNextNumber();
            particleTracker3DModular.all_traj = null;

            /* link the particles found */
            IJ.showStatus("Linking Particles");
            boolean linkParticlesResult = particleTracker3DModular.linkParticles();
            if (linkParticlesResult == false) {
                return;
            }

            /* generate trajectories */
            IJ.showStatus("Generating Trajectories");
            particleTracker3DModular.generateTrajectories();

            configuration_panel.selectAll();
            configuration_panel.clearSelection();
            configuration_panel.append(particleTracker3DModular.getConfiguration().toString());
            configuration_panel.append(particleTracker3DModular.getInputFramesInformation().toString());

            text_panel.selectAll();
            text_panel.clearSelection();
            text_panel.appendLine("Relinking DONE!");
            text_panel.appendLine("Found " + particleTracker3DModular.number_of_trajectories + " Trajectories");
            return;
        }
        /* transfer segmented particle coordinates to ImageJ results window */
        if (source == transfer_particles) {
            particleTracker3DModular.transferParticlesToResultsTable(); // version 1.4 20101115
            return;
        }

        /* transfer trajectory coordinates to ImageJ results window */
        if (source == transfer_trajs) {
            particleTracker3DModular.transferTrajectoriesToResultTable().show("Results");
            return;
        }
        if (source == mssTrajectoryResultButton) {
            final Trajectory trajectory = particleTracker3DModular.all_traj.elementAt(particleTracker3DModular.chosen_traj);
            particleTracker3DModular.mssTrajectoryResultsToTable(trajectory, pixelDimensions, timeInterval);
            return;
        }

        if (source == mssAllResultsButton) {
            particleTracker3DModular.mssAllResultsToTable(pixelDimensions, timeInterval);
            return;
        }

        if (source == mssButton) {
            final Trajectory trajectory = particleTracker3DModular.all_traj.elementAt(particleTracker3DModular.chosen_traj);
            new TrajectoryAnalysisPlot(trajectory, pixelDimensions, timeInterval);
            return;
        }

        IJ.error("Unhandled event: [" + ae.getActionCommand() + "]");
    }
}