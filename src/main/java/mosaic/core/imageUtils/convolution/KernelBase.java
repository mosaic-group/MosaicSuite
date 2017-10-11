package mosaic.core.imageUtils.convolution;


public abstract class KernelBase {
        public int iHalfWidth = 0;       // half width of the kernel.
        
        /**
         * @param aWidth - width of kernel in some directino
         * @return middle index of kernel with 'Matlab style', for example:
         *         if filter has width = 3  [-1 2 -1] it will return 1
         *         if filter has widht = 4  [-1 1 1 -1] it will return 2
         */
        public int getCenterIndex(int aWidth) { return ((aWidth + 2) / 2) - 1; }
}
