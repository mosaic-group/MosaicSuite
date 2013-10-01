package mosaic.core.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import mosaic.core.cluster.LSFBatch;
import mosaic.core.cluster.JobStatus.jobS;
import mosaic.core.cluster.LSFBatch.LSFJob;

import mosaic.core.GUI.ProgressBarWin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ProgressBar;


public class ClusterSession
{
	ClusterProfile cp;
	
	ClusterSession(ClusterProfile cp_)
	{
		cp = cp_;
	}
	
	private String readFileAsString(String filePath) throws IOException {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(
                new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }
	
	/**
	 * Cleanup all the data you created
	 */
	
	private void CleanUp()
	{
		
	}
	
	/**
	 * 
	 * Run a selected command on the cluster, ensure that the cluster has a Fiji installed if not
	 * it provide an automate installation
	 * 
	 * @param img Image the frames are parallelized
	 * @param options options String to pass to the plugins
	 * @param ExtTime extimated running time (to select the queue on the cluster)
	 */
	
	public void runPluginsOnFrames(ImagePlus img, String options, double ExtTime)
	{
		// Save the image
		
		int nImages = img.getNFrames();
		String tmp_dir = IJ.getDirectory("temp");
		
		ProgressBarWin wp = new ProgressBarWin();
		wp.setVisible(true);
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
		
		SecureShellSession ss = new SecureShellSession(cp);
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
		
		if (ss.transfert(cp.getPassword(),fl,wp) == false)
		{
			CleanUp();
		}
		
		// Download a working version of Fiji
		// and copy the plugins
		
		if (ss.checkDirectory(cp.getRunningDir()+"Fiji.app") == false)
		{
			wp.SetStatusMessage("Installing Fiji on cluster... ");
			
			String CommandL[] = {"cd " + cp.getRunningDir(),
					"wget mosaic.mpi-cbg.de/Downloads/fiji-linux64.tar.gz",
					"tar -xf fiji-linux64.tar.gz",
					"cd Fiji.app",
					"cd plugins",
					"mkdir Mosaic_ToolSuite",
					"cd Mosaic_ToolSuite",
					"wget mosaic.mpi-cbg.de/Downloads/Mosaic_ToolSuite.jar"};
		
		
			ss.runCommands(cp.getPassword(), CommandL);
		}
		
		wp.SetStatusMessage("Interfacing with batch system...");
		
		// create the macro script

		String macro = new String("job_id = getArgument();\n"
				   + "if(job_id == \"\" )\n"
				   + "   exit(\"No job id\");\n"
				   + "\n"
				   + "run(\"Squassh\",\"config=" + ss.getTransfertDir() + "spb_settings.dat" + " output=" + ss.getTransfertDir() + "tmp_" + "\"" + " + job_id + " + "\"_seg.tif" + " filepath=" + ss.getTransfertDir() + "tmp_" + "\"" + "+ job_id" + " + \".tif\" );\n");
				   
		// Create the batch script if required and upload it
		
		String run_s = cp.getRunningDir() + ss.getSession_id() + "/" + ss.getSession_id() + ".ijm";
		String scr = bc.getScript(run_s,cp.getRunningDir() + ss.getSession_id() + "/" + ss.getSession_id(),ss.getSession_id(),nImages);
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
				fll[1] = new File(tmp_dir + "spb_settings.dat");
				fll[2] = new File(tmp_dir + ss.getSession_id() + ".ijm");
				ss.transfert(cp.getPassword(),fll,null);
			} 
			catch (FileNotFoundException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// run the command
		
		wp.SetStatusMessage("Running...");
		
		String [] commands = new String[2];
		commands[0] = new String("cd " + ss.getTransfertDir());
		commands[1] = bc.runCommand(ss.getTransfertDir());
		
		String out;
		ss.runCommands(cp.getPassword(), commands);
		
		// Wait that the command get processed
		// Yes of course horrible but it work
		
		int n_attempt = 0;
		while (bc.getJobID() == 0 && n_attempt < 100000000) 
		{try {Thread.sleep(100);}
		 catch (InterruptedException e) 
		 {e.printStackTrace();} 
		n_attempt++;}
		
		// Check if we failed to launch the job
		
		if (bc.getJobID() == 0)
		{IJ.error("Failed to run the Job on the cluster");CleanUp();return;}
		
		// get the status wait completition;
		
		JobStatus [] jb = bc.createJobStatus(nImages);
		ClusterStatusStack css = new ClusterStatusStack();
		ImageStack st = css.CreateStack(jb);
		ImagePlus ip = new ImagePlus("Cluster status",st);
		ip.show();
		bc.setJobStatus(jb);
		
		while (true)
		{
			commands = new String[1];
			commands[0] = bc.statusJobCommand();
			ss.runCommands(cp.getPassword(), commands);
			
			// Wait the command get Processed
			
			try {Thread.sleep(3000);}
			catch (InterruptedException e) 
			{e.printStackTrace();}
			
			// Sleep 3000 avoid the accumulation of commands
			// Process the competition
			
			if (JobStatus.allComplete(jb) == true)
				break;
		
			css.UpdateStack(st, jb);
			
		}
		
		// get the data
		
		// reconstruct the stack
		
		// show the data
	}
	
	public void runPluginsOnImages(ImagePlus img[], String options, double ExtTime)
	{
		// Save the image
		
		// transfert the images
		
		// Download a working version of Fiji
		// and copy the plugins
		
		// create the macro script
		
		// run the command
		
		// get the data
		
		// show the data
	}
}