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

import static org.openconcerto.task.config.ComptaBasePropsConfiguration.getStreamStatic;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.PreviewFrame;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.openoffice.ODPackage;
import org.jopendocument.link.Component;
import org.openconcerto.openoffice.spreadsheet.MutableCell;
import org.openconcerto.openoffice.spreadsheet.Sheet;
import org.openconcerto.openoffice.spreadsheet.SpreadSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.StreamUtils;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jopendocument.model.OpenDocument;

public abstract class SpreadSheetGenerator implements Runnable {

    private Map mCell, mapStyleRow;

    static private String modelDir;
    private String modele, destFileName;
    private File destDirOO, destDirPDF;
    protected int nbPage, nbRowsPerPage;
    private boolean visu, impression;
    private String printer;
    private boolean exportPDF;
    private Map mapReplaceText;
    private String fODSP = null;

    protected static final String defaultLocationTemplate = "/Configuration/Template/Default/";

    static {
        String loc = TemplateNXProps.getInstance().getStringProperty("LocationTemplate");
        if (loc.trim().length() == 0) {
            loc = defaultLocationTemplate;
        }
        setModelDir(loc);
    }

    public static void setModelDir(String loc) {
        modelDir = loc;
        System.err.println("Repertoire des template : " + modelDir);
    }

    protected void searchStyle(Sheet sheet, Map<String, Map<Integer, String>> mapStyleDef, int colEnd, int rowEnd) {

        // on parcourt chaque ligne de la feuille pour recuperer les styles
        int columnCount = (colEnd == -1) ? sheet.getColumnCount() : (colEnd + 1);
        System.err.println("End column search : " + columnCount);

        int rowCount = (rowEnd > 0) ? rowEnd : sheet.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            int x = 0;
            Map<Integer, String> mapCellStyle = new HashMap<Integer, String>();
            String style = "";

            for (int j = 0; j < columnCount; j++) {

                if (sheet.isCellValid(j, i)) {

                    MutableCell c = sheet.getCellAt(j, i);
                    String cellStyle = c.getStyleName();

                    if (mapStyleDef.containsKey(c.getValue().toString())) {
                        style = c.getValue().toString();

                    }

                    mapCellStyle.put(new Integer(x), cellStyle);
                    if (style.trim().length() != 0) {
                        c.clearValue();
                    }
                }
                x++;
            }

            if (style.length() > 0) {
                mapStyleDef.put(style, mapCellStyle);

            }
        }
    }

    protected void fill(Sheet sheet, Map<String, Map<Integer, String>> mapStyleDef) {

        for (Iterator i = this.mCell.keySet().iterator(); i.hasNext();) {

            Object o = i.next();
            if (this.mCell.get(o) != null) {
                // System.err.println(o + " ---> " + m.get(o).toString());
                final Point p = sheet.resolveHint(o.toString());
                Object value = this.mCell.get(o);
                boolean cellValid = false;
                try {
                    cellValid = sheet.isCellValid(p.x, p.y);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                if (cellValid) {
                    MutableCell cell = sheet.getCellAt(p.x, p.y);
                    // on replace les retours a la ligne
                    final String stringValue = value.toString();
                    if (value != null && stringValue.indexOf('\n') >= 0) {

                        String firstPart = stringValue.substring(0, stringValue.indexOf('\n'));
                        String secondPart = stringValue.substring(stringValue.indexOf('\n') + 1, stringValue.length());
                        secondPart = secondPart.replace('\n', ',');
                        cell.setValue(firstPart);

                        try {
                            MutableCell cellSec = sheet.getCellAt(p.x, p.y + 1);
                            cellSec.setValue(secondPart);
                        } catch (Exception e) {
                            e.printStackTrace();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    JOptionPane.showMessageDialog(null, "Impossible d'accéder à la cellule " + p.x + ";" + (p.y + 1), "Erreur accés cellule", JOptionPane.INFORMATION_MESSAGE);
                                }
                            });
                        }
                    } else {
                        cell.setValue(((value == null) ? "" : value));

                        Object sty = this.mapStyleRow.get(new Integer(p.y + 1));
                        if (sty != null) {
                            Map<Integer, String> styleToApply = mapStyleDef.get(sty.toString());
                            if (styleToApply != null) {

                                if (styleToApply.get(new Integer(p.x)) != null) {
                                    cell.setStyleName(styleToApply.get(new Integer(p.x)));
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    protected void replace(Sheet sheet) {
        for (Iterator i = this.mapReplaceText.keySet().iterator(); i.hasNext();) {
            Object o = i.next();
            if (this.mapReplaceText.get(o) != null) {

                Object value = this.mapReplaceText.get(o);
                MutableCell cell = sheet.getCellAt(o.toString());

                cell.replaceBy("_", ((value == null) ? "" : value.toString()));
            }
        }
    }

    protected SpreadSheet loadTemplate() throws IOException {
        InputStream f = getStreamStatic(modelDir + this.modele);
        fODSP = modelDir + this.modele + "p";
        if (f == null) {
            f = getStreamStatic(defaultLocationTemplate + this.modele);
            fODSP = defaultLocationTemplate + this.modele + "p";
            if (f == null) {
                ExceptionHandler.handle("Modele " + this.modele + " introuvable. Impossible de générer le document.");
                System.err.println("Modele introuvable : " + (defaultLocationTemplate + this.modele));
                fODSP = null;
                return null;
            }
        }
        final SpreadSheet res = SpreadSheet.create(new ODPackage(f));
        f.close();
        return res;
    }

    protected File save(SpreadSheet ssheet) throws IOException {
        File fDest = new File(this.destDirOO, this.destFileName + ".ods");

        int i = 0;
        String destName = this.destFileName;

        File oldPath = new File(this.destDirOO, "Historique");
        oldPath.mkdirs();
        while (fDest.exists()) {
            destName = this.destFileName + "_" + i;
            fDest = new File(oldPath, destName + ".ods");
            i++;
        }
        File fTmp = new File(this.destDirOO, this.destFileName + ".ods");
        fTmp.renameTo(fDest);

        fDest = new File(this.destDirOO, this.destFileName + ".ods");
        final InputStream stream = getStreamStatic(fODSP);
        if (stream != null) {
            // Copie de l'odsp
            File odspOut = new File(this.destDirOO, this.destFileName + ".odsp");
            StreamUtils.copy(stream, odspOut);
            stream.close();

        }
        try {
            ssheet.saveAs(fDest);
        } catch (FileNotFoundException e) {
            final File file = fDest;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        ExceptionHandler.handle("Le fichier " + ((file == null) ? "" : file.getCanonicalPath()) + " n'a pu être créé. \n Vérifiez qu'il n'est pas déjà ouvert.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            e.printStackTrace();
        }
        return fDest;
    }

    public SpreadSheetGenerator(SheetInterface sheet, String destFileName, boolean impr, boolean visu) {
        this(sheet, destFileName, impr, visu, true);
    }

    public SpreadSheetGenerator(SheetInterface sheet, String destFileName, boolean impr, boolean visu, boolean exportPDF) {

        this.modele = sheet.modele;
        this.mCell = sheet.mCell;
        this.destDirOO = new File(sheet.locationOO);
        this.destDirOO.mkdirs();
        this.destDirPDF = new File(sheet.locationPDF);
        this.destDirPDF.mkdirs();
        this.nbPage = sheet.nbPage;
        this.nbRowsPerPage = sheet.nbRowsPerPage;
        this.destFileName = destFileName;
        this.mapStyleRow = sheet.mapStyleRow;
        this.mapReplaceText = sheet.mapReplace;
        this.visu = visu;
        this.impression = impr;
        this.printer = sheet.getPrinter();
        this.exportPDF = exportPDF;
    }

    protected abstract File generateWithStyle() throws IOException;

    public void run() {
        File f = null;
        try {

            f = generateWithStyle();

            try {

                if (!Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {

                    final Component doc = ComptaPropsConfiguration.getOOConnexion().loadDocument(f, !this.visu);
                    if (this.exportPDF) {
                        doc.saveToPDF(new File(this.destDirPDF.getAbsolutePath(), this.destFileName + ".pdf"));
                    }

                    if (this.impression) {
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("Name", this.printer);
                        doc.printDocument(map);

                    }
                    doc.close();
                } else {
                    final OpenDocument doc = new OpenDocument(f);
                    if (this.visu) {
                        PreviewFrame.show(f);
                    }

                    SheetUtils.getInstance().convert2PDF(doc, f, this.destFileName);
                    if (this.impression) {
                        // Print !
                        DefaultNXDocumentPrinter printer = new DefaultNXDocumentPrinter();
                        printer.print(doc);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
                ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
            }

            // }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Fichier déjà ouvert!");

            final File ff = f;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExceptionHandler.handle("Le fichier " + ((ff == null) ? "" : ff.getAbsolutePath()) + " n'a pu être créé. \n Vérifiez qu'il n'est pas déjà ouvert.");
                }
            });
        }

        fireGenerateEnd();
    }

    private final List<SpreadSheetGeneratorListener> listeners = new ArrayList<SpreadSheetGeneratorListener>();

    private void fireGenerateEnd() {
        for (SpreadSheetGeneratorListener listener : listeners) {
            listener.taskEnd();
        }
    }

    public void addGenerateListener(SpreadSheetGeneratorListener l) {
        this.listeners.add(l);
    }
}
