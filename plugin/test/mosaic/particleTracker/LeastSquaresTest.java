package mosaic.particleTracker;

import static org.junit.Assert.assertTrue;
import io.scif.img.ImgOpener;
import mosaic.test.framework.CommonTestBase;

import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class LeastSquaresTest extends CommonTestBase {
    //protected static final Logger log = LoggerFactory.getLogger(LeastSquaresTest.class.getSimpleName());
    
    @Test
    public void test1() { 
//        System.out.println("FIRST LINE OF TEST " + new Object(){}.getClass().getEnclosingMethod().getName());
//        log.error("Test started");
//        assertTrue("Whole test suit for LEastSquares is to be implemented", true);
//        System.out.println(this.getClass().getName() + " " + this.getClass().getSimpleName());
//        
//        System.out.println("LAST LINE OF TEST " + new Object(){}.getClass().getEnclosingMethod().getName());
        assertTrue("Whole test suit for LEastSquares is to be implemented", true);
    }

    @Test
    public void test2() { 
        //new ImageJ();
//        final Context context = (Context)
//                IJ.runPlugIn(Context.class.getName(), ""); 
//        ImgOpener io = new ImgOpener(context);
        final ImgOpener io = new ImgOpener();
        System.out.println("FIRST LINE OF TEST " + new Object(){}.getClass().getEnclosingMethod().getName());
        
        assertTrue("Whole test suit for LEastSquares is to be implemented", true);
        System.out.println(this.getClass().getName() + " " + this.getClass().getSimpleName());
        
        System.out.println("LAST LINE OF TEST " + new Object(){}.getClass().getEnclosingMethod().getName());
    }
}
