package mosaic.detection

import org.apache.commons.math.stat.regression._
import scalala.tensor._
import scalala.tensor.dense._

object Regression {
	
	def regression() {
		val regression = new OLSMultipleLinearRegression()
		// example from apache: http://commons.apache.org/math/userguide/stat.html#a1.5_Multiple_linear_regression
		val y = Array(11.0, 12.0, 13.0, 14.0, 15.0, 16.0)
		val x = new Array[Array[Double]](6,6)
		x(0) = Array(1.0, 0, 0, 0, 0, 0)
		x(1) = Array(1.0, 2.0, 0, 0, 0, 0)
		x(2) = Array(1.0, 0, 3.0, 0, 0, 0)
		x(3) = Array(1.0, 0, 0, 4.0, 0, 0)
		x(4) = Array(1.0, 0, 0, 0, 5.0, 0)
		x(5) = Array(1.0, 0, 0, 0, 0, 6.0)
	
		regression.newSampleData(y, x)
 
		val beta = regression.estimateRegressionParameters()
		println("betas: " + beta.map("%.3f".format(_)).mkString(", "))
		 
		// residuals, if needed
		val residuals = regression.estimateResiduals()
		
		val sepp = new DenseVector(5)(0)
	}

}