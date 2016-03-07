package mosaic.core.cluster;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.process.StackStatistics;
import mosaic.core.GUI.ProgressBarWin;
import mosaic.core.utils.ChooseGUI;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;
import mosaic.utils.SysOps;


/**
 * Main class to handle a Session to an HPC cluster
 * usage
 * ss = ClusterSession.processFiles(....);
 *
 * @see processXXXXXX
 *      or
 *      ClusterSession ss = new ClusterSession()
 *      ss.runPluginsOnFrames(.....)
 * @see runPluginsOnFrames
 * @author Pietro Incardona
 */
public class ClusterSession {
    private static final Logger logger = Logger.getLogger(ClusterSession.class);
    
    public static final String DefaultInputParameterName = "input";
    public static final String DefaultSettingsFileName = SysOps.getTmpPath() + "settings.dat";
    
    private int nImages;
    private final ClusterProfile cp;
    private SecureShellSession ss;
    private int ns_pp = ns_pp_preferred;
    private static int ns_pp_preferred = 1;
    private String iInputParameterName = DefaultInputParameterName;

    ClusterSession(ClusterProfile cp_) {
        cp = cp_;
    }

    /**
     * Set preferred number of slots to allocate per process
     *
     * @param ns_pp
     */
    static public void setPreferredSlotPerProcess(int ns_pp) {
        ns_pp_preferred = ns_pp;
    }

    /**
     * Set slots to allocate per process
     *
     * @param ns_pp
     */
    public void setSlotPerProcess(int ns_pp) {
        this.ns_pp = ns_pp;
    }

    /**
     * Set input argument
     * @param aInputParamName string for the input argument
     */
    public void setInputParameterName(String aInputParamName) {
        iInputParameterName = aInputParamName;
    }
    public String getInputParameterName() {
        return iInputParameterName;
    }
    
    /**
     * Cleanup all the data you created
     */
    private void CleanUp() {

    }

    /**
     * Split the images into frames and upload them
     *
     * @param img Image
     * @param dir where to save (relative path)
     * @param wp optionally a progess bar
     * @return true if upload false otherwise
     */
    public boolean splitAndUpload(ImagePlus img, File dir, ProgressBarWin wp) {
        if (ss == null) {
            ss = new SecureShellSession(cp);
        }

        if (img == null) {
            nImages = 0;
            return true;
        }

        boolean dispose = false;
        if (wp == null) {
            wp = new ProgressBarWin();
            dispose = true;
        }

//        wp.setFocusableWindowState(false);
//        wp.setVisible(true);
//        wp.setFocusableWindowState(false);

        nImages = img.getNFrames();
        final String tmp_dir = IJ.getDirectory("temp");

        wp.SetStatusMessage("Preparing data...");

        final ImageStack stk = img.getStack();

        final int stack_size = stk.getSize() / nImages;

        for (int i = 0; i < nImages; i++) {
            final ImageStack tmp_stk = new ImageStack(img.getWidth(), img.getHeight());
            for (int j = 0; j < stack_size; j++) {
                tmp_stk.addSlice("st" + j, stk.getProcessor(i * stack_size + j + 1));
            }

            final ImagePlus ip = new ImagePlus("tmp", tmp_stk);
            IJ.saveAs(ip, "Tiff", tmp_dir + "tmp_" + (i + 1));

            wp.SetProgress(100 * i / nImages);
        }

        // Create an SSH connection with the cluster
        // Get the batch system of the cluster and set the class
        // to process the batch system output

        final BatchInterface bc = cp.getBatchSystem();

        ss.setShellProcessOutput(bc);

        // transfert the images
        final File[] fl = new File[nImages];
        for (int i = 0; i < nImages; i++) {
            fl[i] = new File(tmp_dir + "tmp_" + (i + 1) + ".tif");
        }

        wp.SetProgress(0);
        wp.SetStatusMessage("Uploading...");

        if (ss.upload(fl, dir, wp, cp) == false) {
            CleanUp();
            return false;
        }
        if (dispose == true) {
            wp.dispose();
        }
        return true;
    }

    /**
     * Create a JobArray on Cluster
     *
     * @param img Image to process
     * @param options Plugins options
     * @param ss Secure Shell session
     * @param Ext estimated running time for the job
     * @return false if fail, true if successful
     */
    private boolean createJobArrayFromImage(ImagePlus img, String command, String options, SecureShellSession ss, double Ext, ProgressBarWin wp) {
        if (img == null) {
            nImages = 0;
            return true;
        }

        if (splitAndUpload(img, null, wp) == false) {
            return false;
        }

        final BatchInterface bc = cp.getBatchSystem();
        final String tmp_dir = SysOps.getTmpPath();

        // Download a working version of Fiji
        // and copy the plugins
        boolean hasAllThingsInstalled = isAllSoftwareInstalled(ss, cp);
        
        if (!hasAllThingsInstalled) {
            wp.SetStatusMessage("Installing Fiji on cluster... ");

            ss.runCommands(new String[] {"rm -rf Fiji.app"});

            ss.runCommands(new String[] { "cd " + cp.getRunningDir(), 
                                          "wget mosaic.mpi-cbg.de/Downloads/fiji-linux64.tar.gz", 
                                          "tar -xf fiji-linux64.tar.gz", 
                                          "cd Fiji.app", 
                                          "cd plugins",
                                          "mkdir Mosaic_ToolSuite", 
                                          "cd Mosaic_ToolSuite", 
                                          "wget mosaic.mpi-cbg.de/Downloads/Mosaic_ToolSuite_for_cluster.jar" });
            // Wait to install Fiji
            do {
                try {
                    System.out.println("Expected directory: [" + cp.getRunningDir() + "Fiji.app]");
                    Thread.sleep(10000);
                }
                catch (final InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("Checking Fiji installation");
            } while (!isAllSoftwareInstalled(ss, cp));
        }

        wp.SetStatusMessage("Interfacing with batch system...");

        // create the macro script

        /* ------------------------------ Example output ------------------------
           job_id = getArgument();
           if (job_id == "" )
              exit("No job id");
              
           run("Squassh","config=/home/gonciarz/scratch//session1456411696096/settings.dat filepath=/home/gonciarz/scratch//session1456411696096/tmp_"+ job_id + ".tif min=456.0 max=2940.0  " );
         -------------------------------- */
        final String macro = new String("job_id = getArgument();\n" + 
                                        "if (job_id == \"\" )\n" + 
                                        "   exit(\"No job id\");\n" + 
                                        "\n" + 
                                        "run(\"" + command + "\",\"config=" + ss.getTransfertDir() + "settings.dat" + " " + iInputParameterName + "=" + ss.getTransfertDir() + "tmp_" + "\"" + "+ job_id" + " + \".tif " + options + " \" );\n");

        // Create the batch script if required and upload it

        final String run_s = cp.getRunningDir() + ss.getSession_id() + "/" + ss.getSession_id() + ".ijm";
        final String scr = bc.getScript(run_s, ss.getSession_id(), Ext, nImages, ns_pp);
        if (scr != null) {
            PrintWriter out;
            try {
                // Running script

                out = new PrintWriter(tmp_dir + ss.getSession_id());
                out.println(scr);
                out.close();

                // ImageJ plugins macro

                out = new PrintWriter(tmp_dir + ss.getSession_id() + ".ijm");
                out.println(macro);
                out.close();

                final File fll[] = new File[3];
                fll[0] = new File(tmp_dir + ss.getSession_id());
                fll[1] = new File(DefaultSettingsFileName);
                fll[2] = new File(tmp_dir + ss.getSession_id() + ".ijm");
                ss.upload(fll, null, null);
            }
            catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // run the command
        wp.SetProgress(0);
        wp.SetStatusMessage("Running...");

        final String[] commands = new String[2];
        commands[0] = new String("cd " + ss.getTransfertDir());
        commands[1] = bc.runCommand(ss.getTransfertDir());

        bc.setJobID(0);
        ss.runCommands(commands);

        // Wait that the command get processed
        // horrible but it work
        int n_attempt = 0;
        while (bc.getJobID() == 0 && n_attempt < 300) {
            try {
                Thread.sleep(100);
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
            n_attempt++;
        }

        // Check if we failed to launch the job

        if (bc.getJobID() == 0) {
            IJ.error("Failed to run the Job on the cluster");
            CleanUp();
            return false;
        }

        // create JobID file
        try {
            PrintWriter out;

            // Create jobID file
            out = new PrintWriter(tmp_dir + "JobID");
            final String tmp = new String(command);
            tmp.replace(" ", "_");
            out.println(new String(bc.getJobID() + " " + nImages + " " + img.getTitle() + " " + command));
            out.close();

            final File fll[] = new File[1];
            fll[0] = new File(tmp_dir + "JobID");

            ss.upload(fll, null, null);
        }
        catch (final FileNotFoundException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean isAllSoftwareInstalled(SecureShellSession aSession, ClusterProfile aProfile) {
        // Check Fiji 
        String fijiDir = aProfile.getRunningDir() + "Fiji.app";
        boolean hasFiji = aSession.checkDirectory(fijiDir);
        boolean hasLinuxExe = aSession.checkFile(fijiDir, "ImageJ-linux64");
        
        // Check Mosaic plugin (it can have one of two names).
        String mosaicPluginDir = fijiDir + File.separator + "plugins" + File.separator + "Mosaic_ToolSuite" + File.separator;
        boolean hasPlugin = aSession.checkFile(mosaicPluginDir, "Mosaic_ToolSuite.jar");
        boolean hasPluginForCluster = aSession.checkFile(mosaicPluginDir, "Mosaic_ToolSuite_for_cluster.jar");

        return hasFiji && hasLinuxExe && (hasPluginForCluster || hasPlugin);
    }

    /**
     * Get the Jobs directory in the temporal or other folder
     *
     * @param JobID if 0 return all directory otherwise return the directory associated to the specified jobID
     * @param directory where to search for Job directory (if null the temp directory is choosen)
     * @return Get all the directory string
     */
    static public String[] getJobDirectories(final int JobID, final String directory) {
        // List all job directory
        File file = null;
        String tmp_dir_ = null;

        if (directory != null) {
            if (directory.endsWith(File.separator)) {
                tmp_dir_ = directory;
            }
            else {
                tmp_dir_ = directory + File.separator;
            }
            file = new File(directory);
        }
        else {
            tmp_dir_ = IJ.getDirectory("temp");
            file = new File(tmp_dir_);
        }

        final String tmp_dir = new File(tmp_dir_).getAbsolutePath();

        final String[] directories = file.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                final File fp = new File(tmp_dir + File.separator + "Job[0-9]+");
                final Pattern jobID = Pattern.compile(fp.getAbsolutePath().replace("\\", "\\\\"));

                final File fpm = new File(dir + File.separator + name);
                final Matcher matcher = jobID.matcher(fpm.getAbsolutePath());

                final File f = new File(dir, name);
                if (f.isDirectory() == true && matcher.find()) {
                    if (JobID != 0) {
                        if (f.getAbsolutePath().equals(tmp_dir + "Job" + JobID) == true) {
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                    else {
                        return true;
                    }
                }
                return false;
            }
        });

        if (directories == null) {
            return new String[0];
        }

        for (int i = 0; i < directories.length; i++) {
            directories[i] = tmp_dir + File.separator + directories[i];
        }

        return directories;
    }

    /**
     * Create a JobSelector Window
     *
     * @param all the directory job
     */
    private int CreateJobSelector(String directories[]) {
        final ChooseGUI cg = new ChooseGUI();
        final String c = cg.chooseString("Job Selector", "Select a Job to visualize", directories);
        if (c == null) {
            return 0;
        }
        int l = c.length() - 1;

        while (Character.isDigit(c.charAt(l)) && l >= 0) {
            l--;
        }
        l++;

        return Integer.parseInt(c.substring(l, c.length()));
    }

    /**
     * Load and visualize the stack
     *
     * @param output List of output patterns
     * @param JobID job to visualize (0 for all)
     * @param wp Progress bar window
     */
    private void stackVisualize(String output[], int JobID, ProgressBarWin wp) {
        final String directories[] = getJobDirectories(JobID, null);

        if (JobID == 0) {
            if ((JobID = CreateJobSelector(directories)) == 0) {
                return;
            }
        }

        final GenericDialog gd = new GenericDialog("Job output selector:");

        for (int i = 0; i < output.length; i++) {
            gd.addCheckbox(output[i], false);
        }
        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        final boolean cs[] = new boolean[output.length];

        for (int i = 0; i < output.length; i++) {
            cs[i] = gd.getNextBoolean();
        }

        // Visualize all jobs directory
        for (int j = 0; j < directories.length; j++) {
            for (int i = 0; i < output.length; i++) {
                if (cs[i] == true && (output[i].endsWith(".tiff") || output[i].endsWith(".tif") || output[i].endsWith(".zip"))) {
                    wp.SetStatusMessage("Visualizing " + output[i]);
                    final String dirName = directories[j] + File.separator + output[i].replace("*", "_");
                    logger.debug("Listing files in: [" + dirName + "]");
                    final File[] fl = new File(dirName).listFiles();
                    if (fl == null) {
                        logger.error("Null file array");
                        continue;
                    }
                    final int nf = fl.length;
                    final Opener op = new Opener();

                    if (fl.length != 0) {
                        final ImagePlus ip = op.openImage(fl[0].getAbsolutePath());

                        if (ip == null) {
                            continue;
                        }

                        final int nc = ip.getNChannels();
                        final int ns = ip.getNSlices();

                        ip.close();

                        IJ.run("Image Sequence...", "open=" + directories[j] + File.separator + output[i].replace("*", "_") + " starting=1 increment=1 scale=100 file=[] or=[] sort");
                        IJ.run("Stack to Hyperstack...", "order=xyczt(default) channels=" + nc + " slices=" + ns + " frames=" + nf + " display=Composite");
                    }
                }
            }
        }
    }

    /**
     * Reorganize the download cluster Jobs data directories
     *
     * @param output List of output patterns
     * @param JobID ID of the job to reorganize
     */
    private void reorganize(String output[], int JobID) {
        final String directories[] = getJobDirectories(JobID, null);

        // reorganize

        for (int i = 0; i < directories.length; i++) {
            final String s[] = MosaicUtils.readAndSplit(directories[i] + File.separator + "JobID");
            if (s == null) continue;
            final int nf = Integer.parseInt(s[1]);
            String filename = s[2];

            final int idp = filename.lastIndexOf(".");
            if (idp >= 0) {
                filename = filename.substring(0, idp);
            }

            MosaicUtils.reorganize(output, "tmp", filename, directories[i], nf);
        }
    }

    /**
     * Get data
     *
     * @param output Output format
     * @param ss SecureShellSession
     * @param wp Progress bar window
     * @param bc Batch interface
     */
    private void getData(String output[], SecureShellSession ss, ProgressBarWin wp, BatchInterface bc) {
        final String tmp_dir = IJ.getDirectory("temp");
        final File[] fldw = new File[bc.getNJobs() * output.length + 1];

        fldw[0] = new File(bc.getDir() + File.separator + "JobID");
        for (int i = 0; i < bc.getNJobs(); i++) {
            for (int j = 0; j < output.length; j++) {
                final String tmp = new String(output[j]);
                fldw[i * output.length + j + 1] = new File(bc.getDir() + File.separator + tmp.replace("*", "tmp_" + (i + 1)));
            }
        }

        try {
            ShellCommand.exeCmdNoPrint("mkdir " + tmp_dir + File.separator + "Job" + bc.getJobID());
        }
        catch (final IOException e1) {
            e1.printStackTrace();
        }
        catch (final InterruptedException e1) {
            e1.printStackTrace();
        }

        wp.SetStatusMessage("Downloading...");
        ss.download(fldw, new File(tmp_dir + File.separator + "Job" + bc.getJobID()), wp, cp);
    }

    /**
     * Run a selected command on the cluster, ensure that the cluster has a Fiji
     * installed if not
     * it provide an automate installation, it wait the jobs to complete
     *
     * @param img Image the frames are parallelized
     * @param command to run the plugin
     * @param options options String to pass to the plugins
     * @param output output that the plugins generate with "*" as wild card
     *            example: "dir1/dir*_out/*.tif"
     *            on a file "tmp_1" will be expanded in
     *            "dir1/dirtmp1_1_out/tmp_1.tif"
     * @param ExtTime estimated running time (to select the queue on the
     *            cluster)
     * @return true if done, false if fail (or nothing to do)
     */
    private boolean runPluginsOnFrames(ImagePlus img, String command, String options, String output[], double ExtTime) {
        return runPluginsOnFrames(img, command, options, output, ExtTime, true);
    }

    /**
     * Get the base directory where files are transfer
     *
     * @return base directory as String
     */
    public String getClusterDirectory() {
        if (ss == null) {
            return null;
        }

        return ss.getTransfertDir();
    }

    /**
     * Upload a list of files creating a relative directory
     *
     * @param dir directory
     * @param fl Files to upload
     */
    public void upload(File dir, File[] fl) {
        if (ss == null) {
            ss = new SecureShellSession(cp);
        }

        final ProgressBarWin wp = new ProgressBarWin();
        ss.upload(fl, dir, wp, null);
    }

    /**
     * Upload a list of files
     *
     * @param fl Files to upload
     * @param show progress bar
     */
    public void upload(File[] fl) {
        if (ss == null) {
            ss = new SecureShellSession(cp);
        }

        final ProgressBarWin wp = new ProgressBarWin();
        ss.upload(fl, wp, null);
    }

    /**
     * Run a selected command on the cluster, ensure that the cluster has a Fiji
     * installed if not
     * it provide an automate installation
     *
     * @param img Image the frames are parallelized
     * @param command to run the plugin
     * @param options options String to pass to the plugins
     * @param output output that the plugins generate with "*" as wild card
     *            example: "dir1/dir*_out/*.tif"
     *            on a file "tmp_1" will be expanded in
     *            "dir1/dirtmp1_1_out/tmp_1.tif"
     * @param ExtTime exstimated running time (to select the queue on the
     *            cluster)
     * @param sync wait or not job to complete
     * @return true if done, false if fail (or nothing to do)
     */
    private boolean runPluginsOnFrames(ImagePlus img, String command, String options, String output[], double ExtTime, boolean sync) {
        if (ss == null) {
            ss = new SecureShellSession(cp);
        }
        final ProgressBarWin wp = new ProgressBarWin();

        // Create job array

        if (createJobArrayFromImage(img, command, options, ss, ExtTime, wp) == false) {
            wp.SetStatusMessage("Failed to create job array");
            try {
                ss.close();
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
            ss = null;
            wp.dispose();
            return false;
        }

        // if sync == true we do not wait return
        if (sync == false) {
            // Close the progress bar
            wp.dispose();
            try {
                ss.close();
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
            ss = null;
            return true;
        }

        final BatchInterface bc = cp.getBatchSystem();

        wp.SetProgress(0);
        wp.SetStatusMessage("Getting all jobs ...");

        BatchInterface bcl[] = bc.getAllJobs(ss, command);
        if (bcl == null) {
            bcl = new BatchInterface[0];
        }
        final ClusterStatusStack css[] = new ClusterStatusStack[bcl.length];
        final ImageStack st[] = new ImageStack[bcl.length];
        final ImagePlus ip[] = new ImagePlus[bcl.length];

        // get the status wait competition;
        for (int j = 0; j < bcl.length; j++) {
            bcl[j].createJobStatus();
            css[j] = new ClusterStatusStack();
            st[j] = css[j].CreateStack(bcl[j].getJobStatus());
            ip[j] = new ImagePlus("Cluster status " + bcl[j].getJobID(), st[j]);
            ip[j].show();
            bcl[j].setJobStatus(bcl[j].getJobStatus());
        }

        wp.SetProgress(0);

        /* Wait the various jobs complete */
        int n_bc = 0;
        while (n_bc < bcl.length) {
            double progress = 0.0;
            double total = 0.0;
            n_bc = 0;
            for (int j = 0; j < bcl.length; j++) {
                if (bcl[j] == null) {
                    n_bc++;
                    continue;
                }
                final String commands[] = new String[1];
                commands[0] = bcl[j].statusJobCommand();
                bcl[j].reset();
                ss.setShellProcessOutput(bcl[j]);
                ss.runCommands(commands);

                // Wait the command get Processed
                bcl[j].waitParsing();

                if (JobStatus.allComplete(bcl[j].getJobsStatus()) == true) {
                    css[j].UpdateStack(st[j], bcl[j].getJobStatus());
                    ip[j].updateAndDraw();

                    getData(output, ss, wp, bcl[j]);
                    bcl[j].clean(ss);

                    wp.SetProgress(0);
                    wp.SetStatusMessage("Reorganize...");
                    reorganize(output, bcl[j].getJobID());

                    // wp.SetProgress(0);
                    // wp.SetStatusMessage("Stack visualize...");
                    // stackVisualize(output,bcl[j].getJobID(),wp);

                    bcl[j] = null;

                    wp.SetStatusMessage("Computing ...");
                    final int p = (int) (progress * 100.0 / total);
                    wp.SetProgress(p);

                    continue;
                }
                css[j].UpdateStack(st[j], bcl[j].getJobStatus());
                ip[j].updateAndDraw();

                progress += JobStatus.countComplete(bcl[j].getJobsStatus());
                total += bcl[j].getJobStatus().length;
            }

            wp.SetStatusMessage("Computing ...");
            final int p = (int) (progress * 100.0 / total);
            wp.SetProgress(p);

            // wait to send and get again the status
            try {
                Thread.sleep(500);
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Close cluster status stack
        for (int i = 0; i < ip.length; i++) {
            ip[i].close();
        }

        // It never went online
        wp.SetProgress(0);
        wp.SetStatusMessage("Reorganize...");
        reorganize(output, 0);

        wp.SetProgress(0);
        wp.SetStatusMessage("Stack visualize...");
        stackVisualize(output, 0, wp);

        wp.SetStatusMessage("End");
        return true;
    }

    /**
     * Get the Finished jobs
     *
     * @param out output produced by the plugin
     * @param command to run the plugin
     * @param options to run the plugin
     * @param optionally ClusterGUI
     * @return
     */
    static public ClusterSession getFinishedJob(String[] out, String command, ClusterGUI cg) {
        return processImage(null, command, null, out, cg, new Float(0.0), new Float(0.0), true);
    }

    /**
     * Get the Finished jobs
     *
     * @param out output produced by the plugin
     * @param command to run the plugin
     * @return
     */
    static public ClusterSession getFinishedJob(String[] out, String command) {
        return processImage(null, command, null, out, null, new Float(0.0), new Float(0.0), true);
    }

    /**
     * Process the image
     *
     * @param aImp the image to process
     * @param command to run the plugin
     * @param options to run the plugins
     * @param output produced by the plugin
     * @param cg ClusterGUI
     * @return the session cluster
     */
    static public ClusterSession processImage(ImagePlus aImp, String command, String options, String[] out, ClusterGUI cg) {
        return processImage(aImp, command, options, out, cg, new Float(0.0), new Float(0.0), true);
    }

    /**
     * Process the image
     *
     * @param aImp the image to process
     * @param command to run the plugin
     * @param options to run the plugin
     * @param output produced by the plugin
     * @return the session cluster
     */
    static public ClusterSession processImage(ImagePlus aImp, String command, String options, String[] out) {
        return processImage(aImp, command, options, out, null, new Float(0.0), new Float(0.0), true);
    }

    /**
     * Process the image, min and max value are used for re-normalization if
     * max = 0.0 the max value and min value of the image are used
     *
     * @param aImp the image to process
     * @param command to run the plugin
     * @param output produced by the plugin
     * @param cg ClusterGUI
     * @param min minimum value
     * @param max maximum value
     * @return the session cluster
     */
    static private ClusterSession processImage(ImagePlus aImp, String command, String options, String[] out, ClusterGUI cg, Float max, Float min, boolean sync) {
        if (cg == null) {
            cg = new ClusterGUI();
        }

        final ClusterSession ss = cg.getClusterSession();

        // Get all image processor statistics and calculate the maximum
        if (max == 0.0) {
            if (aImp != null) {
                final StackStatistics stack_stats = new StackStatistics(aImp);
                max = (float) stack_stats.max;
                min = (float) stack_stats.min;

                // get the min and the max
            }
        }

        // Run plugin on frames
        if (ss.runPluginsOnFrames(aImp, command, "min=" + min + " max=" + max + " " + options, out, cg.getEstimatedTime(), sync) == false) {
            return null;
        }

        return ss;
    }

    /**
     * Process a list of files
     *
     * @param list of files to process
     * @param command to run the plugin
     * @param out output produced by the plugin *_xxxxxx.tif or *_xxxxxx.csv
     *            where * is subsituted
     *            with the image name
     * @param options to run the plugins
     * @return the cluster session
     */
    static public ClusterSession processFiles(File list[], String command, String options, String[] out) {
        return processFiles(list, command, options, out, null);
    }

    private static class MM {
        protected MM() {}
        public float min;
        public float max;
    }
    
    /**
     * Get the maximum and the minimum of a video
     *
     * @param mm output min and max
     */
    private static void getMaxMin(File fl, MM mm) {
        final Opener opener = new Opener();
        final ImagePlus imp = opener.openImage(fl.getAbsolutePath());

        float global_max = 0.0f;
        float global_min = 0.0f;

        if (imp != null) {
            final StackStatistics stack_stats = new StackStatistics(imp);
            global_max = (float) stack_stats.max;
            global_min = (float) stack_stats.min;

            // get the min and the max
        }

        if (global_max > mm.max) {
            mm.max = global_max;
        }

        if (global_min < mm.min) {
            mm.min = global_min;
        }
    }

    /**
     * Get the maximum and the minimum of a video
     *
     * @param mm output min and max
     */
    private static void getFilesMaxMin(File fls[], MM mm) {
        for (final File fl : fls) {
            getMaxMin(fl, mm);
        }
    }
    
    /**
     * Process a list of files
     *
     * @param list of files to process
     * @param command to run the plugin
     * @param out output produced by the plugin *_xxxxxx.tif or *_xxxxxx.csv
     *            where * is subsituted
     *            with the image name
     * @param options
     * @param ClusterGUI optionally a ClusterGUI
     * @return the cluster session
     */
    static public ClusterSession processFiles(File list[], String command, String options, String[] out, ClusterGUI cg) {
        if (cg == null) {
            cg = new ClusterGUI();
        }
        final ClusterSession ss = cg.getClusterSession();

        final MM mm = new MM();
        mm.min = new Float(Float.MAX_VALUE);
        mm.max = new Float(0.0);

        getFilesMaxMin(list, mm);

        for (final File fl : list) {
            // File

            processFile(fl, command, options, out, cg, mm.max, mm.min);
        }

        ss.runPluginsOnFrames(null, command, options, out, cg.getEstimatedTime());
        return ss;
    }

    /**
     * Process a file
     *
     * @param fl File to process
     * @param command to run the plugin
     * @param out output produced by the plugin *_xxxxxx.tif or *_xxxxxx.csv
     *            where * is subsituted
     *            with the image name
     * @param options to run the plugins
     * @return the cluster session
     */
    static public ClusterSession processFile(File fl, String command, String options, String[] out) {
        return processFile(fl, command, options, out, null);
    }

    /**
     * Process a file
     *
     * @param fl File to process
     * @param command to run the plugin
     * @param out output produced by the plugin *_xxxxxx.tif or *_xxxxxx.csv
     *            where * is subsituted
     *            with the image name
     * @param ClusterGUI optionally a ClusterGUI
     * @param options to run the plugin
     * @return the cluster session
     */
    static public ClusterSession processFile(File fl, String command, String options, String[] out, ClusterGUI cg) {
        if (cg == null) {
            cg = new ClusterGUI();
        }

        // open the image and process image
        final Opener opener = new Opener();
        final ImagePlus imp = opener.openImage(fl.getAbsolutePath());
        final ClusterSession ss = processImage(imp, command, options, out, cg, new Float(0.0), new Float(0.0), true);

        return ss;
    }

    /**
     * Return the filename produced by the split and upload
     *
     * @param num number
     * @return the filename
     */
    public String getSplitAndUploadFilename(int num) {
        return "tmp_" + (num + 1) + ".tif";
    }

    /**
     * Process a file
     *
     * @param fl File to process
     * @param command to run the plugin
     * @param options to run the plugin
     * @param min external minimum intensity (used for re-normalization across
     *            images)
     * @param max external maximum intensity (used for re-normalization across
     *            images)
     * @param ClusterGUI optionally a cluster GUI
     */
    static private ClusterSession processFile(File fl, String command, String options, String[] out, ClusterGUI cg, Float max, Float min) {
        // open the image and process image

        final Opener opener = new Opener();
        final ImagePlus imp = opener.openImage(fl.getAbsolutePath());
        final ClusterSession ss = processImage(imp, command, options, out, cg, max, min, false);

        return ss;
    }

    /**
     * It post-process the jobs data performing the following operation:
     * It search on the temporal directory for Job directory and for each
     * founded
     * job directory create a JobXXXX directory in path, where XXXXX is the
     * JobID. For each
     * Jobs directory copy all files and for all csv filename
     * supplied in outcsv perform a stitch operation (it try to stitch
     * all the CSV in one)
     *
     * @param path where to save
     * @param property if not null the stitch operation try to enumerate the
     *            file and set the property
     *            in the stitched file according to this enumeration
     * @param cls base class for internal conversion
     * @return File where the data are saved
     */
    public static File processJobsData(String path) {
        String dirS = null;

        // Save all JobID to the image folder
        // or ask for a directory
        final String dir[] = ClusterSession.getJobDirectories(0, null);

        if (dir.length > 0) {

            if (path != null) {
                dirS = path;
            }
            else {
                final DirectoryChooser dc = new DirectoryChooser("Choose directory where to save result");
                dirS = dc.getDirectory();
            }

            final ProgressBarWin wp = new ProgressBarWin();

            for (int i = 0; i < dir.length; i++) {
                wp.SetStatusMessage("Moving " + dir[i]);

                try {
                    final String[] tmp = dir[i].split(File.separator);

                    final File t = new File(dirS + File.separator + tmp[tmp.length - 1]);

                    ShellCommand.exeCmdNoPrint("cp -r " + dir[i] + " " + t);
                    System.out.println("cp -r " + dir[i] + " " + t);

                    // after copy remove the directory
                    ShellCommand.exeCmdNoPrint("rm -rf " + dir[i]);
                    System.out.println("rm -rf " + dir[i]);
                }
                catch (final IOException e) {
                    e.printStackTrace();
                }
                catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }

            wp.dispose();
        }

        return new File(dirS);
    }

    /**
     * Merge more jobs into one
     *
     * @param jobsrc source job
     * @param jobdst[] destination jobs
     */
    public static void mergeJobs(File jobsrc, File jobdst[]) {
        if (jobdst.length == 0) {
            return;
        }

        // Get a temporary directory create a dir
        final String tmp = IJ.getDirectory("temp") + File.separator + "temp_merge" + File.separator;
        try {
            ShellCommand.exeCmd("mkdir " + tmp);
            final ProgressBarWin pbw = new ProgressBarWin();
            ShellCommand.copy(jobsrc, new File(tmp), pbw);
            pbw.dispose();
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
        }

        final String[] sp_src = MosaicUtils.readAndSplit(jobsrc.getAbsolutePath() + File.separator + "JobID");
        final String sp[] = new String[sp_src.length];
        for (int i = 0; i < sp_src.length; i++) {
            sp[i] = sp_src[i];
        }

        // For each directory in job2 check if exist a directory in job1
        for (int i = 0; i < jobdst.length; i++) {
            final File fl[] = jobdst[i].listFiles();

            for (final File t : fl) {
                if (t.isDirectory() == false) {
                    continue;
                }

                final File dir = new File(tmp + File.separator + t.getName());

                if (dir.exists() == true) {
                    // exist the directory in job1
                    final ProgressBarWin pbw = new ProgressBarWin();
                    ShellCommand.copy(t, dir, pbw);
                    pbw.dispose();
                }
                else {
                    // Create a directory and copy
                    try {
                        ShellCommand.exeCmd("mkdir " + dir.getAbsolutePath());
                        final ProgressBarWin pbw = new ProgressBarWin();
                        ShellCommand.copy(t, dir, pbw);
                        pbw.dispose();
                    }
                    catch (final IOException e) {
                        e.printStackTrace();
                    }
                    catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // create a new JobID
            final String[] sp_dst = MosaicUtils.readAndSplit(jobdst[i].getAbsolutePath() + File.separator + "JobID");

            sp[0] = sp[0] + "#" + sp_dst[0];
            sp[1] = Integer.toString(Integer.parseInt(sp[1]) + Integer.parseInt(sp_dst[1]));
            sp[2] = sp[2] + "#" + sp_dst[2];
            if (!sp_src[3].equals(sp_dst[3])) {
                IJ.showMessage("Error : You cannot merge Jobs of two different plugins");
                return;
            }

        }

        PrintWriter out;

        // Create jobID file
        try {
            out = new PrintWriter(tmp + File.separator + "JobID");
            out.print(sp[0]);
            for (final String s : sp) {
                out.print(" " + s);
            }
            out.close();
        }
        catch (final FileNotFoundException e) {
            e.printStackTrace();
        }

        // remove the content from jobsrc
        try {
            final File[] fl_ = jobsrc.listFiles();
            for (final File f : fl_) {
                ShellCommand.exeCmd("rm -rf " + f.getAbsolutePath());
            }
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
        }

        // Copy the content
        final ProgressBarWin pbw = new ProgressBarWin();
        ShellCommand.copy(new File(tmp), jobsrc, pbw);
        pbw.dispose();

        // remove the temporary directory
        try {
            ShellCommand.exeCmd("rm -rf " + new File(tmp).getAbsolutePath());
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
        }

        // Remove other jobs
        for (int i = 0; i < jobdst.length; i++) {
            try {
                ShellCommand.exeCmd("rm -rf " + jobdst[i].getAbsolutePath());
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
