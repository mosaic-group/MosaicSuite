package mosaic.core.imageUtils;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import mosaic.core.binarize.BinarizedImage;
import mosaic.core.imageUtils.images.LabelImage;


/**
 * Flood fill algorithm implementation. It iterates on all indices in given area (thresholds)
 * starting at given seed.
 */
public class FloodFill implements Iterator<Integer>,  Iterable<Integer> {

    private final Set<Integer> iFoundIndices = new HashSet<Integer>();
    private final Iterator<Integer> iIterator;
    
    /**
     * @param aInputImg - input label image
     * @param aAreaForProcessing - provided thresholds for iterated values
     * @param aPointSeed - start point, if its value is outside of input area FloodFill will not iterate
     *                     on any index.
     */
    public FloodFill(LabelImage aInputImg, BinarizedImage aAreaForProcessing, Point aPointSeed) {
        final Stack<Integer> stackIdx = new Stack<Integer>();
        
        Integer inputIndex = aInputImg.pointToIndex(aPointSeed);
        if (aAreaForProcessing.EvaluateAtIndex(inputIndex)) stackIdx.add(inputIndex);
        while (!stackIdx.isEmpty()) {
            final Integer idx = stackIdx.pop();
            for (final int neighbourIdx : aInputImg.iterateNeighbours(idx)) {
                if (aAreaForProcessing.EvaluateAtIndex(neighbourIdx) && !iFoundIndices.contains(neighbourIdx)) {
                    stackIdx.add(neighbourIdx);
                }
            }
            iFoundIndices.add(idx);
        }
        iIterator = iFoundIndices.iterator();
    }

    public int size() {
        return iFoundIndices.size();
    }

    // --------------  Iterator implementations
    @Override
    public boolean hasNext() {
        return iIterator.hasNext();
    }

    @Override
    public Integer next() {
        return iIterator.next();
    }

    @Override
    public void remove() { /* no action - needed by Java < 1.8 */ }
    
    //  --------------  Iterable implementations
    @Override
    public Iterator<Integer> iterator() {
        return iIterator;
    }
}
