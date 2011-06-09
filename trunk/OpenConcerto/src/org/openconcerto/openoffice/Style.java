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

import static java.util.Collections.singleton;
import org.openconcerto.openoffice.ODPackage.RootElement;
import org.openconcerto.openoffice.spreadsheet.CellStyle;
import org.openconcerto.openoffice.spreadsheet.ColumnStyle;
import org.openconcerto.openoffice.spreadsheet.RowStyle;
import org.openconcerto.openoffice.spreadsheet.TableStyle;
import org.openconcerto.openoffice.style.PageLayoutStyle;
import org.openconcerto.openoffice.text.ParagraphStyle;
import org.openconcerto.openoffice.text.TextStyle;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.xml.JDOMUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * A style, see section 16 of v1.2-part1. Maintains a map of family to classes.
 * 
 * @author Sylvain
 */
public class Style extends ODNode {

    private static final Map<XMLVersion, Map<String, StyleDesc<?>>> family2Desc;
    private static final Map<XMLVersion, Map<String, StyleDesc<?>>> elemName2Desc;
    private static final Map<XMLVersion, Map<Class<? extends Style>, StyleDesc<?>>> class2Desc;
    private static boolean descsLoaded = false;
    private static final Map<XMLVersion, Map<Tuple2<String, String>, StyleDesc<?>>> attribute2Desc;
    static {
        final int versionsCount = XMLVersion.values().length;
        family2Desc = new HashMap<XMLVersion, Map<String, StyleDesc<?>>>(versionsCount);
        elemName2Desc = new HashMap<XMLVersion, Map<String, StyleDesc<?>>>(versionsCount);
        class2Desc = new HashMap<XMLVersion, Map<Class<? extends Style>, StyleDesc<?>>>(versionsCount);
        attribute2Desc = new HashMap<XMLVersion, Map<Tuple2<String, String>, StyleDesc<?>>>(versionsCount);
        for (final XMLVersion v : XMLVersion.values()) {
            family2Desc.put(v, new HashMap<String, StyleDesc<?>>());
            elemName2Desc.put(v, new HashMap<String, StyleDesc<?>>());
            class2Desc.put(v, new HashMap<Class<? extends Style>, StyleDesc<?>>());
            attribute2Desc.put(v, new HashMap<Tuple2<String, String>, StyleDesc<?>>(128));
        }
    }

    // lazy initialization to avoid circular dependency (i.e. ClassLoader loads PStyle.DESC which
    // loads StyleStyle which needs PStyle.DESC)
    private static void loadDescs() {
        if (!descsLoaded) {
            registerAllVersions(CellStyle.DESC);
            registerAllVersions(RowStyle.DESC);
            registerAllVersions(ColumnStyle.DESC);
            registerAllVersions(TableStyle.DESC);
            registerAllVersions(TextStyle.DESC);
            registerAllVersions(ParagraphStyle.DESC);
            register(GraphicStyle.DESC);
            register(GraphicStyle.DESC_OO);
            register(PageLayoutStyle.DESC);
            register(PageLayoutStyle.DESC_OO);
            descsLoaded = true;
        }
    }

    // until now styles have remained constant through versions
    private static void registerAllVersions(StyleDesc<? extends Style> desc) {
        for (final XMLVersion v : XMLVersion.values()) {
            if (v == desc.getVersion())
                register(desc);
            else
                register(StyleDesc.copy(desc, v));
        }
    }

    public static void register(StyleDesc<?> desc) {
        if (desc instanceof StyleStyleDesc<?>) {
            final StyleStyleDesc<?> styleStyleDesc = (StyleStyleDesc<?>) desc;
            if (family2Desc.get(desc.getVersion()).put(styleStyleDesc.getFamily(), styleStyleDesc) != null)
                throw new IllegalStateException(styleStyleDesc.getFamily() + " duplicate family");
        } else {
            if (elemName2Desc.get(desc.getVersion()).put(desc.getElementName(), desc) != null)
                throw new IllegalStateException(desc.getElementName() + " duplicate element name");
        }
        if (class2Desc.get(desc.getVersion()).put(desc.getStyleClass(), desc) != null)
            throw new IllegalStateException(desc.getStyleClass() + " duplicate");
    }

    /**
     * Get all registered {@link StyleDesc}.
     * 
     * @param version the version.
     * @return all known descriptions.
     */
    private static Collection<StyleDesc<?>> getDesc(final XMLVersion version) {
        loadDescs();
        return class2Desc.get(version).values();
    }

    /**
     * Return a mapping from element/attribute to its {@link StyleDesc}.
     * 
     * <pre>
     * [ element qualified name, attribute qualified name ] -> StyleDesc ; e.g. :
     * [ "text:p", "text:style-name" ] -> {@link ParagraphStyle#DESC}
     * [ "text:h", "text:style-name" ] -> {@link ParagraphStyle#DESC}
     * [ "text:span", "text:style-name" ] -> {@link TextStyle#DESC}
     * </pre>
     * 
     * @param version the version.
     * @return the mapping from attribute to description.
     */
    private static Map<Tuple2<String, String>, StyleDesc<?>> getAttr2Desc(final XMLVersion version) {
        final Map<Tuple2<String, String>, StyleDesc<?>> map = attribute2Desc.get(version);
        if (map.isEmpty()) {
            for (final StyleDesc<?> desc : getDesc(version)) {
                fillMap(map, desc, desc.getRefElementsMap());
                fillMap(map, desc, desc.getMultiRefElementsMap());
            }
            assert !map.isEmpty();
        }
        return map;
    }

    private static void fillMap(final Map<Tuple2<String, String>, StyleDesc<?>> map, final StyleDesc<?> desc, final CollectionMap<String, String> elemsByAttrs) {
        for (final Entry<String, Collection<String>> e : elemsByAttrs.entrySet()) {
            for (final String elementName : e.getValue()) {
                final Tuple2<String, String> key = Tuple2.create(elementName, e.getKey());
                final StyleDesc<?> previous = map.put(key, desc);
                if (previous != null)
                    throw new IllegalStateException("Duplicate desc for " + key + " : " + previous + " and " + desc);
            }
        }
    }

    /**
     * Create the most specific instance for the passed element.
     * 
     * @param pkg the package where the style is defined.
     * @param styleElem a style:style XML element.
     * @return the most specific instance, e.g. a new ColumnStyle.
     */
    public static Style warp(final ODPackage pkg, final Element styleElem) {
        loadDescs();
        final String name = styleElem.getName();
        if (name.equals(StyleStyleDesc.ELEMENT_NAME)) {
            final String family = StyleStyleDesc.getFamily(styleElem);
            final Map<String, StyleDesc<?>> map = family2Desc.get(pkg.getVersion());
            if (map.containsKey(family)) {
                final StyleDesc<?> styleClass = map.get(family);
                return styleClass.create(pkg, styleElem);
            } else
                return new StyleStyle(pkg, styleElem);
        } else {
            final Map<String, StyleDesc<?>> map = elemName2Desc.get(pkg.getVersion());
            if (map.containsKey(name)) {
                final StyleDesc<?> styleClass = map.get(name);
                return styleClass.create(pkg, styleElem);
            } else
                return new Style(pkg, styleElem);
        }
    }

    public static <S extends Style> S getStyle(final ODPackage pkg, final Class<S> clazz, final String name) {
        final StyleDesc<S> styleDesc = getStyleDesc(clazz, pkg.getVersion());
        return styleDesc.create(pkg, pkg.getStyle(styleDesc, name));
    }

    /**
     * Return the style element referenced by the passed attribute.
     * 
     * @param pkg the package where <code>styleAttr</code> is defined.
     * @param styleAttr any attribute in <code>pkg</code>, e.g.
     *        <code>style:page-layout-name="pm1"</code> of a style:master-page.
     * @return the referenced element if <code>styleAttr</code> is an attribute pointing to a style
     *         in the passed package, <code>null</code> otherwise, e.g.
     *         <code><style:page-layout style:name="pm1"></code>.
     */
    public static Element getReferencedStyleElement(final ODPackage pkg, final Attribute styleAttr) {
        assert styleAttr.getDocument() == pkg.getDocument(RootElement.CONTENT.getZipEntry()) || styleAttr.getDocument() == pkg.getDocument(RootElement.STYLES.getZipEntry()) : "attribute not defined in either the content or the styles of "
                + pkg;
        final StyleDesc<?> desc = getAttr2Desc(pkg.getVersion()).get(Tuple2.create(styleAttr.getParent().getQualifiedName(), styleAttr.getQualifiedName()));
        if (desc != null) {
            return pkg.getStyle(styleAttr.getDocument(), desc, styleAttr.getValue());
        } else
            return null;
    }

    public static <S extends Style> StyleDesc<S> getStyleDesc(Class<S> clazz, final XMLVersion version) {
        return getStyleDesc(clazz, version, true);
    }

    @SuppressWarnings("unchecked")
    private static <S extends Style> StyleDesc<S> getStyleDesc(Class<S> clazz, final XMLVersion version, final boolean mustExist) {
        loadDescs();
        final Map<Class<? extends Style>, StyleDesc<?>> map = class2Desc.get(version);
        if (map.containsKey(clazz))
            return (StyleDesc<S>) map.get(clazz);
        else if (mustExist)
            throw new IllegalArgumentException("unregistered " + clazz + " for version " + version);
        else
            return null;
    }

    protected static <S extends Style> StyleDesc<S> getNonNullStyleDesc(final Class<S> clazz, final XMLVersion version, final Element styleElem, final String styleName) {
        final StyleDesc<S> registered = getStyleDesc(clazz, version, false);
        if (registered != null)
            return registered;
        else
        // generic desc, use styleName as baseName
        if (clazz == StyleStyle.class) {
            @SuppressWarnings("unchecked")
            final StyleDesc<S> res = (StyleDesc<S>) new StyleStyleDesc<StyleStyle>(StyleStyle.class, version, StyleStyleDesc.getFamily(styleElem), styleName) {
                @Override
                public StyleStyle create(ODPackage pkg, Element e) {
                    return new StyleStyle(pkg, styleElem);
                }
            };
            return res;
        } else if (clazz == Style.class) {
            return new StyleDesc<S>(clazz, version, styleElem.getName(), styleName) {
                @Override
                public S create(ODPackage pkg, Element e) {
                    return clazz.cast(new Style(pkg, styleElem));
                }
            };
        } else
            throw new IllegalStateException("Unregistered class which is neither Style not StyleStyle :" + clazz);
    }

    private final StyleDesc<?> desc;
    private final ODPackage pkg;
    private final String name;
    private final XMLVersion ns;

    public Style(final ODPackage pkg, final Element styleElem) {
        super(styleElem);
        this.pkg = pkg;
        this.name = this.getElement().getAttributeValue("name", this.getSTYLE());
        this.ns = this.pkg.getVersion();
        this.desc = getNonNullStyleDesc(this.getClass(), this.ns, styleElem, getName());
        if (!this.desc.getElementName().equals(this.getElement().getName()))
            throw new IllegalArgumentException("expected " + this.desc.getElementName() + " but got " + this.getElement().getName() + " for " + styleElem);
        // assert that styleElem is in pkg (and thus have the same version)
        assert this.pkg.getXMLFile(getElement().getDocument()) != null;
        assert this.pkg.getVersion() == XMLVersion.getVersion(getElement());
    }

    protected final Namespace getSTYLE() {
        return this.getElement().getNamespace("style");
    }

    public final XMLVersion getNS() {
        return this.ns;
    }

    public final String getName() {
        return this.name;
    }

    protected StyleDesc<?> getDesc() {
        return this.desc;
    }

    public Element getFormattingProperties() {
        return this.getFormattingProperties(this.getName());
    }

    /**
     * Create if necessary and return the wanted properties.
     * 
     * @param family type of properties, eg "text".
     * @return the matching properties, eg &lt;text-properties&gt;.
     */
    public final Element getFormattingProperties(final String family) {
        final String childName;
        if (this.getNS() == XMLVersion.OD)
            childName = family + "-properties";
        else
            childName = "properties";
        Element res = this.getElement().getChild(childName, this.getSTYLE());
        if (res == null) {
            res = new Element(childName, this.getSTYLE());
            this.getElement().addContent(res);
        }
        return res;
    }

    /**
     * Return the elements referring to this style in the passed document.
     * 
     * @param doc an XML document.
     * @param wantSingle whether elements that affect only themselves should be included.
     * @param wantMulti whether elements that affect multiple others should be included.
     * @return the list of elements referring to this.
     */
    private final List<Element> getReferences(final Document doc, final boolean wantSingle, boolean wantMulti) {
        return this.desc.getReferences(doc, getName(), wantSingle, wantMulti);
    }

    /**
     * Return the elements referring to this style.
     * 
     * @return the list of elements referring to this.
     */
    public final List<Element> getReferences() {
        return this.getReferences(true, true);
    }

    private final List<Element> getReferences(final boolean wantSingle, final boolean wantMulti) {
        final Document myDoc = this.getElement().getDocument();
        final Document content = this.pkg.getContent().getDocument();
        // my document can always refer to us
        final List<Element> res = this.getReferences(myDoc, wantSingle, wantMulti);
        // but only common styles can be referenced from the content
        if (myDoc != content && !this.isAutomatic())
            res.addAll(this.getReferences(content, wantSingle, wantMulti));
        return res;
    }

    private final boolean isAutomatic() {
        return this.getElement().getParentElement().getQualifiedName().equals("office:automatic-styles");
    }

    public final boolean isReferencedAtMostOnce() {
        // i.e. no multi-references and at most one single reference
        return this.getReferences(false, true).size() == 0 && this.getReferences(true, false).size() <= 1;
    }

    /**
     * Make a copy of this style and add it to its document.
     * 
     * @return the new style with an unused name.
     */
    public final Style dup() {
        // don't use an ODXMLDocument attribute, search for our document in an ODPackage, that way
        // even if our element changes document (toSingle()) we will find the proper ODXMLDocument
        final ODXMLDocument xmlFile = this.pkg.getXMLFile(this.getElement().getDocument());
        final String unusedName = xmlFile.findUnusedName(this.desc, this.desc.getBaseName());
        final Element clone = (Element) this.getElement().clone();
        clone.setAttribute("name", unusedName, this.getSTYLE());
        JDOMUtils.insertAfter(this.getElement(), singleton(clone));
        return this.desc.create(this.pkg, clone);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Style))
            return false;
        final Style o = (Style) obj;
        return this.getName().equals(o.getName()) && this.getElement().getName().equals(o.getElement().getName()) && this.getElement().getNamespace().equals(o.getElement().getNamespace());
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode() + this.getElement().getName().hashCode() + this.getElement().getNamespace().hashCode();
    }
}
