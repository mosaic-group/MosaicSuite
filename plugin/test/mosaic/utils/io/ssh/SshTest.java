package mosaic.utils.io.ssh;

import java.io.File;

import org.junit.Test;

import com.jcraft.jsch.JSchException;

import mosaic.test.framework.CommonBase;

public class SshTest extends CommonBase {
 
    @Test
    public void test() {
        try {
            SSH ssh = new SSH("cherryphi-1.mpi-cbg.de", "gonciarz", null, null);
            ssh.executeCommands("date");
//            ssh.executeCommands("while [ 1 ]; do date; sleep 1; done");
            ssh.executeCommands("pwd", "whoami", "cd bin", "pwd");
            SFTP sftp = new SFTP(ssh.getSession());
            sftp.upload("/tmp/xyz/", "/tmp/1/ala.txt", "/tmp/1/ula.txt", "/tmp/1/stuff.txt");
            sftp.upload(new File("/tmp/xyz/"), new File("/tmp/1/ala.txt"), new File("/tmp/1/ula.txt"), new File("/tmp/1/stuff.txt"));
            sftp.close();
            ssh.close();
        }
        catch (JSchException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testDwnl() {
        try {
            SFTP sftp = new SFTP("cherryphi-1.mpi-cbg.de", "gonciarz", null, null);
            sftp.downloadDir(new File("/tmp/1/"), new File("/tmp/xyz/"));
            sftp.close();
        }
        catch (JSchException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testUpl() {
        try {
            SFTP sftp = new SFTP("cherryphi-1.mpi-cbg.de", "gonciarz", "pwsudz1,", null);
            sftp.uploadDir(new File("/tmp/1"), new File("/tmp/xyz/"));
            sftp.close();
        }
        catch (JSchException e) {
            e.printStackTrace();
        }
    }
}
