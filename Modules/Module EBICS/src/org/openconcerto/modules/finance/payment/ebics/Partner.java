package org.openconcerto.modules.finance.payment.ebics;
public class Partner {
    private final String partnerId;

    public Partner(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerId() {
        return partnerId;
    }
}
