package mosaic.region_competition;

public class LabelInformation 
{
	int label=0;	// label
	int n;			// number of pixels of label
	float mean=0;	// mean of intensity
	float M2=0;
	float var=0;	// variance of intensity
	
	public LabelInformation(int label) 
	{
		this.label=label;
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
	
	public void add(int x)
	{
		n++;
		float delta = x-mean;
		mean = mean + delta/n;
		M2 = M2 + delta*(x-mean);  // This expression uses the new value of mean
		if (n <= 1) {
			var = 0;
		} else {
			var = M2 / (n - 1);
		}
	}
	
	public void remove(int x) {
		n--;
		if (n == 0) {
			mean = 0;
			M2 = 0;
			var = 0;
		} else {
			float delta = x - mean;
			mean = mean - delta / n;
			// TODO correct?
			M2 = M2 - delta * (x - mean); // This expression uses the new value // of mean
			if (n <= 1) {
				var = 0;
			} else {
				var = M2 / (n - 1);
			}
		}
	}
	
}
