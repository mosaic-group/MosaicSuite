package mosaic.core.particleLinking;


import java.util.List;
import java.util.Vector;

import mosaic.core.detection.Particle;


public interface ParticleLinker {

    /**
     * Second phase of the algorithm - <br>
     * Identifies points corresponding to the
     * same physical particle in subsequent frames and links the positions into trajectories <br>
     * The length of the particles next array will be reset here according to the current linkrange
     */
    public boolean linkParticles(List<Vector<Particle>> aParticles, LinkerOptions aLinkerOptions);

}
