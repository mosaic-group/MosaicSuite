package mosaic.region_competition.wizard.score_function;


import ij.ImagePlus;
import mosaic.region_competition.RC.Settings;
import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;


// Score function try to find out the best initialization on all area selected

public interface ScoreFunction extends IObjectiveFunction {

    public enum TypeImage {
        FILENAME, IMAGEPLUS
    }

    void show();
    TypeImage getTypeImage();
    ImagePlus[] getImagesIP();
    String[] getImagesString();
    Settings createSettings(Settings s, double pop[]);
    double[] getAMean(Settings s);
}
