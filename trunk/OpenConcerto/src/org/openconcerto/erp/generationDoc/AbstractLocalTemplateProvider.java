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

public abstract class AbstractLocalTemplateProvider implements TemplateProvider {

    @Override
    public InputStream getTemplate(String templateId, String langage, String type) {
        try {
            final File templateFile = getTemplateFile(templateId, langage, type);
            if (templateFile == null || !templateFile.exists()) {
                return null;
            }
            return new FileInputStream(templateFile);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public InputStream getTemplatePrintConfiguration(String templateId, String langage, String type) {
        final File t = getTemplateFile(templateId, langage, type);
        final String name = t.getName();
        if (name.toLowerCase().endsWith(".ods")) {
            final File file = new File(t.getParent(), name.substring(0, name.length() - 4) + ".odsp");
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                System.err.println("No print configuration " + file.getAbsolutePath() + " for template id: " + templateId);
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public InputStream getTemplateConfiguration(String templateId, String langage, String type) {
        final File t = getTemplateFile(templateId, langage, type);
        final String name = t.getName();
        if (name.toLowerCase().endsWith(".ods")) {
            final File file = new File(t.getParent(), name.substring(0, name.length() - 4) + ".xml");
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                System.err.println("No template configuration " + file.getAbsolutePath() + " for template id: " + templateId);
                e.printStackTrace();
            }
        }
        return null;
    }

    public abstract File getTemplateFile(String templateId, String langage, String type);

    @Override
    public abstract String getTemplatePath(String templateId, String langage, String type);

}
