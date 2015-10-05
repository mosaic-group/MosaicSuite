package mosaic.region_competition.initializers;


import mosaic.core.utils.RegionIterator;
import mosaic.region_competition.LabelImageRC;


public class BoxInitializer extends Initializer {

    public BoxInitializer(LabelImageRC labelImage) {
        super(labelImage);
    }

    /**
     * creates an initial guess (of the size r*labelImageSize)
     * 
     * @param r fraction of sizes of the guess
     */
    public void initRatio(double r) {
        // Calculate size of region for iteration and offset in original dimensions "space"
        final int[] region = dimensions.clone();
        final int[] ofs = dimensions.clone();
        for (int i = 0; i < dim; i++) {
            region[i] = (int) (region[i] * r);
            ofs[i] = (dimensions[i] - region[i]) / 2;
        }

        // Mark chosen region with '1' label
        final int label = 1;
        final RegionIterator it = new RegionIterator(dimensions, region, ofs);
        while (it.hasNext()) {
            final int idx = it.next();
            labelImage.setLabel(idx, label);
        }
    }
}
