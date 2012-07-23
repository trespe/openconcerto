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

import org.openconcerto.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class DefaultLocalTemplateProvider extends AbstractLocalTemplateProvider {
    private static final String[] EXTS = new String[] { ".ods", ".odsp", ".xml" };
    private static final String LOCAL = "local_";
    private File baseDirectory;

    public DefaultLocalTemplateProvider() {
        baseDirectory = new File("Configuration/Template/Default");
    }

    public void setBaseDirectory(File dir) {
        this.baseDirectory = dir;
    }

    @Override
    public File getTemplateFromLocalFile(String templateIdWithExtension, String language, String type) {
        File file = getLocalFile(templateIdWithExtension, language, type);
        if (file != null && !file.exists()) {
            file = getFile(templateIdWithExtension, language, type);
        }
        return file;
    }

    private File getLocalFile(String templateIdWithExtension, String language, String type) {
        // Looking first for baseDirectory/language/local_templateId + type
        String localPath = templateIdWithExtension;

        if (type != null) {
            localPath = insertBeforeExtenstion(localPath, type);
            if (localPath == null) {
                return null;
            }
        }

        if (language != null && language.trim().length() > 0) {
            localPath = language + File.separatorChar + LOCAL + localPath;
        } else {
            localPath = LOCAL + localPath;
        }
        final File file = new File(baseDirectory, localPath);
        return file;
    }

    private File getFile(String templateIdWithExtension, String language, String type) {
        // baseDirectory/language/templateId + type
        String path = templateIdWithExtension;

        if (type != null) {
            path = insertBeforeExtenstion(path, type);
            if (path == null) {
                return null;
            }
        }

        if (language != null && language.trim().length() > 0) {
            path = language + File.separatorChar + path;
        }

        File file = new File(baseDirectory, path);
        if (!file.exists()) {
            file = new File("Configuration/Template/Default", path);
        }
        return file;
    }

    @Override
    public String getTemplatePath(String templateId, String language, String type) {
        String path = "Configuration/Template/Default";
        if (type != null) {
            path = insertBeforeExtenstion(path, type);
            if (path == null) {
                return null;
            }
        }
        if (language != null) {
            path = language + '/' + path;
        }
        return path;
    }

    @Override
    public boolean isSynced(String templateId, String language, String type) {
        return !getLocalFile(templateId + ".ods", language, type).exists();
    }

    @Override
    public void unSync(String templateId, String language, String type) {
        for (int i = 0; i < EXTS.length; i++) {
            final String ext = EXTS[i];
            final File from = getFile(templateId + ext, language, type);
            final File to = getLocalFile(templateId + ext, language, type);
            try {
                if (from.exists() && !to.exists()) {
                    FileUtils.copyFile(from, to);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Copie impossible", e);
            }
        }
    }

    @Override
    public void sync(String templateId, String language, String type) {
        for (int i = 0; i < EXTS.length; i++) {
            final String ext = EXTS[i];
            final File from = getLocalFile(templateId + ext, language, type);
            final File to = getFile(templateId + ext, language, type);
            try {
                if (from.exists()) {
                    FileUtils.copyFile(from, to);
                    ensureDelete(from);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Synchronisation impossible", e);
            }
        }
    }

    @Override
    public void restore(String templateId, String language, String type) {
        for (int i = 0; i < EXTS.length; i++) {
            final String ext = EXTS[i];
            final File local = getLocalFile(templateId + ext, language, type);
            if (local.exists()) {
                local.delete();
                ensureDelete(local);
            }
        }
    }
}
