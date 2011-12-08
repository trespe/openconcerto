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

import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jopendocument.model.OpenDocument;
import org.jopendocument.renderer.ODTRenderer;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

public class SheetUtils {

    static SheetUtils instance;

    public static SheetUtils getInstance() {
        if (instance == null) {
            instance = new SheetUtils();
        }
        return instance;
    }

    public File convertToOldFile(String fileName, File pathDest, File fDest) {
        return convertToOldFile(fileName, pathDest, fDest, ".ods");
    }

    /**
     * Déplace le fichier, si il existe, dans le répertoire.
     * 
     * @param fileName nom du fichier sans extension
     * @param pathDest
     * @param fDest
     * @return
     */
    public File convertToOldFile(String fileName, File pathDest, File fDest, String extension) {
        if (fDest.exists()) {
            int i = 0;
            String destName = fileName;
            File pathOld = new File(pathDest, "Historique");
            pathOld.mkdirs();
            while (fDest.exists()) {
                destName = fileName + "_" + i;
                fDest = new File(pathOld, destName + extension);
                i++;
            }
            File fTmp = new File(pathDest, fileName + extension);

            if (!fTmp.renameTo(fDest)) {
                final File finalFile = fDest;
                System.err.println("Unable to rename:" + fTmp.getAbsolutePath());
                System.err.println("To:" + fDest.getAbsolutePath());
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {

                            JOptionPane.showMessageDialog(null, "Le fichier " + finalFile.getCanonicalPath()
                                    + " n'a pu être créé. \n Impossible de déplacer le fichier existant dans l'historique.\n Vérifier que le document n'est pas déjà ouvert.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                return fTmp;
            }
            fDest = new File(pathDest, fileName + extension);
        }
        return fDest;
    }

    public List<File> getHistorique(final String fileName, File pathDest) {
        File pathOld = new File(pathDest, "Historique");
        File[] files = pathOld.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(fileName);

            }
        });
        List<File> result = new ArrayList<File>();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                result.add(files[i]);
            }
        }
        Collections.sort(result, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                // TODO Auto-generated method stub
                return o1.getName().compareTo(o2.getName());
            }
        });
        return result;
    }

    public static void main(String[] args) {
        final OpenDocument doc = new OpenDocument(new File("E:/Facture_FACT1108-5785.ods"));
        try {
            SheetUtils.getInstance().convert2PDF(doc, new File("E:/test"), "test");
        } catch (Exception exn) {
            // TODO Bloc catch auto-généré
            exn.printStackTrace();
        }
    }

    public void convert2PDF(final OpenDocument doc, final File f, final String fileName) throws Exception {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO Raccord de méthode auto-généré

                // Open the PDF document
                Document document = new Document(PageSize.A4, 50, 50, 50, 50);
                File outFile = new File(f.getParent(), fileName + ".pdf");
                FileOutputStream fileOutputStream;

                try {

                    fileOutputStream = new FileOutputStream(outFile);

                    // Create the writer
                    PdfWriter writer = PdfWriter.getInstance(document, fileOutputStream);
                    writer.setPdfVersion(PdfWriter.VERSION_1_6);
                    writer.setFullCompression();
                    // writer.setPageEmpty(true);

                    document.open();
                    // System.out.println(writer.getPageNumber());
                    // // Create a template and a Graphics2D object
                    // com.itextpdf.text.Rectangle pageSize = document.getPageSize();
                    // int w = (int) (pageSize.getWidth() * 0.9);
                    // int h = (int) (pageSize.getHeight() * 0.95);

                    PdfContentByte cb = writer.getDirectContent();

                    // Configure the renderer
                    ODTRenderer renderer = new ODTRenderer(doc);
                    renderer.setIgnoreMargins(false);
                    renderer.setPaintMaxResolution(true);

                    // Scale the renderer to fit width
                    renderer.setResizeFactor(renderer.getPrintWidth() / document.getPageSize().getWidth());

                    // Print pages
                    for (int i = 0; i < renderer.getPrintedPagesNumber(); i++) {

                        Graphics2D g2 = cb.createGraphics(PageSize.A4.getWidth(), PageSize.A4.getHeight());

                        // If you want to prevent copy/paste, you can use
                        // g2 = tp.createGraphicsShapes(w, h, true, 0.9f);

                        // Render
                        renderer.setCurrentPage(i);
                        renderer.paintComponent(g2);
                        g2.dispose();

                        // Add our spreadsheet in the middle of the page
                        if (i < renderer.getPrintedPagesNumber() - 1)
                            document.newPage();

                    }
                    // Close the PDF document
                    document.close();
                    // writer.close();
                    fileOutputStream.close();

                } catch (Exception exn) {
                    // TODO Bloc catch auto-généré
                    exn.printStackTrace();
                }
            }
        });
    }
}
