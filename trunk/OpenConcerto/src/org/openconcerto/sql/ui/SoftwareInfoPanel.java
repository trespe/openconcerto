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
import org.openconcerto.sql.TM;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.SystemInfoPanel;
import org.openconcerto.ui.component.HTMLTextField;
import org.openconcerto.utils.ProductInfo;
import org.openconcerto.utils.i18n.I18nUtils;

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
        final FormLayouter l = new FormLayouter(this, 1);

        final UserRightsManager userRightsManager = UserRightsManager.getInstance();
        l.add(TM.tr("infoPanel.rights"), new JLabel(org.openconcerto.utils.i18n.TM.tr(I18nUtils.getYesNoKey(userRightsManager != null))));
        final User user = UserManager.getUser();
        if (user != null) {
            final UserRights userRights = UserRightsManager.getCurrentUserRights();
            final String userS = user.toString() + (userRights.isSuperUser() ? " (superuser)" : "");
            l.add(org.openconcerto.utils.i18n.TM.tr("user"), new JLabel(userS));
        }

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

        final String name, version;
        if (productInfo == null) {
            name = TM.tr("infoPanel.noAppName");
            version = TM.tr("infoPanel.noVersion");
        } else {
            name = productInfo.getName();
            version = productInfo.getProperty(ProductInfo.VERSION, TM.tr("infoPanel.noVersion"));
        }
        l.add(TM.tr("infoPanel.appName"), new JLabel(name));
        l.add(TM.tr("infoPanel.version"), new JLabel(version));
        if (propsConf != null && propsConf.isUsingSSH()) {
            l.add(TM.tr("infoPanel.secureLink"), new JLabel(propsConf.getWanHostAndPort()));
        }
        l.add(TM.tr("infoPanel.dbURL"), new JLabel(conf.getSystemRoot().getDataSource().getUrl()));
        final String logs = propsConf == null ? "" : " ; " + SystemInfoPanel.getLink(TM.tr("infoPanel.logs"), propsConf.getLogDir().toURI());
        l.add(TM.tr("infoPanel.dirs"), new HTMLTextField(SystemInfoPanel.getLink(TM.tr("infoPanel.docs"), conf.getWD().toURI()) + logs));
    }
}
