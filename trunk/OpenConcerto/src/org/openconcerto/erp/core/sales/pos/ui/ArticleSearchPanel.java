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
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ArticleSearchPanel extends JPanel implements ListSelectionListener, CaisseListener {
    private final ScrollableList list;
    private final CaisseControler controler;

    public ArticleSearchPanel(final CaisseControler controler) {
        this.controler = controler;
        this.controler.addCaisseListener(this);
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        final FilteredListModel model = new FilteredListModel();
        final Font f1 = new Font("Arial", Font.PLAIN, 24);
        final Font f2 = new Font("Arial", Font.PLAIN, 16);
        list = new ScrollableList(model) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.setColor(Color.GRAY);
                g.drawLine(0, 0, 0, this.getHeight());
            }

            @Override
            public void paintCell(Graphics g, Object object, int index, boolean isSelected, int posY) {
                if (object instanceof Article) {
                    Article article = (Article) object;
                    ArticleSelector.paintArticle(f1, g, article, isSelected, posY, this.getWidth(), this.getCellHeight(), 36, 10);
                } else if (object instanceof Categorie) {
                    Categorie c = (Categorie) object;
                    paintCategorie(f1, f2, g, c, isSelected, posY, this.getWidth(), this.getCellHeight());

                }
            }
        };

        // Bar
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        StatusBar bar = new StatusBar("toolbar.png", "toolbar_list.png");
        bar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                controler.switchListMode();
            }
        });
        bar.setPrevious(true);
        bar.setTitle("Articles");
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 2;
        this.add(bar, c);

        // List
        c.weighty = 1;
        c.gridy++;
        this.add(list, c);
        // Separator
        c.weighty = 0;
        c.gridy++;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);

        // Icon and text
        c.weighty = 0;
        c.gridy++;
        c.insets = new Insets(3, 3, 3, 3);
        final JLabel label = new JLabel(new ImageIcon(this.getClass().getResource("search.png")));
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(label, c);
        final JTextField textField = new JTextField();
        textField.setFont(f1);
        c.weightx = 1;
        c.gridx++;

        textField.setFont(f1);
        this.add(textField, c);
        textField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                model.setFilter(textField.getText());
            }
        });
        list.addListSelectionListener(this);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int nb = e.getClickCount();
                if (nb > 1) {
                    Object sel = list.getSelectedValue();
                    if (sel != null) {
                        Article article = (Article) sel;
                        controler.incrementArticle(article);
                        controler.setArticleSelected(article);
                    }
                }
            }
        });
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        Object sel = list.getSelectedValue();
        if (sel != null && !e.getValueIsAdjusting()) {
            if (sel instanceof Article) {
                Article article = (Article) sel;
                controler.setArticleSelected(article);
                controler.addArticle(article);
            }
        }
    }

    public void paintCategorie(final Font f, Font f2, Graphics g, Categorie c, boolean isSelected, int posY, int cellWidth, int cellHeight) {
        g.setFont(f);

        g.setColor(Color.WHITE);

        g.fillRect(0, posY, cellWidth, cellHeight);

        //
        g.setColor(Color.GRAY);
        g.drawLine(0, posY + cellHeight - 1, cellWidth, posY + cellHeight - 1);

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String label = c.getName();
        final int MAX_WIDTH = 36;
        final int MAX_WIDTH2 = 48;
        if (label.length() > MAX_WIDTH * 2) {
            label = label.substring(0, MAX_WIDTH * 2) + "...";
        }
        String label2 = getCategoriePath(c);
        if (label2 != null) {

            if (label2.length() > MAX_WIDTH2) {
                label2 = label2.substring(0, MAX_WIDTH2) + "...";
            }
        }
        g.setColor(Color.BLACK);
        if (label2 == null) {
            g.drawString(label, 10, posY + 39);
        } else {
            g.drawString(label, 10, posY + 26);
            g.setColor(Color.DARK_GRAY);
            g.setFont(f2);
            g.drawString(label2, 10, posY + 52);
        }
    }

    private Map<Categorie, String> categoryCache = new HashMap<Categorie, String>();

    private String getCategoriePath(Categorie c) {
        if (c.getParent() == null) {
            return null;
        }
        String storedString = categoryCache.get(c);
        if (storedString != null) {
            return storedString;
        }

        Categorie parent = c.getParent();
        String s = "";

        Set<Categorie> set = new HashSet<Categorie>();
        set.add(c);
        while (parent != null) {
            if (set.contains(parent)) {
                System.err.println("ArticleSearchPanel.getCategoriePath() loop detected for category " + c + " " + parent + " already in " + set);
                break;
            }
            set.add(parent);
            if (s.length() > 0) {
                s = parent.getName() + " / " + s;
            } else {
                s = parent.getName();
            }

            parent = c.getParent();

        }
        categoryCache.put(c, s);
        return s;
    }

    @Override
    public void caisseStateChanged() {

        final Article articleSelected = controler.getArticleSelected();
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
            list.setSelectedValue(articleSelected, true);
        }

    }
}
