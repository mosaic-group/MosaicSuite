package mosaic.core.ipc;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import mosaic.bregman.Analysis;
import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.Region3DRScript;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;
import mosaic.region_competition.output.RCOutput;

import org.junit.Test;

public class Jtest 
{
	@Test
	public void csvtest() 
	{
		CSVOutput.initCSV(-1);
		
		// Stitch files all together
		
		String out[] = {"*stitchA","*stitchB"};
		String TestDir = MosaicUtils.getTestDir() + File.separator + "csv";
		
		// Remove previous test files
		
		for (int i = 0 ; i < out.length ; i++)
		{
			try {
				ShellCommand.exeCmdNoPrint(TestDir + File.separator + "test_result" + out[i].replace("*", "_") + ".csv");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Put some metadata
		
		MetaInfo mt[] = new MetaInfo[3];
		mt[0] = new MetaInfo();
		mt[0].par = new String("test1");
		mt[0].value = new String("test1");
		mt[1] = new MetaInfo();
		mt[1].par = new String("test1");
		mt[1].value = new String("test1");
		mt[2] = new MetaInfo();
		mt[2].par = new String("test1");
		mt[2].value = new String("test1");
		
		// Force to Bregman output
		
		InterPluginCSV.StitchConvert(out,new File(TestDir) ,new File(TestDir + File.separator + "test_result"),mt,CSVOutput.occ,CSVOutput.occ.classFactory);
		
		// Check the result
		
		for (int i = 0 ; i < out.length ; i++)
		{
			InterPluginCSV<Region3DRScript> iCSVsrc = new InterPluginCSV<Region3DRScript>(Region3DRScript.class);
			iCSVsrc.setCSVPreferenceFromFile(TestDir + File.separator + "test_result" + out[i].replace("*", "_") + ".csv");
			Vector<Region3DRScript> outsrc = iCSVsrc.Read(TestDir + File.separator + "test_result" + out[i].replace("*", "_") + ".csv");

			InterPluginCSV<Region3DRScript> iCSVdst = new InterPluginCSV<Region3DRScript>(Region3DRScript.class);
			iCSVdst.setCSVPreferenceFromFile(TestDir + File.separator + "test_result.csv");
			Vector<Region3DRScript> outdst = iCSVdst.Read(TestDir + File.separator + "test_result.csv");

			if (outsrc.size() != outdst.size() || outsrc.size() == 0)
				fail("Error: CSV outout does not match");
		
			for (int k = 0 ; k < outsrc.size() ; k++)
			{
				if (outsrc.get(k).equals(outdst.get(k)))
				{
					fail("Error: CSV output does not match");
				}
			}
		}
	}
}