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
import org.openconcerto.ui.component.HTMLTextField;
import org.openconcerto.utils.ProductInfo;
import org.openconcerto.utils.SystemInfo;
import org.openconcerto.utils.cc.IFactory;
import org.openconcerto.utils.i18n.I18nUtils;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A panel displaying various software informations.
 * 
 * @author Sylvain CUAZ
 * @see Configuration
 */
public class SoftwareInfoPanel extends JPanel {

    public static enum Info {
        RIGHTS, USER, APP_NAME, APP_VERSION, SECURE_LINK, DB_URL, DIRS
    }

    public static final IFactory<String> FACTORY = new IFactory<String>() {
        @Override
        public String createChecked() {
            return get(false).toString();
        }
    };

    public static Map<Info, String> get(final boolean html) {
        final Map<Info, String> res = new HashMap<Info, String>();

        final UserRightsManager userRightsManager = UserRightsManager.getInstance();
        res.put(Info.RIGHTS, org.openconcerto.utils.i18n.TM.tr(I18nUtils.getYesNoKey(userRightsManager != null)));
        final User user = UserManager.getUser();
        if (user != null) {
            final UserRights userRights = UserRightsManager.getCurrentUserRights();
            res.put(Info.USER, user.toString() + (userRights.isSuperUser() ? " (superuser)" : ""));
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
        res.put(Info.APP_NAME, name);
        res.put(Info.APP_VERSION, version);
        if (propsConf != null && propsConf.isUsingSSH()) {
            res.put(Info.SECURE_LINK, propsConf.getWanHostAndPort());
        }
        res.put(Info.DB_URL, conf.getSystemRoot().getDataSource().getUrl());
        final String logs = propsConf == null ? "" : " ; " + SystemInfo.getLink(TM.tr("infoPanel.logs"), propsConf.getLogDir().toURI(), html);
        res.put(Info.DIRS, SystemInfo.getLink(TM.tr("infoPanel.docs"), conf.getWD().toURI(), html) + logs);

        return res;
    }

    public SoftwareInfoPanel() {
        final Map<Info, String> infos = get(true);
        final FormLayouter l = new FormLayouter(this, 1);

        l.add(TM.tr("infoPanel.rights"), new JLabel(infos.get(Info.RIGHTS)));
        final String user = infos.get(Info.USER);
        if (user != null) {
            l.add(org.openconcerto.utils.i18n.TM.tr("user"), new JLabel(user));
        }

        l.add(TM.tr("infoPanel.appName"), new JLabel(infos.get(Info.APP_NAME)));
        l.add(TM.tr("infoPanel.version"), new JLabel(infos.get(Info.APP_VERSION)));
        final String secureLink = infos.get(Info.SECURE_LINK);
        if (secureLink != null) {
            l.add(TM.tr("infoPanel.secureLink"), new JLabel(secureLink));
        }
        l.add(TM.tr("infoPanel.dbURL"), new JLabel(infos.get(Info.DB_URL)));
        l.add(TM.tr("infoPanel.dirs"), new HTMLTextField(infos.get(Info.DIRS)));
    }
}
