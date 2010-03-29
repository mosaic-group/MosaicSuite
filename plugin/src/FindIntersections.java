
import java.awt.Point;


public class FindIntersections {
	static int width = 20;
	static float circleX = 10.5f;
	static float circleY = 10.5f;
	static float radius = 2.57f;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		CircleGridIntersectionCalculator ip = new CircleGridIntersectionCalculator(circleX, circleY, radius);

		//		HashMap<Point, Vector<Point2D.Float>> pixelToPolygonMap = new HashMap<Point, Vector<Point2D.Float>>();
		//		
		//		for(Point pixel : pixelToIntersectionPointsMap.keySet()) {
		//			pixelToPolygonMap.put(pixel, ip.getPolygon(pixel, pixelToIntersectionPointsMap.get(pixel)));			
		//		}



		for(Point pixel : ip.getPixelToAreaMap().keySet()) {
			System.out.println("Pixel: " + pixel.toString() + "has centroid: " 
					+ ip.getPixelToCentroidMap().get(pixel) + " and an area of : " 
					+ ip.getPixelToAreaMap().get(pixel));
		}
		//		Vector<Point2D.Float> test = new Vector<Point2D.Float>();
		//		test.add(new Point2D.Float(0.0f,0.0f));
		//		test.add(new Point2D.Float(1.0f,0.0f));
		//		test.add(new Point2D.Float(1.0f,1.0f));
		//		test.add(new Point2D.Float(0.0f,1.0f));
		//		System.out.println("Polygon centroid: " + ip.getPolygonCentroid(test).toString());

		//output:
		//		for(Point pixel : pixelToPolygonMap.keySet()) {
		//			System.out.println("Pixel " + pixel.toString() + "\n" + pixelToPolygonMap.get(pixel).toString());
		//		}
		//		System.out.println(points.size() + " points found");
		//		for(Point pixel : PointsOnPx.keySet()) {
		//			System.out.println("Pixel " + pixel.toString() + " with " + PointsOnPx.get(pixel).size() + " intersection points:");
		//			for(Point2DOnGrid p : PointsOnPx.get(pixel)){
		//				System.out.println(p.toString());
		//			}
		//		}
	}

	public static int getXCoordFromIndex(int index, int width) {
		return index%width;
	}

	public static int getYCoordFromIndex(int index, int width) {
		return (int)(index/width);
	}

	public static int getCoord(int x, int y, int width){
		return width*y+x;
	}


}
