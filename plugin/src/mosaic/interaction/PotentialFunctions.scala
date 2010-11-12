package mosaic.interaction

import scalala.Scalala._
import scalala.tensor._
import scalala.tensor.dense._

object PotentialFunctions {
		
	val functions = Array("Step function","Plummer potential", "Hermquist potential","Non parametric potential")
	val potentials = functions.zipWithIndex.map(i => Potential(i._1,potentialShape(i._2),
			potentialParameters(i._2)._1,potentialParameters(i._2)._2)).toList
			
	val parametricPotentials = potentials.filter(x => !(x.nonParamFlag))
	val nonParametricPotentials = potentials.filter((_.nonParamFlag))
	
	def potentialShape(i:Int): ((Vector,List[Double]) => Vector) = {
		i match {
			case 0 => potentialShapeStepFunction(_,_)
			case 1 => potentialShapePlummer(_,_)
			case 2 => potentialShapeHermquist(_,_)
			case 3 => potentialNonParametric(_,_)
		}
	}
	
	def potentialParameters(i:Int): (Int,Boolean) = {
		i match {
			case 0 => (2,false)
			case 1 => (2,false)
			case 2 => (2,false)
			case 3 => (8,true)
		}
	}
	
	/** step potential function
	 * @param d
	 * @param parameter		List(scale sigma, shift t)
	 * @return
	 */
	def potentialShapeStepFunction(d: Vector, param: List[Double]):Vector = {
		val sigma =param(0)
		val t = param(1)
		val fStep = new DenseVector(d.toArray.map(x => if((x - t)<0) -1d else 0d))
		fStep
	}
	
	/**	% f_plummer: shape of the Plummer potential
	 * @param  d :   	distances
	 * @param sigma :		scale parameter, default = 1
	 * @param t	:		shift parameter, default = 0
	 * @return  f :  	shape function sampled at d
	 */
	def potentialShapePlummer(d: Vector, param: List[Double]):Vector = {
		val sigma =param(0)
		val t = param(1)
		val dScaled = new DenseVector(d.toArray.map(x => (x-t)/sigma))
		//f = -1./sqrt(d.*d + 1);
		
		// Important: d :* d +1 in Scalala is not the same as in Matlab
		// (d :* d) +1 in Scalala is (d.*d + 1) in Matlab
		val f:Vector = (dScaled :* dScaled) + 1
		val fPlummer = new DenseVector(f.toArray.map(x => -1/ Math.sqrt(x)))
		//f(d<=0) = -1;
		val iter = dScaled.filter(x => x match {case (i,k) => k <= 0; case _ => false})
		for ((i, k) <- iter) fPlummer(i) = -1
		fPlummer
	}
	
	/**	% f_hermquist: shape of the Hermquist potential
	 * @param  d :   	distances
	 * @param sigma :	scale parameter, default = 1
	 * @param t	:		shift parameter, default = 0
	 * @return  f :  	shape function sampled at d
	 */
	def potentialShapeHermquist(d: Vector, param: List[Double]):Vector = {
		val sigma =param(0)
		val t = param(1)
		val z = new DenseVector(d.toArray.map(x => (x-t)/sigma))
		//f = -1./(d + 1);
		
		val f:Vector = z + 1
		val fHerm = new DenseVector(f.toArray.map(x => -1/x))
		//f(d<=0) = -(1-d);
		val iter = z.filter(x => x match {case (i,k) => k <= 0; case _ => false})
		for ((i, k) <- iter) fHerm(i) = -(1-z(i))
		fHerm
	}
	
	/**	% f_non_parametric: potential
	 * based on  Jo A. Helmuths Dissertation, chapter 3.2.4.4
	 * wp :   	weights at support points
	 * @param  d 		: distances
	 * @param  param 	: scale: sigma = param(0), shift: t = param(1), wp = param(2:end)
	 * @return  f 		: shape function sampled at d
	 */
	def potentialNonParametric(d: Vector, param: List[Double]):Vector = {
		val wp = new DenseVector(param.drop(2).toArray)
		val P = wp.size //TODO hard coded constant non parametric potential
		val dp = linspace(-5,20,P) //TODO hard coded constant non parametric potential
		val h = dp(1)-dp(0)
		potentialNonParametricBasic(d,dp,wp,h,param(0),param(1))
	}
	
	/** Make sure that all 3 parameters (epsilon: strength, sigma: length-scale, t: shift along distance axis) are set.
	 * @param data : parameters, which are already specified
	 * @return parameters with default values added, if they were not specified.
	 */
	def defaultParameters(data: Array[Double]):List[Double] = {
		val sigma = 1 	//default for sigma: length-scale
		val t = 0 		//default for t: shift along distance axis
		val parameter:List[Double] = data.length match {
			case 1 => List(data(0),sigma, t)
			case 2 => List(data(0),data(1), t)
			case l if l > 2 => data.toList
			case _ => throw new IllegalArgumentException("Only arrays with length between 1 and infinity.")
		}
		parameter
	}
	
	def parametersToString(parameters: Array[Double]): String = {
		parameters.length match {
			case 1 => "epsilon: " + parameters(0)
			case 2 => "epsilon: " + parameters(0) + " sigma: " + parameters(1)
			case l if l > 2 => "epsilon: " + parameters(0) + " sigma: " + parameters(1) + " t: " + parameters(2)
			case _ => throw new IllegalArgumentException("Only arrays with length between 1 and infinity.")
		}
	}
	
	/**	% f_non_parametric: potential
	 * based on  Jo A. Helmuths Dissertation, chapter 3.2.4.4
	 * @param  d :   	distances
	 * @param  dp :   	support points
	 * @param  wp :   	weights at support points
	 * @param  h :   	spacing between support points
	 * @param sigma :	scale parameter, default = 1
	 * @param t	:		shift parameter, default = 0
	 * @return  f :  	shape function sampled at d
	 */
	def potentialNonParametricBasic(d: Vector, dp:Vector, wp:Vector, h:Double, sigma:Double = 1, t:Double= 0):Vector = {
		val dScaled =d.toArray.map(x => (x-t)/sigma)
		val wpdp = wp.toArray.zip(dp.toArray)
		
		def phi(d:Double):Double = {
			@inline
			def summand(x:Double,k:(Double,Double)):Double ={
				k._1 * kappa(d-k._2) + x
			}
			//k(|z|< h) = |z|/h
			@inline
			def kappa(z:Double):Double = z match {
			case z if (Math.abs(z) < h) => Math.abs(z)/h
			case _ => 0
			}

			wpdp.foldLeft[Double](0d)(summand(_,_))
		}	
		
		val nonParamPotential = dScaled.map(phi(_))
		new DenseVector(nonParamPotential)
	}
	
}
