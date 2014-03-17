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

import org.openconcerto.erp.core.common.element.StyleSQLElement;
import org.openconcerto.openoffice.spreadsheet.Sheet;
import org.openconcerto.openoffice.spreadsheet.SpreadSheet;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.Map;

// TODO mettre les style dans des notes --> ex : @style : normal col1, Col2
// TODO popup si cell repeated > 50 ---> java heap space
// TODO popup si fichier introuvable
public class SpreadSheetGeneratorCompta extends SpreadSheetGenerator {

    public SpreadSheetGeneratorCompta(SheetInterface sheet, String destFileName, boolean impr, boolean visu) {
        this(sheet, destFileName, impr, visu, true);
    }

    public SpreadSheetGeneratorCompta(SheetInterface sheet, String destFileName, boolean impr, boolean visu, boolean exportPDF) {
        super(sheet, destFileName, impr, visu, exportPDF);
        new Thread(this).start();
    }

    protected File generateWithStyle() throws IOException {

        final SpreadSheet ssheet = loadTemplate();
        if (ssheet == null) {
            return null;
        }

        final Map<String, Map<Integer, String>> mapStyleDef = StyleSQLElement.getMapAllStyle();

        final Sheet sheet = ssheet.getSheet(0);

        // on parcourt chaque ligne de la feuille pour recuperer les styles
        String s = (sheet.getPrintRanges() == null) ? "" : sheet.getPrintRanges().toString();
        String[] range = s.split(":");

        for (int i = 0; i < range.length; i++) {
            String string = range[i];
            range[i] = string.subSequence(string.indexOf('.') + 1, string.length()).toString();
        }

        // int colDeb = -1;
        int colEnd = -1;
        int rowEnd = -1;
        if (range.length > 1) {
            // colDeb = sheet.resolveHint(range[0]).x;
            final Point resolveHint = sheet.resolveHint(range[1]);
            colEnd = resolveHint.x;
            rowEnd = resolveHint.y;
        }
        searchStyle(sheet, mapStyleDef, colEnd, rowEnd);

        if (colEnd > 0) {
            System.err.println("Set Column Count to :: " + (colEnd + 1));
            sheet.setColumnCount(colEnd + 1);
        }
        sheet.duplicateFirstRows(this.nbRowsPerPage, this.nbPage);

        Object printRangeObj = sheet.getPrintRanges();
        if (printRangeObj != null) {
            String[] range2 = printRangeObj.toString().split(":");

            for (int i = 0; i < range2.length; i++) {
                String string = range2[i];
                range2[i] = string.subSequence(string.indexOf('.') + 1, string.length()).toString();
            }

            int end = -1;
            if (range2.length > 1) {
                end = sheet.resolveHint(range2[1]).y + 1;
                long rowEndNew = end * (this.nbPage + 1);
                String sNew = s.replaceAll(String.valueOf(end), String.valueOf(rowEndNew));
                sheet.setPrintRanges(sNew);
                System.err.println(" ******  Replace print ranges; Old:" + end + "--" + s + " New:" + rowEndNew + "--" + sNew);
            }
        } else {
            sheet.removePrintRanges();
        }

        // on place les valeurs
        fill(sheet, mapStyleDef);

        return save(ssheet);
    }
}
