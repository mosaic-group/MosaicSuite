package mosaic.region_competition.wizard.score_function;


import java.util.Collection;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import mosaic.core.utils.IntensityImage;
import mosaic.plugins.Region_Competition;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.Settings;


// Score function try to find out the best segmentation with PC on all area selected
// use setPS() to select PS

public class ScoreFunctionRCvol implements ScoreFunction {

    private int Area[];
    private final String[] file;

    private final IntensityImage i[];
    private final LabelImageRC l[];
    private final Settings s;

    public ScoreFunctionRCvol(IntensityImage i_[], LabelImageRC l_[], Settings s_) {
        i = i_;
        l = l_;

        s = s_;
        file = new String[l.length];
    }

    public LabelImageRC getLabel(int im) {
        return l[im];
    }

    public void setArea(int a[]) {
        Area = a;
    }

    public int Area(LabelImageRC l) {
        int count = 0;
        // double a1 = 0.0;
        // double a2 = 0.0;

        final Collection<LabelInformation> li = l.getLabelMap().values();

        // a1 = ((LabelInformation)li.toArray()[0]).mean;

        for (int i = 1; i < li.toArray().length; i++) {
            /* a2 += ((LabelInformation)li.toArray()[i]).mean*((LabelInformation)li.toArray()[i]).count; */
            count += ((LabelInformation) li.toArray()[i]).count;
        }
        // a2 /= count;

        return count;
    }

    @Override
    public Settings createSettings(Settings s, double pop[]) {
        final Settings st = new Settings(s);

        st.m_GaussPSEnergyRadius = (int) pop[0];
        st.m_BalloonForceCoeff = (float) pop[1];

        return st;
    }

    @Override
    public double valueOf(double[] x) {
        double result = 0.0;

        s.m_GaussPSEnergyRadius = (int) x[0];
        s.m_BalloonForceCoeff = (float) x[1];
        if (s.m_GaussPSEnergyRadius > 2.0) {
            s.m_EnergyFunctional = EnergyFunctionalType.e_PS;
        }
        else {
            s.m_EnergyFunctional = EnergyFunctionalType.e_PC;
        }

        // write the settings
        Region_Competition.getConfigHandler().SaveToFile(IJ.getDirectory("temp") + "RC_" + x[0] + "_" + x[1], s);

        for (int im = 0; im < i.length; im++) {
            IJ.run(i[im].imageIP, "Region Competition", "config=" + IJ.getDirectory("temp") + "RC_" + x[0] + "_" + x[1] + "  " + "output=" + IJ.getDirectory("temp") + "RC_" + x[0] + "_" + x[1] + "_"
                    + im + "_" + ".tif" + " normalize=false");

            // Read Label Image

            final Opener o = new Opener();
            file[im] = new String(IJ.getDirectory("temp") + "RC_" + x[0] + "_" + x[1] + "_" + im + "_" + ".tif");
            final ImagePlus ip = o.openImage(file[im]);

            l[im].initWithIP(ip);
            l[im].createStatistics(i[im]);

            // Scoring

            // result += Math.abs(l[im].getLabelMap().size()-off[im]);

            int count = 0;
            // double a1 = 0.0;
            // double a2 = 0.0;

            final Collection<LabelInformation> li = l[im].getLabelMap().values();

            // a1 = ((LabelInformation)li.toArray()[0]).mean;

            for (int i = 1; i < li.toArray().length; i++) {
                /* a2 += ((LabelInformation)li.toArray()[i]).mean*((LabelInformation)li.toArray()[i]).count; */
                count += ((LabelInformation) li.toArray()[i]).count;
            }
            // a2 /= count;

            result += (count - Area[im]) * (count - Area[im]);

            // result += 10.0/Math.abs(a1 - a2);
        }

        return result;
    }

    @Override
    public boolean isFeasible(double[] x) {
        int minSz = Integer.MAX_VALUE;
        for (final LabelImageRC lbt : l) {
            for (final int d : lbt.getDimensions()) {
                if (d < minSz) {
                    minSz = d;
                }
            }
        }

        if (x[0] <= 0.0 || x[1] <= 0.0 || x[0] > minSz / 4 || x[1] > 1.0) {
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
        return TypeImage.FILENAME;
    }

    @Override
    public ImagePlus[] getImagesIP() {
        return null;
    }

    @Override
    public String[] getImagesString() {
        return file;
    }

    @Override
    public double[] getAMean(Settings s) {
        final double[] aMean = new double[2];

        aMean[1] = s.m_BalloonForceCoeff;
        aMean[0] = s.m_GaussPSEnergyRadius;

        return aMean;
    }
}
