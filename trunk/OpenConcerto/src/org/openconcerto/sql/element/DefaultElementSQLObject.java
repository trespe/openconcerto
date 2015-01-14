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
 
 /*
 * Créé le 2 mai 2005
 */
package org.openconcerto.sql.element;

import static org.openconcerto.sql.TM.getTM;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * A default ElementSQLObject that displays a button to create, and when created a button to delete
 * and a button to hide/expand.
 * 
 * @author Sylvain CUAZ
 */
public class DefaultElementSQLObject extends ElementSQLObject {

    // possible si currentID != 1 && isValidated
    private JButton expandBtn;
    private boolean expanded;

    // valent null si requis
    private JButton createBtn;
    private JButton supprBtn;
    private JSeparator separator;
    private boolean isSeparatorVisible = true;
    private JPanel editP;
    private JPanel createP;

    public DefaultElementSQLObject(SQLComponent parent, SQLComponent comp) {
        super(parent, comp);

        this.addValidListener(new ValidListener() {
            @Override
            public void validChange(ValidObject src, ValidState newValue) {
                compChanged();
            }
        });
    }

    public void showSeparator(boolean visible) {
        this.isSeparatorVisible = visible;
        if (this.separator != null)
            this.separator.setVisible(visible);
    }

    public void setDecorated(boolean decorated) {
        if (this.expandBtn != null)
            this.expandBtn.setVisible(decorated);
    }

    @Override
    protected void uiChanged() {
        super.uiChanged();
        updateBtns();
    }

    protected void updateBtns() {
        this.createBtn.setVisible(this.isCreatedUIVisible());
        this.supprBtn.setVisible(this.isCreatedUIVisible() && this.deleteAllowed() && !Boolean.FALSE.equals(this.isExpanded()));
    }

    @Override
    protected void uiInit() {
        final boolean isPlastic = UIManager.getLookAndFeel().getClass().getName().startsWith("com.jgoodies.plaf.plastic");

        this.expandBtn = new JButton("+/-");
        this.expandBtn.setEnabled(false);
        this.expandBtn.setOpaque(false);
        this.expandBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleExpand();
            }
        });

        this.supprBtn = new JButton(new ImageIcon(DefaultElementSQLObject.class.getResource("delete.png")));
        this.supprBtn.setToolTipText(getTM().translate("remove"));
        this.supprBtn.setOpaque(false);
        if (isPlastic)
            this.supprBtn.setBorder(BorderFactory.createEmptyBorder());
        this.supprBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0 || this.confirm())
                    setCreated(false);
            }

            private boolean confirm() {
                return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(DefaultElementSQLObject.this, getTM().trM("elementSQLObject.delete", "element", getSQLChild().getElement().getName()),
                        getTM().trA("sqlElement.confirmDelete"), JOptionPane.YES_NO_OPTION);
            }
        });
        this.createBtn = new JButton(EditFrame.getCreateMessage(this.getSQLChild().getElement()));
        // false leaves only a line for the button under Plastic3DLookAndFeel
        this.createBtn.setOpaque(isPlastic);
        this.createBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCreated(true);
            }
        });

        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    }

    @Override
    protected final void setCreatePanel() {
        if (this.editP != null)
            this.editP.setVisible(false);
        this.getCreatePanel().setVisible(true);
        this.createBtn.requestFocusInWindow();
        // don't call setCurrentID() otherwise, when updating we won't know
        // what observation to archive
        this.revalidate();
        this.repaint();
    }

    private final JPanel getCreatePanel() {
        if (this.createP == null) {
            this.createP = new JPanel();
            this.createP.setOpaque(false);
            this.createP.setLayout(new GridLayout());
            this.createP.add(this.createBtn);
            this.add(this.createP);
        }
        return this.createP;
    }

    @Override
    protected final void setEditPanel() {
        if (this.createP != null)
            this.createP.setVisible(false);
        this.getEditPanel().setVisible(true);
        this.getEditPanel().requestFocusInWindow();
        // toujours afficher un composant vierge à la création
        // do it after add() as it calls SQLRowView.activate() which refresh comp from the db
        // possibly resurecting a deleted row
        this.getSQLChild().resetValue();

        this.revalidate();
        // Swing ne le fait pas de lui même
        // => on sélectionne une longue observation, puis une courte
        // la fin de la longue reste affichée à l'écran, pareil pour les DS
        this.repaint();
        this.expand(true);
    }

    private final JPanel getEditPanel() {
        if (this.editP == null) {
            this.editP = new JPanel();
            this.editP.setLayout(new GridBagLayout());
            this.editP.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;

            c.weightx = 0.001;
            c.weighty = 0;
            // en haut a gauche pour que quand on cache, le bouton reste à la même place
            c.anchor = GridBagConstraints.FIRST_LINE_START;
            this.editP.add(this.expandBtn, c);
            c.gridy++;
            this.editP.add(this.supprBtn, c);

            c.gridx = 2;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 1;
            c.gridheight = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            this.getSQLChild().uiInit();
            this.editP.add(this.getSQLChild(), c);

            if (this.isSeparatorVisible) {
                c.gridx = 1;
                c.gridy = 0;
                c.weightx = 0;
                c.insets = new Insets(2, 2, 2, 2);
                this.separator = new JSeparator(SwingConstants.VERTICAL);
                this.editP.add(this.separator, c);
            }
            this.add(this.editP);
        }
        return this.editP;
    }

    @Override
    protected void compChanged() {
        this.expandBtn.setEnabled(this.getCurrentID() != SQLRow.NONEXISTANT_ID && this.getValidState().isValid());
    }

    /**
     * Whether the edit panel is expanded.
     * 
     * @return <code>true</code> if the edit panel is expanded, <code>false</code> if not and
     *         <code>null</code> if not {@link #isCreated()}.
     */
    private final Boolean isExpanded() {
        return this.isCreated() ? this.expanded : null;
    }

    private void expand(boolean b) {
        if (!this.isCreated()) {
            throw new IllegalStateException("cannot expand if not created");
        }

        this.expanded = b;
        this.updateBtns();
        this.getSQLChild().setVisible(b);
        this.validate();
    }

    private void toggleExpand() {
        this.expand(!this.isExpanded());
    }

    @Override
    public void setEditable(boolean enabled) {
        super.setEditable(enabled);
        this.createBtn.setEnabled(enabled);
        this.supprBtn.setEnabled(enabled);
    }

}
