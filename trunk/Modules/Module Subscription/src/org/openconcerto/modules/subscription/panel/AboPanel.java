/*
 * Créé le 1 juin 2012
 */
package org.openconcerto.modules.subscription.panel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JPanel;

import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.panel.ListeFastPrintFrame;
import org.openconcerto.modules.subscription.SubscriptionChecker;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.SwingWorker2;

/**
 * Base pour les panels de gestion des renouvellements d'abonnements
 * 
 * @author Utilisateur
 */
public class AboPanel extends JPanel {

    // Bouton de vérification des abonnements
    protected final PredicateRowAction actionCheck;

    // Action de validation des éléments générés dans les IListes
    protected final PredicateRowAction actionValid;

    // Action pour voir l'abonnement associé
    protected final PredicateRowAction actionGetAbo;

    public AboPanel(final SQLElement elt, final SQLElement itemsElement, final String type) {
        super(new GridBagLayout());

        this.actionCheck = new PredicateRowAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // actionCheck.setEnabled(false);

                Configuration.getInstance().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    public void run() {
                        try {
                            parseAbonnement(elt, itemsElement, type);
                            // createDevis();
                        } catch (Exception exn) {
                            ExceptionHandler.handle("Une erreur est survenue pendant la vérification des abonnements", exn);
                        }
                    }
                });
            }
        }, true, false, "subscription.check");
        this.actionCheck.setPredicate(IListeEvent.createSelectionCountPredicate(0, Integer.MAX_VALUE));
        this.actionValid = new PredicateRowAction(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                List<SQLRowAccessor> list = IListe.get(arg0).getSelectedRows();

                for (SQLRowAccessor sqlRowAccessor : list) {
                    validItem(sqlRowAccessor);
                }

                ListeFastPrintFrame frame = new ListeFastPrintFrame(list, DevisXmlSheet.class);
                frame.setVisible(true);
            }
        }, true, "subscription.validate");
        this.actionValid.setPredicate(IListeEvent.getNonEmptySelectionPredicate());

        this.actionGetAbo = new PredicateRowAction(new AbstractAction() {
            EditFrame frame;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final SQLRowAccessor row = IListe.get(arg0).getSelectedRow();
                if (this.frame == null) {
                    this.frame = new EditFrame(Configuration.getInstance().getDirectory().getElement("ABONNEMENT"), EditMode.MODIFICATION);
                }
                this.frame.selectionId(row.getInt("ID_ABONNEMENT"));
                this.frame.setVisible(true);
            }
        }, false, "subscription.modify");
        this.actionGetAbo.setPredicate(IListeEvent.getSingleSelectionPredicate());

        createUI(elt, itemsElement, type);
    }

    protected List<RowAction> getAdditionnalRowActions() {
        return Collections.emptyList();
    }

    /**
     * Interface
     * 
     * @param elt
     * @param itemsElement
     * @param type
     */
    private void createUI(final SQLElement elt, final SQLElement itemsElement, final String type) {
        final SwingWorker2<SQLTableModelSourceOnline, Object> worker = new SwingWorker2<SQLTableModelSourceOnline, Object>() {

            @Override
            protected SQLTableModelSourceOnline doInBackground() throws Exception {
                final SQLTableModelSourceOnline tableCmd = elt.getTableSource(true);
                Where wD = new Where(elt.getTable().getField("CREATION_AUTO_VALIDER"), "=", Boolean.FALSE);
                wD = wD.and(new Where(elt.getTable().getField("ID_ABONNEMENT"), "IS NOT", (Object) null));
                tableCmd.getReq().setWhere(wD);
                return tableCmd;
            }

            @Override
            protected void done() {
                try {
                    final IListe listCmd = new IListe(get());
                    listCmd.addIListeAction(actionValid);
                    listCmd.addIListeAction(actionGetAbo);
                    listCmd.addIListeAction(actionCheck);
                    listCmd.addIListeActions(getAdditionnalRowActions());
                    final ListeAddPanel listeCmd = new ListeAddPanel(elt, listCmd, "non validés");
                    final GridBagConstraints c = new DefaultGridBagConstraints();
                    c.gridy = GridBagConstraints.RELATIVE;
                    c.weightx = 1;
                    c.weighty = 1;
                    c.fill = GridBagConstraints.BOTH;
                    add(listeCmd, c);
                } catch (Exception e) {
                    ExceptionHandler.handle("Unable to create subscription list", e);
                }
            }

        };
        worker.execute();

    }

    /**
     * Validation d'un élément
     * 
     * @param sqlRowAccessor
     */
    protected void validItem(SQLRowAccessor sqlRowAccessor) {
        SQLRowValues rowVals = sqlRowAccessor.asRowValues();
        rowVals.put("CREATION_AUTO_VALIDER", Boolean.TRUE);
        try {
            rowVals.update();
        } catch (SQLException exn) {
            exn.printStackTrace();
        }
    }

    /**
     * Duplication des rowValuesItem
     * 
     * @param row
     * @param itemsTable
     * @param rowValsDest
     */
    private void copyItems(SQLRow row, SQLTable itemsTable, SQLRowValues rowValsDest) {
        // On duplique les elements de devis
        final List<SQLRow> myListItem = row.getReferentRows(itemsTable);

        for (final SQLRow rowElt : myListItem) {

            final SQLRowValues rowValsItem = rowElt.createUpdateRow();
            rowValsItem.clearPrimaryKeys();
            rowValsItem.put("ID_" + row.getTable().getName(), rowValsDest);
        }
    }

    /**
     * Création d'un nouvel élément à partir de celui de base
     * 
     * @param row
     * @param rowVals
     * @param dateNew
     * @param rowAbonnement
     */
    protected void injectRow(SQLRow row, SQLRowValues rowVals, Date dateNew, SQLRow rowAbonnement) {
        rowVals.put("ID_CLIENT", row.getObject("ID_CLIENT"));

        rowVals.put("ID_COMMERCIAL", row.getObject("ID_COMMERCIAL"));

        rowVals.put("DATE", dateNew);

        rowVals.put("T_HT", row.getObject("T_HT"));
        rowVals.put("T_TVA", row.getObject("T_TVA"));
        rowVals.put("T_SERVICE", row.getObject("T_SERVICE"));
        rowVals.put("T_TTC", row.getObject("T_TTC"));

        rowVals.put("INFOS", row.getObject("INFOS"));

        rowVals.put("T_POIDS", row.getObject("T_POIDS"));
        rowVals.put("ID_TARIF", row.getObject("ID_TARIF"));
        rowVals.put("ID_MODELE", row.getObject("ID_MODELE"));
        rowVals.put("ID_ABONNEMENT", rowAbonnement.getID());
        rowVals.put("CREATION_AUTO_VALIDER", Boolean.FALSE);

        if (row.getTable().contains("SOURCE")) {
            rowVals.put("SOURCE", row.getObject("SOURCE"));
            rowVals.put("IDSOURCE", row.getObject("IDSOURCE"));
        }

        if (row.getTable().contains("ID_AFFAIRE")) {
            rowVals.put("ID_AFFAIRE", row.getObject("ID_AFFAIRE"));
        }
    }

    /**
     * Vérification du renouvellement des abonnements
     * 
     * @param elt
     * @param itemsElement
     * @param type
     */
    public void parseAbonnement(SQLElement elt, SQLElement itemsElement, String type) {

        // Date de renouvellement des abonnements
        SubscriptionChecker checker = new SubscriptionChecker(elt.getTable());
        Map<SQLRow, Calendar> listLastCreateElt = checker.check();

        for (SQLRow rowAbonnement : listLastCreateElt.keySet()) {

            // On duplique le devis
            SQLRow rowsCmd = rowAbonnement.getForeignRow("ID_" + elt.getTable().getName());
            Calendar date = listLastCreateElt.get(rowAbonnement);
            if (date == null) {
                date = rowsCmd.getDate("DATE");
                date.add(Calendar.MONTH, rowAbonnement.getInt("NB_MOIS_" + type));
            }

            // Si l'abonnement n'est pas expiré
            if (date.compareTo(rowAbonnement.getDate("DATE_FIN_" + type)) <= 0) {

                final SQLRowValues rowVals = new SQLRowValues(elt.getTable());

                injectRow(rowsCmd, rowVals, date.getTime(), rowAbonnement);
                // On duplique items
                copyItems(rowsCmd, itemsElement.getTable(), rowVals);

                try {

                    rowVals.commit();
                    // FIXME Voir avec Guillaume create or not create document
                } catch (SQLException exn) {

                    ExceptionHandler.handle("Erreur lors de la création  " + elt.getSingularName() + " d'abonnement.", exn);
                }
            }
        }
    }
}
