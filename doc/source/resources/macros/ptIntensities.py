from ij import WindowManager
from ij.text import TextWindow
from ij.measure import ResultsTable


# ----------------- USER INPUT ------------------

# Name of a tables for input (All Trajectories to Table) and output
inputTableName = "Results";
outputTableName = "Intensity";

# Provide radius used for Particle Tracking (or any other useful in postprocessing)
radius = 2

# prints ready to copy/paste data in CSV format and/or shows another results table (set to False to disable)
printOutputData = False
showOutputTable = True

# ----------------- CODE -----------------------

def findResultsTable(nameOfTable):
	''' Find the window with results '''
	resultsTable = None
	nonImgWindows = WindowManager.getNonImageWindows()
	for win in nonImgWindows:
		if not isinstance(win, TextWindow): continue
		if nameOfTable == win.getTitle(): 
		    resultsTable = win.getTextPanel().getOrCreateResultsTable()
		    break
		    
	return resultsTable


def getIntensityData(stack, radius, frame, x, y, z, xLen, yLen, zLen):
	m0 = 0
	size = 0
	for k in range(-radius, radius + 1):
		pz = z + k;
		if pz < 0 or pz >= zLen: continue

		# there should be no channels so calculate slice from frame and 'z' only
		s = int((frame)*zLen + z + 1)
		p = stack.getProcessor(s).convertToFloat().getPixels()
	
		for j in range(-radius, radius + 1):
			py = y + j
			if py < 0 or py >= yLen: continue
			for i in range(-radius, radius + 1):
				px = x + i
				if px < 0 or px >= xLen: continue

				# Limit pixels to those less or equall radius
				dist = (i*i+j*j+k*k)**(1.0/2)
				if dist > radius: continue
				
				v = p[(int(py))*xLen+int(px)]
				m0 += v
				size += 1
	return (m0, size, m0/size)


def runScript():

	# find table with trajectories
	rt = findResultsTable(inputTableName)
	if rt == None:
	    print("Results table window titled [" + inputTableName + "] not found!")
	    return
	    
	# get input image and its properties
	img = WindowManager.getCurrentImage()
	if img == None:
		print("Could not access input image!")
		return
	print("Processing image:", img)
	xLen = img.getWidth()
	yLen = img.getHeight()
	zLen = img.getNSlices()
	noOfFrames = img.getNFrames()
	noOfChannels = img.getNChannels()
	stack = img.getStack()
	if (noOfChannels > 1):
		print("Cannot process images with channels. Convert image to single channel first!")
		return

	# Start processin data row by row...
	numOfRows = rt.getCounter();	
	
	if numOfRows > 1: 
	
		#create output tableName
		if showOutputTable: outputTable = ResultsTable()

		# if output in csv format requested print header
		if printOutputData:
			print("trajectory;frame;m0;sizeInPixels;avgIntensity");

		for idx in range(0, numOfRows): 
			trajectoryId = rt.getValue("Trajectory", idx)
			x = rt.getValue("x", idx)
			y = rt.getValue("y", idx)
			z = rt.getValue("z", idx)
			frame = rt.getValue("Frame", idx)

			m0, size, avgInt = getIntensityData(stack, radius, frame, x, y, z, xLen, yLen, zLen)
		
			# if output in csv format requested print it
			if printOutputData: 
				print(str(int(trajectoryId)) + ";" + str(frame) + ";" + str(m0) + ";" + str(size) + ";" + str(avgInt)); 
			
			# if output table requested update it with data
			if showOutputTable:
				outputTable.incrementCounter()
				outputTable.addValue("", idx+1)
				outputTable.addValue("trajectory", int(trajectoryId))
				outputTable.addValue("frame", frame)
				outputTable.addValue("m0", m0)
				outputTable.addValue("sizeInPixels", size)				
				outputTable.addValue("avgIntensity", avgInt)
			
		if showOutputTable: outputTable.show(outputTableName)


if __name__ in ['__builtin__','__main__']:
	runScript()
	print("DONE!")
	