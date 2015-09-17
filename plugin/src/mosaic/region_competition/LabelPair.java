package mosaic.region_competition;

/**
 * A pair of labels, used to save 2 competing labels.
 * Is comparable.
 */
class LabelPair implements Comparable<LabelPair>
{
    int first;		// smaller value
    int second;		// bigger value

    public LabelPair(int l1, int l2)
    {
        if (l1<l2){
            first=l1;
            second=l2;
        }
        else{
            first=l2;
            second=l1;
        }
    }

    public int getSmaller(){
        return first;
    }

    public int getBigger(){
        return second;
    }

    /**
     * Compares this object with the specified object for order.
     * Returns a negative integer, zero, or a positive integer
     * as this object is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(LabelPair o)
    {
        int result = this.first-o.first;
        if (result == 0)
        {
            result = this.second - o.second;
        }
        return result;
    }
}