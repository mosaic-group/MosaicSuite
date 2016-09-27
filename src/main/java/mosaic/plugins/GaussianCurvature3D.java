package mosaic.plugins;

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.process.FloatProcessor;

import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextArea;

import mosaic.plugins.utils.PlugInFloat3DBase;
import mosaic.utils.ImgUtils;
import mosaic.variationalCurvatureFilters.CurvatureFilter3D;
import mosaic.variationalCurvatureFilters.FilterKernelGc3D;
import mosaic.variationalCurvatureFilters.NoSplitFilter3D;

/**
 * Implementation of GC for 3D images.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class GaussianCurvature3D extends PlugInFloat3DBase {
    private int iNumberOfIterations;

    // Properties names for saving data from GUI
    private final String PropNoOfIterations  = "GaussianCuvature3D.noOfIterations";

    @Override
    protected void processImg(FloatProcessor[] aOutputImg, FloatProcessor[] aOrigImg, int aChannelNumber) {
        // Get dimensions of input image and create container for it
        final int originalWidth = aOrigImg[0].getWidth();
        final int originalHeight = aOrigImg[0].getHeight();
        final int noOfSlices = aOrigImg.length;
        final float[][][] img = new float[noOfSlices][originalHeight][originalWidth];

        // Get all data from input images/slices for further processing
        for (int slice = 0; slice < noOfSlices; ++slice) {
            ImgUtils.ImgToYX2Darray(aOrigImg[slice], img[slice], 1.0f);
        }

        // Run filter on image data.
        final CurvatureFilter3D filter = new NoSplitFilter3D(new FilterKernelGc3D());
        for (int iteration = 0; iteration < iNumberOfIterations; ++iteration) {
            IJ.showProgress((double)iteration/iNumberOfIterations);
            IJ.showStatus("Running iteration: " + (iteration + 1) + "/" + iNumberOfIterations);
            filter.runFilter(img, 1);
        }

        // Generate output image from processed data
        for (int slice = 0; slice < noOfSlices; ++slice) {
            ImgUtils.YX2DarrayToImg(img[slice], aOutputImg[slice], 1.0f);
        }
    }

    @Override
    protected boolean showDialog() {
        // Create GUI for entering filtering parameters
        final GenericDialog gd = new GenericDialog("Curvature Filter 3D");

        gd.addNumericField("Number_of_iterations: ", (int)Prefs.get(PropNoOfIterations, 100), 0);

        gd.addMessage("\n");
        final String referenceInfo = "\n" +
                                     "@phdthesis{gong:phd, \n" + 
                                     "  title={Spectrally regularized surfaces}, \n" + 
                                     "  author={Gong, Yuanhao}, \n" + 
                                     "  year={2015}, \n" + 
                                     "  school={ETH Zurich, Nr. 22616},\n" + 
                                     "  note={http://dx.doi.org/10.3929/ethz-a-010438292}}\n" +
                                     "\n";
        final Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        final TextArea ta = new TextArea(referenceInfo, 8,  50, TextArea.SCROLLBARS_NONE);
        ta.setBackground(SystemColor.control);
        ta.setEditable(false);
        ta.setFocusable(true);
        panel.add(ta);
        gd.addPanel(panel);

        // Show and check if user want to continue
        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }

        // Read data from all fields and remember it in preferences
        final int iterations = (int)gd.getNextNumber();

        Prefs.set(PropNoOfIterations, iterations);

        // Set segmentation parameters for further use
        iNumberOfIterations = iterations;

        return true;
    }

    @Override
    protected boolean setup(String aArgs) {
        // Do not perform additional checks - it is still possible to run that filter on
        // 2D image but of course it will not change anything.

        setFilePrefix("filtered_");

        return true;
    }
}
