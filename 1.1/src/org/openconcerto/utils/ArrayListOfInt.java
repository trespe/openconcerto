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

import java.util.Arrays;
import java.util.Collection;

public class ArrayListOfInt {
     /**
     * The array buffer into which the elements of the ArrayList are stored. The capacity of the
     * ArrayList is the length of this array buffer.
     */
    private transient int[] elementData;

    /**
     * The size of the ArrayList (the number of elements it contains).
     * 
     * @serial
     */
    private int size;

    /**
     * Constructs an empty list with the specified initial capacity.
     * 
     * @param initialCapacity the initial capacity of the list
     * @exception IllegalArgumentException if the specified initial capacity is negative
     */
    public ArrayListOfInt(int initialCapacity) {

        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        this.elementData = new int[initialCapacity];
    }

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public ArrayListOfInt() {
        this(10);
    }

    /**
     * Trims the capacity of this <tt>ArrayList</tt> instance to be the list's current size. An
     * application can use this operation to minimize the storage of an <tt>ArrayList</tt>
     * instance.
     */
    public void trimToSize() {

        int oldCapacity = elementData.length;
        if (size < oldCapacity) {
            elementData = Arrays.copyOf(elementData, size);
        }
    }

    /**
     * Increases the capacity of this <tt>ArrayList</tt> instance, if necessary, to ensure that it
     * can hold at least the number of elements specified by the minimum capacity argument.
     * 
     * @param minCapacity the desired minimum capacity
     */
    public void ensureCapacity(int minCapacity) {

        int oldCapacity = elementData.length;
        if (minCapacity > oldCapacity) {

            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            // minCapacity is usually close to size, so this is a win:
            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }

    /**
     * Returns the number of elements in this list.
     * 
     * @return the number of elements in this list
     */
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     * 
     * @return <tt>true</tt> if this list contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns <tt>true</tt> if this list contains the specified element. More formally, returns
     * <tt>true</tt> if and only if this list contains at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     * 
     * @param o element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified element
     */
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list, or -1 if
     * this list does not contain the element. More formally, returns the lowest index <tt>i</tt>
     * such that <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>, or -1
     * if there is no such index.
     */
    public int indexOf(Object o) {

        for (int i = 0; i < size; i++)
            if (o.equals(elementData[i]))
                return i;

        return -1;
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list, or -1 if this
     * list does not contain the element. More formally, returns the highest index <tt>i</tt> such
     * that <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>, or -1 if
     * there is no such index.
     */
    public int lastIndexOf(Object o) {

        for (int i = size - 1; i >= 0; i--)
            if (o.equals(elementData[i]))
                return i;

        return -1;
    }

    /**
     * Returns a shallow copy of this <tt>ArrayList</tt> instance. (The elements themselves are
     * not copied.)
     * 
     * @return a clone of this <tt>ArrayList</tt> instance
     */
    public Object clone() {
        ArrayListOfInt v = new ArrayListOfInt();
        v.elementData = Arrays.copyOf(elementData, size);

        return v;

    }

    /**
     * Returns an array containing all of the elements in this list in proper sequence (from first
     * to last element).
     * 
     * <p>
     * The returned array will be "safe" in that no references to it are maintained by this list.
     * (In other words, this method must allocate a new array). The caller is thus free to modify
     * the returned array.
     * 
     * <p>
     * This method acts as bridge between array-based and collection-based APIs.
     * 
     * @return an array containing all of the elements in this list in proper sequence
     */
    public int[] toArray() {
        return Arrays.copyOf(elementData, size);
    }

    // Positional Access Operations

    /**
     * Returns the element at the specified position in this list.
     * 
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public int get(int index) {
        RangeCheck(index);

        return elementData[index];
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     * 
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public int set(int index, int element) {
        RangeCheck(index);

        int oldValue = elementData[index];
        elementData[index] = element;
        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * @param e element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean add(int e) {
        ensureCapacity(size + 1); // Increments modCount!!
        elementData[size++] = e;
        return true;
    }

    /**
     * Inserts the specified element at the specified position in this list. Shifts the element
     * currently at that position (if any) and any subsequent elements to the right (adds one to
     * their indices).
     * 
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public void add(int index, int element) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);

        ensureCapacity(size + 1); // Increments modCount!!
        System.arraycopy(elementData, index, elementData, index + 1, size - index);
        elementData[index] = element;
        size++;
    }

    /**
     * Removes the element at the specified position in this list. Shifts any subsequent elements to
     * the left (subtracts one from their indices).
     * 
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public int removeAtIndex(int index) {
        RangeCheck(index);

        int oldValue = elementData[index];

        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index + 1, elementData, index, numMoved);
        elementData[--size] = 0; // Let gc do its work

        return oldValue;
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present. If
     * the list does not contain the element, it is unchanged. More formally, removes the element
     * with the lowest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt> (if such an
     * element exists). Returns <tt>true</tt> if this list contained the specified element (or
     * equivalently, if this list changed as a result of the call).
     * 
     * @param o element to be removed from this list, if present
     * @return <tt>true</tt> if this list contained the specified element
     */
    public boolean remove(int o) {

        for (int index = 0; index < size; index++)
            if (o == elementData[index]) {
                fastRemove(index);
                return true;

            }
        return false;
    }

    /*
     * Private remove method that skips bounds checking and does not return the value removed.
     */
    private void fastRemove(int index) {

        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index + 1, elementData, index, numMoved);
        elementData[--size] = 0; // Let gc do its work
    }

    /**
     * Removes all of the elements from this list. The list will be empty after this call returns.
     */
    public void clear() {

        size = 0;
    }

    /**
     * Appends all of the elements in the specified collection to the end of this list, in the order
     * that they are returned by the specified collection's Iterator. The behavior of this operation
     * is undefined if the specified collection is modified while the operation is in progress.
     * (This implies that the behavior of this call is undefined if the specified collection is this
     * list, and this list is nonempty.)
     * 
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(int a[]) {

        int numNew = a.length;
        ensureCapacity(size + numNew); // Increments modCount
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
     * Inserts all of the elements in the specified collection into this list, starting at the
     * specified position. Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (increases their indices). The new elements will appear in the list in
     * the order that they are returned by the specified collection's iterator.
     * 
     * @param index index at which to insert the first element from the specified collection
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(int index, int[] a) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);

        int numNew = a.length;
        ensureCapacity(size + numNew); // Increments modCount

        int numMoved = size - index;
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew, numMoved);

        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
     * Removes from this list all of the elements whose index is between <tt>fromIndex</tt>,
     * inclusive, and <tt>toIndex</tt>, exclusive. Shifts any succeeding elements to the left
     * (reduces their index). This call shortens the list by <tt>(toIndex - fromIndex)</tt>
     * elements. (If <tt>toIndex==fromIndex</tt>, this operation has no effect.)
     * 
     * @param fromIndex index of first element to be removed
     * @param toIndex index after last element to be removed
     * @throws IndexOutOfBoundsException if fromIndex or toIndex out of range (fromIndex &lt; 0 ||
     *         fromIndex &gt;= size() || toIndex &gt; size() || toIndex &lt; fromIndex)
     */
    protected void removeRange(int fromIndex, int toIndex) {
        int numMoved = size - toIndex;
        System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);

    }

    /**
     * Checks if the given index is in range. If not, throws an appropriate runtime exception. This
     * method does *not* check if the index is negative: It is always used immediately prior to an
     * array access, which throws an ArrayIndexOutOfBoundsException if index is negative.
     */
    private void RangeCheck(int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

}
