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
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLBackgroundTableCacheItem;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;
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
import java.util.regex.Pattern;

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

            // private boolean isCompteValid = false;
            private ValidState compteNumeroValidState = ValidState.getTrueInstance();
            private JUniqueTextField textNumero = new JUniqueTextField(7);

            @Override
            public void update() {
                final int id = getSelectedID();
                super.update();
                final DBSystemRoot sysRoot = getTable().getDBSystemRoot();
                final SQLTable ecrT = sysRoot.getGraph().findReferentTable(getTable(), "ECRITURE");
                final UpdateBuilder updateBuilder = new UpdateBuilder(ecrT);
                updateBuilder.addTable(getTable());
                updateBuilder.set("COMPTE_NUMERO", getTable().getField("NUMERO").getFieldRef());
                updateBuilder.set("COMPTE_NOM", getTable().getField("NOM").getFieldRef());
                updateBuilder.setWhere(new Where(getTable().getKey(), "=", ecrT.getField("ID_COMPTE_PCE")).and(new Where(getTable().getKey(), "=", id)));
                sysRoot.getDataSource().execute(updateBuilder.asString());
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

                        // On verifie que le numero est correct
                        compteNumeroValidState = getCompteNumeroValidState(textNumero.getText());

                        fireValidChange();
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
                return super.getValidState().and(this.compteNumeroValidState);
            }
        };
    }

    public ValidState getCompteNumeroValidState(String text) {

        if (text.trim().length() > 0) {
            if ((text.trim().charAt(0) < '1') || (text.trim().charAt(0) > '8')) {
                // System.err.println("Numero de compte incorrect");

                return ValidState.create(false, "Le numéro de compte ne commence pas par un chiffre entre 1 et 8");
            } else if (text.endsWith(" ")) {
                return ValidState.create(false, "Le numéro de compte ne doit pas se terminer par un espace");
            } else {
                Pattern p = Pattern.compile("^\\d(\\w)+$");
                return ValidState.create(p.matcher(text).matches(), "Le numéro de compte n'est pas correct.");
            }

        }
        return ValidState.getTrueInstance();

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

    public static SQLRow getRow(String numero, String nom) {
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLSelect selCompte = new SQLSelect();
        selCompte.addSelectStar(compteTable);
        selCompte.setWhere(new Where(compteTable.getField("NUMERO"), "=", numero.trim()));

        // String reqCompte = selCompte.asString();
        //
        // Object obCompte = base.getDataSource().execute(reqCompte, new ArrayListHandler());
        //
        // List myListCompte = (List) obCompte;

        List<SQLRow> myListCompte = SQLRowListRSH.execute(selCompte);
        if (myListCompte.size() != 0) {
            return myListCompte.get(0);
        } else {

            SQLRowValues rowVals = new SQLRowValues(compteTable);
            rowVals.put("NUMERO", numero);
            rowVals.put("NOM", nom);
            try {
                return rowVals.insert();
            } catch (SQLException e) {
                ExceptionHandler.handle("Erreur lors de la création du compte numéro : " + numero, e);
                return null;
            }

        }
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
        return getRow(numero, nom).getID();
    }

    public static boolean isExist(String account) {

        if (account.trim().length() == 0) {
            return false;
        }

        final SQLTable tableAccount = Configuration.getInstance().getDirectory().getElement("COMPTE_PCE").getTable();
        final SQLBackgroundTableCacheItem item = SQLBackgroundTableCache.getInstance().getCacheForTable(tableAccount);
        return (item.getFirstRowContains(account, tableAccount.getField("NUMERO")) != null);

    }

    /**
     * @param name
     * @return Retourne le numero par défaut d'un compte
     * @throws IllegalArgumentException
     */
    public static String getComptePceDefault(final String name) throws IllegalArgumentException {
        final SQLBase base = Configuration.getInstance().getBase();
        final SQLTable tableDefault = base.getTable("COMPTE_PCE_DEFAULT");
        final SQLSelect sel = new SQLSelect();
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

    public static SQLRow getRowComptePceDefault(final String name) throws Exception {
        final String numeroDefault = getComptePceDefault(name);
        return getRow(numeroDefault, "création automatique");
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".code.enterprise";
    }
}
