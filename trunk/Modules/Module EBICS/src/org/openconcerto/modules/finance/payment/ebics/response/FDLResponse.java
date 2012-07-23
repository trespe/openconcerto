package org.openconcerto.modules.finance.payment.ebics.response;

import java.security.interfaces.RSAPublicKey;

import javax.xml.parsers.DocumentBuilderFactory;

import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;

public class FDLResponse extends DataResponse {

    public FDLResponse(EbicsConfiguration config, String xmlResponse) {
        super(config, xmlResponse);
    }

    public void extractData(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        System.out.println("XML:");
        System.out.println(xml);

    }

}
