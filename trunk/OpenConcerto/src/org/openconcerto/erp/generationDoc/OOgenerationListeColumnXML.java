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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.StyleSQLElement;
import org.openconcerto.openoffice.ODPackage;
import org.openconcerto.openoffice.spreadsheet.MutableCell;
import org.openconcerto.openoffice.spreadsheet.Sheet;
import org.openconcerto.openoffice.spreadsheet.SpreadSheet;
import org.openconcerto.openoffice.spreadsheet.Table;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.StreamUtils;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class OOgenerationListeColumnXML {

    // Cache pour la recherche des styles
    private static Map<Sheet, Map<String, Map<Integer, String>>> cacheStyle = new HashMap<Sheet, Map<String, Map<Integer, String>>>();

    public static File genere(String modele, File pathDest, String fileDest, Map<Integer, List<Map<String, Object>>> liste, Map<Integer, Map<String, Object>> values) {
        return genere(modele, pathDest, fileDest, liste, values, new HashMap<Integer, Map<Integer, String>>(), null, null);
    }

    public static File genere(String templateId, File pathDest, String fileDest, Map<Integer, List<Map<String, Object>>> liste, Map<Integer, Map<String, Object>> values,
            Map<Integer, Map<Integer, String>> mapStyle, List<String> sheetName, SQLRow rowLanguage) {
        cacheStyle.clear();
        final SAXBuilder builder = new SAXBuilder();
        try {
            InputStream xmlConfiguration = TemplateManager.getInstance().getTemplateConfiguration(templateId, rowLanguage != null ? rowLanguage.getString("CHEMIN") : null, null);
            Document doc = builder.build(xmlConfiguration);

            // On initialise un nouvel élément racine avec l'élément racine du
            // document.
            final Element racine = doc.getRootElement();

            // Création et génération du fichier OO
            final InputStream template = TemplateManager.getInstance().getTemplate(templateId, rowLanguage != null ? rowLanguage.getString("CHEMIN") : null, null);

            final SpreadSheet spreadSheet = new ODPackage(template).getSpreadSheet();
            Sheet sheet0 = spreadSheet.getSheet(0);
            if (sheetName != null && sheetName.size() > 0) {
                for (int i = 1; i < sheetName.size(); i++) {
                    sheet0.copy(i, (sheetName != null) ? sheetName.get(i) : "Feuille " + i);
                }
                spreadSheet.getSheet(0).setName(sheetName.get(0));
            }

            for (Integer i : liste.keySet()) {
                final Sheet sheet = spreadSheet.getSheet(i);
                List children = racine.getChildren("element" + i);
                if (children.size() == 0) {
                    children = racine.getChildren("element");
                }
                parseElementsXML(children, sheet, values.get(i));
                Element child = racine.getChild("table" + i);
                if (child == null) {
                    child = racine.getChild("table");
                }
                parseListeXML(child, liste.get(i), sheet, mapStyle.get(i));
            }
            // Sauvegarde du fichier
            return saveSpreadSheet(spreadSheet, pathDest, fileDest, templateId, rowLanguage);

        } catch (JDOMException e) {
            ExceptionHandler.handle("Erreur lors de la génération du fichier " + fileDest, e);
        } catch (IOException e) {
            ExceptionHandler.handle("Erreur lors de la création du fichier " + fileDest, e);
        }
        return null;
    }

    private static void parseElementsXML(List<Element> elts, Sheet sheet, Map<String, Object> values) {
        if (values == null) {
            return;
        }
        for (Element elt : elts) {

            String name = elt.getAttributeValue("ValueName");
            Object result = values.get(name);

            if (result != null) {
                boolean controlLine = elt.getAttributeValue("controleMultiline") == null ? true : !elt.getAttributeValue("controleMultiline").equalsIgnoreCase("false");
                boolean replace = elt.getAttributeValue("type").equalsIgnoreCase("Replace");
                String replacePattern = elt.getAttributeValue("replacePattern");
                fill(sheet.getCellAt(elt.getAttributeValue("location")), result, sheet, replace, replacePattern, null, false, controlLine);
            }
        }
    }

    /**
     * Remplit le tableau
     * 
     * @param tableau
     * @param elt
     * @param id
     * @param sheet
     */
    private static void parseListeXML(Element tableau, List<Map<String, Object>> liste, Sheet sheet, Map<Integer, String> style) {

        if (liste == null || tableau == null) {
            return;
        }
        Object oLastColTmp = tableau.getAttributeValue("lastColumn");
        int lastColumn = -1;
        int endPageLine = Integer.valueOf(tableau.getAttributeValue("endPageLine"));
        if (oLastColTmp != null) {
            lastColumn = sheet.resolveHint(oLastColTmp.toString() + 1).x + 1;
        }
        Map<String, Map<Integer, String>> mapStyle = searchStyle(sheet, lastColumn, endPageLine);

        // int nbPage = fillTable(tableau, liste, sheet, mapStyle, true, style);
        // int firstLine = Integer.valueOf(tableau.getAttributeValue("firstLine"));
        // int endLine = Integer.valueOf(tableau.getAttributeValue("endLine"));
        // Object printRangeObj = sheet.getPrintRanges();
        //
        // System.err.println("Nombre de page == " + nbPage);
        // if (nbPage == 1) {
        // fillTable(tableau, liste, sheet, mapStyle, false, style);
        // } else {
        //
        // if (printRangeObj != null) {
        // String s = printRangeObj.toString();
        // String[] range = s.split(":");
        //
        // for (int i = 0; i < range.length; i++) {
        // String string = range[i];
        // range[i] = string.subSequence(string.indexOf('.') + 1, string.length()).toString();
        // }
        //
        // int rowEnd = -1;
        // if (range.length > 1) {
        // rowEnd = sheet.resolveHint(range[1]).y + 1;
        // int rowEndNew = rowEnd * (nbPage + 1);
        // String sNew = s.replaceAll(String.valueOf(rowEnd), String.valueOf(rowEndNew));
        // sheet.setPrintRanges(sNew);
        // System.err.println(" ******  Replace print ranges; Old:" + rowEnd + "--" + s + " New:" +
        // rowEndNew + "--" + sNew);
        // }
        // }
        //
        // // le nombre d'éléments ne tient pas dans le tableau du modéle
        // sheet.duplicateFirstRows(endLine, 1);
        //
        // int lineToAdd = endPageLine - endLine;
        // sheet.insertDuplicatedRows(firstLine, lineToAdd);
        //
        // // On duplique la premiere page si on a besoin de plus de deux pages
        // System.err.println("nbPage == " + nbPage);
        // if (nbPage > 2) {
        // sheet.duplicateFirstRows(endPageLine, nbPage - 2);
        // }
        fillTable(tableau, liste, sheet, mapStyle, false, style);
        // }
    }

    /**
     * Remplit le tableau d'éléments avec les données
     * 
     * @param tableau Element Xml contenant les informations sur le tableau
     * @param elt SQLElement (ex : Bon de livraison)
     * @param id id de l'élément de la table
     * @param sheet feuille calc à remplir
     * @param mapStyle styles trouvés dans la page
     * @param test remplir ou non avec les valeurs
     * @return le nombre de page
     */
    private static int fillTable(Element tableau, List<Map<String, Object>> liste, Sheet sheet, Map<String, Map<Integer, String>> mapStyle, boolean test, Map<Integer, String> style) {

        // int nbPage = 1;
        // int currentLineTmp = Integer.valueOf(tableau.getAttributeValue("firstLine"));
        // int currentLine = Integer.valueOf(tableau.getAttributeValue("firstLine"));
        int endPageLine = Integer.valueOf(tableau.getAttributeValue("endPageLine"));
        int endLine = Integer.valueOf(tableau.getAttributeValue("endLine"));

        List listElts = tableau.getChildren("element");

        Object o = null;
        String columnSousTotal = tableau.getAttributeValue("groupSousTotalColumn");

        Map<String, Double> mapSousTotal = new HashMap<String, Double>();
        Map<String, Double> mapTotal = new HashMap<String, Double>();

        // on remplit chaque colonnes à partir des rows recuperées
        sheet.setColumnCount(liste.size() + 5);
        Integer firstCol = Integer.valueOf(tableau.getAttributeValue("firstLine"));
        int currentCol = firstCol;
        for (int i = 0; i < liste.size(); i++) {
            Map<String, Object> mValues = liste.get(i);
            // System.err.println(mValues);
            if (currentCol != firstCol) {
                for (int k = 0; k < endLine; k++) {
                    MutableCell<SpreadSheet> c1 = sheet.getCellAt(firstCol, k);
                    if (c1.getFormula() != null && c1.getFormula().trim().length() > 0) {
                        String formula = c1.getFormula();
                        MutableCell<SpreadSheet> c2 = sheet.getCellAt(currentCol, k);
                        System.err.println(formula);
                        formula = formula.replaceAll("(" + Table.toStr(c1.getX()) + ")" + "(\\d+)", Table.toStr(c2.getX()) + "$2");
                        formula = formula.replaceAll(Table.toStr(c1.getX()) + "\\$", Table.toStr(c2.getX()) + "\\$");
                        System.err.println(" replace by " + formula);
                        c2.getElement().setAttribute("formula", formula, c2.getElement().getNamespace());
                        c2.setStyleName(c1.getStyleName());
                    }
                }
            }

            String styleName = null;
            // int nbCellule = 1;
            // on remplit chaque cellule de la colonne
            for (Iterator j = listElts.iterator(); j.hasNext();) {

                // if ((currentLine - 1 + fill("A1", "test", sheet, false, null, null, true)) >
                // (endPageLine * nbPage)) {
                // currentLine = currentLineTmp + endPageLine;
                // currentLineTmp = currentLine;
                // nbPage++;
                // }

                Element e = (Element) j.next();
                MutableCell<SpreadSheet> cell = sheet.getCellAt(currentCol, Integer.valueOf(e.getAttributeValue("location").trim()));
                // String loc = e.getAttributeValue("location").trim() + currentLine;
                boolean controlLine = e.getAttributeValue("controleMultiline") == null ? true : !e.getAttributeValue("controleMultiline").equalsIgnoreCase("false");
                // Type normaux fill ou replace
                if (e.getAttributeValue("type").equalsIgnoreCase("fill") || e.getAttributeValue("type").equalsIgnoreCase("replace")) {

                    Object value = getElementValue(e, mValues);

                    boolean replace = e.getAttributeValue("type").equalsIgnoreCase("replace");

                    if (test || cell.isValid()) {
                        // if (style != null) {
                        // styleName = style.get(i);
                        // }
                        // Map mTmp = styleName == null ? null : (Map) mapStyle.get(styleName);
                        // String styleOO = null;
                        // if (mTmp != null) {
                        //
                        // Object oTmp = mTmp.get(new Integer(sheet.resolveHint(loc).x));
                        // styleOO = oTmp == null ? null : oTmp.toString();
                        // System.err.println("Set style " + styleOO);
                        // }
                        if (currentCol != firstCol) {
                            MutableCell<SpreadSheet> c2 = sheet.getCellAt(firstCol, cell.getPoint().y);
                            styleName = c2.getStyleName();
                        }
                        int tmpCelluleAffect = fill(cell, value, sheet, replace, null, styleName, test, controlLine);
                        // nbCellule = Math.max(nbCellule, tmpCelluleAffect);
                    } else {
                        System.err.println("Cell not valid at " + cell);
                    }
                }
            }
            // currentLine += nbCellule;
            currentCol++;

        }

        return 1;
    }

    private static void incrementTotal(String field, Double value, Map<String, Double> map) {
        Double d = map.get(field);
        if (d == null) {
            map.put(field, value);
        } else {
            map.put(field, d + value);
        }
    }

    private static Object getElementValue(Element elt, Map<String, Object> mValues) {
        Object res = "";

        final List eltFields = elt.getChildren("field");

        if (eltFields != null) {
            if (eltFields.size() > 1) {
                String result = "";
                for (Iterator j = eltFields.iterator(); j.hasNext();) {
                    Object o = getValueOfComposant((Element) j.next(), mValues);
                    if (o != null) {
                        result += o.toString() + " ";
                    }
                }
                res = result;
            } else {
                res = getValueOfComposant((Element) eltFields.get(0), mValues);
            }
        }
        return res;
    }

    /**
     * permet d'obtenir la valeur d'un élément field
     * 
     * @param eltField
     * @param row
     * @param elt
     * @param id
     * @return value of composant
     */
    private static Object getValueOfComposant(Element eltField, Map<String, Object> mValues) {

        String field = eltField.getAttributeValue("name");

        return mValues.get(field);
    }

    private static int fill(String location, Object value, Sheet sheet, boolean replace, String replacePattern, String styleOO, boolean test) {
        return fill(sheet.getCellAt(location), value, sheet, replace, replacePattern, styleOO, test, true);
    }

    /**
     * Permet de remplir une cellule
     * 
     * @param location position de la cellule exemple : A3
     * @param value valeur à insérer dans la cellule
     * @param sheet feuille sur laquelle on travaille
     * @param replace efface ou non le contenu original de la cellule
     * @param styleOO style à appliquer
     */
    private static int fill(MutableCell<SpreadSheet> cell, Object value, Sheet sheet, boolean replace, String replacePattern, String styleOO, boolean test, boolean controlLine) {

        int nbCellule = 1;
        // est ce que la cellule est valide
        if (test || cell.isValid()) {

            // on divise en 2 cellules si il y a des retours à la ligne
            if (controlLine && (value != null && value.toString().indexOf('\n') >= 0)) {

                if (!test) {
                    String firstPart = value.toString().substring(0, value.toString().indexOf('\n'));
                    String secondPart = value.toString().substring(value.toString().indexOf('\n') + 1, value.toString().length());
                    secondPart = secondPart.replace('\n', ',');
                    setCellValue(cell, firstPart, replace, replacePattern);
                    if (styleOO != null) {
                        cell.setStyleName(styleOO);
                    }

                    // Point p = sheet.resolveHint(location);
                    Point p = cell.getPoint();
                    try {
                        MutableCell cellSec = sheet.getCellAt(p.x, p.y + 1);
                        setCellValue(cellSec, secondPart, replace, replacePattern);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                nbCellule = 2;
            } else {
                if (!test) {
                    // application de la valeur
                    setCellValue(cell, value, replace, replacePattern);

                    // Application du style
                    if (styleOO != null) {
                        cell.setStyleName(styleOO);
                    }
                }
            }
        }
        return nbCellule;
    }

    /**
     * remplit une cellule
     * 
     * @param cell
     * @param value
     * @param replace
     */
    private static void setCellValue(MutableCell cell, Object value, boolean replace, String replacePattern) {
        if (value == null) {
            value = "";
        }

        if (replace) {
            if (replacePattern != null) {
                cell.replaceBy(replacePattern, value.toString());
            } else {
                cell.replaceBy("_", value.toString());
            }
        } else {
            cell.setValue(value);
        }
    }

    /**
     * Sauver le document au format OpenOffice. Si le fichier existe déjà, le fichier existant sera
     * renommé sous la forme nomFic_1.sxc.
     * 
     * @param ssheet SpreadSheet à sauvegarder
     * @param pathDest répertoire de destination du fichier
     * @param fileName nom du fichier à créer
     * @return un File pointant sur le fichier créé
     * @throws IOException
     */
    private static File saveSpreadSheet(SpreadSheet ssheet, File pathDest, String fileName, String templateId, SQLRow rowLanguage) throws IOException {

        // Test des arguments
        if (ssheet == null || pathDest == null || fileName.trim().length() == 0) {
            throw new IllegalArgumentException();
        }

        // Renommage du fichier si il existe déja
        File fDest = new File(pathDest, fileName + ".ods");

        if (!pathDest.exists()) {
            pathDest.mkdirs();
        }

        SheetUtils.convertToOldFile(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete(), fileName, pathDest, fDest);

        // Sauvegarde
        try {
            ssheet.saveAs(fDest);
        } catch (FileNotFoundException e) {
            final File F = fDest;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        JOptionPane.showMessageDialog(null, "Le fichier " + F.getCanonicalPath() + " n'a pu être créé. \n Vérifiez qu'il n'est pas déjà ouvert.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            e.printStackTrace();
        }

        // Copie de l'odsp
        try {
            File odspOut = new File(pathDest, fileName + ".odsp");
            InputStream odspIn = TemplateManager.getInstance().getTemplatePrintConfiguration(templateId, rowLanguage != null ? rowLanguage.getString("CHEMIN") : null, null);
            if (odspIn != null) {
                StreamUtils.copy(odspIn, odspOut);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Le fichier odsp n'existe pas.");
        }

        return fDest;
    }

    /**
     * parcourt l'ensemble de la feuille pour trouver les style définit
     */
    private static Map<String, Map<Integer, String>> searchStyle(Sheet sheet, int colEnd, int rowEnd) {

        if (cacheStyle.get(sheet) != null) {
            return cacheStyle.get(sheet);
        }

        Map<String, Map<Integer, String>> mapStyleDef = StyleSQLElement.getMapAllStyle();

        // on parcourt chaque ligne de la feuille pour recuperer les styles
        int columnCount = (colEnd == -1) ? sheet.getColumnCount() : (colEnd + 1);
        System.err.println("End column search : " + columnCount);

        int rowCount = (rowEnd > 0) ? rowEnd : sheet.getRowCount();
        System.err.println("End row search : " + rowCount);
        for (int i = 0; i < rowCount; i++) {
            int x = 0;
            Map<Integer, String> mapCellStyle = new HashMap<Integer, String>();
            String style = "";

            for (int j = 0; j < columnCount; j++) {

                try {
                    if (sheet.isCellValid(j, i)) {

                        MutableCell c = sheet.getCellAt(j, i);
                        String cellStyle = c.getStyleName();

                        try {
                            if (mapStyleDef.containsKey(c.getValue().toString())) {
                                style = c.getValue().toString();
                                // System.err.println("FIND STYLE " +
                                // c.getValue().toString() +
                                // " SET VALUE " + cellStyle);
                            }
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        mapCellStyle.put(Integer.valueOf(x), cellStyle);
                        if (style.trim().length() != 0) {
                            c.clearValue();
                            // c.setStyle("Default");
                            if (!style.trim().equalsIgnoreCase("Normal") && mapStyleDef.get("Normal") != null) {
                                String styleCell = mapStyleDef.get("Normal").get(Integer.valueOf(x));
                                if (styleCell != null && styleCell.length() != 0) {
                                    c.setStyleName(styleCell);
                                }
                            }
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("Index out of bounds Exception");
                }
                x++;
            }

            if (style.length() > 0) {
                mapStyleDef.put(style, mapCellStyle);
                // System.err.println("style " + mapCellStyle);
            }
        }
        cacheStyle.put(sheet, mapStyleDef);
        return mapStyleDef;
    }

    public static void main(String[] args) {
        ComptaPropsConfiguration conf = ComptaPropsConfiguration.create();
        System.err.println("Conf created");
        Configuration.setInstance(conf);
        conf.setUpSocieteDataBaseConnexion(36);
        System.err.println("Connection Set up");

        System.err.println("Start Genere");
        // genere("Devis", "C:\\", "Test", elt, 19);
        System.err.println("Stop genere");
    }
}
