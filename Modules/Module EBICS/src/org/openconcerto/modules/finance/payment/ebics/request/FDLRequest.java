package org.openconcerto.modules.finance.payment.ebics.request;

import java.util.Date;

import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.OrderType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FDLRequest extends EbicsRequest {
    private final EbicsConfiguration config;
    private Date start;
    private Date end;
    private String fileFormat;

    /**
     * File download (FDL)
     * 
     * */
    public FDLRequest(EbicsConfiguration config, Date start, Date end) {
        super(config, OrderType.FDL);
        this.config = config;
        // Relevé de compte
        this.fileFormat = "camt.xxx.cfonb120.stm";
    }

    public FDLRequest(EbicsConfiguration config, String fileFormat) {
        super(config, OrderType.FDL);
        this.config = config;
        this.fileFormat = fileFormat;
    }

    public Document getXMLDocument() {
        Document d = super.getXMLDocument();
        insertBankPubKeyDigest(d);

        // FDLOrderParams
        NodeList eDetailList = d.getElementsByTagName("OrderDetails");
        Node eDetail = eDetailList.item(0);
        Element elementFDLOrderParams = d.createElement("FDLOrderParams");
        Element elementFileFormat = d.createElement("FileFormat");
        elementFileFormat.setAttribute("CountryCode", "FR");
        elementFileFormat.setTextContent(fileFormat);
        elementFDLOrderParams.appendChild(elementFileFormat);
        eDetail.appendChild(elementFDLOrderParams);

        // <TransactionPhase>Initialisation</TransactionPhase>
        NodeList eMutableList = d.getElementsByTagName("mutable");
        Node eHeader = eMutableList.item(0);
        Element elementTransactionPhase = d.createElement("TransactionPhase");
        elementTransactionPhase.setTextContent("Initialisation");
        eHeader.appendChild(elementTransactionPhase);

        return d;
    }

}
