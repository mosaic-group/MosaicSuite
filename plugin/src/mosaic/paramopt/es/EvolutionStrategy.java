package mosaic.paramopt.es;


public interface EvolutionStrategy {

	/**
	 * Sets the dimension of the optimization problem which corresponds to the
	 * number of parameters.
	 * 
	 * @param dim Dimension of the optimization space.
	 */
	public void setDimension(int dim);
	
	/**
	 * Sets the lower and upper bounds for the dimensions.
	 * 
	 * @param lower
	 *            an array of lower bounds - one for each dimension
	 * @param upper
	 *            an array of upper bounds - one for each dimension
	 */
	public void setDimensionBounds(Double[] lower, Double[] upper);
	
	/**
	 * Sets the bounds for the selected dimension. Here null as a bound means
	 * that the dimension is not bound on the respective side.
	 * 
	 * @param dim the dimension for which the bounds are to be set
	 * @param lower
	 *            the lower bound for the specified dimension
	 * @param upper
	 *            the upper bound for the specified dimension
	 */
	public void setDimensionBounds(int dim, Double lower, Double upper);
	
	/**
	 * Set the size of the offspring generations.
	 * 
	 * @param offsprings
	 *            The number of offsprings which are to be generated for each
	 *            generation.
	 */
	public void setOffspringSize(int offsprings);
	
	/**
	 * Returns the minimal number of individuals which need to be selected/rated
	 * (having fitness larger than zero) in order for the strategy to work.
	 * 
	 * @return the minimal number of fitness values which need to be non zero in
	 *         each generation
	 */
	public int getMinSelectionSize();

	/**
	 * Returns the maximal number of individuals which are being used in the
	 * strategy.
	 * 
	 * @return the maximal number of fitness values which are being used in each
	 *         generation
	 */
	public int getMaxSelectionSize();
	
	/**
	 * Set the initial value from which the first generation is to be generated.
	 * 
	 * @param value
	 *            Initial value of the parent in the optimization space.
	 */
	public void setInitialX(double[] x);

	/**
	 * Initializes the strategy by creating the initial population. After this
	 * step none of the strategy parameters can be changed.
	 */
	public void initialize();
	
	/**
	 * Sets the fitness of the individuals of the current population.
	 * 
	 * @param fitness
	 *            the fitness values for the individuals of the current
	 *            population
	 */
	public void setFitness(double[] fitness);
	
	public void makeEvolutionStep();
	
	public void repeatEvolutionStep();

	/**
	 * Performs an evolution step based on the fitness values which have been
	 * assigned and returns the values of the new population.
	 * 
	 * @return the values of the new population or null if some required values
	 *         such as fitness have not been set
	 */
	public double[][] getCurrentPopulation();

}
