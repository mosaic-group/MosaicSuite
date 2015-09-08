package mosaic.plugins.utils;


/**
 * Debug printouts for troubleshooting. Contains some convinient methods
 * for usual style of printing out on console.
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class Debug {

    public static void print(Object... aObjects) {
        for (Object o : aObjects) { 
                System.out.print("[" + o + "] ");
        }
        System.out.println();
    }
    
    public static String getString(Object... aObjects) {
        String str = "";
        for (Object o : aObjects) { 
            str += "[" + o + "] ";
        }
        return str;
    }
}
