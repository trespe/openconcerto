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
 
 package org.openconcerto.erp.core.finance.accounting.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

// TODO Mettre le montant et la date du mouvement ???
/***************************************************************************************************
 * Un mouvement regroupe un ensemble d'ecritures liées dont le solde est nul
 **************************************************************************************************/
public class MouvementSQLElement extends ComptaSQLConfElement {

    private final static int MODIFICATION = 1;
    private final static int READONLY = 2;

    public MouvementSQLElement() {
        super("MOUVEMENT", "un mouvement", "mouvements");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> list = new ArrayList<String>(3);
        list.add("NUMERO");
        list.add("ID_PIECE");
        list.add("SOURCE");
        return list;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("ID_PIECE");
        return list;
    }

    @Override
    protected Set<String> getChildren() {
        final Set<String> set = new HashSet<String>(1);
        set.add("ECRITURE");
        return set;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        CollectionMap<String, String> m = new CollectionMap<String, String>();
        m.put(null, "NUMERO");
        m.put(null, "ID_PIECE");
        return m;
    }

    @Override
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            @Override
            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                final JLabel labelSource = new JLabel("Source ");
                this.add(labelSource, c);

                c.gridx++;
                final JTextField source = new JTextField();
                this.add(source, c);

                this.addSQLObject(source, "SOURCE");
            }
        };
    }

    @Override
    protected void archive(final SQLRow row, final boolean cutLinks) throws SQLException {

        // si le mouvement n'est pas archive, on le supprime avec sa source
        if (MouvementSQLElement.isEditable(row.getID())) {
            super.archive(row, cutLinks);
            final String source = row.getString("SOURCE");
            final int idSource = row.getInt("IDSOURCE");

            if (source.trim().length() > 0 && idSource > 1) {
                Configuration.getInstance().getDirectory().getElement(source).archive(idSource);
            }
        } else {
            System.err.println("impossible d'arichiver le mouvement d'id [" + row.getID() + "] car il est validé.");
        }
    }

    public static final int getSourceId(final int idMvt) {
        final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final SQLTable tableMvt = base.getTable("MOUVEMENT");
        final SQLRow row = tableMvt.getRow(idMvt);
        if (row.getInt("ID_MOUVEMENT_PERE") != 1) {
            return getSourceId(row.getInt("ID_MOUVEMENT_PERE"));
        } else {
            return idMvt;
        }
    }

    public static final void showSource(final int idMvt) {

        // on recupere le mouvement racine
        final int id = getSourceId(idMvt);

        if (id != 1) {
            final EditFrame f;
            final ComptaPropsConfiguration comptaPropsConfiguration = (ComptaPropsConfiguration) Configuration.getInstance();
            final SQLTable tableMvt = comptaPropsConfiguration.getRootSociete().getTable("MOUVEMENT");
            final String stringTableSource = tableMvt.getRow(id).getString("SOURCE").trim();
            final int mode = MouvementSQLElement.isEditable(id) ? MouvementSQLElement.MODIFICATION : MouvementSQLElement.READONLY;

            if (stringTableSource.length() != 0 && tableMvt.getRow(id).getInt("IDSOURCE") != 1) {
                final SQLElement elementSource = comptaPropsConfiguration.getDirectory().getElement(stringTableSource);
                if (mode == MouvementSQLElement.MODIFICATION) {

                    f = new EditFrame(elementSource, EditPanel.MODIFICATION);
                    f.getPanel().disableDelete();
                } else {
                    f = new EditFrame(elementSource, EditPanel.READONLY);
                }
                f.selectionId(tableMvt.getRow(id).getInt("IDSOURCE"));
            } else {
                final SQLElement elementSource = comptaPropsConfiguration.getDirectory().getElement(SaisieKmSQLElement.class);
                if (mode == MouvementSQLElement.MODIFICATION) {
                    f = new EditFrame(elementSource, EditPanel.MODIFICATION);
                } else {
                    f = new EditFrame(elementSource, EditPanel.READONLY);
                }
                // FIXME se passer de requete dans Swing...
                try {
                    f.selectionId(SaisieKmSQLElement.createSaisie(id));
                } catch (Exception e) {
                    ExceptionHandler.handle("Impossible de selectionner la source", e);
                }
            }
            f.pack();
            f.setVisible(true);
        } else {
            System.err.println("Aucun mouvement associé, impossible de modifier ou d'accéder à la source de cette ecriture!");
        }
    }

    public static final boolean isEditable(final int idMvt) {

        // TODO si l'id est incorrect(<=1) renvoyé false??
        final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final SQLTable tableEcriture = base.getTable("ECRITURE");

        final SQLSelect sel = new SQLSelect(base);

        sel.addSelect(tableEcriture.getField("VALIDE"));

        final Where w = new Where(tableEcriture.getField("ID_MOUVEMENT"), "=", idMvt);

        sel.setWhere(w);

        final String req = sel.asString();
        final Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        final List myList = (List) ob;

        final int size = myList.size();
        for (int i = 0; i < size; i++) {
            final Object[] objTmp = (Object[]) myList.get(i);
            if (((Boolean) objTmp[0]).booleanValue()) {
                return false;
            }
        }

        return true;
    }

    public static final int getIDForSource(final String source, final int idSource) {
        final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final SQLTable tableMvt = base.getTable("MOUVEMENT");

        final SQLSelect sel = new SQLSelect(base);

        sel.addSelect(tableMvt.getField("ID"));

        final Where w = new Where(tableMvt.getField("SOURCE"), "=", source);
        final Where w2 = new Where(tableMvt.getField("IDSOURCE"), "=", idSource);

        sel.setWhere(w.and(w2));

        final String req = sel.asString();
        final Object ob = base.getDataSource().execute(req, new ArrayListHandler());

        final List<Object[]> myList = (List<Object[]>) ob;

        final int id;
        if (myList.size() != 0) {
            final Object[] objTmp = myList.get(0);
            id = ((Number) objTmp[0]).intValue();
        } else {
            id = 1;
        }
        return id;

    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".entry";
    }

}
