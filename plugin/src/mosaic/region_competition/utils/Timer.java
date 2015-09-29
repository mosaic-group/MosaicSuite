package mosaic.region_competition.utils;


public class Timer {
    private long start;
    private long end;
    private long duration = 0;

    /**
     * starts the timer
     * 
     * @return starttime in ms
     */
    public long tic() {
        duration = 0;
        start = System.nanoTime();
        return start / 1000;
    }

    /**
     * @return time elapsed since tic() in ms
     */
    public long toc() {
        end = System.nanoTime();
        duration = end - start;
        return (duration) / 1000;
    }

    public long lastResult() {
        return duration / 1000;
    }
}
