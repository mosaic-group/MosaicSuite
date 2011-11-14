package mosaic.paramopt;

public interface IParameterRankingController {
	/**
	 * Makes the next optimization step based on the current ranking of the
	 * images.
	 */
	public void nextOptimization(int[] ranks);
	
	/**
	 * Repeats an optimization step if the user does not want to select any
	 * of the images because they all are to bad.
	 */
	public void repeatOptimization();
	
	/**
	 * Finishes the optimization process and applies the macro with the 
	 * parameters of the selected image on the original image.
	 */
	public void finishOptimization(int[] ranks);
	
	/**
	 * Aborts the ranking process and with it the optimization.
	 */
	public void abortRanking();
}
