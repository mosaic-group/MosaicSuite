package mosaic.bregman.output;



import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import mosaic.bregman.FindConnectedRegions.Region;
import mosaic.bregman.Tools;
import mosaic.core.GUI.OutputGUI;
import mosaic.core.GUI.OutputGUI.OutputChoose;
import mosaic.core.ipc.InterPluginCSV;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class CSVOutput
{
	public interface Outdata
	{
		void setRegion(Region r);
	}
	
	public class Region3DTrack implements Outdata
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
		@Override
		public void setRegion(Region r) 
		{
			// TODO Auto-generated method stub
			
		}
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
    	
    	oc = new OutputChoose[1];
    	
    	OutputGUI g = new OutputGUI();
    	CSVOutput outt = new CSVOutput();
    	oc[0] = g.new OutputChoose();
    	oc[0].name = new String("x,y,z,Size,Intensity (For region tracking)");
    	oc[0].cel = Region3DCellProcessor;
    	oc[0].map = Region3DTrack_map;
    	oc[0].pojoC = outt.new Region3DTrack();
    	occ = oc[0];
    }
    
    /**
     * 
     * Get a vector of object with the selected format
     */
    
    public static Vector<?> getVector()
    {
    	return new Vector<CSVOutput.Region3DTrack>();
    }
    
    /**
     * 
     * Get an InterPluginCSV object with the selected object format
     * 
     * @return InterPluginCSV
     */
    
    public static InterPluginCSV<?> getInterPluginCSV()
    {
    	return new InterPluginCSV<CSVOutput.Region3DTrack>(CSVOutput.Region3DTrack.class);
    }
    
    /**
     * 
     * Write CSV file
     * 
     * @param path path of the file
     * @param obl_ vector of data
     * @param csv_ CSV writer
     * @param append true to append the data
     */
    
    public static void Write(String path, Vector<?> obl_, InterPluginCSV<?> csv_ , boolean append)
    {
    	@SuppressWarnings("unchecked")
		Vector<Region3DTrack> obl = (Vector<Region3DTrack>) obl_;
    	@SuppressWarnings("unchecked")
		InterPluginCSV<Region3DTrack> csv = (InterPluginCSV<Region3DTrack>) csv_;
    	
		csv.Write(path, obl, CSVOutput.occ.cel, CSVOutput.occ.map ,append);
    }
    
    /**
     * 
     * Read a CSV file
     * 
     * @param path path of the file to read
     * @param obl_ output vector
     * @param csv_ csv reader
     * @param append true if you want append the data
     */
    
    public static void Read(String path, Vector<?> obl_, InterPluginCSV<?> csv_ , boolean append)
    {
    	@SuppressWarnings("unchecked")
		Vector<Region3DTrack> obl = (Vector<Region3DTrack>) obl_;
    	@SuppressWarnings("unchecked")
		InterPluginCSV<Region3DTrack> csv = (InterPluginCSV<Region3DTrack>) csv_;
    	
		csv.Read(path, obl, CSVOutput.occ.cel, CSVOutput.occ.map ,append);
    }
    
    public static OutputChoose occ;
    public static OutputChoose oc[];
    
    public static boolean Stitch(String output[], File dir)
    {    	
		Vector<?> v = getVector();
		InterPluginCSV<?> csv = getInterPluginCSV();
    	
		for (int j = 0 ; j < output.length ; j++)
		{
			File [] fl = new File(dir + File.separator + output[j].replace("*", "_")).listFiles();
			int nf = fl.length;
			
			for (int i = 1 ; i <= nf ; i++)
			{
				String fll = dir + File.separator + output[j].replace("*", "_") + File.separator + output[j].replace("*", "tmp_" + i);
    		
				Read(fll,v,csv,true);
			}
			
			Write(output[j].replace("*", "global"),v,csv, false);
		}
    	
    	return true;
    }
    
    
    static public Vector<?> convertRegionToCSVArrayOutput(ArrayList<Region> regionslistA, int f)
    {
    	CSVOutput csv = new CSVOutput();
    	
    	@SuppressWarnings("unchecked")
		Vector<Outdata> v = (Vector<Outdata>)getVector();
    	
    	for (Iterator<Region> it = regionslistA.iterator(); it.hasNext();) 
    	{
    		Region3DTrack pt_p = csv.new Region3DTrack();
    		Region r = it.next();
		
    		pt_p.setFrame(f);
    		pt_p.setx(r.getcx());
    		pt_p.sety(r.getcy());
    		pt_p.setz(r.getcz());
    		pt_p.setIntensity(r.getintensity());
    		pt_p.setSize(r.getrsize());
    		pt_p.setSurface(Tools.round(r.getperimeter(),3));
		
    		v.add(pt_p);
    	}
    	
    	return v;
    }
}
