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
 
 package org.openconcerto.ui.light;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LightUIDescriptor extends LightUIElement implements Serializable {

    private static final long serialVersionUID = -3399395824294128572L;
    private List<LightUILine> lines = new ArrayList<LightUILine>();

    private String title;
    private List<LightControler> controlers = new ArrayList<LightControler>();

    public LightUIDescriptor(String id) {
        this.setId(id);
        this.setType(TYPE_DESCRIPTOR);
    }

    public void addLine(LightUILine line) {
        this.lines.add(line);
    }

    public LightUILine getLastLine() {
        if (lines.size() == 0) {
            final LightUILine l = new LightUILine();
            lines.add(l);
            return l;
        }
        return lines.get(lines.size() - 1);
    }

    public void dump(PrintStream out) {
        final int size = lines.size();
        out.println(getId() + " : " + title);
        out.println("LightUIDescriptor " + size + " lines ");
        for (int i = 0; i < size; i++) {
            LightUILine line = lines.get(i);
            out.println("LightUIDescriptor line " + i);
            line.dump(out);
            out.println();
        }

    }

    public LightUILine getLine(int i) {
        return this.lines.get(i);
    }

    public int getSize() {
        return lines.size();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addControler(LightControler controler) {
        this.controlers.add(controler);
    }

    public List<LightControler> getControlers() {
        return controlers;
    }

    public void dumpControllers(PrintStream out) {
        out.println("Contollers for id:" + this.getId() + " title: " + title);
        for (LightControler controler : this.controlers) {
            out.println(controler);
        }
        final int size = lines.size();
        out.println(getId() + " : " + title);
        out.println("LightUIDescriptor " + size + " lines ");
        for (int i = 0; i < size; i++) {
            final LightUILine line = lines.get(i);
            for (int j = 0; j < line.getSize(); j++) {
                final LightUIElement e = line.getElement(j);
                if (e instanceof LightUIDescriptor) {
                    ((LightUIDescriptor) e).dumpControllers(out);

                }
            }
        }
    }
}
