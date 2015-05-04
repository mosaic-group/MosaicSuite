package mosaic.plugins;

import ij.gui.GenericDialog;
import mosaic.plugins.utils.PlugInFloatBase;
import mosaic.variationalCurvatureFilters.CurvatureFilter;
import mosaic.variationalCurvatureFilters.FilterKernel;
import mosaic.variationalCurvatureFilters.FilterKernelGc;
import mosaic.variationalCurvatureFilters.FilterKernelMc;
import mosaic.variationalCurvatureFilters.FilterKernelTv;
import mosaic.variationalCurvatureFilters.NoSplitFilter;
import mosaic.variationalCurvatureFilters.SplitFilter;

/**
 * Base for plugIns working with Curvature Filters. Mainly it shows dialog to user, gets information
 * about wanted filter/iterations and creates filter which can be used in plugIn.
 * @author Krzysztof Gonciarz
 *
 */
public abstract class CurvatureFilterBase extends PlugInFloatBase {

    // Chosen filter
    private CurvatureFilter iCf;

    // Number of iterations to run filter
    private int iNumberOfIterations;

    // flag telling if split/no split choice should be allowed for user
    private boolean hasSplitMethodMenu = false;
    
    /**
     * Allows to show/disable part of dialog with type of filter (split/no split)
     * @param aSplitMethodMenuAvailable If true user can chose split/no split version
     *                                  of filter (false by default)
     */
    void setSplitMethodMenu(boolean aSplitMethodMenuAvailable) {
        hasSplitMethodMenu = aSplitMethodMenuAvailable;
    }
    
    /**
     * @return Filter created basing on user input.
     */
    protected CurvatureFilter getCurvatureFilter() {
        return iCf;
    }

    /**
     * @return Number of iterations provided by user.
     */
    protected int getNumberOfIterations() {
        return iNumberOfIterations;
    }
    
    /**
     * Takes information from user about wanted filterType, filtering method and
     * number of iterations.
     * @return true in case if configuration was successful - false otherwise.
     */
    @Override
    protected boolean showDialog() {
        final String[] filters = {"GC", "MC", "TV"};
        final String[] types = {"Split", "No split"};
        
        GenericDialog gd = new GenericDialog("Curvature Filter Settings");
    
        gd.addRadioButtonGroup("Filter type: ", filters, 1, 3, filters[0]);
        if (hasSplitMethodMenu) gd.addRadioButtonGroup("Method: ", types, 1, 2, types[1]);
        gd.addNumericField("Number of iterations: ", 10, 0);
        
        gd.showDialog();
        
        if (!gd.wasCanceled()) {
            // Create user's chosen filter
            String filter = gd.getNextRadioButton();
            String type;
            if (hasSplitMethodMenu) type = gd.getNextRadioButton();
            else type = types[1];
            
            iNumberOfIterations = (int)gd.getNextNumber();
            FilterKernel fk = null;
            if (filter.equals(filters[0])) {
                fk = new FilterKernelGc(); 
            } 
            else if (filter.equals(filters[1])) {
                fk = new FilterKernelMc(); 
            }
            else if (filter.equals(filters[2])) {
                fk = new FilterKernelTv(); 
            }
            if (fk == null) return false;
            
            if (type.equals(types[0])) {
                iCf = new SplitFilter(fk); 
            }
            else {
                iCf = new NoSplitFilter(fk); 
            }
            
            if (iCf != null && iNumberOfIterations >= 0) return true;
        }
        
        return false;
    }

}