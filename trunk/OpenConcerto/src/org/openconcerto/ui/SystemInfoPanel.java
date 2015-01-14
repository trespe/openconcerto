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

import org.openconcerto.ui.component.HTMLTextField;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.SystemInfo;
import org.openconcerto.utils.SystemInfo.Info;
import org.openconcerto.utils.i18n.TM;

import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.JTextComponent;

/**
 * A panel displaying various system informations (eg vm version, user name, network address).
 * 
 * @author Sylvain CUAZ
 */
public class SystemInfoPanel extends JPanel {

    public SystemInfoPanel() {
        final Map<Info, String> infos = SystemInfo.get(true);

        final FormLayouter l = new FormLayouter(this, 1);

        final JEditorPane p = new HTMLTextField(infos.get(Info.JAVA)) {

            private final String getClassName(HyperlinkEvent e) {
                // the link is not an URL
                final String uri = e.getDescription();
                if (uri.startsWith(SystemInfo.CLASS_PROTOCOL + ':'))
                    return uri.substring(SystemInfo.CLASS_PROTOCOL.length() + 1);
                else
                    return null;
            }

            @Override
            protected String getToolTip(HyperlinkEvent e) {
                final String className = getClassName(e);
                if (className != null)
                    return className;
                return super.getToolTip(e);
            }

            @Override
            protected void linkActivated(HyperlinkEvent e, JComponent src) {
                final String className = getClassName(e);
                if (className != null) {
                    String msg = className;
                    try {
                        final Class<?> cl = Class.forName(className);
                        msg += " (exists\nand its superclass is " + cl.getSuperclass() + ")";
                    } catch (ClassNotFoundException e1) {
                        // OK
                        msg += " (couldn't be loaded)";
                    }
                    final JTextComponent txtComp = new ITextArea(msg, 3, 50);
                    txtComp.setEditable(false);
                    txtComp.setBorder(BorderFactory.createEmptyBorder());
                    txtComp.setOpaque(false);

                    JOptionPane.showMessageDialog(src, txtComp, "Class name", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    super.linkActivated(e, src);
                }
            }
        };

        l.add("Java", p);

        // * Windows XP 5.1 (x86)
        l.add(TM.tr("os"), new JLabel("<html>" + infos.get(Info.OS) + "</html>"));

        // * Sylvain ; C:\Documents and Settings\Sylvain ; D:\workspace\CTech
        l.add(TM.tr("user"), new HTMLTextField(infos.get(Info.USER)));

        // * eth0 192.168.28.52/24, état: inactif, nom complet: ""
        l.add(TM.tr("network"), new HTMLTextField(infos.get(Info.NETWORK)));

        // TODO reverse vnc
    }
}
