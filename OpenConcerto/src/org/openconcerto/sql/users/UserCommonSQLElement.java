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
 
 package org.openconcerto.sql.users;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.itemview.SimpleRowItemView;
import org.openconcerto.sql.ui.Login;
import org.openconcerto.sql.view.QuickAssignPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.ISpinner;
import org.openconcerto.ui.ISpinnerIntegerModel;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.valuewrapper.TextValueWrapper;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.i18n.I18nUtils;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;

// FIXME Login user unique ?
public class UserCommonSQLElement extends ConfSQLElement {

    /**
     * Set this system property to "true" if this should generate old style passwords.
     */
    public static final String LEGACY_PASSWORDS = "org.openconcerto.sql.legacyPasswords";

    public UserCommonSQLElement() {
        super("USER_COMMON");
        this.setL18nPackageName(I18nUtils.getPackageName(TM.class));
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        l.add("LOGIN");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        return l;
    }

    @Override
    protected ComboSQLRequest createComboRequest() {
        final ComboSQLRequest res = super.createComboRequest();
        res.setFieldSeparator(" ");
        return res;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        return CollectionMap.singleton(null, "PRENOM", "NOM");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    @Override
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private JPasswordField passField, passFieldConfirm;
            private JPanel panelWarning;
            private final SQLElement accessSocieteElem;
            private QuickAssignPanel table;
            // TODO transform into real SQLRowItemView
            private final JTextField encryptedPass = new JTextField();
            private JCheckBox noCompanyLimitCheckBox;

            {
                final SQLTable t = getTable().getDBSystemRoot().getGraph().findReferentTable(getTable(), "ACCES_SOCIETE");
                this.accessSocieteElem = t == null ? null : getDirectory().getElement(t);
            }

            protected final JPasswordField getPassField() {
                return this.passField;
            }

            protected final JPasswordField getPassFieldConfirm() {
                return this.passFieldConfirm;
            }

            @Override
            public void addViews() {
                final GridBagConstraints c = new GridBagConstraints();
                c.insets = new Insets(0, 0, 0, 0);
                c.gridx = 0;
                c.gridy = 0;
                c.weightx = 0;
                c.weighty = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.WEST;

                this.panelWarning = new JPanel(new GridBagLayout());
                this.panelWarning.setBorder(BorderFactory.createEmptyBorder());
                final JLabelWarning labelWarning = new JLabelWarning();
                // labelWarning.setBorder(null);
                this.panelWarning.add(labelWarning, c);
                final JLabel labelTextWarning = new JLabel(TM.tr("user.passwordsDontMatch.short"));
                // labelTextWarning.setBorder(null);
                c.gridx++;
                this.panelWarning.add(labelTextWarning, c);

                final GridBagLayout layout = new GridBagLayout();
                this.setLayout(layout);

                // Login
                c.gridx = 0;
                c.insets = new Insets(2, 2, 1, 2);
                final JLabel labelLogin = new JLabel(getLabelFor("LOGIN"));
                labelLogin.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelLogin, c);
                final JTextField textLogin = new JTextField();
                c.gridx++;
                DefaultGridBagConstraints.lockMinimumSize(textLogin);
                c.weightx = 1;
                this.add(textLogin, c);

                // Warning
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx++;
                c.insets = new Insets(0, 0, 0, 0);
                this.add(this.panelWarning, c);
                this.panelWarning.setVisible(false);

                // Pass
                c.gridy++;
                c.gridwidth = 1;
                c.gridx = 0;
                c.weightx = 0;
                c.insets = new Insets(2, 2, 1, 2);
                final JLabel labelPass = new JLabel(getLabelFor("PASSWORD"));
                labelPass.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelPass, c);
                // JTextField textPass = new JTextField();
                this.passField = new JPasswordField(15);
                c.gridx++;
                c.weightx = 1;
                DefaultGridBagConstraints.lockMinimumSize(this.getPassField());
                this.add(this.getPassField(), c);

                // Confirmation password
                c.gridx++;
                c.weightx = 0;
                final JLabel labelConfirmationPass = new JLabel(getLabelFor("PASSWORD_CONFIRM"));
                labelConfirmationPass.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelConfirmationPass, c);
                this.passFieldConfirm = new JPasswordField(15);
                c.gridx++;
                c.weightx = 1;
                DefaultGridBagConstraints.lockMinimumSize(this.getPassFieldConfirm());
                this.add(this.getPassFieldConfirm(), c);

                // Nom
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                final JLabel labelNom = new JLabel(getLabelFor("NOM"));
                labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNom, c);
                final JTextField textNom = new JTextField();
                c.gridx++;
                c.weightx = 1;
                this.add(textNom, c);

                // Prenom
                c.gridx++;
                c.weightx = 0;
                final JLabel labelPrenom = new JLabel(getLabelFor("PRENOM"));
                labelPrenom.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelPrenom, c);
                final JTextField textPrenom = new JTextField();
                c.gridx++;
                c.weightx = 1;
                this.add(textPrenom, c);

                // Surnom
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                final JLabel labelSurnom = new JLabel(getLabelFor("SURNOM"));
                labelSurnom.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelSurnom, c);
                final JTextField textSurnom = new JTextField();
                c.gridx++;
                c.weightx = 1;
                this.add(textSurnom, c);

                if (this.getTable().contains("ADMIN")) {
                    final JCheckBox checkAdmin = new JCheckBox(getLabelFor("ADMIN"));
                    c.gridx++;
                    c.gridwidth = GridBagConstraints.REMAINDER;
                    c.weightx = 0;
                    this.add(checkAdmin, c);
                    this.addView(checkAdmin, "ADMIN");
                }

                c.gridy++;
                c.gridwidth = 1;
                c.gridx = 0;
                c.weightx = 0;

                if (getTable().contains("MAIL")) {
                    final JLabel labelMail = new JLabel(getLabelFor("MAIL"));
                    labelMail.setHorizontalAlignment(SwingConstants.RIGHT);
                    c.anchor = GridBagConstraints.NORTHWEST;
                    this.add(labelMail, c);
                    c.gridx++;
                    final JTextField textMail = new JTextField();
                    c.gridwidth = GridBagConstraints.REMAINDER;
                    c.weightx = 1;

                    this.add(textMail, c);
                    this.addView(textMail, "MAIL");
                }

                boolean gestionHoraire = false;
                if (Configuration.getInstance().getAppName().startsWith("OpenConcerto") && gestionHoraire) {

                    c.gridwidth = 1;
                    JPanel panelHoraires = new JPanel(new GridBagLayout());
                    GridBagConstraints cH = new DefaultGridBagConstraints();

                    createHalfDay(panelHoraires, cH, "Matin :", "MATIN", 8, 12);
                    createHalfDay(panelHoraires, cH, "Après midi :", "MIDI", 13, 17);

                    c.gridy++;
                    c.gridx = 0;
                    c.gridwidth = GridBagConstraints.REMAINDER;
                    c.weightx = 1;
                    c.weighty = 0;
                    panelHoraires.setBorder(BorderFactory.createTitledBorder("Horaires"));
                    this.add(panelHoraires, c);
                }

                if (this.accessSocieteElem != null) {
                    c.gridy++;
                    c.gridx = 0;
                    c.gridwidth = 4;

                    this.add(new JLabelBold("Accés aux sociétés"), c);
                    c.gridy++;
                    noCompanyLimitCheckBox = new JCheckBox("ne pas limiter l'accès à certaines sociétés");
                    this.add(noCompanyLimitCheckBox, c);

                    c.gridy++;
                    c.weighty = 1;
                    c.fill = GridBagConstraints.BOTH;
                    final SQLElement companyElement = this.accessSocieteElem.getForeignElement("ID_SOCIETE_COMMON");
                    final List<SQLTableElement> tableElements = new ArrayList<SQLTableElement>();
                    tableElements.add(new SQLTableElement(companyElement.getTable().getField("NOM")));
                    final RowValuesTableModel model = new RowValuesTableModel(companyElement, tableElements, companyElement.getTable().getKey(), false);
                    this.table = new QuickAssignPanel(companyElement, "ID", model);
                    this.add(this.table, c);

                    noCompanyLimitCheckBox.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                            table.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
                        }
                    });
                    getView("ADMIN").addValueListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            final boolean selected = evt.getNewValue() == Boolean.TRUE;
                            noCompanyLimitCheckBox.setEnabled(!selected);
                            if (selected)
                                noCompanyLimitCheckBox.setSelected(true);
                        }
                    });
                    noCompanyLimitCheckBox.setSelected(true);
                }

                this.addRequiredSQLObject(textLogin, "LOGIN");
                this.addView(new SimpleRowItemView<String>(new TextValueWrapper(this.encryptedPass)) {
                    @Override
                    public void setEditable(boolean b) {
                        getPassField().setEnabled(b);
                        getPassFieldConfirm().setEnabled(b);
                    };
                }, "PASSWORD", REQ);
                this.addSQLObject(textNom, "NOM");
                this.addSQLObject(textPrenom, "PRENOM");
                this.addSQLObject(textSurnom, "SURNOM");

                this.getPassField().getDocument().addDocumentListener(new SimpleDocumentListener() {
                    @Override
                    public void update(DocumentEvent e) {
                        updateEncrypted();
                        fireValidChange();
                    }
                });
                this.getPassFieldConfirm().getDocument().addDocumentListener(new SimpleDocumentListener() {
                    @Override
                    public void update(DocumentEvent e) {
                        fireValidChange();
                    }
                });
            }

            // après midi arrivée 13:30
            // __________ départ 17:53
            private void createHalfDay(JPanel panelHoraires, GridBagConstraints cH, String label, String field, int startHour, int endHour) {
                panelHoraires.add(new JLabel(label), cH);
                cH.gridx++;
                createTime(panelHoraires, cH, "arrivée", field + "_A", startHour);
                cH.gridy++;
                cH.gridx = 1;
                createTime(panelHoraires, cH, "départ", field + "_D", endHour);
                cH.gridy++;
                cH.gridx = 0;
            }

            // départ 17:53
            private void createTime(JPanel panelHoraires, GridBagConstraints cH, String label, String field, int hour) {
                panelHoraires.add(new JLabel(label), cH);
                cH.gridx++;

                final ISpinner spinHourMA = createSpinner(panelHoraires, cH, true, hour);
                final ISpinner spinMinMA = createSpinner(panelHoraires, cH, false, 0);

                this.addView(new SimpleRowItemView<Integer>(spinHourMA), "HEURE_" + field, null);
                this.addView(new SimpleRowItemView<Integer>(spinMinMA), "MINUTE_" + field, null);
            }

            // 17 h or 53 min
            private ISpinner createSpinner(JPanel panelHoraires, GridBagConstraints cH, final boolean hour, int value) {
                ISpinnerIntegerModel modelHourMA = new ISpinnerIntegerModel(0, hour ? 23 : 59, value);
                ISpinner spinHourMA = new ISpinner(modelHourMA);
                panelHoraires.add(spinHourMA.getComp(), cH);
                cH.gridx++;
                panelHoraires.add(new JLabel(hour ? "h" : "min"), cH);
                cH.gridx++;
                return spinHourMA;
            }

            private void updateEncrypted() {
                final String pass = String.valueOf(this.getPassField().getPassword());
                final String dbPass = Boolean.getBoolean(LEGACY_PASSWORDS) ? '"' + pass + '"' : pass;
                this.encryptedPass.setText(Login.encodePassword(dbPass));
            }

            private boolean checkValidityPassword() {
                final boolean b = String.valueOf(this.getPassField().getPassword()).equalsIgnoreCase(String.valueOf(this.getPassFieldConfirm().getPassword()));
                this.panelWarning.setVisible(!b);
                return b;
            }

            @Override
            public synchronized ValidState getValidState() {
                return super.getValidState().and(ValidState.createCached(checkValidityPassword(), TM.tr("user.passwordsDontMatch")));
            }

            @Override
            public void select(final SQLRowAccessor row) {
                // show something in the user-visible text fields, but do it before the real select
                // so that this.encryptedPass (which will be updated by updateEncrypted() and thus
                // have a bogus value) will be changed to its database value and thus the user can
                // update any field without changing the password.
                if (table != null) {
                    table.getModel().clearRows();
                }
                if (row != null) {
                    final String bogusPass = "bogusPass!";
                    this.getPassField().setText(bogusPass);
                    this.getPassFieldConfirm().setText(bogusPass);
                    // Allowed companies
                    if (this.accessSocieteElem != null) {
                        final SQLTable tableCompanyAccess = this.accessSocieteElem.getTable();
                        SQLRowValues graph = new SQLRowValues(tableCompanyAccess);
                        graph.put("ID", null);
                        graph.putRowValues("ID_SOCIETE_COMMON").put("NOM", null);
                        SQLRowValuesListFetcher f = new SQLRowValuesListFetcher(graph);
                        f.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                            @Override
                            public SQLSelect transformChecked(SQLSelect input) {
                                input.setWhere(new Where(tableCompanyAccess.getField("ID_USER_COMMON"), "=", row.getID()));
                                return input;
                            }
                        });
                        List<SQLRowValues> companies = f.fetch();

                        for (SQLRowValues r : companies) {
                            table.getModel().addRow(r.getForeign("ID_SOCIETE_COMMON").asRowValues());
                        }
                        noCompanyLimitCheckBox.setSelected(companies.isEmpty());
                    }
                }
                super.select(row);
            }

            @Override
            public int insert(SQLRow order) {
                int id = super.insert(order);
                if (this.table != null && !noCompanyLimitCheckBox.isSelected()) {
                    insertCompanyAccessForUser(id);
                }
                return id;
            }

            private void insertCompanyAccessForUser(int id) {
                final SQLTable tableCompanyAccess = this.getDirectory().getElement("ACCES_SOCIETE").getTable();
                int stop = table.getModel().getRowCount();
                for (int i = 0; i < stop; i++) {
                    SQLRowValues rCompany = table.getModel().getRowValuesAt(i);
                    SQLRowValues rAccess = new SQLRowValues(tableCompanyAccess);
                    rAccess.put("ID_USER_COMMON", id);
                    rAccess.put("ID_SOCIETE_COMMON", rCompany.getID());
                    try {
                        rAccess.commit();
                    } catch (SQLException e) {
                        ExceptionHandler.handle("Unable to store company access", e);
                    }
                }
            }

            @Override
            public void update() {
                super.update();
                if (this.table != null) {
                    final SQLTable tableCompanyAccess = this.getDirectory().getElement("ACCES_SOCIETE").getTable();
                    String query = "DELETE FROM " + tableCompanyAccess.getSQL() + " WHERE \"ID_USER_COMMON\" = " + getSelectedID();
                    tableCompanyAccess.getDBSystemRoot().getDataSource().execute(query);
                    if (!noCompanyLimitCheckBox.isSelected()) {
                        insertCompanyAccessForUser(getSelectedID());
                    }
                }
            }
        };
    }
}
