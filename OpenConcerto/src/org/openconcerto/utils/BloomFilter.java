/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.utils;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class BloomFilter<E> implements Set<E>, Serializable {
    private static final long serialVersionUID = 3527833617516722215L;
    private final int k;
    private final BitSet bitSet;
    private final int bitArraySize;
    private final int expectedElements;

    /**
     * Construct a SimpleBloomFilter. You must specify the number of bits in the Bloom Filter, and
     * also you should specify the number of items you expect to add. The latter is used to choose
     * some optimal internal values to minimize the false-positive rate (which can be estimated with
     * expectedFalsePositiveRate()).
     * 
     * @param bitArraySize The number of bits in the bit array (often called 'm' in the context of
     *        bloom filters).
     * @param expectedElements The typical number of items you expect to be added to the
     *        SimpleBloomFilter (often called 'n').
     */
    public BloomFilter(int bitArraySize, int expectedElements) {
        this.bitArraySize = bitArraySize;
        this.expectedElements = expectedElements;
        this.k = (int) Math.ceil((bitArraySize / expectedElements) * Math.log(2.0));
        bitSet = new BitSet(bitArraySize);
    }

    /**
     * Calculates the approximate probability of the contains() method returning true for an object
     * that had not previously been inserted into the bloom filter. This is known as the "false
     * positive probability".
     * 
     * @return The estimated false positive rate
     */
    public double expectedFalsePositiveProbability() {
        return Math.pow((1 - Math.exp(-k * (double) expectedElements / bitArraySize)), k);
    }

    /*
     * @return This method will always return false
     * 
     * @see java.util.Set#add(java.lang.Object)
     */
    public boolean add(E o) {
        final Random r = new Random(o.hashCode());
        for (int x = 0; x < k; x++) {
            bitSet.set(r.nextInt(bitArraySize), true);
        }
        return false;
    }

    /**
     * @return This method will always return false
     */
    public boolean addAll(Collection<? extends E> c) {
        for (E o : c) {
            add(o);
        }
        return false;
    }

    /**
     * Clear the Bloom Filter
     */
    public void clear() {
        final int length = bitSet.length();
        for (int x = 0; x < length; x++) {
            bitSet.set(x, false);
        }
    }

    /**
     * @return False indicates that o was definitely not added to this Bloom Filter, true indicates
     *         that it probably was. The probability can be estimated using the
     *         expectedFalsePositiveProbability() method.
     */
    public boolean contains(Object o) {
        Random r = new Random(o.hashCode());
        for (int x = 0; x < k; x++) {
            if (!bitSet.get(r.nextInt(bitArraySize)))
                return false;
        }
        return true;
    }

    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o))
                return false;
        }
        return true;
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    public int getBitLength() {
        return bitSet.length();
    }

    public static void main(String[] args) {
        BloomFilter<String> set = new BloomFilter<String>(100, 5);

        // Add some things to the bloom filter
        set.add("dog");
        set.add("doq");
        set.add("cat");
        set.add("mouse");
        set.add("dolphin");

        // Test to see if the bloom filter remembers
        String test = "dog";
        if (set.contains(test)) {
            System.out.println(test + " is in the set with probability " + (1 - set.expectedFalsePositiveProbability()));
        } else {
            System.out.println(test + " is definitely not in the set");
        }

        System.out.println("BloomFilter stored in " + set.getBitLength() / 8 + " bytes");
    }

}
