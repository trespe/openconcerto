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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * @author ILM Informatique 26 juil. 2004
 */
public final class JDOMUtils {

    public static final XMLOutputter OUTPUTTER;
    private static final SAXBuilder BUILDER;
    static {
        final Format rawFormat = Format.getRawFormat();
        // JDOM has \r\n hardcoded
        rawFormat.setLineSeparator("\n");
        OUTPUTTER = new XMLOutputter(rawFormat);

        BUILDER = new SAXBuilder();
        BUILDER.setValidation(false);
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
        // to be sure that the 0th can be cast use trim(), otherwise we might get a org.jdom.Text
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
    public static List parseString(String xml, Namespace[] namespaces) throws JDOMException {
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

    public static Element getAncestor(Element element, final String name, final Namespace ns) {
        return getAncestor(element, new IPredicate<Element>() {
            @Override
            public boolean evaluateChecked(Element elem) {
                return elem.getName().equals(name) && elem.getNamespace().equals(ns);
            }
        });
    }

    public static Element getAncestor(Element element, final IPredicate<Element> pred) {
        Element current = element;
        while (current != null) {
            if (pred.evaluateChecked(current))
                return current;
            current = current.getParentElement();
        }
        return null;
    }

    /**
     * Add namespace declaration to <code>elem</code> if needed. Necessary since JDOM uses a simple
     * list.
     * 
     * @param elem the element where namespaces should be available.
     * @param c the namespaces to add.
     * @see Element#addNamespaceDeclaration(Namespace)
     */
    public static void addNamespaces(final Element elem, final Collection<Namespace> c) {
        if (c instanceof RandomAccess && c instanceof List) {
            final List<Namespace> list = (List<Namespace>) c;
            final int stop = c.size() - 1;
            for (int i = 0; i < stop; i++) {
                final Namespace ns = list.get(i);
                if (elem.getNamespace(ns.getPrefix()) == null)
                    elem.addNamespaceDeclaration(ns);
            }
        } else {
            for (final Namespace ns : c) {
                if (elem.getNamespace(ns.getPrefix()) == null)
                    elem.addNamespaceDeclaration(ns);
            }
        }
    }

    public static void addNamespaces(final Element elem, final Namespace... l) {
        for (final Namespace ns : l) {
            if (elem.getNamespace(ns.getPrefix()) == null)
                elem.addNamespaceDeclaration(ns);
        }
    }

    /**
     * Aka mkdir -p.
     * 
     * @param current l'élément dans lequel créer la hierarchie.
     * @param path le chemin des éléments à créer, chaque niveau séparé par "/".
     * @return le dernier élément créé.
     */
    public Element mkElem(Element current, String path) {
        String[] items = path.split("/");
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            String[] qname = item.split(":");
            final Element elem;
            if (qname.length == 1)
                elem = new Element(item);
            else
                // MAYBE check if getNS return null and throw exn
                elem = new Element(qname[1], current.getNamespace(qname[0]));
            current.addContent(elem);
            current = elem;
        }
        return current;
    }

    public static void insertAfter(final Element insertAfter, final Collection<? extends Content> toAdd) {
        insertSiblings(insertAfter, toAdd, true);
    }

    public static void insertBefore(final Element insertBefore, final Collection<? extends Content> toAdd) {
        insertSiblings(insertBefore, toAdd, false);
    }

    /**
     * Add content before or after an element.
     * 
     * @param sibling an element with a parent.
     * @param toAdd the content to add alongside <code>sibling</code>.
     * @param after <code>true</code> to add it after <code>sibling</code>.
     */
    public static void insertSiblings(final Element sibling, final Collection<? extends Content> toAdd, final boolean after) {
        final Element parentElement = sibling.getParentElement();
        final int index = parentElement.indexOf(sibling);
        parentElement.addContent(after ? index + 1 : index, toAdd);
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
        if (elem1 == null && elem2 == null)
            return true;
        else if (elem1 == null || elem2 == null)
            return false;
        else
            return elem1.getName().equals(elem2.getName()) && elem1.getNamespace().equals(elem2.getNamespace());
    }

    // @return SAXException If a SAX error occurs during parsing of doc.
    static SAXException validate(final Document doc, final Schema schema, final ErrorHandler errorHandler) {
        ByteArrayInputStream ins;
        try {
            ins = new ByteArrayInputStream(output(doc).getBytes("UTF8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("unicode not found ", e);
        }
        final Validator validator = schema.newValidator();
        // ATTN workaround : contrary to documentation setting to null swallow exceptions
        if (errorHandler != null)
            validator.setErrorHandler(errorHandler);
        try {
            // don't use JDOMSource since it's as inefficient as this plus we can't control the
            // output.
            validator.validate(new StreamSource(ins));
            return null;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read the document", e);
        } catch (SAXException e) {
            return e;
        }
    }

    static void validateDTD(final Document doc, final SAXBuilder b, final ErrorHandler errorHandler) throws JDOMException {
        final ErrorHandler origEH = b.getErrorHandler();
        final boolean origValidation = b.getValidation();
        try {
            b.setErrorHandler(errorHandler);
            b.setValidation(true);
            JDOMUtils.parseStringDocument(output(doc), b);
        } finally {
            b.setErrorHandler(origEH);
            b.setValidation(origValidation);
        }
    }

}
