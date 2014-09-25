package mosaic.ia;

public class PotentialCalculator {
	public double[] getGibbsPotential() {
		return gibbspotential;
	}
	public double[] getPotential() {
		return potential;
	}

	public double getSumPotential() {
		return sumPotential;
	}


	private double [] potential;
	private double [] gibbspotential;
	private double sumPotential=0;
	private double [] D_sample; // distance at which P should be sampled (need not be measured D)
	private int type; 
	private double [] params;
	
	

	//params: threshold, epsilon: for step function
	// threshold, epsilon,sigma for all other parametric functions
	//weights for nonparam
	
	public PotentialCalculator(double [] D_sample,double [] params, int type) // for non parametric
	{
		super();
		this.D_sample=D_sample;
		this.params=params;
		this.type=type;
	}

	public void calculate()   // only Sigma (phi) for loglikelihood , not exponentiating 
	{
		gibbspotential=new double[D_sample.length];
		potential=new double[D_sample.length];
		switch(type)
		{
		case 1:	//step
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=Math.abs(params[1]);
				double epsilon=Math.abs(params[0]);
				potential[i]=epsilon*PotentialFunctions.stepPotential(D_sample[i], threshold);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
		case 2: //hernquist
			for(int i=0;i<D_sample.length;i++)
			{
				
				double threshold=0;
				
				double sigma=Math.abs(params[1]); // if sigma is large, z=d/sigma= small => -1/(1+z) is large => will be chosen during maximum likelihood.
		//		double sigma=1;
		
				
				double epsilon=Math.abs(params[0]);
				potential[i]=epsilon*PotentialFunctions.hernquistPotential(D_sample[i], threshold, sigma);
			//	System.out.println("d:"+D_sample[i]+"pot:"+potential[i]);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
		case 3: //l1
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=0;
		
				double sigma=Math.abs(params[1]);
				double epsilon=params[0];
				
				potential[i]=epsilon*PotentialFunctions.linearType1(D_sample[i], threshold, sigma);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
		case 4: //l2
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=0;
				double sigma=Math.abs(params[1]);
				double epsilon=Math.abs(params[0]);
				gibbspotential[i]=epsilon*PotentialFunctions.linearType2(D_sample[i], threshold, sigma);
				sumPotential=sumPotential+gibbspotential[i];
				gibbspotential[i]=Math.exp(-1*gibbspotential[i]);
			}
			break;
		case 5: //l2
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=0;
				double sigma=Math.abs(params[1]);
				double epsilon=params[0];
				potential[i]=epsilon*PotentialFunctions.plummerPotential(D_sample[i], threshold, sigma);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
		case 6: //nonparam
			for(int i=0;i<D_sample.length;i++)
			{
				double [] weights=params;
				potential[i]=PotentialFunctions.nonParametric(D_sample[i], weights);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
			
		case 7: //coulomb
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=0;
				double sigma=Math.abs(params[1]);
				double epsilon=params[0];
				potential[i]=epsilon*PotentialFunctions.coulomb(D_sample[i], threshold, sigma);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
			
		default:
			break; //is this required?
			
		}
	}
		
	public void calculateWOEpsilon()   // only Sigma (phi) for loglikelihood , not exponentiating 
	{
		gibbspotential=new double[D_sample.length];
		potential=new double[D_sample.length];
		switch(type)
		{
		case 1:	//step
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=Math.abs(params[1]);
				
				potential[i]=PotentialFunctions.stepPotential(D_sample[i], threshold);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			
			break;
		case 2: //hernquist
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=0;
				double sigma=Math.abs(params[1]);
				potential[i]=PotentialFunctions.hernquistPotential(D_sample[i], threshold, sigma);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
		case 3: //l1
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=0;
				double sigma=Math.abs(params[1]);
			
				potential[i]=PotentialFunctions.linearType1(D_sample[i], threshold, sigma);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
		case 4: //l2
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=0;
				double sigma=Math.abs(params[1]);
				
				gibbspotential[i]=PotentialFunctions.linearType2(D_sample[i], threshold, sigma);
				sumPotential=sumPotential+gibbspotential[i];
				gibbspotential[i]=Math.exp(-1*gibbspotential[i]);
			}
			break;
		case 5: //l2
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=0;
				double sigma=Math.abs(params[1]);
			
				potential[i]=PotentialFunctions.plummerPotential(D_sample[i], threshold, sigma);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
		case 6: //nonparam
			for(int i=0;i<D_sample.length;i++)
			{
				double [] weights=params;
				potential[i]=PotentialFunctions.nonParametric(D_sample[i], weights);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
			
		case 7: //coulomb
			for(int i=0;i<D_sample.length;i++)
			{
				double threshold=0;
				double sigma=Math.abs(params[1]);
		
				potential[i]=PotentialFunctions.coulomb(D_sample[i], threshold, sigma);
				sumPotential=sumPotential+potential[i];
				gibbspotential[i]=Math.exp(-1*potential[i]);
			}
			break;
			
		default:
			break; //is this required?
			
		}
	}
		  // still not exponentiated
		
	}


