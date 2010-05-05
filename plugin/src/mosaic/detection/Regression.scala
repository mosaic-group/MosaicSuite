package mosaic.detection

import org.apache.commons.math.stat.regression._
import javax.vecmath._
import scalala.tensor.dense._
import scalala.Scalala._

object Regression {
	
	def regression(values :Array[Array[Double]]) {
		val regression = new OLSMultipleLinearRegression()
		// example from apache: http://commons.apache.org/math/userguide/stat.html#a1.5_Multiple_linear_regression
		
		val xShifts = new Array[Double](values.length)
		val xPositions = new Array[Array[Double]](values.length,1)
		val xShiftsIter = for (i <- Iterator.range(0, values.length-1)) xShifts(i) = values(i)(0)
		val xPositionsxShiftsIter = for (i <- Iterator.range(0, values.length-1)) xPositions(i)(0) = values(i)(3)
				
		regression.newSampleData(xShifts , xPositions)
 
		val beta = regression.estimateRegressionParameters()
		println("betas: " + beta.map("%.3f".format(_)).mkString(", "))
		
		val x = new DenseVector(xShifts)
		plot(x, x :^ 3, '.')

		 
		// residuals, if needed
		val residuals = regression.estimateResiduals()
		
		val sepp = new DenseVector(5)(0)
	}

}