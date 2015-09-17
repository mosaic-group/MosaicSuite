package mosaic.region_competition;


public class LabelInformation {

    public int label = 0; // label
    public int count; // number of pixels of label
    public double mean = 0; // mean of intensity
    public double M2 = 0;
    public double var = 0; // variance of intensity
    public double median = 0;
    public double mean_pos[];
    public int dim;

    /**
     * Create a label that store information
     * 
     * @param label id of the label
     * @param dim dimensions of the problem
     */

    LabelInformation(int label, int dim) {
        this.label = label;
        this.dim = dim;
        mean_pos = new double[dim];
    }

    /**
     * Reset all the values of the label
     */

    void reset() {
        label = 0;
        count = 0;
        mean = 0;
        M2 = 0;
        var = 0;
        mean_pos = new double[dim];
    }

    @Override
    public String toString() {
        return "L: " + label + " count: " + count + " " + " mean: " + mean;
    }

}
