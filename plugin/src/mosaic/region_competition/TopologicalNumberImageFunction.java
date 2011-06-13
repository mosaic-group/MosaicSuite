package mosaic.region_competition;

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
  	
	public TopologicalNumberImageFunction(Connectivity aTFGConnectivity, Connectivity aTBGConnectivity) 
	{
		this.TFGConnectivity = aTFGConnectivity;
		this.TBGConnectivity = aTBGConnectivity;

        m_IgnoreLabel = LabelImage.forbiddenLabel;
        int vImageSize = TFGConnectivity.GetNeighborhoodSize();

        m_SubImage = new char[vImageSize];
        m_Offsets = new Point[vImageSize];
        m_DataSubImage = new int[vImageSize];

        // Get the sub-image
        for (int i = 0; i < vImageSize; ++i) {
            int remainder = i;

            // Get current offset
            for (int j = 0; j < TFGConnectivity.Dimension(); ++j) {
                m_Offsets[i].x[j] = remainder % 3;
                remainder -= m_Offsets[i].x[j];
                remainder /= 3;
                m_Offsets[i].x[j]--;
            }
        }
    
	}
	
//    template<typename TImage, typename TFGConnectivity, typename TBGConnectivity >
//    std::pair<unsigned int, unsigned int>
//    TopologicalNumberImageFunction<TImage, TFGConnectivity, TBGConnectivity>
	
//TODO arguments?
//    ::Evaluate(PointType const & point) const {
	Pair<Integer, Integer> Evaluate(Point point) {
        Point index;
        ConvertPointToNearestIndex(point, index);
        return EvaluateAtIndex(index);
    }

//    std::vector<std::pair<unsigned int, std::pair<unsigned int, unsigned int> > >
//    TopologicalNumberImageFunction<TImage, TFGConnectivity, TBGConnectivity>
//    ::EvaluateAdjacentRegionsFGTNAtIndex(IndexType const & index) const {
    void EvaluateAdjacentRegionsFGTNAtIndex(Point index) 
    {

    	List<Pair<Integer, Pair<Integer, Integer>> > vTNvector;

        int imageSize =TFGConnectivity.GetNeighborhoodSize(); //TFGConnectivity::GetInstance().GetNeighborhoodSize();
        Set<Integer> vAdjacentLabels;


        for (int i = 0; i < imageSize; ++i) {
            m_DataSubImage[i] = abs(this->GetInputImage()->GetPixel(index + m_Offsets[i]));
            if (m_DataSubImage[i] == m_IgnoreLabel) {
                m_DataSubImage[i] = itk::NumericTraits<typename TImage::PixelType>::Zero;
            }


//            if(m_DataSubImage[i] != itk::NumericTraits<typename TImage::PixelType>::Zero) {
//                vAdjacentLabels.insert(m_DataSubImage[i]);
//            }
        }
        
        //TODO: make a member for the neighbor array; 
        //      calculate the linear arrays only once in the constructor or so.
        static const OffsetType * const vFGNeighbors =
                TFGConnectivity::GetInstance().GetNeighborsITKOffsets();
        static const unsigned int vFGNeighborhoodsize =
                TFGConnectivity::GetInstance().GetNumberOfNeighbors();

        for (unsigned int vN = 0; vN < vFGNeighborhoodsize; vN++) {
            int vLinearIndex = 0;
            int vBase3 = 1;
            for (unsigned int vDim = 0; vDim < TFGConnectivity::Dimension; ++vDim) {
                vLinearIndex += (vFGNeighbors[vN][vDim]+1) * vBase3;
                vBase3 *= 3;
            }
//            std::cout << "offset: " << vFGNeighbors[vN] << " corresponding index = " << vLinearIndex << std::endl;
            if (m_DataSubImage[vLinearIndex] != itk::NumericTraits<typename TImage::PixelType>::Zero) {
                vAdjacentLabels.insert(m_DataSubImage[vLinearIndex]);
            }
        }


        typedef typename AdjacentLabelsSetType::iterator AdjacentLabelsSetIteratorType;
        AdjacentLabelsSetIteratorType vLabelsIt = vAdjacentLabels.begin();
        AdjacentLabelsSetIteratorType vLabelsEnd = vAdjacentLabels.end();

        for (; vLabelsIt != vLabelsEnd; ++vLabelsIt) {
            for (int i = 0; i < imageSize; ++i) {
                m_SubImage[i] = (m_DataSubImage[i] == vLabelsIt) ? 255 : 0;
            }

            int middle = imageSize / 2;

            m_SubImage[middle] = 0;

//            if (index[0] >= 69 && index[1] >= 174 && index[0] <= 70 && index[1] <= 175) {
//                ""
//                std::cout << index << ", label to check: " << *vLabelsIt
//                        << ", topo num = " << m_ForegroundUnitCubeCCCounter() << std::endl;
//                std::cout << "data image: ";
//                for (int j = 0; j < imageSize; j++) {
//                    std::cout << m_DataSubImage[j] << ", ";
//                }
//                std::cout << std::endl;
//                std::cout << "binary image: ";
//                for (int j = 0; j < imageSize; j++) {
//                    std::cout << (int) m_SubImage[j] << ", ";
//                }
//                std::cout << std::endl;
//
//            }

            // Topological number in the foreground

            FGandBGTopoNbPairType vFGBGTopoPair;
            m_ForegroundUnitCubeCCCounter.SetImage(m_SubImage, m_SubImage + imageSize);

            vFGBGTopoPair.first = m_ForegroundUnitCubeCCCounter();

            // Invert the sub-image
            for (int bit = 0; bit < middle; ++bit) {
                m_SubImage[bit] = 255 - m_SubImage[bit];
            }
            for (int bit = middle; bit < imageSize - 1; ++bit) {
                m_SubImage[bit + 1] = 255 - m_SubImage[bit + 1];
            }

            // Topological number in the background
            m_BackgroundUnitCubeCCCounter.SetImage(m_SubImage,
                    m_SubImage + TBGConnectivity::GetInstance().GetNeighborhoodSize());

            vFGBGTopoPair.second = m_BackgroundUnitCubeCCCounter();
            
            vTNvector.push_back(new Pair<Integer, FGandBGTopoNbPairType > (
                    vLabelsIt, vFGBGTopoPair));

        }
        return vTNvector;
    }
	
	
	
	
	
	
	
	
	

}


//
//        /**
//         * @name Evaluation functions
//         *
//         * These functions evaluate the topological number at the index.
//         */
//        //@{
//        std::pair<unsigned int, unsigned int>
//        Evaluate(PointType const & point) const;
//
//        std::pair<unsigned int, unsigned int>
//        EvaluateAtIndex(IndexType const & index) const;
//
//        std::pair<unsigned int, unsigned int>
//        EvaluateAtContinuousIndex(ContinuousIndexType const & contIndex) const;
//
//        typedef std::pair<unsigned int, unsigned int> FGandBGTopoNbPairType;
//
//        typedef std::vector<std::pair<unsigned int, FGandBGTopoNbPairType> >
//        ForegroundTopologicalNumbersType;
//
//        ForegroundTopologicalNumbersType
//        EvaluateAdjacentRegionsFGTNAtIndex(IndexType const & index) const;
//
//
//    private:
//        static UnitCubeCCCounter< TFGConnectivity > m_ForegroundUnitCubeCCCounter;
//        static UnitCubeCCCounter< TBGConnectivity > m_BackgroundUnitCubeCCCounter;
//
//    };
