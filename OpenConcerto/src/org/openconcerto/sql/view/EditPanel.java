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
 
 package org.openconcerto.sql.view;

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLComponent.Mode;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.rights.TableAllRights;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.doc.Documented;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.border.Border;

/**
 * @author ilm Created on 8 oct. 2003
 */
public class EditPanel extends JPanel implements IListener, ActionListener, Documented {
    public static enum EditMode {
        CREATION {
            @Override
            protected Mode getCompMode() {
                return Mode.INSERTION;
            }
        },
        MODIFICATION {
            @Override
            protected Mode getCompMode() {
                return Mode.MODIFICATION;
            }
        },
        READONLY {
            @Override
            protected Mode getCompMode() {
                return null;
            }
        };
        protected abstract Mode getCompMode();
    };

    /**
     * If this system property is true, then new lines are always added at the end of the list.
     * Otherwise they're added after the selection.
     */
    public final static String ADD_AT_THE_END = "org.openconcerto.sql.editPanel.endAdd";

    public final static EditMode CREATION = EditMode.CREATION;
    public final static EditMode MODIFICATION = EditMode.MODIFICATION;
    public final static EditMode READONLY = EditMode.READONLY;

    private final EditMode mode;
    private JButton jButtonSupprimer;
    private JButton jButtonModifier;
    private JButton jButtonAjouter;
    private JButton jButtonAnnuler;
    private final SQLComponent component;

    private JCheckBox keepOpen = new JCheckBox("ne pas fermer la fenêtre");

    private final List<EditPanelListener> panelListeners = new Vector<EditPanelListener>();// EditPanelListener
    private JScrollPane p;
    private final SQLElement element;
    private IListe l;
    // whether our component is valid
    private ValidState valid = ValidState.getNoReasonInstance(false);

    /**
     * Creates an creation panel
     * 
     * @param e the element to display.
     */
    public EditPanel(SQLElement e) {
        this(e, CREATION);
    }

    public EditPanel(SQLElement e, EditMode mode) {
        this(e.createDefaultComponent(), mode);
    }

    public EditPanel(SQLComponent e, EditMode mode) {
        this(e, mode, Collections.<SQLField> emptyList());
    }

    public EditPanel(SQLElement e, EditMode mode, List<SQLField> hiddenFields) {
        this(e.createDefaultComponent(), mode, hiddenFields);
    }

    /**
     * Creates an instance:
     * <ul>
     * <li>to create if mode is CREATION</li>
     * <li>to modify if MODIFICATION</li>
     * <li>to view if READONLY</li>
     * </ul>
     * use "org.openconcerto.editpanel.noborder" and "org.openconcerto.editpanel.separator" for custom style
     * 
     * @param e the element to display.
     * @param mode the edit mode, one of CREATION, MODIFICATION or READONLY.
     * @param hiddenFields
     */
    public EditPanel(SQLComponent e, EditMode mode, List<SQLField> hiddenFields) {
        super();

        this.l = null;

        // ATTN verification seulement dans uiInit()
        this.mode = mode;
        this.element = e.getElement();

        this.component = e;
        try {
            this.component.setMode(mode.getCompMode());
            this.component.setNonExistantEditable(this.mode == CREATION);
            if (this.component instanceof BaseSQLComponent) {
                for (int i = 0; i < hiddenFields.size(); i++) {
                    final SQLField hiddenField = hiddenFields.get(i);
                    ((BaseSQLComponent) this.component).doNotShow(hiddenField);
                }
            }

            if (this.mode == READONLY) {
                this.component.setEditable(false);
            } else {
                // on écoute les changements de validation,
                // avant component.uiInit() car il fait un fireValidChange()
                this.component.addValidListener(new ValidListener() {
                    @Override
                    public void validChange(ValidObject src, ValidState newValue) {
                        // expensive so cache it
                        EditPanel.this.valid = newValue;
                        updateBtns();
                    }
                });
                ((BaseSQLComponent) this.component).addSelectionListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateBtns();
                    }
                });
            }

            this.uiInit();
            this.component.uiInit();

            if (Boolean.getBoolean("org.openconcerto.editpanel.noborder")) {
                this.setInnerBorder(null);
            }
        } catch (Exception ex) {
            ExceptionHandler.handle("Erreur d'initialisation", ex);
        }
    }

    private void updateBtns() {
        updateBtn(this.jButtonAjouter, true, false, "d'ajouter", TableAllRights.ADD_ROW_TABLE);
        updateBtn(this.jButtonModifier, true, true, "de modifier", TableAllRights.MODIFY_ROW_TABLE);
        updateBtn(this.jButtonSupprimer, false, true, "de supprimer", TableAllRights.DELETE_ROW_TABLE);
    }

    private void updateBtn(final JButton b, final boolean needValid, final boolean needID, final String desc, final String code) {
        if (b != null) {
            final ValidState res;
            final boolean idOK = this.getSQLComponent().getSelectedID() >= SQLRow.MIN_VALID_ID;
            final UserRights rights = UserRightsManager.getCurrentUserRights();
            if (!TableAllRights.hasRight(rights, code, getSQLComponent().getElement().getTable())) {
                res = ValidState.createCached(false, "Vous n'avez pas le droit " + desc);
            } else if (needID && !idOK)
                res = ValidState.createCached(false, "cet élément n'existe pas");
            else if (needValid && !this.valid.isValid())
                res = this.valid;
            else
                res = ValidState.getTrueInstance();
            updateBtn(b, res);
        }
    }

    private final void uiInit() {
        this.fill();

        // les raccourcis claviers
        this.component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "add");
        this.component.getActionMap().put("add", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ajouter();
            }
        });
    }

    protected void fill() {
        Container container = this;

        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1;
        c.weightx = 0;
        c.insets = new Insets(2, 2, 1, 2);
        // container.add(new JLabel(this.getTitle()), c);

        c.weighty = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;

        this.p = new JScrollPane(this.component);
        this.p.getVerticalScrollBar().setUnitIncrement(9);
        this.p.setOpaque(false);
        this.p.getViewport().setOpaque(false);
        this.p.setMinimumSize(new Dimension(60, 60));

        container.add(this.p, c);

        // Separator, if needed
        if (Boolean.getBoolean("org.openconcerto.editpanel.separator")) {
            c.gridy++;
            c.weightx = 1;
            c.weighty = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            final JSeparator comp = new JSeparator();
            // Evite les tremblements verticaux
            comp.setMinimumSize(new Dimension(comp.getPreferredSize()));
            container.add(comp, c);
        }

        // Buttons
        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.NONE;
        this.keepOpen.setOpaque(false);
        if (this.mode == CREATION) {

            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.EAST;
            if (!Boolean.getBoolean("org.openconcerto.editpanel.hideKeepOpen")) {
                container.add(this.keepOpen, c);
            }
            this.keepOpen.addActionListener(this);
            c.fill = GridBagConstraints.NONE;
            c.gridx = 2;
            c.anchor = GridBagConstraints.EAST;
            this.jButtonAjouter = new JButton("Ajouter");
            container.add(this.jButtonAjouter, c);
            // Listeners
            this.jButtonAjouter.addActionListener(this);
            this.jButtonAjouter.addMouseListener(new MouseAdapter() {
                // not mouseClicked() since we'd be called after mouse release, i.e. after the
                // action. In an EditFrame (with "do not close" checked) on an IListPanel when add
                // is performed the list will select the new row, in doing so it first clears the
                // selection, which empties us, then we get called and thus display that all
                // required fields are empty
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!jButtonAjouter.isEnabled()) {
                        final String toolTipText = jButtonAjouter.getToolTipText();
                        if (toolTipText != null && !toolTipText.isEmpty()) {
                            JOptionPane.showMessageDialog(EditPanel.this, toolTipText);
                        }
                    }
                }
            });

        } else if (this.mode == MODIFICATION) {
            c.gridx = 1;
            c.anchor = GridBagConstraints.EAST;
            this.jButtonModifier = new JButton("Enregistrer les modifications");
            container.add(this.jButtonModifier, c);
            c.weightx = 0;
            c.gridx = 2;
            this.jButtonSupprimer = new JButton("Supprimer");
            container.add(this.jButtonSupprimer, c);
            // Listeners
            this.jButtonModifier.addActionListener(this);
            this.jButtonSupprimer.addActionListener(this);
        }
        c.weightx = 0;
        c.gridx = 3;
        c.anchor = GridBagConstraints.EAST;
        if (this.mode == READONLY)
            this.jButtonAnnuler = new JButton("Fermer");
        else
            this.jButtonAnnuler = new JButton("Annuler");
        container.add(this.jButtonAnnuler, c);
        // Listeners
        this.jButtonAnnuler.addActionListener(this);
        // this.getContentPane().add(container);
        // this.getContentPane().add(new JScrollPane(container));
    }

    /**
     * Redimensionne la frame pour qu'elle soit de taille maximum sans déborder de l'écran. <img
     * src="doc-files/resizeFrame.png"/>
     */
    public Dimension getViewResizedDimesion(Dimension frameSize) {

        // MAYBE remonter la frame pour pas qu'elle dépasse en bas
        final Dimension viewSize = this.p.getViewport().getView().getSize();
        final int verticalHidden = viewSize.height - this.p.getVerticalScrollBar().getVisibleAmount();
        final int horizontalHidden = viewSize.width - this.p.getHorizontalScrollBar().getVisibleAmount();

        final Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        final int maxV = ((int) bounds.getMaxY()) - this.getY();
        final int maxH = ((int) bounds.getMaxX()) - this.getX();

        final int vertical = Math.min(frameSize.height + verticalHidden, maxV);
        final int horizontal = Math.min(frameSize.width + horizontalHidden, maxH);

        return new Dimension(horizontal, vertical);

    }

    public void setInnerBorder(Border b) {
        this.p.setBorder(b);
    }

    public void selectionId(int id, int field) {
        // inutile de ne se remplir qu'avec des valeurs valides (pour éviter le resetValue() qd
        // déselection et donc l'écrasement des modif en cours) car de toute façon on est obligé de
        // laisser passer les valides qui écrasent tout autant.
        if (id < SQLRow.MIN_VALID_ID)
            this.component.select(null);
        else if (this.mode == CREATION) {
            this.component.select(this.element.createCopy(id));
        } else {
            this.component.select(id);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == this.jButtonAnnuler) {
            try {
                this.fireCancelled();// this.dispose();
            } catch (Throwable ex) {
                ExceptionHandler.handle("Erreur pendant l'annulation", ex);
            }
        } else if (e.getSource() == this.jButtonModifier) {
            try {
                modifier();
            } catch (Throwable ex) {
                ExceptionHandler.handle("Erreur pendant la modification", ex);
            }
        } else if (e.getSource() == this.jButtonAjouter) {
            try {
                ajouter();
            } catch (Throwable ex) {
                ExceptionHandler.handle("Erreur pendant l'ajout", ex);
            }
        } else if (e.getSource() == this.jButtonSupprimer) {
            try {
                if (this.element.askArchive(this, this.component.getSelectedID())) {
                    this.fireDeleted();
                    // this.dispose(); // on ferme la fenetre
                }
            } catch (Throwable ex) {
                ExceptionHandler.handle("Erreur pendant la suppression", ex);
            }
        }

    }

    /**
     * 
     */
    public void modifier() {
        this.component.update();
        this.fireModified();
    }

    public boolean alwaysVisible() {
        return this.keepOpen.isSelected();
    }

    public void setAlwaysVisible(boolean b) {
        this.keepOpen.setSelected(true);
    }

    private void ajouter() {
        // ne pas laisser ajouter par le raccourci clavier quand le bouton est grisé
        if (this.jButtonAjouter.isEnabled()) {
            final int id;
            if (!Boolean.getBoolean(ADD_AT_THE_END) && this.l != null && !this.l.isDead() && this.l.getDesiredRow() != null)
                id = this.component.insert(this.l.getDesiredRow());
            else
                id = this.component.insert();
            if (this.l != null)
                this.l.selectID(id);
            this.fireInserted(id);
        }
    }

    private void fireDeleted() {
        for (int i = 0; i < this.panelListeners.size(); i++) {
            EditPanelListener listener = this.panelListeners.get(i);
            listener.deleted();
        }
    }

    private void fireInserted(int id) {
        for (int i = 0; i < this.panelListeners.size(); i++) {
            EditPanelListener listener = this.panelListeners.get(i);
            listener.inserted(id);
        }
    }

    private void fireModified() {
        for (int i = 0; i < this.panelListeners.size(); i++) {
            EditPanelListener listener = this.panelListeners.get(i);
            listener.modified();
        }
    }

    private void fireCancelled() {
        for (int i = 0; i < this.panelListeners.size(); i++) {
            EditPanelListener listener = this.panelListeners.get(i);
            listener.cancelled();
        }
    }

    public void addEditPanelListener(EditPanelListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("null listener");
        }
        if (!this.panelListeners.contains(listener)) {
            this.panelListeners.add(listener);
        }
    }

    public void removeEditPanelListener(EditPanelListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("null listener");
        }
        if (this.panelListeners.contains(listener)) {
            this.panelListeners.remove(listener);
        }
    }

    static void updateBtn(JButton btn, ValidState validState) {
        btn.setEnabled(validState.isValid());
        btn.setToolTipText(computeTooltip(validState));
    }

    static String computeTooltip(ValidState validState) {
        return computeTooltip(validState.isValid(), validState.getValidationText());
    }

    static private String computeTooltip(boolean valid, final String cause) {
        final String res;
        if (valid)
            res = null;
        else {
            final String c = cause == null ? "" : cause.trim();
            String validationText = "Les champs de saisie ne sont pas remplis correctement.\n\nVous ne pouvez enregistrer les modifications car";
            if (c.length() > 0)
                validationText += "\n" + c;
            else
                validationText += " elles ne sont pas valides";
            res = "<html>" + validationText.replaceAll("\n", "<br>") + "</html>";
        }
        return res;
    }

    public void disableCancel() {
        this.jButtonAnnuler.setVisible(false);
    }

    public void disableDelete() {
        this.jButtonSupprimer.setVisible(false);
    }

    public void resetValue() {
        this.component.resetValue();
    }

    public void addComponentListenerOnViewPort(ComponentListener l) {
        this.p.getViewport().getView().addComponentListener(l);
    }

    public void setModifyLabel(String label) {
        this.jButtonModifier.setText(label);

    }

    public SQLComponent getSQLComponent() {
        return this.component;
    }

    /**
     * Permet de forcer qq valeur
     */
    public void setValues(List<SQLRow> sqlRows) {
        // eg /BATIMENT/
        final SQLTable t = this.component.getElement().getTable();
        final SQLRowValues vals = new SQLRowValues(t);
        final String parentForeignField = this.component.getElement().getParentForeignField();
        if (parentForeignField == null)
            return;
        // eg |BATIMENT.ID_SITE|
        final SQLField parentFF = t.getField(parentForeignField);
        // eg /SITE/
        final SQLTable foreignT = t.getBase().getGraph().getForeignTable(parentFF);

        for (int i = 0; i < sqlRows.size(); i++) {
            final SQLRow row = sqlRows.get(i);
            if (row.getTable().equals(foreignT)) {
                vals.put(parentFF.getName(), row.getID());
            }
        }

        this.component.select(vals);
    }

    public void setIList(IListe l) {
        this.l = l;
    }

    public String getDocId() {
        return this.mode + "_" + this.element.getTable().getName();
    }

    public String getGenericDoc() {
        return "";
    }

    public boolean onScreen() {
        return false;
    }

    public boolean isDocTransversable() {
        return true;
    }
}
