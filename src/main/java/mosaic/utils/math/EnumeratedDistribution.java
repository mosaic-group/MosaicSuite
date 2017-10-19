/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mosaic.utils.math;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.NotANumberException;
import org.apache.commons.math3.exception.NotFiniteNumberException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.MathArrays;

/**
 * <p>A generic implementation of a
 * <a href="http://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution">
 * discrete probability distribution (Wikipedia)</a> over a finite sample space,
 * based on an enumerated list of &lt;value, probability&gt; pairs.  Input probabilities must all be non-negative,
 * but zero values are allowed and their sum does not have to equal one. Constructors will normalize input
 * probabilities to make them sum to one.</p>
 *
 * <p>The list of <value, probability> pairs does not, strictly speaking, have to be a function and it can
 * contain null values.  The pmf created by the constructor will combine probabilities of equal values and
 * will treat null values as equal.  For example, if the list of pairs &lt;"dog", 0.2&gt;, &lt;null, 0.1&gt;,
 * &lt;"pig", 0.2&gt;, &lt;"dog", 0.1&gt;, &lt;null, 0.4&gt; is provided to the constructor, the resulting
 * pmf will assign mass of 0.5 to null, 0.3 to "dog" and 0.2 to null.</p>
 *
 * @param <T> type of the elements in the sample space.
 * @since 3.2
 */
public class EnumeratedDistribution implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20123308L;

    /**
     * RNG instance used to generate samples from the distribution.
     */
    protected final RandomGenerator random;

    /**
     * List of random variable values.
     */
    private final int[] singletons;

    /**
     * Probabilities of respective random variable values. For i = 0, ..., singletons.size() - 1,
     * probability[i] is the probability that a random variable following this distribution takes
     * the value singletons[i].
     */
    private final double[] probabilities;

    /**
     * Cumulative probabilities, cached to speed up sampling.
     */
    private final double[] cumulativeProbabilities;

    /**
     * Create an enumerated distribution using the given random number generator
     * and probability mass function enumeration.
     *
     * @param rng random number generator.
     * @param pmf probability mass function enumerated as a list of <T, probability>
     * pairs.
     * @throws NotPositiveException if any of the probabilities are negative.
     * @throws NotFiniteNumberException if any of the probabilities are infinite.
     * @throws NotANumberException if any of the probabilities are NaN.
     * @throws MathArithmeticException all of the probabilities are 0.
     */
    public EnumeratedDistribution(final RandomGenerator rng, int[] keys, double[] pmf)
        throws NotPositiveException, MathArithmeticException, NotFiniteNumberException, NotANumberException {
        random = rng;

        singletons = new int[pmf.length];
        final double[] probs = new double[pmf.length];

        for (int i = 0; i < pmf.length; ++i) {
            singletons[i] = keys[i];
            final double p = pmf[i];
            if (p < 0) {
                throw new NotPositiveException(p);
            }
            if (Double.isInfinite(p)) {
                throw new NotFiniteNumberException(p);
            }
            if (Double.isNaN(p)) {
                throw new NotANumberException();
            }
            probs[i] = p;
        }

        probabilities = MathArrays.normalizeArray(probs, 1.0);

        cumulativeProbabilities = new double[probabilities.length];
        double sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            cumulativeProbabilities[i] = sum;
        }
    }

    /**
     * Generate a random value sampled from this distribution.
     *
     * @return a random value.
     */
    public int sample() {
        final double randomValue = random.nextDouble();

        int index = Arrays.binarySearch(cumulativeProbabilities, randomValue);
        if (index < 0) {
            index = -index-1;
        }

        if (index >= 0 &&
            index < probabilities.length &&
            randomValue < cumulativeProbabilities[index]) {
            return singletons[index];
        }

        /* This should never happen, but it ensures we will return a correct
         * object in case there is some floating point inequality problem
         * wrt the cumulative probabilities. */
        return singletons[singletons.length - 1];
    }
}
