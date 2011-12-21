package mosaic.region_competition;

import java.util.ArrayList;
import java.util.List;

public class MultipleThresholdImageFunction
{
	LabelImage labelImage;
	
    List<Pair<Integer, Integer>> m_Thresholds;
    int m_NThresholds; // to not call size() of the vector at each evaluation.
	
	public MultipleThresholdImageFunction() 
	{
		m_NThresholds = 0;
		m_Thresholds = new ArrayList<Pair<Integer,Integer>>();
	}
	
	void SetInputImage(LabelImage labelImage)
	{
		this.labelImage = labelImage;
	}
	
	
	
	/** Values that lie between lower and upper inclusive are inside. */
    void AddThresholdBetween(int lower, int upper) 
    {
//        Modified();
        m_Thresholds.add(new Pair<Integer, Integer>(lower, upper));
        m_NThresholds += 1;
    }
	
    
		/** MultipleThreshold the image at an index position.
         *
         * Returns true if the image intensity at the specified point position
         * satisfies the threshold criteria.  The point is assumed to lie within
         * the image buffer.
         *
         * ImageFunction::IsInsideBuffer() can be used to check bounds before
         * calling the method. */
        boolean EvaluateAtIndex(int index) 
        {
            int value = labelImage.get(index);
            for (int vI = 0; vI < m_NThresholds; vI++) {
                if (m_Thresholds.get(vI).first <= value && value <= m_Thresholds.get(vI).second) {
                    return true;
                }
            }
            return false;
        }

        boolean EvaluateAtIndex(Point p)
		{
			int idx = labelImage.iterator.pointToIndex(p);
			return EvaluateAtIndex(idx);
		}

		/** Get the lower threshold value. */
        //itkGetConstReferenceMacro(Thresholds,std::vector<std::pair<PixelType,PixelType>>);


        void ClearThresholds() {
            m_Thresholds.clear();
            m_NThresholds = 0;
        }


    }
