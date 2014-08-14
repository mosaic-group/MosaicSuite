package mosaic.core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;

import org.scijava.util.FileUtils;

import mosaic.core.GUI.ProgressBarWin;
import mosaic.core.cluster.ShellProcessOutput;

/**
 * 
 * Utility class to run shell command
 * 
 * All function members are static
 * 
 * @author Pietro Incardona
 *
 */

public class ShellCommand
{
	/**
	 * 
	 * Execute a command without printout
	 * 
	 * @param cmd Command
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	public static void exeCmdNoPrint(String cmd) throws IOException, InterruptedException
	{
		Process tProcess = Runtime.getRuntime().exec(cmd);
		
		tProcess.waitFor();
	}
	
	/**
	 * 
	 * Execute a command and get output as String
	 * 
	 * @param cmd
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	public static String exeCmdString(String cmd) throws IOException, InterruptedException
	{
		int lp = 0;
		Process tProcess = Runtime.getRuntime().exec(cmd);

		BufferedReader stdInput = new BufferedReader (new InputStreamReader(tProcess.getInputStream()));
		
		String out = new String();
		String s = null;
		while ((s = stdInput.readLine()) != null)
		{
			System.out.println(s);
			out += s;
		}
		
		tProcess.waitFor();
		return out;
	}
	
	/**
	 * 
	 * Execute a command and print
	 * 
	 * @param cmd
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	public static void exeCmd(String cmd) throws IOException, InterruptedException
	{
		int lp = 0;
		Process tProcess = Runtime.getRuntime().exec(cmd);

		BufferedReader stdInput = new BufferedReader (new InputStreamReader(tProcess.getInputStream()));
		
		String s = null;
		while ((s = stdInput.readLine()) != null)
		{
			System.out.println(s);
		}
		
		tProcess.waitFor();
	}
	
	
	/**
	 * 
	 * Execute a command, with a defined working dir and defined environment variables
	 * (System environment variables are appended)
	 * 
	 * @param cmd
	 * @param wdir
	 * @param env
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	public static void exeCmd(String cmd, File wdir, String env[]) throws IOException, InterruptedException
	{
		int lp = 0;
		Map<String,String> envi = System.getenv();
		if (env == null)
			env = new String[0];
		String[] envi_p_env = new String[envi.size()+env.length];
		
		int i = 0;
        for (String envName : envi.keySet()) 
        {
            envi_p_env[i] = new String(envName + "=" + envi.get(envName));
            i++;
        }
        
        for (String envName : env) 
        {
            envi_p_env[i] = new String(envName);
            i++;
        }
        
		
		Process tProcess = Runtime.getRuntime().exec(cmd,envi_p_env,wdir);

		BufferedReader stdInput = new BufferedReader (new InputStreamReader(tProcess.getInputStream()));
		
		String s = null;
		while ((s = stdInput.readLine()) != null)
		{
			System.out.println(s);
		}
		
		tProcess.waitFor();
	}
	
	/**
	 * 
	 * Execute a command, with a defined working dir and defined environment variables
	 * (System environment variables are appended)
	 * 
	 * @param cmd
	 * @param wdir
	 * @param env
	 * @param out
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	public static void exeCmd(String cmd, File wdir, String env[], ShellProcessOutput out) throws IOException, InterruptedException
	{
		int lp = 0;
		Map<String,String> envi = System.getenv();
		if (env == null)
			env = new String[0];
		String[] envi_p_env = new String[envi.size()+env.length];
		
		int i = 0;
        for (String envName : envi.keySet()) 
        {
            envi_p_env[i] = new String(envName + "=" + envi.get(envName));
            i++;
        }
        
        for (String envName : env) 
        {
            envi_p_env[i] = new String(envName);
            i++;
        }
        
		
		Process tProcess = Runtime.getRuntime().exec(cmd,envi_p_env,wdir);

		BufferedReader stdInput = new BufferedReader (new InputStreamReader(tProcess.getInputStream()));
		
		String s_full = new String();
		String s;
		while ((s = stdInput.readLine()) != null)
		{
			s_full += s + "\n";
			if (out != null)
				s_full = out.Process(s_full);
			System.out.println(s);
		}
		
		tProcess.waitFor();
	}
	
	/**
	 * 
	 * Execute the command on a defined working dir
	 * 
	 * @param cmd Command
	 * @param env Working dir
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	public static void exeCmd(String cmd, File env) throws IOException, InterruptedException
	{
		int lp = 0;
		Process tProcess = Runtime.getRuntime().exec(cmd,null,env);

		BufferedReader stdInput = new BufferedReader (new InputStreamReader(tProcess.getInputStream()));
		
		String s = null;
		while ((s = stdInput.readLine()) != null)
		{
			System.out.println(s);
		}
		
		tProcess.waitFor();
	}
	
	/**
	 * 
	 * Copy one directory/file recursively
	 * 
	 * @param from dir source
	 * @param to dir destination
	 * @param Optionally a progress bar window
	 * 
	 */
	
	public static void copy(File from, File to, ProgressBarWin wn)
	{
		File[] f = from.listFiles();
		
		if (f == null)
			return;
		
		int cnt = 0;
		
		for (File t : f)
		{
			if (wn != null)
			{
				if (t != null)
					wn.SetStatusMessage("Copy: " + t.getName());
				wn.SetProgress(cnt/f.length);
			}
			
			try {
				exeCmd("cp -R " + t.getAbsoluteFile() + " " + to.getAbsolutePath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cnt++;
		}
	}

	/**
	 * 
	 * Recursively take all the tree structure of a directory
	 * 
	 * @param set
	 * @param dir
	 */
	
	public static void populate(HashSet<File> set, File dir)
	{
		set.add(dir);
		
		if (dir.isDirectory())
		{
			for ( File t : dir.listFiles())
			{
				populate(set,t);
			}
		}
	}
	
	
	/**
	 * 
	 * Compare if two directories are the same as dir and file structure
	 * 
	 * @param a1 dir1
	 * @param a2 dir3
	 * @return true if they match, false otherwise
	 * 
	 */
	
	public static boolean compare(File a1, File a2) 
	{
		// 
		HashSet<File> seta1 = new HashSet<File>();
		
		populate(seta1, a1);
		
		HashSet<File> seta2 = new HashSet<File>();
		
		populate(seta2,a2);
		
		// Check if the two HashSet match
		
		return seta1.containsAll(seta2);
	}
	
	/**
	 * 
	 * Get a the path that contain the file
	 * 
	 * example:
	 * 
	 * cs = /some_path/image_A
	 *      /some_path/image_B
	 *      /some_other_path/image_C
	 * 
	 * img = image_C
	 * 
	 * the function return 2
	 * 
	 * @param cs List of paths
	 * @param fl name of the file
	 * 
	 * @return String of the JobID
	 */
	
	public static int getIDfromFileList(String path[], String fl)
	{
		for (int k = 0 ; k < path.length ; k++)
		{
			if (new File(path[k]).getName().contains(fl))
			{
				return k;
			}
		}
		
		return -1;
	}
}