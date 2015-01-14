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

import static org.openconcerto.utils.CollectionUtils.join;
import static java.lang.System.getProperty;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.i18n.TM;

import java.io.File;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

/**
 * Various system informations (e.g. VM version, user name, network address).
 * 
 * @author Sylvain CUAZ
 */
public final class SystemInfo {

    static public enum Info {
        JAVA, OS, USER, NETWORK
    }

    public static final String CLASS_PROTOCOL = "class";

    static public Map<Info, String> get(final boolean html) {
        final Map<Info, String> res = new HashMap<Info, String>(Info.values().length);

        final String lineBreak = getLineBreak(html);

        // * Version 1.6.0_13-b03 de Sun Microsystems Inc. ; dossier d'installation
        final String version = getProperty("java.runtime.version") != null ? getProperty("java.runtime.version") : getProperty("java.version");
        URI vendorURI = null;
        final LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
        URI lafURI = null;
        try {
            vendorURI = new URI(getProperty("java.vendor.url"));
            lafURI = new URI(CLASS_PROTOCOL, lookAndFeel.getClass().getName(), null);
        } catch (URISyntaxException e1) {
            // tant pis pas de lien
            e1.printStackTrace();
        }
        final Runtime rt = Runtime.getRuntime();
        final String stats = "<i>" + TM.tr("memory") + " :</i> " + formatBytes(rt.freeMemory()) + " / " + formatBytes(rt.totalMemory()) + " ; " + TM.tr("processors", rt.availableProcessors());
        final String lafDesc = lookAndFeel == null ? TM.tr("no.laf") : getLink(lookAndFeel.getName(), lafURI, html) + ", " + lookAndFeel.getDescription();

        final String p = TM.tr("javaVersion", version, getLink(getProperty("java.vendor"), vendorURI, html)) + " ; " + getLink(TM.tr("javaHome"), new File(getProperty("java.home")).toURI(), html)
                + lineBreak + stats + lineBreak + lafDesc;

        res.put(Info.JAVA, p);

        // * Windows XP 5.1 (x86)
        res.put(Info.OS, "<b>" + getProperty("os.name") + "</b> " + getProperty("os.version") + " (" + getProperty("os.arch") + ")");

        // * Sylvain ; C:\Documents and Settings\Sylvain ; D:\workspace\CTech
        res.put(Info.USER,
                getProperty("user.name") + " ; " + getLink(TM.tr("home.dir"), new File(getProperty("user.home")).toURI(), html) + " ; "
                        + getLink(TM.tr("cwd"), new File(getProperty("user.dir")).toURI(), html));

        // * eth0 192.168.28.52/24, état: inactif, nom complet: ""
        final List<String> ifs = new ArrayList<String>();
        try {
            final Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                final NetworkInterface ni = en.nextElement();
                // on mac both the name and the display name are just "vmnet1"
                if (ni.getHardwareAddress() != null && !ni.isLoopback() && !ni.getDisplayName().toLowerCase().contains("vmware") && !ni.getName().toLowerCase().contains("vmnet")) {
                    final StringBuilder sb = new StringBuilder();
                    // don't wrap each nic in a <p> or <li> since it offset the first line
                    sb.append(ni.getName() + " " + join(ni.getInterfaceAddresses(), ", ", new ITransformer<InterfaceAddress, String>() {
                        @Override
                        public String transformChecked(InterfaceAddress input) {
                            return "<b>" + input.getAddress().getHostAddress() + "</b>" + "/" + input.getNetworkPrefixLength();
                        }
                    }));
                    sb.append(" ; <i>" + TM.tr("interfaceState") + " :</i> " + TM.tr(ni.isUp() ? "interfaceStateUp" : "interfaceStateDown"));
                    sb.append(lineBreak);
                    sb.append(" <i>" + TM.tr("interfaceFullName") + " :</i> " + ni.getDisplayName());

                    sb.append(lineBreak);
                    sb.append(" <i>" + TM.tr("hardwareAddress") + " :</i> ");
                    final Formatter fmt = new Formatter(sb);
                    final byte[] mac = ni.getHardwareAddress();
                    for (int i = 0; i < mac.length; i++) {
                        fmt.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : "");
                    }
                    ifs.add(sb.toString());
                }
            }
        } catch (Exception e) {
            // affiche l'erreur
            e.printStackTrace();
            ifs.add(e.getLocalizedMessage());
        }
        res.put(Info.NETWORK, join(ifs, lineBreak));

        return res;
    }

    public static final String getLineBreak(final boolean html) {
        return html ? "<br>" : "\n";
    }

    public static final String getLink(final String name, final URI uri, final boolean html) {
        if (uri == null) {
            return name;
        } else if (html) {
            return "<a href=\"" + uri.toString() + "\" >" + name + "</a>";
        } else {
            return "[" + name + "](" + uri.toString() + ")";
        }
    }

    private static String formatBytes(long b) {
        return TM.tr("megabytes", b / 1024 / 1024);
    }
}
