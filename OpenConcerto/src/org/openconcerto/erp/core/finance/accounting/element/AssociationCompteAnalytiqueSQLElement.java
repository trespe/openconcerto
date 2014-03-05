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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.finance.accounting.model.AssociationAnalytiqueModel;
import org.openconcerto.erp.core.finance.accounting.ui.PlanComptableCellRenderer;
import org.openconcerto.erp.element.objet.ClasseCompte;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class AssociationCompteAnalytiqueSQLElement extends ComptaSQLConfElement {

    private JTabbedPane tabbedClasse;

    // private JTable compteRep;

    public AssociationCompteAnalytiqueSQLElement() {
        super("ASSOCIATION_COMPTE_ANALYTIQUE", "une association compte analytique", "associations comptes analytiques");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_COMPTE_PCE");
        l.add("ID_REPARTITION_ANALYTIQUE");
        l.add("ID_AXE_ANALYTIQUE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_COMPTE_PCE");
        l.add("ID_REPARTITION_ANALYTIQUE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            public void addViews() {
                this.setLayout(new GridBagLayout());

                final GridBagConstraints c = new DefaultGridBagConstraints();
                c.weightx = 1;
                c.weighty = 1;
                c.fill = GridBagConstraints.BOTH;

                final List<ClasseCompte> classeComptes = new ArrayList<ClasseCompte>();
                tabbedClasse = new JTabbedPane();

                final SQLTable classeCompteTable = getTable().getBase().getTable("CLASSE_COMPTE");
                final SQLSelect selClasse = new SQLSelect();
                selClasse.addSelect(classeCompteTable.getField("ID"));
                selClasse.addSelect(classeCompteTable.getField("NOM"));
                selClasse.addSelect(classeCompteTable.getField("TYPE_NUMERO_COMPTE"));
                selClasse.addRawOrder("TYPE_NUMERO_COMPTE");

                String reqClasse = selClasse.asString();
                Object obClasse = getTable().getBase().getDataSource().execute(reqClasse, new ArrayListHandler());

                List<Object[]> myListClasse = (List<Object[]>) obClasse;
                for (Object[] objTmp : myListClasse) {
                    ClasseCompte ccTmp = new ClasseCompte(Integer.parseInt(objTmp[0].toString()), objTmp[1].toString(), objTmp[2].toString());
                    classeComptes.add(ccTmp);
                    tabbedClasse.add(ccTmp.getNom(), new JScrollPane(creerJTable(ccTmp)));
                }

                this.add(tabbedClasse, c);

                final SQLTableModifiedListener tListener = new SQLTableModifiedListener() {

                    @Override
                    public void tableModified(SQLTableEvent evt) {
                        final int tabCount = tabbedClasse.getTabCount();
                        for (int i = 0; i < tabCount; i++) {
                            tabbedClasse.setComponentAt(i, new JScrollPane(creerJTable(classeComptes.get(i))));
                        }

                    }
                };

                final SQLTable tAxeAnalytique = getElement().getDirectory().getElement(AxeAnalytiqueSQLElement.class).getTable();
                tAxeAnalytique.addTableModifiedListener(tListener);

                final SQLTable tRepartitionAnalytique = getElement().getDirectory().getElement(RepartitionAnalytiqueSQLElement.class).getTable();
                tRepartitionAnalytique.addTableModifiedListener(tListener);

                final SQLTable tRepartitionAnalytiqueElement = getElement().getDirectory().getElement(RepartitionAnalytiqueElementSQLElement.class).getTable();
                tRepartitionAnalytiqueElement.addTableModifiedListener(tListener);

                final SQLTable tComptePCE = getElement().getDirectory().getElement(ComptePCESQLElement.class).getTable();
                tComptePCE.addTableModifiedListener(tListener);

            }
        };

    }

    public JTable creerJTable(ClasseCompte ccTmp) {
        final AssociationAnalytiqueModel model = new AssociationAnalytiqueModel(ccTmp);
        final JTable table = new JTable(model);
        final Vector vect = model.getRepartitionsAxe();
        table.getColumnModel().getColumn(0).setCellRenderer(new PlanComptableCellRenderer(0));
        table.getColumnModel().getColumn(1).setCellRenderer(new PlanComptableCellRenderer(0));

        for (int i = 0; i < vect.size(); i++) {
            final Vector rep = (Vector) vect.get(i);
            JComboBox combo = new JComboBox();
            for (int j = 0; j < rep.size(); j++) {
                combo.addItem(rep.get(j));
            }
            table.getColumnModel().getColumn(i + 2).setCellEditor(new DefaultCellEditor(combo));
            table.getColumnModel().getColumn(i + 2).setCellRenderer(new PlanComptableCellRenderer(0));
        }
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".analytic.account.relation";
    }
}
