package mosaic.core.utils;

import static org.junit.Assert.fail;
import ij.IJ;
import ij.ImagePlus;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import mosaic.core.GUI.ProgressBarWin;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.plugins.PlugInFilterExt;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedIntType;

/**
 * 
 * This class expose static member to test every plugins in our Mosaic Toolsuite
 * 
 * 
 * 
 * @author Pietro Incardona
 *
 */

public class MosaicTest
{
	private static String getTestEnvironment()
	{
		return IJ.getDirectory("temp") + File.separator + "test" + File.separator;
	}
	
	private static void prepareTestEnvironment(ProgressBarWin wp, ImgTest tmp)
	{
		wp.SetStatusMessage("Testing... " + new File(tmp.base).getName());
		
		// Save on tmp and reopen
		
		String tmp_dir = getTestEnvironment();
		
		// Remove everything there
		
		try {
			ShellCommand.exeCmdNoPrint("rm -rf " + tmp_dir);
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		} catch (InterruptedException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		// make the test dir
		
		try {
			ShellCommand.exeCmd("mkdir " + tmp_dir);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		
		for (int i = 0 ; i < tmp.img.length ; i++)
		{
			String temp_img = tmp_dir + tmp.img[i].substring(tmp.img[i].lastIndexOf(File.separator)+1);
			IJ.save(MosaicUtils.openImg(tmp.img[i]), temp_img);
		}
			
//		FileSaver fs = new FileSaver(MosaicUtils.openImg(tmp.img));
//		fs.saveAsTiff(temp_img);
		
		// copy the config file
		
		try {
			
			for (int i = 0 ; i < tmp.setup_files.length ; i++)
			{
				String str = new String();
				str = IJ.getDirectory("temp") +  File.separator + tmp.setup_files[i].substring(tmp.setup_files[i].lastIndexOf(File.separator)+1);
				ShellCommand.exeCmdNoPrint("cp -r " + tmp.setup_files[i] + " " + str);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private static <T extends ICSVGeneral> void processResult(PlugInFilterExt BG, ImgTest tmp, ProgressBarWin wp,Class<T> cls)
	{
		// Check the results
		
		// Save on tmp and reopen
		
		String tmp_dir = getTestEnvironment();
		
		// Check if there are job directories
		
		String[] cs = ClusterSession.getJobDirectories(0, tmp_dir);
		String[] csr = ClusterSession.getJobDirectories(0, tmp.base);
		if (cs != null && cs.length != 0)
		{
			// Sort into ascending order
			
//			Arrays.sort(cs);
			
			// Check if result_imgs has the same number
			
			if (csr.length % cs.length != 0)
			{
				fail("Error: Image result does not match the result");
			}
			
			// replace the result dir with the job id
			
			for (int i = 0 ; i < cs.length ; i++)
			{
				String fr[] = MosaicUtils.readAndSplit(cs[i] + File.separator + "JobID");
				String fname = MosaicUtils.removeExtension(fr[2]);
				String JobID = fr[0];
				
				int id = ShellCommand.getIDfromFileList(tmp.result_imgs_rel, fname);
				
				tmp.result_imgs_rel[id] = tmp.result_imgs_rel[id].replace("*", JobID);
				
				
			}
			
			for (int i = 0 ; i < csr.length ; i++)
			{
				String fr[] = MosaicUtils.readAndSplit(csr[i] + File.separator + "JobID");
				String fname = MosaicUtils.removeExtension(fr[2]);
				String JobID = fr[0];
				
				int id = ShellCommand.getIDfromFileList(tmp.result_imgs, fname);
				
				tmp.result_imgs[id] = tmp.result_imgs[id].replace("*", JobID);		
			}
			
			// same things for csv
			
			for (int i = 0 ; i < cs.length ; i++)
			{
				String fr[] = MosaicUtils.readAndSplit(cs[i] + File.separator + "JobID");
				String fname = MosaicUtils.removeExtension(fr[2]);
				String JobID = fr[0];
				
				int id = ShellCommand.getIDfromFileList(tmp.csv_results_rel, fname);
				
				tmp.csv_results_rel[id] = tmp.csv_results_rel[id].replace("*", JobID);
			}
			
			for (int i = 0 ; i < csr.length ; i++)
			{
				String fr[] = MosaicUtils.readAndSplit(csr[i] + File.separator + "JobID");
				String fname = MosaicUtils.removeExtension(fr[2]);
				String JobID = fr[0];

				int id = ShellCommand.getIDfromFileList(tmp.csv_results, fname);
				
				tmp.csv_results[id] = tmp.csv_results[id].replace("*", JobID);
			}
		}
		
		int cnt = 0;
		
        // create the ImgOpener
        ImgOpener imgOpener = new ImgOpener();
		
		for (String rs : tmp.result_imgs)
		{
	        // open with ImgOpener. The type (e.g. ArrayImg, PlanarImg, CellImg) is
	        // automatically determined. For a small image that fits in memory, this
	        // should open as an ArrayImg.
	        Img<?> image = null;
	        Img< ? > image_rs = null;
			try {
				// 
				
				wp.SetStatusMessage("Checking... " + new File(rs).getName());
				
				image = imgOpener.openImgs(rs).get(0);
			
				String filename = null;

				filename = tmp_dir + File.separator + tmp.result_imgs_rel[cnt];

					
				// open the result image
			
	        	image_rs = (Img<?>) imgOpener.openImgs(filename).get(0);
			} catch (ImgIOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (java.lang.UnsupportedOperationException e)	{
				e.printStackTrace();
				fail("Error: Image " + rs + " does not match the result");
			}
	        
			// compare
			
			if (MosaicUtils.compare(image, image_rs) == false)
			{
				fail("Error: Image " + rs + " does not match the result");
			}
			
			cnt++;
		}
		
		// Close all images
		
		if (BG != null)
			BG.closeAll();
		
		// Open csv
		
		cnt = 0;
		
		Arrays.sort(tmp.csv_results);
		Arrays.sort(tmp.csv_results_rel);
		
		for (String rs : tmp.csv_results)
		{
			wp.SetStatusMessage("Checking... " + new File(rs).getName());
			
			InterPluginCSV<T> iCSVsrc = new InterPluginCSV<T>(cls);
		
			String filename = null;
			filename = tmp_dir + File.separator + tmp.csv_results_rel[cnt];
			
			iCSVsrc.setCSVPreferenceFromFile(filename);
			Vector<T> outsrc = iCSVsrc.Read(filename);
			
			InterPluginCSV<T> iCSVdst = new InterPluginCSV<T>(cls);
			iCSVdst.setCSVPreferenceFromFile(rs);
			Vector<T> outdst = iCSVdst.Read(rs);
			
			if (outsrc.size() != outdst.size() || outsrc.size() == 0)
				fail("Error: CSV outout does not match");
			
			for (int i = 0 ; i < outsrc.size() ; i++)
			{
				if (outsrc.get(i).equals(outdst.get(i)))
				{
					// Maybe the order is changed
					int j = 0;
					for (j = 0 ; j < outdst.size() ; j++)
					{
						if (outsrc.get(i).equals(outdst.get(i)))
						{
							break;
						}
					}
					
					if (j == outdst.size())
						fail("Error: CSV output does not match");
				}
			}
			
			cnt++;
		}
	}
	
	/**
	 * 
	 * Test the plugin filter
	 * 
	 * @param BG plugins filter filter
	 * @param testset String that indicate the test to use (all the test are in Jtest_data folder)
	 * @param Class<T> Class for reading csv files used for InterPlugInCSV class
	 */
	
	public static <T extends ICSVGeneral> void testPlugin(PlugInFilterExt BG, String testset,Class<T> cls)
	{
		// Set the plugin in test mode
		BG.setIsOnTest(true);
		
		// Save on tmp and reopen
		
		String tmp_dir = getTestEnvironment();
		
		// 
		
		ProgressBarWin wp = new ProgressBarWin();
		
		// Get all test images
		
		ImgTest imgT[] = MosaicUtils.getTestImages(testset);
		
		if (imgT == null)
		{
			fail("No Images to test");
			return;
		}
		
		// for each image
		
		for (ImgTest tmp : imgT)
		{
			prepareTestEnvironment(wp,tmp);
			
			// Create a plugin filter
			
			int rt = 0;
			if (tmp.img.length == 1)
			{
				String temp_img = tmp_dir + tmp.img[0].substring(tmp.img[0].lastIndexOf(File.separator)+1);
				ImagePlus img = MosaicUtils.openImg(temp_img);
				img.show();
			
				rt = BG.setup(tmp.options, img);
			}
			else
			{
				rt = BG.setup(tmp.options,null);
			}
			
			if (rt != tmp.setup_return)
			{
				fail("Setup error expecting: " + tmp.setup_return + " getting: " + rt);
			}
			
			// run the filter
			
			BG.run(null);
			
			processResult(BG,tmp,wp,cls);
		}
		
		wp.dispose();
	}
	
	/**
	 * 
	 * Test the plugin filter
	 * 
	 * @param plugin command to test
	 * @param testset String that indicate the test to use (all the test are in Jtest_data folder)
	 * @param Class<T> Class for reading csv files used for InterPlugInCSV class
	 */
	
	public static <T extends ICSVGeneral> void testPlugin(String plugin_command,String options, String testset,Class<T> cls)
	{
		ProgressBarWin wp = new ProgressBarWin();
		
		// Get all test images
		
		ImgTest imgT[] = MosaicUtils.getTestImages(testset);
		
		if (imgT == null)
		{
			fail("No Images to test");
			return;
		}
		
		// for each image
		
		for (ImgTest tmp : imgT)
		{
			prepareTestEnvironment(wp,tmp);
			
			// run the command
			
			IJ.run(plugin_command,options);
			
			// Check the results
			
			processResult(null,tmp,wp,cls);
		}
		
		wp.dispose();
	}
}