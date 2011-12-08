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
 
 package org.openconcerto.erp.generationDoc.gestcomm;

import org.openconcerto.erp.generationDoc.SheetInterface;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.Tuple2;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ReleveChequeEmisSheet extends SheetInterface {

    private static final SQLTable tableCheque = base.getTable("CHEQUE_FOURNISSEUR");
    private static final SQLTable tableFourn = base.getTable("FOURNISSEUR");
    private List<Integer> listeCheques;

    public ReleveChequeEmisSheet(List<Integer> idCheques) {
        this.mapReplace = new HashMap();
        this.mapStyleRow = new HashMap();
        this.mCell = new HashMap();
        this.row = tableCheque.getRow(idCheques.get(0));
        // super(idCheques.get(0), tableCheque);
        this.listeCheques = idCheques;
        init();
        createMap();
    }

    public static final String TEMPLATE_ID = "ReleveChequeEmis";
    public static final String TEMPLATE_PROPERTY_NAME = "LocationReleveCheque";

    private void init() {
        this.modele = "ReleveChequeEmis.ods";
    }

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected String getYear() {
        return "";
    }

    protected void createMap() {

        long montantTotal = 0;
        // Element Cheques
        int pos = 5;
        for (Integer i : this.listeCheques) {

            SQLRow rowTmp = tableCheque.getRow(i.intValue());
            SQLRow rowFournTmp = tableFourn.getRow(rowTmp.getInt("ID_FOURNISSEUR"));

            Object nomTmp = rowFournTmp.getObject("NOM");
            this.mCell.put("B" + pos, nomTmp);

            Long montant = (Long) rowTmp.getObject("MONTANT");
            montantTotal += montant;
            this.mCell.put("L" + pos, new Double(GestionDevise.currencyToString(montant.longValue(), false)));

            pos++;
        }

        // Date
        this.mCell.put("C45", (Date) this.row.getObject("DATE_DECAISSE"));

        // Total
        this.mCell.put("L45", new Double(GestionDevise.currencyToString(montantTotal, false)));
    }
}
