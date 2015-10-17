package mosaic.plugins;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.StackWindow;
import ij.process.ShortProcessor;
import mosaic.core.utils.LabelImage;

/**
 * This class is used for showing results of segmentation (step by step if requested).
 * It keeps window with possibility to add new slices (step) of segmentation. Handles
 * both - 2D and 3D - images.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SegmentationProcessWindow {
    // stack saving the segmentation progress images
    protected ImageStack iStack;
    // ImagePlus showing iStack
    protected ImagePlus stackImPlus;
    // Maximum label number for showing stack
    private int iMaxLabel = 100;
    // Should stack keep all added slices or only last added?
    private boolean shouldKeepAllSlices = false;
    
    /**
     * Creates output ImagePlus with given dimensions
     * @param aWidth
     * @param aHeight
     * @param aShouldKeepAllSlices - Should stack keep all added slices or only last added?
     */
    SegmentationProcessWindow(int aWidth, int aHeight, boolean aShouldKeepAllSlices) {
        shouldKeepAllSlices = aShouldKeepAllSlices;

        // Generate stack and ImagePlus with given dimensions
        stackImPlus = new ImagePlus(null, new ShortProcessor(aWidth, aHeight));
        iStack = stackImPlus.createEmptyStack();
        stackImPlus.show();
        if (stackImPlus.getWindow() != null) {
            // Add listener in case when window is generated (not in macro/batch mode)
            stackImPlus.getWindow().addWindowListener(new StackWindowListener());
        }
    }
    
    /**
     * Adds new LabelImageRC to stack
     * @param aLabelImage - image to add
     * @param aTitle - title for image
     * @param aBiggestLabelSoFar - maximum label number used so far
     */
    void addSliceToStack(LabelImage aLabelImage, String aTitle, int aBiggestLabelSoFar ) {
        if (iStack == null) {
            // stack was closed by user, don't reopen
            return;
        }
        
        final int dim = aLabelImage.getNumOfDimensions();
        if (dim <= 3) {
            addSliceToHyperstack(aTitle, aLabelImage.getShortStack(false));
        }
        else {
            throw new RuntimeException("Unsupported dimensions: " + dim);
        }
        
        // Handle new maximum label
        if (aBiggestLabelSoFar > iMaxLabel) {
            iMaxLabel = 2 * aBiggestLabelSoFar;
        }
        adjustLUT();
    }
    
    /**
     * Closes SegmentationProcessWindow
     */
    void close() {
        if (stackImPlus != null) {
            stackImPlus.close();
        }
    }
    
    /**
     * Adds a new slice pixels to the end of the stack, and sets the new stack position to this slice
     *
     * @param aTitle Title of the stack slice
     * @param aPixels data of the new slice (pixel array)
     */
    void addSliceToStackAndShow(String aTitle, final short[] aPixels) {
        if (!shouldKeepAllSlices) {
            iStack.deleteLastSlice();
        }

        iStack.addSlice(aTitle, aPixels);
        stackImPlus.setStack(iStack);
        stackImPlus.setPosition(iStack.getSize());

        adjustLUT();
    }

    /**
     * Adds slices for 3D images to stack, overwrites old images.
     */
    private void add3DtoStaticStack(String aTitle, ImageStack aStackSlices) {
        int oldpos = stackImPlus.getCurrentSlice();

        while (iStack.getSize() > 0) {
            iStack.deleteLastSlice();
        }

        final int numOfSlices = aStackSlices.getSize();
        for (int i = 1; i <= numOfSlices; i++) {
            iStack.addSlice(aTitle + " " + i, aStackSlices.getPixels(i));
        }

        stackImPlus.setStack(iStack);
        stackImPlus.setPosition(oldpos);
    }

    /**
     * TODO: Old implementation moved from Region_Competion. Seems that should be revised
     *       and maybe merged with add3DtoStaticStack.
     * Shows 3D segmentation progress in a hyperstack
     */
    private void addSliceToHyperstack(String title, ImageStack stackslice) {
        if (!shouldKeepAllSlices) {
            add3DtoStaticStack(title, stackslice);
            return;
        }

        // clean the stack, hyperstack must not contain additional slices
        while (iStack.getSize() % stackslice.getSize() != 0) {
            iStack.deleteSlice(1);
        }

        // in first iteration, convert to hyperstack
        if (stackImPlus.getNFrames() <= 2) {
            final ImagePlus imp2 = stackImPlus;
            imp2.setOpenAsHyperStack(true);
            new StackWindow(imp2);
        }

        int lastSlice = stackImPlus.getSlice();
        final int lastFrame = stackImPlus.getFrame();
        final boolean wasLastFrame = lastFrame == stackImPlus.getDimensions()[4];

        for (int i = 1; i <= stackslice.getSize(); i++) {
            iStack.addSlice(title + i, stackslice.getProcessor(i));
        }

        final int total = iStack.getSize();
        final int depth = stackslice.getSize();
        final int timeSlices = total / depth;

        stackImPlus.setDimensions(1, depth, timeSlices);

        // scroll lock on last frame
        int nextFrame = lastFrame;
        if (wasLastFrame) {
            nextFrame++;
        }

        // go to mid in first iteration
        if (timeSlices <= 2) {
            lastSlice = depth / 2;
        }
        try {
            // sometimes here is a ClassCastException
            // when scrolling in the hyperstack
            // it's a IJ problem... catch the Exception, hope it helps
            stackImPlus.setPosition(1, lastSlice, nextFrame);
        }
        catch (final Exception e) {
            System.out.println(e);
        }
    }
    
    private void adjustLUT() {
        IJ.setMinAndMax(stackImPlus, 0, iMaxLabel);
        IJ.run(stackImPlus, "3-3-2 RGB", null);
    }

    /**
     * This {@link WindowListener} sets stack to null if stackwindow was closed by user. 
     * This indicates to not further producing stackframes. 
     * For hyperstacks (for which IJ reopens new Window on each update) it hooks to the new Windows.
     */
    private class StackWindowListener implements WindowListener {

        protected StackWindowListener() {}

        @Override
        public void windowClosing(WindowEvent e) {
            iStack = null;
        }

        @Override
        public void windowClosed(WindowEvent e) {
            // hook to new window
            final Window win = stackImPlus.getWindow();
            if (win != null) {
                win.addWindowListener(this);
            }
        }

        @Override
        public void windowOpened(WindowEvent e) {}

        @Override
        public void windowIconified(WindowEvent e) {}

        @Override
        public void windowDeiconified(WindowEvent e) {}

        @Override
        public void windowDeactivated(WindowEvent e) {}

        @Override
        public void windowActivated(WindowEvent e) {}
    }
}
