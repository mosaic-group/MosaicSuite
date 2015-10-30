package mosaic.utils.io.csv;

import java.util.Arrays;

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
public class CsvColumnConfig {
    public CsvColumnConfig(String[] aMap, CellProcessor[] aCellProcessors) {
        fieldMapping = aMap;
        cellProcessors = aCellProcessors;
    }

    public String[] fieldMapping;
    public CellProcessor cellProcessors[];

    @Override
    public String toString() {
        return new String("FieldMapping: " + Arrays.toString(fieldMapping) + " CellProcessors: " + Arrays.toString(cellProcessors));
    }
}
