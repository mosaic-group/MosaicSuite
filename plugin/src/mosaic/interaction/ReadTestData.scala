package mosaic.interaction

import scalala.tensor.dense.DenseMatrix
import mosaic.interaction.input.ReadMat
import scalala.tensor._
import mosaic.interaction.input.CellOutline
import ij.IJ
object ReadTestData {
	val delx:Double = 160; val delz:Double = 400; val voxDepthFactor = delz/delx ; val res:Double = 80
	
	def readOutlinefromImages(path :String): (Array[Int],(Array[Double] => Boolean)) = {
		val maskOpenMacro = "run(\"Image Sequence...\", \"open=" +path + "/3Ddata/Endosomes/Mask_2/3110.tif number=14 starting=0 increment=1 scale=100 file=[] or=[] sort\");"
		IJ.runMacro(maskOpenMacro)
		val maskLoaded = IJ.getImage()
		val outline = (new CellOutline())
		outline.setMask(maskLoaded)
		
		val isInDomain = (x:Array[Double]) => {outline.inCell(scale2Pixel(x,voxDepthFactor))}

		( Array(maskLoaded.getWidth, maskLoaded.getHeight, maskLoaded.getNSlices),isInDomain)
	}
	
	def readPositions(path :String): (Matrix,Matrix)= {
		
		val matFileName = "TestPlugin/EndVir.mat"
		
		val refGroupTmp = ReadMat.getMatrix(path + matFileName,"Endosomes3D")
		val queryGroupTmp = ReadMat.getMatrix(path + matFileName,"Viruses3D")
		val refGroup = new DenseMatrix(refGroupTmp.rows,refGroupTmp.cols)
		val queryGroup = new DenseMatrix(queryGroupTmp.rows,queryGroupTmp.cols)
		
		// exchange x and y coord.
		refGroup.getCol(0) := refGroupTmp.getCol(1);
		refGroup.getCol(1) := refGroupTmp.getCol(0);
		refGroup.getCol(2) := refGroupTmp.getCol(2);
		queryGroup.getCol(0) := queryGroupTmp.getCol(1);
		queryGroup.getCol(1) := queryGroupTmp.getCol(0);
		queryGroup.getCol(2) := queryGroupTmp.getCol(2);
		
		scale2NM(refGroup, voxDepthFactor)
		scale2NM(queryGroup, voxDepthFactor)
		(refGroup,queryGroup)
	}
	
	def readOptimTest(path :String): (Vector,Vector,Vector)= {
		
		val matFileName = "TestPlugin/optimTest.mat"
		
		val q = ReadMat.getVector(path + matFileName,"q")
		val dq = ReadMat.getVector(path + matFileName,"dq")
		val dval = ReadMat.getVector(path + matFileName,"Dval")

		(q,dq,dval)
	}
	
	def scale2NM(coord: Matrix, voxDepthFactor:Double, pixelSize:Double = 1) {
			// the upper left pixel in the first slice is (0.5,0.5,0.0) in the output of
			// the 3D tracker. Shift z component!
			val voxelDepth = voxDepthFactor * pixelSize
			coord.getCol(0) *= pixelSize; coord.getCol(1) *= pixelSize;  
			coord.getCol(2) *= voxelDepth; coord.getCol(2) += voxelDepth/2;
	}
	
	def scale2PixelMatrix(coord: Matrix, voxDepthFactor:Double, pixelSize:Double = 1) {
			// the upper left pixel in the first slice is (0.5,0.5,0.0) in the output of
			// the 3D tracker. Shift z component!
			val voxelDepth = voxDepthFactor * pixelSize
			coord.getCol(0) /= pixelSize; coord.getCol(1) /= pixelSize;  
			coord.getCol(2) -= voxelDepth/2; coord.getCol(2) /= voxelDepth; 
	}
		
	def scale2Tracker3DCoords(coord: Array[Double], voxDepthFactor:Double, pixelSize:Double = 1):Array[Double]= {
			// the upper left pixel in the first slice is (0.5,0.5,0.0) in the output of
			// the 3D tracker. Shift z component!
			val voxelDepth = voxDepthFactor * pixelSize
			Array(coord(0)/pixelSize,coord(1)/pixelSize, (coord(2)-voxelDepth/2)/voxelDepth)
	}
	
	def scale2Pixel(coord: Array[Double], voxDepthFactor:Double, pixelSize:Double = 1):Array[Double]= {
			// the upper left pixel in the first slice is (0.5,0.5,0.0) in the output of
			// the 3D tracker. Shift z component!
			val voxelDepth = voxDepthFactor * pixelSize
			Array(coord(0)/pixelSize,coord(1)/pixelSize, (coord(2))/voxelDepth)
	}
}