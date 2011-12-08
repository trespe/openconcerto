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
 
 package org.openconcerto.ui;

import javax.swing.SwingUtilities;

/**
 * A timer whose interval begins when r starts.
 * 
 * <pre>
 * &lt;- r -&gt;       &lt;- r -&gt;
 * </pre>
 * <pre>
 * &lt;- interval -&gt;&lt;- interval -&gt;
 * </pre>
 * 
 * @author Sylvain CUAZ
 */
public class RateTimer extends Timer {

    public RateTimer(Runnable r, int delay, int accel, int min, Runnable finalizer) {
        super(r, delay, accel, min, finalizer);
    }

    protected void invokeTask() {
        SwingUtilities.invokeLater(this.r);
    }

    protected void waitForTask() throws InterruptedException {
        // a rate by definition does not wait
    }

}
