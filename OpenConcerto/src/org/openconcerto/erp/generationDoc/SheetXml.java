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
import org.openconcerto.erp.core.common.ui.FastPrintAskFrame;
import org.openconcerto.erp.core.common.ui.PreviewFrame;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.jopendocument.link.Component;
import org.jopendocument.link.OOConnexion;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.jopendocument.model.OpenDocument;
import org.jopendocument.print.DefaultDocumentPrinter;

public abstract class SheetXml {

    protected SQLElement elt;

    // nom de l'imprimante à utiliser
    protected String printer;

    // id
    protected SQLRow row;

    // emplacement du fichier OO généré
    protected String locationOO;

    // emplacement du fichier PDF généré
    protected String locationPDF;

    // nom du modele sans extension
    protected String modele;

    protected File f;

    public static final Tuple2<String, String> tupleDefault = Tuple2.create("Default", "Autres");

    protected static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

    // single threaded and kill its thread after 3 seconds (to allow the program to exit)
    protected static final ExecutorService runnableQueue = new ThreadPoolExecutor(0, 1, 3L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    protected static UncaughtExceptionHandler DEFAULT_HANDLER = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            e.printStackTrace();
        }
    };

    public void useOO(final File f, final boolean visu, final boolean impression, final String fileName) {
        useOO(f, visu, impression, fileName, true);
    }

    public void useOO(final File f, final boolean visu, final boolean impression, final String fileName, boolean exportPDF) {

        if (f == null || fileName.trim().length() == 0) {
            ExceptionHandler.handle("Erreur lors de la génération du fichier " + fileName);
            return;
        }

        try {
            if (!Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {
                final Component doc = ComptaPropsConfiguration.getOOConnexion().loadDocument(f, !visu);

                if (exportPDF) {
                    doc.saveToPDF(getFilePDF()).get();
                }

                if (impression) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("Name", this.printer);
                    doc.printDocument(map);
                }
                doc.close();
            } else {
                final OpenDocument doc = new OpenDocument(f);

                if (exportPDF) {
                    final Thread t = new Thread("PDF Export: " + fileName) {
                        @Override
                        public void run() {

                            try {
                                SheetUtils.getInstance().convert2PDF(doc, f, fileName);
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    };
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();

                }
                if (visu) {
                    showPreviewDocument();
                }
                if (impression) {
                    // Print !
                    DefaultDocumentPrinter printer = new DefaultDocumentPrinter();
                    printer.print(doc);

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
        }
    }

    public abstract Future<File> genere(final boolean visu, final boolean impression);

    public abstract String getFileName();

    private String getOOName() {
        return getValidFileName(getFileName()) + ".ods";
    }

    private String getPDFName() {
        return getValidFileName(getFileName()) + ".pdf";
    }

    private String getOO1Name() {
        return getValidFileName(getFileName()) + ".sxc";
    }

    /**
     * retourne l'emplacement de destination d'un Tuple<id (ex:LocationDevis), Nom (ex:Devis)>
     */
    public static String getLocationForTuple(Tuple2<String, String> t, boolean pdf) {

        final String stringProperty = TemplateNXProps.getInstance().getStringProperty(t.get0() + (pdf ? "PDF" : "OO"));
        if (stringProperty.equalsIgnoreCase(TemplateNXProps.getInstance().getDefaultStringValue())) {
            return stringProperty + File.separator + t.get1();
        } else {
            return stringProperty;
        }
    }

    private File getFile() {
        if (this.f != null) {
            return f;
        }
        File f = new File(this.locationOO + File.separator + getOOName());
        if (!f.exists()) {
            File f2 = new File(this.locationOO + File.separator + getOO1Name());
            if (f2.exists()) {
                return f2;
            } else {
                return f;
            }
        } else {
            return f;
        }
    }

    public File getFilePDF() {
        File f = new File(this.locationPDF + File.separator + getPDFName());
        return f;
    }

    public File getFileWithoutExt() {
        File f = new File(this.locationPDF + File.separator + getValidFileName(getFileName()));
        return f;
    }

    public File getFileODS() {
        File f = new File(this.locationOO + File.separator + getOOName());
        return f;
    }

    // private String getPDFName() {
    // return getFileName() + ".pdf";
    // }

    public void showDocument() {

        final File f = getFile();

        try {
            final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
            if (ooConnexion == null) {
                return;
            }
            ooConnexion.loadDocument(f, false);
        } catch (Exception e) {

            e.printStackTrace();
            ExceptionHandler.handle("Impossible de charger le document OpentOffice", e);
        }
    }

    public void showPreviewDocument() {
        final File f = getFile();
        PreviewFrame.show(f);
    }

    public void fastPrintDocument() {
        FastPrintAskFrame f = new FastPrintAskFrame(this);
        f.display();
    }

    public void fastPrintDocument(short copies) {
        final File f = getFile();

        try {
            if (!Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {

                final Component doc = ComptaPropsConfiguration.getOOConnexion().loadDocument(f, true);

                Map<String, Object> map = new HashMap<String, Object>();
                if (this.printer == null || this.printer.trim().length() == 0) {
                    PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
                    this.printer = printer.getName();
                }
                map.put("Name", this.printer);
                Map<String, Object> map2 = new HashMap<String, Object>();
                map2.put("CopyCount", copies);

                // http://www.openoffice.org/issues/show_bug.cgi?id=99606
                // fix bug if collate = false then print squared number of copies
                map2.put("Collate", Boolean.TRUE);
                doc.printDocument(map, map2);
                doc.close();

            } else {
                // Load the spreadsheet.
                final OpenDocument doc = new OpenDocument(f);

                // Print !
                DefaultNXDocumentPrinter printer = new DefaultNXDocumentPrinter(this.printer, copies);
                printer.print(doc);
            }
        } catch (Exception e) {

            ExceptionHandler.handle("Impossible de charger le document OpentOffice", e);
            e.printStackTrace();
        }
    }

    public void printDocument() {
        final File f = getFile();

        try {

            if (!Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {

                final Component doc = ComptaPropsConfiguration.getOOConnexion().loadDocument(f, true);
                doc.printDocument();
                doc.close();
            } else {
                // Load the spreadsheet.
                final OpenDocument doc = new OpenDocument(f);

                // Print !
                DefaultNXDocumentPrinter printer = new DefaultNXDocumentPrinter();
                printer.print(doc);
            }

        } catch (Exception e) {

            ExceptionHandler.handle("Impossible de charger le document OpentOffice", e);
            e.printStackTrace();
        }
    }

    public boolean isFileOOExist() {
        final File f = getFile();
        return f.exists();
    }

    public SQLRow getSQLRow() {
        return this.row;
    }

    public boolean isFileODSExist() {
        final File f = getFileODS();
        return f.exists();
    }

    /**
     * Remplace tous les caracteres non alphanumeriques (seul le _ est autorisé) par un -. Cela
     * permet d'avoir toujours un nom de fichier valide.
     * 
     * @param fileName nom du fichier à créer ex:FACTURE_2007/03/001
     * @return un nom fichier valide ex:FACTURE_2007-03-001
     */
    public static String getValidFileName(String fileName) {
        StringBuffer result = new StringBuffer(fileName.length());
        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);

            // Si c'est un caractere alphanumerique
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c == '_') || (c == ' ')) {
                result.append(c);
            } else {
                result.append('-');
            }
        }
        return result.toString();
    }

    public String getPrinter() {
        return this.printer;
    }

}
