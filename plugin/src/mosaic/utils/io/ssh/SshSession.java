package mosaic.utils.io.ssh;

import org.apache.log4j.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Base class for secure channels based stuff (SFTP, SSH..)
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SshSession {
    static private final Logger logger = Logger.getLogger(SshSession.class);
    
    private static final int SSH_PORT = 22;
    private static final int CONNECTION_TIMEOUT_MS = 60000;
    private static final int CONNECTION_KEEP_ALIVE_MS = 2000;
    private JSch jsch;
    Session session;
    
    SshSession(String aHostAddress, String aUserName, String aPassword, String aKeyPath) throws JSchException {
        jsch = new JSch();
        session = createSession(aHostAddress, aUserName, aPassword, aKeyPath);
    }
    
    SshSession(Session aSession) throws JSchException {
        if (aSession == null) throw new JSchException("Session cannot be null");
        if (!aSession.isConnected()) throw new JSchException("Provided session is down");
        session = aSession;
    }
    
    public Session getSession() {
        return session;
    }
    
    public void close() {
        session.disconnect();
    }
    
    private Session createSession(String aHostAddress, String aUserName, String aPassword, String aKeyPath) throws JSchException {
        logger.info("Connecting to [" + aHostAddress + "] with username [" + aUserName + "] pass/key provided (yes/no): " + (aPassword != null ? "yes/" : "no/") + (aKeyPath != null ? "yes" : "no"));  
        Session session;
        if (aPassword == null || aPassword.length() == 0) {
            logger.info("Trying to authenticate with auth key.");
            // Try to open the standard private key
            String keyPath = (aKeyPath == null) ? System.getProperty("user.home") + "/.ssh/id_rsa" : aKeyPath;
            jsch.addIdentity(keyPath);
            session = jsch.getSession(aUserName, aHostAddress, SSH_PORT);
        }
        else {
            logger.info("Trying to authenticate with provided password.");
            session = jsch.getSession(aUserName, aHostAddress, SSH_PORT);
            session.setPassword(aPassword);
        }
        session.setServerAliveInterval(CONNECTION_KEEP_ALIVE_MS);
        final java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(CONNECTION_TIMEOUT_MS);
        session.connect();

        return session;
    }
}
