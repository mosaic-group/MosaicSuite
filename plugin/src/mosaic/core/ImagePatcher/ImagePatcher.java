package mosaic.core.ImagePatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.utils.Connectivity;
import mosaic.core.utils.FloodFill;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Point;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;

/**
 * 
 * This class take two images one representing the image and one
 * representing the label, it find the connected regions, and create
 * patches.
 * 
 * 
 * 
 * @author Pietro Incardona
 *
 * @param <E>
 */

public class ImagePatcher<T extends NativeType<T> & NumericType<T>, E extends NativeType<E> & IntegerType<E>>
{
	@SuppressWarnings("rawtypes")
	ImagePatch[] img_p;
	long [] dims;
	
	public ImagePatcher(Img<T> img, Img<E> lbl, int margins[])
	{
		dims = MosaicUtils.getImageLongDimensions(img);
		
		// Create a vector of image patches
		
		Vector<ImagePatch<T,E>> img_pt = new Vector<ImagePatch<T,E>>();
		
		// Find connected regions on lbl
		
		Connectivity connFG = new Connectivity(lbl.numDimensions(),lbl.numDimensions()-1);
		LabelImage lbl_t = new LabelImage(lbl);

		// Find connected regions and create statistics
		
		HashSet<Integer> oldLabels = new HashSet<Integer>();		// set of the old labels
		ArrayList<Integer> newLabels = new ArrayList<Integer>();	// set of new labels
		
		int newLabel=1;
		
		int size=lbl_t.getSize();
		
		// what are the old labels?
		for(int i=0; i<size; i++)
		{
			int l=lbl_t.getLabel(i);
			if(l==lbl_t.bgLabel)
			{
				continue;
			}
			oldLabels.add(l);
		}
		
		for(int i=0; i<size; i++)
		{
			int l = lbl_t.getLabel(i);
			if(l == lbl_t.bgLabel)
			{
				continue;
			}
			if(oldLabels.contains(l))
			{
				// l is an old label
				BinarizedIntervalLabelImage aMultiThsFunctionPtr = new BinarizedIntervalLabelImage(lbl_t);
				aMultiThsFunctionPtr.AddThresholdBetween(l, l);
				FloodFill ff = new FloodFill(connFG, aMultiThsFunctionPtr, lbl_t.iterator.indexToPoint(i));
				
				//find a new label
				while(oldLabels.contains(newLabel))
				{
					newLabel++;
				}
				
				// newLabel is now an unused label
				newLabels.add(newLabel);
				
				img_pt.add(new ImagePatch<T,E>(margins.length));
				
				ImagePatch<T,E> ip = img_pt.get(img_pt.size()-1);
				
				// set region to new label
				for(Point p:ff)
				{
					lbl_t.setLabel(p, newLabel);
					
					// check and extend the border
					
					ip.extendPoint(p);
				}
				// next new label
				newLabel++;
			}
		}
		
		// Add margins for all patches and create patch
		
		for (int i = 0 ; i < img_pt.size() ; i++)
		{
			img_pt.get(i).SubToP1(margins);
			img_pt.get(i).AddToP2(margins);
			img_pt.get(i).createPatch(img,lbl);
		}
		
		// create an array
		
		img_p = new ImagePatch[img_pt.size()];
		
		for (int i = 0 ; i < img_p.length ; i++)
		{
			img_p[i] = img_pt.get(i);
		}
	}
	
	/**
	 * 
	 * Write the patch on the image
	 * 
	 * @param img Image
	 * @param pt Patch
	 */
	
	private void writeOnImage(Img<E> img, ImagePatch<T,E> pt)
	{
		RandomAccess<E> randomAccess = img.randomAccess();
		
		Point offset = pt.getP1();
		Cursor<E> cur = pt.getResult().cursor();
		Point p = new Point(img.numDimensions());
		
		while (cur.hasNext())
		{
			cur.next();
			cur.localize(p.x);
			
			Point psum = offset.add(p);
			
			randomAccess.setPosition(psum.x);
			randomAccess.get().set(cur.get());
		}
	}
	
	/**
	 * 
	 * Assemble the final image fromthe patches
	 * 
	 * @param cls
	 * @param start from the patch start untill the end
	 * 
	 */
	
	@SuppressWarnings("unchecked")
	public Img<E> assemble(Class<E> cls, int start)
	{
		ImgFactory< E > imgFactory_lbl = new ArrayImgFactory< E >( );;
		Img<E> img_ass = null;
		
		try {
			img_ass = imgFactory_lbl.create(dims, cls.newInstance());
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = start ; i < img_p.length ; i++)
		{
			writeOnImage(img_ass,img_p[i]);
		}
		
		return img_ass;
	}
	
	@SuppressWarnings("rawtypes")
	public ImagePatch[] getPathes()
	{
		// Get the patches back
		
		return img_p;
		
	}
}

