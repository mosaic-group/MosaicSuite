package mosaic.plugins.utils;

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
