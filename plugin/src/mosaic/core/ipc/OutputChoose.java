package mosaic.core.ipc;

import org.supercsv.cellprocessor.ift.CellProcessor;

/**
 * This class describe how to process each column, is an array for each colum
 * that describe the column type. Map is the name of each column, and the header
 * output
 * 
 * Example:
 * map[] = {"col1","col2","col3"} cel[] = {ParseDouble(), ParseInteger(), ParseFloat()}
 * 
 * @author Pietro Incardona
 */
public class OutputChoose {
    public OutputChoose(String[] aMap, CellProcessor[] aCellProcessors) {
        map = aMap;
        cel = aCellProcessors;
    }

    public String[] map;
    public CellProcessor cel[];
};
