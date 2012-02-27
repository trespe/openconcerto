package org.openconcerto.modules.customerrelationship.call.ovh;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DurationTableCellRenderer;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JComponentUtils;

public class VoipRecordElement extends ComptaSQLConfElement {

    public VoipRecordElement() {
        super("VOIP_RECORD", "un appel VOIP", "appels VOIP");
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> fields = new ArrayList<String>();
        fields.add("DATE");
        fields.add("TYPE");
        fields.add("NUMBER_FROM");
        fields.add("NUMBER_TO");
        fields.add("FROM");
        fields.add("TO");
        return fields;
    }

    @Override
    protected List<String> getListFields() {
        final List<String> fields = new ArrayList<String>();
        fields.add("DATE");
        fields.add("TYPE");
        fields.add("NUMBER_FROM");
        fields.add("NUMBER_TO");
        fields.add("FROM");
        fields.add("TO");
        fields.add("DESCRIPTION");
        fields.add("DURATION");
        return fields;
    }

    @Override
    protected SQLTableModelSourceOnline createTableSource() {
        final SQLTableModelSourceOnline res = super.createTableSource();
        res.getColumn(getTable().getField("DURATION")).setRenderer(new DurationTableCellRenderer());
        return res;
    }

    @Override
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // De
                c.weightx = 0;
                this.add(new JLabel("De", SwingConstants.RIGHT), c);
                c.gridx++;
                c.weightx = 1;
                final JTextField textDe = new JTextField();
                this.add(textDe, c);
                c.weightx = 0;
                c.gridx++;
                final JTextField telDe = new JTextField(20);
                JComponentUtils.setMinimumWidth(telDe, 120);
                this.add(telDe, c);
                c.gridx++;
                JButton b = new JButton("Rechercher");
                b.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final String text = telDe.getText();
                        final SwingWorker<String, String> worker = new SwingWorker<String, String>() {
                            @Override
                            protected String doInBackground() throws Exception {
                                return PhoneResolver.getInfoFromGoogle(text);
                            }

                            protected void done() {
                                try {
                                    textDe.setText((textDe.getText() + " " + get()).trim());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            };
                        };
                        worker.execute();
                    }
                });
                this.add(b, c);
                // Vers
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(new JLabel("Vers", SwingConstants.RIGHT), c);
                c.gridx++;
                c.weightx = 1;
                final JTextField textVers = new JTextField();
                textVers.setEnabled(false);
                this.add(textVers, c);
                c.gridx++;
                c.weightx = 0;
                final JTextField telVers = new JTextField(20);
                JComponentUtils.setMinimumWidth(telVers, 120);
                this.add(telVers, c);
                c.gridx++;
                JButton b2 = new JButton("Rechercher");
                b2.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final String text = textVers.getText();
                        final SwingWorker<String, String> worker = new SwingWorker<String, String>() {
                            @Override
                            protected String doInBackground() throws Exception {
                                return PhoneResolver.getInfoFromGoogle(text);
                            }

                            protected void done() {
                                try {
                                    textVers.setText((textVers.getText() + " " + get()).trim());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            };
                        };
                        worker.execute();
                    }
                });
                this.add(b2, c);
                // Description
                c.gridx = 0;
                c.gridy++;
                c.weightx = 1;
                c.weighty = 1;
                c.gridwidth = 4;
                c.fill = GridBagConstraints.BOTH;
                JTextArea description = new JTextArea(40, 8);
                description.setFont(textVers.getFont());
                this.add(new JScrollPane(description), c);

                this.addRequiredSQLObject(telDe, "NUMBER_FROM");
                this.addSQLObject(textDe, "FROM");
                this.addRequiredSQLObject(telVers, "NUMBER_TO");
                this.addSQLObject(textVers, "TO");
                this.addSQLObject(description, "DESCRIPTION");
            }
        };
    }
}
