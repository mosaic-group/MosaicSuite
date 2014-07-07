package mosaic.core.ipc;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Vector;

import org.apache.commons.beanutils.PropertyUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.comment.CommentMatcher;
import org.supercsv.comment.CommentStartsWith;
import org.supercsv.io.dozer.CsvDozerBeanReader;
import org.supercsv.io.dozer.CsvDozerBeanWriter;
import org.supercsv.io.dozer.ICsvDozerBeanReader;
import org.supercsv.io.dozer.ICsvDozerBeanWriter;
import org.supercsv.prefs.CsvPreference;


/**
*
* <h2>Inter Plugins CSV</h2>
*
* This class is able to read, generate and convert CSV file
* It use SuperCSV
*
* The main class is InterPluginCSV<E extends ICSVGeneral & Outdata<S>, S>,
* where E is a User choosen POJO JavaBeans class that extends ICSVGeneral
* aka (Internal type), used for sematical conversion from column to class
* attribute/member
* 
* <strong>Note</strong>: The CSV format is not related to the POJO Class but to OutputChoose
* passed to the function. You can have one POJO and different format
* 
* Each function can require OutputChoose, only map and cel of this class 
* are used
* 
* here is an example
* 
* private static final String[] FIELD_MAPPING = new String[] { 
*        "age",                   // simple field mapping (like CsvBeanReader)
*        "consentGiven",          // as above
*        "answers[0].questionNo", // indexed (first element) + deep mapping
*        "answers[0].answer", 
*        "answers[1].questionNo", // indexed (second element) + deep mapping
*        "answers[1].answer", 
*        "answers[2].questionNo", 
*        "answers[2].answer" };
*
* and a cell Processor that process Columns
*
*       final CellProcessor[] processors = new CellProcessor[] { 
*               new Optional(new ParseInt()), // age
*               new ParseBool(),              // consent
*               new ParseInt(),               // questionNo 1
*               new Optional(),               // answer 1
*               new ParseInt(),               // questionNo 2
*               new Optional(),               // answer 2
*               new ParseInt(),               // questionNo 3
*               new Optional()                // answer 3
*       };
*
* 
*
* @author Pietro Incardona
* 
* @param <E> POJO JavaBeans base class
* 
*/

public class InterPluginCSV<E extends ICSVGeneral>
{
	class ExtParam
	{
		ExtParam(String p1, String p2)
		{
			this.p1 = p1;
			this.p2 = p2;
		}
		
		String p1;
		String p2;
	}
	
	class CommentExtendedCSV implements CommentMatcher
	{

		@Override
		public boolean isComment(String s) 
		{
			if (s.startsWith("%"))
			{
				String[] pr = s.split(":");
				
				if (pr.length == 2)
				{
					fld.add(new ExtParam(pr[0].substring(1),pr[1].trim()));
				}
				
				return true;
			}
			return false;
		}
		
	}
	
	private Class<E> p;
	CsvPreference c;
	Vector<ExtParam> fld;
	
	/**
	 * 
	 * Constructor, require the class type of the Generic parameter,
	 * example for InterPluginCSV<T> you have to pass T.class 
	 * 
	 * @param p_ T.class
	 */
	
	public InterPluginCSV(Class<E> p_)
	{
		p = p_;
    	CsvPreference.Builder bld = new CsvPreference.Builder('"', ',', "\n");
    	bld.skipComments(new CommentExtendedCSV());
    	c = bld.build();
    	fld = new Vector<ExtParam>();
	}
	
	/**
	 * 
	 * Set Meta information
	 * 
	 * @param String meta information 
	 * @param Value of the meta information
	 */
	
	public void setMetaInformation(String parameter, String Value)
	{
		ExtParam prm = new ExtParam(parameter, Value);
		fld.add(prm);
	}
	
	/**
	 * 
	 * Get Meta information
	 * 
	 * @param String meta information 
	 * @return Value of the meta information
	 */
	
	public String getMetaInformation(String parameter)
	{
		for (int i = 0 ;i < fld.size() ; i++)
		{
			if (fld.get(i).p1.equals(parameter) )
			{
				return fld.get(i).p2;
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * Check if exist the column
	 * 
	 * @param CsvFilename
	 */
	
	public boolean existColum(String columnString, String CsvFilename)
	{
        ICsvDozerBeanReader beanReader = null;
        try
        {
        	beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), c);
        	
            E element = p.newInstance();
            
            String[] map = beanReader.getHeader(true); // ignore the header
            
            for (String col : map)
            {
            	if (col.equals(columnString))
            	{
            		return true;
            	}
            }
        }
        catch (IOException e) 
        {e.printStackTrace();}
        catch (InstantiationException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        finally 
        {
                if( beanReader != null ) 
                {
                        try 
                        {beanReader.close();}
                        catch (IOException e) 
                        {e.printStackTrace();}
                }
        }
        return false;
	}
	
	/**
	 * 
	 * Trying to figure out the best setting to read the CSV file
	 * 
	 * @param CsvFilename
	 */
	
	public void setCSVPreferenceFromFile(String CsvFilename)
	{
        ICsvDozerBeanReader beanReader = null;
        try
        {
        	beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), c);
        	
            E element = p.newInstance();
            
            String[] map = beanReader.getHeader(true); // ignore the header
            
            if (map.length == 1)
            {
            	// Try to split with point comma
            	
            	if (map[0].split(";").length > 1)
            	{
            		setDelimiter(';');
            		return;
            	}
            	
            	if (map[0].split(" ").length > 1)
            	{
            		setDelimiter(' ');
            		return;
            	}
            }
        }
        catch (IOException e) 
        {e.printStackTrace();}
        catch (InstantiationException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        finally 
        {
                if( beanReader != null ) 
                {
                        try 
                        {beanReader.close();}
                        catch (IOException e) 
                        {e.printStackTrace();}
                }
        }
	}
	
	/**
	 * 
	 * Read more CSV files result returned as a vector of Generics
	 * 
	 * @param CsvFilename Name of the filename to open
	 * @param OutputChoose output choose
	 * @return out output vector
	 */
	
	public Vector<E> Read(String CsvFilename[], OutputChoose occ)
	{
		Vector<E> out = new Vector<E>();
		
		for (int i = 0 ; i < CsvFilename.length ; i++)
		{
			Readv(CsvFilename[i],out,occ);
		}
		
        return out;
	}
	
	private OutputChoose ReadGeneral(String CsvFilename, Vector<E> out)
	{
		OutputChoose occ = new OutputChoose();
        ICsvDozerBeanReader beanReader = null;
        try 
        {
        	beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), c);
        	
            E element = p.newInstance();
            
            String[] map = beanReader.getHeader(true); // ignore the header
            
            // number of column
            
            int nc = map.length;
            
            if (map == null) // we cannot get the header
            	return null;
            
            CellProcessor c[] = new CellProcessor[map.length];
            ProcessorGeneral pc = new ProcessorGeneral();
            
            for (int i = 0 ; i < c.length ; i++)
            {
            	try {
            		map[i] = pc.getMap(map[i].replace(" ", "_"));
					c[i] = (CellProcessor) pc.getClass().getMethod("getProcessor" + map[i].replace(" ", "_")).invoke(pc);
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					c[i] = null;
					map[i] = "Nothing";
					nc--;
					continue;
				}
            	map[i] = map[i].replace(" ", "_");
            }
            
            beanReader.configureBeanMapping(element.getClass(), map);
            
            occ.map = map;
            occ.cel = c;
            
            while( (element = (E)beanReader.read(element.getClass(), c)) != null ) 
            {
            	out.add(element);
            }
                
        } catch (IOException e) 
        {e.printStackTrace();}
        catch (InstantiationException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        finally 
        {
                if( beanReader != null ) 
                {
                        try 
                        {beanReader.close();}
                        catch (IOException e) 
                        {e.printStackTrace();}
                }
        }
        
        return occ;
	}
	
	private void Readv(String CsvFilename, Vector<E> out, OutputChoose occ)
	{
        ICsvDozerBeanReader beanReader = null;
        try 
        {
        	beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), c);
                
            E element = p.newInstance();
                
            beanReader.getHeader(true); // ignore the header
            beanReader.configureBeanMapping(element.getClass(), occ.map);
            
            while( (element = (E)beanReader.read(element.getClass(), occ.cel)) != null ) 
            {
            	out.add(element);
            }
                
        } catch (IOException e) 
        {e.printStackTrace();}
        catch (InstantiationException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        finally 
        {
                if( beanReader != null ) 
                {
                        try 
                        {beanReader.close();}
                        catch (IOException e) 
                        {e.printStackTrace();}
                }
        }
	}
	
	/**
	 * 
	 * Read a General CSV file result is returned as a vector of Generics
	 * 
	 * @param CsvFilename Name of the filename to open
	 * @return out output vector
	 */
	
	public Vector<E> Read(String CsvFilename)
	{
		Vector<E> out = new Vector<E>();
		
        ReadGeneral(CsvFilename,out);
        
        return out;
	}
	
	/**
	 * 
	 * Read a CSV file result is returned as a vector of Generics
	 * 
	 * @param CsvFilename Name of the filename to open
	 * @param OutputChoose Output choose
	 * @return out output vector
	 */
	
	public Vector<E> Read(String CsvFilename, OutputChoose occ)
	{
		Vector<E> out = new Vector<E>();
		
        Readv(CsvFilename,out,occ);
        
        return out;
	}
	
	
	/**
	 * 
	 * Write a CSV file, perform an automatical conversion from the element inside vector
	 * and the internal type, setData(T) where T is the type of the sigle element in the
	 * out vector has to be implemented in the POJO Class (aka internal type)
	 * 
	 * @param CsvFilename Filename
	 * @param out Input file of object
	 * @param occ Output choose
	 */
	
	@SuppressWarnings("unchecked")
	public void Write(String CsvFilename, Vector<?> out, OutputChoose occ, boolean append)
	{   
		if (out.size() == 0)
			return ;
		
		Class<?> cl = out.get(0).getClass();
		
        ICsvDozerBeanWriter beanWriter = null;
        try 
        {
                beanWriter = new CsvDozerBeanWriter(new FileWriter(CsvFilename,append),
                        c);
                
                E element = null;
				try {
					element = p.newInstance();
				} catch (InstantiationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				} catch (IllegalAccessException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				}
                
                // configure the mapping from the fields to the CSV columns
                beanWriter.configureBeanMapping(element.getClass(), occ.map);
                
                // write the header and metainformation
                if (append == false)
                {
                	beanWriter.writeHeader(occ.map);
                
                	// Write meta information
                
                	for (int i = 0 ; i < fld.size() ;i++)
                	{
                		beanWriter.writeComment("%" + fld.get(i).p1 + ":" + fld.get(i).p2);
                	}
                }
      
                // write the beans
                
                Method m = null;
                
                try {
					m = element.getClass().getMethod("setData",cl);
				} catch (NoSuchMethodException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				} catch (SecurityException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				}
                
                for( int i = 0 ; i < out.size() ; i++ ) 
                {
                	try {
						element = p.newInstance();
					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}

					try {
						m.invoke(element, out.get(i));
					} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

                    beanWriter.write(element, occ.cel);
                }
                
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        finally 
        {
                if( beanWriter != null ) 
                {
                        try 
                        {beanWriter.close();}
                        catch (IOException e)
                        {e.printStackTrace();}
                }
        }
	}
	
	/**
	 * 
	 * Set an integer property of a vector of class from r1 to r2
	 * 
	 * @param property Property to set, setX() must be defined in the class
	 *        where X is the value of the string property
	 * @param v Vector
	 * @param number value to set
	 * @param r1 start element in the vector
	 * @param r2 end element in the vector
	 */
	
	private void setProperty(String property, Vector<E> v, int number, int r1, int r2)
	{
		Method m;
		try {
			if (r1 >= v.size())
				return;
				
			m = v.get(r1).getClass().getMethod("set" + property,int.class);
			for (int i = r1 ; i <= r2 ; i++)
			{
					m.invoke(v.get(i), number);
			}
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * Stitch CSV files with unknown format in one, converting to a choosen format
	 * set the property specified to base + n where n run across the files
	 * (Example usefull to enumerate frames if each file is a frame)
	 * 
	 * @param output files to read
	 * @param Sttch output file name
	 * @param occ format choose
	 * @param base number
	 * @return true if success 
	 */
	
    public boolean StitchConvert(String output[], String Sttch , OutputChoose occ, String property, int base)
    {
    	int prev_id = 0;
		Vector<E> out = new Vector<E>();
		
		if (output.length == 0)
			return false;
		
		setCSVPreferenceFromFile(output[0]);
		OutputChoose occr = ReadGeneral(output[0],out);
		if (occr == null)
			return false;
		
		setProperty(property,out,base,prev_id,out.size()-1);
		
		for (int i = 1 ; i < output.length ; i++)
		{
			prev_id = out.size();
			Readv(output[i],out,occr);
			setProperty(property,out,base+i,prev_id,out.size()-1);
		}
		
		Write(Sttch, out, occ, false);
    	
    	return true;
    }
	
	/**
	 * 
	 * Stitch CSV files with unknown format in one, converting to a choosen format
	 * 
	 * @param output files to read
	 * @param Sttch output file name
	 * @param occ format choose
	 * @return true if success 
	 */
	
    public boolean StitchConvert(String output[], String Sttch , OutputChoose occ)
    {			
		Vector<E> out = new Vector<E>();
		
		if (output.length == 0)
			return false;
		
		setCSVPreferenceFromFile(output[0]);
		OutputChoose occr = ReadGeneral(output[0],out);
		if (occr == null)
			return false;
		
		for (int i = 1 ; i < output.length ; i++)
		{
			Readv(output[i],out,occr);
		}
		
		Write(Sttch, out, occ, false);
    	
    	return true;
    }
		
/*    public boolean Stitch(String output[], String Sttch , OutputChoose occ)
    {
		Vector<?> v = Read(output, occ);
		
		Write(Sttch, v, occ, false);
    	
    	return true;
    }*/
    
    /**
     * 
     * Stitch CSV files in one with an unknown (but equal between files)
     * format (the first CSV format file drive the output
     * conversion)
     * 
     * @param csvs files to stitch
     * @param Sttch output stitched file
     * @return
     */
    
    public boolean Stitch(String csvs[], String Sttch)
    {
    	if (csvs.length == 0)
    		return false;
		Vector<E> out = new Vector<E>();
		
		OutputChoose occ = ReadGeneral(csvs[0],out);
		if (occ == null)
			return false;
		
		for (int i = 1 ; i < csvs.length ; i++)
		{
			Readv(csvs[i],out,occ);
		}
		
		Write(Sttch, out, occ, false);
    	
    	return true;
    }
    
    /**
     * 
     * Set delimiter
     * 
     * @param d delimiter
     * 
     */
    
    public void setDelimiter(char d)
    {
    	CsvPreference.Builder bld = new CsvPreference.Builder('"', d, "\n");
    	bld.skipComments(new CommentExtendedCSV());
    	c = bld.build();
    }
    
    /**
     * 
     * Create a vector of the internal type from an array of unknown type
     * 
     * @param ar
     * @return
     */
    
    public Vector<?> getVector(ArrayList<?> ar)
    {
    	Vector<E> v = new Vector<E>();
    	
    	if (ar.size() == 0)
    		return v;
    	
    	Class<?> car = ar.get(0).getClass();
    	E element = null;    	
		Method m = null;
		
		try {
			element = p.newInstance();
			m = element.getClass().getMethod("setData",car);
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0 ; i < ar.size() ; i++)
		{
			try {
				element = p.newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				m.invoke(element, ar.get(i));
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			v.add(element);
		}
			
    	return v;
    }
    
    
    /**
     * 
     * Stitch the CSV files all together in the directory dir/dir_p[]
     * save the result in output_file + dir_p[]
     * "*" are substituted by "_"
     * 
     * @param dir_p list of directories
     * @param dir Base
     * @param output_file stitched file
     * @param Class<T> internal data for conversion
     * @return true if success, false otherwise
     */
    
    public static <T extends ICSVGeneral>boolean Stitch(String dir_p[], File dir, File output_file, MetaInfo ext[], Class<T> cls)
    {
		InterPluginCSV<?> csv = new InterPluginCSV<T>(cls);
    	
		for (int j = 0 ; j < dir_p.length ; j++)
		{
			File [] fl = new File(dir + File.separator + dir_p[j].replace("*", "_")).listFiles();
			if (fl == null)
				continue;
			int nf = fl.length;
			
			String str[] = new String[nf];
			
			for (int i = 1 ; i <= nf ; i++)
			{
				if(fl[i-1].getName().endsWith(".csv"))
					str[i-1] = fl[i-1].getAbsolutePath();
			}
			
			if (ext != null)
			{
				for (int i = 0 ; i < ext.length ; i++)
				csv.setMetaInformation(ext[i].par, ext[i].value);
			}
			
			csv.Stitch(str, output_file + dir_p[j]);
		}
    	
    	return true;
    }
    
    /**
     * 
     * Stitch the CSV files all together in the directory dir/output[]
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
     * 
     */
    
    public static <T extends ICSVGeneral>boolean StitchConvert(String dir_p[], File dir, File output_file , MetaInfo ext[], OutputChoose occ, Class<T> cls)
    {
		InterPluginCSV<?> csv = new InterPluginCSV<T>(cls);
    	
		for (int j = 0 ; j < dir_p.length ; j++)
		{
			File [] fl = new File(dir + File.separator + dir_p[j].replace("*", "_")).listFiles();
			if (fl == null)
				return false;
			
			int nf = fl.length;
			
			String str[] = new String[nf];
			
			for (int i = 1 ; i <= nf ; i++)
			{
				if(fl[i-1].getName().endsWith(".csv"))
					str[i-1] = fl[i-1].getAbsolutePath();
			}
			
			if (ext != null)
			{
				for (int i = 0 ; i < ext.length ; i++)
				csv.setMetaInformation(ext[i].par, ext[i].value);
			}
			csv.StitchConvert(str, output_file + dir_p[j].replace("*", "_"), occ,"Frame",0);
		}
    	
    	return true;
    }
}