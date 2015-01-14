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
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ExportFEC extends AbstractExport {
    static private final Charset CHARSET = StringUtils.ISO8859_15;
    static private final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.00", DecimalFormatSymbols.getInstance(Locale.FRANCE));

    static private final List<String> COLS = Arrays.asList("JournalCode", "JournalLib", "EcritureNum", "EcritureDate", "CompteNum", "CompteLib", "CompAuxNum", "CompAuxLib", "PieceRef", "PieceDate",
            "EcritureLib", "Debit", "Credit", "EcritureLet", "DateLet", "ValidDate", "Montantdevise", "Idevise");

    static private String formatCents(final Number n) {
        return DECIMAL_FORMAT.format(BigDecimal.valueOf(n.longValue()).movePointLeft(2));
    }

    private List<Object[]> data;
    private final char zoneSep = '\t';
    private final char recordSep = '\n';
    private final char replacement = ' ';

    public ExportFEC(DBRoot rootSociete) {
        super(rootSociete, "FEC", ".csv");
    }

    @Override
    protected int fetchData(Date from, Date to, SQLRow selectedJournal, boolean onlyNew) {
        final SQLTable tableEcriture = getEcritureT();
        final SQLTable tableMouvement = tableEcriture.getForeignTable("ID_MOUVEMENT");
        final SQLTable tableCompte = tableEcriture.getForeignTable("ID_COMPTE_PCE");
        final SQLTable tableJrnl = tableEcriture.getForeignTable("ID_JOURNAL");
        final SQLTable tablePiece = tableMouvement.getForeignTable("ID_PIECE");

        final SQLSelect sel = createSelect(from, to, selectedJournal, onlyNew);
        sel.addSelect(tableJrnl.getField("CODE"));
        sel.addSelect(tableJrnl.getField("NOM"));
        sel.addSelect(tableMouvement.getField("NUMERO"));
        sel.addSelect(tableEcriture.getField("DATE"));
        sel.addSelect(tableCompte.getField("NUMERO"));
        sel.addSelect(tableCompte.getField("NOM"));
        sel.addSelect(tablePiece.getField("NOM"));
        // TODO ID_MOUVEMENT_PERE* ; SOURCE.DATE
        sel.addSelect(tableEcriture.getField("NOM"));
        sel.addSelect(tableEcriture.getField("DEBIT"));
        sel.addSelect(tableEcriture.getField("CREDIT"));
        sel.addSelect(tableEcriture.getField("DATE_LETTRAGE"));
        sel.addSelect(tableEcriture.getField("LETTRAGE"));
        sel.addSelect(tableEcriture.getField("DATE_VALIDE"));

        sel.addFieldOrder(tableEcriture.getField("DATE"));
        sel.addFieldOrder(tableMouvement.getField("NUMERO"));

        @SuppressWarnings("unchecked")
        final List<Object[]> l = (List<Object[]>) this.getRootSociete().getDBSystemRoot().getDataSource().execute(sel.asString(), new ArrayListHandler());
        this.data = l;
        return l == null ? 0 : l.size();
    }

    private final void addEmptyField(final List<String> line) {
        line.add(null);
    }

    private final void addAmountField(final List<String> line, final Number cents) {
        line.add(formatCents(cents));
    }

    private final void addField(final List<String> line, final String s) {
        if (this.zoneSep == this.replacement || this.recordSep == this.replacement)
            throw new IllegalStateException("Wrong separators");
        // TODO remove \r
        line.add(s.trim().replace(this.zoneSep, this.replacement).replace(this.recordSep, this.replacement));
    }

    @Override
    protected void export(OutputStream out) throws IOException {
        final Writer bufOut = new OutputStreamWriter(out, CHARSET);
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final int fieldsCount = COLS.size();

        for (final String colName : COLS) {
            bufOut.write(colName);
            bufOut.write(this.zoneSep);
        }
        bufOut.write(this.recordSep);

        final List<String> line = new ArrayList<String>(fieldsCount);
        for (final Object[] array : this.data) {
            line.clear();

            // JournalCode
            addField(line, (String) array[0]);
            // JournalLib
            addField(line, (String) array[1]);
            // EcritureNum
            addField(line, String.valueOf(array[2]));
            // EcritureDate
            final String ecritureDate = dateFormat.format(array[3]);
            line.add(ecritureDate);
            // CompteNum
            addField(line, (String) array[4]);
            // CompteLib
            addField(line, (String) array[5]);
            // CompAuxNum
            addEmptyField(line);
            // CompAuxLib
            addEmptyField(line);
            // PieceRef
            addField(line, (String) array[6]);
            // PieceDate TODO ID_MOUVEMENT_PERE* ; SOURCE.DATE
            line.add(ecritureDate);
            // EcritureLib
            addField(line, (String) array[7]);
            // Debit
            addAmountField(line, (Number) array[8]);
            // Credit
            addAmountField(line, (Number) array[9]);
            // EcritureLet
            addField(line, (String) array[11]);

            // DateLet
            if (array[10] != null) {
                final String ecritureDateLettrage = dateFormat.format(array[10]);
                line.add(ecritureDateLettrage);
            } else {
                line.add("");
            }
            // ValidDate
            if (array[12] != null) {
                final String ecritureDateValid = dateFormat.format(array[12]);
                line.add(ecritureDateValid);
            } else {
                line.add("");
            }
            // Montantdevise
            addEmptyField(line);
            // Idevise
            addEmptyField(line);

            assert line.size() == fieldsCount;
            for (int i = 0; i < fieldsCount; i++) {
                final String zone = line.get(i);
                // blank field
                if (zone != null)
                    bufOut.write(zone);
                if (i < fieldsCount - 1)
                    bufOut.write(this.zoneSep);
            }
            bufOut.write(this.recordSep);
        }
        bufOut.close();
    }
}
