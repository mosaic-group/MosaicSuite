/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

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


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;


class FindConnectedRegions {

    public static final String PLUGIN_VERSION = "1.2";
    public byte[][][] softmask;

    public ImagePlus imagePlus;
    short[][][] tempres;
    public ArrayList<Region> results = new ArrayList<Region>();

    boolean pleaseStop = false;

    public FindConnectedRegions(ImagePlus mask, byte[][][] smask) {
        this.imagePlus = mask;
        this.softmask = smask;
        this.tempres = new short[Analysis.p.nz][Analysis.p.ni][Analysis.p.nj];
    }

    public FindConnectedRegions(ImagePlus mask, int nx, int ny, int nz) {
        this.imagePlus = mask;
        this.tempres = new short[nz][nx][ny];
    }

    public FindConnectedRegions(ImagePlus mask) {
        this.imagePlus = mask;
        this.tempres = new short[Analysis.p.nz][Analysis.p.ni][Analysis.p.nj];
    }

    private static final byte IN_QUEUE = 1;
    private static final byte ADDED = 2;

    @SuppressWarnings("null")
    public void run(double threshold, int maxvesiclesize, int minvesiclesize, double minInt, float[][][] tr) {

        int tag = 0;

        if (minvesiclesize < 0) {
            minvesiclesize = 0;
        }
        boolean diagonal = false;
        boolean display = false;
        boolean showResults = false;
        boolean mustHaveSameValue = false;
        boolean startFromPointROI = false;
        boolean autoSubtract = false;
        double valuesOverDouble = threshold;
        double minimumPointsInRegionDouble = minvesiclesize;
        int stopAfterNumberOfRegions = -1;

        // IJ.log("thres" + valuesOverDouble);
        // ImageCalculator iCalc = new ImageCalculator();

        // ImagePlus imagePlus = IJ.getImage();
        if (imagePlus == null) {
            IJ.error("No image to operate on.");
            return;
        }

        int type = imagePlus.getType();

        if (!(ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type || ImagePlus.GRAY32 == type)) {
            IJ.error("The image must be either 8 bit or 32 bit for this plugin.");
            return;
        }

        boolean byteImage = false;
        if (ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type) {
            byteImage = true;
        }

        if (!byteImage && mustHaveSameValue) {
            IJ.error("You can only specify that each region must have the same value for 8 bit images.");
            return;
        }

        boolean startAtMaxValue = !mustHaveSameValue;

        int point_roi_x = -1;
        int point_roi_y = -1;
        int point_roi_z = -1;

        if (startFromPointROI) {

            Roi roi = imagePlus.getRoi();
            if (roi == null) {
                IJ.error("There's no point selected in the image.");
                return;
            }
            if (roi.getType() != Roi.POINT) {
                IJ.error("There's a selection in the image, but it's not a point selection.");
                return;
            }
            Polygon p = roi.getPolygon();
            if (p.npoints > 1) {
                IJ.error("You can only have one point selected.");
                return;
            }

            point_roi_x = p.xpoints[0];
            point_roi_y = p.ypoints[0];
            point_roi_z = imagePlus.getCurrentSlice() - 1;

            System.out.println("Fetched ROI with co-ordinates: " + p.xpoints[0] + ", " + p.ypoints[0]);
        }

        int width = imagePlus.getWidth();
        int height = imagePlus.getHeight();
        int depth = imagePlus.getStackSize();

        if (maxvesiclesize < 0) {
            maxvesiclesize = width * height * depth;
        }

        if (width * height * depth > Integer.MAX_VALUE) {
            IJ.error("This stack is too large for this plugin (must have less than " + Integer.MAX_VALUE + " points.");
            return;
        }

        ImageStack stack = imagePlus.getStack();

        byte[][] sliceDataBytes = null;
        float[][] sliceDataFloats = null;

        if (byteImage) {
            sliceDataBytes = new byte[depth][];
            for (int z = 0; z < depth; ++z) {
                ByteProcessor bp = (ByteProcessor) stack.getProcessor(z + 1);
                sliceDataBytes[z] = (byte[]) bp.getPixelsCopy();
            }
        }
        else {
            sliceDataFloats = new float[depth][];
            for (int z = 0; z < depth; ++z) {
                FloatProcessor bp = (FloatProcessor) stack.getProcessor(z + 1);
                sliceDataFloats[z] = (float[]) bp.getPixelsCopy();
            }
        }

        // Preserve the calibration and colour lookup tables
        // for generating new images of each individual
        // region.
        // Calibration calibration = imagePlus.getCalibration();

        // ColorModel cm = null;
        // if (ImagePlus.COLOR_256 == type)
        // {
        // cm = stack.getColorModel();
        // }

        ResultsTable rt = ResultsTable.getResultsTable();
        rt.reset();

        // CancelDialog cancelDialog=new CancelDialog(this);
        // cancelDialog.show();

        boolean firstTime = true;
        // int tk=0;
        while (true) {
            // tk++;

            if (pleaseStop) {
                break;
            }

            /*
             * Find one pixel that's above the minimum, or
             * find the maximum in the case where we're
             * not insisting that all regions are made up
             * of the same color. These are set in all
             * cases...
             */

            int initial_x = -1;
            int initial_y = -1;
            int initial_z = -1;

            int foundValueInt = -1;
            float foundValueFloat = Float.MIN_VALUE;
            int maxValueInt = -1;
            float maxValueFloat = Float.MIN_VALUE;

            if (firstTime && startFromPointROI) {

                initial_x = point_roi_x;
                initial_y = point_roi_y;
                initial_z = point_roi_z;

                if (byteImage) {
                    foundValueInt = sliceDataBytes[initial_z][initial_y * width + initial_x] & 0xFF;
                }
                else {
                    foundValueFloat = sliceDataFloats[initial_z][initial_y * width + initial_x];
                }

            }
            else if (byteImage && startAtMaxValue) {
                // IJ.log("");
                for (int z = 0; z < depth; ++z) {
                    for (int y = 0; y < height; ++y) {
                        for (int x = 0; x < width; ++x) {
                            int value = sliceDataBytes[z][y * width + x] & 0xFF;
                            // if (x==78 && y==415){IJ.log("value " + value +
                            // "tr" + tr[z][x][y]);}
                            if (value > maxValueInt && value > tr[z][x][y] && value > minInt) {
                                initial_x = x;
                                initial_y = y;
                                initial_z = z;
                                maxValueInt = value;
                            }
                        }
                    }
                }
                // IJ.log(tk+" found max value:"
                // +maxValueInt+" at: "+initial_x+" "+initial_y+" "+initial_z );

                foundValueInt = maxValueInt;

                /*
                 * If the maximum value is below the
                 * level we care about, we're done.
                 */

                if (foundValueInt < 0) {// valuesOverDouble) {
                    break;
                }

            }
            else if (byteImage && !startAtMaxValue) {
                IJ.log("thres2");
                // Just finding some point in the a region...
                for (int z = 0; z < depth && foundValueInt == -1; ++z) {
                    for (int y = 0; y < height && foundValueInt == -1; ++y) {
                        for (int x = 0; x < width; ++x) {
                            int value = sliceDataBytes[z][y * width + x] & 0xFF;
                            if (value > tr[z][x][y]) {// valuesOverDouble) {

                                initial_x = x;
                                initial_y = y;
                                initial_z = z;
                                foundValueInt = value;
                                break;
                            }
                        }
                    }
                }

                if (foundValueInt == -1) {
                    break;
                }

            }
            else {
                IJ.log("thres3");
                // This must be a 32 bit image and we're starting at the maximum
                assert (!byteImage && startAtMaxValue);

                for (int z = 0; z < depth; ++z) {
                    for (int y = 0; y < height; ++y) {
                        for (int x = 0; x < width; ++x) {
                            float value = sliceDataFloats[z][y * width + x];
                            if (value > tr[z][x][y]) {// valuesOverDouble) {
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

                    // If the maximum value is below the level we
                    // care about, we're done.
                }
                if (foundValueFloat < valuesOverDouble) {
                    break;
                }

            }

            firstTime = false;

            int vint = foundValueInt;

            int pointsInQueue = 0;
            int queueArrayLength = 1024;
            int[] queue = new int[queueArrayLength];

            byte[] pointState = new byte[depth * width * height];
            int i = width * (initial_z * height + initial_y) + initial_x;
            pointState[i] = IN_QUEUE;
            queue[pointsInQueue++] = i;

            int pointsInThisRegion = 0;

            // IJ.log("vint :" + vint + "seuil " + ((int) (0.1*vint)));

            while (pointsInQueue > 0) {
                if (pleaseStop) {
                    break;
                }

                int nextIndex = queue[--pointsInQueue];

                int currentPointStateIndex = nextIndex;
                int pz = nextIndex / (width * height);
                int currentSliceIndex = nextIndex % (width * height);
                int py = currentSliceIndex / width;
                int px = currentSliceIndex % width;

                pointState[currentPointStateIndex] = ADDED;

                if (byteImage) {
                    sliceDataBytes[pz][currentSliceIndex] = 0;
                }
                else {
                    sliceDataFloats[pz][currentSliceIndex] = Float.MIN_VALUE;
                }
                ++pointsInThisRegion;

                int x_unchecked_min = px - 1;
                int y_unchecked_min = py - 1;
                int z_unchecked_min = pz - 1;

                int x_unchecked_max = px + 1;
                int y_unchecked_max = py + 1;
                int z_unchecked_max = pz + 1;

                int x_min = (x_unchecked_min < 0) ? 0 : x_unchecked_min;
                int y_min = (y_unchecked_min < 0) ? 0 : y_unchecked_min;
                int z_min = (z_unchecked_min < 0) ? 0 : z_unchecked_min;

                int x_max = (x_unchecked_max >= width) ? width - 1 : x_unchecked_max;
                int y_max = (y_unchecked_max >= height) ? height - 1 : y_unchecked_max;
                int z_max = (z_unchecked_max >= depth) ? depth - 1 : z_unchecked_max;

                for (int z = z_min; z <= z_max; ++z) {
                    for (int y = y_min; y <= y_max; ++y) {
                        for (int x = x_min; x <= x_max; ++x) {

                            // If we're not including diagonals,
                            // skip those points.
                            if ((!diagonal) && (x == x_unchecked_min || x == x_unchecked_max) && (y == y_unchecked_min || y == y_unchecked_max) && (z == z_unchecked_min || z == z_unchecked_max)) {
                                continue;
                            }
                            int newSliceIndex = y * width + x;
                            int newPointStateIndex = width * (z * height + y) + x;

                            if (byteImage) {

                                int neighbourValue = sliceDataBytes[z][newSliceIndex] & 0xFF;

                                if (mustHaveSameValue) {
                                    if (neighbourValue != vint) {
                                        continue;
                                    }
                                }
                                else {

                                    // if (neighbourValue <= ((int) (0.1*vint))
                                    // || neighbourValue <=minInt){ // for 3D
                                    // mito{//valuesOverDouble)
                                    if (neighbourValue <= tr[z][x][y] || neighbourValue <= minInt) { // for
                                        // 3D
                                        // mito{//valuesOverDouble)
                                        continue;
                                    }
                                }
                            }
                            else {

                                float neighbourValue = sliceDataFloats[z][newSliceIndex];

                                if (neighbourValue <= tr[z][x][y] || neighbourValue <= minInt) {// for
                                    // 3D
                                    // mito
                                    // {//valuesOverDouble)
                                    continue;
                                }
                            }

                            if (0 == pointState[newPointStateIndex]) {
                                pointState[newPointStateIndex] = IN_QUEUE;
                                if (pointsInQueue == queueArrayLength) {
                                    int newArrayLength = (int) (queueArrayLength * 1.2);
                                    int[] newArray = new int[newArrayLength];
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

            if (pleaseStop) {
                break;
            }

            // So now pointState should have no IN_QUEUE
            // status points...
            Region region;

            if (byteImage) {
                // IJ.log("region tag t2" + vint);
                region = new Region(vint, pointsInThisRegion);
            }
            else {
                region = new Region(pointsInThisRegion);
            }

            // IJ.log("size " + pointsInThisRegion);

            if (pointsInThisRegion < minimumPointsInRegionDouble) {
                // System.out.println("Too few points - only " +
                // pointsInThisRegion);
                continue;
            }
            if (pointsInThisRegion > maxvesiclesize) {
                // System.out.println("Too few points - only " +
                // pointsInThisRegion);
                continue;
            }

            // tag only if region not too small or big
            tag++;
            // IJ.log("region tag " + tag);

            if (display || autoSubtract || true) {
                // ImageStack newStack = new ImageStack(width, height);
                for (int z = 0; z < depth; ++z) {
                    // byte[] sliceBytes = new byte[width * height];
                    for (int y = 0; y < height; ++y) {
                        for (int x = 0; x < width; ++x) {
                            byte status = pointState[width * (z * height + y) + x];

                            if (status == IN_QUEUE) {
                                IJ.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE");
                            }

                            if (status == ADDED) {
                                if (region.points <= maxvesiclesize) {
                                    tempres[z][x][y] = (short) tag;// tag;
                                    region.pixels.add(new Pix(z, x, y));
                                    region.value = tag;
                                    // region.intensity=;
                                }
                            }
                        }
                    }
                }

                // Check for z Edge and maxvesiclesize

                if (region.points <= maxvesiclesize) {
                    // check for z processing

                    if (Analysis.p.exclude_z_edges == true && tr.length != 1) {
                        Analysis.regionCenter(region);
                        if (region.getcz() >= 1.0 && region.getcz() <= tr.length - 2) {
                            results.add(region);
                        }
                    }
                    else {
                        results.add(region);
                    }

                }

            }

            if ((stopAfterNumberOfRegions > 0) && (results.size() >= stopAfterNumberOfRegions)) {
                break;
            }
        }

        Collections.sort(results, Collections.reverseOrder());

        if (showResults) {
            rt.show("Results");
        }
    }
}
