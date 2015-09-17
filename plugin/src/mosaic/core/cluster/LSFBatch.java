package mosaic.core.cluster;


import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mosaic.core.cluster.JobStatus.jobS;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;


/**
 * This class implement the Platform LSF batch system, in particular implement
 * the interface BatchInterface
 * to is able to return LSF specific commands based on what we want to do, and
 * is able to parse LSF specific
 * output to get the job information
 * 
 * @author Pietro Incardona
 */
class LSFBatch implements BatchInterface {

    private int AJobID = 0;
    private OutputType tp;
    private JobStatus[] jb;
    private String script;
    private ClusterProfile cp;
    private int nJobs = 0;
    private String lDir;

    public LSFBatch(ClusterProfile cp_) {
        cp = cp_;
    }

    @Override
    public String getScript(String img_script_, String session_id, double ext, int njob, int ns) {
        // Check if exist a queue
        String queue = cp.getQueue(ext);
        if (queue == null) {
            IJ.error("Error", "Error the following cluster has all the queues shorter than " + ext + " minutes");
            return null;
        }

        script = session_id;
                       return new String(
                               "#!/bin/bash \n" +
                               "#BSUB -q " + queue + "\n" +
                               "#BSUB -n "+ ns +" \n" +
                               "#BSUB -J \"" + session_id + "[1-" + njob  + "]\" \n" +
                               "#BSUB -R span[hosts=1]\n" +
                               "#BSUB -o " + session_id + ".out.%J \n" +
                               "\n" +
                               "echo \"running " + script + " on index $LSB_JOBINDEX\" \n" +
                               cp.getRunningDir() + "Fiji.app/ImageJ-linux64" + " --headless -batch " + img_script_ + " $LSB_JOBINDEX");

    }

    @Override
    public String runCommand(String tdir) {
        tp = OutputType.LAUNCH;
        return new String("bsub < " + script);
    }

    @Override
    public String statusJobCommand() {
        tp = OutputType.STATUS;
        return new String("bjobs " + AJobID);
    }

    /**
     * Create an array of jobs
     */
    @Override
    public void createJobStatus() {
        jb = new JobStatus[nJobs];
    }

    private int jobArrayID(String aID) {
        Pattern jobID = Pattern.compile("\\x5B[0-9]+\\x5D");

        Matcher matcher = jobID.matcher(aID);
        if (matcher.find()) {
            String sub = matcher.group(0);
            sub = sub.substring(1, sub.length() - 1);
            return Integer.parseInt(sub);
        }

        return 0;
    }

    private jobS jobArrayStatus(String aID) {
        if (aID.equals("PEND")) {
            return jobS.PENDING;
        }
        else if (aID.equals("RUN")) {
            return jobS.RUNNING;
        }
        else if (aID.equals("DONE")) {
            return jobS.COMPLETE;
        }
        else if (aID.equals("EXIT")) {
            return jobS.FAILED;
        }
        return jobS.UNKNOWN;
    }

    /**
     * Parse the LSF status bjobs JOBID command
     *
     * @param prs String to parse
     * @param jobs array with the updated status of the jobs
     * @return String the string with the unparsed part
     */
    private String parseStatus(String prs, JobStatus jobs[]) {
        int nele = 0;
        boolean unparse_last = true;

        if (prs.endsWith("\n")) unparse_last = false;

        String[] elements = prs.split("\n");
        nele = elements.length - 1;

        if (unparse_last == false) nele = elements.length;

        for (int i = 0; i < nele; i++) {
            if (elements[i].equals("Job <" + AJobID + "> is not found\r")) {
                nele_parsed = nJobs;

                for (int j = 0; j < jobs.length; j++) {
                    jobs[j] = new JobStatus();
                    jobs[j].setStatus(jobS.UNKNOWN);
                }

                return "";
            }

            Vector<String> vt = new Vector<String>();
            String[] sub_elements = elements[i].split(" ");
            for (int j = 0; j < sub_elements.length; j++) {
                if (sub_elements[j].length() != 0) vt.add(sub_elements[j]);
            }

            int ja_id = 0;

            if (vt.size() > 2) {
                if (jobArrayStatus(vt.get(2)) == jobS.RUNNING || jobArrayStatus(vt.get(2)) == jobS.COMPLETE
                        || jobArrayStatus(vt.get(2)) == jobS.FAILED) {
                    ja_id = jobArrayID(vt.get(6));
                    updateJobStatus(jobs, vt, ja_id);
                }
                else if (jobArrayStatus(vt.get(2)) == jobS.PENDING) {
                    ja_id = jobArrayID(vt.get(5));
                    updateJobStatus(jobs, vt, ja_id);
                }
                else if (jobArrayStatus(vt.get(2)) == jobS.UNKNOWN) {
                    if (vt.size() < 6) continue;

                    ja_id = jobArrayID(vt.get(5));
                    updateJobStatus(jobs, vt, ja_id);
                }
            }

            System.out.println("Parsing: [" + elements[i] + "]");
            System.out.println(" nele_parsed: " + nele_parsed);
        }

        if (unparse_last == true)
            return elements[elements.length - 1];
        else
            return new String("");
    }

    private void updateJobStatus(JobStatus[] jobs, Vector<String> vt, int ja_id) {
        ja_id = ja_id - 1;
        if (ja_id >= 0) {
            jobs[ja_id] = new JobStatus();
            jobs[ja_id].setStatus(jobArrayStatus(vt.get(2)));
            nele_parsed++;
        }
    }

    private int parseJobID(String id) {
        Pattern jobID = Pattern.compile("<[0-9]+>");

        Matcher matcher = jobID.matcher(id);
        if (matcher.find()) {
            String sub = matcher.group(0);
            sub = sub.substring(1, sub.length() - 1);
            return Integer.parseInt(sub);
        }

        return 0;
    }

    @Override
    public void setJobStatus(JobStatus[] jb_) {
        jb = jb_;
    }

    @Override
    public JobStatus[] getJobStatus() {
        return jb;
    }

    @Override
    public int getJobID() {
        return AJobID;
    }

    @Override
    public String Process(String str) {
        if (tp == OutputType.STATUS) {
            return parseStatus(str, jb);
        }
        else if (tp == OutputType.LAUNCH) {
            System.out.println("ParseJobID [" + str + "]");
            int tmp = parseJobID(str);
            if (tmp == 0)
                return str;
            else {
                if (AJobID == 0) AJobID = tmp;
                System.out.println("get Job ID: " + AJobID);
                return "";
            }
        }
        return "";
    }

    @Override
    public void reset() {
        nele_parsed = 0;
    }

    private int nele_parsed = 0;

    @Override
    public void waitParsing() {
        int ntime = 0;

        // Ugly but work
        while (nele_parsed < nJobs && ntime < 100) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            ntime++;
        }
        try {
            Thread.sleep(100);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Send Command");
    }

    /**
     * Try to load the dir
     * 
     * @param dir Directory to load
     * @param ss SecureShellSession
     * @param cp_ cluster profile
     * @param command related
     * @return true if the folder contain data
     */
    private boolean loadDir(String dir, SecureShellSession ss, ClusterProfile cp_, String command) {
        String tmp_dir = IJ.getDirectory("temp");
        File[] fl = new File[1];

        fl[0] = new File(dir + File.separator + "JobID");

        try {
            ShellCommand.exeCmdNoPrint("mkdir " + tmp_dir);
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get the Job ID
        File jid = new File(tmp_dir + "JobID");
        jid.delete();
        ss.download(cp.getPassword(), fl, new File(tmp_dir), null, cp);
        if (jid.exists() == false) {
            return false;
        }

        String s[] = MosaicUtils.readAndSplit(tmp_dir + File.separator + "JobID");

        String tmp = new String(command);
        if (s.length < 3 && !s[2].equals(tmp.replace(" ", "_"))) return false;

        AJobID = Integer.parseInt(s[0]);
        nJobs = Integer.parseInt(s[1]);

        tp = OutputType.STATUS;
        cp = cp_;
        lDir = dir;

        return true;
    }

    /**
     * Get all jobs running on the cluster
     * 
     * @param ss Shell channel
     * @return List of Batch Interfaces, null if fail
     */
    @Override
    public BatchInterface[] getAllJobs(SecureShellSession ss, String command) {
        String[] dirs = ss.getDirs(cp.getRunningDir());
        if (dirs == null) return null;

        Vector<LSFBatch> bc_v = new Vector<LSFBatch>();

        for (int i = 0; i < dirs.length; i++) {
            LSFBatch tmp = new LSFBatch(cp);

            if (tmp.loadDir(cp.getRunningDir() + dirs[i], ss, cp, command) == false) {
                continue;
            }
            bc_v.add(tmp);
        }

        LSFBatch[] bc_a = new LSFBatch[bc_v.size()];
        bc_v.toArray(bc_a);

        return bc_a;
    }

    /**
     * Return the JobStatus for the associated Jobs array
     */
    @Override
    public JobStatus[] getJobsStatus() {
        return jb;
    }

    /**
     * Get number of jobs of the associated job array
     */
    @Override
    public int getNJobs() {
        return nJobs;
    }

    /**
     * Clean the Job array directory
     */
    @Override
    public void clean(SecureShellSession ss) {
        String[] commands = new String[1];
        commands[0] = new String("rm -rf " + lDir);

        ss.runCommands(cp.getPassword(), commands);
    }

    /**
     * Get the working directory for this job array
     */
    @Override
    public String getDir() {
        return lDir;
    }

    @Override
    public void setJobID(int id) {
        AJobID = id;
    }
}
