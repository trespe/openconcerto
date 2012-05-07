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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public abstract class AbstractLocalTemplateProvider implements TemplateProvider {

    @Override
    public InputStream getTemplate(String templateId, String language, String type) {
        final File file = getFileTemplate(templateId, language, type);
        return (file == null ? null : getInputStream(file));
    }

    private FileInputStream getInputStream(File f) {
        try {
            return new FileInputStream(f);
        } catch (FileNotFoundException e) {
            System.err.println("Error: no file:" + f.getAbsolutePath());
            e.printStackTrace();
        }
        return null;
    }

    public File getFileTemplate(String templateId, String language, String type) {
        File templateFile = getTemplateFromLocalFile(templateId, language, type);
        if (templateFile != null && templateFile.exists()) {
            return templateFile;
        }
        templateFile = getTemplateFromLocalFile(templateId + ".ods", language, type);
        if (templateFile != null && templateFile.exists()) {
            return templateFile;
        }
        templateFile = getTemplateFromLocalFile(templateId + ".odt", language, type);
        if (templateFile != null && templateFile.exists()) {
            return templateFile;
        }
        return null;
    }

    @Override
    public InputStream getTemplatePrintConfiguration(String templateId, String language, String type) {
        final File file = getFileTemplatePrintConfiguration(templateId, language, type);
        return getInputStream(file);
    }

    public File getFileTemplatePrintConfiguration(String templateId, String language, String type) {
        final File file = getTemplateFromLocalFile(templateId + ".odsp", language, type);
        return file;
    }

    @Override
    public InputStream getTemplateConfiguration(String templateId, String language, String type) {
        final File file = getFileTemplateConfiguration(templateId, language, type);
        return getInputStream(file);
    }

    public File getFileTemplateConfiguration(String templateId, String language, String type) {
        final File file = getTemplateFromLocalFile(templateId + ".xml", language, type);
        return file;
    }

    protected abstract File getTemplateFromLocalFile(String templateIdWithExtension, String language, String type);

    @Override
    public abstract String getTemplatePath(String templateId, String language, String type);

    public static String insertBeforeExtenstion(String fileName, String text) {
        final int index = fileName.lastIndexOf('.');
        if (index < 0) {
            throw new IllegalArgumentException("No extension found in fileName '" + fileName + "'");
        }
        if (index == 0) {
            return fileName + text;
        }
        final String name = fileName.substring(0, index);
        final String ext = fileName.substring(index);
        return name + text + ext;
    }

    void ensureDelete(File f) {
        for (int i = 0; i < 2; i++) {
            if (f.delete()) {
                return;
            }
            JOptionPane.showMessageDialog(new JFrame(), "Fichier " + f.getAbsolutePath() + " vérrouillé.\nMerci de fermer tout programme pouvant y accéder (OpenOffice).");
        }
        f.deleteOnExit();

    }
}
