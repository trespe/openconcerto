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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CategorieSelector extends JPanel implements ListSelectionListener, CaisseListener {
    private CategorieModel model;
    private JList list;
    private StatusBar comp;
    private Categorie previous;
    private ArticleModel articleModel;
    private CaisseControler controller;

    CategorieSelector(CaisseControler controller, final ArticleModel articleModel) {
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
        this.list = new JList(this.model);
        this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.list.setCellRenderer(new CategorieListCellRenderer());
        this.list.setFixedCellHeight(64);
        this.add(this.list, c);
        this.list.getSelectionModel().addListSelectionListener(this);

        this.comp.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                final Categorie prev = CategorieSelector.this.previous;
                CategorieSelector.this.model.setRoot(prev);
                CategorieSelector.this.list.clearSelection();
                if (prev == null) {
                    CategorieSelector.this.comp.setTitle("Catégories");
                    CategorieSelector.this.comp.setPrevious(false);
                    articleModel.setCategorie(null);
                } else {
                    CategorieSelector.this.comp.setTitle(prev.getName());
                    CategorieSelector.this.comp.setPrevious(true);
                }
            }

        });
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        Object sel = this.list.getSelectedValue();
        if (sel != null && !e.getValueIsAdjusting()) {
            Categorie c = (Categorie) sel;
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
            Categorie c = articleSelected.getCategorie();
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
