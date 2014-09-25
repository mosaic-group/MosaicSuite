package mosaic.plugins;


import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class Patching_  implements PlugInFilter{

	int mPatchSize = 80; 
	@Override
	public int setup(String arg, ImagePlus imp) {

		//ImageProcessor vOriginalImageProc = imp.getProcessor();
		GenericDialog gd = new GenericDialog("Get the paths of 2 images");
		gd.addStringField("Data image path", "/");
		gd.addStringField("Label image path", "/");
		gd.addStringField("Patch directory", "/");
		gd.showDialog();
		String vDataPath = gd.getNextString();
		String vLabelPath = gd.getNextString();
		String vPatchPath = gd.getNextString();
//		String vDataPath = "/home/janickc/phd/projects/unispital/scripts/patching/21_2-1_red.tif";
//		String vLabelPath = "/home/janickc/phd/projects/unispital/scripts/patching/21_2-1_segmented.tif";
//		String vPatchPath = "/home/janickc/phd/projects/unispital/scripts/patching/patches/";
		
		ImageProcessor vDataProc = IJ.openImage(vDataPath).getProcessor();
		ImageProcessor vLabelProc = IJ.openImage(vLabelPath).getProcessor();

		for(int i = 0; i < vDataProc.getWidth() - mPatchSize; i = i + mPatchSize/2) {
			for(int j = 0; j < vDataProc.getHeight() - mPatchSize; j = j + mPatchSize/2) {
				int vCenter = vLabelProc.getPixel(i + mPatchSize/2, j + mPatchSize/2);
				int vLabel = -1;

				if(vCenter == 0){vLabel = 1;}  		// healthy crypts
				else if (vCenter == 70) {vLabel = 2;} 	// healthy white region under crypts 	
				else if (vCenter == 9) {vLabel = 3;}	// pink region
				else if (vCenter == 200) {vLabel = 4;}	// unhealthy white region
				else if (vCenter == 250) {vLabel = 5;}	// unhealthy crypts
				else if (vCenter == 100) {vLabel = 6;}	// rest all
				else {vLabel = 7;}		//should be empty
				
				ByteProcessor vPatch = new ByteProcessor(mPatchSize, mPatchSize);
				for(int y = 0; y < mPatchSize; y++){
					for(int x = 0; x < mPatchSize; x++){
						int vValue = vDataProc.get(i+x, j+y);
						vPatch.set(x, y, vValue);
					}
				}
				File vDir = new File(vPatchPath + "/" + vLabel);
				if(!vDir.exists()) {
					vDir.mkdirs();
				}
				
				File vDataFile = new File(vDataPath);
				String vImageName = vDataFile.getName();
				vImageName = vImageName.substring(0, vImageName.length()-8);
				
				IJ.save(new ImagePlus("", vPatch),vDir.getAbsolutePath()+
						"/" + 
						vImageName+"_patch_x_"+i+"_y_"+j+"_class_"+vLabel+".tif");
			}
		}

		
		return DOES_ALL + DONE;
	}

	@Override
	public void run(ImageProcessor ip) {
		// TODO Auto-generated method stub
		
	}	

}

