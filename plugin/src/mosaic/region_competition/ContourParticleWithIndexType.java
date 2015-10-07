package mosaic.region_competition;


import mosaic.core.utils.Point;


class ContourParticleWithIndexType implements Comparable<ContourParticleWithIndexType> {

    final Point iParticleIndex;
    final ContourParticle iParticle;

    public ContourParticleWithIndexType(final Point aIndex, final ContourParticle aParticle) {
        this.iParticleIndex = aIndex;
        this.iParticle = aParticle;
    }

    @Override
    public int compareTo(ContourParticleWithIndexType o) {
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
