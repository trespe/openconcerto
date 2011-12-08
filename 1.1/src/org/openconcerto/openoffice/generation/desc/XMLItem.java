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

import java.util.List;

import org.jdom.Element;

/**
 * Un élément du fichier XML qui possède un nom et (optionellement) des paramètres.
 * 
 * @author Sylvain CUAZ
 */
public abstract class XMLItem {

    protected final Element elem;
    private final String name;
    // necessary to evalute expression of parameters
    private final ReportType type;
    // we need a factory so that we don't call initCommonData() if we don't have to
    // eg initCommonData() has { put("id", this.myRow.getID()) } and we do this.myRow =
    // insert(getParam('foo')); if this called getCommonData() it would throw a NPE.
    private final IFactory<?> dataFactory = new IFactory<Object>() {
        @Override
        public Object createChecked() {
            return getType().getRg().getCommonData();
        }
    };

    public XMLItem(Element elem) {
        this(elem, null);
    }

    public XMLItem(Element elem, final ReportType rt) {
        this.elem = elem;
        this.name = elem.getAttributeValue("name");
        if (this.name == null)
            throw new IllegalStateException("no attribute name for item: " + elem);
        this.type = rt;
    }

    public final String getName() {
        return this.name;
    }

    // not final since ReportType cannot pass this to super()
    public final ReportType getType() {
        return this instanceof ReportType ? (ReportType) this : this.type;
    }

    private final IFactory<?> getData() {
        return this.getType() == null ? null : this.dataFactory;
    }

    protected final Object evaluateOgnl(final String s) {
        return ParamsHelper.evaluteOgnl(s, getData());
    }

    public String getParam(String paramName) {
        return ParamsHelper.getParam(this.elem, paramName, getData());
    }

    public String getParam(String paramName, final String def) {
        final String p = getParam(paramName);
        return p == null ? def : p;
    }

    /**
     * The parameter as a boolean value.
     * 
     * @param paramName the name of the parameter.
     * @return <code>null</code> if not found, otherwise use {@link Boolean#parseBoolean(String)}.
     */
    public Boolean getBooleanParam(String paramName) {
        final String p = getParam(paramName);
        return p == null ? null : Boolean.parseBoolean(p);
    }

    /**
     * The parameter as a boolean value.
     * 
     * @param paramName the name of the parameter.
     * @param def default value if <code>paramName</code> not found.
     * @return {@link #getBooleanParam(String)} if not <code>null</code>, otherwise <code>def</code>
     */
    public boolean getBooleanParam(String paramName, final boolean def) {
        final Boolean p = getBooleanParam(paramName);
        return p == null ? def : p;
    }

    public List<String> getListParam(String paramName) {
        return ParamsHelper.getListParam(this.elem, paramName, getData());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.getName();
    }
}
