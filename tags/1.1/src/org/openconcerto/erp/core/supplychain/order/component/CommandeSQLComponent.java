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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.supplychain.order.element.CommandeSQLElement;
import org.openconcerto.erp.core.supplychain.order.ui.CommandeItemTable;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class CommandeSQLComponent extends TransfertBaseSQLComponent {

    private CommandeItemTable table;
    private JUniqueTextField numeroUniqueCommande;
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final JCheckBox checkImpression = new JCheckBox("Imprimer");
    private final JCheckBox checkVisu = new JCheckBox("Visualiser");
    private final ITextArea infos = new ITextArea(3, 3);
    private ElementComboBox fourn;

    public CommandeSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("COMMANDE"));
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Numero du commande
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("NUMERO"), SwingConstants.RIGHT), c);

        this.numeroUniqueCommande = new JUniqueTextField(16);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        DefaultGridBagConstraints.lockMinimumSize(numeroUniqueCommande);
        this.add(this.numeroUniqueCommande, c);

        // Date
        JLabel labelDate = new JLabel(getLabelFor("DATE"));
        labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(labelDate, c);

        JDate dateCommande = new JDate(true);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(dateCommande, c);

        // Fournisseur
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_FOURNISSEUR"), SwingConstants.RIGHT), c);

        this.fourn = new ElementComboBox();
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.fourn, c);

        // Commande en cours
        JCheckBox boxEnCours = new JCheckBox(getLabelFor("EN_COURS"));
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(boxEnCours, c);
        this.addRequiredSQLObject(boxEnCours, "EN_COURS");

        // Devise
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_DEVISE"), SwingConstants.RIGHT), c);

        final ElementComboBox boxDevise = new ElementComboBox();
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(boxDevise, c);
        this.addView(boxDevise, "ID_DEVISE");

        // Reference
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.EAST;
        this.add(new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT), c);

        final JTextField textNom = new JTextField();
        c.gridx++;
        c.weightx = 1;
        this.add(textNom, c);

        String field;
            field = "ID_COMMERCIAL";
        // Commercial
        c.weightx = 0;
        c.gridx++;
        this.add(new JLabel(getLabelFor(field), SwingConstants.RIGHT), c);

        ElementComboBox commSel = new ElementComboBox(false, 25);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        this.add(commSel, c);
        addRequiredSQLObject(commSel, field);

        // Table d'élément
        this.table = new CommandeItemTable();
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.gridwidth = 4;
        this.add(this.table, c);
        boxDevise.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                table.setDevise(boxDevise.getSelectedRow());

            }
        });
        // Bottom
        c.gridy++;
        c.weighty = 0;
        this.add(getBottomPanel(), c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        JPanel panelOO = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelOO.add(this.checkImpression, c);
        panelOO.add(this.checkVisu, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(panelOO, c);

        addRequiredSQLObject(this.fourn, "ID_FOURNISSEUR");
        addSQLObject(textNom, "NOM");
        addRequiredSQLObject(dateCommande, "DATE");
        // addRequiredSQLObject(radioEtat, "ID_ETAT_DEVIS");
        addRequiredSQLObject(this.numeroUniqueCommande, "NUMERO");
        addSQLObject(this.infos, "INFOS");

        this.numeroUniqueCommande.setText(NumerotationAutoSQLElement.getNextNumero(CommandeSQLElement.class));

        // radioEtat.setValue(EtatDevisSQLElement.EN_ATTENTE);
        // this.numeroUniqueDevis.addLabelWarningMouseListener(new MouseAdapter() {
        // public void mousePressed(MouseEvent e) {
        //
        // if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
        // numeroUniqueDevis.setText(NumerotationAutoSQLElement.getNextNumeroDevis());
        // }
        // }
        // });

        DefaultGridBagConstraints.lockMinimumSize(this.fourn);
        DefaultGridBagConstraints.lockMinimumSize(commSel);
    }

    private JPanel getBottomPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Colonne 1 : Infos
        c.gridx = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new TitledSeparator(getLabelFor("INFOS")), c);

        c.gridy++;
        c.weighty = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        final JScrollPane scrollPane = new JScrollPane(this.infos);
        scrollPane.setBorder(null);
        panel.add(scrollPane, c);

        // Colonne 2 : Poids & autres
        DefaultProps props = DefaultNXProps.getInstance();
        Boolean b = props.getBooleanValue("ArticleShowPoids");
        final JTextField textPoidsTotal = new JTextField(8);
        JTextField poids = new JTextField();
        if (b) {
            final JPanel panelPoids = new JPanel();

            panelPoids.add(new JLabel(getLabelFor("T_POIDS")), c);

            textPoidsTotal.setEnabled(false);
            textPoidsTotal.setHorizontalAlignment(JTextField.RIGHT);
            textPoidsTotal.setDisabledTextColor(Color.BLACK);

            panelPoids.add(textPoidsTotal, c);

            c.gridx++;
            c.gridy = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.gridheight = 2;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHEAST;
            panel.add(panelPoids, c);
            DefaultGridBagConstraints.lockMinimumSize(panelPoids);
            addSQLObject(textPoidsTotal, "T_POIDS");
        } else {
            addSQLObject(poids, "T_POIDS");
        }
        // Total
        DeviseField textPortHT = new DeviseField();
        DeviseField textRemiseHT = new DeviseField();
        DeviseField fieldHT = new DeviseField();
        DeviseField fieldTVA = new DeviseField();
        DeviseField fieldTTC = new DeviseField();
        DeviseField fieldDevise = new DeviseField();
        DeviseField fieldService = new DeviseField();
        fieldHT.setOpaque(false);
        fieldTVA.setOpaque(false);
        fieldTTC.setOpaque(false);
        fieldService.setOpaque(false);
        addRequiredSQLObject(fieldDevise, "T_DEVISE");
        addRequiredSQLObject(fieldHT, "T_HT");
        addRequiredSQLObject(fieldTVA, "T_TVA");
        addRequiredSQLObject(fieldTTC, "T_TTC");
        addRequiredSQLObject(fieldService, "T_SERVICE");
        final TotalPanel totalTTC = new TotalPanel(this.table.getRowValuesTable(), this.table.getPrixTotalHTElement(), this.table.getPrixTotalTTCElement(), this.table.getHaElement(),
                this.table.getQteElement(), fieldHT, fieldTVA, fieldTTC, textPortHT, textRemiseHT, fieldService, this.table.getPrixServiceElement(), fieldDevise,
                this.table.getTableElementTotalDevise(), poids, this.table.getPoidsTotalElement());

        c.gridx++;
        c.gridy--;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        DefaultGridBagConstraints.lockMinimumSize(totalTTC);

        panel.add(totalTTC, c);


        table.getModel().addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                textPoidsTotal.setText(String.valueOf(table.getPoidsTotal()));
            }
        });

        textPortHT.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void removeUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void insertUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });

        textRemiseHT.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void removeUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }

            public void insertUpdate(DocumentEvent e) {
                totalTTC.updateTotal();
            }
        });
        return panel;
    }

    public int insert(SQLRow order) {

        int idCommande = getSelectedID();
        // on verifie qu'un devis du meme numero n'a pas été inséré entre temps
        if (this.numeroUniqueCommande.checkValidation()) {

            idCommande = super.insert(order);
            this.table.updateField("ID_COMMANDE", idCommande);

            // Création des articles
            this.table.createArticle(idCommande, this.getElement());

            // generation du document
            CommandeXmlSheet sheet = new CommandeXmlSheet(getTable().getRow(idCommande));
            sheet.genere(this.checkVisu.isSelected(), this.checkImpression.isSelected());

            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(CommandeSQLElement.class).equalsIgnoreCase(this.numeroUniqueCommande.getText().trim())) {
                SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                int val = this.tableNum.getRow(2).getInt("COMMANDE_CLIENT_START");
                val++;
                rowVals.put("COMMANDE_CLIENT_START", new Integer(val));

                try {
                    rowVals.update(2);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de commande client existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        }

        return idCommande;
    }

    @Override
    public void select(SQLRowAccessor r) {
        if (r != null) {
            this.numeroUniqueCommande.setIdSelected(r.getID());
        }

        super.select(r);
        if (r != null) {
            this.table.insertFrom("ID_COMMANDE", r.getID());
        }
    }

    @Override
    public void update() {
        if (!this.numeroUniqueCommande.checkValidation()) {
            ExceptionHandler.handle("Impossible d'ajouter, numéro de commande client existant.");
            Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
            return;
        }

        super.update();
        this.table.updateField("ID_COMMANDE", getSelectedID());
        this.table.createArticle(getSelectedID(), this.getElement());

        // generation du document
        CommandeXmlSheet dSheet = new CommandeXmlSheet(getTable().getRow(getSelectedID()));
        dSheet.genere(this.checkVisu.isSelected(), this.checkImpression.isSelected());
    }

    public void setDefaults() {
        this.resetValue();
        this.numeroUniqueCommande.setText(NumerotationAutoSQLElement.getNextNumero(CommandeSQLElement.class));
        this.table.getModel().clearRows();
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("T_POIDS", 0.0F);
        rowVals.put("EN_COURS", Boolean.TRUE);

        // User
        // SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("COMMERCIAL");
        int idUser = UserManager.getInstance().getCurrentUser().getId();

        // sel.addSelect(eltComm.getTable().getKey());
        // sel.setWhere(new Where(eltComm.getTable().getField("ID_USER_COMMON"), "=", idUser));
        // List<SQLRow> rowsComm = (List<SQLRow>)
        // Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new
        // SQLRowListRSH(eltComm.getTable()));
        SQLRow rowsComm = SQLBackgroundTableCache.getInstance().getCacheForTable(eltComm.getTable()).getFirstRowContains(idUser, eltComm.getTable().getField("ID_USER_COMMON"));

        if (rowsComm != null) {
            rowVals.put("ID_COMMERCIAL", rowsComm.getID());
        }
        rowVals.put("T_HT", Long.valueOf(0));
        rowVals.put("T_SERVICE", Long.valueOf(0));
        rowVals.put("T_TVA", Long.valueOf(0));
        rowVals.put("T_TTC", Long.valueOf(0));

        return rowVals;
    }

    public CommandeItemTable getRowValuesTable() {
        return this.table;
    }

    /**
     * Chargement des éléments d'une commande dans la table
     * 
     * @param idCommande
     * 
     */
    public void loadCommande(int idCommande) {

        SQLElement commande = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        SQLElement commandeElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");

        if (idCommande > 1) {
            SQLInjector injector = SQLInjector.getInjector(commande.getTable(), this.getTable());
            this.select(injector.createRowValuesFrom(idCommande));
        }

        loadItem(this.table, commande, idCommande, commandeElt);
    }

    /**
     * Chargement des éléments d'un devis dans la table
     * 
     * @param idDevis
     * 
     */
    public void loadDevis(int idDevis) {

        SQLElement devis = Configuration.getInstance().getDirectory().getElement("DEVIS");
        SQLElement devisElt = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");

        if (idDevis > 1) {
            SQLInjector injector = SQLInjector.getInjector(devis.getTable(), this.getTable());
            this.select(injector.createRowValuesFrom(idDevis));
        }

        loadItem(this.table, devis, idDevis, devisElt);
    }

    /**
     * Chargement des éléments d'une facture dans la table
     * 
     * @param idFact
     * 
     */
    public void loadFacture(int idFact) {

        SQLElement facture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement factureElt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");

        if (idFact > 1) {
            SQLInjector injector = SQLInjector.getInjector(facture.getTable(), this.getTable());
            this.select(injector.createRowValuesFrom(idFact));
        }

        loadItem(this.table, facture, idFact, factureElt);
    }

}
