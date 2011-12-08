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
 
 package org.openconcerto.erp.element.objet;

public class Journal {
    private final int id;
    private final String nom, code;

    public Journal(final int id, final String nom, final String code) {
        this.id = id;
        this.nom = nom;
        this.code = code;
    }

    public int getId() {
        return this.id;
    }

    public String getNom() {
        return this.nom;
    }

    public String getCode() {
        return this.code;
    }

    @Override
    public String toString() {
        return "Journal id : " + this.id + " nom : " + this.nom + " code : " + this.code;
    }
}
