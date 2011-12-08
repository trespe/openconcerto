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

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.event.ChangeListener;
import javax.swing.text.Caret;

public class DisabledCaret implements Caret {

    public void install(javax.swing.text.JTextComponent c) {
    }

    public void deinstall(javax.swing.text.JTextComponent c) {
    }

    public void paint(Graphics g) {
    }

    public void addChangeListener(ChangeListener l) {
    }

    public void removeChangeListener(ChangeListener l) {
    }

    public boolean isVisible() {
        return false;
    }

    public void setVisible(boolean v) {
    }

    public boolean isSelectionVisible() {
        return false;
    }

    public void setSelectionVisible(boolean v) {
    }

    public void setMagicCaretPosition(Point p) {
    }

    public Point getMagicCaretPosition() {
        return new Point(0, 0);
    }

    public void setBlinkRate(int rate) {
    }

    public int getBlinkRate() {
        return 10000;
    }

    public int getDot() {
        return 0;
    }

    public int getMark() {
        return 0;
    }

    public void setDot(int dot) {
    }

    public void moveDot(int dot) {
    }
}
