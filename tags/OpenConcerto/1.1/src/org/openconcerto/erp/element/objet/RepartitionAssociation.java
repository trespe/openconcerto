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

public class RepartitionAssociation {

    private int id, idAxe;
    private String nom;
    private String valPoste;
    private boolean modif;
    private boolean suppresion;
    private boolean creation;
    private boolean deleted;

    public RepartitionAssociation(final int id, final String nom, final int idAxe, final String valPoste) {
        this.id = id;
        this.nom = nom;
        this.idAxe = idAxe;
        this.valPoste = valPoste;
        this.modif = false;
        this.suppresion = false;
        this.creation = false;
        this.deleted = false;
    }

    public RepartitionAssociation(final int id, final String nom, final int idAxe, final String valPoste, final boolean creation) {
        this.id = id;
        this.nom = nom;
        this.idAxe = idAxe;
        this.valPoste = valPoste;
        this.modif = false;
        this.suppresion = false;
        this.deleted = false;
        this.creation = creation;
    }

    @Override
    public String toString() {
        // return this.nom;
        if (this.valPoste.trim().length() == 0) {
            return this.nom;
        } else {
            return this.nom + " (" + this.valPoste + ")";
        }
    }

    public int getId() {
        return this.id;
    }

    public String getNom() {
        return this.nom;
    }

    public int getIdAxe() {
        return this.idAxe;
    }

    public String valPoste() {
        return this.valPoste;
    }

    public boolean getModif() {
        return this.modif;
    }

    public boolean getSuppression() {
        return this.suppresion;
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
            this.nom = "Répartition";
        } else {
            this.nom = nom;
        }
        this.modif = true;
    }

    public void setIdAxe(final int idAxe) {
        this.modif = true;
        this.idAxe = idAxe;
    }

    public void setValPoste(final String s) {
        this.valPoste = s;
    }

    public void addValPoste(final String s) {
        if (this.valPoste.trim().length() != 0) {
            this.valPoste += ", " + s;
        } else {
            this.valPoste = s;
        }
    }

    public void setSuppression(final boolean b) {
        this.suppresion = b;
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
