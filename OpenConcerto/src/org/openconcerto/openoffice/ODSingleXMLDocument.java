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

import static org.openconcerto.openoffice.ODPackage.RootElement.CONTENT;
import org.openconcerto.openoffice.ODPackage.RootElement;
import org.openconcerto.utils.CopyUtils;
import org.openconcerto.utils.ProductInfo;
import org.openconcerto.utils.cc.IFactory;
import org.openconcerto.xml.JDOMUtils;
import org.openconcerto.xml.SimpleXMLPath;
import org.openconcerto.xml.Step;
import org.openconcerto.xml.Step.Axis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

/**
 * An XML document containing all of an office document, see section 2.1 of OpenDocument 1.1.
 * 
 * @author Sylvain CUAZ 24 nov. 2004
 */
public class ODSingleXMLDocument extends ODXMLDocument implements Cloneable {

    final static Set<String> DONT_PREFIX;
    static {
        DONT_PREFIX = new HashSet<String>();
        // don't touch to user fields and variables
        // we want them to be the same across the document
        DONT_PREFIX.add("user-field-decl");
        DONT_PREFIX.add("user-field-get");
        DONT_PREFIX.add("variable-get");
        DONT_PREFIX.add("variable-decl");
        DONT_PREFIX.add("variable-set");
    }

    // Voir le TODO du ctor
    // public static OOSingleXMLDocument createEmpty() {
    // }

    /**
     * Create a document from a collection of subdocuments.
     * 
     * @param content the content.
     * @param style the styles, can be <code>null</code>.
     * @return the merged document.
     */
    public static ODSingleXMLDocument createFromDocument(Document content, Document style) {
        return ODPackage.createFromDocuments(content, style).toSingle();
    }

    static ODSingleXMLDocument create(ODPackage files) {
        final Document content = files.getContent().getDocument();
        final Document style = files.getDocument(RootElement.STYLES.getZipEntry());
        // signal that the xml is a complete document (was document-content)
        final Document singleContent = RootElement.createSingle(content);
        copyNS(content, singleContent);
        files.getContentType().setType(singleContent);
        final Element root = singleContent.getRootElement();
        root.addContent(content.getRootElement().removeContent());
        // see section 2.1.1 first meta, then settings, then the rest
        prependToRoot(files.getDocument(RootElement.SETTINGS.getZipEntry()), root);
        prependToRoot(files.getDocument(RootElement.META.getZipEntry()), root);
        final ODSingleXMLDocument single = new ODSingleXMLDocument(singleContent, files);
        if (single.getChild("body") == null)
            throw new IllegalArgumentException("no body in " + single);
        if (style != null) {
            // section 2.1 : Styles used in the document content and automatic styles used in the
            // styles themselves.
            // more precisely in section 2.1.1 : office:document-styles contains style, master
            // style, auto style, font decls ; the last two being also in content.xml but are *not*
            // related : eg P1 of styles.xml is *not* the P1 of content.xml
            try {
                single.mergeAllStyles(new ODXMLDocument(style), true);
            } catch (JDOMException e) {
                throw new IllegalArgumentException("style is not valid", e);
            }
        }
        return single;
    }

    private static void prependToRoot(Document settings, final Element root) {
        if (settings != null) {
            copyNS(settings, root.getDocument());
            final Element officeSettings = (Element) settings.getRootElement().getChildren().get(0);
            root.addContent(0, (Element) officeSettings.clone());
        }
    }

    // some namespaces are needed even if not directly used, see § 18.3.19 namespacedToken
    // of v1.2-part1-cd04 (e.g. 19.31 config:name or 19.260 form:control-implementation)
    @SuppressWarnings("unchecked")
    private static void copyNS(final Document src, final Document dest) {
        JDOMUtils.addNamespaces(dest.getRootElement(), src.getRootElement().getAdditionalNamespaces());
    }

    /**
     * Create a document from a package.
     * 
     * @param f an OpenDocument package file.
     * @return the merged file.
     * @throws JDOMException if the file is not a valid OpenDocument file.
     * @throws IOException if the file can't be read.
     */
    public static ODSingleXMLDocument createFromPackage(File f) throws JDOMException, IOException {
        // this loads all linked files
        return new ODPackage(f).toSingle();
    }

    /**
     * Create a document from a flat XML.
     * 
     * @param f an OpenDocument XML file.
     * @return the created file.
     * @throws JDOMException if the file is not a valid OpenDocument file.
     * @throws IOException if the file can't be read.
     */
    public static ODSingleXMLDocument createFromFile(File f) throws JDOMException, IOException {
        final ODSingleXMLDocument res = new ODSingleXMLDocument(OOUtils.getBuilder().build(f));
        res.getPackage().setFile(f);
        return res;
    }

    public static ODSingleXMLDocument createFromStream(InputStream ins) throws JDOMException, IOException {
        return new ODSingleXMLDocument(OOUtils.getBuilder().build(ins));
    }

    /**
     * fix bug when a SingleXMLDoc is used to create a document (for example with P2 and 1_P2), and
     * then create another instance s2 with the previous document and add a second file (also with
     * P2 and 1_P2) => s2 will contain P2, 1_P2, 1_P2, 1_1_P2.
     */
    private static final String COUNT = "SingleXMLDocument_count";

    /** Le nombre de fichiers concat */
    private int numero;
    /** Les styles présent dans ce document */
    private final Set<String> stylesNames;
    /** Les styles de liste présent dans ce document */
    private final Set<String> listStylesNames;
    /** Les fichiers référencés par ce document */
    private ODPackage pkg;
    private final ODMeta meta;
    // the element between each page
    private Element pageBreak;

    public ODSingleXMLDocument(Document content) {
        this(content, new ODPackage());
    }

    /**
     * A new single document. NOTE: this document will put himself in <code>pkg</code>, replacing
     * any previous content.
     * 
     * @param content the XML.
     * @param pkg the package this document belongs to.
     */
    private ODSingleXMLDocument(Document content, final ODPackage pkg) {
        super(content);

        // inited in getPageBreak()
        this.pageBreak = null;

        this.pkg = pkg;
        for (final RootElement e : RootElement.getPackageElements())
            this.pkg.rmFile(e.getZipEntry());
        this.pkg.putFile(CONTENT.getZipEntry(), this, "text/xml");

        // set the generator
        // creates if necessary meta at the right position
        this.getChild("meta", true);
        ProductInfo props = ProductInfo.getInstance();
        // MAYBE add a version number for this framework (using
        // ODPackage.class.getResourceAsStream("product.properties") and *not* "/product.properties"
        // as it might interfere with products using this framework)
        if (props == null)
            props = new ProductInfo(this.getClass().getName());
        final String generator;
        if (props.getVersion() == null)
            generator = props.getName();
        else
            generator = props.getName() + "/" + props.getVersion();
        this.meta = ODMeta.create(this);
        this.meta.setGenerator(generator);

        final ODUserDefinedMeta userMeta = this.meta.getUserMeta(COUNT);
        if (userMeta != null) {
            final Object countValue = userMeta.getValue();
            if (countValue instanceof Number) {
                this.numero = ((Number) countValue).intValue();
            } else {
                this.numero = new BigDecimal(countValue.toString()).intValue();
            }
        } else {
            // if not hasCount(), it's not us that created content
            // so there should not be any 1_
            this.setNumero(0);
        }

        this.stylesNames = new HashSet<String>(64);
        this.listStylesNames = new HashSet<String>(16);

        // little trick to find the common styles names (not to be prefixed so they remain
        // consistent across the added documents)
        final Element styles = this.getChild("styles");
        if (styles != null) {
            // create a second document with our styles to collect names
            final Element root = this.getDocument().getRootElement();
            final Document clonedDoc = new Document(new Element(root.getName(), root.getNamespace()));
            clonedDoc.getRootElement().addContent(styles.detach());
            try {
                this.mergeStyles(new ODXMLDocument(clonedDoc));
            } catch (JDOMException e) {
                throw new IllegalArgumentException("can't find common styles names.");
            }
            // reattach our styles
            styles.detach();
            this.setChild(styles);
        }
    }

    ODSingleXMLDocument(ODSingleXMLDocument doc, ODPackage p) {
        super(doc);
        if (p == null)
            throw new NullPointerException("Null package");
        this.stylesNames = new HashSet<String>(doc.stylesNames);
        this.listStylesNames = new HashSet<String>(doc.listStylesNames);
        this.pkg = p;
        this.meta = ODMeta.create(this);
        this.setNumero(doc.numero);
    }

    @Override
    public ODSingleXMLDocument clone() {
        final ODPackage copy = new ODPackage(this.pkg);
        return (ODSingleXMLDocument) copy.getContent();
    }

    private void setNumero(int numero) {
        this.numero = numero;
        this.meta.getUserMeta(COUNT, true).setValue(this.numero);
    }

    /**
     * The number of files concatenated with {@link #add(ODSingleXMLDocument)}.
     * 
     * @return number of files concatenated.
     */
    public final int getNumero() {
        return this.numero;
    }

    public ODPackage getPackage() {
        return this.pkg;
    }

    /**
     * Append a document.
     * 
     * @param doc the document to add.
     */
    public synchronized void add(ODSingleXMLDocument doc) {
        // ajoute un saut de page entre chaque document
        this.add(doc, true);
    }

    /**
     * Append a document.
     * 
     * @param doc the document to add, <code>null</code> means no-op.
     * @param pageBreak whether a page break should be inserted before <code>doc</code>.
     */
    public synchronized void add(ODSingleXMLDocument doc, boolean pageBreak) {
        if (doc != null && pageBreak)
            // only add a page break, if a page was really added
            this.getBody().addContent(this.getPageBreak());
        this.add(null, 0, doc);
    }

    public synchronized void replace(Element elem, ODSingleXMLDocument doc) {
        final Element parent = elem.getParentElement();
        this.add(parent, parent.indexOf(elem), doc);
        elem.detach();
    }

    public synchronized void add(Element where, int index, ODSingleXMLDocument doc) {
        if (doc == null)
            return;
        if (!this.getVersion().equals(doc.getVersion()))
            throw new IllegalArgumentException("version mismatch");

        this.setNumero(this.numero + 1);
        try {
            copyNS(doc.getDocument(), this.getDocument());
            this.mergeEmbedded(doc);
            this.mergeSettings(doc);
            this.mergeAllStyles(doc, false);
            this.mergeBody(where, index, doc);
        } catch (JDOMException exn) {
            throw new IllegalArgumentException("XML error", exn);
        }
    }

    /**
     * Merge the four elements of style.
     * 
     * @param doc the xml document to merge.
     * @param sameDoc whether <code>doc</code> is the same OpenDocument than this, eg
     *        <code>true</code> when merging content.xml and styles.xml.
     * @throws JDOMException if an error occurs.
     */
    private void mergeAllStyles(ODXMLDocument doc, boolean sameDoc) throws JDOMException {
        // no reference
        this.mergeFontDecls(doc);
        // section 14.1
        // § Parent Style only refer to other common styles
        // § Next Style cannot refer to an autostyle (only available in common styles)
        // § List Style can refer to an autostyle
        // § Master Page Name cannot (auto master pages does not exist)
        // § Data Style Name (for cells) can
        // but since the UI for common styles doesn't allow to customize List Style
        // and there is no common styles for tables : office:styles doesn't reference any automatic
        // styles
        this.mergeStyles(doc);
        // on the contrary autostyles do refer to other autostyles :
        // choosing "activate bullets" will create an automatic paragraph style:style
        // referencing an automatic text:list-style.
        this.mergeAutoStyles(doc, !sameDoc);
        // section 14.4
        // § Page Layout can refer to an autostyle
        // § Next Style Name refer to another masterPage
        this.mergeMasterStyles(doc, !sameDoc);
    }

    private void mergeEmbedded(ODSingleXMLDocument doc) {
        // since we are adding another document our existing thumbnail is obsolete
        this.pkg.rmFile("Thumbnails/thumbnail.png");
        // copy the files
        final ODPackage opkg = CopyUtils.copy(doc.pkg);
        for (final String name : opkg.getEntries()) {
            final ODPackageEntry e = opkg.getEntry(name);
            if (!ODPackage.isStandardFile(e.getName())) {
                this.pkg.putFile(this.prefix(e.getName()), e.getData(), e.getType());
            }
        }
    }

    private void mergeSettings(ODSingleXMLDocument doc) throws JDOMException {
        this.addIfNotPresent(doc, "./office:settings", 0);
    }

    /**
     * Fusionne les office:font-decls/style:font-decl. On ne préfixe jamais, on ajoute seulement si
     * l'attribut style:name est différent.
     * 
     * @param doc le document à fusionner avec celui-ci.
     * @throws JDOMException
     */
    private void mergeFontDecls(ODXMLDocument doc) throws JDOMException {
        final String[] fontDecls = this.getFontDecls();
        this.mergeUnique(doc, fontDecls[0], fontDecls[1]);
    }

    private String[] getFontDecls() {
        return getXML().getFontDecls();
    }

    // merge everything under office:styles
    private void mergeStyles(ODXMLDocument doc) throws JDOMException {
        // les default-style (notamment tab-stop-distance)
        this.mergeUnique(doc, "styles", "style:default-style", "style:family", NOP_ElementTransformer);
        // les styles
        this.stylesNames.addAll(this.mergeUnique(doc, "styles", "style:style"));
        // on ajoute outline-style si non présent
        this.addStylesIfNotPresent(doc, "outline-style");
        // les list-style
        this.listStylesNames.addAll(this.mergeUnique(doc, "styles", "text:list-style"));
        // les *notes-configuration
        if (getVersion() == XMLVersion.OOo) {
            this.addStylesIfNotPresent(doc, "footnotes-configuration");
            this.addStylesIfNotPresent(doc, "endnotes-configuration");
        } else {
            // 16.29.3 : specifies values for each note class used in a document
            this.mergeUnique(doc, "styles", "text:notes-configuration", "text:note-class", NOP_ElementTransformer);
        }
        this.addStylesIfNotPresent(doc, "bibliography-configuration");
        this.addStylesIfNotPresent(doc, "linenumbering-configuration");
    }

    /**
     * Fusionne les office:automatic-styles, on préfixe tout.
     * 
     * @param doc le document à fusionner avec celui-ci.
     * @param ref whether to prefix hrefs.
     * @throws JDOMException
     */
    private void mergeAutoStyles(ODXMLDocument doc, boolean ref) throws JDOMException {
        final List<Element> addedStyles = this.prefixAndAddAutoStyles(doc);
        for (final Element addedStyle : addedStyles) {
            this.prefix(addedStyle, ref);
        }
    }

    /**
     * Fusionne les office:master-styles. On ne préfixe jamais, on ajoute seulement si l'attribut
     * style:name est différent.
     * 
     * @param doc le document à fusionner avec celui-ci.
     * @param ref whether to prefix hrefs.
     * @throws JDOMException if an error occurs.
     */
    private void mergeMasterStyles(ODXMLDocument doc, boolean ref) throws JDOMException {
        // est référencé dans les styles avec "style:master-page-name"
        this.mergeUnique(doc, "master-styles", "style:master-page", ref ? this.prefixTransf : this.prefixTransfNoRef);
    }

    /**
     * Fusionne les corps.
     * 
     * @param doc le document à fusionner avec celui-ci.
     * @throws JDOMException
     */
    private void mergeBody(Element where, int index, ODSingleXMLDocument doc) throws JDOMException {
        // copy forms from doc to this
        final String formsName = "forms";
        final Namespace formsNS = getVersion().getOFFICE();
        final String bodyPath = this.getContentTypeVersioned().getBodyPath();
        this.add(new IFactory<Element>() {
            @Override
            public Element createChecked() {
                final Element ourForms = getBody().getChild(formsName, formsNS);
                if (ourForms != null) {
                    return ourForms;
                } else {
                    final Element res = new Element(formsName, formsNS);
                    // forms should be the first child of the body
                    getBody().addContent(0, res);
                    return res;
                }
            }
        }, -1, doc, bodyPath + "/" + formsNS.getPrefix() + ":" + formsName, this.prefixTransf);
        this.add(where, index, doc, bodyPath, new ElementTransformer() {
            public Element transform(Element elem) throws JDOMException {
                // ATTN n'ajoute pas sequence-decls
                // forms already added above
                if (elem.getName().equals("sequence-decls") || (elem.getName().equals(formsName) && elem.getNamespace().equals(formsNS)))
                    return null;

                if (elem.getName().equals("user-field-decls")) {
                    // user fields are global to a document, they do not vary across it.
                    // hence they are initialized at declaration
                    // we should assure that there's no 2 declaration with the same name
                    detachDuplicate(elem);
                }

                if (elem.getName().equals("variable-decls")) {
                    // variables are not initialized at declaration
                    // we should still assure that there's no 2 declaration with the same name
                    detachDuplicate(elem);
                }

                // par défaut
                return ODSingleXMLDocument.this.prefixTransf.transform(elem);
            }
        });
    }

    /**
     * Detach the children of elem whose names already exist in the body.
     * 
     * @param elem the elem to be trimmed.
     * @throws JDOMException if an error occurs.
     */
    protected final void detachDuplicate(Element elem) throws JDOMException {
        final String singularName = elem.getName().substring(0, elem.getName().length() - 1);
        final List thisNames = getXPath("./text:" + singularName + "s/text:" + singularName + "/@text:name").selectNodes(getChild("body"));
        CollectionUtils.transform(thisNames, new Transformer() {
            public Object transform(Object obj) {
                return ((Attribute) obj).getValue();
            }
        });

        final Iterator iter = elem.getChildren().iterator();
        while (iter.hasNext()) {
            final Element decl = (Element) iter.next();
            if (thisNames.contains(decl.getAttributeValue("name", getVersion().getTEXT()))) {
                // on retire les déjà existant
                iter.remove();
            }
        }
    }

    // *** Utils

    public final Element getBody() {
        return this.getContentTypeVersioned().getBody(getDocument());
    }

    private ContentTypeVersioned getContentTypeVersioned() {
        return ContentType.TEXT.getVersioned(getVersion());
    }

    /**
     * Préfixe les attributs en ayant besoin.
     * 
     * @param elem l'élément à préfixer.
     * @param references whether to prefix hrefs.
     * @throws JDOMException if an error occurs.
     */
    void prefix(Element elem, boolean references) throws JDOMException {
        Iterator attrs = this.getXPath(".//@text:style-name | .//@table:style-name | .//@draw:style-name | .//@style:data-style-name").selectNodes(elem).iterator();
        while (attrs.hasNext()) {
            Attribute attr = (Attribute) attrs.next();
            // text:list/@text:style-name references text:list-style
            if (!this.listStylesNames.contains(attr.getValue()) && !this.stylesNames.contains(attr.getValue())) {
                attr.setValue(this.prefix(attr.getValue()));
            }
        }

        attrs = this.getXPath(".//@style:list-style-name").selectNodes(elem).iterator();
        while (attrs.hasNext()) {
            Attribute attr = (Attribute) attrs.next();
            if (!this.listStylesNames.contains(attr.getValue())) {
                attr.setValue(this.prefix(attr.getValue()));
            }
        }

        attrs = this.getXPath(".//@style:page-master-name | .//@style:page-layout-name | .//@text:name | .//@form:name | .//@form:property-name").selectNodes(elem).iterator();
        while (attrs.hasNext()) {
            final Attribute attr = (Attribute) attrs.next();
            final String parentName = attr.getParent().getName();
            if (!DONT_PREFIX.contains(parentName))
                attr.setValue(this.prefix(attr.getValue()));
        }

        // prefix references
        if (references) {
            attrs = this.getXPath(".//@xlink:href[../@xlink:show='embed']").selectNodes(elem).iterator();
            while (attrs.hasNext()) {
                final Attribute attr = (Attribute) attrs.next();
                final String prefixedPath = this.prefixPath(attr.getValue());
                if (prefixedPath != null)
                    attr.setValue(prefixedPath);
            }
        }
    }

    /**
     * Prefix a path.
     * 
     * @param href a path inside the pkg, eg "./Object 1/content.xml".
     * @return the prefixed path or <code>null</code> if href is external, eg "./3_Object
     *         1/content.xml".
     */
    private String prefixPath(final String href) {
        if (this.getVersion().equals(XMLVersion.OOo)) {
            // in OOo 1.x inPKG is denoted by a #
            final boolean sharp = href.startsWith("#");
            if (sharp)
                // eg #Pictures/100000000000006C000000ABCC02339E.png
                return "#" + this.prefix(href.substring(1));
            else
                // eg ../../../../Program%20Files/OpenOffice.org1.1.5/share/gallery/apples.gif
                return null;
        } else {
            URI uri;
            try {
                uri = new URI(href);
            } catch (URISyntaxException e) {
                // OO doesn't escape characters for files
                uri = null;
            }
            // section 17.5
            final boolean inPKGFile = uri == null || uri.getScheme() == null && uri.getAuthority() == null && uri.getPath().charAt(0) != '/';
            if (inPKGFile) {
                final String dotSlash = "./";
                if (href.startsWith(dotSlash))
                    return dotSlash + this.prefix(href.substring(dotSlash.length()));
                else
                    return this.prefix(href);
            } else
                return null;
        }
    }

    private String prefix(String value) {
        return "_" + this.numero + value;
    }

    private final ElementTransformer prefixTransf = new ElementTransformer() {
        public Element transform(Element elem) throws JDOMException {
            ODSingleXMLDocument.this.prefix(elem, true);
            return elem;
        }
    };

    private final ElementTransformer prefixTransfNoRef = new ElementTransformer() {
        public Element transform(Element elem) throws JDOMException {
            ODSingleXMLDocument.this.prefix(elem, false);
            return elem;
        }
    };

    /**
     * Ajoute dans ce document seulement les éléments de doc correspondant au XPath spécifié et dont
     * la valeur de l'attribut style:name n'existe pas déjà.
     * 
     * @param doc le document à fusionner avec celui-ci.
     * @param topElem eg "office:font-decls".
     * @param elemToMerge les éléments à fusionner (par rapport à topElem), eg "style:font-decl".
     * @return les noms des éléments ajoutés.
     * @throws JDOMException
     * @see #mergeUnique(ODSingleXMLDocument, String, String, ElementTransformer)
     */
    private List<String> mergeUnique(ODXMLDocument doc, String topElem, String elemToMerge) throws JDOMException {
        return this.mergeUnique(doc, topElem, elemToMerge, NOP_ElementTransformer);
    }

    /**
     * Ajoute dans ce document seulement les éléments de doc correspondant au XPath spécifié et dont
     * la valeur de l'attribut style:name n'existe pas déjà. En conséquence n'ajoute que les
     * éléments possédant un attribut style:name.
     * 
     * @param doc le document à fusionner avec celui-ci.
     * @param topElem eg "office:font-decls".
     * @param elemToMerge les éléments à fusionner (par rapport à topElem), eg "style:font-decl".
     * @param addTransf la transformation à appliquer avant d'ajouter.
     * @return les noms des éléments ajoutés.
     * @throws JDOMException
     */
    private List<String> mergeUnique(ODXMLDocument doc, String topElem, String elemToMerge, ElementTransformer addTransf) throws JDOMException {
        return this.mergeUnique(doc, topElem, elemToMerge, "style:name", addTransf);
    }

    private List<String> mergeUnique(ODXMLDocument doc, String topElem, String elemToMerge, String attrFQName, ElementTransformer addTransf) throws JDOMException {
        List<String> added = new ArrayList<String>();
        Element thisParent = this.getChild(topElem, true);

        XPath xp = this.getXPath("./" + elemToMerge + "/@" + attrFQName);

        // les styles de ce document
        List thisElemNames = xp.selectNodes(thisParent);
        // on transforme la liste d'attributs en liste de String
        CollectionUtils.transform(thisElemNames, new Transformer() {
            public Object transform(Object obj) {
                return ((Attribute) obj).getValue();
            }
        });

        // pour chaque style de l'autre document
        Iterator otherElemNames = xp.selectNodes(doc.getChild(topElem)).iterator();
        while (otherElemNames.hasNext()) {
            Attribute attr = (Attribute) otherElemNames.next();
            // on l'ajoute si non déjà dedans
            if (!thisElemNames.contains(attr.getValue())) {
                thisParent.addContent(addTransf.transform((Element) attr.getParent().clone()));
                added.add(attr.getValue());
            }
        }

        return added;
    }

    /**
     * Ajoute l'élément elemName de doc, s'il n'est pas dans ce document.
     * 
     * @param doc le document à fusionner avec celui-ci.
     * @param elemName l'élément à ajouter, eg "outline-style".
     * @throws JDOMException if elemName is not valid.
     */
    private void addStylesIfNotPresent(ODXMLDocument doc, String elemName) throws JDOMException {
        this.addIfNotPresent(doc, "./office:styles/text:" + elemName);
    }

    /**
     * Prefixe les fils de auto-styles possédant un attribut "name" avant de les ajouter.
     * 
     * @param doc le document à fusionner avec celui-ci.
     * @return les élément ayant été ajoutés.
     * @throws JDOMException
     */
    private List<Element> prefixAndAddAutoStyles(ODXMLDocument doc) throws JDOMException {
        final List<Element> result = new ArrayList<Element>(128);
        final List otherNames = this.getXPath("./*/@style:name").selectNodes(doc.getChild("automatic-styles"));
        Iterator iter = otherNames.iterator();
        while (iter.hasNext()) {
            Attribute attr = (Attribute) iter.next();
            Element parent = (Element) attr.getParent().clone();
            parent.setAttribute("name", this.prefix(attr.getValue()), this.getVersion().getSTYLE());
            this.getChild("automatic-styles").addContent(parent);
            result.add(parent);
        }
        return result;
    }

    /**
     * Return <code>true</code> if this document was split.
     * 
     * @return <code>true</code> if this has no package anymore.
     * @see ODPackage#split()
     */
    public final boolean isDead() {
        return this.getPackage() == null;
    }

    final Map<RootElement, Document> split() {
        final Map<RootElement, Document> res = new HashMap<RootElement, Document>();
        final XMLVersion version = getVersion();
        final Element root = this.getDocument().getRootElement();
        final XMLFormatVersion officeVersion = getFormatVersion();

        // meta
        {
            final Element thisMeta = root.getChild("meta", version.getOFFICE());
            if (thisMeta != null) {
                final Document meta = createDocument(res, RootElement.META, officeVersion);
                meta.getRootElement().addContent(thisMeta.detach());
            }
        }
        // settings
        {
            final Element thisSettings = root.getChild("settings", version.getOFFICE());
            if (thisSettings != null) {
                final Document settings = createDocument(res, RootElement.SETTINGS, officeVersion);
                settings.getRootElement().addContent(thisSettings.detach());
            }
        }
        // styles
        // we must move office:styles, office:master-styles and referenced office:automatic-styles
        {
            final Document styles = createDocument(res, RootElement.STYLES, officeVersion);
            // don't bother finding out which font is used where since there isn't that many of them
            styles.getRootElement().addContent((Element) root.getChild(getFontDecls()[0], version.getOFFICE()).clone());
            // extract common styles
            styles.getRootElement().addContent(root.getChild("styles", version.getOFFICE()).detach());
            // only automatic styles used in the styles themselves.
            final Element contentAutoStyles = root.getChild("automatic-styles", version.getOFFICE());
            final Element stylesAutoStyles = new Element(contentAutoStyles.getName(), contentAutoStyles.getNamespace());
            final Element masterStyles = root.getChild("master-styles", version.getOFFICE());

            // style elements referenced, e.g. <style:page-layout style:name="pm1">
            final Set<Element> referenced = new HashSet<Element>();

            final SimpleXMLPath<Attribute> descAttrs = SimpleXMLPath.create(Step.createElementStep(Axis.descendantOrSelf, null), Step.createAttributeStep(null, null));
            for (final Attribute attr : descAttrs.selectNodes(masterStyles)) {
                final Element referencedStyleElement = Style.getReferencedStyleElement(this.pkg, attr);
                if (referencedStyleElement != null)
                    referenced.add(referencedStyleElement);
            }
            for (final Element r : referenced) {
                // since we already removed common styles
                assert r.getParentElement() == contentAutoStyles;
                stylesAutoStyles.addContent(r.detach());
            }

            styles.getRootElement().addContent(stylesAutoStyles);
            styles.getRootElement().addContent(masterStyles.detach());
        }
        // content
        {
            this.pkg = null;
            final Document content = createDocument(res, RootElement.CONTENT, officeVersion);
            getContentTypeVersioned().setType(content);
            content.getRootElement().addContent(root.removeContent());
        }
        return res;
    }

    private Document createDocument(final Map<RootElement, Document> res, RootElement rootElement, final XMLFormatVersion version) {
        final Document doc = rootElement.createDocument(version);
        copyNS(this.getDocument(), doc);
        res.put(rootElement, doc);
        return doc;
    }

    /**
     * Saves this OO document to a file.
     * 
     * @param f the file where this document will be saved, without extension, eg "dir/myfile".
     * @return the actual file where it has been saved (with extension), eg "dir/myfile.odt".
     * @throws IOException if an error occurs.
     */
    public File saveAs(File f) throws IOException {
        return this.pkg.saveAs(f);
    }

    private Element getPageBreak() {
        if (this.pageBreak == null) {
            final String styleName = "PageBreak";
            try {
                final XPath xp = this.getXPath("./style:style[@style:name='" + styleName + "']");
                final Element styles = this.getChild("styles", true);
                if (xp.selectSingleNode(styles) == null) {
                    final Element pageBreakStyle = new Element("style", this.getVersion().getSTYLE());
                    pageBreakStyle.setAttribute("name", styleName, this.getVersion().getSTYLE());
                    pageBreakStyle.setAttribute("family", "paragraph", this.getVersion().getSTYLE());
                    pageBreakStyle.setContent(getPProps().setAttribute("break-after", "page", this.getVersion().getNS("fo")));
                    // <element name="office:styles"> <interleave>...
                    // so just append the new style
                    styles.addContent(pageBreakStyle);
                }
            } catch (JDOMException e) {
                // static path, shouldn't happen
                throw new IllegalStateException("pb while searching for " + styleName, e);
            }
            this.pageBreak = new Element("p", this.getVersion().getTEXT()).setAttribute("style-name", styleName, this.getVersion().getTEXT());
        }
        return (Element) this.pageBreak.clone();
    }

    private final Element getPProps() {
        return this.getXML().createFormattingProperties("paragraph");
    }
}
