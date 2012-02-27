package org.openconcerto.modules.finance.payment.ebics.request;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.OrderType;

public class INIRequest extends EbicsRequest {

    public INIRequest(EbicsConfiguration config) throws GeneralSecurityException, IOException {
        super(config, OrderType.INI);
        final SignaturePubKeyOrderData data = new SignaturePubKeyOrderData(config);
        setData(data.getXML().getBytes());
    }

}
