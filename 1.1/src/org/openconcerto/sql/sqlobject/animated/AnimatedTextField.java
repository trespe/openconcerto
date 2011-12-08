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
 
 package org.openconcerto.sql.sqlobject.animated;

import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;

/**
 * Blink the background when this text field is empty.
 * 
 * @author Guillaume
 */
public class AnimatedTextField extends JTextField {
    private boolean isAnimating = false;

    // TODO mv to fwk ui
    public AnimatedTextField() {
        this(12);
    }

    public AnimatedTextField(int columns) {
        super(columns);
        this.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update(DocumentEvent e) {
                startOrStop();
            }
        });
        this.addAncestorListener(new AncestorListener() {

            public void ancestorAdded(AncestorEvent event) {
                startOrStop();
            }

            public void ancestorMoved(AncestorEvent event) {
            }

            public void ancestorRemoved(AncestorEvent event) {
                stopAnimation();
            }
        });
    }

    public void stopAnimation() {
        this.setAnimating(false);
    }

    public void startAnimation() {
        this.setAnimating(true);
        final Thread thread = new Thread(new Runnable() {

            public void run() {
                final int[] a = { 0, 0, 64, 80, 160, 255, 255, 160, 64, 1, 1, 64, 80, 160, 255, 218, 160, 64, 0, 0, 0, 0, 0, 0, 0, 0 };
                int i = 0;
                while (AnimatedTextField.this.isAnimating()) {
                    i += 1;
                    if (i >= a.length)
                        i = 0;
                    try {
                        Thread.sleep(200);
                        final int currentColor = a[i];
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                setBackground(new Color(255, 255, 255 - currentColor / 4));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                setBackground(Color.WHITE);

            };
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void startOrStop() {
        if (this.getText().length() > 0) {
            this.stopAnimation();
        } else if (!this.isAnimating()) {
            this.startAnimation();
        }
    }

    synchronized void setAnimating(boolean isAnimating) {
        this.isAnimating = isAnimating;
    }

    synchronized boolean isAnimating() {
        return this.isAnimating;
    }

}
