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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
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

public class SecureShellSession implements Runnable
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
	 * run a sequence of SSH commands
	 * @param pwd password to access the sftp session
	 * @param files to transfert
	 */
	public boolean transfert(String pwd, File files[], ProgressBarWin wp)	
	{
		Random grn = new Random();
		
		try
		{
			createSftpChannel();
		
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
		    
		    for (int i = 0 ; i < files.length ; i++)
		    {
		    	if(wp != null)
		    		wp.SetProgress(100*i/files.length);
		    	
		    	cSFTP.put(files[i].getAbsolutePath(), files[i].getName());
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

	    java.io.InputStream in=System.in;
	    java.io.PrintStream out=System.out;
		
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
		
		try {out = new byte[poutput_in.available()];} 
		catch (IOException e1) {e1.printStackTrace();}
		String sout = new String();
		
		while (true)
		{			
			try 
			{
				poutput_in.read(out);
			}
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			sout += new String(out);
			if (shp != null)
				sout = shp.Process(sout);
		}
	}
}