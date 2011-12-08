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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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

}
