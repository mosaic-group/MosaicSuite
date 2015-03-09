package mosaic.core.cluster;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;
import mosaic.plugins.MergeJobs;

import org.junit.Test;

public class Jtest 
{
	/**
	 * 
	 * It test the merging Jobs plugins functionality
	 * 
	 * @see MergeJobs
	 * 
	 */
	
	@Test
	public void mergetest() 
	{
		MergeJobs mj = new MergeJobs();
		
		String dir = MosaicUtils.getTestDir();
		String dir_test = dir + File.separator + "merge_jobs" + File.separator + "Test";
		String dir_sample = dir + File.separator + "merge_jobs" + File.separator + "Sample";
		
		// Remove test dir, create test dir and copy sample dir
		try {
			ShellCommand.exeCmd("rm -rf " + dir_test);
			ShellCommand.exeCmd("mkdir " + dir_test);
			ShellCommand.copy(new File(dir_sample),new File(dir_test),null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mj.setDir(dir_test);
		mj.setup("",null);
		
		// Check the result
		
		String dir_result = dir + File.separator + "merge_jobs" + File.separator + "Result";
		
		File result[] = new File(dir_result).listFiles();
		File test[] = new File(dir_test).listFiles();
		
		if (result.length != test.length || result.length == 0 || test.length == 0)
			fail("Error: Merging jobs");
		
		for (int i = 0 ; i < result.length ; i++)
		{
			if (ShellCommand.compare(new File(dir_result), new File(dir_test)))
			{
				fail("Error: Merging jobs differs");
			}
		}
	}
}
