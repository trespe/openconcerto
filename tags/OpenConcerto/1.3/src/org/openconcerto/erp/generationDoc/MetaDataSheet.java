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
 
 /*
 * Créé le 17 juin 2013
 */
package org.openconcerto.erp.generationDoc;

import org.openconcerto.openoffice.ODMeta;
import org.openconcerto.openoffice.ODPackage;

import java.util.List;

public class MetaDataSheet {

    private String creator, description, generator, language, subject, title;

    private List<String> keywords;

    public MetaDataSheet() {
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getCreator() {
        return this.creator;
    }

    public String getDescription() {
        return this.description;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getGenerator() {
        return this.generator;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getTitle() {
        return this.title;
    }

    public List<String> getKeywords() {
        return this.keywords;
    }

    public void applyTo(ODMeta meta) {
        if (this.keywords != null && this.keywords.size() > 0) {
            meta.setKeywords(keywords);
        }
        if (this.creator != null) {
            meta.setCreator(creator);
        }

        if (this.description != null) {
            meta.setDescription(description);
        }

        if (this.language != null) {
            meta.setLanguage(language);
        }
        if (this.generator != null) {
            meta.setGenerator(generator);
        }
        if (this.subject != null) {
            meta.setSubject(this.subject);
        }
        if (this.title != null) {
            meta.setTitle(this.title);
        }
    }

}
