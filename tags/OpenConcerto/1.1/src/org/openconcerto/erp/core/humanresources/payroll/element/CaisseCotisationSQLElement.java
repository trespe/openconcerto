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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.TitledSeparator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CaisseCotisationSQLElement extends ConfSQLElement {

    public CaisseCotisationSQLElement() {
        super("CAISSE_COTISATION", "une caisse", "caisses");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("ID_ADRESSE_COMMON");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ADRESSE_COMMON");
        return l;
    }

    private final static SQLTable tableCaisse = Configuration.getInstance().getBase().getTable("CAISSE_COTISATION");

    private static List<SQLRow> liste;

    public static List<SQLRow> getCaisseCotisation() {
        if (liste == null) {
            SQLSelect selCaisse = new SQLSelect(Configuration.getInstance().getBase());
            selCaisse.addSelect(tableCaisse.getField("ID"));
            selCaisse.addSelect(tableCaisse.getField("NOM"));

            String req = selCaisse.asString();
            liste = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(req, SQLRowListRSH.createFromSelect(selCaisse));
        }
        return liste;

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

                /***********************************************************************************
                 * Renseignements
                 **********************************************************************************/
                JPanel panelInfos = new JPanel();
                panelInfos.setBorder(BorderFactory.createTitledBorder("Renseignements"));
                panelInfos.setLayout(new GridBagLayout());

                // Nom
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                JTextField textNom = new JTextField();
                panelInfos.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textNom, c);
                c.weightx = 0;

                // Adresse
                TitledSeparator sep = new TitledSeparator("Adresse");
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridy++;
                c.gridx = 0;
                panelInfos.add(sep, c);

                this.addView("ID_ADRESSE", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltAdr = (ElementSQLObject) this.getView("ID_ADRESSE_COMMON");
                c.gridy++;
                c.gridx = 0;

                panelInfos.add(eltAdr, c);

                TitledSeparator sepContact = new TitledSeparator("Contact");
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridy++;
                c.gridx = 0;
                panelInfos.add(sepContact, c);
                // Telephone
                JLabel labelTel = new JLabel(getLabelFor("TEL"));
                JTextField textTel = new JTextField();

                c.gridy++;
                c.gridwidth = 1;
                c.gridx = 0;
                panelInfos.add(labelTel, c);

                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textTel, c);
                c.weightx = 0;

                // Telephone portable
                JLabel labelTelP = new JLabel(getLabelFor("TEL_PORTABLE"));
                JTextField textTelP = new JTextField();

                c.gridx++;
                panelInfos.add(labelTelP, c);

                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textTelP, c);
                c.weightx = 0;

                // Mail
                JLabel labelMail = new JLabel(getLabelFor("MAIL"));
                JTextField textMail = new JTextField();

                c.gridy++;
                c.gridx = 0;
                panelInfos.add(labelMail, c);

                c.gridx++;
                c.weightx = 1;
                panelInfos.add(textMail, c);
                c.weightx = 0;

                c.gridx = 0;
                c.gridy = 0;
                c.fill = GridBagConstraints.BOTH;
                this.add(panelInfos, c);
                c.fill = GridBagConstraints.HORIZONTAL;

                /***********************************************************************************
                 * Comptabilité
                 **********************************************************************************/
                /*
                 * JPanel panelCompta = new JPanel();
                 * panelCompta.setBorder(BorderFactory.createTitledBorder("Comptabilité"));
                 * panelCompta.setLayout(new GridBagLayout()); // Compte tiers JLabel labelTiers =
                 * new JLabel(getLabelFor("ID_COMPTE_PCE_TIERS")); ISQLCompteSelector selCompteTiers
                 * = new ISQLCompteSelector("1"); panelCompta.add(labelTiers, c); c.gridx++;
                 * c.weightx = 1; panelCompta.add(selCompteTiers, c); c.weightx = 0; // Compte
                 * charge JLabel labelCharge = new JLabel(getLabelFor("ID_COMPTE_PCE_CHARGE"));
                 * ISQLCompteSelector selCompteCharge = new ISQLCompteSelector("1"); c.gridy++;
                 * c.gridx = 0; panelCompta.add(labelCharge, c); c.gridx++; c.weightx = 1;
                 * panelCompta.add(selCompteCharge, c); c.weightx = 0;
                 * 
                 * c.gridx = 0; c.gridy = 1; c.fill = GridBagConstraints.BOTH; this.add(panelCompta,
                 * c);
                 */

                this.addSQLObject(textNom, "NOM");
                this.addSQLObject(textTel, "TEL");
                this.addSQLObject(textTelP, "TEL_PORTABLE");
                // this.addSQLObject(selCompteTiers, "ID_COMPTE_PCE_TIERS");
                // this.addSQLObject(selCompteCharge, "ID_COMPTE_PCE_CHARGE");
                // selCompteTiers.init();
                // selCompteCharge.init();
            }
        };
    }
}
