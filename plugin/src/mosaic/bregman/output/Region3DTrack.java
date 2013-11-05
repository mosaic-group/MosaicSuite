package mosaic.bregman.output;

import mosaic.bregman.FindConnectedRegions.Region;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.Outdata;

public class Region3DTrack implements ICSVGeneral
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
		// TODO Auto-generated method stub
		
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
}
	