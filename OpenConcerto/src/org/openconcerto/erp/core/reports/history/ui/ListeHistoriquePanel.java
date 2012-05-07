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
 
 package org.openconcerto.erp.core.reports.history.ui;

import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.accounting.ui.ListeGestCommEltPanel;
import org.openconcerto.erp.core.finance.accounting.ui.SuppressionEcrituresPanel;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureElementXmlSheet;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.quote.ui.EtatDevisRenderer;
import org.openconcerto.erp.generationDoc.AbstractSheetXml;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.rights.JListSQLTablePanel;
import org.openconcerto.sql.view.IListPanel;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class ListeHistoriquePanel extends JPanel {

    private final Vector<IListPanel> vectListePanel = new Vector<IListPanel>();
    private Map<Integer, List<TableModelListener>> mapListener = new HashMap<Integer, List<TableModelListener>>();
    private JListSQLTablePanel jListePanel;

    private Map<SQLTable, SQLField> listFieldMap = new HashMap<SQLTable, SQLField>();
    private Map<String, Where> whereList = new HashMap<String, Where>();
    private static Map<SQLElement, Class<? extends AbstractSheetXml>> elementSheet = new HashMap<SQLElement, Class<? extends AbstractSheetXml>>();
    private String undefinedLabel;

    static {
        SQLElementDirectory dir = Configuration.getInstance().getDirectory();
        elementSheet.put(dir.getElement("SAISIE_VENTE_FACTURE"), VenteFactureXmlSheet.class);
        elementSheet.put(dir.getElement("SAISIE_VENTE_FACTURE_ELEMENT"), VenteFactureElementXmlSheet.class);
    }

    // Filtre à partir de la JList sur les IListe
    private final ListSelectionListener listListener = new ListSelectionListener() {

        public void valueChanged(ListSelectionEvent e) {

            int selectIndex = ListeHistoriquePanel.this.jListePanel.getSelectedIndex();

            SQLRowAccessor row = ListeHistoriquePanel.this.jListePanel.getModel().getRowAt(selectIndex);

            if ((row == null || row.isUndefined()) && undefinedLabel == null) {
                return;
            }

            int id = SQLRow.NONEXISTANT_ID;
            if (row != null) {
                id = row.getID();
            }

            for (int i = 0; i < ListeHistoriquePanel.this.vectListePanel.size(); i++) {
                IListPanel liste = ListeHistoriquePanel.this.vectListePanel.get(i);

                // remove listener
                if (ListeHistoriquePanel.this.mapListener.get(i) != null) {
                    List<TableModelListener> l = ListeHistoriquePanel.this.mapListener.get(i);
                    for (TableModelListener listener : l) {
                        liste.getListe().getTableModel().removeTableModelListener(listener);
                    }
                }

                Where w = null;
                final SQLTable table = liste.getElement().getTable();
                for (String key : ListeHistoriquePanel.this.whereList.keySet()) {
                    Where wTmp = ListeHistoriquePanel.this.whereList.get(key);

                    if (liste.getListe().getRequest().getAllFields().containsAll(wTmp.getFields())) {
                        if (w == null) {
                            w = wTmp;
                        } else {
                            w = w.and(wTmp);
                        }
                    }
                }
                if (id > 1) {
                    if (ListeHistoriquePanel.this.listFieldMap != null && ListeHistoriquePanel.this.listFieldMap.get(table) != null) {
                        SQLField field = ListeHistoriquePanel.this.listFieldMap.get(table);
                        Where w2 = new Where(field, "=", table.getForeignTable(field.getName()).getKey());
                        w2 = w2.and(new Where(table.getForeignTable(field.getName()).getField("ID_" + ListeHistoriquePanel.this.jListePanel.getModel().getTable().getName()), "=", id));
                        liste.getListe().getRequest().setWhere(w2.and(w));
                    } else {
                        if (liste.getElement().getTable().equals(jListePanel.getModel().getTable())) {
                            liste.getListe().getRequest().setWhere(new Where(table.getKey(), "=", id).and(w));
                        } else {
                            liste.getListe().getRequest().setWhere(new Where(table.getField("ID_" + ListeHistoriquePanel.this.jListePanel.getModel().getTable().getName()), "=", id).and(w));
                        }
                    }
                } else {
                    liste.getListe().getRequest().setWhere(w);
                }
                liste.getListe().setSQLEditable(false);
                // Set renderer
                setRenderer(liste);

                // Set listener
                if (ListeHistoriquePanel.this.mapListener.get(i) != null) {
                    List<TableModelListener> l = ListeHistoriquePanel.this.mapListener.get(i);
                    for (TableModelListener listener : l) {
                        liste.getListe().getTableModel().addTableModelListener(listener);
                        if (elementSheet.get(liste.getElement()) != null) {
                            liste.getListe().addIListeActions(new MouseSheetXmlListeListener(elementSheet.get(liste.getElement())).getRowActions());
                        }
                    }
                }

            }
        }
    };

    public ListeHistoriquePanel(final String title, final SQLTable tableList, Map<String, List<String>> listTableOnglet, JPanel panelBottom, Map<SQLTable, SQLField> listFieldMap) {
        this(title, tableList, listTableOnglet, panelBottom, listFieldMap, "Tous");
    }

    // TODO verifier que les tables contiennent bien la clef etrangere
    /**
     * @param title titre de la JList
     * @param tableList table à afficher la JList
     * @param listTableOnglet liste des tables à afficher
     * @param panelBottom panel à afficher en bas de la frame
     * @param listFieldMap jointure d'une table pour utiliser le filtre si la table ne contient pas
     *        de foreignKey pointant sur tableList
     * @param undefinedLabel label pour l'indéfini permettant de tout sélectionner, null si
     *        l'undefined n'est pas à inclure.
     */
    public ListeHistoriquePanel(final String title, final SQLTable tableList, Map<String, List<String>> listTableOnglet, JPanel panelBottom, Map<SQLTable, SQLField> listFieldMap, String undefinedLabel) {
        super();
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 1, 2);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;

        this.listFieldMap = listFieldMap;

        // Onglet de IListe
        JTabbedPane tabbedPane = new JTabbedPane();

        for (String key : listTableOnglet.keySet()) {

            List<String> listPanelTable = listTableOnglet.get(key);

            JPanel tabbedPanel = new JPanel(new GridBagLayout());
            tabbedPanel.setOpaque(false);
            GridBagConstraints c2 = new DefaultGridBagConstraints();
            c2.fill = GridBagConstraints.BOTH;
            c2.weightx = 1;
            c2.weighty = 1;
            c2.gridy = GridBagConstraints.RELATIVE;

            for (int i = 0; i < listPanelTable.size(); i++) {
                final SQLElement elt = Configuration.getInstance().getDirectory().getElement(listPanelTable.get(i));

                IListPanel liste;

                if (elt.getTable().contains("ID_MOUVEMENT")) {
                    liste = new ListeGestCommEltPanel(elt, Where.FALSE, "historique-" + title) {
                        protected void handleAction(JButton source, ActionEvent evt) {

                            if (elt.getTable().contains("ID_MOUVEMENT")) {
                                SQLRow row = getListe().getSelectedRow();
                                if (source == this.buttonModifier) {
                                    MouvementSQLElement.showSource(row.getInt("ID_MOUVEMENT"));
                                } else {
                                    if (source == this.buttonEffacer) {
                                        PanelFrame f = new PanelFrame(new SuppressionEcrituresPanel(row.getInt("ID_MOUVEMENT")), "Suppresion d'une pièce");
                                        f.setLocationRelativeTo(null);
                                        f.setResizable(false);
                                        f.setVisible(true);
                                    } else {
                                        super.handleAction(source, evt);
                                    }
                                }
                            } else {
                                super.handleAction(source, evt);
                            }
                        }
                    };

                } else {
                    liste = new ListeAddPanel(elt, new IListe(elt.createTableSource(Where.FALSE)), "historique-" + title) {
                        @Override
                        protected void handleAction(JButton source, ActionEvent evt) {
                            if (source == this.buttonAjouter) {
                                final boolean deaf = isDeaf();
                                // toujours remplir la createFrame avec la ligne sélectionnée
                                // car la frame écoute la sélection mais pas les modif, et se reset
                                // qd on la ferme
                                // donc si on clic ajouter, on ferme, on modif la ligne, on clic
                                // ajouter
                                // on doit reremplir l'EditFrame
                                int selectIndex = ListeHistoriquePanel.this.jListePanel.getSelectedIndex();
                                SQLRowAccessor row = ListeHistoriquePanel.this.jListePanel.getModel().getRowAt(selectIndex);
                                if (row != null && !row.isUndefined()) {
                                    SQLTable table = this.getCreateFrame().getSQLComponent().getElement().getTable();
                                    Set<SQLField> fields = table.getForeignKeys(ListeHistoriquePanel.this.jListePanel.getModel().getTable());
                                    if (fields != null && fields.size() > 0) {
                                        SQLRowValues rowVals = new SQLRowValues(table);
                                        rowVals.put(((SQLField) fields.toArray()[0]).getName(), row.getID());
                                        this.getCreateFrame().getSQLComponent().resetValue();
                                        this.getCreateFrame().getSQLComponent().select(rowVals);
                                    }
                                }
                                FrameUtil.show(this.getCreateFrame());
                            } else {
                                super.handleAction(source, evt);
                            }
                        }
                    };
                }

                this.vectListePanel.add(liste);

                setRenderer(liste);
                if (elementSheet.get(liste.getElement()) != null) {
                    liste.getListe().addIListeActions(new MouseSheetXmlListeListener(elementSheet.get(liste.getElement())).getRowActions());
                }
                liste.getListe().setSQLEditable(false);
                liste.setOpaque(false);
                liste.setBorder(null);

                if (listPanelTable.size() > 1) {
                    Font f = UIManager.getFont("TitledBorder.font");
                    f = f.deriveFont(Font.BOLD);
                    Border b = UIManager.getBorder("TitledBorder.border");
                    b = BorderFactory.createLineBorder(Color.BLACK);
                    liste.setBorder(BorderFactory.createTitledBorder(b, elt.getPluralName(), TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, f));
                }

                tabbedPanel.add(liste, c2);

            }

            tabbedPane.add(key, tabbedPanel);
        }

        // Left Panel
        SQLElement e = Configuration.getInstance().getDirectory().getElement(tableList);

        List<String> fields = getListSQLField(e.getComboRequest().getFields());
        this.jListePanel = new JListSQLTablePanel(tableList, fields, undefinedLabel);

        // Right panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new GridBagLayout());
        GridBagConstraints cRight = new DefaultGridBagConstraints();
        cRight.fill = GridBagConstraints.BOTH;
        cRight.weightx = 1;
        cRight.weighty = 1;
        rightPanel.add(tabbedPane, cRight);

        if (panelBottom != null) {
            cRight.fill = GridBagConstraints.HORIZONTAL;
            cRight.weightx = 1;
            cRight.weighty = 0;
            cRight.gridy++;
            panelBottom.setBorder(BorderFactory.createTitledBorder("Récapitulatif"));
            rightPanel.add(panelBottom, cRight);
        }
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.jListePanel, rightPanel);
        split.setBorder(null);
        split.setDividerLocation(275);
        this.add(split, c);

        JButton buttonClose = new JButton("Fermer");
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.add(buttonClose, c);

        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                ((JFrame) SwingUtilities.getRoot(ListeHistoriquePanel.this)).dispose();
            }
        });

        this.jListePanel.addListSelectionListener(this.listListener);
        this.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {

            }

            @Override
            public void ancestorMoved(AncestorEvent event) {

            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                jListePanel.removeListSelectionListener(listListener);
            }
        });
    }

    public void selectIDinJList(int id) {
        // int index = this.jListePanel.getModel().getIndexForId(id);
        // if (index >= 0) {
        // this.jListePanel.getJList().setSelectedIndex(index);
        // this.jListePanel.getJList().ensureIndexIsVisible(index);
        // }

        this.jListePanel.selectID(id);
    }

    private void setRenderer(IListPanel liste) {

        SQLElement propositionItemElement = Configuration.getInstance().getDirectory().getElement("PROPOSITION_ELEMENT");
        SQLElement devisElement = Configuration.getInstance().getDirectory().getElement("DEVIS");
        JTable table = liste.getListe().getJTable();
        TableCellRenderer rend = null;
        if (liste.getElement().getClass() == devisElement.getClass()) {
            rend = new EtatDevisRenderer();
        } else {
                rend = new DeviseNiceTableCellRenderer();
        }
        for (int j = 0; j < table.getColumnCount(); j++) {

            if (rend instanceof EtatDevisRenderer) {
                table.getColumnModel().getColumn(j).setCellRenderer(rend);
            } else {
                if (table.getColumnClass(j) == Long.class || table.getColumnClass(j) == BigInteger.class) {
                    table.getColumnModel().getColumn(j).setCellRenderer(rend);
                }
            }
        }
    }

    /**
     * @param col Collection de SQLField
     * @return liste des noms des champs contenus dans col
     */
    private List<String> getListSQLField(Collection<SQLField> col) {
        List<String> l = new ArrayList<String>();
        for (Iterator<SQLField> i = col.iterator(); i.hasNext();) {
            SQLField field = i.next();
            l.add(field.getName());
        }
        return l;
    }

    public void addListSelectionListener(ListSelectionListener l) {
        this.jListePanel.addListSelectionListener(l);
    }

    public void removeListSelectionListener(ListSelectionListener l) {
        this.jListePanel.removeListSelectionListener(l);
    }

    public SQLRowAccessor getSelectedRow() {
        return this.jListePanel.getModel().getRowAt(this.jListePanel.getSelectedIndex());
    }

    public void addWhere(String key, Where w) {
        this.whereList.put(key, w);
        this.listListener.valueChanged(null);
    }

    public void removeWhere(String key) {
        this.whereList.remove(key);
        this.listListener.valueChanged(null);
    }

    public void removeAllWhere() {
        for (String key : this.whereList.keySet()) {
            this.whereList.remove(key);
        }
        this.listListener.valueChanged(null);
    }

    public Map<String, Where> getWhere() {
        return this.whereList;
    }

    public List<Integer> getListId(String tableName) {
        IListe liste = getIListeFromTableName(tableName);
        List<Integer> listeIds = null;
        if (liste != null) {
            int size = liste.getRowCount();
            listeIds = new ArrayList<Integer>(size);

            for (int i = 0; i < size; i++) {
                listeIds.add(liste.idFromIndex(i));
            }
        } else {
            listeIds = Collections.EMPTY_LIST;
        }
        return listeIds;
    }

    public void removeAllTableListener() {
        this.jListePanel.removeAllTableListener();
        for (Integer i : this.mapListener.keySet()) {
            IListPanel panel = vectListePanel.get(i);
            List<TableModelListener> l = this.mapListener.get(i);
            for (TableModelListener tableModelListener : l) {
                final IListe liste = panel.getListe();
                if (liste != null) {
                    final TableModel tableModel = liste.getTableModel();
                    if (tableModel != null) {
                        tableModel.removeTableModelListener(tableModelListener);
                    }
                }
            }

        }
    }

    public void addListenerTable(TableModelListener listener, String tableName) {
        IListe liste = getIListeFromTableName(tableName);
        int index = getIndexFromTableName(tableName);
        if (liste != null) {
            liste.getTableModel().addTableModelListener(listener);
            List<TableModelListener> l = this.mapListener.get(liste);
            if (l == null) {
                l = new ArrayList<TableModelListener>();
                this.mapListener.put(index, l);
            }
            l.add(listener);
        }
    }

    /**
     * Permet d'obtenir la IListe correspondant au nom d'une table
     * 
     * @param tableName nom de la table
     * @return la Iliste associée, dans le cas échéant null
     */

    public IListe getIListeFromTableName(String tableName) {
        IListe liste = null;
        for (int i = 0; i < this.vectListePanel.size(); i++) {
            IListPanel listeTmp = this.vectListePanel.get(i);
            // FIXME Null pointer Exception when client deleted
            if (listeTmp != null) {
                IListe list = listeTmp.getListe();
                if (list != null) {
                    final ITableModel model = list.getModel();
                    if (model != null) {
                        if (model.getTable().getName().equalsIgnoreCase(tableName)) {
                            liste = listeTmp.getListe();
                        }
                    }
                }
            }
        }
        return liste;
    }

    /**
     * Permet d'obtenir la position dans le vecteur correspondant au nom d'une table
     * 
     * @param tableName nom de la table
     * @return -1 si la table n'est pas dans le vecteur
     */
    private int getIndexFromTableName(String tableName) {

        for (int i = 0; i < this.vectListePanel.size(); i++) {
            IListPanel listeTmp = this.vectListePanel.get(i);
            if (listeTmp.getListe().getModel().getTable().getName().equalsIgnoreCase(tableName)) {
                return i;
            }
        }
        return -1;
    }

    public IListe getListe(int index) {
        return this.vectListePanel.get(index).getListe();
    }

    public void fireListesChanged() {

        for (int i = 0; i < this.vectListePanel.size(); i++) {
            IListPanel listeTmp = this.vectListePanel.get(i);
            listeTmp.getListe().getModel().fireTableDataChanged();
            listeTmp.getListe().getModel().fireTableStructureChanged();
        }

    }
}
