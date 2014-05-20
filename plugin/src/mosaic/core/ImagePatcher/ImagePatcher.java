package mosaic.core.ImagePatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

import mosaic.core.binarize.BinarizedIntervalLabelImage;
import mosaic.core.utils.Connectivity;
import mosaic.core.utils.FloodFill;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.Point;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;

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

public class ImagePatcher<E extends NativeType<E> & NumericType<E>, T extends NativeType<T> & IntegerType<T>>
{
	ImagePatch[] img_p;
	
	public ImagePatcher(Img<E> img, Img<T> lbl, int margins[])
	{
		// Create a vector of image patches
		
		Vector<ImagePatch<E,T>> img_pt = new Vector<ImagePatch<E,T>>();
		
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
				
				img_pt.add(new ImagePatch<E,T>(margins.length));
				ImagePatch<E,T> ip = img_pt.get(img_pt.size()-1);
				
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
	
	public ImagePatch[] getPathes()
	{
		// Get the patches back
		
		return img_p;
		
	}
}

