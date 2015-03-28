package mosaic.test.framework;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

public class CustomRunListener extends RunListener {
    @Override   
    public void testStarted(Description aDescription) {
        Info.iTestCaseName = aDescription.isTest() ? aDescription.getMethodName() : null;
    }
    
    @Override
    public void testRunStarted(Description aDescription) {
        Info.iTestSuiteName = aDescription.isSuite() ? aDescription.getClassName() : null;
    }
}
