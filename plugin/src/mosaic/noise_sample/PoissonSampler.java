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
	double offset = 0.0;
	
	PoissonSampler(double offset_)
	{
		mRandomGenerator = new Random(8888);
		offset = offset_;
	}

	@Override
	public void sample(T x, T out) 
	{
		int aLambda = (int) (x.getRealDouble());
		aLambda -= offset;
		if(aLambda >= 30) 
		{
			double rnd = (mRandomGenerator.nextGaussian() * Math.sqrt(aLambda) + aLambda + 0.5) + offset;
			if (rnd >= out.getMaxValue())
				rnd = out.getMaxValue();
			out.setReal(rnd);
			return;
		}
		double p = 1;
		int k = 0;
		double vL = Math.exp(-aLambda);
		do{
			k++;
			p *= mRandomGenerator.nextDouble();
		} while(p >= vL);
		if ((k - 1) + offset > x.getMaxValue())
			out.setReal(x.getMaxValue());
		else
		{	
			out.setReal((k - 1) + offset);
		}
	}
}
