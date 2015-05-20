package mosaic.plugins;

import java.util.Arrays;

import javax.vecmath.Point3f;

import net.jgeom.nurbs.ControlNet;
import net.jgeom.nurbs.ControlPoint4f;
import net.jgeom.nurbs.NurbsSurface;
import net.jgeom.nurbs.util.InterpolationException;
import net.jgeom.nurbs.util.NurbsCreator;
import ij.IJ;
import ij.process.FloatProcessor;
import mosaic.plugins.utils.PlugInFloatBase;

public class FilamentSegmentation extends PlugInFloatBase {
	@Override
	protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
		IJ.log("Hello world!");
		int nx = 30;
		int ny = 40;
		int dist=2;
		Point3f points[][] = new Point3f[nx/dist][ny/dist];

		for (int x = 0; x < nx; x+=dist) 
			for(int y = 0; y < ny; y+=dist) {
				float val = 0; 
				float xv = x + 1 + (30f/28)*x/(nx);
				float yv = y + 1 + (40f/38)*y/(ny);
				val = (float) (nx/3.0f - Math.sqrt((xv-nx/3.0f)*(xv-nx/3.0f) + (yv-ny/3.0f)*(yv-ny/3.0f)));
				points[x/dist][y/dist] = new Point3f((float)xv, (float)yv, val);
			}
		try {
			NurbsSurface ns = NurbsCreator.globalSurfaceInterpolation(points, 3,3);
		 
			ControlNet cn = ns.getControlNet(); 
			int uLength = cn.uLength();
			int vLength = cn.vLength();

			float max=0.0f;
			for (int u = 0; u < uLength; u++)
				for (int v = 0; v < vLength; v++)  {
					ControlPoint4f p = cn.get(u, v);
					if (Math.abs(p.z) > max) max = Math.abs(p.z);
				}
			
			for (int u = 0; u < uLength; u++)
			for (int v = 0; v < vLength; v++)  {
				ControlPoint4f p = cn.get(u, v);
				p.z = 2.0f * p.z/max;
  
			}
			
			for (int u = 0; u < uLength; u++) {
				for (int v = 0; v < vLength; v++) 
					System.out.print(cn.get(u, v).z + " ");
				System.out.println();
			}
			
			float[] uKnots = ns.getUKnots(); 
			float[] uk = new float[uKnots.length];
			for (int i =0; i < uKnots.length; ++i) uk[i] = ns.pointOnSurface(uKnots[i], 0).x;
			System.out.println(uk.length);
			System.out.println(Arrays.toString(uk));
			
			float[] uKnots2 = ns.getUKnots(); 
			float[] uk2 = new float[uKnots2.length];
			for (int i =0; i < uKnots2.length; ++i) uk2[i] = (uKnots[i]+1f/29)*(29f);
			System.out.println(uk2.length);
			System.out.println(Arrays.toString(uk2));
			
			float[] vKnots = ns.getVKnots(); 
			float[] vk = new float[vKnots.length];
			for (int i =0; i < vKnots.length; ++i) vk[i] = ns.pointOnSurface(0, vKnots[i]).y;
			System.out.println(vk.length);
			System.out.println(Arrays.toString(vk));
			
			
			
			float step = 0.015f;
			String xv = "\n\nx = [";
			for (float x = 0.0f; x < 1.0f + step/2; x+= step) {xv += x; xv+= " ";} xv+="];";
			System.out.println(xv);
			xv = "y = [";
			for (float x = 0.0f; x < 1.0f + step/2; x+= step) {xv += x; xv+= " ";} xv+="];";
			System.out.println(xv);
			String s = "\nz=[";
			for (float x=0.0f; x <= 1.0f + step/2; x+= step) {
				for (float y=0.0f; y <= 1.0f + step/2; y+= step) { 
					s += ns.pointOnSurface(x,y).z;
					s += " ";
				}
				s += ";";
			}
			s+= "];\nsurf(x,y,z);";			
			System.out.println(s);
			IJ.log(s);
			System.out.println(ns.pointOnSurface(0, 0));
			System.out.println(ns.pointOnSurface(-1,-1));
			System.out.println(ns.pointOnSurface(0, 0) + " " + ns.pointOnSurface(1.0f, 1.0f));
		} catch (InterpolationException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected boolean showDialog() {
		return true;
	}

	@Override
	protected boolean setup(String aArgs) {
		return true;
	}



}
