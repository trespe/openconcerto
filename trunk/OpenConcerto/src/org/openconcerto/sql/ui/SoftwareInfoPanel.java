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
 
 package org.openconcerto.sql.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.SystemInfoPanel;
import org.openconcerto.ui.component.HTMLTextField;
import org.openconcerto.utils.ProductInfo;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A panel displaying various software informations.
 * 
 * @author Sylvain CUAZ
 * @see Configuration
 */
public class SoftwareInfoPanel extends JPanel {

    public SoftwareInfoPanel() {
        final Configuration conf = Configuration.getInstance();
        final PropsConfiguration propsConf;
        final ProductInfo productInfo;
        if (conf instanceof PropsConfiguration) {
            propsConf = (PropsConfiguration) conf;
            productInfo = propsConf.getProductInfo();
        } else {
            propsConf = null;
            productInfo = ProductInfo.getInstance();
        }

        final FormLayouter l = new FormLayouter(this, 1);
        final String name, version;
        if (productInfo == null) {
            name = "inconnu";
            version = "inconnue";
        } else {
            name = productInfo.getName();
            version = productInfo.getProperty(ProductInfo.VERSION, "inconnue");
        }
        l.add("Nom de l'application", new JLabel(name));
        l.add("Version de l'application", new JLabel(version));
        if (propsConf != null && propsConf.isUsingSSH()) {
            l.add("Liaison sécurisée", new JLabel(propsConf.getWanHostAndPort()));
        }
        l.add("URL de base de données", new JLabel(conf.getSystemRoot().getDataSource().getUrl()));
        final String logs = propsConf == null ? "" : " ; " + SystemInfoPanel.getLink("Journaux", propsConf.getLogDir().toURI());
        l.add("Dossiers", new HTMLTextField(SystemInfoPanel.getLink("Documents", conf.getWD().toURI()) + logs));
    }
}
