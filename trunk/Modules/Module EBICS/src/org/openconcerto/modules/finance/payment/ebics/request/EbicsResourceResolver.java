package org.openconcerto.modules.finance.payment.ebics.request;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

public class EbicsResourceResolver implements LSResourceResolver {

    private DOMImplementationLS domImplementationLS;
    private String version;

    public EbicsResourceResolver(String version) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        System.setProperty(DOMImplementationRegistry.PROPERTY, "org.apache.xerces.dom.DOMImplementationSourceImpl");
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        domImplementationLS = (DOMImplementationLS) registry.getDOMImplementation("LS");
        this.version = version;
    }

    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        System.err.println("==== Resolving  '" + systemId + "'  '" + namespaceURI + "'");

        LSInput lsInput = domImplementationLS.createLSInput();
        InputStream is = null;
        if (systemId.contains("/")) {
            systemId = systemId.substring(systemId.lastIndexOf('/') + 1);
        }
        final File file = new File("schema/" + version + "/" + systemId);
        if (!file.exists()) {
            throw new IllegalArgumentException(file.getAbsolutePath() + " not found");
        }
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        lsInput.setByteStream(is);
        lsInput.setSystemId(systemId);
        return lsInput;
    }
}
