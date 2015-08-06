package mosaic.region_competition.topology;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import mosaic.core.utils.Connectivity;
import mosaic.core.utils.Point;
import mosaic.region_competition.LabelImageRC;

public class TopologicalNumberImageFunction
{
	
	Connectivity TFGConnectivity;
	Connectivity TBGConnectivity;
	
	UnitCubeCCCounter m_ForegroundUnitCubeCCCounter;
	UnitCubeCCCounter m_BackgroundUnitCubeCCCounter;
	
	protected boolean m_ComputeForegroundTN;
	protected boolean m_ComputeBackgroundTN;

	protected char[] m_SubImage;		// binary subimage (switching fg/bg)
	protected Point[] m_Offsets;		// maps indexes to Points
	protected int[] m_DataSubImage;		// cached input image

	protected int m_IgnoreLabel;


	LabelImageRC labelImage;
	protected int dimension;
	protected int imageSize;

	final int Zero = 0;

	public TopologicalNumberImageFunction(LabelImageRC aLabelImage, Connectivity aTFGConnectivity, Connectivity aTBGConnectivity) 
	{
		this.TFGConnectivity = aTFGConnectivity;
		this.TBGConnectivity = aTBGConnectivity;
		
		this.m_ForegroundUnitCubeCCCounter = new UnitCubeCCCounter(TFGConnectivity);
		this.m_BackgroundUnitCubeCCCounter = new UnitCubeCCCounter(TBGConnectivity);
		
		this.labelImage=aLabelImage;
		this.dimension=aLabelImage.getDim();

        m_IgnoreLabel = labelImage.forbiddenLabel;
        imageSize = TFGConnectivity.GetNeighborhoodSize();
        m_DataSubImage = new int[imageSize];

        m_SubImage = new char[imageSize];
        m_Offsets = new Point[imageSize];
        
        initOffsets();
	}
	
	protected void initOffsets()
	{
        // allocate points
        for (int i=0; i<imageSize; i++)
        {
        	m_Offsets[i] = new Point(dimension);
        }

        // get the ofs for the whole neighborhood.
        for (int i = 0; i < imageSize; ++i) 
        {
        	m_Offsets[i]=TFGConnectivity.ofsIndexToPoint(i);
        }
	}
	
	protected void readImageData(Point p)
	{
        for (int i = 0; i < imageSize; ++i) 
        {
        	//TODO CubeIterator at Connectivity?
            m_DataSubImage[i] = labelImage.getLabelAbs(p.add(m_Offsets[i]));
            if (m_DataSubImage[i] == m_IgnoreLabel) {
            	m_DataSubImage[i] = Zero;
            }
        }
	}
	
	/**
	 * @param index
	 * @return			List(AbjacentLabel, (nFGconnected, nBGconnected))
	 */
    public LinkedList<TopologicalNumberResult> EvaluateAdjacentRegionsFGTNAtIndex(Point index) 
    {

    	LinkedList<TopologicalNumberResult> vTNvector = new LinkedList<TopologicalNumberResult>();
        Set<Integer> vAdjacentLabels = new HashSet<Integer>();
        
        readImageData(index);
        
        for (int vLinearIndex:TFGConnectivity.itOfsInt())
        {
            if (m_DataSubImage[vLinearIndex] != Zero) {
                vAdjacentLabels.add(m_DataSubImage[vLinearIndex]);
            }
        }

        for (int vLabelsIt : vAdjacentLabels)
        {
            for (int i = 0; i < imageSize; ++i) 
            {
                m_SubImage[i] = (char)((m_DataSubImage[i] == vLabelsIt) ? 255 : 0);
            }
            int middle = imageSize / 2;
            m_SubImage[middle] = 0;
            
            TopologicalNumberPair vFGBGTopoPair = new TopologicalNumberPair(0, 0);
            
            // Topological number in the foreground
			m_ForegroundUnitCubeCCCounter.SetImage(m_SubImage);
            vFGBGTopoPair.FGNumber = m_ForegroundUnitCubeCCCounter.connectedComponents();

            // Invert the sub-image
            for (int bit = 0; bit < middle; ++bit) {
                m_SubImage[bit] = (char)(255 - m_SubImage[bit]);
            }
            for (int bit = middle; bit < imageSize - 1; bit++) {
                m_SubImage[bit + 1] = (char)(255 - m_SubImage[bit + 1]);
            }

            
            // Topological number in the background
            
            m_BackgroundUnitCubeCCCounter.SetImage(m_SubImage);
            vFGBGTopoPair.BGNumber = m_BackgroundUnitCubeCCCounter.connectedComponents();
            vTNvector.add(new TopologicalNumberResult(vLabelsIt, vFGBGTopoPair));
            
        }
        return vTNvector;
    }
    
    
    public static class TopologicalNumberPair
    {
    	public int FGNumber;
    	public int BGNumber;
    	
    	public TopologicalNumberPair(int fg, int bg)
		{
			this.FGNumber = fg;
			this.BGNumber = bg;
		}
    }
    
    public static class TopologicalNumberResult
    {
    	public int label;
    	public TopologicalNumberPair topologicalNumberPair;
    	
    	public TopologicalNumberResult(int label, TopologicalNumberPair tn)
		{
    		this.label = label;
			this.topologicalNumberPair = tn; 
		}
    	
    	TopologicalNumberResult(int label, int fg, int bg)
    	{
    		this.label = label; 
    		this.topologicalNumberPair = new TopologicalNumberPair(fg, bg);
    	}
    }

}
