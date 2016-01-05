package mosaic.core.cluster;


import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Date;
import java.util.Vector;

import mosaic.core.GUI.ProgressBarWin;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;


/**
 * This class is able to create SSH and SFTP Sessions for remote
 * access to HPC system or other services. Accept a profile that
 * specify the property of the cluster and use it to perform operation
 * on it like run remote command or transfert data
 *
 * @author Pietro Incardona
 */

class SecureShellSession implements Runnable, ShellProcessOutput, SftpProgressMonitor {

    private ShellProcessOutput shp;
    private final PipedInputStream pinput_in;
    private final PipedOutputStream pinput_out;
    private final PipedInputStream poutput_in;
    private final PipedOutputStream poutput_out;
    private String tdir = null;
    private final ClusterProfile cprof;
    private JSch jsch;
    private ChannelSftp cSFTP;
    private Channel cSSH;
    private Session session;
    private String session_id;

    SecureShellSession(ClusterProfile cprof_) {
        pinput_in = new PipedInputStream();
        pinput_out = new PipedOutputStream();
        poutput_in = new PipedInputStream();
        poutput_out = new PipedOutputStream();
        try {
            poutput_out.connect(poutput_in);
            pinput_in.connect(pinput_out);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }

        cprof = cprof_;
    }

    /**
     * Close the connections
     *
     * @throws InterruptedException
     * @throws IOException
     */
    void close() throws InterruptedException, IOException {
        // Check that all the pipe are empty

        while (poutput_in.available() != 0 && pinput_in.available() != 0) {
            Thread.sleep(100);
        }
        pinput_in.close();
        pinput_out.close();
        poutput_in.close();
        
        poutput_out.close();
        cSFTP.disconnect();
        cSSH.disconnect();
        session.disconnect();
    }

    /**
     * Set the batch interface. Required in order to parse
     * the command output
     */
    void setShellProcessOutput(ShellProcessOutput prc_) {
        shp = prc_;
    }

    /**
     * Get all the directory inside Directory
     *
     * @param Directory
     * @return All directories, return null if there are problems to connect
     */
    String[] getDirs(String Directory) {
        final Vector<String> vs = new Vector<String>();
        try {
            if (createSftpChannel() == false) {
                return null;
            }

            @SuppressWarnings("unchecked")
            final
            Vector<ChannelSftp.LsEntry> list = cSFTP.ls(Directory);

            for (final ChannelSftp.LsEntry entry : list) {
                if (entry.getAttrs().isDir() == true) {
                    vs.add(entry.getFilename());
                }
            }
        }
        catch (final SftpException e) {
            e.printStackTrace();
        }
        catch (final JSchException e) {
            e.printStackTrace();
        }

        final String out[] = new String[vs.size()];
        vs.toArray(out);
        return out;
    }

    /**
     * Check if exist a directory
     *
     * @param Directory
     * @return
     */

    boolean checkDirectory(String Directory) {
        try {
            if (createSftpChannel() == false) {
                return false;
            }

            cSFTP.cd(Directory);

        }
        catch (final JSchException e) {
            return false;
        }
        catch (final SftpException e) {
            return false;
        }

        return true;
    }

    /**
     * Check if exist a file
     *
     * @param Directory
     * @param file_name to check
     * @return
     */

    boolean checkFile(String Directory, String file_name) {
        try {
            if (createSftpChannel() == false) {
                return false;
            }

            @SuppressWarnings("unchecked")
            final
            Vector<LsEntry> fl = cSFTP.ls(Directory);

            for (final LsEntry f : fl) {
                if (f.getFilename().contains(file_name)) {
                    return true;
                }
            }
            return false;
        }
        catch (final JSchException e) {
            return false;
        }
        catch (final SftpException e) {
            return false;
        }
    }

    /**
     * run a sequence of SSH commands
     *
     * @param pwd password to access the ssh session
     * @param commands string to execute
     * @return false, if where is a problem with the connection
     *         true, does not mean that the command succeffuly run
     */
    boolean runCommands(String[] commands) {
        String cmd_list = new String();
        for (int i = 0; i < commands.length; i++) {
            cmd_list += commands[i] + "\n";
        }
        try {
            if (createSSHChannel() == false) {
                return false;
            }

            pinput_out.write(cmd_list.getBytes());
        }
        catch (final JSchException e) {
            e.printStackTrace();
        }
        catch (final IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean createSession() throws JSchException {
        if (jsch == null) {
            jsch = new JSch();
        }

        if (session != null && session.isConnected() == true) {
            return true;
        }

        // Open the private key
        final String host = cprof.getAccessAddress();
        final String user = cprof.getUsername();

        if (cprof.getPassword() == null || cprof.getPassword().length() == 0) {
            File p_key = null;

            // Try to open the standard private key
            if (IJ.isLinux()) {
                p_key = new File("/home/" + cprof.getUsername() + "/.ssh/id_rsa");
            }
            else if (IJ.isMacOSX()) {
                p_key = new File("/Users/" + cprof.getUsername() + "/.ssh/id_rsa");
            }
            else {
                throw new RuntimeException("private key location unknown!");
            }

            jsch.addIdentity(p_key.getAbsolutePath());

            session = jsch.getSession(user, host, 22);
        }
        else {
            session = jsch.getSession(user, host, 22);
            session.setPassword(cprof.getPassword());
        }

        final java.util.Properties config_ = new java.util.Properties();
        config_.put("StrictHostKeyChecking", "no");
        session.setConfig(config_);

        try {
            session.connect();
        }
        catch (final JSchException e) {
            IJ.error("Connection failed", e.getMessage());
            return false;
        }

        return true;
    }

    private boolean createSSHChannel() throws JSchException {
        if (jsch == null) {
            jsch = new JSch();
        }

        if (cSSH != null && cSSH.isConnected() == true) {
            return true;
        }

        if (createSession() == false) {
            return false;
        }

        cSSH = session.openChannel("shell");

        cSSH.setInputStream(pinput_in);
        cSSH.setOutputStream(poutput_out);

        new Thread(this).start();

        cSSH.connect();

        return true;
    }

    private boolean createSftpChannel() throws JSchException {
        if (jsch == null) {
            jsch = new JSch();
        }

        if (cSFTP != null && cSFTP.isConnected() == true) {
            return true;
        }

        if (createSession() == false) {
            return false;
        }

        cSFTP = (ChannelSftp) session.openChannel("sftp");
        cSFTP.connect();

        return true;
    }

    /**
     * run a sequence of SFTP commands to downloads files
     *
     * @param files to transfer locally (Absolute path)
     * @param dir Directory where to download
     * @param wp (Optional) Progress bar window
     * @return return true if the download complete successfully, NOTE: Do not
     *         use to check the existence of the files
     *         true does not warrant that all files has been successfully
     *         downloaded, if does not exist remotely
     *         you can receive a true
     */
    boolean download(File files[], File dir, ProgressBarWin wp, ClusterProfile cp) {
        boolean ret = true;

        try {
            if (createSftpChannel() == false) {
                return false;
            }

            // Create a Compressor

            final DataCompression cmp = new DataCompression();
            if (cp != null) {

                cmp.selectCompressor();
                while (cp.hasCompressor(cmp.getCompressor()) == false) {
                    cmp.nextCompressor();
                }
            }

            for (int i = 0; i < files.length; i++) {
                try {
                    final String absolutePath = files[i].getPath();
                    final String filePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));

                    tdir = filePath + File.separator;
                    cSFTP.cd(tdir);

                    if (cmp.getCompressor() == null) {
                        if (wp != null) {
                            wp.SetProgress(100 * i / files.length);
                        }

                        cSFTP.get(files[i].getName(), dir.getAbsolutePath() + File.separator + files[i].getName());
                    }
                    else {
                        // Compress data on cluster

                        if (wp != null) {
                            wp.SetStatusMessage("Compressing data on cluster");
                        }
                        if (createSSHChannel() == false) {
                            return false;
                        }

                        String s = new String("cd " + tdir + " ; ");
                        final File start_dir = findCommonPathAndDelete(files);
                        s += cmp.compressCommand(files, new File(start_dir + File.separator + files[0].getName() + "_compressed"));
                        s += " ; echo \"JSCH REMOTE COMMAND\"; echo \"COMPRESSION END\"; \n";
                        waitString = new String("JSCH REMOTE COMMAND\r\nCOMPRESSION END");
                        wp_p = wp;
                        final ShellProcessOutput stmp = shp;
                        setShellProcessOutput(this);
                        doing = new String("Compressing on cluster");

                        computed = false;
                        pinput_out.write(s.getBytes());

                        // Ugly but work;

                        while (computed == false) {
                            try {
                                Thread.sleep(100);
                            }
                            catch (final InterruptedException e) {
                                e.printStackTrace();
                                ret = false;
                            }
                        }

                        setShellProcessOutput(stmp);

                        ///////////////////////////

                        if (wp != null) {
                            wp.SetProgress(33);
                            wp.SetStatusMessage("Downloading");
                        }

                        cSFTP.ls(tdir);
                        total = 0;
                        cSFTP.get(files[0].getName() + "_compressed", dir.getAbsolutePath() + File.separator + files[0].getName() + "_compressed", this);
                        cSFTP.rm(files[0].getName() + "_compressed");

                        if (wp != null) {
                            wp.SetProgress(66);
                            wp.SetStatusMessage("Decompressing Data");
                        }
                        cmp.unCompress(new File(dir.getAbsolutePath() + File.separator + files[0].getName() + "_compressed"), new File(dir.getAbsolutePath()));
                        break;
                    }
                }
                catch (final SftpException e) {
                    e.printStackTrace();
                    ret = false;
                }
            }
        }
        catch (final JSchException e) {
            e.printStackTrace();
            ret = false;
        }
        catch (final IOException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    /**
     * Find the common prefix in the array and delete it from File array
     *
     * @param f Set of file
     * @return the common prefix
     */

    private File findCommonPathAndDelete(File f[]) {
        final File common = findCommonPath(f);

        final int l = common.getAbsolutePath().length() + 1;

        for (int i = 0; i < f.length; i++) {
            f[i] = new File(f[i].getAbsolutePath().substring(l, f[i].getAbsolutePath().length()));
        }

        return common;
    }

    /**
     * Find the common prefix in the array
     *
     * @param f Set of file
     * @return the common prefix
     */

    private File findCommonPath(File f[]) {
        String common = f[0].getAbsolutePath();
        for (int i = 1; i < f.length; i++) {
            int j;
            final int minLength = Math.min(common.length(), f[i].getAbsolutePath().length());
            for (j = 0; j < minLength; j++) {
                if (common.charAt(j) != f[i].getAbsolutePath().charAt(j)) {
                    break;
                }
            }
            common = common.substring(0, j);
        }

        common = common.substring(0, common.lastIndexOf("/") + 1);

        return new File(common);
    }

    /**
     * run a sequence of SFTP commands to upload files
     *
     * @param pwd password to access the sftp session
     * @param files to transfer
     * @param wp Progress window bar can be null
     * @param cp Cluster profile (Optional) can be null
     * @return true if all file are uploaded, false trasnfert fail
     */
    boolean upload(File files[], ProgressBarWin wp, ClusterProfile cp) {
        return upload(files, null, wp, cp);
    }

    /**
     * run a sequence of SFTP commands to upload files
     *
     * @param files to transfer
     * @param dir create the relative dir where to store the files
     * @param wp Progress window bar can be null
     * @param cp Cluster profile (Optional) can be null
     * @return true if all file are uploaded, false trasnfert fail
     */
    boolean upload(File files[], File dir, ProgressBarWin wp, ClusterProfile cp) {
        try {
            if (createSftpChannel() == false) {
                return false;
            }

            // Create a Compressor

            final DataCompression cmp = new DataCompression();
            if (cp != null) {

                cmp.selectCompressor();
                while (cp.hasCompressor(cmp.getCompressor()) == false) {
                    cmp.nextCompressor();
                }
            }

            if (tdir == null) {
                tdir = new String(cprof.getRunningDir() + "/");

                cSFTP.cd(cprof.getRunningDir() + "/");
                final String ss = "session" + Long.toString(new Date().getTime());
                session_id = ss;
                tdir += ss + "/";
                cSFTP.mkdir(ss);
                cSFTP.cd(ss);
            }
            else {
                cSFTP.cd(tdir);
            }

            // we have a dir

            if (dir != null) {
                cSFTP.mkdir(dir.getPath());
                cSFTP.cd(dir.getPath());
            }

            if (cmp.getCompressor() == null) {
                /* No compression */

                for (int i = 0; i < files.length; i++) {
                    if (wp != null) {
                        wp.SetProgress(100 * i / files.length);
                    }

                    cSFTP.put(files[i].getAbsolutePath(), files[i].getName());
                }
            }
            else {
                /* Compression */

                wp.SetStatusMessage("Compressing data");
                final File start_dir = findCommonPathAndDelete(files);
                wp_p = wp;
                waitString = new String("COMPRESSION END");
                doing = new String("Compressing ");
                cmp.Compress(start_dir, files, new File(start_dir + File.separator + files[0].getPath() + "_compressed"), this);

                wp.SetProgress(33);
                wp.SetStatusMessage("Uploading");
                cSFTP.put(start_dir + File.separator + files[0].getPath() + "_compressed", files[0].getName() + "_compressed", this);

                wp.SetProgress(66);
                wp.SetStatusMessage("Decompressing Data on cluster");

                if (createSSHChannel() == false) {
                    return false;
                }

                // Getting the string to uncompress + appending the string
                // to print out when the task has been accomplished

                String s = new String();
                if (dir == null) {
                    s += "cd " + tdir + " ; ";
                    s += cmp.unCompressCommand(new File(tdir + files[0].getName() + "_compressed"));
                }
                else {
                    s += "cd " + tdir + File.separator + dir.getPath() + File.separator + " ; ";
                    s += cmp.unCompressCommand(new File(tdir + File.separator + dir.getName() + File.separator + files[0].getName() + "_compressed"));
                }
                s += " ; echo \"JSCH REMOTE COMMAND\"; echo \"COMPRESSION END\"; \n";
                waitString = new String("JSCH REMOTE COMMAND\r\nCOMPRESSION END");
                wp_p = wp;
                final ShellProcessOutput stmp = shp;
                setShellProcessOutput(this);
                doing = new String("Decompressing on cluster");

                computed = false;
                pinput_out.write(s.getBytes());

                // Ugly but work;

                while (computed == false) {
                    try {
                        Thread.sleep(100);
                    }
                    catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                cSFTP.rm(files[0].getName() + "_compressed");

                setShellProcessOutput(stmp);
            }
        }
        catch (final JSchException e) {
            e.printStackTrace();
            return false;
        }
        catch (final SftpException e) {
            e.printStackTrace();
            return false;
        }
        catch (final IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Return the session id as a String
     *
     * @return Session id as string
     */

    String getSession_id() {
        return session_id;
    }

    /**
     * Directory on the cluster where file are transfert
     *
     * @return String where the files are located
     */

    String getTransfertDir() {
        return tdir;
    }

    @Override
    public void run() {
        byte out[] = null;

        out = new byte[1025];
        String sout = new String();

        System.out.println("");
        System.out.println("--------------------------- SecureShell output: -----------------------------");
        while (true) {
            try {
                int len = poutput_in.available();
                if (len == 0) {
                    poutput_in.read(out, 0, 1);
                    if (out[0] != 0) {
                        sout += new String(out, 0, 1, "UTF-8");
                    }
                    System.out.print(new String(out, 0, 1, "UTF-8"));
                }
                else {
                    for (int i = 0; i < out.length; i++) {
                        out[i] = 0;
                    }
                    if (len >= out.length - 1) {
                        len = out.length - 1;
                    }
                    poutput_in.read(out, 0, len);
                    final String tmp = new String(out, 0, len, "UTF-8");
                    System.out.print(tmp);
                    sout += tmp;
                }
            }
            catch (final IOException e) {
                e.printStackTrace();
                System.out.println("--------------------------- SecureShell output [END] ------------------------");
                return;
            }
            if (shp != null) {
                sout = shp.Process(sout);
            }
        }
    }

    private boolean computed = false;
    private String waitString;
    private String doing;
    private ProgressBarWin wp_p;

    /* Parse the compression stage output */

    @Override
    public String Process(String str) {
        int lidx = str.lastIndexOf("\n") - 1;
        final int lidx2 = lidx;
        String print_out = new String();

        /* search for a complete last line */

        while (lidx >= 0) {
            if (str.charAt(lidx) == '\n') {
                break;
            }
            lidx--;
        }

        /* get the line */

        if (lidx >= 0 && lidx2 >= 0) {
            print_out = str.substring(lidx + 1, lidx2);
        }

        /* print the line */

        if (wp_p != null) {
            wp_p.SetStatusMessage(doing + " " + print_out);
        }

        /* End line mark the end of computation */

        if (str.contains(waitString)) {
            computed = true;
            return "";
        }
        return str;
    }

    private long size_total = 0;
    private long total = 0;

    @Override
    public boolean count(long arg0) {
        total += arg0;

        if (wp_p != null) {
            wp_p.SetStatusMessage("Transfert... " + total);
            wp_p.SetProgress((int) (total * 100 / size_total));
        }

        return true;
    }

    @Override
    public void end() {

    }

    @Override
    public void init(int arg0, String arg1, String arg2, long max) {
        size_total = max;

    }
}
