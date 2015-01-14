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
 
 package org.openconcerto.record.spec;

import org.openconcerto.record.Constraint;

import java.util.HashMap;
import java.util.Map;

public final class RecordItemSpecBuilder {

    private final String name;
    private final Type type;
    private int maxListSize;
    private String referencedSpec;
    private Object defaultValue;
    private boolean required;
    private Constraint isValid, isEmpty;
    private final Map<String, Object> validProps;
    private boolean userMustCheck, userMustModify;

    public RecordItemSpecBuilder(final String name, final Type type) {
        super();
        if (name == null)
            throw new NullPointerException("Null value");
        this.name = name;
        this.type = type;
        this.maxListSize = 0;
        this.validProps = new HashMap<String, Object>(8);
    }

    public String getName() {
        return this.name;
    }

    public Type getType() {
        return this.type;
    }

    public int getMaxListSize() {
        return this.maxListSize;
    }

    public void setMaxListSize(int maxListSize) {
        this.maxListSize = maxListSize;
    }

    public String getReferencedSpec() {
        return this.referencedSpec;
    }

    public void setReferencedSpec(String referencedSpec) {
        this.referencedSpec = referencedSpec;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public RecordItemSpecBuilder setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public boolean isRequired() {
        return this.required;
    }

    public RecordItemSpecBuilder setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public Constraint getValidConstraint() {
        return this.isValid;
    }

    public RecordItemSpecBuilder setValidConstraint(Constraint isValid) {
        this.isValid = isValid;
        return this;
    }

    public Map<String, Object> getValidProps() {
        return this.validProps;
    }

    public Constraint getEmptyConstraint() {
        return this.isEmpty;
    }

    public RecordItemSpecBuilder setEmptyConstraint(Constraint isEmpty) {
        this.isEmpty = isEmpty;
        return this;
    }

    public boolean mustUserCheck() {
        return this.userMustCheck;
    }

    public RecordItemSpecBuilder setUserMustCheck(boolean userMustCheck) {
        this.userMustCheck = userMustCheck;
        return this;
    }

    public boolean mustUserModify() {
        return this.userMustModify;
    }

    public RecordItemSpecBuilder setUserMustModify(boolean userMustModify) {
        this.userMustModify = userMustModify;
        return this;
    }

    public final RecordItemSpec build() {
        return new RecordItemSpec(this.name, this.type, this.maxListSize, this.referencedSpec, this.defaultValue, this.required, this.isValid, this.validProps, this.isEmpty, this.userMustCheck,
                this.userMustModify);
    }
}
