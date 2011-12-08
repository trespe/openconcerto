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

public class Ecriture {

    private final int id, numMvt, idMvt;
    private final String nom, journal;
    private final long debit, credit;
    private final boolean valide;
    private final Date date;

    public Ecriture(final int id, final String nom, final int idMvt, final int numMvt, final String journal, final Date date, final long debit, final long credit, final boolean valide) {
        this.id = id;
        this.nom = nom;
        this.idMvt = idMvt;
        this.numMvt = numMvt;
        this.journal = journal;
        this.date = date;
        this.credit = credit;
        this.debit = debit;
        this.valide = valide;
    }

    public int getId() {
        return this.id;
    }

    public String getNom() {
        return this.nom;
    }

    public int getIdMvt() {
        return this.idMvt;
    }

    public int getNumMvt() {
        return this.numMvt;
    }

    public String getJournal() {
        return this.journal;
    }

    public Date getDate() {
        return this.date;
    }

    public long getCredit() {
        return this.credit;
    }

    public long getDebit() {
        return this.debit;
    }

    public boolean getValide() {
        return this.valide;
    }

    @Override
    public String toString() {

        return "ID: " + this.id + " NOM: " + this.nom + " MVT: " + this.numMvt + " JRNL: " + this.journal + " CREDIT: " + this.credit + " DEBIT: " + this.debit + " Vld: " + this.valide;
    }

}
