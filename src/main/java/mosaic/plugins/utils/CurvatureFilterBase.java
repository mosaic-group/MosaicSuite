package mosaic.plugins.utils;

import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextArea;

import ij.gui.GenericDialog;
import mosaic.variationalCurvatureFilters.CurvatureFilter;
import mosaic.variationalCurvatureFilters.FilterKernel;
import mosaic.variationalCurvatureFilters.FilterKernelBernstein;
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
    protected void setSplitMethodMenu(boolean aSplitMethodMenuAvailable) {
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
        final String[] filters = {"GC (Gaussian Curvature)", "MC (Mean Curvature)", "TV (Total Variation)", "Bernstein"};
        final String[] types = {"Split", "No split"};

        final GenericDialog gd = new GenericDialog("Curvature Filter Settings");

        gd.addRadioButtonGroup("Filter type: ", filters, 4, 1, filters[0]);
        if (hasSplitMethodMenu) {
            gd.addRadioButtonGroup("Method: ", types, 1, 2, types[1]);
        }
        gd.addNumericField("Number of iterations: ", 10, 0);

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
        final TextArea ta = new TextArea(referenceInfo, 8, 50, TextArea.SCROLLBARS_NONE);
        ta.setBackground(SystemColor.control);
        ta.setEditable(false);
        ta.setFocusable(true);
        panel.add(ta);
        gd.addPanel(panel);
        
        /*
         * @phdthesis{gong:phd, 
  title={Spectrally regularized surfaces}, 
  author={Gong, Yuanhao}, 
  year={2015}, 
  school={ETH Zurich, Nr. 22616},
  note={http://dx.doi.org/10.3929/ethz-a-010438292}}
         */
        gd.showDialog();

        if (!gd.wasCanceled()) {
            // Create user's chosen filter
            final String filter = gd.getNextRadioButton();
            String type;
            if (hasSplitMethodMenu) {
                type = gd.getNextRadioButton();
            }
            else {
                type = types[1];
            }

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
            else if (filter.equals(filters[3])) {
                fk = new FilterKernelBernstein();
            }
            if (fk == null) {
                return false;
            }

            if (type.equals(types[0])) {
                iCf = new SplitFilter(fk);
            }
            else {
                iCf = new NoSplitFilter(fk);
            }

            if (iCf != null && iNumberOfIterations >= 0) {
                return true;
            }
        }

        return false;
    }

}