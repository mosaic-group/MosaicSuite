package mosaic.region_competition.wizard;

import ij.ImagePlus;
import ij.gui.ImageCanvas;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;

public class PickRegion implements MouseListener
{
	ImagePlus ip;
	int offX;
	int offY;
	Vector<Point> aC;
	
	public PickRegion(ImagePlus img)
	{
		ImageCanvas canvas = img.getWindow().getCanvas();
		canvas.addMouseListener(this);
		ip =  img;
		aC = new Vector<Point>();
	}
		
	@Override
	public void mouseClicked(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
			
	}

	@Override
	public void mouseExited(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
			
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		// TODO Auto-generated method stub
			
		int x = e.getX();
		int y = e.getY();
		offX = ip.getWindow().getCanvas().offScreenX(x);
		offY = ip.getWindow().getCanvas().offScreenY(y);

		Point p = new Point(2);
		p.x[0] = offX;
		p.x[1] = offY;
		
		aC.add(p);
		
//		int size[] = ip.getDimensions();

//		id = ip.getLabel(offscreenX+offscreenY*size[0]);
	}

	@Override
	public void mouseReleased(MouseEvent arg0) 
	{
			// TODO Auto-generated method stub
			
	}
	
	public Vector<Point> getClick()
	{
		return aC;
	}
}
