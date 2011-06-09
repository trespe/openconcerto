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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLFile {

    private String content = "";
    private String root;
    private String path;

    /**
     * Fichier HTML original sans modif ni reel parsing, Le but est de ne pas se taper les problèmes
     * HTML
     */
    public HTMLFile(String f) {
        this(new File(f));
    }

    public HTMLFile(File f) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));

            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                content += line;
            }
            reader.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    synchronized public HTMLDiv getDivClass(String divName) {

        return getDiv(divName, "class");
    }

    synchronized public HTMLDiv getDivId(String divName) {

        return getDiv(divName, "id");
    }

    synchronized public HTMLDiv getDiv(String divName, String ref) {

        final String string = "<div.+" + ref + "=\\\"" + divName + "\\\".*?>";
        Pattern p = Pattern.compile(string);

        Matcher m = p.matcher(this.content);
        if (!m.find()) {
            System.err.println("Unable to find:" + string);
            System.err.println("Unable to find in:" + this.content);
            return null;
        }

        String divContent = this.content.substring(m.end());

        int stop = getEnd(divContent, "div");

        HTMLDiv div = new HTMLDiv(divName, divContent.substring(0, stop), ref);

        return div;
    }

    /**
     * @param divContent
     * @param stop
     * @return
     */
    private int getEnd(final String divContent, final String tagName) {

        int level = 0;
        int stop = 0;
        if (tagName.length() > 5) {
            throw new IllegalArgumentException(tagName + "too long");
        }
        final int length = divContent.length();
        for (int i = 0; i < length - 6; i++) {
            final char c1 = divContent.charAt(i);
            final char c2 = divContent.charAt(i + 1);
            final char c3 = divContent.charAt(i + 2);
            final char c4 = divContent.charAt(i + 3);
            final char c5 = divContent.charAt(i + 4);
            final char c6 = divContent.charAt(i + 5);
            final char c7 = divContent.charAt(i + 6);
            final char c8 = divContent.charAt(i + 7);
            String word = "" + c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8;

            word = word.toLowerCase();

            if (word.startsWith("</" + tagName + ">")) {
                level--;
            } else if (word.startsWith("<" + tagName)) {
                level++;
            }

            stop = i;
            if (level < 0) {

                break;
            }
        }

        return stop;
    }

    synchronized public void saveAs(File file) {
        String currentContent = this.content;
        try {
            BufferedWriter fR = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            fR.append(currentContent);
            fR.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized public void saveAs(String s) {
        File file = new File(s);
        saveAs(file);
        this.path = s;
    }

    public void saveAs(String root, String s) {
        saveAs(root + "/" + s);
        this.root = root;
        this.path = s;
    }

    public void setTitle(String s) {
        replaceContent(new HTMLTitle("", ""), s);
    }

    public static void main(String[] args) {
        HTMLFile f = new HTMLFile("c:\\index.html");
        f.setTitle("New feature of Framework Utils:");

        HTMLDiv d = f.getDivClass("story");

        f.replaceContent(d, "<p>HTML manipulation! oh oui</p>");
        f.saveAs("c:\\features.html");

    }

    synchronized public void replaceContent(HTMLElement element, String tagcontent) {
        final String string;
        if (element.getName().length() > 0) {
            string = "<" + element.getTagName() + ".+" + element.getRef() + "=\\\"" + element.getName() + "\\\".*?>";
        } else {
            string = "<" + element.getTagName() + ".*?>";
        }

        Pattern p = Pattern.compile(string);

        Matcher m = p.matcher(this.content);
        if (!m.find()) {
            throw new IllegalStateException("Unable to find:" + string);
        }

        // Copie du div et de ce qu'il y a avant
        int end = m.end();
        String newContent = this.content.substring(0, end);
        // recherche de la fin du div
        String t = this.content.substring(end);
        int endDiv = getEnd(t, element.getTagName());
        // ajout de notre div modifié

        newContent = newContent.concat(tagcontent);
        // ajout du reste
        newContent = newContent.concat(t.substring(endDiv));
        // System.out.println(":"+m.group());
        this.content = newContent;

    }

    public String getContent() {
        return content;
    }

    public void writeTo(OutputStream outputStream) {
        String currentContent = this.content;
        try {
            BufferedWriter fR = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF8"));
            fR.append(currentContent);
            fR.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setBodyId(String id) {

        String string = "<body.*?>";

        this.content = this.content.replaceFirst(string, "<body id=\"" + id + "\">");
    }

    public void replaceContent(HTMLElement element, HTMLContent content2) {
        this.replaceContent(element, content2.getHTMLCode());

    }

    public String getRoot() {
        return root;
    }

    public String getPath() {
        return path;
    }

    public void setDescription(String description) {
        // TODO Auto-generated method stub

    }
}
