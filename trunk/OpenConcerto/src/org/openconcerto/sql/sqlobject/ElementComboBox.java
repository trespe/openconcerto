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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.EditPanelListener;
import org.openconcerto.sql.view.IListButton;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.list.selection.BaseListStateModel;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * A SQLRequestComboBox with an SQLElement allowing it to have buttons for zooming on a selected
 * item, or listing the items in a separate window.
 * 
 * @author Sylvain CUAZ
 */
public class ElementComboBox extends SQLRequestComboBox implements ActionListener {

    /**
     * This system property is used as the initial value for {@link #setCanModify(boolean)}.
     */
    public static final String CAN_MODIFY = "org.openconcerto.sql.comboCanModify";

    private static ImageIcon icon = null;
    private static ImageIcon iconModif = null;
    private static ImageIcon iconAdd = null;

    private static ITransformer<ElementComboBox, Boolean> globalRowDisplayer = null;

    // no need for synchronization, all in EDT
    private static void checkLoaded() {
        if (icon == null) {
            icon = new ImageIcon(ElementComboBox.class.getResource("loupe.png"));
            iconAdd = new ImageIcon(ElementComboBox.class.getResource("plus.png"));
            iconModif = new ImageIcon(ElementComboBox.class.getResource("pen.png"));
        }
    }

    private static Icon getDetailsIcon() {
        checkLoaded();
        return icon;
    }

    private static Icon getModifIcon() {
        checkLoaded();
        return iconModif;
    }

    private static Icon getAddIcon() {
        checkLoaded();
        return iconAdd;
    }

    /**
     * Set an alternative way to display a row for the view button.
     * 
     * @param transf will be passed the comboBox and must return whether the row was displayed, can
     *        be <code>null</code>.
     */
    public static void setGlobalRowDisplayer(final ITransformer<ElementComboBox, Boolean> transf) {
        globalRowDisplayer = transf;
    }

    // on l'a pas à l'instanciation
    private SQLElement element;

    // Interface graphique
    // true if viewButton can sometimes modify
    private Boolean canModif = null;
    // true if viewButton can currently modify the selection
    private Boolean isModif = null;
    private final JButton viewButton = new JButton();
    private final IListButton listButton = new IListButton();
    private final JButton addButton = new JButton();

    // Frames
    private IListFrame listFrame = null;
    private EditFrame viewFrame = null;
    private EditFrame addFrame = null;

    private boolean minimal = false;

    public ElementComboBox() {
        super();
    }

    public ElementComboBox(boolean addUndefined) {
        super(addUndefined);
    }

    public ElementComboBox(boolean addUndefined, int preferredWidthInChar) {
        super(addUndefined, preferredWidthInChar);
    }

    /**
     * Init de l'interface graphique.
     * 
     * @param element which table to display and how.
     * @return this
     */
    public final ElementComboBox init(SQLElement element) {
        return this.init(element, element.getComboRequest());
    }

    public final ElementComboBox init(SQLElement element, final ComboSQLRequest req) {
        if (element.getTable() != req.getPrimaryTable())
            throw new IllegalArgumentException("Tables are different " + element.getTable().getSQLName() + " != " + req.getPrimaryTable().getSQLName());
        this.element = element;
        this.uiInit(req);
        return this;
    }

    @Override
    public void init(SQLRowItemView v) {
        final SQLTable foreignTable = v.getField().getDBSystemRoot().getGraph().getForeignTable(v.getField());
        if (foreignTable == null) {
            throw new IllegalArgumentException("No foreign table for " + v.getField().getFullName());
        }
        if (this.getElement() == null)
            this.init(Configuration.getInstance().getDirectory().getElement(foreignTable));
        else if (this.getElement().getTable() != foreignTable)
            throw new IllegalArgumentException("Tables are different " + getElement().getTable().getSQLName() + " != " + foreignTable.getSQLName());
    }

    public final SQLElement getElement() {
        return this.element;
    }

    /**
     * Whether the first button allow one to view or modify the selection.
     * 
     * @param canModif <code>false</code> to only view the selection.
     */
    public final void setCanModify(boolean canModif) {
        if (this.canModif == null || this.canModif != canModif) {
            this.canModif = canModif;
            this.updateViewBtn();
        }
    }

    private final void setViewBtn(boolean modif) {
        if (this.isModif == null || this.isModif != modif) {
            this.isModif = modif;
            this.viewFrame = null;
            if (this.isModif) {
                this.viewButton.setToolTipText("Modifier");
                this.viewButton.setIcon(getModifIcon());
            } else {
                this.viewButton.setToolTipText("Voir plus de détails");
                this.viewButton.setIcon(getDetailsIcon());
            }
            // each time its icon change, otherwise (at least on Mac) the opacity is wrong
            IListButton.initButton(this.viewButton);
        }
    }

    @Override
    protected void uiLayout() {
        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.weightx = 1;

        this.combo.getActions().add(new AbstractAction("Tout afficher") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ElementComboBox.this.actionPerformed(e);
            }
        });

        this.add(this.combo, c);

        if (!this.minimal) {
            c.weightx = 0;
            c.gridx++;

            this.viewButton.setPreferredSize(new Dimension(24, 16));
            this.setCanModify(Boolean.getBoolean(CAN_MODIFY));
            DefaultGridBagConstraints.lockMinimumSize(this.viewButton);
            add(this.viewButton, c);
            c.gridx++;
            this.listButton.setToolTipText("Lister les " + this.element.getPluralName());
            DefaultGridBagConstraints.lockMinimumSize(this.listButton);
            add(this.listButton, c);

            // Add button
            this.addButton.setPreferredSize(new Dimension(24, 16));
            this.addButton.setIcon(getAddIcon());
            IListButton.initButton(this.addButton);
            this.addButton.setToolTipText("Créer " + this.element.getSingularName());
            c.gridx++;
            DefaultGridBagConstraints.lockMinimumSize(this.addButton);
            add(this.addButton, c);
            this.addButton.addActionListener(this);
            setAddIconVisible(Boolean.getBoolean("org.openconcerto.ui.addComboButton"));

            this.viewButton.addActionListener(this);
            this.listButton.addActionListener(this);
            this.addValueListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    // don't change our frames' selection when we reload
                    // (furthermore our listener on the list will change idToSelect to the
                    // temporary one, thus rending it ineffective)
                    if (!isUpdating())
                        valueChanged();
                }
            });

            if (Boolean.getBoolean("org.openconcerto.ui.simpleTraversal")) {
                this.viewButton.setFocusable(false);
                this.listButton.setFocusable(false);
                this.addButton.setFocusable(false);
            }
        } else {
            this.setCanModify(false);
        }
        // don't want NPE
        assert this.canModif != null && this.isModif != null : "Modif booleans not initialized, this.canModif: " + this.canModif + ", this.isModif: " + this.isModif;
    }

    private void valueChanged() {
        updateViewBtn();
        if (this.viewFrame != null) {
            // changer la frame du détail
            this.viewFrame.selectionId(this.getSelectedId());
        }
        if (this.listFrame != null) {
            this.listFrame.getPanel().getListe().selectID(this.getSelectedId());
        }
    }

    private final boolean displayRow() {
        if (globalRowDisplayer == null)
            return false;
        else
            return globalRowDisplayer.transformChecked(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.viewButton) {
            final boolean displayed;
            if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                displayed = displayRow();
            } else {
                displayed = false;
            }
            if (!displayed) {
                if (this.viewFrame == null) {
                    this.viewFrame = new EditFrame(this.element, this.isModif ? EditPanel.MODIFICATION : EditPanel.READONLY);
                    // dispose since if we change canModif, the old frame will be orphaned
                    this.viewFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                }
                this.viewFrame.selectionId(getSelectedId());
                FrameUtil.show(this.viewFrame);
            }
        } else if (e.getSource() == this.addButton) {
            FrameUtil.show(this.getAddFrame());
        } else if (e.getSource() == this.listButton || e.getSource() == this.combo) {
            if (this.listFrame == null) {
                this.listFrame = new IListFrame(new ListeAddPanel(this.element));
                // listen to userSelection (eg avoid emptying the selection, when the search or the
                // hibernation empties the list)
                this.listFrame.getPanel().getListe().getSelection().addPropertyChangeListener("userSelectedID", new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        final int newID = ((Number) evt.getNewValue()).intValue();
                        setValue(newID == BaseListStateModel.INVALID_ID ? null : newID);
                    }
                });
                this.listFrame.getPanel().getListe().selectID(getSelectedId());
            }
            FrameUtil.show(this.listFrame);
        }
    }

    private final EditFrame getAddFrame() {
        if (this.addFrame == null) {
            this.addFrame = new EditFrame(this.element, EditPanel.CREATION);
            this.addFrame.addEditPanelListener(new EditPanelListener() {
                @Override
                public void cancelled() {
                }

                @Override
                public void deleted() {
                }

                @Override
                public void inserted(int mid) {
                    ElementComboBox.this.setValue(mid);
                }

                @Override
                public void modified() {
                }
            });
        }
        return this.addFrame;
    }

    /**
     * The sql component of the add frame.
     * 
     * @return the sql component (creating the frame if necessary).
     */
    public final SQLComponent getAddComp() {
        return this.getAddFrame().getSQLComponent();
    }

    public String toString() {
        return this.getClass().getName() + " " + this.element;
    }

    protected void modeChanged(ComboMode mode) {
        super.modeChanged(mode);
        updateViewBtn();
        this.listButton.setEnabled(mode == ComboMode.EDITABLE);
        this.addButton.setEnabled(mode == ComboMode.EDITABLE);
    }

    private void updateViewBtn() {
        final boolean modif, enabled;
        if (getEnabled() == ComboMode.DISABLED || this.getSelectedId() < SQLRow.MIN_VALID_ID) {
            // disabled
            modif = this.canModif;
            enabled = false;
        } else if (this.canModif && getEnabled() == ComboMode.EDITABLE) {
            // modification enabled
            modif = true;
            enabled = true;
        } else {
            // view enabled
            modif = false;
            enabled = true;
        }
        this.setViewBtn(modif);
        this.viewButton.setEnabled(enabled);
    }

    public void setMinimal() {
        this.minimal = true;
    }

    /**
     * Toggle l'icone d'accès à la loupe
     * 
     * @param b <code>true</code> if "info" must be visible.
     */
    public void setInfoIconVisible(boolean b) {
        this.viewButton.setVisible(b);
    }

    /**
     * Toggle l'icone d'accès à la liste
     * 
     * @param b <code>true</code> if "list" must be visible.
     */
    public void setListIconVisible(boolean b) {
        this.listButton.setVisible(b);
    }

    public void setAddIconVisible(boolean b) {
        this.addButton.setVisible(b);
    }

    public void setButtonsVisible(boolean b) {
        this.setInfoIconVisible(b);
        this.setListIconVisible(b);
        this.setAddIconVisible(b);
    }

}
