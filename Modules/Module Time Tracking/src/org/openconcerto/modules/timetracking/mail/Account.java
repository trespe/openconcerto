package org.openconcerto.modules.timetracking.mail;

import javax.mail.PasswordAuthentication;

/**
 * A mail account.
 * 
 * @author Sylvain
 */
public class Account {

    private final String name;
    private final String address;
    private final String smtpServer;
    private final int port;
    private PasswordAuthentication auth;
    private final boolean ssl;

    public Account(String name, String address, String smtpServer, boolean ssl) {
        this(name, address, smtpServer, -1, ssl);
    }

    public Account(String name, String address, String smtpServer, int port, boolean ssl) {
        super();
        this.name = name;
        this.address = address;
        this.smtpServer = smtpServer;
        this.port = port;
        this.auth = null;
        this.ssl = ssl;
    }

    public final String getName() {
        return this.name;
    }

    public final String getAddress() {
        return this.address;
    }

    public final String getSmtpServer() {
        return this.smtpServer;
    }

    public final int getPort() {
        return this.port;
    }

    public boolean isSSL() {
        return this.ssl;
    }

    public final PasswordAuthentication getAuth() {
        return this.auth;
    }

    public void setAuth(PasswordAuthentication auth) {
        this.auth = auth;
    }

    public String getSMTPProtocol() {
        return "smtp";
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + getAddress() + " through " + this.getSmtpServer();
    }
}
