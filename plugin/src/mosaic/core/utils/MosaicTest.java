package mosaic.core.utils;

import static org.junit.Assert.fail;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import io.scif.SCIFIOService;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.scijava.Context;
import org.scijava.app.AppService;
import org.scijava.app.StatusService;

import mosaic.core.cluster.ClusterSession;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.plugins.PlugInFilterExt;
import mosaic.test.framework.SystemOperations;
import net.imglib2.img.Img;


/**
 * This class expose static member to test every plugins in our Mosaic Toolsuite
 * 
 * @author Pietro Incardona
 *
 */
public class MosaicTest
{	
    //private static final Logger logger = LoggerFactory.getLogger(MosaicTest.class);
    protected static final Logger logger = Logger.getLogger(MosaicTest.class);
    //private static final Logger logger = logger.getLogger( MosaicTest.class.getName() );
	public static void prepareTestEnvironment(ImgTest tmp)
	{
//		logger.info("Testing... " + new File(tmp.base).getName());
		String tmp_dir = SystemOperations.getCleanTestTmpPath();
		
		for (int i = 0 ; i < tmp.img.length ; i++)
		{
			String temp_img = tmp_dir + tmp.img[i].substring(tmp.img[i].lastIndexOf(File.separator)+1);
			IJ.save(MosaicUtils.openImg(tmp.img[i]), temp_img);
		}
			
		// copy the config file		
		try {
			
			for (int i = 0 ; i < tmp.setup_files.length ; i++)
			{
				String str = new String();
				str = IJ.getDirectory("temp") +  File.separator + tmp.setup_files[i].substring(tmp.setup_files[i].lastIndexOf(File.separator)+1);
				logger.info("Setup file: ["  + str + "]");
				ShellCommand.exeCmdNoPrint("cp -r " + tmp.setup_files[i] + " " + str);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	private static <T extends ICSVGeneral> void processResult(PlugInFilterExt BG, ImgTest tmp, Class<T> cls)
	{
		String tmp_dir = SystemOperations.getTestTmpPath();
		
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
	
		for (String rs : tmp.result_imgs) {
	        Img<?> image = null;
	        Img<?> image_rs = null;
	        
			try {
				logger.info("Checking... " + new File(rs).getName());
				
				String filename = tmp_dir + File.separator + tmp.result_imgs_rel[cnt];
				
				logger.info("Original img: " + rs);
				logger.info("Result img: " + filename);
				
				// Create ImgOpener with some default context, without it, it search for already existing one
				ImgOpener imgOpener = new ImgOpener(new Context(SCIFIOService.class, AppService.class, StatusService.class ));
				// By default ImgOpener produces a lot of logs, this is one of the ways to switch it off. 
				imgOpener.log().setLevel(0);
				
				image = (Img<?>) imgOpener.openImgs(rs).get(0); // ImagePlusAdapter.wrap(new Opener().openImage(rs));
                image_rs = (Img<?>) imgOpener.openImgs(filename).get(0); //ImagePlusAdapter.wrap(new Opener().openImage(filename));
			}
			catch (java.lang.UnsupportedOperationException e)	{
				e.printStackTrace();
				fail("Error: Image " + rs + " does not match the result");
			} catch (ImgIOException e) {
                e.printStackTrace();
                fail("Failed to open image.");
            }

			if (MosaicUtils.compare(image, image_rs) == false) {
				fail("Error: Image " + rs + " does not match the result");
			}
			
			cnt++;
		}
		
		// Close all images
		if (BG != null) {
			BG.closeAll();
		}
		
		// Open csv
		cnt = 0;
		
		Arrays.sort(tmp.csv_results);
		Arrays.sort(tmp.csv_results_rel);
		
		for (String rs : tmp.csv_results)
		{
			logger.info("Checking... " + new File(rs).getName());
			
			InterPluginCSV<T> iCSVsrc = new InterPluginCSV<T>(cls);
		
			String filename = null;
			filename = tmp_dir + File.separator + tmp.csv_results_rel[cnt];
			
			iCSVsrc.setCSVPreferenceFromFile(filename);
			Vector<T> outsrc = iCSVsrc.Read(filename);
			
			InterPluginCSV<T> iCSVdst = new InterPluginCSV<T>(cls);
			iCSVdst.setCSVPreferenceFromFile(rs);
			Vector<T> outdst = iCSVdst.Read(rs);
			
			if (outsrc.size() != outdst.size() || outsrc.size() == 0) {
			    logger.error("Error: CSV output does not match: " + filename + " vs. " + rs);
				fail("Error: CSV output does not match: " + filename + " vs. " + rs);
			}
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
	 * Test the plugin filter
	 * 
	 * @param BG plugins filter filter
	 * @param testset String that indicate the test to use (all the test are in Jtest_data folder)
	 * @param Class<T> Class for reading csv files used for InterPlugInCSV class
	 */
	public static <T extends ICSVGeneral> void testPlugin(PlugInFilterExt BG, String testset,Class<T> cls)
	{
		String tmp_dir = SystemOperations.getTestTmpPath();
		
		// Get all test images
		List<ImgTest> imgT = getTestData(testset);
		
		if (imgT == null || imgT.size() == 0) {
		    logger.error("No test images found for testcase [" + testset + "])");
			fail("No Images to test");
			return;
		}
		
		for (ImgTest tmp : imgT)
		{
			prepareTestEnvironment(tmp);
			
			// Create a plugin filter
			int rt = 0;
			if (tmp.img.length == 1) {
				String temp_img = tmp_dir + tmp.img[0].substring(tmp.img[0].lastIndexOf(File.separator)+1);
				ImagePlus img = MosaicUtils.openImg(temp_img);

				// TODO: to be decided what method (whether both?) should be used
				Interpreter.batchMode = true;
				tmp.options += "TEST";
				
				rt = BG.setup(tmp.options, img);
				
				logger.info("windowcount: " + WindowManager.getWindowCount());
				logger.info("Interpreter: " + Interpreter.getBatchModeImageCount());
//              
				int [] ids = WindowManager.getIDList();
				if (ids != null)
                for (int id : ids) {
                    logger.info("Filename: id=[" + id + "] name=[" + WindowManager.getImage(id).getTitle() + "]");
                    
                }
			}
			else {
				rt = BG.setup(tmp.options,null);
			}
			
			if (rt != tmp.setup_return) {
				fail("Setup error expecting: " + tmp.setup_return + " getting: " + rt);
			}
			
			// run the filter
			BG.run(null);
			
			processResult(BG,tmp,cls);
		}
		
	}
	
	/**
	 * Test the plugin filter
	 * 
	 * @param plugin command to test
	 * @param testset String that indicate the test to use (all the test are in Jtest_data folder)
	 * @param Class<T> Class for reading csv files used for InterPlugInCSV class
	 */
	public static <T extends ICSVGeneral> void testPlugin(String plugin_command,String options, String testset,Class<T> cls)
	{
		// Get all test images
		List<ImgTest> imgT = getTestData(testset);
		
		if (imgT == null || imgT.size() == 0) {
			fail("No Images to test");
			return;
		}
		
		for (ImgTest tmp : imgT) {
			prepareTestEnvironment(tmp);
			
			// run the command	
			IJ.run(plugin_command,options);
			
			// Check the results
			processResult(null,tmp,cls);
		}
	}
	
	/**
     * It return the test data for a given plugin name
     * 
     * @param aPluginName name of the plugin
     * @return an container with test data
     */
    static public List<ImgTest> getTestData(String aPluginName)
    { 
        List<ImgTest> it = new ArrayList<ImgTest>();
        
        String testFolder = SystemOperations.getTestDataPath() + File.separator + aPluginName + File.separator;

        // List all directories
        File dirs[] = new File(testFolder).listFiles();
        logger.info("Getting images from: ["  + testFolder + "]");
        if (dirs == null) {
            // Wrong path. Break execution intentionally. 
            throw new RuntimeException("Listing of [" + testFolder + "] directory failed.");
            //return null;
        }
        
        for (File dir : dirs)
        {       
            if (dir.isDirectory() == false) {
                // test data should be placed in directories. 
                // If regular file is found - ignore.
                continue;
            }
            
            ImgTest imgT;
        
            try
            {
                // Format of configuration file:
                //-----------------------
                // Image
                // options
                // setup file
                // Expected setup return
                // number of images results
                // ..... List of images result
                // number of csv results
                // ..... List of csv result

                // open configuration file
                String cfg = dir.getAbsolutePath() + File.separator + "config.cfg";
                BufferedReader br = new BufferedReader(new FileReader(cfg));
 
                imgT = new ImgTest();
            
                imgT.base = dir.getAbsolutePath();
                int nimage_file = Integer.parseInt(br.readLine());

                imgT.img = new String[nimage_file];
                for (int i = 0 ; i < imgT.img.length ; i++)
                {
                    imgT.img[i] = dir.getAbsolutePath() + File.separator + br.readLine();
                }
                
                imgT.options = br.readLine();
                int nsetup_file = Integer.parseInt(br.readLine());
                imgT.setup_files = new String[nsetup_file];
                
                for (int i = 0 ; i < imgT.setup_files.length ; i++)
                {
                    imgT.setup_files[i] = dir.getAbsolutePath() + File.separator + br.readLine();
                }
                
                imgT.setup_return = Integer.parseInt(br.readLine());
                
                int n_images = Integer.parseInt(br.readLine());
                imgT.result_imgs = new String[n_images];
                imgT.result_imgs_rel = new String[n_images];
                imgT.csv_results_rel = new String[n_images];
                
                for (int i = 0 ; i < imgT.result_imgs.length ; i++)
                {
                    imgT.result_imgs_rel[i] = br.readLine();
                    imgT.result_imgs[i] = dir.getAbsolutePath() + File.separator + imgT.result_imgs_rel[i];
                } 
                
                int n_csv_res = Integer.parseInt(br.readLine());
                imgT.csv_results = new String[n_csv_res];
                imgT.csv_results_rel = new String[n_csv_res];
                for (int i = 0 ; i < imgT.csv_results.length ; i++)
                {
                    imgT.csv_results_rel[i] = br.readLine();
                    imgT.csv_results[i] = dir.getAbsolutePath() + File.separator + imgT.csv_results_rel[i];
                }
                
                br.close();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
                return null;
            }
            
            it.add(imgT);
        }
        
        return it;
    }
}
