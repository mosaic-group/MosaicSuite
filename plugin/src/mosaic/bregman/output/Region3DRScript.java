package mosaic.bregman.output;

import mosaic.bregman.Region;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.Outdata;
import mosaic.core.ipc.StubProp;



public class Region3DRScript extends StubProp implements ICSVGeneral
{
	int Image_ID;
	int Object_ID;
	double Size;
	double Perimeter;
	double Length;
	double Intensity;
	double x;
	double y;
	double z;
	
	public void setImage_ID(int Image_ID_) {Image_ID = Image_ID_;}
	public void setObject_ID(int Object_ID_)	{Object_ID = Object_ID_;}
	public void setSize(double Size_)	{Size = Size_;}
	public void setPerimeter(double Perimeter_)	{Perimeter = Perimeter_;}
	public void setLength(double Length_)	{Length = Length_;}
	public void setIntensity(double Intensity_)	{Intensity = Intensity_;}
	public void setx(double Coord_X_)		{x = Coord_X_;}
	public void sety(double Coord_Y_)		{y = Coord_Y_;}
	public void setz(double Coord_Z_)		{z = Coord_Z_;}

	
	public Region3DRScript() {}

	public void setData(Region r) 
	{
		// TODO Auto-generated method stub
		
		Image_ID = 0;
		Object_ID = 0;
		Intensity = r.getintensity();
		Perimeter = r.getperimeter();
		Size = r.getrsize();
		x = r.getcx();
		y = r.getcy();
		z = r.getcz();
	}
	
	public void setData(Region3DRScript r) 
	{
		// TODO Auto-generated method stub
		
		Image_ID = r.Image_ID;
		Object_ID = r.Object_ID;
		Size = r.Size;
		Perimeter = r.Perimeter;
		Length = r.Length;
		Intensity = r.Intensity;
		x = r.x;
		y = r.y;
		z = r.z;
	}
	
	public boolean equals(Region3DRScript r)
	{		
		if (Size != r.Size)
			return true;
		
		if (Perimeter != r.Perimeter)
			return true;
			
		if (Length != r.Length)
			return true;
		
		if (Intensity != r.Intensity)
			return true;
		
		if (x != r.x)
			return true;
		
		if (y != r.y)
			return true;
		
		if (z != r.z)
			return true;
		
		return false;
	}
	
	public void setFrame(int fr) {Image_ID = fr;}
	public void setSurface(double Surface_) {Perimeter = Surface_;}
	public void setCoord_X(double Coord_X_) {x = Coord_X_;}
	public void setCoord_Y(double Coord_Y_) {y = Coord_Y_;}
	public void setCoord_Z(double Coord_Z_) {z = Coord_Z_;}
	
	public int getImage_ID()	{return Image_ID;}
	public int getObject_ID()	{return Object_ID;}
	public double getCoord_X()	{return x;}
	public double getCoord_Y()	{return y;}
	public double getCoord_Z()	{return z;}
	public double getIntensity()	{return Intensity;}
	public double getSize()				{return Size;}
	public double getPerimeter()		{return Perimeter;}
	public double getLength()		{return Length;}
	public double getx() {return x;}
	public double gety() {return y;}
	public double getz() {return z;}
	public int getFrame() {return Image_ID;}
	public double getSurface() {return 0;}
}