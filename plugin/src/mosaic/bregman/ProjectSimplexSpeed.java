package mosaic.bregman;


class ProjectSimplexSpeed {

    private static void bubble_sort_descend(double[] a) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            boolean finished = true;
            for (int j = 1; j < len; j++) {
                if (a[j - 1] < a[j]) {
                    finished = false;
                    double temp = a[j - 1];
                    a[j - 1] = a[j];
                    a[j] = temp;
                }
            }

            if (finished) {
                return;
            }
        }
    }

    public static void project(double[][][][] output1, double[][][][] input1, int dx, int dy, int nl) {
        // iterate the pixels of the 2d image
        int dimy, dimx, dimz;
        dimy = dy;
        dimx = dx;
        dimz = nl;

        int x, y, z;
        final double[] v = new double[dimz];
        final double[] mu = new double[dimz];
        double sm, row, sm_row, theta, val;

        for (x = 0; x < dimx; x++) {
            for (y = 0; y < dimy; y++) {

                // along the z axis: get the vector v,mu values:
                for (z = 0; z < dimz; z++) {
                    v[z] = input1[z][0][x][y];
                    mu[z] = v[z];
                }

                // sort v for this x,y position
                bubble_sort_descend(mu);

                // find theta for this x,y position
                sm = 0.0;
                row = sm_row = 1;// init to what ??
                for (z = 0; z < dimz; z++) {
                    sm += mu[z];
                    if (mu[z] - (1.0 / (z + 1)) * (sm - 1) > 0) {
                        row = z + 1;
                        sm_row = sm;
                    }
                }
                theta = (1.0 / row) * (sm_row - 1.0);

                // subtract theta from v
                for (z = 0; z < dimz; z++) {
                    val = v[z] - theta;
                    output1[z][0][x][y] = (val > 0.0) ? val : 0.0;
                }
            }
        }
    }
}
