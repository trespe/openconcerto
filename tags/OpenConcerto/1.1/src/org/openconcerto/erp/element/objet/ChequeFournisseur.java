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

import java.util.Date;

public class ChequeFournisseur {
    private final int id;
    private final int idFourn;
    private final String typeFourn;
    private final String nomFourn;
    private final int idMvt;
    private final Date dateAchat;
    private final Date dateMinDepot;
    private final long montant;

    public ChequeFournisseur(final int id, final int idFourn, final String typeFourn, final String nomFourn, final int idMvt, final Date dateAchat, final Date dateMinDepot, final long montant) {
        this.id = id;
        this.idFourn = idFourn;
        this.idMvt = idMvt;
        this.dateAchat = dateAchat;
        this.dateMinDepot = dateMinDepot;
        this.typeFourn = typeFourn;
        this.nomFourn = nomFourn;
        this.montant = montant;
    }

    public int getId() {
        return this.id;
    }

    public int getIdFournisseur() {
        return this.idFourn;
    }

    public int getIdMvt() {
        return this.idMvt;
    }

    public Date getDateAchat() {
        return this.dateAchat;
    }

    public Date getDateMinDecaisse() {
        return this.dateMinDepot;
    }

    public String getTypeFournisseur() {
        return this.typeFourn;
    }

    public String getNomFournisseur() {
        return this.nomFourn;
    }

    public long getMontant() {
        return this.montant;
    }
}
