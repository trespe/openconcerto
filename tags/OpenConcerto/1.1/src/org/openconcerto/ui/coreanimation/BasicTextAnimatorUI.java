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
 
 package org.openconcerto.ui.coreanimation;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import javax.swing.text.Position.Bias;

public class BasicTextAnimatorUI extends BasicTextUI implements Pulse {

    private BasicTextUI originalUi;
    private boolean isAnimating = false;
    int x = 0;
    int sens = 1;
    private JTextField f;
    protected Thread thread;

    public void stopAnimation() {
        this.setAnimating(false);
    }

    public void startAnimation() {
        this.setAnimating(true);

    }

    public void pulse() {

        if (sens > 0) {
            x += 2;
            if (x > f.getWidth()) {
                x = f.getWidth() - 5;
                sens = -1;
            }
        } else {
            x -= 2;
            if (x < -300) {
                x = 0;
                sens = 1;
            }
        }
        if (x >= 0)
            f.repaint();
    }

    synchronized void setAnimating(boolean isAnimating) {
        this.isAnimating = isAnimating;
    }

    synchronized boolean isAnimating() {
        return this.isAnimating;
    }

    // --------------------------------------

    public BasicTextAnimatorUI(BasicTextUI ui, JTextField f) {
        this.originalUi = ui;
        this.f = f;

        startAnimation();
    }

    @Override
    protected void paintBackground(Graphics g) {
        // super.paintBackground(g);

    }

    @Override
    public void damageRange(JTextComponent t, int p0, int p1) {
        this.originalUi.damageRange(t, p0, p1);

    }

    @Override
    public void damageRange(JTextComponent t, int p0, int p1, Bias firstBias, Bias secondBias) {
        this.originalUi.damageRange(t, p0, p1, firstBias, secondBias);

    }

    @Override
    public EditorKit getEditorKit(JTextComponent t) {

        return this.originalUi.getEditorKit(t);
    }

    @Override
    public int getNextVisualPositionFrom(JTextComponent t, int pos, Bias b, int direction, Bias[] biasRet) throws BadLocationException {

        return this.originalUi.getNextVisualPositionFrom(t, pos, b, direction, biasRet);
    }

    @Override
    public View getRootView(JTextComponent t) {
        return this.originalUi.getRootView(t);
    }

    @Override
    public Rectangle modelToView(JTextComponent t, int pos) throws BadLocationException {
        return this.originalUi.modelToView(t, pos);
    }

    @Override
    public Rectangle modelToView(JTextComponent t, int pos, Bias bias) throws BadLocationException {

        return this.originalUi.modelToView(t, pos, bias);
    }

    @Override
    public int viewToModel(JTextComponent t, Point pt) {
        return this.originalUi.viewToModel(t, pt);
    }

    @Override
    public int viewToModel(JTextComponent t, Point pt, Bias[] biasReturn) {

        return this.originalUi.viewToModel(t, pt, biasReturn);
    }

    @Override
    protected String getPropertyPrefix() {

        return "TextField";
    }

    /**
     * From ComponentUI
     */

    public void installUI(JComponent c) {
        this.originalUi.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        this.originalUi.uninstallUI(c);
    }

    int a[] = new int[] { 252, 250, 245, 240, 235, 230, 220, 210, 170, 150, 50, 0 };

    public void update(Graphics g, JComponent c) {

        g.setColor(f.getBackground());
        g.fillRect(0, 0, f.getWidth(), f.getHeight());
        final int height = c.getHeight();
        for (int i = 0; i < 12; i++) {
            g.setColor(new Color(255, a[i], a[i]));

            g.drawLine(x + i * sens, 0, x + i * sens, height);

        }
        f.setOpaque(false);
        this.originalUi.update(g, c);

    }

    public Dimension getPreferredSize(JComponent c) {
        return this.originalUi.getPreferredSize(c);
    }

    public Dimension getMinimumSize(JComponent c) {
        return this.originalUi.getMinimumSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return this.originalUi.getMaximumSize(c);
    }

    public boolean contains(JComponent c, int x, int y) {
        return this.originalUi.contains(c, x, y);
    }

    /*
     * public static ComponentUI createUI(JComponent c) { return this.originalUi.c }
     */

    public int getBaseline(JComponent c, int width, int height) {
        return this.originalUi.getBaseline(c, width, height);
    }

    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        return this.originalUi.getBaselineResizeBehavior(c);
    }

    public int getAccessibleChildrenCount(JComponent c) {
        return this.originalUi.getAccessibleChildrenCount(c);
    }

    public Accessible getAccessibleChild(JComponent c, int i) {
        return this.originalUi.getAccessibleChild(c, i);
    }

    public void resetState() {
        // TODO Auto-generated method stub

    }
}
