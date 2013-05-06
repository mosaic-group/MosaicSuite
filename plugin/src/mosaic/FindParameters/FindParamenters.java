package mosaic.FindParameters; 

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilterRunner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author      Incardona Pietro <incardon@mpi-cbg.de>
 * @version     1.0
 * @since       2013-04-29
 */

/**
 * Register the set of input paramenters required for preanalysis  (1)
 *
 * Given a StateInput class it analyze the class members, for each one
 * check for the presence of the annotator @Input (Plugins="Plugin to run",
 * par_in=field, ... , out=par_out)    [2]
 * where Plugins is the plugins to call, par_in is the set of paramenters required
 * the possible value are fixed value or dependent from another State input parameters
 * The order for plugins call produced by the dependency is internaly computed.
 * 
 * out is the out paramenters of the plugins to map
 * 
 * Example:
 * 
 * class ExampleStateInput
 * {
 *    @Input( Plugins="Cool_plugins", out=getAttribute() )
 *    int par1
 *    
 *    @Input( Plugins="Other_Cool_plugins", "par1_=<par1>", "par2_=0.5", out=par_out )
 *    float par2;    
 * }
 * 
 * 
 * The following ExampleInputClass produce the following pipeline
 *
 * Cool_plugins is called with no parameters, par1 is filled with the out attribute produced by the
 * plugins. After that the Other_CoolPlugins is calledv with par2_=0.5 and 
 * par1_=Other_Cool_plugins.par_out and the Other_Cool_plugins.par_out is mapped to par2 (by assignement)
 *
 *
 * @param  Get the StateInput class (3)
 * @return void.
 */

class FindParameters<StateInput, StateOutput>
{
	List<Input> lst;
	List<Field> out;
	
	StateInput ip;
	
	FindParameters(StateInput ip_)
	{
		ip = ip_;
		
		// Get the sequence of plugins to run
		
		for (Field field : ip.getClass().getDeclaredFields()) 
		{
			int n_par = 0;
			while (lst.size() != ip.getClass().getAnnotations().length)
			{
				for (Annotation n : field.getAnnotations())
				{
					Input p = (Input) n;
					
					if (p.paramenters().length == n_par)
					{
						CheckPipeline(p);
						
						lst.add(p);
						out.add(field);
					}
				}
				n_par++;
			}	
		}
	}
	
	// check that is possible create a pipeline from the list
	
	private void CheckPipeline(Input p)
	{
		int np = 0;
		List<String> pl = Arrays.asList(p.paramenters());
		List<String> vl = Arrays.asList(p.values());
		for (Input pp : lst)
		{
			ListIterator<String> pli = pl.listIterator();
			ListIterator<String> vli = vl.listIterator();
			while (pli.hasNext() && vli.hasNext())
			{
				String par_f = pli.next();
				String val_f = vli.next();
				if (val_f.charAt(0) == '<' && val_f.charAt(val_f.length()-1) == '>')
				{
					if (par_f.compareTo(pp.out()) == 0)
					{
						pli.remove();
						vli.remove();
					}
				}
				else
				{
					pli.remove();
					vli.remove();
				}
			}
		}
		
		// if we have parameters, some of them are not linked and
		// we cannot create a pipeline
		
		if (pl.size() != 0)
		{
			ListIterator<String> pli = pl.listIterator();
			ListIterator<String> vli = vl.listIterator();
			while (pli.hasNext() && vli.hasNext())
			{
				String par_f = pli.next();
				String val_f = vli.next();
				System.err.println("Plugins: " + p.plugins() + " require the parameter " + par_f + " linked to " + val_f);
			}
			
			throw new RuntimeException("Error not all parameters in the pipeline are linked");
		}
	}
	
	// Compose parameter string
	
	private String Compose(String[] a, String[] b)
	{
		String tmp = new String();
		
		for (int i = 0 ; i < a.length && i < b.length ; i++ )
		{
			tmp += a[i] + "=" + b[i];
		}
		return tmp;
	}
	
	// Set StateInput field
	
	private void SetObject(Field f,Object obj)
	{
		try 
		{
			if (f.getType().equals("double"))
			{
				f.setDouble(ip,(double) obj);
			}
			else if (f.getType().equals("float"))
			{
				f.setFloat(ip,(float) obj);
			}
			else if (f.getType().equals("int"))
			{
				f.setInt(ip,(int) obj);
			}
			else if (f.getType().equals("boolean"))
			{
				f.setBoolean(ip,(boolean) obj);
			}	
		}
		catch (IllegalArgumentException | IllegalAccessException e) 
		{e.printStackTrace();}
	}
	
	
	/*
	 * Process the pipeline and generate the StateInput
	 * 
	 * @param  ImagePlus ip image
	 * @return void.
	 */
	public void ProcessPipeline(ImagePlus ip)
	{
		ListIterator lst_i = lst.listIterator();
		ListIterator out_i = out.listIterator();
		while (lst_i.hasNext() && out_i.hasNext())
		{
			Input p = (Input) lst_i.next();
			Field f = (Field) out_i.next();
			
			ClassLoader loader = ClassLoader.getSystemClassLoader();
			try 
			{
				Class plugin = loader.loadClass(p.plugins()).newInstance().getClass();
				if (p.out().charAt(p.out().length()-1) == ')')
				{
					// only void parameters function
					
					Method m = plugin.getMethod(p.out(), null);
					SetObject(f,m.invoke(plugin)); 
				}
				else
				{
					Field fp = plugin.getDeclaredField(p.out());
					SetObject(f,fp.get(plugin));
				}
			}
			catch (InstantiationException e)
			{e.printStackTrace();}
			catch (IllegalAccessException e)
			{e.printStackTrace();}
			catch (ClassNotFoundException e)
			{e.printStackTrace();}
			catch (NoSuchMethodException | SecurityException e) 
			{e.printStackTrace();} 
			catch (IllegalArgumentException e) 
			{e.printStackTrace();}
			catch (InvocationTargetException e)
			{e.printStackTrace();} 
			catch (NoSuchFieldException e) 
			{e.printStackTrace();}
			
			IJ.runPlugIn(ip,p.plugins(),Compose(p.paramenters(),p.values()));
			
			// We cannot use run because we need the Filter instance in order to get
			// the output
			
		}
	}

	
}