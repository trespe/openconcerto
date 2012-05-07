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
 
 package org.openconcerto.utils.sync;

import java.util.ArrayList;
import java.util.List;

public class RangeList {
    private final List<Range> list = new ArrayList<Range>();
    private final int limit;

    public RangeList(int limit) {
        this.limit = limit;
    }

    public void add(Range range) {
        if (range.getStart() < 0 || range.getStart() >= limit) {
            throw new IllegalArgumentException(range + " start out of limit");
        }
        if (range.getStop() < 0 || range.getStop() > limit) {
            throw new IllegalArgumentException(range + " stop out of limit");
        }
        if (list.size() > 0) {
            Range last = list.get(list.size() - 1);
            if (last.getStop() == range.getStart()) {
                last.setStop(range.getStop());
            } else if (range.getStart() < last.getStart()) {
                throw new IllegalArgumentException("start (" + range.getStart() + ") < lastStart (" + last.getStart() + ")");
            } else {
                list.add(range);
            }
        } else {
            list.add(range);
        }
    }

    public List<Range> getUsedRanges() {
        return this.list;
    }

    public List<Range> getUnusedRanges() {
        List<Range> result = new ArrayList<Range>();
        if (list.size() == 0) {
            result.add(new Range(0, limit));
            return result;
        }
        // First
        Range r = new Range(0, list.get(0).getStart());
        if (!r.isEmpty()) {
            // System.out.println("AddFirst:" + r);
            result.add(r);
        }
        for (int i = 0; i < list.size() - 1; i++) {
            Range r1 = this.list.get(i);
            Range r2 = this.list.get(i + 1);

            result.add(new Range(r1.getStop(), r2.getStart()));

        }
        // Last
        Range lastRange = new Range(list.get(list.size() - 1).getStop(), limit);
        if (!lastRange.isEmpty()) {
            // System.out.println("AddLast:" + lastRange);
            result.add(lastRange);
        }

        return result;

    }

    public void dump() {
        System.out.println("RangeList 0 - " + this.limit);
        System.out.println("Used");
        for (Range r : getUsedRanges()) {
            System.out.println(r);
        }
        System.out.println("Unused");
        final List<Range> unusedRanges = getUnusedRanges();
        for (Range r : unusedRanges) {
            System.out.println(r);
        }
        System.out.println("===============");
    }
}
