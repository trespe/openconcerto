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

public class PdfGenerator_2033C extends PdfGenerator {

    public PdfGenerator_2033C() {
        super("2033C.pdf", "result_2033C.pdf", TemplateNXProps.getInstance().getStringProperty("Location2033CPDF"));
        setTemplateOffset(0, 0);
        setOffset(0, 0);
        setMargin(32, 32);

    }

    public void generate() {
        setFontBold(14);

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        addText("NOM", rowSociete.getString("TYPE") + " " + rowSociete.getString("NOM"), 245, 794);
        setFontRoman(12);

        // Copyright
        setFontRoman(9);
        String cc = "Document généré par le logiciel Bloc, (c) Front Software 2006";
        addText("", cc, 350, 31, 0);

        setFontRoman(10);
        long t = 785123456L;
        int yy = 0;
        int y = 724;
        for (int i = 0; i < 10; i++) {

            addTextRight("IMMO1." + yy, insertCurrencySpaces("" + t), 225, y);
            addTextRight("IMMO2." + yy, insertCurrencySpaces("" + t), 316, y);
            addTextRight("IMMO3." + yy, insertCurrencySpaces("" + t), 407, y);
            addTextRight("IMMO4." + yy, insertCurrencySpaces("" + t), 498, y);
            addTextRight("IMMO5." + yy, insertCurrencySpaces("" + t), 587, y);
            yy++;
            y -= 18;
        }
        y -= 18;
        y -= 18;
        for (int i = 0; i < 8; i++) {

            addTextRight("AM1." + yy, insertCurrencySpaces("" + t), 282, y);
            addTextRight("AM2." + yy, insertCurrencySpaces("" + t), 384, y);
            addTextRight("AM3." + yy, insertCurrencySpaces("" + t), 486, y);
            addTextRight("AM4." + yy, insertCurrencySpaces("" + t), 587, y);

            yy++;
            y -= 18;
        }
        y -= 18;
        y -= 18;
        y -= 18;
        y -= 18;
        for (int i = 0; i < 10; i++) {

            addText("VAL1." + yy, "Nature", 40, y);
            addTextRight("VAL2." + yy, insertCurrencySpaces("" + t), 219, y);
            addTextRight("VAL3." + yy, insertCurrencySpaces("" + t), 293, y);
            addTextRight("VAL4." + yy, insertCurrencySpaces("" + t), 367, y);
            addTextRight("VAL5." + yy, insertCurrencySpaces("" + t), 441, y);

            addTextRight("VAL6." + yy, insertCurrencySpaces("" + t), 515, y);
            addTextRight("VAL7." + yy, insertCurrencySpaces("" + t), 587, y);
            yy++;
            y -= 18;
        }

        //
        addTextRight("TOT2." + yy, insertCurrencySpaces("" + t), 219 - 8, y);
        addTextRight("TOT3." + yy, insertCurrencySpaces("" + t), 293 - 8, y);
        addTextRight("TOT4." + yy, insertCurrencySpaces("" + t), 367 - 8, y);
        addTextRight("TOT5." + yy, insertCurrencySpaces("" + t), 441 - 8, y);

        addTextRight("TOT6." + yy, insertCurrencySpaces("" + t), 515 - 8, y);
        addTextRight("TOT7." + yy, insertCurrencySpaces("" + t), 587, y);
        yy++;
        y -= 18;
        addTextRight("TOT6." + yy, insertCurrencySpaces("" + t), 515 - 8, y);
        addTextRight("TOT7." + yy, insertCurrencySpaces("" + t), 587, y);
        yy++;
        y -= 18;
        addTextRight("TOT7." + yy, insertCurrencySpaces("" + t), 587, y);
        yy++;
        y -= 18;

        setFontBold(10);
        y += 2;
        addTextRight("TOT6." + yy, insertCurrencySpaces("" + t), 515 - 8, y);
        addTextRight("TOT7." + yy, insertCurrencySpaces("" + t), 587, y);
        yy++;
        y -= 18;
    }

}
