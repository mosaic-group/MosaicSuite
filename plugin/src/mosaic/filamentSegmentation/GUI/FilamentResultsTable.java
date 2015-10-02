package mosaic.filamentSegmentation.GUI;


import java.util.List;
import java.util.Map;

import ij.measure.ResultsTable;
import mosaic.filamentSegmentation.SegmentationFunctions;
import mosaic.utils.math.CubicSmoothingSpline;


/**
 * Generates results table with data of all filaments (like frame number, length...)
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FilamentResultsTable {
    
    private final String iTitle;
    private ResultsTable rs = null;
    
    /**
     * @param aTitle A title for result table
     * @param iFilamentsData input data {@link mosaic.plugins.FilamentSegmentation#iFilamentsData check here for details of aFilamentsData structure}
     */
    public FilamentResultsTable (String aTitle, final Map<Integer, Map<Integer, List<CubicSmoothingSpline>>> iFilamentsData) {
        iTitle = aTitle;
        generateResultsTableWithAllFilaments(iFilamentsData);
    }
    
    /**
     * Shows generated table. 
     */
    public void show() {
        if (rs != null) rs.show(iTitle);
    }
    
    private void generateResultsTableWithAllFilaments(final Map<Integer, Map<Integer, List<CubicSmoothingSpline>>> iFilamentsData) {
        // Create result table with all filaments
        rs = new ResultsTable();
        for (final Integer frame : iFilamentsData.keySet()) {
            final Map<Integer, List<CubicSmoothingSpline>> ms = iFilamentsData.get(frame);
            for (final List<CubicSmoothingSpline> ps : ms.values()) {
                int count = 1;
                for (final CubicSmoothingSpline css : ps) {
                    rs.incrementCounter();
                    rs.addValue("Frame", frame);
                    rs.addValue("Filament no", count);
                    rs.addValue("Lenght", SegmentationFunctions.calcualteFilamentLenght(css));
                    
                    // Find and adjust coordinates from 1..n range (used to be compatibilt wiht matlab code) 
                    // to 0..n-1 as used for images in fiji. 
                    // Additionally for x should point to middle of a pixel (currently segmentation 
                    // can found only integer values on x axis).
                    double xBegin = css.getKnot(0);
                    double xEnd = css.getKnot(css.getNumberOfKNots() - 1);
                    double yBegin = css.getValue(xBegin);
                    double yEnd = css.getValue(xEnd);
                    rs.addValue("begin x", xBegin - 1 + 0.5);
                    rs.addValue("begin y", yBegin - 1);
                    rs.addValue("end x", xEnd - 1 + 0.5);
                    rs.addValue("end y", yEnd - 1);
                    count++;
                }
            }
        }
    }
    
}
