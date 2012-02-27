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
 
 package org.openconcerto.ui.component;

import org.openconcerto.utils.ExceptionHandler;

import java.awt.Desktop;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

/**
 * A text field handling HTML and hyperlinks.
 * 
 * @author Sylvain CUAZ
 */
public class HTMLTextField extends JEditorPane {

    static private final ExecutorService exec = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(r, HTMLTextField.class.getSimpleName() + " Desktop thread");
            t.setDaemon(true);
            return t;
        }
    });

    public HTMLTextField(final String html) {
        super("text/html", html);
        this.setEditable(false);
        this.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                final JComponent src = (JComponent) e.getSource();
                if (e.getEventType() == EventType.ACTIVATED) {
                    linkActivated(e, src);
                } else if (e.getEventType() == EventType.ENTERED) {
                    src.setToolTipText(getToolTip(e));
                } else if (e.getEventType() == EventType.EXITED) {
                    src.setToolTipText(null);
                }
            }
        });
        // to look like a text field
        this.setBorder(null);
        this.setOpaque(false);
        this.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        this.setFont(new JTextField().getFont());
    }

    protected String getToolTip(HyperlinkEvent e) {
        return e.getDescription();
    }

    protected void linkActivated(final HyperlinkEvent e, final JComponent src) {
        // this ties into the OS and can block
        exec.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception exn) {
                    ExceptionHandler.handle(src, "Impossible d'ouvrir " + e.getURL(), exn);
                }
            }
        });
    }
}
