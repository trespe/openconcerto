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

import org.openconcerto.utils.cc.IFactory;
import org.openconcerto.xml.JDOMUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ognl.Ognl;
import ognl.OgnlException;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

/**
 * Permet de retrouver la valeur appropriée du paramètre demandé. La valeur retournée est celle
 * spécifiée par le premier ancêtre possédant ce paramètre ; ceci permet une pseudo surcharge de
 * paramètre.
 * 
 * @author Sylvain CUAZ
 */
public abstract class ParamsHelper {

    static private final Element getParamElem(Element elem, String paramName) {
        try {
            XPath p = XPath.newInstance("./ancestor-or-self::*[./params/param/@name='" + paramName + "'][1]/params/param[@name='" + paramName + "']");
            return (Element) p.selectSingleNode(elem);
        } catch (JDOMException e) {
            // can't happen
            throw new IllegalStateException("pb xpath", e);
        }
    }

    /**
     * Returns a parameter value as a String. The value is either the text of a child element named
     * 'value' or the attribute named 'value'.
     * 
     * @param elem the element from which to search.
     * @param paramName the name of the desired parameter.
     * @param data the data if <code>paramName</code> value is not a string constant.
     * @return a String, or <code>null</code> if param not found.
     * @throws IllegalArgumentException if there's more than one value child element.
     */
    static final String getParam(Element elem, String paramName, IFactory<?> data) {
        return (String) getParam(elem, paramName, data, false);
    }

    static private final Object getParam(Element elem, String paramName, final IFactory<?> data, final boolean asList) {
        final Element paramElem = getParamElem(elem, paramName);
        if (paramElem == null)
            return null;

        final String attr = paramElem.getAttributeValue("value");
        final String exprAttr = paramElem.getAttributeValue("valueExpr");
        final List<Element> children = getListParam(paramElem);

        final boolean[] valuesEmptyness = { attr == null, exprAttr == null, children.isEmpty() };
        final String[] names = { "value attribute", "valueExpr attribute", "children" };
        final List<String> definedValues = new ArrayList<String>();
        for (int i = 0; i < valuesEmptyness.length; i++) {
            if (!valuesEmptyness[i])
                definedValues.add(names[i]);
        }
        if (definedValues.size() == 0)
            throw new IllegalArgumentException(JDOMUtils.output(elem) + " has neither an attribute nor any children defined");
        if (definedValues.size() > 1)
            throw new IllegalArgumentException(JDOMUtils.output(elem) + " has defined :" + definedValues);

        if (asList) {
            final List<String> res;
            if (attr != null) {
                res = Collections.singletonList(attr);
            } else if (exprAttr != null) {
                final Object value = evaluteOgnl(exprAttr, data);
                if (value instanceof List) {
                    final List<?> l = (List<?>) value;
                    res = new ArrayList<String>(l.size());
                    for (final Object o : l)
                        res.add(o.toString());
                } else {
                    res = Collections.singletonList(value.toString());
                }
            } else {
                res = new ArrayList<String>();
                for (final Element valElem : children) {
                    res.add(getValue(valElem));
                }
            }
            return res;
        } else {
            final String res;
            if (attr != null) {
                res = attr;
            } else if (exprAttr != null) {
                res = evaluteOgnl(exprAttr, data).toString();
            } else {
                if (children.size() > 1)
                    throw new IllegalArgumentException("more than one children for " + JDOMUtils.output(elem));
                res = getValue(children.get(0));
            }
            return res;
        }
    }

    static Object evaluteOgnl(final String exprAttr, IFactory<?> data) {
        try {
            return Ognl.getValue(exprAttr, data.createChecked());
        } catch (OgnlException e) {
            throw new IllegalArgumentException("error evaluating :" + exprAttr, e);
        }
    }

    private static String getValue(final Element valElem) {
        // ATTN trim : cannot specify " val"
        // but allow longer text to begin (end) with a newline in the xml
        return valElem.getTextTrim();
    }

    @SuppressWarnings("unchecked")
    static private final List<Element> getListParam(Element paramElem) {
        return paramElem.getChildren("value");
    }

    /**
     * Returns a parameter value as a List. The values are the text of child elements named 'value'.
     * If elem has no such child, but has an attribute 'value' then this is returned in a singleton
     * list.
     * 
     * @param elem the element from which to search.
     * @param paramName the name of the desired parameter.
     * @param data the data if <code>paramName</code> value is not a string constant.
     * @return a List of String, or <code>null</code> if param not found.
     */
    @SuppressWarnings("unchecked")
    static final List<String> getListParam(Element elem, String paramName, IFactory<?> data) {
        return (List<String>) getParam(elem, paramName, data, true);
    }

}
