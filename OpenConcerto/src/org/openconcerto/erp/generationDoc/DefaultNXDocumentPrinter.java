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

import org.jopendocument.model.OpenDocument;
import org.jopendocument.print.DocumentPrinter;

public class DefaultNXDocumentPrinter implements DocumentPrinter {

    String printerName;
    int copies;

    public DefaultNXDocumentPrinter() {
        this(null, 1);
    }

    public DefaultNXDocumentPrinter(String printerName) {
        this(printerName, 1);
    }

    public DefaultNXDocumentPrinter(String printerName, int copies) {
        this.printerName = printerName;
        this.copies = copies;
    }

    @Override
    public void print(OpenDocument doc) {
        ODTPrinterNX p = new ODTPrinterNX(doc);
        try {
            p.print(this.printerName, this.copies);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }

}
