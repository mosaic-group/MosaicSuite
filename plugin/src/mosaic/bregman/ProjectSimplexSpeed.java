package mosaic.bregman;


class ProjectSimplexSpeed {

	static	void  bubble_sort_descend(double [] a , int len) {

		int i, j;
		double temp;
		boolean  finished;

		for (i = 0; i < len; i++) {
			finished = true;
			for (j = 1; j < len; j++) {
				if (a[j-1] < a[j]) {
					finished = false;
					temp = a[j-1];
					a[j-1] = a[j];
					a[j] = temp;
				}

			}

			if(finished) return;
		}
		return;
	}

	public static void project(double  [] [] [] [] output1, double  [] [] [] [] input1, int dx, int dy, int nl){
//TODO :3D version 

		// iterate the pixels of the 2d image

		int dimy, dimx, dimz;
		dimy = dy; 
		dimx = dx;
		dimz = nl;

		int x,y,z;
		double []  v = new double[dimz];
		double [] mu = new double[dimz];
		double sm, row, sm_row, theta, val;

		for(x= 0; x < dimx; x++) {
			for(y = 0; y < dimy; y++) {

				// along the z axis: get the vector v,mu values:
				for(z = 0; z < dimz; z++) {
					//IJ.log("z value : " + z + "x" +x + "y"+y);
					v[z] = input1[z][0][x][y];
					mu[z] = v[z];
				}

				// sort v for this x,y position
				bubble_sort_descend(mu, dimz);

				// find theta for this x,y position
				sm = 0.0;
				row=sm_row=1;// init to what ??
				//bool values_set = false;
				for(z = 0; z < dimz; z++) {
					sm += mu[z];
					if(mu[z] - (1.0/(z+1)) * (sm-1) > 0){
						row = z+1;
						sm_row = sm;
						//  values_set = true;
					}
				}
				theta = (1.0/row) * (sm_row - 1.0);


				// subtract theta from v
				for(z = 0; z < dimz; z++) {
					val = v[z]-theta;
					output1[z][0][x][y] = (val > 0.0)? val : 0.0;
				}

			}
		}
		//return;
	}


}
