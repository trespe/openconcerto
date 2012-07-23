package org.openconcerto.modules.finance.payment.ebics.crypto;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.EbicsUtil;

public class KeyStoreGenerator {

    public static void createKeys(KeyStore keyStore, String password, String countryCode, String organization, String locality, String state, String email) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        final KeyPair keyPairX002 = createPrivateKey();
        final KeyPair keyPairE002 = createPrivateKey();
        final KeyPair keyPairA005 = createPrivateKey();

        final PrivateKey privateKeyX002 = keyPairX002.getPrivate();
        final PrivateKey privateKeyE002 = keyPairE002.getPrivate();
        final PrivateKey privateKeyA005 = keyPairA005.getPrivate();

        EbicsUtil.dumpKey(privateKeyX002);
        EbicsUtil.dumpKey(privateKeyE002);
        EbicsUtil.dumpKey(privateKeyA005);
        // Load X002
        Certificate[] chainX002 = new Certificate[1];
        chainX002[0] = generateV3Certificate(keyPairX002, countryCode, organization, locality, state, email);
        keyStore.setKeyEntry(EbicsConfiguration.AUTHENTICATION_X002, privateKeyX002, password.toCharArray(), chainX002);
        // Load E002
        Certificate[] chainE002 = new Certificate[1];
        chainE002[0] = generateV3Certificate(keyPairE002, countryCode, organization, locality, state, email);
        keyStore.setKeyEntry(EbicsConfiguration.ENCRYPTION_E002, privateKeyE002, password.toCharArray(), chainE002);

        // Load A005
        Certificate[] chainA005 = new Certificate[1];
        chainA005[0] = generateV3Certificate(keyPairA005, countryCode, organization, locality, state, email);
        keyStore.setKeyEntry(EbicsConfiguration.SIGNATURE_A005, privateKeyA005, password.toCharArray(), chainA005);

    }

    public static X509Certificate generateV3Certificate(KeyPair pair, String countryCode, String organization, String locality, String state, String email) throws Exception {
        PrivateKey privKey = pair.getPrivate();
        PublicKey pubKey = pair.getPublic();

        final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
        // distinguished name table.
        //
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

        builder.addRDN(BCStyle.C, countryCode);
        builder.addRDN(BCStyle.O, organization);
        builder.addRDN(BCStyle.L, locality);
        builder.addRDN(BCStyle.ST, state);
        builder.addRDN(BCStyle.E, email);

        //
        // create the certificate - version 3 - without extensions
        //
        ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BC).build(privKey);
        final Date dateStart = new Date(System.currentTimeMillis() - 50000);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5);
        final Date dateStop = new Date(cal.getTimeInMillis());

        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(builder.build(), BigInteger.valueOf(1), dateStart, dateStop, builder.build(), pubKey);

        X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certGen.build(sigGen));

        cert.checkValidity(new Date());

        cert.verify(pubKey);

        cert.verify(cert.getPublicKey());
        return cert;
    }

    private static KeyPair createPrivateKey() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return keyPair;
    }

}
