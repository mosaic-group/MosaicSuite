package mosaic.plugins;

import java.io.File;

import ij.IJ;
import ij.plugin.PlugIn;
import mosaic.core.cluster.ClusterSession;

/**
 * Small utility to merge jobs together
 * @author Pietro Incardona
 */
public class MergeJobs implements PlugIn
{
    private String iJobsDir = null;

    @Override
    public void run(String aArgs) {
        String dir = (iJobsDir == null) ? IJ.getDirectory("Choose merge directory") : iJobsDir;

        final String[] directories = ClusterSession.getJobDirectories(0 /* get all */, dir);
        // If there is more than one job directory -> should be merged
        if (directories.length > 1) {
            // Prepare all dirs directories[1..n] and merge them to directories[0]
            final File[] jobDirs = new File[directories.length - 1];
            for (int s = 1 ; s < directories.length; ++s) {
                jobDirs[s - 1] = new File(directories[s]);
            }
            ClusterSession.mergeJobs(new File(directories[0]), jobDirs);
        }
    }
    
    /**
     * Sets the cluster jobs directory
     */
    public void setJobsDir(String aJobsDir) {
        iJobsDir = aJobsDir;
    }
}
