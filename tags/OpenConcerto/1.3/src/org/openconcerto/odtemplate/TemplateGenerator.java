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
 * Créé le 15 nov. 2004
 */
package org.openconcerto.odtemplate;

import org.openconcerto.odtemplate.engine.OGNLDataModel;
import org.openconcerto.openoffice.XMLVersion;
import org.openconcerto.openoffice.ODPackage;
import org.openconcerto.openoffice.ODSingleXMLDocument;
import org.openconcerto.openoffice.generation.DocumentGenerator;
import org.openconcerto.openoffice.generation.ReportGeneration;
import org.openconcerto.utils.ExceptionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Un générateur se servant de d'une template. In this implementation only
 * {@link ReportGeneration#getCommonData()} is provided to the template, subclass {@link #getData()}
 * to add other objects.
 * 
 * @author Sylvain CUAZ
 * @param <R> type of generation
 */
public class TemplateGenerator<R extends ReportGeneration<?>> extends DocumentGenerator<R> {

    private final File file;

    public TemplateGenerator(final R rg, final File f) {
        super(rg);
        this.file = f;
    }

    public final ODSingleXMLDocument generate() throws IOException, InterruptedException {
        return this.substitute(this.getAllData());
    }

    protected final Map<String, Object> getAllData() throws IOException, InterruptedException {
        // ce qui y est toujours
        final Map<String, Object> res = new HashMap<String, Object>(this.getRg().getCommonData());
        // plus les données de ce generateur en particulier
        res.putAll(this.getData());
        return res;
    }

    protected Map<String, Object> getData() throws InterruptedException {
        return new HashMap<String, Object>();
    }

    private final ODSingleXMLDocument substitute(Map data) throws FileNotFoundException, IOException {
        final XMLVersion reportVersion = this.getRg().getCommon().getOOVersion();
        final ODPackage pkg = new ODPackage(this.file);
        if (pkg.getVersion() != reportVersion)
            throw new IllegalArgumentException("version mismatch");
        try {
            this.transform(pkg.toSingle());
            // MAYBE fireStatusChange with the number of tag done out of the total
            return new Template(pkg).createDocument(new OGNLDataModel(data));
        } catch (Exception exn) {
            throw ExceptionUtils.createExn(IOException.class, "generation error in " + this, exn);
        }
    }

    protected void transform(ODSingleXMLDocument single) throws Exception {
    }

    public String toString() {
        return this.getClass() + " with file " + this.file;
    }

}
