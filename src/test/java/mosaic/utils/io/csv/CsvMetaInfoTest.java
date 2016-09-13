package mosaic.utils.io.csv;

import static org.junit.Assert.assertEquals;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class CsvMetaInfoTest extends CommonBase {

    @Test
    public void testGeneral() {
        final String parameter = "param1";
        final String value = "value1";

        final CsvMetaInfo cmi = new CsvMetaInfo(parameter, value);

        assertEquals(parameter, cmi.parameter);
        assertEquals(value, cmi.value);
        assertEquals("{[param1][value1]}", cmi.toString());
    }

}
