package mosaic.variationalCurvatureFilters;


/**
 * This class implements filter running in "no split" mode. (Image is not divided into subsets)
 * It requires proper filter kernel (GC, ...)
 * @author Krzysztof Gonciarz
 */
public class NoSplitFilter3D implements CurvatureFilter3D {
    FilterKernel3D iFk3D;

    public NoSplitFilter3D(FilterKernel3D aFilterKernel) {
        iFk3D = aFilterKernel;
    }

    @Override
    public void runFilter(float[][][] aImg, int aNumOfIterations) {
        final int Z = aImg.length - 1;
        final int Y = aImg[0].length - 1;
        final int X = aImg[0][0].length - 1;

        // 3 hex numbers for starting point  "0x row col depth" for 2x2x2 cube
        // addressing all corners of such cube (with numbers starting from 1)
        // They are following sequence from decomposition:
        // - BT - Black Triangle
        // - BC - Black Circle
        // - BE - Black Ellipse
        // - BR - Black Rectangle
        // - WC - White Circle
        // - WT - White Triangle
        // - WE - White Ellipse
        // - WR - White Rectangle
        int[] sequence = {0x111, 0x221, 0x212, 0x122, 0x121, 0x211, 0x112, 0x222};

        for (int it = 0; it < aNumOfIterations; ++it) {
            for (int seq = 0; seq <= 7; ++seq) {
                int seqCode = sequence[seq];
                int dep = seqCode & 0x00f;
                int row = (seqCode & 0x0f0) >> 4;
            int col = (seqCode & 0xf00) >> 8;

            for (int z = dep; z < Z; z += 2) {
                for (int y = row; y < Y; y += 2) {
                    for (int x = col; x < X; x += 2) {

                        aImg[z][y][x] += iFk3D.filterKernel(aImg, x, y, z);

                    }
                }
            }
            }
        }
    }
}
