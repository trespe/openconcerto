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

import static org.openconcerto.task.config.ComptaBasePropsConfiguration.getStream;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.map.model.Ville;
import org.openconcerto.odtemplate.Template;
import org.openconcerto.odtemplate.engine.OGNLDataModel;
import org.jopendocument.link.Component;
import org.jopendocument.link.OOConnexion;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.Tuple2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


public abstract class AbstractJOOReportsSheet {
    private static final String defaultLocationTemplate = SpreadSheetGenerator.defaultLocationTemplate;
    protected static final DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
    protected static final DateFormat dateFormat2 = new SimpleDateFormat("dd/MM/yy");
    protected static final DateFormat yearFormat = new SimpleDateFormat("yyyy");
    private String year;
    protected String locationTemplate = TemplateNXProps.getInstance().getStringProperty("LocationTemplate");
    private String locationOO, locationPDF;
    protected String templateFileName;
    private String printer;
    protected boolean askOverwriting = false;

    /**
     * @return une Map contenant les valeurs à remplacer dans la template
     */
    abstract protected Map createMap();

    abstract public String getFileName();

    protected void init(String year, String templateFileName, String attributePrinter, Tuple2<String, String> t) {
        this.year = year;
        this.templateFileName = templateFileName;
        this.locationOO = SheetXml.getLocationForTuple(t, false) + File.separator + this.year;
        this.locationPDF = SheetXml.getLocationForTuple(t, true) + File.separator + this.year;
        this.printer = PrinterNXProps.getInstance().getStringProperty(attributePrinter);
    }

    public final void generate(boolean print, boolean show, String printer) {
        generate(print, show, printer, false);
    }

    /**
     * Genere le document OO, le pdf, et ouvre le document OO
     * 
     * @param print
     * @param show
     * @param printer
     */
    public void generate(boolean print, boolean show, String printer, boolean overwrite) {

        if (this.locationTemplate.trim().length() == 0) {
            this.locationTemplate = defaultLocationTemplate;
        }
        try {

            String fileName = getFileName();
            final InputStream fileTemplate = getStream(this.templateFileName, this.locationTemplate, defaultLocationTemplate);

            File fileOutOO = new File(this.locationOO, fileName + ".odt");
            // File fileOutPDF = new File(locationPropositionPDF, fileName);

            if (fileOutOO.exists() && overwrite) {
                if (this.askOverwriting) {
                    int answer = JOptionPane.showConfirmDialog(null, "Voulez vous écraser le document ?", "Remplacement d'un document", JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        SheetUtils.getInstance().convertToOldFile(fileName, new File(this.locationOO), fileOutOO, ".odt");
                    }
                } else {
                    SheetUtils.getInstance().convertToOldFile(fileName, new File(this.locationOO), fileOutOO, ".odt");
                }
            }

            if (!fileOutOO.exists()) {
                fileOutOO.getParentFile().mkdirs();
                Template template;
                // try {
                template = new Template(new BufferedInputStream(fileTemplate));

                // creation du document
                final Map createMap = createMap();
                OGNLDataModel model = new OGNLDataModel(createMap);

                model.putAll(createMap);
                template.createDocument(model).saveAs(fileOutOO);
                // template.createDocument(model, new BufferedOutputStream(new
                // FileOutputStream(fileOutOO)));
                // } catch (JDOMException e) {
                // e.printStackTrace();
                // }
            }

            // ouverture de OO
            if (show || print) {

                try {
                    final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                    if (ooConnexion == null) {
                        return;
                    }
                    final Component doc = ooConnexion.loadDocument(fileOutOO, !show);

                    if (this.savePDF())
                        doc.saveToPDF(new File(this.locationPDF, fileName + ".pdf"), "writer_pdf_Export");

                    if (print) {
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("Name", printer);
                        doc.printDocument(map);
                    }
                    doc.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            ExceptionHandler.handle("Impossible de trouver le modéle.", e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (org.openconcerto.odtemplate.TemplateException e) {
            e.printStackTrace();
        }
    }

    protected boolean savePDF() {
        return false;
    }

    public void showDocument() {
        File fileOutOO = new File(this.locationOO, getFileName() + ".odt");
        if (fileOutOO.exists()) {
            try {
                final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                if (ooConnexion == null) {
                    return;
                }
                ooConnexion.loadDocument(fileOutOO, false);

            } catch (Exception e) {
                e.printStackTrace();
                ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
            }

        } else {
            generate(false, true, "");
        }
    }

    public void printDocument() {
        File fileOutOO = new File(this.locationOO, getFileName() + ".odt");
        if (fileOutOO.exists()) {

            try {
                final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                if (ooConnexion == null) {
                    return;
                }
                final Component doc = ooConnexion.loadDocument(fileOutOO, true);

                Map<String, Object> map = new HashMap<String, Object>();
                map.put("Name", printer);
                doc.printDocument(map);
                doc.close();

            } catch (Exception e) {
                e.printStackTrace();
                ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
            }

        } else {
            generate(true, false, this.printer);
        }
    }

    public void fastPrintDocument() {

        final File f = new File(this.locationOO, getFileName() + ".odt");

        if (!f.exists()) {
            generate(true, false, this.printer);
        } else {

            try {
                final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                if (ooConnexion == null) {
                    return;
                }
                final Component doc = ooConnexion.loadDocument(f, true);

                Map<String, Object> map = new HashMap<String, Object>();
                map.put("Name", this.printer);
                Map<String, Object> map2 = new HashMap<String, Object>();
                map2.put("CopyCount", 1);
                doc.printDocument(map, map2);
                doc.close();

            } catch (Exception e) {

                ExceptionHandler.handle("Impossible de charger le document OpentOffice", e);
                e.printStackTrace();
            }
        }
    }

    public boolean exists() {

        String fileName = getFileName();
        File fileOutOO = new File(this.locationOO, fileName + ".odt");
        return fileOutOO.exists();
    }

    protected String getInitiales(SQLRow row) {
        String init = "";
        if (row != null) {
            final String stringPrenom = row.getString("PRENOM");
            if (stringPrenom != null && stringPrenom.trim().length() != 0) {
                init += stringPrenom.trim().charAt(0);
            }
            final String stringNom = row.getString("NOM");
            if (stringNom != null && stringNom.trim().length() != 0) {
                init += stringNom.trim().charAt(0);
            }
        }
        return init;
    }

    public void exportToPdf() {

        // Export vers PDF
        String fileName = getFileName();
        File fileOutOO = new File(this.locationOO, fileName + ".odt");
        File fileOutPDF = new File(this.locationPDF, fileName);

        if (!fileOutOO.exists()) {
            generate(false, false, "");
        }

        try {

            final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
            if (ooConnexion == null) {
                return;
            }
            final Component doc = ooConnexion.loadDocument(fileOutOO, true);
            doc.saveToPDF(fileOutPDF, "writer_pdf_Export");
            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
            ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
        }
        // Ouverture
        int result = JOptionPane.showOptionDialog(null, "Ouvrir le pdf ?", "Ouverture du PDF", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

        if (result == JOptionPane.YES_OPTION) {
            Gestion.openPDF(new File(fileOutPDF.getAbsolutePath() + ".pdf"));
        } else {
            try {
                FileUtils.openFile(fileOutPDF.getParentFile());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Impossible d'ouvrir le dossier : " + fileOutPDF.getParentFile() + ".");
            }
        }

    }

    protected static String getVille(final String name) {

        Ville ville = Ville.getVilleFromVilleEtCode(name);
        if (ville == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(null, "La ville " + "\"" + name + "\"" + " est introuvable! Veuillez corriger l'erreur!");
                }
            });
            return null;
        }
        return ville.getName();
    }

    public String getLocationOO() {
        return locationOO;
    }

    protected static String getVilleCP(String name) {
        Ville ville = Ville.getVilleFromVilleEtCode(name);
        if (ville == null) {

            return null;
        }
        return ville.getCodepostal();
    }
}
