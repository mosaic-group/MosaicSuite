package mosaic.region_competition;

/**
 * This class provides (always increasing) label number.
 */
class LabelDispenser {
    // Init to 0. This should be value returned as a highest label used, before
    // getNewLabel() is called for first time.
    private int iLabelNumber = 0;

    public int getHighestLabelEverUsed() {
        return iLabelNumber;
    }

    public int getNewLabel() {
        return ++iLabelNumber;
    }

    public void setMaxValueOfUsedLabel(Integer aNumOfUsedLabels) {
        if (aNumOfUsedLabels > iLabelNumber) {
            iLabelNumber = aNumOfUsedLabels;
        }
    }
}
