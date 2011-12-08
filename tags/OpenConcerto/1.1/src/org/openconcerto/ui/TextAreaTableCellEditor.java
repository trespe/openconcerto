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
 
 package org.openconcerto.ui;

import org.openconcerto.utils.text.DocumentFilterList;
import org.openconcerto.utils.text.LimitedSizeDocumentFilter;
import org.openconcerto.utils.text.DocumentFilterList.FilterType;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Iterator;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class TextAreaTableCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {

    final JTextArea textArea = new JTextArea();

    public TextAreaTableCellEditor(final JTable t) {
        // Mimic JTable.GenericEditor behavior
        this.textArea.setBorder(new LineBorder(Color.black));
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    e.consume();

                    final int column;
                    final int row = t.getEditingRow();

                    // gestion tab ou shift+tab
                    if (e.getModifiers() == KeyEvent.SHIFT_MASK) {
                        column = t.getEditingColumn() - 1;
                    } else {
                        column = t.getEditingColumn() + 1;
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                       
                        public void run() {

                            fireEditingStopped();

                            if (column >= 0 && column < t.getColumnCount()) {
                                t.setColumnSelectionInterval(column, column);
                                t.setRowSelectionInterval(row, row);

                                if (t.editCellAt(row, column)) {
                                    t.getEditorComponent().requestFocusInWindow();
                                }
                            }
                        }
                    });
                } else {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getModifiers() == KeyEvent.SHIFT_MASK) {
                        // FIXME Utiliser une autre méthode
                        e.setModifiers(0);
                    }
                }
            }
        });

        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setFont(t.getFont());
        textArea.addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent e) {
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentResized(ComponentEvent e) {
                updateRowHeight(t, textArea.getText());
            }

            public void componentShown(ComponentEvent e) {
            }
        });
        textArea.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                updateRowHeight(t, e);
            }

            public void insertUpdate(DocumentEvent e) {

                updateRowHeight(t, e);

            }

            public void removeUpdate(DocumentEvent e) {
                updateRowHeight(t, e);

            }
        });

        // FIXME si on supprime une ligne sur laquelle on est en train d'éditer, il faut arréter
        // l'édition de la ligne
        // t.getModel().addTableModelListener(new TableModelListener() {
        // @Override
        // public void tableChanged(TableModelEvent e) {
        // // TODO Auto-generated method stub
        // if (e.getType() == TableModelEvent.DELETE) {
        // cancelCellEditing();
        // }
        // }
        // });
        // JScrollPane scrollPane = new JScrollPane(textArea);
        // scrollPane.setBorder(null);
        // editorComponent = textArea;

        // delegate = new DefaultCellEditor.EditorDelegate() {
        // public void setValue(Object value) {
        // System.err.println("TextAreaTableCell setValue ____ " + value);
        // textArea.setText((value != null) ? value.toString() : "");
        // }
        //
        // public Object getCellEditorValue() {
        // return textArea.getText();
        // }
        //
        // };
    }

    private void updateRowHeight(final JTable t, DocumentEvent e) {
        // Le nouveau text qui ira dans le textArea
        String newText = "";
        try {
            newText = e.getDocument().getText(0, e.getDocument().getLength());

        } catch (BadLocationException e1) {
            e1.printStackTrace();
        }
        updateRowHeight(t, newText);
        t.invalidate();
        t.repaint();
    }

    // retourne la hauteur qu'aurait le textArea si on lui mettait
    // le text passé en paramètre
    public static int getHeightFor(String s, JTextArea textArea) {
        Rectangle r = null;
        try {
            r = textArea.modelToView(s.length());
        } catch (BadLocationException e) {
            return -1;
        }
        int h = 0;
        // if the component does not yet have a positive size
        if (r != null) {
            final int newHeight = r.y + r.height + 2;
            h = Math.max(textArea.getMinimumSize().height, newHeight);
        }
        // System.out.println("TextAreaTableCellEditor.getHeightFor() h:" + h);
        return h;
    }

    public void updateRowHeight(final JTable t, String newText) {

        // On recupere la vraie taille du textarea
        int preferredHeight = getHeightFor(newText, textArea);// textArea.getPreferredSize().height;
        // Il peut arriver que ca ne fonctionne pas.. on se rabat alors sur le textarea
        if (preferredHeight == -1) {
            preferredHeight = textArea.getPreferredSize().height + 17;
        }
        EnhancedTable eTable = (EnhancedTable) t;
        int editingRow = eTable.getEditingRow();
        int editingCol = eTable.getEditingColumn();
        eTable.setPreferredRowHeight(editingRow, editingCol, preferredHeight);

        // Hauteur preferrée max de la ligne
        int maxHeight = eTable.getMaxRowHeight(editingRow);

        if (t.getRowHeight(editingRow) != maxHeight) {
            t.setRowHeight(editingRow, maxHeight);

        }
        t.repaint();
    }

    /**
     * @return Returns the textArea.
     */
    protected JTextArea getTextArea() {
        return textArea;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

        final Object o = value;
        textArea.setText((o != null) ? o.toString() : "");
        textArea.selectAll();

        Runnable r = new Runnable() {

            public void run() {
                textArea.grabFocus();
            }
        };
        SwingUtilities.invokeLater(r);
        return textArea;
    }

    public Object getCellEditorValue() {
        return textArea.getText();
    }

    public void actionPerformed(ActionEvent e) {
        this.stopCellEditing();
    }

    public void setLimitedSize(int nbChar) {
        // rm previous ones
        final DocumentFilterList dfl = DocumentFilterList.get((AbstractDocument) textArea.getDocument());
        final Iterator<DocumentFilter> iter = dfl.getFilters().iterator();
        while (iter.hasNext()) {
            final DocumentFilter df = iter.next();
            if (df instanceof LimitedSizeDocumentFilter)
                iter.remove();
        }
        // add the new one
        DocumentFilterList.add((AbstractDocument) textArea.getDocument(), new LimitedSizeDocumentFilter(nbChar), FilterType.SIMPLE_FILTER);
    }
}
