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
 
 package org.openconcerto.laf;

import java.awt.Component;
import java.awt.Font;
import java.awt.Label;
import java.awt.TextField;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * Various methods about look and feels.
 * 
 * @author Sylvain
 */
public class LAFUtils {

    private static LookAndFeel SYSTEM_LAF = null;
    // cache defaults, since after looking at the javadoc, it seems it should not be called often
    private static final Map<LookAndFeel, UIDefaults> defaults = new HashMap<LookAndFeel, UIDefaults>();

    // from LookAndFeel#getID()
    public static final String Mac_ID = "Aqua";
    public static final String ClassicMac_ID = "Mac";
    public static final String Windows_ID = "Windows";
    public static final String Motif_ID = "Metal";
    public static final String Metal_ID = "Motif";
    public static final String GTK_ID = "GTK";
    public static final String Nimbus_ID = "Nimbus";

    private LAFUtils() {
    }

    public static LookAndFeel getSystemLAF() {
        if (SYSTEM_LAF == null) {
            try {
                SYSTEM_LAF = (LookAndFeel) Class.forName(UIManager.getSystemLookAndFeelClassName(), true, Thread.currentThread().getContextClassLoader()).newInstance();
                SYSTEM_LAF.initialize();
            } catch (Exception e) {
                throw new IllegalStateException("could not initialize " + UIManager.getSystemLookAndFeelClassName(), e);
            }
        }
        return SYSTEM_LAF;
    }

    private static UIDefaults getDefaults(LookAndFeel laf) {
        if (!defaults.containsKey(laf)) {
            defaults.put(laf, laf.getDefaults());
        }
        return defaults.get(laf);
    }

    public static Font getFont(String name) {
        return getFont(name, getSystemLAF());
    }

    /**
     * Returns the named font from the passed look and feel.
     * 
     * @param name the name, eg "TextField.font".
     * @param laf the look and feel, eg <code>getSystemLAF()</code>.
     * @return the corresponding font, eg "Tahoma 11".
     */
    public static Font getFont(String name, LookAndFeel laf) {
        return getDefaults(laf).getFont(name);
    }

    /**
     * Returns the name used in UIManager for components.
     * 
     * @param comp any component, eg a JTextField.
     * @return the corresponding name, <code>null</code> if unknown, eg "TextField".
     */
    public static String getUIName(Component comp) {
        final String res;
        if (comp instanceof JComboBox)
            res = "ComboBox";
        else if (comp instanceof JTextField || comp instanceof TextField)
            res = "TextField";
        else if (comp instanceof JLabel || comp instanceof Label)
            res = "Label";
        else
            res = null;
        return res;
    }

}
