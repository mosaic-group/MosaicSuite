package mosaic.region_competition.GUI;

import java.io.IOException;
import java.util.Collection;

import ij.measure.ResultsTable;
import mosaic.region_competition.LabelInformation;

/**
 * StatisticsTable class for handling output of LabelInformation
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class StatisticsTable {

    private final ResultsTable iResultsTable;
    
    /**
     * @param aLabelInfos container with all LabelInformation objects to be shown or/and save in file
     */
    public StatisticsTable(Collection<LabelInformation> aLabelInfos) {
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
    private ResultsTable createStatistics(Collection<LabelInformation> collection) {
        final ResultsTable rt = new ResultsTable();
        rt.showRowNumbers(false);

        int rowNumber = 1;
        for (final LabelInformation info : collection) {
            rt.incrementCounter();
            rt.addValue("Id", rowNumber++);
            rt.addValue("Image_ID", 0);
            rt.addValue("label", info.label);
            rt.addValue("size", info.count);
            rt.addValue("mean", info.mean);
            rt.addValue("variance", info.var);
            rt.addValue("Coord_X", info.mean_pos[0]);
            rt.addValue("Coord_Y", info.mean_pos[1]);
            if (info.mean_pos.length > 2) {
                rt.addValue("Coord_Z", info.mean_pos[2]);
            }
            else {
                rt.addValue("Coord_Z", 0.0);
            }
        }

        return rt;
    }
}
