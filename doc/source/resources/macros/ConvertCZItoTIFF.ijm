directoryWithImages="/Directory/With/InputCZIimages/";
outputDirectory="/Output/Directory/For/TIF/Files/";

// do not show images - run in batch mode
setBatchMode(true);

// Iterate through all files in input direcotry
images=getFileList(directoryWithImages)
for (i = 0; i < images.length; i++) {
	// Process only images with czi extension
	if (endsWith(images[i],".czi")) {
		// Open .czi file
		fullFileName=directoryWithImages+"/"+images[i];
		print("Processing: [" + fullFileName + "]");
		run("Bio-Formats Importer", "open=["+fullFileName+"] color_mode=Default rois_import=[ROI manager] color_mode=Default rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT use_virtual_stack");    
		
		// Save it as a TIFF file with same name as original file
		fullTiffFileName=outputDirectory + "/" + replace(images[i], ".czi", ".tif");
		saveAs("Tiff", fullTiffFileName);
		close();
	}
}

