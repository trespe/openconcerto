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
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.StreamUtils;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Génération d'un document sxc à partir d'un modéle sxc et d'un fichier xml du meme nom (doc.sxc et
 * doc.xml) <element location="D4" (type="fill" || type="replace" replacePattern="_" ||
 * type="codesMissions" ||type="DescriptifArticle" || type="DateEcheance"> <field base="Societe"
 * table="AFFAIRE" name="NUMERO"/> </element>
 * 
 * 
 * @author Administrateur
 * 
 */
public class OOgenerationXML {

    private static DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // Cache pour la recherche des styles
    private static Map<Sheet, Map<String, Map<Integer, String>>> cacheStyle = new HashMap<Sheet, Map<String, Map<Integer, String>>>();

    // Cache pour les SQLRow du tableau
    private static Map<String, List<? extends SQLRowAccessor>> rowsEltCache = new HashMap<String, List<? extends SQLRowAccessor>>();

    private static int answer = JOptionPane.NO_OPTION;

    public static synchronized File createDocument(String templateId, File outputDirectory, final String expectedFileName, SQLRow row, SQLRow rowLanguage) {
        final String langage = rowLanguage != null ? rowLanguage.getString("CHEMIN") : null;
        cacheStyle.clear();
        OOXMLCache.clearCache();
        rowsEltCache.clear();
        taxe.clear();
        cacheForeign.clear();

        File fDest = new File(outputDirectory, expectedFileName);

        if (fDest.exists()) {

            if (SwingUtilities.isEventDispatchThread()) {
                answer = JOptionPane.showConfirmDialog(null, "Voulez vous regénérer et écraser l'ancien document?", "Génération du document", JOptionPane.YES_NO_OPTION);
                Thread.dumpStack();
            } else {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {

                            answer = JOptionPane.showConfirmDialog(null, "Voulez vous regénérer et écraser l'ancien document?", "Génération du document", JOptionPane.YES_NO_OPTION);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            if (answer != JOptionPane.YES_OPTION) {
                return fDest;
            }
        }

        Date d = new Date();
        SAXBuilder builder = new SAXBuilder();
        try {

            if (needAnnexe(templateId, row, rowLanguage)) {
                // check if it exists
                final String annexeTemplateId = templateId + "_annexe";
                InputStream annexeStream = TemplateManager.getInstance().getTemplate(annexeTemplateId, langage, null);
                if (annexeStream != null) {
                    templateId = annexeTemplateId;
                    System.err.println("modele With annexe " + templateId);
                }
            }

            System.err.println("Using template id: " + templateId);
            final InputStream xmlConfiguration = TemplateManager.getInstance().getTemplateConfiguration(templateId, langage, null);

            Document doc = builder.build(xmlConfiguration);

            // On initialise un nouvel élément racine avec l'élément racine du document.
            Element racine = doc.getRootElement();

            // Liste des <element>
            List<Element> listElts = racine.getChildren("element");

            // Création et génération du fichier OO
            final InputStream template = TemplateManager.getInstance().getTemplate(templateId, langage, null);

            final SpreadSheet spreadSheet = new ODPackage(template).getSpreadSheet();
            try {
                // On remplit les cellules de la feuille
                parseElementsXML(listElts, row, spreadSheet);

                // Liste des <element>
                List<Element> listTable = racine.getChildren("table");

                for (Element tableChild : listTable) {
                    // On remplit les cellules du tableau
                    parseTableauXML(tableChild, row, spreadSheet, rowLanguage);
                }
            } catch (Exception e) {
                ExceptionHandler.handle("Impossible de remplir le document " + templateId + " " + ((rowLanguage == null) ? "" : rowLanguage.getString("CHEMIN")), e);
            }
            // Sauvegarde du fichier
            return saveSpreadSheet(spreadSheet, outputDirectory, expectedFileName, templateId, rowLanguage);

        } catch (final JDOMException e) {

            e.printStackTrace();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExceptionHandler.handle("Erreur lors de la génération du fichier " + expectedFileName, e);
                }
            });
        } catch (final IOException e) {

            e.printStackTrace();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExceptionHandler.handle("Erreur lors de la création du fichier " + expectedFileName, e);
                }
            });
        }
        return null;
    }

    /**
     * Remplit le tableau
     * 
     * @param tableau
     * @param elt
     * @param id
     * @param sheet
     */
    private static void parseTableauXML(Element tableau, SQLRow row, SpreadSheet spreadsheet, SQLRow rowLanguage) {

        if (tableau == null) {
            return;
        }

        Object o = tableau.getAttributeValue("sheet");
        int idSheet = (o == null) ? 0 : Integer.valueOf(o.toString().trim());

        Sheet sheet = spreadsheet.getSheet(idSheet);
        // Derniere colonne du tableau permet de ne pas chercher sur toutes les colonnes
        // et d'optimiser la recherche
        Object oLastColTmp = tableau.getAttributeValue("lastColumn");
        int lastColumn = -1;
        int endPageLine = Integer.valueOf(tableau.getAttributeValue("endPageLine"));
        if (oLastColTmp != null) {
            lastColumn = sheet.resolveHint(oLastColTmp.toString() + 1).x + 1;
        }

        Map<String, Map<Integer, String>> mapStyle = searchStyle(sheet, lastColumn, endPageLine);

        if (tableau.getAttributeValue("table").equalsIgnoreCase("TVA")) {
            fillTaxe(tableau, sheet, mapStyle, false);
            return;
        }
        int nbPage = fillTable(tableau, row, sheet, mapStyle, true, rowLanguage);
        int firstLine = Integer.valueOf(tableau.getAttributeValue("firstLine"));
        int endLine = Integer.valueOf(tableau.getAttributeValue("endLine"));
        Object printRangeObj = sheet.getPrintRanges();

        System.err.println("Nombre de page == " + nbPage);
        if (nbPage == 1) {
            fillTable(tableau, row, sheet, mapStyle, false, rowLanguage);
        } else {
            if (printRangeObj != null) {
                String s = printRangeObj.toString();
                String[] range = s.split(":");

                for (int i = 0; i < range.length; i++) {
                    String string = range[i];
                    range[i] = string.subSequence(string.indexOf('.') + 1, string.length()).toString();
                }

                int rowEnd = -1;
                if (range.length > 1) {
                    rowEnd = sheet.resolveHint(range[1]).y + 1;
                    int rowEndNew = rowEnd * (nbPage);
                    String sNew = s.replaceAll(String.valueOf(rowEnd), String.valueOf(rowEndNew));
                    sheet.setPrintRanges(sNew);
                    System.err.println(" ******  Replace print ranges; Old:" + rowEnd + "--" + s + " New:" + rowEndNew + "--" + sNew);
                }
            }

            // le nombre d'éléments ne tient pas dans le tableau du modéle
            // On duplique la premiere page sans ce qui se trouve apres le tableau
            sheet.duplicateFirstRows(endLine, 1);

            // On agrandit le tableau pour qu'il remplisse la premiere page
            int lineToAdd = endPageLine - endLine;
            String repeatedCount = tableau.getAttributeValue("repeatedCount");
            if (repeatedCount != null && repeatedCount.trim().length() > 0) {
                int count = Integer.valueOf(repeatedCount);
                sheet.duplicateRows(firstLine, lineToAdd / count, count);
                final int rest = lineToAdd % count;
                // Si le nombre de ligne ne termine pas à la fin de la page
                if (rest != 0) {
                    sheet.insertDuplicatedRows(firstLine + lineToAdd - rest, rest);
                }
            } else {
                sheet.insertDuplicatedRows(firstLine, lineToAdd);
            }

            // On duplique la premiere page si on a besoin de plus de deux pages
            System.err.println("nbPage == " + nbPage);
            if (nbPage > 2) {
                sheet.duplicateFirstRows(endPageLine, nbPage - 2);
            }
            String pageRef = tableau.getAttributeValue("pageRef");
            if (pageRef != null && pageRef.trim().length() > 0) {
                MutableCell<SpreadSheet> cell = sheet.getCellAt(pageRef);
                cell.setValue("Page 1/" + nbPage);
                for (int i = 1; i < nbPage; i++) {
                    MutableCell<SpreadSheet> cell2 = sheet.getCellAt(cell.getX(), cell.getY() + (endPageLine * i));
                    cell2.setValue("Page " + (i + 1) + "/" + nbPage);
                }
            }
            fillTable(tableau, row, sheet, mapStyle, false, rowLanguage);
        }

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
    static Map<SQLRowAccessor, Map<String, Object>> taxe = new HashMap<SQLRowAccessor, Map<String, Object>>();
    private static Map<String, Map<Integer, SQLRowAccessor>> cacheForeign = new HashMap<String, Map<Integer, SQLRowAccessor>>();

    protected static SQLRowAccessor getForeignRow(SQLRowAccessor row, SQLField field) {
        Map<Integer, SQLRowAccessor> c = cacheForeign.get(field.getName());

        int i = row.getInt(field.getName());

        if (c != null && c.get(i) != null) {
            System.err.println("get foreign row From Cache ");
            return c.get(i);
        } else {

            SQLRowAccessor foreign = row.getForeign(field.getName());

            if (c == null) {
                Map<Integer, SQLRowAccessor> map = new HashMap<Integer, SQLRowAccessor>();
                map.put(i, foreign);
                cacheForeign.put(field.getName(), map);
            } else {
                c.put(i, foreign);
            }

            return foreign;
        }
        // return row.getForeignRow(field.getName());

    }

    private static int fillTable(Element tableau, SQLRow row, Sheet sheet, Map<String, Map<Integer, String>> mapStyle, boolean test, SQLRow rowLanguage) {

        if (tableau == null) {
            return 1;
        }

        int nbPage = 1;
        int nbCellules = 0;

        OOXMLTableElement tableElement = new OOXMLTableElement(tableau, row);
        int currentLineTmp = tableElement.getFirstLine();
        int currentLine = tableElement.getFirstLine();

        SQLElement styleElt = Configuration.getInstance().getDirectory().getElement("STYLE");

        boolean cache = false;
        String ref = tableau.getAttributeValue("table") + "_" + row.getTable().getName() + row.getID();
        if (rowsEltCache.get(ref) == null) {
            rowsEltCache.put(ref, tableElement.getRows());
        } else {
            cache = true;
        }
        List<Element> listElts = tableau.getChildren("element");

        // on remplit chaque ligne à partir des rows recuperées
        int numeroRef = 0;
        for (SQLRowAccessor rowElt : rowsEltCache.get(ref)) {
            numeroRef++;
            if (!cache && rowElt.getTable().getFieldRaw("ID_TAXE") != null) {
                SQLRowAccessor rowTaxe = getForeignRow(rowElt, rowElt.getTable().getField("ID_TAXE"));
                long ht = 0;
                if (rowElt.getTable().getFieldRaw("T_PV_HT") != null) {
                    ht = rowElt.getLong("T_PV_HT");
                }

                if (taxe.get(rowTaxe) != null) {

                    final Object object = taxe.get(rowTaxe).get("MONTANT_HT");
                    long montant = (object == null) ? 0 : (Long) object;
                    taxe.get(rowTaxe).put("MONTANT_HT", montant + ht);
                } else {
                    Map<String, Object> m = new HashMap<String, Object>();
                    m.put("MONTANT_HT", ht);
                    taxe.put(rowTaxe, m);
                }

            }

            Map<String, Integer> mapNbCel = new HashMap<String, Integer>();
            final boolean included = isIncluded(tableElement.getFilterId(), tableElement.getForeignTableWhere(), tableElement.getFilterId(), tableElement.getFieldWhere(), rowElt);
            if (included || tableElement.getTypeStyleWhere()) {

                String styleName = null;
                if (tableElement.getSQLElement().getTable().contains("ID_STYLE")) {
                    styleName = styleElt.getTable().getRow(rowElt.getInt("ID_STYLE")).getString("NOM");
                }

                if (included && tableElement.getTypeStyleWhere()) {
                    styleName = "Titre 1";
                }

                if (!included) {
                    styleName = "Normal";
                }

                int nbCellule = 1;

                String tmp;
                if (styleName != null && tableElement.getListBlankLineStyle().contains(styleName)) {
                    tmp = null;
                } else {
                    tmp = styleName;
                }
                boolean first = true;

                int tableLine = 1;
                int toAdd = 0;

                Map<Element, Object> mapValues = new HashMap<Element, Object>();
                for (Element e : listElts) {

                    OOXMLTableField tableField = new OOXMLTableField(e, rowElt, tableElement.getSQLElement(), rowElt.getID(), tableElement.getTypeStyleWhere() ? -1 : tableElement.getFilterId(),
                            rowLanguage, numeroRef);
                    final Object value = tableField.getValue();

                    mapValues.put(e, value);
                    fill("A1", value, sheet, false, null, tmp, true, tableField.isMultilineAuto());
                    final int fill = fill("A1", value, sheet, false, null, tmp, true, tableField.isMultilineAuto());
                    if ((currentLine + fill) > (tableElement.getEndPageLine() * nbPage)) {
                        currentLine = currentLineTmp + tableElement.getEndPageLine();
                        // currentLine = nbPage * endLine + fisrtLine;
                        currentLineTmp = currentLine;
                        nbPage++;
                    }
                }

                // on remplit chaque cellule de la ligne
                for (Element e : listElts) {

                    OOXMLTableField tableField = new OOXMLTableField(e, rowElt, tableElement.getSQLElement(), rowElt.getID(), tableElement.getTypeStyleWhere() ? -1 : tableElement.getFilterId(),
                            rowLanguage, numeroRef);

                    if (!test && styleName != null && tableElement.getListBlankLineStyle().contains(styleName) && first) {
                        toAdd++;
                        currentLine++;
                        // nbCellule = Math.max(nbCellule, 2);
                        first = false;
                    }

                    if (mapNbCel.get(e.getAttributeValue("location").trim()) != null) {
                        nbCellule = mapNbCel.get(e.getAttributeValue("location").trim());
                    } else {
                        nbCellule = 1;
                    }
                    int line = tableField.getLine();
                    if (tableField.getLine() > 1) {
                        line = Math.max(nbCellule + ((tableLine == tableField.getLine()) ? 0 : 1), tableField.getLine());
                    }
                    tableLine = tableField.getLine();
                    String loc = e.getAttributeValue("location").trim() + (currentLine + (line - 1));

                    // Cellule pour un style défini
                    List<String> listBlankStyle = tableField.getBlankStyle();

                    // nbCellule = Math.max(nbCellule, tableField.getLine());

                    if (styleName == null || !listBlankStyle.contains(styleName)) {

                        try {
                            Object value = mapValues.get(e);
                            // if (value != null && value.toString().trim().length() > 0) {
                            if (tableField.isNeeding2Lines() && tableField.getLine() == 1) {
                                loc = e.getAttributeValue("location").trim() + (currentLine + 1);
                                styleName = null;
                            }
                            final Point resolveHint = sheet.resolveHint(loc);
                            if (test || sheet.isCellValid(resolveHint.x, resolveHint.y)) {

                                String styleNameTmp = styleName;
                                if (tableField.getStyle().trim().length() > 0) {
                                    styleNameTmp = tableField.getStyle();
                                }

                                Map<Integer, String> mTmp = styleName == null ? null : mapStyle.get(styleNameTmp);
                                String styleOO = null;
                                if (mTmp != null) {

                                    Object oTmp = mTmp.get(Integer.valueOf(resolveHint.x));
                                    styleOO = oTmp == null ? null : oTmp.toString();
                                }

                                int tmpCelluleAffect = fill(test ? "A1" : loc, value, sheet, tableField.isTypeReplace(), null, styleOO, test, tableField.isMultilineAuto());
                                // tmpCelluleAffect = Math.max(tmpCelluleAffect,
                                // tableField.getLine());
                                if (tableField.getLine() != 1 && (!tableField.isLineOption() || (value != null && value.toString().trim().length() > 0))) {
                                    if (nbCellule >= tableField.getLine()) {
                                        tmpCelluleAffect = tmpCelluleAffect + nbCellule;
                                    } else {
                                        tmpCelluleAffect += tableField.getLine() - 1;
                                    }
                                }

                                if (tableField.isNeeding2Lines()) {
                                    nbCellule = Math.max(nbCellule, 2);
                                } else {
                                    nbCellule = Math.max(nbCellule, tmpCelluleAffect);
                                }
                            } else {
                                System.err.println("Cell not valid at " + loc);
                            }
                            // }
                        } catch (IndexOutOfBoundsException indexOut) {
                            System.err.println("Cell not valid at " + loc);
                        }
                    }
                    mapNbCel.put(e.getAttributeValue("location").trim(), nbCellule);
                }

                for (String s : mapNbCel.keySet()) {
                    nbCellule = Math.max(nbCellule, mapNbCel.get(s));
                }

                currentLine += nbCellule;
                nbCellules += (nbCellule + toAdd);

            }
        }
        int d = nbCellules / (tableElement.getEndPageLine() - tableElement.getFirstLine());
        int r = nbCellules % (tableElement.getEndPageLine() - tableElement.getFirstLine());

        if (d == 0) {
            d++;
            if (nbCellules > (tableElement.getEndLine() - tableElement.getFirstLine() + 1)) {
                d++;
            }
        } else {
            if (r > (tableElement.getEndLine() - tableElement.getFirstLine() + 1)) {
                d += 2;
            } else {
                d++;
            }
        }
        return d;
    }

    private static void fillTaxe(Element tableau, Sheet sheet, Map<String, Map<Integer, String>> mapStyle, boolean test) {

        int line = Integer.valueOf(tableau.getAttributeValue("firstLine"));
        List<Element> listElts = tableau.getChildren("element");

        for (SQLRowAccessor rowTaxe : taxe.keySet()) {

            Map<String, Object> m = taxe.get(rowTaxe);
            // on remplit chaque cellule de la ligne
            for (Element e : listElts) {

                String loc = e.getAttributeValue("location").trim() + line;
                String name = e.getAttributeValue("name");
                String typeComp = e.getAttributeValue("type");
                if (name == null) {
                    System.err.println("OOgenerationXML.fillTaxe() --> name == null");
                } else {
                    Object value = m.get(name);
                    if (name.equalsIgnoreCase("MONTANT_TVA")) {
                        value = Math.round(((Long) m.get("MONTANT_HT") * rowTaxe.getFloat("TAUX") / 100.0));
                    } else if (name.equalsIgnoreCase("NOM")) {
                        value = rowTaxe.getString("NOM");
                        // TODO prefix et suffix
                        String prefix = e.getAttributeValue("prefix");
                        if (prefix != null) {
                            value = prefix + value;
                        }
                        String suffix = e.getAttributeValue("suffix");
                        if (suffix != null) {
                            value = value + suffix;
                        }
                    }
                    if (typeComp != null && typeComp.equalsIgnoreCase("Devise")) {

                        value = Double.valueOf(GestionDevise.currencyToString((Long) value, false));
                    }
                    fill(test ? "A1" : loc, value, sheet, false, null, null, test, false);
                }
            }
            line++;
        }
    }

    /**
     * Parse l'ensemble des éléments du fichier et insere les valeurs dans le fichier sxc
     * 
     * @param elts
     * @param sqlElt
     * @param id
     */
    private static void parseElementsXML(List<Element> elts, SQLRow row, SpreadSheet spreadSheet) {
        SQLElement sqlElt = Configuration.getInstance().getDirectory().getElement(row.getTable());
        for (Element elt : elts) {

            OOXMLElement OOElt = new OOXMLElement(elt, sqlElt, row.getID(), row);
            Object result = OOElt.getValue();
            if (result != null) {
                Object o = elt.getAttributeValue("sheet");
                int sheet = (o == null) ? 0 : Integer.valueOf(o.toString().trim());
                fill(elt.getAttributeValue("location"), result, spreadSheet.getSheet(sheet), OOElt.isTypeReplace(), OOElt.getReplacePattern(), null, false, OOElt.isMultilineAuto());
            }
        }
    }

    private static boolean isIncluded(int filterID, String foreignTable, int id, String fieldWhere, SQLRowAccessor rowElt) {

        // No filter
        if (filterID <= 1) {
            return true;
        } else {
            // Filter id in foreign table (FICHE_RENDEZ_VOUS<-POURCENT_SERVICE[ID_VERIFICATEUR]
            if (foreignTable != null) {
                boolean b = false;
                SQLTable table = Configuration.getInstance().getRoot().findTable(foreignTable);
                Collection<? extends SQLRowAccessor> set = rowElt.getReferentRows(table);
                for (SQLRowAccessor row : set) {
                    b = b || (row.getInt(fieldWhere) == filterID);
                }
                return b;
            } else {
                return (filterID == id);
            }
        }
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
    private static int fill(String location, Object value, Sheet sheet, boolean replace, String replacePattern, String styleOO, boolean test, boolean controleMultiline) {

        int nbCellule = (test && styleOO == null) ? 2 : 1;
        // est ce que la cellule est valide
        if (test || sheet.isCellValid(sheet.resolveHint(location).x, sheet.resolveHint(location).y)) {

            MutableCell cell = sheet.getCellAt(location);

            // on divise en 2 cellules si il y a des retours à la ligne
            if (controleMultiline && value != null && value.toString().indexOf('\n') >= 0) {
                String[] values = value.toString().split("\n");
                if (!test) {

                    Point p = sheet.resolveHint(location);
                    int y = 0;
                    for (String string : values) {
                        if (string != null && string.trim().length() != 0) {
                            try {
                                MutableCell c = sheet.getCellAt(p.x, p.y + y);
                                setCellValue(c, string, replace, replacePattern);
                                if (styleOO != null) {
                                    c.setStyleName(styleOO);
                                }
                                y++;
                            } catch (IllegalArgumentException e) {
                                JOptionPane.showMessageDialog(null, "La cellule " + location + " n'existe pas ou est fusionnée.", "Erreur pendant la génération", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }

                    // String firstPart = value.toString().substring(0,
                    // value.toString().indexOf('\n'));
                    // String secondPart = value.toString().substring(value.toString().indexOf('\n')
                    // + 1, value.toString().length());
                    // secondPart = secondPart.replace('\n', ',');
                    // // System.err.println("Set cell value 1 " + value);
                    // setCellValue(cell, firstPart, replace, replacePattern);
                    // if (styleOO != null) {
                    // cell.setStyleName(styleOO);
                    // }
                    //
                    // Point p = sheet.resolveHint(location);
                    // System.err.println("Get Cell At " + p.x + " : " + p.y);

                }
                nbCellule = values.length;
            } else {
                if (!test) {
                    // application de la valeur
                    // System.err.println("Set cell value 2 " + value);
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
            return;
            // value = "";
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
        final String langage = rowLanguage != null ? rowLanguage.getString("CHEMIN") : null;
        // Test des arguments
        if (ssheet == null || pathDest == null || fileName.trim().length() == 0) {
            throw new IllegalArgumentException();
        }

        // Renommage du fichier si il existe déja
        File fDest = new File(pathDest, fileName + ".ods");

        if (!pathDest.exists()) {
            pathDest.mkdirs();
        }

        fDest = SheetUtils.convertToOldFile(fileName, pathDest, fDest);

        // Sauvegarde
        try {
            ssheet.saveAs(fDest);
        } catch (FileNotFoundException e) {
            final File file = fDest;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        JOptionPane.showMessageDialog(null, "Le fichier " + file.getCanonicalPath() + " n'a pu être créé. \n Vérifiez qu'il n'est pas déjà ouvert.");
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
            final InputStream odspIn = TemplateManager.getInstance().getTemplatePrintConfiguration(templateId, langage, null);
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
                            }
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        mapCellStyle.put(Integer.valueOf(x), cellStyle);
                        if (style.trim().length() != 0) {
                            c.clearValue();
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
            }
        }
        cacheStyle.put(sheet, mapStyleDef);
        return mapStyleDef;
    }

    public static boolean needAnnexe(String templateId, SQLRow row, SQLRow rowLanguage) {
        final String langage = rowLanguage != null ? rowLanguage.getString("CHEMIN") : null;
        final SAXBuilder builder = new SAXBuilder();
        try {
            final InputStream xmlConfiguration = TemplateManager.getInstance().getTemplateConfiguration(templateId, langage, null);
            final Document doc = builder.build(xmlConfiguration);
            final InputStream template = TemplateManager.getInstance().getTemplate(templateId, langage, null);

            final SpreadSheet spreadSheet = new ODPackage(template).getSpreadSheet();

            // On initialise un nouvel élément racine avec l'élément racine du document.
            Element racine = doc.getRootElement();

            List<Element> listTable = racine.getChildren("table");

            Element tableau;
            if (listTable.size() == 0) {
                return false;
            } else {
                if (listTable.get(0).getAttributeValue("table").equalsIgnoreCase("TVA")) {
                    tableau = listTable.get(1);
                } else {
                    tableau = listTable.get(0);
                }
            }
            final Sheet sheet = spreadSheet.getSheet(0);

            Object oLastColTmp = tableau.getAttributeValue("lastColumn");
            int lastColumn = -1;
            int endPageLine = Integer.valueOf(tableau.getAttributeValue("endPageLine"));
            if (oLastColTmp != null) {
                lastColumn = sheet.resolveHint(oLastColTmp.toString() + 1).x + 1;
            }

            Map<String, Map<Integer, String>> mapStyle = searchStyle(sheet, lastColumn, endPageLine);

            int nbPage = fillTable(tableau, row, sheet, mapStyle, true, rowLanguage);

            return nbPage > 1;
        } catch (final JDOMException e) {

            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected static String getStringProposition(SQLRow rowProp) {

        return "Notre proposition " + rowProp.getString("NUMERO") + " du " + dateFormat.format(rowProp.getObject("DATE"));
    }

    public static void main(String[] args) {
        ComptaPropsConfiguration conf = ComptaPropsConfiguration.create();
        System.err.println("Conf created");
        Configuration.setInstance(conf);
        conf.setUpSocieteDataBaseConnexion(36);
        System.err.println("Connection Set up");
        SQLElement elt = Configuration.getInstance().getDirectory().getElement("DEVIS");

        System.err.println("Start Genere");
        // genere("Devis", "C:\\", "Test", elt.getTable().getRow(19));
        System.err.println("Stop genere");
    }
}
