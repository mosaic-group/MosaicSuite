package mosaic.core.optimization

import cma.fitness.AbstractObjectiveFunction
import scalanlp.optimize._
import scalala.tensor.Vector

abstract class DiffAbstractObjectiveFunction extends AbstractObjectiveFunction with BatchDiffFunction[Int, Vector] {
  


}