package mosaic.core.particleLinking;


import mosaic.core.detection.MyFrame;


public interface ParticleLinker {

    /**
     * Second phase of the algorithm - <br>
     * Identifies points corresponding to the
     * same physical particle in subsequent frames and links the positions into trajectories <br>
     * The length of the particles next array will be reset here according to the current linkrange
     */
    public boolean linkParticles(MyFrame[] frames, int frames_number, linkerOptions l);

}
