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
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.sync.FileProperty;
import org.openconcerto.utils.sync.SyncClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DefaultCloudTemplateProvider extends AbstractLocalTemplateProvider {
    private int idSociete;
    private static final String[] EXTS = new String[] { ".ods", ".odsp", ".xml" };

    // Documents/OpenConcerto/OnCloud/Template/idSosiete/fr/templateIdType.(ods|odsp|xml)
    public DefaultCloudTemplateProvider(int idSociete) {
        ComptaPropsConfiguration config = ComptaPropsConfiguration.getInstanceCompta();
        this.idSociete = idSociete;
        if (!getCloudDir().exists()) {
            getCloudDir().mkdirs();
        }
        if (!getLocalDir().exists()) {
            getLocalDir().mkdirs();
        }
        final SyncClient client = createSyncClient(config);
        final String remotePath = "Template/" + idSociete;
        boolean remoteTemplateDirExists = false;
        try {
            System.out.println("DefaultCloudTemplateProvider: update template:" + getCloudDir().getCanonicalPath() + " from " + remotePath);
            ArrayList<FileProperty> list = client.getList(remotePath, config.getToken());
            System.out.println("DefaultCloudTemplateProvider: " + list.size() + " remote templates found in path: " + remotePath);
            remoteTemplateDirExists = true;
        } catch (Exception e) {
            // Ok
        }
        if (!remoteTemplateDirExists) {
            System.err.println("DefaultCloudTemplateProvider: No remote templates in path: " + remotePath);
            File defaultTemplateDir = new File("Configuration/Template/Default");
            try {
                if (getCloudDir().list().length <= 0) {
                    FileUtils.copyDirectory(defaultTemplateDir, getCloudDir());
                    FileUtils.copyDirectory(defaultTemplateDir, getLocalDir());
                }
            } catch (IOException e) {
                ExceptionHandler.handle("Impossible d'initialiser les modèles", e);
            }
        } else {
            try {
                client.retrieveDirectory(getCloudDir(), remotePath, config.getToken());
            } catch (Exception e) {
                // Set unsync'ed local templates
                ExceptionHandler.handle("Impossible de synchroniser les modèles", e);
            }
        }
    }

    private SyncClient createSyncClient(ComptaPropsConfiguration config) {
        final SyncClient client = new SyncClient("https://" + config.getStorageServer());
        client.setVerifyHost(false);
        return client;
    }

    @Override
    public boolean isSynced(String templateId, String language, String type) {
        return !getLocalFile(templateId + ".ods", language, type).exists();
    }

    @Override
    public File getTemplateFromLocalFile(String templateIdWithExtension, String language, String type) {
        File f = getLocalFile(templateIdWithExtension, language, type);
        if (!f.exists()) {
            f = getCloudFile(templateIdWithExtension, language, type);
        }
        return f;
    }

    private File getLocalFile(String templateIdWithExtension, String language, String type) {
        String path = templateIdWithExtension;

        if (type != null) {
            path = insertBeforeExtenstion(path, type);
        }
        if (language != null && language.trim().length() > 0) {
            path = language + "/" + path;
        }

        File dir = getLocalDir();
        File out = new File(dir, path);

        return out;
    }

    private File getCloudFile(String templateIdWithExtension, String language, String type) {
        String path = templateIdWithExtension;

        if (type != null) {
            path = insertBeforeExtenstion(path, type);
        }
        if (language != null && language.trim().length() > 0) {
            path = language + "/" + path;
        }

        File dir = getCloudDir();
        File out = new File(dir, path);

        return out;
    }

    private File getCloudDir() {
        return new File(ComptaPropsConfiguration.getInstance().getWD(), "OnCloud/Template/" + idSociete);
    }

    private File getLocalDir() {
        return new File(ComptaPropsConfiguration.getInstance().getWD(), "OnCloud/LocalTemplate/" + idSociete);
    }

    @Override
    public String getTemplatePath(String templateId, String language, String type) {
        String path = templateId;

        if (type != null) {
            path = insertBeforeExtenstion(path, type);
        }
        if (language != null && language.trim().length() > 0) {
            path = language + "/" + path;
        }
        path = idSociete + "/" + path;
        return path;
    }

    @Override
    public void unSync(String templateId, String language, String type) {
        for (int i = 0; i < EXTS.length; i++) {
            final String ext = EXTS[i];
            final File from = getCloudFile(templateId + ext, language, type);
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
            final File to = getCloudFile(templateId + ext, language, type);
            try {
                if (from.exists()) {
                    ComptaPropsConfiguration config = ComptaPropsConfiguration.getInstanceCompta();
                    SyncClient c = createSyncClient(config);
                    String remotePath = "Template/";
                    if (language != null && language.trim().length() > 0) {
                        remotePath += language + "/";
                    }
                    remotePath += idSociete;
                    System.out.println("Sending on cloud:" + from.getCanonicalPath() + " to " + remotePath + " " + from.getName());
                    c.sendFile(from, remotePath, from.getName(), config.getToken());

                    FileUtils.copyFile(from, to);
                    ensureDelete(from);
                }
            } catch (Exception e) {
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
                ensureDelete(local);
            }
        }
    }

}
