package mosaic.noise_sample;


public interface NoiseSample<T>
{
	void sample(T x, T out);
}