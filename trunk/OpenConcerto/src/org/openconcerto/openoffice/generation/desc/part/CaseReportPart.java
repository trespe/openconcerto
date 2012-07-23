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
 
 package org.openconcerto.openoffice.generation.desc.part;

import org.openconcerto.openoffice.generation.ReportGeneration;
import org.openconcerto.openoffice.generation.desc.ReportPart;
import org.openconcerto.openoffice.generation.desc.ReportType;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.xml.JDOMUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import ognl.OgnlException;

import org.jdom.Element;

/**
 * A report part allowing to execute a block of parts or another.
 * 
 * <ul>
 * <li>
 * Each condition is an expression that returns a boolean result. If the condition's result is true,
 * the value of the CASE expression is the result that follows the condition, and the remainder of
 * the CASE expression is not processed. If the condition's result is not true, any subsequent WHEN
 * clauses are examined in the same manner. If no WHEN condition yields true, the value of the CASE
 * expression is the result of the ELSE clause. If the ELSE clause is omitted and no condition is
 * true, an exception is thrown.
 * 
 * <pre>
 * {@code
 * <case>
 *   <when condition=""></when>
 *   <else></else>
 * </case>
 * </pre>
 * 
 * }</li>
 * <li>
 * There is a "simple" form of CASE expression that is a variant of the general form above:
 * 
 * <pre>
 * {@code
 * <case expr="">
 *   <when value=""></when>
 *   <when value=""></when>
 *   <else></else>
 * </case>
 * }
 * </pre>
 * 
 * The first expression is computed, then compared to each of the value expressions in the WHEN
 * clauses until one is found that is equal to it. If no match is found, the result of the ELSE
 * clause is returned. This is similar to the switch statement in C. Multiple values can be
 * specified with an or element :
 * 
 * <pre>
 * {@code
 * <case expr="">
 *   <when>
 *       <or value="" />
 *       <or value="" />
 *       <then>
 *       </then>
 *   </when>
 *   <when value=""></when>
 *   <else></else>
 * </case>
 * }
 * </pre>
 * 
 * </li>
 * </ul>
 * 
 * @author Sylvain
 */
public final class CaseReportPart extends ReportPart implements ConditionalPart {

    static private final String getRequiredAttr(final Element elem, final String attrName) {
        final String attrVal = elem.getAttributeValue(attrName);
        if (attrVal == null)
            throw new IllegalStateException("Missing " + attrName + " for " + JDOMUtils.output(elem));
        return attrVal;
    }

    private final LinkedHashMap<String, List<ReportPart>> parts;
    private final List<ReportPart> elseParts;
    private final String expr;

    public CaseReportPart(ReportType type, Element elem) {
        super(type, elem);
        this.parts = new LinkedHashMap<String, List<ReportPart>>();
        this.expr = this.elem.getAttributeValue("expr");
        final List children = this.elem.getChildren();
        final int childrenCount = children.size();
        for (int i = 0; i < childrenCount; i++) {
            final Element child = (Element) children.get(i);
            if (child.getName().equals("when")) {
                final String cond;
                if (this.expr == null) {
                    cond = getRequiredAttr(child, "condition");
                } else {
                    final Element thenElem = child.getChild("then");
                    final List orElems;
                    if (thenElem == null) {
                        orElems = Collections.singletonList(child);
                    } else {
                        orElems = child.getChildren("or");
                    }
                    final int orSize = orElems.size();
                    if (orSize == 0)
                        throw new IllegalStateException("missing value to compare to expr");
                    final List<String> orConditions = new ArrayList<String>(orSize);
                    for (int j = 0; j < orSize; j++) {
                        orConditions.add("( " + this.expr + " ) == " + getRequiredAttr((Element) orElems.get(j), "value"));
                    }
                    cond = CollectionUtils.join(orConditions, " or ");
                }
                this.parts.put(cond, type.createParts(child));
            } else if (i == childrenCount - 1 && child.getName().equals("else")) {
                if (child.getAttributeValue("condition") != null || child.getAttributeValue("value") != null)
                    throw new IllegalStateException("Else shouldn't have condition or value : " + JDOMUtils.output(child));
                this.parts.put(null, type.createParts(child));
            } else {
                throw new IllegalStateException("Unknown element  at " + i + " : " + child);
            }
        }
        this.elseParts = this.parts.remove(null);
    }

    public final List<ReportPart> evaluate(final ReportGeneration<?> rg) throws OgnlException {
        for (final Entry<String, List<ReportPart>> e : this.parts.entrySet()) {
            if (rg.evaluatePredicate(e.getKey()))
                return e.getValue();
        }
        if (this.elseParts == null)
            throw new IllegalStateException("No predicate succeeded");
        return this.elseParts;
    }
}
