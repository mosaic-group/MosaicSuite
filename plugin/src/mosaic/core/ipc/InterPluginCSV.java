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
import org.supercsv.io.dozer.CsvDozerBeanReader;
import org.supercsv.io.dozer.CsvDozerBeanWriter;
import org.supercsv.io.dozer.ICsvDozerBeanReader;
import org.supercsv.io.dozer.ICsvDozerBeanWriter;
import org.supercsv.prefs.CsvPreference;


/**
*
* IPC this is an extended CSV (Comma separated value format)
*
* This class is able to read, generate and convert CSV file
* It use SuperCSV
*
* The class is InterPluginCSV<E extends ICSVGeneral & Outdata<S>, S>
* 
* Where E is a User choosen POJO JavaBeans class that extends ICSVGeneral
* aka (Internal type)
* 
* Note The CSV format is not related to the POJO Class but to OutputChoose
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

public class InterPluginCSV<E extends ICSVGeneral & Outdata<?>>
{
	private Class<E> p;
	
	public InterPluginCSV(Class<E> p_)
	{
		p = p_;
	}
	
	/**
	 * 
	 * Read more CSV files result is stored in out
	 * 
	 * @param CsvFilename Name of the filename to open
	 * @param OutputChoose output choose
	 * @return out output vector
	 */
	
	public Vector<? extends Outdata<?>> Read(String CsvFilename[], OutputChoose occ)
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
        	beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), CsvPreference.STANDARD_PREFERENCE);
                
            E element = p.newInstance();
            
            String[] map = beanReader.getHeader(true); // ignore the header
            beanReader.configureBeanMapping(element.getClass(), map);
            
            CellProcessor c[] = new CellProcessor[map.length];
            for (int i = 0 ; i < c.length ; i++)
            {
            	try {
					c[i] = (CellProcessor) PropertyUtils.getProperty(new ProcessorGeneral(), "Processor" + map[i]);
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
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
        	beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), CsvPreference.STANDARD_PREFERENCE);
                
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
	 * Read a General CSV file result is stored in out
	 * 
	 * @param CsvFilename Name of the filename to open
	 * @return out output vector
	 */
	
	public Vector<? extends ICSVGeneral> Read(String CsvFilename)
	{
		Vector<E> out = new Vector<E>();
		
        ReadGeneral(CsvFilename,out);
        
        return out;
	}
	
	/**
	 * 
	 * Read a CSV file result is stored in out
	 * 
	 * @param CsvFilename Name of the filename to open
	 * @param OutputChoose Output choose
	 * @return out output vector
	 */
	
	public Vector<? extends Outdata<?>> Read(String CsvFilename, OutputChoose occ)
	{
		Vector<E> out = new Vector<E>();
		
        Readv(CsvFilename,out,occ);
        
        return out;
	}
	
	
	/**
	 * 
	 * Write a CSV file, perform an automatical conversion from the element inside vector
	 * and the internal type 
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
                        CsvPreference.STANDARD_PREFERENCE);
                
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
                
                // write the header
                if (append == false)
                	beanWriter.writeHeader(occ.map);
                
                // write the beans
                
                Method m = null;
                
                try {
					m = element.getClass().getMethod("setData",cl);
				} catch (NoSuchMethodException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SecurityException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
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
	 * Stitch CSV files in one with a choose format
	 * 
	 * @param output files to read
	 * @param Sttch output file name
	 * @param occ format choose
	 * @return true if success 
	 */
	
    public boolean Stitch(String output[], String Sttch , OutputChoose occ)
    {			
		Vector<? extends Outdata<?>> v = Read(output, occ);
			
		Write(Sttch, v, occ, false);
    	
    	return true;
    }
    
    /**
     * 
     * Stitch CSV files in one with an unknown (but equal between files)
     * format (the first CSV format file drive the output
     * conversion)
     * 
     * @param output files to stitch
     * @param Sttch output stitched file
     * @param dir
     * @return
     */
    
    public boolean Stitch(String output[], String Sttch , File dir)
    {
		Vector<E> out = new Vector<E>();
		
		OutputChoose occ = ReadGeneral(output[0],out);
		
		for (int i = 1 ; i < output.length ; i++)
		{
			Readv(output[i],out,occ);
		}
		
		Write(Sttch, out, occ, false);
    	
    	return true;
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
		}
			
    	return v;
    }
}