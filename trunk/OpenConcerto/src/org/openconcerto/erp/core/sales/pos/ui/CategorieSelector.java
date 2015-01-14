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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Categorie;
import org.openconcerto.ui.touch.ScrollableList;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CategorieSelector extends JPanel implements ListSelectionListener, CaisseListener {
    private CategorieModel model;
    private ScrollableList list;
    private StatusBar comp;
    private Categorie previous;
    private ArticleModel articleModel;
    private CaisseControler controller;

    CategorieSelector(CaisseControler controller, final ArticleModel articleModel) {
        this.setBackground(Color.WHITE);
        this.articleModel = articleModel;
        this.controller = controller;

        controller.addCaisseListener(this);

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.comp = new StatusBar();
        this.comp.setTitle("Catégories");
        this.add(this.comp, c);

        c.weighty = 1;
        c.gridy++;
        this.model = new CategorieModel();
        this.model.setRoot(null);
        final Font f = new Font("Arial", Font.PLAIN, 36);
        final Font smallFont = new Font("Arial", Font.PLAIN, 23);
        this.list = new ScrollableList(this.model) {
            int maxWidth = 0;

            @Override
            public void paint(Graphics g) {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                if (maxWidth == 0) {
                    g.setFont(f);
                    int w = this.getWidth();

                    int maxW = w - 2 * getLeftMargin();
                    String str = "a";
                    int strW;
                    do {
                        strW = (int) g.getFontMetrics(f).getStringBounds(str, g).getWidth();
                        str += "a";
                    } while (strW < maxW);

                    maxWidth = Math.max(1, str.length() - 1);
                    System.out.println(w + " " + getLeftMargin() + " " + maxWidth);

                }
                super.paint(g);
            }

            private int getLeftMargin() {

                return 10;
            }

            public void paintCell(Graphics g, Object object, int index, boolean selected, int posY) {

                g.setColor(Color.WHITE);

                g.fillRect(0, posY, this.getWidth(), this.getCellHeight());
                g.setColor(Color.GRAY);

                g.drawLine(0, posY + this.getCellHeight() - 1, this.getWidth(), posY + this.getCellHeight() - 1);

                if (selected) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.GRAY);
                }
                String label = object.toString();

                if (label.length() > maxWidth * 2) {
                    label = label.substring(0, maxWidth * 2) + "...";
                }
                String label2 = null;
                if (label.length() > maxWidth) {
                    String t = label.substring(0, maxWidth).trim();
                    int lastSpace = t.lastIndexOf(' ');
                    if (lastSpace <= 0) {
                        lastSpace = maxWidth;
                    }
                    label2 = label.substring(lastSpace).trim();
                    label = label.substring(0, lastSpace).trim();
                    if (label2.length() > maxWidth) {
                        label2 = label2.substring(0, maxWidth) + "...";
                    }
                }

                if (label2 == null) {
                    g.setFont(f);
                    g.drawString(label, getLeftMargin(), posY + 44);
                } else {
                    g.setFont(smallFont);
                    g.drawString(label, getLeftMargin(), posY + 26);
                    g.drawString(label2, getLeftMargin(), posY + 52);
                }

            }

        };
        this.list.setFixedCellHeight(64);
        this.list.setOpaque(true);
        this.list.setBackground(Color.WHITE);
        this.add(this.list, c);
        this.list.addListSelectionListener(this);

        this.comp.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                // User pressed "Previous" button on category
                final Categorie newCategory = CategorieSelector.this.previous;
                CategorieSelector.this.model.setRoot(newCategory);
                CategorieSelector.this.list.clearSelection();
                if (newCategory == null) {
                    CategorieSelector.this.comp.setTitle("Catégories");
                    CategorieSelector.this.comp.setPrevious(false);
                    CategorieSelector.this.previous = null;
                } else {
                    CategorieSelector.this.comp.setTitle(newCategory.getName());
                    CategorieSelector.this.comp.setPrevious(true);
                    CategorieSelector.this.previous = newCategory.getParent();
                }
                articleModel.setCategorie(newCategory);
            }

        });
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        final Object sel = this.list.getSelectedValue();
        if (sel != null && !e.getValueIsAdjusting()) {
            final Categorie c = (Categorie) sel;
            if (!c.getSubCategories().isEmpty()) {
                // Descend la hierarchie
                this.previous = this.model.getRoot();
                this.model.setRoot(c);
                this.comp.setTitle(c.getName());
                this.comp.setPrevious(true);
                this.list.clearSelection();
            }
            this.articleModel.setCategorie(c);
            this.controller.setArticleSelected(null);
        }
    }

    @Override
    public void caisseStateChanged() {
        final Article articleSelected = this.controller.getArticleSelected();
        if (articleSelected != null) {
            final Categorie c = articleSelected.getCategorie();
            if (c.getParent() != null) {
                this.previous = c.getParent().getParent();
                this.model.setRoot(c.getParent());
                this.comp.setTitle(c.getParent().getName());
            }
            this.comp.setPrevious(true);
            this.list.setSelectedValue(c, true);
        }

    }
}
