package mosaic.noise_sample;

import java.util.Random;

import net.imglib2.type.numeric.RealType;


/**
 * 
 * Class that sample from a Poisson distribution
 * 
 * @author Pietro Incardona
 *
 * @param <T>
 */

class PoissonSampler<T extends RealType<T>> implements NoiseSample<T>
{
	Random mRandomGenerator;
	
	PoissonSampler()
	{
		mRandomGenerator = new Random(8888);
	}

	@Override
	public void sample(T x, T out) 
	{
		int aLambda = (int) x.getRealDouble();
		if(aLambda >= 30) 
		{
			out.setReal((mRandomGenerator.nextGaussian() * Math.sqrt(aLambda) + aLambda + 0.5));
		}
		double p = 1;
		int k = 0;
		double vL = Math.exp(-aLambda);
		do{
			k++;
			p *= mRandomGenerator.nextDouble();
		} while(p >= vL);
		out.setReal(k - 1);
	}
}
