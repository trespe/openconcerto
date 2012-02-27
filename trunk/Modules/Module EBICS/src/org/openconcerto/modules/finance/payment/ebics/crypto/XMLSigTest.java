package org.openconcerto.modules.finance.payment.ebics.crypto;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.Reference;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.Data;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.bouncycastle.util.encoders.Base64Encoder;

import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLSigTest {

    /**
     * XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM", new
     * org.jcp.xml.dsig.internal.dom.XMLDSigRI()); Reference ref =
     * fac.newReference("#xpointer(//*[@authenticate='true'])",
     * fac.newDigestMethod(DigestMethod.SHA256, null), Collections.singletonList (fac.newTransform
     * (CanonicalizationMethod.INCLUSIVE, (TransformParameterSpec) null)), null, null); SignedInfo
     * si = fac.newSignedInfo ... KeyStore ks = KeyStore.getInstance("JKS"); DOMSignContext dsc =
     * new DOMSignContext (keyEntry.getPrivateKey(), doc.getDocumentElement()); XMLSignature
     * signature = fac.newXMLSignature(si, null); signature.sign(dsc);
     * 
     * *
     */

    /**
     * @param args
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws Exception {

        String response = FileUtils.read(new File("out.xml"), "UTF-8");
        // EBICS doesn't respect the XML Sig schema,
        // idiot designers always create bad protocols...
        // Here is a dirty fix...
        String correctResponse = response.replace("AuthSignature", "Signature");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final Document doc = dbf.newDocumentBuilder().parse(new StringInputStream(correctResponse));

        NodeList nl = doc.getElementsByTagName("Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Cannot find Signature element");
        }
        String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());

        System.out.println(nl.item(0).getLocalName());

        DOMValidateContext valContext = new DOMValidateContext(new X509KeySelector()

        , nl.item(0));

        valContext.putNamespacePrefix("http://www.w3.org/2000/09/xmldsig#", "ds");
        valContext.setURIDereferencer(new URIDereferencer() {

            @Override
            public Data dereference(URIReference uriReference, XMLCryptoContext context) throws URIReferenceException {
                try {
                    System.out.println(uriReference.getURI());
                    System.out.println(context.getBaseURI());
                    String PATH = "//*[@authenticate='true']";
                    XPathFactory factory = XPathFactory.newInstance();
                    XPath xpath = factory.newXPath();
                    XPathExpression expr = xpath.compile(PATH);
                    NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                    if (nodes.getLength() < 1) {
                        System.out.println("Invalid document, can't find node by PATH: " + PATH);
                        return null;
                    }
                    final List<Node> l = new ArrayList<Node>();
                    for (int i = 0; i < nodes.getLength(); i++)
                        l.add(nodes.item(i));
                    Node nodeToSign = nodes.item(0);
                    Node sigParent = nodeToSign.getParentNode();
                    debug(nodes);

                    return new NodeSetData() {

                        public Iterator iterator() {

                            return l.iterator();
                        };
                    };
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });

        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        System.out.println(signature.getSignatureValue().getValue()[0]);

        boolean coreValidity = signature.validate(valContext);

        // Check core validation status.
        if (coreValidity == false) {
            System.out.println("Signature failed core validation");
            boolean sv = signature.getSignatureValue().validate(valContext);
            System.out.println("signature validation status: " + sv);

            byte[] b = new byte[50000];
            int l = signature.getSignedInfo().getCanonicalizedData().read(b);
            System.out.println(new String(b, 0, l));
            if (sv == false) {
                // Check the validation status of each Reference.
                Iterator<javax.xml.crypto.dsig.Reference> i = signature.getSignedInfo().getReferences().iterator();
                for (int j = 0; i.hasNext(); j++) {
                    boolean refValid = (i.next()).validate(valContext);
                    System.out.println("ref[" + j + "] validity status: " + refValid);
                }
            }
        } else {
            System.out.println("Signature passed core validation");
        }

    }

    protected static void debug(NodeList nodes) {
        System.out.println("Output xml:" + nodes);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            System.out.println(n.getNodeName());
        }

    }
}
