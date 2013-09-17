package mosaic.core.cluster;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Random;

import javax.swing.JOptionPane;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ConfigRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;


public class SecureShellSession
{
	String tdir = null;
	ClusterProfile cprof;
	JSch jsch;
	
	SecureShellSession(ClusterProfile cprof_)
	{
		cprof = cprof_;
	}
	
	/**
	 * 
	 * run a sequence of SSH commands
	 * @param pwd password to access the ssh session
	 * @param commands string to execute
	 */
	public boolean runCommand(String pwd, String commands)
	{
		jsch = new JSch();
		
		String host = cprof.getAccessAddress();
		String user = host.substring(0, host.indexOf('@'));
	    host = host.substring(host.indexOf('@')+1);
		
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
		
	    ConfigRepository configRepository;
		try 
		{
			configRepository = com.jcraft.jsch.OpenSSHConfig.parse(config);
	    	jsch.setConfigRepository(configRepository);
	    	Session session;
			session = jsch.getSession("Automated_cluster_command");
			session.setPassword(pwd);
			session.connect();
			
		    Channel channel=session.openChannel("shell");
		    channel.setOutputStream(System.out);
		    channel.setInputStream(new ByteArrayInputStream(commands.getBytes("UTF-8")));
		    
		    channel.connect(3*1000);
		} 
		catch (JSchException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	};
	
	/**
	 * 
	 * run a sequence of SSH commands
	 * @param pwd password to access the sftp session
	 * @param files to transfert
	 */
	public boolean transfert(String pwd, File files[])	
	{
		Random grn = new Random();
		
		JSch jsch=new JSch();

		String host = cprof.getAccessAddress();
	    String user=host.substring(0, host.indexOf('@'));
	    host=host.substring(host.indexOf('@')+1);

	    Session session;
		try 
		{
			session = jsch.getSession(user, host, 22);
			session.setPassword(pwd);
			session.connect();
		    Channel channel=session.openChannel("sftp");
		    channel.connect();
		    
		    ChannelSftp c=(ChannelSftp)channel;
		    
		    if (tdir == null)
		    	tdir = new String(cprof.getRunningDir()+"/" + Long.toString(new Date().getTime()));
		    
		    tdir = tdir + "/job_" + grn.nextInt() + "/";
		    c.mkdir(tdir);
		    c.cd(tdir);
		    
		    for (int i = 0 ; i < files.length ; i++)
		    {
		    	c.put(files[i].getAbsolutePath(), files[i].getName());
		    }
		}
	    catch (JSchException e) 
	    {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (SftpException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    java.io.InputStream in=System.in;
	    java.io.PrintStream out=System.out;

	    

		
		return true;
	};
}