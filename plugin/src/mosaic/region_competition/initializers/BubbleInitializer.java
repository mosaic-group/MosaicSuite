package mosaic.region_competition.initializers;


import mosaic.core.utils.IndexIterator;
import mosaic.core.utils.Point;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.utils.BubbleDrawer;


public class BubbleInitializer extends Initializer {

    public BubbleInitializer(LabelImageRC labelImage) {
        super(labelImage);
    }

    // default values
    private final int radius = 5;
    private final int displacement = 15;

    /**
     * Initializes bubbles by radius size and the gaps between the bubble center points
     * 
     * @param rad
     * @param displ
     */
    private void initSizeDispl(int rad, int displ) {
        final int[] grid = new int[dim];
        final int[] gap = new int[dim];

        // Check we have at least one bubble

        for (int i = 0; i < dim; i++) {
            final int size = dimensions[i];

            // only one bubble

            if (size < displ) {
                displ = size;
            }

            if (2 * rad > size) {
                rad = size / 2;
            }
            if (rad == 0) {
                rad = 1;
            }
        }

        for (int i = 0; i < dim; i++) {
            final int size = dimensions[i];
            final int n = (size) / displ; // how many bubbles per length
            grid[i] = n;
            gap[i] = (size % displ + displ) / 2 - rad;
            if (gap[i] < 0) {
                gap[i] = 0;
            } // when displ < 2 rad ( with only one bubble displ is meaningless )
        }
        final Point gapPoint = new Point(gap);

        final BubbleDrawer bd = new BubbleDrawer(labelImage, rad, 2 * rad);
        final IndexIterator it = new IndexIterator(grid);
        int bubbleIndex = 1;
        for (Point ofs : it.getPointIterable()) {
            // upper left startpoint of a bubble+spacing
            ofs = ofs.mult(displ).add(gapPoint);
            // RegionIterator rit = new RegionIterator(m_LabelImage.dimensions, region, ofs.x);

            bd.drawUpperLeft(ofs, bubbleIndex);
            bubbleIndex++;
            // bd.doSphereIteration(ofs, labelDispenser.getNewLabel());
        }
    }

    public void initSizePaddig(int radius, int padding) {
        final int displ = padding + 2 * radius;
        initSizeDispl(radius, displ);
    }

    @Override
    public void initDefault() {
        initSizeDispl(radius, displacement);
    }
}
