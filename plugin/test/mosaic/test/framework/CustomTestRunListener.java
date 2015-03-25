package mosaic.test.framework;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

public class CustomTestRunListener extends RunListener {
    @Override   
    public void testStarted(Description aDescription) {
        TestInfo.iTestCaseName = aDescription.isTest() ? aDescription.getMethodName() : null;
    }
    
    @Override
    public void testRunStarted(Description aDescription) {
        TestInfo.iTestSuiteName = aDescription.isSuite() ? aDescription.getClassName() : null;
    }
}
