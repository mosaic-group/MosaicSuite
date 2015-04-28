package mosaic.core.cluster;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.process.StackStatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mosaic.bregman.Analysis;
import mosaic.core.GUI.ChooseGUI;
import mosaic.core.GUI.ProgressBarWin;
import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.utils.MM;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;

/**
 * 
 * Main class to handle a Session to an HPC cluster
 * 
 * usage
 * 
 *	ss = ClusterSession.processFiles(....);
 * 
 * @see processXXXXXX
 * 
 * or
 * 
 * ClusterSession ss = new ClusterSession()
 * ss.runPluginsOnFrames(.....)
 * 
 * @see runPluginsOnFrames
 * 
 * @author Pietro Incardona
 * 
 */


public class ClusterSession
{
	int nImages;
	ClusterProfile cp;
	SecureShellSession ss;
	int ns_pp = ns_pp_preferred;
	static int ns_pp_preferred = 1;
	String ia_s="filepath";
	
	ClusterSession(ClusterProfile cp_)
	{
		cp = cp_;
	}
	
	/**
	 * 
	 * Set preferred number of slots to allocate per process
	 * 
	 * @param ns_pp
	 */
	
	static public void setPreferredSlotPerProcess(int ns_pp)
	{
		ns_pp_preferred = ns_pp;
	}
	
	
	/**
	 * 
	 * Set slots to allocate per process
	 * 
	 * @param ns_pp
	 */
	
	public void setSlotPerProcess(int ns_pp)
	{
		this.ns_pp = ns_pp;
	}
	
	/**
	 * 
	 * Set input argument
	 * 
	 * @param ia_s string for the input argument
	 */
	
	public void setInputArgument(String ia_s)
	{
		this.ia_s = ia_s;
	}
	
//	private String readFileAsString(String filePath) throws IOException {
//        StringBuffer fileData = new StringBuffer();
//        BufferedReader reader = new BufferedReader(
//                new FileReader(filePath));
//        char[] buf = new char[1024];
//        int numRead=0;
//        while((numRead=reader.read(buf)) != -1){
//            String readData = String.valueOf(buf, 0, numRead);
//            fileData.append(readData);
//        }
//        reader.close();
//        return fileData.toString();
//    }
	
	/**
	 * Cleanup all the data you created
	 */
	
	private void CleanUp()
	{
		
	}
	
	/**
	 * 
	 * Split the images into frames and upload them
	 * 
	 * @param img Image
	 * @param dir where to save (relative path)
	 * @param wp optionally a progess bar
	 * @return true if upload false otherwise
	 */
	
	public boolean splitAndUpload(ImagePlus img, File dir , ProgressBarWin wp)
	{
		if (ss == null)
			ss = new SecureShellSession(cp);
		
		if (img == null)
		{nImages = 0; return true;}
		
		boolean dispose = false;
		if (wp == null)
		{
			wp = new ProgressBarWin();
			dispose = true;
		}
		
		wp.setFocusableWindowState(false);
		wp.setVisible(true);
		wp.setFocusableWindowState(false);
		
		nImages = img.getNFrames();
		String tmp_dir = IJ.getDirectory("temp");
	
		wp.SetStatusMessage("Preparing data...");
	
		ImageStack stk = img.getStack();
	
		int stack_size = stk.getSize() / nImages;
	
		for (int i = 0 ; i < nImages ; i++)
		{
			ImageStack tmp_stk = new ImageStack(img.getWidth(),img.getHeight());
			for (int j = 0 ; j < stack_size ; j++)
			{
				tmp_stk.addSlice("st"+j,stk.getProcessor(i*stack_size+j+1));
			}
		
			ImagePlus ip = new ImagePlus("tmp",tmp_stk);
			IJ.saveAs(ip,"Tiff", tmp_dir + "tmp_" + (i+1));
		
			wp.SetProgress(100*i/nImages);
		}
	
		// Create an SSH connection with the cluster
		// Get the batch system of the cluster and set the class
		//  to process the batch system output
	
		BatchInterface bc = cp.getBatchSystem();
	
		ss.setShellProcessOutput(bc);

		// transfert the images

		File [] fl = new File[nImages];
		for (int i = 0 ; i < nImages ; i++)
		{
			fl[i] = new File(tmp_dir + "tmp_" + (i+1) + ".tif");
		}
	
		wp.SetProgress(0);
		wp.SetStatusMessage("Uploading...");
	
		if (ss.upload(cp.getPassword(),fl,dir,wp,cp) == false)
		{
			CleanUp();
			return false;
		}
		if (dispose == true)
			wp.dispose();
		return true;
	}
	
	/**
	 * 
	 * Create a JobArray on Cluster
	 * 
	 * @param img Image to process
	 * @param options Plugins options
	 * @param ss Secure Shell session
	 * @return false if fail, true if successfully
	 * 
	 */
	
	private boolean createJobArrayFromImage(ImagePlus img,String command, String options, SecureShellSession ss, double Ext, ProgressBarWin wp)
	{
		if (img == null)
		{nImages = 0; return true;}
		
		if (splitAndUpload(img,null,wp) == false)
			return false;
	
		BatchInterface bc = cp.getBatchSystem();
		String tmp_dir = IJ.getDirectory("temp");
		
		// Download a working version of Fiji
		// and copy the plugins
	
		if (ss.checkDirectory(cp.getRunningDir()+"Fiji.app") == false 
			|| ss.checkFile(cp.getRunningDir()+"Fiji.app","ImageJ-linux64") == false
			|| ss.checkFile(cp.getRunningDir()+"Fiji.app" + File.separator + "plugins" + File.separator + "Mosaic_ToolSuite" + File.separator, "Mosaic_ToolSuite_for_cluster.jar") == false)
		{
			wp.SetStatusMessage("Installing Fiji on cluster... ");
		
			// Remove previously Fiji
			
			String [] commands = new String[1];
			commands[0] = new String("rm -rf Fiji.app");
		
			ss.runCommands(cp.getPassword(), commands);
			
			String CommandL[] = {"cd " + cp.getRunningDir(),
				"wget mosaic.mpi-cbg.de/Downloads/fiji-linux64.tar.gz",
				"tar -xf fiji-linux64.tar.gz",
				"cd Fiji.app",
				"cd plugins",
				"mkdir Mosaic_ToolSuite",
				"cd Mosaic_ToolSuite",
				"wget mosaic.mpi-cbg.de/Downloads/Mosaic_ToolSuite_for_cluster.jar"};
	
			ss.runCommands(cp.getPassword(), CommandL);
			
			// Wait to install Fiji
			

			do
			{
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
				// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println("Checking Fiji installation");
			}
			while (ss.checkDirectory(cp.getRunningDir()+"Fiji.app") == false 
			|| ss.checkFile(cp.getRunningDir()+"Fiji.app","ImageJ-linux64") == false
			|| ss.checkFile(cp.getRunningDir()+"Fiji.app" + File.separator + "plugins" + File.separator + "Mosaic_ToolSuite" + File.separator, "Mosaic_ToolSuite_for_cluster.jar") == false);
		}
	
		wp.SetStatusMessage("Interfacing with batch system...");
	
		// create the macro script

		String macro = new String("job_id = getArgument();\n"
			   + "if(job_id == \"\" )\n"
			   + "   exit(\"No job id\");\n"
			   + "\n"
			   + "run(\""+command+"\",\"config=" + ss.getTransfertDir() + "settings.dat" + " "+ia_s+"=" + ss.getTransfertDir() + "tmp_" + "\"" + "+ job_id" + " + \".tif " + options + " \" );\n");
			   
		// Create the batch script if required and upload it
	
		String run_s = cp.getRunningDir() + ss.getSession_id() + "/" + ss.getSession_id() + ".ijm";
		String scr = bc.getScript(run_s,ss.getSession_id(),Ext,nImages, ns_pp);
		if (scr != null)
		{
			PrintWriter out;
			try 
			{
				// Running script
			
				out = new PrintWriter(tmp_dir + ss.getSession_id());
				out.println(scr);
				out.close();
			
				// ImageJ plugins macro
			
				out = new PrintWriter(tmp_dir + ss.getSession_id() + ".ijm");
				out.println(macro);
				out.close();
				
				File fll[] = new File[3];
				fll[0] = new File(tmp_dir + ss.getSession_id());
				fll[1] = new File(tmp_dir + "settings.dat");
				fll[2] = new File(tmp_dir + ss.getSession_id() + ".ijm");
				ss.upload(cp.getPassword(),fll,null,null);
			} 
			catch (FileNotFoundException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		// run the command
	
		wp.SetProgress(0);
		wp.SetStatusMessage("Running...");

		String [] commands = new String[2];
		commands[0] = new String("cd " + ss.getTransfertDir());
		commands[1] = bc.runCommand(ss.getTransfertDir());
	
		bc.setJobID(0);
		ss.runCommands(cp.getPassword(), commands);
	
		// Wait that the command get processed
		// horrible but it work
	
		int n_attempt = 0;
		while (bc.getJobID() == 0 && n_attempt < 300) 
		{try {Thread.sleep(100);}
		catch (InterruptedException e) 
		{e.printStackTrace();} 
		n_attempt++;}
	
		// Check if we failed to launch the job
	
		if (bc.getJobID() == 0)
		{IJ.error("Failed to run the Job on the cluster");CleanUp();return false;}
		
		// create JobID file
		
		try 
		{
			PrintWriter out;
		
			// Create jobID file
		
			out = new PrintWriter(tmp_dir + "JobID");
			String tmp = new String(command);
			tmp.replace(" ", "_");
			out.println(new String(bc.getJobID() + " " + nImages + " " + img.getTitle() + " " + command));
			out.close();
		
			File fll[] = new File[1];
			fll[0] = new File(tmp_dir + "JobID");
			
			ss.upload(cp.getPassword(),fll,null,null);
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * 
	 * Get the job number from the directory
	 * 
	 * @param jb Job dir
	 * @return the string job number
	 */
	
	public static String getJobNumber(String jb)
	{
		return jb.substring(2,jb.indexOf(File.separator));
	}
	
	/**
	 * 
	 * Get the Jobs directory in the temporal or other folder
	 * 
	 * @param JobID if 0 return all directory otherwise return the directory associated to the
	 *        specified jobID
	 * @param directory where to search for Job directory (if null the temp directory is choosen)
	 * @return Get all the directory string
	 * 
	 */
	
	static public String[] getJobDirectories(final int JobID, final String directory)
	{	
		// List all job directory
		
		File file = null;
		String tmp_dir_ = null;
		
		if (directory != null)
		{
			if (directory.endsWith(File.separator))
				tmp_dir_ = directory;
			else
				tmp_dir_ = directory + File.separator;
			file = new File(directory);
		}
		else
		{
			tmp_dir_ = IJ.getDirectory("temp");
			file = new File(tmp_dir_);
		}
			
		final String tmp_dir = new File(tmp_dir_).getAbsolutePath();
		
		String[] directories = file.list(new FilenameFilter() 
		{
		  @Override
		  public boolean accept(File dir, String name) 
		  {
			  	File fp = new File(tmp_dir + File.separator + "Job[0-9]+");
				Pattern jobID = Pattern.compile(fp.getAbsolutePath().replace("\\", "\\\\"));
				
				File fpm = new File(dir + File.separator + name);
				Matcher matcher = jobID.matcher(fpm.getAbsolutePath());
			  
				File f = new File(dir, name);
				if (f.isDirectory() == true && matcher.find())
				{
					if (JobID != 0)
					{
						if (f.getAbsolutePath().equals(tmp_dir + "Job" + JobID) == true )
						{
							return true;
						}
						else
							return false;
					}
					else
						return true;
				}
				return false;
		  }
		});
		
		if (directories == null)
			return new String[0];
		
		for (int i = 0 ; i < directories.length ; i++)
		{
			directories[i] = tmp_dir + File.separator + directories[i];
		}
			
		return directories;
	}
	
	/**
	 * 
	 * Create a JobSelector Window
	 * 
	 * @param all the directory job
	 */
	
	public int CreateJobSelector(String directories[])
	{
		ChooseGUI cg = new ChooseGUI();
		String c = cg.choose("Job Selector", "Select a Job to visualize", directories);
		if(c== null)
			return 0;
		int l = c.length() -1;
		
		while ( Character.isDigit(c.charAt(l)) && l >= 0)
		{
			l--;
		}
		l++;
		
		return Integer.parseInt(c.substring(l, c.length()));
	}
	
	/**
	 * 
	 * Load and visualize the stack
	 * 
	 * @param output List of output patterns
	 * @param JobID job to visualize (0 for all)
	 * @param wp Progress bar window
	 * 
	 */
	
	void stackVisualize(String output[], int JobID, ProgressBarWin wp)
	{
		String directories[] = getJobDirectories(JobID,null);
		
		if (JobID == 0)
		{
			if ((JobID = CreateJobSelector(directories)) == 0)
				return;
		}
		
		GenericDialog gd = new GenericDialog("Job output selector:");
		
		for (int i = 0 ; i  < output.length ; i++)
		{
			gd.addCheckbox(output[i], false);
		}
		gd.showDialog();
		
		if(gd.wasCanceled())
			return;
		
		boolean cs[] = new boolean[output.length];
		
		for (int i = 0 ; i  < output.length ; i++)
		{
			cs[i] = gd.getNextBoolean();
		}
		
		// Visualize all jobs directory
		
		for (int j = 0 ; j < directories.length ; j++)
		{
			for (int i = 0; i < output.length ; i++)
			{
				if (cs[i] == true && (output[i].endsWith(".tiff") || output[i].endsWith(".tif") || output[i].endsWith(".zip")))
				{
					wp.SetStatusMessage("Visualizing " + output[i]);
					
					File [] fl = new File(directories[j] + File.separator + output[i].replace("*", "_")).listFiles();
					int nf = fl.length;
					Opener op = new Opener();
				
					if (fl.length != 0)
					{
						ImagePlus ip = op.openImage(fl[0].getAbsolutePath());
				
						if (ip == null)
							continue;
						
						int nc = ip.getNChannels();
						int ns = ip.getNSlices();
						
						ip.close();
						
						IJ.run("Image Sequence...", "open=" + directories[j] + File.separator + output[i].replace("*","_") + " starting=1 increment=1 scale=100 file=[] or=[] sort");
						IJ.run("Stack to Hyperstack...", "order=xyczt(default) channels=" + nc + " slices=" + ns + " frames=" + nf + " display=Composite");
					}
				}
			}
		}
	}

	/**
	 * 
	 * Reorganize the download cluster Jobs data directories
	 * 
	 * @param output List of output patterns
	 * @param JobID ID of the job to reorganize
	 * @param wp eventually a progress bar window
	 * 
	 */
	
	void reorganize(String output[], int JobID ,ProgressBarWin wp)
	{
		String directories[] = getJobDirectories(JobID,null);
		
		// reorganize
		
		for (int i = 0 ; i < directories.length ; i++)
		{
			String s[] = MosaicUtils.readAndSplit(directories[i] + File.separator + "JobID");
			int nf = Integer.parseInt(s[1]);
			String filename = s[2];
			
			int idp = filename.lastIndexOf(".");
			if (idp >= 0)
				filename = filename.substring(0, idp);
			
			MosaicUtils.reorganize(output, "tmp", filename, directories[i], nf);
				
		}
	}
	
	/**
	 * 
	 * Get data
	 * 
	 * @param output Output format
	 * @param ss SecureShellSession
	 * @param wp Progress bar window
	 * @param bc Batch interface
	 */
	
	void getData(String output[], SecureShellSession ss, ProgressBarWin wp, BatchInterface bc)
	{
		String tmp_dir = IJ.getDirectory("temp");
		File [] fldw = new File[bc.getNJobs() * output.length + 1];
	
		fldw[0] = new File(bc.getDir() + File.separator + "JobID");
		for (int i = 0 ; i < bc.getNJobs() ; i++)
		{
			for (int j = 0 ; j < output.length ; j++)
			{
				String tmp = new String(output[j]);
				fldw[i*output.length + j + 1] = new File(bc.getDir() + File.separator + tmp.replace("*","tmp_" + (i+1)) );
			}
		}
	
		try {
			ShellCommand.exeCmdNoPrint("mkdir " + tmp_dir + File.separator + "Job" + bc.getJobID());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		wp.SetStatusMessage("Downloading...");
		ss.download(cp.getPassword(), fldw, new File(tmp_dir + File.separator + "Job" + bc.getJobID()) , wp, cp);
	}
	
	/**
	 * 
	 * Run a selected command on the cluster, ensure that the cluster has a Fiji installed if not
	 * it provide an automate installation, it wait the jobs to complete
	 * 
	 * @param img Image the frames are parallelized
	 * @param command to run the plugin
	 * @param options options String to pass to the plugins
	 * @param output output that the plugins generate with "*" as wild card example: "dir1/dir*_out/*.tif"
	 *        on a file "tmp_1" will be expanded in "dir1/dirtmp1_1_out/tmp_1.tif"
	 * @param ExtTime estimated running time (to select the queue on the cluster)
	 * @return true if done, false if fail (or nothing to do)
	 * 
	 */
	
	public boolean runPluginsOnFrames(ImagePlus img, String command, String options, String output[], double ExtTime)
	{
		return runPluginsOnFrames(img,command,options,output,ExtTime,true);
	}
	
	/**
	 * 
	 * Get the base directory where files are transfer
	 * 
	 * @return base directory as String
	 */
	
	public String getClusterDirectory()
	{
		if (ss == null)
			return null;
		
		return ss.getTransfertDir();
	}
	
	/**
	 * 
	 * Upload a list of files creating a relative directory
	 * 
	 * @param dir directory
	 * @param fl Files to upload
	 */
	
	public void upload(File dir, File[] fl)
	{
		if (ss == null)
			ss = new SecureShellSession(cp);
		
		ProgressBarWin wp = new ProgressBarWin();
		ss.upload(cp.getPassword(), fl, dir, wp, null);
	}
	
	/**
	 * 
	 * Upload a list of files
	 * 
	 * @param fl Files to upload
	 * @param show progress bar
	 */
	
	public void upload(File[] fl)
	{
		if (ss == null)
			ss = new SecureShellSession(cp);
		
		ProgressBarWin wp = new ProgressBarWin();
		ss.upload(cp.getPassword(), fl, wp, null);
	}
	
	/**
	 * 
	 * Run a selected command on the cluster, ensure that the cluster has a Fiji installed if not
	 * it provide an automate installation
	 * 
	 * @param img Image the frames are parallelized
	 * @param command to run the plugin
	 * @param options options String to pass to the plugins
	 * @param output output that the plugins generate with "*" as wild card example: "dir1/dir*_out/*.tif"
	 *        on a file "tmp_1" will be expanded in "dir1/dirtmp1_1_out/tmp_1.tif"
	 * @param ExtTime exstimated running time (to select the queue on the cluster)
	 * @param sync wait or not job to complete
	 * @return true if done, false if fail (or nothing to do)
	 * 
	 */
	
	public boolean runPluginsOnFrames(ImagePlus img,String command, String options, String output[], double ExtTime, boolean sync)
	{
		if (ss == null)
			ss = new SecureShellSession(cp);
		ProgressBarWin wp = new ProgressBarWin();
		wp.setFocusableWindowState(false);
		wp.setVisible(true);
		wp.setFocusableWindowState(true);
		
		// Create job array
		
			if (createJobArrayFromImage(img,command,options,ss,ExtTime,wp) == false)
			{
				wp.SetStatusMessage("Failed to create job array");
				ss = null;
				wp.dispose();
				return false;
			}
		
			// if sync == true we do not wait return
		
			if (sync == false)
			{
				// Close the progress bar
			
				wp.dispose();
				ss = null;
				return true;
			}
		
			BatchInterface bc = cp.getBatchSystem();
		
			//
		
			wp.SetProgress(0);
			wp.SetStatusMessage("Getting all jobs ...");
		
			BatchInterface bcl[] = bc.getAllJobs(ss,command);
			if (bcl == null)
			{			
				bcl = new BatchInterface[0];
			}
			ClusterStatusStack css[] = new ClusterStatusStack[bcl.length];
			ImageStack st[] = new ImageStack[bcl.length];
			ImagePlus ip[] = new ImagePlus[bcl.length];
		
			// get the status wait competition;

			for (int j = 0 ; j < bcl.length ; j++)
			{
				bcl[j].createJobStatus();
				css[j] = new ClusterStatusStack();
				st[j] = css[j].CreateStack(bcl[j].getJobStatus());
				ip[j] = new ImagePlus("Cluster status " + bcl[j].getJobID(),st[j]);
				ip[j].show();
				bcl[j].setJobStatus(bcl[j].getJobStatus());
			}
		
			wp.SetProgress(0);
		
			/* Wait the various jobs complete */
			
			int n_bc = 0;
			while (n_bc < bcl.length)
			{
				double progress = 0.0;
				double total = 0.0;
				n_bc = 0;
				for (int j = 0 ; j < bcl.length ; j++)
				{
					if (bcl[j] == null)
					{n_bc++; continue;}
					String commands[] = new String[1];
					commands[0] = bcl[j].statusJobCommand();
					bcl[j].reset();
					ss.setShellProcessOutput(bcl[j]);
					ss.runCommands(cp.getPassword(), commands);
			
					// Wait the command get Processed

					bcl[j].waitParsing();
			
					if (JobStatus.allComplete(bcl[j].getJobsStatus()) == true)
					{
						css[j].UpdateStack(st[j], bcl[j].getJobStatus());
						ip[j].updateAndDraw();
					
						getData(output,ss,wp,bcl[j]);
						bcl[j].clean(ss);
					
						wp.SetProgress(0);
						wp.SetStatusMessage("Reorganize...");
						reorganize(output,bcl[j].getJobID(),wp);
					
//						wp.SetProgress(0);
//						wp.SetStatusMessage("Stack visualize...");
//						stackVisualize(output,bcl[j].getJobID(),wp);
					
						bcl[j] = null;
					
						wp.SetStatusMessage("Computing ...");
						int p = (int)(progress * 100.0 / total);
						wp.SetProgress(p);
					
						continue;
					}
					css[j].UpdateStack(st[j], bcl[j].getJobStatus());
					ip[j].updateAndDraw();
				
					progress += JobStatus.countComplete(bcl[j].getJobsStatus());
					total += bcl[j].getJobStatus().length;
				}
			
				wp.SetStatusMessage("Computing ...");
				int p = (int)(progress * 100.0 / total);
				wp.SetProgress(p);
			
				// wait 10 second to send and get again the status
			
				try 
				{
					Thread.sleep(3000);
				}
				catch (InterruptedException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			// Close cluster status stack
			
			for (int i = 0 ; i < ip.length ; i++)
				ip[i].close();
			
		// It never went online
		
//		if (bcl.length == 0)
//		{
			wp.SetProgress(0);
			wp.SetStatusMessage("Reorganize...");
			reorganize(output,0,wp);
		
			wp.SetProgress(0);
			wp.SetStatusMessage("Stack visualize...");
			stackVisualize(output,0,wp);
//		}
			
		wp.SetStatusMessage("End");
		return true;
	}
	
	/**
	 * 
	 * Get the Finished jobs
	 * 
	 * @param out output produced by the plugin
	 * @param command to run the plugin
	 * @param options to run the plugin
	 * @param optionally ClusterGUI
	 * @return
	 */
	
	static public ClusterSession getFinishedJob(String[] out, String command, ClusterGUI cg)
	{
		return processImage(null,command,null,out,cg,new Float(0.0),new Float(0.0),true);
	}
	
	/**
	 * 
	 * Get the Finished jobs
	 * 
	 * @param out output produced by the plugin
	 * @param command to run the plugin
	 * @return
	 */
	
	static public ClusterSession getFinishedJob(String[] out, String command)
	{
		return processImage(null,command,null,out,null,new Float(0.0),new Float(0.0),true);
	}
	
	
	/**
	 * 
	 * Process the image
	 * 
	 * @param aImp the image to process
	 * @param command to run the plugin
	 * @param options to run the plugins
	 * @param output produced by the plugin
	 * @param cg ClusterGUI
	 * @return the session cluster
	 */
	
	static public ClusterSession processImage(ImagePlus aImp,String command, String options, String[] out, ClusterGUI cg)
	{
		return processImage(aImp,command,options,out,cg,new Float(0.0),new Float(0.0),true);
	}	
	
	
	/**
	 * 
	 * Process the image
	 * 
	 * @param aImp the image to process
	 * @param command to run the plugin
	 * @param options to run the plugin
	 * @param output produced by the plugin
	 * @return the session cluster
	 */
	
	static public ClusterSession processImage(ImagePlus aImp,String command, String options, String[] out)
	{
		return processImage(aImp,command,options,out,null,new Float(0.0),new Float(0.0),true);
	}
	
	/**
	 * 
	 * Process the image, min and max value are used for re-normalization if
	 * max = 0.0 the max value and min value of the image are used
	 * 
	 * @param aImp the image to process
	 * @param command to run the plugin
	 * @param output produced by the plugin
	 * @param cg ClusterGUI
	 * @param min minimum value
	 * @param max maximum value
	 * @return the session cluster
	 */
	
	static public ClusterSession processImage(ImagePlus aImp, String command, String options, String[] out, ClusterGUI cg, Float max, Float min, boolean sync)
	{
		if (cg == null)
		{
			cg = new ClusterGUI();
		}
	
		ClusterSession ss = cg.getClusterSession();
		
		// Get all image processor statistics and calculate the maximum
		
		if (max == 0.0)
		{		
			if (aImp != null)
			{
				StackStatistics stack_stats = new StackStatistics(aImp);
				max = (float)stack_stats.max;
				min = (float)stack_stats.min;
		
				// get the min and the max
			}
		}
		
		// Run plugin on frames
		
		if (ss.runPluginsOnFrames(aImp, command, "min="+ min + " max="+max + " " + options, out, 180.0, sync) == false)
			return null;
		
		return ss;
	}
	
	/**
	 * 
	 * Process a list of files
	 * 
	 * @param list of files to process
	 * @param command to run the plugin
	 * @param out output produced by the plugin *_xxxxxx.tif or *_xxxxxx.csv where * is subsituted
	 *        with the image name
	 * @param options to run the plugins
	 * @return the cluster session
	 * 
	 */
	
	static public ClusterSession processFiles(File list[], String command, String options, String[] out)
	{
		return processFiles(list,command,options,out,null);
	}
	
	/**
	 * 
	 * Process a list of files
	 * 
	 * @param list of files to process
	 * @param command to run the plugin
	 * @param out output produced by the plugin *_xxxxxx.tif or *_xxxxxx.csv where * is subsituted
	 *        with the image name
	 * @param options 
	 * @param ClusterGUI optionally a ClusterGUI
	 * @return the cluster session
	 * 
	 */
	
	static public ClusterSession processFiles(File list[],String command, String options, String[] out, ClusterGUI cg)
	{
		if (cg == null)
			cg = new ClusterGUI();
		ClusterSession ss = cg.getClusterSession();
		
		MM mm = new MM();
		
		mm.min = new Float(Float.MAX_VALUE);
		mm.max = new Float(0.0);
		
		MosaicUtils.getFilesMaxMin(list,mm);
		
		for (File fl : list)
		{
			// File
			
			processFile(fl,command,options,out,cg,mm.max,mm.min);
		}
		
		ss.runPluginsOnFrames(null,command,options, Analysis.out, 180.0);
		return ss;
	}
	
	/**
	 * 
	 * Process a file
	 * 
	 * @param fl File to process
	 * @param command to run the plugin
	 * @param out output produced by the plugin *_xxxxxx.tif or *_xxxxxx.csv where * is subsituted
	 *        with the image name
	 * @param options to run the plugin
	 * @return the cluster session
	 * 
	 */
	static public ClusterSession processFile(File fl, String [] out, String command, String options)
	{
		return processFile(fl,command,options,out,null);
	}
	
	/**
	 * 
	 * Process a file
	 * 
	 * @param fl File to process
	 * @param command to run the plugin
	 * @param out output produced by the plugin *_xxxxxx.tif or *_xxxxxx.csv where * is subsituted
	 *        with the image name
	 * @param options to run the plugins
	 * @return the cluster session
	 * 
	 */
	static public ClusterSession processFile(File fl, String command, String options,String [] out)
	{
		return processFile(fl,command,options,out,null);
	}
	
	/**
	 * 
	 * Process a file
	 * 
	 * @param fl File to process
	 * @param command to run the plugin
	 * @param out output produced by the plugin *_xxxxxx.tif or *_xxxxxx.csv where * is subsituted
	 *        with the image name
	 * @param ClusterGUI optionally a ClusterGUI
	 * @param options to run the plugin
	 * @return the cluster session
	 * 
	 */
	static public ClusterSession processFile(File fl, String command, String options,String [] out, ClusterGUI cg)
	{
		if (cg == null)
			cg = new ClusterGUI();
		
		// open the image and process image
		
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(fl.getAbsolutePath());
		ClusterSession ss = processImage(imp,command,options,out,cg,new Float(0.0),new Float(0.0),true);
		
//		ClusterSession ss = cg.getClusterSession();
		
//		ss.runPluginsOnFrames(null,null, Analysis.out, 180.0, sync);
		return ss;
	}
	
	/**
	 * 
	 * Return the filename produced by the split and upload
	 * 
	 * @param num number
	 * @return the filename
	 */
	
	public String getSplitAndUploadFilename(int num)
	{
		return "tmp_" + (num+1) + ".tif";
	}
	
	/**
	 * 
	 * Process a file
	 * 
	 * @param fl File to process
	 * @param command to run the plugin
	 * @param options to run the plugin
	 * @param min external minimum intensity (used for re-normalization across images)
	 * @param max external maximum intensity (used for re-normalization across images)
	 * @param ClusterGUI optionally a cluster GUI
	 * 
	 */
	static private ClusterSession processFile(File fl,String command, String options, String [] out, ClusterGUI cg, Float max, Float min)
	{		
		// open the image and process image
		
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(fl.getAbsolutePath());
		ClusterSession ss = processImage(imp,command,options,out,cg,max,min,false);
		
		return ss;
	}
	
	/**
	 * 
	 * It post-process the jobs data performing the following operation:
	 * 
	 * It search on the temporal directory for Job directory and for each founded
	 * job directory create a JobXXXX directory where XXXXX is the JobID for each 
	 * Jobs directory copy all output file there for all csv filename
	 * supplied in outcsv perform a stitch operation (it try to stitch
	 *  all the CSV in one)
	 * 
	 * @param outcsv set of csv output data
	 * @param path where to save
	 * @param property if not null the stitch operation try to enumerate the file and set the property
	 *        in the stitched file according to this enumeration
	 * @param cls base class for internal conversion
	 * @return File where the data are saved
	 * 
	 */
	
	public static <T extends ICSVGeneral> File processJobsData(String[] outcsv , String path, Class<T> cls)
	{
		String dirS = null;
		
		// Save all JobID to the image folder
		// or ask for a directory
		
		String dir[] = ClusterSession.getJobDirectories(0,null);
		
		if (dir.length > 0)
		{

			
			if (path != null)
			{
				dirS = path;
			}
			else
			{
				DirectoryChooser dc = new DirectoryChooser("Choose directory where to save result");
				dirS = dc.getDirectory();
			}
			
			ProgressBarWin wp = new ProgressBarWin();
			
			for (int i = 0 ; i < dir.length ; i++)
			{
				wp.SetStatusMessage("Moving " + dir[i]);
				
				try 
				{					
					String[] tmp = dir[i].split(File.separator);
					
					File t = new File(dirS + File.separator + tmp[tmp.length-1]);
					
					ShellCommand.exeCmdNoPrint("cp -r " + dir[i] + " " + t);
					System.out.println("cp -r " + dir[i] + " " + t);
					
					// after copy remove the directory
					
					ShellCommand.exeCmdNoPrint("rm -rf " + dir[i]);
					System.out.println("rm -rf " + dir[i]);
					
					///////
				}
				catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			wp.dispose();
		}
		
		return new File(dirS);
	}
	
	/**
	 * 
	 * Merge more jobs into one
	 * 
	 * @param jobsrc source job
	 * @param jobdst[] destination jobs
	 * 
	 */
	
	public static void mergeJobs(File jobsrc, File jobdst[])
	{
		if (jobdst.length == 0)
			return;
		
		// Get a temporary directory create a dir
		
		String tmp = IJ.getDirectory("temp") + File.separator + "temp_merge" + File.separator;
		try {
			ShellCommand.exeCmd("mkdir " + tmp);
			ProgressBarWin pbw = new ProgressBarWin();
			ShellCommand.copy( jobsrc, new File(tmp), pbw);
			pbw.dispose();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] sp_src = MosaicUtils.readAndSplit(jobsrc.getAbsolutePath() + File.separator + "JobID");
		String sp[] = new String[sp_src.length];
		for (int i = 0 ; i < sp_src.length ; i++)
		{
			sp[i] = sp_src[i];
		}
		
		// For each directory in job2 check if exist a directory in job1
		
		for (int i = 0 ; i < jobdst.length ; i++)
		{
			File fl[] = jobdst[i].listFiles();
		
			for (File t : fl)
			{
				if (t.isDirectory() == false)
					continue;
			
				// dir
			
				File dir = new File(tmp + File.separator + t.getName());
			
				if (dir.exists() == true)
				{
					// exist the directory in job1
				
					ProgressBarWin pbw = new ProgressBarWin();
					ShellCommand.copy(t,dir,pbw);
					pbw.dispose();
				}
				else
				{
					// Create a directory and copy
				
					try {
						ShellCommand.exeCmd("mkdir " + dir.getAbsolutePath());
						ProgressBarWin pbw = new ProgressBarWin();
						ShellCommand.copy(t, dir,pbw);
						pbw.dispose();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			// create a new JobID
			
			String[] sp_dst = MosaicUtils.readAndSplit(jobdst[i].getAbsolutePath() + File.separator + "JobID");
			
			sp[0] = sp[0] + "#" + sp_dst[0];
			sp[1] = Integer.toString(Integer.parseInt(sp[1]) + Integer.parseInt(sp_dst[1]));
			sp[2] = sp[2] + "#" + sp_dst[2];
			if (!sp_src[3].equals(sp_dst[3]))
			{
				IJ.showMessage("Error : You cannot merge Jobs of two different plugins");
				return;
			}
			
		}
		
		
		PrintWriter out;
		
		// Create jobID file
	
		try {
			out = new PrintWriter(tmp + File.separator + "JobID");
			out.print(sp[0]);
			for (String s : sp)
				out.print(" " + s);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// remove the content from jobsrc
		
		try {
			File[] fl_ = jobsrc.listFiles();
			for (File f : fl_)
			{
				ShellCommand.exeCmd("rm -rf " + f.getAbsolutePath());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Copy the content
		
		ProgressBarWin pbw = new ProgressBarWin();
		ShellCommand.copy(new File(tmp),jobsrc,pbw);
		pbw.dispose();
		
		// remove the temporary directory
		
		try {
			ShellCommand.exeCmd("rm -rf " + new File(tmp).getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Remove other jobs
		
		for (int i = 0 ; i < jobdst.length ; i++)
		{
			try {
				ShellCommand.exeCmd("rm -rf " + jobdst[i].getAbsolutePath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * Merge two jobs together
	 * 
	 * @param jobsrc source job
	 * @param jobdst destination job
	 * 
	 */
	
	public static void mergeJobs(File jobsrc, File jobdst)
	{
		File[] fl = new File[1];
		mergeJobs(jobsrc,fl);
	}
	
/*	public void runPluginsOnImages(ImagePlus img[], String options, double ExtTime)
	{*/
		// Save the image
		
		// transfert the images
		
		// Download a working version of Fiji
		// and copy the plugins
		
		// create the macro script
		
		// run the command
		
		// get the data
		
		// show the data
/*	}*/
}