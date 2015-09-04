package temporary;

import java.util.Vector;

import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.ipc.OutputChoose;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

public class CsvTest extends CommonBase {
    
    @Test
    public void testCsv() {
        InterPluginCSV<Thing> csv = new InterPluginCSV<Thing>(Thing.class);
        Vector<Thing> pt = new Vector<Thing>();
        pt.add(new Thing(1, 3.14));
        pt.add(new Thing(2, 2.28));
        OutputChoose oc = new OutputChoose();
        oc.map = Thing.Thing_map;
        oc.cel = Thing.Thing_CellProcessor;
        csv.Write("/tmp/xxx.csv", pt , oc , false);
    }
}
