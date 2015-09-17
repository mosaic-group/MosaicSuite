package mosaic.noise_sample;

import java.util.Random;
import java.util.Vector;

import net.imglib2.histogram.Histogram1d;
import net.imglib2.type.numeric.RealType;

/**
 *
 * Class that sample from a generic histogram Noise distribution when data is
 * provided otherwise is interpolated
 *
 * @author Pietro
 *
 * @param <T> intensity type
 */

public class GenericNoiseSampler<T extends RealType<T>> implements NoiseSample<T>
{
    /**
     *
     *
     * @author Pietro Incardona
     *
     */

    Random rnd;
    Class<T> cls = null;
    T center;

    private class InterpolateHistogram
    {
        Ihist h1;
        Ihist h2;

        /**
         *
         * Create an interpolation between histogram
         *
         * @param h1_
         * @param h2_
         */

        InterpolateHistogram(Ihist h1_, Ihist h2_)
        {
            // check that h2.inte > h1.inte otherwise store in the other
            // way around

            if (h2_.intensity.getRealDouble() > h1_.intensity.getRealDouble())
            {
                h1 = h1_;
                h2 = h2_;
            }
            else
            {
                h1 = h2_;
                h2 = h1_;
            }
        }

        /**
         *
         * Get bin value of the interpolated Histogram
         *
         * @param inte Intensity value
         * @param bin that we require
         * @return occurrence as integer
         */

        int get(T inte, T bin, Class<T> cls)
        {
            // calculate distance from h1 and h2

            double h1dist = h1.intensity.getRealDouble() - inte.getRealDouble();
            double h2dist = h2.intensity.getRealDouble() - inte.getRealDouble();
            double h2h1dist = h2.intensity.getRealDouble() - h1.intensity.getRealDouble();

            // calculate ratio

            double r1dist = h1dist / h2h1dist;
            double r2dist = h2dist/ h2h1dist;

            long och1 = 0;
            long och2 = 0;

            // calculate shift

            T binsh1;
            try {
                binsh1 = cls.newInstance();
                binsh1.setReal(h1.intensity.getRealDouble() + h1dist);
                T binsh2 = cls.newInstance();
                binsh2.setReal(h2.intensity.getRealDouble() + h2dist);

                och1 = h1.hist.frequency(binsh1);
                och2 = h2.hist.frequency(binsh2);
            }
            catch (InstantiationException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }

            return (int) (och1 * r1dist + och2 * r2dist);
        }

        /**
         *
         * Return the integral of the histogram
         *
         * @return
         */

        long integral(T inte)
        {
            // calculate distance from h1 and h2

            double h1dist = h1.intensity.getRealDouble() - inte.getRealDouble();
            double h2dist = h2.intensity.getRealDouble() - inte.getRealDouble();
            double h2h1dist = h2.intensity.getRealDouble() - h1.intensity.getRealDouble();

            // calculate ratio

            double r1dist = h1dist / h2h1dist;
            double r2dist = h2dist/ h2h1dist;

            // calculate the integral

            return (int) (h1.integral * r1dist + h2.integral * r2dist);
        }
    }

    private class Ihist
    {
        T intensity;
        Histogram1d<T> hist;
        long integral;
    };

    Vector<Ihist> inteHist;

    public GenericNoiseSampler(Class<T> cls_)
    {
        inteHist = new Vector<Ihist>();
        cls = cls_;
        rnd = new Random();

        try {
            center = cls.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * Set an histogram in the list
     *
     * @param intensity of the histogram
     * @param hist Histogram
     */

    public void setHistogram(T intensity, Histogram1d<T> hist)
    {
        Ihist tmp = new Ihist();
        tmp.intensity = intensity;
        tmp.hist = hist;

        for (int i = 0; i < inteHist.size() ; i++)
        {
            if (inteHist.get(i).intensity.getRealDouble() < intensity.getRealDouble())
            {
                // Add here

                inteHist.insertElementAt(tmp,i);

                return;
            }
        }
        inteHist.add(tmp);
    }

    /**
     *
     * Sample from it
     *
     */

    @Override
    public void sample(T x, T out)
    {
        int i = 0;

        // figure out in which interval we are

        for (i = 0 ; i < inteHist.size() ; i++)
        {
            if (x.getRealDouble() > inteHist.get(i).intensity.getRealDouble())
            {
                break;
            }
        }

        // Create an interpolate histogram

        InterpolateHistogram Ih = new InterpolateHistogram(inteHist.get(i),inteHist.get(i+1));

        // Get the integral of the histogram

        long integral = Ih.integral(x);

        // Generate a random number between 0 and total

        long gen = (long) (rnd.nextDouble() * integral);

        // search the bin we fall into (the domain of all histogram is warranted to be the
        // same)

        long tot = 0;

        for (i = 0 ; i < inteHist.get(0).hist.getBinCount() ; i++)
        {
            inteHist.get(0).hist.getCenterValue(i,center);
            tot += Ih.get(x,center, cls);

            if (tot >= gen)
            {
                break;
            }
        }

        // return associated T of the sampled bin

        inteHist.get(0).hist.getCenterValue(i,out);
    }

}