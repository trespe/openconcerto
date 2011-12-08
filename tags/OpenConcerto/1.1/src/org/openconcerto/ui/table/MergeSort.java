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
 
 package org.openconcerto.ui.table;

public abstract class MergeSort extends Object {
    protected Object toSort[];
    protected Object swapSpace[];

    public void sort(Object array[]) {
        if (array != null && array.length > 1) {
            int maxLength;

            maxLength = array.length;
            swapSpace = new Object[maxLength];
            toSort = array;
            this.mergeSort(0, maxLength - 1);
            swapSpace = null;
            toSort = null;
        }
    }

    public abstract int compareElementsAt(int beginLoc, int endLoc);

    protected void mergeSort(int begin, int end) {
        if (begin != end) {
            int mid;

            mid = (begin + end) / 2;
            this.mergeSort(begin, mid);
            this.mergeSort(mid + 1, end);
            this.merge(begin, mid, end);
        }
    }

    protected void merge(int begin, int middle, int end) {
        int firstHalf, secondHalf, count;

        firstHalf = count = begin;
        secondHalf = middle + 1;
        while ((firstHalf <= middle) && (secondHalf <= end)) {
            if (this.compareElementsAt(secondHalf, firstHalf) < 0)
                swapSpace[count++] = toSort[secondHalf++];
            else
                swapSpace[count++] = toSort[firstHalf++];
        }
        if (firstHalf <= middle) {
            while (firstHalf <= middle)
                swapSpace[count++] = toSort[firstHalf++];
        } else {
            while (secondHalf <= end)
                swapSpace[count++] = toSort[secondHalf++];
        }
        for (count = begin; count <= end; count++)
            toSort[count] = swapSpace[count];
    }
}
