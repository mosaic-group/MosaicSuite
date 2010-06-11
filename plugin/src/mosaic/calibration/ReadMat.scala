package mosaic.calibration

import scalala.tensor.dense.DenseVector
import java.io.IOException
import com.jmatio.io._
import com.jmatio.types._

object ReadMat {
	val sketchPath = "/Users/marksutt/Documents/MA/matlab/"
	 
	def main(args : Array[String]) : Unit = {
			
		val DCell = ReadMat.readMatCellFile("DCell.mat", "DCell")
		val ddCell = ReadMat.readMatCellFile("ddCell.mat", "dCell")
		val qCell = ReadMat.readMatCellFile("qCell.mat", "qCell")

		val data = DCell.get(0).asInstanceOf[MLDouble].getArray
		val data1 = getDenseVector("DCell.mat", "DCell", 0)
		println("sdfdsf")
		//		val data : Array[Array[Double]] = ((mfr.getMLArray( "resultsAd2" )).asInstanceOf[MLDouble]).getArray;
		println(data.length +" " + data(0).length + " " + data(0)(0));
	}
	
	def getDenseVector(file: String,cellName : String, i: Int = 0): DenseVector = {
		val cell = readMatCellFile(file, cellName)
		val dArray = cell.get(i).asInstanceOf[MLDouble].getArray
		new DenseVector(dArray.map(_.apply(0)))
	}
	
	def readMatCellFile(filename : String, cellName : String):MLCell = {
		var mfr: MatFileReader = null;
		try {
			mfr = new MatFileReader(sketchPath + filename);
		} catch {
		case e :IOException => {e.printStackTrace();exit();}
		}
	    var dataCell: MLCell = null
		if (mfr != null) {
			dataCell = mfr.getMLArray(cellName).asInstanceOf[MLCell]
		}
	    dataCell
	}
}