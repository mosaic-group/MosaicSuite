package mosaic.region_competition.utils;

import static mosaic.core.imageUtils.images.LabelImage.BGLabel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;

public class LabelStatisticToolbox {
    private static final Logger logger = Logger.getLogger(LabelStatisticToolbox.class);
    
    static public int initStatistics(LabelImage iLabelImage, IntensityImage iIntensityImage, HashMap<Integer, LabelStatistics> iLabelStatistics) {
        // First create all LabelStatistics for each found label and calculate values needed for later variance/mean calculations
        int maxUsedLabel = 0;
        for (int i = 0; i < iLabelImage.getSize(); ++i) {
            final int absLabel = iLabelImage.getLabelAbs(i);

            if (!iLabelImage.isBorderLabel(absLabel)) {
                if (maxUsedLabel < absLabel) maxUsedLabel = absLabel;

                LabelStatistics stats = iLabelStatistics.get(absLabel);
                if (stats == null) {
                    stats = new LabelStatistics(absLabel, iLabelImage.getNumOfDimensions());
                    iLabelStatistics.put(absLabel, stats);
                }
                final double val = iIntensityImage.get(i);
                stats.iSum += val;
                stats.iSumOfSq += val*val;
                stats.iLabelCount++;
            }
        }

        // If background label do not exist add it to collection
        LabelStatistics stats = iLabelStatistics.get(LabelImage.BGLabel);
        if (stats == null) {
            stats = new LabelStatistics(LabelImage.BGLabel, iLabelImage.getNumOfDimensions());
            iLabelStatistics.put(LabelImage.BGLabel, stats);
        }

        // Finally - calculate variance, median and mean for each found label
        for (LabelStatistics stat : iLabelStatistics.values()) {
            int n = stat.iLabelCount;
            stat.iMeanIntensity = n > 0 ? (stat.iSum / n) : 0;
            stat.iVarIntensity = calculateVariance(stats.iSumOfSq,  stat.iMeanIntensity, n);
            // Median on start set equal to mean
            stat.iMedianIntensity = stat.iMeanIntensity;
        }
        
        return maxUsedLabel;
    }
    
    static public void updateLabelStatistics(double aIntensity, int aFromLabelIdx, int aToLabelIdx, HashMap<Integer, LabelStatistics> iLabelStatistics) {
        final LabelStatistics toStats = iLabelStatistics.get(aToLabelIdx);
        final LabelStatistics fromStats = iLabelStatistics.get(aFromLabelIdx);

        toStats.iSumOfSq += aIntensity*aIntensity;
        fromStats.iSumOfSq -= aIntensity*aIntensity;
        toStats.iSum += aIntensity;
        fromStats.iSum -= aIntensity;
        
        toStats.iLabelCount++;
        fromStats.iLabelCount--;
        
        // Update mean/var from updatet sums and label count
        toStats.iMeanIntensity = (toStats.iSum ) / (toStats.iLabelCount);
        fromStats.iMeanIntensity = (fromStats.iLabelCount > 0) ? (fromStats.iSum ) / (fromStats.iLabelCount) : 0;
        toStats.iVarIntensity = calculateVariance(toStats.iSumOfSq, toStats.iMeanIntensity, toStats.iLabelCount);
        fromStats.iVarIntensity = calculateVariance(fromStats.iSumOfSq, fromStats.iMeanIntensity, fromStats.iLabelCount);
    }
    
    /**
     * Removes from statistics labels with 'count == 0'
     */
    static public void removeEmptyStatistics(HashMap<Integer, LabelStatistics> iLabelStatistics) {
        final Iterator<Entry<Integer, LabelStatistics>> labelStatsIt = iLabelStatistics.entrySet().iterator();

        while (labelStatsIt.hasNext()) {
            final Entry<Integer, LabelStatistics> entry = labelStatsIt.next();
            if (entry.getValue().iLabelCount == 0) {
                if (entry.getKey() != BGLabel) {
                    labelStatsIt.remove();
                    continue;
                }
                logger.error("Tried to remove background label from label statistics!");
            }
        }
    }
    
    static private double calculateVariance(double aSumSq, double aMean, int aN) {
        return (aN < 2) ? 0 : (aSumSq - aN * aMean * aMean) / (aN - 1.0);
    }
}
