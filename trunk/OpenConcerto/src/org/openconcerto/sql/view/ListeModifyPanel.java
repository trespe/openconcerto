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
 * ListeModifyFrame created on 8 févr. 2004
 */
package org.openconcerto.sql.view;

import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLComponent.Mode;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * @author ILM Informatique
 */
public class ListeModifyPanel extends IListPanel implements ValidListener {

    private SQLComponent modifComp;
    // le scrollPane qui contient modifComp
    private JScrollPane scrollPane;
    private String validText = null;

    public ListeModifyPanel(SQLElement elem) {
        super(elem);
    }

    public ListeModifyPanel(SQLElement elem, IListe list) {
        super(elem, list);
    }

    @Override
    public SQLComponent getModifComp() {
        return this.modifComp;
    }

    @Override
    protected SQLComponent getSQLComponent() {
        return this.getModifComp();
    }

    protected void addComponents(Container container, GridBagConstraints c) {
        // partie d'edition
        this.modifComp = this.element.createComponent();
        this.modifComp.setMode(Mode.MODIFICATION);
        this.getModifComp().uiInit();
        // initialize sqlObjects
        this.getModifComp().select(null);
        this.getModifComp().addValidListener(this);
        this.btnMngr.setAdditional(this.buttonModifier, new ITransformer<JButton, String>() {
            @Override
            public String transformChecked(JButton input) {
                return EditPanel.computeTooltip(ListeModifyPanel.this.validText == null, ListeModifyPanel.this.validText);
            }
        });

        this.scrollPane = new JScrollPane(this.getModifComp());
        Dimension d = this.getModifComp().getPreferredSize();

        if (d.height > 300)
            d.height = 300;

        // definint la taille miminim a afficher en bas (ne pas le virer)
        this.scrollPane.setMinimumSize(d);

        c.gridy++;
        c.weighty = 0;
        // p.getViewport().setPreferredSize(new Dimension(500,60));
        container.add(this.scrollPane, c);
    }

    protected void handleAction(JButton source, ActionEvent evt) {
        if (source == this.buttonModifier) {
            this.getModifComp().update();
        } else
            super.handleAction(source, evt);
    }

    @Override
    protected void listSelectionChanged(int id) {
        super.listSelectionChanged(id);
        // keep the current scrolling value, to restore it afterwards
        final int scroll = this.scrollPane.getVerticalScrollBar().getValue();
        this.getModifComp().select(id);
        // force buttonModifier update because the super method enables it
        // if a line is selected, then we select the new id, but if the new line
        // has the same empty fields than the previous one, no validChange is fired
        this.validChange(this.getModifComp(), this.getModifComp().isValidated());
        this.getModifComp().setEditable(id != -1);
        // have to invokeLater() since the scrollbar is changed after this method return
        // eg DefaultCaret.changeCaretPosition() -- caused by this.modifComp.select(id) -- will
        // invokeLater() repaintNewCaret() which will scrollRectToVisible()
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ListeModifyPanel.this.scrollPane.getVerticalScrollBar().setValue(scroll);
            }
        });
    }

    /*
     * notre comp de modif à changé d'état
     */
    public void validChange(ValidObject src, boolean newValue) {
        // MAYBE add a isAdjusting while changing id
        this.validText = newValue ? null : this.getModifComp().getValidationText();
        this.btnMngr.updateBtn(this.buttonModifier);
    }

}
