package mosaic.psf2d;



/**
	 * Defines a particle and holds all the relevant info for it.
	 * X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
	 * (0,0) is the upper left corner
	 * x is horizontal left to right
	 * y is vertical top to bottom
	 */
	public class PsfSourcePosition{
		
		public float x;	// x-position of Particle
		public float y;	// y-position of Particle
		float m0;	// m0 and m2 are internal variables used for
		float m2;	// position refinement (see Particle Tracker by Guy Levy)
		
		/**
		 * Constructor. 
		 * @param x - original x coordinates
		 * @param y - original y coordinates
		 */
		public PsfSourcePosition (float x, float y) {
			this.x = x;
			this.y = y;
		}
	}