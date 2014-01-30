package mosaic.core.ipc;

public interface ICSVGeneral
{
	public void setImage_ID(int Image_ID_);
	public void setObject_ID(int Object_ID_);
	public void setSize(double Size_);
	public void setPerimeter(double Perimeter_);
	public void setLength(double Length_);
	public void setIntensity(double Intensity_);
	public void setx(double Coord_X_);
	public void sety(double Coord_Y_);
	public void setz(double Coord_Z_);
	public void setFrame(int fr);
	public void setSurface(double Surface_);
	public void setCoord_X(double Coord_X_);
	public void setCoord_Y(double Coord_Y_);
	public void setCoord_Z(double Coord_Z_);
	
	public int getImage_ID();
	public int getObject_ID();
	public double getSize();
	public double getPerimeter();
	public double getLength();
	public double getIntensity();
	public double getx();
	public double gety();
	public double getz();
	public int getFrame();
	public double getSurface();
	public double getCoord_X();
	public double getCoord_Y();
	public double getCoord_Z();
}