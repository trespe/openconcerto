package org.openconcerto.modules.finance.payment.ebics;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.security.KeyStoreException;
import java.security.cert.Certificate;

import org.openconcerto.utils.FileUtils;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

public class CertificatePDFGenerator {
    private EbicsConfiguration conf;

    public CertificatePDFGenerator(EbicsConfiguration conf) {
        this.conf = conf;
    }

    public File createPDF() throws Exception {
        File f = File.createTempFile("openconcerto_", "cert.pdf");
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(f));

        document.open();

        createPage(document, "X002", "Certificat d'authentification", conf.getAuthenticationCertificate());
        document.newPage();
        createPage(document, "E002", "Certificat de chiffrement", conf.getEncryptionCertificate());
        document.newPage();
        createPage(document, "A005", "Certificat de signature", conf.getSignatureCertificate());

        document.close();
        return f;

    }

    private void createPage(Document document, String type, String name, Certificate cert) throws DocumentException, Exception {

        document.add(new Paragraph(conf.getOrganization()));
        document.add(new Paragraph(conf.getLocality()));
        document.add(new Paragraph(conf.getEmail()));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("UserID: " + conf.getUser().getUserId()));
        document.add(new Paragraph("HostID: " + conf.getHost().getHostId()));
        document.add(new Paragraph("PartnerID: " + conf.getPartner().getPartnerId()));

        final Paragraph paragraph = new Paragraph("Certificat EBICS - " + type);
        paragraph.getFont().setSize(20);
        document.add(paragraph);
        document.add(new Paragraph(" "));
        final Paragraph paragraph2 = new Paragraph(name + " généré par l'ERP Open Source OpenConcerto (www.openconcerto.org)");
        paragraph2.getFont().setSize(11);
        document.add(paragraph2);
        add(document, cert);
    }

    private void add(Document document, Certificate cert) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(o);
        EbicsUtil.dumpCertificate(cert, out);
        out.close();
        o.close();
        BufferedReader reader = new BufferedReader(new StringReader(o.toString()));
        String line = reader.readLine();
        document.add(new Paragraph(" "));
        while (line != null) {
            if (!line.startsWith("SHA")) {
                Font font = new Font(Font.COURIER, 11);
                final Paragraph paragraph = new Paragraph(line, font);

                document.add(paragraph);
            } else {
                document.add(new Paragraph(" "));
                final Paragraph paragraph = new Paragraph(line);
                paragraph.getFont().setSize(10);
                document.add(paragraph);
            }
            line = reader.readLine();
        }
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Je confirme l'utilisation de ce certificat pour nos échanges EBICS."));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Le          /         /                          à"));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Nom et signature"));

    }

    public void openPDF(File f) throws IOException {
        FileUtils.openFile(f);
    }

}
