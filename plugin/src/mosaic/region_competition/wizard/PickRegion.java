package mosaic.region_competition.wizard;


import ij.ImagePlus;
import ij.gui.ImageCanvas;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import mosaic.core.utils.Point;


public class PickRegion implements MouseListener {

    ImagePlus ip;
    int offX;
    int offY;
    Vector<Point> aC;

    public PickRegion(ImagePlus img) {
        final ImageCanvas canvas = img.getWindow().getCanvas();
        canvas.addMouseListener(this);
        ip = img;
        aC = new Vector<Point>();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {

    }

    @Override
    public void mouseEntered(MouseEvent arg0) {

    }

    @Override
    public void mouseExited(MouseEvent arg0) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

        final int x = e.getX();
        final int y = e.getY();
        offX = ip.getWindow().getCanvas().offScreenX(x);
        offY = ip.getWindow().getCanvas().offScreenY(y);

        final Point p = new Point(2);
        p.x[0] = offX;
        p.x[1] = offY;

        aC.add(p);

        // int size[] = ip.getDimensions();

        // id = ip.getLabel(offscreenX+offscreenY*size[0]);
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {

    }

    public Vector<Point> getClick() {
        return aC;
    }
}
