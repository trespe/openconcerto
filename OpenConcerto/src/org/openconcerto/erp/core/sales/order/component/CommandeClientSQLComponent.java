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
 
 package org.openconcerto.erp.core.sales.order.component;

import static org.openconcerto.utils.CollectionUtils.createSet;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.sales.order.element.CommandeClientSQLElement;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.core.sales.order.ui.CommandeClientItemTable;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
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
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class CommandeClientSQLComponent extends TransfertBaseSQLComponent {

    private CommandeClientItemTable table;
    private JUniqueTextField numeroUniqueCommande;
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final ITextArea infos = new ITextArea(3, 3);
    private ElementComboBox comboCommercial, comboDevis, comboClient;
    private PanelOOSQLComponent panelOO;

    private final SQLTextCombo textObjet = new SQLTextCombo();

    public CommandeClientSQLComponent() {
        super(Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT"));
    }

    public CommandeClientItemTable getRowValuesTable() {
        return this.table;
    }

    public void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Numero du commande
        c.gridx = 0;
        this.add(new JLabel(getLabelFor("NUMERO"), SwingConstants.RIGHT), c);

        this.numeroUniqueCommande = new JUniqueTextField(16);
        c.fill = GridBagConstraints.NONE;
        c.gridx++;
        c.weightx = 1;
        this.add(this.numeroUniqueCommande, c);

        // Date
        JLabel labelDate = new JLabel(getLabelFor("DATE"));
        labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        this.add(labelDate, c);

        JDate dateCommande = new JDate(true);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(dateCommande, c);

        // Champ Module
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        final JPanel addP = ComptaSQLConfElement.createAdditionalPanel();

        this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        this.add(addP, c);

        c.gridy++;
        c.gridwidth = 1;

        this.comboDevis = new ElementComboBox();

        // Reference
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel labelObjet = new JLabel(getLabelFor("NOM"));
        labelObjet.setHorizontalAlignment(SwingConstants.RIGHT);
        c.weightx = 0;
        this.add(labelObjet, c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.textObjet, c);

        String field;
            field = "ID_COMMERCIAL";
        c.fill = GridBagConstraints.HORIZONTAL;
        // Commercial
        JLabel labelCommercial = new JLabel(getLabelFor(field));
        labelCommercial.setHorizontalAlignment(SwingConstants.RIGHT);

        c.gridx++;
        c.weightx = 0;
        this.add(labelCommercial, c);

        this.comboCommercial = new ElementComboBox(false, 25);
        this.comboCommercial.setListIconVisible(false);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 1;
        this.add(this.comboCommercial, c);
        addRequiredSQLObject(this.comboCommercial, field);

        // Ligne 3: Client
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT), c);

        this.comboClient = new ElementComboBox();
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 3;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.comboClient, c);
        final ElementComboBox boxTarif = new ElementComboBox();
        this.comboClient.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!isFilling() && comboClient.getValue() != null) {
                    Integer id = comboClient.getValue();

                    if (id > 1) {

                        SQLRow row = comboClient.getElement().getTable().getRow(id);
                        if (comboClient.getElement().getTable().getFieldsName().contains("ID_TARIF")) {

                            SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                            if (!foreignRow.isUndefined() && (boxTarif.getSelectedRow() == null || boxTarif.getSelectedId() != foreignRow.getID())
                                    && JOptionPane.showConfirmDialog(null, "Appliquer les tarifs associés au client?") == JOptionPane.YES_OPTION) {
                                boxTarif.setValue(foreignRow.getID());
                                // SaisieVenteFactureSQLComponent.this.tableFacture.setTarif(foreignRow,
                                // true);
                            } else {
                                boxTarif.setValue(foreignRow.getID());
                            }
                            // SQLRowAccessor foreignRow = row.getForeignRow("ID_TARIF");
                            // if (foreignRow.isUndefined() &&
                            // !row.getForeignRow("ID_DEVISE").isUndefined()) {
                            // SQLRowValues rowValsD = new SQLRowValues(foreignRow.getTable());
                            // rowValsD.put("ID_DEVISE", row.getObject("ID_DEVISE"));
                            // foreignRow = rowValsD;
                            //
                            // }
                            // table.setTarif(foreignRow, true);
                        }
                    }
                }

            }
        });
        // tarif
        if (this.getTable().getFieldsName().contains("ID_TARIF")) {
            // TARIF
            c.gridy++;
            c.gridx = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            this.add(new JLabel("Tarif à appliquer"), c);
            c.gridx++;
            c.gridwidth = GridBagConstraints.REMAINDER;

            c.weightx = 1;
            this.add(boxTarif, c);
            this.addView(boxTarif, "ID_TARIF");
            boxTarif.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    table.setTarif(boxTarif.getSelectedRow(), false);
                }
            });
        }

        // Table d'élément
        this.table = new CommandeClientItemTable();
        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.table, c);

        DeviseField textPortHT = new DeviseField();
        DeviseField textRemiseHT = new DeviseField();

        // INfos
        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        c.weighty = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 2;
        this.add(new TitledSeparator(getLabelFor("INFOS")), c);

        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        final JScrollPane scrollPane = new JScrollPane(this.infos);
        scrollPane.setBorder(null);
        this.add(scrollPane, c);

        // Poids
        c.gridwidth = 1;

        DefaultProps props = DefaultNXProps.getInstance();
        Boolean b = props.getBooleanValue("ArticleShowPoids");
        final JTextField textPoidsTotal = new JTextField(8);
        if (b) {
            JPanel panel = new JPanel();

            panel.add(new JLabel(getLabelFor("T_POIDS")), c);

            textPoidsTotal.setEnabled(false);
            textPoidsTotal.setHorizontalAlignment(JTextField.RIGHT);
            textPoidsTotal.setDisabledTextColor(Color.BLACK);
            panel.add(textPoidsTotal, c);

            panel.setOpaque(false);
            DefaultGridBagConstraints.lockMinimumSize(panel);

            c.gridx = 2;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHEAST;
            this.add(panel, c);
        }

        // Total
        DeviseField fieldHT = new DeviseField();
        DeviseField fieldTVA = new DeviseField();
        DeviseField fieldTTC = new DeviseField();
        DeviseField fieldDevise = new DeviseField();
        DeviseField fieldService = new DeviseField();
        DeviseField fieldHA = new DeviseField();
        fieldHT.setOpaque(false);
        fieldHA.setOpaque(false);
        fieldTVA.setOpaque(false);
        fieldTTC.setOpaque(false);
        fieldService.setOpaque(false);
        addSQLObject(fieldDevise, "T_DEVISE");
        addRequiredSQLObject(fieldHT, "T_HT");
        addRequiredSQLObject(fieldTVA, "T_TVA");
        addRequiredSQLObject(fieldTTC, "T_TTC");
        addRequiredSQLObject(fieldService, "T_SERVICE");
        if (getTable().contains("PREBILAN")) {
            addSQLObject(fieldHA, "PREBILAN");
        } else if (getTable().contains("T_HA")) {
            addSQLObject(fieldHA, "T_HA");
        }

        JTextField poids = new JTextField();
        // addSQLObject(poids, "T_POIDS");
        final TotalPanel totalTTC = new TotalPanel(this.table, fieldHT, fieldTVA, fieldTTC, textPortHT, textRemiseHT, fieldService, fieldHA, fieldDevise, poids, null);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy--;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 2;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;

        this.add(totalTTC, c);

        this.panelOO = new PanelOOSQLComponent(this);
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        c.gridy += 3;
        c.weightx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.panelOO, c);

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

        addRequiredSQLObject(this.comboClient, "ID_CLIENT");
        addSQLObject(this.textObjet, "NOM");
        addSQLObject(textPoidsTotal, "T_POIDS");
        addRequiredSQLObject(dateCommande, "DATE");
        // addRequiredSQLObject(radioEtat, "ID_ETAT_DEVIS");
        addRequiredSQLObject(this.numeroUniqueCommande, "NUMERO");
        addSQLObject(this.infos, "INFOS");
        addSQLObject(this.comboDevis, "ID_DEVIS");

        this.numeroUniqueCommande.setText(NumerotationAutoSQLElement.getNextNumero(CommandeClientSQLElement.class, new Date()));

        this.table.getModel().addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                textPoidsTotal.setText(String.valueOf(CommandeClientSQLComponent.this.table.getPoidsTotal()));
            }
        });
        DefaultGridBagConstraints.lockMinimumSize(comboClient);
        DefaultGridBagConstraints.lockMinimumSize(comboCommercial);
        DefaultGridBagConstraints.lockMinimumSize(comboDevis);
        DefaultGridBagConstraints.lockMinimumSize(totalTTC);
        DefaultGridBagConstraints.lockMaximumSize(totalTTC);
        DefaultGridBagConstraints.lockMinimumSize(numeroUniqueCommande);

    }

    public int insert(SQLRow order) {
        final int idCommande;
        // on verifie qu'un devis du meme numero n'a pas été inséré entre temps
        if (this.numeroUniqueCommande.checkValidation()) {

            idCommande = super.insert(order);
            this.table.updateField("ID_COMMANDE_CLIENT", idCommande);

            // Création des articles
            this.table.createArticle(idCommande, this.getElement());
            // generation du document

            try {
                CommandeClientXmlSheet sheet = new CommandeClientXmlSheet(getTable().getRow(idCommande));
                sheet.createDocumentAsynchronous();
                sheet.showPrintAndExportAsynchronous(CommandeClientSQLComponent.this.panelOO.isVisualisationSelected(), CommandeClientSQLComponent.this.panelOO.isImpressionSelected(), true);
            } catch (Exception e) {
                ExceptionHandler.handle("Impossible de créer la commande", e);
            }

            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(CommandeClientSQLElement.class, new Date()).equalsIgnoreCase(this.numeroUniqueCommande.getText().trim())) {
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
            idCommande = getSelectedID();
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
        if (r == null || r.getIDNumber() == null)
            super.select(r);
        else {
            System.err.println(r);
            final SQLRowValues rVals = r.asRowValues();
            final SQLRowValues vals = new SQLRowValues(r.getTable());
            vals.load(rVals, createSet("ID_CLIENT"));
            vals.setID(rVals.getID());
            System.err.println("Select CLIENT");
            super.select(vals);
            rVals.remove("ID_CLIENT");
            super.select(rVals);
        }
        if (r != null) {
            this.table.insertFrom("ID_COMMANDE_CLIENT", r.getID());
        }
        // this.radioEtat.setVisible(r.getID() > 1);
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
        this.table.updateField("ID_COMMANDE_CLIENT", getSelectedID());
        this.table.createArticle(getSelectedID(), this.getElement());

        // generation du document

        try {
            CommandeClientXmlSheet sheet = new CommandeClientXmlSheet(getTable().getRow(getSelectedID()));
            sheet.createDocumentAsynchronous();
            sheet.showPrintAndExportAsynchronous(CommandeClientSQLComponent.this.panelOO.isVisualisationSelected(), CommandeClientSQLComponent.this.panelOO.isImpressionSelected(), true);
        } catch (Exception e) {
            ExceptionHandler.handle("Impossible de créer la commande", e);
        }

    }

    public void setDefaults() {
        this.resetValue();
        this.numeroUniqueCommande.setText(NumerotationAutoSQLElement.getNextNumero(CommandeClientSQLElement.class, new Date()));
        this.table.getModel().clearRows();
    }

    /**
     * Création d'une commande à partir d'un devis
     * 
     * @param idDevis
     * 
     */
    public void loadDevis(int idDevis) {

        SQLElement devis = Configuration.getInstance().getDirectory().getElement("DEVIS");
        SQLElement devisElt = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");

        if (idDevis > 1) {
            SQLInjector injector = SQLInjector.getInjector(devis.getTable(), this.getTable());
            SQLRowValues createRowValuesFrom = injector.createRowValuesFrom(idDevis);
            SQLRow rowDevis = devis.getTable().getRow(idDevis);

            String string = rowDevis.getString("OBJET");
            createRowValuesFrom.put("NOM", string + (string.trim().length() == 0 ? "" : ",") + rowDevis.getString("NUMERO"));
            this.select(createRowValuesFrom);
        }

        loadItem(this.table, devis, idDevis, devisElt);
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("T_POIDS", 0.0F);
        rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(CommandeClientSQLElement.class, new Date()));
        // User
        // SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("COMMERCIAL");
        int idUser = UserManager.getInstance().getCurrentUser().getId();
        //
        // sel.addSelect(eltComm.getTable().getKey());
        // sel.setWhere(new Where(eltComm.getTable().getField("ID_USER_COMMON"), "=", idUser));
        // List<SQLRow> rowsComm = (List<SQLRow>)
        // Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new
        // SQLRowListRSH(eltComm.getTable()));
        SQLRow rowsComm = SQLBackgroundTableCache.getInstance().getCacheForTable(eltComm.getTable()).getFirstRowContains(idUser, eltComm.getTable().getField("ID_USER_COMMON"));

        if (rowsComm != null) {
            rowVals.put("ID_COMMERCIAL", rowsComm.getID());
        }
        return rowVals;
    }
}
