/*
 * Copyright 2005 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A.
 * All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 */
package org.jdesktop.swingx.plaf.basic;

import org.jdesktop.swingx.plaf.DatePickerUI;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXDatePickerFormatter;
import org.jdesktop.swingx.calendar.DateSpan;
import org.jdesktop.swingx.calendar.JXMonthView;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import java.awt.event.*;
import java.awt.*;
import java.util.Date;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * @author Joshua Outwater
 */
public class BasicDatePickerUI extends DatePickerUI {
    protected JXDatePicker datePicker;
    private JButton popupButton;
    private BasicDatePickerPopup popup;
    private Handler handler;
    protected PropertyChangeListener propertyChangeListener;
    protected MouseListener mouseListener;
    protected MouseMotionListener mouseMotionListener;

    public static ComponentUI createUI(JComponent c) {
        return new BasicDatePickerUI();
    }

    public void installUI(JComponent c) {
        this.datePicker = (JXDatePicker) c;
        this.datePicker.setLayout(createLayoutManager());
        installComponents();
        installDefaults();
        installKeyboardActions();
        installListeners();
    }

    public void uninstallUI(JComponent c) {
        uninstallListeners();
        uninstallKeyboardActions();
        uninstallDefaults();
        uninstallComponents();
        this.datePicker.setLayout(null);
        this.datePicker = null;
    }

    protected void installComponents() {
        JFormattedTextField editor = this.datePicker.getEditor();
        if (editor == null || editor instanceof UIResource) {
            this.datePicker.setEditor(createEditor());
        }
        this.datePicker.add(this.datePicker.getEditor());

        this.popupButton = createPopupButton();

        if (this.popupButton != null) {
            // this is a trick to get hold of the client prop which
            // prevents closing of the popup
            JComboBox box = new JComboBox();
            Object preventHide = box.getClientProperty("doNotCancelPopup");
            this.popupButton.putClientProperty("doNotCancelPopup", preventHide);
            this.datePicker.add(this.popupButton);
        }
    }

    protected void uninstallComponents() {
        this.datePicker.remove(this.datePicker.getEditor());

        if (this.popupButton != null) {
            this.datePicker.remove(this.popupButton);
            this.popupButton = null;
        }
    }

    protected void installDefaults() {

    }

    protected void uninstallDefaults() {

    }

    protected void installKeyboardActions() {
        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);

        JFormattedTextField editor = this.datePicker.getEditor();
        InputMap inputMap = editor.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(enterKey, "COMMIT_EDIT");

        ActionMap actionMap = editor.getActionMap();
        actionMap.put("COMMIT_EDIT", new CommitEditAction());

        KeyStroke spaceKey = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false);

        inputMap = this.popupButton.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(spaceKey, "TOGGLE_POPUP");

        actionMap = this.popupButton.getActionMap();
        actionMap.put("TOGGLE_POPUP", new TogglePopupAction());

    }

    protected void uninstallKeyboardActions() {

    }

    protected void installListeners() {
        this.propertyChangeListener = createPropertyChangeListener();
        this.mouseListener = createMouseListener();
        this.mouseMotionListener = createMouseMotionListener();

        this.datePicker.addPropertyChangeListener(this.propertyChangeListener);

        if (this.popupButton != null) {
            this.popupButton.addPropertyChangeListener(this.propertyChangeListener);
            this.popupButton.addMouseListener(this.mouseListener);
            this.popupButton.addMouseMotionListener(this.mouseMotionListener);
        }

    }

    protected void uninstallListeners() {
        this.datePicker.removePropertyChangeListener(this.propertyChangeListener);

        if (this.popupButton != null) {
            this.popupButton.removePropertyChangeListener(this.propertyChangeListener);
            this.popupButton.removeMouseListener(this.mouseListener);
            this.popupButton.removeMouseMotionListener(this.mouseMotionListener);
        }
        if (this.popup != null) {
            this.datePicker.getMonthView().removeActionListener(this.popup);
        }

        this.propertyChangeListener = null;
        this.mouseListener = null;
        this.mouseMotionListener = null;
        this.handler = null;
    }

    private Handler getHandler() {
        if (this.handler == null) {
            this.handler = new Handler();
        }
        return this.handler;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }

    protected LayoutManager createLayoutManager() {
        return getHandler();
    }

    protected MouseListener createMouseListener() {
        return getHandler();
    }

    protected MouseMotionListener createMouseMotionListener() {
        return getHandler();
    }

    /**
     * Creates the editor used to edit the date selection. Subclasses should override this method if
     * they want to substitute in their own editor.
     * 
     * @return an instance of a JFormattedTextField
     */
    protected JFormattedTextField createEditor() {
        JFormattedTextField f = new DefaultEditor(new JXDatePickerFormatter());
        f.setName("dateField");
        f.setColumns(UIManager.getInt("JXDatePicker.numColumns"));
        // f.setBorder(UIManager.getBorder("JXDatePicker.border"));

        return f;
    }

    protected JButton createPopupButton() {
        JButton b = new JButton();
        b.setName("popupButton");
        b.setRolloverEnabled(false);

        Icon icon = new ImageIcon(this.getClass().getResource("resources/picker.png"));// UIManager.getIcon("JXDatePicker.arrowDown.image");
        if (icon == null) {
            icon = (Icon) UIManager.get("Tree.expandedIcon");
        }
        b.setIcon(icon);
        b.setBorderPainted(false);
        b.setMargin(new Insets(2, 2, 2, 2));
        return b;
    }

    /**
     * {@inheritDoc}
     */

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /**
     * {@inheritDoc}
     */

    public Dimension getPreferredSize(JComponent c) {
        Dimension dim = this.datePicker.getEditor().getPreferredSize();
        if (this.popupButton != null) {
            dim.width += this.popupButton.getPreferredSize().width;
        }
        Insets insets = this.datePicker.getInsets();
        dim.width += insets.left + insets.right;
        dim.height += insets.top + insets.bottom;
        return (Dimension) dim.clone();
    }

    /**
     * Action used to commit the current value in the JFormattedTextField. This action is used by
     * the keyboard bindings.
     */
    private class CommitEditAction extends AbstractAction {
        public CommitEditAction() {
            super("CommitEditPopup");
        }

        public void actionPerformed(ActionEvent ev) {
            try {
                JFormattedTextField editor = BasicDatePickerUI.this.datePicker.getEditor();
                // Commit the current value.
                editor.commitEdit();

                // Reformat the value according to the formatter.
                editor.setValue(editor.getValue());
                BasicDatePickerUI.this.datePicker.postActionEvent();
            } catch (java.text.ParseException ex) {
            }
        }
    }

    /**
     * Action used to commit the current value in the JFormattedTextField. This action is used by
     * the keyboard bindings.
     */
    private class TogglePopupAction extends AbstractAction {
        public TogglePopupAction() {
            super("TogglePopup");
        }

        public void actionPerformed(ActionEvent ev) {
            BasicDatePickerUI.this.handler.toggleShowPopup();
        }
    }

    private class DefaultEditor extends JFormattedTextField implements UIResource {
        public DefaultEditor(AbstractFormatter formatter) {
            super(formatter);
            if (UIManager.getLookAndFeel().getName().equals("Nimbus")) {
                this.setBackground(Color.WHITE);
            }
        }
    }

    /**
     * Popup component that shows a JXMonthView component along with controlling buttons to allow
     * traversal of the months. Upon selection of a date the popup will automatically hide itself
     * and enter the selection into the editable field of the JXDatePicker.
     */
    protected class BasicDatePickerPopup extends JPopupMenu implements ActionListener {

        public BasicDatePickerPopup() {
            JXMonthView monthView = BasicDatePickerUI.this.datePicker.getMonthView();
            monthView.setActionCommand("MONTH_VIEW");
            monthView.addActionListener(this);

            setLayout(new BorderLayout());
            add(monthView, BorderLayout.CENTER);
            JPanel linkPanel = BasicDatePickerUI.this.datePicker.getLinkPanel();
            if (linkPanel != null) {
                add(linkPanel, BorderLayout.SOUTH);
            }
        }

        public void actionPerformed(ActionEvent ev) {
            String command = ev.getActionCommand();
            if ("MONTH_VIEW".equals(command)) {
                DateSpan span = BasicDatePickerUI.this.datePicker.getMonthView().getSelectedDateSpan();
                BasicDatePickerUI.this.datePicker.getEditor().setValue(span.getStartAsDate());
                setVisible(false);
                BasicDatePickerUI.this.datePicker.postActionEvent();
            }
        }
    }

    private class Handler implements LayoutManager, MouseListener, MouseMotionListener, PropertyChangeListener {
        private boolean _forwardReleaseEvent = false;

        public void mouseClicked(MouseEvent ev) {
        }

        public void mousePressed(MouseEvent ev) {
            if (!BasicDatePickerUI.this.datePicker.isEnabled()) {
                return;
            }

            if (!BasicDatePickerUI.this.datePicker.isEditable()) {
                JFormattedTextField editor = BasicDatePickerUI.this.datePicker.getEditor();
                if (editor.isEditValid()) {
                    try {
                        editor.commitEdit();
                    } catch (java.text.ParseException ex) {
                    }
                }
            }
            toggleShowPopup();
        }

        public void mouseReleased(MouseEvent ev) {
            if (!BasicDatePickerUI.this.datePicker.isEnabled() || !BasicDatePickerUI.this.datePicker.isEditable()) {
                return;
            }

            // Retarget mouse event to the month view.
            if (this._forwardReleaseEvent) {
                JXMonthView monthView = BasicDatePickerUI.this.datePicker.getMonthView();
                ev = SwingUtilities.convertMouseEvent(BasicDatePickerUI.this.popupButton, ev, monthView);
                monthView.dispatchEvent(ev);
                this._forwardReleaseEvent = false;
            }
        }

        public void mouseEntered(MouseEvent ev) {
        }

        public void mouseExited(MouseEvent ev) {
        }

        public void mouseDragged(MouseEvent ev) {
            if (!BasicDatePickerUI.this.datePicker.isEnabled() || !BasicDatePickerUI.this.datePicker.isEditable()) {
                return;
            }

            this._forwardReleaseEvent = true;

            if (!BasicDatePickerUI.this.popup.isShowing()) {
                return;
            }

            // Retarget mouse event to the month view.
            JXMonthView monthView = BasicDatePickerUI.this.datePicker.getMonthView();
            ev = SwingUtilities.convertMouseEvent(BasicDatePickerUI.this.popupButton, ev, monthView);
            monthView.dispatchEvent(ev);
        }

        public void mouseMoved(MouseEvent ev) {
        }

        public void toggleShowPopup() {

            if (BasicDatePickerUI.this.popup == null) {
                BasicDatePickerUI.this.popup = new BasicDatePickerPopup();
            }
            if (!BasicDatePickerUI.this.popup.isVisible()) {
                JFormattedTextField editor = BasicDatePickerUI.this.datePicker.getEditor();
                if (editor.getValue() == null) {
                    editor.setValue(new Date(BasicDatePickerUI.this.datePicker.getLinkDate()));
                }
                DateSpan span = new DateSpan((java.util.Date) editor.getValue(), (java.util.Date) editor.getValue());
                JXMonthView monthView = BasicDatePickerUI.this.datePicker.getMonthView();
                monthView.setSelectedDateSpan(span);
                monthView.ensureDateVisible(((Date) editor.getValue()).getTime());
                BasicDatePickerUI.this.popup.show(BasicDatePickerUI.this.datePicker, 0, BasicDatePickerUI.this.datePicker.getHeight());
            } else {
                BasicDatePickerUI.this.popup.setVisible(false);
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String property = e.getPropertyName();

            if ("enabled".equals(property)) {
                boolean isEnabled = BasicDatePickerUI.this.datePicker.isEnabled();
                BasicDatePickerUI.this.popupButton.setEnabled(isEnabled);
                BasicDatePickerUI.this.datePicker.getEditor().setEnabled(isEnabled);
            } else if ("editable".equals(property)) {
                boolean isEditable = BasicDatePickerUI.this.datePicker.isEditable();
                BasicDatePickerUI.this.datePicker.getMonthView().setEnabled(isEditable);
                BasicDatePickerUI.this.datePicker.getEditor().setEditable(isEditable);
            } else if (JComponent.TOOL_TIP_TEXT_KEY.equals(property)) {
                String tip = BasicDatePickerUI.this.datePicker.getToolTipText();
                BasicDatePickerUI.this.datePicker.getEditor().setToolTipText(tip);
                BasicDatePickerUI.this.popupButton.setToolTipText(tip);
            } else if (JXDatePicker.MONTH_VIEW.equals(property)) {
                BasicDatePickerUI.this.popup = null;
            } else if (JXDatePicker.LINK_PANEL.equals(property)) {
                // If the popup is null we haven't shown it yet.
                JPanel linkPanel = BasicDatePickerUI.this.datePicker.getLinkPanel();
                if (BasicDatePickerUI.this.popup != null) {
                    BasicDatePickerUI.this.popup.remove(linkPanel);
                    popup.add(linkPanel, BorderLayout.SOUTH);
                }
            } else if (JXDatePicker.EDITOR.equals(property)) {
                JFormattedTextField oldEditor = (JFormattedTextField) e.getOldValue();
                if (oldEditor != null) {
                    datePicker.remove(oldEditor);
                }

                JFormattedTextField editor = (JFormattedTextField) e.getNewValue();
                datePicker.add(editor);
                datePicker.revalidate();
            } else if ("componentOrientation".equals(property)) {
                datePicker.revalidate();
            }
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            return parent.getPreferredSize();
        }

        public Dimension minimumLayoutSize(Container parent) {
            return parent.getMinimumSize();
        }

        public void layoutContainer(Container parent) {
            Insets insets = datePicker.getInsets();
            int width = datePicker.getWidth() - insets.left - insets.right;
            int height = datePicker.getHeight() - insets.top - insets.bottom;

            int popupButtonWidth = popupButton != null ? popupButton.getPreferredSize().width : 0;

            boolean ltr = datePicker.getComponentOrientation().isLeftToRight();

            datePicker.getEditor().setBounds(ltr ? insets.left : insets.left + popupButtonWidth, insets.bottom, width - popupButtonWidth, height);

            if (popupButton != null) {
                popupButton.setBounds(ltr ? width - popupButtonWidth + insets.right : insets.left, insets.bottom, popupButtonWidth, height);
            }
        }
    }
}
