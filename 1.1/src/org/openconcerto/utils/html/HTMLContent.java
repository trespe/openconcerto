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

import java.util.ArrayList;
import java.util.List;

public class HTMLContent {
    List<HTMLContent> content = new ArrayList<HTMLContent>();

    public void add(HTMLContent c) {
        this.content.add(c);
    }

    public void add(String s) {
        this.content.add(new RawContent(s));
    }

    public String getHTMLCode() {
        StringBuilder b = new StringBuilder();
        for (HTMLContent c : content) {
            b.append(c.getHTMLCode());
        }
        return b.toString();
    }

    public void addTitle1(String string) {
        content.add(new RawContent("<h1>" + string + "</h1>"));

    }

    public void addTitle2(String string) {
        content.add(new RawContent("<h2>" + string + "</h2>"));

    }

    public void addTitle3(String string) {
        content.add(new RawContent("<h3>" + string + "</h3>"));

    }

    public void addParagraph(String string) {
        content.add(new RawContent("<p>" + string + "</p>"));

    }

    public void addLine() {
        content.add(new RawContent("<br />"));

    }
}
