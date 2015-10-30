/**
 * This sample code is made available as part of the book "Digital Image
 * Processing - An Algorithmic Introduction using Java" by Wilhelm Burger
 * and Mark J. Burge, Copyright (C) 2005-2008 Springer-Verlag Berlin,
 * Heidelberg, New York.
 * Note that this code comes with absolutely no warranty of any kind.
 * See http://www.imagingbook.com for details and licensing conditions.
 *
 * Date: 2007/11/10
 */

package mosaic.interpolators;


import java.awt.geom.Point2D;


public class BilinearInterpolator extends PixelInterpolator {

    @Override
    public double getInterpolatedPixel(Point2D pnt) {
        final double x = pnt.getX();
        final double y = pnt.getY();
        final int u = (int) Math.floor(x);
        final int v = (int) Math.floor(y);
        final double a = x - u;
        final double b = y - v;
        final int A = ip.getPixel(u, v);
        final int B = ip.getPixel(u + 1, v);
        final int C = ip.getPixel(u, v + 1);
        final int D = ip.getPixel(u + 1, v + 1);
        final double E = A + a * (B - A);
        final double F = C + a * (D - C);
        final double G = E + b * (F - E);
        return G;
    }

}
