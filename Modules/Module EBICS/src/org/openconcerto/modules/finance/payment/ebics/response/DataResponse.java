package org.openconcerto.modules.finance.payment.ebics.response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.zip.InflaterInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.util.HexDump;
import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.EbicsUtil;
import org.openconcerto.utils.Base64;
import org.openconcerto.utils.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class DataResponse {
    EbicsConfiguration config;

    public DataResponse(EbicsConfiguration config, String xmlResponse) {
        this.config = config;

        String rawOrderData = null, rawDigest = null, rawTransactionKey = null;

        System.out.println(xmlResponse);
        String rawDataTransfert = EbicsUtil.extractAndKeep(xmlResponse, "<DataTransfer>", "</DataTransfer>");
        System.out.println(rawDataTransfert);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            Document doc = dbf.newDocumentBuilder().parse(new StringInputStream(rawDataTransfert));
            NodeList list = doc.getChildNodes().item(0).getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                String nName = n.getNodeName();
                if (nName.equals("DataEncryptionInfo")) {
                    NodeList list2 = n.getChildNodes();
                    for (int j = 0; j < list2.getLength(); j++) {
                        Node n2 = list2.item(j);
                        String n2Name = n2.getNodeName();
                        System.out.println("-" + n2Name);
                        if (n2Name.equals("EncryptionPubKeyDigest")) {
                            rawDigest = n2.getTextContent().trim();
                        } else if (n2Name.equals("TransactionKey")) {
                            rawTransactionKey = n2.getTextContent().trim();
                        }
                    }
                } else if (nName.equals("OrderData")) {
                    rawOrderData = n.getTextContent().trim();
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        if (rawOrderData == null) {
            throw new IllegalArgumentException("Unable to get OrderData");
        }
        if (rawDigest == null) {
            throw new IllegalArgumentException("Unable to get EncryptionPubKeyDigest");
        }
        if (rawTransactionKey == null) {
            throw new IllegalArgumentException("Unable to get TransactionKey");
        }

        try {
            init(rawOrderData, rawDigest, rawTransactionKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to decode HPB response");
        }

    }

    private void init(String rawOrderData, String rawDigest, String rawTransactionKey) throws Exception {
        System.out.println(rawOrderData);
        System.out.println(rawDigest);
        System.out.println(rawTransactionKey);

        byte[] encryptedTransactionKey = Base64.decode(rawTransactionKey);
        System.out.println("EncryptedTransactionKey hash:" + EbicsUtil.getSAH256HashBase64(encryptedTransactionKey));

        PrivateKey pKey = config.getEncryptionPrivateKey();
        System.out.println("Private Encryption key: " + EbicsUtil.getSAH256HashBase64(pKey.getEncoded()));
        System.out.println("Public Encryption key : " + EbicsUtil.getSAH256HashBase64(config.getEncryptionCertificate().getPublicKey().getEncoded()));
        RSAPublicKey rs = (RSAPublicKey) config.getEncryptionCertificate().getPublicKey();
        System.out.println(HexDump.toHex(rs.getModulus().toByteArray()));
        System.out.println("Private Authentication key: " + EbicsUtil.getSAH256HashBase64(config.getAuthenticationPrivateKey().getEncoded()));
        System.out.println("Public Authentication key : " + EbicsUtil.getSAH256HashBase64(config.getAuthenticationCertificate().getPublicKey().getEncoded()));
        rs = (RSAPublicKey) config.getAuthenticationCertificate().getPublicKey();
        System.out.println(HexDump.toHex(rs.getModulus().toByteArray()));
        System.out.println("Private Signature key: " + EbicsUtil.getSAH256HashBase64(config.getSignaturePrivateKey().getEncoded()));
        System.out.println("Public Signature key : " + EbicsUtil.getSAH256HashBase64(config.getSignatureCertificate().getPublicKey().getEncoded()));
        rs = (RSAPublicKey) config.getSignatureCertificate().getPublicKey();
        System.out.println(HexDump.toHex(rs.getModulus().toByteArray()));
        // Ok DE mais resultat avec le padding Cipher cipher =
        // Cipher.getInstance("RSA/ECB/NoPadding");
        // Ok DE Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, pKey);
        byte[] transactionKey = cipher.doFinal(encryptedTransactionKey);
        System.out.println("Decoded transaction key:" + EbicsUtil.getSAH256HashBase64(transactionKey));
        System.out.println(HexDump.toHex(transactionKey));

        byte[] orderData = Base64.decode(rawOrderData);

        SecretKeySpec skeySpec = new SecretKeySpec(transactionKey, "AES");
        // skeySpec = new SecretKeySpec(realTransactionKey, "DESede");
        Cipher cipher2 = Cipher.getInstance("AES/CBC/NoPadding", "BC");
        // cipher2 = Cipher.getInstance("DESede/CBC/NoPadding", "BC");
        cipher2.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher2.getBlockSize()]));
        byte[] data = cipher2.doFinal(orderData);
        // data = orderData;
        // EbicsUtil.saveToFile(data, new File("data.gz"));
        InflaterInputStream zip = new InflaterInputStream(new ByteArrayInputStream(data));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[32];
        try {
            while (zip.available() > 0) {
                int len = zip.read(buf);
                if (len > 0) {
                    out.write(buf, 0, len);
                }
            }
            out.close();
            zip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Output hash:" + EbicsUtil.getSAH256HashBase64(out.toByteArray()));

        String xml = new String(out.toByteArray());
        System.out.println(xml);
        extractData(xml);
    }

    public abstract void extractData(String xml) throws Exception;

}
