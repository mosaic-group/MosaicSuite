package mosaic.ia;


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
    private final int type;
    private double[] params;

    public PotentialCalculator(double[] D_sample, double[] params, int type) // for non parametric
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
            case PotentialFunctions.STEP:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = Math.abs(params[1]);
                    final double epsilon = Math.abs(params[0]);
                    potential[i] = epsilon * PotentialFunctions.stepPotential(D_sample[i], threshold);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case PotentialFunctions.HERNQUIST:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]); // if sigma is large, z=d/sigma= small => -1/(1+z) is large => will be chosen during maximum likelihood.
                    final double epsilon = Math.abs(params[0]);
                    potential[i] = epsilon * PotentialFunctions.hernquistPotential(D_sample[i], threshold, sigma);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case PotentialFunctions.L1:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]);
                    final double epsilon = params[0];
                    potential[i] = epsilon * PotentialFunctions.linearType1(D_sample[i], threshold, sigma);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case PotentialFunctions.L2:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]);
                    final double epsilon = Math.abs(params[0]);
                    potential[i] = epsilon * PotentialFunctions.linearType2(D_sample[i], threshold, sigma);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case PotentialFunctions.PlUMMER:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]);
                    final double epsilon = params[0];
                    potential[i] = epsilon * PotentialFunctions.plummerPotential(D_sample[i], threshold, sigma);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;
            case PotentialFunctions.NONPARAM:
                for (int i = 0; i < D_sample.length; i++) {
                    final double[] weights = params;
                    potential[i] = PotentialFunctions.nonParametric(D_sample[i], weights);
                    sumPotential = sumPotential + potential[i];
                    gibbspotential[i] = Math.exp(-1 * potential[i]);
                }
                break;

            case PotentialFunctions.COULOMB:
                for (int i = 0; i < D_sample.length; i++) {
                    final double threshold = 0;
                    final double sigma = Math.abs(params[1]);
                    final double epsilon = params[0];
                    potential[i] = epsilon * PotentialFunctions.coulomb(D_sample[i], threshold, sigma);
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
        if (type != PotentialFunctions.NONPARAM) {
            params = params.clone();
            params[0] = 1; // set epsilon to 1.0
        }
        calculate();
    }
}
