package mosaic.core.cluster;


import ij.ImageStack;
import ij.process.ColorProcessor;

import java.awt.Color;

import mosaic.core.cluster.JobStatus.jobS;


/**
 * It store the sequence of images (PENDING RUNNING COMPLETE) that show
 * the status of the cluster
 *
 * @author Pietro Incardona
 */

class ClusterStatusStack {

    /**
     * Construct the stack from a status jobs
     *
     * @param js status job
     * @return ColorProcessor is the image
     */

    private ColorProcessor createResult(JobStatus js) {
        ColorProcessor ip = null;
        if (js == null) {
            ip = new ColorProcessor(200, 50);
            ip.setColor(new Color(0, 0, 255));
            ip.fill();
            ip.setColor(new Color(0, 0, 0));
            ip.drawString("UNKNOWN", 10, 25);
        }
        else if (js.getStatus() == jobS.PENDING) {
            if (js.getNotifiedStatus() != jobS.PENDING) {
                ip = new ColorProcessor(200, 50);
                ip.setColor(new Color(255, 164, 0));
                ip.fill();
                ip.setColor(new Color(0, 0, 0));
                ip.drawString(js.getStatus().toString(), 10, 25);
                js.setNotifiedStatus(jobS.PENDING);
            }

        }
        else if (js.getStatus() == jobS.RUNNING) {
            if (js.getNotifiedStatus() != jobS.RUNNING) {
                ip = new ColorProcessor(200, 50);
                ip.setColor(new Color(255, 255, 0));
                ip.fill();
                ip.setColor(new Color(0, 0, 0));
                ip.drawString(js.getStatus().toString(), 10, 25);
                js.setNotifiedStatus(jobS.RUNNING);
            }
        }
        else if (js.getStatus() == jobS.COMPLETE) {
            if (js.getNotifiedStatus() != jobS.COMPLETE) {
                ip = new ColorProcessor(200, 50);
                ip.setColor(new Color(0, 255, 0));
                ip.fill();
                ip.setColor(new Color(0, 0, 0));
                ip.drawString(js.getStatus().toString(), 10, 25);
                js.setNotifiedStatus(jobS.COMPLETE);
            }
        }
        else if (js.getStatus() == jobS.FAILED) {
            if (js.getNotifiedStatus() != jobS.FAILED) {
                ip = new ColorProcessor(200, 50);
                ip.setColor(new Color(255, 0, 0));
                ip.fill();
                ip.setColor(new Color(0, 0, 0));
                ip.drawString(js.getStatus().toString(), 10, 25);
                js.setNotifiedStatus(jobS.FAILED);
            }
        }
        else if (js.getStatus() == jobS.UNKNOWN) {
            if (js.getNotifiedStatus() != jobS.UNKNOWN) {
                ip = new ColorProcessor(200, 50);
                ip.setColor(new Color(0, 0, 255));
                ip.fill();
                ip.setColor(new Color(0, 0, 0));
                ip.drawString(js.getStatus().toString(), 10, 25);
                js.setNotifiedStatus(jobS.UNKNOWN);
            }
        }

        if (ip == null) {
            return null;
        }

        return ip;
    }

    /**
     * Create an image stack from a list of job status
     *
     * @param jb list of job status
     * @return the stack of images
     */

    ImageStack CreateStack(JobStatus jb[]) {
        final ImageStack is = new ImageStack(200, 50);

        for (int i = 0; i < jb.length; i++) {
            is.addSlice("Job " + i + 1, createResult(jb[i]));
        }

        return is;
    }

    /**
     * Update the stack from a list of JobStatus
     * (If the status of a job is changed )
     *
     * @param ip stack to update
     * @param jb list of status jobs
     */

    void UpdateStack(ImageStack ip, JobStatus jb[]) {
        for (int i = 0; i < jb.length; i++) {
            if (jb[i] != null) {
                if (jb[i].getNotifiedStatus() != jb[i].getStatus()) {
                    ip.deleteSlice(i + 1);
                    ip.addSlice("Staus job: " + i + 1, createResult(jb[i]), i);
                }
            }
        }
    }
}
