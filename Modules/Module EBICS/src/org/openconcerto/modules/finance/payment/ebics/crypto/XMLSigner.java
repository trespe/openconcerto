package org.openconcerto.modules.finance.payment.ebics.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.implementations.Canonicalizer11_WithComments;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.Base64;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.util.encoders.Hex;
import org.openconcerto.utils.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLSigner {
    private static final String EBICS_JKS = "ebics.jks";

    public static void createKeys(File keyDir) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            final File file = new File(keyDir, EBICS_JKS);
            if (file.exists()) {
                System.err.println("keys alreaded created");
                return;
            }
            int keysize = 2048;
            // AuthenticationPubKey SignaturePubKey EncryptionPubKey
            String keyAlgName = "RSA";
            String sigAlgName = "SHA1WithRSA";

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(keysize, random);
            KeyPair keypair = keyGen.generateKeyPair();

            PrivateKey privKey = keypair.getPrivate();
            PublicKey pubKey = keypair.getPublic();
            Calendar expiry = Calendar.getInstance();
            // 5 ans
            expiry.add(Calendar.DAY_OF_YEAR, 5);

            V3TBSCertificateGenerator certGen = new V3TBSCertificateGenerator();

            certGen.setSerialNumber(new DERInteger(BigInteger.valueOf(System.currentTimeMillis())));

            certGen.setIssuer(new X500Name("CN=Test Certificate"));
            X500Name x509Name = new X500Name("CN=ILM Informatique");
            certGen.setSubject(x509Name);

            DERObjectIdentifier sigOID = PKCSObjectIdentifiers.sha1WithRSAEncryption;// SHA1?

            AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID, new DERNull());

            certGen.setSignature(sigAlgId);

            certGen.setSubjectPublicKeyInfo(new SubjectPublicKeyInfo((ASN1Sequence) new ASN1InputStream(

            new ByteArrayInputStream(pubKey.getEncoded())).readObject()));

            certGen.setStartDate(new Time(new Date(System.currentTimeMillis())));

            certGen.setEndDate(new Time(expiry.getTime()));

            TBSCertificateStructure tbsCert = certGen.generateTBSCertificate();

            X509Certificate[] chain = new X509Certificate[1];
            chain[0] = createCert(tbsCert, sigAlgId);

            char[] keyPass = "openconcerto".toCharArray();

            final String alias = "AuthenticationPubKey";
            keyStore.setKeyEntry(alias, privKey, keyPass, chain);

            // resign so that -ext are applied.
            // doSelfCert(alias, null, sigAlgName);
            keyStore.store(new FileOutputStream(file), "openconcerto".toCharArray());

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static X509Certificate createCert(TBSCertificateStructure tbsCert, AlgorithmIdentifier sigAlgId) throws Exception {
        SHA1Digest digester = new SHA1Digest();
        AsymmetricBlockCipher rsa = new PKCS1Encoding(new RSAEngine());

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DEROutputStream dOut = new DEROutputStream(bOut);
        dOut.writeObject(tbsCert);

        // and now sign
        byte[] signature;

        byte[] certBlock = bOut.toByteArray();
        // first create digest
        digester.update(certBlock, 0, certBlock.length);
        byte[] hash = new byte[digester.getDigestSize()];
        digester.doFinal(hash, 0);
        // and sign that
        System.out.println("XMLSigner.createCert() BUGGGGGGGGGGGG , remove comment here:");
        // rsa.init(true, caPrivateKey);

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
        // clientCert.verify(caCert.getPublicKey());

        return clientCert;
    }

    /**
     * Creates a self-signed certificate, and stores it as a single-element certificate chain.
     */
    /*
     * private void doSelfCert(String alias, String dname, String sigAlgName) throws Exception { if
     * (alias == null) { alias = keyAlias; }
     * 
     * Pair<Key, char[]> objs = recoverKey(alias, storePass, keyPass); PrivateKey privKey =
     * (PrivateKey) objs.fst; if (keyPass == null) keyPass = objs.snd;
     * 
     * // Determine the signature algorithm if (sigAlgName == null) { sigAlgName =
     * getCompatibleSigAlgName(privKey.getAlgorithm()); }
     * 
     * // Get the old certificate Certificate oldCert = keyStore.getCertificate(alias); if (oldCert
     * == null) { MessageFormat form = new MessageFormat(rb.getString("alias.has.no.public.key"));
     * Object[] source = { alias }; throw new Exception(form.format(source)); } if (!(oldCert
     * instanceof X509Certificate)) { MessageFormat form = new
     * MessageFormat(rb.getString("alias.has.no.X.509.certificate")); Object[] source = { alias };
     * throw new Exception(form.format(source)); }
     * 
     * // convert to X509CertImpl, so that we can modify selected fields // (no public APIs
     * available yet) byte[] encoded = oldCert.getEncoded(); X509CertImpl certImpl = new
     * X509CertImpl(encoded); X509CertInfo certInfo = (X509CertInfo) certImpl.get(X509CertImpl.NAME
     * + "." + X509CertImpl.INFO);
     * 
     * // Extend its validity Date firstDate = getStartDate(startDate); Date lastDate = new Date();
     * lastDate.setTime(firstDate.getTime() + validity * 1000L * 24L * 60L * 60L);
     * CertificateValidity interval = new CertificateValidity(firstDate, lastDate);
     * certInfo.set(X509CertInfo.VALIDITY, interval);
     * 
     * // Make new serial number certInfo.set(X509CertInfo.SERIAL_NUMBER, new
     * CertificateSerialNumber(new java.util.Random().nextInt() & 0x7fffffff));
     * 
     * // Set owner and issuer fields X500Name owner; if (dname == null) { // Get the owner name
     * from the certificate owner = (X500Name) certInfo.get(X509CertInfo.SUBJECT + "." +
     * CertificateSubjectName.DN_NAME); } else { // Use the owner name specified at the command line
     * owner = new X500Name(dname); certInfo.set(X509CertInfo.SUBJECT + "." +
     * CertificateSubjectName.DN_NAME, owner); } // Make issuer same as owner (self-signed!)
     * certInfo.set(X509CertInfo.ISSUER + "." + CertificateIssuerName.DN_NAME, owner);
     * 
     * // The inner and outer signature algorithms have to match. // The way we achieve that is
     * really ugly, but there seems to be no // other solution: We first sign the cert, then
     * retrieve the // outer sigalg and use it to set the inner sigalg X509CertImpl newCert = new
     * X509CertImpl(certInfo); newCert.sign(privKey, sigAlgName); AlgorithmId sigAlgid =
     * (AlgorithmId) newCert.get(X509CertImpl.SIG_ALG); certInfo.set(CertificateAlgorithmId.NAME +
     * "." + CertificateAlgorithmId.ALGORITHM, sigAlgid);
     * 
     * certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
     * 
     * CertificateExtensions ext = createV3Extensions(null, (CertificateExtensions)
     * certInfo.get(X509CertInfo.EXTENSIONS), v3ext, oldCert.getPublicKey(), null);
     * certInfo.set(X509CertInfo.EXTENSIONS, ext); // Sign the new certificate newCert = new
     * X509CertImpl(certInfo); newCert.sign(privKey, sigAlgName);
     * 
     * // Store the new certificate as a single-element certificate chain
     * keyStore.setKeyEntry(alias, privKey, (keyPass != null) ? keyPass : storePass, new
     * Certificate[] { newCert });
     * 
     * System.err.println("New.certificate.self.signed "); System.err.print(newCert.toString());
     * System.err.println();
     * 
     * }
     */
    public static String signOld(String xml, File keyDir) throws Exception {
        // Create a DOM XMLSignatureFactory that will be used to
        // generate the enveloped signature.
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Create a Reference to the enveloped document (in this case,
        // you are signing the whole document, so a URI of "" signifies
        // that, and also specify the SHA1 digest algorithm and
        // the ENVELOPED Transform.

        // TODO: ne signer que authenticate=true
        Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null), Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);

        // Create the SignedInfo.
        SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null), fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                Collections.singletonList(ref));

        // Load the KeyStore and get the signing key and certificate.
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(new File(EBICS_JKS)), "openconcerto".toCharArray());
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry("mykey", new KeyStore.PasswordProtection("changeit".toCharArray()));
        X509Certificate cert = (X509Certificate) keyEntry.getCertificate();

        // Create the KeyInfo containing the X509Data.
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<Object>();
        x509Content.add(cert.getSubjectX500Principal().getName());
        x509Content.add(cert);
        X509Data xd = kif.newX509Data(x509Content);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

        // Instantiate the document to be signed.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new StringInputStream(xml));

        // Create a DOMSignContext and specify the RSA PrivateKey and
        // location of the resulting XMLSignature's parent element.
        DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), doc.getDocumentElement());

        // Create the XMLSignature, but don't sign it yet.
        XMLSignature signature = fac.newXMLSignature(si, ki);

        // Marshal, generate, and sign the enveloped signature.
        signature.sign(dsc);

        // transform the Document into a String
        DOMSource domSource = new DOMSource(doc);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult sr = new StreamResult(sw);
        transformer.transform(domSource, sr);

        return sw.toString();

    }

    public static String sign(String xml, PrivateKey privateKey) throws Exception {
        org.apache.xml.security.Init.init();
        // Create a DOM XMLSignatureFactory that will be used to
        // generate the enveloped signature.
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Create a Reference to the enveloped document (in this case,
        // you are signing the whole document, so a URI of "" signifies
        // that, and also specify the SHA1 digest algorithm and
        // the ENVELOPED Transform.

        // TODO: ne signer que authenticate=true
        Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null), Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);

        // Create the SignedInfo.
        SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null), fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                Collections.singletonList(ref));

        // Load the KeyStore and get the signing key and certificate.
        // final SampleKeyStore sKeyStore = SampleKeyStore.getInstance();

        // Add in the KeyInfo for the certificate that we used the private key of
        // / X509Certificate cert = (X509Certificate) sKeyStore.getAuthenticationCertificate();

        // sig.addKeyInfo(cert);
        // sig.addKeyInfo(cert.getPublicKey());
        // System.out.println("Start signing");
        // sig.sign(sKeyStore.getAuthenticationPrivateKey());

        // Instantiate the document to be signed.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new StringInputStream(xml));

        byte[] authenticatedPart = getAuthenticatedPart(doc);
        System.out.println(new String(authenticatedPart));

        byte[] authenticatedPartDigest = getDigest(authenticatedPart);
        final String sha256Encoded = new String(Hex.encode(authenticatedPartDigest)).toUpperCase();
        System.out.println("SHA-256 HASH: " + sha256Encoded);

        EbicsSignedInfo signedInfo = new EbicsSignedInfo(authenticatedPartDigest);
        String signedInfoXml = signedInfo.getXML();
        System.out.println(signedInfoXml);
        System.out.println("SignedInfo SHA-256 HASH: " + new String(Hex.encode(getDigest(signedInfoXml.getBytes()))));

        byte[] signedInfoSignature = signedInfo.getSignature(privateKey);
        System.out.println(new String(Hex.encode(signedInfoSignature)));
        String sign = Base64.encode(signedInfoSignature, 2048);
        System.out.println(sign);
        String expectedSign = "AD8kXG2bJq0I/LXKiRevMTI/LaD4xYmOX3hGXhoZAaOO8SytCTRj9fPgKm0S86haHRTCRLaYwYMkNHW4RJL6B2JbMdlIt/jOPK3R/jZaUV6UXbquWf11Jav6KW4CNw61VKVOEOdHnVLcpAQWNGJzOZPfJRSFfiJTC9Y6ANMvGjfV9vZjTWT0QecvEPD68NX+XQBNJsIRmvAMPlR1vS3BY58ZRURVB1uVdC9o31Hb73yEDokKYX7+X/6aFw6U6IhZGwqxdSqnq8vQlClL+31yqYqv/VejoaK+VMRHlnTtO3K9HUWphSlGCLEoLEZDgk0O8Vhl147/1P406Fz2tLO69w==";

        Element authSignature = doc.createElement("AuthSignature");

        final Document docSignedInfo = dbf.newDocumentBuilder().parse(new StringInputStream(signedInfoXml));
        Node n = doc.importNode(docSignedInfo.getFirstChild(), true);

        Element e = doc.createElement("ds:SignatureValue");
        e.setTextContent(sign);

        authSignature.appendChild(n);
        authSignature.appendChild(e);
        doc.getFirstChild().insertBefore(authSignature, doc.getFirstChild().getLastChild().getPreviousSibling());

        

        // transform the Document into a String
        DOMSource domSource = new DOMSource(doc);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult sr = new StreamResult(sw);
        transformer.transform(domSource, sr);

        Canonicalizer11_WithComments c = new Canonicalizer11_WithComments();
        String result = new String(c.engineCanonicalize(sw.toString().getBytes()));


        System.out.println("RESULT:");
        System.out.println(result);
        return result;

    }

    private static byte[] getDigest(byte[] authenticatedPart) throws NoSuchAlgorithmException {

        final MessageDigest sha256 = MessageDigest.getInstance("SHA256", new BouncyCastleProvider());
        byte[] result = sha256.digest(authenticatedPart);
        return result;
    }

    private static byte[] getAuthenticatedPart(Document doc) throws XPathExpressionException, CanonicalizationException, IOException {
        String PATH = "//*[@authenticate='true']";
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr = xpath.compile(PATH);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        if (nodes.getLength() < 1) {
            System.out.println("Invalid document, can't find node by PATH: " + PATH);
            return null;
        }

        final Set<Node> l = new HashSet<Node>();
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node item = nodes.item(i);
            l.add(item);

        }
        XMLSignatureInput out = new XMLSignatureInput(nodes.item(0));
        return out.getBytes();
    }
}
