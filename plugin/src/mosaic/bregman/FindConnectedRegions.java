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

package mosaic.bregman;


import java.util.ArrayList;
import java.util.Collections;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;


class FindConnectedRegions {
    private final ImagePlus imagePlus;
    final short[][][] tempres;
    public final ArrayList<Region> results = new ArrayList<Region>();

    private static final byte IN_QUEUE = 1;
    private static final byte ADDED = 2;

    public FindConnectedRegions(ImagePlus mask) {
        this(mask, Analysis.p.ni, Analysis.p.nj, Analysis.p.nz);
    }
    
    public FindConnectedRegions(ImagePlus mask, int nx, int ny, int nz) {
        this.imagePlus = mask;
        this.tempres = new short[nz][nx][ny];
    }

    @SuppressWarnings("null")
    public void run(double aValuesOverDoubleThreshold, int aMaximumPointsInRegion, int aMinimumPointsInRegion, float aThreshold) {
        if (imagePlus == null) {
            IJ.error("No image to operate on.");
            return;
        }

        final int type = imagePlus.getType();
        if (!(ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type || ImagePlus.GRAY32 == type)) {
            IJ.error("The image must be either 8 bit or 32 bit for this plugin.");
            return;
        }

        boolean byteImage = (ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type);
        final int width = imagePlus.getWidth();
        final int height = imagePlus.getHeight();
        final int depth = imagePlus.getStackSize();

        if (aMinimumPointsInRegion < 0) {
            aMinimumPointsInRegion = 0;
        }
        if (aMaximumPointsInRegion < 0) {
            aMaximumPointsInRegion = width * height * depth;
        }

        final ImageStack stack = imagePlus.getStack();
        byte[][] sliceDataBytes = null;
        float[][] sliceDataFloats = null;
        if (byteImage) {
            sliceDataBytes = new byte[depth][];
            for (int z = 0; z < depth; ++z) {
                final ByteProcessor bp = (ByteProcessor) stack.getProcessor(z + 1);
                sliceDataBytes[z] = (byte[]) bp.getPixelsCopy();
            }
        }
        else {
            sliceDataFloats = new float[depth][];
            for (int z = 0; z < depth; ++z) {
                final FloatProcessor bp = (FloatProcessor) stack.getProcessor(z + 1);
                sliceDataFloats[z] = (float[]) bp.getPixelsCopy();
            }
        }

        int tag = 0;
        while (true) {
            // Find one pixel that's above the minimum, or find the maximum in the case where we're
            // not insisting that all regions are made up of the same color. These are set in all cases...
            int initial_x = -1;
            int initial_y = -1;
            int initial_z = -1;

            int foundValueInt = -1;
            float foundValueFloat = Float.MIN_VALUE;
            int maxValueInt = -1;
            float maxValueFloat = Float.MIN_VALUE;

            if (byteImage) {
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

                foundValueInt = maxValueInt;

                // If the maximum value is below the level we care about, we're done.
                if (foundValueInt < 0) {
                    break;
                }
            }
            else {
                for (int z = 0; z < depth; ++z) {
                    for (int y = 0; y < height; ++y) {
                        for (int x = 0; x < width; ++x) {
                            final float value = sliceDataFloats[z][y * width + x];
                            if (value > aThreshold) {
                                initial_x = x;
                                initial_y = y;
                                initial_z = z;
                                maxValueFloat = value;
                            }
                        }
                    }
                }

                foundValueFloat = maxValueFloat;

                if (foundValueFloat == Float.MIN_VALUE) {
                    break;
                }
                // If the maximum value is below the level we care about, we're done.
                if (foundValueFloat < aValuesOverDoubleThreshold) {
                    break;
                }
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

                if (byteImage) {
                    sliceDataBytes[pz][currentSliceIndex] = 0;
                }
                else {
                    sliceDataFloats[pz][currentSliceIndex] = Float.MIN_VALUE;
                }
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

                            if (byteImage) {
                                final int neighbourValue = sliceDataBytes[z][newSliceIndex] & 0xFF;

                                if (neighbourValue <= aThreshold) {
                                    continue;
                                }
                            }
                            else {
                                final float neighbourValue = sliceDataFloats[z][newSliceIndex];

                                if (neighbourValue <= aThreshold) {
                                    continue;
                                }
                            }

                            if (0 == pointState[newPointStateIndex]) {
                                pointState[newPointStateIndex] = IN_QUEUE;
                                if (pointsInQueue == queueArrayLength) {
                                    final int newArrayLength = (int) (queueArrayLength * 1.2);
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
            Region region = byteImage ? new Region(foundValueInt, pointsInThisRegion) : 
                                        new Region(pointsInThisRegion);

            if (pointsInThisRegion < aMinimumPointsInRegion || pointsInThisRegion > aMaximumPointsInRegion) {
                continue;
            }

            // tag only if region not too small or big
            tag++;

            for (int z = 0; z < depth; ++z) {
                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        final byte status = pointState[width * (z * height + y) + x];

                        if (status == IN_QUEUE) {
                            IJ.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE");
                        }

                        if (status == ADDED) {
                            if (region.points <= aMaximumPointsInRegion) {
                                tempres[z][x][y] = (short) tag;
                                region.pixels.add(new Pix(z, x, y));
                                region.value = tag;
                            }
                        }
                    }
                }
            }

            // Check for z Edge and maxvesiclesize
            if (region.points <= aMaximumPointsInRegion) {
                // check for z processing

                if (Analysis.p.exclude_z_edges == true && depth /*aThreshold.length*/ != 1) {
                    Analysis.regionCenter(region);
                    if (region.getcz() >= 1.0 && region.getcz() <= depth /*aThreshold.length*/ - 2) {
                        results.add(region);
                    }
                }
                else {
                    results.add(region);
                }
            }
        }

        Collections.sort(results, Collections.reverseOrder());
    }
}
