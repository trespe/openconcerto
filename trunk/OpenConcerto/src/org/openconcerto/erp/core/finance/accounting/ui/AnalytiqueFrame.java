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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;

// TODO vérifier si on place une repartition sur un compte
// et qu'ensuite le compte devient racine ???????
public class AnalytiqueFrame extends JFrame {

    // private JButton jButtonAppliquer;
    // private JButton jButtonAnnuler;
    private SQLComponent component;
    private JScrollPane p;
    private boolean frameResize;

    /**
     * Fenetre Analytique
     */
    public AnalytiqueFrame(SQLElement elt) {
        super("Gérer les " + elt.getPluralName());

        this.frameResize = false;
        this.component = elt.createComponent();

        this.component.uiInit();

        this.uiInit();
        this.setLocation(0, 50);
        this.viewResized();
        if (Boolean.getBoolean("org.openconcerto.editframe.noborder")) {
            this.setInnerBorder(null);
        }
    }

    private final void uiInit() {

        this.fill();
    }

    private void fill() {
        Container container = this.getContentPane();

        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1;
        c.weightx = 0;
        c.insets = new Insets(2, 2, 1, 2);
        // container.add(new JLabel(this.getTitle()), c);

        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;

        this.p = new JScrollPane(this.component);
        this.p.setOpaque(false);

        // definint la taille miminim a afficher en bas (ne pas le virer)
        // p.setMinimumSize(d);

        container.add(this.p, c);
        c.gridy++;

        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.NONE;
    }

    /**
     * Redimensionne la frame pour qu'elle soit de taille maximum sans déborder de l'écran. <img
     * src="doc-files/resizeFrame.png"/>
     */
    protected void viewResized() {
        if (!this.frameResize) {
            System.out.println("ViewResized");
            // MAYBE remonter la frame pour ne pas qu'elle dépasse en bas
            final Dimension viewSize = this.component.getSize();// this.p.getViewport().getView().getSize();
            // p.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

            final int verticalHidden = viewSize.height - this.p.getVerticalScrollBar().getVisibleAmount();
            final int horizontalHidden = viewSize.width - this.p.getHorizontalScrollBar().getVisibleAmount();

            final Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            final int maxV = ((int) bounds.getMaxY()) - this.getY();
            final int maxH = ((int) bounds.getMaxX()) - this.getX();

            final Dimension frameSize = this.getSize();
            System.out.println("Avant: Frame: size:" + this.getSize());
            final int vertical = Math.min(frameSize.height + verticalHidden /*
                                                                             * +verticalHidden++this.getInsets().top+this.getInsets().bottom +
                                                                             * verticalHidden
                                                                             */, maxV);
            final int horizontal = Math.min(frameSize.width + horizontalHidden /* viewSize.width *//*
                                                                                                     * +
                                                                                                     * this.getInsets().left+this.getInsets().right
                                                                                                     */, maxH);
            /*
             * System.out.println("\nView: vertical:"+vertical); System.out.println("Frame:
             * size:"+this.getSize()); System.out.println("Viewport:
             * minsize:"+this.p.getViewport().getView().getMinimumSize());
             * System.out.println("Viewport:
             * maxfsize:"+this.p.getViewport().getView().getMaximumSize());
             * System.out.println("Viewport: size:"+this.p.getViewport().getView().getSize());
             * System.out.println("Viewport:
             * prefsize:"+this.p.getViewport().getView().getPreferredSize());
             * System.out.println("Frame: ContentPane: size:"+this.getContentPane().getSize());
             */
            this.setSize(horizontal, vertical);
            System.out.println("Resultat: Frame: size:" + this.getSize());

        }
        this.setFrameResize(false);
        // p.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    }

    protected void setFrameResize(boolean b) {
        this.frameResize = b;
    }

    public void setInnerBorder(Border b) {
        this.p.setBorder(b);
    }
}
