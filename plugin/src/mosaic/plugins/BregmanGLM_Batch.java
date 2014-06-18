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
import mosaic.core.utils.Segmentation;
import mosaic.region_competition.Settings;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.io.Opener;


public class BregmanGLM_Batch implements PlugInFilter, Segmentation
{
	private ImagePlus OriginalImagePlus = null;
	private String savedSettings;
	
	public int setup(String arg0, ImagePlus active_img) 
	{
		// init basic structure
		
		Analysis.init();
		
		// if is a macro get the arguments from macro arguments
		
		if (IJ.isMacro())
			arg0 = Macro.getOptions();
		
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
			Pattern min = Pattern.compile("min");
			Pattern max = Pattern.compile("max");
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
			
			// min

			matcher = min.matcher(arg0);
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
						String norm = matcher.group(0);
						
						Analysis.norm_min = Double.parseDouble(norm);
						System.out.println("min norm " + Analysis.norm_min);
					}
				}
			}
			
			// max
			
			matcher = max.matcher(arg0);
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
						String norm = matcher.group(0);
						
						Analysis.norm_max = Double.parseDouble(norm);
						System.out.println("max norm " + Analysis.norm_max);
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
		int l=0;
		if (args != null)
			l = args.length;
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
			// 
			if (batch == true)
				Analysis.p.dispwindows = false;
			
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

	/**
	 * 
	 * Get Mask images
	 * @return set of possible output
	 * 
	 */
	public String[] getMask(ImagePlus aImp) 
	{
		String[] gM = new String[2];
		gM[0] = new String(aImp.getTitle() + "_seg_c1_RGB.tif");
		gM[1] = new String(aImp.getTitle() + "_seg_c2_RGB.tif");
		return gM;
	}
	
	/**
	 * 
	 * Get regions list
	 * 
	 * @param aImp
	 * @return set of possible ouput
	 */
	
	public String[] getRegions(ImagePlus aImp)
	{
		String[] gM = new String[2];
		gM[0] = new String(aImp.getTitle() + "_Seg_c1_RGB.tif");
		gM[1] = new String(aImp.getTitle() + "_Seg_c2_RGB.tif");
		return gM;
	}

	@Override
	public String[] getRegionList(ImagePlus aImp) 
	{
		String[] gM = new String[2];
		gM[0] = new String(aImp.getTitle() + "_ObjectsData_c1.csv");
		gM[1] = new String(aImp.getTitle() + "_ObjectsData_c2.csv");
		return gM;
	}
}
