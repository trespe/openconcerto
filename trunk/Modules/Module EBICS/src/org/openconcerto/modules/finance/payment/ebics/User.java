package org.openconcerto.modules.finance.payment.ebics;
public class User {
    private final String userId;

    public User(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
