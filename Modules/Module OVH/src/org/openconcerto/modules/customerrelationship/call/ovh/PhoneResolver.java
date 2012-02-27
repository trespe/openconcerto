package org.openconcerto.modules.customerrelationship.call.ovh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class PhoneResolver {
    public static final String getContent(String address) {
        String content = "";
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new ByteArrayOutputStream();
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;

            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);

            }
            content = out.toString();
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
        return content;
    }

    public static String getInfoFromGoogle(String tel) {
        tel = tel.trim();
        String c = getContent("http://maps.google.com/maps?q=" + tel);
        int i = c.indexOf("app.showMoreInfo");
        if (i > 0) {
            c = c.substring(i);
            c = c.substring(c.indexOf("<span>") + 6);
            c = c.substring(0, c.indexOf("</span>"));
            // Nouvelle string pour que garbage collector fasse son travail
            c = new String(c.toCharArray()).trim();
        } else {
            c = "";
        }
        return c;
    }
}
