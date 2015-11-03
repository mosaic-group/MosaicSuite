package mosaic.region_competition.topology;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mosaic.core.image.Connectivity;
import mosaic.core.image.LabelImage;
import mosaic.core.image.Point;


public class TopologicalNumberImageFunction {

    private final Connectivity TFGConnectivity;

    private final UnitCubeConnectedComponenetsCounter m_ForegroundUnitCubeCCCounter;
    private final UnitCubeConnectedComponenetsCounter m_BackgroundUnitCubeCCCounter;

    private final char[] m_SubImage; // binary subimage (switching fg/bg)
    private final Point[] m_Offsets; // maps indexes to Points
    private final int[] m_DataSubImage; // cached input image

    private final LabelImage labelImage;
    private final int imageSize;

    private final static int ValueForForbiddenPoints = 0;

    public TopologicalNumberImageFunction(LabelImage aLabelImage) {
        this.TFGConnectivity = aLabelImage.getConnFG();

        this.m_ForegroundUnitCubeCCCounter = new UnitCubeConnectedComponenetsCounter(TFGConnectivity);
        this.m_BackgroundUnitCubeCCCounter = new UnitCubeConnectedComponenetsCounter(aLabelImage.getConnBG());

        this.labelImage = aLabelImage;

        imageSize = TFGConnectivity.getNeighborhoodSize();
        m_DataSubImage = new int[imageSize];
        m_SubImage = new char[imageSize];
        m_Offsets = new Point[imageSize];

        initOffsets();
    }

    private void initOffsets() {
//        // allocate points
//        for (int i = 0; i < imageSize; i++) {
//            m_Offsets[i] = new Point(new int[labelImage.getNumOfDimensions()]);
//        }

        // get the ofs for the whole neighborhood.
        for (int i = 0; i < imageSize; ++i) {
            m_Offsets[i] = TFGConnectivity.toPoint(i);
        }
    }

    private void readImageData(Point p) {
        for (int i = 0; i < imageSize; ++i) {
            // TODO CubeIterator at Connectivity?
            m_DataSubImage[i] = labelImage.getLabelAbs(p.add(m_Offsets[i]));
            if (labelImage.isForbiddenLabel(m_DataSubImage[i])) {
                m_DataSubImage[i] = ValueForForbiddenPoints;
            }
        }
    }

    /**
     * @param index
     * @return List(AbjacentLabel, (nFGconnected, nBGconnected))
     */
    public List<TopologicalNumberResult> EvaluateAdjacentRegionsFGTN(Point index) {
        readImageData(index);

        final Set<Integer> vAdjacentLabels = new HashSet<Integer>();
        for (final int vLinearIndex : TFGConnectivity.itOfsInt()) {
            if (m_DataSubImage[vLinearIndex] != ValueForForbiddenPoints) {
                vAdjacentLabels.add(m_DataSubImage[vLinearIndex]);
            }
        }
        
        final int middle = imageSize / 2;

        final List<TopologicalNumberResult> vTNvector = new ArrayList<TopologicalNumberResult>(vAdjacentLabels.size());
        for (final int vLabelsIt : vAdjacentLabels) {
            for (int i = 0; i < imageSize; ++i) {
                m_SubImage[i] = (char) ((m_DataSubImage[i] == vLabelsIt) ? 255 : 0);
            }
            m_SubImage[middle] = 0;
            // Topological number in the foreground
            int FGNumber = m_ForegroundUnitCubeCCCounter.SetImage(m_SubImage).getNumberOfConnectedComponents();

            // Invert the sub-image
            for (int bit = 0; bit < imageSize; ++bit) {
                m_SubImage[bit] = (char) (255 - m_SubImage[bit]);
            }
            m_SubImage[middle] = 0;
            // Topological number in the background
            int BGNumber = m_BackgroundUnitCubeCCCounter.SetImage(m_SubImage).getNumberOfConnectedComponents();
            
            vTNvector.add(new TopologicalNumberResult(vLabelsIt, FGNumber, BGNumber));

        }
        return vTNvector;
    }

    public static class TopologicalNumberResult {
        public final int label;
        public int FGNumber;
        public int BGNumber;
        
        protected TopologicalNumberResult(int label, int FGNumber, int BGNumber) {
            this.label = label;
            this.FGNumber = FGNumber;
            this.BGNumber = BGNumber;
        }
    }
}
