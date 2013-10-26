package mosaic.plugins;

import java.beans.PropertyDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;

import mosaic.bregman.Analysis;
import mosaic.bregman.GUIold;
import mosaic.bregman.GenericGUI;
import mosaic.bregman.Parameters;
import mosaic.bregman.output.CSVOutput;
import mosaic.region_competition.Settings;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;


public class BregmanGLM_Batch implements PlugInFilter 
{
	private ImagePlus OriginalImagePlus = null;
	private String savedSettings;
	
	public int setup(String arg0, ImagePlus active_img) 
	{
		// Initialize CSV format
		
		CSVOutput.initCSV();
		
		//
		
		String dir = IJ.getDirectory("temp");
		savedSettings = dir+"spb_settings.dat";
		
		try 
		{
			LoadConfig();
			
			// Command line interface search for config file
			
			String path;
			Pattern spaces = Pattern.compile("[\\s]*=[\\s]*");
			Pattern config = Pattern.compile("config");
			Pattern output = Pattern.compile("output");
			Pattern normalize = Pattern.compile("normalize");
			Pattern par[] = new Pattern[7];
			par[0] = Pattern.compile("method");
			par[1] = Pattern.compile("init");
			par[2] = Pattern.compile("ps_radius");
			par[3] = Pattern.compile("b_force");
			par[4] = Pattern.compile("c_flow_coeff");
			par[5] = Pattern.compile("c_flow_radius");
			par[6] = Pattern.compile("normalize");
			Pattern pathp = Pattern.compile("[a-zA-Z0-9/_.-]+");
			
			// config
			
			Matcher matcher = config.matcher(arg0);
			if (matcher.find())
			{
				String sub = arg0.substring(matcher.end());
				matcher = spaces.matcher(sub);
				if (matcher.find())
				{
					sub = sub.substring(matcher.end());
					matcher = pathp.matcher(sub);
					if (matcher.find())
					{
						path = matcher.group(0);
						
						LoadConfig(path);
						System.out.println("Loaded batch config");
					}
				}
			}

		}
		catch (ClassNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.OriginalImagePlus = active_img;
		//IJ.log("arg0 " + arg0);
		String[] args =ImageJ.getArgs();
		int l=args.length;
		//IJ.log("args" + l);
		boolean batch=false;
		for(int i=0; i<l; i++)
		{
			//IJ.log("arg" + i+ args[i]);
			if(args[i]!=null)
			{
				if (args[i].endsWith("batch")) batch=true;
			}
		}
		
		//IJ.log("batchmode" + batch);
		try 
		{
			GenericGUI window = new GenericGUI(batch,active_img);
			window.run("",active_img);
			
			SaveConfig(Analysis.p);
			
			//IJ.log("Plugin exiting");
			//Headless hd= new Headless("/Users/arizk/tmpt/");
			//Headless hd= new Headless("/gpfs/home/rizk_a/Rab7_b/");
			//window.frame.setVisible(true);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return DONE;
	}

	public void run(ImageProcessor imp) 
	{
	}

	void LoadConfig() throws ClassNotFoundException
	{
		try
		{
			FileInputStream fin = new FileInputStream(savedSettings);
			ObjectInputStream ois = new ObjectInputStream(fin);
			Analysis.p = (Parameters)ois.readObject();
			ois.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void LoadConfig(String ss) throws ClassNotFoundException
	{
		try
		{
			FileInputStream fin = new FileInputStream(ss);
			ObjectInputStream ois = new ObjectInputStream(fin);
			Analysis.p = (Parameters)ois.readObject();
			ois.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void SaveConfig(Parameters p, String savePath) throws IOException
	{
		FileOutputStream fout = new FileOutputStream(savePath);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(p);
		oos.close();
	}
	
	void SaveConfig(Parameters p) throws IOException
	{
		FileOutputStream fout = new FileOutputStream(savedSettings);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(p);
		oos.close();
	}
}
