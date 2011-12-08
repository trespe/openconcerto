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

import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Parent;
import org.jdom.xpath.XPath;

/**
 * Permet d'organiser une collection en une hiérarchie XML. Avec <code>
 * Obs1(bat=BAT A, local=A1, num=1),
 * Obs2(bat=BAT B, local=B1, num=2),
 * Obs3(bat=BAT B, local=B2, num=3),
 * Obs4(bat=BAT B, local=B2, num=4)
 * <code>
 * ainsi que 3 extracteurs pour trouver le batiment, le local, et l'observation, on a
 * <pre>
 *       &lt;batiment name=&quot;A&quot;&gt;
 *           &lt;local name=&quot;A1&quot;&gt;
 *            &lt;obs nb=&quot;1&quot;/&gt;
 *           &lt;/local&gt;
 *       &lt;/batiment&gt;
 *       &lt;batiment name=&quot;B&quot;&gt;
 *           &lt;local name=&quot;B1&quot;&gt;
 *            &lt;obs nb=&quot;2&quot;/&gt;
 *           &lt;/local&gt;
 *           &lt;local name=&quot;B2&quot;&gt;
 *            &lt;obs nb=&quot;3&quot;/&gt;
 *            &lt;obs nb=&quot;4&quot;/&gt;
 *           &lt;/local&gt;
 *       &lt;/batiment&gt;
 * </pre>
 * 
 * @author Sylvain CUAZ
 * @param <T> type of elements to organize
 */
public class ElementOrganizer<T> {

    private final List<Extractor<T>> propExtractors;
    private final Comparator<T> propComp;

    public ElementOrganizer(List<Extractor<T>> extractors, Comparator<T> propComp) {
        this.propExtractors = extractors;
        if (this.propExtractors.size() == 0)
            throw new IllegalArgumentException("Empty property extractors");
        this.propComp = propComp;
    }

    /**
     * Creates an organizer.
     * 
     * @param extractors a list of Extractor.
     * @param propComp a list of Comparator, can be <code>null</code> to do no sort.
     */
    public ElementOrganizer(List<Extractor<T>> extractors, List<Comparator<T>> propComp) {
        this(extractors, propComp == null ? null : CompareUtils.createComparator(propComp));
    }

    /**
     * Permet d'organiser une collection en une hiérarchie XML. The children of the returned Element
     * will have extractors[0].getName() for name, their children will be named
     * extractors[1].getName(), and so on.
     * 
     * @param col la collection à organiser.
     * @return une hiérarchie XML.
     * @throws IllegalStateException si l'ordre de <code>col</code> après tri n'est pas le même que
     *         l'ordre du document XML.
     */
    public final Element organize(Collection<? extends T> col) {
        final List<T> l = new ArrayList<T>(col);
        if (this.propComp != null)
            Collections.sort(l, this.propComp);

        final Element res = new Element("root");

        for (final T item : l) {
            Element elem = res;
            for (int i = 0; i < this.propExtractors.size(); i++) {
                final Extractor<T> extractor = this.propExtractors.get(i);
                try {
                    // eg ./batiment[@id='132' and ./label='batA']
                    final XPath xpath = extractor.getXPath(item);
                    final Element itemElem = (Element) xpath.selectSingleNode(elem);
                    if (itemElem == null) {
                        final Element newElement = extractor.createElement(item);
                        elem.addContent(newElement);
                        elem = newElement;
                    } else {
                        final Parent parent = itemElem.getParent();
                        if (parent.indexOf(itemElem) != parent.getContentSize() - 1)
                            throw new IllegalStateException("noncoherent sort: " + item + " would have been added in " + JDOMUtils.output(itemElem) + "\nof\n" + JDOMUtils.output(res));
                        elem = itemElem;
                    }
                } catch (JDOMException e) {
                    throw ExceptionUtils.createExn(IllegalStateException.class, "xpath pb", e);
                }
            }
        }

        return res;
    }

}
