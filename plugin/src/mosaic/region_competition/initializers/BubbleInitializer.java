package mosaic.region_competition.initializers;


import mosaic.core.image.IndexIterator;
import mosaic.core.image.LabelImage;
import mosaic.core.utils.Point;
import mosaic.region_competition.utils.BubbleDrawer;

/**
 * Initialize label image with bubbles with requested radius of bubble and padding space between bubbles.
 * Maximum possible number of bubbles is generated which can fit the image.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class BubbleInitializer extends Initializer {

    public BubbleInitializer(LabelImage labelImage) {
        super(labelImage);
    }

    public void initialize(int aRadius, int aPadding) {
        // Set radius with minimum value of 1
        int radius = (aRadius <= 0) ? 1 : aRadius;

        // Try to fit at least one bubble by shrinking expected radius
        for (int i = 0; i < iNumOfDimensions; i++) {
            final int size = iDimensionsSize[i];
            
            if (2 * radius > size) {
                radius = size / 2;
            }
        }
        
        // TODO: any action if image is really small (<2px) and radius is equal to 0?
        
        // Now we are ready to generate all needed bubbles
        initializeBubbles(radius, aPadding);
    }
    
    /**
     * Initializes bubbles
     * @param aRadius - radius of bubbles
     * @param aDisplacement - length between bubbles centers
     */
    private void initializeBubbles(final int aRadius, final int aPadding) {
        final int[] gridDims = new int[iNumOfDimensions];
        final int[] borderOffset = new int[iNumOfDimensions];
        calculateGridDimensionsAndGapsBetweenBubbles(aRadius, aPadding, gridDims, borderOffset);
        
        drawBubblesWithLabels(aRadius, aPadding, gridDims, borderOffset);
    }

    /**
     * Calculate grid dimensions and border offset.
     */
    private void calculateGridDimensionsAndGapsBetweenBubbles(final int aRadius, final int aPadding, final int[] aGridDims, final int[] aBorderOffset) {
        for (int i = 0; i < iNumOfDimensions; i++) {
            final int size = iDimensionsSize[i];
            
            final int bubblesPerLength = (size + aPadding) / (aPadding + 2 * aRadius);
            aGridDims[i] = bubblesPerLength;
            aBorderOffset[i] = (size - (bubblesPerLength * 2 * aRadius + (bubblesPerLength - 1) * aPadding)) / 2;
        }
    }

    /**
     * Draw bubbles on a each point of grid with offset
     * @param aRadius
     * @param aDisplacement
     * @param aGrid
     * @param aBorderOffset
     */
    private void drawBubblesWithLabels(int aRadius, int aPadding, final int[] aGrid, final int[] aBorderOffset) {
        final Point offset = new Point(aBorderOffset);

        final BubbleDrawer bd = new BubbleDrawer(iLabelImage, aRadius, 2 * aRadius);
        final IndexIterator it = new IndexIterator(aGrid);
        int bubbleLabel = 1;
        for (Point ofs : it.getPointIterable()) {
            ofs = ofs.mult(aPadding + 2 * aRadius).add(offset);
            bd.drawUpperLeft(ofs, bubbleLabel);
            bubbleLabel++;
        }
    }
}
