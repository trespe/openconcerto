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
 
 package org.openconcerto.openoffice.generation.desc;

import org.jdom.Attribute;
import org.jdom.Element;

/**
 * A group of report part that will replace corresponding "include" part. The resulting part will
 * have the content of the define and its attributes if they're not set on the include. This allows
 * to override attributes for some include.
 * 
 * @author Sylvain CUAZ
 */
public final class ReportDefine extends XMLItem {

    public ReportDefine(Element elem) {
        super(elem);
        if (!"defineSub".equals(this.elem.getName()))
            throw new IllegalArgumentException("not a define: " + elem);
    }

    /**
     * Remplace l'élément passé par notre contenu.
     * 
     * @param includeElem l'élément à remplacer.
     */
    public void replace(Element includeElem) {
        includeElem.setContent(this.elem.cloneContent());
        // let include override attributes
        for (final Object attrO : this.elem.getAttributes()) {
            final Attribute attr = (Attribute) attrO;
            if (includeElem.getAttribute(attr.getName(), attr.getNamespace()) == null)
                includeElem.setAttribute(attr.getName(), attr.getValue(), attr.getNamespace());
        }
        includeElem.setName("sub");
    }
}
