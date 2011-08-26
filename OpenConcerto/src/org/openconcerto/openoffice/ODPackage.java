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
import static org.openconcerto.openoffice.ODPackage.RootElement.META;
import static org.openconcerto.openoffice.ODPackage.RootElement.SETTINGS;
import static org.openconcerto.openoffice.ODPackage.RootElement.STYLES;
import org.openconcerto.openoffice.text.ParagraphStyle;
import org.openconcerto.utils.CopyUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.StreamUtils;
import org.openconcerto.utils.StringInputStream;
import org.openconcerto.utils.Zip;
import org.openconcerto.utils.ZippedFilesProcessor;
import org.openconcerto.xml.Validator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;

import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * An OpenDocument package, ie a zip containing XML documents and their associated files.
 * 
 * @author ILM Informatique 2 août 2004
 */
public class ODPackage {

    // use raw format, otherwise spaces are added to every spreadsheet cell
    private static final XMLOutputter OUTPUTTER = new XMLOutputter(Format.getRawFormat());
    /** Normally mimetype contains only ASCII characters */
    static final Charset MIMETYPE_ENC = Charset.forName("UTF-8");

    /**
     * Root element of an OpenDocument document. See section 22.2.1 of v1.2-part1-cd04.
     * 
     * @author Sylvain CUAZ
     */
    public static enum RootElement {
        /** Contains the entire document, see 3.1.2 of OpenDocument-v1.2-part1-cd04 */
        SINGLE_CONTENT("office", "document", null),
        /** Document content and automatic styles used in the content, see 3.1.3.2 */
        CONTENT("office", "document-content", "content.xml"),
        // TODO uncomment and create ContentTypeVersioned for .odf and .otf, see 22.2.9 Conforming
        // OpenDocument Formula Document
        // MATH("math", "math", "content.xml"),
        /** Styles used in document content and automatic styles used in styles, see 3.1.3.3 */
        STYLES("office", "document-styles", "styles.xml"),
        /** Document metadata elements, see 3.1.3.4 */
        META("office", "document-meta", "meta.xml"),
        /** Implementation-specific settings, see 3.1.3.5 */
        SETTINGS("office", "document-settings", "settings.xml");

        public final static EnumSet<RootElement> getPackageElements() {
            return EnumSet.of(CONTENT, STYLES, META, SETTINGS);
        }

        public final static RootElement fromElementName(final String name) {
            for (final RootElement e : values()) {
                if (e.getElementName().equals(name))
                    return e;
            }
            return null;
        }

        static final Document createSingle(final Document from) {
            final XMLFormatVersion version = XMLFormatVersion.get(from);
            return SINGLE_CONTENT.createDocument(version.getXMLVersion(), version.getOfficeVersion());
        }

        private final String nsPrefix;
        private final String name;
        private final String zipEntry;

        private RootElement(String prefix, String rootName, String zipEntry) {
            this.nsPrefix = prefix;
            this.name = rootName;
            this.zipEntry = zipEntry;
        }

        public final String getElementNSPrefix() {
            return this.nsPrefix;
        }

        public final String getElementName() {
            return this.name;
        }

        public final Document createDocument(final XMLVersion version, final String officeVersion) {
            final Element root = new Element(getElementName(), version.getNS(getElementNSPrefix()));
            // 19.388 office:version identifies the version of ODF specification
            if (officeVersion != null)
                root.setAttribute("version", officeVersion, version.getOFFICE());
            // avoid declaring namespaces in each child
            for (final Namespace ns : version.getALL())
                root.addNamespaceDeclaration(ns);

            final Document res = new Document(root);
            // OpenDocument use relaxNG
            if (version == XMLVersion.OOo)
                res.setDocType(new DocType(getElementNSPrefix() + ":" + getElementName(), "-//OpenOffice.org//DTD OfficeDocument 1.0//EN", "office.dtd"));
            return res;
        }

        /**
         * The name of the zip entry in the package.
         * 
         * @return the path of the file, <code>null</code> if this element shouldn't be in a
         *         package.
         */
        public final String getZipEntry() {
            return this.zipEntry;
        }
    }

    private static final Set<String> subdocNames;
    static {
        subdocNames = new HashSet<String>();
        for (final RootElement r : RootElement.getPackageElements())
            if (r.getZipEntry() != null)
                subdocNames.add(r.getZipEntry());
    }

    /**
     * Whether the passed entry is specific to a package.
     * 
     * @param name a entry name, eg "mimetype"
     * @return <code>true</code> if <code>name</code> is a standard file, eg <code>true</code>.
     */
    public static final boolean isStandardFile(final String name) {
        return name.equals("mimetype") || subdocNames.contains(name) || name.startsWith("Thumbnails") || name.startsWith("META-INF") || name.startsWith("Configurations");
    }

    private final Map<String, ODPackageEntry> files;
    private ContentTypeVersioned type;
    private File file;

    public ODPackage() {
        this.files = new HashMap<String, ODPackageEntry>();
        this.type = null;
        this.file = null;
    }

    public ODPackage(InputStream ins) throws IOException {
        this();

        final ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        new ZippedFilesProcessor() {
            @Override
            protected void processEntry(ZipEntry entry, InputStream in) throws IOException {
                final String name = entry.getName();
                final Object res;
                if (subdocNames.contains(name)) {
                    try {
                        res = new ODXMLDocument(OOUtils.getBuilder().build(in));
                    } catch (JDOMException e) {
                        // always correct
                        throw new IllegalStateException("parse error", e);
                    }
                } else {
                    out.reset();
                    StreamUtils.copy(in, out);
                    res = out.toByteArray();
                }
                // we don't know yet the types
                putFile(name, res, null, entry.getMethod() == ZipEntry.DEFLATED);
            }
        }.process(ins);
        // fill in the missing types from the manifest, if any
        final ODPackageEntry me = this.files.remove(Manifest.ENTRY_NAME);
        if (me != null) {
            final byte[] m = (byte[]) me.getData();
            try {
                final Map<String, String> manifestEntries = Manifest.parse(new ByteArrayInputStream(m));
                for (final Map.Entry<String, String> e : manifestEntries.entrySet()) {
                    final String path = e.getKey();
                    final ODPackageEntry entry = this.files.get(path);
                    // eg directory
                    if (entry == null)
                        this.files.put(path, new ODPackageEntry(path, e.getValue(), null));
                    else
                        entry.setType(e.getValue());
                }
            } catch (JDOMException e) {
                throw new IllegalArgumentException("bad manifest " + new String(m), e);
            }
        }
    }

    public ODPackage(File f) throws IOException {
        this(new BufferedInputStream(new FileInputStream(f), 512 * 1024));
        this.file = f;
    }

    public ODPackage(ODPackage o) {
        this();
        // ATTN this works because, all files are read upfront
        for (final String name : o.getEntries()) {
            final ODPackageEntry entry = o.getEntry(name);
            final Object data = entry.getData();
            final Object myData;
            if (data instanceof byte[])
                // assume byte[] are immutable
                myData = data;
            else if (data instanceof ODSingleXMLDocument) {
                myData = new ODSingleXMLDocument((ODSingleXMLDocument) data, this);
            } else {
                myData = CopyUtils.copy(data);
            }
            this.putFile(name, myData, entry.getType(), entry.isCompressed());
        }
        this.type = o.type;
        this.file = o.file;
    }

    public final File getFile() {
        return this.file;
    }

    public final void setFile(File f) {
        this.file = this.addExt(f);
    }

    private final File addExt(File f) {
        final String ext = '.' + this.getContentType().getExtension();
        if (!f.getName().endsWith(ext))
            f = new File(f.getParentFile(), f.getName() + ext);
        return f;
    }

    /**
     * The version of this package, <code>null</code> if it cannot be found (eg this package is
     * empty, or contains no xml).
     * 
     * @return the version of this package, can be <code>null</code>.
     */
    public final XMLVersion getVersion() {
        final XMLFormatVersion res = getFormatVersion();
        return res == null ? null : res.getXMLVersion();
    }

    public final XMLFormatVersion getFormatVersion() {
        final ODXMLDocument content = this.getContent();
        if (content == null)
            return null;
        else
            return content.getFormatVersion();
    }

    /**
     * The type of this package, <code>null</code> if it cannot be found (eg this package is empty).
     * 
     * @return the type of this package, can be <code>null</code>.
     */
    public final ContentTypeVersioned getContentType() {
        if (this.type == null) {
            if (this.files.containsKey("mimetype"))
                this.type = ContentTypeVersioned.fromMime(new String(this.getBinaryFile("mimetype"), MIMETYPE_ENC));
            else if (this.getVersion().equals(XMLVersion.OOo)) {
                final Element contentRoot = this.getContent().getDocument().getRootElement();
                final String docClass = contentRoot.getAttributeValue("class", contentRoot.getNamespace("office"));
                this.type = ContentTypeVersioned.fromClass(docClass);
            } else if (this.getVersion().equals(XMLVersion.OD)) {
                final Element bodyChild = (Element) this.getContent().getChild("body").getChildren().get(0);
                this.type = ContentTypeVersioned.fromBody(bodyChild.getName());
            }
        }
        return this.type;
    }

    public final String getMimeType() {
        return this.getContentType().getMimeType();
    }

    /**
     * Call {@link Validator#isValid()} on each XML subdocuments.
     * 
     * @return all problems indexed by subdocuments names, i.e. empty if all OK, <code>null</code>
     *         if validation couldn't occur.
     */
    public final Map<String, String> validateSubDocuments() {
        final OOXML ooxml = this.getFormatVersion().getXML();
        if (!ooxml.canValidate())
            return null;
        final Map<String, String> res = new HashMap<String, String>();
        for (final String s : subdocNames) {
            if (this.getEntries().contains(s)) {
                final String valid = ooxml.getValidator(this.getDocument(s)).isValid();
                if (valid != null)
                    res.put(s, valid);
            }
        }
        return res;
    }

    // *** getter on files

    public final Set<String> getEntries() {
        return this.files.keySet();
    }

    public final ODPackageEntry getEntry(String entry) {
        return this.files.get(entry);
    }

    protected final Object getData(String entry) {
        final ODPackageEntry e = this.getEntry(entry);
        return e == null ? null : e.getData();
    }

    public final byte[] getBinaryFile(String entry) {
        return (byte[]) this.getData(entry);
    }

    public final ODXMLDocument getXMLFile(String xmlEntry) {
        return (ODXMLDocument) this.getData(xmlEntry);
    }

    public final ODXMLDocument getXMLFile(final Document doc) {
        for (final String s : subdocNames) {
            final ODXMLDocument xmlFile = getXMLFile(s);
            if (xmlFile != null && xmlFile.getDocument() == doc) {
                return xmlFile;
            }
        }
        return null;
    }

    public final ODXMLDocument getContent() {
        return this.getXMLFile(CONTENT.getZipEntry());
    }

    public final ODMeta getMeta() {
        final ODMeta meta;
        if (this.getEntries().contains(META.getZipEntry()))
            meta = ODMeta.create(this.getXMLFile(META.getZipEntry()));
        else
            meta = ODMeta.create(this.getContent());
        return meta;
    }

    /**
     * Return an XML document.
     * 
     * @param xmlEntry the filename, eg "styles.xml".
     * @return the matching document, or <code>null</code> if there's none.
     * @throws JDOMException if error about the XML.
     * @throws IOException if an error occurs while reading the file.
     */
    public Document getDocument(String xmlEntry) {
        final ODXMLDocument xml = this.getXMLFile(xmlEntry);
        return xml == null ? null : xml.getDocument();
    }

    /**
     * Find the passed automatic or common style referenced from the content.
     * 
     * @param desc the family, eg {@link ParagraphStyle#DESC}.
     * @param name the name, eg "P1".
     * @return the corresponding XML element.
     */
    public final Element getStyle(final StyleDesc<?> desc, final String name) {
        return this.getStyle(this.getContent().getDocument(), desc, name);
    }

    /**
     * Find the passed automatic or common style. NOTE : <code>referent</code> is needed because
     * there can exist automatic styles with the same name in both "content.xml" and "styles.xml".
     * 
     * @param referent the document referencing the style.
     * @param desc the family, eg {@link ParagraphStyle#DESC}.
     * @param name the name, eg "P1".
     * @return the corresponding XML element.
     * @see ODXMLDocument#getStyle(StyleDesc, String)
     */
    public final Element getStyle(final Document referent, final StyleDesc<?> desc, final String name) {
        // avoid searching in content then styles if it cannot be found
        if (name == null)
            return null;

        String refSubDoc = null;
        final String[] stylesContainer = new String[] { CONTENT.getZipEntry(), STYLES.getZipEntry() };
        for (final String subDoc : stylesContainer)
            if (this.getDocument(subDoc) == referent)
                refSubDoc = subDoc;
        if (refSubDoc == null)
            throw new IllegalArgumentException("neither in content nor styles : " + referent);

        Element res = this.getXMLFile(refSubDoc).getStyle(desc, name);
        // if it isn't in content.xml it might be in styles.xml
        if (res == null && refSubDoc.equals(stylesContainer[0]) && this.getXMLFile(stylesContainer[1]) != null)
            res = this.getXMLFile(stylesContainer[1]).getStyle(desc, name);
        return res;
    }

    // *** setter

    public void putFile(String entry, Object data) {
        this.putFile(entry, data, null);
    }

    public void putFile(final String entry, final Object data, final String mediaType) {
        this.putFile(entry, data, mediaType, true);
    }

    public void putFile(final String entry, final Object data, final String mediaType, final boolean compress) {
        if (entry == null)
            throw new NullPointerException("null name");
        final Object myData;
        if (subdocNames.contains(entry)) {
            final ODXMLDocument oodoc;
            if (data instanceof Document)
                oodoc = new ODXMLDocument((Document) data);
            else
                oodoc = (ODXMLDocument) data;
            // si le package est vide n'importe quelle version convient
            if (this.getVersion() != null && !oodoc.getVersion().equals(this.getVersion()))
                throw new IllegalArgumentException("version mismatch " + this.getVersion() + " != " + oodoc);
            myData = oodoc;
        } else if (data != null && !(data instanceof byte[]))
            throw new IllegalArgumentException("should be byte[] for " + entry + ": " + data);
        else
            myData = data;
        final String inferredType = mediaType != null ? mediaType : FileUtils.findMimeType(entry);
        this.files.put(entry, new ODPackageEntry(entry, inferredType, myData, compress));
    }

    public void rmFile(String entry) {
        this.files.remove(entry);
    }

    /**
     * Transform this to use a {@link ODSingleXMLDocument}. Ie after this method, only "content.xml"
     * remains and it's an instance of ODSingleXMLDocument.
     * 
     * @return the created ODSingleXMLDocument.
     */
    public ODSingleXMLDocument toSingle() {
        if (!this.isSingle()) {
            // this removes xml files used by OOSingleXMLDocument
            final Document content = removeAndGetDoc(CONTENT.getZipEntry());
            final Document styles = removeAndGetDoc(STYLES.getZipEntry());
            final Document settings = removeAndGetDoc(SETTINGS.getZipEntry());
            final Document meta = removeAndGetDoc(META.getZipEntry());

            return ODSingleXMLDocument.createFromDocument(content, styles, settings, meta, this);
        } else
            return (ODSingleXMLDocument) this.getContent();
    }

    public final boolean isSingle() {
        return this.getContent() instanceof ODSingleXMLDocument;
    }

    private Document removeAndGetDoc(String name) {
        if (!this.files.containsKey(name))
            return null;
        final ODXMLDocument xmlDoc = (ODXMLDocument) this.files.remove(name).getData();
        return xmlDoc == null ? null : xmlDoc.getDocument();
    }

    /**
     * Split the {@link RootElement#SINGLE_CONTENT}. If this was {@link #isSingle() single} the
     * former {@link #getContent() content} won't be useable anymore, you can check it with
     * {@link ODSingleXMLDocument#isDead()}.
     * 
     * @return <code>true</code> if this was modified.
     */
    public final boolean split() {
        final boolean res;
        if (this.isSingle()) {
            final Map<RootElement, Document> split = ((ODSingleXMLDocument) this.getContent()).split();
            // from 22.2.1 (D1.1.2) of OpenDocument-v1.2-part1-cd04
            assert (split.containsKey(RootElement.CONTENT) || split.containsKey(RootElement.STYLES)) && RootElement.getPackageElements().containsAll(split.keySet()) : "wrong elements " + split;
            final XMLFormatVersion version = getFormatVersion();
            for (final Entry<RootElement, Document> e : split.entrySet()) {
                this.putFile(e.getKey().getZipEntry(), new ODXMLDocument(e.getValue(), version));
            }
            res = true;
        } else {
            res = false;
        }
        assert !this.isSingle();
        return res;
    }

    // *** save

    public final void save(OutputStream out) throws IOException {
        // from 22.2.1 (D1.2)
        if (this.isSingle()) {
            // assert we can use this copy constructor (instead of the slower CopyUtils)
            assert this.getClass() == ODPackage.class;
            final ODPackage copy = new ODPackage(this);
            copy.split();
            copy.save(out);
            return;
        }

        final Zip z = new Zip(out);

        // magic number, see section 17.4
        z.zipNonCompressed("mimetype", this.getMimeType().getBytes(MIMETYPE_ENC));

        final Manifest manifest = new Manifest(this.getVersion(), this.getMimeType());
        for (final String name : this.files.keySet()) {
            // added at the end
            if (name.equals("mimetype") || name.equals(Manifest.ENTRY_NAME))
                continue;

            final ODPackageEntry entry = this.files.get(name);
            final Object val = entry.getData();
            if (val != null) {
                if (val instanceof ODXMLDocument) {
                    final OutputStream o = z.createEntry(name);
                    OUTPUTTER.output(((ODXMLDocument) val).getDocument(), o);
                    o.close();
                } else {
                    z.zip(name, (byte[]) val, entry.isCompressed());
                }
            }
            final String mediaType = entry.getType();
            manifest.addEntry(name, mediaType == null ? "" : mediaType);
        }

        z.zip(Manifest.ENTRY_NAME, new StringInputStream(manifest.asString()));
        z.close();
    }

    /**
     * Save the content of this package to our file, overwriting it if it exists.
     * 
     * @return the saved file.
     * @throws IOException if an error occurs while saving.
     */
    public File save() throws IOException {
        return this.saveAs(this.getFile());
    }

    public File saveAs(final File fNoExt) throws IOException {
        final File f = this.addExt(fNoExt);
        if (f.getParentFile() != null)
            f.getParentFile().mkdirs();
        // ATTN at this point, we must have read all the content of this file
        // otherwise we could save to File.createTempFile("oofd", null).deleteOnExit();
        final FileOutputStream out = new FileOutputStream(f);
        final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(out, 512 * 1024);
        try {
            this.save(bufferedOutputStream);
        } finally {
            bufferedOutputStream.close();
        }
        return f;
    }
}
