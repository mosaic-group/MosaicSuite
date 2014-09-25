package mosaic.bregman;

import java.util.Iterator;

public class DistanceAnalysis {



	public static void computeCenterPosition(Region r){


		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {	
			//Pix px = it.next();

		}
	}


	public static double findMinDistance(Region r, double [][][] distmap){
		double dist=Math.sqrt(Math.pow(Analysis.p.ni, 2)+ Math.pow(Analysis.p.nj,2)+Math.pow(Analysis.p.nz,2));
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {	
			Pix px = it.next();
			if(distmap[px.pz][px.px][px.pz] < dist) dist=distmap[px.pz][px.px][px.pz];
		}
		return dist;
	}




}
