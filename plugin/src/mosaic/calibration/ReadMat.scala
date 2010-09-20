package mosaic.calibration

import scalala.tensor.dense.DenseVector
import scalala.tensor._
import scalala.tensor.dense.DenseMatrix
import java.io.IOException
import com.jmatio.io._
import com.jmatio.types._

object ReadMat {
	val sketchPath = "/Users/marksutt/Documents/MA/matlab/"
	 
	def main(args : Array[String]) : Unit = {
			
		val DCell = ReadMat.readMatCellFile(sketchPath +"DCell.mat", "DCell")
		val ddCell = ReadMat.readMatCellFile(sketchPath +"ddCell.mat", "dCell")
		val qCell = ReadMat.readMatCellFile(sketchPath +"qCell.mat", "qCell")

		val data = DCell.get(0).asInstanceOf[MLDouble].getArray
		val data1 = getVectorFromCellFile(sketchPath +"DCell.mat", "DCell", 0)

		//		val data : Array[Array[Double]] = ((mfr.getMLArray( "resultsAd2" )).asInstanceOf[MLDouble]).getArray;
		println(data.length +" " + data(0).length + " " + data(0)(0));
		
		val doubleArray = ReadMat.readMatDoubleArrayFile("/Users/marksutt/Desktop/01green/Output/Mask.mat", "Mask")
		val maskMatrix = ReadMat.getMatrix("/Users/marksutt/Desktop/01green/Output/Mask.mat", "Mask")
		println("")
	}
	
	def getVector(file: String,vectorName : String, i: Int = 0): Vector = {
		val matrix = getMatrix(file,vectorName)
		if (matrix.cols == 1)
			//column vector
			matrix.getCol(0)
		else
			//row vector
			matrix.getRow(0)
	}
	
	def getMatrix(file: String,matrixName : String): Matrix = {
			val dArray = readMatDoubleArrayFile(file, matrixName)			
			val matrix = new DenseMatrix(dArray.size,dArray(0).size)
			 for (i <- 0 until dArray.length) {
				matrix.getRow(i) := new DenseVector(dArray(i))
			}
			matrix
	}
	
	def getVectorFromCellFile(file: String,cellName : String, i: Int = 0): Vector = {
			val cell = readMatCellFile(file, cellName)
			val dArray = cell.get(i).asInstanceOf[MLDouble].getArray
			
//		TODO cleanup 
//		debug
//		val x = 1.21231231231231231245646546546546545646545645646546513134654465464564
//		System.out.format("%.100f%n", dArray(0)(0): java.lang.Double);
//		System.out.format("%.100f%n", x: java.lang.Double);
//		Console.printf("%.100f%n", x);
//		end debug
			
			new DenseVector(dArray.map(_.apply(0)))
	}

	def readMatFile(filename : String):MatFileReader = {
			var mfr: MatFileReader = null;
			try {
					mfr = new MatFileReader(filename);
			} catch {
				case e :IOException => {e.printStackTrace();exit();}
			}
			mfr
	}
	
	def readMatCellFile(filename : String, cellName : String):MLCell = {
		val mfr = readMatFile(filename)
	    var dataCell: MLCell = null
		if (mfr != null) {
			dataCell = mfr.getMLArray(cellName).asInstanceOf[MLCell]
		}
	    dataCell
	}
	
	def readMatDoubleArrayFile(filename : String, doubleArrayName : String):Array[Array[Double]] = {
		val mfr = readMatFile(filename)
	    var data: MLDouble = null
		if (mfr != null) {
			data = mfr.getMLArray(doubleArrayName).asInstanceOf[MLDouble]
		}
	    data.getArray
	}
}