package mosaic.region_competition.wizard.score_function;

import ij.ImagePlus;
import fr.inria.optimization.cmaes.fitness.IObjectiveFunction;
import mosaic.region_competition.Settings;

// Score function try to find out the best initialization on all area selected

public interface ScoreFunction extends IObjectiveFunction
{
	abstract void incrementStep();
	abstract void show();
	abstract int getNImg();
	abstract TypeImage getTypeImage();
	abstract ImagePlus[] getImagesIP();
	abstract String[] getImagesString();
	abstract Settings createSettings(Settings s, double pop[]);
	abstract double [] getAMean(Settings s);
}
