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
import org.openconcerto.erp.generationDoc.element.TypeModeleSQLElement;
import org.openconcerto.erp.storage.CloudStorageEngine;
import org.openconcerto.erp.storage.StorageEngine;
import org.openconcerto.erp.storage.StorageEngines;
import org.openconcerto.openoffice.OOUtils;
import org.jopendocument.link.Component;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.ExceptionHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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

    // return null to keep default value
    public interface StorageDirs {
        public File getDocumentOutputDirectory(SheetXml sheet);

        public File getPDFOutputDirectory(SheetXml sheet);

        public String getStoragePath(SheetXml sheet);
    }

    private static StorageDirs STORAGE_DIRS;

    // allow to redirect all documents
    public static void setStorageDirs(StorageDirs d) {
        STORAGE_DIRS = d;
    }

    public static final String DEFAULT_PROPERTY_NAME = "Default";
    protected SQLElement elt;

    // nom de l'imprimante à utiliser
    protected String printer;

    // id
    protected SQLRow row;

    // Language du document
    protected SQLRow rowLanguage;

    protected static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

    // single threaded and kill its thread after 3 seconds (to allow the program to exit)
    protected static final ExecutorService runnableQueue = new ThreadPoolExecutor(0, 1, 3L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    protected static UncaughtExceptionHandler DEFAULT_HANDLER = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            ExceptionHandler.handle("Erreur de generation", e);
        }
    };

    public final SQLElement getElement() {
        return this.elt;
    }

    /**
     * Show, print and export the document to PDF. This method is asynchronous, but is executed in a
     * single threaded queue shared with createDocument
     * */
    public Future<SheetXml> showPrintAndExportAsynchronous(final boolean showDocument, final boolean printDocument, final boolean exportToPDF) {
        final Callable<SheetXml> c = new Callable<SheetXml>() {
            @Override
            public SheetXml call() throws Exception {
                showPrintAndExport(showDocument, printDocument, exportToPDF);
                return SheetXml.this;
            }
        };
        return runnableQueue.submit(c);

    }

    public void showPrintAndExport(final boolean showDocument, final boolean printDocument, boolean exportToPDF) {
        showPrintAndExport(showDocument, printDocument, exportToPDF, Boolean.getBoolean("org.openconcerto.oo.useODSViewer"), false);
    }

    /**
     * Show, print and export the document to PDF. This method is synchronous
     * */
    public void showPrintAndExport(final boolean showDocument, final boolean printDocument, boolean exportToPDF, boolean useODSViewer, boolean exportPDFSynch) {

        final File generatedFile = getGeneratedFile();
        final File pdfFile = getGeneratedPDFFile();
        if (generatedFile == null || !generatedFile.exists()) {
            ExceptionHandler.handle("Fichier généré manquant: " + generatedFile);
            return;
        }

        try {
            if (!useODSViewer) {
                final Component doc = ComptaPropsConfiguration.getOOConnexion().loadDocument(generatedFile, !showDocument);

                if (printDocument) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("Name", this.printer);
                    doc.printDocument(map);
                }
                if (exportToPDF) {
                    doc.saveToPDF(pdfFile).get();
                }
                doc.close();
            } else {
                final OpenDocument doc = new OpenDocument(generatedFile);

                if (showDocument) {
                    showPreviewDocument();
                }
                if (printDocument) {
                    // Print !
                    DefaultNXDocumentPrinter printer = new DefaultNXDocumentPrinter();
                    printer.print(doc);
                }

                // FIXME Profiler pour utiliser moins de ram --> ex : demande trop de mémoire pour
                // faire
                // un grand livre KD
                if (exportToPDF) {

                    Thread t = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                SheetUtils.convert2PDF(doc, pdfFile);

                            } catch (Throwable e) {
                                ExceptionHandler.handle("Impossible de créer le PDF.", e);
                            }
                            List<StorageEngine> engines = StorageEngines.getInstance().getActiveEngines();
                            for (StorageEngine storageEngine : engines) {
                                if (storageEngine.isConfigured() && storageEngine.allowAutoStorage()) {
                                    try {
                                        storageEngine.connect();
                                        final BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(pdfFile));
                                        final String path = getStoragePath();
                                        storageEngine.store(inStream, path, pdfFile.getName(), true);
                                        inStream.close();
                                        storageEngine.disconnect();
                                    } catch (IOException e) {
                                        ExceptionHandler.handle("Impossible de sauvegarder le PDF");
                                    }
                                    if (storageEngine instanceof CloudStorageEngine) {
                                        try {
                                            storageEngine.connect();
                                            final BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(generatedFile));
                                            final String path = getStoragePath();
                                            storageEngine.store(inStream, path, generatedFile.getName(), true);
                                            inStream.close();
                                            storageEngine.disconnect();
                                        } catch (IOException e) {
                                            ExceptionHandler.handle("Impossible de sauvegarder le fichier généré");
                                        }
                                    }
                                }
                            }

                        }
                    }, "convert and upload to pdf");

                    t.setDaemon(true);
                    t.start();
                    if (exportPDFSynch) {
                        t.join();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
        }
    }

    public abstract String getDefaultTemplateId();

    /**
     * Path of the directory used for storage. Ex: Devis/2010
     * */
    public final String getStoragePath() {
        final String res = STORAGE_DIRS == null ? null : STORAGE_DIRS.getStoragePath(this);
        if (res != null)
            return res;
        else
            return this.getStoragePathP();
    }

    public final File getDocumentOutputDirectory() {
        final File res = STORAGE_DIRS == null ? null : STORAGE_DIRS.getDocumentOutputDirectory(this);
        if (res != null)
            return res;
        else
            return this.getDocumentOutputDirectoryP();
    }

    public final File getPDFOutputDirectory() {
        final File res = STORAGE_DIRS == null ? null : STORAGE_DIRS.getPDFOutputDirectory(this);
        if (res != null)
            return res;
        else
            return this.getPDFOutputDirectoryP();
    }

    protected abstract String getStoragePathP();

    protected abstract File getDocumentOutputDirectoryP();

    protected abstract File getPDFOutputDirectoryP();

    /**
     * Name of the generated document (without extension), do not rely on this name.
     * 
     * Use getGeneratedFile().getName() to get the generated file name.
     * */
    public abstract String getName();

    /**
     * @return the template id for this template (ex: "sales.quote")
     * */
    public String getTemplateId() {
        if (this.row != null && this.row.getTable().getFieldsName().contains("ID_MODELE")) {
            SQLRow rowModele = this.row.getForeignRow("ID_MODELE");
            if (rowModele.isUndefined()) {
                TypeModeleSQLElement typeModele = Configuration.getInstance().getDirectory().getElement(TypeModeleSQLElement.class);
                String modele = typeModele.getTemplateMapping().get(this.row.getTable().getName());
                if (modele == null) {
                    System.err.println("No default modele in table TYPE_MODELE for table " + this.row.getTable().getName());
                    Thread.dumpStack();
                    return getDefaultTemplateId();
                } else {
                    return modele;
                }
            } else {
                return rowModele.getString("NOM");
            }
        }
        return getDefaultTemplateId();
    }

    public abstract Future<SheetXml> createDocumentAsynchronous();

    public void createDocument() throws InterruptedException, ExecutionException {
        createDocumentAsynchronous().get();
    }

    /**
     * get the File that is, or must be generated.
     * 
     * @return a file (not null)
     * */
    public abstract File getGeneratedFile();

    public File getGeneratedPDFFile() {
        return SheetUtils.getFileWithExtension(getGeneratedFile(), ".pdf");
    }

    public SQLRow getRowLanguage() {
        return this.rowLanguage;
    }

    public String getReference() {
        return "";
    }

    /**
     * Creates the document if needed and returns the generated file (OpenDocument)
     * */
    public File getOrCreateDocumentFile() throws Exception {
        File f = getGeneratedFile();
        if (!f.exists()) {
            return createDocumentAsynchronous().get().getGeneratedFile();
        } else {
            return f;
        }
    }

    /**
     * Creates the document if needed and returns the generated file (OpenDocument)
     * 
     * @param createRecent true for recreate the pdf document if older than ods
     * @return
     * @throws Exception
     * 
     * */
    public File getOrCreatePDFDocumentFile(boolean createRecent) throws Exception {
        return getOrCreatePDFDocumentFile(createRecent, Boolean.getBoolean("org.openconcerto.oo.useODSViewer"));
    }

    public File getOrCreatePDFDocumentFile(boolean createRecent, boolean useODSViewer) throws Exception {
        File f = getGeneratedPDFFile();
        if (!f.exists()) {
            getOrCreateDocumentFile();
            showPrintAndExport(false, false, true, useODSViewer, true);
            return f;
        } else {
            File fODS = getOrCreateDocumentFile();
            if (fODS.lastModified() > f.lastModified()) {
                showPrintAndExport(false, false, true, useODSViewer, true);
            }
            return f;
        }
    }

    /**
     * Open the document with the native application
     * 
     * @param synchronous
     * */
    public void openDocument(boolean synchronous) {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                File f;
                try {
                    f = getOrCreateDocumentFile();
                    // ComptaPropsConfiguration.getOOConnexion().loadDocument(f, false);
                    OOUtils.open(f);
                } catch (Exception e) {
                    ExceptionHandler.handle("Impossible d'ouvrir le document.", e);
                }
            }
        };
        if (synchronous) {
            r.run();
        } else {
            Thread thread = new Thread(r, "openDocument: " + getGeneratedFile().getAbsolutePath());
            thread.setDaemon(true);
            thread.start();
        }

    }

    public void showPreviewDocument() throws Exception {
        File f = null;
        f = getOrCreateDocumentFile();
        PreviewFrame.show(f);
    }

    public void fastPrintDocument() {
        FastPrintAskFrame f = new FastPrintAskFrame(this);
        f.display();
    }

    public void fastPrintDocument(short copies) {

        try {
            final File f = getOrCreateDocumentFile();

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

        try {
            final File f = getOrCreateDocumentFile();

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

    public SQLRow getSQLRow() {
        return this.row;
    }

    /**
     * Remplace tous les caracteres non alphanumeriques (seul le _ est autorisé) par un -. Cela
     * permet d'avoir toujours un nom de fichier valide.
     * 
     * @param fileName nom du fichier à créer ex:FACTURE_2007/03/001
     * @return un nom fichier valide ex:FACTURE_2007-03-001
     */
    static String getValidFileName(String fileName) {
        final StringBuffer result = new StringBuffer(fileName.length());
        for (int i = 0; i < fileName.length(); i++) {
            char ch = fileName.charAt(i);

            // Si c'est un caractere alphanumerique
            if (Character.isLetterOrDigit(ch) || (ch == '_') || (ch == ' ')) {
                result.append(ch);
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
