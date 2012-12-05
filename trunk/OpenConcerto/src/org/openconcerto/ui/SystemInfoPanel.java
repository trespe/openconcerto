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
 
 package org.openconcerto.ui;

import static org.openconcerto.utils.CollectionUtils.join;
import static java.lang.System.getProperty;
import org.openconcerto.ui.component.HTMLTextField;
import org.openconcerto.utils.cc.ITransformer;

import java.io.File;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

/**
 * A panel displaying various system informations (eg vm version, user name, network address).
 * 
 * @author Sylvain CUAZ
 */
public class SystemInfoPanel extends JPanel {

    public SystemInfoPanel() {
        final FormLayouter l = new FormLayouter(this, 1);

        // * Version 1.6.0_13-b03 de Sun Microsystems Inc. ; dossier d'installation
        final String version = getProperty("java.runtime.version") != null ? getProperty("java.runtime.version") : getProperty("java.version");
        URI vendorURI = null;
        try {
            vendorURI = new URI(getProperty("java.vendor.url"));
        } catch (URISyntaxException e1) {
            // tant pis pas de lien
            e1.printStackTrace();
        }
        final Runtime rt = Runtime.getRuntime();
        final String stats = "<i>mémoire :</i> " + formatBytes(rt.freeMemory()) + " / " + formatBytes(rt.totalMemory()) + " ; " + rt.availableProcessors() + " processeur(s)";
        final LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
        final String lafDesc = lookAndFeel == null ? "Aucun thème" : lookAndFeel.getName() + ", " + lookAndFeel.getDescription();

        final JEditorPane p = new HTMLTextField(("Version <b>" + version + "</b> de " + getLink(getProperty("java.vendor"), vendorURI) + " ; "
                + getLink("dossier d'installation", new File(getProperty("java.home")).toURI()) + "<br>" + stats + "<br>" + lafDesc));

        l.add("Java", p);

        // * Windows XP 5.1 (x86)
        l.add("Système d'exploitation", new JLabel("<html><b>" + getProperty("os.name") + "</b> " + getProperty("os.version") + " (" + getProperty("os.arch") + ")</html>"));

        // * Sylvain ; C:\Documents and Settings\Sylvain ; D:\workspace\CTech
        final JEditorPane userPane = new HTMLTextField((getProperty("user.name") + " ; " + getLink("dossier utilisateur", new File(getProperty("user.home")).toURI()) + " ; " + getLink(
                "dossier courant", new File(getProperty("user.dir")).toURI())));
        l.add("Utilisateur", userPane);

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
                    sb.append(" ; <i>état :</i> " + (ni.isUp() ? "actif" : "inactif"));
                    sb.append("<br> <i>nom complet :</i> " + ni.getDisplayName());

                    sb.append("<br> <i>adresse matérielle :</i> ");
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
        l.add("Réseau", new HTMLTextField(join(ifs, "<br>")));

        // TODO reverse vnc
    }

    public static final String getLink(final String name, final URI uri) {
        return uri == null ? name : "<a href=\"" + uri.toString() + "\" >" + name + "</a>";
    }

    private static String formatBytes(long b) {
        return b / 1024 / 1024 + "Mo";
    }
}
