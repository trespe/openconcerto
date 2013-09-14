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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.table.TableCellRendererDecorator;
import org.openconcerto.ui.table.TableCellRendererUtils;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ListeFactureRenderer extends TableCellRendererDecorator {

    // Acompte (bleu ciel)
    public final static Color acompte = new Color(232, 238, 250);
    private final static Color acompteDark = new Color(222, 228, 240);
    private final static Color acompteGrey = acompte.darker();

    // Complementaire (vert pale)
    public final static Color complement = new Color(225, 254, 207);
    private final static Color complementDark = new Color(215, 244, 197);
    private final static Color complementGrey = complement.darker();

    // prévisionnelle (orange)
    public final static Color prev = new Color(253, 243, 204);
    private final static Color prevDark = new Color(243, 233, 194);
    private final static Color prevGrey = prev.darker();

    private static final SQLTable tableModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT").getTable();
    private static final Map<Color, Color> COLORS = new HashMap<Color, Color>();
    static {
        COLORS.put(acompte, acompteDark);
        COLORS.put(complement, complementDark);
        COLORS.put(prev, prevDark);
    }

    static public final TableCellRendererDecoratorUtils<ListeFactureRenderer> UTILS = createUtils(ListeFactureRenderer.class);

    public ListeFactureRenderer() {
        super();
    }

    public ListeFactureRenderer(TableCellRenderer renderer) {
        super(renderer);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = this.getRenderer(table, column).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        AlternateTableCellRenderer.setBGColorMap((JComponent) comp, COLORS);

        final ListSQLLine line = ITableModel.getLine(table.getModel(), row);
        final SQLRowValues rowAt = line.getRow();

        if (rowAt != null) {
            if (rowAt.getBoolean("ACOMPTE") == Boolean.TRUE) {
                comp.setBackground(isSelected ? acompteGrey : acompte);
            } else if (rowAt.getBoolean("COMPLEMENT") == Boolean.TRUE) {
                comp.setBackground(isSelected ? complementGrey : complement);
            } else if (rowAt.getBoolean("PREVISIONNELLE") == Boolean.TRUE) {
                comp.setBackground(isSelected ? prevGrey : prev);
            } else {
                TableCellRendererUtils.setBackgroundColor(comp, table, isSelected);
            }

            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;

                final int realColIndex = table.getColumnModel().getColumn(column).getModelIndex();
                final Set<SQLField> fields = line.getSrc().getParent().getColumn(realColIndex).getFields();
                // System.err.println("Column " + column + " Fields : " + fields);
                if (fields.contains(tableModeReglement.getField("AJOURS"))) {
                    final SQLRowAccessor foreignRow = rowAt.getForeign("ID_MODE_REGLEMENT");
                    if (foreignRow != null && !foreignRow.isUndefined()) {

                        int ajours = (foreignRow.getObject("AJOURS") == null) ? 0 : foreignRow.getInt("AJOURS");
                        int njour = (foreignRow.getObject("LENJOUR") == null) ? 0 : foreignRow.getInt("LENJOUR");

                        if (ajours == 0 && njour == 0) {
                            if (foreignRow.getObject("COMPTANT") != null && !foreignRow.getBoolean("COMPTANT")) {
                                label.setText("Date de facture");
                            } else {
                                label.setText("Comptant");
                            }
                        } else {
                            StringBuffer s = new StringBuffer();
                            if (ajours != 0) {
                                s.append(ajours + ((ajours > 1) ? " jours" : " jour"));
                            }
                            if (njour > 0 && njour < 31) {
                                s.append(" le " + njour);
                            } else {
                                if (njour == 0) {
                                    s.append(" date de facture");
                                } else {
                                    s.append(" fin de mois");
                                }
                            }
                            label.setText(s.toString());
                        }
                    }
                }
            }
        }

        return comp;
    }
}
