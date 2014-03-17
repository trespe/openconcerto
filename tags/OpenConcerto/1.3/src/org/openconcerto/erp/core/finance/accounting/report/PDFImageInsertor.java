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
 
 /*
 * Créé le 2 août 2012
 */
package org.openconcerto.erp.core.finance.accounting.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

public class PDFImageInsertor {

    public PDFImageInsertor() {

    }

    public void insert(File pdf, Image img, int page, boolean under) throws Exception {

        PdfReader reader;

        reader = new PdfReader(new FileInputStream(pdf));

        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(pdf));

        // img.setAbsolutePosition(20, 40);

        // int total = reader.getNumberOfPages() + 1;
        PdfContentByte content;
        if (under) {
            content = stamper.getUnderContent(page);
        } else {
            content = stamper.getOverContent(page);
        }
        content.addImage(img);
        stamper.close();
    }

}
