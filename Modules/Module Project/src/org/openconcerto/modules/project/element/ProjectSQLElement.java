package org.openconcerto.modules.project.element;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;

public class ProjectSQLElement extends ComptaSQLConfElement {

    public ProjectSQLElement() {
        super("AFFAIRE", "une affaire", "affaires");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_CLIENT");
        l.add("ID_TYPE_AFFAIRE");
        l.add("NUMERO");
        l.add("ID_ETAT_AFFAIRE");
        l.add("ID_COMMERCIAL");
        l.add("INFOS");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("ID_CLIENT");
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        return CollectionMap.singleton(null, getComboFields());
    }

    @Override
    public Set<String> getInsertOnlyFields() {
        Set<String> s = new HashSet<String>(1);
        s.add("ID_DEVIS");
        return s;
    }

    @Override
    public SQLComponent createComponent() {
        return new UISQLComponent(this, 2) {

            final JUniqueTextField field = new JUniqueTextField();

            @Override
            protected void addViews() {

                this.addView(this.field, "NUMERO", "1");
                this.addView(new JDate(true), "DATE", "1");
                this.addView("ID_CLIENT", "1;" + REQ);
                this.addView("ID_COMMERCIAL", "1");
                this.addView("ID_DEVIS", "1");

                this.addView("ID_TYPE_AFFAIRE", "1;left");
                final ElementComboBox boxEtatAffaire = new ElementComboBox();
                this.addView(boxEtatAffaire, "ID_ETAT_AFFAIRE", "1;left;" + REQ);
                this.addView(new ITextArea(), "INFOS", "2");

            }

            @Override
            public void select(SQLRowAccessor r) {
                super.select(r);
                if (r != null) {
                    this.field.setIdSelected(r.getID());
                }
            }

            @Override
            public void update() {
                if (!this.field.checkValidation()) {
                    ExceptionHandler.handle("Impossible d'ajouter, numéro d'affaire existant.");
                    Object root = SwingUtilities.getRoot(this);
                    if (root instanceof EditFrame) {
                        EditFrame frame = (EditFrame) root;
                        frame.getPanel().setAlwaysVisible(true);
                    }
                    return;
                }
                super.update();
            }

            @Override
            public int insert(SQLRow order) {

                int idCommande = getSelectedID();

                // on verifie qu'un devis du meme numero n'a pas été inséré entre temps
                if (this.field.checkValidation()) {

                    idCommande = super.insert(order);
                    // incrémentation du numéro auto
                    if (NumerotationAutoSQLElement.getNextNumero(ProjectSQLElement.class).equalsIgnoreCase(this.field.getText().trim())) {
                        SQLTable tableNum = Configuration.getInstance().getRoot().findTable("NUMEROTATION_AUTO");
                        SQLRowValues rowVals = new SQLRowValues(tableNum);
                        int val = tableNum.getRow(2).getInt("AFFAIRE_START");
                        val++;
                        rowVals.put("AFFAIRE_START", new Integer(val));

                        try {
                            rowVals.update(2);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                } else {
                    ExceptionHandler.handle("Impossible d'ajouter, numéro d'affaire existant.");
                    Object root = SwingUtilities.getRoot(this);
                    if (root instanceof EditFrame) {
                        EditFrame frame = (EditFrame) root;
                        frame.getPanel().setAlwaysVisible(true);
                    }
                }
                SQLRow row = getTable().getRow(idCommande);
                SQLRow rowDevis = row.getForeign("ID_DEVIS");
                if (rowDevis != null && !rowDevis.isUndefined()) {
                    SQLRowValues rowVals = rowDevis.asRowValues();
                    rowVals.put("ID_AFFAIRE", idCommande);
                    try {
                        rowVals.update();
                    } catch (SQLException exn) {
                        // TODO Bloc catch auto-généré
                        exn.printStackTrace();
                    }
                }
                return idCommande;
            }

            @Override
            protected SQLRowValues createDefaults() {
                SQLRowValues rowVals = new SQLRowValues(getTable());
                rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(ProjectSQLElement.class));
                rowVals.put("ID_ETAT_AFFAIRE", ProjectStateSQLElement.EN_COURS);
                return rowVals;
            }
        };

    }
}
