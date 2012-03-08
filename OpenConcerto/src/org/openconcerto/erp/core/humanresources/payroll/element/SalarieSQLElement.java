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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

public class SalarieSQLElement extends ComptaSQLConfElement {

    public SalarieSQLElement() {
        super("SALARIE", "un salarié", "salariés");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("PRENOM");
        l.add("ID_FICHE_PAYE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("PRENOM");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ETAT_CIVIL");
        l.add("ID_REGLEMENT_PAYE");
        l.add("ID_INFOS_SALARIE_PAYE");
        l.add("ID_FICHE_PAYE");
        l.add("ID_CUMULS_CONGES");
        l.add("ID_CUMULS_PAYE");
        l.add("ID_VARIABLE_SALARIE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            private final JLabel warningCodeSalLabel = new JLabelWarning();
            private final JTextField textCode = new JTextField();

            private JTabbedPane tabbedPane;
            private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");

            public void addViews() {

                // TODO LIKE EBP --> CALCUL DU BRUT A PARTIR DU NET
                this.warningCodeSalLabel.setText("Ce code est déjà affecté à un autre salarié!");

                this.setLayout(new GridBagLayout());
                GridBagConstraints c = new DefaultGridBagConstraints();

                // Titre personnel
                final JLabel labelTitrePersonnel = new JLabel(getLabelFor("ID_TITRE_PERSONNEL"));
                final ElementComboBox comboTitre = new ElementComboBox(false, 5);

                this.add(labelTitrePersonnel, c);
                c.gridx++;
                c.weightx = 0;
                c.fill = GridBagConstraints.NONE;
                this.add(comboTitre, c);

                // Nom
                final JLabel labelNom = new JLabel(getLabelFor("NOM"));
                final JTextField textNom = new JTextField();
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx++;
                c.weightx = 0;
                this.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNom, c);

                // Prénom
                final JLabel labelPrenom = new JLabel(getLabelFor("PRENOM"));
                final JTextField textPrenom = new JTextField();

                c.gridx++;
                c.weightx = 0;
                this.add(labelPrenom, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textPrenom, c);

                // Code
                final JLabel labelCode = new JLabel(getLabelFor("CODE"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelCode, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textCode, c);

                c.weightx = 0;
                c.gridx++;
                this.add(this.warningCodeSalLabel, c);
                this.warningCodeSalLabel.setVisible(false);

                this.textCode.getDocument().addDocumentListener(new SimpleDocumentListener() {
                    @Override
                    public void update(DocumentEvent e) {
                        checkCode();
                    }
                });

                /***********************************************************************************
                 * TABBED PANE
                 **********************************************************************************/
                this.tabbedPane = new JTabbedPane();

                // Etat Civil
                this.addView("ID_ETAT_CIVIL", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltEtatCivil = (ElementSQLObject) this.getView("ID_ETAT_CIVIL");
                JScrollPane scrollEtatCivil = new JScrollPane(eltEtatCivil);
                scrollEtatCivil.setBorder(null);
                this.tabbedPane.add("Etat Civil", scrollEtatCivil);

                // Règlement de la paye
                this.addView("ID_REGLEMENT_PAYE", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltReglPaye = (ElementSQLObject) this.getView("ID_REGLEMENT_PAYE");
                JScrollPane scrollReglPaye = new JScrollPane(eltReglPaye);
                scrollReglPaye.setBorder(null);
                this.tabbedPane.add("Règlement", scrollReglPaye);

                // Infos salarie-paye
                this.addView("ID_INFOS_SALARIE_PAYE", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltInfosPaye = (ElementSQLObject) this.getView("ID_INFOS_SALARIE_PAYE");
                JScrollPane scrollInfosPaye = new JScrollPane(eltInfosPaye);
                scrollInfosPaye.setBorder(null);
                this.tabbedPane.add("Informations salarié-paye", scrollInfosPaye);

                // Fiche de paye
                this.addView("ID_FICHE_PAYE", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltFichePaye = (ElementSQLObject) this.getView("ID_FICHE_PAYE");
                JScrollPane scrollFichePaye = new JScrollPane(eltFichePaye);
                scrollFichePaye.setBorder(null);
                this.tabbedPane.add("Fiche de paye en cours", scrollFichePaye);
                // this.tabbedPane.setEnabledAt(this.tabbedPane.getTabCount() - 1, false);

                // Cumuls
                this.addView("ID_CUMULS_CONGES", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltCumulsConges = (ElementSQLObject) this.getView("ID_CUMULS_CONGES");
                JPanel panelCumulsConges = new JPanel();
                panelCumulsConges.setBorder(BorderFactory.createTitledBorder("Cumuls congés"));
                panelCumulsConges.add(eltCumulsConges);

                this.addView("ID_CUMULS_PAYE", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltCumulPaye = (ElementSQLObject) this.getView("ID_CUMULS_PAYE");
                JPanel panelCumulsPaye = new JPanel();
                panelCumulsPaye.setBorder(BorderFactory.createTitledBorder("Cumuls paye"));
                panelCumulsPaye.add(eltCumulPaye);

                this.addView("ID_VARIABLE_SALARIE", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltVarSalarie = (ElementSQLObject) this.getView("ID_VARIABLE_SALARIE");
                JPanel panelVarSalarie = new JPanel();
                panelVarSalarie.setBorder(BorderFactory.createTitledBorder("Variables de la période"));
                panelVarSalarie.add(eltVarSalarie);

                JPanel panelAllCumul = new JPanel();
                GridBagConstraints cPanel = new DefaultGridBagConstraints();
                panelAllCumul.setLayout(new GridBagLayout());
                cPanel.fill = GridBagConstraints.NONE;

                panelAllCumul.add(panelCumulsConges, cPanel);
                cPanel.gridx++;
                cPanel.weightx = 1;
                panelAllCumul.add(panelCumulsPaye, cPanel);
                cPanel.gridwidth = GridBagConstraints.REMAINDER;
                cPanel.gridy++;
                cPanel.gridx = 0;
                cPanel.weighty = 1;
                cPanel.weightx = 1;
                cPanel.anchor = GridBagConstraints.NORTHWEST;
                panelAllCumul.add(panelVarSalarie, cPanel);

                this.tabbedPane.add("Cumuls et variables de la période", new JScrollPane(panelAllCumul));
                // this.tabbedPane.setEnabledAt(this.tabbedPane.getTabCount() - 1, false);

                c.gridy++;
                c.gridx = 0;
                c.weighty = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.BOTH;
                c.weighty = 1;
                c.weightx = 1;
                c.anchor = GridBagConstraints.NORTHWEST;
                this.add(this.tabbedPane, c);

                this.addRequiredSQLObject(textNom, "NOM");
                this.addRequiredSQLObject(textPrenom, "PRENOM");
                this.addRequiredSQLObject(this.textCode, "CODE");
                this.addRequiredSQLObject(comboTitre, "ID_TITRE_PERSONNEL");
                comboTitre.setButtonsVisible(false);
                DefaultGridBagConstraints.lockMinimumSize(comboTitre);

                this.textCode.setText(NumerotationAutoSQLElement.getNextNumero(SalarieSQLElement.class, new Date()));
            }

            @Override
            public synchronized ValidState getValidState() {
                return super.getValidState().and(ValidState.createCached(!this.warningCodeSalLabel.isVisible(), this.warningCodeSalLabel.getText()));
            }

            private void checkCode() {
                SQLSelect selNum = new SQLSelect(getTable().getBase());
                selNum.addSelectFunctionStar("count");
                selNum.setWhere(new Where(getTable().getField("CODE"), "=", this.textCode.getText().trim()));
                selNum.andWhere(new Where(getTable().getField("ID"), "!=", getSelectedID()));

                final Number count = (Number) getTable().getBase().getDataSource().executeScalar(selNum.asString());
                final boolean isValid = count.intValue() == 0;
                final boolean currentValid = !this.warningCodeSalLabel.isVisible();
                if (currentValid != isValid) {
                    this.warningCodeSalLabel.setVisible(!isValid);
                    this.fireValidChange();
                }
            }

            @Override
            public void select(SQLRowAccessor r) {
                super.select(r);
                /*
                 * if (r.getID() > 1) { this.tabbedPane.setEnabledAt(this.tabbedPane.getTabCount() -
                 * 1, true); this.tabbedPane.setEnabledAt(this.tabbedPane.getTabCount() - 2, true);
                 * }
                 */
                checkCode();
            }

            public void update() {

                super.update();

                SQLTable tableFichePaye = getTable().getBase().getTable("FICHE_PAYE");
                SQLRowValues rowVals = new SQLRowValues(tableFichePaye);
                rowVals.put("ID_SALARIE", getSelectedID());
                SQLRow row = getTable().getRow(getSelectedID());
                try {
                    rowVals.update(row.getInt("ID_FICHE_PAYE"));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            public int insert(SQLRow order) {
                int id = super.insert(order);
                SQLTable tableFichePaye = getTable().getBase().getTable("FICHE_PAYE");
                SQLTable tableInfosPaye = getTable().getBase().getTable("INFOS_SALARIE_PAYE");
                SQLRow row = getTable().getRow(id);
                SQLRow rowInfosPaye = tableInfosPaye.getRow(row.getInt("ID_INFOS_SALARIE_PAYE"));

                SQLRowValues rowVals = new SQLRowValues(tableFichePaye);
                rowVals.put("ID_SALARIE", id);
                rowVals.put("CONGES_ACQUIS", rowInfosPaye.getObject("CONGES_PAYES"));
                try {
                    rowVals.update(row.getInt("ID_FICHE_PAYE"));
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                // incrémentation du numéro auto
                if (NumerotationAutoSQLElement.getNextNumero(SalarieSQLElement.class, new Date()).equalsIgnoreCase(this.textCode.getText().trim())) {
                    SQLRowValues rowValsNum = new SQLRowValues(this.tableNum);
                    int val = this.tableNum.getRow(2).getInt("SALARIE_START");
                    val++;
                    rowValsNum.put("SALARIE_START", new Integer(val));

                    try {
                        rowValsNum.update(2);
                    } catch (SQLException e) {

                        e.printStackTrace();
                    }
                }

                return id;
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".employe";
    }
}
