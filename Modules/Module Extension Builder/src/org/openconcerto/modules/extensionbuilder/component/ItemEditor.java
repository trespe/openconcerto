package org.openconcerto.modules.extensionbuilder.component;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;
import org.openconcerto.ui.group.LayoutHintsBuilder;

public class ItemEditor extends JPanel {

    private JTextField textId;
    private JComboBox comboType;
    private JCheckBox checkSeparated;
    private JCheckBox checkLabel;
    private boolean isEditingGroup;
    private JCheckBox checkFillH;

    public ItemEditor(final Item item, final ComponentDescritor component) {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        if (item instanceof Group) {
            this.isEditingGroup = true;
        }

        if (this.isEditingGroup) {
            // Identifiant du groupe
            c.weightx = 0;
            this.add(new JLabel("Identifiant", SwingConstants.RIGHT), c);
            c.gridx++;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            textId = new JTextField(30);
            this.add(textId, c);
            c.gridy++;
        } else {
            // Label du champs
            c.gridwidth = 2;
            String label = item.getId();
            SQLTable t = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable(component.getTable());
            if (t != null) {
                SQLField field = t.getField(item.getId());
                if (field != null) {
                    final String labelFor = ComptaPropsConfiguration.getTranslator(t).getLabelFor(field);
                    if (labelFor != null) {
                        label += " (" + labelFor + ")";
                    }
                }
            }
            this.add(new JLabelBold(label), c);
            c.gridy++;
        }
        c.gridwidth = 1;

        if (!this.isEditingGroup) {
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel("Type", SwingConstants.RIGHT), c);
            c.gridx++;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            comboType = new JComboBox(new String[] { "normal", "large", "très large" });
            this.add(comboType, c);
            c.gridy++;
        }
        c.gridx = 0;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        final JLabel labelSep = new JLabel("Sépararer le label", SwingConstants.RIGHT);
        if (this.isEditingGroup) {
            labelSep.setText("Dissocier");
        }
        this.add(labelSep, c);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        checkSeparated = new JCheckBox();
        c.weightx = 1;
        this.add(checkSeparated, c);
        c.gridx = 0;
        c.gridy++;
        if (!this.isEditingGroup) {
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0;
            this.add(new JLabel("Afficher le label", SwingConstants.RIGHT), c);
            c.gridx++;
            c.weightx = 1;
            c.fill = GridBagConstraints.NONE;
            checkLabel = new JCheckBox();

            this.add(checkLabel, c);

            c.gridy++;

            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.add(new JLabel("Maximiser la taille", SwingConstants.RIGHT), c);
            c.gridx++;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            checkFillH = new JCheckBox();
            this.add(checkFillH, c);
            c.gridy++;
        }

        JPanel spacer = new JPanel();
        c.weighty = 1;

        this.add(spacer, c);
        initUIFrom(item);

        // Listeners
        if (isEditingGroup) {
            textId.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void removeUpdate(DocumentEvent e) {
                    changedUpdate(e);

                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    changedUpdate(e);

                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    item.setId(textId.getText());
                    component.fireGroupChanged();

                }
            });
        }
        checkSeparated.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                item.setLocalHint(item.getLocalHint().getBuilder().setSeparated(checkSeparated.isSelected()).build());
                component.fireGroupChanged();
            }
        });

        if (!isEditingGroup) {
            checkLabel.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    item.setLocalHint(item.getLocalHint().getBuilder().setShowLabel(checkLabel.isSelected()).build());
                    component.fireGroupChanged();
                }
            });
            checkFillH.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    item.setLocalHint(item.getLocalHint().getBuilder().setFillHeight(checkFillH.isSelected()).build());
                    component.fireGroupChanged();
                }
            });

            comboType.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    int i = comboType.getSelectedIndex();
                    final LayoutHintsBuilder h = item.getLocalHint().getBuilder();
                    if (i == 0) {
                        h.setFillWidth(false);
                        h.setLargeWidth(false);
                    } else if (i == 1) {
                        h.setFillWidth(true);
                        h.setLargeWidth(false);
                    } else if (i == 2) {
                        h.setFillWidth(true);
                        h.setLargeWidth(true);
                    } else {
                        throw new IllegalStateException("No hints for index " + i);
                    }
                    item.setLocalHint(h.build());
                    component.fireGroupChanged();
                }
            });
        }
    }

    private void initUIFrom(Item item) {

        final LayoutHints localHint = item.getLocalHint();

        checkSeparated.setSelected(localHint.isSeparated());

        if (!isEditingGroup) {
            if (localHint.fillWidth() && localHint.largeWidth()) {
                comboType.setSelectedIndex(2);
            } else if (localHint.fillWidth()) {
                comboType.setSelectedIndex(1);
            } else {
                comboType.setSelectedIndex(0);
            }
            checkFillH.setSelected(localHint.fillHeight());
            checkLabel.setSelected(localHint.showLabel());
        } else {
            textId.setText(item.getId());

        }
    }
}
