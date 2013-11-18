package mosaic.region_competition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import mosaic.core.utils.Point;

public class ForestFire
{
	
	Algorithm algorithm;
	LabelImage labelImage;
	IntensityImage intensityImage;
	
	HashMap<Point, ContourParticle> m_InnerContourContainer;
	
	public ForestFire(Algorithm algorithm, LabelImage labelImage, IntensityImage intensityImage)
	{
		this.algorithm = algorithm;
		this.labelImage = labelImage;
		this.intensityImage = intensityImage;
		this.m_InnerContourContainer = algorithm.m_InnerContourContainer;
	}

	/**
	 * m_InnerContourContainer.remove(vCurrentCIndex);
	 */
	public void fire(Point aIndex, int aNewLabel, MultipleThresholdImageFunction aMultiThsFunctionPtr)
	{
		
//		displaySlice("pre forest fire");

		//Set<Integer> vVisitedNewLabels = new HashSet<Integer>();
		Set<Integer> vVisitedOldLabels = new HashSet<Integer>();

		FloodFill ff = new FloodFill(labelImage.getConnFG(), aMultiThsFunctionPtr, aIndex);
		Iterator<Point> vLit = ff.iterator();

		Set<Point> vSetOfAncientContourIndices = new HashSet<Point>(); // ContourIndexType

		double vSum = 0;
		double vSqSum = 0;
		int vN = 0;
//		double vLengthEnergy = 0;

        while(vLit.hasNext())
        {
			// InputPixelType vImageValue = vDit.Get();
			Point vCurrentIndex = vLit.next();
			int vLabelValue = labelImage.getLabel(vCurrentIndex);
			int absLabel = labelImage.labelToAbs(vLabelValue);
			float vImageValue = intensityImage.get(vCurrentIndex);
			
			// the visited labels statistics will be removed later.
			vVisitedOldLabels.add(absLabel);
			
			labelImage.setLabel(vCurrentIndex, aNewLabel);
			
			if(labelImage.isContourLabel(vLabelValue)) 
			{
				// m_InnerContourContainer[static_cast<ContourIndexType>(vLit.GetIndex())].first = vNewLabel;
				// ContourIndexType vCurrentIndex = static_cast<ContourIndexType>(vLit.GetIndex());
				
				vSetOfAncientContourIndices.add(vCurrentIndex);
				
			}

			vN++;
			vSum += vImageValue;
			vSqSum += vImageValue * vImageValue;

		} // while iterating over floodfill area


        /// Delete the contour points that are not needed anymore:
        for(Point vCurrentCIndex: vSetOfAncientContourIndices) 
        {
            if (labelImage.isBoundaryPoint(vCurrentCIndex)) 
            {
            	ContourParticle vPoint = m_InnerContourContainer.get(vCurrentCIndex);
            	vPoint.label = aNewLabel;
//	 			vPoint.modifiedCounter = 0;

//						vLengthEnergy += m_ContourLengthFunction->EvaluateAtIndex(vCurrentCIndex);
            	labelImage.setLabel(vCurrentCIndex, labelImage.labelToNeg(aNewLabel));
            } else {
            	m_InnerContourContainer.remove(vCurrentCIndex);
            }
        }

        /// Store the statistics of the new region (the vectors will
        /// store more and more trash of old regions).
		double vN_ = vN;

		// create a labelInformation for the new label, add to container
		LabelInformation newLabelInformation = new LabelInformation(aNewLabel);
		algorithm.labelMap.put(aNewLabel, newLabelInformation);

		newLabelInformation.mean = vSum / vN_;
		// TODO m_Intensities[vNewLabel] = m_Means[vNewLabel];
		double var = (vN_>1)?(vSqSum - vSum * vSum / vN_) / (vN_ - 1) : 0;
		newLabelInformation.var=(var);
		newLabelInformation.count = vN;
		// TODO m_Lengths[vNewLabel] = vLengthEnergy;

//		displaySlice("after forestfire");
		
		/// Clean up the statistics of non valid regions.
		for(int vVisitedIt : vVisitedOldLabels) 
		{
//			debug("Freed label " + vVisitedIt);
			algorithm.FreeLabelStatistics(vVisitedIt);
		}
		
		algorithm.CleanUp();
		
		/// TODO: this must as well be only performed for the affected
		///       regions! A call to this function may result in segmentation
		///       faults since the labelimage is not 'consistent': It may contain
		///       still regions with 'old' labels. This happens if the 'old'
		///       region has 2 topological changes at once.
		//        if (m_EnergyFunctional == e_Deconvolution) {
		//            RenewDeconvolutionStatistics(m_LabelImage, this->GetDataInput());
		//        }
		
	}
}

