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
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelectJoin;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.utils.Tuple2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

abstract class AbstractExport {

    static private final DateFormat FILE_DF = new SimpleDateFormat("yyyyMMdd");
    static private final DateFormat UNIQUE_DF = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private final DBRoot rootSociete;
    private final String type;
    private final String extension;
    private boolean used;

    protected AbstractExport(final DBRoot rootSociete, final String type, final String extension) {
        this.rootSociete = rootSociete;
        this.type = type;
        this.extension = extension;
        this.used = false;
    }

    protected final DBRoot getRootSociete() {
        return this.rootSociete;
    }

    protected final SQLDataSource getDS() {
        return this.getRootSociete().getDBSystemRoot().getDataSource();
    }

    protected final SQLTable getEcritureT() {
        return this.getRootSociete().getTable("ECRITURE");
    }

    protected final Where getWhere(Date from, Date to, SQLRow selectedJournal, boolean onlyNew) {
        final SQLTable tableEcriture = getEcritureT();
        Where w = new Where(tableEcriture.getField("DATE"), from, to);

        if (selectedJournal != null && !selectedJournal.isUndefined()) {
            w = w.and(new Where(tableEcriture.getField("ID_JOURNAL"), "=", selectedJournal.getID()));
        }

        if (onlyNew) {
            w = w.and(Where.isNull(tableEcriture.getField("DATE_EXPORT")));
        }
        return w;
    }

    protected SQLSelect createSelect(Date from, Date to, SQLRow selectedJournal, boolean onlyNew) {
        final SQLSelect sel = new SQLSelect();

        final SQLTable tableEcriture = this.getRootSociete().getTable("ECRITURE");
        sel.addFrom(tableEcriture);
        sel.addJoin("LEFT", tableEcriture.getField("ID_JOURNAL"));
        sel.addJoin("LEFT", tableEcriture.getField("ID_COMPTE_PCE"));
        final SQLSelectJoin mvtJoin = sel.addJoin("LEFT", tableEcriture.getField("ID_MOUVEMENT"));
        sel.addJoin("LEFT", mvtJoin.getJoinedTable().getField("ID_PIECE"));
        sel.setWhere(getWhere(from, to, selectedJournal, onlyNew));

        return sel;
    }

    public final Tuple2<File, Number> export(final File selectedFile, Date from, Date to, SQLRow selectedJournal, boolean onlyNew) throws Exception {
        synchronized (this) {
            if (this.used)
                throw new IllegalStateException("Already used");
            this.used = true;
        }
        if (selectedFile == null)
            throw new IllegalArgumentException("Dossier sélectionné incorrect");

        final int count = this.fetchData(from, to, selectedJournal, onlyNew);
        if (count == 0)
            return Tuple2.<File, Number> create(null, count);

        if (!selectedFile.isDirectory()) {
            throw new IllegalArgumentException("Vous n'avez pas sélectionné un dossier");
        }
        if (!selectedFile.canWrite()) {
            throw new IllegalArgumentException("Vous n'avez pas les droits pour écrire dans le dossier " + selectedFile.getAbsolutePath());
        }
        final Date now = new Date();
        // ExportOC_20131101-20131127_koala.144356.txt
        // MAYBE only include generation date if necessary
        final File fOut = new File(selectedFile, "ExportOC_" + FILE_DF.format(from) + "-" + FILE_DF.format(to) + "_" + this.type + "." + UNIQUE_DF.format(now) + this.extension);
        final BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(fOut.getAbsolutePath()));
        try {
            this.export(bufOut);
        } finally {
            bufOut.close();
        }
        // Store export date
        final UpdateBuilder update = new UpdateBuilder(getEcritureT());
        update.set("DATE_EXPORT", getEcritureT().getField("DATE_EXPORT").getType().toString(now));
        // onlyNew=true to not overwrite already exported
        update.setWhere(getWhere(from, to, selectedJournal, true));
        getDS().execute(update.asString());

        return Tuple2.<File, Number> create(fOut, count);
    }

    protected abstract int fetchData(Date from, Date to, SQLRow selectedJournal, boolean onlyNew);

    protected abstract void export(final OutputStream out) throws IOException;
}
