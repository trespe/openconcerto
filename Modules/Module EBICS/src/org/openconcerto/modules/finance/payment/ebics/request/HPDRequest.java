package org.openconcerto.modules.finance.payment.ebics.request;

import java.util.Date;

import org.apache.xml.security.utils.Base64;
import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.EbicsUtil;
import org.openconcerto.modules.finance.payment.ebics.OrderType;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HPDRequest extends EbicsRequest {
    private final EbicsConfiguration config;

    /**
     * HPD : download bank parameters
     */
    public HPDRequest(EbicsConfiguration config) {
        super(config, OrderType.HPD);
        this.config = config;

    }

    public Document getXMLDocument() {
        Document d = super.getXMLDocument();
        insertBankPubKeyDigest(d);
        // OrderParams
        NodeList eDetailList = d.getElementsByTagName("OrderDetails");
        Node eDetail = eDetailList.item(0);
        Element elementOrderParams = d.createElement("GenericOrderParams");
        eDetail.appendChild(elementOrderParams);
        //
        // <TransactionPhase>Initialisation</TransactionPhase>
        NodeList eMutableList = d.getElementsByTagName("mutable");
        Node eHeader = eMutableList.item(0);
        Element elementTransactionPhase = d.createElement("TransactionPhase");
        elementTransactionPhase.setTextContent("Initialisation");
        eHeader.appendChild(elementTransactionPhase);

        return d;
    }
}
