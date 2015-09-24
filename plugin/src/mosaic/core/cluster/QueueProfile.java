package mosaic.core.cluster;


/**
 * A cluster in general has a batch system, all (most) of them has a queue system to manage
 * the jobs. Queue profile store the information of a queue
 *
 * @author Pietro Incardona
 */
public class QueueProfile {

    protected String queue;
    protected String hardware;
    protected double limit;

    public void setqueue(String queue) {
        this.queue = queue;
    }

    public void sethardware(String hardware) {
        this.hardware = hardware;
    }

    public void setlimit(double limit) {
        this.limit = limit;
    }

    public String getqueue() {
        return queue;
    }

    public String gethardware() {
        return hardware;
    }

    public double getlimit() {
        return limit;
    }
}
