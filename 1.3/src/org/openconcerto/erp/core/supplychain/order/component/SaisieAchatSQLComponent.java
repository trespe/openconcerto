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
 
 package org.openconcerto.erp.core.supplychain.order.component;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.MontantPanel;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieAchat;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;

public class SaisieAchatSQLComponent extends BaseSQLComponent {

    public SaisieAchatSQLComponent(SQLElement element) {
        super(element);
    }

    private JTextField textNumeroFacture;
    private JTextField textNumeroCmd;
    private JTextField textSource, textIdSource;

    private DeviseField fieldMontantRegle = new DeviseField();
    final ISQLCompteSelector compteSel = new ISQLCompteSelector();

    private JCheckBox checkImmo;

    private MontantPanel montant;
    private ElementComboBox nomFournisseur;
    private ElementComboBox comboAvoir;
    protected ElementSQLObject eltModeRegl;

    private PropertyChangeListener listenerModeReglDefaut = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {

            SQLRow rowFourn = SaisieAchatSQLComponent.this.nomFournisseur.getRequest().getPrimaryTable().getRow(nomFournisseur.getWantedID());

            if (!isFilling() && rowFourn != null && !rowFourn.isUndefined()) {
                SaisieAchatSQLComponent.this.montant.setUE(rowFourn.getBoolean("UE"));

                SQLRow rowCharge = rowFourn.getForeign("ID_COMPTE_PCE_CHARGE");
                if (rowCharge != null && !rowCharge.isUndefined()) {
                    compteSel.setValue(rowCharge);
                }

                int idModeRegl = rowFourn.getInt("ID_MODE_REGLEMENT");
                if (idModeRegl > 1 && SaisieAchatSQLComponent.this.eltModeRegl != null && getMode() == Mode.INSERTION) {
                    SQLElement sqlEltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    SQLRow rowModeRegl = sqlEltModeRegl.getTable().getRow(idModeRegl);
                    SQLRowValues rowVals = rowModeRegl.createUpdateRow();
                    rowVals.clearPrimaryKeys();
                    SaisieAchatSQLComponent.this.eltModeRegl.setValue(rowVals);
                    System.err.println("Select Mode regl " + idModeRegl);
                }
                // }
            }

        }

    };

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Source de la saisie --> Commande, BR
        this.textIdSource = new JTextField();
        this.textSource = new JTextField();

        /*******************************************************************************************
         * * RENSEIGNEMENTS
         ******************************************************************************************/

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;

        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;

        // libellé
        JLabel labelLibelle = new JLabel("Achat de ");
        SQLTextCombo textLibelle = new SQLTextCombo();
        c.weightx = 0;
        labelLibelle.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelLibelle, c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(textLibelle, c);

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();
        this.setAdditionalFieldsPanel(new FormLayouter(addP, 1));
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        // Fournisseurs
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;

        this.add(new JLabel("Fournisseur", SwingConstants.RIGHT), c);
        c.gridx++;

        c.gridwidth = 4;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.nomFournisseur = new ElementComboBox();
        this.add(this.nomFournisseur, c);

        // Ligne 3 : Compte de charges
        c.gridy++;
        c.weightx = 0;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        this.add(new JLabel(getLabelFor("ID_COMPTE_PCE"), SwingConstants.RIGHT), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx++;
        c.weightx = 1;
        this.add(compteSel, c);

        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        this.add(new JLabel(getLabelFor("NUMERO_FACTURE"), SwingConstants.RIGHT), c);

        this.textNumeroFacture = new JTextField(16);
        DefaultGridBagConstraints.lockMinimumSize(textNumeroFacture);
        c.gridx = 1;
        c.gridwidth = 1;
        this.add(this.textNumeroFacture, c);

        // Date

        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Date"), c);
        c.gridx++;
        c.weightx = 1;
        JDate dateSaisie = new JDate();
        c.fill = GridBagConstraints.NONE;

        this.add(dateSaisie, c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;

        this.add(new JLabel(getLabelFor("NUMERO_COMMANDE"), SwingConstants.RIGHT), c);

        this.textNumeroCmd = new JTextField(16);
        c.gridx = 1;
        c.gridwidth = 1;
        DefaultGridBagConstraints.lockMinimumSize(textNumeroCmd);
        this.add(this.textNumeroCmd, c);

        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.anchor = GridBagConstraints.EAST;
        this.checkImmo = new JCheckBox(getLabelFor("IMMO"));
        this.add(this.checkImmo, c);
        c.anchor = GridBagConstraints.WEST;

        // MONTANT
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(new JLabel("Montant", SwingConstants.RIGHT), c);

        c.gridx = 1;
        c.gridwidth = 3;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        this.montant = new MontantPanel();
        this.add(this.montant, c);

        // Avoir
        JPanel panelAvoir = new JPanel();
        this.comboAvoir = new ElementComboBox(true);

        this.comboAvoir.setAddIconVisible(false);
        panelAvoir.add(this.comboAvoir);

        panelAvoir.add(new JLabel("Montant réglé"));
        panelAvoir.add(this.fieldMontantRegle);
        this.fieldMontantRegle.setEditable(false);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_AVOIR_FOURNISSEUR"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        this.add(panelAvoir, c);

        /*******************************************************************************************
         * * MODE DE REGLEMENT
         ******************************************************************************************/

        // Mode de règlement
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        c.gridy++;
        c.insets = new Insets(10, 2, 1, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        TitledSeparator sep = new TitledSeparator("Mode de règlement");

        this.add(sep, c);

        c.insets = new Insets(2, 2, 1, 2);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.addView("ID_MODE_REGLEMENT", REQ + ";" + DEC + ";" + SEP);
        this.eltModeRegl = (ElementSQLObject) this.getView("ID_MODE_REGLEMENT");
        this.add(this.eltModeRegl, c);

        /*******************************************************************************************
         * * INFORMATIONS COMPLEMENTAIRES
         ******************************************************************************************/
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        c.gridy++;
        sep = new TitledSeparator("Informations complémentaires");
        c.insets = new Insets(10, 2, 1, 2);
        this.add(sep, c);
        c.insets = new Insets(2, 2, 1, 2);

        c.gridx = 0;
        c.gridy++;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        ITextArea textInfos = new ITextArea(4, 4);

        JScrollPane scrollPane = new JScrollPane(textInfos);
        DefaultGridBagConstraints.lockMinimumSize(scrollPane);

        this.add(textInfos, c);

        this.addSQLObject(textInfos, "INFOS");
        this.addRequiredSQLObject(dateSaisie, "DATE");
        this.addRequiredSQLObject(this.nomFournisseur, "ID_FOURNISSEUR");
        this.addRequiredSQLObject(this.montant.getChoixTaxe(), "ID_TAXE");
        this.addRequiredSQLObject(this.montant.getMontantTTC(), "MONTANT_TTC");
        this.addRequiredSQLObject(this.montant.getMontantHT(), "MONTANT_HT");
        this.addRequiredSQLObject(this.montant.getMontantTVA(), "MONTANT_TVA");
        this.addSQLObject(textLibelle, "NOM");
        this.addSQLObject(this.textNumeroFacture, "NUMERO_FACTURE");
        this.addSQLObject(this.textNumeroCmd, "NUMERO_COMMANDE");
        this.addSQLObject(this.textIdSource, "IDSOURCE");
        this.addSQLObject(this.textSource, "SOURCE");
        this.addRequiredSQLObject(compteSel, "ID_COMPTE_PCE");
        this.addSQLObject(this.comboAvoir, "ID_AVOIR_FOURNISSEUR");
        this.addSQLObject(this.checkImmo, "IMMO");

        this.montant.setChoixTaxe(TaxeCache.getCache().getFirstTaxe().getID());

        this.nomFournisseur.addModelListener("wantedID", this.listenerModeReglDefaut);

        this.montant.getMontantTTC().getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                refreshText();
            }
        });

        this.comboAvoir.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                refreshText();
            }
        });

        final SQLTable tableAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_FOURNISSEUR").getTable();
        this.nomFournisseur.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                Where w = new Where(tableAvoir.getField("SOLDE"), "=", Boolean.FALSE);
                if (SaisieAchatSQLComponent.this.nomFournisseur.isEmpty()) {
                    w.and(new Where(getTable().getBase().getTable("AVOIR_FOURNISSEUR").getField("ID_FOURNISSEUR"), "=", -1));
                } else {
                    w.and(new Where(getTable().getBase().getTable("AVOIR_FOURNISSEUR").getField("ID_FOURNISSEUR"), "=", SaisieAchatSQLComponent.this.nomFournisseur.getValue()));
                }
                if (getSelectedID() > 1) {
                    SQLRow row = getTable().getRow(getSelectedID());
                    w = w.or(new Where(getTable().getBase().getTable("AVOIR_FOURNISSEUR").getKey(), "=", row.getInt("ID_AVOIR_FOURNISSEUR")));
                }

                SaisieAchatSQLComponent.this.comboAvoir.getRequest().setWhere(null);
                SaisieAchatSQLComponent.this.comboAvoir.getRequest().setWhere(w);
                SaisieAchatSQLComponent.this.comboAvoir.fillCombo();
            }
        });

        Where w = new Where(tableAvoir.getField("SOLDE"), "=", Boolean.FALSE);
        this.comboAvoir.getRequest().setWhere(w);
        this.comboAvoir.fillCombo();

        // Lock UI
        DefaultGridBagConstraints.lockMinimumSize(nomFournisseur);
        DefaultGridBagConstraints.lockMinimumSize(panelAvoir);
        DefaultGridBagConstraints.lockMinimumSize(montant);
        DefaultGridBagConstraints.lockMaximumSize(montant);

    }

    @Override
    public ValidState getValidState() {
        ValidState result = super.getValidState();
        if (result.isValid()) {
            if (this.montant.getMontantTTC() != null && this.montant.getMontantTTC().getValue() != null) {

                long l = this.montant.getMontantTTC().getValue();

                if (this.comboAvoir != null && !this.comboAvoir.isEmpty() && this.comboAvoir.getSelectedId() > 1) {
                    SQLElement eltAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_FOURNISSEUR");
                    SQLRow rowAvoir = eltAvoir.getTable().getRow(this.comboAvoir.getSelectedId());
                    l -= ((Number) rowAvoir.getObject("MONTANT_TTC")).longValue();
                }
                if (l < 0) {
                    result = new ValidState(false, "Le montant est négatif");
                }
            }
        }
        return result;
    }

    /***********************************************************************************************
     * * GENERATION DU MOUVEMENT ET DES ECRITURES ASSOCIEES A L'INSERTION
     **********************************************************************************************/
    public int insert(SQLRow order) {
        final int id = super.insert(order);

        // on solde l'avoir
        if (this.comboAvoir.getSelectedId() > 1) {
            SQLTable tableAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_FOURNISSEUR").getTable();
            SQLRow rowAvoir = tableAvoir.getRow(this.comboAvoir.getSelectedId());
            SQLRowValues rowVals = rowAvoir.createEmptyUpdateRow();
            rowVals.put("SOLDE", Boolean.TRUE);
            try {
                rowVals.update();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        new GenerationMvtSaisieAchat(id);

        return id;
    }

    @Override
    public Set<String> getPartialResetNames() {
        Set<String> s = new HashSet<String>();
        s.addAll(super.getPartialResetNames());
        s.add("INFOS");
        s.add("NUMERO_FACTURE");
        s.add("NUMERO_COMMANDE");
        s.add("MONTANT_HT");
        return s;
    }

    public void select(SQLRowAccessor r) {

        // FIXME probleme selection du mode de reglement par defaut -> l'id est celui du défaut dans
        // la saisie

        super.select(r);

        if (r != null) {
            System.err.println(r);
            this.montant.calculMontant();
            Object idF = r.getObject("ID_FOURNISSEUR");
            System.err.println("Founisseur " + idF);
            if (idF != null) {
                int idSeleted = Integer.valueOf(idF.toString());

                if (idSeleted > 1) {
                    SQLElement fournisseur = Configuration.getInstance().getDirectory().getElement("FOURNISSEUR");
                    SQLRow rowFourn = fournisseur.getTable().getRow(idSeleted);
                    this.montant.setUE(rowFourn.getBoolean("UE"));
                }
            }
            System.out.println("select id Saisie Achat " + r.getID());
        }
    }

    final SQLTable tablePrefCompte = getTable().getTable("PREFS_COMPTE");

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues vals = new SQLRowValues(this.getTable());
        SQLRow r;

        try {
            r = ModeReglementDefautPrefPanel.getDefaultRow(false);
            SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");

            if (r.getID() > 1) {
                SQLRowValues rowVals = eltModeReglement.createCopy(r, null);
                System.err.println("Primary Keys " + rowVals.getID());
                rowVals.clearPrimaryKeys();
                System.err.println(rowVals.getInt("ID_TYPE_REGLEMENT"));
                vals.put("ID_MODE_REGLEMENT", rowVals);
            }
        } catch (SQLException e) {
            System.err.println("Impossible de sélectionner le mode de règlement par défaut du client.");
            e.printStackTrace();
        }

        // Select Compte charge par defaut

        final SQLRow rowPrefsCompte = SQLBackgroundTableCache.getInstance().getCacheForTable(tablePrefCompte).getRowFromId(2);
        // compte Achat
        int idCompteAchat = rowPrefsCompte.getInt("ID_COMPTE_PCE_ACHAT");
        if (idCompteAchat <= 1) {
            try {
                idCompteAchat = ComptePCESQLElement.getIdComptePceDefault("Achats");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        vals.put("ID_COMPTE_PCE", idCompteAchat);
        vals.put("ID_TAXE", TaxeCache.getCache().getFirstTaxe().getID());

        return vals;
    }

    public void update() {
        if (JOptionPane.showConfirmDialog(this, "Attention en modifiant cette facture, vous supprimerez les chéques et les échéances associés. Continuer?", "Modification de facture",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            super.update();

            // on solde l'avoir
            if (this.comboAvoir.getSelectedId() > 1) {
                SQLTable tableAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_FOURNISSEUR").getTable();
                SQLRow rowAvoir = tableAvoir.getRow(this.comboAvoir.getSelectedId());
                SQLRowValues rowVals = rowAvoir.createEmptyUpdateRow();
                rowVals.put("SOLDE", Boolean.TRUE);
                try {
                    rowVals.update();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            SQLRow row = getTable().getRow(this.getSelectedID());
            int idMvt = row.getInt("ID_MOUVEMENT");
            System.err.println("__________***************** UPDATE" + idMvt);

            // on supprime tout ce qui est lié à la facture
            EcritureSQLElement eltEcr = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement("ECRITURE");
            eltEcr.archiveMouvementProfondeur(idMvt, false);

            // regenere les ecritures
            new GenerationMvtSaisieAchat(this.getSelectedID(), idMvt);
        }
    }

    private void refreshText() {
        Number n = this.montant.getMontantTTC().getValue();
        if (this.comboAvoir.getSelectedId() > 1) {
            SQLTable tableAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_FOURNISSEUR").getTable();
            if (n != null) {
                long ttc = n.longValue();
                SQLRow rowAvoir = tableAvoir.getRow(this.comboAvoir.getSelectedId());
                long totalAvoir = ((Number) rowAvoir.getObject("MONTANT_TTC")).longValue();
                this.fieldMontantRegle.setValue(ttc - totalAvoir);
            } else {
                this.fieldMontantRegle.setValue(0l);
            }
        } else {
            if (n != null) {
                this.fieldMontantRegle.setValue(n.longValue());
            } else {
                this.fieldMontantRegle.setValue(0l);
            }
        }
    }

    public final void loadCommande(int id) {
        loadFromTable("COMMANDE", id);
    }

    public final void loadBonReception(int id) {
        loadFromTable("BON_RECEPTION", id);
    }

    private final void loadFromTable(final String tableName, final int id) {
        // Mise à jour des totaux
        // this.montant.setEnabled(false);
        // this.montant.getChoixTaxe().setVisible(false);
        if (id > 1) {
            final SQLElement eltCommande = Configuration.getInstance().getDirectory().getElement(tableName);
            final SQLInjector injector = SQLInjector.getInjector(eltCommande.getTable(), this.getTable());
            this.select(injector.createRowValuesFrom(id));
        }
    }

}
