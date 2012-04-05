package mosaic.region_competition;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TopologicalNumberImageFunction
{
	
//  static UnitCubeCCCounter< TFGConnectivity > m_ForegroundUnitCubeCCCounter;
//  static UnitCubeCCCounter< TBGConnectivity > m_BackgroundUnitCubeCCCounter;


  boolean m_ComputeForegroundTN;
  boolean m_ComputeBackgroundTN;

  char[] m_SubImage;
  Point[] m_Offsets;
  int[] m_DataSubImage;

  int m_IgnoreLabel;
	
  Connectivity TFGConnectivity;
  Connectivity TBGConnectivity;
  
  
  UnitCubeCCCounter m_ForegroundUnitCubeCCCounter;
  UnitCubeCCCounter m_BackgroundUnitCubeCCCounter;
  
  LabelImage labelImage;
  int dimension;
  int imageSize;
  
  int Zero = 0; //TODO itk::NumericTraits<typename TImage::PixelType>::Zero
  
  	
	public TopologicalNumberImageFunction(LabelImage aLabelImage, Connectivity aTFGConnectivity, Connectivity aTBGConnectivity) 
	{
		this.TFGConnectivity = aTFGConnectivity;
		this.TBGConnectivity = aTBGConnectivity;
		
		//TODO!!!!!!! is this correct? switching ok?
		this.m_ForegroundUnitCubeCCCounter = new UnitCubeCCCounter(TFGConnectivity, TBGConnectivity);
		this.m_BackgroundUnitCubeCCCounter = new UnitCubeCCCounter(TBGConnectivity, TFGConnectivity);
		
		this.labelImage=aLabelImage;
		this.dimension=TFGConnectivity.getDim();

        m_IgnoreLabel = labelImage.forbiddenLabel;
        imageSize = TFGConnectivity.GetNeighborhoodSize();

        m_SubImage = new char[imageSize];
        m_DataSubImage = new int[imageSize];
        m_Offsets = new Point[imageSize];		// maps indexes to Points
        
        initOffsets();
	}
	
	private void initOffsets()
	{
        // allocate points
        for(int i=0; i<imageSize; i++)
        {
        	m_Offsets[i] = new Point(dimension);
        }

        // get the ofs for the whole neighborhood.
        //TODO could be done in Conn
        for (int i = 0; i < imageSize; ++i) {

//            // Get current offset
//            int remainder = i;
//            for (int j = 0; j < TFGConnectivity.getDim(); ++j) {
//                m_Offsets[i].x[j] = remainder % 3;
//                remainder -= m_Offsets[i].x[j];
//                remainder /= 3;
//                m_Offsets[i].x[j]--;
//            }
        	m_Offsets[i]=TFGConnectivity.ofsIndexToPoint(i);
        }
	}
	
	private void readImageData(Point p)
	{
        for (int i = 0; i < imageSize; ++i) {
        	//TODO CubeIterator at Connectivity?
            m_DataSubImage[i] = labelImage.getAbs(p.add(m_Offsets[i]));
            if (m_DataSubImage[i] == m_IgnoreLabel) {
            	m_DataSubImage[i] = Zero;
            }
        }
	}
	
	/**
	 * @param index
	 * @return			List(AbjacentLabel, (nFGconnected, nBGconnected))
	 */
    public List<Pair<Integer, Pair<Integer, Integer>>> EvaluateAdjacentRegionsFGTNAtIndex(Point index) 
    {

    	List<Pair<Integer, Pair<Integer, Integer>> > vTNvector = new LinkedList<Pair<Integer,Pair<Integer,Integer>>>();
//        int imageSize = TFGConnectivity.GetNeighborhoodSize(); //TFGConnectivity::GetInstance().GetNeighborhoodSize();
        Set<Integer> vAdjacentLabels = new HashSet<Integer>();
        
        readImageData(index);
        
//        UnitCubeCCCounter unitCubeCCCounter = new UnitCubeCCCounter(TFGConnectivity, TBGConnectivity);
//        for (Point p: TFGConnectivity) {
//            int vLinearIndex = unitCubeCCCounter.pointToOfs(p);
//            if (m_DataSubImage[vLinearIndex] != Zero) {
//                vAdjacentLabels.add(m_DataSubImage[vLinearIndex]);
//            }
//        }
        for(int vLinearIndex:TFGConnectivity.itOfsInt())
        {
            if (m_DataSubImage[vLinearIndex] != Zero) {
                vAdjacentLabels.add(m_DataSubImage[vLinearIndex]);
            }
        }

        for(int vLabelsIt : vAdjacentLabels)
        {
            for (int i = 0; i < imageSize; ++i) 
            {
                m_SubImage[i] = (char)((m_DataSubImage[i] == vLabelsIt) ? 255 : 0);
            }
            int middle = imageSize / 2;
            m_SubImage[middle] = 0;

            Pair<Integer, Integer> vFGBGTopoPair = new Pair<Integer, Integer>(0, 0);
            
            // Topological number in the foreground
			m_ForegroundUnitCubeCCCounter.SetImage(m_SubImage);
            vFGBGTopoPair.first = m_ForegroundUnitCubeCCCounter.connectedComponents();

            // Invert the sub-image
            for (int bit = 0; bit < middle; ++bit) {
                m_SubImage[bit] = (char)(255 - m_SubImage[bit]);
            }
            for (int bit = middle; bit < imageSize - 1; bit++) {
                m_SubImage[bit + 1] = (char)(255 - m_SubImage[bit + 1]);
            }

            
            // Topological number in the background
            
//            UnitCubeCCCounter m_BackgroundUnitCubeCCCounter = new UnitCubeCCCounter(TFGConnectivity, TBGConnectivity);
//            UnitCubeCCCounter m_BackgroundUnitCubeCCCounter = new UnitCubeCCCounter(TBGConnectivity, TFGConnectivity);

            m_BackgroundUnitCubeCCCounter.SetImage(m_SubImage);
            vFGBGTopoPair.second = m_BackgroundUnitCubeCCCounter.connectedComponents();

            vTNvector.add(new Pair<Integer, Pair<Integer, Integer>>(vLabelsIt, vFGBGTopoPair));
        }
        return vTNvector;
    }

}
