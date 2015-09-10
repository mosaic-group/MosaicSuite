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

import org.junit.Before;
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
    
    /**
     * TestThing is a helper class used for testing CSV
     */
    @SuppressWarnings("unused") // get/set methods are accessed via reflection
    static public class TestSmall {
        // Definitions for CSV input/output
        public static final String[] Small_map = new String[] 
        { 
            "ID"
        };
        public static final CellProcessor[] Small_CellProcessor = new CellProcessor[] 
        {
            new ParseInt()
        };
        
        
        // Getters/Setters for input/output CSV
        public int getID() {return id;}
        public void setID(int aId) {id = aId;}

        // ---------------------------------------
        private int id;
        
        public TestSmall(int aId) {id = aId;}
        public TestSmall() {}
        
        @Override 
        public String toString() { 
            String str = "TestSmall {ID: " + id + "}";
            return str;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (obj == null || obj.getClass() != this.getClass())
                return false;
            TestSmall cmp = (TestSmall) obj;
            if (cmp.id != this.id)
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
    
    static String fullFileName(String aFileName) {
        String testDir = SystemOperations.getTestTmpPath();
        return testDir + aFileName;
    }
    
    static void verifyFileContent(String aFileName, String aExpectedContent) {
        String content = readFile(aFileName);
        assertEquals(aExpectedContent, content);
    }
    
    // These guys are updated before each test to fresh state 
    InterPluginCSV<TestThing> csv;
    OutputChoose oc;
    
    @Before 
    public void initialize() {
        csv = new InterPluginCSV<TestThing>(TestThing.class);
        oc = new OutputChoose( TestThing.Thing_map, TestThing.Thing_CellProcessor);
     }
    
    // --------------------- Test methods -----------------------------------------------
    
    @Test
    public void testWrite() {
        String fullFileName = fullFileName("testWrite.csv");
        
        // Data to be written
        List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 33.3));
        data.add(new TestThing(4, 99.1));
        
        // Tested method
        csv.Write(fullFileName, data , oc , false);
        
        // Verify
        String expectedContent = "ID,CalculatedValue\n" +
                                 "1,33.3\n" +
                                 "4,99.1\n";
        verifyFileContent(fullFileName, expectedContent);
    }
    
    @Test
    public void testRead() {
        String fullFileName = fullFileName("testRead.csv");
        
        // Prepare data
        String expectedContent = "ID,CalculatedValue\n" +
                                 "1,33.3\n" +
                                 "4,99.1\n";
        saveFile(fullFileName, expectedContent);
        
        // Tested method
        List<TestThing> outdst = csv.Read(fullFileName, oc);

        // Verify
        List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(1, 33.3));
        expectedData.add(new TestThing(4, 99.1));        
        assertEquals(expectedData, outdst);
    }
    
    @Test
    public void testWriteMetaInformation() {
        String fullFileName = fullFileName("testWriteMetaInformation.csv");
        
        // Generate data
        List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 5.2));
        csv.setMetaInformation("name1", "valueForName1");
        csv.setMetaInformation("name2", "valueForName2");
        
        // Tested method
        csv.Write(fullFileName, data , oc , false);
   
        // Verify
        String expectedContent = "ID,CalculatedValue\n" +
                                 "%name1:valueForName1\n" +
                                 "%name2:valueForName2\n" +
                                 "1,5.2\n";
        verifyFileContent(fullFileName, expectedContent);
    }
    
    @Test
    public void testReadMetaInformation() {
        String fullFileName = fullFileName("testReadMetaInformation.csv");

        // Prepare data
        String expectedContent = "ID,CalculatedValue\n" +
                                 "%name1:valueForName1\n" +
                                 "%name2:valueForName2\n" +
                                 "4,5.2\n";;
        saveFile(fullFileName, expectedContent);
        
        // Tested method
        List<TestThing> outdst = csv.Read(fullFileName, oc);

        // Verify
        List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(4, 5.2));
        assertEquals(expectedData, outdst);
        assertEquals("valueForName1", csv.getMetaInformation("name1"));
        assertEquals("valueForName2", csv.getMetaInformation("name2"));
    }
    
    @Test
    public void testWriteAppend() {
        String fullFileName = fullFileName("testWriteAppend.csv");
        
        {  // Save first version of file
            
            // Generate data
            List<TestThing> data = new ArrayList<TestThing>();
            data.add(new TestThing(1, 33.3));
            data.add(new TestThing(4, 99.1));
            csv.setMetaInformation("anyName", "anyValue");

            // Tested method
            csv.Write(fullFileName, data , oc , false);
            
            // Verify
            String expectedContent = "ID,CalculatedValue\n" +
                                     "%anyName:anyValue\n" +
                                     "1,33.3\n" +
                                     "4,99.1\n";
            verifyFileContent(fullFileName, expectedContent);
        }
        {  // Append additional data to that file
            
            // Generate new InterPluginCSV just to refresh state
            csv = new InterPluginCSV<TestThing>(TestThing.class);
            
            // Tested method
            csv.Write(fullFileName, Arrays.asList(new TestThing(5, 1.23)) , oc , true);
            
            // Verify
            String expectedContent = "ID,CalculatedValue\n" +
                                     "%anyName:anyValue\n" +
                                     "1,33.3\n" +
                                     "4,99.1\n" +
                                     "5,1.23\n";
            verifyFileContent(fullFileName, expectedContent);
        }
    }
    
    @Test
    public void testStitch() {
        String fullFileName1 = fullFileName("testStitch1.csv");
        String fullFileName2 = fullFileName("testStitch2.csv");

        {   // Generate first file
            
            csv.setMetaInformation("name1", "valueForName1");
            csv.Write(fullFileName1, Arrays.asList(new TestThing(5, 1.23)) , oc , false);
        }
        {   // Generate second file
            
            // Generate new InterPluginCSV just to refresh state
            csv = new InterPluginCSV<TestThing>(TestThing.class);
            csv.setMetaInformation("name2", "valueForName2");
            csv.Write(fullFileName2, Arrays.asList(new TestThing(3, 3.14)) , oc , false);
        }
        
        // Add additional meta information before joining files together
        String fullFileName = fullFileName("testStitchEd.csv");
        // Generate new InterPluginCSV just to refresh state
        csv = new InterPluginCSV<TestThing>(TestThing.class);
        csv.setMetaInformation("name3", "valueForName3");
        
        // Tested method
        assertTrue(csv.Stitch(new String[] {fullFileName1, fullFileName2}, fullFileName , oc));
        
        // Verify
        String expectedContent = "ID,CalculatedValue\n" +
                                 "%name3:valueForName3\n" +
                                 "%name1:valueForName1\n" +
                                 "%name2:valueForName2\n" +
                                 "5,1.23\n" +
                                 "3,3.14\n";
        verifyFileContent(fullFileName, expectedContent);
    }
    
    @Test
    public void testWriteChangedDelimieter() {
        String fullFileName = fullFileName("testWriteChangedDelimieter.csv");
        
        // Prepare data
        List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 33.3));
        data.add(new TestThing(4, 99.1));
        
        // Tested methods
        csv.setDelimiter(';');
        csv.Write(fullFileName, data , oc , false);
        
        // Verify
        String expectedContent = "ID;CalculatedValue\n" +
                                 "1;33.3\n" +
                                 "4;99.1\n";
        verifyFileContent(fullFileName, expectedContent);
    }
    
    @Test
    public void testReadChangedDelimieter() {
        String fullFileName = fullFileName("testReadChangedDelimieter.csv");
        
        // Prepare data
        String expectedContent = "ID;CalculatedValue\n" +
                                 "1;33.3\n" +
                                 "4;99.1\n";
        saveFile(fullFileName, expectedContent);
        
        // Tested methods
        csv.setCSVPreferenceFromFile(fullFileName);
        List<TestThing> outdst = csv.Read(fullFileName, oc);

        // Verify
        List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(1, 33.3));
        expectedData.add(new TestThing(4, 99.1));
        assertEquals(expectedData, outdst);
    }
    
    @Test
    public void testReadDefault() {
        String fullFileName = fullFileName("testReadDefault.csv");
        
        // Prepare data
        String expectedContent = "ID,CalculatedValue\n" +
                                 "1,33.3\n" +
                                 "4,99.1\n";
        saveFile(fullFileName, expectedContent);
        
        // Tested method (do not specify columns with OutputChoose)
        List<TestThing> outdst = csv.Read(fullFileName);

        // Verify
        List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(1, 33.3));
        expectedData.add(new TestThing(4, 99.1));        
        assertEquals(expectedData, outdst);
    }
    
    @Test
    public void testReadDefaultWithMissingColumn() {
        // Class TestSmall has defined less getters/setters than CSV file
        // In such case additional columns should be ignored and only fields defined
        // in given class should be set/loaded.
        
        String fullFileName = fullFileName("testReadDefaultWithMissingColumn.csv");
        
        // Prepare data
        String expectedContent = "ID,CalculatedValue\n" +
                                 "1,33.3\n" +
                                 "4,99.1\n";
        saveFile(fullFileName, expectedContent);
        
        InterPluginCSV<TestSmall> csv =  new InterPluginCSV<TestSmall>(TestSmall.class);
        // Tested method (do not specify columns with OutputChoose)
        List<TestSmall> outdst = csv.Read(fullFileName);

        // Verify
        List<TestSmall> expectedData = new ArrayList<TestSmall>();
        expectedData.add(new TestSmall(1));
        expectedData.add(new TestSmall(4));        
        assertEquals(expectedData, outdst);
    }
}
