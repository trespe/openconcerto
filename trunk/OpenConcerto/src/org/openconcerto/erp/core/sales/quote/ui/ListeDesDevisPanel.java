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
 
 package org.openconcerto.erp.core.sales.quote.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.sales.invoice.ui.DateEnvoiRenderer;
import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.PercentTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.SwingWorker2;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

public class ListeDesDevisPanel extends JPanel {

    private JTabbedPane tabbedPane = new JTabbedPane();
    private Map<Integer, ListeAddPanel> map = new HashMap<Integer, ListeAddPanel>();
    private SQLElement eltDevis = Configuration.getInstance().getDirectory().getElement("DEVIS");
    private SQLElement eltEtatDevis = Configuration.getInstance().getDirectory().getElement("ETAT_DEVIS");

    public ListeDesDevisPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;

        // Tous
        ListeAddPanel panelAll = createPanel(-1);
        this.map.put(this.tabbedPane.getTabCount(), panelAll);
        this.tabbedPane.add("Tous", panelAll);

        // En cours
        ListeAddPanel panelCours = createPanel(EtatDevisSQLElement.EN_COURS);
        this.map.put(this.tabbedPane.getTabCount(), panelCours);

        this.tabbedPane.add("  ", panelCours);

        // Attente
        ListeAddPanel panelAttente = createPanel(EtatDevisSQLElement.EN_ATTENTE);
        this.map.put(this.tabbedPane.getTabCount(), panelAttente);
        this.tabbedPane.add("  ", panelAttente);

        // Accepte
        ListeAddPanel panelAccepte = createPanel(EtatDevisSQLElement.ACCEPTE);
        this.map.put(this.tabbedPane.getTabCount(), panelAccepte);
        this.tabbedPane.add("  ", panelAccepte);

        // Refuse
        ListeAddPanel panelRefuse = createPanel(EtatDevisSQLElement.REFUSE);
        this.map.put(this.tabbedPane.getTabCount(), panelRefuse);
        this.tabbedPane.add("  ", panelRefuse);
        Map<IListe, SQLField> mapDate = new HashMap<IListe, SQLField>();


        this.tabbedPane.setSelectedIndex(1);

        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.tabbedPane, c);

        // Date panel

        mapDate.put(panelAttente.getListe(), eltDevis.getTable().getField("DATE"));
        mapDate.put(panelAccepte.getListe(), eltDevis.getTable().getField("DATE"));
        mapDate.put(panelRefuse.getListe(), eltDevis.getTable().getField("DATE"));
        mapDate.put(panelCours.getListe(), eltDevis.getTable().getField("DATE"));
        mapDate.put(panelAll.getListe(), eltDevis.getTable().getField("DATE"));

        final IListFilterDatePanel datePanel = new IListFilterDatePanel(mapDate, IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        c.weighty = 0;
        datePanel.setFilterOnDefault();
        this.add(datePanel, c);
        initTabTitles();
    }


    private void initTabTitles() {
        SwingWorker2<List<String>, Object> worker = new SwingWorker2<List<String>, Object>() {

            @Override
            protected List<String> doInBackground() throws Exception {
                final SQLSelect quoteStates = new SQLSelect();
                final SQLTable quoteStatesTable = eltEtatDevis.getTable();
                quoteStates.addSelect(quoteStatesTable.getKey());
                quoteStates.addSelect(quoteStatesTable.getField("NOM"));
                final List<Integer> labelIds = Arrays.asList(EtatDevisSQLElement.EN_COURS, EtatDevisSQLElement.EN_ATTENTE, EtatDevisSQLElement.ACCEPTE, EtatDevisSQLElement.REFUSE);
                quoteStates.setWhere(new Where(quoteStatesTable.getKey(), true, labelIds));
                @SuppressWarnings("unchecked")
                final List<Map<String, Object>> values = quoteStatesTable.getDBSystemRoot().getDataSource().execute(quoteStates.asString());
                final List<String> tabNames = new ArrayList<String>();

                final String keyFieldName = quoteStatesTable.getKey().getName();
                for (Integer id : labelIds) {
                    for (Map<String, Object> m : values) {
                        if (m.get(keyFieldName).equals(id)) {
                            tabNames.add(m.get("NOM").toString());
                            break;
                        }
                    }
                }

                return tabNames;
            }

            @Override
            protected void done() {
                final List<String> tabNames;
                try {
                    tabNames = get();
                    for (int index = 0; index < tabNames.size(); index++) {
                        tabbedPane.setTitleAt(index + 1, tabNames.get(index));
                    }
                } catch (Exception e) {
                    ExceptionHandler.handle("Unable to set tab names", e);
                }
            }
        };
        worker.execute();

    }

    protected void setRenderer(SQLTableModelSourceOnline source) {

    }

    private BigDecimal getAvancement(SQLRowAccessor r) {
        Collection<? extends SQLRowAccessor> rows = r.getReferentRows(r.getTable().getTable("TR_DEVIS"));
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

    private ListeAddPanel createPanel(int idFilter) {
        // Filter
        final SQLTableModelSourceOnline lAttente = this.eltDevis.getTableSource(true);
        final SQLTableModelColumnPath dateEnvoiCol;
        if (idFilter == EtatDevisSQLElement.ACCEPTE) {
            dateEnvoiCol = new SQLTableModelColumnPath(this.eltDevis.getTable().getField("DATE_ENVOI"));
            lAttente.getColumns().add(dateEnvoiCol);
            dateEnvoiCol.setRenderer(new DateEnvoiRenderer());
            dateEnvoiCol.setEditable(true);

            final BaseSQLTableModelColumn colAvancement = new BaseSQLTableModelColumn("Avancement facturation", BigDecimal.class) {

                @Override
                protected Object show_(SQLRowAccessor r) {

                    return getAvancement(r);
                }

                @Override
                public Set<FieldPath> getPaths() {
                    final Path p = new PathBuilder(eltDevis.getTable()).addTable("TR_DEVIS").addTable("SAISIE_VENTE_FACTURE").build();
                    return CollectionUtils.createSet(new FieldPath(p, "T_HT"));
                }
            };
            lAttente.getColumns().add(colAvancement);
            colAvancement.setRenderer(new PercentTableCellRenderer());
        } else {
            dateEnvoiCol = null;
        }
        if (idFilter > 1) {
            Where wAttente = new Where(this.eltDevis.getTable().getField("ID_ETAT_DEVIS"), "=", idFilter);
            lAttente.getReq().setWhere(wAttente);
        } else {
            lAttente.getColumns().add(new BaseSQLTableModelColumn("Etat", String.class) {

                @Override
                protected Object show_(SQLRowAccessor r) {
                    // TODO Raccord de méthode auto-généré
                    return r.getForeign("ID_ETAT_DEVIS").getString("NOM");
                }

                @Override
                public Set<FieldPath> getPaths() {
                    // TODO Raccord de méthode auto-généré
                    Set<FieldPath> s = new HashSet<FieldPath>();
                    SQLTable table = eltDevis.getTable();
                    s.add(table.getField("ID_ETAT_DEVIS").getFieldPath());
                    // Path p = new Path(table);
                    // p.add(table.getForeignTable("ID_ETAT_DEVIS"));
                    // s.add(new FieldPath(p, "NOM"));
                    return s;
                }
            });
        }

        setRenderer(lAttente);
        // one config file per idFilter since they haven't the same number of
        // columns
        final ListeAddPanel pane = new ListeAddPanel(this.eltDevis, new IListe(lAttente), "idFilter" + idFilter);

        IListTotalPanel total;
        if (this.eltDevis.getTable().contains("PREBILAN")) {
            // asList = Arrays.asList(this.eltDevis.getTable().getField("PREBILAN"),
            // this.eltDevis.getTable().getField("T_HT"));
            List<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>> fields = new ArrayList<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>>(2);
            fields.add(Tuple2.create(pane.getListe().getSource().getColumn(this.eltDevis.getTable().getField("T_HT")), IListTotalPanel.Type.SOMME));
            fields.add(Tuple2.create(pane.getListe().getSource().getColumn(this.eltDevis.getTable().getField("PREBILAN")), IListTotalPanel.Type.SOMME));
            fields.add(Tuple2.create(new BaseSQLTableModelColumn("%MB", String.class) {

                @Override
                protected Object show_(SQLRowAccessor r) {
                    // TODO Raccord de méthode auto-généré
                    return null;
                }

                @Override
                public Set<FieldPath> getPaths() {
                    // TODO Raccord de méthode auto-généré
                    return null;
                }
            }, IListTotalPanel.Type.MOYENNE_MARGE));
            total = new IListTotalPanel(pane.getListe(), fields, null, "Total Global");
        } else if (this.eltDevis.getTable().contains("T_HA")) {

            total = new IListTotalPanel(pane.getListe(), Arrays.asList(this.eltDevis.getTable().getField("T_HA"), this.eltDevis.getTable().getField("T_HT")));
        } else {
            total = new IListTotalPanel(pane.getListe(), Arrays.asList(this.eltDevis.getTable().getField("T_HT")));
        }

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridy = 4;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        pane.add(total, c);

        // Renderer
        JTable table = pane.getListe().getJTable();

        if (idFilter == EtatDevisSQLElement.ACCEPTE) {

            pane.getListe().setSQLEditable(true);
            // Edition des dates d'envois
            TableColumn columnDateEnvoi = pane.getListe().getJTable().getColumnModel().getColumn(table.getColumnCount() - 1);
            columnDateEnvoi.setCellEditor(new org.openconcerto.ui.table.TimestampTableCellEditor());
            final SQLTableModelSourceOnline src = (SQLTableModelSourceOnline) pane.getListe().getModel().getReq();
            for (SQLTableModelColumn column : src.getColumns()) {
                if (column != dateEnvoiCol && column.getClass().isAssignableFrom(SQLTableModelColumnPath.class)) {
                    ((SQLTableModelColumnPath) column).setEditable(false);
                }
            }
        }

        // MouseSheetXmlListeListener mouseSheetXmlListeListener = new
        // MouseSheetXmlListeListener(DevisXmlSheet.class) {
        // @Override
        // public List<RowAction> addToMenu() {
        //
        // // int type =
        // // pane.getListe().getSelectedRow().getInt("ID_ETAT_DEVIS");
        // // factureAction.setEnabled(type ==
        // // EtatDevisSQLElement.ACCEPTE);
        // // commandeAction.setEnabled(type ==
        // // EtatDevisSQLElement.ACCEPTE);
        // // if (type == EtatDevisSQLElement.EN_ATTENTE) {
        // // list.add(accepteAction);
        // // }
        // // list.add(factureAction);
        // // list.add(commandeAction);
        // // list.add(actionTransfertCmd);
        // }
        // };
        // mouseSheetXmlListeListener.setGenerateHeader(true);
        // mouseSheetXmlListeListener.setShowHeader(true);
        // pane.getListe().addIListeActions(mouseSheetXmlListeListener.getRowActions());

        // activation des boutons
        // pane.getListe().addIListener(new IListener() {
        // public void selectionId(int id, int field) {
        // checkButton(id);
        // }
        // });

        pane.getListe().setOpaque(false);

        pane.setOpaque(false);
        return pane;
    }

    public Map<Integer, ListeAddPanel> getListePanel() {
        return this.map;
    }
}
