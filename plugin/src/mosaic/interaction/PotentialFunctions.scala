package mosaic.interaction

import scalala.Scalala._
import scalala.tensor.dense._

object PotentialFunctions {
	
	val functions = Array("Step function","Plummer potential")
	
	def potentialShape(i:Int): ((DenseVector,Double,Double) => DenseVector) = {
		i match {
			case 0 => potentialShapeStepFunction(_,_,_)
			case 1 => potentialShapePlummer(_,_,_)
		}
	}
	
	/** step potential function
	 * @param d
	 * @param sigma		scale parameter, default = 1
	 * @param t			shift parameter, default = 0
	 * @return
	 */
	def potentialShapeStepFunction(d: DenseVector, sigma: Double = 1, t: Double = 0):DenseVector = {
		val fStep = new DenseVector(d.toArray.map(x => if((x - t)<0) -1d else 0d))
		fStep
	}
	
	/**	% f_plummer: shape of the Plummer potential
	 * @param  d :   	distances
	 * @param sigma :		scale parameter, default = 1
	 * @param t	:		shift parameter, default = 0
	 * @return  f :  	shape function sampled at d
	 */
	def potentialShapePlummer(d: DenseVector, sigma: Double = 1, t: Double = 0):DenseVector = {
		val dScaled = new DenseVector(d.toArray.map(x => x/sigma))
		//f = -1./sqrt(d.*d + 1);
		val f = dScaled :* dScaled + 1
		val fPlummer = new DenseVector(f.toArray.map(x => -1/ Math.sqrt(x)))
		//f(d<=0) = -1;
		val iter = d.filter(x => x match {case (i,k) => k <= 0; case _ => false})
		for ((i, k) <- iter) fPlummer(i) = -1
		fPlummer
	}

}
