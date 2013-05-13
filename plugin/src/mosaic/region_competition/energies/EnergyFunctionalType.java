package mosaic.region_competition.energies;

public enum EnergyFunctionalType 
{
	e_PC{public String toString() {return "Piecewise Constant";}}, 
	e_PS,
	e_DeconvolutionPC,
}
