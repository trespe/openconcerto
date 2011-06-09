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

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SpreadSheetGeneratorGestComm extends SpreadSheetGenerator {

    public SpreadSheetGeneratorGestComm(SheetInterface sheetInterface, String destFileName, boolean impression, boolean visu) {
        super(sheetInterface, destFileName, impression, visu);
        System.err.println("Initialisation Spread Sheet End");
        // SwingUtilities.invokeLater(this);
        new Thread(this).start();
    }

    protected File generateWithStyle() throws IOException {
        final SpreadSheet ssheet = loadTemplate();
        if (ssheet == null) {
            return null;
        }

        final Map<String, Map<Integer, String>> mapStyleDef = StyleSQLElement.getMapAllStyle();

        final Sheet sheet = ssheet.getSheet(0);
        System.err.println("get sheet 0, print ranges --> " + sheet.getPrintRanges());

        String s = (sheet.getPrintRanges() == null) ? "" : sheet.getPrintRanges().toString();
        String[] range = s.split(":");
        // sheet.removePrintRanges();

        for (int i = 0; i < range.length; i++) {
            String string = range[i];
            range[i] = string.subSequence(string.indexOf('.') + 1, string.length()).toString();
        }

        // int colDeb = -1;
        int colEnd = -1;
        int rowEnd = -1;
        if (range.length > 1) {
            // colDeb = sheet.resolveHint(range[0]).x;
            colEnd = sheet.resolveHint(range[1]).x;
            rowEnd = sheet.resolveHint(range[1]).y;
        }

        // on parcourt chaque ligne de la feuille pour recuperer les styles
        searchStyle(sheet, mapStyleDef, colEnd, rowEnd);

        // on place les valeurs
        fill(sheet, mapStyleDef);

        // Replace
        replace(sheet);
        return save(ssheet);
    }
}
