package mosaic.bregman.output;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
	// Region3DTrack
	
	public static final String[] Region3DTrack_map = new String[] 
	{ 
		"Frame",
        "x",
        "y",
        "z",
        "Size", 
        "Intensity",
        "Surface"
    };
	
    public static CellProcessor[] Region3DTrackCellProcessor;
      
    // Region3DRscript
    
	public static final String[] Region3DRScript_map = new String[] 
	{ 
		"Image_ID",
        "Object_ID",
        "Size",
        "Perimeter",
        "Length", 
        "Intensity",
        "Coord_X",
        "Coord_Y",
        "Coord_Z",
    };
    
    public static CellProcessor[] Region3DRScriptCellProcessor;
	
    public static void initCSV()
    {
    	Region3DTrackCellProcessor = new CellProcessor[] 
    	{ 
    		 new ParseInt(),
    		 new ParseDouble(),
    	     new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
    	 };
    	
    	Region3DRScriptCellProcessor = new CellProcessor[] 
    	{
    		 new ParseInt(),
    		 new ParseInt(),
    	     new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
             new ParseDouble(),
    	 };
    	
    	oc = new SquasshOutputChoose[2];
    	
    	oc[0] = new SquasshOutputChoose();
    	oc[0].name = new String("Format for region tracking)");
    	oc[0].cel = Region3DTrackCellProcessor;
    	oc[0].map = Region3DTrack_map;
    	oc[0].classFactory = Region3DTrack.class;
    	oc[0].vectorFactory = (Class<Vector<?>>) new Vector<Region3DTrack>().getClass();
    	oc[0].InterPluginCSVFactory = (Class<InterPluginCSV<?>>) new InterPluginCSV<Region3DTrack>(Region3DTrack.class).getClass();
    	oc[0].delimiter = ',';
    	oc[1] = new SquasshOutputChoose();
    	oc[1].name = new String("Format for R script");
    	oc[1].cel = Region3DRScriptCellProcessor;
    	oc[1].map = Region3DRScript_map;
    	oc[1].classFactory = Region3DRScript.class;
    	oc[1].vectorFactory = (Class<Vector<?>>) new Vector<Region3DRScript>().getClass();
    	oc[1].InterPluginCSVFactory = (Class<InterPluginCSV<?>>) new InterPluginCSV<Region3DRScript>(Region3DRScript.class).getClass();
    	oc[1].delimiter = ';';
    	occ = oc[1];
    }
    
    /**
     * 
     * Get a vector of object with the selected format
     */
    
    public static Vector<?> getVector()
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
    
    public static InterPluginCSV<?> getInterPluginCSV()
    {
    	try {
			Constructor<?> c = occ.InterPluginCSVFactory.getDeclaredConstructor(occ.classFactory.getClass());
			InterPluginCSV<?> csv = (InterPluginCSV<?>)c.newInstance(occ.classFactory.newInstance().getClass());
			csv.setDelimiter(occ.delimiter);
			return csv;
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }
    
    public static SquasshOutputChoose occ;
    public static SquasshOutputChoose oc[];
    
    public static boolean Stitch(String output[], File dir)
    {
		InterPluginCSV<?> csv = getInterPluginCSV();
    	
		for (int j = 0 ; j < output.length ; j++)
		{
			File [] fl = new File(dir + File.separator + output[j].replace("*", "_")).listFiles();
			int nf = fl.length;
			
			String str[] = new String[nf];
			
			for (int i = 1 ; i <= nf ; i++)
			{
				str[i-1] = dir + File.separator + output[j].replace("*", "_") + File.separator + output[j].replace("*", "tmp_" + i);
			}
			
			csv.StitchConvert(str, dir + File.separator + output[j].replace("*", "stitch"), occ,"Frame",0);
		}
    	
    	return true;
    }
}
