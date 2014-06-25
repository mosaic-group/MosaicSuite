package mosaic.region_competition;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import mosaic.core.utils.ImgTest;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;
import mosaic.plugins.Region_Competition;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

public class Jtest 
{	
	@Test
	public void segmentation() 
	{
		// Create a Region Competition filter
		
		ImgTest imgT[] = MosaicUtils.getTestImages("Region_Competition");
		
		if (imgT == null)
		{
			fail("No Images to test");
			return;
		}
		
		for (ImgTest tmp : imgT)
		{
			// Save on tmp and reopen
			
			String tmp_dir = IJ.getDirectory("tmp");
			String file = tmp_dir + File.separator + tmp.img.getTitle();
			
			FileSaver fs = new FileSaver(tmp.img);
			fs.saveAsTiff(tmp_dir);
			
			// copy the config file
			
			try {
				ShellCommand.exeCmdNoPrint("cp -r " + tmp.setup_file + " " + IJ.getDirectory("tmp") + File.separator + tmp.setup_file);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			// Create a Region Competition filter
			
			Region_Competition RC = new Region_Competition();
			int rt = RC.setup(tmp.options, tmp.img);
			if (rt != tmp.setup_return);
			{
				fail("Setup error expecting: " + tmp.setup_return + " getting: " + rt);
			}
			
			// run the filter
			
			RC.run(null);
			
			// Check the results
			
			for (String rs : tmp.result_imgs)
			{
		        // create the ImgOpener
		        ImgOpener imgOpener = new ImgOpener();
		 
		        // open with ImgOpener. The type (e.g. ArrayImg, PlanarImg, CellImg) is
		        // automatically determined. For a small image that fits in memory, this
		        // should open as an ArrayImg.
		        Img<?> image = null;
		        Img< ? > image_rs = null;
				try {
					image = imgOpener.openImgs(rs).get(0);
				
					String filename = rs.substring(rs.lastIndexOf(File.separator)+1);
		        
					// open the result image
				
		        	image_rs = (Img<?>) imgOpener.openImgs(IJ.getDirectory("tmp") + File.separator + filename);
				} catch (ImgIOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        
				// compare
				
				if (MosaicUtils.compare(image, image_rs) == false)
				{
					fail("Error: Image " + rs + " does not match the result");
				}
			}
		}
		
		fail("Not yet implemented");
	}
}
