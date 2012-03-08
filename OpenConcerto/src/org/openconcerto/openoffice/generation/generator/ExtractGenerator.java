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
 * 
 */
package org.openconcerto.openoffice.generation.generator;

import org.openconcerto.openoffice.ODSingleXMLDocument;
import org.openconcerto.openoffice.generation.DocumentGenerator;
import org.openconcerto.openoffice.generation.ReportGeneration;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;

/**
 * Un générateur qui extrait l'XML d'un fichier.
 * 
 * @author Sylvain CUAZ
 * @param <R> type of generation
 */
public final class ExtractGenerator<R extends ReportGeneration<?>> extends DocumentGenerator<R> {

    private File fname;

    public ExtractGenerator(final R rg, final File fname) {
        super(rg);
        this.fname = fname;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ideation.rapport.OOTextDocumentGenerator#generate()
     */
    public ODSingleXMLDocument generate() throws IOException {
        this.fireStatusChange(0);
        try {
            this.fireStatusChange(20);
            ODSingleXMLDocument xml = ODSingleXMLDocument.createFromPackage(this.fname);
            this.fireStatusChange(100);
            return xml;
        } catch (JDOMException exn) {
            throw new IOException("non valid XML" + this, exn);
        }
    }

    public String toString() {
        return this.getClass() + " with file " + this.fname;
    }

}
