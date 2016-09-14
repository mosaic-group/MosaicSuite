package mosaic.region_competition;


import mosaic.core.imageUtils.Point;


class ContourParticleWithIndex implements Comparable<ContourParticleWithIndex> {

    final Point iPoint;
    final ContourParticle iContourParticle;

    public ContourParticleWithIndex(final Point aPoint, final ContourParticle aParticle) {
        iPoint = aPoint;
        iContourParticle = aParticle;
    }

    @Override
    public int compareTo(ContourParticleWithIndex o) {
        // Sort with increasing energy difference
        if (iContourParticle.energyDifference > o.iContourParticle.energyDifference) {
            return 1;
        }
        else if (iContourParticle.energyDifference < o.iContourParticle.energyDifference) {
            return -1;
        }
        return 0;
    }
}
