run("32-bit");
run("Brightness/Contrast...");
run("Enhance Contrast", "saturated=0.35");
run("Gaussian Blur...", "sigma=15 stack");
setAutoThreshold("Default dark");
run("Convert to Mask", "  black");
