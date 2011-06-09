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
 
 package org.openconcerto.openoffice.generation.generator;

import org.openconcerto.openoffice.ODSingleXMLDocument;
import org.openconcerto.openoffice.OOHandGeneratedXMLDocument;
import org.openconcerto.openoffice.generation.DocumentGenerator;
import org.openconcerto.openoffice.generation.ReportGeneration;

import java.io.IOException;

import org.jdom.JDOMException;

/**
 * Un générateur de XML OpenOffice manuel. C'est à dire construit seulement avec des chaines Java.
 * 
 * @author ILM Informatique 16 nov. 2004
 * @param <R> type of generation
 */
public abstract class ManualGenerator<R extends ReportGeneration<?>> extends DocumentGenerator<R> {

    /**
     * Crée un nouveau générateur pour cette génération.
     * 
     * @param rg la génération.
     */
    public ManualGenerator(R rg) {
        super(rg);
    }

    public ODSingleXMLDocument generate() throws IOException {
        try {
            return OOHandGeneratedXMLDocument.create(this.getOOXML());
        } catch (JDOMException exn) {
            throw new IOException("getOOXML() produit de l'XML non valide dans " + this, exn);
        }
    }

    protected abstract String getOOXML();

}
