package org.openconcerto.modules.finance.payment.ebics.request;

import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.OrderType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HAARequest extends EbicsRequest {

    /**
     * HAA : download retrievable order types
     */
    public HAARequest(EbicsConfiguration config) {
        super(config, OrderType.HAA);
        // Epic fail: the server we use doesn't support the order type HAA :)
    }

    public Document getXMLDocument() {
        Document d = super.getXMLDocument();
        insertBankPubKeyDigest(d);
        // OrderParams
        NodeList eDetailList = d.getElementsByTagName("OrderDetails");
        Node eDetail = eDetailList.item(0);
        Element elementOrderParams = d.createElement("GenericOrderParams");
        eDetail.appendChild(elementOrderParams);
        // <TransactionPhase>Initialisation</TransactionPhase>
        NodeList eMutableList = d.getElementsByTagName("mutable");
        Node eHeader = eMutableList.item(0);
        Element elementTransactionPhase = d.createElement("TransactionPhase");
        elementTransactionPhase.setTextContent("Initialisation");
        eHeader.appendChild(elementTransactionPhase);
        return d;
    }
}
