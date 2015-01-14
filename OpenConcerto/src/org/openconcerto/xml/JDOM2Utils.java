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
 
 package org.openconcerto.xml;

import org.openconcerto.utils.cc.IPredicate;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import javax.xml.validation.SchemaFactory;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.AbstractFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;

/**
 * @author Sylvain
 */
public final class JDOM2Utils {

    public static final XMLOutputter OUTPUTTER;
    private static final SAXBuilder BUILDER;
    static {
        final Format rawFormat = Format.getRawFormat();
        // JDOM defaults to \r\n but \n is shorter and faster (internally used, see LineSeparator
        // class comment)
        rawFormat.setLineSeparator(LineSeparator.NL);
        OUTPUTTER = new XMLOutputter(rawFormat);

        BUILDER = new SAXBuilder();
        BUILDER.setXMLReaderFactory(XMLReaders.NONVALIDATING);
    }

    /**
     * Try to set the platform default for {@link SchemaFactory}. {@link SAXBuilder} constructor
     * initialize {@link org.jdom2.input.sax.XMLReaders} which calls
     * {@link SchemaFactory#newInstance(String)} (this makes JDOM2 a little less fast than JDOM1 on
     * the 1st builder creation : about 60ms vs 15ms). The problem is that if xerces.jar is
     * referenced in the class path, it specifies org.apache.xerces.jaxp.validation.XMLSchemaFactory
     * which is really slow (~700ms to initialize). Alternatively one can create
     * <code>META-INF/services/javax.xml.validation.SchemaFactory</code>.
     * 
     * @throws ClassNotFoundException if the platform default cannot be set.
     */
    public static void forcePlatformDefaultSchemaFactory() throws ClassNotFoundException {
        final String platformClassName = "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory";
        if (Class.forName(platformClassName) != null)
            System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema", platformClassName);
    }

    /**
     * Analyse la chaine passée et retourne l'Element correspondant.
     * 
     * @param xml une chaine contenant un élément XML.
     * @param namespaces les namespaces utilisés dans la chaine.
     * @return l'Element correspondant à la chaine passée.
     * @throws JDOMException si l'xml n'est pas bien formé.
     */
    public static Element parseElementString(String xml, Namespace[] namespaces) throws JDOMException {
        // l'element passé est le seul enfant de dummy
        // to be sure that the 0th can be cast use trim(), otherwise we might get a org.jdom2.Text
        return (Element) parseString(xml.trim(), namespaces).get(0);
    }

    /**
     * Analyse la chaine passée et retourne la liste correspondante.
     * 
     * @param xml une chaine contenant de l'XML.
     * @param namespaces les namespaces utilisés dans la chaine.
     * @return la liste correspondant à la chaine passée.
     * @throws JDOMException si l'xml n'est pas bien formé.
     */
    public static List<Content> parseString(String xml, Namespace[] namespaces) throws JDOMException {
        // construit le dummy pour déclarer les namespaces
        String dummy = "<dummy";
        for (int i = 0; i < namespaces.length; i++) {
            Namespace ns = namespaces[i];
            dummy += " xmlns:" + ns.getPrefix() + "=\"" + ns.getURI() + "\"";
        }
        xml = dummy + ">" + xml + "</dummy>";

        return parseStringDocument(xml).getRootElement().removeContent();
    }

    /**
     * Analyse la chaine passée et retourne l'Element correspondant.
     * 
     * @param xml une chaine contenant de l'XML.
     * @return l'Element correspondant à la chaine passée.
     * @throws JDOMException si l'xml n'est pas bien formé.
     * @see #parseElementString(String, Namespace[])
     */
    public static Element parseString(String xml) throws JDOMException {
        return parseElementString(xml, new Namespace[0]);
    }

    /**
     * Analyse la chaine passée avec un builder par défaut et retourne le Document correspondant.
     * 
     * @param xml une chaine représentant un document XML.
     * @return le document correspondant.
     * @throws JDOMException si l'xml n'est pas bien formé.
     * @see #parseStringDocument(String, SAXBuilder)
     */
    public static synchronized Document parseStringDocument(String xml) throws JDOMException {
        // BUILDER is not thread safe
        return parseStringDocument(xml, BUILDER);
    }

    /**
     * Analyse la chaine passée et retourne le Document correspondant.
     * 
     * @param xml une chaine représentant un document XML.
     * @param builder le builder à utiliser.
     * @return le document correspondant.
     * @throws JDOMException si l'xml n'est pas bien formé.
     */
    public static Document parseStringDocument(String xml, SAXBuilder builder) throws JDOMException {
        Document doc = null;
        try {
            doc = builder.build(new StringReader(xml));
        } catch (IOException e) {
            // peut pas arriver, lis depuis une String
            e.printStackTrace();
        }
        return doc;
    }

    /**
     * Ecrit l'XML en chaine, contrairement a toString().
     * 
     * @param xml l'élément à écrire.
     * @return l'XML en tant que chaine.
     */
    public static String output(Element xml) {
        return OUTPUTTER.outputString(xml);
    }

    /**
     * Ecrit l'XML en chaine, contrairement a toString().
     * 
     * @param xml l'élément à écrire.
     * @return l'XML en tant que chaine.
     */
    public static String output(Document xml) {
        return OUTPUTTER.outputString(xml);
    }

    /**
     * Test if two elements have the same namespace and name.
     * 
     * @param elem1 an element, can be <code>null</code>.
     * @param elem2 an element, can be <code>null</code>.
     * @return <code>true</code> if both elements have the same name and namespace, or if both are
     *         <code>null</code>.
     */
    public static boolean equals(Element elem1, Element elem2) {
        if (elem1 == elem2 || elem1 == null && elem2 == null)
            return true;
        else if (elem1 == null || elem2 == null)
            return false;
        else
            return elem1.getName().equals(elem2.getName()) && elem1.getNamespace().equals(elem2.getNamespace());
    }

    /**
     * Compare two elements and their descendants (only Element and Text). Texts are merged and
     * normalized.
     * 
     * @param elem1 first element.
     * @param elem2 second element.
     * @return <code>true</code> if both elements are equal.
     * @see #getContent(Element, IPredicate, boolean)
     */
    public static boolean equalsDeep(Element elem1, Element elem2) {
        return equalsDeep(elem1, elem2, true);
    }

    public static boolean equalsDeep(Element elem1, Element elem2, final boolean normalizeText) {
        return getDiff(elem1, elem2, normalizeText) == null;
    }

    static String getDiff(Element elem1, Element elem2, final boolean normalizeText) {
        if (elem1 == elem2)
            return null;
        if (!equals(elem1, elem2))
            return "element name or namespace";

        // ignore attributes order
        final List<Attribute> attr1 = elem1.getAttributes();
        final List<Attribute> attr2 = elem2.getAttributes();
        if (attr1.size() != attr2.size())
            return "attributes count";
        for (final Attribute attr : attr1) {
            if (!attr.getValue().equals(elem2.getAttributeValue(attr.getName(), attr.getNamespace())))
                return "attribute value";
        }

        // use content order
        final IPredicate<Content> filter = new IPredicate<Content>() {
            @Override
            public boolean evaluateChecked(Content input) {
                return input instanceof Text || input instanceof Element;
            }
        };
        // only check Element and Text (also merge them)
        final Iterator<Content> contents1 = getContent(elem1, filter, true);
        final Iterator<Content> contents2 = getContent(elem2, filter, true);
        while (contents1.hasNext() && contents2.hasNext()) {
            final Content content1 = contents1.next();
            final Content content2 = contents2.next();
            if (content1.getClass() != content2.getClass())
                return "content";
            if (content1 instanceof Text) {
                final String s1 = normalizeText ? ((Text) content1).getTextNormalize() : content1.getValue();
                final String s2 = normalizeText ? ((Text) content2).getTextNormalize() : content2.getValue();
                if (!s1.equals(s2))
                    return "text";
            } else {
                final String rec = getDiff((Element) content1, (Element) content2, normalizeText);
                if (rec != null)
                    return rec;
            }
        }
        if (contents1.hasNext() || contents2.hasNext())
            return "content size";

        return null;
    }

    /**
     * Get the filtered content of an element, optionnaly merging adjacent {@link Text}. Adjacent
     * text can only happen programmatically.
     * 
     * @param elem the parent.
     * @param pred which content to return.
     * @param mergeText <code>true</code> if adjacent Text should be merged into one,
     *        <code>false</code> to leave the list as it is.
     * @return the filtered content (not supportting {@link Iterator#remove()}).
     */
    public static Iterator<Content> getContent(final Element elem, final IPredicate<? super Content> pred, final boolean mergeText) {
        final Iterator<Content> iter = (Iterator<Content>) elem.getContent(new AbstractFilter<Content>() {
            @Override
            public Content filter(Object obj) {
                final Content c = (Content) obj;
                return pred.evaluateChecked(c) ? c : null;
            }
        }).iterator();
        if (!mergeText)
            return iter;

        return new Iterator<Content>() {

            private Content next = null;

            @Override
            public boolean hasNext() {
                return this.next != null || iter.hasNext();
            }

            @Override
            public Content next() {
                if (this.next != null) {
                    final Content res = this.next;
                    this.next = null;
                    return res;
                }

                Content res = iter.next();
                assert res != null;
                if (res instanceof Text && iter.hasNext()) {
                    this.next = iter.next();
                    Text concatText = null;
                    while (this.next instanceof Text) {
                        if (concatText == null) {
                            concatText = new Text(res.getValue());
                        }
                        concatText.append((Text) this.next);
                        this.next = iter.hasNext() ? iter.next() : null;
                    }
                    assert this.next != null;
                    if (concatText != null)
                        res = concatText;
                }

                return res;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
