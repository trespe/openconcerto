package org.openconcerto.modules.finance.payment.ebics.crypto;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.openconcerto.utils.Base64;

public class EbicsSignedInfo {
    static final byte[] RSA_SHA256prefix = new byte[] { 0x30, 0x31, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20 };

    private String digest;

    public EbicsSignedInfo(byte[] authenticatedPartDigest) {
        this.digest = Base64.encodeBytes(authenticatedPartDigest);
    }

    public byte[] getSignature(PrivateKey privateKey) throws Exception {
        if (privateKey == null) {
            throw new IllegalArgumentException("null private key");
        }
        final MessageDigest sha256 = MessageDigest.getInstance("SHA256", new BouncyCastleProvider());
        byte[] signedInfoDigest = sha256.digest(getXML().getBytes());
        byte[] digestToSign = new byte[RSA_SHA256prefix.length + signedInfoDigest.length];
        System.arraycopy(RSA_SHA256prefix, 0, digestToSign, 0, RSA_SHA256prefix.length);
        System.arraycopy(signedInfoDigest, 0, digestToSign, RSA_SHA256prefix.length, signedInfoDigest.length);
        System.out.println("SignedInfo SHA-256 HASH: " + new String(Hex.encode(digestToSign)));

        Signature sig = Signature.getInstance("RSA", "BC");
        sig.initSign(privateKey);
        sig.update(digestToSign);
        byte[] result = sig.sign();
        // System.out.println(new String(result));
        return result;

    }

    public String getXML() {
        StringBuilder b = new StringBuilder();
        b.append("<ds:SignedInfo xmlns=\"http://www.ebics.org/H003\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        b.append("      <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"></ds:CanonicalizationMethod>\n");
        b.append("      <ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"></ds:SignatureMethod>\n");
        b.append("      <ds:Reference URI=\"#xpointer(//*[@authenticate='true'])\">\n");
        b.append("        <ds:Transforms>\n");
        b.append("          <ds:Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"></ds:Transform>\n");
        b.append("        </ds:Transforms>\n");
        b.append("        <ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"></ds:DigestMethod>\n");
        b.append("        <ds:DigestValue>");
        b.append(digest);
        b.append("</ds:DigestValue>\n");
        b.append("      </ds:Reference>\n");
        b.append("    </ds:SignedInfo>");
        return b.toString();
    }
}
