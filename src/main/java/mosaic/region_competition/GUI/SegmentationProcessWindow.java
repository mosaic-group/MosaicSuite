package mosaic.region_competition.GUI;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.StackWindow;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;

/**
 * This class is used for showing results of segmentation (step by step if requested).
 * It keeps window with possibility to add new slices (step) of segmentation. Handles
 * both - 2D and 3D - images.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SegmentationProcessWindow {
    protected ImageStack iStack;
    protected ImagePlus iImage;
    private int iMaxLabel = 0;
    private boolean iShouldKeepAllSlices = false;
    private boolean iIsThatFirstPass = true;
    
    /**
     * Creates output ImagePlus with given dimensions
     * @param aWidth
     * @param aHeight
     * @param aShouldKeepAllSlices - Should stack keep all added slices or only last added?
     */
    public SegmentationProcessWindow(int aWidth, int aHeight, boolean aShouldKeepAllSlices, boolean aFloat) {
        iShouldKeepAllSlices = aShouldKeepAllSlices;

        iImage = aFloat == false ? new ImagePlus(null, new ShortProcessor(aWidth, aHeight)) :
                                   new ImagePlus(null, new FloatProcessor(aWidth, aHeight));
        iStack = iImage.createEmptyStack();
        
        if (iImage.getWindow() != null) {
            // Add listener in case when window is created (not in macro/batch mode)
            iImage.getWindow().addWindowListener(new StackWindowListener());
        }
    }
    
    public SegmentationProcessWindow(int aWidth, int aHeight, boolean aShouldKeepAllSlices) {
        this(aWidth, aHeight, aShouldKeepAllSlices, false);
    }
    
    public ImagePlus getImage() {
        return iImage;
    }
    
    /**
     * Adds new LabelImageRC to stack
     * @param aLabelImage - image to add
     * @param aTitle - title for image
     * @param aBiggestLabelSoFar - maximum label value used so far
     */
    public void addSliceToStack(IntensityImage aLabelImage, String aTitle, int aBiggestLabelSoFar ) {
        if (iStack == null) {
            // stack was closed by user, don't reopen
            return;
        }
        
        if (aLabelImage.getNumOfDimensions() <= 3) {
            addStack(aTitle, aLabelImage.getFloatStack());
        }
        else {
            throw new RuntimeException("Unsupported dimensions: " + aLabelImage.getNumOfDimensions());
        }
        
        if (iIsThatFirstPass) {
            // We have first slices in stack so we can add it to image (it is impossible to add empty stack).
            iIsThatFirstPass = false;
            iImage.setStack(iStack);
            iImage.show();
        }
        
        // Handle new maximum label value and colors of image
//        if (aBiggestLabelSoFar > iMaxLabel) {
//            iMaxLabel = 2 * aBiggestLabelSoFar;
//            IJ.setMinAndMax(iImage, 0, iMaxLabel);
//            IJ.run(iImage, "3-3-2 RGB", null);
//        }
    }
    
    /**
     * Adds new LabelImageRC to stack
     * @param aLabelImage - image to add
     * @param aTitle - title for image
     * @param aBiggestLabelSoFar - maximum label value used so far
     */
    public void addSliceToStack(LabelImage aLabelImage, String aTitle, int aBiggestLabelSoFar ) {
        if (iStack == null) {
            // stack was closed by user, don't reopen
            return;
        }
        
        if (aLabelImage.getNumOfDimensions() <= 3) {
            addStack(aTitle, aLabelImage.getShortStack(false, false, false));
        }
        else {
            throw new RuntimeException("Unsupported dimensions: " + aLabelImage.getNumOfDimensions());
        }
        
        if (iIsThatFirstPass) {
            // We have first slices in stack so we can add it to image (it is impossible to add empty stack).
            iIsThatFirstPass = false;
            iImage.setStack(iStack);
            iImage.show();
        }
        
        // Handle new maximum label value and colors of image
        if (aBiggestLabelSoFar > iMaxLabel) {
            iMaxLabel = 2 * aBiggestLabelSoFar;
            IJ.setMinAndMax(iImage, 0, iMaxLabel);
            IJ.run(iImage, "3-3-2 RGB", null);
        }
    }
    
    /**
     * Adds single new stack to existing stack
     */
    private void addStack(String aTitle, ImageStack aStack) {
        if (!iShouldKeepAllSlices) {
            // We don't keep hitory - remove old slices
            while (iStack.getSize() > 0) {
                iStack.deleteLastSlice();
            }
        }
        else {
            // clean the stack - hyperstack must contain "modulo depth" number of slices
            while (iStack.getSize() % aStack.getSize() != 0) {
                iStack.deleteSlice(1);
            }
        }
        

        int lastSlice = iImage.getSlice();
        final int lastFrame = iImage.getFrame();
        int nextFrame = lastFrame;
        final boolean isLastFrameInImage = (lastFrame == iImage.getDimensions()[4 /* frame no */]);
        if (isLastFrameInImage) { // If yes, continue to point to the last frame after adding new stack
            nextFrame++;
        }

        // Finally add new stack
        for (int i = 1; i <= aStack.getSize(); ++i) {
            iStack.addSlice(aTitle, aStack.getProcessor(i));
        }

        final int depth = aStack.getSize();
        final int frame = iStack.getSize() / depth;
        iImage.setDimensions(1, depth, frame);
        
        // convert to hyperstack if necessary (after second frame is added)
        if (iShouldKeepAllSlices && frame == 2) {
            iImage.setOpenAsHyperStack(true);
            new StackWindow(iImage);
        }

        // go to mid in first iteration
        if (iIsThatFirstPass) {
            lastSlice = (depth + 1) / 2;
        }
        
        iImage.setPosition(1 /* channel */, lastSlice, nextFrame);
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
            final Window win = iImage.getWindow();
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
