package mosaic.paramopt.ui;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Roi;
import ij.util.Java2;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


public class ImageCanvas extends Canvas {

	private static final long serialVersionUID = -1975955208923504901L;

	private final int id;
	private ImagePlus imagePlus;
	private ImagePlus overlayImagePlus;
	private Image image = null;
	private Image overlayImage = null;
	private int rank = 0;
	private boolean highlighted = false;
	private double magnification = 1.0;
	private double effectiveMagnification = 1.0;
	private double canvasMagnification = 1.0;
	private float overlayOpacity = 0.0f;
	
	private Graphics bg;
	private Image canvas;
	
	private int imageWidth, imageHeight;
	private int canvasWidth, canvasHeight;
	private int border = 2;
	private double focusX = -1, focusY = -1;
	private int srcX, srcY, srcWidth, srcHeight;
	private int dstX, dstY, dstWidth, dstHeight;
	// The current slice
	private int slice = 1;
    // Flag to enable/disable the zoom indicator
	protected boolean zoomIndicatorEnabled = false;
	// Color of the zoom indicator
	protected Color zoomIndicatorColor;

	// Some display colors
//	private Color defaultColor = Color.getColor("win.frame.backgroundColor");
	private static Color defaultColor;
	private static Color highlightColor = Color.RED;
	private static Color selectedColor = new Color(255, 150, 150);
	
	static{
		// Get the default background color of a JPanel.
		defaultColor = new JPanel().getBackground();
		defaultColor = new Color(defaultColor.getRed(), defaultColor.getGreen(),
				defaultColor.getBlue());
	}

	public ImageCanvas(int id) {
		this(null, id);
	}
	
	public ImageCanvas(ImagePlus imp, int index) {
		setImage(imp);
		this.id = index;
	}

	public int getId() {
		return id;
	}

	/**
	 * Sets the rank of the image.
	 * 
	 * @param rank
	 *           the new rank of the image.
	 */
	public void setRank(int rank) {
		this.rank = rank;
		repaint();
	}
	
	public int getRank() {
		return rank;
	}
	
	/**
	 * Sets if the zoom indicator is being drawn.
	 * 
	 * @param enabled
	 *            flag for whether the zoom indicator is to be drawn or not
	 */
	public void setZoomIndicatorEnabled(boolean enabled) {
		zoomIndicatorEnabled = enabled;
	}
	
	/**
	 * Sets the ImagePlus resource.
	 * 
	 * @param imp the ImagePlus resource for the canvas
	 */
	public void setImage(ImagePlus imp) {
		imagePlus = imp;
		if (imagePlus != null) {
			imagePlus.setSlice(slice);
			image = imagePlus.getImage();
			imageWidth = imagePlus.getWidth();
			imageHeight = imagePlus.getHeight();
			if (focusX < 0 || focusY < 0) {
				focusX = imageWidth / 2;
				focusY = imageHeight / 2;
			}
		}
	}
	
	/**
	 * Sets the overlay image resource.
	 * 
	 * @param img the image resource for the overlay
	 */
	public void setOverlayImage(ImagePlus imp) {
		overlayImagePlus = imp;
		if (overlayImagePlus != null) {
			overlayImagePlus.setSlice(slice);
			overlayImagePlus.setProcessor(overlayImagePlus.getStack()
					.getProcessor(slice));
			overlayImage = overlayImagePlus.getImage();
		}
	}
	
	@Override
	public void setBounds(int x, int y, int width, int height) {
		// Ignore calls before layout is correctly set.
		if (width < 1 || height < 1)
			return;
		super.setBounds(x, y, width, height);
		canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		bg = canvas.getGraphics();
		// Get the canvas dimensions.
		canvasWidth = width;
		canvasHeight = height;
		// Compute the canvas magnification.
		double magX = (double) canvasWidth / (double) imageWidth;
		double magY = (double) canvasHeight / (double) imageHeight;
		if (magX < magY)
			canvasMagnification = magX;
		else
			canvasMagnification = magY;
		// Compute the effective magnification of the image.
		effectiveMagnification = canvasMagnification * magnification;
		updateSrc();
		repaint();
	}
	
	/**
	 * Updates the source region of the image.
	 */
	private void updateSrc() {
		if (imagePlus == null)
			return;
		// Compute the width and height of the source region.
		srcWidth = (int) ((double) canvasWidth / effectiveMagnification); 
		srcHeight = (int) ((double) canvasHeight / effectiveMagnification);
		if (srcWidth > imageWidth) {
			srcWidth = imageWidth;
			dstWidth = (int) Math.ceil(srcWidth * effectiveMagnification);
		} else {
			dstWidth = canvasWidth;
		}
		if (srcHeight > imageHeight) {
			srcHeight = imageHeight;
			dstHeight = (int) Math.ceil(srcHeight * effectiveMagnification);
		} else {
			dstHeight = canvasHeight;
		}
		// Compute the offset of the source region.
		srcX = (int) (focusX - (srcWidth / 2));
		srcY = (int) (focusY - (srcHeight / 2));
		if (srcX < 0) {
			focusX -= srcX;
			srcX = 0;
		} else if (srcX + srcWidth > imageWidth) {
			focusX -= srcX + srcWidth - imageWidth;
			srcX = imageWidth - srcWidth;
		}
		if (srcY < 0) {
			focusY -= srcY;
			srcY = 0;
		} else if (srcY + srcHeight > imageHeight) {
			focusY -= srcY + srcHeight - imageHeight;
			srcY = imageHeight - srcHeight;
		}
		// Compute the destination region.
		dstX = (canvasWidth - dstWidth) / 2 + border;
		dstY = (canvasHeight - dstHeight) / 2 + border;
		dstWidth -= 2 * border;
		dstHeight -= 2 * border;
	}
	
	@Override
	public void update(Graphics g) {
		paint(g);
	}
	
	@Override
	public void paint(Graphics g) {
		Java2.setBilinearInterpolation(bg, Prefs.interpolateScaledImages);
		((Graphics2D) bg).setComposite(AlphaComposite
				.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		bg.setColor(defaultColor);
		bg.fillRect(0, 0, canvasWidth, canvasHeight);
		if (highlighted)
			bg.setColor(highlightColor);
		else if (rank > 0)
			bg.setColor(selectedColor);
		else
			bg.setColor(defaultColor);
		bg.drawRect(0, 0, canvasWidth-1, canvasHeight-1);
		bg.drawRect(1, 1, canvasWidth-3, canvasHeight-3);
		bg.setColor(defaultColor);
		if (rank > 0 || highlighted) {
			// TODO: Draw the background color for the border.
		}
		if (image != null) {
			// Draw the image.
			bg.drawImage(image, dstX, dstY, dstX + dstWidth, dstY + dstHeight,
					srcX, srcY, srcX + srcWidth, srcY + srcHeight, null);
//			if (dstX > border)
//				g.fillRect(border, border, dstX - border, 
//						canvasHeight - 2 * border);
//			if (dstX + dstWidth < (canvasWidth - 1) - border)
//				g.fillRect(dstX + dstWidth, border, 
//						canvasWidth - (border + dstX + dstWidth), 
//						canvasHeight - 2 * border);
//			if (dstY > border)
//				g.fillRect(dstX, border, dstWidth, dstY - border);
//			if (dstY + dstHeight < canvasHeight - border)
//				g.fillRect(dstX, dstY + dstHeight, dstWidth, 
//						canvasHeight - (border + dstY + dstHeight));
		}
		drawOverlayImage(bg);
		if (rank > 0) {
			// TODO: Draw the rank.
			((Graphics2D) bg).setComposite(AlphaComposite
					.getInstance(AlphaComposite.SRC_OVER, 0.8f));
			if (highlighted) {
				bg.setColor(highlightColor);
				bg.setFont(bg.getFont().deriveFont(Font.BOLD, 30));
				bg.drawString(Integer.toString(rank), canvasWidth - 23 , 30);
			} else {
				bg.setColor(selectedColor);
				bg.setFont(bg.getFont().deriveFont(Font.BOLD, 20));
				bg.drawString(Integer.toString(rank), canvasWidth - 20 , 25);
			}
		}
		// Draw the zoom indicator if it is enabled.
		drawZoomIndicator(bg);
		g.drawImage(canvas, 0, 0, canvasWidth, canvasHeight, null);
	}

	/**
	 * Draws the overlay image if it has been set with the opacity defined by 
	 * <code>overlayOpacity</code>.
	 * 
	 * @param g the graphics context used to draw this image
	 */
	protected final void drawOverlayImage(Graphics g) {
		// Check if an overlay is defined and if so try to get its image and
		// draw it.
		if (overlayImage == null)
			return;
		Graphics2D g2d = (Graphics2D) g;
		// Set the overlay opacity.
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				overlayOpacity));
		// Draw the region of the image defined by drawingSource.
		g2d.drawImage(overlayImage, dstX, dstY, dstX + dstWidth, dstY + dstHeight,
				srcX, srcY, srcX + srcWidth, srcY + srcHeight, null);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
	}
	
	/**
	 * If just a part of the image is drawn and the zoom indicator is enabled
	 * then a small zoom indicator is drawn in the top left corner of the
	 * canvas.
	 * This implementation has been copied from the corresponding method in
	 * <code>ImageCanvas</code>.
	 * 
	 * @param g the graphics context used to draw upon
	 */
	protected final void drawZoomIndicator(Graphics g) {
		// If zoom indicator is disabled or if the whole image is drawn then
		// the zoom indicator will not be drawn.
		if (magnification <= 1.0 || !zoomIndicatorEnabled )
			return;
		int x1 = 10;
		int y1 = 10;
		double aspectRatio = (double) imageHeight / (double) imageWidth;
		int w1 = 64;
		if (aspectRatio > 1.0)
			w1 = (int) (w1 / aspectRatio);
		int h1 = (int) (w1 * aspectRatio);
		if (w1 < 4)
			w1 = 4;
		if (h1 < 4)
			h1 = 4;
		int w2 = (int) (w1 * ((double) srcWidth / imageWidth));
		int h2 = (int) (h1 * ((double) srcHeight / imageHeight));
		if (w2 < 1)
			w2 = 1;
		if (h2 < 1)
			h2 = 1;
		int x2 = (int) (w1 * ((double) srcX / imageWidth));
		int y2 = (int) (h1 * ((double) srcY / imageHeight));
		if (zoomIndicatorColor == null)
			zoomIndicatorColor = new Color(128, 128, 255);
		g.setColor(zoomIndicatorColor);
		((Graphics2D) g).setStroke(Roi.onePixelWide);
		g.drawRect(x1, y1, w1, h1);
		if (w2 * h2 <= 200 || w2 < 10 || h2 < 10)
			g.fillRect(x1 + x2, y1 + y2, w2, h2);
		else
			g.drawRect(x1 + x2, y1 + y2, w2, h2);
	}
	
	/**
	 * Sets the magnification of the image.
	 * 
	 * @param mag the new magnification of the image
	 */
	public void setMagnification(double mag) {
		// Set the magnification.
		magnification = mag;
	}
	
	/**
	 * Sets the opacity of the overlay.
	 * 
	 * @param opacity
	 *            the new opacity for the overlay
	 */
	public void setOverlayOpacity(float opacity) {
		// Check if the opacity is within the interval [0,1] and if it is not
		// then set it to the closer interval bound.
		if (opacity < 0.0f)
			opacity = 0.0f;
		else if (opacity > 1.0f)
			opacity = 1.0f;
		// Set the opacity.
		overlayOpacity = opacity;
		repaint();
	}
	
	/**
	 * Moves the image by the specified amount in screen coordinates.
	 * 
	 * @param x
	 * @param y
	 */
	public void panBy(int x, int y) {
		double oldFocusX = focusX;
		double oldFocusY = focusY;
		focusX += x / effectiveMagnification;
		focusY += y / effectiveMagnification;
		updateSrc();
		if ((int) oldFocusX != (int) focusX || (int) oldFocusY != (int) focusY)
			repaint();
	}

	public void setHighlighted(boolean b) {
		highlighted = b;
	}

	// TODO: Write JavaDoc for this method!
	public void setSlice(int slice) {
		if (imagePlus == null)
			return;
		// If slice is already set do nothing.
		if (slice == imagePlus.getCurrentSlice())
			return;
		// Check if slice is within bounds.
		if (slice < 1)
			slice = 1;
		else if (slice > imagePlus.getStackSize())
			slice = imagePlus.getStackSize();
		// Set the new slice
		this.slice = slice;
		imagePlus.setSlice(slice);
		imagePlus.setProcessor(imagePlus.getStack().getProcessor(slice));
		image = imagePlus.getImage();
		if (overlayImagePlus != null) {
			if (slice > overlayImagePlus.getStackSize())
				slice = overlayImagePlus.getStackSize();
			overlayImagePlus.setSlice(slice);
			overlayImagePlus.setProcessor(overlayImagePlus.getStack()
					.getProcessor(slice));
			overlayImage = overlayImagePlus.getImage();
		}
		// Repaint the canvas with the new slice.
		repaint();
	}
	
	public double screenToImageX(int x) {
		return srcX + (double) x / effectiveMagnification;
	}
	
	public double screenToImageY(int y) {
		return srcY + (double) y / effectiveMagnification;
	}

	public void setFocus(double x, double y) {
		focusX = x;
		focusY = y;
	}


}
