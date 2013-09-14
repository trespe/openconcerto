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

public class PdfGenerator_2033A extends PdfGenerator {

    public PdfGenerator_2033A() {

        super("2033A.pdf", "result_2033A.pdf", TemplateNXProps.getInstance().getStringProperty("Location2033APDF"));
        setTemplateOffset(0, 0);
        setOffset(0, 0);
        setMargin(32, 32);

    }

    public void generate() {
        setFontBold(14);

        // TODO recupérer les infos de la societe
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        addText("NOM", rowSociete.getString("TYPE") + " " + rowSociete.getString("NOM"), 120, 794);
        setFontRoman(12);

        SQLRow rowAdresse = Configuration.getInstance().getBase().getTable("ADRESSE_COMMON").getRow(rowSociete.getInt("ID_ADRESSE_COMMON"));
        String ville = rowAdresse.getString("VILLE");
        final Object cedex = rowAdresse.getObject("CEDEX");
        final boolean hasCedex = rowAdresse.getBoolean("HAS_CEDEX");

        if (hasCedex) {
            ville += " CEDEX";
            if (cedex != null && cedex.toString().trim().length() > 0) {
                ville += " " + cedex.toString().trim();
            }
        }

        final String adresse = rowAdresse.getString("RUE") + ", " + rowAdresse.getString("CODE_POSTAL") + " " + ville;
        System.err.println(adresse);
        addText("ADRESSE", adresse, 120, 794 - 18);

        addSplittedText("SIRET", rowSociete.getString("NUM_SIRET"), 111 - 28, 794 - 18 - 18, 17);

        addSplittedText("APE", rowSociete.getString("NUM_APE"), 366, 794 - 18 - 18, 16);

        addSplittedText("CLOS1", "08202006", 416, 707, 9.7);
        addSplittedText("CLOS2", "08202006", 502, 707, 9.7);

        addSplittedText("DUREE1", "12", 211 - 28, 741, 14);
        addSplittedText("DUREE2", "88", 366, 741, 14);

        // Copyright
        setFontRoman(9);
        String cc = "Document généré par le logiciel Bloc, (c) Front Software 2006";
        addText("", cc, getWidth() - 2, 16, 90);

        setFontRoman(10);
        long t = 53L;
        int yy = 0;
        int y = 671;
        for (y = 671; y > 410; y -= 18) {
            t *= y / 100;
            t += y;
            addTextRight("ACTIF1." + yy, insertCurrencySpaces("" + t), 334 - 28, y);
            addTextRight("ACTIF2." + yy, insertCurrencySpaces("" + t), 435 - 28, y);
            addTextRight("ACTIF3." + yy, insertCurrencySpaces("" + t), 521 - 28, y);
            addTextRight("ACTIF4." + yy, insertCurrencySpaces("" + t), 608 - 28, y);
            yy++;
        }
        y -= 18;
        for (; y > 330; y -= 18) {

            t += y;
            addTextRight("PASSIF3." + yy, insertCurrencySpaces("" + t), 521 - 28, y);
            addTextRight("PASSIF4." + yy, insertCurrencySpaces("" + t), 608 - 28, y);
            yy++;
        }
        for (; y > 300; y -= 18) {
            addTextRight("PASSIF2." + yy, insertCurrencySpaces("" + t / 10000), 435 - 28 - 28, y);
            addTextRight("PASSIF3." + yy, insertCurrencySpaces("" + t), 521 - 28, y);
            addTextRight("PASSIF4." + yy, insertCurrencySpaces("" + t), 608 - 28, y);
            yy++;
        }
        for (; y > 160; y -= 18) {
            addTextRight("PASSIF3." + yy, insertCurrencySpaces("" + t), 521 - 28, y);
            addTextRight("PASSIF4." + yy, insertCurrencySpaces("" + t), 608 - 28, y);
            yy++;
        }
        for (; y > 140; y -= 18) {
            addTextRight("PASSIF2." + yy, insertCurrencySpaces("" + t / 10000), 435 - 28 - 28, y);
            addTextRight("PASSIF3." + yy, insertCurrencySpaces("" + t), 521 - 28, y);
            addTextRight("PASSIF4." + yy, insertCurrencySpaces("" + t), 608 - 28, y);
            yy++;
        }
        for (; y > 90; y -= 18) {
            addTextRight("PASSIF3." + yy, insertCurrencySpaces("" + t), 521 - 28, y);
            addTextRight("PASSIF4." + yy, insertCurrencySpaces("" + t), 608 - 28, y);
            yy++;
        }

        for (; y > 30; y -= 18) {
            addTextRight("PASSIF1." + yy, insertCurrencySpaces("" + t), 294, y);
            addTextRight("PASSIF4." + yy, insertCurrencySpaces("" + t), 608 - 28, y);
            yy++;
        }

    }

}
