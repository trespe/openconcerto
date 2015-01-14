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
 
 package org.openconcerto.erp.core.common.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellEditor;

public class NiveauTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final Color BG = new Color(232, 242, 252);
    private final int max;
    private final JPopupMenu popup;
    private final JPanel btnPanel;
    private int value;
    final NiveauTableCellRender renderer = new NiveauTableCellRender();

    public NiveauTableCellEditor() {
        this(4);
    }

    public NiveauTableCellEditor(final int max) {
        super();
        this.max = max;
        this.value = -1;

        this.popup = new JPopupMenu("levelEditor");
        this.popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            // a click outside
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                fireEditingCanceled();
            }
        });
        this.btnPanel = new JPanel(new FlowLayout());
        this.btnPanel.setOpaque(false);
        this.fillPopup();
        this.popup.pack();
    }

    protected void levelChosen(final int btnLevel) {
        this.value = btnLevel;
        this.popup.setVisible(false);
        fireEditingStopped();
    }

    private void fillPopup() {
        final JPanel p = new JPanel(new BorderLayout());
        p.setFocusable(true);
        p.setBackground(Color.WHITE);
        p.add(new JLabel(" Niveau"), BorderLayout.PAGE_START);
        for (int i = 1; i <= NiveauTableCellEditor.this.max; i++) {
            final JLabel btn = new JLabel(String.valueOf(i));
            btn.setHorizontalAlignment(SwingConstants.CENTER);
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    levelChosen(Integer.parseInt(btn.getText()));
                }
            });

            btn.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    highlight(Integer.parseInt(btn.getText()));
                }
            });

            btn.setPreferredSize(new Dimension(btn.getMinimumSize().width + 12, btn.getMinimumSize().height + 6));
            btn.setOpaque(true);
            this.btnPanel.add(btn);
        }
        p.add(this.btnPanel, BorderLayout.CENTER);
        btnPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_UP) {
                    value++;
                    if (value > max) {
                        value = max;
                    }
                    highlight(value);
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    value--;
                    if (value < 1) {
                        value = 1;
                    }
                    highlight(value);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    levelChosen(value);
                } else if (e.getKeyCode() == KeyEvent.VK_1) {
                    levelChosen(1);
                } else if (e.getKeyCode() == KeyEvent.VK_2) {
                    if (max >= 2)
                        levelChosen(2);
                } else if (e.getKeyCode() == KeyEvent.VK_3) {
                    if (max >= 3)
                        levelChosen(3);
                } else if (e.getKeyCode() == KeyEvent.VK_4) {
                    if (max >= 4)
                        levelChosen(4);
                } else if (e.getKeyCode() == KeyEvent.VK_5) {
                    if (max >= 5)
                        levelChosen(5);
                } else if (e.getKeyCode() == KeyEvent.VK_6) {
                    if (max >= 6)
                        levelChosen(6);
                } else if (e.getKeyCode() == KeyEvent.VK_7) {
                    if (max >= 7)
                        levelChosen(7);
                } else if (e.getKeyCode() == KeyEvent.VK_8) {
                    if (max >= 8)
                        levelChosen(8);
                } else if (e.getKeyCode() == KeyEvent.VK_9) {
                    if (max >= 9)
                        levelChosen(9);
                }

            }
        });
        this.popup.add(p);
    }

    private Component getBtnForLevel(final int level) {
        return this.btnPanel.getComponent(level - 1);
    }

    public void highlight(final int btnLevel) {
        for (int index = 1; index <= NiveauTableCellEditor.this.max; index++) {
            if (index != btnLevel) {
                getBtnForLevel(index).setBackground(Color.WHITE);
            } else {
                getBtnForLevel(index).setBackground(BG);
            }
        }
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, int row, int column) {
        if (value == null) {
            value = Integer.valueOf(1);
        }
        this.value = ((Number) value).intValue();
        this.renderer.setText(String.valueOf(this.value));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showPopup(renderer);
            }
        });
        return renderer;
    }

    protected void showPopup(final Component renderer) {
        if (renderer.isShowing()) {
            this.popup.show(renderer, 0, renderer.getHeight() + 2);
        }
        highlight(this.value);
        // For keyboard, must be temporary to avoid unexpected JTable focus lost and editing stopped
        btnPanel.requestFocus(true);
        // For table focus
        btnPanel.requestFocusInWindow();
    }

    @Override
    public Object getCellEditorValue() {
        return this.value;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JFrame f = new JFrame("Level test");
                JPanel p = new JPanel();
                p.setLayout(new BorderLayout());
                JTable t = new JTable(new Integer[][] { new Integer[] { 4, 3 }, new Integer[] { 2, 1 } }, new String[] { "A", "B" });
                p.add(new JTextField("Hello"), BorderLayout.NORTH);
                p.add(new JScrollPane(t), BorderLayout.CENTER);
                f.setContentPane(p);
                t.getColumnModel().getColumn(0).setCellEditor(new NiveauTableCellEditor());
                t.getColumnModel().getColumn(1).setCellEditor(new NiveauTableCellEditor());
                f.setSize(200, 200);
                f.setVisible(true);

            }
        });

    }
}
