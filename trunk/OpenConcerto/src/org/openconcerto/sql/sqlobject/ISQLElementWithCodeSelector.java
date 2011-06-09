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
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.EditPanelListener;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JMultiLineToolTip;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.JImage;
import org.openconcerto.utils.OrderedSet;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.EmptyObject;
import org.openconcerto.utils.checks.EmptyObjectHelper;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.doc.Documented;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.text.JTextComponent;

import org.apache.commons.collections.Predicate;

public class ISQLElementWithCodeSelector extends JPanel implements ValueWrapper<Integer>, EmptyObject, RowItemViewComponent, SelectionListener, ActionListener, Documented, TextComponent {

    //
    private int id = SQLRow.NONEXISTANT_ID;
    private OrderedSet<ValidListener> validListener = new OrderedSet<ValidListener>();
    private EmptyObjectHelper emptyHelper;
    private PropertyChangeSupport supp;

    private SQLElement element;

    private SQLField optField;
    private SQLField mainField;

    ComboSQLRequest mainCombo;
    ComboSQLRequest optCombo;

    protected JButton addButton;
    boolean updating;

    protected boolean modeToSelect;

    boolean disableCompletion = false;
    private JImage warning;
    boolean mainOk;
    boolean optOk;

    private ITextWithCompletion textMain;
    private ITextWithCompletion textOpt;

    public SQLRowValues defaultCreateRowValues;

    private static ImageIcon icon = null;
    private JButton viewButton = new JButton() {
        @Override
        public JToolTip createToolTip() {
            // TODO Auto-generated method stub
            return new JMultiLineToolTip();
        }
    };
    private EditFrame viewFrame = null;

    public ISQLElementWithCodeSelector(SQLElement e, SQLField optField) {
        this(e, optField, null);
    }

    public ISQLElementWithCodeSelector(SQLElement e, SQLField optField, SQLRowValues defaultCreateRowValues) {

        this.optField = optField;
        this.element = e;
        this.updating = false;
        this.supp = new PropertyChangeSupport(this);
        this.emptyHelper = new EmptyObjectHelper(this, new Predicate() {
            public boolean evaluate(Object object /* cad le getUncheckedValue() */) {
                // final Integer val = (Integer) object;
                return ISQLElementWithCodeSelector.this.id == SQLRow.NONEXISTANT_ID;
            }
        });
        if (defaultCreateRowValues == null) {
            this.defaultCreateRowValues = new SQLRowValues(e.getTable());
        } else {
            this.defaultCreateRowValues = defaultCreateRowValues;
        }
    }

    public void setModeCompletionOnOpt(int mode) {
        this.textOpt.setModeCompletion(mode);
    }

    public void setModeCompletionOnMain(int mode) {
        this.textMain.setModeCompletion(mode);
    }

    @Override
    public void init(SQLRowItemView riv) {
        init(false);
    }

    protected boolean init(boolean expandWithShowAs) {
        this.setOpaque(false);
        // this.mainCombo = new ComboSQLRequest(this.element.getComboRequest());
        this.mainCombo = this.element.getComboRequest();
        // this.mainCombo = new ComboSQLRequest(this.element.getTable(), mainFields);

        this.mainField = this.mainCombo.getFields().iterator().next();

        if (expandWithShowAs) {
            this.optCombo = new ComboSQLRequest(this.element.getComboRequest());
        } else {
            List<String> optFields = new ArrayList<String>();
            optFields.add(this.optField.getName());
            this.optCombo = new ComboSQLRequest(this.element.getTable(), optFields);

        }

        this.uiInit();

        // Listeners
        this.textMain.addSelectionListener(this);
        this.textOpt.addSelectionListener(this);

        if (expandWithShowAs) {
            this.textMain.setFillWithField(this.mainField.getName());
            this.textOpt.setFillWithField(this.optField.getName());
        }

        return true;
    }

    private void uiInit() {

        if (icon == null) {
            icon = new ImageIcon(ISQLElementWithCodeSelector.class.getResource("loupe.png"));
        }
        this.viewButton.setBorder(null);
        this.viewButton.setOpaque(false);
        this.viewButton.setPreferredSize(new Dimension(24, 16));
        this.viewButton.setFocusPainted(false);
        this.viewButton.setEnabled(false);
        this.addEmptyListener(new EmptyListener() {
            @Override
            public void emptyChange(EmptyObj src, boolean newValue) {
                ISQLElementWithCodeSelector.this.viewButton.setEnabled(!isEmpty());
            }
        });

        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        // JLabel labelMain = this.getLabelFor(this.mainField);
        c.gridx = GridBagConstraints.RELATIVE;
        // c.weightx = 0;
        //
        // this.add(labelMain, c);

        this.textMain = new ITextWithCompletion(this.mainCombo, false);
        c.weightx = 1;
        this.textMain.setPreferredSize(new Dimension(160, (int) this.textMain.getPreferredSize().getHeight()));
        this.textMain.setMinimumSize(new Dimension(160, (int) this.textMain.getPreferredSize().getHeight()));
        this.add(this.textMain, c);
        c.insets = new Insets(0, 3, 0, 2);
        this.warning = new JImage(ISQLElementWithCodeSelector.class.getResource("warning.png"));
        c.weightx = 0;
        this.add(this.warning, c);

        JLabel labelCode = this.getLabelFor(this.optField); // optField
        c.weightx = 0;
        this.add(labelCode, c);

        this.textOpt = new ITextWithCompletion(this.optCombo, false);
        this.textOpt.setPreferredSize(new Dimension(100, (int) this.textOpt.getPreferredSize().getHeight()));
        this.textOpt.setMinimumSize(new Dimension(100, (int) this.textOpt.getPreferredSize().getHeight()));
        c.weightx = 1;
        this.add(this.textOpt, c);

        this.addButton = new JButton("Créer " + this.element.getSingularName());
        this.addButton.addActionListener(this);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        this.add(this.addButton, c);

        this.viewButton.setIcon(icon);
        this.viewButton.setContentAreaFilled(false);
        this.add(this.viewButton, c);
        this.viewButton.addActionListener(this);

    }

    @Override
    public void setValue(Integer val) {
        this.selectId(val);
    }

    @Override
    public void resetValue() {
    }

    @Override
    public Object getUncheckedValue() {
        return this.getValue();
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        this.textOpt.setEditable(b);
        this.textMain.setEditable(b);
        this.addButton.setEnabled(b);
    }

    @Override
    public Integer getValue() throws IllegalStateException {
        return this.getSelectedId();
    }

    private int getSelectedId() {
        return this.id;
    }

    @Override
    public boolean isEmpty() {
        return this.emptyHelper.isEmpty();
    }

    @Override
    public void addEmptyListener(EmptyListener l) {
        this.emptyHelper.addListener(l);
    }

    @Override
    public void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    public void removeValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.removeValueListener(l);
    }

    @Override
    public boolean isValidated() {
        return true;
    }

    @Override
    public void addValidListener(ValidListener l) {
        if (!this.validListener.contains(l)) {
            this.validListener.add(l);
        }
    }

    public void removeValidListener(ValidListener l) {
        this.validListener.remove(l);
    }

    public JLabel getLabelFor(SQLField aField) {
        return new JLabel(Configuration.getTranslator(aField.getTable()).getLabelFor(aField));
    }

    void selectId(int anId) {
        this.setId(anId);
        this.textMain.selectId(anId);
        this.textOpt.selectId(anId);
    }

    private void setId(int id) {
        if (id < SQLRow.MIN_VALID_ID) {
            this.id = SQLRow.NONEXISTANT_ID;
        } else {
            this.id = id;
        }
        this.warning.setVisible(this.id == SQLRow.NONEXISTANT_ID);
        this.supp.firePropertyChange("value", null, this.getUncheckedValue());
    }

    @Override
    public void idSelected(int anId, Object source) {
        if (source == this.textOpt) {
            this.textMain.selectId(anId);
        } else {
            this.textOpt.selectId(anId);
        }
        this.setId(anId);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == this.viewButton) {
            if (this.viewFrame == null) {
                this.viewFrame = new EditFrame(this.element, EditPanel.MODIFICATION);
            }
            this.viewFrame.selectionId(getSelectedId());
            if (!this.viewFrame.isShowing())
                this.viewFrame.setVisible(true);
            else
                this.viewFrame.setVisible(false);
        } else {

            if (e.getSource() == this.addButton) {
                EditFrame edit = new EditFrame(this.element);

                // Reprise des champs saisis
                // SQLRowValues rowValsAdd = new SQLRowValues(this.element.getTable());
                this.defaultCreateRowValues.put(this.mainField.getName(), this.textMain.getText());
                this.defaultCreateRowValues.put(this.optField.getName(), this.textOpt.getText());
                edit.getPanel().getSQLComponent().select(this.defaultCreateRowValues);

                edit.setVisible(true);
                // SQLRowValues rowVals = new SQLRowValues(this.element.getTable());
                // System.err.println("Main Field " + this.mainField.getName() + " OptField " +
                // this.optField.getName());
                // rowVals.put(this.mainField.getName(), this.textMain.getText());
                // rowVals.put(this.optField.getName(), this.textOpt.getText());
                // edit.getSQLComponent().select(rowVals);
                edit.addEditPanelListener(new EditPanelListener() {
                    public void cancelled() {
                    }

                    public void deleted() {
                    }

                    public void inserted(int mid) {
                        ISQLElementWithCodeSelector.this.textMain.loadCache();
                        ISQLElementWithCodeSelector.this.textOpt.loadCache();
                        selectId(mid);
                    }

                    public void modified() {
                    }
                });
            }
        }
    }

    @Override
    public JComponent getComp() {
        return this;
    }

    @Override
    public JTextComponent getTextComp() {
        return this.textMain.getTextComp();
    }

    public String getTextMain() {
        return this.textMain.getText().toString();
    }

    public String getTextOpt() {
        return this.textOpt.getText().toString();
    }

    public void loadCache() {
        this.textMain.loadCache();
        this.textOpt.loadCache();
    }

    @Override
    public String getDocId() {
        return "ECOMPL_" + this.element.getTable().getName() + "_" + this.optField.getFullName();
    }

    @Override
    public String getGenericDoc() {
        return "";
    }

    @Override
    public boolean onScreen() {
        return true;
    }

    @Override
    public boolean isDocTransversable() {
        return false;
    }

    public SQLElement getSQLElement() {
        return this.element;
    }

    public void setViewButtonDefaultIcon() {

        this.viewButton.setIcon(icon);
    }

    public JButton getViewButton() {

        return this.viewButton;
    }

    @Override
    public String getValidationText() {
        // TODO Auto-generated method stub
        return "aucun champ n'est sélectionné";
    }

    /**
     * Applique un filtre w sur le ISQLElementWithCode
     * 
     * @param w Where qui permet de filtrer. Si null aucun filtre.
     */
    public void setWhereOnRequest(Where w) {
        this.mainCombo.setWhere(w);
        this.optCombo.setWhere(w);
        loadCache();
    }

}
