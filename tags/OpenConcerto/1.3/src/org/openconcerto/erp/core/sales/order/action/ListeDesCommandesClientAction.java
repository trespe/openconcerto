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
 
 package org.openconcerto.erp.core.sales.order.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.component.TransfertGroupSQLComponent;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.sales.account.VenteFactureSituationSQLComponent;
import org.openconcerto.erp.core.sales.account.VenteFactureSoldeSQLComponent;
import org.openconcerto.erp.core.sales.order.element.CommandeClientSQLElement;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.ui.table.PercentTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ListeDesCommandesClientAction extends CreateFrameAbstractAction {

    public ListeDesCommandesClientAction() {
        super();
        this.putValue(Action.NAME, "Liste des commandes clients");
    }

    public JFrame createFrame() {
        final JFrame frame = new JFrame("Commandes clients");
        // Actions

        frame.getContentPane().add(createAllOrderPanel());
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        FrameUtil.setBounds(frame);
        final File file = IListFrame.getConfigFile(eltCmd, frame.getClass());
        if (file != null)
            new WindowStateManager(frame, file).loadState();
        return frame;
    }

    JPanel createAllOrderPanel() {
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        final List<RowAction> allowedActions = new ArrayList<RowAction>();
        // Transfert vers facture
        PredicateRowAction bonAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                transfertBonLivraisonClient(IListe.get(e).copySelectedRows());
            }
        }, false, "sales.order.create.deliverynote");

        // Transfert vers facture
        RowAction factureAction = new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                transfertFactureClient(IListe.get(e).copySelectedRows());
            }
        }, false, "sales.order.create.invoice") {

            @Override
            public boolean enabledFor(List<SQLRowAccessor> selection) {
                if (selection.isEmpty()) {
                    return false;
                } else if (selection.size() > 1) {
                    return true;
                } else {
                    BigDecimal d = getAvancement(selection.get(0));
                    return d.signum() == 0;
                }
            }
        };

        // Transfert vers facture intermédiaire
        RowAction acompteAction = new RowAction(new AbstractAction("Créer une facture intermédiaire") {
            public void actionPerformed(ActionEvent e) {
                transfertAcompteClient(IListe.get(e).copySelectedRows());
            }
        }, false, "sales.order.create.account") {
            BigDecimal cent = BigDecimal.ONE.movePointRight(2);

            @Override
            public boolean enabledFor(List<SQLRowAccessor> selection) {
                if (selection.isEmpty() || selection.size() > 1) {
                    return false;
                } else {
                    BigDecimal d = getAvancement(selection.get(0));
                    return NumberUtils.compare(d, cent) != 0;
                }
            }
        };

        // Transfert vers facture solde
        RowAction soldeAction = new RowAction(new AbstractAction("Facturer le solde") {
            public void actionPerformed(ActionEvent e) {
                transfertSoldeClient(IListe.get(e).copySelectedRows());
            }
        }, false, "sales.order.create.account.solde") {
            BigDecimal cent = BigDecimal.ONE.movePointRight(2);

            @Override
            public boolean enabledFor(List<SQLRowAccessor> selection) {
                if (selection.isEmpty() || selection.size() > 1) {
                    return false;
                } else {
                    BigDecimal d = getAvancement(selection.get(0));
                    return NumberUtils.compare(d, cent) != 0 && NumberUtils.compare(d, BigDecimal.ZERO) != 0;
                }
            }
        };

        // Transfert vers commande
        PredicateRowAction cmdAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                final int selectedId = IListe.get(e).getSelectedId();
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        final CommandeClientSQLElement elt = (CommandeClientSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
                        elt.transfertCommande(selectedId);

                    }
                });

            }

        }, false, "sales.order.create.supplier.order");

        cmdAction.setPredicate(IListeEvent.getSingleSelectionPredicate());

        bonAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        allowedActions.add(bonAction);
        allowedActions.add(factureAction);
        allowedActions.add(acompteAction);
        allowedActions.add(soldeAction);
        allowedActions.add(cmdAction);

        final BaseSQLTableModelColumn colAvancement = new BaseSQLTableModelColumn("Avancement facturation", BigDecimal.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                return getAvancement(r);
            }

            @Override
            public Set<FieldPath> getPaths() {
                final Path p = new PathBuilder(eltCmd.getTable()).addTable("TR_COMMANDE_CLIENT").addTable("SAISIE_VENTE_FACTURE").build();
                return CollectionUtils.createSet(new FieldPath(p, "T_HT"));
            }
        };
        tableSource.getColumns().add(colAvancement);
        colAvancement.setRenderer(new PercentTableCellRenderer());
        final ListeAddPanel panel = getPanel(eltCmd, tableSource, allowedActions);
        return panel;
    }

    private BigDecimal getAvancement(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("TR_COMMANDE_CLIENT"));
        long totalFact = 0;
        long total = r.getLong("T_HT");
        for (SQLRowAccessor row : rows) {
            if (!row.isForeignEmpty("ID_SAISIE_VENTE_FACTURE")) {
                SQLRowAccessor rowFact = row.getForeign("ID_SAISIE_VENTE_FACTURE");
                Long l = rowFact.getLong("T_HT");
                totalFact += l;
            }
        }
        if (total > 0) {
            return new BigDecimal(totalFact).divide(new BigDecimal(total), MathContext.DECIMAL128).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ONE.movePointRight(2);
        }
    }

    private ListeAddPanel getPanel(final SQLElement eltCmd, final SQLTableModelSourceOnline tableSource, final List<RowAction> allowedActions) {
        final ListeAddPanel panel = new ListeAddPanel(eltCmd, new IListe(tableSource)) {
            @Override
            protected void createUI() {
                super.createUI();
                this.btnMngr.setAdditional(this.buttonEffacer, new ITransformer<JButton, String>() {

                    @Override
                    public String transformChecked(JButton input) {

                        SQLRowAccessor row = getListe().fetchSelectedRow();

                        BigDecimal b = getAvancement(row);

                        if (b.signum() != 0) {
                            return "Vous ne pouvez pas supprimer une commande facturée !";
                        }
                        return null;
                    }
                });
                this.btnMngr.setAdditional(this.buttonModifier, new ITransformer<JButton, String>() {

                    @Override
                    public String transformChecked(JButton input) {

                        SQLRowAccessor row = getListe().fetchSelectedRow();

                        BigDecimal b = getAvancement(row);

                        if (b.signum() != 0) {
                            return "Vous ne pouvez pas modifier une commande facturée !";
                        }
                        return null;
                    }
                });
            }
        };

        final List<SQLField> fields = new ArrayList<SQLField>(2);
        fields.add(eltCmd.getTable().getField("T_HT"));
        final IListTotalPanel totalPanel = new IListTotalPanel(panel.getListe(), fields, "Total des commandes de la liste");

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridy = 4;

        // Date panel
        final IListFilterDatePanel datePanel = new IListFilterDatePanel(panel.getListe(), eltCmd.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());

        panel.getListe().addIListeActions(new MouseSheetXmlListeListener(CommandeClientXmlSheet.class) {
            @Override
            public List<RowAction> addToMenu() {
                return allowedActions;
            }
        }.getRowActions());

        datePanel.setFilterOnDefault();

        final JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        bottomPanel.setOpaque(false);
        final GridBagConstraints c2 = new DefaultGridBagConstraints();
        c2.fill = GridBagConstraints.NONE;
        c2.weightx = 1;
        bottomPanel.add(datePanel, c2);

        c2.gridx++;
        c2.weightx = 0;
        c2.anchor = GridBagConstraints.EAST;
        bottomPanel.add(totalPanel, c2);

        panel.add(bottomPanel, c);
        return panel;
    }

    /**
     * Transfert en BL
     * 
     * @param row
     */
    private void transfertBonLivraisonClient(List<SQLRowValues> rows) {
        TransfertBaseSQLComponent.openTransfertFrame(rows, "BON_DE_LIVRAISON");
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertFactureClient(List<SQLRowValues> rows) {
        TransfertBaseSQLComponent.openTransfertFrame(rows, "SAISIE_VENTE_FACTURE");

    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertAcompteClient(List<SQLRowValues> rows) {
        TransfertGroupSQLComponent.openTransfertFrame(rows, "SAISIE_VENTE_FACTURE", VenteFactureSituationSQLComponent.ID);
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertSoldeClient(List<SQLRowValues> rows) {
        TransfertGroupSQLComponent.openTransfertFrame(rows, "SAISIE_VENTE_FACTURE", VenteFactureSoldeSQLComponent.ID);
    }
}
