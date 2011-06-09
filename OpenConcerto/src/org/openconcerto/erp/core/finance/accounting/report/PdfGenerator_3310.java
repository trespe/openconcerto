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

import org.openconcerto.erp.preferences.TemplateNXProps;

public class PdfGenerator_3310 extends PdfGenerator {

    public PdfGenerator_3310() {

        super("3310_2.pdf", "result_3310_2.pdf", TemplateNXProps.getInstance().getStringProperty("Location3310PDF"));
        setTemplateOffset(0, 0);
        setOffset(0, 0);
        setMargin(32, 32);
    }

    public void generate() {
        setFontBold(14);

        // Copyright
        setFontRoman(9);
        String cc = "Document généré par le logiciel Bloc, (c) Front Software 2006";

        setFontRoman(10);
        long t = 53L;

        int y = 762;
        addTextRight("A01", insertCurrencySpaces("" + t), 300, y);
        y -= 23;
        addTextRight("A02", insertCurrencySpaces("" + t), 300, y);
        y -= 25;
        addTextRight("A03", insertCurrencySpaces("" + t), 300, y);

        y = 762;
        addTextRight("A04", insertCurrencySpaces("" + t), 575, y);
        y -= 23;
        addTextRight("A05", insertCurrencySpaces("" + t), 575, y);
        y -= 25;
        addTextRight("A06", insertCurrencySpaces("" + t), 575, y);
        y -= 23;
        addTextRight("A07", insertCurrencySpaces("" + t), 575, y);

        y = 623;
        addTextRight("B08HT", insertCurrencySpaces("" + t), 490, y);
        addTextRight("B08", insertCurrencySpaces("" + t), 575, y);

        y -= 18;
        addTextRight("B09", insertCurrencySpaces("" + t), 490, y);
        addTextRight("B09HT", insertCurrencySpaces("" + t), 575, y);

        y -= 18;
        addTextRight("B09B", insertCurrencySpaces("" + t), 490, y);
        addTextRight("B09BHT", insertCurrencySpaces("" + t), 575, y);

        y -= 28;
        addTextRight("B10", insertCurrencySpaces("" + t), 490, y);
        addTextRight("B10HT", insertCurrencySpaces("" + t), 575, y);

        y -= 18;
        addTextRight("B11", insertCurrencySpaces("" + t), 490, y);
        addTextRight("B11HT", insertCurrencySpaces("" + t), 575, y);

        y -= 18;
        addTextRight("B12", insertCurrencySpaces("" + t), 490, y);
        addTextRight("B12HT", insertCurrencySpaces("" + t), 575, y);

        y -= 41;
        addTextRight("B13", insertCurrencySpaces("" + t), 490, y);
        addTextRight("B13HT", insertCurrencySpaces("" + t), 575, y);

        y -= 18;
        addTextRight("B14", insertCurrencySpaces("" + t), 490, y);
        addTextRight("B14HT", insertCurrencySpaces("" + t), 575, y);

        y -= 18;
        addTextRight("B15", insertCurrencySpaces("" + t), 575, y);

        y -= 23;
        addTextRight("B16", insertCurrencySpaces("" + t), 575, y);
        y -= 22;
        addTextRight("B17", insertCurrencySpaces("" + t), 575, y);
        y -= 22;
        addTextRight("B18", insertCurrencySpaces("" + t), 575, y);
        y -= 40;
        addTextRight("B19", insertCurrencySpaces("" + t), 575, y);
        y -= 23;
        addTextRight("B20", insertCurrencySpaces("" + t), 575, y);
        y -= 23;
        addTextRight("B21", insertCurrencySpaces("" + t), 575, y);
        y -= 23;
        addTextRight("B22", insertCurrencySpaces("" + t), 575, y);

        y -= 46;
        addTextRight("B24", insertCurrencySpaces("" + t), 565, y);

        y -= 61;
        addTextRight("C25", insertCurrencySpaces("" + t), 300, y);
        addTextRight("C28", insertCurrencySpaces("" + t), 575, y);
        y -= 31;
        addTextRight("C26", insertCurrencySpaces("" + t), 300, y);
        addTextRight("C29", insertCurrencySpaces("" + t), 575, y);

        y -= 23;
        addTextRight("C27", insertCurrencySpaces("" + t), 300, y);
        addTextRight("C30", insertCurrencySpaces("" + t), 575, y);

        y -= 23;
        addTextRight("C31", insertCurrencySpaces("" + t), 575, y);

        y -= 40;
        addTextRight("C32", insertCurrencySpaces("" + t), 565, y);
    }
}
