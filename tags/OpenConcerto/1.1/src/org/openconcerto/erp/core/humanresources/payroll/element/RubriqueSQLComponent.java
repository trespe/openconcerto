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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;

public abstract class RubriqueSQLComponent extends BaseSQLComponent {

    private final JTextField textCode;
    private final JLabel labelWarningBadName;
    private final JTextField textLibelle = new JTextField();
    private ValidState validCode = ValidState.getTrueInstance();

    public RubriqueSQLComponent(SQLElement element) {
        super(element);
        this.textCode = new JTextField();
        this.textCode.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                updateCodeValidity();
            }
        });
        this.labelWarningBadName = new JLabelWarning("Code déjà attribué");
    }

    @Override
    public final void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Code
        JLabel labelCode = new JLabel(getLabelFor("CODE"));
        labelCode.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelCode, c);

        c.gridx++;
        c.weightx = 1;
        this.add(this.textCode, c);
        c.weightx = 0;

        c.gridx++;
        this.add(this.labelWarningBadName, c);
        this.labelWarningBadName.setVisible(false);

        // Libelle
        c.gridy++;
        c.gridx = 0;
        JLabel labelNom = new JLabel(getLabelFor("NOM"));
        labelNom.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(labelNom, c);

        final boolean nl = this.newline();
        c.gridx++;
        c.gridwidth = nl ? GridBagConstraints.REMAINDER : 1;
        c.weightx = 1;
        this.add(this.textLibelle, c);
        c.weightx = 0;
        c.gridwidth = 1;
        if (nl)
            c.gridy++;

        this.addRequiredSQLObject(this.textCode, "CODE");
        this.addSQLObject(this.textLibelle, "NOM");

        this.addViews(c);
    }

    // new line after Libellé ?
    protected boolean newline() {
        return true;
    }

    protected abstract void addViews(GridBagConstraints c);

    private ValidState computeCodeValidity() {
        final String code = this.textCode.getText().trim();

        if (VariablePayeSQLElement.isForbidden(code))
            return ValidState.createCached(false, "Code réservé");

        // on vérifie que la variable n'existe pas déja
        final SQLSelect selAllCodeName = new SQLSelect(getTable().getBase());

        selAllCodeName.addSelectFunctionStar("count");
        selAllCodeName.setWhere(new Where(getTable().getField("CODE"), "=", code));

        final int idSelected = this.getSelectedID();
        if (idSelected >= SQLRow.MIN_VALID_ID) {
            selAllCodeName.andWhere(new Where(getTable().getField("ID"), "!=", idSelected));
        }

        final Number rubCount = (Number) getTable().getDBSystemRoot().getDataSource().executeScalar(selAllCodeName.asString());
        if (rubCount.intValue() > 0)
            return ValidState.createCached(false, "Code déjà attribué");

        final SQLSelect selAllVarName = new SQLSelect(getTable().getBase());
        final SQLTable tableVar = getTable().getTable("VARIABLE_PAYE");
        selAllVarName.addSelectFunctionStar("count");
        selAllVarName.setWhere(new Where(tableVar.getField("NOM"), "=", code));
        final Number payVarCount = (Number) getTable().getDBSystemRoot().getDataSource().executeScalar(selAllVarName.asString());

        return ValidState.createCached(payVarCount.intValue() == 0, "Code déjà attribué à une variable de paye");
    }

    private void updateCodeValidity() {
        this.setValidCode(this.computeCodeValidity());
    }

    private final void setValidCode(ValidState state) {
        if (!state.equals(this.validCode)) {
            this.validCode = state;
            final boolean visible = !this.validCode.isValid();
            this.labelWarningBadName.setVisible(visible);
            if (visible)
                this.labelWarningBadName.setText(this.validCode.getValidationText());
            this.fireValidChange();
        }
    }

    @Override
    public synchronized ValidState getValidState() {
        return super.getValidState().and(this.validCode);
    }

    @Override
    public void select(SQLRowAccessor r) {
        super.select(r);
        updateCodeValidity();
    }
}
