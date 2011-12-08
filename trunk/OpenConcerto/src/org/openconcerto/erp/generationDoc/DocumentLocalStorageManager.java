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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentLocalStorageManager {
    private static DocumentLocalStorageManager instance = new DocumentLocalStorageManager();
    private Map<String, File> dirs = new HashMap<String, File>();
    private Map<String, File> dirsPDF = new HashMap<String, File>();
    private File documentDefaultDirectory;
    private File PDFDefaultDirectory;

    public static DocumentLocalStorageManager getInstance() {
        return instance;
    }

    /**
     * Returns the directory to store the document created from a template
     * 
     * @param templateId : the id of the template used to create the document
     * */
    public File getDocumentOutputDirectory(String templateId) {
        if (templateId == null) {
            throw new IllegalArgumentException("null template id");
        }
        final File f = dirs.get(templateId);
        if (f != null) {
            return f;
        }
        return documentDefaultDirectory;

    }

    /**
     * Returns the directory to store the PDF document created from a template
     * 
     * @param templateId : the id of the template used to create the document
     * */
    public File getPDFOutputDirectory(String templateId) {
        if (templateId == null) {
            throw new IllegalArgumentException("null template id");
        }
        final File f = dirsPDF.get(templateId);
        if (f != null) {
            return f;
        }
        return PDFDefaultDirectory;
    }

    public void addDocumentDirectory(String templateId, File directory) {
        if (templateId == null) {
            throw new IllegalArgumentException("null template id");
        }
        this.dirs.put(templateId, directory);
        TemplateManager.getInstance().register(templateId);
    }

    public void removeDocumentDirectory(String templateId, File directory) {
        if (templateId == null) {
            throw new IllegalArgumentException("null template id");
        }
        this.dirs.remove(templateId);
    }

    public void addPDFDirectory(String templateId, File directory) {
        if (templateId == null) {
            throw new IllegalArgumentException("null template id");
        }
        this.dirsPDF.put(templateId, directory);
        TemplateManager.getInstance().register(templateId);
    }

    public void removePDFDirectory(String templateId, File directory) {
        if (templateId == null) {
            throw new IllegalArgumentException("null template id");
        }
        this.dirsPDF.remove(templateId);
    }

    public void setDocumentDefaultDirectory(File directory) {
        this.documentDefaultDirectory = directory;
    }

    public void setPDFDefaultDirectory(File directory) {
        this.PDFDefaultDirectory = directory;
    }

    public List<File> getAllPDFDirectories() {
        List<File> list = new ArrayList<File>();
        for (String id : this.dirsPDF.keySet()) {
            list.add(this.dirsPDF.get(id));
        }
        return list;
    }

    public List<File> getAllDocumentDirectories() {
        List<File> list = new ArrayList<File>();
        for (String id : this.dirs.keySet()) {
            list.add(this.dirs.get(id));
        }
        return list;
    }

    public void dump() {
        System.out.println(this.getClass().getCanonicalName());
        System.out.println("Default document directory: " + getAndTest(this.documentDefaultDirectory));
        System.out.println("Default PFD directory     : " + getAndTest(this.PDFDefaultDirectory));
        System.out.println("Document directories:");
        for (String key : this.dirs.keySet()) {
            System.out.println(rightAlign("'" + key + "'") + " : " + getAndTest(this.dirs.get(key)));
        }
        System.out.println("PDF directories:");
        for (String key : this.dirsPDF.keySet()) {
            System.out.println(rightAlign("'" + key + "'") + " : " + getAndTest(this.dirsPDF.get(key)));
        }
    }

    private String rightAlign(String s) {
        String r = s;
        int n = 20 - s.length();
        for (int i = 0; i < n; i++) {
            r = ' ' + r;
        }
        return r;
    }

    private String getAndTest(File dir) {
        if (dir == null) {
            return "null !!!!!!";
        }
        String r = "'" + dir.getAbsolutePath() + "'";
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                r += " is not a directory!!";
            } else if (!dir.canWrite()) {
                r += " is write protected!!";
            }
        } else {
            r += " does not exist !!";
        }
        return r;
    }

}
