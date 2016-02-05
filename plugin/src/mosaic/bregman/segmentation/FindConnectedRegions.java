/* Copyright 2006, 2007 Mark Longair */

/*
 This file is part of the ImageJ plugin "Find Connected Regions".

 The ImageJ plugin "Find Connected Regions" is free software; you
 can redistribute it and/or modify it under the terms of the GNU
 General Public License as published by the Free Software
 Foundation; either version 3 of the License, or (at your option)
 any later version.

 The ImageJ plugin "Find Connected Regions" is distributed in the
 hope that it will be useful, but WITHOUT ANY WARRANTY; without
 even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE.  See the GNU General Public License for more
 details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* This plugin looks for connected regions with the same value in 8
 * bit images, and optionally displays images with just each of those
 * connected regions.  (Otherwise the useful information is just
 * printed out.)
 */

package mosaic.bregman.segmentation;


import java.util.ArrayList;
import java.util.Collections;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;


class FindConnectedRegions {
    private final ImagePlus iInputImg;
    
    private short[][][] iLabeledRegions;
    private final ArrayList<Region> iFoundRegions = new ArrayList<Region>();

    private static final byte IN_QUEUE = 1;
    private static final byte ADDED = 2;

    FindConnectedRegions(ImagePlus aInputImg) {
        iInputImg = aInputImg;
    }

    @SuppressWarnings("null")
    void run(int aMaximumPointsInRegion, int aMinimumPointsInRegion, float aThreshold, boolean exclude_z_edges, int oversampling2ndstep, int interpolation) {
        if (iInputImg == null) {
            IJ.error("No image to operate on.");
            return;
        }
        if (ImagePlus.GRAY8 != iInputImg.getType()) {
            IJ.error("The image must be 8 bit");
            return;
        }

        final int width = iInputImg.getWidth();
        final int height = iInputImg.getHeight();
        final int depth = iInputImg.getStackSize();
        this.iLabeledRegions = new short[depth][width][height];

        if (aMinimumPointsInRegion < 0) {
            aMinimumPointsInRegion = 0;
        }
        if (aMaximumPointsInRegion < 0) {
            aMaximumPointsInRegion = width * height * depth;
        }

        final ImageStack stack = iInputImg.getStack();
        byte[][] sliceDataBytes = new byte[depth][];
        for (int z = 0; z < depth; ++z) {
            final ByteProcessor bp = (ByteProcessor) stack.getProcessor(z + 1);
            sliceDataBytes[z] = (byte[]) bp.getPixelsCopy();
        }

        int tag = 0;
        while (true) {
            // Find one pixel that's above the minimum, or find the maximum in the case where we're
            // not insisting that all regions are made up of the same color. These are set in all cases...
            int initial_x = -1;
            int initial_y = -1;
            int initial_z = -1;

            int maxValueInt = -1;

            for (int z = 0; z < depth; ++z) {
                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        final int value = sliceDataBytes[z][y * width + x] & 0xFF;
                        if (value > maxValueInt && value > aThreshold) {
                            initial_x = x;
                            initial_y = y;
                            initial_z = z;
                            maxValueInt = value;
                        }
                    }
                }
            }

            // If the maximum value is below the level we care about, we're done.
            if (maxValueInt < 0) {
                break;
            }

            int pointsInQueue = 0;
            int queueArrayLength = 1024;
            int[] queue = new int[queueArrayLength];

            final byte[] pointState = new byte[depth * width * height];
            final int i = width * (initial_z * height + initial_y) + initial_x;
            pointState[i] = IN_QUEUE;
            queue[pointsInQueue++] = i;

            int pointsInThisRegion = 0;

            while (pointsInQueue > 0) {
                final int nextIndex = queue[--pointsInQueue];

                final int currentPointStateIndex = nextIndex;
                final int pz = nextIndex / (width * height);
                final int currentSliceIndex = nextIndex % (width * height);
                final int py = currentSliceIndex / width;
                final int px = currentSliceIndex % width;

                pointState[currentPointStateIndex] = ADDED;

                sliceDataBytes[pz][currentSliceIndex] = 0;
                ++pointsInThisRegion;

                final int x_unchecked_min = px - 1;
                final int y_unchecked_min = py - 1;
                final int z_unchecked_min = pz - 1;

                final int x_unchecked_max = px + 1;
                final int y_unchecked_max = py + 1;
                final int z_unchecked_max = pz + 1;

                final int x_min = (x_unchecked_min < 0) ? 0 : x_unchecked_min;
                final int y_min = (y_unchecked_min < 0) ? 0 : y_unchecked_min;
                final int z_min = (z_unchecked_min < 0) ? 0 : z_unchecked_min;

                final int x_max = (x_unchecked_max >= width) ? width - 1 : x_unchecked_max;
                final int y_max = (y_unchecked_max >= height) ? height - 1 : y_unchecked_max;
                final int z_max = (z_unchecked_max >= depth) ? depth - 1 : z_unchecked_max;

                for (int z = z_min; z <= z_max; ++z) {
                    for (int y = y_min; y <= y_max; ++y) {
                        for (int x = x_min; x <= x_max; ++x) {

                            // If we're not including diagonals, skip those points.
                            if ((x == x_unchecked_min || x == x_unchecked_max) && (y == y_unchecked_min || y == y_unchecked_max) && (z == z_unchecked_min || z == z_unchecked_max)) {
                                continue;
                            }
                            final int newSliceIndex = y * width + x;
                            final int newPointStateIndex = width * (z * height + y) + x;

                            final int neighbourValue = sliceDataBytes[z][newSliceIndex] & 0xFF;

                            if (neighbourValue <= aThreshold) {
                                continue;
                            }

                            if (0 == pointState[newPointStateIndex]) {
                                pointState[newPointStateIndex] = IN_QUEUE;
                                if (pointsInQueue == queueArrayLength) {
                                    final int newArrayLength = queueArrayLength * 2;
                                    final int[] newArray = new int[newArrayLength];
                                    System.arraycopy(queue, 0, newArray, 0, pointsInQueue);
                                    queue = newArray;
                                    queueArrayLength = newArrayLength;
                                }
                                queue[pointsInQueue++] = newPointStateIndex;
                            }
                        }
                    }
                }
            }

            // So now pointState should have no IN_QUEUE status points...

            if (pointsInThisRegion < aMinimumPointsInRegion || pointsInThisRegion > aMaximumPointsInRegion) {
                continue;
            }

            // tag only if region not too small or big
            tag++;
            ArrayList<Pix> pixels = new ArrayList<Pix>(pointsInThisRegion);
            for (int z = 0; z < depth; ++z) {
                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        final byte status = pointState[width * (z * height + y) + x];
                        if (status == ADDED) {
                            iLabeledRegions[z][x][y] = (short) tag;
                            pixels.add(new Pix(z, x, y));
                        }
                        else if (status == IN_QUEUE) {
                            IJ.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE");
                        }
                    }
                }
            }
            
            Region region = new Region(tag, pixels);

            // Check for z Edge and maxvesiclesize
            // check for z processing
            if (exclude_z_edges == true && depth /*aThreshold.length*/ != 1) {
                regionCenter(region, oversampling2ndstep, interpolation);
                if (region.getcz() >= 1.0 && region.getcz() <= depth - 2) {
                    iFoundRegions.add(region);
                }
            }
            else {
                iFoundRegions.add(region);
            }
        }

        Collections.sort(iFoundRegions, Collections.reverseOrder());
    }

    private static void regionCenter(Region r, int oversampling2ndstep, int interpolation) {
        double sumx = 0;
        double sumy = 0;
        double sumz = 0;
        for (Pix p : r.iPixels) {
            sumx += p.px;
            sumy += p.py;
            sumz += p.pz;
        }
        int count = r.iPixels.size();

        r.cx = (float) (sumx / count);
        r.cy = (float) (sumy / count);
        r.cz = (float) (sumz / count);

        r.cx = r.cx / (oversampling2ndstep * interpolation);
        r.cy = r.cy / (oversampling2ndstep * interpolation);
        r.cz = r.cz / (oversampling2ndstep * interpolation);
    }
    
    short[][][] getLabeledRegions() {
        return iLabeledRegions;
    }

    ArrayList<Region> getFoundRegions() {
        return iFoundRegions;
    }
}
