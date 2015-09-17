package mosaic.utils.io.csv;

import static org.junit.Assert.*;
import mosaic.test.framework.CommonBase;
import mosaic.utils.io.csv.CsvMetaInfo;

import org.junit.Test;

public class CsvMetaInfoTest extends CommonBase {

    @Test
    public void testGeneral() {
        String parameter = "param1";
        String value = "value1";
        
        CsvMetaInfo cmi = new CsvMetaInfo(parameter, value);
        
        assertEquals(parameter, cmi.parameter);
        assertEquals(value, cmi.value);
        assertEquals("{[param1][value1]}", cmi.toString());
    }

}
