package mosaic.region_competition.GUI;

public class Controller {
    protected ControllerWindow controllerFrame = null;
    private final Object pauseMonitor = new Object();
    private boolean pause = false;
    private boolean abort = false;
    
    public Controller(boolean aShowWindow) {
        if (aShowWindow) {
            controllerFrame = new ControllerWindow(this);
            controllerFrame.setVisible(true);
        }
    }
    
    public void close() {
        if (controllerFrame != null) {
            // If closing not done in thread then it is not possible to run plugin once again since it hangs on dispose() method.
            Thread closeThread = new Thread() {
                @Override
                public void run() {
                    controllerFrame.dispose();
                    controllerFrame = null;
                }
            };
            try {
                Thread.sleep(0);
                closeThread.start();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean hasAborted() {
        // Check if we should pause for a moment or if simulation is not aborted by user
        synchronized (pauseMonitor) {
            if (pause) {
                try {
                    pauseMonitor.wait();
                }
                catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (abort) {
                return true; // Pretend that we finished
            }
        }
        
        return false;
    }
    
    void stop() {
        synchronized (pauseMonitor) {
            abort = true;
            pause = false;
            pauseMonitor.notify();
        }
    }

    void pause() {
        pause = true;
    }

    void resume() {
        synchronized (pauseMonitor) {
            pause = false;
            pauseMonitor.notify();
        }
    }
    
}
