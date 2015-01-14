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
 
 package org.openconcerto.ui.component;

import java.text.ParseException;

import javax.swing.text.MaskFormatter;

/**
 * A special version of the {@link javax.swing.text.MaskFormatter} for
 * {@link javax.swing.JFormattedTextField formatted text fields} that supports the field being
 * emptied/left blank.
 * 
 * @author R.J. Lorimer
 */
public class AllowBlankMaskFormatter extends MaskFormatter {

    private boolean allowBlankField = true;
    private String blankRepresentation;

    public AllowBlankMaskFormatter() {
        super();
    }

    public AllowBlankMaskFormatter(String mask) throws ParseException {
        super(mask);
    }

    public void setAllowBlankField(boolean allowBlankField) {
        this.allowBlankField = allowBlankField;
    }

    public boolean isAllowBlankField() {
        return this.allowBlankField;
    }

    /**
     * Update our blank representation whenever the mask is updated.
     */
    @Override
    public void setMask(String mask) throws ParseException {
        super.setMask(mask);
        updateBlankRepresentation();
    }

    /**
     * Update our blank representation whenever the mask is updated.
     */
    @Override
    public void setPlaceholderCharacter(char placeholder) {
        super.setPlaceholderCharacter(placeholder);
        updateBlankRepresentation();
    }

    /**
     * Override the stringToValue method to check the blank representation.
     */
    @Override
    public Object stringToValue(String value) throws ParseException {
        Object result = value;
        if (isAllowBlankField() && this.blankRepresentation != null && this.blankRepresentation.equals(value)) {
            // an empty field should have a 'null' value.
            result = null;
        } else {
            result = super.stringToValue(value);
        }
        return result;
    }

    private void updateBlankRepresentation() {
        try {
            // calling valueToString on the parent class with a null attribute will get the 'blank'
            // representation.
            this.blankRepresentation = valueToString(null);
        } catch (ParseException e) {
            this.blankRepresentation = null;
        }
    }
}
