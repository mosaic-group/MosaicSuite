/**
 * 
 */
package mosaic.core

import scalala.Scalala._
import scalala.tensor.dense._
import scalala.tensor.Vector

/**
 * @author marksutt
 *
 */
object ScalalaUtils {
	
	/** interpolates to find yi, the values of the underlying function Y at the points in the vector or array xi
	 * @param Y underlying Function Y
	 * @param x positions where the underlying function Y is specified
	 * @param xi positions where we want to interpolate the function Y
	 * @return yi interpolated values
	 */
	def interpolate(x:DenseVector ,Y:DenseVector,  xi: DenseVector): DenseVector = {
		// sort xi
		val xiSorted = xi.toList.sort(_._2 < _._2)
		val (xiSortedPos,xiSortedValues) = xiSorted.unzip
		val intervallPos = findIntervall((for ((_,x) <- x.toList) yield x),(for (x <- xiSortedValues) yield x))
		val interpols = for ((d_si,k) <- xiSortedValues zip intervallPos) 
			yield interpolateLinear(x(k), Y(k), x(k+1), Y(k+1),d_si)
		// sort interpolated values with original indices sort key to get same order as in xi
		val interpolatedUnsorted = interpols.zip(xiSortedPos).sort(_._2 < _._2).unzip._1
		new DenseVector(interpolatedUnsorted.toArray)
	}
	
	/** Interpolates at x linearly between two known points given by the coordinates (x0,y0) and (x1,y1)
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param x
	 * @return y(x)
	 */
//	@inline
	def interpolateLinear(x0 :Double, y0:Double, x1:Double, y1:Double, x:Double) :Double = {
		y0+ (x-x0)* ((y1-y0)/(x1-x0))
	}
	
	/**
	 * e.g. pos0 pos1 x0 pos2 pos3 x1 x2 pos4
	 * @param pos (pos0, pos1, pos2, pos3, pos4)
	 * @param x (x0, x1, x2)
	 * @param counter (default parameter,used for recursion)
	 * @return (1, 3, 3) 
	 */
	def findIntervall(pos:List[Double], x:List[Double], counter:Int =0):List[Int] = {
		(pos,x)  match {
			case (_, Nil) => Nil
			case (Nil, _) => Nil
			case (h1Pos::h2Pos::tailPos, headX::tailX) => {
					if (h2Pos > headX && h1Pos <= headX) 
						counter::findIntervall(pos, tailX, counter) 
					else
						findIntervall(h2Pos::tailPos, x, counter + 1)
			}
			case _ => Nil			
		}
	}	

}