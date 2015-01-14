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
 
 package org.openconcerto.erp.core.sales.product.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.sales.product.ui.FamilleArticlePanel;
import org.openconcerto.erp.panel.ITreeSelection;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.PanelFrame;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class ListeDesArticlesAction extends CreateFrameAbstractAction {

    private PanelFrame panelFrame;
    String title = "Liste des articles";
    private final SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
    private final SQLTable sqlTableFamilleArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("FAMILLE_ARTICLE");

    public ListeDesArticlesAction() {
        super();
        this.putValue(Action.NAME, "Liste des articles");
    }

    public JFrame createFrame() {
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement(this.sqlTableArticle);
        final FamilleArticlePanel panelFam = new FamilleArticlePanel(elt.getForeignElement("ID_FAMILLE_ARTICLE"));

        // Renderer pour les devises
        // frame.getPanel().getListe().getJTable().setDefaultRenderer(Long.class, new
        // DeviseNiceTableCellRenderer());
        final SQLTableModelSourceOnline createTableSource = elt.createTableSource(getWhere(panelFam));
        final BaseSQLTableModelColumn colStock = new BaseSQLTableModelColumn("Valeur HT du stock", BigDecimal.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                SQLRowAccessor stock = r.getForeign("ID_STOCK");
                if (stock == null || stock.isUndefined()) {
                    return BigDecimal.ZERO;
                } else {
                    float qte = stock.getFloat("QTE_REEL");
                    BigDecimal ha = r.getBigDecimal("PA_HT");

                    BigDecimal total = ha.multiply(new BigDecimal(qte), MathContext.DECIMAL128);
                    if (total.signum() == 1) {
                        return total;
                    } else {
                        return BigDecimal.ZERO;
                    }
                }
            }

            @Override
            public Set<FieldPath> getPaths() {
                final SQLTable table = elt.getTable();
                Path p = new Path(table);
                Path p2 = new Path(table).addForeignField("ID_STOCK");
                return CollectionUtils.createSet(new FieldPath(p, "PA_HT"), new FieldPath(p2, "QTE_REEL"));
            }
        };
        colStock.setRenderer(ComptaSQLConfElement.CURRENCY_RENDERER);
        createTableSource.getColumns().add(colStock);
        IListe liste = new IListe(createTableSource);
        final ListeAddPanel panel = new ListeAddPanel(elt, liste);

        List<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>> fields = new ArrayList<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>>(1);
        fields.add(Tuple2.create(liste.getSource().getColumn(liste.getSource().getColumns().size() - 1), IListTotalPanel.Type.SOMME));
        IListTotalPanel total = new IListTotalPanel(liste, fields, null, "Total");
        GridBagConstraints c2 = new DefaultGridBagConstraints();
        c2.gridy = 4;
        c2.anchor = GridBagConstraints.EAST;
        c2.weightx = 0;
        c2.fill = GridBagConstraints.NONE;
        panel.add(total, c2);

        JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(panelFam), panel);
        JPanel panelAll = new JPanel(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        panelAll.add(pane, c);

        final ITreeSelection tree = panelFam.getFamilleTree();
        tree.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                panel.getListe().getRequest().setWhere(getWhere(panelFam));

            }
        });

        // rafraichir le titre à chaque changement de la liste
        panel.getListe().addListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                setTitle(panel);
            }
        });
        panel.getListe().addListenerOnModel(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() == null || evt.getPropertyName().equals("loading") || evt.getPropertyName().equals("searching"))
                    setTitle(panel);
            }
        });

        panelFam.getCheckObsolete().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                panel.getListe().getRequest().setWhere(getWhere(panelFam));
            }
        });

        this.panelFrame = new PanelFrame(panelAll, "Liste des articles");
        return this.panelFrame;
    }

    protected void setTitle(ListeAddPanel panel) {
        String title = this.title;
        if (panel.getListe().getModel().isLoading())
            title += ", chargement en cours";
        if (panel.getListe().getModel().isSearching())
            title += ", recherche en cours";

        this.panelFrame.setTitle(title);
    }

    /**
     * Filtre par rapport à la famille sélectionnée
     * 
     * @param panel
     * @return le where approprié
     */
    public Where getWhere(FamilleArticlePanel panel) {
        int id = panel.getFamilleTree().getSelectedID();

        Where w = null;

        if (panel.getCheckObsolete().isSelected()) {
            w = new Where(this.sqlTableArticle.getField("OBSOLETE"), "=", Boolean.FALSE);
        }

        if (id > 1) {
            SQLRow row = this.sqlTableFamilleArticle.getRow(id);

            Where w2 = new Where(this.sqlTableArticle.getField("ID_FAMILLE_ARTICLE"), "=", this.sqlTableFamilleArticle.getKey());

            String code = row.getString("CODE") + ".%";
            final Where w3 = new Where(this.sqlTableFamilleArticle.getField("CODE"), "LIKE", code);
            w2 = w2.and(w3.or(new Where(this.sqlTableFamilleArticle.getKey(), "=", id)));

            if (w != null) {
                w = w.and(w2);
            } else {
                w = w2;
            }

        }
        return w;
    }
}
