package mosaic.region_competition.energies;

import java.util.ArrayList;
import java.util.HashMap;

import mosaic.core.utils.Point;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.Settings;

/**
	 * exponential moving average factor over sum of energies (smoothing)
	 * dann verhalnis aufeinanderfolgender werte - 1.
	 * naehert sich 0 an bei oszillationen (da x/x' -> 1)
	 * smooth again and detect a number of almost-zeros
	 * 
	 * geht nicht so gut, da oszillationen beliebig gross sein konnen
 *
 */
class OscillationDetection3 extends OscillationDetection
{
	private double alpha = 0.1; //  exponential moving average factor
	private double alpha2 = 0.01;
//	private double sum = 0;
	
	private ArrayList<Double> sums;
	private double sumAvg;
	private ArrayList<Double> sumsAvg;
	
	private ArrayList<Double> factors;
	private double factorAvg;
	private ArrayList<Double> factorsAvg;
	
	private boolean isFirstRound;
	
	public OscillationDetection3(Algorithm algo, Settings settings)
	{
		super(algo, settings);
		initMembeers(settings.m_MaxNbIterations);
	}
	
	void initMembeers(int maxIt)
	{
		sums = new ArrayList<Double>(maxIt);
		sumsAvg = new ArrayList<Double>(maxIt);
		factors = new ArrayList<Double>(maxIt);
		factorsAvg = new ArrayList<Double>(maxIt);
		
		sumAvg = 0;
		factorAvg = -1;
		
		isFirstRound=true;
	}
	
	

	@Override
	public boolean DetectOscillations(HashMap<Point, ContourParticle> m_Candidates)
	{
        double sumNew = SumAllEnergies(m_Candidates);
        sums.add(sumNew);
        
        double oldSumAvg = sumAvg;
        
        double newSumAvg = alpha*sumNew + (1-alpha)*oldSumAvg;
        sumsAvg.add(newSumAvg);
        sumAvg = newSumAvg; 
        
        if(isFirstRound) oldSumAvg = newSumAvg; // division by zero
        double newFac = newSumAvg/oldSumAvg-1;
        factors.add(newFac);
        
        double oldFactorAvg = factorAvg;
        double newFactorAvg = alpha*newFac + (1-alpha2)*oldFactorAvg;
        factorsAvg.add(newFactorAvg);
        factorAvg = newFactorAvg;
        
        // detect mutliple occurencies of factorAvg ~~ 0
        
        debug("sum="+sumNew+ " sumAvg="+sumAvg);
        debug("fac="+newFac+ " facAvg="+factorAvg);
        
        isFirstRound=false;
        return false;
	}


}