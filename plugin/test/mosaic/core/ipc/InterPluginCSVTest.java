package mosaic.core.ipc;

import java.util.Vector;

import mosaic.plugins.utils.Debug;
import mosaic.test.framework.CommonBase;

import org.junit.Test;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class InterPluginCSVTest extends CommonBase {
    static public class Thing {
        public static final String[] Thing_map = new String[] 
        { 
            "Frame",
            "x"
        };
        public static final CellProcessor[] Thing_CellProcessor = new CellProcessor[] 
        {
            new ParseInt(),
            new ParseDouble()
//            new ParseInt()
        };
        
        int frame;
        double x;
        
        public Thing(int aFrame, double aX) {frame = aFrame; x = aX;}
        public Thing() {}//frame = 444; x = 555; deadVariable = 1;}
        

        public int getFrame() {return frame;}
        

        public void setFrame(int aFrame) {System.out.println("SSSSSSSSET");frame = aFrame;}
        

        public double getx() {return x;}
        

        public void setx(double val) {x = val;}
        
        //////// 
        public void setCoord_X(double Coord_X_) {x = Coord_X_;}
        public void setImage_ID2(int Image_ID_) {frame = Image_ID_;}

//        public void setData(Thing p)
//        {
//            this.frame = p.frame;
//            this.x = p.x;
//        }
        
        @Override 
        public String toString() { 
            String str = "Thing {" + frame + "," + x + "}";
            return str;
        }
    }
    
    @Test
    public void testCsv() {
        String file = "/tmp/xxx.csv";
        
        InterPluginCSV<Thing> csv = new InterPluginCSV<Thing>(Thing.class);
        csv.setMetaInformation("asdf", "Info meta added ");
        Vector<Thing> pt = new Vector<Thing>();
        pt.add(new Thing(1, 3.14));
        pt.add(new Thing(2, 2.287));
        OutputChoose oc = new OutputChoose();
        oc.map = Thing.Thing_map;
        oc.cel = Thing.Thing_CellProcessor;
        csv.Write(file, pt , oc , false);
        System.out.println("+++++++++++++++++ WRITEN");
        InterPluginCSV<Thing> csvRead = new InterPluginCSV<Thing>(Thing.class);
        csvRead.setCSVPreferenceFromFile(file);
        Vector<Thing> outdst = csvRead.Read(file, oc);
        
        for (Thing th : outdst) {System.out.println(th);};
        System.out.println("+++++++++++++++++ READ");
        Debug.print(csvRead.getMetaInformation("asdf"));
        csvRead.setMetaInformation("aad2", "f f sf as f");
        csvRead.Write("/tmp/yyy.csv", pt , oc , false);
    }


}
