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
			
			String tmp_dir = IJ.getDirectory("temp");
			String temp_img = tmp_dir + tmp.img.substring(tmp.img.lastIndexOf(File.separator)+1);
			
			FileSaver fs = new FileSaver(MosaicUtils.openImg(tmp.img));
			fs.saveAsTiff(temp_img);
			
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
			
			// Create a Region Competition filter
			
			Region_Competition RC = new Region_Competition();
			
			ImagePlus img = MosaicUtils.openImg(temp_img);
			img.show();
			
			int rt = RC.setup(tmp.options, img);
			if (rt != tmp.setup_return)
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
				
		        	image_rs = (Img<?>) imgOpener.openImgs(IJ.getDirectory("temp") + File.separator + filename).get(0);
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
	}
}
