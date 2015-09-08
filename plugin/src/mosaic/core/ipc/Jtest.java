package mosaic.core.ipc;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mosaic.bregman.Analysis;
import mosaic.bregman.Region;
import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.Region3DColocRScript;
import mosaic.bregman.output.Region3DRScript;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;

import org.junit.Test;
import org.scijava.util.FileUtils;

public class Jtest 
{
    private static class StringLong implements Comparable<StringLong>
    {
        String v;
        long number;
        
        /**
         * 
         * Strint int constructor
         * 
         * @param v file string
         * @param l 
         * 
         */
        
        StringLong(String v, long l)
        {
            this.v = v;
            this.number = l;
        }

        @Override
        public int compareTo(StringLong a) 
        {
            if (a.number > number)
            {
                return -1;
            }
            else if (a.number < number)
            {
                return 1;
            }
            
            return 0;
        }
    };


    /**
     * 
     * Convert a File name to number
     * 
     * @param f file
     * @return the number of the file
     * 
     */
    
    private static long FileToNumber(File f)
    {
        long id = 1;
        long stride = 1;
        Pattern p = Pattern.compile("[0-9]+");
        Matcher m = p.matcher(new StringBuilder(f.getName()).reverse());
        while (m.find()) 
        {
            String result = m.group();
            id += Integer.parseInt((new StringBuilder(result).reverse()).toString())*stride;
            stride *= 65536;
        }
        
        return id;
    }
    
    /**
     * Set an integer property of a vector of class from r1 to r2
     * 
     * @param property
     *            Property to set, setX() must be defined in the class where X
     *            is the value of the string property
     * @param v Vector
     * @param number value to set
     * @param r1 start element in the vector
     * @param r2 end element in the vector
     */
    private static <E extends Outdata<Region>>void setProperty(String property, Vector<E> v, int number, int r1, int r2) {
        Method m;
        try {
            if (r1 >= v.size())
                return;

            m = v.get(r1).getClass().getMethod("set" + property, int.class);
            for (int i = r1; i <= r2; i++) {
                m.invoke(v.get(i), number);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Stitch CSV files in one with an unknown (but equal between files) format
     * (the first CSV format file drive the output conversion). Set the property
     * specified to base + n where n run across the files (Example usefull to
     * enumerate frames if each file is a frame)
     * 
     * @param csvs files to stitch
     * @param Sttch output stitched file
     * @return true if success
     */
    private static <T extends Outdata<Region>>boolean Stitch(InterPluginCSV<T> csv, String csvs[], String Sttch, String property, int base) {
        int prev_id = 0;
        if (csvs.length == 0)
            return false;
        Vector<T> out = new Vector<T>();

        OutputChoose occ = csv.readData(csvs[0], out, null);
        if (occ == null)
            return false;

        for (int i = 1; i < csvs.length; i++) {
            prev_id = out.size();
            csv.readData(csvs[i], out, occ);
            setProperty(property, out, base + i, prev_id, out.size() - 1);
        }

        csv.Write(Sttch, out, occ, false);

        return true;
    }
    
    /**
     * 
     * Stitch the CSV files all together in the directory dir/dir_p[]
     * save the result in dir/output_file + dir_p[]
     * "*" are substituted by "_", it set the property selected to the
     * number of the file
     * 
     * @param dir_p list of directories
     * @param dir Base
     * @param output_file stitched file
     * @param property property to set
     * @param ext Meta information to supply (if null the meta information are stitched too, if provided only the provided meta-information are inserted into the stitched file)
     * @param Class<T> internal data for conversion
     * @return true if success, false otherwise
     */
    
    private static <T extends Outdata<Region>>boolean Stitch(String dir_p[], File dir, File output_file, MetaInfo ext[], String property , Class<T> cls)
    {
        boolean first = true;
        InterPluginCSV<T> csv = new InterPluginCSV<T>(cls);
        
        if (ext != null) {
            for (int i = 0 ; i < ext.length ; i++)
            csv.setMetaInformation(ext[i].parameter, ext[i].value);
        }
        
        for (int j = 0 ; j < dir_p.length ; j++)
        {
            File [] fl = new File(dir + File.separator + dir_p[j].replace("*", "_")).listFiles();
            if (fl == null)
                continue;
            int nf = fl.length;
            
            
            Vector<StringLong> si = new Vector<StringLong>();
            
            // Check all csv file
            
            for (int i = 1 ; i <= nf ; i++)
            {
                if (fl[i-1].getName().endsWith(".csv"))
                {
                    si.add(new StringLong(fl[i-1].getAbsolutePath(),FileToNumber(fl[i-1])));
                }
            }
            
            // reorder these files by number
            
            Collections.sort(si);
            
            // Construct the sorted file list
            
            String str[] = new String[si.size()];
            
            for (int i = 0 ; i < si.size() ; i++)
            {
                str[i] = si.get(i).v;
            }
            
            // sorted
            
            if (first == true)
            {
                // if it is the first time set the file preference from the first file
                
                first = false;
                
                csv.setCSVPreferenceFromFile(str[0]);
            }
            
            Stitch(csv, str, output_file + dir_p[j],property,0);
        }
        
        return true;
    }
    
    /**
     * Stitch CSV files with unknown format in one, converting to a choosen
     * format set the property specified to base + n where n run across the
     * files (Example usefull to enumerate frames if each file is a frame)
     * 
     * @param output files to read
     * @param Sttch output file name
     * @param occ format choose
     * @param base number
     * @return true if success
     */
    private static <T extends Outdata<Region>> boolean StitchConvert(InterPluginCSV<T> csv, String output[], String Sttch, OutputChoose occ, String property, int base) {
        int prev_id = 0;
        Vector<T> out = new Vector<T>();

        if (output.length == 0)
            return false;

        csv.setCSVPreferenceFromFile(output[0]);
        OutputChoose occr = csv.readData(output[0], out, null);
        if (occr == null)
            return false;

        setProperty(property, out, base, prev_id, out.size() - 1);

        for (int i = 1; i < output.length; i++) {
            prev_id = out.size();
            csv.readData(output[i], out, occr);
            setProperty(property, out, base + i, prev_id, out.size() - 1);
        }

        csv.Write(Sttch, out, occ, false);

        return true;
    }
    
    /**
     * 
     * Stitch the CSV files all together in the directory dir/dir_p[]
     * save the result in output_file + dir_p[]. 
     * "*" are substituted by "_"
     * 
     * @param dir_p list of directories
     * @param dir Base
     * @param output_file stitched file (without .csv)
     * @param ExtParam optionally an array of metadata information
     * @param OutputChoose occ Format of the output
     * @param Class<T> Internal data for conversion
     * @return true if success, false otherwise
     * 
     */
    
    private static <T extends Outdata<Region>>boolean StitchConvert(String dir_p[], File dir, File output_file , MetaInfo ext[], OutputChoose occ, Class<T> cls)
    {
        InterPluginCSV<T> csv = new InterPluginCSV<T>(cls);
        
        for (int j = 0 ; j < dir_p.length ; j++)
        {
            csv.clearMetaInformation();
            File [] fl = new File(dir + File.separator + dir_p[j].replace("*", "_")).listFiles();
            if (fl == null)
                return false;
            
            int nf = fl.length;
            
            String str[] = new String[nf];
            
            for (int i = 1 ; i <= nf ; i++)
            {
                if (fl[i-1].getName().endsWith(".csv"))
                    str[i-1] = fl[i-1].getAbsolutePath();
            }
            
            if (ext != null)
            {
                for (int i = 0 ; i < ext.length ; i++)
                csv.setMetaInformation(ext[i].parameter, ext[i].value);
            }
            StitchConvert(csv, str, output_file + dir_p[j].replace("*", "_") + ".csv", occ,"Frame",0);
        }
        
        return true;
    }

    
	@Test
	public void csvtest() 
	{		
		CSVOutput.initCSV(-1);
		
		// Stitch files all together
		
		String out[] = {"*stitchA","*stitchB"};
		String TestDir = MosaicUtils.getTestDir() + File.separator + "csv";
		
		// Remove previous test files
		
		for (int i = 0 ; i < out.length ; i++)
		{	
			FileUtils.deleteRecursively(new File(TestDir + File.separator + "test_result" + out[i].replace("*", "_") + ".csv"));
		}
		
		// Put some metadata
		
		MetaInfo mt[] = new MetaInfo[3];
		mt[0] = new MetaInfo();
		mt[0].parameter = new String("test1");
		mt[0].value = new String("test1");
		mt[1] = new MetaInfo();
		mt[1].parameter = new String("test1");
		mt[1].value = new String("test1");
		mt[2] = new MetaInfo();
		mt[2].parameter = new String("test1");
		mt[2].value = new String("test1");
		
		// Force to Bregman output
		
	    StitchConvert(out,new File(TestDir) ,new File(TestDir + File.separator + "test_result"),mt,CSVOutput.occ,CSVOutput.occ.classFactory);
		
		// Check the result
		
		for (int i = 0 ; i < out.length ; i++)
		{
			InterPluginCSV<Region3DRScript> iCSVsrc = new InterPluginCSV<Region3DRScript>(Region3DRScript.class);
			iCSVsrc.setCSVPreferenceFromFile(TestDir + File.separator + "test_result_res" + out[i].replace("*", "_") + ".csv");
			Vector<Region3DRScript> outsrc = iCSVsrc.Read(TestDir + File.separator + "test_result" + out[i].replace("*", "_") + ".csv");

			InterPluginCSV<Region3DRScript> iCSVdst = new InterPluginCSV<Region3DRScript>(Region3DRScript.class);
			iCSVdst.setCSVPreferenceFromFile(TestDir + File.separator + "test_result.csv");
			Vector<Region3DRScript> outdst = iCSVdst.Read(TestDir + File.separator + "test_result.csv");

			if (outsrc.size() != outdst.size() || outsrc.size() == 0)
				fail("Error: CSV outout does not match");
		
			for (int k = 0 ; k < outsrc.size() ; k++)
			{
				if (outsrc.get(k).equals(outdst.get(k)) == false)
				{
					fail("Error: CSV output does not match");
				}
			}
		}
		
		// test Stitch with enumerating
		
		/**
		 * 
		 * It test the post process function
		 * 
		 */
		
		String dir = MosaicUtils.getTestDir();
		String dir_test = dir + File.separator + "csv_stitch_enumerating" + File.separator + "Test";
		String dir_sample = dir + File.separator + "csv_stitch_enumerating" + File.separator + "Sample";
			
		// put all the jobs dir into tmp

		try {
			ShellCommand.exeCmd("rm -rf " + dir_test);
			ShellCommand.exeCmd("mkdir " + dir_test);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ShellCommand.copy(new File(dir_sample),new File(dir_test),null);
			
		// replace "*" with "_"
			
		Vector<String> outcsv = new Vector<String>();
		
		// get the csv output
		
		for (int i = 0 ; i < Analysis.out.length ; i++)
		{
			if (Analysis.out[i].endsWith(".csv"))
				outcsv.add(Analysis.out[i].replace("*", "_"));
		}
			
		String [] outcsvS = new String[outcsv.size()];
			
		for (int i = 0 ; i < outcsvS.length ; i++)
		{
			outcsvS[i] = outcsv.get(i);
		}
			
		// fill meta

		mt = new MetaInfo[1];
		mt[0] = new MetaInfo();
		mt[0].parameter = new String("Background");
		mt[0].value = new String("test_bck");
		
		// Filter out csv output data
		
		String[] outcsv_dir = MosaicUtils.getCSV(Analysis.out);
		
		// Stitch
		
	    Stitch(outcsv_dir, new File(dir_test + File.separator + "Job291370"), new File(dir_test + File.separator + "Job291370" + File.separator + "test_out") , mt, "Image_ID", Region3DColocRScript.class);
		
		// test Stitch with enumerating2
		
		dir = MosaicUtils.getTestDir();
		dir_test = dir + File.separator + "csv_stitch_enumerating2" + File.separator + "Test";
		dir_sample = dir + File.separator + "csv_stitch_enumerating2" + File.separator + "Sample";
			
		// put all the jobs dir into tmp

		try {
			ShellCommand.exeCmd("rm -rf " + dir_test);
			ShellCommand.exeCmd("mkdir " + dir_test);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ShellCommand.copy(new File(dir_sample),new File(dir_test),null);
			
		// replace "*" with "_"
			
		outcsv = new Vector<String>();
		
		// get the csv output
		
		for (int i = 0 ; i < Analysis.out.length ; i++)
		{
			if (Analysis.out[i].endsWith(".csv"))
				outcsv.add(Analysis.out[i].replace("*", "_"));
		}
			
		outcsvS = new String[outcsv.size()];
			
		for (int i = 0 ; i < outcsvS.length ; i++)
		{
			outcsvS[i] = outcsv.get(i);
		}
			
		// fill meta

		mt = new MetaInfo[1];
		mt[0] = new MetaInfo();
		mt[0].parameter = new String("Background");
		mt[0].value = new String("test_bck");
		
		// Filter out csv output data
		
		outcsv_dir = MosaicUtils.getCSV(Analysis.out);
		
		// Stitch
		
		Stitch(outcsv_dir, new File(dir_test + File.separator + "Job469867"), new File(dir_test + File.separator + "Job469867" + File.separator + "test_out") , mt, "Image_ID", Region3DColocRScript.class);
		
/*		if (new File(dir_test + File.separator + "Job237666" + File.separator + "Test_Cell_stitch.csv").exists() == false)
		{
			fail("Error postprocess_jobs wrong output");
		}*/
	}
}