package mosaic.region_competition.RC;


public class LabelStatistics {

    public int label = 0; // absolute label
    public int count; // number of pixels of label
    public double mean = 0; // mean of intensity
    public double var = 0; // variance of intensity
    public double median = 0;
    public double mean_pos[];

    /**
     * Create a label that store information
     *
     * @param label id of the label
     * @param dim dimensions of the problem
     */

    public LabelStatistics(int label, int dim) {
        this.label = label;
        mean_pos = new double[dim];
    }

    @Override
    public String toString() {
        return "L: " + label + " count: " + count + " " + " mean: " + mean;
    }

}
