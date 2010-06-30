package mosaic.core

import ij.plugin.Macro_Runner
import ij.plugin.Duplicator
import ij.ImagePlus
import ij.gui.Roi
import ij.plugin.frame.RoiManager

class CellOutline {
	
	var rois : Array[Roi] = null
	var mask : ImagePlus = null

	/** Is this data point inside the cell outlined by a ROI?
	 * @param coord query coordinate
	 * @return true, if data point is inside the cell
	 */
	def inRoi(coord: Array[Double]): Boolean = {
			val x = coord(0).floor.toInt
			val y = coord(1).floor.toInt
			var z = coord(2).floor.toInt
			if ( z == rois.size) {
				z = z-1
			}
			rois(z).contains(x, y)
	}
		
	/** Is this data point inside the cell, outlined by binary mask?
	 * @param coord query coordinate
	 * @return true, if data point is inside the cell
	 */
	private def inCell(coord: Array[Double]): Boolean = {
		val x = coord(0).round.toInt
		val y = coord(1).round.toInt
		val z = coord(2).round.toInt
		mask.setSlice(z)
		val value = mask.getPixel(x, y)
		(value(0) != 0)
	}
	
	/** Generate cell outline
	 * @param imp image, for which we want to generate cell outline
	 */
	def init(imp: ImagePlus) = {
		generateMask(imp) 
		generateRois()
	}
	
	/** Generates one ROI for each slice, based on the binary mask.
	 *  If multiple ROIs are generated, they get combined to one per slice.
	 *  Uses a imageJ macro 'macros/GenerateROIs_.ijm' to generate ROI
	 */
	private def generateRois() {
		// generate ROIs
		mask.show()
		(new Macro_Runner).run("JAR:macros/GenerateROIs_.ijm")
		
		// combine all ROIs in a slice to one ROI
		val roiManager = RoiManager.getInstance
		val list = roiManager.getList
		// Loop over slices
		for (i <- List.range(1, mask.getNSlices + 1)) {
			mask.setSlice(i)
			var currentRois: Array[(Roi, Int)] = roiManager.getRoisAsArray.zipWithIndex
			val (sliceIRois, tmp) =  currentRois.span(x => (roiManager.getSliceNumber(x._1.getName) == i))
			currentRois = tmp
			// Combine all ROIs of current slice i to a single ROI 
			if (sliceIRois.size > 1) {
				roiManager.runCommand("deselect")
				sliceIRois foreach (x => list.select(x._2))
				roiManager.runCommand("combine")
				roiManager.runCommand("add")
				// delete separate ROIs
					roiManager.runCommand("deselect")
					sliceIRois foreach (x => list.select(x._2))
					roiManager.runCommand("delete")
			}
		}
		rois = roiManager.getRoisAsArray
		roiManager.close
	}
	
	/** Generates a binary image as mask for the cell
	 *  Uses imageJ macro 'macros/GenerateMask_.ijm' to generate binary image
	 * @param imp image for which the mask will be generated
	 */
	private def generateMask(imp: ImagePlus) = {
		mask = new Duplicator().run(imp)
		mask.show()
//		IJ.run("GenerateMask ")
		(new Macro_Runner).run("JAR:macros/GenerateMask_.ijm")
	}

}
