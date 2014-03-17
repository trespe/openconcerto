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
 
 package org.openconcerto.erp.core.sales.invoice.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.EmailComposer;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.GestionDevise;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class EcheanceClientSQLElement extends ComptaSQLConfElement {

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);

    public EcheanceClientSQLElement() {
        super("ECHEANCE_CLIENT", "une échéance client", "échéances clients");
        {
            PredicateRowAction action = new PredicateRowAction(new AbstractAction("Voir la source") {

                @Override
                public void actionPerformed(ActionEvent arg0) {

                    SQLRow row = IListe.get(arg0).fetchSelectedRow();
                    MouvementSQLElement.showSource(row.getInt("ID_MOUVEMENT"));
                }
            }, false);
            action.setPredicate(IListeEvent.getSingleSelectionPredicate());
            getRowActions().add(action);
        }

        {
            PredicateRowAction action = new PredicateRowAction(new AbstractAction("Envoyer un mail") {

                @Override
                public void actionPerformed(ActionEvent arg0) {

                    SQLRow row = IListe.get(arg0).fetchSelectedRow();
                    sendMail(row);
                }
            }, false);
            action.setPredicate(IListeEvent.getSingleSelectionPredicate());
            getRowActions().add(action);
        }

        if (UserRightsManager.getCurrentUserRights().haveRight(ComptaUserRight.MENU)) {
            RowAction actionCancel = new RowAction(new AbstractAction("Annuler la régularisation en comptabilité") {

                public void actionPerformed(ActionEvent e) {

                    int answer = JOptionPane.showConfirmDialog(null, "Etes vous sûr de vouloir annuler la régularisation ?");
                    if (answer == JOptionPane.YES_OPTION) {
                        SQLRow row = IListe.get(e).getSelectedRow().asRow();
                        SQLRowValues rowVals = row.createEmptyUpdateRow();
                        rowVals.put("REG_COMPTA", Boolean.FALSE);
                        try {
                            rowVals.commit();
                        } catch (SQLException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                }
            }, false) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    if (selection != null && selection.size() == 1) {
                        SQLRowAccessor row = selection.get(0);
                        return row.getBoolean("REG_COMPTA");
                    } else {
                        return true;
                    }
                }
            };
            getRowActions().add(actionCancel);

            RowAction actionRegul = new RowAction(new AbstractAction("Régularisation en comptabilité") {

                public void actionPerformed(ActionEvent e) {

                    SQLRow row = IListe.get(e).fetchSelectedRow();
                    String price = GestionDevise.currencyToString(row.getLong("MONTANT"));
                    SQLRow rowClient = row.getForeignRow("ID_CLIENT");
                    String nomClient = rowClient.getString("NOM");
                    String piece = "";
                    SQLRow rowMvt = row.getForeignRow("ID_MOUVEMENT");
                    if (rowMvt != null) {
                        SQLRow rowPiece = rowMvt.getForeignRow("ID_PIECE");
                        piece = rowPiece.getString("NOM");
                    }
                    int answer = JOptionPane.showConfirmDialog(null, "Etes vous sûr de vouloir régulariser l'échéance de " + nomClient + " d'un montant de " + price
                            + "€ avec une saisie au kilometre?\nNom de la piéce : " + piece + ".");
                    if (answer == JOptionPane.YES_OPTION) {

                        SQLRowValues rowVals = row.createEmptyUpdateRow();
                        rowVals.put("REG_COMPTA", Boolean.TRUE);
                        try {
                            rowVals.commit();
                        } catch (SQLException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                }
            }, false) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    if (selection != null && selection.size() == 1) {
                        SQLRowAccessor row = selection.get(0);
                        return !row.getBoolean("REG_COMPTA");
                    } else {
                        return true;
                    }
                }
            };
            getRowActions().add(actionRegul);
        }

    }


    private void sendMail(SQLRow row) {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

        if (row != null) {
            int idMvtSource = MouvementSQLElement.getSourceId(row.getInt("ID_MOUVEMENT"));
            SQLRow rowMvtSource = base.getTable("MOUVEMENT").getRow(idMvtSource);

            if (!rowMvtSource.getString("SOURCE").equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
                // this.relancer.setEnabled(false);
                return;
            }
            int idFact = rowMvtSource.getInt("IDSOURCE");
            SQLRow rowFacture = base.getTable("SAISIE_VENTE_FACTURE").getRow(idFact);

            Set<SQLField> setContact = null;
            SQLTable tableContact = Configuration.getInstance().getRoot().findTable("CONTACT");
            setContact = row.getTable().getForeignKeys(tableContact);

            Set<SQLField> setClient = null;
            SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("CLIENT");
            setClient = row.getTable().getForeignKeys(tableClient);

            // Infos facture
            Long lTotal = (Long) rowFacture.getObject("T_TTC");
            Long lRestant = (Long) row.getObject("MONTANT");
            long lRestantDevise = lRestant.longValue();
            final String devise;
            SQLRow rowTarif = rowFacture.getForeign("ID_TARIF");
            if (rowTarif != null && !rowTarif.isUndefined()) {
                SQLRow rowDevise = rowTarif.getForeign("ID_DEVISE");
                BigDecimal t = (BigDecimal) rowDevise.getObject("TAUX");
                BigDecimal bigDecimal = new BigDecimal(lRestantDevise);
                lRestantDevise = t.signum() == 0 ? lRestantDevise : bigDecimal.multiply(t, MathContext.DECIMAL128).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                if (rowDevise.getString("CODE").trim().length() > 0) {
                    devise = rowDevise.getString("CODE");
                } else {
                    devise = "€";
                }
            } else {
                devise = "€";
            }

            Long lVerse = new Long(lTotal.longValue() - lRestant.longValue());
            // m.put("FactureNumero", rowFacture.getString("NUMERO"));
            // m.put("FactureTotal", GestionDevise.currencyToString(lTotal.longValue(), true));
            // m.put("FactureRestant", GestionDevise.currencyToString(lRestant.longValue(), true));
            // m.put("FactureVerse", GestionDevise.currencyToString(lVerse.longValue(), true));
            // m.put("FactureDate", dateFormat2.format((Date) rowFacture.getObject("DATE")));
            Date dFacture = (Date) rowFacture.getObject("DATE");
            SQLRow modeRegRow = rowFacture.getForeignRow("ID_MODE_REGLEMENT");
            Date dateEch = ModeDeReglementSQLElement.calculDate(modeRegRow.getInt("AJOURS"), modeRegRow.getInt("LENJOUR"), dFacture);

            final String references = rowFacture.getString("NOM");
            final String text = "Date: "
                    + dateFormat.format(new Date())
                    + "\n"
                    + "Concerning : Late Payment reminder\n"
                    + "\n\n\nTo "
                    + rowFacture.getForeign("ID_CLIENT").getString("NOM")
                    + ","
                    +

                    "\n\n\nIt has come to our attention that the following invoice has not been paid to this day."
                    +

                    "\nInvoice # "
                    + rowFacture.getString("NUMERO")
                    + " from "
                    + dateFormat.format(rowFacture.getDate("DATE").getTime())
                    + " of "
                    + devise
                    + " "
                    + GestionDevise.currencyToString(lRestantDevise, true)
                    + " duedate "
                    + dateFormat.format(dateEch)
                    + (references.trim().length() == 0 ? "" : (". Your references : " + references))
                    + ".\nWe assume that this is a mere oversight and we would appreciate it if you would settle this invoice as soon as possible. In the event that this has already been accomplished in the meantime, please ignore this notice."
                    +

                    "\n\n\nThanking you in advance.";
            String mail = "";
            for (SQLField field : setContact) {
                if (mail == null || mail.trim().length() == 0) {
                    mail = row.getForeignRow(field.getName()).getString("EMAIL");
                }
            }

            for (SQLField field : setClient) {
                SQLRow rowCli = row.getForeignRow(field.getName());
                if (mail == null || mail.trim().length() == 0) {
                    mail = rowCli.getString("MAIL");
                }
            }

            final String adresseMail = mail;

            final Thread t = new Thread() {
                @Override
                public void run() {

                    try {
                        EmailComposer.getInstance().compose(adresseMail, "Late Payment reminder - " + references, text);
                    } catch (IOException exn) {
                        // TODO Bloc catch auto-généré
                        exn.printStackTrace();
                    } catch (InterruptedException exn) {
                        // TODO Bloc catch auto-généré
                        exn.printStackTrace();
                    }

                }
            };

            t.start();
        }
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("ID_MOUVEMENT");
        l.add("DATE");
        l.add("MONTANT");
        l.add("ID_CLIENT");
        l.add("ID_SAISIE_VENTE_FACTURE");
            l.add("NOMBRE_RELANCE");
        l.add("INFOS");
        l.add("DATE_LAST_RELANCE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("ID_CLIENT");
        l.add("MONTANT");
        return l;
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(this.getTable(), this.getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("REG_COMPTA", null);
                graphToFetch.put("REGLE", null);
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private DeviseField montant;
            private JTextField nbRelance;
            private JDate date;
            private JTextField idMouvement;
            private ElementComboBox comboClient;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                this.montant = new DeviseField();
                this.nbRelance = new JTextField();
                this.date = new JDate();
                this.idMouvement = new JTextField();
                this.comboClient = new ElementComboBox();

                // Mouvement
                JLabel labelMouvement = new JLabel("Mouvement");
                this.add(labelMouvement, c);

                c.weightx = 1;
                c.gridx++;
                this.add(this.idMouvement, c);

                // Date
                JLabel labelDate = new JLabel("Date");
                c.gridx++;
                this.add(labelDate, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.date, c);

                // Client
                JLabel labelClient = new JLabel("Client");
                c.gridy++;
                c.gridx = 0;

                this.add(labelClient, c);

                c.gridx++;
                c.weightx = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.comboClient, c);

                // libellé
                JLabel labelRelance = new JLabel("Nombre de relance");
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                this.add(labelRelance, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.nbRelance, c);

                // montant
                c.gridwidth = 1;
                JLabel labelMontant = new JLabel("Montant");
                c.gridx++;
                this.add(labelMontant, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.montant, c);

                this.addSQLObject(this.montant, "MONTANT");
                this.addRequiredSQLObject(this.date, "DATE");
                this.addSQLObject(this.nbRelance, "NOMBRE_RELANCE");
                this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");
                this.addSQLObject(this.idMouvement, "ID_MOUVEMENT");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".commitment";
    }

}
