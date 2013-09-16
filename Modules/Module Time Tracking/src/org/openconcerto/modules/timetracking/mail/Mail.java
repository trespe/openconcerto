package org.openconcerto.modules.timetracking.mail;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

/**
 * An electronic message.
 * 
 * @author Sylvain
 */
public class Mail {

    private final String subject;
    private final InternetAddress recipient;
    private String text;
    private final List<File> attachements;

    private final Session session;
    private final MimeMessage msg;

    public Mail(String subject, String recipient) throws AddressException {
        super();
        this.subject = subject;
        this.recipient = new InternetAddress(recipient);
        this.text = "";
        this.attachements = new ArrayList<File>();

        // final boolean debug = Boolean.getBoolean("ilm.backup.debugMail");
        // this debug JavaMail API (eg javamail.default.providers)
        // if (debug)
        // props.put("mail.debug", debug + "");
        this.session = Session.getInstance(new Properties(), null);
        // this.session.setDebug(debug);

        this.msg = new MimeMessage(this.session);
    }

    public final void setText(String text) {
        this.text = text;
    }

    public List<File> getAttachements() {
        return this.attachements;
    }

    /**
     * The underlying message, usefull for using exotic properties.
     * 
     * @return the underlying message.
     */
    public final MimeMessage getMessage() {
        return this.msg;
    }

    public final void send(Account account) throws IOException, MessagingException {
        // create some properties and get the default Session
        final Properties props = this.session.getProperties();
        props.put("mail.transport.protocol", account.getSMTPProtocol());
        props.put("mail.host", account.getSmtpServer());
        if (account.getAuth() != null) {
            props.put("mail.smtp.auth", "true");
            props.put("mail.user", account.getAuth().getUserName());
            if (account.isSSL()) {
                props.put("mail.transport.protocol", "smtps");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
                props.put("mail.smtp.starttls.enable", "true");
            }
        }

        // create a message
        final MimeMessage msg = this.msg;
        try {
            msg.setFrom(new InternetAddress(account.getAddress(), account.getName()));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("can't encode account.getName()", e);
        }
        msg.setRecipient(RecipientType.TO, this.recipient);
        msg.setSubject(this.subject);
        msg.setSentDate(new Date());

        if (this.attachements.isEmpty()) {
            setText(msg);
        } else {
            final MimeBodyPart txt = new MimeBodyPart();
            setText(txt);

            final MimeMultipart multi = new MimeMultipart();
            multi.addBodyPart(txt);
            for (final File f : this.attachements) {
                final MimeBodyPart att = new MimeBodyPart();
                att.attachFile(f);
                multi.addBodyPart(att);
            }

            msg.setContent(multi);
        }

        final Transport t = this.session.getTransport();
        try {
            final String pass = account.getAuth() == null ? null : account.getAuth().getPassword();
            t.connect(null, account.getPort(), null, pass);
            t.sendMessage(msg, msg.getAllRecipients());
        } finally {
            t.close();
        }
    }

    /**
     * Parse the given exception to a more human readable format.
     * 
     * @param mex an exception.
     * @return some useful informations.
     */
    public static final String handle(MessagingException mex) {
        final StringBuilder sb = new StringBuilder(512);
        // sb.append(ExceptionUtils.getStackTrace(mex) + "\n");
        Exception ex = mex;
        do {
            if (ex instanceof SendFailedException) {
                SendFailedException sfex = (SendFailedException) ex;
                Address[] invalid = sfex.getInvalidAddresses();
                if (invalid != null) {
                    sb.append("    ** Invalid Addresses\n");
                    if (invalid != null) {
                        for (int i = 0; i < invalid.length; i++)
                            sb.append("         " + invalid[i]);
                    }
                }
                Address[] validUnsent = sfex.getValidUnsentAddresses();
                if (validUnsent != null) {
                    sb.append("    ** ValidUnsent Addresses\n");
                    if (validUnsent != null) {
                        for (int i = 0; i < validUnsent.length; i++)
                            sb.append("         " + validUnsent[i]);
                    }
                }
                Address[] validSent = sfex.getValidSentAddresses();
                if (validSent != null) {
                    sb.append("    ** ValidSent Addresses\n");
                    if (validSent != null) {
                        for (int i = 0; i < validSent.length; i++)
                            sb.append("         " + validSent[i]);
                    }
                }
            }
            sb.append("\n");
            if (ex instanceof MessagingException)
                ex = ((MessagingException) ex).getNextException();
            else
                ex = null;
        } while (ex != null);

        return sb.toString();
    }

    private void setText(final MimePart txt) throws MessagingException {
        // If the desired charset is known, you can use
        // setText(text, charset)
        txt.setText(this.text, "UTF8");
    }
}
