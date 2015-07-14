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
    
    public static void print(String aPrefix, Object... aObjects) {
        System.out.print(aPrefix + ": ");
        print(aObjects);
    }

}
