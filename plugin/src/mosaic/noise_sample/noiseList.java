package mosaic.noise_sample;

import net.imglib2.type.numeric.RealType;

/**
 *
 * This class contain a list a all available noise
 *
 * @author Pietro Incardona
 *
 */

public class noiseList
{
    static public String noiseList[] = {"Poisson"};

    /**
     *
     * Create a specified noise distribution
     *
     * @param n String noise type
     * @return psf
     */

    static public <T extends RealType<T>> NoiseSample<T> factory(String f, double dilatation)
    {
        for (int i = 0 ; i <  noiseList.length ; i++)
        {
            if (f.equals("Poisson"))
            {
                return new PoissonSampler<T>(dilatation);
            }
        }
        return null;
    }
};
