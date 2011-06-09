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
 
 package org.openconcerto.erp.core.customerrelationship.customer.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.AdresseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.generationDoc.gestcomm.CourrierClientSheet;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.ISQLElementWithCodeSelector;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class CourrierClientSQLElement extends ComptaSQLConfElement {

    public CourrierClientSQLElement() {
        super("COURRIER_CLIENT", "un courrier", "courriers");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_AFFAIRE");
        l.add("NUMERO");
        l.add("NOM");
        l.add("DATE");
        l.add("ID_SECRETAIRE");
        l.add("INFOS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ADRESSE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            JUniqueTextField textNumero;
            JDate date;
            SQLTextCombo objet;
            ISQLElementWithCodeSelector selAffaire;
            ElementComboBox comboModele = new ElementComboBox();
            ElementComboBox boxAdresse;
            JCheckBox checkImpression = new JCheckBox("Impression");
            JCheckBox checkVisu = new JCheckBox("Visualisation");
            JButton buttonShowDoc;

            public void addViews() {
                DefaultGridBagConstraints c = new DefaultGridBagConstraints();

                this.setLayout(new GridBagLayout());

                // Numéro
                c.fill = GridBagConstraints.NONE;
                this.add(new JLabel(getLabelFor("NUMERO")), c);
                this.textNumero = new JUniqueTextField();
                c.gridx++;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(this.textNumero, c);

                // Date
                c.weightx = 0;
                c.gridx++;
                c.fill = GridBagConstraints.NONE;
                this.add(new JLabel(getLabelFor("DATE")), c);
                this.date = new JDate(true);
                c.weightx = 1;
                c.gridx++;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(this.date, c);

                // Affaire
                c.weightx = 0;
                c.gridy++;
                c.gridx = 0;
                c.fill = GridBagConstraints.NONE;
                this.add(new JLabel(getLabelFor("ID_AFFAIRE")), c);
                final SQLElement eltAffaire = Configuration.getInstance().getDirectory().getElement("AFFAIRE");
                this.selAffaire = new ISQLElementWithCodeSelector(eltAffaire, eltAffaire.getTable().getField("OBJET"));
                c.weightx = 1;
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(this.selAffaire, c);

                // Secretaire
                c.weightx = 0;
                c.gridy++;
                c.gridx = 0;
                c.fill = GridBagConstraints.NONE;
                c.gridwidth = 1;
                this.add(new JLabel(getLabelFor("ID_SECRETAIRE")), c);
                SQLElement eltSecr = Configuration.getInstance().getDirectory().getElement("SECRETAIRE");
                ISQLElementWithCodeSelector selSecr = new ISQLElementWithCodeSelector(eltSecr, eltSecr.getTable().getField("NOM"));
                c.weightx = 1;
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(selSecr, c);

                // Objet
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                c.weightx = 0;
                c.fill = GridBagConstraints.NONE;
                this.add(new JLabel(getLabelFor("NOM")), c);
                c.weightx = 1;
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.objet = new SQLTextCombo();
                this.add(this.objet, c);

                // Modele
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                c.weightx = 0;
                c.fill = GridBagConstraints.NONE;
                this.add(new JLabel(getLabelFor("ID_MODELE_COURRIER_CLIENT")), c);
                c.gridx++;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(this.comboModele, c);

                // Adresse
                c.gridx = 0;
                c.gridy++;
                c.weightx = 1;
                c.weighty = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.insets = new Insets(15, 2, 3, 2);
                TitledSeparator sep = new TitledSeparator("Adresse");
                this.add(sep, c);
                c.insets = c.getDefaultInsets();

                this.addView("ID_ADRESSE", REQ + ";" + DEC + ";" + SEP);
                final ElementSQLObject eltAdr = (ElementSQLObject) this.getView("ID_ADRESSE");

                // Combo
                this.boxAdresse = new ElementComboBox();
                final SQLElement adresseElt = Configuration.getInstance().getDirectory().getElement("ADRESSE");

                this.boxAdresse.init(adresseElt);
                c.gridwidth = 1;
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.boxAdresse.setAddIconVisible(false);
                this.add(new JLabel("Importer l'adresse"), c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                this.add(this.boxAdresse, c);
                this.boxAdresse.addValueListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        // TODO Auto-generated method stub
                        if (boxAdresse.getSelectedId() > 1) {
                            SQLRowValues rowVals = adresseElt.getTable().getRow(boxAdresse.getSelectedId()).createUpdateRow();
                            rowVals.clearPrimaryKeys();
                            eltAdr.setValue(rowVals);
                        }
                    }
                });

                // Adr principale

                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy++;
                AdresseSQLComponent adrSqlComp = (AdresseSQLComponent) eltAdr.getSQLChild();
                adrSqlComp.setDestinataireVisible(true);
                this.add(eltAdr, c);

                // Infos
                c.gridx = 0;
                c.gridy++;
                c.weighty = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                c.insets = new Insets(15, 2, 3, 2);
                this.add(new TitledSeparator(getLabelFor("INFOS")), c);
                c.gridy++;
                c.weightx = 1;
                c.weighty = 1;
                c.fill = GridBagConstraints.BOTH;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.insets = c.getDefaultInsets();
                JTextArea textInfos = new JTextArea();
                this.add(textInfos, c);

                JPanel panelCheckBox = new JPanel();
                panelCheckBox.add(this.checkVisu);
                this.checkVisu.setSelected(true);
                panelCheckBox.add(this.checkImpression);
                this.buttonShowDoc = new JButton("Voir le document");
                panelCheckBox.add(this.buttonShowDoc, FlowLayout.RIGHT);
                c.gridx = 0;
                c.gridy++;
                c.fill = GridBagConstraints.NONE;
                c.weighty = 0;
                c.weightx = 0;
                c.anchor = GridBagConstraints.WEST;
                this.add(panelCheckBox, c);

                this.buttonShowDoc.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        CourrierClientSheet s = new CourrierClientSheet(getTable().getRow(getSelectedID()));
                        s.showDocument();
                    }
                });

                this.addRequiredSQLObject(this.selAffaire, "ID_AFFAIRE");
                this.addRequiredSQLObject(this.objet, "NOM");
                this.addRequiredSQLObject(this.textNumero, "NUMERO");
                this.addRequiredSQLObject(this.comboModele, "ID_MODELE_COURRIER_CLIENT");
                this.addRequiredSQLObject(this.date, "DATE");
                this.addRequiredSQLObject(selSecr, "ID_SECRETAIRE");

                this.selAffaire.addValueListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        int idSel = getSelectedID();

                        if (idSel <= 1) {
                            Integer i = selAffaire.getValue();
                            if (i != null) {

                                SQLRow rowAff = eltAffaire.getTable().getRow(i);
                                setWhereComboAdresse(rowAff);
                                if (rowAff != null) {

                                    SQLRow rowCli = rowAff.getForeignRow("ID_CLIENT");

                                    if (rowCli != null) {
                                        SQLRow rowAdresse;
                                            rowAdresse = rowCli.getForeignRow("ID_ADRESSE");
                                        if (rowAdresse != null) {

                                            SQLRowValues rowVals = rowAdresse.createUpdateRow();
                                            rowVals.clearPrimaryKeys();
                                            final String string = rowCli.getString("FORME_JURIDIQUE") + " " + rowCli.getString("NOM");
                                            rowVals.put("DEST", string.trim());
                                            eltAdr.setValue(rowVals);
                                        }

                                    }
                                }
                            }
                        }
                    }
                });

                adresseElt.getTable().addTableModifiedListener(new SQLTableModifiedListener() {
                    @Override
                    public void tableModified(SQLTableEvent evt) {
                        Integer i = selAffaire.getValue();
                        if (i != null) {

                            SQLRow rowAff = eltAffaire.getTable().getRow(i);
                            setWhereComboAdresse(rowAff);
                        }
                    }
                });
                eltAffaire.getTable().addTableModifiedListener(new SQLTableModifiedListener() {
                    @Override
                    public void tableModified(SQLTableEvent evt) {
                        Integer i = selAffaire.getValue();
                        if (i != null) {

                            SQLRow rowAff = eltAffaire.getTable().getRow(i);
                            setWhereComboAdresse(rowAff);
                        }
                    }
                });
            }

            private void setWhereComboAdresse(SQLRow rowAff) {
                System.err.println("Set Where Adresse " + rowAff);
                if (rowAff != null) {
                    SQLElement adresseElt = Configuration.getInstance().getDirectory().getElement("ADRESSE");
                    SQLRow rowCli = rowAff.getForeignRow("ID_CLIENT");
                    Where w = new Where(adresseElt.getTable().getKey(), "=", rowAff.getInt("ID_ADRESSE_COURRIER_1"));
                    w = w.or(new Where(adresseElt.getTable().getKey(), "=", rowAff.getInt("ID_ADRESSE_COURRIER_2")));
                    w = w.or(new Where(adresseElt.getTable().getKey(), "=", rowAff.getInt("ID_ADRESSE_COURRIER_3")));
                    if (rowCli != null) {
                        SQLRow rowAdresse = rowCli.getForeignRow("ID_ADRESSE");

                        w = w.or(new Where(adresseElt.getTable().getKey(), "=", rowAdresse.getID()));
                        this.boxAdresse.getRequest().setWhere(w);
                        this.boxAdresse.fillCombo();
                        return;
                    }
                }
                this.boxAdresse.getRequest().setWhere(null);
                this.boxAdresse.fillCombo();
            }

            @Override
            protected SQLRowValues createDefaults() {
                SQLRowValues rowVals = new SQLRowValues(getTable());

                rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(CourrierClientSQLElement.class));

                // User
                SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
                SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("SECRETAIRE");
                int idUser = UserManager.getInstance().getCurrentUser().getId();

                sel.addSelect(eltComm.getTable().getKey());
                sel.setWhere(new Where(eltComm.getTable().getField("ID_USER_COMMON"), "=", idUser));
                List<SQLRow> rowsComm = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel, eltComm.getTable()));

                if (rowsComm != null && rowsComm.size() > 0) {
                    rowVals.put("ID_SECRETAIRE", rowsComm.get(0).getID());
                }
                return rowVals;
            }

            @Override
            public int insert(SQLRow order) {

                if (this.textNumero.checkValidation()) {
                    // incrémentation du numéro auto
                    if (NumerotationAutoSQLElement.getNextNumero(CourrierClientSQLElement.class).equalsIgnoreCase(this.textNumero.getText().trim())) {
                        SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
                        SQLRowValues rowVals = new SQLRowValues(tableNum);
                        int val = tableNum.getRow(2).getInt("COURRIER_START");
                        val++;
                        rowVals.put("COURRIER_START", new Integer(val));

                        try {
                            rowVals.update(2);
                        } catch (SQLException e) {

                            e.printStackTrace();
                        }
                    }
                    int id = super.insert(order);
                    CourrierClientSheet s = new CourrierClientSheet(getTable().getRow(id));
                    String printer = PrinterNXProps.getInstance().getStringProperty("CourrierClientPrinter");
                    s.generate(this.checkImpression.isSelected(), this.checkVisu.isSelected(), printer);
                    return id;

                } else {
                    ExceptionHandler.handle("Impossible d'ajouter, numéro de courrier existant.");
                    Object root = SwingUtilities.getRoot(this);
                    if (root instanceof EditFrame) {
                        EditFrame frame = (EditFrame) root;
                        frame.getPanel().setAlwaysVisible(true);
                    }
                    return getSelectedID();
                }
            }

            @Override
            public void select(SQLRowAccessor r) {
                if (r != null) {
                    this.textNumero.setIdSelected(r.getID());
                }
                super.select(r);
            }

            @Override
            public void update() {

                if (!this.textNumero.checkValidation()) {
                    ExceptionHandler.handle("Impossible d'ajouter, numéro de courrier existant.");
                    Object root = SwingUtilities.getRoot(this);
                    if (root instanceof EditFrame) {
                        EditFrame frame = (EditFrame) root;
                        frame.getPanel().setAlwaysVisible(true);
                    }
                }

                super.update();

                if (this.checkVisu.isSelected()) {
                    CourrierClientSheet s = new CourrierClientSheet(getTable().getRow(getSelectedID()));
                    s.showDocument();
                }
            }
        };
    }
}
