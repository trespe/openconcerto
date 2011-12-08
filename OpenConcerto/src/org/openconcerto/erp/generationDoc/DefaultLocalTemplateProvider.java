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

public class DefaultLocalTemplateProvider extends AbstractLocalTemplateProvider {

    private File baseDirectory;

    public DefaultLocalTemplateProvider() {
        baseDirectory = new File("Configuration/Template/Default");
    }

    public void setBaseDirectory(File dir) {
        this.baseDirectory = dir;
    }

    @Override
    public File getTemplateFile(String templateId, String langage, String type) {
        File file = getFile(templateId, langage, type);
        if (!file.exists()) {
            file = getFile(templateId + ".ods", langage, type);
        }
        return file;
    }

    private File getFile(String templateId, String langage, String type) {
        String path = templateId;
        if (type != null) {
            path += type;
        }
        if (langage != null) {
            path = langage + File.separatorChar + path;
        }
        File file = new File(baseDirectory, path);
        if (!file.exists()) {
            file = new File("Configuration/Template/Default", path);
        }
        return file;
    }

    @Override
    public String getTemplatePath(String templateId, String langage, String type) {
        String path = "Configuration/Template/Default";
        if (type != null) {
            path += type;
        }
        if (langage != null) {
            path = langage + '/' + path;
        }
        return path;
    }

}
