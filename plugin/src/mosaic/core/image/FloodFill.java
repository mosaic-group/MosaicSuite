package mosaic.core.image;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import mosaic.core.binarize.BinarizedImage;


/**
 * This class provide a Flood fill algorithm
 *
 * @author Stephan Semmler
 */

public class FloodFill {

    private final Stack<Point> stack = new Stack<Point>();
    private final Set<Point> checkedSet  = new HashSet<Point>();
    private final Stack<Integer> stackIdx = new Stack<Integer>();
    private final Set<Integer> checkedSetIdx = new HashSet<Integer>();
    /**
     * Perform a Flood fill starting from a seed point
     *
     * @param conn Connectivity
     * @param foo Image
     * @param seed point
     */

    public FloodFill(Connectivity conn, BinarizedImage foo, Point seed) {
        stack.add(seed);

        while (!stack.isEmpty()) {
            final Point p = stack.pop();
            for (final Point q : conn.iterateNeighbors(p)) {
                if (foo.EvaluateAtIndex(q) && !checkedSet.contains(q)) {
                    stack.add(q);
                }
            }
            checkedSet.add(p);
        }
    }
    
    public FloodFill(LabelImage img, BinarizedImage foo, Point seed) {
        stackIdx.add(img.pointToIndex(seed));

        while (!stackIdx.isEmpty()) {
            final Integer p = stackIdx.pop();
            for (final int q : img.iterateNeighbours(p)) {
                if (foo.EvaluateAtIndex(q) && !checkedSetIdx.contains(q)) {
                    stackIdx.add(q);
                }
            }
            checkedSetIdx.add(p);
        }
    }
    
    public int size() {
        return checkedSet.size();
    }
    
    public Iterable<Point> iteratorPoint() {
        return new Iterable<Point>() {

            @Override
            public Iterator<Point> iterator() {
                return iteratorPointOffset();
            }
        };
    }
    
    public Iterator<Point> iteratorPointOffset() {
        return checkedSet.iterator();
    }
    
    public Iterable<Integer> iteratorIdx() {
        return new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return iteratorIndex();
            }
        };
    }
    
    public Iterator<Integer> iteratorIndex() {
        return checkedSetIdx.iterator();
    }
}
