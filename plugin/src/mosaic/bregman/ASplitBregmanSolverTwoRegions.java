package mosaic.bregman;

abstract class ASplitBregmanSolverTwoRegions extends ASplitBregmanSolver {

    final int l = 0; // use mask etc of level 0

    public ASplitBregmanSolverTwoRegions(Parameters params, double[][][] image, double[][][][] mask, MasksDisplay md, int channel, AnalysePatch ap) {
        super(params, image, mask, md, channel, ap);
    }
}
