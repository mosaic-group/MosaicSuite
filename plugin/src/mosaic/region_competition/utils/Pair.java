package mosaic.region_competition.utils;


/**
 * http://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-c-pairl-r-in-java
 */
public class Pair<A, B> {

    public A first;
    public B second;

    public Pair(A first, B second) {
        super();
        this.first = first;
        this.second = second;
    }

    @Override
    public int hashCode() {
        final int hashFirst = first != null ? first.hashCode() : 0;
        final int hashSecond = second != null ? second.hashCode() : 0;

        return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Pair) {
            final Pair<?, ?> otherPair = (Pair<?, ?>) other;
            return ((this.first == otherPair.first || (this.first != null && otherPair.first != null && this.first.equals(otherPair.first))) && (this.second == otherPair.second || (this.second != null
                    && otherPair.second != null && this.second.equals(otherPair.second))));
        }

        return false;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }
}
