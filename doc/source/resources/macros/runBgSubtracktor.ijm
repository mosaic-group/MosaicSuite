// This macro run Background Subtractor plugin from MosaicSuite


// ----- USER PARAMETERS

// Provide input directory with images and output directory 
// for saving processed images (if same then input images will be changed!)
inputDirectory = "/home/gonciarz/1/testImages/"
outputDirectory="/home/gonciarz/1/results/"

// provide lenght of the sliding window, if set to -1 then plugin will auto-detect
bgSubtractorSlidingWindowLength=20

// If auto-detection is ON it might happen that plugin is not able to calculate proper length
// If skipOnFailure is 'false' then popup window will show in such a case (good for interactive operations), 
// if set to 'true' such a case will be only logged and detected lenght will be set to -1
//
// Example of log:
// 		Macro Options: [length=-1 skipOnFailure ]
// 		SkipOnFailure = true
// 		Auto-detected length for image a.tif=201
// 		Macro Options: [length=-1 skipOnFailure ]
// 		SkipOnFailure = true
// 		Auto-detected length for image failingImage.tif=-1

skipOnFailure=false


// ----- CODE

// Create output (results) directory, if exist nothing happens
File.makeDirectory(outputDirectory);
if (!File.exists(outputDirectory)) {
    exit("Unable to create directory: " + outputDirectory);
}


// Iterate over all 'tif' images in inputDirectory
images=getFileList(inputDirectory);
for (i = 0; i < images.length; i++) {

    // Skip if not 'tif' image
    if (!endsWith(images[i],".tif")) continue;

    // Open current image
    fullFileName=inputDirectory + "/" + images[i];
    print("Processing [" + fullFileName + "]");
    open(fullFileName);
    

    // ----- Run background subtractor ------
    skipCmd = "";
    if (skipOnFailure) skipCmd = "skipOnFailure";
    run("Background Subtractor", "length=" + bgSubtractorSlidingWindowLength + " " + skipCmd);	


    // Save processed image in output directory and close it
    save(outputDirectory + "/" + images[i]);
    close();
}

