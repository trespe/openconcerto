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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.jxpath.JXPathContext;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

/**
 * Allow to use JXPath with JDOM's {@link XPath}.
 * 
 * @author Sylvain
 */
public class JXPathXPath extends XPath {

    private static final Pattern quotePattern = Pattern.compile("\"");

    public static final String toLiteral(final String s) {
        return '"' + quotePattern.matcher(s).replaceAll("&quot;") + '"';
    }

    private final String path;
    private final List<Namespace> namespaces;
    private final Map<String, Object> variables;

    public JXPathXPath(final String path) {
        this.path = path;
        this.namespaces = new ArrayList<Namespace>();
        this.variables = new HashMap<String, Object>();
    }

    @Override
    public String getXPath() {
        return this.path;
    }

    @Override
    public void addNamespace(Namespace namespace) {
        this.namespaces.add(namespace);
    }

    @Override
    public void setVariable(String name, Object value) {
        this.variables.put(name, value);
    }

    @Override
    public String valueOf(Object context) throws JDOMException {
        return (String) getJXPath(context).getValue("fn:string(" + this.getXPath() + ")");
    }

    @Override
    public Number numberValueOf(Object context) throws JDOMException {
        return (Number) getJXPath(context).getValue(this.getXPath(), Number.class);
    }

    @Override
    public Object selectSingleNode(Object context) throws JDOMException {
        return getJXPath(context).selectSingleNode(this.getXPath());
    }

    @Override
    public List selectNodes(Object context) throws JDOMException {
        return getJXPath(context).selectNodes(this.getXPath());
    }

    private JXPathContext getJXPath(final Object context) {
        final JXPathContext newContext = JXPathContext.newContext(context);
        for (final Namespace ns : this.namespaces)
            newContext.registerNamespace(ns.getPrefix(), ns.getURI());
        for (final Entry<String, Object> e : this.variables.entrySet())
            newContext.getVariables().declareVariable(e.getKey(), e.getValue());
        // otherwise /a/b on an empty document throws an exception
        newContext.setLenient(true);
        return newContext;
    }
}
