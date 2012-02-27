package org.openconcerto.modules.finance.payment.ebics.crypto;

/*
 * Copyright Rene Mayrhofer File created 2006-03
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.jce.provider.JDKPKCS12KeyStore;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

/**
 * This class uses the Bouncycastle lightweight API to generate X.509 certificates programmatically.
 * It assumes a CA certificate and its private key to be available and can sign the new certificate
 * with this CA. Some of the code for this class was taken from
 * org.bouncycastle.x509.X509V3CertificateGenerator, but adapted to work with the lightweight API
 * instead of JCE (which is usually not available on MIDP2.0).
 * 
 * @author Rene Mayrhofer
 * @version 1.0
 */
public class X509CertificateGenerator {

    /** This method is used for signing the certificate. */
    public static final String CertificateSignatureAlgorithm = "SHA1WithRSAEncryption";
    /** This string is used as the friendly name for the certificate in the PKCS12 exported file. */
    public static final String CertificateExportFriendlyName = "Certificate for IPSec WLAN access";
    /** This string is used as the friendly name for the private key in the PKCS12 exported file. */
    public static final String KeyExportFriendlyName = "Private key for IPSec WLAN access";

    /**
     * This holds the certificate of the CA used to sign the new certificate. The object is created
     * in the constructor.
     */
    private X509Certificate caCert;
    /**
     * This holds the private key of the CA used to sign the new certificate. The object is created
     * in the constructor.
     */
    private RSAPrivateCrtKeyParameters caPrivateKey;

    /**
     * Creates a new self-signed CA (certificate authority) for subsequently signing certificates.
     * 
     * @param commonName The common name (CN) field of the X.509 distinguished name that should be
     *        set for the new certificate. All other fields of the distinguished name are not set.
     * @param validityDays How long the new certificate should be valid, in days.
     * @param caFile The PKCS12 encoded file to which the CA should be exported to. It will contain
     *        the self-signed certificate and the matching private key.
     * @param caPassword The password used to encode the PKCS12 file.
     * @param caAlias The friendly name of the CA in the PKCS12 file.
     * @param useBCAPI Set to true if the Bouncycastle lightweight API should be used for
     *        cryptographical operations. If set to false, the JCE infrastructure with the
     *        configured default provider will be used. JCE may be faster depending on the provider
     *        implementation, but it might not be available on embedded platforms, i.e. J2ME.
     * @return true if the CA could be created, self-signed, and exported successfully, false
     *         otherwise.
     * @throws CryptoException
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws SecurityException
     * @throws DataLengthException
     * @throws InvalidKeyException
     */
    public static boolean createNewCa(String commonName, int validityDays, String caFile, String caPassword, String caAlias, boolean useBCAPI) throws InvalidKeyException, DataLengthException,
            SecurityException, SignatureException, NoSuchAlgorithmException, KeyStoreException, CertificateException, InvalidKeySpecException, IOException, CryptoException {
        X509CertificateGenerator g = new X509CertificateGenerator();
        return g.createCertificate(commonName, validityDays, caFile, caPassword, caAlias);
    }

    /**
     * Converts from a PKCS12 encoded file to PEM encoded files readable by openssl (and
     * subsequently e.g. openswan and racoon).
     * 
     * @param inFile The PKCS12 encoded input file. It is assumed to contain the (client)
     *        certificate, the matching private key, and the complete certificate chain up to the
     *        self-signed root CA.
     * @param inPassword The password needed to decrypt the PKCS12 encoded input file.
     * @param outCertFile The output certificate in PEM format. If null, it will not be created.
     * @param outKeyFile The output private key in PEM format. If null, it will not be created.
     * @param outCertChainFile The output certificate chain in PEM format. If null, it will not be
     *        created.
     * @param useBCAPI Set to true if the Bouncycastle lightweight API should be used for
     *        cryptographical operations. If set to false, the JCE infrastructure with the
     *        configured default provider will be used. JCE may be faster depending on the provider
     *        implementation, but it might not be available on embedded platforms, i.e. J2ME.
     * @return true if all requested parts could be exported successfully, false otherwise.
     */
    public static boolean convertPKCS12toPEM(String inFile, String inPassword, String outCertFile, String outKeyFile, String outCertChainFile, boolean useBCAPI) {
        if (inFile == null || inPassword == null || outCertFile == null || outKeyFile == null || outCertChainFile == null) {
            throw new IllegalArgumentException("Can not work with null parameter");
        }

        // echo "test" | openssl pkcs12 -in test.p12 -passin stdin -nodes -clcerts -nokeys >
        // /tmp/gateway.pem
        // echo "test" | openssl pkcs12 -in test.p12 -passin stdin -nodes -cacerts -nokeys >
        // /tmp/ipsecca.pem
        // echo "test" | openssl pkcs12 -in test.p12 -passin stdin -nodes -nocerts >
        // /tmp/gatewayKey.pem

        /*
         * System.out.println("Loading CA certificate and private key from file '" + caFile +
         * "', using alias '" + caAlias + "' with " + (useBCAPI ? "Bouncycastle lightweight API" :
         * "JCE API"));
         */

        return false;
    }

    /**
     * Initializes the objects without an existing CA. This is useful to create a new CA, because
     * createCertificate will create a self-signed certificate if no CA has been set. This
     * constructor is used by createNewCa.
     * 
     * @see #createNewCa
     * @see #createCertificate(String, int, String, String, String)
     * @param useBCAPI Set to true if the Bouncycastle lightweight API should be used for
     *        cryptographical operations. If set to false, the JCE infrastructure with the
     *        configured default provider will be used. JCE may be faster depending on the provider
     *        implementation, but it might not be available on embedded platforms, i.e. J2ME.
     */
    protected X509CertificateGenerator() {

        System.out.println("Protected constructor has been called. Assuming that no CA should be loaded but that a new one will be created");
        caPrivateKey = null;
        caCert = null;
    }

    /**
     * Initializes the object for creating certificates by loading the CA certificate and private
     * key.
     * 
     * A new CA can be created with:
     * 
     * Comment out basicConstraints in /etc/ssl/openssl.cnf (CA:FALSE should not be set, but it does
     * not need to be set to true) /usr/lib/ssl/misc/CA.sh -newca openssl pkcs12 -export -in
     * demoCA/cacert.pem -inkey demoCA/private/cakey.pem -out ca.p12 -name "Test CA"
     * 
     * @param caFile The PKCS12 encoded file containing the whole CA to use. It must contain the CA
     *        certificate (which will be self-signed for top-level CAs) and the matching private
     *        key.
     * @param caPassword The password necessary to decode the PKCS12 file.
     * @param caAlias The friendly name of the CA in the PKCS12 file.
     * @param useBCAPI Set to true if the Bouncycastle lightweight API should be used for
     *        cryptographical operations. If set to false, the JCE infrastructure with the
     *        configured default provider will be used. JCE may be faster depending on the provider
     *        implementation, but it might not be available on embedded platforms, i.e. J2ME.
     */
    public X509CertificateGenerator(String caFile, String caPassword, String caAlias, boolean useBCAPI) throws NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException,
            InvalidKeyException, NoSuchProviderException, SignatureException {

        if (caFile == null || caPassword == null || caAlias == null) {
            throw new IllegalArgumentException("Can not work with null parameter");
        }

        System.out.println("Loading CA certificate and private key from file '" + caFile + "', using alias '" + caAlias + "' with  Bouncycastle lightweight API");

        PKCS12Content content = loadFromKeyStore(new FileInputStream(new File(caFile)), caPassword, caAlias, useBCAPI);
        if (content.privateKeys == null || content.privateKeys[0] == null) {
            System.out.println("Got null private key from keystore, initialization failed");
            throw new RuntimeException("Got null key from keystore!");
        }
        if (content.certificates == null || content.certificates[0] == null) {
            System.out.println("Got null certificate from keystore, initialization failed");
            throw new RuntimeException("Got null cert from keystore!");
        }

        caPrivateKey = content.privateKeys[0];
        caCert = content.certificates[0];

        System.out.println("Successfully loaded CA key and certificate. CA DN is '" + caCert.getSubjectDN().getName() + "'");
        caCert.verify(caCert.getPublicKey());
        System.out.println("Successfully verified CA certificate with its own public key.");
    }

    /**
     * Returns the distinguished name of the CA used to sign the newly generated certificates. This
     * is important e.g. when using the new certificates for IPSec connections under Windows
     * 2000/XP, because the CA DN must be specified for such connections.
     * 
     * @return The CA DN.
     */
    public String getCaDistinguishedName() {
        if (caCert == null) {
            System.out.println("CA has not been loaded properly, can not get distinguished name");
            return null;
        }
        return caCert.getSubjectDN().toString();
    }

    /**
     * This method should create something similar to:
     * 
     * openssl req -new -outform PEM -newkey rsa:1024 -nodes -keyout /tmp/test.key -keyform PEM -out
     * /tmp/test.pem -days 30 -config /etc/ssl/openssl.cnf openssl ca -policy policy_anything -out
     * /tmp/test.crt -config /etc/ssl/openssl.cnf -infiles /tmp/test.pem openssl pkcs12 -export -in
     * /tmp/test.crt -inkey /tmp/test.key -certfile demoCA/cacert.pem -out test.p12
     * 
     * @param commonName The common name (CN) field of the X.509 distinguished name that should be
     *        set for the new certificate. All other fields of the distinguished name are not set.
     * @param validityDays How long the new certificate should be valid, in days.
     * @param exportFile The PKCS12 encoded file to which the certificate should be exported to. It
     *        will contain the self-signed certificate and the matching private key.
     * @param exportPassword The password used to encode the PKCS12 file.
     * @return true if the certificate could be created, signed, and exported successfully, false
     *         otherwise.
     */
    public boolean createCertificate(String commonName, int validityDays, String exportFile, String exportPassword) throws IOException, InvalidKeyException, SecurityException, SignatureException,
            NoSuchAlgorithmException, DataLengthException, CryptoException, KeyStoreException, CertificateException, InvalidKeySpecException {
        return createCertificate(commonName, validityDays, exportFile, exportPassword, null);
    }

    /**
     * This method implements the public one, but offers an additional parameter which is only used
     * when creating a new CA, namely the export alias to use.
     * 
     * @param commonName @see #createCertificate(String, int, String, String)
     * @param validityDays @see #createCertificate(String, int, String, String)
     * @param exportFile @see #createCertificate(String, int, String, String)
     * @param exportPassword @see #createCertificate(String, int, String, String)
     * @param exportAlias If this additional parameter is null, a default value will be used as the
     *        "friendly name" in the PKCS12 file.
     * @return @see #createCertificate(String, int, String, String)
     * 
     * @see #X509CertificateGenerator(boolean)
     */
    protected boolean createCertificate(String commonName, int validityDays, String exportFile, String exportPassword, String exportAlias) throws IOException, InvalidKeyException, SecurityException,
            SignatureException, NoSuchAlgorithmException, DataLengthException, CryptoException, KeyStoreException, CertificateException, InvalidKeySpecException {
        if (commonName == null || exportFile == null || exportPassword == null || validityDays < 1) {
            throw new IllegalArgumentException("Can not work with null parameter");
        }

        System.out.println("Generating certificate for distinguished common subject name '" + commonName + "', valid for " + validityDays + " days");
        SecureRandom sr = new SecureRandom();

        // the JCE representation
        PublicKey pubKey;
        PrivateKey privKey;

        // the BCAPI representation
        RSAPrivateCrtKeyParameters privateKey = null;

        System.out.println("Creating RSA keypair");
        // generate the keypair for the new certificate

        RSAKeyPairGenerator gen = new RSAKeyPairGenerator();
        // TODO: what are these values??
        gen.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), sr, 1024, 80));
        AsymmetricCipherKeyPair keypair = gen.generateKeyPair();
        System.out.println("Generated keypair, extracting components and creating public structure for certificate");
        RSAKeyParameters publicKey = (RSAKeyParameters) keypair.getPublic();
        privateKey = (RSAPrivateCrtKeyParameters) keypair.getPrivate();
        // used to get proper encoding for the certificate
        RSAPublicKeyStructure pkStruct = new RSAPublicKeyStructure(publicKey.getModulus(), publicKey.getExponent());
        System.out.println("New public key is '" + new String(Hex.encode(pkStruct.getEncoded())) + ", exponent=" + publicKey.getExponent() + ", modulus=" + publicKey.getModulus());
        // TODO: these two lines should go away
        // JCE format needed for the certificate - because getEncoded() is necessary...
        pubKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(publicKey.getModulus(), publicKey.getExponent()));
        // and this one for the KeyStore
        privKey = KeyFactory.getInstance("RSA").generatePrivate(
                new RSAPrivateCrtKeySpec(publicKey.getModulus(), publicKey.getExponent(), privateKey.getExponent(), privateKey.getP(), privateKey.getQ(), privateKey.getDP(), privateKey.getDQ(),
                        privateKey.getQInv()));

        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.DAY_OF_YEAR, validityDays);

        X500Name x509Name = new X500Name("CN=" + commonName);

        V3TBSCertificateGenerator certGen = new V3TBSCertificateGenerator();
        certGen.setSerialNumber(new DERInteger(BigInteger.valueOf(System.currentTimeMillis())));
        if (caCert != null) {
            // Attention: this is a catch! Just using
            // "new X509Name(caCert.getSubjectDN().getName())" will not work!
            // I don't know why, because the issuerDN strings look similar with both versions.
            certGen.setIssuer(PrincipalUtil.getSubjectX509Principal(caCert));
        } else {
            // aha, no CA set, which means that we should create a self-signed certificate (called
            // from createCA)
            certGen.setIssuer(x509Name);
        }
        certGen.setSubject(x509Name);

        // TODO GM:
        DERObjectIdentifier sigOID = PKCSObjectIdentifiers.sha1WithRSAEncryption;// DERObjectIdentifier.
                                                                                 // X509Util.getAlgorithmOID(CertificateSignatureAlgorithm);
        AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID, new DERNull());
        certGen.setSignature(sigAlgId);
        // certGen.setSubjectPublicKeyInfo(new SubjectPublicKeyInfo(sigAlgId,
        // pkStruct.toASN1Object()));
        // TODO: why does the coding above not work? - make me work without PublicKey class
        certGen.setSubjectPublicKeyInfo(new SubjectPublicKeyInfo((ASN1Sequence) new ASN1InputStream(new ByteArrayInputStream(pubKey.getEncoded())).readObject()));
        certGen.setStartDate(new Time(new Date(System.currentTimeMillis())));
        certGen.setEndDate(new Time(expiry.getTime()));

        // These X509v3 extensions are not strictly necessary, but be nice and provide them...
        Hashtable extensions = new Hashtable();
        Vector extOrdering = new Vector();
        addExtensionHelper(X509Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(pubKey), extOrdering, extensions);
        if (caCert != null) {
            // again: only if we have set CA
            addExtensionHelper(X509Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert), extOrdering, extensions);
        } else {
            // but if we create a new self-signed cert, set its capability to be a CA
            // this is a critical extension (true)!
            addExtensionHelper(X509Extension.basicConstraints, true, new BasicConstraints(0), extOrdering, extensions);
        }
        certGen.setExtensions(new X509Extensions(extOrdering, extensions));

        System.out.println("Certificate structure generated, creating SHA1 digest");
        // attention: hard coded to be SHA1+RSA!
        SHA1Digest digester = new SHA1Digest();
        AsymmetricBlockCipher rsa = new PKCS1Encoding(new RSAEngine());
        TBSCertificateStructure tbsCert = certGen.generateTBSCertificate();

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(tbsCert);

        // and now sign
        byte[] signature;

        byte[] certBlock = bOut.toByteArray();
        // first create digest
        System.out.println("Block to sign is '" + new String(Hex.encode(certBlock)) + "'");
        digester.update(certBlock, 0, certBlock.length);
        byte[] hash = new byte[digester.getDigestSize()];
        digester.doFinal(hash, 0);
        // and sign that
        if (caCert != null) {
            rsa.init(true, caPrivateKey);
        } else {
            // no CA - self sign
            System.out.println("No CA has been set, creating self-signed certificate as a new CA");
            rsa.init(true, privateKey);
        }
        DigestInfo dInfo = new DigestInfo(new AlgorithmIdentifier(X509ObjectIdentifiers.id_SHA1, null), hash);
        byte[] digest = dInfo.getEncoded(ASN1Encodable.DER);
        signature = rsa.processBlock(digest, 0, digest.length);

        System.out.println("SHA1/RSA signature of digest is '" + new String(Hex.encode(signature)) + "'");

        // and finally construct the certificate structure
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(tbsCert);
        v.add(sigAlgId);
        v.add(new DERBitString(signature));

        X509CertificateObject clientCert = new X509CertificateObject(new X509CertificateStructure(new DERSequence(v)));
        System.out.println("Verifying certificate for correct signature with CA public key");
        /*
         * if (caCert != null) { clientCert.verify(caCert.getPublicKey()); } else {
         * clientCert.verify(pubKey); }
         */

        // and export as PKCS12 formatted file along with the private key and the CA certificate
        System.out.println("Exporting certificate in PKCS12 format");

        PKCS12BagAttributeCarrier bagCert = clientCert;
        // if exportAlias is set, use that, otherwise a default name
        bagCert.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(exportAlias == null ? CertificateExportFriendlyName : exportAlias));
        bagCert.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId, new SubjectKeyIdentifierStructure(pubKey));

        // this does not work as in the example
        /*
         * PKCS12BagAttributeCarrier bagKey = (PKCS12BagAttributeCarrier)privKey;
         * bagKey.setBagAttribute( PKCSObjectIdentifiers.pkcs_9_at_localKeyId, new
         * SubjectKeyIdentifierStructure(tmpKey));
         */

        JDKPKCS12KeyStore store;

        store = new JDKPKCS12KeyStore.BCPKCS12KeyStore();
        store.engineLoad(null, null);

        FileOutputStream fOut = new FileOutputStream(exportFile);
        X509Certificate[] chain;

        if (caCert != null) {
            chain = new X509Certificate[2];
            // first the client, then the CA certificate - this is the expected order for a
            // certificate chain
            chain[0] = clientCert;
            chain[1] = caCert;
        } else {
            // for a self-signed certificate, there is no chain...
            chain = new X509Certificate[1];
            chain[0] = clientCert;
        }

        store.engineSetKeyEntry(exportAlias == null ? KeyExportFriendlyName : exportAlias, privKey, exportPassword.toCharArray(), chain);
        store.engineStore(fOut, exportPassword.toCharArray());

        return true;
    }

    /**
     * This is only a small helper function for adding X.509v3 extensions
     * 
     * @throws IOException
     */
    private void addExtensionHelper(DERObjectIdentifier extId, boolean critical, ASN1Encodable extVal, Vector extensionsOrder, Hashtable extensions) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(extVal);
        extensions.put(extId, new X509Extension(critical, new DEROctetString(bOut.toByteArray())));
        extensionsOrder.addElement(extId);
    }

    private static class PKCS12Content {
        X509Certificate[] certificates;
        String[] certificateAliases;
        RSAPrivateCrtKeyParameters[] privateKeys;
        String[] privateKeyAliases;
    }

    /**
     * This is a helper function for loading from a PKCS12 file.
     * 
     * @param keystore The keystore/PKCS12 file to load from.
     * @param password The password used to encrypt the keystore/PKCS12 file.
     * @param aliasStartsWith If not null, then only those elements of the file/keystore whose alias
     *        starts with this string will be returned.
     * @param useBCAPI Set to true if the Bouncycastle API should be used instead of JCE.
     * @return The elements if they could be successfully loaded, null otherwise.
     */
    private static PKCS12Content loadFromKeyStore(InputStream keystore, String password, String aliasStartsWith, boolean useBCAPI) {
        PKCS12Content ret = new PKCS12Content();

        try {
            Object caKs;
            String[] aliases;
            X509Certificate[] certificates;
            Key[] keys;
            int numKeys = 0, numCertificates = 0, numEntries;

            if (!useBCAPI) {
                caKs = java.security.KeyStore.getInstance("PKCS12");
                ((java.security.KeyStore) caKs).load(keystore, password.toCharArray());

                // extract all known aliases and remember if they are keys or certificates
                Enumeration entries = ((java.security.KeyStore) caKs).aliases();
                numEntries = ((java.security.KeyStore) caKs).size();
                System.out.println("Key store contains " + numEntries + " entries");
                aliases = new String[numEntries];
                certificates = new X509Certificate[numEntries];
                keys = new Key[numEntries];
                for (int i = 0; i < numEntries; i++) {
                    aliases[i] = (String) entries.nextElement();
                    if (aliasStartsWith != null && aliases[i].startsWith(aliasStartsWith)) {
                        System.out.println("Entry " + i + " with alias '" + aliases[i] + "' skipped, does not start with '" + aliasStartsWith + "'");
                        continue;
                    }

                    if (((java.security.KeyStore) caKs).isCertificateEntry(aliases[i])) {
                        System.out.println("Entry " + i + " with alias '" + aliases[i] + "' is a certificate");
                        certificates[i] = (X509Certificate) ((java.security.KeyStore) caKs).getCertificate(aliases[i]);
                        numCertificates++;
                    } else if (((java.security.KeyStore) caKs).isKeyEntry(aliases[i])) {
                        System.out.println("Entry " + i + " with alias '" + aliases[i] + "' is a private key");
                        keys[i] = ((java.security.KeyStore) caKs).getKey(aliases[i], password.toCharArray());
                        numKeys++;
                        // this is a dirty hack - when an entry is marked as a key, it may also have
                        // a certificate with the same alias
                        certificates[i] = (X509Certificate) ((java.security.KeyStore) caKs).getCertificate(aliases[i]);
                        if (certificates[i] != null) {
                            System.out.println("Entry " + i + " with alias '" + aliases[i] + "' also has a certificate");
                            numCertificates++;
                        }
                    } else {
                        System.out.println("Entry " + i + " with alias '" + aliases[i] + "' is neither certificate nor private key, ignoring!");
                    }
                }
            } else {
                caKs = new JDKPKCS12KeyStore.BCPKCS12KeyStore();
                ((JDKPKCS12KeyStore) caKs).engineLoad(keystore, password.toCharArray());

                // extract all known aliases and remember if they are keys or certificates
                Enumeration entries = ((JDKPKCS12KeyStore) caKs).engineAliases();
                numEntries = ((JDKPKCS12KeyStore) caKs).engineSize();
                System.out.println("Key store contains " + numEntries + " entries");
                aliases = new String[numEntries];
                certificates = new X509Certificate[numEntries];
                keys = new Key[numEntries];
                for (int i = 0; i < numEntries; i++) {
                    aliases[i] = (String) entries.nextElement();
                    if (aliasStartsWith != null && !aliases[i].startsWith(aliasStartsWith)) {
                        System.out.println("Entry " + i + " with alias '" + aliases[i] + "' skipped, does not start with '" + aliasStartsWith + "'");
                        continue;
                    }

                    if (((JDKPKCS12KeyStore) caKs).engineIsCertificateEntry(aliases[i])) {
                        System.out.println("Entry " + i + " with alias '" + aliases[i] + "' is a certificate");
                        certificates[i] = (X509Certificate) ((JDKPKCS12KeyStore) caKs).engineGetCertificate(aliases[i]);
                        numCertificates++;
                    } else if (((JDKPKCS12KeyStore) caKs).engineIsKeyEntry(aliases[i])) {
                        System.out.println("Entry " + i + " with alias '" + aliases[i] + "' is a private key");
                        keys[i] = ((JDKPKCS12KeyStore) caKs).engineGetKey(aliases[i], password.toCharArray());
                        numKeys++;
                        // this is a dirty hack - when an entry is marked as a key, it may also have
                        // a certificate with the same alias
                        certificates[i] = (X509Certificate) ((JDKPKCS12KeyStore) caKs).engineGetCertificate(aliases[i]);
                        if (certificates[i] != null) {
                            System.out.println("Entry " + i + " with alias '" + aliases[i] + "' also has a certificate");
                            numCertificates++;
                        }
                    } else {
                        System.out.println("Entry " + i + " with alias '" + aliases[i] + "' is neither certificate nor private key, ignoring!");
                    }
                }
            }

            System.out.println("Finished reading from key store, found " + numCertificates + " certificates and " + numKeys + " keys with matching aliases");
            // now that we know how many elements we have, initialize
            ret.certificates = new X509Certificate[numCertificates];
            ret.privateKeys = new RSAPrivateCrtKeyParameters[numKeys];
            ret.certificateAliases = new String[numCertificates];
            ret.privateKeyAliases = new String[numKeys];
            // and copy
            int outCertInd = 0;
            for (int i = 0; i < numEntries; i++) {
                if (certificates[i] != null) {
                    ret.certificates[outCertInd] = certificates[i];
                    ret.certificateAliases[outCertInd++] = aliases[i];
                }
            }
            int outKeyInd = 0;
            for (int i = 0; i < numCertificates; i++) {
                if (keys[i] != null) {
                    RSAPrivateCrtKey privKey = (RSAPrivateCrtKey) keys[i];
                    ret.privateKeys[outKeyInd] = new RSAPrivateCrtKeyParameters(privKey.getModulus(), privKey.getPublicExponent(), privKey.getPrivateExponent(), privKey.getPrimeP(),
                            privKey.getPrimeQ(), privKey.getPrimeExponentP(), privKey.getPrimeExponentQ(), privKey.getCrtCoefficient());
                    ret.privateKeyAliases[outKeyInd++] = aliases[i];
                }
            }

            return ret;
        } catch (IOException e) {
            System.out.println("Could not load from key store: " + e);
            return null;
        } catch (KeyStoreException e) {
            System.out.println("Could not load from key store: " + e);
            return null;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Could not load from key store: " + e);
            return null;
        } catch (CertificateException e) {
            System.out.println("Could not load from key store: " + e);
            return null;
        } catch (UnrecoverableKeyException e) {
            System.out.println("Could not load from key store: " + e);
            return null;
        }
    }

    /**
     * This is a helper function for fetching the distinguished name from a certificate.
     * 
     * @param keystore The keystore/PKCS12 file to load from.
     * @param password The password used to encrypt the keystore/PKCS12 file.
     * @param alias The alias used to store the certificate in the keystore/PKCS12 file.
     * @param useBCAPI Set to true if the Bouncycastle API should be used instead of JCE.
     * @return The subject DN if the certificate could be successfully loaded, null otherwise.
     */
    public static String getCertificateDistinguishedName(InputStream keystore, String password, String alias, boolean useBCAPI) {
        PKCS12Content content = loadFromKeyStore(keystore, password, alias, useBCAPI);
        if (content == null || content.certificates == null || content.certificates[0] == null)
            return null;
        return content.certificates[0].getSubjectDN().toString();
    }

    /**
     * This is a helper function for fetching the validity from a certificate.
     * 
     * @param keystore The keystore/PKCS12 file to load from.
     * @param password The password used to encrypt the keystore/PKCS12 file.
     * @param alias The alias used to store the certificate in the keystore/PKCS12 file.
     * @param useBCAPI Set to true if the Bouncycastle API should be used instead of JCE.
     * @return The validity in days (from now) if the certificate could be loaded successfully, -1
     *         otherwise
     */
    public static int getCertificateValidity(InputStream keystore, String password, String alias, boolean useBCAPI) {
        PKCS12Content content = loadFromKeyStore(keystore, password, alias, useBCAPI);
        if (content == null || content.certificates == null || content.certificates[0] == null)
            return -1;
        return (int) ((content.certificates[0].getNotAfter().getTime() - System.currentTimeMillis()) / 1000 / 3600 / 24);
    }

    /**
     * The test CA can e.g. be created with
     * 
     * Hmm, this CA doesn't work - look at the Javadoc comment for the constructor for how to create
     * it correctly. echo -e
     * "AT\nUpper Austria\nSteyr\nMy Organization\nNetwork tests\nTest CA certificate\nme@myserver.com\n\n\n"
     * | \ openssl req -new -x509 -outform PEM -newkey rsa:2048 -nodes -keyout /tmp/ca.key -keyform
     * PEM -out /tmp/ca.crt -days 365; echo "test password" | openssl pkcs12 -export -in /tmp/ca.crt
     * -inkey /tmp/ca.key -out ca.p12 -name "Test CA" -passout stdin
     * 
     * The created certificate can be displayed with
     * 
     * openssl pkcs12 -nodes -info -in test.p12 > /tmp/test.cert && openssl x509 -noout -text -in
     * /tmp/test.cert
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // if (args.length > 0 && args[0].equals("newca")) {
            System.out.println(X509CertificateGenerator.createNewCa("My Test CA", 365, "ca.p12", "test password", "Test CA", true));
        // return;
        // }

        String cn = "Test CN";
        if (args.length > 0)
            cn = args[0];

        System.out.println(new X509CertificateGenerator("ca.p12", "test password", "Test CA", true).createCertificate(cn, 180, "test.p12", "test"));
    }
}

/*
 * Here we go again, new year, new experiences, new ideas, new cognitions, new strength â€¦ :) Here
 * is a piece of code that I have written for my thesis to convert a RSA key that have been created
 * with BC into an OpenSSL readable PEM Format. This was a little bit tricky but it works.
 * 
 * private String getPublicKeyPEM() throws IOException { int line_length = 64; // PEM-encoded data
 * has 64-character lines int length, remaining, position=0; String pem_encoded_data = "";
 * 
 * RSAPublicKeyStructure rpks = new RSAPublicKeyStructure(RSApubKey.getModulus(),
 * RSApubKey.getExponent()); String key_pem_format = new
 * String(Base64.encode(rpks.getDERObject().getEncoded())); key_pem_format = key_pem_format.trim();
 * length = key_pem_format.length(); remaining = length - position; while (remaining > line_length)
 * { pem_encoded_data += key_pem_format.substring(position, position + line_length) + "\n";
 * remaining -= line_length; position += line_length; } if (position < length) pem_encoded_data +=
 * key_pem_format.substring(position) + "\n";
 * 
 * key_pem_format = "â€”â€“BEGIN RSA PUBLIC KEYâ€”â€“\n"+ pem_encoded_data+
 * "â€”â€“END RSA PUBLIC KEYâ€”â€“\n";
 * 
 * return key_pem_format; }
 */