package mosaic.region_competition;


import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.TreeSet;


class LabelDispenser {

    private final TreeSet<Integer> labels;
    // LinkedList<Integer> labels;

    /**
     * Labels freed up during an iteration gets collected in here
     * and added to labels at the end of the iteration.
     */
    private final TreeSet<Integer> tempList;

    private int highestLabelEverUsed;

    /**
     * @param maxLabels maximal number of labels
     */
    public LabelDispenser(int maxLabels) {
        labels = new TreeSet<Integer>();
        // labels = new LinkedList<Integer>();
        tempList = new TreeSet<Integer>();

        highestLabelEverUsed = 0;
        for (int i = 1; i < maxLabels; i++) // don't start at 0
        {
            labels.add(i);
        }
    }

    /**
     * Adds a freed up label to the list of free labels
     * 
     * @param label Has to be absLabel (non-contour)
     */
    public void addFreedUpLabel(int label) {
        tempList.add(label);
        // labels.add(label);
    }

    /**
     * add the tempList to labels at the end of an iteration
     */
    public void addTempFree() {
        // for (int label : tempList)
        // {
        // labels.addFirst(label);
        // }
        labels.addAll(tempList);

        tempList.clear();
    }

    /**
     * @return an unused label
     * @throws NoSuchElementException - if this list is empty
     */
    public int getNewLabel() {
        final Integer result = labels.pollFirst();
        checkAndSetNewMax(result);
        return result;
    }

    private void checkAndSetNewMax(Integer newMax) {
        if (newMax == null) {
            newMax = highestLabelEverUsed + 1;
        }
        if (newMax > highestLabelEverUsed) {
            highestLabelEverUsed = newMax;
        }
    }

    /**
     * If deleted labels gets reused, it may be possible that the
     * highest label in the final iteration is not the highest label ever used
     * (for example, initialization with n=100 regions, and only n=1...5 survive).
     * Setting brightness/contrast in visualization to 1...5 would not show the
     * labels>5 correctly
     * 
     * @return The highest label ever assigned to a region
     */
    public int getHighestLabelEverUsed() {
        return highestLabelEverUsed;
    }

    public void setLabelsInUse(Collection<Integer> used) {
        labels.removeAll(used);
        final int max = Collections.max(used);
        checkAndSetNewMax(max);
    }

    public LabelDispenserInc getIncrementingDispenser() {
        return new LabelDispenserInc();
    }

    /**
     * A LabelDispenser that only increments and does not reuse labels
     */
    public static class LabelDispenserInc extends LabelDispenser {

        private int label = 0;

        public LabelDispenserInc() {
            super(0);
        }

        @Override
        public int getHighestLabelEverUsed() {
            return label;
        }

        @Override
        public int getNewLabel() {
            label++;
            return label;
        }

        @Override
        public void setLabelsInUse(Collection<Integer> used) {
            if (used.isEmpty()) {
                return;
            }

            final int max = Collections.max(used);
            if (max > label) {
                label = max;
            }
        }

    }
}
