package mosaic.bregman.output;



import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import mosaic.bregman.FindConnectedRegions.Region;
import mosaic.bregman.Tools;
import mosaic.core.GUI.OutputGUI;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.ipc.Outdata;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class CSVOutput
{	
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
    	
    	oc = new SquasshOutputChoose[1];
    	
    	oc[0] = new SquasshOutputChoose();
    	oc[0].name = new String("x,y,z,Size,Intensity (For region tracking)");
    	oc[0].cel = Region3DCellProcessor;
    	oc[0].map = Region3DTrack_map;
    	oc[0].classFactory = Region3DTrack.class;
    	oc[0].vectorFactory = (Class<Vector<? extends Outdata<?>>>) new Vector<Region3DTrack>().getClass();
    	oc[0].InterPluginCSVFactory = (Class<InterPluginCSV<? extends Outdata<?>>>) new InterPluginCSV<Region3DTrack>(Region3DTrack.class).getClass();
    	occ = oc[0];
    }
    
    /**
     * 
     * Get a vector of object with the selected format
     */
    
    public static Vector<? extends Outdata<?>> getVector()
    {
    	
    	try {
			return occ.vectorFactory.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }
    
    /**
     * 
     * Get a vector of objects with the selected format
     * 
     * @param v ArrayList of Region objects
     * @return Vector of object of the selected format
     */
    
    @SuppressWarnings("unchecked")
	public static Vector<? extends Outdata<Region>> getVector(ArrayList<Region> v)
    {
		InterPluginCSV<? extends Outdata<Region>> csv = (InterPluginCSV<? extends Outdata<Region>>) getInterPluginCSV();
    	
    	return (Vector<? extends Outdata<Region>> ) csv.getVector(v);
    }
    
    /**
     * 
     * Get an InterPluginCSV object with the selected format
     * 
     * @return InterPluginCSV
     */
    
    public static InterPluginCSV<? extends Outdata<?>> getInterPluginCSV()
    {
    	try {
			return occ.InterPluginCSVFactory.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }
    
    public static SquasshOutputChoose occ;
    public static SquasshOutputChoose oc[];
    
    public static boolean Stitch(String output[], File dir)
    {
		InterPluginCSV<? extends Outdata<?>> csv = getInterPluginCSV();
    	
		for (int j = 0 ; j < output.length ; j++)
		{
			File [] fl = new File(dir + File.separator + output[j].replace("*", "_")).listFiles();
			int nf = fl.length;
			
			String str[] = new String[nf];
			
			for (int i = 1 ; i <= nf ; i++)
			{
				str[i-1] = dir + File.separator + output[j].replace("*", "_") + File.separator + output[j].replace("*", "tmp_" + i);
			}
			
			csv.Stitch(str, output[j].replace("*", "stitch"), occ);
		}
    	
    	return true;
    }
}
