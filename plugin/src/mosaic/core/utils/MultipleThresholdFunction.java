package mosaic.core.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * This class just store intervals and evaluate if a value is in one
 * of this intervals
 * 
 * @author Stephan Semmler
 *
 */


class MultipleThresholdFunction
{
    protected List<ThresholdInterval> m_Thresholds;
    protected int m_NThresholds; // to not call size() of the vector at each evaluation.
    
    public MultipleThresholdFunction()
	{
		m_NThresholds = 0;
		m_Thresholds = new ArrayList<ThresholdInterval>();
	}
    
	public void AddThreshold(double value)
	{
		AddThresholdBetween(value, value);
	}
    
	public void AddThresholdBetween(double lower, double upper) 
    {
        m_Thresholds.add(new ThresholdInterval(lower, upper));
        m_NThresholds += 1;
    }
	
	public boolean Evaluate(double value)
	{
		for(int vI = 0; vI < m_NThresholds; vI++)
		{
			if(m_Thresholds.get(vI).lower <= value && value <= m_Thresholds.get(vI).higher)
			{
				return true;
			}
		}
		return false;
	}
	
	public void clearThresholds()
	{
		m_NThresholds = 0;
		m_Thresholds.clear();
	}
}

class ThresholdInterval
{
	public double lower; 
	public double higher;
	public ThresholdInterval(double lower, double higher)
	{
		this.lower = lower; 
		this.higher = higher; 
	}
}

