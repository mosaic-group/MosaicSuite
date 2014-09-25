package mosaic.region_competition.output;

import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.StubProp;

/**
 * 
 * Region Competition internal data conversion
 * 
 * @author Pietro Incardona
 *
 */

public class RCOutput extends StubProp implements ICSVGeneral
{
	int Frame;
	double x;
	double y;
	double z;
	double mean;
	double size;
	double var;
	
	@Override
	public void setImage_ID(int Image_ID_) 
	{
		Frame = Image_ID_;
	}

	@Override
	public void setSize(double Size_)
	{
		size = Size_;
	}

	@Override
	public void setx(double Coord_X_) 
	{
		x = Coord_X_;
	}

	@Override
	public void sety(double Coord_Y_) 
	{
		y = Coord_Y_;
	}

	@Override
	public void setz(double Coord_Z_) 
	{
		z = Coord_Z_;
	}

	@Override
	public void setFrame(int fr) 
	{
		Frame = fr;
	}

	@Override
	public void setIntensity(double intensity)
	{
		mean = intensity;
	}
	
	@Override
	public void setCoord_X(double Coord_X_) 
	{
		x = Coord_X_;
	}

	@Override
	public double getCoord_Y() 
	{
		return y;
	}

	@Override
	public double getCoord_Z() 
	{
		return z;
	}

	@Override
	public double getCoord_X() 
	{
		return x;
	}

	@Override
	public double getIntensity() 
	{
		return mean;
	}
	
	@Override
	public int getImage_ID() 
	{
		return Frame;
	}
	
	@Override
	public void setCoord_Y(double Coord_Y_) 
	{
		y = Coord_Y_;
	}

	@Override
	public void setCoord_Z(double Coord_Z_) 
	{
		z = Coord_Z_;
	}
	

	@Override
	public double getSize() 
	{
		return size;
	}
	
	public void setData(RCOutput rco)
	{
		Frame = rco.Frame;
		x = rco.x;
		y = rco.y;
		z = rco.z;
		mean = rco.mean;
		size = rco.size;
		var = rco.var;
	}
}