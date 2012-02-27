package org.openconcerto.modules.finance.payment.ebics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Properties;

import org.apache.poi.util.HexDump;
import org.apache.xml.security.utils.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class EbicsConfiguration {
    private Host host;
    private Partner partner;
    private User user;
    private KeyStore keyStore;
    private String keyStorePassword;
    public static final String AUTHENTICATION_X002 = "x002";
    public static final String ENCRYPTION_E002 = "e002";
    public static final String SIGNATURE_A005 = "a005";

    public EbicsConfiguration(InputStream inStream) throws IOException {
        Properties p = new Properties();
        Security.addProvider(new BouncyCastleProvider());
        p.load(inStream);
        inStream.close();
        this.host = new Host(p.getProperty("hostId"), p.getProperty("url"));
        this.partner = new Partner(p.getProperty("partnerId"));
        this.user = new User(p.getProperty("userId"));
    }

    public EbicsConfiguration(Host host, Partner partner, User user) {
        this.host = host;
        this.partner = partner;
        this.user = user;
        Security.addProvider(new BouncyCastleProvider());
    }

    public Host getHost() {
        return host;
    }

    public Partner getPartner() {
        return partner;
    }

    public User getUser() {
        return user;
    }

    public void setKeyStore(KeyStore ks, String pass) throws Exception {
        this.keyStore = ks;
        this.keyStorePassword = pass;
        System.out.println("Private Encryption key: Hash " + EbicsUtil.getSAH256HashBase64(getEncryptionPrivateKey().getEncoded()) + " "
                + EbicsUtil.getSAH256HashBase64(((RSAPrivateKey) getEncryptionPrivateKey()).getEncoded()));
        System.out.println("Public Encryption key : Hash " + EbicsUtil.getSAH256HashBase64(getEncryptionCertificate().getPublicKey().getEncoded()));

        RSAPublicKey rs = (RSAPublicKey) getEncryptionCertificate().getPublicKey();
        System.out.println(HexDump.toHex(rs.getModulus().toByteArray()));
        System.out.println("Private Authentication key: Hash " + EbicsUtil.getSAH256HashBase64(getAuthenticationPrivateKey().getEncoded()));
        System.out.println("Public Authentication key : Hash " + EbicsUtil.getSAH256HashBase64(getAuthenticationCertificate().getPublicKey().getEncoded()));
        rs = (RSAPublicKey) getAuthenticationCertificate().getPublicKey();
        System.out.println(HexDump.toHex(rs.getModulus().toByteArray()));
        System.out.println("Private Signature key: Hash " + EbicsUtil.getSAH256HashBase64(getSignaturePrivateKey().getEncoded()));
        System.out.println("Public Signature key : Hash " + EbicsUtil.getSAH256HashBase64(getSignatureCertificate().getPublicKey().getEncoded()));
        rs = (RSAPublicKey) getSignatureCertificate().getPublicKey();
        System.out.println(HexDump.toHex(EbicsUtil.getPublicKeyHash256((RSAPublicKey) loadBankPublicEncryptionKey())));
        System.out.println();

        try {
            System.out.println("Bank Public Authentication key : Hash " + EbicsUtil.getSAH256HashBase64(loadBankPublicAuthenticationKey().getEncoded()));
            byte[] byteArray = loadBankPublicAuthenticationKey().getModulus().toByteArray();
            System.out.println(byteArray.length);
            byte[] b = new byte[byteArray.length - 1];
            System.arraycopy(byteArray, 1, b, 0, b.length);
            // byteArray = b;
            System.out.println(Base64.encode(byteArray, Integer.MAX_VALUE));
            System.out.println(EbicsUtil.getSAH256HashBase64(byteArray));
            System.out.println(EbicsUtil.getSAH256HashBase64(loadBankPublicAuthenticationKey().getEncoded()));
            System.out.println(HexDump.toHex(byteArray));
            System.out.println(HexDump.toHex(byteArray));
            System.out.println("Bank Public Encryption key : Hash " + EbicsUtil.getSAH256HashBase64(loadBankPublicEncryptionKey().getEncoded()));
        } catch (Exception e) {
            System.err.println("Error reading bank key");
        }
    }

    public Certificate getAuthenticationCertificate() throws KeyStoreException {
        return this.keyStore.getCertificate(AUTHENTICATION_X002);
    }

    public PrivateKey getAuthenticationPrivateKey() throws Exception {
        return (PrivateKey) this.keyStore.getKey(AUTHENTICATION_X002, keyStorePassword.toCharArray());
    }

    public Certificate getEncryptionCertificate() throws KeyStoreException {
        return this.keyStore.getCertificate(ENCRYPTION_E002);
    }

    public PrivateKey getEncryptionPrivateKey() throws Exception {
        return (PrivateKey) this.keyStore.getKey(ENCRYPTION_E002, keyStorePassword.toCharArray());
    }

    public Certificate getSignatureCertificate() throws KeyStoreException {
        return this.keyStore.getCertificate(SIGNATURE_A005);
    }

    public PrivateKey getSignaturePrivateKey() throws Exception {
        return (PrivateKey) this.keyStore.getKey(SIGNATURE_A005, keyStorePassword.toCharArray());
    }

    public Date getKeyGenerationDate() {
        // TODO Stocker la date
        return new Date();
    }

    public void saveBankPublicAuthenticationKey(RSAPublicKey publicAuthenticationKey) throws Exception {
        // Store Public Key.
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicAuthenticationKey.getEncoded());
        FileOutputStream fos = new FileOutputStream("keys/" + "BANK-" + host.getHostId() + "-" + AUTHENTICATION_X002 + ".key");
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();
    }

    public RSAPublicKey loadBankPublicAuthenticationKey() throws Exception {
        final String name = "keys/" + "BANK-" + host.getHostId() + "-" + AUTHENTICATION_X002 + ".key";
        File filePublicKey = new File(name);
        RSAPublicKey publicKey = loadPublicKey(name, filePublicKey);
        return publicKey;

    }

    public void saveBankPublicEncryptionKey(RSAPublicKey publicAuthenticationKey) throws Exception {
        // Store Public Key.
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicAuthenticationKey.getEncoded());
        FileOutputStream fos = new FileOutputStream("keys/" + "BANK-" + host.getHostId() + "-" + ENCRYPTION_E002 + ".key");
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();

    }

    public RSAPublicKey loadBankPublicEncryptionKey() throws Exception {
        final String name = "keys/" + "BANK-" + host.getHostId() + "-" + ENCRYPTION_E002 + ".key";
        File filePublicKey = new File(name);
        RSAPublicKey publicKey = loadPublicKey(name, filePublicKey);
        return publicKey;

    }

    private RSAPublicKey loadPublicKey(final String name, File filePublicKey) throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        FileInputStream fis = new FileInputStream(name);
        byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
        fis.read(encodedPublicKey);
        fis.close();
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        RSAPublicKey k = (RSAPublicKey) publicKey;
        return k;

    }
}
