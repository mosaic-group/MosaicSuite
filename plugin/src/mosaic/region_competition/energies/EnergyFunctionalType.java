package mosaic.region_competition.energies;

public enum EnergyFunctionalType
{
    e_PC{@Override
        public String toString() {return "Piecewise Constant";}},
        e_PS,
        e_DeconvolutionPC,
}
