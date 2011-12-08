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
 
 package org.openconcerto.erp.core.sales.product.model;

public class Article {
    private int id;
    public Article(int id) {
        this.id=id;
    }
    public String getNomPourQuantiteMetrique() {
        // Recuperer METRIQUE.NOM de ARTICLE.ID_METRIQUE_QTE_METRIQUE de l'article ID=id
        return "Article.getUnitePourQuantite a coder";
    }
    public String getUnitePourQuantiteMetrique() {
        // Recuperer METRIQUE.UNITE de ARTICLE.ID_METRIQUE_QTE_METRIQUE de l'article ID=id
        return "Article.getUnitePourQuantite a coder";
    }

}
