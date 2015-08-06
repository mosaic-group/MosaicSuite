package mosaic.region_competition.wizard.score_function;

import ij.ImagePlus;

import java.util.HashMap;

import mosaic.core.utils.IntensityImage;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.Settings;
import mosaic.region_competition.initializers.MaximaBubbles;


// Type images
	
public class ScoreFunctionInit implements ScoreFunction
{
	int off[];
	int inc_step[];
	int r_t = 8;
	int rad = 8;
		
	IntensityImage i[];
	LabelImageRC l[];
	
	public ScoreFunctionInit(IntensityImage i_[], LabelImageRC l_[],int r_t_,int rad_)
	{
		i = i_;
		l = l_;
		r_t = r_t_;
		rad = rad_;
		off = new int [i_.length];
		inc_step = new int [i_.length];
	}

	public void incrementStep()
	{
		for (int j = 0 ; j < off.length ; j++)
		{
			off[j] += inc_step[j];
		}
	}
	
	/*
	 * Set the number of objects
	 */
	
	public void setObject(int i, int off_)
	{
		off[i] = off_;
		if (2*off_ / 5 >= 1)
			inc_step[i] = 2*off_ / 5;
		else
			inc_step[i] = 1;
	}
	
	int [] getObject()
	{
		return off;
	}
		
	public Settings createSettings(Settings s, double pop[])
	{
		Settings st = new Settings(s);
			
		st.l_Sigma = pop[0];
		st.l_Tolerance = pop[1];
		
		return st;
	}
		
	@Override
	public double valueOf(double[] x) 
	{
		double sigma = x[0];
		double tol = x[1];
	
		double result = 0.0;

		for (int im = 0 ; im < i.length ; im++)
		{
			l[im].initZero();
			MaximaBubbles b = new MaximaBubbles(i[im],l[im],rad,sigma,tol,r_t);
			b.initFloodFilled();
			int c = l[im].createStatistics(i[im]);
			HashMap<Integer, LabelInformation> Map = l[im].getLabelMap();
			Map.remove(0);  // remove background
			for (LabelInformation lb : Map.values())
			{
				result += 2.0*Math.abs(lb.count - l[im].getSize()/4.0/off[im])/l[im].getSize();
			}

			result += Math.abs(c - (off[im]+1)) /**l[im].getSize()*/;
			l[im].initBoundary();
			l[im].initContour();
			result += ScoreFunctionRCsmo.SmoothNorm(l[im])/8;
			
//			l[im].show("test",10);
		}
			
		return result;
	}
		
	@Override
	public boolean isFeasible(double[] x) 
	{
		if (x[0] <= 0.0 || x[1] <= 0.0 )
			return false;
	
		if (x[0] >= 20.0)
			return false;

		// TODO Auto-generated method stub
		return true;
	}

	public void show()
	{
		for (int im = 0 ;  im < l.length ; im++)
			l[im].show("init", 255);
	}

	@Override
	public int getNImg() {
		// TODO Auto-generated method stub
		return l.length;
	}

	@Override
	public TypeImage getTypeImage() 
	{
		return TypeImage.IMAGEPLUS;
	}

	@Override
	public ImagePlus[] getImagesIP() 
	{
		ImagePlus ip[] = new ImagePlus[l.length];

		for (int i = 0 ; i < l.length ; i++)
		{
			ip[i] = l[i].convert("image", off[i]);
		}
		
		return ip;
	}

	@Override
	public String[] getImagesString() {
		// TODO Auto-generated method stub
		return null;
	}
		
	public double [] getAMean(Settings s)
	{
		double [] aMean = new double [2];
		aMean[0] = s.l_Sigma;
		aMean[1] = s.l_Tolerance;
		return aMean;
	}
}
	