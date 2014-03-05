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
 
 package org.openconcerto.erp.panel.compta;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ExportRelationExpertPanel extends AbstractExport {

    private List<Object[]> data;

    public ExportRelationExpertPanel(DBRoot rootSociete) {
        super(rootSociete, "relationExpert", ".txt");
    }

    @Override
    protected int fetchData(Date from, Date to, SQLRow selectedJournal, boolean onlyNew) {
        final SQLTable tableEcriture = getEcritureT();
        final SQLTable tableMouvement = tableEcriture.getForeignTable("ID_MOUVEMENT");
        final SQLTable tableCompte = tableEcriture.getForeignTable("ID_COMPTE_PCE");
        final SQLTable tableJrnl = tableEcriture.getForeignTable("ID_JOURNAL");
        final SQLTable tablePiece = tableMouvement.getForeignTable("ID_PIECE");

        final SQLSelect sel = createSelect(from, to, selectedJournal, onlyNew);
        sel.addSelect(tableEcriture.getField("NOM"));
        sel.addSelect(tableMouvement.getField("NUMERO"));
        sel.addSelect(tableCompte.getField("NUMERO"));
        sel.addSelect(tableEcriture.getField("DATE"));
        sel.addSelect(tableEcriture.getField("DEBIT"));
        sel.addSelect(tableEcriture.getField("CREDIT"));
        sel.addSelect(tableJrnl.getField("CODE"));
        if (tableEcriture.contains("CODE_CLIENT")) {
            final SQLField fieldCodeClient = tableEcriture.getField("CODE_CLIENT");
            sel.addSelect(fieldCodeClient);
        }
        sel.addSelect(tablePiece.getField("NOM"));

        sel.addFieldOrder(tableEcriture.getField("ID_MOUVEMENT"));
        sel.addFieldOrder(tableCompte.getField("NUMERO"));

        @SuppressWarnings("unchecked")
        final List<Object[]> l = (List<Object[]>) this.getRootSociete().getDBSystemRoot().getDataSource().execute(sel.asString(), new ArrayListHandler());
        this.data = l;
        return l == null ? 0 : l.size();
    }

    @Override
    protected void export(OutputStream bufOut) throws IOException {
        final List<Object[]> l = this.data;
        final boolean containsCodeClient = getEcritureT().contains("CODE_CLIENT");
        final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        final int size = l.size();
        for (int i = 0; i < size; i++) {

            // Ligne à insérer dans le fichier
            final StringBuffer line = new StringBuffer();

            final Object[] tmp = l.get(i);

            // Date
            final Date d = (Date) tmp[3];
            line.append(dateFormat.format(d));
            line.append('\t');
            // Jrnl
            line.append(tmp[6].toString().trim());
            line.append('\t');
            // N° Cpt
            final String cpt = tmp[2].toString().trim();
            line.append(cpt);
            line.append('\t');

            // ?
            line.append('\t');

            // Libellé
            line.append(tmp[0].toString().trim());
            line.append('\t');

            // Debit
            final Long debit = new Long(tmp[4].toString().trim());
            line.append(GestionDevise.currencyToString(debit.longValue()));
            line.append('\t');
            // Credit
            final Long credit = new Long(tmp[5].toString().trim());
            line.append(GestionDevise.currencyToString(credit.longValue()));
            line.append('\t');
            line.append('E');

            int z = 7;

            if (containsCodeClient) {
                // Code Client
                String codeClient = "";
                if (tmp[z] != null) {
                    codeClient = tmp[z].toString().trim();
                }
                line.append('\t');
                line.append(codeClient);
                z++;
            }

            // Piece
            line.append('\t');
            line.append(tmp[z].toString().trim());

            line.append('\r');
            line.append('\n');
            bufOut.write(line.toString().getBytes(StringUtils.Cp1252));
        }
    }

}
