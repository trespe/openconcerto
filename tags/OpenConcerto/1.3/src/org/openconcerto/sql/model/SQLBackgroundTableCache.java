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
 
 package org.openconcerto.sql.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SQLBackgroundTableCache {
    private static SQLBackgroundTableCache instance;
    private Map<SQLTable, SQLBackgroundTableCacheItem> list = new HashMap<SQLTable, SQLBackgroundTableCacheItem>();

    /**
     * @param args
     */
    public static void main(String[] args) {
        SQLBackgroundTableCache cache = SQLBackgroundTableCache.getInstance();
        SQLTable t = new SQLTable(null, "test");
        cache.add(t, 30);

    }

    private SQLBackgroundTableCache() {

    }

    public static synchronized SQLBackgroundTableCache getInstance() {
        if (instance == null) {
            instance = new SQLBackgroundTableCache();
        }
        return instance;
    }

    public synchronized void add(SQLTable t, int second) {
        if (!isCached(t)) {
            final SQLBackgroundTableCacheItem item = new SQLBackgroundTableCacheItem(t, second);
            this.list.put(t, item);
        }
    }

    public void startCacheWatcher() {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        synchronized (SQLBackgroundTableCache.this) {
                            Set<SQLTable> set = list.keySet();
                            for (SQLTable table : set) {
                                getCacheForTable(table);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {// 1 minute
                        Thread.sleep(60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        t.setName("SQLBackgroundTableCache");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
    }

    public synchronized boolean isCached(SQLTable t) {
        return this.list.containsKey(t);
    }

    public synchronized SQLBackgroundTableCacheItem getCacheForTable(SQLTable t) {
        final SQLBackgroundTableCacheItem item = this.list.get(t);
        if (item != null) {
            item.reloadFromDbIfNeeded();
        }
        return item;
    }

}
