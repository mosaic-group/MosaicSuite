package mosaic.io.csv;

import static org.junit.Assert.*;

import org.junit.Test;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class CsvColumnConfigTest {

    @Test
    public void test() {
        String[] fieldMapping = new String[] {"column1", "column2"};
        CellProcessor[] cellProcessors = new CellProcessor[] {new ParseInt(), new ParseDouble()};
        
        CsvColumnConfig ccc = new CsvColumnConfig(fieldMapping, cellProcessors);
        
        assertArrayEquals(fieldMapping, ccc.fieldMapping);
        assertArrayEquals(cellProcessors, ccc.cellProcessors);
    }

}
