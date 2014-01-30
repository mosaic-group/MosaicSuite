package mosaic.plugins;


import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Vector;

public class CircleGridIntersectionCalculator {
	private float circleX, circleY, radius;
	private HashMap<Point, Vector<Point2DOnGrid>> pixelToIntersectionPointsMap;
	private HashMap<Point, Point2D.Float> pixelToCentroidMap;
	private HashMap<Point, Float> pixelToAreaMap;

	public CircleGridIntersectionCalculator() {
		this.circleX = 0f;
		this.circleY = 0f;
		this.radius = 0f;
	}
	
	public CircleGridIntersectionCalculator(float circleCenterX, float circleCenterY, float r) {
		this.circleX = circleCenterX;
		this.circleY = circleCenterY;
		this.radius = r;

		reCalculate();

	}

	public void reCalculate(){
		pixelToIntersectionPointsMap= mapPixelToIntersectionPoints(findGridCircleIntersections());
		pixelToCentroidMap = new HashMap<Point, Point2D.Float>();
		pixelToAreaMap = new HashMap<Point, Float>();
		calculateAreaAndCentroids();
		
	}

	/**
	 * Finds area and center of mass of the pixel-circle intersection for each pixel.
	 */
	public void calculateAreaAndCentroids(){

		for(Point pixel : pixelToIntersectionPointsMap.keySet()) {

			Vector<Point2DOnGrid> intersectionPoints = pixelToIntersectionPointsMap.get(pixel);
			Vector<Point2DOnGrid> polygon = calculatePolygon(pixel, intersectionPoints);
			Point2D.Float polygonCentroid = new Point2D.Float(0f,0f); //TODO
			float polygonArea = 0f;
			float pixelArea = 0f, cx = 0f, cy = 0f;
			
//			if(pixel.x == 10 && pixel.y == 10){
//				System.out.println("stop");
//			}
			
			
			// 1. get polygon area and the polygon-centroid:
			if(polygon.size() > 2) { 
				//there is only a polygon if there are 3 vertices
				polygonArea = calculatePolygonArea(polygon);
				polygonCentroid = calculatePolygonCentroid(polygon, polygonArea);
			}


			if(pixelToIntersectionPointsMap.get(pixel).size() == 2){
				// for pixel with 2 intersection points: 
				// 2. get circlecap area and intersection
				// 3. add them up
				Point2D.Float capCentroid = new Point2D.Float(0f,0f);
				float capArea = calculateCircleCapAreaAndCentroid(intersectionPoints.elementAt(0), 
						intersectionPoints.elementAt(1), capCentroid);
				pixelArea = polygonArea + capArea;
				if(pixelArea < 0.0001f){
					//the following division gets inaccurate, use one of the intersection points as centroid.
					int sum_x = 0, sum_y = 0;
					for(Point2D.Float p : polygon) {
						sum_x += p.x;
						sum_y += p.y;
					}
					cx = sum_x / polygon.size(); 
					cy = sum_y / polygon.size();
				} else {
					cx = (polygonCentroid.x * polygonArea + capCentroid.x * capArea)/(pixelArea);
					cy = (polygonCentroid.y * polygonArea + capCentroid.y * capArea)/(pixelArea);
				}
			}


			if(pixelToIntersectionPointsMap.get(pixel).size() == 4) {
				// for pixel with 4 intersection points:
				// 2. iterate through the polygon and find the first intersection point 
				//    This is, find the point after 2 succ. cornerpoints
				// 3. this point and the following are used for one circle segment (cap)
				// 4. the following 2 points are used for the second circle segment
				// 5. add the 3 terms up.	
				float cap1Area = 0f, cap2Area = 0f;
				Point2D.Float cap1Centroid = new Point2D.Float(0f,0f);
				Point2D.Float cap2Centroid = new Point2D.Float(0f,0f);
				
				boolean firstCornerPointFound = false;
				int index = 0;
				int size = polygon.size();
				while(true) {
					if(index > 20) {
						//TODO find a nice solution for wrong input
						break;
					}
					if(polygon.elementAt(index%size).onCorner){
						//grid point found
						if(firstCornerPointFound) {
							//the next 2 points are the intersection points for cap1 
							//and the following 2 points are the intersection points for cap2
							cap1Area = calculateCircleCapAreaAndCentroid(polygon.elementAt((index+1)%size), polygon.elementAt((index+2)%size), cap1Centroid);
							cap2Area = calculateCircleCapAreaAndCentroid(polygon.elementAt((index+3)%size), polygon.elementAt((index+4)%size), cap2Centroid);
							
							break;
						}else{
							//wait for the second point on the grid.
							firstCornerPointFound = true;
						}
					}else{
						firstCornerPointFound = false;
					}	
					index++;
				}
				
				
//				System.out.println("area offset (inside - outside caps): " + (capInsideArea-capOutsideArea));
				pixelArea = polygonArea + cap1Area + cap2Area;
				if(pixelArea < 0.0001f){
					//the following division gets inaccurate, use the mean of the points as centroid
					int sum_x = 0, sum_y = 0;
					for(Point2D.Float p : polygon) {
						sum_x += p.x;
						sum_y += p.y;
					}
					cx = sum_x / polygon.size();
					cy = sum_y / polygon.size();
					
				} else {
					cx = (polygonCentroid.x * polygonArea + cap1Centroid.x * cap1Area 
							+ cap2Centroid.x * cap2Area)/(pixelArea);
					cy = (polygonCentroid.y * polygonArea + cap1Centroid.y * cap1Area 
							+ cap2Centroid.y * cap2Area)/(pixelArea);
				}

			}
			pixelToCentroidMap.put(pixel, new Point2D.Float(cx,cy));
			pixelToAreaMap.put(pixel, pixelArea);
		}
	}

	/**
	 * returns all intersection points of a line. 
	 * !!! - If the line is a tangent line, the point will occure twice or not 
	 * TODO deal with numerical inacc for the Discriminant
	 * in the result
	 * @param x1 x-coordinate of point1 on the line
	 * @param y1 y-coordinate of point1 on the line
	 * @param x2 x-coordinate of point2 on the line
	 * @param y2 y-coordinate of point2 on the line
	 * @see http://mathworld.wolfram.com/Circle-LineIntersection.html for variable names explanation
	 * @return
	 */
	public Vector<Point2DOnGrid> findLineCircleIntersections(float x1, float y1, float x2, float y2) {
		Vector<Point2DOnGrid> Points = new Vector<Point2DOnGrid>(2);
		x1 = x1 - circleX; 
		x2 = x2 - circleX;
		y1 = y1 - circleY;
		y2 = y2 - circleY;

		//Begin calculations:
		float dx = x2 - x1;
		float dy = y2 - y1;
		float dr = (float) Math.sqrt(dx*dx + dy*dy);
		float D = x1*y2 - x2*y1;
		float sgn = Math.signum(dy);
		if(sgn == 0f) {
			sgn = 1; 
		}
		float Delta = radius*radius*dr*dr- D*D;

		if(Delta > 0f)	{
			Points.add(new Point2DOnGrid((float)(D*dy+sgn*dx*Math.sqrt(Delta))/(dr*dr) + circleX,
					(float)(-D*dx+Math.abs(dy)*Math.sqrt(Delta))/(dr*dr) + circleY));
			Points.add(new Point2DOnGrid((float)(D*dy-sgn*dx*Math.sqrt(Delta))/(dr*dr) + circleX,
					(float)(-D*dx-Math.abs(dy)*Math.sqrt(Delta))/(dr*dr) + circleY));		       
		}
		return Points;
	}

	public Vector<Point2DOnGrid> findGridCircleIntersections() {
		Vector<Point2DOnGrid> Points = new Vector<Point2DOnGrid>();
		for(int l = (int)circleY - (int)(radius+1); l < (int)circleY+1 +(int)radius+1; l++) {
			Vector<Point2DOnGrid> pointsOnThisLine = findLineCircleIntersections(l, 0, l, 1);
			for(Point2DOnGrid p : pointsOnThisLine) {
				p.onVerticalGridLine = true;
			}
			Points.addAll(pointsOnThisLine);
		}
		for(int l = (int)circleX - (int)(radius+1); l < (int)circleX+1 +(int)radius+1; l++) {
			Vector<Point2DOnGrid> pointsOnThisLine = findLineCircleIntersections(0, l, 1, l);
			for(Point2DOnGrid p : pointsOnThisLine) {
				p.onHorizontalGridLine = true;
			}
			Points.addAll(pointsOnThisLine);			
		}
		return Points;
	}

	public HashMap<Point, Vector<Point2DOnGrid>> mapPixelToIntersectionPoints(Vector<Point2DOnGrid> points) {

		HashMap<Point, Vector<Point2DOnGrid>> PointsOnPx = new HashMap<Point, Vector<Point2DOnGrid>>();
		for(Point2DOnGrid p : points) {
//			System.out.println("point: ("+p.x+","+p.y+")");
			//			if(p.x-(int)p.x == 0f) {
			if(p.onVerticalGridLine) {
				if (p.y-(int)p.y == 0f) { //if we're on a gridpoint:
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x-0.5f),(int)(p.y-0.5)), p);
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x+0.5f),(int)(p.y+0.5)), p);
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x-0.5f),(int)(p.y+0.5)), p);
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x+0.5f),(int)(p.y-0.5)), p);
				}else{ //else
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x-0.5f),(int)p.y), p);
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x+0.5f),(int)p.y), p);
				}
			}

			//			
			if(p.onHorizontalGridLine) {
				if (p.x-(int)p.x == 0f) { //if we're on a gridpoint:
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x-0.5f),(int)(p.y-0.5)), p);
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x+0.5f),(int)(p.y+0.5)), p);
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x-0.5f),(int)(p.y+0.5)), p);
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x+0.5f),(int)(p.y-0.5)), p);
				}else{ //else
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x),(int)(p.y-0.5f)), p);
					informPointAboutIntersection(PointsOnPx, new Point((int)(p.x),(int)(p.y+0.5f)), p);
				}
			}

		}
		return PointsOnPx;
	}

	public void informPointAboutIntersection(HashMap<Point, Vector<Point2DOnGrid>> PointsOnPx, Point pixel, Point2DOnGrid intersectionPoint){

		if(PointsOnPx.containsKey(pixel)){
			PointsOnPx.get(pixel).add(intersectionPoint);
		}
		else {
			Vector<Point2DOnGrid> v = new Vector<Point2DOnGrid>();
			v.add(intersectionPoint);
			PointsOnPx.put(pixel, v);
		}		
	}

	public float calculateCircleCapAreaAndCentroid(Point2D.Float p1, Point2D.Float p2, Point2D.Float centroid){
		float a = (float) Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
		float r = (float) (0.5f * Math.sqrt(4*radius*radius-a*a));
		//		float h = radius -r;
		float theta = (float) (2f*Math.acos(r/radius));
		float area =  (radius*radius*0.5f*theta-r*(float)Math.sqrt(radius*radius-r*r));
//		float area = 0.5f*radius*radius*(theta-(float)Math.sin(theta));
		if(theta < 0.0174) {
			//if theta is smaller than 1 degree, return one of the intersection points and 0 for the area
			centroid.x = p1.x;
			centroid.y = p1.y;
			return 0f;
		}
		float y_bar = (4f*radius*(float)Math.pow(Math.sin(theta/2f),3f))/(3f*(theta-(float)Math.sin(theta)));

		float nx = (p1.x + 0.5f * (p2.x-p1.x)) - circleX;
		float ny = (p1.y + 0.5f * (p2.y-p1.y)) - circleY;

		float n_length = (float)Math.sqrt(nx*nx+ny*ny);
		nx /= n_length;
		ny /= n_length;

		centroid.x = circleX + nx * y_bar;
		centroid.y = circleY + ny * y_bar;

		return area;

	}

	public Point2DOnGrid calculatePolygonCentroid(Vector<Point2DOnGrid> polygon, float polygonArea) {

		float cx = 0f;
		float cy = 0f;
		float xi, xii, yi, yii;
		polygon.add(polygon.elementAt(0));
		int lastIndex = polygon.size()-1;
		for(int i = 0; i < lastIndex; i++) {
			xi = polygon.elementAt(i).x;
			xii = polygon.elementAt(i+1).x;
			yi = polygon.elementAt(i).y;
			yii = polygon.elementAt(i+1).y;
			cx += (xi+xii) * (xi*yii-xii*yi);
			cy += (yi+yii) * (xi*yii-xii*yi);
		}
		polygon.remove(lastIndex);
		if(polygonArea < 0.0001f) {
			// if this happens, the following division gets inaccurate.
			// just return the center of the polygon;
			return calculatePolygonCenter(polygon);
			
		}
		cx /= (6*polygonArea);
		cy /= (6*polygonArea);
		return new Point2DOnGrid(cx, cy);
	}

	
	public Point2DOnGrid getPolygonCentroid(Vector<Point2DOnGrid> polygon) {
		return calculatePolygonCentroid(polygon, calculatePolygonArea(polygon));
	}
	
	public Point2DOnGrid calculatePolygonCenter(Vector<Point2DOnGrid> polygon) {
		int sum_x = 0, sum_y = 0;
		for(Point2D.Float p : polygon) {
			sum_x += p.x;
			sum_y += p.y;
		}
		return new Point2DOnGrid(sum_x / polygon.size(), sum_y / polygon.size());
	}

	public float calculatePolygonArea(Vector<Point2DOnGrid> polygon) {
		float sum = 0.0f;
		int lastIndex = polygon.size()-1;
		for (int i = 0; i < lastIndex; i++) {
			sum += (polygon.elementAt(i).x * polygon.elementAt(i+1).y) - (polygon.elementAt(i).y * polygon.elementAt(i+1).x);
		}
		sum += (polygon.elementAt(lastIndex).x * polygon.elementAt(0).y) - (polygon.elementAt(lastIndex).y * polygon.elementAt(0).x);
		return 0.5f * Math.abs(sum);
	}

	public Vector<Point2DOnGrid> calculatePolygon(Point pixel, Vector<Point2DOnGrid> intersectionPoints) {
		Vector<Point2DOnGrid> polygon = new Vector<Point2DOnGrid>(5);
		Point2DOnGrid A = new Point2DOnGrid(pixel.x, pixel.y);			
		Point2DOnGrid B = new Point2DOnGrid(pixel.x+1, pixel.y);
		Point2DOnGrid C = new Point2DOnGrid(pixel.x+1, pixel.y+1);
		Point2DOnGrid D = new Point2DOnGrid(pixel.x, pixel.y+1);
		A.onCorner = true;
		B.onCorner = true;
		C.onCorner = true;
		D.onCorner = true;
		A.isInsideCircle = isInside(A.x,A.y);
		B.isInsideCircle = isInside(B.x,B.y);
		C.isInsideCircle = isInside(C.x,C.y);
		D.isInsideCircle = isInside(D.x,D.y);
		
		if(intersectionPoints.size() == 2) {
			//after the 2 intersection points, start counterclockwise, if we're not successfull.			
			Point2DOnGrid intersection0 = intersectionPoints.elementAt(0);
			Point2DOnGrid intersection1 = intersectionPoints.elementAt(1);
			

			if(A.isInsideCircle)
				polygon.addElement(A);
			if(intersection0.onHorizontalGridLine && intersection0.y < A.y+.5)
				polygon.addElement(intersection0);
			if(intersection1.onHorizontalGridLine && intersection1.y < A.y+.5)
				polygon.addElement(intersection1);
			if(B.isInsideCircle)
				polygon.addElement(B);
			if(intersection0.onVerticalGridLine && intersection0.x > A.x+.5)
				polygon.addElement(intersection0);
			if(intersection1.onVerticalGridLine && intersection1.x > A.x+.5)
				polygon.addElement(intersection1);
			if(C.isInsideCircle)
				polygon.addElement(C);			
			if(intersection0.onHorizontalGridLine && intersection0.y > A.y+.5)
				polygon.addElement(intersection0);
			if(intersection1.onHorizontalGridLine && intersection1.y > A.y+.5)
				polygon.addElement(intersection1);
			if(D.isInsideCircle)
				polygon.addElement(D);
			if(intersection0.onVerticalGridLine && intersection0.x < A.x+.5)
				polygon.addElement(intersection0);
			if(intersection1.onVerticalGridLine && intersection1.x < A.x+.5)
				polygon.addElement(intersection1);							
		}
		if(intersectionPoints.size() == 4) {
//			System.out.println("4IntersectionPoints!!!!");
			Point2DOnGrid intersection0 = intersectionPoints.elementAt(0);
			Point2DOnGrid intersection1 = intersectionPoints.elementAt(1);
			Point2DOnGrid intersection2 = intersectionPoints.elementAt(2);
			Point2DOnGrid intersection3 = intersectionPoints.elementAt(3);
			

			int addCount = 0;
			Vector<Point2DOnGrid> pointsToAdd = new Vector<Point2DOnGrid>(2);	
			pointsToAdd.add(null);
			pointsToAdd.add(null);

			if(A.isInsideCircle)
				polygon.addElement(A);
			if(intersection0.onHorizontalGridLine && intersection0.y < A.y+.5) {
				pointsToAdd.set(addCount,intersection0);				
				addCount++;
			}
			if(intersection1.onHorizontalGridLine && intersection1.y < A.y+.5) {
				pointsToAdd.set(addCount,intersection1);
				addCount++;	
			}
			if(intersection2.onHorizontalGridLine && intersection2.y < A.y+.5) {
				pointsToAdd.set(addCount,intersection2);
				addCount++;
			}
			if(intersection3.onHorizontalGridLine && intersection3.y < A.y+.5) {
				pointsToAdd.set(addCount,intersection3);
				addCount++;
			}
			if(addCount==2) {
				if(pointsToAdd.elementAt(0).x < pointsToAdd.elementAt(1).x) {
					polygon.add(pointsToAdd.elementAt(0));
					polygon.add(pointsToAdd.elementAt(1));
				} else {
					polygon.add(pointsToAdd.elementAt(1));
					polygon.add(pointsToAdd.elementAt(0));
				}
			} 
			if(addCount==1){
				polygon.add(pointsToAdd.elementAt(0));
			}
			addCount = 0;
			if(B.isInsideCircle)
				polygon.addElement(B);
			if(intersection0.onVerticalGridLine && intersection0.x > A.x+.5) {
				pointsToAdd.set(addCount,intersection0);				
				addCount++;
			}
			if(intersection1.onVerticalGridLine && intersection1.x > A.x+.5) {
				pointsToAdd.set(addCount,intersection1);
				addCount++;	
			}
			if(intersection2.onVerticalGridLine && intersection2.x > A.x+.5) {
				pointsToAdd.set(addCount,intersection2);
				addCount++;
			}
			if(intersection3.onVerticalGridLine && intersection3.x > A.x+.5) {
				pointsToAdd.set(addCount,intersection3);
				addCount++;
			}
			if(addCount==2) {
				if(pointsToAdd.elementAt(0).y < pointsToAdd.elementAt(1).y) {
					polygon.add(pointsToAdd.elementAt(0));
					polygon.add(pointsToAdd.elementAt(1));
				} else {
					polygon.add(pointsToAdd.elementAt(1));
					polygon.add(pointsToAdd.elementAt(0));
				}
			}
			if(addCount==1){
				polygon.add(pointsToAdd.elementAt(0));
			}


			addCount = 0;
			if(C.isInsideCircle)
				polygon.addElement(C);			
			if(intersection0.onHorizontalGridLine && intersection0.y > A.y+.5) {
				pointsToAdd.set(addCount,intersection0);				
				addCount++;
			}
			if(intersection1.onHorizontalGridLine && intersection1.y > A.y+.5) {
				pointsToAdd.set(addCount,intersection1);
				addCount++;	
			}
			if(intersection2.onHorizontalGridLine && intersection2.y > A.y+.5) {
				pointsToAdd.set(addCount,intersection2);
				addCount++;
			}
			if(intersection3.onHorizontalGridLine && intersection3.y > A.y+.5) {
				pointsToAdd.set(addCount,intersection3);
				addCount++;
			}
			if(addCount==2) {
				if(pointsToAdd.elementAt(0).x > pointsToAdd.elementAt(1).x) {
					polygon.add(pointsToAdd.elementAt(0));
					polygon.add(pointsToAdd.elementAt(1));
				} else {
					polygon.add(pointsToAdd.elementAt(1));
					polygon.add(pointsToAdd.elementAt(0));
				}
			} 
			if(addCount==1){
				polygon.add(pointsToAdd.elementAt(0));
			}

			addCount = 0;
			if(D.isInsideCircle)
				polygon.addElement(D);
			if(intersection0.onVerticalGridLine && intersection0.x < A.x+.5) {
				pointsToAdd.set(addCount,intersection0);				
				addCount++;
			}
			if(intersection1.onVerticalGridLine && intersection1.x < A.x+.5) {
				pointsToAdd.set(addCount,intersection1);
				addCount++;	
			}
			if(intersection2.onVerticalGridLine && intersection2.x < A.x+.5) {
				pointsToAdd.set(addCount,intersection2);
				addCount++;
			}
			if(intersection3.onVerticalGridLine && intersection3.x < A.x+.5) {
				pointsToAdd.set(addCount,intersection3);
				addCount++;
			}
			if(addCount==2) {
				if(pointsToAdd.elementAt(0).y > pointsToAdd.elementAt(1).y) {
					polygon.add(pointsToAdd.elementAt(0));
					polygon.add(pointsToAdd.elementAt(1));
				} else {
					polygon.add(pointsToAdd.elementAt(1));
					polygon.add(pointsToAdd.elementAt(0));
				}
			} 
			if(addCount==1){
				polygon.add(pointsToAdd.elementAt(0));
			}

		}
		return polygon;

	}
	public boolean isInside(float x, float y){
		return (radius*radius > (x-circleX)*(x-circleX) + (y-circleY)*(y-circleY));
	}

	public float getCircleX() {
		return circleX;
	}
	public void setCircleX(float circleX) {
		this.circleX = circleX;
		reCalculate();
	}
	public float getCircleY() {
		return circleY;
	}
	public void setCircleY(float circleY) {
		this.circleY = circleY;
		reCalculate();
	}
	public float getRadius() {
		return radius;

	}
	public void setRadius(float radius) {
		this.radius = radius;
		reCalculate();
	}
	public void setCircle(float circleX, float circleY, float radius) {
		this.circleX = circleX;
		this.circleY = circleY;
		this.radius = radius;
		reCalculate();
	}

	public HashMap<Point, Vector<Point2DOnGrid>> getPixelToIntersectionPointsMap() {
		return pixelToIntersectionPointsMap;
	}

	public HashMap<Point, Point2D.Float> getPixelToCentroidMap() {
		return pixelToCentroidMap;
	}

	public HashMap<Point, Float> getPixelToAreaMap() {
		return pixelToAreaMap;
	}

	public class Point2DOnGrid extends Point2D.Float{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public boolean onVerticalGridLine = false;
		public boolean onHorizontalGridLine = false;
		public boolean onCorner = false;
		public boolean isInsideCircle = false;

		public Point2DOnGrid(float x, float y){
			super(x, y);
		}
		public Point2DOnGrid(float x, float y, boolean onX, boolean onY){
			super(x, y);
			this.onVerticalGridLine = onX;
			this.onHorizontalGridLine = onY;
		}


	}

	

}
