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
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Various methods about look and feels.
 * 
 * @author Sylvain
 */
public class LAFUtils {

    /**
     * Boolean system property.
     * 
     * @see #setLookAndFeel()
     */
    public static final String LAF_CROSSPLATFORM = "laf.crossplatform";

    /**
     * Boolean system property.
     * 
     * @see #fixLookAndFeel(String)
     */
    public static final String LAF_DONT_FIX = "laf.dontFix";

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

    /**
     * Set the look and feel. UIManager default look and feel is the cross platform one, you can
     * override this behavior by setting the <code>swing.defaultlaf</code> system property but to do
     * that you must know the exact name of the look and feel : you can't just specify
     * "SystemLookAndFeel". The class used for the look and feel is chosen in the following manner:
     * <ol>
     * <li>If the system property <code>swing.defaultlaf</code> is {@code non-null}, use its value.</li>
     * <li>If the system property {@value #LAF_CROSSPLATFORM} is {@code false}, use
     * {@link UIManager#getSystemLookAndFeelClassName()}.</li>
     * <li>Else use {@link UIManager#getCrossPlatformLookAndFeelClassName()} which can be overridden
     * by setting the <code>swing.crossplatformlaf</code> system property.</li>
     * </ol>
     * I.e. if no property is defined, you get the system look and feel.
     * 
     * @throws IllegalAccessException if the class or initializer isn't accessible
     * @throws InstantiationException if a new instance of the class couldn't be created
     * @throws ClassNotFoundException if the <code>LookAndFeel</code> class could not be found
     * @throws ClassCastException if {@code className} does not identify a class that extends
     *         {@code LookAndFeel}
     * @throws UnsupportedLookAndFeelException if {@link LookAndFeel#isSupportedLookAndFeel()} is
     *         false.
     */
    static public final void setLookAndFeel() throws ClassNotFoundException, ClassCastException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        final String defaultLAF = System.getProperty("swing.defaultlaf");
        if (defaultLAF == null) {
            final String laf;
            if (Boolean.getBoolean(LAF_CROSSPLATFORM))
                laf = UIManager.getCrossPlatformLookAndFeelClassName();
            else
                laf = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(fixLookAndFeel(laf));
        } else {
            // same rule than UIManager
            assert UIManager.getLookAndFeel().getClass().getName().equals(defaultLAF);
        }
    }

    /**
     * Attempt to replace a look and feel with one less buggy.
     * 
     * @param laf the look and feel to replace.
     * @return an equivalent look and feel, or <code>laf</code> if {@value #LAF_DONT_FIX} is
     *         <code>true</code>.
     */
    static public final String fixLookAndFeel(final String laf) {
        if (!Boolean.getBoolean(LAF_DONT_FIX) && laf.equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel")) {
            return WindowsLookAndFeelFix.class.getName();
        }
        return laf;
    }
}
