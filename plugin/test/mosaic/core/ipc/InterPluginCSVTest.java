package mosaic.core.ipc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mosaic.test.framework.CommonBase;
import mosaic.test.framework.SystemOperations;

import org.junit.Test;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class InterPluginCSVTest extends CommonBase {
    
    /**
     * TestThing is a helper class used for testing CSV
     */
    @SuppressWarnings("unused") // get/set methods are accessed via reflection
    static public class TestThing {
        // Definitions for CSV input/output
        public static final String[] Thing_map = new String[] 
        { 
            "ID",
            "CalculatedValue"
        };
        public static final CellProcessor[] Thing_CellProcessor = new CellProcessor[] 
        {
            new ParseInt(),
            new ParseDouble()
        };
        
        
        // Getters/Setters for input/output CSV
        public int getID() {return id;}
        public void setID(int aId) {id = aId;}

        public double getCalculatedValue() {return value;}
        public void setCalculatedValue(double aValue) {value = aValue;}

        // ---------------------------------------
        private int id;
        private double value;
        
        public TestThing(int aId, double aValue) {id = aId; value = aValue;}
        public TestThing() {}
                
        @Override 
        public String toString() { 
            String str = "TestThing {ID: " + id + ", CalculatedValue: " + value + "}";
            return str;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (obj == null || obj.getClass() != this.getClass())
                return false;
            TestThing cmp = (TestThing) obj;
            if (cmp.id != this.id || cmp.value != this.value)
                return false;

            return true;
        }
    }
    
    // --------------------- Helper methods -----------------------------------------------
    static String readFile(String aFullPathFile) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(aFullPathFile));
            return new String(encoded, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Reading [" + aFullPathFile + "] file failed.");
        }
        return null;
    }
    
    static void saveFile(String aFullPathFile, String aContent) {
        try {
            PrintWriter out = new PrintWriter(aFullPathFile);
            out.print(aContent);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("Writing [" + aFullPathFile + "] file failed.");
        }
    }
    
    // --------------------- Test methods -----------------------------------------------
    
    @Test
    public void testWrite() {
        String testDir = SystemOperations.getTestTmpPath();
        
        String fullFileName = testDir + "testWrite.csv";
        List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 33.3));
        data.add(new TestThing(4, 99.1));
        
        InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
        OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
        csv.Write(fullFileName, data , oc , false);
        
        String content = readFile(fullFileName);
        String expectedContent = "ID,CalculatedValue\n" +
                                 "1,33.3\n" +
                                 "4,99.1\n";
        assertEquals(expectedContent, content);
    }
    
    @Test
    public void testRead() {
        String testDir = SystemOperations.getTestTmpPath();
        
        String fullFileName = testDir + "testRead.csv";
        
        String expectedContent = "ID,CalculatedValue\n" +
                                 "1,33.3\n" +
                                 "4,99.1\n";
        saveFile(fullFileName, expectedContent);
        
        List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(1, 33.3));
        expectedData.add(new TestThing(4, 99.1));
        
        InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
        OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
        List<TestThing> outdst = csv.Read(fullFileName, oc);

        assertEquals(expectedData, outdst);
    }
    
    @Test
    public void testWriteMetaInformation() {
        String testDir = SystemOperations.getTestTmpPath();
        
        String fullFileName = testDir + "testWriteMetaInformation.csv";
        List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 5.2));
        
        InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
        OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
        csv.setMetaInformation("name1", "valueForName1");
        csv.setMetaInformation("name2", "valueForName2");
        csv.Write(fullFileName, data , oc , false);
        
        String content = readFile(fullFileName);
        String expectedContent = "ID,CalculatedValue\n" +
                                 "%name1:valueForName1\n" +
                                 "%name2:valueForName2\n" +
                                 "1,5.2\n";;
        assertEquals(expectedContent, content);
    }
    
    @Test
    public void testReadMetaInformation() {
        String testDir = SystemOperations.getTestTmpPath();
        
        String fullFileName = testDir + "testReadMetaInformation.csv";
        
        String expectedContent = "ID,CalculatedValue\n" +
                                 "%name1:valueForName1\n" +
                                 "%name2:valueForName2\n" +
                                 "4,5.2\n";;
        saveFile(fullFileName, expectedContent);
        
        List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(4, 5.2));
        
        InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
        OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
        List<TestThing> outdst = csv.Read(fullFileName, oc);

        assertEquals(expectedData, outdst);
        assertEquals("valueForName1", csv.getMetaInformation("name1"));
        assertEquals("valueForName2", csv.getMetaInformation("name2"));
    }
    
    @Test
    public void testWriteAppend() {
        String testDir = SystemOperations.getTestTmpPath();
        
        String fullFileName = testDir + "testWriteAppend.csv";
        
        {
            List<TestThing> data = new ArrayList<TestThing>();
            data.add(new TestThing(1, 33.3));
            data.add(new TestThing(4, 99.1));

            InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
            OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
            csv.setMetaInformation("anyName", "anyValue");
            csv.Write(fullFileName, data , oc , false);
            
            String content = readFile(fullFileName);
            String expectedContent = "ID,CalculatedValue\n" +
                                     "%anyName:anyValue\n" +
                                     "1,33.3\n" +
                                     "4,99.1\n";
            
            assertEquals(expectedContent, content);
        }
        {
            InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
            OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
            csv.Write(fullFileName, Arrays.asList(new TestThing(5, 1.23)) , oc , true);
            
            String content = readFile(fullFileName);
            String expectedContent = "ID,CalculatedValue\n" +
                                     "%anyName:anyValue\n" +
                                     "1,33.3\n" +
                                     "4,99.1\n" +
                                     "5,1.23\n";
            
            assertEquals(expectedContent, content);
        }
    }
    
    @Test
    public void testStitch() {
        String testDir = SystemOperations.getTestTmpPath();
        
        String fullFileName1 = testDir + "testStitch1.csv";
        String fullFileName2 = testDir + "testStitch2.csv";

        {
            InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
            OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
            csv.setMetaInformation("name1", "valueForName1");
            csv.Write(fullFileName1, Arrays.asList(new TestThing(5, 1.23)) , oc , false);
        }
        {
            InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
            OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
            csv.setMetaInformation("name2", "valueForName2");
            csv.Write(fullFileName2, Arrays.asList(new TestThing(3, 3.14)) , oc , false);
        }
        
        String fullFileName = testDir + "testStitchEd.csv";
        InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
        OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
        csv.setMetaInformation("name3", "valueForName3");
        assertTrue(csv.Stitch(new String[] {fullFileName1, fullFileName2}, fullFileName , oc));
        
        String content = readFile(fullFileName);
        String expectedContent = "ID,CalculatedValue\n" +
                                 "%name3:valueForName3\n" +
                                 "%name1:valueForName1\n" +
                                 "%name2:valueForName2\n" +
                                 "5,1.23\n" +
                                 "3,3.14\n";
        assertEquals(expectedContent, content);
    }
    
    @Test
    public void testWriteChangedDelimieter() {
        String testDir = SystemOperations.getTestTmpPath();
        
        String fullFileName = testDir + "testWriteChangedDelimieter.csv";
        List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 33.3));
        data.add(new TestThing(4, 99.1));
        
        InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
        csv.setDelimiter(';');
        OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
        csv.Write(fullFileName, data , oc , false);
        
        String content = readFile(fullFileName);
        String expectedContent = "ID;CalculatedValue\n" +
                                 "1;33.3\n" +
                                 "4;99.1\n";
        assertEquals(expectedContent, content);
    }
    
    @Test
    public void testReadChangedDelimieter() {
        String testDir = SystemOperations.getTestTmpPath();
        
        String fullFileName = testDir + "testReadChangedDelimieter.csv";
        
        String expectedContent = "ID;CalculatedValue\n" +
                                 "1;33.3\n" +
                                 "4;99.1\n";
        saveFile(fullFileName, expectedContent);
        
        List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(1, 33.3));
        expectedData.add(new TestThing(4, 99.1));
        
        InterPluginCSV<TestThing> csv = new InterPluginCSV<TestThing>(TestThing.class);
        OutputChoose oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
        csv.setCSVPreferenceFromFile(fullFileName);
        List<TestThing> outdst = csv.Read(fullFileName, oc);

        assertEquals(expectedData, outdst);
    }
}
