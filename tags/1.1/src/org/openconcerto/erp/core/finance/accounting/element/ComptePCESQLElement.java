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
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ComptePCESQLElement extends ComptaSQLConfElement {

    public ComptePCESQLElement() {
        super("COMPTE_PCE", "un compte", "comptes");
    }

    protected List<String> getListFields() {
        final List<String> list = new ArrayList<String>(3);
        list.add("NUMERO");
        list.add("NOM");
        list.add("INFOS");
        return list;
    }

    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("NUMERO");
        list.add("NOM");
        return list;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private boolean isCompteValid = false;
            private JUniqueTextField textNumero = new JUniqueTextField(7);

            @Override
            public void update() {
                // TODO Raccord de méthode auto-généré
                int id = getSelectedID();
                super.update();
                SQLElement eltEcr = Configuration.getInstance().getDirectory().getElement("ECRITURE");
                Configuration
                        .getInstance()
                        .getBase()
                        .getDataSource()
                        .execute(
                                "UPDATE " + eltEcr.getTable().getSQLName().quote() + " SET \"COMPTE_NUMERO\"=c.\"NUMERO\",\"COMPTE_NOM\"=c.\"NOM\" FROM " + getTable().getSQLName().quote()
                                        + " c WHERE c.\"ID\"=\"ID_COMPTE_PCE\" AND c.\"ID\"=" + id);
            }

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Numero
                JLabel labelNumero = new JLabel("Numéro ");

                this.add(labelNumero, c);

                c.gridx++;
                c.weightx = 0;
                c.weightx = 0.5;
                this.add(this.textNumero, c);
                this.textNumero.getTextField().getDocument().addDocumentListener(new SimpleDocumentListener() {

                    @Override
                    public void update(DocumentEvent e) {

                        // On verifie que le numero est correct compris qu'il commence par 1-8
                        if (textNumero.getText().trim().length() > 0) {
                            if ((textNumero.getText().trim().charAt(0) < '1') || (textNumero.getText().trim().charAt(0) > '8')) {
                                System.err.println("Numero de compte incorrect");
                                isCompteValid = false;
                            } else {
                                isCompteValid = true;
                            }

                            // else {
                            //
                            // // on verifie que le numero n'existe pas deja
                            // SQLSelect selCompte = new SQLSelect(getTable().getBase());
                            // selCompte.addSelect(getTable().getField("NUMERO"));
                            //
                            // selCompte.setWhere("COMPTE_PCE.NUMERO", "=",
                            // textNumero.getText().trim());
                            //
                            // String reqCompte = selCompte.asString();
                            // Object obRep =
                            // getTable().getBase().getDataSource().execute(reqCompte, new
                            // ArrayListHandler());
                            //
                            // List tmpCpt = (List) obRep;
                            //
                            // System.err.println("REQUEST :: " + reqCompte);
                            // System.err.println("nb Same :: " + tmpCpt.size());
                            //
                            // isCompteValid = tmpCpt.size() == 0;
                            // }
                        } else {
                            isCompteValid = false;
                        }
                        fireValidChange();
                        System.err.println("Compte Valid " + isCompteValid);
                    }
                });

                /*
                 * textNumero.addKeyListener(new KeyAdapter(){
                 * 
                 * public void keyPressed(KeyEvent e) { } });
                 */

                // Libellé
                JLabel labelNom = new JLabel("Libellé ");
                c.gridx++;
                c.weightx = 0;
                this.add(labelNom, c);

                JTextField textNom = new JTextField(30);
                c.gridx++;
                c.weightx = 1;
                this.add(textNom, c);

                // c.gridy++;
                c.gridx++;
                c.weightx = 0;
                JCheckBox checkRacine = new JCheckBox("Racine");
                this.add(checkRacine, c);

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

                this.addRequiredSQLObject(this.textNumero, "NUMERO");
                this.addRequiredSQLObject(textNom, "NOM");
                this.addSQLObject(textInfos, "INFOS");
                this.addSQLObject(checkRacine, "RACINE");
            }

            @Override
            public void select(SQLRowAccessor r) {
                super.select(r);
                if (r != null) {
                    this.textNumero.setIdSelected(r.getID());
                }
            }

            @Override
            public synchronized ValidState getValidState() {
                return super.getValidState().and(ValidState.createCached(this.isCompteValid, "Le numéro de compte ne commence pas par un chiffre entre 1 et 8"));
            }
        };
    }

    @Override
    protected void archive(SQLRow row, boolean cutLinks) throws SQLException {
        // on verifie qu'aucune ecriture n'est asssociée à ce compte
        SQLBase base = getTable().getBase();
        SQLTable ecritureTable = base.getTable("ECRITURE");
        SQLSelect selEcr = new SQLSelect(base);
        selEcr.addSelect(ecritureTable.getField("ID_COMPTE_PCE"));
        selEcr.setWhere(new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", row.getID()));

        String reqEcriture = selEcr.asString();

        Object obEcriture = base.getDataSource().execute(reqEcriture, new ArrayListHandler());

        List myListEcriture = (List) obEcriture;

        if (myListEcriture.size() != 0) {

            System.err.println("Impossible de supprimer un compte mouvementé!");
            ExceptionHandler.handle("", new Exception("Impossible de supprimer un compte mouvementé!"));
        } else {
            super.archive(row, cutLinks);
        }
    }

    public static int getId(String numero) {
        return getId(numero, "Création automatique");
    }

    /**
     * retourne l'id d'un compte en fonction de son numero, si le compte n'existe pas il sera créé
     * automatiquement
     * 
     * @param numero du compte
     * @param nom nom du compte
     * @return id du compte
     */
    public static int getId(String numero, String nom) {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLSelect selCompte = new SQLSelect(base);
        selCompte.addSelect(compteTable.getField("ID"));
        selCompte.setWhere(new Where(compteTable.getField("NUMERO"), "=", numero.trim()));

        String reqCompte = selCompte.asString();

        Object obCompte = base.getDataSource().execute(reqCompte, new ArrayListHandler());

        List myListCompte = (List) obCompte;

        if (myListCompte.size() != 0) {
            return Integer.parseInt(((Object[]) myListCompte.get(0))[0].toString());
        } else {

            SQLRowValues rowVals = new SQLRowValues(compteTable);
            rowVals.put("NUMERO", numero);
            rowVals.put("NOM", nom);
            try {
                SQLRow row = rowVals.insert();
                return row.getID();
            } catch (SQLException e) {

                e.printStackTrace();
            }
            return rowVals.getID();
        }
    }

    public static boolean isExist(String numero) {

        if (numero.trim().length() == 0) {
            return false;
        }

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLSelect selCompte = new SQLSelect(base);
        selCompte.addSelect(compteTable.getField("ID"));
        selCompte.setWhere(new Where(compteTable.getField("NUMERO"), "=", numero.trim()));

        String reqCompte = selCompte.asString();

        Object obCompte = base.getDataSource().execute(reqCompte, new ArrayListHandler());

        List myListCompte = (List) obCompte;

        return (myListCompte.size() != 0);

    }

    /**
     * @param name
     * @return Retourne le numero par défaut d'un compte
     * @throws IllegalArgumentException
     */
    public static String getComptePceDefault(final String name) throws IllegalArgumentException {
        final SQLBase base = Configuration.getInstance().getBase();
        final SQLTable tableDefault = base.getTable("COMPTE_PCE_DEFAULT");
        final SQLSelect sel = new SQLSelect(base);
        sel.addSelect(tableDefault.getField("NUMERO_DEFAULT"));

        sel.setWhere(Where.quote("UPPER(%n) = %s", tableDefault.getField("NOM"), name.toUpperCase()));

        String numero = (String) base.getDataSource().executeScalar(sel.asString());
        if (numero == null) {
            throw new IllegalArgumentException("Impossible de trouver le compte PCE par défaut " + name);
        } else {
            return numero;
        }
    }

    public static int getIdComptePceDefault(final String name) throws Exception {
        final String numeroDefault = getComptePceDefault(name);
        return getId(numeroDefault);
    }
}
