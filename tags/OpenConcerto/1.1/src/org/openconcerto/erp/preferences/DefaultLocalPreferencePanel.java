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

import org.openconcerto.sql.Configuration;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;
import org.openconcerto.utils.ExceptionHandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class DefaultLocalPreferencePanel extends DefaultPreferencePanel {
    private final String title, fileName;
    protected final Properties properties = new Properties();

    public DefaultLocalPreferencePanel(final String title, final String fileName) {
        this.title = title;
        this.fileName = fileName;
        if (getPrefFile().exists()) {
            try {
                FileInputStream fIp = new FileInputStream(getPrefFile());
                properties.load(new BufferedInputStream(fIp));
                fIp.close();
            } catch (Exception e) {
                ExceptionHandler.handle("Impossible de lire les préférences pour " + this.getTitleName(), e);
            }
        }

    }

    @Override
    public void storeValues() {
        try {
            FileOutputStream fOp = new FileOutputStream(getPrefFile());
            properties.store(new BufferedOutputStream(fOp), getTitleName());
            fOp.close();
        } catch (Exception e) {
            ExceptionHandler.handle("Impossible de sauvegarde les préférences pour " + this.getTitleName(), e);
        }
    }

    @Override
    public void restoreToDefaults() {
        properties.clear();
    }

    @Override
    public String getTitleName() {
        return title;
    }

    private File getPrefFile() {
        return getPrefFile(this.fileName);
    }

    public static File getPrefFile(String fileName) {
        return new File(Configuration.getInstance().getConfDir(), "/Configuration/" + fileName);
    }
}
