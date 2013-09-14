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
 
 package org.openconcerto.erp.core.sales.order.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.order.component.CommandeClientSQLComponent;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.utils.CollectionMap;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandeClientSQLElement extends ComptaSQLConfElement {

    public CommandeClientSQLElement() {
        super("COMMANDE_CLIENT", "une commande client", "commandes clients");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.BaseSQLElement#getComboFields()
     */
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.BaseSQLElement#getListFields()
     */
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_CLIENT");
        l.add("ID_COMMERCIAL");
        l.add("T_HT");
        l.add("T_TTC");
        l.add("NOM");
        l.add("INFOS");
        return l;
    }

    @Override
    protected Set<String> getChildren() {
        final Set<String> set = new HashSet<String>();
        set.add("COMMANDE_CLIENT_ELEMENT");
        return set;
    }

    @Override
    public Set<String> getReadOnlyFields() {
        final Set<String> s = new HashSet<String>();
        s.add("ID_DEVIS");
        return s;
    }

    @Override
    protected SQLTableModelSourceOnline createTableSource() {
        final SQLTableModelSourceOnline source = super.createTableSource();
        // TODO: refaire un renderer pour les commandes transférées en BL
        // final CommandeClientRenderer rend = CommandeClientRenderer.getInstance();
        // final SQLTableModelColumn col = source.getColumn(getTable().getField("T_HT"));
        // col.setColumnInstaller(new IClosure<TableColumn>() {
        // @Override
        // public void executeChecked(TableColumn input) {
        // input.setCellRenderer(rend);
        // }
        // });
        return source;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new CommandeClientSQLComponent();
    }

    /**
     * Transfert d'une commande en commande fournisseur
     * 
     * @param commandeID
     */
    public void transfertCommande(int commandeID) {

        SQLElement elt = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT_ELEMENT");
        SQLTable tableCmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT").getTable();
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
        SQLRow rowCmd = getTable().getRow(commandeID);
        List<SQLRow> rows = rowCmd.getReferentRows(elt.getTable());
        CollectionMap<SQLRow, List<SQLRowValues>> map = new CollectionMap<SQLRow, List<SQLRowValues>>();
        for (SQLRow sqlRow : rows) {
            // on récupére l'article qui lui correspond
            SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
            for (SQLField field : eltArticle.getTable().getFields()) {
                if (sqlRow.getTable().getFieldsName().contains(field.getName())) {
                    rowArticle.put(field.getName(), sqlRow.getObject(field.getName()));
                }
            }

            int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
            SQLRow rowArticleFind = eltArticle.getTable().getRow(idArticle);
            SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
            SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticleFind));
            rowValsElt.put("ID_STYLE", sqlRow.getObject("ID_STYLE"));
            rowValsElt.put("QTE", sqlRow.getObject("QTE"));
            rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
            rowValsElt.put("T_PA_HT", ((BigDecimal) rowValsElt.getObject("PA_HT")).multiply(new BigDecimal(rowValsElt.getInt("QTE")), MathContext.DECIMAL128));
            rowValsElt.put("T_PA_TTC",
                    ((BigDecimal) rowValsElt.getObject("T_PA_HT")).multiply(new BigDecimal((rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0)), MathContext.DECIMAL128));

            map.put(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);

        }
        MouvementStockSQLElement.createCommandeF(map, rowCmd.getForeignRow("ID_TARIF").getForeignRow("ID_DEVISE"), rowCmd.getString("NUMERO") + " - " + rowCmd.getString("NOM"));
    }
}
