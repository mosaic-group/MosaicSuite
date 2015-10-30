package mosaic.particleTracker;


import java.awt.Button;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.measure.Calibration;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.MyFrame.DrawType;
import mosaic.plugins.ParticleTracker3DModular_;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;


/**
 * Class that visualize a window with trajectory + insert a mouse listener to select trajectory
 *
 * @author Pietro Incardona
 */
public class TrajectoryStackWin extends StackWindow implements MouseListener {

    private final ParticleTracker3DModular_ particleTracker3DModular;
    private static final long serialVersionUID = 1L;
    private Button filter_length;
    private final Label numberOfParticlesLabel;
    private Img<ARGBType> out;

    /**
     * Constructor. <br>
     * Creates an instance of TrajectoryStackWindow from a given <code>ImagePlus</code> and <code>ImageCanvas</code> and a creates GUI panel. <br>
     * Adds this class as a <code>MouseListener</code> to the given <code>ImageCanvas</code>
     * 
     * @param aimp
     * @param icanvas
     * @param particleTracker3DModular_
     */
    public TrajectoryStackWin(ParticleTracker3DModular_ particleTracker3DModular_, ImagePlus aimp, ImageCanvas icanvas, Img<ARGBType> out_) {
        super(aimp, icanvas);
        particleTracker3DModular = particleTracker3DModular_;
        numberOfParticlesLabel = new Label("");
        icanvas.addMouseListener(this);
        addPanel();
        changeParticleNumberLabel();
        out = out_;
    }

    private void changeParticleNumberLabel() {
        int currentframe = this.getImagePlus().getSlice() - 1;

        // understand the dimansionality
        final int nslices = this.getImagePlus().getNSlices();
        final int nframes = this.getImagePlus().getNFrames();

        // check the dimensionality
        if (nslices == 1) {
            // 2D
            currentframe = this.getImagePlus().getChannel();
        }
        else if (nframes == 1) {
            // 3D
            currentframe = this.getImagePlus().getSlice();
        }

        numberOfParticlesLabel.setText("Frame " + currentframe + ": " + particleTracker3DModular.frames[currentframe - 1].real_particles_number + " particles");
    }

    @Override
    public String createSubtitle() {
        // overrided to get the right moment to update the label.
        changeParticleNumberLabel();
        return super.createSubtitle();
    }

    /**
     * Adds a Panel with filter options button and number of particles in it to this window
     */
    private void addPanel() {
        final Panel panel = new Panel(new GridLayout(2, 1));
        filter_length = new Button(" Filter Options ");
        filter_length.addActionListener(this);
        panel.add(filter_length);
        panel.add(numberOfParticlesLabel);
        add(panel);
        pack();
        final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        final Point loc = getLocation();
        final Dimension size = getSize();
        if (loc.y + size.height > screen.height) {
            getCanvas().zoomOut(0, 0);
        }
    }

    /**
     * Defines the action taken upon an <code>ActionEvent</code> triggered from buttons that have class <code>TrajectoryStackWindow</code> as their action listener: <br>
     * <code>Button filter_length</code>
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        final Object b = e.getSource();
        if (b == filter_length) {
            // if user cancelled the filter dialog - do nothing
            if (!particleTracker3DModular.filterTrajectories()) {
                return;
            }
        }
        // Regenerate the image
        out = particleTracker3DModular.createHyperStackFromFrames(particleTracker3DModular.background);

        // generate an updated view with the ImagePlus in this window according to the new filter
        particleTracker3DModular.generateView(this.imp, this.out);
    }

    /**
     * Defines the action taken upon an <code>MouseEvent</code> triggered by left-clicking the mouse anywhere in this <code>TrajectoryStackWindow</code>
     * 
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    @Override
    public synchronized void mousePressed(MouseEvent e) {

        /* Reset selected trajectory */
        if (particleTracker3DModular.chosen_traj != -1) {
            final Vector<Trajectory> v = new Vector<Trajectory>();
            v.add(particleTracker3DModular.all_traj.get(particleTracker3DModular.chosen_traj));

            final Calibration cal = particleTracker3DModular.original_imp.getCalibration();

            MyFrame.updateImage(out, v, cal, DrawType.TRAJECTORY_HISTORY, particleTracker3DModular.getRadius());

            particleTracker3DModular.chosen_traj = -1;
        }

        /* get the coordinates of mouse while it was clicked */
        final int x = e.getX();
        final int y = e.getY();
        /* covert them to offScreen coordinates using the ImageCanvas of this window */
        final int offscreenX = this.ic.offScreenX(x);
        final int offscreenY = this.ic.offScreenY(y);

        boolean trajectory_clicked = false;
        final Iterator<Trajectory> iter = particleTracker3DModular.all_traj.iterator();

        /* Get pixel color */
        if (this.imp == null) {
            // there is not image this listener is dead remove it
            removeMouseListener(this);
            return;
        }

        final int cl[] = this.imp.getPixel(offscreenX, offscreenY);

        /* find the best Trajectory to match the mouse click */
        int ct = 0;
        while (iter.hasNext()) {
            final Trajectory curr_traj = iter.next();

            if (curr_traj.color.getRed() == cl[0] && curr_traj.color.getGreen() == cl[1] && curr_traj.color.getBlue() == cl[2]) {
                // Redraw trajectory white

                final Vector<Trajectory> v = new Vector<Trajectory>();
                v.add(curr_traj);

                final Calibration cal = particleTracker3DModular.original_imp.getCalibration();

                MyFrame.updateImage(out, v, cal, DrawType.TRAJECTORY_HISTORY, particleTracker3DModular.getRadius());

                trajectory_clicked = true;
                particleTracker3DModular.chosen_traj = ct;
                break;
            }

            ct++;

        }

        if (trajectory_clicked) {
            /* focus or mark the selected Trajectory according the the type of mouse click */
            this.imp.killRoi();
            this.imp.updateImage();
            // show the number of the selected Trajectory on the per trajectory
            // panel in the results window
            particleTracker3DModular.results_window.per_traj_label.setText("Trajectory " + (particleTracker3DModular.chosen_traj + 1));
            if (e.getClickCount() == 2) {
                // "double-click"
                // Set the ROI to the trajectory focus_area
                IJ.getImage().setRoi((particleTracker3DModular.all_traj.elementAt(particleTracker3DModular.chosen_traj)).focus_area);
                // focus on Trajectory (ROI)
                particleTracker3DModular.generateTrajFocusView(particleTracker3DModular.chosen_traj, particleTracker3DModular.magnification_factor);
            }
            else {
                // single-click - mark the selected trajectory by setting the ROI to the
                // trajectory mouse_selection_area
                this.imp.setRoi((particleTracker3DModular.all_traj.elementAt(particleTracker3DModular.chosen_traj)).mouse_selection_area);
            }
        }
        else {
            particleTracker3DModular.chosen_traj = -1;
            particleTracker3DModular.results_window.per_traj_label.setText("Trajectory (select from view)");
        }

        // ?????????????? is this the only way to update
        this.showSlice(this.imp.getCurrentSlice() + 1);
        this.showSlice(this.imp.getCurrentSlice() - 1);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }
}
