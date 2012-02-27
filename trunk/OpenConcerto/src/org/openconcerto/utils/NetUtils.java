/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class NetUtils {

    /**
     * Whether the passed address refers to this computer.
     * 
     * @param addr an ip or dns address, eg "192.168.28.52".
     * @return <code>true</code> if <code>addr</code> is bound to an interface of this computer.
     */
    static public final boolean isSelfAddr(String addr) {
        if (addr == null)
            return false;
        if (addr.startsWith("127.") || addr.startsWith("localhost"))
            return true;

        try {
            final Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                final NetworkInterface ni = en.nextElement();
                final Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    final InetAddress inetAddress = addresses.nextElement();
                    if (addr.startsWith(inetAddress.getHostAddress()))
                        return true;
                }
            }
            return false;
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class CustomizedHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public static final String getHTTPContent(String address, boolean useCustomVerifier) {
        String content = "";
        OutputStream out = null;
        HttpsURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new ByteArrayOutputStream();
            conn = (HttpsURLConnection) url.openConnection();
            // Sur la connexion
            if (useCustomVerifier) {
                conn.setHostnameVerifier(new CustomizedHostnameVerifier());
                // ou globalement
                // HttpsURLConnection.setDefaultHostnameVerifier(new CustomizedHostnameVerifier());
            }

            in = conn.getInputStream();
            final byte[] buffer = new byte[1024];
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
}
