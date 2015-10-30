package mosaic.core.binarize;


import java.util.ArrayList;
import java.util.List;


/**
 * This class just store intervals and evaluate if a value is in one
 * of this intervals
 *
 * @author Pietro Incardona
 */
class IntervalsListDouble {

    private final List<ThresholdIntervalDouble> m_Thresholds;

    public IntervalsListDouble() {
        m_Thresholds = new ArrayList<ThresholdIntervalDouble>();
    }

    public void AddThresholdBetween(double lower, double upper) {
        m_Thresholds.add(new ThresholdIntervalDouble(lower, upper));
    }

    public boolean Evaluate(double value) {
        for (int vI = m_Thresholds.size() - 1; vI >= 0; --vI) {
            if (m_Thresholds.get(vI).lower <= value && value <= m_Thresholds.get(vI).higher) {
                return true;
            }
        }
        return false;
    }

    public void clearThresholds() {
        m_Thresholds.clear();
    }
    
    private class ThresholdIntervalDouble {
        public final double lower;
        public final double higher;
        
        protected ThresholdIntervalDouble(double lower, double higher) {
            this.lower = lower;
            this.higher = higher;
        }
    }
}
