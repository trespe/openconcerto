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
 
 package org.openconcerto.erp.core.finance.accounting.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;

public class PdfGenerator_2033E extends PdfGenerator {

    public PdfGenerator_2033E() {
        super("2033E.pdf", "result_2033E.pdf", TemplateNXProps.getInstance().getStringProperty("Location2033E"));
        setTemplateOffset(0, 0);
        setOffset(0, 0);
        setMargin(32, 32);
    }

    public void generate() {
        setFontBold(14);

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        addText("NOM", rowSociete.getString("TYPE") + " " + rowSociete.getString("NOM"), 138, 722);

        setFontRoman(12);
        addText("OUVERT", "1er janvier 2004", 105, 703);
        addText("CLOS", "31 décembre 2004", 274, 703);
        addSplittedText("DUREE", "12", 502, 703, 19);

        // Copyright
        setFontRoman(9);
        String cc = "Document généré par le logiciel Bloc, (c) Front Software 2006";
        addText("", cc, 350, 161, 0);

        setFontRoman(10);
        long t = 785123456L;
        int yy = 0;
        int y = 651;
        for (int i = 0; i < 8; i++) {

            addTextRight("PROD1." + yy, insertCurrencySpaces("" + t), 563, y);

            yy++;
            y -= 18;
        }
        y -= 18;
        for (int i = 0; i < 11; i++) {

            addTextRight("CONSO1." + yy, insertCurrencySpaces("" + t), 563, y);

            yy++;
            y -= 18;
        }
        y -= 16;

        //
        addTextRight("TOT1." + yy, insertCurrencySpaces("" + t), 563, y);

        yy++;
        y -= 18;

    }

}
