package org.openconcerto.modules.finance.payment.ebics.request;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.apache.poi.util.HexDump;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.HexEncoder;
import org.bouncycastle.util.encoders.HexTranslator;
import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.EbicsUtil;
import org.openconcerto.modules.finance.payment.ebics.Host;
import org.openconcerto.modules.finance.payment.ebics.OrderType;
import org.openconcerto.modules.finance.payment.ebics.Partner;
import org.openconcerto.modules.finance.payment.ebics.User;
import org.openconcerto.modules.finance.payment.ebics.crypto.XMLSigner;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public class EbicsRequest {

    private EbicsConfiguration config;
    private String type;
    private String encodedData;
    private static SecureRandom random;
    private static SimpleDateFormat sdfIso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private String xml = null;
    static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    public EbicsRequest(EbicsConfiguration config, String type) {
        this.config = config;
        this.type = type;
        if (random == null) {
            try {
                random = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void setEncodedData(String s) {
        this.encodedData = s;
    }

    public String getEncodedData() {
        return encodedData;
    }

    public Document getXMLDocument() {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final DOMImplementation impl = builder.getDOMImplementation();

            final Document doc = impl.createDocument(null, null, null);
            String reqType = "ebicsNoPubKeyDigestsRequest";
            if (this.type.equals(OrderType.INI) || this.type.equals(OrderType.HIA)) {
                reqType = "ebicsUnsecuredRequest";
            }
            if (this.type.equals(OrderType.HPD) || this.type.equals(OrderType.FDL) || this.type.equals(OrderType.HAA)) {
                reqType = "ebicsRequest";
            }

            final Element eRequest = doc.createElementNS("http://www.ebics.org/H003", reqType);

            eRequest.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
            eRequest.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            eRequest.setAttribute("xsi:schemaLocation", "http://www.ebics.org/H003 http://www.ebics.org/H003/ebics_keymgmt_request.xsd");
            eRequest.setAttribute("Version", "H003");
            eRequest.setAttribute("Revision", "1");
            doc.appendChild(eRequest);
            // header
            final Element eHeader = doc.createElement("header");
            eHeader.setAttribute("authenticate", "true");
            eRequest.appendChild(eHeader);
            final Element eStatic = doc.createElement("static");
            eHeader.appendChild(eStatic);
            final Element eHostId = doc.createElement("HostID");
            eHostId.setTextContent(this.config.getHost().getHostId());
            eStatic.appendChild(eHostId);

            if (!this.type.equals(OrderType.INI) && !this.type.equals(OrderType.HIA)) {
                final Element eNonce = doc.createElement("Nonce");
                final byte[] bytes = new byte[16];
                random.nextBytes(bytes);
                eNonce.setTextContent(DatatypeConverter.printHexBinary(bytes));
                eStatic.appendChild(eNonce);

                final Element eTypeStamp = doc.createElement("Timestamp");

                eTypeStamp.setTextContent(sdfIso8601.format(new Date()));
                eStatic.appendChild(eTypeStamp);
            }
            final Element ePartnerId = doc.createElement("PartnerID");
            ePartnerId.setTextContent(this.config.getPartner().getPartnerId());
            eStatic.appendChild(ePartnerId);
            final Element eUserId = doc.createElement("UserID");
            eUserId.setTextContent(this.config.getUser().getUserId());
            eStatic.appendChild(eUserId);
            final Element eOrderDetails = doc.createElement("OrderDetails");
            eStatic.appendChild(eOrderDetails);

            final Element eOrderType = doc.createElement("OrderType");
            eOrderType.setTextContent(this.type);
            eOrderDetails.appendChild(eOrderType);

            if (this.type.equals(OrderType.INI) || this.type.equals(OrderType.HIA)) {
                final Element eOrderID = doc.createElement("OrderID");
                // TODO investiguer OrderID
                eOrderID.setTextContent("A101");
                eOrderDetails.appendChild(eOrderID);
            }
            final Element eOrderAttribute = doc.createElement("OrderAttribute");
            // TODO investiguer OrderAttribute
            if (this.type.equals(OrderType.INI) || this.type.equals(OrderType.HIA)) {
                eOrderAttribute.setTextContent("DZNNN");
            } else {
                eOrderAttribute.setTextContent("DZHNN");
            }
            eOrderDetails.appendChild(eOrderAttribute);

            final Element eSecurityMedium = doc.createElement("SecurityMedium");
            // TODO investiguer SecurityMedium
            eSecurityMedium.setTextContent("0000");
            eStatic.appendChild(eSecurityMedium);
            final Element eMutable = doc.createElement("mutable");
            eHeader.appendChild(eMutable);

            // body
            final Element eBody = doc.createElement("body");
            eRequest.appendChild(eBody);
            if (this.encodedData != null) {
                final Element eDataTransfer = doc.createElement("DataTransfer");
                eBody.appendChild(eDataTransfer);

                final Element eOrderData = doc.createElement("OrderData");
                eDataTransfer.appendChild(eOrderData);

                eOrderData.setTextContent(this.encodedData);
            }
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getXML() {
        if (xml == null) {
            try {
                // transform the Document into a String
                DOMSource domSource = new DOMSource(getXMLDocument());
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                java.io.StringWriter sw = new java.io.StringWriter();
                StreamResult sr = new StreamResult(sw);
                transformer.transform(domSource, sr);
                String xmlFormated = sw.toString();
                if (!type.equals("INI") && !type.equals("HIA")) {
                    // XML Signature
                    xml = XMLSigner.sign(xmlFormated, config.getAuthenticationPrivateKey());
                } else {

                    xml = xmlFormated;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }
        return xml;

    }

    /**
     * unencoded bytes
     * 
     * @throws UnsupportedEncodingException
     * */
    public void setData(byte[] bytes) throws UnsupportedEncodingException {
        setEncodedData(EbicsUtil.encodeData(bytes));
    }

    public String send() throws Exception {
        URL url = new URL(config.getHost().getURL());

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        // connection.setSSLSocketFactory(fact);

        connection.setHostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession session) {
                System.out.println(hostname);
                return true;
            }
        });

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        connection.setRequestProperty("Accept", "text/xml");
        byte[] query = getXML().getBytes("UTF-8");
        int queryLength = query.length;
        connection.setRequestProperty("Content-length", String.valueOf(queryLength));
        OutputStream out = connection.getOutputStream();
        out.write(query);
        System.out.println("Sending " + queryLength + " bytes");
        System.out.println("Resp Code:" + connection.getResponseCode());
        System.out.println("Resp Message:" + connection.getResponseMessage());
        System.out.println("====");
        // out.close();
        // read the response
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        InputStream in = connection.getInputStream();
        FileOutputStream fOut = new FileOutputStream("out.xml");
        int ch = 0;
        while ((ch = in.read()) >= 0) {
            fOut.write(ch);
            bOut.write(ch);
        }
        fOut.close();
        bOut.close();
        return bOut.toString("UTF-8");
    }

    public void validate() throws Exception {
        // 1. Lookup a factory for the W3C XML Schema language
        // System.setProperty("jaxp.debug", "0");

        // File schemaLocation = new File("schema/H003/ebics.xsd");
        // System.out.println(schemaLocation.exists());
        File schemaLocationTypes = new File("schema/H003/ebics_request.xsd");
        System.out.println(schemaLocationTypes.exists());
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);// http://www.w3.org/2001/XMLSchema");
            schemaFactory.setResourceResolver(new EbicsResourceResolver("H003"));
            SAXParser parser = null;

            try {
                factory.setSchema(schemaFactory.newSchema(new Source[] { new StreamSource(schemaLocationTypes.getAbsolutePath()) }));
                parser = factory.newSAXParser();

            } catch (SAXException se) {
                System.err.println("SCHEMA : " + se.getMessage()); // problem in the XSD itself
                se.printStackTrace();
                return;
            }

            XMLReader reader = parser.getXMLReader();
            reader.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) throws SAXException {
                    System.err.println("WARNING: " + e.getMessage()); // do nothing
                }

                public void error(SAXParseException e) throws SAXException {
                    System.err.println("ERROR : " + e.getMessage());
                    throw e;
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    System.err.println("FATAL : " + e.getMessage());
                    throw e;
                }
            });
            System.err.println("====PARSING");
            reader.parse(new InputSource(new StringReader(EbicsUtil.getXML(this.getXMLDocument()))));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void insertBankPubKeyDigest(Document d) {
        NodeList eStaticList = d.getElementsByTagName("static");
        Node eStatic = eStaticList.item(0);

        // <BankPubKeyDigests>
        // <Authentication Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"
        // Version="X002">QPET4PGr0R20f5fVIvPwKaYn7fnibFlY9k69RAYT78w=</Authentication>
        // <Encryption Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"
        // Version="E002">bWj+X9kefkuV6X/+mbiVQ3o9wZLevJcFcbS1WMeC554=</Encryption>
        // </BankPubKeyDigests>
        Element elementBankPubKeyDigests = d.createElement("BankPubKeyDigests");
        Element elementAuthentication = d.createElement("Authentication");
        elementAuthentication.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha256");
        elementAuthentication.setAttribute("Version", "X002");

        try {
            elementAuthentication.setTextContent(new String(Base64.encode(EbicsUtil.getPublicKeyHash256(config.loadBankPublicAuthenticationKey()))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        Element elementEncryption = d.createElement("Encryption");
        elementEncryption.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha256");
        elementEncryption.setAttribute("Version", "E002");
        try {
            elementEncryption.setTextContent(new String(Base64.encode(EbicsUtil.getPublicKeyHash256(config.loadBankPublicEncryptionKey()))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        elementBankPubKeyDigests.appendChild(elementAuthentication);
        elementBankPubKeyDigests.appendChild(elementEncryption);
        eStatic.insertBefore(elementBankPubKeyDigests, eStatic.getLastChild());
    }
}
