package mosaic.paramopt.es;

import java.util.Random;

public class VarMuFixLambdaWithSigmaAdaption implements EvolutionStrategy {
	
	// Static value used to mark the strategy parameters as not yet initialized.
	public final static int UNINITIALIZED = -1;
	
	// Fitness value assigned to the individuals of the current population.
	private double[] fitness;
	// Dimension of the evolution space.
	private int dimension = UNINITIALIZED;
	// Lower and upper bounds for the values in linear space.
	private Double[] lowerBounds;
	private Double[] upperBounds;
	// Internally used lower bound for the values in linear space.
	private double[] internalLowerBounds;
	// Internally used upper bounds for the values in logarithmic space.
	private double[] internalUpperBounds;
	// The strategy parameter mu which can vary in this implementation.
	private int mu = UNINITIALIZED;
	// The strategy parameter lambda defining the size of the population.
	private int lambda = UNINITIALIZED;
	// Flag to mark whether the strategy has been initialized.
	private boolean initialized = false;
	// The current population of values.
	private double[][] currentPopulation;
	// The initial value which is used to create the first population.
	private double[] initial;
	// The current weighted mean of the population.
	private double[] mean;
	// The strategy parameters associated with the individuals of the
	// population.
	private double[] sigmas;
	// The strategy parameter sigma which is the weighted mean of the sigmas.
	private double sigma;
	// The maximal value for the strategy parameter sigma
	private double sigmaMax;
	// Random number generator used for sampling.
	protected Random random = new Random();
	// The self-adaption learning rate.
	protected double tau;
	
	/**
	 * Creates a new instance of the strategy.
	 */
	public VarMuFixLambdaWithSigmaAdaption() {
		mu = 1;
	}

	/**
	 * Sets the dimension of the problem and resets the bounds for each
	 * dimension to null.
	 * 
	 * @param dim the dimension of the domain in which the optimization takes 
	 *            place
	 */
	public void setDimension(int dim) {
		// Changes to the dimension are only possible if the strategy has not
		// yet been initialized.
		if (initialized)
			return;
		
		// Check if it is a valid dimension.
		if (dim < 1)
			return;
		
		// Set the new dimension and create the bounds arrays.
		dimension = dim;
		lowerBounds = new Double[dimension];
		upperBounds = new Double[dimension];
		// Set the self-adaption learning rate depending on numParams.
		tau = 1.0 / Math.sqrt(2.0 * dimension);
	}

	/**
	 * Sets the upper and lower bounds for each dimension to the specified
	 * values. If a bound is null then it means that the space is not bounded
	 * in the corresponding direction of the dimension.
	 * 
	 * @param lower
	 *            an array containing the lower bounds for each dimension
	 * @param upper
	 *            an array containing the upper bounds for each dimension
	 */
	public void setDimensionBounds(Double[] lower, Double[] upper) {
		// Changes to the bounds are only possible if the strategy has not yet
		// been initialized.
		if (initialized)
			return;
		
		// Check if the arrays are not null and have the right size.
		if (lower == null || upper == null || lower.length != dimension
				|| upper.length != dimension)
			return;
		// Set the dimension bounds.
		for (int i = 0; i < dimension; i++) {
			lowerBounds[i] = lower[i];
			upperBounds[i] = upper[i];
		}
	}

	/**
	 * Sets the upper and lower bound of the specified dimension to the
	 * specified bound values.
	 * 
	 * @param dim the dimension for which the bounds are to be set
	 * @param lower
	 *            the new value of the lower bound for the specified dimension
	 * @param upper
	 *            the new value of the upper bound for the specified dimension
	 */
	public void setDimensionBounds(int dim, Double lower, Double upper) {
		// Changes to the bounds are only possible if the strategy has not yet
		// been initialized.
		if (initialized)
			return;
		
		// Check if dim is a valid dimension.
		if (dim < 0 || dim >= dimension)
			return;
		// Set the new bounds.
		lowerBounds[dim] = lower;
		upperBounds[dim] = upper;
	}

	/**
	 * Sets the value of lambda which is the size of the offspring generation.
	 * 
	 * @param offsprings
	 *            the size of the offspring generation referred to as lambda in
	 *            this strategy
	 */
	public void setOffspringSize(int offsprings) {
		// Changes to the offspring size are only possible if the strategy has
		// not yet been initialized.
		if (initialized)
			return;

		// Check if offsprings is a valid value.
		if (offsprings < 1)
			return;
		// Set the new value for lambda.
		lambda = offsprings;
	}

	/**
	 * Returns the minimal number of individuals which need to be given a
	 * fitness larger than 0 in order for this strategy to work.
	 * 
	 * @return 1 which is the minimal number of individuals which need to have
	 *         fitness larger than 0 for this strategy to work
	 */
	public int getMinSelectionSize() {
		// Return 1 as this is a strategy with variable mu.
		return 1;
	}
	
	/**
	 * Returns the maximal number of individuals which are being used in the
	 * strategy.
	 * 
	 * @return the maximal number of fitness values which are being used in each
	 *         generation
	 */
	public int getMaxSelectionSize() {
		return lambda;
	}

	/**
	 * Sets the initial value from which the first population is generated.
	 * 
	 * @param x the initial value
	 */
	public void setInitialX(double[] x) {
		// Changes to the initial x are only possible if the strategy has not
		// yet been initialized.
		if (initialized)
			return;

		// Check if the x array is not null and has the right dimension
		if (x == null || x.length != dimension)
			return;
		// Set the initial values.
		initial = x;
	}

	/**
	 * Initializes the strategy by generating an initial population based on the
	 * initial value. After initialization the strategy parameters cannot be
	 * modified anymore.
	 */
	public void initialize() {
		// Initialization is only possible if the strategy has not yet been
		// initialized.
		if (initialized)
			return;
		
		// Check if dimension (implicitly), lambda and initial value have been
		// set.
		if (initial == null || lambda == UNINITIALIZED)
			return;
		
		// Create the internal lower bounds
		internalLowerBounds = new double[dimension];
		for (int i = 0; i < dimension; i++) {
			// Use the defined lower bound and if none is set then guess one.
			if (lowerBounds[i] == null) {
				// If the initial value is not zero than take the negative of
				// the absolute value of the initial value times 100 as a guess
				// for the lower bound. Otherwise we make a bold guess and set
				// it to -100.
				if (Math.abs(initial[i]) > 0.0)
					internalLowerBounds[i] = -100 * Math.abs(initial[i]);
				else
					internalLowerBounds[i] = -100;
			} else
				internalLowerBounds[i] = lowerBounds[i];
		}
		
		// Create the internal upper bounds.
		internalUpperBounds = new double[dimension];
		for (int i = 0; i < dimension; i++) {
			if (upperBounds[i] == null)
				internalUpperBounds[i] = Double.MAX_VALUE;
			else
				internalUpperBounds[i] = upperBounds[i];
		}
		internalUpperBounds = externalToInternal(internalUpperBounds);
		
		// Set the mean.
		mean = externalToInternal(initial);
		
		// Compute the initial sigma as 20% of the largest dimensional extent.
		sigma = 0.0;
		for (int i = 0; i < dimension; i++) {
			if (internalUpperBounds[i] > sigma)
				sigma = internalUpperBounds[i];
		}
		sigmaMax = 0.5 * sigma;
		sigma *= 0.2;
		
		// Create initial population
		createNewPopulation();
		
		// Mark strategy as initialized
		initialized = true;
	}
	
	/**
	 * Sets the fitness of the individuals of the current population to the
	 * specified values.
	 * 
	 * @param fitness
	 *            the fitness values for the individuals of the population
	 */
	public void setFitness(double[] fitness) {
		// Check if the strategy has been initialized and if not return
		// immediately.
		if (!initialized)
			return;

		// Check fitness array and its values.
		if (fitness == null || fitness.length != lambda)
			return;
		int newMu = 0;
		for (double f : fitness)
			if (f > 0.0)
				newMu++;
		if (newMu < 1)
			return;
		// There are newMu >= 1 fitness values larger than zero.
		// Set the new mu.
//		mu = newMu > (lambda / 2) + 1 ? (lambda / 2) + 1 : newMu;
		// Set the fitness.
//		this.fitness = normalize(fitness);
		this.fitness = fitness;
		sortDescByFitness();
	}

	/**
	 * Makes an evolution step by creating a new population based on the fitness
	 * which has been assigned to the individuals of the population.
	 */
	public void makeEvolutionStep() {
		// Check if the strategy has been initialized and if not return
		// immediately.
		if (!initialized)
			return;

		// Check if fitness values have been set.
		if (fitness == null)
			return;
		
		// Update the mean and the step size sigma.
		updateMean();
		updateSigma();
		
		// Compute the next generation.
		createNewPopulation();
		
		// Keep a reference to the last fitness and set fitness to null.
		fitness = null;
	}

	/**
	 * Repeats an evolution step by slightly decreasing the step size sigma and
	 * creating a new population based on the new step size.
	 */
	public void repeatEvolutionStep() {
		// Check if the strategy has been initialized and if not return
		// immediately.
		if (!initialized)
			return;
		
		// Repeat the current evolution step with a slightly smaller step size
		// sigma.
		sigma *= 0.8;
		for (int i = 0; i < lambda; i++)
			sigmas[i] = sigmas[i] * 0.8;
		createNewPopulation();
	}

	/**
	 * Returns the current population or null if the strategy has not yet been 
	 * initialized.
	 * 
	 * @return the current population
	 */
	public double[][] getCurrentPopulation() {
		// Check if the strategy has been initialized and if not return
		// immediately.
		if (!initialized)
			return null;

		// After initialization there is always a current population so return
		// the current population after a transformation to external space.
		double[][] result = new double[lambda][];
		for (int i = 0; i < lambda; i++)
			result[i] = internalToExternal(currentPopulation[i]);
		return result;
	}
	
	/**
	 * Transforms a value from linear space to logarithmic space which is used
	 * internally for the values of the population.
	 * 
	 * @param value
	 *            the value which has to be transformed into logarithmic space
	 * @return the transformed value
	 */
	private double[] externalToInternal(double[] value) {
		if (value == null || value.length != dimension)
			return null;
		double[] result = new double[dimension];
		for (int i = 0; i < dimension; i++) {
			// If the value is less than the internal lower bound, then project
			// it onto the lower bound. (This case should never occur)
			if (value[i] < internalLowerBounds[i])
				value[i] = internalLowerBounds[i];
			// Make a log transformation of the value using the lower bound.
			result[i] = Math.log(1 - internalLowerBounds[i] + value[i]);
		}
		return result;
	}

	/**
	 * Transforms a value from the internally used logarithmic space back into
	 * linear space.
	 * 
	 * @param value
	 *            the value which has to be transformed
	 * @return the transformed value
	 */
	private double[] internalToExternal(double[] value) {
		if (value == null || value.length != dimension)
			return null;
		double[] result = new double[dimension];
		for (int i = 0; i < dimension; i++) {
			// Make an exponential transformation of the value using the lower
			// bound.
			result[i] = Math.exp(value[i]) - 1 + internalLowerBounds[i];
		}
		return result;
	}
	
	/**
	 * Checks whether a given value is within the bounds of the evolution
	 * domain.
	 * 
	 * @param value
	 *            the value which is being checked
	 * @return true if the value satisfies all bounds and false otherwise
	 */
//	private boolean isWithinBounds(double[] value) {
//		// Check if value is not null and has the right dimension
//		if (value == null || value.length != dimension)
//			return false;
//		// If the value in any of the dimensions is out of bounds then return
//		// false.
//		for (int i = 0; i < dimension; i++)
//			if (value[i] < 0 || value[i] > internalUpperBounds[i])
//				return false;
//		// All values passed the bounds check.
//		return true;
//	}
	
	/**
	 * Checks if the given value is within the bounded space and if it is not
	 * then it returns the projection of that value into the bounded space.
	 * 
	 * @param value
	 *            the value which is checked and projected if outside of the
	 *            bounds
	 */
	private double[] validateValue(double[] value) {
		// Create a variable for the return value.
		double[] result = null;
		// Check if value has the right dimension.
		if (value != null && value.length == dimension) {
			// Initialize the return array.
			result = new double[dimension];
			// Iterate over all dimensions and check the bounds.
			for (int i = 0; i < dimension; i++) {
				// If value in dimension i is larger than the upper bound then
				// set it to the upper bound.
				if (value[i] > internalUpperBounds[i])
					value[i] = internalUpperBounds[i];
				// If value in dimension i is smaller than 0 then set it to 0.
				else if (value[i] < 0)
					value[i] = 0;
			}
		}
		// Return the results array or null.
		return result;
	}

	/**
	 * Creates a new population based on the current mean and sigma.
	 */
	private void createNewPopulation() {
		double newPopulation[][] = new double[lambda][];
		double sigmas[] = new double[lambda];
		for (int i = 0; i < lambda; i++) {
			// Sample an individual value.
			newPopulation[i] = validateValue(sampleValue());
			// Instead of re-sampling an individual until it is within bounds
			// just sample it once and project it into the bounded space.
//			newPopulation[i] = sampleValue();
//			// Resample until the value is within the bounds.
//			while (!isWithinBounds(newPopulation[i]))
//				newPopulation[i] = sampleValue();
			// Sample the strategy parameter for that individual
			sigmas[i] = sampleSigma();
		}
		currentPopulation = newPopulation;
		this.sigmas = sigmas;
	}

	/**
	 * Samples a new individual around the mean with step size sigma.
	 * 
	 * @return a new individual value
	 */
	private double[] sampleValue() {
		double[] sample = new double[dimension];
		for(int i = 0; i < dimension; i++) {
			sample[i] = mean[i] + sigma * random.nextGaussian();
		}
		return sample;
	}
	
	/**
	 * Samples a new strategy parameter sigma around the mean sigma.
	 * 
	 * @return a new strategy parameter sigma
	 */
	private double sampleSigma() {
		double result = sigma * Math.exp(tau * random.nextGaussian());
		return result > sigmaMax ? sigmaMax : result;
	}
	
	/**
	 * Updates the mean as the weighted average of the current individuals.
	 */
	private void updateMean() {
		mean = new double[dimension];
		for (int i = 0; i < mu; i++)
			for (int j = 0; j < dimension; j++)
				mean[j] += currentPopulation[i][j] / mu;
//				mean[j] += fitness[i] * currentPopulation[i][j];
	}
	
	/**
	 * Updates the strategy parameter sigma as the weighted mean of the strategy
	 * parameters sigma.
	 */
	private void updateSigma() {
		sigma = 0.0;
		for (int i = 0; i < mu; i++)
			sigma += sigmas[i] / mu;
//			sigma += fitness[i] * sigmas[i];
	}

	/**
	 * This method takes an array of double and returns its normalized version.
	 * 
	 * @param in  An array of type <Code>double</Code> whose entries sum up to a positive number.
	 * @return    The normalized version of <Code>in</Code> such that entries sum up to 1.
	 */
	protected double[] normalize(double[] in) {
		double[] out = new double[in.length];
		double sum = 0.0;
		for(int i=0; i<in.length; i++) {
			sum += in[i];
		}
		if(sum <= 0.0)
			throw new IllegalArgumentException("Cannot be normalized because entries sum up to " + sum + "!");
		for(int i=0; i<out.length; i++) {
			out[i] = in[i] / sum;
		}
		return out;
	}
	
	protected void sortDescByFitness() {
		double[] tmp = fitness.clone();
		double max = 0.0;
		int index = 0;
		double[][] sortedPop = new double[lambda][];
		double[] sortedFit = new double[lambda];
		double[] sortedSig = new double[lambda];
		for (int i = 0; i < lambda; i++) {
			max = 0.0;
			for (int j = 0; j < lambda; j++) {
				if (tmp[j] > max) {
					max = tmp[j];
					index = j;
				}
			}
			sortedFit[i] = tmp[index];
			sortedSig[i] = sigmas[index];
			sortedPop[i] = currentPopulation[index];
			tmp[index] = 0;
		}
		currentPopulation = sortedPop;
		fitness = sortedFit;
		sigmas = sortedSig;
	}
	
	protected void sortAscByFitness() {
		double[] tmp = fitness.clone();
		double min;
		int index = 0;
		double[][] sortedPop = new double[lambda][];
		double[] sortedFit = new double[lambda];
		double[] sortedSig = new double[lambda];
		for (int i = 0; i < lambda; i++) {
			min = Double.POSITIVE_INFINITY;
			for (int j = 0; j < lambda; j++) {
				if (tmp[j] < min) {
					min = tmp[j];
					index = j;
				}
			}
			sortedFit[i] = tmp[index];
			sortedSig[i] = sigmas[index];
			sortedPop[i] = currentPopulation[index];
			tmp[index] = Double.POSITIVE_INFINITY;
		}
		currentPopulation = sortedPop;
		fitness = sortedFit;
		sigmas = sortedSig;
	}

	protected String array2String(double[] arr) {
		String tmp = "[";
		for(int i=0; i<arr.length; i++) {
			tmp += (i>0 ? "," : "") + arr[i];
		}
		return tmp + "]";
	}
}
