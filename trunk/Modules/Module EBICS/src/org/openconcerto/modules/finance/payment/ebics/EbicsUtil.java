package org.openconcerto.modules.finance.payment.ebics;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.poi.util.HexDump;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.encoders.HexEncoder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EbicsUtil {

    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";

    public static final String END_CERT = "-----END CERTIFICATE-----";

    public static byte[] decodeData(String encodedData) {
        InflaterInputStream zip = new InflaterInputStream(new ByteArrayInputStream(Base64.decode(encodedData)));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[2048];
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
        return out.toByteArray();
    }

    public static String encodeData(byte[] data) throws UnsupportedEncodingException {
        final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        try {
            DeflaterOutputStream out = new DeflaterOutputStream(bOut);
            out.write(data);
            out.close();
            bOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final byte[] zippedData = bOut.toByteArray();

        return new String(Base64.encode(zippedData));
    }

    public static byte[] getFileContent(final File f) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
        final byte[] buffer = new byte[40960];
        while (in.available() > 0) {
            final int s = in.read(buffer);
            out.write(buffer, 0, s);
        }
        // dispose all the resources after using them.
        in.close();
        return out.toByteArray();
    }

    public static void saveToFile(byte[] b, File f) throws IOException {
        final FileOutputStream fOut = new FileOutputStream(f);
        fOut.write(b);
        fOut.close();
    }

    public static byte[] unixToDos(byte[] fileContent) {
        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (int i = 0; i < fileContent.length; i++) {
            byte c = fileContent[i];
            if (c == '\n') {
                b.write('\r');
            }
            b.write(c);
        }
        return b.toByteArray();
    }

    public static String extract(String string, String start, String stop) {
        int i = string.indexOf(start) + start.length();
        int j = string.indexOf(stop);
        return string.substring(i, j);
    }

    public static String extractAndKeep(String string, String start, String stop) {
        int i = string.indexOf(start);
        int j = string.indexOf(stop) + stop.length();
        return string.substring(i, j);
    }

    /**
     * transform the Document into a String
     * */
    public static String getXML(Document doc) {
        try {

            DOMSource domSource = new DOMSource(doc);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter sw = new StringWriter();
            StreamResult sr = new StreamResult(sw);
            transformer.transform(domSource, sr);
            String xml = sw.toString();
            return xml;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }

    /**
     * Returns the keystore with the configured CA certificates.
     */
    public static final KeyStore getCacertsKeyStore() throws Exception {
        final String sep = File.separator;
        final File file = new File(System.getProperty("java.home") + sep + "lib" + sep + "security" + sep + "cacerts");
        if (!file.exists()) {
            return null;
        }
        FileInputStream fis = null;
        KeyStore caks = null;
        try {
            fis = new FileInputStream(file);
            caks = KeyStore.getInstance("jks");
            caks.load(fis, null);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return caks;
    }

    /**
     * Writes an X.509 certificate in base64 or binary encoding to an output stream.
     */
    public static void dumpCertificate(Certificate cert, PrintStream out) throws IOException, CertificateException {
        final Base64Encoder encoder = new Base64Encoder();
        out.println(BEGIN_CERT);
        final byte[] encoded = cert.getEncoded();
        encoder.encode(encoded, 0, encoded.length, out);
        out.println(END_CERT);
    }

    public static boolean compare(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            System.out.println("Length error: " + b1.length + " != " + b2.length);
            return false;
        }
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                System.out.println("Value error at " + i + " : " + b1[i] + " != " + b2[i]);
                return false;
            }
        }
        return true;
    }

    public static String getSAH256HashBase64(byte[] toHash) throws NoSuchAlgorithmException {
        final MessageDigest sha256 = MessageDigest.getInstance("SHA256", new BouncyCastleProvider());
        byte[] result = sha256.digest(toHash);

        return new String(Base64.encode(result));
    }

    public static byte[] getSHA256(byte[] toHash) {
        byte[] result;
        try {
            final MessageDigest sha256 = MessageDigest.getInstance("SHA256", new BouncyCastleProvider());
            result = sha256.digest(toHash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    public static void dumpKey(Key key) {
        RSAKey p = (RSAKey) key;
        System.out.println("PrivateKey modulus: " + HexDump.toHex(p.getModulus().toByteArray()));

    }

    public static Node getChildNode(Node n, String name) {
        if (n == null) {
            throw new IllegalArgumentException("null node");
        }
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }
        NodeList l = n.getChildNodes();
        for (int i = 0; i < l.getLength(); i++) {
            Node node = l.item(i);
            if (name.equals(node.getLocalName())) {
                return node;
            }
        }
        return null;
    }

    /**
     * Remove the first byte of an byte array
     * 
     * @return the array
     * */
    public static byte[] removeFirstByte(byte[] byteArray) {
        byte[] b = new byte[byteArray.length - 1];
        System.arraycopy(byteArray, 1, b, 0, b.length);
        return b;
    }

    /**
     * The SHA-256 hash values of the financial institution's public keys for X002 and E002 are
     * composed by concatenating the exponent with a blank character and the modulus in hexadecimal
     * representation (using lower case letters) without leading zero (as to the hexadecimal
     * representation). The resulting string has to be converted into a byte array based on US ASCII
     * code.In Version “H004” of the EBICS protocol the ES of the financial
     * 
     * Say NO to drugs!
     */
    public static byte[] getPublicKeyHash256(RSAPublicKey publicBankKey) {
        final byte[] byteArray = publicBankKey.getPublicExponent().toByteArray();
        final String s1 = new String(Hex.encode(byteArray));
        final byte[] byteArray2 = publicBankKey.getModulus().toByteArray();
        final String s2 = new String(Hex.encode(removeFirstByte(byteArray2)));
        String s = s1 + " " + s2;
        if (s.charAt(0) == '0') {
            // Amazing optimization
            s = s.substring(1);
        }
        return getSHA256(s.getBytes());
    }
}
