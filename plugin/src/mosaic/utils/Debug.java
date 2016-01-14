package mosaic.utils;

import java.lang.reflect.Array;

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
     * prints output with additional new line of string taken from {@link Debug#getString}
     */
    public static void print(Object... aObjects) {
        System.out.println(getString(aObjects));
        System.out.println();
    }

    /**
     * @return String with values taken from toString() from each object formated as "[obj1.toString()] [obj2.toString] ..."
     */
    public static String getString(Object... aObjects) {
        String str = "";
        for (final Object o : aObjects) {
            str += "[" + o + "] ";
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
}
