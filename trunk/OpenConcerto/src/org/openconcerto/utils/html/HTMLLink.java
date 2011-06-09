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
 
 package org.openconcerto.utils.html;

public class HTMLLink extends HTMLContent{

    private String url;
    private String name;
    private String image;

    public HTMLLink(String url, String name) {
        this(url, name, null);
    }

    public HTMLLink(String url, String name, String image) {
        this.url = url;
        this.name = name;
        this.image = image;
    }

    public String getHTMLCode() {
        if (image == null) {
            return "<a href=\"" + url + "\">" + name + "</a>";
        }
        return "<a href=\"" + url + "\"><img src=\"" + image + "\" border=\"0\"/>" + name + "</a>";
    }
}
