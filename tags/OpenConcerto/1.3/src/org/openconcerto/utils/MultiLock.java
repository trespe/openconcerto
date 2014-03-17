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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Group multiple locks into one.
 * 
 * @author Sylvain
 */
public class MultiLock implements Lock {

    static private void lock(List<Lock> locks, final boolean interruptibly) throws InterruptedException {
        final int stop = locks.size();
        int i = 0;
        try {
            for (; i < stop; i++) {
                if (interruptibly)
                    locks.get(i).lockInterruptibly();
                else
                    locks.get(i).lock();
            }
        } catch (InterruptedException e) {
            unlock(locks, i);
            throw e;
        } catch (RuntimeException e) {
            unlock(locks, i);
            throw e;
        }
    }

    static private void unlock(List<Lock> locks, int toIndex) {
        // locks.get(toIndex) wasn't locked by this method
        // (but it might have been elsewhere so we can't check that we don't hold it)
        unlock(locks.subList(0, toIndex));
    }

    static private void unlock(List<Lock> locks) {
        for (int i = locks.size() - 1; i >= 0; i--) {
            locks.get(i).unlock();
        }
    }

    private final List<Lock> locks;

    public MultiLock(List<? extends Lock> locks) {
        super();
        this.locks = Collections.unmodifiableList(new ArrayList<Lock>(locks));
    }

    @Override
    public void lock() {
        try {
            lock(this.locks, false);
        } catch (InterruptedException e) {
            // cannot happen
            throw new Error(e);
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        lock(this.locks, true);
    }

    @Override
    public boolean tryLock() {
        try {
            return tryLock(-1, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // cannot happen
            throw new Error(e);
        }
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
        final int stop = this.locks.size();
        boolean ok = true;
        Exception exn = null;
        int i = 0;
        long remainingTime = unit.toNanos(time);
        try {
            for (; i < stop && ok; i++) {
                if (time <= 0) {
                    ok = this.locks.get(i).tryLock();
                } else {
                    final long before = System.nanoTime();
                    ok = this.locks.get(i).tryLock(remainingTime, TimeUnit.NANOSECONDS);
                    final long after = System.nanoTime();
                    // allow zero remainingTime to allow the first lock to use up all allocated
                    // time and then the rest none.
                    remainingTime -= after - before;
                }
            }
        } catch (InterruptedException e) {
            exn = e;
        } catch (RuntimeException e) {
            exn = e;
        }
        if (!ok || exn != null) {
            unlock(this.locks, i);
            if (exn instanceof RuntimeException)
                throw (RuntimeException) exn;
            else if (exn instanceof InterruptedException)
                throw (InterruptedException) exn;
            else
                assert exn == null;
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void unlock() {
        unlock(this.locks);
    }

    /**
     * Unsupported.
     * 
     * @return never.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public Condition newCondition() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
