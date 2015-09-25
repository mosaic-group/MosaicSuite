package mosaic.core.utils;


/**
 * Iterates over a Region within an InputImage,
 * but returns indices relative to the region (and not input)
 */
class MaskIterator extends RegionIterator {

    /**
     * Iterating over region is implemented in such a way,
     * that region is used as input' (for RegionIterator) and
     * the intersection(input,region) is used as region'.
     * Offset' is old(Offset) to intersection
     * So the indices are returned relative to old(region).
     */
    public MaskIterator(int[] input, int[] region, int[] ofs) {

        // sets the "input size" to the region size
        super(region, region, ofs);
        final int[] maskSizes = region.clone();
        final int[] maskOfs = new int[dimensions];

        // TODO: this is cropping, actually?
        for (int i = 0; i < dimensions; i++) {
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

        setRegion(maskSizes);
        setOfs(maskOfs);
        crop(); // recalculates startindex and new size, cropping should be done already.
    }

}
