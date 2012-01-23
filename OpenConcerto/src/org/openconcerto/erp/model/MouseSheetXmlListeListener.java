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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;

public class MouseSheetXmlListeListener {

    private Class<? extends AbstractSheetXml> clazz;
    protected IListe liste;
    private String fastPrintString = "Impression rapide";
    private String printString = "Impression ...";
    private String printAllString = "Imprimer les documents";
    private String previewString = "Voir le document";
    private String showString = "Modifier le document avec OpenOffice";
    private String generateString = "Générer le document";
    private String mailPDFString = "Envoyer le document PDF par email";
    private String mailString = "Envoyer le document par email";

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

    // FIXME clear cache if row was updated
    private static Map<SQLRow, AbstractSheetXml> cache = new HashMap<SQLRow, AbstractSheetXml>();

    protected Class<? extends AbstractSheetXml> getSheetClass() {
        return this.clazz;
    }

    protected AbstractSheetXml createAbstractSheet(SQLRow row) {
        if (cache.get(row) != null) {
            return cache.get(row);
        } else {
            try {
                Constructor<? extends AbstractSheetXml> ctor = getSheetClass().getConstructor(SQLRow.class);
                AbstractSheetXml sheet = ctor.newInstance(row);
                cache.put(row, sheet);
                return sheet;
            } catch (Exception e) {
                // FIXME Exception Handler ??
                e.printStackTrace();
            }
            return null;
        }
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

        SQLRow row = sheet.getSQLRow();
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

        final String adresseMail = mail;

        final String subject = sheet.getReference();

        if (readOnly) {

            final Thread t = new Thread() {
                @Override
                public void run() {
                    final File f = sheet.getGeneratedPDFFile();
                    if (!f.exists()) {
                        try {
                            sheet.getOrCreateDocumentFile();
                            sheet.showPrintAndExport(false, false, true);
                            EmailComposer.getInstance().compose(adresseMail, subject + (subject.trim().length() == 0 ? "" : ", ") + f.getName(), "", f.getAbsoluteFile());
                        } catch (Exception e) {
                            e.printStackTrace();
                            ExceptionHandler.handle("Impossible de charger le document PDF", e);
                        }
                    } else {
                        try {
                            EmailComposer.getInstance().compose(adresseMail, subject + (subject.trim().length() == 0 ? "" : ", ") + f.getName(), "", f.getAbsoluteFile());
                        } catch (Exception exn) {
                            ExceptionHandler.handle(null, "Impossible de créer le courriel", exn);
                        }
                    }

                }
            };

            t.start();
        } else {
            try {
                EmailComposer.getInstance().compose(adresseMail, subject + (subject.trim().length() == 0 ? "" : ", ") + sheet.getGeneratedFile().getName(), "",
                        sheet.getGeneratedFile().getAbsoluteFile());
            } catch (Exception exn) {
                ExceptionHandler.handle(null, "Impossible de créer le courriel", exn);
            }
        }

    }

    public List<RowAction> addToMenu() {
        return null;
    }

    public void setGenerateString(String generateString) {
        this.generateString = generateString;
    }

    public void setPreviewString(String previewString) {
        this.previewString = previewString;
    }

    public void setShowString(String showString) {
        this.showString = showString;
    }

    public void setFastPrintString(String printString) {
        this.fastPrintString = printString;
    }

    public void setprintString(String printString) {
        this.printString = printString;
    }

    public List<RowAction> getRowActions() {
        List<RowAction> l = new ArrayList<RowAction>();

        if (!Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {
            if (this.showIsVisible) {
                RowAction action = new RowAction(new AbstractAction(this.showString) {
                    public void actionPerformed(ActionEvent ev) {
                        System.err.println("");
                        createAbstractSheet(IListe.get(ev).getSelectedRow()).openDocument(false);
                    }

                }, this.previewHeader) {
                    @Override
                    public boolean enabledFor(IListeEvent evt) {

                        return evt.getSelectedRow() != null && (evt.getTotalRowCount() >= 1) && (createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists());
                    }
                };
                l.add(action);

            }
        } else {
            if (this.previewIsVisible) {
                l.add(new RowAction(new AbstractAction(this.previewString) {
                    public void actionPerformed(ActionEvent ev) {
                        try {
                            createAbstractSheet(IListe.get(ev).getSelectedRow()).showPreviewDocument();
                        } catch (Exception e) {
                            ExceptionHandler.handle("Impossilbe d'ouvrir le fichier", e);
                        }
                    }

                }, this.previewHeader) {

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
                l.add(new RowAction(new AbstractAction(this.showString) {
                    public void actionPerformed(ActionEvent ev) {
                        createAbstractSheet(IListe.get(ev).getSelectedRow()).openDocument(false);
                    }
                }, this.showHeader) {
                    @Override
                    public boolean enabledFor(IListeEvent evt) {
                        return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                    }
                });
            }
        }

        if (this.printIsVisible) {

            l.add(new RowAction(new AbstractAction(this.fastPrintString) {
                public void actionPerformed(ActionEvent ev) {
                    createAbstractSheet(IListe.get(ev).getSelectedRow()).fastPrintDocument();
                }
            }, this.printHeader) {
                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

            l.add(new RowAction(new AbstractAction(this.printString) {
                public void actionPerformed(ActionEvent ev) {
                    createAbstractSheet(IListe.get(ev).getSelectedRow()).printDocument();
                }
            }, false) {
                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

            PredicateRowAction rowAction = new PredicateRowAction(new AbstractAction(this.printAllString) {
                @Override
                public void actionPerformed(ActionEvent e) {

                    // final int[] l = liste.getJTable().getSelectedRows();
                    // final List<SQLRow> list = new ArrayList<SQLRow>();
                    List<SQLRowAccessor> list = IListe.get(e).getSelectedRows();
                    ListeFastPrintFrame frame = new ListeFastPrintFrame(list, clazz);
                    frame.setVisible(true);
                }
            }, this.previewHeader);
            rowAction.setPredicate(IListeEvent.createSelectionCountPredicate(2, Integer.MAX_VALUE));

            l.add(rowAction);

        }

        if (this.showIsVisible) {

            // if (createAbstractSheet(liste.getSelectedRow()).getSQLRow() !=
            // null) {
            l.add(new RowAction(new AbstractAction(this.mailPDFString) {
                public void actionPerformed(ActionEvent ev) {
                    sendMail(createAbstractSheet(IListe.get(ev).getSelectedRow()), true);
                }
            }, false) {
                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

            // }
            l.add(new RowAction(new AbstractAction(this.mailString) {
                public void actionPerformed(ActionEvent ev) {
                    sendMail(createAbstractSheet(IListe.get(ev).getSelectedRow()), false);
                }
            }, false) {
                @Override
                public boolean enabledFor(IListeEvent evt) {
                    return evt.getSelectedRow() != null && evt.getTotalRowCount() >= 1 && createAbstractSheet(evt.getSelectedRow().asRow()).getGeneratedFile().exists();
                }
            });

        }
        if (this.generateIsVisible) {
            l.add(new RowAction(new AbstractAction(this.generateString) {
                public void actionPerformed(ActionEvent ev) {

                    final AbstractSheetXml sheet = createAbstractSheet(IListe.get(ev).getSelectedRow());
                    sheet.createDocumentAsynchronous();
                    sheet.showPrintAndExportAsynchronous(true, false, true);
                }
            }, this.generateHeader) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    return selection != null && selection.size() == 1;
                }
            });
        }

        return l;
    }
}
