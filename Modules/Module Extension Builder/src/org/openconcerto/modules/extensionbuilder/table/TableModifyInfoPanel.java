package org.openconcerto.modules.extensionbuilder.table;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.modules.extensionbuilder.ClickableLabel;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.DefaultListModel;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.Tuple2;

public class TableModifyInfoPanel extends JPanel implements Scrollable {
    private Extension extension;

    // TODO: tooltip sur un champs pour indiquer quelles extensions l'utilisent

    public TableModifyInfoPanel(Extension extension, SQLTable t, final TableDescritor desc, final TableModifyLeftPanel leftPanel) {
        this.extension = extension;
        this.setLayout(new GridBagLayout());
        this.setBackground(Color.WHITE);
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;

        Set<SQLField> fields = t.getFields();
        List<SQLField> allFields = new ArrayList<SQLField>();
        for (SQLField sqlField : fields) {
            final String fName = sqlField.getName();
            if (!fName.equalsIgnoreCase("ARCHIVE") && !fName.equalsIgnoreCase("ORDRE")) {
                allFields.add(sqlField);
            }
        }
        Collections.sort(allFields, new Comparator<SQLField>() {

            @Override
            public int compare(SQLField o1, SQLField o2) {

                return o1.getName().compareTo(o2.getName());
            }
        });

        List<SQLTable> lt = new ArrayList<SQLTable>(t.getDBSystemRoot().getGraph().getReferentTables(t));
        Collections.sort(lt, new Comparator<SQLTable>() {

            @Override
            public int compare(SQLTable o1, SQLTable o2) {

                return o1.getName().compareTo(o2.getName());
            }
        });
        if (lt.size() > 0) {
            this.add(new JLabel(lt.size() + " champs des tables suivantes référencent la table " + t.getName() + " :"), c);
            c.gridy++;
            c.weightx = 0;
            for (final SQLTable sqlTable : lt) {
                this.add(new ClickableLabel(sqlTable.getName(), new Runnable() {

                    @Override
                    public void run() {
                        leftPanel.selectTable(sqlTable.getName());

                    }
                }), c);
                c.gridy++;
            }
            c.gridy++;
            c.weightx = 1;
            this.add(new JLabel(" "), c);
            c.gridy++;
        }
        this.add(new JLabel("Cette table contient " + allFields.size() + " champs :"), c);

        c.gridy++;
        for (SQLField sqlField : allFields) {
            String str = "";

            JPanel line = new JPanel();
            line.add(new JLabelBold(sqlField.getName()));
            line.setOpaque(false);
            line.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
            try {
                String label = ComptaPropsConfiguration.getTranslator(t).getLabelFor(sqlField);
                if (label != null) {
                    str += " (" + label + ")";
                }
            } catch (Exception e) {
                // No label...
            }
            String type = sqlField.getTypeDecl().toLowerCase();
            if (type.contains("double") || type.contains("real") || type.contains("float")) {
                type = "nombre décimal";
            } else if (type.contains("int")) {
                type = "nombre entier";
            } else if (type.contains("text") || type.contains("varchar")) {
                type = "texte";
            } else if (type.contains("boolean")) {
                type = "booléen";
            } else if (type.contains("timestamp")) {
                type = "date et heure";
            }
            if (sqlField.isKey() && sqlField.getForeignTable() != null) {
                final SQLTable foreignTable = sqlField.getForeignTable();
                str += " référence vers la table ";
                line.add(new JLabel(str));
                if (foreignTable.getDBRoot().equals(t.getDBRoot())) {
                    line.add(new ClickableLabel(foreignTable.getName(), new Runnable() {
                        @Override
                        public void run() {
                            leftPanel.selectTable(foreignTable.getName());
                        }
                    }));
                } else {
                    line.add(new JLabel(foreignTable.getName()));
                }
                SQLElement e = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(foreignTable);
                if (e != null) {
                    line.add(new JLabel(" (cad " + e.getSingularName() + ")"));
                }

            } else if (sqlField.isPrimaryKey()) {
                str += " clef primaire";
                line.add(new JLabel(str));
            } else {
                str += " de type " + type;
                line.add(new JLabel(str));
            }

            this.add(line, c);
            c.gridy++;
        }

        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        c.weighty = 1;
        c.gridy++;
        this.add(spacer, c);

    }

    Tuple2<JPanel, GridBagConstraints> createEditorList(final TableDescritor desc) {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        List<FieldDescriptor> fields = desc.getFields();
        for (final FieldDescriptor field : fields) {
            addField(desc, p, c, field);
        }
        Tuple2<JPanel, GridBagConstraints> result = Tuple2.create(p, c);
        return result;

    }

    private void addField(final TableDescritor desc, final JPanel p, GridBagConstraints c, final FieldDescriptor field) {
        c.weightx = 1;
        c.gridx = 0;
        final FieldDescriptorEditor editor = new FieldDescriptorEditor(extension, field);
        p.add(editor, c);

        c.gridx++;
        c.weightx = 0;
        final JButton close = new JButton(new ImageIcon(DefaultListModel.class.getResource("close_popup.png")));

        p.add(close, c);
        close.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                desc.remove(field);
                p.remove(editor);
                p.remove(close);
                p.revalidate();
            }
        });
        c.gridy++;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        switch (orientation) {
        case SwingConstants.VERTICAL:
            return visibleRect.height / 10;
        case SwingConstants.HORIZONTAL:
            return visibleRect.width / 10;
        default:
            throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }

    public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        switch (orientation) {
        case SwingConstants.VERTICAL:
            return visibleRect.height;
        case SwingConstants.HORIZONTAL:
            return visibleRect.width;
        default:
            throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }

    public boolean getScrollableTracksViewportWidth() {
        if (getParent() instanceof JViewport) {
            return ((JViewport) getParent()).getWidth() > getPreferredSize().width;
        }
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            return ((JViewport) getParent()).getHeight() > getPreferredSize().height;
        }
        return false;
    }

}
