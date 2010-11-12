package mosaic.core.optimization

import scalala.Scalala._
import scalala.tensor.dense._
import scalala.tensor.Vector
import mosaic.interaction.ScalalaUtils

/**
 * @author marksutt
 * 
 * @param q:      values of state density q
 * @param di:     distances at which q is specified
 * @param d_s:	  distances at which p should be sampled
 */
class LikelihoodOptimizer(var q :Vector,var di: Vector,var d_s: Vector, var potentialShape: ((Vector,List[Double]) => Vector)) extends DiffAbstractObjectiveFunction {
	
	var nbrParameter = 2
	var nonParametric = false
	
	/** computes the density of the NN-interaction Gibbs process
	 * @param sampleDistances distances at which p should be sampled
	 * @param potentialShape function handle to shape f of potential
	 * @param potentialParam parameters of potential (epsilon: strength, sigma: length-scale, t: shift along distance axis)
	 * @return value of distance density at d_s
	 */
	def calculatePofD(q :Vector, di: Vector, sampleDistances: Vector, potentialEvaluated: Vector):Vector = {
		
			/*
			% p_of_d: computes the density of the NN-interaction Gibbs process
			%
			% IN:   q:      values of state density q
			%       d:      distances at which q is specified
			%       params: vector with parameters of potential; params(0): strength,
			%               params(1): scale
			%       shape:  function handle to shape f of potential
			%       d_s:    distances at which p should be sampled
			%
			% OUT:  p_s:    value of distance density at d_s*/
//		% STEP 1: compute Z, use trapezoidal rule
//			    g_of_r = exp(-epsilon*shape(d/sigma));
			val g_of_r = new DenseVector(potentialEvaluated.toArray.map(Math.exp(_)))
//			support = g_of_r.*q;
			var support = g_of_r :* this.q value
//			integrand = (support(1:end-1) + support(2:end))/2;
			var integrand = new DenseVector(support.size -1)
			integrand(0 until integrand.size) = support(0 until (support.size-1))
				var intTemp = new DenseVector(support.size -1)
				intTemp(0 until integrand.size) = support(1 until (support.size))
			integrand += intTemp
			integrand /= 2
//			dd = diff(d);
			val diArray = this.di.toArray
			val diff: Vector = new DenseVector (for ((x,y) <-  diArray.take(di.size-1).zip(diArray.drop(1))) yield y-x)
			val Z = sum(integrand :* diff)
//			% STEP 2: compute p(d)
			val p = g_of_r :* this.q * 1/Z
//			% STEP 3: sample p(d) at d_s
//			p_s = interp1(d,p,d_s,'linear');
			val pOfD = ScalalaUtils.interpolate(this.di, p, sampleDistances)
			pOfD
	}

	
	/** computes the negative log-likelihood of the NN-interaction Gibbs process
	 * @param sampleDistances observed distances
	 * @param potentialShape function handle to shape f of potential
	 * @param potentialParam parameters of potential (epsilon: strength, sigma: length-scale, t: shift along distance axis)
	 * @return value of negative log-likelihood
	 * @see calculatePofD
	 */
	def negLogLikelihood(q :Vector, di: Vector, sampleDistances: Vector, potentialShape: ((Vector,List[Double]) => Vector), potentialParam: List[Double]):Double = {
		val params = potentialParam ::: List(0d) // the weight of the last point is fixed to 0.
		
		val fEvaluated:Vector = potentialShape(this.di,params.tail) * (-params(0))

		val p = calculatePofD(q :Vector, di: Vector, sampleDistances, fEvaluated)
		val logP = new DenseVector(p.toArray.map(Math.log(_)))
		val loglikeli:Double = -sum(logP)
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
		if (nonParametric) {
			var weightsPenalty = 0d
			val s = 2 //TODO hard coded parameter: smoothness of non parametric potentials
			val wp:Vector = new DenseVector(params.drop(3).toArray)
			val diffPairs = wp(0 until (wp.size-1)).zip( wp(1 until wp.size))
			weightsPenalty = diffPairs.map(x => {val tmp = (x._1 - x._2)/s; tmp*tmp}).reduceLeft(_+_)
			loglikeli + weightsPenalty
		} else {
			loglikeli
		}
	}	
	
	def pooledNegLogliklihood(qCell:Array[Vector],dCell:Array[Vector],DCell:Array[Vector], potentialShape: ((Vector,List[Double]) => Vector), potentialParam: List[Double]): Double = {
		var nll = 0d;
		for (i <- Iterator.range(0, qCell.length)) {
			var q = qCell(i);
			var d = dCell(i);
			var D = DCell(i);
			
			var nllt = negLogLikelihood(q,d,D,potentialShape,potentialParam);

			nll = nll + nllt;
		}
		nll
	}
	
	/** 
	 * @param data  a point (candidate solution) in the pre-image of the objective function 
     * @return  objective function value of the input search point  
     */
	def valueOf(data: Array[Double]): Double ={
		
		val parameters = mosaic.interaction.PotentialFunctions.defaultParameters(data)
		
		val x = negLogLikelihood(this.q, this.di, this.d_s, this.potentialShape , parameters)
		//println(data(0) + ": Epsilon , nll: "+ x)
		if (x.isNaN) {
			println("problem")
		}
		x
	}
	
	override def isFeasible(data: Array[Double]):Boolean = true
	
	def calculate(x:Vector, batch: Seq[Int]): (Double,Vector) ={
		// TODO calculate gradient at x
		(valueOf(x.toArray),grad(x, valueAt))
	}
	
	def grad(x:Vector, f: Vector => Double):Vector = {
		// forward difference
		// f'(x) = (f(x+h) - f(x))/h
		
		// step size for finite difference
//		val h = 0.1
		//TODO  for the forward-difference approximation of first-order derivatives using function calls
		//  and second-order derivatives using gradient calls: h_j = \sqrt[2]{\eta_j} (1. + | x_j|),
		// src: http://www.uc.edu/sashtml/ormp/chap5/sect28.htm
		val eps = 0.0000000001
		val h = Math.sqrt(eps) * (1. + Math.abs(x(0)))		
		val hInv = 1/h
		val fx = f(x)
		// function value at position (x0, x1, ..., xi + h, .. ,x(n-1)) for each coordinate
		val fh = for (val i <- (0 to (x.size-1))) yield {
			val xTmp = x; 
			xTmp(i) = x(i) + h; 
			f(xTmp)
		}
		val gradi = new DenseVector((fh.map(fhi => (fhi-fx) * hInv)).toArray)
		gradi
	}
		
	override def fullRange: Seq[Int] = {
		//TODO fix full range?
		1 to 10
	}
	
	override def valueAt(x:Vector):Double ={
		valueOf(x.toArray)
	}

}
