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
 
 package org.openconcerto.erp.core.common.component;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.panel.ChargementCreationSocietePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.utils.CollectionMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Sociétés existantes avec le nom de la base associée
 */
public class SocieteCommonSQLElement extends ConfSQLElement {

    private static final String TABLE_NAME = "SOCIETE_COMMON";

    static public final DBRoot getRoot(final SQLRow company) {
        try {
            return getRoot(company, false);
        } catch (SQLException e) {
            // shouldn't happen since we don't allow refresh
            throw new IllegalStateException(e);
        }
    }

    static public final DBRoot getRoot(final SQLRow company, final boolean allowRefresh) throws SQLException {
        final String rootName = company.getString("DATABASE_NAME");
        final DBSystemRoot sysRoot = company.getTable().getDBSystemRoot();
        if (allowRefresh && !sysRoot.contains(rootName)) {
            sysRoot.addRootToMap(rootName);
            sysRoot.refresh(TablesMap.createFromTables(rootName, Collections.singleton(TABLE_NAME)), true);
        }
        return sysRoot.getRoot(rootName);
    }

    public SocieteCommonSQLElement() {
        super(TABLE_NAME, "une société", "sociétés");
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        return CollectionMap.singleton(null, getListFields());
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_EXERCICE_COMMON");
        l.add("ID_ADRESSE_COMMON");
        return l;
    }

    public final static Date getDateDebutExercice() {

        SQLTable tableExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON");

        SQLRow societeRow = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        return (Date) tableExercice.getRow(societeRow.getInt("ID_EXERCICE_COMMON")).getObject("DATE_DEB");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private JTextField textNom, textNumSiret, textNumNII, textNumAPE;
            private JTextField textNumTel, textNumFax, textEmail, textNumUrssaff;
            // private ITextArea textAdresse;
            private JComboBox combo;
            private JLabel labelPlan;
            private ElementSQLObject eltExercice;
            private TitledSeparator sep, sepPlan;
            private SQLTextCombo textType;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                this.textNom = new JTextField();
                this.textNumAPE = new JTextField();
                this.textNumNII = new JTextField();
                this.textNumSiret = new JTextField();
                this.textNumTel = new JTextField();
                this.textNumFax = new JTextField();
                this.textEmail = new JTextField();
                this.textType = new SQLTextCombo();
                this.textNumUrssaff = new JTextField();

                /***********************************************************************************
                 * Informations générales
                 **********************************************************************************/
                TitledSeparator sepInfos = new TitledSeparator("Informations générales");
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy++;
                this.add(sepInfos, c);

                // Type
                c.gridy++;
                c.gridwidth = 1;
                JLabel labelType = new JLabel(getLabelFor("TYPE"));
                labelType.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelType, c);

                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 1;
                this.add(this.textType, c);

                // Nom
                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 0;
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNom, c);

                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 1;
                this.add(this.textNom, c);

                // Numero Siret
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                JLabel labelNumSiret = new JLabel(getLabelFor("NUM_SIRET"));
                labelNumSiret.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNumSiret, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.textNumSiret, c);

                // Numero NII
                c.gridx++;
                c.weightx = 0;
                JLabel labelNumNII = new JLabel(getLabelFor("NUM_NII"));
                labelNumNII.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNumNII, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.textNumNII, c);

                // Numero URSSAF
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                JLabel labelNumUrs = new JLabel(getLabelFor("NUMERO_URSSAF"));
                labelNumUrs.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNumUrs, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.textNumUrssaff, c);

                // Numero APE
                c.gridx++;
                c.weightx = 0;
                JLabel labelNumAPE = new JLabel(getLabelFor("NUM_APE"));
                labelNumAPE.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNumAPE, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.textNumAPE, c);

                // RCS
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                JLabel labelRCS = new JLabel(getLabelFor("RCS"));
                labelRCS.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelRCS, c);

                c.gridx++;
                c.weightx = 1;
                JTextField fieldRCS = new JTextField();
                this.add(fieldRCS, c);
                this.addView(fieldRCS, "RCS");

                // Capital
                c.gridx++;
                c.weightx = 0;
                JLabel labelCapital = new JLabel(getLabelFor("CAPITAL"));
                labelCapital.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelCapital, c);

                c.gridx++;
                c.weightx = 1;
                JTextField fieldCapital = new JTextField();
                this.add(fieldCapital, c);
                this.addView(fieldCapital, "CAPITAL");

                // Assurance
                if (getTable().contains("NUMERO_POLICE")) {
                    c.gridy++;
                    c.gridx = 0;
                    c.weightx = 0;
                    JLabel labelPolice = new JLabel(getLabelFor("NUMERO_POLICE"));
                    labelPolice.setHorizontalAlignment(SwingConstants.RIGHT);
                    this.add(labelPolice, c);

                    c.gridx++;
                    c.weightx = 1;
                    JTextField fieldPolice = new JTextField();
                    this.add(fieldPolice, c);
                    this.addView(fieldPolice, "NUMERO_POLICE");
                }

                // Adresse
                final TitledSeparator sepAdresse = new TitledSeparator(getLabelFor("ID_ADRESSE_COMMON"));
                c.gridx = 0;
                c.gridy++;
                c.gridwidth = 4;
                c.gridheight = 1;
                c.weightx = 1;
                c.weighty = 0;
                c.fill = GridBagConstraints.BOTH;
                this.add(sepAdresse, c);
                this.addView("ID_ADRESSE_COMMON", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltAdr = (ElementSQLObject) this.getView("ID_ADRESSE_COMMON");
                c.gridy++;
                this.add(eltAdr, c);
                // Contact
                JPanel panelContact = new JPanel();
                panelContact.setLayout(new GridBagLayout());
                panelContact.setBorder(BorderFactory.createTitledBorder("Contacts"));
                final GridBagConstraints cc = new DefaultGridBagConstraints();

                // Numero de telephone
                JLabel labelNumTel = new JLabel(getLabelFor("NUM_TEL"));
                labelNumTel.setHorizontalAlignment(SwingConstants.RIGHT);
                cc.weightx = 0;
                panelContact.add(labelNumTel, cc);

                cc.gridx++;
                cc.weightx = 1;
                panelContact.add(this.textNumTel, cc);

                // Numero de fax
                cc.gridy++;
                cc.gridx = 0;
                cc.weightx = 0;
                JLabel labelNumFax = new JLabel(getLabelFor("NUM_FAX"));
                labelNumFax.setHorizontalAlignment(SwingConstants.RIGHT);
                panelContact.add(labelNumFax, cc);

                cc.gridx++;
                cc.weightx = 1;
                panelContact.add(this.textNumFax, cc);

                // EMail
                cc.gridx = 0;
                cc.gridy++;
                cc.weightx = 0;
                JLabel labelEMail = new JLabel(getLabelFor("MAIL"));
                labelEMail.setHorizontalAlignment(SwingConstants.RIGHT);
                panelContact.add(labelEMail, cc);

                cc.gridx++;
                cc.weightx = 1;
                panelContact.add(this.textEmail, cc);

                c.gridy++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.BOTH;
                this.add(panelContact, c);
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridwidth = 1;

                // Devise
                if (getTable().contains("ID_DEVISE")) {
                    c.gridy++;
                    c.gridx = 0;
                    c.weightx = 0;
                    JLabel labelDevise = new JLabel(getLabelFor("ID_DEVISE"));
                    labelDevise.setHorizontalAlignment(SwingConstants.RIGHT);
                    this.add(labelDevise, c);

                    c.gridx++;
                    c.weightx = 1;
                    ElementComboBox boxDevise = new ElementComboBox();
                    this.add(boxDevise, c);
                    this.addView(boxDevise, "ID_DEVISE");
                }

                /***********************************************************************************
                 * DATE D'EXERCICE
                 **********************************************************************************/
                this.sep = new TitledSeparator("Date de l'exercice");
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy++;
                c.insets = new Insets(10, 2, 1, 2);
                this.add(this.sep, c);

                c.gridwidth = 1;
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                c.weighty = 1;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.insets = new Insets(2, 2, 1, 2);

                this.addView("ID_EXERCICE_COMMON", REQ + ";" + DEC + ";" + SEP);
                this.eltExercice = (ElementSQLObject) this.getView("ID_EXERCICE_COMMON");
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.eltExercice, c);

                // Regime fiscale
                /*
                 * c.gridx = 0; c.gridy++; c.gridwidth = 1; c.weightx = 0; JLabel labelRegime = new
                 * JLabel("Régime fiscale ");
                 * labelRegime.setHorizontalAlignment(SwingConstants.RIGHT); this.add(labelRegime,
                 * c);
                 */

                /***********************************************************************************
                 * Choix du plan comptable
                 **********************************************************************************/
                this.sepPlan = new TitledSeparator("Plan comptable de l'entreprise");
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy++;
                c.insets = new Insets(10, 2, 1, 2);
                this.add(this.sepPlan, c);
                JPanel panelPlan = new JPanel();
                this.labelPlan = new JLabel("Choix du plan comptable", SwingConstants.RIGHT);
                this.combo = new JComboBox();
                this.combo.addItem("Base");
                this.combo.addItem("Abrégé");
                this.combo.addItem("Développé");
                panelPlan.add(this.labelPlan);
                panelPlan.add(this.combo);

                c.gridx = 0;
                c.gridy++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.insets = new Insets(2, 2, 1, 2);
                c.fill = GridBagConstraints.NONE;
                this.add(panelPlan, c);

                c.gridy++;
                final JPanel additionalPanel = new JPanel();
                this.add(additionalPanel, c);
                this.setAdditionalFieldsPanel(new FormLayouter(additionalPanel, 2, 1));

                this.addRequiredSQLObject(this.textNom, "NOM");
                // this.addSQLObject(this.textAdresse, "ADRESSE");
                this.addRequiredSQLObject(this.textNumAPE, "NUM_APE");
                this.addRequiredSQLObject(this.textNumNII, "NUM_NII");
                this.addRequiredSQLObject(this.textNumSiret, "NUM_SIRET");
                this.addSQLObject(this.textEmail, "MAIL");
                this.addSQLObject(this.textNumTel, "NUM_TEL");
                this.addSQLObject(this.textNumFax, "NUM_FAX");
                this.addSQLObject(this.textNumUrssaff, "NUMERO_URSSAF");
                this.addSQLObject(this.textType, "TYPE");
            }

            public int insert(SQLRow order) {
                int id = super.insert(order);
                SQLRow row = getTable().getRow(id);
                SQLRowValues rowVals = row.getForeignRow("ID_EXERCICE_COMMON").createEmptyUpdateRow();
                rowVals.put("ID_SOCIETE_COMMON", id);
                try {
                    rowVals.update();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                creationBase(id, this.combo.getSelectedIndex());
                return id;
            }

            /*
             * public int insert() {
             * 
             * int id = super.insert(); creationBase(id); return id; }
             */

            private void creationBase(int id, int typePCG) {

                System.err.println("display chargement societe panel");

                PanelFrame frameChargement = new PanelFrame(new ChargementCreationSocietePanel(id, typePCG), "Création d'une société");
                frameChargement.setVisible(true);

                /*
                 * System.err.println("Création de la base");
                 * ActionDB.dupliqueMySQLDB("Default", "OpenConcerto" + id); SQLRowValues
                 * rowVals = new SQLRowValues(getTable()); rowVals.put("DATABASE_NAME", "OpenConcerto"
                 * + id); try { rowVals.update(id); } catch (SQLException e) {
                 * 
                 * e.printStackTrace(); }
                 */
                /*
                 * SQLTable comptePCETable = ((ComptaPropsConfiguration)
                 * Configuration.getInstance()).getSQLBaseSociete().getTable("COMPTE_PCE"); SQLTable
                 * comptePCGTable = ((ComptaPropsConfiguration)
                 * Configuration.getInstance()).getSQLBaseSociete().getTable("COMPTE_PCG"); // MAYBE
                 * Vérifier qu'aucun n'est deja créé???? // On crée le PCE à partir du PCG
                 * selectionné; SQLSelect selCompte = new SQLSelect(getTable().getBase());
                 * selCompte.addSelect(comptePCGTable.getField("NUMERO"));
                 * selCompte.addSelect(comptePCGTable.getField("NOM"));
                 * selCompte.addSelect(comptePCGTable.getField("INFOS"));
                 * 
                 * if (this.combo.getSelectedIndex() == 0) {
                 * selCompte.setWhere("ID_TYPE_COMPTE_PCG_BASE", "!=", 1); } else { if
                 * (this.combo.getSelectedIndex() == 1) {
                 * selCompte.setWhere("ID_TYPE_COMPTE_PCG_AB", "!=", 1); } }
                 * 
                 * String reqCompte = selCompte.asString(); Object obRep =
                 * getTable().getBase().getDataSource().execute(reqCompte, new ArrayListHandler());
                 * 
                 * List tmpCpt = (List) obRep;
                 * 
                 * for (int i = 0; i < tmpCpt.size(); i++) { Object[] tmp = (Object[])
                 * tmpCpt.get(i);
                 * 
                 * SQLRowValues vals = new SQLRowValues(comptePCETable); vals.put("NUMERO", tmp[0]);
                 * vals.put("NOM", tmp[1]); vals.put("INFOS", tmp[2]);
                 * 
                 * try { vals.insert(); } catch (SQLException e) { e.printStackTrace(); } }
                 */
            }

            @Override
            public void select(SQLRowAccessor r) {
                super.select(r);
                if (r != null) {
                    disableEdition();
                }
            }

            public void disableEdition() {
                this.combo.setVisible(false);
                this.labelPlan.setVisible(false);
                this.eltExercice.setEditable(false);

                this.sepPlan.setVisible(false);
            }
        };
    }
}
