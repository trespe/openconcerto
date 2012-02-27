package org.openconcerto.modules.finance.payment.ebics.request;

import java.io.File;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.apache.xml.security.utils.Base64;
import org.openconcerto.modules.finance.payment.ebics.EbicsConfiguration;
import org.openconcerto.modules.finance.payment.ebics.EbicsUtil;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public class HIARequestOrderData {
    private final EbicsConfiguration config;
    private Date date;
    // Authentication
    private BigInteger modulusAuthentication;
    private BigInteger exponentAuthentication;
    private String certificateAuthentication;
    // Encryption
    private BigInteger modulusEncryption;
    private BigInteger exponentEncryption;
    private String certificateEncryption;

    /**
     * Public authentication and encryption keys
     * 
     * @param encodedX509CertificateAuthentication can be null (except for French bank)
     * @param encodedX509CertificateEncryption can be null (except for French bank)
     * */
    public HIARequestOrderData(EbicsConfiguration config, Date date, BigInteger modulusAuthentication, BigInteger publicExponentAuthentication, String encodedX509CertificateAuthentication,
            BigInteger modulusEncryption, BigInteger publicExponentEncryption, String encodedX509CertificateEncryption) {
        this.config = config;
        this.date = date;
        this.modulusAuthentication = modulusAuthentication;
        this.exponentAuthentication = publicExponentAuthentication;
        this.certificateAuthentication = encodedX509CertificateAuthentication;

        this.modulusEncryption = modulusEncryption;
        this.exponentEncryption = publicExponentEncryption;
        this.certificateEncryption = encodedX509CertificateEncryption;
    }

    public HIARequestOrderData(EbicsConfiguration config) {
        this.config = config;
        this.date = config.getKeyGenerationDate();
        try {
            this.modulusAuthentication = ((RSAKey) config.getAuthenticationCertificate().getPublicKey()).getModulus();
            this.exponentAuthentication = ((RSAPublicKey) config.getAuthenticationCertificate().getPublicKey()).getPublicExponent();
            this.certificateAuthentication = Base64.encode(config.getAuthenticationCertificate().getEncoded());

            this.modulusEncryption = ((RSAKey) config.getEncryptionCertificate().getPublicKey()).getModulus();
            this.exponentEncryption = ((RSAPublicKey) config.getEncryptionCertificate().getPublicKey()).getPublicExponent();
            this.certificateEncryption = Base64.encode(config.getEncryptionCertificate().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Document getXMLDocument() {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final DOMImplementation impl = builder.getDOMImplementation();

            final Document doc = impl.createDocument(null, null, null);

            final Element eHIARequestOrderData = doc.createElementNS("http://www.ebics.org/H003", "HIARequestOrderData");

            eHIARequestOrderData.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
            eHIARequestOrderData.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            eHIARequestOrderData.setAttribute("xsi:schemaLocation", "http://www.ebics.org/H003 http://www.ebics.org/H003/ebics_orders.xsd");
            doc.appendChild(eHIARequestOrderData);

            addAuthentication(doc, eHIARequestOrderData);
            addEncryption(doc, eHIARequestOrderData);

            // PartnerID
            final Element ePartnerID = doc.createElement("PartnerID");
            ePartnerID.setTextContent(this.config.getPartner().getPartnerId());
            eHIARequestOrderData.appendChild(ePartnerID);
            // UserID
            final Element eUserID = doc.createElement("UserID");
            eUserID.setTextContent(this.config.getUser().getUserId());
            eHIARequestOrderData.appendChild(eUserID);
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addAuthentication(final Document doc, final Element eHIARequestOrderData) {
        final Element eAuthenticationPubKeyInfo = doc.createElement("AuthenticationPubKeyInfo");
        eHIARequestOrderData.appendChild(eAuthenticationPubKeyInfo);
        if (this.certificateAuthentication != null) {
            final Element eX509Data = doc.createElement("ds:X509Data");
            final Element eX509IssuerSerial = doc.createElement("ds:X509IssuerSerial");
            eX509Data.appendChild(eX509IssuerSerial);
            final Element eX509IssuerName = doc.createElement("ds:X509IssuerName");
            eX509IssuerName.setTextContent("CN=OpenConcerto");
            eX509IssuerSerial.appendChild(eX509IssuerName);
            final Element eX509SerialNumber = doc.createElement("ds:X509SerialNumber");
            eX509SerialNumber.setTextContent("01");
            eX509IssuerSerial.appendChild(eX509SerialNumber);
            eAuthenticationPubKeyInfo.appendChild(eX509Data);
            final Element eX509Certificate = doc.createElement("ds:X509Certificate");
            eX509Certificate.setTextContent(this.certificateAuthentication);
            eX509Data.appendChild(eX509Certificate);

        }

        // PubKeyValue
        final Element ePubKeyValue = doc.createElement("PubKeyValue");
        eAuthenticationPubKeyInfo.appendChild(ePubKeyValue);
        final Element eRSAKeyValue = doc.createElement("ds:RSAKeyValue");
        final Element eModulus = doc.createElement("ds:Modulus");
        eModulus.setTextContent(new String(Base64.encode(this.modulusAuthentication.toByteArray())));
        eRSAKeyValue.appendChild(eModulus);
        final Element eExponent = doc.createElement("ds:Exponent");
        eExponent.setTextContent(new String(Base64.encode(this.exponentAuthentication.toByteArray())));
        eRSAKeyValue.appendChild(eExponent);
        ePubKeyValue.appendChild(eRSAKeyValue);

        // SignatureVersion
        final Element eAuthenticationVersion = doc.createElement("AuthenticationVersion");
        eAuthenticationVersion.setTextContent("X002");
        eAuthenticationPubKeyInfo.appendChild(eAuthenticationVersion);
    }

    private void addEncryption(final Document doc, final Element eHIARequestOrderData) {
        final Element eAuthenticationPubKeyInfo = doc.createElement("EncryptionPubKeyInfo");
        eHIARequestOrderData.appendChild(eAuthenticationPubKeyInfo);
        if (this.certificateEncryption != null) {
            final Element eX509Data = doc.createElement("ds:X509Data");
            final Element eX509IssuerSerial = doc.createElement("ds:X509IssuerSerial");
            eX509Data.appendChild(eX509IssuerSerial);
            final Element eX509IssuerName = doc.createElement("ds:X509IssuerName");
            eX509IssuerName.setTextContent("CN=OpenConcerto");
            eX509IssuerSerial.appendChild(eX509IssuerName);
            final Element eX509SerialNumber = doc.createElement("ds:X509SerialNumber");
            eX509SerialNumber.setTextContent("01");
            eX509IssuerSerial.appendChild(eX509SerialNumber);
            eAuthenticationPubKeyInfo.appendChild(eX509Data);
            final Element eX509Certificate = doc.createElement("ds:X509Certificate");
            eX509Certificate.setTextContent(this.certificateEncryption);
            eX509Data.appendChild(eX509Certificate);

        }

        // PubKeyValue
        final Element ePubKeyValue = doc.createElement("PubKeyValue");
        eAuthenticationPubKeyInfo.appendChild(ePubKeyValue);
        final Element eRSAKeyValue = doc.createElement("ds:RSAKeyValue");
        final Element eModulus = doc.createElement("ds:Modulus");
        eModulus.setTextContent(new String(Base64.encode(this.modulusEncryption.toByteArray())));
        eRSAKeyValue.appendChild(eModulus);
        final Element eExponent = doc.createElement("ds:Exponent");
        eExponent.setTextContent(new String(Base64.encode(this.exponentEncryption.toByteArray())));
        eRSAKeyValue.appendChild(eExponent);
        ePubKeyValue.appendChild(eRSAKeyValue);

        // SignatureVersion
        final Element eAuthenticationVersion = doc.createElement("EncryptionVersion");
        eAuthenticationVersion.setTextContent("E002");
        eAuthenticationPubKeyInfo.appendChild(eAuthenticationVersion);
    }

    public String getFormatedDate() {
        return SignaturePubKeyOrderData.getFormatedDate(this.date);
    }

    public void validate() throws Exception {
        // 1. Lookup a factory for the W3C XML Schema language
        System.setProperty("jaxp.debug", "1");

        File schemaLocation = new File("schema/ebics_orders_H004.xsd");
        File xmlLocation = new File("test/ebics/INI_order_data.xml");
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            SAXParser parser = null;
            try {
                factory.setSchema(schemaFactory.newSchema(new Source[] { new StreamSource(schemaLocation.getAbsolutePath()) }));
                parser = factory.newSAXParser();
            } catch (SAXException se) {
                System.err.println("SCHEMA : " + se.getMessage()); // problem in the XSD itself
                se.printStackTrace();
                return;
            }

            XMLReader reader = parser.getXMLReader();
            reader.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) throws SAXException {
                    System.out.println("WARNING: " + e.getMessage()); // do nothing
                }

                public void error(SAXParseException e) throws SAXException {
                    System.out.println("ERROR : " + e.getMessage());
                    throw e;
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    System.out.println("FATAL : " + e.getMessage());
                    throw e;
                }
            });
            reader.parse(new InputSource(new StringReader(EbicsUtil.getXML(this.getXMLDocument()))));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
