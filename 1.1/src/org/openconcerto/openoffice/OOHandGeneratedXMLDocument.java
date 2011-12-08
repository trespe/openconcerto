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
 
 package org.openconcerto.openoffice;

import org.openconcerto.xml.JDOMUtils;

import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * Le contenu d'un document texte d'OpenOffice créé à la main.
 * 
 * @author Sylvain CUAZ 28 oct. 2004
 */
public class OOHandGeneratedXMLDocument extends ODSingleXMLDocument {

    /**
     * Crée un document avec les éléments raçines spécifiés. Par exemple font-decls, styles et body.
     * 
     * @param elements les éléments du document à créer.
     * @return le document correspondant.
     * @throws IllegalArgumentException si pas de body.
     */
    static public OOHandGeneratedXMLDocument create(Element[] elements) {
        final XMLVersion ooVersion = XMLVersion.OOo;
        Element root = new Element("document-content", ooVersion.getOFFICE());
        root.setAttribute("class", "text", ooVersion.getOFFICE());
        root.setAttribute("version", "1.0", ooVersion.getOFFICE());
        boolean hasBody = false;
        for (int i = 0; i < elements.length; i++) {
            Element elem = elements[i];
            root.addContent(elem);
            hasBody = !hasBody && elem.getName().equals("body");
        }
        if (!hasBody)
            throw new IllegalArgumentException("you must specify a body.");
        return new OOHandGeneratedXMLDocument(new Document(root));
    }

    /**
     * Crée un document en spécifiant le corps et en prenant les styles définis dans
     * handAutoStyles.xml.
     * 
     * @param body le corps du document à créer.
     * @return le document correspondant.
     * @throws JDOMException si XML non valide.
     * @throws IOException si erreur lecture de handAutoStyles.xml.
     */
    static public OOHandGeneratedXMLDocument create(String body) throws JDOMException, IOException {
        Document stylesDoc = OOUtils.getBuilder().build(OOHandGeneratedXMLDocument.class.getResource("handAutoStyles.xml"));
        return create(stylesDoc.detachRootElement(), JDOMUtils.parseElementString("<office:body>" + body + "</office:body>", XMLVersion.getOOo().getALL()));
    }

    static private OOHandGeneratedXMLDocument create(Element autoStyles, Element body) {
        return create(new Element[] { autoStyles, body });
    }

    // passer par les static
    private OOHandGeneratedXMLDocument(Document content) {
        super(content);
    }

}
