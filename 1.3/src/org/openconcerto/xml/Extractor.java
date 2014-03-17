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

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

/**
 * Define how to create an XML element for a particular property of an object. Eg the localisation
 * of an item : the element name might be "local" with one attribute "floor" and a subelement to
 * describe its use.
 * 
 * @author Sylvain CUAZ
 * @param <T> the type of object this extractor operates on.
 * @see org.openconcerto.xml.ElementOrganizer
 */
public class Extractor<T> {

    // eg "local"
    private final String name;
    // eg ["id","designation"]
    private final List<String> propNames;
    // eg ["id"]
    private final List<String> equalityPropNames;
    // {propName -> Transformer}
    private final Map<String, ITransformer<? super T, ?>> transformers;
    private final Set<String> attributes, elements;

    public Extractor(String name, String propNames) {
        this(name, propNames, -1);
    }

    /**
     * Creates a new extractor.
     * 
     * @param name the name of element ; eg "local".
     * @param propNames the properties (attributes+elements) of this element ; eg "id,designation".
     * @param equality the number of properties to use for testing equality, -1 for all ; eg 1 for
     *        testing on id.
     */
    public Extractor(String name, String propNames, int equality) {
        this.name = name;
        this.propNames = Arrays.asList(propNames.split(","));
        this.equalityPropNames = this.propNames.subList(0, equality == -1 ? this.propNames.size() : equality);
        this.transformers = new HashMap<String, ITransformer<? super T, ?>>();
        // predictable and determinist Element
        this.attributes = new LinkedHashSet<String>();
        this.elements = new LinkedHashSet<String>();
    }

    /**
     * Tell this extractor that <code>propName</code> is an attribute whose value can be computed
     * thanks to <code>t</code>.
     * 
     * @param propName the name of a property listed in constructor.
     * @param t a transformer that will return the value of <code>propName</code>.
     * @return this.
     */
    public Extractor<T> addAttribute(String propName, ITransformer<? super T, ?> t) {
        addTransformer(propName, t);
        this.attributes.add(propName);
        return this;
    }

    /**
     * Tell this extractor that <code>propName</code> is a sub-element whose value can be computed
     * thanks to <code>t</code>.
     * 
     * @param propName the name of a property listed in constructor.
     * @param t a transformer that will return the value of <code>propName</code>.
     * @return this.
     */
    public Extractor<T> addElement(String propName, ITransformer<? super T, ?> t) {
        addTransformer(propName, t);
        this.elements.add(propName);
        return this;
    }

    private void addTransformer(String propName, ITransformer<? super T, ?> t) {
        if (!this.propNames.contains(propName))
            throw new IllegalArgumentException("unknown property: " + propName);
        this.transformers.put(propName, t);
    }

    // *** get

    /**
     * Get the predicate to find an element created by this extractor. For example :
     * ./person[@name="hi" and ./desc="a long desc"]
     * 
     * @param o the object from which the element was created.
     * @return an XPath predicate using properties specified in the constructor.
     * @throws JDOMException if an xpath problem.
     */
    public XPath getXPath(T o) throws JDOMException {
        final List<String> props = new ArrayList<String>(this.equalityPropNames.size());
        int i = 0;
        // use variables since both Jaxen and JXPath do not support XPath 2.0 escaping (ie doubling
        // quotes) : Jaxen not at all and JXPath use XML escaping (ie &quot;).
        final Map<String, Object> vars = new HashMap<String, Object>();
        for (final String propName : this.equalityPropNames) {
            final Object propValue = getValue(propName, o);
            final String varName = "var" + i;

            if (this.attributes.contains(propName))
                props.add("@" + propName + "=$" + varName + "");
            else if (this.elements.contains(propName))
                props.add("./" + propName + "=$" + varName + "");
            else
                throw new IllegalStateException(propName + " has not been added");

            vars.put(varName, propValue);
            i++;
        }
        final XPath res = XPath.newInstance("./" + this.getName() + "[" + CollectionUtils.join(props, " and ") + "]");
        for (final Entry<String, Object> e : vars.entrySet()) {
            res.setVariable(e.getKey(), e.getValue());
        }
        return res;
    }

    private Object getValue(final String propName, T o) {
        return this.transformers.get(propName).transformChecked(o);
    }

    public final String getName() {
        return this.name;
    }

    /**
     * Create an XML element representing the passed object with the attributes and child elements
     * specified by add*().
     * 
     * @param o the object to convert.
     * @return the corresponding XML element.
     */
    public Element createElement(T o) {
        final Element res = new Element(this.getName());
        Iterator<String> iter = this.attributes.iterator();
        while (iter.hasNext()) {
            final String propName = iter.next();
            res.setAttribute(propName, getValue(propName, o).toString());
        }
        iter = this.elements.iterator();
        while (iter.hasNext()) {
            final String propName = iter.next();
            res.addContent(new Element(propName).setText(getValue(propName, o).toString()));
        }
        return res;
    }

}
