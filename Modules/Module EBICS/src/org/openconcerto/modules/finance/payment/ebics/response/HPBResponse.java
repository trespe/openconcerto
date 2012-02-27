package org.openconcerto.modules.finance.payment.ebics.response;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.xml.parsers.DocumentBuilderFactory;

import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.EbicsUtil;
import org.openconcerto.utils.Base64;
import org.openconcerto.utils.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class HPBResponse extends DataResponse {

    private RSAPublicKey publicAuthenticationKey;
    private RSAPublicKey publicEncryptionKey;

    public HPBResponse(EbicsConfiguration config, String xmlResponse) {
        super(config, xmlResponse);
    }

    public void extractData(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        Document doc = dbf.newDocumentBuilder().parse(new StringInputStream(xml));
        Node authentication = doc.getElementsByTagName("AuthenticationPubKeyInfo").item(0);
        if (authentication == null) {
            throw new IllegalArgumentException("no AuthenticationPubKeyInfo");
        }
        publicAuthenticationKey = extractPublicKey(authentication);
        Node encryption = doc.getElementsByTagName("EncryptionPubKeyInfo").item(0);
        if (encryption == null) {
            throw new IllegalArgumentException("no EncryptionPubKeyInfo");
        }
        publicEncryptionKey = extractPublicKey(encryption);

    }

    private RSAPublicKey extractPublicKey(Node node) throws InvalidKeySpecException, NoSuchAlgorithmException {
        Node pubKeyValueNode = EbicsUtil.getChildNode(node, "PubKeyValue");
        Node RSAKeyValueNode = EbicsUtil.getChildNode(pubKeyValueNode, "RSAKeyValue");
        Node modulusNode = EbicsUtil.getChildNode(RSAKeyValueNode, "Modulus");
        Node exponentNode = EbicsUtil.getChildNode(RSAKeyValueNode, "Exponent");
        System.out.println("Modulus:" + modulusNode.getTextContent());
        byte[] modulus = Base64.decode(modulusNode.getTextContent());
        byte[] exponent = Base64.decode(exponentNode.getTextContent());
        System.out.println("-----------------" + modulus.length);
        if (modulus.length == 257) {
            byte[] b = new byte[modulus.length - 1];
            System.arraycopy(modulus, 1, b, 0, b.length);
            modulus = b;
        }
        System.out.println("-----------------" + modulus.length);
        RSAPublicKeySpec ks = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(exponent));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKey k = (RSAPublicKey) kf.generatePublic(ks);
        System.out.println(k.getModulus().toByteArray().length);
        return k;
    }

    public RSAPublicKey getPublicAuthenticationKey() {
        return publicAuthenticationKey;
    }

    public RSAPublicKey getPublicEncryptionKey() {
        return publicEncryptionKey;
    }
}
