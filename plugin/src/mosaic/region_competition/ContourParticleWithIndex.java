package mosaic.region_competition;


import mosaic.core.utils.Point;


class ContourParticleWithIndex implements Comparable<ContourParticleWithIndex> {

    final Point iParticleIndex;
    final ContourParticle iParticle;

    public ContourParticleWithIndex(final Point aIndex, final ContourParticle aParticle) {
        this.iParticleIndex = aIndex;
        this.iParticle = aParticle;
    }

    @Override
    public int compareTo(ContourParticleWithIndex o) {
        if (this.iParticle.energyDifference > o.iParticle.energyDifference) {
            return 1;
        }
        else if (this.iParticle.energyDifference < o.iParticle.energyDifference) {
            return -1;
        }
        else {
            return 0;
        }
    }
}
