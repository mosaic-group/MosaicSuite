package mosaic.region_competition;

import java.util.ArrayList;
import java.util.List;

class MultipleThresholdImageFunctionDOUBLE
{
	LabelImage labelImage;
	
    List<Pair<Double, Double>> m_Thresholds;
    int m_NThresholds; // to not call size() of the vector at each evaluation.
	
	public MultipleThresholdImageFunctionDOUBLE(LabelImage aLabelImage) 
	{
		SetInputImage(aLabelImage);
		m_NThresholds = 0;
		m_Thresholds = new ArrayList<Pair<Double,Double>>();
	}
	
	void SetInputImage(LabelImage labelImage)
	{
		this.labelImage = labelImage;
	}
	
	
	/** Values that lie between lower and upper inclusive are inside. */
    void AddThresholdBetween(double lower, double upper) 
    {
        m_Thresholds.add(new Pair<Double, Double>(lower, upper));
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

        void ClearThresholds() {
            m_Thresholds.clear();
            m_NThresholds = 0;
        }
    }





/**
 * Versuch einer generischen implementierung. 
 * T extends comparable, da Number comparable nicht implementiert. 
 * T extends Comparable, Number scheint nicht zu gehen. 
 * interface ParamGetter<T>
 * 
 */
class MultipleThresholdImageFunction<N extends Number & Comparable<N>>
{
	
	public interface ParamGetter<T>
	{
		T getT(int idx);
	}
	
//	LabelImageG<N> labelImage;
	
	ParamGetter<N> getter;
	
    List<Pair<N, N>> m_Thresholds;
    int m_NThresholds; // to not call size() of the vector at each evaluation.
	
	public MultipleThresholdImageFunction(ParamGetter<N> getter) 
	{
		this.getter=getter;
		m_NThresholds = 0;
		m_Thresholds = new ArrayList<Pair<N,N>>();
	}
	
	
	/** Values that lie between lower and upper inclusive are inside. */
    void AddThresholdBetween(N lower, N upper) 
    {
//        Modified();
        m_Thresholds.add(new Pair<N, N>(lower, upper));
        m_NThresholds += 1;
    }
	
    
		/** MultipleThreshold the image at an index position.
         *
         * Returns true if the image intensity at the specified point position
         * satisfies the threshold criteria.  The point is assumed to lie within
         * the image buffer.
         * */
        boolean EvaluateAtIndex(int index) 
        {
//            int value = labelImage.get(index);
            N value = getter.getT(index);
            for (int vI = 0; vI < m_NThresholds; vI++) 
            {
            	N first = m_Thresholds.get(vI).first;
            	N second = m_Thresholds.get(vI).second;
                if (first.compareTo(value)<=0 
                		&& value.compareTo(second) <=0) 
//            	if (m_Thresholds.get(vI).first <= value && value <= m_Thresholds.get(vI).second)
                
                {
                    return true;
                }
            }
            return false;
        }


		/** Get the lower threshold value. */
        //itkGetConstReferenceMacro(Thresholds,std::vector<std::pair<PixelType,PixelType>>);


        void ClearThresholds() {
            m_Thresholds.clear();
            m_NThresholds = 0;
        }

    }

//
///**
// *		use int array instead of Integer ArrayList. no performance gain, though
// */
//class MultipleThresholdImageFunctionARRAY extends MultipleThresholdImageFunction
//{
//	
//	int elementData[];
//	int size;
//
//	public MultipleThresholdImageFunctionARRAY(LabelImage aLabelImage)
//	{
//		super(aLabelImage);
//		size=0;
//		elementData = new int[2];
//	}
//	
//	
//    void AddThresholdBetween(int lower, int upper) 
//    {
//        add(lower, upper);
//    }
//	
//    boolean EvaluateAtIndex(int index) 
//    {
//        int value = labelImage.get(index);
//        for (int i = 0; i < size;) {
//            if (elementData[i++] <= value && value <= elementData[i++]) {
//                return true;
//            }
//        }
//        return false;
//    }
//	
//    public boolean add(int lower, int upper) {
//    	ensureCapacity(size + 2);
//    	elementData[size++] = lower;
//    	elementData[size++] = upper;
//    	return true;
//        }
//	
//    public void ensureCapacity(int minCapacity) 
//    {
//    	int oldCapacity = elementData.length;
//    	if(minCapacity > oldCapacity) 
//    	{
//    	    int oldData[] = elementData;
//    	    int newCapacity = (oldCapacity * 3)/2 + 1;
//        	    if (newCapacity < minCapacity)
//        	    	newCapacity = minCapacity;
//                // minCapacity is usually close to size, so this is a win:
//                elementData = Arrays.copyOf(elementData, newCapacity);
//    	}
//	}
//	
//}





