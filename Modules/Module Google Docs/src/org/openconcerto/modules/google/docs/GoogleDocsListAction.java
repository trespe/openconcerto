package org.openconcerto.modules.google.docs;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Calendar;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.utils.ExceptionHandler;

public class GoogleDocsListAction implements IListeAction {

    @Override
    public ButtonsBuilder getHeaderButtons() {
        return ButtonsBuilder.emptyInstance();
    }

    @Override
    public Action getDefaultAction(IListeEvent evt) {
        return null;
    }

    @Override
    public PopupBuilder getPopupContent(PopupEvent evt) {
        final PopupBuilder actions = new PopupBuilder(this.getClass().getPackage().getName());
        final List<SQLRowAccessor> rows = evt.getSelectedRows();
        final JMenuItem createCallAction = createAction("Envoyer sur Google Docs", rows);
        actions.addItem(createCallAction);
        return actions;
    }

    private JMenuItem createAction(final String label, final List<SQLRowAccessor> rows) {
        return new JMenuItem(new AbstractAction(label) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        final public void run() {
                            try {
                                final GoogleDocsStorageEngine engine = new GoogleDocsStorageEngine();
                                engine.connect();
                                final SQLElementDirectory directory = Configuration.getInstance().getDirectory();
                                final SQLTable invoiceTable = directory.getElement(SaisieVenteFactureSQLElement.class).getTable();
                                final SQLTable quoteTable = directory.getElement(DevisSQLElement.class).getTable();
                                for (SQLRowAccessor sqlRowAccessor : rows) {

                                    if (sqlRowAccessor.getTable().equals(invoiceTable)) {
                                        final SQLRow row = sqlRowAccessor.asRow();
                                        final Calendar c = row.getDate("DATE");
                                        final VenteFactureXmlSheet venteFactureXmlSheet = new VenteFactureXmlSheet(row);
                                        final File pdf = venteFactureXmlSheet.getGeneratedPDFFile();
                                        if (!pdf.exists()) {
                                            venteFactureXmlSheet.getOrCreateDocumentFile();
                                            venteFactureXmlSheet.showPrintAndExport(false, false, true);
                                        }
                                        engine.store(pdf, "Factures/" + c.get(Calendar.YEAR), pdf.getName(), true);
                                    } else if (sqlRowAccessor.getTable().equals(quoteTable)) {
                                        final SQLRow row = sqlRowAccessor.asRow();
                                        final Calendar c = row.getDate("DATE");
                                        final DevisXmlSheet devisXmlSheet = new DevisXmlSheet(row);
                                        final File pdf = devisXmlSheet.getGeneratedPDFFile();
                                        if (!pdf.exists()) {
                                            devisXmlSheet.getOrCreateDocumentFile();
                                            devisXmlSheet.showPrintAndExport(false, false, true);
                                        }
                                        engine.store(pdf, "Devis/" + c.get(Calendar.YEAR), pdf.getName(), true);
                                    }

                                }
                                engine.disconnect();
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(null, e.getMessage());
                            }

                        }
                    });
                    t.start();
                } catch (Exception ex) {
                    ExceptionHandler.handle("Echec de l'envoi", ex);
                }
            }
        });
    }
}
