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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.Log;
import org.openconcerto.utils.i18n.Grammar;
import org.openconcerto.utils.i18n.NounClass;
import org.openconcerto.utils.i18n.Phrase;
import org.openconcerto.utils.i18n.VariantKey;
import org.openconcerto.xml.JDOMUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.jcip.annotations.ThreadSafe;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Parses XML to create phrases.
 * 
 * <pre>
 *     &lt;element refid="elementCode">
 *         &lt;name base="elemName" nounClass="masculine">
 *             &lt;variant refids="singular,plural" value="elemNameBothSingularAndPlural" />
 *             &lt;variant idPattern="(singular|plural)" value="elemNameBothSingularAndPlural" />
 *         &lt;/name>
 *     &lt;/element>
 * </pre>
 * 
 * @author Sylvain
 * 
 */
@ThreadSafe
public class SQLElementNamesFromXML extends SQLElementNamesMap.ByCode {

    static public final Pattern SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");

    public SQLElementNamesFromXML(Locale locale) {
        super(locale);
    }

    public final void load(final InputStream ins) throws JDOMException, IOException {
        final Grammar gr = Grammar.getInstance(getLocale());
        final Document doc = new SAXBuilder().build(ins);
        @SuppressWarnings("unchecked")
        final List<Element> elements = doc.getRootElement().getChildren("element");
        for (final Element elem : elements)
            this.load(gr, elem);
    }

    public final Entry<String, Phrase> createPhrase(final Element elem) throws IOException {
        return this.createPhrase(Grammar.getInstance(getLocale()), elem);
    }

    private Entry<String, Phrase> createPhrase(final Grammar gr, final Element elem) throws IOException {
        final String refid = elem.getAttributeValue("refid");
        if (refid == null)
            throw new IOException("No refid attribute");

        final Element nameElem = elem.getChild("name");
        final boolean hasChild = nameElem != null;
        final String nameAttr = elem.getAttributeValue("name");
        if (!hasChild && nameAttr == null) {
            Log.get().warning("No name for code : " + refid);
            return null;
        }
        if (hasChild && nameAttr != null) {
            Log.get().warning("Ignoring attribute : " + nameAttr);
        }

        final String base = hasChild ? nameElem.getAttributeValue("base") : nameAttr;
        if (base == null)
            throw new IOException("No base for the name of " + refid);
        final String nounClassName = hasChild ? nameElem.getAttributeValue("nounClass") : elem.getAttributeValue("nameClass");
        final NounClass nounClass = nounClassName == null ? null : gr.getNounClass(nounClassName);

        final Phrase res = new Phrase(gr, base, nounClass);
        if (!hasChild) {
            // most languages have at most 2 grammatical number
            final String plural = elem.getAttributeValue("namePlural");
            if (plural != null)
                res.putVariant(Grammar.PLURAL, plural);
        } else {
            @SuppressWarnings("unchecked")
            final List<Element> variantElems = nameElem.getChildren("variant");
            for (final Element variantElem : variantElems) {
                final String value = variantElem.getAttributeValue("value");
                if (value == null) {
                    warning(refid, variantElem, "No value");
                    continue;
                }
                final String variantIDs = variantElem.getAttributeValue("refids");
                final String variantPattern = variantElem.getAttributeValue("idPattern");
                if (variantIDs == null && variantPattern == null) {
                    warning(refid, variantElem, "No ID");
                } else if (variantIDs != null) {
                    if (variantPattern != null) {
                        warning(refid, variantElem, "Ignorig pattern " + variantPattern);
                    }
                    for (final String variantID : SPLIT_PATTERN.split(variantIDs)) {
                        final VariantKey variantKey = gr.getVariantKey(variantID);
                        if (variantKey == null) {
                            warning(refid, variantElem, "Ignorig " + variantID);
                        } else {
                            res.putVariant(variantKey, value);
                        }
                    }
                } else {
                    assert variantIDs == null && variantPattern != null;
                    final Pattern p = Pattern.compile(variantPattern);
                    for (final VariantKey vk : gr.getVariantKeys()) {
                        if (p.matcher(vk.getID()).matches()) {
                            res.putVariant(vk, value);
                        }
                    }
                }
            }
        }
        return new SimpleEntry<String, Phrase>(refid, res);
    }

    private void load(final Grammar gr, final Element elem) throws IOException {
        final Entry<String, Phrase> entry = this.createPhrase(gr, elem);
        if (entry != null)
            this.put(entry.getKey(), entry.getValue());
    }

    private void warning(final String refid, final Element variantElem, final String msg) {
        Log.get().warning(msg + " for variant of " + refid + " : " + JDOMUtils.output(variantElem));
    }
}
