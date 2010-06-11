package mosaic.calibration

import mosaic.plugins.CMAES.CMAESProblem
import scalala.Scalala._
import scalala.tensor.dense._

/**
 * @author marksutt
 * 
 * @param q:      values of state density q
 * @param di:     distances at which q is specified
 * @param d_s:	  distances at which p should be sampled
 */
class LikelihoodOptimizer(q :DenseVector, di: DenseVector, d_s: DenseVector) extends CMAESProblem {
	
	
	
	/** computes the density of the NN-interaction Gibbs process
	 * @param sampleDistances distances at which p should be sampled
	 * @param potentialShape function handle to shape f of potential
	 * @param potentialParam parameters of potential (epsilon: strength, sigma: length-scale, t: shift along distance axis)
	 * @return value of distance density at d_s
	 */
	def calculatePofD(sampleDistances: DenseVector, potentialShape: ((DenseVector,Double,Double) => DenseVector), potentialParam: Double*):DenseVector = {
			/*
			% p_of_d: computes the density of the NN-interaction Gibbs process
			%
			% IN:   q:      values of state density q
			%       d:      distances at which q is specified
			%       params: vector with parameters of potential; params(1): strength,
			%               params(2): scale
			%       shape:  function handle to shape f of potential
			%       d_s:    distances at which p should be sampled
			%
			% OUT:  p_s:    value of distance density at d_s*/
//		% STEP 1: compute Z, use trapezoidal rule
//			    g_of_r = exp(-epsilon*shape(d/sigma));
			val fEvaluated = potentialShape(this.di,potentialParam(1), potentialParam(2)) * (-potentialParam(0)) value
			val g_of_r = new DenseVector(fEvaluated.toArray.map(Math.exp(_)))
//			support = g_of_r.*q;
			var support = g_of_r :* this.q value
//			integrand = (support(1:end-1) + support(2:end))/2;
			var integrand = new DenseVector(support.size -1)
			integrand(0 to integrand.size) = support(0 to (support.size-1))
				var intTemp = integrand
				intTemp(0 to integrand.size) = support(1 to (support.size))
			integrand += intTemp
			integrand /= 2
//			dd = diff(d);
			val diArray = this.di.toArray
			val diff: DenseVector = new DenseVector (for ((x,y) <-  diArray.take(di.size-1).zip(diArray.drop(1))) yield y-x)
//			Z = sum(integrand.*dd);
			val Z = sum(integrand :* diff)
//			% STEP 2: compute p(d)
			val p = g_of_r :* this.q * 1/Z
//			% STEP 3: sample p(d) at d_s
//			p_s = interp1(d,p,d_s,'linear');
			interpolate(this.di, p, sampleDistances)
	}

	
	/** computes the negative log-likelihood of the NN-interaction Gibbs process
	 * @param sampleDistances observed distances
	 * @param potentialShape function handle to shape f of potential
	 * @param potentialParam parameters of potential (epsilon: strength, sigma: length-scale, t: shift along distance axis)
	 * @return value of negative log-likelihood
	 * @see calculatePofD
	 */
	def negLogLikelihood(sampleDistances: DenseVector, potentialShape: ((DenseVector,Double,Double) => DenseVector), potentialParam: Double*):Double = {
		val p = calculatePofD(sampleDistances, potentialShape, potentialParam :_*)
		val logP = new DenseVector(p.toArray.map(Math.log(_)))
		-sum(logP)
		/*
		% negloglik: computes the negative log-likelihood of the NN-interaction
		% Gibbs process
		%
		% IN:   q:      values of state density q
		%       d:      distances at which q is specified
		%       params: vector with parameters of potential
		%       shape:  function handle to shape f of potential
		%       D:      observed distances
		%
		% OUT:  nll:    value of negative log-likelihood
		%
		
		function nll = negloglik(q,d,params,shape,D)
		
		p = p_of_d(q,d,params,shape,D);
		
		nll = -sum(log(p));*/
	}
	
	 /** step potential function
	 * @param d
	 * @param sigma
	 * @param t
	 * @return
	 */
	def potentialShapeStepFunction(d: DenseVector, sigma: Double = 0, t: Double = 0):DenseVector = {
		val fStep = new DenseVector(d.toArray.map(x => if((x - t)<0) -1d else 0d))
		fStep
	}

	
	/** interpolates to find yi, the values of the underlying function Y at the points in the vector or array xi
	 * @param Y underlying Function Y
	 * @param x positions where the underlying function Y is specified
	 * @param xi positions where we want to interpolate the function Y
	 * @return yi interpolated values
	 */
	def interpolate(Y:DenseVector, x:DenseVector = this.di, xi: DenseVector): DenseVector = {
		val intervallPos = findIntervall((for ((_,x) <- x.toList) yield x),(for ((_,x) <- xi.toList) yield x))
		
		val interpolated = new DenseVector(xi.size)
		for (((i,d_si),k) <- xi zip intervallPos) interpolated(i)=interpolateLinaer(x(k), Y(k), x(k+1), Y(k+1),d_si)
		interpolated
	}
	
	/** Interpolates at x linearly between two known points given by the coordinates (x0,y0) and (x1,y1)
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param x
	 * @return y(x)
	 */
	def interpolateLinaer(x0 :Double, y0:Double, x1:Double, y1:Double, x:Double) :Double = {
		y0+ (x-x0)* ((y1-y0)/(x1-x0))
	}
	
	/**
	 * pos0 pos1 x0 pos2 pos3 x1 x2 pos4
	 * @param pos (pos0, pos1, pos2, pos3, pos4)
	 * @param x (x0, x1, x2)
	 * @param counter (default parameter,used for recursion)
	 * @return (1, 3, 3) 
	 */
	def findIntervall(pos:List[Double], x:List[Double], counter:Int =0):List[Int] = {
		(pos,x)  match {
			case (_, Nil) => Nil
			case (Nil, _) => Nil
			case (h1Pos::h2Pos::tailPos, headX::tailX) => if (h2Pos > headX && h1Pos <= headX) counter::findIntervall(pos, tailX) else findIntervall(h2Pos::tailPos, x, counter + 1)
			case _ => Nil			
		}
	}		
	
	def objectiveFunction(data: Array[Double]): Double ={
		negLogLikelihood(this.d_s, potentialShapeStepFunction, data :_*)
	}
	
}
