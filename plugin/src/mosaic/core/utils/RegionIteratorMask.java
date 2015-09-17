package mosaic.core.utils;


import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Given a mask and an imput imae, it produce a fast jumping table to
 * iterate through the mask relative to the input image (out of boundary
 * points are automatically excluded)
 * (This is a fast implementation)
 *
 * @author Pietro Incardona
 */
public class RegionIteratorMask {

    Mask mask;
    private int[] input; // size of the input image
    private int[] m_Size; // size of the mask
    int[] ofs; // "upper left" coordinates of the sphere-region within input image
    int[] crop_s; // "left crop start"
    int[] crop_e; // "right left crop start"
    Point ofs_p;

    // iterator
    int itInput = 0;
    int cachedNext = -1; // detect if there is a next next.
    RegionIterator regionIt;
    MaskIterator maskIt;

    private class MovePoint {

        public Point p;
        public int idx;
    };

    private class RJmp {

        public int idx = 0;
    };

    // Crop type

    private boolean simple;

    private int idx_j = 0;
    private int rjmp_idx = 0;
    private RJmp rJmp[];
    private int jumpTable[] = null;
    private Point jumpTableGeo[] = null;
    private int maskAdjTable[];
    private int next_idx_jump = 0;

    private void fillJump() {
        int jidx = 0;
        jumpTable = new int[mask.getFgPoints()];
        jumpTableGeo = new Point[mask.getFgPoints()];
        maskAdjTable = new int[regionIt.getSize()];
        List<RJmp> rJmpTmp = new ArrayList<RJmp>();

        while (regionIt.hasNext()) {
            Point pt = regionIt.getPoint();
            int idx = regionIt.nextRmask();
            int reset = regionIt.getRMask();
            int itMask = maskIt.next();

            if (reset != 0) {
                RJmp tmp = new RJmp();
                tmp.idx = jidx;

                rJmpTmp.add(tmp);
            }

            if (mask.isInMask(itMask)) {
                // found another index
                // Move backward

                int s = itMask - 1;
                while (s >= 0 && maskAdjTable[s] == -1) {
                    maskAdjTable[s] = jidx;
                    s--;
                }
                maskAdjTable[itMask] = jidx;

                jumpTable[jidx] = idx;
                jumpTableGeo[jidx] = pt;
                jidx += 1;
            }
            else {
                maskAdjTable[itMask] = -1;

                // more luck next time
                continue;
            }
        }

        rJmp = rJmpTmp.toArray(new RJmp[rJmpTmp.size()]);
        Point p = new Point(this.input.length);
        p.zero();
        setUpperLeft(p);
    }

    /**
     * Create a Region iterator Mask
     *
     * @param mask Is the mask
     * @param inputSize size of the original image where is going to run the iterator (width/height/...)
     */
    public RegionIteratorMask(Mask mask, int[] inputSize) {
        this.input = inputSize;
        int[] tmpInputSize = new int[inputSize.length];
        this.mask = mask;
        m_Size = mask.getDimensions();

        int x[] = new int[inputSize.length];
        for (int i = 0; i < inputSize.length; i++) {
            x[i] = 0;
        }

        // We need that regionIt span all mask space without crop

        for (int i = 0; i < inputSize.length; i++) {
            if (this.m_Size[i] >= inputSize[i]) {
                tmpInputSize[i] = m_Size[i];
            }
            else {
                tmpInputSize[i] = inputSize[i];
            }
        }

        regionIt = new RegionIterator(tmpInputSize, this.m_Size, x);

        maskIt = new MaskIterator(tmpInputSize, this.m_Size, x);
        fillJump();
    }

    /**
     * Set the offset of the mask region within the input
     * by giving the midpoint of the mask region
     * 
     * @param midPoint
     */
    public void setMidPoint(Point midPoint) {
        Point half = (new Point(m_Size)).div(2);
        ofs = midPoint.sub(half).x;
        ofs_p = new Point(ofs);
        initIterators();
    }

    /**
     * Set the offset of the mask region within the input
     * by giving the upper left point
     * 
     * @param upperleft Offset of the mask region
     */
    public void setUpperLeft(Point upperleft) {
        this.ofs = upperleft.x;
        ofs_p = new Point(ofs);
        initIterators();
    }

    private void initIterators() {
        cachedNext = -1;
        crop_s = new int[ofs.length];
        crop_e = new int[ofs.length];

        itInput = 0;
        simple = true;
        int faci = 1;
        for (int i = 0; i < ofs.length; i++) {
            crop_e[i] = m_Size[i];
            if (ofs[i] < 0) {
                simple = false;
                crop_s[i] = -ofs[i];
            }
            if (ofs[i] + m_Size[i] >= input[i]) {
                simple = false;
                crop_e[i] = input[i] - ofs[i];
            }
            itInput += ofs[i] * faci;
            faci *= input[i];
        }
        idx_j = 0;
        rjmp_idx = 0;

        if (simple == false) {
            MovePoint tmpM = new MovePoint();

            // Bug

            // if (jumpTableGeo.length == 0)
            // {
            // int debug = 0;
            // debug++;
            // }

            // /////
            tmpM.p = new Point(jumpTableGeo[idx_j]);

            do {
                if (tmpM.idx >= maskAdjTable.length) {
                    idx_j = jumpTable.length;
                    return;
                }
                idx_j = maskAdjTable[tmpM.idx];
                if (idx_j == -1) {
                    idx_j = jumpTable.length;
                    return;
                }
                tmpM.p = new Point(jumpTableGeo[idx_j]);
            } while (isValidAndAdjust(tmpM) == false);

            while (rjmp_idx < rJmp.length && rJmp[rjmp_idx].idx <= idx_j) {
                rjmp_idx++;
            }

            if (rjmp_idx < rJmp.length) {
                next_idx_jump = rJmp[rjmp_idx].idx;
            }
        }
    }

    /**
     * Has next
     *
     * @return true if we have next point
     */

    public boolean hasNext() {
        if (idx_j < jumpTable.length) {
            if (cachedNext < 0) {
                cachedNext = calcNext();
                if (cachedNext < 0) {
                    return false;
                }
                else {
                    return true;
                }
            }
            else {
                // there is a cached next
                return true;
            }
        }
        return false;
    }

    private boolean isValidAndAdjust(MovePoint p) {
        int fac = 1;

        boolean val = true;
        p.idx = 0;
        int i = 0;
        for (i = 0; i < p.p.x.length - 1; i++) {
            if (p.p.x[i] < crop_s[i]) {
                p.p.x[i] = crop_s[i];
                for (int s = 0; s < i - 1; s++) {
                    p.p.x[s] = 0;
                }
                p.idx = 0;
                val = false;
            }
            else if (p.p.x[i] >= crop_e[i]) {
                p.idx = 0;
                int faci = 1;
                for (int s = 0; s < i - 1; s++) {
                    p.p.x[s] = crop_s[s];
                    p.idx += crop_s[s] * faci;
                    faci *= m_Size[s];
                }
                p.idx += crop_s[i] * faci;
                p.p.x[i] = crop_s[i];
                p.p.x[i + 1]++;
                val = false;
            }
            p.idx += p.p.x[i] * fac;
            fac *= m_Size[i];
        }

        if (p.p.x[i] < crop_s[i]) {
            p.p.x[i] = crop_s[i];
            for (int s = i - 1; s >= 0; s--) {
                p.p.x[s] = 0;
            }
            p.idx = 0;
            val = false;
        }
        else if (p.p.x[i] >= crop_e[i]) {
            p.idx = m_Size[i] * fac;
            return false;
        }

        p.idx += p.p.x[i] * fac;

        return val;
    }

    /**
     * Get the next masked index (input coordinates).
     * 
     * @return next index in input coordinates. -1 if there is no more index
     */
    public int next() {
        if (cachedNext < 0) {
            int result = calcNext();
            if (result < 0) {
                throw new NoSuchElementException();
            }
            return result;
        }
        else {
            // there is a cached next. return and reset it
            int result = cachedNext;
            cachedNext = -1;
            return result;
        }

    }

    /**
     * Get the next masked Point (input coordinates).
     * 
     * @return next Point, null if ended
     */
    public Point nextP() {
        next();
        return jumpTableGeo[idx_j - 1].add(ofs_p);
    }

    /**
     * Get the next masked index (input coordinates).
     * 
     * @return next index in input coordinates. -1 if there is no more index
     */
    private int calcNext() {
        if (simple == true) {
            if (idx_j < jumpTable.length) {
                int val = itInput + jumpTable[idx_j];
                idx_j += 1;
                return val;
            }
            else {
                return -1;
            }
        }
        else {
            if (jumpTableGeo[idx_j].x[0] < crop_e[0] && idx_j < next_idx_jump) {
                int val = itInput + jumpTable[idx_j];
                idx_j += 1;
                return val;
            }
            else {
                int val;
                MovePoint p = new MovePoint();
                p.p = new Point(jumpTableGeo[idx_j]);
                while (isValidAndAdjust(p) == false) {
                    if (p.idx >= maskAdjTable.length) {
                        return -1;
                    }
                    idx_j = maskAdjTable[p.idx];
                    if (idx_j == -1) {
                        return -1;
                    }
                    p.p = new Point(jumpTableGeo[idx_j]);
                }

                while (rjmp_idx < rJmp.length && rJmp[rjmp_idx].idx <= idx_j) {
                    rjmp_idx++;
                }
                if (rjmp_idx < rJmp.length) {
                    next_idx_jump = rJmp[rjmp_idx].idx;
                }

                val = itInput + jumpTable[idx_j];
                idx_j += 1;
                return val;
            }
        }
    }
}
