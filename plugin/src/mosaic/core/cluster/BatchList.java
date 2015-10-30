package mosaic.core.cluster;


/**
 * A list of all the batch system classes implemented
 *
 * @author Pietro Incardona
 */

public class BatchList {

    /**
     * Return a list of all implemented batch system
     *
     * @return A list of all implemented batch systems
     */

    public static String[] getList() {
        return new String[] { "LSF" };
    }
}
