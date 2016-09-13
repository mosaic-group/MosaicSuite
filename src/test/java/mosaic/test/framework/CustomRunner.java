package mosaic.test.framework;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * CustomRunner class with {@link CustomRunListener} improves logging capabilities of
 * MOSAIC suite testing
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 *
 */
public class CustomRunner extends BlockJUnit4ClassRunner {
    protected CustomRunListener testListener;

    public CustomRunner(Class<?> aClass) throws InitializationError {
        super(aClass);
    }

    @Override
    public void run(final RunNotifier aNotifier) {
        // Add MOSAIC test run listener
        if (testListener == null) {
            testListener = new CustomRunListener();
            aNotifier.addListener(testListener);
        }
        aNotifier.fireTestRunStarted(super.getDescription());

        super.run(aNotifier);
    }
}
