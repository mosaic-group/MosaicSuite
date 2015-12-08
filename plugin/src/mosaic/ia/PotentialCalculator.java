package mosaic.ia;

import mosaic.ia.Potential.PotentialType;

class PotentialCalculator {

    public double[] getGibbsPotential() {
        return gibbspotential;
    }

    public double[] getPotential() {
        return potential;
    }

    public double getSumPotential() {
        return sumPotential;
    }

    private double[] potential;
    private double[] gibbspotential;
    private double sumPotential = 0;
    
    private final double[] D_sample; // distance at which P should be sampled (need not be measured D)
    private final PotentialType type;
    private double[] params;

    public PotentialCalculator(double[] D_sample, double[] params, PotentialType type) // for non parametric
    {
        this.D_sample = D_sample;
        this.params = params;
        this.type = type;
    }

    public void calculate() // only Sigma (phi) for loglikelihood , not exponentiating
    {
        gibbspotential = new double[D_sample.length];
        potential = new double[D_sample.length];
        sumPotential = 0;
        switch (type) {
            case STEP:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = Math.abs(params[1]);
                    final double epsilon = Math.abs(params[0]);
                    potential[i] = epsilon * Potential.stepPotential(D_sample[i], threshold);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case HERNQUIST:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]); // if sigma is large, z=d/sigma= small => -1/(1+z) is large => will be chosen during maximum likelihood.
                    final double epsilon = Math.abs(params[0]);
                    potential[i] = epsilon * Potential.hernquistPotential(D_sample[i], threshold, sigma);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case L1:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]);
                    final double epsilon = params[0];
                    potential[i] = epsilon * Potential.linearType1(D_sample[i], threshold, sigma);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case L2:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]);
                    final double epsilon = Math.abs(params[0]);
                    potential[i] = epsilon * Potential.linearType2(D_sample[i], threshold, sigma);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case PlUMMER:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]);
                    final double epsilon = params[0];
                    potential[i] = epsilon * Potential.plummerPotential(D_sample[i], threshold, sigma);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case NONPARAM:
                for (int i = 0; i < D_sample.length; i++) {
                    final double[] weights = params;
                    potential[i] = Potential.nonParametric(D_sample[i], weights);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;

            case COULOMB:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]);
                    final double epsilon = params[0];
                    potential[i] = epsilon * Potential.coulomb(D_sample[i], threshold, sigma);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;

            default:
                break;
        }
    }

    public void calculateWOEpsilon() // only Sigma (phi) for loglikelihood , not exponentiating
    {
        if (type != PotentialType.NONPARAM) {
            params = params.clone();
            params[0] = 1; // set epsilon to 1.0
        }
        calculate();
    }
}
