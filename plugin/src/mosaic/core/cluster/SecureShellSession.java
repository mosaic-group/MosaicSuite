package mosaic.core.cluster;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import javax.swing.JOptionPane;

import mosaic.core.GUI.ProgressBarWin;
import mosaic.core.utils.DataCompression;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.File;

/**
 * 
 * This class is able to create SSH and SFTP Sessions for remote
 * access to HPC system or other services. Accept a profile that
 * specify the property of the cluster and use it to perform operation
 * on it like run remote command or transfert data
 * 
 * @author Pietro Incardona
 *
 */

public class SecureShellSession implements Runnable, ShellProcessOutput, SftpProgressMonitor
{
	ShellProcessOutput shp;
	PipedInputStream pinput_in;
	PipedOutputStream pinput_out;
	PipedInputStream poutput_in;
	PipedOutputStream poutput_out;
	String tdir = null;
	ClusterProfile cprof;
	JSch jsch;
	ChannelSftp cSFTP;
	Channel cSSH;
	Session session;
	
	String session_id;
	
	SecureShellSession(ClusterProfile cprof_)
	{
		pinput_in = new PipedInputStream();
		pinput_out = new PipedOutputStream();
		poutput_in = new PipedInputStream();
		poutput_out = new PipedOutputStream();
		try 
		{
			poutput_out.connect(poutput_in);
			pinput_in.connect(pinput_out);
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		cprof = cprof_;
	}
	
	/**
	 * Set the batch interface. Required in order to parse 
	 * the command output
	 * 
	 */
	
	public void setShellProcessOutput(ShellProcessOutput prc_)
	{
		shp = prc_;
	}
	
	/**
	 * 
	 * Get all the directory inside Directory
	 * 
	 * @param Directory 
	 * 
	 * @return All directories 
	 */
	
	public String[] getDirs(String Directory)
	{
		Vector<String> vs = new Vector<String>();
		try 
		{
			createSftpChannel();
			
			Vector<ChannelSftp.LsEntry> list = cSFTP.ls(Directory);
			
			for(ChannelSftp.LsEntry entry : list) 
			{
			    if (entry.getAttrs().isDir() == true)
			    {
			    	vs.add(entry.getFilename());
			    }
			}
		} 
		catch (SftpException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String out[] = new String[vs.size()];
		vs.toArray(out);
		return out;
	}
	
	/**
	 * 
	 * Check if exist a directory
	 * 
	 * @param Directory
	 * @return
	 */
	
	public boolean checkDirectory(String Directory)
	{
		try 
		{	
			createSftpChannel();
			
			cSFTP.cd(Directory);
			
		} 
		catch (JSchException e) 
		{
			return false;
		}
		catch (IOException e) 
		{
			return false;
		} 
		catch (SftpException e) 
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * 
	 * run a sequence of SSH commands
	 * @param pwd password to access the ssh session
	 * @param commands string to execute
	 */
	public void runCommands(String pwd, String [] commands)
	{
		OutputStream os = null;
		
	    String cmd_list = new String();
	    for (int i = 0 ; i < commands.length ; i++)
	    {
	    	cmd_list += commands[i] + "\n";
	    }
		try 
		{
			createSSHChannel();
		    
			pinput_out.write(cmd_list.getBytes());
		} 
		catch (JSchException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	};
	
	
	private boolean createSession() throws IOException, JSchException
	{		
		if (jsch == null)
			jsch = new JSch();
		
		if (session != null && session.isConnected() == true)
			return true;
		
	    ConfigRepository configRepository;

		String host = cprof.getAccessAddress();
	    String user=cprof.getUsername();
	    
	    String config =
    	        "Port 22\n"+
    	        "\n"+
    	        "Host Automated_cluster_command\n"+
    	        "  User "+user+"\n"+
    	        "  Hostname "+host+"\n"+
    	        "Host *\n"+
    	        "  ConnectTime 30000\n"+
    	        "  PreferredAuthentications keyboard-interactive,password,publickey\n"+
    	        "  #ForwardAgent yes\n"+ 
    	        "  #StrictHostKeyChecking no\n"+
    	        "  #IdentityFile ~/.ssh/id_rsa\n"+
    	        "  #UserKnownHostsFile ~/.ssh/known_hosts";
	    
		configRepository = com.jcraft.jsch.OpenSSHConfig.parse(config);
    	jsch.setConfigRepository(configRepository);
		
		session = jsch.getSession(user, host, 22);
		session.setPassword(cprof.getPassword());
		java.util.Properties config_ = new java.util.Properties(); 
		config_.put("StrictHostKeyChecking", "no");
		session.setConfig(config_);
		session.connect();
		
		return true;
	}
	
	private boolean createSSHChannel() throws JSchException, IOException
	{
		if (jsch == null)
			jsch = new JSch();

		if (cSSH != null && cSSH.isConnected() == true)
			return true;
		
		createSession();
		
		cSSH = (Channel) session.openChannel("shell");
		
	    cSSH.setInputStream(pinput_in);
	    cSSH.setOutputStream(poutput_out);
	    
	    new Thread(this).start();
	    
	    cSSH.connect();
	    
		return true;
	}
	
	private boolean createSftpChannel() throws JSchException, IOException
	{
		if (jsch == null)
			jsch = new JSch();

		if (cSFTP != null && cSFTP.isConnected() == true)
			return true;
		
		createSession();
		
		cSFTP = (ChannelSftp) session.openChannel("sftp");
		cSFTP.connect();
		    
		return true;
	}
	
	/**
	 * 
	 * run a sequence of SFTP commands to downloads files
	 * @param pwd password to access the sftp session
	 * @param files to transfer locally (Absolute path)
	 * @param dir Directory where to download
	 * @param wp (Optional) Progress bar window
	 * @return return true if the download complete successfully, NOTE: Do not use to check the existence of the files
	 *         true does not warrant that all files has been successfully downloaded, if does not exist remotely
	 * 
	 */
	public boolean download(String pwd, File files[], File dir,ProgressBarWin wp, ClusterProfile cp)	
	{
		boolean ret = true;
		
		try
		{
			createSftpChannel();
		
			// Create a Compressor
			
			DataCompression cmp = new DataCompression();
			if (cp != null)
			{
				
				cmp.selectCompressor();
				while (cp.hasCompressor(cmp.getCompressor()) == false)
				{
					cmp.nextCompressor();
				}
			}
			
		    for (int i = 0 ; i < files.length ; i++)
		    {
		    	try
		    	{		    	
		    		String absolutePath = files[i].getPath();
		    		String filePath = absolutePath.
		    			substring(0,absolutePath.lastIndexOf(File.separator));
		    	
		    		tdir = filePath + File.separator;
		    		cSFTP.cd(tdir);
		    
				    if (cmp.getCompressor() == null)
				    {
				    	if(wp != null)
				    		wp.SetProgress(100*i/files.length);
		    	
				    	cSFTP.get(files[i].getName(),dir.getAbsolutePath() + File.separator + files[i].getName());
				    }
				    else
				    {
				    	// Compress data on cluster
				    	
				    	if (wp != null)
				    		wp.SetStatusMessage("Compressing data on cluster");
				    	createSSHChannel();

				    	String s = new String("cd " + tdir + " ; ");
				    	File start_dir = findCommonPathAndDelete(files);
				    	s += cmp.compressCommand(start_dir, files, new File(start_dir + File.separator + files[0].getName() + "_compressed"));
				    	s += " ; echo \"JSCH REMOTE COMMAND\"; echo \"COMPRESSION END\"; \n";
						waitString = new String("JSCH REMOTE COMMAND\r\nCOMPRESSION END");
						wp_p = wp;
						ShellProcessOutput stmp = shp;
						setShellProcessOutput(this);
						doing = new String("Compressing on cluster");
						
						computed = false;
						pinput_out.write(s.getBytes());
						
						// Ugly but work;
						
						while (computed == false) 
						{try {Thread.sleep(100);}
						catch (InterruptedException e) 
						{e.printStackTrace(); ret = false;}}
						
						setShellProcessOutput(stmp);
				    	
						////////////////////////////
				    	
						if (wp != null)
						{
							wp.SetProgress(33);
				    		wp.SetStatusMessage("Downloading");
						}
						
						cSFTP.ls(tdir);
						total = 0;
				    	cSFTP.get(files[0].getName() + "_compressed",dir.getAbsolutePath() + File.separator + files[0].getName() + "_compressed",this);
						cSFTP.rm(files[0].getName() + "_compressed");
						
				    	if (wp != null)
				    	{
				    		wp.SetProgress(66);
				    		wp.SetStatusMessage("Decompressing Data");
				    	}
				    	cmp.unCompress(new File(dir.getAbsolutePath() + File.separator + files[0].getName() + "_compressed"),new File(dir.getAbsolutePath()));
				    	break;
				    }
		    	}
				catch (SftpException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					ret = false;
				}
		    }
		}
		catch (JSchException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			ret = false;
		}  
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			ret = false;
		}

		return ret;
	};
	
	/**
	 * 
	 * Find the common prefix in the array and delete it from File array
	 * 
	 * @param f Set of file
	 * @return the common prefix
	 */
	
	File findCommonPathAndDelete(File f[])
	{		
		File common = findCommonPath(f);
		
		int l = common.getAbsolutePath().length() + 1;
		
		for (int i = 0 ; i < f.length ; i++)
		{
			f[i] = new File(f[i].getAbsolutePath().substring(l, f[i].getAbsolutePath().length()));
		}
		
		return common;
	}
	
	/**
	 * 
	 * Find the common prefix in the array
	 * 
	 * @param f Set of file
	 * @return the common prefix
	 */
	
	File findCommonPath(File f[])
	{
		String common = f[0].getAbsolutePath();
		for (int i = 1 ; i < f.length ; i++)
		{
			int j;
			int minLength = Math.min(common.length(), f[i].getAbsolutePath().length());
			for (j = 0; j < minLength; j++) 
			{
				if (common.charAt(j) != f[i].getAbsolutePath().charAt(j)) 
				{
					break;
			    }
			}
			common = common.substring(0, j);
		}
		
		common = common.substring(0, common.lastIndexOf("/")+1);
		
		return new File(common);
	}
	
	/**
	 * 
	 * run a sequence of SFTP commands to upload files
	 * @param pwd password to access the sftp session
	 * @param files to transfer
	 * @param wp Progress window bar can be null
	 * @param cp Cluster profile (Optional) can be null
	 */
	public boolean upload(String pwd, File files[], ProgressBarWin wp, ClusterProfile cp)	
	{
		Random grn = new Random();
		
		try
		{
			createSftpChannel();
		
			// Create a Compressor
			
			DataCompression cmp = new DataCompression();
			if (cp != null)
			{
				
				cmp.selectCompressor();
				while (cp.hasCompressor(cmp.getCompressor()) == false)
				{
					cmp.nextCompressor();
				}
			}
			
		    if (tdir == null)
		    {
		    	tdir = new String(cprof.getRunningDir()+"/");
		    
		    	cSFTP.cd(cprof.getRunningDir()+"/");
		    	String ss = "session" + Long.toString(new Date().getTime());
		    	session_id = ss;
		    	tdir += ss + "/";
		    	cSFTP.mkdir(ss);
		    	cSFTP.cd(ss);
		    }
		    else
		    {
		    	cSFTP.cd(tdir);
		    }
		    
		    
		    if (cmp.getCompressor() == null)
		    {
		    	for (int i = 0 ; i < files.length ; i++)
		    	{
		    		if(wp != null)
		    			wp.SetProgress(100*i/files.length);
		    	
		    		cSFTP.put(files[i].getAbsolutePath(), files[i].getName());
		    	}
		    }
		    else
		    {
		    	wp.SetStatusMessage("Compressing data");
		    	File start_dir = findCommonPathAndDelete(files);
		    	cmp.Compress(start_dir,files, new File(start_dir + File.separator + files[0].getPath() + "_compressed"));
		    	
		    	wp.SetProgress(33);
		    	wp.SetStatusMessage("Uploading");
		    	cSFTP.put(start_dir + File.separator + files[0].getPath() + "_compressed", files[0].getName() + "_compressed");
		    	
		    	wp.SetProgress(66);
		    	wp.SetStatusMessage("Decompressing Data on cluster");
		    	
		    	createSSHChannel();

		    	String s = new String("cd " + tdir + " ; ");
		    	s += cmp.unCompressCommand(new File(tdir + files[0].getName() + "_compressed"));
		    	s += " ; echo \"JSCH REMOTE COMMAND\"; echo \"COMPRESSION END\"; \n";
				waitString = new String("JSCH REMOTE COMMAND\r\nCOMPRESSION END");
				wp_p = wp;
				ShellProcessOutput stmp = shp;
				setShellProcessOutput(this);
				doing = new String("Decompressing on cluster");
				
				pinput_out.write(s.getBytes());
				
				// Ugly but work;
				
				computed = false;
				while (computed == false) 
				{try {Thread.sleep(100);}
				catch (InterruptedException e) 
				{e.printStackTrace();}}
				
				cSFTP.rm(files[0].getName() + "_compressed");
				
				setShellProcessOutput(stmp);
		    }
		}
		catch (JSchException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} 
		catch (SftpException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	};
	
	/**
	 * 
	 * Return the session id as a String
	 * 
	 * @return Session id as string
	 */
	
	public String getSession_id()
	{
		return session_id;
	}
	
	/**
	 * 
	 * Directory on the cluster where file are transfert
	 * 
	 * @return String where the files are located
	 */
	
	public String getTransfertDir()
	{
		return tdir;
	}

	@Override
	public void run() 
	{
		byte out[] = null;
		
		out = new byte[1025];
		String sout = new String();
		
		while (true)
		{			
			try 
			{
				int len = poutput_in.available();
				if (len == 0)
				{
					poutput_in.read(out,0,1);
					if (out[0] != 0)
						sout += new String(out,0,1,"UTF-8");
					System.out.print(new String(out,0,1,"UTF-8"));
				}
				else
				{
					for (int i = 0 ; i < out.length ; i++)
					{out[i] = 0;}
					if (len >= out.length-1)
						len = out.length-1;
					poutput_in.read(out,0,len);
					String tmp = new String(out,0,len,"UTF-8");
					System.out.print(tmp);
					sout += tmp;
				}
			}
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			if (shp != null)
				sout = shp.Process(sout);
		}
	}

	boolean computed = false;
	String waitString;
	String doing;
	ProgressBarWin wp_p;
	
	@Override
	public String Process(String str) 
	{
		int lidx = str.lastIndexOf("\n")-1;
		int lidx2 = lidx;
		String print_out = new String();
		
		while (lidx >= 0)
		{
			if (str.charAt(lidx) == '\n')
				break;
			lidx--;
		}
		
		if (lidx >= 0 && lidx2 >= 0)
		print_out = str.substring(lidx, lidx2);
		
		if (wp_p != null)
			wp_p.SetStatusMessage(doing + " " + print_out);
		
		if (str.contains(waitString))
		{
			computed = true;
			return "";
		}
		return str;
	}

	long size_total = 0;
	long total = 0;
	
	@Override
	public boolean count(long arg0) 
	{
		total += arg0;
		
		if (wp_p != null)
		{
			wp_p.SetStatusMessage("Transfert... " + total);	
			wp_p.SetProgress((int)(total * 100 / size_total) );
		}
		
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void end() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(int arg0, String arg1, String arg2, long max) 
	{
		size_total = max;
		// TODO Auto-generated method stub
		
	}
}