package mosaic.region_competition;

public class LabelInformation 
{
	int label=0;	// label
	int count;			// number of pixels of label
	double mean=0;	// mean of intensity
	double M2=0;
	double var=0;	// variance of intensity
	
	
	void setVar(double v)
	{
		//TODO setVar only for debugging
		
//		if(v<0 && v > -0.01) // very small vars are due to numerical errors
//		{
//			v = 0.0;
//			System.out.println("var<0 && var > -0.01 0.0");
//		}
//		assert(v>=0.0) : "setVar <0 ("+v+")";
		
		if(v<0)
			v=0;

		var=v;
	}
	
	public LabelInformation(int label) 
	{
		this.label=label;
	}
	
	public void reset()
	{
		label=0;
		count=0;
		mean=0;
		M2=0;
		var=0;
	}
	
// http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#On-line_algorithm
//	A numerically stable algorithm is given below. It also computes the mean. This algorithm is due to Knuth,[1] who cites Welford.[2]
//	def online_variance(data):
//	    n = 0
//	    mean = 0
//	    M2 = 0
//    for x in data:
//        n = n + 1
//        delta = x - mean
//        mean = mean + delta/n
//        M2 = M2 + delta*(x - mean)  # This expression uses the new value of mean
// 
//    variance_n = M2/n
//    variance = M2/(n - 1)
//    return variance
	
	public void add(float x)
	{
		count++;
		double delta = x-mean;
		mean = mean + delta/count;
		M2 = M2 + delta*(x-mean);  // This expression uses the new value of mean
		if (count <= 1) {
			var = 0;
		} else {
			var = M2 / (count - 1);
		}
	}
	
	public void remove(float x) {
		count--;
		if (count == 0) {
			mean = 0;
			M2 = 0;
			var = 0;
		} else {
			double delta = x - mean;
			mean = mean - delta / count;
			// TODO correct?
			M2 = M2 - delta * (x - mean); // This expression uses the new value // of mean
			if (count <= 1) {
				var = 0;
			} else {
				var = M2 / (count - 1);
			}
		}
	}
	
	@Override
	public String toString()
	{
		return 	"L: " + label
				+ " count: " + count + " " 
				+ " mean: " + mean;
	}
	
}
