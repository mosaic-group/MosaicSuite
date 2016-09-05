package mosaic.utils.io.serialize;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;

import mosaic.test.framework.CommonBase;


public class DataFileTest extends CommonBase {
    /**
     * Object used for testing serialization
     */
    static public class TestConfig implements java.io.Serializable {
        private static final long serialVersionUID = 5786702961653019842L;

        public int iId;
        public double iValue;
        public String[] iNames;

        @Override
        public String toString() {
            return "[" + iId + ", " + iValue + ", (" + Arrays.toString(iNames) + ")]";
        }
    }

    /**
     * This test saves a object and after that reads it again and validates data.
     */
    @Test
    public void testGeneral() {
        // Prepare data
        final String fileName = getCleanTestTmpPath() + "testGeneral.dat";
        final TestConfig expectedData = new TestConfig();
        expectedData.iId = 3;
        expectedData.iNames = new String[] {"Krzysztof", "Magdalena"};
        expectedData.iValue = 1.23;

        // Tested method - write object to file
        final DataFile<TestConfig> df = new SerializedDataFile<TestConfig>();
        assertTrue(df.SaveToFile(fileName, expectedData));

        // Tested method - read object from file
        final TestConfig result = new SerializedDataFile<TestConfig>().LoadFromFile(fileName, TestConfig.class);

        // Verify result
        assertNotNull(result);
        assertEquals(expectedData.iId, result.iId);
        assertEquals(expectedData.iValue, result.iValue, 0.0);
        assertArrayEquals(expectedData.iNames, result.iNames);
    }

    @Test
    public void testReadNotExisting() {
        // Prepare data
        final String fileName = getCleanTestTmpPath() + "testReadNotExisting.dat";

        // Make sure that file is not existing
        final File f = new File(fileName);
        assertFalse(f.exists());

        // Tested method - read object from file
        final TestConfig result = new SerializedDataFile<TestConfig>().LoadFromFile(fileName, TestConfig.class);

        // Verify result
        assertNull(result);
    }

    @Test
    public void testReadWrongObject() {
        // Prepare data
        final String fileName = getCleanTestTmpPath() + "testReadWrongObject.dat";

        // Write different type of object than the one to be read.
        final DataFile<String> df = new SerializedDataFile<String>();
        df.SaveToFile(fileName, new String("Hello world!!!"));

        // Tested method - read object from file
        final TestConfig result = new SerializedDataFile<TestConfig>().LoadFromFile(fileName, TestConfig.class);

        // Verify result
        assertNull(result);
    }

    @Test
    public void testWriteToWrongFile() {
        // Prepare data
        final String fileName = getCleanTestTmpPath(); // try to write to directory instead of file
        final TestConfig expectedData = new TestConfig();
        expectedData.iId = 3;
        expectedData.iNames = new String[] {"Krzysztof", "Magdalena"};
        expectedData.iValue = 1.23;

        // Tested method - write object to file
        final DataFile<TestConfig> df = new SerializedDataFile<TestConfig>();
        assertFalse(df.SaveToFile(fileName, expectedData));
    }
}
