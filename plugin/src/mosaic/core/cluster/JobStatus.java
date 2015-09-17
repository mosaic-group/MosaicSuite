package mosaic.core.cluster;


/**
 * Structure that store and retain information about the status of a job
 *
 * @author Pietro Incardona
 */

class JobStatus {

    enum jobS {
        PENDING, RUNNING, COMPLETE, FAILED, UNKNOWN
    };

    private jobS js;
    private jobS js_N;

    /**
     * Count the number of complete jobs
     *
     * @param jb List of jobs
     * @return the number of completed jobs
     */
    static int countComplete(JobStatus jb[]) {
        int ncc = 0;

        for (int i = 0; i < jb.length; i++) {
            if (jb[i] == null || jb[i].getStatus() == jobS.COMPLETE || jb[i].getStatus() == jobS.UNKNOWN
                    || jb[i].getStatus() == jobS.FAILED) {
                ncc++;
            }
        }

        return ncc;
    }

    /**
     * Check if all the jobs are completed
     *
     * @param jb list of jobs
     * @return true if all jobs are completed
     */
    static boolean allComplete(JobStatus jb[]) {
        for (int i = 0; i < jb.length; i++) {
            if (jb[i] != null
                    && (jb[i].getStatus() != jobS.COMPLETE && jb[i].getStatus() != jobS.UNKNOWN && jb[i].getStatus() != jobS.FAILED)) {
                return false;
            }
        }

        return true;
    }
    /**
     * Get the status of the job
     *
     * @return jobS enum
     */
    jobS getStatus() {
        return js;
    }

    /**
     * Get the notified status of the job (used by Cluster status stack
     * to understand if it has to change the status of the job from
     * the previous time )
     *
     * @return
     */
    jobS getNotifiedStatus() {
        return js_N;
    }

    /**
     * Set the notified status of the job
     *
     * @see getNotifiedStatus()
     * @param js_
     */
    void setNotifiedStatus(jobS js_) {
        js_N = js_;
    }

    /**
     * Set the status of the job
     *
     * @param js_ job status
     */
    void setStatus(jobS js_) {
        js = js_;
    }
};
