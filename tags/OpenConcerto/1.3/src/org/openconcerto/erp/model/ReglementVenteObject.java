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
 
 package org.openconcerto.erp.model;

import org.openconcerto.sql.model.SQLRowAccessor;

import java.util.Date;

public class ReglementVenteObject {

    private String numero, modeRegl, lib;
    private long montant;
    private Date dFacture, dReglement;

    private ReglementVenteObject() {
        this.dFacture = new Date();
        this.dReglement = new Date();
        this.numero = "";
        this.modeRegl = "";
        this.montant = 0;
        this.lib = "";
    }

    private ReglementVenteObject(SQLRowAccessor rowFacture, SQLRowAccessor rowReglement) {
        this.numero = rowFacture.getString("NUMERO");
        this.lib = rowFacture.getString("NOM");
        this.dFacture = (Date) rowFacture.getObject("DATE");

        this.dReglement = (Date) rowReglement.getObject("DATE");
        if (rowReglement == null) {
            this.modeRegl = rowFacture.getForeign("ID_MODE_REGLEMENT").getForeign("ID_TYPE_REGLEMENT").getString("NOM");
            this.montant = Long.valueOf(rowFacture.getObject("T_TTC").toString());
        } else {
            this.modeRegl = rowReglement.getForeign("ID_MODE_REGLEMENT").getForeign("ID_TYPE_REGLEMENT").getString("NOM");
            this.montant = Long.valueOf(rowFacture.getObject("T_TTC").toString());
        }
    }

    public Date getDFacture() {
        return dFacture;
    }

    public Date getDReglement() {
        return dReglement;
    }

    public String getModeRegl() {
        return modeRegl;
    }

    public long getMontant() {
        return montant;
    }

    public String getNumero() {
        return numero;
    }

    public String getLib() {
        return lib;
    }
}
