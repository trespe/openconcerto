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
 
 package org.openconcerto.ui.component.text;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public final class TextComponentUtils {

    static public final JTextComponent getTextComp(Object o) {
        if (o instanceof JTextComponent)
            return (JTextComponent) o;
        else if (o instanceof TextComponent)
            return ((TextComponent) o).getTextComp();
        else
            return null;
    }

    static public final Document getDocument(Object o) {
        final JTextComponent textComp = getTextComp(o);
        if (textComp != null)
            return textComp.getDocument();
        else if (o instanceof DocumentComponent)
            return ((DocumentComponent) o).getDocument();
        else
            return null;
    }

    private TextComponentUtils() {
    }

}
