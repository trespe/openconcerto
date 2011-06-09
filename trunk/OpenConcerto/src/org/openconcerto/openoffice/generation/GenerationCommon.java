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
 * Créé le 26 oct. 2004
 */
package org.openconcerto.openoffice.generation;

import org.openconcerto.openoffice.ODSingleXMLDocument;
import org.openconcerto.openoffice.OOXML;
import org.openconcerto.openoffice.XMLVersion;
import org.openconcerto.openoffice.generation.desc.ReportType;
import org.openconcerto.openoffice.generation.desc.part.GeneratorReportPart;
import org.openconcerto.openoffice.generation.generator.ExtractGenerator;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

/**
 * Gather utility methods needed to generate a report (per {@link ReportType}).
 * 
 * @author Sylvain
 * 
 * @param <R> type of generation
 */
public class GenerationCommon<R extends ReportGeneration> {

    private static final NumberFormat FLOAT_FMT = new DecimalFormat("#.###");

    /** Notre génération */
    private final R rg;

    public GenerationCommon(R rg) {
        this.rg = rg;
    }

    public final XMLVersion getOOVersion() {
        // MAYBE more formal, but ATTN costly to unzip
        return this.getRg().getReportType().getTemplate().getName().endsWith(".sxw") ? XMLVersion.OOo : XMLVersion.OD;
    }

    public final OOXML getOOXML() {
        return OOXML.get(getOOVersion());
    }

    /**
     * Encode s as rich text. Ie with embedded [] to indicate styles.
     * 
     * @param s the string to be encoded, e.g. "hi how [b]are[/b] you ?"
     * @return the corresponding OOXML, e.g.
     * 
     *         <pre>
     *     &lt;text:span&gt;hi how &lt;text:span text:style-name=&quot;bold&quot;&gt;are&lt;/text:span&gt; you ?&lt;/text:span&gt;
     * </pre>
     * 
     */
    protected final Element encodeRT(String s) {
        return this.getOOXML().encodeRT(s, this.getStyleMap());
    }

    protected Map<String, String> getStyleMap() {
        final Map<String, String> m = new HashMap<String, String>();
        m.put("b", "Gras");
        m.put("i", "Italique");
        return m;
    }

    /**
     * Encode s to an OO XML Element. Handles both rich text and whitespace.
     * 
     * @param s the string to be encoded.
     * @return the corresponding OOXML.
     */
    public final Element encode(String s) {
        return this.getOOXML().encodeWS(encodeRT(s));
    }

    /**
     * Encode s to an OO XML Element. Handles white spaces but not rich text, and thus is much
     * faster than encode().
     * 
     * @param s the string to be encoded.
     * @return the corresponding OOXML.
     */
    public final Element encodeNoRT(String s) {
        return this.getOOXML().encodeWS(s);
    }

    public final String formatNonZero(Number n) {
        return n.intValue() == 0 ? "" : n.intValue() + "";
    }

    public final String formatNonZeroFloat(Number n) {
        return n.floatValue() == 0 ? "" : FLOAT_FMT.format(n.floatValue());
    }

    protected final R getRg() {
        return this.rg;
    }

    /**
     * The generator needed for the first document of the report where all the styles are defined.
     * 
     * @param f the location of the style template.
     * @return the generator for f, can be <code>null</code> if none needed.
     */
    protected DocumentGenerator<R> getStyleTemplateGenerator(File f) {
        return null;
    }

    /**
     * The generator need for the passed part. This implementation use the file attribute of
     * <code>part</code> to create an {@link ExtractGenerator}.
     * 
     * @param part a part of a report.
     * @return the corresponding generator.
     */
    protected DocumentGenerator<R> createGenerator(GeneratorReportPart part) {
        final File f = this.getRg().getReportType().getParent().resolve(part.getElem().getAttributeValue("file"));
        return new ExtractGenerator<R>(this.getRg(), f);
    }

    /**
     * Called for each creation of document.
     * 
     * @param doc the empty newly created document.
     */
    protected void preProcessDocument(final ODSingleXMLDocument doc) {
        // nothing
    }
}
