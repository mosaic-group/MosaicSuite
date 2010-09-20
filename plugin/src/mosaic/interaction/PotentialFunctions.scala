package mosaic.interaction

import scalala.Scalala._
import scalala.tensor._
import scalala.tensor.dense._

object PotentialFunctions {
	
	val functions = Array("Step function","Plummer potential", "Hermquist potential")
	
	def potentialShape(i:Int): ((Vector,Double,Double) => Vector) = {
		i match {
			case 0 => potentialShapeStepFunction(_,_,_)
			case 1 => potentialShapePlummer(_,_,_)
			case 2 => potentialShapeHermquist(_,_,_)
		}
	}
	
	/** step potential function
	 * @param d
	 * @param sigma		scale parameter, default = 1
	 * @param t			shift parameter, default = 0
	 * @return
	 */
	def potentialShapeStepFunction(d: Vector, sigma: Double = 1, t: Double = 0):Vector = {
		val fStep = new DenseVector(d.toArray.map(x => if((x - t)<0) -1d else 0d))
		fStep
	}
	
	/**	% f_plummer: shape of the Plummer potential
	 * @param  d :   	distances
	 * @param sigma :		scale parameter, default = 1
	 * @param t	:		shift parameter, default = 0
	 * @return  f :  	shape function sampled at d
	 */
	def potentialShapePlummer(d: Vector, sigma: Double = 1, t: Double = 0):Vector = {
		val dScaled = new DenseVector(d.toArray.map(x => x/sigma))
		//f = -1./sqrt(d.*d + 1);
		
		// Important: d :* d +1 in Scalala is not the same as in Matlab
		// (d :* d) +1 in Scalala is (d.*d + 1) in Matlab
		val f:Vector = (dScaled :* dScaled) + 1
		val fPlummer = new DenseVector(f.toArray.map(x => -1/ Math.sqrt(x)))
		//f(d<=0) = -1;
		val iter = d.filter(x => x match {case (i,k) => k <= 0; case _ => false})
		for ((i, k) <- iter) fPlummer(i) = -1
		fPlummer
	}
	
	/**	% f_hermquist: shape of the Hermquist potential
	 * @param  d :   	distances
	 * @param sigma :	scale parameter, default = 1
	 * @param t	:		shift parameter, default = 0
	 * @return  f :  	shape function sampled at d
	 */
	def potentialShapeHermquist(d: Vector, sigma: Double = 1, t: Double = 0):Vector = {
		val dScaled = new DenseVector(d.toArray.map(x => x/sigma))
		//f = -1./(d + 1);
		
		val f:Vector = dScaled + 1
		val fHerm = new DenseVector(f.toArray.map(x => -1/x))
		//f(d<=0) = -(1-d);
		val iter = d.filter(x => x match {case (i,k) => k <= 0; case _ => false})
		for ((i, k) <- iter) fHerm(i) = -(1-d(i))
		fHerm
	}
	
	/** Make sure that all 3 parameters (epsilon: strength, sigma: length-scale, t: shift along distance axis) are set.
	 * @param data : parameters, which are already specified
	 * @return parameters with default values added, if they were not specified.
	 */
	def defaultParameters(data: Array[Double]):Array[Double] = {
		val sigma = 1 	//default for sigma: length-scale
		val t = 0 		//default for t: shift along distance axis
		val parameter:Array[Double] = data.length match {
			case 1 => Array(data(0),sigma, t)
			case 2 => Array(data(0),data(1), t)
			case 3 => data
			case _ => throw new IllegalArgumentException("Only arrays with length between 1 and 3.")
		}
		parameter
	}
	
	def parametersToString(parameters: Array[Double]): String = {
		parameters.length match {
			case 1 => "epsilon: " + parameters(0)
			case 2 => "epsilon: " + parameters(0) + " sigma: " + parameters(1)
			case 3 => "epsilon: " + parameters(0) + " sigma: " + parameters(1) + " t: " + parameters(2)
			case _ => throw new IllegalArgumentException("Only arrays with length between 1 and 3.")
		}
	}
}
