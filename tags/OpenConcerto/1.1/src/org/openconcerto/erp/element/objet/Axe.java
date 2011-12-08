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

public class Axe {

    private int id;
    private String nom;
    private boolean modif;
    private boolean suppression;
    private boolean creation;
    private boolean deleted;

    public Axe(final int id, final String nom) {
        this.id = id;
        this.nom = nom;
        this.modif = false;
        this.suppression = false;
        this.creation = false;
        this.deleted = false;
    }

    public Axe(final int id, final String nom, final boolean creation) {
        this.id = id;
        this.nom = nom;
        this.modif = false;
        this.suppression = false;
        this.deleted = false;
        this.creation = creation;
    }

    @Override
    public String toString() {

        return "ID : " + this.id + ", NOM : " + this.nom;
    }

    public int getId() {

        return this.id;
    }

    public String getNom() {

        return this.nom;
    }

    public boolean getModif() {
        return this.modif;
    }

    public boolean getSuppression() {
        return this.suppression;
    }

    public boolean getCreation() {
        return this.creation;
    }

    public boolean getDeleted() {
        return this.deleted;
    }

    public void setId(final int id) {
        this.modif = true;
        this.id = id;
    }

    public void setNom(final String nom) {
        if (nom.trim().length() == 0) {
            this.nom = "Axe";
        } else {
            this.nom = nom;
        }
        this.modif = true;
    }

    public void setSuppression(final boolean b) {
        this.suppression = b;
    }

    public void setCreation(final boolean b) {
        this.creation = b;
    }

    public void setModif(final boolean b) {
        this.modif = b;
    }

    public void setDeleted(final boolean b) {
        this.deleted = b;
    }
}
