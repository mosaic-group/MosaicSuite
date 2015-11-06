package mosaic.core.imageUtils.iterators;

/**
 * Iterates over a Region within an InputImage,
 * but returns indices relative to the region (and not input)
 */
public class MaskIterator {

    RegionIterator it = null;
    
    /**
     * Iterating over region is implemented in such a way,
     * that region is used as input' (for RegionIterator) and
     * the intersection(input,region) is used as region'.
     * Offset' is old(Offset) to intersection
     * So the indices are returned relative to old(region).
     */
    public MaskIterator(int[] input, int[] region, int[] ofs) {
        // sets the "input size" to the region size
        final int[] maskSizes = region.clone();
        final int[] maskOfs = ofs.clone();

        // TODO: this is cropping, actually?
        for (int i = 0; i < region.length; i++) {
            if (ofs[i] < 0) {
                // if ofs < 0, then region is cropped, and the iterator' doesn't start at 0,0
                // but starts at an ofs which points to the intersection(input, region)
                maskOfs[i] = (-ofs[i]); // start in mask and not at 0,0
                maskSizes[i] += ofs[i]; // this may be done in crop?
            }
            else {
                // the startpoint of region' is within old(input), so the region'-iterator starts at 0,0
                maskOfs[i] = 0;
            }
            // mask overflow
            if (ofs[i] + region[i] > input[i]) {
                final int diff = ofs[i] + region[i] - input[i];
                maskSizes[i] -= diff;
            }
        }

        it = new RegionIterator(region, maskSizes, maskOfs);
    }
    
    public int next() {
        return it.next();
    }
}
