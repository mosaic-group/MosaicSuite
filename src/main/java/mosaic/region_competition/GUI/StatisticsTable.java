package mosaic.region_competition.GUI;

import java.io.IOException;
import java.util.Collection;

import ij.measure.ResultsTable;
import mosaic.region_competition.utils.LabelStatistics;

/**
 * StatisticsTable class for handling output of LabelInformation
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class StatisticsTable {

    private final int iPadSize;
    private final ResultsTable iResultsTable;
    
    /**
     * @param aLabelInfos container with all LabelInformation objects to be shown or/and save in file
     * @param aPadSize pad size used during segmentation - objects coordinates should be corrected with this value
     */
    public StatisticsTable(Collection<LabelStatistics> aLabelInfos, int aPadSize) {
        iPadSize = aPadSize;
        iResultsTable = createStatistics(aLabelInfos);
    }
    
    /**
     * Shows statistics as a ResutlsTable.
     */
    public void show(String aNameOfTable) { 
        iResultsTable.show(aNameOfTable);
    }
    
    /**
     * Save statistics
     * @param aFileName file name of output csv file
     */
    public void save(String aFileName) {
        try {
            iResultsTable.saveAs(aFileName);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates ResultsTable and fills it with LabelInformation data
     * @param collection LabelInformation data
     * @return created table with data
     */
    private ResultsTable createStatistics(Collection<LabelStatistics> collection) {
        final ResultsTable rt = new ResultsTable();
        rt.showRowNumbers(false);

        for (final LabelStatistics info : collection) {
            rt.incrementCounter();
            rt.addValue("Id", rt.getCounter());
            rt.addValue("Image_ID", 0);
            rt.addValue("label", info.iLabel);
            rt.addValue("size", info.iLabelCount);
            rt.addValue("mean", info.iMeanIntensity);
            rt.addValue("variance", info.iVarIntensity);
            rt.addValue("Coord_X", info.iMeanPosition[0] - iPadSize);
            rt.addValue("Coord_Y", info.iMeanPosition[1] - iPadSize);
            rt.addValue("Coord_Z", (info.iMeanPosition.length > 2) ? (info.iMeanPosition[2] - iPadSize) : 0.0);
        }

        return rt;
    }
}
