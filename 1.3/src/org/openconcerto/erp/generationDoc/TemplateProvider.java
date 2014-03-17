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

import java.io.InputStream;

public interface TemplateProvider {
    /**
     * Path: ex ILM/Devis
     * */
    public String getTemplatePath(String templateId, String language, String type);

    /**
     * Returns the content of template file (ex: the ODS file)
     * */
    public InputStream getTemplate(String templateId, String language, String type);

    /**
     * Returns the content of template configuration file (ex: the odsp file)
     * */
    public InputStream getTemplateConfiguration(String templateId, String language, String type);

    /**
     * Returns the content of template print configuration file (ex: the odsp file)
     * */
    public InputStream getTemplatePrintConfiguration(String templateId, String language, String type);

    /**
     * Returns true if the template is synchronized with other computers
     */
    public boolean isSynced(String templateId, String language, String type);

    public void unSync(String templateId, String language, String type);

    public void sync(String templateId, String language, String type);

    public void restore(String templateId, String language, String type);
}
