package mosaic.region_competition;

class ContourPointWithIndexType implements Comparable<ContourPointWithIndexType> 
{
	Point pIndex;
	ContourParticle p;

	public ContourPointWithIndexType(Point index, ContourParticle p) {
		this.pIndex = index;
		this.p = p;
	}

	@Override
	public int compareTo(ContourPointWithIndexType o) {
		// !!!!!!! TODO ascending / descending
		if (this.p.energyDifference > o.p.energyDifference)
			return 1;
		else
			return -1;
	}
}