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
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.ui.EmailComposer;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

public class MouseSheetXmlListeListener implements MouseListener {

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

    public MouseSheetXmlListeListener(IListe liste, Class<? extends AbstractSheetXml> clazz) {
        this.clazz = clazz;
        this.liste = liste;
    }

    public MouseSheetXmlListeListener(IListe liste, Class<? extends AbstractSheetXml> clazz, boolean show, boolean preview, boolean print, boolean generate) {
        this.clazz = clazz;
        this.liste = liste;
        this.printIsVisible = print;
        this.previewIsVisible = preview;
        this.showIsVisible = show;
        this.generateIsVisible = generate;
    }

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
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {

        if (this.liste.getSelectedId() > 1) {
            if (e.getButton() == MouseEvent.BUTTON3) {
                System.err.println("Display Menu");
                JPopupMenu menuDroit = new JPopupMenu();

                final AbstractSheetXml bSheet = createAbstractSheet(this.liste.getSelectedRow());
                if (bSheet != null) {

                    for (RowAction action : getRowActions()) {
                        menuDroit.add(action.getAction());
                    }

                    menuDroit.pack();
                    menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
                    menuDroit.setVisible(true);
                }
            } else {
                String sfe = DefaultNXProps.getInstance().getStringProperty("ArticleSFE");
                Boolean bSfe = Boolean.valueOf(sfe);
                boolean isSFE = bSfe != null && bSfe.booleanValue();
                if (!isSFE) {
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                        final AbstractSheetXml bSheet = createAbstractSheet(this.liste.getSelectedRow());
                        if (bSheet.isFileODSExist()) {
                            if (!Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {
                                bSheet.showDocument();
                            } else {
                                bSheet.showPreviewDocument();
                            }
                        } else {
                            bSheet.genere(true, false);
                        }
                    }
                }
            }
        }
    }

    private void sendMail(final AbstractSheetXml sheet, final boolean readOnly) {

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
                    final File f = sheet.getFilePDF();
                    if (!f.exists()) {
                        try {
                            sheet.genere(false, false).get();
                            // final Component doc =
                            // ComptaPropsConfiguration.getOOConnexion().loadDocument(sheet.getFileODS(),
                            // true);

                            // Future<File> pdf = doc.saveToPDF(f);

                            EmailComposer.getInstance().compose(adresseMail, subject + (subject.trim().length() == 0 ? "" : ", ") + sheet.getFilePDF().getName(), "",
                                    sheet.getFilePDF().getAbsoluteFile());
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
                EmailComposer.getInstance().compose(adresseMail, subject + (subject.trim().length() == 0 ? "" : ", ") + sheet.getFileODS().getName(), "", sheet.getFileODS().getAbsoluteFile());
            } catch (Exception exn) {
                ExceptionHandler.handle(null, "Impossible de créer le courriel", exn);
            }
        }

    }

    public void mouseReleased(MouseEvent e) {
    }

    public List<AbstractAction> addToMenu() {
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
                l.add(new RowAction(new AbstractAction(this.showString) {
                    public void actionPerformed(ActionEvent ev) {
                        createAbstractSheet(liste.getSelectedRow()).showDocument();
                    }

                }, false) {
                    @Override
                    public boolean enabledFor(List<SQLRowAccessor> selection) {
                        return createAbstractSheet(liste.getSelectedRow()).isFileODSExist();
                    }
                });

                // if (bSheet.isFileOOExist()) {
                // item.setFont(item.getFont().deriveFont(Font.BOLD));
                // }
            }
        } else {
            if (this.previewIsVisible) {
                l.add(new RowAction(new AbstractAction(this.previewString) {
                    public void actionPerformed(ActionEvent ev) {
                        createAbstractSheet(liste.getSelectedRow()).showPreviewDocument();
                    }

                }, false) {
                    @Override
                    public boolean enabledFor(List<SQLRowAccessor> selection) {
                        return createAbstractSheet(liste.getSelectedRow()).isFileODSExist();
                    }
                });
                // if (bSheet.isFileOOExist()) {
                // item.setFont(item.getFont().deriveFont(Font.BOLD));
                // }
            }
        }

        // action supplémentaire
        List<AbstractAction> list = addToMenu();
        if (list != null) {
            for (AbstractAction abstractAction : list) {
                // JMenuItem itemItalic = new JMenuItem(abstractAction);
                // itemItalic.setFont(itemItalic.getFont().deriveFont(Font.ITALIC));
                l.add(new RowAction(abstractAction, false) {
                    @Override
                    public boolean enabledFor(List<SQLRowAccessor> selection) {
                        return selection != null && selection.size() > 0;
                    }
                });
            }
        }

        if (Boolean.getBoolean("org.openconcerto.oo.useODSViewer")) {

            if (this.showIsVisible) {
                l.add(new RowAction(new AbstractAction(this.showString) {
                    public void actionPerformed(ActionEvent ev) {
                        createAbstractSheet(liste.getSelectedRow()).showDocument();
                    }
                }, false) {
                    @Override
                    public boolean enabledFor(List<SQLRowAccessor> selection) {
                        return createAbstractSheet(liste.getSelectedRow()).isFileODSExist();
                    }
                });
            }
        }

        if (this.printIsVisible) {

            l.add(new RowAction(new AbstractAction(this.fastPrintString) {
                public void actionPerformed(ActionEvent ev) {
                    createAbstractSheet(liste.getSelectedRow()).fastPrintDocument();
                }
            }, false) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    return createAbstractSheet(liste.getSelectedRow()).isFileODSExist();
                }
            });

            l.add(new RowAction(new AbstractAction(this.printString) {
                public void actionPerformed(ActionEvent ev) {
                    createAbstractSheet(liste.getSelectedRow()).printDocument();
                }
            }, false) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    return createAbstractSheet(liste.getSelectedRow()).isFileODSExist();
                }
            });

            if (this.liste.getSelection().getSelectedIDs().size() > 1) {
                l.add(new RowAction(new AbstractAction(this.printAllString) {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        final int[] l = liste.getJTable().getSelectedRows();
                        final List<SQLRow> list = new ArrayList<SQLRow>();

                        for (int i = 0; i < l.length; i++) {
                            list.add(liste.getModel().getTable().getRow(liste.getLine(l[i]).getRow().getID()));
                        }

                        ListeFastPrintFrame frame = new ListeFastPrintFrame(list, clazz);
                        frame.setVisible(true);
                    }
                }, false) {
                    @Override
                    public boolean enabledFor(List<SQLRowAccessor> selection) {
                        return createAbstractSheet(liste.getSelectedRow()).isFileODSExist();
                    }
                });

            }

        }

        if (this.showIsVisible) {

            // if (createAbstractSheet(liste.getSelectedRow()).getSQLRow() != null) {
            l.add(new RowAction(new AbstractAction(this.mailPDFString) {
                public void actionPerformed(ActionEvent ev) {
                    sendMail(createAbstractSheet(liste.getSelectedRow()), true);
                }
            }, false) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    return createAbstractSheet(liste.getSelectedRow()).isFileODSExist();
                }
            });

            // }
            l.add(new RowAction(new AbstractAction(this.mailString) {
                public void actionPerformed(ActionEvent ev) {
                    sendMail(createAbstractSheet(liste.getSelectedRow()), false);
                }
            }, false) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    return createAbstractSheet(liste.getSelectedRow()).isFileODSExist();
                }
            });

        }
        if (this.generateIsVisible) {
            l.add(new RowAction(new AbstractAction(this.generateString) {
                public void actionPerformed(ActionEvent ev) {

                    createAbstractSheet(liste.getSelectedRow()).genere(true, false);
                }
            }, false) {
                @Override
                public boolean enabledFor(List<SQLRowAccessor> selection) {
                    return selection != null && selection.size() == 1;
                }
            });
        }

        return l;
    }
}
