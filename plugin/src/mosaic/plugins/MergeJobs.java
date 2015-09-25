package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.File;

import mosaic.core.cluster.ClusterSession;


/**
 * @author Pietro Incardona
 *
 * Small utility to merge jobs together
 *
 */

public class MergeJobs implements PlugInFilter
{
    String sd = null;

    /**
     *
     * Set the directory
     *
     * @param st Directory
     */
    public void setDir(String st)
    {
        sd = st;
    }

    @Override
    public void run(ImageProcessor arg0)
    {}

    @Override
    public int setup(String arg0, ImagePlus arg1)
    {
        String dir = null;
        if (sd == null) {
            dir = IJ.getDirectory("Choose merge directory");
        }
        else {
            dir = sd;
        }

        // Here we merge all the jobs

        final String [] jbs = ClusterSession.getJobDirectories(0, dir);
        if (jbs.length <= 1) {
            return DONE;
        }

        final File [] jbs_d = new File[jbs.length-1];

        for (int s = 0 ; s < jbs.length-1 ; s++)
        {
            jbs_d[s] = new File(jbs[s+1]);
        }

        ClusterSession.mergeJobs(new File(jbs[0]),jbs_d);

        return DONE;
    }
}