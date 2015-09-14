package mosaic.bregman.output;

import mosaic.bregman.Region;



public class Region3DRScript implements Outdata<Region>
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
	public void setlabel(int aID) {setImage_ID(aID);}
	public void setObject_ID(int Object_ID_)	{Object_ID = Object_ID_;}
	public void setSize(double Size_)	{Size = Size_;}
	public void setsize(double aSize) {setSize(aSize);}
	public void setPerimeter(double Perimeter_)	{Perimeter = Perimeter_;}
	public void setLength(double Length_)	{Length = Length_;}
	public void setIntensity(double Intensity_)	{Intensity = Intensity_;}
	public void setmean(double aVal) {setIntensity(aVal);}
	public void setx(double Coord_X_)		{x = Coord_X_;}
	public void sety(double Coord_Y_)		{y = Coord_Y_;}
	public void setz(double Coord_Z_)		{z = Coord_Z_;}

	
	public Region3DRScript() {}

	public void setData(Region r) 
	{
		
		Image_ID = 0;
		Object_ID = 0;
		Intensity = r.getintensity();
		Perimeter = r.getperimeter();
		Length = r.getlength();
		Size = r.getrsize();
		x = r.getcx();
		y = r.getcy();
		z = r.getcz();
	}
	
	public void setData(Region3DRScript r) 
	{
		
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
	
	@Override
	public boolean equals(Object r)
	{
	    if (r instanceof Region3DRScript)
	    {
	    	Region3DRScript r_ = (Region3DRScript ) r;
	    	
	    	if (Size != r_.Size) return false;
			if (Perimeter != r_.Perimeter) return false;
			if (Length != r_.Length) return false;
			if (Intensity != r_.Intensity) return false;
			if (x != r_.x) return false;
			if (y != r_.y) return false;
			if (z != r_.z) return false;
	    }
		
		return true;
	}
	
	public void setFrame(int fr) {Image_ID = fr;}
	public void setSurface(double Surface_) {Perimeter = Surface_;}
	public void setCoord_X(double Coord_X_) {x = Coord_X_;}
	public void setCoord_Y(double Coord_Y_) {y = Coord_Y_;}
	public void setCoord_Z(double Coord_Z_) {z = Coord_Z_;}
	
	public int getImage_ID()	{return Image_ID;}
	public int getlabel() {return getImage_ID();}
	public int getObject_ID()	{return Object_ID;}
	public double getCoord_X()	{return x;}
	public double getCoord_Y()	{return y;}
	public double getCoord_Z()	{return z;}
	public double getIntensity()	{return Intensity;}
	public double getmean() {return getIntensity();}
	public double getSize()				{return Size;}
	public double getsize() {return getSize();}
	public double getPerimeter()		{return Perimeter;}
	public double getLength()		{return Length;}
	public double getx() {return x;}
	public double gety() {return y;}
	public double getz() {return z;}
	public int getFrame() {return Image_ID;}
	public double getSurface() {return 0;}
	
	public void setFile(String dummy) {}
	public String getFile() {return null;}
}
