package mosaic.core.imageUtils.masks;

import mosaic.core.imageUtils.Connectivity;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.iterators.IndexIterator;

public class CircleMask extends Mask {

    private final int dim;
    private final int rad;

    private final int m_Size[];
    private final int m_Radius[];
    private final float spacing[];

    private final IndexIterator iterator;
    private int fgPoints = 0;

    /**
     * Create a circle mask with radius and spacing
     *
     * @param radius Radius of the circle
     * @param size Size of the region containing the circle
     * @param dim dimensionality
     * @param spacing Coordinate spacing
     */
    public CircleMask(int radius, int size, int dim, float[] spacing) {
        this.dim = dim;
        rad = radius;

        m_Size = new int[dim];
        m_Radius = new int[dim];

        for (int i = 0; i < dim; i++) {
            m_Radius[i] = radius;
            m_Size[i] = size;
        }

        iterator = new IndexIterator(m_Size);

        this.spacing = spacing;
        mask = new byte[iterator.getSize()];
        fillMask();
    }

    private float rHypEllipse(int[] vIndex) {
        float vHypEllipse = 0.0f;
        for (int vD = 0; vD < dim; vD++) {
            vHypEllipse += (vIndex[vD] + 0.5 - (m_Size[vD]) / 2.0) * spacing[vD] * (vIndex[vD] + 0.5 - (m_Size[vD]) / 2.0) * spacing[vD] / (m_Radius[vD] * m_Radius[vD]);
        }

        return vHypEllipse;
    }

    private boolean isBoundary(Point ps, Connectivity c) {
        for (Point p : c.iterator()) {
            p = p.add(ps);
            final float vHypEllipse = rHypEllipse(p.iCoords);
            if (vHypEllipse >= 1.0f) {
                return true;
            }
        }
        return false;
    }

    private void fillMask() {
        final Connectivity c = new Connectivity(this.dim, this.dim - 1);
        fgPoints = 0;
        final int size = iterator.getSize();
        for (int i = 0; i < size; i++) // over region
        {
            final Point ofs = iterator.indexToPoint(i);

            final float vHypEllipse = rHypEllipse(ofs.iCoords);

            if (vHypEllipse < 1.0f) {
                // Check the neighboorhood

                if (isBoundary(ofs, c) == true) {
                    mask[i] = fgVal;
                    fgPoints++;
                }
                else {
                    mask[i] = bgVal;
                }

            }
        } // for
    }

    @Override
    public boolean isInMask(int idx) {
        return mask[idx] == fgVal;
    }

    public int getRadius() {
        return this.rad;
    }

    @Override
    public int[] getDimensions() {
        return this.m_Size;
    }

    /*
     * Get the number or Foreground points in the mask
     */
    @Override
    public int getFgPoints() {
        return fgPoints;
    }

}
