package mosaic.utils;

import org.apache.log4j.Logger;

/**
 * Simple class used for time measurement.
 *
 * Usage:
 *  SomeMethod() {
 *      TimeMeasurement tm = new TimeMeasurement();
 *      ....
 *      tm.logLapTimeSec("Sim start");
 *      ....
 *      tm.logLapTimeSec("Sim stop");
 *  }
 *
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class TimeMeasurement {
    private static final Logger logger = Logger.getLogger(TimeMeasurement.class);

    private final long startTime;
    private long lastTime;

    public TimeMeasurement() {
        startTime = System.nanoTime();
        lastTime = startTime;
    }

    /**
     * @return Number of nanoseconds from moment this object was created.
     */
    public long getTimeNanoSec() {
        final long endTime = System.nanoTime();
        final long duration = (endTime - startTime);
        lastTime = endTime;
        return duration;
    }

    /**
     * @return Number of microseconds from moment this object was created.
     */
    public long getTimeMicroSec() {
        return getTimeNanoSec()/1000;
    }

    /**
     * @return Number of milliseconds from moment this object was created.
     */
    public long getTimeMilliSec() {
        return getTimeNanoSec()/1000000;
    }

    /**
     * @return Number of seconds from moment this object was created.
     */
    public long getTimeSec() {
        return getTimeNanoSec()/1000000000;
    }

    /**
     * @return Number of nanoseconds from last call to any "getTime*" method.
     */
    public long getLapTimeNanoSec() {
        final long endTime = System.nanoTime();
        final long duration = (endTime - lastTime);
        lastTime = endTime;
        return duration;
    }

    /**
     * @return Number of microseconds from last call to any "getTime*" method.
     */
    public long getLapTimeMicroSec() {
        return getLapTimeNanoSec()/1000;
    }

    /**
     * @return Number of milliseconds from last call to any "getTime*" method.
     */
    public long getLapTimeMilliSec() {
        return getLapTimeNanoSec()/1000000;
    }

    /**
     * @return Number of seconds from last call to any "getTime*" method.
     */
    public long getLapTimeSec() {
        return getLapTimeNanoSec()/1000000000;
    }

    /**
     * Logs number of seconds from last call to any "get(Lap)Time*" method.
     * @param aName name used in log in format: "aName (s): 12412"
     */
    public void logLapTimeNanoSec(String aName) {
        logger.info(aName + " (ns): " + getLapTimeNanoSec());
    }
    
    /**
     * Logs number of seconds from moment this object was created.
     * @param aName name used in log in format: "aName (s): 12412"
     */
    public void logTimeNanoSec(String aName) {
        logger.info(aName + " (ns): " + getTimeNanoSec());
    }
    
    /**
     * Logs number of seconds from last call to any "get(Lap)Time*" method.
     * @param aName name used in log in format: "aName (s): 12412"
     */
    public void logLapTimeSec(String aName) {
        logger.info(aName + " (s): " + getLapTimeSec());
    }
    
    /**
     * Logs number of seconds from moment this object was created.
     * @param aName name used in log in format: "aName (s): 12412"
     */
    public void logTimeSec(String aName) {
        logger.info(aName + " (s): " + getTimeSec());
    }
}
