from ij import WindowManager
from ij.text import TextWindow
from ij.measure import ResultsTable


# ----------------- USER INPUT ------------------

# Name of a tables for input (All Trajectories to Table) and output
inputTableName = "Results";
outputTableName = "Velocity";

# prints ready to copy/paste data in CSV format and/or shows another results table (set to False to disable)
printOutputData = True
showOutputTable = True

# if both set to 1 then velocity is calculated as a pixels/frame ratio. 
# if resolution and fram interval known set it correctly for example if resolution is 5um/pixel
# and frame interval is 20 ms/frame then pixelResolution = 5e-06 and frameInterval = 20e-03
pixelResolution = 1e0 # in m/pixel
frameInterval = 1e0 # in s/frame


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


def mean(data):
	''' calculate mean of data '''
	n = len(data)
	if n < 1:
		raise ValueError('mean requires at least one data point')
	return sum(data)/float(n)


def stddev(data):
	''' calculate standard dev. of data '''
	n = len(data)
	if n < 1:
		raise ValueError('stddev requires at least one data point')
	m = mean(data)
	return (sum((x-m)**2 for x in data)/n)**0.5


def runScript():

	# find table with trajectories
	rt = findResultsTable(inputTableName)
	if rt == None:
	    print("Results table window titled [" + inputTableName + "] not found!")
	    return
	
	numOfRows = rt.getCounter();	
	
	if numOfRows > 1: 
	
		#create output tableName
		if showOutputTable: velocityRT = ResultsTable()

		# if output in csv format requested print header
		if printOutputData:
			print("trajectory;pathLen;noOfFrames;velocity;velocity stddev");

		# read first data point
		trajectoryId = rt.getValue("Trajectory", 0)
		posX = rt.getValue("x", 0)
		posY = rt.getValue("y", 0)
		posZ = rt.getValue("z", 0)
		startFrame = rt.getValue("Frame", 0);
		frame = startFrame
		trajDistances = [] # distances per frame measured in pixels
		
		for idx in range(1, numOfRows): 
			currTrajectoryId = rt.getValue("Trajectory", idx)
			currPosX = rt.getValue("x", idx)
			currPosY = rt.getValue("y", idx)
			currPosZ = rt.getValue("z", idx)
			currFrame = rt.getValue("Frame", idx)

			# if true we are still reading data from 'current' trajectory
			if trajectoryId == currTrajectoryId:
				distance = ((currPosX - posX)**2 + (currPosY - posY)**2 + (currPosZ - posZ)**2)**(0.5)
				# since trajectory data point can "jump" over some frames (depending on link range setting) calculated distance
				# is divided to get average per one frame and added multiple time if needed (necessary for calculating correct stddev)
				noOfFrames = currFrame - frame
				distancePerFrame = distance / (currFrame - frame)
				for i in range(int(noOfFrames)):
					trajDistances.append(distancePerFrame)
					
			# if true we have read data point from next trajectory or this is very last point of data
			# calculate trajectory info
			if trajectoryId != currTrajectoryId or idx == numOfRows - 1:

				# calculate trajectory lenght
				trajDistancesTrueLen = [pixelResolution * d for d in trajDistances]
				pathLen = sum(trajDistancesTrueLen)

				# calculate length of trajectory in frames
				if idx == numOfRows - 1:
					stopFrame = currFrame
				else:
					stopFrame = frame
				numOfFrames = stopFrame - startFrame;

				# calculate velocity and std dev
				velocity = pathLen/(numOfFrames * frameInterval);
				standardDev = stddev([d/frameInterval for d in trajDistancesTrueLen])

				# if output in csv format requested print it
				if printOutputData: 
					print(str(int(trajectoryId)) + ";" + str(pathLen) + ";" + str(numOfFrames) + ";" + str(velocity) + ";" + str(standardDev)); 
				
				# if output table requested update it with data
				if showOutputTable:
					velocityRT.incrementCounter()
					velocityRT.addValue("trajectory", int(trajectoryId))
					velocityRT.addValue("pathLen", pathLen)
					velocityRT.addValue("noOfFrames", numOfFrames)
					velocityRT.addValue("velocity", velocity)				
					velocityRT.addValue("velocity stddev", standardDev)
				
				# beginning of new trajectory
				trajDistances = []
				startFrame = rt.getValue("Frame", idx)
	
				
			trajectoryId = currTrajectoryId
			posX = currPosX
			posY = currPosY
			posZ = currPosZ
			frame = currFrame
			
		velocityRT.show(outputTableName)


if __name__ in ['__builtin__','__main__']:
    runScript()
