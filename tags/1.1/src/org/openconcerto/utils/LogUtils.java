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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LogUtils {

    private LogUtils() {
    }

    static private final Map<String, Logger> loggers = new HashMap<String, Logger>();

    /**
     * Maintains a map of loggers by name. LogManager only use a {@link WeakReference} to refer to
     * Logger, thus for the same name different logger can be returned. Note that since this class
     * keeps strong references to loggers, other code needs no modifications to be assured to always
     * use the same instance.
     * 
     * @param name A name for the logger. This should be a dot-separated name and should normally be
     *        based on the package name or class name of the subsystem, such as java.net or
     *        javax.swing
     * @return a suitable Logger.
     * @see Logger#getLogger(String)
     */
    static synchronized public final Logger getLogger(final String name) {
        Logger res = loggers.get(name);
        if (res == null) {
            res = Logger.getLogger(name);
            loggers.put(name, res);
        }
        return res;
    }

    /**
     * Remove and close all root handlers.
     */
    static public final void rmRootHandlers() {
        final Logger root = Logger.getLogger("");
        final Handler[] handlers = root.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            final Handler handler = handlers[i];
            root.removeHandler(handler);
            handler.close();
        }
    }

    // assure that there's 1 console logger that outputs everything
    static public final void setUpConsoleHandler() {
        final Logger rootLogger = Logger.getLogger("");
        ConsoleHandler ch = null;
        for (final Handler h : rootLogger.getHandlers()) {
            if (h instanceof ConsoleHandler)
                ch = (ConsoleHandler) h;
        }
        if (ch == null) {
            ch = new ConsoleHandler();
            rootLogger.addHandler(ch);
        }
        ch.setLevel(Level.ALL);
    }

}
