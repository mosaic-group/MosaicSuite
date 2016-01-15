package mosaic.bregman;


class ProjectSimplexSpeed {
//
//    private static void bubble_sort_descend(double[] a) {
//        int len = a.length;
//        for (int i = 0; i < len; i++) {
//            boolean finished = true;
//            for (int j = 1; j < len - i; j++) {
//                if (a[j - 1] < a[j]) {
//                    finished = false;
//                    double temp = a[j - 1];
//                    a[j - 1] = a[j];
//                    a[j] = temp;
//                }
//            }
//
//            if (finished) {
//                return;
//            }
//        }
//    }
//
//    public static void project(double[][][][] output1, double[][][][] input1, int dx, int dy, int dl) {
//        final double[] mu = new double[dl];
//
//        for (int x = 0; x < dx; x++) {
//            for (int y = 0; y < dy; y++) {
//
//                // along the l axis: get the vector mu values:
//                for (int l = 0; l < dl; l++) {
//                    mu[l] = input1[l][0][x][y];
//                }
//
//                // sort v for this x,y position
//                bubble_sort_descend(mu);
//
//                // find theta for this x,y position
//                double sm = 0;
//                double row = 0; 
//                double sm_row = 0;
//                for (int l = 0; l < dl; l++) {
//                    sm += mu[l];
//                    if (mu[l] - (1.0 / (l + 1)) * (sm - 1) > 0) {
//                        row = l + 1;
//                        sm_row = sm;
//                    }
//                }
//                double theta = (1.0 / row) * (sm_row - 1.0);
//                for (int l = 0; l < dl; l++) { 
//                    double val = input1[l][0][x][y] - theta;
//                    output1[l][0][x][y] = (val > 0.0) ? val : 0.0;
//                }
//            }
//        }
//    }
}
