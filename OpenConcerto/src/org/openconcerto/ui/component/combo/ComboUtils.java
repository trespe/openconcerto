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
 
 package org.openconcerto.ui.component.combo;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import javax.swing.JComponent;

public class ComboUtils {

    /**
     * Swing dismisses any popup before it let us know about the event, that's sometimes
     * undesirable. For example to implement a combo, you could write a listener on the combo button
     * and depending on the visible state of the popup either show it or hide it. In practice this
     * is impossible since by the time your listener is called, the popup is always closed.
     * 
     * @param comp the comp which shouldn't hide popups.
     * @return <code>true</code> if it succeeded.
     */
    public static boolean doNotCancelPopupHack(JComponent comp) {
        try {
            final Class<?> clazz = javax.swing.plaf.basic.BasicComboBoxUI.class;
            final Object val = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    final Field field = clazz.getDeclaredField("HIDE_POPUP_KEY");
                    field.setAccessible(true);
                    return field.get(null);
                }
            });
            cancelPopupHack(comp, val);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static void cancelPopupHack(JComponent comp) {
        cancelPopupHack(comp, null);
    }

    private static void cancelPopupHack(JComponent comp, Object val) {
        comp.putClientProperty("doNotCancelPopup", val);
    }

}
