package mosaic.utils;

import java.lang.reflect.Array;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * Debug printouts for troubleshooting. Contains some convenient methods
 * for usual style of printing out on console.
 *
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class Debug { // NO_UCD (code used only for debugging)
    /**
     * This method should be used for outputing debug. Easy to turn off all messages or change it to logger.
     * @param aDebugMessage
     */
    public static void debugPrint(String aDebugMessage) {
        System.out.println(aDebugMessage);
    }
    
    /**
     * prints output with additional new line of string taken from {@link Debug#getString}
     */
    public static void print(Object... aObjects) {
        debugPrint(getString(aObjects) + "\n");
    }

    /**
     * @return String with values taken from toString() from each object formated as "[obj1.toString()] [obj2.toString] ..."
     */
    public static String getString(Object... aObjects) {
        String str = "";
        for (final Object o : aObjects) {
            if (o != null && o.getClass().isArray()) {
                // Just to be able use Arrays utils pack an array (of unknown type) into one level more of
                // objects array. Arrays.* will do the job. Additionally it will allow to pack more information
                // like dimensions of array
                Object[] arr = new Object[] {"Dim/Content", getArrayDims(o), o};
                str += Arrays.deepToString(arr);
            }
            else {
                str += "[" + o + "] ";
            }
        }
        return str;
    }

    /**
     * @return JSON pretty string with object information.
     */
    public static String getJsonString(Object... aObjects) {
        String str = "";
        for (final Object obj : aObjects) {
            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            final String json = gson.toJson(obj);
            str += "[" + json + "] ";
        }
        return str;
    }
    
    /**
     * @return String with all dimensions of array. It handles n-dimensional input like int[] or double[][][][].
     */
    public static String getArrayDims(Object aArray) {
        if (aArray == null) return "null[]";
        if (!aArray.getClass().isArray()) return "Not an array!";
        StringBuilder result = new StringBuilder();
        deepLength(aArray, result);
        return result.toString();
    }

    // Helper for getArrayDims which recurently goes deeper into provided array
    private static void deepLength(Object aArrayObj, StringBuilder aOutputBuffer) {
        int len = Array.getLength(aArrayObj);
        if (len == 0) {
            // possibly more dims if nasty (but possible) creation of array like new int[3][0][11]
            aOutputBuffer.append("[0] ...(possibly more dims)");
            
            // cannot go deeper
            return;
        } else {
            aOutputBuffer.append("[" + (len) + "]");
        }
        
        Object deepElement = Array.get(aArrayObj, 0);
        if (deepElement != null) {
            Class<?> deepClass = deepElement.getClass();
            if (deepClass.isArray()) {
                // We can still go deeper
                deepLength(deepElement, aOutputBuffer);
            }
        }
    }
    
    /**
     * @return String with requested number of entries of stack trace. If aNumberOfEntires == -1 then whole stack is printed.
     */
    public static String getStack(int aNumberOfEntries) {
        // Skip two first lines which are entries for getStackTrace, getStack(,) and current getStack() methods.
        return getStack(aNumberOfEntries, 3);
    }
    
    /**
     * @param aNumberOfEntries - how many lines of stack should be in output string?
     * @param aSkipNumOfFirstLines - how many lines of stack skip? (default 2: skips entries for debug code
     *                               and first line is a place of calling this method).
     * @return String with requested number of entries of stack trace
     */
    public static String getStack(int aNumberOfEntries, int aSkipNumOfFirstLines) {
        StringBuilder result = new StringBuilder();
        // new Exception().printStackTrace();
        int count = -aSkipNumOfFirstLines;
        for (StackTraceElement ste : Thread.currentThread( ).getStackTrace()) {
            // Skip requested number of first lines
            if (count++ <= -1) continue;
            
            // Output requested number of stack trace
            if (aNumberOfEntries != -1 && count > aNumberOfEntries) break;
            result.append(ste.toString() + "\n");
        }
        
        return result.toString();
    }
    
    /**
     * Prints whole stack trace.
     */
    public static void printStack() {
        debugPrint(getStack(-1, 3));
    }
}
