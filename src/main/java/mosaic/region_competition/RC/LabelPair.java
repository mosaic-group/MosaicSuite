package mosaic.region_competition.RC;


/**
 * A pair of labels, used to save 2 competing labels.
 */
class LabelPair {

    final int first; // smaller value
    final int second; // bigger value

    public LabelPair(int l1, int l2) {
        if (l1 < l2) {
            first = l1;
            second = l2;
        }
        else {
            first = l2;
            second = l1;
        }
    }
}
