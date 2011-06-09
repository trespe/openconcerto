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
 * Créé le 28 oct. 2004
 */
package org.openconcerto.openoffice;

import org.openconcerto.xml.JDOMUtils;
import org.openconcerto.xml.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.map.LazyMap;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Parent;
import org.jdom.Text;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;

/**
 * Various bits of OpenDocument XML.
 * 
 * @author Sylvain CUAZ
 * @see #get(XMLVersion)
 */
public class OOXML {

    private static final Map instances = LazyMap.decorate(new HashMap(), new Transformer() {
        public Object transform(Object input) {
            return new OOXML((XMLVersion) input);
        }
    });

    /**
     * Returns the instance that match the requested version.
     * 
     * @param version the version.
     * @return the corresponding instance.
     */
    public static OOXML get(XMLVersion version) {
        return (OOXML) instances.get(version);
    }

    static public final String getLineBreakS() {
        return "<text:line-break/>";
    }

    static private final String rt2oo(String content, String tagName, String styleName) {
        return content.replaceAll("\\[" + tagName + "\\]", "<text:span text:style-name=\"" + styleName + "\">").replaceAll("\\[/" + tagName + "\\]", "</text:span>");
    }

    /**
     * Encode spaces for OpenOffice 1, and escape characters for XML.
     * 
     * @param s a string to encode, eg "hi\n 3<4".
     * @return the string encoded in XML, eg "hi<text:line-break/><text:s text:c="2"/>3&lt;4".
     * @deprecated see {@link #encodeWS(String)}
     */
    static public final String encodeOOWS(final String s) {
        String tmp = JDOMUtils.OUTPUTTER.escapeElementEntities(s).replaceAll("\n", getLineBreakS()).replaceAll("\t", OOXML.get(XMLVersion.OOo).getTabS());

        String res = "";
        // les séries de plus d'un espace consécutifs
        final Pattern p = Pattern.compile("  +");
        final Matcher m = p.matcher(tmp);
        int lastEnd = 0;
        while (m.find()) {
            // c == le nombre d'espaces
            res += tmp.substring(lastEnd, m.start()) + "<text:s text:c=\"" + (m.group().length()) + "\"/>";
            lastEnd = m.end();
        }
        res += tmp.substring(lastEnd);

        return res;
    }

    // *** instances

    private final XMLVersion version;
    private Schema schema = null;

    private OOXML(XMLVersion version) {
        this.version = version;
    }

    public final XMLVersion getVersion() {
        return this.version;
    }

    private Schema getSchema() throws SAXException {
        if (this.schema == null) {
            this.schema = SchemaFactory.newInstance(XMLConstants.RELAXNG_NS_URI).newSchema(getClass().getResource("oofficeDTDs/OpenDocument-strict-schema-v1.1.rng"));
        }
        assert this.schema != null;
        return this.schema;
    }

    /**
     * Verify that the passed document is a valid OpenOffice.org 1 or ODF document.
     * 
     * @param doc the xml to test.
     * @return a validator on <code>doc</code>.
     */
    public Validator getValidator(Document doc) {
        if (this.getVersion() == XMLVersion.OD) {
            final Schema schema;
            try {
                schema = this.getSchema();
            } catch (SAXException e) {
                throw new IllegalStateException("relaxNG schemas pb", e);
            }
            return new Validator.JAXPValidator(doc, schema);
        } else {
            // DTDs are stubborn, xmlns have to be exactly where they want
            // in this case the root element
            for (final Namespace n : getVersion().getALL())
                doc.getRootElement().addNamespaceDeclaration(n);
            return new Validator.DTDValidator(doc, OOUtils.getBuilderLoadDTD());
        }
    }

    /**
     * Return the names of font face declarations.
     * 
     * @return at index 0 the name of the container element, at 1 the qualified name of its
     *         children.
     */
    public String[] getFontDecls() {
        if (this.getVersion() == XMLVersion.OOo)
            return new String[] { "font-decls", "style:font-decl" };
        else
            return new String[] { "font-face-decls", "style:font-face" };
    }

    public final Element getLineBreak() {
        return new Element("line-break", getVersion().getTEXT());
    }

    public final Element getTab() {
        return new Element(this.getVersion().equals(XMLVersion.OD) ? "tab" : "tab-stop", getVersion().getTEXT());
    }

    /**
     * How to encode a tab.
     * 
     * @return the xml string to encode a tab.
     * @deprecated use {@link #getTab()}
     */
    public final String getTabS() {
        return this.getVersion().equals(XMLVersion.OD) ? "<text:tab/>" : "<text:tab-stop/>";
    }

    protected final List encodeRT_L(String content, Map styles) {
        String res = JDOMUtils.OUTPUTTER.escapeElementEntities(content);
        final Iterator iter = styles.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry e = (Entry) iter.next();
            res = rt2oo(res, (String) e.getKey(), (String) e.getValue());
        }
        try {
            return JDOMUtils.parseString(res, getVersion().getALL());
        } catch (JDOMException e) {
            // should not happpen as we did escapeElementEntities which gives valid xml and then
            // rt2oo which introduce only static xml
            throw new IllegalStateException("could not parse " + res, e);
        }
    }

    /**
     * Convert rich text (with [] tags) into XML.
     * 
     * @param content the string to convert, eg "texte [b]gras[/b]".
     * @param styles the mapping from tagname (eg "b") to the name of the character style (eg
     *        "Gras").
     * @return the corresponding element.
     */
    public final Element encodeRT(String content, Map styles) {
        return new Element("span", getVersion().getTEXT()).addContent(encodeRT_L(content, styles));
    }

    // create the necessary <text:s c="n"/>
    private Element createSpaces(String spacesS) {
        return new Element("s", getVersion().getTEXT()).setAttribute("c", spacesS.length() + "", getVersion().getTEXT());
    }

    /**
     * Encode a String to OO XML. Handles substition of whitespaces to their OO equivalent.
     * 
     * @param s a plain ole String, eg "term\tdefinition".
     * @return an Element suitable to be inserted in an OO XML document, eg
     * 
     *         <pre>
     *     &lt;text:span&gt;term&lt;text:tab-stop/&gt;definition&lt;/text:span&gt;
     * </pre>
     * 
     *         .
     */
    public final Element encodeWS(final String s) {
        return new Element("span", getVersion().getTEXT()).setContent(encodeWSasList(s));
    }

    private final List<Content> encodeWSasList(final String s) {
        final List<Content> res = new ArrayList<Content>();
        final Matcher m = Pattern.compile("\n|\t| {2,}").matcher(s);
        int last = 0;
        while (m.find()) {
            res.add(new Text(s.substring(last, m.start())));
            switch (m.group().charAt(0)) {
            case '\n':
                res.add(getLineBreak());
                break;
            case '\t':
                res.add(getTab());
                break;
            case ' ':
                res.add(createSpaces(m.group()));
                break;

            default:
                throw new IllegalStateException("unknown item: " + m.group());
            }
            last = m.end();
        }
        res.add(new Text(s.substring(last)));
        return res;
    }

    @SuppressWarnings("unchecked")
    public final void encodeWS(final Text t) {
        final Parent parent = t.getParent();
        final int ind = parent.indexOf(t);
        t.detach();
        parent.getContent().addAll(ind, encodeWSasList(t.getText()));
    }

    @SuppressWarnings("unchecked")
    public final Element encodeWS(final Element elem) {
        final XPath path;
        try {
            path = OOUtils.getXPath(".//text()", XMLVersion.getVersion(elem));
        } catch (JDOMException e) {
            // static path, hence always valid
            throw new IllegalStateException("cannot create XPath", e);
        }
        try {
            final Iterator iter = new ArrayList(path.selectNodes(elem)).iterator();
            while (iter.hasNext()) {
                final Text t = (Text) iter.next();
                encodeWS(t);
            }
        } catch (JDOMException e) {
            throw new IllegalArgumentException("cannot find text nodes of " + elem, e);
        }
        return elem;
    }

}
