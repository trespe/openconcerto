package org.openconcerto.modules.finance.payment.ebics.request;

import java.io.UnsupportedEncodingException;

import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.EbicsUtil;
import org.openconcerto.modules.finance.payment.ebics.OrderType;

public class HIARequest extends EbicsRequest {

    public HIARequest(EbicsConfiguration config) throws UnsupportedEncodingException {
        super(config, OrderType.HIA);
        final HIARequestOrderData data = new HIARequestOrderData(config);
        setData(EbicsUtil.getXML(data.getXMLDocument()).getBytes());
    }

}
