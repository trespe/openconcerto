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
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ITextArea;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

// Saisie MySQL compte 2186 Emballages recuperables
// 75 gestion type compte

public class ComptePCGSQLElement extends ComptaSQLConfElement {

    public ComptePCGSQLElement() {
        super("COMPTE_PCG", "un compte", "comptes");

    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("INFOS");
        l.add("ID_TYPE_COMPTE_PCG_BASE");
        // l.add("ID_TYPE_COMPTE_PCG_AB");
        // l.add("ID_TYPE_COMPTE_PCG_DEV");
        l.add("ID_NATURE_COMPTE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        return l;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            public void addViews() {
                this.setLayout(new GridBagLayout());

                final GridBagConstraints c = new DefaultGridBagConstraints();
                c.anchor = GridBagConstraints.NORTHEAST;
                c.gridwidth = 1;

                // Numero
                JLabel labelNumero = new JLabel("Numéro ");
                this.add(labelNumero, c);

                JTextField textNumero = new JTextField();
                c.gridx++;
                c.weightx = 1;
                this.add(textNumero, c);

                // Libellé
                JLabel labelNom = new JLabel("Libellé ");
                c.gridx++;
                c.weightx = 0;
                this.add(labelNom, c);

                JTextField textNom = new JTextField();
                c.gridx++;
                c.weightx = 1;
                this.add(textNom, c);

                // Type de compte Base
                JLabel labelTypeCompteBase = new JLabel("Type Compte Base");
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                labelTypeCompteBase.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelTypeCompteBase, c);

                ElementComboBox ComboTypeBase = new ElementComboBox();
                c.gridx++;
                c.weightx = 1;
                this.add(ComboTypeBase, c);

                // Nature du compte
                JLabel labelNature = new JLabel("Nature  du compte");
                c.gridx++;
                c.weightx = 0;
                labelNature.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelNature, c);

                ElementComboBox ComboNature = new ElementComboBox();
                c.gridx++;
                c.weightx = 1;
                this.add(ComboNature, c);

                // Type compte Abrege
                JLabel labelTypeCompteAb = new JLabel("Type Compte Abrégé");
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                labelTypeCompteAb.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelTypeCompteAb, c);

                ElementComboBox ComboTypeAb = new ElementComboBox();
                c.gridx++;
                c.weightx = 1;
                this.add(ComboTypeAb, c);

                // Type compte developpe
                JLabel labelTypeCompteDev = new JLabel("Type Compte Développé");
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                labelTypeCompteDev.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelTypeCompteDev, c);

                ElementComboBox ComboTypeDev = new ElementComboBox();
                c.gridx++;
                c.weightx = 1;
                this.add(ComboTypeDev, c);

                // Infos
                JLabel labelInfos = new JLabel(getLabelFor("INFOS"));
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelInfos, c);

                ITextArea textInfos = new ITextArea();
                c.gridx++;
                c.weightx = 1;
                c.weighty = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridheight = GridBagConstraints.REMAINDER;
                c.fill = GridBagConstraints.BOTH;
                this.add(textInfos, c);

                this.addRequiredSQLObject(textNumero, "NUMERO");
                this.addSQLObject(textNom, "NOM");
                this.addSQLObject(textInfos, "INFOS");
                this.addSQLObject(ComboTypeBase, "ID_TYPE_COMPTE_PCG_BASE");
                this.addSQLObject(ComboTypeAb, "ID_TYPE_COMPTE_PCG_AB");
                this.addSQLObject(ComboTypeDev, "ID_TYPE_COMPTE_PCG_DEV");
                this.addSQLObject(ComboNature, "ID_NATURE_COMPTE");
            }
        };
    }

    /*
     * public static final void supprimePointInterrogation() { SQLTable compteTable =
     * Configuration.getInstance().getBase().getTable("COMPTE_PCG");
     * 
     * SQLSelect selCompte = new SQLSelect(Configuration.getInstance().getBase());
     * 
     * selCompte.addSelect(compteTable.getField("ID"));
     * 
     * String req = selCompte.asString();
     * 
     * Object obj = Configuration.getInstance().getBase().getDataSource().execute(req, new
     * ArrayListHandler());
     * 
     * List myList = (List) obj; SQLRowValues vals = new SQLRowValues(compteTable);
     * 
     * for (int i = 0; i < myList.size(); i++) {
     * 
     * Object[] tmp = (Object[]) myList.get(i);
     * 
     * SQLRow row = compteTable.getRow(new Integer(tmp[0].toString()).intValue());
     * 
     * String nom = row.getString("NOM"); String infos = row.getString("INFOS");
     * 
     * vals.put("NOM", nom.trim().replace('?', '\'')); vals.put("INFOS", infos.trim().replace('?',
     * '\''));
     * 
     * try { vals.update(new Integer(tmp[0].toString()).intValue()); } catch (NumberFormatException
     * e) { e.printStackTrace(); } catch (SQLException e) { // e.printStackTrace(); } } }
     */
}
