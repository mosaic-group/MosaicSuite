package mosaic.test.framework;

import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class CustomTestRunner extends BlockJUnit4ClassRunner {
    protected static CustomTestRunListener testListener;

    public CustomTestRunner(Class<?> aClass) throws InitializationError {
        super(aClass);
    }

    @Override
    public void run(final RunNotifier aNotifier) {
        // Add mosaic test run listener
        if (testListener == null) {
            testListener = new CustomTestRunListener();
            aNotifier.addListener(testListener);
        }

        aNotifier.fireTestRunStarted(super.getDescription());
        super.run(aNotifier);
    }
    
//    @Override
//    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
//        Description description = describeChild(method);
//        
//        // Check annotations of each test and take proper actions
//        EachTestNotifier notifiers = new EachTestNotifier(notifier, description);
//        if (isAnnotationSet(SkipThisTest.class, description)) {
//            notifiers.fireTestIgnored();
//            return;
//        }
//        
//        // Run test!
//        notifiers.fireTestStarted();
//        try {
//            methodBlock(method).evaluate();
//        } catch (Throwable e) {
//            e.printStackTrace();
//        } finally {
//            notifiers.fireTestFinished();
//        }
//    }

    /**
     * Check if given annotation is set for 
     * @param aClass
     * @param aDescription
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean isAnnotationSet( Class aClass, Description aDescription) {
        return (aDescription.getAnnotation(aClass) != null);
    }

}
