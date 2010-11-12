package mosaic.interaction.input

import ij.IJ
import ij.Prefs
import mosaic.interaction.ScalalaUtils

object MatlabData {
	
	
	def readModelInput:(Array[Int],(Array[Double] => Boolean),Array[Array[Double]],Array[Array[Double]])= {
		
		val path = Prefs.get("ia.matlabPath", "")
		val yName = Prefs.get("ia.MatrixY", "")
		val xName = Prefs.get("ia.MatrixX", "")
		var y: Array[Array[Double]] = null
		var x: Array[Array[Double]] = null
		
		try {
			y = ScalalaUtils.matrix2Array(ReadMat.getMatrix(path,yName))
			x = ScalalaUtils.matrix2Array(ReadMat.getMatrix(path,xName))		
		} catch {
			case e:java.io.FileNotFoundException => IJ.showMessage("File not found: "+ path)
		}
			val dx = Prefs.get("ia.dx", 1).toInt
			val dy = Prefs.get("ia.dy", 1).toInt
			val dz = Prefs.get("ia.dz", 1).toInt
			
			(Array(dx,dy,dz), (_) =>true, y, x)
	}

}