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
import java.math.RoundingMode;

import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ArticleSelector extends JPanel implements ListSelectionListener, CaisseListener {
    private ArticleModel model;
    private ScrollableList list;
    private StatusBar comp;
    private CaisseControler controller;

    ArticleSelector(final CaisseControler controller) {
        this.controller = controller;
        this.controller.addCaisseListener(this);

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        comp = new StatusBar("toolbar.png", "toolbar_list.png");
        comp.setPrevious(true);
        comp.setTitle("Articles");
        this.add(comp, c);

        c.weighty = 1;
        c.gridy++;
        model = new ArticleModel();
        model.setCategorie(null);

        final Font f = new Font("Arial", Font.PLAIN, 21);
        list = new ScrollableList(model) {
            int maxStringWidth = 0;

            @Override
            public void paint(Graphics g) {

                if (maxStringWidth == 0) {
                    g.setFont(f);
                    int w = this.getWidth();
                    int priceWidth = (int) g.getFontMetrics(f).getStringBounds(getPrice(BigDecimal.valueOf(999)), g).getWidth();
                    int maxW = w - priceWidth - getLeftMargin();
                    String str = "a";
                    int strW;
                    do {
                        strW = (int) g.getFontMetrics(f).getStringBounds(str, g).getWidth();
                        str += "a";
                    } while (strW < maxW);

                    maxStringWidth = Math.max(1, str.length() - 1);
                    System.out.println(w + " " + priceWidth + " " + maxStringWidth);

                }
                super.paint(g);
                g.setColor(Color.GRAY);
                g.drawLine(0, 0, 0, this.getHeight());
            }

            @Override
            public void paintCell(Graphics g, Object object, int index, boolean isSelected, int posY) {
                Article article = (Article) object;
                paintArticle(f, g, article, isSelected, posY, this.getWidth(), this.getCellHeight(), maxStringWidth, getLeftMargin());

            }
        };

        list.setFixedCellHeight(64);
        list.setOpaque(true);
        this.add(list, c);
        list.addListSelectionListener(this);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int nb = e.getClickCount();
                if (nb > 1) {
                    Object sel = list.getSelectedValue();
                    if (sel != null) {
                        Article article = (Article) sel;
                        controller.incrementArticle(article);
                        controller.setArticleSelected(article);
                    }
                }
            }
        });
        comp.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                controller.switchListMode();
            }
        });

    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        Object sel = list.getSelectedValue();
        if (sel != null && !e.getValueIsAdjusting()) {
            Article article = (Article) sel;
            controller.setArticleSelected(article);
            controller.addArticle(article);
        }
    }

    public ArticleModel getModel() {
        return this.model;
    }

    @Override
    public void caisseStateChanged() {

        final Article articleSelected = controller.getArticleSelected();
        if (articleSelected == null) {
            return;
        }

        Object selectedValue = null;
        try {
            selectedValue = list.getSelectedValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (articleSelected != null && !articleSelected.equals(selectedValue)) {
            Categorie c = articleSelected.getCategorie();
            model.setCategorie(c);
            list.setSelectedValue(articleSelected, true);
        }

    }

    public static void paintArticle(final Font f, Graphics g, Article article, boolean isSelected, int posY, int cellWidth, int cellHeight, int maxWidth, int leftMargin) {

        g.setFont(f);

        if (isSelected) {
            g.setColor(new Color(232, 242, 254));
        } else {
            g.setColor(Color.WHITE);
        }
        g.fillRect(0, posY, cellWidth, cellHeight);

        //
        g.setColor(Color.GRAY);
        g.drawLine(0, posY + cellHeight - 1, cellWidth, posY + cellHeight - 1);

        if (isSelected) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(Color.GRAY);
        }
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String label = article.getName();

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

        final BigDecimal priceInCents = article.getPriceInCents();
        String euro = getPrice(priceInCents);

        int wEuro = (int) g.getFontMetrics().getStringBounds(euro, g).getWidth();
        if (label2 == null) {
            g.drawString(label, leftMargin, posY + 39);
        } else {
            g.drawString(label, leftMargin, posY + 26);
            g.drawString(label2, leftMargin, posY + 52);
        }
        g.drawString(euro, cellWidth - 5 - wEuro, posY + 39);
    }

    public int getLeftMargin() {
        return 10;
    }

    public static String getPrice(final BigDecimal price) {
        return TicketCellRenderer.centsToString(price.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue()) + "€";
    }

}
