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
 
 package org.openconcerto.erp.core.sales.pos.model;

public class TicketLine {

    private String text, style;

    public TicketLine(String text, String style) {
        this.text = text;
        this.style = style;
    }

    public String getText() {
        return text;
    }

    public String getStyle() {
        return style;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setStyle(String style) {
        this.style = style;
    }

}
