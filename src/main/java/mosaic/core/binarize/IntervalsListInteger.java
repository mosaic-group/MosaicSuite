package mosaic.core.binarize;


import java.util.ArrayList;
import java.util.List;


/**
 * This class just store intervals and evaluate if a value is in one
 * of this intervals
 *
 * @author Pietro Incardona
 */
class IntervalsListInteger {

    private final List<ThresholdIntervalInteger> m_Thresholds;

    public IntervalsListInteger() {
        m_Thresholds = new ArrayList<ThresholdIntervalInteger>();
    }

    public void AddThresholdBetween(int lower, int upper) {
        m_Thresholds.add(new ThresholdIntervalInteger(lower, upper));
    }
    
    public void AddOneValThreshold(int aTresholdValue) {
        AddThresholdBetween(aTresholdValue, aTresholdValue);
    }

    public boolean Evaluate(int value) {
        for (int vI = m_Thresholds.size() - 1; vI >= 0; --vI) {
            if (m_Thresholds.get(vI).lower <= value && value <= m_Thresholds.get(vI).higher) {
                return true;
            }
        }
        return false;
    }

    private class ThresholdIntervalInteger {
        public final int lower;
        public final int higher;
        
        protected ThresholdIntervalInteger(int lower, int higher) {
            this.lower = lower;
            this.higher = higher;
        }
    }
}
