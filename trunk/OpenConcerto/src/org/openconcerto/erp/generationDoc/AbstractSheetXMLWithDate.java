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
 
 package org.openconcerto.erp.generationDoc;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.StringUtils;

import java.io.File;
import java.util.Calendar;

public abstract class AbstractSheetXMLWithDate extends AbstractSheetXml {

    public AbstractSheetXMLWithDate(SQLRow row) {
        super(row);
    }

    protected final String getYear() {
        final Calendar cal = this.row.getDate("DATE");
        return cal == null ? "Date inconnue" : String.valueOf(cal.get(Calendar.YEAR));
    }

    @Override
    public File getDocumentOutputDirectoryP() {
        return new File(DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory(this.getDefaultTemplateId()), getYear());
    }

    @Override
    public File getPDFOutputDirectoryP() {
        return new File(DocumentLocalStorageManager.getInstance().getPDFOutputDirectory(this.getDefaultTemplateId()), getYear());
    }

    @Override
    public String getStoragePathP() {
        return StringUtils.firstUp(this.elt.getPluralName() + File.separator + getYear());
    }
}
