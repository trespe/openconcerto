package org.openconcerto.modules.finance.payment.ebics.request;

import java.util.HashSet;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AuthenticateResourceResolver extends ResourceResolverSpi {
    private static final String AUTH_XPOINTER = "#xpointer(//*[@authenticate='true'])";
    private final Document doc;

    public AuthenticateResourceResolver(Document doc) {
        this.doc = doc;
    }

    @Override
    public XMLSignatureInput engineResolve(Attr uri, String BaseURI) throws ResourceResolverException {
        try {
            String PATH = "//*[@authenticate='true']";
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile(PATH);
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            if (nodes.getLength() < 1) {
                System.out.println("Invalid document, can't find node by PATH: " + PATH);
                return null;
            }

            final Set<Node> l = new HashSet<Node>();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node item = nodes.item(i);
                l.add(item);

            }
            XMLSignatureInput out = new XMLSignatureInput(nodes.item(0));
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean engineCanResolve(Attr uri, String BaseURI) {
        return uri.getValue().equals(AUTH_XPOINTER);
    }

}
