package org.openconcerto.modules.finance.payment.ebics.response;

import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;

public class HPDResponse extends DataResponse {

    public HPDResponse(EbicsConfiguration conf, String xmlResponse) {
        super(conf, xmlResponse);
    }

    @Override
    public void extractData(String xml) throws Exception {
        // TODO parse HPDResponseOrderData

    }

}
