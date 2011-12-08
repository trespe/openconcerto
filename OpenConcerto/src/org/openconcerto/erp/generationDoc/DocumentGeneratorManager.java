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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DocumentGeneratorManager {
    private static DocumentGeneratorManager instance = new DocumentGeneratorManager();
    private Map<String, DocumentGenerator> generators = new HashMap<String, DocumentGenerator>();
    private DocumentGenerator defautGenerator;

    public static DocumentGeneratorManager getInstance() {
        return instance;
    }

    public void add(String templateId, DocumentGenerator generator) {
        this.generators.put(templateId, generator);
    }

    public void remove(DocumentGenerator generator) {
        final Set<String> keys = generators.keySet();
        for (String key : keys) {
            if (generators.get(key).equals(generator)) {
                generators.remove(key);
            }
        }
    }

    public void setDefaultGenerator(DocumentGenerator generator) {
        this.defautGenerator = generator;
    }

    /**
     * Returns the document generator a specific templateId
     * */
    public DocumentGenerator getGenerator(String templateId) {
        DocumentGenerator generator = this.generators.get(templateId);
        if (generator == null) {
            generator = defautGenerator;
        }
        return generator;
    }

    public void dump() {
        System.out.println(this.getClass().getCanonicalName());
        System.out.println("Default generator:" + this.defautGenerator);
        Set<String> ids = generators.keySet();
        for (String templateId : ids) {
            System.out.println("'" + templateId + "' : " + generators.get(templateId));
        }
    }

}
