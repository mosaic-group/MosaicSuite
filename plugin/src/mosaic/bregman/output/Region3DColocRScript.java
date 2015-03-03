package mosaic.bregman.output;

import mosaic.bregman.Region;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.Outdata;
import mosaic.core.ipc.StubProp;



public class Region3DColocRScript extends StubProp implements ICSVGeneral
{
	int Image_ID;
	int Object_ID;
	double Size;
	double Perimeter;
	double Length;
	double Intensity;
	double Overlap_with_ch;
	double Coloc_object_size;
	double Coloc_object_intensity;
	boolean Single_Coloc;
	double Coloc_image_intensity;
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
	public void setOverlap_with_ch(double Overlap_with_ch_)		{Overlap_with_ch = Overlap_with_ch_;}
	public void setColoc_object_size(double cos)		{Coloc_object_size = cos;}
	public void setColoc_object_intensity(double coi)	{Coloc_object_intensity = coi;}
	public void setSingle_Coloc(String sc)			{Single_Coloc = Boolean.getBoolean(sc);}
	public void setColoc_image_intensity(double cii)	{Coloc_image_intensity = cii;}
	
	
	public Region3DColocRScript() {}

	public void setData(Region r) 
	{
		// TODO Auto-generated method stub
		
		Image_ID = 0;
		Object_ID = 0;
		Intensity = r.getintensity();
		Perimeter = r.getperimeter();
		Size = r.getrsize();
		Overlap_with_ch = r.getoverlap_with_ch();
		Coloc_object_size = r.getcoloc_object_size();
		Coloc_object_intensity = r.getcoloc_object_intensity();
		Single_Coloc = r.getsingle_coloc();
		Coloc_image_intensity = r.getcoloc_image_intensity();
		x = r.getcx();
		y = r.getcy();
		z = r.getcz();
	}
	
	public void setData(Region3DColocRScript r) 
	{
		// TODO Auto-generated method stub
		
		Image_ID = r.Image_ID;
		Object_ID = r.Object_ID;
		Size = r.Size;
		Perimeter = r.Perimeter;
		Length = r.Length;
		Intensity = r.Intensity;
		Overlap_with_ch = r.Overlap_with_ch;
		Coloc_object_size = r.Coloc_object_size;
		Coloc_object_intensity = r.Coloc_object_intensity;
		Single_Coloc = r.Single_Coloc;
		Coloc_image_intensity = r.Coloc_image_intensity;
		x = r.x;
		y = r.y;
		z = r.z;
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
	public double getOverlap_with_ch() {return Overlap_with_ch;}
	public double getColoc_object_size() {return Coloc_object_size;}
	public double getColoc_object_intensity() {return Coloc_object_intensity;}
	public String getSingle_Coloc() {return Boolean.toString(Single_Coloc);}
	public double getColoc_image_intensity() {return Coloc_image_intensity;}
}
