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
 
 package org.openconcerto.ui.preferences;

public class EmailProps extends AbstractProps {
    public static final int DEFAULT = 0;
    public static final int THUNDERBIRD = 1;
    public static final int OUTLOOK = 2;
    protected static EmailProps instance;

    public synchronized static EmailProps getInstance() {
        if (instance == null) {
            instance = new EmailProps();
        }
        return instance;
    }

    @Override
    protected String getPropsFileName() {
        return "./Configuration/Email.properties";
    }

    public String getThunderbirdPath() {
        final String stringProperty = this.getStringProperty("thunderbird.path");
        if (stringProperty.trim().length() == 0) {
            return "C:\\Program Files\\Mozilla Thunderbird\\thunderbird.exe";
        }
        return stringProperty;
    }

    public void setThunderbirdPath(String text) {
        this.setProperty("thunderbird.path", text);
    }

    public int getMode() {
        return this.getIntProperty("mode");
    }

    public void setMode(int mode) {
        this.setProperty("mode", String.valueOf(mode));
    }

    public void setTitle(String text) {
        this.setProperty("title", String.valueOf(text));
    }

    public String getTitle() {
        return this.getStringProperty("title");
    }

    public void setHeader(String text) {
        this.setProperty("header", String.valueOf(text));
    }

    public String getHeader() {
        return this.getStringProperty("header");
    }

    public void setFooter(String text) {
        this.setProperty("footer", String.valueOf(text));
    }

    public void setEncodingCharsetName(String charset) {
        this.setProperty("encoding", charset);
    }

    public String getFooter() {
        return this.getStringProperty("footer");
    }

    public String getEncodingCharsetName() {
        String charset = "UTF-8";
        String tmp = this.getStringProperty("encoding");
        if (tmp.trim().length() > 0) {
            charset = tmp;
        }
        return charset;
    }

    @Override
    protected int getDefautIntValue() {
        return DEFAULT;
    }

}
