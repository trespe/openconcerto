package org.openconcerto.modules.finance.payment.ebics;
public class Host {
    private final String hostId, url;

    public Host(String hostId, String url) {
        this.hostId = hostId;
        this.url = url;
    }

    public String getHostId() {
        return hostId;
    }

    public String getURL() {
        return url;
    }
}
