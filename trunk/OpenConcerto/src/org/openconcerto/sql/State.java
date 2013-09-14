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
 
 package org.openconcerto.sql;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Une classe qui permet de connaître l'état du Framework en temps réel.
 * 
 * @author Sylvain CUAZ
 */
public final class State {

    public static final boolean DEBUG = true;

    public static final org.openconcerto.sql.State INSTANCE = new org.openconcerto.sql.State();

    public static final DateFormat TIME_FMT;

    static {
        TIME_FMT = DateFormat.getTimeInstance();
        TIME_FMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final List<String> requests;

    private final List<String> failed;

    private int failedStmts;

    private int connectionTotalCount;

    private int connectionCount;

    private int requestsTotalCount;

    private final long upDate;

    private int frameCount;

    private int framesVisible;

    private int cacheHit;

    /**
     * Once created, this thread starts immediately.
     */
    private State() {
        this.requests = new ArrayList<String>();
        this.requestsTotalCount = 0;
        this.failed = new ArrayList<String>();
        this.failedStmts = 0;
        this.connectionTotalCount = 0;
        this.connectionCount = 0;
        this.frameCount = 0;
        this.framesVisible = 0;
        this.cacheHit = 0;
        this.upDate = System.currentTimeMillis();
    }

    final String getFull() {
        String res = "failed requests: " + this.failed.size();
        res += "\nfailed statements: " + this.failedStmts;
        res += "\nrequests: " + this.requests.size();
        res += "\ntotal requests: " + this.requestsTotalCount;
        res += "\nconnections: " + this.connectionCount;
        res += "\ntotal connections: " + this.connectionTotalCount;
        res += "\ncache hit: " + this.cacheHit;
        res += "\nuptime: " + this.getUptime();

        res += "\n\nvisible frames: " + this.framesVisible;
        res += "\ntotal: " + this.frameCount;

        res += "\n\nhost: " + this.getHostDesc();
        return res;
    }

    private String getUptime() {
        return TIME_FMT.format(new Long(System.currentTimeMillis() - this.upDate));
    }

    private String getHostDesc() {
        final Runtime rt = Runtime.getRuntime();
        String res = "processors: " + rt.availableProcessors();
        res += "\nfree memory: " + formatBytes(rt.freeMemory());
        res += "\ntotal memory: " + formatBytes(rt.totalMemory());
        return res;
    }

    private String formatBytes(long b) {
        return b / 1024 / 1024 + "MB";
    }

    public synchronized void beginRequest(String req) {
        this.requests.add(req);
        this.requestsTotalCount++;
    }

    public synchronized void endRequest(String req) {
        this.requests.remove(req);
    }

    public final synchronized List<String> getRequests() {
        return new ArrayList<String>(this.requests);
    }

    public synchronized void addFailedRequest(String query) {
        this.failed.add(query);
    }

    public synchronized void addFailedStatement() {
        this.failedStmts++;
    }

    public synchronized void connectionCreated() {
        this.connectionTotalCount++;
        this.connectionCount++;
    }

    public synchronized void connectionRemoved() {
        this.connectionCount--;
    }

    public synchronized void frameCreated() {
        this.frameCount++;
    }

    public synchronized void frameShown() {
        this.framesVisible++;
    }

    public synchronized void frameHidden() {
        this.framesVisible--;
    }

    public synchronized void addCacheHit() {
        this.cacheHit++;
    }

}
