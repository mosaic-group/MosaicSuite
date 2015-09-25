package mosaic.particleTracker;

import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import ij.ImagePlus;
import ij.gui.StackWindow;

/**
 * Class that visualize a window with a focused trajectory
 *
 * @author Pietro Incardona
 */
public class FocusStackWin extends StackWindow implements ItemListener {

    private static final long serialVersionUID = 1L;
    private Checkbox Auto_Z;
    Trajectory traj;
    float scal_z;

    /**
     * Constructor.
     * <br>
     * Creates an instance of FocusStackWindow from a given <code>ImagePlus</code>
     * and <code>ImageCanvas</code> and a creates GUI panel.
     * <br>
     * Adds this class as a <code>MouseListener</code> to the given <code>ImageCanvas</code>
     * 
     * @param aimp
     * @param icanvas
     */
    public FocusStackWin(ImagePlus aimp, Trajectory traj_, float scal_z_) {
        super(aimp);
        addPanel();

        traj = traj_;
        scal_z = scal_z_;

        // Add a listener on t scrollbar
        if (tSelector != null) {
            tSelector.addAdjustmentListener(this);
        }
        else if (zSelector != null) {
            zSelector.addAdjustmentListener(this);
        }
        else if (cSelector != null) {
            cSelector.addAdjustmentListener(this);
        }
    }

    /**
     * Adds a Panel with filter options button and number of particles in it to this window
     */
    private void addPanel() {
        Panel panel = new Panel(new GridLayout(2, 1));
        Auto_Z = new Checkbox("Auto Z");
        Auto_Z.addItemListener(this);
        panel.add(Auto_Z);
        add(panel);
        pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Point loc = getLocation();
        Dimension size = getSize();
        if (loc.y + size.height > screen.height) {
            getCanvas().zoomOut(0, 0);
        }
    }

    @Override
    public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
        if (Auto_Z.getState() == true) {
            int frame = 0;
            if (getImagePlus().getNFrames() != 1) {
                frame = getImagePlus().getFrame() - 1;
            }
            else if (getImagePlus().getNSlices() != 1) {
                frame = getImagePlus().getSlice() - 1;
            }
            else if (getImagePlus().getNChannels() != 1) {
                frame = getImagePlus().getChannel() - 1;
            }

            int slice = (int) (traj.existing_particles[frame].z / scal_z);
            getImagePlus().setSlice(slice);

            if (getImagePlus().getNFrames() != 1) {
                getImagePlus().setPosition(getImagePlus().getChannel(), slice + 1, getImagePlus().getFrame());
            }
            else if (getImagePlus().getNSlices() != 1) {
                getImagePlus().setPosition(slice + 1, getImagePlus().getSlice(), getImagePlus().getFrame());
            }
        }
        super.adjustmentValueChanged(e);
    }

    @Override
    public void itemStateChanged(ItemEvent arg0) {
        if (Auto_Z.getState() == true) {
            int frame = 0;
            if (getImagePlus().getNFrames() != 1) {
                frame = getImagePlus().getFrame() - 1;
            }
            else if (getImagePlus().getNSlices() != 1) {
                frame = getImagePlus().getSlice() - 1;
            }
            else if (getImagePlus().getNChannels() != 1) {
                frame = getImagePlus().getChannel() - 1;
            }

            int slice = (int) (traj.existing_particles[frame].z / scal_z);
            getImagePlus().setSlice(slice);

            if (getImagePlus().getNFrames() != 1) {
                getImagePlus().setPosition(getImagePlus().getChannel(), slice + 1, getImagePlus().getFrame());
            }
            else if (getImagePlus().getNSlices() != 1) {
                getImagePlus().setPosition(slice + 1, getImagePlus().getSlice(), getImagePlus().getFrame());
            }
        }

    }
} // FocusStackWindow inner class