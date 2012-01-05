package mosaic.region_competition;

import java.util.HashMap;
import java.util.Map.Entry;

import ij.process.ImageProcessor;

/**
 * checks the consistency of the LabelImage
 * 
 * is every contour point in the container? and vice versa
 * is the label information correct?
 * 		output from LabelInformation
 * 		count again from LabelImage
 * @author Stephan
 *
 */
public class DebugConsistencyCheck
{
	
	LabelImage labelImage;
	ImageProcessor data;
	short pixels[];
	int liSize;
	
	int hist[];
	
	HashMap<Integer, LabelInformation> labelMap;
	HashMap<Point, ContourParticle> contourContainer;

	public DebugConsistencyCheck(LabelImage labelImage) 
	{
		this.labelImage=labelImage;
		
		data=labelImage.getLabelImageProcessor();
		pixels=(short[])data.getPixels();
		
		//TODO these seems to be 2D methods
		liSize=pixels.length;
		liSize=data.getPixelCount();
		
		contourContainer = labelImage.getContourContainer();
		labelMap = labelImage.getLabelMap();
		
		hist = new int[Short.MAX_VALUE];
		for(int i=0; i<hist.length; i++)
		{
			hist[i]=0;
		}
	}
	
	void checkLabels()
	{
		// check contour
		
		for(Entry<Point, ContourParticle> e : contourContainer.entrySet())
		{
			Point p = e.getKey();
			ContourParticle c = e.getValue();
			int label = c.label;
			
			if(labelImage.getAbs(p)!=label)
			{
				System.out.println("contourparticle.label != labelImage.label");
				System.out.println(label+"!="+labelImage.getAbs(p));
			}
		}
		
		//check labelimage
		
		for(int i=0; i<liSize; i++)
		{
			int label=labelImage.get(i);
			if(labelImage.isContourLabel(label))
			{
				Point p = labelImage.iterator.indexToPoint(i);
				ContourParticle c = contourContainer.get(p);
				if(c==null)
				{
					System.out.println("labelImage has contourPoint which is not in contourcontainer");
				}
				else
				{
					if(c.label!=labelImage.labelToAbs(label))
					{
						System.out.println("label in contourContainer != label in labelImage");
					}
				}
			}
		}
		
	}
	
	
	void checkHist()
	{
		getHist();
		
		for(int i=0; i<hist.length; i++)
		{
			int countHist = hist[i];
			
			if(countHist>0)
			{
				int count = labelMap.get(i).count;
				if(count != countHist)
				{
					System.out.println("labelMap.count != hist.count ");
					System.out.println("("+count+"!="+countHist+")");
					System.out.println("");
				}
			}
		}
		
		for(Entry<Integer, LabelInformation> e :labelMap.entrySet())
		{
			int label = e.getKey();
			int count = e.getValue().count;
			
			int countHist = hist[label];
			
			if(hist[label]!=count)
			{
				System.out.println("labelMap.count != hist.count ");
				System.out.println("("+count+"!="+countHist+")");
				System.out.println("");
			}
		}
	}


	private void getHist()
	{
		for(int i=0; i<liSize; i++)
		{
			int label=labelImage.getAbs(i);
			hist[label]+=1;
		}
		System.out.println(hist);
	}
	

}













