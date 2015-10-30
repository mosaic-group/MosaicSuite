package mosaic.core.particleLinking;


/**
 * Linker options for the particle Linker
 *
 * @author Pietro Incardona
 */

public class linkerOptions {

    /**
     * Maximum range (in frame) between link
     */
    public int linkrange = 1;

    /**
     * Maximum displacement
     */

    public float displacement = 10;

    /**
     * Minimum displacement in order to consider a link a a valid
     * indication of angle deviation
     */

    final float r_sq = 9;

    /**
     * introduce an angle deviation cost term
     */

    public boolean straight_line = false;

    /**
     * Introduce a force cost term
     */

    public boolean force = false;

    /**
     * Coupling constant between term
     * in the cost function
     * space cost term l_d (how much object change position)
     * feature cost term l_s (how much objects change internal characteristics or attribute intensity or size ... )
     * dynamic cost term l_f (how much objects change their dynamic from a given model)
     */

    public float l_s = 1.0f;
    public float l_f = 1.0f;
    public float l_d = 1.0f;
}
