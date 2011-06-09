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

public class ChequeAEncaisser {
    private transient final int id;
    private transient final int idClient;
    private transient final String typeClient;
    private transient final String nomClient;
    private transient final int idMvt;
    private transient final Date dateVente;
    private transient final Date dateMinDepot;
    private transient final long montant;
    private transient final String source;

    public ChequeAEncaisser(final int id, final int idClient, final String typeClient, final String nomClient, final int idMvt, final Date dateVente, final Date dateMinDepot, final long montant,
            final String source) {
        this.id = id;
        this.idClient = idClient;
        this.idMvt = idMvt;
        this.dateVente = dateVente;
        this.dateMinDepot = dateMinDepot;
        this.typeClient = typeClient;
        this.nomClient = nomClient;
        this.montant = montant;
        this.source = source;
    }

    public int getId() {
        return this.id;
    }

    public int getIdClient() {
        return this.idClient;
    }

    public int getIdMvt() {
        return this.idMvt;
    }

    public Date getDateVente() {
        return this.dateVente;
    }

    public Date getDateMinDepot() {
        return this.dateMinDepot;
    }

    public String getTypeClient() {
        return this.typeClient;
    }

    public String getNomClient() {
        return this.nomClient;
    }

    public long getMontant() {
        return this.montant;
    }

    public String getSource() {
        return this.source;
    }
}
