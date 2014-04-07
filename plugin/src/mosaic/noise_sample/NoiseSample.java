package mosaic.noise_sample;


public interface NoiseSample<T>
{
	T sample(T x);
}