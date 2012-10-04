package mosaic.region_competition.energies;

public enum EnergyFunctionalType 
{
	e_GaussPC{public String toString() {return "Piecewise Constant";}}, 
	e_GaussPS, 
	e_GaussPS_Sphis, 
	e_GaussWithVariancePC, 
	e_MS, e_LocalCV, e_LocalLi, e_DeconvolutionPC, e_PSwithCurvatureFlow
}