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
 
 package org.openconcerto.erp.core.sales.pos.model;


import java.util.ArrayList;

import java.util.List;

public class Categorie {
    private static List<Categorie> topLevelCategories = new ArrayList<Categorie>();
    private String name;
    // Sous catégories
    private List<Categorie> l = new ArrayList<Categorie>();
    // Articles dans cette categorie
    private List<Article> articles = new ArrayList<Article>();
    private Categorie parent;

    public Categorie(String string) {
        this(string, false);
    }

    public Categorie(String string, boolean top) {
        this.name = string;
        if (top)
            topLevelCategories.add(this);
    }

    @Override
    public String toString() {

        return name;
    }

    public void add(Categorie s) {
        l.add(s);
        s.setParentCategorie(this);
    }

    private void setParentCategorie(Categorie categorie) {
        this.parent = categorie;

    }

    public Categorie getParent() {
        return parent;
    }

    void addArticle(Article a) {
        this.articles.add(a);
    }

    public static List<Categorie> getTopLevelCategories() {

        return topLevelCategories;
    }

    public List<Categorie> getSubCategories() {

        return l;
    }

    public String getName() {
        return name;
    }

    public List<Article> getArticles() {
        System.out.println("\nArticles de " + this.getName());
        List<Article> result = new ArrayList<Article>();
        result.addAll(articles);
        for (Categorie c : l) {
            System.out.println("\nArticles de sous cat:" + c.getName());
            result.addAll(c.getArticles());
        }

        System.out.println(result);
        return result;
    }
}
