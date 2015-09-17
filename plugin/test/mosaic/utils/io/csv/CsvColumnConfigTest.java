package mosaic.utils.io.csv;

import static org.junit.Assert.assertArrayEquals;
import mosaic.test.framework.CommonBase;

import org.junit.Test;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class CsvColumnConfigTest extends CommonBase {

    @Test
    public void testGeneral() {
        String[] fieldMapping = new String[] {"column1", "column2"};
        CellProcessor[] cellProcessors = new CellProcessor[] {new ParseInt(), new ParseDouble()};

        CsvColumnConfig ccc = new CsvColumnConfig(fieldMapping, cellProcessors);

        assertArrayEquals(fieldMapping, ccc.fieldMapping);
        assertArrayEquals(cellProcessors, ccc.cellProcessors);
    }

}
