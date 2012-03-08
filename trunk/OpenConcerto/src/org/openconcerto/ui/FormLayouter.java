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

import org.openconcerto.utils.CollectionUtils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Permet de disposer des champs avec labels en colonnes. Exemple : <img
 * src="doc-files/FormLayouter.png"/>.<br/>
 * 
 * Les champs sont placés grace aux add*().
 * 
 * @author ILM Informatique 2 sept. 2004
 */
public class FormLayouter {

    // Label, gap, Field, gap
    private static final int CELL_WIDTH = 4;
    // row, gap
    private static final int CELL_HEIGHT = 2;

    private static final String BORDER_GAP = "3dlu";
    private static final String ROW_GAP = BORDER_GAP;
    private static final String ROW_HEIGHT = "top:p";

    private final Container co;
    // le nombre de colonnes
    private final int width;
    // le nombre de colonnes par défaut
    private final int defaultWidth;
    // le layout
    private final FormLayout layout;
    private final CellConstraints constraints;
    // les coordonnées de la prochaine cellule
    private int x, y;

    public FormLayouter(Container co, int width) {
        this(co, width, 1);
    }

    public FormLayouter(Container co, int width, int defaultWidth) {
        if (width < 1)
            throw new IllegalArgumentException("width must be at least 1 : " + width);

        this.x = 0;
        this.y = 0;
        this.constraints = new CellConstraints();

        this.co = co;
        this.width = width;
        this.defaultWidth = defaultWidth;
        final String colSpec = BORDER_GAP + ", " + CollectionUtils.join(Collections.nCopies(width, "max(25dlu;p), 5dlu, d:g"), ", 5dlu, ") + ", " + BORDER_GAP;
        final String rowSpec = BORDER_GAP + ", " + ROW_HEIGHT + ", " + BORDER_GAP;
        // tous les fields ont une taille égale
        final int[] colGroups = new int[width];
        for (int i = 0; i < width; i++) {
            colGroups[i] = CELL_WIDTH * (i + 1);
        }

        this.layout = new FormLayout(colSpec, rowSpec);
        this.layout.setColumnGroups(new int[][] { colGroups });
        co.setLayout(this.layout);
    }

    /**
     * Ajout un composant sur une ligne avec la description passee en parametre. Si comp est null,
     * un titre est créé.
     * 
     * @param desc le label du champ
     * @param comp le composant graphique d'edition ou null si titre
     * @return the created label.
     */
    public JLabel add(String desc, Component comp) {
        if (comp != null) {
            return this.add(desc, comp, this.defaultWidth);
        } else {
            this.newLine();
            final JLabel lab = new JLabel(desc);
            lab.setFont(lab.getFont().deriveFont(Font.BOLD, 15));
            this.layout.setRowSpec(this.getY() - 1, new RowSpec("10dlu"));
            this.co.add(lab, this.constraints.xyw(this.getLabelX(), this.getY(), this.width * CELL_WIDTH - 1));
            this.endLine();
            return lab;
        }
    }

    /**
     * Ajout un composant sur une ligne Si comp est null, un titre est créé.
     * 
     * @param desc le label du champ.
     * @param comp le composant graphique d'edition.
     * @param w la largeur, entre 1 et la largeur de ce layout, ou 0 pour toute la largeur.
     * @return the created label.
     * @throws NullPointerException if comp is <code>null</code>.
     * @throws IllegalArgumentException if w is less than 1.
     */
    public JLabel add(String desc, Component comp, int w) {
        w = this.checkArgs(comp, w);

        final int realWidth = this.getRealFieldWidth(w);
        // Guillaume : right alignment like the Mac ; vertically centred for checkboxes
        final JLabel lab = new JLabel(desc);
        this.co.add(lab, this.constraints.xy(this.getLabelX(), this.getY(), CellConstraints.RIGHT, CellConstraints.CENTER));
        this.co.add(comp, this.constraints.xyw(this.getFieldX(), this.getY(), realWidth));
        this.x += w;
        return lab;
    }

    // assure that comp & w are valid, and do a newLine if necessary
    private int checkArgs(Component comp, int w) {
        if (comp == null)
            throw new NullPointerException();
        if (w < 0 || w > this.width)
            throw new IllegalArgumentException("w must be between 0 and " + this.width + " but is : " + w);

        int res = w == 0 ? w = this.width : w;

        if (this.x + res - 1 >= this.width) {
            this.newLine();
        }
        return res;
    }

    public JPanel addBordered(String desc, Component comp) {
        return this.addBordered(desc, comp, this.defaultWidth);
    }

    public JPanel addBordered(String desc, Component comp, int w) {
        w = this.checkArgs(comp, w);

        final int realWidth = w * CELL_WIDTH - 1;
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new GridLayout());
        p.setBorder(BorderFactory.createTitledBorder(desc));
        p.add(comp);

        this.co.add(p, this.constraints.xyw(this.getLabelX(), this.getY(), realWidth));
        this.x += w;
        return p;
    }

    private final int getRealFieldWidth(int w) {
        return (w - 1) * CELL_WIDTH + 1;
    }

    private final int getY() {
        // +1 pour le premier gap, et +1 car formLayout indexé a partir de 1
        return this.y * CELL_HEIGHT + 2;
    }

    private final int getLabelX() {
        return this.x * CELL_WIDTH + 2;
    }

    private final int getFieldX() {
        return this.getLabelX() + 2;
    }

    // next line
    public final void newLine() {
        // only append => remove the BORDER_GAP
        this.layout.removeRow(this.getY() + 1);
        this.layout.appendRow(new RowSpec(ROW_GAP));
        this.layout.appendRow(new RowSpec(ROW_HEIGHT));
        this.layout.appendRow(new RowSpec(BORDER_GAP));

        this.y++;
        this.x = 0;
    }

    /** Finit la ligne actuelle */
    private void endLine() {
        this.x = this.width;
    }

    public JLabel addRight(String desc, Component comp) {
        this.newLine();
        this.x = this.width - 1;
        final JLabel res = this.add(desc, comp);
        this.endLine();
        return res;
    }

    public void add(JButton btn) {
        this.addRight("", btn);
    }

    public final Container getComponent() {
        return this.co;
    }

    public final int getWidth() {
        return this.width;
    }
}
