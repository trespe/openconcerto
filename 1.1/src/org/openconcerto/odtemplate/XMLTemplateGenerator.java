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
 
 package org.openconcerto.odtemplate;

import org.openconcerto.openoffice.generation.ReportGeneration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

/**
 * Un générateur utilisant un élément XML comme donnée.
 * 
 * @author Sylvain CUAZ 15 nov. 2004
 * @param <R> type of generation
 */
public abstract class XMLTemplateGenerator<R extends ReportGeneration<?>> extends TemplateGenerator<R> {

    public XMLTemplateGenerator(final R rg, final File f) {
        super(rg, f);
    }

    protected final Map<String, Object> getData() throws InterruptedException {
        Element data = this.getDBXML();
        Map<String, Object> m = new HashMap<String, Object>();
        m.put(data.getName(), data);
        return m;
    }

    public abstract Element getDBXML() throws InterruptedException;

}
