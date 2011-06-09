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

public class Association {

    private int id;
    private transient final int idCompte;
    private int idRep;

    private boolean creation;
    private boolean modification;
    private boolean suppression;

    public Association(final int id, final int id_compte, final int id_rep) {
        this.id = id;
        this.idCompte = id_compte;
        this.idRep = id_rep;
        this.creation = false;
        this.modification = false;
        this.suppression = false;
    }

    public Association(final int id, final int id_compte, final int id_rep, final boolean cree) {
        this.id = id;
        this.idCompte = id_compte;
        this.idRep = id_rep;
        this.creation = cree;
        this.modification = false;
        this.suppression = false;
    }

    public int getId() {
        return this.id;
    }

    public int getIdCompte() {
        return this.idCompte;
    }

    public int getIdRep() {
        return this.idRep;
    }

    public boolean getModification() {
        return this.modification;
    }

    public boolean getCreation() {
        return this.creation;
    }

    public boolean getSuppression() {
        return this.suppression;
    }

    public void setId(final int id) {
        this.modification = true;
        this.id = id;
    }

    public void setIdRep(final int id) {
        this.modification = true;
        this.idRep = id;
    }

    public void setModification(final boolean b) {
        this.modification = b;
    }

    public void setCreation(final boolean b) {
        this.creation = b;
    }

    public void setSuppression(final boolean b) {
        this.suppression = b;
    }

    @Override
    public String toString() {
        return "Association --> " + this.id + " Compte : " + this.idCompte + " Repartition : " + this.idRep;
    }
}
