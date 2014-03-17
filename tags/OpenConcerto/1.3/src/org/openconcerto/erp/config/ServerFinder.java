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
 
 package org.openconcerto.erp.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ServerFinder {

    /**
     * @param args
     */
    public static void main(String[] args) {
        ServerFinder finder = new ServerFinder();
        List<InetAddress> networks = finder.getMyLocalAdresses();
        for (InetAddress inetAddress : networks) {
            System.out.println("ServerFinder.main():" + inetAddress.getHostAddress());
        }
        List<String> l = finder.getIPsToScan();
        for (String string : l) {
            System.out.println(string);
        }
        System.out.println(finder.isPortOpen("192.168.1.10", 5432));
        // Check la conf remplie

        // Puis scan
        // Scan PG 5432
        // Scan MySQL 3306
        // Scan H2
    }

    public List<String> getIPsToScan() {
        final List<String> ips = new ArrayList<String>();
        ips.add("127.0.0.1");
        final List<InetAddress> addrs = getMyLocalAdresses();
        for (InetAddress inetAddress : addrs) {
            final String myIp = inetAddress.getHostAddress();
            final int i = myIp.lastIndexOf('.');
            if (i > 0) {
                final String s = myIp.substring(0, i + 1);
                for (int j = 0; j < 255; j++) {
                    final String ip = s + j;
                    if (!ips.contains(ip) && !ip.equals(myIp)) {
                        ips.add(ip);
                    }
                }
            }
        }
        return ips;
    }

    public List<InetAddress> getMyLocalAdresses() {
        List<InetAddress> result = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> en;
        try {
            en = NetworkInterface.getNetworkInterfaces();

            while (en.hasMoreElements()) {
                final NetworkInterface ni = en.nextElement();
                // on mac both the name and the display name are just "vmnet1"
                if (ni.getHardwareAddress() != null && !ni.isLoopback() && !ni.getDisplayName().toLowerCase().contains("vmware") && !ni.getName().toLowerCase().contains("vmnet")) {
                    Enumeration<InetAddress> addrs = ni.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        result.add(addrs.nextElement());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean isPortOpen(String ip, int port) {
        try {

            InetAddress addr = InetAddress.getByName(ip);

            SocketAddress sockaddr = new InetSocketAddress(addr, port);

            // Create an unbound socket
            Socket sock = new Socket();

            int timeoutMs = 2000;
            sock.connect(sockaddr, timeoutMs);
            sock.close();
            System.err.println("->" + ip + " " + port + "open");
            return true;
        } catch (Exception e) {
            // System.err.println("->" + ip + " " + port + "closed" + e.getMessage());
            // e.printStackTrace();
            return false;
        }
    }

    private static int PING_TIMEOUT = 50;

    public static boolean ping(InetAddress host) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {

                final int exit = Runtime.getRuntime().exec("ping -n 1 -w " + PING_TIMEOUT + " " + host.getHostAddress()).waitFor();
                return exit == 0;

            } else {
                final int exit = Runtime.getRuntime().exec("ping -c 1 -w " + PING_TIMEOUT + " " + host.getHostAddress()).waitFor();
                return exit == 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

}
