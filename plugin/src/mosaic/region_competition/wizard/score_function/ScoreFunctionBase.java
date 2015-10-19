package mosaic.region_competition.wizard.score_function;

import java.util.HashMap;
import java.util.HashSet;

import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.LabelImage;
import mosaic.region_competition.LabelStatistics;

public abstract class ScoreFunctionBase implements ScoreFunction {
    public int createStatistics(LabelImage lirc, IntensityImage intensityImage, HashMap<Integer, LabelStatistics> labelMap) {
        labelMap.clear();

        final HashSet<Integer> usedLabels = new HashSet<Integer>();

        final int size = lirc.getSize();
        for (int i = 0; i < size; i++) {
            final int absLabel = lirc.getLabelAbs(i);

            if (!lirc.isForbiddenLabel(absLabel) /* && absLabel != bgLabel */) {
                usedLabels.add(absLabel);

                LabelStatistics stats = labelMap.get(absLabel);
                if (stats == null) {
                    stats = new LabelStatistics(absLabel, lirc.getNumOfDimensions());
                    labelMap.put(absLabel, stats);
                }
                final double val = intensityImage.get(i);
                stats.count++;
                // only sum up, mean and var are computed below
                stats.mean += val;
                stats.var = (stats.var + val * val);
            }
        }

        // if background label do not exist add it
        LabelStatistics stats = labelMap.get(0);
        if (stats == null) {
            stats = new LabelStatistics(0, lirc.getNumOfDimensions());
            labelMap.put(0, stats);
        }

        // now we have in all LabelInformation:
        // in mean the sum of the values, in var the sum of val^2
        for (final LabelStatistics stat : labelMap.values()) {
            final int n = stat.count;
            if (n > 1) {
                final double var = (stat.var - stat.mean * stat.mean / n) / (n - 1);
                stat.var = (var);
                // stat.var = (stat.var - stat.mean*stat.mean / n) / (n-1);
            }
            else {
                stat.var = 0;
            }

            if (n > 0) {
                stat.mean = stat.mean / n;
            }
            else {
                stat.mean = 0.0;
            }

            // Median on start set equal to mean
            stat.median = stat.mean;
        }
        return usedLabels.size();
    }
}
