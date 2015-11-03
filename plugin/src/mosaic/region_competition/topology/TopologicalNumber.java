package mosaic.region_competition.topology;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mosaic.core.image.Connectivity;
import mosaic.core.image.LabelImage;
import mosaic.core.image.Point;


public class TopologicalNumber {

    private final Connectivity iFgConnectivity;
    private final UnitCubeConnectedComponenetsCounter iFgConnectedComponentsCounter;
    private final UnitCubeConnectedComponenetsCounter iBgConnectedComponentsCounter;

    private final LabelImage iLabelImage;
    private final int iUnitCubeSize;

    private final char[] iOneLabelSubImageFg;
    private final char[] iOneLabelSubImageBg;
    private final Point[] iPointOffsets;
    private final int[] iSubImage;


    private final static int ValueForForbiddenPoints = 0; // background

    public TopologicalNumber(LabelImage aLabelImage) {
        iFgConnectivity = aLabelImage.getConnFG();
        iFgConnectedComponentsCounter = new UnitCubeConnectedComponenetsCounter(iFgConnectivity);
        iBgConnectedComponentsCounter = new UnitCubeConnectedComponenetsCounter(aLabelImage.getConnBG());

        iLabelImage = aLabelImage;

        iUnitCubeSize = iFgConnectivity.getNeighborhoodSize();
        
        iSubImage = new int[iUnitCubeSize];
        iOneLabelSubImageFg = new char[iUnitCubeSize];
        iOneLabelSubImageBg = new char[iUnitCubeSize];
        
        // Get the offsets for the whole neighborhood
        iPointOffsets = new Point[iUnitCubeSize];
        for (int i = 0; i < iUnitCubeSize; ++i) {
            iPointOffsets[i] = iFgConnectivity.toPoint(i);
        }
    }

    /**
     * Gets sub image (unit cube size) around middle point
     */
    private void getSubImage(Point aMiddlePoint) {
        for (int i = 0; i < iUnitCubeSize; ++i) {
            int label = iLabelImage.getLabelAbs(aMiddlePoint.add(iPointOffsets[i]));
            if (iLabelImage.isForbiddenLabel(label)) {
                label = ValueForForbiddenPoints;
            }
            iSubImage[i] = label;
        }
    }

    /**
     * @param aMiddlePoint - around that point connected components are searched (in unit cube)
     * @return list of results
     */
    public List<TopologicalNumberResult> getTopologicalNumbersForAllAdjacentLabels(Point aMiddlePoint) {
        getSubImage(aMiddlePoint);

        // Find all labels of FG neighbors of aMiddlePoint
        final Set<Integer> adjacentLabels = new HashSet<Integer>();
        for (final int index : iFgConnectivity.itOfsInt()) {
            if (iSubImage[index] != ValueForForbiddenPoints) {
                adjacentLabels.add(iSubImage[index]);
            }
        }
        
        // Calculate number of connected components for each label in FG and BG
        final List<TopologicalNumberResult> topologicalNumsResult = new ArrayList<TopologicalNumberResult>(adjacentLabels.size());
        for (final int label : adjacentLabels) {
            for (int i = 0; i < iUnitCubeSize; ++i) {
                iOneLabelSubImageFg[i] = (char) ((iSubImage[i] == label) ? 1 : 0);
                iOneLabelSubImageBg[i] = (char) (1 - iOneLabelSubImageFg[i]);
            }
            
            int FGNumber = iFgConnectedComponentsCounter.SetImage(iOneLabelSubImageFg).getNumberOfConnectedComponents();
            int BGNumber = iBgConnectedComponentsCounter.SetImage(iOneLabelSubImageBg).getNumberOfConnectedComponents();
            topologicalNumsResult.add(new TopologicalNumberResult(label, FGNumber, BGNumber));
        }
        
        return topologicalNumsResult; 
    }

    public static class TopologicalNumberResult {
        public final int iLabel;
        public int iNumOfConnectedComponentsFG;
        public int iNumOfConnectedComponentsBG;
        
        protected TopologicalNumberResult(int label, int FGNumber, int BGNumber) {
            this.iLabel = label;
            this.iNumOfConnectedComponentsFG = FGNumber;
            this.iNumOfConnectedComponentsBG = BGNumber;
        }
        
        @Override
        public String toString() {
            return "TopologicalNumberResult {" + iLabel + ", " + iNumOfConnectedComponentsFG + ", " + iNumOfConnectedComponentsBG + "}";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + iLabel;
            result = prime * result + iNumOfConnectedComponentsBG;
            result = prime * result + iNumOfConnectedComponentsFG;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            TopologicalNumberResult other = (TopologicalNumberResult) obj;
            if (iLabel != other.iLabel) return false;
            if (iNumOfConnectedComponentsBG != other.iNumOfConnectedComponentsBG) return false;
            if (iNumOfConnectedComponentsFG != other.iNumOfConnectedComponentsFG) return false;
            return true;
        }
    }
}
