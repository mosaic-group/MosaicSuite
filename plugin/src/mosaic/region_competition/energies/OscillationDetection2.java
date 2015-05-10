package mosaic.region_competition.energies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mosaic.core.utils.Point;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.Settings;

public class OscillationDetection2 extends OscillationDetection
{
	private double alpha = 0.1; //  exponential moving average factor
//	private double alpha2 = 0.01;
//	private double sum = 0;
	
	private ArrayList<Double> sums;
	private double sumAvg;
	private ArrayList<Double> sumsAvg;
	
	private boolean isFirstRound;
	
	private int length = 10; 
	private double threshold=0.02;
	
	public OscillationDetection2(Algorithm algo, Settings settings)
	{
		super(algo, settings);
		threshold = settings.m_OscillationThreshold;
		initMembeers(settings.m_MaxNbIterations);
	}
	
	void initMembeers(int maxIt)
	{
		sums = new ArrayList<Double>(maxIt);
		sumsAvg = new ArrayList<Double>(maxIt);
		
		sumAvg = 0;
		
		isFirstRound=true;
	}
	
	@Override
	public boolean DetectOscillations(HashMap<Point, ContourParticle> m_Candidates)
	{
		boolean result = false;
        double sumNew = SumAllEnergies(m_Candidates);
        
        sums.add(sumNew);
        
        double oldSumAvg = sumAvg;
        if(isFirstRound)
        	oldSumAvg=sumNew;
        
        double newSumAvg = alpha*sumNew + (1-alpha)*oldSumAvg;
        sumsAvg.add(newSumAvg);
        sumAvg = newSumAvg;
        
        double totstd = std(sumsAvg);
        int n = sumsAvg.size();
        int start=Math.max(0, n-length);
        double winstd = std(sumsAvg.subList(start, n));
        
        double fac = 1;
        if(!isFirstRound){
        	fac = winstd/totstd;
        }
        
        debug("sum="+sumNew+ " sumAvg="+sumAvg);
        debug("fac="+fac);
        
        isFirstRound=false;
        
        if(fac<threshold)
        {
        	result = true;
        	debug("***NEW Oscillation detected***");
        	algorithm.m_AcceptedPointsFactor/=2.0;
        }
        return result;
	}
	
	double mean(List<Double> data)
	{
		int n=data.size();
		double sum=0;
		for(int i=0; i<n; i++)
		{
			sum+=data.get(i);
		}
		
		return sum/n;
	}
	
	
	double std(List<Double> data)
	{
		double m = mean(data);

		int n=data.size();
		double sum=0;
		for(int i=0; i<n; i++)
		{
			double d = m-data.get(i);
			sum+=(d*d);
		}
		
		sum=sum/n;
		return Math.sqrt(sum);
	}
	
}