package mosaic.region_competition.wizard.score_function;


import ij.ImagePlus;

import java.util.HashMap;

import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.region_competition.RC.LabelStatistics;
import mosaic.region_competition.RC.Settings;
import mosaic.region_competition.initializers.MaximaBubbles;


// Type images
public class ScoreFunctionInit extends ScoreFunctionBase {

    private final int off[];
    private final int inc_step[];
    private int r_t = 8;
    private int rad = 8;

    private final IntensityImage i[];
    private final LabelImage l[];

    public ScoreFunctionInit(IntensityImage i_[], LabelImage l_[], int r_t_, int rad_) {
        i = i_;
        l = l_;
        r_t = r_t_;
        rad = rad_;
        off = new int[i_.length];
        inc_step = new int[i_.length];
    }

    /*
     * Set the number of objects
     */
    public void setObject(int i, int off_) {
        off[i] = off_;
        if (2 * off_ / 5 >= 1) {
            inc_step[i] = 2 * off_ / 5;
        }
        else {
            inc_step[i] = 1;
        }
    }

    @Override
    public Settings createSettings(Settings s, double pop[]) {
        final Settings st = new Settings(s);

        st.l_Sigma = pop[0];
        st.l_Tolerance = pop[1];

        return st;
    }

    @Override
    public double valueOf(double[] x) {
        final double sigma = x[0];
        final double tol = x[1];

        double result = 0.0;

        for (int im = 0; im < i.length; im++) {
            l[im].initZero();
            final MaximaBubbles b = new MaximaBubbles(i[im], l[im], sigma, tol, rad, r_t);
            b.initialize();
            HashMap<Integer, LabelStatistics> labelMap = new HashMap<Integer, LabelStatistics>();
            final int c = createStatistics(l[im], i[im], labelMap);
            labelMap.remove(0); // remove background
            for (final LabelStatistics lb : labelMap.values()) {
                result += 2.0 * Math.abs(lb.count - l[im].getSize() / 4.0 / off[im]) / l[im].getSize();
            }

            result += Math.abs(c - (off[im] + 1)) /** l[im].getSize() */
            ;
            l[im].initBoundary();
            l[im].initContour();
            result += ScoreFunctionRCsmo.SmoothNorm(l[im]) / 8;

            // l[im].show("test",10);
        }

        return result;
    }

    @Override
    public boolean isFeasible(double[] x) {
        if (x[0] <= 0.0 || x[1] <= 0.0) {
            return false;
        }

        if (x[0] >= 20.0) {
            return false;
        }

        return true;
    }

    @Override
    public void show() {
        for (int im = 0; im < l.length; im++) {
            l[im].show("init", 255);
        }
    }

    @Override
    public TypeImage getTypeImage() {
        return TypeImage.IMAGEPLUS;
    }

    @Override
    public ImagePlus[] getImagesIP() {
        final ImagePlus ip[] = new ImagePlus[l.length];

        for (int i = 0; i < l.length; i++) {
            ip[i] = l[i].convert("image", off[i]);
        }

        return ip;
    }

    @Override
    public String[] getImagesString() {
        return null;
    }

    @Override
    public double[] getAMean(Settings s) {
        final double[] aMean = new double[2];
        aMean[0] = s.l_Sigma;
        aMean[1] = s.l_Tolerance;
        return aMean;
    }
}
