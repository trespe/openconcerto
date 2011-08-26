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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.ui.preferences.TemplateProps;
import org.openconcerto.utils.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;

public class TemplateNXProps extends TemplateProps {

    private static final String societeBaseName = ((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteBaseName();

    @Override
    protected String getPropsFileName() {
        final File f = Configuration.getInstance().getConfDir();
        final File f2 = new File(f, "Configuration" + File.separator + "Template.properties");

        if (!f2.exists()) {
            final InputStream fConf = ComptaBasePropsConfiguration.getStreamStatic("/Configuration/Template.properties");
            if (fConf == null) {
                JOptionPane.showMessageDialog(null, "L'emplacement des modéles n'est pas défini.");
            } else {
                try {
                    StreamUtils.copy(fConf, f2);
                    fConf.close();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,
                            "Impossible de copier le fichier de configuration de l'emplacement des modéles\ndepuis le serveur, veuillez définir l'emplacement des modéles manuellement.");
                    e.printStackTrace();
                }
            }
        }

        if (f2.exists()) {
            return f2.getAbsolutePath();
        } else {
            try {
                f2.getParentFile().mkdirs();
                f2.createNewFile();
            } catch (IOException e) {
                System.err.println(f2);
                e.printStackTrace();
            }
            return f2.getAbsolutePath();
        }
    }

    @Override
    public String getPropertySuffix() {

        return societeBaseName;
    }

    @Override
    public String getDefaultStringValue() {

        final Configuration conf = ComptaPropsConfiguration.getInstance();
        final SQLRow rowSociete = ((ComptaPropsConfiguration) conf).getRowSociete();
        return conf.getWD().getAbsolutePath() + File.separator + rowSociete.getString("NOM") + "-" + rowSociete.getID();
    }

    synchronized public static TemplateProps getInstance() {
        if (instance == null) {
            instance = new TemplateNXProps();
        }
        return instance;
    }
}
