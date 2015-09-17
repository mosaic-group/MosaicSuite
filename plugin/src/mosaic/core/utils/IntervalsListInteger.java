package mosaic.core.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * This class just store intervals and evaluate if a value is in one
 * of this intervals
 * 
 * @author Pietro Incardona
 *
 */


public class IntervalsListInteger
{
    protected List<ThresholdIntervalInteger> m_Thresholds;
    protected int m_NThresholds; // to not call size() of the vector at each evaluation.
    
    public IntervalsListInteger()
	{
		m_NThresholds = 0;
		m_Thresholds = new ArrayList<ThresholdIntervalInteger>();
	}
    
	public void AddThresholdBetween(int lower, int upper) 
    {
        m_Thresholds.add(new ThresholdIntervalInteger(lower, upper));
        m_NThresholds += 1;
    }
	
	public boolean Evaluate(int value)
	{
		for (int vI = 0; vI < m_NThresholds; vI++)
		{
			if (m_Thresholds.get(vI).lower <= value && value <= m_Thresholds.get(vI).higher)
			{
				return true;
			}
		}
		return false;
	}
}

class ThresholdIntervalInteger
{
	public int lower; 
	public int higher;
	public ThresholdIntervalInteger(int lower, int higher)
	{
		this.lower = lower; 
		this.higher = higher; 
	}
}

