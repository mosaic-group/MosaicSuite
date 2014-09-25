package mosaic.plugins;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.imglib2.type.numeric.real.DoubleType;
import mosaic.bregman.Analysis;
import mosaic.bregman.Analysis.outputF;
import mosaic.bregman.GUIold;
import mosaic.bregman.GenericGUI;
import mosaic.bregman.Parameters;
import mosaic.bregman.output.CSVOutput;
import mosaic.core.psf.psf;
import mosaic.core.utils.MosaicUtils;
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


public class BregmanGLM_Batch implements Segmentation
{
	private ImagePlus OriginalImagePlus = null;
	private String savedSettings;
	GenericGUI window;
	boolean gui_use_cluster = false;
	
	/**
	 * 
	 * set to use the cluster
	 * 
	 */
	
	public void setUseCluster(boolean bl)
	{
		gui_use_cluster = bl;
	}
	
	public int setup(String arg0, ImagePlus active_img) 
	{
		// init basic structure
		
		Analysis.init();
		
		// if is a macro get the arguments from macro arguments
		
		if (IJ.isMacro())
			arg0 = Macro.getOptions();
		
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
		
		// Initialize CSV format
		
		CSVOutput.initCSV(Analysis.p.oc_s);
		
		this.OriginalImagePlus = active_img;
		//IJ.log("arg0 " + arg0);
		String[] args =ImageJ.getArgs();
		
		// Check the argument
		
		int l = 0;
		if (args != null)
			l=args.length;
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
			
			window = new GenericGUI(batch,active_img);
			window.setUseCluster(gui_use_cluster);
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
	
	/**
	 * 
	 * Close all the file
	 * 
	 */
	
	public void closeAll()
	{
		if (OriginalImagePlus != null)
		{
			OriginalImagePlus.close();
		}
		
		window.closeAll();
	}
	
	public static void SaveConfig(Parameters p, String savePath) throws IOException
	{
		FileOutputStream fout = new FileOutputStream(savePath);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		
		// Nullify PSF is not Serializable
		
		psf<DoubleType> psf_old = p.PSF;
		p.PSF = null;
		oos.writeObject(p);
		oos.close();
		p.PSF = psf_old;
		
	}
	
	/**
	 * 
	 * Save the Config file
	 * 
	 * @param p Setting file
	 * @throws IOException
	 */
	
	void SaveConfig(Parameters p) throws IOException
	{
		FileOutputStream fout = new FileOutputStream(savedSettings);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		
		// Nullify PSF is not Serializable
		
		psf<DoubleType> psf_old = p.PSF;
		p.PSF = null;
		oos.writeObject(p);
		oos.close();
		p.PSF = psf_old;
	}
	
	/**
	 * 
	 * Get Mask images name output
	 * @param aImp image
	 * @return set of possible output
	 * 
	 */
	public String[] getMask(ImagePlus aImp) 
	{
		String[] gM = new String[2];
		gM[0] = new String(Analysis.out[outputF.MASK.getNumVal()].replace("*", "_") + File.separator + Analysis.out[outputF.MASK.getNumVal()].replace("*", MosaicUtils.removeExtension(aImp.getTitle())) );
		gM[1] = new String(Analysis.out[outputF.MASK.getNumVal()+1].replace("*", "_") + File.separator + Analysis.out[outputF.MASK.getNumVal()+1].replace("*", MosaicUtils.removeExtension(aImp.getTitle())) );
		return gM;
	}
	
	/**
	 * 
	 * Get Mask images name output
	 * 
	 * @param aImp image
	 * @return set of possible output
	 */
	
	public String[] getRegions(ImagePlus aImp)
	{
		String[] gM = new String[2];
		gM[0] = new String(Analysis.out[outputF.MASK.getNumVal()].replace("*", "_") + File.separator + Analysis.out[outputF.MASK.getNumVal()].replace("*", MosaicUtils.removeExtension(aImp.getTitle())) );
		gM[1] = new String(Analysis.out[outputF.MASK.getNumVal()+1].replace("*", "_") + File.separator + Analysis.out[outputF.MASK.getNumVal()+1].replace("*", MosaicUtils.removeExtension(aImp.getTitle())) );
		return gM;
	}

	/**
	 * 
	 * Get CSV regions list name output
	 * 
	 * @param aImp image
	 * @return set of possible output
	 */
	
	@Override
	public String[] getRegionList(ImagePlus aImp) 
	{
		String[] gM = new String[4];
		gM[0] = new String(Analysis.out[outputF.OBJECT.getNumVal()].replace("*", "_") + File.separator + Analysis.out[outputF.OBJECT.getNumVal()].replace("*", MosaicUtils.removeExtension(aImp.getTitle())) );
		gM[1] = new String(Analysis.out[outputF.OBJECT.getNumVal()+1].replace("*", "_") + File.separator + Analysis.out[outputF.OBJECT.getNumVal()+1].replace("*", MosaicUtils.removeExtension(aImp.getTitle())) );
		
		// This is produced if there is a stitch operation
		
		gM[2] = new String(MosaicUtils.removeExtension(aImp.getTitle()) + Analysis.out[outputF.OBJECT.getNumVal()].replace("*", "_") );
		gM[3] = new String(MosaicUtils.removeExtension(aImp.getTitle()) + Analysis.out[outputF.OBJECT.getNumVal()+1].replace("*", "_") );
		
		return gM;
	}
	
	/**
	 * 
	 * Get name of the plugins
	 * 
	 */
	
	public String getName()
	{
		return "Squassh";
	}
}
