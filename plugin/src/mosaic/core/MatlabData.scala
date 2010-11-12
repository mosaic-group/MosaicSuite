package mosaic.core

import mosaic.calibration.ReadMat
import ij.Prefs

object MatlabData {
	
	
	def readModelInput:(Array[Int],(Array[Double] => Boolean),Array[Array[Double]],Array[Array[Double]])= {
		
		val path = Prefs.get("ia.matlabPath", "")
		val yName = Prefs.get("ia.MatrixY", "")
		val xName = Prefs.get("ia.MatrixX", "")
		val y = ScalalaUtils.matrix2Array(ReadMat.getMatrix(path,yName))
		val x = ScalalaUtils.matrix2Array(ReadMat.getMatrix(path,xName))
		
		val dx = Prefs.get("ia.dx", 1).toInt
		val dy = Prefs.get("ia.dy", 1).toInt
		val dz = Prefs.get("ia.dz", 1).toInt
		
		(Array(dx,dy,dz), (_) =>true, y, x)
	}

}