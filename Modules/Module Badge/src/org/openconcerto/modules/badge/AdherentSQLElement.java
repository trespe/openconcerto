/*
 * Créé le 1 sept. 2011
 */
package org.openconcerto.modules.badge;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.CollectionMap;

public class AdherentSQLElement extends ComptaSQLConfElement {
    public AdherentSQLElement() {
        super("ADHERENT", "un adhérent", "adhérents");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        l.add("TEL");
        l.add("MAIL");
        l.add("ID_PLAGE_HORAIRE");
        l.add("DATE_VALIDITE_INSCRIPTION");
        l.add("NUMERO_CARTE");
        l.add("ACTIF");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("PRENOM");
        return l;
    }

    @Override
    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ADRESSE");
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        return CollectionMap.singleton(null, getComboFields());
    }

    @Override
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private JLabel getJLabelFor(String field) {
                JLabel label = new JLabel(getLabelFor(field));
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                return label;
            }

            @Override
            protected SQLRowValues createDefaults() {
                SQLRowValues rowVals = new SQLRowValues(getTable());
                rowVals.put("ACTIF", Boolean.TRUE);
                return rowVals;
            }

            @Override
            protected void addViews() {
                GridBagConstraints c = new DefaultGridBagConstraints();
                this.setLayout(new GridBagLayout());

                c.weightx = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(new TitledSeparator("Informations personnelles", true), c);

                c.gridy++;
                c.weightx = 0;
                c.gridwidth = 1;
                JTextField nom = new JTextField();
                this.add(getJLabelFor("NOM"), c);
                c.gridx++;
                c.weightx = 1;
                this.add(nom, c);
                this.addView(nom, "NOM", REQ);

                c.gridx++;
                c.weightx = 0;
                JTextField prenom = new JTextField();
                this.add(getJLabelFor("PRENOM"), c);
                c.gridx++;
                c.weightx = 1;
                this.add(prenom, c);
                this.addView(prenom, "PRENOM", REQ);

                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                JDate dateNaissance = new JDate();
                this.add(getJLabelFor("DATE_NAISSANCE"), c);
                c.gridx++;
                this.add(dateNaissance, c);
                this.addView(dateNaissance, "DATE_NAISSANCE");

                c.gridy++;
                c.gridx = 0;
                JTextField tel = new JTextField();
                c.weightx = 0;
                this.add(getJLabelFor("TEL"), c);
                c.gridx++;
                c.weightx = 1;
                this.add(tel, c);
                this.addView(tel, "TEL");

                c.gridx++;
                c.weightx = 0;
                JTextField mail = new JTextField();
                this.add(getJLabelFor("MAIL"), c);
                c.gridx++;
                c.weightx = 1;
                this.add(mail, c);
                this.addView(mail, "MAIL");

                c.gridy++;
                c.gridx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                this.addView("ID_ADRESSE", REQ + ";" + DEC + ";" + SEP);
                final ElementSQLObject view = (ElementSQLObject) this.getView("ID_ADRESSE");

                this.add(view, c);

                c.gridy++;
                c.gridx = 0;
                c.weightx = 1;
                c.insets = new Insets(15, 3, 2, 2);
                this.add(new TitledSeparator("Gestion des entrées", true), c);

                JDate dateInscr = new JDate();
                c.gridwidth = 1;
                c.gridy++;
                c.weightx = 0;
                c.insets = DefaultGridBagConstraints.getDefaultInsets();
                this.add(getJLabelFor("DATE_VALIDITE_INSCRIPTION"), c);

                c.gridx++;
                this.add(dateInscr, c);
                this.addView(dateInscr, "DATE_VALIDITE_INSCRIPTION");

                c.gridwidth = 1;
                c.gridx++;
                c.weightx = 0;
                this.add(getJLabelFor("NUMERO_CARTE"), c);

                c.gridx++;
                JTextField fieldCarte = new JTextField();
                this.add(fieldCarte, c);
                this.addView(fieldCarte, "NUMERO_CARTE");

                ElementComboBox boxPlage = new ElementComboBox();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                c.gridwidth = 1;
                this.add(getJLabelFor("ID_PLAGE_HORAIRE"), c);
                c.gridx++;
                // c.weightx = 1;
                this.add(boxPlage, c);
                this.addView(boxPlage, "ID_PLAGE_HORAIRE");

                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                JCheckBox boxActif = new JCheckBox(getLabelFor("ACTIF"));
                this.add(boxActif, c);
                this.addView(boxActif, "ACTIF");
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                JCheckBox boxAdmin = new JCheckBox(getLabelFor("ADMIN"));
                this.add(boxAdmin, c);
                this.addView(boxAdmin, "ADMIN");

                c.gridy++;
                c.gridx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.insets = new Insets(15, 3, 2, 2);
                this.add(new TitledSeparator("Informations complémentaires", true), c);

                c.gridy++;
                c.weighty = 1;
                c.fill = GridBagConstraints.BOTH;
                c.insets = DefaultGridBagConstraints.getDefaultInsets();
                ITextArea infos = new ITextArea();
                this.add(infos, c);
                this.addView(infos, "INFOS");
            }
        };
    }
}
