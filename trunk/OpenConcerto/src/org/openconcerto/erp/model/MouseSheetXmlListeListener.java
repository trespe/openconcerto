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
 
 package org.openconcerto.erp.model;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.AbstractSheetXml;
import org.openconcerto.erp.panel.ListeFastPrintFrame;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.ui.EmailComposer;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class MouseSheetXmlListeListener {

    private Class<? extends AbstractSheetXml> clazz;
    protected IListe liste;

    private boolean previewIsVisible = true;
    private boolean showIsVisible = true;
    private boolean printIsVisible = true;
    private boolean generateIsVisible = true;
    private boolean previewHeader = false;
    private boolean showHeader = false;
    private boolean printHeader = false;
    private boolean generateHeader = false;

    public MouseSheetXmlListeListener(Class<? extends AbstractSheetXml> clazz) {
        this(clazz, true, true, true, true);

    }

    public MouseSheetXmlListeListener(Class<? extends AbstractSheetXml> clazz, boolean show, boolean preview, boolean print, boolean generate) {
        this.clazz = clazz;
        this.printIsVisible = print;
        this.previewIsVisible = preview;
        this.showIsVisible = show;
        this.generateIsVisible = generate;
    }

    protected Class<? extends AbstractSheetXml> getSheetClass() {
        return this.clazz;
    }

    protected AbstractSheetXml createAbstractSheet(SQLRow row) {
        try {
            Constructor<? extends AbstractSheetXml> ctor = getSheetClass().getConstructor(SQLRow.class);
            AbstractSheetXml sheet = ctor.newInstance(row);
            return sheet;
        } catch (Exception e) {
            ExceptionHandler.handle("sheet creation error", e);
        }
        return null;
    }

    protected String getMailObject(SQLRow row) {
        return "";
    }

    public void setPreviewHeader(boolean previewHeader) {
        this.previewHeader = previewHeader;
    }

    public void setGenerateHeader(boolean generateHeader) {
        this.generateHeader = generateHeader;
    }

    public void setPrintHeader(boolean printHeader) {
        this.printHeader = printHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }

    protected void sendMail(final AbstractSheetXml sheet, final boolean readOnly) {

        final SQLRow row = sheet.getSQLRow();
        Set<SQLField> setContact = null;
        SQLTable tableContact = Configuration.getInstance().getRoot().findTable("CONTACT");
        setContact = row.getTable().getForeignKeys(tableContact);

        Set<SQLField> setClient = null;
        SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("CLIENT");
        setClient = row.getTable().getForeignKeys(tableClient);

        String mail = "";

        for (SQLField field : setContact) {
            if (mail == null || mail.trim().length() == 0) {
                mail = row.getForeignRow(field.getName()).getString("EMAIL");
            }
        }

        if (setClient != null && (mail == null || mail.trim().length() == 0)) {
                for (SQLField field : setClient) {
                    SQLRow rowCli = row.getForeignRow(field.getName());
                    if (mail == null || mail.trim().length() == 0) {
                        mail = rowCli.getString("MAIL");
                    }
                }
        }

        if (mail == null || mail.trim().length() == 0) {
            SQLTable tableF = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("FOURNISSEUR");
            Set<SQLField> setF = null;
            setF = row.getTable().getForeignKeys(tableF);

            if (setF != null) {

                for (SQLField field : setF) {
                    SQLRow rowF = row.getForeignRow(field.getName());
                    if (mail == null || mail.trim().length() == 0) {
                        mail = rowF.getString("MAIL");
                    }
                }
            }

            if (mail == null || mail.trim().length() == 0) {
                SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                if (base.containsTable("MONTEUR")) {

                    SQLTable tableM = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("MONTEUR");
                    Set<SQLField> setM = null;
                    setM = row.getTable().getForeignKeys(tableM);

                    if (setM != null) {

                        for (SQLField field : setM) {
                            SQLRow rowM = row.getForeignRow(field.getName());
                            if (rowM.getForeignRow("ID_CONTACT_FOURNISSEUR") != null && !rowM.getForeignRow("ID_CONTACT_FOURNISSEUR").isUndefined()) {
                                mail = rowM.getForeignRow("ID_CONTACT_FOURNISSEUR").getString("EMAIL");
                            }
                        }
                    }
                }
            }
            if (mail == null || mail.trim().length() == 0) {
                SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                if (base.containsTable("TRANSPORTEUR")) {

                    SQLTable tableM = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("TRANSPORTEUR");
                    Set<SQLField> setM = null;
                    setM = row.getTable().getForeignKeys(tableM);

                    if (setM != null) {

                        for (SQLField field : setM) {
                            SQLRow rowM = row.getForeignRow(field.getName());
                            if (rowM.getForeignRow("ID_CONTACT_FOURNISSEUR") != null && !rowM.getForeignRow("ID_CONTACT_FOURNISSEUR").isUndefined()) {
                                mail = rowM.getForeignRow("ID_CONTACT_FOURNISSEUR").getString("EMAIL");
                            }
                        }
                    }
                }
            }
        }

        final String adresseMail = mail;

        final String subject = sheet.getReference();

        if (readOnly) {

            final Thread t = new Thread() {
                @Override
                public void run() {
                    final File f;
                    try {
                        f = sheet.getOrCreatePDFDocumentFile(true);
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    EmailComposer.getInstance().compose(adresseMail, subject + (subject.trim().length() == 0 ? "" : ", ") + f.getName(), getMailObject(row), f.getAbsoluteFile());
                                } catch (Exception e) {
                                    ExceptionHandler.handle("Impossible de charger le document PDF dans l'email!", e);
                                }
                            }
                        });
                    } catch (Exception e) {
                        ExceptionHandler.handle("Impossible de charger le document PDF", e);
                    }
                }
            };

            t.start();
        } else {
            try {
                EmailComposer.getInstance().compose(adresseMail, subject + (subject.trim().length() == 0 ? "" : ", ") + sheet.getGeneratedFile().getName(), getMailObject(row),
                        sheet.getGeneratedFile().getAbsoluteFile());
            } catch (Exception exn) {
                ExceptionHandler.handle(null, "Impossible de créer le courriel", exn);
            }
        }

    }

    public List<RowAction> addToMenu() {
        return null;
    }

    public List<RowAction> getRowActions() {
        List<RowAction> l = new ArrayList<RowAction>();

        if (!Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {
            if (this.showIsVisible) {
                RowAction action = new RowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent ev) {
                        System.err.println("");
                        createAbstractSheet(IListe.get(ev).getSelectedRow()).openDocument(false);
                    }

                }, this.previewHeader, "document.modify") {
                    @Override
                    public boolean enabledFor(IListeEvent evt) {

                        return evt.getSelectedRow() != null && (evt.getTotalRowCount() >= 1) && (createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists());
                    }

                };
                l.add(action);

            }
        } else {
            if (this.previewIsVisible) {
                l.add(new RowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent ev) {
                        try {
                            createAbstractSheet(IListe.get(ev).getSelectedRow()).showPreviewDocument();
                        } catch (Exception e) {
                            ExceptionHandler.handle("Impossilbe d'ouvrir le fichier", e);
                        }
                    }

                }, this.previewHeader, "document.preview") {

                    @Override
                    public boolean enabledFor(IListeEvent evt) {
                        return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                    }
                });

            }
        }

        // action supplémentaire
        List<RowAction> list = addToMenu();
        if (list != null) {
            for (RowAction rowAction : list) {
                l.add(rowAction);
            }
        }

        if (Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {

            if (this.showIsVisible) {
                l.add(new RowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent ev) {
                        createAbstractSheet(IListe.get(ev).getSelectedRow()).openDocument(false);
                    }
                }, this.showHeader, "document.modify") {
                    @Override
                    public boolean enabledFor(IListeEvent evt) {
                        return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                    }
                });
            }
        }

        if (this.printIsVisible) {

            l.add(new RowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent ev) {
                    createAbstractSheet(IListe.get(ev).getSelectedRow()).fastPrintDocument();
                }

            }, this.printHeader, "document.quickprint") {

                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

            l.add(new RowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent ev) {
                    createAbstractSheet(IListe.get(ev).getSelectedRow()).printDocument();
                }
            }, false, "document.print") {
                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

            PredicateRowAction rowAction = new PredicateRowAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    List<SQLRowAccessor> list = IListe.get(e).getSelectedRows();
                    ListeFastPrintFrame frame = new ListeFastPrintFrame(list, clazz);
                    frame.setVisible(true);
                }
            }, this.previewHeader, "document.print.all");
            rowAction.setPredicate(IListeEvent.createSelectionCountPredicate(2, Integer.MAX_VALUE));

            l.add(rowAction);

        }

        if (this.showIsVisible) {

            l.add(new RowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent ev) {
                    sendMail(createAbstractSheet(IListe.get(ev).getSelectedRow()), true);
                }
            }, false, "document.pdf.send.email") {
                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

            l.add(new RowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent ev) {
                    sendMail(createAbstractSheet(IListe.get(ev).getSelectedRow()), false);
                }
            }, false, "document.send.email") {
                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

        }
        if (this.generateIsVisible) {
            l.add(new RowAction(new AbstractAction() {
                public void actionPerformed(ActionEvent ev) {
                    createDocument(ev);
                }
            }, this.generateHeader, "document.create") {

                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    return selection != null && selection.size() == 1;
                }

            });
        }

        return l;
    }

    private void createDocument(ActionEvent ev) {
        final AbstractSheetXml sheet = createAbstractSheet(IListe.get(ev).getSelectedRow());
        if (sheet.getGeneratedFile().exists()) {
            int a = JOptionPane.showConfirmDialog(null, "Voulez vous remplacer le document existant?", "Génération de documents", JOptionPane.YES_NO_OPTION);
            if (a == JOptionPane.YES_OPTION) {
                sheet.createDocumentAsynchronous();
                sheet.showPrintAndExportAsynchronous(true, false, true);
                return;
            }
        }

        try {
            sheet.getOrCreateDocumentFile();
            sheet.showPrintAndExportAsynchronous(true, false, false);
        } catch (Exception exn) {
            // TODO Bloc catch auto-généré
            exn.printStackTrace();
        }

    }

    /**
     * Action sur le double clic
     * 
     * @return
     */
    public RowAction getDefaultRowAction() {
        return new RowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent ev) {
                final AbstractSheetXml sheet = createAbstractSheet(IListe.get(ev).getSelectedRow().asRow());
                try {
                    sheet.getOrCreateDocumentFile();
                    sheet.showPrintAndExportAsynchronous(true, false, true);
                } catch (Exception exn) {
                    ExceptionHandler.handle("Une erreur est survenue lors de la création du document.", exn);
                }
            }
        }, false, false, "document.create") {
            @Override
            public boolean enabledFor(List<SQLRowAccessor> selection) {
                return selection != null && selection.size() == 1;
            }

            @Override
            public Action getDefaultAction(final IListeEvent evt) {
                return this.getAction();
            }
        };
    }
}
