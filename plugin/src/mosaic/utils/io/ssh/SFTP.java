package mosaic.utils.io.ssh;

import java.io.File;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import mosaic.utils.SysOps;

/**
 * SFTP util based on Jsch library. Handles uploading/downloading single and multiple files and directories.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SFTP extends SshSession {
    static private final Logger logger = Logger.getLogger(SFTP.class);
    
    ChannelSftp sftp;
    
    public SFTP(String aHostAddress, String aUserName, String aPassword, String aKeyPath) throws JSchException {
        super(aHostAddress, aUserName, aPassword, aKeyPath);
        sftp = createSftpChannel();
    }
    
    public SFTP(Session aSession) throws JSchException {
        super(aSession);
        sftp = createSftpChannel();
    }

    /**
     * Uploads aFiles to remote aRemoteDirectory
     * @param aRemoteDirectory path on remote system where files are to be uploaded
     * @param aFiles files to be uploaded
     * @return true on success and false otherwise
     */
    public boolean upload(String aRemoteDirectory, String... aFiles) {
        File[] files = new File[aFiles.length];
        for (int i = 0; i < aFiles.length; ++i) files[i] = new File(aFiles[i]);
        File dir = new File(aRemoteDirectory);
        
        return upload(dir, files);
    }
    
    /**
     * Uploads aFiles to remote aDestDirectory
     * @param aRemoteDirectory path on remote system where files are to be uploaded
     * @param aFiles files to be uploaded
     * @return true on success and false otherwise
     */
    public boolean upload(File aRemoteDirectory, File... aFiles) {
        for (File file : aFiles) {
            String destFileAbsolutePath = aRemoteDirectory.getPath() + File.separator + file.getName();
            String uploadString = "[" + file.getAbsolutePath() + "] file to [" + session.getHost() + ":" + destFileAbsolutePath + "]";
            try {
                logger.debug("Uploding " + uploadString);
                sftp.put(file.getAbsolutePath(), destFileAbsolutePath);
            }
            catch (final SftpException e) {
                logger.error("Could not upload " + uploadString);
                logger.error("Reason: " + e.toString());
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }
    
    /**
     * Downloads aFiles from remote to aLocalDirectory
     * @param aLocalDirectory path on local system where files are to be downloaded
     * @param aFiles absolute path to files to be downloaded
     * @return true on success and false otherwise
     */
    public boolean download(String aLocalDirectory, String... aFiles) {
        File[] files = new File[aFiles.length];
        for (int i = 0; i < aFiles.length; ++i) files[i] = new File(aFiles[i]);
        File dir = new File(aLocalDirectory);
        
        return download(dir, files);
    }
    
    /**
     * Downloads aFiles from remote to aLocalDirectory
     * @param aLocalDirectory path on local system where files are to be downloaded
     * @param aFiles absolute path to files to be downloaded
     * @return true on success and false otherwise
     */
    public boolean download(File aLocalDirectory, File... aFiles) {
        for (File file : aFiles) {
            String destFileAbsolutePath = aLocalDirectory.getPath() + File.separator + file.getName();
            String downloadString = "[" + session.getHost() + ":" + file.getAbsolutePath() + "] file to [" +  destFileAbsolutePath + "]";
            try {
                logger.debug("Downloading " + downloadString);
                sftp.get(file.getAbsolutePath(), destFileAbsolutePath);
            }
            catch (final SftpException e) {
                logger.error("Could not download " + downloadString);
                logger.error("Reason: " + e.toString());
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }
    
    /**
     * Downloads all files (including directories) from aRemoteDirectory to aLocalDirectory.
     * Notice: directory aRemoteDirectory is not crated locally, only all files inside this directory
     * are downloaded and structure of this files and directories is preserved.
     * @param aLocalDirectory - directory where files should be downloaded
     * @param aRemoteDirectory - remote directory from where files should be downloaded
     * @return true on success
     */
    public boolean downloadDir(File aLocalDirectory, File aRemoteDirectory) {
        logger.info("Downloading directory [" + aRemoteDirectory + "] to [" + aLocalDirectory + "]");
        try {
            SftpATTRS remoteDirStat = getFileStat(aRemoteDirectory.getAbsolutePath());
            if (remoteDirStat == null || !remoteDirStat.isDir()) {
                logger.error("aRemoteDirectory is not a directory type! [" + aRemoteDirectory + "]");
                return false;
            }
            
            @SuppressWarnings("unchecked")
            Vector<LsEntry> ls = sftp.ls(aRemoteDirectory.getAbsolutePath());
            
            for (LsEntry entry : ls) {
                SftpATTRS attrs = entry.getAttrs();
                String name = entry.getFilename();
                
                String remoteAbsoluteFileName = aRemoteDirectory + File.separator + name;
                
                if (attrs.isDir()) {
                    if (!name.equals("..") && !name.equals(".")) {
                        String deepDirectory = aLocalDirectory + File.separator + name;
                        File newDir = new File(deepDirectory);
                        if (!newDir.exists()) {
                            logger.debug("Making local dir [" + newDir.getAbsolutePath() + "]");
                            SysOps.createDir(newDir);
                        }
                        if (!downloadDir(newDir, new File(remoteAbsoluteFileName))) {
                            return false;
                        }
                    }
                }
                else if (attrs.isReg()) {
                    if (!download(aLocalDirectory.getAbsolutePath(), remoteAbsoluteFileName)) {
                        return false;
                    }
                }
                else {
                    logger.debug("skipping: " + remoteAbsoluteFileName + " to " + aLocalDirectory);
                }
            }
        }
        catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    /**
     * Uploads all files (including directories) from aLocalDirectory to aRemoteDirectory.
     * Notice: directory aLocalDirectory is not created remotely, only all files inside this directory
     * are uploaded and structure of this files and directories is preserved.
     * @param aLocalDirectory - directory from where files should be uploaded
     * @param aRemoteDirectory - remote directory where files should be uploaded
     * @return true on success
     */
    public boolean uploadDir(File aLocalDirectory, File aRemoteDirectory) {
        logger.info("Uploading directory [" + aLocalDirectory + "] to [" + aRemoteDirectory + "]");
        try {
            if (!aLocalDirectory.isDirectory()) {
                logger.error("aLocalDirectory is not a directory type! [" + aLocalDirectory + "]");
                return false;
            }
            
            File[] ls = aLocalDirectory.listFiles();
            
            for (File entry : ls) {
                String name = entry.getName();
                
                String localAbsoluteFileName = aLocalDirectory + File.separator + name;
                
                if (entry.isDirectory()) {
                    if (!name.equals("..") && !name.equals(".")) {
                        String deepDirectory = aRemoteDirectory + File.separator + name;
                        if (getFileStat(deepDirectory) == null) {
                            logger.debug("Making remote dir [" + deepDirectory + "]");
                            sftp.mkdir(deepDirectory);
                        }
                        if (!uploadDir(new File(localAbsoluteFileName), new File(deepDirectory))) {
                            return false;
                        }
                    }
                }
                else if (entry.isFile()) {
                    if (!upload(aRemoteDirectory.getAbsolutePath(), localAbsoluteFileName)) {
                        return false;
                    }
                }
                else {
                    logger.debug("skipping: " + localAbsoluteFileName + " to " + aRemoteDirectory);
                }
            }
        }
        catch (SftpException e) {
            e.printStackTrace();
            return false;
        }
        
        return true;
    }

    private SftpATTRS getFileStat(String aFullPathName) {
        try {
            return sftp.stat(aFullPathName);
        }
        catch (SftpException e) {
            // It is OK - file or dir just does not exist
            return null;
        }
    }
    
    private ChannelSftp createSftpChannel() throws JSchException {
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.setExtOutputStream(System.err);
        sftp.connect();

        return sftp;
    }
}
