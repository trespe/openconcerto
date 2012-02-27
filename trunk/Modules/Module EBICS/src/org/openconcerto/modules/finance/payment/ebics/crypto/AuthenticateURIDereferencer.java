package org.openconcerto.modules.finance.payment.ebics.crypto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.crypto.Data;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.openconcerto.utils.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.crypto.URIDereferencer;

public class AuthenticateURIDereferencer implements URIDereferencer {
    private Document doc;

    public AuthenticateURIDereferencer(Document doc) {
        this.doc = doc;
    }

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
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node node = nodes.item(i);
                System.out.println(i + ":" + node.getChildNodes().getLength());
                l.add(node);
            }

            Node nodeToSign = nodes.item(0);
            Node sigParent = nodeToSign.getParentNode();
            // return new DOMSubTreeData(nodeToSign, true);
            return new NodeSetData() {

                @Override
                public Iterator iterator() {

                    return l.iterator();
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
