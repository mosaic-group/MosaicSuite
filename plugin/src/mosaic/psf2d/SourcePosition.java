package mosaic.psf2d;



	/**
	 * Defines a particle and holds all the relevant info for it.
	 * X and Y coordinates are not in the usual graph coordinates sense but in the image sense.
	 * (0,0) is the upper left corner,
	 * x is horizontal left to right,
	 * y is vertical top to bottom.
	 * @author Benedikt Baumgartner
	 */
	public class SourcePosition{
		
		public float x;	// x-position of Particle
		public float y;	// y-position of Particle
		public float m0;	// m0 and m2 are internal variables used for
		public float m2;	// position refinement (see Particle Tracker by Guy Levy)
		
		/**
		 * Constructor. 
		 * @param x - original x coordinates
		 * @param y - original y coordinates
		 */
		public SourcePosition (float x, float y) {
			this.x = x;
			this.y = y;
		}

		public float getX() {
			return x;
		}

		public void setX(float x) {
			this.x = x;
		}

		public float getY() {
			return y;
		}

		public void setY(float y) {
			this.y = y;
		}
	}