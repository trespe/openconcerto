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

import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

/**
 * A listener that hides components when they're empty.
 * 
 * @author Sylvain CUAZ
 */
public class AutoHideListener implements ContainerListener {

    static private final AutoHideListener instance = new AutoHideListener();

    static public <T extends Container> T listen(T comp) {
        comp.addContainerListener(instance);
        setVisible(comp);
        return comp;
    }

    static public <T extends Container> T unlisten(T comp) {
        comp.removeContainerListener(instance);
        return comp;
    }

    static private void setVisible(Container comp) {
        comp.setVisible(comp.getComponentCount() > 0);
    }

    // singleton
    private AutoHideListener() {
    }

    @Override
    public void componentAdded(ContainerEvent e) {
        setVisible(e.getContainer());
    }

    @Override
    public void componentRemoved(ContainerEvent e) {
        setVisible(e.getContainer());
    }
}
