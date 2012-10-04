package mosaic.region_competition.initializers;

import mosaic.region_competition.IndexIterator;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;
import mosaic.region_competition.utils.BubbleDrawer;

public class BubbleInitializer extends Initializer
{
	public BubbleInitializer(LabelImage labelImage)
	{
		super(labelImage);
	}
	
	// default values
	int radius = 5;
	int displacement = 15;
	
	
/**
 * Initializes bubbles by radius size and the gaps between the bubble center points
 * @param rad
 * @param displ
 */
	public void initSizeDispl(int rad, int displ)
	{
		int[] grid = new int[dim];
		int[] gap = new int[dim];
		for(int i = 0; i < dim; i++)
		{
			int size = dimensions[i];
			int n = (size)/displ; // how many bubbles per length
			grid[i] = n; 
			gap[i] = (size%displ+displ)/2-rad;
		}
		Point gapPoint = new Point(gap);
		
		BubbleDrawer bd = new BubbleDrawer(labelImage, rad, 2*rad);
		IndexIterator it = new IndexIterator(grid);
		int bubbleIndex = 1;
		for(Point ofs : it.getPointIterable())
		{
			// upper left startpoint of a bubble+spacing
			ofs = ofs.mult(displ).add(gapPoint); 
			// RegionIterator rit = new RegionIterator(m_LabelImage.dimensions, region, ofs.x);

			bd.drawUpperLeft(ofs, bubbleIndex);
			bubbleIndex++;
			// bd.doSphereIteration(ofs, labelDispenser.getNewLabel());
		}
	}
	
	public void initSizePaddig(int radius, int padding)
	{
		int displ=padding + 2*radius; 
		initSizeDispl(radius, displ);
	}
	
	/**
	 * 
	 * @param countWidth Number of bubbles for the width. 
	 * @param fillRatio The ratio of the diameter of the bubbles 
	 * to the distances of the midpoints of the bubbles <br>
	 * (0.0: zero size bubbles, 1.0: touching bubbles)
	 */
	public void initWidthCount(int countWidth, double fillRatio)
	{
		if(countWidth<=0)
		{
			throw new RuntimeException("Choose bubble number > 0");
		}
		
		int displ = dimensions[0]/countWidth;
		int rad = (int)(displ*fillRatio/2);
		initSizeDispl(rad, displ);
	}

	@Override
	public void initDefault()
	{
		initSizeDispl(radius, displacement);
	}
}
