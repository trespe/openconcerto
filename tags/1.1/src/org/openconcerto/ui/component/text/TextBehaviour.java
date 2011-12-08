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
 
 package org.openconcerto.ui.component.text;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.text.JTextComponent;

/**
 * Listens to its passed text component to select all on focus gained and revert when pressing ESC.
 * 
 * @author Sylvain
 */
public class TextBehaviour {

    // MAYBE faire un stopManage() qui enlèverait tous les listeners
    public static final TextBehaviour manage(JTextComponent comp) {
        return new TextBehaviour(comp);
    }

    private final JTextComponent comp;

    // does this component just gained focus
    protected boolean gained;
    protected boolean mousePressed;
    // the content of this text field when it gained focus
    protected String initialText;

    private TextBehaviour(JTextComponent comp) {
        this.comp = comp;

        // select all on focus gained
        // except if the user is selecting with the mouse
        this.comp.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                TextBehaviour.this.gained = true;
                TextBehaviour.this.initialText = TextBehaviour.this.comp.getText();
                if (!TextBehaviour.this.mousePressed) {
                    TextBehaviour.this.comp.selectAll();
                }
            }
        });
        this.comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                TextBehaviour.this.mousePressed = true;
            }

            public void mouseReleased(MouseEvent e) {
                // don't override the user selection
                if (TextBehaviour.this.gained && TextBehaviour.this.comp.getSelectedText() == null) {
                    TextBehaviour.this.comp.selectAll();
                }
                // don't select all for each mouse released
                TextBehaviour.this.gained = false;
                TextBehaviour.this.mousePressed = false;
            }
        });
        this.comp.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent keyEvent) {
                // Sert a annuler une saisie
                if (keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    TextBehaviour.this.comp.setText(TextBehaviour.this.initialText);
                    TextBehaviour.this.comp.selectAll();
                }
            }
        });
    }

}
