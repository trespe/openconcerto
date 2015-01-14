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
import org.openconcerto.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

public class FilteredListModel extends AbstractListModel {
    private final List<Object> items = new ArrayList<Object>();
    private final Stack<String> searches = new Stack<String>();
    Thread t;

    FilteredListModel() {
        final List<Categorie> l = new ArrayList<Categorie>(Categorie.getAllCategories());
        Collections.sort(l, new Comparator<Categorie>() {

            @Override
            public int compare(Categorie o1, Categorie o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        for (Categorie categorie : l) {
            final List<Article> articles = categorie.getArticles();
            if (!articles.isEmpty()) {
                items.add(categorie);
                items.addAll(articles);
            }
        }
        t = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {

                    String s = null;
                    synchronized (searches) {
                        if (!searches.isEmpty()) {
                            s = searches.lastElement().toLowerCase();
                            searches.clear();
                        }
                    }
                    if (s != null) {
                        final List<Object> newitems = new ArrayList<Object>();
                        for (Categorie categorie : l) {
                            final List<Article> allArticles = categorie.getArticles();

                            if (s.trim().isEmpty()) {
                                if (!allArticles.isEmpty()) {
                                    newitems.add(categorie);
                                    newitems.addAll(allArticles);
                                }

                            } else {
                                String[] parts = StringUtils.fastSplit(s, ' ').toArray(new String[] {});
                                final int length = parts.length;
                                final List<Article> articles = new ArrayList<Article>();
                                int size = allArticles.size();
                                for (int i = 0; i < size; i++) {
                                    Article a = allArticles.get(i);
                                    final String name = a.getName().toLowerCase() + a.getCode().toLowerCase() + a.getBarCode().toLowerCase();
                                    for (int j = 0; j < length; j++) {

                                        if (name.contains(parts[j])) {
                                            if (j == length - 1) {
                                                articles.add(a);
                                            }

                                        } else {
                                            break;
                                        }
                                    }
                                }

                                if (!articles.isEmpty()) {
                                    newitems.add(categorie);
                                    newitems.addAll(articles);
                                }
                            }
                        }
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                items.clear();
                                items.addAll(newitems);
                                fireContentsChanged(this, 0, items.size());
                            }
                        });
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        });
        t.setName("FilteredListModel search");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public int getSize() {
        return items.size();
    }

    @Override
    public Object getElementAt(int index) {
        return items.get(index);
    }

    public void setFilter(String text) {
        System.err.println("FilteredListModel.setFilter() " + text);
        items.clear();
        synchronized (searches) {
            searches.add(text);
        }
    }

}
