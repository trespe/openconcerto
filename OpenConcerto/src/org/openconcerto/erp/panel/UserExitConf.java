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
 
 package org.openconcerto.erp.panel;

public class UserExitConf {

    static public final UserExitConf DEFAULT = new UserExitConf(null, false);

    private final String msg;
    private final boolean restart;

    public UserExitConf(String msg) {
        this(msg, false);
    }

    public UserExitConf(String msg, boolean restart) {
        super();
        this.msg = msg;
        this.restart = restart;
    }

    /**
     * The message to present the user.
     * 
     * @return the message to display.
     */
    public final String getMessage() {
        return this.msg;
    }

    /**
     * Whether the application should restart after quitting.
     * 
     * @return <code>true</code> if the application should restart.
     */
    public final boolean shouldRestart() {
        return this.restart;
    }

    /**
     * Called early on, just after all windows are closed.
     * 
     * @throws Exception if an error occurs.
     */
    protected void afterWindowsClosed() throws Exception {
    }

    /**
     * Called in a {@link Runtime#addShutdownHook(Thread) shutdown hook} so this should be quick and
     * without GUI.
     * 
     * @throws Exception if an error occurs.
     */
    protected void beforeShutdown() throws Exception {
    }
}
