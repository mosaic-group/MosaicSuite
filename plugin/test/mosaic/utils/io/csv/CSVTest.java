package mosaic.utils.io.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

import mosaic.test.framework.CommonBase;

public class CSVTest extends CommonBase {

    /**
     * TestThing is a helper class used for testing CSV
     */
    static public class TestThing {
        // Definitions for CSV input/output
        public static final String[] Thing_map = new String[] { "ID", "CalculatedValue" };
        public static final CellProcessor[] Thing_CellProcessor = new CellProcessor[] { new ParseInt(), new ParseDouble() };


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
            final String str = "TestThing {ID: " + id + ", CalculatedValue: " + value + "}";
            return str;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            final TestThing cmp = (TestThing) obj;
            if (cmp.id != this.id || cmp.value != this.value) {
                return false;
            }

            return true;
        }
        
        @Override
        public int hashCode() {
            return (int)(32 * id + value);
        }
    }
    
    /**
     * TestThing is a helper class used for testing CSV
     */
    static public class TestSmall {
        // Definitions for CSV input/output
        public static final String[] Small_map = new String[] {"ID"};
        public static final CellProcessor[] Small_CellProcessor = new CellProcessor[] {new ParseInt()};


        // Getters/Setters for input/output CSV
        public int getID() {return id;}
        public void setID(int aId) {id = aId;}

        // ---------------------------------------
        private int id;

        public TestSmall(int aId) {id = aId;}
        public TestSmall() {}

        @Override
        public String toString() {
            final String str = "TestSmall {ID: " + id + "}";
            return str;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            final TestSmall cmp = (TestSmall) obj;
            if (cmp.id != this.id) {
                return false;
            }

            return true;
        }
        @Override
        public int hashCode() {
            return id;
        }
    }

    // --------------------- Helper methods -----------------------------------------------
    static void saveFile(String aFullPathFile, String aContent) {
        try (PrintWriter out = new PrintWriter(aFullPathFile)) {
            out.print(aContent);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            fail("Writing [" + aFullPathFile + "] file failed.");
        }
    }

    static String fullFileName(String aFileName) {
        final String testDir = getTestTmpPath();
        return testDir + aFileName;
    }

    static void verifyFileContent(String aFileName, String aExpectedContent) {
        final String content = readFile(aFileName);
        assertEquals(aExpectedContent, content);
    }

    // These guys are updated before each test to fresh state
    CSV<TestThing> csv;
    CsvColumnConfig oc;

    @Before
    public void initialize() {
        csv = new CSV<TestThing>(TestThing.class);
        oc = new CsvColumnConfig( TestThing.Thing_map, TestThing.Thing_CellProcessor);
    }

    
    // --------------------- Test methods -----------------------------------------------

    @Test
    public void testWrite() {
        final String fullFileName = fullFileName("testWrite.csv");

        // Data to be written
        final List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 33.3));
        data.add(new TestThing(4, 99.1));

        // Tested method
        csv.Write(fullFileName, data , oc , false);

        // Verify
        final String expectedContent = "ID,CalculatedValue\n" +
                                       "1,33.3\n" +
                                       "4,99.1\n";
        verifyFileContent(fullFileName, expectedContent);
    }

    @Test
    public void testRead() {
        final String fullFileName = fullFileName("testRead.csv");

        // Prepare data
        final String expectedContent = "ID,CalculatedValue\n" +
                                       "1,33.3\n" +
                                       "4,99.1\n";
        saveFile(fullFileName, expectedContent);

        // Tested method
        final List<TestThing> outdst = csv.Read(fullFileName, oc);

        // Verify
        final List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(1, 33.3));
        expectedData.add(new TestThing(4, 99.1));
        assertEquals(expectedData, outdst);
    }

    @Test
    public void testWriteMetaInformation() {
        final String fullFileName = fullFileName("testWriteMetaInformation.csv");

        // Generate data
        final List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 5.2));
        csv.setMetaInformation("name1", "valueForName1");
        csv.setMetaInformation("name2", "valueForName2");

        // Tested method
        csv.Write(fullFileName, data , oc , false);

        // Verify
        final String expectedContent = "ID,CalculatedValue\n" +
                                        "%name1:valueForName1\n" +
                                        "%name2:valueForName2\n" +
                                        "1,5.2\n";
        verifyFileContent(fullFileName, expectedContent);
    }

    @Test
    public void testReadMetaInformation() {
        final String fullFileName = fullFileName("testReadMetaInformation.csv");

        // Prepare data
        final String expectedContent = "ID,CalculatedValue\n" +
                                       "%name1:valueForName1\n" +
                                       "%name2:valueForName2\n" +
                                       "%name3:c:\\Windows\\Path\n" +
                                       "4,5.2\n";
                saveFile(fullFileName, expectedContent);

                // Tested method
                final List<TestThing> outdst = csv.Read(fullFileName, oc);

                // Verify
                final List<TestThing> expectedData = new ArrayList<TestThing>();
                expectedData.add(new TestThing(4, 5.2));
                assertEquals(expectedData, outdst);
                assertEquals("valueForName1", csv.getMetaInformation("name1"));
                assertEquals("valueForName2", csv.getMetaInformation("name2"));
                assertEquals("c:\\Windows\\Path", csv.getMetaInformation("name3"));
    }

    @Test
    public void testWriteAppend() {
        final String fullFileName = fullFileName("testWriteAppend.csv");

        {  // Save first version of file

            // Generate data
            final List<TestThing> data = new ArrayList<TestThing>();
            data.add(new TestThing(1, 33.3));
            data.add(new TestThing(4, 99.1));
            csv.setMetaInformation("anyName", "anyValue");

            // Tested method
            csv.Write(fullFileName, data , oc , false);

            // Verify
            final String expectedContent = "ID,CalculatedValue\n" +
                                            "%anyName:anyValue\n" +
                                            "1,33.3\n" +
                                            "4,99.1\n";
            verifyFileContent(fullFileName, expectedContent);
        }
        {  // Append additional data to that file

            // Generate new CSV just to refresh state
            csv = new CSV<TestThing>(TestThing.class);

            // Tested method
            csv.Write(fullFileName, Arrays.asList(new TestThing(5, 1.23)) , oc , true);

            // Verify
            final String expectedContent = "ID,CalculatedValue\n" +
                                            "%anyName:anyValue\n" +
                                            "1,33.3\n" +
                                            "4,99.1\n" +
                                            "5,1.23\n";
            verifyFileContent(fullFileName, expectedContent);
        }
    }

    @Test
    public void testStitch() {
        final String fullFileName1 = fullFileName("testStitch1.csv");
        final String fullFileName2 = fullFileName("testStitch2.csv");

        {   // Generate first file

            csv.setMetaInformation("name1", "valueForName1");
            csv.Write(fullFileName1, Arrays.asList(new TestThing(5, 1.23)) , oc , false);
        }
        {   // Generate second file

            // Generate new CSV just to refresh state
            csv = new CSV<TestThing>(TestThing.class);
            csv.setMetaInformation("name2", "valueForName2");
            csv.Write(fullFileName2, Arrays.asList(new TestThing(3, 3.14)) , oc , false);
        }

        // Add additional meta information before joining files together
        final String fullFileName = fullFileName("testStitchEd.csv");
        // Generate new CSV just to refresh state
        csv = new CSV<TestThing>(TestThing.class);
        csv.setMetaInformation("name3", "valueForName3");

        // Tested method
        assertTrue(csv.Stitch(new String[] {fullFileName1, fullFileName2}, fullFileName , oc));

        // Verify
        final String expectedContent = "ID,CalculatedValue\n" +
                                        "%name3:valueForName3\n" +
                                        "%name1:valueForName1\n" +
                                        "%name2:valueForName2\n" +
                                        "5,1.23\n" +
                                        "3,3.14\n";
        verifyFileContent(fullFileName, expectedContent);
    }

    @Test
    public void testWriteChangedDelimieter() {
        final String fullFileName = fullFileName("testWriteChangedDelimieter.csv");

        // Prepare data
        final List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 33.3));
        data.add(new TestThing(4, 99.1));

        // Tested methods
        csv.setDelimiter(';');
        csv.Write(fullFileName, data , oc , false);

        // Verify
        final String expectedContent = "ID;CalculatedValue\n" +
                                       "1;33.3\n" +
                                       "4;99.1\n";
        verifyFileContent(fullFileName, expectedContent);
    }

    @Test
    public void testReadChangedDelimieter() {
        final String fullFileName = fullFileName("testReadChangedDelimieter.csv");

        // Prepare data
        final String expectedContent = "ID;CalculatedValue\n" +
                                       "1;33.3\n" +
                                       "4;99.1\n";
        saveFile(fullFileName, expectedContent);

        // Tested methods
        assertEquals(2, csv.setCSVPreferenceFromFile(fullFileName));
        final List<TestThing> outdst = csv.Read(fullFileName, oc);

        // Verify
        final List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(1, 33.3));
        expectedData.add(new TestThing(4, 99.1));
        assertEquals(expectedData, outdst);
    }

    @Test
    public void testReadDefault() {
        final String fullFileName = fullFileName("testReadDefault.csv");

        // Prepare data
        final String expectedContent = "ID,CalculatedValue\n" +
                                       "1,33.3\n" +
                                       "4,99.1\n";
        saveFile(fullFileName, expectedContent);

        // Tested method (do not specify columns with OutputChoose)
        final List<TestThing> outdst = csv.Read(fullFileName);

        // Verify
        final List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(1, 33.3));
        expectedData.add(new TestThing(4, 99.1));
        assertEquals(expectedData, outdst);
    }

    @Test
    public void testReadDefaultWithMissingColumn() {
        // Class TestSmall has defined less getters/setters than CSV file
        // In such case additional columns should be ignored and only fields defined
        // in given class should be set/loaded.

        final String fullFileName = fullFileName("testReadDefaultWithMissingColumn.csv");

        // Prepare data
        final String expectedContent = "ID,CalculatedValue\n" +
                                       "1,33.3\n" +
                                       "4,99.1\n";
        saveFile(fullFileName, expectedContent);

        final CSV<TestSmall> csv =  new CSV<TestSmall>(TestSmall.class);
        // Tested method (do not specify columns with OutputChoose)
        final List<TestSmall> outdst = csv.Read(fullFileName);

        // Verify
        final List<TestSmall> expectedData = new ArrayList<TestSmall>();
        expectedData.add(new TestSmall(1));
        expectedData.add(new TestSmall(4));
        assertEquals(expectedData, outdst);
    }

    @Test
    public void testWriteWithWronglySetupOfColumField() {
        final String fullFileName = fullFileName("testWriteWithWronglySetupOfColumField.csv");

        // Data to be written
        final List<TestThing> data = new ArrayList<TestThing>();
        data.add(new TestThing(1, 33.3));
        data.add(new TestThing(4, 99.1));

        // Tested method
        final CsvColumnConfig oc = new CsvColumnConfig(new String[] {"ID", null}, new CellProcessor[] {new ParseInt(), null});
        csv.Write(fullFileName, data , oc , false);

        // Verify (in case when column is wrongly provided it will not be save in output file).
        final String expectedContent = "ID\n" +
                                       "1\n" +
                                       "4\n";
        verifyFileContent(fullFileName, expectedContent);
    }
    
    @Test
    public void testReadNoHeader() {
        final String fullFileName = fullFileName("testRead.csv");

        // Prepare data
        final String expectedContent = "1,33.3\n" +
                                       "4,99.1\n";
        saveFile(fullFileName, expectedContent);

        // Tested method
        final List<TestThing> outdst = csv.Read(fullFileName, oc, /* skipHeader */ true);

        // Verify
        final List<TestThing> expectedData = new ArrayList<TestThing>();
        expectedData.add(new TestThing(1, 33.3));
        expectedData.add(new TestThing(4, 99.1));
        assertEquals(expectedData, outdst);
    }
}
