

import bregman.GUI;
import bregman.GenericGUI;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;


public class BregmanGLM_Batch implements PlugInFilter {

	private ImagePlus OriginalImagePlus = null;

	public int setup(String arg0, ImagePlus active_img) {
		this.OriginalImagePlus = active_img;
		//IJ.log("arg0 " + arg0);
		String[] args =ImageJ.getArgs();
		int l=args.length;
		//IJ.log("args" + l);
		boolean batch=false;
		for(int i=0; i<l; i++){
			//IJ.log("arg" + i+ args[i]);
			if(args[i]!=null){
				if (args[i].endsWith("batch")) batch=true;
			}
		}
		//IJ.log("batchmode" + batch);
		try {
			GenericGUI window = new GenericGUI(batch);
			window.run("");
			//IJ.log("Plugin exiting");
			//Headless hd= new Headless("/Users/arizk/tmpt/");
			//Headless hd= new Headless("/gpfs/home/rizk_a/Rab7_b/");
			//window.frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return DONE;
	}

	public void run(ImageProcessor imp) {


	}




}