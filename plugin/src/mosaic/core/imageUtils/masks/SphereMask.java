package mosaic.core.imageUtils.masks;


import java.util.Random;

import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.iterators.IndexIterator;


public class SphereMask extends Mask {

    private boolean rnd = false;
    private final int dim;

    private final int m_Size[];
    private final int m_Radius[];
    private final float spacing[];

    private final IndexIterator iterator;
    private int fgPoints = 0;

    /*
     * Get the number or Foreground points in the mask
     */
    @Override
    public int getFgPoints() {
        return fgPoints;
    }

    /**
     * Create a sphere mask
     *
     * @param radius Radius of the sphere
     * @param size Size of the region containing the sphere
     * @param dim dimensionality
     */
    public SphereMask(int radius, int size, int dim) {
        this(radius, size, dim, null, false);
    }

    /**
     * Create a Sphere mask with radius and spacing
     *
     * @param radius Radius of the circle
     * @param size Size of the region containing the circle
     * @param dim dimensionality
     * @param spacing Coordinate spacing
     * @param rnd subpixel randomizer
     */
    public SphereMask(int radius, int size, int dim, float[] spacing, boolean rnd_) {
        // TODO: It seems that it does not draw nice spheres - right/bottom pixels are cut (in 2D case) when size = 2 * radius
        this.dim = dim;
        rnd = rnd_;
        m_Size = new int[dim];
        m_Radius = new int[dim];

        for (int i = 0; i < dim; i++) {
            m_Radius[i] = radius;
            m_Size[i] = size;
        }

        iterator = new IndexIterator(m_Size);

        if (spacing != null) {
            this.spacing = spacing;
        }
        else {
            this.spacing = new float[dim];
            for (int i = 0; i < dim; i++) {
                this.spacing[i] = 1.0f;
            }
        }
        
        mask = new byte[iterator.getSize()];
        fillMask();
    }

    private void fillMask() {
        fgPoints = 0;
        final int size = iterator.getSize();

        if (rnd == true) {
            final Random r = new Random();

            for (int i = 0; i < size; i++) // over region
            {
                final Point ofs = iterator.indexToPoint(i);

                final int[] vIndex = (ofs).iCoords;

                float vHypEllipse = 0;
                for (int vD = 0; vD < dim; vD++) {
                    vHypEllipse += (vIndex[vD] + 0.5 - (m_Size[vD]) / 2.0) * spacing[vD] * (vIndex[vD] + 0.5 - (m_Size[vD]) / 2.0) * spacing[vD] / (m_Radius[vD] * m_Radius[vD]);
                }

                if (vHypEllipse == 0) {
                    // mid - point (ensure at least one point)

                    fgPoints++;

                    // is in region
                    mask[i] = fgVal;
                }
                else {
                    vHypEllipse += 3.0 * (r.nextFloat() - 0.001f) / (m_Radius[0] * m_Radius[0]);

                    if (vHypEllipse <= 1.0f) {
                        fgPoints++;
                        mask[i] = fgVal;
                    }
                    else {
                        mask[i] = bgVal;
                    }
                }
            } 
        }
        else {
            for (int i = 0; i < size; i++) // over region
            {
                final Point ofs = iterator.indexToPoint(i);

                final int[] vIndex = (ofs).iCoords;

                float vHypEllipse = 0;
                for (int vD = 0; vD < dim; vD++) {
                    vHypEllipse += (vIndex[vD] - (m_Size[vD]) / 2.0) * spacing[vD] * (vIndex[vD] - (m_Size[vD]) / 2.0) * spacing[vD] / (m_Radius[vD] * m_Radius[vD]);
                }

                if (vHypEllipse <= 1.0f) {
                    fgPoints++;
                    mask[i] = fgVal;
                }
                else {
                    mask[i] = bgVal;
                }
            }
        }
    }

    @Override
    public boolean isInMask(int idx) {
        return mask[idx] == fgVal;
    }

    @Override
    public int[] getDimensions() {
        return this.m_Size;
    }
}
