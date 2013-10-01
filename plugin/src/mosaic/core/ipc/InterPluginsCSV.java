package mosaic.core.ipc;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;


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
* This class is able to read and generate CSV file
* It use SuperCSV with Dozer library to parse it
*
* Metadata information can be stored, these kind of data are
* stored separately
*
* What you need is a Valid POJO JavaBeans class
* and a FIELD_MAPPING that map column class field
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

public class InterPluginCSV<E>
{
	/**
	 * 
	 * Read a CSV file result is stored in out
	 * 
	 * @param CsvFilename Name of the filename to open
	 * @param out output vector
	 * @param processors Columns processor
	 * @param FIELD_MAPPING Field mapping
	 */
	
	void Read(String CsvFilename, Vector<E> out, CellProcessor[] processors, String[] FIELD_MAPPING)
	{        
        ICsvDozerBeanReader beanReader = null;
        try 
        {
        	beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), CsvPreference.STANDARD_PREFERENCE);
                
            E element = null;
                
            beanReader.getHeader(true); // ignore the header
            beanReader.configureBeanMapping(element.getClass(), FIELD_MAPPING);
                
            while( (element = (E)beanReader.read(element.getClass(), processors)) != null ) 
            {
            	out.add(element);
            }
                
        } catch (IOException e) 
        {e.printStackTrace();}
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
	 * Write a CSV file
	 * 
	 * @param CsvFilename Filename
	 * @param out Input file of object
	 * @param processors Column processor
	 * @param FIELD_MAPPING Field Mapping
	 */
	
	void Write(String CsvFilename, Vector<E> out, CellProcessor[] processors, String[] FIELD_MAPPING)
	{   
		if (out.size() != 0)
			return ;
			
        ICsvDozerBeanWriter beanWriter = null;
        try 
        {
                beanWriter = new CsvDozerBeanWriter(new FileWriter("target/writeWithCsvDozerBeanWriter.csv"),
                        CsvPreference.STANDARD_PREFERENCE);
                
                // configure the mapping from the fields to the CSV columns
                beanWriter.configureBeanMapping(out.get(0).getClass(), FIELD_MAPPING);
                
                // write the header
                beanWriter.writeHeader("age", "consentGiven", "questionNo1", "answer1", "questionNo2", "answer2",
                        "questionNo3", "answer3");
                
                // write the beans
                for( final E tmp : out ) 
                {
                        beanWriter.write(tmp, processors);
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
}