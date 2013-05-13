package junit.bregman;

import static org.junit.Assert.*;
import org.junit.Test;
import mosaic.bregman.Analysis;

public class JTests {
	//local intensity tests valid with weka classifier only
	
	@Test
	public void matlabEnergyEquality2DPSF() {
		//matlab output(test_2DPSF.m):  iteration 11 -- Energy: 1.424505e+03  
		assertEquals(1.424505e+03, Tests.TestEnergy2DPSF(), 1e-3);
	}
	
	@Test
	public void matlabEnergyEquality_5levelswoPSF() {
		//matlab output(test_5levels_woPSF.m)  iteration  11 -- Energy: 8.400318e+03   
		assertEquals(8.400318e+03, Tests.TestEnergy2D_5levelswoPSF(), 1e-2);
	}	
	
	
	@Test
	public void objectsNumbers2DPSF_firstphase() {
		Tests.TestObjects2DPSF_firstphase();
		assertEquals(66, Analysis.na);
		assertEquals(285, Analysis.regionslistA.get(0).points);
	}
	
	@Test
	public void objects_Numbers_size_perimeter_2DPSF_secondphase() {
		double meanP=Tests.TestObjects2DPSF_secondphase();
		assertEquals(67, Analysis.na);//number of objects found
		assertEquals(52.5223, Analysis.meana,1e-4);//mean size
		assertEquals(28.5373, meanP, 1e-4);//mean perimeter
	}
	
	
	@Test
	public void objects_Numbers_size_perimeter_2DPSF_secondphase_subpixel() {
		Tests.TestObjects2DPSF_secondphase_subpixel();
		assertEquals(64, Analysis.na);//number of objects found
		assertEquals(55.1262, Analysis.meana,1e-4);//mean size
		assertEquals(29.2851, Tests.meanSA, 1e-4);//mean perimeter
		assertEquals(6.2636, Tests.meanLA, 1e-4);//mean perimeter
	}
	
	@Test
	public void coloc_2DPSF() {
		Tests.TestColoc2DPSF();
		assertEquals(0.1139, Tests.colocAB, 1e-4);//number of objects found
		assertEquals(0.0439, Tests.colocABnumber, 1e-4);//number of objects found
		assertEquals(0.8675, Tests.colocBA, 1e-4);//number of objects found
		assertEquals(0.7666, Tests.colocBAnumber, 1e-4);//number of objects found
		//assertEquals(52.5223, Analysis.meana,1e-4);//mean size
		//assertEquals(28.5373, meanP, 1e-4);//mean perimeter
	}
	
	@Test
	public void objects_Numbers_size_perimeter_2DPSF_secondphase_low() {
		Tests.TestObjects2DPSF_secondphase_low();
		assertEquals(66, Analysis.na);//number of objects found
		assertEquals(55.1818, Analysis.meana,1e-4);//mean size
		assertEquals(29.8787, Tests.meanSA, 1e-4);//mean perimeter
		assertEquals(6.3787, Tests.meanLA, 1e-4);//mean perimeter
	}
	
//	@Test
//	public void objects_Numbers_size_perimeter_2DPSF_secondphase_medium() {
//		Tests.TestObjects2DPSF_secondphase_medium();
//		assertEquals(70, Analysis.na);//number of objects found
//		assertEquals(39.0285, Analysis.meana,1e-4);//mean size
//		assertEquals(24.5428, Tests.meanSA, 1e-4);//mean perimeter
//		assertEquals(4.6142, Tests.meanLA, 1e-4);//mean perimeter
//	}
//	
//	
//	@Test
//	public void objects_Numbers_size_perimeter_2DPSF_secondphase_high() {
//		Tests.TestObjects2DPSF_secondphase_high();
//		assertEquals(81, Analysis.na);//number of objects found
//		assertEquals(18.6666, Analysis.meana,1e-4);//mean size
//		assertEquals(19.3086, Tests.meanSA, 1e-4);//mean perimeter
//		assertEquals(4.4320, Tests.meanLA, 1e-4);//mean perimeter
//	}
	
	@Test
	public void objects_Numbers_size_perimeter_3DPSF() {
		Tests.TestObjects3DPSF();
		assertEquals(139, Analysis.na);//number of objects found
		assertEquals(61.5539, Analysis.meana,1e-4);//mean size
		assertEquals(101.741, Tests.meanSA, 1e-4);//mean perimeter
		assertEquals(6.3812, Tests.meanLA, 1e-4);//mean perimeter
	}
	
	
	@Test
	public void objectsNumbers2DPSF_oldmode_before_local_estimation() {
		Tests.TestObjects2DPSF_oldmode();
		assertEquals(34, Analysis.na);
		assertEquals(92.88, Analysis.meana,1e-2);
	}
	
//	@Test
//	public void objectsNumbers2DPSFAdaptiveThresholds_subpixel() {
//		Tests.TestObjects2DPSFadaptive_subpixel();
//		assertEquals(90, Analysis.na);
//		assertEquals(41.47, Analysis.meana,1e-2);
//		assertEquals(80.91, Analysis.meansize_refined,1e-2);
//	}
//	
//	@Test
//	public void objectsColocalization2DPSFAdaptiveThresholds() {	
//		assertEquals(0.875, Tests.TestColocBA(),1e-3);
//		assertEquals(21,Analysis.positiveB);
//		assertEquals(24,Analysis.nb);
//	}
	
	//tests for 3D cases : matlab equality, objects numbers, adaptive thresholds, colocalization
	//2D test with region statistics solver (energy and intensity)
	


}
