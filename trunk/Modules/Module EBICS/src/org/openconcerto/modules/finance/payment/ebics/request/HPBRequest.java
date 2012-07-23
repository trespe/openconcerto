package org.openconcerto.modules.finance.payment.ebics.request;

import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.OrderType;

public class HPBRequest extends EbicsRequest {

    public HPBRequest(EbicsConfiguration config) {
        super(config, OrderType.HPB);
    }

}
