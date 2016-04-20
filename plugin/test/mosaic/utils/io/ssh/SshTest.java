package mosaic.utils.io.ssh;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.jcraft.jsch.JSchException;

import mosaic.test.framework.CommonBase;
import mosaic.utils.io.ssh.SSH.SshOutput;
import mosaic.utils.io.ssh.SSH.SshOutput.Result;

public class SshTest extends CommonBase {

    /**
     * Naive test that tests whole functionality. Should be done smarter but currently it is good enough to keep going.
     */
    @Test
    public void mixTest() {
        String userName = System.getProperty("user.name");
        String remoteDir = "/tmp/sshTest_" + userName;
        
        try {
            // Connect
            SSH ssh = new SSH("cherryphi-1.mpi-cbg.de", userName, null, null);
            
            // Create some test files on remote system
            assertEquals(Result.SUCCESS, ssh.executeCommands("rm -rf " + remoteDir, "mkdir -p " + remoteDir, "cd " + remoteDir, "echo \"Hello world\" > test1.txt", "mkdir subDir", "echo \"Hello world!\" > subDir/test2.txt").cmdExecutionResult);
            
            // Download them
            SFTP sftp = new SFTP(ssh.getSession());
            assertTrue(sftp.downloadDir(new File(getTestTmpPath()), new File(remoteDir)));
            
            // Remove remote site
            assertEquals(Result.SUCCESS, ssh.executeCommands("rm -rf " + remoteDir + "/*").cmdExecutionResult);
            
            // Upload files back
            assertTrue(sftp.uploadDir(new File(getTestTmpPath()), new File(remoteDir)));
            
            // To verify upload just count files..
            SshOutput output = ssh.executeCommands("find " + remoteDir + " | wc | awk '{print $1}'");
            assertEquals(Result.SUCCESS, output.cmdExecutionResult);
            assertEquals("4\r\n", output.out);
            
            // Cleanup
            sftp.close();
            ssh.close();
        }
        catch (JSchException e) {
            e.printStackTrace();
            fail();
        }
    }
}
