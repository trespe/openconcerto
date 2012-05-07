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

import java.util.HashMap;
import java.util.Map;

public class Article {
    private Categorie s;
    private String name;
    int priceInCents;
    int idTaxe;
    int priceHTInCents;
    String barCode = "empty barcode";
    String code = "";
    private final int id;
    private static Map<String, Article> codes = new HashMap<String, Article>();

    public Article(Categorie s1, String string, int id) {
        this.s = s1;
        this.id = id;
        this.name = string;
        s1.addArticle(this);
    }

    public int getId() {
        return this.id;
    }

    public int getPriceHTInCents() {
        return this.priceHTInCents;
    }

    public void setPriceHTInCents(int priceHTInCents) {
        this.priceHTInCents = priceHTInCents;
    }

    public int getIdTaxe() {
        return this.idTaxe;
    }

    public void setIdTaxe(int idTaxe) {
        this.idTaxe = idTaxe;
    }

    public void setBarCode(String bar) {
        this.barCode = bar;
        codes.put(bar, this);
    }

    public void setPriceInCents(int priceInCents) {
        this.priceInCents = priceInCents;
    }

    public int getPriceInCents() {
        return this.priceInCents;
    }

    public String getName() {
        return this.name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    };

    public String getBarCode() {
        return this.barCode;
    }

    public Categorie getCategorie() {
        return this.s;
    }

    @Override
    public String toString() {

        return "Article:" + this.name + " " + this.priceInCents + " cents";
    }

    public static Article getArticleFromBarcode(String code) {
        return codes.get(code);
    }
}
