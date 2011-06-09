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

import static org.openconcerto.task.config.ComptaBasePropsConfiguration.getStreamStatic;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

public abstract class PdfGenerator {
    private int marginX, marginY;
    private int offsetX, offsetY;
    private int templateOffsetX, templateOffsetY;
    private PdfContentByte cb;
    private Document document;
    private BaseFont bf, bfb;
    private int width;
    private Map map;
    private String fileNameIn, fileNameOut;
    private File directoryOut;

    public static void main(String[] args) {
        new PdfGenerator_2033B().generateFrom(null);
        HashMap h = new HashMap();
        // h.put("NOM","Front Software");
        // new PdfGenerator_2033A().generateFrom(h);
    }

    PdfGenerator(String fileNameIn, String fileNameOut, String directoryOut) {

        this.fileNameIn = "/Configuration/Template/PDF/" + fileNameIn;
        this.fileNameOut = fileNameOut;

        System.err.println("First folder " + directoryOut);
        this.directoryOut = new File(directoryOut, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
    }

    public void open(File f) {
        if (Desktop.isDesktopSupported()) {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.OPEN)) {

                try {
                    d.open(f.getCanonicalFile());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(null, "Cette action n'est pas supporté par votre système d'exploitation.");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Votre système d'exploitation n'est pas supporté.");
        }
    }

    public void generateFrom(Map m) {
        try {
            if (m == null) {
                System.out.println("Filling with defaults");
            }
            this.map = m;

            init();

            this.cb.beginText();
            generate();
            this.cb.endText();

            // step 5: we close the document
            // FIXME ASK IF CLOSE WRITER
            this.document.close();
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            ExceptionHandler.handle("Impossible de générer le fichier. \n" + e, e);
        }
    }

    private void init() throws FileNotFoundException {

        // we create a reader for a certain document
        PdfReader reader = null;
        PdfWriter writer = null;
        try {
            reader = new PdfReader(getStreamStatic(this.fileNameIn));

            // we retrieve the total number of pages
            int n = reader.getNumberOfPages();
            // we retrieve the size of the first page
            Rectangle psize = reader.getPageSize(1);

            psize.setRight(psize.getRight() - this.templateOffsetX);
            psize.setTop(psize.getTop() - this.templateOffsetY);

            this.width = (int) psize.getWidth();
            float height = psize.getHeight();

            // step 1: creation of a document-object
            int MARGIN = 32;
            this.document = new Document(psize, MARGIN, MARGIN, MARGIN, MARGIN);
            // step 2: we create a writer that listens to the document
            if (!this.directoryOut.exists()) {
                this.directoryOut.mkdirs();
            }
            System.err.println("Directory out " + this.directoryOut.getAbsolutePath());
            File f = new File(this.directoryOut, this.fileNameOut);
            if (f.exists()) {
                f.renameTo(new File(this.directoryOut, "Old" + this.fileNameOut));
                f = new File(this.directoryOut, this.fileNameOut);
            }

            System.err.println("Creation du fichier " + f.getAbsolutePath());

            writer = PdfWriter.getInstance(this.document, new FileOutputStream(f));

            this.document.open();
            // step 4: we add content
            this.cb = writer.getDirectContent();

            System.out.println("There are " + n + " pages in the document.");

            this.document.newPage();

            PdfImportedPage page1 = writer.getImportedPage(reader, 1);

            this.cb.addTemplate(page1, -this.templateOffsetX, -this.templateOffsetY);

            this.bf = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.CP1252, BaseFont.EMBEDDED);
            this.bfb = BaseFont.createFont(BaseFont.TIMES_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);

        } catch (FileNotFoundException fE) {
            throw fE;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    abstract public void generate();

    protected void setMargin(int i, int j) {
        this.marginX = i;
        this.marginY = j;

    }

    protected void setOffset(int i, int j) {
        this.offsetX = i;
        this.offsetY = j;

    }

    protected void setTemplateOffset(int i, int j) {
        this.templateOffsetX = i;
        this.templateOffsetY = j;

    }

    protected void addSplittedText(String code, String string, int fromx, int y, double deltax) {
        float x = fromx - this.offsetX - this.templateOffsetX;
        y = y - this.offsetY - this.templateOffsetY;
        boolean error = false;
        String s = string;
        if (this.map != null) {
            s = (String) this.map.get(code);

        }
        if (s == null) {
            s = code;
            error = true;
            this.cb.setColorFill(Color.RED);
        }

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String sub = String.valueOf(c);

            this.cb.showTextAligned(PdfContentByte.ALIGN_LEFT, sub, x, y, 0);
            x += deltax;
        }

        if (error) {

            this.cb.setColorStroke(Color.BLACK);
        }

    }

    protected void setFontRoman(int i) {
        this.cb.setFontAndSize(this.bf, i);

    }

    protected void setFontBold(int i) {
        this.cb.setFontAndSize(this.bfb, i);
    }

    protected void addText(String code, String string, int i, int j) {
        addText(code, string, i, j, 0);
    }

    protected void addText(String code, String string, int i, int j, int k) {
        addText(PdfContentByte.ALIGN_LEFT, code, string, i, j, k);

    }

    protected void addTextRight(String code, String string, int i, int j) {
        // addText(PdfContentByte.ALIGN_RIGHT, code, string, i, j, 0);

        int a = PdfContentByte.ALIGN_RIGHT;
        int k = 0;

        if (this.map == null)
            this.cb.showTextAligned(a, string, i, j, k);
        else {
            boolean error = false;
            String s = (String) this.map.get(code);
            if (s == null) {
                System.out.println("Impossibe de trouver: " + code + " Set color red");
                s = code;
                error = true;

                // cb.setColorFill(Color.RED);
            } else {
                if (s.equalsIgnoreCase("-0.0")) {
                    s = "0.0";
                }
                s = insertCurrencySpaces(s);
            }
            System.out.println("print " + s);
            this.cb.showTextAligned(a, s, i, j, k);
            if (error) {
                System.out.println(" Set color black");
                this.cb.setColorStroke(Color.BLACK);
            }
        }

    }

    private final void addText(int a, String code, String string, int i, int j, int k) {

        if (this.map == null)
            this.cb.showTextAligned(a, string, i, j, k);
        else {
            boolean error = false;
            String s = (String) this.map.get(code);
            if (s == null) {
                System.out.println("Impossibe de trouver: " + code + " Set color red");
                s = code;
                error = true;

                // cb.setColorFill(Color.RED);
            }
            System.out.println("print " + s);
            this.cb.showTextAligned(a, s, i, j, k);
            if (error) {
                System.out.println(" Set color black");
                this.cb.setColorStroke(Color.BLACK);
            }
        }
    }

    protected int getWidth() {
        return this.width;
    }

    protected static String insertCurrencySpaces(String string) {
        StringBuffer s = new StringBuffer();
        for (int i = string.length() - 1; i >= 0; i--) {

            s.insert(0, string.charAt(i));
            if ((i - string.length()) % 3 == 0) {
                s.insert(0, " ");
            }

        }

        return s.toString().trim();
    }

}
