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
 
 package org.openconcerto.erp.core.sales.invoice.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.ui.ListeGestCommEltPanel;
import org.openconcerto.erp.core.sales.invoice.component.SaisieVenteFactureSQLComponent;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.report.ListeFactureXmlSheet;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.invoice.ui.DateEnvoiRenderer;
import org.openconcerto.erp.core.sales.invoice.ui.ListeFactureRenderer;
import org.openconcerto.erp.generationEcritures.GenerationMvtRetourNatexis;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.rights.NXRights;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.EditPanelListener;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.sql.rowset.Predicate;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.table.TableColumn;

public class ListeSaisieVenteFactureAction extends CreateFrameAbstractAction {

    private IListFrame frame;
    // private EditFrame editFrame;
    private ListeGestCommEltPanel listeAddPanel;
    private SQLElement eltEcheance = Configuration.getInstance().getDirectory().getElement("ECHEANCE_CLIENT");
    private SQLElement eltMvt = Configuration.getInstance().getDirectory().getElement("MOUVEMENT");
    private boolean affact = UserManager.getInstance().getCurrentUser().getRights().haveRight(NXRights.ACCES_RETOUR_AFFACTURAGE.getCode());

    public ListeSaisieVenteFactureAction() {
        super();
        this.putValue(Action.NAME, "Liste des factures");
    }

    public JFrame createFrame() {
        SQLElement eltFacture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");

        final SQLTableModelSourceOnline src = eltFacture.getTableSource(true);

        for (SQLTableModelColumn column : src.getColumns()) {
            // if (column.getValueClass() == Long.class ||
            // column.getValueClass() ==
            // BigInteger.class || column.getValueClass() == BigDecimal.class)
            column.setRenderer(ListeFactureRenderer.UTILS.getRenderer(column.getRenderer()));
            if (column.getClass().isAssignableFrom(SQLTableModelColumnPath.class)) {
                ((SQLTableModelColumnPath) column).setEditable(false);
            }
        }
            final SQLTableModelColumn dateEnvoiCol = src.getColumn(eltFacture.getTable().getField("DATE_ENVOI"));

            if (dateEnvoiCol != null) {
                ((SQLTableModelColumnPath) dateEnvoiCol).setEditable(true);


                dateEnvoiCol.setColumnInstaller(new IClosure<TableColumn>() {
                    @Override
                    public void executeChecked(TableColumn columnDateEnvoi) {
                        columnDateEnvoi.setCellEditor(new org.openconcerto.ui.table.TimestampTableCellEditor());
                        columnDateEnvoi.setCellRenderer(new DateEnvoiRenderer());
                    }
                });
            }
            final SQLTableModelColumn dateReglCol = src.getColumn(eltFacture.getTable().getField("DATE_REGLEMENT"));
            ((SQLTableModelColumnPath) dateReglCol).setEditable(true);

            // Edition des dates de reglement
            dateReglCol.setColumnInstaller(new IClosure<TableColumn>() {
                @Override
                public void executeChecked(TableColumn columnDateReglement) {
                    columnDateReglement.setCellEditor(new org.openconcerto.ui.table.TimestampTableCellEditor());
                    columnDateReglement.setCellRenderer(new DateEnvoiRenderer());
                }
            });
        this.listeAddPanel = new ListeGestCommEltPanel(eltFacture, new IListe(src)) {


            @Override
            protected GridBagConstraints createConstraints() {
                GridBagConstraints c = super.createConstraints();
                c.gridy++;
                return c;
            }
        };
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        this.listeAddPanel.add(getPanelLegende(), c);

        // Total panel
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridy = 4;
        JPanel panelTotal = new JPanel(new FlowLayout());

        String sfe = DefaultNXProps.getInstance().getStringProperty("ArticleSFE");
        Boolean bSfe = Boolean.valueOf(sfe);
        boolean isSFE = bSfe != null && bSfe.booleanValue();

        List<SQLField> fields = new ArrayList<SQLField>(2);
        if (isSFE) {
            fields.add(eltFacture.getTable().getField("T_HA"));
        }

        fields.add(eltFacture.getTable().getField("T_HT"));
        fields.add(eltFacture.getTable().getField("T_TTC"));
        IListTotalPanel totalPanel = new IListTotalPanel(this.listeAddPanel.getListe(), fields, null, "Total Global");

        panelTotal.add(totalPanel);

        this.listeAddPanel.add(panelTotal, c);

        // Date panel
        IListFilterDatePanel datePanel = new IListFilterDatePanel(this.listeAddPanel.getListe(), eltFacture.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        this.listeAddPanel.add(datePanel, c);

        this.frame = new IListFrame(this.listeAddPanel);

        // FIXME Maybe Stock rowSelection in new List
        final MouseSheetXmlListeListener mouseListener = new MouseSheetXmlListeListener(VenteFactureXmlSheet.class) {
            @Override
            public List<RowAction> addToMenu() {

                // final SQLRow row = liste.getSelectedRow();
                List<RowAction> l = new ArrayList<RowAction>(5);
                    PredicateRowAction actionBL = new PredicateRowAction(new AbstractAction("Transférer en bon de livraison") {
                        public void actionPerformed(ActionEvent e) {
                            SaisieVenteFactureSQLElement elt = (SaisieVenteFactureSQLElement) Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                            elt.transfertBL(IListe.get(e).getSelectedId());
                        }
                    }, false);
                    actionBL.setPredicate(IListeEvent.getSingleSelectionPredicate());
                    l.add(actionBL);
                PredicateRowAction actionClone = new PredicateRowAction(new AbstractAction("Créer à partir de ...") {
                    public void actionPerformed(ActionEvent e) {

                        SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                        EditFrame editFrame = new EditFrame(eltFact, EditPanel.CREATION);

                        ((SaisieVenteFactureSQLComponent) editFrame.getSQLComponent()).loadFactureExistante(IListe.get(e).getSelectedId());
                        editFrame.setVisible(true);
                    }
                }, false);
                actionClone.setPredicate(IListeEvent.getSingleSelectionPredicate());
                l.add(actionClone);

                PredicateRowAction actionAvoir = new PredicateRowAction(new AbstractAction("Transférer en avoir") {
                    public void actionPerformed(ActionEvent e) {
                        SaisieVenteFactureSQLElement elt = (SaisieVenteFactureSQLElement) Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                        elt.transfertAvoir(IListe.get(e).getSelectedId());
                    }
                }, false);
                actionAvoir.setPredicate(IListeEvent.getSingleSelectionPredicate());
                l.add(actionAvoir);

                return l;
                // return super.addToMenu();

            }
        };
        // this.frame.getPanel().getListe().addRowActions(mouseListener.getRowActions());
        this.frame.getPanel().getListe().addIListeActions(mouseListener.getRowActions());


        return this.frame;
    }

    public List<Integer> getListId() {
        IListe liste = this.listeAddPanel.getListe();

        if (liste != null) {
            List<Integer> listeIds = new ArrayList<Integer>(liste.getRowCount());
            for (int i = 0; i < liste.getRowCount(); i++) {
                listeIds.add(liste.idFromIndex(i));
            }
            return listeIds;
        }
        return null;
    }

    private JPanel getPanelLegende() {
        JPanel panelLegende = new JPanel();

        JLabel labelLeg = new JLabel("Légende : ");
        labelLeg.setOpaque(true);
        panelLegende.add(labelLeg);

        // Acompte
        JLabel labelAcompte = new JLabel("  Acompte ");
        labelAcompte.setOpaque(true);
        labelAcompte.setBackground(ListeFactureRenderer.acompte);
        final Border lineBorder = BorderFactory.createEtchedBorder();
        labelAcompte.setBorder(lineBorder);
        panelLegende.add(labelAcompte);

        // Complement
        JLabel labelCompl = new JLabel("  Complément ");
        labelCompl.setOpaque(true);
        labelCompl.setBackground(ListeFactureRenderer.complement);
        labelCompl.setBorder(lineBorder);
        panelLegende.add(labelCompl);

        // Previsionnelle
        JLabel labelPrev = new JLabel("  Prévisionnelle ");
        labelPrev.setOpaque(true);
        labelPrev.setBackground(ListeFactureRenderer.prev);
        labelPrev.setBorder(lineBorder);

        panelLegende.add(labelPrev);

        return panelLegende;
    }
}
