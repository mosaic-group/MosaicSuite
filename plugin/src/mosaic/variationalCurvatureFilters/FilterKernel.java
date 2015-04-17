package mosaic.variationalCurvatureFilters;

public interface FilterKernel {
    /*
     * Naming:
     * 
     *       lu | u | ru
     *       ---+---+---
     *       l  | m |  r
     *       ---+---+---
     *       ld | d | rd
     *       
     *       @returns Middle pixel change value
     */
    float filterKernel(float lu, float u, float ru, float l, float m, float r, float ld, float d, float rd);
}
