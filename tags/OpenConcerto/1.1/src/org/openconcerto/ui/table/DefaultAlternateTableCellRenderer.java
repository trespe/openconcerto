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
 
 package org.openconcerto.ui.table;

import java.awt.Component;

import javax.swing.JTable;

// use superclass
@Deprecated
public final class DefaultAlternateTableCellRenderer extends AlternateTableCellRenderer {

    // TODO remove : now this to set colours like DefaultTableCellRenderer and not like
    // Alternate 
    public static Component setColors(final Component comp, final JTable table, boolean isSelected, int row) {
        return TableCellRendererUtils.setColors(comp, table, isSelected, DEFAULT_BG_COLOR, null);
    }
}
