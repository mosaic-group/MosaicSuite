package mosaic.bregman;


import edu.emory.mathcs.jtransforms.dct.DoubleDCT_3D;


abstract class ASplitBregmanSolverTwoRegions3D extends ASplitBregmanSolverTwoRegions {

    public final double[][][][] w2zk;
    public final double[][][][] b2zk;
    public final double[][][][] ukz;
    public final double[][][] eigenLaplacian3D;
    public final DoubleDCT_3D dct3d;

    public ASplitBregmanSolverTwoRegions3D(Parameters params, double[][][] image, double[][][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        super(params, image, mask, md, channel, ap);
        this.w2zk = new double[nl][nz][ni][nj];
        this.ukz = new double[nl][nz][ni][nj];
        this.b2zk = new double[nl][nz][ni][nj];
        this.eigenLaplacian3D = new double[nz][ni][nj];
        dct3d = new DoubleDCT_3D(nz, ni, nj);

        for (int i = 0; i < nl; i++) {
            LocalTools.fgradz2D(w2zk[i], mask[i]);
        }

        for (int l = 0; l < nl; l++) {
            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        b2zk[l][z][i][j] = 0;
                    }
                }
            }
        }

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    this.eigenLaplacian3D[z][i][j] = 2 + (2 - 2 * Math.cos((j) * Math.PI / (nj)) + (2 - 2 * Math.cos((i) * Math.PI / (ni))) + (2 - 2 * Math.cos((z) * Math.PI / (nz))));
                }
            }
        }
    }
}
