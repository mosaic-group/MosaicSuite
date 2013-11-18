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
//import ij.gui.PointRoi;
//import ij.io.FileSaver;
//import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
//import ij.process.ImageConverter;
//import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

//import java.util.Arrays;
import java.util.Collections;
//import java.util.Iterator;
import java.util.ArrayList;
//import amira.AmiraParameters;
//import ij.measure.Calibration;
import ij.process.FloatProcessor;
//import ij.plugin.ImageCalculator;
//import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

import ij.measure.ResultsTable;

import java.awt.Color;
//import java.awt.Dialog;
//import java.awt.Button;
import java.awt.Polygon;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;

//import net.sf.javaml.clustering.Clusterer;
//import net.sf.javaml.core.Dataset;
//import net.sf.javaml.core.DefaultDataset;
//import net.sf.javaml.core.DenseInstance;
//import net.sf.javaml.core.Instance;
//import net.sf.javaml.tools.DatasetTools;
//import net.sf.javaml.tools.weka.WekaClusterer;
//import weka.clusterers.SimpleKMeans;

//pourquoi utile ? :
//import net.sf.javaml.core.Dataset;
//import net.sf.javaml.core.DefaultDataset;



public class FindConnectedRegions  {

	public static final String PLUGIN_VERSION = "1.2";
	public byte  [] [] [] softmask;
	public static ImageStack regstackx;
	public static ImageStack regstacky;

	public static ImagePlus regsresultx=new ImagePlus();
	public static ImagePlus regsresulty=new ImagePlus();
	public ImagePlus imagePlus;
	short [] [] [] tempres;
	public ArrayList<Region> results = new ArrayList<Region>();


	boolean pleaseStop = false;

	public void cancel() {
		pleaseStop = true;
	}

	public FindConnectedRegions(ImagePlus mask, byte [][][] smask){
		this.imagePlus= mask;
		this.softmask= smask;
		this.tempres=new short [Analysis.p.nz][Analysis.p.ni][Analysis.p.nj];
	}

	public FindConnectedRegions(ImagePlus mask, int nx, int ny, int nz){
		this.imagePlus= mask;
		this.tempres=new short [nz][nx][ny];
	}

	public FindConnectedRegions(ImagePlus mask){
		this.imagePlus= mask;
		this.tempres=new short [Analysis.p.nz][Analysis.p.ni][Analysis.p.nj];
	}


	public static IndexColorModel backgroundAndSpectrum(int maximum) {
		if( maximum > 255 )
			maximum = 255;
		byte [] reds = new byte[256];
		byte [] greens = new byte[256];
		byte [] blues = new byte[256];
		// Set all to white:
		for( int i = 0; i < 256; ++i ) {
			reds[i] = greens[i] = blues[i] = (byte)255;
		}
		// Set 0 to black:
		reds[0] = greens[0] = blues[0] = 0;
		float divisions = maximum;
		Color c;
		for( int i = 1; i <= maximum; ++i ) {
			float h = (i - 1) / divisions;
			c = Color.getHSBColor(h,1f,1f);
			reds[i] = (byte)c.getRed();
			greens[i] = (byte)c.getGreen();
			blues[i] = (byte)c.getBlue();
		}
		return new IndexColorModel( 8, 256, reds, greens, blues );
	}



	/* An inner class to make the results list sortable. */
	public class Region implements Comparable {


		boolean colocpositive=false;

		Region(int value, String materialName, int points, boolean sameValue) {
			//	byteImage = true;
			this.value = value;
			//this.materialName = materialName;
			this.points = points;
			//	this.sameValue = sameValue;
		}

		Region(int points, boolean sameValue) {
			//	byteImage = false;
			this.points = points;
			//	this.sameValue = sameValue;
		}

		ArrayList<Pix> pixels = new ArrayList<Pix>();
		//boolean byteImage;
		public int points;
		float rsize;
		//String materialName;
		int value;
		double perimeter;
		double length;
		Region rvoronoi;
		//boolean sameValue;
		double intensity;
		float cx,cy,cz;
		float overlap;
		float over_int;
		float over_size;
		float beta_in;
		float beta_out;
		boolean singlec;
		double coloc_o_int;
		public int compareTo(Object otherRegion) {
			Region o = (Region) otherRegion;
			return (value < o.value) ? 1 : ((value  > o.value) ? -1 : 0);
		}

		//		@Override
		//		public String toString() {
		//			if (byteImage) {
		//				String materialBit = "";
		//				if (materialName != null) {
		//					materialBit = " (" + materialName + ")";
		//				}
		//				return "Region of value " + value + materialBit + " containing " + points + " points";
		//			} else {
		//				return "Region containing " + points + " points";
		//			}
		//		}

		//		public void addRow( ResultsTable rt ) {
		//			rt.incrementCounter();
		//			if(byteImage) {
		//				if(sameValue)
		//					rt.addValue("Value in Region",value);
		//				rt.addValue("Points In Region",points);
		//				if(materialName!=null)
		//					rt.addLabel("Material Name",materialName);
		//			} else {
		//				rt.addValue("Points in Region",points);
		//			}
		//		}
		
		public double getcx()
		{
			return cx;
		}

		public double getcy()
		{
			return cy;
		}
		
		public double getcz()
		{
			return cz;
		}
		
		public double getintensity()
		{
			return intensity;
		}
		
		public double getrsize()
		{
			return rsize;
		}
		
		public double getperimeter()
		{
			return perimeter;
		}
	}




	private static final byte IN_QUEUE = 1;
	private static final byte ADDED = 2;

	public void run(double threshold, int channel, int maxvesiclesize,int minvesiclesize, double minInt,float [][][] tr, boolean displ, boolean save) {

		int tag=0;

		//		GenericDialog gd = new GenericDialog("Find Connected Regions Options (version: "+PLUGIN_VERSION+")");
		//		gd.addCheckbox("Allow_diagonal connections?", false);
		//		gd.addCheckbox("Display_image_for_each region?", true);
		//		gd.addCheckbox("Display_results table?", true);
		//		gd.addCheckbox("Regions_must have the same value?", true);
		//		gd.addCheckbox("Start_from_point selection?", false);
		//		gd.addCheckbox("Autosubtract discovered regions from original image?", false);
		//		gd.addNumericField("Regions_for_values_over: ", 0, 0);
		//		gd.addNumericField("Minimum_number_of_points in a region", 1, 0);
		//		gd.addNumericField("Stop_after this number of regions are found: ", 1, 0);
		//		gd.addMessage("(If number of regions is -1, find all of them.)");
		//
		//		gd.showDialog();

		//		if (gd.wasCanceled()) {
		//			return;
		//		}
		//		boolean diagonal = gd.getNextBoolean();
		//		boolean display = gd.getNextBoolean();
		//		boolean showResults = gd.getNextBoolean();
		//		boolean mustHaveSameValue = gd.getNextBoolean();
		//		boolean startFromPointROI = gd.getNextBoolean();
		//		boolean autoSubtract = gd.getNextBoolean();
		//		double valuesOverDouble = gd.getNextNumber();
		//		double minimumPointsInRegionDouble = gd.getNextNumber();
		//		int stopAfterNumberOfRegions = (int) gd.getNextNumber();

		if (minvesiclesize <0) minvesiclesize=  0;
		boolean diagonal = false;
		boolean display = false;
		boolean showResults = false;
		boolean mustHaveSameValue = false;
		boolean startFromPointROI = false;
		boolean autoSubtract = false;
		double valuesOverDouble = threshold;
		double minimumPointsInRegionDouble = minvesiclesize;
		int stopAfterNumberOfRegions = -1;

		//IJ.log("thres" + valuesOverDouble);
//		ImageCalculator iCalc = new ImageCalculator();

		//ImagePlus imagePlus = IJ.getImage();
		if (imagePlus == null) 
		{
			IJ.error("No image to operate on.");
			return;
		}

		int type = imagePlus.getType();

		if (!(ImagePlus.GRAY8 == type || ImagePlus.COLOR_256 == type || ImagePlus.GRAY32 == type)) 
		{
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

		if( startFromPointROI ) 
		{

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
			if(p.npoints > 1) {
				IJ.error("You can only have one point selected.");
				return;
			}

			point_roi_x = p.xpoints[0];
			point_roi_y = p.ypoints[0];
			point_roi_z = imagePlus.getCurrentSlice()-1;

			System.out.println("Fetched ROI with co-ordinates: "+p.xpoints[0]+", "+p.ypoints[0]);			
		}

		int width = imagePlus.getWidth();
		int height = imagePlus.getHeight();
		int depth = imagePlus.getStackSize();

		if (maxvesiclesize <0) maxvesiclesize=  width*height*depth;


		if (width * height * depth > Integer.MAX_VALUE) 
		{
			IJ.error("This stack is too large for this plugin (must have less than " + Integer.MAX_VALUE + " points.");
			return;
		}

		String[] materialList = null;

		//		AmiraParameters parameters = null;
		//		if (AmiraParameters.isAmiraLabelfield(imagePlus)) {
		//			parameters = new AmiraParameters(imagePlus);
		//			materialList = parameters.getMaterialList();
		//		}



		ImageStack stack = imagePlus.getStack();

		byte[][] sliceDataBytes = null;
		float[][] sliceDataFloats = null;

		if (byteImage) 
		{
			sliceDataBytes = new byte[depth][];
			for (int z = 0; z < depth; ++z) 
			{
				ByteProcessor bp = (ByteProcessor) stack.getProcessor(z+1);
				sliceDataBytes[z] = (byte[]) bp.getPixelsCopy();
			}
		}
		else
		{
			sliceDataFloats = new float[depth][];
			for (int z = 0; z < depth; ++z) 
			{
				FloatProcessor bp = (FloatProcessor) stack.getProcessor(z+1);
				sliceDataFloats[z] = (float[]) bp.getPixelsCopy();
			}
		}

		// Preserve the calibration and colour lookup tables
		// for generating new images of each individual
		// region.
//		Calibration calibration = imagePlus.getCalibration();

//		ColorModel cm = null;
//		if (ImagePlus.COLOR_256 == type) 
//		{
//			cm = stack.getColorModel();
//		}

		ResultsTable rt=ResultsTable.getResultsTable();
		rt.reset();

		//	CancelDialog cancelDialog=new CancelDialog(this);
		//	cancelDialog.show();

		boolean firstTime = true;
//		int tk=0;
		while (true) 
		{
//			tk++;

			if( pleaseStop )
				break;

			/* Find one pixel that's above the minimum, or
			   find the maximum in the case where we're
			   not insisting that all regions are made up
			   of the same color.  These are set in all
			   cases... */

			int initial_x = -1;
			int initial_y = -1;
			int initial_z = -1;

			int foundValueInt = -1;
			float foundValueFloat = Float.MIN_VALUE;
			int maxValueInt = -1;
			float maxValueFloat = Float.MIN_VALUE;

			if (firstTime && startFromPointROI ) 
			{

				initial_x = point_roi_x;
				initial_y = point_roi_y;
				initial_z = point_roi_z;

				if(byteImage)
					foundValueInt = sliceDataBytes[initial_z][initial_y * width + initial_x] & 0xFF;
				else
					foundValueFloat = sliceDataFloats[initial_z][initial_y * width + initial_x];

			} 
			else if (byteImage && startAtMaxValue) 
			{
				//IJ.log("");
				for (int z = 0; z < depth; ++z) {
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							int value = sliceDataBytes[z][y * width + x] & 0xFF;
							//	if(x==78 && y==415){IJ.log("value " + value + "tr" + tr[z][x][y]);}
							if (value > maxValueInt && value>tr[z][x][y] && value>minInt) {
								initial_x = x;
								initial_y = y;
								initial_z = z;
								maxValueInt = value;
							}
						}
					}
				}
				//IJ.log(tk+" found max value:" +maxValueInt+" at: "+initial_x+" "+initial_y+" "+initial_z );

				foundValueInt = maxValueInt;

				/* If the maximum value is below the
				   level we care about, we're done. */

				if (foundValueInt < 0){//valuesOverDouble) {
					break;
				}

			}
			else if (byteImage && !startAtMaxValue) 
			{
				IJ.log("thres2" );
				// Just finding some point in the a region...
				for (int z = 0; z < depth && foundValueInt == -1; ++z) {
					for (int y = 0; y < height && foundValueInt == -1; ++y) {
						for (int x = 0; x < width; ++x) {
							int value = sliceDataBytes[z][y * width + x] & 0xFF;
							if (value > tr[z][x][y]){//valuesOverDouble) {

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
			else 
			{
				IJ.log("thres3" );
				// This must be a 32 bit image and we're starting at the maximum
				assert (!byteImage && startAtMaxValue);

				for (int z = 0; z < depth; ++z) {
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							float value = sliceDataFloats[z][y * width + x];
							if (value > tr[z][x][y]){//valuesOverDouble) {
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
//			float vfloat = foundValueFloat;

			String materialName = null;
			if (materialList != null) {
				materialName = materialList[vint];
			}
			int pointsInQueue = 0;
			int queueArrayLength = 1024;
			int[] queue = new int[queueArrayLength];

			byte[] pointState = new byte[depth * width * height];
			int i = width * (initial_z * height + initial_y) + initial_x;
			pointState[i] = IN_QUEUE;
			queue[pointsInQueue++] = i;

			int pointsInThisRegion = 0;

			//IJ.log("vint :" + vint + "seuil " + ((int) (0.1*vint)));

			while (pointsInQueue > 0) 
			{
				if(pleaseStop)
					break;

				int nextIndex = queue[--pointsInQueue];

				int currentPointStateIndex = nextIndex;
				int pz = nextIndex / (width * height);
				int currentSliceIndex = nextIndex % (width * height);
				int py = currentSliceIndex / width;
				int px = currentSliceIndex % width;

				pointState[currentPointStateIndex] = ADDED;

				if (byteImage) {
					sliceDataBytes[pz][currentSliceIndex] = 0;
				} else {
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



				for (int z = z_min; z <= z_max; ++z) 
				{
					for (int y = y_min; y <= y_max; ++y) 
					{
						for (int x = x_min; x <= x_max; ++x) 
						{

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
								} else {

									//if (neighbourValue <= ((int) (0.1*vint)) || neighbourValue <=minInt){ // for 3D mito{//valuesOverDouble)
									if (neighbourValue <= tr[z][x][y] || neighbourValue <=minInt){ // for 3D mito{//valuesOverDouble) 
										continue;
									}
								}
							} else {

								float neighbourValue = sliceDataFloats[z][newSliceIndex];

								if (neighbourValue <= tr[z][x][y] || neighbourValue <=minInt){// for 3D mito {//valuesOverDouble) 
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

			if(pleaseStop)
				break;

			// So now pointState should have no IN_QUEUE
			// status points...
			Region region;

			if (byteImage) {
				//	IJ.log("region tag t2" + vint);
				region = new Region(vint, materialName, pointsInThisRegion, mustHaveSameValue);
			} else {
				region = new Region(pointsInThisRegion, mustHaveSameValue);
			}

			//IJ.log("size " + pointsInThisRegion);

			if (pointsInThisRegion < minimumPointsInRegionDouble) {
				// System.out.println("Too few points - only " + pointsInThisRegion);
				continue;
			}
			if (pointsInThisRegion > maxvesiclesize) {
				// System.out.println("Too few points - only " + pointsInThisRegion);
				continue;
			}

			//tag only if region not too small or big
			tag++;
			//IJ.log("region tag " + tag);


			if (display || autoSubtract || true) 
			{
				//ImageStack newStack = new ImageStack(width, height);
				for (int z = 0; z < depth; ++z) 
				{
					//byte[] sliceBytes = new byte[width * height];
					for (int y = 0; y < height; ++y) 
					{
						for (int x = 0; x < width; ++x) 
						{
							byte status = pointState[width * (z * height + y) + x];

							if (status == IN_QUEUE) {
								IJ.log("BUG: point " + x + "," + y + "," + z + " is still marked as IN_QUEUE");
							}

							if (status == ADDED) 
							{
								if(region.points<= maxvesiclesize)
								{
									tempres[z][x][y]= (short) tag;//tag;
									region.pixels.add(new Pix(z,x,y));
									region.value=tag;
									//region.intensity=;
								}
								//sliceBytes[y * width + x] = replacementValue;
								//IJ.log("pixel z" + z + "pixel x " + x + "pixel y " + y + "tag " + vint);
							}
						}
					}
					//ByteProcessor bp = new ByteProcessor(width, height);
					//bp.setPixels(sliceBytes);
					//newStack.addSlice("", bp);
				}

				if(region.points<= maxvesiclesize)results.add(region);

				//if(region.value==43 || region.value==2000)test_clustering(region);

			}

			if ((stopAfterNumberOfRegions > 0) && (results.size() >= stopAfterNumberOfRegions)) 
			{
				break;
			}
		}

		Collections.sort(results, Collections.reverseOrder());
		//Collections.sort(results);

		//cancelDialog.dispose();

		//		int index =1;
		//		for (Iterator<Region> it = results.iterator(); it.hasNext();) {
		//			Region r = it.next();
		//			r.value=index;
		//			index ++;
		//			//System.out.println(r.toString());		       			
		////			if( showResults ) {
		////				r.addRow(rt);
		////			}		
		//		}

		if( showResults )
			rt.show("Results");

		if(displ|| save){
			displayRegions(tempres, width, height, depth, channel, displ, save);
		}

		//{
		//			if(channel==0){
		//				regstackx=new ImageStack(width,height);
		//
		//				int min = 0;
		//				int max = Math.max(results.size(), 255 );
		//				for (int z=0; z<depth; z++){
		//					short[] mask_short = new short[width*height];
		//					for (int i=0; i<width; i++) {
		//						for (int j=0; j<height; j++) {  
		//							mask_short[j * width + i]= (short) tempres[z][i][j];
		//						}
		//					}
		//					ShortProcessor sp = new ShortProcessor(width, height);
		//					sp.setPixels(mask_short);
		//					sp.setMinAndMax( min, max );
		//					regstackx.addSlice("", sp);
		//				}
		//			
		//				
		//				//IJ.log("size colormodel"+results.size());
		//				regstackx.setColorModel(backgroundAndSpectrum(Math.min(results.size(),255)));				
		//				regsresultx.setStack("Regions X",regstackx);
		//				//regsresultx.setColorModel(backgroundAndSpectrum(Math.min(results.size(),255)));
		//				//regsresultx.setDisplayRange(0,results.size()+1);
		//				//IJ.run(regsresultx,"3-3-2 RGB", "");
		//				regsresultx.show(); 
		//				regsresultx.setActivated();
		//
		//			}
		//			else
		//			{
		//
		//				regstacky=new ImageStack(width,height);
		//
		//				int min = 0;
		//				int max = Math.max(results.size(), 255 );
		//				for (int z=0; z<depth; z++){
		//					short[] mask_short = new short[width*height];
		//					for (int i=0; i<width; i++) {
		//						for (int j=0; j<height; j++) {  
		//							mask_short[j * width + i]= (short) tempres[z][i][j];
		//						}
		//					}
		//					ShortProcessor sp = new ShortProcessor(width, height);
		//					sp.setPixels(mask_short);
		//					sp.setMinAndMax( min, max );
		//					regstacky.addSlice("", sp);
		//				}
		//			
		//				
		//				//IJ.log("size colormodel"+results.size());
		//				regstacky.setColorModel(backgroundAndSpectrum(Math.min(results.size(),255)));				
		//				regsresulty.setStack("Regions Y",regstacky);
		//				//regsresultx.setColorModel(backgroundAndSpectrum(Math.min(results.size(),255)));
		//				//regsresultx.setDisplayRange(0,results.size()+1);
		//				//IJ.run(regsresultx,"3-3-2 RGB", "");
		//				regsresulty.show(); 
		//				regsresulty.setActivated();
		//
		//
		//			}
		//		}
	}



	public void displayRegions(short [][][] regions, int width,int height,int depth, int channel, boolean displ, boolean save){
		if(channel==0){
			regstackx=new ImageStack(width,height);

			int min = 0;
			int max = Math.max(results.size(), 255 );
			for (int z=0; z<depth; z++){
				short[] mask_short = new short[width*height];
				for (int i=0; i<width; i++) {
					for (int j=0; j<height; j++) {  
						mask_short[j * width + i]= (short) regions[z][i][j];
					}
				}
				ShortProcessor sp = new ShortProcessor(width, height);
				sp.setPixels(mask_short);
				sp.setMinAndMax( min, max );
				regstackx.addSlice("", sp);
			}


			//IJ.log("size colormodel"+results.size());
			regstackx.setColorModel(backgroundAndSpectrum(Math.min(results.size(),255)));				
			regsresultx.setStack("Regions X",regstackx);
			//regsresultx.setColorModel(backgroundAndSpectrum(Math.min(results.size(),255)));
			//regsresultx.setDisplayRange(0,results.size()+1);
			//IJ.run(regsresultx,"3-3-2 RGB", "");


			//			if(displ && !save){
			//				//IJ.log("displaying");
			//				
			//			regsresultx.show(); 
			//			regsresultx.setActivated();
			//			}

			//IJ.log("in fcr dispcolors" + Analysis.p.dispcolors);
			if(displ && Analysis.p.dispcolors && Analysis.p.dispwindows){// && !Analysis.p.save_images
				regsresultx.setTitle("Colorized objectsfirst, channel 1");
				regsresultx.show();
			}

			if(displ && Analysis.p.displabels  && Analysis.p.dispwindows){// && !Analysis.p.save_images
				ImagePlus dupx= regsresultx.duplicate();
				dupx.setTitle("Labelized objects, channel 1");
				IJ.run(dupx, "Grays", "");
				dupx.show();
			}



			String savepath;
			if(save && Analysis.p.save_images && Analysis.p.displabels){
				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1" +".zip";
				IJ.saveAs(regsresultx, "ZIP", savepath);
			}

			if(save && Analysis.p.save_images && Analysis.p.dispcolors){
				IJ.run(regsresultx,"RGB Color", "");
				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1_RGBfirst" +".zip";
				IJ.saveAs(regsresultx, "ZIP", savepath);
			}

			//			if (save){
			//				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1" +".zip";
			//				IJ.saveAs(regsresultx, "ZIP", savepath);
			//				
			//				
			//				IJ.run(regsresultx,"RGB Color", "");
			//				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1_RGB" +".zip";
			//				IJ.saveAs(regsresultx, "ZIP", savepath);
			////				ImagePlus imx=regsresultx.duplicate();
			////				ImageConverter imc = new ImageConverter(imx);
			////				imc.convertToRGB();
			////				FileSaver fs= new FileSaver(regsresultx);
			////				String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1" +".tif";
			////				if (Analysis.p.nz >1) fs.saveAsTiffStack(savepath);
			////				else fs.saveAsTiff(savepath);	
			//			}

		}
		else
		{

			regstacky=new ImageStack(width,height);

			int min = 0;
			int max = Math.max(results.size(), 255 );
			for (int z=0; z<depth; z++){
				short[] mask_short = new short[width*height];
				for (int i=0; i<width; i++) {
					for (int j=0; j<height; j++) {  
						mask_short[j * width + i]= (short) regions[z][i][j];
					}
				}
				ShortProcessor sp = new ShortProcessor(width, height);
				sp.setPixels(mask_short);
				sp.setMinAndMax( min, max );
				regstacky.addSlice("", sp);
			}


			//IJ.log("size colormodel"+results.size());
			regstacky.setColorModel(backgroundAndSpectrum(Math.min(results.size(),255)));				
			regsresulty.setStack("Regions Y",regstacky);
			//regsresultx.setColorModel(backgroundAndSpectrum(Math.min(results.size(),255)));
			//regsresultx.setDisplayRange(0,results.size()+1);
			//IJ.run(regsresultx,"3-3-2 RGB", "");


			if(displ && Analysis.p.dispcolors  && Analysis.p.dispwindows){// && !Analysis.p.save_images
				regsresulty.setTitle("Colorized objects, channel 2");
				regsresulty.show();
			}

			if(displ && Analysis.p.displabels  && Analysis.p.dispwindows){// && !Analysis.p.save_images
				ImagePlus dupy= regsresulty.duplicate();
				dupy.setTitle("Labelized objects, channel 2");
				IJ.run(dupy, "Grays", "");
				dupy.show();
			}



			String savepath;
			if(save && Analysis.p.save_images && Analysis.p.displabels){
				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c2" +".zip";
				IJ.saveAs(regsresulty, "ZIP", savepath);
			}

			if(save && Analysis.p.save_images && Analysis.p.dispcolors){
				IJ.run(regsresulty,"RGB Color", "");
				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c2_RGB" +".zip";
				IJ.saveAs(regsresulty, "ZIP", savepath);
			}




			//			if(displ && !save){
			//			regsresulty.show(); 
			//			regsresulty.setActivated();
			//			}
			//			
			//			if (save){
			//				String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1" +".zip";
			//				IJ.saveAs(regsresulty, "ZIP", savepath);
			//				
			//				
			//				IJ.run(regsresulty,"RGB Color", "");
			//				savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1_RGB" +".zip";
			//				IJ.saveAs(regsresulty, "ZIP", savepath);
			//				
			//				
			//				
			////				ImagePlus imy=regsresulty.duplicate();
			////				ImageConverter imc = new ImageConverter(imy);
			////				imc.convertToRGB();
			////				FileSaver fs= new FileSaver(imy);
			////				String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_seg_c1" +".tif";
			////				if (Analysis.p.nz >1) fs.saveAsTiffStack(savepath);
			////				else fs.saveAsTiff(savepath);	
			//			}

		}

	}


//	private void test_clustering(Region r){
//		int nk=5;//3
//		double [] pixel = new double[1];
//		double [] levels= new double[nk];
//
//		Dataset data = new DefaultDataset();
//		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
//			Pix p = it.next();
//			int i=p.px;
//			int j=p.py;
//			int z=p.pz;
//
//			//IJ.log("i j z " +i +" " +j+ " "+ z +"val" +  (softmask[z][i][j]& 0xFF) );
//			pixel[0]=softmask[z][i][j] & 0xFF;
//			Instance instance = new DenseInstance(pixel);
//			data.add(instance);
//		}
//		/* Create Weka classifier */
//		SimpleKMeans xm = new SimpleKMeans();
//		try{
//			xm.setNumClusters(2);//3
//			xm.setMaxIterations(100);
//		}catch (Exception ex) {}
//
//		/* Wrap Weka clusterer in bridge */
//		Clusterer jmlxm = new WekaClusterer(xm);
//		/* Perform clustering */
//		Dataset[] data2 = jmlxm.cluster(data);
//		/* Output results */
//		//System.out.println(clusters.length);
//
//
//		nk=data2.length;//get number of clusters  really found (usually = 3 = setNumClusters but not always)
//		for (int i=0; i<nk; i++) {  
//			//Instance inst =DatasetTools.minAttributes(data2[i]);
//			Instance inst =DatasetTools.average(data2[i]);
//			levels[i]=((float)inst.value(0)) / 255;
//		}
//
//
//		Arrays.sort(levels);
//		IJ.log("");
//		IJ.log("levels :");
//		IJ.log("level 1 : " + levels[0]);
//		IJ.log("level 2 : " + levels[1]);
//
//
//
//		////////3
//		/* Create Weka classifier */
//		xm = new SimpleKMeans();
//		try{
//			xm.setNumClusters(3);//3
//			xm.setMaxIterations(100);
//		}catch (Exception ex) {}
//
//		/* Wrap Weka clusterer in bridge */
//		jmlxm = new WekaClusterer(xm);
//		/* Perform clustering */
//		data2 = jmlxm.cluster(data);
//		/* Output results */
//		//System.out.println(clusters.length);
//
//
//		nk=data2.length;//get number of clusters  really found (usually = 3 = setNumClusters but not always)
//		for (int i=0; i<nk; i++) {  
//			//Instance inst =DatasetTools.minAttributes(data2[i]);
//			Instance inst =DatasetTools.average(data2[i]);
//			levels[i]=inst.value(0)  / 255;
//		}
//
//
//		Arrays.sort(levels);
//		IJ.log("");
//		IJ.log("levels :");
//		IJ.log("level 1 : " + levels[0]);
//		IJ.log("level 2 : " + levels[1]);
//		IJ.log("level 3 : " + levels[2]);
//
//
//
//		////////4
//		/* Create Weka classifier */
//		xm = new SimpleKMeans();
//		try{
//			xm.setNumClusters(4);//3
//			xm.setMaxIterations(100);
//		}catch (Exception ex) {}
//
//		/* Wrap Weka clusterer in bridge */
//		jmlxm = new WekaClusterer(xm);
//		/* Perform clustering */
//		data2 = jmlxm.cluster(data);
//		/* Output results */
//		//System.out.println(clusters.length);
//
//
//		nk=data2.length;//get number of clusters  really found (usually = 3 = setNumClusters but not always)
//		for (int i=0; i<nk; i++) {  
//			//Instance inst =DatasetTools.minAttributes(data2[i]);
//			Instance inst =DatasetTools.average(data2[i]);
//			levels[i]=inst.value(0)  / 255;
//		}
//
//
//		Arrays.sort(levels);
//		IJ.log("");
//		IJ.log("levels :");
//		IJ.log("level 1 : " + levels[0]);
//		IJ.log("level 2 : " + levels[1]);
//		IJ.log("level 3 : " + levels[2]);
//		IJ.log("level 4 : " + levels[3]);
//
//
//		////////5
//		/* Create Weka classifier */
//		xm = new SimpleKMeans();
//		try{
//			xm.setNumClusters(5);//3
//			xm.setMaxIterations(100);
//		}catch (Exception ex) {}
//
//		/* Wrap Weka clusterer in bridge */
//		jmlxm = new WekaClusterer(xm);
//		/* Perform clustering */
//		data2 = jmlxm.cluster(data);
//		/* Output results */
//		//System.out.println(clusters.length);
//
//
//		nk=data2.length;//get number of clusters  really found (usually = 3 = setNumClusters but not always)
//		for (int i=0; i<nk; i++) {  
//			//Instance inst =DatasetTools.minAttributes(data2[i]);
//			Instance inst =DatasetTools.average(data2[i]);
//			levels[i]=inst.value(0)  / 255;
//		}
//
//
//		Arrays.sort(levels);
//		IJ.log("");
//		IJ.log("levels :");
//		IJ.log("level 1 : " + levels[0]);
//		IJ.log("level 2 : " + levels[1]);
//		IJ.log("level 3 : " + levels[2]);
//		IJ.log("level 4 : " + levels[3]);
//		IJ.log("level 5 : " + levels[4]);
//	}




}


