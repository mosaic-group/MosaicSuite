package mosaic.plugins;

import ij.Prefs;
import ij.gui.GenericDialog;
import ij.process.FloatProcessor;

import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextArea;

import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloat3DBase;
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
        int originalWidth = aOrigImg[0].getWidth();
        int originalHeight = aOrigImg[0].getHeight();
        int noOfSlices = aOrigImg.length;
        float[][][] img = new float[noOfSlices][originalHeight][originalWidth]; 

        // Get all data from input images/slices for further processing
        for (int slice = 0; slice < noOfSlices; ++slice) {
            ImgUtils.ImgToYX2Darray(aOrigImg[slice], img[slice], 1.0f);
        }
    
        // Run filter on image data.
        CurvatureFilter3D filter = new NoSplitFilter3D(new FilterKernelGc3D());
        filter.runFilter(img, iNumberOfIterations);
        
        // Generate output image from processed data
        for (int slice = 0; slice < noOfSlices; ++slice) {                
            ImgUtils.YX2DarrayToImg(img[slice], aOutputImg[slice], 1.0f);
        }
    }

    @Override
    protected boolean showDialog() {
        // Create GUI for entering filtering parameters
        GenericDialog gd = new GenericDialog("Curvature Filter 3D");

        gd.addNumericField("Number_of_iterations: ", (int)Prefs.get(PropNoOfIterations, 100), 0);

        gd.addMessage("\n");
        final String info = "Y. Gong and I. F. Sbalzarini\n\n\"Image enhancement by gradient distribution specification.\"\nIn Proc. ACCV, 12th Asian Conference on Computer Vision,\nWorkshop on Emerging Topics in Image Enhancement and Restoration,\npages w7–p3, Singapore, November 2014.";
        Panel panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        TextArea ta = new TextArea(info, 7,  57, TextArea.SCROLLBARS_NONE); 
        ta.setBackground(SystemColor.control);
        ta.setEditable(false);
        ta.setFocusable(true);
        panel.add(ta);
        gd.addPanel(panel);
        
        // Show and check if user want to continue
        gd.showDialog();
        if (gd.wasCanceled()) return false;
        
        // Read data from all fields and remember it in preferences
        int iterations = (int)gd.getNextNumber();
        
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