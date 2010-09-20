package mosaic.interaction

import scalala.tensor._

case class Potential(name:String, function:((Vector,List[Double]) => Vector),
		nbrParam:Int, nonParamFlag:Boolean)
