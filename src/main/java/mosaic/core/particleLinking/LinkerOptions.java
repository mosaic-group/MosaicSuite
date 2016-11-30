package mosaic.core.particleLinking;


/**
 * Linker options for the particle Linker
 */
public class LinkerOptions {
    
    // Maximum linking range (in frame numbers)
    public int linkRange = 1;

    // Maximum displacement per frame
    public float maxDisplacement = 10;

    // Introduce a force cost term
    public boolean force = false;

    // Introduce an angle deviation cost term
    public boolean straightLine = false;
    
    // Minimum displacement in order to consider a link a a valid
    // indication of angle deviation
    final float minSquaredDisplacementForAngleCalculation = 9;

    // space cost term (how much object change position)
    public float lSpace = 1.0f;

    // feature cost term (how much objects change internal characteristics 
    // or attribute intensity or size ... )
    public float lFeature = 1.0f; 
    
    // dynamic cost term (how much objects change their dynamic from a given model)
    public float lDynamic = 1.0f;
}
