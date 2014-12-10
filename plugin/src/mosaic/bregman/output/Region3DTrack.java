package mosaic.bregman.output;

import mosaic.bregman.Region;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.Outdata;
import mosaic.core.ipc.StubProp;
import mosaic.core.utils.Point;

public class Region3DTrack extends StubProp implements ICSVGeneral
{
	int Frame;
	double x;
	double y;
	double z;
	double Size;
	double Intensity;
	double Surface;
	
	public void setFrame(int fr) {Frame = fr;}
	public void setx(double x_)	{x = x_;}
	public void sety(double y_)	{y = y_;}
	public void setz(double z_)	{z = z_;}
	public void setIntensity(double Intensity_)	{Intensity = Intensity_;}
	public void setSize(double Size_)				{Size = Size_;}
	public void setSurface(double Surface_)		{Surface = Surface_;}
		
	public int getFrame()	{return Frame;}
	public double getx()	{return x;}
	public double gety()	{return y;}
	public double getz()	{return z;}
	public double getIntensity()	{return Intensity;}
	public double getSize()				{return Size;}
	public double getSurface()		{return Surface;}

		
	public Region3DTrack() {}

	public void setData(Region r) 
	{
		Frame = 0;
		x = r.getcx();
		y = r.getcy();
		z = r.getcz();
		Size = r.getrsize();
		Intensity = r.getintensity();
		Surface = r.getperimeter();
	}

	public void setData(Region3DTrack r) 
	{
		Frame = r.Frame;
		x = r.x;
		y = r.y;
		z = r.z;
		Size = r.Size;
		Intensity = r.Intensity;
		Surface = r.Surface;
	}
	
	@Override
	public void setObject_ID(int Object_ID_) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setPerimeter(double Perimeter_) {Surface = Perimeter_;}
	@Override
	public void setLength(double Length_) {
		// TODO Auto-generated method stub
		
	}

	public void setImage_ID(int Image_ID_) {Frame = Image_ID_;}
	public void Coord_X(double Coord_X_) {x = Coord_X_;}
	public void Coord_Y(double Coord_Y_) {y = Coord_Y_;}
	public void Coord_Z(double Coord_Z_) {z = Coord_Z_;}
	public void setCoord_X(double Coord_X_) {x = Coord_X_;}
	public void setCoord_Y(double Coord_Y_) {y = Coord_Y_;}
	public void setCoord_Z(double Coord_Z_) {z = Coord_Z_;}
	public int getImage_ID() {return Frame;}
	public int getObject_ID() {return 0;}
	public double getPerimeter() {return 0;}
	public double getLength() {return 0;}
	public double getCoord_X() {return x;}
	public double getCoord_Y() {return y;}
	public double getCoord_Z() {return z;}
	public void setData(Point point) 
	{
		if (point.x.length >= 3)
		{
			x = point.x[0];
			y = point.x[1];
			z = point.x[2];
		}
		else
		{
			x = point.x[0];
			y = point.x[1];
		}
	}
}
	