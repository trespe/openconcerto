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

import org.openconcerto.laf.LAFUtils;

import java.awt.Component;
import java.awt.Font;

public class FontUtils {

    public static Font setFontFor(Component comp, String toDisplay) {
        return setFontFor(comp, LAFUtils.getUIName(comp), toDisplay);
    }

    /**
     * If the current font for <code>comp</code> cannot handle <code>toDisplay</code>, this
     * method set the font to the system LAF (which should have appropriate fallbacks).
     * 
     * @param comp the component, eg a JComboBox.
     * @param name the name of system LAF font, eg ComboBox.
     * @param toDisplay which string to test with, eg "^|".
     * @return the font applied to <code>comp</code>, <code>null</code> if none was applied.
     */
    public static Font setFontFor(Component comp, String name, String toDisplay) {
        if (name == null)
            throw new NullPointerException();

        final Font res;
        if (comp.getFont().canDisplayUpTo(toDisplay) == -1)
            res = null;
        else {
            // if current font cannot handle needed chars, try the default system font
            // which should have appropriate fallbacks (CompositeFont)
            final Font comboSysFont = LAFUtils.getFont(name + ".font");
            if (comboSysFont == null) {
                res = null;
            } else {
                res = comboSysFont.deriveFont(comp.getFont().getAttributes());
                comp.setFont(res);
            }
        }
        return res;
    }

}
