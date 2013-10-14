package mosaic.bregman.output;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class CSVOutput
{
	public class Region3DTrack
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
	}
	
	public static final String[] Region3DTrack_map = new String[] 
	{ 
		"Frame",
        "x",                   // simple field mapping (like CsvBeanReader)
        "y",          // as above
        "z", // indexed (first element) + deep mapping
        "Size", 
        "Intensity", // indexed (second element) + deep mapping
        "Surface"
    };

    public static CellProcessor[] Region3DCellProcessor;
       
       
    public static void initCSV()
    {
    	Region3DCellProcessor = new CellProcessor[] 
    	{ 
    		 new ParseInt(),
    		 new ParseDouble(),
    	     new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
    	 };
     }
}