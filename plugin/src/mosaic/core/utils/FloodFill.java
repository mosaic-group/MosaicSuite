package mosaic.core.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import mosaic.core.binarize.BinarizedImage;


/**
 *
 * This class provide a Flood fill algorithm
 *
 * @author Stephan Semmler
 *
 */


public class FloodFill implements Iterable<Point>
{
    Connectivity conn;

    Stack<Point> stack;
    Set<Point> checkedSet;

    /**
     *
     * Perform a Flood fill starting from a seed point
     *
     * @param conn Connectivity
     * @param foo Image
     * @param seed point
     */

    public FloodFill(Connectivity conn, BinarizedImage foo, Point seed)
    {
        this.conn = conn;

        stack = new Stack<Point>();
        checkedSet = new HashSet<Point>();
        stack.add(seed);

        while (!stack.isEmpty())
        {
            Point p = stack.pop();
            for (Point q: conn.iterateNeighbors(p))
            {
                if (foo.EvaluateAtIndex(q) && !isPointChecked(q))
                {
                    stack.add(q);
                }
            }
            setPointChecked(p);
        }
    }

    private void setPointChecked(Point p)
    {
        checkedSet.add(p);
    }


    private boolean isPointChecked(Point p)
    {
        return checkedSet.contains(p);
    }

    public Set<Point> getPoints()
    {
        return checkedSet;
    }

    //Iterable
    @Override
    public Iterator<Point> iterator()
    {
        return checkedSet.iterator();
    }


}








