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

public class Compte {
    private int id;
    private String numero;
    private String nom;
    private String infos;
    private long totalDebit;
    private long totalCredit;

    public Compte(final int id, final String numero, final String nom) {
        this.id = id;
        this.numero = numero;
        this.nom = nom;
        this.infos = "";
        this.totalCredit = 0;
        this.totalDebit = 0;
    }

    public Compte(final int id, final String numero, final String nom, final String infos) {
        this.id = id;
        this.numero = numero;
        this.nom = nom;
        this.infos = infos;
        this.totalCredit = 0;
        this.totalDebit = 0;
    }

    public Compte(final int id, final String numero, final String nom, final String infos, final long totalDebit, final long totalCredit) {
        this.id = id;
        this.numero = numero;
        this.nom = nom;
        this.infos = infos;
        this.totalCredit = totalCredit;
        this.totalDebit = totalDebit;
    }

    public int getId() {
        return this.id;
    }

    public String getNumero() {
        return this.numero;
    }

    public String getNom() {
        return this.nom;
    }

    public String getInfos() {
        return this.infos;
    }

    public long getTotalDebit() {
        return this.totalDebit;
    }

    public long getTotalCredit() {
        return this.totalCredit;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public void setNumero(final String num) {
        this.numero = num;
    }

    public void setNom(final String nom) {
        this.nom = nom;
    }

    public void setInfos(final String infos) {
        this.infos = infos;
    }

    public void setTotalDebit(final long f) {
        this.totalDebit = f;
    }

    public void setTotalCredit(final long f) {
        this.totalCredit = f;
    }

    @Override
    public String toString() {
        return "Compte --> " + this.id + " " + this.numero + " " + this.nom + " infos :" + this.infos;
    }
}
