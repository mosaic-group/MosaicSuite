package mosaic.utils.io.ssh;

import java.io.ByteArrayOutputStream;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import mosaic.utils.io.ssh.SSH.SshOutput.Result;

/**
 * SSH util based on Jsch library. 
 * Handles command(s) execution on remote system.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SSH extends SshSession {
    static private final Logger logger = Logger.getLogger(SSH.class);
    
    private static final int CMD_EXECUTION_TIMEOUT_MS = 10000;

    public SSH(String aHostAddress, String aUserName, String aPassword, String aKeyPath) throws JSchException {
        super(aHostAddress, aUserName, aPassword, aKeyPath);
    }
    
    public SSH(Session aSession) throws JSchException {
        super(aSession);
    }
    
    static public class SshOutput {
        enum Result {SUCCESS, ERROR, DISCONNECTED}
        
        Result cmdExecutionResult;
        String out;
        String err;
        int cmdErrorCode;
        
        SshOutput(Result aExecutionResult, String aOutput, String aError, int aCmdErrorCode) {
            cmdExecutionResult = aExecutionResult;
            out = aOutput;
            err = aError;
            cmdErrorCode = aCmdErrorCode;
        }
        
        @Override
        public String toString() {
            return "Result=" + cmdExecutionResult + 
                    " CmdErrCode=" + cmdErrorCode + 
                    "\nOut=[\n" + out + "]" +
                    "\nErr=[\n" + err + "]";
        }
    }

    public SshOutput executeCommands(String... aCommands) {
        StringBuilder sb = new StringBuilder();
        for (String cmd : aCommands) {
            sb.append(cmd);
            sb.append(";\n");
        }
        return executeTaggedCommand(sb.toString(), session);
    }

    /**
     * Executes command with removing possible additional output messages from ssh channel (like
     * one defined in .bashrc or so).
     * @param aCommandStr command(s) to be executed
     * @param aSshSession SSH session
     * @return
     */
    private SshOutput executeTaggedCommand(String aCommandStr, Session aSshSession) {
        logger.debug("Executing command: [" + aCommandStr + "]");
        
        // Add tags in begin end end of all commands
        final String startStr = "---START_CMD_OUTPUT_TAG---";
        final String stopStr =  "---STOP_CMD_OUTPUT_TAG---";
        
        String cmds = "echo -n \"" + startStr + "\"; " +
                      aCommandStr +
                      "echo -n \"" + stopStr + "\"; ";
        SshOutput output = executeCommand(cmds, aSshSession);
        
        // Leave only output from between tags 
        int from = output.out.indexOf(startStr);
        int to = output.out.lastIndexOf(stopStr);
        logger.trace("Indexes: " + from + " : " + to);
        from = (from < 0) ? 0 : from  + startStr.length();
        to =  (to < 0) ? output.out.length() : to - 1;
        logger.trace("Indexes: " + from + " : " + to);
        output.out = output.out.substring(from, to);
        
        logger.debug(output);
        
        return output;
    }

    private SshOutput executeCommand(String command, Session session) {
        Result success = Result.ERROR;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream errout = new ByteArrayOutputStream();
        int errCode = -1234;
        
        logger.trace("Executing command: [" + command + "]");
        
        try {
            final ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setOutputStream(out);
            channel.setExtOutputStream(out);
            channel.setErrStream(errout);
            channel.setPty(true);
            channel.connect();
            
            final Thread timeoutThread = new Thread() {
                @Override
                public void run() {
                    try { 
                        while (!channel.isClosed()) {
                            if (Thread.currentThread().isInterrupted()) break;
                            sleep(100);  
                        }
                    }
                    catch (final InterruptedException e) {
                        // Clear interrupted flag - not needed but clear in purpose
                        Thread.interrupted(); 
                    }   
                }
            };
            timeoutThread.start();
            timeoutThread.join(CMD_EXECUTION_TIMEOUT_MS);
            
            if (timeoutThread.isAlive()) {
                // Command not finished till time deadline - interrupt!
                logger.error("Command not executed entirely");
                timeoutThread.interrupt();
            }
            else {
                // We can end up here when command was finished or if connection was broken. 
                success = session.isConnected() ? Result.SUCCESS : Result.DISCONNECTED;
            }
            errCode = channel.getExitStatus();
            if (!channel.isClosed()) {
                channel.disconnect();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (JSchException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        final String removeColorInfo = "\u001B\\[[;\\d]*[ -/]*[@-~]"; // Alternative version: "\u001B\\[[;\\d]*m"
        
        SshOutput result =  new SshOutput(success, 
                             out.toString().replaceAll(removeColorInfo, ""),
                             errout.toString().replaceAll(removeColorInfo, ""),
                             errCode);
        logger.trace(result);
        
        return result; 
    }
}
